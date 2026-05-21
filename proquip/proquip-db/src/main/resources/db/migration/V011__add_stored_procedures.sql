-- ============================================================================
-- ProQuip - Enterprise Procurement & Inventory Management System
-- V011: ストアドプロシージャの作成
--
-- ※※※ 技術的負債（テックデット）の注意事項 ※※※
-- 以下のストアドプロシージャはビジネスロジックをデータベース層に含んでいる。
-- これは意図的な技術的負債であり、本来はアプリケーション層（サービス層）で
-- 実装すべきロジックである。
--
-- 問題点:
--   1. ビジネスロジックがアプリケーションコードとDB間で分散し、保守性が低下する
--   2. ユニットテストが困難になる
--   3. DBベンダーへのロックインが発生する
--   4. バージョン管理・コードレビューのワークフローに乗りにくい
--   5. デバッグが困難（アプリケーション側のデバッガが使えない）
--
-- 将来的にはアプリケーション層のサービスクラスに移行すべきである。
-- 移行チケット: PROQUIP-xxxx（バックログに登録予定）
-- ============================================================================

-- ----------------------------------------------------------------------------
-- sp_calculate_reorder_suggestions: 発注提案計算プロシージャ
--
-- 【技術的負債】このプロシージャはビジネスロジック（発注点管理・発注量計算）を
-- DB層に実装している。本来は ReorderSuggestionService 等のアプリケーション
-- サービスで実装し、テスタビリティとメンテナンス性を確保すべきである。
--
-- 目的:
--   在庫が発注点を下回った商品を検出し、推奨発注情報を算出する。
--   リードタイムと現在の発注残を考慮した発注量を提案する。
--
-- パラメータ:
--   p_warehouse_id - 対象倉庫ID（NULLの場合は全倉庫）
--   p_category_id  - 対象カテゴリID（NULLの場合は全カテゴリ）
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION sp_calculate_reorder_suggestions(
    p_warehouse_id  BIGINT DEFAULT NULL,
    p_category_id   BIGINT DEFAULT NULL
)
RETURNS TABLE (
    product_id              BIGINT,
    product_sku             VARCHAR(50),
    product_name            VARCHAR(300),
    category_name           VARCHAR(200),
    warehouse_id            BIGINT,
    warehouse_name          VARCHAR(200),
    current_on_hand         INTEGER,
    current_reserved        INTEGER,
    available_quantity      INTEGER,
    current_on_order        INTEGER,
    reorder_point           INTEGER,
    reorder_quantity        INTEGER,
    lead_time_days          INTEGER,
    shortage_quantity       INTEGER,
    suggested_order_qty     INTEGER,
    estimated_cost          NUMERIC(15, 2),
    preferred_supplier_id   BIGINT,
    preferred_supplier_name VARCHAR(300),
    preferred_unit_price    NUMERIC(15, 2),
    priority_level          VARCHAR(10),
    expected_stockout_date  DATE
) AS $$
-- ※ 技術的負債: このロジックはアプリケーション層に移行すべき
-- TODO: ReorderSuggestionService に移行する (PROQUIP-xxxx)
DECLARE
    v_avg_daily_usage NUMERIC(10, 2);
