package com.proquip.web.resource;

import com.proquip.common.constant.AppConstants;
import com.proquip.common.dto.PageResultDto;
import com.proquip.common.dto.PurchaseOrderResponse;
import com.proquip.ejb.mapper.PurchaseOrderMapper;
import com.proquip.ejb.entity.procurement.GoodsReceipt;
import com.proquip.ejb.entity.procurement.PurchaseOrder;
import com.proquip.ejb.service.PurchaseOrderServiceBean;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 発注管理RESTリソース。
 *
 * <p>発注のCRUD操作、承認ワークフロー、入庫処理、ステータス管理、
 * エクスポート、サマリー統計を提供する。</p>
 *
 * <p>【技術的負債 #7】
 * 一部のエンドポイントで例外をcatchし、{@code Response.ok()} でエラーメッセージを
 * 返している。HTTPステータス200でエラーレスポンスを返すのはRESTful設計に反する。</p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
@Path("/purchase-orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PurchaseOrderResource {

    private static final Logger logger = Logger.getLogger(PurchaseOrderResource.class.getName());

    @Inject
    private PurchaseOrderServiceBean orderService;

    /** 技術的負債 #10: PurchaseOrderMapperは手書き実装のため、newでインスタンス化 */
    private final PurchaseOrderMapper orderMapper = new PurchaseOrderMapper();

    // ========================================================================
    // CRUD
    // ========================================================================

    /**
     * 発注一覧を取得する（フィルタ・ページネーション付き）。
     *
     * @param page       ページ番号（0始まり）
     * @param size       ページサイズ
     * @param status     ステータスフィルタ
     * @param keyword    キーワード（発注番号・備考）
     * @param supplierId サプライヤーIDフィルタ
     * @param fromDate   検索開始日（yyyy-MM-dd形式）
     * @param toDate     検索終了日（yyyy-MM-dd形式）
     * @return 発注一覧のページネーション結果
     */
    @GET
    public Response listOrders(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("status") String status,
            @QueryParam("keyword") String keyword,
            @QueryParam("supplierId") Long supplierId,
            @QueryParam("fromDate") String fromDate,
            @QueryParam("toDate") String toDate) {

        logger.info("発注一覧取得。page=" + page + ", status=" + status + ", keyword=" + keyword);

        if (size > AppConstants.MAX_PAGE_SIZE) {
            size = AppConstants.MAX_PAGE_SIZE;
        }

        Date from = parseDate(fromDate);
        Date to = parseDate(toDate);

        List<PurchaseOrder> orders = orderService.searchOrders(
                keyword, status, supplierId, from, to, page, size);

        List<PurchaseOrderResponse> dtoList = new ArrayList<>();
        for (int i = 0; i < orders.size(); i++) {
            dtoList.add(orderMapper.toResponse(orders.get(i)));
        }

        // 技術的負債: 総件数を別クエリで取得すべきだが、ここではリストサイズで代用
        PageResultDto<PurchaseOrderResponse> result =
                new PageResultDto<>(dtoList, dtoList.size(), page, size);
        return Response.ok(result).build();
    }

    /**
     * 発注詳細を取得する。
     *
     * @param id 発注ID
     * @return 発注応答DTO
     */
    @GET
    @Path("/{id}")
    public Response getOrder(@PathParam("id") Long id) {
        logger.info("発注詳細取得。ID=" + id);

        PurchaseOrder order = orderService.findOrderById(id);
        PurchaseOrderResponse dto = orderMapper.toResponse(order);

        return Response.ok(dto).build();
    }

    /**
     * 発注を新規作成する。
     *
     * @param orderResponse 発注データ
     * @param secCtx        セキュリティコンテキスト
     * @return 作成された発注
     */
    @POST
    public Response createOrder(PurchaseOrderResponse orderResponse,
                                @Context SecurityContext secCtx) {
        logger.info("発注作成。ユーザー=" + secCtx.getUserPrincipal().getName());

        PurchaseOrder entity = orderMapper.toEntity(orderResponse);
        PurchaseOrder created = orderService.createOrder(entity);
        PurchaseOrderResponse dto = orderMapper.toResponse(created);

        return Response.status(Response.Status.CREATED).entity(dto).build();
    }

    /**
     * 発注を更新する。
     *
     * @param id            発注ID
     * @param orderResponse 更新データ
     * @param secCtx        セキュリティコンテキスト
     * @return 更新後の発注
     */
    @PUT
    @Path("/{id}")
    public Response updateOrder(@PathParam("id") Long id,
                                PurchaseOrderResponse orderResponse,
                                @Context SecurityContext secCtx) {
        logger.info("発注更新。ID=" + id + ", ユーザー=" + secCtx.getUserPrincipal().getName());

        PurchaseOrder entity = orderMapper.toEntity(orderResponse);
        PurchaseOrder updated = orderService.updateOrder(id, entity);
        PurchaseOrderResponse dto = orderMapper.toResponse(updated);

        return Response.ok(dto).build();
    }

    /**
     * 発注をキャンセルする（論理削除）。
     *
     * @param id     発注ID
     * @param body   キャンセル理由（reason フィールド）
     * @param secCtx セキュリティコンテキスト
     * @return 204 No Content
     */
    @DELETE
    @Path("/{id}")
    public Response cancelOrder(@PathParam("id") Long id,
                                Map<String, String> body,
                                @Context SecurityContext secCtx) {
        logger.info("発注キャンセル。ID=" + id + ", ユーザー=" + secCtx.getUserPrincipal().getName());

        String reason = body != null ? body.get("reason") : "キャンセル";
        orderService.cancelOrder(id, reason);

        return Response.noContent().build();
    }

    // ========================================================================
    // 承認ワークフロー
    // ========================================================================

    /**
     * 発注を承認申請する。
     *
     * @param id     発注ID
     * @param secCtx セキュリティコンテキスト
     * @return 200 OK
     */
    @POST
    @Path("/{id}/submit")
    public Response submitForApproval(@PathParam("id") Long id,
                                      @Context SecurityContext secCtx) {
        logger.info("発注承認申請。ID=" + id + ", ユーザー=" + secCtx.getUserPrincipal().getName());

        try {
            orderService.submitForApproval(id);
            Map<String, String> result = new HashMap<>();
            result.put("message", "承認申請が完了しました。");
            return Response.ok(result).build();
        } catch (Exception e) {
            logger.log(Level.WARNING, "承認申請エラー。", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }
    }

    /**
     * 発注を承認する。
     *
     * @param id     発注ID
     * @param body   承認データ（stepId, comment）
     * @param secCtx セキュリティコンテキスト
     * @return 200 OK
     */
    @POST
    @Path("/{id}/approve")
    public Response approveOrder(@PathParam("id") Long id,
                                 Map<String, Object> body,
                                 @Context SecurityContext secCtx) {
        logger.info("発注承認。ID=" + id + ", ユーザー=" + secCtx.getUserPrincipal().getName());

        Long stepId = body != null && body.get("stepId") != null
                ? Long.parseLong(body.get("stepId").toString()) : null;
        String comment = body != null && body.get("comment") != null
                ? body.get("comment").toString() : null;

        orderService.approveOrder(id, stepId, comment);

        Map<String, String> result = new HashMap<>();
        result.put("message", "承認が完了しました。");
        return Response.ok(result).build();
    }

    /**
     * 発注を却下する。
     *
     * @param id     発注ID
     * @param body   却下データ（stepId, reason）
     * @param secCtx セキュリティコンテキスト
     * @return 200 OK
     */
    @POST
    @Path("/{id}/reject")
    public Response rejectOrder(@PathParam("id") Long id,
                                Map<String, Object> body,
                                @Context SecurityContext secCtx) {
        logger.info("発注却下。ID=" + id + ", ユーザー=" + secCtx.getUserPrincipal().getName());

        Long stepId = body != null && body.get("stepId") != null
                ? Long.parseLong(body.get("stepId").toString()) : null;
        String reason = body != null && body.get("reason") != null
                ? body.get("reason").toString() : "却下";

        orderService.rejectOrder(id, stepId, reason);

        Map<String, String> result = new HashMap<>();
        result.put("message", "却下が完了しました。");
        return Response.ok(result).build();
    }

    /**
     * 承認履歴を取得する。
     *
     * @param id 発注ID
     * @return 承認ステップ一覧
     */
    @GET
    @Path("/{id}/approval-history")
    public Response getApprovalHistory(@PathParam("id") Long id) {
        logger.info("承認履歴取得。発注ID=" + id);

        return Response.ok(orderService.getApprovalHistory(id)).build();
    }

    // ========================================================================
    // 入庫処理
    // ========================================================================

    /**
     * 入庫処理を実行する。
     *
     * @param id      発注ID
     * @param receipt 入庫情報
     * @param secCtx  セキュリティコンテキスト
     * @return 処理結果
     */
    @POST
    @Path("/{id}/goods-receipt")
    public Response processGoodsReceipt(@PathParam("id") Long id,
                                        GoodsReceipt receipt,
                                        @Context SecurityContext secCtx) {
        logger.info("入庫処理。発注ID=" + id + ", ユーザー=" + secCtx.getUserPrincipal().getName());

        try {
            GoodsReceipt processed = orderService.processGoodsReceipt(id, receipt);
            return Response.ok(processed).build();
        } catch (Exception e) {
            logger.log(Level.WARNING, "入庫処理エラー。", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "入庫処理に失敗しました: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }
    }

    @POST
    @Path("/{id}/receive")
    public Response receiveGoods(@PathParam("id") Long id,
                                 GoodsReceipt receipt,
                                 @Context SecurityContext secCtx) {
        return processGoodsReceipt(id, receipt, secCtx);
    }

    @POST
    @Path("/{id}/cancel")
    public Response cancelOrderByPost(@PathParam("id") Long id,
                                      Map<String, String> body,
                                      @Context SecurityContext secCtx) {
        logger.info("発注キャンセル(POST)。ID=" + id + ", ユーザー=" + secCtx.getUserPrincipal().getName());

        String reason = body != null && body.get("reason") != null ? body.get("reason") : "キャンセル";
        orderService.cancelOrder(id, reason);

        Map<String, String> result = new HashMap<>();
        result.put("message", "キャンセルが完了しました。");
        return Response.ok(result).build();
    }

    // ========================================================================
    // ステータス・履歴
    // ========================================================================

    /**
     * ステータス変更履歴を取得する。
     *
     * @param id 発注ID
     * @return ステータス履歴
     */
    @GET
    @Path("/{id}/status-history")
    public Response getStatusHistory(@PathParam("id") Long id) {
        logger.info("ステータス履歴取得。発注ID=" + id);

        List<Object[]> history = orderService.getStatusHistory(id);

        // 技術的負債 #12: Object[]をMapに変換して返す（型安全なDTOを使うべき）
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < history.size(); i++) {
            Object[] row = history.get(i);
            Map<String, Object> entry = new HashMap<>();
            entry.put("fromStatus", row[0]);
            entry.put("toStatus", row[1]);
            entry.put("changedAt", row[2]);
            entry.put("changedBy", row[3]);
            entry.put("comments", row.length > 4 ? row[4] : null);
            result.add(entry);
        }

        return Response.ok(result).build();
    }

    /**
     * 承認待ちの発注一覧を取得する。
     *
     * @param secCtx セキュリティコンテキスト
     * @return 承認待ち発注一覧
     */
    @GET
    @Path("/pending-approval")
    public Response getPendingApprovalOrders(@Context SecurityContext secCtx) {
        logger.info("承認待ち発注取得。ユーザー=" + secCtx.getUserPrincipal().getName());

        // 技術的負債: ユーザーに紐づく承認待ちのフィルタが不完全
        List<PurchaseOrder> orders = orderService.findOrders(
                "SUBMITTED", null, null, 0, 50);

        List<PurchaseOrderResponse> dtoList = new ArrayList<>();
        for (int i = 0; i < orders.size(); i++) {
            dtoList.add(orderMapper.toResponse(orders.get(i)));
        }

        return Response.ok(dtoList).build();
    }

    @GET
    @Path("/pending-approvals")
    public Response getPendingApprovalOrdersAlias(@Context SecurityContext secCtx) {
        return getPendingApprovalOrders(secCtx);
    }

    // ========================================================================
    // エクスポート・サマリー
    // ========================================================================

    /**
     * 発注をCSV形式でエクスポートする。
     *
     * @param status ステータスフィルタ
     * @return CSV形式のレスポンス
     */
    @GET
    @Path("/export")
    @Produces("text/csv")
    public Response exportOrders(@QueryParam("status") String status) {
        logger.info("発注エクスポート。status=" + status);

        List<PurchaseOrder> orders = orderService.findOrders(
                status, null, null, 0, AppConstants.MAX_PAGE_SIZE);

        List<Long> orderIds = new ArrayList<>();
        for (int i = 0; i < orders.size(); i++) {
            orderIds.add(orders.get(i).getId());
        }

        String csv = orderService.exportOrdersToCsv(orderIds);
        return Response.ok(csv)
                .header("Content-Disposition", "attachment; filename=\"purchase-orders.csv\"")
                .build();
    }

    /**
     * 発注サマリー統計を取得する。
     *
     * @return ステータス別の発注件数分布
     */
    @GET
    @Path("/summary")
    public Response getOrderSummary() {
        logger.info("発注サマリー取得。");

        List<Object[]> distribution = orderService.getOrderStatusDistribution();

        // 技術的負債 #12: Object[]をMapに変換
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < distribution.size(); i++) {
            Object[] row = distribution.get(i);
            Map<String, Object> entry = new HashMap<>();
            entry.put("status", row[0]);
            entry.put("count", row[1]);
            result.add(entry);
        }

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
            SimpleDateFormat sdf = new SimpleDateFormat(AppConstants.ISO_DATE_FORMAT);
            return sdf.parse(dateStr);
        } catch (ParseException e) {
            logger.warning("日付パースエラー: " + dateStr);
            return null;
        }
    }
}
