package com.quxiangshe.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Embedding向量生成服务
 *
 * 核心职责：调用豆包embedding API将文本转换为768维向量
 * 业务模块：审核模块（RAG Layer 2 - 向量化）
 *
 * 技术细节：
 *   - API格式：POST {endpoint}/embeddings
 *   - Request: { "model": "doubao-embedding-v1", "input": "文本内容" }
 *   - Response: { "data": [{ "embedding": [0.1, 0.2, ...] }] }
 *   - 向量维度：768维
 *
 * @author 趣享社技术团队
 */
@Slf4j
@Service
public class EmbeddingService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${embedding.doubao.endpoint:}")
    private String endpoint;

    @Value("${embedding.doubao.api-key:}")
    private String apiKey;

    @Value("${embedding.doubao.model:doubao-embedding-v1}")
    private String model;

    @Value("${embedding.doubao.timeout:10}")
    private int timeout;

    public EmbeddingService(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    /**
     * 将文本转换为768维向量
     *
     * 流程：
     *   1. 配置校验（endpoint和apiKey不能为空）
     *   2. 构建HTTP请求（POST /v3/embeddings）
     *   3. 调用豆包embedding API
     *   4. 解析返回的embedding向量
     *   5. 转为List<Float>返回
     *
     * @param text 待向量化的文本（通常为标题+内容拼接）
     * @return 768维向量（List<Float>）
     * @throws RuntimeException API调用失败时抛出
     */
    public List<Float> embedText(String text) {
        if (text == null || text.trim().isEmpty()) {
            log.warn("文本为空，返回零向量");
            return Collections.nCopies(768, 0.0f);
        }

        if (apiKey == null || apiKey.isEmpty() || endpoint == null || endpoint.isEmpty()) {
            log.warn("Embedding API配置未设置，返回随机向量（测试模式）");
            return generateRandomVector();
        }

        try {
            String url = endpoint + "/embeddings";
            log.debug("调用Embedding API: url={}, textLength={}", url, text.length());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("input", text);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("Embedding API调用失败: " + response.getStatusCode());
            }

            return parseEmbeddingResponse(response.getBody());

        } catch (Exception e) {
            log.error("Embedding API调用异常: {}", e.getMessage(), e);
            throw new RuntimeException("Embedding向量生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析embedding API响应
     *
     * Response格式：
     * {
     *   "data": [
     *     {
     *       "embedding": [0.1, 0.2, ...],
     *       "index": 0
     *     }
     *   ],
     *   "model": "doubao-embedding-v1"
     * }
     *
     * @param responseBody 响应体JSON
     * @return 向量List<Float>
     */
    @SuppressWarnings("unchecked")
    private List<Float> parseEmbeddingResponse(String responseBody) {
        try {
            Map<String, Object> respMap = objectMapper.readValue(responseBody, Map.class);
            List<Map<String, Object>> data = (List<Map<String, Object>>) respMap.get("data");

            if (data == null || data.isEmpty()) {
                throw new RuntimeException("Embedding响应中没有data字段");
            }

            List<Double> embedding = (List<Double>) data.get(0).get("embedding");
            if (embedding == null || embedding.isEmpty()) {
                throw new RuntimeException("Embedding响应中没有embedding字段");
            }

            // Double转Float
            List<Float> result = new ArrayList<>(embedding.size());
            for (Double d : embedding) {
                result.add(d.floatValue());
            }

            log.debug("成功生成向量，维度={}", result.size());
            return result;

        } catch (Exception e) {
            log.error("解析Embedding响应失败: {}", e.getMessage());
            throw new RuntimeException("解析Embedding响应失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成随机向量（配置缺失时的降级方案）
     *
     * 用于测试或配置未完成的降级场景
     *
     * @return 768维随机向量
     */
    private List<Float> generateRandomVector() {
        Random random = new Random();
        List<Float> vector = new ArrayList<>(768);
        for (int i = 0; i < 768; i++) {
            vector.add(random.nextFloat());
        }
        return vector;
    }

    /**
     * 批量文本向量化
     *
     * @param texts 文本列表
     * @return 向量列表（与输入文本顺序对应）
     */
    public List<List<Float>> embedTexts(List<String> texts) {
        List<List<Float>> results = new ArrayList<>(texts.size());
        for (String text : texts) {
            results.add(embedText(text));
        }
        return results;
    }
}