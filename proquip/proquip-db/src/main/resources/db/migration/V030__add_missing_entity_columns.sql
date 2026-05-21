-- V030: JPA エンティティに定義されているがDDLに存在しないカラムを追加

-- purchase_order_item: status カラム追加
ALTER TABLE purchase_order_item ADD COLUMN status VARCHAR(20);

-- delegation_rule: 不足カラム追加
ALTER TABLE delegation_rule ADD COLUMN scope VARCHAR(100);
ALTER TABLE delegation_rule ADD COLUMN valid_to DATE;
ALTER TABLE delegation_rule ADD COLUMN delegate_from_user_id BIGINT REFERENCES user_profile(id) ON DELETE SET NULL;
ALTER TABLE delegation_rule ADD COLUMN delegate_to_user_id BIGINT REFERENCES user_profile(id) ON DELETE SET NULL;

-- product_image: ファイル情報カラム追加
ALTER TABLE product_image ADD COLUMN file_name VARCHAR(500);
ALTER TABLE product_image ADD COLUMN file_path VARCHAR(500);

-- system_configuration: last_modified_by カラム追加
ALTER TABLE system_configuration ADD COLUMN last_modified_by VARCHAR(100);
