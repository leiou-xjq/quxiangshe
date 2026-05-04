-- 生成50万关注关系
USE quxiangshe;
DROP PROCEDURE IF EXISTS gen_follows;

DELIMITER $$
CREATE PROCEDURE gen_follows()
BEGIN
  DECLARE i INT DEFAULT 302;
  DECLARE target INT DEFAULT 0;
  DECLARE cnt INT DEFAULT 0;
  DECLARE batch INT DEFAULT 0;
  SET autocommit = 0;
  SET unique_checks = 0;
  SET foreign_key_checks = 0;

  WHILE i <= 200301 DO
    SET cnt = FLOOR(RAND() * 4) + 2;

    WHILE cnt > 0 DO
      SET target = CASE
        WHEN RAND() < 0.6 THEN FLOOR(RAND() * 20) + 2
        WHEN RAND() < 0.9 THEN FLOOR(RAND() * 80) + 22
        ELSE FLOOR(RAND() * 200) + 102
      END;

      INSERT IGNORE INTO follow (follower_id, following_id, created_at)
      VALUES (i, target, DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY));

      SET cnt = cnt - 1;
      SET batch = batch + 1;

      IF batch >= 10000 THEN
        COMMIT;
        SET batch = 0;
      END IF;
    END WHILE;

    SET i = i + 1;

    IF i >= 10000 AND i % 10000 = 1 THEN
      SELECT CONCAT('Processed ', i, ' users...') as status;
    END IF;
  END WHILE;

  COMMIT;
  SET unique_checks = 1;
  SET foreign_key_checks = 1;
END$$
DELIMITER ;

CALL gen_follows();
DROP PROCEDURE IF EXISTS gen_follows;

SELECT 'Follows generated: ' as msg, COUNT(*) as cnt FROM follow;