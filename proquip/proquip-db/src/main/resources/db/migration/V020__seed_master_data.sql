-- ============================================================================
-- ProQuip - Enterprise Procurement & Inventory Management System
-- V020: マスタデータのシード投入
-- 部門、ロール、権限、ユーザー、通貨、税率、単位、通知テンプレート
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 部門マスタ（8件）
-- 経営企画部をルートとし、他の部門が配下に紐づく階層構造
-- ----------------------------------------------------------------------------
INSERT INTO department (id, parent_id, department_code, name, name_en, description, level, sort_order, is_active, cost_center)
VALUES
    (1, NULL, 'DEPT-MGMT', '経営企画部', 'Corporate Planning', '全社の経営戦略・企画を統括する部門', 0, 1, TRUE, 'CC1000'),
    (2, 1, 'DEPT-IT', '情報システム部', 'Information Systems', '社内ITインフラ・システム開発・運用を担当する部門', 1, 2, TRUE, 'CC2000'),
    (3, 1, 'DEPT-PROC', '調達部', 'Procurement', '全社の購買・調達業務を担当する部門', 1, 3, TRUE, 'CC3000'),
    (4, 1, 'DEPT-WH', '倉庫管理部', 'Warehouse Management', '在庫管理・入出庫業務を担当する部門', 1, 4, TRUE, 'CC4000'),
    (5, 1, 'DEPT-SALES', '営業部', 'Sales', '法人営業・顧客対応を担当する部門', 1, 5, TRUE, 'CC5000'),
    (6, 1, 'DEPT-ACCT', '経理部', 'Accounting', '会計・財務・予算管理を担当する部門', 1, 6, TRUE, 'CC6000'),
    (7, 1, 'DEPT-HR', '人事部', 'Human Resources', '人事・労務・採用を担当する部門', 1, 7, TRUE, 'CC7000'),
    (8, 1, 'DEPT-QA', '品質管理部', 'Quality Assurance', '購買品の品質検査・品質保証を担当する部門', 1, 8, TRUE, 'CC8000');

-- シーケンスを更新
SELECT setval('department_id_seq', 8);

-- ----------------------------------------------------------------------------
-- ロールマスタ（5件）
-- システムの役割を定義する。is_system_role=TRUEの場合は削除不可。
-- ----------------------------------------------------------------------------
INSERT INTO role (id, role_code, name, description, is_system_role, is_active)
VALUES
    (1, 'ADMIN', 'システム管理者', 'システム全体の管理権限を持つ管理者ロール', TRUE, TRUE),
    (2, 'MANAGER', '部門管理者', '部門の承認・管理権限を持つ管理者ロール', TRUE, TRUE),
    (3, 'BUYER', '購買担当者', '調達・発注業務を担当するロール', TRUE, TRUE),
    (4, 'WAREHOUSE_STAFF', '倉庫担当者', '在庫管理・入出庫業務を担当するロール', TRUE, TRUE),
    (5, 'VIEWER', '閲覧者', '各種データの閲覧のみ可能なロール', TRUE, TRUE);

SELECT setval('role_id_seq', 5);

-- ----------------------------------------------------------------------------
-- 権限マスタ（20件）
-- リソースとアクションの組み合わせで操作権限を定義する。
-- ----------------------------------------------------------------------------
INSERT INTO permission (id, permission_code, resource, action, description)
VALUES
    (1,  'product.read',      'product',       'READ',    '商品情報の閲覧'),
    (2,  'product.write',     'product',       'WRITE',   '商品情報の作成・更新'),
    (3,  'supplier.read',     'supplier',      'READ',    '仕入先情報の閲覧'),
    (4,  'supplier.write',    'supplier',      'WRITE',   '仕入先情報の作成・更新'),
    (5,  'order.read',        'purchase_order','READ',    '発注情報の閲覧'),
    (6,  'order.write',       'purchase_order','WRITE',   '発注の作成・更新'),
    (7,  'order.approve',     'purchase_order','APPROVE', '発注の承認'),
    (8,  'inventory.read',    'inventory',     'READ',    '在庫情報の閲覧'),
    (9,  'inventory.write',   'inventory',     'WRITE',   '在庫の入出庫・調整'),
    (10, 'budget.read',       'budget',        'READ',    '予算情報の閲覧'),
    (11, 'budget.write',      'budget',        'WRITE',   '予算の作成・更新'),
    (12, 'report.read',       'report',        'READ',    'レポートの閲覧'),
    (13, 'admin.read',        'admin',         'READ',    '管理画面の閲覧'),
    (14, 'admin.write',       'admin',         'WRITE',   '管理設定の変更'),
    (15, 'requisition.read',  'requisition',   'READ',    '購買依頼の閲覧'),
    (16, 'requisition.write', 'requisition',   'WRITE',   '購買依頼の作成・更新'),
    (17, 'requisition.approve','requisition',  'APPROVE', '購買依頼の承認'),
    (18, 'warehouse.read',    'warehouse',     'READ',    '倉庫情報の閲覧'),
    (19, 'warehouse.write',   'warehouse',     'WRITE',   '倉庫情報の管理'),
    (20, 'user.manage',       'user',          'MANAGE',  'ユーザー管理');

