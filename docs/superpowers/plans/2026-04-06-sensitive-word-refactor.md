# 敏感词检测重构实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 基于Redis实现DFA敏感词检测，使用AOP统一处理，重构敏感词校验逻辑

**Architecture:** 
- 使用DFA状态机进行高效敏感词匹配（内存加载）
- Redis存储敏感词库，支持变体词识别
- AOP拦截器统一处理敏感词检测，不侵入业务代码

**Tech Stack:** Spring Boot 3, Redis, Spring AOP, DFA算法

---

## 文件结构

```
backend/src/main/java/com/quxiangshe/backend/
├── util/
│   ├── DFAStateMachine.java          # DFA状态机核心实现
│   └── SensitiveWordCache.java       # Redis敏感词库管理
├── aspect/
│   └── SensitiveWordAspect.java      # AOP统一拦截发布/评论
├── exception/
│   └── SensitiveWordException.java   # 敏感词违规异常
├── service/
│   ├── ISensitiveWordService.java    # 修改：简化接口
│   └── impl/SensitiveWordServiceImpl.java  # 修改：调用DFA
└── controller/
    └── NoteController.java            # 修改：移除侵入代码
```

---

## Task 1: 创建敏感词违规异常类

**Files:**
- Create: `backend/src/main/java/com/quxiangshe/backend/exception/SensitiveWordException.java`

- [ ] **Step 1: 创建异常类**

```java
package com.quxiangshe.backend.exception;

import lombok.Getter;

/**
 * 敏感词违规异常
 * 用于敏感词检测不通过时抛出
 * 
 * @author 趣享社技术团队
 */
@Getter
public class SensitiveWordException extends RuntimeException {
    
    /** 敏感词列表 */
    private final List<String> words;
    
    /** 风险等级：1-低风险，2-中风险，3-高风险 */
    private final int level;
    
    /** 提示消息 */
    private final String message;
    
    public SensitiveWordException(int level, List<String> words, String message) {
        super(message);
        this.level = level;
        this.words = words;
        this.message = message;
    }
    
    public SensitiveWordException(int level, String message) {
        super(message);
        this.level = level;
        this.words = List.of();
        this.message = message;
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd backend && mvn compile -q
```

---

## Task 2: 创建Redis敏感词库管理类

**Files:**
- Create: `backend/src/main/java/com/quxiangshe/backend/util/SensitiveWordCache.java`

- [ ] **Step 1: 创建敏感词缓存类**

```java
package com.quxiangshe.backend.util;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis敏感词库管理
 * 负责从Redis加载敏感词到DFA状态机
 * 
 * @author 趣享社技术团队
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SensitiveWordCache {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    /** 敏感词库Key */
    private static final String WORDS_LIBRARY_KEY = "sensitive:words:library";
    
    /** 变体词Key前缀 */
    private static final String VARIANTS_KEY = "sensitive:words:variants:";
    
    /** 敏感词等级 */
    public static final int LEVEL_HIGH = 3;
    public static final int LEVEL_MEDIUM = 2;
    public static final int LEVEL_LOW = 1;
    
    /** 默认敏感词库（用于初始化） */
    private static final Map<String, Integer> DEFAULT_WORDS = new HashMap<>();
    static {
        DEFAULT_WORDS.put("色情", LEVEL_HIGH);
        DEFAULT_WORDS.put("赌博", LEVEL_HIGH);
        DEFAULT_WORDS.put("毒品", LEVEL_HIGH);
        DEFAULT_WORDS.put("政治", LEVEL_HIGH);
        DEFAULT_WORDS.put("诈骗", LEVEL_HIGH);
        DEFAULT_WORDS.put("暴力", LEVEL_MEDIUM);
        DEFAULT_WORDS.put("广告", LEVEL_MEDIUM);
        DEFAULT_WORDS.put("垃圾", LEVEL_LOW);
    }
    
    /**
     * 初始化敏感词库
     */
    @PostConstruct
    public void init() {
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(WORDS_LIBRARY_KEY))) {
            redisTemplate.opsForHash().putAll(WORDS_LIBRARY_KEY, DEFAULT_WORDS);
            log.info("敏感词库初始化完成，共加载 {} 个词汇", DEFAULT_WORDS.size());
        }
    }
    
    /**
     * 获取所有敏感词（按等级分组）
     */
    public Map<Integer, Set<String>> getAllWordsGroupedByLevel() {
        Map<Integer, Set<String>> result = new HashMap<>();
        result.put(LEVEL_HIGH, new HashSet<>());
        result.put(LEVEL_MEDIUM, new HashSet<>());
        result.put(LEVEL_LOW, new HashSet<>());
        
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(WORDS_LIBRARY_KEY);
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            String word = entry.getKey().toString();
            int level = Integer.parseInt(entry.getValue().toString());
            result.computeIfAbsent(level, k -> new HashSet<>()).add(word);
        }
        
        return result;
    }
    
    /**
     * 获取所有敏感词列表
     */
    public List<String> getAllWords() {
        List<String> words = new ArrayList<>();
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(WORDS_LIBRARY_KEY);
        entries.forEach((k, v) -> words.add(k.toString()));
        return words;
    }
    
    /**
     * 获取指定等级的敏感词
     */
    public Set<String> getWordsByLevel(int level) {
        Set<String> words = new HashSet<>();
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(WORDS_LIBRARY_KEY);
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            if (Integer.parseInt(entry.getValue().toString()) == level) {
                words.add(entry.getKey().toString());
            }
        }
        return words;
    }
    
    /**
     * 添加敏感词
     */
    public void addWord(String word, int level) {
        redisTemplate.opsForHash().put(WORDS_LIBRARY_KEY, word, level);
        log.info("添加敏感词: word={}, level={}", word, level);
    }
    
    /**
     * 删除敏感词
     */
    public void removeWord(String word) {
        redisTemplate.opsForHash().delete(WORDS_LIBRARY_KEY, word);
        log.info("删除敏感词: word={}", word);
    }
    
    /**
     * 获取变体词列表
     */
    public Set<String> getVariants(String word) {
        Set<Object> result = redisTemplate.opsForSet().members(VARIANTS_KEY + word);
        if (result == null) {
            return Collections.emptySet();
        }
        Set<String> variants = new HashSet<>();
        result.forEach(v -> variants.add(v.toString()));
        return variants;
    }
    
    /**
     * 添加变体词
     */
    public void addVariant(String word, String variant) {
        redisTemplate.opsForSet().add(VARIANTS_KEY + word, variant);
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd backend && mvn compile -q
```

