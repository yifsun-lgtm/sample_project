package com.proquip.web.filter;

import com.proquip.common.dto.ApiErrorResponse;
import com.proquip.common.exception.ValidationException;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * バリデーション例外専用のマッパー。
 *
 * <p>{@link ValidationException} を422 Unprocessable Entityレスポンスに変換する。
 * バリデーション違反の詳細メッセージを {@link ApiErrorResponse#getDetails()} に含める。</p>
 *
 * <p>技術的負債: {@link GlobalExceptionMapper} 内にもValidationExceptionの
 * 処理があり、このクラスと重複している。JAX-RSの仕様上、より具体的な
 * ExceptionMapperが優先されるため動作に問題はないが、コードの整理が必要。</p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ValidationException> {

    private static final Logger logger = Logger.getLogger(ValidationExceptionMapper.class.getName());

    /**
     * バリデーション例外をHTTPレスポンスに変換する。
     *
     * @param exception バリデーション例外
     * @return 422 Unprocessable Entity レスポンス
     */
    @Override
    public Response toResponse(ValidationException exception) {
        logger.log(Level.INFO, "バリデーションエラー: {0}", exception.getMessage());

        ApiErrorResponse error = new ApiErrorResponse(
                422, exception.getErrorCode(), exception.getMessage());
        error.setDetails(exception.getViolations());

        return Response.status(422)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
