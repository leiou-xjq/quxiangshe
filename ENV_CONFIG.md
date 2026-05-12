# 环境变量配置清单

本文档列出了项目部署时需要配置的所有环境变量。

---

## 一、必填配置（必需）

### 1.1 数据库配置

| 环境变量 | 说明 | 示例值 |
|----------|------|--------|
| `MYSQL_URL` | MySQL 连接地址 | `jdbc:mysql://mysql:3306/quxiangshe?useUnicode=true&characterEncoding=utf-8&useSSL=true&serverTimezone=Asia/Shanghai` |
| `MYSQL_USERNAME` | MySQL 用户名 | `quxiangshe` |
| `MYSQL_PASSWORD` | MySQL 密码（必须强密码） | `YourStrong@Password123` |

### 1.2 Redis 配置

| 环境变量 | 说明 | 示例值 |
|----------|------|--------|
| `REDIS_HOST` | Redis 主机地址 | `redis` |
| `REDIS_PORT` | Redis 端口 | `6379` |
| `REDIS_PASSWORD` | Redis 密码（必须强密码） | `YourRedis@Password456` |

### 1.3 JWT 配置

| 环境变量 | 说明 | 示例值 |
|----------|------|--------|
| `JWT_SECRET` | JWT 密钥（必须 256 位以上随机字符串） | `your-256-bit-random-secret-key-here-must-be-very-long` |

**生成随机 JWT 密钥**：
```bash
openssl rand -base64 64 | tr -d '\n'
```

---

## 二、重要配置（建议）

### 2.1 邮件服务配置

| 环境变量 | 说明 | 示例值 |
|----------|------|--------|
| `MAIL_USERNAME` | 发送邮箱地址 | `your-email@qq.com` |
| `MAIL_PASSWORD` | 邮箱授权码（不是登录密码） | `abcdefghijklmnop` |

### 2.2 内容审核（AI）配置

| 环境变量 | 说明 | 示例值 |
|----------|------|--------|
| `DOUBAO_API_KEY` | 豆包 API Key | `ark-xxxxxxxxxxxxx` |
| `REVIEW_ENABLED` | 是否启用内容审核 | `true` |
| `REVIEW_ASYNC_ENABLED` | 是否启用异步审核 | `true` |
| `VALUE_REVIEW_ENABLED` | 是否启用内容价值审核 | `true` |

### 2.3 阿里云 OSS 配置

| 环境变量 | 说明 | 示例值 |
|----------|------|--------|
| `OSS_ENABLED` | 是否启用 OSS | `true` 或 `false` |
| `OSS_ENDPOINT` | OSS 区域节点 | `oss-cn-beijing.aliyuncs.com` |
| `OSS_ACCESS_KEY_ID` | OSS AccessKey ID | `LTAIxxxxx` |
| `OSS_ACCESS_KEY_SECRET` | OSS AccessKey Secret | `xxxxxxxxxxxx` |
| `OSS_BUCKET_NAME` | OSS Bucket 名称 | `quxiangshe-prod` |

---

## 三、可选配置

### 3.1 服务器配置

| 环境变量 | 默认值 | 说明 |
|----------|--------|------|
| `SERVER_PORT` | `8080` | 服务端口 |
| `LOG_LEVEL` | `INFO` | 日志级别 |

### 3.2 安全配置

| 环境变量 | 默认值 | 说明 |
|----------|--------|------|
| `CORS_ALLOWED_ORIGINS` | 见下方 | 允许的跨域来源，多个用逗号分隔 |

```
CORS_ALLOWED_ORIGINS: https://yourdomain.com,https://www.yourdomain.com
```

### 3.3 文件上传配置

| 环境变量 | 默认值 | 说明 |
|----------|--------|------|
| `MULTIPART_MAX_SIZE` | `10MB` | 最大文件上传大小 |

---

## 四、K8s Secret 配置

建议使用 K8s Secret 管理敏感配置：

```yaml
# backend-secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: quxiangshe-backend-secret
  namespace: default
type: Opaque
stringData:
  # 数据库
  MYSQL_PASSWORD: YourStrong@Password123
  MYSQL_URL: jdbc:mysql://mysql:3306/quxiangshe?useUnicode=true&characterEncoding=utf-8&useSSL=true&serverTimezone=Asia/Shanghai
  
  # Redis
  REDIS_PASSWORD: YourRedis@Password456
  
  # JWT
  JWT_SECRET: your-256-bit-random-secret-key-here-must-be-very-long
  
  # 邮件
  MAIL_USERNAME: your-email@qq.com
  MAIL_PASSWORD: your-email-auth-code
  
  # AI 审核
  DOUBAO_API_KEY: ark-xxxxxxxxxxxxx
  
  # 阿里云 OSS
  OSS_ACCESS_KEY_ID: LTAIxxxxx
  OSS_ACCESS_KEY_SECRET: xxxxxxxxxxxx
```

---

## 五、K8s Deployment 配置

在 Deployment 中引用 Secret：

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: quxiangshe-backend
spec:
  template:
    spec:
      containers:
      - name: backend
        envFrom:
        - configMapRef:
            name: quxiangshe-backend-config
        - secretRef:
            name: quxiangshe-backend-secret
```

---

## 六、环境变量检查脚本

部署前验证环境变量：

```bash
#!/bin/bash

# 检查必填环境变量
REQUIRED_VARS=(
  "MYSQL_URL"
  "MYSQL_USERNAME"
  "MYSQL_PASSWORD"
  "REDIS_PASSWORD"
  "JWT_SECRET"
)

MISSING=()
for var in "${REQUIRED_VARS[@]}"; do
  if [ -z "${!var}" ]; then
    MISSING+=("$var")
  fi
done

if [ ${#MISSING[@]} -gt 0 ]; then
  echo "错误：以下必填环境变量未配置："
  for var in "${MISSING[@]}"; do
    echo "  - $var"
  done
  exit 1
fi

echo "✓ 所有必填环境变量已配置"
```

---

## 七、配置示例

### 7.1 Docker Compose 本地开发

```bash
# .env 文件
MYSQL_PASSWORD=dev_password
REDIS_PASSWORD=dev_redis_password
JWT_SECRET=dev_jwt_secret_key_very_long_string
MAIL_USERNAME=test@qq.com
MAIL_PASSWORD=test_auth_code
```

### 7.2 K8s 生产环境

使用 SealedSecret 或 Vault 管理敏感配置，参考上方 K8s 配置部分。

---

> **注意**：生产环境务必使用强密码，避免使用默认值或弱密码。