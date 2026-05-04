# 技术提案

> 本目录存放各类技术提案（RFC）和改进建议

## 目录

### 数据库层

- [数据库设计规范](./database/db-design-guidelines.md) - 数据库命名、设计原则
- [SQL编码规范](./database/sql-coding-standards.md) - SQL编写规范和最佳实践
- [数据库迁移规范](./database/db-migration-guidelines.md) - 数据库版本管理和迁移策略

### 后端服务

- [RESTful API设计规范](./backend/restful-api-guidelines.md) - REST API设计原则和约定
- [认证授权方案](./backend/auth-design.md) - 认证和授权机制设计
- [错误处理规范](./backend/error-handling.md) - 错误码和异常处理规范
- [日志规范](./backend/logging-standard.md) - 日志记录规范

### 前端应用

- [前端代码规范](./frontend/code-standards.md) - 前端代码编写规范
- [组件设计规范](./frontend/component-design.md) - 组件开发规范
- [状态管理方案](./frontend/state-management.md) - 前端状态管理设计
- [API调用规范](./frontend/api-client.md) - 前端API调用规范

## 提案流程

1. 在对应子目录创建提案文件（Markdown格式）
2. 填写提案模板（见下方）
3. 提交Pull Request进行评审

## 提案模板

```markdown
# 提案标题

## 提案人
[姓名]

## 概述
[简要描述提案内容]

## 背景
[问题背景和动机]

## 提案内容
[详细描述提案方案]

## 替代方案
[如有替代方案在此列出]

## 影响范围
[说明提案影响的范围]

## 评审意见
[评审过程中记录意见]
```