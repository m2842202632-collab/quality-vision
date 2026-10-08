DROP TABLE IF EXISTS defect_detail;
DROP TABLE IF EXISTS inspection_record;
DROP TABLE IF EXISTS inspection_standard;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(32) NOT NULL,
  password_hash VARCHAR(128) NOT NULL,
  salt VARCHAR(64) NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_username (username)
);

CREATE TABLE product (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(64) NOT NULL,
  spec VARCHAR(128),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_code (code)
);

CREATE TABLE inspection_standard (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  check_item VARCHAR(64) NOT NULL,
  defect_types TEXT,
  severity_rules VARCHAR(512),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_product (product_id)
);

CREATE TABLE inspection_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  batch_no VARCHAR(32) NOT NULL,
  image_path VARCHAR(256),
  ai_raw_response TEXT,
  ai_result_json TEXT,
  defect_count INT NOT NULL DEFAULT 0,
  overall_result VARCHAR(16),
  source VARCHAR(16) NOT NULL DEFAULT 'MOCK',
  latency_ms BIGINT,
  status VARCHAR(16) NOT NULL DEFAULT 'AI_DONE',
  human_verdict VARCHAR(16),
  human_note VARCHAR(512),
  inspector VARCHAR(32),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  confirmed_at DATETIME,
  KEY idx_product_time (product_id, create_time),
  KEY idx_batch (batch_no),
  KEY idx_status (status)
);

CREATE TABLE defect_detail (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  record_id BIGINT NOT NULL,
  defect_type VARCHAR(32) NOT NULL,
  severity VARCHAR(16) NOT NULL,
  bbox_x1 INT,
  bbox_y1 INT,
  bbox_x2 INT,
  bbox_y2 INT,
  confidence DECIMAL(5,4),
  description VARCHAR(512),
  suggestion VARCHAR(32),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_record (record_id),
  KEY idx_type (defect_type)
);
