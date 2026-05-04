# 状态管理方案

## 概述

本文档定义了前端状态管理的标准规范，确保状态的可预测性和可维护性。

## 状态分类

### 本地状态

```typescript
// 组件内部状态（React）
const [count, setCount] = useState(0);

// Vue
const count = ref(0);
```

### 共享状态

```typescript
// 用户状态
interface UserState {
    id: number | null;
    username: string;
    avatar: string;
    isLoggedIn: boolean;
}

// 购物车状态
interface CartState {
    items: CartItem[];
    totalAmount: number;
    itemCount: number;
}
```

## 状态管理方案

### React 状态管理

```typescript
// 使用 Zustand
import { create } from 'zustand';

interface UserStore {
    user: User | null;
    isLoggedIn: boolean;
    login: (user: User) => void;
    logout: () => void;
}

export const useUserStore = create<UserStore>((set) => ({
    user: null,
    isLoggedIn: false,
    login: (user) => set({ user, isLoggedIn: true }),
    logout: () => set({ user: null, isLoggedIn: false }),
}));

// 使用
const { user, login } = useUserStore();
```

### Vue 状态管理

```typescript
// 使用 Pinia
import { defineStore } from 'pinia';

export const useUserStore = defineStore('user', {
    state: () => ({
        user: null as User | null,
        isLoggedIn: false,
    }),
    actions: {
        login(user: User) {
            this.user = user;
            this.isLoggedIn = true;
        },
        logout() {
            this.user = null;
            this.isLoggedIn = false;
        },
    },
});
```

## 状态分层

```
┌─────────────────────────────────────┐
│         页面级状态                   │
│    (组件内部 useState/useRef)       │
├─────────────────────────────────────┤
│         业务模块状态                 │
│    (Zustand/Pinia 模块)             │
├─────────────────────────────────────┤
│         全局共享状态                 │
│   (用户、主题、权限等)               │
├─────────────────────────────────────┤
│         服务端状态                   │
│      (TanStack Query/SWR)           │
└─────────────────────────────────────┘
```

## 数据获取

```typescript
// 使用 React Query
import { useQuery } from '@tanstack/react-query';

function UserList() {
    const { data, isLoading, error } = useQuery({
        queryKey: ['users'],
        queryFn: () => api.getUsers(),
        staleTime: 5 * 60 * 1000, // 5分钟
    });
    
    if (isLoading) return <Loading />;
    if (error) return <Error />;
    
    return <List users={data} />;
}
```

## 持久化

```typescript
// 状态持久化
import { persist } from 'zustand/middleware';

export const useUserStore = create(
    persist(
        (set) => ({
            user: null,
            login: (user) => set({ user }),
        }),
        {
            name: 'user-storage', // localStorage key
        }
    )
);
```