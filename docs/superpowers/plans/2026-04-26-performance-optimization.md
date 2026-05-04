# 小蓝书性能优化技术引入实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标：** 引入4项技术栈，全面提升系统性能：
1. RabbitMQ优化Feed分发异步化
2. AC自动机优化敏感词检测性能
3. Canal实现MySQL→Redis评论实时同步
4. 多级缓存优化热点数据访问

**架构：** 
- RabbitMQ作为消息中间件，实现Feed分发的削峰填谷
- AC自动机（Aho-Corasick）实现O(n)复杂度的敏感词多模式匹配
- Canal监听MySQL binlog，实现评论数据的增量同步
- Caffeine本地缓存 + Redis分布式缓存的多级缓存架构

**Tech Stack:** 
- Spring Boot 3.2.1 + Spring AMQP
- Aho-Corasick自动机算法
- Canal (阿里巴巴开源MySQL binlog同步中间件)
- Caffeine (Guava Cache升级版)

---

## 一、RabbitMQ优化Feed分发

### 1.1 架构设计

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Note     │───>│  Rabbit   │───>│  Feed      │
│ Controller │    │   Queue   │    │  Pusher   │
└─────────────┘    └─────────────┘    └─────────────┘
                      ▲
                      │ 异步消费
              ┌───────┴───────┐
              │ 消费者线程池 │ (推荐配置: 核心线程=CPU核数, 最大线程=2倍CPU核数)
```

### 1.2 实施任务

- [ ] **Task 1: 添加Maven依赖**

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
    <version>3.2.1</version>
</dependency>
```

- [ ] **Task 2: 创建RabbitMQ配置类**

```java
// config/RabbitMQConfig.java
@Configuration
public class RabbitMQConfig {
    public static final String FEED_PUSH_EXCHANGE = "feed.push.exchange";
    public static final String FEED_PUSH_QUEUE = "feed.push.queue";
    public static final String FEED_PUSH_ROUTING_KEY = "feed.push";
    
    @Bean
    public DirectExchange feedPushExchange() {
        return new DirectExchange(FEED_PUSH_EXCHANGE);
    }
    
    @Bean
    public Queue feedPushQueue() {
        return QueueBuilder.durable(FEED_PUSH_QUEUE)
                .withArgument("x-dead-letter-exchange", FEED_PUSH_EXCHANGE + ".dlx")
                .build();
    }
    
    @Bean
    public Binding feedPushBinding(Queue feedPushQueue, DirectExchange feedPushExchange) {
        return BindingBuilder.bind(feedPushQueue)
                .to(feedPushExchange)
                .with(FEED_PUSH_ROUTING_KEY);
    }
}
```

- [ ] **Task 3: 创建Feed分发消息体**

```java
// dto/FeedPushMessage.java
@Data
@Builder
public class FeedPushMessage {
    private Long noteId;
    private Long authorId;
    private List<Long> targetUserIds;  // 批量推送的用户ID列表
    private int batchNum;            // 当前批次号
    private int totalBatches;        // 总批次数
    private LocalDateTime pushTime;
}
```

- [ ] **Task 4: 修改NoteController发布笔记接口**

```java
// controller/NoteController.java

@PostMapping("/create")
public R<NoteVO> createNote(@Validated CreateNoteRequest request) {
    // 1. 创建笔记（原有逻辑不变）
    NoteVO noteVO = noteService.createNote(userId, request);
    
    // 2. 异步推送Feed（新增逻辑）
    FeedPushMessage message = FeedPushMessage.builder()
            .noteId(noteVO.getId())
            .authorId(userId)
            .pushTime(LocalDateTime.now())
            .build();
    
    // 获取粉丝列表并分批
    List<Long> followers = followService.getFollowerIds(userId);
    int totalBatches = (followers.size() + BATCH_SIZE - 1) / BATCH_SIZE;
    
    for (int i = 0; i < totalBatches; i++) {
        List<Long> batch = followers.subList(
                i * BATCH_SIZE, 
                Math.min((i + 1) * BATCH_SIZE, followers.size())
        );
        message.setTargetUserIds(batch);
        message.setBatchNum(i);
        message.setTotalBatches(totalBatches);
        
        // 发送到消息队列
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.FEED_PUSH_EXCHANGE,
                RabbitMQConfig.FEED_PUSH_ROUTING_KEY,
                message
        );
    }
    
    return R.ok(noteVO);
}
```

