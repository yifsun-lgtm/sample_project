package com.proquip.common.dto.admin;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * インポートジョブデータ転送オブジェクト。
 *
 * <p>CSVインポートなどのバッチインポート処理の状態を保持する。
 * ジョブの進捗、処理結果、エラー情報を含む。</p>
 *
 * @author ProQuip開発チーム
 */
public class ImportJobDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** ジョブID */
    private String jobId;

    /** エンティティ種別（PRODUCT, SUPPLIER, INVENTORY） */
    private String entityType;

    /** ファイル名 */
    private String fileName;

    /** ステータス（PENDING, RUNNING, COMPLETED, FAILED, CANCELLED） */
    private String status;

    /** 総行数 */
    private int totalRows;

    /** 処理済み行数 */
    private int processedRows;

    /** 成功件数 */
    private int successCount;

    /** エラー件数 */
    private int errorCount;

    /** 進捗率（パーセンテージ） */
    private double progress;

    /** 開始日時 */
    private Date startedAt;

    /** 完了日時 */
    private Date completedAt;

    /** 実行ユーザー名 */
    private String startedBy;

    /** エラー詳細一覧 */
    private List<String> errors = new ArrayList<>();

    /** エラーレポートURL */
    private String errorReportUrl;

    /**
     * デフォルトコンストラクタ。
     */
    public ImportJobDto() {
    }

    // --- Getter / Setter ---

    /**
     * ジョブIDを返す。
     *
     * @return ジョブID
     */
    public String getJobId() {
        return jobId;
    }

    /**
     * ジョブIDを設定する。
     *
     * @param jobId ジョブID
     */
    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    /**
     * エンティティ種別を返す。
     *
     * @return エンティティ種別
     */
    public String getEntityType() {
        return entityType;
    }

    /**
     * エンティティ種別を設定する。
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
     * ステータスを返す。
     *
     * @return ステータス
     */
    public String getStatus() {
        return status;
    }

    /**
     * ステータスを設定する。
     *
     * @param status ステータス
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 総行数を返す。
     *
     * @return 総行数
     */
    public int getTotalRows() {
        return totalRows;
    }

    /**
     * 総行数を設定する。
     *
     * @param totalRows 総行数
     */
    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    /**
     * 処理済み行数を返す。
     *
     * @return 処理済み行数
     */
    public int getProcessedRows() {
        return processedRows;
    }

    /**
     * 処理済み行数を設定する。
     *
     * @param processedRows 処理済み行数
     */
    public void setProcessedRows(int processedRows) {
        this.processedRows = processedRows;
    }

    /**
     * 成功件数を返す。
     *
     * @return 成功件数
     */
    public int getSuccessCount() {
        return successCount;
    }

    /**
     * 成功件数を設定する。
     *
     * @param successCount 成功件数
     */
    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    /**
     * エラー件数を返す。
     *
     * @return エラー件数
     */
    public int getErrorCount() {
        return errorCount;
    }

    /**
     * エラー件数を設定する。
     *
     * @param errorCount エラー件数
     */
    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    /**
     * 進捗率を返す。
     *
     * @return 進捗率（0.0〜100.0）
     */
    public double getProgress() {
        return progress;
    }

    /**
     * 進捗率を設定する。
     *
     * @param progress 進捗率（0.0〜100.0）
     */
    public void setProgress(double progress) {
        this.progress = progress;
    }

    /**
     * 開始日時を返す。
     *
     * @return 開始日時
     */
    public Date getStartedAt() {
        return startedAt;
    }

    /**
     * 開始日時を設定する。
     *
     * @param startedAt 開始日時
     */
    public void setStartedAt(Date startedAt) {
        this.startedAt = startedAt;
    }

    /**
     * 完了日時を返す。
     *
     * @return 完了日時
     */
    public Date getCompletedAt() {
        return completedAt;
    }

    /**
     * 完了日時を設定する。
     *
     * @param completedAt 完了日時
     */
    public void setCompletedAt(Date completedAt) {
        this.completedAt = completedAt;
    }

    /**
     * 実行ユーザー名を返す。
     *
     * @return 実行ユーザー名
     */
    public String getStartedBy() {
        return startedBy;
    }

    /**
     * 実行ユーザー名を設定する。
     *
     * @param startedBy 実行ユーザー名
     */
    public void setStartedBy(String startedBy) {
        this.startedBy = startedBy;
    }

    /**
     * エラー詳細一覧を返す。
     *
     * @return エラーメッセージのリスト
     */
    public List<String> getErrors() {
        return errors;
    }

    /**
     * エラー詳細一覧を設定する。
     *
     * @param errors エラーメッセージのリスト
     */
    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    /**
     * エラーレポートURLを返す。
     *
     * @return エラーレポートURL
     */
    public String getErrorReportUrl() {
        return errorReportUrl;
    }

    /**
     * エラーレポートURLを設定する。
     *
     * @param errorReportUrl エラーレポートURL
     */
    public void setErrorReportUrl(String errorReportUrl) {
        this.errorReportUrl = errorReportUrl;
    }
}
