-- =============================================
-- 趣享社评论系统数据库表
-- 创建时间: 2026-04-03
-- =============================================

-- 评论表
DROP TABLE IF EXISTS t_comment;
CREATE TABLE t_comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评论ID',
    target_id BIGINT DEFAULT 0 COMMENT '回复的目标评论ID（0=一级评论）',
    target_user_id BIGINT DEFAULT NULL COMMENT '被回复的用户ID',
    article_id BIGINT NOT NULL COMMENT '文章/内容ID',
    user_id BIGINT NOT NULL COMMENT '评论用户ID',
    content TEXT NOT NULL COMMENT '评论内容',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    reply_count INT DEFAULT 0 COMMENT '回复数（一级评论的回复数）',
    status TINYINT DEFAULT 1 COMMENT '状态：0=待审核，1=正常，2=敏感词替换',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0=正常，1=已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_article (article_id),
    INDEX idx_user (user_id),
    INDEX idx_target (target_id),
    INDEX idx_article_deleted (article_id, deleted),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '评论表';

-- 评论点赞记录表
DROP TABLE IF EXISTS t_comment_like;
CREATE TABLE t_comment_like (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '点赞记录ID',
    comment_id BIGINT NOT NULL COMMENT '评论ID',
    user_id BIGINT NOT NULL COMMENT '点赞用户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    UNIQUE KEY uk_comment_user (comment_id, user_id),
    INDEX idx_comment (comment_id),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '评论点赞记录表';

-- 敏感词表
DROP TABLE IF EXISTS t_sensitive_word;
CREATE TABLE t_sensitive_word (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '敏感词ID',
    word VARCHAR(100) NOT NULL COMMENT '敏感词',
    category VARCHAR(50) DEFAULT '通用' COMMENT '分类',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_word (word)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '敏感词表';

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
('恶心', '违规'),
('讨厌', '违规'),
('滚蛋', '违规'),
('去死', '违规'),
('弱智', '违规'),
('智障', '违规');
