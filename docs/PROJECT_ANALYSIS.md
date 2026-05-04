# 小蓝书项目完整分析文档

> 项目原名：趣享社
> 重构定位：面向男性大学生的垂直内容社区，对标小红书
> 文档生成时间：2026年4月

---

## 一、项目概述

### 1.1 技术栈明细

| 层级 | 技术选型 | 版本 |
|------|----------|------|
| 后端框架 | Spring Boot | 3.2.1 |
| 安全框架 | Spring Security + JWT | jjwt 0.12.3 |
| ORM | MyBatis Plus | 3.5.5 |
| 数据库 | MySQL | 8.0+ |
| 缓存 | Redis | - |
| 搜索 | Elasticsearch | - |
| 对象存储 | 阿里云OSS | - |
| API文档 | Knife4j | - |
| 前端框架 | Vue | 3.4 |
| 构建工具 | Vite | 5.0 |
| UI组件库 | Element Plus | - |
| 状态管理 | Pinia | - |
| HTTP客户端 | Axios | - |

### 1.2 项目目录结构

```
quxiansghe-project/
├── backend/                          # 后端服务 (Spring Boot 3)
│   ├── src/main/
│   │   ├── java/com/quxiangshe/backend/
│   │   │   ├── config/              # 配置类
│   │   │   ├── controller/           # 控制器 (11个)
│   │   │   ├── service/              # 业务逻辑 (18个Service接口)
│   │   │   ├── service/impl/         # 业务实现
│   │   │   ├── mapper/               # 数据访问 (12个Mapper)
│   │   │   ├── entity/               # 实体类 (15个)
│   │   │   ├── dto/                  # 数据传输对象
│   │   │   ├── vo/                   # 视图对象
│   │   │   ├── exception/            # 异常处理
│   │   │   ├── security/             # 安全认证
│   │   │   ├── util/                # 工具类
│   │   │   ├── component/             # 组件
│   │   │   ├── aspect/               # AOP切面
│   │   │   ├── annotation/           # 自定义注解
│   │   │   ├── scheduler/           # 定时任务
│   │   │   └── document/            # ES文档对象
│   │   └── resources/
│   │       ├── application.yml       # 配置文件
│   │       └── mapper/               # MyBatis XML
│   └── pom.xml
│
├── frontend/                         # 前端服务 (Vue 3 + Vite)
│   ├── src/
│   │   ├── api/                     # API接口 (8个文件)
│   │   ├── router/                   # 路由配置
│   │   ├── stores/                   # Pinia状态管理
│   │   ├── views/                    # 页面组件 (14个)
│   │   ├── styles/                   # 样式
│   │   └── stores/user.js           # 用户状态管理
│   ├── package.json
│   └── vite.config.js
│
└── docs/                            # 文档目录
```

---

## 二、已实现功能模块总结

### 2.1 用户认证模块

#### 2.1.1 功能清单

| 功能 | 接口路径 | HTTP方法 | 说明 |
|------|----------|----------|------|
| 用户注册 | /auth/register | POST | 用户名密码注册，60秒内最多3次（滑动窗口限流） |
| 用户登录 | /auth/login | POST | 用户名密码登录，1分钟内最多5次（固定窗口限流） |
| 用户登出 | /auth/logout | POST | 清除用户登录状态 |
| 刷新Token | /auth/refresh | POST | 使用RefreshToken刷新AccessToken |
| 发送验证码 | /auth/send-code | POST | 发送手机验证码，60秒内最多1次 |
| 手机号登录 | /auth/phone-login | POST | 手机号+验证码登录 |
| 手机号注册 | /auth/phone-register | POST | 手机号+验证码注册 |
| 发送重置码 | /auth/reset-code | POST | 发送密码重置验证码，60秒内最多3次 |
| 重置密码 | /auth/reset-password | POST | 使用验证码重置密码 |
| 微信登录 | /auth/wechat-login | POST | 微信授权登录 |

