<template>
  <div class="user-profile-page" v-loading="loading">
    <!-- 返回按钮 -->
    <div class="page-header">
      <el-button text @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
      <span class="page-title">{{ userInfo?.nickname || '用户主页' }}</span>
      <div style="width: 40px;"></div>
    </div>
    
    <!-- 用户信息 -->
    <div class="user-card">
      <div class="user-header-bg"></div>
      <div class="user-info">
        <el-avatar :size="80" :src="userInfo?.avatar || '/avatar/default.png'" />
        <div class="user-name">{{ userInfo?.nickname || userInfo?.username }}</div>
        <div class="user-username">@{{ userInfo?.username }}</div>
        <div class="user-bio" v-if="userInfo?.bio">{{ userInfo.bio }}</div>
        
        <div class="user-actions">
          <el-button :type="isFollowing ? 'default' : 'primary'" @click="handleFollow">
            {{ isFollowing ? '已关注' : '关注' }}
          </el-button>
          <el-button type="primary" @click="handleMessage">
            <el-icon><Message /></el-icon>
            私信
          </el-button>
        </div>
        
        <div class="user-stats">
          <div class="stat-item">
            <div class="stat-value">{{ followingCount }}</div>
            <div class="stat-label">关注</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ fansCount }}</div>
            <div class="stat-label">粉丝</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ likesCount }}</div>
            <div class="stat-label">获赞</div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 笔记列表 -->
    <div class="notes-section">
      <div class="section-title">笔记</div>
      <div class="notes-list" v-if="notes.length > 0">
        <div 
          v-for="note in notes" 
          :key="note.id" 
          class="note-item"
          @click="goToNote(note.id)"
        >
          <div class="note-title">{{ note.title }}</div>
          <div class="note-meta">
            <el-tag v-if="note.status === 0" type="warning" size="small">审核中</el-tag>
            <span>{{ note.likeCount || 0 }} 赞</span>
            <span>{{ note.commentCount || 0 }} 评论</span>
            <span>{{ formatTime(note.createdAt) }}</span>
          </div>
        </div>
        <div class="load-more" v-if="hasMore" @click="loadMore">
          <el-button link>加载更多</el-button>
        </div>
      </div>
      <div class="empty-tip" v-else>还没有发布过笔记</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Message } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getUserInfo } from '@/api/auth'
import { createSession } from '@/api/message'
import { getFollowingCount, getFollowersCount, followUser, unfollowUser, getFollowStatus } from '@/api/follow'
import { getUserLikesCount, getMyNotes, getUserNotes } from '@/api/note'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const userInfo = ref(null)
const notes = ref([])
const notesPage = ref(1)
const hasMore = ref(true)
const isFollowing = ref(false)
const followingCount = ref(0)
const fansCount = ref(0)
const likesCount = ref(0)

const userId = computed(() => route.params.id)

onMounted(async () => {
  console.log('UserProfile mounted, route.params:', route.params)
  await loadData()
})

async function loadData() {
  const id = route.params.id
  console.log('loadData - id:', id, 'type:', typeof id)
  
  if (!id) {
    ElMessage.error('用户不存在')
    router.back()
    return
  }
  
  loading.value = true
  try {
    await userStore.fetchUserInfo()
    console.log('loadData - userInfo:', userStore.userInfo)
    
    const [userRes, followingRes, fansRes, likesRes, statusRes] = await Promise.all([
      getUserInfo(id),
      getFollowingCount(id),
      getFollowersCount(id),
      getUserLikesCount(id),
      getFollowStatus(id)
    ])
    
    console.log('getFollowStatus response:', statusRes)
    userInfo.value = userRes.data
    followingCount.value = userRes.data?.followingCount || followingRes.data || 0
    fansCount.value = userRes.data?.followerCount || fansRes.data || 0
    likesCount.value = likesRes.data || 0
    isFollowing.value = statusRes?.data || false
    console.log('UserProfile - isFollowing:', isFollowing.value)
    
    await loadNotes()
  } catch (e) {
    console.error('加载失败:', e)
    ElMessage.error('加载用户信息失败')
    router.back()
  } finally {
    loading.value = false
  }
}

