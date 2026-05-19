package com.quxiangshe.backend.service;

import com.quxiangshe.backend.entity.ViolationCaseLibrary;

import java.util.List;

/**
 * 违规案例库服务接口
 *
 * 核心职责：管理违规案例库的CRUD操作和RAG检索
 * 业务模块：审核模块（RAG Layer 2）
 *
 * 功能清单：
 *   - 案例新增（自动向量化存储到Milvus）
 *   - 案例查询（根据ID、来源审核记录ID）
 *   - 相似案例检索（RAG向量搜索）
 *   - 案例同步（DB→Milvus）
 *   - 案例统计
 *
 * @author 趣享社技术团队
 */
public interface IViolationCaseLibraryService {

    /**
     * 新增违规案例（自动向量化）
     *
     * 流程：
     *   1. 插入DB记录
     *   2. 生成向量并存储到Milvus
     *   3. 更新DB的embedding_id
     *
     * @param caseInfo 案例信息
     * @return 成功返回案例ID，失败返回null
     */
    Long addCase(ViolationCaseLibrary caseInfo);

    /**
     * 检索相似案例（RAG核心方法）
     *
     * 流程：
     *   1. 将文本向量化
     *   2. 在Milvus中搜索TopK相似向量
     *   3. 根据embedding_id查询案例详情
     *   4. 返回案例列表（按相似度降序）
     *
     * @param text 待检索文本（通常为标题+内容拼接）
     * @param topK 返回TopK个最相似案例
     * @return 相似案例列表
     */
    List<ViolationCaseLibrary> searchSimilar(String text, int topK);

    /**
     * 根据ID查询案例
     *
     * @param id 案例ID
     * @return 案例（如果存在）
     */
    ViolationCaseLibrary getById(Long id);

    /**
     * 根据来源审核记录ID查询案例
     *
     * @param sourceReviewId 来源审核记录ID
     * @return 案例（如果存在）
     */
    ViolationCaseLibrary getBySourceReviewId(Long sourceReviewId);

    /**
     * 同步待入库案例到Milvus
     *
     * 定时任务调用，查询embedding_id为null的记录，
     * 逐个生成向量并插入Milvus
     *
     * @param limit 同步数量限制
     * @return 成功同步数量
     */
    int syncPendingCases(int limit);

    /**
     * 统计启用状态的案例数量
     *
     * @return 总数
     */
    long countEnabled();

    /**
     * 根据caseType统计各类型案例数量
     *
     * @return 按类型分组的统计列表
     */
    List<Object> countByCaseType();

    /**
     * 格式化案例为Prompt文本（供LLM参考）
     *
     * 将相似案例列表格式化为字符串，
     * 注入到价值观审核Prompt中作为参考
     *
     * @param cases 相似案例列表
     * @return 格式化后的文本
     */
    String formatCasesForPrompt(List<ViolationCaseLibrary> cases);
}