# ============================================================
# ProQuip Nginx リバースプロキシ & フロントエンド配信
# Angular SPAの静的ファイル配信とAPIプロキシ
#
# FIXME: マルチステージビルドでAngularのビルドも含めるべき
# ============================================================
FROM nginx:1.25.4-alpine3.18

# ラベル設定
LABEL maintainer="proquip-dev@example.com"
LABEL description="ProQuip フロントエンド配信サーバー (Nginx)"

# デフォルトの設定ファイルを削除
RUN rm -f /etc/nginx/conf.d/default.conf

# カスタムNginx設定をコピー
COPY nginx.conf /etc/nginx/conf.d/proquip.conf

# ビルド済みAngularアプリの配置先ディレクトリを作成
# TODO: CI/CDパイプラインでビルド成果物を配置する想定
RUN mkdir -p /usr/share/nginx/html/proquip

# ヘルスチェック用エンドポイント
RUN echo '{"status":"ok"}' > /usr/share/nginx/html/proquip/health.json

# ポート公開
EXPOSE 80

# Nginxをフォアグラウンドで実行
CMD ["nginx", "-g", "daemon off;"]
