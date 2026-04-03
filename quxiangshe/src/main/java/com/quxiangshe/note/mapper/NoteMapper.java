package com.quxiangshe.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quxiangshe.note.entity.NoteEntity;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 笔记Mapper接口
 * 对应数据库表 t_note
 */
@Mapper
public interface NoteMapper extends BaseMapper<NoteEntity> {

    /**
     * 查询用户的笔记列表
     * 用户自己的笔记：查询审核通过的，排除用户删除的
     *
     * @param userId 用户ID
     * @param lastId 游标ID
     * @param limit 每页数量
     * @return 笔记列表
     */
    @Select("<script>" +
            "SELECT * FROM t_note WHERE user_id = #{userId} AND deleted = 0 " +
            "AND audit_status = 1 " +
            "<if test='lastId != null and lastId > 0'> AND id &lt; #{lastId} </if>" +
            "ORDER BY create_time DESC, id DESC LIMIT #{limit}" +
            "</script>")
    List<NoteEntity> selectByUserId(
            @Param("userId") Long userId,
            @Param("lastId") Long lastId,
            @Param("limit") int limit);

    /**
     * 查询首页笔记列表
     * 展示审核通过(status=1)的笔记，排除用户删除的
     *
     * @param lastId 游标ID
     * @param limit 每页数量
     * @return 笔记列表
     */
    @Select("<script>" +
            "SELECT * FROM t_note WHERE deleted = 0 " +
            "AND audit_status = 1 AND status = 1 " +
            "<if test='lastId != null and lastId > 0'> AND id &lt; #{lastId} </if>" +
            "ORDER BY create_time DESC, id DESC LIMIT #{limit}" +
            "</script>")
    List<NoteEntity> selectForHome(
            @Param("lastId") Long lastId,
            @Param("limit") int limit);

    /**
     * 更新点赞数
     *
     * @param noteId 笔记ID
     * @return 更新行数
     */
    @Update("UPDATE t_note SET like_count = like_count + 1 WHERE id = #{noteId}")
    int incrementLikeCount(@Param("noteId") Long noteId);

    /**
     * 减少点赞数
     *
     * @param noteId 笔记ID
     * @return 更新行数
     */
    @Update("UPDATE t_note SET like_count = like_count - 1 WHERE id = #{noteId} AND like_count > 0")
    int decrementLikeCount(@Param("noteId") Long noteId);

    /**
     * 更新评论数
     *
     * @param noteId 笔记ID
     * @return 更新行数
     */
    @Update("UPDATE t_note SET comment_count = comment_count + 1 WHERE id = #{noteId}")
    int incrementCommentCount(@Param("noteId") Long noteId);

    /**
     * 更新浏览数
     *
     * @param noteId 笔记ID
     * @return 更新行数
     */
    @Update("UPDATE t_note SET view_count = view_count + 1 WHERE id = #{noteId}")
    int incrementViewCount(@Param("noteId") Long noteId);

    /**
     * 更新收藏数
     *
     * @param noteId 笔记ID
     * @return 更新行数
     */
    @Update("UPDATE t_note SET collect_count = collect_count + 1 WHERE id = #{noteId}")
    int incrementCollectCount(@Param("noteId") Long noteId);

    /**
     * 减少收藏数
     *
     * @param noteId 笔记ID
     * @return 更新行数
     */
    @Update("UPDATE t_note SET collect_count = collect_count - 1 WHERE id = #{noteId} AND collect_count > 0")
    int decrementCollectCount(@Param("noteId") Long noteId);

    /**
     * 批量插入笔记
     *
     * @param notes 笔记列表
     * @return 插入数量
     */
    int batchInsert(@Param("list") List<NoteEntity> notes);
}
