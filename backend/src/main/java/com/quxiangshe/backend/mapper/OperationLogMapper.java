package com.quxiangshe.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quxiangshe.backend.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志数据访问层接口，管理系统操作日志（OperationLog）的数据库操作。
 * <p>
 * 继承MyBatis-Plus BaseMapper，提供基础CRUD功能。
 * 操作日志用于记录管理员和用户的敏感操作（如审核、删除、封禁等），
 * 支持审计追溯和合规要求。
 * </p>
 * 
 * @author 趣享社技术团队
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}