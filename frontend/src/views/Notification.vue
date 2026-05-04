<template>
  <div class="notification-page">
    <div class="notification-header">
      <el-button circle @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
      </el-button>
      <h2>通知</h2>
      <el-button text type="primary" @click="handleMarkAllRead" :disabled="unreadCount === 0">
        全部已读
      </el-button>
    </div>
    
    <div class="notification-list" v-loading="loading">
      <div v-if="notifications.length === 0 && !loading" class="empty">
        <el-empty description="暂无通知" />
      </div>
      
      <div 
        v-for="item in notifications" 
        :key="item.id" 
        class="notification-item"
        :class="{ unread: !item.isRead }"
        @click="handleClick(item)"
      >
        <div class="notification-avatar">
          <el-avatar :src="item.fromUser?.avatar || '/avatar/default.png'" />
        </div>
        <div class="notification-content">
          <div class="notification-text">{{ item.content }}</div>
          <div class="notification-time">{{ formatTime(item.createdAt) }}</div>
        </div>
        <div class="notification-actions">
          <el-button text type="danger" size="small" @click.stop="handleDelete(item.id)">
            删除
          </el-button>
        </div>
      </div>
      
      <div class="load-more" v-if="hasMore">
        <el-button text @click="loadMore">加载更多</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getNotifications, markAsRead, markAllAsRead, deleteNotification, getUnreadCount } from '@/api/notification'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const notifications = ref([])
const unreadCount = ref(0)
const hasMore = ref(true)
const offset = ref(0)
const size = 20

function formatTime(time) {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 604800000) return Math.floor(diff / 86400000) + '天前'
  return time.split('T')[0]
}

async function fetchNotifications() {
  loading.value = true
  try {
    const res = await getNotifications(size, offset.value)
    notifications.value = [...notifications.value, ...(res.data || [])]
    hasMore.value = res.data?.length === size
    offset.value += size
  } catch (error) {
    console.error('获取通知失败:', error)
  } finally {
    loading.value = false
  }
}

async function fetchUnreadCount() {
  try {
    const res = await getUnreadCount()
    unreadCount.value = res.data || 0
    userStore.updateUnreadCount(unreadCount.value)
  } catch (error) {
    console.error('获取未读数失败:', error)
  }
}

const TYPE_REVIEW_REJECTED = 6

async function handleClick(item) {
  if (!item.isRead) {
    try {
      await markAsRead(item.id)
      item.isRead = 1
      unreadCount.value = Math.max(0, unreadCount.value - 1)
      userStore.updateUnreadCount(unreadCount.value)
    } catch (error) {
      console.error('标记已读失败:', error)
    }
  }
  
  // 审核未通过的通知只标记已读，不跳转
  if (item.type === TYPE_REVIEW_REJECTED) {
    return
  }
  
  if (item.noteId) {
    router.push(`/note/${item.noteId}`)
  } else if (item.fromUserId) {
    router.push(`/user/${item.fromUserId}`)
  }
}

async function handleMarkAllRead() {
  try {
    await markAllAsRead()
    notifications.value.forEach(item => item.isRead = 1)
    unreadCount.value = 0
    userStore.updateUnreadCount(0)
    ElMessage.success('已全部标记为已读')
  } catch (error) {
    console.error('标记失败:', error)
  }
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定删除这条通知吗？', '提示')
    await deleteNotification(id)
    notifications.value = notifications.value.filter(item => item.id !== id)
    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

function loadMore() {
  fetchNotifications()
}

function goBack() {
  router.back()
}

onMounted(() => {
  fetchNotifications()
  fetchUnreadCount()
})
</script>

<style scoped>
.notification-page {
  padding: 20px;
  max-width: 600px;
  margin: 0 auto;
}

.notification-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
}

.notification-header h2 {
  flex: 1;
  margin: 0;
}

.notification-list {
  min-height: 400px;
}

.notification-item {
  display: flex;
  align-items: center;
  padding: 15px;
  border-bottom: 1px solid #eee;
  cursor: pointer;
  transition: background 0.2s;
}

.notification-item:hover {
  background: #f5f5f5;
}

.notification-item.unread {
  background: #f0f9ff;
}

.notification-item.unread::before {
  content: '';
  width: 8px;
  height: 8px;
  background: #409eff;
  border-radius: 50%;
  margin-right: 10px;
}

.notification-avatar {
  margin-right: 15px;
}

.notification-content {
  flex: 1;
}

.notification-text {
  margin-bottom: 5px;
}

.notification-time {
  font-size: 12px;
  color: #999;
}

.notification-actions {
  margin-left: 10px;
}

.empty {
  padding: 50px 0;
}

.load-more {
  text-align: center;
  padding: 20px;
}
</style>
