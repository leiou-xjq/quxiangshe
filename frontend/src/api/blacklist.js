import request from './request'

// 拉黑用户
export function blockUser(userId) {
  return request({
    url: `/blacklist/${userId}`,
    method: 'post'
  })
}

// 取消拉黑
export function unblockUser(userId) {
  return request({
    url: `/blacklist/${userId}`,
    method: 'delete'
  })
}

// 检查是否已拉黑
export function checkBlocked(userId) {
  return request({
    url: `/blacklist/check/${userId}`
  })
}