SELECT setval('permission_id_seq', 20);

-- ----------------------------------------------------------------------------
-- ロール・権限マッピング
-- ADMIN: 全権限（20件）
-- MANAGER: admin.write, user.manage以外（18件）
-- BUYER: 商品・仕入先・発注・購買依頼・予算閲覧・レポート（12件）
-- WAREHOUSE_STAFF: 在庫・倉庫・商品閲覧・レポート（6件）
-- VIEWER: *.read系のみ（9件）
-- ----------------------------------------------------------------------------

-- ADMIN: 全権限
INSERT INTO role_permission_mapping (role_id, permission_id)
SELECT 1, id FROM permission;

-- MANAGER: admin.write(14)とuser.manage(20)以外
INSERT INTO role_permission_mapping (role_id, permission_id)
SELECT 2, id FROM permission WHERE id NOT IN (14, 20);

-- BUYER: 商品・仕入先・発注・購買依頼の読み書き、予算閲覧、レポート閲覧
INSERT INTO role_permission_mapping (role_id, permission_id)
VALUES
    (3, 1),  -- product.read
    (3, 2),  -- product.write
    (3, 3),  -- supplier.read
    (3, 4),  -- supplier.write
    (3, 5),  -- order.read
    (3, 6),  -- order.write
    (3, 8),  -- inventory.read
    (3, 10), -- budget.read
    (3, 12), -- report.read
    (3, 15), -- requisition.read
    (3, 16), -- requisition.write
    (3, 18); -- warehouse.read

-- WAREHOUSE_STAFF: 在庫・倉庫管理、商品閲覧、レポート閲覧
INSERT INTO role_permission_mapping (role_id, permission_id)
VALUES
    (4, 1),  -- product.read
    (4, 8),  -- inventory.read
    (4, 9),  -- inventory.write
    (4, 12), -- report.read
    (4, 18), -- warehouse.read
    (4, 19); -- warehouse.write

-- VIEWER: 各種閲覧権限のみ
INSERT INTO role_permission_mapping (role_id, permission_id)
VALUES
    (5, 1),  -- product.read
    (5, 3),  -- supplier.read
    (5, 5),  -- order.read
    (5, 8),  -- inventory.read
    (5, 10), -- budget.read
    (5, 12), -- report.read
    (5, 13), -- admin.read
    (5, 15), -- requisition.read
    (5, 18); -- warehouse.read

-- ----------------------------------------------------------------------------
-- ユーザープロファイル（10件）
-- Keycloak連携を前提とした業務ユーザー情報
-- ----------------------------------------------------------------------------
INSERT INTO user_profile (id, keycloak_id, employee_number, username, email, first_name, last_name,
    first_name_kana, last_name_kana, phone, mobile_phone, department_id, job_title,
    approval_limit, locale, timezone, is_active, last_login_at)
