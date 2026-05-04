import request from '@/utils/request'

export const noteApi = {
  // 获取首页笔记列表
  getHomeNotes(lastId, size = 20) {
    return request.get('/notes', { params: { lastId, size } })
  },

  // 获取用户笔记列表
  getUserNotes(userId, lastId, size = 20) {
    return request.get(`/users/${userId}/notes`, { params: { lastId, size } })
  },

  // 获取笔记详情
  getNoteDetail(noteId) {
    return request.get(`/notes/${noteId}`)
  },

  // 创建笔记
  createNote(data) {
    return request.post('/notes', data)
  },

  // 删除笔记
  deleteNote(noteId) {
    return request.delete(`/notes/${noteId}`)
  },

  // 点赞笔记
  likeNote(noteId) {
    return request.post(`/notes/${noteId}/like`)
  },

  // 取消点赞
  unlikeNote(noteId) {
    return request.delete(`/notes/${noteId}/like`)
  },

  // 收藏笔记
  collectNote(noteId) {
    return request.post(`/notes/${noteId}/collect`)
  },

  // 取消收藏
  uncollectNote(noteId) {
    return request.delete(`/notes/${noteId}/collect`)
  },

  // 搜索笔记
  searchNotes(keyword, category, page = 1, size = 20) {
    return request.get('/search/notes', { params: { keyword, category, page, size } })
  },

  // 搜索用户
  searchUsers(keyword, page = 1, size = 20) {
    return request.get('/search/users', { params: { keyword, page, size } })
  },

  // 统一搜索
  search(keyword, type = 'all', page = 1, size = 20) {
    return request.get('/search', { params: { keyword, type, page, size } })
  },

  // 获取评论列表
  getComments(noteId, lastCommentId, size = 20) {
    return request.get(`/posts/${noteId}/comments`, { params: { lastCommentId, size } })
  },

  // 创建评论
  createComment(noteId, data) {
    return request.post(`/posts/${noteId}/comments`, data)
  },

  // 删除评论
  deleteComment(commentId) {
    return request.delete(`/comments/${commentId}`)
  },

  // 点赞评论
  likeComment(commentId) {
    return request.post(`/comments/${commentId}/like`)
  },

  // 取消点赞评论
  unlikeComment(commentId) {
    return request.delete(`/comments/${commentId}/like`)
  }
}
