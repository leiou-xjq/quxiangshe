import request from './request'

// 关注用户
export function followUser(userId) {
  return request({
    url: `/follow/${userId}`,
    method: 'post'
  })
}

// 取消关注
export function unfollowUser(userId) {
  return request({
    url: `/follow/${userId}`,
    method: 'delete'
  })
}

// 获取关注列表（游标分页）
export function getFollowingList(userId = null, cursor = null, size = 20) {
  return request({
    url: '/follow/following',
    params: { userId, cursor, size }
  })
}

// 获取粉丝列表（游标分页）
export function getFollowersList(cursor = null, size = 20, userId = null) {
  return request({
    url: '/follow/followers',
    params: { cursor, size, userId }
  })
}

// 获取关注状态
export function getFollowStatus(userId) {
  return request({
    url: `/follow/status/${userId}`
  })
}

// 获取用户关注数
export function getFollowingCount(userId) {
  return request({
    url: `/follow/count/following/${userId}`
  })
}

// 获取用户粉丝数
export function getFollowersCount(userId) {
  return request({
    url: `/follow/count/followers/${userId}`
  })
}