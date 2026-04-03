# 趣享社笔记模块技术文档

## 一、模块概述

笔记模块（Note Module）是趣享社社交平台的核心内容模块，负责用户笔记（类似微博帖子）的创建、浏览、搜索、互动（点赞/收藏）等功能。

### 核心功能

| 功能 | 说明 |
|------|------|
| 发布笔记 | 用户创建笔记，支持标题、正文、图片、分类、标签 |
| 浏览笔记 | 首页信息流、用户笔记列表、笔记详情 |
| 搜索笔记 | ES分词模糊搜索、分类筛选、高亮展示 |
| 点赞/收藏 | 笔记互动功能，记录用户关系，实时统计 |
| 敏感词过滤 | DFA算法检测，敏感词替换/拒绝 |
| 审核机制 | 笔记发布需审核，支持管理员审核 |
| ES同步 | 审核通过后异步同步到Elasticsearch |

---

## 二、服务接口说明

### 2.1 NoteService 接口

```java
public interface NoteService {
    NoteResponseDTO createNote(Long userId, NoteCreateRequestDTO request);
    NoteResponseDTO getNoteDetail(Long noteId, Long currentUserId);
    NoteListResponse getUserNotes(Long userId, Long lastId, Integer size);
    NoteListResponse getHomeNotes(Long lastId, Integer size);
    NoteListResponse searchNotes(String keyword, String category, Integer page, Integer size);
    void likeNote(Long userId, Long noteId);
    void unlikeNote(Long userId, Long noteId);
    void collectNote(Long userId, Long noteId);
    void uncollectNote(Long userId, Long noteId);
    void deleteNote(Long userId, Long noteId);
    void reviewNote(Long noteId, boolean approved);
}
```

---

## 三、方法实现详解

### 3.1 createNote - 创建笔记

**方法签名**：
```java
public NoteResponseDTO createNote(Long userId, NoteCreateRequestDTO request)
```

**实现流程**（共7步）：

```
┌─────────────────────────────────────────────────────────────────┐
│                    createNote 实现流程                          │
├─────────────────────────────────────────────────────────────────┤
│ 第1步：敏感词校验                                                │
│   ├── 调用 sensitiveWordService.findSensitiveWords()           │
│   ├── 检测标题敏感词                                             │
│   └── 检测正文敏感词                                             │
│   ↓                                                              │
│ 第2步：确定审核状态                                              │
│   ├── 敏感词≥3个 → AUDIT_REJECTED（拒绝）                       │
│   ├── 敏感词1-2个 → AUDIT_PASSED（替换后通过）                  │
│   └── 无敏感词 → AUDIT_PASSED（直接通过）                        │
│   ↓                                                              │
│ 第3步：保存笔记实体                                              │
│   ├── 构建 NoteEntity 对象                                       │
│   ├── 序列化tags为JSON                                           │
│   └── noteMapper.insert(note)                                   │
│   ↓                                                              │
│ 第4步：保存图片                                                  │
│   ├── 遍历 request.getImages()                                   │
│   ├── 构建 NoteImageEntity                                       │
│   └── noteImageMapper.insert(image)                             │
│   ↓                                                              │
│ 第5步：保存敏感词校验日志                                        │
│   ├── saveSensitiveCheckLog()                                   │
│   └── 写入 t_sensitive_check_log 表                             │
│   ↓                                                              │
│ 第6步：异步同步到ES                                              │
│   ├── auditStatus == AUDIT_PASSED 时触发                        │
│   ├── syncToElasticsearch(note)                                 │
│   └── 发送RabbitMQ消息，由消费者同步到ES                        │
│   ↓                                                              │
│ 第7步：构建返回结果                                              │
│   └── 返回 NoteResponseDTO（含noteId, title, auditStatus等）  │
└─────────────────────────────────────────────────────────────────┘
```

**关键代码逻辑**：

```java
// 第1步：敏感词校验
Set<String> titleWords = sensitiveWordService.findSensitiveWords(originalTitle);
Set<String> contentWords = sensitiveWordService.findSensitiveWords(originalContent);

// 第2步：审核决策
if (allWords.size() >= SENSITIVE_WORD_REJECT_THRESHOLD) {  // 阈值=3
    auditStatus = NoteEntity.AUDIT_REJECTED;
} else {
    auditStatus = NoteEntity.AUDIT_PASSED;
}

// 第3步：保存笔记
noteMapper.insert(note);

// 第4步：批量保存图片
for (int i = 0; i < request.getImages().size(); i++) {
    noteImageMapper.insert(image);
}

// 第6步：ES同步（仅审核通过时）
if (auditStatus == NoteEntity.AUDIT_PASSED) {
    syncToElasticsearch(note);  // 重试机制：3次，间隔1s,2s,4s
}
```

