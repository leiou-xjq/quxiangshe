package com.quxiangshe.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quxiangshe.auth.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户Mapper接口（继承auth模块的UserMapper）
 * 这里可以添加用户模块特定的查询方法
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
    // 继承自auth模块的基础Mapper
    // 可在此添加用户模块特定的扩展方法
}
