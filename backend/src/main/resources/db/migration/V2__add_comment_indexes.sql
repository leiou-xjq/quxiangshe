-- 评论区性能优化 - 添加索引和热度字段
-- 执行方式: 在 MySQL 客户端执行此 SQL

-- 为 note_comment 表添加索引
-- 索引1: note_id + status 联合索引 (查询笔记评论)
ALTER TABLE note_comment ADD INDEX idx_note_comment_note_status (note_id, status);

-- 索引2: root_id 索引 (查询子评论)
ALTER TABLE note_comment ADD INDEX idx_note_comment_root_id (root_id);

-- 索引3: note_id + created_at 索引 (按时间排序查询)
ALTER TABLE note_comment ADD INDEX idx_note_comment_note_created (note_id, created_at);

-- 索引4: note_id + root_id + created_at 索引 (查询根评论的子评论按时间排序)
ALTER TABLE note_comment ADD INDEX idx_note_comment_root_created (note_id, root_id, created_at);

-- 索引5: note_id + hot_score 索引 (按热度排序查询) - 最重要的索引！
ALTER TABLE note_comment ADD INDEX idx_note_comment_note_hot (note_id, hot_score DESC);

-- 3. 初始化现有评论的热度值
UPDATE note_comment SET hot_score = COALESCE(like_count, 0) * 2 + COALESCE(reply_count, 0) * 3 
WHERE status = 1 AND (parent_id IS NULL OR parent_id = 0);

-- 查看索引是否创建成功
SHOW INDEX FROM note_comment;