---

### 3.2 getNoteDetail - 获取笔记详情

**方法签名**：
```java
public NoteResponseDTO getNoteDetail(Long noteId, Long currentUserId)
```

**实现流程**：

```
┌─────────────────────────────────────────────────────────────────┐
│                  getNoteDetail 实现流程                         │
├─────────────────────────────────────────────────────────────────┤
│ 第1步：查询笔记                                                  │
│   └── noteMapper.selectById(noteId)                             │
│   ↓                                                              │
│ 第2步：验证笔记存在                                             │
│   ├── note == null → 抛出"笔记不存在"                          │
│   └── note.getDeleted() == 1 → 抛出"笔记不存在"               │
│   ↓                                                              │
│ 第3步：验证审核状态                                             │
│   ├── auditStatus != 1 → 抛出"笔记不存在"                      │
│   └── status != 1 → 抛出"笔记不存在"                          │
│   ↓                                                              │
│ 第4步：增加浏览计数                                             │
│   └── noteMapper.incrementViewCount(noteId)                    │
│   ↓                                                              │
│ 第5步：查询关联数据                                             │
│   ├── noteImageMapper.selectByNoteId(noteId)                   │
│   └── userMapper.selectById(note.getUserId())                  │
│   ↓                                                              │
│ 第6步：构建返回结果                                              │
│   └── buildNoteDTO(note, images, user, currentUserId)         │
└─────────────────────────────────────────────────────────────────┘
```

**关键代码逻辑**：

```java
// 查询笔记
NoteEntity note = noteMapper.selectById(noteId);
if (note == null || note.getDeleted() == 1) {
    throw new BusinessException("笔记不存在");
}

// 验证审核状态
if (note.getAuditStatus() != NoteEntity.AUDIT_PASSED 
    || note.getStatus() != NoteEntity.STATUS_NORMAL) {
    throw new BusinessException("笔记不存在");
}

// 增加浏览数
noteMapper.incrementViewCount(noteId);

// 查询图片和用户
List<NoteImageEntity> images = noteImageMapper.selectByNoteId(noteId);
UserEntity user = userMapper.selectById(note.getUserId());
```

---

### 3.3 getUserNotes - 获取用户笔记列表

**方法签名**：
```java
public NoteListResponse getUserNotes(Long userId, Long lastId, Integer size)
```

**实现流程**：

```
┌─────────────────────────────────────────────────────────────────┐
│                  getUserNotes 实现流程                          │
├─────────────────────────────────────────────────────────────────┤
│ 第1步：参数处理                                                  │
│   ├── size默认20                                                 │
│   └── lastId游标处理                                             │
│   ↓                                                              │
│ 第2步：查询笔记                                                  │
│   └── noteMapper.selectByUserId(userId, cursor, size+1)         │
│   ↓                                                              │
│ 第3步：处理分页                                                  │
│   ├── 判断hasMore                                                │
│   └── 截取实际数量                                               │
│   ↓                                                              │
│ 第4步：批量查询用户信息                                          │
│   └── getUserMap(userIds)                                       │
│   ↓                                                              │
│ 第5步：构建DTO列表                                               │
│   └── notes.stream().map(n -> buildNoteDTO(...))               │
│   ↓                                                              │
│ 第6步：返回结果                                                 │
│   └── NoteListResponse(items, lastNoteId, hasMore)             │
└─────────────────────────────────────────────────────────────────┘
```

**游标分页原理**：
- SQL: `WHERE id < lastId ORDER BY create_time DESC LIMIT size+1`
- 多查1条用于判断hasMore
- 返回lastNoteId作为下次请求的游标

---

### 3.4 getHomeNotes - 获取首页笔记列表

**方法签名**：
```java
public NoteListResponse getHomeNotes(Long lastId, Integer size)
```

**实现流程**：与 `getUserNotes` 类似，区别在于：

```java
// 查询条件不同
List<NoteEntity> notes = noteMapper.selectForHome(cursor, size + 1);

// SQL: SELECT * FROM t_note 
//      WHERE deleted = 0 AND audit_status = 1 AND status = 1
//      AND id < lastId 
//      ORDER BY create_time DESC, id DESC 
//      LIMIT size+1
```

