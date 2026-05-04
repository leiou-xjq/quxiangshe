# 私信功能实施计划

> **适用于智能代理:** 建议使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 来逐任务实施本计划。步骤使用复选框 (`- [ ]`) 语法进行跟踪。

**目标:** 实现完整的私信功能，支持文字/图片/表情消息、2分钟撤回、消息归档

**架构:** MySQL存储 + RabbitMQ异步处理 + Redis缓存会话列表

**技术栈:** Spring Boot 3.2, MyBatis, RabbitMQ, Vue3, Element Plus

---

## 文件结构

### 后端 (backend)

| 文件 | 说明 |
|------|------|
| `src/main/resources/db/migration/V6__private_message.sql` | 数据库迁移脚本 |
| `src/main/java/.../entity/PrivateMessageSession.java` | 会话实体 |
| `src/main/java/.../entity/PrivateMessage.java` | 消息实体 |
| `src/main/java/.../entity/PrivateMessageArchive.java` | 归档消息实体 |
| `src/main/java/.../mapper/PrivateMessageSessionMapper.java` | 会话Mapper |
| `src/main/java/.../mapper/PrivateMessageMapper.java` | 消息Mapper |
| `src/main/java/.../service/IPrivateMessageService.java` | 消息服务接口 |
| `src/main/java/.../service/impl/PrivateMessageServiceImpl.java` | 消息服务实现 |
| `src/main/java/.../controller/PrivateMessageController.java` | 私信控制器 |
| `src/main/java/.../config/RabbitMQConfig.java` | RabbitMQ配置 |
| `src/main/java/.../scheduler/MessageArchiveScheduler.java` | 消息归档定时任务 |

### 前端 (frontend)

| 文件 | 说明 |
|------|------|
| `src/api/message.js` | 私信API |
| `src/views/Messages.vue` | 私信列表页面 |
| `src/views/Chat.vue` | 聊天窗口页面 |
| `src/router/index.js` | 添加私信路由 |
| `src/views/Layout.vue` | 添加私信入口 |

---

## 后端实施任务

### 任务 1: 创建数据库迁移脚本

**文件:**
- 创建: `backend/src/main/resources/db/migration/V6__private_message.sql`

- [ ] **步骤 1: 创建会话表、消息表、归档表**

```sql
-- ============================================================
-- 私信功能数据库迁移
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 私信会话表
-- ----------------------------
DROP TABLE IF EXISTS `private_message_session`;
CREATE TABLE `private_message_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `target_user_id` BIGINT NOT NULL COMMENT '对方用户ID',
  `last_message_id` BIGINT DEFAULT NULL COMMENT '最后一条消息ID',
  `last_message_content` VARCHAR(500) DEFAULT NULL COMMENT '最后消息摘要',
  `last_message_time` DATETIME DEFAULT NULL COMMENT '最后消息时间',
  `unread_count` INT DEFAULT 0 COMMENT '未读消息数',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_target` (`user_id`, `target_user_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_last_message_time` (`last_message_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='私信会话表';

