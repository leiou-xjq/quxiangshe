package com.quxiangshe.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quxiangshe.backend.entity.NoteReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 笔记审核记录数据访问层接口，管理笔记审核记录（NoteReview）的数据库操作。
 * <p>
 * 支持三层审核体系（敏感词检测→RAG相似度→大模型判定）的审核记录存储与查询。
 * 提供待审核列表、用户审核历史、违规统计、今日审核统计、
 * 违规案例导入标记及按笔记ID查询审核记录等功能。
 * </p>
 * 
 * @author 趣享社技术团队
 */
@Mapper
public interface NoteReviewMapper extends BaseMapper<NoteReview> {
    
    /**
     * 查询待审核列表
     */
    @Select("SELECT * FROM note_review WHERE review_status = 0 ORDER BY created_at DESC LIMIT #{limit}")
    List<NoteReview> findPendingReviews(@Param("limit") int limit);
    
    /**
     * 查询用户的审核历史
     */
    @Select("SELECT * FROM note_review WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{offset}, #{limit}")
    List<NoteReview> findUserReviewHistory(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);
    
    /**
     * 统计违规数量
     */
    @Select("SELECT COUNT(*) FROM note_review WHERE review_status = 3")
    long countViolations();
    
    /**
     * 统计待审核数量
     */
    @Select("SELECT COUNT(*) FROM note_review WHERE review_status = 0")
    long countPending();
    
    /**
     * 获取今日审核统计
     */
    @Select("SELECT review_status, COUNT(*) as count FROM note_review WHERE DATE(created_at) = CURDATE() GROUP BY review_status")
    List<Map<String, Object>> getTodayStatistics();
    
    /**
     * 查询待导入的违规记录
     */
    @Select("SELECT * FROM note_review WHERE review_status = 3 AND (case_imported IS NULL OR case_imported = 0) AND violation_reason IS NOT NULL ORDER BY review_time DESC LIMIT 100")
    List<NoteReview> findPendingImport();
    
    /**
     * 标记为已导入案例库
     */
    @Update("UPDATE note_review SET case_imported = 1 WHERE id = #{id}")
    void markAsImported(@Param("id") Long id);

    /**
     * 根据笔记ID查询审核记录
     */
    @Select("SELECT * FROM note_review WHERE note_id = #{noteId} LIMIT 1")
    NoteReview selectByNoteId(@Param("noteId") Long noteId);
}