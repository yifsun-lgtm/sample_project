-- ============================================================================
-- ProQuip - Enterprise Procurement & Inventory Management System
-- V004: 調達管理テーブルの作成
-- 購買依頼、発注、承認ワークフロー、検収、返品
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 購買依頼テーブル
-- 各部門からの購買依頼（Purchase Requisition）を管理する。
-- 承認後に発注書（Purchase Order）に変換される。
-- ----------------------------------------------------------------------------
CREATE TABLE purchase_requisition (
    id              BIGSERIAL       PRIMARY KEY,
    requisition_number VARCHAR(30)  NOT NULL,
    title           VARCHAR(300)    NOT NULL,
    description     TEXT,
    requester_id    BIGINT          NOT NULL REFERENCES user_profile(id) ON DELETE RESTRICT,
    department_id   BIGINT          NOT NULL REFERENCES department(id) ON DELETE RESTRICT,
    status          VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    priority        VARCHAR(10)     NOT NULL DEFAULT 'NORMAL',
    required_date   DATE,
    total_amount    NUMERIC(15, 2) NOT NULL DEFAULT 0,
    currency_code   VARCHAR(3)      NOT NULL DEFAULT 'JPY',
    justification   TEXT,
    budget_id       BIGINT,         -- FK追加はV006のbudgetテーブル作成後
    rejected_reason TEXT,
    submitted_at    TIMESTAMP,
    approved_at     TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT uq_requisition_number UNIQUE (requisition_number),
    CONSTRAINT ck_requisition_status CHECK (status IN (
        'DRAFT', 'SUBMITTED', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED',
        'PARTIALLY_ORDERED', 'ORDERED', 'CANCELLED'
    )),
    CONSTRAINT ck_requisition_priority CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    CONSTRAINT ck_requisition_total CHECK (total_amount >= 0)
);

COMMENT ON TABLE purchase_requisition IS '購買依頼 - 部門からの購入要求';
COMMENT ON COLUMN purchase_requisition.requisition_number IS '購買依頼番号（自動採番、例: PR-2025-000001）';
COMMENT ON COLUMN purchase_requisition.status IS '状態: DRAFT=下書き, SUBMITTED=提出済, APPROVED=承認済等';
COMMENT ON COLUMN purchase_requisition.justification IS '購入理由・妥当性の説明';

-- ----------------------------------------------------------------------------
-- 購買依頼明細テーブル
-- 購買依頼に含まれる個々の商品とその数量・金額を定義する。
-- ----------------------------------------------------------------------------
CREATE TABLE purchase_requisition_item (
    id              BIGSERIAL       PRIMARY KEY,
    requisition_id  BIGINT          NOT NULL REFERENCES purchase_requisition(id) ON DELETE CASCADE,
    product_id      BIGINT          NOT NULL REFERENCES product(id) ON DELETE RESTRICT,
    quantity        INTEGER         NOT NULL,
    unit_price      NUMERIC(15, 2) NOT NULL,
    total_price     NUMERIC(15, 2) NOT NULL,
    preferred_supplier_id BIGINT    REFERENCES supplier(id) ON DELETE SET NULL,
    notes           TEXT,
    sort_order      INTEGER         NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT ck_req_item_quantity CHECK (quantity >= 1),
    CONSTRAINT ck_req_item_unit_price CHECK (unit_price >= 0),
    CONSTRAINT ck_req_item_total_price CHECK (total_price >= 0)
);

COMMENT ON TABLE purchase_requisition_item IS '購買依頼明細 - 依頼対象の商品・数量・金額';
COMMENT ON COLUMN purchase_requisition_item.preferred_supplier_id IS '希望仕入先（依頼者が推薦する仕入先）';