#### 2.1.2 核心方法实现

**AuthServiceImpl.register(RegisterRequest request, String ipAddress)**
- 验证注册类型、密码强度
- 检查用户名/手机号/邮箱唯一性
- BCrypt密码加密存储
- 生成JWT令牌（AccessToken + RefreshToken）
- 限流控制：滑动窗口算法

**AuthServiceImpl.login(LoginRequest request, String ipAddress)**
- 验证用户名/密码
- 错误登录计数，固定窗口限流（5次/分钟）
- 记录最后登录IP和时间
- 生成JWT令牌

**JwtUtil**
- 使用jjwt库生成和验证JWT
- AccessToken：2小时过期
- RefreshToken：7天过期
- 支持Token刷新机制

---

### 2.2 用户信息模块

#### 2.2.1 功能清单

| 功能 | 接口路径 | HTTP方法 | 说明 |
|------|----------|----------|------|
| 获取当前用户 | /users/me | GET | 获取已登录用户基本信息 |
| 更新当前用户 | /users/me | PUT | 更新头像、昵称等信息 |
| 修改密码 | /users/me/password | PUT | 需验证原密码 |
| 获取指定用户 | /users/{id} | GET | 获取用户公开信息 |

#### 2.2.2 核心方法实现

**UserServiceImpl.updateUser(Long userId, UpdateUserRequest request)**
- 更新用户基本信息（nickname、avatar、bio、gender、birthday等）
- 敏感词检测（AOP统一处理）

**UserServiceImpl.changePassword(Long userId, ChangePasswordRequest request)**
- 验证原密码
- BCrypt加密新密码
- 更新到数据库

---

### 2.3 笔记模块

#### 2.3.1 功能清单

| 功能 | 接口路径 | HTTP方法 | 说明 |
|------|----------|----------|------|
| 发布笔记 | /note/create | POST | 含敏感词检测AOP |
| 获取笔记列表 | /note/list | GET | 分页获取 |
| 发现精彩 | /note/discover | GET | 稳定随机排序，游标分页 |
| 热门笔记 | /note/popular | GET | 热度排序，游标分页 |
| 获取我的笔记 | /note/my | GET | 当前用户笔记列表 |
| 获取用户笔记 | /note/notes-by/{userId} | GET | 指定用户笔记 |
| 获取我的收藏 | /note/favorites | GET | 收藏列表 |
| 获取获赞数 | /note/my/likes-count | GET | 笔记获赞总数 |
| 获取笔记详情 | /note/{noteId} | GET | 单条笔记详情 |
| 删除笔记 | /note/{noteId} | DELETE | 仅发布者可删除 |
| 点赞笔记 | /note/{noteId}/like | POST | 点赞笔记 |
| 取消点赞 | /note/{noteId}/like | DELETE | 取消点赞 |
| 收藏笔记 | /note/{noteId}/favorite | POST | 收藏笔记 |
| 取消收藏 | /note/{noteId}/favorite | DELETE | 取消收藏 |
| 上传图片 | /note/upload | POST | 上传到OSS，限制5MB |
| 敏感词检测 | /note/validate | POST | 文本敏感词检测 |

#### 2.3.2 核心方法实现

**NoteServiceImpl.createNote(Long userId, CreateNoteRequest request)**
```
1. 接收创建请求（title、content、images、tags、location）
2. 敏感词检测（AOP自动触发）
3. 生成stable_random（用于发现页稳定排序）
4. 保存到数据库（MySQL）
5. 同步数据到Elasticsearch
6. 推送笔记到粉丝Feed流（IFeedService）
7. 初始化热度值（likeCount×1 + commentCount×2 + favoriteCount×3 + forwardCount×5）
```

**NoteServiceImpl.getDiscoverNotes(String cursor, int size, Long userId)**
```
1. 使用stable_random字段实现稳定随机排序
2. 游标分页格式：stable_random_noteId
3. 每次查询后更新游标供下次使用
4. 过滤当前用户已浏览笔记
```

