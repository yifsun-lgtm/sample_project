package com.proquip.common.exception;

/**
 * 承認ワークフローに関するエラー時にスローされる例外。
 *
 * <p>承認権限がない場合や、ステータス不正による承認操作の失敗時に使用する。</p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
public class ApprovalException extends BusinessException {

    private static final long serialVersionUID = 1L;

    /**
     * エラーメッセージを指定して例外を生成する。
     *
     * @param message エラーメッセージ
     */
    public ApprovalException(String message) {
        super("APPROVAL_ERROR", message);
    }

    /**
     * エラーメッセージと原因例外を指定して例外を生成する。
     *
     * @param message エラーメッセージ
     * @param cause 原因となった例外
     */
    public ApprovalException(String message, Throwable cause) {
        super("APPROVAL_ERROR", message, cause);
    }
}
