# RESTful接口清单

## 认证模块 (Auth)

### 1. 用户注册

| 项目 | 内容 |
|------|------|
| **接口路径** | `/api/v1/auth/register` |
| **请求方式** | POST |
| **认证要求** | 无需认证 |
| **Content-Type** | `application/json` |

**请求参数**
```json
{
  "username": "string(必填, 4-20字符)",
  "password": "string(必填, 6-20字符)",
  "phone": "string(可选)",
  "email": "string(可选)",
  "nickname": "string(可选)"
}
```

**响应示例**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "userId": 123456789,
    "username": "user001"
  }
}
```

---

### 2. 用户登录

| 项目 | 内容 |
|------|------|
| **接口路径** | `/api/v1/auth/login` |
| **请求方式** | POST |
| **认证要求** | 无需认证 |
| **Content-Type** | `application/json` |

**请求参数**
```json
{
  "username": "string(必填, 用户名/手机号/邮箱)",
  "password": "string(必填)"
}
```

**响应示例**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 900
  }
}
```

---

### 3. 令牌刷新

| 项目 | 内容 |
|------|------|
| **接口路径** | `/api/v1/auth/refresh` |
| **请求方式** | POST |
| **认证要求** | 无需认证 |
| **Content-Type** | `application/json` |

**请求参数**
```json
{
  "refreshToken": "string(必填)"
}
```

**响应示例**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 900
  }
}
```

---

### 4. 登出

| 项目 | 内容 |
|------|------|
| **接口路径** | `/api/v1/auth/logout` |
| **请求方式** | POST |
| **认证要求** | AccessToken |
| **Content-Type** | `application/json` |

**请求参数**
无

**响应示例**
```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

---

## 用户模块 (User)

### 5. 获取当前用户信息

| 项目 | 内容 |
|------|------|
| **接口路径** | `/api/v1/user/me` |
| **请求方式** | GET |
| **认证要求** | AccessToken |

**响应示例**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "userId": 123456789,
    "username": "user001",
    "nickname": "昵称",
    "avatarUrl": "https://...",
    "bio": "个人简介",
    "followCount": 100,
    "followerCount": 200,
    "postCount": 50
  }
}
```

---

### 6. 获取用户主页信息

| 项目 | 内容 |
|------|------|
| **接口路径** | `/api/v1/user/{userId}/profile` |
| **请求方式** | GET |
| **认证要求** | 无需认证 |

**路径参数**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID |

**响应示例**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "userId": 123456789,
    "username": "user001",
    "nickname": "昵称",
    "avatarUrl": "https://...",
    "bio": "个人简介",
    "followCount": 100,
    "followerCount": 200,
    "postCount": 50,
    "isFollowing": true,
    "isFollowed": false
  }
}
```

---

### 7. 关注用户

| 项目 | 内容 |
|------|------|
| **接口路径** | `/api/v1/user/follow/{userId}` |
| **请求方式** | POST |
| **认证要求** | AccessToken |

**路径参数**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 被关注用户ID |

**响应示例**
```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

---

### 8. 取消关注

| 项目 | 内容 |
|------|------|
| **接口路径** | `/api/v1/user/follow/{userId}` |
| **请求方式** | DELETE |
| **认证要求** | AccessToken |

**路径参数**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 取消关注用户ID |

**响应示例**
```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

---

## 动态模块 (Post)

### 9. 发布动态

| 项目 | 内容 |
|------|------|
| **接口路径** | `/api/v1/posts` |
| **请求方式** | POST |
| **认证要求** | AccessToken |
| **Content-Type** | `application/json` |

**请求参数**
```json
{
  "content": "string(必填, 1-2000字符)",
  "mediaUrls": ["string(可选, 图片/视频URL)"],
  "mediaTypes": ["image"(可选)],
  "visibility": 0
}
```

**visibility字段说明**
| 值 | 说明 |
|----|------|
| 0 | 公开 |
| 1 | 仅粉丝可见 |
| 2 | 仅自己可见 |