---

### 3.5 searchNotes - 搜索笔记

**方法签名**：
```java
public NoteListResponse searchNotes(String keyword, String category, Integer page, Integer size)
```

**实现流程**：

```
┌─────────────────────────────────────────────────────────────────┐
│                  searchNotes 实现流程                           │
├─────────────────────────────────────────────────────────────────┤
│ 第1步：参数处理                                                  │
│   ├── page默认1                                                  │
│   └── size默认20                                                 │
│   ↓                                                              │
│ 第2步：调用搜索服务                                              │
│   └── noteSearchService.searchNotes(keyword, category, ...)    │
│   ↓                                                              │
│ 第3步：处理分页                                                  │
│   ├── 判断hasMore                                                │
│   └── 截取实际数量                                               │
│   ↓                                                              │
│ 第4步：返回结果                                                 │
│   └── NoteListResponse(items, lastNoteId, hasMore)             │
└─────────────────────────────────────────────────────────────────┘
```

**搜索实现**：
- 基于Elasticsearch全文检索
- 支持关键词搜索和分类筛选
- 返回结果封装为NoteResponseDTO列表

---

### 3.6 likeNote - 点赞笔记

**方法签名**：
```java
public void likeNote(Long userId, Long noteId)
```

**实现流程**（关键方法）：

```
┌─────────────────────────────────────────────────────────────────┐
│                    likeNote 实现流程                             │
├─────────────────────────────────────────────────────────────────┤
│ 第1步：验证笔记存在                                              │
│   └── noteMapper.selectById(noteId)                             │
│   ├── note == null → 抛出"笔记不存在"                           │
│   └── note.getDeleted() == 1 → 抛出"笔记不存在"                │
│   ↓                                                              │
│ 第2步：幂等性检查（双重检查）                                    │
│   ├── 第①层：Redis缓存检查                                      │
│   │   └── checkUserLikedFromCache(noteId, userId)              │
│   │   └── key="note:like:{noteId}:{userId}"                   │
│   ├── 第②层：数据库唯一约束检查                                 │
│   │   └── noteLikeMapper.checkUserLiked(noteId, userId)        │
│   └── 任一存在 → 抛出"已点赞"                                   │
│   ↓                                                              │
│ 第3步：写入点赞记录                                              │
│   ├── 构建 NoteLikeEntity                                        │
│   └── noteLikeMapper.insert(like)                              │
│   ↓                                                              │
│ 第4步：更新点赞计数                                              │
│   └── noteMapper.incrementLikeCount(noteId)                    │
│   │   └── SQL: UPDATE t_note SET like_count = like_count + 1   │
│   ↓                                                              │
│ 第5步：更新Redis缓存                                             │
│   └── setUserLikedCache(noteId, userId, true)                  │
│   │   └── key="note:like:{noteId}:{userId}"，过期1小时         │
│   ↓                                                              │
│ 第6步：记录日志                                                  │
│   └── log.info("用户 {} 点赞笔记 {}", userId, noteId)           │
└─────────────────────────────────────────────────────────────────┘
```

**关键代码逻辑**：

```java
// 第1步：验证笔记存在
NoteEntity note = noteMapper.selectById(noteId);
if (note == null || note.getDeleted() == 1) {
    throw new BusinessException("笔记不存在");
}

// 第2步：幂等性检查
if (checkUserLikedFromCache(noteId, userId) || noteLikeMapper.checkUserLiked(noteId, userId)) {
    throw new BusinessException("已点赞");
}

// 第3步：写入记录
NoteLikeEntity like = NoteLikeEntity.builder()
        .noteId(noteId)
        .userId(userId)
        .createTime(LocalDateTime.now())
        .build();
noteLikeMapper.insert(like);

// 第4步：更新计数
noteMapper.incrementLikeCount(noteId);

// 第5步：更新缓存
setUserLikedCache(noteId, userId, true);
```

**幂等性保证机制**：

| 层级 | 检查方式 | 失败处理 |
|------|----------|----------|
| 第①层 | Redis缓存 | 快速返回，缓存未命中则查DB |
| 第②层 | DB唯一约束 | `uk_note_user(note_id, user_id)` 兜底 |

---

### 3.7 unlikeNote - 取消点赞

**方法签名**：
```java
public void unlikeNote(Long userId, Long noteId)
```

**实现流程**：

