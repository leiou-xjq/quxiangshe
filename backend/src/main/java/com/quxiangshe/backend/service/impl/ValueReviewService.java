package com.quxiangshe.backend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quxiangshe.backend.entity.ViolationCaseLibrary;
import com.quxiangshe.backend.service.IViolationCaseLibraryService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 基于社会主义核心价值观的内容审核服务
 * 
 * 核心职责：通过LLM（豆包）对用户笔记进行价值观 + 论语五常 + 毒鸡汤全方位审核
 * 业务模块：审核模块（AI审核层）
 * 
 * 审核标准（三层维度）：
 *   1. 社会主义核心价值观24字审核：富强/民主/文明/和谐（国家层面）、
 *      自由/平等/公正/法治（社会层面）、爱国/敬业/诚信/友善（个人层面）
 *   2. 论语五常（仁义礼智信）审核：识别扭曲传统价值观的"毒鸡汤"
 *   3. 毒鸡汤识别：绝对化表述、错误价值观、消极厌世、制造焦虑、性别对立等
 * 
 * 审核结果：NORMAL(通过) / VIOLATION(违规) / SUSPICIOUS(疑似违规)
 * 
 * 容错机制：LLM调用失败时重试3次（指数退避），全部失败后降级为SUSPICIOUS需人工复核
 * 
 * @author 趣享社技术团队
 */
