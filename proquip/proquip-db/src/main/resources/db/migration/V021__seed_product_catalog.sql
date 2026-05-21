-- ============================================================================
-- ProQuip - Enterprise Procurement & Inventory Management System
-- V021: 商品カタログのシード投入
-- メーカー、カテゴリ、商品、仕様、タグ、代替品、バンドル
-- ============================================================================

-- ----------------------------------------------------------------------------
-- メーカーマスタ（15件）
-- IT機器の主要メーカー
-- ----------------------------------------------------------------------------
INSERT INTO manufacturer (id, manufacturer_code, name, name_en, website, country, description, is_active)
VALUES
    (1,  'MFR-DELL',   'Dell Technologies', 'Dell Technologies',     'https://www.dell.com',       'USA', 'PC・サーバー・ストレージの大手メーカー', TRUE),
    (2,  'MFR-HP',     'HP Inc.',           'HP Inc.',               'https://www.hp.com',         'USA', 'PC・プリンター大手メーカー', TRUE),
    (3,  'MFR-LENOVO', 'Lenovo',            'Lenovo',                'https://www.lenovo.com',     'CHN', 'ThinkPadシリーズで知られるPC大手', TRUE),
    (4,  'MFR-APPLE',  'Apple',             'Apple Inc.',            'https://www.apple.com',      'USA', 'Mac・iPadの開発・製造メーカー', TRUE),
    (5,  'MFR-CISCO',  'Cisco Systems',     'Cisco Systems',         'https://www.cisco.com',      'USA', 'ネットワーク機器の世界最大手', TRUE),
    (6,  'MFR-BROTHER','ブラザー工業',       'Brother Industries',    'https://www.brother.co.jp',  'JPN', 'プリンター・複合機メーカー', TRUE),
    (7,  'MFR-EIZO',   'EIZO',              'EIZO Corporation',      'https://www.eizo.co.jp',     'JPN', '高品質モニターの専業メーカー', TRUE),
    (8,  'MFR-ELECOM', 'エレコム',           'ELECOM',                'https://www.elecom.co.jp',   'JPN', 'PC周辺機器・アクセサリーメーカー', TRUE),
    (9,  'MFR-BUFFALO','バッファロー',       'Buffalo',               'https://www.buffalo.jp',     'JPN', 'ネットワーク機器・ストレージメーカー', TRUE),
    (10, 'MFR-CANON',  'キヤノン',           'Canon Inc.',            'https://www.canon.co.jp',    'JPN', '複合機・プリンター大手メーカー', TRUE),
    (11, 'MFR-EPSON',  'エプソン',           'Seiko Epson',           'https://www.epson.co.jp',    'JPN', 'プリンター・プロジェクターメーカー', TRUE),
    (12, 'MFR-FUJITSU','富士通',             'Fujitsu',               'https://www.fujitsu.com',    'JPN', '国産PC・サーバーメーカー', TRUE),
    (13, 'MFR-NEC',    'NEC',               'NEC Corporation',       'https://www.nec.com',        'JPN', '国産PC・ネットワーク機器メーカー', TRUE),
    (14, 'MFR-ASUS',   'ASUS',              'ASUSTeK Computer',      'https://www.asus.com',       'TWN', 'マザーボード・PC大手メーカー', TRUE),
    (15, 'MFR-MS',     'Microsoft',         'Microsoft Corporation', 'https://www.microsoft.com',  'USA', 'ソフトウェア・クラウドサービスの最大手', TRUE);

SELECT setval('manufacturer_id_seq', 15);

-- ----------------------------------------------------------------------------
-- カテゴリマスタ（20件）
-- 階層構造を持つ商品分類。ルートカテゴリ（level=0）の下にサブカテゴリ。
-- ----------------------------------------------------------------------------
INSERT INTO category (id, parent_id, category_code, name, name_en, description, level, sort_order, path, is_active)
VALUES
    -- ルートカテゴリ（level=0）
    (1,  NULL, 'CAT-PC',      'コンピュータ',  'Computers',         'デスクトップ・ノートPC・ワークステーション',    0, 1,  '/1/',       TRUE),
    (2,  NULL, 'CAT-PERIPH',  '周辺機器',      'Peripherals',       'モニター・キーボード・マウス・プリンター',      0, 2,  '/2/',       TRUE),
    (3,  NULL, 'CAT-NET',     'ネットワーク',   'Networking',        'ルーター・スイッチ・アクセスポイント',          0, 3,  '/3/',       TRUE),
    (4,  NULL, 'CAT-STORAGE', 'ストレージ',     'Storage',           'HDD・SSD・NAS',                              0, 4,  '/4/',       TRUE),
    (5,  NULL, 'CAT-SW',      'ソフトウェア',   'Software',          'OS・オフィス・セキュリティソフト',              0, 5,  '/5/',       TRUE),
    (6,  NULL, 'CAT-SUPPLY',  'サプライ品',     'Supplies',          'トナー・ケーブル・消耗品',                    0, 6,  '/6/',       TRUE),
    -- サブカテゴリ: コンピュータ
    (7,  1,    'CAT-DESKTOP', 'デスクトップPC', 'Desktop PC',        '据置型パーソナルコンピュータ',                 1, 1,  '/1/7/',     TRUE),
    (8,  1,    'CAT-LAPTOP',  'ノートPC',      'Laptop',            '携帯型パーソナルコンピュータ',                 1, 2,  '/1/8/',     TRUE),
    (9,  1,    'CAT-WS',      'ワークステーション','Workstation',    '高性能業務用コンピュータ',                     1, 3,  '/1/9/',     TRUE),
    -- サブカテゴリ: 周辺機器
    (10, 2,    'CAT-MONITOR', 'モニター',       'Monitor',           'ディスプレイ・モニター',                      1, 1,  '/2/10/',    TRUE),
    (11, 2,    'CAT-KB',      'キーボード',     'Keyboard',          'キーボード',                                 1, 2,  '/2/11/',    TRUE),
    (12, 2,    'CAT-MOUSE',   'マウス',         'Mouse',             'マウス・トラックボール',                      1, 3,  '/2/12/',    TRUE),
    (13, 2,    'CAT-PRINTER', 'プリンター',     'Printer',           'プリンター・複合機',                          1, 4,  '/2/13/',    TRUE),
    -- サブカテゴリ: ネットワーク
    (14, 3,    'CAT-ROUTER',  'ルーター',       'Router',            'ルーター・ゲートウェイ',                      1, 1,  '/3/14/',    TRUE),
    (15, 3,    'CAT-SWITCH',  'スイッチ',       'Switch',            'L2/L3スイッチ',                              1, 2,  '/3/15/',    TRUE),
    (16, 3,    'CAT-AP',      'アクセスポイント','Access Point',      '無線LANアクセスポイント',                     1, 3,  '/3/16/',    TRUE),
    -- サブカテゴリ: ストレージ
    (17, 4,    'CAT-HDD',     'HDD',            'HDD',              'ハードディスクドライブ',                       1, 1,  '/4/17/',    TRUE),
    (18, 4,    'CAT-SSD',     'SSD',            'SSD',              'ソリッドステートドライブ',                      1, 2,  '/4/18/',    TRUE),
    -- サブカテゴリ: ソフトウェア
    (19, 5,    'CAT-OS',      'OS',             'Operating System',  'オペレーティングシステム',                     1, 1,  '/5/19/',    TRUE),
    (20, 5,    'CAT-OFFICE',  'オフィスソフト',  'Office Software',   'オフィス生産性ソフトウェア',                   1, 2,  '/5/20/',    TRUE);

