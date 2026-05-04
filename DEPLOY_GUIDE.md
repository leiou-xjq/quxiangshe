# 部署指南

本文档详细说明如何将项目部署到Cloudflare Pages和Railway。

## 目录
- [一、部署架构](#一部署架构)
- [二、前端部署到Cloudflare Pages](#二前端部署到cloudflare-pages)
- [三、后端部署到Railway](#三后端部署到railway)
- [四、配置环境变量](#四配置环境变量)
- [五、验证部署](#五验证部署)
- [六、常见问题](#六常见问题)

---

## 一、部署架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Cloudflare Pages                          │
│                      (前端静态网站 + PWA)                        │
│                    https://xxx.pages.dev                        │
└─────────────────────────────┬───────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         Railway                                    │
│                    (后端 Spring Boot)                             │
│                    https://xxx.railway.app                        │
└─────────────────────────────┬───────────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
        ┌─────────┐    ┌─────────┐    ┌─────────┐
        │  MySQL  │    │  Redis  │    │ RabbitMQ│
        └─────────┘    └─────────┘    └─────────┘
```

---

## 二、前端部署到Cloudflare Pages

### 步骤1：登录Cloudflare Pages

1. 打开浏览器，访问：https://pages.cloudflare.com
2. 点击"Sign in"登录你的Cloudflare账号
3. 如果没有账号，点击"Sign up"注册

### 步骤2：创建新项目

1. 登录后，点击"Create a project"
2. 选择"Connect to Git"

### 步骤3：连接GitHub仓库

1. 点击"Connect GitHub"
2. 授权Cloudflare访问你的GitHub账号
3. 在弹窗中选择仓库：`leiou-xjq/quxiangshe`
4. 点击"Install & Authorize"

### 步骤4：配置部署

1. 选择项目后，填写配置信息：
   - **Project name**: `quxiangshe-frontend`（或你喜欢的名称）
   - **Production branch**: `master`

2. 在"Build settings"中填写：
   - **Build command**: `npm run build`
   - **Build output directory**: `frontend/dist`

3. 点击"Advanced"展开更多设置：
   - **Node version**: `18`

### 步骤5：开始部署

1. 点击"Save and Deploy"
2. 等待构建完成（约1-2分钟）
3. 部署成功后，会显示一个URL，例如：`https://quxiangshe-frontend.pages.dev`

### 步骤6：记录前端URL

部署完成后，**复制并保存你的前端URL**，后面配置后端时会用到。

---

## 三、后端部署到Railway

### 步骤1：登录Railway

1. 打开浏览器，访问：https://railway.app
2. 点击"Login"登录
3. 选择"Continue with GitHub"，授权GitHub账号

### 步骤2：创建新项目

1. 登录后，点击"New Project"
2. 在弹窗中选择"Deploy from GitHub repo"

### 步骤3：选择GitHub仓库

1. 在列表中找到并选择 `quxiangshe` 仓库
2. Railway会自动检测为Maven (Java)项目

### 步骤4：等待部署

1. 点击"Deploy Now"
2. 等待构建完成（约3-5分钟）
3. 部署成功后，会显示项目面板

### 步骤5：配置环境变量

在项目面板中，点击"Variables"标签，添加以下环境变量：

| 变量名 | 值 | 说明 |
|--------|-----|------|
| `SERVER_PORT` | `8080` | 服务端口 |
| `MYSQL_URL` | （Railway会自动创建）| MySQL连接地址 |
| `MYSQL_USERNAME` | `root` | MySQL用户名 |
| `MYSQL_PASSWORD` | （设置一个强密码）| MySQL密码 |
| `REDIS_PASSWORD` | （设置一个强密码）| Redis密码 |
| `JWT_SECRET` | （设置一个随机字符串，至少32位）| JWT密钥 |
| `DOUBAO_API_KEY` | `ark-8f29c9b7-1a77-493e-8b24-1e6c5ddf6655-4a2e0` | 豆包AI审核API Key |
| `DOUBAO_ENDPOINT` | `ep-20260428080756-pbltx` | 豆包Endpoint |
| `REVIEW_ENABLED` | `true` | 启用内容审核 |
| `VALUE_REVIEW_ENABLED` | `true` | 启用价值观审核 |
| `RABBITMQ_ENABLED` | `false` | 先关闭RabbitMQ |

**设置完成后，点击"Deploy"重新部署**

### 步骤6：记录后端URL

1. 部署完成后，点击"Domains"
2. 复制你的后端URL，例如：`https://quxiangshe-production-xxx.railway.app`

---

## 四、配置前后端对接

### 步骤1：获取后端URL

在Railway的"Domains"页面，复制你的后端URL，例如：
```
https://quxiangshe-production-abc123.railway.app
```

### 步骤2：更新前端API配置

你需要修改前端配置，让它指向你的后端URL。

由于我们在部署时不能直接修改代码，需要在Cloudflare Pages的环境变量中配置：

1. 在Cloudflare Pages项目页面，点击"Settings" → "Environment Variables"
2. 添加环境变量：
   - **Variable name**: `VITE_API_BASE_URL`
   - **Value**: 你的后端URL + `/api`
   
   例如：`https://quxiangshe-production-abc123.railway.app/api`

3. 点击"Save"
4. 重新部署项目

### 或者：使用Cloudflare Functions（可选）

如果上面的方法不生效，可以创建一个`_redirects`文件来转发API请求：

1. 在`frontend/public/`目录创建`_redirects`文件
2. 内容填写：
   ```
   /api/*  https://your-railway-url/api/:splat  200
   ```
3. 重新构建并部署

---

## 五、验证部署

### 测试前端访问

1. 打开浏览器，访问你的Cloudflare Pages URL
2. 检查页面是否正常加载

### 测试注册/登录

1. 点击"注册"按钮
2. 输入邮箱和验证码
3. 完成注册

### 测试发布笔记

1. 登录后，点击发布按钮
2. 填写标题和内容
3. 点击发布
4. 检查是否发布成功

### 测试PWA功能

1. 用手机浏览器访问你的前端URL
2. 浏览器应该会提示"添加到主屏幕"
3. 点击"添加到主屏幕"
4. 从桌面图标打开应用

---

## 六、常见问题

### Q1：构建失败，提示找不到npm

A：在Cloudflare Pages设置中，确保Node version设置为18，并且构建命令正确。

### Q2：后端部署失败，提示无法连接数据库

A：确保MySQL和Redis的环境变量已正确配置，并重新部署。

### Q3：前端无法访问后端API

A：
1. 检查后端是否部署成功
2. 检查前端API配置是否指向正确的后端URL
3. 查看浏览器开发者工具中的网络请求错误

### Q4：图片/视频无法上传

A：在Railway中，需要配置持久化存储或使用外部对象存储（如阿里云OSS）。

---

## 总结

完成以上步骤后，你的应用应该已经成功部署：

- **前端**: https://xxx.pages.dev
- **后端**: https://xxx.railway.app

用户可以通过浏览器访问前端，并添加到手机桌面作为PWA应用使用。