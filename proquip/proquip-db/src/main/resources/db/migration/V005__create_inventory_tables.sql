-- ============================================================================
-- ProQuip - Enterprise Procurement & Inventory Management System
-- V005: 在庫管理テーブルの作成
-- 倉庫、ゾーン、保管場所、在庫、入出庫履歴、在庫移動、棚卸
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 倉庫テーブル
-- 在庫を保管する物理的な倉庫拠点を管理する。
-- ----------------------------------------------------------------------------
CREATE TABLE warehouse (
    id              BIGSERIAL       PRIMARY KEY,
    warehouse_code  VARCHAR(20)     NOT NULL,
    name            VARCHAR(200)    NOT NULL,
    warehouse_type  VARCHAR(20)     NOT NULL DEFAULT 'GENERAL',
    address_line1   VARCHAR(300)    NOT NULL,
    address_line2   VARCHAR(300),
    city            VARCHAR(100)    NOT NULL,
    state_province  VARCHAR(100),
    postal_code     VARCHAR(20)     NOT NULL,
    country_code    VARCHAR(3)      NOT NULL DEFAULT 'JPN',
    phone           VARCHAR(20),
    manager_id      BIGINT          REFERENCES user_profile(id) ON DELETE SET NULL,
    capacity_sqm    NUMERIC(10, 2),
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    operating_hours VARCHAR(100),
    notes           TEXT,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT uq_warehouse_code UNIQUE (warehouse_code),
    CONSTRAINT ck_warehouse_type CHECK (warehouse_type IN (
        'GENERAL', 'COLD_STORAGE', 'HAZARDOUS', 'BONDED', 'TRANSIT', 'RETURNS'
    )),
    CONSTRAINT ck_warehouse_capacity CHECK (capacity_sqm IS NULL OR capacity_sqm > 0)
);

COMMENT ON TABLE warehouse IS '倉庫マスタ - 在庫保管拠点の基本情報';
COMMENT ON COLUMN warehouse.warehouse_type IS '倉庫種別: GENERAL=一般, COLD_STORAGE=冷蔵, HAZARDOUS=危険物, BONDED=保税等';
COMMENT ON COLUMN warehouse.capacity_sqm IS '倉庫面積（平方メートル）';

-- 検収テーブルに倉庫への外部キーを追加
ALTER TABLE goods_receipt
    ADD CONSTRAINT fk_goods_receipt_warehouse
    FOREIGN KEY (warehouse_id) REFERENCES warehouse(id) ON DELETE SET NULL;

-- ----------------------------------------------------------------------------
-- 倉庫ゾーンテーブル
-- 倉庫内のエリア区分を管理する。温度帯や用途で分類する。
-- ----------------------------------------------------------------------------
CREATE TABLE warehouse_zone (
    id              BIGSERIAL       PRIMARY KEY,
    warehouse_id    BIGINT          NOT NULL REFERENCES warehouse(id) ON DELETE CASCADE,
    zone_code       VARCHAR(20)     NOT NULL,
    name            VARCHAR(200)    NOT NULL,
    zone_type       VARCHAR(30)     NOT NULL DEFAULT 'GENERAL',
    temperature_min NUMERIC(5, 1),
    temperature_max NUMERIC(5, 1),
    humidity_min    NUMERIC(5, 1),
    humidity_max    NUMERIC(5, 1),
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT uq_warehouse_zone UNIQUE (warehouse_id, zone_code),
    CONSTRAINT ck_zone_type CHECK (zone_type IN (
        'GENERAL', 'RECEIVING', 'SHIPPING', 'PICKING', 'BULK', 'COLD', 'HAZARDOUS', 'QUARANTINE'
    )),
    CONSTRAINT ck_zone_temperature CHECK (
        temperature_min IS NULL OR temperature_max IS NULL OR temperature_max >= temperature_min
    ),
    CONSTRAINT ck_zone_humidity CHECK (
        humidity_min IS NULL OR humidity_max IS NULL OR humidity_max >= humidity_min
    )
);

COMMENT ON TABLE warehouse_zone IS '倉庫ゾーン - 倉庫内のエリア区分';
COMMENT ON COLUMN warehouse_zone.zone_type IS 'ゾーン種別: RECEIVING=入荷, SHIPPING=出荷, PICKING=ピッキング, QUARANTINE=検疫等';

