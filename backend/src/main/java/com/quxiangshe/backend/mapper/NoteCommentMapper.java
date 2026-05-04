package com.quxiangshe.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quxiangshe.backend.entity.NoteComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 笔记评论Mapper接口 - 抖音风格
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
}