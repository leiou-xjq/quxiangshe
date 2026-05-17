package com.quxiangshe.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quxiangshe.backend.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户数据访问层接口，管理用户（User）的数据库操作。
 * <p>
 * 提供用户按用户名/手机号/邮箱/微信OpenID查询、唯一性校验、
 * 关键词搜索（含随机展示）、批量查询及批量导入等功能。
 * </p>
 * 
 * @author 趣享社技术团队
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户信息
     */
    User selectByUsername(@Param("username") String username);
    
    /**
     * 根据手机号查询用户
     * @param phone 手机号
     * @return 用户信息
     */
    User selectByPhone(@Param("phone") String phone);
    
    /**
     * 根据邮箱查询用户
     * @param email 邮箱
     * @return 用户信息
     */
    User selectByEmail(@Param("email") String email);
    
    /**
     * 检查用户名是否存在
     * @param username 用户名
     * @return 存在返回true
     */
    boolean isUsernameExists(@Param("username") String username);
    
    /**
     * 检查手机号是否存在
     * @param phone 手机号
     * @return 存在返回true
     */
    boolean isPhoneExists(@Param("phone") String phone);
    
    /**
     * 检查邮箱是否存在
     * @param email 邮箱
     * @return 存在返回true
     */
    boolean isEmailExists(@Param("email") String email);
    
    /**
     * 根据微信OpenID查询用户
     * @param openId 微信OpenID
     * @return 用户信息
     */
    User selectByWechatOpenId(@Param("openId") String openId);

    /**
     * 搜索用户 - 支持关键词搜索和随机展示
     * keyword为空时随机展示用户
     */
    @Select("""
        <script>
        SELECT * FROM user 
        WHERE status = 1 
        <if test="keyword != null and keyword != ''">
        AND (username LIKE CONCAT('%', #{keyword}, '%') 
             OR nickname LIKE CONCAT('%', #{keyword}, '%') 
             OR bio LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        ORDER BY 
            <choose>
                <when test="keyword == null or keyword == ''">RAND()</when>
                <otherwise>created_at DESC</otherwise>
            </choose>
        LIMIT #{limit} OFFSET #{offset}
        </script>
        """)
    List<User> searchUsers(@Param("keyword") String keyword, @Param("limit") int limit, @Param("offset") int offset);
    
    /**
     * 批量查询用户
     */
    @Select("""
        <script>
        SELECT * FROM user WHERE id IN 
        <foreach collection="ids" item="id" open="(" separator="," close=")">
            #{id}
        </foreach>
        </script>
        """)
    List<User> selectByIds(@Param("ids") List<Long> ids);
    
    /**
     * 批量插入用户
     */
    int batchInsert(@Param("list") List<User> users);
}