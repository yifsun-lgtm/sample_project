# ============================================================
# ProQuip Keycloak 認証サーバー
# Stage 1: ビルドステージ (--optimized 用)
# Stage 2: 本番用ランタイム
# ============================================================

# ----- Stage 1: Build -----
FROM quay.io/keycloak/keycloak:22.0.5 AS builder

ENV KC_DB=postgres
ENV KC_HEALTH_ENABLED=true

RUN /opt/keycloak/bin/kc.sh build

# ----- Stage 2: Runtime -----
FROM quay.io/keycloak/keycloak:22.0.5

COPY --from=builder /opt/keycloak/ /opt/keycloak/

COPY proquip-realm.json /opt/keycloak/data/import/proquip-realm.json

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", "--optimized", "--hostname-strict=false", "--http-enabled=true", "--proxy=edge"]
