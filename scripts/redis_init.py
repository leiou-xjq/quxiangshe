#!/usr/bin/env python3
"""
Redis初始化脚本
用于将数据库中的数据同步到Redis缓存
"""
import redis
import mysql.connector
from datetime import datetime

# Redis连接
r = redis.Redis(host='localhost', port=6379, db=0, decode_responses=True)

# MySQL连接
db = mysql.connector.connect(
    host='localhost',
    user='root',
    password='123456',
    database='quxiangshe'
)
cursor = db.cursor()

print("=" * 50)
print("开始初始化Redis缓存")
print("=" * 50)

# 1. 清理旧数据
print("\n[1] 清理Redis旧数据...")
r.flushdb()
print("    Redis已清理")

# 2. 初始化笔记计数
print("\n[2] 初始化笔记计数...")
cursor.execute("SELECT id, like_count, favorite_count, view_count, comment_count, forward_count FROM note WHERE status = 1")
pipe = r.pipeline()
count = 0
for (note_id, like_count, favorite_count, view_count, comment_count, forward_count) in cursor:
    pipe.set(f"note:like:count:{note_id}", like_count or 0)
    pipe.set(f"note:favorite:count:{note_id}", favorite_count or 0)
    pipe.set(f"note:view:count:{note_id}", view_count or 0)
    pipe.set(f"note:comment:count:{note_id}", comment_count or 0)
    pipe.set(f"note:forward:count:{note_id}", forward_count or 0)
    count += 1
    if count % 100 == 0:
        pipe.execute()
        print(f"    已处理 {count} 条笔记...")
pipe.execute()
print(f"    完成，共 {count} 条笔记计数")

# 3. 初始化热门笔记排行
print("\n[3] 初始化热门笔记排行...")
cursor.execute("SELECT id, hot_score FROM note WHERE status = 1 ORDER BY hot_score DESC LIMIT 1000")
pipe = r.pipeline()
count = 0
for (note_id, hot_score) in cursor:
    pipe.zadd("note:hot", {str(note_id): float(hot_score or 0)})
    count += 1
pipe.execute()
print(f"    完成，共 {count} 条热门笔记")

# 4. 初始化粉丝数缓存
print("\n[4] 初始化粉丝数缓存...")
cursor.execute("SELECT following_id, COUNT(*) as cnt FROM follow GROUP BY following_id")
pipe = r.pipeline()
count = 0
for (user_id, cnt) in cursor:
    pipe.set(f"feed:follower:count:{user_id}", int(cnt))
    count += 1
pipe.execute()
print(f"    完成，共 {count} 个用户的粉丝数")

# 5. 初始化关注数缓存
print("\n[5] 初始化关注数缓存...")
cursor.execute("SELECT follower_id, COUNT(*) as cnt FROM follow GROUP BY follower_id")
pipe = r.pipeline()
count = 0
for (user_id, cnt) in cursor:
    pipe.set(f"feed:following:count:{user_id}", int(cnt))
    count += 1
pipe.execute()
print(f"    完成，共 {count} 个用户的关注数")

# 6. 初始化Feed发件箱（拉模式作者笔记）
print("\n[6] 初始化Feed发件箱...")
cursor.execute("SELECT id, user_id, UNIX_TIMESTAMP(created_at) as ts FROM note WHERE status = 1")
pipe = r.pipeline()
count = 0
author_notes = {}
for (note_id, author_id, ts) in cursor:
    if author_id not in author_notes:
        author_notes[author_id] = []
    score = (int(ts) if ts else int(datetime.now().timestamp())) * 1024 + (note_id % 1024)
    pipe.zadd(f"feed:outbox:pull:{author_id}", {str(note_id): score})
    count += 1
pipe.execute()
print(f"    完成，共 {count} 条笔记")

# 7. 初始化用户Feed收件箱（根据关注关系）
print("\n[7] 初始化用户Feed收件箱...")
cursor.execute("""
    SELECT f.follower_id, f.following_id, n.id, n.user_id,
           UNIX_TIMESTAMP(n.created_at) as ts
    FROM follow f
    JOIN note n ON f.following_id = n.user_id AND n.status = 1
""")
pipe = r.pipeline()
count = 0
for (follower_id, following_id, note_id, author_id, ts) in cursor:
    score = (int(ts) if ts else int(datetime.now().timestamp())) * 1024 + (note_id % 1024)
    pipe.zadd(f"feed:inbox:push:{follower_id}", {str(note_id): score})
    count += 1
    if count % 1000 == 0:
        pipe.execute()
        print(f"    已处理 {count} 条收件箱记录...")
pipe.execute()
print(f"    完成，共 {count} 条收件箱记录")

# 8. 初始化笔记点赞关系
print("\n[8] 初始化笔记点赞关系...")
cursor.execute("SELECT note_id, user_id FROM note_like")
pipe = r.pipeline()
count = 0
for (note_id, user_id) in cursor:
    pipe.sadd(f"note:liked:{note_id}", str(user_id))
    pipe.sadd(f"user:liked:{user_id}", str(note_id))
    count += 1
    if count % 1000 == 0:
        pipe.execute()
pipe.execute()
print(f"    完成，共 {count} 条点赞关系")

# 9. 初始化笔记收藏关系
print("\n[9] 初始化笔记收藏关系...")
cursor.execute("SELECT note_id, user_id FROM note_favorite")
pipe = r.pipeline()
count = 0
for (note_id, user_id) in cursor:
    pipe.sadd(f"note:favorited:{note_id}", str(user_id))
    pipe.sadd(f"user:favorited:{user_id}", str(note_id))
    count += 1
pipe.execute()
print(f"    完成，共 {count} 条收藏关系")

# 10. 初始化根评论排序
print("\n[10] 初始化根评论排序...")
cursor.execute("SELECT note_id, id, hot_score FROM note_comment WHERE parent_id = 0")
pipe = r.pipeline()
count = 0
for (note_id, comment_id, hot_score) in cursor:
    pipe.zadd(f"post:{note_id}:root_comments", {str(comment_id): float(hot_score or 0)})
    count += 1
pipe.execute()
print(f"    完成，共 {count} 条根评论")

# 11. 初始化通知未读数
print("\n[11] 初始化通知未读数...")
cursor.execute("SELECT user_id, COUNT(*) FROM notification WHERE is_read = 0 GROUP BY user_id")
pipe = r.pipeline()
count = 0
for (user_id, cnt) in cursor:
    pipe.set(f"notification:unread:{user_id}", int(cnt))
    count += 1
pipe.execute()
print(f"    完成，共 {count} 个用户的未读通知数")

cursor.close()
db.close()

print("\n" + "=" * 50)
print("Redis初始化完成！")
print("=" * 50)