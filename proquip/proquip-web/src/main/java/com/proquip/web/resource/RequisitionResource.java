package com.proquip.web.resource;

import com.proquip.common.dto.PageResultDto;
import com.proquip.common.dto.PurchaseRequisitionDto;
import com.proquip.ejb.entity.procurement.PurchaseRequisition;
import com.proquip.ejb.service.RequisitionServiceBean;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.stream.Collectors;

import com.proquip.ejb.entity.organization.Department;
import com.proquip.ejb.entity.organization.UserProfile;
import com.proquip.ejb.entity.procurement.PurchaseRequisitionItem;
import com.proquip.ejb.entity.product.Product;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 購買依頼管理RESTリソース。
 *
 * <p>購買依頼のCRUD操作、承認ワークフロー、発注への変換を提供する。</p>
 *
 * <p>【技術的負債 #2 - コピペパターン】
 * 承認関連のエンドポイント（submit, approve, reject）のロジックは
 * {@link PurchaseOrderResource} とほぼ同一のコピーペーストである。
 * 共通の承認リソースまたはミックスインに抽出すべき。</p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
@Path("/requisitions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RequisitionResource {

    private static final Logger logger = Logger.getLogger(RequisitionResource.class.getName());

    @Inject
    private RequisitionServiceBean requisitionService;

    @PersistenceContext
    private EntityManager em;

    // ========================================================================
    // CRUD
    // ========================================================================

    /**
     * 購買依頼一覧を取得する。
     *
     * @param page   ページ番号（0始まり）
     * @param size   ページサイズ
     * @param status ステータスフィルタ
     * @return 購買依頼一覧
     */
    @GET
    @SuppressWarnings("unchecked")
    public Response listRequisitions(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("status") String status) {

        logger.info("購買依頼一覧取得。page=" + page + ", size=" + size + ", status=" + status);

        // 技術的負債 #5: EntityManager直接使用
        StringBuffer jpql = new StringBuffer(
                "SELECT r FROM PurchaseRequisition r LEFT JOIN FETCH r.items WHERE 1=1");
        if (status != null && !status.isEmpty()) {
            jpql.append(" AND r.status = :status");
        }
        jpql.append(" ORDER BY r.requiredDate DESC");

        var query = em.createQuery(jpql.toString());
        if (status != null && !status.isEmpty()) {
            query.setParameter("status", status);
        }
        query.setFirstResult(page * size);
        query.setMaxResults(size);

        List<PurchaseRequisition> requisitions = query.getResultList();

        // 総件数カウント
        StringBuffer countJpql = new StringBuffer("SELECT COUNT(r) FROM PurchaseRequisition r WHERE 1=1");
        if (status != null && !status.isEmpty()) {
            countJpql.append(" AND r.status = :status");
        }
        var countQuery = em.createQuery(countJpql.toString());
        if (status != null && !status.isEmpty()) {
            countQuery.setParameter("status", status);
        }
        long totalElements = (Long) countQuery.getSingleResult();

        // 依頼者情報のバッチ取得
        List<Long> requesterIds = requisitions.stream()
                .map(PurchaseRequisition::getRequesterId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, UserProfile> userMap = new HashMap<>();
        if (!requesterIds.isEmpty()) {
            List<UserProfile> users = em.createQuery(
                    "SELECT u FROM UserProfile u LEFT JOIN FETCH u.department WHERE u.id IN :ids",
                    UserProfile.class)
                    .setParameter("ids", requesterIds)
                    .getResultList();
            for (UserProfile u : users) {
                userMap.put(u.getId(), u);
            }
        }

        // エンティティからDTOに変換（手動マッピング）
        List<PurchaseRequisitionDto> dtoList = new ArrayList<>();
        for (int i = 0; i < requisitions.size(); i++) {
            dtoList.add(toDto(requisitions.get(i), userMap));
        }

        PageResultDto<PurchaseRequisitionDto> pageResult = new PageResultDto<>(dtoList, totalElements, page, size);
        return Response.ok(pageResult).build();
    }

    /**
     * 購買依頼詳細を取得する。
     *
     * @param id 購買依頼ID
     * @return 購買依頼DTO
     */
    @GET
    @Path("/{id}")
    public Response getRequisition(@PathParam("id") Long id) {
        logger.info("購買依頼詳細取得。ID=" + id);

        PurchaseRequisition requisition = requisitionService.findById(id);
        Map<Long, UserProfile> userMap = new HashMap<>();
        if (requisition.getRequesterId() != null) {
            List<UserProfile> users = em.createQuery(
                    "SELECT u FROM UserProfile u LEFT JOIN FETCH u.department WHERE u.id = :id",
                    UserProfile.class)
                    .setParameter("id", requisition.getRequesterId())
                    .getResultList();
            if (!users.isEmpty()) {
                userMap.put(users.get(0).getId(), users.get(0));
            }
        }
        PurchaseRequisitionDto dto = toDto(requisition, userMap);

        return Response.ok(dto).build();
    }

    /**
     * 購買依頼を新規作成する。
     *
     * @param dto    購買依頼DTO
     * @param secCtx セキュリティコンテキスト
     * @return 作成された購買依頼
     */
    @POST
    public Response createRequisition(Map<String, Object> body,
                                      @Context SecurityContext secCtx) {
        logger.info("購買依頼作成。ユーザー=" + secCtx.getUserPrincipal().getName());

        PurchaseRequisition entity = new PurchaseRequisition();
        entity.setTitle(body.get("title") != null ? body.get("title").toString() : "購買依頼");
        entity.setJustification(body.get("justification") != null ? body.get("justification").toString() : null);
        Object priorityVal = body.get("priority") != null ? body.get("priority") : body.get("urgency");
        entity.setPriority(priorityVal != null ? priorityVal.toString() : "NORMAL");
        if (body.get("requiredDate") != null) {
            try {
                entity.setRequiredDate(new SimpleDateFormat("yyyy-MM-dd").parse(body.get("requiredDate").toString()));
            } catch (ParseException e) {
                logger.warning("requiredDate パースエラー: " + body.get("requiredDate"));
            }
        }

        String username = secCtx.getUserPrincipal().getName();
        List<UserProfile> users = em.createQuery(
                "SELECT u FROM UserProfile u WHERE u.keycloakId = :keycloakId", UserProfile.class)
                .setParameter("keycloakId", username)
                .getResultList();
        if (!users.isEmpty()) {
            entity.setRequesterId(users.get(0).getId());
        } else {
            List<UserProfile> fallback = em.createQuery(
                    "SELECT u FROM UserProfile u", UserProfile.class)
                    .setMaxResults(1)
                    .getResultList();
            entity.setRequesterId(fallback.isEmpty() ? 1L : fallback.get(0).getId());
        }

        if (body.get("department") != null) {
            List<Department> depts = em.createQuery(
                    "SELECT d FROM Department d WHERE d.name = :name", Department.class)
                    .setParameter("name", body.get("department").toString())
                    .getResultList();
            entity.setDepartmentId(depts.isEmpty() ? 1L : depts.get(0).getId());
        } else {
            entity.setDepartmentId(1L);
        }

        Object itemsObj = body.get("items");
        if (itemsObj instanceof List) {
            List<PurchaseRequisitionItem> items = new ArrayList<>();
            for (Object rawItem : (List<?>) itemsObj) {
                if (rawItem instanceof Map) {
                    Map<?, ?> itemMap = (Map<?, ?>) rawItem;
                    PurchaseRequisitionItem item = new PurchaseRequisitionItem();
                    item.setQuantity(itemMap.get("quantity") != null ? ((Number) itemMap.get("quantity")).intValue() : 1);
                    if (itemMap.get("estimatedUnitPrice") != null) {
                        item.setEstimatedUnitCost(new BigDecimal(itemMap.get("estimatedUnitPrice").toString()));
                    }
                    if (itemMap.get("productId") != null) {
                        Product product = em.find(Product.class, ((Number) itemMap.get("productId")).longValue());
                        item.setProduct(product);
                    } else {
                        continue;
                    }
                    items.add(item);
                }
            }
            entity.setItems(items);
        }

        PurchaseRequisition created = requisitionService.createRequisition(entity);
        PurchaseRequisitionDto resultDto = toDto(created, new HashMap<>());

        return Response.status(Response.Status.CREATED).entity(resultDto).build();
    }

    /**
     * 購買依頼を更新する。
     *
     * @param id     購買依頼ID
     * @param dto    更新データ
     * @param secCtx セキュリティコンテキスト
     * @return 更新後の購買依頼
     */
    @PUT
    @Path("/{id}")
    public Response updateRequisition(@PathParam("id") Long id,
                                      PurchaseRequisitionDto dto,
                                      @Context SecurityContext secCtx) {
        logger.info("購買依頼更新。ID=" + id + ", ユーザー=" + secCtx.getUserPrincipal().getName());

        PurchaseRequisition existing = requisitionService.findById(id);
        existing.setJustification(dto.getJustification());
        existing.setPriority(dto.getUrgency());

        PurchaseRequisition updated = requisitionService.updateRequisition(existing);
        PurchaseRequisitionDto resultDto = toDto(updated, new HashMap<>());

        return Response.ok(resultDto).build();
    }

    /**
     * 購買依頼を削除する。
     *
     * @param id     購買依頼ID
     * @param secCtx セキュリティコンテキスト
     * @return 204 No Content
     */
    @DELETE
    @Path("/{id}")
    public Response deleteRequisition(@PathParam("id") Long id,
                                      @Context SecurityContext secCtx) {
        logger.info("購買依頼削除。ID=" + id + ", ユーザー=" + secCtx.getUserPrincipal().getName());

        requisitionService.cancelRequisition(id);
        return Response.noContent().build();
    }

    // ========================================================================
    // 承認ワークフロー（技術的負債 #2: PurchaseOrderResourceとほぼ同一のコピペ）
    // ========================================================================

    /**
     * 購買依頼を承認申請する。
     *
     * <p>技術的負債 #2: PurchaseOrderResource.submitForApproval()のコピペ。</p>
     *
     * @param id     購買依頼ID
     * @param secCtx セキュリティコンテキスト
     * @return 200 OK
     */
    @POST
    @Path("/{id}/submit")
    public Response submitForApproval(@PathParam("id") Long id,
                                      @Context SecurityContext secCtx) {
        logger.info("購買依頼承認申請。ID=" + id + ", ユーザー=" + secCtx.getUserPrincipal().getName());

        try {
            requisitionService.submitForApproval(id);
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
     * 購買依頼を承認する。
     *
     * <p>技術的負債 #2: PurchaseOrderResource.approveOrder()のコピペ。</p>
     *
     * @param id     購買依頼ID
     * @param body   承認データ
     * @param secCtx セキュリティコンテキスト
     * @return 200 OK
     */
    @POST
    @Path("/{id}/approve")
    public Response approveRequisition(@PathParam("id") Long id,
                                       Map<String, Object> body,
                                       @Context SecurityContext secCtx) {
        logger.info("購買依頼承認。ID=" + id + ", ユーザー=" + secCtx.getUserPrincipal().getName());

        Long approverUserId = resolveUserId(secCtx);
        String approverRole = body != null && body.get("role") != null
                ? body.get("role").toString() : "MANAGER";
        requisitionService.approveRequisition(id, approverUserId, approverRole);

        Map<String, String> result = new HashMap<>();
        result.put("message", "承認が完了しました。");
        return Response.ok(result).build();
    }

    /**
     * 購買依頼を却下する。
     *
     * <p>技術的負債 #2: PurchaseOrderResource.rejectOrder()のコピペ。</p>
     *
     * @param id     購買依頼ID
     * @param body   却下データ
     * @param secCtx セキュリティコンテキスト
     * @return 200 OK
     */
    @POST
    @Path("/{id}/reject")
    public Response rejectRequisition(@PathParam("id") Long id,
                                      Map<String, Object> body,
                                      @Context SecurityContext secCtx) {
        logger.info("購買依頼却下。ID=" + id + ", ユーザー=" + secCtx.getUserPrincipal().getName());

        String reason = body != null && body.get("reason") != null
                ? body.get("reason").toString() : "却下";

        Long rejectorUserId = resolveUserId(secCtx);
        requisitionService.rejectRequisition(id, rejectorUserId, reason);

        Map<String, String> result = new HashMap<>();
        result.put("message", "却下が完了しました。");
        return Response.ok(result).build();
    }

    /**
     * 購買依頼を発注に変換する。
     *
     * @param id     購買依頼ID
     * @param secCtx セキュリティコンテキスト
     * @return 変換結果
     */
    @POST
    @Path("/{id}/convert-to-order")
    public Response convertToOrder(@PathParam("id") Long id,
                                   @Context SecurityContext secCtx) {
        logger.info("購買依頼→発注変換。ID=" + id + ", ユーザー=" + secCtx.getUserPrincipal().getName());

        try {
            // 技術的負債: サプライヤーIDをnullで渡しており、サービス側で自動選定に依存
            requisitionService.convertToOrder(id, null);
            Map<String, String> result = new HashMap<>();
            result.put("message", "購買依頼を発注に変換しました。");
            return Response.ok(result).build();
        } catch (Exception e) {
            logger.log(Level.WARNING, "発注変換エラー。", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }
    }

    // ========================================================================
    // ヘルパーメソッド
    // ========================================================================

    /**
     * SecurityContextからUserProfileのIDを解決する。
     *
     * @param secCtx セキュリティコンテキスト
     * @return ユーザーID（解決できない場合は1L）
     */
    private Long resolveUserId(SecurityContext secCtx) {
        if (secCtx == null || secCtx.getUserPrincipal() == null) {
            return 1L;
        }
        String keycloakId = secCtx.getUserPrincipal().getName();
        List<UserProfile> users = em.createQuery(
                "SELECT u FROM UserProfile u WHERE u.keycloakId = :keycloakId", UserProfile.class)
                .setParameter("keycloakId", keycloakId)
                .getResultList();
        if (!users.isEmpty()) {
            return users.get(0).getId();
        }
        // フォールバック: ユーザーが見つからない場合はデフォルト値を返す
        logger.warning("SecurityContextのkeycloakId=" + keycloakId + " に対応するUserProfileが見つかりません。デフォルトID=1Lを使用します。");
        return 1L;
    }

    /**
     * 購買依頼エンティティをDTOに手動変換する。
     *
     * <p>技術的負債 #10: MapStructマッパーが存在しないため手動変換。</p>
     *
     * @param entity 購買依頼エンティティ
     * @return 購買依頼DTO
     */
    private PurchaseRequisitionDto toDto(PurchaseRequisition entity, Map<Long, UserProfile> userMap) {
        if (entity == null) {
            return null;
        }

        PurchaseRequisitionDto dto = new PurchaseRequisitionDto();
        dto.setId(entity.getId());
        dto.setRequisitionNumber(entity.getReqNumber());
        dto.setStatus(entity.getStatus());
        if (entity.getRequiredDate() != null) {
            Date converted = new Date(entity.getRequiredDate().getTime());
            dto.setRequestDate(converted);
            dto.setRequiredDate(converted);
        }
        dto.setJustification(entity.getJustification());
        dto.setUrgency(entity.getPriority());

        if (entity.getCreatedAt() != null) {
            dto.setCreatedAt(new Date(entity.getCreatedAt().getTime()));
        }
        if (entity.getUpdatedAt() != null) {
            dto.setUpdatedAt(new Date(entity.getUpdatedAt().getTime()));
        }

        if (entity.getRequesterId() != null && userMap.containsKey(entity.getRequesterId())) {
            UserProfile user = userMap.get(entity.getRequesterId());
            dto.setRequestedBy(user.getLastName() + " " + user.getFirstName());
            if (user.getDepartment() != null) {
                dto.setDepartment(user.getDepartment().getName());
                dto.setDepartmentName(user.getDepartment().getName());
            }
        }

        if (entity.getItems() != null) {
            dto.setItemCount(entity.getItems().size());
            BigDecimal total = BigDecimal.ZERO;
            for (PurchaseRequisitionItem item : entity.getItems()) {
                if (item.getQuantity() != null && item.getEstimatedUnitCost() != null) {
                    total = total.add(item.getEstimatedUnitCost().multiply(BigDecimal.valueOf(item.getQuantity())));
                }
            }
            dto.setTotalAmount(total);
        }

        return dto;
    }
}
