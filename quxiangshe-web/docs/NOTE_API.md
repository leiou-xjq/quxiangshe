# 趣享社笔记模块接口文档

## 一、接口总览

下列接口覆盖笔记的发布、编辑、删除、列表展示、详情查看、点赞、收藏、评论、ES搜索、热门笔记排序等功能。所有接口遵循统一的响应结构，成功返回数据在 data 字段内，失败统一返回错误信息。该文档覆盖 OpenSpec 风格的接口描述，便于前后端对接及自动化文档生成。

| 模块 | 接口地址 | 方法 | 说明 | 备注 |
|---|---|---|---|---|
| 笔记 | `/api/v1/notes` | POST | 发布笔记 | 需登录 |
| 笔记 | `/api/v1/notes/{noteId}` | GET | 笔记详情 | 可公开访问，需要鉴权时按接口返回 |
| 笔记 | `/api/v1/users/{userId}/notes` | GET | 用户笔记列表 | 分页游标/大小 |
| 笔记 | `/api/v1/notes` | GET | 首页笔记列表 | 调整排序/分页 |
| 笔记 | `/api/v1/notes/{noteId}/like` | POST | 点赞笔记 | 登录/幂等 |
| 笔记 | `/api/v1/notes/{noteId}/like` | DELETE | 取消点赞 | 登录 |
| 笔记 | `/api/v1/notes/{noteId}/collect` | POST | 收藏笔记 | 登录 |
| 笔记 | `/api/v1/notes/{noteId}/collect` | DELETE | 取消收藏 | 登录 |
| 笔记 | `/api/v1/notes/{noteId}/comments` | POST | 发布笔记评论 | 登录 |
| 笔记 | `/api/v1/notes/{noteId}/comments` | GET | 获取笔记评论 | 需鉴权时要求登录 |
| 笔记 | `/api/v1/comments/{commentId}/like` | POST | 点赞评论 | 登录 |
| 笔记 | `/api/v1/comments/{commentId}/like` | DELETE | 取消点赞 | 登录 |
| 搜索 | `/api/v1/search/notes` | GET | 搜索笔记 | keyword、category、page、size |
| 搜索 | `/api/v1/search` | GET | 统一搜索（note/user） | type 支持 all/note/user |
| 热门笔记排序 | 由客户端对返回笔记按 likeCount + collectCount 进行排序 | - | - | - |

> 备注：对外暴露的搜索和笔记相关接口遵循 OpenSpec 风格描述，具体字段与返回结构请参照后续各接口的详细信息段落。

---

## 二、接口详情

### 1. 发布笔记

**请求**
```
POST /api/v1/notes
Content-Type: application/json
```

**请求参数**
| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| title | string | 是 | 笔记标题，1-200 字符 |
| content | string | 是 | 笔记正文，1-10000 字符 |
| coverImage | string | 否 | 封面图片 URL |
| category | string | 否 | 分类 |
| tags | array<string> | 否 | 标签数组 |
| images | array<string> | 否 | 追加图片 URL 数组 |

**响应结构**
| 字段 | 类型 | 说明 |
|---|---|---|
| code | int | 状态码（0 表示成功） |
| message | string | 错误信息/成功信息 |
| data | object | 成功时返回笔记信息 |

**响应示例**
```
{ "code": 0, "message": "success", "data": { "noteId": 123456, "title": "笔记标题", "createTime": "2026-04-03T10:30:00" } }
```

**错误码**
- 400: 参数校验失败
- 401: 未认证
- 429: 请求过于频繁

---

### 2. 获取笔记详情

**请求**
```
GET /api/v1/notes/{noteId}
```

**响应参数示例**
```
{
  "code": 0,
  "message": "success",
  "data": {
    "noteId": 123456,
    "userId": 100001,
    "username": "author",
    "nickname": "作者",
    "avatarUrl": "https://example.com/avatar.jpg",
    "title": "笔记标题",
    "content": "笔记正文...",
    "coverImage": "https://example.com/cover.jpg",
    "category": "旅行",
    "tags": ["风景","日本"],
    "images": ["https://example.com/1.jpg"],
    "likeCount": 10,
    "commentCount": 5,
    "collectCount": 3,
    "viewCount": 120,
    "createTime": "2026-04-03T10:30:00"
  }
}
```

**错误码**
- 400: 笔记不存在

---

### 3. 获取用户笔记列表

**请求**
```
GET /api/v1/users/{userId}/notes
```

**查询参数**
| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| lastId | long | 否 | 游标，最后一条笔记 ID |
| size | int | 否 | 每页数量，默认 20 |

**响应示例**
```
{ "code": 0, "message": "success", "data": { "items": [ /*笔记对象数组*/ ], "lastNoteId": 123456, "hasMore": true } }
```

