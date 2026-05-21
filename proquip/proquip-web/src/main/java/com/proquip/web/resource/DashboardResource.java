package com.proquip.web.resource;

import com.proquip.common.dto.DashboardSummaryDTO;
import com.proquip.common.dto.DashboardSummaryDTO.RecentOrderDto;
import com.proquip.ejb.entity.inventory.InventoryItem;
import com.proquip.ejb.entity.procurement.PurchaseOrder;
import com.proquip.ejb.service.InventoryServiceBean;
import com.proquip.ejb.service.PurchaseOrderServiceBean;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ダッシュボードRESTリソース。
 *
 * <p>ダッシュボード画面に必要な集計情報、直近の発注、保留タスク、
 * アラート情報を提供する。</p>
 *
 * <p>【技術的負債 #3】
 * サマリー取得エンドポイントで複数のサービスとEntityManagerを個別呼び出ししており、
 * N+1パターンに該当する。専用のダッシュボードサービスに統合すべき。</p>
 *
 * <p>【技術的負債 #5】
 * EntityManagerを直接使用してカウントクエリを発行している。</p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
@Path("/dashboard")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DashboardResource {

    private static final Logger logger = Logger.getLogger(DashboardResource.class.getName());

    @Inject
    private PurchaseOrderServiceBean orderService;

    @Inject
    private InventoryServiceBean inventoryService;

    /**
     * 技術的負債 #5: RESTリソースにEntityManagerを直接注入。
     * ダッシュボード専用のサービスBeanを作成すべき。
     */
    @PersistenceContext
    private EntityManager em;

    // ========================================================================
    // サマリー
    // ========================================================================

    /**
     * ダッシュボードサマリーを取得する。
     *
     * <p>【技術的負債 #3】N+1パターン。
     * 各集計値を個別にクエリしており、1つの集約クエリにまとめるべき。</p>
     *
     * @return ダッシュボードサマリーDTO
     */
    @GET
    @Path("/summary")
    @SuppressWarnings("unchecked")
    public Response getSummary() {
        logger.info("ダッシュボードサマリー取得。");

        DashboardSummaryDTO summary = new DashboardSummaryDTO();

        try {
            // 技術的負債 #3: N+1パターン — 各カウントを個別クエリで取得
            // 技術的負債 #5: EntityManager直接使用

            // 商品総数
            Long productCount = (Long) em.createQuery(
                    "SELECT COUNT(p) FROM Product p WHERE p.status = 'ACTIVE'")
                    .getSingleResult();
            summary.setTotalProducts(productCount != null ? productCount : 0);

            // 有効仕入先数
            Long supplierCount = (Long) em.createQuery(
                    "SELECT COUNT(s) FROM Supplier s WHERE s.status = 'ACTIVE'")
                    .getSingleResult();
            summary.setActiveSuppliers(supplierCount != null ? supplierCount : 0);

            // 承認待ち発注数
            Long pendingCount = (Long) em.createQuery(
                    "SELECT COUNT(o) FROM PurchaseOrder o WHERE o.status = 'SUBMITTED'")
                    .getSingleResult();
            summary.setPendingOrders(pendingCount != null ? pendingCount : 0);

            // 在庫不足品目数
            List<InventoryItem> lowStock = inventoryService.getLowStockItems(null);
            summary.setLowStockItems(lowStock != null ? lowStock.size() : 0);

            // 承認待ち合計（発注＋購買依頼）
            Long pendingReqCount = (Long) em.createQuery(
                    "SELECT COUNT(r) FROM PurchaseRequisition r WHERE r.status = 'SUBMITTED'")
                    .getSingleResult();
            long totalPending = (pendingCount != null ? pendingCount : 0)
                    + (pendingReqCount != null ? pendingReqCount : 0);
            summary.setPendingApprovals(totalPending);

        } catch (Exception e) {
            // 技術的負債 #7: サマリー取得エラーを握りつぶして空のDTOを返す
            logger.log(Level.WARNING, "ダッシュボードサマリー取得エラー。", e);
        }

        return Response.ok(summary).build();
    }

    /**
     * 直近の発注一覧を取得する。
     *
     * @return 直近10件の発注
     */
    @GET
    @Path("/recent-orders")
    public Response getRecentOrders() {
        logger.info("直近発注取得。");

        List<PurchaseOrder> orders = orderService.findOrders(null, null, null, 0, 10);

        // 技術的負債 #3: N+1パターン — 各発注を個別にDTOに変換
        List<RecentOrderDto> dtoList = new ArrayList<>();
        for (int i = 0; i < orders.size(); i++) {
            PurchaseOrder order = orders.get(i);
            RecentOrderDto dto = new RecentOrderDto();
            dto.setId(order.getId());
            dto.setOrderNumber(order.getPoNumber());
            dto.setTotalAmount(order.getTotalAmount());
            dto.setStatus(order.getStatus());
            dto.setOrderDate(order.getOrderDate() != null ? new Date(order.getOrderDate().getTime()) : null);

            // 技術的負債 #3: サプライヤー名の取得でN+1が発生する可能性
            if (order.getSupplier() != null) {
                dto.setSupplierName(order.getSupplier().getName());
            }

            dtoList.add(dto);
        }

        return Response.ok(dtoList).build();
    }

    /**
     * 保留中のタスク（承認待ち等）を取得する。
     *
     * <p>【技術的負債 #5】EntityManagerで直接クエリを発行している。</p>
     *
     * @param secCtx セキュリティコンテキスト
     * @return 保留タスク一覧
     */
    @GET
    @Path("/pending-tasks")
    @SuppressWarnings("unchecked")
    public Response getPendingTasks(@Context SecurityContext secCtx) {
        logger.info("保留タスク取得。ユーザー=" + secCtx.getUserPrincipal().getName());

        // 技術的負債 #5: EntityManager直接使用
        // 技術的負債: ユーザーごとのフィルタが不完全（全承認待ちを返す）
        List<Map<String, Object>> tasks = new ArrayList<>();

        // 承認待ち発注
        List<PurchaseOrder> pendingOrders = orderService.findOrders(
                "SUBMITTED", null, null, 0, 20);
        for (int i = 0; i < pendingOrders.size(); i++) {
            PurchaseOrder order = pendingOrders.get(i);
            Map<String, Object> task = new HashMap<>();
            task.put("type", "PURCHASE_ORDER_APPROVAL");
            task.put("entityId", order.getId());
            task.put("description", "発注承認: " + order.getPoNumber());
            task.put("createdAt", order.getOrderDate() != null ? new Date(order.getOrderDate().getTime()) : null);
            tasks.add(task);
        }

        // 承認待ち購買依頼
        List<Object[]> pendingReqs = em.createQuery(
                "SELECT r.id, r.reqNumber, r.requiredDate " +
                "FROM PurchaseRequisition r WHERE r.status = 'SUBMITTED' " +
                "ORDER BY r.requiredDate DESC")
                .setMaxResults(20)
                .getResultList();

        for (int i = 0; i < pendingReqs.size(); i++) {
            Object[] row = pendingReqs.get(i);
            Map<String, Object> task = new HashMap<>();
            task.put("type", "REQUISITION_APPROVAL");
            task.put("entityId", row[0]);
            task.put("description", "購買依頼承認: " + row[1]);
            task.put("createdAt", row[2] instanceof java.util.Date ? new Date(((java.util.Date) row[2]).getTime()) : row[2]);
            tasks.add(task);
        }

        return Response.ok(tasks).build();
    }

    /**
     * システムアラートを取得する。
     *
     * <p>在庫不足品目、期限切れ間近の契約などのアラートを集約する。</p>
     *
     * <p>【技術的負債 #3】N+1パターン。各アラートソースを個別にクエリしている。</p>
     *
     * @return アラート一覧
     */
    @GET
    @Path("/alerts")
    public Response getAlerts() {
        logger.info("システムアラート取得。");

        List<Map<String, Object>> alerts = new ArrayList<>();

        // 在庫不足アラート
        try {
            List<InventoryItem> lowStockItems = inventoryService.getLowStockItems(null);
            // 技術的負債 #3: N+1 — 各品目を個別にMapに変換
            for (int i = 0; i < lowStockItems.size(); i++) {
                InventoryItem item = lowStockItems.get(i);
                Map<String, Object> alert = new HashMap<>();
                alert.put("type", "LOW_STOCK");
                alert.put("severity", "WARNING");
                alert.put("message", "在庫不足: "
                        + (item.getProduct() != null ? item.getProduct().getName() : "不明")
                        + " (残: " + item.getQuantityOnHand() + ")");
                alert.put("entityType", "InventoryItem");
                alert.put("entityId", item.getId());
                alerts.add(alert);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "在庫不足アラート取得エラー。", e);
        }

        // 承認待ち超過アラート（承認待ちが5件以上の場合）
        try {
            Long pendingCount = (Long) em.createQuery(
                    "SELECT COUNT(o) FROM PurchaseOrder o WHERE o.status = 'SUBMITTED'")
                    .getSingleResult();
            if (pendingCount != null && pendingCount >= 5) {
                Map<String, Object> alert = new HashMap<>();
                alert.put("type", "PENDING_APPROVALS");
                alert.put("severity", "INFO");
                alert.put("message", "承認待ちの発注が " + pendingCount + " 件あります。");
                alert.put("entityType", "PurchaseOrder");
                alert.put("entityId", null);
                alerts.add(alert);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "承認待ちアラート取得エラー。", e);
        }

        return Response.ok(alerts).build();
    }

    @GET
    @Path("/spending-trend")
    @SuppressWarnings("unchecked")
    public Response getSpendingTrend(
            @QueryParam("months") @DefaultValue("12") int months) {
        logger.info("月別支出推移取得。months=" + months);

        List<Map<String, Object>> trend = new ArrayList<>();
        try {
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT TO_CHAR(o.order_date, 'YYYY-MM') as month, " +
                    "SUM(o.total_amount) as total " +
                    "FROM public.purchase_order o " +
                    "WHERE o.order_date >= CURRENT_DATE - INTERVAL '" + months + " months' " +
                    "AND o.status NOT IN ('CANCELLED', 'DRAFT') " +
                    "GROUP BY TO_CHAR(o.order_date, 'YYYY-MM') " +
                    "ORDER BY 1")
                    .getResultList();

            for (Object[] row : rows) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("month", row[0]);
                item.put("amount", row[1]);
                trend.add(item);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "月別支出推移取得エラー。", e);
        }

        return Response.ok(trend).build();
    }

    @GET
    @Path("/category-spending")
    @SuppressWarnings("unchecked")
    public Response getCategorySpending() {
        logger.info("カテゴリ別支出取得。");

        List<Map<String, Object>> categories = new ArrayList<>();
        try {
            List<Object[]> rows = em.createQuery(
                    "SELECT c.name, COUNT(DISTINCT o.id), SUM(o.totalAmount) " +
                    "FROM PurchaseOrder o " +
                    "JOIN o.items oi " +
                    "JOIN oi.product p " +
                    "JOIN p.category c " +
                    "WHERE o.status NOT IN ('CANCELLED', 'DRAFT') " +
                    "GROUP BY c.name " +
                    "ORDER BY SUM(o.totalAmount) DESC")
                    .getResultList();

            for (Object[] row : rows) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("categoryName", row[0]);
                item.put("orderCount", row[1]);
                item.put("totalAmount", row[2]);
                categories.add(item);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "カテゴリ別支出取得エラー。", e);
        }

        return Response.ok(categories).build();
    }
}
