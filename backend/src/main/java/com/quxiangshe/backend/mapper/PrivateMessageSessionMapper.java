package com.quxiangshe.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quxiangshe.backend.entity.PrivateMessageSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 私信会话数据访问层接口，管理私信会话列表（PrivateMessageSession）的数据库操作。
 * <p>
 * 提供按用户分页查询会话列表、按双方用户查询特定会话、
 * 以及获取用户总未读消息数等功能。
 * 会话按最后消息时间倒序排列。
 * </p>
 * 
 * @author 趣享社技术团队
 */
@Mapper
public interface PrivateMessageSessionMapper extends BaseMapper<PrivateMessageSession> {
    
    /**
     * 分页查询用户的私信会话列表，按最后消息时间倒序。
     * @param userId 用户ID
     * @param offset 分页偏移量
     * @param size 每页数量
     * @return 会话列表
     */
    @Select("SELECT * FROM private_message_session WHERE user_id = #{userId} ORDER BY last_message_time DESC LIMIT #{offset}, #{size}")
    List<PrivateMessageSession> selectByUserId(@Param("userId") Long userId, @Param("offset") int offset, @Param("size") int size);
    
    /**
     * 根据双方用户ID查询特定会话，用于定位已有会话或创建新会话。
     * @param userId 当前用户ID
     * @param targetUserId 对方用户ID
     * @return 会话记录，不存在则返回null
     */
    @Select("SELECT * FROM private_message_session WHERE user_id = #{userId} AND target_user_id = #{targetUserId}")
    PrivateMessageSession selectByUserAndTarget(@Param("userId") Long userId, @Param("targetUserId") Long targetUserId);
    
    /**
     * 获取用户所有会话的总未读消息数。
     * @param userId 用户ID
     * @return 总未读数
     */
    @Select("SELECT SUM(unread_count) FROM private_message_session WHERE user_id = #{userId}")
    Integer getTotalUnreadCount(@Param("userId") Long userId);
}