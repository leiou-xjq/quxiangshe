# 趣享社搜索模块接口文档

## 一、接口概述

本文档描述趣享社统一搜索模块的REST API接口，支持笔记和用户的混合搜索。

**接口基础路径**：`/api/v1`

**通用响应格式**：
```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

---

## 二、技术方案对比

### 2.1 MySQL LIKE vs Elasticsearch

| 维度 | MySQL LIKE | Elasticsearch |
|------|------------|---------------|
| **性能** | 深度分页性能差，全表扫描 | 分布式索引，毫秒级响应 |
| **分词** | 不支持中文分词 | IK分词器，支持智能/最大化匹配 |
| **模糊匹配** | `LIKE '%keyword%'` 前缀无法使用索引 | 倒排索引+TF-IDF评分 |
| **分页** | OFFSET大时性能下降 | scroll/after分页，支持深度分页 |
| **高亮** | 需手动实现 | 内置高亮支持，`<em>`标签标记 |
| **扩展性** | 难以支持复杂查询 | 支持布尔查询、聚合、权重调整 |
| **排序** | 单一字段排序 | 多字段组合排序 |
| **并发** | 受限于MySQL连接数 | 分布式架构，高并发支撑 |

### 2.2 为什么选择Elasticsearch

1. **性能**：ES搜索响应时间通常在10ms以内，而MySQL LIKE可能需要数百毫秒甚至数秒
2. **分词**：支持中文IK分词，MySQL无法实现中文全文检索
3. **高亮**：内置高亮功能，无需应用层处理
4. **高并发**：分布式架构，支持水平扩展
5. **复杂查询**：支持多条件组合、权重调整、布尔查询

---

## 三、搜索接口

### 3.1 统一搜索（混合搜索）

**请求地址**：`GET /api/v1/search` 或 `GET /api/v1/search/all`

**查询参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 否 | 搜索关键词 |
| type | String | 否 | 搜索类型：all-全部(默认)，note-仅笔记，user-仅用户 |
| page | Integer | 否 | 页码，默认1 |
| size | Integer | 否 | 每页数量，默认20，最大50 |

**响应示例（成功）**：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "type": "all",
    "notes": [
      {
        "noteId": 123456789,
        "userId": 100001,
        "username": "user001",
        "nickname": "旅行达人",
        "avatarUrl": "https://example.com/avatar.jpg",
        "title": "日本东京旅行攻略",
        "content": "东京是日本的首都，也是旅行热门目的地...",
        "coverImage": "https://example.com/cover.jpg",
        "category": "旅行",
        "tags": ["风景", "日本", "东京"],
        "likeCount": 100,
        "commentCount": 20,
        "collectCount": 15,
        "viewCount": 1000,
        "createTime": "2026-04-03T10:30:00",
        "highlightTitle": "日本<em>东京</em>旅行攻略",
        "highlightContent": "<em>东京</em>是日本的首都，也是旅行热门目的地..."
      }
    ],
    "users": [
      {
        "userId": 100001,
        "username": "traveler",
        "nickname": "旅行达人",
        "avatarUrl": "https://example.com/avatar.jpg",
        "bio": "热爱旅行，分享旅行攻略",
        "createTime": "2026-01-01T00:00:00",
        "highlightUsername": "<em>travel</em>er",
        "highlightNickname": "<em>旅行</em>达人"
      }
    ],
    "totalCount": 25,
    "page": 1,
    "size": 20,
    "hasMore": true,
    "costTime": 15
  }
}
```

---

### 3.2 搜索笔记

**请求地址**：`GET /api/v1/search/notes` 或 `GET /api/v1/search/note`

**查询参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 否 | 搜索关键词（标题、内容） |
| category | String | 否 | 分类筛选 |
| page | Integer | 否 | 页码，默认1 |
| size | Integer | 否 | 每页数量，默认20 |

**响应示例**：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "type": "note",
    "notes": [
      {
        "noteId": 123456789,
        "userId": 100001,
        "username": "user001",
        "nickname": "旅行达人",
        "avatarUrl": "https://example.com/avatar.jpg",
        "title": "日本东京旅行攻略",
        "content": "东京是日本的首都...",
        "coverImage": "https://example.com/cover.jpg",
        "category": "旅行",
        "tags": ["风景", "日本"],
        "likeCount": 100,
        "commentCount": 20,
        "collectCount": 15,
        "viewCount": 1000,
        "createTime": "2026-04-03T10:30:00",
        "highlightTitle": "日本<em>东京</em>旅行攻略",
        "highlightContent": "<em>东京</em>是日本的首都..."
      }
    ],
    "totalCount": 15,
    "page": 1,
    "size": 20,
    "hasMore": false
  }
}
```

**搜索字段权重**：
- 标题(title)：权重x2
- 内容(content)：权重x1

---

### 3.3 搜索用户

**请求地址**：`GET /api/v1/search/users`

**查询参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 否 | 搜索关键词（用户名、昵称） |
| page | Integer | 否 | 页码，默认1 |
| size | Integer | 否 | 每页数量，默认20 |

**响应示例**：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "type": "user",
    "users": [
      {
        "userId": 100001,
        "username": "traveler",
        "nickname": "旅行达人",
        "avatarUrl": "https://example.com/avatar.jpg",
        "bio": "热爱旅行，分享旅行攻略",
        "createTime": "2026-01-01T00:00:00",
        "highlightUsername": "<em>travel</em>er",
        "highlightNickname": "<em>旅行</em>达人"
      }
    ],
    "totalCount": 10,
    "page": 1,
    "size": 20,
    "hasMore": false
  }
}
```

