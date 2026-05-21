-- ============================================================================
-- ProQuip - Enterprise Procurement & Inventory Management System
-- V006: 価格・予算管理テーブルの作成
-- 通貨、価格表、税率、予算
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 通貨テーブル
-- システムで使用する通貨の定義と為替レート情報を管理する。
-- ----------------------------------------------------------------------------
CREATE TABLE currency (
    id              BIGSERIAL       PRIMARY KEY,
    currency_code   VARCHAR(3)      NOT NULL,
    name            VARCHAR(100)    NOT NULL,
    name_en         VARCHAR(100),
    symbol          VARCHAR(5)      NOT NULL,
    decimal_places  INTEGER         NOT NULL DEFAULT 2,
    exchange_rate   NUMERIC(15, 6) NOT NULL DEFAULT 1.0,
    rate_updated_at TIMESTAMP,
    is_base         BOOLEAN         NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT uq_currency_code UNIQUE (currency_code),
    CONSTRAINT ck_currency_decimal CHECK (decimal_places >= 0 AND decimal_places <= 4),
    CONSTRAINT ck_currency_exchange_rate CHECK (exchange_rate > 0)
);

COMMENT ON TABLE currency IS '通貨マスタ - 使用通貨と為替レート';
COMMENT ON COLUMN currency.exchange_rate IS '基準通貨に対する為替レート';
COMMENT ON COLUMN currency.is_base IS '基準通貨フラグ（システム全体で1通貨のみTRUE）';
COMMENT ON COLUMN currency.decimal_places IS '小数点以下桁数（JPYは0、USDは2等）';

-- ----------------------------------------------------------------------------
-- 価格表テーブル
-- 商品の価格体系を管理する。有効期間を設けることで価格改定に対応する。
-- 複数の価格表を使い分けることで、顧客別・地域別の価格設定に対応する。
-- ----------------------------------------------------------------------------
CREATE TABLE price_list (
    id              BIGSERIAL       PRIMARY KEY,
    price_list_code VARCHAR(30)     NOT NULL,
    name            VARCHAR(200)    NOT NULL,
    description     TEXT,
    currency_code   VARCHAR(3)      NOT NULL DEFAULT 'JPY',
    price_list_type VARCHAR(20)     NOT NULL DEFAULT 'STANDARD',
    status          VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    effective_from  DATE            NOT NULL,
    effective_until DATE,
    is_default      BOOLEAN         NOT NULL DEFAULT FALSE,
    created_by      BIGINT          REFERENCES user_profile(id) ON DELETE SET NULL,
    approved_by     BIGINT          REFERENCES user_profile(id) ON DELETE SET NULL,
    approved_at     TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT uq_price_list_code UNIQUE (price_list_code),
    CONSTRAINT ck_price_list_type CHECK (price_list_type IN (
        'STANDARD', 'CONTRACT', 'PROMOTIONAL', 'INTERNAL', 'COST'
    )),
    CONSTRAINT ck_price_list_status CHECK (status IN (
        'DRAFT', 'PENDING_APPROVAL', 'ACTIVE', 'EXPIRED', 'ARCHIVED'
    )),
    CONSTRAINT ck_price_list_dates CHECK (
        effective_until IS NULL OR effective_until >= effective_from
    )
);

COMMENT ON TABLE price_list IS '価格表 - 商品価格体系の定義';
COMMENT ON COLUMN price_list.price_list_type IS '種別: STANDARD=標準, CONTRACT=契約, PROMOTIONAL=キャンペーン, INTERNAL=社内振替, COST=原価';
COMMENT ON COLUMN price_list.effective_from IS '有効開始日';
COMMENT ON COLUMN price_list.effective_until IS '有効終了日（NULLの場合は無期限）';

