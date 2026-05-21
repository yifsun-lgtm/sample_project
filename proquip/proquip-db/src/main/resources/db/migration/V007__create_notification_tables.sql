-- ============================================================================
-- ProQuip - Enterprise Procurement & Inventory Management System
-- V007: 通知管理テーブルの作成
-- 通知、通知テンプレート
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 通知テンプレートテーブル
-- 通知メッセージのテンプレートを管理する。
-- テンプレートはプレースホルダー（{{variable}}形式）を含むことができる。
-- チャネル別にテンプレートを定義することで、メール・アプリ内通知の内容を
-- それぞれ最適化できる。
-- ----------------------------------------------------------------------------
CREATE TABLE notification_template (
    id              BIGSERIAL       PRIMARY KEY,
    template_code   VARCHAR(50)     NOT NULL,
    name            VARCHAR(200)    NOT NULL,
    description     TEXT,
    channel         VARCHAR(20)     NOT NULL DEFAULT 'BOTH',
    event_type      VARCHAR(50)     NOT NULL,
    subject         VARCHAR(500),
    body_text       TEXT            NOT NULL,
    body_html       TEXT,
    locale          VARCHAR(10)     NOT NULL DEFAULT 'ja',
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT uq_notification_template UNIQUE (template_code, locale),
    CONSTRAINT ck_template_channel CHECK (channel IN ('EMAIL', 'IN_APP', 'BOTH', 'SMS', 'WEBHOOK')),
    CONSTRAINT ck_template_event_type CHECK (event_type IN (
        'PO_CREATED', 'PO_APPROVED', 'PO_REJECTED', 'PO_RECEIVED',
        'PR_SUBMITTED', 'PR_APPROVED', 'PR_REJECTED',
        'APPROVAL_REQUIRED', 'APPROVAL_REMINDER', 'APPROVAL_ESCALATED',
        'LOW_STOCK_ALERT', 'REORDER_SUGGESTION',
        'CONTRACT_EXPIRING', 'CERTIFICATION_EXPIRING',
        'BUDGET_THRESHOLD', 'BUDGET_EXCEEDED',
        'GOODS_RECEIVED', 'GOODS_REJECTED',
        'SYSTEM_ANNOUNCEMENT', 'IMPORT_COMPLETED', 'IMPORT_FAILED'
    ))
);

COMMENT ON TABLE notification_template IS '通知テンプレート - 通知メッセージの雛形定義';
COMMENT ON COLUMN notification_template.channel IS '通知チャネル: EMAIL=メール, IN_APP=アプリ内, BOTH=両方, SMS=SMS, WEBHOOK=Webhook';
COMMENT ON COLUMN notification_template.event_type IS 'トリガーとなるイベント種別';
COMMENT ON COLUMN notification_template.body_text IS 'テキスト本文（プレースホルダー: {{variable}} 形式）';
COMMENT ON COLUMN notification_template.body_html IS 'HTML本文（メール用、NULLの場合はテキスト本文を使用）';

-- ----------------------------------------------------------------------------
-- 通知テーブル
-- ユーザーへの通知メッセージを管理する。
-- テンプレートから生成された通知の送信状態と既読状態を追跡する。
-- ----------------------------------------------------------------------------
CREATE TABLE notification (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL REFERENCES user_profile(id) ON DELETE CASCADE,
    template_id     BIGINT          REFERENCES notification_template(id) ON DELETE SET NULL,
    channel         VARCHAR(20)     NOT NULL DEFAULT 'IN_APP',
    title           VARCHAR(500)    NOT NULL,
    message         TEXT            NOT NULL,
    message_html    TEXT,
    priority        VARCHAR(10)     NOT NULL DEFAULT 'NORMAL',
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    entity_type     VARCHAR(50),
    entity_id       BIGINT,
    action_url      VARCHAR(500),
    sent_at         TIMESTAMP,
    read_at         TIMESTAMP,
    dismissed_at    TIMESTAMP,
    retry_count     INTEGER         NOT NULL DEFAULT 0,
    max_retries     INTEGER         NOT NULL DEFAULT 3,
    error_message   TEXT,
    metadata        JSONB,
    expires_at      TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT ck_notification_channel CHECK (channel IN ('EMAIL', 'IN_APP', 'BOTH', 'SMS', 'WEBHOOK')),
    CONSTRAINT ck_notification_priority CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    CONSTRAINT ck_notification_status CHECK (status IN (
        'PENDING', 'SENT', 'DELIVERED', 'READ', 'FAILED', 'CANCELLED', 'EXPIRED'
    )),
    CONSTRAINT ck_notification_retry CHECK (retry_count >= 0 AND retry_count <= max_retries)
);

COMMENT ON TABLE notification IS '通知 - ユーザーへの通知メッセージ';
COMMENT ON COLUMN notification.entity_type IS '関連エンティティ種別（通知元のオブジェクト）';
COMMENT ON COLUMN notification.entity_id IS '関連エンティティID';
COMMENT ON COLUMN notification.action_url IS '通知クリック時の遷移先URL';
COMMENT ON COLUMN notification.metadata IS '追加データ（JSONB形式、テンプレート変数の展開等に使用）';
COMMENT ON COLUMN notification.expires_at IS '通知の有効期限（期限切れの通知は自動的にEXPIRED状態に遷移）';
