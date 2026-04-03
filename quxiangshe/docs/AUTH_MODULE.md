# 趣享社认证模块技术文档

## 一、模块概述

认证模块（auth）是趣享社系统的核心模块，负责用户注册、登录、令牌管理等认证相关功能。采用Spring Boot + Spring Security + JWT技术栈，支持多种登录方式（密码登录、手机验证码登录、邮箱验证码登录），并实现了基于Redis+Lua的滑动窗口限流和IP黑名单机制。

---

## 二、功能清单

### 2.1 用户注册
- 用户名、密码、手机号、邮箱必填
- 密码加密存储（BCrypt）
- 用户名、手机号、邮箱唯一性校验
- 限流：5次/60秒（IP维度）+ 3次/10分钟（手机号维度）

### 2.2 用户登录
- **密码登录**：用户名/手机号/邮箱 + 密码
- **手机验证码登录**：手机号 + 6位数字验证码
- **邮箱验证码登录**：邮箱 + 6位数字验证码
- 登录成功返回JWT双令牌

### 2.3 验证码服务
- 手机短信验证码（6位数字）
- 邮箱验证码（6位数字）
- 验证码有效期5分钟
- 限流：10次/60秒

### 2.4 令牌管理
- **AccessToken**：30分钟（1800秒）有效期
- **RefreshToken**：7天（604800秒）有效期
- 支持令牌刷新
- 支持登出（清除Redis中的RefreshToken）

### 2.5 限流防护
- 滑动窗口算法（Redis + Lua）
- IP黑名单机制（限流触发后自动加入黑名单10分钟）
- 高可用设计：Redis不可用时自动降级到Caffeine本地限流
- 数据持久化：服务重启后自动恢复限流/黑名单数据
- Redis恢复时自动同步本地数据到Redis

### 2.6 数据持久化
- 限流数据持久化：定时30秒写入Redis，服务重启后恢复
- 黑名单数据持久化：定时30秒写入Redis，服务重启后恢复
- 本地Caffeine缓存兜底：Redis不可用时继续提供限流/黑名单功能

---

## 三、API接口清单

| 接口 | 方法 | 说明 | 限流 |
|------|------|------|------|
| `/api/v1/auth/register` | POST | 用户注册 | 5次/60秒 |
| `/api/v1/auth/login` | POST | 密码登录 | 无 |
| `/api/v1/auth/phone-login` | POST | 手机验证码登录 | 10次/60秒 |
| `/api/v1/auth/email-login` | POST | 邮箱验证码登录 | 10次/60秒 |
| `/api/v1/auth/send-code` | POST | 发送手机验证码 | 10次/60秒 |
| `/api/v1/auth/send-email-code` | POST | 发送邮箱验证码 | 10次/60秒 |
| `/api/v1/auth/check-phone` | GET | 检查手机号是否存在 | - |
| `/api/v1/auth/check-email` | GET | 检查邮箱是否存在 | - |
| `/api/v1/auth/refresh` | POST | 刷新AccessToken | - |
| `/api/v1/auth/logout` | POST | 登出 | - |

---

## 四、技术架构

### 4.1 核心组件

```
auth/
├── controller/
│   └── AuthController.java       # 认证控制器
├── service/
│   ├── AuthService.java          # 认证服务接口
│   └── impl/
│       └── AuthServiceImpl.java # 认证服务实现
├── dto/
│   ├── LoginRequestDTO.java      # 登录请求
│   ├── PhoneLoginRequestDTO.java # 手机登录请求
│   ├── EmailLoginRequestDTO.java # 邮箱登录请求
│   ├── SendCodeRequestDTO.java  # 发送验证码请求
│   ├── SendEmailCodeRequestDTO.java # 发送邮箱验证码请求
│   ├── RegisterRequestDTO.java  # 注册请求
│   ├── RefreshTokenRequestDTO.java # 刷新令牌请求
│   └── LoginResponseDTO.java     # 登录响应
├── entity/
│   └── UserEntity.java          # 用户实体
├── mapper/
│   └── AuthUserMapper.java       # 用户Mapper
├── config/
│   └── SecurityConfig.java       # Spring Security配置
└── filter/
    └── JwtAuthenticationFilter.java # JWT认证过滤器
```

### 4.2 限流组件

