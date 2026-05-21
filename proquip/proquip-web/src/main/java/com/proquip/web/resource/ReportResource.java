package com.proquip.web.resource;

import com.proquip.common.constant.AppConstants;
import com.proquip.ejb.service.BudgetServiceBean;
import com.proquip.ejb.service.PurchaseOrderServiceBean;
import com.proquip.ejb.service.SupplierServiceBean;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * レポートRESTリソース。
 *
 * <p>カテゴリ別支出、部門別支出、在庫評価、サプライヤー実績、
 * 予算対実績、発注サマリー、上位サプライヤーなどの各種レポートを提供する。</p>
 *
 * <p>【技術的負債 #12】
 * 各エンドポイントがList&lt;Object[]&gt;やMap&lt;String, Object&gt;で応答を返しており、
 * 型安全なDTOを使用していない。API応答の型がドキュメント化されておらず、
 * フロントエンド側との型定義の乖離リスクが高い。</p>
 *
 * <p>【技術的負債 #5】
 * 一部のレポートでEntityManagerを直接使用してネイティブSQLクエリを発行している。
 * レポート専用のサービスBeanを作成すべき。</p>
 *
 * <p>【技術的負債 #6】
 * 日付パースに{@link SimpleDateFormat}を使用している。</p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
@Path("/reports")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReportResource {

    private static final Logger logger = Logger.getLogger(ReportResource.class.getName());

    @Inject
    private PurchaseOrderServiceBean orderService;

    @Inject
    private SupplierServiceBean supplierService;

    @Inject
    private BudgetServiceBean budgetService;

    /**
     * 技術的負債 #5: レポート用のEntityManager直接注入。
     * ReportServiceBeanを作成すべき。
     */
    @PersistenceContext
    private EntityManager em;

    // ========================================================================
    // 支出レポート
    // ========================================================================

    /**
     * カテゴリ別支出レポートを取得する。
     *
     * <p>【技術的負債 #5】EntityManagerでネイティブSQLクエリを直接発行。</p>
     * <p>【技術的負債 #12】List&lt;Map&gt;で応答を返す。</p>
     *
     * @param fromDate 検索開始日（yyyy-MM-dd形式）
     * @param toDate   検索終了日（yyyy-MM-dd形式）
     * @return カテゴリ別支出データ
     */
    @GET
    @Path("/spending-by-category")
    @SuppressWarnings("unchecked")
    public Response getSpendingByCategory(
            @QueryParam("fromDate") String fromDate,
            @QueryParam("toDate") String toDate) {

        logger.info("カテゴリ別支出レポート取得。from=" + fromDate + ", to=" + toDate);

        // 技術的負債 #5: ネイティブSQLクエリをEntityManagerで直接実行
        String sql = "SELECT c.name AS category_name, COUNT(poi.id) AS item_count, " +
                "COALESCE(SUM(poi.quantity * poi.unit_price), 0) AS total_amount " +
                "FROM purchase_order_item poi " +
                "JOIN purchase_order po ON po.id = poi.order_id " +
                "JOIN product p ON p.id = poi.product_id " +
                "JOIN category c ON c.id = p.category_id " +
                "WHERE po.status IN ('APPROVED', 'SENT', 'RECEIVED', 'COMPLETED') ";

        Date from = parseDate(fromDate);
        Date to = parseDate(toDate);

        if (from != null) {
            sql += "AND po.order_date >= ?1 ";
        }
        if (to != null) {
            sql += "AND po.order_date <= ?2 ";
        }
        sql += "GROUP BY c.name ORDER BY total_amount DESC";

        var query = em.createNativeQuery(sql);
        if (from != null) {
            query.setParameter(1, from);
        }
        if (to != null) {
            query.setParameter(2, to);
        }

        List<Object[]> results = query.getResultList();

        // 技術的負債 #12: Object[]をMapに変換
        List<Map<String, Object>> report = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            Object[] row = results.get(i);
            Map<String, Object> entry = new HashMap<>();
            entry.put("categoryName", row[0]);
            entry.put("itemCount", row[1]);
            entry.put("totalAmount", row[2]);
            report.add(entry);
        }

        return Response.ok(report).build();
    }

    /**
     * 部門別支出レポートを取得する。
     *
     * <p>【技術的負債 #12】List&lt;Map&gt;で応答を返す。</p>
     *
     * @param fromDate 検索開始日
     * @param toDate   検索終了日
     * @return 部門別支出データ
     */
    @GET
    @Path("/spending-by-department")
    public Response getSpendingByDepartment(
            @QueryParam("fromDate") String fromDate,
            @QueryParam("toDate") String toDate) {

        logger.info("部門別支出レポート取得。from=" + fromDate + ", to=" + toDate);

        Date from = parseDate(fromDate);
        Date to = parseDate(toDate);

        if (from == null || to == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "fromDate, toDateは必須です（yyyy-MM-dd形式）。");
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }

        List<Object[]> results = orderService.getSpendingByDepartment(from, to);

        // 技術的負債 #12: Object[]をMapに変換
        List<Map<String, Object>> report = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            Object[] row = results.get(i);
            Map<String, Object> entry = new HashMap<>();
            entry.put("departmentId", row[0]);
            entry.put("departmentName", row[1]);
            entry.put("totalAmount", row[2]);
            report.add(entry);
        }

        return Response.ok(report).build();
    }

    /**
     * 在庫評価レポートを取得する。
     *
     * <p>【技術的負債 #5】EntityManagerでネイティブSQLクエリを直接発行。</p>
     *
     * @param warehouseId 倉庫IDフィルタ
     * @return 在庫評価データ
     */
    @GET
    @Path("/inventory-valuation")
    @SuppressWarnings("unchecked")
    public Response getInventoryValuation(@QueryParam("warehouseId") Long warehouseId) {
        logger.info("在庫評価レポート取得。warehouseId=" + warehouseId);

        // 技術的負債 #5: ネイティブSQLクエリ
        String sql = "SELECT w.name AS warehouse_name, c.name AS category_name, " +
                "COUNT(ii.id) AS item_count, " +
                "COALESCE(SUM(ii.quantity_on_hand), 0) AS total_quantity, " +
                "COALESCE(SUM(ii.quantity_on_hand * p.unit_price), 0) AS total_value " +
                "FROM inventory_item ii " +
                "JOIN warehouse w ON w.id = ii.warehouse_id " +
                "JOIN product p ON p.id = ii.product_id " +
                "LEFT JOIN category c ON c.id = p.category_id " +
                "WHERE 1=1 ";

        if (warehouseId != null) {
            sql += "AND w.id = ?1 ";
        }
        sql += "GROUP BY w.name, c.name ORDER BY w.name, total_value DESC";

        var query = em.createNativeQuery(sql);
        if (warehouseId != null) {
            query.setParameter(1, warehouseId);
        }

        List<Object[]> results = query.getResultList();

        // 技術的負債 #12: Object[]をMapに変換
        List<Map<String, Object>> report = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            Object[] row = results.get(i);
            Map<String, Object> entry = new HashMap<>();
            entry.put("warehouseName", row[0]);
            entry.put("categoryName", row[1]);
            entry.put("itemCount", row[2]);
            entry.put("totalQuantity", row[3]);
            entry.put("totalValue", row[4]);
            report.add(entry);
        }

        return Response.ok(report).build();
    }

    // ========================================================================
    // サプライヤーレポート
    // ========================================================================

    /**
     * サプライヤー実績レポートを取得する。
     *
     * <p>【技術的負債 #12】Map&lt;String, Object&gt;で応答を返す。</p>
     *
     * @param supplierId サプライヤーID
     * @return サプライヤー実績データ
     */
    @GET
    @Path("/supplier-performance")
    public Response getSupplierPerformance(@QueryParam("supplierId") Long supplierId) {
        logger.info("サプライヤー実績レポート取得。supplierId=" + supplierId);

        if (supplierId == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "supplierIdパラメータは必須です。");
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }

        // 技術的負債 #12: サービスがMap<String, Object>を返す
        Map<String, Object> report = supplierService.getPerformanceReport(supplierId);
        return Response.ok(report).build();
    }

    /**
     * 上位サプライヤーレポートを取得する。
     *
     * <p>【技術的負債 #12】List&lt;Map&gt;で応答を返す。</p>
     *
     * @param limit 取得件数
     * @return 上位サプライヤーデータ
     */
    @GET
    @Path("/top-suppliers")
    public Response getTopSuppliers(
            @QueryParam("limit") @DefaultValue("10") int limit) {

        logger.info("上位サプライヤーレポート取得。limit=" + limit);

        List<Object[]> results = orderService.getTopSuppliersBySpending(limit);

        // 技術的負債 #12: Object[]をMapに変換
        List<Map<String, Object>> report = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            Object[] row = results.get(i);
            Map<String, Object> entry = new HashMap<>();
            entry.put("supplierId", row[0]);
            entry.put("supplierName", row[1]);
            entry.put("orderCount", row[2]);
            entry.put("totalSpending", row[3]);
            report.add(entry);
        }

        return Response.ok(report).build();
    }

    // ========================================================================
    // 予算・発注サマリー
    // ========================================================================

    /**
     * 予算対実績レポートを取得する。
     *
     * <p>【技術的負債 #12】Map&lt;String, Object&gt;で応答を返す。</p>
     * <p>【技術的負債 #3】N+1パターン。各予算を個別にサービス呼び出ししている。</p>
     *
     * @param fiscalYear 会計年度
     * @return 予算対実績データ
     */
    @GET
    @Path("/budget-vs-actual")
    public Response getBudgetVsActual(
            @QueryParam("fiscalYear") Integer fiscalYear) {

        logger.info("予算対実績レポート取得。fiscalYear=" + fiscalYear);

        if (fiscalYear == null) {
            fiscalYear = budgetService.getCurrentFiscalYear();
        }

        var budgets = budgetService.findByFiscalYear(fiscalYear);

        // 技術的負債 #3: N+1パターン — 各予算の利用状況を個別に取得
        List<Map<String, Object>> report = new ArrayList<>();
        for (int i = 0; i < budgets.size(); i++) {
            Map<String, Object> utilization = budgetService.getBudgetUtilization(
                    budgets.get(i).getId());
            report.add(utilization);
        }

        return Response.ok(report).build();
    }

    /**
     * 発注月次サマリーを取得する。
     *
     * <p>【技術的負債 #12】List&lt;Map&gt;で応答を返す。</p>
     *
     * @param year  年
     * @param month 月
     * @return 発注月次サマリーデータ
     */
    @GET
    @Path("/order-summary")
    public Response getOrderSummary(
            @QueryParam("year") Integer year,
            @QueryParam("month") Integer month) {

        logger.info("発注月次サマリー取得。year=" + year + ", month=" + month);

        if (year == null || month == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "year, monthパラメータは必須です。");
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }

        List<Object[]> results = orderService.getOrderSummaryByMonth(year, month);

        // 技術的負債 #12: Object[]をMapに変換
        List<Map<String, Object>> report = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            Object[] row = results.get(i);
            Map<String, Object> entry = new HashMap<>();
            entry.put("status", row[0]);
            entry.put("count", row[1]);
            entry.put("totalAmount", row[2]);
            report.add(entry);
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("year", year);
        summary.put("month", month);
        summary.put("details", report);

        return Response.ok(summary).build();
    }

    // ========================================================================
    // 調達サマリー
    // ========================================================================

    @GET
    @Path("/procurement-summary")
    @SuppressWarnings("unchecked")
    public Response getProcurementSummary(
            @QueryParam("startDate") String startDate,
            @QueryParam("endDate") String endDate) {

        logger.info("調達サマリーレポート取得。startDate=" + startDate + ", endDate=" + endDate);

        Date from = parseDate(startDate);
        Date to = parseDate(endDate);

        // ステータス別件数
        String statusSql = "SELECT po.status, COUNT(po.id), COALESCE(SUM(po.total_amount), 0) " +
                "FROM purchase_order po WHERE 1=1 ";
        if (from != null) statusSql += "AND po.order_date >= ?1 ";
        if (to != null) statusSql += "AND po.order_date <= ?2 ";
        statusSql += "GROUP BY po.status ORDER BY po.status";

        var statusQuery = em.createNativeQuery(statusSql);
        if (from != null) statusQuery.setParameter(1, from);
        if (to != null) statusQuery.setParameter(2, to);

        List<Object[]> statusResults = statusQuery.getResultList();

        List<Map<String, Object>> statusBreakdown = new ArrayList<>();
        long totalOrders = 0;
        double totalAmount = 0;
        for (Object[] row : statusResults) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("status", row[0]);
            entry.put("count", row[1]);
            entry.put("amount", row[2]);
            statusBreakdown.add(entry);
            totalOrders += ((Number) row[1]).longValue();
            totalAmount += ((Number) row[2]).doubleValue();
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalOrders", totalOrders);
        summary.put("totalAmount", totalAmount);
        summary.put("statusBreakdown", statusBreakdown);
        summary.put("startDate", startDate);
        summary.put("endDate", endDate);

        Map<String, Object> result = new HashMap<>();
        result.put("data", summary);

        return Response.ok(result).build();
    }

    // ========================================================================
    // ヘルパーメソッド
    // ========================================================================

    /**
     * 日付文字列をDateオブジェクトに変換する。
     *
     * <p>技術的負債 #6: SimpleDateFormatを使用。java.time.LocalDateに移行すべき。</p>
     *
     * @param dateStr 日付文字列（yyyy-MM-dd形式）
     * @return Dateオブジェクト（変換失敗時はnull）
     */
    private Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            // 技術的負債 #6: SimpleDateFormat使用
            SimpleDateFormat sdf = new SimpleDateFormat(AppConstants.ISO_DATE_FORMAT);
            return sdf.parse(dateStr);
        } catch (ParseException e) {
            logger.warning("日付パースエラー: " + dateStr);
            return null;
        }
    }
}
