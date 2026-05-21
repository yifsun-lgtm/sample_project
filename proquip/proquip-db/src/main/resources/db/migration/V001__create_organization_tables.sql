-- ============================================================================
-- ProQuip - Enterprise Procurement & Inventory Management System
-- V001: 組織管理テーブルの作成
-- 部門、ユーザー、ロール、権限、委任ルールを管理するテーブル群
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 部門テーブル
-- 階層構造を持つ部門マスタ。parent_id による自己参照で組織ツリーを表現する。
-- ----------------------------------------------------------------------------
CREATE TABLE department (
    id              BIGSERIAL       PRIMARY KEY,
    parent_id       BIGINT          REFERENCES department(id) ON DELETE RESTRICT,
    department_code VARCHAR(20)     NOT NULL,
    name            VARCHAR(200)    NOT NULL,
    name_en         VARCHAR(200),
    description     TEXT,
    manager_id      BIGINT,         -- user_profile作成後に外部キーを追加
    level           INTEGER         NOT NULL DEFAULT 0,
    sort_order      INTEGER         NOT NULL DEFAULT 0,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    cost_center     VARCHAR(20),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT uq_department_code UNIQUE (department_code),
    CONSTRAINT ck_department_level CHECK (level >= 0),
    CONSTRAINT ck_department_sort_order CHECK (sort_order >= 0)
);

COMMENT ON TABLE department IS '部門マスタ - 階層構造を持つ組織の部門情報';
COMMENT ON COLUMN department.parent_id IS '親部門ID（NULLの場合はルート部門）';
COMMENT ON COLUMN department.department_code IS '部門コード（一意）';
COMMENT ON COLUMN department.level IS '階層レベル（0がルート）';
COMMENT ON COLUMN department.cost_center IS '原価センターコード（会計連携用）';

-- ----------------------------------------------------------------------------
-- ユーザープロファイルテーブル
-- Keycloakで認証されたユーザーの業務プロファイル情報を保持する。
-- keycloak_id でIdPと連携し、employee_number で社内システムと連携する。
-- ----------------------------------------------------------------------------
CREATE TABLE user_profile (
    id              BIGSERIAL       PRIMARY KEY,
    keycloak_id     VARCHAR(36)     NOT NULL,
    employee_number VARCHAR(20)     NOT NULL,
    username        VARCHAR(100)    NOT NULL,
    email           VARCHAR(255)    NOT NULL,
    first_name      VARCHAR(100)    NOT NULL,
    last_name       VARCHAR(100)    NOT NULL,
    first_name_kana VARCHAR(100),
    last_name_kana  VARCHAR(100),
    phone           VARCHAR(20),
    mobile_phone    VARCHAR(20),
    department_id   BIGINT          NOT NULL REFERENCES department(id) ON DELETE RESTRICT,
    job_title       VARCHAR(100),
    approval_limit  NUMERIC(15, 2) NOT NULL DEFAULT 0,
    locale          VARCHAR(10)     NOT NULL DEFAULT 'ja',
    timezone        VARCHAR(50)     NOT NULL DEFAULT 'Asia/Tokyo',
    avatar_url      VARCHAR(500),
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    last_login_at   TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT uq_user_keycloak_id UNIQUE (keycloak_id),
    CONSTRAINT uq_user_employee_number UNIQUE (employee_number),
    CONSTRAINT uq_user_email UNIQUE (email),
    CONSTRAINT ck_user_approval_limit CHECK (approval_limit >= 0)
);

COMMENT ON TABLE user_profile IS 'ユーザープロファイル - Keycloak連携の業務ユーザー情報';
COMMENT ON COLUMN user_profile.keycloak_id IS 'KeycloakのユーザーUUID（IdP連携キー）';
COMMENT ON COLUMN user_profile.employee_number IS '社員番号（社内システム連携キー）';
COMMENT ON COLUMN user_profile.approval_limit IS '承認可能金額上限（通貨単位）';
COMMENT ON COLUMN user_profile.first_name_kana IS '名（カナ）- 日本語環境用';
COMMENT ON COLUMN user_profile.last_name_kana IS '姓（カナ）- 日本語環境用';

-- 部門テーブルの manager_id に外部キー制約を追加
ALTER TABLE department
    ADD CONSTRAINT fk_department_manager
    FOREIGN KEY (manager_id) REFERENCES user_profile(id) ON DELETE SET NULL;

-- ----------------------------------------------------------------------------
-- ロールテーブル
-- システム上の役割を定義する。role_code で一意に識別される。
-- ----------------------------------------------------------------------------
CREATE TABLE role (
    id              BIGSERIAL       PRIMARY KEY,
    role_code       VARCHAR(50)     NOT NULL,
    name            VARCHAR(200)    NOT NULL,
    description     TEXT,
    is_system_role  BOOLEAN         NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT uq_role_code UNIQUE (role_code)
);

