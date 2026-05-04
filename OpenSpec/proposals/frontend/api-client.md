# API 调用规范

## 概述

本文档定义了前端API调用的标准规范，确保请求的统一性和错误处理的规范性。

## API 封装

### 请求实例

```typescript
// request.ts
import axios from 'axios';

const request = axios.create({
    baseURL: '/api/v1',
    timeout: 30000,
    headers: {
        'Content-Type': 'application/json',
    },
});

// 请求拦截器
request.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

// 响应拦截器
request.interceptors.response.use(
    (response) => response.data,
    (error) => {
        if (error.response?.status === 401) {
            // 处理未授权
            localStorage.removeItem('token');
            window.location.href = '/login';
        }
        return Promise.reject(error);
    }
);

export default request;
```

### API 模块

```typescript
// userAPI.ts
import request from './request';

export interface User {
    id: number;
    username: string;
    email: string;
}

export const userAPI = {
    list: (params: { page: number; pageSize: number }) => 
        request.get<{ data: User[]; total: number }>('/users', { params }),
    
    get: (id: number) => 
        request.get<User>(`/users/${id}`),
    
    create: (data: Partial<User>) => 
        request.post<User>('/users', data),
    
    update: (id: number, data: Partial<User>) => 
        request.put<User>(`/users/${id}`, data),
    
    delete: (id: number) => 
        request.delete(`/users/${id}`),
};
```

## 错误处理

```typescript
// 统一的错误处理
interface ApiError {
    code: number;
    message: string;
    details?: Record<string, string[]>;
}

function handleApiError(error: unknown): string {
    if (axios.isAxiosError(error)) {
        const { response } = error;
        if (response) {
            const data = response.data as ApiError;
            return data.message || '请求失败';
        }
        return '网络错误';
    }
    return '未知错误';
}

// 使用示例
async function fetchUsers() {
    try {
        const data = await userAPI.list({ page: 1, pageSize: 20 });
        return data;
    } catch (error) {
        const message = handleApiError(error);
        Toast.error(message);
        throw error;
    }
}
```

## 请求取消

```typescript
// 取消重复请求
import axiosCancel from 'axios-cancel';

axiosCancel(axios);

function getUser(id: number) {
    // 自动取消相同请求
    return request.get(`/users/${id}`, {
        cancelToken: axiosCancel.cancelToken(`user-${id}`),
    });
}

// 批量取消
axiosCancel.cancelAll('user-list');
```

## 缓存策略

```typescript
// 请求缓存
import { useQuery } from '@tanstack/react-query';

function UserDetail({ userId }: { userId: number }) {
    const { data, isLoading } = useQuery({
        queryKey: ['user', userId],
        queryFn: () => userAPI.get(userId),
        staleTime: 5 * 60 * 1000, // 缓存5分钟
        cacheTime: 30 * 60 * 1000, // 30分钟后清理
    });
    
    return isLoading ? <Skeleton /> : <UserInfo user={data} />;
}
```

## 最佳实践

```typescript
// 1. 使用 async/await
const users = await userAPI.list({ page: 1 });

// 2. 显示加载状态
const [loading, setLoading] = useState(false);
async function fetch() {
    setLoading(true);
    try {
        const data = await userAPI.get(id);
        setData(data);
    } finally {
        setLoading(false);
    }
}

// 3. 错误边界
if (error) return <ErrorView error={error} />;

// 4. 清理副作用
useEffect(() => {
    const controller = new AbortController();
    fetchData(controller.signal);
    return () => controller.abort();
}, []);
```