-- ============================================================================
-- ProQuip - Enterprise Procurement & Inventory Management System
-- V002: 商品管理テーブルの作成
-- メーカー、カテゴリ、単位、商品、仕様、画像、タグ、代替品、バンドル等
-- ============================================================================

-- ----------------------------------------------------------------------------
-- メーカーテーブル
-- 商品の製造元企業情報を管理する。
-- ----------------------------------------------------------------------------
CREATE TABLE manufacturer (
    id              BIGSERIAL       PRIMARY KEY,
    manufacturer_code VARCHAR(20)   NOT NULL,
    name            VARCHAR(200)    NOT NULL,
    name_en         VARCHAR(200),
    website         VARCHAR(500),
    country         VARCHAR(3),
    description     TEXT,
    logo_url        VARCHAR(500),
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT uq_manufacturer_code UNIQUE (manufacturer_code),
    CONSTRAINT ck_manufacturer_country CHECK (country IS NULL OR LENGTH(country) = 3)
);

COMMENT ON TABLE manufacturer IS 'メーカーマスタ - 商品の製造元企業情報';
COMMENT ON COLUMN manufacturer.country IS '国コード（ISO 3166-1 alpha-3）';

-- ----------------------------------------------------------------------------
-- カテゴリテーブル
-- 商品の分類体系を管理する。parent_id による自己参照で階層構造を表現する。
-- level カラムでカテゴリの深さを明示的に保持し、クエリの効率化を図る。
-- ----------------------------------------------------------------------------
CREATE TABLE category (
    id              BIGSERIAL       PRIMARY KEY,
    parent_id       BIGINT          REFERENCES category(id) ON DELETE RESTRICT,
    category_code   VARCHAR(20)     NOT NULL,
    name            VARCHAR(200)    NOT NULL,
    name_en         VARCHAR(200),
    description     TEXT,
    level           INTEGER         NOT NULL DEFAULT 0,
    sort_order      INTEGER         NOT NULL DEFAULT 0,
    path            VARCHAR(500),
    icon_name       VARCHAR(100),
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT uq_category_code UNIQUE (category_code),
    CONSTRAINT ck_category_level CHECK (level >= 0 AND level <= 10),
    CONSTRAINT ck_category_sort_order CHECK (sort_order >= 0)
);

COMMENT ON TABLE category IS 'カテゴリマスタ - 商品分類の階層構造';
COMMENT ON COLUMN category.level IS '階層レベル（0=ルートカテゴリ、最大10階層）';
COMMENT ON COLUMN category.path IS 'マテリアライズドパス（例: /1/5/12/）- ツリー検索の高速化用';

-- ----------------------------------------------------------------------------
-- 単位テーブル
-- 商品の数量単位を管理する。base_unit_id で基本単位への変換関係を定義する。
-- 例: 1箱 = 12個 のように、conversion_factor で基本単位との換算係数を保持する。
-- ----------------------------------------------------------------------------
CREATE TABLE unit_of_measure (
    id              BIGSERIAL       PRIMARY KEY,
    base_unit_id    BIGINT          REFERENCES unit_of_measure(id) ON DELETE RESTRICT,
    unit_code       VARCHAR(10)     NOT NULL,
    name            VARCHAR(100)    NOT NULL,
    name_en         VARCHAR(100),
    symbol          VARCHAR(10),
    conversion_factor NUMERIC(15, 6) NOT NULL DEFAULT 1.0,
    unit_type       VARCHAR(30)     NOT NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT uq_unit_code UNIQUE (unit_code),
    CONSTRAINT ck_unit_conversion_factor CHECK (conversion_factor > 0),
    CONSTRAINT ck_unit_type CHECK (unit_type IN (
        'LENGTH', 'WEIGHT', 'VOLUME', 'AREA', 'COUNT', 'PACK', 'TIME', 'OTHER'
    ))
);

COMMENT ON TABLE unit_of_measure IS '単位マスタ - 数量単位と基本単位への換算係数';
COMMENT ON COLUMN unit_of_measure.base_unit_id IS '基本単位ID（NULLの場合は自身が基本単位）';
COMMENT ON COLUMN unit_of_measure.conversion_factor IS '基本単位への換算係数（例: 1箱=12個なら12.0）';

