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
 * <p>核心职责：
 * <ul>
 *   <li>创建并持久化各类系统通知（点赞、评论、关注、审核结果等）</li>
 *   <li>通知创建后通过 WebSocket 推送实时消息到客户端</li>
 *   <li>提供通知列表查询、未读数统计、标记已读、删除等管理功能</li>
 * </ul>
 *
 * <p>所属业务模块：消息通知管理
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

    /**
     * 创建通知记录并推送到客户端
     *
     * <p>事务操作：先持久化通知记录，再通过 WebSocket 推送。
     * 推送成功与否不影响事务提交（推送在事务内但 catch 了异常）。
     *
     * @param notification 通知实体
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createNotification(Notification notification) {
        notification.setIsRead(0);
        notification.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notification);

        // 如果推送服务可用，通过WebSocket实时推送通知给用户
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

    /**
     * 根据通知类型获取对应的通知标题文案
     *
     * @param type 通知类型（{@link Notification} 中的 TYPE_* 常量）
     * @return 标题文案
     */
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
    
    /**
     * 分页查询用户通知列表
     *
     * @param userId 用户ID
     * @param limit  每页条数
     * @param offset 偏移量
     * @return 通知列表
     */
    @Override
    public List<Notification> getNotifications(Long userId, int limit, int offset) {
        return notificationMapper.selectUserNotifications(userId, limit, offset);
    }
    
    /**
     * 获取用户未读通知数
     *
     * @param userId 用户ID
     * @return 未读通知数量，无通知时返回 0
     */
    @Override
    public Integer getUnreadCount(Long userId) {
        return notificationMapper.selectUnreadCount(userId);
    }
    
    /**
     * 将指定通知标记为已读
     *
     * @param userId         用户ID（预留扩展，当前未做权限校验）
     * @param notificationId 通知ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = new Notification();
        notification.setId(notificationId);
        notification.setIsRead(1);
        notificationMapper.updateById(notification);
    }
    
    /**
     * 将用户的所有未读通知标记为已读
     *
     * <p>查询该用户所有未读通知，逐条更新状态。
     * 注意：若未读通知量很大，逐条更新存在性能瓶颈，可后续优化为批量更新。
     *
     * @param userId 用户ID
     */
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
    
    /**
     * 删除指定通知
     *
     * @param notificationId 通知ID
     * @param userId         用户ID（预留扩展）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotification(Long notificationId, Long userId) {
        notificationMapper.deleteById(notificationId);
    }
    
    /**
     * 发送点赞通知
     *
     * <p>自己给自己点赞不发送通知。通知内容格式为"{昵称}赞了你的笔记"。
     *
     * @param noteId     被点赞的笔记ID
     * @param userId     笔记作者ID（被通知用户）
     * @param fromUserId 点赞者ID
     */
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
    
    /**
     * 发送评论通知
     *
     * <p>自己评论自己笔记不发送通知。
     *
     * @param noteId     评论所属的笔记ID
     * @param commentId  评论ID
     * @param userId     笔记作者ID（被通知用户）
     * @param fromUserId 评论者ID
     */
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
    
    /**
     * 发送关注通知
     *
     * @param userId     被关注者ID（被通知用户）
     * @param fromUserId 关注者ID
     */
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

    /**
     * 发送审核通过通知
     *
     * @param noteId 审核通过的笔记ID
     * @param userId 笔记作者ID
     */
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

    /**
     * 发送审核驳回通知
     *
     * @param noteId 审核驳回的笔记ID
     * @param userId 笔记作者ID
     * @param reason 驳回原因
     */
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
