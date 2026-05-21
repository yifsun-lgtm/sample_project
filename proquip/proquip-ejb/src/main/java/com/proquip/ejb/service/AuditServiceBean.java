package com.proquip.ejb.service;

import com.proquip.common.constant.AppConstants;
import com.proquip.common.util.JsonHelper;
import com.proquip.ejb.entity.system.AuditLog;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 監査ログ管理サービスBean。
 * <p>
 * エンティティの変更操作（作成・更新・削除）の監査ログを記録・検索する。
 * セキュリティ監査およびデータ変更追跡のために使用される。
 * </p>
 *
 * <p>【技術的負債】
 * <ul>
 *   <li>手動のJSON文字列構築（JsonHelperに依存）。Jakarta JSON-Bに移行すべき。</li>
 *   <li>CSV出力でStringBufferを使用（pre-Java 5パターン）。</li>
 *   <li>検索メソッドが文字列連結でJPQLを構築している。Criteria APIを使うべき。</li>
 *   <li>監査ログの書き込みが非同期化されておらず、パフォーマンスに影響を与える。</li>
 * </ul>
 * </p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
@Stateless
public class AuditServiceBean {

    private static final Logger logger = Logger.getLogger(AuditServiceBean.class.getName());

    @PersistenceContext
    private EntityManager em;

    // ========================================================================
    // 監査ログ記録
    // ========================================================================

    /**
     * 操作ログを記録する。
     *
     * <p>エンティティに対する操作（CREATE/UPDATE/DELETE）を監査ログとして永続化する。</p>
     *
     * @param entityType エンティティ種別（例: "PurchaseOrder", "Product"）
     * @param entityId エンティティID
     * @param action 操作種別（"CREATE", "UPDATE", "DELETE"）
     * @param performedBy 操作実行者のユーザー名
     * @param oldValues 変更前の値（JSON文字列）
     * @param newValues 変更後の値（JSON文字列）
     */
    public void logAction(String entityType, Long entityId, String action,
                          String performedBy, String oldValues, String newValues) {
        try {
            AuditLog log = new AuditLog();
            log.setEntityType(entityType != null ? entityType : "UNKNOWN");
            log.setEntityId(entityId != null ? entityId : 0L);
            log.setAction(action != null ? action : "UNKNOWN");
            log.setPerformedBy(parseUserId(performedBy));
            log.setPerformedAt(new Date());
            log.setOldValues(ensureJson(oldValues));
            log.setNewValues(ensureJson(newValues));

            em.persist(log);
            em.flush();

            logger.fine("監査ログ記録。エンティティ: " + entityType + ", ID: " + entityId
                    + ", 操作: " + action);
        } catch (Exception e) {
            // 技術的負債 #7: 監査ログ記録失敗を握りつぶし
            // 監査ログの失敗が業務処理をブロックしないようにする設計だが、
            // ログが欠落するリスクがある
            logger.log(Level.SEVERE, "監査ログの記録に失敗しました。", e);
        }
    }

