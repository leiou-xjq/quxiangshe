# 代码风格规范

## 包结构规范

### 顶层包命名
```
com.quxiangshe
├── auth           # 认证模块
├── user           # 用户模块
├── feed           # Feed流模块
├── post           # 动态模块
├── comment        # 评论模块
├── search         # 搜索模块
└── common         # 公共模块
    ├── config     # 配置类
    ├── constant   # 常量
    ├── dto        # 数据传输对象
    ├── entity     # 实体类
    ├── enums      # 枚举
    ├── exception  # 异常定义
    ├── util       # 工具类
    └── vo         # 视图对象
```

### 类命名规范

| 类型 | 命名规范 | 示例 |
|------|----------|------|
| Controller | XxxController | AuthController, FeedController |
| Service接口 | XxxService | UserService, PostService |
| Service实现 | XxxServiceImpl | UserServiceImpl |
| Mapper接口 | XxxMapper | UserMapper, PostMapper |
| 实体类 | XxxEntity | UserEntity, PostEntity |
| DTO | XxxDTO | LoginRequestDTO, UserDTO |
| VO | XxxVO | FeedItemVO, CommentVO |
| 配置类 | XxxConfig | RedisConfig, RabbitMQConfig |
| 异常类 | XxxException | BusinessException |
| 工具类 | XxxUtil | JwtUtil, JsonUtil |

---

## Controller层规范

### 请求处理方法命名

```java
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequestDTO request) {
        // 方法命名：动词+名词
        return success(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO request) {
        return success(authService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        authService.logout(getCurrentUserId());
        return success(null);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        return success(userService.getUserProfile(getCurrentUserId()));
    }
}
```

### Controller层原则

1. **只做参数校验和响应组装**，业务逻辑全部下沉到Service层
2. **禁止在Controller层直接操作数据库**
3. **使用@Valid进行参数校验**，配合BindingResult或全局异常处理
4. **统一返回ResponseEntity<?>或统一响应包装**

---

## Service层规范

### Service接口定义

```java
public interface AuthService {
    UserDTO register(RegisterRequestDTO request);
    LoginResponseDTO login(LoginRequestDTO request);
    void logout(Long userId);
    LoginResponseDTO refresh(String refreshToken);
}
```

### Service实现规范

```java
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public UserDTO register(RegisterRequestDTO request) {
        // 1. 参数校验
        Assert.hasText(request.getUsername(), "用户名不能为空");
        Assert.hasText(request.getPassword(), "密码不能为空");
        
        // 2. 业务逻辑
        validateUsernameUnique(request.getUsername());
        
        UserEntity user = new UserEntity();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userMapper.insert(user);
        
        // 3. 返回结果
        return BeanUtil.copyProperties(user, UserDTO.class);
    }

    private void validateUsernameUnique(String username) {
        Long count = userMapper.countByUsername(username);
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }
    }
}
```

### Service层原则

1. **使用接口+实现分离**，便于扩展和单元测试
2. **使用@RequiredArgsConstructor代替@Autowired**
3. **@Transactional声明在实现类方法上**
4. **每个方法单一职责**，复杂逻辑拆分为私有方法
5. **禁止在Service层直接返回Entity**，必须转换为DTO/VO

---

## Repository层规范

### Mapper接口定义

```java
@Mapper
public interface UserMapper {
    
    @Insert("INSERT INTO user(username, password_hash, phone, email, nickname) " +
            "VALUES(#{username}, #{passwordHash}, #{phone}, #{email}, #{nickname})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserEntity user);

    @Select("SELECT * FROM user WHERE id = #{id}")
    UserEntity selectById(Long id);

    @Select("SELECT * FROM user WHERE username = #{username}")
    UserEntity selectByUsername(String username);

    @Update("UPDATE user SET last_login_time = NOW() WHERE id = #{id}")
    int updateLastLoginTime(Long id);

    @Select("SELECT COUNT(*) FROM user WHERE username = #{username}")
    Long countByUsername(String username);
}
```

### Mapper层原则

1. **使用MyBatis注解或XML**，保持风格一致
2. **禁止在Mapper层写业务逻辑**
3. **使用@Options获取自增主键**
4. **复杂查询使用XML**

---

## 实体类规范

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user")
public class UserEntity {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String username;
    
    @TableField(exist = false)
    private String passwordHash;
    
    private String phone;
    
    private String email;
    
    private String nickname;
    
    private String avatarUrl;
    
    private String bio;
    
    private Integer status;
    
    private LocalDateTime lastLoginTime;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

### Entity层原则

1. **使用Lombok @Data @Builder @NoArgsConstructor @AllArgsConstructor**
2. **@TableName指定表名**
3. **@TableId指定主键策略**
4. **使用@TableField区分数据库字段**
5. **自动填充字段使用MetaObjectHandler**

---

## DTO/VO规范

### Request DTO

```java
@Data
public class RegisterRequestDTO {
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度4-20字符")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度6-20字符")
    private String password;
    
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @Size(max = 50, message = "昵称最长50字符")
    private String nickname;
}
```

### Response VO

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {
    private Long userId;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String bio;
    private Integer followCount;
    private Integer followerCount;
    private Integer postCount;
    private Boolean isFollowing;
    private Boolean isFollowed;
}
```

### DTO/VO原则

1. **Request DTO用于请求参数校验**
2. **Response VO用于响应数据组装**
3. **使用@Valid进行级联校验**
4. **使用@Size @NotBlank @Email等注解**
5. **VO使用Builder模式构建**

---

## 异常处理规范

### 自定义异常

```java
@Data
@EqualsAndHashCode(callSuper = true)
public class BusinessException extends RuntimeException {
    
