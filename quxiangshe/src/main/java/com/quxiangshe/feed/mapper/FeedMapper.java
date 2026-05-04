package com.quxiangshe.feed.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quxiangshe.feed.entity.FeedEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Feed流Mapper接口
 */
@Mapper
public interface FeedMapper extends BaseMapper<FeedEntity> {

    /**
     * 查询用户的Feed流（用于拉模式兜底）
     *
     * @param userId    用户ID
     * @param cursorTime 游标时间
     * @param cursorId   游标ID
     * @param limit     数量限制
     * @return Feed列表
     */
    @Select("SELECT f.* FROM feed f " +
            "INNER JOIN user_follow uf ON f.creator_id = uf.follow_user_id " +
            "WHERE uf.user_id = #{userId} " +
            "AND f.created_at < #{cursorTime} " +
            "ORDER BY f.created_at DESC, f.id DESC " +
            "LIMIT #{limit}")
    List<FeedEntity> selectUserFeed(
            @Param("userId") Long userId,
            @Param("cursorTime") LocalDateTime cursorTime,
            @Param("cursorId") Long cursorId,
            @Param("limit") int limit);

    /**
     * 批量插入Feed
     */
    int batchInsert(@Param("list") List<FeedEntity> feeds);
}