```
ratelimit/
├── aspect/
│   ├── RateLimitAspect.java      # 限流AOP切面
│   └── LoginRateLimitAspect.java # 登录限流AOP
└── service/
    └── LoginRateLimitService.java # 登录限流服务

common/
├── annotation/
│   └── RateLimit.java            # 限流注解
├── config/
│   └── RateLimitProperties.java  # 限流配置属性
├── constant/
│   └── RateLimitConstants.java   # 限流常量
├── util/
│   ├── RedisLuaRateLimiter.java  # Redis+Lua限流工具
│   ├── LocalRateLimitCache.java  # Caffeine本地限流缓存（降级兜底）
│   ├── RateLimitPersistenceService.java # 限流数据持久化服务
│   ├── BlacklistUtil.java       # IP黑名单工具
│   ├── LocalBlacklistCache.java # 本地黑名单缓存
│   ├── BlacklistPersistenceService.java # 黑名单持久化服务
│   ├── RedisHealthManager.java  # Redis健康检查与降级管理器
│   ├── JwtUtil.java             # JWT工具类
│   └── RedisUtil.java           # Redis工具类
└── exception/
    ├── RateLimitException.java  # 限流异常
    ├── BusinessException.java   # 业务异常
    └── GlobalExceptionHandler.java # 全局异常处理
```

---

## 五、核心实现

### 5.1 JWT认证流程

```
请求 → JwtAuthenticationFilter → 验证JWT → 设置SecurityContext → Controller
```

- **AccessToken**：包含用户ID、用户名、角色信息、token类型（type: access）
- **RefreshToken**：包含用户ID、token类型（type: refresh）
- RefreshToken存储于Redis，7天后自动过期
- Token验证使用HMAC-SHA256

### 5.2 滑动窗口限流

使用Redis ZSet实现滑动窗口算法，Lua脚本保证原子性：

```lua
-- 1. 删除窗口外无效计数
redis.call('ZREMRANGEBYSCORE', key, '-inf', windowStart)
-- 2. 查询窗口内请求数
local count = redis.call('ZCARD', key)
-- 3. 阈值判断
if count >= limit then return 0 end
-- 4. 计数自增
redis.call('ZADD', key, currentTime, requestId)
-- 5. 设置key过期时间
redis.call('PEXPIRE', key, expireTime)
return 1
```

### 5.3 IP黑名单机制

**执行顺序**：
```
请求进入
    ↓
[第一步] 检查黑名单 → 是 → 返回429
    ↓ 否
[第二步] 滑动窗口限流 → 超限 → 加入黑名单10分钟
    ↓ 通过
[第三步] 正常放行
```

**黑名单Key**：`blacklist:ip:{ip}`
**有效期**：10分钟

### 5.4 高可用设计

当Redis不可用时，系统自动降级：

```
┌─────────────────────────────────────────────────────────┐
│                    Redis健康检查                         │
│  每10秒检查一次，连续3次失败则标记为不可用               │
│  恢复时需连续2次成功才确认为真正恢复                    │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                    降级策略                              │
│  • 限流：降级到Caffeine本地缓存（滑动窗口算法）         │
│  • 黑名单：降级到本地ConcurrentHashMap黑名单           │
│  • 数据双写：同时写Redis和本地缓存                       │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                    数据同步                              │
│  Redis恢复时自动触发回调：                               │
│  • 黑名单：从本地同步到Redis                            │
│  • 限流：清理过期数据，保留最近请求记录                │
└─────────────────────────────────────────────────────────┘
```

**核心组件**：

| 组件 | 职责 |
|------|------|
| RedisHealthManager | Redis健康检查、状态管理、回调通知 |
| LocalRateLimitCache | Caffeine本地限流缓存（降级兜底） |
| RedisLuaRateLimiter | 限流主逻辑（Redis + 本地） |
| BlacklistUtil | 黑名单双写（Redis + 本地） |
| LocalBlacklistCache | 本地黑名单缓存 |
| RateLimitPersistenceService | 限流数据持久化 |
| BlacklistPersistenceService | 黑名单数据持久化 |

### 5.5 数据持久化

系统实现了完整的持久化机制，确保服务重启后限流/黑名单数据不丢失：

```
┌─────────────────────────────────────────────────────────┐
│                    持久化策略                           │
│                                                         │
│  [启动时]                                               │
│  1. 从Redis读取持久化数据                               │
│  2. 恢复限流计数器到Caffeine缓存                        │
│  3. 恢复黑名单到本地缓存                                │
│                                                         │
│  [运行时]                                               │
│  1. 限流/黑名单操作同时写Redis和本地缓存               │
│  2. 每30秒定时持久化本地数据到Redis                    │
│                                                         │
│  [Redis恢复时]                                          │
│  1. 将本地活跃数据同步到Redis                          │
│  2. 清理过期数据                                       │
└─────────────────────────────────────────────────────────┘
```

**持久化Key**：
- 限流数据：`ratelimit:persistence:data`
- 黑名单数据：`blacklist:persistence:data`

### 5.5 限流注解使用

```java
@RateLimit(keyPrefix = "limit:captcha:", limit = 10, windowMs = 60000, message = "验证码获取过于频繁，请稍后再试")
@PostMapping("/send-code")
public Response<String> sendVerifyCode(...) {
    // ...
}
```

