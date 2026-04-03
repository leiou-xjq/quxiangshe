# 趣享社笔记模块接口文档

## 一、接口概述

本文档描述趣享社笔记模块的REST API接口，包括笔记管理、互动功能（点赞/收藏）、搜索功能等。

**接口基础路径**：`/api/v1`

**通用响应格式**：
```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

**错误响应格式**：
```json
{
  "code": 400,
  "message": "错误描述",
  "data": null
}
```

---

## 二、笔记管理接口

### 2.1 创建笔记

**请求地址**：`POST /api/v1/notes`

**请求头**：
| 参数 | 说明 | 必填 |
|------|------|------|
| Authorization | Bearer Token | 是 |

**请求体**：
```json
{
  "title": "笔记标题",
  "content": "笔记正文内容",
  "coverImage": "https://example.com/cover.jpg",
  "category": "旅行",
  "tags": ["风景", "日本"],
  "images": [
    "https://example.com/image1.jpg",
    "https://example.com/image2.jpg"
  ]
}
```

**请求参数说明**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | String | 是 | 标题，1-200字符 |
| content | String | 是 | 正文内容，1-10000字符 |
| coverImage | String | 否 | 封面图片URL |
| category | String | 否 | 分类，默认"默认" |
| tags | Array | 否 | 标签数组 |
| images | Array | 否 | 图片URL数组 |

**响应示例（成功）**：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "noteId": 123456789,
    "title": "笔记标题",
    "content": "笔记正文内容",
    "auditStatus": 1,
    "rejectReason": null,
    "createTime": "2026-04-03T10:30:00"
  }
}
```

**响应示例（敏感词拒绝）**：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "noteId": 123456789,
    "title": "笔记标题",
    "content": "笔记正文内容",
    "auditStatus": 2,
    "rejectReason": "敏感词数量超过阈值",
    "createTime": "2026-04-03T10:30:00"
  }
}
```

**错误码**：
| 错误码 | 说明 |
|--------|------|
| 400 | 参数校验失败 |
| 429 | 请求过于频繁（限流） |

---

### 2.2 获取笔记详情

**请求地址**：`GET /api/v1/notes/{noteId}`

**请求头**：
| 参数 | 说明 | 必填 |
|------|------|------|
| Authorization | Bearer Token | 否 |

**路径参数**：
| 参数 | 类型 | 说明 |
|------|------|------|
| noteId | Long | 笔记ID |

**响应示例（成功）**：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "noteId": 123456789,
    "userId": 100001,
    "username": "user001",
    "nickname": "用户名",
    "avatarUrl": "https://example.com/avatar.jpg",
    "title": "笔记标题",
    "content": "笔记正文内容",
    "coverImage": "https://example.com/cover.jpg",
    "category": "旅行",
    "tags": ["风景", "日本"],
    "images": [
      "https://example.com/image1.jpg"
    ],
    "likeCount": 100,
    "commentCount": 20,
    "collectCount": 15,
    "viewCount": 1000,
    "auditStatus": 1,
    "rejectReason": null,
    "isLiked": false,
    "isCollected": false,
    "createTime": "2026-04-03T10:30:00"
  }
}
```

**错误码**：
| 错误码 | 说明 |
|--------|------|
| 400 | 笔记不存在 |

---

### 2.3 获取用户笔记列表

**请求地址**：`GET /api/v1/users/{userId}/notes`

**路径参数**：
| 参数 | 类型 | 说明 |
|------|------|------|
| userId | Long | 用户ID |

**查询参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| lastId | Long | 否 | 游标，最后一条笔记ID |
| size | Integer | 否 | 每页数量，默认20 |

**响应示例**：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [...],
    "lastNoteId": 123456780,
    "hasMore": true
  }
}
```

---

### 2.4 获取首页笔记列表

**请求地址**：`GET /api/v1/notes`

**查询参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| lastId | Long | 否 | 游标，最后一条笔记ID |
| size | Integer | 否 | 每页数量，默认20 |

**响应示例**：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [...],
    "lastNoteId": 123456780,
    "hasMore": true
  }
}
```

---

### 2.5 删除笔记

**请求地址**：`DELETE /api/v1/notes/{noteId}`

**请求头**：
| 参数 | 说明 | 必填 |
|------|------|------|
| Authorization | Bearer Token | 是 |

**路径参数**：
| 参数 | 类型 | 说明 |
|------|------|------|
| noteId | Long | 笔记ID |

