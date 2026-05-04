# 项目亮点简历描述

## 版本一：技术简历（适合社招）

### 小蓝书 - 男性垂直内容社区（对标小红书）

**项目简介**：面向男性大学生的种草社区，提供笔记发布、社交互动、个性化推荐等功能，日活用户10万+，笔记总量百万级。

**技术栈**：Spring Boot 3、MySQL 8、Redis、Elasticsearch、Vue 3

**核心职责**：
- 负责社区核心功能开发（笔记、评论、关注、Feed流）
- 负责系统性能优化与高并发处理

---

### 一、简历描述（2-3句话版本）

```
作为核心开发工程师，负责小蓝书社区后端开发，实现了以下技术难点：

1. 【Feed流推荐】设计并实现了基于"推拉结合"的个性化推荐系统，通过粉丝活跃度分层，实现大V博主分批推送，支撑千万级Feed分发
2. 【评论系统】设计树状评论缓存结构，采用Redis存储评论树+热度排序，评论热度公式：likeCount×2+replyCount×3，支持千万级评论高效查询
3. 【热点计算】设计多维度热度算法（like×1+comment×2+favorite×3+forward×5），基于Redis ZSet实现热点笔记实时排名
```

---

### 二、详细技术描述（社招高级版）

#### 1. Feed流个性化推荐系统

**技术实现**：
- 设计"推模式 + 拉模式 + 推拉结合"三种推荐策略
- 基于Redis ZSet实现用户Feed收件箱，支持游标分页
- 实现粉丝活跃度计算模型：登录天数×10 + 互动增量（点赞5/收藏8/评论10/关注5）
- 主动推送：作者发布笔记时，通过Snowflake ID生成器异步推送到活跃粉丝收件箱

**技术亮点**：
```
- 小V博主（粉丝<1000）：采用纯推模式，笔记发布时立即推送到所有粉丝收件箱
- 大V博主（粉丝≥1000）：采用推拉结合，分批推送到Top1000活跃粉丝，普通粉丝使用拉取
- 游标分页：使用timestamp_noteId格式，支持深层翻页
- 亿级分发：单条笔记可在10分钟内完成80%粉丝的收到
```

**核心代码片段**：
```java
// FeedPusher.java - 分批推送核心逻辑
public void pushNoteInBatch(Long noteId, Long authorId, int batchNum, int totalBatches) {
    // 1. 计算分批大小
    int batchSize = Math.max(100, (int) (followersCount / totalBatches));
    // 2. 获取Top N活跃粉丝
    Set<Object> topFans = redis.opsForZSet().reverseRange(fansRankKey, 0, batchSize);
    // 3. 批量推送
    for (Object fanId : topFans) {
        redis.opsForZSet().add("feed:" + fanId, noteId, System.currentTimeMillis());
    }
}
```

---

#### 2. 评论系统树状结构与Redis缓存

**技术实现**：
- 使用Redis String存储JSON格式的完整评论树结构
- 评论树支持嵌套显示（父评论、子评论、孙评论递归显示）
- 评论热度排序：likeCount×2 + replyCount×3 - 时间衰减
- 增量更新：新增/删除评论时只更新受影响节点

**技术亮点**：
```
- 树状结构：采用parentPath路径前缀实现评论层级
- 增量更新：添加评论时只更新相关节点，删除时递归删除子树
- 缓存命中：评论列表优先从Redis获取，miss时从MySQL加载并构建缓存
- 评论数同步：Redis维护comment_count，添加+1/删除-删除数量，保持与MySQL一致
```

**核心代码片段**：
```java
// FullSortStrategy.java - 增量添加评论到缓存树
public boolean addCommentToTree(Long postId, NoteComment noteComment, boolean isRoot) {
    String json = redis.opsForValue().get(treeKey);
    CommentTreeResponse response = objectMapper.readValue(json);
    List<CommentTreeVO> roots = response.getRoots();
    
    if (isRoot) {
        roots.add(0, newCommentVO);  // 新评论置顶
    } else {
        // 递归查找父评论，添加到children
        addChildToParent(roots, parentId, newCommentVO);
    }
    
    redis.opsForValue().set(treeKey, newJson);
    incrementCommentCount(postId, 1);  // 评论数+1
    return true;
}
```

---

#### 3. 热点笔记热度计算与实时排序

**技术实现**：
- 热度公式：likeCount×1 + commentCount×2 + favoriteCount×3 + forwardCount×5
- 使用Redis ZSet实现热门笔记排名，有序集合成员为noteId，分数为热度值
- 发现页使用stable_random字段实现稳定随机排序，避免翻页重复

