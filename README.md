# 理享 - 校园社交平台

基于 Spring Boot + Vue 3 的校园社交平台，支持笔记分享、互动社交、Feed流推荐、AI内容审核等功能。

## 核心技术栈

### 后端
| 技术 | 说明 |
|------|------|
| Spring Boot 3.2 | 核心框架 |
| MyBatis-Plus 3.5 | ORM框架 |
| MySQL 8.0 | 主数据库 |
| Redis 7 + Redisson | 分布式缓存/锁 |
| Caffeine | 本地缓存 |
| RabbitMQ 3.12 | 消息队列 |
| Milvus 2.3 | 向量数据库 |
| WebSocket | 实时通信 |

### AI服务
| 技术 | 说明 |
|------|------|
| 豆包大模型 | 内容审核、价值观判断 |
| Milvus | RAG向量检索 |
| Embedding | 文本向量化 |

### 前端
Vue 3 + Vite + Element Plus + Pinia

## 核心功能特性

### 内容发布与社交
- 笔记发布（图片/视频）
- 点赞、评论、收藏、转发
- 关注/粉丝系统
- 私信聊天
- 黑名单管理

### 智能Feed流分发
| 博主类型 | 策略 | 说明 |
|----------|------|------|
| 小博主 (<1K粉) | PUSH直推 | 直接写入粉丝收件箱 |
| 中博主 (1K-10W粉) | PULL拉取 | 写入发件箱，粉丝读取时合并 |
| 大博主 (>10W粉) | HYBRID混合 | 活跃粉丝PUSH，普通粉丝PULL |

### AI内容审核（三层递进）
```
┌─────────────────────────────────────────────────────────┐
│                    L1: 敏感词检测                        │
│                    关键词/正则匹配，即时返回               │
└─────────────────────────┬─────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                 L2: RAG向量案例匹配                       │
│    Milvus向量库检索Top5相似违规案例，辅助LLM判断          │
└─────────────────────────┬─────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                 L3: LLM价值观审核                        │
│     豆包大模型，结合24字核心价值观+论语五常综合判定       │
└─────────────────────────────────────────────────────────┘
```

### 用户信誉分体系
| 操作 | 分数变化 |
|------|---------|
| 审核通过 | +2分 |
| 审核违规 | -5分 |
| 举报核实 | -10分 |

```
信誉分 >= 80 → 同步审核（快速通道）
信誉分 < 80 → 异步审核（普通模式）
```

**缓存策略**：Redis缓存(TTL=1h) → DB为真相源 → 定时校正

### 热点内容计算
```
hotScore = like×1 + comment×2 + favorite×3 + forward×5
每日0.9衰减，避免老笔记霸榜
```

## 高并发设计

| 优化手段 | 说明 |
|---------|------|
| 多级缓存 | Caffeine(L1) + Redis(L2) |
| 计数分离 | Redis原子计数 + 异步DB同步 |
| 线程池隔离 | 推送/审核/分发三池分离 |
| 分布式锁 | Redisson + Lua脚本原子操作 |
| MQ异步解耦 | 审核、转码、通知异步处理 |

## 快速部署

### 前置要求
- Docker 20.10+
- Docker Compose 2.0+

### 部署步骤
```bash
# 1. 克隆项目
git clone https://github.com/leiou-xjq/quxiangshe.git
cd quxiangshe

# 2. 复制并配置环境变量
cp .env.example .env
# 编辑 .env 填入真实配置

# 3. 启动所有服务
docker-compose up -d

# 4. 查看服务状态
docker-compose ps

# 5. 查看后端日志
docker-compose logs -f backend
```

### 服务访问地址
| 服务 | 地址 |
|------|------|
| 前端Web | http://localhost |
| 后端API | http://localhost:8080/api |
| API文档 | http://localhost:8080/api/doc.html |
| RabbitMQ | http://localhost:15672 (admin/admin123) |
| Milvus | http://localhost:9091 |

## 环境变量说明

```bash
# 数据库
MYSQL_PASSWORD=your-mysql-password

# Redis
REDIS_PASSWORD=your-redis-password

# JWT
JWT_SECRET=your-jwt-secret-at-least-32-chars

# 豆包大模型 (内容审核)
DOUBAO_API_KEY=your-api-key
DOUBAO_BASE_URL=https://ark.cn-beijing.volces.com/api/v3

# 信誉分阈值 (默认80分走同步审核)
REVIEW_SYNC_THRESHOLD=80

# RAG功能开关
RAG_ENABLED=true
```

## 项目结构

```
quxiangshe/
├── backend/
│   ├── src/main/java/com/quxiangshe/backend/
│   │   ├── config/         # 配置类 (Redis/RabbitMQ/Milvus)
│   │   ├── controller/      # REST控制器
│   │   ├── service/        # 服务层
│   │   │   └── impl/      # 服务实现
│   │   ├── entity/         # 实体类
│   │   ├── mapper/        # MyBatis Mapper
│   │   ├── consumer/      # MQ消费者
│   │   ├── scheduler/     # 定时任务
│   │   ├── task/         # 异步任务
│   │   └── util/         # 工具类
│   └── src/main/resources/
│       ├── application.yml
│       └── db/migration/  # Flyway数据库迁移
├── frontend/               # Vue 3前端
├── docker-compose.yml      # 容器编排
├── .env.example          # 环境变量模板
└── README.md
```

## 数据库迁移

项目使用Flyway管理数据库版本，迁移脚本位于：
```
backend/src/main/resources/db/migration/
```

**重要迁移**：
- V9: 创建违规案例库表 (violation_case_library)
- V10: 添加用户信誉分字段 (reputation_score)

## 本地开发

```bash
# 后端
cd backend
mvn clean compile
mvn spring-boot:run

# 前端
cd frontend
npm install
npm run dev
```

## API文档

启动后端后访问：http://localhost:8080/api/doc.html

## 许可证

MIT License