```
┌─────────────────────────────────────────────────────────────────┐
│                   unlikeNote 实现流程                           │
├─────────────────────────────────────────────────────────────────┤
│ 第1步：验证笔记存在                                              │
│   └── noteMapper.selectById(noteId)                             │
│   ↓                                                              │
│ 第2步：检查是否已点赞                                            │
│   ├── Redis缓存检查                                              │
│   └── DB检查                                                     │
│   └── 都未点赞 → 抛出"未点赞"                                   │
│   ↓                                                              │
│ 第3步：删除点赞记录                                              │
│   └── noteLikeMapper.deleteByUserAndNote(noteId, userId)       │
│   │   └── SQL: DELETE FROM t_note_like                        │
│   │              WHERE note_id = ? AND user_id = ?             │
│   ↓                                                              │
│ 第4步：减少点赞计数                                              │
│   └── noteMapper.decrementLikeCount(noteId)                    │
│   │   └── SQL: UPDATE t_note SET like_count = like_count - 1  │
│   │              WHERE id = ? AND like_count > 0               │
│   ↓                                                              │
│ 第5步：删除Redis缓存                                             │
│   └── setUserLikedCache(noteId, userId, false)                  │
│   ↓                                                              │
│ 第6步：记录日志                                                  │
└─────────────────────────────────────────────────────────────────┘
```

**关键代码逻辑**：

```java
// 第2步：检查是否已点赞
if (!checkUserLikedFromCache(noteId, userId) && !noteLikeMapper.checkUserLiked(noteId, userId)) {
    throw new BusinessException("未点赞");
}

// 第3步：删除记录
noteLikeMapper.deleteByUserAndNote(noteId, userId);

// 第4步：减少计数（防负数）
noteMapper.decrementLikeCount(noteId);

// 第5步：删除缓存
setUserLikedCache(noteId, userId, false);
```

---

### 3.8 collectNote - 收藏笔记

**方法签名**：
```java
public void collectNote(Long userId, Long noteId)
```

**实现流程**：与 `likeNote` 完全对称

```
┌─────────────────────────────────────────────────────────────────┐
│                   collectNote 实现流程                          │
├─────────────────────────────────────────────────────────────────┤
│ 第1步：验证笔记存在                                              │
│   └── noteMapper.selectById(noteId)                             │
│   ↓                                                              │
│ 第2步：幂等性检查                                                │
│   ├── Redis: checkUserCollectedFromCache()                     │
│   └── DB: noteCollectMapper.checkUserCollected()               │
│   └── 任一存在 → 抛出"已收藏"                                   │
│   ↓                                                              │
│ 第3步：写入收藏记录                                              │
│   └── noteCollectMapper.insert(collect)                        │
│   ↓                                                              │
│ 第4步：更新收藏计数                                              │
│   └── noteMapper.incrementCollectCount(noteId)                │
│   ↓                                                              │
│ 第5步：更新Redis缓存                                             │
│   └── setUserCollectedCache(noteId, userId, true)              │
│   ↓                                                              │
│ 第6步：记录日志                                                  │
└─────────────────────────────────────────────────────────────────┘
```

**关键代码逻辑**：

```java
// 幂等性检查
if (checkUserCollectedFromCache(noteId, userId) || noteCollectMapper.checkUserCollected(noteId, userId)) {
    throw new BusinessException("已收藏");
}

// 写入收藏记录
NoteCollectEntity collect = NoteCollectEntity.builder()
        .noteId(noteId)
        .userId(userId)
        .createTime(LocalDateTime.now())
        .build();
noteCollectMapper.insert(collect);

// 更新计数
noteMapper.incrementCollectCount(noteId);

// 更新缓存
setUserCollectedCache(noteId, userId, true);
```

---

### 3.9 uncollectNote - 取消收藏

**方法签名**：
```java
public void uncollectNote(Long userId, Long noteId)
```

**实现流程**：与 `unlikeNote` 完全对称