---

## Task 3: 创建DFA状态机核心实现类

**Files:**
- Create: `backend/src/main/java/com/quxiangshe/backend/util/DFAStateMachine.java`

- [ ] **Step 1: 创建DFA状态机类**

```java
package com.quxiangshe.backend.util;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * DFA状态机敏感词匹配器
 * 使用Deterministic Finite Automaton算法进行高效敏感词匹配
 * 支持变体词、谐音词、特殊符号分隔词识别
 * 
 * @author 趣享社技术团队
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DFAStateMachine {
    
    private final SensitiveWordCache cache;
    
    /** 状态机根节点 */
    private final Map<Character, Map<String, Object>> root = new HashMap<>();
    
    /** 变体词字符替换映射 */
    private static final Map<Character, Character> VARIANT_CHARS = new HashMap<>();

    static {
        // 数字变体
        VARIANT_CHARS.put('0', 'o');
        VARIANT_CHARS.put('1', 'i');
        VARIANT_CHARS.put('2', 'z');
        VARIANT_CHARS.put('3', 'e');
        VARIANT_CHARS.put('4', 'a');
        VARIANT_CHARS.put('5', 's');
        VARIANT_CHARS.put('6', 'g');
        VARIANT_CHARS.put('7', 't');
        VARIANT_CHARS.put('8', 'b');
        VARIANT_CHARS.put('9', 'q');
        // 字母变体
        VARIANT_CHARS.put('o', '0');
        VARIANT_CHARS.put('i', '1');
        VARIANT_CHARS.put('z', '2');
        VARIANT_CHARS.put('e', '3');
        VARIANT_CHARS.put('a', '4');
        VARIANT_CHARS.put('s', '5');
        VARIANT_CHARS.put('g', '6');
        VARIANT_CHARS.put('t', '7');
        VARIANT_CHARS.put('b', '8');
        VARIANT_CHARS.put('q', '9');
    }
    
    /** 特殊符号 */
    private static final Set<Character> SPECIAL_CHARS = new HashSet<>();
    static {
        SPECIAL_CHARS.add(' ');
        SPECIAL_CHARS.add('-');
        SPECIAL_CHARS.add('_');
        SPECIAL_CHARS.add('.');
        SPECIAL_CHARS.add('*');
        SPECIAL_CHARS.add('#');
        SPECIAL_CHARS.add('@');
        SPECIAL_CHARS.add('!');
        SPECIAL_CHARS.add('~');
    }
    
    /**
     * 初始化DFA状态机
     * 从Redis加载敏感词构建状态机
     */
    @PostConstruct
    public void init() {
        buildStateMachine();
    }
    
    /**
     * 构建DFA状态机
     */
    private void buildStateMachine() {
        Map<Integer, Set<String>> wordsByLevel = cache.getAllWordsGroupedByLevel();
        
        // 处理各等级敏感词
        for (Map.Entry<Integer, Set<String>> entry : wordsByLevel.entrySet()) {
            int level = entry.getKey();
            for (String word : entry.getValue()) {
                addWordToStateMachine(word, level);
                // 添加变体词
                addVariantWords(word, level);
            }
        }
        
        log.info("DFA状态机初始化完成，共加载 {} 个敏感词", getWordCount());
    }
    
    /**
     * 添加敏感词到状态机
     */
    private void addWordToStateMachine(String word, int level) {
        Map<Character, Map<String, Object>> current = root;
        
        for (char c : word.toCharArray()) {
            c = normalizeChar(c);
            current.computeIfAbsent(c, k -> new HashMap<String, Object>());
            current = (Map<Character, Map<String, Object>>) current.get(c);
        }
        
        // 设置结束标记和等级
        current.put("isEnd", true);
        current.put("level", level);
    }
    
    /**
     * 添加变体词
     */
    private void addVariantWords(String word, int level) {
        // 添加特殊符号分隔变体
        addWordToStateMachine(word, level);
        
        // 在每个位置插入特殊字符的变体
        for (int i = 1; i < word.length() - 1; i++) {
            StringBuilder sb = new StringBuilder(word);
            sb.insert(i, ' ');
            addWordToStateMachine(sb.toString(), level);
        }
        
        // 数字/字母替换变体
        char[] chars = word.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char original = chars[i];
            Character variant = VARIANT_CHARS.get(original);
            if (variant != null) {
                chars[i] = variant;
                addWordToStateMachine(new String(chars), level);
                chars[i] = original;
            }
        }
    }
    
    /**
     * 标准化字符
     */
    private char normalizeChar(char c) {
        // 转换为小写
        if (c >= 'A' && c <= 'Z') {
            c = (char) (c + 32);
        }
        return c;
    }
    
    /**
     * 检测文本中的敏感词
     */
    public DetectResult detect(String text) {
        if (text == null || text.isEmpty()) {
            return new DetectResult(false, 0, new ArrayList<>());
        }
        
        List<MatchWord> matchedWords = new ArrayList<>();
        int maxLevel = 0;
        
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            char normalized = normalizeChar(c);
            sb.append(normalized);
        }
        
        String normalizedText = sb.toString();
        
        // 遍历文本进行匹配
        for (int i = 0; i < normalizedText.length(); i++) {
            Map<Character, Map<String, Object>> current = root;
            int matchEnd = -1;
            int matchLevel = 0;
            StringBuilder matchWord = new StringBuilder();
            
            for (int j = i; j < normalizedText.length(); j++) {
                char c = normalizedText.charAt(j);
                
                // 跳过特殊字符
                if (SPECIAL_CHARS.contains(c)) {
                    continue;
                }
                
                if (!current.containsKey(c)) {
                    break;
                }
                
                matchWord.append(c);
                Map<String, Object> node = current.get(c);
                current = (Map<Character, Map<String, Object>>) node.get(c);
                
                if (Boolean.TRUE.equals(node.get("isEnd"))) {
                    matchEnd = j;
                    matchLevel = Math.max(matchLevel, (Integer) node.getOrDefault("level", 1));
                }
            }
            
            if (matchEnd >= i) {
                matchedWords.add(new MatchWord(matchWord.toString(), matchLevel, i, matchEnd));
                maxLevel = Math.max(maxLevel, matchLevel);
            }
        }
        
        return new DetectResult(maxLevel > 0, maxLevel, matchedWords);
    }
    
    /**
     * 检测标题（只检测高风险）
     */
    public boolean containsHighRisk(String text) {
        DetectResult result = detect(text);
        return result.isContainsSensitive() && result.getMaxLevel() >= SensitiveWordCache.LEVEL_HIGH;
    }
    
    /**
     * 获取敏感词数量
     */
    private int getWordCount() {
        return cache.getAllWords().size();
    }
    
    /**
     * 刷新状态机（当敏感词库更新时调用）
     */
    public void refresh() {
        root.clear();
        buildStateMachine();
        log.info("DFA状态机已刷新");
    }
    
    /**
     * 检测结果
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class DetectResult {
        private boolean containsSensitive;
        private int maxLevel;
        private List<MatchWord> matchedWords;
    }
    
    /**
     * 匹配的敏感词
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class MatchWord {
        private String word;
        private int level;
        private int startIndex;
        private int endIndex;
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd backend && mvn compile -q
```