-- ----------------------------------------------------------------------------
-- 商品テーブル
-- 調達・在庫管理の対象となる商品の基本情報を保持する。
-- SKU（Stock Keeping Unit）で一意に識別される。
-- ----------------------------------------------------------------------------
CREATE TABLE product (
    id              BIGSERIAL       PRIMARY KEY,
    sku             VARCHAR(50)     NOT NULL,
    name            VARCHAR(300)    NOT NULL,
    name_en         VARCHAR(300),
    description     TEXT,
    category_id     BIGINT          NOT NULL REFERENCES category(id) ON DELETE RESTRICT,
    manufacturer_id BIGINT          REFERENCES manufacturer(id) ON DELETE SET NULL,
    unit_id         BIGINT          NOT NULL REFERENCES unit_of_measure(id) ON DELETE RESTRICT,
    unit_price      NUMERIC(15, 2) NOT NULL DEFAULT 0,
    currency_code   VARCHAR(3)      NOT NULL DEFAULT 'JPY',
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    min_order_qty   INTEGER         NOT NULL DEFAULT 1,
    max_order_qty   INTEGER,
    reorder_point   INTEGER         NOT NULL DEFAULT 0,
    reorder_qty     INTEGER         NOT NULL DEFAULT 0,
    lead_time_days  INTEGER         NOT NULL DEFAULT 0,
    weight_kg       NUMERIC(10, 3),
    width_mm        NUMERIC(10, 2),
    height_mm       NUMERIC(10, 2),
    depth_mm        NUMERIC(10, 2),
    model_number    VARCHAR(100),
    barcode         VARCHAR(50),
    is_hazardous    BOOLEAN         NOT NULL DEFAULT FALSE,
    is_serialized   BOOLEAN         NOT NULL DEFAULT FALSE,
    notes           TEXT,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT uq_product_sku UNIQUE (sku),
    CONSTRAINT ck_product_unit_price CHECK (unit_price >= 0),
    CONSTRAINT ck_product_status CHECK (status IN (
        'ACTIVE', 'INACTIVE', 'DISCONTINUED', 'PENDING_APPROVAL', 'DRAFT'
    )),
    CONSTRAINT ck_product_min_order_qty CHECK (min_order_qty >= 1),
    CONSTRAINT ck_product_max_order_qty CHECK (max_order_qty IS NULL OR max_order_qty >= min_order_qty),
    CONSTRAINT ck_product_reorder_point CHECK (reorder_point >= 0),
    CONSTRAINT ck_product_reorder_qty CHECK (reorder_qty >= 0),
    CONSTRAINT ck_product_lead_time CHECK (lead_time_days >= 0),
    CONSTRAINT ck_product_weight CHECK (weight_kg IS NULL OR weight_kg >= 0),
    CONSTRAINT ck_product_dimensions CHECK (
        (width_mm IS NULL OR width_mm >= 0) AND
        (height_mm IS NULL OR height_mm >= 0) AND
        (depth_mm IS NULL OR depth_mm >= 0)
    )
);

COMMENT ON TABLE product IS '商品マスタ - 調達・在庫管理対象の商品基本情報';
COMMENT ON COLUMN product.sku IS 'SKU（Stock Keeping Unit）- 在庫管理単位の一意識別子';
COMMENT ON COLUMN product.min_order_qty IS '最小発注数量';
COMMENT ON COLUMN product.reorder_point IS '発注点（この在庫数を下回ったら補充が必要）';
COMMENT ON COLUMN product.reorder_qty IS '補充発注数量';
COMMENT ON COLUMN product.lead_time_days IS 'リードタイム（発注から納品までの日数）';
COMMENT ON COLUMN product.is_serialized IS 'シリアル番号管理対象フラグ';

-- ----------------------------------------------------------------------------
-- 商品仕様テーブル
-- 商品の技術仕様を key-value ペアで柔軟に保持する。
-- 型情報を持つことで、数値比較やフィルタリングに対応する。
-- ----------------------------------------------------------------------------
CREATE TABLE product_specification (
    id              BIGSERIAL       PRIMARY KEY,
    product_id      BIGINT          NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    spec_name       VARCHAR(100)    NOT NULL,
    spec_value      VARCHAR(500)    NOT NULL,
    spec_unit       VARCHAR(50),
    value_type      VARCHAR(20)     NOT NULL DEFAULT 'TEXT',
    numeric_value   NUMERIC(15, 4),
    sort_order      INTEGER         NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT uq_product_spec UNIQUE (product_id, spec_name),
    CONSTRAINT ck_spec_value_type CHECK (value_type IN ('TEXT', 'NUMERIC', 'BOOLEAN', 'DATE'))
);