**响应示例**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "postId": 123456789,
    "createdAt": "2024-01-01T12:00:00Z"
  }
}
```

---

### 10. 删除动态

| 项目 | 内容 |
|------|------|
| **接口路径** | `/api/v1/posts/{postId}` |
| **请求方式** | DELETE |
| **认证要求** | AccessToken(仅动态作者) |

**路径参数**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| postId | Long | 是 | 动态ID |

**响应示例**
```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

---

### 11. 点赞动态

| 项目 | 内容 |
|------|------|
| **接口路径** | `/api/v1/posts/{postId}/like` |
| **请求方式** | POST |
| **认证要求** | AccessToken |

**路径参数**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| postId | Long | 是 | 动态ID |

**响应示例**
```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

---

### 12. 取消点赞

| 项目 | 内容 |
|------|------|
| **接口路径** | `/api/v1/posts/{postId}/like` |
| **请求方式** | DELETE |
| **认证要求** | AccessToken |

**路径参数**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| postId | Long | 是 | 动态ID |

**响应示例**
```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

---

## Feed流模块 (Feed)

### 13. 获取Feed流(游标分页)

| 项目 | 内容 |
|------|------|
| **接口路径** | `/api/v1/feed` |
| **请求方式** | GET |
| **认证要求** | AccessToken |
| **分页方式** | 游标分页 |

**Query参数**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| cursor | Long | 否 | 上次返回的lastPostId |
| postTime | Long | 否 | 上次返回的lastPostTime(时间戳秒) |
| size | Integer | 否 | 每页数量(默认20,最大50) |

**响应示例**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [
      {
        "postId": 123456789,
        "userId": 111,
        "username": "user111",
        "nickname": "昵称",
        "avatarUrl": "https://...",
        "content": "动态内容",
        "mediaUrls": ["https://..."],
        "mediaTypes": ["image"],
        "aiSummary": "AI摘要内容",
        "likeCount": 100,
        "commentCount": 20,
        "shareCount": 5,
        "isLiked": false,
        "createdAt": "2024-01-01T12:00:00Z"
      }
    ],
    "lastPostId": 123456789,
    "lastPostTime": 1704067200,
    "hasMore": true
  }
}
```

### Feed流拉取策略

| 场景 | 数据源 | 说明 |
|------|--------|------|
| Redis收件箱有数据 | Redis List inbox:{userId} | 优先读取，性能最优 |
| 收件箱为空/过期 | MySQL feed表 | 拉模式兜底 |
| 混合模式 | 合并Redis+MySQL | 取最新200条合并排序 |

---

### 14. 获取用户动态列表

| 项目 | 内容 |
|------|------|
| **接口路径** | `/api/v1/users/{userId}/posts` |
| **请求方式** | GET |
| **认证要求** | 无需认证(公开内容) |

**路径参数**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID |

**Query参数**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| cursor | Long | 否 | 上次返回的postId |
| postTime | Long | 否 | 上次返回的postTime |
| size | Integer | 否 | 每页数量(默认20) |

**响应示例**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [...],
    "lastPostId": 123456789,
    "lastPostTime": 1704067200,
    "hasMore": true
  }
}
```

---

## 评论模块 (Comment)

### 15. 获取动态评论(两层扁平)

| 项目 | 内容 |
|------|------|
| **接口路径** | `/api/v1/posts/{postId}/comments` |
| **请求方式** | GET |
| **认证要求** | 无需认证 |

**路径参数**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| postId | Long | 是 | 动态ID |

**Query参数**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| cursor | Long | 否 | 上次返回的commentId |
| size | Integer | 否 | 每页数量(默认20) |