-- ----------------------------------------------------------------------------
-- 価格表明細テーブル
-- 価格表内の個々の商品の価格情報を定義する。
-- ボリュームディスカウント（数量別価格帯）に対応する。
-- ----------------------------------------------------------------------------
CREATE TABLE price_list_item (
    id              BIGSERIAL       PRIMARY KEY,
    price_list_id   BIGINT          NOT NULL REFERENCES price_list(id) ON DELETE CASCADE,
    product_id      BIGINT          NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    unit_price      NUMERIC(15, 2) NOT NULL,
    min_quantity    INTEGER         NOT NULL DEFAULT 1,
    max_quantity    INTEGER,
    discount_pct    NUMERIC(5, 2)   NOT NULL DEFAULT 0,
    markup_pct      NUMERIC(5, 2)   NOT NULL DEFAULT 0,
    notes           TEXT,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT ck_pli_unit_price CHECK (unit_price >= 0),
    CONSTRAINT ck_pli_min_qty CHECK (min_quantity >= 1),
    CONSTRAINT ck_pli_max_qty CHECK (max_quantity IS NULL OR max_quantity >= min_quantity),
    CONSTRAINT ck_pli_discount CHECK (discount_pct >= 0 AND discount_pct <= 100),
    CONSTRAINT ck_pli_markup CHECK (markup_pct >= 0)
);

COMMENT ON TABLE price_list_item IS '価格表明細 - 商品ごとの価格・数量別ティア';
COMMENT ON COLUMN price_list_item.min_quantity IS '適用最小数量（ボリュームディスカウントの下限）';
COMMENT ON COLUMN price_list_item.max_quantity IS '適用最大数量（NULLの場合は上限なし）';
COMMENT ON COLUMN price_list_item.discount_pct IS '割引率（%）';
COMMENT ON COLUMN price_list_item.markup_pct IS 'マークアップ率（%）- 原価に対する上乗せ';

-- ----------------------------------------------------------------------------
-- 税率テーブル
-- 国・地域別の税率情報を管理する。有効期間を設けて税制改正に対応する。
-- ----------------------------------------------------------------------------
CREATE TABLE tax_rate (
    id              BIGSERIAL       PRIMARY KEY,
    tax_code        VARCHAR(20)     NOT NULL,
    name            VARCHAR(200)    NOT NULL,
    description     TEXT,
    tax_type        VARCHAR(20)     NOT NULL DEFAULT 'VAT',
    country_code    VARCHAR(3)      NOT NULL DEFAULT 'JPN',
    state_province  VARCHAR(100),
    rate_pct        NUMERIC(5, 2)   NOT NULL,
    effective_from  DATE            NOT NULL,
    effective_until DATE,
    is_compound     BOOLEAN         NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT uq_tax_code UNIQUE (tax_code),
    CONSTRAINT ck_tax_type CHECK (tax_type IN ('VAT', 'GST', 'SALES_TAX', 'CUSTOMS', 'EXCISE', 'WITHHOLDING')),
    CONSTRAINT ck_tax_rate CHECK (rate_pct >= 0 AND rate_pct <= 100),
    CONSTRAINT ck_tax_dates CHECK (effective_until IS NULL OR effective_until >= effective_from)
);

COMMENT ON TABLE tax_rate IS '税率マスタ - 国・地域別の税率定義';
COMMENT ON COLUMN tax_rate.tax_type IS '税種別: VAT=付加価値税, GST=物品サービス税, SALES_TAX=売上税等';
COMMENT ON COLUMN tax_rate.is_compound IS '複合課税フラグ（他の税額に対して課税する場合TRUE）';
COMMENT ON COLUMN tax_rate.country_code IS '適用国コード（ISO 3166-1 alpha-3）';

