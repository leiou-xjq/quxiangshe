package com.quxiangshe.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quxiangshe.backend.entity.FeedPushLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Feed推送日志数据访问层接口，管理Feed推送投递日志（FeedPushLog）的数据库操作。
 * <p>
 * 记录每次Feed推送的投递状态（作者→目标用户），支持推模式/拉模式/推拉结合三种推送模式。
 * 提供批量插入推送日志功能，用于实时追踪推送投递情况。
 * </p>
 * 
 * @author 趣享社技术团队
 */
@Mapper
public interface FeedPushLogMapper extends BaseMapper<FeedPushLog> {
    
    /**
     * 批量插入推送日志
     */
    int batchInsert(@Param("logs") List<FeedPushLog> logs);
}