```
┌─────────────────────────────────────────────────────────────────┐
│                  uncollectNote 实现流程                         │
├─────────────────────────────────────────────────────────────────┤
│ 第1步：验证笔记存在                                              │
│ 第2步：检查是否已收藏                                            │
│ 第3步：删除收藏记录                                              │
│ 第4步：减少收藏计数                                              │
│ 第5步：删除Redis缓存                                             │
│ 第6步：记录日志                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

### 3.10 deleteNote - 删除笔记

**方法签名**：
```java
public void deleteNote(Long userId, Long noteId)
```

**实现流程**：

```
┌─────────────────────────────────────────────────────────────────┐
│                   deleteNote 实现流程                          │
├─────────────────────────────────────────────────────────────────┤
│ 第1步：查询笔记                                                  │
│   └── noteMapper.selectById(noteId)                             │
│   ↓                                                              │
│ 第2步：验证存在                                                  │
│   └── note == null → 抛出"笔记不存在"                           │
│   ↓                                                              │
│ 第3步：权限校验                                                  │
│   └── note.getUserId().equals(userId)                          │
│   └── 不相等 → 抛出"无权限操作"                                 │
│   ↓                                                              │
│ 第4步：逻辑删除                                                  │
│   ├── note.setDeleted(NoteEntity.DELETED_YES)                  │
│   ├── note.setStatus(NoteEntity.STATUS_USER_DELETED)            │
│   └── noteMapper.updateById(note)                              │
│   ↓                                                              │
│ 第5步：发送ES删除消息                                            │
│   └── queueProducer.sendNoteMessage(note)                      │
│   ↓                                                              │
│ 第6步：记录日志                                                  │
└─────────────────────────────────────────────────────────────────┘
```

**逻辑删除**：
- 不物理删除数据
- 设置 `deleted = 1` 和 `status = 3`（用户删除）
- 异步通知ES删除索引

---

### 3.11 reviewNote - 审核笔记

**方法签名**：
```java
public void reviewNote(Long noteId, boolean approved)
```

**实现流程**：

```
┌─────────────────────────────────────────────────────────────────┐
│                   reviewNote 实现流程                          │
├─────────────────────────────────────────────────────────────────┤
│ 第1步：查询笔记                                                  │
│   └── noteMapper.selectById(noteId)                             │
│   ↓                                                              │
│ 第2步：验证存在                                                  │
│   └── note == null → 抛出"笔记不存在"                           │
│   ↓                                                              │
│ 第3步：审核通过时                                                │
│   ├── auditStatus = 1（通过）                                   │
│   ├── status = 1（正常）                                       │
│   └── 同步到ES                                                  │
│   ↓                                                              │
│ 第4步：审核拒绝时                                                │
│   ├── auditStatus = 2（拒绝）                                   │
│   ├── status = 2（违规）                                       │
│   ├── deleted = 1（隐藏）                                      │
│   ├── rejectReason = "管理员审核拒绝"                          │
│   └── 发送ES删除消息                                            │
│   ↓                                                              │
│ 第5步：记录日志                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 四、核心私有方法

### 4.1 checkSensitiveWords - 敏感词校验

```java
private SensitiveWordCheckResult checkSensitiveWords(Long userId, NoteCreateRequestDTO request)
```

**实现逻辑**：

1. 调用 `sensitiveWordService.findSensitiveWords()` 检测标题和正文中的敏感词
2. 将敏感词替换为 `*` 号（调用 `replaceSensitiveWord()`）
3. 根据敏感词数量判断是否拒绝：
   - ≥3个：标记为 `rejected = true`
   - 1-2个：标记为 `replaced = true`
   - 0个：正常通过

---

### 4.2 saveSensitiveCheckLog - 保存敏感词日志

```java
private void saveSensitiveCheckLog(Long userId, Long noteId, SensitiveWordCheckResult checkResult)
```

**实现逻辑**：

遍历检测到的敏感词，逐条写入 `t_sensitive_check_log` 表，记录：
- content_type：标题/正文
- found_words：发现的敏感词
- check_result：替换/拒绝

---

### 4.3 syncToElasticsearch - ES同步

```java
@Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
public void syncToElasticsearch(NoteEntity note)
```

**实现逻辑**：

1. 使用 `@Retryable` 注解实现重试机制
2. 发送RabbitMQ消息到队列
3. 消费者监听并同步到Elasticsearch
4. 重试策略：3次，间隔1s → 2s → 4s

---

### 4.4 getUserMap - 批量查询用户

```java
private Map<Long, UserEntity> getUserMap(Set<Long> userIds)
```

**实现逻辑**：

1. 批量查询用户：`userMapper.selectBatchIds(userIds)`
2. 转换为Map便于快速查找

---

### 4.5 buildNoteDTO - 构建响应DTO

```java
private NoteResponseDTO buildNoteDTO(NoteEntity note, List<NoteImageEntity> images,
                                      UserEntity user, Long currentUserId)
```

**实现逻辑**：