SELECT setval('category_id_seq', 20);

-- ----------------------------------------------------------------------------
-- 商品マスタ（50件）
-- IT機器の実在製品に近いリアルなデータ。SKUはカテゴリごとの接頭辞を使用。
-- unit_id: 1=個, 2=台, 3=本, 5=セット, 6=ライセンス, 10=ロール
-- ----------------------------------------------------------------------------
INSERT INTO product (id, sku, name, name_en, description, category_id, manufacturer_id, unit_id,
    unit_price, currency_code, status, min_order_qty, reorder_point, reorder_qty, lead_time_days,
    weight_kg, model_number, is_serialized)
VALUES
    -- デスクトップPC（5件）
    (1,  'DPC-001001', 'Dell OptiPlex 7010 SFF',              'Dell OptiPlex 7010 SFF',
     'Intel Core i5-13500T搭載 省スペース型デスクトップPC。メモリ16GB、SSD 512GB。法人向けスタンダードモデル。',
     7, 1, 2, 148000.00, 'JPY', 'ACTIVE', 1, 5, 10, 7, 5.00, 'OPTIPLEX-7010-SFF', TRUE),

    (2,  'DPC-001002', 'Dell OptiPlex 5000 MT',               'Dell OptiPlex 5000 MT',
     'Intel Core i5-12500搭載 ミニタワー型デスクトップPC。メモリ8GB、SSD 256GB。コストパフォーマンスモデル。',
     7, 1, 2, 112000.00, 'JPY', 'ACTIVE', 1, 3, 5, 7, 7.50, 'OPTIPLEX-5000-MT', TRUE),

    (3,  'DPC-001003', 'HP ProDesk 400 G9 SFF',               'HP ProDesk 400 G9 SFF',
     'Intel Core i5-12500搭載 スモールフォームファクタ。メモリ8GB、SSD 256GB。エントリーモデル。',
     7, 2, 2, 98000.00, 'JPY', 'ACTIVE', 1, 3, 5, 5, 5.30, 'PRODESK-400-G9', TRUE),

    (4,  'DPC-001004', '富士通 ESPRIMO D7012/M',              'Fujitsu ESPRIMO D7012/M',
     'Intel Core i5-12400搭載 国産デスクトップPC。メモリ8GB、SSD 256GB。官公庁向け実績多数。',
     7, 12, 2, 125000.00, 'JPY', 'ACTIVE', 1, 2, 5, 10, 6.80, 'ESPRIMO-D7012M', TRUE),

    (5,  'DPC-001005', 'NEC Mate MKM30/E-6',                  'NEC Mate MKM30/E-6',
     'Intel Core i5-13400搭載 国産デスクトップPC。メモリ16GB、SSD 512GB。堅牢設計。',
     7, 13, 2, 142000.00, 'JPY', 'INACTIVE', 1, 2, 3, 14, 7.20, 'MATE-MKM30E6', TRUE),

    -- ノートPC（10件）
    (6,  'NPC-002001', 'HP EliteBook 840 G10',                 'HP EliteBook 840 G10',
     'Intel Core i5-1340P搭載 14型ビジネスノート。メモリ16GB、SSD 512GB。指紋認証・IR対応。',
     8, 2, 2, 198000.00, 'JPY', 'ACTIVE', 1, 5, 10, 5, 1.36, 'ELITEBOOK-840-G10', TRUE),

    (7,  'NPC-002002', 'Lenovo ThinkPad X1 Carbon Gen 11',     'Lenovo ThinkPad X1 Carbon Gen 11',
     'Intel Core i7-1365U搭載 14型軽量ビジネスノート。メモリ16GB、SSD 512GB。約1.12kgの軽量設計。',
     8, 3, 2, 265000.00, 'JPY', 'ACTIVE', 1, 3, 5, 7, 1.12, 'X1CARBON-GEN11', TRUE),

    (8,  'NPC-002003', 'Lenovo ThinkPad L14 Gen 4',            'Lenovo ThinkPad L14 Gen 4',
     'Intel Core i5-1345U搭載 14型スタンダードノート。メモリ8GB、SSD 256GB。コスト重視モデル。',
     8, 3, 2, 138000.00, 'JPY', 'ACTIVE', 1, 5, 10, 5, 1.46, 'THINKPAD-L14-G4', TRUE),

    (9,  'NPC-002004', 'Dell Latitude 5540',                    'Dell Latitude 5540',
     'Intel Core i5-1345U搭載 15.6型ビジネスノート。メモリ16GB、SSD 512GB。大画面で事務作業に最適。',
     8, 1, 2, 178000.00, 'JPY', 'ACTIVE', 1, 3, 5, 7, 1.58, 'LATITUDE-5540', TRUE),

    (10, 'NPC-002005', 'Apple MacBook Air 15 M3',               'Apple MacBook Air 15 M3',
     'Apple M3チップ搭載 15.3型ノートPC。メモリ16GB、SSD 512GB。クリエイティブ業務向け。',
     8, 4, 2, 228000.00, 'JPY', 'ACTIVE', 1, 2, 3, 3, 1.51, 'MBA15-M3', TRUE),

    (11, 'NPC-002006', 'Apple MacBook Pro 14 M3 Pro',           'Apple MacBook Pro 14 M3 Pro',
     'Apple M3 Proチップ搭載 14.2型ノートPC。メモリ18GB、SSD 512GB。プロフェッショナル向け。',
     8, 4, 2, 328000.00, 'JPY', 'ACTIVE', 1, 2, 3, 3, 1.55, 'MBP14-M3PRO', TRUE),

    (12, 'NPC-002007', '富士通 LIFEBOOK U7413/M',               'Fujitsu LIFEBOOK U7413/M',
     'Intel Core i5-1340P搭載 14型軽量ノート。メモリ16GB、SSD 512GB。約997gの軽量国産モデル。',
     8, 12, 2, 215000.00, 'JPY', 'ACTIVE', 1, 2, 5, 10, 0.997, 'LIFEBOOK-U7413M', TRUE),

    (13, 'NPC-002008', 'NEC VersaPro UltraLite VG-E',           'NEC VersaPro UltraLite VG-E',
     'Intel Core i5-1340P搭載 14型モバイルノート。メモリ16GB、SSD 512GB。軽量・長時間バッテリー。',
     8, 13, 2, 208000.00, 'JPY', 'ACTIVE', 1, 2, 5, 10, 0.98, 'VERSAPRO-VGE', TRUE),

    (14, 'NPC-002009', 'HP ProBook 450 G10',                    'HP ProBook 450 G10',
     'Intel Core i5-1335U搭載 15.6型ビジネスノート。メモリ8GB、SSD 256GB。エントリービジネスモデル。',
     8, 2, 2, 118000.00, 'JPY', 'ACTIVE', 1, 5, 10, 5, 1.79, 'PROBOOK-450-G10', TRUE),

    (15, 'NPC-002010', 'ASUS ExpertBook B5 B5402CBA',           'ASUS ExpertBook B5 B5402CBA',
     'Intel Core i5-1240P搭載 14型ビジネスノート。メモリ16GB、SSD 512GB。MIL-STD-810H準拠。',
     8, 14, 2, 165000.00, 'JPY', 'DISCONTINUED', 1, 0, 0, 14, 1.39, 'EXPERTBOOK-B5402', TRUE),

    -- ワークステーション（2件）
    (16, 'WKS-003001', 'Dell Precision 5680',                   'Dell Precision 5680',
     'Intel Core i7-13800H搭載 16型モバイルワークステーション。メモリ32GB、SSD 1TB。NVIDIA RTX 3500 Ada。',
     9, 1, 2, 498000.00, 'JPY', 'ACTIVE', 1, 1, 2, 14, 1.91, 'PRECISION-5680', TRUE),

    (17, 'WKS-003002', 'HP ZBook Fury 16 G10',                  'HP ZBook Fury 16 G10',
     'Intel Core i7-13850HX搭載 16型モバイルワークステーション。メモリ32GB、SSD 1TB。NVIDIA RTX 4000 Ada。',
     9, 2, 2, 548000.00, 'JPY', 'ACTIVE', 1, 1, 2, 14, 2.85, 'ZBOOK-FURY16-G10', TRUE),

    -- モニター（5件）
    (18, 'MON-004001', 'EIZO FlexScan EV2760',                  'EIZO FlexScan EV2760',
     '27型 WQHD(2560x1440) IPS液晶モニター。USB-C対応、デイジーチェーン接続可能。',
     10, 7, 2, 78000.00, 'JPY', 'ACTIVE', 1, 5, 10, 5, 7.60, 'EV2760', TRUE),

    (19, 'MON-004002', 'EIZO FlexScan EV2490',                  'EIZO FlexScan EV2490',
     '24.1型 WUXGA(1920x1200) IPS液晶モニター。USB-C PD 70W対応。省スペース設計。',
     10, 7, 2, 55000.00, 'JPY', 'ACTIVE', 1, 8, 15, 5, 5.80, 'EV2490', TRUE),

    (20, 'MON-004003', 'Dell U2723QE',                          'Dell U2723QE',
     '27型 4K UHD(3840x2160) IPS液晶モニター。USB-C PD 90W対応。VESA DisplayHDR 400。',
     10, 1, 2, 68000.00, 'JPY', 'ACTIVE', 1, 5, 10, 7, 6.64, 'U2723QE', TRUE),

    (21, 'MON-004004', 'HP E27k G5 4K USB-C モニター',         'HP E27k G5 4K USB-C Monitor',
     '27型 4K UHD(3840x2160) IPS液晶モニター。USB-C PD 65W。回転・高さ調整スタンド付属。',
     10, 2, 2, 62000.00, 'JPY', 'ACTIVE', 1, 3, 5, 5, 7.10, 'E27K-G5', TRUE),

    (22, 'MON-004005', 'Lenovo ThinkVision T27p-30',            'Lenovo ThinkVision T27p-30',
     '27型 4K UHD IPS液晶モニター。USB-C PD 100W。Thunderbolt 4対応。',
     10, 3, 2, 72000.00, 'JPY', 'ACTIVE', 1, 3, 5, 7, 6.30, 'T27P-30', TRUE),

    -- キーボード（3件）
    (23, 'KBD-005001', 'エレコム TK-FDM110TBK',                 'ELECOM TK-FDM110TBK',
     'テンキー付きワイヤレスフルキーボード。メンブレン式。USB無線2.4GHz。単三電池駆動。',
     11, 8, 1, 3200.00, 'JPY', 'ACTIVE', 1, 20, 50, 3, 0.55, 'TK-FDM110TBK', FALSE),

    (24, 'KBD-005002', 'Lenovo ThinkPad トラックポイントキーボード II',
     'Lenovo ThinkPad TrackPoint Keyboard II',
     'TrackPoint搭載コンパクトキーボード。Bluetooth/USB無線両対応。充電式バッテリー。',
     11, 3, 1, 12800.00, 'JPY', 'ACTIVE', 1, 10, 20, 7, 0.51, 'TPKBD2-BT', FALSE),

    (25, 'KBD-005003', 'Apple Magic Keyboard テンキー付き',     'Apple Magic Keyboard with Numeric Keypad',
     'Apple純正ワイヤレスキーボード。Touch ID搭載。USB-C充電。Mac専用。',
     11, 4, 1, 26800.00, 'JPY', 'ACTIVE', 1, 5, 10, 3, 0.39, 'MK2C3J/A', FALSE),

    -- マウス（3件）
    (26, 'MOU-006001', 'エレコム M-XGM30BBSKBK',               'ELECOM M-XGM30BBSKBK',
     'ワイヤレスBlueLEDマウス。静音タイプ。Bluetooth接続。5ボタン。Lサイズ。',
     12, 8, 1, 3500.00, 'JPY', 'ACTIVE', 1, 20, 50, 3, 0.08, 'M-XGM30BBSKBK', FALSE),

    (27, 'MOU-006002', 'Lenovo ThinkPad Bluetooth サイレントマウス',
     'Lenovo ThinkPad Bluetooth Silent Mouse',
     'ThinkPad専用設計の静音Bluetoothマウス。小型軽量。省電力設計。',
     12, 3, 1, 4500.00, 'JPY', 'ACTIVE', 1, 15, 30, 7, 0.06, '4Y50X88822', FALSE),

    (28, 'MOU-006003', 'Apple Magic Mouse',                     'Apple Magic Mouse',
     'Apple純正ワイヤレスマウス。Multi-Touchジェスチャー対応。Lightning充電。',
     12, 4, 1, 13800.00, 'JPY', 'ACTIVE', 1, 5, 10, 3, 0.10, 'MK2C3J/A-M', FALSE),

    -- プリンター（4件）
    (29, 'PRT-007001', 'Brother MFC-L3780CDW',                  'Brother MFC-L3780CDW',
     'カラーレーザー複合機。A4対応。プリント/コピー/スキャン/FAX。自動両面印刷対応。',
     13, 6, 2, 58000.00, 'JPY', 'ACTIVE', 1, 2, 3, 5, 22.70, 'MFC-L3780CDW', TRUE),

    (30, 'PRT-007002', 'Canon SATERA MF753Cdw',                 'Canon SATERA MF753Cdw',
     'カラーレーザー複合機。A4対応。プリント/コピー/スキャン/FAX。無線LAN対応。',
     13, 10, 2, 65000.00, 'JPY', 'ACTIVE', 1, 2, 3, 5, 24.10, 'MF753CDW', TRUE),

    (31, 'PRT-007003', 'Epson LP-S3290',                        'Epson LP-S3290',
     'A3対応モノクロレーザープリンター。高速印刷35枚/分。大容量給紙対応。',
     13, 11, 2, 89000.00, 'JPY', 'ACTIVE', 1, 1, 2, 7, 18.50, 'LP-S3290', TRUE),

    (32, 'PRT-007004', 'HP Color LaserJet Pro MFP 4302fdn',     'HP Color LaserJet Pro MFP 4302fdn',
     'カラーレーザー複合機。A4対応。プリント/コピー/スキャン/FAX。有線LAN。',
     13, 2, 2, 72000.00, 'JPY', 'DISCONTINUED', 1, 0, 0, 10, 21.80, '4RA82F', TRUE),

    -- ルーター（2件）
    (33, 'RTR-008001', 'Cisco ISR 1100-4G',                     'Cisco ISR 1100-4G',
     'Cisco統合型サービスルーター。4ポートGigabit Ethernet。セキュリティ機能内蔵。',
     14, 5, 2, 320000.00, 'JPY', 'ACTIVE', 1, 1, 2, 14, 3.20, 'C1111-4P', TRUE),

    (34, 'RTR-008002', 'Cisco ISR 1100-8P',                     'Cisco ISR 1100-8P',
     'Cisco統合型サービスルーター。8ポートGigabit Ethernet。PoE対応。拠点間VPN。',
     14, 5, 2, 480000.00, 'JPY', 'ACTIVE', 1, 1, 2, 14, 3.80, 'C1111-8P', TRUE),

    -- スイッチ（3件）
    (35, 'SWT-009001', 'Cisco Catalyst 1300-24T-4G',            'Cisco Catalyst 1300-24T-4G',
     '24ポート ギガビットL2マネージドスイッチ。4つのSFPアップリンク搭載。',
     15, 5, 2, 85000.00, 'JPY', 'ACTIVE', 1, 2, 3, 14, 2.90, 'C1300-24T-4G', TRUE),

    (36, 'SWT-009002', 'Cisco Catalyst 1300-48T-4G',            'Cisco Catalyst 1300-48T-4G',
     '48ポート ギガビットL2マネージドスイッチ。4つのSFPアップリンク搭載。',
     15, 5, 2, 145000.00, 'JPY', 'ACTIVE', 1, 1, 2, 14, 3.50, 'C1300-48T-4G', TRUE),

    (37, 'SWT-009003', 'Buffalo BS-GS2016P',                    'Buffalo BS-GS2016P',
     '16ポート ギガビットL2スマートスイッチ。PoE給電対応。省電力設計。',
     15, 9, 2, 52000.00, 'JPY', 'ACTIVE', 1, 2, 5, 5, 2.10, 'BS-GS2016P', TRUE),

    -- アクセスポイント（2件）
    (38, 'WAP-010001', 'Cisco Meraki MR36',                     'Cisco Meraki MR36',
     'クラウド管理型Wi-Fi 6アクセスポイント。デュアルバンド。最大接続数50台。',
     16, 5, 2, 98000.00, 'JPY', 'ACTIVE', 1, 2, 5, 14, 0.68, 'MR36-HW', TRUE),

    (39, 'WAP-010002', 'Buffalo WAPM-AX8R',                     'Buffalo WAPM-AX8R',
     '法人向けWi-Fi 6対応無線LANアクセスポイント。トライバンド。PoE受電対応。',
     16, 9, 2, 58000.00, 'JPY', 'ACTIVE', 1, 3, 5, 5, 0.55, 'WAPM-AX8R', TRUE),

    -- SSD（2件）
    (40, 'SSD-011001', 'Samsung 870 EVO 1TB',                   'Samsung 870 EVO 1TB',
     '2.5インチ SATA III SSD。1TB容量。読取560MB/s、書込530MB/s。5年保証。',
     18, NULL, 1, 12800.00, 'JPY', 'ACTIVE', 1, 10, 20, 3, 0.058, '870-EVO-1TB', FALSE),

    (41, 'SSD-011002', 'Samsung 990 PRO 2TB',                   'Samsung 990 PRO 2TB',
     'M.2 NVMe Gen4 SSD。2TB容量。読取7450MB/s、書込6900MB/s。ハイエンドモデル。',
     18, NULL, 1, 28000.00, 'JPY', 'ACTIVE', 1, 5, 10, 3, 0.009, '990-PRO-2TB', FALSE),

    -- OS（2件）
    (42, 'SFT-012001', 'Microsoft Windows 11 Pro',              'Microsoft Windows 11 Pro',
     'Windows 11 Pro 64bit ボリュームライセンス。法人向けプロフェッショナルエディション。',
     19, 15, 6, 28600.00, 'JPY', 'ACTIVE', 1, 10, 20, 1, NULL, 'WIN11PRO-VL', FALSE),

    (43, 'SFT-012002', 'Microsoft Windows 11 Home',             'Microsoft Windows 11 Home',
     'Windows 11 Home 64bit パッケージ版。個人向け基本エディション。',
     19, 15, 6, 19360.00, 'JPY', 'ACTIVE', 1, 5, 10, 1, NULL, 'WIN11HOME-PKG', FALSE),

    -- オフィスソフト（2件）
    (44, 'SFT-013001', 'Microsoft 365 Business Standard',       'Microsoft 365 Business Standard',
     'Microsoft 365 法人向け年間サブスクリプション。Word/Excel/PowerPoint/Teams/OneDrive含む。',
     20, 15, 6, 18720.00, 'JPY', 'ACTIVE', 1, 10, 50, 1, NULL, 'M365-BS-1YR', FALSE),

    (45, 'SFT-013002', 'Microsoft 365 Apps for Enterprise',     'Microsoft 365 Apps for Enterprise',
     'Microsoft 365 大企業向け年間サブスクリプション。Officeフルスイート + クラウドサービス。',
     20, 15, 6, 23400.00, 'JPY', 'ACTIVE', 1, 10, 50, 1, NULL, 'M365-E3-1YR', FALSE),

    -- サプライ品（5件）
    (46, 'TNR-014001', 'Brother TN-493BK トナーカートリッジ（黒）',
     'Brother TN-493BK Black Toner',
     'Brother MFC-L3780CDW用 純正トナーカートリッジ（黒）。印刷可能枚数 約4,500枚。',
     6, 6, 1, 8800.00, 'JPY', 'ACTIVE', 1, 5, 10, 3, 0.35, 'TN-493BK', FALSE),

    (47, 'TNR-014002', 'Canon CRG-057H トナーカートリッジ',
     'Canon CRG-057H Toner',
     'Canon SATERA MF753Cdw用 純正トナーカートリッジ（黒）。大容量タイプ。印刷可能枚数 約10,000枚。',
     6, 10, 1, 15800.00, 'JPY', 'ACTIVE', 1, 3, 5, 5, 0.85, 'CRG-057H', FALSE),

    (48, 'CBL-015001', 'エレコム USB-C to USB-C ケーブル 1m',
     'ELECOM USB-C to USB-C Cable 1m',
     'USB Type-C ケーブル 1m。USB PD 100W対応。映像出力対応。ナイロンメッシュ被覆。',
     6, 8, 3, 1980.00, 'JPY', 'ACTIVE', 5, 30, 50, 3, 0.04, 'USB4-CC10NBK', FALSE),

    (49, 'CBL-015002', 'エレコム LANケーブル Cat6A 3m',
     'ELECOM LAN Cable Cat6A 3m',
     'カテゴリ6A LANケーブル 3m。10Gbps対応。ツメ折れ防止コネクタ。',
     6, 8, 3, 780.00, 'JPY', 'ACTIVE', 5, 50, 100, 3, 0.08, 'LD-GPA/BU3', FALSE),

    (50, 'CBL-015003', 'エレコム HDMIケーブル 4K対応 2m',
     'ELECOM HDMI Cable 4K 2m',
     'Premium HDMI ケーブル 2m。4K/60Hz HDR対応。イーサネット対応。',
     6, 8, 3, 1580.00, 'JPY', 'ACTIVE', 5, 20, 50, 3, 0.06, 'DH-HDP14E20BK', FALSE);

