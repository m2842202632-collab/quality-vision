CREATE DATABASE IF NOT EXISTS quality_vision DEFAULT CHARSET utf8mb4;
USE quality_vision;

CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  username VARCHAR(32) NOT NULL COMMENT '用户名',
  password_hash VARCHAR(128) NOT NULL COMMENT 'SHA256加盐哈希',
  salt VARCHAR(64) NOT NULL COMMENT '盐',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

CREATE TABLE product (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  code VARCHAR(32) NOT NULL COMMENT '产品编码',
  name VARCHAR(64) NOT NULL COMMENT '产品名称',
  spec VARCHAR(128) COMMENT '规格说明',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品表';

CREATE TABLE inspection_standard (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  product_id BIGINT NOT NULL COMMENT '产品id',
  check_item VARCHAR(64) NOT NULL COMMENT '检测项：外观/尺寸/装配',
  defect_types TEXT COMMENT '该检测项关注的缺陷类型JSON数组',
  severity_rules VARCHAR(512) COMMENT '严重度判定规则描述',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质检标准表';

CREATE TABLE inspection_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  product_id BIGINT NOT NULL COMMENT '产品id',
  batch_no VARCHAR(32) NOT NULL COMMENT '批次号',
  image_path VARCHAR(256) COMMENT '原图存储路径',
  ai_raw_response TEXT COMMENT 'VLM原始返回（存档备查）',
  ai_result_json TEXT COMMENT '清洗校验后的结构化结果JSON',
  defect_count INT NOT NULL DEFAULT 0 COMMENT 'AI检出缺陷数',
  overall_result VARCHAR(16) COMMENT 'AI判定：PASS/FAIL',
  source VARCHAR(16) NOT NULL DEFAULT 'MOCK' COMMENT 'VLM/MOCK：结果来源',
  latency_ms BIGINT COMMENT 'AI调用耗时毫秒',
  status VARCHAR(16) NOT NULL DEFAULT 'AI_DONE' COMMENT 'AI_DONE待复核/CONFIRMED已复核',
  human_verdict VARCHAR(16) COMMENT '人工复核结论：PASS/FAIL',
  human_note VARCHAR(512) COMMENT '人工复核备注',
  inspector VARCHAR(32) COMMENT '复核人',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  confirmed_at DATETIME COMMENT '复核时间',
  KEY idx_product_time (product_id, create_time),
  KEY idx_batch (batch_no),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质检记录表';

CREATE TABLE defect_detail (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  record_id BIGINT NOT NULL COMMENT '质检记录id',
  defect_type VARCHAR(32) NOT NULL COMMENT '缺陷类型',
  severity VARCHAR(16) NOT NULL COMMENT 'high/medium/low',
  bbox_x1 INT COMMENT '归一化坐标 0-1000',
  bbox_y1 INT,
  bbox_x2 INT,
  bbox_y2 INT,
  confidence DECIMAL(5,4) COMMENT '置信度',
  description VARCHAR(512) COMMENT '缺陷描述',
  suggestion VARCHAR(32) COMMENT '处置建议：放行/隔离待评审/返工/报废',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_record (record_id),
  KEY idx_type (defect_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='缺陷明细表：一条记录 N 条明细，同事务写入';
