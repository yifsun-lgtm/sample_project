# ============================================================
# ProQuip WildFly アプリケーションサーバー
# Jakarta EE アプリケーションのデプロイ用
# ============================================================
FROM quay.io/wildfly/wildfly:30.0.1.Final-jdk17

# ラベル設定
LABEL maintainer="proquip-dev@example.com"
LABEL description="ProQuip アプリケーションサーバー (WildFly 30)"

# rootユーザーで設定ファイルをコピー
USER root

# PostgreSQL JDBCドライバーモジュールのインストール
# TODO: ドライバーのバージョンを外部から指定できるようにすべき
ARG POSTGRESQL_DRIVER_VERSION=42.7.1
ADD --chown=jboss:root \
    https://jdbc.postgresql.org/download/postgresql-${POSTGRESQL_DRIVER_VERSION}.jar \
    /opt/jboss/wildfly/modules/org/postgresql/main/postgresql-jdbc.jar

# モジュール定義ファイルをコピー
COPY --chown=jboss:root modules/org/postgresql/main/module.xml \
    /opt/jboss/wildfly/modules/org/postgresql/main/module.xml

# JBoss CLIスクリプトをコピーして実行
COPY --chown=jboss:root configure-wildfly.cli /tmp/configure-wildfly.cli

# jbossユーザーに戻す
USER jboss

# CLIスクリプトで standalone-full.xml に追加設定を適用
RUN /opt/jboss/wildfly/bin/jboss-cli.sh \
    --file=/tmp/configure-wildfly.cli && \
    rm -f /tmp/configure-wildfly.cli

# 管理ユーザーの作成（開発用）
# TODO: 本番環境では削除すること
RUN /opt/jboss/wildfly/bin/add-user.sh admin admin123! --silent

# EARファイルのデプロイ用ディレクトリ確認
# ビルド成果物は CI/CD パイプラインで配置する想定
# FIXME: マルチステージビルドにしてMavenビルドも含めるべき
RUN mkdir -p /opt/jboss/wildfly/standalone/deployments/

# ポート公開
# 8080: HTTPアプリケーション
# 9990: 管理コンソール
EXPOSE 8080 9990

# standalone-full.xml で起動（CLIで設定済み）
CMD ["/opt/jboss/wildfly/bin/standalone.sh", \
     "-c", "standalone-full.xml", \
     "-b", "0.0.0.0", \
     "-bmanagement", "0.0.0.0"]
