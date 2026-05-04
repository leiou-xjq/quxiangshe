# 趣享社用户体系模块需求规格说明书

## 文档信息

| 项目 | 内容 |
|-----|------|
| 文档编号 | SPEC-USER-001 |
| 模块名称 | 趣享社用户体系模块 |
| 版本 | v1.0.0 |
| 状态 | 已发布 |
| 创建日期 | 2024-01-01 |
| 作者 | 技术团队 |

---

## 1. 模块概述

### 1.1 业务背景

趣享社是一个社交分享平台，用户体系模块是整个系统的基础支撑模块。该模块负责管理用户的身份认证、信息维护和权限控制，是用户进入系统的第一道门槛，也是保障系统安全的关键模块。

### 1.2 模块目标

本模块旨在构建一个安全、稳定、易用的用户管理体系，主要实现以下目标：

- 提供用户注册功能，支持手机号和邮箱注册
- 实现用户登录功能，支持多种登录方式
- 构建基于JWT的认证授权体系
- 实现密码安全加密存储
- 实现登录和注册的频率限制，防止恶意攻击
- 提供完整的用户信息管理功能
- 实现基于RBAC的权限管理机制

### 1.3 适用范围

本规格说明书适用于趣享社后端服务开发、前端应用开发、测试用例设计以及相关系统集成工作。

---

## 2. 功能清单

| 功能编号 | 功能名称 | 功能描述 | 优先级 |
|---------|---------|---------|-------|
| F001 | 用户注册 | 支持手机号和邮箱注册新用户 | P0 |
| F002 | 用户登录 | 支持手机号密码登录和邮箱密码登录 | P0 |
| F003 | JWT认证 | 生成和验证JWT访问令牌 | P0 |
| F004 | 密码加密 | 使用BCrypt加密用户密码 | P0 |
| F005 | 登录限流 | 限制单IP短时间内的登录尝试次数 | P0 |
| F006 | 注册限流 | 限制单IP短时间内的注册尝试次数 | P0 |
| F007 | 用户信息查询 | 查询用户基本信息 | P0 |
| F008 | 用户信息更新 | 更新用户基本信息 | P0 |
| F009 | 密码修改 | 用户修改登录密码 | P0 |
| F010 | 角色管理 | 管理用户角色 | P1 |
| F011 | 权限管理 | 管理角色权限 | P1 |
| F012 | Token刷新 | 刷新访问令牌 | P1 |
| F013 | 用户登出 | 用户退出登录 | P2 |

---

## 3. 数据模型

### 3.1 数据库表设计

#### 3.1.1 用户表（sys_user）

用户表存储系统用户的基本信息，是用户体系的核心表。

```sql
CREATE TABLE sys_user (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username        VARCHAR(50) NOT NULL COMMENT '用户名',
    phone           VARCHAR(20) NULL COMMENT '手机号',
    email           VARCHAR(100) NULL COMMENT '邮箱',
    password        VARCHAR(255) NOT NULL COMMENT '密码',
    avatar          VARCHAR(255) NULL DEFAULT '/avatar/default.png' COMMENT '头像URL',
    nickname        VARCHAR(50) NULL COMMENT '昵称',
    gender          TINYINT NULL DEFAULT 0 COMMENT '性别：0-未知，1-男，2-女',
    birthday        DATE NULL COMMENT '生日',
    bio             VARCHAR(255) NULL COMMENT '个人简介',
    status          TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常，2-待审核',
    last_login_ip   VARCHAR(45) NULL COMMENT '最后登录IP',
    last_login_at   DATETIME NULL COMMENT '最后登录时间',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at      DATETIME NULL DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    UNIQUE INDEX uk_username (username),
    UNIQUE INDEX uk_phone (phone),
    UNIQUE INDEX uk_email (email),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
```

**字段说明：**

| 字段名 | 数据类型 | 约束 | 说明 |
|-------|---------|------|------|
| id | BIGINT | 主键自增 | 用户唯一标识 |
| username | VARCHAR(50) | 唯一非空 | 用户名，用于登录 |
| phone | VARCHAR(20) | 唯一可空 | 手机号，用于登录 |
| email | VARCHAR(100) | 唯一可空 | 邮箱，用于登录 |
| password | VARCHAR(255) | 非空 | BCrypt加密后的密码 |
| avatar | VARCHAR(255) | 默认值 | 头像URL |
| nickname | VARCHAR(50) | 可空 | 用户昵称 |
| gender | TINYINT | 默认值 | 性别 |
| birthday | DATE | 可空 | 生日 |
| bio | VARCHAR(255) | 可空 | 个人简介 |
| status | TINYINT | 默认值1 | 用户状态 |
| last_login_ip | VARCHAR(45) | 可空 | 最后登录IP |
| last_login_at | DATETIME | 可空 | 最后登录时间 |
| created_at | DATETIME | 默认值 | 创建时间 |
| updated_at | DATETIME | 自动更新 | 更新时间 |
| deleted_at | DATETIME | 可空 | 删除时间（软删除） |

#### 3.1.2 角色表（sys_role）

角色表定义系统中的角色，用于权限管理。

