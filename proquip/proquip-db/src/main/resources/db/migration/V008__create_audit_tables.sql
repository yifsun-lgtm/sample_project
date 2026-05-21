-- ============================================================================
-- ProQuip - Enterprise Procurement & Inventory Management System
-- V008: 監査・システム管理テーブルの作成
-- 監査ログ、システム設定、インポートジョブ
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 監査ログテーブル
-- すべてのエンティティに対する変更操作を記録する。
-- entity_type + entity_id のポリモーフィック参照で対象を特定する。
-- 変更前後の値をJSONBカラムに保持することで、変更内容の追跡と復元を可能にする。
-- ----------------------------------------------------------------------------
CREATE TABLE audit_log (
    id              BIGSERIAL       PRIMARY KEY,
    entity_type     VARCHAR(50)     NOT NULL,
    entity_id       BIGINT          NOT NULL,
    action          VARCHAR(20)     NOT NULL,
    performed_by    BIGINT          REFERENCES user_profile(id) ON DELETE SET NULL,
    performed_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(500),
    old_values      JSONB,
    new_values      JSONB,
    changed_fields  TEXT[],
    request_id      VARCHAR(36),
    session_id      VARCHAR(100),
    description     TEXT,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT ck_audit_action CHECK (action IN (
        'CREATE', 'UPDATE', 'DELETE', 'READ', 'LOGIN', 'LOGOUT',
        'APPROVE', 'REJECT', 'EXPORT', 'IMPORT', 'PRINT',
        'STATUS_CHANGE', 'BULK_UPDATE', 'ARCHIVE', 'RESTORE'
    ))
);

COMMENT ON TABLE audit_log IS '監査ログ - 全エンティティの変更操作記録';
COMMENT ON COLUMN audit_log.entity_type IS '対象エンティティ種別（テーブル名に対応）';
COMMENT ON COLUMN audit_log.entity_id IS '対象エンティティID';
COMMENT ON COLUMN audit_log.old_values IS '変更前の値（JSONB形式）';
COMMENT ON COLUMN audit_log.new_values IS '変更後の値（JSONB形式）';
COMMENT ON COLUMN audit_log.changed_fields IS '変更されたフィールド名のリスト';
COMMENT ON COLUMN audit_log.request_id IS 'HTTPリクエストの一意識別子（リクエスト追跡用）';
COMMENT ON COLUMN audit_log.ip_address IS '操作元IPアドレス（IPv6対応のため45文字）';

-- ----------------------------------------------------------------------------
-- システム設定テーブル
-- アプリケーションの設定値をKey-Valueペアで管理する。
-- config_group でグループ化し、設定画面での表示を整理する。
-- ----------------------------------------------------------------------------
CREATE TABLE system_configuration (
    id              BIGSERIAL       PRIMARY KEY,
    config_key      VARCHAR(200)    NOT NULL,
    config_value    TEXT            NOT NULL,
    config_group    VARCHAR(50)     NOT NULL DEFAULT 'GENERAL',
    value_type      VARCHAR(20)     NOT NULL DEFAULT 'STRING',
    display_name    VARCHAR(200)    NOT NULL,
    description     TEXT,
    default_value   TEXT,
    validation_rule VARCHAR(500),
    is_encrypted    BOOLEAN         NOT NULL DEFAULT FALSE,
    is_editable     BOOLEAN         NOT NULL DEFAULT TRUE,
    is_visible      BOOLEAN         NOT NULL DEFAULT TRUE,
    sort_order      INTEGER         NOT NULL DEFAULT 0,
    updated_by      BIGINT          REFERENCES user_profile(id) ON DELETE SET NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT uq_config_key UNIQUE (config_key),
    CONSTRAINT ck_config_group CHECK (config_group IN (
        'GENERAL', 'PROCUREMENT', 'INVENTORY', 'NOTIFICATION', 'SECURITY',
        'INTEGRATION', 'REPORTING', 'UI', 'APPROVAL', 'PRICING'
    )),
    CONSTRAINT ck_config_value_type CHECK (value_type IN (
        'STRING', 'INTEGER', 'DECIMAL', 'BOOLEAN', 'JSON', 'DATE', 'URL', 'EMAIL'
    ))
);

