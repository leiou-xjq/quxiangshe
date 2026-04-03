package com.quxiangshe.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quxiangshe.note.entity.NoteLikeEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 笔记点赞Mapper接口
 */
@Mapper
public interface NoteLikeMapper extends BaseMapper<NoteLikeEntity> {

    /**
     * 检查用户是否已点赞
     *
     * @param noteId 笔记ID
     * @param userId 用户ID
     * @return true表示已点赞
     */
    @Select("SELECT COUNT(*) > 0 FROM t_note_like WHERE note_id = #{noteId} AND user_id = #{userId}")
    boolean checkUserLiked(@Param("noteId") Long noteId, @Param("userId") Long userId);

    /**
     * 删除用户的点赞记录
     *
     * @param noteId 笔记ID
     * @param userId 用户ID
     * @return 删除数量
     */
    @Delete("DELETE FROM t_note_like WHERE note_id = #{noteId} AND user_id = #{userId}")
    int deleteByUserAndNote(@Param("noteId") Long noteId, @Param("userId") Long userId);
}
