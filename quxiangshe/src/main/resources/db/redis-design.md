# Redis Key设计文档

## Redis Key 设计规范

### 1. 命名格式
```
{前缀}:{模块}:{业务}:{标识}
```

### 2. 各模块Key设计

#### 2.1 认证模块（Auth）

| Key格式 | 类型 | 说明 | 过期时间 |
|---------|------|------|----------|
| `auth:refresh:{userId}` | String | RefreshToken存储 | 7天 |
| `auth:token:{userId}` | String | AccessToken(可选) | 15分钟 |
| `auth:login:{userId}` | String | 登录状态标记 | 30分钟 |

#### 2.2 用户模块（User）

| Key格式 | 类型 | 说明 | 过期时间 |
|---------|------|------|----------|
| `user:profile:{userId}` | Hash | 用户信息缓存 | 30分钟 |
| `user:followers:{userId}` | Set | 粉丝列表 | 1小时 |
| `user:followings:{userId}` | Set | 关注列表 | 1小时 |

#### 2.3 Feed流模块（Feed）

| Key格式 | 类型 | 说明 | 过期时间 |
|---------|------|------|----------|
| `feed:inbox:{userId}` | List | 用户收件箱(推模式) | 7天 |
| `feed:timeline:{userId}` | ZSet | 用户时间线(推模式排序) | 7天 |
| `feed:cache:{userId}` | String | Feed缓存 | 5分钟 |

#### 2.4 动态模块（Post）

| Key格式 | 类型 | 说明 | 过期时间 |
|---------|------|------|----------|
| `post:detail:{postId}` | Hash | 动态详情 | 10分钟 |
| `post:like:{postId}` | Set | 点赞用户集合 | 30分钟 |
| `post:user:{userId}` | List | 用户动态列表 | 30分钟 |
| `post:trending` | ZSet | 热门动态 | 1小时 |

#### 2.5 评论模块（Comment）

| Key格式 | 类型 | 说明 | 过期时间 |
|---------|------|------|----------|
| `comment:queue` | List | 评论异步写入队列 | 持久化 |
| `comment:post:{postId}` | List | 动态评论列表 | 10分钟 |
| `comment:hot:{postId}` | ZSet | 热门评论 | 1小时 |
| `comment:like:{commentId}` | Set | 评论点赞集合 | 30分钟 |

#### 2.6 限流模块（RateLimit）

| Key格式 | 类型 | 说明 | 过期时间 |
|---------|------|------|----------|
| `ratelimit:{userId}:{second}` | ZSet | 滑动窗口限流 | 60秒 |
| `ratelimit:ip:{ip}:{second}` | ZSet | IP限流 | 60秒 |
| `ratelimit:dedup:{userId}:{requestId}` | String | 请求去重 | 5秒 |

#### 2.7 AI摘要模块（AI）

| Key格式 | 类型 | 说明 | 过期时间 |
|---------|------|------|----------|
| `ai:summary:task:{postId}` | String | AI摘要任务状态 | 1小时 |
| `ai:summary:cache:{postId}` | String | 摘要缓存 | 24小时 |

---

## Redis 数据结构使用规范

### String
- 用于简单缓存：Token、状态标记、计数值
- 序列化方式：JSON（对象）、String（简单值）

### Hash
- 用于存储对象：用户信息、动态详情
- 适合频繁访问的聚合数据

### List
- 用于队列：收件箱、评论队列
- 支持LPUSH/LPOP、RPUSH/RPOP操作

### Set
- 用于去重：点赞列表、粉丝列表
- 支持SADD、SISMEMBER、SCARD操作

### ZSet
- 用于排序：时间线、热榜、限流窗口
- 支持ZADD、ZRANGE、ZRANK操作
