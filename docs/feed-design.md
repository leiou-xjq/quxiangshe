# Feed流分发系统设计

## 1. 概述

Feed流分发系统负责将用户发布的笔记高效地分发给关注者。本系统采用**三级分发策略**，根据博主粉丝量自适应选择最优分发模式，平衡分发效率与系统压力。

## 2. 三级分发策略

### 2.1 分级标准

| 粉丝量级 | 分发模式 | 说明 |
|---------|---------|------|
| < 1K | **PUSH（推模式）** | 直接写入粉丝收件箱 |
| 1K ~ 100K | **PULL（拉模式）** | 写入博主发件箱，粉丝拉取时聚合 |
| > 100K | **HYBRID（推拉结合）** | 按粉丝活跃度分段处理 |

### 2.2 PUSH 推模式

**适用场景**：小博主（< 1000 粉丝）

**流程**：
1. 笔记发布时，直接写入所有粉丝的收件箱
2. 粉丝获取 Feed 时直接从收件箱读取

**Redis Key**：
```
feed:inbox:push:{userId}  # 用户的收件箱 ZSet
```

**优点**：读取极快，适合粉丝量少的场景

**缺点**：粉丝量大时写入压力大

### 2.3 PULL 拉模式

**适用场景**：中等博主（1000 ~ 100000 粉丝）

**流程**：
1. 笔记发布时，写入博主发件箱
2. 粉丝获取 Feed 时，查询所有关注对象的发件箱并聚合

**Redis Key**：
```
feed:outbox:pull:{authorId}  # 博主的发件箱 ZSet
```

**优点**：写入压力小，适合中等规模

**缺点**：读取时需要聚合多个发件箱

### 2.4 HYBRID 推拉结合模式

**适用场景**：大V（> 100000 粉丝）

**核心思想**：按粉丝活跃度分级处理，只对活跃粉丝进行推送

**粉丝分级**：

| 等级 | 活跃度分 | 处理方式 |
|------|---------|---------|
| 活跃粉 | ≥ 120 分 | Pipeline 批量 Push 到收件箱 |
| 普通粉 | 20 ~ 120 分 | Pull 模式，拉取发件箱 |
| 僵尸粉 | < 20 分 | 跳过，不分发 |

**活跃度计算公式**：
```
活跃度分 = 近7天登录次数 × 10 + 近7天互动次数 × 5 + 近30天登录次数 × 1
```

**互动行为**：点赞、评论、收藏、转发

## 3. 缓存策略

### 3.1 多级缓存架构

| 层级 | 组件 | 缓存内容 | TTL |
|------|------|---------|-----|
| L1 | Caffeine 本地缓存 | 热点用户 Feed | 5 分钟 |
| L2 | Redis | 收件箱、发件箱、关注列表 | 15 分钟 ~ 24 小时 |
| L3 | MySQL | 持久化存储 | 永久 |

### 3.2 缓存 Key 设计

```
feed:user:{userId}              # 用户 Feed 列表
feed:following:{userId}          # 用户关注列表
feed:follower:count:{userId}     # 粉丝数
feed:inbox:push:{userId}         # PUSH 收件箱
feed:outbox:pull:{authorId}      # PULL 发件箱
author:{authorId}:active_fans    # 大V活跃粉丝 Set
author:{authorId}:normal_fans    # 大V普通粉丝 Set
```

## 4. 一致性保证

### 4.1 失败降级策略

```
笔记发布 → 尝试推送 → 失败?
    ↓是              ↓否
降级为 PULL       完成
写入发件箱
```

### 4.2 缓存预热

- 笔记发布后，主动删除粉丝的 Feed 缓存，触发下次访问时重新加载
- 关注变更时，刷新关注列表和粉丝数缓存

## 5. 分批推送机制

对于大V的 HYBRID 模式，采用分批推送避免 Redis 阻塞：

```java
// 每批处理 1000 粉丝，间隔 500ms
int fansPerBatch = 1000;
int totalBatches = (followerCount + fansPerBatch - 1) / fansPerBatch;

for (int i = 0; i < totalBatches; i++) {
    pushNoteInBatch(noteId, authorId, i, totalBatches);
    Thread.sleep(500);  // 间隔控制
}
```

## 6. 性能指标

| 指标 | 数值 |
|------|------|
| Feed 读取 P99 延迟 | < 50ms |
| 大V发帖 CPU 峰值降低 | 70% |
| 支持粉丝量级 | 10 万+ |

## 7. 相关代码

| 文件 | 说明 |
|------|------|
| `FeedServiceImpl.java` | Feed流服务核心实现 |
| `FeedPushConsumer.java` | MQ 消费者，处理异步推送 |
| `SmartFeedDistributionService.java` | 智能分发策略选择 |
