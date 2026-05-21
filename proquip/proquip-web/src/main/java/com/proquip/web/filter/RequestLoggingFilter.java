package com.proquip.web.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

/**
 * リクエスト/レスポンスのログ出力フィルタ。
 *
 * <p>全てのREST APIリクエストに対して、メソッド・URI・レスポンスステータス・
 * 処理時間をログ出力する。パフォーマンス監視やデバッグに使用する。</p>
 *
 * <p>【技術的負債 #6 - スレッドセーフでないSimpleDateFormat】
 * {@link SimpleDateFormat} をインスタンスフィールドとして保持しているが、
 * このクラスはJAX-RSランタイムからシングルトンとして扱われるため、
 * 複数スレッドから同時にアクセスされるとフォーマット結果が破損する可能性がある。
 * {@code java.time.format.DateTimeFormatter}（スレッドセーフ）に移行すべき。</p>
 *
 * <p>【技術的負債】
 * ログメッセージを文字列連結（+演算子）で構築しており、
 * ログレベルが無効な場合でも文字列構築コストが発生する。
 * ロガーのパラメータ化メッセージを使用すべき。</p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
@Provider
public class RequestLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger logger = Logger.getLogger(RequestLoggingFilter.class.getName());

    /** リクエスト開始時刻の属性キー */
    private static final String START_TIME_PROPERTY = "proquip.request.startTime";

    /**
     * 技術的負債 #6: SimpleDateFormatはスレッドセーフでない。
     * シングルトンフィルタでインスタンスフィールドとして保持すると、
     * マルチスレッド環境で日付フォーマットが破損するリスクがある。
     */
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * リクエスト受信時のログ出力と開始時刻の記録。
     *
     * @param requestContext リクエストコンテキスト
     * @throws IOException I/Oエラー時
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // リクエスト開始時刻を記録
        requestContext.setProperty(START_TIME_PROPERTY, System.currentTimeMillis());

        // 技術的負債: 文字列連結によるログメッセージ構築
        String method = requestContext.getMethod();
        String uri = requestContext.getUriInfo().getRequestUri().toString();
        String timestamp = dateFormat.format(new Date());

        // 技術的負債: ログレベルに関係なく文字列連結が実行される
        logger.info("[" + timestamp + "] >>> " + method + " " + uri
                + " | Content-Type: " + requestContext.getHeaderString("Content-Type")
                + " | User-Agent: " + requestContext.getHeaderString("User-Agent"));
    }

    /**
     * レスポンス送信時のログ出力（ステータスコードと処理時間を含む）。
     *
     * @param requestContext  リクエストコンテキスト
     * @param responseContext レスポンスコンテキスト
     * @throws IOException I/Oエラー時
     */
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {

        // 処理時間の計算
        long duration = 0;
        Object startTimeObj = requestContext.getProperty(START_TIME_PROPERTY);
        if (startTimeObj != null) {
            long startTime = (Long) startTimeObj;
            duration = System.currentTimeMillis() - startTime;
        }

        String method = requestContext.getMethod();
        String uri = requestContext.getUriInfo().getRequestUri().toString();
        int status = responseContext.getStatus();
        String timestamp = dateFormat.format(new Date());

        // 技術的負債: 文字列連結によるログメッセージ構築
        String logMessage = "[" + timestamp + "] <<< " + method + " " + uri
                + " | Status: " + status
                + " | Duration: " + duration + "ms"
                + " | Response-Type: " + responseContext.getMediaType();

        // ステータスコードに応じてログレベルを変更
        if (status >= 500) {
            logger.severe(logMessage);
        } else if (status >= 400) {
            logger.warning(logMessage);
        } else {
            logger.info(logMessage);
        }
    }
}