COMMENT ON TABLE product_specification IS '商品仕様 - 商品の技術仕様（key-valueペア）';
COMMENT ON COLUMN product_specification.value_type IS '値の型情報（フィルタリング・ソート用）';
COMMENT ON COLUMN product_specification.numeric_value IS '数値型の場合の数値表現（数値比較用）';

-- ----------------------------------------------------------------------------
-- 商品画像テーブル
-- 商品に紐づく画像ファイルのメタ情報を管理する。
-- ----------------------------------------------------------------------------
CREATE TABLE product_image (
    id              BIGSERIAL       PRIMARY KEY,
    product_id      BIGINT          NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    image_url       VARCHAR(500)    NOT NULL,
    thumbnail_url   VARCHAR(500),
    alt_text        VARCHAR(200),
    image_type      VARCHAR(20)     NOT NULL DEFAULT 'PHOTO',
    sort_order      INTEGER         NOT NULL DEFAULT 0,
    is_primary      BOOLEAN         NOT NULL DEFAULT FALSE,
    file_size_bytes BIGINT,
    width_px        INTEGER,
    height_px       INTEGER,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT ck_image_type CHECK (image_type IN ('PHOTO', 'DIAGRAM', 'DRAWING', 'ICON', 'OTHER')),
    CONSTRAINT ck_image_file_size CHECK (file_size_bytes IS NULL OR file_size_bytes > 0)
);

COMMENT ON TABLE product_image IS '商品画像 - 商品に紐づく画像のメタ情報';
COMMENT ON COLUMN product_image.is_primary IS 'メイン画像フラグ（商品ごとに1つのみ推奨）';

-- ----------------------------------------------------------------------------
-- 商品タグテーブル
-- 商品に自由に付与できるタグの定義。検索・分類の柔軟性を高める。
-- ----------------------------------------------------------------------------
CREATE TABLE product_tag (
    id              BIGSERIAL       PRIMARY KEY,
    tag_name        VARCHAR(100)    NOT NULL,
    tag_color       VARCHAR(7),
    description     TEXT,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT uq_product_tag_name UNIQUE (tag_name),
    CONSTRAINT ck_tag_color CHECK (tag_color IS NULL OR tag_color ~ '^#[0-9A-Fa-f]{6}$')
);

COMMENT ON TABLE product_tag IS '商品タグマスタ - 商品の自由分類用タグ定義';
COMMENT ON COLUMN product_tag.tag_color IS 'タグ表示色（HEXカラーコード）';

-- ----------------------------------------------------------------------------
-- 商品・タグマッピングテーブル
-- 商品とタグの多対多関係を管理する中間テーブル。
-- ----------------------------------------------------------------------------
CREATE TABLE product_tag_mapping (
    product_id      BIGINT          NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    tag_id          BIGINT          NOT NULL REFERENCES product_tag(id) ON DELETE CASCADE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT pk_product_tag_mapping PRIMARY KEY (product_id, tag_id)
);

COMMENT ON TABLE product_tag_mapping IS '商品・タグ対応 - 商品とタグの多対多マッピング';

-- ----------------------------------------------------------------------------
-- 代替商品テーブル
-- 商品間の代替関係を管理する。自己結合で2つの商品を関連付ける。
-- priority により代替品の優先順位を表現する。
-- ----------------------------------------------------------------------------
CREATE TABLE product_alternative (
    id              BIGSERIAL       PRIMARY KEY,
    product_id      BIGINT          NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    alternative_product_id BIGINT   NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    priority        INTEGER         NOT NULL DEFAULT 0,
    compatibility   VARCHAR(20)     NOT NULL DEFAULT 'EQUIVALENT',
    notes           TEXT,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT uq_product_alternative UNIQUE (product_id, alternative_product_id),
    CONSTRAINT ck_alternative_not_self CHECK (product_id <> alternative_product_id),
    CONSTRAINT ck_alternative_compatibility CHECK (compatibility IN (
        'EQUIVALENT', 'SIMILAR', 'UPGRADE', 'DOWNGRADE'
    )),
    CONSTRAINT ck_alternative_priority CHECK (priority >= 0)
);

COMMENT ON TABLE product_alternative IS '代替商品 - 商品間の代替・互換関係';
COMMENT ON COLUMN product_alternative.compatibility IS '互換性レベル: EQUIVALENT=同等, SIMILAR=類似, UPGRADE=上位互換, DOWNGRADE=下位互換';

