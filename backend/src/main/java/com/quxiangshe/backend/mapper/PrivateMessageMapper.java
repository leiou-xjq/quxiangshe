package com.quxiangshe.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quxiangshe.backend.entity.PrivateMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 私信消息数据访问层接口，管理私信消息（PrivateMessage）的数据库操作。
 * <p>
 * 提供按会话分页查询消息（排除双方已删除及非己方撤回的消息）、
 * 消息撤回（2分钟内有效）、发送者/接收者单边删除、
 * 以及查询过期消息等功能。
 * </p>
 * 
 * @author 趣享社技术团队
 */
@Mapper
public interface PrivateMessageMapper extends BaseMapper<PrivateMessage> {
    
    /**
     * 按会话分页查询私信消息。
     * 排除发送者已删除、接收者已删除的消息；
     * 已撤回的消息仅对非发送者隐藏（发送者可见撤回提示）。
     * 
     * @param sessionId 会话ID
     * @param currentUserId 当前用户ID
     * @param offset 分页偏移量
     * @param size 每页数量
     * @return 消息列表，按创建时间倒序
     */
    @Select("SELECT * FROM private_message WHERE session_id = #{sessionId} " +
            "AND ((sender_id = #{currentUserId} AND is_deleted_sender = 0) " +
            "OR (receiver_id = #{currentUserId} AND is_deleted_receiver = 0)) " +
            "AND (is_recalled = 0 OR (is_recalled = 1 AND sender_id != #{currentUserId})) " +
            "ORDER BY created_at DESC LIMIT #{offset}, #{size}")
    List<PrivateMessage> selectBySessionId(@Param("sessionId") Long sessionId, 
                                           @Param("currentUserId") Long currentUserId,
                                           @Param("offset") int offset, 
                                           @Param("size") int size);
    
    /**
     * 撤回消息（发送后2分钟内有效）。
     * @param messageId 消息ID
     * @param senderId 发送者ID（仅发送者可撤回）
     * @return 影响的行数（1表示撤回成功，0表示超时或无权）
     */
    @Update("UPDATE private_message SET is_recalled = 1, recall_time = NOW() WHERE id = #{messageId} AND sender_id = #{senderId} AND TIMESTAMPDIFF(SECOND, created_at, NOW()) <= 120")
    int recallMessage(@Param("messageId") Long messageId, @Param("senderId") Long senderId);
    
    /**
     * 发送者单边删除消息（仅标记为已删除，接收者仍可见）。
     * @param messageId 消息ID
     * @param userId 发送者用户ID
     * @return 影响的行数
     */
    @Update("UPDATE private_message SET is_deleted_sender = 1 WHERE id = #{messageId} AND sender_id = #{userId}")
    int deleteAsSender(@Param("messageId") Long messageId, @Param("userId") Long userId);
    
    /**
     * 接收者单边删除消息（仅标记为已删除，发送者仍可见）。
     * @param messageId 消息ID
     * @param userId 接收者用户ID
     * @return 影响的行数
     */
    @Update("UPDATE private_message SET is_deleted_receiver = 1 WHERE id = #{messageId} AND receiver_id = #{userId}")
    int deleteAsReceiver(@Param("messageId") Long messageId, @Param("userId") Long userId);
    
    /**
     * 查询指定会话中超过指定天数的历史消息，用于定期清理归档。
     * @param sessionId 会话ID
     * @param days 保留天数（超过此天数的消息将被查询出来）
     * @return 过期消息列表
     */
    @Select("SELECT * FROM private_message WHERE session_id = #{sessionId} AND created_at < DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    List<PrivateMessage> selectOldMessages(@Param("sessionId") Long sessionId, @Param("days") int days);
}