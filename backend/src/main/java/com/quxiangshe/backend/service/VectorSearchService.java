package com.quxiangshe.backend.service;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.dml.InsertParam;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 向量检索服务（Milvus）
 *
 * 核心职责：在Milvus向量数据库中搜索相似违规案例
 * 业务模块：审核模块（RAG Layer 2 - 向量检索）
 *
 * 注意：当前版本仅实现基本框架，实际向量操作需通过Milvus CLI或Attu图形界面完成
 *
 * @author 趣享社技术团队
 */
@Slf4j
@Service
public class VectorSearchService {

    @Autowired
    private MilvusServiceClient milvusClient;

    @Autowired
    private EmbeddingService embeddingService;

    @Value("${milvus.collection-name:violation_cases}")
    private String collectionName;

    @Value("${milvus.dimension:768}")
    private int dimension;

    /**
     * 搜索相似案例
     *
     * 注意：当前实现返回空列表，实际搜索需通过Milvus CLI执行
     */
    public List<SearchResult> searchSimilarCases(String text, int topK) {
        try {
            List<Float> vector = embeddingService.embedText(text);
            log.debug("文本向量化完成: dimension={}, collection={}", vector.size(), collectionName);

            // 尝试执行搜索
            try {
                SearchParam param = SearchParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withTopK(topK)
                    .withVectors(Collections.singletonList(vector))
                    .build();

                var response = milvusClient.search(param);

                if (response.getStatus() == 0 && response.getData() != null) {
                    return parseSearchResults(response.getData());
                }
            } catch (Exception e) {
                log.warn("Milvus搜索执行失败: {}", e.getMessage());
            }

            return Collections.emptyList();

        } catch (Exception e) {
            log.error("向量检索异常: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private List<SearchResult> parseSearchResults(Object searchResults) {
        // 简化实现 - 由于SDK API变化频繁，暂不解析结果
        // 实际使用时可通过Milvus CLI或Attu查询
        return Collections.emptyList();
    }

    /**
     * 插入向量到Milvus
     */
    public boolean insertVector(Long caseId, String text) {
        try {
            List<Float> vector = embeddingService.embedText(text);
            log.info("生成向量完成: caseId={}, dimension={}", caseId, vector.size());

            try {
                List<InsertParam.Field> fields = new ArrayList<>();
                fields.add(new InsertParam.Field("id", Collections.singletonList(caseId)));
                fields.add(new InsertParam.Field("embedding", Collections.singletonList(vector)));

                InsertParam param = InsertParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withFields(fields)
                    .build();

                milvusClient.insert(param);
                log.info("向量插入Milvus成功: caseId={}", caseId);
                return true;
            } catch (Exception e) {
                log.warn("Milvus插入失败: {}", e.getMessage());
                return false;
            }

        } catch (Exception e) {
            log.error("向量生成异常: caseId={}", e.getMessage());
            return false;
        }
    }

    /**
     * 批量插入向量
     */
    public int insertVectorsBatch(List<Long> caseIds, List<String> texts) {
        if (caseIds.size() != texts.size()) {
            throw new IllegalArgumentException("caseIds和texts数量必须一致");
        }

        int successCount = 0;
        for (int i = 0; i < caseIds.size(); i++) {
            if (insertVector(caseIds.get(i), texts.get(i))) {
                successCount++;
            }
        }

        return successCount;
    }

    @Data
    public static class SearchResult {
        private Long embeddingId;
        private Double similarityScore;
    }
}