BEGIN
    RETURN QUERY
    WITH daily_usage AS (
        -- 過去90日間の平均日次使用量を算出
        -- ※ 技術的負債: この集計ロジックもアプリ層で行うべき
        SELECT
            it.product_id AS du_product_id,
            it.warehouse_id AS du_warehouse_id,
            CASE
                WHEN COUNT(*) FILTER (WHERE it.transaction_type IN ('ISSUE', 'TRANSFER_OUT')) = 0 THEN 0
                ELSE ROUND(
                    ABS(SUM(it.quantity) FILTER (WHERE it.transaction_type IN ('ISSUE', 'TRANSFER_OUT')))::NUMERIC
                    / GREATEST(
                        EXTRACT(DAY FROM (CURRENT_TIMESTAMP - MIN(it.transaction_date))),
                        1
                    ),
                    2
                )
            END AS avg_daily_qty
        FROM inventory_transaction it
        WHERE it.transaction_date >= CURRENT_TIMESTAMP - INTERVAL '90 days'
        GROUP BY it.product_id, it.warehouse_id
    ),
    preferred_suppliers AS (
        -- 優先仕入先の情報を取得
        SELECT DISTINCT ON (sp.product_id)
            sp.product_id AS ps_product_id,
            sp.supplier_id,
            s.name AS supplier_name,
            sp.unit_price,
            sp.lead_time_days AS supplier_lead_time
        FROM supplier_product sp
        INNER JOIN supplier s ON sp.supplier_id = s.id AND s.status = 'ACTIVE'
        WHERE sp.is_preferred = TRUE AND sp.is_active = TRUE
        ORDER BY sp.product_id, sp.unit_price ASC
    )
    SELECT
        p.id,
        p.sku,
        p.name,
        c.name,
        w.id,
        w.name,
        ii.quantity_on_hand,
        ii.quantity_reserved,
        ii.quantity_on_hand - ii.quantity_reserved,
        ii.quantity_on_order,
        p.reorder_point,
        p.reorder_qty,
        p.lead_time_days,
        -- 不足数量
        GREATEST(
            p.reorder_point - (ii.quantity_on_hand - ii.quantity_reserved) + COALESCE(ii.quantity_on_order, 0),
            0
        )::INTEGER,
        -- 推奨発注数量（発注ロット単位に切り上げ、発注残を考慮）
        CASE
            WHEN (ii.quantity_on_hand - ii.quantity_reserved + ii.quantity_on_order) >= p.reorder_point
                THEN 0
            WHEN p.reorder_qty > 0 THEN
                (CEIL(
                    GREATEST(
                        p.reorder_point - (ii.quantity_on_hand - ii.quantity_reserved - ii.quantity_on_order),
                        p.reorder_qty
                    )::NUMERIC / p.reorder_qty
                ) * p.reorder_qty)::INTEGER
            ELSE
                GREATEST(
                    p.reorder_point - (ii.quantity_on_hand - ii.quantity_reserved - ii.quantity_on_order),
                    1
                )::INTEGER
        END,
        -- 予想コスト
        CASE
            WHEN ps.unit_price IS NOT NULL THEN
                ps.unit_price * CASE
                    WHEN p.reorder_qty > 0 THEN
                        CEIL(
                            GREATEST(
                                p.reorder_point - (ii.quantity_on_hand - ii.quantity_reserved - ii.quantity_on_order),
                                p.reorder_qty
                            )::NUMERIC / p.reorder_qty
                        ) * p.reorder_qty
                    ELSE
                        GREATEST(
                            p.reorder_point - (ii.quantity_on_hand - ii.quantity_reserved - ii.quantity_on_order),
                            1
                        )
                END
            ELSE
                p.unit_price * p.reorder_qty
        END,
        ps.supplier_id,
        ps.supplier_name,
        ps.unit_price,
        -- 優先度の判定
        -- ※ 技術的負債: 優先度のビジネスルールはアプリ層で管理すべき
        CASE
            WHEN ii.quantity_on_hand = 0 THEN 'CRITICAL'::VARCHAR(10)
            WHEN (ii.quantity_on_hand - ii.quantity_reserved) <= 0 THEN 'CRITICAL'::VARCHAR(10)
            WHEN (ii.quantity_on_hand - ii.quantity_reserved) <= p.reorder_point * 0.3 THEN 'HIGH'::VARCHAR(10)
            WHEN (ii.quantity_on_hand - ii.quantity_reserved) <= p.reorder_point * 0.7 THEN 'MEDIUM'::VARCHAR(10)
            ELSE 'LOW'::VARCHAR(10)
        END,
        -- 在庫切れ予想日
        -- ※ 技術的負債: 需要予測ロジックはアプリ層で実装すべき
        CASE
            WHEN COALESCE(du.avg_daily_qty, 0) > 0 THEN
                CURRENT_DATE + (
                    (ii.quantity_on_hand - ii.quantity_reserved)::NUMERIC / du.avg_daily_qty
                )::INTEGER
            ELSE NULL
        END
    FROM
        inventory_item ii
        INNER JOIN product p ON ii.product_id = p.id
        INNER JOIN warehouse w ON ii.warehouse_id = w.id
        LEFT JOIN category c ON p.category_id = c.id
        LEFT JOIN daily_usage du ON p.id = du.du_product_id AND w.id = du.du_warehouse_id
        LEFT JOIN preferred_suppliers ps ON p.id = ps.ps_product_id
    WHERE
        p.status = 'ACTIVE'
        AND w.is_active = TRUE
        AND p.reorder_point > 0
        AND (ii.quantity_on_hand - ii.quantity_reserved) <= p.reorder_point
        AND (p_warehouse_id IS NULL OR w.id = p_warehouse_id)
        AND (p_category_id IS NULL OR p.category_id = p_category_id)
    ORDER BY
        CASE
            WHEN ii.quantity_on_hand = 0 THEN 1
            WHEN (ii.quantity_on_hand - ii.quantity_reserved) <= 0 THEN 2
            WHEN (ii.quantity_on_hand - ii.quantity_reserved) <= p.reorder_point * 0.3 THEN 3
            ELSE 4
        END,
        (ii.quantity_on_hand - ii.quantity_reserved)::NUMERIC / GREATEST(p.reorder_point, 1) ASC;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION sp_calculate_reorder_suggestions IS
