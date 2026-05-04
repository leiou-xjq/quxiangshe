-- 优化数据库索引，提升查询性能
-- 执行方式: mysql -u root -p quxiangshe < V2__optimize_indexes.sql

-- 1. 笔记表索引优化
-- 状态+创建时间索引（Feed流查询）
CREATE INDEX idx_note_status_created ON note(status, created_at DESC);

-- 状态+热度索引（热门榜单）
CREATE INDEX idx_note_status_hot ON note(status, hot_score DESC);

-- 用户+状态索引（用户笔记列表）
CREATE INDEX idx_note_user_status ON note(user_id, status);

-- 稳定随机排序索引（发现精彩）
CREATE INDEX idx_note_stable_random ON note(stable_random DESC);

-- 2. 关注表索引优化
CREATE INDEX idx_follow_follower ON follow(follower_id);
CREATE INDEX idx_follow_following ON follow(following_id);

-- 3. 私信会话表索引
CREATE INDEX idx_private_message_session ON private_message(session_id, created_at DESC);

-- 4. 笔记点赞表索引
CREATE INDEX idx_note_like_note_user ON note_like(note_id, user_id);
CREATE INDEX idx_note_like_user ON note_like(user_id, created_at DESC);

-- 5. 笔记收藏表索引
CREATE INDEX idx_note_favorite_note_user ON note_favorite(note_id, user_id);
CREATE INDEX idx_note_favorite_user ON note_favorite(user_id, created_at DESC);

-- 6. 评论表索引
CREATE INDEX idx_note_comment_note ON note_comment(note_id, created_at DESC);
CREATE INDEX idx_note_comment_user ON note_comment(user_id, created_at DESC);