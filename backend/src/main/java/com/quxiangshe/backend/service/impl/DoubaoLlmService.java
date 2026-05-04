package com.quxiangshe.backend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 豆包大模型内容审核服务
 * 基于火山引擎方舟平台的豆包模型进行深度内容审核
 * 
 * 审核标准：严格按照社会主义核心价值观24字进行内容审核
 * - 富强、民主、文明、和谐 (国家层面)
 * - 自由、平等、公正、法治 (社会层面)
 * - 爱国、敬业、诚信、友善 (个人层面)
 * 
 * @author 趣享社技术团队
 */
@Slf4j
@Service
public class DoubaoLlmService {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${review.doubao.api-key:}")
    private String apiKey;
    
    @Value("${review.doubao.endpoint:}")
    private String endpoint;
    
    @Value("${review.doubao.base-url:}")
    private String baseUrl;
    
    @Value("${review.doubao.timeout:30000}")
    private int timeout;
    
    /**
     * 审核结果
     */
    @Data
    public static class ReviewResult {
        // 审核状态: NORMAL-正常, SUSPICIOUS-疑似, VIOLATION-违规
        private String status;
        // 违规原因
        private String reason;
        // 违规标签
        private List<String> tags;
        // 置信度
        private double confidence;
        // 原始响应
        private String rawResponse;
        
        public static ReviewResult normal() {
            ReviewResult result = new ReviewResult();
            result.setStatus("NORMAL");
            result.setConfidence(1.0);
            return result;
        }
        
        public static ReviewResult violation(String reason, List<String> tags) {
            ReviewResult result = new ReviewResult();
            result.setStatus("VIOLATION");
            result.setReason(reason);
            result.setTags(tags);
            result.setConfidence(1.0);
            return result;
        }
        
        public static ReviewResult suspicious(String reason, List<String> tags) {
            ReviewResult result = new ReviewResult();
            result.setStatus("SUSPICIOUS");
            result.setReason(reason);
            result.setTags(tags);
            result.setConfidence(0.7);
            return result;
        }
    }
    
    public DoubaoLlmService(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }
    
    /**
     * 价值观审核提示词
     */
    private static final String CONTENT_REVIEW_PROMPT = """
你是一个严格的内容审核专家，负责审核用户发布的笔记内容。

## 审核标准（社会主义核心价值观）
你必须严格按照以下标准进行审核：

### 国家层面
- 富强：内容是否有助于国家发展、人民幸福
- 民主：内容是否尊重人民意愿、倡导民主参与
- 文明：内容是否符合社会文明规范、倡导文明新风
- 和谐：内容是否促进社会和谐、人际关系融洽

### 社会层面
- 自由：内容是否在法律框架内保障合理自由
- 平等：内容是否尊重人人平等、不歧视
- 公正：内容是否公平正义、不偏不倚
- 法治：内容是否遵守法律法规

### 个人层面
- 爱国：内容是否维护国家形象、不损害国家利益
- 敬业：内容是否倡导认真负责的工作态度
- 诚信：内容是否诚实守信、不欺诈
- 友善：内容是否友善待人、不攻击他人

## 违规判定标准
以下内容必须判定为【违规】：

1. **政治敏感** - 危害国家安全、损害国家形象、歪曲历史、抹黑英雄人物
2. **色情低俗** - 色情内容、低俗信息、暴露隐私
3. **暴力血腥** - 暴力行为、血腥内容、自杀自残
4. **诈骗广告** - 诈骗钱财、虚假宣传、诱导消费、传销
5. **违法犯罪** - 违法犯罪指南、违规内容、作弊技巧
6. **种族歧视** - 种族歧视、地域歧视、性别歧视、职业歧视
7. **网络暴力** - 侮辱攻击、网暴他人、恶意诅咒、隐私泄露
8. **三观扭曲** - 错误引导、不良导向、扭曲价值观

## 判定结果格式
请严格按照以下JSON格式返回审核结果，不要输出其他内容：
```json
{
    "status": "NORMAL/SUSPICIOUS/VIOLATION",
    "reason": "具体违规原因（如果违规）",
    "tags": ["标签1", "标签2"]
}
```

## 参考信息（相似违规案例）
以下是历史上类似的违规案例，供你参考判断：
%s

## 待审核内容
标题：%s
内容：%s

请给出审核结果：""";
    
