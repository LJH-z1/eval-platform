-- 启动验证用的 H2 数据
-- admin 用户的 BCrypt 密码(明文 "admin123"):$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
INSERT INTO `user` (id, username, password, email, role, status) VALUES
  (1, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'admin@test.local', 'ADMIN', 1),
  (2, 'org1',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'org1@test.local',  'ORGANIZER', 1),
  (3, 'scorer1','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'scorer1@test.local','SCORER', 1);