    /**
     * 操作ログを記録する（IPアドレス・ユーザーエージェント付き）。
     *
     * @param entityType エンティティ種別
     * @param entityId エンティティID
     * @param action 操作種別
     * @param performedBy 操作実行者
     * @param ipAddress 操作元IPアドレス
     * @param userAgent 操作元ユーザーエージェント
     * @param oldValues 変更前の値
     * @param newValues 変更後の値
     */
    public void logActionWithDetails(String entityType, Long entityId, String action,
                                     String performedBy, String ipAddress, String userAgent,
                                     String oldValues, String newValues) {
        try {
            AuditLog log = new AuditLog();
            log.setEntityType(entityType != null ? entityType : "UNKNOWN");
            log.setEntityId(entityId != null ? entityId : 0L);
            log.setAction(action != null ? action : "UNKNOWN");
            log.setPerformedBy(parseUserId(performedBy));
            log.setPerformedAt(new Date());
            log.setIpAddress(ipAddress);
            log.setUserAgent(userAgent);
            log.setOldValues(ensureJson(oldValues));
            log.setNewValues(ensureJson(newValues));

            em.persist(log);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "監査ログの記録に失敗しました。", e);
        }
    }

    /**
     * 変更前後の値をMapから監査ログ用JSON文字列に変換して記録する。
     *
     * <p>【技術的負債】JsonHelperの手動JSON構築に依存。
     * Jakarta JSON-B (Yasson) やJacksonに移行すべき。</p>
     *
     * @param entityType エンティティ種別
     * @param entityId エンティティID
     * @param action 操作種別
     * @param performedBy 操作実行者
     * @param oldValuesMap 変更前の値のMap
     * @param newValuesMap 変更後の値のMap
     */
    public void logActionWithMap(String entityType, Long entityId, String action,
                                String performedBy,
                                Map<String, Object> oldValuesMap,
                                Map<String, Object> newValuesMap) {
        // 技術的負債: JsonHelperの手動JSON構築を使用
        String oldJson = null;
        String newJson = null;

        if (oldValuesMap != null && !oldValuesMap.isEmpty()) {
            oldJson = JsonHelper.toJsonMap(oldValuesMap);
        }
        if (newValuesMap != null && !newValuesMap.isEmpty()) {
            newJson = JsonHelper.toJsonMap(newValuesMap);
        }

        logAction(entityType, entityId, action, performedBy, oldJson, newJson);
    }

    // ========================================================================
    // 監査ログ検索
    // ========================================================================

    /**
     * エンティティ種別で監査ログを検索する。
     *
     * @param entityType エンティティ種別
     * @return 監査ログのリスト
     */
    @SuppressWarnings("unchecked")
    public List<AuditLog> findByEntityType(String entityType) {
        if (entityType == null || entityType.isEmpty()) {
            return new ArrayList<AuditLog>();
        }

        return em.createNamedQuery("AuditLog.findByEntityType")
                .setParameter("entityType", entityType)
                .getResultList();
    }

    /**
     * 操作実行者で監査ログを検索する。
     *
     * @param performedBy 操作実行者
     * @return 監査ログのリスト
     */
    @SuppressWarnings("unchecked")
    public List<AuditLog> findByPerformedBy(String performedBy) {
        if (performedBy == null || performedBy.isEmpty()) {
            return new ArrayList<AuditLog>();
        }

        return em.createNamedQuery("AuditLog.findByPerformedBy")
                .setParameter("performedBy", performedBy)
                .getResultList();
    }

    /**
     * 日付範囲で監査ログを検索する。
     *
     * @param startDate 開始日
     * @param endDate 終了日
     * @return 監査ログのリスト
     */
    @SuppressWarnings("unchecked")
    public List<AuditLog> findByDateRange(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) {
            return new ArrayList<AuditLog>();
        }

        return em.createNamedQuery("AuditLog.findByDateRange")
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getResultList();
    }

    /**
     * 複合条件で監査ログを検索する。
     *
     * <p>【技術的負債 #11】文字列連結によるJPQL構築。
     * Criteria APIまたはQueryDSLに移行すべき。
     * SQLインジェクションのリスクはパラメータバインドにより軽減されているが、
     * コードの可読性が著しく低い。</p>
     *
     * @param entityType エンティティ種別（nullの場合は条件に含めない）
     * @param action 操作種別（nullの場合は条件に含めない）
     * @param performedBy 操作実行者（nullの場合は条件に含めない）
     * @param startDate 開始日（nullの場合は条件に含めない）
     * @param endDate 終了日（nullの場合は条件に含めない）
     * @return 監査ログのリスト
     */
    @SuppressWarnings("unchecked")
    public List<AuditLog> searchAuditLogs(String entityType, String action,
                                           String performedBy,
                                           Date startDate, Date endDate) {
        // 技術的負債 #11: StringBufferによるJPQL構築
        StringBuffer jpql = new StringBuffer();
        jpql.append("SELECT a FROM AuditLog a WHERE 1=1");

        if (entityType != null && !entityType.isEmpty()) {
            jpql.append(" AND a.entityType = :entityType");
        }
        if (action != null && !action.isEmpty()) {
            jpql.append(" AND a.action = :action");
        }
        if (performedBy != null && !performedBy.isEmpty()) {
            jpql.append(" AND a.performedBy = :performedBy");
        }
        if (startDate != null) {
            jpql.append(" AND a.performedAt >= :startDate");
        }
        if (endDate != null) {
            jpql.append(" AND a.performedAt <= :endDate");
        }

        jpql.append(" ORDER BY a.performedAt DESC");

        Query query = em.createQuery(jpql.toString());

        if (entityType != null && !entityType.isEmpty()) {
            query.setParameter("entityType", entityType);
        }
        if (action != null && !action.isEmpty()) {
            query.setParameter("action", action);
        }
        if (performedBy != null && !performedBy.isEmpty()) {
            query.setParameter("performedBy", performedBy);
        }
        if (startDate != null) {
            query.setParameter("startDate", startDate);
        }
        if (endDate != null) {
            query.setParameter("endDate", endDate);
        }

        return query.getResultList();
    }

    /**
     * 特定エンティティの変更履歴を取得する。
     *
     * @param entityType エンティティ種別
     * @param entityId エンティティID
     * @return 監査ログのリスト（時系列降順）
     */
    @SuppressWarnings("unchecked")
    public List<AuditLog> getAuditTrail(String entityType, Long entityId) {
        if (entityType == null || entityId == null) {
            return new ArrayList<AuditLog>();
        }

        return em.createQuery(
                "SELECT a FROM AuditLog a " +
                "WHERE a.entityType = :entityType AND a.entityId = :entityId " +
                "ORDER BY a.performedAt DESC")
                .setParameter("entityType", entityType)
                .setParameter("entityId", entityId)
                .getResultList();
    }

    // ========================================================================
    // CSV出力
    // ========================================================================

    /**
     * 監査ログをCSV形式で出力する。
     *
     * <p>【技術的負債 #11】StringBufferでCSVを手動構築。
     * OpenCSVやJakarta Beanの使用を検討すべき。
     * また、大量データの場合はストリーミング出力（OutputStreamWriter）を
     * 使うべきだが、ここではString全体を返している。</p>
     *
     * @param logs 出力対象の監査ログリスト
     * @return CSV文字列
     */
    public String exportToCsv(List<AuditLog> logs) {
        // 技術的負債 #11: StringBufferによるCSV構築
        StringBuffer csv = new StringBuffer();

        // ヘッダ行
        csv.append("ID,エンティティ種別,エンティティID,操作,実行者,実行日時,IPアドレス,変更前,変更後");
        csv.append("\r\n");

        if (logs == null || logs.isEmpty()) {
            return csv.toString();
        }

        // 技術的負債 #6: for-indexループ
        for (int i = 0; i < logs.size(); i++) {
            AuditLog log = logs.get(i);

            csv.append(log.getId() != null ? log.getId() : "");
            csv.append(",");
            csv.append(escapeCsvField(log.getEntityType()));
            csv.append(",");
            csv.append(log.getEntityId() != null ? log.getEntityId() : "");
            csv.append(",");
            csv.append(escapeCsvField(log.getAction()));
            csv.append(",");
            csv.append(log.getPerformedBy() != null ? log.getPerformedBy() : "");
            csv.append(",");
            csv.append(log.getPerformedAt() != null ? log.getPerformedAt().toString() : "");
            csv.append(",");
            csv.append(escapeCsvField(log.getIpAddress()));
            csv.append(",");
            // 技術的負債: oldValues/newValuesがJSONの場合、CSVフィールドとして
            // 適切にエスケープされない可能性がある
            csv.append(escapeCsvField(log.getOldValues()));
            csv.append(",");
            csv.append(escapeCsvField(log.getNewValues()));
            csv.append("\r\n");
        }

        return csv.toString();
    }

    // ========================================================================
    // クリーンアップ
    // ========================================================================

    /**
     * 古い監査ログを削除する。
     *
     * <p>保持期間（デフォルト365日）を超えたログを一括削除する。
     * スケジューラーからの定期実行を想定。</p>
     *
     * @param retentionDays 保持日数
     * @return 削除した件数
     */
    public int cleanupOldLogs(int retentionDays) {
        if (retentionDays <= 0) {
            retentionDays = 365; // デフォルト1年
        }

        // 技術的負債: Calendar APIを使用（java.time移行すべき）
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DAY_OF_MONTH, -retentionDays);
        Date cutoffDate = cal.getTime();

        int deleted = em.createQuery(
                "DELETE FROM AuditLog a WHERE a.performedAt < :cutoffDate")
                .setParameter("cutoffDate", cutoffDate)
                .executeUpdate();

        logger.info("古い監査ログのクリーンアップ完了。削除件数: " + deleted
                + ", 保持期間: " + retentionDays + "日");

        return deleted;
    }

    /**
     * 統計情報を取得する。
     *
     * <p>【技術的負債 #12】Map<String, Object>を返却しており、型安全でない。
     * 専用のDTOクラスを作成すべき。</p>
     *
     * @return 統計情報のMap
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getAuditStatistics() {
        Map<String, Object> stats = new HashMap<String, Object>();

        try {
            // 総件数
            Long totalCount = (Long) em.createQuery(
                    "SELECT COUNT(a) FROM AuditLog a")
                    .getSingleResult();
            stats.put("totalCount", totalCount);

            // 操作種別ごとの件数
            List<Object[]> actionCounts = em.createQuery(
                    "SELECT a.action, COUNT(a) FROM AuditLog a GROUP BY a.action")
                    .getResultList();

            Map<String, Long> actionCountMap = new HashMap<String, Long>();
            // 技術的負債 #6: for-indexループ
            for (int i = 0; i < actionCounts.size(); i++) {
                Object[] row = actionCounts.get(i);
                actionCountMap.put((String) row[0], (Long) row[1]);
            }
            stats.put("actionCounts", actionCountMap);

            // エンティティ種別ごとの件数
            List<Object[]> entityCounts = em.createQuery(
                    "SELECT a.entityType, COUNT(a) FROM AuditLog a GROUP BY a.entityType")
                    .getResultList();

            Map<String, Long> entityCountMap = new HashMap<String, Long>();
            for (int i = 0; i < entityCounts.size(); i++) {
                Object[] row = entityCounts.get(i);
                entityCountMap.put((String) row[0], (Long) row[1]);
            }
            stats.put("entityCounts", entityCountMap);

        } catch (Exception e) {
            logger.log(Level.WARNING, "監査統計情報の取得に失敗しました。", e);
        }

        return stats;
    }

    // ========================================================================
    // プライベートヘルパーメソッド
    // ========================================================================

    /**
     * CSVフィールドのエスケープ処理。
     *
     * <p>【技術的負債】CsvParser.escape()と同じ処理を重複実装している。
     * 共通ユーティリティを使うべき。</p>
     */
    private Long parseUserId(String performedBy) {
        if (performedBy == null || performedBy.isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(performedBy);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String ensureJson(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if ((trimmed.startsWith("{") && trimmed.endsWith("}"))
                || (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            return value;
        }
        return "{\"message\":\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
    }

    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }

        // 技術的負債: CsvParser.escape()の重複実装
        if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}
