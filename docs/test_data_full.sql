-- ============================================================
-- 趣享社 完整测试数据生成脚本（支持20万粉丝）
-- ============================================================
-- 使用方法：先执行清理脚本，再执行生成脚本
-- ============================================================

-- ============================================================
-- 第1部分：清理脚本（执行1次）
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE feed_push_log;
TRUNCATE TABLE note_comment;
TRUNCATE TABLE note_favorite;
TRUNCATE TABLE note_like;
TRUNCATE TABLE forward;
TRUNCATE TABLE note;
TRUNCATE TABLE follow;
TRUNCATE TABLE user_activity;
DELETE FROM user WHERE username != 'admin';

SET FOREIGN_KEY_CHECKS = 1;

ALTER TABLE user AUTO_INCREMENT = 1;

-- ============================================================
-- 第2部分：用户数据（6个博主+20万粉丝）
-- ============================================================

-- 插入6个博主
INSERT INTO user (id, username, nickname, avatar, password, gender, bio, status, created_at, updated_at) VALUES
(1, 'admin', '管理员', 'https://picsum.photos/200', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EH', 0, '管理员', 1, NOW(), NOW()),
(2, 'xiaoboker1', '穿搭达人小美', 'https://picsum.photos/201', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EH', 2, '分享每日穿搭', 1, NOW(), NOW()),
(3, 'zhongboker1', '美食家大V', 'https://picsum.photos/202', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EH', 1, '美食博主', 1, NOW(), NOW()),
(4, 'zhongboker2', '旅行摄影师', 'https://picsum.photos/203', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EH', 1, '旅行摄影', 1, NOW(), NOW()),
(5, 'daboker1', '超级网红', 'https://picsum.photos/204', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EH', 2, '千万粉丝博主', 1, NOW(), NOW()),
(6, 'daboker2', '知名博主', 'https://picsum.photos/205', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EH', 1, '知名博主', 1, NOW(), NOW());

-- 创建存储过程：批量生成用户
DELIMITER //

CREATE PROCEDURE generate_fans(IN start_num INT, IN end_num INT)
BEGIN
    DECLARE i INT DEFAULT start_num;
    WHILE i <= end_num DO
        INSERT INTO user (username, nickname, avatar, password, gender, bio, status, created_at, updated_at)
        VALUES (
            CONCAT('user', i),
            CONCAT('粉丝用户', i),
            CONCAT('https://picsum.photos/', 200 + (i % 800)),
            '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EH',
            FLOOR(RAND() * 3),
            '测试用户',
            1,
            NOW(),
            NOW()
        );
        SET i = i + 1;
    END WHILE;
END //

CREATE PROCEDURE generate_fans_batch(IN batch_count INT, IN batch_size INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE j INT DEFAULT 0;
    DECLARE start_id INT;
    DECLARE batch_id INT;
    
    SET batch_id = 0;
    WHILE batch_id < batch_count DO
        SET start_id = 7 + (batch_id * batch_size);
        SET i = 0;
        
        START TRANSACTION;
        WHILE i < batch_size DO
            INSERT INTO user (username, nickname, avatar, password, gender, bio, status, created_at, updated_at)
            VALUES (
                CONCAT('user', start_id + i),
                CONCAT('粉丝用户', start_id + i),
                CONCAT('https://picsum.photos/', 200 + ((start_id + i) % 800)),
                '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EH',
                FLOOR(RAND() * 3),
                '测试用户',
                1,
                NOW(),
                NOW()
            );
            SET i = i + 1;
        END WHILE;
        COMMIT;
        
        SET batch_id = batch_id + 1;
        
        -- 每10批显示进度
        IF batch_id % 10 = 0 THEN
            SELECT CONCAT('已生成 ', batch_id * batch_size, ' 个用户') as progress;
        END IF;
    END WHILE;
END //

DELIMITER ;

-- 执行批量生成（200批，每批1000个 = 20万用户）
-- 生产环境建议分批执行，每批5-10万
CALL generate_fans_batch(200, 1000);

-- 生成用户活跃度数据
DELIMITER //

CREATE PROCEDURE generate_user_activity(IN start_num INT, IN end_num INT)
BEGIN
    DECLARE i INT DEFAULT start_num;
    DECLARE score DOUBLE;
    
    WHILE i <= end_num DO
        SET score = 20 + RAND() * 180;
        
        INSERT INTO user_activity (user_id, login_days, interaction_count, last_login_date, today_interaction_count, today_interaction_date, activity_score)
        VALUES (
            i,
            FLOOR(1 + RAND() * 30),
            FLOOR(RAND() * 100),
            CURDATE() - INTERVAL FLOOR(RAND() * 30) DAY,
            FLOOR(RAND() * 10),
            CURDATE() - INTERVAL FLOOR(RAND() * 7) DAY,
            score
        );
        SET i = i + 1;
    END WHILE;
END //

DELIMITER ;

-- 批量生成活跃度（每批1万）
CREATE PROCEDURE generate_activity_batch(IN batch_count INT, IN batch_size INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE j INT DEFAULT 0;
    DECLARE start_id INT;
    DECLARE batch_id INT;
    DECLARE score DOUBLE;
    
    SET batch_id = 0;
    WHILE batch_id < batch_count DO
        SET start_id = 2 + (batch_id * batch_size);
        SET i = 0;
        
        WHILE i < batch_size DO
            SET score = 20 + RAND() * 180;
            
            INSERT INTO user_activity (user_id, login_days, interaction_count, last_login_date, today_interaction_count, today_interaction_date, activity_score)
            VALUES (
                start_id + i,
                FLOOR(1 + RAND() * 30),
                FLOOR(RAND() * 100),
                CURDATE() - INTERVAL FLOOR(RAND() * 30) DAY,
                FLOOR(RAND() * 10),
                CURDATE() - INTERVAL FLOOR(RAND() * 7) DAY,
                score
            );
            SET i = i + 1;
        END WHILE;
        
        SET batch_id = batch_id + 1;
    END WHILE;
END //

DELIMITER ;

-- 执行活跃度生成（200批，每批1000个 = 20万活跃度）
CALL generate_activity_batch(200, 1000);

-- ============================================================
-- 第3部分：关注关系数据
-- ============================================================

-- 小博主(ID=2)：1个粉丝 -> 推模式
INSERT INTO follow (follower_id, following_id) VALUES (3, 2);

-- 中博主1(ID=3)：999个粉丝 -> 拉模式
INSERT INTO follow (follower_id, following_id)
SELECT user_id, 3 FROM user_activity WHERE activity_score > 20 LIMIT 999;

-- 中博主2(ID=4)：999个粉丝 -> 拉模式  
INSERT INTO follow (follower_id, following_id)
SELECT user_id + 1000, 4 FROM user_activity WHERE activity_score > 20 LIMIT 999;

-- 大博主1(ID=5)：10万粉丝 -> 推拉结合模式
INSERT INTO follow (follower_id, following_id)
SELECT user_id, 5 FROM user LIMIT WHERE id >= 7 AND id <= 100007;

-- 大博主2(ID=6)：10万粉丝 -> 推拉结合模式
INSERT INTO follow (follower_id, following_id)
SELECT user_id + 100000, 6 FROM user WHERE id >= 100007 AND id <= 200006;

-- ============================================================
-- 第4部分：笔记数据
-- ============================================================

-- 小博主：5篇笔记
INSERT INTO note (user_id, title, content, location, images, tags, status, like_count, comment_count, favorite_count, view_count, forward_count, created_at, updated_at) VALUES
(2, '今日穿搭｜简约黑色连衣裙', '黑色连衣裙+小白鞋，简约时尚', '上海', '["https://picsum.photos/400/300"]', '["穿搭"]', 1, 0, 0, 100, 0, NOW(), NOW()),
(2, '通勤穿搭｜白衬衫+牛仔裤', '经典搭配，永不过时', '北京', '["https://picsum.photos/400/301"]', '["穿搭"]', 1, 0, 0, 80, 0, NOW(), NOW()),
(2, '春季外套推荐', '早春外套合集', '杭州', '["https://picsum.photos/400/302"]', '["穿搭"]', 1, 0, 0, 60, 0, NOW(), NOW()),
(2, '配色技巧', '穿衣配色技巧分享', '家中', '["https://picsum.photos/400/303"]', '["穿搭"]', 1, 0, 0, 50, 0, NOW(), NOW()),
(2, '基础款穿搭', '基础款也能穿出时尚感', '家中', '["https://picsum.photos/400/304"]', '["穿搭"]', 1, 0, 0, 40, 0, NOW(), NOW());

-- 中博主1：10篇笔记
INSERT INTO note (user_id, title, content, location, images, tags, status, like_count, comment_count, favorite_count, view_count, forward_count, created_at, updated_at) VALUES
(3, '探店｜宝藏火锅店', '人均30元火锅店推荐', '成都', '["https://picsum.photos/400/305"]', '["美食"]', 1, 0, 0, 500, 0, NOW(), NOW()),
(3, '家常菜教程', '蒜香排骨做法', '家中', '["https://picsum.photos/400/306"]', '["美食"]', 1, 0, 0, 400, 0, NOW(), NOW()),
(3, '早餐推荐', '快手早餐合集', '家中', '["https://picsum.photos/400/307"]', '["美食"]', 1, 0, 0, 300, 0, NOW(), NOW()),
(3, '甜品做法', '杨枝甘露在家做', '家中', '["https://picsum.photos/400/308"]', '["美食"]', 1, 0, 0, 250, 0, NOW(), NOW()),
(3, '减肥餐', '健康减肥餐推荐', '家中', '["https://picsum.photos/400/309"]', '["美食"]', 1, 0, 0, 200, 0, NOW(), NOW()),
(3, '外卖推荐', '干净外卖店铺', '北京', '["https://picsum.photos/400/310"]', '["美食"]', 1, 0, 0, 180, 0, NOW(), NOW()),
(3, '一人食', '一人食食谱', '家中', '["https://picsum.photos/400/311"]', '["美食"]', 1, 0, 0, 150, 0, NOW(), NOW()),
(3, '便当教程', '工作日便当', '家中', '["https://picsum.photos/400/312"]', '["美食"]', 1, 0, 0, 120, 0, NOW(), NOW()),
(3, '夜宵推荐', '健康夜宵选择', '家中', '["https://picsum.photos/400/313"]', '["美食"]', 1, 0, 0, 100, 0, NOW(), NOW()),
(3, '零食测评', '网红零食真实测评', '家中', '["https://picsum.photos/400/314"]', '["美食"]', 1, 0, 0, 80, 0, NOW(), NOW());

-- 中博主2：10篇笔记
INSERT INTO note (user_id, title, content, location, images, tags, status, like_count, comment_count, favorite_count, view_count, forward_count, created_at, updated_at) VALUES
(4, '西藏之旅', '羊湖太美了', '西藏', '["https://picsum.photos/400/315"]', '["旅行"]', 1, 0, 0, 800, 0, NOW(), NOW()),
(4, '厦门攻略', '三天两夜厦门游', '厦门', '["https://picsum.photos/400/316"]', '["旅行"]', 1, 0, 0, 600, 0, NOW(), NOW()),
(4, '海岛穿搭', '马尔代夫穿搭', '马尔代夫', '["https://picsum.photos/400/317"]', '["旅行"]', 1, 0, 0, 500, 0, NOW(), NOW()),
(4, '大理旅拍', '���理���点推荐', '大理', '["https://picsum.photos/400/318"]', '["旅行"]', 1, 0, 0, 400, 0, NOW(), NOW()),
(4, '新疆之旅', '北疆环线游', '新疆', '["https://picsum.photos/400/319"]', '["旅行"]', 1, 0, 0, 350, 0, NOW(), NOW()),
(4, '日本攻略', '东京大阪攻略', '日本', '["https://picsum.photos/400/320"]', '["旅行"]', 1, 0, 0, 300, 0, NOW(), NOW()),
(4, '泰国之旅', '曼谷芭提雅', '泰国', '["https://picsum.photos/400/321"]', '["旅行"]', 1, 0, 0, 250, 0, NOW(), NOW()),
(4, '云南旅拍', '丽江大理', '云南', '["https://picsum.photos/400/322"]', '["旅行"]', 1, 0, 0, 200, 0, NOW(), NOW()),
(4, '川西环线', '川西小环线', '四川', '["https://picsum.photos/400/323"]', '["旅行"]', 1, 0, 0, 150, 0, NOW(), NOW()),
(4, '青海湖', '青海湖之旅', '青海', '["https://picsum.photos/400/324"]', '["旅行"]', 1, 0, 0, 120, 0, NOW(), NOW());

-- 大博主1：20篇笔记
INSERT INTO note (user_id, title, content, location, images, tags, status, like_count, comment_count, favorite_count, view_count, forward_count, created_at, updated_at) VALUES
(5, '直播预告', '今晚8点直播见', '直播间', '["https://picsum.photos/400/325"]', '["直播"]', 1, 0, 0, 5000, 0, NOW(), NOW()),
(5, '618必买', '618购物清单', '家中', '["https://picsum.photos/400/326"]', '["好物"]', 1, 0, 0, 4000, 0, NOW(), NOW()),
(5, '护肤顺序', '正确护肤步骤', '家中', '["https://picsum.photos/400/327"]', '["护肤"]', 1, 0, 0, 3500, 0, NOW(), NOW()),
(5, '减肥分享', '我瘦了20斤', '家中', '["https://picsum.photos/400/328"]', '["减肥"]', 1, 0, 0, 3000, 0, NOW(), NOW()),
(5, '短视频技巧', '拍摄教程', '家中', '["https://picsum.photos/400/329"]', '["教程"]', 1, 0, 0, 2500, 0, NOW(), NOW()),
(5, '好物分享1', '自用好物', '家中', '["https://picsum.photos/400/330"]', '["好物"]', 1, 0, 0, 2000, 0, NOW(), NOW()),
(5, '好物分享2', '平价好物', '家中', '["https://picsum.photos/400/331"]', '["好物"]', 1, 0, 0, 1800, 0, NOW(), NOW()),
(5, '护肤教程', '新手护肤', '家中', '["https://picsum.photos/400/332"]', '["护肤"]', 1, 0, 0, 1500, 0, NOW(), NOW()),
(5, '彩妆教程', '日常妆', '家中', '["https://picsum.photos/400/333"]', '["彩妆"]', 1, 0, 0, 1200, 0, NOW(), NOW()),
(5, '发型教程', '简单扎发', '家中', '["https://picsum.photos/400/334"]', '["发型"]', 1, 0, 0, 1000, 0, NOW(), NOW()),
(5, '穿搭分享', '一周穿搭', '家中', '["https://picsum.photos/400/335"]', '["穿搭"]', 1, 0, 0, 900, 0, NOW(), NOW()),
(5, '收纳技巧', '家居收纳', '家中', '["https://picsum.photos/400/336"]', '["家居"]', 1, 0, 0, 800, 0, NOW(), NOW()),
(5, '早餐教程', '快手早餐', '家中', '["https://picsum.photos/400/337"]', '["美食"]', 1, 0, 0, 700, 0, NOW(), NOW()),
(5, '健身分享', '居家健身', '家中', '["https://picsum.photos/400/338"]', '["健身"]', 1, 0, 0, 600, 0, NOW(), NOW()),
(5, '读书分享', '近期书单', '家中', '["https://picsum.photos/400/339"]', '["读书"]', 1, 0, 0, 500, 0, NOW(), NOW()),
(5, '自律分享', '时间管理', '家中', '["https://picsum.photos/400/340"]', '["成长"]', 1, 0, 0, 450, 0, NOW(), NOW()),
(5, 'vlog1', '日常vlog', '家中', '["https://picsum.photos/400/341"]', '["vlog"]', 1, 0, 0, 400, 0, NOW(), NOW()),
(5, 'vlog2', '工作日常', '公司', '["https://picsum.photos/400/342"]', '["vlog"]', 1, 0, 0, 350, 0, NOW(), NOW()),
(5, '问答1', '粉丝问答', '直播间', '["https://picsum.photos/400/343"]', '["问答"]', 1, 0, 0, 300, 0, NOW(), NOW()),
(5, '问答2', '第二期问答', '直播间', '["https://picsum.photos/400/344"]', '["问答"]', 1, 0, 0, 250, 0, NOW(), NOW());

-- 大博主2：20篇笔记
INSERT INTO note (user_id, title, content, location, images, tags, status, like_count, comment_count, favorite_count, view_count, forward_count, created_at, updated_at) VALUES
(6, '情感｜为什么不表白', '男生不表白原因', '家中', '["https://picsum.photos/400/345"]', '["情感"]', 1, 0, 0, 4000, 0, NOW(), NOW()),
(6, '婚姻保鲜', '结婚5年经验', '家中', '["https://picsum.photos/400/346"]', '["婚姻"]', 1, 0, 0, 3000, 0, NOW(), NOW()),
(6, '分手应该做的事', '疗愈方法', '家中', '["https://picsum.photos/400/347"]', '["情感"]', 1, 0, 0, 2500, 0, NOW(), NOW()),
(6, '识别渣男', '防渣指南', '家中', '["https://picsum.photos/400/348"]', '["情感"]', 1, 0, 0, 2000, 0, NOW(), NOW()),
(6, '相亲注意事项', '避坑指南', '家中', '["https://picsum.photos/400/349"]', '["相亲"]', 1, 0, 0, 1800, 0, NOW(), NOW()),
(6, '恋爱技巧', '相处之道', '家中', '["https://picsum.photos/400/350"]', '["恋爱"]', 1, 0, 0, 1500, 0, NOW(), NOW()),
(6, '聊天技巧', '这样聊加分', '家中', '["https://picsum.photos/400/351"]', '["情感"]', 1, 0, 0, 1200, 0, NOW(), NOW()),
(6, '礼物推荐', '送这个准没错', '家中', '["https://picsum.photos/400/352"]', '["礼物"]', 1, 0, 0, 1000, 0, NOW(), NOW()),
(6, '节日祝福', '情人节快乐', '家中', '["https://picsum.photos/400/353"]', '["情感"]', 1, 0, 0, 900, 0, NOW(), NOW()),
(6, '单身日常', '一个人的生活', '家中', '["https://picsum.photos/400/354"]', '["日常"]', 1, 0, 0, 800, 0, NOW(), NOW()),
(6, '职场情感', '办公室恋情', '公司', '["https://picsum.photos/400/355"]', '["职场"]', 1, 0, 0, 700, 0, NOW(), NOW()),
(6, '闺蜜日常', '和闺蜜出游', '上海', '["https://picsum.photos/400/356"]', '["日常"]', 1, 0, 0, 600, 0, NOW(), NOW()),
(6, '母胎solo', 'solo日常', '家中', '["https://picsum.photos/400/357"]', '["日常"]', 1, 0, 0, 550, 0, NOW(), NOW()),
(6, '相亲经历', '奇葩相亲', '家中', '["https://picsum.photos/400/358"]', '["相亲"]', 1, 0, 0, 500, 0, NOW(), NOW()),
(6, '择偶标准', '我的标准', '家中', '["https://picsum.photos/400/359"]', '["情感"]', 1, 0, 0, 450, 0, NOW(), NOW()),
(6, '脱单技巧', '实用方法', '家中', '["https://picsum.photos/400/360"]', '["脱单"]', 1, 0, 0, 400, 0, NOW(), NOW()),
(6, '回复模板', '高情商回复', '家中', '["https://picsum.photos/400/361"]', '["教程"]', 1, 0, 0, 350, 0, NOW(), NOW()),
(6, '朋友圈发什么', '朋友圈内容', '家中', '["https://picsum.photos/400/362"]', '["社交"]', 1, 0, 0, 300, 0, NOW(), NOW()),
(6, '约会地点', '推荐去处', '上海', '["https://picsum.photos/400/363"]', '["约会"]', 1, 0, 0, 250, 0, NOW(), NOW()),
(6, '表白被拒', '被拒怎么办', '家中', '["https://picsum.photos/400/364"]', '["情感"]', 1, 0, 0, 200, 0, NOW(), NOW());

-- ============================================================
-- 第5部分：互动数据（点赞、评论、收藏）
-- ============================================================

-- 为笔记生成点赞数据（每个笔记随机5-50个赞）
INSERT INTO note_like (note_id, user_id, created_at)
SELECT n.id, FLOOR(7 + RAND() * 1000), NOW()
FROM note n
CROSS JOIN (
    SELECT 1 as i UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
) t
WHERE n.id <= 10
LIMIT 200;

-- 为笔记生成收藏数据
INSERT INTO note_favorite (note_id, user_id, created_at)
SELECT n.id, FLOOR(7 + RAND() * 500), NOW()
FROM note n
WHERE n.id <= 15
LIMIT 100;

-- 为笔记生成评论
INSERT INTO note_comment (note_id, user_id, content, status, created_at) VALUES
(1, 7, '太好看了！求链接', 1, NOW()),
(1, 8, '太种草了', 1, NOW()),
(2, 7, '搭配技巧学到了', 1, NOW()),
(3, 7, '大衣求品牌', 1, NOW()),
(4, 7, '收藏了', 1, NOW()),
(4, 8, '下周去！', 1, NOW()),
(5, 7, '已关注！', 1, NOW()),
(11, 7, '买过，好用！', 1, NOW()),
(21, 7, '太真实了', 1, NOW()),
(21, 8, '最后一个就是我', 1, NOW());

-- ============================================================
-- 第6部分：更新热度数据
-- ============================================================

-- 更新点赞数
UPDATE note n SET like_count = (
    SELECT COUNT(*) FROM note_like nl WHERE nl.note_id = n.id
);

-- 更新评论数  
UPDATE note n SET comment_count = (
    SELECT COUNT(*) FROM note_comment nc WHERE nc.note_id = n.id
);

-- 更新收藏数
UPDATE note n SET favorite_count = (
    SELECT COUNT(*) FROM note_favorite nf WHERE nf.note_id = n.id
);

-- ============================================================
-- 数据统计查询
-- ============================================================

SELECT '========== 数据统计 ==========' as '';
SELECT '用户总数' as 类型, COUNT(*) as 数量 FROM user
UNION ALL SELECT '关注关系', COUNT(*) FROM follow
UNION ALL SELECT '笔记', COUNT(*) FROM note
UNION ALL SELECT '点赞', COUNT(*) FROM note_like
UNION ALL SELECT '收藏', COUNT(*) FROM note_favorite
UNION ALL SELECT '评论', COUNT(*) FROM note_comment
UNION ALL SELECT '用户活跃度', COUNT(*) FROM user_activity;

-- 查看博主粉丝数
SELECT 
    u.nickname as 博主,
    (SELECT COUNT(*) FROM follow f WHERE f.following_id = u.id) as 粉丝数,
    CASE 
        WHEN (SELECT COUNT(*) FROM follow f WHERE f.following_id = u.id) < 1000 THEN '推模式'
        WHEN (SELECT COUNT(*) FROM follow f WHERE f.following_id = u.id) < 100000 THEN '拉模式'
        ELSE '推拉结合'
    END as 推送模式
FROM user u WHERE u.id <= 6;