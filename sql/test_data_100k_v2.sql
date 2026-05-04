-- ============================================================
-- 趣享社 超大规模测试数据生成脚本（优化版）
-- 数据规模: 10万用户, 2万笔记, 20万评论, 20万关注
-- 使用高效的批量INSERT方式
-- ============================================================

USE quxiangshe;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 准备阶段：清理现有数据
-- ============================================================
TRUNCATE TABLE note_comment;
TRUNCATE TABLE comment_like;
TRUNCATE TABLE note_like;
TRUNCATE TABLE note_favorite;
TRUNCATE TABLE note_forward;
TRUNCATE TABLE note;
TRUNCATE TABLE follow;
TRUNCATE TABLE user_activity;
DELETE FROM user WHERE id >= 7;

-- ============================================================
-- 第一部分：生成用户（使用数字表批量插入）
-- ============================================================

-- 创建数字辅助表
CREATE TEMPORARY TABLE IF NOT EXISTS numbers (num INT);
TRUNCATE TABLE numbers;
INSERT INTO numbers VALUES (0),(1),(2),(3),(4),(5),(6),(7),(8),(9);

-- 插入6个博主
INSERT IGNORE INTO user (id, username, nickname, avatar, password, gender, bio, status, role, created_at, updated_at) VALUES
(1, 'admin', '管理员', 'https://picsum.photos/200', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EH', 0, '系统管理员', 1, 'ADMIN', NOW(), NOW()),
(2, 'xiaoboker1', '穿搭达人小美', 'https://picsum.photos/201', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EH', 2, '分享每日穿搭', 1, 'USER', NOW(), NOW()),
(3, 'zhongboker1', '美食家大V', 'https://picsum.photos/202', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EH', 1, '美食博主', 1, 'USER', NOW(), NOW()),
(4, 'zhongboker2', '旅行摄影师', 'https://picsum.photos/203', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EH', 1, '旅行摄影', 1, 'USER', NOW(), NOW()),
(5, 'daboker1', '超级网红', 'https://picsum.photos/204', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EH', 2, '千万粉丝博主', 1, 'USER', NOW(), NOW()),
(6, 'daboker2', '知名博主', 'https://picsum.photos/205', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EH', 1, '知名博主', 1, 'USER', NOW(), NOW());

-- 批量生成10万普通用户（使用交叉连接，直接生成）
INSERT INTO user (username, nickname, avatar, password, gender, bio, status, created_at, updated_at)
SELECT 
    CONCAT('user', n.num) AS username,
    CONCAT('用户', n.num) AS nickname,
    CONCAT('https://picsum.photos/', 200 + (n.num % 800)) AS avatar,
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EH' AS password,
    FLOOR(RAND() * 3) AS gender,
    '测试用户' AS bio,
    1 AS status,
    NOW() - INTERVAL FLOOR(RAND() * 365) DAY,
    NOW()
FROM (
    SELECT (a1.num + a2.num*10 + a3.num*100 + a4.num*1000 + a5.num*10000) AS num
    FROM 
        (SELECT 0 num UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a1,
        (SELECT 0 num UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a2,
        (SELECT 0 num UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a3,
        (SELECT 0 num UNION SELECT 4 UNION SELECT 8 UNION SELECT 2 UNION SELECT 6) a4,
        (SELECT 0 num UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a5
    LIMIT 100000
) n;

SELECT CONCAT('已生成 ', COUNT(*), ' 个用户') AS user_count FROM user;

-- 生成用户活跃度
INSERT INTO user_activity (user_id, login_days, interaction_count, last_login_date, today_interaction_count, today_interaction_date, activity_score, created_at, updated_at)
SELECT 
    id,
    FLOOR(1 + RAND() * 30),
    FLOOR(RAND() * 100),
    CURDATE() - INTERVAL FLOOR(RAND() * 30) DAY,
    FLOOR(RAND() * 10),
    CURDATE() - INTERVAL FLOOR(RAND() * 7) DAY,
    20 + RAND() * 180,
    NOW(),
    NOW()
FROM user WHERE id >= 7;

-- ============================================================
-- 第二部分：生成笔记
-- ============================================================

-- 基础笔记（25篇）
INSERT INTO note (user_id, title, content, location, images, tags, like_count, comment_count, favorite_count, view_count, forward_count, status, stable_random, created_at, updated_at) VALUES
(2, '今日穿搭｜简约黑色连衣裙', '黑色连衣裙+小白鞋，简约时尚', '上海', '["https://picsum.photos/400/300"]', '["穿搭"]', 100, 10, 50, 1000, 20, 1, RAND(), NOW() - INTERVAL 30 DAY, NOW()),
(2, '通勤穿搭｜白衬衫+牛仔裤', '经典搭配，永不过时', '北京', '["https://picsum.photos/400/301"]', '["穿搭"]', 80, 8, 40, 800, 15, 1, RAND(), NOW() - INTERVAL 29 DAY, NOW()),
(2, '春季外套推荐', '早春外套合集', '杭州', '["https://picsum.photos/400/302"]', '["穿搭"]', 60, 6, 30, 600, 10, 1, RAND(), NOW() - INTERVAL 28 DAY, NOW()),
(2, '配色技巧', '穿衣配色技巧分享', '家中', '["https://picsum.photos/400/303"]', '["穿搭"]', 50, 5, 25, 500, 8, 1, RAND(), NOW() - INTERVAL 27 DAY, NOW()),
(2, '基础款穿搭', '基础款也能穿出时尚感', '家中', '["https://picsum.photos/400/304"]', '["穿搭"]', 40, 4, 20, 400, 5, 1, RAND(), NOW() - INTERVAL 26 DAY, NOW()),
(3, '探店｜宝藏火锅店', '人均30元火锅店推荐', '成都', '["https://picsum.photos/400/305"]', '["美食"]', 500, 50, 250, 5000, 100, 1, RAND(), NOW() - INTERVAL 25 DAY, NOW()),
(3, '家常菜教程', '蒜香排骨做法', '家中', '["https://picsum.photos/400/306"]', '["美食"]', 400, 40, 200, 4000, 80, 1, RAND(), NOW() - INTERVAL 24 DAY, NOW()),
(3, '早餐推荐', '快手早餐合集', '家中', '["https://picsum.photos/400/307"]', '["美食"]', 300, 30, 150, 3000, 60, 1, RAND(), NOW() - INTERVAL 23 DAY, NOW()),
(3, '甜品做法', '杨枝甘露在家做', '家中', '["https://picsum.photos/400/308"]', '["美食"]', 250, 25, 125, 2500, 50, 1, RAND(), NOW() - INTERVAL 22 DAY, NOW()),
(3, '减肥餐', '健康减肥餐推荐', '家中', '["https://picsum.photos/400/309"]', '["美食"]', 200, 20, 100, 2000, 40, 1, RAND(), NOW() - INTERVAL 21 DAY, NOW()),
(4, '西藏之旅', '羊湖太美了', '西藏', '["https://picsum.photos/400/315"]', '["旅行"]', 800, 80, 400, 8000, 160, 1, RAND(), NOW() - INTERVAL 20 DAY, NOW()),
(4, '厦门攻略', '三天两夜厦门游', '厦门', '["https://picsum.photos/400/316"]', '["旅行"]', 600, 60, 300, 6000, 120, 1, RAND(), NOW() - INTERVAL 19 DAY, NOW()),
(4, '海岛穿搭', '马尔代夫穿搭', '马尔代夫', '["https://picsum.photos/400/317"]', '["旅行"]', 500, 50, 250, 5000, 100, 1, RAND(), NOW() - INTERVAL 18 DAY, NOW()),
(4, '大理旅拍', '大理景点推荐', '大理', '["https://picsum.photos/400/318"]', '["旅行"]', 400, 40, 200, 4000, 80, 1, RAND(), NOW() - INTERVAL 17 DAY, NOW()),
(4, '新疆之旅', '北疆环线游', '新疆', '["https://picsum.photos/400/319"]', '["旅行"]', 350, 35, 175, 3500, 70, 1, RAND(), NOW() - INTERVAL 16 DAY, NOW()),
(5, '直播预告', '今晚8点直播见', '直播间', '["https://picsum.photos/400/325"]', '["直播"]', 5000, 500, 2500, 50000, 1000, 1, RAND(), NOW() - INTERVAL 15 DAY, NOW()),
(5, '618必买', '618购物清单', '家中', '["https://picsum.photos/400/326"]', '["好物"]', 4000, 400, 2000, 40000, 800, 1, RAND(), NOW() - INTERVAL 14 DAY, NOW()),
(5, '护肤顺序', '正确护肤步骤', '家中', '["https://picsum.photos/400/327"]', '["护肤"]', 3500, 350, 1750, 35000, 700, 1, RAND(), NOW() - INTERVAL 13 DAY, NOW()),
(5, '减肥分享', '我瘦了20斤', '家中', '["https://picsum.photos/400/328"]', '["减肥"]', 3000, 300, 1500, 30000, 600, 1, RAND(), NOW() - INTERVAL 12 DAY, NOW()),
(5, '短视频技巧', '拍摄教程', '家中', '["https://picsum.photos/400/329"]', '["教程"]', 2500, 250, 1250, 25000, 500, 1, RAND(), NOW() - INTERVAL 11 DAY, NOW()),
(6, '日常分享1', '今天的心情', '家中', '["https://picsum.photos/400/340"]', '["生活"]', 2000, 200, 1000, 20000, 400, 1, RAND(), NOW() - INTERVAL 10 DAY, NOW()),
(6, '日常分享2', '周末好去处', '杭州', '["https://picsum.photos/400/341"]', '["生活"]', 1800, 180, 900, 18000, 360, 1, RAND(), NOW() - INTERVAL 9 DAY, NOW()),
(6, '好物推荐', '近期爱用物', '家中', '["https://picsum.photos/400/342"]', '["好物"]', 1500, 150, 750, 15000, 300, 1, RAND(), NOW() - INTERVAL 8 DAY, NOW()),
(6, '读书笔记', '最近看的好书', '家中', '["https://picsum.photos/400/343"]', '["阅读"]', 1200, 120, 600, 12000, 240, 1, RAND(), NOW() - INTERVAL 7 DAY, NOW()),
(6, '运动日常', '健身打卡', '健身房', '["https://picsum.photos/400/344"]', '["运动"]', 1000, 100, 500, 10000, 200, 1, RAND(), NOW() - INTERVAL 6 DAY, NOW());

-- 批量生成更多笔记（使用数字表快速生成）
INSERT INTO note (user_id, title, content, location, images, tags, like_count, comment_count, favorite_count, view_count, forward_count, status, stable_random, created_at, updated_at)
SELECT 
    FLOOR(2 + RAND() * 5) AS user_id,
    CONCAT('笔记 #', n.num) AS title,
    CONCAT('这是笔记', n.num, '的内容，包含丰富的文字描述和图片分享。') AS content,
    ELT(FLOOR(1 + RAND() * 5), '北京', '上海', '杭州', '成都', '广州') AS location,
    CONCAT('["https://picsum.photos/400/', 350 + (n.num % 100), '"]') AS images,
    CONCAT('["标签', (n.num % 10), '"]') AS tags,
    FLOOR(RAND() * 500) AS like_count,
    FLOOR(RAND() * 50) AS comment_count,
    FLOOR(RAND() * 200) AS favorite_count,
    FLOOR(RAND() * 5000) AS view_count,
    FLOOR(RAND() * 50) AS forward_count,
    1 AS status,
    RAND() AS stable_random,
    NOW() - INTERVAL FLOOR(RAND() * 180) DAY,
    NOW()
FROM (
    SELECT (a1.num + a2.num*10 + a3.num*100 + a4.num*1000 + a5.num*10000 + 26) AS num
    FROM 
        (SELECT 0 num UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a1,
        (SELECT 0 num UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a2,
        (SELECT 0 num UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a3,
        (SELECT 0 num UNION SELECT 1 UNION SELECT 2) a4,
        (SELECT 0 num UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a5
    LIMIT 20000
) n;

SELECT CONCAT('已生成 ', COUNT(*), ' 篇笔记') AS note_count FROM note;

-- ============================================================
-- 第三部分：生成关注关系
-- ============================================================

INSERT INTO follow (follower_id, following_id, created_at)
SELECT DISTINCT user_id, 5, NOW() - INTERVAL FLOOR(RAND() * 365) DAY
FROM user_activity WHERE user_id >= 7 AND user_id <= 30007 LIMIT 30000;

INSERT INTO follow (follower_id, following_id, created_at)
SELECT DISTINCT user_id, 6, NOW() - INTERVAL FLOOR(RAND() * 365) DAY
FROM user_activity WHERE user_id >= 30007 AND user_id <= 60007 LIMIT 30000;

INSERT INTO follow (follower_id, following_id, created_at)
SELECT DISTINCT user_id, 3, NOW() - INTERVAL FLOOR(RAND() * 365) DAY
FROM user_activity WHERE user_id >= 60007 AND user_id <= 65007 LIMIT 5000;

INSERT INTO follow (follower_id, following_id, created_at)
SELECT DISTINCT user_id, 4, NOW() - INTERVAL FLOOR(RAND() * 365) DAY
FROM user_activity WHERE user_id >= 65007 AND user_id <= 70007 LIMIT 5000;

INSERT INTO follow (follower_id, following_id, created_at)
SELECT user_id, 2, NOW() - INTERVAL FLOOR(RAND() * 365) DAY
FROM user_activity WHERE user_id >= 70007 AND user_id <= 70207 LIMIT 200;

SELECT CONCAT('已生成 ', COUNT(*), ' 条关注关系') AS follow_count FROM follow;

-- ============================================================
-- 第四部分：生成点赞
-- ============================================================

INSERT INTO note_like (note_id, user_id, created_at)
SELECT n.id, FLOOR(7 + RAND() * 99993), NOW() - INTERVAL FLOOR(RAND() * 180) DAY
FROM note n WHERE n.status = 1
LIMIT 100000;

SELECT CONCAT('已生成 ', COUNT(*), ' 条点赞') AS like_count FROM note_like;

-- ============================================================
-- 第五部分：生成收藏
-- ============================================================

INSERT INTO note_favorite (note_id, user_id, created_at)
SELECT n.id, FLOOR(7 + RAND() * 99993), NOW() - INTERVAL FLOOR(RAND() * 180) DAY
FROM note n WHERE n.status = 1
LIMIT 50000;

SELECT CONCAT('已生成 ', COUNT(*), ' 条收藏') AS favorite_count FROM note_favorite;

-- ============================================================
-- 第六部分：生成评论（含层级）
-- ============================================================

-- 根评论（使用数字表批量生成10万条）
INSERT INTO note_comment (note_id, user_id, parent_id, root_id, content, like_count, reply_count, status, hot_score, created_at)
SELECT 
    (n.num % (SELECT COUNT(*) FROM note)) + 1 AS note_id,
    FLOOR(7 + RAND() * 99993) AS user_id,
    0 AS parent_id,
    0 AS root_id,
    ELT(FLOOR(1 + RAND() * 15), 
        '写得真好！', '学到了', '太棒了', '支持下', '不错哦', '喜欢', '太优秀了', '真棒', '支持', '点赞', '收藏了', '学习了', '真好看', '太喜欢了', '超级棒') AS content,
    FLOOR(RAND() * 100) AS like_count,
    FLOOR(RAND() * 10) AS reply_count,
    1 AS status,
    FLOOR(RAND() * 50) AS hot_score,
    NOW() - INTERVAL FLOOR(RAND() * 180) DAY AS created_at
FROM (
    SELECT a1.num + a2.num*10 + a3.num*100 + a4.num*1000 + a5.num*10000 AS num
    FROM 
        (SELECT 0 num UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a1,
        (SELECT 0 num UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a2,
        (SELECT 0 num UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a3,
        (SELECT 0 num UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a4,
        (SELECT 0 num UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a5
    LIMIT 100000
) n;

SELECT CONCAT('已生成根评论 ', COUNT(*), ' 条') AS root_comment_count FROM note_comment WHERE parent_id = 0;

-- 子评论（回复根评论，8万条）
INSERT INTO note_comment (note_id, user_id, parent_id, root_id, content, like_count, reply_count, status, hot_score, created_at)
SELECT 
    nc.note_id,
    FLOOR(7 + RAND() * 99993),
    nc.id,
    nc.id,
    ELT(FLOOR(1 + RAND() * 10), 
        '同意楼上的', '说得对', '没错', '哈哈', '笑死', '真实', '太真实了', '哈哈', '回复一下', '支持下'),
    FLOOR(RAND() * 30),
    0,
    1,
    FLOOR(RAND() * 20),
    NOW() - INTERVAL FLOOR(RAND() * 180) DAY
FROM note_comment nc 
WHERE nc.parent_id = 0
LIMIT 80000;

SELECT CONCAT('已生成子评论 ', COUNT(*), ' 条') AS child_comment_count FROM note_comment WHERE parent_id > 0 AND root_id = parent_id;

-- 孙评论（回复子评论，2万条）
INSERT INTO note_comment (note_id, user_id, parent_id, root_id, content, like_count, reply_count, status, hot_score, created_at)
SELECT 
    nc2.note_id,
    FLOOR(7 + RAND() * 99993),
    nc2.id,
    nc2.root_id,
    ELT(FLOOR(1 + RAND() * 8), 
        '哈哈', '笑死', '真实', '回复一下', '支持', '点赞', '同意', '没错'),
    FLOOR(RAND() * 10),
    0,
    1,
    FLOOR(RAND() * 10),
    NOW() - INTERVAL FLOOR(RAND() * 180) DAY
FROM note_comment nc2
WHERE nc2.parent_id > 0 AND nc2.root_id > 0
LIMIT 20000;

SELECT CONCAT('已生成孙评论 ', COUNT(*), ' 条') AS grandchild_comment_count FROM note_comment WHERE parent_id > 0 AND root_id <> parent_id;

SELECT CONCAT('已生成 ', COUNT(*), ' 条评论') AS total_comment_count FROM note_comment;

-- ============================================================
-- 第七部分：更新热度值和统计数据
-- ============================================================

UPDATE note_comment 
SET hot_score = COALESCE(like_count, 0) * 2 + COALESCE(reply_count, 0) * 3
WHERE status = 1;

UPDATE note n SET 
    like_count = (SELECT COUNT(*) FROM note_like nl WHERE nl.note_id = n.id),
    comment_count = (SELECT COUNT(*) FROM note_comment nc WHERE nc.note_id = n.id),
    favorite_count = (SELECT COUNT(*) FROM note_favorite nf WHERE nf.note_id = n.id);

-- ============================================================
-- 第八部分：数据统计
-- ============================================================

SELECT '========== 超大规模数据统计 ==========' AS '';
SELECT '用户总数' AS 类型, COUNT(*) AS 数量 FROM user;
SELECT '笔记总数' AS 类型, COUNT(*) AS 数量 FROM note;
SELECT '评论总数' AS 类型, COUNT(*) AS 数量 FROM note_comment;
SELECT '关注关系' AS 类型, COUNT(*) AS 数量 FROM follow;
SELECT '点赞总数' AS 类型, COUNT(*) AS 数量 FROM note_like;
SELECT '收藏总数' AS 类型, COUNT(*) AS 数量 FROM note_favorite;

SET FOREIGN_KEY_CHECKS = 1;