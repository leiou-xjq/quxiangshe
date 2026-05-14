const WS_URL = 'ws://localhost:8080/ws/chat'
const RECONNECT_DELAY = 3000
const TOKEN_REFRESH_URL = '/api/auth/refresh'

function parseJwt(token) {
  try {
    const base64Url = token.split('.')[1]
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    const jsonPayload = decodeURIComponent(atob(base64).split('').map(c =>
      '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)
    ).join(''))
    return JSON.parse(jsonPayload)
  } catch (e) {
    return null
  }
}

function isTokenExpired(token) {
  const payload = parseJwt(token)
  if (!payload || !payload.exp) return true
  return payload.exp * 1000 < Date.now()
}

async function refreshAccessToken() {
  const refreshToken = localStorage.getItem('refreshToken')
  if (!refreshToken) return null

  try {
    const res = await fetch(TOKEN_REFRESH_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken })
    })
    const json = await res.json()
    if (json.success && json.data) {
      localStorage.setItem('accessToken', json.data.accessToken)
      if (json.data.refreshToken) {
        localStorage.setItem('refreshToken', json.data.refreshToken)
      }
      return json.data.accessToken
    }
  } catch (e) {
    console.error('Token refresh failed:', e)
  }
  return null
}

class WebSocketManager {
  constructor() {
    this.ws = null
    this.token = null
    this.reconnectTimer = null
    this.listeners = new Map()
    this.isConnecting = false
    this.isConnected = false
  }

  get connected() {
    return this.ws && this.ws.readyState === WebSocket.OPEN
  }

  async connect(token) {
    if (this.connected || this.isConnecting) {
      console.log('WebSocket already connected or connecting')
      return
    }

    if (isTokenExpired(token)) {
      console.log('Token expired, refreshing...')
      const newToken = await refreshAccessToken()
      if (!newToken) {
        console.warn('Token refresh failed, cannot connect WebSocket')
        return
      }
      token = newToken
    }

    this.token = token
    this.isConnecting = true

    try {
      this.ws = new WebSocket(`${WS_URL}?token=${token}`)

      this.ws.onopen = () => {
        console.log('WebSocket connected')
        this.isConnecting = false
        this.isConnected = true
        this.emit('connected', {})
      }

      this.ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          console.log('WebSocket message:', data)
          this.handleMessage(data)
        } catch (e) {
          console.error('Parse message error:', e)
        }
      }

      this.ws.onerror = (error) => {
        console.error('WebSocket error:', error)
        this.isConnecting = false
        this.emit('error', { error })
      }

      this.ws.onclose = () => {
        console.log('WebSocket closed')
        this.isConnected = false
        this.isConnecting = false
        this.emit('disconnected', {})
        this.scheduleReconnect()
      }
    } catch (e) {
      console.error('WebSocket connection error:', e)
      this.isConnecting = false
      this.scheduleReconnect()
    }
  }

  handleMessage(data) {
    const type = data.type
    switch (type) {
      case 'new_message':
        this.emit('message', data.data)
        break
      case 'unread_update':
        this.emit('unread', data.count)
        break
      case 'notification':
        window.dispatchEvent(new CustomEvent('push-notification', { detail: data.data }))
        break
      default:
        console.log('Unknown message type:', type)
    }
  }

  send(type, payload) {
    if (!this.connected) {
      console.warn('WebSocket not connected, attempting to reconnect...')
      if (this.token && !this.isConnecting) {
        this.connect(this.token)
      }
      return false
    }

    const message = JSON.stringify({ type, ...payload })

    try {
      this.ws.send(message)
      return true
    } catch (e) {
      console.error('Send message error:', e)
      return false
    }
  }

  sendMessage(receiverId, messageType, content, imageUrl = null) {
    return this.send('message', {
      receiverId,
      messageType,
      content,
      imageUrl
    })
  }

  disconnect() {
    this.stopReconnect()
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
    this.isConnected = false
    this.isConnecting = false
  }

  scheduleReconnect() {
    if (this.reconnectTimer) return
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null
      if (this.token) {
        console.log('Attempting to reconnect...')
        this.connect(this.token)
      }
    }, RECONNECT_DELAY)
  }

  stopReconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
  }

  on(event, callback) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, [])
    }
    this.listeners.get(event).push(callback)
  }

  off(event, callback) {
    if (!this.listeners.has(event)) return
    const callbacks = this.listeners.get(event)
    const index = callbacks.indexOf(callback)
    if (index > -1) {
      callbacks.splice(index, 1)
    }
  }

  emit(event, data) {
    if (!this.listeners.has(event)) return
    this.listeners.get(event).forEach(callback => callback(data))
  }
}

export const wsManager = new WebSocketManager()

export default wsManager