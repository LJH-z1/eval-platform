-- H2 兼容的初始化脚本(用于 dev profile 启动验证)
SET MODE MySQL;

-- user(H2 关键字,需用引号或改名)
CREATE TABLE IF NOT EXISTS "user" (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(100) NOT NULL,
  email VARCHAR(100),
  role VARCHAR(20) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  failed_count INT NOT NULL DEFAULT 0,
  locked_until TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO "user" (id, username, password, email, role, status) VALUES
  (1, 'admin', '$2a$10$Oe33tH5qPJRTuWzpvAbLxeiIibqfZ.YwV/X3AQGCWPDoOP2UnORGy', 'admin@test.local', 'ADMIN', 1),
  (2, 'org1',  '$2a$10$Oe33tH5qPJRTuWzpvAbLxeiIibqfZ.YwV/X3AQGCWPDoOP2UnORGy', 'org1@test.local',  'ORGANIZER', 1);

-- question
CREATE TABLE IF NOT EXISTS question (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  content TEXT NOT NULL,
  category VARCHAR(30),
  difficulty VARCHAR(10),
  type VARCHAR(20),
  expected_answer TEXT,
  created_by BIGINT,
  deleted TINYINT DEFAULT 0,
  is_public TINYINT DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- audit_log
CREATE TABLE IF NOT EXISTS audit_log (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT,
  username VARCHAR(50),
  action VARCHAR(50) NOT NULL,
  target VARCHAR(200),
  ip VARCHAR(50),
  status VARCHAR(20),
  detail VARCHAR(500),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- model_config(FR-02)
CREATE TABLE IF NOT EXISTS model_config (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(50) NOT NULL UNIQUE,
  provider VARCHAR(20) NOT NULL,
  api_key VARCHAR(500) NOT NULL,
  endpoint VARCHAR(500),
  model_version VARCHAR(100),
  temperature DECIMAL(3,2) DEFAULT 0.70,
  top_p DECIMAL(3,2) DEFAULT 0.90,
  max_tokens INT DEFAULT 2048,
  price_per_k DECIMAL(10,4),
  status TINYINT NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- evaluation(FR-04)
CREATE TABLE IF NOT EXISTS evaluation (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  description TEXT,
  created_by BIGINT,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  model_ids VARCHAR(500),
  question_ids VARCHAR(2000),
  started_at TIMESTAMP,
  finished_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- answer(FR-04)
CREATE TABLE IF NOT EXISTS answer (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  evaluation_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  model_id BIGINT NOT NULL,
  content LONGTEXT,
  latency_ms INT,
  token_input INT,
  token_output INT,
  estimated_cost DECIMAL(10,4),
  error_code VARCHAR(50),
  error_message VARCHAR(500),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- score(FR-05 占位) — value 是 H2 关键字,改名 score_value
CREATE TABLE IF NOT EXISTS score (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  answer_id BIGINT NOT NULL,
  dimension VARCHAR(50) NOT NULL,
  score_value DECIMAL(4,2),
  scorer_id BIGINT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

SELECT 'init-h2 done' AS status;