**NoteServiceImpl.getPopularNotes(String cursor, int size, Long userId)**
```
1. 从Redis Hot Rank获取（ZSet结构）
2. 热度公式：likeCount×1 + commentCount×2 + favoriteCount×3 + forwardCount×5
3. 游标分页格式：timestamp_noteId
4. 访问时检查是否需要刷新热度
```

**热度更新策略**
- 实时增量：互动时调用incrementHotScore()
- 定时全量刷新：HotScoreScheduler每天执行

---

### 2.4 评论模块

#### 2.4.1 功能清单

| 功能 | 接口路径 | HTTP方法 | 说明 |
|------|----------|----------|------|
| 添加评论 | /note/comment | POST | 用户登录后添加，自动更新Redis |
| 获取评论列表 | /note/{noteId}/comments | GET | 使用Redis树状结构 |
| 删除评论 | /note/comment/{commentId} | DELETE | 发布者/笔记所有者可删除 |
| 获取根评论 | /comment/sorted/{postId}/roots | GET | 分页获取根评论 |
| 获取子评论 | /comment/sorted/{postId}/children/{rootId} | GET | 分页获取子评论 |
| 评论点赞 | /comment/sorted/{commentId}/like | POST | 点赞评论 |
| 取消评论点赞 | /comment/sorted/{commentId}/like | DELETE | 取消点赞 |
| 初始化评论排序 | /comment/sorted/init/{postId} | POST | 同步到Redis |
| 评论状态查询 | /comment/sorted/status/{postId} | GET | 查询Redis中的状态 |

#### 2.4.2 核心方法实现

**CommentServiceImpl.addComment(Long userId, CreateCommentRequest requestBody)**
```
1. 构建评论实体（noteId、userId、parentId、content）
2. 设置rootId：parentId=0时rootId=0，否则rootId=父评论的rootId或parentId
3. 保存到MySQL
4. 更新Note表的commentCount+1
5. 增量更新Redis comment_tree
6. 增量更新Redis comment_count+1
7. 发送通知（INotificationService）
```

**CommentServiceImpl.deleteComment(Long commentId, Long userId, boolean isNoteOwner)**
```
1. 验证权限（评论发布者或笔记所有者）
2. 递归删除所有子评论（通过parentId递归）
3. 更新note表的commentCount为剩余状态数量
4. 增量更新Redis：removeCommentAndChildrenFromTree()
5. 增量更新Redis comment-count
6. 刷新Redis comment_tree
```

**FullSortStrategy评论树缓存**
```
- Key: post:{noteId}:comment_tree
- Value: JSON格式的树状结构
- 存储内容：commentId、parentId、rootId、userId、nickname、avatar、content、likeCount、replyCount、createdAt、children[]
- 过期时间：永不过期（添加/删除时更新）
- 评论数Key: post:{noteId}:comment_count
```

**评论热度计算**
```
- 公式：likeCount×2 + replyCount×3 - 时间衰减
- 排序支持：hottest（热度）、time_desc（时间倒序）
```

---

### 2.5 关注模块

#### 2.5.1 功能清单

| 功能 | 接口路径 | HTTP方法 | 说明 |
|------|----------|----------|------|
| 关注用户 | /follow/{userId} | POST | 关注指定用户 |
| 取消关注 | /follow/{userId} | DELETE | 取消关注 |
| 获取关注列表 | /follow/following | GET | 游标分页 |
| 获取粉丝列表 | /follow/followers | GET | 游标分页 |
| 获取关注状态 | /follow/status/{userId} | GET | 检查是否已关注 |
| 获取关注数 | /follow/count/following/{userId} | GET | 获取关注数量 |
| 获取粉丝数 | /follow/count/followers/{userId} | GET | 获取粉丝数量 |

#### 2.5.2 核心方法实现