-- ----------------------------------------------------------------------------
-- 発注テーブル
-- 仕入先への発注書（Purchase Order）を管理する。
-- 発注のライフサイクル全体をstatusで追跡する。
-- ----------------------------------------------------------------------------
CREATE TABLE purchase_order (
    id              BIGSERIAL       PRIMARY KEY,
    order_number    VARCHAR(30)     NOT NULL,
    requisition_id  BIGINT          REFERENCES purchase_requisition(id) ON DELETE SET NULL,
    supplier_id     BIGINT          NOT NULL REFERENCES supplier(id) ON DELETE RESTRICT,
    ordered_by      BIGINT          NOT NULL REFERENCES user_profile(id) ON DELETE RESTRICT,
    approved_by     BIGINT          REFERENCES user_profile(id) ON DELETE SET NULL,
    department_id   BIGINT          NOT NULL REFERENCES department(id) ON DELETE RESTRICT,
    status          VARCHAR(30)     NOT NULL DEFAULT 'DRAFT',
    priority        VARCHAR(10)     NOT NULL DEFAULT 'NORMAL',
    order_date      DATE,
    expected_delivery_date DATE,
    actual_delivery_date DATE,
    subtotal        NUMERIC(15, 2) NOT NULL DEFAULT 0,
    tax_amount      NUMERIC(15, 2) NOT NULL DEFAULT 0,
    shipping_cost   NUMERIC(15, 2) NOT NULL DEFAULT 0,
    discount_amount NUMERIC(15, 2) NOT NULL DEFAULT 0,
    total_amount    NUMERIC(15, 2) NOT NULL DEFAULT 0,
    currency_code   VARCHAR(3)      NOT NULL DEFAULT 'JPY',
    payment_terms   VARCHAR(50)     NOT NULL DEFAULT 'NET30',
    shipping_method VARCHAR(50),
    shipping_address_id BIGINT      REFERENCES supplier_address(id) ON DELETE SET NULL,
    billing_address TEXT,
    notes           TEXT,
    internal_memo   TEXT,
    contract_id     BIGINT          REFERENCES supplier_contract(id) ON DELETE SET NULL,
    submitted_at    TIMESTAMP,
    approved_at     TIMESTAMP,
    cancelled_at    TIMESTAMP,
    cancelled_reason TEXT,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT uq_order_number UNIQUE (order_number),
    CONSTRAINT ck_po_status CHECK (status IN (
        'DRAFT', 'SUBMITTED', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED',
        'ORDERED', 'ACKNOWLEDGED', 'PARTIALLY_RECEIVED', 'RECEIVED',
        'INVOICED', 'PARTIALLY_PAID', 'PAID', 'CANCELLED', 'CLOSED'
    )),
    CONSTRAINT ck_po_priority CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    CONSTRAINT ck_po_subtotal CHECK (subtotal >= 0),
    CONSTRAINT ck_po_tax CHECK (tax_amount >= 0),
    CONSTRAINT ck_po_shipping CHECK (shipping_cost >= 0),
    CONSTRAINT ck_po_discount CHECK (discount_amount >= 0),
    CONSTRAINT ck_po_total CHECK (total_amount >= 0)
);

COMMENT ON TABLE purchase_order IS '発注書 - 仕入先への購買発注';
COMMENT ON COLUMN purchase_order.order_number IS '発注番号（自動採番、例: PO-2025-000001）';
COMMENT ON COLUMN purchase_order.status IS '発注状態のライフサイクル: DRAFT→SUBMITTED→APPROVED→ORDERED→RECEIVED→PAID→CLOSED';
COMMENT ON COLUMN purchase_order.internal_memo IS '社内メモ（仕入先には送付しない内部情報）';

