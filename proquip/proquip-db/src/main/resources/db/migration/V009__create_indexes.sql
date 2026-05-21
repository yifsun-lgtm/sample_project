-- ============================================================================
-- ProQuip - Enterprise Procurement & Inventory Management System
-- V009: パフォーマンスインデックスの作成
-- 外部キー、ステータス、日付、複合インデックス、全文検索インデックス
-- ============================================================================

-- ============================================================================
-- 組織管理テーブルのインデックス（V001）
-- ============================================================================

-- 部門テーブル
CREATE INDEX idx_department_parent_id ON department(parent_id);
CREATE INDEX idx_department_is_active ON department(is_active);
CREATE INDEX idx_department_manager_id ON department(manager_id);

-- ユーザープロファイルテーブル
CREATE INDEX idx_user_profile_department_id ON user_profile(department_id);
CREATE INDEX idx_user_profile_is_active ON user_profile(is_active);
CREATE INDEX idx_user_profile_last_name ON user_profile(last_name);
-- 名前（カナ）での検索・ソート用
CREATE INDEX idx_user_profile_last_name_kana ON user_profile(last_name_kana);
CREATE INDEX idx_user_profile_email ON user_profile(email);

-- 委任ルールテーブル
CREATE INDEX idx_delegation_delegator ON delegation_rule(delegator_id);
CREATE INDEX idx_delegation_delegate ON delegation_rule(delegate_id);
-- 有効な委任ルールの検索（現在有効かつアクティブなもの）
CREATE INDEX idx_delegation_active ON delegation_rule(is_active, valid_from, valid_until);

-- ============================================================================
-- 商品管理テーブルのインデックス（V002）
-- ============================================================================

-- メーカーテーブル
CREATE INDEX idx_manufacturer_is_active ON manufacturer(is_active);

-- カテゴリテーブル
CREATE INDEX idx_category_parent_id ON category(parent_id);
CREATE INDEX idx_category_level ON category(level);
CREATE INDEX idx_category_is_active ON category(is_active);
-- マテリアライズドパスによるツリー検索
CREATE INDEX idx_category_path ON category(path);

-- 単位テーブル
CREATE INDEX idx_unit_base_unit_id ON unit_of_measure(base_unit_id);

-- 商品テーブル
CREATE INDEX idx_product_category_id ON product(category_id);
CREATE INDEX idx_product_manufacturer_id ON product(manufacturer_id);
CREATE INDEX idx_product_status ON product(status);
CREATE INDEX idx_product_barcode ON product(barcode);
-- カテゴリ×ステータスでの商品一覧検索
CREATE INDEX idx_product_category_status ON product(category_id, status);
-- 発注点による在庫補充対象の検索
CREATE INDEX idx_product_reorder ON product(reorder_point, status) WHERE status = 'ACTIVE';

-- 商品の全文検索インデックス（商品名・説明文での検索）
-- GINインデックスを使用し、日本語を含む多言語検索に対応する
CREATE INDEX idx_product_fulltext ON product
    USING GIN (to_tsvector('simple', COALESCE(name, '') || ' ' || COALESCE(name_en, '') || ' ' || COALESCE(description, '') || ' ' || COALESCE(sku, '')));

-- 商品仕様テーブル
CREATE INDEX idx_product_spec_product_id ON product_specification(product_id);
-- 仕様名での検索（例: 特定の仕様を持つ商品の絞り込み）
CREATE INDEX idx_product_spec_name ON product_specification(spec_name);

-- 商品画像テーブル
CREATE INDEX idx_product_image_product_id ON product_image(product_id);
CREATE INDEX idx_product_image_primary ON product_image(product_id, is_primary) WHERE is_primary = TRUE;

-- 代替商品テーブル
CREATE INDEX idx_product_alt_product_id ON product_alternative(product_id);
CREATE INDEX idx_product_alt_alternative_id ON product_alternative(alternative_product_id);

-- 商品バンドル明細テーブル
CREATE INDEX idx_bundle_item_bundle_id ON product_bundle_item(bundle_id);
CREATE INDEX idx_bundle_item_product_id ON product_bundle_item(product_id);

-- 商品変更履歴テーブル
CREATE INDEX idx_product_changelog_product_id ON product_change_log(product_id);
CREATE INDEX idx_product_changelog_created ON product_change_log(created_at DESC);

-- 商品ドキュメントテーブル
CREATE INDEX idx_product_doc_product_id ON product_document(product_id);
CREATE INDEX idx_product_doc_type ON product_document(document_type);