-- ----------------------------------------------------------------------------
-- 商品バンドルテーブル
-- 複数商品をセットにしたバンドル（パッケージ）商品を定義する。
-- ----------------------------------------------------------------------------
CREATE TABLE product_bundle (
    id              BIGSERIAL       PRIMARY KEY,
    bundle_code     VARCHAR(50)     NOT NULL,
    name            VARCHAR(300)    NOT NULL,
    description     TEXT,
    bundle_price    NUMERIC(15, 2),
    discount_pct    NUMERIC(5, 2)   NOT NULL DEFAULT 0,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    valid_from      DATE,
    valid_until     DATE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT uq_bundle_code UNIQUE (bundle_code),
    CONSTRAINT ck_bundle_price CHECK (bundle_price IS NULL OR bundle_price >= 0),
    CONSTRAINT ck_bundle_discount CHECK (discount_pct >= 0 AND discount_pct <= 100),
    CONSTRAINT ck_bundle_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'DRAFT')),
    CONSTRAINT ck_bundle_valid_range CHECK (valid_until IS NULL OR valid_until >= valid_from)
);

COMMENT ON TABLE product_bundle IS '商品バンドル - 複数商品をまとめたセット商品の定義';
COMMENT ON COLUMN product_bundle.discount_pct IS 'バンドル割引率（%）';

-- ----------------------------------------------------------------------------
-- 商品バンドル明細テーブル
-- バンドルに含まれる個々の商品とその数量を定義する。
-- ----------------------------------------------------------------------------
CREATE TABLE product_bundle_item (
    id              BIGSERIAL       PRIMARY KEY,
    bundle_id       BIGINT          NOT NULL REFERENCES product_bundle(id) ON DELETE CASCADE,
    product_id      BIGINT          NOT NULL REFERENCES product(id) ON DELETE RESTRICT,
    quantity        INTEGER         NOT NULL DEFAULT 1,
    sort_order      INTEGER         NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT uq_bundle_item UNIQUE (bundle_id, product_id),
    CONSTRAINT ck_bundle_item_qty CHECK (quantity >= 1)
);

COMMENT ON TABLE product_bundle_item IS '商品バンドル明細 - バンドルに含まれる商品と数量';

-- ----------------------------------------------------------------------------
-- 商品変更履歴テーブル
-- 商品マスタの変更内容をログとして記録する。
-- old_values / new_values に変更前後の値をJSONBで保持する。
-- ----------------------------------------------------------------------------
CREATE TABLE product_change_log (
    id              BIGSERIAL       PRIMARY KEY,
    product_id      BIGINT          NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    changed_by      BIGINT          REFERENCES user_profile(id) ON DELETE SET NULL,
    change_type     VARCHAR(20)     NOT NULL,
    field_name      VARCHAR(100),
    old_value       TEXT,
    new_value       TEXT,
    change_reason   TEXT,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT ck_change_type CHECK (change_type IN ('CREATE', 'UPDATE', 'DELETE', 'STATUS_CHANGE'))
);

COMMENT ON TABLE product_change_log IS '商品変更履歴 - 商品マスタの変更ログ';
COMMENT ON COLUMN product_change_log.change_type IS '変更種別: CREATE=新規作成, UPDATE=更新, DELETE=削除, STATUS_CHANGE=状態変更';

-- ----------------------------------------------------------------------------
-- 商品ドキュメントテーブル
-- 商品に関連する技術文書（仕様書、MSDS、マニュアル等）を管理する。
-- ----------------------------------------------------------------------------
CREATE TABLE product_document (
    id              BIGSERIAL       PRIMARY KEY,
    product_id      BIGINT          NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    document_name   VARCHAR(300)    NOT NULL,
    document_type   VARCHAR(30)     NOT NULL,
    file_url        VARCHAR(500)    NOT NULL,
    file_size_bytes BIGINT,
    mime_type       VARCHAR(100),
    language        VARCHAR(10)     NOT NULL DEFAULT 'ja',
    document_version VARCHAR(20),
    uploaded_by     BIGINT          REFERENCES user_profile(id) ON DELETE SET NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT ck_document_type CHECK (document_type IN (
        'DATASHEET', 'MSDS', 'MANUAL', 'DRAWING', 'CERTIFICATE', 'WARRANTY', 'BROCHURE', 'OTHER'
    )),
    CONSTRAINT ck_document_file_size CHECK (file_size_bytes IS NULL OR file_size_bytes > 0)
);

COMMENT ON TABLE product_document IS '商品ドキュメント - 商品関連の技術文書・資料';
COMMENT ON COLUMN product_document.document_type IS '文書種別: DATASHEET=仕様書, MSDS=安全データシート, MANUAL=マニュアル等';
