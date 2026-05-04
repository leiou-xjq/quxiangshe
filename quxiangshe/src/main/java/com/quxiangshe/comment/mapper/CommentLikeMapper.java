package com.quxiangshe.comment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quxiangshe.comment.entity.CommentLikeEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 评论点赞Mapper接口
 * 对应数据库表 t_comment_like
 */
@Mapper
public interface CommentLikeMapper extends BaseMapper<CommentLikeEntity> {

    /**
     * 检查用户是否已点赞
     *
     * @param commentId 评论ID
     * @param userId 用户ID
     * @return 点赞记录存在返回true
     */
    @Select("SELECT COUNT(*) > 0 FROM t_comment_like WHERE comment_id = #{commentId} AND user_id = #{userId}")
    boolean checkUserLiked(@Param("commentId") Long commentId, @Param("userId") Long userId);

    /**
     * 根据评论ID统计点赞数
     *
     * @param commentId 评论ID
     * @return 点赞数
     */
    @Select("SELECT COUNT(*) FROM t_comment_like WHERE comment_id = #{commentId}")
    Long countByCommentId(@Param("commentId") Long commentId);
}
