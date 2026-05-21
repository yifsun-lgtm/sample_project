-- ============================================================================
-- ProQuip - Enterprise Procurement & Inventory Management System
-- V003: 仕入先管理テーブルの作成
-- 仕入先、連絡先、住所、取扱商品、契約、評価、認証情報
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 仕入先テーブル
-- 商品を購入する取引先企業の基本情報を管理する。
-- ----------------------------------------------------------------------------
CREATE TABLE supplier (
    id              BIGSERIAL       PRIMARY KEY,
    supplier_code   VARCHAR(20)     NOT NULL,
    name            VARCHAR(300)    NOT NULL,
    name_en         VARCHAR(300),
    legal_name      VARCHAR(300),
    tax_id          VARCHAR(50),
    website         VARCHAR(500),
    payment_terms   VARCHAR(50)     NOT NULL DEFAULT 'NET30',
    payment_method  VARCHAR(30)     NOT NULL DEFAULT 'BANK_TRANSFER',
    credit_limit    NUMERIC(15, 2),
    currency_code   VARCHAR(3)      NOT NULL DEFAULT 'JPY',
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    risk_level      VARCHAR(10)     NOT NULL DEFAULT 'LOW',
    notes           TEXT,
    registered_at   DATE            NOT NULL DEFAULT CURRENT_DATE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT uq_supplier_code UNIQUE (supplier_code),
    CONSTRAINT ck_supplier_payment_terms CHECK (payment_terms IN (
        'PREPAID', 'COD', 'NET15', 'NET30', 'NET45', 'NET60', 'NET90', 'EOM'
    )),
    CONSTRAINT ck_supplier_payment_method CHECK (payment_method IN (
        'BANK_TRANSFER', 'CHECK', 'CREDIT_CARD', 'CASH', 'LETTER_OF_CREDIT'
    )),
    CONSTRAINT ck_supplier_credit_limit CHECK (credit_limit IS NULL OR credit_limit >= 0),
    CONSTRAINT ck_supplier_status CHECK (status IN (
        'ACTIVE', 'INACTIVE', 'SUSPENDED', 'PENDING_APPROVAL', 'BLACKLISTED'
    )),
    CONSTRAINT ck_supplier_risk_level CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

COMMENT ON TABLE supplier IS '仕入先マスタ - 取引先企業の基本情報';
COMMENT ON COLUMN supplier.payment_terms IS '支払条件: NET30=納品後30日払い等';
COMMENT ON COLUMN supplier.risk_level IS 'リスクレベル: 取引先の信用リスク評価';
COMMENT ON COLUMN supplier.credit_limit IS '与信限度額';

-- ----------------------------------------------------------------------------
-- 仕入先連絡先テーブル
-- 仕入先企業の担当者・連絡先情報を管理する。
-- 1つの仕入先に対して複数の連絡先を持つことができる。
-- ----------------------------------------------------------------------------
CREATE TABLE supplier_contact (
    id              BIGSERIAL       PRIMARY KEY,
    supplier_id     BIGINT          NOT NULL REFERENCES supplier(id) ON DELETE CASCADE,
    contact_type    VARCHAR(20)     NOT NULL DEFAULT 'GENERAL',
    first_name      VARCHAR(100)    NOT NULL,
    last_name       VARCHAR(100)    NOT NULL,
    job_title       VARCHAR(100),
    department      VARCHAR(100),
    email           VARCHAR(255),
    phone           VARCHAR(20),
    mobile_phone    VARCHAR(20),
    fax             VARCHAR(20),
    is_primary      BOOLEAN         NOT NULL DEFAULT FALSE,
    notes           TEXT,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT ck_contact_type CHECK (contact_type IN (
        'GENERAL', 'SALES', 'SUPPORT', 'BILLING', 'SHIPPING', 'EXECUTIVE'
    ))
);

COMMENT ON TABLE supplier_contact IS '仕入先連絡先 - 仕入先企業の担当者情報';
COMMENT ON COLUMN supplier_contact.contact_type IS '連絡先種別: GENERAL=一般, SALES=営業, BILLING=経理等';
COMMENT ON COLUMN supplier_contact.is_primary IS '主要連絡先フラグ';

-- ----------------------------------------------------------------------------
-- 仕入先住所テーブル
-- 仕入先の本社、工場、倉庫等の所在地情報を管理する。
-- ----------------------------------------------------------------------------
CREATE TABLE supplier_address (
    id              BIGSERIAL       PRIMARY KEY,
    supplier_id     BIGINT          NOT NULL REFERENCES supplier(id) ON DELETE CASCADE,
    address_type    VARCHAR(20)     NOT NULL DEFAULT 'OFFICE',
    address_line1   VARCHAR(300)    NOT NULL,
    address_line2   VARCHAR(300),
    city            VARCHAR(100)    NOT NULL,
    state_province  VARCHAR(100),
    postal_code     VARCHAR(20)     NOT NULL,
    country_code    VARCHAR(3)      NOT NULL DEFAULT 'JPN',
    is_primary      BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT ck_address_type CHECK (address_type IN (
        'OFFICE', 'FACTORY', 'WAREHOUSE', 'BILLING', 'SHIPPING'
    ))
);

COMMENT ON TABLE supplier_address IS '仕入先住所 - 仕入先の所在地情報';
COMMENT ON COLUMN supplier_address.address_type IS '住所種別: OFFICE=本社, FACTORY=工場, WAREHOUSE=倉庫等';

-- ----------------------------------------------------------------------------
-- 仕入先取扱商品テーブル
-- 仕入先が供給可能な商品と、仕入先固有の価格情報を管理する。
-- 同一商品でも仕入先により型番・価格・リードタイムが異なる。
-- ----------------------------------------------------------------------------
CREATE TABLE supplier_product (
    id              BIGSERIAL       PRIMARY KEY,
    supplier_id     BIGINT          NOT NULL REFERENCES supplier(id) ON DELETE CASCADE,
    product_id      BIGINT          NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    supplier_sku    VARCHAR(50),
    supplier_product_name VARCHAR(300),
    unit_price      NUMERIC(15, 2) NOT NULL,
    currency_code   VARCHAR(3)      NOT NULL DEFAULT 'JPY',
    min_order_qty   INTEGER         NOT NULL DEFAULT 1,
    lead_time_days  INTEGER         NOT NULL DEFAULT 0,
    is_preferred    BOOLEAN         NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    last_price_update DATE,
    notes           TEXT,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT uq_supplier_product UNIQUE (supplier_id, product_id),
    CONSTRAINT ck_supplier_product_price CHECK (unit_price >= 0),
    CONSTRAINT ck_supplier_product_moq CHECK (min_order_qty >= 1),
    CONSTRAINT ck_supplier_product_lead_time CHECK (lead_time_days >= 0)
);

COMMENT ON TABLE supplier_product IS '仕入先取扱商品 - 仕入先ごとの商品情報・価格';
COMMENT ON COLUMN supplier_product.supplier_sku IS '仕入先側の商品コード（仕入先カタログ番号）';
COMMENT ON COLUMN supplier_product.is_preferred IS '優先仕入先フラグ（この商品の推奨調達先）';

-- ----------------------------------------------------------------------------
-- 仕入先契約テーブル
-- 仕入先との取引契約情報を管理する。価格協定、数量コミット等。
-- ----------------------------------------------------------------------------
CREATE TABLE supplier_contract (
    id              BIGSERIAL       PRIMARY KEY,
    supplier_id     BIGINT          NOT NULL REFERENCES supplier(id) ON DELETE CASCADE,
    contract_number VARCHAR(50)     NOT NULL,
    contract_type   VARCHAR(30)     NOT NULL DEFAULT 'GENERAL',
    title           VARCHAR(300)    NOT NULL,
    description     TEXT,
    status          VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    start_date      DATE            NOT NULL,
    end_date        DATE            NOT NULL,
    total_value     NUMERIC(15, 2),
    currency_code   VARCHAR(3)      NOT NULL DEFAULT 'JPY',
    auto_renew      BOOLEAN         NOT NULL DEFAULT FALSE,
    renewal_notice_days INTEGER     NOT NULL DEFAULT 30,
    terms_and_conditions TEXT,
    signed_by       BIGINT          REFERENCES user_profile(id) ON DELETE SET NULL,
    signed_at       TIMESTAMP,
    document_url    VARCHAR(500),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT uq_contract_number UNIQUE (contract_number),
    CONSTRAINT ck_contract_type CHECK (contract_type IN (
        'GENERAL', 'FRAMEWORK', 'BLANKET_ORDER', 'SERVICE_LEVEL', 'NDA'
    )),
    CONSTRAINT ck_contract_status CHECK (status IN (
        'DRAFT', 'PENDING_APPROVAL', 'ACTIVE', 'EXPIRED', 'TERMINATED', 'RENEWED'
    )),
    CONSTRAINT ck_contract_date_range CHECK (end_date > start_date),
    CONSTRAINT ck_contract_total_value CHECK (total_value IS NULL OR total_value >= 0),
    CONSTRAINT ck_contract_renewal_notice CHECK (renewal_notice_days >= 0)
);

COMMENT ON TABLE supplier_contract IS '仕入先契約 - 取引契約の基本情報と条件';
COMMENT ON COLUMN supplier_contract.contract_type IS '契約種別: FRAMEWORK=基本契約, BLANKET_ORDER=包括発注等';
COMMENT ON COLUMN supplier_contract.auto_renew IS '自動更新フラグ';
COMMENT ON COLUMN supplier_contract.renewal_notice_days IS '更新通知期限（契約終了の何日前に通知するか）';

-- ----------------------------------------------------------------------------
-- 仕入先契約明細テーブル
-- 契約に含まれる個々の商品と合意価格を定義する。
-- ----------------------------------------------------------------------------
CREATE TABLE supplier_contract_item (
    id              BIGSERIAL       PRIMARY KEY,
    contract_id     BIGINT          NOT NULL REFERENCES supplier_contract(id) ON DELETE CASCADE,
    product_id      BIGINT          NOT NULL REFERENCES product(id) ON DELETE RESTRICT,
    agreed_price    NUMERIC(15, 2) NOT NULL,
    currency_code   VARCHAR(3)      NOT NULL DEFAULT 'JPY',
    min_quantity     INTEGER,
    max_quantity     INTEGER,
    committed_qty   INTEGER,
    delivered_qty   INTEGER         NOT NULL DEFAULT 0,
    notes           TEXT,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT uq_contract_item UNIQUE (contract_id, product_id),
    CONSTRAINT ck_contract_item_price CHECK (agreed_price >= 0),
    CONSTRAINT ck_contract_item_qty_range CHECK (
        max_quantity IS NULL OR min_quantity IS NULL OR max_quantity >= min_quantity
    ),
    CONSTRAINT ck_contract_item_delivered CHECK (delivered_qty >= 0)
);

COMMENT ON TABLE supplier_contract_item IS '仕入先契約明細 - 契約対象商品と合意価格';
COMMENT ON COLUMN supplier_contract_item.committed_qty IS 'コミット数量（契約期間内の購入約束数量）';
COMMENT ON COLUMN supplier_contract_item.delivered_qty IS '納品済数量（コミットに対する実績）';

-- ----------------------------------------------------------------------------
-- 仕入先評価テーブル
-- 仕入先の品質・納期・価格等に関する評価を記録する。
-- 定期的な評価とスポット評価の両方に対応する。
-- ----------------------------------------------------------------------------
CREATE TABLE supplier_rating (
    id              BIGSERIAL       PRIMARY KEY,
    supplier_id     BIGINT          NOT NULL REFERENCES supplier(id) ON DELETE CASCADE,
    rated_by        BIGINT          NOT NULL REFERENCES user_profile(id) ON DELETE RESTRICT,
    rating_period   VARCHAR(20)     NOT NULL,
    quality_score   NUMERIC(3, 1)   NOT NULL,
    delivery_score  NUMERIC(3, 1)   NOT NULL,
    price_score     NUMERIC(3, 1)   NOT NULL,
    service_score   NUMERIC(3, 1)   NOT NULL,
    overall_score   NUMERIC(3, 1)   NOT NULL,
    comments        TEXT,
    rated_at        DATE            NOT NULL DEFAULT CURRENT_DATE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT ck_rating_quality CHECK (quality_score >= 0 AND quality_score <= 5),
    CONSTRAINT ck_rating_delivery CHECK (delivery_score >= 0 AND delivery_score <= 5),
    CONSTRAINT ck_rating_price CHECK (price_score >= 0 AND price_score <= 5),
    CONSTRAINT ck_rating_service CHECK (service_score >= 0 AND service_score <= 5),
    CONSTRAINT ck_rating_overall CHECK (overall_score >= 0 AND overall_score <= 5)
);

COMMENT ON TABLE supplier_rating IS '仕入先評価 - 仕入先のパフォーマンス評価スコア';
COMMENT ON COLUMN supplier_rating.rating_period IS '評価対象期間（例: 2025Q1, 2025H1, SPOT）';
COMMENT ON COLUMN supplier_rating.quality_score IS '品質スコア（0.0〜5.0）';
COMMENT ON COLUMN supplier_rating.overall_score IS '総合スコア（0.0〜5.0）';

-- ----------------------------------------------------------------------------
-- 仕入先認証テーブル
-- 仕入先が取得している認証（ISO、品質認証等）を管理する。
-- 有効期限の管理により、認証失効のリスクを把握する。
-- ----------------------------------------------------------------------------
CREATE TABLE supplier_certification (
    id              BIGSERIAL       PRIMARY KEY,
    supplier_id     BIGINT          NOT NULL REFERENCES supplier(id) ON DELETE CASCADE,
    certification_name VARCHAR(200) NOT NULL,
    certification_body VARCHAR(200),
    certificate_number VARCHAR(100),
    issued_date     DATE            NOT NULL,
    expiry_date     DATE,
    document_url    VARCHAR(500),
    status          VARCHAR(20)     NOT NULL DEFAULT 'VALID',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT ck_certification_status CHECK (status IN ('VALID', 'EXPIRED', 'REVOKED', 'PENDING_RENEWAL')),
    CONSTRAINT ck_certification_dates CHECK (expiry_date IS NULL OR expiry_date > issued_date)
);

COMMENT ON TABLE supplier_certification IS '仕入先認証 - ISO等の取得認証情報と有効期限管理';
COMMENT ON COLUMN supplier_certification.certification_body IS '認証機関名';
COMMENT ON COLUMN supplier_certification.status IS '認証状態: VALID=有効, EXPIRED=期限切れ, REVOKED=取消';
