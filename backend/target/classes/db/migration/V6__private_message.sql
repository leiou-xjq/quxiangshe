-- ============================================================
-- 私信功能数据库迁移
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 私信会话表
-- ----------------------------
DROP TABLE IF EXISTS `private_message_session`;
CREATE TABLE `private_message_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `target_user_id` BIGINT NOT NULL COMMENT '对方用户ID',
  `last_message_id` BIGINT DEFAULT NULL COMMENT '最后一条消息ID',
  `last_message_content` VARCHAR(500) DEFAULT NULL COMMENT '最后消息摘要',
  `last_message_time` DATETIME DEFAULT NULL COMMENT '最后消息时间',
  `unread_count` INT DEFAULT 0 COMMENT '未读消息数',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_target` (`user_id`, `target_user_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_last_message_time` (`last_message_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='私信会话表';

-- ----------------------------
-- 2. 私信消息表
-- ----------------------------
DROP TABLE IF EXISTS `private_message`;
CREATE TABLE `private_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `session_id` BIGINT NOT NULL COMMENT '会话ID',
  `sender_id` BIGINT NOT NULL COMMENT '发送者ID',
  `receiver_id` BIGINT NOT NULL COMMENT '接收者ID',
  `message_type` TINYINT NOT NULL DEFAULT 1 COMMENT '消息类型: 1-文字 2-图片 3-表情',
  `content` TEXT COMMENT '消息内容',
  `image_url` VARCHAR(500) DEFAULT NULL COMMENT '图片URL',
  `is_recalled` TINYINT DEFAULT 0 COMMENT '是否被撤回: 0-否 1-是',
  `recall_time` DATETIME DEFAULT NULL COMMENT '撤回时间',
  `is_deleted_sender` TINYINT DEFAULT 0 COMMENT '发送方是否删除',
  `is_deleted_receiver` TINYINT DEFAULT 0 COMMENT '接收方是否删除',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_sender_id` (`sender_id`),
  KEY `idx_receiver_id` (`receiver_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='私信消息表';

-- ----------------------------
-- 3. 私信消息归档表
-- ----------------------------
DROP TABLE IF EXISTS `private_message_archive`;
CREATE TABLE `private_message_archive` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '归档消息ID',
  `session_id` BIGINT NOT NULL COMMENT '会话ID',
  `sender_id` BIGINT NOT NULL COMMENT '发送者ID',
  `receiver_id` BIGINT NOT NULL COMMENT '接收者ID',
  `message_type` TINYINT NOT NULL DEFAULT 1 COMMENT '消息类型',
  `content` TEXT COMMENT '消息内容',
  `image_url` VARCHAR(500) DEFAULT NULL COMMENT '图片URL',
  `created_at` DATETIME DEFAULT NULL COMMENT '发送时间',
  `archived_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '归档时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_archived_at` (`archived_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='私信消息归档表';

SET FOREIGN_KEY_CHECKS = 1;