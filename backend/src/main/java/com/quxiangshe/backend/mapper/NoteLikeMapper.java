package com.quxiangshe.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quxiangshe.backend.entity.NoteLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 笔记点赞Mapper接口
 * 
 * @author 趣享社技术团队
 */
@Mapper
public interface NoteLikeMapper extends BaseMapper<NoteLike> {
    
    /**
     * 检查用户是否已点赞
     */
    @Select("""
        SELECT COUNT(*) > 0 FROM note_like 
        WHERE note_id = #{noteId} AND user_id = #{userId}
        """)
    boolean checkUserLiked(@Param("noteId") Long noteId, @Param("userId") Long userId);
    
    /**
     * 批量检查用户是否已点赞
     */
    @Select("""
        <script>
        SELECT note_id FROM note_like 
        WHERE user_id = #{userId} AND note_id IN 
        <foreach collection="noteIds" item="id" open="(" separator="," close=")">
            #{id}
        </foreach>
        </script>
        """)
    java.util.List<Long> checkUserLikedBatch(@Param("noteIds") java.util.List<Long> noteIds, @Param("userId") Long userId);
    
    /**
     * 批量查询用户点赞的笔记ID列表
     */
    @Select("""
        <script>
        SELECT note_id FROM note_like 
        WHERE user_id = #{userId} AND note_id IN 
        <foreach collection="noteIds" item="id" open="(" separator="," close=")">
            #{id}
        </foreach>
        </script>
        """)
    java.util.List<Long> selectLikedNoteIds(@Param("userId") Long userId, @Param("noteIds") java.util.List<Long> noteIds);
}