'【技術的負債】発注提案計算 - 在庫補充が必要な商品の発注推奨情報を算出する。
本来はアプリケーション層の ReorderSuggestionService で実装すべきロジック。
将来的にサービス層へ移行予定（PROQUIP-xxxx）。';


-- ----------------------------------------------------------------------------
-- sp_generate_spend_report: 購買支出レポート生成プロシージャ
--
-- 【技術的負債】このプロシージャは購買分析のビジネスロジック（集計・分析）を
-- DB層に実装している。本来は SpendAnalyticsService 等のアプリケーション
-- サービスまたは専用のBIツール/データパイプラインで実装すべきである。
--
-- 特に問題となるのは:
--   - 集計軸の追加・変更にスキーマ変更が必要になる
--   - 複雑な分析要件に対してSQLでの対応が困難になる
--   - テスト可能なドメインロジックとして管理できない
--
-- 目的:
--   指定期間の購買支出を、カテゴリ別・仕入先別・月別に集計する。
--
-- パラメータ:
--   p_start_date    - 集計開始日
--   p_end_date      - 集計終了日
--   p_department_id - 対象部門ID（NULLの場合は全部門）
--   p_group_by      - 集計軸: 'CATEGORY', 'SUPPLIER', 'MONTH'
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION sp_generate_spend_report(
    p_start_date    DATE,
    p_end_date      DATE,
    p_department_id BIGINT DEFAULT NULL,
    p_group_by      VARCHAR(20) DEFAULT 'CATEGORY'
)
RETURNS TABLE (
    group_key           VARCHAR(300),
    group_id            BIGINT,
    order_count         BIGINT,
    total_items         BIGINT,
    total_quantity      BIGINT,
    subtotal_amount     NUMERIC(15, 2),
    tax_amount          NUMERIC(15, 2),
    total_amount        NUMERIC(15, 2),
    avg_order_value     NUMERIC(15, 2),
    max_order_value     NUMERIC(15, 2),
    min_order_value     NUMERIC(15, 2),
    pct_of_total_spend  NUMERIC(5, 2)
) AS $$
-- ※ 技術的負債: このレポーティングロジックはアプリケーション層または
-- 専用のBI/分析基盤に移行すべきである。
-- TODO: SpendAnalyticsService + レポーティング基盤に移行する (PROQUIP-xxxx)
DECLARE
    v_grand_total NUMERIC(15, 2);
