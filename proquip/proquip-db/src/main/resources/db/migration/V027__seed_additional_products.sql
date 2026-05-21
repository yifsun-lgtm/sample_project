-- ============================================================================
-- ProQuip - Enterprise Procurement & Inventory Management System
-- V027: 追加商品データのシード投入
-- 追加商品、追加仕様、商品変更ログ、商品ドキュメント
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 追加商品（50件、ID=51〜100）
-- モニター、プリンター、ネットワーク、アクセサリ、ソフトウェアを追加し
-- 合計商品数を約100件にする
-- ----------------------------------------------------------------------------
INSERT INTO product (id, sku, name, name_en, description, category_id, manufacturer_id, unit_id,
    unit_price, currency_code, status, min_order_qty, reorder_point, reorder_qty, lead_time_days,
    weight_kg, model_number, is_serialized)
VALUES
    -- 追加ノートPC（5件）
    (51, 'NPC-002011', 'Dell Latitude 7440',               'Dell Latitude 7440',
     'Intel Core i7-1365U搭載 14型プレミアムビジネスノート。メモリ16GB、SSD 512GB。軽量1.21kg。',
     8, 1, 2, 248000.00, 'JPY', 'ACTIVE', 1, 2, 5, 7, 1.21, 'LATITUDE-7440', TRUE),

    (52, 'NPC-002012', 'HP EliteBook 1040 G10',             'HP EliteBook 1040 G10',
     'Intel Core i7-1365U搭載 14型プレミアムノート。メモリ32GB、SSD 1TB。Sure View対応。',
     8, 2, 2, 320000.00, 'JPY', 'ACTIVE', 1, 1, 3, 7, 1.24, 'ELITEBOOK-1040-G10', TRUE),

    (53, 'NPC-002013', 'Lenovo ThinkPad T14s Gen 4',        'Lenovo ThinkPad T14s Gen 4',
     'Intel Core i7-1360P搭載 14型スリムノート。メモリ16GB、SSD 512GB。1.22kgの軽量モデル。',
     8, 3, 2, 225000.00, 'JPY', 'ACTIVE', 1, 3, 5, 5, 1.22, 'THINKPAD-T14S-G4', TRUE),

    (54, 'NPC-002014', 'Lenovo ThinkPad E14 Gen 5',          'Lenovo ThinkPad E14 Gen 5',
     'Intel Core i5-1335U搭載 14型エントリーノート。メモリ8GB、SSD 256GB。コスト最優先モデル。',
     8, 3, 2, 105000.00, 'JPY', 'ACTIVE', 1, 5, 10, 5, 1.58, 'THINKPAD-E14-G5', TRUE),

    (55, 'NPC-002015', 'Dell Inspiron 14 5430',               'Dell Inspiron 14 5430',
     'Intel Core i5-1340P搭載 14型スタンダードノート。メモリ16GB、SSD 512GB。個人利用にも適する。',
     8, 1, 2, 125000.00, 'JPY', 'ACTIVE', 1, 3, 5, 5, 1.59, 'INSPIRON-14-5430', TRUE),

    -- 追加デスクトップPC（3件）
    (56, 'DPC-001006', 'Lenovo ThinkCentre M75q Gen 2',      'Lenovo ThinkCentre M75q Gen 2',
     'AMD Ryzen 5 PRO 5650GE搭載 ミニPC。メモリ8GB、SSD 256GB。超小型でデスク省スペース。',
     7, 3, 2, 85000.00, 'JPY', 'ACTIVE', 1, 3, 5, 5, 1.32, 'M75Q-GEN2', TRUE),

    (57, 'DPC-001007', 'Apple Mac mini M2',                   'Apple Mac mini M2',
     'Apple M2チップ搭載 デスクトップPC。メモリ8GB、SSD 256GB。超コンパクトな高性能機。',
     7, 4, 2, 84800.00, 'JPY', 'ACTIVE', 1, 2, 3, 3, 1.18, 'MACMINI-M2', TRUE),

    (58, 'DPC-001008', 'HP ProDesk 405 G8 DM',                'HP ProDesk 405 G8 DM',
     'AMD Ryzen 5 5600GE搭載 デスクトップミニPC。メモリ8GB、SSD 256GB。省電力設計。',
     7, 2, 2, 78000.00, 'JPY', 'ACTIVE', 1, 3, 5, 5, 1.41, 'PRODESK-405-G8', TRUE),

    -- 追加モニター（5件）
    (59, 'MON-004006', 'Dell P2723QE',                        'Dell P2723QE',
     '27型 4K UHD(3840x2160) IPS液晶モニター。USB-C PD 65W。コストパフォーマンスモデル。',
     10, 1, 2, 52000.00, 'JPY', 'ACTIVE', 1, 5, 10, 7, 6.21, 'P2723QE', TRUE),

    (60, 'MON-004007', 'EIZO FlexScan EV3240X',               'EIZO FlexScan EV3240X',
     '31.5型 4K UHD(3840x2160) IPS液晶モニター。USB-C PD 94W。広画面プレミアムモデル。',
     10, 7, 2, 158000.00, 'JPY', 'ACTIVE', 1, 1, 2, 7, 11.50, 'EV3240X', TRUE),

    (61, 'MON-004008', 'Dell U3423WE',                         'Dell U3423WE',
     '34型 WQHD(3440x1440) 曲面IPS液晶ウルトラワイドモニター。USB-C PD 65W。',
     10, 1, 2, 92000.00, 'JPY', 'ACTIVE', 1, 1, 3, 7, 8.60, 'U3423WE', TRUE),

    (62, 'MON-004009', 'HP E24mv G4 FHD カンファレンスモニター',
     'HP E24mv G4 FHD Conference Monitor',
     '23.8型 FHD(1920x1080) IPS。内蔵カメラ・マイク・スピーカー搭載。Web会議特化モデル。',
     10, 2, 2, 48000.00, 'JPY', 'ACTIVE', 1, 3, 5, 5, 6.80, 'E24MV-G4', TRUE),

    (63, 'MON-004010', 'Lenovo ThinkVision T24i-30',           'Lenovo ThinkVision T24i-30',
     '23.8型 FHD(1920x1080) IPS液晶モニター。USB-C PD 75W。エントリービジネスモデル。',
     10, 3, 2, 32000.00, 'JPY', 'ACTIVE', 1, 8, 15, 5, 5.10, 'T24I-30', TRUE),

    -- 追加プリンター（3件）
    (64, 'PRT-007005', 'Brother HL-L2375DW',                   'Brother HL-L2375DW',
     'モノクロレーザープリンター。A4対応。自動両面印刷。無線LAN対応。個人デスク向け。',
     13, 6, 2, 18000.00, 'JPY', 'ACTIVE', 1, 5, 10, 3, 7.20, 'HL-L2375DW', TRUE),

    (65, 'PRT-007006', 'Canon SATERA LBP322i',                 'Canon SATERA LBP322i',
     'モノクロレーザープリンター。A4対応。高速印刷43枚/分。大量印刷向け。',
     13, 10, 2, 45000.00, 'JPY', 'ACTIVE', 1, 2, 3, 5, 10.20, 'LBP322I', TRUE),

    (66, 'PRT-007007', 'Epson EW-M873T',                       'Epson EW-M873T',
     'カラーインクジェット複合機。A4対応。エコタンク搭載で低ランニングコスト。写真印刷対応。',
     13, 11, 2, 42000.00, 'JPY', 'ACTIVE', 1, 2, 3, 5, 8.60, 'EW-M873T', TRUE),

    -- 追加ネットワーク機器（4件）
    (67, 'SWT-009004', 'Cisco Catalyst 1200-8P-E-2G',          'Cisco Catalyst 1200-8P-E-2G',
     '8ポート ギガビット PoEスマートスイッチ。2つのSFPアップリンク。小規模オフィス向け。',
     15, 5, 2, 42000.00, 'JPY', 'ACTIVE', 1, 3, 5, 14, 1.50, 'C1200-8P-E-2G', TRUE),

    (68, 'WAP-010003', 'Buffalo WAPM-AX4R',                    'Buffalo WAPM-AX4R',
     '法人向けWi-Fi 6対応無線LANアクセスポイント。デュアルバンド。コストパフォーマンスモデル。',
     16, 9, 2, 35000.00, 'JPY', 'ACTIVE', 1, 3, 5, 5, 0.44, 'WAPM-AX4R', TRUE),

    (69, 'RTR-008003', 'Buffalo VR-U500X',                     'Buffalo VR-U500X',
     '法人向けVPNルーター。10Gbps対応。最大50拠点のVPN接続。中小企業向け。',
     14, 9, 2, 85000.00, 'JPY', 'ACTIVE', 1, 2, 3, 5, 1.80, 'VR-U500X', TRUE),

    (70, 'SWT-009005', 'Buffalo BS-GS2008P',                   'Buffalo BS-GS2008P',
     '8ポート ギガビットL2スマートスイッチ。PoE給電対応。コンパクト設計。',
     15, 9, 2, 28000.00, 'JPY', 'ACTIVE', 1, 5, 10, 5, 1.10, 'BS-GS2008P', TRUE),

    -- 追加ストレージ（3件）
    (71, 'SSD-011003', 'Samsung 870 EVO 500GB',                'Samsung 870 EVO 500GB',
     '2.5インチ SATA III SSD。500GB容量。読取560MB/s、書込530MB/s。5年保証。',
     18, NULL, 1, 7800.00, 'JPY', 'ACTIVE', 1, 10, 30, 3, 0.054, '870-EVO-500GB', FALSE),

    (72, 'HDD-011004', 'Western Digital WD Blue 2TB',           'WD Blue 2TB',
     '3.5インチ SATA III HDD。2TB容量。7200rpm。バックアップ・アーカイブ用途。',
     17, NULL, 1, 8500.00, 'JPY', 'ACTIVE', 1, 5, 10, 5, 0.45, 'WD20EZBX', FALSE),

    (73, 'HDD-011005', 'Seagate IronWolf 4TB NAS用',           'Seagate IronWolf 4TB',
     '3.5インチ SATA III NAS用HDD。4TB容量。5400rpm。24時間365日稼働設計。3年保証。',
     17, NULL, 1, 14800.00, 'JPY', 'ACTIVE', 1, 3, 5, 7, 0.60, 'ST4000VN006', FALSE),

    -- 追加ソフトウェア（4件）
    (74, 'SFT-013003', 'Microsoft Visio Plan 2',                'Microsoft Visio Plan 2',
     'Microsoft Visio オンライン版年間サブスクリプション。フロー図・組織図作成ツール。',
     20, 15, 6, 10800.00, 'JPY', 'ACTIVE', 1, 5, 10, 1, NULL, 'VISIO-P2-1YR', FALSE),

    (75, 'SFT-013004', 'Microsoft Project Plan 3',              'Microsoft Project Plan 3',
     'Microsoft Project オンライン版年間サブスクリプション。プロジェクト管理ツール。',
     20, 15, 6, 16320.00, 'JPY', 'ACTIVE', 1, 3, 5, 1, NULL, 'PROJECT-P3-1YR', FALSE),

    (76, 'SFT-012003', 'Red Hat Enterprise Linux Server',       'Red Hat Enterprise Linux Server',
     'RHEL サーバー向け年間サブスクリプション。Standard サポート付き。1ソケット/2ソケット。',
     19, NULL, 6, 120000.00, 'JPY', 'ACTIVE', 1, 2, 5, 5, NULL, 'RHEL-SVR-STD', FALSE),

    (77, 'SFT-014001', 'ESET PROTECT Entry',                    'ESET PROTECT Entry',
     'エンドポイントセキュリティ年間サブスクリプション。ウイルス対策・ファイアウォール。1端末。',
     5, NULL, 6, 4800.00, 'JPY', 'ACTIVE', 10, 50, 100, 3, NULL, 'ESET-PE-1YR', FALSE),

    -- 追加サプライ品（8件）
    (78, 'TNR-014003', 'Brother TN-493C トナーカートリッジ（シアン）',
     'Brother TN-493C Cyan Toner',
     'Brother MFC-L3780CDW用 純正トナーカートリッジ（シアン）。印刷可能枚数 約4,000枚。',
     6, 6, 1, 9500.00, 'JPY', 'ACTIVE', 1, 5, 10, 3, 0.35, 'TN-493C', FALSE),

    (79, 'TNR-014004', 'Brother TN-493M トナーカートリッジ（マゼンタ）',
     'Brother TN-493M Magenta Toner',
     'Brother MFC-L3780CDW用 純正トナーカートリッジ（マゼンタ）。印刷可能枚数 約4,000枚。',
     6, 6, 1, 9500.00, 'JPY', 'ACTIVE', 1, 5, 10, 3, 0.35, 'TN-493M', FALSE),

    (80, 'TNR-014005', 'Brother TN-493Y トナーカートリッジ（イエロー）',
     'Brother TN-493Y Yellow Toner',
     'Brother MFC-L3780CDW用 純正トナーカートリッジ（イエロー）。印刷可能枚数 約4,000枚。',
     6, 6, 1, 9500.00, 'JPY', 'ACTIVE', 1, 5, 10, 3, 0.35, 'TN-493Y', FALSE),

    (81, 'TNR-014006', 'Canon CRG-057 トナーカートリッジ（標準）',
     'Canon CRG-057 Standard Toner',
     'Canon SATERA MF753Cdw用 純正トナーカートリッジ（黒・標準容量）。印刷可能枚数 約3,100枚。',
     6, 10, 1, 9800.00, 'JPY', 'ACTIVE', 1, 5, 10, 5, 0.60, 'CRG-057', FALSE),

    (82, 'CBL-015004', 'エレコム USB-C to USB-A ケーブル 1m',
     'ELECOM USB-C to USB-A Cable 1m',
     'USB Type-C to Type-A ケーブル 1m。USB 3.2 Gen1対応。データ転送・充電両用。',
     6, 8, 3, 1280.00, 'JPY', 'ACTIVE', 5, 30, 50, 3, 0.03, 'USB3-AFCM10NBK', FALSE),

    (83, 'CBL-015005', 'エレコム DisplayPort ケーブル 2m',
     'ELECOM DisplayPort Cable 2m',
     'DisplayPort 1.4 ケーブル 2m。8K/60Hz、4K/120Hz対応。ロック機構付き。',
     6, 8, 3, 2180.00, 'JPY', 'ACTIVE', 5, 15, 30, 3, 0.06, 'CAC-DP14BK20', FALSE),

    (84, 'SUP-016001', 'エレコム OAクリーナー ウェットティッシュ 80枚',
     'ELECOM OA Cleaner Wet Tissue 80pcs',
     'PC・OA機器用ウェットクリーニングティッシュ。除菌タイプ。80枚入りボトル。',
     6, 8, 1, 550.00, 'JPY', 'ACTIVE', 10, 20, 50, 3, 0.35, 'WC-AL80N', FALSE),

    (85, 'SUP-016002', 'エレコム セキュリティワイヤーロック',
     'ELECOM Security Wire Lock',
     'ノートPC用ワイヤーロック。ダイヤル式4桁。ケーブル長1.5m。盗難防止用。',
     6, 8, 1, 2200.00, 'JPY', 'ACTIVE', 5, 10, 20, 3, 0.15, 'ESL-12R', FALSE),

    -- 追加アクセサリ（7件）
    (86, 'ACC-017001', 'エレコム ノートPCスタンド',
     'ELECOM Laptop Stand',
     '折りたたみ式ノートPCスタンド。アルミ合金製。角度調整可能。放熱性向上。',
     2, 8, 1, 4500.00, 'JPY', 'ACTIVE', 1, 10, 20, 3, 0.28, 'PCA-LTST8BK', FALSE),

    (87, 'ACC-017002', 'エレコム ヘッドセット HS-HP30UBK',
     'ELECOM Headset HS-HP30UBK',
     'USB接続ヘッドセット。ノイズキャンセリングマイク搭載。Web会議用。軽量設計。',
     2, 8, 1, 3800.00, 'JPY', 'ACTIVE', 1, 15, 30, 3, 0.16, 'HS-HP30UBK', FALSE),

    (88, 'ACC-017003', 'Lenovo ThinkPad USB-C ドック',
     'Lenovo ThinkPad USB-C Dock',
     'USB-C接続ドッキングステーション。デュアルモニター出力。USB-A x3、USB-C x2。GbE。',
     2, 3, 1, 32000.00, 'JPY', 'ACTIVE', 1, 5, 10, 7, 0.38, '40B50090JP', FALSE),

    (89, 'ACC-017004', 'Dell WD19TBS Thunderbolt ドック',
     'Dell WD19TBS Thunderbolt Dock',
     'Thunderbolt 4接続ドッキングステーション。トリプルモニター出力。130W PD対応。',
     2, 1, 1, 38000.00, 'JPY', 'ACTIVE', 1, 3, 5, 7, 0.62, 'WD19TBS', FALSE),

    (90, 'ACC-017005', 'エレコム USBハブ 4ポート',
     'ELECOM USB Hub 4-port',
     'USB 3.2 Gen1対応 4ポートUSBハブ。バスパワー駆動。コンパクト設計。',
     2, 8, 1, 2800.00, 'JPY', 'ACTIVE', 1, 15, 30, 3, 0.05, 'U3H-T405BBK', FALSE),

    (91, 'ACC-017006', 'ロジクール C930e ビジネスウェブカメラ',
     'Logitech C930e Business Webcam',
     'Full HD 1080p ビジネスウェブカメラ。広角90度。H.264ハードウェアエンコード。',
     2, NULL, 1, 15800.00, 'JPY', 'ACTIVE', 1, 5, 10, 5, 0.16, 'C930E', FALSE),

    (92, 'ACC-017007', 'Jabra Speak2 75 スピーカーフォン',
     'Jabra Speak2 75 Speakerphone',
     'ポータブルUSB/Bluetooth会議用スピーカーフォン。最大12名の会議に対応。フルバンドオーディオ。',
     2, NULL, 1, 45000.00, 'JPY', 'ACTIVE', 1, 2, 5, 7, 0.40, 'SPEAK2-75', FALSE),

    -- NAS（1件）
    (93, 'NAS-018001', 'Synology DiskStation DS923+',          'Synology DS923+',
     '4ベイNASサーバー。AMD Ryzen搭載。メモリ4GB（最大32GB）。法人向け共有ストレージ。',
     4, NULL, 2, 78000.00, 'JPY', 'ACTIVE', 1, 1, 2, 10, 2.24, 'DS923+', TRUE),

    -- 追加ソフトウェア（3件）
    (94, 'SFT-014002', 'Adobe Creative Cloud コンプリート',      'Adobe Creative Cloud Complete',
     'Adobe Creative Cloud 全アプリケーション年間サブスクリプション。1ライセンス。法人版。',
     5, NULL, 6, 86880.00, 'JPY', 'ACTIVE', 1, 2, 5, 3, NULL, 'CC-COMPLETE-1YR', FALSE),

    (95, 'SFT-014003', 'Slack Business+',                       'Slack Business+',
     'Slack Business+プラン 年間サブスクリプション。1ユーザー。高度なセキュリティ・コンプライアンス。',
     5, NULL, 6, 21000.00, 'JPY', 'ACTIVE', 10, 20, 50, 1, NULL, 'SLACK-BP-1YR', FALSE),

    (96, 'SFT-014004', 'Zoom Workplace Business',               'Zoom Workplace Business',
     'Zoom ビジネスプラン 年間サブスクリプション。1ライセンス。最大300名参加。クラウド録画10GB。',
     5, NULL, 6, 33000.00, 'JPY', 'ACTIVE', 10, 10, 30, 1, NULL, 'ZOOM-BIZ-1YR', FALSE),

    -- UPS（1件）
    (97, 'UPS-019001', 'APC Smart-UPS SMT1500J',                'APC Smart-UPS SMT1500J',
     '無停電電源装置 1500VA/1000W。タワー型。サーバー・ネットワーク機器用。LCD管理パネル。',
     2, NULL, 2, 128000.00, 'JPY', 'ACTIVE', 1, 1, 2, 10, 22.00, 'SMT1500J', TRUE),

    -- タブレット（2件）
    (98, 'TAB-020001', 'Apple iPad Air M2 11インチ WiFi 256GB',  'Apple iPad Air M2 11-inch WiFi 256GB',
     'Apple M2チップ搭載 11型タブレット。WiFiモデル。256GBストレージ。法人利用向け。',
     1, 4, 2, 98800.00, 'JPY', 'ACTIVE', 1, 2, 5, 3, 0.46, 'IPAD-AIR-M2-256', TRUE),

    (99, 'TAB-020002', 'Lenovo Tab P12 Pro',                     'Lenovo Tab P12 Pro',
     'Snapdragon 870搭載 12.6型Android タブレット。メモリ8GB、SSD 256GB。ペン・キーボード対応。',
     1, 3, 2, 75000.00, 'JPY', 'INACTIVE', 1, 0, 0, 10, 0.56, 'TAB-P12-PRO', TRUE),

    -- その他周辺機器（1件）
    (100, 'ACC-017008', 'エレコム 電源タップ 6口 雷ガード',
     'ELECOM Power Strip 6-outlet Thunder Guard',
     '6口電源タップ。雷サージ保護。個別スイッチ付き。3mケーブル。マグネット固定対応。',
     2, 8, 1, 2800.00, 'JPY', 'ACTIVE', 1, 10, 20, 3, 0.55, 'T-K5A-2630WH', FALSE);

