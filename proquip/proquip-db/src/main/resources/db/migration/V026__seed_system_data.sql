-- ============================================================================
-- ProQuip - Enterprise Procurement & Inventory Management System
-- V026: システムデータのシード投入
-- システム設定、通知、監査ログ、インポートジョブ
-- ============================================================================

-- ----------------------------------------------------------------------------
-- システム設定（20件）
-- アプリケーション全体の動作を制御する設定値
-- ----------------------------------------------------------------------------
INSERT INTO system_configuration (id, config_key, config_value, config_group, value_type,
    display_name, description, default_value, is_encrypted, is_editable, is_visible, sort_order)
VALUES
    -- 承認設定
    (1,  'approval.threshold.manager', '1000000', 'APPROVAL', 'INTEGER',
     '部門管理者承認閾値', '部門管理者の承認が必要な金額の閾値（円）。この金額以上の発注には管理者承認が必要。',
     '1000000', FALSE, TRUE, TRUE, 1),
    (2,  'approval.threshold.director', '5000000', 'APPROVAL', 'INTEGER',
     '取締役承認閾値', '取締役の承認が必要な金額の閾値（円）。この金額以上の発注には取締役承認が必要。',
     '5000000', FALSE, TRUE, TRUE, 2),
    (3,  'approval.auto_escalation.hours', '48', 'APPROVAL', 'INTEGER',
     '自動エスカレーション時間', '承認依頼から指定時間経過後に自動エスカレーションを実行する（時間）',
     '48', FALSE, TRUE, TRUE, 3),
    (4,  'approval.reminder.interval_hours', '24', 'APPROVAL', 'INTEGER',
     '承認リマインダー間隔', '承認待ちの案件に対するリマインダー通知の送信間隔（時間）',
     '24', FALSE, TRUE, TRUE, 4),

    -- 通知設定
    (5,  'notification.email.smtp.host', 'smtp.proquip.example.com', 'NOTIFICATION', 'STRING',
     'SMTPサーバーホスト', 'メール送信に使用するSMTPサーバーのホスト名',
     'smtp.proquip.example.com', FALSE, TRUE, TRUE, 1),
    (6,  'notification.email.smtp.port', '587', 'NOTIFICATION', 'INTEGER',
     'SMTPサーバーポート', 'SMTPサーバーの接続ポート番号',
     '587', FALSE, TRUE, TRUE, 2),
    (7,  'notification.email.from', 'noreply@proquip.example.com', 'NOTIFICATION', 'EMAIL',
     '送信元メールアドレス', 'システム通知メールの送信元アドレス',
     'noreply@proquip.example.com', FALSE, TRUE, TRUE, 3),
    (8,  'notification.email.enabled', 'true', 'NOTIFICATION', 'BOOLEAN',
     'メール通知有効', 'メール通知機能の有効/無効フラグ',
     'true', FALSE, TRUE, TRUE, 4),

    -- 在庫設定
    (9,  'inventory.reorder.check.enabled', 'true', 'INVENTORY', 'BOOLEAN',
     '発注点チェック有効', '在庫が発注点を下回った際の自動アラートの有効/無効',
     'true', FALSE, TRUE, TRUE, 1),
    (10, 'inventory.reorder.check.schedule', '0 0 8 * * MON-FRI', 'INVENTORY', 'STRING',
     '発注点チェックスケジュール', '発注点チェックバッチのcron式スケジュール（平日午前8時）',
     '0 0 8 * * MON-FRI', FALSE, TRUE, TRUE, 2),
    (11, 'inventory.low_stock.threshold_pct', '20', 'INVENTORY', 'INTEGER',
     '低在庫閾値（%）', '発注点に対する在庫割合がこの値を下回ると低在庫警告を発行',
     '20', FALSE, TRUE, TRUE, 3),

    -- 調達設定
    (12, 'procurement.po_number.prefix', 'PO', 'PROCUREMENT', 'STRING',
     '発注番号接頭辞', '発注番号の自動採番で使用する接頭辞',
     'PO', FALSE, TRUE, TRUE, 1),
    (13, 'procurement.pr_number.prefix', 'PR', 'PROCUREMENT', 'STRING',
     '購買依頼番号接頭辞', '購買依頼番号の自動採番で使用する接頭辞',
     'PR', FALSE, TRUE, TRUE, 2),
    (14, 'procurement.auto_approve.max_amount', '100000', 'PROCUREMENT', 'INTEGER',
     '自動承認上限額', 'この金額以下の発注は自動承認される（円）。0の場合は自動承認なし。',
     '0', FALSE, TRUE, TRUE, 3),

    -- 一般設定
    (15, 'system.locale', 'ja_JP', 'GENERAL', 'STRING',
     'システムロケール', 'システムのデフォルトロケール設定',
     'ja_JP', FALSE, TRUE, TRUE, 1),
    (16, 'system.timezone', 'Asia/Tokyo', 'GENERAL', 'STRING',
     'システムタイムゾーン', 'システムのデフォルトタイムゾーン',
     'Asia/Tokyo', FALSE, TRUE, TRUE, 2),
    (17, 'system.currency.default', 'JPY', 'GENERAL', 'STRING',
     'デフォルト通貨', 'システムのデフォルト通貨コード',
     'JPY', FALSE, TRUE, TRUE, 3),
    (18, 'system.pagination.default_size', '20', 'UI', 'INTEGER',
     '一覧表示件数', '各種一覧画面のデフォルト表示件数',
     '20', FALSE, TRUE, TRUE, 1),

    -- セキュリティ設定
    (19, 'security.session.timeout_minutes', '30', 'SECURITY', 'INTEGER',
     'セッションタイムアウト', 'ユーザーセッションの自動タイムアウト時間（分）',
     '30', FALSE, TRUE, TRUE, 1),
    (20, 'security.password.min_length', '12', 'SECURITY', 'INTEGER',
     'パスワード最小長', 'パスワードの最小文字数',
     '12', FALSE, TRUE, TRUE, 2);

