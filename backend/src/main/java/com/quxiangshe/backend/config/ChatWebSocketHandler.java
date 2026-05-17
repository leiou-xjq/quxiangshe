package com.quxiangshe.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quxiangshe.backend.entity.PrivateMessage;
import com.quxiangshe.backend.service.IPrivateMessageService;
import com.quxiangshe.backend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天WebSocket处理器
 * <p>负责WebSocket连接的生命周期管理和私信消息的实时推送：
 * <ul>
 *   <li>连接建立：从URL参数中提取JWT token验证身份，通过后注册到sessions映射表</li>
 *   <li>消息处理：接收JSON消息，解析type字段（目前仅支持"message"），调用私信服务持久化后实时推送</li>
 *   <li>连接关闭：从sessions映射表中移除，释放资源</li>
 *   <li>通知推送：提供sendNotification方法，供其他服务向在线用户推送实时通知</li>
 * </ul>
 * sessions使用ConcurrentHashMap保证线程安全。</p>
 * 
 * @author 趣享社技术团队
 */
@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final IPrivateMessageService privateMessageService;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public ChatWebSocketHandler(IPrivateMessageService privateMessageService, JwtUtil jwtUtil) {
        this.privateMessageService = privateMessageService;
        this.jwtUtil = jwtUtil;
        this.objectMapper = new ObjectMapper();
    }

    /** userId -> WebSocketSession映射，线程安全 */
    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /**
     * 根据用户ID获取WebSocket会话
     * 
     * @param userId 用户ID
     * @return WebSocket会话，用户不在线返回null
     */
    public WebSocketSession getSession(Long userId) {
        return sessions.get(userId);
    }

    /**
     * WebSocket连接建立回调
     * <p>从URL查询参数中提取token进行JWT验证，验证通过后将session存入映射表。
     * 验证失败则直接关闭连接。</p>
     * 
     * @param session WebSocket会话
     * @throws Exception 连接异常
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = getTokenFromSession(session);
        if (token != null && jwtUtil.isTokenValid(token)) {
            Long userId = jwtUtil.getUserIdFromToken(token);
            sessions.put(userId, session);
            session.getAttributes().put("userId", userId);
            log.info("WebSocket connected: userId={}", userId);
        } else {
            log.warn("WebSocket connection rejected: invalid token");
            session.close();
        }
    }

    /**
     * 接收并处理文本消息
     * <p>解析JSON消息体，根据type字段路由到不同处理方法。
     * 目前支持的type：message（私信）。</p>
     * 
     * @param session WebSocket会话
     * @param message 文本消息
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) {
            return;
        }

        String payload = message.getPayload();
        log.info("Received message from user {}: {}", userId, payload);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            String type = (String) data.get("type");

            // 消息类型路由：目前仅支持普通私信
            if ("message".equals(type)) {
                handleChatMessage(userId, data);
            }
        } catch (IOException e) {
            log.error("Error parsing message", e);
        }
    }

    /**
     * 处理私信消息
     * <p>1. 调用私信服务持久化存储消息
     * 2. 向发送者和接收者实时推送（如果在线）</p>
     * 
     * @param userId 发送者用户ID
     * @param data   JSON消息体（含receiverId、messageType、content、imageUrl）
     */
    private void handleChatMessage(Long userId, Map<String, Object> data) {
        Long receiverId = ((Number) data.get("receiverId")).longValue();
        Integer messageType = ((Number) data.get("messageType")).intValue();
        String content = (String) data.get("content");
        String imageUrl = (String) data.get("imageUrl");

        // 1. 持久化消息到数据库
        PrivateMessage message = privateMessageService.sendMessage(
                userId, receiverId, messageType, content, imageUrl);

        // 2. 实时推送：向接收者和发送者同步消息
        if (message != null) {
            try {
                // 推送给接收者（如果在线）
                WebSocketSession receiverSession = sessions.get(receiverId);
                if (receiverSession != null && receiverSession.isOpen()) {
                    String json = objectMapper.writeValueAsString(Map.of(
                            "type", "new_message",
                            "data", message
                    ));
                    receiverSession.sendMessage(new TextMessage(json));
                }

                // 推送给发送者（确认消息已发送）
                WebSocketSession senderSession = sessions.get(userId);
                if (senderSession != null && senderSession.isOpen()) {
                    String json = objectMapper.writeValueAsString(Map.of(
                            "type", "new_message",
                            "data", message
                    ));
                    senderSession.sendMessage(new TextMessage(json));
                }
            } catch (IOException e) {
                log.error("Error sending message", e);
            }
        }
    }

    /**
     * WebSocket连接关闭回调
     * <p>从sessions映射表中移除用户记录。</p>
     * 
     * @param session WebSocket会话
     * @param status  关闭状态码
     * @throws Exception 关闭异常
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            sessions.remove(userId);
            log.info("WebSocket disconnected: userId={}", userId);
        }
    }

    /**
     * 向指定用户推送通知
     * <p>供系统其他模块调用（如通知服务通过RabbitMQ消费后推送），
     * 仅当目标用户在线时推送。</p>
     * 
     * @param userId 目标用户ID
     * @param type   通知类型（如notification/new_message）
     * @param data   通知数据
     */
    public void sendNotification(Long userId, String type, Object data) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(Map.of(
                        "type", type,
                        "data", data
                ));
                session.sendMessage(new TextMessage(json));
                log.debug("Notification sent to userId={}: type={}", userId, type);
            } catch (IOException e) {
                log.error("Error sending notification to userId={}", userId, e);
            }
        }
    }

    /**
     * 从WebSocket连接URL参数中提取JWT token
     * <p>URL格式：ws://host/ws/chat?token=xxx，提取token参数值。</p>
     * 
     * @param session WebSocket会话
     * @return JWT token字符串，未找到返回null
     */
    private String getTokenFromSession(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.contains("token=")) {
            for (String param : query.split("&")) {
                if (param.startsWith("token=")) {
                    return param.substring(6);
                }
            }
        }
        return null;
    }
}