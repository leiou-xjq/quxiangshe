TRUNCATE TABLE note;

INSERT INTO note (user_id, title, content) VALUES
(2, 'Test Note 1', 'Content 1'),
(3, 'Test Note 2', 'Content 2');

SELECT COUNT(*) as cnt FROM note;