VALUES
    (1,  'kc-001-tanaka-taro',    'EMP-001', 'tanaka.taro',    'tanaka.taro@proquip.example.com',
     '太郎', '田中', 'タロウ', 'タナカ', '03-1234-5001', '090-1234-5001', 1, '取締役 経営企画部長',
     50000000.00, 'ja', 'Asia/Tokyo', TRUE, '2025-01-15 09:00:00'),
    (2,  'kc-002-sato-hanako',    'EMP-002', 'sato.hanako',    'sato.hanako@proquip.example.com',
     '花子', '佐藤', 'ハナコ', 'サトウ', '03-1234-5002', '090-1234-5002', 2, '情報システム部長',
     10000000.00, 'ja', 'Asia/Tokyo', TRUE, '2025-01-15 08:30:00'),
    (3,  'kc-003-suzuki-ichiro',  'EMP-003', 'suzuki.ichiro',  'suzuki.ichiro@proquip.example.com',
     '一郎', '鈴木', 'イチロウ', 'スズキ', '03-1234-5003', '090-1234-5003', 3, '調達部長',
     20000000.00, 'ja', 'Asia/Tokyo', TRUE, '2025-01-14 10:00:00'),
    (4,  'kc-004-takahashi-yuki', 'EMP-004', 'takahashi.yuki', 'takahashi.yuki@proquip.example.com',
     '優希', '高橋', 'ユウキ', 'タカハシ', '03-1234-5004', '090-1234-5004', 3, '購買担当 主任',
     5000000.00, 'ja', 'Asia/Tokyo', TRUE, '2025-01-15 09:15:00'),
    (5,  'kc-005-watanabe-kenji', 'EMP-005', 'watanabe.kenji', 'watanabe.kenji@proquip.example.com',
     '健二', '渡辺', 'ケンジ', 'ワタナベ', '03-1234-5005', '090-1234-5005', 4, '倉庫管理部長',
     5000000.00, 'ja', 'Asia/Tokyo', TRUE, '2025-01-13 08:00:00'),
    (6,  'kc-006-ito-mika',       'EMP-006', 'ito.mika',       'ito.mika@proquip.example.com',
     '美香', '伊藤', 'ミカ', 'イトウ', '03-1234-5006', '090-1234-5006', 4, '倉庫担当',
     1000000.00, 'ja', 'Asia/Tokyo', TRUE, '2025-01-15 07:45:00'),
    (7,  'kc-007-yamamoto-daiki', 'EMP-007', 'yamamoto.daiki', 'yamamoto.daiki@proquip.example.com',
     '大輝', '山本', 'ダイキ', 'ヤマモト', '03-1234-5007', '090-1234-5007', 5, '営業部長',
     10000000.00, 'ja', 'Asia/Tokyo', TRUE, '2025-01-14 09:30:00'),
    (8,  'kc-008-nakamura-aoi',   'EMP-008', 'nakamura.aoi',   'nakamura.aoi@proquip.example.com',
     '葵', '中村', 'アオイ', 'ナカムラ', '03-1234-5008', '090-1234-5008', 6, '経理部長',
     15000000.00, 'ja', 'Asia/Tokyo', TRUE, '2025-01-15 08:45:00'),
    (9,  'kc-009-kobayashi-ren',  'EMP-009', 'kobayashi.ren',  'kobayashi.ren@proquip.example.com',
     '蓮', '小林', 'レン', 'コバヤシ', '03-1234-5009', '090-1234-5009', 7, '人事部長',
     5000000.00, 'ja', 'Asia/Tokyo', TRUE, '2025-01-12 10:00:00'),
    (10, 'kc-010-kato-sakura',    'EMP-010', 'kato.sakura',    'kato.sakura@proquip.example.com',
     'さくら', '加藤', 'サクラ', 'カトウ', '03-1234-5010', '090-1234-5010', 8, '品質管理部長',
     5000000.00, 'ja', 'Asia/Tokyo', TRUE, '2025-01-15 09:00:00');

SELECT setval('user_profile_id_seq', 10);

-- 部門にmanager_idを設定
UPDATE department SET manager_id = 1  WHERE id = 1; -- 経営企画部 → 田中太郎
UPDATE department SET manager_id = 2  WHERE id = 2; -- 情報システム部 → 佐藤花子
UPDATE department SET manager_id = 3  WHERE id = 3; -- 調達部 → 鈴木一郎
UPDATE department SET manager_id = 5  WHERE id = 4; -- 倉庫管理部 → 渡辺健二
UPDATE department SET manager_id = 7  WHERE id = 5; -- 営業部 → 山本大輝
UPDATE department SET manager_id = 8  WHERE id = 6; -- 経理部 → 中村葵
UPDATE department SET manager_id = 9  WHERE id = 7; -- 人事部 → 小林蓮
UPDATE department SET manager_id = 10 WHERE id = 8; -- 品質管理部 → 加藤さくら