    /**
     * 进行内容审核
     * 带重试机制
     * 
     * @param title 笔记标题
     * @param content 笔记内容
     * @param similarCases 相似违规案例（可选）
     * @return 审核结果
     */
    public ReviewResult review(String title, String content, String similarCases) {
        log.info("开始豆包大模型审核, title: {}", title);
        
        String casesInfo = similarCases != null && !similarCases.isEmpty() 
            ? similarCases 
            : "无相似案例";
        String prompt = String.format(CONTENT_REVIEW_PROMPT, casesInfo, 
            title != null ? title : "", 
            content != null ? content : "");
        
        int maxRetries = 3;
        int baseDelayMs = 1000;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String response = callDoubao(prompt);
                ReviewResult result = parseResponse(response);
                log.info("豆包审核完成, status: {}, reason: {}", result.getStatus(), result.getReason());
                return result;
                
            } catch (Exception e) {
                log.warn("豆包审核第{}次失败: {}", attempt, e.getMessage());
                
                if (attempt == maxRetries) {
                    log.error("豆包审核全部重试失败: {}", e.getMessage(), e);
                    return ReviewResult.suspicious("AI审核失败，需人工复核", Arrays.asList("审核失败"));
                }
                
                try {
                    int delayMs = baseDelayMs * (int) Math.pow(2, attempt - 1);
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        return ReviewResult.suspicious("AI审核失败，需人工复核", Arrays.asList("审核失败"));
    }
    
    /**
     * 调用豆包API
     */
    private String callDoubao(String prompt) {
        String url = baseUrl + "/chat/completions";
        
        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", endpoint);
        
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", prompt));
        requestBody.put("messages", messages);
        
        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                request, 
                String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else {
                throw new RuntimeException("API调用失败, status: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("调用豆包API失败: {}", e.getMessage());
            throw new RuntimeException("调用豆包API失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析API响应
     */
    private ReviewResult parseResponse(String responseBody) {
        try {
            // 解析JSON响应
            Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
            
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                return ReviewResult.suspicious("API响应为空", Collections.emptyList());
            }
            
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");
            
            // 提取JSON部分
            String jsonStr = extractJson(content);
            
            Map<String, Object> result = objectMapper.readValue(jsonStr, Map.class);
            
            String status = (String) result.get("status");
            String reason = (String) result.get("reason");
            List<String> tags = result.get("tags") != null 
                ? (List<String>) result.get("tags") 
                : Collections.emptyList();
            
            ReviewResult reviewResult = new ReviewResult();
            reviewResult.setStatus(status);
            reviewResult.setReason(reason);
            reviewResult.setTags(tags);
            reviewResult.setRawResponse(content);
            
            return reviewResult;
            
        } catch (Exception e) {
            log.error("解析响应失败: {}", e.getMessage());
            // 解析失败，返回疑似
            return ReviewResult.suspicious("解析AI响应失败，需人工复核", Arrays.asList("解析失败"));
        }
    }
    
    /**
     * 从文本中提取JSON
     */
    private String extractJson(String text) {
        if (text == null || text.isEmpty()) {
            return "{}";
        }
        
        // 尝试找JSON块
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        
        // 如果没有找到JSON块，返回原文本（可能有帮助的提示）
        return "{\"status\":\"SUSPICIOUS\",\"reason\":\"AI响应格式异常\",\"tags\":[\"需人工审核\"]}";
    }
    
    /**
     * 简化的同步审核（无RAG案例）
     */
    public ReviewResult review(String title, String content) {
        return review(title, content, null);
    }
}