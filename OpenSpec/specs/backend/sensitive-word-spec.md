# 趣享社 - 敏感词检测规范

## 一、概述

趣享社社交平台采用 Redis + DFA算法实现敏感词检测，支持多级风险判断和变体词识别。敏感词检测通过AOP统一处理，不侵入业务代码。

## 二、检测规则

### 2.1 标题敏感词检测

| 项目 | 说明 |
|-----|------|
| 存储 | Redis Hash `sensitive:words:library` |
| 算法 | DFA状态机匹配 |
| 风险等级 | 高风险（3级）直接阻断，其他等级允许发布 |
| 提示信息 | "标题包含违规内容，请修改后重新发布" |

### 2.2 内容敏感词检测

| 风险等级 | 典型词汇 | 处理方式 |
|---------|---------|----------|
| 高风险（3级） | 政治、色情、赌博、毒品、诈骗 | 直接阻断，返回400错误 |
| 中风险（2级） | 暴力、广告 | 弹窗提醒，但允许发布 |
| 低风险（1级） | 垃圾 | 仅文字提示，允许发布 |

### 2.3 变体词识别

系统支持以下变体词识别：
- **数字/字母替换**：0→o, 1→i, 2→z, 3→e 等
- **特殊符号分隔**：在敏感词中间插入空格、-、_等
- **大小写混合**：不区分大小写

## 三、技术架构

### 3.1 核心组件

| 组件 | 位置 | 说明 |
|-----|------|------|
| SensitiveWordCache | util/SensitiveWordCache.java | Redis敏感词库管理，从Redis加载敏感词 |
| DFAStateMachine | util/DFAStateMachine.java | DFA状态机实现，内存中高效匹配 |
| SensitiveWordAspect | aspect/SensitiveWordAspect.java | AOP统一拦截器，处理敏感词检测 |
| SensitiveWordException | exception/SensitiveWordException.java | 敏感词违规异常 |
| GlobalExceptionHandler | exception/GlobalExceptionHandler.java | 全局异常处理，捕获敏感词异常 |

### 3.2 处理流程

```
┌─────────────────┐
│  用户提交发布请求  │
└────────┬────────┘
         ▼
┌─────────────────┐
│  AOP拦截器生效   │
└────────┬────────┘
         ▼
┌─────────────────┐
│  DFA状态机检测   │
│  标题 + 内容     │
└────────┬────────┘
         ▼
   ┌─────┴─────┐
   │ 风险等级判断│
   └─────┬─────┘
    ┌────┼────┐
    ▼    ▼    ▼
  高风险 中风险 低风险
   │     │      │
   ▼     ▼      ▼
 阻断   提醒   提示
         │      │
         └──────┘
           │
           ▼
    ┌──────────────┐
    │  GlobalExcep │
    │ tionHandler │
    └──────────────┘
           │
           ▼
      返回响应给前端
```

## 四、Redis Key 设计

```
敏感词库: sensitive:words:library (Hash结构)
          - field: 敏感词
          - value: 等级(1/2/3)

变体词:   sensitive:words:variants:{word} (Set结构)
```

## 五、AOP统一处理

### 5.1 拦截接口

- `NoteController.createNote()` - 笔记发布
- `NoteController.addComment()` - 评论添加

### 5.2 检测逻辑

```java
// 标题检测
if (titleResult.isContainsSensitive() && titleResult.getMaxLevel() >= LEVEL_HIGH) {
    throw new SensitiveWordException(...)
}

// 内容检测
if (contentResult.getMaxLevel() >= LEVEL_HIGH) {
    // 高风险：禁止提交
    throw new SensitiveWordException(...)
} else if (contentResult.getMaxLevel() == LEVEL_MEDIUM) {
    // 中风险：弹窗提醒但允许发布
    throw new SensitiveWordException(...)
}
// 低风险：不阻止发布
```

## 六、异常响应格式

### 6.1 标题敏感词（高风险）

```json
{
  "code": 400,
  "message": "标题包含违规内容，请修改后重新发布",
  "data": null,
  "timestamp": "2026-04-06T17:31:17"
}
```

### 6.2 内容敏感词（高风险）

```json
{
  "code": 400,
  "message": "内容包含违规信息，请调整后重新发布",
  "data": null,
  "timestamp": "2026-04-06T17:31:17"
}
```

### 6.3 内容敏感词（中风险）

```json
{
  "code": 400,
  "message": "内容包含敏感信息，请注意：暴力,广告",
  "data": null,
  "timestamp": "2026-04-06T17:31:17"
}
```

## 七、前端处理

前端通过响应拦截器捕获400错误，提取敏感词信息并展示提示：

```javascript
if (error.response && error.response.status === 400) {
  const message = error.response.data?.message
  if (message.includes('敏感词') || message.includes('违规')) {
    ElMessage.warning(message)
  }
}
```

## 八、默认敏感词库

| 敏感词 | 风险等级 | 分类 |
|-------|---------|------|
| 色情 | 高(3) | porn |
| 赌博 | 高(3) | ads |
| 毒品 | 高(3) | general |
| 政治 | 高(3) | politics |
| 诈骗 | 高(3) | general |
| 暴力 | 中(2) | general |
| 广告 | 中(2) | ads |
| 垃圾 | 低(1) | ads |

## 九、注意事项

1. 敏感词库只存储在Redis，不使用数据库
2. DFA状态机在应用启动时从Redis加载到内存
3. 敏感词更新后需调用 `DFAStateMachine.refresh()` 刷新状态机
4. AOP检测发生在业务逻辑之前，高风险直接阻断
5. 中风险抛出异常由GlobalExceptionHandler处理，返回400但前端可继续操作
6. 低风险不抛出异常，不阻止发布，仅记录日志
