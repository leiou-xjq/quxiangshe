-- =====================================================
-- 极速评论测试数据生成SQL
-- =====================================================

DELETE FROM note_comment;

SET @note1 = (SELECT id FROM note ORDER BY RAND() LIMIT 1);
SET @note2 = (SELECT id FROM note WHERE id != @note1 ORDER BY RAND() LIMIT 1);
SET @note3 = (SELECT id FROM note WHERE id != @note1 AND id != @note2 ORDER BY RAND() LIMIT 1);

SELECT CONCAT('笔记: ', @note1, ', ', @note2, ', ', @note3) AS info;

-- =====================================================
-- 笔记1: 1000条评论
-- =====================================================
INSERT INTO note_comment (note_id, user_id, parent_id, root_id, content, like_count, status, created_at, updated_at)
SELECT @note1, 100 + n, 0, 0, CONCAT('根评论_1_', n), FLOOR(RAND() * 1000), 1, NOW(), NOW()
FROM (SELECT @r1:=@r1+1 n FROM (SELECT 1) x, (SELECT @r1:=0) y, mysql.help_topic LIMIT 300) t;

SET @root1 = (SELECT GROUP_CONCAT(id) FROM note_comment WHERE note_id = @note1);

INSERT INTO note_comment (note_id, user_id, parent_id, root_id, content, like_count, status, created_at, updated_at)
SELECT @note1, 100 + FLOOR(RAND() * 200000), 
       CAST(ELT(1 + FLOOR(RAND() * 300), @root1) AS SIGNED),
       CAST(ELT(1 + FLOOR(RAND() * 300), @root1) AS SIGNED),
       CONCAT('子评论_1_', n), FLOOR(RAND() * 500), 1, NOW(), NOW()
FROM (SELECT @r2:=@r2+1 n FROM (SELECT 1) x, (SELECT @r2:=0) y, mysql.help_topic LIMIT 350) t;

SET @child1 = (SELECT GROUP_CONCAT(id) FROM note_comment WHERE note_id = @note1 AND parent_id != 0);

INSERT INTO note_comment (note_id, user_id, parent_id, root_id, content, like_count, status, created_at, updated_at)
SELECT @note1, 100 + FLOOR(RAND() * 200000), 
       CAST(ELT(1 + FLOOR(RAND() * 350), @child1) AS SIGNED),
       CAST(ELT(1 + FLOOR(RAND() * 300), @root1) AS SIGNED),
       CONCAT('孙评论_1_', n), FLOOR(RAND() * 300), 1, NOW(), NOW()
FROM (SELECT @r3:=@r3+1 n FROM (SELECT 1) x, (SELECT @r3:=0) y, mysql.help_topic LIMIT 350) t;

UPDATE note SET comment_count = 1000 WHERE id = @note1;
SELECT '笔记1完成' AS status;

-- =====================================================
-- 笔记2: 10000条评论
-- =====================================================
INSERT INTO note_comment (note_id, user_id, parent_id, root_id, content, like_count, status, created_at, updated_at)
SELECT @note2, 100 + n, 0, 0, CONCAT('根评论_2_', n), FLOOR(RAND() * 1000), 1, NOW(), NOW()
FROM (SELECT @r4:=@r4+1 n FROM (SELECT 1) x, (SELECT @r4:=0) y, mysql.help_topic a, mysql.help_topic b LIMIT 3000) t;

SET @root2 = (SELECT GROUP_CONCAT(id) FROM note_comment WHERE note_id = @note2);

INSERT INTO note_comment (note_id, user_id, parent_id, root_id, content, like_count, status, created_at, updated_at)
SELECT @note2, 100 + FLOOR(RAND() * 200000), 
       CAST(ELT(1 + FLOOR(RAND() * 3000), @root2) AS SIGNED),
       CAST(ELT(1 + FLOOR(RAND() * 3000), @root2) AS SIGNED),
       CONCAT('子评论_2_', n), FLOOR(RAND() * 500), 1, NOW(), NOW()
FROM (SELECT @r5:=@r5+1 n FROM (SELECT 1) x, (SELECT @r5:=0) y, mysql.help_topic a, mysql.help_topic b LIMIT 3500) t;

SET @child2 = (SELECT GROUP_CONCAT(id) FROM note_comment WHERE note_id = @note2 AND parent_id != 0);

INSERT INTO note_comment (note_id, user_id, parent_id, root_id, content, like_count, status, created_at, updated_at)
SELECT @note2, 100 + FLOOR(RAND() * 200000), 
       CAST(ELT(1 + FLOOR(RAND() * 3500), @child2) AS SIGNED),
       CAST(ELT(1 + FLOOR(RAND() * 3000), @root2) AS SIGNED),
       CONCAT('孙评论_2_', n), FLOOR(RAND() * 300), 1, NOW(), NOW()
FROM (SELECT @r6:=@r6+1 n FROM (SELECT 1) x, (SELECT @r6:=0) y, mysql.help_topic a, mysql.help_topic b LIMIT 3500) t;

UPDATE note SET comment_count = 10000 WHERE id = @note2;
SELECT '笔记2完成' AS status;

-- =====================================================
-- 笔记3: 100000条评论
-- =====================================================
INSERT INTO note_comment (note_id, user_id, parent_id, root_id, content, like_count, status, created_at, updated_at)
SELECT @note3, 100 + n, 0, 0, CONCAT('根评论_3_', n), FLOOR(RAND() * 1000), 1, NOW(), NOW()
FROM (SELECT @r7:=@r7+1 n FROM (SELECT 1) x, (SELECT @r7:=0) y, mysql.help_topic a, mysql.help_topic b, mysql.help_topic c LIMIT 30000) t;

SET @root3 = (SELECT GROUP_CONCAT(id) FROM note_comment WHERE note_id = @note3);

INSERT INTO note_comment (note_id, user_id, parent_id, root_id, content, like_count, status, created_at, updated_at)
SELECT @note3, 100 + FLOOR(RAND() * 200000), 
       CAST(ELT(1 + FLOOR(RAND() * 30000), @root3) AS SIGNED),
       CAST(ELT(1 + FLOOR(RAND() * 30000), @root3) AS SIGNED),
       CONCAT('子评论_3_', n), FLOOR(RAND() * 500), 1, NOW(), NOW()
FROM (SELECT @r8:=@r8+1 n FROM (SELECT 1) x, (SELECT @r8:=0) y, mysql.help_topic a, mysql.help_topic b, mysql.help_topic c LIMIT 35000) t;

SET @child3 = (SELECT GROUP_CONCAT(id) FROM note_comment WHERE note_id = @note3 AND parent_id != 0);

INSERT INTO note_comment (note_id, user_id, parent_id, root_id, content, like_count, status, created_at, updated_at)
SELECT @note3, 100 + FLOOR(RAND() * 200000), 
       CAST(ELT(1 + FLOOR(RAND() * 35000), @child3) AS SIGNED),
       CAST(ELT(1 + FLOOR(RAND() * 30000), @root3) AS SIGNED),
       CONCAT('孙评论_3_', n), FLOOR(RAND() * 300), 1, NOW(), NOW()
FROM (SELECT @r9:=@r9+1 n FROM (SELECT 1) x, (SELECT @r9:=0) y, mysql.help_topic a, mysql.help_topic b, mysql.help_topic c LIMIT 35000) t;

UPDATE note SET comment_count = 100000 WHERE id = @note3;
SELECT '笔记3完成' AS status;

SELECT note_id, COUNT(*) AS total, SUM(parent_id = 0) AS root, SUM(parent_id != 0) AS child
FROM note_comment GROUP BY note_id;