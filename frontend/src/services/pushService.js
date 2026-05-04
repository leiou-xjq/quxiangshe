import Pusher from 'pusher-js'

const PUSHER_KEY = '5039ccc58e39370fb8e0'
const PUSHER_CLUSTER = 'ap3'

let pusher = null
let subscribedChannels = new Map()

export const initPusher = (userId) => {
  if (!userId) return

  if (pusher) {
    pusher.disconnect()
  }

  pusher = new Pusher(PUSHER_KEY, {
    cluster: PUSHER_CLUSTER,
    authEndpoint: '/api/pusher/auth'
  })

  subscribeToUserChannel(userId)

  pusher.connection.bind('connected', () => {
    console.log('Pusher connected')
  })

  pusher.connection.bind('disconnected', () => {
    console.log('Pusher disconnected')
  })
}

export const subscribeToUserChannel = (userId) => {
  if (!pusher || !userId) return

  const channelName = `private-user-${userId}`

  if (subscribedChannels.has(channelName)) {
    return
  }

  const channel = pusher.subscribe(channelName)

  channel.bind('pusher:subscription_succeeded', () => {
    console.log('Subscribed to channel:', channelName)
  })

  channel.bind('pusher:subscription_error', (error) => {
    console.error('Subscription error:', error)
  })

  channel.bind('notification', (data) => {
    console.log('Received notification:', data)
    window.dispatchEvent(new CustomEvent('push-notification', { detail: data }))
  })

  subscribedChannels.set(channelName, channel)
}

export const unsubscribeFromUserChannel = (userId) => {
  if (!pusher || !userId) return

  const channelName = `private-user-${userId}`
  pusher.unsubscribe(channelName)
  subscribedChannels.delete(channelName)
}

export const disconnectPusher = () => {
  if (pusher) {
    pusher.disconnect()
    pusher = null
    subscribedChannels.clear()
  }
}

export default {
  initPusher,
  subscribeToUserChannel,
  unsubscribeFromUserChannel,
  disconnectPusher
}