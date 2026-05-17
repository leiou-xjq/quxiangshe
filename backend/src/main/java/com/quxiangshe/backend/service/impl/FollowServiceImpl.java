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
 * <p>核心职责：
 * <ul>
 *   <li>管理用户之间的关注 / 取关关系</li>
 *   <li>提供基于游标分页的关注列表与粉丝列表查询</li>
 *   <li>关注成功后触发活跃度记录、Feed 缓存清除、异步通知发送</li>
 * </ul>
 *
 * <p>所属业务模块：社交关系管理
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
     *
     * <p>关注成功后依次执行：
     * <ol>
     *   <li>记录互动活跃度（DB）</li>
     *   <li>增量更新 Redis 粉丝活跃度排名</li>
     *   <li>清除相关 Feed 缓存</li>
     *   <li>通过 RabbitMQ 异步发送关注通知</li>
     * </ol>
     *
     * @param followerId  关注者用户ID
     * @param followingId 被关注者用户ID
     * @return true 表示关注成功（或已关注）
     * @throws RuntimeException 当关注者和被关注者为同一人时
     */
    @Override
    @CacheEvict(value = {"followingCount", "followersCount"}, allEntries = true)
    public boolean follow(Long followerId, Long followingId) {
        // 不允许关注自己
        if (followerId.equals(followingId)) {
            throw new RuntimeException("不能关注自己");
        }
        
        // 如果已经关注，直接返回成功（幂等）
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
            // 发送关注通知 (通过RabbitMQ异步投递，解耦通知发送与关注主流程)
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
     *
     * <p>取关成功后清除 Feed 缓存。注意到取关时不发送通知也不扣减活跃度。
     *
     * @param followerId  取关者用户ID
     * @param followingId 被取关者用户ID
     * @return true 表示取关成功
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
     * 获取关注列表（游标分页）
     *
     * @param userId   目标用户ID
     * @param cursor   游标（上一页最后一条记录的ID），首次请求传 null
     * @param size     每页大小
     * @return 分页结果，包含用户列表、是否有下一页、下一页游标
     */
    @Override
    public PageVO<UserVO> getFollowingList(Long userId, String cursor, int size) {
        Long cursorId = cursor != null ? Long.parseLong(cursor) : null;
        return getFollowingListByCursor(userId, cursorId, size, null);
    }
    
    /**
     * 获取关注列表（带当前用户关注状态）
     *
     * @param userId        目标用户ID
     * @param cursor        游标
     * @param size          每页大小
     * @param currentUserId 当前登录用户ID，用于判断是否互关
     * @return 分页结果
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
        // 判断查看的是否是本人的关注列表
        final boolean isOwnProfile = currentUserId != null && currentUserId.equals(userId);
        
        log.info("isOwnProfile: {}", isOwnProfile);
        
        List<UserVO> records = users.stream()
                .map(user -> {
                    UserVO vo = convertToUserVO(user);
                    if (isOwnProfile) {
                        // 查看自己的关注列表，标记全部为"已关注"
                        vo.setIsFollowing(true);
                    } else {
                        // 查看别人的关注列表，暂设为 false，由前端进一步校验
                        vo.setIsFollowing(false);
                    }
                    return vo;
                })
                .collect(Collectors.toList());
        
        return new PageVO<>(records, hasMore, nextCursor);
    }
    
    /**
     * 获取粉丝列表（游标分页）
     *
     * @param userId 目标用户ID
     * @param cursor 游标
     * @param size   每页大小
     * @return 分页结果
     */
    @Override
    public PageVO<UserVO> getFollowersList(Long userId, String cursor, int size) {
        Long cursorId = cursor != null ? Long.parseLong(cursor) : null;
        return getFollowersListByCursor(userId, cursorId, size, null);
    }
    
    /**
     * 获取粉丝列表（带当前用户关注状态）
     *
     * @param userId        目标用户ID
     * @param cursor        游标
     * @param size          每页大小
     * @param currentUserId 当前登录用户ID
     * @return 分页结果
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
     *
     * <p>结果由 Spring Cache 缓存（cacheName = "followingCount"），
     * 关注 / 取关操作时通过 {@code @CacheEvict} 清除缓存。
     *
     * @param userId 用户ID
     * @return 该用户关注的人数
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
     *
     * <p>结果由 Spring Cache 缓存（cacheName = "followersCount"）。
     *
     * @param userId 用户ID
     * @return 该用户的粉丝人数
     */
    @Override
    @Cacheable(value = "followersCount", key = "#userId")
    public long getFollowersCount(Long userId) {
        QueryWrapper<Follow> wrapper = new QueryWrapper<>();
        wrapper.eq("following_id", userId);
        return followMapper.selectCount(wrapper);
    }
    
    /**
     * 检查用户是否已关注另一用户
     *
     * @param followerId  关注者ID
     * @param followingId 被关注者ID
     * @return true 表示已关注
     */
    @Override
    public boolean isFollowing(Long followerId, Long followingId) {
        return followMapper.checkFollowing(followerId, followingId) > 0;
    }
    
    /**
     * 获取用户的所有粉丝ID列表
     *
     * <p>用于 Feed 推送场景，批量获取博主的粉丝以便推送内容。
     *
     * @param userId 博主ID
     * @return 粉丝ID列表
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