COMMENT ON TABLE system_configuration IS 'システム設定 - アプリケーション設定値のKey-Valueストア';
COMMENT ON COLUMN system_configuration.config_key IS '設定キー（ドット区切り、例: procurement.auto_approve_threshold）';
COMMENT ON COLUMN system_configuration.config_group IS '設定グループ（画面表示でのカテゴリ分け）';
COMMENT ON COLUMN system_configuration.is_encrypted IS '暗号化フラグ（パスワード等の機密情報の場合TRUE）';
COMMENT ON COLUMN system_configuration.validation_rule IS 'バリデーションルール（正規表現または範囲指定）';

-- ----------------------------------------------------------------------------
-- インポートジョブテーブル
-- CSV/Excelファイルによる一括インポート処理の進捗と結果を管理する。
-- 非同期バッチ処理の追跡に使用する。
-- ----------------------------------------------------------------------------
CREATE TABLE import_job (
    id              BIGSERIAL       PRIMARY KEY,
    job_code        VARCHAR(36)     NOT NULL,
    import_type     VARCHAR(50)     NOT NULL,
    file_name       VARCHAR(500)    NOT NULL,
    file_url        VARCHAR(500),
    file_size_bytes BIGINT,
    mime_type       VARCHAR(100),
    status          VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    total_rows      INTEGER,
    processed_rows  INTEGER         NOT NULL DEFAULT 0,
    success_rows    INTEGER         NOT NULL DEFAULT 0,
    error_rows      INTEGER         NOT NULL DEFAULT 0,
    skipped_rows    INTEGER         NOT NULL DEFAULT 0,
    progress_pct    NUMERIC(5, 2)   NOT NULL DEFAULT 0,
    error_log       JSONB,
    error_file_url  VARCHAR(500),
    started_by      BIGINT          NOT NULL REFERENCES user_profile(id) ON DELETE RESTRICT,
    started_at      TIMESTAMP,
    completed_at    TIMESTAMP,
    cancelled_at    TIMESTAMP,
    cancelled_by    BIGINT          REFERENCES user_profile(id) ON DELETE SET NULL,
    options         JSONB,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT uq_import_job_code UNIQUE (job_code),
    CONSTRAINT ck_import_type CHECK (import_type IN (
        'PRODUCT', 'SUPPLIER', 'INVENTORY', 'PRICE_LIST', 'CATEGORY',
        'USER_PROFILE', 'BUDGET', 'PURCHASE_ORDER'
    )),
    CONSTRAINT ck_import_status CHECK (status IN (
        'PENDING', 'VALIDATING', 'IN_PROGRESS', 'COMPLETED', 'COMPLETED_WITH_ERRORS',
        'FAILED', 'CANCELLED', 'ROLLING_BACK'
    )),
    CONSTRAINT ck_import_progress CHECK (progress_pct >= 0 AND progress_pct <= 100),
    CONSTRAINT ck_import_rows CHECK (
        processed_rows >= 0 AND success_rows >= 0 AND error_rows >= 0 AND skipped_rows >= 0
    ),
    CONSTRAINT ck_import_file_size CHECK (file_size_bytes IS NULL OR file_size_bytes > 0)
);

COMMENT ON TABLE import_job IS 'インポートジョブ - CSV/Excelインポートの進捗・結果管理';
COMMENT ON COLUMN import_job.job_code IS 'ジョブ識別子（UUID形式）';
COMMENT ON COLUMN import_job.import_type IS 'インポート対象のエンティティ種別';
COMMENT ON COLUMN import_job.error_log IS 'エラー詳細ログ（JSONB形式、行番号とエラー内容）';
COMMENT ON COLUMN import_job.error_file_url IS 'エラー行のみを含むファイルのURL（再インポート用）';
COMMENT ON COLUMN import_job.options IS 'インポートオプション（JSONB形式、区切り文字、重複時の動作等）';
