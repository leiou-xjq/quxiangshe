-- 关注表索引优化脚本
-- 执行方式: mysql -u root -p quxiangshe < follow_index.sql

-- 添加索引：查询某用户的所有粉丝
CREATE INDEX idx_follow_following_id ON follow(following_id);

-- 添加索引：查询某用户的所有关注
CREATE INDEX idx_follow_follower_id ON follow(follower_id);

-- 复合索引：用于检查重复关注
CREATE UNIQUE INDEX idx_follow_unique ON follow(follower_id, following_id);