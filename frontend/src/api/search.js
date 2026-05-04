import request from './request'

const paramsSerializer = params => {
  const searchParams = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (Array.isArray(value)) {
      value.forEach(item => searchParams.append(key, item))
    } else {
      searchParams.append(key, value)
    }
  }
  return searchParams.toString()
}

// 搜索笔记
export function searchNotes(keyword, size = 20, searchAfter = null, tags = null) {
  const params = { size }
  if (keyword && keyword.trim()) {
    params.keyword = keyword
  }
  if (searchAfter && searchAfter.length > 0) {
    params.searchAfter = searchAfter
  }
  if (tags && tags.length > 0) {
    params.tags = tags
  }
  return request({
    url: '/search/notes',
    params,
    paramsSerializer
  })
}

// 搜索用户
export function searchUsers(keyword, size = 20, searchAfter = null) {
  const params = { size }
  if (keyword && keyword.trim()) {
    params.keyword = keyword
  }
  if (searchAfter && searchAfter.length > 0) {
    params.searchAfter = searchAfter
  }
  return request({
    url: '/search/users',
    params,
    paramsSerializer
  })
}

// 创建索引
export function createSearchIndex() {
  return request({
    url: '/search/index',
    method: 'post'
  })
}

// 全量同步
export function syncAllData() {
  return request({
    url: '/search/sync',
    method: 'post'
  })
}

// 同步单条笔记
export function syncNote(noteId) {
  return request({
    url: `/search/sync/note/${noteId}`,
    method: 'post'
  })
}

// 同步单个用户
export function syncUser(userId) {
  return request({
    url: `/search/sync/user/${userId}`,
    method: 'post'
  })
}

// 删除笔记索引
export function deleteNoteIndex(noteId) {
  return request({
    url: `/search/note/${noteId}`,
    method: 'delete'
  })
}

// 删除用户索引
export function deleteUserIndex(userId) {
  return request({
    url: `/search/user/${userId}`,
    method: 'delete'
  })
}