---

## Task 4: 创建AOP统一拦截器

**Files:**
- Create: `backend/src/main/java/com/quxiangshe/backend/aspect/SensitiveWordAspect.java`

- [ ] **Step 1: 创建AOP拦截器**

```java
package com.quxiangshe.backend.aspect;

import com.quxiangshe.backend.exception.SensitiveWordException;
import com.quxiangshe.backend.util.DFAStateMachine;
import com.quxiangshe.backend.util.SensitiveWordCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 敏感词检测AOP拦截器
 * 统一处理笔记发布、评论等敏感词检测
 * 不侵入业务代码
 * 
 * @author 趣享社技术团队
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class SensitiveWordAspect {
    
    private final DFAStateMachine dfaStateMachine;
    
    /**
     * 定义切点：笔记发布接口
     */
    @Pointcut("execution(* com.quxiangshe.backend.controller.NoteController.createNote(..))")
    public void noteCreatePointcut() {}
    
    /**
     * 定义切点：评论接口
     */
    @Pointcut("execution(* com.quxiangshe.backend.controller.NoteController.addComment(..))")
    public void commentPointcut() {}
    
    /**
     * 定义切点：笔记更新接口
     */
    @Pointcut("execution(* com.quxiangshe.backend.controller.NoteController.updateNote(..))")
    public void noteUpdatePointcut() {}
    
    /**
     * 统一拦截处理
     */
    @Around("noteCreatePointcut() || commentPointcut() || noteUpdatePointcut()")
    public Object checkSensitiveWord(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getName();
        
        // 获取请求参数
        Object[] args = joinPoint.getArgs();
        
        if ("createNote".equals(methodName)) {
            // 笔记发布：检测标题和内容
            return checkNoteCreate(args, joinPoint);
        } else if ("addComment".equals(methodName)) {
            // 评论：检测内容
            return checkComment(args, joinPoint);
        } else if ("updateNote".equals(methodName)) {
            // 笔记更新：检测标题和内容
            return checkNoteUpdate(args, joinPoint);
        }
        
        return joinPoint.proceed();
    }
    
    /**
     * 检测笔记发布
     */
    private Object checkNoteCreate(Object[] args, ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取请求对象（CreateNoteRequest）
        Object requestBody = args[0];
        
        String title = getFieldValue(requestBody, "title");
        String content = getFieldValue(requestBody, "content");
        
        // 检测标题
        if (title != null && !title.isEmpty()) {
            DFAStateMachine.DetectResult titleResult = dfaStateMachine.detect(title);
            if (titleResult.isContainsSensitive() && titleResult.getMaxLevel() >= SensitiveWordCache.LEVEL_HIGH) {
                List<String> words = titleResult.getMatchedWords().stream()
                        .map(DFAStateMachine.MatchWord::getWord)
                        .collect(Collectors.toList());
                throw new SensitiveWordException(
                        titleResult.getMaxLevel(),
                        words,
                        "标题包含违规内容，请修改后重新发布"
                );
            }
        }
        
        // 检测内容
        if (content != null && !content.isEmpty()) {
            DFAStateMachine.DetectResult contentResult = dfaStateMachine.detect(content);
            if (contentResult.isContainsSensitive()) {
                if (contentResult.getMaxLevel() >= SensitiveWordCache.LEVEL_HIGH) {
                    // 高风险：禁止提交
                    List<String> words = contentResult.getMatchedWords().stream()
                            .map(DFAStateMachine.MatchWord::getWord)
                            .collect(Collectors.toList());
                    throw new SensitiveWordException(
                            contentResult.getMaxLevel(),
                            words,
                            "内容包含违规信息，请调整后重新发布"
                    );
                } else if (contentResult.getMaxLevel() == SensitiveWordCache.LEVEL_MEDIUM) {
                    // 中风险：抛异常由前端弹窗提醒，但允许继续发布
                    throw new SensitiveWordException(
                            contentResult.getMaxLevel(),
                            contentResult.getMatchedWords().stream()
                                    .map(DFAStateMachine.MatchWord::getWord)
                                    .collect(Collectors.toList()),
                            "内容包含敏感信息，请注意：" + contentResult.getMatchedWords().stream()
                                    .map(DFAStateMachine.MatchWord::getWord)
                                    .collect(Collectors.joining(","))
                    );
                }
                // 低风险：仅作提示，不阻止发布
            }
        }
        
        return joinPoint.proceed();
    }
    
    /**
     * 检测评论
     */
    private Object checkComment(Object[] args, ProceedingJoinPoint joinPoint) throws Throwable {
        Object requestBody = args[0];
        
        String content = getFieldValue(requestBody, "content");
        
        if (content != null && !content.isEmpty()) {
            DFAStateMachine.DetectResult result = dfaStateMachine.detect(content);
            if (result.isContainsSensitive() && result.getMaxLevel() >= SensitiveWordCache.LEVEL_HIGH) {
                List<String> words = result.getMatchedWords().stream()
                        .map(DFAStateMachine.MatchWord::getWord)
                        .collect(Collectors.toList());
                throw new SensitiveWordException(
                        result.getMaxLevel(),
                        words,
                        "评论内容包含违规信息，请调整后重新发布"
                );
            }
        }
        
        return joinPoint.proceed();
    }
    
    /**
     * 检测笔记更新
     */
    private Object checkNoteUpdate(Object[] args, ProceedingJoinPoint joinPoint) throws Throwable {
        return checkNoteCreate(args, joinPoint);
    }
    
    /**
     * 反射获取字段值
     */
    private String getFieldValue(Object obj, String fieldName) {
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(obj);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.warn("获取字段失败: field={}, error={}", fieldName, e.getMessage());
            return null;
        }
    }
}
```