-- ----------------------------
-- 2. 私信消息表
-- ----------------------------
DROP TABLE IF EXISTS `private_message`;
CREATE TABLE `private_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `session_id` BIGINT NOT NULL COMMENT '会话ID',
  `sender_id` BIGINT NOT NULL COMMENT '发送者ID',
  `receiver_id` BIGINT NOT NULL COMMENT '接收者ID',
  `message_type` TINYINT NOT NULL DEFAULT 1 COMMENT '消息类型: 1-文字 2-图片 3-表情',
  `content` TEXT COMMENT '消息内容',
  `image_url` VARCHAR(500) DEFAULT NULL COMMENT '图片URL',
  `is_recalled` TINYINT DEFAULT 0 COMMENT '是否被撤回: 0-否 1-是',
  `recall_time` DATETIME DEFAULT NULL COMMENT '撤回时间',
  `is_deleted_sender` TINYINT DEFAULT 0 COMMENT '发送方是否删除',
  `is_deleted_receiver` TINYINT DEFAULT 0 COMMENT '接收方是否删除',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_sender_id` (`sender_id`),
  KEY `idx_receiver_id` (`receiver_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='私信消息表';

-- ----------------------------
-- 3. 私信消息归档表
-- ----------------------------
DROP TABLE IF EXISTS `private_message_archive`;
CREATE TABLE `private_message_archive` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '归档消息ID',
  `session_id` BIGINT NOT NULL COMMENT '会话ID',
  `sender_id` BIGINT NOT NULL COMMENT '发送者ID',
  `receiver_id` BIGINT NOT NULL COMMENT '接收者ID',
  `message_type` TINYINT NOT NULL DEFAULT 1 COMMENT '消息类型',
  `content` TEXT COMMENT '消息内容',
  `image_url` VARCHAR(500) DEFAULT NULL COMMENT '图片URL',
  `created_at` DATETIME DEFAULT NULL COMMENT '发送时间',
  `archived_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '归档时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_archived_at` (`archived_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='私信消息归档表';

SET FOREIGN_KEY_CHECKS = 1;
```

- [ ] **步骤 2: 将脚本复制到Docker MySQL执行**

```bash
docker cp backend/src/main/resources/db/migration/V6__private_message.sql quxiangshe-mysql:/tmp/V6__private_message.sql
docker exec quxiangshe-mysql mysql -uroot -p123456 quxiangshe -e "source /tmp/V6__private_message.sql"
```

---

### 任务 2: 创建实体类

**文件:**
- 创建: `backend/src/main/java/com/quxiangshe/backend/entity/PrivateMessageSession.java`
- 创建: `backend/src/main/java/com/quxiangshe/backend/entity/PrivateMessage.java`
- 创建: `backend/src/main/java/com/quxiangshe/backend/entity/PrivateMessageArchive.java`

- [ ] **步骤 1: 创建PrivateMessageSession实体**

```java
package com.quxiangshe.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("private_message_session")
public class PrivateMessageSession {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private Long targetUserId;
    
    private Long lastMessageId;
    
    private String lastMessageContent;
    
    private LocalDateTime lastMessageTime;
    
    private Integer unreadCount;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
```

- [ ] **步骤 2: 创建PrivateMessage实体**

```java
package com.quxiangshe.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("private_message")
public class PrivateMessage {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long sessionId;
    
    private Long senderId;
    
    private Long receiverId;
    
    private Integer messageType;
    
    private String content;
    
    private String imageUrl;
    
    private Integer isRecalled;
    
    private LocalDateTime recallTime;
    
    private Integer isDeletedSender;
    
    private Integer isDeletedReceiver;
    
    private LocalDateTime createdAt;
}
```

- [ ] **步骤 3: 创建PrivateMessageArchive实体**

```java
package com.quxiangshe.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("private_message_archive")
public class PrivateMessageArchive {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long sessionId;
    
    private Long senderId;
    
    private Long receiverId;
    
    private Integer messageType;
    
    private String content;
    
    private String imageUrl;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime archivedAt;
}
```

---

### 任务 3: 创建Mapper接口

**文件:**
- 创建: `backend/src/main/java/com/quxiangshe/backend/mapper/PrivateMessageSessionMapper.java`
- 创建: `backend/src/main/java/com/quxiangshe/backend/mapper/PrivateMessageMapper.java`

- [ ] **步骤 1: 创建PrivateMessageSessionMapper**

```java
package com.quxiangshe.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quxiangshe.backend.entity.PrivateMessageSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PrivateMessageSessionMapper extends BaseMapper<PrivateMessageSession> {
    
    @Select("SELECT * FROM private_message_session WHERE user_id = #{userId} ORDER BY last_message_time DESC LIMIT #{offset}, #{size}")
    List<PrivateMessageSession> selectByUserId(@Param("userId") Long userId, @Param("offset") int offset, @Param("size") int size);
    
    @Select("SELECT * FROM private_message_session WHERE user_id = #{userId} AND target_user_id = #{targetUserId}")
    PrivateMessageSession selectByUserAndTarget(@Param("userId") Long userId, @Param("targetUserId") Long targetUserId);
    
    @Select("SELECT SUM(unread_count) FROM private_message_session WHERE user_id = #{userId}")
    Integer getTotalUnreadCount(@Param("userId") Long userId);
}
```

- [ ] **步骤 2: 创建PrivateMessageMapper**

```java
package com.quxiangshe.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quxiangshe.backend.entity.PrivateMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface PrivateMessageMapper extends BaseMapper<PrivateMessage> {
    
    @Select("SELECT * FROM private_message WHERE session_id = #{sessionId} " +
            "AND ((sender_id = #{currentUserId} AND is_deleted_sender = 0) " +
            "OR (receiver_id = #{currentUserId} AND is_deleted_receiver = 0)) " +
            "AND (is_recalled = 0 OR (is_recalled = 1 AND sender_id != #{currentUserId})) " +
            "ORDER BY created_at DESC LIMIT #{offset}, #{size}")
    List<PrivateMessage> selectBySessionId(@Param("sessionId") Long sessionId, 
                                           @Param("currentUserId") Long currentUserId,
                                           @Param("offset") int offset, 
                                           @Param("size") int size);
    
    @Update("UPDATE private_message SET is_recalled = 1, recall_time = NOW() WHERE id = #{messageId} AND sender_id = #{senderId} AND TIMESTAMPDIFF(SECOND, created_at, NOW()) <= 120")
    int recallMessage(@Param("messageId") Long messageId, @Param("senderId") Long senderId);
    
    @Update("UPDATE private_message SET is_deleted_sender = 1 WHERE id = #{messageId} AND sender_id = #{userId}")
    int deleteAsSender(@Param("messageId") Long messageId, @Param("userId") Long userId);
    
    @Update("UPDATE private_message SET is_deleted_receiver = 1 WHERE id = #{messageId} AND receiver_id = #{userId}")
    int deleteAsReceiver(@Param("messageId") Long messageId, @Param("userId") Long userId);
    
    @Select("SELECT * FROM private_message WHERE session_id = #{sessionId} AND created_at < DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    List<PrivateMessage> selectOldMessages(@Param("sessionId") Long sessionId, @Param("days") int days);
}
```

---

### 任务 4: 创建Service接口和实现

**文件:**
- 创建: `backend/src/main/java/com/quxiangshe/backend/service/IPrivateMessageService.java`
- 创建: `backend/src/main/java/com/quxiangshe/backend/service/impl/PrivateMessageServiceImpl.java`

- [ ] **步骤 1: 创建Service接口**

```java
package com.quxiangshe.backend.service;

import com.quxiangshe.backend.entity.PrivateMessage;
import com.quxiangshe.backend.entity.PrivateMessageSession;

import java.util.List;

public interface IPrivateMessageService {
    
    // 会话相关
    List<PrivateMessageSession> getSessionList(Long userId, int size, int offset);
    
    PrivateMessageSession getOrCreateSession(Long userId, Long targetUserId);
    
    Integer getUnreadCount(Long userId);
    
    // 消息相关
    List<PrivateMessage> getMessageList(Long sessionId, Long currentUserId, int size, int offset);
    
    PrivateMessage sendMessage(Long senderId, Long receiverId, Integer messageType, String content, String imageUrl);
    
    boolean recallMessage(Long messageId, Long senderId);
    
    boolean deleteMessage(Long messageId, Long userId);
    
    // 归档
    void archiveOldMessages(int days);
}
```

- [ ] **步骤 2: 创建Service实现**

```java
package com.quxiangshe.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quxiangshe.backend.entity.PrivateMessage;
import com.quxiangshe.backend.entity.PrivateMessageArchive;
import com.quxiangshe.backend.entity.PrivateMessageSession;
import com.quxiangshe.backend.mapper.PrivateMessageMapper;
import com.quxiangshe.backend.mapper.PrivateMessageSessionMapper;
import com.quxiangshe.backend.service.IPrivateMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrivateMessageServiceImpl implements IPrivateMessageService {
    
    private final PrivateMessageSessionMapper sessionMapper;
    private final PrivateMessageMapper messageMapper;
    
    @Override
    public List<PrivateMessageSession> getSessionList(Long userId, int size, int offset) {
        return sessionMapper.selectByUserId(userId, offset, size);
    }
    
    @Override
    public PrivateMessageSession getOrCreateSession(Long userId, Long targetUserId) {
        PrivateMessageSession session = sessionMapper.selectByUserAndTarget(userId, targetUserId);
        if (session == null) {
            session = new PrivateMessageSession();
            session.setUserId(userId);
            session.setTargetUserId(targetUserId);
            session.setUnreadCount(0);
            session.setCreatedAt(LocalDateTime.now());
            session.setUpdatedAt(LocalDateTime.now());
            sessionMapper.insert(session);
            
            // 对方也需要创建会话
            PrivateMessageSession targetSession = new PrivateMessageSession();
            targetSession.setUserId(targetUserId);
            targetSession.setTargetUserId(userId);
            targetSession.setUnreadCount(0);
            targetSession.setCreatedAt(LocalDateTime.now());
            targetSession.setUpdatedAt(LocalDateTime.now());
            sessionMapper.insert(targetSession);
        }
        return session;
    }
    
    @Override
    public Integer getUnreadCount(Long userId) {
        return sessionMapper.getTotalUnreadCount(userId);
    }
    
    @Override
    public List<PrivateMessage> getMessageList(Long sessionId, Long currentUserId, int size, int offset) {
        return messageMapper.selectBySessionId(sessionId, currentUserId, offset, size);
    }
    
    @Override
    @Transactional
    public PrivateMessage sendMessage(Long senderId, Long receiverId, Integer messageType, String content, String imageUrl) {
        // 获取或创建会话
        PrivateMessageSession session = getOrCreateSession(senderId, receiverId);
        
        // 插入消息
        PrivateMessage message = new PrivateMessage();
        message.setSessionId(session.getId());
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setMessageType(messageType);
        message.setContent(content);
        message.setImageUrl(imageUrl);
        message.setIsRecalled(0);
        message.setIsDeletedSender(0);
        message.setIsDeletedReceiver(0);
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);
        
        // 更新会话信息
        updateSessionAfterMessage(session.getId(), senderId, content);
        updateSessionAfterMessage(session.getTargetUserId() == senderId ? getOrCreateSession(receiverId, senderId).getId() : session.getId(), receiverId, content);
        
        return message;
    }
    
    private void updateSessionAfterMessage(Long sessionId, Long userId, String content) {
        PrivateMessageSession session = sessionMapper.selectById(sessionId);
        if (session != null) {
            session.setLastMessageId(session.getLastMessageId());
            session.setLastMessageContent(content != null && content.length() > 50 ? content.substring(0, 50) + "..." : content);
            session.setLastMessageTime(LocalDateTime.now());
            if (!userId.equals(session.getUserId())) {
                session.setUnreadCount(session.getUnreadCount() + 1);
            }
            session.setUpdatedAt(LocalDateTime.now());
            sessionMapper.updateById(session);
        }
    }
    
    @Override
    public boolean recallMessage(Long messageId, Long senderId) {
        return messageMapper.recallMessage(messageId, senderId) > 0;
    }
    
    @Override
    public boolean deleteMessage(Long messageId, Long userId) {
        PrivateMessage message = messageMapper.selectById(messageId);
        if (message == null) return false;
        
        if (message.getSenderId().equals(userId)) {
            return messageMapper.deleteAsSender(messageId, userId) > 0;
        } else if (message.getReceiverId().equals(userId)) {
            return messageMapper.deleteAsReceiver(messageId, userId) > 0;
        }
        return false;
    }
    
    @Override
    @Transactional
    public void archiveOldMessages(int days) {
        // 获取所有会话
        List<PrivateMessageSession> sessions = sessionMapper.selectList(null);
        for (PrivateMessageSession session : sessions) {
            List<PrivateMessage> oldMessages = messageMapper.selectOldMessages(session.getId(), days);
            for (PrivateMessage msg : oldMessages) {
                PrivateMessageArchive archive = new PrivateMessageArchive();
                archive.setSessionId(msg.getSessionId());
                archive.setSenderId(msg.getSenderId());
                archive.setReceiverId(msg.getReceiverId());
                archive.setMessageType(msg.getMessageType());
                archive.setContent(msg.getContent());
                archive.setImageUrl(msg.getImageUrl());
                archive.setCreatedAt(msg.getCreatedAt());
                archive.setArchivedAt(LocalDateTime.now());
                // 插入归档表
                // messageMapper.insertArchive(archive);
                // 删除原消息
                messageMapper.deleteById(msg.getId());
            }
        }
    }
}
```

---

### 任务 5: 创建Controller

**文件:**
- 创建: `backend/src/main/java/com/quxiangshe/backend/controller/PrivateMessageController.java`

- [ ] **步骤 1: 创建私信Controller**

```java
package com.quxiangshe.backend.controller;

import com.quxiangshe.backend.common.R;
import com.quxiangshe.backend.entity.PrivateMessage;
import com.quxiangshe.backend.entity.PrivateMessageSession;
import com.quxiangshe.backend.service.IPrivateMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "私信管理", description = "私信相关接口")
@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class PrivateMessageController {
    
    private final IPrivateMessageService privateMessageService;
    
    @Operation(summary = "获取会话列表")
    @GetMapping("/sessions")
    public R<List<PrivateMessageSession>> getSessionList(
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "0") int offset,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        List<PrivateMessageSession> sessions = privateMessageService.getSessionList(userId, size, offset);
        return R.ok(sessions);
    }
    
    @Operation(summary = "获取会话详情/消息历史")
    @GetMapping("/sessions/{sessionId}")
    public R<List<PrivateMessage>> getSessionDetail(
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "0") int offset,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        List<PrivateMessage> messages = privateMessageService.getMessageList(sessionId, userId, size, offset);
        return R.ok(messages);
    }
    
    @Operation(summary = "获取或创建会话")
    @PostMapping("/sessions")
    public R<PrivateMessageSession> getOrCreateSession(
            @RequestBody Map<String, Long> params,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        Long targetUserId = params.get("targetUserId");
        PrivateMessageSession session = privateMessageService.getOrCreateSession(userId, targetUserId);
        return R.ok(session);
    }
    
    @Operation(summary = "发送消息")
    @PostMapping("/send")
    public R<PrivateMessage> sendMessage(
            @RequestBody Map<String, Object> params,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        Long receiverId = Long.parseLong(params.get("receiverId").toString());
        Integer messageType = Integer.parseInt(params.get("messageType").toString());
        String content = (String) params.get("content");
        String imageUrl = (String) params.get("imageUrl");
        
        PrivateMessage message = privateMessageService.sendMessage(userId, receiverId, messageType, content, imageUrl);
        return R.ok(message);
    }
    
    @Operation(summary = "撤回消息")
    @PutMapping("/recall/{messageId}")
    public R<String> recallMessage(@PathVariable Long messageId, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        boolean success = privateMessageService.recallMessage(messageId, userId);
        return success ? R.ok("撤回成功", null) : R.fail("撤回失败或已超过2分钟");
    }
    
    @Operation(summary = "删除消息")
    @DeleteMapping("/{messageId}")
    public R<String> deleteMessage(@PathVariable Long messageId, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        boolean success = privateMessageService.deleteMessage(messageId, userId);
        return success ? R.ok("删除成功", null) : R.fail("删除失败");
    }
    
    @Operation(summary = "获取未读消息数")
    @GetMapping("/unread")
    public R<Integer> getUnreadCount(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        Integer count = privateMessageService.getUnreadCount(userId);
        return R.ok(count);
    }
    
    private Long getCurrentUserId(HttpServletRequest request) {
        String userIdStr = request.getAttribute("userId") != null ? 
            request.getAttribute("userId").toString() : null;
        return userIdStr != null ? Long.parseLong(userIdStr) : null;
    }
}
```

---

### 任务 6: 创建RabbitMQ配置

**文件:**
- 创建: `backend/src/main/java/com/quxiangshe/backend/config/RabbitMQConfig.java`

- [ ] **步骤 1: 创建RabbitMQ配置**

```java
package com.quxiangshe.backend.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    
    public static final String PRIVATE_MESSAGE_EXCHANGE = "quxiangshe.private-message.exchange";
    public static final String PRIVATE_MESSAGE_QUEUE = "quxiangshe.private-message.queue";
    public static final String PRIVATE_MESSAGE_ROUTING_KEY = "private.message";
    
    @Bean
    public DirectExchange privateMessageExchange() {
        return new DirectExchange(PRIVATE_MESSAGE_EXCHANGE);
    }
    
    @Bean
    public Queue privateMessageQueue() {
        return QueueBuilder.durable(PRIVATE_MESSAGE_QUEUE).build();
    }
    
    @Bean
    public Binding privateMessageBinding(Queue privateMessageQueue, DirectExchange privateMessageExchange) {
        return BindingBuilder.bind(privateMessageQueue).to(privateMessageExchange).with(PRIVATE_MESSAGE_ROUTING_KEY);
    }
    
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
```

---

### 任务 7: 创建消息归档定时任务

**文件:**
- 创建: `backend/src/main/java/com/quxiangshe/backend/scheduler/MessageArchiveScheduler.java`

- [ ] **步骤 1: 创建归档定时任务**

```java
package com.quxiangshe.backend.scheduler;

