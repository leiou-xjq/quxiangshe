package com.quxiangshe.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quxiangshe.backend.entity.UserActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 用户活跃度Mapper接口
 * 
 * @author 趣享社技术团队
 */
@Mapper
public interface UserActivityMapper extends BaseMapper<UserActivity> {
    
    /**
     * 根据用户ID查询
     */
    UserActivity selectByUserId(@Param("userId") Long userId);
    
    /**
     * 批量查询用户活跃度
     */
    List<UserActivity> selectByUserIds(@Param("userIds") List<Long> userIds);
    
    /**
     * 重置今日互动次数
     */
    int resetTodayInteractionCount();
    
    /**
     * 更新登录天数
     */
    int updateLoginDays(@Param("userId") Long userId, @Param("loginDays") Integer loginDays);
    
    /**
     * 更新互动次数
     */
    int updateInteractionCount(@Param("userId") Long userId, @Param("interactionCount") Integer interactionCount);
    
    /**
     * 批量插入用户活跃度
     */
    int batchInsert(@Param("list") List<UserActivity> activities);
    
    /**
     * 批量更新活跃度分数（衰减用）
     */
    int batchUpdateScore(@Param("list") List<UserActivity> activities);
    
    /**
     * 查询今日有互动的用户
     */
    List<UserActivity> selectTodayActiveUsers(@Param("date") LocalDate date);
}