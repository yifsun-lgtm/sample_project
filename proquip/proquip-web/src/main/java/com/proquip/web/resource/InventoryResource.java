package com.proquip.web.resource;

import com.proquip.common.constant.AppConstants;
import com.proquip.common.dto.InventoryItemDto;
import com.proquip.common.dto.PageResultDto;
import com.proquip.ejb.mapper.InventoryMapper;
import com.proquip.ejb.entity.inventory.InventoryCount;
import com.proquip.ejb.entity.inventory.InventoryItem;
import com.proquip.ejb.service.InventoryServiceBean;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import com.proquip.ejb.entity.inventory.StockTransfer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 在庫管理RESTリソース。
 *
 * <p>在庫品目の参照、入出庫、移動、棚卸、在庫分析を提供する。</p>
 *
 * <p>【技術的負債 #5】
 * 一部のメソッドで {@link EntityManager} を直接使用し、
 * サービス層をバイパスしている。</p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
@Path("/inventory")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InventoryResource {

    private static final Logger logger = Logger.getLogger(InventoryResource.class.getName());

    @Inject
    private InventoryServiceBean inventoryService;

    @Inject
    private InventoryMapper inventoryMapper;

    /**
     * 技術的負債 #5: RESTリソースにEntityManagerを直接注入。
     */
    @PersistenceContext
    private EntityManager em;

    // ========================================================================
    // 在庫品目
    // ========================================================================

    /**
     * 在庫品目一覧を取得する。
     *
     * @param page        ページ番号
     * @param size        ページサイズ
     * @param warehouseId 倉庫IDフィルタ
     * @return 在庫品目一覧
     */
    @GET
    @SuppressWarnings("unchecked")
    public Response listInventoryItems(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("warehouseId") Long warehouseId) {

        logger.info("在庫品目一覧取得。page=" + page + ", warehouseId=" + warehouseId);

        if (size > AppConstants.MAX_PAGE_SIZE) {
            size = AppConstants.MAX_PAGE_SIZE;
        }

        // 技術的負債 #5: EntityManagerで直接クエリ
        StringBuffer jpql = new StringBuffer(
                "SELECT i FROM InventoryItem i LEFT JOIN FETCH i.product LEFT JOIN FETCH i.warehouse WHERE 1=1");
        if (warehouseId != null) {
            jpql.append(" AND i.warehouse.id = :warehouseId");
        }
        jpql.append(" ORDER BY i.product.name ASC");

        Query query = em.createQuery(jpql.toString());
        if (warehouseId != null) {
            query.setParameter("warehouseId", warehouseId);
        }
        query.setFirstResult(page * size);
        query.setMaxResults(size);

        List<InventoryItem> items = query.getResultList();

        List<InventoryItemDto> dtoList = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            dtoList.add(inventoryMapper.toDto(items.get(i)));
        }

        PageResultDto<InventoryItemDto> result = new PageResultDto<>(dtoList, dtoList.size(), page, size);
        return Response.ok(result).build();
    }

    /**
     * 在庫品目詳細を取得する。
     *
     * @param id 在庫品目ID
     * @return 在庫品目DTO
     */
    @GET
    @Path("/{id}")
    public Response getInventoryItem(@PathParam("id") Long id) {
        logger.info("在庫品目詳細取得。ID=" + id);

        List<InventoryItem> items = em.createQuery(
                "SELECT i FROM InventoryItem i LEFT JOIN FETCH i.product LEFT JOIN FETCH i.warehouse WHERE i.id = :id",
                InventoryItem.class)
                .setParameter("id", id)
                .getResultList();
        if (items.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"在庫品目が見つかりません。ID: " + id + "\"}")
                    .build();
        }
        InventoryItemDto dto = inventoryMapper.toDto(items.get(0));

        return Response.ok(dto).build();
    }

    /**
     * 製品別の在庫サマリーを取得する（全倉庫の在庫状況）。
     *
     * @param productId 製品ID
     * @return 在庫品目一覧
     */
    @GET
    @Path("/product/{productId}")
    @SuppressWarnings("unchecked")
    public Response getProductInventorySummary(@PathParam("productId") Long productId) {
        logger.info("製品別在庫サマリー取得。productId=" + productId);

        List<InventoryItem> items = em.createQuery(
                "SELECT i FROM InventoryItem i LEFT JOIN FETCH i.product LEFT JOIN FETCH i.warehouse WHERE i.product.id = :productId",
                InventoryItem.class)
                .setParameter("productId", productId)
                .getResultList();

        List<InventoryItemDto> dtoList = new ArrayList<>();
        for (InventoryItem item : items) {
            dtoList.add(inventoryMapper.toDto(item));
        }

        return Response.ok(dtoList).build();
    }

    // ========================================================================
    // 在庫レベル
    // ========================================================================

    /**
     * 在庫レベルを取得する。
     *
     * @param productId   製品ID
     * @param warehouseId 倉庫ID
     * @return 在庫レベル情報
     */
    @GET
    @Path("/stock-levels")
    @SuppressWarnings("unchecked")
    public Response getStockLevels(
            @QueryParam("productId") Long productId,
            @QueryParam("warehouseId") Long warehouseId) {

        logger.info("在庫レベル取得。productId=" + productId + ", warehouseId=" + warehouseId);

        // 技術的負債 #5: EntityManager直接使用
        StringBuffer jpql = new StringBuffer(
                "SELECT i FROM InventoryItem i LEFT JOIN FETCH i.product LEFT JOIN FETCH i.warehouse WHERE 1=1");
        if (productId != null) {
            jpql.append(" AND i.product.id = :productId");
        }
        if (warehouseId != null) {
            jpql.append(" AND i.warehouse.id = :warehouseId");
        }

        Query query = em.createQuery(jpql.toString());
        if (productId != null) {
            query.setParameter("productId", productId);
        }
        if (warehouseId != null) {
            query.setParameter("warehouseId", warehouseId);
        }

        List<InventoryItem> items = query.getResultList();

        List<InventoryItemDto> dtoList = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            dtoList.add(inventoryMapper.toDto(items.get(i)));
        }

        return Response.ok(dtoList).build();
    }

    // ========================================================================
    // 入出庫・移動
    // ========================================================================

    /**
     * 入庫処理を実行する。
     *
     * @param stockData 入庫データ（productId, warehouseId, quantity, referenceType, referenceId）
     * @param secCtx    セキュリティコンテキスト
     * @return 処理結果
     */
    @POST
    @Path("/stock-in")
    public Response stockIn(Map<String, Object> stockData, @Context SecurityContext secCtx) {
        logger.info("入庫処理。ユーザー=" + secCtx.getUserPrincipal().getName());

        Long productId = Long.parseLong(stockData.get("productId").toString());
        Long warehouseId = Long.parseLong(stockData.get("warehouseId").toString());
        int quantity = Integer.parseInt(stockData.get("quantity").toString());
        String referenceType = stockData.get("referenceType") != null
                ? stockData.get("referenceType").toString() : "ADJUSTMENT";
        Long referenceId = stockData.get("referenceId") != null
                ? Long.parseLong(stockData.get("referenceId").toString()) : null;

        inventoryService.addStock(productId, warehouseId, quantity, referenceType, referenceId);

        Map<String, String> result = new HashMap<>();
        result.put("message", "入庫処理が完了しました。");
        return Response.ok(result).build();
    }

    /**
     * 出庫処理を実行する。
     *
     * @param stockData 出庫データ（productId, warehouseId, quantity, referenceType, referenceId）
     * @param secCtx    セキュリティコンテキスト
     * @return 処理結果
     */
    @POST
    @Path("/stock-out")
    public Response stockOut(Map<String, Object> stockData, @Context SecurityContext secCtx) {
        logger.info("出庫処理。ユーザー=" + secCtx.getUserPrincipal().getName());

        Long productId = Long.parseLong(stockData.get("productId").toString());
        Long warehouseId = Long.parseLong(stockData.get("warehouseId").toString());
        int quantity = Integer.parseInt(stockData.get("quantity").toString());
        String referenceType = stockData.get("referenceType") != null
                ? stockData.get("referenceType").toString() : "ADJUSTMENT";
        Long referenceId = stockData.get("referenceId") != null
                ? Long.parseLong(stockData.get("referenceId").toString()) : null;

        inventoryService.removeStock(productId, warehouseId, quantity, referenceType);

        Map<String, String> result = new HashMap<>();
        result.put("message", "出庫処理が完了しました。");
        return Response.ok(result).build();
    }

    /**
     * 在庫移動を実行する。
     *
     * @param transferData 移動データ（productId, fromWarehouseId, toWarehouseId, quantity）
     * @param secCtx       セキュリティコンテキスト
     * @return 処理結果
     */
    @POST
    @Path("/transfer")
    public Response transferStock(Map<String, Object> transferData,
                                  @Context SecurityContext secCtx) {
        logger.info("在庫移動。ユーザー=" + secCtx.getUserPrincipal().getName());

        Long productId = Long.parseLong(transferData.get("productId").toString());
        Long fromWarehouseId = Long.parseLong(transferData.get("fromWarehouseId").toString());
        Long toWarehouseId = Long.parseLong(transferData.get("toWarehouseId").toString());
        int quantity = Integer.parseInt(transferData.get("quantity").toString());

        inventoryService.transferStock(fromWarehouseId, toWarehouseId, productId, quantity);

        Map<String, String> result = new HashMap<>();
        result.put("message", "在庫移動が完了しました。");
        return Response.ok(result).build();
    }

    // ========================================================================
    // 在庫移動 (transfers)
    // ========================================================================

    @GET
    @Path("/transfers")
    @SuppressWarnings("unchecked")
    public Response listTransfers(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {

        logger.info("在庫移動一覧取得。page=" + page);

        List<StockTransfer> transfers = em.createQuery(
                "SELECT st FROM StockTransfer st ORDER BY st.requestDate DESC")
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();

        List<Map<String, Object>> dtoList = new ArrayList<>();
        for (StockTransfer st : transfers) {
            dtoList.add(transferToMap(st));
        }

        long total = (Long) em.createQuery("SELECT COUNT(st) FROM StockTransfer st").getSingleResult();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", dtoList);
        result.put("totalElements", total);
        result.put("page", page);
        result.put("size", size);
        return Response.ok(result).build();
    }

    @POST
    @Path("/transfers")
    public Response createTransfer(Map<String, Object> body,
                                   @Context SecurityContext secCtx) {
        logger.info("在庫移動作成。ユーザー=" + secCtx.getUserPrincipal().getName());

        Long productId = Long.parseLong(body.get("productId").toString());
        Long fromWarehouseId = Long.parseLong(body.get("fromWarehouseId").toString());
        Long toWarehouseId = Long.parseLong(body.get("toWarehouseId").toString());
        int quantity = Integer.parseInt(body.get("quantity").toString());

        String transferNumber = "ST-" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
                + "-" + String.format("%04d", new Random().nextInt(10000));

        StockTransfer transfer = new StockTransfer();
        transfer.setTransferNumber(transferNumber);
        transfer.setStatus("REQUESTED");
        transfer.setRequestDate(new Date());
        transfer.setSourceWarehouseId(fromWarehouseId);
        transfer.setDestinationWarehouseId(toWarehouseId);
        em.persist(transfer);

        inventoryService.transferStock(fromWarehouseId, toWarehouseId, productId, quantity);

        return Response.status(Response.Status.CREATED).entity(transferToMap(transfer)).build();
    }

    @POST
    @Path("/transfers/{id}/complete")
    public Response completeTransfer(@PathParam("id") Long id,
                                     @Context SecurityContext secCtx) {
        logger.info("在庫移動完了。ID=" + id + ", ユーザー=" + secCtx.getUserPrincipal().getName());

        StockTransfer transfer = em.find(StockTransfer.class, id);
        if (transfer == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"在庫移動が見つかりません。ID: " + id + "\"}")
                    .build();
        }

        transfer.setStatus("COMPLETED");
        transfer.setCompletedDate(new Date());
        em.merge(transfer);

        return Response.ok(transferToMap(transfer)).build();
    }

    private Map<String, Object> transferToMap(StockTransfer st) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", st.getId());
        map.put("transferNumber", st.getTransferNumber());
        map.put("status", st.getStatus());
        map.put("sourceWarehouseId", st.getSourceWarehouseId());
        map.put("destinationWarehouseId", st.getDestinationWarehouseId());
        map.put("requestDate", st.getRequestDate() != null
                ? new java.util.Date(st.getRequestDate().getTime()) : null);
        map.put("completedDate", st.getCompletedDate() != null
                ? new java.util.Date(st.getCompletedDate().getTime()) : null);
        return map;
    }

    // ========================================================================
    // 在庫調整
    // ========================================================================

    @POST
    @Path("/{id}/adjust")
    public Response adjustStock(@PathParam("id") Long id,
                                Map<String, Object> body,
                                @Context SecurityContext secCtx) {
        logger.info("在庫調整。ID=" + id + ", ユーザー=" + secCtx.getUserPrincipal().getName());

        List<InventoryItem> found = em.createQuery(
                "SELECT i FROM InventoryItem i LEFT JOIN FETCH i.product LEFT JOIN FETCH i.warehouse WHERE i.id = :id",
                InventoryItem.class)
                .setParameter("id", id)
                .getResultList();
        if (found.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"在庫品目が見つかりません。ID: " + id + "\"}")
                    .build();
        }
        InventoryItem item = found.get(0);

        int quantity = Integer.parseInt(body.get("quantity").toString());
        String reason = body.get("reason") != null ? body.get("reason").toString() : "ADJUSTMENT";

        if (quantity > 0) {
            inventoryService.addStock(
                    item.getProduct().getId(), item.getWarehouse().getId(),
                    quantity, "ADJUSTMENT", null);
        } else if (quantity < 0) {
            inventoryService.removeStock(
                    item.getProduct().getId(), item.getWarehouse().getId(),
                    Math.abs(quantity), reason);
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"数量は0以外を指定してください。\"}")
                    .build();
        }

        List<InventoryItem> updatedList = em.createQuery(
                "SELECT i FROM InventoryItem i LEFT JOIN FETCH i.product LEFT JOIN FETCH i.warehouse WHERE i.id = :id",
                InventoryItem.class)
                .setParameter("id", id)
                .getResultList();
        InventoryItemDto dto = inventoryMapper.toDto(updatedList.get(0));
        return Response.ok(dto).build();
    }

    // ========================================================================
    // 在庫アラート・分析
    // ========================================================================

    /**
     * 在庫アラート（在庫不足品目）を取得する。
     * フロントエンドの /alerts/low-stock パスに対応。
     *
     * @return 在庫不足品目一覧
     */
    @GET
    @Path("/alerts/low-stock")
    public Response getLowStockAlerts() {
        return getLowStockItems();
    }

    /**
     * 在庫不足品目を取得する。
     *
     * @return 在庫不足品目一覧
     */
    @GET
    @Path("/low-stock")
    public Response getLowStockItems() {
        logger.info("在庫不足品目取得。");

        // 技術的負債: warehouseId=nullで全倉庫の在庫不足を取得
        List<InventoryItem> lowStockItems = inventoryService.getLowStockItems(null);

        List<InventoryItemDto> dtoList = new ArrayList<>();
        for (int i = 0; i < lowStockItems.size(); i++) {
            dtoList.add(inventoryMapper.toDto(lowStockItems.get(i)));
        }

        return Response.ok(dtoList).build();
    }

    /**
     * 在庫トランザクション履歴を取得する。
     *
     * @param productId   製品IDフィルタ
     * @param warehouseId 倉庫IDフィルタ
     * @param page        ページ番号
     * @param size        ページサイズ
     * @return トランザクション履歴
     */
    @GET
    @Path("/transactions")
    @SuppressWarnings("unchecked")
    public Response getTransactions(
            @QueryParam("productId") Long productId,
            @QueryParam("warehouseId") Long warehouseId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {

        logger.info("在庫トランザクション取得。productId=" + productId);

        // 技術的負債 #5: EntityManager直接使用
        StringBuffer jpql = new StringBuffer(
                "SELECT t FROM InventoryTransaction t WHERE 1=1");
        if (productId != null) {
            jpql.append(" AND t.productId = :productId");
        }
        if (warehouseId != null) {
            jpql.append(" AND t.warehouseId = :warehouseId");
        }
        jpql.append(" ORDER BY t.transactionDate DESC");

        Query query = em.createQuery(jpql.toString());
        if (productId != null) {
            query.setParameter("productId", productId);
        }
        if (warehouseId != null) {
            query.setParameter("warehouseId", warehouseId);
        }
        query.setFirstResult(page * size);
        query.setMaxResults(size);

        return Response.ok(query.getResultList()).build();
    }

    // ========================================================================
    // 棚卸
    // ========================================================================

    /**
     * 棚卸用の在庫一覧を取得する。
     * 指定された倉庫の全在庫品目を返す。
     *
     * @param warehouseId 倉庫ID
     * @return 在庫品目一覧
     */
    @GET
    @Path("/stock-check")
    @SuppressWarnings("unchecked")
    public Response getStockCheckList(@QueryParam("warehouseId") Long warehouseId) {
        logger.info("棚卸用在庫一覧取得。warehouseId=" + warehouseId);

        if (warehouseId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"warehouseId は必須です\"}").build();
        }

        List<InventoryItem> items = em.createQuery(
                "SELECT ii FROM InventoryItem ii JOIN FETCH ii.product JOIN FETCH ii.warehouse WHERE ii.warehouse.id = :warehouseId ORDER BY ii.product.name",
                InventoryItem.class)
                .setParameter("warehouseId", warehouseId)
                .getResultList();

        List<InventoryItemDto> dtoList = new ArrayList<>();
        for (InventoryItem item : items) {
            dtoList.add(inventoryMapper.toDto(item));
        }

        return Response.ok(dtoList).build();
    }

    /**
     * 棚卸結果を保存する。
     * 差異がある品目の在庫数量を実棚数量に調整する。
     *
     * @param body   棚卸結果データ（results: [{itemId, actualQuantity}]）
     * @param secCtx セキュリティコンテキスト
     * @return 処理結果
     */
    @POST
    @Path("/stock-check/save")
    @SuppressWarnings("unchecked")
    public Response saveStockCheckResults(Map<String, Object> body,
                                          @Context SecurityContext secCtx) {
        logger.info("棚卸結果保存。ユーザー=" + secCtx.getUserPrincipal().getName());

        List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("results");
        if (results == null || results.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"results は必須です\"}").build();
        }

        int adjustedCount = 0;
        for (Map<String, Object> result : results) {
            Number itemIdNum = (Number) result.get("itemId");
            Number actualQtyNum = (Number) result.get("actualQuantity");
            if (itemIdNum == null || actualQtyNum == null) continue;

            Long itemId = itemIdNum.longValue();
            int actualQuantity = actualQtyNum.intValue();

            List<InventoryItem> itemList = em.createQuery(
                    "SELECT i FROM InventoryItem i JOIN FETCH i.product JOIN FETCH i.warehouse WHERE i.id = :id",
                    InventoryItem.class)
                    .setParameter("id", itemId)
                    .getResultList();
            if (itemList.isEmpty()) continue;
            InventoryItem item = itemList.get(0);

            int currentQty = item.getQuantityOnHand();
            if (currentQty != actualQuantity) {
                int diff = actualQuantity - currentQty;
                if (diff > 0) {
                    inventoryService.addStock(
                            item.getProduct().getId(), item.getWarehouse().getId(),
                            diff, "STOCK_COUNT", null);
                } else {
                    inventoryService.removeStock(
                            item.getProduct().getId(), item.getWarehouse().getId(),
                            Math.abs(diff), "棚卸調整");
                }
                adjustedCount++;
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "棚卸結果を保存しました");
        response.put("totalItems", results.size());
        response.put("adjustedItems", adjustedCount);

        return Response.ok(response).build();
    }

    /**
     * 棚卸を作成する。
     *
     * @param countData 棚卸データ（warehouseId）
     * @param secCtx    セキュリティコンテキスト
     * @return 作成された棚卸
     */
    @POST
    @Path("/count")
    public Response createInventoryCount(Map<String, Object> countData,
                                         @Context SecurityContext secCtx) {
        logger.info("棚卸作成。ユーザー=" + secCtx.getUserPrincipal().getName());

        Long warehouseId = Long.parseLong(countData.get("warehouseId").toString());

        // 技術的負債: productIdsをnullで渡し、全品目を対象とする
        InventoryCount count = inventoryService.createInventoryCount(warehouseId, null);
        return Response.status(Response.Status.CREATED).entity(count).build();
    }

    /**
     * 棚卸を処理する（差異の反映）。
     *
     * @param id        棚卸ID
     * @param countData 棚卸結果データ
     * @param secCtx    セキュリティコンテキスト
     * @return 処理結果
     */
    @PUT
    @Path("/count/{id}")
    public Response processInventoryCount(@PathParam("id") Long id,
                                          Map<String, Object> countData,
                                          @Context SecurityContext secCtx) {
        logger.info("棚卸処理。ID=" + id + ", ユーザー=" + secCtx.getUserPrincipal().getName());

        try {
            Map<Long, Integer> actualQuantities = new HashMap<>();
            if (countData != null) {
                for (Map.Entry<String, Object> entry : countData.entrySet()) {
                    Long itemId = Long.parseLong(entry.getKey());
                    Integer qty = entry.getValue() instanceof Number
                            ? ((Number) entry.getValue()).intValue() : Integer.parseInt(entry.getValue().toString());
                    actualQuantities.put(itemId, qty);
                }
            }
            inventoryService.processInventoryCount(id, actualQuantities);
            Map<String, String> result = new HashMap<>();
            result.put("message", "棚卸処理が完了しました。");
            return Response.ok(result).build();
        } catch (Exception e) {
            logger.log(Level.WARNING, "棚卸処理エラー。", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }
    }

    // ========================================================================
    // 在庫評価
    // ========================================================================

    /**
     * 在庫評価レポートを取得する。
     *
     * @param warehouseId 倉庫IDフィルタ
     * @return 在庫評価情報
     */
    @GET
    @Path("/valuation")
    @SuppressWarnings("unchecked")
    public Response getInventoryValuation(@QueryParam("warehouseId") Long warehouseId) {
        logger.info("在庫評価取得。warehouseId=" + warehouseId);

        // 技術的負債 #5: EntityManager直接使用、ネイティブSQLクエリ
        String sql = "SELECT w.name AS warehouse_name, " +
                "COUNT(ii.id) AS item_count, " +
                "COALESCE(SUM(ii.quantity_on_hand * p.unit_price), 0) AS total_value " +
                "FROM inventory_item ii " +
                "JOIN warehouse w ON w.id = ii.warehouse_id " +
                "JOIN product p ON p.id = ii.product_id " +
                "WHERE 1=1 ";

        if (warehouseId != null) {
            sql += "AND w.id = ?1 ";
        }
        sql += "GROUP BY w.name ORDER BY total_value DESC";

        Query query = em.createNativeQuery(sql);
        if (warehouseId != null) {
            query.setParameter(1, warehouseId);
        }

        List<Object[]> results = query.getResultList();

        List<Map<String, Object>> valuation = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            Object[] row = results.get(i);
            Map<String, Object> entry = new HashMap<>();
            entry.put("warehouseName", row[0]);
            entry.put("itemCount", row[1]);
            entry.put("totalValue", row[2]);
            valuation.add(entry);
        }

        return Response.ok(valuation).build();
    }
}
