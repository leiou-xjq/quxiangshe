# 趣享社 - 社交平台

基于 Spring Boot + Vue 的校园社交平台，支持笔记发布、互动（点赞/评论/关注）、Feed流推荐等功能。

## 技术栈

- **后端**: Spring Boot 3.2 + MyBatis-Plus + Redis + RabbitMQ
- **前端**: Vue 3 + Vite + Element Plus
- **数据库**: MySQL 8.0
- **缓存**: Redis 7
- **消息队列**: RabbitMQ 3.12

## 功能特性

- ✅ 用户认证 (JWT)
- ✅ 笔记发布 (支持图片/视频)
- ✅ 互动功能 (点赞/评论/收藏/转发)
- ✅ 关注/粉丝系统
- ✅ Feed流推荐 (推拉结合)
- ✅ 异步通知 (MQ)
- ✅ 邮件验证码
- ✅ 视频异步转码
- ✅ 内容审核 (AI)

## 快速部署 (Docker Compose)

### 前置要求

- Docker 20.10+
- Docker Compose 2.0+

### 部署步骤

```bash
# 1. 克隆项目
git clone <项目地址>
cd quxiangshe-project

# 2. 复制环境配置
cp .env.example .env

# 3. 启动所有服务 (首次构建需要几分钟)
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
| Swagger文档 | http://localhost:8080/api/doc.html |
| RabbitMQ管理 | http://localhost:15672 (账号: admin/admin123) |

## 手动部署 (不使用Docker)

### 后端部署

```bash
# 1. 安装依赖
cd backend
mvn clean package -DskipTests

# 2. 配置环境变量
cp src/main/resources/application.yml.example src/main/resources/application.yml
# 编辑 application.yml 填入数据库、Redis等配置

# 3. 启动
java -jar target/lixiang-backend-1.0.0.jar
```

### 前端部署

```bash
# 1. 安装依赖
cd frontend
npm install

# 2. 构建
npm run build

# 3. 部署
# 将 dist 目录内容部署到 Nginx
```

## 环境变量说明

| 变量 | 说明 | 示例 |
|------|------|------|
| MYSQL_PASSWORD | MySQL root密码 | quxiangshe123 |
| REDIS_PASSWORD | Redis密码 | quxiangshe123 |
| RABBITMQ_PASSWORD | RabbitMQ密码 | admin123 |
| JWT_SECRET | JWT签名密钥 | 至少32位随机字符串 |
| MAIL_USERNAME | QQ邮箱 | 123456789@qq.com |
| MAIL_PASSWORD | QQ邮箱授权码 | 需要在邮箱设置中获取 |
| OSS_ENABLED | 是否启用阿里云OSS | false |
| DOUBAO_API_KEY | 豆包AI审核API密钥 | 可选 |

## 目录结构

```
quxiangshe-project/
├── backend/               # Spring Boot后端
│   ├── src/
│   │   └── main/
│   │       ├── java/     # Java源码
│   │       └── resources/
│   │           ├── application.yml
│   │           └── mapper/
│   ├── pom.xml
│   └── Dockerfile
├── frontend/             # Vue前端
│   ├── src/
│   ├── public/
│   ├── package.json
│   ├── vite.config.ts
│   └── nginx.conf
├── docker-compose.yml    # Docker编排配置
├── .env                  # 环境变量配置
└── README.md
```

## 常见问题

### 1. 启动后端失败，提示连接不上MySQL

- 检查 .env 中的 MYSQL_PASSWORD 是否正确
- 检查 MySQL 容器是否正常启动: `docker-compose ps`
- 查看日志: `docker-compose logs mysql`

### 2. 启动后端失败，提示连接不上Redis

- 检查 .env 中的 REDIS_PASSWORD 是否正确
- 检查 Redis 容器是否正常启动

### 3. 前端无法访问后端API

- 检查后端是否正常启动: `docker-compose logs backend`
- 检查 Nginx 配置是否正确

### 4. 邮件发送失败

- 检查 MAIL_USERNAME 和 MAIL_PASSWORD 是否正确
- QQ邮箱需要使用授权码而非登录密码

## API文档

启动后访问: http://localhost:8080/api/doc.html

## 性能优化建议

1. **MQ异步处理**: 点赞/评论/关注通知、邮件发送、视频转码已通过MQ异步化
2. **多级缓存**: Redis + Caffeine本地缓存
3. **数据库索引**: 合理建立索引
4. **Feed流优化**: 推拉结合模式

## 许可证

MIT License