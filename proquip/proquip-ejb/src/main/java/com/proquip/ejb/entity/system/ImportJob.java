package com.proquip.ejb.entity.system;

import com.proquip.ejb.entity.base.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import java.util.Date;

/**
 * データインポートジョブを表すエンティティ。
 *
 * <p>CSV/Excel等によるデータ一括取込処理の実行状況と結果を管理する。
 * 商品マスタ、仕入先マスタ、在庫データ等の一括インポートに使用される。</p>
 *
 * <p>技術的負債:
 * <ul>
 *   <li>{@code entityType} / {@code status} フィールドが文字列型で定義されている。
 *       本来はEnumを使用すべき。</li>
 *   <li>{@code filePath} がファイルシステムの絶対パスを前提としている。
 *       オブジェクトストレージ（S3等）への移行を考慮していない。</li>
 *   <li>{@code errorDetails} にスタックトレース全体をテキストとして保存しており、
 *       構造化されたエラー情報（JSON等）に移行すべき。</li>
 *   <li>{@link #getProgressPercentage()} にゼロ除算のリスクがある。</li>
 *   <li>{@code findByEntityType} クエリがJOIN FETCHを使用していない。</li>
 * </ul>
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "import_job")
@NamedQueries({
    @NamedQuery(
        name = "ImportJob.findByStatus",
        query = "SELECT j FROM ImportJob j WHERE j.status = :status ORDER BY j.createdAt DESC"
    ),
    // 技術的負債: N+1問題のリスクあり（関連データのJOIN FETCHなし）
    @NamedQuery(
        name = "ImportJob.findByEntityType",
        query = "SELECT j FROM ImportJob j WHERE j.entityType = :entityType ORDER BY j.createdAt DESC"
    ),
    @NamedQuery(
        name = "ImportJob.findRecentJobs",
        query = "SELECT j FROM ImportJob j ORDER BY j.createdAt DESC"
    )
})
public class ImportJob extends AuditableEntity {

    private static final long serialVersionUID = 1L;

    /** ジョブ名（管理用の表示名） */
    @Column(name = "job_code", nullable = false, length = 200)
    private String jobName;

    /**
     * インポート対象エンティティ種別。
     * 技術的負債: 文字列で管理している。Enum型に移行すべき。
     * 想定値: "PRODUCT", "SUPPLIER", "INVENTORY"
     */
    @Column(name = "import_type", nullable = false, length = 50)
    private String entityType;

    /** インポートファイル名（アップロード時のオリジナルファイル名） */
    @Column(name = "file_name", nullable = false, length = 300)
    private String fileName;

    /**
     * インポートファイルの保存パス。
     * 技術的負債: ローカルファイルシステムの絶対パスを前提としている。
     * オブジェクトストレージ（S3等）への移行時に大幅な改修が必要。
     */
    @Column(name = "file_url", length = 500)
    private String filePath;

    /** ファイルサイズ（バイト） */
    @Column(name = "file_size")
    private Long fileSize;

    /**
     * ジョブステータス。
     * 技術的負債: 文字列で管理している。Enum型に移行すべき。
     * 想定値: "PENDING", "PROCESSING", "COMPLETED", "FAILED", "CANCELLED"
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /** 総レコード数 */
    @Column(name = "total_records")
    private Integer totalRecords;

    /** 処理済みレコード数 */
    @Column(name = "processed_records")
    private Integer processedRecords;

    /** エラーレコード数 */
    @Column(name = "error_records")
    private Integer errorRecords;

    /**
     * エラー詳細。
     * 技術的負債: スタックトレース全体をプレーンテキストとして格納している。
     * 構造化されたエラー情報（JSON配列等）に移行すべき。
     */
    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;

    /** ジョブ開始日時 */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "started_at")
    private Date startedAt;

    /** ジョブ完了日時 */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "completed_at")
    private Date completedAt;

    /** ジョブ実行者のユーザー名 */
    @Column(name = "started_by", nullable = false, length = 100)
    private String startedBy;

    /**
     * デフォルトコンストラクタ。
     */
    public ImportJob() {
        super();
    }

    // --- ビジネスメソッド ---

    /**
     * インポート処理の進捗率（パーセント）を返す。
     *
     * <p>技術的負債: totalRecordsが0またはnullの場合にゼロ除算が発生する可能性がある。
     * 呼び出し元でnullチェックを行う必要があるが、ドキュメントに記載されていない。</p>
     *
     * @return 進捗率（0〜100）
     */
    public int getProgressPercentage() {
        // 技術的負債: totalRecords == 0 の場合に ArithmeticException が発生する
        if (processedRecords == null || totalRecords == null) {
            return 0;
        }
        return (processedRecords * 100) / totalRecords;
    }

    // --- Getter / Setter ---

    /**
     * ジョブ名を返す。
     *
     * @return ジョブ名
     */
    public String getJobName() {
        return jobName;
    }

    /**
     * ジョブ名を設定する。
     *
     * @param jobName ジョブ名
     */
    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    /**
     * インポート対象エンティティ種別を返す。
     * 技術的負債: 文字列型。Enumに移行予定。
     *
     * @return エンティティ種別（"PRODUCT" / "SUPPLIER" / "INVENTORY"）
     */
    public String getEntityType() {
        return entityType;
    }

    /**
     * インポート対象エンティティ種別を設定する。
     *
     * @param entityType エンティティ種別
     */
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    /**
     * ファイル名を返す。
     *
     * @return ファイル名
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * ファイル名を設定する。
     *
     * @param fileName ファイル名
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * ファイルパスを返す。
     * 技術的負債: ローカルファイルシステムの絶対パスを前提としている。
     *
     * @return ファイルパス
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * ファイルパスを設定する。
     *
     * @param filePath ファイルパス
     */
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    /**
     * ファイルサイズ（バイト）を返す。
     *
     * @return ファイルサイズ
     */
    public Long getFileSize() {
        return fileSize;
    }

    /**
     * ファイルサイズ（バイト）を設定する。
     *
     * @param fileSize ファイルサイズ
     */
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    /**
     * ジョブステータスを返す。
     * 技術的負債: 文字列型。Enumに移行予定。
     *
     * @return ステータス（"PENDING" / "PROCESSING" / "COMPLETED" / "FAILED" / "CANCELLED"）
     */
    public String getStatus() {
        return status;
    }

    /**
     * ジョブステータスを設定する。
     *
     * @param status ステータス
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 総レコード数を返す。
     *
     * @return 総レコード数
     */
    public Integer getTotalRecords() {
        return totalRecords;
    }

    /**
     * 総レコード数を設定する。
     *
     * @param totalRecords 総レコード数
     */
    public void setTotalRecords(Integer totalRecords) {
        this.totalRecords = totalRecords;
    }

    /**
     * 処理済みレコード数を返す。
     *
     * @return 処理済みレコード数
     */
    public Integer getProcessedRecords() {
        return processedRecords;
    }

    /**
     * 処理済みレコード数を設定する。
     *
     * @param processedRecords 処理済みレコード数
     */
    public void setProcessedRecords(Integer processedRecords) {
        this.processedRecords = processedRecords;
    }

    /**
     * エラーレコード数を返す。
     *
     * @return エラーレコード数
     */
    public Integer getErrorRecords() {
        return errorRecords;
    }

    /**
     * エラーレコード数を設定する。
     *
     * @param errorRecords エラーレコード数
     */
    public void setErrorRecords(Integer errorRecords) {
        this.errorRecords = errorRecords;
    }

    /**
     * エラー詳細を返す。
     * 技術的負債: スタックトレース全体がテキストとして格納されている。
     *
     * @return エラー詳細
     */
    public String getErrorDetails() {
        return errorDetails;
    }

    /**
     * エラー詳細を設定する。
     *
     * @param errorDetails エラー詳細
     */
    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }

    /**
     * ジョブ開始日時を返す。
     *
     * @return 開始日時
     */
    public Date getStartedAt() {
        return startedAt;
    }

    /**
     * ジョブ開始日時を設定する。
     *
     * @param startedAt 開始日時
     */
    public void setStartedAt(Date startedAt) {
        this.startedAt = startedAt;
    }

    /**
     * ジョブ完了日時を返す。
     *
     * @return 完了日時
     */
    public Date getCompletedAt() {
        return completedAt;
    }

    /**
     * ジョブ完了日時を設定する。
     *
     * @param completedAt 完了日時
     */
    public void setCompletedAt(Date completedAt) {
        this.completedAt = completedAt;
    }

    /**
     * ジョブ実行者のユーザー名を返す。
     *
     * @return 実行者のユーザー名
     */
    public String getStartedBy() {
        return startedBy;
    }

    /**
     * ジョブ実行者のユーザー名を設定する。
     *
     * @param startedBy 実行者のユーザー名
     */
    public void setStartedBy(String startedBy) {
        this.startedBy = startedBy;
    }

    @Override
    public String toString() {
        return "ImportJob{" +
                "id=" + getId() +
                ", jobName='" + jobName + '\'' +
                ", entityType='" + entityType + '\'' +
                ", fileName='" + fileName + '\'' +
                ", status='" + status + '\'' +
                ", totalRecords=" + totalRecords +
                ", processedRecords=" + processedRecords +
                ", errorRecords=" + errorRecords +
                '}';
    }
}