**搜索字段权重**：
- 用户名(username)：权重x2
- 昵称(nickname)：权重x2
- 个人简介(bio)：权重x1

---

## 四、ES索引结构

### 4.1 笔记索引 t_note

```json
{
  "mappings": {
    "properties": {
      "id": { "type": "long" },
      "userId": { "type": "long" },
      "nickname": { "type": "text", "analyzer": "ik_max_word" },
      "username": { "type": "text", "analyzer": "ik_max_word" },
      "avatarUrl": { "type": "keyword" },
      "title": { 
        "type": "text", 
        "analyzer": "ik_max_word", 
        "search_analyzer": "ik_smart" 
      },
      "content": { 
        "type": "text", 
        "analyzer": "ik_max_word", 
        "search_analyzer": "ik_smart" 
      },
      "coverImage": { "type": "keyword" },
      "category": { "type": "keyword" },
      "tags": { "type": "keyword" },
      "likeCount": { "type": "integer" },
      "commentCount": { "type": "integer" },
      "collectCount": { "type": "integer" },
      "viewCount": { "type": "integer" },
      "status": { "type": "integer" },
      "deleted": { "type": "integer" },
      "createTime": { "type": "date" }
    }
  },
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 1,
    "analysis": {
      "analyzer": {
        "ik_max_word": {
          "type": "custom",
          "tokenizer": "ik_max_word"
        },
        "ik_smart": {
          "type": "custom",
          "tokenizer": "ik_smart"
        }
      }
    }
  }
}
```

### 4.2 用户索引 t_user

```json
{
  "mappings": {
    "properties": {
      "id": { "type": "long" },
      "username": { 
        "type": "text", 
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart"
      },
      "nickname": { 
        "type": "text", 
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart"
      },
      "phone": { "type": "keyword" },
      "avatarUrl": { "type": "keyword" },
      "bio": { 
        "type": "text", 
        "analyzer": "ik_max_word"
      },
      "status": { "type": "integer" },
      "lastLoginTime": { "type": "date" },
      "createdAt": { "type": "date" },
      "updatedAt": { "type": "date" }
    }
  }
}
```

---

## 五、数据同步方案

> **重要提示**：当前系统使用**应用内同步方案**（RabbitMQ + 异步处理），不需要部署Canal Server。
> 
> 如需启用Canal实时同步，请参考 `CANAL_DEPLOY.md` 文档手动部署。

### 5.1 当前同步架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                     应用内同步架构                                    │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   ┌─────────────┐                        ┌─────────────┐            │
│   │   应用层    │  RabbitMQ              │    ES       │            │
│   │  Service   │ ──────────────────────►│  Index     │            │
│   │            │    异步消息             │            │            │
│   └─────────────┘                        └─────────────┘            │
│         │                                      ▲                   │
│         │                                      │                   │
│         ▼                                      │                   │
│   ┌─────────────┐                               │                   │
│   │   MySQL    │ ──── 数据变更 ────► NoteQueue │ ──► 消费者       │
│   │            │                               │                   │
│   └─────────────┘                               │                   │
│                                                │                   │
└─────────────────────────────────────────────────────────────────────┘
```

### 5.2 同步逻辑

| 数据库操作 | 触发方式 | ES处理 |
|-----------|----------|--------|
| 新增笔记 | 审核通过后自动同步 | 新增文档 |
| 更新笔记 | 审核状态变更时 | 更新/删除文档 |
| 删除笔记 | 逻辑删除时 | 删除文档 |
| 新增用户 | 注册时同步 | 新增文档 |
| 更新用户 | 更新时同步 | 更新文档 |

### 5.3 当前配置

```yaml
# 应用内同步（已启用）
canal:
  enabled: false
```

### 5.4 升级到Canal（可选）

如需升级到Canal实时同步：

1. 下载Canal Server（需要能访问GitHub）
2. 配置MySQL binlog
3. 修改配置 `canal.enabled: true`

详见 `CANAL_DEPLOY.md`

---

## 六、错误码

| 错误码 | 说明 |
|--------|------|
| 0 | 成功 |
| 400 | 参数错误 |
| 500 | 服务器内部错误 |

---

## 七、响应字段说明

### 7.1 统一响应字段

| 字段 | 类型 | 说明 |
|------|------|------|
| type | String | 结果类型：all/note/user |
| totalCount | Long | 总记录数 |
| page | Integer | 当前页码 |
| size | Integer | 每页数量 |
| hasMore | Boolean | 是否有更多 |
| costTime | Long | 搜索耗时（毫秒） |

### 7.2 笔记结果字段

| 字段 | 类型 | 说明 |
|------|------|------|
| noteId | Long | 笔记ID |
| userId | Long | 发布者ID |
| username | String | 发布者用户名 |
| nickname | String | 发布者昵称 |
| avatarUrl | String | 发布者头像 |
| title | String | 标题（原始） |
| content | String | 内容（原始） |
| highlightTitle | String | 高亮标题 |
| highlightContent | String | 高亮内容 |
| category | String | 分类 |
| tags | Array | 标签 |
| likeCount | Integer | 点赞数 |
| commentCount | Integer | 评论数 |
| collectCount | Integer | 收藏数 |
| viewCount | Integer | 浏览数 |
| createTime | String | 创建时间 |

### 7.3 用户结果字段

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | Long | 用户ID |
| username | String | 用户名（原始） |
| nickname | String | 昵称（原始） |
| highlightUsername | String | 高亮用户名 |
| highlightNickname | String | 高亮昵称 |
| avatarUrl | String | 头像 |
| bio | String | 个人简介 |
| createTime | String | 创建时间 |

---

*文档更新时间：2026-04-03*