-- ----------------------------------------------------------------------------
-- 保管場所テーブル
-- 倉庫ゾーン内の具体的な保管ロケーション（棚、ラック等）を管理する。
-- aisle-rack-shelf-bin の階層で位置を特定する。
-- ----------------------------------------------------------------------------
CREATE TABLE storage_location (
    id              BIGSERIAL       PRIMARY KEY,
    zone_id         BIGINT          NOT NULL REFERENCES warehouse_zone(id) ON DELETE CASCADE,
    location_code   VARCHAR(30)     NOT NULL,
    aisle           VARCHAR(10),
    rack            VARCHAR(10),
    shelf           VARCHAR(10),
    bin             VARCHAR(10),
    location_type   VARCHAR(20)     NOT NULL DEFAULT 'SHELF',
    max_weight_kg   NUMERIC(10, 2),
    max_volume_m3   NUMERIC(10, 4),
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    is_occupied     BOOLEAN         NOT NULL DEFAULT FALSE,
    barcode         VARCHAR(50),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT uq_storage_location_code UNIQUE (zone_id, location_code),
    CONSTRAINT ck_location_type CHECK (location_type IN (
        'SHELF', 'RACK', 'PALLET', 'BIN', 'FLOOR', 'HANGING'
    )),
    CONSTRAINT ck_location_weight CHECK (max_weight_kg IS NULL OR max_weight_kg > 0),
    CONSTRAINT ck_location_volume CHECK (max_volume_m3 IS NULL OR max_volume_m3 > 0)
);

COMMENT ON TABLE storage_location IS '保管場所 - ゾーン内の具体的な保管ロケーション';
COMMENT ON COLUMN storage_location.location_code IS 'ロケーションコード（例: A-01-03-B）';
COMMENT ON COLUMN storage_location.barcode IS 'ロケーションバーコード（ハンディターミナル用）';

-- 検収明細テーブルに保管場所への外部キーを追加
ALTER TABLE goods_receipt_item
    ADD CONSTRAINT fk_gri_storage_location
    FOREIGN KEY (storage_location_id) REFERENCES storage_location(id) ON DELETE SET NULL;

-- ----------------------------------------------------------------------------
-- 在庫テーブル
-- 商品ごと・倉庫ごとの現在在庫数量を管理する。
-- 手持在庫、引当済、発注中の各数量を個別に追跡する。
-- ----------------------------------------------------------------------------
CREATE TABLE inventory_item (
    id              BIGSERIAL       PRIMARY KEY,
    product_id      BIGINT          NOT NULL REFERENCES product(id) ON DELETE RESTRICT,
    warehouse_id    BIGINT          NOT NULL REFERENCES warehouse(id) ON DELETE RESTRICT,
    storage_location_id BIGINT      REFERENCES storage_location(id) ON DELETE SET NULL,
    quantity_on_hand INTEGER        NOT NULL DEFAULT 0,
    quantity_reserved INTEGER       NOT NULL DEFAULT 0,
    quantity_on_order INTEGER       NOT NULL DEFAULT 0,
    quantity_in_transit INTEGER     NOT NULL DEFAULT 0,
    unit_cost       NUMERIC(15, 2) NOT NULL DEFAULT 0,
    total_value     NUMERIC(15, 2) NOT NULL DEFAULT 0,
    lot_number      VARCHAR(50),
    serial_number   VARCHAR(100),
    expiry_date     DATE,
    last_counted_at TIMESTAMP,
    last_movement_at TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT ck_inv_on_hand CHECK (quantity_on_hand >= 0),
    CONSTRAINT ck_inv_reserved CHECK (quantity_reserved >= 0),
    CONSTRAINT ck_inv_on_order CHECK (quantity_on_order >= 0),
    CONSTRAINT ck_inv_in_transit CHECK (quantity_in_transit >= 0),
    CONSTRAINT ck_inv_reserved_vs_hand CHECK (quantity_reserved <= quantity_on_hand),
    CONSTRAINT ck_inv_unit_cost CHECK (unit_cost >= 0),
    CONSTRAINT ck_inv_total_value CHECK (total_value >= 0)
);

CREATE UNIQUE INDEX uq_inventory_item ON inventory_item (product_id, warehouse_id, COALESCE(lot_number, ''), COALESCE(serial_number, ''));

COMMENT ON TABLE inventory_item IS '在庫 - 商品×倉庫ごとの現在在庫数量';
COMMENT ON COLUMN inventory_item.quantity_on_hand IS '手持在庫数（物理的に倉庫にある数量）';
COMMENT ON COLUMN inventory_item.quantity_reserved IS '引当済数量（出荷予定等で確保済の数量）';
COMMENT ON COLUMN inventory_item.quantity_on_order IS '発注中数量（未納品の発注残数量）';
COMMENT ON COLUMN inventory_item.quantity_in_transit IS '輸送中数量（倉庫間移動中の数量）';

