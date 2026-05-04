package com.quxiangshe.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.quxiangshe.backend.config.RabbitMQConfig;
import com.quxiangshe.backend.dto.NotificationMessage;
import com.quxiangshe.backend.entity.Follow;
import com.quxiangshe.backend.entity.Notification;
import com.quxiangshe.backend.entity.User;
import com.quxiangshe.backend.mapper.FollowMapper;
import com.quxiangshe.backend.mapper.UserMapper;
import com.quxiangshe.backend.service.IActivityService;
import com.quxiangshe.backend.service.IFeedService;
import com.quxiangshe.backend.service.IFollowService;
import com.quxiangshe.backend.service.INotificationService;
import com.quxiangshe.backend.vo.PageVO;
import com.quxiangshe.backend.vo.UserVO;
import static com.quxiangshe.backend.service.impl.ActivityServiceImpl.ACTION_FOLLOW;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Lazy;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 关注服务实现类
 * 
 * @author 趣享社技术团队
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements IFollowService {
    
    private final FollowMapper followMapper;
    private final UserMapper userMapper;
    private final IActivityService activityService;
    private IFeedService feedService;
    private INotificationService notificationService;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Lazy
    @Autowired
    public void setFeedService(IFeedService feedService) {
        this.feedService = feedService;
    }
    
    @Lazy
    @Autowired
    public void setNotificationService(INotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    /**
     * 关注用户
     */
    @Override
    @CacheEvict(value = {"followingCount", "followersCount"}, allEntries = true)
    public boolean follow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new RuntimeException("不能关注自己");
        }
        
        if (followMapper.checkFollowing(followerId, followingId) > 0) {
            return true;
        }
        
        Follow follow = new Follow();
        follow.setFollowerId(followerId);
        follow.setFollowingId(followingId);
        boolean result = followMapper.insert(follow) > 0;
        
        if (result) {
            // 记录用户互动活跃度（数据库）
            activityService.recordInteraction(followerId);
            // 增量更新Redis粉丝活跃度排名
            activityService.incrementActivityScore(followerId, ACTION_FOLLOW);
            // 清除Feed缓存
            feedService.evictFollowingCache(followerId);
            feedService.evictFollowerCache(followingId);
            // 发送关注通知 (异步MQ)
            if (rabbitTemplate != null) {
                try {
                    NotificationMessage msg = NotificationMessage.builder()
                            .type(NotificationMessage.TYPE_FOLLOW)
                            .userId(followingId)
                            .fromUserId(followerId)
                            .timestamp(LocalDateTime.now())
                            .build();
                    rabbitTemplate.convertAndSend(RabbitMQConfig.NOTIFICATION_EXCHANGE,
                            RabbitMQConfig.NOTIFICATION_ROUTING_KEY, msg);
                } catch (Exception e) {
                    log.error("发送关注通知消息失败", e);
                }
            }
        }
        
        return result;
    }
    
    /**
     * 取消关注
     */
    @Override
    @CacheEvict(value = {"followingCount", "followersCount"}, allEntries = true)
    public boolean unfollow(Long followerId, Long followingId) {
        QueryWrapper<Follow> wrapper = new QueryWrapper<>();
        wrapper.eq("follower_id", followerId)
               .eq("following_id", followingId);
        boolean result = followMapper.delete(wrapper) > 0;
        if (result) {
            feedService.evictFollowingCache(followerId);
            feedService.evictFollowerCache(followingId);
        }
        return result;
    }
    
    /**
     * 获取关注列表
     */
    @Override
    public PageVO<UserVO> getFollowingList(Long userId, String cursor, int size) {
        Long cursorId = cursor != null ? Long.parseLong(cursor) : null;
        return getFollowingListByCursor(userId, cursorId, size, null);
    }
    
    /**
     * 获取关注列表（带当前用户关注状态）
     */
    public PageVO<UserVO> getFollowingList(Long userId, String cursor, int size, Long currentUserId) {
        Long cursorId = cursor != null ? Long.parseLong(cursor) : null;
        return getFollowingListByCursor(userId, cursorId, size, currentUserId);
    }
    
    /**
     * 获取关注列表（游标分页）
     */
    public PageVO<UserVO> getFollowingListByCursor(Long userId, Long cursor, int size, Long currentUserId) {
        log.info("getFollowingListByCursor - userId: {}, currentUserId: {}", userId, currentUserId);
        
        List<User> users = followMapper.selectFollowingByCursor(userId, cursor, size + 1);
        
        boolean hasMore = users.size() > size;
        if (hasMore) {
            users = users.subList(0, size);
        }
        
        // 直接使用最后一个用户的ID作为下一个游标
        String nextCursor = null;
        if (hasMore && !users.isEmpty()) {
            Long lastUserId = users.get(users.size() - 1).getId();
            nextCursor = String.valueOf(lastUserId);
        }
        
        // 无论currentUserId是否为null，都设置isFollowing
        // 如果是查看自己的关注列表，所有人都是已关注
        // 如果是查看别人的关注列表，检查是否已关注
        final boolean isOwnProfile = currentUserId != null && currentUserId.equals(userId);
        
        log.info("isOwnProfile: {}", isOwnProfile);
        
        List<UserVO> records = users.stream()
                .map(user -> {
                    UserVO vo = convertToUserVO(user);
                    if (isOwnProfile) {
                        // 查看自己的关注列表，全部已关注
                        vo.setIsFollowing(true);
                    } else {
                        // 查看别人的关注列表，设置为null让前端判断（需要调用API检查）
                        // 但为了简化，这里先设置false
                        vo.setIsFollowing(false);
                    }
                    return vo;
                })
                .collect(Collectors.toList());
        
        return new PageVO<>(records, hasMore, nextCursor);
    }
    
    /**
     * 获取粉丝列表
     */
    @Override
    public PageVO<UserVO> getFollowersList(Long userId, String cursor, int size) {
        Long cursorId = cursor != null ? Long.parseLong(cursor) : null;
        return getFollowersListByCursor(userId, cursorId, size, null);
    }
    
    /**
     * 获取粉丝列表（带当前用户关注状态）
     */
    @Override
    public PageVO<UserVO> getFollowersList(Long userId, String cursor, int size, Long currentUserId) {
        Long cursorId = cursor != null ? Long.parseLong(cursor) : null;
        return getFollowersListByCursor(userId, cursorId, size, currentUserId);
    }
    
    /**
     * 获取粉丝列表（游标分页 - 内部方法）
     */
    public PageVO<UserVO> getFollowersListByCursor(Long userId, Long cursor, int size, Long currentUserId) {
        log.info("getFollowersListByCursor - userId: {}, currentUserId: {}", userId, currentUserId);
        
        List<User> users = followMapper.selectFollowersByCursor(userId, cursor, size + 1);
        
        boolean hasMore = users.size() > size;
        if (hasMore) {
            users = users.subList(0, size);
        }
        
        String nextCursor = null;
        if (hasMore && !users.isEmpty()) {
            Long lastUserId = users.get(users.size() - 1).getId();
            nextCursor = String.valueOf(lastUserId);
        }
        
        log.info("粉丝数量: {}", users.size());
        
        List<UserVO> records = users.stream()
                .map(user -> {
                    UserVO vo = convertToUserVO(user);
                    // 无论是谁的粉丝列表，都检查是否已关注
                    int count = followMapper.checkFollowing(currentUserId, user.getId());
                    vo.setIsFollowing(count > 0);
                    log.info("用户 {} 的关注状态: {}", user.getId(), count > 0);
                    return vo;
                })
                .collect(Collectors.toList());
        
        return new PageVO<>(records, hasMore, nextCursor);
    }
    
    /**
     * 获取关注数
     */
    @Override
    @Cacheable(value = "followingCount", key = "#userId")
    public long getFollowingCount(Long userId) {
        QueryWrapper<Follow> wrapper = new QueryWrapper<>();
        wrapper.eq("follower_id", userId);
        return followMapper.selectCount(wrapper);
    }
    
    /**
     * 获取粉丝数
     */
    @Override
    @Cacheable(value = "followersCount", key = "#userId")
    public long getFollowersCount(Long userId) {
        QueryWrapper<Follow> wrapper = new QueryWrapper<>();
        wrapper.eq("following_id", userId);
        return followMapper.selectCount(wrapper);
    }
    
    /**
     * 检查是否已关注
     */
    @Override
    public boolean isFollowing(Long followerId, Long followingId) {
        return followMapper.checkFollowing(followerId, followingId) > 0;
    }
    
    /**
     * 获取所有粉丝ID列表（用于Feed推送）
     */
    @Override
    public List<Long> getFollowerIds(Long userId) {
        QueryWrapper<Follow> wrapper = new QueryWrapper<>();
        wrapper.eq("following_id", userId);
        wrapper.select("follower_id");
        List<Follow> follows = followMapper.selectList(wrapper);
        return follows.stream()
                .map(Follow::getFollowerId)
                .collect(Collectors.toList());
    }
    
    /**
     * User转UserVO
     */
    private UserVO convertToUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setAvatar(user.getAvatar());
        vo.setNickname(user.getNickname());
        vo.setBio(user.getBio());
        return vo;
    }
}