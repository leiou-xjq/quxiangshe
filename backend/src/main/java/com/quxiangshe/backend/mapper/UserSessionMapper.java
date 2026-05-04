package com.quxiangshe.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quxiangshe.backend.entity.UserSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户会话Mapper接口
 * 
 * @author 趣享社技术团队
 */
@Mapper
public interface UserSessionMapper extends BaseMapper<UserSession> {
    
    /**
     * 根据Token查询会话
     * @param token Token值
     * @return 会话信息
     */
    UserSession selectByToken(@Param("token") String token);
    
    /**
     * 根据用户ID查询有效会话
     * @param userId 用户ID
     * @return 会话列表
     */
    List<UserSession> selectValidSessionsByUserId(@Param("userId") Long userId);
    
    /**
     * 失效用户所有会话
     * @param userId 用户ID
     */
    void invalidateUserSessions(@Param("userId") Long userId);
    
    /**
     * 清理过期会话
     */
    void cleanExpiredSessions(@Param("now") LocalDateTime now);
}