-- ----------------------------------------------------------------------------
-- 在庫トランザクションテーブル
-- すべての在庫変動（入庫、出庫、調整等）を時系列で記録する。
-- 在庫の追跡可能性（トレーサビリティ）を保証するための台帳。
-- ----------------------------------------------------------------------------
CREATE TABLE inventory_transaction (
    id              BIGSERIAL       PRIMARY KEY,
    product_id      BIGINT          NOT NULL REFERENCES product(id) ON DELETE RESTRICT,
    warehouse_id    BIGINT          NOT NULL REFERENCES warehouse(id) ON DELETE RESTRICT,
    transaction_type VARCHAR(30)    NOT NULL,
    reference_type  VARCHAR(50),
    reference_id    BIGINT,
    quantity         INTEGER        NOT NULL,
    quantity_before  INTEGER        NOT NULL,
    quantity_after   INTEGER        NOT NULL,
    unit_cost       NUMERIC(15, 2),
    total_cost      NUMERIC(15, 2),
    lot_number      VARCHAR(50),
    serial_number   VARCHAR(100),
    performed_by    BIGINT          REFERENCES user_profile(id) ON DELETE SET NULL,
    notes           TEXT,
    transaction_date TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT ck_txn_type CHECK (transaction_type IN (
        'RECEIPT', 'ISSUE', 'TRANSFER_IN', 'TRANSFER_OUT',
        'ADJUSTMENT_PLUS', 'ADJUSTMENT_MINUS', 'RETURN_FROM_SUPPLIER',
        'RETURN_TO_SUPPLIER', 'SCRAP', 'COUNT_ADJUSTMENT', 'INITIAL_STOCK'
    )),
    CONSTRAINT ck_txn_reference_type CHECK (reference_type IS NULL OR reference_type IN (
        'PURCHASE_ORDER', 'GOODS_RECEIPT', 'STOCK_TRANSFER', 'INVENTORY_COUNT',
        'RETURN_TO_SUPPLIER', 'MANUAL_ADJUSTMENT'
    )),
    CONSTRAINT ck_txn_quantity_after CHECK (quantity_after >= 0)
);

COMMENT ON TABLE inventory_transaction IS '在庫トランザクション - 全在庫変動の時系列ログ';
COMMENT ON COLUMN inventory_transaction.transaction_type IS '取引種別: RECEIPT=入庫, ISSUE=出庫, ADJUSTMENT_PLUS=増加調整等';
COMMENT ON COLUMN inventory_transaction.reference_type IS '参照元エンティティ種別（ポリモーフィック参照）';
COMMENT ON COLUMN inventory_transaction.quantity_before IS '取引前の在庫数量';
COMMENT ON COLUMN inventory_transaction.quantity_after IS '取引後の在庫数量';

-- ----------------------------------------------------------------------------
-- 在庫移動テーブル
-- 倉庫間の在庫移動（Stock Transfer）を管理する。
-- ----------------------------------------------------------------------------
CREATE TABLE stock_transfer (
    id              BIGSERIAL       PRIMARY KEY,
    transfer_number VARCHAR(30)     NOT NULL,
    from_warehouse_id BIGINT        NOT NULL REFERENCES warehouse(id) ON DELETE RESTRICT,
    to_warehouse_id BIGINT          NOT NULL REFERENCES warehouse(id) ON DELETE RESTRICT,
    requested_by    BIGINT          NOT NULL REFERENCES user_profile(id) ON DELETE RESTRICT,
    approved_by     BIGINT          REFERENCES user_profile(id) ON DELETE SET NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    priority        VARCHAR(10)     NOT NULL DEFAULT 'NORMAL',
    requested_date  DATE            NOT NULL DEFAULT CURRENT_DATE,
    shipped_date    DATE,
    received_date   DATE,
    notes           TEXT,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT uq_transfer_number UNIQUE (transfer_number),
    CONSTRAINT ck_transfer_warehouses CHECK (from_warehouse_id <> to_warehouse_id),
    CONSTRAINT ck_transfer_status CHECK (status IN (
        'DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'IN_TRANSIT', 'RECEIVED',
        'PARTIALLY_RECEIVED', 'CANCELLED'
    )),
    CONSTRAINT ck_transfer_priority CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT'))
);

COMMENT ON TABLE stock_transfer IS '在庫移動 - 倉庫間の在庫移動管理';
COMMENT ON COLUMN stock_transfer.transfer_number IS '移動番号（自動採番、例: ST-2025-000001）';

-- ----------------------------------------------------------------------------
-- 在庫移動明細テーブル
-- 在庫移動に含まれる個々の商品と数量を定義する。
-- ----------------------------------------------------------------------------
CREATE TABLE stock_transfer_item (
    id              BIGSERIAL       PRIMARY KEY,
    transfer_id     BIGINT          NOT NULL REFERENCES stock_transfer(id) ON DELETE CASCADE,
    product_id      BIGINT          NOT NULL REFERENCES product(id) ON DELETE RESTRICT,
    quantity_requested INTEGER      NOT NULL,
    quantity_shipped INTEGER        NOT NULL DEFAULT 0,
    quantity_received INTEGER       NOT NULL DEFAULT 0,
    from_location_id BIGINT         REFERENCES storage_location(id) ON DELETE SET NULL,
    to_location_id  BIGINT          REFERENCES storage_location(id) ON DELETE SET NULL,
    lot_number      VARCHAR(50),
    notes           TEXT,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT ck_sti_qty_requested CHECK (quantity_requested >= 1),
    CONSTRAINT ck_sti_qty_shipped CHECK (quantity_shipped >= 0),
    CONSTRAINT ck_sti_qty_received CHECK (quantity_received >= 0),
    CONSTRAINT ck_sti_shipped_vs_requested CHECK (quantity_shipped <= quantity_requested),
    CONSTRAINT ck_sti_received_vs_shipped CHECK (quantity_received <= quantity_shipped)
);

