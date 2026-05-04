# 错误处理规范

## 概述

本文档定义了后端错误处理的标准规范，确保错误信息的统一性和可追溯性。

## 错误码定义

### 错误码结构

```
{模块码}{错误类型}{序号}
示例：1001001
- 10 - 用户模块
- 01 - 参数错误
- 001 - 序号
```

### 常用错误码

| 错误码 | 说明 |
|-------|------|
| 1001001 | 用户名不能为空 |
| 1001002 | 用户名格式不正确 |
| 1001003 | 密码不能为空 |
| 1001004 | 密码强度不足 |
| 1002001 | 用户不存在 |
| 1002002 | 用户已禁用 |
| 1002003 | 用户名已存在 |
| 2001001 | 订单不存在 |
| 2001002 | 订单状态不允许该操作 |
| 5000001 | 服务器内部错误 |
| 5000002 | 数据库连接失败 |

## 异常类定义

```typescript
// 基础异常类
class BaseException extends Error {
    code: number;
    message: string;
    details?: any;
}

// 参数错误异常
class ValidationException extends BaseException {
    constructor(message: string, details?: any) {
        super();
        this.code = 400;
        this.message = message;
        this.details = details;
    }
}

// 业务异常
class BusinessException extends BaseException {
    constructor(code: number, message: string) {
        super();
        this.code = code;
        this.message = message;
    }
}

// 资源不存在异常
class NotFoundException extends BaseException {
    constructor(message: string = '资源不存在') {
        super();
        this.code = 404;
        this.message = message;
    }
}

// 认证异常
class UnauthorizedException extends BaseException {
    constructor(message: string = '未授权') {
        super();
        this.code = 401;
        this.message = message;
    }
}

// 禁止访问异常
class ForbiddenException extends BaseException {
    constructor(message: string = '禁止访问') {
        super();
        this.code = 403;
        this.message = message;
    }
}
```

## 全局错误处理

```typescript
// 错误处理中间件
async function errorHandler(ctx, next) {
    try {
        await next();
    } catch (err) {
        const error = {
            code: err.code || 500,
            message: err.message || '服务器内部错误',
            details: err.details
        };
        
        // 记录错误日志
        logger.error({
            code: error.code,
            message: error.message,
            stack: err.stack,
            path: ctx.path,
            method: ctx.method
        });
        
        ctx.status = err.code || 500;
        ctx.body = error;
    }
}
```

## 错误响应格式

```json
// 参数验证错误
{
    "code": 400,
    "message": "请求参数错误",
    "errors": [
        {"field": "username", "message": "用户名不能为空"},
        {"field": "email", "message": "邮箱格式不正确"}
    ]
}

// 业务错误
{
    "code": 1002001,
    "message": "用户不存在",
    "details": {
        "userId": 123
    }
}

// 系统错误（生产环境不暴露详细信息）
{
    "code": 5000001,
    "message": "服务器内部错误"
}
```

## 日志记录

```typescript
// 错误日志格式
logger.error({
    level: 'error',
    code: error.code,
    message: error.message,
    path: ctx.path,
    method: ctx.method,
    query: ctx.query,
    body: ctx.request.body,
    userId: ctx.user?.id,
    timestamp: new Date().toISOString()
});
```