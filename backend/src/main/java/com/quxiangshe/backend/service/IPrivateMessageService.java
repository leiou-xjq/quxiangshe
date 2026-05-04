package com.quxiangshe.backend.service;

import com.quxiangshe.backend.entity.PrivateMessage;
import com.quxiangshe.backend.entity.PrivateMessageSession;

import java.util.List;

public interface IPrivateMessageService {
    
    // 会话相关
    List<PrivateMessageSession> getSessionList(Long userId, int size, int offset);

    PrivateMessageSession getSessionInfo(Long sessionId, Long userId);

    PrivateMessageSession getOrCreateSession(Long userId, Long targetUserId);
    
    Integer getUnreadCount(Long userId);

    void markSessionRead(Long sessionId, Long userId);

    // 消息相关
    List<PrivateMessage> getMessageList(Long sessionId, Long currentUserId, int size, int offset);
    
    PrivateMessage sendMessage(Long senderId, Long receiverId, Integer messageType, String content, String imageUrl);
    
    boolean recallMessage(Long messageId, Long senderId);
    
    boolean deleteMessage(Long messageId, Long userId);
    
    // 归档
    void archiveOldMessages(int days);
}