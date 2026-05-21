-- ============================================================================
-- ProQuip - Enterprise Procurement & Inventory Management System
-- V010: ビューの作成
-- 在庫サマリ、発注サマリ、仕入先パフォーマンス、低在庫アラート
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 在庫サマリビュー
-- 商品ごとに全倉庫の在庫を集約し、在庫状況の全体像を提供する。
-- 手持在庫・引当済・発注中・利用可能在庫を一覧で表示する。
-- ----------------------------------------------------------------------------
CREATE OR REPLACE VIEW v_inventory_summary AS
SELECT
    p.id                    AS product_id,
    p.sku,
    p.name                  AS product_name,
    c.name                  AS category_name,
    m.name                  AS manufacturer_name,
    uom.name                AS unit_name,
    p.unit_price,
    p.reorder_point,
    p.reorder_qty,
    p.lead_time_days,
    p.status                AS product_status,
    COUNT(DISTINCT ii.warehouse_id)     AS warehouse_count,
    COALESCE(SUM(ii.quantity_on_hand), 0)       AS total_on_hand,
    COALESCE(SUM(ii.quantity_reserved), 0)      AS total_reserved,
    COALESCE(SUM(ii.quantity_on_order), 0)      AS total_on_order,
    COALESCE(SUM(ii.quantity_in_transit), 0)    AS total_in_transit,
    COALESCE(SUM(ii.quantity_on_hand), 0)
        - COALESCE(SUM(ii.quantity_reserved), 0)    AS total_available,
    COALESCE(SUM(ii.total_value), 0)            AS total_inventory_value,
    CASE
        WHEN COALESCE(SUM(ii.quantity_on_hand), 0) = 0 THEN 'OUT_OF_STOCK'
        WHEN COALESCE(SUM(ii.quantity_on_hand), 0) <= p.reorder_point THEN 'LOW_STOCK'
        WHEN COALESCE(SUM(ii.quantity_on_hand), 0) <= p.reorder_point * 2 THEN 'ADEQUATE'
        ELSE 'SUFFICIENT'
    END                     AS stock_status
FROM
    product p
    LEFT JOIN category c ON p.category_id = c.id
    LEFT JOIN manufacturer m ON p.manufacturer_id = m.id
    LEFT JOIN unit_of_measure uom ON p.unit_id = uom.id
    LEFT JOIN inventory_item ii ON p.id = ii.product_id
WHERE
    p.status = 'ACTIVE'
GROUP BY
    p.id, p.sku, p.name, c.name, m.name, uom.name,
    p.unit_price, p.reorder_point, p.reorder_qty, p.lead_time_days, p.status;

COMMENT ON VIEW v_inventory_summary IS '在庫サマリ - 商品ごとの全倉庫合計在庫状況（商品マスタ×在庫の集約）';

-- ----------------------------------------------------------------------------
-- 発注サマリビュー
-- 発注書の概要情報を明細の集計値とともに提供する。
-- 発注一覧画面や管理ダッシュボードでの利用を想定する。
-- ----------------------------------------------------------------------------
CREATE OR REPLACE VIEW v_purchase_order_summary AS
SELECT
    po.id                   AS order_id,
    po.order_number,
    po.status,
    po.priority,
    po.order_date,
    po.expected_delivery_date,
    po.actual_delivery_date,
    s.id                    AS supplier_id,
    s.supplier_code,
    s.name                  AS supplier_name,
    d.id                    AS department_id,
    d.name                  AS department_name,
    orderer.employee_number AS ordered_by_employee,
    orderer.last_name || ' ' || orderer.first_name AS ordered_by_name,
    approver.last_name || ' ' || approver.first_name AS approved_by_name,
    po.subtotal,
    po.tax_amount,
    po.shipping_cost,
    po.discount_amount,
    po.total_amount,
    po.currency_code,
    po.payment_terms,
    COUNT(poi.id)           AS item_count,
    COALESCE(SUM(poi.quantity_ordered), 0)  AS total_qty_ordered,
    COALESCE(SUM(poi.quantity_received), 0) AS total_qty_received,
    COALESCE(SUM(poi.quantity_accepted), 0) AS total_qty_accepted,
    COALESCE(SUM(poi.quantity_rejected), 0) AS total_qty_rejected,
    CASE
        WHEN COALESCE(SUM(poi.quantity_ordered), 0) = 0 THEN 0
        ELSE ROUND(
            COALESCE(SUM(poi.quantity_received), 0)::NUMERIC
            / SUM(poi.quantity_ordered) * 100, 1
        )
    END                     AS fulfillment_pct,
    CASE
        WHEN po.status IN ('CANCELLED', 'CLOSED', 'PAID') THEN FALSE
        WHEN po.expected_delivery_date < CURRENT_DATE
             AND po.status IN ('ORDERED', 'ACKNOWLEDGED', 'PARTIALLY_RECEIVED') THEN TRUE
        ELSE FALSE
    END                     AS is_overdue,
    CASE
        WHEN po.expected_delivery_date IS NOT NULL THEN
            po.expected_delivery_date - CURRENT_DATE
        ELSE NULL
    END                     AS days_until_delivery,
    po.created_at,
    po.updated_at
