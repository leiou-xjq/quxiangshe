# 开发任务清单

## 任务总览

| 模块 | 子功能 | 代码文件 | 预计工时 |
|------|--------|----------|----------|
| 公共模块 | 项目初始化 | pom.xml, application.yml | 2h |
| 公共模块 | 通用工具 | JwtUtil, ResponseWrapper | 4h |
| 公共模块 | 异常处理 | BusinessException, GlobalExceptionHandler | 2h |
| 公共模块 | 配置类 | RedisConfig, RabbitMQConfig | 4h |
| 认证模块 | 用户实体 | UserEntity, UserMapper | 2h |
| 认证模块 | 注册接口 | AuthController, AuthService | 4h |
| 认证模块 | 登录接口 | JwtAuthenticationFilter | 4h |
| 认证模块 | 令牌刷新 | RefreshTokenService | 2h |
| 用户模块 | 用户服务 | UserService, UserProfileVO | 2h |
| 用户模块 | 关注功能 | UserFollowEntity, UserFollowMapper | 4h |
| 动态模块 | 动态实体 | PostEntity, PostMapper | 2h |
| 动态模块 | 发布动态 | PostController, PostService | 4h |
| 动态模块 | 点赞功能 | PostLikeEntity, PostLikeMapper | 2h |
| Feed模块 | Feed实体 | FeedEntity, FeedMapper | 2h |
| Feed模块 | 推模式 | FeedPushService | 4h |
| Feed模块 | 拉模式 | FeedPullService | 4h |
| Feed模块 | 游标分页 | CursorPageService | 2h |
| 评论模块 | 评论实体 | CommentEntity, CommentMapper | 2h |
| 评论模块 | 时间分桶 | TimeBucketStrategy | 2h |
| 评论模块 | 异步写入 | CommentAsyncWriteTask | 2h |
| 评论模块 | 两层查询 | CommentTreeService | 4h |
| AI模块 | AI配置 | SpringAIConfig | 2h |
| AI模块 | 摘要生成 | AISummaryService | 4h |
| AI模块 | 消费者 | AISummaryConsumer | 2h |
| 限流模块 | 滑动窗口 | RateLimitService | 4h |
| 搜索模块 | ES配置 | ElasticsearchConfig | 2h |
| 搜索模块 | 索引同步 | PostSyncToESService | 4h |
| 搜索模块 | 搜索接口 | SearchController, SearchService | 4h |

---

## 详细任务拆分

### 模块1: 公共模块

#### 1.1 项目初始化
- **文件**: `pom.xml`
- **内容**: Spring Boot3.2, Spring Security6.2, Spring AI1.0, MySQL8, Redis7, RabbitMQ3.12, ES8, Lombok依赖
- **文件**: `application.yml`
- **内容**: 数据源/Redis/RabbitMQ/ES配置

#### 1.2 通用工具
- **文件**: `com.quxiangshe.common.util.JwtUtil`
- **内容**: AccessToken生成/解析/验证, RefreshToken生成
- **文件**: `com.quxiangshe.common.util.ResponseWrapper`
- **内容**: 统一响应包装, 错误码定义

#### 1.3 异常处理
- **文件**: `com.quxiangshe.common.exception.BusinessException`
- **内容**: 业务异常定义, 错误码构造
- **文件**: `com.quxiangshe.common.exception.GlobalExceptionHandler`
- **内容**: 全局异常处理, 参数校验异常

#### 1.4 配置类
- **文件**: `com.quxiangshe.common.config.RedisConfig`
- **内容**: RedisTemplate, StringRedisSerializer配置
- **文件**: `com.quxiangshe.common.config.RabbitMQConfig`
- **内容**: FanoutExchange, DirectExchange, Queue声明
- **文件**: `com.quxiangshe.common.config.SecurityConfig`
- **内容**: Spring Security过滤器链, 路径权限配置
- **文件**: `com.quxiangshe.common.config.MybatisPlusConfig`
- **内容**: 分页插件, 自动填充配置

---

### 模块2: 认证模块

