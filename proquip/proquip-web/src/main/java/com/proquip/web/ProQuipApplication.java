package com.proquip.web;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * ProQuip REST APIのJAX-RSアプリケーション定義クラス。
 *
 * <p>全てのREST APIリソースを {@code /api} パス配下に登録する。
 * リソースクラスおよびプロバイダーはCDIスキャンにより自動検出される。</p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
@ApplicationPath("/api")
public class ProQuipApplication extends Application {

    /**
     * デフォルトコンストラクタ。
     *
     * <p>リソースクラスの登録はアノテーションスキャンに委譲するため、
     * {@link #getClasses()} や {@link #getSingletons()} のオーバーライドは不要。</p>
     */
    public ProQuipApplication() {
        // CDIによる自動検出を使用するため、明示的な登録は行わない
    }
}