**响应示例（成功）**：
```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

**错误码**：
| 错误码 | 说明 |
|--------|------|
| 400 | 笔记不存在 |
| 403 | 无权限操作 |

---

### 2.6 审核笔记（管理员）

**请求地址**：`POST /api/v1/admin/notes/{noteId}/review`

**请求头**：
| 参数 | 说明 | 必填 |
|------|------|------|
| Authorization | Bearer Token | 是 |

**路径参数**：
| 参数 | 类型 | 说明 |
|------|------|------|
| noteId | Long | 笔记ID |

**查询参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| approved | Boolean | 是 | 审核结果，true=通过，false=拒绝 |

**响应示例（成功）**：
```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

---

## 三、互动功能接口

### 3.1 点赞笔记

**请求地址**：`POST /api/v1/notes/{noteId}/like`

**请求头**：
| 参数 | 说明 | 必填 |
|------|------|------|
| Authorization | Bearer Token | 是 |

**路径参数**：
| 参数 | 类型 | 说明 |
|------|------|------|
| noteId | Long | 笔记ID |

**响应示例（成功）**：
```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

**响应示例（已点赞）**：
```json
{
  "code": 400,
  "message": "已点赞",
  "data": null
}
```

**错误码**：
| 错误码 | 说明 |
|--------|------|
| 400 | 笔记不存在 |
| 400 | 已点赞 |
| 429 | 请求过于频繁（限流） |

---

### 3.2 取消点赞

**请求地址**：`DELETE /api/v1/notes/{noteId}/like`

**请求头**：
| 参数 | 说明 | 必填 |
|------|------|------|
| Authorization | Bearer Token | 是 |

**路径参数**：
| 参数 | 类型 | 说明 |
|------|------|------|
| noteId | Long | 笔记ID |

**响应示例（成功）**：
```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

**响应示例（未点赞）**：
```json
{
  "code": 400,
  "message": "未点赞",
  "data": null
}
```

**错误码**：
| 错误码 | 说明 |
|--------|------|
| 400 | 笔记不存在 |
| 400 | 未点赞 |

---

### 3.3 收藏笔记

**请求地址**：`POST /api/v1/notes/{noteId}/collect`

**请求头**：
| 参数 | 说明 | 必填 |
|------|------|------|
| Authorization | Bearer Token | 是 |

**路径参数**：
| 参数 | 类型 | 说明 |
|------|------|------|
| noteId | Long | 笔记ID |

**响应示例（成功）**：
```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

**响应示例（已收藏）**：
```json
{
  "code": 400,
  "message": "已收藏",
  "data": null
}
```

**错误码**：
| 错误码 | 说明 |
|--------|------|
| 400 | 笔记不存在 |
| 400 | 已收藏 |

---

### 3.4 取消收藏

**请求地址**：`DELETE /api/v1/notes/{noteId}/collect`

**请求头**：
| 参数 | 说明 | 必填 |
|------|------|------|
| Authorization | Bearer Token | 是 |

**路径参数**：
| 参数 | 类型 | 说明 |
|------|------|------|
| noteId | Long | 笔记ID |

**响应示例（成功）**：
```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

**响应示例（未收藏）**：
```json
{
  "code": 400,
  "message": "未收藏",
  "data": null
}
```

**错误码**：
| 错误码 | 说明 |
|--------|------|
| 400 | 笔记不存在 |
| 400 | 未收藏 |

---

## 四、搜索功能接口

### 4.1 搜索笔记

**请求地址**：`GET /api/v1/notes/search`

**查询参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 否 | 搜索关键词 |
| category | String | 否 | 分类筛选 |
| page | Integer | 否 | 页码，默认1 |
| size | Integer | 否 | 每页数量，默认20 |

**响应示例**：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [...],
    "lastNoteId": 123456780,
    "hasMore": true
  }
}
```

---

## 五、接口幂等性说明

### 5.1 点赞/收藏幂等性

点赞和收藏接口具有幂等性：

1. **首次操作**：正常执行，返回成功
2. **重复操作**：
   - 点赞重复：返回 `400，已点赞`
   - 收藏重复：返回 `400，已收藏`
3. **取消操作**：
   - 未点赞时取消：返回 `400，未点赞`
   - 未收藏时取消：返回 `400，未收藏`

**实现机制**：
- Redis缓存快速判断（key: `note:like:{noteId}:{userId}`）
- MySQL唯一约束兜底（`uk_note_user(note_id, user_id)`）

---

## 六、限流说明

| 接口 | 限制 |
|------|------|
| POST /api/v1/notes | 10次/分钟 |
| POST /api/v1/notes/{noteId}/like | 20次/分钟 |

触发限流时返回：
```json
{
  "code": 429,
  "message": "请求过于频繁，请稍后再试",
  "data": null
}
```

---

*文档更新时间：2026-04-03*