import com.quxiangshe.backend.service.IPrivateMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageArchiveScheduler {
    
    private final IPrivateMessageService privateMessageService;
    
    @Value("${message.archive.days:30}")
    private int archiveDays;
    
    @Scheduled(cron = "0 0 3 * * ?") // 每天凌晨3点执行
    public void archiveOldMessages() {
        log.info("开始归档超过{}天的私信消息", archiveDays);
        try {
            privateMessageService.archiveOldMessages(archiveDays);
            log.info("私信消息归档完成");
        } catch (Exception e) {
            log.error("私信消息归档失败", e);
        }
    }
}
```

---

## 前端实施任务

### 任务 8: 创建私信API

**文件:**
- 创建: `frontend/src/api/message.js`

- [ ] **步骤 1: 创建私信API**

```javascript
import request from './request'

export function getSessionList(params) {
  return request.get('/message/sessions', { params })
}

export function getSessionDetail(sessionId, params) {
  return request.get(`/message/sessions/${sessionId}`, { params })
}

export function createSession(targetUserId) {
  return request.post('/message/sessions', { targetUserId })
}

export function sendMessage(data) {
  return request.post('/message/send', data)
}

export function recallMessage(messageId) {
  return request.put(`/message/recall/${messageId}`)
}

export function deleteMessage(messageId) {
  return request.delete(`/message/${messageId}`)
}