**注解参数**：

| 参数 | 说明 | 默认值 |
|------|------|--------|
| keyPrefix | 限流key前缀 | - |
| limit | 限流阈值 | 10 |
| windowMs | 窗口时间（毫秒） | 60000 |
| message | 限流提示消息 | "请求过于频繁" |
| type | 限流类型（IP/PHONE/IP_AND_INTERFACE） | IP |

---

## 六、数据库表设计

### user表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 用户ID（主键） |
| username | VARCHAR(50) | 用户名（唯一） |
| phone | VARCHAR(20) | 手机号（唯一） |
| email | VARCHAR(100) | 邮箱（唯一） |
| password_hash | VARCHAR(255) | 密码哈希 |
| nickname | VARCHAR(50) | 昵称 |
| avatar_url | VARCHAR(500) | 头像URL |
| bio | VARCHAR(500) | 个人简介 |
| status | TINYINT | 状态：0-禁用 1-正常 |
| last_login_time | DATETIME | 最后登录时间 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

---

## 七、安全机制

### 7.1 密码安全
- 使用BCryptPasswordEncoder加密
- 不存储明文密码
- 密码验证使用BCryptmatches方法

### 7.2 令牌安全
- AccessToken短期有效（30分钟）
- RefreshToken长期有效（7天），存储于Redis
- 登出时清除Redis中的RefreshToken
- Token类型标识（access/refresh）

### 7.3 限流防护
- 验证码接口：防止薅羊毛（10次/60秒）
- 注册接口：防止批量注册垃圾账号（5次/60秒）
- IP黑名单：限流触发后自动加入，10分钟内禁止访问

### 7.4 高可用策略
- Redis健康检查：每10秒检测，连续3次失败触发降级
- 本地缓存备份：黑名单和限流数据双写
- 自动数据同步：Redis恢复时自动同步本地数据
- 连续成功确认：需连续2次成功才确认Redis真正恢复

---

## 八、返回格式

### 成功响应
```json
{
  "code": 0,
  "data": {},
  "message": "success"
}
```

### 限流响应
```json
{
  "code": 429,
  "data": null,
  "message": "验证码获取过于频繁，请稍后再试"
}
```

### 黑名单拦截响应
```json
{
  "code": 429,
  "data": null,
  "message": "您的请求过于频繁，已被限制10分钟"
}
```

---

## 九、配置说明

### application.yml

```yaml
# JWT配置
jwt:
  secret: quxiangshe-secret-key-change-in-production-2024
  access-token-validity: 1800    # 30分钟
  refresh-token-validity: 604800  # 7天

# 登录限流配置
login-ratelimit:
  window-size: 1        # 滑动窗口大小（秒）
  max-request: 1000     # 每秒最大登录请求数

# 限流模块配置
ratelimit:
  config:
    # 是否启用本地降级限流（Redis不可用时使用Caffeine兜底）
    enable-local-fallback: true
    # 本地缓存过期时间（分钟）
    local-cache-expire-minutes: 10
    # 本地缓存最大条目数
    local-cache-max-size: 10000
    # 是否启用Redis持久化（服务重启后恢复数据）
    enable-persistence: true
    # 持久化间隔（秒）
    persistence-interval-seconds: 30
    # Redis恢复时是否同步数据
    sync-on-redis-recover: true
    # 持久化Key前缀
    persistence-key-prefix: "ratelimit:persistence:"
    # 持久化批量大小
    persistence-batch-size: 100
```

### 限流配置类（RateLimitProperties.java）

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| enableLocalFallback | 是否启用本地降级限流 | true |
| localCacheExpireMinutes | 本地缓存过期时间（分钟） | 10 |
| localCacheMaxSize | 本地缓存最大条目数 | 10000 |
| enablePersistence | 是否启用Redis持久化 | true |
| persistenceIntervalSeconds | 持久化间隔（秒） | 30 |
| syncOnRedisRecover | Redis恢复时是否同步数据 | true |
| persistenceKeyPrefix | 持久化Key前缀 | ratelimit:persistence: |
| persistenceBatchSize | 持久化批量大小 | 100 |

### 限流常量（RateLimitConstants.java）

```java
// 验证码接口限流配置
CAPTCHA_DEFAULT_LIMIT = 10          // 默认阈值
CAPTCHA_DEFAULT_WINDOW_MS = 60000   // 60秒窗口

// 注册接口限流配置
REGISTER_DEFAULT_LIMIT = 5          // 默认阈值
REGISTER_DEFAULT_WINDOW_MS = 60000  // 60秒窗口

// 单手机号注册限流配置
REGISTER_PHONE_DEFAULT_LIMIT = 3    // 默认阈值
REGISTER_PHONE_WINDOW_MS = 600000   // 10分钟窗口

// 黑名单配置
BLACKLIST_DURATION_MINUTES = 10      // 10分钟有效期

// Caffeine本地缓存配置
LOCAL_CACHE_MAX_SIZE = 10000         // 最大条目数
LOCAL_CACHE_EXPIRE_MINUTES = 10      // 过期时间
```

