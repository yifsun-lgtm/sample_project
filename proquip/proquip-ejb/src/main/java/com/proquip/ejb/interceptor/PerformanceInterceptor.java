package com.proquip.ejb.interceptor;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * パフォーマンス監視インターセプター。
 *
 * <p>{@link Monitored} アノテーションが付与されたメソッドの実行時間を計測し、
 * 閾値（デフォルト1000ミリ秒）を超える場合は警告ログを出力する。</p>
 *
 * <p>技術的負債:
 * <ul>
 *   <li>{@link System#currentTimeMillis()} を使用して実行時間を計測している。
 *       ナノ秒精度が必要な場合は {@link System#nanoTime()} を使用すべき。
 *       {@code currentTimeMillis()} はシステムクロック依存のため、
 *       NTP調整等で不正確になる可能性がある。</li>
 *   <li>閾値がハードコードされている。設定ファイルやシステム設定から読み込むべき。</li>
 * </ul>
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Interceptor
@Monitored
@Priority(Interceptor.Priority.APPLICATION + 100)
public class PerformanceInterceptor implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = Logger.getLogger(PerformanceInterceptor.class.getName());

    /**
     * スロークエリ警告の閾値（ミリ秒）。
     * 技術的負債: ハードコードされている。設定から読み込むべき。
     */
    private static final long SLOW_THRESHOLD_MS = 1000;

    /**
     * メソッド呼び出しをインターセプトし、実行時間を計測する。
     *
     * <p>技術的負債: {@code System.currentTimeMillis()} を使用している。
     * モノトニッククロックではないため、システムクロック変更の影響を受ける。
     * {@code System.nanoTime()} を使用すべき。</p>
     *
     * @param ctx インターセプター呼び出しコンテキスト
     * @return インターセプト対象メソッドの戻り値
     * @throws Exception メソッド実行中に発生した例外
     */
    @AroundInvoke
    public Object measurePerformance(InvocationContext ctx) throws Exception {
        String className = ctx.getTarget().getClass().getSimpleName();
        String methodName = ctx.getMethod().getName();
        String fullMethodName = className + "." + methodName;

        // 技術的負債 #6: System.currentTimeMillis()を使用。System.nanoTime()を使うべき。
        // currentTimeMillis()はウォールクロック時刻であり、NTP同期等で
        // 巻き戻りが発生すると負の実行時間が計測される可能性がある。
        long startTime = System.currentTimeMillis();

        try {
            Object result = ctx.proceed();

            long executionTime = System.currentTimeMillis() - startTime;
            logExecutionTime(fullMethodName, executionTime);

            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.log(Level.WARNING,
                    "パフォーマンス監視: メソッド " + fullMethodName
                    + " が例外で終了しました。実行時間: " + executionTime + "ms",
                    e);
            throw e;
        }
    }

    /**
     * 実行時間をログに記録する。閾値を超えた場合は警告レベルで出力する。
     *
     * @param methodName メソッド名（クラス名.メソッド名形式）
     * @param executionTime 実行時間（ミリ秒）
     */
    private void logExecutionTime(String methodName, long executionTime) {
        if (executionTime > SLOW_THRESHOLD_MS) {
            logger.warning("パフォーマンス警告: メソッド " + methodName
                    + " の実行に " + executionTime + "ms かかりました。"
                    + " 閾値: " + SLOW_THRESHOLD_MS + "ms");
        } else {
            logger.fine("パフォーマンス監視: メソッド " + methodName
                    + " 実行時間: " + executionTime + "ms");
        }
    }
}