#### 2.1 用户实体
- **文件**: `com.quxiangshe.auth.entity.UserEntity`
- **内容**: 用户实体, TableName, TableId注解
- **文件**: `com.quxiangshe.auth.mapper.UserMapper`
- **内容**: Insert, Select, Update注解方法

#### 2.2 注册接口
- **文件**: `com.quxiangshe.auth.controller.AuthController`
- **内容**: `/register` POST接口
- **文件**: `com.quxiangshe.auth.dto.RegisterRequestDTO`
- **内容**: username, password, phone, email, nickname
- **文件**: `com.quxiangshe.auth.service.AuthService`
- **内容**: 接口定义
- **文件**: `com.quxiangshe.auth.service.impl.AuthServiceImpl`
- **内容**: 注册逻辑, 用户名唯一校验, 密码加密

#### 2.3 登录接口
- **文件**: `com.quxiangshe.auth.dto.LoginRequestDTO`
- **内容**: username, password
- **文件**: `com.quxiangshe.auth.dto.LoginResponseDTO`
- **内容**: accessToken, refreshToken, expiresIn
- **文件**: `com.quxiangshe.auth.service.impl.AuthServiceImpl` - login方法
- **内容**: 密码校验, JWT生成, Redis存RefreshToken

#### 2.4 JWT过滤器
- **文件**: `com.quxiangshe.auth.filter.JwtAuthenticationFilter`
- **内容**: 从Header解析Token, 设置SecurityContext
- **文件**: `com.quxiangshe.auth.filter.JwtAuthenticationEntryPoint`
- **内容**: 认证失败返回401

#### 2.5 令牌刷新
- **文件**: `com.quxiangshe.auth.service.RefreshTokenService`
- **内容**: Redis存储/验证/删除RefreshToken
- **文件**: `AuthController.refresh()` 方法
- **内容**: `/refresh` POST接口

#### 2.6 登出
- **文件**: `AuthController.logout()` 方法
- **内容**: 删除Redis中RefreshToken

---

### 模块3: 用户模块

#### 3.1 用户服务
- **文件**: `com.quxiangshe.user.service.UserService`
- **内容**: 接口定义
- **文件**: `com.quxiangshe.user.service.impl.UserServiceImpl`
- **内容**: getUserProfile, getUserById
- **文件**: `com.quxiangshe.user.vo.UserProfileVO`
- **内容**: 用户主页VO

#### 3.2 关注功能
- **文件**: `com.quxiangshe.user.entity.UserFollowEntity`
- **内容**: 关注关系实体
- **文件**: `com.quxiangshe.user.mapper.UserFollowMapper`
- **内容**: 关注/取消关注/粉丝列表/关注列表
- **文件**: `com.quxiangshe.user.controller.UserController`
- **内容**: `/follow/{userId}` POST/DELETE

---

### 模块4: 动态模块

#### 4.1 动态实体
- **文件**: `com.quxiangshe.post.entity.PostEntity`
- **内容**: 动态实体, aiSummary字段
- **文件**: `com.quxiangshe.post.mapper.PostMapper`
- **内容**: Insert, Select, Update, 分页查询

#### 4.2 发布动态
- **文件**: `com.quxiangshe.post.controller.PostController`
- **内容**: `/posts` POST接口
- **文件**: `com.quxiangshe.post.dto.PostCreateRequestDTO`
- **内容**: content, mediaUrls, mediaTypes, visibility
- **文件**: `com.quxiangshe.post.service.PostService`
- **内容**: 接口定义
- **文件**: `com.quxiangshe.post.service.impl.PostServiceImpl`
- **内容**: 保存动态, 发送AI摘要任务到RabbitMQ

#### 4.3 删除动态
- **文件**: `PostController.delete()` 方法
- **内容**: `/posts/{postId}` DELETE, 逻辑删除

#### 4.4 点赞功能
- **文件**: `com.quxiangshe.post.entity.PostLikeEntity`
- **内容**: 点赞实体
- **文件**: `com.quxiangshe.post.mapper.PostLikeMapper`
- **内容**: 点赞/取消点赞/点赞列表
- **文件**: `PostController.like()` 方法
- **内容**: `/posts/{postId}/like` POST/DELETE