-- ----------------------------------------------------------------------------
-- 発注明細テーブル
-- 発注書に含まれる個々の商品と数量・金額を定義する。
-- 受入数量を追跡し、部分納品に対応する。
-- ----------------------------------------------------------------------------
CREATE TABLE purchase_order_item (
    id              BIGSERIAL       PRIMARY KEY,
    order_id        BIGINT          NOT NULL REFERENCES purchase_order(id) ON DELETE CASCADE,
    product_id      BIGINT          NOT NULL REFERENCES product(id) ON DELETE RESTRICT,
    requisition_item_id BIGINT      REFERENCES purchase_requisition_item(id) ON DELETE SET NULL,
    quantity_ordered INTEGER        NOT NULL,
    quantity_received INTEGER       NOT NULL DEFAULT 0,
    quantity_accepted INTEGER       NOT NULL DEFAULT 0,
    quantity_rejected INTEGER       NOT NULL DEFAULT 0,
    unit_price      NUMERIC(15, 2) NOT NULL,
    discount_pct    NUMERIC(5, 2)   NOT NULL DEFAULT 0,
    tax_rate_pct    NUMERIC(5, 2)   NOT NULL DEFAULT 0,
    total_price     NUMERIC(15, 2) NOT NULL,
    delivery_date   DATE,
    notes           TEXT,
    sort_order      INTEGER         NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT ck_poi_quantity_ordered CHECK (quantity_ordered >= 1),
    CONSTRAINT ck_poi_quantity_received CHECK (quantity_received >= 0),
    CONSTRAINT ck_poi_quantity_accepted CHECK (quantity_accepted >= 0),
    CONSTRAINT ck_poi_quantity_rejected CHECK (quantity_rejected >= 0),
    CONSTRAINT ck_poi_received_vs_ordered CHECK (quantity_received <= quantity_ordered),
    CONSTRAINT ck_poi_accepted_rejected CHECK (quantity_accepted + quantity_rejected <= quantity_received),
    CONSTRAINT ck_poi_unit_price CHECK (unit_price >= 0),
    CONSTRAINT ck_poi_discount CHECK (discount_pct >= 0 AND discount_pct <= 100),
    CONSTRAINT ck_poi_tax_rate CHECK (tax_rate_pct >= 0 AND tax_rate_pct <= 100),
    CONSTRAINT ck_poi_total_price CHECK (total_price >= 0)
);

COMMENT ON TABLE purchase_order_item IS '発注明細 - 発注対象の商品・数量・金額';
COMMENT ON COLUMN purchase_order_item.quantity_ordered IS '発注数量';
COMMENT ON COLUMN purchase_order_item.quantity_received IS '受入数量（検収時に更新）';
COMMENT ON COLUMN purchase_order_item.quantity_accepted IS '合格数量（検品合格分）';
COMMENT ON COLUMN purchase_order_item.quantity_rejected IS '不合格数量（検品不合格分）';

-- ----------------------------------------------------------------------------
-- 承認ワークフローテーブル
-- ポリモーフィックな承認フロー管理。entity_type + entity_id で対象エンティティを
-- 汎用的に参照する。購買依頼・発注書等の承認に共通で使用する。
-- ----------------------------------------------------------------------------
CREATE TABLE approval_workflow (
    id              BIGSERIAL       PRIMARY KEY,
    entity_type     VARCHAR(50)     NOT NULL,
    entity_id       BIGINT          NOT NULL,
    workflow_name   VARCHAR(200)    NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    initiated_by    BIGINT          NOT NULL REFERENCES user_profile(id) ON DELETE RESTRICT,
    current_step    INTEGER         NOT NULL DEFAULT 1,
    total_steps     INTEGER         NOT NULL DEFAULT 1,
    initiated_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at    TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT uq_workflow_entity UNIQUE (entity_type, entity_id),
    CONSTRAINT ck_workflow_entity_type CHECK (entity_type IN (
        'PURCHASE_REQUISITION', 'PURCHASE_ORDER', 'SUPPLIER_CONTRACT',
        'BUDGET', 'RETURN_TO_SUPPLIER'
    )),
    CONSTRAINT ck_workflow_status CHECK (status IN (
        'PENDING', 'IN_PROGRESS', 'APPROVED', 'REJECTED', 'CANCELLED', 'ESCALATED'
    )),
    CONSTRAINT ck_workflow_steps CHECK (current_step >= 1 AND total_steps >= 1 AND current_step <= total_steps + 1)
);

COMMENT ON TABLE approval_workflow IS '承認ワークフロー - ポリモーフィック承認フロー管理';
COMMENT ON COLUMN approval_workflow.entity_type IS '対象エンティティ種別（ポリモーフィック参照）';
COMMENT ON COLUMN approval_workflow.entity_id IS '対象エンティティID';
COMMENT ON COLUMN approval_workflow.current_step IS '現在のステップ番号（1始まり）';