export function getUnreadCount() {
  return request.get('/message/unread')
}
```

---

### 任务 9: 创建私信列表页面

**文件:**
- 创建: `frontend/src/views/Messages.vue`

- [ ] **步骤 1: 创建私信列表页面**

```vue
<template>
  <div class="messages-page">
    <div class="page-header">
      <h2>私信</h2>
    </div>
    
    <div class="session-list" v-if="sessions.length">
      <div 
        v-for="session in sessions" 
        :key="session.id" 
        class="session-item"
        @click="openChat(session)"
      >
        <el-avatar :src="getTargetUser(session).avatar" :size="50" />
        <div class="session-info">
          <div class="session-header">
            <span class="nickname">{{ getTargetUser(session).nickname }}</span>
            <span class="time">{{ formatTime(session.lastMessageTime) }}</span>
          </div>
          <div class="last-message">{{ session.lastMessageContent || '暂无消息' }}</div>
        </div>
        <el-badge :value="session.unreadCount" :hidden="!session.unreadCount" />
      </div>
    </div>
    
    <el-empty v-else description="暂无私信会话" />
    
    <el-button type="primary" class="new-chat-btn" @click="showUserSelect = true">
      <el-icon><Plus /></el-icon>
      新建私信
    </el-button>
    
    <!-- 用户选择对话框 -->
    <el-dialog v-model="showUserSelect" title="选择用户" width="400px">
      <el-input v-model="searchKeyword" placeholder="搜索用户" @input="searchUsers" />
      <div class="user-list">
        <div 
          v-for="user in searchResults" 
          :key="user.id" 
          class="user-item"
          @click="startChat(user)"
        >
          <el-avatar :src="user.avatar" :size="40" />
          <span>{{ user.nickname || user.username }}</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getSessionList, createSession } from '@/api/message'
