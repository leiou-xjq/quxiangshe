# 组件设计规范

## 概述

本文档定义了前端组件开发的标准规范，确保组件的可复用性和一致性。

## 组件分类

### 基础组件

- Button - 按钮
- Input - 输入框
- Select - 选择器
- Checkbox - 复选框
- Radio - 单选框
- Switch - 开关
- Modal - 模态框
- Table - 表格

### 业务组件

- UserCard - 用户卡片
- OrderCard - 订单卡片
- ProductList - 产品列表
- SearchBar - 搜索栏

## 组件规范

### 组件结构

```typescript
// 组件文件结构
// UserCard/
// ├── index.tsx          # 组件实现
// ├── index.module.css   # 样式
// ├── UserCardProps.ts   # 类型定义
// └── index.md           # 文档
```

### 组件模板

```typescript
import React from 'react';
import styles from './index.module.css';

interface UserCardProps {
  id: number;
  name: string;
  avatar?: string;
  role?: string;
  onClick?: (id: number) => void;
}

export function UserCard({ 
  id, 
  name, 
  avatar, 
  role,
  onClick 
}: UserCardProps) {
  const handleClick = () => {
    onClick?.(id);
  };

  return (
    <div className={styles.card} onClick={handleClick}>
      {avatar && <img src={avatar} alt={name} className={styles.avatar} />}
      <div className={styles.info}>
        <h3 className={styles.name}>{name}</h3>
        {role && <span className={styles.role}>{role}</span>}
      </div>
    </div>
  );
}
```

## Props 规范

### 命名规则

```typescript
// 事件处理函数以 on 开头
interface Props {
  onClick?: () => void;
  onChange?: (value: string) => void;
  onSubmit?: (data: FormData) => void;
}

// 布尔属性使用 is/has/can 前缀
interface Props {
  isLoading?: boolean;
  hasError?: boolean;
  canEdit?: boolean;
}

// 可选属性使用 ? 标记
interface Props {
  title?: string;
  description?: string;
}
```

### 默认值

```typescript
// 使用 withDefaults（Vue）或默认值（React）
interface Props {
  size?: 'small' | 'medium' | 'large';
  variant?: 'primary' | 'secondary';
}

const defaultProps = {
  size: 'medium' as const,
  variant: 'primary' as const,
};
```

## 样式规范

### CSS Modules

```css
/* 命名空间 */
.container { }
.title { }
.content { }
.footer { }

/* 状态变体 */
.container.disabled { }
.title.active { }
```

### 主题变量

```css
:root {
  --primary-color: #1890ff;
  --success-color: #52c41a;
  --warning-color: #faad14;
  --error-color: #f5222d;
  --text-color: #333;
  --border-color: #d9d9d9;
}
```

## 组件文档

```markdown
# UserCard 用户卡片

## 组件用途
展示用户基本信息

## 属性

| 属性 | 类型 | 默认值 | 说明 |
|-----|------|-------|------|
| id | number | - | 用户ID |
| name | string | - | 用户名称 |
| avatar | string | - | 头像URL |
| role | string | - | 角色 |
| onClick | (id: number) => void | - | 点击回调 |

## 使用示例

```tsx
<UserCard 
  id={1} 
  name="张三" 
  avatar="/avatar.png"
  onClick={(id) => console.log(id)}
/>
```