SELECT setval('product_id_seq', 100);

-- ----------------------------------------------------------------------------
-- 追加商品仕様（100件）
-- 追加した50商品の技術仕様
-- ----------------------------------------------------------------------------
INSERT INTO product_specification (id, product_id, spec_name, spec_value, spec_unit, value_type, numeric_value, sort_order)
VALUES
    -- Dell Latitude 7440 (id=51)
    (101, 51, 'CPU',        'Intel Core i7-1365U',    NULL, 'TEXT', NULL, 1),
    (102, 51, 'メモリ',     '16GB LPDDR5-6400',      NULL, 'TEXT', NULL, 2),
    (103, 51, 'ストレージ', '512GB NVMe SSD',        NULL, 'TEXT', NULL, 3),
    (104, 51, 'ディスプレイ','14型 WUXGA IPS',        NULL, 'TEXT', NULL, 4),
    (105, 51, '重量',       '1.21',                  'kg', 'NUMERIC', 1.21, 5),
    -- HP EliteBook 1040 G10 (id=52)
    (106, 52, 'CPU',        'Intel Core i7-1365U',    NULL, 'TEXT', NULL, 1),
    (107, 52, 'メモリ',     '32GB LPDDR5-6400',      NULL, 'TEXT', NULL, 2),
    (108, 52, 'ストレージ', '1TB NVMe SSD',          NULL, 'TEXT', NULL, 3),
    (109, 52, 'ディスプレイ','14型 WUXGA IPS Sure View', NULL, 'TEXT', NULL, 4),
    (110, 52, '重量',       '1.24',                  'kg', 'NUMERIC', 1.24, 5),
    -- ThinkPad T14s Gen 4 (id=53)
    (111, 53, 'CPU',        'Intel Core i7-1360P',    NULL, 'TEXT', NULL, 1),
    (112, 53, 'メモリ',     '16GB LPDDR5-5200',      NULL, 'TEXT', NULL, 2),
    (113, 53, 'ストレージ', '512GB NVMe SSD',        NULL, 'TEXT', NULL, 3),
    (114, 53, 'ディスプレイ','14型 WUXGA IPS',        NULL, 'TEXT', NULL, 4),
    (115, 53, '重量',       '1.22',                  'kg', 'NUMERIC', 1.22, 5),
    -- ThinkPad E14 Gen 5 (id=54)
    (116, 54, 'CPU',        'Intel Core i5-1335U',    NULL, 'TEXT', NULL, 1),
    (117, 54, 'メモリ',     '8GB DDR4-3200',         NULL, 'TEXT', NULL, 2),
    (118, 54, 'ストレージ', '256GB NVMe SSD',        NULL, 'TEXT', NULL, 3),
    (119, 54, 'ディスプレイ','14型 FHD IPS',          NULL, 'TEXT', NULL, 4),
    -- ThinkCentre M75q Gen 2 (id=56)
    (120, 56, 'CPU',        'AMD Ryzen 5 PRO 5650GE', NULL, 'TEXT', NULL, 1),
    (121, 56, 'メモリ',     '8GB DDR4-3200',         NULL, 'TEXT', NULL, 2),
    (122, 56, 'ストレージ', '256GB NVMe SSD',        NULL, 'TEXT', NULL, 3),
    (123, 56, '筐体サイズ', '超小型（約1L）',         NULL, 'TEXT', NULL, 4),
    -- Mac mini M2 (id=57)
    (124, 57, 'CPU',        'Apple M2 (8コアCPU/10コアGPU)', NULL, 'TEXT', NULL, 1),
    (125, 57, 'メモリ',     '8GB ユニファイドメモリ', NULL, 'TEXT', NULL, 2),
    (126, 57, 'ストレージ', '256GB SSD',             NULL, 'TEXT', NULL, 3),
    (127, 57, 'インターフェース', 'Thunderbolt 4 x2, HDMI, USB-A x2', NULL, 'TEXT', NULL, 4),
    -- Dell P2723QE (id=59)
    (128, 59, '画面サイズ',  '27',                   '型', 'NUMERIC', 27, 1),
    (129, 59, '解像度',      '3840x2160 (4K UHD)',   NULL, 'TEXT', NULL, 2),
    (130, 59, 'パネル種別',  'IPS',                  NULL, 'TEXT', NULL, 3),
    (131, 59, 'USB-C給電',   '65',                   'W',  'NUMERIC', 65, 4),
    -- EIZO FlexScan EV3240X (id=60)
    (132, 60, '画面サイズ',  '31.5',                 '型', 'NUMERIC', 31.5, 1),
    (133, 60, '解像度',      '3840x2160 (4K UHD)',   NULL, 'TEXT', NULL, 2),
    (134, 60, 'パネル種別',  'IPS',                  NULL, 'TEXT', NULL, 3),
    (135, 60, 'USB-C給電',   '94',                   'W',  'NUMERIC', 94, 4),
    -- Dell U3423WE (id=61)
    (136, 61, '画面サイズ',  '34',                   '型', 'NUMERIC', 34, 1),
    (137, 61, '解像度',      '3440x1440 (WQHD)',     NULL, 'TEXT', NULL, 2),
    (138, 61, 'パネル種別',  'IPS (曲面)',           NULL, 'TEXT', NULL, 3),
    (139, 61, 'USB-C給電',   '65',                   'W',  'NUMERIC', 65, 4),
    -- HP E24mv G4 (id=62)
    (140, 62, '画面サイズ',  '23.8',                 '型', 'NUMERIC', 23.8, 1),
    (141, 62, '解像度',      '1920x1080 (FHD)',      NULL, 'TEXT', NULL, 2),
    (142, 62, '内蔵カメラ',  'true',                 NULL, 'BOOLEAN', NULL, 3),
    (143, 62, '内蔵スピーカー', 'true',              NULL, 'BOOLEAN', NULL, 4),
    -- Brother HL-L2375DW (id=64)
    (144, 64, '印刷速度',    '34',                   '枚/分', 'NUMERIC', 34, 1),
    (145, 64, '対応用紙',    'A4',                   NULL, 'TEXT', NULL, 2),
    (146, 64, '自動両面印刷','true',                 NULL, 'BOOLEAN', NULL, 3),
    -- Canon LBP322i (id=65)
    (147, 65, '印刷速度',    '43',                   '枚/分', 'NUMERIC', 43, 1),
    (148, 65, '対応用紙',    'A4',                   NULL, 'TEXT', NULL, 2),
    (149, 65, '給紙容量',    '900',                  '枚', 'NUMERIC', 900, 3),
    -- Cisco Catalyst 1200-8P (id=67)
    (150, 67, 'ポート数',    '8',                    'ポート', 'NUMERIC', 8, 1),
    (151, 67, 'PoE対応',     'true',                 NULL, 'BOOLEAN', NULL, 2),
    (152, 67, 'PoE給電',     '67',                   'W',  'NUMERIC', 67, 3),
    -- Buffalo WAPM-AX4R (id=68)
    (153, 68, 'Wi-Fi規格',   'Wi-Fi 6 (802.11ax)',   NULL, 'TEXT', NULL, 1),
    (154, 68, 'バンド',      'デュアルバンド',       NULL, 'TEXT', NULL, 2),
    (155, 68, '最大接続数',  '30',                   '台', 'NUMERIC', 30, 3),
    -- Samsung 870 EVO 500GB (id=71)
    (156, 71, '容量',        '500',                  'GB', 'NUMERIC', 500, 1),
    (157, 71, 'インターフェース', 'SATA III 6Gbps',  NULL, 'TEXT', NULL, 2),
    (158, 71, '読取速度',    '560',                  'MB/s', 'NUMERIC', 560, 3),
    -- WD Blue 2TB (id=72)
    (159, 72, '容量',        '2000',                 'GB', 'NUMERIC', 2000, 1),
    (160, 72, 'インターフェース', 'SATA III 6Gbps',  NULL, 'TEXT', NULL, 2),
    (161, 72, '回転速度',    '7200',                 'rpm', 'NUMERIC', 7200, 3),
    -- Seagate IronWolf 4TB (id=73)
    (162, 73, '容量',        '4000',                 'GB', 'NUMERIC', 4000, 1),
    (163, 73, 'インターフェース', 'SATA III 6Gbps',  NULL, 'TEXT', NULL, 2),
    (164, 73, '用途',        'NAS専用',              NULL, 'TEXT', NULL, 3),
    -- Synology DS923+ (id=93)
    (165, 93, 'ベイ数',      '4',                    'ベイ', 'NUMERIC', 4, 1),
    (166, 93, 'CPU',         'AMD Ryzen R1600',      NULL, 'TEXT', NULL, 2),
    (167, 93, 'メモリ',      '4GB DDR4 ECC（最大32GB）', NULL, 'TEXT', NULL, 3),
    (168, 93, 'インターフェース', '1GbE x2, eSATA, USB 3.2 x2', NULL, 'TEXT', NULL, 4),
    -- APC Smart-UPS (id=97)
    (169, 97, '容量',        '1500',                 'VA', 'NUMERIC', 1500, 1),
    (170, 97, '出力',        '1000',                 'W',  'NUMERIC', 1000, 2),
    (171, 97, 'バックアップ時間', '約10分（フルロード）', NULL, 'TEXT', NULL, 3),
    -- iPad Air M2 (id=98)
    (172, 98, 'CPU',         'Apple M2',             NULL, 'TEXT', NULL, 1),
    (173, 98, 'ストレージ',  '256GB',               NULL, 'TEXT', NULL, 2),
    (174, 98, 'ディスプレイ','11型 Liquid Retina',   NULL, 'TEXT', NULL, 3),
    (175, 98, '重量',        '0.46',                'kg', 'NUMERIC', 0.46, 4),
    -- ドッキングステーション (id=88)
    (176, 88, '出力',        'HDMI x1, DP x1, USB-C x1', NULL, 'TEXT', NULL, 1),
    (177, 88, 'USB-A',       '3',                   'ポート', 'NUMERIC', 3, 2),
    (178, 88, '有線LAN',     'Gigabit Ethernet',    NULL, 'TEXT', NULL, 3),
    -- Dell WD19TBS (id=89)
    (179, 89, '接続方式',    'Thunderbolt 4',       NULL, 'TEXT', NULL, 1),
    (180, 89, '出力',        'HDMI x1, DP x2, USB-C x1', NULL, 'TEXT', NULL, 2),
    (181, 89, 'PD給電',      '130',                 'W',  'NUMERIC', 130, 3),
    -- エレコム ヘッドセット (id=87)
    (182, 87, '接続方式',    'USB-A',               NULL, 'TEXT', NULL, 1),
    (183, 87, 'マイク',      'ノイズキャンセリング', NULL, 'TEXT', NULL, 2),
    (184, 87, '重量',        '160',                 'g',  'NUMERIC', 160, 3),
    -- Jabra Speak2 75 (id=92)
    (185, 92, '接続方式',    'USB-A / Bluetooth 5.2', NULL, 'TEXT', NULL, 1),
    (186, 92, '対応人数',    '12',                   '名', 'NUMERIC', 12, 2),
    (187, 92, 'バッテリー駆動', '約15時間',          NULL, 'TEXT', NULL, 3),
    -- ロジクール C930e (id=91)
    (188, 91, '解像度',      '1920x1080 (Full HD)',  NULL, 'TEXT', NULL, 1),
    (189, 91, '画角',        '90',                   '度', 'NUMERIC', 90, 2),
    (190, 91, '接続方式',    'USB-A',               NULL, 'TEXT', NULL, 3),
    -- ESET PROTECT (id=77)
    (191, 77, 'ライセンス形態', '年間サブスクリプション', NULL, 'TEXT', NULL, 1),
    (192, 77, '対応OS',      'Windows, macOS, Linux, Android', NULL, 'TEXT', NULL, 2),
    -- Adobe CC (id=94)
    (193, 94, 'ライセンス形態', '年間サブスクリプション（法人VIPプログラム）', NULL, 'TEXT', NULL, 1),
    (194, 94, '含まれるアプリ', 'Photoshop, Illustrator, InDesign, Premiere Pro, After Effects 他', NULL, 'TEXT', NULL, 2),
    -- Slack (id=95)
    (195, 95, 'ライセンス形態', '年間サブスクリプション', NULL, 'TEXT', NULL, 1),
    (196, 95, 'メッセージ保存', '無制限',            NULL, 'TEXT', NULL, 2),
    -- Zoom (id=96)
    (197, 96, 'ライセンス形態', '年間サブスクリプション', NULL, 'TEXT', NULL, 1),
    (198, 96, '最大参加者数',   '300',               '名', 'NUMERIC', 300, 2),
    -- Buffalo VR-U500X (id=69)
    (199, 69, 'VPN接続数',   '50',                   '拠点', 'NUMERIC', 50, 1),
    (200, 69, 'WAN速度',     '10',                   'Gbps', 'NUMERIC', 10, 2);