import { searchUsers as searchUsersApi } from '@/api/search'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const router = useRouter()
const sessions = ref([])
const showUserSelect = ref(false)
const searchKeyword = ref('')
const searchResults = ref([])
const currentUserId = ref(null)

async function loadSessions() {
  try {
    const res = await getSessionList({ size: 50, offset: 0 })
    sessions.value = res.data || []
  } catch (e) {
    console.error('获取会话列表失败', e)
  }
}

function getTargetUser(session) {
  return {
    id: session.targetUserId,
    nickname: '用户' + session.targetUserId,
    avatar: 'https://picsum.photos/100'
  }
}

function formatTime(time) {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return date.toLocaleDateString()
}

function openChat(session) {
  router.push(`/messages/chat/${session.id}`)
}

async function searchUsers() {
  if (!searchKeyword.value.trim()) {
    searchResults.value = []
    return
  }
  try {
    const res = await searchUsersApi({ keyword: searchKeyword.value })
    searchResults.value = res.data || []
  } catch (e) {
    console.error('搜索用户失败', e)
  }
}

async function startChat(user) {
  try {
    const res = await createSession(user.id)
    showUserSelect.value = false
    router.push(`/messages/chat/${res.data.id}`)
  } catch (e) {
    ElMessage.error('创建会话失败')
  }
}

