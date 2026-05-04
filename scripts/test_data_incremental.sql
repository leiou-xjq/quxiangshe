-- 增量测试数据脚本 - 基于现有用户
SET FOREIGN_KEY_CHECKS = 0;

-- 清空互动数据
TRUNCATE TABLE notification;
TRUNCATE TABLE note_like;
TRUNCATE TABLE note_favorite;
TRUNCATE TABLE note_comment;
TRUNCATE TABLE follow;
TRUNCATE TABLE note;

-- 插入笔记 (50条)
INSERT INTO note (id, user_id, title, content, images, tags, location, stable_random, like_count, comment_count, favorite_count, view_count, forward_count, hot_score, status, created_at) VALUES
(1, 2, '地道的四川火锅配方', '今天分享我家传了三十年的四川火锅配方！底料用郫县豆瓣酱，配上花椒、干辣椒等香料，小火慢炒两个小时。', '[]', '["美食", "火锅"]', '成都', 0.172270, 1250, 89, 456, 8900, 234, 2789, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 3, '云南大理三日游攻略', '大理三天两夜精华攻略：Day1苍山洱海，Day2古城漫游，Day3双廊日落。', '[]', '["旅行", "云南"]', '大理', 0.278900, 2340, 156, 890, 15600, 456, 6789, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(3, 4, '2024最值得买的笔记本电脑', 'MacBook Pro M3、ThinkPad X1 Carbon、ROG幻16，到底选哪款？', '[]', '["科技", "笔记本"]', '深圳', 0.356780, 3456, 234, 1234, 25000, 567, 8901, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(4, 5, '春季穿搭灵感', '春天穿什么？分享几套通勤和约会穿搭，轻松get高级感。', '[]', '["时尚", "穿搭"]', '上海', 0.345670, 2345, 167, 890, 18000, 456, 6789, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(5, 6, '新手跑步入门指南', '想开始跑步但不知道怎么做？从装备到训练计划，跑步新手看这一篇就够了。', '[]', '["运动", "跑步"]', '北京', 0.312340, 1678, 123, 589, 12000, 312, 4567, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(6, 7, '养猫新手指南', '准备养猫需要准备什么？猫粮、猫砂盆、猫窝...分享我的养猫清单。', '[]', '["宠物", "猫"]', '北京', 0.367890, 3456, 245, 1234, 28000, 678, 9876, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(7, 8, '油画入门基础教程', '想学油画但不知从何开始？材料准备、构图基础，调色技巧。', '[]', '["艺术", "油画"]', '北京', 0.287650, 1234, 98, 456, 9800, 234, 3456, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(8, 9, '古典音乐入门推荐', '想听古典音乐但不知道从哪里开始？推荐几部入门级作品。', '[]', '["音乐", "古典"]', '维也纳', 0.276540, 987, 76, 356, 7800, 189, 2345, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(9, 10, '2024年阅读书单', '今年已经读了47本书，精选十本推荐给大家。', '[]', '["阅读", "书单"]', '北京', 0.312340, 1567, 123, 578, 12000, 312, 4567, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(10, 11, '2024年必看电影清单', '今年看了几十部电影，选出十部年度最佳。', '[]', '["电影", "推荐"]', '北京', 0.345670, 2345, 178, 876, 19000, 456, 6789, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(11, 12, 'ChatGPT使用技巧大全', 'ChatGPT不只是问答机器！分享10个高效使用技巧。', '[]', '["科技", "AI"]', '硅谷', 0.412340, 5678, 456, 2345, 45000, 890, 12345, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(12, 13, '程序员电脑配置推荐', '写代码到底需要什么配置的电脑？给你一份省钱又好用的配置清单。', '[]', '["科技", "程序员"]', '杭州', 0.298760, 1890, 145, 678, 14000, 345, 4567, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(13, 14, '小众设计师品牌推荐', '不想撞款？这几个小众设计师品牌必须知道。', '[]', '["时尚", "品牌"]', '巴黎', 0.287650, 1234, 98, 456, 9800, 234, 3456, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(14, 15, '职场穿搭指南', '职场穿着也能又美又专业！不同场合的穿搭公式。', '[]', '["时尚", "职场"]', '深圳', 0.298760, 1456, 112, 534, 11000, 289, 4567, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(15, 16, 'NBA西部季后赛分析', '今年西部竞争太激烈了！掘金、太阳、湖人谁能冲出西部？', '[]', '["运动", "NBA"]', '洛杉矶', 0.287650, 1234, 98, 456, 9800, 234, 3456, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(16, 17, '布偶猫喂养经验', '布偶猫真的太可爱了！但肠胃也比较脆弱。', '[]', '["宠物", "猫"]', '上海', 0.323450, 2345, 178, 876, 19000, 456, 6789, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(17, 18, '吉他初学者必学曲目', '学吉他从哪里开始？推荐几首适合新手的入门曲目。', '[]', '["音乐", "吉他"]', '北京', 0.312340, 1456, 112, 534, 11000, 289, 4567, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(18, 19, '摄影构图十大法则', '提升摄影水平的第一步：掌握构图！', '[]', '["艺术", "摄影"]', '上海', 0.345670, 2345, 167, 876, 19000, 456, 6789, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(19, 20, '职场成长经验分享', '工作五年，从月薪3000到年薪50万，我总结了这些职场经验。', '[]', '["职场", "成长"]', '北京', 0.345670, 2345, 178, 876, 19000, 456, 6789, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(20, 21, '减脂餐食谱一周不重样', '减脂期间吃什么？分享一周七天的减脂餐食谱，好吃又不饿。', '[]', '["运动", "减脂"]', '深圳', 0.345670, 2789, 198, 1023, 21000, 534, 6789, 1, DATE_SUB(NOW(), INTERVAL 1 DAY));

COMMIT;

-- 插入关注关系
INSERT IGNORE INTO follow (follower_id, following_id, created_at)
SELECT DISTINCT 
  FLOOR(20 + RAND() * 180),
  FLOOR(1 + RAND() * 19) + 1,
  DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY)
FROM (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) AS a,
     (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8) AS b;

COMMIT;

-- 插入评论
INSERT INTO note_comment (note_id, user_id, parent_id, root_id, content, like_count, reply_count, hot_score, status, created_at)
SELECT 
  1 + FLOOR(RAND() * 20),
  2 + FLOOR(RAND() * 19),
  0, 0,
  '写得真好！受益匪浅，学到了很多。',
  FLOOR(5 + RAND() * 50),
  0, 0, 1,
  DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 7) DAY)
FROM (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) AS a,
     (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) AS b;

UPDATE note_comment SET hot_score = like_count * 2 WHERE parent_id = 0;
COMMIT;

-- 插入点赞收藏
INSERT IGNORE INTO note_like (note_id, user_id, created_at)
SELECT DISTINCT 1 + FLOOR(RAND() * 20), 2 + FLOOR(RAND() * 19), DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY)
FROM (SELECT 1 UNION SELECT 2 UNION SELECT 3) AS a,
     (SELECT 1 UNION SELECT 2 UNION SELECT 3) AS b;

INSERT IGNORE INTO note_favorite (note_id, user_id, created_at)
SELECT DISTINCT 1 + FLOOR(RAND() * 20), 2 + FLOOR(RAND() * 19), DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY)
FROM (SELECT 1 UNION SELECT 2) AS a;

COMMIT;

-- 插入通知
INSERT INTO notification (user_id, type, from_user_id, note_id, content, is_read, created_at)
SELECT 2 + FLOOR(RAND() * 19), 1, 2 + FLOOR(RAND() * 19), 1 + FLOOR(RAND() * 20), '赞了你的笔记', 0, DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 7) DAY)
FROM (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) AS a;

INSERT INTO notification (user_id, type, from_user_id, note_id, content, is_read, created_at)
SELECT 2 + FLOOR(RAND() * 19), 2, 2 + FLOOR(RAND() * 19), 1 + FLOOR(RAND() * 20), '评论了你的笔记', 0, DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 7) DAY)
FROM (SELECT 1 UNION SELECT 2 UNION SELECT 3) AS a;

INSERT INTO notification (user_id, type, from_user_id, content, is_read, created_at)
SELECT 2 + FLOOR(RAND() * 19), 3, 2 + FLOOR(RAND() * 19), '关注了你', 0, DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 7) DAY)
FROM (SELECT 1 UNION SELECT 2) AS a;

COMMIT;

SET FOREIGN_KEY_CHECKS = 1;

-- 验证
SELECT 'Users' AS metric, COUNT(*) FROM user
UNION ALL SELECT 'Notes', COUNT(*) FROM note
UNION ALL SELECT 'Follows', COUNT(*) FROM follow
UNION ALL SELECT 'Comments', COUNT(*) FROM note_comment
UNION ALL SELECT 'NoteLikes', COUNT(*) FROM note_like
UNION ALL SELECT 'Favorites', COUNT(*) FROM note_favorite
UNION ALL SELECT 'Notifications', COUNT(*) FROM notification;