**FollowServiceImpl.follow(Long followerId, Long followingId)**
```
1. 检查是否重复关注
2. 保存关注关系到MySQL
3. 增量更新双方粉丝/关注数缓存
4. 发送关注通知
5. 初始化新粉丝的活跃度排名数据
```

**Feed推送机制**
- 推模式：作者发布笔记时主动推送
- 拉模式：用户访问时主动拉取关注者的笔记
- 推拉结合：活跃粉丝用推，普通粉丝用拉

---

### 2.6 Feed流模块

#### 2.6.1 功能清单

| 功能 | 接口路径 | HTTP方法 | 说明 |
|------|----------|----------|------|
| 获取Feed流 | /feed | GET | 个性化推荐，游标分页 |
| 初始化粉丝活跃度 | /feed/init-fans-activity/{authorId} | POST | 初始化Redis粉丝活跃度 |

#### 2.6.2 核心方法实现

**FeedServiceImpl.getFeed(Long userId, String cursor, int size)**
```
1. 获取用户的关注列表
2. 从Redis feed:收件箱获取笔记ID
3. 游标分页：格式timestamp_noteId
4. 获取笔��详��（需过滤黑名单用户）
5. 混合推荐：收件箱 + 发现页推荐
```

**FeedPusher推送到粉丝收件箱**
```
1. 获取作者的粉丝列表
2. 计算活跃度分数
3. 分批推送：活跃粉丝用推，普通粉丝用拉
4. 推送模式选择：
   - 小V博主（粉丝<1000）：纯推模式
   - 大V博主（粉丝≥1000）：推拉结合
```

---

### 2.7 搜索模块（基于Elasticsearch）

#### 2.7.1 功能清单

| 功能 | 接口路径 | HTTP方法 | 说明 |
|------|----------|----------|------|
| 搜索笔记 | /search/notes | GET | 模糊搜索+热度排序 |
| 搜索用户 | /search/users | GET | 模糊搜索 |
| 创建索引 | /search/index | POST | 创建ES索引结构 |
| 全量同步 | /search/sync | POST | 同步所有数据 |
| 同步笔记 | /search/sync/note/{noteId} | POST | 同步单条笔记 |
| 同步用户 | /search/sync/user/{userId} | POST | 同步单个用户 |
| 删除笔记索引 | /search/note/{noteId} | DELETE | 删除笔记索引 |
| 删除用户索引 | /search/user/{userId} | DELETE | 删除用户索引 |

#### 2.7.2 核心方法实现

**NoteDocument（ES笔记索引结构）**
```json
{
  "id": "笔记ID",
  "title": "标题",
  "content": "内容",
  "tags": "标签",
  "userId": "作者ID",
  "nickname": "作者昵称",
  "avatar": "作者头像",
  "likeCount": "点赞数",
  "commentCount": "评论数",
  "favoriteCount": "收藏数",
  "hotScore": "热度分数",
  "createdAt": "创建时间"
}
```

**SearchServiceImpl.searchNotes(String keyword, int size, List<Object> searchAfter)**
```
1. 构建ES查询：multi_match（title、content、tags）
2. 热度排序：hotScore降序 + createdAt降序
3. 游标分页：searchAfter格式[hotScore, createdAt, id]
4. 返回结果和nextSearchAfter
```

---

### 2.8 通知模块

#### 2.8.1 功能清单

| 功能 | 接口路径 | HTTP方法 | 说明 |
|------|----------|----------|------|
| 获取通知列表 | /notification/list | GET | 获取通知列表 |
| 获取未读数量 | /notification/unread-count | GET | 未读数量 |
| 标记已读 | /notification/read/{id} | PUT | 单条已读 |
| 全部标记已读 | /notification/read-all | PUT | 全部已读 |
| 删除通知 | /notification/{id} | DELETE | 删除通知 |

#### 2.8.2 通知类型