```sql
CREATE TABLE sys_role (
    id              INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    code            VARCHAR(50) NOT NULL COMMENT '角色编码',
    name            VARCHAR(50) NOT NULL COMMENT '角色名称',
    description     VARCHAR(255) NULL COMMENT '角色描述',
    status          TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at      DATETIME NULL DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    UNIQUE INDEX uk_code (code),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';
```

#### 3.1.3 权限表（sys_permission）

权限表定义系统中的权限，用于细粒度的访问控制。

```sql
CREATE TABLE sys_permission (
    id              INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '权限ID',
    code            VARCHAR(100) NOT NULL COMMENT '权限编码',
    name            VARCHAR(50) NOT NULL COMMENT '权限名称',
    description     VARCHAR(255) NULL COMMENT '权限描述',
    type            VARCHAR(20) NOT NULL COMMENT '权限类型：menu-菜单，button-按钮，api-接口',
    parent_id       INT UNSIGNED NULL DEFAULT 0 COMMENT '父权限ID',
    path            VARCHAR(255) NULL COMMENT '路由路径',
    component       VARCHAR(255) NULL COMMENT '组件路径',
    icon            VARCHAR(50) NULL COMMENT '图标',
    sort            INT NOT NULL DEFAULT 0 COMMENT '排序',
    status          TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at      DATETIME NULL DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    UNIQUE INDEX uk_code (code),
    INDEX idx_parent_id (parent_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';
```

#### 3.1.4 用户角色关联表（sys_user_role）

用户角色关联表建立用户和角色的多对多关系。

```sql
CREATE TABLE sys_user_role (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'ID',
    user_id         BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    role_id         INT UNSIGNED NOT NULL COMMENT '角色ID',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE INDEX uk_user_role (user_id, role_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';
```

#### 3.1.5 角色权限关联表（sys_role_permission）

角色权限关联表建立角色和权限的多对多关系。

```sql
CREATE TABLE sys_role_permission (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'ID',
    role_id         INT UNSIGNED NOT NULL COMMENT '角色ID',
    permission_id   INT UNSIGNED NOT NULL COMMENT '权限ID',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE INDEX uk_role_perm (role_id, permission_id),
    INDEX idx_role_id (role_id),
    INDEX idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';
```

#### 3.1.6 登录记录表（sys_login_log）

登录记录表用于记录用户登录历史，支持登录限流功能。

```sql
CREATE TABLE sys_login_log (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'ID',
    user_id         BIGINT UNSIGNED NULL COMMENT '用户ID',
    username        VARCHAR(50) NOT NULL COMMENT '用户名',
    login_type      VARCHAR(20) NOT NULL COMMENT '登录类型：phone-手机号，email-邮箱，username-用户名',
    ip              VARCHAR(45) NOT NULL COMMENT 'IP地址',
    user_agent      VARCHAR(500) NULL COMMENT '用户代理',
    status          TINYINT NOT NULL COMMENT '登录状态：0-失败，1-成功',
    fail_reason     VARCHAR(255) NULL COMMENT '失败原因',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_username (username),
    INDEX idx_ip (ip),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录记录表';
```

#### 3.1.7 接口限流表（sys_rate_limit）

接口限流表用于记录接口调用次数，支持注册和登录限流。

```sql
CREATE TABLE sys_rate_limit (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'ID',
    ip              VARCHAR(45) NOT NULL COMMENT 'IP地址',
    endpoint        VARCHAR(100) NOT NULL COMMENT '接口路径',
    count           INT NOT NULL DEFAULT 0 COMMENT '请求次数',
    expire_at       DATETIME NOT NULL COMMENT '过期时间',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE INDEX uk_ip_endpoint (ip, endpoint),
    INDEX idx_expire_at (expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='接口限流表';
```

### 3.2 数据模型ER图

```
┌─────────────┐       ┌─────────────┐       ┌─────────────┐
│  sys_user   │       │sys_user_role│       │  sys_role   │
├─────────────┤       ├─────────────┤       ├─────────────┤
│ id          │◄──────│ user_id     │       │ id          │
│ username    │       │ role_id     │──────►│ code        │
│ phone       │       └─────────────┘       │ name        │
│ email       │                              │ description │
│ password   │       ┌─────────────┐         └─────────────┘
│ nickname    │       │sys_role_perm│
│ avatar      │       ├─────────────┤         ┌─────────────┐
│ ...         │       │ role_id     │────────►│sys_permission│
└─────────────┘       │ perm_id     │         ├─────────────┤
                      └─────────────┘         │ id          │
                                            │ code        │
                      ┌─────────────┐       │ name        │
                      │sys_login_log│       │ type        │
                      ├─────────────┤       │ path        │
                      │ id          │       └─────────────┘
                      │ user_id     │
                      │ ip          │
                      │ status      │
                      └─────────────┘

┌─────────────┐
│sys_rate_limit│
├─────────────┤
│ id          │
│ ip          │
│ endpoint    │
│ count       │
│ expire_at   │
└─────────────┘
```

### 3.3 数据类型定义

#### 3.3.1 用户相关类型

