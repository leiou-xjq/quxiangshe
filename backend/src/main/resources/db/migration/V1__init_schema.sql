-- ============================================================
-- 趣享社 V1: 基础表结构初始化
-- 创建日期: 2026-04-30
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 用户表
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `password` VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
  `wechat_open_id` VARCHAR(100) DEFAULT NULL COMMENT '微信OpenID',
  `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  `gender` TINYINT DEFAULT 0 COMMENT '性别: 0-未知, 1-男, 2-女',
  `birthday` DATE DEFAULT NULL COMMENT '生日',
  `bio` VARCHAR(500) DEFAULT NULL COMMENT '个人简介',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常, 2-待审核',
  `role` VARCHAR(20) DEFAULT 'USER' COMMENT '角色: USER/MODERATOR/ADMIN',
  `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
  `last_login_at` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间(软删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_phone` (`phone`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ----------------------------
-- 2. 用户会话表
-- ----------------------------
DROP TABLE IF EXISTS `user_session`;
CREATE TABLE `user_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `session_type` VARCHAR(20) NOT NULL COMMENT '会话类型: access/refresh',
  `token` VARCHAR(500) NOT NULL COMMENT 'Token值',
  `device_info` VARCHAR(200) DEFAULT NULL COMMENT '设备信息',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` VARCHAR(500) DEFAULT NULL COMMENT '用户代理',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-无效, 1-有效',
  `expires_at` DATETIME NOT NULL COMMENT '过期时间',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_token` (`token`),
  KEY `idx_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户会话表';

-- ----------------------------
-- 3. 用户活跃度表
-- ----------------------------
DROP TABLE IF EXISTS `user_activity`;
CREATE TABLE `user_activity` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `login_days` INT DEFAULT 0 COMMENT '累计登录天数',
  `interaction_count` INT DEFAULT 0 COMMENT '累计互动次数',
  `activity_score` DOUBLE DEFAULT 0 COMMENT '活跃分数',
  `last_login_date` DATE DEFAULT NULL COMMENT '上次登录日期',
  `today_interaction_count` INT DEFAULT 0 COMMENT '今日互动次数',
  `today_interaction_date` DATE DEFAULT NULL COMMENT '今日互动日期',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_activity_score` (`activity_score`),
  KEY `idx_last_login_date` (`last_login_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户活跃度表';

-- ----------------------------
-- 4. 关注关系表
-- ----------------------------
DROP TABLE IF EXISTS `follow`;
CREATE TABLE `follow` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '关注ID',
  `follower_id` BIGINT NOT NULL COMMENT '关注者用户ID',
  `following_id` BIGINT NOT NULL COMMENT '被关注者用户ID',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_follow` (`follower_id`, `following_id`),
  KEY `idx_follower_id` (`follower_id`),
  KEY `idx_following_id` (`following_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='关注关系表';

-- ----------------------------
-- 5. 笔记表
-- ----------------------------
DROP TABLE IF EXISTS `note`;
CREATE TABLE `note` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '笔记ID',
  `user_id` BIGINT NOT NULL COMMENT '发布者用户ID',
  `title` VARCHAR(200) DEFAULT NULL COMMENT '笔记标题',
  `content` TEXT NOT NULL COMMENT '笔记内容',
  `images` TEXT COMMENT '图片JSON数组',
  `video` VARCHAR(500) DEFAULT NULL COMMENT '视频URL',
  `video_cover` VARCHAR(500) DEFAULT NULL COMMENT '视频封面URL',
  `tags` VARCHAR(500) DEFAULT NULL COMMENT '标签JSON数组',
  `location` VARCHAR(200) DEFAULT NULL COMMENT '地理位置',
  `stable_random` DECIMAL(16,6) DEFAULT NULL COMMENT '稳定随机排序字段',
  `like_count` INT DEFAULT 0 COMMENT '点赞数',
  `comment_count` INT DEFAULT 0 COMMENT '评论数',
  `favorite_count` INT DEFAULT 0 COMMENT '收藏数',
  `view_count` INT DEFAULT 0 COMMENT '浏览数',
  `forward_count` INT DEFAULT 0 COMMENT '转发数',
  `status` TINYINT DEFAULT 0 COMMENT '状态: 0-待审核, 1-正常, 2-违规下架',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间(软删除)',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_stable_random` (`stable_random`),
  KEY `idx_video` (`video`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记表';

-- ----------------------------
-- 6. 笔记点赞表
-- ----------------------------
DROP TABLE IF EXISTS `note_like`;
CREATE TABLE `note_like` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `note_id` BIGINT NOT NULL COMMENT '笔记ID',
  `user_id` BIGINT NOT NULL COMMENT '点赞用户ID',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_note_user` (`note_id`, `user_id`),
  KEY `idx_note_id` (`note_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记点赞表';

-- ----------------------------
-- 7. 笔记收藏表
-- ----------------------------
DROP TABLE IF EXISTS `note_favorite`;
CREATE TABLE `note_favorite` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `note_id` BIGINT NOT NULL COMMENT '笔记ID',
  `user_id` BIGINT NOT NULL COMMENT '收藏用户ID',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_note_user` (`note_id`, `user_id`),
  KEY `idx_note_id` (`note_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记收藏表';

-- ----------------------------
-- 8. 笔记评论表（支持树形结构）
-- ----------------------------
DROP TABLE IF EXISTS `note_comment`;
CREATE TABLE `note_comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `note_id` BIGINT NOT NULL COMMENT '笔记ID',
  `user_id` BIGINT NOT NULL COMMENT '评论者用户ID',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父评论ID: 0-根评论, >0-回复',
  `root_id` BIGINT DEFAULT 0 COMMENT '所属根评论ID: 0-根评论本身, >0-属于哪个根评论',
  `content` TEXT NOT NULL COMMENT '评论内容',
  `like_count` INT DEFAULT 0 COMMENT '点赞数',
  `reply_count` INT DEFAULT 0 COMMENT '回复数',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-待审核, 1-正常, 2-违规',
  `hot_score` DOUBLE DEFAULT 0 COMMENT '热度值=点赞数*2+评论数*3',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间(软删除)',
  PRIMARY KEY (`id`),
  KEY `idx_note_id` (`note_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_root_id` (`root_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记评论表';

-- ----------------------------
-- 9. 评论点赞表
-- ----------------------------
DROP TABLE IF EXISTS `comment_like`;
CREATE TABLE `comment_like` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `comment_id` BIGINT NOT NULL COMMENT '评论ID',
  `user_id` BIGINT NOT NULL COMMENT '点赞用户ID',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_comment_user` (`comment_id`, `user_id`),
  KEY `idx_comment_id` (`comment_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论点赞表';

-- ----------------------------
-- 10. 转发记录表
-- ----------------------------
DROP TABLE IF EXISTS `note_forward`;
CREATE TABLE `note_forward` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '转发ID',
  `original_note_id` BIGINT NOT NULL COMMENT '原笔记ID',
  `user_id` BIGINT NOT NULL COMMENT '转发者用户ID',
  `content` TEXT COMMENT '转发时的评论内容',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_original_note_id` (`original_note_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='转发记录表';

-- ----------------------------
-- 11. 推送日志表
-- ----------------------------
DROP TABLE IF EXISTS `feed_push_log`;
CREATE TABLE `feed_push_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `note_id` BIGINT NOT NULL COMMENT '笔记ID',
  `author_id` BIGINT NOT NULL COMMENT '作者ID',
  `target_user_id` BIGINT NOT NULL COMMENT '接收推送的用户ID',
  `push_mode` TINYINT DEFAULT 1 COMMENT '推送模式: 1-推模式, 2-拉模式, 3-推拉结合',
  `push_status` TINYINT DEFAULT 1 COMMENT '推送状态: 0-失败, 1-成功',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_note_id` (`note_id`),
  KEY `idx_author_id` (`author_id`),
  KEY `idx_target_user_id` (`target_user_id`),
  KEY `idx_push_status` (`push_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='推送日志表';

-- ----------------------------
-- 12. 举报记录表
-- ----------------------------
DROP TABLE IF EXISTS `report`;
CREATE TABLE `report` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `reporter_id` BIGINT NOT NULL COMMENT '举报者ID',
  `target_type` TINYINT NOT NULL COMMENT '目标类型: 1-笔记, 2-评论, 3-用户',
  `target_id` BIGINT NOT NULL COMMENT '被举报的目标ID',
  `reason` TINYINT NOT NULL COMMENT '举报原因: 1-垃圾广告, 2-涉黄, 3-抄袭, 4-其他',
  `description` TEXT COMMENT '详细描述',
  `status` TINYINT DEFAULT 0 COMMENT '状态: 0-待处理, 1-已处理, 2-已驳回',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `handled_at` DATETIME DEFAULT NULL COMMENT '处理时间',
  PRIMARY KEY (`id`),
  KEY `idx_reporter_id` (`reporter_id`),
  KEY `idx_target` (`target_type`, `target_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='举报记录表';

-- ----------------------------
-- 13. 黑名单表
-- ----------------------------
DROP TABLE IF EXISTS `blacklist`;
CREATE TABLE `blacklist` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID(拉黑者)',
  `blocked_id` BIGINT NOT NULL COMMENT '被拉黑的用户ID',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_blocked` (`user_id`, `blocked_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_blocked_id` (`blocked_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='黑名单表';

-- ----------------------------
-- 14. 通知表
-- ----------------------------
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `user_id` BIGINT NOT NULL COMMENT '接收通知的用户ID',
  `type` TINYINT NOT NULL COMMENT '通知类型: 1-点赞, 2-评论, 3-关注, 4-系统',
  `from_user_id` BIGINT DEFAULT NULL COMMENT '触发通知的用户ID',
  `note_id` BIGINT DEFAULT NULL COMMENT '相关笔记ID',
  `comment_id` BIGINT DEFAULT NULL COMMENT '相关评论ID',
  `content` VARCHAR(500) DEFAULT NULL COMMENT '通知内容',
  `is_read` TINYINT DEFAULT 0 COMMENT '是否已读: 0-未读, 1-已读',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_type` (`type`),
  KEY `idx_is_read` (`is_read`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

-- ----------------------------
-- 15. 操作日志表
-- ----------------------------
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '操作用户ID',
  `username` VARCHAR(50) DEFAULT NULL COMMENT '操作用户名',
  `module` VARCHAR(50) DEFAULT NULL COMMENT '操作模块',
  `operation` VARCHAR(50) DEFAULT NULL COMMENT '操作类型',
  `method` VARCHAR(200) DEFAULT NULL COMMENT '请求方法',
  `endpoint` VARCHAR(500) DEFAULT NULL COMMENT '请求路径',
  `request_method` VARCHAR(10) DEFAULT NULL COMMENT 'HTTP请求方法',
  `request_params` TEXT COMMENT '请求参数',
  `request_body` TEXT COMMENT '请求体',
  `response_status` INT DEFAULT NULL COMMENT '响应状态码',
  `response_body` TEXT COMMENT '响应体',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT '客户端IP地址',
  `user_agent` VARCHAR(500) DEFAULT NULL COMMENT '用户代理',
  `execution_time` INT DEFAULT NULL COMMENT '执行耗时(毫秒)',
  `status` TINYINT DEFAULT 1 COMMENT '操作状态: 0-失败, 1-成功',
  `error_message` TEXT COMMENT '错误信息',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_module` (`module`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

SET FOREIGN_KEY_CHECKS = 1;

-- 创建管理员用户 (密码: admin123)
INSERT INTO `user` (`id`, `username`, `nickname`, `avatar`, `password`, `gender`, `bio`, `status`, `role`) VALUES
(1, 'admin', '管理员', 'https://picsum.photos/200', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EH', 0, '系统管理员', 1, 'ADMIN');