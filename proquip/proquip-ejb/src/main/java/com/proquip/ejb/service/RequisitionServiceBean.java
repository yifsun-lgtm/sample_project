package com.proquip.ejb.service;

import com.proquip.common.constant.AppConstants;
import com.proquip.common.exception.ApprovalException;
import com.proquip.common.exception.EntityNotFoundException;
import com.proquip.common.exception.ValidationException;
import com.proquip.ejb.entity.procurement.PurchaseOrder;
import com.proquip.ejb.entity.procurement.PurchaseOrderItem;
import com.proquip.ejb.entity.procurement.PurchaseRequisition;
import com.proquip.ejb.entity.procurement.PurchaseRequisitionItem;
import com.proquip.ejb.entity.product.Product;
import com.proquip.ejb.entity.supplier.Supplier;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * 購買依頼管理サービスBean。
 * <p>
 * 購買依頼のCRUD操作、承認ワークフロー、発注への変換を提供する。
 * </p>
 *
 * <p>【技術的負債 #2 - コピペ承認ロジック】
 * PurchaseOrderServiceBeanの承認ロジックをほぼそのままコピーしている。
 * 承認ワークフローは共通サービスとして抽出すべきである。
 * 金額閾値（100万円→部長承認、500万円→管理者承認）もPOServiceBeanと
 * 完全に重複しているハードコード値。</p>
 *
 * <p>【その他の技術的負債】
 * <ul>
 *   <li>for-indexループの多用。</li>
 *   <li>マジックストリングによるステータス判定。</li>
 *   <li>バリデーションロジックもPOServiceBeanと重複。</li>
 * </ul>
 * </p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
@Stateless
public class RequisitionServiceBean {

    private static final Logger logger = Logger.getLogger(RequisitionServiceBean.class.getName());

    @PersistenceContext
    private EntityManager em;

    @EJB
    private AuditServiceBean auditService;

    @EJB
    private NotificationServiceBean notificationService;

    @EJB
    private PurchaseOrderServiceBean purchaseOrderService;

    // ========================================================================
    // CRUD操作
    // ========================================================================

    /**
     * 購買依頼を新規作成する。
     *
     * @param requisition 購買依頼エンティティ
     * @return 永続化された購買依頼
     * @throws ValidationException バリデーションエラー
     */
    public PurchaseRequisition createRequisition(PurchaseRequisition requisition) {
        if (requisition == null) {
            throw new ValidationException("requisition", "購買依頼情報は必須です。");
        }

        // バリデーション
        validateRequisition(requisition);

        // 購買依頼番号の採番
        if (requisition.getReqNumber() == null || requisition.getReqNumber().isEmpty()) {
            requisition.setReqNumber(generateReqNumber());
        }

        requisition.setStatus("DRAFT");
        // requestDate field was removed from PurchaseRequisition (DDL alignment)
        requisition.setCreatedBy(AppConstants.SYSTEM_USER);
        requisition.setUpdatedBy(AppConstants.SYSTEM_USER);

        // 明細の関連付け
        if (requisition.getItems() != null) {
            // 技術的負債 #6: for-indexループ
            for (int i = 0; i < requisition.getItems().size(); i++) {
                PurchaseRequisitionItem item = requisition.getItems().get(i);
                item.setRequisition(requisition);
            }
        }

        em.persist(requisition);

        // 監査ログ
        auditService.logAction("PurchaseRequisition", requisition.getId(), "CREATE",
                AppConstants.SYSTEM_USER, null, "購買依頼作成: " + requisition.getReqNumber());

        logger.info("購買依頼を作成しました。依頼番号: " + requisition.getReqNumber());
        return requisition;
    }

    /**
     * 購買依頼を更新する。
     *
     * @param requisition 更新する購買依頼エンティティ
     * @return 更新後の購買依頼
     */
    public PurchaseRequisition updateRequisition(PurchaseRequisition requisition) {
        if (requisition == null || requisition.getId() == null) {
            throw new ValidationException("requisition", "購買依頼情報とIDは必須です。");
        }

        PurchaseRequisition existing = em.find(PurchaseRequisition.class, requisition.getId());
        if (existing == null) {
            throw new EntityNotFoundException("PurchaseRequisition", requisition.getId());
        }

        // DRAFT以外は更新不可
        // 技術的負債 #14: マジックストリング
        if (!"DRAFT".equals(existing.getStatus())) {
            throw new ValidationException("status",
                    "ドラフト状態の購買依頼のみ更新できます。現在のステータス: " + existing.getStatus());
        }

        requisition.setUpdatedBy(AppConstants.SYSTEM_USER);
        PurchaseRequisition merged = em.merge(requisition);

        auditService.logAction("PurchaseRequisition", merged.getId(), "UPDATE",
                AppConstants.SYSTEM_USER, null, "購買依頼更新");

        return merged;
    }