- [ ] **Task 5: 创建Feed消费者**

```java
// consumer/FeedPushConsumer.java
@Component
@Slf4j
public class FeedPushConsumer {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @RabbitListener(queues = RabbitMQConfig.FEED_PUSH_QUEUE)
    public void consumeFeedPush(FeedPushMessage message) {
        log.info("收到Feed推送消息: noteId={}, batch={}/{}", 
                message.getBatchNum(), message.getTotalBatches());
        
        try {
            // 批量推送笔记到粉丝收件箱
            for (Long userId : message.getTargetUserIds()) {
                String feedKey = "feed:" + userId;
                redisTemplate.opsForZSet().add(
                        feedKey, 
                        message.getNoteId().toString(), 
                        System.currentTimeMillis()
                );
            }
            log.info("Feed推送完成: noteId={}, 推送用户数={}", 
                    message.getNoteId(), message.getTargetUserIds().size());
        } catch (Exception e) {
            log.error("Feed推送失败: noteId={}, error={}", 
                    message.getNoteId(), e.getMessage());
            throw new AmqpRejectAndDontRequeueException(e);
        }
    }
}
```

- [ ] **Task 6: 添加application.yml配置**

```yaml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}
    listener:
      simple:
        concurrency: 10
        max-concurrency: 20
        prefetch: 10
        default-requeue-rejected: false
```

- [ ] **Task 7: 编写单元测试**

```java
// test/FeedPushConsumerTest.java
@SpringBootTest
@RabbitMQTest
public class FeedPushConsumerTest {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Test
    public void testFeedPushMessage() {
        FeedPushMessage message = FeedPushMessage.builder()
                .noteId(123L)
                .authorId(456L)
                .targetUserIds(Arrays.asList(1L, 2L, 3L))
                .batchNum(0)
                .totalBatches(1)
                .pushTime(LocalDateTime.now())
                .build();
        
        // 发送消息
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.FEED_PUSH_EXCHANGE,
                RabbitMQConfig.FEED_PUSH_ROUTING_KEY,
                message
        );
        
        // 等待消费
        Thread.sleep(2000);
        
        // 验证
        for (Long userId : message.getTargetUserIds()) {
            Set<Object> feed = redisTemplate.opsForZSet().range("feed:" + userId, 0, -1);
            assertThat(feed).contains("123");
        }
    }
}
```

---

## 二、AC自动机优化敏感词性能

### 2.1 架构设计

```
敏��词库初始化:
┌────────────┐    ┌─────────────┐    ┌────────────┐
│  MySQL     │───>│  初始化    │───>│  AC自动机  │
│ sensitive_ │    │  定时任务  │    │  状态机   │
│ word_table │    │            │    │  (内存)    │
└────────────┘    └─────────────┘    └────────────┘
                              │
检测流程:                      │
┌────────────┐    ┌─────────────┐    ┌────────────┐
│  待检测    │───>│  AC自动机   │───>│  匹配结果  │
│  文本     │    │  多模式匹配  │    │  List返回  │
└────────────┘    └─────────────┘    └────────────┘
复杂度: O(n) - 不受敏感词数量影响
```

### 2.2 实施任务

- [ ] **Task 1: 创建AC自动机核心类**

