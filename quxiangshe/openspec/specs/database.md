# 数据库表设计

## 核心表清单

| 表名 | 用途 | 分片策略 |
|------|------|----------|
| user | 用户信息 | 按user_id |
| post | 动态内容 | 按user_id |
| feed | Feed流关系 | 按user_id |
| comment | 评论内容 | 按time_bucket |
| ai_summary | AI摘要 | 与post同表 |

---

## 1. user 用户表

```sql
CREATE TABLE `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  `avatar_url` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
  `bio` VARCHAR(500) DEFAULT NULL COMMENT '个人简介',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0禁用1正常2待审核',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_phone` (`phone`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

### 索引说明

| 索引名 | 字段 | 类型 | 说明 |
|--------|------|------|------|
| PRIMARY | id | 主键 | 自增主键 |
| uk_username | username | 唯一 | 登录账号唯一 |
| uk_phone | phone | 唯一 | 手机号唯一 |
| uk_email | email | 唯一 | 邮箱唯一 |
| idx_status | status | 普通 | 用户状态查询 |
| idx_created_at | created_at | 普通 | 时间排序 |

---

## 2. post 动态表

```sql
CREATE TABLE `post` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '动态ID',
  `user_id` BIGINT NOT NULL COMMENT '发布者用户ID',
  `content` TEXT NOT NULL COMMENT '动态内容',
  `media_urls` JSON DEFAULT NULL COMMENT '媒体URL数组[url1,url2]',
  `media_types` JSON DEFAULT NULL COMMENT '媒体类型数组[image,video]',
  `ai_summary` VARCHAR(500) DEFAULT NULL COMMENT 'AI摘要',
  `ai_summary_status` TINYINT NOT NULL DEFAULT 0 COMMENT 'AI摘要状态:0待生成1成功2失败',
  `visibility` TINYINT NOT NULL DEFAULT 0 COMMENT '可见性:0公开1粉丝可见2仅自己',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  `comment_count` INT NOT NULL DEFAULT 0 COMMENT '评论数',
  `share_count` INT NOT NULL DEFAULT 0 COMMENT '转发数',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除:0否1是',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_ai_summary_status` (`ai_summary_status`),
  KEY `idx_visibility_is_deleted` (`visibility`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态表';
```

### 索引说明

| 索引名 | 字段 | 类型 | 说明 |
|--------|------|------|------|
| PRIMARY | id | 主键 | 自增主键 |
| idx_user_id | user_id | 普通 | 用户动态查询 |
| idx_created_at | created_at | 普通 | Feed流排序 |
| idx_ai_summary_status | ai_summary_status | 普通 | AI任务查询 |
| idx_visibility_is_deleted | visibility, is_deleted | 复合 | 可见性过滤 |

### 高并发优化

- 分表策略：按user_id哈希分表，预留1024表
- 冷热分离：90天前数据归档到冷库

---

## 3. feed Feed流关系表

```sql
CREATE TABLE `feed` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `post_id` BIGINT NOT NULL COMMENT '动态ID',
  `creator_id` BIGINT NOT NULL COMMENT '动态创建者ID',
  `source_type` TINYINT NOT NULL DEFAULT 0 COMMENT '来源类型:0关注1收藏2推荐',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_post` (`user_id`, `post_id`),
  KEY `idx_user_created` (`user_id`, `created_at`),
  KEY `idx_creator_id` (`creator_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Feed流表';
```

### 索引说明

| 索引名 | 字段 | 类型 | 说明 |
|--------|------|------|------|
| PRIMARY | id | 主键 | 自增主键 |
| uk_user_post | user_id, post_id | 唯一 | 去重 |
| idx_user_created | user_id, created_at | 复合 | 用户Feed拉取 |
| idx_creator_id | creator_id | 普通 | 发布者查询 |
| idx_created_at | created_at | 普通 | 时间排序 |

### 推模式存储设计

- Redis收件箱：`inbox:{userId}` (List结构，左侧推入最新动态ID)
- 收件箱容量：每个用户最多保留200条，超出写MySQL兜底
- 数据结构：List `LPUSH inbox:{userId} postId`

---

## 4. comment 评论表

```sql
CREATE TABLE `comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `post_id` BIGINT NOT NULL COMMENT '动态ID',
  `user_id` BIGINT NOT NULL COMMENT '评论者用户ID',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父评论ID:0顶级评论',
  `root_id` BIGINT DEFAULT 0 COMMENT '根评论ID:顶级为自身ID',
  `content` VARCHAR(1000) NOT NULL COMMENT '评论内容',
  `at_users` JSON DEFAULT NULL COMMENT '@用户ID数组',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  `reply_count` INT NOT NULL DEFAULT 0 COMMENT '回复数',
  `time_bucket` BIGINT NOT NULL COMMENT '时间分桶:UNIX_TIMESTAMP/3600',
  `level` TINYINT NOT NULL DEFAULT 1 COMMENT '层级:1顶级2二级3+级',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除:0否1是',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_root_id` (`root_id`),
  KEY `idx_time_bucket` (`time_bucket`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_post_level` (`post_id`, `level`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';
```

### 索引说明

| 索引名 | 字段 | 类型 | 说明 |
|--------|------|------|------|
| PRIMARY | id | 主键 | 自增主键 |
| idx_post_id | post_id | 普通 | 动态评论查询 |
| idx_user_id | user_id | 普通 | 用户评论查询 |
| idx_parent_id | parent_id | 普通 | 回复查询 |
| idx_root_id | root_id | 普通 | 根评论查询 |
| idx_time_bucket | time_bucket | 普通 | 时间分桶查询 |
| idx_created_at | created_at | 普通 | 时间排序 |
| idx_post_level | post_id, level, created_at | 复合 | 两层扁平查询 |

### 无限层级设计

- 存储层：单表存储，parent_id自关联
- 展示层：API返回顶级评论+直接子评论(最多2层)
- 应用层：前端递归组装完整评论树
- 分桶：按小时分桶查询，减少全表扫描

### 高并发优化

- 异步写入：评论先写Redis队列 `comment:queue`，定时任务批量落库
- 批量落库：每5秒批量写入100条
- 读写分离：写主库，读从库

---

## 5. ai_summary AI摘要表

```sql
CREATE TABLE `ai_summary` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `post_id` BIGINT NOT NULL COMMENT '动态ID',
  `summary` VARCHAR(500) NOT NULL COMMENT 'AI摘要内容',
  `model` VARCHAR(50) DEFAULT NULL COMMENT '使用的AI模型',
  `tokens_used` INT DEFAULT NULL COMMENT '消耗Token数',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态:0处理中1成功2失败',
  `error_msg` VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_id` (`post_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI摘要表';
```

### 索引说明

| 索引名 | 字段 | 类型 | 说明 |
|--------|------|------|------|
| PRIMARY | id | 主键 | 自增主键 |
| uk_post_id | post_id | 唯一 | 动态关联 |
| idx_status | status | 普通 | 状态查询 |
| idx_created_at | created_at | 普通 | 时间排序 |

### 设计说明

- 实际上与post表通过post_id一一对应，可选择冗余到post表
- 本设计保留独立表，便于后续扩展摘要版本管理

---

## 补充表

### user_follow 用户关注关系表

```sql
CREATE TABLE `user_follow` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '关注者用户ID',
  `follow_user_id` BIGINT NOT NULL COMMENT '被关注者用户ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_follow` (`user_id`, `follow_user_id`),
  KEY `idx_follow_user_id` (`follow_user_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关注关系表';
```

### post_like 动态点赞表

```sql
CREATE TABLE `post_like` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `post_id` BIGINT NOT NULL COMMENT '动态ID',
  `user_id` BIGINT NOT NULL COMMENT '点赞用户ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_user` (`post_id`, `user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态点赞表';
```

### comment_like 评论点赞表

```sql
CREATE TABLE `comment_like` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `comment_id` BIGINT NOT NULL COMMENT '评论ID',
  `user_id` BIGINT NOT NULL COMMENT '点赞用户ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_comment_user` (`comment_id`, `user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论点赞表';
```

---

## 数据库连接配置

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/quxiangshe?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: ${MYSQL_PASSWORD}
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

---

## Redis缓存设计

| Key模式 | 类型 | 用途 | 过期时间 |
|---------|------|------|----------|
| `user:{userId}` | Hash | 用户信息缓存 | 30分钟 |
| `refresh:{userId}` | String | RefreshToken | 7天 |
| `inbox:{userId}` | List | 收件箱(推模式) | 7天 |
| `rate_limit:{userId}:{second}` | ZSet | 限流滑动窗口 | 60秒 |
| `comment:queue` | List | 评论异步队列 | 持久化 |
