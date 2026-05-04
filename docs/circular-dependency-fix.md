# 循环依赖问题修复文档

## 问题描述

启动应用时出现循环依赖错误：

```
The dependencies of some of the beans in the application context form a cycle:

   feedController defined in ... FeedController.class
┌─────┐
|  feedServiceImpl defined in ... FeedServiceImpl.class
↑     ↓
|  noteServiceImpl defined in ... NoteServiceImpl.class
└─────┘
```

## 原因分析

服务间存在双向依赖：
- `FeedServiceImpl` 依赖 `NoteServiceImpl`
- `NoteServiceImpl` 依赖 `FeedServiceImpl`
- `FollowServiceImpl` 也依赖 `FeedServiceImpl`

Spring在初始化这些Bean时形成循环，无法确定注入顺序。

## 最终解决方案

由于Lombok的 `@RequiredArgsConstructor` 与 `@Lazy` 配合存在问题，改用 **setter注入 + @Lazy** 组合：

### 1. FeedServiceImpl.java

```java
private INoteService noteService;
private IBlacklistService blacklistService;

@Lazy
@Autowired
public void setNoteService(INoteService noteService) {
    this.noteService = noteService;
}

@Lazy
@Autowired
public void setBlacklistService(IBlacklistService blacklistService) {
    this.blacklistService = blacklistService;
}
```

### 2. NoteServiceImpl.java

```java
private IActivityService activityService;

@Lazy
@Autowired
private IFeedService feedService;
```

### 3. FollowServiceImpl.java

```java
private IFeedService feedService;

@Lazy
@Autowired
public void setFeedService(IFeedService feedService) {
    this.feedService = feedService;
}
```

## 修改文件

| 文件 | 修改内容 |
|-----|---------|
| `FeedServiceImpl.java` | setter注入noteService和blacklistService |
| `NoteServiceImpl.java` | 字段注入feedService |
| `FollowServiceImpl.java` | setter注入feedService |

## 测试

```bash
mvn clean compile -q
mvn spring-boot:run
```

启动成功输出：
```
趣享社后端服务启动成功！
API文档: http://localhost:8080/api/doc.html
```