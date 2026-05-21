package com.proquip.ejb.scheduler;

import com.proquip.ejb.entity.system.ImportJob;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * インポートジョブクリーンアップスケジューラ。
 *
 * <p>毎日午前3時に実行され、古いインポートジョブレコードおよび
 * 関連するインポートファイルを削除する。</p>
 *
 * <p>技術的負債:
 * <ul>
 *   <li>ファイルパスがハードコードされている ({@code /opt/proquip/imports/})。
 *       環境変数やシステム設定から読み込むべき。</li>
 *   <li>ファイル削除時のエラーハンドリングが最小限。
 *       削除失敗時のリトライや代替処理が実装されていない。</li>
 *   <li>大量ファイル削除時にI/O負荷が集中する可能性がある。</li>
 * </ul>
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Singleton
@Startup
public class ImportJobCleanupScheduler {

    private static final Logger logger = Logger.getLogger(ImportJobCleanupScheduler.class.getName());

    /**
     * インポートファイルの基本ディレクトリパス。
     * 技術的負債 #19: ハードコードされたファイルパス。
     * 環境変数やシステム設定から読み込むべき。
     * コンテナ化された環境ではマウントパスが異なる可能性がある。
     */
    private static final String IMPORT_BASE_PATH = "/opt/proquip/imports/";

    /** ジョブレコードの保持日数 */
    private static final int JOB_RETENTION_DAYS = 90;

    @PersistenceContext(unitName = "proquipPU")
    private EntityManager em;

    /**
     * 初期化処理。アプリケーション起動時に実行される。
     */
    @PostConstruct
    public void init() {
        logger.info("インポートジョブクリーンアップスケジューラが初期化されました");
    }

    /**
     * インポートジョブクリーンアップを実行するスケジュールメソッド。
     *
     * <p>毎日午前3時に実行される。保持期間を超えたジョブレコードと
     * 関連するファイルを削除する。</p>
     */
    @Schedule(hour = "3", minute = "0", persistent = false)
    public void cleanupImportJobs() {
        logger.info("インポートジョブクリーンアップを開始します");

        try {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -JOB_RETENTION_DAYS);
            Date cutoffDate = cal.getTime();

            // 古いジョブを検索
            TypedQuery<ImportJob> query = em.createQuery(
                    "SELECT j FROM ImportJob j "
                    + "WHERE j.createdAt < :cutoff "
                    + "AND j.status IN ('COMPLETED', 'FAILED', 'CANCELLED') "
                    + "ORDER BY j.createdAt ASC",
                    ImportJob.class);
            query.setParameter("cutoff", cutoffDate);

            List<ImportJob> oldJobs = query.getResultList();

            logger.info("クリーンアップ対象ジョブ数: " + oldJobs.size());

            int filesDeleted = 0;
            int jobsDeleted = 0;

            for (ImportJob job : oldJobs) {
                try {
                    // ファイル削除
                    boolean fileDeleted = deleteImportFile(job);
                    if (fileDeleted) {
                        filesDeleted++;
                    }

                    // レコード削除
                    em.remove(em.merge(job));
                    jobsDeleted++;

                } catch (Exception e) {
                    // ファイル削除のエラーハンドリングが最小限（技術的負債）
                    logger.log(Level.WARNING,
                            "ジョブクリーンアップ中にエラーが発生しました: " + job.getJobName(), e);
                }
            }

            logger.info("インポートジョブクリーンアップ完了。ジョブ削除: " + jobsDeleted
                    + "件, ファイル削除: " + filesDeleted + "件");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "インポートジョブクリーンアップ全体でエラーが発生しました", e);
        }
    }

    /**
     * インポートジョブに関連するファイルを削除する。
     *
     * <p>技術的負債 #19: ファイルパスがハードコードされた基本ディレクトリに依存。
     * ファイル削除時のエラーハンドリングが最小限で、リトライ処理なし。</p>
     *
     * @param job 対象のインポートジョブ
     * @return ファイルが正常に削除された場合 {@code true}
     */
    private boolean deleteImportFile(ImportJob job) {
        if (job.getFilePath() == null || job.getFilePath().trim().isEmpty()) {
            return false;
        }

        // 技術的負債 #19: ハードコードされたパスとの結合
        // セキュリティ: パストラバーサル攻撃への対策が不十分
        String filePath = job.getFilePath();
        if (!filePath.startsWith(IMPORT_BASE_PATH)) {
            logger.warning("インポートファイルパスが基本ディレクトリ外です: " + filePath);
            return false;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            logger.fine("インポートファイルが存在しません（既に削除済み？）: " + filePath);
            return false;
        }

        // ファイル削除（技術的負債: エラーハンドリングが最小限）
        boolean deleted = file.delete();
        if (deleted) {
            logger.info("インポートファイルを削除しました: " + filePath);
        } else {
            // 技術的負債: 削除失敗時のリトライや代替処理がない
            logger.warning("インポートファイルの削除に失敗しました: " + filePath);
        }

        return deleted;
    }
}