```typescript
// 用户状态
enum UserStatus {
    DISABLED = 0,  // 禁用
    NORMAL = 1,    // 正常
    PENDING = 2    // 待审核
}

// 性别
enum Gender {
    UNKNOWN = 0,   // 未知
    MALE = 1,      // 男
    FEMALE = 2     // 女
}

// 用户实体
interface User {
    id: number;
    username: string;
    phone?: string;
    email?: string;
    avatar: string;
    nickname?: string;
    gender?: Gender;
    birthday?: string;
    bio?: string;
    status: UserStatus;
    lastLoginIp?: string;
    lastLoginAt?: string;
    createdAt: string;
    updatedAt: string;
}

// 用户创建DTO
interface CreateUserDTO {
    username: string;
    password: string;
    phone?: string;
    email?: string;
    nickname?: string;
    gender?: Gender;
}

// 用户更新DTO
interface UpdateUserDTO {
    nickname?: string;
    avatar?: string;
    gender?: Gender;
    birthday?: string;
    bio?: string;
}

// 修改密码DTO
interface ChangePasswordDTO {
    oldPassword: string;
    newPassword: string;
}
```

#### 3.3.2 认证相关类型

```typescript
// 登录请求
interface LoginRequest {
    loginType: 'username' | 'phone' | 'email';
    loginValue: string;
    password: string;
    captcha?: string;
    captchaKey?: string;
}

// 登录响应
interface LoginResponse {
    accessToken: string;
    refreshToken: string;
    tokenType: string;
    expiresIn: number;
    user: User;
}

// 注册请求
interface RegisterRequest {
    registerType: 'username' | 'phone' | 'email';
    registerValue: string;
    password: string;
    confirmPassword: string;
    code?: string;  // 验证码（手机号或邮箱注册时使用）
}

// Token刷新请求
interface RefreshTokenRequest {
    refreshToken: string;
}
```

#### 3.3.3 权限相关类型

```typescript
// 角色状态
enum RoleStatus {
    DISABLED = 0,
    NORMAL = 1
}

// 权限类型
enum PermissionType {
    MENU = 'menu',
    BUTTON = 'button',
    API = 'api'
}

// 角色实体
interface Role {
    id: number;
    code: string;
    name: string;
    description?: string;
    status: RoleStatus;
    createdAt: string;
    updatedAt: string;
}

// 权限实体
interface Permission {
    id: number;
    code: string;
    name: string;
    description?: string;
    type: PermissionType;
    parentId: number;
    path?: string;
    component?: string;
    icon?: string;
    sort: number;
    status: RoleStatus;
    createdAt: string;
    updatedAt: string;
}
```

---

## 4. 接口契约

### 4.1 接口列表

| 接口编号 | 接口路径 | 方法 | 说明 |
|---------|---------|------|------|
| API-001 | /api/v1/auth/register | POST | 用户注册 |
| API-002 | /api/v1/auth/login | POST | 用户登录 |
| API-003 | /api/v1/auth/logout | POST | 用户登出 |
| API-004 | /api/v1/auth/refresh | POST | 刷新Token |
| API-005 | /api/v1/users/me | GET | 获取当前用户信息 |
| API-006 | /api/v1/users/me | PUT | 更新当前用户信息 |
| API-007 | /api/v1/users/me/password | PUT | 修改密码 |
| API-008 | /api/v1/users/:id | GET | 获取指定用户信息 |
| API-009 | /api/v1/roles | GET | 获取角色列表 |
| API-010 | /api/v1/roles | POST | 创建角色 |
| API-011 | /api/v1/roles/:id | PUT | 更新角色 |
| API-012 | /api/v1/roles/:id | DELETE | 删除角色 |
| API-013 | /api/v1/permissions | GET | 获取权限列表 |
| API-014 | /api/v1/captcha | GET | 获取验证码 |

### 4.2 接口详细设计

#### 4.2.1 用户注册接口

**接口信息：**

| 项目 | 内容 |
|-----|------|
| 接口路径 | /api/v1/auth/register |
| 请求方法 | POST |
| 请求头 | Content-Type: application/json |
| 认证要求 | 否 |

**请求参数：**

```json
{
    "registerType": "username",
    "registerValue": "zhangsan",
    "password": "Aa123456",
    "confirmPassword": "Aa123456",
    "nickname": "张三"
}
```

| 参数 | 类型 | 必填 | 描述 |
|-----|------|-----|------|
| registerType | string | 是 | 注册类型：username/phone/email |
| registerValue | string | 是 | 注册值（用户名/手机号/邮箱） |
| password | string | 是 | 密码，6-20位，必须包含大小写字母和数字 |
| confirmPassword | string | 是 | 确认密码，与password一致 |
| code | string | 否 | 验证码（手机号或邮箱注册时必填） |
| nickname | string | 否 | 昵称 |

**响应成功（201 Created）：**

```json
{
    "code": 0,
    "message": "注册成功",
    "data": {
        "userId": 10001,
        "username": "zhangsan"
    },
    "timestamp": "2024-01-01T12:00:00.000+08:00"
}
```

**错误响应：**

```json
{
    "code": 1001001,
    "message": "用户名已存在",
    "timestamp": "2024-01-01T12:00:00.000+08:00"
}
```

