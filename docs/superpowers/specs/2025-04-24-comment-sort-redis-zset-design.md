# 评论排序系统设计 - 基于 Redis ZSet 高性能方案

## 一、概述

设计并实现一套高性能评论排序系统，基于 Redis ZSet 实现，支持根评论与子评论分离排序，采用自适应策略（数量少时全量排序，数量多时时间分桶+聚合）。

### 1.1 设计目标

- 替换现有评论排序查询逻辑
- 支持 4 种排序模式：最新、最热、时间正序、时间倒序
- 自适应策略：数量少全量，数量多分桶
- 热点评论优先展示

### 1.2 核心特性

| 特性 | 说明 |
|------|------|
| 根评论/子评论分离 | 分别存储，独立排序 |
| 自适应分桶 | 数量少全量排序，数量多分桶聚合 |
| 热点聚合 | 热点评论优先返回 |
| 游标分页 | 支持大量数据分页 |
| 实时更新 | 点赞/回复/删除同步更新 ZSet |

---

## 二、存储结构设计

### 2.1 策略选择规则

```
评论数量 < 阈值（默认1000）: 全量排序
评论数量 >= 阈值（默认1000）: 时间分桶 + 聚合排序

阈值可通过配置调整：comment.sort.threshold=1000
```

### 2.2 Redis ZSet Key 命名规范

```
# 场景1：评论数量少（<=1000）- 全量排序
post:{postId}:root_comments          # 根评论全量 ZSet
post:{postId}:comment:{rootId}:children  # 子评论全量 ZSet

# 场景2：评论数量多（>1000）- 时间分桶
post:{postId}:bucket:{date}:root_comments    # 根评论时间桶
post:{postId}:hot:root_comments              # 根评论热点桶
post:{postId}:comment:{rootId}:bucket:{date}:children  # 子评论时间桶
post:{postId}:comment:{rootId}:hot:children          # 子评论热点桶
```

### 2.3 Score 计算公式

```
全量排序 score = 点赞数 * 2 + 回复数 * 3 + 时间衰减因子

时间衰减因子 = (当前时间 - 评论创建时间) / 3600 * 0.1
（每小时衰减 0.1，越新权重越高）

时间排序 score = 时间戳（毫秒）
（直接使用时间戳，Redis ZSet 按 score 排序）

最热排序 score = 点赞数 * 2 + 回复数 * 3

正序排序 score = 时间戳
倒序排序 score = -时间戳（取负值实现倒序）
```

### 2.4 数据结构

```java
// Redis 存储的评论数据（JSON序列化）
CommentSortData {
    Long commentId;      // 评论ID
    Long postId;       // 笔记ID
    Long rootId;       // 根评论ID（0表示根评论）
    Long userId;       // 用户ID
    String nickname;   // 昵称
    String avatar;     // 头像
    String content;   // 评论内容
    Integer likeCount;    // 点赞数
    Integer replyCount;   // 回复数
    Long createdAt;      // 创建时间戳（毫秒）
    Integer status;     // 状态
}
```

---

## 三、自适应排序策略

### 3.1 数量少时（全量排序）

```
条件：评论数量 <= 1000

方案：单个 ZSet 存储所有评论
Key: post:{postId}:root_comments

优点：
- 查询简单，一次 ZRANGE 获取
- 无需跨桶聚合
- 性能最优
```

### 3.2 数量多时（分桶+聚合）

```
条件：评论数量 > 1000

方案：时间分桶 + 热点聚合

分桶规则：
- 每个桶最多100条数据（用户指定）
- 超出则新建桶
- 热点评论进入热点桶

聚合顺序：
1. 热点桶（优先返回）
2. 最新时间桶
3. 跨桶聚合（每次从桶中取100条）
```

### 3.3 桶大小配置

```yaml
# 桶大小配置
comment:
  sort:
    threshold: 1000           # 切换分桶策略的数量阈值
    bucket-size: 100          # 每个桶最多100条数据
    hot-threshold: 50         # 热点判��分数（提高）
```

### 3.4 热点判定条件（分数提高）

```
满足以下任一条件进入热点桶：
- 点赞数 >= 20
- 回复数 >= 10
- 综合热度（点赞*2 + 回复*3） >= 50
```

---

## 四、时间分桶策略

### 4.1 分桶规则

| 桶类型 | Key 格式 | 用途 | 保留时间 |
|--------|----------|------|----------|
| 全量桶 | `post:{postId}:root_comments` | 数量少时使用 | 永久 |
| 热点桶 | `post:{postId}:hot:root_comments` | 高互动评论 | 永久 |
| 时间桶 | `post:{postId}:bucket:{date}:root_comments` | 按天存储 | 30天 |

### 4.2 分桶查询流程（数量多时）

```
1. 判断评论数量级别
2. 数量少 → 直接查全量 ZSet
3. 数量多 → 执行分桶聚合：
   a) 先查热点桶 → 获取热点评论
   b) 再查时间桶 → 按时间范围查询
   c) 每次从桶中取100条
   d) 合并结果 → 去重返回
```

