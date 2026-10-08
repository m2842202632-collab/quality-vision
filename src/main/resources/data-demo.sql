
-- 演示账号 admin / 123456（SHA256(salt+password)，salt见AuthService常量）
INSERT INTO sys_user (username, password_hash, salt) VALUES
('admin', 'facfa67a68e1643b47ac51888b82247cec3a039c686354e52629a94c72bf9951', 'qv-demo-salt');

INSERT INTO product (code, name, spec) VALUES
('GLV-001', '一次性医用检查手套', '丁腈乳胶，M码，100只/盒'),
('MSK-002', '医用外科口罩', '三层无纺布，耳挂式，50只/盒'),
('SYR-003', '一次性无菌注射器', '5ml，带针，独立包装');

INSERT INTO inspection_standard (product_id, check_item, defect_types, severity_rules) VALUES
(1, '外观', '["scratch","stain","deformation","hole"]', '破洞hole一律high；划痕长度>2cm为high，否则medium；污点面积>1cm²为medium，否则low'),
(1, '包装', '["missing_part","stain","deformation"]', '缺件missing_part一律high；包装变形影响密封为high'),
(2, '外观', '["stain","deformation","earloop_defect"]', '耳带缺陷earloop_defect一律high；污点>1cm²为medium'),
(2, '包装', '["missing_part","stain"]', '缺件一律high'),
(3, '外观', '["scratch","crack","stain","deformation"]', '裂纹crack一律high；划痕>1cm为medium'),
(3, '包装', '["missing_part","seal_defect"]', '封口缺陷seal_defect一律high；缺件一律high');

