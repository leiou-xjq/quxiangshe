package com.quxiangshe.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quxiangshe.backend.entity.NoteComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 笔记评论数据访问层接口，管理笔记评论（NoteComment）的数据库操作。
 * <p>
 * 采用抖音风格的评论结构：支持根评论与子评论（回复）的树状层级关系。
 * 提供评论按笔记/根评论查询、批量查询、批量导入、
 * 点赞数的原子递增/递减（解决并发竞态）及权威点赞数查询等功能。
 * </p>
 *
 * @author 趣享社技术团队
 */
@Mapper
public interface NoteCommentMapper extends BaseMapper<NoteComment> {

    /**
     * 查询笔记的所有评论（不分组）
     * 按创建时间倒序
     */
    @Select("""
        SELECT c.*, u.nickname as nickname, u.avatar as avatar
        FROM note_comment c
        LEFT JOIN user u ON c.user_id = u.id
        WHERE c.note_id = #{noteId} AND c.status = 1
        ORDER BY c.created_at DESC
        """)
    List<NoteComment> selectByNoteId(@Param("noteId") Long noteId);

    /**
     * 查询根评论的所有子评论
     */
    @Select("""
        SELECT c.*, u.nickname as nickname, u.avatar as avatar
        FROM note_comment c
        LEFT JOIN user u ON c.user_id = u.id
        WHERE c.root_id = #{rootId} AND c.status = 1
        ORDER BY c.created_at ASC
        """)
    List<NoteComment> selectRepliesByRootId(@Param("rootId") Long rootId);

    /**
     * 批量查询评论（用于一次性获取多个评论详情）
     */
    @Select("""
        <script>
        SELECT c.*, u.nickname as nickname, u.avatar as avatar
        FROM note_comment c
        LEFT JOIN user u ON c.user_id = u.id
        WHERE c.id IN
        <foreach collection="ids" item="id" open="(" separator="," close=")">
            #{id}
        </foreach>
        AND c.status = 1
        </script>
        """)
    List<NoteComment> selectByIds(@Param("ids") List<Long> ids);

    /**
     * 批量插入评论
     */
    void batchInsert(@Param("list") List<NoteComment> comments);

    /**
     * 原子递增评论点赞数（解决并发竞态问题）
     * SQL: UPDATE note_comment SET like_count = like_count + 1 WHERE id = #{id} AND status = 1
     */
    @Update("UPDATE note_comment SET like_count = like_count + 1 WHERE id = #{id} AND status = 1")
    int incrementLikeCount(@Param("id") Long id);

    /**
     * 原子递减评论点赞数（解决并发竞态问题，最小值为0）
     * SQL: UPDATE note_comment SET like_count = GREATEST(0, like_count - 1) WHERE id = #{id} AND status = 1
     */
    @Update("UPDATE note_comment SET like_count = GREATEST(0, like_count - 1) WHERE id = #{id} AND status = 1")
    int decrementLikeCount(@Param("id") Long id);

    /**
     * 查询评论点赞数（用于返回权威计数）
     */
    @Select("SELECT like_count FROM note_comment WHERE id = #{id} AND status = 1")
    Integer selectLikeCount(@Param("id") Long id);
}