---

## 十、前端集成

### 10.1 请求拦截器

前端通过axios拦截器实现Token自动刷新：

```javascript
// 401响应时使用RefreshToken换取新AccessToken
if (response.status === 401) {
  const newToken = await refreshToken();
  if (newToken) {
    // 重发原请求
  } else {
    // 跳转登录页
  }
}
```

### 10.2 登录页面

| 页面 | 路由 | 功能 |
|------|------|------|
| Login.vue | /login | 密码登录 |
| PhoneLogin.vue | /phone-login | 手机验证码登录（60秒倒计时） |
| EmailLogin.vue | /email-login | 邮箱验证码登录（60秒倒计时） |
| Register.vue | /register | 用户注册 |

---

## 十一、压测与排查

### 11.1 压测要点

1. JMeter设置 `Content-Type: application/json`
2. 使用Body Data方式发送JSON请求
3. 测试前清理Redis：`redis-cli FLUSHDB`
4. 白名单已禁用，本地测试会触发限流

### 11.2 常见问题

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| 100%异常 | Content-Type错误 | 设置为application/json |
| 无法触发限流 | IP在白名单 | 检查WHITE_LIST_IPS配置 |
| 限流不生效 | Redis未启动 | 检查Redis服务 |
| Key不存在 | 限流未执行 | 检查注解是否添加 |
| 本地限流触发 | Redis不可用 | 检查Redis连接状态 |

### 11.3 查看限流数据

```bash
# 查看限流key
redis-cli KEYS "limit:*"

# 查看黑名单key
redis-cli KEYS "blacklist:ip:*"

# 查看key详情
redis-cli ZRANGE "limit:captcha:127.0.0.1" 0 -1 WITHSCORES

# 查看Redis状态
redis-cli PING
```

### 11.4 监控与持久化状态

```java
// 获取本地黑名单数量
blacklistUtil.getLocalCacheSize()

// 获取本地限流缓存key数量
redisLuaRateLimiter.getLocalCacheSize()

// 获取Redis健康状态
redisHealthManager.getStatus()

// 手动触发健康检查
redisHealthManager.triggerHealthCheck()

// 获取限流器状态
redisLuaRateLimiter.getStatus()

// 获取限流持久化状态
rateLimitPersistenceService.getStatus()

// 获取黑名单持久化状态
blacklistPersistenceService.getStatus()

// 手动触发限流数据持久化
rateLimitPersistenceService.triggerPersist()

// 手动触发限流数据恢复
rateLimitPersistenceService.triggerRecover()
```

### 11.5 Redis持久化数据查看

```bash
# 查看限流持久化数据
redis-cli GET "ratelimit:persistence:data"

# 查看黑名单持久化数据
redis-cli GET "blacklist:persistence:data"

# 查看黑名单key
redis-cli KEYS "blacklist:ip:*"

# 查看限流key
redis-cli KEYS "limit:*"
```

---

## 十二、升级日志

| 日期 | 版本 | 变更说明 |
|------|------|----------|
| 2026-04-02 | 1.0 | 初始版本，支持密码/手机/邮箱登录 |
| 2026-04-03 | 1.1 | 新增Redis+Lua滑动窗口限流 |
| 2026-04-03 | 1.2 | 新增IP黑名单机制 |
| 2026-04-03 | 1.3 | 新增Redis降级方案（本地缓存） |
| 2026-04-03 | 1.4 | 新增Redis恢复时数据同步机制 |
| 2026-04-03 | 1.5 | 新增Caffeine本地限流缓存（降级兜底） |
| 2026-04-03 | 1.6 | 新增限流/黑名单数据Redis持久化（服务重启恢复） |

---

## 十三、相关文件路径

### 后端
- 项目根目录：`D:\quxiangshe\quxiangshe`
- 源码目录：`src/main/java/com/quxiangshe`
- 配置文件：`src/main/resources/application.yml`
- Lua脚本：`src/main/resources/lua/sliding_window_rate_limit.lua`

### 前端
- 项目目录：`D:\quxiangshe\quxiangshe\quxiangshe-web`
- 源码目录：`quxiangshe-web/src`
- 认证页面：`quxiangshe-web/src/views/auth/`
- 路由配置：`quxiangshe-web/src/router/index.js`
- 状态管理：`quxiangshe-web/src/store/user.js`
- 请求拦截：`quxiangshe-web/src/utils/request.js`

---

*文档更新时间：2026-04-03*
*版本：1.6*