    /**
     * IDで購買依頼を取得する。
     *
     * @param requisitionId 購買依頼ID
     * @return 購買依頼エンティティ
     * @throws EntityNotFoundException 購買依頼が見つからない場合
     */
    public PurchaseRequisition findById(Long requisitionId) {
        if (requisitionId == null) {
            throw new ValidationException("requisitionId", "購買依頼IDは必須です。");
        }

        List<PurchaseRequisition> results = em.createQuery(
                "SELECT r FROM PurchaseRequisition r LEFT JOIN FETCH r.items WHERE r.id = :id",
                PurchaseRequisition.class)
                .setParameter("id", requisitionId)
                .getResultList();
        if (results.isEmpty()) {
            throw new EntityNotFoundException("PurchaseRequisition", requisitionId);
        }
        return results.get(0);
    }

    /**
     * 購買依頼番号で検索する。
     *
     * @param reqNumber 購買依頼番号
     * @return 購買依頼エンティティ（見つからない場合はnull）
     */
    @SuppressWarnings("unchecked")
    public PurchaseRequisition findByReqNumber(String reqNumber) {
        if (reqNumber == null || reqNumber.isEmpty()) {
            return null;
        }

        List<PurchaseRequisition> results = em.createNamedQuery("PurchaseRequisition.findByReqNumber")
                .setParameter("reqNumber", reqNumber)
                .getResultList();

        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * ステータスで購買依頼を検索する。
     *
     * @param status ステータス
     * @return 購買依頼のリスト
     */
    @SuppressWarnings("unchecked")
    public List<PurchaseRequisition> findByStatus(String status) {
        if (status == null || status.isEmpty()) {
            return new ArrayList<PurchaseRequisition>();
        }

        return em.createNamedQuery("PurchaseRequisition.findByStatus")
                .setParameter("status", status)
                .getResultList();
    }

    /**
     * 依頼者で購買依頼を検索する。
     *
     * @param requesterId 依頼者のユーザーID
     * @return 購買依頼のリスト
     */
    @SuppressWarnings("unchecked")
    public List<PurchaseRequisition> findByRequester(Long requesterId) {
        if (requesterId == null) {
            return new ArrayList<PurchaseRequisition>();
        }

        return em.createNamedQuery("PurchaseRequisition.findByRequester")
                .setParameter("requesterId", requesterId)
                .getResultList();
    }

    // ========================================================================
    // 承認ワークフロー
    // ========================================================================

    /**
     * 購買依頼を承認依頼する（DRAFT → SUBMITTED）。
     *
     * @param requisitionId 購買依頼ID
     */
    public void submitForApproval(Long requisitionId) {
        PurchaseRequisition requisition = findById(requisitionId);

        if (!"DRAFT".equals(requisition.getStatus())) {
            throw new ValidationException("status",
                    "ドラフト状態の購買依頼のみ承認依頼できます。");
        }

        // 明細が存在するか確認
        if (requisition.getItems() == null || requisition.getItems().isEmpty()) {
            throw new ValidationException("items", "明細が存在しない購買依頼は承認依頼できません。");
        }

        requisition.setStatus("SUBMITTED");
        requisition.setUpdatedBy(AppConstants.SYSTEM_USER);
        em.merge(requisition);

        // 承認通知の送信
        BigDecimal totalAmount = calculateTotalAmount(requisition);
        String approverRole = determineApproverRole(totalAmount);

        try {
            notificationService.sendNotification(
                    1L, // 技術的負債: 承認者IDがハードコード
                    "購買依頼の承認依頼: " + requisition.getReqNumber(),
                    "購買依頼「" + requisition.getReqNumber() + "」の承認が必要です。"
                            + " 合計金額: " + totalAmount + "円"
                            + " 必要承認権限: " + approverRole,
                    "APPROVAL_REQUEST",
                    "PurchaseRequisition", requisition.getId());
        } catch (Exception e) {
            // 技術的負債 #7: 通知失敗を握りつぶし
            logger.warning("承認依頼通知の送信に失敗しました。");
        }

        auditService.logAction("PurchaseRequisition", requisitionId, "UPDATE",
                AppConstants.SYSTEM_USER, "status=DRAFT", "status=SUBMITTED");

        logger.info("購買依頼を承認依頼しました。依頼番号: " + requisition.getReqNumber());
    }

    /**
     * 購買依頼を承認する（SUBMITTED → APPROVED）。
     *
     * <p>【技術的負債 #2 - コピペ承認ロジック】
     * PurchaseOrderServiceBean.approvePurchaseOrder()とほぼ同一のロジック。
     * 金額閾値による承認権限チェックも完全に重複している。</p>
     *
     * @param requisitionId 購買依頼ID
     * @param approverUserId 承認者のユーザーID
     * @param approverRole 承認者のロール
     */
    public void approveRequisition(Long requisitionId, Long approverUserId, String approverRole) {
        PurchaseRequisition requisition = findById(requisitionId);

        if (!"SUBMITTED".equals(requisition.getStatus())) {
            throw new ApprovalException(
                    "承認依頼状態の購買依頼のみ承認できます。現在のステータス: " + requisition.getStatus());
        }

        // 技術的負債 #2: PurchaseOrderServiceBeanからコピペされた承認ロジック
        // 金額に基づく承認権限チェック
        BigDecimal totalAmount = calculateTotalAmount(requisition);
        String requiredRole = determineApproverRole(totalAmount);

        // 技術的負債 #14: マジックストリングによるロール比較
        if ("ADMIN".equals(requiredRole) && !"ADMIN".equals(approverRole)) {
            throw new ApprovalException(
                    "この購買依頼の承認には管理者権限が必要です。合計金額: " + totalAmount);
        }
        if ("DIRECTOR".equals(requiredRole)
                && !"DIRECTOR".equals(approverRole)
                && !"ADMIN".equals(approverRole)) {
            throw new ApprovalException(
                    "この購買依頼の承認には部長以上の権限が必要です。合計金額: " + totalAmount);
        }

        requisition.setStatus("APPROVED");
        requisition.setUpdatedBy(approverUserId != null ? approverUserId.toString() : AppConstants.SYSTEM_USER);
        em.merge(requisition);

        // 依頼者への承認完了通知
        try {
            notificationService.sendNotification(
                    requisition.getRequesterId(),
                    "購買依頼が承認されました: " + requisition.getReqNumber(),
                    "購買依頼「" + requisition.getReqNumber() + "」が承認されました。",
                    "INFO",
                    "PurchaseRequisition", requisition.getId());
        } catch (Exception e) {
            logger.warning("承認完了通知の送信に失敗しました。");
        }

        auditService.logAction("PurchaseRequisition", requisitionId, "UPDATE",
                approverUserId != null ? approverUserId.toString() : AppConstants.SYSTEM_USER,
                "status=SUBMITTED", "status=APPROVED");

        logger.info("購買依頼を承認しました。依頼番号: " + requisition.getReqNumber()
                + ", 承認者: " + approverUserId);
    }

    /**
     * 購買依頼を却下する（SUBMITTED → REJECTED）。
     *
     * @param requisitionId 購買依頼ID
     * @param rejectorUserId 却下者のユーザーID
     * @param reason 却下理由
     */
    public void rejectRequisition(Long requisitionId, Long rejectorUserId, String reason) {
        PurchaseRequisition requisition = findById(requisitionId);

        if (!"SUBMITTED".equals(requisition.getStatus())) {
            throw new ApprovalException(
                    "承認依頼状態の購買依頼のみ却下できます。現在のステータス: " + requisition.getStatus());
        }

        requisition.setStatus("REJECTED");
        requisition.setUpdatedBy(rejectorUserId != null ? rejectorUserId.toString() : AppConstants.SYSTEM_USER);
        em.merge(requisition);

        // 依頼者への却下通知
        try {
            notificationService.sendNotification(
                    requisition.getRequesterId(),
                    "購買依頼が却下されました: " + requisition.getReqNumber(),
                    "購買依頼「" + requisition.getReqNumber() + "」が却下されました。"
                            + " 理由: " + (reason != null ? reason : "未記入"),
                    "WARNING",
                    "PurchaseRequisition", requisition.getId());
        } catch (Exception e) {
            logger.warning("却下通知の送信に失敗しました。");
        }

        auditService.logAction("PurchaseRequisition", requisitionId, "UPDATE",
                rejectorUserId != null ? rejectorUserId.toString() : AppConstants.SYSTEM_USER,
                "status=SUBMITTED", "status=REJECTED");

        logger.info("購買依頼を却下しました。依頼番号: " + requisition.getReqNumber()
                + ", 理由: " + reason);
    }

    /**
     * 購買依頼をキャンセルする。
     *
     * @param requisitionId 購買依頼ID
     */
    public void cancelRequisition(Long requisitionId) {
        PurchaseRequisition requisition = findById(requisitionId);

        // 技術的負債 #14: マジックストリング
        if ("CONVERTED".equals(requisition.getStatus())
                || "CANCELLED".equals(requisition.getStatus())) {
            throw new ValidationException("status",
                    "変換済みまたはキャンセル済みの購買依頼はキャンセルできません。");
        }

        String oldStatus = requisition.getStatus();
        requisition.setStatus("CANCELLED");
        requisition.setUpdatedBy(AppConstants.SYSTEM_USER);
        em.merge(requisition);

        auditService.logAction("PurchaseRequisition", requisitionId, "UPDATE",
                AppConstants.SYSTEM_USER, "status=" + oldStatus, "status=CANCELLED");

        logger.info("購買依頼をキャンセルしました。依頼番号: " + requisition.getReqNumber());
    }

    // ========================================================================
    // 発注への変換
    // ========================================================================

    /**
     * 承認済み購買依頼を発注に変換する（APPROVED → CONVERTED）。
     *
     * <p>購買依頼の明細情報を元に新しい発注を作成する。
     * 仕入先の指定が必要。</p>
     *
     * <p>【技術的負債 #2】PurchaseOrderServiceBean.createOrder()のバリデーション
     * ロジックと一部重複している。</p>
     *
     * @param requisitionId 購買依頼ID
     * @param supplierId 発注先仕入先ID
     * @return 作成された発注エンティティ
     */
    public PurchaseOrder convertToOrder(Long requisitionId, Long supplierId) {
        PurchaseRequisition requisition = findById(requisitionId);

        if (!"APPROVED".equals(requisition.getStatus())) {
            throw new ValidationException("status",
                    "承認済みの購買依頼のみ発注に変換できます。現在のステータス: " + requisition.getStatus());
        }

        if (supplierId == null) {
            throw new ValidationException("supplierId", "発注先の仕入先IDは必須です。");
        }

        Supplier supplier = em.find(Supplier.class, supplierId);
        if (supplier == null) {
            throw new EntityNotFoundException("Supplier", supplierId);
        }

        // 発注の作成
        PurchaseOrder order = new PurchaseOrder();
        order.setPoNumber(generatePoNumber());
        order.setOrderDate(new Date());
        order.setStatus("DRAFT");
        order.setSupplier(supplier);
        order.setCurrency("JPY");
        order.setNotes("購買依頼「" + requisition.getReqNumber() + "」からの変換");
        order.setCreatedBy(AppConstants.SYSTEM_USER);
        order.setUpdatedBy(AppConstants.SYSTEM_USER);

        // 納品予定日の設定（30日後をデフォルト）
        // 技術的負債: Calendar API使用
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DAY_OF_MONTH, 30);
        order.setExpectedDeliveryDate(cal.getTime());

        // 明細の変換
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<PurchaseRequisitionItem> reqItems = requisition.getItems();

        if (reqItems != null) {
            // 技術的負債 #6: for-indexループ
            for (int i = 0; i < reqItems.size(); i++) {
                PurchaseRequisitionItem reqItem = reqItems.get(i);

                PurchaseOrderItem orderItem = new PurchaseOrderItem();
                orderItem.setPurchaseOrder(order);
                // lineNumber field was removed from PurchaseOrderItem (DDL alignment)
                orderItem.setProduct(reqItem.getProduct());
                orderItem.setQuantity(new BigDecimal(reqItem.getQuantity()));

                // 単価の設定
                BigDecimal unitPrice = reqItem.getEstimatedUnitCost();
                if (unitPrice == null && reqItem.getProduct() != null) {
                    unitPrice = reqItem.getProduct().getUnitPrice();
                }
                if (unitPrice == null) {
                    unitPrice = BigDecimal.ZERO;
                }
                orderItem.setUnitPrice(unitPrice);

                // 明細金額
                BigDecimal lineAmount = unitPrice.multiply(new BigDecimal(reqItem.getQuantity()));
                orderItem.setSubtotal(lineAmount);

                order.getItems().add(orderItem);
                totalAmount = totalAmount.add(lineAmount);
            }
        }

        order.setTotalAmount(totalAmount);

        em.persist(order);

        // 購買依頼のステータス更新
        requisition.setStatus("CONVERTED");
        requisition.setUpdatedBy(AppConstants.SYSTEM_USER);
        em.merge(requisition);

        // 監査ログ
        auditService.logAction("PurchaseRequisition", requisitionId, "UPDATE",
                AppConstants.SYSTEM_USER, "status=APPROVED", "status=CONVERTED");
        auditService.logAction("PurchaseOrder", order.getId(), "CREATE",
                AppConstants.SYSTEM_USER, null,
                "購買依頼「" + requisition.getReqNumber() + "」から変換");

        logger.info("購買依頼を発注に変換しました。依頼番号: " + requisition.getReqNumber()
                + " → 発注番号: " + order.getPoNumber());

        return order;
    }

