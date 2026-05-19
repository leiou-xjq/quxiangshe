package com.quxiangshe.backend.service.impl;

import com.quxiangshe.backend.entity.ViolationCaseLibrary;
import com.quxiangshe.backend.mapper.ViolationCaseLibraryMapper;
import com.quxiangshe.backend.service.IViolationCaseLibraryService;
import com.quxiangshe.backend.service.VectorSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 违规案例库服务实现类
 *
 * 核心职责：实现案例库CRUD和RAG相似案例检索
 * 业务模块：审核模块（RAG Layer 2）
 *
 * 设计要点：
 *   - 新增案例时自动向量化并存储到Milvus
 *   - RAG检索时先向量搜索再查询案例详情
 *   - 支持定时任务同步待入库案例
 *
 * @author 趣享社技术团队
 */
@Slf4j
@Service
public class ViolationCaseLibraryServiceImpl implements IViolationCaseLibraryService {

    @Autowired
    private ViolationCaseLibraryMapper caseLibraryMapper;

    @Autowired
    private VectorSearchService vectorSearchService;

    @Value("${rag.top-k:5}")
    private int defaultTopK;

    @Value("${rag.similarity-threshold:0.7}")
    private double similarityThreshold;

    /**
     * 新增违规案例（自动向量化存储到Milvus）
     *
     * 流程：
     *   1. 插入DB记录，获取自增ID
     *   2. 拼接文本（标题+内容）生成向量
     *   3. 调用VectorSearchService插入Milvus
     *   4. 更新DB的embedding_id
     *
     * @param caseInfo 案例信息
     * @return 成功返回案例ID，失败返回null
     */
    @Override
    public Long addCase(ViolationCaseLibrary caseInfo) {
        try {
            // 1. 插入DB记录
            caseInfo.setStatus(ViolationCaseLibrary.STATUS_ENABLED);
            caseLibraryMapper.insert(caseInfo);

            Long caseId = caseInfo.getId();
            log.info("案例DB记录插入成功: caseId={}, caseType={}", caseId, caseInfo.getCaseType());

            // 2. 生成向量并插入Milvus
            String text = buildCaseText(caseInfo.getTitle(), caseInfo.getContent());
            boolean insertSuccess = vectorSearchService.insertVector(caseId, text);

            if (insertSuccess) {
                // 3. 标记已同步到Milvus（embedding_id已在insertVector中作为主键）
                log.info("案例向量已存储到Milvus: caseId={}", caseId);
            } else {
                log.warn("案例向量存储失败，将通过定时任务重试: caseId={}", caseId);
            }

            return caseId;

        } catch (Exception e) {
            log.error("新增案例失败: caseType={}, error={}", caseInfo.getCaseType(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * 检索相似案例（RAG核心方法）
     *
     * 流程：
     *   1. VectorSearchService搜索TopK相似向量
     *   2. 根据embedding_id查询案例详情
     *   3. 过滤低于阈值的案例
     *   4. 返回按相似度降序的案例列表
     *
     * @param text 待检索文本
     * @param topK 返回TopK个最相似案例
     * @return 相似案例列表（按相似度降序）
     */
    @Override
    public List<ViolationCaseLibrary> searchSimilar(String text, int topK) {
        try {
            // 1. 向量搜索
            List<VectorSearchService.SearchResult> searchResults =
                vectorSearchService.searchSimilarCases(text, topK);

            if (searchResults == null || searchResults.isEmpty()) {
                log.debug("未找到相似案例: text={}", text);
                return List.of();
            }

            // 2. 过滤低于阈值的案例
            List<VectorSearchService.SearchResult> filteredResults = searchResults.stream()
                .filter(r -> r.getSimilarityScore() != null && r.getSimilarityScore() >= similarityThreshold)
                .collect(Collectors.toList());

            if (filteredResults.isEmpty()) {
                log.debug("无高于阈值的相似案例: threshold={}", similarityThreshold);
                return List.of();
            }

            // 3. 根据embedding_id查询案例详情
            List<Long> embeddingIds = filteredResults.stream()
                .map(VectorSearchService.SearchResult::getEmbeddingId)
                .collect(Collectors.toList());

            List<ViolationCaseLibrary> cases = caseLibraryMapper.selectByEmbeddingIds(embeddingIds);

            // 4. 按相似度降序排序（保持与搜索结果顺序一致）
            return sortBySimilarity(cases, filteredResults);

        } catch (Exception e) {
            log.error("检索相似案例异常: text={}, error={}", text, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 根据ID查询案例
     */
    @Override
    public ViolationCaseLibrary getById(Long id) {
        return caseLibraryMapper.selectById(id);
    }

    /**
     * 根据来源审核记录ID查询案例
     */
    @Override
    public ViolationCaseLibrary getBySourceReviewId(Long sourceReviewId) {
        return caseLibraryMapper.selectBySourceReviewId(sourceReviewId);
    }

    /**
     * 同步待入库案例到Milvus（定时任务调用）
     */
    @Override
    public int syncPendingCases(int limit) {
        try {
            // 1. 查询待同步案例
            List<ViolationCaseLibrary> pendingCases = caseLibraryMapper.selectPendingSync(limit);

            if (pendingCases.isEmpty()) {
                log.debug("无待同步案例");
                return 0;
            }

            log.info("开始同步待入库案例: count={}", pendingCases.size());

            // 2. 逐个生成向量并插入Milvus
            int successCount = 0;
            for (ViolationCaseLibrary caseInfo : pendingCases) {
                try {
                    String text = buildCaseText(caseInfo.getTitle(), caseInfo.getContent());
                    boolean success = vectorSearchService.insertVector(caseInfo.getId(), text);

                    if (success) {
                        // 3. 更新embedding_id（由于使用caseId作为主键，embedding_id就是caseId本身）
                        caseLibraryMapper.updateEmbeddingId(caseInfo.getId(), caseInfo.getId());
                        successCount++;
                    }

                } catch (Exception e) {
                    log.error("同步案例失败: caseId={}, error={}", caseInfo.getId(), e.getMessage());
                }
            }

            log.info("案例同步完成: success={}, total={}", successCount, pendingCases.size());
            return successCount;

        } catch (Exception e) {
            log.error("同步案例异常: error={}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 统计启用状态的案例数量
     */
    @Override
    public long countEnabled() {
        return caseLibraryMapper.countEnabled();
    }

    /**
     * 统计各类型案例数量
     */
    @Override
    public List<Object> countByCaseType() {
        return caseLibraryMapper.countByCaseType();
    }

    /**
     * 格式化案例为Prompt文本（供LLM参考）
     *
     * 格式：
     * ---
     * 参考案例1（相似度: 0.85, 类型: 毒鸡汤）
     * 标题: xxx
     * 内容: xxx
     * 违规原因: xxx
     * ---
     * 参考案例2（相似度: 0.78, 类型: 性别对立）
     * ...
     *
     * @param cases 相似案例列表
     * @return 格式化后的文本
     */
    @Override
    public String formatCasesForPrompt(List<ViolationCaseLibrary> cases) {
        if (cases == null || cases.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n\n## 参考相似案例\n");

        for (int i = 0; i < cases.size(); i++) {
            ViolationCaseLibrary c = cases.get(i);
            sb.append(String.format("---案例%d（类型: %s）---\n", i + 1, c.getCaseType()));
            sb.append(String.format("标题: %s\n", c.getTitle() != null ? c.getTitle() : ""));
            sb.append(String.format("内容: %s\n", c.getContent() != null ? c.getContent() : ""));
            sb.append(String.format("违规原因: %s\n", c.getViolationReason() != null ? c.getViolationReason() : "无"));
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * 构建案例文本（标题+内容拼接）
     */
    private String buildCaseText(String title, String content) {
        StringBuilder sb = new StringBuilder();
        if (title != null && !title.isEmpty()) {
            sb.append(title).append(" ");
        }
        if (content != null && !content.isEmpty()) {
            sb.append(content);
        }
        return sb.toString().trim();
    }

    /**
     * 按相似度排序（与搜索结果顺序保持一致）
     */
    private List<ViolationCaseLibrary> sortBySimilarity(
            List<ViolationCaseLibrary> cases,
            List<VectorSearchService.SearchResult> searchResults) {

        // 构建embeddingId -> similarityScore映射
        java.util.Map<Long, Double> scoreMap = new java.util.HashMap<>();
        for (VectorSearchService.SearchResult r : searchResults) {
            if (r.getEmbeddingId() != null && r.getSimilarityScore() != null) {
                scoreMap.put(r.getEmbeddingId(), r.getSimilarityScore());
            }
        }

        // 按相似度降序排序
        return cases.stream()
            .sorted((a, b) -> {
                Double scoreA = scoreMap.get(a.getId());
                Double scoreB = scoreMap.get(b.getId());
                if (scoreA == null) return 1;
                if (scoreB == null) return -1;
                return scoreB.compareTo(scoreA);
            })
            .collect(Collectors.toList());
    }
}