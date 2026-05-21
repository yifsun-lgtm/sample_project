package com.proquip.ejb.interceptor;

import jakarta.annotation.Priority;
import jakarta.annotation.Resource;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.transaction.TransactionSynchronizationRegistry;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * トランザクションログ記録インターセプター。
 *
 * <p>EJBメソッドのトランザクション開始・コミット・ロールバックをログに記録する。
 * トランザクションの状態遷移を追跡するためのデバッグ／監視用インターセプター。</p>
 *
 * <p>技術的負債:
 * <ul>
 *   <li>{@link SimpleDateFormat} をインスタンスフィールドとして保持しているが、
 *       {@code SimpleDateFormat} はスレッドセーフではないため、
 *       マルチスレッド環境で不正な日時文字列が生成される可能性がある。
 *       {@code java.time.format.DateTimeFormatter} に移行すべき。</li>
 *   <li>文字列連結（{@code +}演算子）を多用しており、パラメータ化ログ出力に移行すべき。
 *       現状ではログレベルがDISABLEDの場合でも文字列連結のコストが発生する。</li>
 * </ul>
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 100)
public class TransactionLoggingInterceptor implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = Logger.getLogger(TransactionLoggingInterceptor.class.getName());

    /**
     * 日時フォーマッター。
     * 技術的負債 #6: SimpleDateFormatはスレッドセーフではない。
     * 複数のスレッドが同時にこのインターセプターを通過すると、
     * 日時文字列が破損する可能性がある。
     * DateTimeFormatterを使用すべき。
     */
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    @Resource
    private TransactionSynchronizationRegistry txRegistry;

    /**
     * メソッド呼び出しをインターセプトし、トランザクション状態をログに記録する。
     *
     * <p>技術的負債: 文字列連結によるログ出力を使用しているため、
     * ログレベルが無効な場合でも連結コストが発生する。
     * SLF4Jやパラメータ化ログに移行すべき。</p>
     *
     * @param ctx インターセプター呼び出しコンテキスト
     * @return インターセプト対象メソッドの戻り値
     * @throws Exception メソッド実行中に発生した例外
     */
    @AroundInvoke
    public Object logTransaction(InvocationContext ctx) throws Exception {
        String className = ctx.getTarget().getClass().getSimpleName();
        String methodName = ctx.getMethod().getName();

        // 技術的負債: 文字列連結によるログ出力（パラメータ化ログに移行すべき）
        // 技術的負債: SimpleDateFormat.format()はスレッドセーフではない
        String timestamp = dateFormat.format(new Date());
        String transactionKey = getTransactionKey();

        // 技術的負債: ログレベル確認なしの文字列連結
        logger.info("[TX-BEGIN] " + timestamp + " | "
                + "トランザクション: " + transactionKey + " | "
                + "クラス: " + className + " | "
                + "メソッド: " + methodName + " | "
                + "パラメータ数: " + (ctx.getParameters() != null ? ctx.getParameters().length : 0));

        Object result = null;
        boolean committed = false;

        try {
            result = ctx.proceed();
            committed = true;

            // 技術的負債: 文字列連結（パラメータ化ログに移行すべき）
            String completionTimestamp = dateFormat.format(new Date());
            logger.info("[TX-COMMIT] " + completionTimestamp + " | "
                    + "トランザクション: " + transactionKey + " | "
                    + "クラス: " + className + " | "
                    + "メソッド: " + methodName + " | "
                    + "結果: " + (result != null ? result.getClass().getSimpleName() : "void") + " | "
                    + "ステータス: 正常完了");

            return result;
        } catch (Exception e) {
            // 技術的負債: 文字列連結（パラメータ化ログに移行すべき）
            String errorTimestamp = dateFormat.format(new Date());
            logger.log(Level.SEVERE,
                    "[TX-ROLLBACK] " + errorTimestamp + " | "
                    + "トランザクション: " + transactionKey + " | "
                    + "クラス: " + className + " | "
                    + "メソッド: " + methodName + " | "
                    + "例外: " + e.getClass().getName() + " | "
                    + "メッセージ: " + e.getMessage() + " | "
                    + "ステータス: ロールバック",
                    e);

            throw e;
        } finally {
            if (!committed) {
                // 技術的負債: 文字列連結
                String finalTimestamp = dateFormat.format(new Date());
                logger.warning("[TX-FINAL] " + finalTimestamp + " | "
                        + "トランザクション: " + transactionKey + " | "
                        + "メソッド: " + className + "." + methodName + " | "
                        + "ステータス: 未コミット（ロールバックの可能性あり）");
            }
        }
    }

    /**
     * 現在のトランザクションキーを取得する。
     *
     * @return トランザクションキー文字列。取得不可の場合は "UNKNOWN"
     */
    private String getTransactionKey() {
        try {
            if (txRegistry != null) {
                Object key = txRegistry.getTransactionKey();
                return key != null ? key.toString() : "NO_TX";
            }
        } catch (Exception e) {
            logger.fine("トランザクションキーの取得に失敗しました: " + e.getMessage());
        }
        return "UNKNOWN";
    }
}