| 类型码 | 说明 | 触发场景 |
|--------|------|----------|
| 1 | 点赞通知 | 他人点赞笔记/评论 |
| 2 | 评论通知 | 他人评论笔记 |
| 3 | 关注通知 | 他人关注用户 |

---

### 2.9 用户活跃度模块

#### 2.9.1 核心方法实现

**ActivityServiceImpl.recordLogin(Long userId)**
```
1. 判断是否新的一天登录
2. 更新login_days连续登录天数
3. 计算活跃分数：loginDays × 10 + 基础分
4. 同步到Redis Sorted Set
```

**ActivityServiceImpl.recordInteraction(Long userId, int actionType)**
```
1. 检查今日互动次数限制（100次/天）
2. 更新todayInteractionCount
3. 计算活跃分数增量：
   - 点赞：+5
   - 收藏：+8
   - 评论：+10
   - 关注：+5
4. 同步到Redis：ZINCRBY实时增量更新
```

**粉丝分类逻辑**
- 铁杆粉丝：活跃度≥120
- 活跃粉丝：80≤活跃度<120
- 普通粉丝：活跃度<80

---

### 2.10 敏感词检测模块

#### 2.10.1 核心方法实现

**SensitiveWordDetector（DFA状态机算法）**
```
1. 初始化敏感词库到Redis（Hash结构）
2. 构建DFA状态机（多叉树）
3. 检测文本：状态机匹配敏感词
4. 返回分级结果：违规级别（低/中/高）
```

**SensitiveWordAspect（AOP切面）**
```
- 切入点：NoteController.createNote()、UserController.updateCurrentUser()等
- 执行时机：方法执行前
- 处理逻辑：敏感词检测，违规则抛出BusinessException
```

---

### 2.11 举报模块

#### 2.11.1 功能清单

| 功能 | 接口路径 | HTTP方法 | 说明 |
|------|----------|----------|------|
| 提交举报 | /report | POST | 举报笔记/评论/用户 |

#### 2.11.2 举报原因

| 原因码 | 说明 |
|--------|------|
| 1 | 垃圾广告 |
| 2 | 涉黄内容 |
| 3 | 抄袭搬运 |
| 4 | 其他 |

---

### 2.12 黑名单模块

#### 2.12.1 功能清单

| 功能 | 接口路径 | HTTP方法 | 说明 |
|------|----------|----------|------|
| 拉黑用户 | /blacklist/{userId} | POST | 加入黑名单 |
| 取消拉黑 | /blacklist/{userId} | DELETE | 移出黑名单 |
| 检查拉黑 | /blacklist/check/{userId} | GET | 检查是否在黑名单 |

---

### 2.13 限流模块

#### 2.13.1 限流策略

**固定窗口限流（FixedWindowRateLimiter）**
- 应用场景：登录限流（1分钟5次）
- 算法：固定时间窗口内计数

**滑动窗口限流（SlidingWindowRateLimiter）**
- 应用场景：注册限流（1小时5次）
- 算法：滑动时间窗口内计数

**Redis实现**
- Key：`rate_limit:{endpoint}:{userId}` 或 `rate_limit:{endpoint}:{ip}`
- Value：时间戳集合（ZSet）
-  TTL：时间窗口大小

---

## 三、Redis缓存设计

### 3.1 Key分类汇总

| Key模式 | 类型 | 说明 |
|---------|------|------|
| `note:hot` | ZSet | 热门笔记排名 |
| `note:hot:block` | Set | 热门笔记热度刷新锁 |
| `post:{noteId}:comment_tree` | String | 评论树JSON |
| `post:{noteId}:comment_count` | String | 评论数 |
| `feed:{userId}` | ZSet | 用户Feed收件箱 |
| `user:activity:{userId}` | Sorted Set | 用户活跃度排名 |
| `followers:{userId}` | Set | 用户粉丝集合 |
| `following:{userId}` | Set | 用户关注集合 |
| `sensitive:words` | Hash | 敏感词库 |
| `rate_limit:*` | ZSet | 限流计数器 |
| `comment:strategy:cache:{noteId}:0` | String | 评论策略缓存 |

