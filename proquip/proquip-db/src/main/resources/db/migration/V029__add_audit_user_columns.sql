-- V029: AuditableEntity に対応する created_by / updated_by カラムを追加
-- JPA エンティティ (AuditableEntity) が要求するカラムが DDL に存在しなかった不整合を修正

-- V001: 組織・ユーザー
ALTER TABLE department ADD COLUMN created_by VARCHAR(100);
ALTER TABLE department ADD COLUMN updated_by VARCHAR(100);

ALTER TABLE user_profile ADD COLUMN created_by VARCHAR(100);
ALTER TABLE user_profile ADD COLUMN updated_by VARCHAR(100);

-- V002: 製品カタログ
ALTER TABLE category ADD COLUMN created_by VARCHAR(100);
ALTER TABLE category ADD COLUMN updated_by VARCHAR(100);

ALTER TABLE manufacturer ADD COLUMN created_by VARCHAR(100);
ALTER TABLE manufacturer ADD COLUMN updated_by VARCHAR(100);

ALTER TABLE product ADD COLUMN created_by VARCHAR(100);
ALTER TABLE product ADD COLUMN updated_by VARCHAR(100);

ALTER TABLE product_bundle ADD COLUMN created_by VARCHAR(100);
ALTER TABLE product_bundle ADD COLUMN updated_by VARCHAR(100);

ALTER TABLE product_document ADD COLUMN created_by VARCHAR(100);
ALTER TABLE product_document ADD COLUMN updated_by VARCHAR(100);

-- V003: サプライヤー
ALTER TABLE supplier ADD COLUMN created_by VARCHAR(100);
ALTER TABLE supplier ADD COLUMN updated_by VARCHAR(100);

ALTER TABLE supplier_contract ADD COLUMN created_by VARCHAR(100);
ALTER TABLE supplier_contract ADD COLUMN updated_by VARCHAR(100);

-- V004: 調達管理
ALTER TABLE purchase_requisition ADD COLUMN created_by VARCHAR(100);
ALTER TABLE purchase_requisition ADD COLUMN updated_by VARCHAR(100);

ALTER TABLE purchase_order ADD COLUMN created_by VARCHAR(100);
ALTER TABLE purchase_order ADD COLUMN updated_by VARCHAR(100);

ALTER TABLE goods_receipt ADD COLUMN created_by VARCHAR(100);
ALTER TABLE goods_receipt ADD COLUMN updated_by VARCHAR(100);

ALTER TABLE return_to_supplier ADD COLUMN created_by VARCHAR(100);
ALTER TABLE return_to_supplier ADD COLUMN updated_by VARCHAR(100);

-- V005: 在庫・倉庫
ALTER TABLE warehouse ADD COLUMN created_by VARCHAR(100);
ALTER TABLE warehouse ADD COLUMN updated_by VARCHAR(100);

ALTER TABLE stock_transfer ADD COLUMN created_by VARCHAR(100);
ALTER TABLE stock_transfer ADD COLUMN updated_by VARCHAR(100);

ALTER TABLE inventory_count ADD COLUMN created_by VARCHAR(100);
ALTER TABLE inventory_count ADD COLUMN updated_by VARCHAR(100);

-- V006: 価格・予算
-- price_list.created_by は BIGINT (FK) で既存 → FK削除後に VARCHAR(100) に変更
ALTER TABLE price_list DROP CONSTRAINT IF EXISTS price_list_created_by_fkey;
ALTER TABLE price_list ALTER COLUMN created_by TYPE VARCHAR(100) USING created_by::VARCHAR;
ALTER TABLE price_list ADD COLUMN updated_by VARCHAR(100);

ALTER TABLE budget ADD COLUMN created_by VARCHAR(100);
ALTER TABLE budget ADD COLUMN updated_by VARCHAR(100);

-- V007: 通知
ALTER TABLE notification_template ADD COLUMN created_by VARCHAR(100);
ALTER TABLE notification_template ADD COLUMN updated_by VARCHAR(100);

ALTER TABLE notification ADD COLUMN created_by VARCHAR(100);
ALTER TABLE notification ADD COLUMN updated_by VARCHAR(100);

-- V008: システム
ALTER TABLE system_configuration ADD COLUMN created_by VARCHAR(100);
-- system_configuration.updated_by は BIGINT (FK) で既存 → FK削除後に VARCHAR(100) に変更
ALTER TABLE system_configuration DROP CONSTRAINT IF EXISTS system_configuration_updated_by_fkey;
ALTER TABLE system_configuration ALTER COLUMN updated_by TYPE VARCHAR(100) USING updated_by::VARCHAR;

ALTER TABLE import_job ADD COLUMN created_by VARCHAR(100);
ALTER TABLE import_job ADD COLUMN updated_by VARCHAR(100);