SELECT setval('product_id_seq', 50);

-- ----------------------------------------------------------------------------
-- 商品仕様（100件）
-- 各商品の技術仕様をkey-valueペアで定義
-- ----------------------------------------------------------------------------
INSERT INTO product_specification (id, product_id, spec_name, spec_value, spec_unit, value_type, numeric_value, sort_order)
VALUES
    -- Dell OptiPlex 7010 SFF (id=1)
    (1,  1, 'CPU',        'Intel Core i5-13500T', NULL, 'TEXT', NULL, 1),
    (2,  1, 'メモリ',     '16GB DDR5-4800',      NULL, 'TEXT', NULL, 2),
    (3,  1, 'ストレージ', '512GB NVMe SSD',      NULL, 'TEXT', NULL, 3),
    (4,  1, '消費電力',   '65',                  'W',  'NUMERIC', 65, 4),
    -- Dell OptiPlex 5000 MT (id=2)
    (5,  2, 'CPU',        'Intel Core i5-12500',  NULL, 'TEXT', NULL, 1),
    (6,  2, 'メモリ',     '8GB DDR4-3200',       NULL, 'TEXT', NULL, 2),
    (7,  2, 'ストレージ', '256GB NVMe SSD',      NULL, 'TEXT', NULL, 3),
    (8,  2, '消費電力',   '200',                 'W',  'NUMERIC', 200, 4),
    -- HP ProDesk 400 G9 (id=3)
    (9,  3, 'CPU',        'Intel Core i5-12500',  NULL, 'TEXT', NULL, 1),
    (10, 3, 'メモリ',     '8GB DDR4-3200',       NULL, 'TEXT', NULL, 2),
    (11, 3, 'ストレージ', '256GB NVMe SSD',      NULL, 'TEXT', NULL, 3),
    (12, 3, '消費電力',   '180',                 'W',  'NUMERIC', 180, 4),
    -- HP EliteBook 840 G10 (id=6)
    (13, 6, 'CPU',        'Intel Core i5-1340P',  NULL, 'TEXT', NULL, 1),
    (14, 6, 'メモリ',     '16GB DDR5-5200',      NULL, 'TEXT', NULL, 2),
    (15, 6, 'ストレージ', '512GB NVMe SSD',      NULL, 'TEXT', NULL, 3),
    (16, 6, 'ディスプレイ','14型 WUXGA IPS',      NULL, 'TEXT', NULL, 4),
    (17, 6, 'バッテリー駆動時間', '約14時間',     NULL, 'TEXT', NULL, 5),
    -- Lenovo ThinkPad X1 Carbon Gen 11 (id=7)
    (18, 7, 'CPU',        'Intel Core i7-1365U',  NULL, 'TEXT', NULL, 1),
    (19, 7, 'メモリ',     '16GB LPDDR5-6400',    NULL, 'TEXT', NULL, 2),
    (20, 7, 'ストレージ', '512GB NVMe SSD',      NULL, 'TEXT', NULL, 3),
    (21, 7, 'ディスプレイ','14型 WUXGA IPS',      NULL, 'TEXT', NULL, 4),
    (22, 7, 'バッテリー駆動時間', '約15時間',     NULL, 'TEXT', NULL, 5),
    (23, 7, '重量',       '1.12',                'kg', 'NUMERIC', 1.12, 6),
    -- ThinkPad L14 Gen 4 (id=8)
    (24, 8, 'CPU',        'Intel Core i5-1345U',  NULL, 'TEXT', NULL, 1),
    (25, 8, 'メモリ',     '8GB DDR4-3200',       NULL, 'TEXT', NULL, 2),
    (26, 8, 'ストレージ', '256GB NVMe SSD',      NULL, 'TEXT', NULL, 3),
    (27, 8, 'ディスプレイ','14型 FHD IPS',        NULL, 'TEXT', NULL, 4),
    -- Dell Latitude 5540 (id=9)
    (28, 9, 'CPU',        'Intel Core i5-1345U',  NULL, 'TEXT', NULL, 1),
    (29, 9, 'メモリ',     '16GB DDR4-3200',      NULL, 'TEXT', NULL, 2),
    (30, 9, 'ストレージ', '512GB NVMe SSD',      NULL, 'TEXT', NULL, 3),
    (31, 9, 'ディスプレイ','15.6型 FHD IPS',      NULL, 'TEXT', NULL, 4),
    -- Apple MacBook Air 15 M3 (id=10)
    (32, 10, 'CPU',       'Apple M3 (8コアCPU/10コアGPU)', NULL, 'TEXT', NULL, 1),
    (33, 10, 'メモリ',    '16GB ユニファイドメモリ', NULL, 'TEXT', NULL, 2),
    (34, 10, 'ストレージ','512GB SSD',            NULL, 'TEXT', NULL, 3),
    (35, 10, 'ディスプレイ','15.3型 Liquid Retina', NULL, 'TEXT', NULL, 4),
    (36, 10, 'バッテリー駆動時間', '約18時間',    NULL, 'TEXT', NULL, 5),
    -- Apple MacBook Pro 14 M3 Pro (id=11)
    (37, 11, 'CPU',       'Apple M3 Pro (12コアCPU/18コアGPU)', NULL, 'TEXT', NULL, 1),
    (38, 11, 'メモリ',    '18GB ユニファイドメモリ', NULL, 'TEXT', NULL, 2),
    (39, 11, 'ストレージ','512GB SSD',            NULL, 'TEXT', NULL, 3),
    (40, 11, 'ディスプレイ','14.2型 Liquid Retina XDR', NULL, 'TEXT', NULL, 4),
    (41, 11, 'バッテリー駆動時間', '約17時間',    NULL, 'TEXT', NULL, 5),
    -- 富士通 LIFEBOOK U7413/M (id=12)
    (42, 12, 'CPU',       'Intel Core i5-1340P',  NULL, 'TEXT', NULL, 1),
    (43, 12, 'メモリ',    '16GB LPDDR5',         NULL, 'TEXT', NULL, 2),
    (44, 12, 'ストレージ','512GB NVMe SSD',      NULL, 'TEXT', NULL, 3),
    (45, 12, 'ディスプレイ','14型 WUXGA',          NULL, 'TEXT', NULL, 4),
    (46, 12, '重量',      '0.997',               'kg', 'NUMERIC', 0.997, 5),
    -- NEC VersaPro (id=13)
    (47, 13, 'CPU',       'Intel Core i5-1340P',  NULL, 'TEXT', NULL, 1),
    (48, 13, 'メモリ',    '16GB LPDDR5',         NULL, 'TEXT', NULL, 2),
    (49, 13, 'ストレージ','512GB NVMe SSD',      NULL, 'TEXT', NULL, 3),
    (50, 13, 'ディスプレイ','14型 WUXGA',          NULL, 'TEXT', NULL, 4),
    (51, 13, '重量',      '0.98',                'kg', 'NUMERIC', 0.98, 5),
    -- HP ProBook 450 G10 (id=14)
    (52, 14, 'CPU',       'Intel Core i5-1335U',  NULL, 'TEXT', NULL, 1),
    (53, 14, 'メモリ',    '8GB DDR4-3200',       NULL, 'TEXT', NULL, 2),
    (54, 14, 'ストレージ','256GB NVMe SSD',      NULL, 'TEXT', NULL, 3),
    (55, 14, 'ディスプレイ','15.6型 FHD IPS',      NULL, 'TEXT', NULL, 4),
    -- Dell Precision 5680 (id=16)
    (56, 16, 'CPU',       'Intel Core i7-13800H', NULL, 'TEXT', NULL, 1),
    (57, 16, 'メモリ',    '32GB DDR5-5600',      NULL, 'TEXT', NULL, 2),
    (58, 16, 'ストレージ','1TB NVMe SSD',        NULL, 'TEXT', NULL, 3),
    (59, 16, 'GPU',       'NVIDIA RTX 3500 Ada 12GB', NULL, 'TEXT', NULL, 4),
    (60, 16, 'ディスプレイ','16型 WQXGA+ OLED',    NULL, 'TEXT', NULL, 5),
    -- HP ZBook Fury 16 G10 (id=17)
    (61, 17, 'CPU',       'Intel Core i7-13850HX', NULL, 'TEXT', NULL, 1),
    (62, 17, 'メモリ',    '32GB DDR5-4800',      NULL, 'TEXT', NULL, 2),
    (63, 17, 'ストレージ','1TB NVMe SSD',        NULL, 'TEXT', NULL, 3),
    (64, 17, 'GPU',       'NVIDIA RTX 4000 Ada 12GB', NULL, 'TEXT', NULL, 4),
    (65, 17, 'ディスプレイ','16型 WQXGA IPS',      NULL, 'TEXT', NULL, 5),
    -- EIZO FlexScan EV2760 (id=18)
    (66, 18, '画面サイズ',   '27',                '型', 'NUMERIC', 27, 1),
    (67, 18, '解像度',       '2560x1440 (WQHD)',  NULL, 'TEXT', NULL, 2),
    (68, 18, 'パネル種別',   'IPS',               NULL, 'TEXT', NULL, 3),
    (69, 18, '入力端子',     'USB-C, DisplayPort, HDMI', NULL, 'TEXT', NULL, 4),
    -- EIZO FlexScan EV2490 (id=19)
    (70, 19, '画面サイズ',   '24.1',              '型', 'NUMERIC', 24.1, 1),
    (71, 19, '解像度',       '1920x1200 (WUXGA)', NULL, 'TEXT', NULL, 2),
    (72, 19, 'パネル種別',   'IPS',               NULL, 'TEXT', NULL, 3),
    (73, 19, '入力端子',     'USB-C, DisplayPort, HDMI', NULL, 'TEXT', NULL, 4),
    -- Dell U2723QE (id=20)
    (74, 20, '画面サイズ',   '27',                '型', 'NUMERIC', 27, 1),
    (75, 20, '解像度',       '3840x2160 (4K UHD)', NULL, 'TEXT', NULL, 2),
    (76, 20, 'パネル種別',   'IPS Black',         NULL, 'TEXT', NULL, 3),
    (77, 20, 'USB-C給電',    '90',                'W',  'NUMERIC', 90, 4),
    -- Brother MFC-L3780CDW (id=29)
    (78, 29, '印刷速度',     '26',                '枚/分', 'NUMERIC', 26, 1),
    (79, 29, '対応用紙',     'A4',                NULL, 'TEXT', NULL, 2),
    (80, 29, '接続方式',     'USB, 有線LAN, 無線LAN', NULL, 'TEXT', NULL, 3),
    (81, 29, '自動両面印刷', 'true',              NULL, 'BOOLEAN', NULL, 4),
    -- Canon SATERA MF753Cdw (id=30)
    (82, 30, '印刷速度',     '33',                '枚/分', 'NUMERIC', 33, 1),
    (83, 30, '対応用紙',     'A4',                NULL, 'TEXT', NULL, 2),
    (84, 30, '接続方式',     'USB, 有線LAN, 無線LAN', NULL, 'TEXT', NULL, 3),
    -- Cisco ISR 1100-4G (id=33)
    (85, 33, 'ポート数',     '4',                 'ポート', 'NUMERIC', 4, 1),
    (86, 33, 'スループット', '300',               'Mbps', 'NUMERIC', 300, 2),
    (87, 33, 'VPN対応',      'true',              NULL, 'BOOLEAN', NULL, 3),
    -- Cisco Catalyst 1300-24T-4G (id=35)
    (88, 35, 'ポート数',     '24',                'ポート', 'NUMERIC', 24, 1),
    (89, 35, 'スイッチング容量', '56',             'Gbps', 'NUMERIC', 56, 2),
    (90, 35, 'PoE対応',      'false',             NULL, 'BOOLEAN', NULL, 3),
    -- Cisco Meraki MR36 (id=38)
    (91, 38, 'Wi-Fi規格',    'Wi-Fi 6 (802.11ax)', NULL, 'TEXT', NULL, 1),
    (92, 38, 'バンド',       'デュアルバンド',      NULL, 'TEXT', NULL, 2),
    (93, 38, '最大接続数',   '50',                '台', 'NUMERIC', 50, 3),
    -- Samsung 870 EVO 1TB (id=40)
    (94, 40, '容量',         '1000',              'GB', 'NUMERIC', 1000, 1),
    (95, 40, 'インターフェース', 'SATA III 6Gbps', NULL, 'TEXT', NULL, 2),
    (96, 40, '読取速度',     '560',               'MB/s', 'NUMERIC', 560, 3),
    (97, 40, '書込速度',     '530',               'MB/s', 'NUMERIC', 530, 4),
    -- Microsoft 365 Business Standard (id=44)
    (98, 44, 'ライセンス形態', '年間サブスクリプション', NULL, 'TEXT', NULL, 1),
    (99, 44, '含まれるアプリ', 'Word, Excel, PowerPoint, Outlook, Teams, OneDrive, SharePoint', NULL, 'TEXT', NULL, 2),
    (100, 44, 'ユーザー数',  '1',                 'ユーザー', 'NUMERIC', 1, 3);

