package com.proquip.common.exception;

/**
 * ビジネスロジックエラーの基底例外クラス。
 *
 * <p>アプリケーション固有のビジネスルール違反が発生した場合にスローされる。
 * すべてのビジネス例外はこのクラスを継承する。</p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** エラーコード（画面表示やログ出力で使用） */
    private final String errorCode;

    /**
     * エラーコードとメッセージを指定して例外を生成する。
     *
     * @param errorCode エラーコード
     * @param message エラーメッセージ
     */
    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * エラーコード・メッセージ・原因例外を指定して例外を生成する。
     *
     * @param errorCode エラーコード
     * @param message エラーメッセージ
     * @param cause 原因となった例外
     */
    public BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * エラーコードを返す。
     *
     * @return エラーコード
     */
    public String getErrorCode() {
        return errorCode;
    }
}