-- ============================================================================
-- 仕入先管理テーブルのインデックス（V003）
-- ============================================================================

-- 仕入先テーブル
CREATE INDEX idx_supplier_status ON supplier(status);
CREATE INDEX idx_supplier_risk_level ON supplier(risk_level);
CREATE INDEX idx_supplier_payment_terms ON supplier(payment_terms);

-- 仕入先連絡先テーブル
CREATE INDEX idx_supplier_contact_supplier_id ON supplier_contact(supplier_id);
CREATE INDEX idx_supplier_contact_primary ON supplier_contact(supplier_id, is_primary) WHERE is_primary = TRUE;

-- 仕入先住所テーブル
CREATE INDEX idx_supplier_address_supplier_id ON supplier_address(supplier_id);

-- 仕入先取扱商品テーブル
CREATE INDEX idx_supplier_product_supplier_id ON supplier_product(supplier_id);
CREATE INDEX idx_supplier_product_product_id ON supplier_product(product_id);
-- 商品ごとの優先仕入先検索
CREATE INDEX idx_supplier_product_preferred ON supplier_product(product_id, is_preferred) WHERE is_preferred = TRUE;

-- 仕入先契約テーブル
CREATE INDEX idx_supplier_contract_supplier_id ON supplier_contract(supplier_id);
CREATE INDEX idx_supplier_contract_status ON supplier_contract(status);
-- 契約期限管理用（有効期限切れ間近の契約検索）
CREATE INDEX idx_supplier_contract_dates ON supplier_contract(end_date, status);
CREATE INDEX idx_supplier_contract_signed_by ON supplier_contract(signed_by);

-- 仕入先契約明細テーブル
CREATE INDEX idx_contract_item_contract_id ON supplier_contract_item(contract_id);
CREATE INDEX idx_contract_item_product_id ON supplier_contract_item(product_id);

-- 仕入先評価テーブル
CREATE INDEX idx_supplier_rating_supplier_id ON supplier_rating(supplier_id);
CREATE INDEX idx_supplier_rating_rated_at ON supplier_rating(rated_at DESC);
-- 仕入先ごとの最新評価取得用
CREATE INDEX idx_supplier_rating_latest ON supplier_rating(supplier_id, rated_at DESC);

-- 仕入先認証テーブル
CREATE INDEX idx_supplier_cert_supplier_id ON supplier_certification(supplier_id);
CREATE INDEX idx_supplier_cert_expiry ON supplier_certification(expiry_date, status);

-- ============================================================================
-- 調達管理テーブルのインデックス（V004）
-- ============================================================================

-- 購買依頼テーブル
CREATE INDEX idx_requisition_requester ON purchase_requisition(requester_id);
CREATE INDEX idx_requisition_department ON purchase_requisition(department_id);
CREATE INDEX idx_requisition_status ON purchase_requisition(status);
CREATE INDEX idx_requisition_submitted ON purchase_requisition(submitted_at DESC);
-- 自分の部門の承認待ち依頼検索
CREATE INDEX idx_requisition_dept_status ON purchase_requisition(department_id, status);

-- 購買依頼明細テーブル
CREATE INDEX idx_req_item_requisition_id ON purchase_requisition_item(requisition_id);
CREATE INDEX idx_req_item_product_id ON purchase_requisition_item(product_id);

-- 発注テーブル
CREATE INDEX idx_po_supplier_id ON purchase_order(supplier_id);
CREATE INDEX idx_po_ordered_by ON purchase_order(ordered_by);
CREATE INDEX idx_po_department_id ON purchase_order(department_id);
CREATE INDEX idx_po_status ON purchase_order(status);
CREATE INDEX idx_po_order_date ON purchase_order(order_date DESC);
CREATE INDEX idx_po_expected_delivery ON purchase_order(expected_delivery_date);
-- 仕入先別の発注履歴検索
CREATE INDEX idx_po_supplier_status ON purchase_order(supplier_id, status);
-- 期限超過の発注検索
CREATE INDEX idx_po_overdue ON purchase_order(expected_delivery_date, status)
    WHERE status IN ('ORDERED', 'ACKNOWLEDGED', 'PARTIALLY_RECEIVED');
CREATE INDEX idx_po_contract_id ON purchase_order(contract_id);

-- 発注明細テーブル
CREATE INDEX idx_poi_order_id ON purchase_order_item(order_id);
CREATE INDEX idx_poi_product_id ON purchase_order_item(product_id);