-- ----------------------------------------------------------------------------
-- ユーザー・ロールマッピング
-- 各ユーザーにロールを割り当てる
-- ----------------------------------------------------------------------------
INSERT INTO user_role_mapping (user_id, role_id, assigned_by, assigned_at, valid_from)
VALUES
    -- 田中太郎（経営企画部長）: ADMIN
    (1, 1, 1, '2024-04-01 00:00:00', '2024-04-01 00:00:00'),
    -- 佐藤花子（情報システム部長）: ADMIN
    (2, 1, 1, '2024-04-01 00:00:00', '2024-04-01 00:00:00'),
    -- 鈴木一郎（調達部長）: MANAGER
    (3, 2, 1, '2024-04-01 00:00:00', '2024-04-01 00:00:00'),
    -- 高橋優希（購買担当）: BUYER
    (4, 3, 3, '2024-04-01 00:00:00', '2024-04-01 00:00:00'),
    -- 渡辺健二（倉庫管理部長）: MANAGER
    (5, 2, 1, '2024-04-01 00:00:00', '2024-04-01 00:00:00'),
    -- 伊藤美香（倉庫担当）: WAREHOUSE_STAFF
    (6, 4, 5, '2024-04-01 00:00:00', '2024-04-01 00:00:00'),
    -- 山本大輝（営業部長）: MANAGER
    (7, 2, 1, '2024-04-01 00:00:00', '2024-04-01 00:00:00'),
    -- 中村葵（経理部長）: MANAGER
    (8, 2, 1, '2024-04-01 00:00:00', '2024-04-01 00:00:00'),
    -- 小林蓮（人事部長）: VIEWER
    (9, 5, 1, '2024-04-01 00:00:00', '2024-04-01 00:00:00'),
    -- 加藤さくら（品質管理部長）: VIEWER
    (10, 5, 1, '2024-04-01 00:00:00', '2024-04-01 00:00:00');

-- ----------------------------------------------------------------------------
-- 通貨マスタ（3件）
-- 基準通貨はJPY。USDとEURの為替レートは2025年1月時点の概算値。
-- ----------------------------------------------------------------------------
INSERT INTO currency (id, currency_code, name, name_en, symbol, decimal_places, exchange_rate, rate_updated_at, is_base, is_active)
VALUES
    (1, 'JPY', '日本円',   'Japanese Yen', '¥', 0, 1.000000,   '2025-01-15 00:00:00', TRUE,  TRUE),
    (2, 'USD', '米ドル',   'US Dollar',    '$', 2, 157.500000, '2025-01-15 00:00:00', FALSE, TRUE),
    (3, 'EUR', 'ユーロ',   'Euro',         '€', 2, 162.300000, '2025-01-15 00:00:00', FALSE, TRUE);

SELECT setval('currency_id_seq', 3);

-- ----------------------------------------------------------------------------
-- 税率マスタ（3件）
-- 日本の消費税率体系（標準税率、軽減税率、非課税）
-- ----------------------------------------------------------------------------
INSERT INTO tax_rate (id, tax_code, name, description, tax_type, country_code, rate_pct, effective_from, is_active)
VALUES
    (1, 'JP-STD-10',   '標準税率',   '消費税 標準税率10%（2019年10月施行）', 'VAT', 'JPN', 10.00, '2019-10-01', TRUE),
    (2, 'JP-RED-08',   '軽減税率',   '消費税 軽減税率8%（食料品・新聞等）',  'VAT', 'JPN',  8.00, '2019-10-01', TRUE),
    (3, 'JP-EXEMPT-00','非課税',     '消費税非課税対象',                      'VAT', 'JPN',  0.00, '2019-10-01', TRUE);

SELECT setval('tax_rate_id_seq', 3);

