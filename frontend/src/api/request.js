import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const service = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 是否正在刷新Token
let isRefreshing = false
// 待重试的请求队列
let requests = []

// 请求拦截器
service.interceptors.request.use(
  config => {
    const userStore = useUserStore()
    if (userStore.accessToken) {
      config.headers['Authorization'] = `Bearer ${userStore.accessToken}`
    }
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    const res = response.data
    
    if (res.code === 0) {
      return res
    }
    
    // 处理业务错误
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  async error => {
    const userStore = useUserStore()
    
    // 判断是否为429限流错误
    if (error.response && error.response.status === 429) {
      const message = error.response.data?.message || '请求过于频繁，请稍后再试'
      ElMessage.warning(message)
      return Promise.reject(error)
    }
    
    // 判断是否为401错误
    if (error.response && error.response.status === 401) {
      // 如果没有refreshToken，直接跳转登录
      if (!userStore.refreshToken) {
        ElMessage.error('登录已过期，请重新登录')
        userStore.logout()
        window.location.href = '/login'
        return Promise.reject(error)
      }
      
      // 如果正在刷新Token，将请求加入队列等待
      if (isRefreshing) {
        return new Promise(resolve => {
          requests.push(() => {
            resolve(service(error.config))
          })
        })
      }
      
      // 开始刷新Token
      isRefreshing = true
      
      try {
        // 调用刷新接口
        const res = await axios.post('/api/auth/refresh', {
          refreshToken: userStore.refreshToken
        }, {
          headers: {
            'Content-Type': 'application/json'
          }
        })
        
        if (res.data.code === 0) {
          // 刷新成功，更新Token
          const newAccessToken = res.data.data.accessToken
          const newRefreshToken = res.data.data.refreshToken
          
          userStore.accessToken = newAccessToken
          userStore.refreshToken = newRefreshToken
          localStorage.setItem('accessToken', newAccessToken)
          localStorage.setItem('refreshToken', newRefreshToken)
          
          // 重试失败的请求
          requests.forEach(cb => cb())
          requests = []
          
          // 重试当前请求
          error.config.headers['Authorization'] = `Bearer ${newAccessToken}`
          return service(error.config)
        } else {
          // 刷新失败
          throw new Error(res.data.message)
        }
      } catch (e) {
        // 刷新失败，清除Token并跳转登录
        ElMessage.error('登录已过期，请重新登录')
        userStore.logout()
        window.location.href = '/login'
        return Promise.reject(error)
      } finally {
        isRefreshing = false
      }
    }
    
    // 处理其他HTTP错误
    if (error.response) {
      const status = error.response.status
      
      if (status === 403) {
        ElMessage.error('没有权限')
      } else if (status === 404) {
        ElMessage.error('请求的资源不存在')
      } else if (status >= 500) {
        ElMessage.error('服务器错误')
      } else {
        ElMessage.error(error.response.data?.message || '请求失败')
      }
    } else {
      ElMessage.error('网络错误')
    }
    
    return Promise.reject(error)
  }
)

export default service
