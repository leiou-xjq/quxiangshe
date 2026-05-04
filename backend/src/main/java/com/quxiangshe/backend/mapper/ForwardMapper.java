package com.quxiangshe.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quxiangshe.backend.entity.Forward;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 转发记录Mapper接口
 * 
 * @author 趣享社技术团队
 */
@Mapper
public interface ForwardMapper extends BaseMapper<Forward> {
    
    /**
     * 查询某笔记的所有转发记录
     */
    @Select("""
        SELECT * FROM note_forward 
        WHERE original_note_id = #{noteId} 
        ORDER BY created_at DESC 
        LIMIT #{limit} OFFSET #{offset}
        """)
    List<Forward> selectByOriginalNoteId(@Param("noteId") Long noteId, @Param("limit") int limit, @Param("offset") int offset);
    
    /**
     * 查询某用户的所有转发记录
     */
    @Select("""
        SELECT * FROM note_forward 
        WHERE user_id = #{userId} 
        ORDER BY created_at DESC 
        LIMIT #{limit} OFFSET #{offset}
        """)
    List<Forward> selectByUserId(@Param("userId") Long userId, @Param("limit") int limit, @Param("offset") int offset);
}
