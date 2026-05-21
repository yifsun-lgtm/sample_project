package com.proquip.ejb.service;

import com.proquip.common.constant.AppConstants;
import com.proquip.common.exception.EntityNotFoundException;
import com.proquip.common.exception.ValidationException;
import com.proquip.ejb.entity.supplier.Supplier;
import com.proquip.ejb.entity.supplier.SupplierContract;
import com.proquip.ejb.entity.supplier.SupplierRating;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 仕入先管理サービスBean。
 * <p>
 * 仕入先のCRUD操作、評価、契約管理、パフォーマンスレポートを提供する。
 * </p>
 *
 * <p>【技術的負債】
 * <ul>
 *   <li>N+1パターン：評価計算時にサプライヤーごとに個別クエリを実行。</li>
 *   <li>DAO未使用：EntityManagerを直接操作しており、永続化層が分離されていない。</li>
 *   <li>for-indexループ：拡張for文やStream APIを使用すべき。</li>
 *   <li>StringBufferによるCSV構築：Apache Commons CSVやOpenCSVを使うべき。</li>
 * </ul>
 * </p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
@Stateless
public class SupplierServiceBean {

    private static final Logger logger = Logger.getLogger(SupplierServiceBean.class.getName());

    @PersistenceContext
    private EntityManager em;

    @EJB
    private AuditServiceBean auditService;

    @EJB
    private NotificationServiceBean notificationService;

    // ========================================================================
    // CRUD操作
    // ========================================================================

    /**
     * 仕入先を新規作成する。
     *
     * @param supplier 仕入先エンティティ
     * @return 永続化された仕入先
     * @throws ValidationException バリデーションエラー
     */
    public Supplier createSupplier(Supplier supplier) {
        if (supplier == null) {
            throw new ValidationException("supplier", "仕入先情報は必須です。");
        }
        if (supplier.getCode() == null || supplier.getCode().isEmpty()) {
            throw new ValidationException("code", "仕入先コードは必須です。");
        }
        if (supplier.getName() == null || supplier.getName().isEmpty()) {
            throw new ValidationException("name", "仕入先名は必須です。");
        }

        // 仕入先コードの重複チェック
        if (findByCode(supplier.getCode()) != null) {
            throw new ValidationException("code",
                    "仕入先コード「" + supplier.getCode() + "」は既に使用されています。");
        }

        supplier.setStatus("PENDING_APPROVAL");
        supplier.setCreatedBy(AppConstants.SYSTEM_USER);
        supplier.setUpdatedBy(AppConstants.SYSTEM_USER);

        em.persist(supplier);

        // 監査ログ
        auditService.logAction("Supplier", supplier.getId(), "CREATE",
                AppConstants.SYSTEM_USER, null, "仕入先作成: " + supplier.getName());

        logger.info("仕入先を作成しました。コード: " + supplier.getCode());
        return supplier;
    }

    /**
     * 仕入先を更新する。
     *
     * @param supplier 更新する仕入先エンティティ
     * @return 更新後の仕入先
     */
    public Supplier updateSupplier(Supplier supplier) {
        if (supplier == null || supplier.getId() == null) {
            throw new ValidationException("supplier", "仕入先情報とIDは必須です。");
        }

        Supplier existing = em.find(Supplier.class, supplier.getId());
        if (existing == null) {
            throw new EntityNotFoundException("Supplier", supplier.getId());
        }

        // 更新者の設定
        supplier.setUpdatedBy(AppConstants.SYSTEM_USER);
        Supplier merged = em.merge(supplier);

        // 監査ログ
        auditService.logAction("Supplier", merged.getId(), "UPDATE",
                AppConstants.SYSTEM_USER, null, "仕入先更新: " + merged.getName());

        return merged;
    }

    /**
     * IDで仕入先を取得する。
     *
     * @param supplierId 仕入先ID
     * @return 仕入先エンティティ
     * @throws EntityNotFoundException 仕入先が見つからない場合
     */
    public Supplier findById(Long supplierId) {
        if (supplierId == null) {
            throw new ValidationException("supplierId", "仕入先IDは必須です。");
        }

        Supplier supplier = em.find(Supplier.class, supplierId);
        if (supplier == null) {
            throw new EntityNotFoundException("Supplier", supplierId);
        }
        return supplier;
    }

