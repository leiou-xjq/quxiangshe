package com.quxiangshe.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quxiangshe.backend.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 通知Mapper接口
 * 
 * @author 趣享社技术团队
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
    
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
    
    @Select("SELECT COUNT(*) FROM notification WHERE user_id = #{userId} AND is_read = 0")
    Integer selectUnreadCount(@Param("userId") Long userId);
}
