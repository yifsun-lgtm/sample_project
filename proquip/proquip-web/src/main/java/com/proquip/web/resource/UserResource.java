package com.proquip.web.resource;

import com.proquip.common.dto.PageResultDto;
import com.proquip.common.dto.UserProfileDto;
import com.proquip.ejb.entity.organization.Role;
import com.proquip.ejb.entity.organization.UserProfile;

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

import com.proquip.ejb.entity.organization.Permission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * ユーザー管理RESTリソース。
 *
 * <p>ユーザープロファイルの参照、現在のユーザー情報取得、プロファイル更新、
 * 部門別ユーザー一覧を提供する。</p>
 *
 * <p>【技術的負債 #5】
 * EntityManagerを直接使用してユーザー検索を行っている。
 * UserServiceBeanが存在しないため、RESTリソース内でクエリを直接発行している。</p>
 *
 * <p>【技術的負債 #12】
 * パスワードハッシュ相当のKeycloak IDをDTOに含めてしまうリスクがある。
 * セキュリティレビューで指摘予定。</p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    private static final Logger logger = Logger.getLogger(UserResource.class.getName());

    /**
     * 技術的負債 #5: UserServiceBeanが存在しないためEntityManagerを直接注入。
     */
    @PersistenceContext
    private EntityManager em;

    // ========================================================================
    // ユーザー一覧・詳細
    // ========================================================================

    /**
     * ユーザー一覧を取得する。
     *
     * <p>【技術的負債 #5】EntityManagerで直接クエリを発行している。</p>
     *
     * @param page   ページ番号（0始まり）
     * @param size   ページサイズ
     * @param status ステータスフィルタ
     * @return ユーザー一覧
     */
    @GET
    @SuppressWarnings("unchecked")
    public Response listUsers(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("status") String status) {

        logger.info("ユーザー一覧取得。page=" + page + ", size=" + size + ", status=" + status);

        // 技術的負債 #5: EntityManager直接使用
        StringBuffer jpql = new StringBuffer(
                "SELECT u FROM UserProfile u LEFT JOIN FETCH u.department WHERE 1=1");
        if (status != null && !status.isEmpty()) {
            jpql.append(" AND u.active = :active");
        }
        jpql.append(" ORDER BY u.lastName, u.firstName");

        var query = em.createQuery(jpql.toString());
        if (status != null && !status.isEmpty()) {
            query.setParameter("active", "ACTIVE".equalsIgnoreCase(status));
        }
        query.setFirstResult(page * size);
        query.setMaxResults(size);

        List<UserProfile> users = query.getResultList();

        // 総件数カウント
        StringBuffer countJpql = new StringBuffer("SELECT COUNT(u) FROM UserProfile u WHERE 1=1");
        if (status != null && !status.isEmpty()) {
            countJpql.append(" AND u.active = :active");
        }
        var countQuery = em.createQuery(countJpql.toString());
        if (status != null && !status.isEmpty()) {
            countQuery.setParameter("active", "ACTIVE".equalsIgnoreCase(status));
        }
        long totalElements = (Long) countQuery.getSingleResult();

        List<UserProfileDto> dtoList = new ArrayList<>();
        for (int i = 0; i < users.size(); i++) {
            dtoList.add(toDto(users.get(i)));
        }

        PageResultDto<UserProfileDto> pageResult = new PageResultDto<>(dtoList, totalElements, page, size);
        return Response.ok(pageResult).build();
    }

    /**
     * ロール一覧を取得する。
     *
     * @return ロール一覧
     */
    @GET
    @Path("/roles")
    @SuppressWarnings("unchecked")
    public Response listRoles() {
        logger.info("ロール一覧取得。");

        List<Role> roles = em.createQuery(
                "SELECT DISTINCT r FROM Role r LEFT JOIN FETCH r.permissions ORDER BY r.name",
                Role.class).getResultList();

        List<Map<String, Object>> dtoList = new ArrayList<>();
        for (Role role : roles) {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", role.getId());
            dto.put("name", role.getName());
            dto.put("description", role.getDescription());
            List<String> permNames = new ArrayList<>();
            try {
                if (role.getPermissions() != null) {
                    role.getPermissions().forEach(p -> permNames.add(p.getName()));
                }
            } catch (Exception e) {
                logger.warning("パーミッション取得エラー。ロール: " + role.getName());
            }
            dto.put("permissions", permNames);
            dtoList.add(dto);
        }

        return Response.ok(dtoList).build();
    }

    /**
     * ユーザー詳細を取得する。
     *
     * <p>【技術的負債 #12】Keycloak IDを含むエンティティ情報がDTOに
     * 露出するリスクがある。toDto()でフィルタしているが不完全。</p>
     *
     * @param id ユーザーID
     * @return ユーザーDTO
     */
    @GET
    @Path("/{id}")
    public Response getUser(@PathParam("id") Long id) {
        logger.info("ユーザー詳細取得。ID=" + id);

        List<UserProfile> userResults = em.createQuery(
                "SELECT u FROM UserProfile u LEFT JOIN FETCH u.department WHERE u.id = :id",
                UserProfile.class)
                .setParameter("id", id)
                .getResultList();
        if (userResults.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"ユーザーが見つかりません。ID: " + id + "\"}")
                    .build();
        }

        UserProfileDto dto = toDto(userResults.get(0));
        return Response.ok(dto).build();
    }

    /**
     * 現在ログイン中のユーザー情報を取得する。
     *
     * <p>SecurityContextからKeycloak IDを取得し、
     * 対応するUserProfileを返す。</p>
     *
     * @param secCtx セキュリティコンテキスト
     * @return 現在のユーザーDTO
     */
    @GET
    @Path("/me")
    public Response getCurrentUser(@Context SecurityContext secCtx) {
        String keycloakId = secCtx.getUserPrincipal().getName();
        logger.info("現在のユーザー情報取得。keycloakId=" + keycloakId);

        List<UserProfile> results = em.createQuery(
                "SELECT u FROM UserProfile u LEFT JOIN FETCH u.department WHERE u.keycloakId = :keycloakId",
                UserProfile.class)
                .setParameter("keycloakId", keycloakId)
                .getResultList();

        if (results.isEmpty()) {
            // 技術的負債: Keycloakには存在するがDBにプロファイルがないケース
            Map<String, String> fallback = new HashMap<>();
            fallback.put("username", keycloakId);
            fallback.put("message", "プロファイルが未登録です。");
            return Response.ok(fallback).build();
        }

        UserProfileDto dto = toDto(results.get(0));
        return Response.ok(dto).build();
    }

    // ========================================================================
    // プロファイル更新
    // ========================================================================

    /**
     * ユーザープロファイルを更新する。
     *
     * <p>技術的負債 #5: EntityManagerで直接更新している。</p>
     *
     * @param id     ユーザーID
     * @param dto    更新データ
     * @param secCtx セキュリティコンテキスト
     * @return 更新後のユーザーDTO
     */
    @PUT
    @Path("/{id}")
    public Response updateProfile(@PathParam("id") Long id, UserProfileDto dto,
                                  @Context SecurityContext secCtx) {
        logger.info("プロファイル更新。ID=" + id
                + ", ユーザー=" + secCtx.getUserPrincipal().getName());

        UserProfile user = em.find(UserProfile.class, id);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"ユーザーが見つかりません。ID: " + id + "\"}")
                    .build();
        }

        // 技術的負債: 更新権限チェックが不完全（自分自身のプロファイルのみ更新可能にすべき）
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getDisplayName() != null) {
            // 表示名からfirstName/lastNameへの分割は未対応
            // 技術的負債: displayNameの分割ロジックが未実装
        }

        em.merge(user);

        UserProfileDto resultDto = toDto(user);
        return Response.ok(resultDto).build();
    }

    // ========================================================================
    // ロール割当
    // ========================================================================

    @POST
    @Path("/{userId}/roles")
    public Response assignRole(@PathParam("userId") Long userId,
                               Map<String, Object> body,
                               @Context SecurityContext secCtx) {
        logger.info("ロール割当。ユーザーID=" + userId
                + ", 実行者=" + secCtx.getUserPrincipal().getName());

        UserProfile user = em.find(UserProfile.class, userId);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"ユーザーが見つかりません。ID: " + userId + "\"}")
                    .build();
        }

        Long roleId = Long.parseLong(body.get("roleId").toString());
        Role role = em.find(Role.class, roleId);
        if (role == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"ロールが見つかりません。ID: " + roleId + "\"}")
                    .build();
        }

        user.getRoles().add(role);
        em.merge(user);

        UserProfileDto dto = toDto(user);
        return Response.ok(dto).build();
    }

    @POST
    @Path("/roles")
    public Response createRole(Map<String, Object> body,
                               @Context SecurityContext secCtx) {
        logger.info("ロール作成。ユーザー=" + secCtx.getUserPrincipal().getName());

        String name = body.get("name") != null ? body.get("name").toString() : null;
        if (name == null || name.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"ロール名は必須です。\"}").build();
        }

        Role role = new Role();
        role.setName(name);
        role.setDescription(body.get("description") != null ? body.get("description").toString() : null);
        em.persist(role);

        Map<String, Object> dto = new HashMap<>();
        dto.put("id", role.getId());
        dto.put("name", role.getName());
        dto.put("description", role.getDescription());
        dto.put("permissions", new ArrayList<>());
        return Response.status(Response.Status.CREATED).entity(dto).build();
    }

    @PUT
    @Path("/roles/{roleId}")
    public Response updateRole(@PathParam("roleId") Long roleId,
                               Map<String, Object> body,
                               @Context SecurityContext secCtx) {
        logger.info("ロール更新。ID=" + roleId);

        Role role = em.find(Role.class, roleId);
        if (role == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"ロールが見つかりません。\"}").build();
        }

        if (body.get("name") != null) {
            role.setName(body.get("name").toString());
        }
        if (body.get("description") != null) {
            role.setDescription(body.get("description").toString());
        }
        if (body.get("permissions") != null && body.get("permissions") instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> permNames = (List<String>) body.get("permissions");
            role.getPermissions().clear();
            for (String permName : permNames) {
                List<Permission> perms = em.createNamedQuery("Permission.findByCode", Permission.class)
                        .setParameter("code", permName)
                        .getResultList();
                if (!perms.isEmpty()) {
                    role.getPermissions().add(perms.get(0));
                }
            }
        }
        em.merge(role);

        Map<String, Object> dto = new HashMap<>();
        dto.put("id", role.getId());
        dto.put("name", role.getName());
        dto.put("description", role.getDescription());
        List<String> permNames = new ArrayList<>();
        try {
            role.getPermissions().forEach(p -> permNames.add(p.getName()));
        } catch (Exception ignored) {}
        dto.put("permissions", permNames);
        return Response.ok(dto).build();
    }

    @DELETE
    @Path("/roles/{roleId}")
    public Response deleteRole(@PathParam("roleId") Long roleId,
                               @Context SecurityContext secCtx) {
        logger.info("ロール削除。ID=" + roleId);

        Role role = em.find(Role.class, roleId);
        if (role == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"ロールが見つかりません。\"}").build();
        }

        em.remove(role);
        return Response.noContent().build();
    }

    // ========================================================================
    // 部門別ユーザー
    // ========================================================================

    /**
     * 部門別ユーザー一覧を取得する。
     *
     * @param departmentId 部門ID
     * @return ユーザー一覧
     */
    @GET
    @Path("/by-department/{departmentId}")
    @SuppressWarnings("unchecked")
    public Response getUsersByDepartment(@PathParam("departmentId") Long departmentId) {
        logger.info("部門別ユーザー取得。部門ID=" + departmentId);

        List<UserProfile> users = em.createNamedQuery("UserProfile.findByDepartment")
                .setParameter("departmentId", departmentId)
                .getResultList();

        List<UserProfileDto> dtoList = new ArrayList<>();
        for (int i = 0; i < users.size(); i++) {
            dtoList.add(toDto(users.get(i)));
        }

        return Response.ok(dtoList).build();
    }

    // ========================================================================
    // ヘルパーメソッド
    // ========================================================================

    /**
     * UserProfileエンティティをDTOに変換する。
     *
     * <p>【技術的負債 #10】MapStructマッパーが存在しないため手動変換。</p>
     *
     * <p>【技術的負債 #12】Keycloak IDをusernameとして露出している。
     * セキュリティ上、内部IDを公開すべきでない。</p>
     *
     * @param entity ユーザーエンティティ
     * @return ユーザーDTO
     */
    private UserProfileDto toDto(UserProfile entity) {
        if (entity == null) {
            return null;
        }

        UserProfileDto dto = new UserProfileDto();
        dto.setId(entity.getId());
        dto.setUsername(entity.getKeycloakId());
        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());
        dto.setDisplayName(entity.getLastName() + " " + entity.getFirstName());
        dto.setEmail(entity.getEmail());
        dto.setActive(entity.isActive());
        dto.setEnabled(entity.isActive());
        dto.setCreatedAt(entity.getCreatedAt());

        if (entity.getDepartment() != null) {
            dto.setDepartmentId(entity.getDepartment().getId());
            dto.setDepartmentName(entity.getDepartment().getName());
            dto.setDepartment(entity.getDepartment().getName());
        }

        try {
            if (entity.getRoles() != null) {
                List<String> names = new ArrayList<>();
                List<Map<String, Object>> roleObjects = new ArrayList<>();
                for (var role : entity.getRoles()) {
                    names.add(role.getName());
                    Map<String, Object> roleMap = new LinkedHashMap<>();
                    roleMap.put("id", role.getId() != null ? role.getId().toString() : "");
                    roleMap.put("name", role.getName());
                    roleMap.put("description", role.getDescription() != null ? role.getDescription() : "");
                    List<String> permNames = new ArrayList<>();
                    try {
                        if (role.getPermissions() != null) {
                            role.getPermissions().forEach(p -> permNames.add(p.getName()));
                        }
                    } catch (Exception ignored) {
                    }
                    roleMap.put("permissions", permNames);
                    roleObjects.add(roleMap);
                }
                dto.setRoleNames(names);
                dto.setRoles(roleObjects);
            }
        } catch (Exception e) {
            logger.warning("ロール取得エラー。ユーザーID: " + entity.getId());
        }

        return dto;
    }
}