    /**
     * 仕入先コードで仕入先を検索する。
     *
     * @param code 仕入先コード
     * @return 仕入先エンティティ（見つからない場合はnull）
     */
    @SuppressWarnings("unchecked")
    public Supplier findByCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }

        List<Supplier> results = em.createNamedQuery("Supplier.findByCode")
                .setParameter("code", code)
                .getResultList();

        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * ステータスで仕入先を検索する。
     *
     * @param status ステータス
     * @return 仕入先のリスト
     */
    @SuppressWarnings("unchecked")
    public List<Supplier> findByStatus(String status) {
        if (status == null || status.isEmpty()) {
            return new ArrayList<Supplier>();
        }

        return em.createNamedQuery("Supplier.findByStatus")
                .setParameter("status", status)
                .getResultList();
    }

    /**
     * 仕入先を論理削除する（ステータスをINACTIVEに変更）。
     *
     * @param supplierId 仕入先ID
     */
    public void deactivateSupplier(Long supplierId) {
        Supplier supplier = findById(supplierId);
        supplier.setStatus("INACTIVE");
        supplier.setUpdatedBy(AppConstants.SYSTEM_USER);
        em.merge(supplier);

        auditService.logAction("Supplier", supplierId, "UPDATE",
                AppConstants.SYSTEM_USER, "status=ACTIVE", "status=INACTIVE");

        logger.info("仕入先を無効化しました。ID: " + supplierId);
    }

    /**
     * 全仕入先を取得する。
     *
     * <p>【技術的負債】ページネーション未対応。大量データで問題発生の可能性。</p>
     *
     * @return 全仕入先のリスト
     */
    @SuppressWarnings("unchecked")
    public List<Supplier> findAllSuppliers() {
        // 技術的負債: ページネーションなしで全件取得
        return em.createQuery("SELECT s FROM Supplier s ORDER BY s.name")
                .getResultList();
    }

    // ========================================================================
    // 評価管理
    // ========================================================================

    /**
     * 仕入先の評価を登録する。
     *
     * @param supplierId 仕入先ID
     * @param qualityScore 品質スコア（0.00〜5.00）
     * @param deliveryScore 納期スコア（0.00〜5.00）
     * @param priceScore 価格スコア（0.00〜5.00）
     * @param comments コメント
     * @param ratedBy 評価者のユーザーID
     * @return 登録された評価
     */
    public SupplierRating addRating(Long supplierId, BigDecimal qualityScore,
                                    BigDecimal deliveryScore, BigDecimal priceScore,
                                    BigDecimal serviceScore,
                                    String comments, Long ratedBy) {
        Supplier supplier = findById(supplierId);

        // バリデーション
        // 技術的負債 #14: マジックナンバーによるスコア範囲チェック
        BigDecimal maxScore = new BigDecimal("5.00");
        BigDecimal minScore = BigDecimal.ZERO;

        if (qualityScore == null || qualityScore.compareTo(minScore) < 0
                || qualityScore.compareTo(maxScore) > 0) {
            throw new ValidationException("qualityScore",
                    "品質スコアは0.00〜5.00の範囲で指定してください。");
        }
        if (deliveryScore == null || deliveryScore.compareTo(minScore) < 0
                || deliveryScore.compareTo(maxScore) > 0) {
            throw new ValidationException("deliveryScore",
                    "納期スコアは0.00〜5.00の範囲で指定してください。");
        }
        if (priceScore == null || priceScore.compareTo(minScore) < 0
                || priceScore.compareTo(maxScore) > 0) {
            throw new ValidationException("priceScore",
                    "価格スコアは0.00〜5.00の範囲で指定してください。");
        }
        if (serviceScore == null || serviceScore.compareTo(minScore) < 0
                || serviceScore.compareTo(maxScore) > 0) {
            throw new ValidationException("serviceScore",
                    "対応スコアは0.00〜5.00の範囲で指定してください。");
        }

        BigDecimal overallScore = qualityScore.add(deliveryScore).add(priceScore).add(serviceScore)
                .divide(new BigDecimal("4"), 2, RoundingMode.HALF_UP);

        Calendar cal = Calendar.getInstance();
        int quarter = (cal.get(Calendar.MONTH) / 3) + 1;
        String ratingPeriod = cal.get(Calendar.YEAR) + "-Q" + quarter;

        SupplierRating rating = new SupplierRating();
        rating.setSupplier(supplier);
        rating.setQualityScore(qualityScore);
        rating.setDeliveryScore(deliveryScore);
        rating.setPriceScore(priceScore);
        rating.setServiceScore(serviceScore);
        rating.setOverallScore(overallScore);
        rating.setRatingDate(new Date());
        rating.setRatingPeriod(ratingPeriod);
        rating.setComments(comments);
        rating.setRatedBy(ratedBy);

        em.persist(rating);

        // 仕入先の総合評価を更新
        updateSupplierOverallRating(supplierId);

        logger.info("仕入先評価を登録しました。仕入先ID: " + supplierId
                + ", 総合スコア: " + overallScore);

        return rating;
    }

    /**
     * 仕入先の総合評価を更新する。
     *
     * <p>全評価の平均を計算して仕入先の総合評価に反映する。</p>
     *
     * <p>【技術的負債 #3】N+1パターン。評価一覧を全取得してJava側で平均計算している。
     * JPQLの集約関数（AVG）で済む処理。</p>
     *
     * @param supplierId 仕入先ID
     */
    @SuppressWarnings("unchecked")
    private void updateSupplierOverallRating(Long supplierId) {
        // 技術的負債 #3: N+1パターン — 全評価を取得してJava側で計算
        List<SupplierRating> ratings = em.createNamedQuery("SupplierRating.findBySupplier")
                .setParameter("supplierId", supplierId)
                .getResultList();

        if (ratings == null || ratings.isEmpty()) {
            return;
        }

        BigDecimal totalScore = BigDecimal.ZERO;
        // 技術的負債 #6: for-indexループ
        for (int i = 0; i < ratings.size(); i++) {
            totalScore = totalScore.add(ratings.get(i).getOverallScore());
        }

        BigDecimal avgRating = totalScore.divide(
                new BigDecimal(ratings.size()), 2, RoundingMode.HALF_UP);

        // rating field was removed from Supplier entity (DDL alignment).
        // The overall rating is stored in SupplierRating records; no direct field to update.
    }

    /**
     * 仕入先の評価履歴を取得する。
     *
     * @param supplierId 仕入先ID
     * @return 評価のリスト（日付降順）
     */
    @SuppressWarnings("unchecked")
    public List<SupplierRating> getRatingHistory(Long supplierId) {
        if (supplierId == null) {
            return new ArrayList<SupplierRating>();
        }

        return em.createNamedQuery("SupplierRating.findBySupplier")
                .setParameter("supplierId", supplierId)
                .getResultList();
    }

    /**
     * 評価が指定値以上の仕入先を取得する。
     *
     * @param minRating 最低評価値
     * @return 仕入先のリスト
     */
    @SuppressWarnings("unchecked")
    public List<Supplier> findByRatingAbove(BigDecimal minRating) {
        if (minRating == null) {
            minRating = BigDecimal.ZERO;
        }

        return em.createNamedQuery("Supplier.findByRatingAbove")
                .setParameter("minRating", minRating)
                .getResultList();
    }

    // ========================================================================
    // 契約管理
    // ========================================================================

    /**
     * 仕入先の有効な契約を取得する。
     *
     * @param supplierId 仕入先ID
     * @return 契約のリスト
     */
    @SuppressWarnings("unchecked")
    public List<SupplierContract> getActiveContracts(Long supplierId) {
        if (supplierId == null) {
            return new ArrayList<SupplierContract>();
        }

        return em.createQuery(
                "SELECT sc FROM SupplierContract sc " +
                "WHERE sc.supplier.id = :supplierId AND sc.status = 'ACTIVE' " +
                "AND sc.endDate >= :now " +
                "ORDER BY sc.endDate ASC")
                .setParameter("supplierId", supplierId)
                .setParameter("now", new Date())
                .getResultList();
    }

    /**
     * 期限が近い契約を検出する。
     *
     * <p>有効契約のうち、指定日数以内に期限切れとなる契約を返す。</p>
     *
     * @param daysBeforeExpiry 期限切れまでの日数
     * @return 期限が近い契約のリスト
     */
    @SuppressWarnings("unchecked")
    public List<SupplierContract> findExpiringContracts(int daysBeforeExpiry) {
        // 技術的負債: Calendar APIを使用
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DAY_OF_MONTH, daysBeforeExpiry);
        Date expiryDate = cal.getTime();

        return em.createQuery(
                "SELECT sc FROM SupplierContract sc " +
                "WHERE sc.status = 'ACTIVE' " +
                "AND sc.endDate BETWEEN :now AND :expiryDate " +
                "ORDER BY sc.endDate ASC")
                .setParameter("now", new Date())
                .setParameter("expiryDate", expiryDate)
                .getResultList();
    }

    /**
     * 契約番号で契約を取得する。
     *
     * @param contractNumber 契約番号
     * @return 契約エンティティ（見つからない場合はnull）
     */
    @SuppressWarnings("unchecked")
    public SupplierContract findContractByNumber(String contractNumber) {
        if (contractNumber == null || contractNumber.isEmpty()) {
            return null;
        }

        List<SupplierContract> results = em.createNamedQuery("SupplierContract.findByContractNumber")
                .setParameter("contractNumber", contractNumber)
                .getResultList();

        return results.isEmpty() ? null : results.get(0);
    }

    // ========================================================================
    // パフォーマンスレポート
    // ========================================================================

    /**
     * 仕入先のパフォーマンスレポートを生成する。
     *
     * <p>【技術的負債 #3 / #12】N+1パターンかつMap<String, Object>での返却。
     * 専用DTOクラスとバッチクエリに移行すべき。</p>
     *
     * @param supplierId 仕入先ID
     * @return パフォーマンスレポートのMap
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getPerformanceReport(Long supplierId) {
        Map<String, Object> report = new HashMap<String, Object>();

        Supplier supplier = findById(supplierId);
        report.put("supplierName", supplier.getName());
        report.put("supplierCode", supplier.getCode());
        report.put("status", supplier.getStatus());
        // rating field was removed from Supplier entity (DDL alignment)
        report.put("currentRating", null);

        // 技術的負債 #3: N+1クエリ — 個別にクエリを発行
        // 発注件数
        Long orderCount = (Long) em.createQuery(
                "SELECT COUNT(po) FROM PurchaseOrder po WHERE po.supplier.id = :supplierId")
                .setParameter("supplierId", supplierId)
                .getSingleResult();
        report.put("totalOrders", orderCount);

        // 発注金額合計
        Object totalAmountObj = em.createQuery(
                "SELECT SUM(po.totalAmount) FROM PurchaseOrder po " +
                "WHERE po.supplier.id = :supplierId AND po.status != 'CANCELLED'")
                .setParameter("supplierId", supplierId)
                .getSingleResult();
        BigDecimal totalAmount = totalAmountObj != null
                ? (BigDecimal) totalAmountObj : BigDecimal.ZERO;
        report.put("totalAmount", totalAmount);

        // 直近の評価
        List<SupplierRating> recentRatings = em.createNamedQuery("SupplierRating.findLatestBySupplier")
                .setParameter("supplierId", supplierId)
                .setMaxResults(5)
                .getResultList();
        report.put("recentRatings", recentRatings);

        // 有効契約数
        List<SupplierContract> activeContracts = getActiveContracts(supplierId);
        report.put("activeContractCount", activeContracts.size());

        // 完了率
        if (orderCount > 0) {
            Long completedCount = (Long) em.createQuery(
                    "SELECT COUNT(po) FROM PurchaseOrder po " +
                    "WHERE po.supplier.id = :supplierId AND po.status = 'COMPLETED'")
                    .setParameter("supplierId", supplierId)
                    .getSingleResult();
            double completionRate = (double) completedCount / orderCount * 100;
            report.put("completionRate", Math.round(completionRate * 100.0) / 100.0);
        } else {
            report.put("completionRate", 0.0);
        }

        return report;
    }

    /**
     * 仕入先の納品実績を取得する。
     *
     * <p>【技術的負債 #12】List<Object[]>を返却。型安全でない。</p>
     *
     * @param supplierId 仕入先ID
     * @param from 開始日
     * @param to 終了日
     * @return 納品実績データ
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> getDeliveryHistory(Long supplierId, Date from, Date to) {
        if (supplierId == null) {
            return new ArrayList<Object[]>();
        }

        // 技術的負債: ネイティブSQLクエリ
        String sql = "SELECT po.po_number, po.order_date, po.status, po.total_amount " +
                "FROM purchase_order po " +
                "WHERE po.supplier_id = ?1 " +
                "AND po.order_date BETWEEN ?2 AND ?3 " +
                "ORDER BY po.order_date DESC";

        return em.createNativeQuery(sql)
                .setParameter(1, supplierId)
                .setParameter(2, from != null ? from : new Date(0))
                .setParameter(3, to != null ? to : new Date())
                .getResultList();
    }

    /**
     * 仕入先リストをCSV形式で出力する。
     *
     * <p>【技術的負債 #11】StringBufferでCSVを手動構築。</p>
     *
     * @param suppliers 出力対象の仕入先リスト
     * @return CSV文字列
     */
    public String exportSuppliersToCsv(List<Supplier> suppliers) {
        StringBuffer csv = new StringBuffer();

        // ヘッダ行
        csv.append("ID,コード,名称,ステータス,評価,支払条件（日）");
        csv.append("\r\n");

        if (suppliers == null || suppliers.isEmpty()) {
            return csv.toString();
        }

        // 技術的負債 #6: for-indexループ
        for (int i = 0; i < suppliers.size(); i++) {
            Supplier s = suppliers.get(i);
            csv.append(s.getId() != null ? s.getId() : "");
            csv.append(",");
            csv.append(s.getCode() != null ? s.getCode() : "");
            csv.append(",");
            // 技術的負債: CSVエスケープ処理がない
            csv.append(s.getName() != null ? s.getName() : "");
            csv.append(",");
            csv.append(s.getStatus() != null ? s.getStatus() : "");
            csv.append(",");
            // rating and paymentTermDays fields were removed from Supplier (DDL alignment)
            csv.append("");
            csv.append(",");
            csv.append("");
            csv.append("\r\n");
        }

        return csv.toString();
    }
}
