# 多级缓存架构设计

## 1. 概述

本系统采用 **L0 ~ L3 四级缓存架构**，从本地缓存到分布式缓存再到持久化存储，层层递进，平衡性能与一致性。

## 2. 缓存层级

```
┌─────────────────────────────────────────────────┐
│                   请求入口                         │
└─────────────────────┬───────────────────────────┘
                      ↓
┌─────────────────────────────────────────────────┐
│  L0: Spring Cache 注解（方法级）                  │
│  - @Cacheable、@CacheEvict                      │
└─────────────────────┬───────────────────────────┘
                      ↓
┌─────────────────────────────────────────────────┐
│  L1: Caffeine 本地缓存（进程内）                   │
│  - 热点 Feed 数据（1000 条/5 分钟）               │
│  - 用户基本信息（500 条/10 分钟）                  │
└─────────────────────┬───────────────────────────┘
                      ↓
┌─────────────────────────────────────────────────┐
│  L2: Redis 分布式缓存                             │
│  - 关注列表、粉丝数                              │
│  - Feed 发件箱/收件箱                           │
│  - 热点笔记 ZSet                                │
└─────────────────────┬───────────────────────────┘
                      ↓
┌─────────────────────────────────────────────────┐
│  L3: MySQL 持久化存储                            │
│  - 所有业务数据                                  │
└─────────────────────────────────────────────────┘
```

## 3. 各层级详解

### 3.1 L0: Spring Cache 注解

**作用**：方法级缓存声明，通过注解定义缓存行为

**常用注解**：
```java
@Cacheable(value = "users", key = "#userId")
User getUser(Long userId);

@CacheEvict(value = "users", key = "#userId")
void updateUser(User user);
```

### 3.2 L1: Caffeine 本地缓存

**特点**：进程内缓存，访问速度极快，无网络开销

**配置**：
| 缓存类型 | 最大容量 | 过期时间 |
|---------|---------|---------|
| 热点 Feed | 1000 条 | 5 分钟 |
| 用户信息 | 500 条 | 10 分钟 |

**适用场景**：
- 高频读取的数据
- 允许一定时间不一致的数据

### 3.3 L2: Redis 分布式缓存

**特点**：多实例共享，保证分布式一致性

**核心 Key**：

| Key Pattern | 用途 | TTL |
|-------------|------|-----|
| `feed:following:{userId}` | 用户关注列表 | 24 小时 |
| `feed:follower:count:{userId}` | 粉丝数 | 6 小时 |
| `feed:user:{userId}` | 用户 Feed | 15 分钟 |
| `feed:inbox:push:{userId}` | PUSH 收件箱 | 7 天 |
| `feed:outbox:pull:{authorId}` | PULL 发件箱 | 24 小时 |
| `note:hot` | 热点笔记 ZSet | 7 天 |
| `post:{id}:comment_tree` | 评论树 JSON | 7 天 |

### 3.4 L3: MySQL 持久化

**作用**：最终数据持久化存储

## 4. 一致性保证

### 4.1 Canal binlog 同步

通过 Canal 监听 MySQL binlog，实现 Redis 与 MySQL 的准实时同步：

```
MySQL → Canal → Redis
   ↓
数据变更 → binlog → Canal 解析 → 更新 Redis 缓存
```

### 4.2 主动更新 + 被动过期

**主动更新**：
- 数据变更时，先更新 MySQL，再删除 Redis 缓存
- 下次访问时重新从 MySQL 加载

**被动过期**：
- 设置合理的 TTL，自动淘汰过期数据

### 4.3 缓存预热

**场景**：系统重启或缓存失效后

**策略**：
- 热点数据提前加载到缓存
- 启动时主动查询高频访问数据

## 5. 缓存击穿处理

### 5.1 问题描述

热点数据过期瞬间，大量请求直接打到数据库

### 5.2 解决方案

| 方案 | 说明 |
|------|------|
| 互斥锁 | 只有一个请求去加载数据库，其他等待 |
| 永不过期 | 热点数据不设置过期时间，定时更新 |
| 逻辑过期 | 设置逻辑过期时间，快过期时异步更新 |

**本系统采用**：互斥锁 + 逻辑过期结合

```java
// 伪代码示例
public User getUser(Long userId) {
    User user = caffeineCache.get(userId);
    if (user != null) {
        return user;
    }

    // 加锁，防止击穿
    synchronized (lock) {
        user = caffeineCache.get(userId);
        if (user != null) {
            return user;
        }

        // 从数据库加载
        user = userMapper.selectById(userId);
        caffeineCache.put(userId, user);
        return user;
    }
}
```

## 6. 分布式锁

### 6.1 场景

计数器操作（点赞、评论、收藏）需要保证并发安全

### 6.2 Redisson 分布式锁

```java
RLock lock = redissonClient.getLock("like:note:" + noteId);
try {
    lock.lock();
    // 执行点赞逻辑
    noteMapper.incrLikeCount(noteId);
} finally {
    lock.unlock();
}
```

## 7. 对账机制

### 7.1 定时对账

每 5 分钟执行一次 Redis 与 MySQL 计数器对账：

```java
// 对账任务
for (Note note : notes) {
    long dbLikeCount = note.getLikeCount();
    long redisLikeCount = getRedisLikeCount(note.getId());

    if (dbLikeCount != redisLikeCount) {
        // 数据不一致，以 Redis 为准，更新 MySQL
        noteMapper.updateLikeCount(note.getId(), redisLikeCount);
    }
}
```

### 7.2 对账范围

- 点赞数 `like_count`
- 评论数 `comment_count`
- 收藏数 `favorite_count`
- 浏览数 `view_count`
- 转发数 `forward_count`

## 8. 性能指标

| 缓存层级 | 命中率 | 延迟 |
|---------|-------|------|
| L1 Caffeine | > 80% | < 1ms |
| L2 Redis | > 90% | < 5ms |
| L3 MySQL | - | > 10ms |

**热点读取**：50ms+ → < 2ms

## 9. 相关代码

| 文件 | 说明 |
|------|------|
| `FeedServiceImpl.java` | Feed 多级缓存实现 |
| `RedisTokenServiceImpl.java` | Token 缓存 |
| `RedissonConfig.java` | Redisson 配置 |
| `RateLimitAspect.java` | 限流切面 |
