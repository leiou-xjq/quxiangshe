# 数据库ER图设计

## 概述

本文档描述核心业务表的ER图设计

## 实体说明

### 用户模块

```
┌─────────────┐       ┌─────────────┐       ┌─────────────┐
│   sys_user  │       │sys_user_role│       │  sys_role   │
├─────────────┤       ├─────────────┤       ├─────────────┤
│ id          │◄──────│ user_id     │       │ id          │
│ username    │       │ role_id     │──────►│ name        │
│ password    │       └─────────────┘       │ code        │
│ email       │                              │ description │
│ phone       │       ┌─────────────┐       └─────────────┘
│ real_name   │       │sys_role_perm│
│ status      │       ├─────────────┤       ┌─────────────┐
│ created_at  │       │ role_id     │──────►│sys_permission│
│ updated_at  │       │ perm_id     │       ├─────────────┤
│ deleted_at  │       └─────────────┘       │ id          │
└─────────────┘                              │ name        │
                                              │ code        │
                                              │ description │
                                              └─────────────┘
```

### 订单模块

```
┌─────────────┐       ┌─────────────┐
│  oms_order  │       │ oms_order_item│
├─────────────┤       ├─────────────┤
│ id          │◄──────│ order_id    │
│ order_no    │       │ product_id  │
│ user_id     │──────►│ product_name│
│ total_amount│       │ price       │
│ status      │       │ quantity    │
│ created_at  │       │ subtotal    │
│ updated_at  │       └─────────────┘
└─────────────┘
```

## 表关系

| 关系 | 类型 | 说明 |
-----|------|------|
| sys_user - sys_user_role | 一对多 | 一个用户可以有多个角色 |
| sys_role - sys_user_role | 一对多 | 一个角色可以有多个用户 |
| sys_role - sys_role_permission | 一对多 | 一个角色可以有多个权限 |
| sys_permission - sys_role_permission | 一对多 | 一个权限可以属于多个角色 |
| oms_order - oms_order_item | 一对多 | 一个订单可以有多个明细 |

## 索引设计

### 用户表索引

| 索引名 | 字段 | 类型 |
|-------|------|------|
| pk_sys_user | id | 主键 |
| uk_username | username | 唯一 |
| uk_email | email | 唯一 |
| idx_status | status | 普通 |
| idx_created_at | created_at | 普通 |

### 订单表索引

| 索引名 | 字段 | 类型 |
|-------|------|------|
| pk_oms_order | id | 主键 |
| uk_order_no | order_no | 唯一 |
| idx_user_id | user_id | 普通 |
| idx_status | status | 普通 |
| idx_created_at | created_at | 普通 |