SELECT setval('system_configuration_id_seq', 20);

-- ----------------------------------------------------------------------------
-- 通知（30件）
-- 各ユーザーへの業務通知
-- ----------------------------------------------------------------------------
INSERT INTO notification (id, user_id, template_id, channel, title, message,
    priority, status, entity_type, entity_id, action_url, sent_at, read_at)
VALUES
    -- 承認依頼通知
    (1,  3, 1, 'BOTH', '【承認依頼】購買依頼 PR-2025-000001 の承認をお願いします',
     '鈴木一郎 様\n\n高橋優希 より 購買依頼「PR-2025-000001」（2025年度新入社員PC調達）の承認依頼が届いています。\n\n金額: 3,800,000 円\n期限: 2025-03-15\n\n詳細はシステムからご確認ください。',
     'HIGH', 'SENT', 'PURCHASE_REQUISITION', 8, '/requisitions/8', '2025-01-10 09:05:00', NULL),

    (2,  1, 1, 'BOTH', '【承認依頼】購買依頼 PR-2025-000002 の承認をお願いします',
     '田中太郎 様\n\n佐藤花子 より 購買依頼「PR-2025-000002」（ワークステーション追加調達）の承認依頼が届いています。\n\n金額: 2,090,000 円\n\n詳細はシステムからご確認ください。',
     'NORMAL', 'SENT', 'PURCHASE_REQUISITION', 9, '/requisitions/9', '2025-01-12 10:05:00', NULL),

    (3,  3, 1, 'BOTH', '【承認依頼】発注 PO-2025-000004 の承認をお願いします',
     '鈴木一郎 様\n\n高橋優希 より 発注「PO-2025-000004」（新入社員用PC・モニター大口発注）の承認依頼が届いています。\n\n金額: 4,146,000 円\n\n詳細はシステムからご確認ください。',
     'HIGH', 'SENT', 'PURCHASE_ORDER', 15, '/orders/15', '2025-01-14 10:05:00', NULL),

    -- 発注承認通知
    (4,  4, 2, 'BOTH', '【承認完了】発注 PO-2024-000010 が承認されました',
     '高橋優希 様\n\n発注「PO-2024-000010」が鈴木一郎により承認されました。\n\n承認日時: 2024-12-13 09:00\n金額: 2,330,000 円\n\n仕入先への発注手続きを進めてください。',
     'NORMAL', 'READ', 'PURCHASE_ORDER', 10, '/orders/10', '2024-12-13 09:05:00', '2024-12-13 09:30:00'),

    (5,  4, 2, 'BOTH', '【承認完了】発注 PO-2024-000011 が承認されました',
     '高橋優希 様\n\n発注「PO-2024-000011」が鈴木一郎により承認されました。\n\n承認日時: 2024-12-19 10:00\n金額: 1,012,000 円',
     'NORMAL', 'READ', 'PURCHASE_ORDER', 11, '/orders/11', '2024-12-19 10:05:00', '2024-12-19 10:20:00'),

    -- 発注却下通知
    (6,  4, 3, 'BOTH', '【却下】発注 PO-2024-000012 が却下されました',
     '高橋優希 様\n\n発注「PO-2024-000012」が田中太郎により却下されました。\n\n却下理由: テクノアルファは取引停止中のため、別の仕入先で再検討してください。',
     'HIGH', 'READ', 'PURCHASE_ORDER', 18, '/orders/18', '2024-11-12 10:05:00', '2024-11-12 10:15:00'),

    -- 在庫不足アラート
    (7,  5, 4, 'BOTH', '【在庫警告】Cisco ISR 1100-8P の在庫が不足しています',
     '倉庫管理担当者 各位\n\n以下の商品の在庫が発注点を下回りました。\n\n商品名: Cisco ISR 1100-8P\nSKU: RTR-008002\n現在在庫: 0 台\n発注点: 1 台\n倉庫: 東京メイン倉庫\n\n速やかに補充発注を検討してください。',
     'HIGH', 'SENT', 'PRODUCT', 34, '/inventory?product=34', '2025-01-02 08:00:00', NULL),

    (8,  5, 4, 'BOTH', '【在庫警告】エレコム TK-FDM110TBK の在庫が不足しています（名古屋）',
     '倉庫管理担当者 各位\n\n以下の商品の在庫が発注点を下回りました。\n\n商品名: エレコム TK-FDM110TBK\nSKU: KBD-005001\n現在在庫: 3 個\n発注点: 20 個\n倉庫: 名古屋配送センター\n\n速やかに補充発注を検討してください。',
     'NORMAL', 'SENT', 'PRODUCT', 23, '/inventory?product=23&warehouse=3', '2025-01-02 08:00:00', NULL),

    (9,  6, 4, 'BOTH', '【在庫警告】Canon CRG-057H の在庫が不足しています（大阪）',
     '倉庫管理担当者 各位\n\nCanon CRG-057H トナーの在庫が発注点を下回りました。\n\n現在在庫: 1 個\n発注点: 3 個\n倉庫: 大阪支社倉庫',
     'NORMAL', 'READ', 'PRODUCT', 47, '/inventory?product=47&warehouse=2', '2025-01-02 08:00:00', '2025-01-02 09:15:00'),

    (10, 5, 4, 'BOTH', '【在庫警告】Windows 11 Home の在庫が不足しています',
     '倉庫管理担当者 各位\n\nWindows 11 Home ライセンスの在庫が発注点を下回りました。\n\n現在在庫: 3 ライセンス\n発注点: 5 ライセンス',
     'NORMAL', 'SENT', 'PRODUCT', 43, '/inventory?product=43', '2025-01-06 08:00:00', NULL),

    -- 予算警告
    (11, 2, 5, 'BOTH', '【予算警告】情報システム部 の予算消化率が 80% を超えました',
     '佐藤花子 様\n\n情報システム部 の 2024年度 購買予算の消化率が 80% に達しました。\n\n予算総額: 30,000,000 円\n使用済: 18,500,000 円\n残額: 8,300,000 円\n\n今後の発注計画をご確認ください。',
     'HIGH', 'READ', 'BUDGET', 1, '/budgets/1', '2024-11-15 09:00:00', '2024-11-15 09:30:00'),

    (12, 3, 5, 'BOTH', '【予算警告】調達部 の予算消化率が 75% を超えました',
     '鈴木一郎 様\n\n調達部 の 2024年度 購買予算の消化率が 75% に達しました。\n\n予算総額: 50,000,000 円\n使用済: 32,000,000 円\n残額: 12,500,000 円',
     'NORMAL', 'READ', 'BUDGET', 2, '/budgets/2', '2024-12-01 09:00:00', '2024-12-01 10:00:00'),

    -- 検収完了通知
    (13, 4, NULL, 'IN_APP', '検収完了: GR-2024-000001（情シスPC更新）',
     'PO-2024-000001 の検収が完了しました。全品合格です。',
     'NORMAL', 'READ', 'GOODS_RECEIPT', 1, '/receipts/1', '2024-07-14 14:30:00', '2024-07-14 15:00:00'),

    (14, 4, NULL, 'IN_APP', '検収完了（一部不合格）: GR-2024-000006',
     'PO-2024-000006 の検収結果: 10台中1台が初期不良。返品手続きを進めてください。',
     'HIGH', 'READ', 'GOODS_RECEIPT', 6, '/receipts/6', '2024-10-14 15:30:00', '2024-10-14 15:45:00'),

    -- 契約更新通知
    (15, 3, NULL, 'BOTH', '【契約更新】大塚商会 年間契約の更新期限が近づいています',
     '鈴木一郎 様\n\n大塚商会との年間基本取引契約（CNT-2024-001）の更新期限が60日後に迫っています。\n\n契約期間: 2024-04-01 〜 2025-03-31\n契約金額: 30,000,000 円\n\n更新手続きを開始してください。',
     'HIGH', 'SENT', 'SUPPLIER_CONTRACT', 1, '/contracts/1', '2025-01-15 09:00:00', NULL),

    -- システム通知
    (16, 1, NULL, 'IN_APP', 'システムメンテナンスのお知らせ',
     '2025年1月18日（土）22:00〜翌6:00にシステムメンテナンスを実施します。メンテナンス中はシステムをご利用いただけません。',
     'HIGH', 'DELIVERED', NULL, NULL, NULL, '2025-01-13 10:00:00', NULL),
    (17, 2, NULL, 'IN_APP', 'システムメンテナンスのお知らせ',
     '2025年1月18日（土）22:00〜翌6:00にシステムメンテナンスを実施します。',
     'HIGH', 'DELIVERED', NULL, NULL, NULL, '2025-01-13 10:00:00', NULL),
    (18, 3, NULL, 'IN_APP', 'システムメンテナンスのお知らせ',
     '2025年1月18日（土）22:00〜翌6:00にシステムメンテナンスを実施します。',
     'HIGH', 'DELIVERED', NULL, NULL, NULL, '2025-01-13 10:00:00', NULL),

    -- 過去の既読通知
    (19, 4, 2, 'BOTH', '【承認完了】発注 PO-2024-000001 が承認されました',
     '高橋優希 様\n\n発注「PO-2024-000001」が承認されました。',
     'NORMAL', 'READ', 'PURCHASE_ORDER', 1, '/orders/1', '2024-07-04 10:05:00', '2024-07-04 10:10:00'),

    (20, 4, 2, 'BOTH', '【承認完了】発注 PO-2024-000002 が承認されました',
     '高橋優希 様\n\n発注「PO-2024-000002」が承認されました。',
     'NORMAL', 'READ', 'PURCHASE_ORDER', 2, '/orders/2', '2024-07-19 09:05:00', '2024-07-19 09:20:00'),

    (21, 4, 2, 'BOTH', '【承認完了】発注 PO-2024-000003 が承認されました',
     '高橋優希 様\n\n発注「PO-2024-000003」が承認されました。',
     'NORMAL', 'READ', 'PURCHASE_ORDER', 3, '/orders/3', '2024-08-06 10:05:00', '2024-08-06 10:30:00'),

    (22, 4, 2, 'BOTH', '【承認完了】発注 PO-2024-000004 が承認されました',
     '高橋優希 様\n\n発注「PO-2024-000004」（M365ライセンス更新）が承認されました。',
     'URGENT', 'READ', 'PURCHASE_ORDER', 4, '/orders/4', '2024-08-15 11:05:00', '2024-08-15 11:10:00'),

    -- 認証期限通知
    (23, 3, NULL, 'BOTH', '【認証期限】リコージャパン ISO 9001 の有効期限が近づいています',
     '鈴木一郎 様\n\nリコージャパンのISO 9001認証の有効期限が2025年1月14日です。更新状況を確認してください。',
     'NORMAL', 'READ', 'SUPPLIER_CERTIFICATION', 4, '/suppliers/2/certifications', '2024-12-15 09:00:00', '2024-12-15 10:00:00'),

    -- インポート完了通知
    (24, 4, NULL, 'IN_APP', 'インポート完了: 商品マスタ一括更新',
     '商品マスタの一括インポートが完了しました。処理結果: 50件成功 / 0件エラー / 2件スキップ',
     'NORMAL', 'READ', 'IMPORT_JOB', 1, '/admin/imports/1', '2024-10-01 15:30:00', '2024-10-01 15:45:00'),

    -- 追加の通知
    (25, 7, 1, 'BOTH', '【承認依頼】購買依頼 PR-2025-000003 の承認をお願いします',
     '山本大輝 様\n\n購買依頼「PR-2025-000003」（大阪支社会議室AV機器更新）の承認依頼が届いています。\n\n金額: 890,000 円',
     'LOW', 'SENT', 'PURCHASE_REQUISITION', 10, '/requisitions/10', '2025-01-14 11:05:00', NULL),

    (26, 8, NULL, 'IN_APP', '経理部予算: 月次レポートが利用可能です',
     '2024年12月の経理部予算執行レポートが生成されました。確認してください。',
     'LOW', 'READ', 'BUDGET', 5, '/reports/budget/5', '2025-01-05 09:00:00', '2025-01-05 10:30:00'),

    (27, 5, NULL, 'IN_APP', '棚卸予定のお知らせ',
     '2025年1月末の定期棚卸を予定しています。対象倉庫: 東京メイン倉庫。詳細は追って連絡します。',
     'NORMAL', 'DELIVERED', NULL, NULL, NULL, '2025-01-10 09:00:00', NULL),

    (28, 6, NULL, 'IN_APP', '入庫予定のお知らせ: PO-2024-000010',
     'PO-2024-000010（年始補充発注）の納品予定日が2025年1月15日です。受入準備をお願いします。',
     'NORMAL', 'READ', 'PURCHASE_ORDER', 10, '/orders/10', '2025-01-13 09:00:00', '2025-01-13 09:15:00'),

    (29, 10, NULL, 'IN_APP', '品質検査依頼: GR-2024-000006 返品品の代替品',
     'Dell Latitude 5540 の代替品が到着予定です。受入時の品質検査をお願いします。',
     'NORMAL', 'READ', 'GOODS_RECEIPT', 6, '/receipts/6', '2024-10-20 09:00:00', '2024-10-20 09:30:00'),

    (30, 9, NULL, 'IN_APP', '年末年始のシステム運用について',
     '年末年始（12/29-1/3）はシステムサポートが休止となります。緊急連絡先は管理者にお問い合わせください。',
     'LOW', 'READ', NULL, NULL, NULL, '2024-12-25 10:00:00', '2024-12-25 14:00:00');

