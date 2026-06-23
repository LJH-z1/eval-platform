-- =============================================================
-- 多模型回答对比与评测平台 — 数据库初始化(完整表结构)
-- 模块划分见 docs/ARCHITECTURE.md §5
-- 数据库:MySQL 8.0+
-- 字符集:utf8mb4
-- =============================================================

CREATE DATABASE IF NOT EXISTS eval_platform
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE eval_platform;

-- -------------------------------------------------------------
-- 1. user(FR-01) 负责人:刘家豪
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `username`     VARCHAR(50)  NOT NULL,
  `password`     VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密',
  `email`        VARCHAR(100),
  `role`         VARCHAR(20)  NOT NULL COMMENT 'ADMIN/ORGANIZER/SCORER/VISITOR',
  `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '1 启用 / 0 禁用',
  `failed_count` INT          NOT NULL DEFAULT 0,
  `locked_until` DATETIME,
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表(刘家豪 FR-01)';

-- -------------------------------------------------------------
-- 2. model_config(FR-02) 负责人:向锏楠
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `model_config`;
CREATE TABLE `model_config` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT,
  `name`           VARCHAR(50)   NOT NULL,
  `provider`       VARCHAR(30)   NOT NULL COMMENT 'M3/ZHIPU/QWEN/WENXIN/KIMI',
  `api_key`        VARCHAR(500)  NOT NULL COMMENT 'AES-256 加密',
  `endpoint`       VARCHAR(200)  NOT NULL,
  `model_version`  VARCHAR(50)   NOT NULL,
  `temperature`    DECIMAL(3,2)  DEFAULT 0.70,
  `top_p`          DECIMAL(3,2)  DEFAULT 0.90,
  `max_tokens`     INT           DEFAULT 2048,
  `price_per_k`    DECIMAL(8,4)  DEFAULT 0.0000 COMMENT '每千 Token 单价(元)',
  `status`         TINYINT       DEFAULT 1,
  `created_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_provider` (`provider`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型配置表(向锏楠 FR-02)';

-- -------------------------------------------------------------
-- 3. question(FR-03) 负责人:向锏楠
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `question`;
CREATE TABLE `question` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT,
  `content`         TEXT         NOT NULL,
  `category`        VARCHAR(30)  COMMENT '学科分类',
  `difficulty`      VARCHAR(10)  COMMENT '简单/中等/困难',
  `type`            VARCHAR(20)  COMMENT '事实/推理/创作/代码',
  `expected_answer` TEXT,
  `created_by`      BIGINT,
  `deleted`         TINYINT      DEFAULT 0,
  `is_public`       TINYINT      DEFAULT 0 COMMENT '1=公共题库,0=个人题库',
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问题表(向锏楠 FR-03)';

-- -------------------------------------------------------------
-- 4. evaluation(FR-04) 负责人:梁倩倩
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `evaluation`;
CREATE TABLE `evaluation` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `name`         VARCHAR(100) NOT NULL,
  `description`  TEXT,
  `created_by`   BIGINT       NOT NULL,
  `status`       VARCHAR(20)  DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/COMPLETED/FAILED',
  `model_ids`    VARCHAR(500) COMMENT '逗号分隔',
  `question_ids` VARCHAR(2000) COMMENT '逗号分隔',
  `started_at`   DATETIME,
  `finished_at`  DATETIME,
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_by` (`created_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评测表(梁倩倩 FR-04)';

-- -------------------------------------------------------------
-- 5. answer(FR-04) 负责人:梁倩倩
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `answer`;
CREATE TABLE `answer` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT,
  `evaluation_id`   BIGINT       NOT NULL,
  `question_id`     BIGINT       NOT NULL,
  `model_id`        BIGINT       NOT NULL,
  `content`         LONGTEXT     NOT NULL,
  `latency_ms`      INT          COMMENT '响应耗时(毫秒)',
  `token_input`     INT          COMMENT '输入 Token',
  `token_output`    INT          COMMENT '输出 Token',
  `estimated_cost`  DECIMAL(10,4) COMMENT '估算费用',
  `error_code`      VARCHAR(50)  COMMENT '错误码(成功为 null)',
  `error_message`   VARCHAR(500),
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_eval` (`evaluation_id`),
  KEY `idx_question` (`question_id`),
  KEY `idx_model` (`model_id`),
  UNIQUE KEY `uk_eval_question_model` (`evaluation_id`, `question_id`, `model_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='答案表(梁倩倩 FR-04)';

-- -------------------------------------------------------------
-- 6. score(FR-05) 负责人:宋子翔
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `score`;
CREATE TABLE `score` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `answer_id`  BIGINT       NOT NULL,
  `scorer_id`  BIGINT       NOT NULL,
  `accuracy`   TINYINT      NOT NULL COMMENT '1-5',
  `relevance`  TINYINT      NOT NULL,
  `fluency`    TINYINT      NOT NULL,
  `safety`     TINYINT      NOT NULL,
  `comment`    VARCHAR(500),
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_answer_scorer` (`answer_id`, `scorer_id`),
  KEY `idx_answer` (`answer_id`),
  KEY `idx_scorer` (`scorer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评分表(宋子翔 FR-05)';

-- -------------------------------------------------------------
-- 7. audit_log(FR-01) 负责人:刘家豪
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `audit_log`;
CREATE TABLE `audit_log` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT,
  `username`   VARCHAR(50),
  `action`     VARCHAR(50)  NOT NULL COMMENT 'LOGIN/LOGIN_FAIL/LOGOUT/SCORE/EXPORT/CREATE_USER',
  `target`     VARCHAR(200),
  `ip`         VARCHAR(50),
  `status`     VARCHAR(20)  COMMENT 'SUCCESS / FAIL',
  `detail`     VARCHAR(500),
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_action` (`action`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表(刘家豪 FR-01)';

-- -------------------------------------------------------------
-- 8. arena_vote(Arena 盲评投票) 负责人:刘家豪
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `arena_vote`;
CREATE TABLE `arena_vote` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT,
  `evaluation_id`   BIGINT       COMMENT '关联的快速评测 ID(可空)',
  `voter_id`        BIGINT       NOT NULL COMMENT '投票人 user_id',
  `prompt`          VARCHAR(4000) NOT NULL COMMENT '用户提问',
  `left_model_id`   BIGINT       NOT NULL COMMENT 'A 侧模型',
  `right_model_id`  BIGINT       NOT NULL COMMENT 'B 侧模型',
  `winner`          VARCHAR(10)  NOT NULL COMMENT 'A / B / tie / bad',
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_voter` (`voter_id`),
  KEY `idx_left` (`left_model_id`),
  KEY `idx_right` (`right_model_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Arena 盲评投票(刘家豪)';

-- =============================================================
-- 初始化完成
-- 接下来:
--   1. 刘家豪实现 AuthService.createUser 后,自行 INSERT admin 用户
--   2. 向锏楠实现 model 模块后,自行 INSERT 5 个示例模型
--   3. 各模块用各自 migration 增量(也可在 V1.0 中追加)
-- =============================================================
SELECT '✅ 数据库结构初始化完成' AS message;
SELECT '共 7 张表:user, model_config, question, evaluation, answer, score, audit_log' AS tables;
SELECT '⚠️  初始 admin 账号由刘家豪在实现 AuthService 后自行创建' AS notice;
