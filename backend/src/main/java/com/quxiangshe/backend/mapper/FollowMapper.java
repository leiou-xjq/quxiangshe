package com.quxiangshe.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quxiangshe.backend.entity.Follow;
import com.quxiangshe.backend.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 关注关系Mapper接口
 * 
 * @author 趣享社技术团队
 */
@Mapper
public interface FollowMapper extends BaseMapper<Follow> {
    
    /**
     * 检查用户是否已关注
     */
    int checkFollowing(@Param("followerId") Long followerId, @Param("followingId") Long followingId);
    
    /**
     * 游标分页查询粉丝列表
     */
    List<User> selectFollowersByCursor(@Param("userId") Long userId, 
                                        @Param("cursor") Long cursor, 
                                        @Param("size") int size);
    
    /**
     * 游标分页查询关注列表
     */
    List<User> selectFollowingByCursor(@Param("userId") Long userId, 
                                     @Param("cursor") Long cursor, 
                                     @Param("size") int size);
    
    /**
     * 批量查询关注状态
     */
    List<Long> batchCheckFollowing(@Param("currentUserId") Long currentUserId, 
                                    @Param("userIds") List<Long> userIds);
    
    /**
     * 获取粉丝关注记录ID
     */
    Long getFollowId(@Param("followingId") Long followingId, @Param("followerId") Long followerId);
    
    /**
     * 获取关注记录ID
     */
    Long getFollowIdByFollower(@Param("followerId") Long followerId, @Param("followingId") Long followingId);
    
    /**
     * 批量插入关注关系
     */
    int batchInsert(@Param("list") List<Follow> follows);
    
    /**
     * 批量查询用户关注的作者ID列表
     */
    List<Long> selectFollowingAuthorIds(@Param("userIds") List<Long> userIds);
    
/**
 * 查询指定作者的所有粉丝ID
 */
List<Long> selectFollowerIdsByAuthorId(@Param("authorId") Long authorId);

/**
 * 批量查询粉丝ID（带游标分页）
 */
List<Long> selectFollowerIdsByAuthorIdWithCursor(@Param("authorId") Long authorId, 
                                          @Param("cursor") Long cursor, 
                                          @Param("limit") int limit);

/**
 * 查询粉丝数超过阈值的用户ID列表（用于确定大博主）
 */
    List<Long> selectBloggerIdsByFollowerCount(@Param("threshold") Long threshold);
}