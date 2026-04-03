package com.quxiangshe.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quxiangshe.note.entity.NoteCollectEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 笔记收藏Mapper接口
 */
@Mapper
public interface NoteCollectMapper extends BaseMapper<NoteCollectEntity> {

    /**
     * 检查用户是否已收藏
     *
     * @param noteId 笔记ID
     * @param userId 用户ID
     * @return true表示已收藏
     */
    @Select("SELECT COUNT(*) > 0 FROM t_note_collect WHERE note_id = #{noteId} AND user_id = #{userId}")
    boolean checkUserCollected(@Param("noteId") Long noteId, @Param("userId") Long userId);

    /**
     * 删除用户的收藏记录
     *
     * @param noteId 笔记ID
     * @param userId 用户ID
     * @return 删除数量
     */
    @Delete("DELETE FROM t_note_collect WHERE note_id = #{noteId} AND user_id = #{userId}")
    int deleteByUserAndNote(@Param("noteId") Long noteId, @Param("userId") Long userId);
}
