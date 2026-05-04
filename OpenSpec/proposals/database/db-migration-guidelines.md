# 数据库迁移规范

## 概述

本文档定义了数据库版本管理和迁移的标准规范，确保数据库变更的可追溯性和安全性。

## 迁移文件命名

```bash
# 格式：{version}_{description}.sql
# 示例：
# 20240101000001_create_user_table.sql
# 20240101000002_add_user_phone_column.sql
# 20240101000003_create_user_index.sql
```

## 迁移目录结构

```
migrations/
├── 2024/
│   ├── 01/
│   │   ├── 20240101000001_create_user_table.sql
│   │   └── 20240101000002_add_user_phone_column.sql
│   └── 02/
│       └── 20240201000001_create_order_table.sql
└── schema_versions.csv  -- 迁移记录表
```

## 迁移脚本规范

### 升级脚本

```sql
-- 文件名：20240101000001_create_user_table.sql
-- 描述：创建用户表

-- 升级
CREATE TABLE sys_user (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 记录版本
INSERT INTO schema_versions (version, description, applied_at) 
VALUES ('20240101000001', '创建用户表', NOW());
```

### 回滚脚本

```sql
-- 文件名：20240101000001_create_user_table_rollback.sql
-- 描述：回滚创建用户表

DROP TABLE IF EXISTS sys_user;
DELETE FROM schema_versions WHERE version = '20240101000001';
```

## 版本记录表

```sql
CREATE TABLE schema_versions (
    version VARCHAR(50) NOT NULL PRIMARY KEY,
    description VARCHAR(255) NOT NULL COMMENT '迁移描述',
    applied_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '应用时间',
    rollback_sql TEXT COMMENT '回滚SQL'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据库版本记录表';
```

## 迁移工具

推荐使用以下迁移工具：

- **Flyway** (Java/Kotlin)
- **Liquibase** (Java)
- **Golang-Migrate** (Go)
- **Alembic** (Python)
- **Knex.js** (Node.js)

## 执行流程

1. 创建迁移脚本（包含升级和回滚）
2. 代码审查通过
3. 在测试环境执行
4. 验证功能正常
5. 生产环境执行（建议使用工具自动执行）
6. 记录版本信息