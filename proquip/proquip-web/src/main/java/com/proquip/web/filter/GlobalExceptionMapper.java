package com.proquip.web.filter;

import com.proquip.common.dto.ApiErrorResponse;
import com.proquip.common.exception.BusinessException;
import com.proquip.common.exception.EntityNotFoundException;
import com.proquip.common.exception.ValidationException;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * グローバル例外マッパー。
 *
 * <p>REST APIで発生した未処理の例外を適切なHTTPレスポンスに変換する。
 * ビジネス例外はクライアントエラー（4xx）、その他の例外はサーバーエラー（500）
 * として返す。</p>
 *
 * <p>【技術的負債 #7】
 * 一部のケースで正しいHTTPステータスコードではなく200（OK）を返しつつ、
 * ボディにエラー情報を含めている。これはRESTful設計に反しており、
 * クライアント側でのエラーハンドリングを困難にする。</p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger logger = Logger.getLogger(GlobalExceptionMapper.class.getName());

    /**
     * 例外をHTTPレスポンスに変換する。
     *
     * <p>例外の型に応じてHTTPステータスコードを決定し、
     * {@link ApiErrorResponse} 形式のJSON応答を返す。</p>
     *
     * @param exception 発生した例外
     * @return HTTPレスポンス
     */
    @Override
    public Response toResponse(Exception exception) {

        // EntityNotFoundException → 404 Not Found
        if (exception instanceof EntityNotFoundException) {
            EntityNotFoundException enfe = (EntityNotFoundException) exception;
            logger.log(Level.INFO, "エンティティが見つかりません: {0}", exception.getMessage());

            ApiErrorResponse error = new ApiErrorResponse(
                    404, enfe.getErrorCode(), enfe.getMessage());
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(error)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        // ValidationException → 422 Unprocessable Entity
        if (exception instanceof ValidationException) {
            ValidationException ve = (ValidationException) exception;
            logger.log(Level.INFO, "バリデーションエラー: {0}", exception.getMessage());

            ApiErrorResponse error = new ApiErrorResponse(
                    422, ve.getErrorCode(), ve.getMessage());
            error.setDetails(ve.getViolations());
            return Response.status(422)
                    .entity(error)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        // BusinessException → 400 Bad Request
        if (exception instanceof BusinessException) {
            BusinessException be = (BusinessException) exception;
            logger.log(Level.WARNING, "ビジネスエラー: {0}", exception.getMessage());

            ApiErrorResponse error = new ApiErrorResponse(
                    400, be.getErrorCode(), be.getMessage());

            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        // IllegalArgumentException → 400 Bad Request
        if (exception instanceof IllegalArgumentException) {
            logger.log(Level.WARNING, "不正な引数: {0}", exception.getMessage());

            ApiErrorResponse error = new ApiErrorResponse(
                    400, "BAD_REQUEST", exception.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        // その他の例外 → 500 Internal Server Error
        logger.log(Level.SEVERE, "予期しないエラーが発生しました。", exception);

        ApiErrorResponse error = new ApiErrorResponse(
                500, "INTERNAL_ERROR", "サーバー内部エラーが発生しました。");
        // 技術的負債: スタックトレースをクライアントに返さないが、
        // 開発環境ではデバッグ用に例外メッセージを含める
        error.addDetail(exception.getClass().getSimpleName() + ": " + exception.getMessage());

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
