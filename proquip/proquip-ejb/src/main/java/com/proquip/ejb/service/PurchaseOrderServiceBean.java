package com.proquip.ejb.service;

import com.proquip.common.constant.AppConstants;
import com.proquip.common.exception.ApprovalException;
import com.proquip.common.exception.BusinessException;
import com.proquip.common.exception.EntityNotFoundException;
import com.proquip.common.exception.ValidationException;
import com.proquip.common.util.DateUtils;
import com.proquip.common.util.PriceCalculationHelper;
import com.proquip.ejb.entity.organization.UserProfile;
import com.proquip.ejb.entity.pricing.Budget;
import com.proquip.ejb.entity.procurement.ApprovalStep;
import com.proquip.ejb.entity.procurement.ApprovalWorkflow;
import com.proquip.ejb.entity.procurement.GoodsReceipt;
import com.proquip.ejb.entity.procurement.GoodsReceiptItem;
import com.proquip.ejb.entity.procurement.PurchaseOrder;
import com.proquip.ejb.entity.procurement.PurchaseOrderItem;
import com.proquip.ejb.entity.procurement.PurchaseOrderStatusHistory;
import com.proquip.ejb.entity.product.Product;
import com.proquip.ejb.entity.supplier.Supplier;
import com.proquip.ejb.entity.supplier.SupplierProduct;
import com.proquip.ejb.entity.inventory.InventoryItem;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 発注管理サービスBean。
 * <p>
 * 発注に関するすべてのビジネスロジックを集約したステートレスセッションBean。
 * 発注のCRUD操作、承認ワークフロー、入庫処理、予算チェック、レポート生成、
 * 通知送信、CSV出力など、発注に関連するあらゆる処理を一手に引き受ける。
 * </p>
 *
 * <p>【技術的負債 #1 - Godクラス】
 * このクラスは40以上のメソッドと15以上のインジェクション依存を持つGodクラスである。
 * 単一責任の原則に明らかに違反しており、以下のサービスに分割すべき：
 * <ul>
 *   <li>PurchaseOrderCrudService — CRUD操作</li>
 *   <li>PurchaseOrderApprovalService — 承認ワークフロー</li>
 *   <li>PurchaseOrderReceiptService — 入庫処理</li>
 *   <li>PurchaseOrderReportService — レポート生成</li>
 *   <li>PurchaseOrderExportService — CSV出力</li>
 * </ul>
 * </p>
 *
 * @author ProQuip開発チーム（初期実装: 2019年、最終更新: 2024年）
 * @since 1.0.0
 */
@Stateless
public class PurchaseOrderServiceBean {

    private static final Logger logger = Logger.getLogger(PurchaseOrderServiceBean.class.getName());

    // 技術的負債 #20: 循環依存 — InventoryServiceBeanとの相互参照
    @EJB
    private InventoryServiceBean inventoryService;

    @EJB
    private SupplierServiceBean supplierService;

    @EJB
    private BudgetServiceBean budgetService;

    @EJB
    private NotificationServiceBean notificationService;

    @EJB
    private AuditServiceBean auditService;

    // 技術的負債: DAOがあるのにEntityManagerも直接使用している
    @PersistenceContext
    private EntityManager em;

    @Resource
    private SessionContext sessionContext;

    // ========================================================================
    // CRUD操作
    // ========================================================================

    /**
     * 発注を新規作成する。
     * <p>
     * バリデーション、エンティティ生成、明細作成、予算チェック、承認開始、
     * 通知送信をすべて1つのメソッドで実行する。
     * </p>
     *
     * <p>【技術的負債 #15 - モノリシックトランザクション】
     * このメソッドは80行以上あり、1つのトランザクション内で
     * あまりにも多くの処理を行っている。障害発生時の原因特定が困難。</p>
     *
     * @param order 発注エンティティ（明細を含む）
     * @return 作成された発注エンティティ
     * @throws ValidationException バリデーションエラー時
     * @throws BudgetExceededException 予算超過時
     */
    public PurchaseOrder createOrder(PurchaseOrder order) {
        logger.info("発注作成処理を開始します。");

        // バリデーション
        validateOrder(order);

        // 発注番号の自動採番
        String poNumber = generatePoNumber();
        order.setPoNumber(poNumber);
        order.setOrderDate(new Date());
        order.setStatus("DRAFT");

        // 技術的負債: 監査フィールドを手動設定（インターセプターで自動化すべき）
        String currentUser = getCurrentUser();
        order.setCreatedBy(currentUser);
        order.setUpdatedBy(currentUser);

        // サプライヤーの存在チェック
        if (order.getSupplier() == null || order.getSupplier().getId() == null) {
            throw new ValidationException("VAL_001", "サプライヤーが指定されていません。");
        }
        Supplier supplier = em.find(Supplier.class, order.getSupplier().getId());
        if (supplier == null) {
            throw new EntityNotFoundException("Supplier", order.getSupplier().getId());
        }
        // 技術的負債 #14: ステータス文字列のマジックストリング
        if (!"ACTIVE".equals(supplier.getStatus()) && !"active".equals(supplier.getStatus())) {
            throw new ValidationException("VAL_002",
                    "無効なサプライヤーです。ステータス: " + supplier.getStatus());
        }
        order.setSupplier(supplier);

        // 明細の処理
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (order.getItems() != null) {
            int lineNum = 1;
            for (int i = 0; i < order.getItems().size(); i++) {
                PurchaseOrderItem item = order.getItems().get(i);
                // lineNumber field was removed from PurchaseOrderItem (DDL alignment)
                lineNum++;
                item.setPurchaseOrder(order);
                item.setStatus("PENDING");

                // 小計の計算
                BigDecimal subtotal = calculateItemAmount(item);
                item.setSubtotal(subtotal);
                item.setReceivedQuantity(BigDecimal.ZERO);
                totalAmount = totalAmount.add(subtotal);
            }
        }
        order.setTotalAmount(totalAmount);

        // 通貨のデフォルト設定
        if (order.getCurrency() == null || order.getCurrency().isEmpty()) {
            order.setCurrency("JPY");
        }

        // 予算チェック
        if (order.getBuyerId() != null) {
            try {
                UserProfile buyer = em.find(UserProfile.class, order.getBuyerId());
                if (buyer != null && buyer.getDepartment() != null) {
                    boolean budgetAvailable = checkBudgetAvailability(
                            buyer.getDepartment().getId(), totalAmount);
                    if (!budgetAvailable) {
                        throw new BusinessException("BDG_001",
                                "予算が不足しています。必要額: " + totalAmount);
                    }
                }
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                // 技術的負債 #7: Exception一括キャッチ → ログだけ出して続行
                logger.log(Level.WARNING, "予算チェックでエラーが発生しましたが、処理を続行します。", e);
            }
        }

        // 永続化
        em.persist(order);
        em.flush();

        // ステータス履歴の記録
        recordStatusHistory(order.getId(), null, "DRAFT", "発注作成");

        // 監査ログ
        try {
            auditService.logAction("PurchaseOrder", order.getId(), "CREATE",
                    getCurrentUser(), null, order.toString());
        } catch (Exception e) {
            // 技術的負債 #7: 監査ログ失敗を握りつぶし
            logger.warning("監査ログの記録に失敗しました: " + e.getMessage());
        }

        // 自動承認判定
        // 技術的負債 #4: 承認閾値のハードコード
        if (totalAmount.compareTo(new BigDecimal("50000")) <= 0) {
            // 5万円以下は自動承認
            order.setStatus("APPROVED");
            updateStatus(order.getId(), "APPROVED");
            logger.info("少額のため自動承認されました。金額: " + totalAmount);
        }

        logger.info("発注が作成されました。発注番号: " + poNumber);
        return order;
    }

