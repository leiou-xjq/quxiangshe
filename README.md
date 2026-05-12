# 趣享社 - 校园社交平台

基于 Spring Boot + Vue 3 的校园社交平台，支持笔记分享、互动社交、Feed流推荐等功能。

## 技术栈

### 后端
- **框架**: Spring Boot 3.2 + Spring Security
- **ORM**: MyBatis-Plus 3.5
- **数据库**: MySQL 8.0
- **缓存**: Redis 7 + Redisson + Caffeine
- **消息队列**: RabbitMQ 3.12
- **实时通信**: WebSocket + Pusher
- **API文档**: Knife4j

### 前端
- **框架**: Vue 3 + Composition API
- **构建工具**: Vite
- **UI组件**: Element Plus
- **状态管理**: Pinia
- **路由**: Vue Router

### 中间件
- **对象存储**: 阿里云 OSS
- **短信服务**: 腾讯云 SMS
- **AI服务**: 豆包 (字节跳动)
- **数据同步**: Canal (MySQL binlog)

## 功能特性

### 用户系统
- ✅ 邮箱注册/登录
- ✅ JWT 认证
- ✅ 微信登录 (可选)
- ✅ 用户资料管理
- ✅ 密码修改/重置

### 社交功能
- ✅ 笔记发布 (支持图片/视频)
- ✅ 点赞/评论/收藏/转发
- ✅ 关注/粉丝系统
- ✅ 私信聊天
- ✅ 黑名单

### 内容生态
- ✅ Feed流推荐 (推拉结合)
- ✅ 热门内容计算
- ✅ 搜索功能
- ✅ AI内容审核 (异步)
- ✅ 视频转码处理

### 系统特性
- ✅ 异步通知 (MQ)
- ✅ WebSocket实时推送
- ✅ 接口限流
- ✅ 反垃圾检测
- ✅ 多级缓存

## 快速部署 (Docker Compose)

### 前置要求

- Docker 20.10+
- Docker Compose 2.0+

### 部署步骤

```bash
# 1. 克隆项目
git clone https://github.com/leiou-xjq/quxiangshe.git
cd quxiangshe

# 2. 复制环境配置
cp .env.example .env
# 编辑 .env 填入真实配置

# 3. 启动所有服务
docker-compose up -d

# 4. 查看服务状态
docker-compose ps

# 5. 查看日志
docker-compose logs -f backend
```

### 访问服务

| 服务 | 地址 |
|------|------|
| 前端Web | http://localhost |
| 后端API | http://localhost:8080/api |
| API文档 | http://localhost:8080/api/doc.html |
| RabbitMQ | http://localhost:15672 (admin/admin123) |

## 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| MYSQL_PASSWORD | MySQL密码 | quxiangshe123 |
| REDIS_PASSWORD | Redis密码 | quxiangshe123 |
| JWT_SECRET | JWT密钥 (至少32位) | - |
| MAIL_USERNAME | 邮箱地址 | - |
| MAIL_PASSWORD | 邮箱授权码 | - |

完整环境变量列表请参考 `.env.example`。

## 目录结构

```
quxiangshe/
├── backend/
│   ├── src/main/java/com/quxiangshe/backend/
│   │   ├── config/          # 配置类
│   │   ├── controller/      # 控制器
│   │   ├── service/        # 服务层
│   │   ├── entity/         # 实体类
│   │   ├── mapper/         # MyBatis映射
│   │   ├── consumer/      # MQ消费者
│   │   ├── scheduler/     # 定时任务
│   │   └── util/          # 工具类
│   └── src/main/resources/
│       └── application.yml
├── frontend/
│   ├── src/
│   │   ├── api/           # API请求
│   │   ├── views/        # 页面组件
│   │   ├── components/   # 通用组件
│   │   ├── stores/       # Pinia状态
│   │   └── router/       # 路由配置
│   └── package.json
├── docker-compose.yml
├── .env.example
└── README.md
```

## 本地开发

### 后端

```bash
cd backend
mvn clean package -DskipTests
java -jar target/lixiang-backend-1.0.0.jar
```

### 前端

```bash
cd frontend
npm install
npm run dev
```

## API文档

启动后端后访问: http://localhost:8080/api/doc.html

## 技术文档

详细设计文档请参考：

- [Feed流分发系统设计](docs/feed-design.md)
- [多级缓存架构设计](docs/cache-design.md)
- [消息队列设计](docs/mq-design.md)
- [热点笔记排序设计](docs/hot-score-design.md)

## 性能优化

1. **多级缓存**: Redis + Caffeine本地缓存
2. **异步处理**: RabbitMQ异步通知、转码、审核
3. **Feed流优化**: 推拉结合模式
4. **数据库**: 合理索引 + 读写分离
5. **分布式锁**: Redisson实现

## 许可证

MIT License
