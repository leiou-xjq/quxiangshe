import { defineStore } from 'pinia'
import { wsManager } from '@/utils/websocket'

export const useWebSocketStore = defineStore('websocket', {
  state: () => ({
    isConnected: false,
    unreadCount: 0
  }),

  actions: {
    init(token) {
      wsManager.on('connected', () => {
        this.isConnected = true
      })

      wsManager.on('disconnected', () => {
        this.isConnected = false
      })

      wsManager.on('message', (message) => {
        console.log('New message received:', message)
      })

      wsManager.on('unread', (count) => {
        this.unreadCount = count
      })

      wsManager.connect(token)
    },

    disconnect() {
      wsManager.disconnect()
      this.isConnected = false
    },

    sendMessage(sessionId, messageType, content, imageUrl = null) {
      return wsManager.sendMessage(sessionId, messageType, content, imageUrl)
    }
  }
})