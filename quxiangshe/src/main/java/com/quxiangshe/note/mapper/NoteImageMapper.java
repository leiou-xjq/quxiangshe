package com.quxiangshe.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quxiangshe.note.entity.NoteImageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 笔记图片Mapper接口
 */
@Mapper
public interface NoteImageMapper extends BaseMapper<NoteImageEntity> {

    /**
     * 根据笔记ID查询图片列表
     *
     * @param noteId 笔记ID
     * @return 图片列表
     */
    @Select("SELECT * FROM t_note_image WHERE note_id = #{noteId} ORDER BY image_order ASC")
    List<NoteImageEntity> selectByNoteId(@Param("noteId") Long noteId);
}
