# 认证授权方案

## 概述

本文档定义了系统的认证和授权机制，确保应用的安全性。

## 认证方案

### 令牌认证（JWT）

```json
{
    "alg": "HS256",
    "typ": "JWT"
}
```

### 令牌结构

```json
// Header
{
    "alg": "HS256",
    "typ": "JWT"
}

// Payload
{
    "sub": "1234567890",
    "username": "admin",
    "role": "admin",
    "iat": 1704067200,
    "exp": 1704153600
}

// Signature
HMACSHA256(
    base64UrlEncode(header) + "." + base64UrlEncode(payload),
    secret
)
```

### 令牌配置

| 参数 | 说明 | 推荐值 |
|-----|------|-------|
| access_token | 访问令牌 | 2小时有效期 |
| refresh_token | 刷新令牌 | 7天有效期 |

## 授权方案

### 角色定义

```typescript
// 角色层次
enum Role {
    SUPER_ADMIN = 'super_admin',     // 超级管理员
    ADMIN = 'admin',                 // 管理员
    USER = 'user',                   // 普通用户
    GUEST = 'guest'                  // 访客
}

// 权限
enum Permission {
    USER_READ = 'user:read',
    USER_CREATE = 'user:create',
    USER_UPDATE = 'user:update',
    USER_DELETE = 'user:delete',
    ORDER_READ = 'order:read',
    ORDER_CREATE = 'order:create'
}
```

### RBAC 模型

```sql
-- 用户表
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(50),
    password VARCHAR(255),
    role VARCHAR(20)
);

-- 角色表
CREATE TABLE sys_role (
    id INT PRIMARY KEY,
    name VARCHAR(50),
    description VARCHAR(255)
);

-- 权限表
CREATE TABLE sys_permission (
    id INT PRIMARY KEY,
    name VARCHAR(50),
    code VARCHAR(100)
);

-- 用户角色关联
CREATE TABLE sys_user_role (
    user_id BIGINT,
    role_id INT
);

-- 角色权限关联
CREATE TABLE sys_role_permission (
    role_id INT,
    permission_id INT
);
```

## 登录流程

```
1. 用户提交用户名密码
2. 服务器验证凭证
3. 生成JWT令牌
4. 返回令牌给客户端
5. 客户端存储令牌
6. 后续请求携带令牌
```

## 安全措施

- 密码使用BCrypt加密存储
- 令牌使用HTTPS传输
- 实现令牌过期和刷新机制
- 记录登录日志
- 实施账户锁定策略