---

### 4. 获取首页笔记列表

**请求**
```
GET /api/v1/notes
```

**查询参数**
| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| lastId | long | 否 | 游标，最后一条笔记 ID |
| size | int | 否 | 每页数量，默认 20 |

**响应示例**同上，包含 items/lastNoteId/hasMore。 

---

### 5. 搜索笔记

**请求**
```
GET /api/v1/search/notes
```

**查询参数**
| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| keyword | string | 否 | 搜索关键词（标题/内容） |
| category | string | 否 | 分类筛选 |
| page | int | 否 | 页码，默认 1 |
| size | int | 否 | 每页数量，默认 20 |

**响应示例**
```
{
  "code": 0,
  "message": "success",
  "data": {
    "type": "note",
    "notes": [ /* NoteResponseDTO 集合，带 highlight 字段 */ ],
    "totalCount": 25,
    "page": 1,
    "size": 20,
    "hasMore": true
  }
}
```

---

### 6. 点赞笔记

**请求**
```
POST /api/v1/notes/{noteId}/like
```

**响应示例**
```
{ "code": 0, "message": "success", "data": null }
```

**错误码**
- 401: 未认证
- 400: 笔记不存在
- 400: 已点赞

---

### 7. 取消点赞

**请求**
```
DELETE /api/v1/notes/{noteId}/like
```

**响应示例**
```
{ "code": 0, "message": "success", "data": null }
```

---

### 8. 收藏笔记

**请求**
```
POST /api/v1/notes/{noteId}/collect
```

**响应示例**
```
{ "code": 0, "message": "success", "data": null }
```

**错误码**
- 401: 未认证
- 400: 笔记不存在
- 400: 已收藏

---

### 9. 取消收藏

**请求**
```
DELETE /api/v1/notes/{noteId}/collect
```

**响应示例**
```
{ "code": 0, "message": "success", "data": null }
```

---

### 10. 发布笔记评论

**请求**
```
POST /api/v1/notes/{noteId}/comments
```

**请求参数**
| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| content | string | 是 | 评论内容 |
| parentId | long | 否 | 父级评论 ID（若为回复） |

**响应示例**
```
{ "code": 0, "message": "success", "data": { "commentId": 987, "createTime": "2026-04-03T12:00:00" } }
```

---

### 11. 获取笔记评论

**请求**
```
GET /api/v1/notes/{noteId}/comments
```

**响应示例**
```
{ "code": 0, "message": "success", "data": { "items": [ /*Comment DTOs*/ ], "lastCommentId": 0, "hasMore": false } }
```

---
### 12. ES 搜索
- 统一搜索接口 GET /api/v1/search 可以通过 type=note 或 /search/notes 进行笔记检索，详情参见搜索 API。

---
### 13. 热门笔记排序
- 客户端对笔记数据按“点赞数 + 收藏数”的综合得分进行排序，后端返回的笔记数据包含 likeCount、collectCount、viewCount 等字段用于排序与展示。

---

## 三、开发要点

- 核心技术栈
  - 后端：Java、Spring Boot、MyBatis-Plus、Elasticsearch、RabbitMQ、Redis、JWT、Spring Security
  - 前端：Vue3、Pinia、Element Plus、Axios
- 数据存储与索引
  - 数据库：MySQL（t_note、t_note_like、t_note_collect、t_note_image、t_sensitive_check_log 等表）
  - 搜索：Elasticsearch，笔记索引 t_note、用户索引 t_user
- 关键流程设计
  - 发布笔记：敏感词校验、审核状态、写入数据库、图片处理、ES 同步、返回笔记信息
  - 详情与浏览：校验笔记存在性、增浏览、组装图片、标签、作者信息，返回完整 DTO
  - 互动：点赞/收藏具备幂等性，使用 Redis 缓存 + DB 约束兜底
  - 评论：创建、显示、点赞、回复等功能
  - ES 搜索：基于全文检索实现高亮、分词、排序
  - 热门笔记排序：通过客户端排序（如 likeCount + collectCount）实现热度排序
- 与 Auth 的协作
  - 点赞/收藏/评论等需要鉴权，未登录将返回未认证错误
- 模块间的关联逻辑
  - Note 与 User、Comment、Search、ES 的集成点明确，互相解耦，使用消息队列完成 ES 同步

---

## 四、版本与变更记录
- 版本 1.0.0：实现笔记的发布、详情、列表、点赞、收藏、评论、搜索、热度排序等核心功能
- 版本 1.x：逐步增加笔记编辑、图片处理、管理员审核等能力

---

*文档更新时间：2026-04-03*