SELECT setval('product_specification_id_seq', 100);

-- ----------------------------------------------------------------------------
-- 商品タグ（5件）と商品タグマッピング
-- 商品の特性をタグで分類する
-- ----------------------------------------------------------------------------
INSERT INTO product_tag (id, tag_name, tag_color, description)
VALUES
    (1, '人気',     '#FF6B6B', '社内で利用頻度が高い人気商品'),
    (2, '推奨',     '#4ECDC4', '情報システム部が推奨する標準構成品'),
    (3, '新製品',   '#45B7D1', '直近3ヶ月以内に追加された新商品'),
    (4, '法人向け', '#96CEB4', '法人専用モデル・ライセンス'),
    (5, '在庫限り', '#FFEAA7', '在庫がなくなり次第終了する商品');

SELECT setval('product_tag_id_seq', 5);

-- 人気タグ: ThinkPad X1, EliteBook, FlexScan, M365
INSERT INTO product_tag_mapping (product_id, tag_id)
VALUES
    (7,  1), (6,  1), (18, 1), (19, 1), (44, 1),
    -- 推奨タグ: OptiPlex 7010, EliteBook 840, ThinkPad X1, FlexScan EV2760, M365
    (1,  2), (6,  2), (7,  2), (18, 2), (44, 2), (42, 2),
    -- 新製品タグ: MacBook Air M3, MacBook Pro M3 Pro
    (10, 3), (11, 3),
    -- 法人向けタグ: Win11 Pro, M365 Business, M365 Enterprise, OptiPlex
    (42, 4), (44, 4), (45, 4), (1, 4), (6, 4),
    -- 在庫限りタグ: ASUS ExpertBook (DISCONTINUED), HP LaserJet (DISCONTINUED)
    (15, 5), (32, 5);