| 错误码 | 错误信息 |
|-------|---------|
| 1001001 | 用户名已存在 |
| 1001002 | 手机号已注册 |
| 1001003 | 邮箱已注册 |
| 1001004 | 密码格式不正确 |
| 1001005 | 两次密码不一致 |
| 1001006 | 验证码错误 |
| 1001007 | 验证码已过期 |
| 1001008 | 注册过于频繁，请稍后重试 |
| 4000001 | 请求参数错误 |

#### 4.2.2 用户登录接口

**接口信息：**

| 项目 | 内容 |
|-----|------|
| 接口路径 | /api/v1/auth/login |
| 请求方法 | POST |
| 请求头 | Content-Type: application/json |
| 认证要求 | 否 |

**请求参数：**

```json
{
    "loginType": "username",
    "loginValue": "zhangsan",
    "password": "Aa123456",
    "captcha": "abcd",
    "captchaKey": "captcha-key-123"
}
```

| 参数 | 类型 | 必填 | 描述 |
|-----|------|-----|------|
| loginType | string | 是 | 登录类型：username/phone/email |
| loginValue | string | 是 | 登录值 |
| password | string | 是 | 密码 |
| captcha | string | 否 | 图形验证码（错误3次后需要） |
| captchaKey | string | 否 | 图形验证码Key |

**响应成功（200 OK）：**

```json
{
    "code": 0,
    "message": "登录成功",
    "data": {
        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "tokenType": "Bearer",
        "expiresIn": 7200,
        "user": {
            "id": 10001,
            "username": "zhangsan",
            "phone": "13800138000",
            "email": "zhangsan@example.com",
            "avatar": "/avatar/default.png",
            "nickname": "张三",
            "gender": 1,
            "status": 1
        }
    },
    "timestamp": "2024-01-01T12:00:00.000+08:00"
}
```

**错误响应：**

| 错误码 | 错误信息 |
|-------|---------|
| 1002001 | 用户名/手机号/邮箱不存在 |
| 1002002 | 密码错误 |
| 1002003 | 账户已被禁用 |
| 1002004 | 登录过于频繁，请稍后重试 |
| 1002005 | 验证码错误 |
| 4000001 | 请求参数错误 |

#### 4.2.3 用户登出接口

**接口信息：**

| 项目 | 内容 |
|-----|------|
| 接口路径 | /api/v1/auth/logout |
| 请求方法 | POST |
| 认证要求 | 是 |

**请求头：**

```
Authorization: Bearer {accessToken}
```

**响应成功（200 OK）：**

```json
{
    "code": 0,
    "message": "登出成功",
    "data": null,
    "timestamp": "2024-01-01T12:00:00.000+08:00"
}
```

#### 4.2.4 Token刷新接口

**接口信息：**

| 项目 | 内容 |
|-----|------|
| 接口路径 | /api/v1/auth/refresh |
| 请求方法 | POST |
| 认证要求 | 否 |

**请求参数：**

