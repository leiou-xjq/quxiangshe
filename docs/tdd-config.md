# TDD 开发模式配置

## 严格 TDD 模式

本项目已启用严格 TDD 开发模式，所有功能开发必须遵循以下流程：

### 开发流程

```
1. 编写测试 → 2. 运行测试(失败) → 3. 编写代码 → 4. 运行测试(通过) → 5. 重构 → 6. 提交
```

### 测试要求

- 所有业务代码必须有对应的单元测试
- 单元测试覆盖率 >= 80%
- 集成测试覆盖核心业务流程
- 测试用例必须包含边界条件

### 测试文件结构

```
src/test/
├── java/com/quxian/
│   ├── user/
│   │   ├── UserServiceTest.java
│   │   ├── AuthServiceTest.java
│   │   └── UserControllerTest.java
│   └── ...
└── resources/
    ├── application-test.yml
    └── data-test.sql
```

### 运行测试

```bash
# 运行所有测试
mvn test

# 运行单元测试
mvn test -Dtest=*Test

# 运行集成测试
mvn verify -Dtest=*IT

# 生成覆盖率报告
mvn test jacoco:report
```

### 测试规范

#### 单元测试规范

```java
@SpringBootTest
class UserServiceTest {
    
    @Autowired
    private UserService userService;
    
    @Test
    void testRegister_Success() {
        // Arrange - 准备测试数据
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setPassword("Aa123456");
        request.setConfirmPassword("Aa123456");
        
        // Act - 执行测试
        User result = userService.register(request);
        
        // Assert - 断言结果
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }
}
```

#### 测试命名规范

- `test{方法名}_{场景}_{预期结果}`
- 示例: `testRegister_Success`, `testRegister_UsernameExists`

#### Mock 使用规范

- 使用 Mockito 进行依赖 Mock
- 避免对外部服务进行真实调用
- 验证 Mock 方法调用次数和参数

### 验收标准

| 指标 | 标准 |
|-----|------|
| 单元测试覆盖率 | >= 80% |
| 核心业务覆盖率 | >= 90% |
| 测试通过率 | 100% |
| 新功能测试 | 100%覆盖 |