SELECT setval('notification_id_seq', 30);

-- ----------------------------------------------------------------------------
-- 監査ログ（50件）
-- システム操作の監査証跡
-- ----------------------------------------------------------------------------
INSERT INTO audit_log (id, entity_type, entity_id, action, performed_by,
    performed_at, ip_address, user_agent, old_values, new_values,
    changed_fields, request_id, description)
VALUES
    -- ユーザーログイン
    (1,  'user_profile', 1, 'LOGIN',  1, '2025-01-15 09:00:00', '192.168.1.100',
     'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', NULL, NULL, NULL,
     'req-001-login', '田中太郎がログインしました'),
    (2,  'user_profile', 2, 'LOGIN',  2, '2025-01-15 08:30:00', '192.168.1.101',
     'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)', NULL, NULL, NULL,
     'req-002-login', '佐藤花子がログインしました'),
    (3,  'user_profile', 4, 'LOGIN',  4, '2025-01-15 09:15:00', '192.168.1.103',
     'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', NULL, NULL, NULL,
     'req-003-login', '高橋優希がログインしました'),

    -- 購買依頼の作成・提出
    (4,  'purchase_requisition', 8, 'CREATE', 3, '2025-01-10 09:00:00', '192.168.1.102',
     NULL, NULL, '{"status":"DRAFT","total_amount":3800000}', NULL,
     'req-004-create', '購買依頼 PR-2025-000001 を作成しました'),
    (5,  'purchase_requisition', 8, 'STATUS_CHANGE', 3, '2025-01-10 09:00:00', '192.168.1.102',
     NULL, '{"status":"DRAFT"}', '{"status":"SUBMITTED"}', ARRAY['status'],
     'req-005-submit', '購買依頼 PR-2025-000001 を提出しました'),

    (6,  'purchase_requisition', 9, 'CREATE', 2, '2025-01-12 10:00:00', '192.168.1.101',
     NULL, NULL, '{"status":"DRAFT","total_amount":2090000}', NULL,
     'req-006-create', '購買依頼 PR-2025-000002 を作成しました'),
    (7,  'purchase_requisition', 9, 'STATUS_CHANGE', 2, '2025-01-12 10:00:00', '192.168.1.101',
     NULL, '{"status":"DRAFT"}', '{"status":"SUBMITTED"}', ARRAY['status'],
     'req-007-submit', '購買依頼 PR-2025-000002 を提出しました'),

    -- 発注書の作成・提出
    (8,  'purchase_order', 15, 'CREATE', 4, '2025-01-14 09:00:00', '192.168.1.103',
     NULL, NULL, '{"status":"DRAFT","total_amount":4146000}', NULL,
     'req-008-create', '発注書 PO-2025-000004 を作成しました'),
    (9,  'purchase_order', 15, 'STATUS_CHANGE', 4, '2025-01-14 10:00:00', '192.168.1.103',
     NULL, '{"status":"DRAFT"}', '{"status":"SUBMITTED"}', ARRAY['status'],
     'req-009-submit', '発注書 PO-2025-000004 を提出しました'),

    -- 発注の承認
    (10, 'purchase_order', 10, 'APPROVE', 3, '2024-12-13 09:00:00', '192.168.1.102',
     NULL, '{"status":"SUBMITTED"}', '{"status":"APPROVED"}', ARRAY['status'],
     'req-010-approve', '発注書 PO-2024-000010 を承認しました'),
    (11, 'purchase_order', 11, 'APPROVE', 3, '2024-12-19 10:00:00', '192.168.1.102',
     NULL, '{"status":"SUBMITTED"}', '{"status":"APPROVED"}', ARRAY['status'],
     'req-011-approve', '発注書 PO-2024-000011 を承認しました'),

    -- 発注の却下
    (12, 'purchase_order', 18, 'REJECT', 1, '2024-11-12 10:00:00', '192.168.1.100',
     NULL, '{"status":"SUBMITTED"}', '{"status":"REJECTED"}', ARRAY['status'],
     'req-012-reject', '発注書 PO-2024-000012 を却下しました（テクノアルファは取引停止中）'),

    -- 商品マスタの更新
    (13, 'product', 15, 'STATUS_CHANGE', 2, '2024-10-15 10:00:00', '192.168.1.101',
     NULL, '{"status":"ACTIVE"}', '{"status":"DISCONTINUED"}', ARRAY['status'],
     'req-013-update', '商品 ASUS ExpertBook B5 を販売終了に変更しました'),
    (14, 'product', 32, 'STATUS_CHANGE', 2, '2024-11-01 10:00:00', '192.168.1.101',
     NULL, '{"status":"ACTIVE"}', '{"status":"DISCONTINUED"}', ARRAY['status'],
     'req-014-update', '商品 HP Color LaserJet Pro を販売終了に変更しました'),

    -- 仕入先の状態変更
    (15, 'supplier', 12, 'STATUS_CHANGE', 3, '2024-10-20 10:00:00', '192.168.1.102',
     NULL, '{"status":"ACTIVE"}', '{"status":"SUSPENDED"}', ARRAY['status'],
     'req-015-update', '仕入先 テクノアルファ を取引停止にしました'),

    -- 検収操作
    (16, 'goods_receipt', 6, 'CREATE', 6, '2024-10-14 14:00:00', '192.168.2.10',
     NULL, NULL, '{"status":"DRAFT","order_id":6}', NULL,
     'req-016-create', '検収 GR-2024-000006 を作成しました'),
    (17, 'goods_receipt', 6, 'STATUS_CHANGE', 10, '2024-10-14 15:00:00', '192.168.2.11',
     NULL, '{"status":"DRAFT"}', '{"status":"PARTIALLY_ACCEPTED"}', ARRAY['status'],
     'req-017-inspect', '検収 GR-2024-000006: 1台不合格'),

    -- 在庫調整
    (18, 'inventory_item', 8, 'UPDATE', 5, '2024-10-20 16:00:00', '192.168.2.10',
     NULL, '{"quantity_on_hand":11}', '{"quantity_on_hand":10}', ARRAY['quantity_on_hand'],
     'req-018-adjust', '在庫調整: Dell Latitude 5540 棚卸差異 -1台'),

    -- 返品処理
    (19, 'return_to_supplier', 1, 'CREATE', 6, '2024-11-18 10:00:00', '192.168.2.10',
     NULL, NULL, '{"status":"DRAFT","return_reason":"DEFECTIVE"}', NULL,
     'req-019-create', '返品 RT-2024-000001 を作成しました'),
    (20, 'return_to_supplier', 1, 'APPROVE', 3, '2024-11-19 09:00:00', '192.168.1.102',
     NULL, '{"status":"PENDING_APPROVAL"}', '{"status":"APPROVED"}', ARRAY['status'],
     'req-020-approve', '返品 RT-2024-000001 を承認しました'),

    -- 契約関連
    (21, 'supplier_contract', 1, 'CREATE', 4, '2024-03-10 10:00:00', '192.168.1.103',
     NULL, NULL, '{"status":"DRAFT","supplier_id":1}', NULL,
     'req-021-create', '仕入先契約 CNT-2024-001 を作成しました'),
    (22, 'supplier_contract', 1, 'APPROVE', 1, '2024-03-15 10:00:00', '192.168.1.100',
     NULL, '{"status":"PENDING_APPROVAL"}', '{"status":"ACTIVE"}', ARRAY['status'],
     'req-022-approve', '仕入先契約 CNT-2024-001 を承認しました'),

    -- 予算関連
    (23, 'budget', 1, 'CREATE', 8, '2024-03-01 10:00:00', '192.168.1.107',
     NULL, NULL, '{"status":"DRAFT","total_amount":30000000}', NULL,
     'req-023-create', '予算 BDG-2024-IT を作成しました'),
    (24, 'budget', 1, 'APPROVE', 1, '2024-03-10 14:00:00', '192.168.1.100',
     NULL, '{"status":"PENDING_APPROVAL"}', '{"status":"ACTIVE"}', ARRAY['status'],
     'req-024-approve', '予算 BDG-2024-IT を承認しました'),

    -- 仕入先評価
    (25, 'supplier_rating', 21, 'CREATE', 4, '2024-04-15 10:00:00', '192.168.1.103',
     NULL, NULL, '{"supplier_id":12,"overall_score":3.0}', NULL,
     'req-025-create', 'テクノアルファの2024Q1評価を登録しました'),

    -- 価格表関連
    (26, 'price_list', 3, 'CREATE', 4, '2024-09-25 10:00:00', '192.168.1.103',
     NULL, NULL, '{"status":"DRAFT","price_list_type":"PROMOTIONAL"}', NULL,
     'req-026-create', 'Q4キャンペーン価格表を作成しました'),
    (27, 'price_list', 3, 'APPROVE', 3, '2024-09-28 10:00:00', '192.168.1.102',
     NULL, '{"status":"DRAFT"}', '{"status":"ACTIVE"}', ARRAY['status'],
     'req-027-approve', 'Q4キャンペーン価格表を承認しました'),

    -- 商品価格変更
    (28, 'product', 1, 'UPDATE', 4, '2024-10-01 10:00:00', '192.168.1.103',
     NULL, '{"unit_price":145000}', '{"unit_price":148000}', ARRAY['unit_price'],
     'req-028-update', 'Dell OptiPlex 7010 SFF の単価を更新しました'),

    -- ユーザーログアウト
    (29, 'user_profile', 4, 'LOGOUT', 4, '2025-01-14 18:30:00', '192.168.1.103',
     NULL, NULL, NULL, NULL,
     'req-029-logout', '高橋優希がログアウトしました'),

    -- 追加の操作ログ
    (30, 'purchase_order', 1, 'CREATE', 4, '2024-07-03 14:30:00', '192.168.1.103',
     NULL, NULL, '{"status":"DRAFT"}', NULL, 'req-030', 'PO-2024-000001作成'),
    (31, 'purchase_order', 2, 'CREATE', 4, '2024-07-18 12:00:00', '192.168.1.103',
     NULL, NULL, '{"status":"DRAFT"}', NULL, 'req-031', 'PO-2024-000002作成'),
    (32, 'purchase_order', 3, 'CREATE', 4, '2024-08-05 16:00:00', '192.168.1.103',
     NULL, NULL, '{"status":"DRAFT"}', NULL, 'req-032', 'PO-2024-000003作成'),
    (33, 'purchase_order', 4, 'CREATE', 4, '2024-08-15 10:30:00', '192.168.1.103',
     NULL, NULL, '{"status":"DRAFT"}', NULL, 'req-033', 'PO-2024-000004作成'),
    (34, 'purchase_order', 1, 'APPROVE', 1, '2024-07-04 10:00:00', '192.168.1.100',
     NULL, '{"status":"SUBMITTED"}', '{"status":"APPROVED"}', ARRAY['status'],
     'req-034', 'PO-2024-000001承認'),
    (35, 'purchase_order', 2, 'APPROVE', 3, '2024-07-19 09:00:00', '192.168.1.102',
     NULL, '{"status":"SUBMITTED"}', '{"status":"APPROVED"}', ARRAY['status'],
     'req-035', 'PO-2024-000002承認'),
    (36, 'purchase_order', 3, 'APPROVE', 3, '2024-08-06 10:00:00', '192.168.1.102',
     NULL, '{"status":"SUBMITTED"}', '{"status":"APPROVED"}', ARRAY['status'],
     'req-036', 'PO-2024-000003承認'),
    (37, 'purchase_order', 4, 'APPROVE', 1, '2024-08-15 11:00:00', '192.168.1.100',
     NULL, '{"status":"SUBMITTED"}', '{"status":"APPROVED"}', ARRAY['status'],
     'req-037', 'PO-2024-000004承認'),

    -- 検収作成
    (38, 'goods_receipt', 1, 'CREATE', 6, '2024-07-14 14:00:00', '192.168.2.10',
     NULL, NULL, '{"status":"DRAFT"}', NULL, 'req-038', 'GR-2024-000001作成'),
    (39, 'goods_receipt', 2, 'CREATE', 6, '2024-07-28 14:00:00', '192.168.2.10',
     NULL, NULL, '{"status":"DRAFT"}', NULL, 'req-039', 'GR-2024-000002作成'),
    (40, 'goods_receipt', 3, 'CREATE', 6, '2024-09-08 14:00:00', '192.168.2.10',
     NULL, NULL, '{"status":"DRAFT"}', NULL, 'req-040', 'GR-2024-000003作成'),

    -- 商品の作成ログ
    (41, 'product', 10, 'CREATE', 2, '2024-11-01 10:00:00', '192.168.1.101',
     NULL, NULL, '{"sku":"NPC-002005","status":"ACTIVE"}', NULL,
     'req-041', 'MacBook Air 15 M3を商品マスタに登録'),
    (42, 'product', 11, 'CREATE', 2, '2024-11-01 10:00:00', '192.168.1.101',
     NULL, NULL, '{"sku":"NPC-002006","status":"ACTIVE"}', NULL,
     'req-042', 'MacBook Pro 14 M3 Proを商品マスタに登録'),

    -- 仕入先評価の追加
    (43, 'supplier_rating', 22, 'CREATE', 4, '2024-07-15 10:00:00', '192.168.1.103',
     NULL, NULL, '{"supplier_id":12,"overall_score":2.6}', NULL,
     'req-043', 'テクノアルファ2024Q2評価を登録'),
    (44, 'supplier_rating', 23, 'CREATE', 3, '2024-10-15 10:00:00', '192.168.1.102',
     NULL, NULL, '{"supplier_id":12,"overall_score":2.3}', NULL,
     'req-044', 'テクノアルファ2024Q3評価を登録'),

    -- レポート出力
    (45, 'report', 0, 'EXPORT', 8, '2025-01-05 09:00:00', '192.168.1.107',
     NULL, NULL, NULL, NULL,
     'req-045', '2024年12月度 予算執行レポートをエクスポート'),
    (46, 'report', 0, 'EXPORT', 3, '2025-01-06 10:00:00', '192.168.1.102',
     NULL, NULL, NULL, NULL,
     'req-046', '2024年度 仕入先評価サマリーレポートをエクスポート'),

    -- 倉庫関連
    (47, 'warehouse', 1, 'UPDATE', 5, '2024-12-01 09:00:00', '192.168.2.10',
     NULL, '{"operating_hours":"平日 08:00-17:30"}', '{"operating_hours":"平日 08:00-18:00 / 土曜 09:00-15:00"}',
     ARRAY['operating_hours'], 'req-047', '東京メイン倉庫の営業時間を更新'),

    -- システム設定変更
    (48, 'system_configuration', 14, 'UPDATE', 1, '2024-11-01 10:00:00', '192.168.1.100',
     NULL, '{"config_value":"0"}', '{"config_value":"100000"}', ARRAY['config_value'],
     'req-048', '自動承認上限額を100,000円に設定'),

    -- ユーザープロファイル更新
    (49, 'user_profile', 4, 'UPDATE', 4, '2024-10-01 09:00:00', '192.168.1.103',
     NULL, '{"phone":"03-1234-5004"}', '{"phone":"03-1234-5004"}', ARRAY['phone'],
     'req-049', '高橋優希のプロファイルを更新'),

    (50, 'user_profile', 6, 'UPDATE', 5, '2024-11-15 10:00:00', '192.168.2.10',
     NULL, '{"last_login_at":"2024-11-14 07:45:00"}', '{"last_login_at":"2024-11-15 07:45:00"}',
     ARRAY['last_login_at'], 'req-050', '伊藤美香のログイン情報更新');

