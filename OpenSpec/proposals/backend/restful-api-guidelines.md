# RESTful API 设计规范

## 概述

本文档定义了RESTful API的设计规范，确保API的一致性、可读性和易用性。

## URL 设计

### 基本规则

```
GET    /resources          -- 获取资源列表
GET    /resources/:id      -- 获取单个资源
POST   /resources          -- 创建资源
PUT    /resources/:id      -- 完整更新资源
PATCH  /resources/:id      -- 部分更新资源
DELETE /resources/:id      -- 删除资源
```

### 命名规范

```bash
# 使用复数形式
/users          # 用户列表
/orders         # 订单列表
/products       # 产品列表

# 使用下划线分隔
user_comments   # 用户评论
order_items     # 订单明细

# 避免嵌套过深（建议不超过2层）
/users/123/orders        # 用户123的订单
/users/123/orders/456    # 用户123的订单456
```

### 示例

```http
GET    /api/v1/users              # 获取用户列表
GET    /api/v1/users/123          # 获取用户123
POST   /api/v1/users              # 创建用户
PUT    /api/v1/users/123          # 更新用户123
DELETE /api/v1/users/123          # 删除用户123

# 资源关联
GET    /api/v1/users/123/orders   # 获取用户123的订单
POST   /api/v1/users/123/orders   # 为用户123创建订单
```

## 请求规范

### 请求头

```http
Content-Type: application/json
Accept: application/json
Authorization: Bearer {token}
Language: zh-CN
```

### 请求参数

```http
# 分页参数
GET /users?page=1&page_size=20

# 排序参数
GET /users?sort=created_at,desc&sort=name,asc

# 过滤参数
GET /users?status=active&role=admin

# 搜索参数
GET /users?q=keyword
```

## 响应规范

### 成功响应

```json
// 单个资源
{
    "code": 0,
    "message": "success",
    "data": {
        "id": 123,
        "username": "admin",
        "email": "admin@example.com"
    }
}

// 资源列表
{
    "code": 0,
    "message": "success",
    "data": [
        {"id": 1, "name": "User A"},
        {"id": 2, "name": "User B"}
    ],
    "pagination": {
        "page": 1,
        "page_size": 20,
        "total": 100,
        "total_pages": 5
    }
}

// 创建成功（201 Created）
{
    "code": 0,
    "message": "创建成功",
    "data": {
        "id": 456,
        "created_at": "2024-01-01T00:00:00Z"
    }
}
```

### 错误响应

```json
// 通用错误（400 Bad Request）
{
    "code": 400,
    "message": "请求参数错误",
    "errors": [
        {"field": "email", "message": "邮箱格式不正确"}
    ]
}

// 401 Unauthorized
{
    "code": 401,
    "message": "未授权或登录已过期"
}

// 403 Forbidden
{
    "code": 403,
    "message": "无权访问该资源"
}

// 404 Not Found
{
    "code": 404,
    "message": "资源不存在"
}

// 500 Internal Server Error
{
    "code": 500,
    "message": "服务器内部错误"
}
```

### 响应码规范

| 状态码 | 说明 |
|-------|------|
| 0 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 422 | 业务逻辑错误 |
| 500 | 服务器错误 |

## 版本控制

```http
# URL 中包含版本
/api/v1/users
/api/v2/users

# 或使用 Header
Accept: application/vnd.example.v1+json
```

## 安全规范

- 所有接口需认证访问（除公开接口外）
- 使用HTTPS加密传输
- 实施请求频率限制
- 敏感数据脱敏处理