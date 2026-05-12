# 消息队列（MQ）设计文档

## 1. 概述

本系统使用 RabbitMQ 作为消息队列，实现异步通知、Feed分发、视频转码等功能的解耦。MQ 的引入提高了系统的吞吐量和可靠性，确保耗时操作不阻塞主流程。

## 2. MQ 交换机与队列设计

### 2.1 核心组件

| 组件 | 说明 |
|------|------|
| Exchange | 交换机，负责消息路由 |
| Queue | 队列，存储消息 |
| Routing Key | 路由键，决定消息投递到哪个队列 |

### 2.2 交换机配置

| 交换机名称 | 类型 | 说明 |
|-----------|------|------|
| `notification.exchange` | Direct | 通知交换机 |
| `feed.push.exchange` | Direct | Feed推送交换机 |
| `video.transcode.exchange` | Direct | 视频转码交换机 |
| `email.exchange` | Direct | 邮件交换机 |

### 2.3 队列配置

| 队列名称 | 绑定交换机 | Routing Key | 说明 |
|---------|-----------|-------------|------|
| `notification.queue` | notification.exchange | notification | 通知队列 |
| `feed.push.queue` | feed.push.exchange | feed.push | Feed推送队列 |
| `video.transcode.queue` | video.transcode.exchange | video.transcode | 视频转码队列 |
| `email.queue` | email.exchange | email | 邮件队列 |

## 3. Event 定义

### 3.1 通知消息 (NotificationMessage)

**队列**：`notification.queue`

**字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| type | String | 通知类型 |
| userId | Long | 目标用户ID |
| fromUserId | Long | 触发通知的用户ID |
| noteId | Long | 相关笔记ID |
| commentId | Long | 相关评论ID |
| extra | String | 扩展信息 |
| timestamp | LocalDateTime | 时间戳 |

**通知类型**：

| type | 说明 |
|------|------|
| `LIKE` | 有人点赞了你的笔记 |
| `COMMENT` | 有人评论了你的笔记 |
| `FOLLOW` | 有人关注了你 |
| `REVIEW_PASSED` | 笔记审核通过 |
| `REVIEW_REJECTED` | 笔记审核被拒 |

### 3.2 Feed推送消息 (FeedPushMessage)

**队列**：`feed.push.queue`

**字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| noteId | Long | 笔记ID |
| authorId | Long | 作者ID |
| targetUserIds | List<Long> | 目标用户ID列表 |
| batchNum | int | 当前批次号 |
| totalBatches | int | 总批次数量 |
| pushTime | LocalDateTime | 推送时间 |

### 3.3 视频转码消息 (VideoTranscodeMessage)

**队列**：`video.transcode.queue`

### 3.4 邮件消息 (EmailMessage)

**队列**：`email.queue`

## 4. 消费者设计

### 4.1 NotificationConsumer

**功能**：处理通知消息，发送站内通知

**处理流程**：

```
收到消息 → 解析类型 → 调用对应通知服务 → 发送通知
    ↓失败
重试（最多3次） → 超过则丢弃
```

**幂等保证**：通过 `userId + noteId + type + timestamp` 联合主键防止重复发送

### 4.2 FeedPushConsumer

**功能**：处理Feed分发消息

**处理流程**：

```
收到消息 → 分批获取粉丝 → 写入收件箱 → 记录推送日志
    ↓失败
重试 → 降级为PULL模式
```

### 4.3 VideoTranscodeConsumer

**功能**：异步处理视频转码

### 4.4 EmailConsumer

**功能**：异步发送邮件

## 5. 可靠性保证

### 5.1 消息持久化

- Exchange、Queue、Message 均开启持久化
- 保证 RabbitMQ 重启后消息不丢失

### 5.2 消费者确认机制

```java
@RabbitListener(queues = "notification.queue")
public void consume(NotificationMessage message, Channel channel,
                    @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
    try {
        handleNotification(message);
        channel.basicAck(deliveryTag, false);  // 确认成功
    } catch (Exception e) {
        channel.basicNack(deliveryTag, false, true);  // 失败重试
    }
}
```

### 5.3 重试机制

- 最大重试次数：3次
- 重试策略：指数退避（1s → 2s → 4s）
- 超过最大重试次数：消息进入死信队列或丢弃

```java
// 伪代码：指数退避重试
int retryCount = getRetryCount(message);
if (retryCount < MAX_RETRY) {
    Thread.sleep(1000 * (1 << retryCount));  // 1s, 2s, 4s
    throw e;  // 触发重试
} else {
    // 丢弃或进入死信队列
}
```

## 6. 幂等设计

### 6.1 为什么要幂等

MQ 可能因为网络问题导致消息重复消费，必须保证幂等性。

### 6.2 幂等方案

**方案一：唯一键约束**

```sql
-- 通知表添加唯一键
CREATE UNIQUE INDEX idx_notification_idempotent
ON notification(user_id, type, source_id, created_at);
```

**方案二：Redis 去重**

```java
String key = "notification:idempotent:" + userId + ":" + type + ":" + sourceId;
if (redis.setIfAbsent(key, "1", 24 * 3600)) {
    // 不存在，执行通知逻辑
} else {
    // 已存在，跳过
}
```

## 7. 死信队列

### 7.1 概念

无法正常处理的消息进入死信队列（DLX），便于后续分析和处理。

### 7.2 配置

```yaml
notification.queue:
  x-dead-letter-exchange: notification.dlx.exchange
  x-dead-letter-routing-key: notification.dead
```

## 8. 监控与告警

### 8.1 监控指标

| 指标 | 说明 |
|------|------|
| 队列深度 | 等待消费的消息数 |
| 消费速率 | 消息/秒 |
| 失败率 | 消费失败比例 |
| 积压告警 | 队列深度 > 1000 |

### 8.2 告警方式

- 邮件告警
- 钉钉/飞书 WebHook

## 9. 相关代码

| 文件 | 说明 |
|------|------|
| `NotificationConsumer.java` | 通知消费者 |
| `FeedPushConsumer.java` | Feed推送消费者 |
| `VideoTranscodeConsumer.java` | 视频转码消费者 |
| `EmailConsumer.java` | 邮件消费者 |
| `NotificationMessage.java` | 通知消息体 |
| `FeedPushMessage.java` | Feed推送消息体 |
| `RabbitMQConfig.java` | RabbitMQ 配置 |
