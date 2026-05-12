package com.quxiangshe.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quxiangshe.backend.service.IAiReplySuggestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * AI回复建议服务实现
 * 使用RestTemplate直接调用豆包大模型API
 */
@Slf4j
@Service
public class AiReplySuggestionServiceImpl implements IAiReplySuggestionService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${review.doubao.api-key:}")
    private String apiKey;

    @Value("${review.doubao.base-url:https://ark.cn-beijing.volces.com/api/v3}")
    private String baseUrl;

    @Value("${review.doubao.model:doubao-1-5-lite-32k-250115}")
    private String model;

    @Value("${review.doubao.timeout:30000}")
    private int timeout;

    private static final String CACHE_PREFIX = "ai:reply:suggestion:";
    private static final int CACHE_HOURS = 24;

    private static final String SYSTEM_PROMPT = """
            你是一个社交平台的AI助手，负责为用户的评论生成友好、有趣的回复建议。
            请根据笔记内容和评论上下文，生成3-5条回复建议。

            要求：
            1. 回复要自然、友好，符合社交平台的氛围
            2. 长度控制在20字以内
            3. 语气多样化（感谢、认同、互动、幽默等）
            4. 不要包含特殊符号或表情
            5. 直接输出回复内容，每行一条，不要编号

            输出格式：
            每行一个回复建议，不要其他内容
            """;

    public AiReplySuggestionServiceImpl(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public List<String> generateSuggestions(String noteContent, String comment) {
        try {
            String userContent = String.format("笔记内容：%s\n用户评论：%s\n请生成回复建议：",
                    noteContent != null ? noteContent : "无",
                    comment);

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 1000);

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> systemMsg = new LinkedHashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", SYSTEM_PROMPT);
            messages.add(systemMsg);

            Map<String, String> userMsg = new LinkedHashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userContent);
            messages.add(userMsg);

            requestBody.put("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String url = baseUrl + "/chat/completions";
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode choices = root.path("choices");
                if (choices.isArray() && !choices.isEmpty()) {
                    String content = choices.get(0).path("message").path("content").asText();
                    return parseSuggestions(content);
                }
            }

            log.warn("AI API返回异常: code={}, body={}", response.getStatusCode(), response.getBody());
            return getDefaultSuggestions();

        } catch (Exception e) {
            log.error("生成AI回复建议失败: comment={}", comment, e);
            return getDefaultSuggestions();
        }
    }

    @Override
    public List<String> getSuggestions(Long noteId, Long commentId, String noteContent, String comment) {
        String cacheKey = buildCacheKey(noteId, commentId);

        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("从缓存获取回复建议: noteId={}, commentId={}", noteId, commentId);
                return (List<String>) cached;
            }

            List<String> suggestions = generateSuggestions(noteContent, comment);

            redisTemplate.opsForValue().set(cacheKey, suggestions, CACHE_HOURS, java.util.concurrent.TimeUnit.HOURS);
            log.info("生成并缓存回复建议: noteId={}, commentId={}, count={}", noteId, commentId, suggestions.size());

            return suggestions;

        } catch (Exception e) {
            log.error("获取回复建议失败: noteId={}, commentId={}", noteId, commentId, e);
            return getDefaultSuggestions();
        }
    }

    private List<String> parseSuggestions(String response) {
        List<String> suggestions = new ArrayList<>();

        if (response == null || response.isBlank()) {
            return getDefaultSuggestions();
        }

        String[] lines = response.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.matches("^\\d+[.、].*")) {
                trimmed = trimmed.replaceAll("^[\\-●◆\\s]+", "");
                if (!trimmed.isEmpty() && trimmed.length() <= 20) {
                    suggestions.add(trimmed);
                }
            }
            if (suggestions.size() >= 5) {
                break;
            }
        }

        return suggestions.isEmpty() ? getDefaultSuggestions() : suggestions;
    }

    private String buildCacheKey(Long noteId, Long commentId) {
        return CACHE_PREFIX + noteId + ":" + commentId;
    }

    private List<String> getDefaultSuggestions() {
        List<String> defaults = new ArrayList<>();
        defaults.add("谢谢观看");
        defaults.add("写得真好");
        defaults.add("支持一下");
        return defaults;
    }
}
