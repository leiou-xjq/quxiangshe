package com.quxiangshe.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quxiangshe.backend.entity.Forward;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 转发记录数据访问层接口，管理笔记转发记录（Forward）的数据库操作。
 * <p>
 * 提供按原始笔记查询转发列表、按用户查询转发历史等功能，
 * 支持转发记录的追溯与展示。
 * </p>
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
