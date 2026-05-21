package com.proquip.common.dto.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * バッチ操作の結果を表すDTOクラス。
 *
 * <p>一括インポートや一括ステータス変更など、複数件をまとめて処理する
 * 操作の結果（成功件数、失敗件数、エラー詳細）を保持する。</p>
 *
 * @author ProQuip開発チーム
 */
public class BatchOperationResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 成功件数 */
    private int successCount;

    /** 失敗件数 */
    private int failCount;

    /** 処理対象の総件数 */
    private int totalCount;

    /** エラー詳細一覧 */
    private List<String> errors = new ArrayList<>();

    /** 警告一覧 */
    private List<String> warnings = new ArrayList<>();

    /**
     * デフォルトコンストラクタ。
     */
    public BatchOperationResult() {
    }

    // --- Getter / Setter ---

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
     * 失敗件数を返す。
     *
     * @return 失敗件数
     */
    public int getFailCount() {
        return failCount;
    }

    /**
     * 失敗件数を設定する。
     *
     * @param failCount 失敗件数
     */
    public void setFailCount(int failCount) {
        this.failCount = failCount;
    }

    /**
     * 処理対象の総件数を返す。
     *
     * @return 総件数
     */
    public int getTotalCount() {
        return totalCount;
    }

    /**
     * 処理対象の総件数を設定する。
     *
     * @param totalCount 総件数
     */
    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
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
     * 警告一覧を返す。
     *
     * @return 警告メッセージのリスト
     */
    public List<String> getWarnings() {
        return warnings;
    }

    /**
     * 警告一覧を設定する。
     *
     * @param warnings 警告メッセージのリスト
     */
    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
}
