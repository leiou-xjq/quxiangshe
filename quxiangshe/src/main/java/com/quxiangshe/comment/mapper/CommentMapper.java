package com.quxiangshe.comment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quxiangshe.comment.entity.CommentEntity;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 评论Mapper接口
 * 对应数据库表 t_comment
 */
@Mapper
public interface CommentMapper extends BaseMapper<CommentEntity> {

    /**
     * 查询文章的一级评论列表（两层扁平结构）
     * 返回一级评论及其直接回复
     *
     * @param articleId 文章ID
     * @param lastId 游标ID（上一页最后一条评论的ID）
     * @param limit 每页数量
     * @return 评论列表
     */
    @Select("<script>" +
            "SELECT * FROM t_comment WHERE article_id = #{articleId} AND deleted = 0 " +
            "<if test='lastId != null and lastId > 0'> AND id &lt; #{lastId} </if>" +
            "ORDER BY create_time DESC, id DESC LIMIT #{limit}" +
            "</script>")
    List<CommentEntity> selectCommentsWithReplies(
            @Param("articleId") Long articleId,
            @Param("lastId") Long lastId,
            @Param("limit") int limit);

    /**
     * 查询一级评论列表
     *
     * @param articleId 文章ID
     * @param lastId 游标ID
     * @param limit 每页数量
     * @return 一级评论列表
     */
    @Select("<script>" +
            "SELECT * FROM t_comment WHERE article_id = #{articleId} AND deleted = 0 " +
            "AND target_id = 0 " +
            "<if test='lastId != null and lastId > 0'> AND id &lt; #{lastId} </if>" +
            "ORDER BY create_time DESC, id DESC LIMIT #{limit}" +
            "</script>")
    List<CommentEntity> selectTopComments(
            @Param("articleId") Long articleId,
            @Param("lastId") Long lastId,
            @Param("limit") int limit);

    /**
     * 查询某个一级评论下的所有回复（二级评论）
     *
     * @param articleId 文章ID
     * @param targetId 目标评论ID（一级评论ID）
     * @param lastId 游标ID
     * @param limit 每页数量
     * @return 回复列表
     */
    @Select("<script>" +
            "SELECT * FROM t_comment WHERE article_id = #{articleId} AND target_id = #{targetId} " +
            "AND deleted = 0 " +
            "<if test='lastId != null and lastId > 0'> AND id &lt; #{lastId} </if>" +
            "ORDER BY create_time ASC, id ASC LIMIT #{limit}" +
            "</script>")
    List<CommentEntity> selectReplies(
            @Param("articleId") Long articleId,
            @Param("targetId") Long targetId,
            @Param("lastId") Long lastId,
            @Param("limit") int limit);

    /**
     * 查询某个一级评论下的回复数量
     *
     * @param articleId 文章ID
     * @param targetId 目标评论ID
     * @return 回复数量
     */
    @Select("SELECT COUNT(*) FROM t_comment WHERE article_id = #{articleId} AND target_id = #{targetId} " +
            "AND deleted = 0")
    Long countReplies(@Param("articleId") Long articleId, @Param("targetId") Long targetId);

    /**
     * 批量插入评论
     *
     * @param comments 评论列表
     * @return 插入数量
     */
    int batchInsert(@Param("list") List<CommentEntity> comments);

    /**
     * 更新回复数（对一级评论）
     *
     * @param commentId 评论ID
     * @return 更新行数
     */
    @Update("UPDATE t_comment SET reply_count = reply_count + 1 WHERE id = #{commentId}")
    int incrementReplyCount(@Param("commentId") Long commentId);

    /**
     * 减少回复数
     *
     * @param commentId 评论ID
     * @return 更新行数
     */
    @Update("UPDATE t_comment SET reply_count = reply_count - 1 WHERE id = #{commentId} AND reply_count > 0")
    int decrementReplyCount(@Param("commentId") Long commentId);

    /**
     * 更新点赞数
     *
     * @param commentId 评论ID
     * @return 更新行数
     */
    @Update("UPDATE t_comment SET like_count = like_count + 1 WHERE id = #{commentId}")
    int incrementLikeCount(@Param("commentId") Long commentId);

    /**
     * 减少点赞数
     *
     * @param commentId 评论ID
     * @return 更新行数
     */
    @Update("UPDATE t_comment SET like_count = like_count - 1 WHERE id = #{commentId} AND like_count > 0")
    int decrementLikeCount(@Param("commentId") Long commentId);

    /**
     * 查询文章的一级评论数量
     *
     * @param articleId 文章ID
     * @return 一级评论数量
     */
    @Select("SELECT COUNT(*) FROM t_comment WHERE article_id = #{articleId} AND target_id = 0 AND deleted = 0")
    Long countTopComments(@Param("articleId") Long articleId);
}
