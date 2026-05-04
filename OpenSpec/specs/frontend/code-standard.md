# 前端代码规范

## 文档信息

| 项目 | 内容 |
|-----|------|
| 版本 | v1.0.0 |
| 状态 | 已发布 |
| 更新日期 | 2024-01-01 |

## 1. 范围

本规范定义了前端代码的编写标准，适用于React/Vue等前端框架开发。

## 2. 命名规范

### 2.1 文件命名

```
# 组件文件 - 帕斯卡命名
UserCard.tsx
OrderList.tsx

# 工具文件 - 驼峰命名
formatDate.ts
debounce.ts

# 类型文件 - 驼峰命名
userType.ts
apiResponse.ts

# 样式文件 - 组件名.module.css
UserCard.module.css
```

### 2.2 变量/函数命名

```typescript
// 变量 - 驼峰
const userName = 'admin';
const userList: User[] = [];

// 常量 - 大写下划线
const API_BASE_URL = '/api/v1';
const MAX_UPLOAD_SIZE = 5 * 1024 * 1024;

// 函数 - 驼峰
function getUserInfo(id: number) {}
function formatDate(date: Date) {}

// 组件函数 - 帕斯卡
function UserCard() {}
function OrderList() {}
```

### 2.3 CSS 类命名

```css
/* 块 */
.user-card {}
.order-list {}

/* 元素 */
.user-card-title {}
.user-card-avatar {}

/* 修饰符 */
.user-card--disabled {}
.button--primary {}
```

## 3. 代码结构

### 3.1 目录结构

```
src/
├── api/              # API 接口
├── assets/           # 静态资源
├── components/       # 公共组件
│   ├── Button/
│   ├── Input/
│   └── Modal/
├── hooks/            # 自定义 Hook
├── layouts/          # 布局组件
├── pages/            # 页面组件
├── router/           # 路由配置
├── stores/           # 状态管理
├── styles/           # 全局样式
├── types/            # 类型定义
├── utils/            # 工具函数
└── App.tsx
```

### 3.2 组件结构

```tsx
// React 组件
import React from 'react';
import styles from './index.module.css';

interface Props {
  title: string;
  onConfirm?: () => void;
}

export function Modal({ title, onConfirm }: Props) {
  return (
    <div className={styles.modal}>
      <h2 className={styles.title}>{title}</h2>
    </div>
  );
}
```

```vue
<!-- Vue 组件 -->
<template>
  <div class="modal">
    <h2 class="title">{{ title }}</h2>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string;
}>();
</script>

<style scoped>
.modal { }
</style>
```

## 4. TypeScript 规范

### 4.1 类型定义

```typescript
// 接口
interface User {
  id: number;
  name: string;
  email?: string;
}

// 类型别名
type Status = 'pending' | 'active' | 'disabled';

// 枚举
enum UserRole {
  Admin = 'admin',
  User = 'user',
}
```

### 4.2 类型断言

```typescript
// 避免使用 as
// 推荐使用类型守卫
function isUser(obj: unknown): obj is User {
  return 'id' in obj && 'name' in obj;
}
```

## 5. 组件规范

### 5.1 Props 定义

```typescript
interface ButtonProps {
  type?: 'primary' | 'secondary';
  size?: 'small' | 'medium' | 'large';
  loading?: boolean;
  disabled?: boolean;
  onClick?: () => void;
}
```

### 5.2 事件处理

```typescript
// 使用箭头函数或解构
<Button onClick={() => handleClick()} />
<Button onClick={handleClick} />
```

## 6. 样式规范

### 6.1 CSS Modules

```css
/* index.module.css */
.container {
  padding: 16px;
}

.title {
  font-size: 16px;
  font-weight: 500;
}
```

### 6.2 响应式

```css
/* 使用媒体查询 */
.container {
  width: 100%;
}

@media (min-width: 768px) {
  .container {
    width: 750px;
  }
}
```

## 7. 代码检查

### 7.1 ESLint 规则

```json
{
  "rules": {
    "no-unused-vars": "error",
    "no-console": "warn",
    "prefer-const": "error"
  }
}
```

### 7.2 Prettier 配置

```json
{
  "semi": false,
  "singleQuote": true,
  "tabWidth": 2,
  "trailingComma": "es5"
}
```