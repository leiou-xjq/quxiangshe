# SQL编码规范

## 文档信息

| 项目 | 内容 |
|-----|------|
| 版本 | v1.0.0 |
| 状态 | 已发布 |
| 更新日期 | 2024-01-01 |

## 1. 范围

本规范定义了SQL代码的编写标准，确保SQL代码的可读性和可维护性。

## 2. 关键字规范

### 2.1 大写规则

所有SQL关键字必须使用大写：

```sql
SELECT id, username, email FROM sys_user WHERE status = 1;

INSERT INTO sys_user (username, email) VALUES ('admin', 'admin@example.com');

UPDATE sys_user SET status = 1 WHERE id = 1;

DELETE FROM sys_user WHERE id = 1;
```

## 3. 缩进规范

### 3.1 简单语句

单行书写，无需缩进：

```sql
SELECT id, username FROM sys_user WHERE status = 1;
```

### 3.2 复杂语句

多行书写，缩进2个空格：

```sql
SELECT 
    u.id,
    u.username,
    u.email,
    COUNT(o.id) AS order_count
FROM sys_user u
LEFT JOIN oms_order o ON u.id = o.user_id
WHERE u.status = 1
GROUP BY u.id
ORDER BY u.created_at DESC;
```

## 4. 别名规范

### 4.1 表别名

使用简短的别名，有意义：

```sql
-- 推荐
FROM sys_user u
FROM oms_order o
FROM product_category pc

-- 不推荐
FROM sys_user as user
FROM order_table as t1
```

### 4.2 字段别名

使用下划线或驼峰：

```sql
SELECT 
    username AS user_name,
    created_at AS create_time,
    total_amount AS totalAmount
FROM sys_user;
```

## 5. 查询规范

### 5.1 SELECT 使用

```sql
-- 必须明确指定列
SELECT id, username, email FROM sys_user;

-- 禁止使用 SELECT *
```

### 5.2 分页查询

```sql
-- 传统分页
SELECT * FROM orders LIMIT 20 OFFSET 0;

-- 基于主键的分页（推荐）
SELECT * FROM orders 
WHERE id > #{lastId}
ORDER BY id 
LIMIT 20;
```

### 5.3 JOIN 使用

```sql
-- 明确指定 JOIN 类型
SELECT 
    u.username,
    o.order_no
FROM sys_user u
INNER JOIN oms_order o ON u.id = o.user_id
WHERE o.status = 1;

-- 避免在 ON 条件中使用函数
-- 不推荐
JOIN table2 ON DATE(t1.created_at) = DATE(t2.created_at)
```

## 6. DDL 规范

### 6.1 表创建

```sql
CREATE TABLE module_entity (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    field_name VARCHAR(50) NOT NULL COMMENT '字段说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表说明';
```

### 6.2 索引创建

```sql
-- 索引命名规范
CREATE INDEX idx_table_column ON table(column);
CREATE UNIQUE INDEX uk_table_column ON table(column);
```

## 7. 性能规范

### 7.1 避免全表扫描

- WHERE 条件使用索引列
- 避免在 WHERE 条件中使用函数
- 避免 LIKE 开头使用通配符

### 7.2 优化分页

- 大数据量使用游标分页
- 避免 OFFSET 过大

### 7.3 批量操作

```sql
-- 批量插入
INSERT INTO table (a, b) VALUES 
(1, 2),
(3, 4),
(5, 6);

-- 避免循环单条插入
```

## 8. 格式化工具

推荐使用以下工具：

- SQLFluff
- pgFormatter
- DBeaver 格式化功能