FROM
    purchase_order po
    INNER JOIN supplier s ON po.supplier_id = s.id
    INNER JOIN department d ON po.department_id = d.id
    INNER JOIN user_profile orderer ON po.ordered_by = orderer.id
    LEFT JOIN user_profile approver ON po.approved_by = approver.id
    LEFT JOIN purchase_order_item poi ON po.id = poi.order_id
GROUP BY
    po.id, po.order_number, po.status, po.priority,
    po.order_date, po.expected_delivery_date, po.actual_delivery_date,
    s.id, s.supplier_code, s.name,
    d.id, d.name,
    orderer.employee_number, orderer.last_name, orderer.first_name,
    approver.last_name, approver.first_name,
    po.subtotal, po.tax_amount, po.shipping_cost, po.discount_amount,
    po.total_amount, po.currency_code, po.payment_terms,
    po.created_at, po.updated_at;

COMMENT ON VIEW v_purchase_order_summary IS '発注サマリ - 発注書の概要と明細集計値（一覧表示・ダッシュボード用）';

-- ----------------------------------------------------------------------------
-- 仕入先パフォーマンスビュー
-- 仕入先ごとの評価スコア集計、取引実績、契約状況を統合した分析ビュー。
-- 仕入先の総合的なパフォーマンス評価に使用する。
-- ----------------------------------------------------------------------------
CREATE OR REPLACE VIEW v_supplier_performance AS
SELECT
    s.id                    AS supplier_id,
    s.supplier_code,
    s.name                  AS supplier_name,
    s.status                AS supplier_status,
    s.risk_level,
    s.payment_terms,
    s.registered_at,
    -- 評価スコア（全期間平均）
    ROUND(AVG(sr.quality_score), 2)     AS avg_quality_score,
    ROUND(AVG(sr.delivery_score), 2)    AS avg_delivery_score,
    ROUND(AVG(sr.price_score), 2)       AS avg_price_score,
    ROUND(AVG(sr.service_score), 2)     AS avg_service_score,
    ROUND(AVG(sr.overall_score), 2)     AS avg_overall_score,
    COUNT(DISTINCT sr.id)               AS rating_count,
    MAX(sr.rated_at)                    AS last_rated_at,
    -- 発注実績
    COUNT(DISTINCT po.id)               AS total_orders,
    COUNT(DISTINCT po.id) FILTER (WHERE po.status = 'PAID')     AS completed_orders,
    COUNT(DISTINCT po.id) FILTER (WHERE po.status = 'CANCELLED') AS cancelled_orders,
    COALESCE(SUM(po.total_amount) FILTER (WHERE po.status NOT IN ('DRAFT', 'CANCELLED')), 0)
                                        AS total_order_value,
    -- 納期遵守率
    COUNT(DISTINCT po.id) FILTER (
        WHERE po.actual_delivery_date IS NOT NULL
          AND po.actual_delivery_date <= po.expected_delivery_date
    )                                   AS on_time_deliveries,
    CASE
        WHEN COUNT(DISTINCT po.id) FILTER (WHERE po.actual_delivery_date IS NOT NULL) = 0 THEN NULL
        ELSE ROUND(
            COUNT(DISTINCT po.id) FILTER (
                WHERE po.actual_delivery_date IS NOT NULL
                  AND po.actual_delivery_date <= po.expected_delivery_date
            )::NUMERIC
            / COUNT(DISTINCT po.id) FILTER (WHERE po.actual_delivery_date IS NOT NULL) * 100, 1
        )
    END                                 AS on_time_delivery_pct,
    -- 取扱商品数
    COUNT(DISTINCT sp.product_id)       AS active_product_count,
    -- 有効契約数
    COUNT(DISTINCT sc.id) FILTER (WHERE sc.status = 'ACTIVE') AS active_contract_count,
    -- 有効認証数
    COUNT(DISTINCT cert.id) FILTER (WHERE cert.status = 'VALID') AS valid_certification_count
