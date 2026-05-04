package com.quxiangshe.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quxiangshe.backend.entity.PrivateMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface PrivateMessageMapper extends BaseMapper<PrivateMessage> {
    
    @Select("SELECT * FROM private_message WHERE session_id = #{sessionId} " +
            "AND ((sender_id = #{currentUserId} AND is_deleted_sender = 0) " +
            "OR (receiver_id = #{currentUserId} AND is_deleted_receiver = 0)) " +
            "AND (is_recalled = 0 OR (is_recalled = 1 AND sender_id != #{currentUserId})) " +
            "ORDER BY created_at DESC LIMIT #{offset}, #{size}")
    List<PrivateMessage> selectBySessionId(@Param("sessionId") Long sessionId, 
                                           @Param("currentUserId") Long currentUserId,
                                           @Param("offset") int offset, 
                                           @Param("size") int size);
    
    @Update("UPDATE private_message SET is_recalled = 1, recall_time = NOW() WHERE id = #{messageId} AND sender_id = #{senderId} AND TIMESTAMPDIFF(SECOND, created_at, NOW()) <= 120")
    int recallMessage(@Param("messageId") Long messageId, @Param("senderId") Long senderId);
    
    @Update("UPDATE private_message SET is_deleted_sender = 1 WHERE id = #{messageId} AND sender_id = #{userId}")
    int deleteAsSender(@Param("messageId") Long messageId, @Param("userId") Long userId);
    
    @Update("UPDATE private_message SET is_deleted_receiver = 1 WHERE id = #{messageId} AND receiver_id = #{userId}")
    int deleteAsReceiver(@Param("messageId") Long messageId, @Param("userId") Long userId);
    
    @Select("SELECT * FROM private_message WHERE session_id = #{sessionId} AND created_at < DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    List<PrivateMessage> selectOldMessages(@Param("sessionId") Long sessionId, @Param("days") int days);
}