-- 生成20万普通用户
USE quxiangshe;
DROP PROCEDURE IF EXISTS gen_users;

DELIMITER $$
CREATE PROCEDURE gen_users()
BEGIN
  DECLARE i INT DEFAULT 302;
  DECLARE batch INT DEFAULT 0;
  SET autocommit = 0;
  SET unique_checks = 0;
  SET foreign_key_checks = 0;

  WHILE i <= 200301 DO
    INSERT INTO user (username, phone, password, avatar, nickname, gender, bio, status, role, created_at)
    VALUES (
      CONCAT('u', i),
      CONCAT('139', LPAD(i, 8, '0')),
      '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs8HQl4Bypx9naP7Kv9a',
      CONCAT('https://picsum.photos/400?random=', i),
      CONCAT('User', i),
      FLOOR(RAND() * 3),
      'Love life',
      1, 'USER', NOW()
    );

    SET i = i + 1;
    SET batch = batch + 1;

    IF batch >= 5000 THEN
      COMMIT;
      SET batch = 0;
    END IF;
  END WHILE;

  COMMIT;
  SET unique_checks = 1;
  SET foreign_key_checks = 1;
END$$
DELIMITER ;

CALL gen_users();
DROP PROCEDURE IF EXISTS gen_users;

SELECT 'Users generated: ' as msg, COUNT(*) as cnt FROM user;