-- ----------------------------------------------------------------------------
-- 承認ステップテーブル
-- 承認ワークフロー内の各承認ステップの詳細と結果を記録する。
-- ----------------------------------------------------------------------------
CREATE TABLE approval_step (
    id              BIGSERIAL       PRIMARY KEY,
    workflow_id     BIGINT          NOT NULL REFERENCES approval_workflow(id) ON DELETE CASCADE,
    step_number     INTEGER         NOT NULL,
    approver_id     BIGINT          NOT NULL REFERENCES user_profile(id) ON DELETE RESTRICT,
    delegate_id     BIGINT          REFERENCES user_profile(id) ON DELETE SET NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    decision        VARCHAR(20),
    comments        TEXT,
    required_by     TIMESTAMP,
    decided_at      TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT uq_approval_step UNIQUE (workflow_id, step_number),
    CONSTRAINT ck_step_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'SKIPPED')),
    CONSTRAINT ck_step_decision CHECK (decision IS NULL OR decision IN (
        'APPROVED', 'REJECTED', 'RETURNED', 'ESCALATED'
    )),
    CONSTRAINT ck_step_number CHECK (step_number >= 1)
);

COMMENT ON TABLE approval_step IS '承認ステップ - ワークフロー内の各承認者の判断結果';
COMMENT ON COLUMN approval_step.delegate_id IS '代理承認者（委任ルールに基づく代理の場合）';
COMMENT ON COLUMN approval_step.decision IS '判断結果: APPROVED=承認, REJECTED=却下, RETURNED=差戻し';

-- ----------------------------------------------------------------------------
-- 検収テーブル
-- 発注した商品の受入検収（Goods Receipt）を管理する。
-- 1つの発注に対して複数回の検収（部分納品）に対応する。
-- ----------------------------------------------------------------------------
CREATE TABLE goods_receipt (
    id              BIGSERIAL       PRIMARY KEY,
    receipt_number  VARCHAR(30)     NOT NULL,
    order_id        BIGINT          NOT NULL REFERENCES purchase_order(id) ON DELETE RESTRICT,
    received_by     BIGINT          NOT NULL REFERENCES user_profile(id) ON DELETE RESTRICT,
    warehouse_id    BIGINT,         -- FK追加はV005のwarehouseテーブル作成後
    receipt_date    DATE            NOT NULL DEFAULT CURRENT_DATE,
    status          VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    delivery_note_number VARCHAR(50),
    carrier         VARCHAR(200),
    tracking_number VARCHAR(100),
    notes           TEXT,
    inspected_by    BIGINT          REFERENCES user_profile(id) ON DELETE SET NULL,
    inspected_at    TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT uq_receipt_number UNIQUE (receipt_number),
    CONSTRAINT ck_receipt_status CHECK (status IN (
        'DRAFT', 'RECEIVED', 'INSPECTING', 'ACCEPTED', 'PARTIALLY_ACCEPTED', 'REJECTED', 'CANCELLED'
    ))
);

COMMENT ON TABLE goods_receipt IS '検収 - 発注商品の受入検収記録';
COMMENT ON COLUMN goods_receipt.receipt_number IS '検収番号（自動採番、例: GR-2025-000001）';
COMMENT ON COLUMN goods_receipt.delivery_note_number IS '仕入先の納品書番号';

-- ----------------------------------------------------------------------------
-- 検収明細テーブル
-- 検収における個々の商品の受入数量・合否を記録する。
-- ----------------------------------------------------------------------------
CREATE TABLE goods_receipt_item (
    id              BIGSERIAL       PRIMARY KEY,
    receipt_id      BIGINT          NOT NULL REFERENCES goods_receipt(id) ON DELETE CASCADE,
    order_item_id   BIGINT          NOT NULL REFERENCES purchase_order_item(id) ON DELETE RESTRICT,
    product_id      BIGINT          NOT NULL REFERENCES product(id) ON DELETE RESTRICT,
    quantity_received INTEGER       NOT NULL,
    quantity_accepted INTEGER       NOT NULL DEFAULT 0,
    quantity_rejected INTEGER       NOT NULL DEFAULT 0,
    rejection_reason TEXT,
    storage_location_id BIGINT,     -- FK追加はV005のstorage_locationテーブル作成後
    lot_number      VARCHAR(50),
    serial_numbers  TEXT[],
    expiry_date     DATE,
    notes           TEXT,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT ck_gri_received CHECK (quantity_received >= 1),
    CONSTRAINT ck_gri_accepted CHECK (quantity_accepted >= 0),
    CONSTRAINT ck_gri_rejected CHECK (quantity_rejected >= 0),
    CONSTRAINT ck_gri_accepted_rejected CHECK (quantity_accepted + quantity_rejected <= quantity_received)
);