**响应示例**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [
      {
        "commentId": 1001,
        "userId": 111,
        "username": "user111",
        "nickname": "昵称",
        "avatarUrl": "https://...",
        "content": "顶级评论内容",
        "likeCount": 10,
        "replyCount": 5,
        "isLiked": false,
        "createdAt": "2024-01-01T12:00:00Z",
        "replies": [
          {
            "commentId": 1002,
            "userId": 222,
            "username": "user222",
            "nickname": "昵称",
            "content": "二级回复内容",
            "likeCount": 3,
            "isLiked": false,
            "createdAt": "2024-01-01T12:05:00Z"
          }
        ]
      }
    ],
    "lastCommentId": 1001,
    "hasMore": true
  }
}
```

### 评论查询策略

- 数据存储：单表存储，parent_id自关联实现无限层级
- API返回：顶级评论+直接子评论(最多展示2层)
- 前端渲染：递归组装完整评论树
- 时间分桶：查询时按`time_bucket`定位，减少全表扫描

---

### 16. 发布评论

| 项目 | 内容 |
|------|------|
| **接口路径** | `/api/v1/posts/{postId}/comments` |
| **请求方式** | POST |
| **认证要求** | AccessToken |
| **Content-Type** | `application/json` |
| **异步处理** | 写入Redis队列，批量异步落库 |

**路径参数**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| postId | Long | 是 | 动态ID |

**请求参数**
```json
{
  "content": "string(必填, 1-1000字符)",
  "parentId": 0,
  "atUsers": [123, 456]
}
```

**响应示例**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "commentId": 123456789,
    "createdAt": "2024-01-01T12:00:00Z"
  }
}
```

---

### 17. 删除评论

| 项目 | 内容 |
|------|------|
| **接口路径** | `/api/v1/comments/{commentId}` |
| **请求方式** | DELETE |
| **认证要求** | AccessToken(仅评论作者或动态作者) |

**路径参数**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| commentId | Long | 是 | 评论ID |

**响应示例**
```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

---

### 18. 点赞评论

| 项目 | 内容 |
|------|------|
| **接口路径** | `/api/v1/comments/{commentId}/like` |
| **请求方式** | POST |
| **认证要求** | AccessToken |

**路径参数**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| commentId | Long | 是 | 评论ID |

**响应示例**
```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

---

## 搜索模块 (Search)

### 19. 搜索动态

| 项目 | 内容 |
|------|------|
| **接口路径** | `/api/v1/search/posts` |
| **请求方式** | GET |
| **认证要求** | 无需认证 |

**Query参数**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| keyword | String | 是 | 搜索关键词 |
| cursor | Long | 否 | 上次返回的postId |
| size | Integer | 否 | 每页数量(默认20) |

**响应示例**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [
      {
        "postId": 123456789,
        "userId": 111,
        "username": "user111",
        "nickname": "昵称",
        "content": "匹配的动态内容...",
        "aiSummary": "AI摘要...",
        "likeCount": 100,
        "commentCount": 20,
        "createdAt": "2024-01-01T12:00:00Z"
      }
    ],
    "lastPostId": 123456789,
    "hasMore": true
  }
}
```

### 搜索实现

- 搜索引擎：Elasticsearch 8.0
- 索引同步：动态发布时通过RabbitMQ异步同步到ES
- 分词策略：IK中文分词器
- 高亮显示：关键词高亮

---

## 通用响应格式

### 成功响应
```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

### 错误响应
```json
{
  "code": 1001,
  "message": "用户名或密码错误",
  "data": null
}
```

### 公共错误码

| 错误码 | 说明 |
|--------|------|
| 0 | 成功 |
| 1001 | 用户名或密码错误 |
| 1002 | 用户不存在 |
| 1003 | 令牌已过期 |
| 1004 | 令牌无效 |
| 1005 | 刷新令牌已失效 |
| 2001 | 动态不存在 |
| 2002 | 无权操作 |
| 3001 | 评论不存在 |
| 4001 | 请求频率超限 |
| 5001 | 内部服务器错误 |

---

## 认证头说明

### 请求头
```
Authorization: Bearer <accessToken>
```

### 令牌有效期
| 令牌类型 | 有效期 |
|----------|--------|
| AccessToken | 15分钟 |
| RefreshToken | 7天 |

---

## 限流说明

### 限流配置
- 滑动窗口：1秒
- 请求上限：100次/秒
- 去重窗口：5秒

### 限流响应
```json
{
  "code": 4001,
  "message": "请求频率超限，请稍后重试",
  "data": null
}
```
