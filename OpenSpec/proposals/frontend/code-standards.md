# 前端代码规范

## 概述

本文档定义了前端代码编写的标准规范，确保代码的可维护性和一致性。

## 代码风格

### 通用规则

- 使用ESLint和Prettier进行代码检查和格式化
- 使用2空格缩进
- 使用单引号
- 语句结尾不加分号
- 变量声明使用const/let，避免使用var

### 命名规范

```typescript
// 变量和函数：使用驼峰命名
const userName = 'admin';
function getUserInfo() {}

// 常量：使用大写和下划线
const MAX_RETRY_COUNT = 3;
const API_BASE_URL = '/api/v1';

// 类和组件：使用帕斯卡命名
class UserService {}
function UserCard() {}

// 文件命名
// 组件：UserCard.tsx
// 工具：formatDate.ts
// 类型：userType.ts
// 样式：userCard.module.css
```

### 注释规范

```typescript
// 单行注释
const name = 'admin'; // 用户名称

// 多行注释
/**
 * 获取用户信息
 * @param id 用户ID
 * @returns 用户信息
 */
async function getUser(id: number) {}

// TODO标记
// TODO: 优化性能
// FIXME: 修复此bug
```

## TypeScript 规范

### 类型定义

```typescript
// 接口
interface User {
    id: number;
    name: string;
    email?: string; // 可选属性
    readonly role: string; // 只读属性
}

// 类型别名
type Status = 'pending' | 'active' | 'disabled';

// 枚举
enum UserRole {
    Admin = 'admin',
    User = 'user',
    Guest = 'guest'
}

// 泛型
interface Response<T> {
    code: number;
    data: T;
    message: string;
}
```

### 类型断言

```typescript
// 不推荐
const value = something as string;

// 推荐
const value = something;
if (typeof value === 'string') {}

// 类型守卫
function isUser(obj: any): obj is User {
    return 'id' in obj && 'name' in obj;
}
```

## Vue/React 规范

### 组件规范

```vue
<!-- Vue 组件示例 -->
<template>
  <div class="user-card">
    <h3>{{ user.name }}</h3>
  </div>
</template>

<script setup lang="ts">
interface Props {
  user: User;
}

const props = withDefaults(defineProps<Props>(), {
  user: () => ({ id: 0, name: '' })
});
</script>

<style scoped>
.user-card {
  padding: 16px;
}
</style>
```

```tsx
// React 组件示例
interface UserCardProps {
  user: User;
}

export function UserCard({ user }: UserCardProps) {
  return (
    <div className="user-card">
      <h3>{user.name}</h3>
    </div>
  );
}
```

### 目录结构

```
src/
├── components/      # 公共组件
├── views/          # 页面组件
├── hooks/          # 自定义Hook
├── utils/          # 工具函数
├── api/            # API接口
├── types/          # 类型定义
├── styles/         # 全局样式
└── router/         # 路由配置
```

## 文件组织

```typescript
// 导入顺序
// 1. 外部库
import React from 'react';
import { useNavigate } from 'react-router-dom';

// 2. 内部模块
import { useUserStore } from '@/stores/user';
import { formatDate } from '@/utils/date';

// 3. 组件
import { Button } from '@/components/Button';
import UserCard from './UserCard';

// 4. 类型
import type { User } from '@/types';

// 5. 样式
import styles from './index.module.css';
```