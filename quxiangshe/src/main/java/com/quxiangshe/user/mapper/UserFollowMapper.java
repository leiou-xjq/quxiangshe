package com.quxiangshe.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quxiangshe.user.entity.UserFollowEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 用户关注Mapper接口
 */
@Mapper
public interface UserFollowMapper extends BaseMapper<UserFollowEntity> {

    /**
     * 查询粉丝ID列表
     */
    @Select("SELECT user_id FROM user_follow WHERE follow_user_id = #{userId}")
    @Results({
            @Result(property = "userId", column = "user_id")
    })
    List<Long> selectFollowerIds(@Param("userId") Long userId);

    /**
     * 查询关注ID列表
     */
    @Select("SELECT follow_user_id FROM user_follow WHERE user_id = #{userId}")
    @Results({
            @Result(property = "followUserId", column = "follow_user_id")
    })
    List<Long> selectFollowingIds(@Param("userId") Long userId);

    /**
     * 检查是否已关注
     */
    @Select("SELECT COUNT(*) FROM user_follow WHERE user_id = #{userId} AND follow_user_id = #{followUserId}")
    int checkFollowing(@Param("userId") Long userId, @Param("followUserId") Long followUserId);

    /**
     * 插入关注关系
     */
    @Insert("INSERT INTO user_follow(user_id, follow_user_id) VALUES(#{userId}, #{followUserId})")
    int insertFollowing(@Param("userId") Long userId, @Param("followUserId") Long followUserId);

    /**
     * 删除关注关系
     */
    @Delete("DELETE FROM user_follow WHERE user_id = #{userId} AND follow_user_id = #{followUserId}")
    int deleteFollowing(@Param("userId") Long userId, @Param("followUserId") Long followUserId);

    /**
     * 统计粉丝数
     */
    @Select("SELECT COUNT(*) FROM user_follow WHERE follow_user_id = #{userId}")
    Long countFollowers(@Param("userId") Long userId);

    /**
     * 统计关注数
     */
    @Select("SELECT COUNT(*) FROM user_follow WHERE user_id = #{userId}")
    Long countFollowings(@Param("userId") Long userId);
}
