import request from './request'

// 登录
export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

// 注册
export function register(data) {
  return request({
    url: '/auth/register',
    method: 'post',
    data
  })
}

// 登出
export function logout() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}

// 刷新Token
export function refreshToken(data) {
  return request({
    url: '/auth/refresh',
    method: 'post',
    data
  })
}

// 获取当前用户信息
export function getCurrentUser() {
  return request({
    url: '/users/me',
    method: 'get'
  })
}

// 更新当前用户信息
export function updateUser(data) {
  return request({
    url: '/users/me',
    method: 'put',
    data
  })
}

// 修改密码
export function changePassword(data) {
  return request({
    url: '/users/me/password',
    method: 'put',
    data
  })
}

// 获取指定用户信息
export function getUserInfo(id) {
  return request({
    url: `/users/${id}`,
    method: 'get'
  })
}

// 发送验证码
export function sendCode(email) {
  return request({
    url: '/auth/send-code',
    method: 'post',
    data: { email }
  })
}

// 邮箱验证码登录
export function emailLogin(data) {
  return request({
    url: '/auth/email-login',
    method: 'post',
    data
  })
}

// 邮箱验证码注册
export function emailRegister(data) {
  return request({
    url: '/auth/email-register',
    method: 'post',
    data
  })
}

// 微信登录
export function wechatLogin(data) {
  return request({
    url: '/auth/wechat-login',
    method: 'post',
    data
  })
}