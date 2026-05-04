import request from './request'

// 获取通知列表
export function getNotifications(size = 20, offset = 0) {
  return request({
    url: '/notification/list',
    method: 'get',
    params: { size, offset }
  })
}

// 获取未读数量
export function getUnreadCount() {
  return request({
    url: '/notification/unread-count',
    method: 'get'
  })
}

// 标记已读
export function markAsRead(id) {
  return request({
    url: `/notification/read/${id}`,
    method: 'put'
  })
}

// 全部标记已读
export function markAllAsRead() {
  return request({
    url: '/notification/read-all',
    method: 'put'
  })
}

// 删除通知
export function deleteNotification(id) {
  return request({
    url: `/notification/${id}`,
    method: 'delete'
  })
}

// 发送重置密码验证码
export function sendResetCode(phone) {
  return request({
    url: '/auth/reset-code',
    method: 'post',
    data: { phone }
  })
}

// 重置密码
export function resetPassword(data) {
  return request({
    url: '/auth/reset-password',
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