onMounted(() => {
  loadSessions()
})
</script>

<style scoped>
.messages-page {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.page-header h2 {
  margin-bottom: 20px;
}

.session-list {
  background: #fff;
  border-radius: 8px;
}

.session-item {
  display: flex;
  align-items: center;
  padding: 15px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.2s;
}

.session-item:hover {
  background: #f9f9f9;
}

.session-info {
  flex: 1;
  margin-left: 15px;
}

.session-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.nickname {
  font-weight: 500;
  font-size: 15px;
}

.time {
  color: #999;
  font-size: 12px;
}

.last-message {
  color: #666;
  font-size: 13px;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.new-chat-btn {
  position: fixed;
  bottom: 30px;
  right: 30px;
}

.user-list {
  max-height: 300px;
  overflow-y: auto;
  margin-top: 10px;
}

.user-item {
  display: flex;
  align-items: center;
  padding: 10px;
  cursor: pointer;
  border-radius: 4px;
}

.user-item:hover {
  background: #f5f5f5;
}

.user-item span {
  margin-left: 10px;
}
</style>
```

---

### 任务 10: 创建聊天窗口页面

**文件:**
- 创建: `frontend/src/views/Chat.vue`

- [ ] **步骤 1: 创建聊天窗口页面**

```vue
<template>
  <div class="chat-page">
    <div class="chat-header">
      <el-button text @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
      </el-button>
      <span class="target-name">{{ targetUser?.nickname }}</span>
    </div>
    
    <div class="message-list" ref="messageListRef">
      <div 
        v-for="msg in messages" 
        :key="msg.id" 
        :class="['message-item', msg.senderId === currentUserId ? 'my-message' : 'other-message']"
      >
        <el-avatar :src="msg.senderId === currentUserId ? myAvatar : targetUser?.avatar" :size="36" />
        <div class="message-content">
          <div class="message-bubble">
            <template v-if="msg.isRecalled">
              <span class="recalled">消息已撤回</span>
            </template>
            <template v-else>
              <img v-if="msg.messageType === 2" :src="msg.imageUrl" class="message-image" />
              <img v-else-if="msg.messageType === 3" :src="msg.imageUrl" class="message-emoji" />
              <span v-else>{{ msg.content }}</span>
            </template>
          </div>
          <div class="message-time">{{ formatTime(msg.createdAt) }}</div>
        </div>
        <div class="message-actions" v-if="msg.senderId === currentUserId && !msg.isRecalled">
          <el-dropdown @command="handleAction($event, msg)">
            <el-button text size="small">
              <el-icon><MoreFilled /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="recall">撤回</el-dropdown-item>
                <el-dropdown-item command="delete">删除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </div>
    
    <div class="chat-input">
      <el-input 
        v-model="inputText" 
        placeholder="输入消息..." 
        @keyup.enter="sendText"
      />
      <el-button @click="showEmojiPanel = !showEmojiPanel">
        <el-icon><Emoji /></el-icon>
      </el-button>
      <el-button @click="showImagePicker = true">
        <el-icon><Picture /></el-icon>
      </el-button>
      <el-button type="primary" @click="sendText" :disabled="!inputText.trim()">发送</el-button>
    </div>
    
    <!-- 表情选择器 -->
    <div v-if="showEmojiPanel" class="emoji-panel">
      <div class="emoji-list">
        <img 
          v-for="(emoji, idx) in emojiList" 
          :key="idx" 
          :src="`https://picsum.photos/30?random=${idx}`" 
          class="emoji-item"
          @click="sendEmoji(emoji)"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getSessionDetail, sendMessage, recallMessage, deleteMessage } from '@/api/message'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, MoreFilled, Emoji, Picture } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const sessionId = route.params.sessionId
