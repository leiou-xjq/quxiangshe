-- ============================================
-- 趣享社测试数据生成脚本
-- 执行方式: mysql -u root -p123456 quxiangshe < test_data.sql
-- ============================================

-- 关闭外键检查以提高插入速度
SET FOREIGN_KEY_CHECKS = 0;
SET UNIQUE_CHECKS = 0;
SET AUTOCOMMIT = 0;

-- ============================================
-- 第一部分：清空相关表数据
-- ============================================

TRUNCATE TABLE notification;
TRUNCATE TABLE comment_like;
TRUNCATE TABLE note_favorite;
TRUNCATE TABLE note_like;
TRUNCATE TABLE note_comment;
TRUNCATE TABLE follow;
TRUNCATE TABLE note;
-- 注意：不清理 user 表，保留 admin (id=1)

-- ============================================
-- 第二部分：插入博主用户 (ID 2-301)
-- ============================================

INSERT INTO user (id, username, phone, password, avatar, nickname, gender, bio, status, role, created_at) VALUES
-- 大博主 (ID 2-21, 粉丝 > 10万)
(2, 'xiaomei', '13800000002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/201', '穿搭达人小美', 2, '分享每日穿搭灵感，教你穿出高级感', 1, 'USER', NOW()),
(3, 'dachu', '13800000003', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/202', '美食家大厨', 1, '二十年厨艺经验，专注分享家常美味', 1, 'USER', NOW()),
(4, 'lvxing', '13800000004', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/203', '旅行摄影师', 1, '用镜头记录世界之美，专注旅行摄影攻略', 1, 'USER', NOW()),
(5, 'keji', '13800000005', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/204', '科技极客', 1, '最新数码产品测评，科技前沿资讯', 1, 'USER', NOW()),
(6, 'yundong', '13800000006', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/205', '运动达人', 1, '健身、跑步、户外运动爱好者', 1, 'USER', NOW()),
(7, 'yishujia', '13800000007', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/206', '自由艺术家', 1, '油画、水彩、插画创作分享', 1, 'USER', NOW()),
(8, 'dushu', '13800000008', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/207', '读书人', 2, '每年阅读100本书，分享读书笔记', 1, 'USER', NOW()),
(9, 'chongwu', '13800000009', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/208', '萌宠博主', 2, '养猫养狗经验分享，萌宠日常', 1, 'USER', NOW()),
(10, 'yinyue', '13800000010', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/209', '音乐爱好者', 1, '古典乐、流行乐、音乐分享', 1, 'USER', NOW()),
(11, 'dianying', '13800000011', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/210', '电影推荐官', 1, '院线新片评测、经典电影推荐', 1, 'USER', NOW()),
(12, 'shuma', '13800000012', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/211', '数码测评师', 1, '手机、电脑、数码产品深度测评', 1, 'USER', NOW()),
(13, 'youxi', '13800000013', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/212', '游戏主播', 1, 'Steam游戏、主机游戏实况解说', 1, 'USER', NOW()),
(14, 'meizhuang', '13800000014', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/213', '美妆达人', 2, '化妆技巧、护肤品测评、穿搭分享', 1, 'USER', NOW()),
(15, 'jiaju', '13800000015', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/214', '家居博主', 2, '装修灵感、家居好物、收纳技巧', 1, 'USER', NOW()),
(16, 'gupiao', '13800000016', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/215', '股票分析师', 1, 'A股分析、投资心得、财经解读', 1, 'USER', NOW()),
(17, 'jiaoyu', '13800000017', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/216', '教育博主', 1, 'K12教育、留学资讯、学习方法', 1, 'USER', NOW()),
(18, 'lishi', '13800000018', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/217', '历史爱好者', 1, '历史故事、人物传记、文化解读', 1, 'USER', NOW()),
(19, 'qiche', '13800000019', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/218', '汽车达人', 1, '新车评测、购车指南、用车知识', 1, 'USER', NOW()),
(20, 'xingzuo', '13800000020', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/219', '星座博主', 2, '星座运势、情感分析、星座配对', 1, 'USER', NOW()),
(21, 'jianshen', '13800000021', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/220', '健身教练', 1, '科学健身、增肌减脂、运动康复', 1, 'USER', NOW()),

-- 中博主 (ID 22-101, 粉丝 1万-10万)
(22, 'meishi01', '13800000022', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/221', '地方美食家', 1, '探索各地特色美食', 1, 'USER', NOW()),
(23, 'meishi02', '13800000023', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/222', '烘焙达人', 2, '面包、蛋糕、甜点制作', 1, 'USER', NOW()),
(24, 'lvyou01', '13800000024', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/223', '自驾游攻略', 1, '公路旅行、自驾路线分享', 1, 'USER', NOW()),
(25, 'lvyou02', '13800000025', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/224', '出境游达人', 1, '护照签证、出境游攻略', 1, 'USER', NOW()),
(26, 'keji01', '13800000026', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/225', 'AI研究员', 1, '人工智能技术解读、前沿动态', 1, 'USER', NOW()),
(27, 'keji02', '13800000027', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/226', '程序员老王', 1, '编程技术、软件开发经验', 1, 'USER', NOW()),
(28, 'shishang01', '13800000028', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/227', '潮流教主', 1, '街头潮流、潮牌穿搭', 1, 'USER', NOW()),
(29, 'shishang02', '13800000029', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/228', '轻奢主义', 2, '小众品牌、轻奢生活方式', 1, 'USER', NOW()),
(30, 'tiyu01', '13800000030', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/229', '篮球解说', 1, 'NBA、CBA赛事分析', 1, 'USER', NOW()),
(31, 'tiyu02', '13800000031', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/230', '足球先锋', 1, '五大联赛、世界杯热点', 1, 'USER', NOW()),
(32, 'chongwu01', '13800000032', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/231', '养猫日记', 2, '猫咪日常、养猫知识', 1, 'USER', NOW()),
(33, 'chongwu02', '13800000033', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/232', '汪星人俱乐部', 1, '狗狗养护、训犬技巧', 1, 'USER', NOW()),
(34, 'yinxiang01', '13800000034', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/233', '乐器学习', 1, '吉他、钢琴、架子鼓教学', 1, 'USER', NOW()),
(35, 'yinxiang02', '13800000035', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/234', 'HIFI发烧友', 1, '音响设备、音乐欣赏', 1, 'USER', NOW()),
(36, 'yishu01', '13800000036', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/235', '摄影技巧', 1, '摄影教程、后期修图', 1, 'USER', NOW()),
(37, 'yishu02', '13800000037', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/236', '手账达人', 2, '文具、手账制作、收纳', 1, 'USER', NOW()),
(38, 'qita01', '13800000038', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/237', '职场成长', 1, '职业规划、职场技能', 1, 'USER', NOW()),
(39, 'qita02', '13800000039', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/238', '理财知识', 1, '基金、理财入门科普', 1, 'USER', NOW()),
(40, 'qita03', '13800000040', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a', 'https://picsum.photos/239', '亲子教育', 2, '育儿经验、亲子活动', 1, 'USER', NOW());

-- 继续插入更多中博主 (ID 41-101)
INSERT INTO user (id, username, phone, password, avatar, nickname, gender, bio, status, role, created_at) 
SELECT 
  40 + n,
  CONCAT('blogger_', 40 + n),
  CONCAT('138', LPAD(40000040 + n, 8, '0')),
  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a',
  CONCAT('https://picsum.photos/240?random=', n),
  CONCAT('博主', 40 + n),
  FLOOR(RAND() * 3),
  CONCAT('这是一个专注于某领域的博主', 40 + n),
  1, 'USER', NOW()
FROM (
  SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
  UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
  UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15
  UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20
  UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25
  UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29 UNION SELECT 30
  UNION SELECT 31 UNION SELECT 32 UNION SELECT 33 UNION SELECT 34 UNION SELECT 35
  UNION SELECT 36 UNION SELECT 37 UNION SELECT 38 UNION SELECT 39 UNION SELECT 40
  UNION SELECT 41 UNION SELECT 42 UNION SELECT 43 UNION SELECT 44 UNION SELECT 45
  UNION SELECT 46 UNION SELECT 47 UNION SELECT 48 UNION SELECT 49 UNION SELECT 50
  UNION SELECT 51 UNION SELECT 52 UNION SELECT 53 UNION SELECT 54 UNION SELECT 55
  UNION SELECT 56 UNION SELECT 57 UNION SELECT 58 UNION SELECT 59 UNION SELECT 60
) AS numbers;

-- 小博主 (ID 102-301, 粉丝 50-999)
INSERT INTO user (id, username, phone, password, avatar, nickname, gender, bio, status, role, created_at) 
SELECT 
  100 + n,
  CONCAT('small_blogger_', 100 + n),
  CONCAT('138', LPAD(10000100 + n, 8, '0')),
  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a',
  CONCAT('https://picsum.photos/300?random=', n),
  CONCAT('小博主', 100 + n),
  FLOOR(RAND() * 3),
  '热爱分享的普通用户',
  1, 'USER', NOW()
FROM (
  SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
  UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
  UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15
  UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20
  UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25
  UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29 UNION SELECT 30
  UNION SELECT 31 UNION SELECT 32 UNION SELECT 33 UNION SELECT 34 UNION SELECT 35
  UNION SELECT 36 UNION SELECT 37 UNION SELECT 38 UNION SELECT 39 UNION SELECT 40
  UNION SELECT 41 UNION SELECT 42 UNION SELECT 43 UNION SELECT 44 UNION SELECT 45
  UNION SELECT 46 UNION SELECT 47 UNION SELECT 48 UNION SELECT 49 UNION SELECT 50
  UNION SELECT 51 UNION SELECT 52 UNION SELECT 53 UNION SELECT 54 UNION SELECT 55
  UNION SELECT 56 UNION SELECT 57 UNION SELECT 58 UNION SELECT 59 UNION SELECT 60
  UNION SELECT 61 UNION SELECT 62 UNION SELECT 63 UNION SELECT 64 UNION SELECT 65
  UNION SELECT 66 UNION SELECT 67 UNION SELECT 68 UNION SELECT 69 UNION SELECT 70
  UNION SELECT 71 UNION SELECT 72 UNION SELECT 73 UNION SELECT 74 UNION SELECT 75
  UNION SELECT 76 UNION SELECT 77 UNION SELECT 78 UNION SELECT 79 UNION SELECT 80
  UNION SELECT 81 UNION SELECT 82 UNION SELECT 83 UNION SELECT 84 UNION SELECT 85
  UNION SELECT 86 UNION SELECT 87 UNION SELECT 88 UNION SELECT 89 UNION SELECT 90
  UNION SELECT 91 UNION SELECT 92 UNION SELECT 93 UNION SELECT 94 UNION SELECT 95
  UNION SELECT 96 UNION SELECT 97 UNION SELECT 98 UNION SELECT 99 UNION SELECT 100
  UNION SELECT 101 UNION SELECT 102 UNION SELECT 103 UNION SELECT 104 UNION SELECT 105
  UNION SELECT 106 UNION SELECT 107 UNION SELECT 108 UNION SELECT 109 UNION SELECT 110
  UNION SELECT 111 UNION SELECT 112 UNION SELECT 113 UNION SELECT 114 UNION SELECT 115
  UNION SELECT 116 UNION SELECT 117 UNION SELECT 118 UNION SELECT 119 UNION SELECT 120
  UNION SELECT 121 UNION SELECT 122 UNION SELECT 123 UNION SELECT 124 UNION SELECT 125
  UNION SELECT 126 UNION SELECT 127 UNION SELECT 128 UNION SELECT 129 UNION SELECT 130
  UNION SELECT 131 UNION SELECT 132 UNION SELECT 133 UNION SELECT 134 UNION SELECT 135
  UNION SELECT 136 UNION SELECT 137 UNION SELECT 138 UNION SELECT 139 UNION SELECT 140
  UNION SELECT 141 UNION SELECT 142 UNION SELECT 143 UNION SELECT 144 UNION SELECT 145
  UNION SELECT 146 UNION SELECT 147 UNION SELECT 148 UNION SELECT 149 UNION SELECT 150
  UNION SELECT 151 UNION SELECT 152 UNION SELECT 153 UNION SELECT 154 UNION SELECT 155
  UNION SELECT 156 UNION SELECT 157 UNION SELECT 158 UNION SELECT 159 UNION SELECT 160
  UNION SELECT 161 UNION SELECT 162 UNION SELECT 163 UNION SELECT 164 UNION SELECT 165
  UNION SELECT 166 UNION SELECT 167 UNION SELECT 168 UNION SELECT 169 UNION SELECT 170
  UNION SELECT 171 UNION SELECT 172 UNION SELECT 173 UNION SELECT 174 UNION SELECT 175
  UNION SELECT 176 UNION SELECT 177 UNION SELECT 178 UNION SELECT 179 UNION SELECT 180
  UNION SELECT 181 UNION SELECT 182 UNION SELECT 183 UNION SELECT 184 UNION SELECT 185
  UNION SELECT 186 UNION SELECT 187 UNION SELECT 188 UNION SELECT 189 UNION SELECT 190
  UNION SELECT 191 UNION SELECT 192 UNION SELECT 193 UNION SELECT 194 UNION SELECT 195
  UNION SELECT 196 UNION SELECT 197 UNION SELECT 198 UNION SELECT 199 UNION SELECT 200
) AS numbers;

-- 普通消费用户 (ID 302-100301)
INSERT INTO user (id, username, phone, password, avatar, nickname, gender, bio, status, role, created_at) 
SELECT 
  300 + n,
  CONCAT('user_', 300 + n),
  CONCAT('139', LPAD(30000001 + n, 8, '0')),
  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a',
  CONCAT('https://picsum.photos/400?random=', n),
  CONCAT('用户', 300 + n),
  FLOOR(RAND() * 3),
  '热爱生活的普通用户',
  1, 'USER', NOW()
FROM (
  SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
  UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
) AS tens, (
  SELECT 0 n UNION ALL SELECT 1000 UNION ALL SELECT 2000 UNION ALL SELECT 3000 UNION ALL SELECT 4000
  UNION ALL SELECT 5000 UNION ALL SELECT 6000 UNION ALL SELECT 7000 UNION ALL SELECT 8000 UNION ALL SELECT 9000
) AS ten_thousands
WHERE 300 + n + ten_thousands.n <= 100301;

COMMIT;

-- ============================================
-- 第三部分：插入笔记 (100条)
-- ============================================

INSERT INTO note (id, user_id, title, content, images, tags, location, stable_random, like_count, comment_count, favorite_count, view_count, forward_count, status, created_at) VALUES
-- 美食类 (10条)
(1, 3, '地道的四川火锅配方', '今天分享我家传了三十年的四川火锅配方！底料用郫县豆瓣酱，配上花椒、干辣椒、八角、桂皮等香料，小火慢炒两个小时，香气四溢。', '["https://picsum.photos/800/600?random=1"]', '["美食", "火锅", "川菜"]', '成都', 0.172270, 1250, 89, 456, 8900, 234, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 3, '手把手教你做提拉米苏', '史上最详细的提拉米苏教程！马斯卡彭奶酪要室温软化，手指饼干只用蘸咖啡液不要泡，层叠三次冷藏四小时以上。', '["https://picsum.photos/800/600?random=2"]', '["烘焙", "甜点", "提拉米苏"]', '上海', 0.182330, 890, 67, 320, 5600, 123, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(3, 22, '东北酸菜的腌制方法', '入冬了，又到了腌酸菜的季节。选用新鲜大白菜，撒盐加水密封，一个月就能吃到正宗的东北酸菜。', '["https://picsum.photos/800/600?random=3"]', '["美食", "腌制", "东北菜"]', '哈尔滨', 0.192450, 567, 45, 189, 3400, 67, 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(4, 22, '广东早茶点心清单', '去广州一定要尝的早茶点心：虾饺、烧麦、叉烧包、蛋挞、肠粉、凤爪、萝卜糕...每一样都让人回味无穷！', '["https://picsum.photos/800/600?random=4"]', '["美食", "粤菜", "早茶"]', '广州', 0.156780, 432, 38, 156, 2800, 45, 1, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(5, 23, '家庭版戚风蛋糕', '戚风蛋糕看似简单其实讲究很多！蛋白打发要到位，混合时不要画圈，烤箱温度要稳定。分享一下我失败过十次总结出的经验。', '["https://picsum.photos/800/600?random=5"]', '["烘焙", "蛋糕", "戚风"]', '杭州', 0.234560, 789, 56, 234, 4500, 89, 1, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(6, 23, '十分钟营养早餐', '上班族也能吃上丰盛早餐！鸡蛋三明治+牛奶+水果，十分钟搞定，营养均衡。', '["https://picsum.photos/800/600?random=6"]', '["美食", "早餐", "快手菜"]', '深圳', 0.167890, 345, 28, 98, 2100, 34, 1, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(7, 3, '麻辣香锅家庭版', '买了一包麻辣香锅底料，加上自己喜欢的蔬菜、肉丸、午餐肉，在家也能做出餐厅的味道！', '["https://picsum.photos/800/600?random=7"]', '["美食", "川菜", "家常菜"]', '重庆', 0.145670, 456, 34, 123, 3200, 56, 1, DATE_SUB(NOW(), INTERVAL 7 DAY)),
(8, 22, '老火靓汤食谱', '广东人离不开的老火汤！排骨莲藕汤、乌鸡汤、花生猪脚汤...每个季节都有对应的汤谱。', '["https://picsum.photos/800/600?random=8"]', '["美食", "粤菜", "汤"]', '佛山', 0.198760, 623, 48, 201, 3800, 78, 1, DATE_SUB(NOW(), INTERVAL 8 DAY)),
(9, 23, '如何挑选好牛排', '煎牛排最重要的是选肉！教你看纹路、看颜色、看油花，教你分辨西冷、眼肉、菲力的区别。', '["https://picsum.photos/800/600?random=9"]', '["美食", "牛排", "选购"]', '北京', 0.223450, 534, 41, 178, 4100, 67, 1, DATE_SUB(NOW(), INTERVAL 9 DAY)),
(10, 3, '凉皮自制教程', '夏天最爱的凉皮自己做！面粉加水和成面团，洗出面筋，蒸出面皮，切条凉拌，完美复刻街头小吃。', '["https://picsum.photos/800/600?random=10"]', '["美食", "凉皮", "小吃"]', '西安', 0.187650, 678, 52, 212, 4500, 89, 1, DATE_SUB(NOW(), INTERVAL 10 DAY)),

-- 旅行类 (10条)
(11, 4, '云南大理三日游攻略', '大理三天两夜精华攻略：Day1苍山洱海，Day2古城漫游+喜洲古镇，Day3双廊日落。住宿推荐洱海边民宿。', '["https://picsum.photos/800/600?random=11"]', '["旅行", "云南", "大理"]', '大理', 0.278900, 2340, 156, 890, 15600, 456, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(12, 4, '日本京都赏枫最佳路线', '秋天去京都看红叶，推荐一条避开人潮的私藏路线：永观堂→南禅寺→哲学之道→祇园。', '["https://picsum.photos/800/600?random=12"]', '["旅行", "日本", "红叶"]', '京都', 0.312450, 1890, 123, 678, 12300, 345, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(13, 24, '自驾318川藏线全攻略', '人生必走一次的318川藏线！从成都出发，经康定、稻城亚丁、林芝到拉萨，全程2000多公里。', '["https://picsum.photos/800/600?random=13"]', '["旅行", "自驾", "西藏"]', '成都', 0.289760, 1567, 98, 567, 9800, 234, 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(14, 24, '欧洲背包旅行30天', '辞职后用一个月走了西欧六国：法国→瑞士→意大利→德国→荷兰→比利时。住青旅吃街头美食，人均花费15000。', '["https://picsum.photos/800/600?random=14"]', '["旅行", "欧洲", "穷游"]', '巴黎', 0.245670, 1234, 87, 456, 8900, 189, 1, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(15, 25, '泰国曼谷+清迈七天游', '泰国落地签太方便了！曼谷逛大皇宫+四面佛，清迈逛古城+逛夜市，还要去一次周末夜市。', '["https://picsum.photos/800/600?random=15"]', '["旅行", "泰国", "东南亚"]', '曼谷', 0.234560, 987, 76, 345, 6700, 145, 1, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(16, 4, '厦门鼓浪屿深度游', '鼓浪屿不只是网红打卡地，这里的万国建筑、钢琴博物馆、日光岩日落才是真正的浪漫。', '["https://picsum.photos/800/600?random=16"]', '["旅行", "厦门", "海岛"]', '厦门', 0.198760, 756, 54, 234, 5600, 123, 1, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(17, 25, '新疆喀纳斯秋色', '九月的喀纳斯美得像油画！禾木村的晨雾、喀纳斯湖的湖水、月亮湾的日落，手机随便拍都是大片。', '["https://picsum.photos/800/600?random=17"]', '["旅行", "新疆", "秋色"]', '阿勒泰', 0.267890, 1123, 78, 389, 7800, 234, 1, DATE_SUB(NOW(), INTERVAL 7 DAY)),
(18, 4, '冰岛环岛自驾14天', '冰与火之歌的真实版！黄金圈→南岸→东部峡湾→北部火山→斯奈山半岛，自驾穿越整个冰岛。', '["https://picsum.photos/800/600?random=18"]', '["旅行", "冰岛", "自驾"]', '雷克雅未克', 0.345670, 2345, 167, 789, 15000, 456, 1, DATE_SUB(NOW(), INTERVAL 8 DAY)),
(19, 24, '成都周边古镇推荐', '成都附近藏着很多冷门古镇：街子古镇、黄龙溪、洛带古镇...周末自驾两日游刚刚好。', '["https://picsum.photos/800/600?random=19"]', '["旅行", "成都", "古镇"]', '成都', 0.187650, 543, 43, 189, 4100, 87, 1, DATE_SUB(NOW(), INTERVAL 9 DAY)),
(20, 25, '马尔代夫选岛指南', '马尔代夫一百多个度假岛怎么选？根据预算和需求推荐：浮潜选阿雅达，亲子选神仙珊瑚，性价比选奥露岛。', '["https://picsum.photos/800/600?random=20"]', '["旅行", "海岛", "马尔代夫"]', '马尔代夫', 0.298760, 1678, 112, 567, 11200, 345, 1, DATE_SUB(NOW(), INTERVAL 10 DAY)),

-- 科技类 (8条)
(21, 5, '2024最值得买的笔记本电脑', '今年笔记本电脑市场神仙打架！MacBook Pro M3、ThinkPad X1 Carbon、ROG幻16，到底选哪款？', '["https://picsum.photos/800/600?random=21"]', '["科技", "数码", "笔记本"]', '深圳', 0.356780, 3456, 234, 1234, 25000, 567, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(22, 5, 'iPhone15 Pro深度测评', '用了三个月iPhone15 Pro，钛金属机身、A17 Pro芯片、5倍长焦镜头，到底值不值这个价？', '["https://picsum.photos/800/600?random=22"]', '["科技", "手机", "苹果"]', '北京', 0.389010, 4567, 312, 1567, 32000, 678, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(23, 12, '索尼A7M4对比佳能R6II', '全画幅微单之争！索尼A7M4和佳能R6II深度对比，对焦、宽容度、视频能力全面测评。', '["https://picsum.photos/800/600?random=23"]', '["科技", "相机", "摄影"]', '上海', 0.323450, 2345, 178, 890, 18000, 456, 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(24, 26, 'ChatGPT使用技巧大全', 'ChatGPT不只是问答机器！分享10个高效使用技巧，让你的工作效率提升10倍。', '["https://picsum.photos/800/600?random=24"]', '["科技", "AI", "效率"]', '硅谷', 0.412340, 5678, 456, 2345, 45000, 890, 1, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(25, 27, '程序员电脑配置推荐', '写代码到底需要什么配置的电脑？CPU、内存、硬盘、显示器...给你一份省钱又好用的配置清单。', '["https://picsum.photos/800/600?random=25"]', '["科技", "程序员", "装机"]', '杭州', 0.298760, 1890, 145, 678, 14000, 345, 1, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(26, 5, '智能家居入门指南', '从零开始搭建智能家居：米家生态链产品怎么选，网关/传感器/摄像头一篇讲清楚。', '["https://picsum.photos/800/600?random=26"]', '["科技", "智能家居", "IoT"]', '深圳', 0.276540, 1567, 123, 567, 11000, 289, 1, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(27, 12, '无线耳机横评', 'AirPods Pro2、索尼WF-1000XM5、Bose大鲨2，三款旗舰降噪耳机深度对比。', '["https://picsum.photos/800/600?random=27"]', '["科技", "耳机", "数码"]', '广州', 0.345670, 2789, 198, 987, 20000, 456, 1, DATE_SUB(NOW(), INTERVAL 7 DAY)),
(28, 26, 'Midjourney注册和使用教程', 'AI绘画工具Midjourney注册、订阅、提示词技巧，从入门到精通的完整教程。', '["https://picsum.photos/800/600?random=28"]', '["科技", "AI", "绘画"]', '北京', 0.398760, 4567, 345, 1789, 35000, 678, 1, DATE_SUB(NOW(), INTERVAL 8 DAY)),

-- 时尚类 (7条)
(29, 2, '春季穿搭灵感', '春天穿什么？分享几套通勤和约会穿搭，衬衫+半裙、西装外套+牛仔裤，轻松get高级感。', '["https://picsum.photos/800/600?random=29"]', '["时尚", "穿搭", "春季"]', '上海', 0.345670, 2345, 167, 890, 18000, 456, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(30, 14, '平价替代护肤品清单', '大牌护肤品的平价替代！兰蔻小黑瓶→欧莱雅黑精华，雅诗兰黛小棕瓶→珀莱雅双抗，效果差不多价格差一半。', '["https://picsum.photos/800/600?random=30"]', '["时尚", "美妆", "护肤"]', '杭州', 0.312340, 1890, 134, 678, 13000, 345, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(31, 28, '小众设计师品牌推荐', '不想撞款？这几个小众设计师品牌必须知道：By Far、Jacquemus、Maison Margiela...', '["https://picsum.photos/800/600?random=31"]', '["时尚", "品牌", "小众"]', '巴黎', 0.287650, 1234, 98, 456, 9800, 234, 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(32, 2, '衣橱收纳技巧', '换季了衣服堆成山？教你三步整理衣橱：分类→筛选→收纳，同款收纳盒让衣柜变大两倍。', '["https://picsum.photos/800/600?random=32"]', '["时尚", "收纳", "生活"]', '北京', 0.256780, 876, 67, 312, 6700, 167, 1, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(33, 29, '职场穿搭指南', '职场穿着也能又美又专业！西装裙、衬衫、西装裤，不同场合的穿搭公式。', '["https://picsum.photos/800/600?random=33"]', '["时尚", "职场", "穿搭"]', '深圳', 0.298760, 1456, 112, 534, 11000, 289, 1, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(34, 14, '化妆新手入门', '手把手教你化日常妆：隔离→遮瑕→粉底→眉妆→口红，六个步骤十分钟搞定。', '["https://picsum.photos/800/600?random=34"]', '["时尚", "美妆", "教程"]', '成都', 0.323450, 2345, 178, 876, 17000, 456, 1, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(35, 28, '轻奢包包推荐', '一万预算买什么包？推荐几个口碑好的轻奢品牌：Coach、MK、TB、Furla。', '["https://picsum.photos/800/600?random=35"]', '["时尚", "包包", "轻奢"]', '广州', 0.276540, 1123, 89, 412, 8900, 234, 1, DATE_SUB(NOW(), INTERVAL 7 DAY)),

-- 运动类 (6条)
(36, 6, '新手跑步入门指南', '想开始跑步但不知道怎么做？从装备到训练计划，跑步新手看这一篇就够了。', '["https://picsum.photos/800/600?random=36"]', '["运动", "跑步", "健身"]', '北京', 0.312340, 1678, 123, 589, 12000, 312, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(37, 21, '五个居家健身动作', '不需要健身房，五个动作练遍全身：深蹲、俯卧撑、平板支撑、罗马尼亚硬拉、箭步蹲。', '["https://picsum.photos/800/600?random=37"]', '["运动", "健身", "居家"]', '上海', 0.356780, 2345, 167, 876, 18000, 456, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(38, 30, 'NBA西部季后赛分析', '今年西部竞争太激烈了！掘金、太阳、湖人、快船谁能冲出西部？来一波深度分析。', '["https://picsum.photos/800/600?random=38"]', '["运动", "篮球", "NBA"]', '洛杉矶', 0.287650, 1234, 98, 456, 9800, 234, 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(39, 31, 'C罗梅西时代终结了吗', '绝代双骄时代落幕？评价一下梅罗职业生涯末期表现，以及新生代姆巴佩、哈兰德的崛起。', '["https://picsum.photos/800/600?random=39"]', '["运动", "足球", "梅西"]', '马德里', 0.323450, 1567, 112, 578, 11000, 289, 1, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(40, 6, '户外徒步装备清单', '准备去徒步需要带什么？一份详细的户外装备清单，从背包到登山杖到急救包。', '["https://picsum.photos/800/600?random=40"]', '["运动", "户外", "徒步"]', '成都', 0.298760, 987, 76, 356, 7800, 189, 1, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(41, 21, '减脂餐食谱一周不重样', '减脂期间吃什么？分享一周七天的减脂餐食谱，好吃又不饿，一周掉两斤。', '["https://picsum.photos/800/600?random=41"]', '["运动", "减脂", "饮食"]', '深圳', 0.345670, 2789, 198, 1023, 21000, 534, 1, DATE_SUB(NOW(), INTERVAL 6 DAY)),

-- 宠物类 (5条)
(42, 9, '养猫新手指南', '准备养猫需要准备什么？猫粮、猫砂盆、猫窝、玩具...分享我的养猫清单。', '["https://picsum.photos/800/600?random=42"]', '["宠物", "猫", "新手"]', '北京', 0.367890, 3456, 245, 1234, 28000, 678, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(43, 32, '布偶猫喂养经验', '布偶猫真的太可爱了！但肠胃也比较脆弱，分享一下我的喂养经验和注意事项。', '["https://picsum.photos/800/600?random=43"]', '["宠物", "猫", "布偶"]', '上海', 0.323450, 2345, 178, 876, 19000, 456, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(44, 33, '狗狗基础指令训练', '坐下、握手、趴下...狗狗基础指令其实不难，关键是用对方法和零食。', '["https://picsum.photos/800/600?random=44"]', '["宠物", "狗", "训练"]', '杭州', 0.298760, 1567, 123, 567, 12000, 312, 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(45, 9, '猫咪行为解读', '猫咪这些行为是什么意思？摇尾巴、蹭腿、踩奶...带你读懂猫主子的小心思。', '["https://picsum.photos/800/600?random=45"]', '["宠物", "猫", "行为"]', '广州', 0.345670, 2789, 198, 1023, 23000, 534, 1, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(46, 32, '自制猫饭食谱', '不想只喂猫粮？分享几款营养均衡的自制猫饭食谱，猫咪超爱吃！', '["https://picsum.photos/800/600?random=46"]', '["宠物", "猫", "饮食"]', '成都', 0.312340, 1890, 145, 678, 15000, 389, 1, DATE_SUB(NOW(), INTERVAL 5 DAY)),

-- 艺术类 (4条)
(47, 7, '油画入门基础教程', '想学油画但不知从何开始？材料准备、构图基础、调色技巧，零基础也能画出第一幅作品。', '["https://picsum.photos/800/600?random=47"]', '["艺术", "油画", "教程"]', '北京', 0.287650, 1234, 98, 456, 9800, 234, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(48, 36, '摄影构图十大法则', '提升摄影水平的第一步：掌握构图！分享十个简单实用的构图法则。', '["https://picsum.photos/800/600?random=48"]', '["艺术", "摄影", "教程"]', '上海', 0.345670, 2345, 167, 876, 19000, 456, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(49, 37, '手账入门：从零开始', '手账是什么？需要准备什么？手账小白入门指南，包含基础排版和好用文具推荐。', '["https://picsum.photos/800/600?random=49"]', '["艺术", "手账", "文具"]', '杭州', 0.298760, 1567, 123, 578, 12000, 312, 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(50, 7, '水彩画写生技巧', '户外水彩写生需要注意什么？光线、时间和水分控制，分享我的写生心得。', '["https://picsum.photos/800/600?random=50"]', '["艺术", "水彩", "写生"]', '苏州', 0.323450, 1123, 89, 412, 8900, 234, 1, DATE_SUB(NOW(), INTERVAL 4 DAY)),

-- 音乐类 (4条)
(51, 10, '古典音乐入门推荐', '想听古典音乐但不知道从哪里开始？推荐几部入门级作品，让你爱上古典乐。', '["https://picsum.photos/800/600?random=51"]', '["音乐", "古典", "入门"]', '维也纳', 0.276540, 987, 76, 356, 7800, 189, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(52, 34, '吉他初学者必学曲目', '学吉他从哪里开始？推荐几首适合新手的入门曲目，简单又好听。', '["https://picsum.photos/800/600?random=52"]', '["音乐", "吉他", "教程"]', '北京', 0.312340, 1456, 112, 534, 11000, 289, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(53, 35, 'HIFI耳机选购指南', '想入坑HIFI但预算有限？1000-5000元价位有哪些值得买的耳机推荐。', '["https://picsum.photos/800/600?random=53"]', '["音乐", "HIFI", "耳机"]', '深圳', 0.356780, 1678, 123, 612, 13000, 312, 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(54, 10, '流行音乐推荐歌单', '分享最近在听的一些好歌，涵盖华语、欧美、日韩，总有一首你喜欢。', '["https://picsum.photos/800/600?random=54"]', '["音乐", "歌单", "推荐"]', '上海', 0.298760, 1234, 98, 456, 9800, 234, 1, DATE_SUB(NOW(), INTERVAL 4 DAY)),

-- 电影类 (3条)
(55, 11, '2024年必看电影清单', '今年看了几十部电影，选出十部年度最佳，类型涵盖科幻、喜剧、剧情、动画。', '["https://picsum.photos/800/600?random=55"]', '["电影", "推荐", "年度"]', '北京', 0.345670, 2345, 178, 876, 19000, 456, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(56, 11, '诺兰电影深度解析', '盗梦空间、星际穿越、敦刻尔克...诺兰电影的叙事技巧和视觉语言深度分析。', '["https://picsum.photos/800/600?random=56"]', '["电影", "导演", "分析"]', '洛杉矶', 0.323450, 1789, 134, 678, 14000, 356, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(57, 11, '冷门佳片推荐', '不想看爆米花大片？推荐几部被低估的冷门佳片，保证看完回味无穷。', '["https://picsum.photos/800/600?random=57"]', '["电影", "推荐", "冷门"]', '东京', 0.287650, 1123, 89, 412, 8900, 223, 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),

-- 阅读类 (3条)
(58, 8, '2024年阅读书单', '今年已经读了47本书，精选十本推荐给大家，类型涵盖小说、历史、心理学。', '["https://picsum.photos/800/600?random=58"]', '["阅读", "书单", "推荐"]', '北京', 0.312340, 1567, 123, 578, 12000, 312, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(59, 8, '如何养成阅读习惯', '以前一年读不完一本书，现在每月至少四本。我是是如何培养阅读习惯的。', '["https://picsum.photos/800/600?random=59"]', '["阅读", "习惯", "方法"]', '上海', 0.276540, 1234, 98, 456, 9800, 245, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(60, 8, '历史类书籍推荐', '对历史感兴趣但不知道读什么？推荐几本通俗易懂又内容扎实的历史入门书。', '["https://picsum.photos/800/600?random=60"]', '["阅读", "历史", "书单"]', '西安', 0.298760, 987, 78, 367, 7800, 189, 1, DATE_SUB(NOW(), INTERVAL 3 DAY));

-- 继续插入更多笔记 (61-100)
INSERT INTO note (id, user_id, title, content, tags, like_count, comment_count, favorite_count, view_count, forward_count, status, created_at) 
SELECT 
  60 + n,
  -- 博主ID分布：大博主(2-21)多发，中博主(22-40)适量，小博主(41-60)少发
  CASE 
    WHEN n <= 15 THEN FLOOR(RAND() * 20) + 2
    WHEN n <= 25 THEN FLOOR(RAND() * 20) + 22
    ELSE FLOOR(RAND() * 20) + 41
  END,
  CONCAT('这是测试笔记标题第', 60 + n, '条'),
  CONCAT('这是测试笔记内容第', 60 + n, '条，包含丰富的信息和话题。'),
  CONCAT('["测试", "内容", "笔记"]'),
  FLOOR(RAND() * 500) + 50,
  FLOOR(RAND() * 50) + 5,
  FLOOR(RAND() * 200) + 20,
  FLOOR(RAND() * 5000) + 500,
  FLOOR(RAND() * 100) + 10,
  1,
  DATE_SUB(NOW(), INTERVAL (60 + n) DAY)
FROM (
  SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
  UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
  UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15
  UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20
  UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25
  UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29 UNION SELECT 30
  UNION SELECT 31 UNION SELECT 32 UNION SELECT 33 UNION SELECT 34 UNION SELECT 35
  UNION SELECT 36 UNION SELECT 37 UNION SELECT 38 UNION SELECT 39 UNION SELECT 40
) AS numbers;

-- 更新笔记的 stable_random 字段
UPDATE note SET stable_random = id * 0.000001 + RAND() * 0.1 WHERE stable_random IS NULL;

-- 更新笔记的热度分数 (hot_score = like*1 + comment*2 + favorite*3 + forward*5)
UPDATE note SET hot_score = like_count * 1 + comment_count * 2 + favorite_count * 3 + forward_count * 5 WHERE hot_score IS NULL;

COMMIT;

-- ============================================
-- 第四部分：生成关注关系
-- ============================================

-- 普通用户关注博主 (用户302-100301 随机关注2-5个博主)
INSERT INTO follow (follower_id, following_id, created_at)
SELECT 
  302 + uid_offset,
  -- 幂律分布：60%关注大博主(2-21)，30%关注中博主(22-40)，10%关注小博主(41-60)
  CASE 
    WHEN RAND() < 0.6 THEN FLOOR(RAND() * 20) + 2
    WHEN RAND() < 0.9 THEN FLOOR(RAND() * 20) + 22
    ELSE FLOOR(RAND() * 20) + 42
  END,
  DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY)
FROM (
  SELECT 1 uid_offset UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
) AS offsets,
(
  SELECT 0 n UNION ALL SELECT 1000 UNION ALL SELECT 2000 UNION ALL SELECT 3000 UNION ALL SELECT 4000
  UNION ALL SELECT 5000 UNION ALL SELECT 6000 UNION ALL SELECT 7000 UNION ALL SELECT 8000 UNION ALL SELECT 9000
  UNION ALL SELECT 10000 UNION ALL SELECT 11000 UNION ALL SELECT 12000 UNION ALL SELECT 13000 UNION ALL SELECT 14000
  UNION ALL SELECT 15000 UNION ALL SELECT 16000 UNION ALL SELECT 17000 UNION ALL SELECT 18000 UNION ALL SELECT 19000
  UNION ALL SELECT 20000 UNION ALL SELECT 21000 UNION ALL SELECT 22000 UNION ALL SELECT 23000 UNION ALL SELECT 24000
  UNION ALL SELECT 25000 UNION ALL SELECT 26000 UNION ALL SELECT 27000 UNION ALL SELECT 28000 UNION ALL SELECT 29000
  UNION ALL SELECT 30000 UNION ALL SELECT 31000 UNION ALL SELECT 32000 UNION ALL SELECT 33000 UNION ALL SELECT 34000
  UNION ALL SELECT 35000 UNION ALL SELECT 36000 UNION ALL SELECT 37000 UNION ALL SELECT 38000 UNION ALL SELECT 39000
  UNION ALL SELECT 40000 UNION ALL SELECT 41000 UNION ALL SELECT 42000 UNION ALL SELECT 43000 UNION ALL SELECT 44000
  UNION ALL SELECT 45000 UNION ALL SELECT 46000 UNION ALL SELECT 47000 UNION ALL SELECT 48000 UNION ALL SELECT 49000
  UNION ALL SELECT 50000 UNION ALL SELECT 51000 UNION ALL SELECT 52000 UNION ALL SELECT 53000 UNION ALL SELECT 54000
  UNION ALL SELECT 55000 UNION ALL SELECT 56000 UNION ALL SELECT 57000 UNION ALL SELECT 58000 UNION ALL SELECT 59000
  UNION ALL SELECT 60000 UNION ALL SELECT 61000 UNION ALL SELECT 62000 UNION ALL SELECT 63000 UNION ALL SELECT 64000
  UNION ALL SELECT 65000 UNION ALL SELECT 66000 UNION ALL SELECT 67000 UNION ALL SELECT 68000 UNION ALL SELECT 69000
  UNION ALL SELECT 70000 UNION ALL SELECT 71000 UNION ALL SELECT 72000 UNION ALL SELECT 73000 UNION ALL SELECT 74000
  UNION ALL SELECT 75000 UNION ALL SELECT 76000 UNION ALL SELECT 77000 UNION ALL SELECT 78000 UNION ALL SELECT 79000
  UNION ALL SELECT 80000 UNION ALL SELECT 81000 UNION ALL SELECT 82000 UNION ALL SELECT 83000 UNION ALL SELECT 84000
  UNION ALL SELECT 85000 UNION ALL SELECT 86000 UNION ALL SELECT 87000 UNION ALL SELECT 88000 UNION ALL SELECT 89000
  UNION ALL SELECT 90000 UNION ALL SELECT 91000 UNION ALL SELECT 92000 UNION ALL SELECT 93000 UNION ALL SELECT 94000
  UNION ALL SELECT 95000 UNION ALL SELECT 96000 UNION ALL SELECT 97000 UNION ALL SELECT 98000 UNION ALL SELECT 99000
) AS big_offsets
WHERE 302 + uid_offset + big_offsets.n <= 100301;

-- 博主之间互相关注 (增加社区活跃度)
INSERT INTO follow (follower_id, following_id, created_at)
SELECT a.follower_id, b.following_id, DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 10) DAY)
FROM follow a, follow b
WHERE a.following_id = b.follower_id 
  AND a.follower_id < b.following_id
  AND a.follower_id <= 60
  AND b.following_id <= 60
  AND RAND() < 0.3;

COMMIT;

-- ============================================
-- 第五部分：插入评论 (500条)
-- ============================================

-- 插入根评论
INSERT INTO note_comment (note_id, user_id, parent_id, root_id, content, like_count, reply_count, hot_score, status, created_at)
SELECT 
  FLOOR(RAND() * 60) + 1,
  FLOOR(RAND() * 60) + 302,  -- 普通用户
  0, 0,
  CONCAT('这条笔记写得真好！受益匪浅，学到了很多。'),
  FLOOR(RAND() * 50) + 5,
  0,
  0,
  1,
  DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 7) DAY)
FROM (
  SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
  UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
) AS tens,
(
  SELECT 0 n UNION ALL SELECT 10 UNION ALL SELECT 20 UNION ALL SELECT 30 UNION ALL SELECT 40
  UNION ALL SELECT 50 UNION ALL SELECT 60 UNION ALL SELECT 70 UNION ALL SELECT 80 UNION ALL SELECT 90
  UNION ALL SELECT 100 UNION ALL SELECT 110 UNION ALL SELECT 120 UNION ALL SELECT 130 UNION ALL SELECT 140
  UNION ALL SELECT 150 UNION ALL SELECT 160 UNION ALL SELECT 170 UNION ALL SELECT 180 UNION ALL SELECT 190
  UNION ALL SELECT 200 UNION ALL SELECT 210 UNION ALL SELECT 220 UNION ALL SELECT 230 UNION ALL SELECT 240
  UNION ALL SELECT 250 UNION ALL SELECT 260 UNION ALL SELECT 270 UNION ALL SELECT 280 UNION ALL SELECT 290
  UNION ALL SELECT 300 UNION ALL SELECT 310 UNION ALL SELECT 320 UNION ALL SELECT 330 UNION ALL SELECT 340
  UNION ALL SELECT 350 UNION ALL SELECT 360 UNION ALL SELECT 370 UNION ALL SELECT 380 UNION ALL SELECT 390
  UNION ALL SELECT 400 UNION ALL SELECT 410 UNION ALL SELECT 420 UNION ALL SELECT 430 UNION ALL SELECT 440
  UNION ALL SELECT 450 UNION ALL SELECT 460 UNION ALL SELECT 470 UNION ALL SELECT 480 UNION ALL SELECT 490
) AS numbers;

-- 更新根评论的 hot_score
UPDATE note_comment SET hot_score = like_count * 2 + reply_count * 3 WHERE parent_id = 0;

-- 插入子评论 (回复)
INSERT INTO note_comment (note_id, user_id, parent_id, root_id, content, like_count, reply_count, hot_score, status, created_at)
SELECT 
  nc.note_id,
  FLOOR(RAND() * 60) + 302,
  nc.id,
  nc.id,
  CONCAT('回复: ', nc.content),
  FLOOR(RAND() * 10) + 1,
  0,
  0,
  1,
  DATE_SUB(nc.created_at, INTERVAL 1 DAY)
FROM note_comment nc
WHERE nc.parent_id = 0
  AND nc.id <= 100
  AND RAND() < 0.4;

-- 插入二级回复
INSERT INTO note_comment (note_id, user_id, parent_id, root_id, content, like_count, reply_count, hot_score, status, created_at)
SELECT 
  nc.note_id,
  FLOOR(RAND() * 60) + 302,
  nc.id,
  nc.root_id,
  '同意你的观点，说得很对！',
  FLOOR(RAND() * 5) + 1,
  0,
  0,
  1,
  DATE_SUB(nc.created_at, INTERVAL 12 HOUR)
FROM note_comment nc
WHERE nc.parent_id != 0
  AND nc.root_id != nc.id
  AND nc.id <= 50
  AND RAND() < 0.3;

COMMIT;

-- ============================================
-- 第六部分：生成点赞关系
-- ============================================

-- 笔记点赞 (note_like)
INSERT INTO note_like (note_id, user_id, created_at)
SELECT DISTINCT
  FLOOR(RAND() * 60) + 1,
  302 + FLOOR(RAND() * 100000),
  DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY)
FROM (
  SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
) AS a,
(
  SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
) AS b,
(
  SELECT 0 n UNION ALL SELECT 100 UNION ALL SELECT 200 UNION ALL SELECT 300 UNION ALL SELECT 400 UNION ALL SELECT 500 UNION ALL SELECT 600 UNION ALL SELECT 700 UNION ALL SELECT 800 UNION ALL SELECT 900
) AS c
WHERE RAND() < 0.3
LIMIT 50000;

-- 收藏 (note_favorite)
INSERT INTO note_favorite (note_id, user_id, created_at)
SELECT DISTINCT
  FLOOR(RAND() * 60) + 1,
  302 + FLOOR(RAND() * 100000),
  DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY)
FROM (
  SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
) AS a,
(
  SELECT 0 n UNION ALL SELECT 100 UNION ALL SELECT 200 UNION ALL SELECT 300 UNION ALL SELECT 400 UNION ALL SELECT 500 UNION ALL SELECT 600 UNION ALL SELECT 700 UNION ALL SELECT 800 UNION ALL SELECT 900
) AS c
WHERE RAND() < 0.1
LIMIT 10000;

-- 评论点赞 (comment_like)
INSERT INTO comment_like (comment_id, user_id, created_at)
SELECT DISTINCT
  FLOOR(RAND() * 100) + 1,
  302 + FLOOR(RAND() * 100000),
  DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY)
FROM (
  SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
) AS a,
(
  SELECT 0 n UNION ALL SELECT 50 UNION ALL SELECT 100 UNION ALL SELECT 150 UNION ALL SELECT 200 UNION ALL SELECT 250 UNION ALL SELECT 300 UNION ALL SELECT 350 UNION ALL SELECT 400 UNION ALL SELECT 450
) AS c
WHERE RAND() < 0.5
LIMIT 5000;

COMMIT;

-- ============================================
-- 第七部分：生成通知
-- ============================================

-- 点赞通知 (type=1)
INSERT INTO notification (user_id, type, from_user_id, note_id, content, is_read, created_at)
SELECT 
  302 + FLOOR(RAND() * 100000),
  1,
  FLOOR(RAND() * 20) + 2,
  FLOOR(RAND() * 60) + 1,
  '赞了你的笔记',
  0,
  DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 7) DAY)
FROM (
  SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
) AS a,
(
  SELECT 0 n UNION ALL SELECT 20 UNION ALL SELECT 40 UNION ALL SELECT 60 UNION ALL SELECT 80 UNION ALL SELECT 100 UNION ALL SELECT 120 UNION ALL SELECT 140 UNION ALL SELECT 160 UNION ALL SELECT 180
) AS b
LIMIT 200;

-- 评论通知 (type=2)
INSERT INTO notification (user_id, type, from_user_id, note_id, comment_id, content, is_read, created_at)
SELECT 
  302 + FLOOR(RAND() * 100000),
  2,
  FLOOR(RAND() * 20) + 2,
  FLOOR(RAND() * 60) + 1,
  FLOOR(RAND() * 100) + 1,
  '评论了你的笔记',
  0,
  DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 7) DAY)
FROM (
  SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
) AS a,
(
  SELECT 0 n UNION ALL SELECT 20 UNION ALL SELECT 40 UNION ALL SELECT 60 UNION ALL SELECT 80 UNION ALL SELECT 100 UNION ALL SELECT 120 UNION ALL SELECT 140 UNION ALL SELECT 160 UNION ALL SELECT 180
) AS b
LIMIT 150;

-- 关注通知 (type=3)
INSERT INTO notification (user_id, type, from_user_id, content, is_read, created_at)
SELECT 
  FLOOR(RAND() * 20) + 2,
  3,
  302 + FLOOR(RAND() * 100000),
  '关注了你',
  0,
  DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 7) DAY)
FROM (
  SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
) AS a,
(
  SELECT 0 n UNION ALL SELECT 20 UNION ALL SELECT 40 UNION ALL SELECT 60 UNION ALL SELECT 80 UNION ALL SELECT 100 UNION ALL SELECT 120 UNION ALL SELECT 140 UNION ALL SELECT 160 UNION ALL SELECT 180
) AS b
LIMIT 100;

-- 系统通知 (type=4)
INSERT INTO notification (user_id, type, content, is_read, created_at)
SELECT 
  302 + FLOOR(RAND() * 100000),
  4,
  '恭喜你的笔记被推荐到首页！',
  0,
  DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 7) DAY)
FROM (
  SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
) AS a,
(
  SELECT 0 n UNION ALL SELECT 20 UNION ALL SELECT 40 UNION ALL SELECT 60 UNION ALL SELECT 80 UNION ALL SELECT 100
) AS b
LIMIT 50;

COMMIT;

-- ============================================
-- 第八部分：更新统计字段
-- ============================================

-- 更新 note 表的评论数
UPDATE note n SET comment_count = (
  SELECT COUNT(*) FROM note_comment c WHERE c.note_id = n.id AND c.status = 1
);

-- 更新 note_comment 表的回复数
UPDATE note_comment nc SET reply_count = (
  SELECT COUNT(*) - 1 FROM note_comment c WHERE c.root_id = nc.id
) WHERE nc.parent_id = 0;

-- 更新 note_comment 的热度分数
UPDATE note_comment SET hot_score = like_count * 2 + reply_count * 3 WHERE hot_score = 0;

COMMIT;

-- ============================================
-- 重新开启检查
-- ============================================
SET FOREIGN_KEY_CHECKS = 1;
SET UNIQUE_CHECKS = 1;

-- ============================================
-- 数据验证查询
-- ============================================
SELECT '用户总数' AS metric, COUNT(*) AS value FROM user
UNION ALL
SELECT '笔记总数', COUNT(*) FROM note WHERE status = 1
UNION ALL
SELECT '关注总数', COUNT(*) FROM follow
UNION ALL
SELECT '评论总数', COUNT(*) FROM note_comment
UNION ALL
SELECT '笔记点赞总数', COUNT(*) FROM note_like
UNION ALL
SELECT '笔记收藏总数', COUNT(*) FROM note_favorite
UNION ALL
SELECT '评论点赞总数', COUNT(*) FROM comment_like
UNION ALL
SELECT '通知总数', COUNT(*) FROM notification;