```json
{
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**响应成功（200 OK）：**

```json
{
    "code": 0,
    "message": "刷新成功",
    "data": {
        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "tokenType": "Bearer",
        "expiresIn": 7200
    },
    "timestamp": "2024-01-01T12:00:00.000+08:00"
}
```

**错误响应：**

| 错误码 | 错误信息 |
|-------|---------|
| 1003001 | refreshToken无效 |
| 1003002 | refreshToken已过期 |

#### 4.2.5 获取当前用户信息接口

**接口信息：**

| 项目 | 内容 |
|-----|------|
| 接口路径 | /api/v1/users/me |
| 请求方法 | GET |
| 认证要求 | 是 |

**响应成功（200 OK）：**

```json
{
    "code": 0,
    "message": "success",
    "data": {
        "id": 10001,
        "username": "zhangsan",
        "phone": "13800138000",
        "email": "zhangsan@example.com",
        "avatar": "/avatar/default.png",
        "nickname": "张三",
        "gender": 1,
        "birthday": "1995-06-15",
        "bio": "热爱生活，积极向上",
        "status": 1,
        "lastLoginIp": "192.168.1.100",
        "lastLoginAt": "2024-01-01T10:30:00.000+08:00",
        "createdAt": "2023-06-01T00:00:00.000+08:00",
        "updatedAt": "2024-01-01T12:00:00.000+08:00"
    },
    "timestamp": "2024-01-01T12:00:00.000+08:00"
}
```

#### 4.2.6 更新当前用户信息接口

**接口信息：**

| 项目 | 内容 |
|-----|------|
| 接口路径 | /api/v1/users/me |
| 请求方法 | PUT |
| 认证要求 | 是 |

**请求参数：**

```json
{
    "nickname": "张三更新",
    "avatar": "/avatar/custom.png",
    "gender": 1,
    "birthday": "1995-06-15",
    "bio": "这是我的个人简介"
}
```

| 参数 | 类型 | 必填 | 描述 |
|-----|------|-----|------|
| nickname | string | 否 | 昵称，2-20位 |
| avatar | string | 否 | 头像URL |
| gender | int | 否 | 性别：0-未知，1-男，2-女 |
| birthday | string | 否 | 生日，格式：YYYY-MM-DD |
| bio | string | 否 | 个人简介，不超过255字符 |

**响应成功（200 OK）：**

```json
{
    "code": 0,
    "message": "更新成功",
    "data": {
        "id": 10001,
        "nickname": "张三更新",
        "avatar": "/avatar/custom.png",
        "gender": 1,
        "birthday": "1995-06-15",
        "bio": "这是我的个人简介"
    },
    "timestamp": "2024-01-01T12:00:00.000+08:00"
}
```

#### 4.2.7 修改密码接口

**接口信息：**

| 项目 | 内容 |
|-----|------|
| 接口路径 | /api/v1/users/me/password |
| 请求方法 | PUT |
| 认证要求 | 是 |

**请求参数：**

```json
{
    "oldPassword": "Aa123456",
    "newPassword": "Bb123456",
    "confirmPassword": "Bb123456"
}
```

| 参数 | 类型 | 必填 | 描述 |
|-----|------|-----|------|
| oldPassword | string | 是 | 原密码 |
| newPassword | string | 是 | 新密码，6-20位，必须包含大小写字母和数字 |
| confirmPassword | string | 是 | 确认新密码 |

**错误响应：**

| 错误码 | 错误信息 |
|-------|---------|
| 1004001 | 原密码错误 |
| 1004002 | 新密码格式不正确 |
| 1004003 | 两次密码不一致 |

#### 4.2.8 获取指定用户信息接口

**接口信息：**

| 项目 | 内容 |
|-----|------|
| 接口路径 | /api/v1/users/:id |
| 请求方法 | GET |
| 认证要求 | 是 |

**响应成功（200 OK）：**

```json
{
    "code": 0,
    "message": "success",
    "data": {
        "id": 10001,
        "username": "zhangsan",
        "avatar": "/avatar/default.png",
        "nickname": "张三",
        "gender": 1,
        "bio": "热爱生活，积极向上",
        "createdAt": "2023-06-01T00:00:00.000+08:00"
    },
    "timestamp": "2024-01-01T12:00:00.000+08:00"
}
```

**说明：** 返回的公开信息不包含手机号、邮箱等敏感信息。

#### 4.2.9 获取验证码接口

**接口信息：**

| 项目 | 内容 |
|-----|------|
| 接口路径 | /api/v1/captcha |
| 请求方法 | GET |

**响应成功（200 OK）：**

```json
{
    "code": 0,
    "message": "success",
    "data": {
        "captchaKey": "captcha-key-123456",
        "captchaImage": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA..."
    },
    "timestamp": "2024-01-01T12:00:00.000+08:00"
}
```

---

## 5. 业务规则

### 5.1 用户注册规则

#### 5.1.1 用户名注册规则

- 用户名长度为4-20个字符
- 支持字母、数字、下划线
- 首字符必须是字母
- 用户名唯一，不可重复
- 不区分大小写存储，但登录时区分大小写

#### 5.1.2 手机号注册规则

- 手机号必须为中国大陆手机号（11位）
- 手机号唯一，不可重复
- 需要验证码验证
- 手机号脱敏显示：138****8000

#### 5.1.3 邮箱注册规则

- 邮箱格式符合RFC 5322标准
- 邮箱唯一，不可重复
- 需要邮箱验证码验证
- 邮箱脱敏显示：a***@example.com

#### 5.1.4 密码规则

- 密码长度为6-20个字符
- 必须包含至少一个大写字母
- 必须包含至少一个小写字母
- 必须包含至少一个数字
- 禁止使用连续重复字符（如111、aaa）
- 禁止使用常见弱密码（如password、123456）

### 5.2 密码加密规则

- 使用BCrypt算法加密密码
- 加密强度：Cost Factor = 10
- 存储格式：`$2a$10${salt}${hash}`
- 密码验证时使用BCrypt验证
- 用户设置密码时需要使用安全的随机数生成器生成salt

### 5.3 认证规则

#### 5.3.1 JWT令牌规则

**Access Token：**
- 签名算法：HS256
- 有效期：2小时（7200秒）
- Payload包含：userId、username、role、iat、exp

```json
{
    "sub": "10001",
    "username": "zhangsan",
    "role": "user",
    "iat": 1704067200,
    "exp": 1704074400
}
```

**Refresh Token：**
- 签名算法：HS256
- 有效期：7天（604800秒）
- Payload包含：userId、type、iat、exp

```json
{
    "sub": "10001",
    "type": "refresh",
    "iat": 1704067200,
    "exp": 1704672000
}
```

#### 5.3.2 Token存储规则

- Access Token：存储在客户端内存中
- Refresh Token：存储在客户端本地存储（如localStorage或cookie）
- 服务器端不存储Token，通过签名验证
- 支持Token blacklist机制用于登出

### 5.4 登录限流规则

#### 5.4.1 限流条件

- 同一IP地址
- 同一接口路径
- 时间窗口：15分钟
- 最大尝试次数：10次

#### 5.4.2 限流处理

- 超过最大尝试次数，返回错误码1004004
- 限流期间：15分钟
- 超过限流后，需要等待或更换IP
- 登录失败记录到sys_login_log表

#### 5.4.3 错误次数阈值

- 连续错误3次：需要图形验证码
- 连续错误5次：账号锁定30分钟
- 连续错误10次：IP限流15分钟

### 5.5 注册限流规则

#### 5.5.1 限流条件

- 同一IP地址
- 注册接口路径
- 时间窗口：1小时
- 最大尝试次数：5次

#### 5.5.2 限流处理

- 超过最大尝试次数，返回错误码1001008
- 限流期间：1小时

### 5.6 用户状态规则

| 状态值 | 状态名称 | 说明 | 是否可登录 |
|-------|---------|------|-----------|
| 0 | 禁用 | 管理员禁用 | 否 |
| 1 | 正常 | 正常状态 | 是 |
| 2 | 待审核 | 注册待审核 | 否 |

### 5.7 权限管理规则

#### 5.7.1 角色规则

- 系统内置角色：超级管理员（super_admin）、管理员（admin）、普通用户（user）、访客（guest）
- 超级管理员拥有所有权限，不可修改和删除
- 角色名称唯一，不可重复
- 角色编码唯一，不可重复

#### 5.7.2 权限规则

- 权限类型：菜单（menu）、按钮（button）、接口（api）
- 权限编码格式：{模块}:{功能}
- 菜单权限：控制页面访问
- 按钮权限：控制页面内操作
- 接口权限：控制API访问

#### 5.7.3 RBAC规则

- 用户可以拥有多个角色
- 角色可以拥有多个权限
- 权限判断：用户权限 = 用户所有角色权限的并集
- 继承关系：子菜单权限继承父菜单权限

### 5.8 数据校验规则

#### 5.8.1 用户名校验

```regex
^[a-zA-Z][a-zA-Z0-9_]{3,19}$
```

#### 5.8.2 手机号校验

```regex
^1[3-9]\d{9}$
```

#### 5.8.3 邮箱校验

```regex
^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$
```

#### 5.8.4 密码校验

```regex
^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d@$!%*?&]{6,20}$
```

---

## 6. 测试标准

### 6.1 单元测试

#### 6.1.1 用户注册模块测试

| 测试用例ID | 测试用例名称 | 测试步骤 | 预期结果 |
|-----------|-------------|---------|---------|
| UT-REG-001 | 用户名注册成功 | 传入合法用户名、密码、确认密码 | 返回201，生成用户记录 |
| UT-REG-002 | 用户名已存在 | 传入已存在的用户名 | 返回错误码1001001 |
| UT-REG-003 | 用户名格式错误 | 传入非法格式用户名 | 返回错误码4000001 |
| UT-REG-004 | 密码格式错误 | 传入不符合规则的密码 | 返回错误码1001004 |
| UT-REG-005 | 密码不一致 | 传入不同密码和确认密码 | 返回错误码1001005 |
| UT-REG-006 | 手机号注册成功 | 传入合法手机号、验证码 | 返回201，生成用户记录 |
| UT-REG-007 | 邮箱注册成功 | 传入合法邮箱、验证码 | 返回201，生成用户记录 |

#### 6.1.2 用户登录模块测试

| 测试用例ID | 测试用例名称 | 测试步骤 | 预期结果 |
|-----------|-------------|---------|---------|
| UT-LOGIN-001 | 用户名登录成功 | 传入正确用户名和密码 | 返回200，accessToken |
| UT-LOGIN-002 | 密码错误 | 传入正确用户名和错误密码 | 返回错误码1002002 |
| UT-LOGIN-003 | 用户不存在 | 传入不存在的用户名 | 返回错误码1002001 |
| UT-LOGIN-004 | 账户禁用 | 登录被禁用的账户 | 返回错误码1002003 |
| UT-LOGIN-005 | 登录限流 | 连续10次错误登录 | 返回错误码1004004 |
| UT-LOGIN-006 | 需要验证码 | 连续3次错误登录 | 提示需要验证码 |

#### 6.1.3 密码加密测试

| 测试用例ID | 测试用例名称 | 测试步骤 | 预期结果 |
|-----------|-------------|---------|---------|
| UT-PWD-001 | BCrypt加密 | 对明文密码进行加密 | 加密后与原密码不相似 |
| UT-PWD-002 | BCrypt验证 | 验证正确密码 | 验证通过 |
| UT-PWD-003 | BCrypt错误验证 | 验证错误密码 | 验证不通过 |
| UT-PWD-004 | 不同salt | 同一密码两次加密 | 结果不同 |
| UT-PWD-005 | 密码修改 | 修改用户密码 | 新密码可用，旧密码不可用 |

#### 6.1.4 JWT认证测试

| 测试用例ID | 测试用例名称 | 测试步骤 | 预期结果 |
|-----------|-------------|---------|---------|
| UT-JWT-001 | 生成Token | 登录成功后生成Token | 返回accessToken和refreshToken |
| UT-JWT-002 | 验证有效Token | 使用有效Token访问受保护接口 | 返回200，数据正常 |
| UT-JWT-003 | Token过期 | 使用过期Token访问 | 返回401，提示Token过期 |
| UT-JWT-004 | Token伪造 | 使用伪造Token访问 | 返回401，提示Token无效 |
| UT-JWT-005 | 刷新Token | 使用refreshToken获取新Token | 返回新的accessToken |

#### 6.1.5 限流模块测试

| 测试用例ID | 测试用例名称 | 测试步骤 | 预期结果 |
|-----------|-------------|---------|---------|
| UT-RATE-001 | 登录限流 | 15分钟内10次错误登录 | 返回错误码1004004 |
| UT-RATE-002 | 注册限流 | 1小时内5次注册 | 返回错误码1001008 |
| UT-RATE-003 | 限流解除 | 限流时间过后 | 可以正常访问 |

### 6.2 集成测试

#### 6.2.1 用户注册流程测试

| 测试用例ID | 测试用例名称 | 测试步骤 | 预期结果 |
|-----------|-------------|---------|---------|
| IT-REG-001 | 完整注册流程 | 1.获取验证码 2.提交注册 | 用户创建成功，可登录 |
| IT-REG-002 | 验证码错误 | 提交错误验证码 | 返回验证码错误 |
| IT-REG-003 | 验证码过期 | 等待验证码过期后提交 | 返回验证码已过期 |

#### 6.2.2 用户登录流程测试

| 测试用例ID | 测试用例名称 | 测试步骤 | 预期结果 |
|-----------|-------------|---------|---------|
| IT-LOGIN-001 | 完整登录流程 | 1.登录 2.访问受保护资源 | 登录成功，资源可访问 |
| IT-LOGIN-002 | Token失效 | 使用失效Token访问 | 返回401 |
| IT-LOGIN-003 | 登出流程 | 1.登录 2.登出 3.使用原Token访问 | 登出成功，原Token失效 |

#### 6.2.3 权限控制测试

| 测试用例ID | 测试用例名称 | 测试步骤 | 预期结果 |
|-----------|-------------|---------|---------|
| IT-AUTH-001 | 角色权限验证 | 使用不同角色访问对应权限 | 正确角色可访问，非正确角色拒绝 |
| IT-AUTH-002 | 菜单权限验证 | 访问不同角色对应菜单 | 只显示有权限的菜单 |
| IT-AUTH-003 | 按钮权限验证 | 点击不同角色对应按钮 | 只显示有权限的按钮 |

### 6.3 性能测试

| 测试项 | 测试指标 | 预期值 |
|-------|---------|-------|
| 注册接口响应时间 | P99 | < 500ms |
| 登录接口响应时间 | P99 | < 300ms |
| 用户信息查询 | P99 | < 200ms |
| 并发注册 | QPS | > 100 |
| 并发登录 | QPS | > 200 |

### 6.4 安全测试

| 测试项 | 测试内容 | 预期结果 |
|-------|---------|---------|
| SQL注入 | 尝试SQL注入攻击 | 被阻止，无数据泄露 |
| XSS攻击 | 尝试XSS攻击 | 被阻止，特殊字符被转义 |
| 暴力破解 | 暴力破解密码 | 被限流机制阻止 |
| Token伪造 | 尝试伪造Token | 验证失败，返回401 |
| 权限绕过 | 尝试访问无权限接口 | 被拒绝，返回403 |

---

## 7. 验收条件

### 7.1 功能验收

#### 7.1.1 用户注册

| 验收项 | 验收标准 | 验收方式 |
|-------|---------|---------|
| 用户名注册 | 4-20位字母开头，支持数字下划线 | 输入各种格式验证 |
| 手机号注册 | 11位大陆手机号，需要验证码 | 输入正确和错误手机号 |
| 邮箱注册 | 符合邮箱格式，需要验证码 | 输入正确和错误邮箱 |
| 密码验证 | 6-20位大小写数字组合 | 输入各种密码格式 |
| 注册限流 | 1小时5次，超过限流 | 测试超过限流 |

**验收通过标准：** 所有规则均正确实现并通过测试

#### 7.1.2 用户登录

| 验收项 | 验收标准 | 验收方式 |
|-------|---------|---------|
| 用户名登录 | 用户名+密码登录 | 测试登录成功和失败 |
| 手机号登录 | 手机号+密码登录 | 测试登录成功和失败 |
| 邮箱登录 | 邮箱+密码登录 | 测试登录成功和失败 |
| 登录限流 | 15分钟10次，超过限流 | 测试限流机制 |
| 验证码 | 连续3次错误出现验证码 | 测试验证码机制 |

**验收通过标准：** 所有登录方式正常，限流机制有效

#### 7.1.3 JWT认证

| 验收项 | 验收标准 | 验收方式 |
|-------|---------|---------|
| Token生成 | 登录成功生成正确格式Token | 检查Token格式和内容 |
| Token验证 | 有效Token可访问受保护资源 | 测试受保护接口 |
| Token过期 | 过期Token返回401 | 测试过期Token |
| Token刷新 | refreshToken可刷新Token | 测试刷新流程 |

**验收通过标准：** JWT机制完整实现

#### 7.1.4 密码安全

| 验收项 | 验收标准 | 验收方式 |
|-------|---------|---------|
| BCrypt加密 | 密码加密存储 | 检查数据库存储内容 |
| 密码验证 | 正确密码可通过验证 | 测试登录验证 |
| 密码修改 | 原密码验证后修改 | 测试修改密码流程 |

**验收通过标准：** 密码安全机制有效

#### 7.1.5 用户信息管理

| 验收项 | 验收标准 | 验收方式 |
|-------|---------|---------|
| 查询用户 | 获取用户公开信息 | 测试查询接口 |
| 更新信息 | 更新用户基本信息 | 测试更新接口 |
| 修改密码 | 修改用户密码 | 测试修改密码 |

**验收通过标准：** 用户信息管理功能正常

#### 7.1.6 权限管理

| 验收项 | 验收标准 | 验收方式 |
|-------|---------|---------|
| 角色管理 | 创建、修改、删除角色 | 测试CRUD操作 |
| 权限管理 | 创建、修改、删除权限 | 测试CRUD操作 |
| 用户角色 | 分配和撤销用户角色 | 测试角色分配 |
| 权限验证 | 正确验证用户权限 | 测试权限控制 |

**验收通过标准：** RBAC权限管理完整实现

### 7.2 性能验收

| 验收项 | 验收指标 | 验收标准 |
|-------|---------|---------|
| 注册性能 | 响应时间P99 | < 500ms |
| 登录性能 | 响应时间P99 | < 300ms |
| 查询性能 | 响应时间P99 | < 200ms |
| 并发能力 | 支持QPS | 注册>100，登录>200 |
| 系统稳定性 | 24小时运行 | 无内存泄漏，无崩溃 |

### 7.3 安全验收

| 验收项 | 验收标准 | 验收方式 |
|-------|---------|---------|
| 密码安全 | BCrypt加密 | 检查数据库存储 |
| 暴力破解防护 | 限流机制有效 | 模拟暴力破解 |
| SQL注入防护 | 无SQL注入漏洞 | 安全扫描 |
| XSS防护 | 无XSS漏洞 | 安全扫描 |
| Token安全 | Token不可伪造 | 安全测试 |

### 7.4 接口验收

| 验收项 | 验收标准 | 验收方式 |
|-------|---------|---------|
| 接口规范性 | 符合RESTful规范 | 接口审查 |
| 响应格式 | 统一响应格式 | 接口测试 |
| 错误码规范 | 错误码统一管理 | 接口测试 |
| 文档完整性 | 接口文档完整 | 文档审查 |

---

## 8. 附录

### 8.1 错误码对照表

#### 用户模块错误码（10）

| 错误码 | 说明 |
|-------|------|
| 1001001 | 用户名已存在 |
| 1001002 | 手机号已注册 |
| 1001003 | 邮箱已注册 |
| 1001004 | 密码格式不正确 |
| 1001005 | 两次密码不一致 |
| 1001006 | 验证码错误 |
| 1001007 | 验证码已过期 |
| 1001008 | 注册过于频繁 |
| 1001009 | 用户名格式不正确 |
| 1001010 | 手机号格式不正确 |

#### 认证模块错误码（20）

| 错误码 | 说明 |
|-------|------|
| 1002001 | 用户不存在 |
| 1002002 | 密码错误 |
| 1002003 | 账户已禁用 |
| 1002004 | 登录过于频繁 |
| 1002005 | 验证码错误 |
| 1002006 | 账户待审核 |

#### Token模块错误码（30）

| 错误码 | 说明 |
|-------|------|
| 1003001 | Token无效 |
| 1003002 | Token已过期 |
| 1003003 | Token已撤销 |
| 1003004 | refreshToken无效 |

#### 密码模块错误码（40）

| 错误码 | 说明 |
|-------|------|
| 1004001 | 原密码错误 |
| 1004002 | 新密码格式不正确 |
| 1004003 | 两次密码不一致 |
| 1004004 | 登录限流中 |

### 8.2 数据库初始化数据

#### 8.2.1 初始角色

| code | name | description |
|-----|------|-------------|
| super_admin | 超级管理员 | 拥有系统所有权限 |
| admin | 管理员 | 系统管理角色 |
| user | 普通用户 | 普通用户角色 |
| guest | 访客 | 访客角色 |

#### 8.2.2 初始权限

| code | name | type | parent_id |
|-----|------|------|-----------|
| system | 系统管理 | menu | 0 |
| system.user | 用户管理 | menu | 1 |
| system.user.list | 用户列表 | button | 2 |
| system.user.create | 创建用户 | button | 2 |
| system.user.update | 更新用户 | button | 2 |
| system.user.delete | 删除用户 | button | 2 |
| system.role | 角色管理 | menu | 1 |
| system.role.list | 角色列表 | button | 5 |
| system.role.create | 创建角色 | button | 5 |
| system.role.update | 更新角色 | button | 5 |
| system.role.delete | 删除角色 | button | 5 |

---

## 9. 版本历史

| 版本 | 日期 | 作者 | 变更说明 |
|-----|------|------|---------|
| v1.0.0 | 2024-01-01 | 技术团队 | 初始版本 |

---

**文档结束**