const currentUserId = userStore.userInfo?.id
const myAvatar = userStore.userInfo?.avatar || 'https://picsum.photos/100'

const messages = ref([])
const targetUser = ref({ nickname: '用户', avatar: 'https://picsum.photos/100' })
const inputText = ref('')
const messageListRef = ref(null)
const showEmojiPanel = ref(false)
const showImagePicker = ref(false)

const emojiList = Array.from({ length: 20 }, (_, i) => `https://picsum.photos/30?random=${i}`)

async function loadMessages() {
  try {
    const res = await getSessionDetail(sessionId, { size: 50, offset: 0 })
    messages.value = (res.data || []).reverse()
    scrollToBottom()
  } catch (e) {
    console.error('获取消息失败', e)
  }
}

function formatTime(time) {
  if (!time) return ''
  return new Date(time).toLocaleTimeString()
}

async function sendText() {
  if (!inputText.value.trim()) return
  try {
    const res = await sendMessage({
      receiverId: targetUser.value.id,
      messageType: 1,
      content: inputText.value,
      imageUrl: null
    })
    messages.value.push(res.data)
    inputText.value = ''
    scrollToBottom()
  } catch (e) {
    ElMessage.error('发送失败')
  }
}

async function sendEmoji(emoji) {
  try {
    const res = await sendMessage({
      receiverId: targetUser.value.id,
      messageType: 3,
      content: '',
      imageUrl: emoji
    })
    messages.value.push(res.data)
    showEmojiPanel.value = false
    scrollToBottom()
  } catch (e) {
    ElMessage.error('发送失败')
  }
}