```java
// util/ACAutomaton.java
@Slf4j
public class ACAutomaton {
    
    private final Map<Integer, Map<Character, Integer>> gotoFunc = new HashMap<>();
    private final Map<Integer, String> output = new HashMap<>();
    private final Map<Integer, Set<String>> failure = new HashMap<>();
    private int nextState = 0;
    
    public ACAutomaton() {
        gotoFunc.put(0, new HashMap<>());
    }
    
    /**
     * 添加敏感词到自动机
     */
    public void addPattern(String word) {
        int currentState = 0;
        for (char c : word.toCharArray()) {
            Map<Character, Integer> transitions = gotoFunc.get(currentState);
            Integer nextState = transitions.get(c);
            
            if (nextState == null) {
                nextState = ++this.nextState;
                transitions.put(c, nextState);
                gotoFunc.put(nextState, new HashMap<>());
            }
            currentState = nextState;
        }
        // 设置输出状态
        output.put(currentState, word);
    }
    
    /**
     * 构建失败函数（使用BFS）
     */
    public void build() {
        Queue<Integer> queue = new LinkedList<>();
        
        // 初始化第一层失败函数
        Map<Character, Integer> level0 = gotoFunc.get(0);
        for (Map.Entry<Character, Integer> entry : level0.entrySet()) {
            int nextState = entry.getValue();
            queue.add(nextState);
            failure.put(nextState, new HashSet<>());
        }
        
        // BFS构建失败函数
        while (!queue.isEmpty()) {
            int currentState = queue.poll();
            
            for (Map.Entry<Character, Integer> entry : 
                    gotoFunc.get(currentState).entrySet()) {
                char c = entry.getKey();
                int nextState = entry.getValue();
                queue.add(nextState);
                
                // 计算失败函数
                int failState = failure.get(currentState);
                Map<Character, Integer> failTransitions = gotoFunc.get(failState);
                Integer failTarget = failTransitions.get(c);
                
                if (failTarget != null) {
                    failure.put(nextState, failure.get(failTarget));
                } else if (failState == 0) {
                    failure.put(nextState, new HashSet<>());
                } else {
                    failure.put(nextState, failure.get(failState));
                }
                
                // 合并输出
                Set<String> failOutput = failure.get(failTarget);
                if (failOutput != null && !failOutput.isEmpty()) {
                    Set<String> combined = new HashSet<>();
                    combined.addAll(failOutput);
                    if (output.containsKey(failTarget)) {
                        combined.add(output.get(failTarget));
                    }
                    failure.put(nextState, combined);
                }
            }
        }
    }
    
    /**
     * 多模式匹配检测
     * 时间复杂度: O(n) - 与文本长度成正比,不受敏感词数量影响
     */
    public List<String> match(String text) {
        List<String> matches = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return matches;
        }
        
        int currentState = 0;
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            // 尝试匹配
            Map<Character, Integer> transitions = gotoFunc.get(currentState);
            Integer nextState = transitions.get(c);
            
            if (nextState != null) {
                currentState = nextState;
            } else {
                // 失败转移
                while (currentState != 0 && transitions.get(c) == null) {
                    currentState = 0;
                    transitions = gotoFunc.get(currentState);
                    nextState = transitions.get(c);
                    if (nextState != null) {
                        currentState = nextState;
                        break;
                    }
                }
            }
            
            // 检查输出
            if (output.containsKey(currentState)) {
                matches.add(output.get(currentState));
            }
            Set<String> failOutput = failure.get(currentState);
            if (failOutput != null) {
                matches.addAll(failOutput);
            }
        }
        
        return matches;
    }
}
```

- [ ] **Task 2: 创建敏感词服务实现**

```java
// service/impl/SensitiveWordServiceImpl.java
@Slf4j
@Service
public class SensitiveWordServiceImpl implements ISensitiveWordService {
    
    private final NoteCommentMapper commentMapper;
    private ACAutomaton automaton = new ACAutomaton();
    private volatile boolean initialized = false;
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    
    private static final String SENSITIVE_WORDS_KEY = "sensitive:words";
    
    @PostConstruct
    public void init() {
        // 启动时加载敏感词库
        initWordLibrary();
    }
    
    @Override
    public void initWordLibrary() {
        rwLock.writeLock().lock();
        try {
            List<String> words = commentMapper.selectSensitiveWords();
            automaton = new ACAutomaton();
            for (String word : words) {
                automaton.addPattern(word);
            }
            automaton.build();
            initialized = true;
            log.info("敏感词库初始化完成, 共加载{}个敏感词", words.size());
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    
    @Override
    public List<String> checkSensitiveWords(String text) {
        if (!initialized) {
            return Collections.emptyList();
        }
        
        rwLock.readLock().lock();
        try {
            return automaton.match(text);
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    @Override
    public boolean containsSensitiveWord(String text) {
        List<String> matches = checkSensitiveWords(text);
        return matches != null && !matches.isEmpty();
    }
    
    /**
     * 定时更新敏感词库（可选: 每天凌晨执行）
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void reloadWordLibrary() {
        initWordLibrary();
    }
}
```

