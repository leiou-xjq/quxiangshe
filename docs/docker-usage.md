# 趣享社 Docker部署指南

## 环境要求

- Docker: 20.10+
- Docker Compose: 2.0+
- 内存: 推荐16GB（最低8GB）

## 快速启动

### 1. 首次启动（构建镜像）

```powershell
# 进入项目根目录
cd D:\quxiansghe-project

# 启动所有服务（首次需要构建镜像，约10-15分钟）
docker compose up -d --build

# 查看启动状态
docker compose ps
```

### 2. 日常启动

```powershell
# 启动已构建的容器
docker compose up -d

# 查看日志
docker compose logs -f

# 停止服务
docker compose down
```

---

## 服务地址

| 服务 | 地址 | 说明 |
|------|------|------|
| **前端** | http://localhost | 趣享社首页 |
| **后端API** | http://localhost:8080/api | REST API |
| **MySQL** | localhost:3306 | 用户: root, 密码: 123456 |
| **Redis** | localhost:6379 | 无密码 |
| **Elasticsearch** | http://localhost:9200 | 搜索服务 |
| **RabbitMQ** | http://localhost:15672 | 用户: admin, 密码: admin123 |
| **Milvus** | http://localhost:9091 | 向量数据库 |

---

## 常用命令

### 构建和启动

```powershell
# 构建镜像（不启动）
docker compose build

# 启动并后台运行
docker compose up -d

# 启动并查看日志
docker compose up

# 强制重新构建
docker compose up -d --build --force-recreate
```

### 查看状态

```powershell
# 查看运行中的容器
docker compose ps

# 查看所有容器（含停止）
docker compose ps -a

# 查看容器资源使用
docker stats

# 查看服务健康状态
docker compose ps
```

### 日志

```powershell
# 查看所有服务日志
docker compose logs

# 查看指定服务日志
docker compose logs -f backend
docker compose logs -f frontend

# 查看最近100行日志
docker compose logs --tail=100 backend
```

### 停止和清理

```powershell
# 停止所有服务
docker compose stop

# 停止并删除容器
docker compose down

# 删除镜像
docker compose down --rmi all

# 删除所有数据卷
docker compose down -v

# 完全清理（删除镜像+容器+数据卷）
docker compose down --rmi all -v
```

### 重启单个服务

```powershell
# 重启后端
docker compose restart backend

# 重启前端
docker compose restart frontend
```

---

## 数据持久化

以下数据存储在Docker卷中，重启后不会丢失：

|  Volume | 用途 |
|---------|------|
| mysql-data | MySQL数据 |
| redis-data | Redis缓存 |
| es-data | Elasticsearch索引 |
| rabbitmq-data | RabbitMQ消息 |
| milvus-data | 向量数据 |
| etcd-data | Milvus元数据 |
| minio-data | Milvus对象存储 |

---

## 常见问题

### 1. 端口被占用

```powershell
# 检查端口占用
netstat -ano | findstr "3306 6379 9200 5672 19530 80 8080"

# 停止占用端口的进程
taskkill /PID <PID> /F
```

### 2. 内存不足

```powershell
# 查看Docker总内存使用
docker stats

# 停止不需要的容器
docker stop <container-name>
```

### 3. 服务启动失败

```powershell
# 查看具体错误
docker compose logs <service-name>

# 检查健康状态
docker inspect <container-name> | findstr "Health"
```

### 4. 重新初始化数据

```powershell
# 停止服务
docker compose down

# 删除数据卷
docker volume rm quxiangshe-mysql-data
docker volume rm quxiangshe-redis-data
docker volume rm quxiangshe-es-data
# ... 其他卷

# 重新启动
docker compose up -d
```

---

## 开发模式 vs 生产模式

### 开发模式（当前）

- 后端：IDEA本地运行
- 前端：IDEA/Vite热更新
- 基础设施：Docker

```powershell
# 只需启动基础设施
docker compose up -d mysql redis elasticsearch rabbitmq milvus-standalone
```

### 生产模式

- 所有服务都通过Docker运行

```powershell
# 一键启动
docker compose up -d --build
```

---

## 注意事项

1. **首次启动较慢**：需要下载Docker镜像和构建项目
2. **Milvus启动**：Milvus首次启动需要约2-3分钟
3. **数据初始化**：MySQL需要通过Flyway自动执行数据库迁移
4. **资源限制**：如果内存不足，可以减少服务数量（如不使用Milvus）

---

## 快速故障排查

```powershell
# 1. 检查所有容器状态
docker compose ps

# 2. 检查关键服务是否健康
curl http://localhost:9091/healthz  # Milvus
curl http://localhost:9200/_cluster/health  # ES

# 3. 查看最近错误
docker compose logs --tail=50 | findstr ERROR

# 4. 重置Docker网络
docker network prune
docker compose down
docker compose up -d
```

---

## 访问服务

启动成功后：

1. 打开浏览器访问 http://localhost
2. 使用系统功能
3. 后端API文档: http://localhost:8080/doc.html