    // ========================================================================
    // プライベートヘルパーメソッド
    // ========================================================================

    /**
     * 購買依頼のバリデーション。
     *
     * <p>【技術的負債 #2】PurchaseOrderServiceBeanのバリデーションと重複。</p>
     */
    private void validateRequisition(PurchaseRequisition requisition) {
        if (requisition.getRequesterId() == null) {
            throw new ValidationException("requesterId", "依頼者IDは必須です。");
        }
    }

    /**
     * 購買依頼の合計金額を計算する。
     *
     * <p>【技術的負債 #2】PurchaseOrderServiceBean.calculateTotalAmount()と
     * ほぼ同一のロジック。</p>
     */
    private BigDecimal calculateTotalAmount(PurchaseRequisition requisition) {
        BigDecimal total = BigDecimal.ZERO;

        if (requisition.getItems() == null) {
            return total;
        }

        // 技術的負債 #6: for-indexループ
        for (int i = 0; i < requisition.getItems().size(); i++) {
            PurchaseRequisitionItem item = requisition.getItems().get(i);
            BigDecimal unitCost = item.getEstimatedUnitCost();

            if (unitCost == null && item.getProduct() != null) {
                unitCost = item.getProduct().getUnitPrice();
            }

            if (unitCost != null && item.getQuantity() != null) {
                total = total.add(unitCost.multiply(new BigDecimal(item.getQuantity())));
            }
        }

        return total;
    }

