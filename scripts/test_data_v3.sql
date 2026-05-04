-- ============================================
-- 趣享社测试数据生成脚本 v3
-- ============================================

SET FOREIGN_KEY_CHECKS = 0;
SET UNIQUE_CHECKS = 0;
SET AUTOCOMMIT = 0;

-- 清空数据
TRUNCATE TABLE notification;
TRUNCATE TABLE comment_like;
TRUNCATE TABLE note_favorite;
TRUNCATE TABLE note_like;
TRUNCATE TABLE note_comment;
TRUNCATE TABLE follow;
TRUNCATE TABLE note;

-- 插入博主用户 (ID 2-101)
INSERT INTO user (id, username, phone, password, avatar, nickname, gender, bio, status, role, created_at) VALUES
(2, 'xiaomei', '13800000002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/201', '穿搭达人小美', 2, '分享每日穿搭灵感', 1, 'USER', NOW()),
(3, 'dachu', '13800000003', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/202', '美食家大厨', 1, '二十年厨艺经验', 1, 'USER', NOW()),
(4, 'lvxing', '13800000004', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/203', '旅行摄影师', 1, '用镜头记录世界', 1, 'USER', NOW()),
(5, 'keji', '13800000005', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/204', '科技极客', 1, '最新数码产品测评', 1, 'USER', NOW()),
(6, 'yundong', '13800000006', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/205', '运动达人', 1, '健身户外运动', 1, 'USER', NOW()),
(7, 'yishujia', '13800000007', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/206', '自由艺术家', 1, '油画水彩插画', 1, 'USER', NOW()),
(8, 'dushu', '13800000008', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/207', '读书人', 2, '每年阅读100本书', 1, 'USER', NOW()),
(9, 'chongwu', '13800000009', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/208', '萌宠博主', 2, '养猫养狗经验分享', 1, 'USER', NOW()),
(10, 'yinyue', '13800000010', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/209', '音乐爱好者', 1, '古典乐流行乐', 1, 'USER', NOW()),
(11, 'dianying', '13800000011', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/210', '电影推荐官', 1, '院线新片评测', 1, 'USER', NOW()),
(12, 'shuma', '13800000012', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/211', '数码测评师', 1, '手机电脑测评', 1, 'USER', NOW()),
(13, 'youxi', '13800000013', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/212', '游戏主播', 1, 'Steam游戏实况', 1, 'USER', NOW()),
(14, 'meizhuang', '13800000014', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/213', '美妆达人', 2, '化妆技巧测评', 1, 'USER', NOW()),
(15, 'jiaju', '13800000015', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/214', '家居博主', 2, '装修灵感好物', 1, 'USER', NOW()),
(16, 'gupiao', '13800000016', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/215', '股票分析师', 1, 'A股分析投资', 1, 'USER', NOW()),
(17, 'jiaoyu', '13800000017', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/216', '教育博主', 1, 'K12留学资讯', 1, 'USER', NOW()),
(18, 'lishi', '13800000018', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/217', '历史爱好者', 1, '历史故事人物', 1, 'USER', NOW()),
(19, 'qiche', '13800000019', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/218', '汽车达人', 1, '新车评测购车', 1, 'USER', NOW()),
(20, 'xingzuo', '13800000020', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/219', '星座博主', 2, '星座运势分析', 1, 'USER', NOW()),
(21, 'jianshen', '13800000021', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/220', '健身教练', 1, '科学健身增肌', 1, 'USER', NOW()),
(22, 'meishi01', '13800000022', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/221', '地方美食家', 1, '探索各地美食', 1, 'USER', NOW()),
(23, 'meishi02', '13800000023', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/222', '烘焙达人', 2, '面包蛋糕甜点', 1, 'USER', NOW()),
(24, 'lvyou01', '13800000024', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/223', '自驾游攻略', 1, '公路旅行分享', 1, 'USER', NOW()),
(25, 'lvyou02', '13800000025', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/224', '出境游达人', 1, '护照签证攻略', 1, 'USER', NOW()),
(26, 'keji01', '13800000026', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/225', 'AI研究员', 1, 'AI技术解读', 1, 'USER', NOW()),
(27, 'keji02', '13800000027', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/226', '程序员老王', 1, '编程开发经验', 1, 'USER', NOW()),
(28, 'shishang01', '13800000028', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/227', '潮流教主', 1, '街头潮流穿搭', 1, 'USER', NOW()),
(29, 'shishang02', '13800000029', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/228', '轻奢主义', 2, '小众品牌生活', 1, 'USER', NOW()),
(30, 'tiyu01', '13800000030', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/229', '篮球解说', 1, 'NBA赛事分析', 1, 'USER', NOW()),
(31, 'tiyu02', '13800000031', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/230', '足球先锋', 1, '五大联赛热点', 1, 'USER', NOW()),
(32, 'chongwu01', '13800000032', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/231', '养猫日记', 2, '猫咪日常养护', 1, 'USER', NOW()),
(33, 'chongwu02', '13800000033', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/232', '汪星人俱乐部', 1, '狗狗养护训练', 1, 'USER', NOW()),
(34, 'yinxiang01', '13800000034', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/233', '乐器学习', 1, '吉他钢琴教学', 1, 'USER', NOW()),
(35, 'yinxiang02', '13800000035', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/234', 'HIFI发烧友', 1, '音响设备欣赏', 1, 'USER', NOW()),
(36, 'yishu01', '13800000036', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/235', '摄影技巧', 1, '摄影教程修图', 1, 'USER', NOW()),
(37, 'yishu02', '13800000037', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/236', '手账达人', 2, '文具手账制作', 1, 'USER', NOW()),
(38, 'qita01', '13800000038', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/237', '职场成长', 1, '职业规划技能', 1, 'USER', NOW()),
(39, 'qita02', '13800000039', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/238', '理财知识', 1, '基金理财入门', 1, 'USER', NOW()),
(40, 'qita03', '13800000040', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/239', '亲子教育', 2, '育儿经验活动', 1, 'USER', NOW());

-- 插入更多博主 ID 41-101
INSERT INTO user (id, username, phone, password, avatar, nickname, gender, bio, status, role, created_at)
SELECT 40 + n, CONCAT('blogger_', 40 + n), CONCAT('138', LPAD(40000040 + n, 8, '0')), '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', CONCAT('https://picsum.photos/240?random=', n), CONCAT('Blogger', 40 + n), 1, 'Content creator', 1, 'USER', NOW()
FROM (SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20 UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25 UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29 UNION SELECT 30 UNION SELECT 31 UNION SELECT 32 UNION SELECT 33 UNION SELECT 34 UNION SELECT 35 UNION SELECT 36 UNION SELECT 37 UNION SELECT 38 UNION SELECT 39 UNION SELECT 40 UNION SELECT 41 UNION SELECT 42 UNION SELECT 43 UNION SELECT 44 UNION SELECT 45 UNION SELECT 46 UNION SELECT 47 UNION SELECT 48 UNION SELECT 49 UNION SELECT 50 UNION SELECT 51 UNION SELECT 52 UNION SELECT 53 UNION SELECT 54 UNION SELECT 55 UNION SELECT 56 UNION SELECT 57 UNION SELECT 58 UNION SELECT 59 UNION SELECT 60) AS numbers;

-- 插入小博主 ID 102-301
INSERT INTO user (id, username, phone, password, avatar, nickname, gender, bio, status, role, created_at)
SELECT 100 + n, CONCAT('small_blogger_', 100 + n), CONCAT('138', LPAD(10000100 + n, 8, '0')), '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', CONCAT('https://picsum.photos/300?random=', n), CONCAT('SmallBlogger', 100 + n), 1, 'Regular user', 1, 'USER', NOW()
FROM (SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20 UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25 UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29 UNION SELECT 30 UNION SELECT 31 UNION SELECT 32 UNION SELECT 33 UNION SELECT 34 UNION SELECT 35 UNION SELECT 36 UNION SELECT 37 UNION SELECT 38 UNION SELECT 39 UNION SELECT 40 UNION SELECT 41 UNION SELECT 42 UNION SELECT 43 UNION SELECT 44 UNION SELECT 45 UNION SELECT 46 UNION SELECT 47 UNION SELECT 48 UNION SELECT 49 UNION SELECT 50 UNION SELECT 51 UNION SELECT 52 UNION SELECT 53 UNION SELECT 54 UNION SELECT 55 UNION SELECT 56 UNION SELECT 57 UNION SELECT 58 UNION SELECT 59 UNION SELECT 60 UNION SELECT 61 UNION SELECT 62 UNION SELECT 63 UNION SELECT 64 UNION SELECT 65 UNION SELECT 66 UNION SELECT 67 UNION SELECT 68 UNION SELECT 69 UNION SELECT 70 UNION SELECT 71 UNION SELECT 72 UNION SELECT 73 UNION SELECT 74 UNION SELECT 75 UNION SELECT 76 UNION SELECT 77 UNION SELECT 78 UNION SELECT 79 UNION SELECT 80 UNION SELECT 81 UNION SELECT 82 UNION SELECT 83 UNION SELECT 84 UNION SELECT 85 UNION SELECT 86 UNION SELECT 87 UNION SELECT 88 UNION SELECT 89 UNION SELECT 90 UNION SELECT 91 UNION SELECT 92 UNION SELECT 93 UNION SELECT 94 UNION SELECT 95 UNION SELECT 96 UNION SELECT 97 UNION SELECT 98 UNION SELECT 99 UNION SELECT 100) AS numbers;

COMMIT;

-- 插入笔记 (50条)
INSERT INTO note (id, user_id, title, content, images, tags, location, stable_random, like_count, comment_count, favorite_count, view_count, forward_count, hot_score, status, created_at) VALUES
(1, 3, '地道的四川火锅配方', '今天分享我家传了三十年的四川火锅配方！底料用郫县豆瓣酱，配上花椒、干辣椒、八角、桂皮等香料，小火慢炒两个小时，香气四溢。', '["https://picsum.photos/800/600?random=1"]', '["美食", "火锅", "川菜"]', '成都', 0.172270, 1250, 89, 456, 8900, 234, 2789, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 3, '手把手教你做提拉米苏', '史上最详细的提拉米苏教程！马斯卡彭奶酪要室温软化，手指饼干只用蘸咖啡液不要泡，层叠三次冷藏四小时以上。', '["https://picsum.photos/800/600?random=2"]', '["烘焙", "甜点", "提拉米苏"]', '上海', 0.182330, 890, 67, 320, 5600, 123, 2341, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(3, 4, '云南大理三日游攻略', '大理三天两夜精华攻略：Day1苍山洱海，Day2古城漫游+喜洲古镇，Day3双廊日落。住宿推荐洱海边民宿。', '["https://picsum.photos/800/600?random=3"]', '["旅行", "云南", "大理"]', '大理', 0.278900, 2340, 156, 890, 15600, 456, 6789, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(4, 4, '日本京都赏枫最佳路线', '秋天去京都看红叶，推荐一条避开人潮的私藏路线：永观堂→南禅寺→哲学之道→祇园。', '["https://picsum.photos/800/600?random=4"]', '["旅行", "日本", "红叶"]', '京都', 0.312450, 1890, 123, 678, 12300, 345, 4567, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(5, 5, '2024最值得买的笔记本电脑', '今年笔记本电脑市场神仙打架！MacBook Pro M3、ThinkPad X1 Carbon、ROG幻16，到底选哪款？', '["https://picsum.photos/800/600?random=5"]', '["科技", "数码", "笔记本"]', '深圳', 0.356780, 3456, 234, 1234, 25000, 567, 8901, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(6, 5, 'iPhone15 Pro深度测评', '用了三个月iPhone15 Pro，钛金属机身、A17 Pro芯片、5倍长焦镜头，到底值不值这个价？', '["https://picsum.photos/800/600?random=6"]', '["科技", "手机", "苹果"]', '北京', 0.389010, 4567, 312, 1567, 32000, 678, 11234, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(7, 2, '春季穿搭灵感', '春天穿什么？分享几套通勤和约会穿搭，衬衫+半裙、西装外套+牛仔裤，轻松get高级感。', '["https://picsum.photos/800/600?random=7"]', '["时尚", "穿搭", "春季"]', '上海', 0.345670, 2345, 167, 890, 18000, 456, 6789, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(8, 6, '新手跑步入门指南', '想开始跑步但不知道怎么做？从装备到训练计划，跑步新手看这一篇就够了。', '["https://picsum.photos/800/600?random=8"]', '["运动", "跑步", "健身"]', '北京', 0.312340, 1678, 123, 589, 12000, 312, 4567, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(9, 9, '养猫新手指南', '准备养猫需要准备什么？猫粮、猫砂盆、猫窝、玩具...分享我的养猫清单。', '["https://picsum.photos/800/600?random=9"]', '["宠物", "猫", "新手"]', '北京', 0.367890, 3456, 245, 1234, 28000, 678, 9876, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(10, 7, '油画入门基础教程', '想学油画但不知从何开始？材料准备、构图基础，调色技巧，零基础也能画出第一幅作品。', '["https://picsum.photos/800/600?random=10"]', '["艺术", "油画", "教程"]', '北京', 0.287650, 1234, 98, 456, 9800, 234, 3456, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(11, 10, '古典音乐入门推荐', '想听古典音乐但不知道从哪里开始？推荐几部入门级作品，让你爱上古典乐。', '["https://picsum.photos/800/600?random=11"]', '["音乐", "古典", "入门"]', '维也纳', 0.276540, 987, 76, 356, 7800, 189, 2345, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(12, 8, '2024年阅读书单', '今年已经读了47本书，精选十本推荐给大家，类型涵盖小说、历史、心理学。', '["https://picsum.photos/800/600?random=12"]', '["阅读", "书单", "推荐"]', '北京', 0.312340, 1567, 123, 578, 12000, 312, 4567, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(13, 11, '2024年必看电影清单', '今年看了几十部电影，选出十部年度最佳，类型涵盖科幻、喜剧、剧情、动画。', '["https://picsum.photos/800/600?random=13"]', '["电影", "推荐", "年度"]', '北京', 0.345670, 2345, 178, 876, 19000, 456, 6789, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(14, 12, '索尼A7M4对比佳能R6II', '全画幅微单之争！索尼A7M4和佳能R6II深度对比，对焦、宽容度、视频能力全面测评。', '["https://picsum.photos/800/600?random=14"]', '["科技", "相机", "摄影"]', '上海', 0.323450, 2345, 178, 890, 18000, 456, 6789, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(15, 13, '游戏主播的日常', '很多人问我当游戏主播是什么体验？今天就来分享一下我的日常工作和生活。', '["https://picsum.photos/800/600?random=15"]', '["游戏", "主播", "日常"]', '成都', 0.298760, 1234, 89, 456, 8900, 234, 3456, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(16, 14, '平价替代护肤品清单', '大牌护肤品的平价替代！兰蔻小黑瓶、雅诗兰黛小棕瓶，效果差不多价格差一半。', '["https://picsum.photos/800/600?random=16"]', '["时尚", "美妆", "护肤"]', '杭州', 0.312340, 1890, 134, 678, 13000, 345, 4567, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(17, 15, '家居收纳技巧', '换季了衣服堆成山？教你三步整理衣橱，同款收纳盒让衣柜变大两倍。', '["https://picsum.photos/800/600?random=17"]', '["时尚", "收纳", "生活"]', '北京', 0.256780, 876, 67, 312, 6700, 167, 2345, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(18, 16, '股票入门知识', 'A股市场复杂多变，作为新手应该如何入门？今天分享一些基础知识。', '["https://picsum.photos/800/600?random=18"]', '["财经", "股票", "入门"]', '上海', 0.298760, 1567, 112, 534, 11000, 289, 4567, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(19, 17, '留学申请经验分享', '申请海外研究生需要准备哪些材料？GPA、托福雅思、文书如何提升？', '["https://picsum.photos/800/600?random=19"]', '["教育", "留学", "申请"]', '北京', 0.287650, 1234, 98, 456, 9800, 234, 3456, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(20, 18, '三国演义解读', '很多人读三国只看到谋略，其实里面最动人的是那些小人物的命运。', '["https://picsum.photos/800/600?random=20"]', '["历史", "文学", "解读"]', '西安', 0.323450, 1789, 134, 678, 14000, 356, 4567, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(21, 19, '新能源汽车选购', '想买电动车？比亚迪、特斯拉、蔚来该怎么选？从续航、配置、价格对比。', '["https://picsum.photos/800/600?random=21"]', '["汽车", "新能源", "选购"]', '深圳', 0.345670, 2345, 167, 789, 17000, 389, 5678, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(22, 20, '十二星座性格分析', '每个星座都有独特的性格特点，来看看你属于哪种吧！', '["https://picsum.photos/800/600?random=22"]', '["星座", "性格", "分析"]', '北京', 0.276540, 1123, 89, 412, 8900, 234, 3456, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(23, 21, '减脂餐食谱一周不重样', '减脂期间吃什么？分享一周七天的减脂餐食谱，好吃又不饿。', '["https://picsum.photos/800/600?random=23"]', '["运动", "减脂", "饮食"]', '深圳', 0.345670, 2789, 198, 1023, 21000, 534, 6789, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(24, 22, '广东早茶点心清单', '去广州一定要尝的早茶点心：虾饺、烧麦、叉烧包、蛋挞、肠粉...', '["https://picsum.photos/800/600?random=24"]', '["美食", "粤菜", "早茶"]', '广州', 0.156780, 432, 38, 156, 2800, 45, 1234, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(25, 23, '家庭版戚风蛋糕', '戚风蛋糕看似简单其实讲究很多！分享一下我失败过十次总结出的经验。', '["https://picsum.photos/800/600?random=25"]', '["烘焙", "蛋糕", "戚风"]', '杭州', 0.234560, 789, 56, 234, 4500, 89, 2345, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(26, 24, '自驾318川藏线全攻略', '人生必走一次的318川藏线！从成都出发到拉萨，全程2000多公里。', '["https://picsum.photos/800/600?random=26"]', '["旅行", "自驾", "西藏"]', '成都', 0.289760, 1567, 98, 567, 9800, 234, 4567, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(27, 25, '泰国曼谷清迈七天游', '泰国落地签太方便了！曼谷逛大皇宫，清迈逛古城。', '["https://picsum.photos/800/600?random=27"]', '["旅行", "泰国", "东南亚"]', '曼谷', 0.234560, 987, 76, 345, 6700, 145, 2345, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(28, 26, 'ChatGPT使用技巧大全', 'ChatGPT不只是问答机器！分享10个高效使用技巧。', '["https://picsum.photos/800/600?random=28"]', '["科技", "AI", "效率"]', '硅谷', 0.412340, 5678, 456, 2345, 45000, 890, 12345, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(29, 27, '程序员电脑配置推荐', '写代码到底需要什么配置的电脑？给你一份省钱又好用的配置清单。', '["https://picsum.photos/800/600?random=29"]', '["科技", "程序员", "装机"]', '杭州', 0.298760, 1890, 145, 678, 14000, 345, 4567, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(30, 28, '小众设计师品牌推荐', '不想撞款？这几个小众设计师品牌必须知道。', '["https://picsum.photos/800/600?random=30"]', '["时尚", "品牌", "小众"]', '巴黎', 0.287650, 1234, 98, 456, 9800, 234, 3456, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(31, 29, '职场穿搭指南', '职场穿着也能又美又专业！不同场合的穿搭公式。', '["https://picsum.photos/800/600?random=31"]', '["时尚", "职场", "穿搭"]', '深圳', 0.298760, 1456, 112, 534, 11000, 289, 4567, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(32, 30, 'NBA西部季后赛分析', '今年西部竞争太激烈了！掘金、太阳、湖人谁能冲出西部？', '["https://picsum.photos/800/600?random=32"]', '["运动", "篮球", "NBA"]', '洛杉矶', 0.287650, 1234, 98, 456, 9800, 234, 3456, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(33, 31, 'C罗梅西时代终结了吗', '绝代双骄时代落幕？评价一下梅罗职业生涯末期表现。', '["https://picsum.photos/800/600?random=33"]', '["运动", "足球", "梅西"]', '马德里', 0.323450, 1567, 112, 578, 11000, 289, 4567, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(34, 32, '布偶猫喂养经验', '布偶猫真的太可爱了！但肠胃也比较脆弱，分享一下我的喂养经验。', '["https://picsum.photos/800/600?random=34"]', '["宠物", "猫", "布偶"]', '上海', 0.323450, 2345, 178, 876, 19000, 456, 6789, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(35, 33, '狗狗基础指令训练', '坐下、握手、趴下...狗狗基础指令其实不难，关键是用对方法和零食。', '["https://picsum.photos/800/600?random=35"]', '["宠物", "狗", "训练"]', '杭州', 0.298760, 1567, 123, 567, 12000, 312, 4567, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(36, 34, '吉他初学者必学曲目', '学吉他从哪里开始？推荐几首适合新手的入门曲目。', '["https://picsum.photos/800/600?random=36"]', '["音乐", "吉他", "教程"]', '北京', 0.312340, 1456, 112, 534, 11000, 289, 4567, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(37, 35, 'HIFI耳机选购指南', '想入坑HIFI但预算有限？1000-5000元价位有哪些值得买的耳机推荐。', '["https://picsum.photos/800/600?random=37"]', '["音乐", "HIFI", "耳机"]', '深圳', 0.356780, 1678, 123, 612, 13000, 312, 4567, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(38, 36, '摄影构图十大法则', '提升摄影水平的第一步：掌握构图！分享十个简单实用的构图法则。', '["https://picsum.photos/800/600?random=38"]', '["艺术", "摄影", "教程"]', '上海', 0.345670, 2345, 167, 876, 19000, 456, 6789, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(39, 37, '手账入门：从零开始', '手账是什么？需要准备什么？手账小白入门指南。', '["https://picsum.photos/800/600?random=39"]', '["艺术", "手账", "文具"]', '杭州', 0.298760, 1567, 123, 578, 12000, 312, 4567, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(40, 38, '职场成长经验分享', '工作五年，从月薪3000到年薪50万，我总结了这些职场经验。', '["https://picsum.photos/800/600?random=40"]', '["职场", "成长", "经验"]', '北京', 0.345670, 2345, 178, 876, 19000, 456, 6789, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(41, 39, '基金入门知识科普', '不想存银行定期？基金入门科普，让你了解基金是什么。', '["https://picsum.photos/800/600?random=41"]', '["理财", "基金", "入门"]', '上海', 0.298760, 1567, 123, 578, 12000, 312, 4567, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(42, 40, '亲子手工活动', '周末陪孩子做什么？分享几个简单有趣的亲子手工活动。', '["https://picsum.photos/800/600?random=42"]', '["亲子", "教育", "手工"]', '北京', 0.287650, 1234, 98, 456, 9800, 234, 3456, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(43, 6, '五个居家健身动作', '不需要健身房，五个动作练遍全身：深蹲、俯卧撑、平板支撑...', '["https://picsum.photos/800/600?random=43"]', '["运动", "健身", "居家"]', '上海', 0.356780, 2345, 167, 876, 18000, 456, 6789, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(44, 9, '猫咪行为解读', '猫咪这些行为是什么意思？摇尾巴、蹭腿、踩奶...带你读懂猫主子的小心思。', '["https://picsum.photos/800/600?random=44"]', '["宠物", "猫", "行为"]', '广州', 0.345670, 2789, 198, 1023, 23000, 534, 6789, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(45, 7, '水彩画写生技巧', '户外水彩写生需要注意什么？光线、时间和水分控制。', '["https://picsum.photos/800/600?random=45"]', '["艺术", "水彩", "写生"]', '苏州', 0.323450, 1123, 89, 412, 8900, 234, 3456, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(46, 10, '流行音乐推荐歌单', '分享最近在听的一些好歌，涵盖华语、欧美、日韩。', '["https://picsum.photos/800/600?random=46"]', '["音乐", "歌单", "推荐"]', '上海', 0.298760, 1234, 98, 456, 9800, 234, 3456, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(47, 8, '如何养成阅读习惯', '以前一年读不完一本书，现在每月至少四本。我是如何培养阅读习惯的。', '["https://picsum.photos/800/600?random=47"]', '["阅读", "习惯", "方法"]', '上海', 0.276540, 1234, 98, 456, 9800, 245, 3456, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(48, 11, '诺兰电影深度解析', '盗梦空间、星际穿越、敦刻尔克...诺兰电影的叙事技巧和视觉语言深度分析。', '["https://picsum.photos/800/600?random=48"]', '["电影", "导演", "分析"]', '洛杉矶', 0.323450, 1789, 134, 678, 14000, 356, 4567, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(49, 26, 'Midjourney注册和使用教程', 'AI绘画工具Midjourney注册、订阅、提示词技巧，从入门到精通的完整教程。', '["https://picsum.photos/800/600?random=49"]', '["科技", "AI", "绘画"]', '北京', 0.398760, 4567, 345, 1789, 35000, 678, 12345, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(50, 5, '智能家居入门指南', '从零开始搭建智能家居：米家生态链产品怎么选，一篇讲清楚。', '["https://picsum.photos/800/600?random=50"]', '["科技", "智能家居", "IoT"]', '深圳', 0.276540, 1567, 123, 567, 11000, 289, 4567, 1, DATE_SUB(NOW(), INTERVAL 1 DAY));

COMMIT;

-- 插入关注关系 (使用 INSERT IGNORE 避免重复)
INSERT IGNORE INTO follow (follower_id, following_id, created_at)
SELECT DISTINCT 
  302 + FLOOR(a.n * 100 + b.n),
  CASE 
    WHEN RAND() < 0.5 THEN FLOOR(RAND() * 20) + 2
    WHEN RAND() < 0.8 THEN FLOOR(RAND() * 60) + 22
    ELSE FLOOR(RAND() * 100) + 42
  END,
  DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY)
FROM (SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) AS a,
     (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) AS b,
     (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7) AS c
WHERE 302 + a.n * 100 + b.n <= 1000;

COMMIT;

-- 插入评论
INSERT INTO note_comment (note_id, user_id, parent_id, root_id, content, like_count, reply_count, hot_score, status, created_at)
SELECT 
  1 + FLOOR(RAND() * 50),
  302 + FLOOR(RAND() * 200),
  0, 0,
  '写得真好！受益匪浅，学到了很多。',
  5 + FLOOR(RAND() * 50),
  0, 0, 1,
  DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 7) DAY)
FROM (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) AS a,
     (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) AS b,
     (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) AS c;

UPDATE note_comment SET hot_score = like_count * 2 WHERE parent_id = 0;
COMMIT;

-- 插入点赞收藏 (使用 INSERT IGNORE)
INSERT IGNORE INTO note_like (note_id, user_id, created_at)
SELECT DISTINCT 1 + FLOOR(RAND() * 50), 302 + FLOOR(RAND() * 700), DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY)
FROM (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) AS a,
     (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) AS b,
     (SELECT 1 UNION SELECT 2 UNION SELECT 3) AS c;

INSERT IGNORE INTO note_favorite (note_id, user_id, created_at)
SELECT DISTINCT 1 + FLOOR(RAND() * 50), 302 + FLOOR(RAND() * 700), DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY)
FROM (SELECT 1 UNION SELECT 2 UNION SELECT 3) AS a,
     (SELECT 1 UNION SELECT 2 UNION SELECT 3) AS b;

COMMIT;

-- 插入通知
INSERT INTO notification (user_id, type, from_user_id, note_id, content, is_read, created_at)
SELECT 302 + FLOOR(RAND() * 100), 1, 2 + FLOOR(RAND() * 20), 1 + FLOOR(RAND() * 50), '赞了你的笔记', 0, DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 7) DAY)
FROM (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) AS a;

INSERT INTO notification (user_id, type, from_user_id, note_id, content, is_read, created_at)
SELECT 302 + FLOOR(RAND() * 100), 2, 2 + FLOOR(RAND() * 20), 1 + FLOOR(RAND() * 50), '评论了你的笔记', 0, DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 7) DAY)
FROM (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7) AS a;

INSERT INTO notification (user_id, type, from_user_id, content, is_read, created_at)
SELECT 2 + FLOOR(RAND() * 100), 3, 302 + FLOOR(RAND() * 200), '关注了你', 0, DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 7) DAY)
FROM (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) AS a;

COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
SET UNIQUE_CHECKS = 1;

-- 数据验证
SELECT 'Users' AS metric, COUNT(*) AS value FROM user
UNION ALL SELECT 'Notes', COUNT(*) FROM note WHERE status = 1
UNION ALL SELECT 'Follows', COUNT(*) FROM follow
UNION ALL SELECT 'Comments', COUNT(*) FROM note_comment
UNION ALL SELECT 'Note Likes', COUNT(*) FROM note_like
UNION ALL SELECT 'Favorites', COUNT(*) FROM note_favorite
UNION ALL SELECT 'Notifications', COUNT(*) FROM notification;