BEGIN
    -- 全体合計を先に算出（構成比計算用）
    -- ※ 技術的負債: この2パスの集計は非効率。アプリ層ならウィンドウ関数等で最適化可能
    SELECT COALESCE(SUM(po.total_amount), 0)
    INTO v_grand_total
    FROM purchase_order po
    WHERE po.order_date BETWEEN p_start_date AND p_end_date
      AND po.status NOT IN ('DRAFT', 'CANCELLED')
      AND (p_department_id IS NULL OR po.department_id = p_department_id);

    IF v_grand_total = 0 THEN
        v_grand_total := 1; -- ゼロ除算回避
    END IF;

    -- カテゴリ別集計
    -- ※ 技術的負債: 集計軸の追加のたびにELSIFブロックを追加する必要があり、
    --   拡張性に乏しい。Strategy パターン等でアプリ層に実装すべき。
    IF p_group_by = 'CATEGORY' THEN
        RETURN QUERY
        SELECT
            COALESCE(c.name, '未分類')::VARCHAR(300),
            c.id,
            COUNT(DISTINCT po.id),
            COUNT(poi.id)::BIGINT,
            COALESCE(SUM(poi.quantity_ordered), 0)::BIGINT,
            COALESCE(SUM(poi.total_price), 0),
            COALESCE(SUM(poi.total_price * poi.tax_rate_pct / 100), 0),
            COALESCE(SUM(poi.total_price * (1 + poi.tax_rate_pct / 100)), 0),
            CASE WHEN COUNT(DISTINCT po.id) = 0 THEN 0
                 ELSE ROUND(SUM(poi.total_price) / COUNT(DISTINCT po.id), 2)
            END,
            MAX(poi.total_price),
            MIN(poi.total_price),
            ROUND(COALESCE(SUM(poi.total_price), 0) / v_grand_total * 100, 2)
        FROM
            purchase_order po
            INNER JOIN purchase_order_item poi ON po.id = poi.order_id
            INNER JOIN product p ON poi.product_id = p.id
            LEFT JOIN category c ON p.category_id = c.id
        WHERE
            po.order_date BETWEEN p_start_date AND p_end_date
            AND po.status NOT IN ('DRAFT', 'CANCELLED')
            AND (p_department_id IS NULL OR po.department_id = p_department_id)
        GROUP BY c.id, c.name
        ORDER BY COALESCE(SUM(poi.total_price), 0) DESC;

    ELSIF p_group_by = 'SUPPLIER' THEN
        RETURN QUERY
        SELECT
            s.name::VARCHAR(300),
            s.id,
            COUNT(DISTINCT po.id),
            COUNT(poi.id)::BIGINT,
            COALESCE(SUM(poi.quantity_ordered), 0)::BIGINT,
            COALESCE(SUM(po.subtotal), 0),
            COALESCE(SUM(po.tax_amount), 0),
            COALESCE(SUM(po.total_amount), 0),
            CASE WHEN COUNT(DISTINCT po.id) = 0 THEN 0
                 ELSE ROUND(SUM(po.total_amount) / COUNT(DISTINCT po.id), 2)
            END,
            MAX(po.total_amount),
            MIN(po.total_amount),
            ROUND(COALESCE(SUM(po.total_amount), 0) / v_grand_total * 100, 2)
        FROM
            purchase_order po
            INNER JOIN supplier s ON po.supplier_id = s.id
            LEFT JOIN purchase_order_item poi ON po.id = poi.order_id
        WHERE
            po.order_date BETWEEN p_start_date AND p_end_date
            AND po.status NOT IN ('DRAFT', 'CANCELLED')
            AND (p_department_id IS NULL OR po.department_id = p_department_id)
        GROUP BY s.id, s.name
        ORDER BY COALESCE(SUM(po.total_amount), 0) DESC;

    ELSIF p_group_by = 'MONTH' THEN
        -- ※ 技術的負債: 月次集計は典型的なBIツールのユースケースであり、
        --   ストアドプロシージャで実装すべきではない
        RETURN QUERY
        SELECT
            TO_CHAR(po.order_date, 'YYYY-MM')::VARCHAR(300),
            EXTRACT(YEAR FROM po.order_date)::BIGINT * 100
                + EXTRACT(MONTH FROM po.order_date)::BIGINT,
            COUNT(DISTINCT po.id),
            COUNT(poi.id)::BIGINT,
            COALESCE(SUM(poi.quantity_ordered), 0)::BIGINT,
            COALESCE(SUM(po.subtotal), 0),
            COALESCE(SUM(po.tax_amount), 0),
            COALESCE(SUM(po.total_amount), 0),
            CASE WHEN COUNT(DISTINCT po.id) = 0 THEN 0
                 ELSE ROUND(SUM(po.total_amount) / COUNT(DISTINCT po.id), 2)
            END,
            MAX(po.total_amount),
            MIN(po.total_amount),
            ROUND(COALESCE(SUM(po.total_amount), 0) / v_grand_total * 100, 2)
        FROM
            purchase_order po
            LEFT JOIN purchase_order_item poi ON po.id = poi.order_id
        WHERE
            po.order_date BETWEEN p_start_date AND p_end_date
            AND po.status NOT IN ('DRAFT', 'CANCELLED')
            AND (p_department_id IS NULL OR po.department_id = p_department_id)
        GROUP BY TO_CHAR(po.order_date, 'YYYY-MM'),
                 EXTRACT(YEAR FROM po.order_date),
                 EXTRACT(MONTH FROM po.order_date)
        ORDER BY TO_CHAR(po.order_date, 'YYYY-MM');

    ELSE
        RAISE EXCEPTION '不正な集計軸: %. 有効な値は CATEGORY, SUPPLIER, MONTH です。', p_group_by;
    END IF;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION sp_generate_spend_report IS