---

### 模块5: Feed流模块

#### 5.1 Feed实体
- **文件**: `com.quxiangshe.feed.entity.FeedEntity`
- **内容**: Feed流实体
- **文件**: `com.quxiangshe.feed.mapper.FeedMapper`
- **内容**: Insert, SelectByUserId

#### 5.2 推模式服务
- **文件**: `com.quxiangshe.feed.service.FeedPushService`
- **内容**: 获取粉丝列表, LPUSH到Redis inbox
- **文件**: `com.quxiangshe.feed.config.RabbitMQFeedConfig`
- **内容**: fanout.exchange声明

#### 5.3 拉模式服务
- **文件**: `com.quxiangshe.feed.service.FeedPullService`
- **内容**: 从MySQL feed表拉取兜底
- **文件**: `com.quxiangshe.feed.service.FeedMergeService`
- **内容**: 合并Redis+MySQL结果, 按时间排序

#### 5.4 游标分页
- **文件**: `com.quxiangshe.feed.service.CursorPageService`
- **内容**: 基于lastPostTime+lastPostId分页
- **文件**: `com.quxiangshe.feed.controller.FeedController`
- **内容**: `/feed` GET接口, cursor, postTime, size参数

---

### 模块6: 评论模块

#### 6.1 评论实体
- **文件**: `com.quxiangshe.comment.entity.CommentEntity`
- **内容**: 评论实体, parentId, rootId, timeBucket, level
- **文件**: `com.quxiangshe.comment.mapper.CommentMapper`
- **内容**: Insert, SelectByPostId, batchInsert

#### 6.2 时间分桶策略
- **文件**: `com.quxiangshe.comment.strategy.TimeBucketStrategy`
- **内容**: 计算timeBucket = UNIX_TIMESTAMP/3600

#### 6.3 异步写入
- **文件**: `com.quxiangshe.comment.task.CommentAsyncWriteTask`
- **内容**: @Scheduled(fixedRate = 5000), 批量从Redis写入MySQL
- **文件**: `com.quxiangshe.comment.queue.CommentQueueProducer`
- **内容**: LPUSH到comment:queue

#### 6.4 两层查询
- **文件**: `com.quxiangshe.comment.service.CommentTreeService`
- **内容**: 查询顶级评论+直接子评论, 组装两层扁平
- **文件**: `com.quxiangshe.comment.controller.CommentController`
- **内容**: `/posts/{postId}/comments` GET接口

#### 6.5 发布评论
- **文件**: `CommentController.create()` 方法
- **内容**: `/posts/{postId}/comments` POST, 写入Redis队列

#### 6.6 评论点赞
- **文件**: `com.quxiangshe.comment.entity.CommentLikeEntity`
- **内容**: 评论点赞实体
- **文件**: `CommentController.like()` 方法
- **内容**: `/comments/{commentId}/like` POST/DELETE

---

### 模块7: AI摘要模块

#### 7.1 Spring AI配置
- **文件**: `com.quxiangshe.ai.config.SpringAIConfig`
- **内容**: ChatClient配置, 模型客户端

#### 7.2 摘要服务
- **文件**: `com.quxiangshe.ai.service.AISummaryService`
- **内容**: 调用Spring AI生成摘要
- **文件**: `com.quxiangshe.ai.service.impl.AISummaryServiceImpl`
- **内容**: 生成摘要, 降级处理

#### 7.3 RabbitMQ消费者
- **文件**: `com.quxiangshe.ai.consumer.AISummaryConsumer`
- **内容**: 监听ai.summary.queue, 调用AI服务, 更新Post
- **文件**: `com.quxiangshe.ai.config.RabbitMQAIConfig`
- **内容**: ai.summary.queue声明

---

### 模块8: 限流模块

#### 8.1 滑动窗口限流
- **文件**: `com.quxiangshe.ratelimit.service.RateLimitService`
- **内容**: ZSet实现滑动窗口, 检查请求频率
- **文件**: `com.quxiangshe.ratelimit.filter.RateLimitFilter`
- **内容**: 请求限流检查, 返回429

