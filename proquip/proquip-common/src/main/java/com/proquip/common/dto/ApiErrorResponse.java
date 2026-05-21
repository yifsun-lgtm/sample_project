package com.proquip.common.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * APIエラー応答データ転送オブジェクト。
 *
 * <p>REST APIのエラー応答を統一的なフォーマットで返すためのクラス。
 * HTTPステータス、エラーコード、メッセージ、詳細情報、タイムスタンプを含む。</p>
 *
 * @author ProQuip開発チーム
 */
public class ApiErrorResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** HTTPステータスコード */
    private int status;

    /** アプリケーション固有のエラーコード */
    private String errorCode;

    /** エラーメッセージ */
    private String message;

    /** 詳細情報のリスト（バリデーションエラー等） */
    private List<String> details = new ArrayList<>();

    /** エラー発生日時 */
    private Date timestamp;

    /**
     * デフォルトコンストラクタ。
     * タイムスタンプを現在時刻で初期化する。
     */
    public ApiErrorResponse() {
        this.timestamp = new Date();
    }

    /**
     * 基本パラメータを指定するコンストラクタ。
     *
     * @param status    HTTPステータスコード
     * @param errorCode アプリケーション固有のエラーコード
     * @param message   エラーメッセージ
     */
    public ApiErrorResponse(int status, String errorCode, String message) {
        this.status = status;
        this.errorCode = errorCode;
        this.message = message;
        this.timestamp = new Date();
    }

    // --- Getter / Setter ---

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getDetails() {
        return details;
    }

    public void setDetails(List<String> details) {
        this.details = details;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * 詳細情報を追加する。
     *
     * @param detail 追加する詳細情報
     */
    public void addDetail(String detail) {
        this.details.add(detail);
    }
}
