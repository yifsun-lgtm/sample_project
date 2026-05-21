package com.proquip.ejb.scheduler;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 通知クリーンアップスケジューラ。
 *
 * <p>毎日午前2時に実行され、古い通知レコードを削除する。
 * 既読通知は90日、未読通知は365日を経過したものが削除対象となる。</p>
 *
 * <p>技術的負債:
 * <ul>
 *   <li>ネイティブSQLのDELETE文を使用している。JPQLの一括削除クエリに移行すべき。
 *       ネイティブSQLはデータベース方言に依存し、ポータビリティが低い。</li>
 *   <li>削除閾値（90日、365日）がハードコードされている。</li>
 *   <li>大量レコード削除時のトランザクションサイズが考慮されていない。
 *       バッチ処理で分割削除すべき。</li>
 * </ul>
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Singleton
@Startup
public class NotificationCleanupScheduler {

    private static final Logger logger = Logger.getLogger(NotificationCleanupScheduler.class.getName());

    /** 既読通知の保持日数 */
    private static final int READ_RETENTION_DAYS = 90;

    /** 未読通知の保持日数 */
    private static final int UNREAD_RETENTION_DAYS = 365;

    @PersistenceContext(unitName = "proquipPU")
    private EntityManager em;

    /**
     * 初期化処理。アプリケーション起動時に実行される。
     */
    @PostConstruct
    public void init() {
        logger.info("通知クリーンアップスケジューラが初期化されました");
    }

    /**
     * 通知クリーンアップを実行するスケジュールメソッド。
     *
     * <p>毎日午前2時に実行される。期限切れの通知レコードを削除する。</p>
     *
     * <p>技術的負債 #18: ネイティブSQLを使用しているため、
     * データベースの変更時にクエリの修正が必要。</p>
     */
    @Schedule(hour = "2", minute = "0", persistent = false)
    public void cleanupNotifications() {
        logger.info("通知クリーンアップを開始します");

        try {
            int deletedRead = deleteReadNotifications();
            int deletedUnread = deleteUnreadNotifications();

            logger.info("通知クリーンアップ完了。既読削除: " + deletedRead
                    + "件, 未読削除: " + deletedUnread + "件");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "通知クリーンアップ中にエラーが発生しました", e);
        }
    }

    /**
     * 古い既読通知を削除する。
     *
     * <p>技術的負債 #18: ネイティブSQLのDELETE文を使用。
     * JPQLの {@code DELETE FROM Notification n WHERE ...} に移行すべき。</p>
     *
     * @return 削除されたレコード数
     */
    private int deleteReadNotifications() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -READ_RETENTION_DAYS);
        Date cutoffDate = cal.getTime();

        // 技術的負債 #18: ネイティブSQLの使用
        // JPQLでは: DELETE FROM Notification n WHERE n.status = 'READ' AND n.readAt < :cutoff
        Query query = em.createNativeQuery(
                "DELETE FROM notification WHERE status = 'READ' AND read_at < ?1");
        query.setParameter(1, cutoffDate);

        int deleted = query.executeUpdate();

        logger.info("既読通知削除: " + deleted + "件 (基準日: " + cutoffDate + ")");
        return deleted;
    }

    /**
     * 古い未読通知を削除する。
     *
     * <p>技術的負債 #18: ネイティブSQLのDELETE文を使用。</p>
     *
     * @return 削除されたレコード数
     */
    private int deleteUnreadNotifications() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -UNREAD_RETENTION_DAYS);
        Date cutoffDate = cal.getTime();

        // 技術的負債 #18: ネイティブSQLの使用
        Query query = em.createNativeQuery(
                "DELETE FROM notification WHERE status = 'UNREAD' AND created_at < ?1");
        query.setParameter(1, cutoffDate);

        int deleted = query.executeUpdate();

        logger.info("未読通知削除: " + deleted + "件 (基準日: " + cutoffDate + ")");
        return deleted;
    }
}