- [ ] **Task 3: 添加数据库表结构**

```sql
-- 敏感词表
CREATE TABLE IF NOT EXISTS sensitive_word (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    word VARCHAR(255) NOT NULL UNIQUE COMMENT '敏感词',
    level TINYINT DEFAULT 1 COMMENT '级别: 1-低 2-中 3-高',
    category VARCHAR(50) COMMENT '分类',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-启用 0-禁用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_word (word),
    INDEX idx_status (status)
) COMMENT '敏感词表';

-- 初始化常用敏感词
INSERT INTO sensitive_word (word, level, category) VALUES
('test1', 1, '测试'),
('test2', 1, '测试'),
('test3', 2, '测试');
```

- [ ] **Task 4: 性能测试**

```java
// test/ACAutomatonTest.java
public class ACAutomatonTest {
    
    @Test
    public void testPerformance() {
        ACAutomaton automaton = new ACAutomaton();
        
        // 添加10万条敏感词
        for (int i = 0; i < 100000; i++) {
            automaton.addPattern("敏感词" + i);
        }
        automaton.build();
        
        // 测试文本: 包含10个敏感词
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            if (i % 100 == 0) {
                text.append("敏感词").append(i / 100);
            } else {
                text.append("正常内容");
            }
        }
        
        // 性能测试
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            automaton.match(text.toString());
        }
        long elapsed = System.currentTimeMillis() - start;
        
        log.info("10万敏感词, 1000字文本, 100次匹配耗时: {}ms", elapsed);
        assertThat(elapsed).isLessThan(1000);  // 应该在1秒内完成
    }
}
```

---

## 三、Canal实现评论实时同步

### 3.1 架构设计

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   MySQL     │───>│   Canal    ��─��─>│   Redis    │
│  binlog    │    │  Server   │    │  缓存      │
│            │    │            │    │            │
│ 插入/更新/  │───>│  解析      │───>│  评论树   │
│ 删除操作  │    │  binlog    │    │  更新     │
└─────────────┘    └─────────────┘    └─────────────┘
                         │
                         │ 增量同步
                    ┌─────┴─────┐
                    │ Canal     │
                    │ Adapter  │
                    │ (Java)    │
```

### 3.2 实施任务

- [ ] **Task 1: 添加Canal依赖**

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.alibaba.otter</groupId>
    <artifactId>canal.client</artifactId>
    <version>1.1.7</version>
</dependency>
<dependency>
    <groupId>com.alibaba.otner</groupId>
    <artifactId>canal.protocol</artifactId>
    <version>1.1.7</version>
</dependency>
```

- [ ] **Task 2: 开启MySQL binlog**

```properties
# my.cnf
binlog-format=ROW
server-id=1
log-bin=mysql-bin
binlog-row-image=FULL
```

- [ ] **Task 3: 创建Canal客户端配置**

```java
// config/CanalConfig.java
@Data
@Configuration
public class CanalConfig {
    @Value("${canal.host:127.0.0.1}")
    private String host;
    
    @Value("${canal.port:11111}")
    private int port;
    
    @Value("${canal.destination:example}")
    private String destination;
    
    @Value("${canal.username:}")
    private String username;
    
    @Value("${canal.password:}")
    private String password;
}
```

- [ ] **Task 4: 创建Canal客户端**