-- 承認ワークフローテーブル
CREATE INDEX idx_workflow_entity ON approval_workflow(entity_type, entity_id);
CREATE INDEX idx_workflow_status ON approval_workflow(status);
CREATE INDEX idx_workflow_initiated_by ON approval_workflow(initiated_by);

-- 承認ステップテーブル
CREATE INDEX idx_approval_step_workflow ON approval_step(workflow_id);
CREATE INDEX idx_approval_step_approver ON approval_step(approver_id);
-- 承認待ちタスク一覧の表示用
CREATE INDEX idx_approval_step_pending ON approval_step(approver_id, status) WHERE status IN ('PENDING', 'IN_PROGRESS');

-- 検収テーブル
CREATE INDEX idx_receipt_order_id ON goods_receipt(order_id);
CREATE INDEX idx_receipt_received_by ON goods_receipt(received_by);
CREATE INDEX idx_receipt_warehouse ON goods_receipt(warehouse_id);
CREATE INDEX idx_receipt_status ON goods_receipt(status);
CREATE INDEX idx_receipt_date ON goods_receipt(receipt_date DESC);

-- 検収明細テーブル
CREATE INDEX idx_gri_receipt_id ON goods_receipt_item(receipt_id);
CREATE INDEX idx_gri_order_item_id ON goods_receipt_item(order_item_id);
CREATE INDEX idx_gri_product_id ON goods_receipt_item(product_id);

-- 発注状態履歴テーブル
CREATE INDEX idx_po_history_order_id ON purchase_order_status_history(order_id);
CREATE INDEX idx_po_history_changed_at ON purchase_order_status_history(changed_at DESC);

-- 仕入先返品テーブル
CREATE INDEX idx_return_order_id ON return_to_supplier(order_id);
CREATE INDEX idx_return_supplier_id ON return_to_supplier(supplier_id);
CREATE INDEX idx_return_status ON return_to_supplier(status);

-- ============================================================================
-- 在庫管理テーブルのインデックス（V005）
-- ============================================================================

-- 倉庫テーブル
CREATE INDEX idx_warehouse_is_active ON warehouse(is_active);
CREATE INDEX idx_warehouse_manager ON warehouse(manager_id);

-- 倉庫ゾーンテーブル
CREATE INDEX idx_zone_warehouse_id ON warehouse_zone(warehouse_id);

-- 保管場所テーブル
CREATE INDEX idx_location_zone_id ON storage_location(zone_id);
CREATE INDEX idx_location_is_occupied ON storage_location(is_occupied);

-- 在庫テーブル
CREATE INDEX idx_inv_item_product_id ON inventory_item(product_id);
CREATE INDEX idx_inv_item_warehouse_id ON inventory_item(warehouse_id);
-- 商品×倉庫での在庫検索（最も頻繁に使用されるクエリパターン）
CREATE INDEX idx_inv_item_product_warehouse ON inventory_item(product_id, warehouse_id);
-- 在庫切れ・低在庫の検索
CREATE INDEX idx_inv_item_low_stock ON inventory_item(quantity_on_hand);
CREATE INDEX idx_inv_item_lot ON inventory_item(lot_number) WHERE lot_number IS NOT NULL;
-- 有効期限管理品の検索
CREATE INDEX idx_inv_item_expiry ON inventory_item(expiry_date) WHERE expiry_date IS NOT NULL;

-- 在庫トランザクションテーブル
CREATE INDEX idx_inv_txn_product_id ON inventory_transaction(product_id);
CREATE INDEX idx_inv_txn_warehouse_id ON inventory_transaction(warehouse_id);
CREATE INDEX idx_inv_txn_type ON inventory_transaction(transaction_type);
CREATE INDEX idx_inv_txn_date ON inventory_transaction(transaction_date DESC);
-- 参照元エンティティからの検索
CREATE INDEX idx_inv_txn_reference ON inventory_transaction(reference_type, reference_id);
-- 商品ごとの取引履歴表示
CREATE INDEX idx_inv_txn_product_date ON inventory_transaction(product_id, transaction_date DESC);

-- 在庫移動テーブル
CREATE INDEX idx_transfer_from ON stock_transfer(from_warehouse_id);
CREATE INDEX idx_transfer_to ON stock_transfer(to_warehouse_id);
CREATE INDEX idx_transfer_status ON stock_transfer(status);
CREATE INDEX idx_transfer_requested_by ON stock_transfer(requested_by);

