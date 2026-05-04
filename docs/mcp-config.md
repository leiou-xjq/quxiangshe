# MCP 服务配置

## 服务说明

趣享社项目已注册为 MCP (Model Context Protocol) 服务，可通过标准 MCP 协议进行调用。

## 服务配置

### 服务类型

- **协议**: stdio
- **认证**: Token-based
- **超时**: 30秒

### 可用工具

| 工具名称 | 功能说明 |
|---------|---------|
| skill | 加载和管理技能 |
| task | 创建和管理任务 |
| todowrite | 任务清单管理 |
| read | 读取文件内容 |
| write | 写入文件 |
| edit | 编辑文件内容 |
| grep | 搜索文件内容 |
| glob | 查找文件 |
| bash | 执行命令行 |

### 能力列表

```json
{
  "capabilities": {
    "tools": true,
    "resources": true,
    "prompts": true
  },
  "tools": [
    {
      "name": "skill",
      "description": "加载和管理技能模块"
    },
    {
      "name": "task",
      "description": "创建和管理任务"
    },
    {
      "name": "todowrite",
      "description": "任务清单管理"
    },
    {
      "name": "read",
      "description": "读取文件内容"
    },
    {
      "name": "write",
      "description": "写入文件内容"
    },
    {
      "name": "edit",
      "description": "编辑文件内容"
    }
  ]
}
```

## 使用方式

### 1. 通过 OpenCode 调用

项目已配置为自动加载 superpowers 插件，可直接使用 skill 工具调用 TDD 开发流程。

### 2. 通过 MCP 客户端调用

```python
from mcp import Client

client = Client("quxiansghe-project")
result = client.call_tool("skill", {"name": "test-driven-development"})
```

### 3. 通过命令行调用

```bash
# 列出可用工具
opencode tools list

# 执行 skill
opencode run --skill test-driven-development

# 执行任务
opencode run --task user-system-register
```

## 技能模块

### 已启用的技能

| 技能名称 | 功能说明 |
|---------|---------|
| brainstorming | 头脑风暴 |
| test-driven-development | TDD 开发模式 |
| subagent-driven-development | 子代理驱动开发 |
| receiving-code-review | 代码审查 |
| requesting-code-review | 请求代码审查 |
| systematic-debugging | 系统化调试 |
| writing-plans | 编写计划 |
| verification-before-completion | 完成前验证 |

### 自定义技能

项目可根据需求添加自定义技能，位于 `docs/skills/` 目录。

## 认证配置

### Token 认证

MCP 服务使用 Token 进行认证：

```json
{
  "auth": {
    "type": "token",
    "token": "mcp_service_token_placeholder"
  }
}
```

### 权限控制

- 只读操作: 无需认证
- 写操作: 需要认证
- 管理操作: 需要管理员权限

## 错误处理

MCP 服务错误响应格式：

```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "错误描述",
    "details": {}
  }
}
```

常见错误码：

| 错误码 | 说明 |
|-------|------|
| AUTH_REQUIRED | 需要认证 |
| AUTH_INVALID | 认证无效 |
| PERMISSION_DENIED | 权限不足 |
| TOOL_NOT_FOUND | 工具不存在 |
| TOOL_EXECUTE_ERROR | 工具执行错误 |
| RESOURCE_NOT_FOUND | 资源不存在 |