# 趣享社 - 接口限流规范

## 一、概述

趣享社社交平台采用 Redis 实现接口限流，使用固定窗口和滑动窗口两种算法。

## 二、限流规则

### 2.1 登录限流（固定窗口）

| 项目 | 说明 |
|-----|------|
| 算法 | 固定窗口限流 |
| 限制 | 同一IP，1分钟内最多允许 5 次登录尝试 |
| 超限提示 | "登录尝试过于频繁，请稍后再试" |
| Redis Key | `rate_limit:fixed:login:{ip}` |

### 2.2 注册限流（滑动窗口）

| 项目 | 说明 |
|-----|------|
| 算法 | 滑动窗口限流（ZSet实现） |
| 限制 | 同一IP，60秒内最多允许 3 次注册请求 |
| 超限提示 | "注册请求过于频繁，请稍后再试" |
| Redis Key | `rate_limit:sliding:register:{ip}` |

## 三、算法说明

### 3.1 固定窗口算法

```
原理：使用Redis INCR计数器，每个窗口期独立计数

优点：实现简单，性能高
缺点：窗口边界可能出现突发流量（2倍限制）
```

### 3.2 滑动窗口算法

```
原理：使用Redis ZSet记录每个请求的时间戳，精确控制时间窗口

优点：精确控制，无边界突发问题
缺点：实现稍复杂，需要维护有序集合
```

## 四、代码实现

### 4.1 核心组件

| 组件 | 说明 |
|-----|------|
| FixedWindowRateLimiter | 固定窗口限流器 |
| SlidingWindowRateLimiter | 滑动窗口限流器 |
| @RateLimit | 限流注解 |
| RateLimitAspect | 限流切面 |

### 4.2 使用示例

```java
// 登录接口 - 固定窗口限流
@RateLimit(
    key = "login",
    maxRequests = 5,
    windowSeconds = 60,
    type = RateLimit.LimitType.FIXED_WINDOW,
    message = "登录尝试过于频繁，请稍后再试"
)
@PostMapping("/login")
public R<LoginVO> login(...) { }

// 注册接口 - 滑动窗口限流
@RateLimit(
    key = "register",
    maxRequests = 3,
    windowSeconds = 60,
    type = RateLimit.LimitType.SLIDING_WINDOW,
    message = "注册请求过于频繁，请稍后再试"
)
@PostMapping("/register")
public R<LoginVO> register(...) { }
```

## 五、Redis Key 设计

```
固定窗口: rate_limit:fixed:{key}
滑动窗口: rate_limit:sliding:{key}

示例:
- 登录限流: rate_limit:fixed:login:192.168.1.1
- 注册限流: rate_limit:sliding:register:192.168.1.1
```

## 六、前端处理

前端通过 axios 拦截器捕获 429 状态码，优雅展示限流提示：

```javascript
// 捕获限流错误
if (error.response && error.response.status === 429) {
  const message = error.response.data?.message || '请求过于频繁，请稍后再试'
  ElMessage.warning(message)
}
```

## 七、异常响应格式

```json
{
  "code": 429,
  "message": "登录尝试过于频繁，请稍后再试",
  "data": null,
  "timestamp": "2024-01-01T12:00:00"
}
```

## 八、注意事项

1. 限流基于客户端IP，需要确保获取到真实IP
2. Redis 服务需要正常运行
3. 限流逻辑不侵入业务代码，使用AOP统一处理
4. 滑动窗口算法更精准，推荐重要接口使用
