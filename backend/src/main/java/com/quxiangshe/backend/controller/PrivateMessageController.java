package com.quxiangshe.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quxiangshe.backend.common.R;
import com.quxiangshe.backend.config.ChatWebSocketHandler;
import com.quxiangshe.backend.entity.PrivateMessage;
import com.quxiangshe.backend.entity.PrivateMessageSession;
import com.quxiangshe.backend.service.IPrivateMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Map;

@Tag(name = "私信管理", description = "私信相关接口")
@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class PrivateMessageController {

    private final IPrivateMessageService privateMessageService;
    private final ChatWebSocketHandler chatWebSocketHandler;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
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

    @Operation(summary = "获取会话信息")
    @GetMapping("/sessions/{sessionId}/info")
    public R<PrivateMessageSession> getSessionInfo(
            @PathVariable Long sessionId,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        PrivateMessageSession session = privateMessageService.getSessionInfo(sessionId, userId);
        return R.ok(session);
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
        String imageUrl = params.get("imageUrl") != null ? (String) params.get("imageUrl") : null;

        PrivateMessage message = privateMessageService.sendMessage(userId, receiverId, messageType, content, imageUrl);

        pushMessageToClients(userId, receiverId, message);

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

    @Operation(summary = "标记会话已读")
    @PutMapping("/sessions/{sessionId}/read")
    public R<String> markSessionRead(@PathVariable Long sessionId, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        privateMessageService.markSessionRead(sessionId, userId);
        return R.ok("已标记为已读", null);
    }

    private void pushMessageToClients(Long senderId, Long receiverId, PrivateMessage message) {
        try {
            String json = objectMapper.writeValueAsString(Map.of(
                "type", "new_message",
                "data", message
            ));
            TextMessage textMessage = new TextMessage(json);

            WebSocketSession receiverSession = chatWebSocketHandler.getSession(receiverId);
            if (receiverSession != null && receiverSession.isOpen()) {
                receiverSession.sendMessage(textMessage);
            }

            WebSocketSession senderSession = chatWebSocketHandler.getSession(senderId);
            if (senderSession != null && senderSession.isOpen()) {
                senderSession.sendMessage(textMessage);
            }
        } catch (Exception e) {
            // WebSocket push failure should not fail the API response
        }
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        String userIdStr = request.getAttribute("userId") != null ? 
            request.getAttribute("userId").toString() : null;
        return userIdStr != null ? Long.parseLong(userIdStr) : null;
    }
}