-- ----------------------------------------------------------------------------
-- 予算テーブル
-- 部門×年度ごとの購買予算を管理する。
-- 予算超過の防止と予実管理に使用する。
-- ----------------------------------------------------------------------------
CREATE TABLE budget (
    id              BIGSERIAL       PRIMARY KEY,
    budget_code     VARCHAR(30)     NOT NULL,
    department_id   BIGINT          NOT NULL REFERENCES department(id) ON DELETE RESTRICT,
    fiscal_year     INTEGER         NOT NULL,
    fiscal_period   VARCHAR(10)     NOT NULL DEFAULT 'ANNUAL',
    name            VARCHAR(200)    NOT NULL,
    description     TEXT,
    total_amount    NUMERIC(15, 2) NOT NULL,
    allocated_amount NUMERIC(15, 2) NOT NULL DEFAULT 0,
    spent_amount    NUMERIC(15, 2) NOT NULL DEFAULT 0,
    committed_amount NUMERIC(15, 2) NOT NULL DEFAULT 0,
    remaining_amount NUMERIC(15, 2) NOT NULL,
    currency_code   VARCHAR(3)      NOT NULL DEFAULT 'JPY',
    status          VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    approved_by     BIGINT          REFERENCES user_profile(id) ON DELETE SET NULL,
    approved_at     TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT uq_budget_code UNIQUE (budget_code),
    CONSTRAINT uq_budget_dept_year UNIQUE (department_id, fiscal_year, fiscal_period),
    CONSTRAINT ck_budget_total CHECK (total_amount >= 0),
    CONSTRAINT ck_budget_allocated CHECK (allocated_amount >= 0),
    CONSTRAINT ck_budget_spent CHECK (spent_amount >= 0),
    CONSTRAINT ck_budget_committed CHECK (committed_amount >= 0),
    CONSTRAINT ck_budget_remaining CHECK (remaining_amount >= 0),
    CONSTRAINT ck_budget_fiscal_year CHECK (fiscal_year >= 2000 AND fiscal_year <= 2100),
    CONSTRAINT ck_budget_period CHECK (fiscal_period IN (
        'ANNUAL', 'H1', 'H2', 'Q1', 'Q2', 'Q3', 'Q4'
    )),
    CONSTRAINT ck_budget_status CHECK (status IN (
        'DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'ACTIVE', 'FROZEN', 'CLOSED'
    ))
);

COMMENT ON TABLE budget IS '予算 - 部門×年度の購買予算管理';
COMMENT ON COLUMN budget.allocated_amount IS '配分済金額（予算枠から配分した金額の合計）';
COMMENT ON COLUMN budget.spent_amount IS '使用済金額（実際に支払い完了した金額）';
COMMENT ON COLUMN budget.committed_amount IS 'コミット済金額（発注済だが未払いの金額）';
COMMENT ON COLUMN budget.remaining_amount IS '残余予算額（total - spent - committed）';

-- 購買依頼テーブルに予算への外部キーを追加
ALTER TABLE purchase_requisition
    ADD CONSTRAINT fk_requisition_budget
    FOREIGN KEY (budget_id) REFERENCES budget(id) ON DELETE SET NULL;

-- ----------------------------------------------------------------------------
-- 予算明細テーブル
-- 予算をカテゴリ別に細分化した配分を管理する。
-- ----------------------------------------------------------------------------
CREATE TABLE budget_line_item (
    id              BIGSERIAL       PRIMARY KEY,
    budget_id       BIGINT          NOT NULL REFERENCES budget(id) ON DELETE CASCADE,
    category_id     BIGINT          REFERENCES category(id) ON DELETE SET NULL,
    line_name       VARCHAR(200)    NOT NULL,
    description     TEXT,
    allocated_amount NUMERIC(15, 2) NOT NULL,
    spent_amount    NUMERIC(15, 2) NOT NULL DEFAULT 0,
    committed_amount NUMERIC(15, 2) NOT NULL DEFAULT 0,
    notes           TEXT,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT ck_bli_allocated CHECK (allocated_amount >= 0),
    CONSTRAINT ck_bli_spent CHECK (spent_amount >= 0),
    CONSTRAINT ck_bli_committed CHECK (committed_amount >= 0)
);

COMMENT ON TABLE budget_line_item IS '予算明細 - カテゴリ別の予算配分';
COMMENT ON COLUMN budget_line_item.category_id IS '対象商品カテゴリ（NULLの場合は未分類分）';
