package com.proquip.web.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * ヘルスチェック用のRESTリソース。
 *
 * <p>ロードバランサーやモニタリングツールからの死活監視に使用する。
 * 認証不要でアクセス可能。</p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
@Path("/health")
@Produces(MediaType.APPLICATION_JSON)
public class HealthResource {

    /**
     * ヘルスチェックを実行し、システムステータスを返す。
     *
     * @return ステータス "UP" とタイムスタンプを含むJSON応答
     */
    @GET
    public Response healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", new Date().toString());
        return Response.ok(health).build();
    }
}
