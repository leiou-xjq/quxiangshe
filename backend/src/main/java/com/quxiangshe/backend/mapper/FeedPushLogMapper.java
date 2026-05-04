package com.quxiangshe.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quxiangshe.backend.entity.FeedPushLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 推送日志Mapper接口
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