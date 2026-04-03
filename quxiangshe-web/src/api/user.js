import request from '../utils/request'

export const userApi = {
  // 获取当前用户信息
  getCurrentUser() {
    return request.get('/user/me')
  },

  // 获取指定用户信息
  getUserProfile(userId) {
    return request.get(`/user/${userId}/profile`)
  }
}
