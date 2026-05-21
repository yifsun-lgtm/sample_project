package com.proquip.ejb.interceptor;

import com.proquip.ejb.entity.system.AuditLog;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 監査ログ記録インターセプター。
 *
 * <p>{@link Audited} アノテーションが付与されたメソッドの呼び出しをインターセプトし、
 * メソッド名、パラメータ、戻り値、実行時間を {@link AuditLog} エンティティとして
 * データベースに記録する。</p>
 *
 * <p>技術的負債:
 * <ul>
 *   <li>タイムスタンプに {@link java.util.Date} を使用している。
 *       {@code java.time.Instant} に移行すべき。</li>
 *   <li>例外を広範に {@code catch(Exception)} で捕捉し、一部のパスで
 *       適切に再スローしていない。</li>
 *   <li>パラメータの文字列変換時に {@code Arrays.toString()} を使用しており、
 *       大きなオブジェクトがログに含まれる場合にパフォーマンス問題が発生する。</li>
 * </ul>
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Interceptor
@Audited
@Priority(Interceptor.Priority.APPLICATION)
public class AuditInterceptor implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = Logger.getLogger(AuditInterceptor.class.getName());

    /** 監査ログの最大パラメータ文字列長 */
    private static final int MAX_PARAM_LENGTH = 500;

    @PersistenceContext(unitName = "proquipPU")
    private EntityManager em;

    /**
     * メソッド呼び出しをインターセプトし、監査ログを記録する。
     *
     * <p>メソッドの実行前後でタイムスタンプを取得し、実行時間を計算する。
     * メソッド名、パラメータ、戻り値、実行時間を監査ログとして永続化する。</p>
     *
     * <p>技術的負債: 例外のキャッチが広すぎ、一部のパスで再スローされていない。
     * これにより、実際のビジネスエラーが握りつぶされる可能性がある。</p>
     *
     * @param ctx インターセプター呼び出しコンテキスト
     * @return インターセプト対象メソッドの戻り値
     * @throws Exception メソッド実行中に発生した例外
     */
    @AroundInvoke
    public Object intercept(InvocationContext ctx) throws Exception {
        String className = ctx.getTarget().getClass().getSimpleName();
        String methodName = ctx.getMethod().getName();
        String fullMethodName = className + "." + methodName;

        // 技術的負債: java.util.Dateを使用。java.time.Instantに移行すべき
        Date startTime = new Date();
        long startMillis = System.currentTimeMillis();

        String parameterString = truncateString(
                Arrays.toString(ctx.getParameters()), MAX_PARAM_LENGTH);

        logger.info("監査ログ: メソッド開始 - " + fullMethodName + " パラメータ: " + parameterString);

        Object result = null;
        boolean success = false;

        try {
            result = ctx.proceed();
            success = true;
            return result;
        } catch (Exception e) {
            // 技術的負債 #7: Exceptionを広範にキャッチしている
            logger.log(Level.SEVERE, "監査ログ: メソッド実行中にエラー発生 - " + fullMethodName, e);

            // 監査ログにエラー情報を記録
            try {
                saveAuditLog(fullMethodName, parameterString, "ERROR: " + e.getMessage(),
                        startTime, System.currentTimeMillis() - startMillis);
            } catch (Exception logError) {
                // 技術的負債 #7: ログ保存時のエラーを握りつぶしている
                logger.log(Level.WARNING, "監査ログの保存に失敗しました", logError);
            }

            // 技術的負債 #7: このthrowは実行されるが、finallyブロックでも
            // ログ保存を試みるため、二重ログのリスクがある
            throw e;
        } finally {
            long executionTime = System.currentTimeMillis() - startMillis;

            if (success) {
                String resultString = result != null
                        ? truncateString(result.toString(), MAX_PARAM_LENGTH)
                        : "null";

                logger.info("監査ログ: メソッド完了 - " + fullMethodName
                        + " 戻り値: " + resultString
                        + " 実行時間: " + executionTime + "ms");

                try {
                    saveAuditLog(fullMethodName, parameterString, resultString,
                            startTime, executionTime);
                } catch (Exception e) {
                    // 技術的負債 #7: 監査ログ保存失敗を握りつぶしている
                    // 本来は何らかの代替手段（ファイルログ等）で記録すべき
                    logger.log(Level.WARNING, "監査ログの保存に失敗しました: " + fullMethodName, e);
                }
            }
        }
    }

    /**
     * 監査ログをデータベースに保存する。
     *
     * @param methodName メソッド名
     * @param parameters パラメータ文字列
     * @param result     戻り値文字列
     * @param timestamp  実行開始日時
     * @param duration   実行時間（ミリ秒）
     */
    private void saveAuditLog(String methodName, String parameters, String result,
                              Date timestamp, long duration) {
        AuditLog auditLog = new AuditLog();
        auditLog.setEntityType("METHOD_INVOCATION");
        auditLog.setEntityId(0L);
        auditLog.setAction("INVOKE");
        auditLog.setPerformedBy(0L);
        // 技術的負債: java.util.Dateを直接使用
        auditLog.setPerformedAt(timestamp);
        auditLog.setAdditionalInfo(
                "method=" + methodName
                + ", params=" + parameters
                + ", result=" + result
                + ", duration=" + duration + "ms"
        );

        em.persist(auditLog);
    }

    /**
     * 文字列を指定された最大長に切り詰める。
     *
     * @param str    元の文字列
     * @param maxLen 最大長
     * @return 切り詰められた文字列
     */
    private String truncateString(String str, int maxLen) {
        if (str == null) {
            return "null";
        }
        if (str.length() <= maxLen) {
            return str;
        }
        return str.substring(0, maxLen) + "...(truncated)";
    }
}