'【技術的負債】購買支出レポート生成 - カテゴリ別/仕入先別/月別の購買支出集計。
本来はアプリケーション層の SpendAnalyticsService または専用BI基盤で実装すべき。
集計軸の追加に拡張性がなく、テスト困難。将来的にサービス層へ移行予定（PROQUIP-xxxx）。';


-- ----------------------------------------------------------------------------
-- sp_adjust_inventory_after_count: 棚卸差異調整プロシージャ
--
-- 【技術的負債】このプロシージャは在庫調整のビジネスロジック（差異計算・
-- トランザクション生成・承認チェック）をDB層に実装している。
-- 本来は InventoryAdjustmentService で実装すべきロジック。
--
-- 特に問題となるのは:
--   - 在庫調整の承認フローがアプリ層のワークフローと分離してしまう
--   - エラーハンドリングやリトライロジックがDB層では限定的
--   - ドメインイベントの発行（通知トリガー等）がDB層では困難
--
-- 目的:
--   棚卸結果に基づいて在庫数量を調整し、調整トランザクションを自動生成する。
--
-- パラメータ:
--   p_count_id     - 棚卸ID
--   p_adjusted_by  - 調整実行者のユーザーID
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION sp_adjust_inventory_after_count(
    p_count_id      BIGINT,
    p_adjusted_by   BIGINT
)
RETURNS TABLE (
    adjusted_items      INTEGER,
    total_variance_qty  INTEGER,
    total_variance_value NUMERIC(15, 2),
    positive_adjustments INTEGER,
    negative_adjustments INTEGER
) AS $$
-- ※ 技術的負債: 在庫調整のビジネスロジックはアプリケーション層に移行すべき
-- TODO: InventoryAdjustmentService に移行する (PROQUIP-xxxx)
DECLARE
    v_count_record      RECORD;
    v_item_record       RECORD;
    v_adjusted          INTEGER := 0;
    v_total_var_qty     INTEGER := 0;
    v_total_var_value   NUMERIC(15, 2) := 0;
    v_positive          INTEGER := 0;
    v_negative          INTEGER := 0;
    v_variance          INTEGER;
    v_inv_item          RECORD;