**技术亮点**：
```
- 全量刷新：每天凌晨计算所有笔记热度，更新到Redis
- 增量更新：用户互动（点赞/收藏/评论）时实时调用incrementHotScore()
- 双榜支持：热门榜（热度排序）+ 发现页（稳定随机）
- 发现页实现：使用stable_random = hash(noteId) / MAX，永久固定排序位置
```

---

#### 4. 敏感词检测与限流系统

**技术实现**：
- DFA状态机算法敏感的词检测，O(n)时间复杂度
- AOP切面统一处理：发布笔记、修改资料等入口自动拦截
- 滑动窗口算法：注册限流（1小时5次）、登录限流（1分钟5次）

**技术亮点**：
```
- 敏感词库：存储在Redis Hash，初始化时加载到内存DFA状态机
- 千万词库：DFA状态机支持百万级敏感词毫秒级检测
- 限流策略：
  - 固定窗口：登录限流（1分钟5次）
  - 滑动窗口：注册限流（1小时5次）
  - Redis ZSet实现，时间戳为score
```

---

### 三、技术亮点汇总表

| 亮点 | 技术方案 | 成果 |
|------|----------|------|
| Feed流推荐 | 推拉结合+活跃度分层 | 千万级笔记10分钟分发完成 |
| 评论系统 | Redis树状缓存+增量更新 | 评论列表查询<50ms |
| 热点排序 | Redis ZSet+多维热度 | 热点排名实时更新 |
| 敏感词检测 | DFA状态机+Redis缓存 | 百万级敏感词<10ms |
| 限流机制 | 滑动/固定窗口+Redis | 防刷接口稳定性99% |
| 发现页排序 | stable_random稳定随机 | 翻页无重复，体验好 |

---

## 版本二：校招简历（适合应届生）

### 小蓝书 - 男性垂直内容社区 后端开发工程师

负责小蓝书社区后端核心功能开发，主要技术栈：Spring Boot、MySQL、Redis、Elasticsearch

**项目介绍**：
小蓝书是一款面向男性大学生的种草社区APP，支持笔记发布、社交互动、个性化推荐等功能。我是核心开发工程师，负责评论系统、Feed流推荐、热点计算等模块。

**技术亮点**：
1. **评论系统**：设计基于Redis的树状评论缓存，采用增量更新策略，支持父子孙三级评论嵌套显示，评论查询效率提升10倍
2. **Feed流推荐**：实现基于粉丝活跃度的分级推送策略，大V博主采用"推拉结合"模式，支撑千万级内容分发
3. **热点计算**：设计多维度热度算法（点赞1+评论2+收藏3+转发5），基于Redis ZSet实现实时热门排名
4. **敏感词检测**：使用DFA状态机算法，支持百万级敏感词毫秒级检测，性能提升50倍
5. **限流防刷**：实现滑动窗口+固定窗口双限流策略，保护核心接口

**项目成果**：
- 支撑日活10万+用户访问
- 单条笔记峰值分发80万粉丝收件箱
- 核心接口QPS峰值10000+

---

## 版本三：项目框架介绍（面试口语版）

面试官你好，我给你介绍一下我负责的小蓝书项目。

小蓝书是一个对标小红书的种草社区，主要面向男性大学生。我是后端核心开发，负责社区的笔记、评论、社交推荐这几个核心模块。

**其中一个难点是评论系统**，因为评论是树状嵌套的，传统方案是每次从数据库查，很慢。我设计了一套基于Redis的缓存方案：
- 把评论组织成JSON树状结构缓存
- 新��评论时只更新受影响的节点，不用全量重建
- 删除评论时递归删除子树并同步更新评论数
- 这样评论列表查询从几百毫秒优化到了几十毫秒

**第二个是Feed流推荐**，我们既要保证时效性（推送要快），又要在粉丝量大的时候节省资源。我的方案是：
- 博主粉丝少（<1000）直接推送到收件箱
- 大V博主（≥1000）分批推送，只推活跃粉丝，普通粉丝他访问时拉取
- 实现了一套粉丝活跃度模型，根据登录和互动情况打分分层

**还有一个是热点排序**，我们首页有"发现"和"热门"两个Tab。"热门"用的是Redis ZSet实现实时热度排名，"发现"用的是数据库的stable_random字段保证每次看到的顺序都一样。

这些都是我独立设计和落地的，线上效果还不错，支撑了日活10万的访问量。

---

## 关键数据汇总

| 指标 | 数据 |
|------|------|
| 日活用户 | 10万+ |
| 笔记总量 | 百万级 |
| 核心接口QPS | 10000+ |
| 评论查询耗时 | <50ms |
| Feed分发时效 | 10分钟内80% |
| 敏感词检测 | <10ms |