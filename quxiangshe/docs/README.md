# 趣享社 (QuXiangShe) 社交平台技术文档

## 一、项目概述

趣享社是一款基于 Spring Boot + Vue3 构建的社交内容平台，支持用户发布动态、浏览Feed流、评论互动、点赞收藏等功能。系统面向高并发场景设计，采用推拉结合的Feed流、Redis滑动窗口限流等技术方案。

### 技术栈

| 层级 | 技术选型 |
|------|----------|
| 后端框架 | Spring Boot 3.2.x |
| 安全认证 | Spring Security 6.2.x + JWT |
| AI能力 | Spring AI 1.0.x (Ollama) |
| 数据库 | MySQL 8.0.x |
| 缓存 | Redis 7.0.x |
| 消息队列 | RabbitMQ 3.12.x |
| 前端 | Vue3 + Vite + Element Plus + Pinia |
| 构建工具 | Maven |

---

## 二、用户功能模块

### 2.1 认证模块

**功能清单：**
- 用户注册（用户名、密码、手机、邮箱、昵称）
- 用户登录（用户名+密码）
- JWT双令牌认证（AccessToken 15分钟 + RefreshToken 7天）
- 令牌刷新与登出

**实现思路：**
- 使用JJWT库生成JWT令牌，AccessToken包含用户基本信息，RefreshToken仅包含用户ID
- RefreshToken存储于Redis，7天后自动过期
- 使用Spring Security Filter Chain拦截请求，验证JWT并设置SecurityContext
- 登录/注册接口public，其他接口需要认证

### 2.2 用户模块

**功能清单：**
- 用户个人信息查看（头像、昵称、简介、粉丝数、关注数）
- 关注/取消关注用户
- 查看用户关注列表、粉丝列表

**实现思路：**
- 用户信息存储于MySQL，使用MyBatis Plus操作
- 关注关系存储于`t_user_follow`表，通过唯一索引防止重复关注
- 关注列表、粉丝列表使用MySQL查询，支持分页

### 2.3 帖子模块

**功能清单：**
- 发布动态（文字+图片/视频）
- 删除动态（仅作者可删除）
- 查看帖子详情
- 查看用户发布的帖子列表
- **点赞功能（新增）**
- **收藏功能（新增）**
- **按点赞数/收藏数排序获取帖子列表（新增）**

**实现思路：**

#### 点赞功能
- 使用Redis ZSet存储：`post:like:{postId}` 存储点赞用户ID，score为点赞时间戳
- 使用`post:like:count:{postId}` 存储点赞数（String类型）
- 使用`user:like:{userId}` 存储用户点赞的帖子ID
- 支持查询帖子点赞数、用户点赞列表、防止重复点赞

#### 收藏功能
- MySQL表`t_post_favorite`存储收藏记录
- Redis缓存：`post:favorite:count:{postId}` 收藏数、`user:favorite:{userId}` 用户收藏的帖子
- 先检查Redis缓存，缓存未命中时查询MySQL并回填缓存

#### 帖子排序
- 按点赞数排序：查询MySQL `post`表，按`like_count`降序
- 按收藏数排序：通过LEFT JOIN `t_post_favorite`表，按收藏数降序

### 2.4 Feed流模块

**功能清单：**
- 获取关注用户的动态流（推拉结合模式）
- 游标分页无限滚动

**实现思路：**
- **推模式（Push）**：发布动态时，异步推送到所有粉丝的Redis收件箱
- **拉模式（Pull）**：Redis收件箱为空时，从MySQL feed表拉取
- 游标分页：基于`lastPostTime` + `lastPostId`实现，避免数据重复/遗漏

### 2.5 评论模块

**功能清单：**
- 发布评论（顶级评论 + 回复）
- 查看评论列表（两层扁平展示：顶级评论 + 直接子评论）
- 点赞评论
- 删除评论

**实现思路：**
- 评论存储于MySQL `comment`表，支持无限层级（parent_id自关联）
- 时间分桶：`time_bucket = UNIX_TIMESTAMP/3600`，用于高效查询
- 异步写入：评论先写入Redis队列，5秒批量刷新到MySQL
- 两层查询：先查顶级评论，再批量查询每个顶级评论的直接子评论