FROM
    supplier s
    LEFT JOIN supplier_rating sr ON s.id = sr.supplier_id
    LEFT JOIN purchase_order po ON s.id = po.supplier_id
    LEFT JOIN supplier_product sp ON s.id = sp.supplier_id AND sp.is_active = TRUE
    LEFT JOIN supplier_contract sc ON s.id = sc.supplier_id
    LEFT JOIN supplier_certification cert ON s.id = cert.supplier_id
GROUP BY
    s.id, s.supplier_code, s.name, s.status,
    s.risk_level, s.payment_terms, s.registered_at;

COMMENT ON VIEW v_supplier_performance IS '仕入先パフォーマンス - 評価・取引実績・契約状況の統合分析ビュー';

-- ----------------------------------------------------------------------------
-- 低在庫アラートビュー
-- 発注点を下回った商品を検出し、補充が必要な在庫のアラート一覧を提供する。
-- 在庫管理ダッシュボードのアラートウィジェットで使用する。
-- ----------------------------------------------------------------------------
CREATE OR REPLACE VIEW v_low_stock_alerts AS
SELECT
    p.id                    AS product_id,
    p.sku,
    p.name                  AS product_name,
    c.name                  AS category_name,
    m.name                  AS manufacturer_name,
    w.id                    AS warehouse_id,
    w.warehouse_code,
    w.name                  AS warehouse_name,
    ii.quantity_on_hand,
    ii.quantity_reserved,
    ii.quantity_on_hand - ii.quantity_reserved   AS available_quantity,
    ii.quantity_on_order,
    p.reorder_point,
    p.reorder_qty,
    p.lead_time_days,
    p.unit_price,
    -- 不足数量の算出
    GREATEST(p.reorder_point - (ii.quantity_on_hand - ii.quantity_reserved), 0) AS shortage_quantity,
    -- 推奨発注数量（不足分と発注ロットの整合）
    CASE
        WHEN p.reorder_qty > 0 THEN
            CEIL(
                GREATEST(p.reorder_point - (ii.quantity_on_hand - ii.quantity_reserved), 0)::NUMERIC
                / p.reorder_qty
            ) * p.reorder_qty
        ELSE
            GREATEST(p.reorder_point - (ii.quantity_on_hand - ii.quantity_reserved), 0)
    END                     AS suggested_order_qty,
    -- アラートレベルの判定
    CASE
        WHEN ii.quantity_on_hand = 0 THEN 'CRITICAL'
        WHEN ii.quantity_on_hand - ii.quantity_reserved <= 0 THEN 'CRITICAL'
        WHEN ii.quantity_on_hand - ii.quantity_reserved <= p.reorder_point * 0.5 THEN 'HIGH'
        ELSE 'MEDIUM'
    END                     AS alert_level,
    -- 既に発注中かどうか
    CASE
        WHEN ii.quantity_on_order > 0 THEN TRUE
        ELSE FALSE
    END                     AS has_pending_order,
    -- 優先仕入先情報
    preferred_sp.supplier_id AS preferred_supplier_id,
    preferred_s.name        AS preferred_supplier_name,
    preferred_sp.unit_price AS preferred_supplier_price,
    preferred_sp.lead_time_days AS preferred_supplier_lead_time,
    ii.last_movement_at
FROM
    inventory_item ii
    INNER JOIN product p ON ii.product_id = p.id
    INNER JOIN warehouse w ON ii.warehouse_id = w.id
    LEFT JOIN category c ON p.category_id = c.id
    LEFT JOIN manufacturer m ON p.manufacturer_id = m.id
    LEFT JOIN LATERAL (
        SELECT sp.supplier_id, sp.unit_price, sp.lead_time_days
        FROM supplier_product sp
        WHERE sp.product_id = p.id AND sp.is_preferred = TRUE AND sp.is_active = TRUE
        LIMIT 1
    ) preferred_sp ON TRUE
    LEFT JOIN supplier preferred_s ON preferred_sp.supplier_id = preferred_s.id
WHERE
    p.status = 'ACTIVE'
    AND w.is_active = TRUE
    AND (ii.quantity_on_hand - ii.quantity_reserved) <= p.reorder_point
    AND p.reorder_point > 0;

COMMENT ON VIEW v_low_stock_alerts IS '低在庫アラート - 発注点を下回った商品の補充アラート一覧';
