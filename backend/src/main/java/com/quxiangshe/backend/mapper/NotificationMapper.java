package com.quxiangshe.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quxiangshe.backend.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 通知数据访问层接口，管理系统通知（Notification）的数据库操作。
 * <p>
 * 提供用户通知列表查询（关联发送者信息）、未读通知数量统计等功能。
 * 通知类型包括点赞、评论、关注、系统消息及审核结果通知。
 * </p>
 * 
 * @author 趣享社技术团队
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
    
    /**
     * 分页查询用户通知列表，关联发送者信息（用户名、昵称、头像）。
     * @param userId 接收通知的用户ID
     * @param limit 每页数量
     * @param offset 偏移量
     * @return 通知列表（含发送者信息），按创建时间倒序
     */
    @Select("""
        SELECT n.*, u.username as from_username, u.nickname as from_nickname, u.avatar as from_avatar
        FROM notification n
        LEFT JOIN user u ON n.from_user_id = u.id
        WHERE n.user_id = #{userId}
        ORDER BY n.created_at DESC
        LIMIT #{limit} OFFSET #{offset}
        """)
    List<Notification> selectUserNotifications(@Param("userId") Long userId, 
                                                 @Param("limit") int limit, 
                                                 @Param("offset") int offset);
    
    /**
     * 查询用户未读通知数量。
     * @param userId 用户ID
     * @return 未读通知总数
     */
    @Select("SELECT COUNT(*) FROM notification WHERE user_id = #{userId} AND is_read = 0")
    Integer selectUnreadCount(@Param("userId") Long userId);
}
