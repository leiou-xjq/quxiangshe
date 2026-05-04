# SQL编码规范

## 概述

本文档定义了SQL编码的标准规范，确保SQL代码的可读性、可维护性和性能。

## 基本规范

### 关键词大写

```sql
-- 推荐
SELECT id, username, email FROM sys_user WHERE status = 1;

-- 不推荐
select id, username, email from sys_user where status = 1;
```

### 缩进和换行

```sql
-- 简单查询
SELECT id, username, email FROM sys_user WHERE status = 1;

-- 复杂查询
SELECT 
    u.id,
    u.username,
    u.email,
    o.order_count
FROM sys_user u
LEFT JOIN (
    SELECT user_id, COUNT(*) AS order_count
    FROM oms_order
    GROUP BY user_id
) o ON u.id = o.user_id
WHERE u.status = 1
ORDER BY u.created_at DESC;
```

### 别名使用

```sql
-- 表别名：简短有意义
SELECT 
    u.id,
    o.total_amount
FROM sys_user u
JOIN oms_order o ON u.id = o.user_id;

-- 字段别名：使用下划线或驼峰
SELECT 
    username AS user_name,
    created_at AS createTime
FROM sys_user;
```

## 查询规范

### SELECT 使用

```sql
-- 明确指定列，避免使用 SELECT *
SELECT id, username, email FROM sys_user;

-- 使用 LIMIT 分页
SELECT * FROM sys_user LIMIT 10 OFFSET 20;
```

### JOIN 使用

```sql
-- 使用明确的 JOIN 类型
SELECT 
    u.username,
    o.order_no
FROM sys_user u
INNER JOIN oms_order o ON u.id = o.user_id;

-- 避免在 JOIN 条件中使用函数
-- 不推荐
SELECT * FROM a JOIN b ON DATE(a.created_at) = DATE(b.created_at)

-- 推荐
SELECT * FROM a JOIN b ON a.created_at >= b.created_at 
                AND a.created_at < DATE_ADD(b.created_at, INTERVAL 1 DAY)
```

### WHERE 子句

```sql
-- 使用参数化查询
SELECT * FROM sys_user WHERE username = ?;

-- 避免在 WHERE 中使用函数
-- 不推荐
SELECT * FROM sys_user WHERE LOWER(username) = 'admin';

-- 推荐
SELECT * FROM sys_user WHERE username = LOWER('Admin');

-- 使用 IN 时控制数量
-- 不推荐
WHERE id IN (1, 2, 3, ..., 1000)

-- 推荐（分批处理或使用临时表）
```

## DDL 规范

### 表创建

```sql
-- 表名使用下划线命名
CREATE TABLE sys_user_login_log (...);

-- 字段名使用下划线命名
username VARCHAR(50) NOT NULL COMMENT '用户名';

-- 添加注释
COMMENT ON TABLE sys_user IS '用户表';
COMMENT ON COLUMN sys_user.username IS '用户名';
```

### 索引规范

```sql
-- 索引命名
-- 主键：pk_{table}
-- 唯一索引：uk_{table}_{column}
-- 普通索引：idx_{table}_{column}

CREATE INDEX idx_sys_user_username ON sys_user(username);
```

## 性能规范

### 避免全表扫描

```sql
-- 使用索引字段
WHERE index_column = value

-- 避免
WHERE function(column) = value
WHERE column LIKE '%xxx'
WHERE column IS NULL  -- 对于需要查询空值的字段，建立索引
```

### 分页查询

```sql
-- 传统分页（大数据量性能差）
SELECT * FROM orders LIMIT 100 OFFSET 10000;

-- 基于主键的分页（性能更好）
SELECT * FROM orders 
WHERE id > 10000 
ORDER BY id 
LIMIT 100;

-- 或使用游标分页
SELECT * FROM orders 
WHERE created_at < '2024-01-01' 
ORDER BY created_at DESC 
LIMIT 100;
```

## 格式化工具

推荐使用以下工具进行SQL格式化：

- SQLFluff（Python）
- pgFormatter
- SQLbolt
- DBeaver 内置格式化