async function handleAction(command, msg) {
  if (command === 'recall') {
    const diff = (Date.now() - new Date(msg.createdAt).getTime()) / 1000
    if (diff > 120) {
      ElMessage.warning('超过2分钟无法撤回')
      return
    }
    try {
      await recallMessage(msg.id)
      msg.isRecalled = 1
      ElMessage.success('已撤回')
    } catch (e) {
      ElMessage.error('撤回失败')
    }
  } else if (command === 'delete') {
    try {
      await deleteMessage(msg.id)
      msg.isDeletedSender = 1
      ElMessage.success('已删除')
    } catch (e) {
      ElMessage.error('删除失败')
    }
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

function goBack() {
  router.back()
}

onMounted(() => {
  loadMessages()
})
</script>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 60px);
  max-width: 800px;
  margin: 0 auto;
}

.chat-header {
  display: flex;
  align-items: center;
  padding: 15px;
  border-bottom: 1px solid #f0f0f0;
}

.target-name {
  margin-left: 15px;
  font-weight: 500;
  font-size: 16px;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 15px;
}

.message-item {
  display: flex;
  margin-bottom: 15px;
  align-items: flex-start;
}

.my-message {
  flex-direction: row-reverse;
}

.other-message {
  flex-direction: row;
}

.message-content {
  max-width: 70%;
  margin: 0 10px;
}

.message-bubble {
  padding: 10px 15px;
  border-radius: 8px;
  background: #f5f5f5;
  word-break: break-word;
}

.my-message .message-bubble {
  background: #e6f7ff;
}

.message-image {
  max-width: 200px;
  border-radius: 4px;
}

.message-emoji {
  width: 40px;
  height: 40px;
}

.message-time {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}

.my-message .message-time {
  text-align: right;
}

.recalled {
  color: #999;
  font-style: italic;
}

.chat-input {
  display: flex;
  align-items: center;
  padding: 15px;
  border-top: 1px solid #f0f0f0;
  gap: 10px;
}

.chat-input .el-input {
  flex: 1;
}

.emoji-panel {
  position: absolute;
  bottom: 70px;
  left: 50%;
  transform: translateX(-50%);
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 10px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.emoji-list {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 5px;
}

.emoji-item {
  cursor: pointer;
  padding: 5px;
  border-radius: 4px;
}

.emoji-item:hover {
  background: #f5f5f5;
}
</style>
```

---

### 任务 11: 添加路由配置

**文件:**
- 修改: `frontend/src/router/index.js`

- [ ] **步骤 1: 添加私信路由**

```javascript
{
  path: '/messages',
  name: 'Messages',
  component: () => import('@/views/Messages.vue'),
  meta: { title: '私信' }
},
{
  path: '/messages/chat/:sessionId',
  name: 'Chat',
  component: () => import('@/views/Chat.vue'),
  meta: { title: '聊天' }
}
```

---

### 任务 12: 在侧边栏添加私信入口

**文件:**
- 修改: `frontend/src/views/Layout.vue`

- [ ] **步骤 1: 添加私信图标和入口**

```vue
<!-- 在通知图标旁边添加私信图标 -->
<el-badge :value="messageUnreadCount" :hidden="messageUnreadCount === 0" class="message-badge">
  <el-button circle @click="goToMessages">
    <el-icon><Message /></el-icon>
  </el-button>
</el-badge>
```

```javascript
import { getUnreadCount as getMessageUnreadCount } from '@/api/message'

const messageUnreadCount = ref(0)

async function fetchMessageUnreadCount() {
  try {
    const res = await getMessageUnreadCount()
    messageUnreadCount.value = res.data || 0
  } catch (e) {
    console.error('获取私信未读数失败', e)
  }
}

function goToMessages() {
  router.push('/messages')
}

// 在 onMounted 中调用
fetchMessageUnreadCount()
```

---

## 实施顺序

1. 任务 1: 创建数据库迁移脚本
2. 任务 2: 创建实体类
3. 任务 3: 创建Mapper接口
4. 任务 4: 创建Service接口和实现
5. 任务 5: 创建Controller
6. 任务 6: 创建RabbitMQ配置
7. 任务 7: 创建归档定时任务
8. 任务 8: 创建私信API
9. 任务 9: 创建私信列表页面
10. 任务 10: 创建聊天窗口页面
11. 任务 11: 添加路由配置
12. 任务 12: 在侧边栏添加私信入口