```java
// component/CanalCommentSync.java
@Component
@Slf4j
public class CanalCommentSync {
    
    @Autowired
    private FullSortStrategy fullSortStrategy;
    
    @Autowired
    private NoteCommentMapper noteCommentMapper;
    
    private CanalConnector connector;
    
    @PostConstruct
    public void init() {
        new Thread(this::startCanalClient).start();
    }
    
    private void startCanalClient() {
        while (true) {
            try {
                connector = SpringCanalConnector.newConnector();
                connector.connect();
                connector.subscribe("quxiangshe\\..*");
                connector.seek("quxiangshe", " WHERE update_time > NOW() - INTERVAL 1 DAY");
                
                log.info("Canal客户端已连接, 开始监听MySQL变更");
                
                while (true) {
                    Message message = connector.get(100);
                    for (Entry entry : message.getEntries()) {
                        processEntry(entry);
                    }
                }
            } catch (Exception e) {
                log.error("Canal连接断开, 5秒后重连: {}", e.getMessage());
                connector.disconnect();
                Thread.sleep(5000);
            }
        }
    }
    
    private void processEntry(RowChange rowChange) throws Exception {
        if (!rowChange.hasRowData()) {
            return;
        }
        
        for (RowData rowData : rowChange.getRowDataList()) {
            switch (rowChange.getEventType()) {
                case INSERT:
                case UPDATE:
                    // 新增/更新: 增量更新到Redis
                    handleInsertOrUpdate(rowData);
                    break;
                case DELETE:
                    // 删除: 从Redis移除
                    handleDelete(rowData);
                    break;
            }
        }
    }
    
    private void handleInsertOrUpdate(RowData rowData) {
        Map<String, String> row = new HashMap<>();
        for (Column column : rowData.getColumns()) {
            row.put(column.getName(), column.getValue());
        }
        
        Long noteId = Long.parseLong(row.get("note_id"));
        Long commentId = Long.parseLong(row.get("id"));
        Long parentId = row.get("parent_id") == null ? 0L : 
                Long.parseLong(row.get("parent_id"));
        
        // 增量更新到Redis评论树
        NoteComment comment = noteCommentMapper.selectById(commentId);
        fullSortStrategy.addCommentToTree(noteId, comment, 
                parentId == null || parentId == 0);
    }
    
    private void handleDelete(RowData rowData) {
        Map<String, String> row = new HashMap<>();
        for (Column column : rowData.getColumns()) {
            row.put(column.getName(), column.getValue());
        }
        
        Long noteId = Long.parseLong(row.get("note_id"));
        Long commentId = Long.parseLong(row.get("id"));
        
        // 从Redis评论树删除
        fullSortStrategy.removeCommentAndChildrenFromTree(noteId, commentId, 
                Long.parseLong(row.get("parent_id")));
    }
}
```

- [ ] **Task 5: 配置Canal Server（docker-compose.yml）**

```yaml
version: '3'
services:
  canal-server:
    image: canal/canal-server:v1.1.7
    container_name: canal-server
    ports:
      - "11111:11111"
      - "9100:9100"
    environment:
      - canal.auto.scan=false
      - canal.destinations=example
      - canal.mq.topic=example
      - canal.instance.master.address=mysql:3306
      - canal.instance.dbUsername=root
      - canal.instance.dbPassword=root
      - canal.instance.filter.regex=quxiangshe\\..*
    volumes:
      - ./canal/conf:/home/canal/conf
```

- [ ] **Task 6: 数据一致性保证**

```java
// 补偿任务: 每天凌晨比对MySQL和Redis,
// 修复不一致的数据
@Scheduled(cron = "0 0 3 * * ?")
public void fixInconsistentData() {
    log.info("开始修复不一致的评论数据...");
    
    List<Long> noteIds = noteMapper.selectActiveNoteIds();
    for (Long noteId : noteIds) {
        // 从MySQL统计评论数
        long dbCount = noteCommentMapper.selectCount(
                new LambdaQueryWrapper<NoteComment>()
                        .eq(NoteComment::getNoteId, noteId)
                        .eq(NoteComment::getStatus, 1)
        );
        
        // 从Redis获取评论数
        long redisCount = fullSortStrategy.getCommentCount(noteId);
        
        // 不一致则修复
        if (dbCount != redisCount) {
            fullSortStrategy.setCommentCount(noteId, dbCount);
            log.warn("修复评论数不一致: noteId={}, db={}, redis={}", 
                    noteId, dbCount, redisCount);
        }
    }
    
    log.info("评论数据修复完成");
}
```

---

## 四、热点数据多级缓存

### 4.1 架构设计