    private final int code;
    private final String message;
    
    public BusinessException(String message) {
        this(2002, message);
    }
    
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
}
```

### 全局异常处理器

```java
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldError().getDefaultMessage();
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(1001, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("系统异常", e);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(5001, "系统繁忙，请稍后重试"));
    }
}
```

---

## 日志规范

### 日志使用

```java
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    public void someMethod() {
        // 入口日志
        log.info("开始处理用户注册, username={}", username);
        
        try {
            // 业务逻辑
        } catch (Exception e) {
            // 异常日志
            log.error("用户注册失败, username={}, error={}", username, e.getMessage(), e);
            throw new BusinessException("注册失败");
        }
        
        // 成功日志
        log.info("用户注册成功, userId={}", userId);
    }
}
```

### 日志原则

1. **使用@Slf4j注解**
2. **INFO日志记录关键流程**
3. **ERROR日志记录异常堆栈**
4. **禁止在日志中打印敏感信息**(密码、Token、身份证号)
5. **使用占位符{}而不是字符串拼接**

---

## 配置规范

### application.yml

```yaml
server:
  port: 8080

spring:
  application:
    name: quxiangshe-backend
  
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/quxiangshe?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: ${MYSQL_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
  
  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD}
      database: 0
      timeout: 5000ms
  
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
  
  elasticsearch:
    uris: http://localhost:9200

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: com.quxiangshe.**.entity
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl

jwt:
  secret: ${JWT_SECRET:quxiangshe-secret-key-change-in-production}
  access-token-validity: 900
  refresh-token-validity: 604800
```

### 配置类

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        
        return template;
    }
}
```

---

## 安全规范

### Spring Security配置

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final JwtAuthenticationEntryPoint jwtAuthEntryPoint;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/feed/**").authenticated()
                .requestMatchers("/api/v1/posts/**").authenticated()
                .requestMatchers("/api/v1/comments/**").authenticated()
                .anyRequest().permitAll()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(jwtAuthEntryPoint)
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
```

### JWT过滤器

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            try {
                String userId = jwtUtil.extractUserId(token);
                
                if (StringUtils.hasText(userId) && 
                    SecurityContextHolder.getContext().getAuthentication() == null) {
                    
                    UserDetails userDetails = userDetailsService.loadUserByUsername(userId);
                    
                    if (jwtUtil.validateToken(token, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken = 
                            new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            } catch (Exception e) {
                log.error("JWT验证失败: {}", e.getMessage());
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
```

---

## 定时任务规范

### 定时任务配置

```java
@Component
public class CommentAsyncWriteTask {

    private final CommentMapper commentMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Scheduled(fixedRate = 5000)
    public void asyncWriteComment() {
        Long size = redisTemplate.opsForList().size("comment:queue");
        if (size == null || size == 0) {
            return;
        }
        
        List<CommentEntity> batch = new ArrayList<>();
        for (int i = 0; i < Math.min(size, 100); i++) {
            Object obj = redisTemplate.opsForList().rightPop("comment:queue");
            if (obj instanceof CommentEntity) {
                batch.add((CommentEntity) obj);
            }
        }
        
        if (!batch.isEmpty()) {
            commentMapper.batchInsert(batch);
            log.info("批量写入评论 {} 条", batch.size());
        }
    }
}
```

### 定时任务原则

1. **使用@Scheduled注解**
2. **避免任务执行时间超过间隔时间**
3. **添加日志记录执行情况**
4. **长任务添加分布式锁**

---

## 代码格式规范

### 缩进和空格

```java
// 缩进：4空格
// 行长度：最大120字符
public void method(String parameter1, String parameter2, 
                   String parameter3) {
    if (condition) {
        doSomething();
    } else {
        doOtherThing();
    }
}
```

### 导入顺序

```java
import java.util.List;
import org.springframework.stereotype.Service;
import com.quxiangshe.common.exception.BusinessException;
import com.quxiangshe.user.entity.UserEntity;
import com.quxiangshe.user.mapper.UserMapper;
// 顺序：java > org > com.quxiangshe > 其他
```

---

## 单元测试规范

```java
@SpringBootTest
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Test
    void testLogin_Success() {
        LoginRequestDTO request = LoginRequestDTO.builder()
            .username("testuser")
            .password("password123")
            .build();
        
        LoginResponseDTO response = authService.login(request);
        
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotEmpty();
    }
}
```

### 测试原则

1. **Service层必须编写单元测试**
2. **使用@SpringBootTest或@ExtendWith(MockitoExtension.class)**
3. **测试方法命名：test方法名_场景**
4. **Assert断言必须有业务意义**