async function loadNotes() {
  if (!hasMore.value) return
  const id = Number(route.params.id)
  const currentUserId = userStore.userInfo?.id ? Number(userStore.userInfo.id) : null
  const isSelf = currentUserId !== null && currentUserId === id
  
  console.log('loadNotes - userStore.userInfo:', userStore.userInfo, 'currentUserId:', currentUserId, 'id:', id, 'isSelf:', isSelf)
  
  // 如果当前用户ID不存在（未登录），显示提示
  if (!currentUserId) {
    console.warn('用户未登录，无法判断是否为自己的主页')
    hasMore.value = false
    return
  }
  
  try {
    console.log('loadNotes - API call, isSelf:', isSelf)
    const res = isSelf 
      ? await getMyNotes(notesPage.value, 10) 
      : await getUserNotes(id, notesPage.value, 10)
    console.log('loadNotes response:', res)
    const newNotes = res.data || []
    console.log('loadNotes - newNotes:', newNotes)
    newNotes.forEach((note, idx) => {
      console.log(`note[${idx}] - id: ${note.id}, status: ${note.status}, title: ${note.title}`)
    })
    if (notesPage.value === 1) {
      notes.value = newNotes
    } else {
      notes.value.push(...newNotes)
    }
    hasMore.value = newNotes.length === 10
    if (hasMore.value) {
      notesPage.value++
    }
  } catch (e) {
    console.error('加载笔记失败:', e)
  }
}

function loadMore() {
  loadNotes()
}

async function handleFollow() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  try {
    if (isFollowing.value) {
      await unfollowUser(userId.value)
      isFollowing.value = false
      ElMessage.success('已取消关注')
    } else {
      await followUser(userId.value)
      isFollowing.value = true
      ElMessage.success('关注成功')
    }
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function handleMessage() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  const targetUserId = Number(userId.value)
  if (!targetUserId || targetUserId === userStore.userInfo?.id) {
    ElMessage.warning('不能给自己发私信')
    return
  }
  
  try {
    console.log('createSession param:', targetUserId)
    const res = await createSession(targetUserId)
    console.log('createSession response:', res)
    const sessionId = res.data?.id
    console.log('sessionId:', sessionId)
    if (sessionId) {
      router.push(`/messages/chat/${sessionId}`)
    } else {
      ElMessage.error('会话创建失败')
    }
  } catch (e) {
    console.error('创建会话失败', e)
    ElMessage.error('创建会话失败')
  }
}

function formatTime(time) {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  const oneDay = 24 * 60 * 60 * 1000
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < oneDay * 7) return Math.floor(diff / oneDay) + '天前'
  
  return date.toLocaleDateString('zh-CN')
}

function goBack() {
  router.back()
}

function goToNote(noteId) {
  router.push(`/note/${noteId}`)
}
</script>

<style scoped>
.user-profile-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #fff;
  position: sticky;
  top: 0;
  z-index: 10;
}

.page-title {
  font-size: 16px;
  font-weight: 500;
}

.user-card {
  background: #fff;
  margin-bottom: 10px;
}

.user-header-bg {
  height: 60px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.user-info {
  padding: 0 20px 20px;
  text-align: center;
}

.user-info .el-avatar {
  margin-top: -40px;
  border: 3px solid #fff;
}

.user-name {
  margin-top: 12px;
  font-size: 18px;
  font-weight: 600;
}

.user-username {
  color: #999;
  font-size: 14px;
  margin-top: 4px;
}

.user-bio {
  margin-top: 12px;
  color: #333;
  font-size: 14px;
  line-height: 1.5;
}

.user-actions {
  margin-top: 16px;
}

.user-stats {
  display: flex;
  justify-content: center;
  gap: 40px;
  margin-top: 20px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 18px;
  font-weight: 600;
}

.stat-label {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.notes-section {
  background: #fff;
  padding: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
}

.notes-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.note-item {
  padding: 12px;
  background: #f9f9f9;
  border-radius: 8px;
  cursor: pointer;
}

.note-item:hover {
  background: #f0f0f0;
}

.note-title {
  font-size: 15px;
  font-weight: 500;
  margin-bottom: 8px;
}

.note-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #999;
}

.load-more {
  text-align: center;
  padding: 12px;
}

.empty-tip {
  text-align: center;
  color: #999;
  padding: 40px 0;
}
</style>