SELECT setval('product_specification_id_seq', 200);

-- ----------------------------------------------------------------------------
-- 商品変更ログ（30件）
-- 商品マスタの変更履歴
-- ----------------------------------------------------------------------------
INSERT INTO product_change_log (id, product_id, changed_by, change_type, field_name,
    old_value, new_value, change_reason, created_at)
VALUES
    -- 商品作成ログ
    (1,  1,  4, 'CREATE', NULL, NULL, NULL, '初期マスタ登録', '2024-04-01 10:00:00'),
    (2,  6,  4, 'CREATE', NULL, NULL, NULL, '初期マスタ登録', '2024-04-01 10:00:00'),
    (3,  7,  4, 'CREATE', NULL, NULL, NULL, '初期マスタ登録', '2024-04-01 10:00:00'),
    (4,  8,  4, 'CREATE', NULL, NULL, NULL, '初期マスタ登録', '2024-04-01 10:00:00'),
    (5,  18, 4, 'CREATE', NULL, NULL, NULL, '初期マスタ登録', '2024-04-01 10:00:00'),
    (6,  10, 2, 'CREATE', NULL, NULL, NULL, '新製品追加（MacBook Air M3）', '2024-11-01 10:00:00'),
    (7,  11, 2, 'CREATE', NULL, NULL, NULL, '新製品追加（MacBook Pro M3 Pro）', '2024-11-01 10:00:00'),

    -- 価格変更ログ
    (8,  1,  4, 'UPDATE', 'unit_price', '145000', '148000', 'メーカー価格改定に伴う価格更新', '2024-10-01 10:00:00'),
    (9,  6,  4, 'UPDATE', 'unit_price', '195000', '198000', 'メーカー価格改定に伴う価格更新', '2024-10-01 10:00:00'),
    (10, 7,  4, 'UPDATE', 'unit_price', '258000', '265000', 'メーカー価格改定に伴う価格更新', '2024-10-01 10:00:00'),
    (11, 18, 4, 'UPDATE', 'unit_price', '75000',  '78000',  'EIZO価格改定', '2024-10-01 10:00:00'),

    -- ステータス変更ログ
    (12, 15, 2, 'STATUS_CHANGE', 'status', 'ACTIVE', 'DISCONTINUED', '後継モデル発売に伴い販売終了', '2024-10-15 10:00:00'),
    (13, 32, 2, 'STATUS_CHANGE', 'status', 'ACTIVE', 'DISCONTINUED', '後継モデル MFP 4303fdn 発売に伴い販売終了', '2024-11-01 10:00:00'),
    (14, 5,  2, 'STATUS_CHANGE', 'status', 'ACTIVE', 'INACTIVE', '在庫消化後に販売終了予定。新規発注停止。', '2024-12-01 10:00:00'),

    -- 発注点・発注量の変更
    (15, 23, 4, 'UPDATE', 'reorder_point', '15', '20', '消費ペースの増加に伴い発注点を引き上げ', '2024-09-01 10:00:00'),
    (16, 26, 4, 'UPDATE', 'reorder_point', '15', '20', '消費ペースの増加に伴い発注点を引き上げ', '2024-09-01 10:00:00'),
    (17, 19, 4, 'UPDATE', 'reorder_qty',   '10', '15', '大口発注による在庫効率化', '2024-10-01 10:00:00'),
    (18, 48, 4, 'UPDATE', 'reorder_point', '20', '30', 'ケーブル需要増に伴い発注点変更', '2024-11-01 10:00:00'),

    -- リードタイム変更
    (19, 33, 4, 'UPDATE', 'lead_time_days', '21', '14', 'Cisco供給状況改善に伴うリードタイム短縮', '2024-09-01 10:00:00'),
    (20, 35, 4, 'UPDATE', 'lead_time_days', '14', '10', '在庫安定化に伴うリードタイム短縮', '2024-10-01 10:00:00'),

    -- 追加商品の作成ログ
    (21, 51, 4, 'CREATE', NULL, NULL, NULL, 'Dell Latitude 7440をカタログに追加', '2024-12-01 10:00:00'),
    (22, 52, 4, 'CREATE', NULL, NULL, NULL, 'HP EliteBook 1040 G10をカタログに追加', '2024-12-01 10:00:00'),
    (23, 53, 4, 'CREATE', NULL, NULL, NULL, 'ThinkPad T14s Gen 4をカタログに追加', '2024-12-01 10:00:00'),
    (24, 56, 4, 'CREATE', NULL, NULL, NULL, 'ThinkCentre M75q Gen 2をカタログに追加', '2024-12-01 10:00:00'),
    (25, 57, 4, 'CREATE', NULL, NULL, NULL, 'Mac mini M2をカタログに追加', '2024-12-01 10:00:00'),
    (26, 93, 2, 'CREATE', NULL, NULL, NULL, 'Synology DS923+をカタログに追加', '2025-01-06 10:00:00'),
    (27, 97, 2, 'CREATE', NULL, NULL, NULL, 'APC Smart-UPS SMT1500Jをカタログに追加', '2025-01-06 10:00:00'),
    (28, 98, 2, 'CREATE', NULL, NULL, NULL, 'iPad Air M2をカタログに追加', '2025-01-10 10:00:00'),

    -- 説明文の更新
    (29, 44, 4, 'UPDATE', 'description',
     'Microsoft 365 法人向け年間サブスクリプション。Word/Excel/PowerPoint/Teams含む。',
     'Microsoft 365 法人向け年間サブスクリプション。Word/Excel/PowerPoint/Teams/OneDrive含む。',
     '説明文にOneDriveを追記', '2024-08-01 10:00:00'),

    (30, 99, 2, 'STATUS_CHANGE', 'status', 'ACTIVE', 'INACTIVE', '後継モデル発表に伴い取扱い停止', '2025-01-10 10:00:00');

