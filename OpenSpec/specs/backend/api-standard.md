# RESTful API 设计规范

## 文档信息

| 项目 | 内容 |
|-----|------|
| 版本 | v1.0.0 |
| 状态 | 已发布 |
| 更新日期 | 2024-01-01 |

## 1. 范围

本规范定义了RESTful API的设计标准，适用于后端服务接口设计。

## 2. URL 设计

### 2.1 资源命名

- 使用复数形式
- 使用下划线分隔
- 全部小写

```http
GET    /api/v1/users
GET    /api/v1/orders
GET    /api/v1/product-categories
```

### 2.2 HTTP 方法

| 方法 | 说明 | 示例 |
|-----|------|------|
| GET | 获取资源 | GET /users |
| POST | 创建资源 | POST /users |
| PUT | 完整更新 | PUT /users/123 |
| PATCH | 部分更新 | PATCH /users/123 |
| DELETE | 删除资源 | DELETE /users/123 |

### 2.3 嵌套资源

```http
GET    /users/123/orders          -- 获取用户123的订单
POST   /users/123/orders          -- 为用户123创建订单
GET    /orders/456/items          -- 获取订单456的明细
```

## 3. 请求规范

### 3.1 请求头

```http
Content-Type: application/json
Accept: application/json
Authorization: Bearer {token}
X-Request-ID: {uuid}
X-Language: zh-CN
```

### 3.2 查询参数

```http
# 分页
GET /users?page=1&page_size=20

# 排序
GET /users?sort=created_at,desc&sort=username,asc

# 过滤
GET /users?status=active&role=admin

# 搜索
GET /users?q=keyword
```

## 4. 响应规范

### 4.1 成功响应

```json
{
    "code": 0,
    "message": "success",
    "data": {},
    "timestamp": "2024-01-01T00:00:00Z"
}
```

### 4.2 列表响应

```json
{
    "code": 0,
    "message": "success",
    "data": [],
    "pagination": {
        "page": 1,
        "page_size": 20,
        "total": 100,
        "total_pages": 5
    },
    "timestamp": "2024-01-01T00:00:00Z"
}
```

### 4.3 错误响应

```json
{
    "code": 400,
    "message": "请求参数错误",
    "errors": [
        {"field": "email", "message": "邮箱格式不正确"}
    ],
    "timestamp": "2024-01-01T00:00:00Z"
}
```

### 4.4 HTTP 状态码

| 状态码 | 说明 |
-------|------|
| 200 | 成功 |
| 201 | 创建成功 |
| 204 | 删除成功（无返回体） |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 422 | 业务逻辑错误 |
| 500 | 服务器错误 |

## 5. 版本控制

### 5.1 URL 版本

```http
/api/v1/users
/api/v2/users
```

### 5.2 Header 版本

```http
Accept: application/vnd.myapp.v1+json
```

## 6. 错误码规范

### 6.1 错误码结构

```
{模块码}{错误类型}{序号}
示例：1001001
- 10 - 用户模块
- 01 - 参数错误
- 001 - 序号
```

### 6.2 通用错误码

| 错误码 | 说明 |
-------|------|
| 4000001 | 请求参数错误 |
| 4000002 | 请求体解析失败 |
| 4010001 | Token无效 |
| 4010002 | Token过期 |
| 4030001 | 无权限 |
| 4040001 | 资源不存在 |
| 5000001 | 服务器内部错误 |

## 7. 安全规范

- 所有接口需认证（除公开接口外）
- 使用HTTPS
- 实现请求频率限制
- 敏感数据脱敏