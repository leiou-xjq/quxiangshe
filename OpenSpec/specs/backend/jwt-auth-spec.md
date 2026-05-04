# 趣享社 - JWT 双令牌认证系统规范

## 一、概述

趣享社社交平台采用 JWT 双令牌机制实现用户认证，采用 Redis 存储 refreshToken 实现安全的令牌刷新机制。

## 二、双令牌机制

### 2.1 令牌类型

| 令牌类型 | 存储位置 | 有效期 | 用途 |
|---------|---------|-------|------|
| accessToken | 前端 localStorage | 15分钟 | 接口鉴权 |
| refreshToken | Redis | 7天 | 刷新 accessToken |

### 2.2 认证流程

```
┌─────────────────────────────────────────────────────────────────┐
│                        用户登录流程                              │
├─────────────────────────────────────────────────────────────────┤
│  1. 用户提交登录凭证（用户名/密码）                              │
│  2. 后端校验成功后生成:                                          │
│     - accessToken (15分钟有效)                                  │
│     - refreshToken (7天有效)                                     │
│  3. 前端存储:                                                    │
│     - accessToken → localStorage                                 │
│     - refreshToken → localStorage                                │
│  4. 后端将 refreshToken 存储到 Redis，key: refresh_token:{userId}│
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                       Token刷新流程                              │
├─────────────────────────────────────────────────────────────────┤
│  1. 前端发起请求，携带 accessToken                              │
│  2. 后端返回 401，提示 Token 过期                               │
│  3. 前端使用 refreshToken 调用刷新接口                          │
│  4. 后端验证:                                                    │
│     - JWT 中 refreshToken 是否有效                              │
│     - Redis 中 refreshToken 是否存在且匹配                      │
│  5. 校验通过后生成新的 accessToken + refreshToken              │
│  6. 前端更新 localStorage，继续重试请求                         │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                       退出登录流程                              │
├─────────────────────────────────────────────────────────────────┤
│  1. 前端调用 /api/auth/logout 接口                             │
│  2. 后端删除 Redis 中的 refreshToken                           │
│  3. 前端清除 localStorage 中的所有 Token                       │
└─────────────────────────────────────────────────────────────────┘
```

## 三、后端实现

### 3.1 Redis 配置

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
```

### 3.2 JWT 配置

```yaml
jwt:
  secret: quxiangshe-jwt-secret-key-2024-very-long-and-secure
  access-token-validity: 900      # 15分钟 = 900秒
  refresh-token-validity: 604800  # 7天 = 604800秒
  token-prefix: "Bearer "
  header-name: Authorization
```

### 3.3 核心服务

- **IRedisTokenService**: Redis 令牌服务，管理 refreshToken 存储
- **AuthServiceImpl**: 认证服务，实现双令牌生成与验证
- **JwtAuthenticationFilter**: JWT 认证过滤器，校验 accessToken

### 3.4 接口列表

| 接口 | 方法 | 说明 |
|-----|------|------|
| /api/auth/login | POST | 用户登录，返回双令牌 |
| /api/auth/register | POST | 用户注册 |
| /api/auth/logout | POST | 退出登录，删除 Redis 中的 refreshToken |
| /api/auth/refresh | POST | 刷新 Token |

## 四、前端实现

### 4.1 存储策略

- **accessToken**: 存储在 `localStorage`，用于每次请求携带
- **refreshToken**: 存储在 `localStorage`，用于 Token 过期时刷新

### 4.2 Axios 拦截器

**请求拦截器**: 自动在请求头添加 `Authorization: Bearer {accessToken}`

**响应拦截器**:
- 捕获 401 错误
- 检查是否存在 refreshToken
- 调用刷新接口获取新令牌
- 重试失败的请求

### 4.3 刷新队列机制

当多个请求同时触发 Token 刷新时，使用队列机制确保只发起一次刷新请求：

```javascript
let isRefreshing = false
let requests = []
```

## 五、数据库设计

### 5.1 核心表

| 表名 | 说明 |
|-----|------|
| user | 用户表 |
| note | 笔记主表 |
| note_comment | 笔记评论表 |
| note_like | 笔记点赞表 |
| note_favorite | 笔记收藏表 |
| sensitive_word | 敏感词库表 |

### 5.2 Redis Key 设计

```
refresh_token:{userId} -> refreshToken 值，7天过期
```

## 六、安全性

1. **refreshToken 存储在 Redis**: 即使前端 localStorage 被攻击者获取，也无法单独使用 refreshToken
2. **双重验证**: 刷新时同时验证 JWT 和 Redis 中的 refreshToken
3. **短有效期 accessToken**: 15分钟过期，减少 Token 泄露的风险
4. **退出登录清除**: 退出时立即删除 Redis 中的 refreshToken

## 七、注意事项

1. 确保 Redis 服务正常运行
2. 前端需要处理 Token 刷新失败的情况
3. 退出登录时前后端配合清除 Token
4. 敏感操作建议使用短期有效的 accessToken