COMMENT ON TABLE stock_transfer_item IS '在庫移動明細 - 移動対象の商品と数量';

-- ----------------------------------------------------------------------------
-- 棚卸テーブル
-- 定期棚卸（Inventory Count / Physical Inventory）を管理する。
-- 棚卸の計画・実施・結果記録を一元管理する。
-- ----------------------------------------------------------------------------
CREATE TABLE inventory_count (
    id              BIGSERIAL       PRIMARY KEY,
    count_number    VARCHAR(30)     NOT NULL,
    warehouse_id    BIGINT          NOT NULL REFERENCES warehouse(id) ON DELETE RESTRICT,
    count_type      VARCHAR(20)     NOT NULL DEFAULT 'FULL',
    status          VARCHAR(20)     NOT NULL DEFAULT 'PLANNED',
    planned_date    DATE            NOT NULL,
    started_at      TIMESTAMP,
    completed_at    TIMESTAMP,
    initiated_by    BIGINT          NOT NULL REFERENCES user_profile(id) ON DELETE RESTRICT,
    approved_by     BIGINT          REFERENCES user_profile(id) ON DELETE SET NULL,
    total_items     INTEGER         NOT NULL DEFAULT 0,
    counted_items   INTEGER         NOT NULL DEFAULT 0,
    discrepancy_items INTEGER      NOT NULL DEFAULT 0,
    notes           TEXT,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT uq_count_number UNIQUE (count_number),
    CONSTRAINT ck_count_type CHECK (count_type IN ('FULL', 'CYCLE', 'SPOT', 'ANNUAL')),
    CONSTRAINT ck_count_status CHECK (status IN (
        'PLANNED', 'IN_PROGRESS', 'COMPLETED', 'APPROVED', 'CANCELLED'
    )),
    CONSTRAINT ck_count_items CHECK (counted_items <= total_items),
    CONSTRAINT ck_count_discrepancy CHECK (discrepancy_items <= counted_items)
);

COMMENT ON TABLE inventory_count IS '棚卸 - 在庫棚卸の計画・実施・結果管理';
COMMENT ON COLUMN inventory_count.count_type IS '棚卸種別: FULL=全数, CYCLE=循環, SPOT=スポット, ANNUAL=年次';

-- ----------------------------------------------------------------------------
-- 棚卸明細テーブル
-- 棚卸における個々の商品のカウント結果と差異を記録する。
-- ----------------------------------------------------------------------------
CREATE TABLE inventory_count_item (
    id              BIGSERIAL       PRIMARY KEY,
    count_id        BIGINT          NOT NULL REFERENCES inventory_count(id) ON DELETE CASCADE,
    product_id      BIGINT          NOT NULL REFERENCES product(id) ON DELETE RESTRICT,
    storage_location_id BIGINT      REFERENCES storage_location(id) ON DELETE SET NULL,
    system_quantity INTEGER         NOT NULL,
    counted_quantity INTEGER,
    variance        INTEGER,
    variance_value  NUMERIC(15, 2),
    counted_by      BIGINT          REFERENCES user_profile(id) ON DELETE SET NULL,
    counted_at      TIMESTAMP,
    adjustment_applied BOOLEAN      NOT NULL DEFAULT FALSE,
    notes           TEXT,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT ck_ci_system_qty CHECK (system_quantity >= 0),
    CONSTRAINT ck_ci_counted_qty CHECK (counted_quantity IS NULL OR counted_quantity >= 0)
);

CREATE UNIQUE INDEX uq_count_item ON inventory_count_item (count_id, product_id, COALESCE(storage_location_id, 0));

COMMENT ON TABLE inventory_count_item IS '棚卸明細 - 商品ごとのカウント結果と差異';
COMMENT ON COLUMN inventory_count_item.system_quantity IS 'システム上の在庫数量';
COMMENT ON COLUMN inventory_count_item.counted_quantity IS '実地棚卸で数えた数量';
COMMENT ON COLUMN inventory_count_item.variance IS '差異（counted_quantity - system_quantity）';
COMMENT ON COLUMN inventory_count_item.adjustment_applied IS '差異を在庫に反映済みかどうか';
