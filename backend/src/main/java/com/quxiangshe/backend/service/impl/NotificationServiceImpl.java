package com.quxiangshe.backend.service.impl;

import com.quxiangshe.backend.entity.Note;
import com.quxiangshe.backend.entity.Notification;
import com.quxiangshe.backend.entity.User;
import com.quxiangshe.backend.mapper.NoteMapper;
import com.quxiangshe.backend.mapper.NotificationMapper;
import com.quxiangshe.backend.mapper.UserMapper;
import com.quxiangshe.backend.service.INotificationService;
import com.quxiangshe.backend.service.IPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知服务实现类
 * 
 * @author 趣享社技术团队
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements INotificationService {
    
    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;
    private final NoteMapper noteMapper;
    @Lazy
    @Autowired
    private IPushService pushService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createNotification(Notification notification) {
        notification.setIsRead(0);
        notification.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notification);

        if (pushService != null) {
            String title = getNotificationTitle(notification.getType());
            pushService.pushNotification(
                notification.getUserId(),
                title,
                notification.getContent(),
                String.valueOf(notification.getType()),
                notification.getNoteId()
            );
        }
    }

    private String getNotificationTitle(int type) {
        switch (type) {
            case Notification.TYPE_LIKE:
                return "收到赞";
            case Notification.TYPE_COMMENT:
                return "收到评论";
            case Notification.TYPE_FOLLOW:
                return "新粉丝";
            case Notification.TYPE_REVIEW_PASSED:
                return "审核通过";
            case Notification.TYPE_REVIEW_REJECTED:
                return "审核未通过";
            default:
                return "新通知";
        }
    }
    
    @Override
    public List<Notification> getNotifications(Long userId, int limit, int offset) {
        return notificationMapper.selectUserNotifications(userId, limit, offset);
    }
    
    @Override
    public Integer getUnreadCount(Long userId) {
        return notificationMapper.selectUnreadCount(userId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = new Notification();
        notification.setId(notificationId);
        notification.setIsRead(1);
        notificationMapper.updateById(notification);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead(Long userId) {
        List<Notification> notifications = notificationMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
        );
        
        for (Notification notification : notifications) {
            notification.setIsRead(1);
            notificationMapper.updateById(notification);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotification(Long notificationId, Long userId) {
        notificationMapper.deleteById(notificationId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendLikeNotification(Long noteId, Long userId, Long fromUserId) {
        if (userId.equals(fromUserId)) return;
        
        User fromUser = userMapper.selectById(fromUserId);
        
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(Notification.TYPE_LIKE);
        notification.setFromUserId(fromUserId);
        notification.setNoteId(noteId);
        notification.setContent(fromUser != null ? fromUser.getNickname() + "赞了你的笔记" : "有人赞了你的笔记");
        
        createNotification(notification);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendCommentNotification(Long noteId, Long commentId, Long userId, Long fromUserId) {
        if (userId.equals(fromUserId)) return;
        
        User fromUser = userMapper.selectById(fromUserId);
        
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(Notification.TYPE_COMMENT);
        notification.setFromUserId(fromUserId);
        notification.setNoteId(noteId);
        notification.setCommentId(commentId);
        notification.setContent(fromUser != null ? fromUser.getNickname() + "评论了你的笔记" : "有人评论了你的笔记");
        
        createNotification(notification);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendFollowNotification(Long userId, Long fromUserId) {
        if (userId.equals(fromUserId)) return;
        
        User fromUser = userMapper.selectById(fromUserId);
        
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(Notification.TYPE_FOLLOW);
        notification.setFromUserId(fromUserId);
        notification.setContent(fromUser != null ? fromUser.getNickname() + "关注了你" : "有人关注了你");
        
        createNotification(notification);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendReviewPassedNotification(Long noteId, Long userId) {
        Note note = noteMapper.selectById(noteId);
        
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(Notification.TYPE_REVIEW_PASSED);
        notification.setNoteId(noteId);
        notification.setContent(note != null ? "您的笔记《" + note.getTitle() + "》已通过审核" : "您的笔记已通过审核");
        
        createNotification(notification);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendReviewRejectedNotification(Long noteId, Long userId, String reason) {
        Note note = noteMapper.selectById(noteId);
        
        String title = note != null ? note.getTitle() : "笔记";
        String content = "您的笔记《" + title + "》未通过审核，原因：" + reason;
        
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(Notification.TYPE_REVIEW_REJECTED);
        notification.setNoteId(noteId);
        notification.setContent(content);
        
        createNotification(notification);
    }
}
