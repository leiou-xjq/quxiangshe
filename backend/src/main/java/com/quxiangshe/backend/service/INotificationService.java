package com.quxiangshe.backend.service;

import com.quxiangshe.backend.entity.Notification;

import java.util.List;

/**
 * 通知服务接口
 * 
 * @author 趣享社技术团队
 */
public interface INotificationService {
    
    /**
     * 创建通知
     */
    void createNotification(Notification notification);
    
    /**
     * 获取通知列表
     */
    List<Notification> getNotifications(Long userId, int limit, int offset);
    
    /**
     * 获取未读数量
     */
    Integer getUnreadCount(Long userId);
    
    /**
     * 标记已读
     */
    void markAsRead(Long userId, Long notificationId);
    
    /**
     * 全部标记已读
     */
    void markAllAsRead(Long userId);
    
    /**
     * 删除通知
     */
    void deleteNotification(Long notificationId, Long userId);
    
    /**
     * 发送点赞通知
     */
    void sendLikeNotification(Long noteId, Long userId, Long fromUserId);
    
    /**
     * 发送评论通知
     */
    void sendCommentNotification(Long noteId, Long commentId, Long userId, Long fromUserId);
    
    /**
     * 发送关注通知
     */
    void sendFollowNotification(Long userId, Long fromUserId);
    
    /**
     * 发送审核通过通知
     */
    void sendReviewPassedNotification(Long noteId, Long userId);
    
    /**
     * 发送审核未通过通知
     */
    void sendReviewRejectedNotification(Long noteId, Long userId, String reason);
}
