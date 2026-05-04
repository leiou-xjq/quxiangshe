# 后端代码规范

## 文档信息

| 项目 | 内容 |
|-----|------|
| 版本 | v1.0.0 |
| 状态 | 已发布 |
| 更新日期 | 2024-01-01 |

## 1. 范围

本规范定义了后端代码的编写标准，适用于Java/Go/Node.js等后端服务开发。

## 2. 命名规范

### 2.1 包/模块命名

```java
// Java
package com.example.user.service;

// Go
package user

// Node.js
const userService = require('./user/service');
```

### 2.2 类/结构体命名

```java
// Java - 帕斯卡命名
public class UserService {}
public class UserController {}

public interface UserRepository {}
public class UserEntity {}

// Go - 帕斯卡命名
type UserService struct {}
type UserController struct {}
```

### 2.3 方法/函数命名

```java
// 驼峰命名
public User getUserById(Long id) {}
public List<User> listUsers(UserQuery query) {}
public void createUser(User user) {}
public void updateUser(Long id, User user) {}
public void deleteUser(Long id) {}
```

### 2.4 变量命名

```java
// 驼峰命名
private Long userId;
private String userName;
private List<User> userList;

// 常量 - 大写下划线
private static final int MAX_RETRY_COUNT = 3;
public static final String DEFAULT_PASSWORD = "123456";
```

## 3. 代码结构

### 3.1 分层结构

```
src/
├── controller/      # 控制器层
├── service/        # 业务逻辑层
├── repository/    # 数据访问层
├── model/          # 数据模型
├── dto/            # 数据传输对象
├── vo/             # 视图对象
├── exception/     # 异常定义
├── config/         # 配置类
└── util/           # 工具类
```

### 3.2 Controller 层

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping
    public ResponseEntity<List<UserVO>> list(UserQuery query) {
        return ResponseEntity.ok(userService.list(query));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<UserVO> get(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getById(id));
    }
    
    @PostMapping
    public ResponseEntity<UserVO> create(@RequestBody @Valid CreateUserDTO dto) {
        return ResponseEntity.ok(userService.create(dto));
    }
}
```

### 3.3 Service 层

```java
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    public UserVO getById(Long id) {
        UserEntity entity = userRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("用户不存在"));
        return UserMapper.toVO(entity);
    }
    
    @Transactional
    public UserVO create(CreateUserDTO dto) {
        // 业务逻辑
        return result;
    }
}
```

## 4. 异常处理

### 4.1 异常定义

```java
public class BusinessException extends RuntimeException {
    private int code;
    
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}

public class NotFoundException extends BusinessException {
    public NotFoundException(String message) {
        super(404, message);
    }
}
```

### 4.2 全局异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        return ResponseEntity.status(e.getCode())
            .body(new ErrorResponse(e.getCode(), e.getMessage()));
    }
}
```

## 5. 日志规范

### 5.1 日志级别

```java
logger.debug("调试信息：{}", detail);
logger.info("业务操作：{}", message);
logger.warn("警告信息：{}", message);
logger.error("错误信息：{}", error, exception);
```

### 5.2 记录内容

```java
// 操作日志
logger.info("用户登录: userId={}, ip={}", userId, ip);

// 业务日志
logger.info("创建订单: orderId={}, userId={}", orderId, userId);

// 错误日志
logger.error("处理失败: orderId={}, error={}", orderId, error, exception);
```

## 6. 单元测试

### 6.1 测试类命名

```java
// 类名 + Test
public class UserServiceTest {}

// 测试方法
@Test
public void testGetUserById() {}

@Test
public void testCreateUser() {}
```

### 6.2 测试用例

```java
@Test
public void testGetUserById_UserExists() {
    // Arrange
    Long userId = 1L;
    when(userRepository.findById(userId)).thenReturn(Optional.of(entity));
    
    // Act
    UserVO result = userService.getById(userId);
    
    // Assert
    Assert.assertNotNull(result);
    Assert.assertEquals(userId, result.getId());
}
```