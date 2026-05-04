#!/usr/bin/env python3
"""Generate test data"""
import mysql.connector
import random
from datetime import datetime, timedelta

conn = mysql.connector.connect(
    host='localhost',
    user='root',
    password='123456',
    database='quxiangshe'
)
cursor = conn.cursor()

print("=" * 50)
print("Starting data generation...")

# Truncate tables
print("[1] Truncating tables...")
for table in ['notification', 'note_like', 'note_favorite', 'note_comment', 'follow', 'note']:
    try:
        cursor.execute(f"TRUNCATE TABLE {table}")
        print(f"  - {table} truncated")
    except Exception as e:
        print(f"  - {table}: {e}")
conn.commit()

# Generate notes
print("[2] Generating 100 notes...")
notes_data = []
for i in range(1, 101):
    user_id = random.randint(2, 301)
    title = f"Note Title {i}"
    content = f"Content for note {i} " * 5
    stable_random = random.random()
    like_count = random.randint(100, 5000)
    comment_count = random.randint(10, 200)
    favorite_count = random.randint(50, 1000)
    view_count = random.randint(1000, 30000)
    forward_count = random.randint(50, 500)
    hot_score = like_count * 1 + comment_count * 2 + favorite_count * 3 + forward_count * 5
    created_at = datetime.now() - timedelta(days=random.randint(1, 30))

    notes_data.append((
        user_id, title, content, '[]', '[]', stable_random,
        like_count, comment_count, favorite_count, view_count, forward_count, hot_score, 1, created_at
    ))

    cursor.execute("""
        INSERT INTO note (user_id, title, content, images, tags, stable_random, like_count, comment_count, favorite_count, view_count, forward_count, hot_score, status, created_at)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
    """, notes_data[-1])

conn.commit()
print(f"  Inserted {len(notes_data)} notes")

# Generate follows
print("[3] Generating 500K follows...")
cursor.execute("SELECT COUNT(*) FROM user")
user_count = cursor.fetchone()[0]
print(f"  Total users: {user_count}")

total = 0
for follower in range(302, min(200302, user_count + 1)):
    follows_num = random.randint(2, 5)
    for _ in range(follows_num):
        r = random.random()
        if r < 0.6:
            following_id = random.randint(2, 21)
        elif r < 0.9:
            following_id = random.randint(22, 101)
        else:
            following_id = random.randint(102, 301)
        try:
            cursor.execute("""
                INSERT IGNORE INTO follow (follower_id, following_id, created_at)
                VALUES (%s, %s, %s)
            """, (follower, following_id, datetime.now() - timedelta(days=random.randint(0, 30))))
            total += 1
        except:
            pass
    if total % 10000 == 0 and total > 0:
        conn.commit()
        print(f"  {total} follows inserted...")
conn.commit()
print(f"  Done: {total} follows")

# Generate likes
print("[4] Generating 200K likes...")
cursor.execute("SELECT id FROM note")
note_ids = [row[0] for row in cursor.fetchall()]
total = 0
for _ in range(200000):
    note_id = random.choice(note_ids)
    user_id = random.randint(302, min(200301, user_count))
    try:
        cursor.execute("""
            INSERT IGNORE INTO note_like (note_id, user_id, created_at)
            VALUES (%s, %s, %s)
        """, (note_id, user_id, datetime.now() - timedelta(days=random.randint(0, 30))))
        total += 1
        if total % 10000 == 0:
            conn.commit()
            print(f"  {total} likes...")
    except:
        pass
conn.commit()
print(f"  Done: {total} likes")

# Generate favorites
print("[5] Generating 50K favorites...")
total = 0
for _ in range(50000):
    note_id = random.choice(note_ids)
    user_id = random.randint(302, min(200301, user_count))
    try:
        cursor.execute("""
            INSERT IGNORE INTO note_favorite (note_id, user_id, created_at)
            VALUES (%s, %s, %s)
        """, (note_id, user_id, datetime.now() - timedelta(days=random.randint(0, 30))))
        total += 1
        if total % 10000 == 0:
            conn.commit()
            print(f"  {total} favorites...")
    except:
        pass
conn.commit()
print(f"  Done: {total} favorites")

# Generate comments
print("[6] Generating 1K comments...")
for _ in range(1000):
    note_id = random.choice(note_ids)
    user_id = random.randint(302, min(200301, user_count))
    cursor.execute("""
        INSERT INTO note_comment (note_id, user_id, parent_id, root_id, content, like_count, reply_count, hot_score, status, created_at)
        VALUES (%s, %s, 0, 0, %s, %s, 0, 0, 1, %s)
    """, (note_id, user_id, f'Comment {_}', random.randint(5, 50), datetime.now() - timedelta(days=random.randint(0, 7))))
conn.commit()
print("  Done: 1000 comments")

# Generate notifications
print("[7] Generating 2K notifications...")
for _ in range(2000):
    user_id = random.randint(302, min(200301, user_count))
    notif_type = random.choice([1, 2, 3, 4])
    from_user_id = random.randint(2, 301)
    note_id = random.choice(note_ids) if notif_type in [1, 2] else None
    cursor.execute("""
        INSERT INTO notification (user_id, type, from_user_id, note_id, content, is_read, created_at)
        VALUES (%s, %s, %s, %s, %s, 0, %s)
    """, (user_id, notif_type, from_user_id, note_id, f'Notification {_}', datetime.now() - timedelta(days=random.randint(0, 7))))
conn.commit()
print("  Done: 2000 notifications")

# Summary
print("\n" + "=" * 50)
print("SUMMARY:")
cursor.execute("""
    SELECT 'users' as t, COUNT(*) as c FROM user
    UNION ALL SELECT 'notes', COUNT(*) FROM note
    UNION ALL SELECT 'follows', COUNT(*) FROM follow
    UNION ALL SELECT 'likes', COUNT(*) FROM note_like
    UNION ALL SELECT 'favorites', COUNT(*) FROM note_favorite
    UNION ALL SELECT 'comments', COUNT(*) FROM note_comment
    UNION ALL SELECT 'notifications', COUNT(*) FROM notification
""")
for row in cursor.fetchall():
    print(f"  {row[0]}: {row[1]:,}")

cursor.close()
conn.close()
print("\nDone!")