-- 在庫移動明細テーブル
CREATE INDEX idx_sti_transfer_id ON stock_transfer_item(transfer_id);
CREATE INDEX idx_sti_product_id ON stock_transfer_item(product_id);

-- 棚卸テーブル
CREATE INDEX idx_count_warehouse ON inventory_count(warehouse_id);
CREATE INDEX idx_count_status ON inventory_count(status);
CREATE INDEX idx_count_planned_date ON inventory_count(planned_date);

-- 棚卸明細テーブル
CREATE INDEX idx_ci_count_id ON inventory_count_item(count_id);
CREATE INDEX idx_ci_product_id ON inventory_count_item(product_id);

-- ============================================================================
-- 価格・予算管理テーブルのインデックス（V006）
-- ============================================================================

-- 価格表テーブル
CREATE INDEX idx_price_list_status ON price_list(status);
CREATE INDEX idx_price_list_effective ON price_list(effective_from, effective_until);
-- 現在有効な価格表の検索
CREATE INDEX idx_price_list_active ON price_list(status, effective_from, effective_until)
    WHERE status = 'ACTIVE';

-- 価格表明細テーブル
CREATE INDEX idx_pli_price_list_id ON price_list_item(price_list_id);
CREATE INDEX idx_pli_product_id ON price_list_item(product_id);
-- 商品ごとの有効な価格検索
CREATE INDEX idx_pli_product_price ON price_list_item(product_id, min_quantity);

-- 税率テーブル
CREATE INDEX idx_tax_rate_country ON tax_rate(country_code);
CREATE INDEX idx_tax_rate_effective ON tax_rate(effective_from, effective_until);
CREATE INDEX idx_tax_rate_active ON tax_rate(is_active, country_code);

-- 予算テーブル
CREATE INDEX idx_budget_department ON budget(department_id);
CREATE INDEX idx_budget_fiscal_year ON budget(fiscal_year);
CREATE INDEX idx_budget_status ON budget(status);
CREATE INDEX idx_budget_dept_year ON budget(department_id, fiscal_year);

-- 予算明細テーブル
CREATE INDEX idx_bli_budget_id ON budget_line_item(budget_id);
CREATE INDEX idx_bli_category_id ON budget_line_item(category_id);

-- ============================================================================
-- 通知テーブルのインデックス（V007）
-- ============================================================================

-- 通知テンプレートテーブル
CREATE INDEX idx_template_event_type ON notification_template(event_type);
CREATE INDEX idx_template_channel ON notification_template(channel);

-- 通知テーブル
CREATE INDEX idx_notification_user_id ON notification(user_id);
CREATE INDEX idx_notification_status ON notification(status);
-- ユーザーの未読通知取得（最も頻繁に使用）
CREATE INDEX idx_notification_user_unread ON notification(user_id, status, created_at DESC)
    WHERE status IN ('PENDING', 'SENT', 'DELIVERED');
-- 関連エンティティからの通知検索
CREATE INDEX idx_notification_entity ON notification(entity_type, entity_id);
CREATE INDEX idx_notification_expires ON notification(expires_at) WHERE expires_at IS NOT NULL;

-- ============================================================================
-- 監査・システム管理テーブルのインデックス（V008）
-- ============================================================================

-- 監査ログテーブル
CREATE INDEX idx_audit_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_performed_by ON audit_log(performed_by);
CREATE INDEX idx_audit_performed_at ON audit_log(performed_at DESC);
CREATE INDEX idx_audit_action ON audit_log(action);
-- エンティティ単位の変更履歴表示用
CREATE INDEX idx_audit_entity_time ON audit_log(entity_type, entity_id, performed_at DESC);
-- リクエスト追跡用
CREATE INDEX idx_audit_request_id ON audit_log(request_id) WHERE request_id IS NOT NULL;
-- JSONB内の検索用GINインデックス
CREATE INDEX idx_audit_old_values ON audit_log USING GIN (old_values);
CREATE INDEX idx_audit_new_values ON audit_log USING GIN (new_values);

-- システム設定テーブル
CREATE INDEX idx_config_group ON system_configuration(config_group);

-- インポートジョブテーブル
CREATE INDEX idx_import_started_by ON import_job(started_by);
CREATE INDEX idx_import_status ON import_job(status);
CREATE INDEX idx_import_type ON import_job(import_type);
CREATE INDEX idx_import_created ON import_job(created_at DESC);