### 4.3 跨桶聚合逻辑

```java
// 数量多时查询流程：
1. 热点桶（优先，最多100条）
2. 当天时间桶（最多100条）
3. 昨天时间桶（最多100条）
4. 最近7天时间桶（最多100条）
5. 合并并返回
```

---

## 五、热点评论策略

### 5.1 热点判定（分数提高）

```
综合热度 = 点赞数 * 2 + 回复数 * 3

进入热点桶条件（满足任一）：
- 点赞数 >= 20
- 回复数 >= 10
- 综合热度 >= 50
```

### 5.2 热点桶优先策略

```java
// 分页查询时：
1. 先从热点桶获取（优先返回）
2. 热点桶数据不足时，从时间桶补充
3. 合并返回，保证热点优先
```

---

## 六、排序模式

### 6.1 支持的排序模式

| 模式 | 字段 | 说明 | Score |
|------|------|------|-------|
| latest | sort=latest | 最新排序，按创建时间倒序 | createdAt |
| hottest | sort=hottest | 综合热度排序 | like*2 + reply*3 |
| time_asc | sort=time_asc | 时间正序 | createdAt |
| time_desc | sort=time_desc | 时间倒序 | -createdAt |

### 6.2 默认排序

默认使用 `hottest`（综合热度）排序

---

## 七、API 设计

### 7.1 获取根评论列表

```
GET /api/comment/sorted/{postId}/roots?sort=hottest&cursor=xxx&size=20

Response:
{
    "data": [CommentSortData...],
    "hasMore": true/false,
    "nextCursor": "xxx"
}
```

### 7.2 获取子评论列表

```
GET /api/comment/sorted/{postId}/children/{rootId}?sort=hottest&cursor=xxx&size=20
```

### 7.3 发表评论

```
POST /api/comment/sorted
{
    "postId": 400858,
    "parentId": 0,  // 0表示根评论
    "content": "评论内容"
}
```

### 7.4 点赞评论

```
POST /api/comment/sorted/{commentId}/like
```

### 7.5 删除评论

```
DELETE /api/comment/sorted/{commentId}
```

---

## 八、核心代码模块

### 8.1 模块结构

```
comment-sort/
├── config/
│   └── CommentSortConfig.java         # 配置类
├── service/
│   ├── ICommentSortService.java      # 接口
│   └── impl/
│       └── CommentSortServiceImpl.java  # 实现
├── strategy/
│   ├── FullSortStrategy.java         # 全量排序策略
│   ├── TimeBucketStrategy.java       # 时间分桶策略
│   ├── HotTopicStrategy.java      # 热点聚合策略
│   └── ScoreCalculator.java     # 热度计算
└── util/
    └── CursorUtils.java         # 游标工具
```

### 8.2 核心方法

```java
public interface ICommentSortService {
    // 根评论排序查询
    List<CommentSortData> getRootComments(Long postId, String sort, String cursor, int size);
    
    // 子评论排序查询
    List<CommentSortData> getChildComments(Long postId, Long rootId, String sort, String cursor, int size);
    
    // 发表评论
    CommentSortData addComment(Long userId, Long postId, Long parentId, String content);
    
    // 点赞评论
    void likeComment(Long commentId);
    
    // 取消点赞
    void unlikeComment(Long commentId);
    
    // 删除评论（级联删除子评论）
    void deleteComment(Long commentId, Long userId);
    
    // 初始化评论排序（从数据库迁移）
    void initCommentSort(Long postId);
}
```

---

## 九、高并发优化

### 9.1 优化策略

| 场景 | 方案 |
|------|------|
| 评论数量少 | 单个 ZSet 全量排序，一次查询 |
| 评论数量多 | 时间分桶，每次从桶中取100条 |
| 热点数据 | 热点桶单独存储，优先返回 |
| 分页性能 | 游标分页，避免 OFFSET |
| 并发写入 | 异步更新 ZSet |

### 9.2 并发考虑

- 写入时使用 Redis 事务/管道
- 热点桶更新异步执行
- 时间桶按天自动创建

### 9.3 数据一致性

- MySQL 作为数据源
- Redis 作为排序索引
- 定期全量同步校正

---

## 十、与现有系统关系

**完全替换现有评论排序查询逻辑：**

- 新增 `CommentSortService` 处理排序查询
- 保留 `CommentService` 处理数据存储
- 评论数据源仍然使用 MySQL
- Redis 仅用于排序索引

---

## 十一、实施计划

### 阶段一：基础功能
1. 创建 CommentSortService
2. 实现全量排序 ZSet
3. 实现四种排序模式

### 阶段二：分桶策略
1. 实现自适应分桶（数量阈值判断）
2. 实现时间分桶
3. 实现热点桶
4. 每次从桶中取100条数据

### 阶段三：API 对接
1. 新增排序 API 接口
2. 替换现有评论查询
3. 同步更新逻辑

### 阶段四：优化
1. 游标分页优化
2. 并发写入优化
3. 测试压测