COMMENT ON TABLE role IS 'ロールマスタ - ユーザーに割り当て可能な役割定義';
COMMENT ON COLUMN role.is_system_role IS 'システム定義ロールフラグ（TRUEの場合は削除不可）';

-- ----------------------------------------------------------------------------
-- 権限テーブル
-- 個々の操作権限を定義する。resource + action の組み合わせで権限を表現する。
-- ----------------------------------------------------------------------------
CREATE TABLE permission (
    id              BIGSERIAL       PRIMARY KEY,
    permission_code VARCHAR(100)    NOT NULL,
    resource        VARCHAR(100)    NOT NULL,
    action          VARCHAR(50)     NOT NULL,
    description     TEXT,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT uq_permission_code UNIQUE (permission_code),
    CONSTRAINT uq_permission_resource_action UNIQUE (resource, action)
);

COMMENT ON TABLE permission IS '権限マスタ - リソースとアクションの組み合わせで定義される操作権限';
COMMENT ON COLUMN permission.resource IS '対象リソース名（例: purchase_order, inventory）';
COMMENT ON COLUMN permission.action IS '操作種別（例: CREATE, READ, UPDATE, DELETE, APPROVE）';

-- ----------------------------------------------------------------------------
-- ユーザー・ロールマッピングテーブル
-- ユーザーとロールの多対多関係を管理する中間テーブル。
-- 有効期間を設けることで期限付きロール割り当てに対応する。
-- ----------------------------------------------------------------------------
CREATE TABLE user_role_mapping (
    user_id         BIGINT          NOT NULL REFERENCES user_profile(id) ON DELETE CASCADE,
    role_id         BIGINT          NOT NULL REFERENCES role(id) ON DELETE CASCADE,
    assigned_by     BIGINT          REFERENCES user_profile(id) ON DELETE SET NULL,
    assigned_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valid_from      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valid_until     TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT pk_user_role_mapping PRIMARY KEY (user_id, role_id),
    CONSTRAINT ck_user_role_valid_range CHECK (valid_until IS NULL OR valid_until > valid_from)
);

COMMENT ON TABLE user_role_mapping IS 'ユーザー・ロール対応 - ユーザーへのロール割り当て（期限付き可）';
COMMENT ON COLUMN user_role_mapping.valid_until IS 'ロール有効期限（NULLの場合は無期限）';

-- ----------------------------------------------------------------------------
-- ロール・権限マッピングテーブル
-- ロールと権限の多対多関係を管理する中間テーブル。
-- ----------------------------------------------------------------------------
CREATE TABLE role_permission_mapping (
    role_id         BIGINT          NOT NULL REFERENCES role(id) ON DELETE CASCADE,
    permission_id   BIGINT          NOT NULL REFERENCES permission(id) ON DELETE CASCADE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT pk_role_permission_mapping PRIMARY KEY (role_id, permission_id)
);

COMMENT ON TABLE role_permission_mapping IS 'ロール・権限対応 - ロールに含まれる権限の定義';

-- ----------------------------------------------------------------------------
-- 委任ルールテーブル
-- ユーザーが不在時に別のユーザーへ権限を委任するためのルール。
-- 承認権限の委任フローに利用される。
-- ----------------------------------------------------------------------------
CREATE TABLE delegation_rule (
    id              BIGSERIAL       PRIMARY KEY,
    delegator_id    BIGINT          NOT NULL REFERENCES user_profile(id) ON DELETE CASCADE,
    delegate_id     BIGINT          NOT NULL REFERENCES user_profile(id) ON DELETE CASCADE,
    delegation_type VARCHAR(50)     NOT NULL,
    max_amount      NUMERIC(15, 2),
    valid_from      TIMESTAMP       NOT NULL,
    valid_until     TIMESTAMP       NOT NULL,
    reason          TEXT,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT ck_delegation_valid_range CHECK (valid_until > valid_from),
    CONSTRAINT ck_delegation_max_amount CHECK (max_amount IS NULL OR max_amount > 0),
    CONSTRAINT ck_delegation_not_self CHECK (delegator_id <> delegate_id),
    CONSTRAINT ck_delegation_type CHECK (delegation_type IN ('APPROVAL', 'PURCHASE', 'FULL'))
);

COMMENT ON TABLE delegation_rule IS '委任ルール - 不在時の権限委任設定';
COMMENT ON COLUMN delegation_rule.delegator_id IS '委任者（権限を委譲するユーザー）';
COMMENT ON COLUMN delegation_rule.delegate_id IS '受任者（権限を受け取るユーザー）';
COMMENT ON COLUMN delegation_rule.delegation_type IS '委任種別: APPROVAL=承認権限, PURCHASE=購買権限, FULL=全権限';
COMMENT ON COLUMN delegation_rule.max_amount IS '委任上限金額（NULLの場合は上限なし）';
