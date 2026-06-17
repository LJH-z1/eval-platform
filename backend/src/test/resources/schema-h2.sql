-- H2 test schema (与 MySQL 兼容模式下使用)

CREATE TABLE IF NOT EXISTS `user` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `username`     VARCHAR(50)  NOT NULL,
  `password`     VARCHAR(100) NOT NULL,
  `email`        VARCHAR(100),
  `role`         VARCHAR(20)  NOT NULL,
  `status`       TINYINT      NOT NULL DEFAULT 1,
  `failed_count` INT          NOT NULL DEFAULT 0,
  `locked_until` TIMESTAMP,
  `created_at`   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
);

CREATE TABLE IF NOT EXISTS `audit_log` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT,
  `username`   VARCHAR(50),
  `action`     VARCHAR(50)  NOT NULL,
  `target`     VARCHAR(200),
  `ip`         VARCHAR(50),
  `status`     VARCHAR(20),
  `detail`     VARCHAR(500),
  `created_at` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
);
