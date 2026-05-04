<template>
  <div class="messages-page">
    <div class="page-header">
      <el-button text @click="goBack" class="back-btn">
        <el-icon><ArrowLeft /></el-icon>
      </el-button>
      <h2>私信</h2>
    </div>
    
    <div class="session-list" v-if="sessions.length">
      <div 
        v-for="session in sessions" 
        :key="session.id" 
        class="session-item"
        @click="openChat(session)"
      >
        <el-avatar :src="getTargetUser(session).avatar" :size="50" />
        <div class="session-info">
          <div class="session-header">
            <span class="nickname">{{ getTargetUser(session).nickname }}</span>
            <span class="time">{{ formatTime(session.lastMessageTime) }}</span>
          </div>
          <div class="last-message">{{ session.lastMessageContent || '暂无消息' }}</div>
        </div>
        <el-badge :value="session.unreadCount" :hidden="!session.unreadCount" />
      </div>
    </div>
    
    <el-empty v-else description="暂无私信会话" />
    
    <el-button type="primary" class="new-chat-btn" @click="showUserSelect = true">
      <el-icon><Plus /></el-icon>
      新建私信
    </el-button>
    
    <el-dialog v-model="showUserSelect" title="选择用户" width="400px">
      <el-input v-model="searchKeyword" placeholder="搜索用户" @input="searchUsers" />
      <div class="user-list">
        <div 
          v-for="user in searchResults" 
          :key="user.id" 
          class="user-item"
          @click="startChat(user)"
        >
          <el-avatar :src="user.avatar || 'https://picsum.photos/100'" :size="40" />
          <span>{{ user.nickname || user.username }}</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getSessionList, createSession } from '@/api/message'
import { searchUsers as searchUsersApi } from '@/api/search'
import { ElMessage } from 'element-plus'
import { Plus, ArrowLeft } from '@element-plus/icons-vue'

const router = useRouter()
const sessions = ref([])
const showUserSelect = ref(false)
const searchKeyword = ref('')
const searchResults = ref([])
const currentUserId = ref(null)

async function loadSessions() {
  try {
    const res = await getSessionList({ size: 50, offset: 0 })
    sessions.value = res.data || []
  } catch (e) {
    console.error('获取会话列表失败', e)
  }
}

function getTargetUser(session) {
  return {
    id: session.targetUserId,
    nickname: '用户' + session.targetUserId,
    avatar: 'https://picsum.photos/100'
  }
}

function formatTime(time) {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return date.toLocaleDateString()
}

function openChat(session) {
  router.push(`/messages/chat/${session.id}`)
}

function goBack() {
  router.back()
}

async function searchUsers() {
  if (!searchKeyword.value.trim()) {
    searchResults.value = []
    return
  }
  try {
    const res = await searchUsersApi({ keyword: searchKeyword.value })
    searchResults.value = res.data || []
  } catch (e) {
    console.error('搜索用户失败', e)
  }
}

async function startChat(user) {
  try {
    const res = await createSession(user.id)
    showUserSelect.value = false
    router.push(`/messages/chat/${res.data.id}`)
  } catch (e) {
    ElMessage.error('创建会话失败')
  }
}

onMounted(() => {
  loadSessions()
})
</script>

<style scoped>
.messages-page {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.back-btn {
  margin-right: 10px;
  font-size: 20px;
}

.session-list {
  background: #fff;
  border-radius: 8px;
}

.session-item {
  display: flex;
  align-items: center;
  padding: 15px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.2s;
}

.session-item:hover {
  background: #f9f9f9;
}

.session-info {
  flex: 1;
  margin-left: 15px;
}

.session-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.nickname {
  font-weight: 500;
  font-size: 15px;
}

.time {
  color: #999;
  font-size: 12px;
}

.last-message {
  color: #666;
  font-size: 13px;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 300px;
}

.new-chat-btn {
  position: fixed;
  bottom: 30px;
  right: 30px;
}

.user-list {
  max-height: 300px;
  overflow-y: auto;
  margin-top: 10px;
}

.user-item {
  display: flex;
  align-items: center;
  padding: 10px;
  cursor: pointer;
  border-radius: 4px;
}

.user-item:hover {
  background: #f5f5f5;
}

.user-item span {
  margin-left: 10px;
}
</style>