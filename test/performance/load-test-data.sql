-- ============================================
-- 趣享社压力测试数据准备脚本
-- 用于生成测试所需的基准数据
-- ============================================

-- 使用前请先创建数据库
-- CREATE DATABASE IF NOT EXISTS quxiangshe DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- USE quxiangshe;

-- ============================================
-- 1. 用户数据 (10,000+ 用户)
-- ============================================

INSERT INTO user (id, username, password, nickname, avatar, email, status, created_at, updated_at, version)
SELECT
    i,
    CONCAT('testuser', i),
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt/IOj2',  -- password123
    CONCAT('测试用户', i),
    CONCAT('https://api.dicebear.com/7.x/avataaars/svg?seed=', i),
    CONCAT('testuser', i, '@test.com'),
    1,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY),
    NOW(),
    0
FROM (
    SELECT @row := @row + 1 AS i
    FROM (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t1,
         (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t2,
         (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t3,
         (SELECT @row := 0) r
    LIMIT 10000
) numbers;

-- ============================================
-- 2. 笔记数据 (50,000+ 笔记)
-- ============================================

INSERT INTO note (id, user_id, title, content, images, video, video_cover, tags, location,
                  like_count, comment_count, favorite_count, view_count, forward_count, hot_score,
                  status, created_at, updated_at, version, stable_random)
SELECT
    i + 10000,  -- 笔记ID从10001开始，避免与用户ID冲突
    (i % 10000) + 1,  -- 随机分配给用户
    CONCAT('测试笔记标题 ', i),
    CONCAT('这是测试笔记的内容，用于压力测试。笔记ID: ', i, ' Lorem ipsum dolor sit amet, consectetur adipiscing elit.'),
    '[]',  -- 空图片数组
    NULL,
    NULL,
    '["测试", "压力测试"]',
    CONCAT('测试地点', i),
    FLOOR(RAND() * 1000),      -- like_count: 0-999
    FLOOR(RAND() * 500),       -- comment_count: 0-499
    FLOOR(RAND() * 300),       -- favorite_count: 0-299
    FLOOR(RAND() * 5000),     -- view_count: 0-4999
    FLOOR(RAND() * 100),      -- forward_count: 0-99
    FLOOR(RAND() * 500),      -- hot_score: 0-499
    1,  -- status: 1=正常
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 90) DAY),  -- 90天内发布
    NOW(),
    0,
    RAND()  -- stable_random: 随机排序字段
FROM (
    SELECT @note_row := @note_row + 1 AS i
    FROM (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t1,
         (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t2,
         (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t3,
         (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t4,
         (SELECT @note_row := 0) r
    LIMIT 50000
) numbers;

-- ============================================
-- 3. 关注关系数据 (100,000+ 关系)
-- ============================================

INSERT INTO follow (id, follower_id, following_id, created_at)
SELECT
    i + 100000,  -- follow ID从100001开始
    follower_id,
    following_id,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 180) DAY)  -- 180天内关注
FROM (
    SELECT
        @follow_row := @follow_row + 1 AS i,
        FLOOR(1 + RAND() * 10000) AS follower_id,
        FLOOR(1 + RAND() * 10000) AS following_id
    FROM
        (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t1,
        (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t2,
        (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t3,
        (SELECT @follow_row := 0) r
    LIMIT 100000
) numbers
WHERE follower_id != following_id  -- 排除自我关注
ON DUPLICATE KEY UPDATE id = id;  -- 忽略重复

-- ============================================
-- 4. 评论数据 (200,000+ 评论)
-- ============================================

INSERT INTO note_comment (id, note_id, user_id, parent_id, root_id, content,
                          like_count, reply_count, status, created_at, updated_at)
SELECT
    i + 200000,  -- comment ID从200001开始
    (i % 50000) + 10001,  -- note_id: 10001-60000
    (i % 10000) + 1,      -- user_id: 1-10000
    CASE
        WHEN RAND() < 0.3 THEN NULL  -- 30%为根评论
        ELSE FLOOR(200001 + RAND() * (i - 1))  -- 70%为子评论
    END AS parent_id,
    CASE
        WHEN RAND() < 0.3 THEN NULL  -- 30%为根评论，root_id为NULL
        ELSE FLOOR(200001 + RAND() * i)  -- 70%的子评论
    END AS root_id,
    CONCAT('这是测试评论内容，用于压力测试。评论ID: ', i, ' Great content!'),
    FLOOR(RAND() * 100),   -- like_count: 0-99
    FLOOR(RAND() * 50),    -- reply_count: 0-49
    1,  -- status: 1=正常
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 60) DAY),  -- 60天内评论
    NOW()
FROM (
    SELECT @comment_row := @comment_row + 1 AS i
    FROM (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t1,
         (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t2,
         (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t3,
         (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t4,
         (SELECT @comment_row := 0) r
    LIMIT 200000
) numbers;

-- ============================================
-- 5. 点赞关系数据 (100,000+ 点赞)
-- ============================================

INSERT INTO note_like (id, user_id, note_id, created_at)
SELECT
    i + 300000,
    (i % 10000) + 1,
    (i % 50000) + 10001,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 90) DAY)
FROM (
    SELECT @like_row := @like_row + 1 AS i
    FROM (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t1,
         (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t2,
         (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t3,
         (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t4,
         (SELECT @like_row := 0) r
    LIMIT 100000
) numbers
ON DUPLICATE KEY UPDATE id = id;

-- ============================================
-- 6. 收藏关系数据 (50,000+ 收藏)
-- ============================================

INSERT INTO note_favorite (id, user_id, note_id, created_at)
SELECT
    i + 400000,
    (i % 10000) + 1,
    (i % 50000) + 10001,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 90) DAY)
FROM (
    SELECT @fav_row := @fav_row + 1 AS i
    FROM (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t1,
         (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t2,
         (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t3,
         (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t4,
         (SELECT @fav_row := 0) r
    LIMIT 50000
) numbers
ON DUPLICATE KEY UPDATE id = id;

-- ============================================
-- 7. 索引优化（提升查询性能）
-- ============================================

-- 用户索引
CREATE INDEX idx_user_status ON user(status);
CREATE INDEX idx_user_created_at ON user(created_at);

-- 笔记索引
CREATE INDEX idx_note_user_id ON note(user_id);
CREATE INDEX idx_note_status ON note(status);
CREATE INDEX idx_note_created_at ON note(created_at);
CREATE INDEX idx_note_hot_score ON note(hot_score);
CREATE INDEX idx_note_like_count ON note(like_count);

-- 关注索引
CREATE INDEX idx_follow_follower ON follow(follower_id);
CREATE INDEX idx_follow_following ON follow(following_id);
CREATE UNIQUE INDEX idx_follow_relation ON follow(follower_id, following_id);

-- 评论索引
CREATE INDEX idx_comment_note_id ON note_comment(note_id);
CREATE INDEX idx_comment_user_id ON note_comment(user_id);
CREATE INDEX idx_comment_root_id ON note_comment(root_id);
CREATE INDEX idx_comment_parent_id ON note_comment(parent_id);
CREATE INDEX idx_comment_status ON note_comment(status);
CREATE INDEX idx_comment_created_at ON note_comment(created_at);

-- 点赞索引
CREATE INDEX idx_note_like_user_note ON note_like(user_id, note_id);
CREATE INDEX idx_note_like_note_id ON note_like(note_id);

-- 收藏索引
CREATE INDEX idx_note_favorite_user_note ON note_favorite(user_id, note_id);
CREATE INDEX idx_note_favorite_note_id ON note_favorite(note_id);

-- ============================================
-- 8. 数据验证查询
-- ============================================

-- SELECT '用户数据' AS item, COUNT(*) AS count FROM user;
-- SELECT '笔记数据' AS item, COUNT(*) AS count FROM note;
-- SELECT '关注关系' AS item, COUNT(*) AS count FROM follow;
-- SELECT '评论数据' AS item, COUNT(*) AS count FROM note_comment;
-- SELECT '点赞数据' AS item, COUNT(*) AS count FROM note_like;
-- SELECT '收藏数据' AS item, COUNT(*) AS count FROM note_favorite;

-- ============================================
-- 9. Redis 热点数据预热（可选）
-- ============================================
-- 以下为Redis预热脚本，可在压测前手动执行

-- 预热Feed收件箱
-- for i in {1..1000}; do curl -s "http://localhost:8080/api/feed?userId=$i&size=20" > /dev/null; done

-- 预热热点笔记
-- for i in {1..100}; do curl -s "http://localhost:8080/api/note/popular?size=20" > /dev/null; done

-- ============================================
-- 执行完成
-- ============================================
