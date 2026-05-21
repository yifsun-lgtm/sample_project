-- V028: 製品画像の初期データ投入
-- 製品ドキュメントが存在する14製品に対してメイン画像を1枚ずつ登録

INSERT INTO product_image (id, product_id, image_url, thumbnail_url, alt_text, image_type, sort_order, is_primary, file_size_bytes, width_px, height_px, file_name, file_path, mime_type)
VALUES
    (1,  1,  '/images/products/product-1-dell-optiplex-7010.png',       NULL, 'Dell OptiPlex 7010 SFF',              'PHOTO', 1, TRUE,  4108, 400, 300, 'product-1-dell-optiplex-7010.png',       '/images/products/product-1-dell-optiplex-7010.png',       'image/png'),
    (2,  6,  '/images/products/product-6-hp-elitebook-840.png',         NULL, 'HP EliteBook 840 G10',                'PHOTO', 1, TRUE,  3782, 400, 300, 'product-6-hp-elitebook-840.png',         '/images/products/product-6-hp-elitebook-840.png',         'image/png'),
    (3,  7,  '/images/products/product-7-thinkpad-x1carbon-gen11.png',  NULL, 'ThinkPad X1 Carbon Gen 11',           'PHOTO', 1, TRUE,  3926, 400, 300, 'product-7-thinkpad-x1carbon-gen11.png',  '/images/products/product-7-thinkpad-x1carbon-gen11.png',  'image/png'),
    (4,  10, '/images/products/product-10-apple-mba15-m3.png',          NULL, 'Apple MacBook Air 15 M3',             'PHOTO', 1, TRUE,  3970, 400, 300, 'product-10-apple-mba15-m3.png',          '/images/products/product-10-apple-mba15-m3.png',          'image/png'),
    (5,  18, '/images/products/product-18-eizo-ev2760.png',             NULL, 'EIZO FlexScan EV2760',                'PHOTO', 1, TRUE,  4350, 400, 300, 'product-18-eizo-ev2760.png',             '/images/products/product-18-eizo-ev2760.png',             'image/png'),
    (6,  29, '/images/products/product-29-brother-mfc-l3780cdw.png',    NULL, 'Brother MFC-L3780CDW',                'PHOTO', 1, TRUE,  4256, 400, 300, 'product-29-brother-mfc-l3780cdw.png',    '/images/products/product-29-brother-mfc-l3780cdw.png',    'image/png'),
    (7,  33, '/images/products/product-33-cisco-isr1100.png',           NULL, 'Cisco ISR 1100-4G',                   'PHOTO', 1, TRUE,  3787, 400, 300, 'product-33-cisco-isr1100.png',           '/images/products/product-33-cisco-isr1100.png',           'image/png'),
    (8,  35, '/images/products/product-35-cisco-catalyst1300.png',      NULL, 'Cisco Catalyst 1300-24T-4G',          'PHOTO', 1, TRUE,  4194, 400, 300, 'product-35-cisco-catalyst1300.png',      '/images/products/product-35-cisco-catalyst1300.png',      'image/png'),
    (9,  38, '/images/products/product-38-cisco-meraki-mr36.png',       NULL, 'Cisco Meraki MR36',                   'PHOTO', 1, TRUE,  4461, 400, 300, 'product-38-cisco-meraki-mr36.png',       '/images/products/product-38-cisco-meraki-mr36.png',       'image/png'),
    (10, 40, '/images/products/product-40-samsung-870evo.png',          NULL, 'Samsung 870 EVO 1TB',                 'PHOTO', 1, TRUE,  4064, 400, 300, 'product-40-samsung-870evo.png',          '/images/products/product-40-samsung-870evo.png',          'image/png'),
    (11, 44, '/images/products/product-44-ms365-business.png',          NULL, 'Microsoft 365 Business Standard',     'PHOTO', 1, TRUE,  4369, 400, 300, 'product-44-ms365-business.png',          '/images/products/product-44-ms365-business.png',          'image/png'),
    (12, 77, '/images/products/product-77-eset-protect-entry.png',      NULL, 'ESET PROTECT Entry',                  'PHOTO', 1, TRUE,  3782, 400, 300, 'product-77-eset-protect-entry.png',      '/images/products/product-77-eset-protect-entry.png',      'image/png'),
    (13, 93, '/images/products/product-93-synology-ds923plus.png',      NULL, 'Synology DiskStation DS923+',         'PHOTO', 1, TRUE,  3836, 400, 300, 'product-93-synology-ds923plus.png',      '/images/products/product-93-synology-ds923plus.png',      'image/png'),
    (14, 97, '/images/products/product-97-apc-smt1500j.png',           NULL, 'APC Smart-UPS SMT1500J',              'PHOTO', 1, TRUE,  7324, 400, 300, 'product-97-apc-smt1500j.png',           '/images/products/product-97-apc-smt1500j.png',           'image/png');

SELECT setval('product_image_id_seq', 14);