#### 8.2 限流配置
- **文件**: `com.quxiangshe.ratelimit.config.RateLimitConfig`
- **内容**: windowSize=1秒, maxRequest=100

---

### 模块9: 搜索模块

#### 9.1 ES配置
- **文件**: `com.quxiangshe.search.config.ElasticsearchConfig`
- **内容**: ElasticsearchClient配置

#### 9.2 索引同步
- **文件**: `com.quxiangshe.search.service.PostSyncToESService`
- **内容**: 动态发布时同步到ES
- **文件**: `com.quxiangshe.search.document.PostDocument`
- **内容**: ES文档映射

#### 9.3 搜索服务
- **文件**: `com.quxiangshe.search.service.SearchService`
- **内容**: 关键词搜索, 分页返回
- **文件**: `com.quxiangshe.search.controller.SearchController`
- **内容**: `/search/posts` GET接口

---

## 核心代码文件对照表

| 模块 | 类名 | 路径 |
|------|------|------|
| 公共 | JwtUtil | com.quxiangshe.common.util.JwtUtil |
| 公共 | ResponseWrapper | com.quxiangshe.common.util.ResponseWrapper |
| 公共 | BusinessException | com.quxiangshe.common.exception.BusinessException |
| 公共 | GlobalExceptionHandler | com.quxiangshe.common.exception.GlobalExceptionHandler |
| 公共 | RedisConfig | com.quxiangshe.common.config.RedisConfig |
| 公共 | RabbitMQConfig | com.quxiangshe.common.config.RabbitMQConfig |
| 公共 | SecurityConfig | com.quxiangshe.common.config.SecurityConfig |
| 认证 | AuthController | com.quxiangshe.auth.controller.AuthController |
| 认证 | AuthService | com.quxiangshe.auth.service.AuthService |
| 认证 | AuthServiceImpl | com.quxiangshe.auth.service.impl.AuthServiceImpl |
| 认证 | JwtAuthenticationFilter | com.quxiangshe.auth.filter.JwtAuthenticationFilter |
| 用户 | UserService | com.quxiangshe.user.service.UserService |
| 用户 | UserController | com.quxiangshe.user.controller.UserController |
| 动态 | PostController | com.quxiangshe.post.controller.PostController |
| 动态 | PostService | com.quxiangshe.post.service.PostService |
| 动态 | PostServiceImpl | com.quxiangshe.post.service.impl.PostServiceImpl |
| Feed | FeedController | com.quxiangshe.feed.controller.FeedController |
| Feed | FeedPushService | com.quxiangshe.feed.service.FeedPushService |
| Feed | FeedPullService | com.quxiangshe.feed.service.FeedPullService |
| Feed | FeedMergeService | com.quxiangshe.feed.service.FeedMergeService |
| 评论 | CommentController | com.quxiangshe.comment.controller.CommentController |
| 评论 | CommentService | com.quxiangshe.comment.service.CommentService |
| 评论 | CommentTreeService | com.quxiangshe.comment.service.CommentTreeService |
| 评论 | CommentAsyncWriteTask | com.quxiangshe.comment.task.CommentAsyncWriteTask |
| AI | AISummaryService | com.quxiangshe.ai.service.AISummaryService |
| AI | AISummaryConsumer | com.quxiangshe.ai.consumer.AISummaryConsumer |
| 限流 | RateLimitService | com.quxiangshe.ratelimit.service.RateLimitService |
| 限流 | RateLimitFilter | com.quxiangshe.ratelimit.filter.RateLimitFilter |
| 搜索 | SearchController | com.quxiangshe.search.controller.SearchController |
| 搜索 | SearchService | com.quxiangshe.search.service.SearchService |

---

## 执行顺序建议

1. **第一周**: 公共模块(配置/工具/异常) + 认证模块
2. **第二周**: 用户模块 + 动态模块
3. **第三周**: Feed流模块(推拉模式 + 分页)
4. **第四周**: 评论模块(异步写入 + 两层查询)
5. **第五周**: AI摘要模块 + 限流模块
6. **第六周**: 搜索模块 + 集成测试 + 性能优化