-- ----------------------------------------------------------------------------
-- 代替商品（10件）
-- 同一カテゴリ内で代替可能な商品の関連定義
-- ----------------------------------------------------------------------------
INSERT INTO product_alternative (id, product_id, alternative_product_id, priority, compatibility, notes)
VALUES
    -- デスクトップPC間の代替関係
    (1,  1, 3,  1, 'SIMILAR',     'HP ProDesk 400はコスト重視の代替候補'),
    (2,  1, 4,  2, 'EQUIVALENT',  '富士通 ESPRIMOは国産志向の場合の代替候補'),
    -- ノートPC間の代替関係
    (3,  7, 6,  1, 'SIMILAR',     'HP EliteBookはThinkPad X1の代替候補'),
    (4,  7, 12, 2, 'EQUIVALENT',  '富士通 LIFEBOOKは軽量国産の代替候補'),
    (5,  6, 9,  1, 'SIMILAR',     'Dell Latitudeは同価格帯の代替候補'),
    (6,  10, 11, 1, 'UPGRADE',   'MacBook ProはMacBook Airの上位モデル'),
    -- モニター間の代替関係
    (7,  18, 20, 1, 'SIMILAR',    'Dell U2723QEは4K対応の代替候補'),
    (8,  18, 22, 2, 'SIMILAR',    'ThinkVision T27pは価格重視の代替候補'),
    -- プリンター間の代替関係
    (9,  29, 30, 1, 'EQUIVALENT', 'Canon SATERA MF753Cdwは同等機能の代替候補'),
    (10, 30, 29, 1, 'EQUIVALENT', 'Brother MFC-L3780CDWは同等機能の代替候補');

