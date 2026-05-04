# 数据库设计规范

## 文档信息

| 项目 | 内容 |
|-----|------|
| 版本 | v1.0.0 |
| 状态 | 已发布 |
| 更新日期 | 2024-01-01 |

## 1. 范围

本规范适用于全栈开发项目的数据库设计，包括数据库、表、字段、索引的设计标准。

## 2. 数据库命名

### 2.1 命名规则

- 使用英文命名，全部小写
- 使用下划线分隔
- 格式：`{project_name}_db`

### 2.2 示例

```sql
CREATE DATABASE ecommerce_db;
CREATE DATABASE admin_system_db;
```

## 3. 表设计规范

### 3.1 命名规范

- 格式：`{module}_{entity}`
- 使用单数形式
- 全部小写，下划线分隔

### 3.2 示例

```sql
sys_user          -- 系统用户
oms_order         -- 订单管理
pms_product       -- 商品管理
wms_warehouse     -- 仓库管理
```

### 3.3 必含字段

每个表必须包含以下字段：

```sql
id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT  -- 主键
created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP  -- 创建时间
updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP  -- 更新时间
deleted_at  DATETIME NULL DEFAULT NULL  -- 删除时间（软删除）
```

## 4. 字段设计规范

### 4.1 字段类型选择

| 数据类型 | 存储内容 | 示例 |
|---------|---------|------|
| TINYINT | 状态、布尔值 | status: 0/1 |
| INT | 普通整数 | 数量、页码 |
| BIGINT | 大整数、ID | 主键、用户ID |
| DECIMAL | 金额 | price: DECIMAL(10,2) |
| VARCHAR | 短文本 | 名称、邮箱 |
| TEXT | 长文本 | 描述、备注 |
| DATETIME | 日期时间 | created_at |
| JSON | 结构化数据 | extra_info |

### 4.2 字段命名

| 字段类型 | 命名示例 | 说明 |
|---------|---------|------|
| 主键 | id | 通用主键 |
| 外键 | {table}_id | 关联表_id |
| 布尔 | is_xxx, has_xxx | is_active, has_children |
| 时间 | xxx_at | created_at, login_at |
| 状态 | status | 状态字段 |

### 4.3 字段约束

```sql
-- 非空约束
username VARCHAR(50) NOT NULL

-- 默认值
status TINYINT NOT NULL DEFAULT 1

-- 注释
COMMENT '用户名'

-- 唯一约束
UNIQUE INDEX idx_username (username)
```

## 5. 索引设计规范

### 5.1 索引命名

| 类型 | 格式 | 示例 |
|-----|------|------|
| 主键 | pk_{table} | pk_sys_user |
| 唯一 | uk_{table}_{column} | uk_sys_user_email |
| 普通 | idx_{table}_{column} | idx_oms_order_user_id |

### 5.2 索引原则

- 主键自动创建唯一索引
- WHERE条件字段应建立索引
- 避免在索引列上使用函数
- 复合索引注意字段顺序
- 避免创建过多索引

## 6. 外键约束

### 6.1 命名格式

```sql
fk_{table}_{related_table}
-- 示例
fk_oms_order_sys_user
```

### 6.2 使用建议

- 生产环境建议使用外键约束保证完整性
- 大数据量导入时可暂时关闭外键检查
- 定期检查外键一致性

## 7. 表注释

```sql
-- 表注释
COMMENT '用户表';

-- 字段注释
COMMENT '用户ID';
COMMENT '用户名';
```

## 8. 示例

```sql
CREATE TABLE sys_user (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username    VARCHAR(50) NOT NULL COMMENT '用户名',
    email       VARCHAR(100) NOT NULL COMMENT '邮箱',
    password    VARCHAR(255) NOT NULL COMMENT '密码',
    real_name   VARCHAR(50) NULL COMMENT '真实姓名',
    phone       VARCHAR(20) NULL COMMENT '手机号',
    avatar      VARCHAR(255) NULL COMMENT '头像URL',
    status      TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at  DATETIME NULL DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    UNIQUE INDEX uk_username (username),
    UNIQUE INDEX uk_email (email),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
```