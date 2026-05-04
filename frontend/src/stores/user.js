import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, register as registerApi, logout as logoutApi, getCurrentUser as getUserApi, emailLogin as emailLoginApi } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  // 状态 - accessToken存储在localStorage
  const accessToken = ref(localStorage.getItem('accessToken') || '')
  const refreshToken = ref(localStorage.getItem('refreshToken') || '')
  const userInfo = ref(null)
  const unreadCount = ref(0)
  
  // 计算属性
  const isLoggedIn = computed(() => !!accessToken.value)
  
  // 登录
  async function login(credentials) {
    const res = await loginApi(credentials)
    setTokens(res.data)
    return res
  }
  
  // 设置Token（用于验证码登录）
  function setTokens(data) {
    accessToken.value = data.accessToken
    refreshToken.value = data.refreshToken
    userInfo.value = data.user
    
    localStorage.setItem('accessToken', accessToken.value)
    localStorage.setItem('refreshToken', refreshToken.value)
  }
  
  // 邮箱验证码登录
  async function emailLogin(credentials) {
    const res = await emailLoginApi(credentials)
    setTokens(res.data)
    return res
  }
  
  // 注册
  async function register(data) {
    const res = await registerApi(data)
    return res
  }
  
  // 登出
  async function logout() {
    try {
      await logoutApi()
    } catch (e) {
      console.error('登出失败:', e)
    } finally {
      // 清除状态和localStorage
      accessToken.value = ''
      refreshToken.value = ''
      userInfo.value = null
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
    }
  }
  
  // 获取用户信息
  async function fetchUserInfo() {
    if (!accessToken.value) return null
    
    try {
      const res = await getUserApi()
      userInfo.value = res.data
      return res.data
    } catch (e) {
      console.error('获取用户信息失败:', e)
      return null
    }
  }
  
  // 更新accessToken（刷新成功后调用）
  function updateToken(newAccessToken, newRefreshToken) {
    accessToken.value = newAccessToken
    refreshToken.value = newRefreshToken
    localStorage.setItem('accessToken', newAccessToken)
    localStorage.setItem('refreshToken', newRefreshToken)
  }
  
  // 更新未读通知数
  function updateUnreadCount(count) {
    unreadCount.value = count
  }
  
  return {
    accessToken,
    refreshToken,
    userInfo,
    unreadCount,
    isLoggedIn,
    login,
    emailLogin,
    register,
    logout,
    fetchUserInfo,
    updateToken,
    updateUnreadCount,
    setTokens
  }
})
