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

    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public WebSocketSession getSession(Long userId) {
        return sessions.get(userId);
    }

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

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) {
            return;
        }

        String payload = message.getPayload();
        log.info("Received message from user {}: {}", userId, payload);

        try {
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            String type = (String) data.get("type");

            if ("message".equals(type)) {
                handleChatMessage(userId, data);
            }
        } catch (IOException e) {
            log.error("Error parsing message", e);
        }
    }

    private void handleChatMessage(Long userId, Map<String, Object> data) {
        Long receiverId = ((Number) data.get("receiverId")).longValue();
        Integer messageType = ((Number) data.get("messageType")).intValue();
        String content = (String) data.get("content");
        String imageUrl = (String) data.get("imageUrl");

        PrivateMessage message = privateMessageService.sendMessage(
                userId, receiverId, messageType, content, imageUrl);

        if (message != null) {
            try {
                WebSocketSession receiverSession = sessions.get(receiverId);
                if (receiverSession != null && receiverSession.isOpen()) {
                    String json = objectMapper.writeValueAsString(Map.of(
                            "type", "new_message",
                            "data", message
                    ));
                    receiverSession.sendMessage(new TextMessage(json));
                }

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

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            sessions.remove(userId);
            log.info("WebSocket disconnected: userId={}", userId);
        }
    }

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