- [ ] **Step 2: 添加AOP依赖**

检查pom.xml是否包含spring-boot-starter-aop

- [ ] **Step 3: 编译验证**

```bash
cd backend && mvn compile -q
```

---

## Task 5: 修改全局异常处理器

**Files:**
- Modify: `backend/src/main/java/com/quxiangshe/backend/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: 添加异常处理方法**

在GlobalExceptionHandler类中添加：

```java
/**
 * 处理敏感词违规异常
 */
@ExceptionHandler(SensitiveWordException.class)
public R<?> handleSensitiveWordException(SensitiveWordException e) {
    log.warn("敏感词检测未通过: level={}, words={}", e.getLevel(), e.getWords());
    return R.fail(400, e.getMessage());
}
```

- [ ] **Step 2: 编译验证**

```bash
cd backend && mvn compile -q
```

---

## Task 6: 修改NoteController移除侵入代码

**Files:**
- Modify: `backend/src/main/java/com/quxiangshe/backend/controller/NoteController.java`

- [ ] **Step 1: 移除敏感词检测代码**

将发布笔记方法简化为：

```java
/**
 * 发布笔记
 * 敏感词检测由AOP统一处理
 */
@Operation(summary = "发布笔记")
@PostMapping("/create")
public R<NoteVO> createNote(
        @Valid @RequestBody CreateNoteRequest requestBody,
        HttpServletRequest request) {
    Long userId = getCurrentUserId(request);
    if (userId == null) {
        return R.fail(401, "请先登录");
    }
    
    NoteVO note = noteService.createNote(userId, requestBody);
    return R.ok("发布成功", note);
}
```

删除以下代码：
- `sensitiveWordService.containsSensitiveWord()` 调用
- `SensitiveWordCheckResult` 相关逻辑

- [ ] **Step 2: 编译验证**

```bash
cd backend && mvn compile -q
```

---

## Task 7: 验证与测试

**Files:**
- Backend运行测试

- [ ] **Step 1: 启动后端服务**

```bash
cd backend && mvn spring-boot:run
```

- [ ] **Step 2: 测试敏感词检测**

使用curl或Postman测试：

```bash
# 测试高风险敏感词（应返回400）
curl -X POST http://localhost:8080/api/note/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"title":"测试标题","content":"这是色情内容测试"}'
```

- [ ] **Step 3: 编译验证**

```bash
cd backend && mvn compile -q
```

---

## Task 8: 更新OpenSpec规范文档

**Files:**
- Create/Modify: `OpenSpec/specs/backend/sensitive-word-spec.md`

- [ ] **Step 1: 更新规范文档**

添加以下内容：
- 笔记标题敏感词检测规则
- 笔记内容多级风险判断规则
- Redis敏感词库存储说明
- 敏感词异常提示规范
- AOP统一处理机制说明

---

## 执行完成检查

- [ ] Task 1: 敏感词异常类创建完成
- [ ] Task 2: Redis敏感词库管理创建完成
- [ ] Task 3: DFA状态机创建完成
- [ ] Task 4: AOP拦截器创建完成
- [ ] Task 5: 全局异常处理修改完成
- [ ] Task 6: NoteController修改完成
- [ ] Task 7: 验证测试完成
- [ ] Task 8: OpenSpec更新完成
