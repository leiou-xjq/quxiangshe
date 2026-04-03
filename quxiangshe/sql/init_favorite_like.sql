-- 趣享社 点赞收藏功能数据库脚本
-- 执行顺序: 1. 先执行此脚本创建表 2. 重启后端服务

-- =============================================
-- 收藏表 t_post_favorite
-- =============================================
CREATE TABLE IF NOT EXISTS t_post_favorite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    post_id BIGINT NOT NULL COMMENT '帖子ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    is_deleted TINYINT DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    UNIQUE KEY uk_user_post (user_id, post_id),
    INDEX idx_user_id (user_id),
    INDEX idx_post_id (post_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子收藏表';

-- =============================================
-- 说明
-- =============================================
-- 点赞数据存储在Redis中，使用ZSet有序集合:
--   - post:like:{postId} - 存储点赞用户，score为点赞时间戳
--   - post:like:count:{postId} - 存储点赞数
--   - user:like:{userId} - 存储用户点赞的帖子ID
--
-- 收藏数据存储在MySQL t_post_favorite表中，Redis作为缓存:
--   - post:favorite:count:{postId} - 缓存收藏数
--   - user:favorite:{userId} - 缓存用户收藏的帖子ID集合