# 数据库设计规范

## 概述

本文档定义了数据库设计的标准规范，适用于全栈开发项目中的数据库设计和开发工作。

## 命名规范

### 通用规则

- 使用英文命名，禁止使用中文或特殊字符
- 全部使用小写字母
- 使用下划线 `_` 分隔单词
- 避免使用保留字
- 名称应简洁明了，具有描述性

### 数据库命名

```sql
-- 项目数据库命名
database_name: {project_name}_db

-- 示例
CREATE DATABASE ecommerce_db;
```

### 表命名

```sql
-- 表命名格式：模块名_业务实体名
-- 单数形式，避免复数

-- 用户表
CREATE TABLE sys_user (...);

-- 订单表
CREATE TABLE oms_order (...);

-- 订单明细表
CREATE TABLE oms_order_item (...);
```

### 字段命名

```sql
-- 主键命名
id              -- 通用主键
{table_name}_id -- 业务主键

-- 外键命名
{related_table}_id

-- 布尔字段
is_xxx / has_xxx / can_xxx

-- 时间字段
created_at      -- 创建时间
updated_at      -- 更新时间
deleted_at      -- 删除时间（软删除）
```

## 设计原则

### 规范化

- 遵循第三范式（3NF）
- 避免数据冗余
- 确保数据完整性

### 索引设计

```sql
-- 主键索引（自动创建）
PRIMARY KEY (id)

-- 唯一索引
UNIQUE INDEX idx_{table}_{column} ({column})

-- 普通索引
INDEX idx_{table}_{column} ({column})

-- 复合索引
INDEX idx_{table}_{columns} ({col1}, {col2})
```

### 字段类型选择

| 数据类型 | 使用场景 |
|---------|---------|
| INT/BIGINT | 整数数值 |
| DECIMAL | 精确金额 |
| VARCHAR | 短文本 |
| TEXT | 长文本 |
| DATETIME | 日期时间 |
| TIMESTAMP | 时间戳 |
| JSON | 结构化数据 |

## 示例

```sql
CREATE TABLE sys_user (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username    VARCHAR(50) NOT NULL COMMENT '用户名',
    email       VARCHAR(100) NOT NULL COMMENT '邮箱',
    password    VARCHAR(255) NOT NULL COMMENT '密码',
    status      TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at  DATETIME NULL DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    UNIQUE INDEX idx_username (username),
    UNIQUE INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
```