-- ----------------------------------------------------------------------------
-- 単位マスタ（10件）
-- IT機器・オフィス用品の管理に必要な数量単位
-- ----------------------------------------------------------------------------
INSERT INTO unit_of_measure (id, base_unit_id, unit_code, name, name_en, symbol, conversion_factor, unit_type, is_active)
VALUES
    (1,  NULL, 'PCS',  '個',         'Piece',       '個',   1.000000, 'COUNT', TRUE),
    (2,  NULL, 'UNIT', '台',         'Unit',        '台',   1.000000, 'COUNT', TRUE),
    (3,  NULL, 'HON',  '本',         'Piece(long)', '本',   1.000000, 'COUNT', TRUE),
    (4,  1,    'BOX',  '箱',         'Box',         '箱',  10.000000, 'PACK',  TRUE),
    (5,  NULL, 'SET',  'セット',      'Set',         'セット', 1.000000, 'COUNT', TRUE),
    (6,  NULL, 'LIC',  'ライセンス',  'License',     'Lic',  1.000000, 'OTHER', TRUE),
    (7,  NULL, 'MON',  '月',         'Month',       '月',   1.000000, 'TIME',  TRUE),
    (8,  NULL, 'YR',   '年',         'Year',        '年',   1.000000, 'TIME',  TRUE),
    (9,  1,    'PACK', 'パック',      'Pack',        'パック', 5.000000, 'PACK',  TRUE),
    (10, NULL, 'ROLL', 'ロール',      'Roll',        'ロール', 1.000000, 'COUNT', TRUE);

SELECT setval('unit_of_measure_id_seq', 10);

-- ----------------------------------------------------------------------------
-- 通知テンプレート（5件）
-- 主要な業務イベントに対する通知メッセージのテンプレート
-- ----------------------------------------------------------------------------
INSERT INTO notification_template (id, template_code, name, description, channel, event_type, subject, body_text, body_html, locale, is_active)
VALUES
    (1, 'TMPL_APPROVAL_REQ', '承認依頼通知', '承認が必要な場合にユーザーへ送信される通知テンプレート',
     'BOTH', 'APPROVAL_REQUIRED',
     '【承認依頼】{{entity_type}} {{entity_number}} の承認をお願いします',
     '{{approver_name}} 様\n\n{{requester_name}} より {{entity_type}}「{{entity_number}}」の承認依頼が届いています。\n\n金額: {{amount}} 円\n期限: {{due_date}}\n\n詳細は以下のリンクからご確認ください。\n{{action_url}}',
     NULL, 'ja', TRUE),

    (2, 'TMPL_ORDER_APPROVED', '発注承認完了通知', '発注が承認された場合に申請者へ送信される通知テンプレート',
     'BOTH', 'PO_APPROVED',
     '【承認完了】発注 {{order_number}} が承認されました',
     '{{requester_name}} 様\n\n発注「{{order_number}}」が {{approver_name}} により承認されました。\n\n承認日時: {{approved_at}}\n金額: {{amount}} 円\n\n仕入先への発注手続きを進めてください。',
     NULL, 'ja', TRUE),

    (3, 'TMPL_ORDER_REJECTED', '発注却下通知', '発注が却下された場合に申請者へ送信される通知テンプレート',
     'BOTH', 'PO_REJECTED',
     '【却下】発注 {{order_number}} が却下されました',
     '{{requester_name}} 様\n\n発注「{{order_number}}」が {{approver_name}} により却下されました。\n\n却下理由: {{rejection_reason}}\n\n内容を修正の上、再提出してください。',
     NULL, 'ja', TRUE),

    (4, 'TMPL_LOW_STOCK', '在庫不足アラート', '在庫が発注点を下回った場合の警告通知テンプレート',
     'BOTH', 'LOW_STOCK_ALERT',
     '【在庫警告】{{product_name}} の在庫が不足しています',
     '倉庫管理担当者 各位\n\n以下の商品の在庫が発注点を下回りました。\n\n商品名: {{product_name}}\nSKU: {{sku}}\n現在在庫: {{current_stock}} {{unit}}\n発注点: {{reorder_point}} {{unit}}\n倉庫: {{warehouse_name}}\n\n速やかに補充発注を検討してください。',
     NULL, 'ja', TRUE),

    (5, 'TMPL_BUDGET_WARNING', '予算超過警告', '予算の消化率が閾値を超えた場合の警告通知テンプレート',
     'BOTH', 'BUDGET_THRESHOLD',
     '【予算警告】{{department_name}} の予算消化率が {{threshold_pct}}% を超えました',
     '{{manager_name}} 様\n\n{{department_name}} の {{fiscal_year}}年度 購買予算の消化率が {{threshold_pct}}% に達しました。\n\n予算総額: {{total_amount}} 円\n使用済: {{spent_amount}} 円\n残額: {{remaining_amount}} 円\n\n今後の発注計画をご確認ください。',
     NULL, 'ja', TRUE);

SELECT setval('notification_template_id_seq', 5);
