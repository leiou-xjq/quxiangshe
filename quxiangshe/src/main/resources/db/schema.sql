-- 趣享社用户表
CREATE TABLE `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  `avatar_url` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
  `bio` VARCHAR(500) DEFAULT NULL COMMENT '个人简介',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0禁用1正常2待审核',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_phone` (`phone`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 趣享社动态表
CREATE TABLE `post` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '动态ID',
  `user_id` BIGINT NOT NULL COMMENT '发布者用户ID',
  `content` TEXT NOT NULL COMMENT '动态内容',
  `media_urls` JSON DEFAULT NULL COMMENT '媒体URL数组',
  `media_types` JSON DEFAULT NULL COMMENT '媒体类型数组',
  `ai_summary` VARCHAR(500) DEFAULT NULL COMMENT 'AI摘要',
  `ai_summary_status` TINYINT NOT NULL DEFAULT 0 COMMENT 'AI摘要状态:0待生成1成功2失败',
  `visibility` TINYINT NOT NULL DEFAULT 0 COMMENT '可见性:0公开1粉丝可见2仅自己',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  `comment_count` INT NOT NULL DEFAULT 0 COMMENT '评论数',
  `share_count` INT NOT NULL DEFAULT 0 COMMENT '转发数',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除:0否1是',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_ai_summary_status` (`ai_summary_status`),
  KEY `idx_visibility_is_deleted` (`visibility`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态表';

-- 趣享社Feed流表
CREATE TABLE `feed` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `post_id` BIGINT NOT NULL COMMENT '动态ID',
  `creator_id` BIGINT NOT NULL COMMENT '动态创建者ID',
  `source_type` TINYINT NOT NULL DEFAULT 0 COMMENT '来源类型:0关注1收藏2推荐',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_post` (`user_id`, `post_id`),
  KEY `idx_user_created` (`user_id`, `created_at`),
  KEY `idx_creator_id` (`creator_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Feed流表';

-- 趣享社评论表
CREATE TABLE `comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `post_id` BIGINT NOT NULL COMMENT '动态ID',
  `user_id` BIGINT NOT NULL COMMENT '评论者用户ID',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父评论ID:0顶级评论',
  `root_id` BIGINT DEFAULT 0 COMMENT '根评论ID:顶级为自身ID',
  `content` VARCHAR(1000) NOT NULL COMMENT '评论内容',
  `at_users` JSON DEFAULT NULL COMMENT '@用户ID数组',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  `reply_count` INT NOT NULL DEFAULT 0 COMMENT '回复数',
  `time_bucket` BIGINT NOT NULL COMMENT '时间分桶:UNIX_TIMESTAMP/3600',
  `level` TINYINT NOT NULL DEFAULT 1 COMMENT '层级:1顶级2二级3+级',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除:0否1是',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_root_id` (`root_id`),
  KEY `idx_time_bucket` (`time_bucket`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_post_level` (`post_id`, `level`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- 趣享社AI摘要表
CREATE TABLE `ai_summary` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `post_id` BIGINT NOT NULL COMMENT '动态ID',
  `summary` VARCHAR(500) NOT NULL COMMENT 'AI摘要内容',
  `model` VARCHAR(50) DEFAULT NULL COMMENT '使用的AI模型',
  `tokens_used` INT DEFAULT NULL COMMENT '消耗Token数',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态:0处理中1成功2失败',
  `error_msg` VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_id` (`post_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI摘要表';

-- 趣享社用户关注关系表
CREATE TABLE `user_follow` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '关注者用户ID',
  `follow_user_id` BIGINT NOT NULL COMMENT '被关注者用户ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_follow` (`user_id`, `follow_user_id`),
  KEY `idx_follow_user_id` (`follow_user_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关注关系表';

-- 趣享社笔记表
CREATE TABLE `t_note` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '笔记ID',
  `user_id` BIGINT NOT NULL COMMENT '发布者用户ID',
  `title` VARCHAR(200) NOT NULL COMMENT '标题',
  `content` TEXT NOT NULL COMMENT '正文内容',
  `cover_image` VARCHAR(500) DEFAULT NULL COMMENT '封面图片URL',
  `category` VARCHAR(50) DEFAULT '默认' COMMENT '分类',
  `tags` VARCHAR(500) DEFAULT NULL COMMENT '标签（JSON数组）',
  `like_count` INT DEFAULT 0 COMMENT '点赞数',
  `comment_count` INT DEFAULT 0 COMMENT '评论数',
  `collect_count` INT DEFAULT 0 COMMENT '收藏数',
  `view_count` INT DEFAULT 0 COMMENT '浏览数',
  `status` TINYINT DEFAULT 0 COMMENT '状态：0=待审核，1=正常，2=违规，3=用户删除',
  `audit_status` TINYINT DEFAULT 0 COMMENT '审核状态：0=待审核，1=通过，2=拒绝',
  `reject_reason` VARCHAR(500) DEFAULT NULL COMMENT '拒绝原因',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0=正常，1=已删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_category` (`category`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_status` (`status`),
  KEY `idx_audit_status` (`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记表';

-- 笔记图片表
CREATE TABLE `t_note_image` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '图片ID',
  `note_id` BIGINT NOT NULL COMMENT '笔记ID',
  `image_url` VARCHAR(500) NOT NULL COMMENT '图片URL',
  `image_order` INT DEFAULT 0 COMMENT '图片顺序',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY `idx_note` (`note_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记图片表';

-- 敏感词表
CREATE TABLE `t_sensitive_word` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '敏感词ID',
  `word` VARCHAR(100) NOT NULL COMMENT '敏感词',
  `category` VARCHAR(50) DEFAULT '通用' COMMENT '分类',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY `uk_word` (`word`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感词表';

-- 敏感词校验日志表
CREATE TABLE `t_sensitive_check_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `note_id` BIGINT NOT NULL COMMENT '笔记ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `content_type` TINYINT DEFAULT 0 COMMENT '内容类型：0=标题，1=正文',
  `original_content` TEXT COMMENT '原始内容',
  `found_words` TEXT COMMENT '发现的敏感词（JSON数组）',
  `check_result` TINYINT DEFAULT 0 COMMENT '校验结果：0=通过，1=敏感词替换，2=拒绝',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_note_id` (`note_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感词校验日志表';

-- 插入测试敏感词
INSERT INTO t_sensitive_word (word, category) VALUES 
('色情', '违规'),
('赌博', '违规'),
('暴力', '违规'),
('毒品', '违规'),
('欺诈', '违规'),
('傻逼', '违规'),
('笨蛋', '违规'),
('废物', '违规'),
('变态', '违规'),
('恶心', '违规');

-- 笔记点赞表
CREATE TABLE `t_note_like` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '点赞记录ID',
  `note_id` BIGINT NOT NULL COMMENT '笔记ID',
  `user_id` BIGINT NOT NULL COMMENT '点赞用户ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_note_user` (`note_id`, `user_id`),
  KEY `idx_note_id` (`note_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记点赞表';

-- 笔记收藏表
CREATE TABLE `t_note_collect` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '收藏记录ID',
  `note_id` BIGINT NOT NULL COMMENT '笔记ID',
  `user_id` BIGINT NOT NULL COMMENT '收藏用户ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_note_user` (`note_id`, `user_id`),
  KEY `idx_note_id` (`note_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记收藏表';
