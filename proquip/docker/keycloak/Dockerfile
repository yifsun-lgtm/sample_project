# ============================================================
# ProQuip Keycloak 認証サーバー
# 開発環境用：レルム設定を自動インポートして起動
#
# TODO: 本番環境では開発モードを使わないこと
# ============================================================
FROM quay.io/keycloak/keycloak:22.0.5

# レルム設定ファイルをインポートディレクトリにコピー
COPY proquip-realm.json /opt/keycloak/data/import/proquip-realm.json

# 開発モードで起動し、レルム設定をインポート
# FIXME: --optimized フラグを使ったプロダクションビルドに切り替えるべき
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start-dev", "--import-realm"]
