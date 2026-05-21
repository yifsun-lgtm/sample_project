package com.proquip.web.resource;

import com.proquip.common.constant.AppConstants;
import com.proquip.ejb.entity.system.AuditLog;
import com.proquip.ejb.entity.system.ImportJob;
import com.proquip.ejb.entity.system.SystemConfiguration;
import com.proquip.ejb.service.AuditServiceBean;
import com.proquip.ejb.service.ImportExportServiceBean;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

import com.proquip.ejb.entity.organization.Permission;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 管理者用RESTリソース。
 *
 * <p>監査ログ検索、システム設定管理、データインポート処理を提供する。</p>
 *
 * <p>【技術的負債 #7】
 * ロールチェック（@RolesAllowed）が欠如しており、すべての認証済みユーザーが
 * 管理機能にアクセスできてしまう。セキュリティレビューで要対処。</p>
 *
 * <p>【技術的負債 #5】
 * EntityManagerを直接使用してシステム設定やインポートジョブを操作している。
 * 専用のサービスBeanを作成すべき。</p>
 *
 * <p>【技術的負債 #6】
 * 日付パースに{@link SimpleDateFormat}を使用している。
 * java.time.LocalDateに移行すべき。</p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
@Path("/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
public class AdminResource {

    private static final Logger logger = Logger.getLogger(AdminResource.class.getName());

    @Inject
    private AuditServiceBean auditService;

    @Inject
    private ImportExportServiceBean importExportService;

    /**
     * 技術的負債 #5: EntityManagerを直接注入。
     * SystemConfigurationServiceBean、ImportJobServiceBeanが存在しないため。
     */
    @PersistenceContext
    private EntityManager em;

    // ========================================================================
    // 監査ログ
    // ========================================================================

    /**
     * 監査ログを検索する。
     *
     * <p>【技術的負債 #6】日付パースにSimpleDateFormatを使用。</p>
     *
     * @param entityType  エンティティ種別フィルタ
     * @param action      操作種別フィルタ
     * @param performedBy 操作実行者フィルタ
     * @param fromDate    検索開始日（yyyy-MM-dd形式）
     * @param toDate      検索終了日（yyyy-MM-dd形式）
     * @return 監査ログ一覧
     */
    @GET
    @Path("/audit-logs")
    @RolesAllowed({"ADMIN", "MANAGER"})
    public Response getAuditLogs(
            @QueryParam("entityType") String entityType,
            @QueryParam("action") String action,
            @QueryParam("performedBy") String performedBy,
            @QueryParam("fromDate") String fromDate,
            @QueryParam("toDate") String toDate) {

        logger.info("監査ログ検索。entityType=" + entityType + ", action=" + action
                + ", performedBy=" + performedBy);

        // 技術的負債 #6: SimpleDateFormatを使用
        Date from = parseDate(fromDate);
        Date to = parseDate(toDate);

        List<AuditLog> logs = auditService.searchAuditLogs(
                entityType, action, performedBy, from, to);

        return Response.ok(logs).build();
    }

    /**
     * 監査ログの統計情報を取得する。
     *
     * <p>【技術的負債 #12】Map&lt;String, Object&gt;で応答を返す。</p>
     *
     * @return 監査統計情報
     */
    @GET
    @Path("/audit-logs/statistics")
    @RolesAllowed({"ADMIN", "MANAGER"})
    public Response getAuditStatistics() {
        logger.info("監査ログ統計取得。");

        Map<String, Object> stats = auditService.getAuditStatistics();
        return Response.ok(stats).build();
    }

    /**
     * 監査ログをCSV形式でエクスポートする。
     *
     * @param entityType  エンティティ種別フィルタ
     * @param performedBy 操作実行者フィルタ
     * @param fromDate    検索開始日
     * @param toDate      検索終了日
     * @return CSV形式のレスポンス
     */
    @GET
    @Path("/audit-logs/export")
    @Produces("text/csv")
    @RolesAllowed({"ADMIN", "MANAGER"})
    public Response exportAuditLogs(
            @QueryParam("entityType") String entityType,
            @QueryParam("performedBy") String performedBy,
            @QueryParam("fromDate") String fromDate,
            @QueryParam("toDate") String toDate) {

        logger.info("監査ログエクスポート。entityType=" + entityType);

        Date from = parseDate(fromDate);
        Date to = parseDate(toDate);

        List<AuditLog> logs = auditService.searchAuditLogs(
                entityType, null, performedBy, from, to);

        String csv = auditService.exportToCsv(logs);
        return Response.ok(csv)
                .header("Content-Disposition", "attachment; filename=\"audit-logs.csv\"")
                .build();
    }

    // ========================================================================
    // システム設定
    // ========================================================================

    /**
     * システム設定一覧を取得する。
     *
     * <p>【技術的負債 #5】EntityManagerで直接クエリを発行している。
     * 【技術的負債 #7】ロールチェックなし。管理者のみアクセス可能にすべき。</p>
     *
     * @param category カテゴリフィルタ
     * @return システム設定一覧
     */
    @GET
    @Path("/system-config")
    @SuppressWarnings("unchecked")
    public Response getSystemConfig(
            @QueryParam("category") String category) {

        logger.info("システム設定取得。category=" + category);

        List<SystemConfiguration> configs;
        if (category != null && !category.isEmpty()) {
            configs = em.createNamedQuery("SystemConfiguration.findByCategory")
                    .setParameter("category", category)
                    .getResultList();
        } else {
            configs = em.createNamedQuery("SystemConfiguration.findEditableConfigs")
                    .getResultList();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (SystemConfiguration c : configs) {
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("key", c.getConfigKey());
            dto.put("value", c.getConfigValue());
            dto.put("description", c.getDescription());
            dto.put("category", translateConfigGroup(c.getCategory()));
            dto.put("updatedAt", c.getUpdatedAt());
            dto.put("updatedBy", c.getLastModifiedBy());
            result.add(dto);
        }

        return Response.ok(result).build();
    }

    /**
     * システム設定を更新する。
     *
     * <p>【技術的負債 #7】ロールチェックなし。管理者のみ更新可能にすべき。</p>
     *
     * @param key    設定キー
     * @param body   更新データ（value フィールド）
     * @param secCtx セキュリティコンテキスト
     * @return 更新結果
     */
    @PUT
    @Path("/system-config/{key}")
    @SuppressWarnings("unchecked")
    public Response updateSystemConfig(@PathParam("key") String key,
                                       Map<String, Object> body,
                                       @Context SecurityContext secCtx) {
        logger.info("システム設定更新。key=" + key
                + ", ユーザー=" + secCtx.getUserPrincipal().getName());

        // 技術的負債 #5: EntityManager直接使用
        List<SystemConfiguration> configs = em.createNamedQuery("SystemConfiguration.findByKey")
                .setParameter("configKey", key)
                .getResultList();

        if (configs.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"設定が見つかりません。キー: " + key + "\"}")
                    .build();
        }

        SystemConfiguration config = configs.get(0);

        // 編集可能チェック
        if (!Boolean.TRUE.equals(config.getEditable())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "この設定は編集できません。キー: " + key);
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }

        String newValue = body != null && body.get("value") != null
                ? body.get("value").toString() : null;
        config.setConfigValue(newValue);
        config.setLastModifiedBy(secCtx.getUserPrincipal().getName());
        em.merge(config);

        Map<String, String> result = new HashMap<>();
        result.put("message", "設定を更新しました。キー: " + key);
        return Response.ok(result).build();
    }

    // ========================================================================
    // データエクスポート
    // ========================================================================

    @GET
    @Path("/export/{type}")
    @Produces("text/csv")
    public Response exportMasterData(@PathParam("type") String type) {
        logger.info("マスターデータエクスポート。type=" + type);

        String csv;
        String fileName;
        switch (type.toLowerCase()) {
            case "products":
            case "product":
                csv = importExportService.exportProducts();
                fileName = "products_export.csv";
                break;
            case "suppliers":
            case "supplier":
                csv = importExportService.exportSuppliers();
                fileName = "suppliers_export.csv";
                break;
            default:
                return Response.status(Response.Status.BAD_REQUEST)
                        .type(MediaType.APPLICATION_JSON)
                        .entity("{\"error\":\"未対応のエクスポートタイプ: " + type + "\"}")
                        .build();
        }

        return Response.ok(csv)
                .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                .build();
    }

    // ========================================================================
    // データインポート
    // ========================================================================

    /**
     * データインポートジョブを作成する。
     *
     * <p>【技術的負債 #5】EntityManagerで直接永続化している。
     * 【技術的負債 #7】ロールチェックなし。</p>
     *
     * @param body   インポートデータ（entityType, fileName, filePath）
     * @param secCtx セキュリティコンテキスト
     * @return 作成されたインポートジョブ
     */
    @POST
    @Path("/import")
    public Response createImportJob(Map<String, Object> body,
                                    @Context SecurityContext secCtx) {
        logger.info("インポートジョブ作成。ユーザー=" + secCtx.getUserPrincipal().getName());

        String entityType = body != null && body.get("entityType") != null
                ? body.get("entityType").toString() : null;
        String fileName = body != null && body.get("fileName") != null
                ? body.get("fileName").toString() : null;
        String filePath = body != null && body.get("filePath") != null
                ? body.get("filePath").toString() : null;

        if (entityType == null || fileName == null || filePath == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "entityType, fileName, filePathは必須です。");
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }

        // 技術的負債 #5: EntityManager直接使用
        ImportJob job = new ImportJob();
        job.setJobName(entityType + "インポート: " + fileName);
        job.setEntityType(entityType);
        job.setFileName(fileName);
        job.setFilePath(filePath);
        job.setStatus("PENDING");
        job.setStartedBy(secCtx.getUserPrincipal().getName());

        em.persist(job);

        return Response.status(Response.Status.CREATED).entity(job).build();
    }

    /**
     * インポートジョブ一覧を取得する。
     *
     * @param status ステータスフィルタ
     * @return インポートジョブ一覧
     */
    @GET
    @Path("/import-jobs")
    @SuppressWarnings("unchecked")
    public Response getImportJobs(@QueryParam("status") String status) {
        logger.info("インポートジョブ一覧取得。status=" + status);

        List<ImportJob> jobs;
        if (status != null && !status.isEmpty()) {
            jobs = em.createNamedQuery("ImportJob.findByStatus")
                    .setParameter("status", status)
                    .getResultList();
        } else {
            jobs = em.createNamedQuery("ImportJob.findRecentJobs")
                    .setMaxResults(50)
                    .getResultList();
        }

        return Response.ok(jobs).build();
    }

    /**
     * インポートジョブ詳細を取得する。
     *
     * @param id ジョブID
     * @return インポートジョブ
     */
    @GET
    @Path("/import-jobs/{id}")
    public Response getImportJob(@PathParam("id") Long id) {
        logger.info("インポートジョブ詳細取得。ID=" + id);

        ImportJob job = em.find(ImportJob.class, id);
        if (job == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"インポートジョブが見つかりません。ID: " + id + "\"}")
                    .build();
        }

        return Response.ok(job).build();
    }

    // ========================================================================
    // ヘルスチェック
    // ========================================================================

    @GET
    @Path("/health")
    public Response getHealth() {
        logger.info("ヘルスチェック実行。");

        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("timestamp", new Date());

        try {
            em.createNativeQuery("SELECT 1").getSingleResult();
            health.put("database", "UP");
        } catch (Exception e) {
            health.put("database", "DOWN");
            health.put("status", "DEGRADED");
        }

        return Response.ok(health).build();
    }

    // ========================================================================
    // パーミッション管理
    // ========================================================================

    @GET
    @Path("/permissions")
    @SuppressWarnings("unchecked")
    public Response getPermissions() {
        logger.info("パーミッション一覧取得。");

        List<Permission> permissions = em.createNamedQuery("Permission.findAll")
                .getResultList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Permission p : permissions) {
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("id", p.getId());
            dto.put("code", p.getCode());
            dto.put("name", p.getName());
            dto.put("resource", p.getResource());
            dto.put("action", p.getAction());
            result.add(dto);
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
    private String translateConfigGroup(String group) {
        if (group == null) return "一般";
        switch (group) {
            case "GENERAL": return "一般";
            case "PROCUREMENT": return "調達";
            case "INVENTORY": return "在庫";
            case "NOTIFICATION": return "通知";
            case "APPROVAL": return "承認";
            case "SECURITY": return "セキュリティ";
            case "UI": return "画面";
            default: return group;
        }
    }

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
