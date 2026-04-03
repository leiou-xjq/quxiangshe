import { defineStore } from 'pinia'
import { logout as apiLogout, refreshToken as apiRefreshToken } from '@/api/auth'
import { userApi } from '@/api/user'

export const useUserStore = defineStore('user', {
  state: () => ({
    userId: localStorage.getItem('userId') || null,
    username: localStorage.getItem('username') || null,
    nickname: localStorage.getItem('nickname') || null,
    avatarUrl: localStorage.getItem('avatarUrl') || null,
    token: localStorage.getItem('accessToken') || null,
    refreshToken: localStorage.getItem('refreshToken') || null
  }),

  actions: {
    setUser(userId, username, token, nickname = null, avatarUrl = null, refreshToken = null) {
      this.userId = userId
      this.username = username
      this.nickname = nickname || username
      this.avatarUrl = avatarUrl
      this.token = token
      this.refreshToken = refreshToken
      localStorage.setItem('userId', userId)
      localStorage.setItem('username', username)
      localStorage.setItem('nickname', nickname || username)
      localStorage.setItem('avatarUrl', avatarUrl || '')
      localStorage.setItem('accessToken', token)
      if (refreshToken) {
        localStorage.setItem('refreshToken', refreshToken)
      }
    },

    async logout() {
      try {
        await apiLogout()
      } catch (e) {
      }
      this.clearUser()
    },

    clearUser() {
      this.userId = null
      this.username = null
      this.nickname = null
      this.avatarUrl = null
      this.token = null
      this.refreshToken = null
      localStorage.removeItem('userId')
      localStorage.removeItem('username')
      localStorage.removeItem('nickname')
      localStorage.removeItem('avatarUrl')
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
    },

    isLoggedIn() {
      return !!this.token
    },

    async fetchCurrentUser() {
      try {
        const res = await userApi.getCurrentUser()
        if (res.code === 0 || res.code === 200) {
          const user = res.data
          this.setUser(user.userId, user.username, this.token, user.nickname, user.avatarUrl, this.refreshToken)
          return user
        }
      } catch (e) {
        console.error('获取用户信息失败', e)
      }
      return null
    }
  }
})
