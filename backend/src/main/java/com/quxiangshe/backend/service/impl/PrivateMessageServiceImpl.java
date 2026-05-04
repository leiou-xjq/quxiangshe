package com.quxiangshe.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quxiangshe.backend.entity.PrivateMessage;
import com.quxiangshe.backend.entity.PrivateMessageArchive;
import com.quxiangshe.backend.entity.PrivateMessageSession;
import com.quxiangshe.backend.mapper.PrivateMessageArchiveMapper;
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
    private final PrivateMessageArchiveMapper archiveMapper;
    
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
    public PrivateMessageSession getSessionInfo(Long sessionId, Long userId) {
        PrivateMessageSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            return null;
        }
        // 确保是当前用户的会话
        if (!session.getUserId().equals(userId)) {
            return null;
        }
        return session;
    }

    @Override
    public Integer getUnreadCount(Long userId) {
        Integer count = sessionMapper.getTotalUnreadCount(userId);
        return count != null ? count : 0;
    }

    @Override
    public void markSessionRead(Long sessionId, Long userId) {
        PrivateMessageSession session = sessionMapper.selectById(sessionId);
        if (session != null && session.getUserId().equals(userId)) {
            session.setUnreadCount(0);
            sessionMapper.updateById(session);
        }
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
        
        // 更新发送方会话
        updateSessionAfterMessage(session.getId(), senderId, content, message.getId());
        
        // 更新接收方会话
        PrivateMessageSession receiverSession = sessionMapper.selectByUserAndTarget(receiverId, senderId);
        if (receiverSession != null) {
            updateSessionAfterMessage(receiverSession.getId(), receiverId, content, message.getId());
        }
        
        return message;
    }
    
    private void updateSessionAfterMessage(Long sessionId, Long userId, String content, Long messageId) {
        PrivateMessageSession session = sessionMapper.selectById(sessionId);
        if (session != null) {
            session.setLastMessageId(messageId);
            String summary = content != null && content.length() > 50 ? content.substring(0, 50) + "..." : content;
            session.setLastMessageContent(summary);
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
        List<PrivateMessageSession> sessions = sessionMapper.selectList(new LambdaQueryWrapper<>());
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
                
                archiveMapper.insert(archive);
                messageMapper.deleteById(msg.getId());
            }
        }
    }
}