---

## 四、数据库表结构

### 4.1 核心表汇总

| 表名 | 实体类 | ���明 |
|------|--------|------|
| user | User | 用户表 |
| note | Note | 笔记表 |
| note_comment | NoteComment | 评论表 |
| note_like | NoteLike | 笔记点赞表 |
| note_favorite | NoteFavorite | 笔记收藏表 |
| comment_like | CommentLike | 评论点赞表 |
| follow | Follow | 关注关系表 |
| blacklist | Blacklist | 黑名单表 |
| notification | Notification | 通知表 |
| user_activity | UserActivity | 用户活跃度表 |
| report | Report | 举报表 |
| note_forward | Forward | 转发记录表 |
| feed_push_log | FeedPushLog | Feed推送日志表 |
| operation_log | OperationLog | 操作日志表 |
| user_session | UserSession | 用户会话表 |

### 4.2 核心索引（建议添加）

```sql
-- 笔记表索引
ALTER TABLE note ADD INDEX idx_user_id (user_id);
ALTER TABLE note ADD INDEX idx_status (status);
ALTER TABLE note ADD INDEX idx_created_at (created_at);
ALTER TABLE note ADD INDEX idx_hot_score (hot_score);

-- 评论表索引
ALTER TABLE note_comment ADD INDEX idx_note_id (note_id);
ALTER TABLE note_comment ADD INDEX idx_user_id (user_id);
ALTER TABLE note_comment ADD INDEX idx_parent_id (parent_id);
ALTER TABLE note_comment ADD INDEX idx_root_id (root_id);
ALTER TABLE note_comment ADD INDEX idx_status (status);

-- 关注表索引（复合唯一索引）
ALTER TABLE follow ADD UNIQUE INDEX uk_follow (follower_id, following_id);

-- 点赞/收藏表索引
ALTER TABLE note_like ADD UNIQUE INDEX uk_note_user (note_id, user_id);
ALTER TABLE note_favorite ADD UNIQUE INDEX uk_note_user (note_id, user_id);
ALTER TABLE comment_like ADD UNIQUE INDEX uk_comment_user (comment_id, user_id);

-- 通知表索引
ALTER TABLE notification ADD INDEX idx_user_id (user_id);
ALTER TABLE notification ADD INDEX idx_created_at (created_at);
```

---

## 五、项目现存问题与优化方向

### 5.1 代码层面

| 问题 | 优化方案 |
|------|----------|
| 部分Controller方法注释不完整 | 补充Javadoc注释 |
| 有些VO对象未序列化 | 检查@TODO标记 |
| 异常信息未国际化 | 考虑中文提示国际化 |

### 5.2 架构层面

| 问题 | 优化方案 |
|------|----------|
| Service层有些方法职责过重 | 拆分大型Service |
| 缺少统一的日志门面 | 引入Slf4j统一日志 |
| 定时任务散落各处 | 统一到Scheduler包 |

### 5.3 数据库层面

| 问题 | 优化方案 |
|------|----------|
| 缺少复合索引 | 按查询场景添加组合索引 |
| 无分表策略 | 考虑历史数据归档 |
| 无读写分离 | 引入主从分离 |

### 5.4 接口层面

| 问题 | 优化方案 |
|------|----------|
| 参数校验注解分散 | 统一使用JSR-303校验 |
| 缺少接口版本管理 | 加/api/v1/前缀 |
| 响应无脱敏处理 | 敏感字段统一脱敏 |

### 5.5 性能层面

| 问题 | 优化方案 |
|------|----------|
| Feed流无缓存过期策略 | 设置合理TTL |
| 搜索ES无数据同步日志 | 增加同步监控 |
| 敏感词库需定期更新 | 增加更新机制 |

---

## 六、新增功能规划

### 6.1 高优先级