COMMENT ON TABLE goods_receipt_item IS '検収明細 - 商品ごとの受入数量と合否判定';
COMMENT ON COLUMN goods_receipt_item.serial_numbers IS 'シリアル番号リスト（シリアル管理品の場合）';
COMMENT ON COLUMN goods_receipt_item.lot_number IS 'ロット番号（ロット管理品の場合）';

-- ----------------------------------------------------------------------------
-- 発注状態履歴テーブル
-- 発注書の状態遷移を時系列で記録する。監査証跡として利用する。
-- ----------------------------------------------------------------------------
CREATE TABLE purchase_order_status_history (
    id              BIGSERIAL       PRIMARY KEY,
    order_id        BIGINT          NOT NULL REFERENCES purchase_order(id) ON DELETE CASCADE,
    from_status     VARCHAR(30),
    to_status       VARCHAR(30)     NOT NULL,
    changed_by      BIGINT          NOT NULL REFERENCES user_profile(id) ON DELETE RESTRICT,
    change_reason   TEXT,
    changed_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0
);

COMMENT ON TABLE purchase_order_status_history IS '発注状態履歴 - 発注の状態遷移ログ（監査証跡）';
COMMENT ON COLUMN purchase_order_status_history.from_status IS '変更前の状態（初回作成時はNULL）';

-- ----------------------------------------------------------------------------
-- 仕入先返品テーブル
-- 不良品等の仕入先への返品処理を管理する。
-- ----------------------------------------------------------------------------
CREATE TABLE return_to_supplier (
    id              BIGSERIAL       PRIMARY KEY,
    return_number   VARCHAR(30)     NOT NULL,
    order_id        BIGINT          NOT NULL REFERENCES purchase_order(id) ON DELETE RESTRICT,
    supplier_id     BIGINT          NOT NULL REFERENCES supplier(id) ON DELETE RESTRICT,
    initiated_by    BIGINT          NOT NULL REFERENCES user_profile(id) ON DELETE RESTRICT,
    status          VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    return_reason   VARCHAR(50)     NOT NULL,
    description     TEXT,
    total_amount    NUMERIC(15, 2) NOT NULL DEFAULT 0,
    currency_code   VARCHAR(3)      NOT NULL DEFAULT 'JPY',
    return_date     DATE,
    credit_note_number VARCHAR(50),
    credit_note_amount NUMERIC(15, 2),
    shipping_method VARCHAR(50),
    tracking_number VARCHAR(100),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT uq_return_number UNIQUE (return_number),
    CONSTRAINT ck_return_status CHECK (status IN (
        'DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'SHIPPED', 'RECEIVED_BY_SUPPLIER',
        'CREDIT_ISSUED', 'CLOSED', 'CANCELLED'
    )),
    CONSTRAINT ck_return_reason CHECK (return_reason IN (
        'DEFECTIVE', 'WRONG_ITEM', 'DAMAGED', 'EXCESS_QUANTITY', 'QUALITY_ISSUE',
        'EXPIRED', 'NOT_AS_DESCRIBED', 'OTHER'
    )),
    CONSTRAINT ck_return_total CHECK (total_amount >= 0),
    CONSTRAINT ck_return_credit CHECK (credit_note_amount IS NULL OR credit_note_amount >= 0)
);

COMMENT ON TABLE return_to_supplier IS '仕入先返品 - 不良品等の返品処理';
COMMENT ON COLUMN return_to_supplier.return_reason IS '返品理由: DEFECTIVE=不良品, WRONG_ITEM=誤送品, DAMAGED=破損等';
COMMENT ON COLUMN return_to_supplier.credit_note_number IS '仕入先発行のクレジットノート番号';
