# 理享 (YouLin) 部署指南

## 环境要求

| 组件 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 17+ | 后端运行环境 |
| Node.js | 18+ | 前端构建 |
| Maven | 3.8+ | 后端构建 |
| Docker | 20.10+ | 容器化部署（可选） |
| MySQL | 8.0 | 数据库 |
| Redis | 7+ | 缓存 |
| Elasticsearch | 7.17 | 搜索 |

---

## 快速部署 (Docker)

### 1. 配置环境变量

```bash
cp backend/.env.example backend/.env
# 编辑 backend/.env，填入真实配置值
```

必需填写的配置：
- `MYSQL_PASSWORD` — 数据库密码
- `JWT_SECRET` — JWT 签名密钥（至少32位随机字符串）
- `MAIL_USERNAME` / `MAIL_PASSWORD` — QQ邮箱和授权码

### 2. 初始化数据库

将 `scripts/database/init.sql` 在 MySQL 中执行，创建表结构。

### 3. 一键启动

```bash
docker compose up -d
```

### 4. 访问

- 前端页面：http://localhost
- API 文档：http://localhost:8080/api/doc.html

---

## 手动部署

### 1. 构建

```bash
# Windows
build.bat

# Linux/Mac
chmod +x build.sh
./build.sh
```

### 2. 配置环境变量

```bash
# 导出所有必需的环境变量
export MYSQL_PASSWORD=your_password
export JWT_SECRET=your_jwt_secret
export MAIL_USERNAME=your_email@qq.com
export MAIL_PASSWORD=your_auth_code
# ... 其他变量见 backend/.env.example
```

### 3. 启动

```bash
java -jar backend/target/lixiang-backend-1.0.0.jar
```

---

## 环境变量完整列表

| 变量名 | 必填 | 默认值 | 说明 |
|--------|------|--------|------|
| `SERVER_PORT` | 否 | `8080` | 服务端口 |
| `MYSQL_URL` | 否 | `jdbc:mysql://localhost:3306/quxiangshe...` | 数据库连接 |
| `MYSQL_USERNAME` | 否 | `root` | 数据库用户名 |
| `MYSQL_PASSWORD` | **是** | `123456` | 数据库密码 |
| `REDIS_HOST` | 否 | `localhost` | Redis 地址 |
| `REDIS_PASSWORD` | 否 | 空 | Redis 密码 |
| `ES_URIS` | 否 | `http://localhost:9200` | Elasticsearch 地址 |
| `JWT_SECRET` | **是** | 无 | JWT 签名密钥 |
| `MAIL_HOST` | 否 | `smtp.qq.com` | 邮件服务器 |
| `MAIL_USERNAME` | **是** | 无 | 发件邮箱地址 |
| `MAIL_PASSWORD` | **是** | 无 | 邮箱授权码 |
| `OSS_ACCESS_KEY_ID` | 否 | 无 | 阿里云 OSS AK |
| `OSS_ACCESS_KEY_SECRET` | 否 | 无 | 阿里云 OSS SK |
| `OSS_BUCKET_NAME` | 否 | 无 | OSS Bucket 名称 |
| `OSS_ENABLED` | 否 | `true` | 是否启用 OSS |
| `DOUBAO_API_KEY` | 否 | 无 | 豆包 AI API Key |
| `DOUBAO_ENDPOINT` | 否 | 无 | 豆包 AI Endpoint |
| `REVIEW_ENABLED` | 否 | `true` | 是否启用内容审核 |
| `FFMPEG_PATH` | 否 | `ffmpeg` | FFmpeg 可执行文件路径 |
| `LOG_LEVEL` | 否 | `INFO` | 日志级别 |

---

## 目录结构

```
quxiangshe-project/
├── backend/                 # Spring Boot 后端
│   ├── src/main/resources/
│   │   ├── application.yml       # 配置文件（环境变量）
│   │   └── static/               # 打包后的前端静态文件
│   ├── Dockerfile                # 后端容器镜像
│   ├── .env.example              # 环境变量模板
│   └── pom.xml
├── frontend/                # Vue 3 前端
│   ├── src/
│   ├── dist/                     # 构建产物
│   ├── Dockerfile                # 前端容器镜像
│   ├── nginx.conf                # Nginx 配置
│   └── package.json
├── scripts/database/        # 数据库脚本
├── docs/                    # 文档
├── docker-compose.yml       # Docker 编排文件
├── build.bat                # Windows 构建脚本
├── build.sh                 # Linux/Mac 构建脚本
└── DEPLOY.md                # 本文件
```