```
请求流程:
┌────────────┐
│   请求     │
└─────┬──────┘
      │
      ▼
┌──────────────────┐
│  本地缓存(Caffeine)│──命中──> 返回
└─────┬────────────┘
      │ 未命中
      ▼
┌──────────────────┐
│  Redis缓存       │──命中──> 写入本地缓存→返回
└─────┬────────────┘
      │ 未命中
      ▼
┌──────────────────┐
│  MySQL数据库     │──读取──> 写入Redis→写入本地缓存→返回
└──────────────────┘

热点数据:
- 热门笔记列表 (Hot Notes)
- 用户信息 (User Info)
- 配置信息 (Config)
```

### 4.2 实施任务

- [ ] **Task 1: 添��Caffeine依赖**

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
    <version>3.1.8</version>
</dependency>
```

- [ ] **Task 2: 创建缓存配置类**

```java
// config/CacheConfig.java
@Configuration
public class CacheConfig {
    
    // 热门笔记本地缓存
    @Bean
    public Cache<Long, List<NoteVO>> hotNotesCache() {
        return Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }
    
    // 用户信息本地缓存
    @Bean
    public Cache<Long, UserVO> userInfoCache() {
        return Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }
    
    // 配置信息本地缓存
    @Bean
    public Cache<String, Object> configCache() {
        return Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .recordStats()
                .build();
    }
}
```

- [ ] **Task 3: 创建多级缓存服务**

```java
// service/impl/MultiLevelCacheService.java
@Slf4j
@Service
public class MultiLevelCacheService {
    
    @Autowired
    private Cache<Long, List<NoteVO>> hotNotesCache;
    
    @Autowired
    private Cache<Long, UserVO> userInfoCache;
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    private static final String HOT_NOTES_KEY = "note:hot:list";
    private static final String USER_INFO_KEY = "user:info:";
    
    /**
     * 获取热门笔记列表 - 三级缓存
     */
    public List<NoteVO> getHotNotes(int size) {
        // 1. 尝试本地缓存
        List<NoteVO> localCache = hotNotesCache.getIfPresent(0L);
        if (localCache != null) {
            log.debug("热门笔记命中本地缓存");
            return localCache;
        }
        
        // 2. 尝试Redis缓存
        String redisData = redisTemplate.opsForValue().get(HOT_NOTES_KEY);
        if (redisData != null) {
            try {
                List<NoteVO> notes = objectMapper.readValue(redisData, 
                        new TypeReference<List<NoteVO>>() {});
                hotNotesCache.put(0L, notes);
                log.debug("热门笔记命中Redis缓存");
                return notes;
            } catch (JsonProcessingException e) {
                log.error("解析热门笔记失败: {}", e.getMessage());
            }
        }
        
        // 3. 查询MySQL
        List<NoteVO> notes = noteService.getPopularNotes(null, size, null);
        
        // 4. 写入Redis缓存
        try {
            redisTemplate.opsForValue().set(HOT_NOTES_KEY, 
                    objectMapper.writeValueAsString(notes),
                    5, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            log.error("序列化热门笔记失败: {}", e.getMessage());
        }
        
        // 5. 写入本地缓存
        hotNotesCache.put(0L, notes);
        log.debug("热门笔记写入缓存完成");
        
        return notes;
    }
    
    /**
     * 获取用户信息 - 三级缓存
     */
    public UserVO getUserInfo(Long userId) {
        // 1. 尝试本地缓存
        UserVO localCache = userInfoCache.getIfPresent(userId);
        if (localCache != null) {
            return localCache;
        }
        
        // 2. 尝试Redis缓存
        String redisData = redisTemplate.opsForValue().get(USER_INFO_KEY + userId);
        if (redisData != null) {
            try {
                UserVO userVO = objectMapper.readValue(redisData, UserVO.class);
                userInfoCache.put(userId, userVO);
                return userVO;
            } catch (JsonProcessingException e) {
                log.error("解析用户信息失败: {}", e.getMessage());
            }
        }
        
        // 3. 查询MySQL
        UserVO userVO = userService.getUserInfo(userId);
        
        // 4. 写入缓存
        if (userVO != null) {
            try {
                redisTemplate.opsForValue().set(USER_INFO_KEY + userId,
                        objectMapper.writeValueAsString(userVO),
                        10, TimeUnit.MINUTES);
            } catch (JsonProcessingException e) {
                log.error("序列化用户信息失败: {}", e.getMessage());
            }
            userInfoCache.put(userId, userVO);
        }
        
        return userVO;
    }
    
    /**
     * 主动失效缓存
     */
    public void invalidateHotNotes() {
        hotNotesCache.invalidate(0L);
        redisTemplate.delete(HOT_NOTES_KEY);
        log.info("热门笔记缓存已失效");
    }
    
    public void invalidateUserInfo(Long userId) {
        userInfoCache.invalidate(userId);
        redisTemplate.delete(USER_INFO_KEY + userId);
        log.info("用户信息缓存已失效: userId={}", userId);
    }
    
    /**
     * 打印缓存命中率统计
     */
    @PostConstruct
    public void printStats() {
        CacheStats stats = hotNotesCache.stats();
        log.info("热门笔记缓存统计: hitRate={}, hitCount={}, missCount={}", 
                stats.hitRate(), stats.hitCount(), stats.missCount());
    }
}
```

- [ ] **Task 4: 本地缓存预热**

```java
// component/CacheWarmer.java
@Component
@Slf4j
public class CacheWarmer implements CommandLineRunner {
    
