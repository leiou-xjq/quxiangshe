package com.quxiangshe.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quxiangshe.backend.entity.NoteFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 笔记收藏数据访问层接口，管理用户笔记收藏关系（NoteFavorite）的数据库操作。
 * <p>
 * 提供单个及批量收藏状态检查、用户收藏列表分页查询、
 * 用户已收藏笔记ID列表查询等功能。
 * </p>
 * 
 * @author 趣享社技术团队
 */
@Mapper
public interface NoteFavoriteMapper extends BaseMapper<NoteFavorite> {
    
    /**
     * 检查用户是否已收藏
     */
    @Select("""
        SELECT COUNT(*) > 0 FROM note_favorite 
        WHERE note_id = #{noteId} AND user_id = #{userId}
        """)
    boolean checkUserFavorited(@Param("noteId") Long noteId, @Param("userId") Long userId);
    
    /**
     * 查询用户的收藏列表
     */
    @Select("""
        SELECT * FROM note_favorite 
        WHERE user_id = #{userId} 
        ORDER BY created_at DESC 
        LIMIT #{limit} OFFSET #{offset}
        """)
    List<NoteFavorite> selectByUserId(@Param("userId") Long userId, @Param("limit") int limit, @Param("offset") int offset);
    
    /**
     * 批量检查用户是否已收藏
     */
    @Select("""
        <script>
        SELECT note_id FROM note_favorite 
        WHERE user_id = #{userId} AND note_id IN 
        <foreach collection="noteIds" item="id" open="(" separator="," close=")">
            #{id}
        </foreach>
        </script>
        """)
    List<Long> checkUserFavoritedBatch(@Param("noteIds") List<Long> noteIds, @Param("userId") Long userId);
    
    /**
     * 批量查询用户收藏的笔记ID列表
     */
    @Select("""
        <script>
        SELECT note_id FROM note_favorite 
        WHERE user_id = #{userId} AND note_id IN 
        <foreach collection="noteIds" item="id" open="(" separator="," close=")">
            #{id}
        </foreach>
        </script>
        """)
    List<Long> selectFavoritedNoteIds(@Param("userId") Long userId, @Param("noteIds") List<Long> noteIds);
}
