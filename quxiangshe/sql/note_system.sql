-- =============================================
-- 趣享社笔记系统数据库表
-- 创建时间: 2026-04-03
-- =============================================

-- 笔记表
DROP TABLE IF EXISTS t_note;
CREATE TABLE t_note (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '笔记ID',
    user_id BIGINT NOT NULL COMMENT '发布者用户ID',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    content TEXT NOT NULL COMMENT '正文内容',
    cover_image VARCHAR(500) DEFAULT NULL COMMENT '封面图片URL',
    category VARCHAR(50) DEFAULT '默认' COMMENT '分类',
    tags VARCHAR(500) DEFAULT NULL COMMENT '标签（JSON数组）',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    comment_count INT DEFAULT 0 COMMENT '评论数',
    collect_count INT DEFAULT 0 COMMENT '收藏数',
    view_count INT DEFAULT 0 COMMENT '浏览数',
    status TINYINT DEFAULT 0 COMMENT '状态：0=待审核，1=正常，2=违规，3=用户删除',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0=正常，1=已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user (user_id),
    INDEX idx_category (category),
    INDEX idx_create_time (create_time),
    INDEX idx_deleted (deleted),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '笔记表';

-- 笔记图片表
DROP TABLE IF EXISTS t_note_image;
CREATE TABLE t_note_image (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '图片ID',
    note_id BIGINT NOT NULL COMMENT '笔记ID',
    image_url VARCHAR(500) NOT NULL COMMENT '图片URL',
    image_order INT DEFAULT 0 COMMENT '图片顺序',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_note (note_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '笔记图片表';

-- 笔记点赞记录表
DROP TABLE IF EXISTS t_note_like;
CREATE TABLE t_note_like (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '点赞记录ID',
    note_id BIGINT NOT NULL COMMENT '笔记ID',
    user_id BIGINT NOT NULL COMMENT '点赞用户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    UNIQUE KEY uk_note_user (note_id, user_id),
    INDEX idx_note (note_id),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '笔记点赞记录表';

-- 笔记收藏记录表
DROP TABLE IF EXISTS t_note_collect;
CREATE TABLE t_note_collect (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '收藏记录ID',
    note_id BIGINT NOT NULL COMMENT '笔记ID',
    user_id BIGINT NOT NULL COMMENT '收藏用户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    UNIQUE KEY uk_note_user (note_id, user_id),
    INDEX idx_note (note_id),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '笔记收藏记录表';