    @Autowired
    private MultiLevelCacheService cacheService;
    
    @Override
    public void run(String... args) {
        log.info("开始预热缓存...");
        
        // 预热热门笔记
        cacheService.getHotNotes(20);
        
        // 预热系统配置
        configService.loadAllConfigs();
        
        log.info("缓存预热完成");
    }
}
```

- [ ] **Task 5: 监控配置**

```yaml
# application.yml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=10000,expireAfterWrite=10m

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,cache
  metrics:
    cache:
      specs:
        caffeine: true
```

- [ ] **Task 6: 测试用例**

```java
// test/MultiLevelCacheServiceTest.java
@SpringBootTest
public class MultiLevelCacheServiceTest {
    
    @Autowired
    private MultiLevelCacheService cacheService;
    
    @Test
    public void testHitLocalCache() {
        // 第一次查询
        long start1 = System.nanoTime();
        List<NoteVO> notes1 = cacheService.getHotNotes(20);
        long time1 = System.nanoTime() - start1;
        
        // 第二次查询(应该命中本地缓存)
        long start2 = System.nanoTime();
        List<NoteVO> notes2 = cacheService.getHotNotes(20);
        long time2 = System.nanoTime() - start2;
        
        log.info("第一次: {}ns, 第二次: {}ns", time1, time2);
        assertThat(time2).isLessThan(time1);
    }
    
    @Test
    public void testInvalidate() {
        // 预热
        cacheService.getHotNotes(20);
        
        // 失效
        cacheService.invalidateHotNotes();
        
        // 再次查询应该从MySQL获取
        List<NoteVO> notes = cacheService.getHotNotes(20);
        assertThat(notes).isNotNull();
    }
}
```

---

## 三、测试与验证

### 测试清单

- [ ] RabbitMQ消息发送和消费测试
- [ ] AC自动机性能测试（10万敏感词）
- [ ] Canal增量同步测试
- [ ] 多级缓存命中率���试
- [ ] 压测：Feed分发QPS
- [ ] 压测：敏感词检测耗时
- [ ] 压测：热点数据响应时间

### 性能目标

| 指标 | 目标值 |
|------|--------|
| Feed分发QPS | 5000+ |
| 敏感词检测 | <10ms（百万词库） |
| 热点列表响应 | <30ms |
| 缓存命中率 | >95% |

---

**Plan complete and saved to `docs/superpowers/plans/2026-04-26-performance-optimization.md`. Two execution options:**

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**

**If Subagent-Driven chosen:**
- **REQUIRED SUB-SKILL:** Use superpowers:subagent-driven-development
- Fresh subagent per task + two-stage review

**If Inline Execution chosen:**
- **REQUIRED SUB-SKILL:** Use superpowers:executing-plans
- Batch execution with checkpoints for review