SELECT setval('product_change_log_id_seq', 30);

-- ----------------------------------------------------------------------------
-- 商品ドキュメント（15件）
-- 商品に関連する技術文書
-- ----------------------------------------------------------------------------
INSERT INTO product_document (id, product_id, document_name, document_type,
    file_url, file_size_bytes, mime_type, language, document_version, uploaded_by)
VALUES
    (1,  1,  'Dell OptiPlex 7010 SFF 仕様書',     'DATASHEET',
     '/documents/products/dell-optiplex-7010-sff-spec.pdf', 2456789, 'application/pdf', 'ja', '2024.1', 4),
    (2,  6,  'HP EliteBook 840 G10 製品カタログ',  'BROCHURE',
     '/documents/products/hp-elitebook-840-g10-brochure.pdf', 3567890, 'application/pdf', 'ja', '2024.1', 4),
    (3,  7,  'ThinkPad X1 Carbon Gen 11 仕様書',   'DATASHEET',
     '/documents/products/lenovo-x1carbon-gen11-spec.pdf', 1890123, 'application/pdf', 'ja', '2024.1', 4),
    (4,  7,  'ThinkPad X1 Carbon Gen 11 ユーザーガイド', 'MANUAL',
     '/documents/products/lenovo-x1carbon-gen11-manual.pdf', 8901234, 'application/pdf', 'ja', '1.0', 4),
    (5,  18, 'EIZO FlexScan EV2760 仕様書',        'DATASHEET',
     '/documents/products/eizo-ev2760-spec.pdf', 1234567, 'application/pdf', 'ja', '2024.1', 4),
    (6,  29, 'Brother MFC-L3780CDW セットアップガイド', 'MANUAL',
     '/documents/products/brother-mfc-l3780cdw-setup.pdf', 4567890, 'application/pdf', 'ja', '1.0', 4),
    (7,  33, 'Cisco ISR 1100 シリーズ データシート', 'DATASHEET',
     '/documents/products/cisco-isr1100-datasheet.pdf', 2345678, 'application/pdf', 'ja', '2024.2', 4),
    (8,  35, 'Cisco Catalyst 1300 導入ガイド',      'MANUAL',
     '/documents/products/cisco-catalyst1300-install-guide.pdf', 5678901, 'application/pdf', 'ja', '1.1', 4),
    (9,  44, 'Microsoft 365 ライセンスガイド',       'BROCHURE',
     '/documents/products/m365-licensing-guide.pdf', 3456789, 'application/pdf', 'ja', '2024.10', 4),
    (10, 38, 'Cisco Meraki MR36 設置マニュアル',     'MANUAL',
     '/documents/products/cisco-meraki-mr36-install.pdf', 2678901, 'application/pdf', 'ja', '1.0', 4),
    (11, 10, 'MacBook Air 15 M3 技術仕様',          'DATASHEET',
     '/documents/products/apple-mba15-m3-spec.pdf', 1567890, 'application/pdf', 'ja', '2024.1', 2),
    (12, 93, 'Synology DS923+ 製品仕様書',           'DATASHEET',
     '/documents/products/synology-ds923plus-spec.pdf', 2890123, 'application/pdf', 'ja', '2024.1', 2),
    (13, 97, 'APC Smart-UPS SMT1500J 仕様書',       'DATASHEET',
     '/documents/products/apc-smt1500j-spec.pdf', 1789012, 'application/pdf', 'ja', '2024.1', 2),
    (14, 40, 'Samsung 870 EVO 製品保証書',            'WARRANTY',
     '/documents/products/samsung-870evo-warranty.pdf', 456789, 'application/pdf', 'ja', '1.0', 4),
    (15, 77, 'ESET PROTECT Entry 導入ガイド',         'MANUAL',
     '/documents/products/eset-protect-entry-guide.pdf', 5123456, 'application/pdf', 'ja', '7.0', 2);

SELECT setval('product_document_id_seq', 15);
