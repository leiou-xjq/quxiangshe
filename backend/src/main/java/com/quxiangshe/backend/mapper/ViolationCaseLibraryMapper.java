package com.quxiangshe.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quxiangshe.backend.entity.ViolationCaseLibrary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 违规案例库数据访问层接口
 *
 * 核心职责：管理违规案例库（ViolationCaseLibrary）的数据库操作
 * 业务模块：审核模块（RAG Layer 2）
 *
 * 功能：
 *   - 案例CRUD操作
 *   - 根据embedding_id查询案例
 *   - 查询待同步到Milvus的案例
 *   - 统计各类型案例数量
 *
 * @author 趣享社技术团队
 */
@Mapper
public interface ViolationCaseLibraryMapper extends BaseMapper<ViolationCaseLibrary> {

    /**
     * 根据embedding_id列表查询案例详情
     *
     * @param embeddingIds Milvus向量ID列表
     * @return 案例列表（按embedding_id顺序）
     */
    @Select("<script>" +
        "SELECT * FROM violation_case_library WHERE embedding_id IN " +
        "<foreach collection='embeddingIds' item='id' open='(' separator=',' close=')'>" +
        "#{id}" +
        "</foreach>" +
        " AND status = 1" +
        "</script>")
    List<ViolationCaseLibrary> selectByEmbeddingIds(@Param("embeddingIds") List<Long> embeddingIds);

    /**
     * 查询待同步到Milvus的案例（embedding_id为null的记录）
     *
     * @param limit 查询数量限制
     * @return 待同步案例列表
     */
    @Select("SELECT * FROM violation_case_library WHERE embedding_id IS NULL AND status = 1 ORDER BY created_at DESC LIMIT #{limit}")
    List<ViolationCaseLibrary> selectPendingSync(@Param("limit") int limit);

    /**
     * 统计启用状态的案例数量
     *
     * @return 总数
     */
    @Select("SELECT COUNT(*) FROM violation_case_library WHERE status = 1")
    long countEnabled();

    /**
     * 统计各类型案例数量
     *
     * @return 按类型分组的统计结果
     */
    @Select("SELECT case_type, COUNT(*) as count FROM violation_case_library WHERE status = 1 GROUP BY case_type")
    List<Object> countByCaseType();

    /**
     * 根据来源审核记录ID查询案例
     *
     * @param sourceReviewId 来源审核记录ID
     * @return 案例（如果有）
     */
    @Select("SELECT * FROM violation_case_library WHERE source_review_id = #{sourceReviewId} LIMIT 1")
    ViolationCaseLibrary selectBySourceReviewId(@Param("sourceReviewId") Long sourceReviewId);

    /**
     * 更新案例的embedding_id
     *
     * @param id 案例ID
     * @param embeddingId Milvus向量ID
     */
    @Update("UPDATE violation_case_library SET embedding_id = #{embeddingId}, updated_at = NOW() WHERE id = #{id}")
    void updateEmbeddingId(@Param("id") Long id, @Param("embeddingId") Long embeddingId);
}