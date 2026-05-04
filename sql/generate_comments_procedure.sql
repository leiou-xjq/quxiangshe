-- =====================================================
-- 评论区性能测试数据 - 高效生成脚本
-- =====================================================

-- 1. 先查询笔记数量
SELECT COUNT(*) as note_count FROM note;

-- 2. 随机选择3篇笔记并记录
SET @note1_id = (SELECT id FROM note ORDER BY RAND() LIMIT 1);
SET @note2_id = (SELECT id FROM note WHERE id != @note1_id ORDER BY RAND() LIMIT 1);
SET @note3_id = (SELECT id FROM note WHERE id != @note1_id AND id != @note2_id ORDER BY RAND() LIMIT 1);

SELECT @note1_id AS note1, @note2_id AS note2, @note3_id AS note3;

-- 3. 生成1000条评论的存储过程
DELIMITER //
DROP PROCEDURE IF EXISTS generate_1000_comments//
CREATE PROCEDURE generate_1000_comments(IN note_id BIGINT)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE root_count INT;
    DECLARE root_ids TEXT DEFAULT '';
    DECLARE child_ids TEXT DEFAULT '';
    
    SET root_count = 300;
    
    -- 插入根评论
    WHILE i < root_count DO
        INSERT INTO note_comment (note_id, user_id, parent_id, root_id, content, like_count, status, created_at, updated_at)
        VALUES (
            note_id,
            7 + FLOOR(RAND() * 200000),
            0,
            0,
            CONCAT('评论内容', UNIX_TIMESTAMP() * 1000000 + i),
            FLOOR(RAND() * 1000),
            1,
            DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY),
            NOW()
        );
        SET root_ids = CONCAT(root_ids, LAST_INSERT_ID(), ',');
        SET i = i + 1;
    END WHILE;
    
    -- 插入子评论
    SET i = 0;
    WHILE i < 350 DO
        INSERT INTO note_comment (note_id, user_id, parent_id, root_id, content, like_count, status, created_at, updated_at)
        VALUES (
            note_id,
            7 + FLOOR(RAND() * 200000),
            SUBSTRING_INDEX(SUBSTRING_INDEX(root_ids, ',', 1 + FLOOR(RAND() * root_count)), ',', -1),
            SUBSTRING_INDEX(SUBSTRING_INDEX(root_ids, ',', 1 + FLOOR(RAND() * root_count)), ',', -1),
            CONCAT('回复内容', UNIX_TIMESTAMP() * 1000000 + i),
            FLOOR(RAND() * 500),
            1,
            DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 25) DAY),
            NOW()
        );
        SET child_ids = CONCAT(child_ids, LAST_INSERT_ID(), ',');
        SET i = i + 1;
    END WHILE;
    
    -- 插入孙评论
    SET i = 0;
    WHILE i < 350 DO
        INSERT INTO note_comment (note_id, user_id, parent_id, root_id, content, like_count, status, created_at, updated_at)
        VALUES (
            note_id,
            7 + FLOOR(RAND() * 200000),
            SUBSTRING_INDEX(SUBSTRING_INDEX(child_ids, ',', 1 + FLOOR(RAND() * 350)), ',', -1),
            SUBSTRING_INDEX(SUBSTRING_INDEX(root_ids, ',', 1 + FLOOR(RAND() * root_count)), ',', -1),
            CONCAT('追问内容', UNIX_TIMESTAMP() * 1000000 + i),
            FLOOR(RAND() * 300),
            1,
            DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 20) DAY),
            NOW()
        );
        SET i = i + 1;
    END WHILE;
    
    -- 更新笔记评论数
    UPDATE note SET comment_count = 1000 WHERE id = note_id;
END//
DELIMITER ;

-- 4. 调用存储过程生成1000条评论
-- CALL generate_1000_comments(@note1_id);

-- 5. 使用纯SQL批量插入（更高效）
-- 为10000条和100000条评论，直接使用批量INSERT
DELIMITER //
DROP PROCEDURE IF EXISTS generate_mass_comments//
CREATE PROCEDURE generate_mass_comments(IN p_note_id BIGINT, IN p_total_count INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE batch_size INT DEFAULT 1000;
    DECLARE root_count INT;
    DECLARE child_count INT;
    
    SET root_count = FLOOR(p_total_count * 0.30);
    SET child_count = FLOOR(p_total_count * 0.35);
    
    -- 批量插入根评论
    WHILE i < root_count DO
        INSERT INTO note_comment (note_id, user_id, parent_id, root_id, content, like_count, status, created_at, updated_at)
        VALUES (
            p_note_id,
            7 + FLOOR(RAND() * 200000),
            0, 0,
            CONCAT('评论内容', UNIX_TIMESTAMP() * 1000000 + i + p_total_count),
            FLOOR(RAND() * 1000),
            1,
            DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY),
            NOW()
        );
        SET i = i + 1;
        
        IF i % batch_size = 0 THEN
            COMMIT;
        END IF;
    END WHILE;
    
    COMMIT;
    
    -- 批量插入子评论 (假设已有根评论ID)
    SET i = 0;
    WHILE i < child_count DO
        INSERT INTO note_comment (note_id, user_id, parent_id, root_id, content, like_count, status, created_at, updated_at)
        SELECT 
            p_note_id,
            7 + FLOOR(RAND() * 200000),
            (SELECT id FROM note_comment WHERE note_id = p_note_id AND parent_id = 0 ORDER BY RAND() LIMIT 1),
            (SELECT id FROM note_comment WHERE note_id = p_note_id AND parent_id = 0 ORDER BY RAND() LIMIT 1),
            CONCAT('回复内容', UNIX_TIMESTAMP() * 1000000 + i + p_total_count),
            FLOOR(RAND() * 500),
            1,
            DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 25) DAY),
            NOW()
        FROM DUAL;
        SET i = i + 1;
        
        IF i % batch_size = 0 THEN
            COMMIT;
        END IF;
    END WHILE;
    
    COMMIT;
    
    -- 批量插入孙评论
    SET i = 0;
    WHILE i < (p_total_count - root_count - child_count) DO
        INSERT INTO note_comment (note_id, user_id, parent_id, root_id, content, like_count, status, created_at, updated_at)
        SELECT 
            p_note_id,
            7 + FLOOR(RAND() * 200000),
            (SELECT id FROM note_comment WHERE note_id = p_note_id AND parent_id != 0 ORDER BY RAND() LIMIT 1),
            (SELECT root_id FROM note_comment WHERE note_id = p_note_id AND parent_id != 0 ORDER BY RAND() LIMIT 1),
            CONCAT('追问内容', UNIX_TIMESTAMP() * 1000000 + i + p_total_count),
            FLOOR(RAND() * 300),
            1,
            DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 20) DAY),
            NOW()
        FROM DUAL;
        SET i = i + 1;
        
        IF i % batch_size = 0 THEN
            COMMIT;
        END IF;
    END WHILE;
    
    COMMIT;
    
    UPDATE note SET comment_count = p_total_count WHERE id = p_note_id;
END//
DELIMITER ;

-- 查看选中的笔记
SELECT id, title, comment_count FROM note WHERE id IN (@note1_id, @note2_id, @note3_id);