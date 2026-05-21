# ============================================================
# ProQuip Nginx リバースプロキシ & フロントエンド配信
# Stage 1: Angular SPA をビルド
# Stage 2: Nginx で配信
# ============================================================

# ----- Stage 1: Angular Build -----
FROM node:18.20-alpine AS frontend-builder

WORKDIR /app

COPY proquip-frontend/package.json proquip-frontend/package-lock.json ./
RUN npm ci

COPY proquip-frontend/ ./
RUN npm run build

# ----- Stage 2: Nginx -----
FROM nginx:1.25.4-alpine3.18

LABEL maintainer="proquip-dev@example.com"
LABEL description="ProQuip フロントエンド配信サーバー (Nginx)"

RUN rm -f /etc/nginx/conf.d/default.conf

COPY docker/nginx/nginx.conf /etc/nginx/conf.d/proquip.conf

COPY --from=frontend-builder /app/dist/proquip/browser /usr/share/nginx/html/proquip

RUN echo '{"status":"ok"}' > /usr/share/nginx/html/proquip/health.json

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
