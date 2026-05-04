# 前端组件库规范

## 文档信息

| 项目 | 内容 |
|-----|------|
| 版本 | v1.0.0 |
| 状态 | 已发布 |
| 更新日期 | 2024-01-01 |

## 1. 范围

本规范定义了前端组件库的使用标准，确保组件的一致性和可维护性。

## 2. 组件选型

### 2.1 推荐组件库

| 框架 | 组件库 |
-----|-------|
| React | Ant Design / Material UI |
| Vue | Element Plus / Naive UI |

### 2.2 选择原则

- 活跃的社区支持
- 完善的文档
- 良好的TypeScript支持
- 主题定制能力

## 3. 使用规范

### 3.1 全局引入

```typescript
// 按需引入（推荐）
import { Button, Input, Select } from 'antd';
```

### 3.2 主题定制

```typescript
// config-overwrite.js
const { override, fixBabelImports, addLessLoader } = require('customize-cra');

module.exports = override(
  fixBabelImports('import', {
    libraryName: 'antd',
    libraryDirectory: 'es',
    style: true,
  }),
  addLessLoader({
    javascriptEnabled: true,
    modifyVars: {
      '@primary-color': '#1890ff',
      '@border-radius-base': '4px',
    },
  })
);
```

## 4. 自定义组件

### 4.1 基础组件

在公共组件目录维护：

```
src/components/
├── Button/
│   ├── index.tsx
│   ├── index.module.css
│   └── index.md
├── Input/
├── Select/
└── Modal/
```

### 4.2 组件模板

```typescript
import React from 'react';
import styles from './index.module.css';

export interface ButtonProps {
  type?: 'primary' | 'secondary' | 'default';
  size?: 'small' | 'medium' | 'large';
  children: React.ReactNode;
  onClick?: () => void;
}

export function Button({ type = 'default', size = 'medium', children, onClick }: ButtonProps) {
  return (
    <button 
      className={`${styles.button} ${styles[type]} ${styles[size]}`}
      onClick={onClick}
    >
      {children}
    </button>
  );
}
```

## 5. 图标使用

### 5.1 图标库

- Ant Design Icons
- Font Awesome
- 自定义SVG图标

### 5.2 使用方式

```typescript
import { IconName } from '@ant-design/icons-vue';

<Icon component={CustomSvg} />
```

## 6. 表单组件

### 6.1 表单布局

```tsx
<Form layout="vertical">
  <Form.Item label="用户名" name="username" rules={[{ required: true }]}>
    <Input />
  </Form.Item>
  <Form.Item label="邮箱" name="email" rules={[{ type: 'email' }]}>
    <Input />
  </Form.Item>
</Form>
```

### 6.2 表单验证

```typescript
const rules = {
  username: [
    { required: true, message: '请输入用户名' },
    { min: 3, message: '用户名至少3个字符' },
  ],
  email: [
    { required: true, message: '请输入邮箱' },
    { type: 'email', message: '邮箱格式不正确' },
  ],
};
```

## 7. 表格组件

### 7.1 基本使用

```tsx
<Table 
  columns={columns} 
  dataSource={data} 
  pagination={{
    current: 1,
    pageSize: 20,
    total: 100,
  }}
/>
```

### 7.2 列定义

```typescript
const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '状态', dataIndex: 'status', key: 'status', 
    render: (status) => status === 1 ? '正常' : '禁用' 
  },
  { title: '操作', key: 'action', 
    render: (_, record) => (
      <Button onClick={() => handleEdit(record.id)}>编辑</Button>
    )
  },
];
```