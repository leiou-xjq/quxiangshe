package com.quxiangshe.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quxiangshe.backend.entity.PrivateMessageSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PrivateMessageSessionMapper extends BaseMapper<PrivateMessageSession> {
    
    @Select("SELECT * FROM private_message_session WHERE user_id = #{userId} ORDER BY last_message_time DESC LIMIT #{offset}, #{size}")
    List<PrivateMessageSession> selectByUserId(@Param("userId") Long userId, @Param("offset") int offset, @Param("size") int size);
    
    @Select("SELECT * FROM private_message_session WHERE user_id = #{userId} AND target_user_id = #{targetUserId}")
    PrivateMessageSession selectByUserAndTarget(@Param("userId") Long userId, @Param("targetUserId") Long targetUserId);
    
    @Select("SELECT SUM(unread_count) FROM private_message_session WHERE user_id = #{userId}")
    Integer getTotalUnreadCount(@Param("userId") Long userId);
}