### 2.6 AI摘要模块

**功能清单：**
- 自动生成帖子AI摘要
- 手动触发AI摘要生成

**实现思路：**
- 发布帖子时，发送消息到RabbitMQ队列
- AI消费者监听队列，调用Spring AI Ollama生成摘要
- 摘要生成失败时降级展示：帖子内容前100字

### 2.7 搜索模块

**功能清单：**
- 关键词搜索帖子

**实现思路：**
- 支持ES搜索（需要ES服务）
- ES不可用时降级到MySQL LIKE查询

### 2.8 限流模块

**功能清单：**
- 接口请求限流

**实现思路：**
- Redis ZSet实现滑动窗口限流
- 配置：窗口60秒，最大1000次请求
- 去重机制：5秒内相同URI不能重复请求

---

## 三、API接口清单

### 认证接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/v1/auth/register` | POST | 用户注册 |
| `/api/v1/auth/login` | POST | 用户登录 |
| `/api/v1/auth/refresh` | POST | 刷新令牌 |
| `/api/v1/auth/logout` | POST | 登出 |

### 用户接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/v1/user/profile/{userId}` | GET | 获取用户信息 |
| `/api/v1/user/follow/{userId}` | POST | 关注用户 |
| `/api/v1/user/follow/{userId}` | DELETE | 取消关注 |
| `/api/v1/user/followers/{userId}` | GET | 获取粉丝列表 |
| `/api/v1/user/following/{userId}` | GET | 获取关注列表 |

### 帖子接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/v1/posts` | POST | 发布动态 |
| `/api/v1/posts/{postId}` | DELETE | 删除动态 |
| `/api/v1/posts/{postId}` | GET | 获取帖子详情 |
| `/api/v1/users/{userId}/posts` | GET | 获取用户帖子列表 |
| `/api/v1/users/{userId}/favorites` | GET | 获取用户收藏列表 |
| `/api/v1/posts/{postId}/like` | POST | 点赞 |
| `/api/v1/posts/{postId}/like` | DELETE | 取消点赞 |
| `/api/v1/posts/{postId}/like/count` | GET | 获取点赞数 |
| `/api/v1/posts/{postId}/like/users` | GET | 获取点赞用户列表 |
| `/api/v1/posts/{postId}/favorite` | POST | 收藏 |
| `/api/v1/posts/{postId}/favorite` | DELETE | 取消收藏 |
| `/api/v1/posts/{postId}/favorite/count` | GET | 获取收藏数 |
| `/api/v1/posts/popular` | GET | 按点赞数排序帖子 |
| `/api/v1/posts/hot` | GET | 按收藏数排序帖子 |

### 评论接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/v1/posts/{postId}/comments` | POST | 发布评论 |
| `/api/v1/posts/{postId}/comments` | GET | 获取评论列表 |
| `/api/v1/comments/{commentId}/like` | POST | 点赞评论 |
| `/api/v1/comments/{commentId}/like` | DELETE | 取消点赞评论 |
| `/api/v1/comments/{commentId}` | DELETE | 删除评论 |

### Feed流接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/v1/feed` | GET | 获取动态流 |

### 搜索接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/v1/search/posts` | GET | 搜索帖子 |

---

## 四、数据库表结构

### 核心表

| 表名 | 说明 |
|------|------|
| `user` | 用户表 |
| `post` | 帖子表 |
| `comment` | 评论表 |
| `t_post_favorite` | 收藏表（新增） |
| `user_follow` | 关注关系表 |
| `feed` | Feed流表 |
| `ai_summary` | AI摘要表 |

### 关键字段说明

**post表：**
- `like_count` - 点赞数（MySQL存储）
- `comment_count` - 评论数
- `share_count` - 转发数
- `ai_summary` - AI摘要内容

**t_post_favorite表（新增）：**
- `user_id` - 用户ID
- `post_id` - 帖子ID
- `create_time` - 收藏时间

