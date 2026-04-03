# 趣享社认证模块接口文档

## 一、接口总览

下列接口覆盖用户注册、登录、验证码服务、登录态校验、退出登录，以及与安全相关的限流/黑名单机制设计。所有接口遵循统一的响应结构，成功返回数据在 `data` 字段内，失败统一返回错误信息。

| 模块 | 接口地址 | 方法 | 说明 | 备注 |
|---|---|---|---|---|
| 认证 | `/api/v1/auth/register` | POST | 用户注册 | - |
| 认证 | `/api/v1/auth/login` | POST | 密码登录 | - |
| 认证 | `/api/v1/auth/phone-login` | POST | 手机验证码登录 | - |
| 认证 | `/api/v1/auth/email-login` | POST | 邮箱验证码登录 | - |
| 认证 | `/api/v1/auth/send-code` | POST | 发送手机验证码 | - |
| 认证 | `/api/v1/auth/send-email-code` | POST | 发送邮箱验证码 | - |
| 认证 | `/api/v1/auth/refresh` | POST | 刷新Token | - |
| 认证 | `/api/v1/auth/logout` | POST | 退出登录 | - |
| 用户 | `/api/v1/user/me` | GET | 登录状态校验 | 需要 Token |
| 认证 | `/api/v1/auth/check-phone` | GET | 检查手机号是否存在 | - |
| 认证 | `/api/v1/auth/check-email` | GET | 检查邮箱是否存在 | - |
| 认证 | `/api/v1/auth/check-username` | GET | 检查用户名是否存在 | - |

> 备注：IP 黑名单与滑动窗口限流等安全机制通过网关/中间件实现，公开接口未暴露专门的 IP 黑名单管理端点，限流机制在接口层面通过注解和中间件进行控制。具体策略详见 AUTH_MODULE.md 的实现设计章节。

---

## 二、接口详情

### 1. 用户注册

**请求**
```
POST /api/v1/auth/register
Content-Type: application/json
```

**请求参数**
| 字段 | 类型 | 必填 | 说明 | 校验规则 |
|---|---|---|---|---|
| username | string | 是 | 用户名 | 4-20 字符 |
| password | string | 是 | 密码 | 6-20 字符 |
| phone | string | 是 | 手机号 | 1[3-9]\d{9} |
| email | string | 是 | 邮箱 | 邮箱格式 |
| nickname | string | 否 | 昵称 | 最长 50 字符 |

**请求体示例**
```
{
  "username": "testuser",
  "password": "123456",
  "phone": "13800138000",
  "email": "test@example.com",
  "nickname": "测试用户"
}
```

**响应参数**
| 字段 | 类型 | 说明 |
|---|---|---|
| code | int | 状态码（0 表示成功） |
| message | string | 消息 |
| data | object | 成功时返回数据 |

**响应示例**
```
{ "code": 0, "message": "success", "data": { "userId": 1, "username": "testuser" } }
```

**错误码**
- 400: 用户名已被注册
- 400: 手机号已被注册
- 400: 邮箱已被注册
- 429: 注册请求过于频繁

---

### 2. 密码登录

**请求**
```
POST /api/v1/auth/login
Content-Type: application/json
```

**请求参数**
| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| username | string | 是 | 用户名/手机号/邮箱 |
| password | string | 是 | 密码 |

**响应参数**
| 字段 | 类型 | 说明 |
|---|---|---|
| code | int |
| message | string |
| data | object |

**响应示例**
```
{ "code": 0, "message": "success", "data": {
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 1800,
  "userId": 1,
  "username": "testuser",
  "nickname": "测试用户",
  "avatarUrl": ""
} }
```

**错误码**
- 401: 用户名或密码错误

---

### 3. 发送手机验证码

**请求**
```
POST /api/v1/auth/send-code
Content-Type: application/json
```

**请求参数**
| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| phone | string | 是 | 手机号 |

**响应示例**
```
{ "code": 0, "message": "success", "data": "验证码已发送" }
```

**错误码**
- 400: 手机号格式不正确
- 429: 验证码获取过于频繁

---

### 4. 短信验证码登录

**请求**
```
POST /api/v1/auth/phone-login
Content-Type: application/json
```

**请求参数**
| 字段 | 类型 | 必填 | 说明 | 校验规则 |
|---|---|---|---|---|
| phone | string | 是 | 手机号 | 1[3-9]\d{9} |
| code | string | 是 | 验证码 | 6 位数字 |

**响应示例**
```
{ "code": 0, "message": "success", "data": {
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 1800,
  "userId": 1,
  "username": "testuser"
} }
```

**错误码**
- 400: 验证码错误
- 400: 手机号未注册

---

### 5. 邮箱验证码登录

**请求**
```
POST /api/v1/auth/email-login
Content-Type: application/json
```

**请求参数**
| 字段 | 类型 | 必填 | 说明 | 校验规则 |
|---|---|---|---|---|
| email | string | 是 | 邮箱 |
| code | string | 是 | 验证码 | 6 位数字 |

**响应示例**
```
{ "code": 0, "message": "success", "data": {
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 1800,
  "userId": 1,
  "username": "testuser"
} }
```

**错误码**
- 400: 验证码错误
- 400: 邮箱未注册

---

### 6. 刷新 Token

**请求**
```
POST /api/v1/auth/refresh
Content-Type: application/json
```

**请求参数**
| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| refreshToken | string | 是 | 刷新令牌 |

**响应示例**
```
{ "code": 0, "message": "success", "data": { "accessToken": "...", "expiresIn": 1800 } }
```

**错误码**
- 401: 刷新令牌无效

---

### 7. 退出登录

**请求**
```
POST /api/v1/auth/logout
Authorization: Bearer {accessToken}
```

**响应示例**
```
{ "code": 0, "message": "success", "data": null }
```

**错误码**
- 401: 未认证

---

### 8. 检查手机号是否存在

**请求**
```
GET /api/v1/auth/check-phone?phone=13800138000
```

**响应示例**
```
{ "code": 0, "message": "success", "data": true }
```

---
### 9. 检查邮箱是否存在

**请求**
```
GET /api/v1/auth/check-email?email=test@example.com
```

**响应示例**
```
{ "code": 0, "message": "success", "data": true }
```

---
### 10. 检查用户名是否存在

**请求**
```
GET /api/v1/auth/check-username?username=testuser
```

**响应示例**
```
{ "code": 0, "message": "success", "data": true }
```

---
### 11. 登录状态校验

**请求**
```
GET /api/v1/user/me
Authorization: Bearer {accessToken}
```

**响应示例**
```
{ "code": 0, "message": "success", "data": { "userId": 1, "username": "testuser", "nickname": "测试用户" } }
```

---

## 三、错误码总览
- 200/0: 成功
- 400: 参数错误/业务逻辑错误
- 401: 未认证/无权限
- 403: 禁止访问
- 404: 资源不存在
- 429: 限流/频率限制
- 500: 服务器错误

---

## 四、开发变更记录
- OpenSpec 版本迭代：v1.0.0（初始合并）
- OpenSpec 版本：1.x（持续追加接口）

---

*文档更新时间：2026-04-03*
