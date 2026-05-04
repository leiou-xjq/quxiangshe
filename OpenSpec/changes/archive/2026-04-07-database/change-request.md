# 数据库变更申请

## 变更记录

| 变更编号 | 变更类型 | 状态 | 创建日期 |
|---------|---------|------|---------|
| DB-CHANGE-001 | Feature | 待评审 | 2024-01-01 |

### DB-CHANGE-001：添加用户手机号字段

#### 变更描述

为sys_user表添加手机号字段，用于用户手机号登录

#### 变更内容

```sql
ALTER TABLE sys_user 
ADD COLUMN phone VARCHAR(20) NULL COMMENT '手机号' AFTER email,
ADD UNIQUE INDEX uk_phone (phone);
```

#### 影响范围

- 后端：用户服务需适配新字段
- 前端：用户表单需增加手机号字段

#### 评审意见

[待评审]

#### 回滚方案

```sql
ALTER TABLE sys_user 
DROP COLUMN phone;
```