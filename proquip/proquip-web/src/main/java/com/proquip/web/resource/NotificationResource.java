package com.proquip.web.resource;

import com.proquip.ejb.entity.system.Notification;
import com.proquip.ejb.service.NotificationServiceBean;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
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

import com.proquip.ejb.entity.organization.UserProfile;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 通知管理RESTリソース。
 *
 * <p>ユーザー通知の一覧取得、既読処理、一括既読、未読件数取得を提供する。</p>
 *
 * <p>【技術的負債 #12】
 * 未読件数エンドポイントで {@link HashMap} を返しており、型安全なDTOを使用していない。</p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
@Path("/notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NotificationResource {

    private static final Logger logger = Logger.getLogger(NotificationResource.class.getName());

    @Inject
    private NotificationServiceBean notificationService;

    @PersistenceContext
    private EntityManager em;

    // ========================================================================
    // 通知一覧
    // ========================================================================

    /**
     * 未読通知一覧を取得する。
     *
     * <p>技術的負債: ユーザーIDの取得がハードコードされている。
     * SecurityContextからKeycloak IDを解決し、UserProfileのIDに変換すべき。</p>
     *
     * @param userId ユーザーIDクエリパラメータ
     * @param secCtx セキュリティコンテキスト
     * @return 未読通知一覧
     */
    @GET
    public Response listNotifications(
            @QueryParam("userId") Long userId,
            @Context SecurityContext secCtx) {

        if (userId == null) {
            userId = resolveUserId(secCtx);
        }

        logger.info("通知一覧取得。userId=" + userId);

        List<Notification> notifications = notificationService.getUnreadNotifications(userId);
        return Response.ok(notifications).build();
    }

    // ========================================================================
    // 既読処理
    // ========================================================================

    /**
     * 通知を既読にする。
     *
     * @param id     通知ID
     * @param secCtx セキュリティコンテキスト
     * @return 200 OK
     */
    @PUT
    @Path("/{id}/read")
    public Response markAsRead(@PathParam("id") Long id,
                               @Context SecurityContext secCtx) {
        logger.info("通知既読処理。通知ID=" + id
                + ", ユーザー=" + secCtx.getUserPrincipal().getName());

        Long userId = resolveUserId(secCtx);
        notificationService.markAsRead(id, userId);

        Map<String, String> result = new HashMap<>();
        result.put("message", "通知を既読にしました。");
        return Response.ok(result).build();
    }

    /**
     * 指定ユーザーの全通知を既読にする。
     *
     * @param body   リクエストボディ（userId）
     * @param secCtx セキュリティコンテキスト
     * @return 既読にした件数
     */
    @POST
    @Path("/mark-all-read")
    public Response markAllAsRead(Map<String, Object> body,
                                  @Context SecurityContext secCtx) {
        logger.info("全通知既読処理。ユーザー=" + secCtx.getUserPrincipal().getName());

        // 技術的負債: ユーザーIDをリクエストボディから取得（SecurityContextから解決すべき）
        Long userId = body != null && body.get("userId") != null
                ? Long.parseLong(body.get("userId").toString()) : null;

        if (userId == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "userIdは必須です。");
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }

        int updatedCount = notificationService.markAllAsRead(userId);

        // 技術的負債 #12: HashMapで応答を返す
        Map<String, Object> result = new HashMap<>();
        result.put("message", "全通知を既読にしました。");
        result.put("updatedCount", updatedCount);
        return Response.ok(result).build();
    }

    // ========================================================================
    // 未読件数
    // ========================================================================

    /**
     * 未読通知件数を取得する。
     *
     * <p>【技術的負債 #12】HashMapで応答を返している。</p>
     *
     * @param userId ユーザーID
     * @return 未読件数
     */
    @GET
    @Path("/unread-count")
    public Response getUnreadCount(@QueryParam("userId") Long userId,
                                   @Context SecurityContext secCtx) {
        if (userId == null) {
            userId = resolveUserId(secCtx);
        }
        logger.info("未読通知件数取得。userId=" + userId);

        long count = notificationService.getUnreadCount(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("unreadCount", count);
        return Response.ok(result).build();
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
        String principalName = secCtx.getUserPrincipal().getName();

        List<UserProfile> users = em.createQuery(
                "SELECT u FROM UserProfile u WHERE u.keycloakId = :keycloakId", UserProfile.class)
                .setParameter("keycloakId", principalName)
                .getResultList();
        if (!users.isEmpty()) {
            return users.get(0).getId();
        }

        @SuppressWarnings("unchecked")
        List<Object> ids = em.createNativeQuery("SELECT id FROM user_profile WHERE username = ?1")
                .setParameter(1, principalName)
                .getResultList();
        if (!ids.isEmpty()) {
            return ((Number) ids.get(0)).longValue();
        }

        logger.warning("principalName=" + principalName + " に対応するUserProfileが見つかりません。デフォルトID=1Lを使用します。");
        return 1L;
    }
}