BEGIN
    -- 棚卸レコードの存在と状態を確認
    SELECT ic.* INTO v_count_record
    FROM inventory_count ic
    WHERE ic.id = p_count_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION '棚卸ID % が見つかりません。', p_count_id;
    END IF;

    IF v_count_record.status <> 'COMPLETED' THEN
        RAISE EXCEPTION '棚卸ID % の状態が COMPLETED ではありません（現在: %）。調整は完了済の棚卸に対してのみ実行可能です。',
            p_count_id, v_count_record.status;
    END IF;

    -- 棚卸明細を1件ずつ処理
    -- ※ 技術的負債: カーソルループによる逐次処理は非効率。
    --   アプリ層ではバッチ処理やバルク更新で最適化可能
    FOR v_item_record IN
        SELECT
            ci.id AS count_item_id,
            ci.product_id,
            ci.storage_location_id,
            ci.system_quantity,
            ci.counted_quantity,
            ci.adjustment_applied
        FROM inventory_count_item ci
        WHERE ci.count_id = p_count_id
          AND ci.counted_quantity IS NOT NULL
          AND ci.adjustment_applied = FALSE
    LOOP
        v_variance := v_item_record.counted_quantity - v_item_record.system_quantity;

        -- 差異がない場合はスキップ
        IF v_variance = 0 THEN
            -- 調整済フラグのみ更新
            UPDATE inventory_count_item
            SET adjustment_applied = TRUE,
                variance = 0,
                variance_value = 0,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = v_item_record.count_item_id;
            CONTINUE;
        END IF;

        -- 対象の在庫レコードを取得
        SELECT ii.* INTO v_inv_item
        FROM inventory_item ii
        WHERE ii.product_id = v_item_record.product_id
          AND ii.warehouse_id = v_count_record.warehouse_id;

        IF FOUND THEN
            -- 在庫トランザクションを生成
            -- ※ 技術的負債: トランザクション生成はドメインイベントとして
            --   アプリ層で管理すべき（通知・監査ログとの連携のため）
            INSERT INTO inventory_transaction (
                product_id, warehouse_id, transaction_type,
                reference_type, reference_id,
                quantity, quantity_before, quantity_after,
                unit_cost, total_cost,
                performed_by, notes, transaction_date
            ) VALUES (
                v_item_record.product_id,
                v_count_record.warehouse_id,
                CASE WHEN v_variance > 0 THEN 'ADJUSTMENT_PLUS' ELSE 'ADJUSTMENT_MINUS' END,
                'INVENTORY_COUNT',
                p_count_id,
                ABS(v_variance),
                v_inv_item.quantity_on_hand,
                v_inv_item.quantity_on_hand + v_variance,
                v_inv_item.unit_cost,
                ABS(v_variance) * v_inv_item.unit_cost,
                p_adjusted_by,
                '棚卸差異調整（棚卸番号: ' || v_count_record.count_number || '）',
                CURRENT_TIMESTAMP
            );

            -- 在庫数量を更新
            UPDATE inventory_item
            SET quantity_on_hand = quantity_on_hand + v_variance,
                total_value = (quantity_on_hand + v_variance) * unit_cost,
                last_counted_at = CURRENT_TIMESTAMP,
                last_movement_at = CURRENT_TIMESTAMP,
                updated_at = CURRENT_TIMESTAMP,
                version = version + 1
            WHERE id = v_inv_item.id;

            -- 棚卸明細の差異情報を更新
            UPDATE inventory_count_item
            SET variance = v_variance,
                variance_value = ABS(v_variance) * v_inv_item.unit_cost,
                adjustment_applied = TRUE,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = v_item_record.count_item_id;

            v_adjusted := v_adjusted + 1;
            v_total_var_qty := v_total_var_qty + ABS(v_variance);
            v_total_var_value := v_total_var_value + ABS(v_variance) * v_inv_item.unit_cost;

            IF v_variance > 0 THEN
                v_positive := v_positive + 1;
            ELSE
                v_negative := v_negative + 1;
            END IF;
        END IF;
    END LOOP;

    -- 棚卸の状態を「承認済」に更新
    UPDATE inventory_count
    SET status = 'APPROVED',
        approved_by = p_adjusted_by,
        discrepancy_items = v_adjusted,
        updated_at = CURRENT_TIMESTAMP,
        version = version + 1
    WHERE id = p_count_id;

    -- 結果を返却
    RETURN QUERY SELECT v_adjusted, v_total_var_qty, v_total_var_value, v_positive, v_negative;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION sp_adjust_inventory_after_count IS
'【技術的負債】棚卸差異調整 - 棚卸結果に基づく在庫数量の自動調整とトランザクション生成。
本来は InventoryAdjustmentService で実装すべきロジック。
在庫調整の承認フロー・ドメインイベント発行がDB層では困難。
将来的にサービス層へ移行予定（PROQUIP-xxxx）。';
