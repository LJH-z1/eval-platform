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