-- 历史质检记录（演示看板用，近14天）
INSERT INTO inspection_record
(product_id, batch_no, image_path, ai_result_json, defect_count, overall_result, source, latency_ms, status, human_verdict, inspector, create_time, confirmed_at)
VALUES
(1, 'B20261001', 'samples/glove_ok.jpg', '{"has_defect":false,"defects":[]}', 0, 'PASS', 'MOCK', 820, 'CONFIRMED', 'PASS', 'admin', DATEADD('DAY', -13, NOW()), DATEADD('DAY', -13, NOW())),
(1, 'B20261001', 'samples/glove_stain.jpg', '{"has_defect":true,"defects":[{"type":"stain","severity":"medium","suggestion":"返工","bbox":[120,80,300,260],"confidence":0.88,"description":"手套表面有一处约1cm污点"}]}', 1, 'FAIL', 'MOCK', 1150, 'CONFIRMED', 'FAIL', 'admin', DATEADD('DAY', -12, NOW()), DATEADD('DAY', -12, NOW())),
(2, 'B20261002', 'samples/mask_ok.jpg', '{"has_defect":false,"defects":[]}', 0, 'PASS', 'MOCK', 760, 'CONFIRMED', 'PASS', 'admin', DATEADD('DAY', -11, NOW()), DATEADD('DAY', -11, NOW())),
(2, 'B20261003', 'samples/mask_deform.jpg', '{"has_defect":true,"defects":[{"type":"deformation","severity":"high","suggestion":"隔离待评审","bbox":[400,200,700,500],"confidence":0.93,"description":"口罩本体明显褶皱变形"}]}', 1, 'FAIL', 'MOCK', 1320, 'CONFIRMED', 'FAIL', 'admin', DATEADD('DAY', -10, NOW()), DATEADD('DAY', -10, NOW())),
(3, 'B20261004', 'samples/syringe_ok.jpg', '{"has_defect":false,"defects":[]}', 0, 'PASS', 'MOCK', 690, 'CONFIRMED', 'PASS', 'admin', DATEADD('DAY', -9, NOW()), DATEADD('DAY', -9, NOW())),
(3, 'B20261004', 'samples/syringe_scratch.jpg', '{"has_defect":true,"defects":[{"type":"scratch","severity":"medium","suggestion":"返工","bbox":[200,150,520,220],"confidence":0.85,"description":"针筒表面一道约1.5cm划痕"}]}', 1, 'FAIL', 'MOCK', 1040, 'CONFIRMED', 'FAIL', 'admin', DATEADD('DAY', -8, NOW()), DATEADD('DAY', -8, NOW())),
(1, 'B20261005', 'samples/glove_ok2.jpg', '{"has_defect":false,"defects":[]}', 0, 'PASS', 'MOCK', 710, 'CONFIRMED', 'PASS', 'admin', DATEADD('DAY', -7, NOW()), DATEADD('DAY', -7, NOW())),
(2, 'B20261006', 'samples/mask_ok2.jpg', '{"has_defect":false,"defects":[]}', 0, 'PASS', 'MOCK', 680, 'CONFIRMED', 'PASS', 'admin', DATEADD('DAY', -6, NOW()), DATEADD('DAY', -6, NOW())),
(1, 'B20261007', 'samples/glove_hole.jpg', '{"has_defect":true,"defects":[{"type":"hole","severity":"high","suggestion":"隔离待评审","bbox":[300,300,380,380],"confidence":0.96,"description":"手套指部有一处破洞"}]}', 1, 'FAIL', 'MOCK', 1210, 'CONFIRMED', 'FAIL', 'admin', DATEADD('DAY', -5, NOW()), DATEADD('DAY', -5, NOW())),
(3, 'B20261008', 'samples/syringe_ok2.jpg', '{"has_defect":false,"defects":[]}', 0, 'PASS', 'MOCK', 730, 'CONFIRMED', 'PASS', 'admin', DATEADD('DAY', -4, NOW()), DATEADD('DAY', -4, NOW())),
(2, 'B20261009', 'samples/mask_stain.jpg', '{"has_defect":true,"defects":[{"type":"stain","severity":"low","suggestion":"放行","bbox":[150,120,230,200],"confidence":0.78,"description":"口罩边缘轻微污点"}]}', 1, 'FAIL', 'MOCK', 990, 'CONFIRMED', 'PASS', 'admin', DATEADD('DAY', -3, NOW()), DATEADD('DAY', -3, NOW())),
(1, 'B20261010', 'samples/glove_ok3.jpg', '{"has_defect":false,"defects":[]}', 0, 'PASS', 'MOCK', 700, 'CONFIRMED', 'PASS', 'admin', DATEADD('DAY', -2, NOW()), DATEADD('DAY', -2, NOW())),
(3, 'B20261011', 'samples/syringe_crack.jpg', '{"has_defect":true,"defects":[{"type":"crack","severity":"high","suggestion":"隔离待评审","bbox":[250,180,600,240],"confidence":0.94,"description":"针筒有一道裂纹"}]}', 1, 'FAIL', 'MOCK', 1180, 'AI_DONE', NULL, NULL, DATEADD('DAY', -1, NOW()), NULL),
(2, 'B20261012', 'samples/mask_ok3.jpg', '{"has_defect":false,"defects":[]}', 0, 'PASS', 'MOCK', 660, 'AI_DONE', NULL, NULL, NOW(), NULL);

-- 缺陷明细（与上面 FAIL 记录的 ai_result_json 对应，record_id 按插入顺序 1..14）
INSERT INTO defect_detail
(record_id, defect_type, severity, bbox_x1, bbox_y1, bbox_x2, bbox_y2, confidence, description, suggestion)
VALUES
(2, 'stain', 'medium', 120, 80, 300, 260, 0.8800, '手套表面有一处约1cm污点', '返工'),
(4, 'deformation', 'high', 400, 200, 700, 500, 0.9300, '口罩本体明显褶皱变形', '隔离待评审'),
(6, 'scratch', 'medium', 200, 150, 520, 220, 0.8500, '针筒表面一道约1.5cm划痕', '返工'),
(9, 'hole', 'high', 300, 300, 380, 380, 0.9600, '手套指部有一处破洞', '隔离待评审'),
(11, 'stain', 'low', 150, 120, 230, 200, 0.7800, '口罩边缘轻微污点', '放行'),
(13, 'crack', 'high', 250, 180, 600, 240, 0.9400, '针筒有一道裂纹', '隔离待评审');
