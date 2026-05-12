<template>
  <div class="chat-page">
    <div class="chat-header">
      <el-button text @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
      </el-button>
      <span class="target-name">{{ targetUser?.nickname }}</span>
    </div>
    
    <div class="message-list" ref="messageListRef">
      <div 
        v-for="msg in messages" 
        :key="msg.id" 
        :class="['message-item', Number(msg.senderId) === currentUserId ? 'my-message' : 'other-message']"
      >
        <el-avatar :src="Number(msg.senderId) === currentUserId ? myAvatar : targetUser?.avatar" :size="36" />
        <div class="message-content">
          <div class="message-bubble">
            <template v-if="msg.isRecalled === 1">
              <span class="recalled">消息已撤回</span>
            </template>
            <template v-else>
              <img v-if="msg.messageType === 2" :src="msg.imageUrl" class="message-image" />
              <img v-else-if="msg.messageType === 3" :src="msg.imageUrl" class="message-emoji" />
              <span v-else>{{ msg.content }}</span>
            </template>
          </div>
          <div class="message-time">{{ formatTime(msg.createdAt) }}</div>
        </div>
        <div class="message-actions" v-if="Number(msg.senderId) === currentUserId && msg.isRecalled !== 1">
          <el-dropdown @command="handleAction($event, msg)">
            <el-button text size="small">
              <el-icon><MoreFilled /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="recall">撤回</el-dropdown-item>
                <el-dropdown-item command="delete">删除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </div>
    
    <div class="chat-input">
      <input
        ref="fileInput"
        type="file"
        accept="image/*"
        style="display:none"
        @change="handleImageUpload"
      />
      <el-input 
        v-model="inputText" 
        placeholder="输入消息..." 
        :disabled="uploading"
        @keyup.enter="sendText"
      />
      <el-button @click="fileInput.click()" :disabled="uploading" :loading="uploading">
        <el-icon><Picture /></el-icon>
      </el-button>
      <el-button @click="showEmojiPanel = !showEmojiPanel">
        <el-icon><Star /></el-icon>
      </el-button>
      <el-button type="primary" @click="sendText" :disabled="!inputText.trim()">发送</el-button>
    </div>
    
    <div v-if="showEmojiPanel" class="emoji-panel">
      <div class="emoji-list">
        <img 
          v-for="(emoji, idx) in emojiList" 
          :key="idx" 
          :src="`https://picsum.photos/30?random=${idx}`" 
          class="emoji-item"
          @click="sendEmoji(emoji)"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getSessionDetail, getSessionInfo, recallMessage, deleteMessage, markSessionRead } from '@/api/message'
import { uploadImage } from '@/api/note'
import { getUserInfo } from '@/api/auth'
import { wsManager } from '@/utils/websocket'
import { ElMessage } from 'element-plus'
import { ArrowLeft, MoreFilled, Star, Picture } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const sessionId = route.params.sessionId

// 从 localStorage 直接获取用户ID（最可靠）
const currentUserId = computed(() => {
  try {
    const info = localStorage.getItem('userInfo')
    if (info) {
      const user = JSON.parse(info)
      return user?.id ? Number(user.id) : null
    }
  } catch {}
  return userStore.userInfo?.id ? Number(userStore.userInfo.id) : null
})

const myAvatar = computed(() => {
  try {
    const info = localStorage.getItem('userInfo')
    if (info) {
      const user = JSON.parse(info)
      return user?.avatar || 'https://picsum.photos/100'
    }
  } catch {}
  return userStore.userInfo?.avatar || 'https://picsum.photos/100'
})

const token = localStorage.getItem('accessToken')

const messages = ref([])
const targetUser = ref({ id: null, nickname: '用户', avatar: 'https://picsum.photos/100' })
const inputText = ref('')
const messageListRef = ref(null)
const showEmojiPanel = ref(false)
const fileInput = ref(null)
const uploading = ref(false)

const emojiList = Array.from({ length: 20 }, (_, i) => `https://picsum.photos/30?random=${i}`)

async function handleImageUpload(e) {
  const file = e.target.files[0]
  if (!file) return

  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片不能超过5MB')
    fileInput.value.value = ''
    return
  }

  uploading.value = true
  try {
    const res = await uploadImage(file)
    const url = res.data
    if (!url) {
      ElMessage.error('图片上传失败')
      return
    }

    const tempMsg = {
      id: 'temp_' + Date.now(),
      senderId: currentUserId.value,
      messageType: 2,
      content: '',
      imageUrl: url,
      createdAt: new Date().toISOString(),
      isRecalled: 0,
      isTemp: true
    }
    messages.value.push(tempMsg)
    scrollToBottom()

    wsManager.sendMessage(targetUser.value.id, 2, '', url)
  } catch (e) {
    ElMessage.error('图片上传失败')
  } finally {
    uploading.value = false
    fileInput.value.value = ''
  }
}

async function loadMessages() {
  try {
    const res = await getSessionDetail(sessionId, { size: 50, offset: 0 })
    messages.value = (res.data || []).reverse()
    scrollToBottom()
  } catch (e) {
    console.error('获取消息失败', e)
  }
}