@Slf4j
@Service
public class ValueReviewService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 社会主义核心价值观24字
     */
    private static final Map<String, String> CORE_VALUES = new HashMap<String, String>() {{
        // 国家层面
        put("富强", "内容是否有助于国家发展、人民幸福");
        put("民主", "内容是否尊重人民意愿、倡导民主参与");
        put("文明", "内容是否符合社会文明规范、倡导文明新风");
        put("和谐", "内容是否促进社会和谐、人际关系融洽");
        // 社会层面
        put("自由", "内容是否在法律框架内保障合理自由");
        put("平等", "内容是否尊重人人平等、不歧视");
        put("公正", "内容是否公平正义、不偏不倚");
        put("法治", "内容是否遵守法律法规");
        // 个人层面
        put("爱国", "内容是否维护国家形象、不损害国家利益");
        put("敬业", "内容是否倡导认真负责的工作态度");
        put("诚信", "内容是否诚实守信、不欺诈");
        put("友善", "内容是否友善待人、不攻击他人");
    }};

    @Value("${review.doubao.api-key:}")
    private String apiKey;

    @Value("${review.doubao.endpoint:}")
    private String endpoint;

    @Value("${review.doubao.base-url:}")
    private String baseUrl;

    @Value("${review.doubao.model:doubao-pro-32k}")
    private String model;

    @Value("${review.doubao.timeout:30000}")
    private int timeout;

    @Value("${review.value-review-enabled:true}")
    private boolean valueReviewEnabled;

    @Autowired
    private IViolationCaseLibraryService caseLibraryService;

    @Value("${rag.enabled:true}")
    private boolean ragEnabled;

    @Value("${rag.top-k:5}")
    private int ragTopK;

    public ValueReviewService(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    /**
     * 价值观审核结果（内部静态类）
     * 
     * status取值：
     *   - NORMAL：内容符合价值观，审核通过
     *   - SUSPICIOUS：疑似违规，需人工复核
     *   - VIOLATION：确认违规，禁止发布
     */
    @Data
    public static class ValueReviewResult {
        private boolean passed;              // 是否通过审核
        private String status;               // 审核状态：NORMAL/SUSPICIOUS/VIOLATION
        private String reason;               // 违规原因描述
        private List<String> violatedValues; // 触犯的价值观列表（如：爱国、诚信、仁）
        private List<String> tags;           // 违规标签（如：毒鸡汤、性别对立）
        private double confidence;           // 置信度（0~1）

        public static ValueReviewResult pass() {
            ValueReviewResult result = new ValueReviewResult();
            result.setPassed(true);
            result.setStatus("NORMAL");
            result.setConfidence(1.0);
            return result;
        }

        public static ValueReviewResult violation(String reason, List<String> violatedValues, List<String> tags) {
            ValueReviewResult result = new ValueReviewResult();
            result.setPassed(false);
            result.setStatus("VIOLATION");
            result.setReason(reason);
            result.setViolatedValues(violatedValues);
            result.setTags(tags);
            result.setConfidence(1.0);
            return result;
        }

        public static ValueReviewResult suspicious(String reason, List<String> tags) {
            ValueReviewResult result = new ValueReviewResult();
            result.setPassed(false);
            result.setStatus("SUSPICIOUS");
            result.setReason(reason);
            result.setTags(tags);
            result.setConfidence(0.7);
            return result;
        }
    }

    /**
     * 价值观审核提示词
     */
    private static final String VALUE_REVIEW_PROMPT = """
你是一个严格的内容审核专家，负责以社会主义核心价值观和论语仁义礼智信为基础，审核用户发布的笔记内容。

## 一、社会主义核心价值观24字审核

### 国家层面（富强、民主、文明、和谐）
- 富强：内容是否有助于国家发展、人民幸福，不损害国家经济安全
- 民主：内容是否尊重人民意愿、倡导民主参与，不煽动对立
- 文明：内容是否符合社会文明规范、倡导文明新风，不低俗恶俗
- 和谐：内容是否促进社会和谐、人际关系融洽，不挑起纷争

### 社会层面（自由、平等、公正、法治）
- 自由：内容是否在法律框架内保障合理自由，不传播违法内容
- 平等：内容是否尊重人人平等、不歧视，不包含种族/性别/地域歧视
- 公正：内容是否公平正义、不偏不倚，不歪曲事实造谣传谣
- 法治：内容是否遵守法律法规，不传授违法犯罪方法

### 个人层面（爱国、敬业、诚信、友善）
- 爱国：内容是否维护国家形象、不损害国家利益，不做有损国格的事
- 敬业：内容是否倡导认真负责的工作态度，不传播消极怠工
- 诚信：内容是否诚实守信、不欺诈，不虚假宣传坑蒙拐骗
- 友善：内容是否友善待人、不攻击他人，不网络暴力

## 二、论语五常（仁义礼智信）审核

### 1. 仁（仁爱、善良、同情心）
- 审核内容是否真正体现仁爱之心
- 识别扭曲"善良"毒鸡汤，如"善良的人活该被欺负"、"老实人注定吃亏"
- 识别破坏人际关系和谐的言论

### 2. 义（道义、正义、责任感）
- 审核内容是否符合道义，不违背社会公德
- 识别违背道义的毒鸡汤，如"自私是人的本性"、"人不为己天诛地灭"
- 识别逃避责任、扭曲"正义"的言论

### 3. 礼（礼仪、尊重、秩序）
- 审核内容是否尊重他人、遵守社会礼仪
- 识别破坏礼仪的毒鸡汤，如"不尊重长辈是独立"、"没教养是直率"
- 识别破坏社会秩序的言论

### 4. 智（智慧、明辨是非）
- 审核内容是否传播正确知识、倡导学习
- 识别反智毒鸡汤，如"读书无用论"、"大学生给小学毕业的打工"
- 识别愚昧无知、误导他人的言论

### 5. 信（诚信、信用、信任）
- 审核内容是否诚实守信、言行一致
- 识别违背诚信的毒鸡汤，如"欺骗是情商高"、"会撒谎才能成功"
- 识别虚假宣传、欺骗他人的言论

## 三、毒鸡汤识别特征

以下内容必须判定为【违规】-毒鸡汤：

### 1. 扭曲论语原意
- 断章取义引用古语
- 曲解传统文化为毒鸡汤

### 2. 绝对化表述
- "人这一生必须..."
- "所有男人都..."
- "女人一定要..."

### 3. 错误价值观
- "有钱就是成功"
- "善良就是傻"
- "自私才是明智"

### 4. 消极厌世
- "努力没用"
- "社会就是这样"
- "认命吧"

### 5. 制造焦虑
- "30岁之前必须结婚"
- "没房没车就是失败"
- "月薪低于X万就是废柴"

### 6. 伪逻辑错误因果
- "因为是女孩，所以..."
- "因为穷，所以..."

### 7. 性别对立
- 物化女性/男性
- 制造性别矛盾

### 8. 扭曲亲情
- "父母欠你的"
- "孝顺是道德绑架"

### 9. 错误婚恋观念（延续原有）
- 高价彩礼、物质至上、拜金主义
- 买卖婚姻、胁迫婚姻

### 10. 误导性人生建议
- 尤其对青少年有害的观点
- 传播错误人生观

## 四、图片内容审核

请逐张审核提供的图片，判断是否包含以下违规内容：
1. 色情、裸体、低俗、不雅内容
2. 暴力、血腥、恐怖、残忍内容
3. 违法信息（毒品、赌博、诈骗、传销）
4. 政治敏感符号、旗帜、标志
5. 歧视性、侮辱性内容
6. 违反社会主义核心价值观的任何不当内容

## 五、待审核内容分析要求

请对以下内容进行详细分析：

### 标题分析
- 观点是否偏激
- 是否制造焦虑
- 是否以偏概全

### 内容分析
- 论点是否有依据
- 是否有正确的数据或事实支撑
- 是否以偏概全、过度概括

### 作者观点分析
- 三观是否正确
- 是否有误导性
- 是否符合社会主义核心价值观
- 是否符合论语五常

## 六、违规判定标准
以下内容必须判定为【违规】：
1. 政治敏感：危害国家安全、损害国家形象、歪曲历史
2. 色情低俗：色情内容、低俗信息、暴露隐私
3. 暴力血腥：暴力行为、血腥内容、自杀自残
4. 诈骗广告：诈骗钱财、虚假宣传、诱导消费
5. 违法犯罪：违法犯罪指南、违规内容、作弊技巧
6. 歧视仇恨：种族歧视、地域歧视、性别歧视、网络暴力
7. 错误婚恋观念：高价彩礼、物质至上、拜金主义、买卖婚姻、胁迫婚姻
8. 扭曲亲情：否定父母养育之恩、鼓吹原生家庭伤害
9. 毒鸡汤：符合上述毒鸡汤识别特征的内容
10. 图片违规：图片包含色情、暴力、政治敏感等不良内容

## 七、判定结果格式
请严格按照以下JSON格式返回审核结果：
```json
{
    "status": "NORMAL/SUSPICIOUS/VIOLATION",
    "analysis": {
        "core_values": "社会主义核心价值观分析",
        "lunyu_analysis": "论语五常角度分析",
        "toxic_soup": "是否是毒鸡汤及原因（如是）",
        "title_analysis": "标题分析",
        "content_analysis": "内容分析",
        "viewpoint_analysis": "作者观点分析",
        "image_analysis": "图片审核结果（如有图片）"
    },
    "reason": "具体违规原因（如果违规）",
    "violated_values": ["触犯的价值观如：爱国、诚信、仁"],
    "tags": ["毒鸡汤", "误导性", "消极", "图片违规"]
}
```

## 待审核内容
标题：%s
内容：%s

请给出审核结果：""";

    /**
     * 构建带RAG相似案例的审核Prompt
     *
     * 将RAG检索到的相似案例注入到Prompt中，
     * 帮助LLM更准确地判断内容是否违规
     *
     * @param title 笔记标题
     * @param content 笔记内容
     * @param ragCasesPrompt RAG相似案例文本
     * @return 完整Prompt
     */
    private String buildPromptWithRag(String title, String content, String ragCasesPrompt) {
        if (ragCasesPrompt == null || ragCasesPrompt.isEmpty()) {
            // 无RAG案例时使用原始Prompt
            return String.format(VALUE_REVIEW_PROMPT,
                title != null ? title : "",
                content != null ? content : "");
        }

        // 在原始Prompt基础上追加RAG案例
        String basePrompt = String.format(VALUE_REVIEW_PROMPT,
            title != null ? title : "",
            content != null ? content : "");

        return basePrompt + ragCasesPrompt;
    }

    /**
     * 价值观审核（调用LLM，不含图片）
     * 
     * @param title 笔记标题
     * @param content 笔记内容
     * @param similarCases 相似违规案例（可选，辅助LLM判定）
     * @return 审核结果
     */
    public ValueReviewResult review(String title, String content, String similarCases) {
        return review(title, content, similarCases, null);
    }

    /**
     * 价值观审核（调用LLM，支持图片多模态审核）
     * 
     * 流程：
     *   1. 检查审核开关是否启用，未启用直接通过
     *   2. 构建审核Prompt（含社会主义核心价值观、论语五常、毒鸡汤识别规则）
     *   3. 调用豆包LLM API（带重试机制，指数退避）
     *   4. 解析LLM返回的JSON格式审核结果
     *   5. 3次全部失败后降级为SUSPICIOUS（需人工复核）
     * 
     * @param title 笔记标题
     * @param content 笔记内容
     * @param similarCases 相似违规案例（可选）
     * @param imageUrls 图片URL列表（可选，触发多模态审核）
     * @return 审核结果
     */
    public ValueReviewResult review(String title, String content, String similarCases, List<String> imageUrls) {
        // 审核开关关闭时直接放行
        if (!valueReviewEnabled) {
            log.info("价值观审核未启用，直接通过");
            return ValueReviewResult.pass();
        }

        log.info("开始价值观审核: title={}, images={}", title, imageUrls != null ? imageUrls.size() : 0);

        // ===== RAG Layer 2: 检索相似违规案例 =====
        String ragCasesPrompt = "";
        if (ragEnabled) {
            try {
                String combinedText = ((title != null) ? title : "") + " " + ((content != null) ? content : "");
                List<ViolationCaseLibrary> similarCasesList = caseLibraryService.searchSimilar(combinedText, ragTopK);
                if (similarCasesList != null && !similarCasesList.isEmpty()) {
                    ragCasesPrompt = caseLibraryService.formatCasesForPrompt(similarCasesList);
                    log.info("RAG检索相似案例: count={}", similarCasesList.size());
                }
            } catch (Exception e) {
                log.warn("RAG检索失败，将跳过案例参考: {}", e.getMessage());
            }
        }
        // ===== RAG检索结束 =====

        // 构建审核Prompt（包含RAG相似案例）
        String prompt = buildPromptWithRag(title, content, ragCasesPrompt);

        // 重试机制：最多3次，指数退避（1s, 2s, 4s）
        int maxRetries = 3;
        int baseDelayMs = 1000;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String response = callDoubao(prompt, imageUrls);
                ValueReviewResult result = parseResponse(response);

                log.info("价值观审核完成: status={}, violatedValues={}",
                    result.getStatus(), result.getViolatedValues());
                return result;

            } catch (Exception e) {
                log.warn("价值观审核第{}次失败: {}", attempt, e.getMessage());

                // 达到最大重试次数，降级返回SUSPICIOUS
                if (attempt == maxRetries) {
                    log.error("价值观审核全部重试失败: {}", e.getMessage());
                    return ValueReviewResult.suspicious("AI审核失败，需人工复核", Arrays.asList("审核失败"));
                }

                // 指数退避等待
                try {
                    int delayMs = baseDelayMs * (int) Math.pow(2, attempt - 1);
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // 兜底：重试被中断后返回SUSPICIOUS
        return ValueReviewResult.suspicious("AI审核失败，需人工复核", Arrays.asList("审核失败"));
    }

    /**
     * 调用豆包LLM
     */
    private String callDoubao(String prompt) {
        return callDoubao(prompt, null);
    }

    /**
     * 调用豆包LLM API
     * 
     * @param prompt 审核提示词
     * @param imageUrls 图片URL列表（多模态模式下传入）
     * @return LLM原始响应文本
     * @throws RuntimeException API调用或配置异常
     */
    private String callDoubao(String prompt, List<String> imageUrls) {
        // 配置校验（apiKey和baseUrl缺失时快速失败）
        if (apiKey == null || apiKey.isEmpty() || baseUrl == null || baseUrl.isEmpty()) {
            throw new RuntimeException("豆包API配置未正确设置");
        }

        String url = baseUrl + "/chat/completions";

        log.info("调用豆包API - model: {}, url: {}, images: {}",
            model, url, imageUrls != null ? imageUrls.size() : 0);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("max_tokens", 1024);
        requestBody.put("temperature", 0.1);  // 低温度保证审核结果一致性

        List<Map<String, Object>> messages = new ArrayList<>();

        // 有图片时使用多模态消息格式（text + image_url）
        if (imageUrls != null && !imageUrls.isEmpty()) {
            List<Map<String, Object>> contentParts = new ArrayList<>();
            contentParts.add(Map.of("type", "text", "text", prompt));
            for (String imageUrl : imageUrls) {
                contentParts.add(Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", imageUrl)
                ));
            }
            messages.add(Map.of("role", "user", "content", contentParts));
        } else {
            // 纯文本模式
            messages.add(Map.of("role", "user", "content", prompt));
        }

        requestBody.put("messages", messages);

        // 构建HTTP请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("豆包API调用失败: " + response.getStatusCode());
            }

            // 解析OpenAI格式响应：choices[0].message.content
            Map<String, Object> respMap = objectMapper.readValue(response.getBody(), Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) respMap.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");
            }

            throw new RuntimeException("豆包API响应格式异常");

        } catch (Exception e) {
            throw new RuntimeException("豆包API调用异常: " + e.getMessage());
        }
    }

    /**
     * 解析LLM响应，提取JSON格式的审核结果
     * 
     * 如果LLM返回非JSON文本，尝试从```json```代码块或直接大括号中提取
     * 解析失败时降级为SUSPICIOUS
     * 
     * @param response LLM原始响应
     * @return 结构化的审核结果
     */
    private ValueReviewResult parseResponse(String response) {
        try {
            // 从响应中提取JSON部分（兼容LLM可能输出的markdown格式）
            String jsonStr = extractJson(response);
            if (jsonStr == null) {
                log.warn("无法从响应中提取JSON: {}", response);
                return ValueReviewResult.suspicious("响应解析失败", Arrays.asList("解析异常"));
            }

            Map<String, Object> resultMap = objectMapper.readValue(jsonStr, Map.class);
            String status = (String) resultMap.get("status");
            String reason = (String) resultMap.get("reason");

            @SuppressWarnings("unchecked")
            List<String> violatedValues = (List<String>) resultMap.get("violated_values");
            @SuppressWarnings("unchecked")
            List<String> tags = (List<String>) resultMap.get("tags");

            // 根据status字段分发结果
            if ("NORMAL".equalsIgnoreCase(status)) {
                return ValueReviewResult.pass();
            } else if ("VIOLATION".equalsIgnoreCase(status)) {
                return ValueReviewResult.violation(reason, violatedValues, tags);
            } else {
                return ValueReviewResult.suspicious(reason, tags);
            }

        } catch (Exception e) {
            log.error("解析LLM响应失败: {}", e.getMessage());
            return ValueReviewResult.suspicious("响应解析失败", Arrays.asList("解析异常"));
        }
    }

    /**
     * 从LLM响应文本中提取JSON字符串
     * 
     * 策略：
     *   1. 优先查找```json```标记块
     *   2. 其次匹配第一个{到最后一个}之间的内容
     *   3. 都未找到返回null
     * 
     * @param response 原始响应文本
     * @return JSON字符串（null表示提取失败）
     */
    private String extractJson(String response) {
        if (response == null) return null;

        // 策略1：查找```json...```代码块
        int jsonStart = response.indexOf("```json");
        if (jsonStart >= 0) {
            int jsonEnd = response.indexOf("```", jsonStart + 7);
            if (jsonEnd > jsonStart) {
                return response.substring(jsonStart + 7, jsonEnd).trim();
            }
        }

        // 策略2：查找首个{和最后一个}之间的文本
        jsonStart = response.indexOf("{");
        if (jsonStart >= 0) {
            int jsonEnd = response.lastIndexOf("}");
            if (jsonEnd > jsonStart) {
                return response.substring(jsonStart, jsonEnd + 1);
            }
        }

        return null;
    }
}