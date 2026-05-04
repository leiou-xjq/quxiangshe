import request from './request'

export function getSessionList(params) {
  return request.get('/message/sessions', { params })
}

export function getSessionDetail(sessionId, params) {
  return request.get(`/message/sessions/${sessionId}`, { params })
}

export function getSessionInfo(sessionId) {
  return request.get(`/message/sessions/${sessionId}/info`)
}

export function createSession(targetUserId) {
  return request.post('/message/sessions', { targetUserId })
}

export function sendMessage(data) {
  return request.post('/message/send', data)
}

export function recallMessage(messageId) {
  return request.put(`/message/recall/${messageId}`)
}

export function deleteMessage(messageId) {
  return request.delete(`/message/${messageId}`)
}

export function markSessionRead(sessionId) {
  return request.put(`/message/sessions/${sessionId}/read`)
}

export function getUnreadCount() {
  return request.get('/message/unread')
}