    /**
     * 発注IDで発注を検索する。
     *
     * <p>【技術的負債 #12】エンティティを直接返却しており、DTOに変換していない。
     * プレゼンテーション層でLazyInitializationExceptionが発生するリスクがある。</p>
     *
     * @param id 発注ID
     * @return 発注エンティティ
     * @throws EntityNotFoundException 発注が見つからない場合
     */
    public PurchaseOrder findOrderById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("発注IDがnullです。");
        }
        List<PurchaseOrder> results = em.createQuery(
                "SELECT po FROM PurchaseOrder po LEFT JOIN FETCH po.supplier LEFT JOIN FETCH po.items WHERE po.id = :id",
                PurchaseOrder.class)
                .setParameter("id", id)
                .getResultList();
        if (results.isEmpty()) {
            throw new EntityNotFoundException("PurchaseOrder", id);
        }
        PurchaseOrder po = results.get(0);
        if (po.getItems() != null) {
            for (PurchaseOrderItem item : po.getItems()) {
                if (item.getProduct() != null) {
                    item.getProduct().getName();
                }
            }
        }
        return po;
    }

    /**
     * 条件を指定して発注一覧を検索する。
     *
     * <p>【技術的負債】ページング処理がDAOと重複している。
     * また、ページ番号の計算ロジックが他のサービスとも重複。</p>
     *
     * @param status ステータス（nullの場合は全ステータス）
     * @param from 検索開始日（nullの場合は制限なし）
     * @param to 検索終了日（nullの場合は制限なし）
     * @param page ページ番号（0始まり）
     * @param size ページサイズ
     * @return 発注エンティティのリスト
     */
    @SuppressWarnings("unchecked")
    public List<PurchaseOrder> findOrders(String status, Date from, Date to, int page, int size) {
        // 技術的負債: ページングのデフォルト値チェックが各所で重複
        if (page < 0) {
            page = 0;
        }
        if (size <= 0 || size > AppConstants.MAX_PAGE_SIZE) {
            size = AppConstants.DEFAULT_PAGE_SIZE;
        }

        // 技術的負債 #11: JPQL文字列連結によるクエリ構築
        StringBuffer jpql = new StringBuffer("SELECT DISTINCT po FROM PurchaseOrder po LEFT JOIN FETCH po.supplier LEFT JOIN FETCH po.items WHERE 1=1");
        if (status != null && !status.trim().isEmpty()) {
            jpql.append(" AND po.status = :status");
        }
        if (from != null) {
            jpql.append(" AND po.orderDate >= :fromDate");
        }
        if (to != null) {
            jpql.append(" AND po.orderDate <= :toDate");
        }
        jpql.append(" ORDER BY po.orderDate DESC");

        Query query = em.createQuery(jpql.toString());
        if (status != null && !status.trim().isEmpty()) {
            query.setParameter("status", status);
        }
        if (from != null) {
            query.setParameter("fromDate", from);
        }
        if (to != null) {
            query.setParameter("toDate", to);
        }

        query.setFirstResult(page * size);
        query.setMaxResults(size);

        List<PurchaseOrder> results = query.getResultList();
        for (PurchaseOrder po : results) {
            if (po.getItems() != null) {
                for (PurchaseOrderItem item : po.getItems()) {
                    if (item.getProduct() != null) {
                        item.getProduct().getName();
                    }
                }
            }
        }
        return results;
    }

    /**
     * 発注を更新する。
     *
     * <p>ステータスがDRAFTの場合のみ更新可能。明細の追加・変更・削除も処理する。</p>
     *
     * @param id 発注ID
     * @param updatedOrder 更新内容を含む発注エンティティ
     * @return 更新後の発注エンティティ
     * @throws EntityNotFoundException 発注が見つからない場合
     * @throws BusinessException 更新不可のステータスの場合
     */
    public PurchaseOrder updateOrder(Long id, PurchaseOrder updatedOrder) {
        PurchaseOrder existing = findOrderById(id);

        // 技術的負債 #14: マジックストリングによるステータスチェック
        if (!"DRAFT".equals(existing.getStatus()) && !"draft".equals(existing.getStatus())) {
            throw new BusinessException("BIZ_001",
                    "DRAFT以外のステータスでは発注を更新できません。現在のステータス: " + existing.getStatus());
        }

        // フィールドの更新
        if (updatedOrder.getExpectedDeliveryDate() != null) {
            existing.setExpectedDeliveryDate(updatedOrder.getExpectedDeliveryDate());
        }
        if (updatedOrder.getShippingMethod() != null) {
            existing.setShippingMethod(updatedOrder.getShippingMethod());
        }
        if (updatedOrder.getNotes() != null) {
            existing.setNotes(updatedOrder.getNotes());
        }

        // サプライヤー変更
        if (updatedOrder.getSupplier() != null && updatedOrder.getSupplier().getId() != null) {
            Supplier newSupplier = em.find(Supplier.class, updatedOrder.getSupplier().getId());
            if (newSupplier == null) {
                throw new EntityNotFoundException("Supplier", updatedOrder.getSupplier().getId());
            }
            existing.setSupplier(newSupplier);
        }

        // 明細の更新
        if (updatedOrder.getItems() != null && !updatedOrder.getItems().isEmpty()) {
            existing.getItems().clear();
            BigDecimal newTotal = BigDecimal.ZERO;
            int lineNum = 1;
            for (int i = 0; i < updatedOrder.getItems().size(); i++) {
                PurchaseOrderItem item = updatedOrder.getItems().get(i);
                // lineNumber field was removed from PurchaseOrderItem (DDL alignment)
                lineNum++;
                item.setPurchaseOrder(existing);
                item.setStatus("PENDING");
                BigDecimal subtotal = calculateItemAmount(item);
                item.setSubtotal(subtotal);
                item.setReceivedQuantity(BigDecimal.ZERO);
                existing.getItems().add(item);
                newTotal = newTotal.add(subtotal);
            }
            existing.setTotalAmount(newTotal);
        }

        existing.setUpdatedBy(getCurrentUser());
        PurchaseOrder merged = em.merge(existing);

        // 監査ログ
        try {
            auditService.logAction("PurchaseOrder", id, "UPDATE",
                    getCurrentUser(), existing.toString(), merged.toString());
        } catch (Exception e) {
            // 技術的負債 #7: 例外握りつぶし
        }

        logger.info("発注が更新されました。発注番号: " + merged.getPoNumber());
        return merged;
    }

    /**
     * 発注を削除する（論理削除）。
     *
     * <p>実際にはステータスをCANCELLEDに変更する論理削除を行う。</p>
     *
     * @param id 発注ID
     * @throws EntityNotFoundException 発注が見つからない場合
     */
    public void deleteOrder(Long id) {
        PurchaseOrder order = findOrderById(id);

        // CANCELLED以外のステータスからのみ削除可能
        if ("CANCELLED".equals(order.getStatus())) {
            throw new BusinessException("BIZ_002", "既にキャンセル済みの発注です。");
        }
        if ("COMPLETED".equals(order.getStatus())) {
            throw new BusinessException("BIZ_003", "完了済みの発注はキャンセルできません。");
        }

        String oldStatus = order.getStatus();
        order.setStatus("CANCELLED");
        order.setUpdatedBy(getCurrentUser());
        em.merge(order);

        recordStatusHistory(id, oldStatus, "CANCELLED", "発注削除（論理削除）");

        // 予算の解放
        try {
            releaseBudget(id);
        } catch (Exception e) {
            logger.warning("予算解放に失敗しました: " + e.getMessage());
        }

        logger.info("発注が論理削除されました。発注番号: " + order.getPoNumber());
    }

    // ========================================================================
    // 承認ワークフロー
    // 技術的負債 #2: RequisitionServiceBeanと重複するロジック
    // ========================================================================

    /**
     * 発注を承認申請する。
     *
     * <p>承認ワークフローと承認ステップを作成し、ステータスをSUBMITTEDに変更する。</p>
     *
     * <p>【技術的負債 #2】このメソッドのロジックはRequisitionServiceBean.submitForApproval()と
     * ほぼ同一。共通の承認サービスに抽出すべき。</p>
     *
     * @param orderId 発注ID
     * @throws ApprovalException 承認申請不可の場合
     */
    public void submitForApproval(Long orderId) {
        PurchaseOrder order = findOrderById(orderId);

        if (!"DRAFT".equals(order.getStatus())) {
            throw new ApprovalException(
                    "DRAFT状態の発注のみ承認申請できます。現在のステータス: " + order.getStatus());
        }

        // 発注金額のバリデーション
        if (order.getTotalAmount() == null || order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("VAL_003", "発注金額が0以下です。");
        }

        // 承認ワークフローの作成
        ApprovalWorkflow workflow = new ApprovalWorkflow();
        workflow.setEntityType("PURCHASE_ORDER");
        workflow.setEntityId(orderId);
        workflow.setStatus("PENDING");
        workflow.setCurrentStep(1);
        workflow.setCreatedDate(new Date());

        // 技術的負債 #4: 承認閾値のハードコード
        BigDecimal amount = order.getTotalAmount();
        List<ApprovalStep> steps = new ArrayList<ApprovalStep>();

        // ステップ1: マネージャー承認（常に必要）
        // approverRole field was removed from ApprovalStep; use comments to note role (DDL alignment)
        ApprovalStep step1 = new ApprovalStep();
        step1.setStepOrder(1);
        step1.setApproverId(0L); // placeholder; actual approver assigned later
        step1.setComments("MANAGER");
        step1.setStatus("PENDING");
        step1.setWorkflow(workflow);
        steps.add(step1);

        // ステップ2: 100万円超の場合はディレクター承認
        if (amount.compareTo(new BigDecimal("1000000")) > 0) {
            ApprovalStep step2 = new ApprovalStep();
            step2.setStepOrder(2);
            step2.setApproverId(0L);
            step2.setComments("DIRECTOR");
            step2.setStatus("PENDING");
            step2.setWorkflow(workflow);
            steps.add(step2);
        }

        // ステップ3: 500万円超の場合はADMIN承認
        if (amount.compareTo(new BigDecimal("5000000")) > 0) {
            ApprovalStep step3 = new ApprovalStep();
            step3.setStepOrder(3);
            step3.setApproverId(0L);
            step3.setComments("ADMIN");
            step3.setStatus("PENDING");
            step3.setWorkflow(workflow);
            steps.add(step3);
        }

        workflow.setSteps(steps);
        em.persist(workflow);

        // ステータス変更
        String oldStatus = order.getStatus();
        order.setStatus("SUBMITTED");
        order.setUpdatedBy(getCurrentUser());
        em.merge(order);

        recordStatusHistory(orderId, oldStatus, "SUBMITTED", "承認申請");

        // 承認者への通知
        try {
            sendApprovalRequestNotification(orderId, null);
        } catch (Exception e) {
            // 技術的負債 #7: 通知失敗を握りつぶし
            logger.warning("承認依頼通知の送信に失敗しました: " + e.getMessage());
        }

        logger.info("発注の承認申請が完了しました。発注番号: " + order.getPoNumber());
    }

    /**
     * 発注を承認する。
     *
     * <p>指定された承認ステップを承認し、全ステップ完了時に発注をAPPROVED状態にする。</p>
     *
     * <p>【技術的負債 #4】承認閾値がハードコードされている。
     * 【技術的負債 #14】ステータス文字列が"APPROVED"と"approved"の両方で比較される箇所がある。</p>
     *
     * @param orderId 発注ID
     * @param stepId 承認ステップID
     * @param comment 承認コメント
     * @throws ApprovalException 承認不可の場合
     */
    @SuppressWarnings("unchecked")
    public void approveOrder(Long orderId, Long stepId, String comment) {
        PurchaseOrder order = findOrderById(orderId);

        // 技術的負債 #14: 大文字小文字の不統一チェック
        if (!"SUBMITTED".equals(order.getStatus()) && !"submitted".equals(order.getStatus())) {
            throw new ApprovalException(
                    "承認待ち状態の発注のみ承認できます。");
        }

        // ワークフロー取得
        List<ApprovalWorkflow> workflows = em.createNamedQuery("ApprovalWorkflow.findByEntity")
                .setParameter("entityType", "PURCHASE_ORDER")
                .setParameter("entityId", orderId)
                .getResultList();

        if (workflows == null || workflows.isEmpty()) {
            throw new ApprovalException("承認ワークフローが見つかりません。");
        }

        ApprovalWorkflow workflow = workflows.get(0);

        // 承認ステップの特定
        ApprovalStep targetStep = null;
        if (stepId != null) {
            targetStep = em.find(ApprovalStep.class, stepId);
        } else {
            // stepIdが指定されない場合は現在のステップを探す
            for (int i = 0; i < workflow.getSteps().size(); i++) {
                ApprovalStep step = workflow.getSteps().get(i);
                if ("PENDING".equals(step.getStatus())) {
                    targetStep = step;
                    break;
                }
            }
        }

        if (targetStep == null) {
            throw new ApprovalException("承認対象のステップが見つかりません。");
        }

        // 承認権限チェック
        String currentUser = getCurrentUser();
        Long currentUserId = getCurrentUserId();

        // 技術的負債 #4: 金額による承認者ロールの判定がハードコード
        BigDecimal orderAmount = order.getTotalAmount();
        // approverRole was removed from ApprovalStep; role info stored in comments (DDL alignment)
        String requiredRole = targetStep.getComments();

        // 承認者のロールチェック（簡易的な実装）
        if (currentUserId != null) {
            UserProfile approver = em.find(UserProfile.class, currentUserId);
            if (approver != null) {
                boolean hasRole = false;
                // 技術的負債: ロールチェックをEntityManager直接クエリで実行
                List<Object[]> roles = em.createNativeQuery(
                        "SELECT r.name FROM user_role_mapping urm " +
                        "JOIN role r ON r.id = urm.role_id " +
                        "WHERE urm.user_id = ?1")
                        .setParameter(1, currentUserId)
                        .getResultList();

                for (int i = 0; i < roles.size(); i++) {
                    // 技術的負債: 配列アクセスのキャストが不完全
                    Object roleRow = roles.get(i);
                    // ロールチェックは暫定的にスキップ（後述のTODO参照）
                }
                // 上記のロールチェックは不完全なので、暫定的にスキップ
                // TODO: 正しいロールチェックを実装する
            }
        }

        // ステップを承認済みに更新
        // approvedBy/approvedAt were removed; use approverId/decidedAt instead (DDL alignment)
        targetStep.setStatus("APPROVED");
        targetStep.setApproverId(currentUserId != null ? currentUserId : 0L);
        targetStep.setDecidedAt(new Date());
        targetStep.setComments(comment);
        em.merge(targetStep);

        // 全ステップの完了チェック
        boolean allApproved = true;
        for (int i = 0; i < workflow.getSteps().size(); i++) {
            ApprovalStep step = workflow.getSteps().get(i);
            if (!"APPROVED".equals(step.getStatus())) {
                allApproved = false;
                break;
            }
        }

        if (allApproved) {
            // 全ステップ承認完了
            workflow.setStatus("APPROVED");
            em.merge(workflow);

            String oldStatus = order.getStatus();
            order.setStatus("APPROVED");
            order.setUpdatedBy(currentUser);
            em.merge(order);

            recordStatusHistory(orderId, oldStatus, "APPROVED", "全ステップ承認完了");

            // 予算の引当
            try {
                reserveBudget(orderId);
            } catch (Exception e) {
                logger.warning("予算引当に失敗しました: " + e.getMessage());
            }

            // 通知
            sendOrderNotification(orderId, "APPROVED");

            logger.info("発注が承認されました。発注番号: " + order.getPoNumber());
        } else {
            // 次のステップへ
            int currentStepOrder = targetStep.getStepOrder();
            workflow.setCurrentStep(currentStepOrder + 1);
            em.merge(workflow);

            logger.info("承認ステップ" + currentStepOrder + "が完了しました。次のステップに進みます。");
        }
    }

    /**
     * 発注を却下する。
     *
     * @param orderId 発注ID
     * @param stepId 承認ステップID
     * @param reason 却下理由
     * @throws ApprovalException 却下不可の場合
     */
    @SuppressWarnings("unchecked")
    public void rejectOrder(Long orderId, Long stepId, String reason) {
        PurchaseOrder order = findOrderById(orderId);

        if (!"SUBMITTED".equals(order.getStatus())) {
            throw new ApprovalException(
                    "承認待ちの発注のみ却下できます。");
        }

        // ワークフロー取得
        List<ApprovalWorkflow> workflows = em.createNamedQuery("ApprovalWorkflow.findByEntity")
                .setParameter("entityType", "PURCHASE_ORDER")
                .setParameter("entityId", orderId)
                .getResultList();

        if (workflows == null || workflows.isEmpty()) {
            throw new ApprovalException("承認ワークフローが見つかりません。");
        }

        ApprovalWorkflow workflow = workflows.get(0);

        // ステップの更新
        ApprovalStep targetStep = null;
        if (stepId != null) {
            targetStep = em.find(ApprovalStep.class, stepId);
        } else {
            for (int i = 0; i < workflow.getSteps().size(); i++) {
                ApprovalStep step = workflow.getSteps().get(i);
                if ("PENDING".equals(step.getStatus())) {
                    targetStep = step;
                    break;
                }
            }
        }

        if (targetStep != null) {
            targetStep.setStatus("REJECTED");
            Long rejectorId = getCurrentUserId();
            targetStep.setApproverId(rejectorId != null ? rejectorId : 0L);
            targetStep.setDecidedAt(new Date());
            targetStep.setComments(reason);
            em.merge(targetStep);
        }

        // ワークフローをREJECTEDに
        workflow.setStatus("REJECTED");
        em.merge(workflow);

        // 発注ステータスの変更
        String oldStatus = order.getStatus();
        order.setStatus("REJECTED");
        order.setUpdatedBy(getCurrentUser());
        em.merge(order);

        recordStatusHistory(orderId, oldStatus, "REJECTED", "却下: " + reason);

        // 通知
        sendOrderNotification(orderId, "REJECTED");

        logger.info("発注が却下されました。発注番号: " + order.getPoNumber() + ", 理由: " + reason);
    }

    /**
     * 承認履歴を取得する。
     *
     * @param orderId 発注ID
     * @return 承認ステップのリスト
     */
    @SuppressWarnings("unchecked")
    public List<ApprovalStep> getApprovalHistory(Long orderId) {
        List<ApprovalWorkflow> workflows = em.createNamedQuery("ApprovalWorkflow.findByEntity")
                .setParameter("entityType", "PURCHASE_ORDER")
                .setParameter("entityId", orderId)
                .getResultList();

        if (workflows == null || workflows.isEmpty()) {
            return new ArrayList<ApprovalStep>();
        }

        return workflows.get(0).getSteps();
    }

    // ========================================================================
    // 入庫処理
    // ========================================================================

    /**
     * 入庫処理を実行する。
     *
     * <p>入庫レコードを作成し、各明細の受入数量を更新する。
     * 全品目の入庫が完了した場合は発注をRECEIVED、
     * 一部入庫の場合はPARTIALLY_RECEIVEDステータスに変更する。</p>
     *
     * <p>【技術的負債 #20】inventoryService.addStock()を呼び出しており、
     * 循環依存が発生している。</p>
     *
     * @param orderId 発注ID
     * @param receipt 入庫情報
     * @return 処理後の入庫エンティティ
     * @throws BusinessException 入庫処理不可の場合
     */
    @SuppressWarnings("unchecked")
    public GoodsReceipt processGoodsReceipt(Long orderId, GoodsReceipt receipt) {
        PurchaseOrder order = findOrderById(orderId);

        // ステータスチェック
        if (!"APPROVED".equals(order.getStatus()) && !"SENT".equals(order.getStatus())
                && !"PARTIALLY_RECEIVED".equals(order.getStatus())) {
            throw new BusinessException("BIZ_004",
                    "入庫処理が可能なステータスではありません。現在のステータス: " + order.getStatus());
        }

        // 入庫番号の自動採番
        String receiptNumber = generateReceiptNumber();
        receipt.setReceiptNumber(receiptNumber);
        receipt.setReceiptDate(new Date());
        receipt.setStatus("ACCEPTED");
        receipt.setPurchaseOrder(order);
        receipt.setCreatedBy(getCurrentUser());
        receipt.setUpdatedBy(getCurrentUser());

        // 入庫明細の処理
        boolean allReceived = true;
        if (receipt.getReceiptItems() != null) {
            for (int i = 0; i < receipt.getReceiptItems().size(); i++) {
                GoodsReceiptItem receiptItem = receipt.getReceiptItems().get(i);
                receiptItem.setGoodsReceipt(receipt);

                // 発注明細の受入数量を更新
                Long poItemId = receiptItem.getPurchaseOrderItemId();
                if (poItemId != null) {
                    PurchaseOrderItem poItem = em.find(PurchaseOrderItem.class, poItemId);
                    if (poItem != null) {
                        BigDecimal prevReceived = poItem.getReceivedQuantity();
                        if (prevReceived == null) {
                            prevReceived = BigDecimal.ZERO;
                        }
                        BigDecimal newReceived = prevReceived.add(
                                new BigDecimal(receiptItem.getReceivedQuantity()));
                        poItem.setReceivedQuantity(newReceived);

                        // 明細ステータスの判定
                        if (newReceived.compareTo(poItem.getQuantity()) >= 0) {
                            poItem.setStatus("RECEIVED");
                        } else {
                            poItem.setStatus("PARTIALLY_RECEIVED");
                            allReceived = false;
                        }
                        em.merge(poItem);

                        // 技術的負債 #20: 循環依存 — 在庫への入庫
                        try {
                            if (poItem.getProduct() != null && receipt.getWarehouseId() != null) {
                                int acceptedQty = receiptItem.getAcceptedQuantity() != null
                                        ? receiptItem.getAcceptedQuantity() : receiptItem.getReceivedQuantity();
                                inventoryService.addStock(
                                        poItem.getProduct().getId(),
                                        receipt.getWarehouseId(),
                                        acceptedQty,
                                        "PURCHASE_ORDER",
                                        orderId);
                            }
                        } catch (Exception e) {
                            // 技術的負債 #7: 在庫更新失敗を握りつぶし
                            logger.log(Level.SEVERE, "在庫更新に失敗しました。品目ID: " + poItemId, e);
                        }
                    }
                }
            }
        }

        // 未入庫の明細があるかチェック
        for (int i = 0; i < order.getItems().size(); i++) {
            PurchaseOrderItem item = order.getItems().get(i);
            if (!"RECEIVED".equals(item.getStatus()) && !"CANCELLED".equals(item.getStatus())) {
                allReceived = false;
            }
        }

        // 発注ステータスの更新
        String oldStatus = order.getStatus();
        if (allReceived) {
            order.setStatus("RECEIVED");
        } else {
            order.setStatus("PARTIALLY_RECEIVED");
        }
        order.setUpdatedBy(getCurrentUser());
        em.merge(order);

        em.persist(receipt);

        recordStatusHistory(orderId, oldStatus, order.getStatus(), "入庫処理: " + receiptNumber);

        // 通知
        sendOrderNotification(orderId, "GOODS_RECEIVED");

        logger.info("入庫処理が完了しました。入庫番号: " + receiptNumber);
        return receipt;
    }

    /**
     * 発注をキャンセルする。
     *
     * @param orderId 発注ID
     * @param reason キャンセル理由
     */
    public void cancelOrder(Long orderId, String reason) {
        PurchaseOrder order = findOrderById(orderId);

        if ("CANCELLED".equals(order.getStatus()) || "COMPLETED".equals(order.getStatus())) {
            throw new BusinessException("BIZ_005",
                    "この発注はキャンセルできません。現在のステータス: " + order.getStatus());
        }

        String oldStatus = order.getStatus();
        order.setStatus("CANCELLED");
        order.setUpdatedBy(getCurrentUser());
        order.setNotes(order.getNotes() != null
                ? order.getNotes() + "\n[キャンセル理由] " + reason : "[キャンセル理由] " + reason);
        em.merge(order);

        recordStatusHistory(orderId, oldStatus, "CANCELLED", "キャンセル: " + reason);

        // 予算解放
        try {
            releaseBudget(orderId);
        } catch (Exception e) {
            // 技術的負債 #7: 空のcatchブロック
        }

        // 通知
        sendOrderNotification(orderId, "CANCELLED");

        logger.info("発注がキャンセルされました。発注番号: " + order.getPoNumber());
    }

    /**
     * 発注をクローズする。
     *
     * @param orderId 発注ID
     */
    public void closeOrder(Long orderId) {
        PurchaseOrder order = findOrderById(orderId);

        if (!"RECEIVED".equals(order.getStatus())) {
            throw new BusinessException("BIZ_006",
                    "受入完了状態の発注のみクローズできます。");
        }

        String oldStatus = order.getStatus();
        order.setStatus("COMPLETED");
        order.setUpdatedBy(getCurrentUser());
        em.merge(order);

        recordStatusHistory(orderId, oldStatus, "COMPLETED", "発注クローズ");

        logger.info("発注がクローズされました。発注番号: " + order.getPoNumber());
    }

    // ========================================================================
    // 金額計算
    // 技術的負債: ユーティリティに移動すべきロジック
    // ========================================================================

    /**
     * 発注の合計金額を計算する。
     *
     * <p>【技術的負債 #4】税率がif/elseでハードコードされている。
     * PriceCalculationHelperも使用しているが、一部はインラインで計算しており不一致。</p>
     *
     * @param order 発注エンティティ
     * @return 合計金額
     */
    public BigDecimal calculateOrderTotal(PurchaseOrder order) {
        if (order == null || order.getItems() == null || order.getItems().isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < order.getItems().size(); i++) {
            PurchaseOrderItem item = order.getItems().get(i);
            BigDecimal itemAmount = calculateItemAmount(item);

            // 技術的負債 #4: 税率のハードコード
            BigDecimal taxRate = item.getTaxRate();
            if (taxRate == null) {
                // デフォルト税率の判定 — ハードコード
                if ("JP".equals(order.getCurrency()) || "JPY".equals(order.getCurrency())) {
                    taxRate = new BigDecimal("0.10"); // 標準税率10%
                } else if ("US".equals(order.getCurrency()) || "USD".equals(order.getCurrency())) {
                    taxRate = new BigDecimal("0.08"); // 仮の税率
                } else {
                    taxRate = new BigDecimal("0.05"); // デフォルト5%
                }
            }

            BigDecimal taxAmount = itemAmount.multiply(taxRate)
                    .setScale(2, RoundingMode.HALF_UP);
            total = total.add(itemAmount).add(taxAmount);
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 発注明細の金額を計算する。
     *
     * @param item 発注明細エンティティ
     * @return 明細金額
     */
    public BigDecimal calculateItemAmount(PurchaseOrderItem item) {
        if (item == null || item.getUnitPrice() == null || item.getQuantity() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal amount = item.getUnitPrice().multiply(item.getQuantity());

        // 割引の適用
        if (item.getDiscount() != null && item.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discountMultiplier = BigDecimal.ONE.subtract(item.getDiscount());
            amount = amount.multiply(discountMultiplier);
        }

        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 発注に割引を適用する。
     *
     * @param order 発注エンティティ
     * @param discountRate 割引率（0.0〜1.0）
     */
    public void applyDiscount(PurchaseOrder order, BigDecimal discountRate) {
        if (order == null || discountRate == null) {
            return;
        }

        if (discountRate.compareTo(BigDecimal.ZERO) < 0 || discountRate.compareTo(BigDecimal.ONE) > 0) {
            throw new ValidationException("VAL_004", "割引率は0〜1の範囲で指定してください。");
        }

        if (order.getItems() != null) {
            for (int i = 0; i < order.getItems().size(); i++) {
                PurchaseOrderItem item = order.getItems().get(i);
                item.setDiscount(discountRate);
                BigDecimal newSubtotal = calculateItemAmount(item);
                item.setSubtotal(newSubtotal);
            }
        }

        // 合計の再計算
        recalculateAllItems(order.getId());
    }

    /**
     * 全明細の金額を再計算する。
     *
     * @param orderId 発注ID
     */
    public void recalculateAllItems(Long orderId) {
        PurchaseOrder order = findOrderById(orderId);
        BigDecimal newTotal = BigDecimal.ZERO;

        if (order.getItems() != null) {
            for (int i = 0; i < order.getItems().size(); i++) {
                PurchaseOrderItem item = order.getItems().get(i);
                BigDecimal subtotal = calculateItemAmount(item);
                item.setSubtotal(subtotal);
                em.merge(item);
                newTotal = newTotal.add(subtotal);
            }
        }

        order.setTotalAmount(newTotal);
        order.setUpdatedBy(getCurrentUser());
        em.merge(order);
    }

    // ========================================================================
    // 予算チェック
    // 技術的負債: BudgetServiceBeanと重複するロジック
    // ========================================================================

    /**
     * 予算の利用可否をチェックする。
     *
     * <p>【技術的負債】BudgetServiceBean.checkAvailability()と重複するロジック。
     * 本来はBudgetServiceBeanに委譲すべきだが、歴史的経緯でここにも実装が残っている。</p>
     *
     * @param departmentId 部門ID
     * @param amount 必要金額
     * @return 予算が利用可能な場合true
     */
    @SuppressWarnings("unchecked")
    public boolean checkBudgetAvailability(Long departmentId, BigDecimal amount) {
        if (departmentId == null || amount == null) {
            return false;
        }

        // 技術的負債: 会計年度のハードコード
        int fiscalYear = DateUtils.getFiscalYear(new Date());

        // 技術的負債: DAOを使わずEntityManager直接クエリ
        List<Budget> budgets = em.createNamedQuery("Budget.findByDepartmentAndYear")
                .setParameter("departmentId", departmentId)
                .setParameter("fiscalYear", fiscalYear)
                .getResultList();

        if (budgets == null || budgets.isEmpty()) {
            // 予算未設定の場合はtrueを返す（技術的負債: 安全でない）
            logger.warning("部門ID: " + departmentId + " の予算が未設定です。予算チェックをスキップします。");
            return true;
        }

        Budget budget = budgets.get(0);
        BigDecimal totalAmount = budget.getTotalAmount();
        BigDecimal spentAmount = budget.getSpentAmount() != null ? budget.getSpentAmount() : BigDecimal.ZERO;
        BigDecimal remaining = totalAmount.subtract(spentAmount);

        return remaining.compareTo(amount) >= 0;
    }

    /**
     * 予算を引き当てる。
     *
     * @param orderId 発注ID
     */
    @SuppressWarnings("unchecked")
    public void reserveBudget(Long orderId) {
        PurchaseOrder order = findOrderById(orderId);
        if (order.getBuyerId() == null) {
            return;
        }

        UserProfile buyer = em.find(UserProfile.class, order.getBuyerId());
        if (buyer == null || buyer.getDepartment() == null) {
            return;
        }

        int fiscalYear = DateUtils.getFiscalYear(new Date());
        List<Budget> budgets = em.createNamedQuery("Budget.findByDepartmentAndYear")
                .setParameter("departmentId", buyer.getDepartment().getId())
                .setParameter("fiscalYear", fiscalYear)
                .getResultList();

        if (budgets != null && !budgets.isEmpty()) {
            Budget budget = budgets.get(0);
            BigDecimal currentSpent = budget.getSpentAmount() != null
                    ? budget.getSpentAmount() : BigDecimal.ZERO;
            budget.setSpentAmount(currentSpent.add(order.getTotalAmount()));
            em.merge(budget);
            logger.info("予算が引き当てられました。金額: " + order.getTotalAmount());
        }
    }

    /**
     * 予算の引当を解放する。
     *
     * @param orderId 発注ID
     */
    @SuppressWarnings("unchecked")
    public void releaseBudget(Long orderId) {
        PurchaseOrder order = findOrderById(orderId);
        if (order.getBuyerId() == null) {
            return;
        }

        UserProfile buyer = em.find(UserProfile.class, order.getBuyerId());
        if (buyer == null || buyer.getDepartment() == null) {
            return;
        }

        int fiscalYear = DateUtils.getFiscalYear(new Date());
        List<Budget> budgets = em.createNamedQuery("Budget.findByDepartmentAndYear")
                .setParameter("departmentId", buyer.getDepartment().getId())
                .setParameter("fiscalYear", fiscalYear)
                .getResultList();

        if (budgets != null && !budgets.isEmpty()) {
            Budget budget = budgets.get(0);
            BigDecimal currentSpent = budget.getSpentAmount() != null
                    ? budget.getSpentAmount() : BigDecimal.ZERO;
            BigDecimal newSpent = currentSpent.subtract(order.getTotalAmount());
            if (newSpent.compareTo(BigDecimal.ZERO) < 0) {
                newSpent = BigDecimal.ZERO;
            }
            budget.setSpentAmount(newSpent);
            em.merge(budget);
            logger.info("予算引当が解放されました。金額: " + order.getTotalAmount());
        }
    }

    // ========================================================================
    // ステータス管理
    // ========================================================================

    /**
     * 発注ステータスを更新する。
     *
     * <p>【技術的負債 #14】ステータスが文字列ベースで、Enumを使用していない。
     * "DRAFT", "SUBMITTED", "APPROVED"等が生の文字列として使われている。</p>
     *
     * @param orderId 発注ID
     * @param newStatus 新しいステータス文字列
     */
    public void updateStatus(Long orderId, String newStatus) {
        PurchaseOrder order = findOrderById(orderId);
        String oldStatus = order.getStatus();

        // 技術的負債: ステータス遷移のバリデーションが不完全
        // 本来はステートマシンパターンを使用すべき
        if ("COMPLETED".equals(oldStatus) || "CANCELLED".equals(oldStatus)) {
            throw new BusinessException("BIZ_007",
                    "完了済みまたはキャンセル済みの発注のステータスは変更できません。");
        }

        order.setStatus(newStatus);
        order.setUpdatedBy(getCurrentUser());
        em.merge(order);

        recordStatusHistory(orderId, oldStatus, newStatus, null);
    }

    /**
     * ステータス変更履歴を記録する。
     *
     * @param orderId 発注ID
     * @param oldStatus 変更前ステータス
     * @param newStatus 変更後ステータス
     * @param comment コメント
     */
    public void recordStatusHistory(Long orderId, String oldStatus, String newStatus, String comment) {
        try {
            PurchaseOrder order = em.find(PurchaseOrder.class, orderId);
            if (order == null) {
                return;
            }

            PurchaseOrderStatusHistory history = new PurchaseOrderStatusHistory();
            history.setFromStatus(oldStatus);
            history.setToStatus(newStatus);
            history.setChangedAt(new Date());
            // changedBy is Long (user ID), not String (DDL alignment)
            history.setChangedBy(getCurrentUserId());
            history.setComments(comment);
            history.setPurchaseOrder(order);

            em.persist(history);
        } catch (Exception e) {
            // 技術的負債 #7: 履歴記録失敗を握りつぶし
            logger.warning("ステータス履歴の記録に失敗しました: " + e.getMessage());
        }
    }

    /**
     * ステータス変更履歴を取得する。
     *
     * <p>【技術的負債 #12】List<Object[]>を返却しており、型安全でない。
     * DTOクラスを定義して返却すべき。</p>
     *
     * @param orderId 発注ID
     * @return ステータス履歴のリスト（各要素は[fromStatus, toStatus, changedAt, changedBy]）
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> getStatusHistory(Long orderId) {
        // 技術的負債: DAOを経由せずEntityManager直接クエリ
        return em.createQuery(
                "SELECT h.fromStatus, h.toStatus, h.changedAt, h.changedBy, h.comments " +
                "FROM PurchaseOrderStatusHistory h WHERE h.purchaseOrder.id = :orderId " +
                "ORDER BY h.changedAt DESC")
                .setParameter("orderId", orderId)
                .getResultList();
    }

    // ========================================================================
    // 検索
    // 技術的負債: DAO層と重複
    // ========================================================================

    /**
     * 発注を検索する。
     *
     * <p>【技術的負債 #11】JPQLを文字列連結で構築しており、SQLインジェクションのリスクは
     * パラメータバインドで回避しているが、コードの可読性が低い。
     * Criteria APIまたはQueryDSLに移行すべき。</p>
     *
     * <p>【技術的負債】PurchaseOrderDao.searchOrders()と重複する実装。</p>
     *
     * @param keyword キーワード（発注番号・備考）
     * @param status ステータス
     * @param supplierId サプライヤーID
     * @param fromDate 開始日
     * @param toDate 終了日
     * @param page ページ番号
     * @param size ページサイズ
     * @return 発注エンティティのリスト
     */
    @SuppressWarnings("unchecked")
    public List<PurchaseOrder> searchOrders(String keyword, String status, Long supplierId,
                                            Date fromDate, Date toDate, int page, int size) {
        // 技術的負債 #11: 文字列連結によるJPQL構築
        StringBuffer jpql = new StringBuffer();
        jpql.append("SELECT DISTINCT po FROM PurchaseOrder po LEFT JOIN FETCH po.supplier LEFT JOIN FETCH po.items WHERE 1=1");

        if (keyword != null && !keyword.trim().isEmpty()) {
            jpql.append(" AND (po.poNumber LIKE :keyword OR po.notes LIKE :keyword)");
        }
        if (status != null && !status.trim().isEmpty()) {
            jpql.append(" AND po.status = :status");
        }
        if (supplierId != null) {
            jpql.append(" AND po.supplier.id = :supplierId");
        }
        if (fromDate != null) {
            jpql.append(" AND po.orderDate >= :fromDate");
        }
        if (toDate != null) {
            jpql.append(" AND po.orderDate <= :toDate");
        }
        jpql.append(" ORDER BY po.orderDate DESC, po.poNumber DESC");

        Query query = em.createQuery(jpql.toString());

        if (keyword != null && !keyword.trim().isEmpty()) {
            query.setParameter("keyword", "%" + keyword.trim() + "%");
        }
        if (status != null && !status.trim().isEmpty()) {
            query.setParameter("status", status);
        }
        if (supplierId != null) {
            query.setParameter("supplierId", supplierId);
        }
        if (fromDate != null) {
            query.setParameter("fromDate", fromDate);
        }
        if (toDate != null) {
            query.setParameter("toDate", toDate);
        }

        // ページング
        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 20;
        query.setFirstResult(page * size);
        query.setMaxResults(size);

        List<PurchaseOrder> results = query.getResultList();
        for (PurchaseOrder po : results) {
            if (po.getItems() != null) {
                for (PurchaseOrderItem item : po.getItems()) {
                    if (item.getProduct() != null) {
                        item.getProduct().getName();
                    }
                }
            }
        }
        return results;
    }

    // ========================================================================
    // サプライヤー関連
    // ========================================================================

    /**
     * 指定商品の優先サプライヤーを取得する。
     *
     * @param productId 商品ID
     * @return サプライヤー情報のリスト
     */
    @SuppressWarnings("unchecked")
    public List<SupplierProduct> getPreferredSuppliers(Long productId) {
        if (productId == null) {
            return new ArrayList<SupplierProduct>();
        }

        List<SupplierProduct> supplierProducts = em.createNamedQuery("SupplierProduct.findByProduct")
                .setParameter("productId", productId)
                .getResultList();

        // rating field was removed from Supplier entity (DDL alignment).
        // Sorting by supplier name as a fallback since rating is no longer a direct field.
        // TODO: Sort by latest SupplierRating.overallScore instead.
        for (int i = 0; i < supplierProducts.size() - 1; i++) {
            for (int j = i + 1; j < supplierProducts.size(); j++) {
                Supplier si = supplierProducts.get(i).getSupplier();
                Supplier sj = supplierProducts.get(j).getSupplier();
                String nameI = si.getName() != null ? si.getName() : "";
                String nameJ = sj.getName() != null ? sj.getName() : "";
                if (nameI.compareTo(nameJ) > 0) {
                    SupplierProduct temp = supplierProducts.get(i);
                    supplierProducts.set(i, supplierProducts.get(j));
                    supplierProducts.set(j, temp);
                }
            }
        }

        return supplierProducts;
    }

    /**
     * サプライヤー間の価格を比較する。
     *
     * <p>【技術的負債 #12】List<Object[]>を返しており、型安全でない。</p>
     *
     * @param productId 商品ID
     * @param supplierIds 比較対象のサプライヤーIDリスト
     * @return 比較結果のリスト（各要素は[supplierId, supplierName, unitCost, leadTimeDays]）
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> compareSupplierPrices(Long productId, List<Long> supplierIds) {
        if (productId == null || supplierIds == null || supplierIds.isEmpty()) {
            return new ArrayList<Object[]>();
        }

        // 技術的負債: N+1クエリパターン
        List<Object[]> results = new ArrayList<Object[]>();
        for (int i = 0; i < supplierIds.size(); i++) {
            Long supplierId = supplierIds.get(i);
            try {
                List<SupplierProduct> sps = em.createQuery(
                        "SELECT sp FROM SupplierProduct sp WHERE sp.product.id = :productId " +
                        "AND sp.supplier.id = :supplierId")
                        .setParameter("productId", productId)
                        .setParameter("supplierId", supplierId)
                        .getResultList();

                if (sps != null && !sps.isEmpty()) {
                    SupplierProduct sp = sps.get(0);
                    Supplier supplier = sp.getSupplier();
                    Object[] row = new Object[]{
                            supplier.getId(),
                            supplier.getName(),
                            sp.getUnitCost(),
                            sp.getLeadTimeDays()
                    };
                    results.add(row);
                }
            } catch (Exception e) {
                // 技術的負債 #7: 個別の取得失敗を握りつぶし
                logger.warning("サプライヤー価格の取得に失敗しました。サプライヤーID: " + supplierId);
            }
        }

        return results;
    }

    // ========================================================================
    // レポート
    // 技術的負債: 別のサービスに分離すべき
    // ========================================================================

    /**
     * 月次の発注サマリーを取得する。
     *
     * <p>ネイティブSQLで集計する。ReportQueryHelperに移動すべき。</p>
     *
     * @param year 年
     * @param month 月
     * @return サマリーデータ（各要素は[status, count, totalAmount]）
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> getOrderSummaryByMonth(int year, int month) {
        // 技術的負債: ネイティブSQLの使用
        String sql = "SELECT po.status, COUNT(*), SUM(po.total_amount) " +
                "FROM purchase_order po " +
                "WHERE EXTRACT(YEAR FROM po.order_date) = ?1 " +
                "AND EXTRACT(MONTH FROM po.order_date) = ?2 " +
                "GROUP BY po.status ORDER BY po.status";

        return em.createNativeQuery(sql)
                .setParameter(1, year)
                .setParameter(2, month)
                .getResultList();
    }

    /**
     * 部門別の支出レポートを取得する。
     *
     * @param from 開始日
     * @param to 終了日
     * @return 部門別支出データ
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> getSpendingByDepartment(Date from, Date to) {
        String sql = "SELECT up.department_id, d.name, SUM(po.total_amount) " +
                "FROM purchase_order po " +
                "JOIN user_profile up ON up.id = po.buyerId " +
                "JOIN department d ON d.id = up.department_id " +
                "WHERE po.order_date BETWEEN ?1 AND ?2 " +
                "AND po.status IN ('APPROVED', 'SENT', 'RECEIVED', 'COMPLETED') " +
                "GROUP BY up.department_id, d.name " +
                "ORDER BY SUM(po.total_amount) DESC";

        return em.createNativeQuery(sql)
                .setParameter(1, from)
                .setParameter(2, to)
                .getResultList();
    }

    /**
     * 支出上位のサプライヤーを取得する。
     *
     * @param limit 取得件数
     * @return サプライヤー別支出データ
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> getTopSuppliersBySpending(int limit) {
        if (limit <= 0) limit = 10;

        String sql = "SELECT s.id, s.name, COUNT(po.id), SUM(po.total_amount) " +
                "FROM purchase_order po " +
                "JOIN supplier s ON s.id = po.supplier_id " +
                "WHERE po.status NOT IN ('CANCELLED', 'DRAFT') " +
                "GROUP BY s.id, s.name " +
                "ORDER BY SUM(po.total_amount) DESC";

        return em.createNativeQuery(sql)
                .setMaxResults(limit)
                .getResultList();
    }

    /**
     * ステータス別の発注件数分布を取得する。
     *
     * @return ステータス分布データ
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> getOrderStatusDistribution() {
        String sql = "SELECT status, COUNT(*) FROM purchase_order GROUP BY status ORDER BY COUNT(*) DESC";
        return em.createNativeQuery(sql).getResultList();
    }

    // ========================================================================
    // 通知
    // 技術的負債: NotificationServiceBeanに移動すべき
    // ========================================================================

    /**
     * 発注に関する通知を送信する。
     *
     * @param orderId 発注ID
     * @param eventType イベント種別
     */
    public void sendOrderNotification(Long orderId, String eventType) {
        try {
            PurchaseOrder order = em.find(PurchaseOrder.class, orderId);
            if (order == null) {
                return;
            }

            String title = "";
            String message = "";

            // 技術的負債: マジックストリングの使用
            if ("APPROVED".equals(eventType)) {
                title = "発注承認完了";
                message = "発注番号 " + order.getPoNumber() + " が承認されました。";
            } else if ("REJECTED".equals(eventType)) {
                title = "発注却下";
                message = "発注番号 " + order.getPoNumber() + " が却下されました。";
            } else if ("GOODS_RECEIVED".equals(eventType)) {
                title = "入庫完了";
                message = "発注番号 " + order.getPoNumber() + " の入庫処理が完了しました。";
            } else if ("CANCELLED".equals(eventType)) {
                title = "発注キャンセル";
                message = "発注番号 " + order.getPoNumber() + " がキャンセルされました。";
            } else {
                title = "発注更新";
                message = "発注番号 " + order.getPoNumber() + " が更新されました。";
            }

            // 購入担当者への通知
            if (order.getBuyerId() != null) {
                notificationService.sendNotification(
                        order.getBuyerId(), title, message, "INFO",
                        "PurchaseOrder", orderId);
            }

        } catch (Exception e) {
            // 技術的負債 #7: 通知失敗を握りつぶし
            logger.warning("通知の送信に失敗しました: " + e.getMessage());
        }
    }

    /**
     * 承認依頼通知を送信する。
     *
     * @param orderId 発注ID
     * @param approverId 承認者ID（nullの場合は自動判定）
     */
    @SuppressWarnings("unchecked")
    public void sendApprovalRequestNotification(Long orderId, Long approverId) {
        try {
            PurchaseOrder order = em.find(PurchaseOrder.class, orderId);
            if (order == null) return;

            String title = "承認依頼: 発注 " + order.getPoNumber();
            String message = "発注番号 " + order.getPoNumber() + " の承認をお願いします。"
                    + " 金額: " + order.getTotalAmount() + " " + order.getCurrency();

            if (approverId != null) {
                notificationService.sendNotification(
                        approverId, title, message, "APPROVAL_REQUEST",
                        "PurchaseOrder", orderId);
            } else {
                // 承認者が未指定の場合、MANAGERロールの全ユーザーに通知
                List<UserProfile> managers = em.createQuery(
                        "SELECT DISTINCT u FROM UserProfile u JOIN u.roles r WHERE r.name = :roleName")
                        .setParameter("roleName", "MANAGER")
                        .getResultList();

                for (int i = 0; i < managers.size(); i++) {
                    UserProfile manager = managers.get(i);
                    notificationService.sendNotification(
                            manager.getId(), title, message, "APPROVAL_REQUEST",
                            "PurchaseOrder", orderId);
                }
            }
        } catch (Exception e) {
            logger.warning("承認依頼通知の送信に失敗しました: " + e.getMessage());
        }
    }

    /**
     * サプライヤーに通知を送信する。
     *
     * <p>【技術的負債 #9】メソッド本体が未実装。TODO状態のまま残っている。</p>
     *
     * @param orderId 発注ID
     */
    public void notifySupplier(Long orderId) {
        // TODO: サプライヤーへの通知機能を実装する
        // メール送信またはEDI連携が必要
        // 2020年のリリースで実装予定だったが、先送りされている
        logger.info("notifySupplier: 未実装。発注ID: " + orderId);
    }

    // ========================================================================
    // CSV出力
    // ========================================================================

    /**
     * 発注をCSV形式でエクスポートする。
     *
     * <p>手動でCSVを構築する。Apache Commons CSVの使用を推奨。</p>
     *
     * @param orderId 発注ID
     * @return CSV文字列
     */
    public String exportOrderToCsv(Long orderId) {
        PurchaseOrder order = findOrderById(orderId);

        // 技術的負債 #6: StringBufferの使用（StringBuilderで十分）
        StringBuffer csv = new StringBuffer();

        // ヘッダー行
        csv.append("発注番号,ステータス,発注日,サプライヤー,合計金額,通貨\n");

        // データ行
        csv.append(escapeCsvField(order.getPoNumber())).append(",");
        csv.append(escapeCsvField(order.getStatus())).append(",");
        csv.append(order.getOrderDate() != null ? DateUtils.formatDate(order.getOrderDate()) : "").append(",");
        csv.append(order.getSupplier() != null ? escapeCsvField(order.getSupplier().getName()) : "").append(",");
        csv.append(order.getTotalAmount() != null ? order.getTotalAmount().toString() : "0").append(",");
        csv.append(order.getCurrency() != null ? order.getCurrency() : "JPY").append("\n");

        // 明細行
        csv.append("\n行番号,商品名,数量,単価,割引率,税率,小計,ステータス\n");
        if (order.getItems() != null) {
            for (int i = 0; i < order.getItems().size(); i++) {
                PurchaseOrderItem item = order.getItems().get(i);
                // lineNumber field was removed from PurchaseOrderItem (DDL alignment)
                csv.append(i + 1).append(",");
                csv.append(item.getProduct() != null ? escapeCsvField(item.getProduct().getName()) : "").append(",");
                csv.append(item.getQuantity()).append(",");
                csv.append(item.getUnitPrice()).append(",");
                csv.append(item.getDiscount() != null ? item.getDiscount() : "0").append(",");
                csv.append(item.getTaxRate() != null ? item.getTaxRate() : "0").append(",");
                csv.append(item.getSubtotal() != null ? item.getSubtotal() : "0").append(",");
                csv.append(item.getStatus() != null ? item.getStatus() : "").append("\n");
            }
        }

        return csv.toString();
    }

    /**
     * 複数の発注をCSVエクスポートする。
     *
     * <p>【技術的負債 #3】N+1パターン。各発注を個別にエクスポートしてループで結合している。
     * バッチ処理に最適化すべき。</p>
     *
     * @param orderIds 発注IDのリスト
     * @return 結合されたCSV文字列
     */
    public String exportOrdersToCsv(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return "";
        }

        StringBuffer allCsv = new StringBuffer();
        // 技術的負債 #3: N+1パターン — 各発注を個別にエクスポート
        for (int i = 0; i < orderIds.size(); i++) {
            try {
                String csv = exportOrderToCsv(orderIds.get(i));
                allCsv.append(csv);
                allCsv.append("\n---\n"); // セパレータ
            } catch (Exception e) {
                // 技術的負債 #7: エラーを握りつぶしてスキップ
                logger.warning("CSV出力に失敗しました。発注ID: " + orderIds.get(i));
            }
        }

        return allCsv.toString();
    }

    // ========================================================================
    // バリデーション
    // ========================================================================

    /**
     * 発注のバリデーションを実行する。
     *
     * <p>【技術的負債 #17】フロントエンドのバリデーションと異なるルールが存在する。
     * 例: FEでは金額上限が1,000,000だが、BEでは999,999.99となっている。</p>
     *
     * @param order 発注エンティティ
     * @throws ValidationException バリデーションエラー時
     */
    public void validateOrder(PurchaseOrder order) {
        if (order == null) {
            throw new ValidationException("VAL_010", "発注データがnullです。");
        }

        // サプライヤーチェック
        if (order.getSupplier() == null) {
            if (order.getSupplier() == null) {
                throw new ValidationException("VAL_011", "サプライヤーが未指定です。");
            }
        }

        // 明細チェック
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new ValidationException("VAL_012", "発注明細が1件以上必要です。");
        }

        // 明細の個別バリデーション
        for (int i = 0; i < order.getItems().size(); i++) {
            PurchaseOrderItem item = order.getItems().get(i);

            if (item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("VAL_013",
                        "明細" + (i + 1) + ": 数量は1以上を指定してください。");
            }

            if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("VAL_014",
                        "明細" + (i + 1) + ": 単価は0より大きい値を指定してください。");
            }

            // 技術的負債 #17: FEとの不一致 — FEは1000000まで、BEは999999.99まで
            BigDecimal maxAmount = new BigDecimal("999999.99");
            BigDecimal itemTotal = item.getUnitPrice().multiply(item.getQuantity());
            if (itemTotal.compareTo(maxAmount) > 0) {
                throw new ValidationException("VAL_015",
                        "明細" + (i + 1) + ": 明細金額が上限（999,999.99）を超えています。");
            }

            // 割引率チェック
            if (item.getDiscount() != null) {
                if (item.getDiscount().compareTo(BigDecimal.ZERO) < 0
                        || item.getDiscount().compareTo(BigDecimal.ONE) > 0) {
                    throw new ValidationException("VAL_016",
                            "明細" + (i + 1) + ": 割引率は0〜1の範囲で指定してください。");
                }
            }

            // 税率チェック
            if (item.getTaxRate() != null) {
                if (item.getTaxRate().compareTo(BigDecimal.ZERO) < 0
                        || item.getTaxRate().compareTo(new BigDecimal("0.30")) > 0) {
                    throw new ValidationException("VAL_017",
                            "明細" + (i + 1) + ": 税率は0〜30%の範囲で指定してください。");
                }
            }
        }

        // 納品予定日チェック
        if (order.getExpectedDeliveryDate() != null) {
            if (order.getOrderDate() != null
                    && order.getExpectedDeliveryDate().before(order.getOrderDate())) {
                throw new ValidationException("VAL_018",
                        "納品予定日は発注日以降の日付を指定してください。");
            }
        }

        // 通貨チェック
        if (order.getCurrency() != null && !order.getCurrency().isEmpty()) {
            // 技術的負債: サポート通貨のハードコード
            String currency = order.getCurrency();
            if (!"JPY".equals(currency) && !"USD".equals(currency)
                    && !"EUR".equals(currency) && !"GBP".equals(currency)
                    && !"CNY".equals(currency)) {
                throw new ValidationException("VAL_019",
                        "サポートされていない通貨です: " + currency);
            }
        }
    }

    // ========================================================================
    // レガシー/デッドメソッド
    // 技術的負債 #9: 使われなくなったメソッドが残存
    // ========================================================================

    /**
     * 発注合計金額を計算する（旧バージョン）。
     *
     * <p>【技術的負債 #9】@Deprecatedだが、ReportQueryHelperから参照されている模様。
     * calculateOrderTotalに置き換えるべきだが、丸め処理が微妙に異なるため
     * 単純な置き換えができない。</p>
     *
     * @param order 発注エンティティ
     * @return 合計金額
     * @deprecated calculateOrderTotal を使用してください。
     */
    @Deprecated
    public BigDecimal calculateOrderTotalLegacy(PurchaseOrder order) {
        if (order == null || order.getItems() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = BigDecimal.ZERO;
        // 技術的負債 #6: for-indexループ
        for (int i = 0; i < order.getItems().size(); i++) {
            PurchaseOrderItem item = order.getItems().get(i);
            if (item.getSubtotal() != null) {
                total = total.add(item.getSubtotal());
            }
        }

        // 技術的負債: HALF_DOWN丸め（calculateOrderTotalではHALF_UP）
        return total.setScale(2, RoundingMode.HALF_DOWN);
    }

    /**
     * 発注処理V1（旧バージョン）。
     *
     * <p>【技術的負債 #9】@Deprecatedで、ロジックの大半がコメントアウトされている。
     * 完全に不要だが、呼び出し元の特定ができていないため削除できていない。</p>
     *
     * @param orderId 発注ID
     * @deprecated 使用しないでください。
     */
    @Deprecated
    public void processOrderV1(Long orderId) {
        // 旧版の発注処理ロジック
        // 2021年のリファクタリングで processGoodsReceipt に移行
        // 以下のコードは参考のためコメントアウトで残す

        /*
        PurchaseOrder order = findOrderById(orderId);
        if (order == null) return;

        order.setStatus("PROCESSING");
        em.merge(order);

        for (PurchaseOrderItem item : order.getItems()) {
            // 在庫への反映（旧方式）
            // inventoryService.addStock(item.getProduct().getId(), 1L,
            //     item.getQuantity().intValue(), "PO", orderId);
        }

        order.setStatus("COMPLETED");
        em.merge(order);
        */

        logger.warning("processOrderV1は非推奨です。processGoodsReceiptを使用してください。");
    }

    /**
     * 発注のレポートデータを取得する。
     *
     * <p>【技術的負債 #12】HashMapを返しており、型安全でない。DTOを使用すべき。</p>
     *
     * @param orderId 発注ID
     * @return レポートデータのMap
     * @deprecated 型安全なDTOを使用したメソッドに移行予定。
     */
    @Deprecated
    public HashMap<String, Object> getOrderReportData(Long orderId) {
        HashMap<String, Object> reportData = new HashMap<String, Object>();

        try {
            PurchaseOrder order = findOrderById(orderId);
            reportData.put("poNumber", order.getPoNumber());
            reportData.put("status", order.getStatus());
            reportData.put("orderDate", order.getOrderDate());
            reportData.put("totalAmount", order.getTotalAmount());
            reportData.put("currency", order.getCurrency());
            reportData.put("supplierName", order.getSupplier() != null ? order.getSupplier().getName() : "");
            reportData.put("itemCount", order.getItems() != null ? order.getItems().size() : 0);

            // 明細データ
            List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
            if (order.getItems() != null) {
                for (int i = 0; i < order.getItems().size(); i++) {
                    PurchaseOrderItem item = order.getItems().get(i);
                    Map<String, Object> itemMap = new HashMap<String, Object>();
                    // lineNumber was removed from PurchaseOrderItem (DDL alignment)
                    itemMap.put("lineNumber", i + 1);
                    itemMap.put("productName", item.getProduct() != null ? item.getProduct().getName() : "");
                    itemMap.put("quantity", item.getQuantity());
                    itemMap.put("unitPrice", item.getUnitPrice());
                    itemMap.put("subtotal", item.getSubtotal());
                    items.add(itemMap);
                }
            }
            reportData.put("items", items);

        } catch (Exception e) {
            // 技術的負債 #7: Exception一括キャッチ → null返却
            logger.log(Level.SEVERE, "レポートデータの取得に失敗しました。", e);
            return null;
        }

        return reportData;
    }

    // ========================================================================
    // プライベートヘルパーメソッド
    // ========================================================================

    /**
     * 発注番号を自動採番する。
     *
     * <p>形式: PO-yyyyMMdd-XXXX（Xは連番）</p>
     *
     * @return 採番された発注番号
     */
    private String generatePoNumber() {
        // 技術的負債 #6: SimpleDateFormatの使用（スレッドセーフでない）
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String dateStr = sdf.format(new Date());

        // 技術的負債: シーケンス取得にネイティブSQLを使用
        try {
            Object result = em.createNativeQuery(
                    "SELECT COALESCE(MAX(CAST(SUBSTRING(po_number, 13) AS INTEGER)), 0) + 1 " +
                    "FROM purchase_order WHERE po_number LIKE ?1")
                    .setParameter(1, AppConstants.PO_NUMBER_PREFIX + "-" + dateStr + "-%")
                    .getSingleResult();
            int seq = result != null ? ((Number) result).intValue() : 1;
            return String.format("%s-%s-%04d", AppConstants.PO_NUMBER_PREFIX, dateStr, seq);
        } catch (Exception e) {
            // フォールバック: タイムスタンプベースの番号
            return AppConstants.PO_NUMBER_PREFIX + "-" + dateStr + "-" +
                    String.format("%04d", (int) (Math.random() * 9999));
        }
    }

    /**
     * 入庫番号を自動採番する。
     *
     * @return 入庫番号
     */
    private String generateReceiptNumber() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String dateStr = sdf.format(new Date());

        try {
            Object result = em.createNativeQuery(
                    "SELECT COALESCE(MAX(CAST(SUBSTRING(receipt_number, 12) AS INTEGER)), 0) + 1 " +
                    "FROM goods_receipt WHERE receipt_number LIKE ?1")
                    .setParameter(1, "GR-" + dateStr + "-%")
                    .getSingleResult();
            int seq = result != null ? ((Number) result).intValue() : 1;
            return String.format("GR-%s-%04d", dateStr, seq);
        } catch (Exception e) {
            return "GR-" + dateStr + "-" + String.format("%04d", (int) (Math.random() * 9999));
        }
    }

    /**
     * 現在のユーザー名を取得する。
     *
     * @return ユーザー名
     */
    private String getCurrentUser() {
        try {
            if (sessionContext != null && sessionContext.getCallerPrincipal() != null) {
                return sessionContext.getCallerPrincipal().getName();
            }
        } catch (Exception e) {
            // 技術的負債 #7: 空のcatchブロック
        }
        return AppConstants.SYSTEM_USER;
    }

    /**
     * 現在のユーザーIDを取得する。
     *
     * @return ユーザーID
     */
    @SuppressWarnings("unchecked")
    private Long getCurrentUserId() {
        String username = getCurrentUser();
        if (AppConstants.SYSTEM_USER.equals(username)) {
            return null;
        }

        try {
            List<UserProfile> users = em.createQuery(
                    "SELECT u FROM UserProfile u WHERE u.keycloakId = :keycloakId")
                    .setParameter("keycloakId", username)
                    .getResultList();
            if (users != null && !users.isEmpty()) {
                return users.get(0).getId();
            }
        } catch (Exception e) {
            // 技術的負債: 握りつぶし
        }
        return null;
    }

    /**
     * CSVフィールドをエスケープする。
     *
     * @param field フィールド値
     * @return エスケープ済みの値
     */
    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}