    /**
     * 金額に基づく承認権限を判定する。
     *
     * <p>【技術的負債 #2 / #14】PurchaseOrderServiceBeanからのコピペ。
     * ハードコードされた金額閾値。</p>
     */
    private String determineApproverRole(BigDecimal totalAmount) {
        if (totalAmount == null) {
            return "APPROVER";
        }

        // 技術的負債 #14: ハードコードされた閾値
        // PurchaseOrderServiceBeanと完全に重複
        BigDecimal directorThreshold = new BigDecimal("1000000"); // 100万円
        BigDecimal adminThreshold = new BigDecimal("5000000");     // 500万円

        if (totalAmount.compareTo(adminThreshold) >= 0) {
            return "ADMIN";
        } else if (totalAmount.compareTo(directorThreshold) >= 0) {
            return "DIRECTOR";
        } else {
            return "APPROVER";
        }
    }

    /**
     * 購買依頼番号を採番する。
     *
     * <p>【技術的負債】Math.random()による採番。シーケンスやUUIDを使うべき。</p>
     */
    private String generateReqNumber() {
        // 技術的負債: PurchaseOrderServiceBeanの番号採番ロジックのコピペ
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMdd");
        String dateStr = sdf.format(new Date());
        int seq = (int) (Math.random() * 9999);
        return AppConstants.REQ_NUMBER_PREFIX + "-" + dateStr + "-" + String.format("%04d", seq);
    }

    /**
     * 発注番号を採番する。
     *
     * <p>【技術的負債】PurchaseOrderServiceBeanの番号採番ロジックのコピペ。</p>
     */
    private String generatePoNumber() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMdd");
        String dateStr = sdf.format(new Date());
        int seq = (int) (Math.random() * 9999);
        return AppConstants.PO_NUMBER_PREFIX + "-" + dateStr + "-" + String.format("%04d", seq);
    }
}
