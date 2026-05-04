package com.quxiangshe.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quxiangshe.backend.entity.Blacklist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 黑名单Mapper接口
 * 
 * @author 趣享社技术团队
 */
@Mapper
public interface BlacklistMapper extends BaseMapper<Blacklist> {
    
    /**
     * 检查是否已拉黑
     * @param userId 用户ID（拉黑者）
     * @param blockedId 被拉黑的用户ID
     * @return 存在返回true
     */
    @Select("SELECT COUNT(*) > 0 FROM blacklist WHERE user_id = #{userId} AND blocked_id = #{blockedId}")
    boolean isBlocked(@Param("userId") Long userId, @Param("blockedId") Long blockedId);
}