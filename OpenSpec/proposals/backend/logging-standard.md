# 日志规范

## 概述

本文档定义了日志记录的标准规范，确保日志的可读性、可搜索性和性能。

## 日志级别

| 级别 | 说明 | 使用场景 |
|-----|------|---------|
| DEBUG | 调试信息 | 开发环境调试信息 |
| INFO | 一般信息 | 正常运行日志 |
| WARN | 警告信息 | 可恢复的异常 |
| ERROR | 错误信息 | 程序错误 |
| FATAL | 致命错误 | 系统崩溃 |

## 日志格式

### 结构化日志

```json
{
    "timestamp": "2024-01-01T12:00:00.000+08:00",
    "level": "INFO",
    "logger": "UserService",
    "message": "用户登录成功",
    "context": {
        "userId": 123,
        "username": "admin",
        "ip": "192.168.1.1",
        "userAgent": "Mozilla/5.0..."
    }
}
```

### 日志字段说明

| 字段 | 说明 |
-----|------|
| timestamp | 日志时间 |
| level | 日志级别 |
| logger | 日志记录器名称 |
| message | 日志消息 |
| context | 上下文信息 |

## 记录规范

### 关键操作日志

```typescript
// 用户登录
logger.info({
    message: '用户登录',
    context: {
        userId: user.id,
        username: user.username,
        ip: ctx.ip,
        result: 'success'
    }
});

// 订单创建
logger.info({
    message: '订单创建成功',
    context: {
        orderId: order.id,
        userId: user.id,
        amount: order.totalAmount
    }
});
```

### 错误日志

```typescript
// 业务异常
logger.error({
    message: '业务处理失败',
    context: {
        userId: user.id,
        orderId: order.id,
        errorCode: 'ORDER_CREATE_FAILED',
        stack: err.stack
    }
});

// 系统异常
logger.error({
    message: '系统异常',
    context: {
        path: ctx.path,
        method: ctx.method,
        error: err.message,
        stack: err.stack
    }
});
```

### 性能日志

```typescript
// 接口性能
logger.info({
    message: 'API响应时间',
    context: {
        path: '/api/users',
        method: 'GET',
        duration: 125, // 毫秒
        status: 200
    }
});

// 慢查询日志
logger.warn({
    message: '慢查询',
    context: {
        sql: 'SELECT * FROM orders WHERE ...',
        duration: 3000, // 毫秒
        explain: 'Using filesort'
    }
});
```

## 日志存储

### 存储策略

| 环境 | 存储方式 |
-----|---------|
| 开发环境 | 控制台输出 |
| 测试环境 | 文件存储 |
| 生产环境 | 日志服务（ELK/Loki） |

### 日志轮转

```yaml
# logback.xml 示例
<appender name="rolling" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/app.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <fileNamePattern>logs/app-%d{yyyy-MM-dd}.log</fileNamePattern>
        <maxHistory>30</maxHistory>
    </rollingPolicy>
</appender>
```

## 敏感信息处理

- 禁止记录密码、Token等敏感信息
- 对敏感字段进行脱敏处理
- 使用日志脱敏组件