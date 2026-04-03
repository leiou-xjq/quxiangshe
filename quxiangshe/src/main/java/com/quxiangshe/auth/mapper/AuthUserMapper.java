package com.quxiangshe.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quxiangshe.auth.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户Mapper接口
 */
@Mapper
public interface AuthUserMapper extends BaseMapper<UserEntity> {

    /**
     * 根据用户名查询用户
     */
    @Select("SELECT * FROM user WHERE username = #{username}")
    UserEntity selectByUsername(@Param("username") String username);

    /**
     * 根据手机号查询用户
     */
    @Select("SELECT * FROM user WHERE phone = #{phone}")
    UserEntity selectByPhone(@Param("phone") String phone);

    /**
     * 根据邮箱查询用户
     */
    @Select("SELECT * FROM user WHERE email = #{email}")
    UserEntity selectByEmail(@Param("email") String email);

    /**
     * 统计用户名数量（用于唯一性校验）
     */
    @Select("SELECT COUNT(*) FROM user WHERE username = #{username}")
    Long countByUsername(@Param("username") String username);

    /**
     * 统计手机号数量
     */
    @Select("SELECT COUNT(*) FROM user WHERE phone = #{phone}")
    Long countByPhone(@Param("phone") String phone);

    /**
     * 统计邮箱数量
     */
    @Select("SELECT COUNT(*) FROM user WHERE email = #{email}")
    Long countByEmail(@Param("email") String email);

    /**
     * 更新最后登录时间
     */
    @Update("UPDATE user SET last_login_time = NOW() WHERE id = #{userId}")
    int updateLastLoginTime(@Param("userId") Long userId);
}