**comment表：**
- `parent_id` - 父评论ID（0表示顶级评论）
- `root_id` - 根评论ID
- `level` - 层级（1=顶级, 2=二级）
- `time_bucket` - 时间分桶

---

## 五、用户期望功能（待实现）

### 5.1 消息通知

**功能描述：**
- 点赞通知、收藏通知、评论通知、关注通知
- 消息聚合展示（点赞/收藏等合并显示）

**实现思路：**
- 通知存储于MySQL + Redis缓存
- 异步写入（发布行为触发消息推送）
- 支持未读数统计、消息已读

### 5.2 聊天功能

**功能描述：**
- 用户间私信
- 消息会话列表
- 实时消息推送（WebSocket）

**实现思路：**
- 消息存储于MySQL + Redis
- WebSocket实现实时推送
- 消息已读回执

### 5.3 个人主页定制

**功能描述：**
- 背景图设置
- 喜欢的主题分类
- 个人标签

**实现思路：**
- 用户设置表存储配置
- 主题色系可选

### 5.4 内容举报

**功能描述：**
- 举报违规帖子/评论
- 举报类型选择（色情、暴力、广告等）
- 举报处理状态查询

**实现思路：**
- 举报记录存储于MySQL
- 管理员后台处理（待实现管理后台）

### 5.5 数据统计

**功能描述：**
- 帖子浏览量统计
- 用户行为分析（点赞/收藏/评论偏好）
- 数据大盘（DAU/MAU）

**实现思路：**
- 浏览量存储于Redis，定时写入MySQL
- 用户画像标签化

---

## 六、性能与安全

### 6.1 高并发优化

- Feed流推拉结合，减少数据库压力
- 评论异步写入，削峰填谷
- Redis缓存热点数据（用户信息、点赞数、收藏数）
- 滑动窗口限流防止恶意请求

### 6.2 安全策略

- JWT双令牌，AccessToken短期有效，RefreshToken长期有效
- 接口幂等性设计（去重机制）
- SQL注入防护（MyBatis参数绑定）
- XSS防护（前端转义）

---

## 七、项目结构

```
quxiangshe/                      # 后端项目
├── src/main/java/com/quxiangshe/
│   ├── auth/                    # 认证模块
│   ├── user/                    # 用户模块
│   ├── post/                    # 帖子模块
│   ├── feed/                    # Feed流模块
│   ├── comment/                 # 评论模块
│   ├── ai/                      # AI摘要模块
│   ├── search/                  # 搜索模块
│   ├── ratelimit/               # 限流模块
│   └── common/                  # 公共模块
├── src/main/resources/
│   ├── db/schema.sql            # 数据库脚本
│   └── application.yml          # 配置文件

quxiangshe-web/                  # 前端项目
├── src/
│   ├── views/                   # 页面组件
│   │   ├── auth/                # 登录/注册
│   │   ├── feed/                # 首页Feed
│   │   ├── comment/             # 评论页
│   │   ├── search/              # 搜索页
│   │   ├── user/                # 个人主页
│   │   └── ai/                  # AI摘要
│   ├── store/                   # Pinia状态管理
│   ├── router/                  # 路由配置
│   └── utils/                   # 工具函数
```

---

## 八、快速开始

### 8.1 环境准备

1. MySQL 8.0+ 创建数据库 `quxiangshe`
2. Redis 7.0+ 启动服务
3. RabbitMQ 3.12+ 启动服务（可选）
4. Ollama 启动AI服务（可选）

### 8.2 初始化数据库

```bash
mysql -u root -p quxiangshe < src/main/resources/db/schema.sql
# 收藏表（如需新建）
mysql -u root -p quxiangshe < sql/init_favorite_like.sql
```

### 8.3 启动后端

```bash
cd quxiangshe
mvn spring-boot:run
# 默认端口 8081
```

### 8.4 启动前端

```bash
cd quxiangshe-web
npm install
npm run dev
# 默认端口 3000
```

### 8.5 访问

- 前端：http://localhost:3000
- 后端API：http://localhost:8081
- Swagger文档：http://localhost:8081/swagger-ui.html

---

*文档更新时间：2026-04-02*