SELECT setval('audit_log_id_seq', 50);

-- ----------------------------------------------------------------------------
-- インポートジョブ（5件）
-- 過去のCSV/Excel一括インポート処理の記録
-- ----------------------------------------------------------------------------
INSERT INTO import_job (id, job_code, import_type, file_name, file_url,
    file_size_bytes, mime_type, status, total_rows, processed_rows,
    success_rows, error_rows, skipped_rows, progress_pct,
    started_by, started_at, completed_at, options)
VALUES
    (1, 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'PRODUCT',
     '商品マスタ一括登録_2024Q3.xlsx',
     '/uploads/imports/product_2024q3.xlsx',
     245760, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
     'COMPLETED', 52, 52, 50, 0, 2, 100.00,
     4, '2024-10-01 15:00:00', '2024-10-01 15:25:00',
     '{"skip_header": true, "update_existing": false, "encoding": "UTF-8"}'),

    (2, 'b2c3d4e5-f6a7-8901-bcde-f12345678901', 'SUPPLIER',
     '仕入先マスタ更新_2024.csv',
     '/uploads/imports/supplier_2024.csv',
     32768, 'text/csv',
     'COMPLETED', 12, 12, 12, 0, 0, 100.00,
     4, '2024-07-01 10:00:00', '2024-07-01 10:05:00',
     '{"skip_header": true, "delimiter": ",", "encoding": "UTF-8"}'),

    (3, 'c3d4e5f6-a7b8-9012-cdef-123456789012', 'PRICE_LIST',
     '標準価格表2024_一括登録.xlsx',
     '/uploads/imports/pricelist_2024.xlsx',
     184320, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
     'COMPLETED_WITH_ERRORS', 55, 55, 50, 3, 2, 100.00,
     4, '2024-04-01 09:00:00', '2024-04-01 09:30:00',
     '{"skip_header": true, "update_existing": true}'),

    (4, 'd4e5f6a7-b8c9-0123-defa-234567890123', 'INVENTORY',
     '期初在庫データ_2024H2.csv',
     '/uploads/imports/inventory_2024h2.csv',
     65536, 'text/csv',
     'COMPLETED', 80, 80, 80, 0, 0, 100.00,
     6, '2024-07-01 08:30:00', '2024-07-01 09:00:00',
     '{"skip_header": true, "delimiter": ",", "encoding": "Shift_JIS"}'),

    (5, 'e5f6a7b8-c9d0-1234-efab-345678901234', 'PRODUCT',
     '商品マスタ追加_テスト.csv',
     '/uploads/imports/product_test.csv',
     8192, 'text/csv',
     'FAILED', 10, 3, 0, 3, 0, 30.00,
     4, '2024-12-20 14:00:00', '2024-12-20 14:02:00',
     '{"skip_header": true, "update_existing": false}');

SELECT setval('import_job_id_seq', 5);