async function loadSessionInfo() {
  try {
    const res = await getSessionInfo(sessionId)
    if (res.data) {
      const targetId = res.data.targetUserId
      targetUser.value.id = targetId
      try {
        const userRes = await getUserInfo(targetId)
        if (userRes.data) {
          targetUser.value.nickname = userRes.data.nickname || userRes.data.username || '用户'
          targetUser.value.avatar = userRes.data.avatar || 'https://picsum.photos/100'
        }
      } catch (e) {
        // 获取用户信息失败，使用默认值
      }
    }
  } catch (e) {
    console.error('获取会话信息失败', e)
  }
}

function formatTime(time) {
  if (!time) return ''
  return new Date(time).toLocaleTimeString()
}

function sendText() {
  if (!inputText.value.trim()) return
  if (!targetUser.value.id) {
    ElMessage.error('会话信息错误')
    return
  }

  const content = inputText.value.trim()
  const tempMsg = {
    id: 'temp_' + Date.now(),
    senderId: currentUserId.value,
    messageType: 1,
    content,
    createdAt: new Date().toISOString(),
    isRecalled: 0,
    isTemp: true
  }
  messages.value.push(tempMsg)
  inputText.value = ''
  scrollToBottom()

  const success = wsManager.sendMessage(targetUser.value.id, 1, content, null)
  if (!success) {
    ElMessage.error('发送失败')
  }
}

function sendEmoji(emoji) {
  if (!targetUser.value.id) {
    ElMessage.error('会话信息错误')
    return
  }

  const tempMsg = {
    id: 'temp_' + Date.now(),
    senderId: currentUserId.value,
    messageType: 3,
    content: '',
    imageUrl: emoji,
    createdAt: new Date().toISOString(),
    isRecalled: 0,
    isTemp: true
  }
  messages.value.push(tempMsg)
  showEmojiPanel.value = false
  scrollToBottom()

  const success = wsManager.sendMessage(targetUser.value.id, 3, '', emoji)
  if (!success) {
    ElMessage.error('发送失败')
  }
}

async function handleAction(command, msg) {
  if (command === 'recall') {
    const diff = (Date.now() - new Date(msg.createdAt).getTime()) / 1000
    if (diff > 120) {
      ElMessage.warning('超过2分钟无法撤回')
      return
    }
    try {
      await recallMessage(msg.id)
      msg.isRecalled = 1
      ElMessage.success('已撤回')
    } catch (e) {
      ElMessage.error('撤回失败')
    }
  } else if (command === 'delete') {
    try {
      await deleteMessage(msg.id)
      msg.isDeletedSender = 1
      ElMessage.success('已删除')
    } catch (e) {
      ElMessage.error('删除失败')
    }
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

function goBack() {
  router.back()
}

function handleNewMessage(message) {
  console.log('Received new message:', message, 'sessionId:', sessionId)
  // 转换为字符串比较，避免类型不一致
  const msgSessionId = String(message.sessionId)
  const currentSessionId = String(sessionId)
  
  if (msgSessionId === currentSessionId) {
    if (message.senderId === currentUserId.value) {
      const idx = messages.value.findIndex(m =>
        m.isTemp && m.messageType === message.messageType &&
        (message.messageType === 2 ? m.imageUrl === message.imageUrl : m.content === message.content)
      )
      if (idx !== -1) {
        messages.value.splice(idx, 1)
      }
    }
    messages.value.push(message)
    scrollToBottom()
  }
}

onMounted(async () => {
  // 确保 userInfo 加载完成
  if (!userStore.userInfo) {
    await userStore.fetchUserInfo()
  }
  
  loadSessionInfo()
  loadMessages().then(() => {
    markSessionRead(sessionId).catch(() => {})
  })

  if (token) {
    wsManager.connect(token)
  }

  wsManager.on('message', handleNewMessage)
})

onUnmounted(() => {
  wsManager.off('message', handleNewMessage)
})
</script>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 60px);
  max-width: 800px;
  margin: 0 auto;
}

.chat-header {
  display: flex;
  align-items: center;
  padding: 15px;
  border-bottom: 1px solid #f0f0f0;
}

.target-name {
  margin-left: 15px;
  font-weight: 500;
  font-size: 16px;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 15px;
}

.message-item {
  display: flex;
  margin-bottom: 15px;
  align-items: flex-start;
}

.my-message {
  flex-direction: row;
}

.other-message {
  flex-direction: row-reverse;
}

.message-content {
  max-width: 70%;
  margin: 0 10px;
}

.message-bubble {
  padding: 10px 15px;
  border-radius: 8px;
  background: #f5f5f5;
  word-break: break-word;
}

.my-message .message-bubble {
  background: #e6f7ff;
}

.message-image {
  max-width: 200px;
  border-radius: 4px;
}

.message-emoji {
  width: 40px;
  height: 40px;
}

.message-time {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}

.my-message .message-time {
  text-align: right;
}

.recalled {
  color: #999;
  font-style: italic;
}

.chat-input {
  display: flex;
  align-items: center;
  padding: 15px;
  border-top: 1px solid #f0f0f0;
  gap: 10px;
}

.chat-input .el-input {
  flex: 1;
}

.emoji-panel {
  position: absolute;
  bottom: 70px;
  left: 50%;
  transform: translateX(-50%);
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 10px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.emoji-list {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 5px;
}

.emoji-item {
  cursor: pointer;
  padding: 5px;
  border-radius: 4px;
}

.emoji-item:hover {
  background: #f5f5f5;
}
</style>