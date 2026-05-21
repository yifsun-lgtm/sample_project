-- ============================================================
-- ProQuip 初期データベース作成スクリプト
-- Keycloak用データベースの作成と権限付与
-- ============================================================

-- Keycloak認証サーバー用データベースを作成
-- PostgreSQLの初期化時に自動実行される
CREATE DATABASE keycloak
    WITH OWNER = proquip
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE = 'en_US.utf8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;

-- proquipユーザーにkeycloakデータベースの全権限を付与
GRANT ALL PRIVILEGES ON DATABASE keycloak TO proquip;

-- アプリケーションデータベースへの追加設定
-- TODO: 本番環境では読み取り専用ユーザーも作成すべき
\connect proquip;

-- 拡張機能の有効化
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- スキーマ作成（アプリケーション用）
CREATE SCHEMA IF NOT EXISTS procurement;
CREATE SCHEMA IF NOT EXISTS inventory;
CREATE SCHEMA IF NOT EXISTS master;

GRANT ALL ON SCHEMA procurement TO proquip;
GRANT ALL ON SCHEMA inventory TO proquip;
GRANT ALL ON SCHEMA master TO proquip;

-- keycloakデータベースにも接続して初期設定
\connect keycloak;

-- Keycloak用の拡張機能
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
