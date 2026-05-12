import request from './request'

// 获取笔记列表
export function getNoteList(page = 1, size = 10) {
  return request({
    url: '/note/list',
    params: { page, size }
  })
}

// 获取笔记详情
export function getNoteDetail(noteId) {
  return request({
    url: `/note/${noteId}`
  })
}

// 发布笔记
export function createNote(data) {
  return request({
    url: '/note/create',
    method: 'post',
    data
  })
}

// 删除笔记
export function deleteNote(noteId) {
  return request({
    url: `/note/${noteId}`,
    method: 'delete'
  })
}

// 点赞笔记
export function likeNote(noteId) {
  return request({
    url: `/note/${noteId}/like`,
    method: 'post'
  })
}

// 取消点赞
export function unlikeNote(noteId) {
  return request({
    url: `/note/${noteId}/like`,
    method: 'delete'
  })
}

// 收藏笔记
export function favoriteNote(noteId) {
  return request({
    url: `/note/${noteId}/favorite`,
    method: 'post'
  })
}

// 取消收藏
export function unfavoriteNote(noteId) {
  return request({
    url: `/note/${noteId}/favorite`,
    method: 'delete'
  })
}

// 获取评论列表
export function getCommentList(noteId, size = 20, cursor = null) {
  return request({
    url: `/note/${noteId}/comments`,
    params: { size, cursor }
  })
}

// 获取子评论列表（分页）
export function getChildComments(noteId, rootId, size = 10, cursor = null) {
  return request({
    url: `/comment/sorted/${noteId}/children/${rootId}`,
    params: { size, cursor, sort: 'hottest' }
  })
}

// 添加评论
export function addComment(data) {
  return request({
    url: '/note/comment',
    method: 'post',
    data
  })
}

// 删除评论
export function deleteComment(commentId, noteId) {
  return request({
    url: `/note/comment/${commentId}`,
    method: 'delete',
    params: { noteId }
  })
}

// 获取AI回复建议
export function getAiReplySuggestions(commentId) {
  return request({
    url: `/note/comment/${commentId}/ai-suggestions`,
    method: 'get'
  })
}

// 上传图片
export function uploadImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/note/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 上传视频
export function uploadVideo(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/note/upload-video',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 获取我的笔记
export function getMyNotes(page = 1, size = 10) {
  return request({
    url: '/note/my',
    params: { page, size }
  })
}

// 获取指定用户笔记
export function getUserNotes(userId, page = 1, size = 10) {
  return request({
    url: `/note/notes-by/${userId}`,
    params: { page, size }
  })
}

// 获取我的收藏
export function getMyFavorites(page = 1, size = 10) {
  return request({
    url: '/note/favorites',
    params: { page, size }
  })
}

// 获取我的获赞数
export function getMyLikesCount() {
  return request({
    url: '/note/my/likes-count',
    method: 'get'
  })
}

// 获取指定用户获赞数
export function getUserLikesCount(userId) {
  return request({
    url: `/note/user/${userId}/likes-count`,
    method: 'get'
  })
}

// 获取Feed流 (游标分页)
export function getFeed(cursor = '', size = 20) {
  return request({
    url: '/feed',
    params: { cursor, size }
  })
}

// 获取发现精彩列表 (游标分页)
export function getDiscoverFeed(cursor = '', size = 20) {
  return request({
    url: '/note/discover',
    params: { cursor, size }
  })
}

// 获取热门列表 (游标分页)
export function getPopularFeed(cursor = '', size = 20) {
  return request({
    url: '/note/popular',
    params: { cursor, size }
  })
}

// 评论点赞
export function likeComment(commentId) {
  return request({
    url: `/comment/sorted/${commentId}/like`,
    method: 'post'
  })
}

// 取消评论点赞
export function unlikeComment(commentId) {
  return request({
    url: `/comment/sorted/${commentId}/like`,
    method: 'delete'
  })
}

// 查询关注Tab是否有更新（红点提示）
export function getFollowHasUpdate() {
  return request({
    url: '/feed/follow-updated'
  })
}

// 清除关注Tab更新标记
export function clearFollowUpdate() {
  return request({
    url: '/feed/follow-updated/clear',
    method: 'post'
  })
}
