package com.proquip.web.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

/**
 * CORSヘッダーを付与するレスポンスフィルタ。
 *
 * <p>すべてのREST APIレスポンスにCross-Origin Resource Sharing（CORS）
 * ヘッダーを追加し、フロントエンドアプリケーションからのクロスオリジン
 * リクエストを許可する。</p>
 *
 * <p>【技術的負債】
 * Access-Control-Allow-Origin に "*"（全オリジン許可）を設定しており、
 * 本番環境ではセキュリティリスクとなる。
 * 許可するオリジンを環境変数またはDB設定から取得するように変更すべき。
 * また、Access-Control-Allow-Credentials との併用時に "*" は使えないため、
 * 認証付きリクエストでCORS問題が発生する可能性がある。</p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
@Provider
public class CorsFilter implements ContainerResponseFilter {

    /**
     * レスポンスにCORSヘッダーを追加する。
     *
     * @param requestContext  リクエストコンテキスト
     * @param responseContext レスポンスコンテキスト
     * @throws IOException I/Oエラー時
     */
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {

        // 技術的負債: 全オリジンを許可（セキュリティリスク）
        responseContext.getHeaders().add("Access-Control-Allow-Origin", "*");

        responseContext.getHeaders().add("Access-Control-Allow-Headers",
                "origin, content-type, accept, authorization, X-Requested-With");

        responseContext.getHeaders().add("Access-Control-Allow-Methods",
                "GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH");

        responseContext.getHeaders().add("Access-Control-Allow-Credentials", "true");

        responseContext.getHeaders().add("Access-Control-Max-Age", "86400");

        // プリフライトリクエスト（OPTIONS）への即時応答
        if ("OPTIONS".equalsIgnoreCase(requestContext.getMethod())) {
            responseContext.setStatus(200);
        }
    }
}