1. 反序列化tags JSON为List
2. 提取images URL列表
3. 构建NoteResponseDTO对象

---

### 4.6 缓存方法

#### checkUserLikedFromCache

```java
private boolean checkUserLikedFromCache(Long noteId, Long userId)
```

- Key: `note:like:{noteId}:{userId}`
- 检查Redis中是否存在该key

#### setUserLikedCache

```java
private void setUserLikedCache(Long noteId, Long userId, boolean liked)
```

- 点赞时：写入key，过期1小时
- 取消点赞时：删除key
- 异常时仅记录日志，不影响主流程

#### checkUserCollectedFromCache / setUserCollectedCache

与点赞缓存方法对称，key前缀为 `note:collect:`

---

## 五、数据模型

### 5.1 t_note 表（笔记主表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| user_id | BIGINT | 发布者ID |
| title | VARCHAR(200) | 标题 |
| content | TEXT | 正文内容 |
| cover_image | VARCHAR(500) | 封面图 |
| category | VARCHAR(50) | 分类 |
| tags | JSON | 标签数组 |
| like_count | INT | 点赞数 |
| comment_count | INT | 评论数 |
| collect_count | INT | 收藏数 |
| view_count | INT | 浏览数 |
| status | TINYINT | 状态（0待审/1正常/2违规/3删除） |
| audit_status | TINYINT | 审核状态（0待审/1通过/2拒绝） |
| reject_reason | VARCHAR(200) | 拒绝原因 |
| deleted | TINYINT | 逻辑删除 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### 5.2 t_note_like 表（笔记点赞）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| note_id | BIGINT | 笔记ID |
| user_id | BIGINT | 点赞用户ID |
| create_time | DATETIME | 点赞时间 |

**唯一约束**：`uk_note_user(note_id, user_id)`

### 5.3 t_note_collect 表（笔记收藏）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| note_id | BIGINT | 笔记ID |
| user_id | BIGINT | 收藏用户ID |
| create_time | DATETIME | 收藏时间 |

**唯一约束**：`uk_note_user(note_id, user_id)`

---

## 六、异常处理

### 6.1 业务异常

| 异常 | 错误码 | 说明 |
|------|--------|------|
| 笔记不存在 | 400 | 笔记ID不存在或已删除 |
| 已点赞 | 400 | 用户已点赞，不能重复点赞 |
| 未点赞 | 400 | 用户未点赞，无法取消 |
| 已收藏 | 400 | 用户已收藏，不能重复收藏 |
| 未收藏 | 400 | 用户未收藏，无法取消 |
| 无权限操作 | 403 | 非笔记作者无权删除 |

### 6.2 全局异常处理

`GlobalExceptionHandler.java` 统一处理：
- `BusinessException` → 400响应
- `RateLimitException` → 429响应
- 参数校验异常 → 400响应
- 系统异常 → 500响应

---

## 七、相关文件

| 文件 | 说明 |
|------|------|
| `NoteService.java` | 服务接口定义 |
| `NoteServiceImpl.java` | 服务实现（620行） |
| `NoteController.java` | REST接口 |
| `NoteEntity.java` | 笔记实体 |
| `NoteLikeEntity.java` | 点赞实体 |
| `NoteCollectEntity.java` | 收藏实体 |
| `NoteMapper.java` | 笔记Mapper |
| `NoteLikeMapper.java` | 点赞Mapper |
| `NoteCollectMapper.java` | 收藏Mapper |
| `SensitiveWordService.java` | 敏感词服务 |
| `NoteQueueProducer.java` | 消息生产者 |
| `GlobalExceptionHandler.java` | 全局异常处理 |

---

## 八、数据库初始化SQL

```sql
-- 笔记点赞表
CREATE TABLE `t_note_like` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '点赞记录ID',
  `note_id` BIGINT NOT NULL COMMENT '笔记ID',
  `user_id` BIGINT NOT NULL COMMENT '点赞用户ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_note_user` (`note_id`, `user_id`),
  KEY `idx_note_id` (`note_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记点赞表';

-- 笔记收藏表
CREATE TABLE `t_note_collect` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '收藏记录ID',
  `note_id` BIGINT NOT NULL COMMENT '笔记ID',
  `user_id` BIGINT NOT NULL COMMENT '收藏用户ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_note_user` (`note_id`, `user_id`),
  KEY `idx_note_id` (`note_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记收藏表';
```

---

*文档更新时间：2026-04-03*