| 功能 | 说明 | 涉及模块 |
|------|------|----------|
| 笔记置顶 | 作者可置顶自己的笔记 | NoteService |
| 评论审核 | 新评论需审核后显示 | CommentService |
| 话题功能 | #话题#标签支持 | NoteService |
| @提及功能 | 评论中@用户 | CommentService |
| 笔记举报 | 多次违规封禁 | NoteService |

### 6.2 中优先级

| 功能 | 说明 | 涉及模块 |
|------|------|----------|
| 笔记合集 | 多个笔记组成合集 | NoteService |
| 笔记可见性 | 公开/私密/好友可见 | NoteService |
| 私信功能 | 用户间私信 | NotificationService |
|笔记分享码 | 短链分享 | NoteService |

### 6.3 低优先级

| 功能 | 说明 | 涉及模块 |
|------|------|----------|
| 笔记模板 | 发布模板选择 | NoteService |
| ��区��能 | 基于位置的推荐 | FeedService |
| IP属地显示 | 评论区IP归属地 | CommentService |
| 夜间模式 | 前端主题切换 | Frontend |

---

## 七、部署上线流程

### 7.1 环境准备

| 项目 | 要求 |
|------|------|
| 服务器 | 2核4G以上Linux服务器 |
| 域名 | 已备案域名 |
| MySQL | 8.0+，独库独表 |
| Redis | 4G+内存 |
| Elasticsearch | 7.x单节点或集群 |
| OSS | 阿里云对象存储 |

### 7.2 配置修改

**application.yml生产环境配置**
```yaml
spring:
  profile: prod
  datasource:
    url: jdbc:mysql://prod-host:3306/quxianshe?useSSL=true
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  redis:
    host: ${REDIS_HOST}
    password: ${REDIS_PASSWORD}
  elasticsearch:
    uris: http://${ES_HOST}:9200

jwt:
  secret: ${JWT_SECRET}
  expiration: 7200

server:
  port: 8080
```

### 7.3 安全加固

| 项目 | 说明 |
|------|------|
| 接口鉴权 | JWT + Filter |
| SQL注入 | MyBatis #{} |
| XSS防护 | 过滤器转义 |
| 敏感信息 | 配置外部化+加密 |
| 限流配置 | 接口级限流 |
| CORS配置 | 限制域名 |

### 7.4 服务部署

**Dockerfile（后端）**
```dockerfile
FROM openjdk:17-slim
WORKDIR /app
COPY target/quxianshe-backend-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**nginx.conf（前端）**
```nginx
server {
    listen 80;
    server_name your-domain.com;
    root /usr/share/nginx/html;
    index index.html;
    
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    location /api {
        proxy_pass http://backend:8080;
    }
}
```

### 7.5 监控配置

| 项目 | 工具 |
|------|------|
| 服务监控 | Spring Boot Actuator + Prometheus |
| 日志排查 | ELK（Elasticsearch + Logstash + Kibana） |
| 告警配置 | 钉钉/企业微信群机器人 |
| 链路追踪 | SkyWalking |

### 7.6 上线检查清单

- [ ] 数据库迁移脚本执行
- [ ] Redis连接配置验证
- [ ] ES索引创建和数据同步
- [ ] OSS配置验证
- [ ] 敏感词库初始化
- [ ] JWT密钥配置
- [ ] 限流策略配置
- [ ] 服务启动验证
- [ ] 接口自动化测试
- [ ] 压力测试
- [ ] 告警配置生效
- [ ] 数据备份策略配置

---

## 八、附录

### 8.1 接口Swagger文档地址
```
开发环境：http://localhost:8080/api/doc.html
生产环境：https://your-domain.com/api/doc.html
```

### 8.2 相关文档
- 后端项目：README.md
- 前端项目：frontend/README.md
- 数据库设计：database/init.sql
- 开发规范：OpenSpec/specs/

---

**文档版本：v1.0**
**最后更新：2026年4月**