SELECT setval('product_alternative_id_seq', 10);

-- ----------------------------------------------------------------------------
-- 商品バンドル（3件）
-- 複数商品をまとめたセット商品
-- ----------------------------------------------------------------------------
INSERT INTO product_bundle (id, bundle_code, name, description, bundle_price, discount_pct, status, valid_from, valid_until)
VALUES
    (1, 'BDL-NEWEMPLOYEE', '新入社員セット',
     'ノートPC + モニター + キーボード + マウス + USBケーブルの基本業務セット',
     285000.00, 5.00, 'ACTIVE', '2024-04-01', '2025-03-31'),
    (2, 'BDL-TELEWORK', '在宅勤務パッケージ',
     'ノートPC + モニター + キーボード + マウス + ヘッドセット想定の在宅勤務セット',
     320000.00, 7.50, 'ACTIVE', '2024-04-01', '2025-03-31'),
    (3, 'BDL-MEETINGROOM', '会議室セット',
     'モニター + 無線AP + ケーブル類の会議室整備セット',
     155000.00, 3.00, 'ACTIVE', '2024-04-01', '2025-03-31');

SELECT setval('product_bundle_id_seq', 3);

-- バンドル明細
INSERT INTO product_bundle_item (id, bundle_id, product_id, quantity, sort_order)
VALUES
    -- 新入社員セット
    (1, 1, 8,  1, 1),  -- ThinkPad L14 Gen 4
    (2, 1, 19, 1, 2),  -- EIZO FlexScan EV2490
    (3, 1, 23, 1, 3),  -- エレコム キーボード
    (4, 1, 26, 1, 4),  -- エレコム マウス
    (5, 1, 48, 1, 5),  -- USB-Cケーブル
    -- 在宅勤務パッケージ
    (6, 2, 6,  1, 1),  -- HP EliteBook 840 G10
    (7, 2, 20, 1, 2),  -- Dell U2723QE
    (8, 2, 24, 1, 3),  -- ThinkPad トラックポイントキーボード
    (9, 2, 27, 1, 4),  -- ThinkPad Bluetoothマウス
    (10, 2, 48, 2, 5), -- USB-Cケーブル x2
    -- 会議室セット
    (11, 3, 18, 1, 1), -- EIZO FlexScan EV2760
    (12, 3, 39, 1, 2), -- Buffalo WAPM-AX8R
    (13, 3, 50, 2, 3), -- HDMIケーブル x2
    (14, 3, 49, 3, 4); -- LANケーブル x3

SELECT setval('product_bundle_item_id_seq', 14);
