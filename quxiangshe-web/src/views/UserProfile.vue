<template>
  <div class="user-profile-page">
    <!-- 返回首页 -->
    <div class="back-home" @click="$router.push('/')">
      <el-icon><ArrowLeft /></el-icon>
      <span>返回首页</span>
    </div>

    <!-- 用户信息卡片 -->
    <div class="profile-card">
      <div class="profile-header">
        <div class="avatar-section">
          <el-avatar :size="100" :src="userInfo.avatarUrl">
            {{ userInfo.nickname?.charAt(0) || userInfo.username?.charAt(0) || 'U' }}
          </el-avatar>
        </div>
        
        <div class="info-section">
          <h1 class="nickname">{{ userInfo.nickname || userInfo.username }}</h1>
          <p class="username">@{{ userInfo.username }}</p>
          <p class="bio" v-if="userInfo.bio">{{ userInfo.bio }}</p>
          <p class="create-time" v-if="userInfo.createTime">
            <el-icon><Calendar /></el-icon>
            加入于 {{ formatDate(userInfo.createTime) }}
          </p>
        </div>
        
        <div class="action-section">
          <template v-if="isOwnProfile">
            <el-button @click="handleEditProfile" class="edit-btn">
              <el-icon><Edit /></el-icon>
              编辑资料
            </el-button>
            <el-button type="danger" @click="handleLogout" class="logout-btn">
              <el-icon><SwitchButton /></el-icon>
              退出登录
            </el-button>
          </template>
          <template v-else>
            <el-button :type="userInfo.isFollowing ? 'default' : 'primary'" @click="handleFollow" class="follow-btn">
              {{ userInfo.isFollowing ? '已关注' : '关注' }}
            </el-button>
          </template>
        </div>
      </div>
      
      <!-- 统计数据 -->
      <div class="stats-section">
        <div class="stat-item">
          <span class="stat-value">{{ stats.noteCount }}</span>
          <span class="stat-label">笔记</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ stats.likeCount }}</span>
          <span class="stat-label">获赞</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ stats.collectCount }}</span>
          <span class="stat-label">收藏</span>
        </div>
      </div>
    </div>

    <!-- 笔记列表 -->
    <div class="notes-section">
      <div class="section-header">
        <h2 class="section-title">我的笔记</h2>
        <el-radio-group v-model="notesType" @change="handleNotesTypeChange">
          <el-radio-button label="published">发布的</el-radio-button>
          <el-radio-button label="collected">收藏的</el-radio-button>
        </el-radio-group>
      </div>
      
      <div class="notes-container" v-loading="loading" element-loading-text="加载中...">
        <template v-if="notes.length > 0">
          <div class="notes-grid">
            <NoteCard
              v-for="(note, index) in notes"
              :key="note.noteId"
              :note="note"
              :style="{ animationDelay: `${index * 0.05}s` }"
              class="note-card-animate"
            />
          </div>
          
          <!-- 加载更多 -->
          <div class="load-more" v-if="hasMore">
            <el-button 
              :loading="loading" 
              @click="loadMore"
              type="primary" 
              link
            >
              加载更多
            </el-button>
          </div>
          
          <el-empty 
            v-if="!hasMore && notes.length > 0" 
            description="没有更多了"
          />
        </template>
        
        <!-- 空状态 -->
        <el-empty 
          v-else-if="!loading" 
          :description="notesType === 'published' ? '还没有发布任何笔记' : '还没有收藏任何笔记'"
        >
          <el-button type="primary" @click="$router.push('/publish')">
            发布笔记
          </el-button>
        </el-empty>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { userApi } from '@/api/user'
import { noteApi } from '@/api/note'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Calendar, Edit, SwitchButton } from '@element-plus/icons-vue'
import NoteCard from '@/components/NoteCard.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const notesType = ref('published')
const notes = ref([])
const lastNoteId = ref(null)
const hasMore = ref(false)
const userInfo = ref({})
const stats = reactive({
  noteCount: 0,
  likeCount: 0,
  collectCount: 0
})

const isOwnProfile = computed(() => {
  return !route.params.id || route.params.id == userStore.userId
})

onMounted(() => {
  loadUserInfo()
  loadUserNotes()
  window.addEventListener('visibilitychange', handleVisibilityChange)
})

const handleVisibilityChange = () => {
  if (document.visibilityState === 'visible') {
    loadUserInfo()
    loadUserNotes(true)
  }
}

onUnmounted(() => {
  window.removeEventListener('visibilitychange', handleVisibilityChange)
})

const loadUserInfo = async () => {
  try {
    let userId = route.params.id || userStore.userId
    let res
    if (isOwnProfile.value) {
      res = await userApi.getCurrentUser()
    } else {
      res = await userApi.getUserProfile(userId)
    }
    
    if (res.code === 0 || res.code === 200) {
      userInfo.value = res.data
    }
  } catch (e) {
    userInfo.value = {
      userId: userStore.userId,
      username: userStore.username,
      nickname: userStore.nickname,
      avatarUrl: userStore.avatarUrl,
      bio: ''
    }
  }
}

const loadUserNotes = async (refresh = false) => {
  if (loading.value || (!hasMore.value && !refresh)) return
  
  loading.value = true
  try {
    let targetUserId = route.params.id || userStore.userId
    let data
    if (notesType.value === 'published') {
      data = await noteApi.getUserNotes(targetUserId, lastNoteId.value, 20)
    } else {
      data = { items: [], hasMore: false }
    }
    
    if (refresh) {
      notes.value = data.items || []
    } else {
      notes.value = [...notes.value, ...(data.items || [])]
    }
    lastNoteId.value = data.lastNoteId
    hasMore.value = data.hasMore
    
    stats.noteCount = userInfo.value.postCount || 0
    stats.likeCount = 0
    stats.collectCount = 0
  } catch (e) {
  } finally {
    loading.value = false
  }
}

const handleNotesTypeChange = () => {
  notes.value = []
  lastNoteId.value = null
  hasMore.value = false
  loadUserNotes(true)
}

const loadMore = () => {
  loadUserNotes(false)
}

const formatDate = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const handleEditProfile = () => {
  ElMessage.info('个人资料编辑功能开发中...')
}

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    userStore.logout()
    router.push('/login')
  })
}

const handleFollow = () => {
  ElMessage.info('关注功能开发中...')
}
</script>

<style scoped>
.user-profile-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 24px 20px;
}

/* 返回首页 */
.back-home {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #666;
  font-size: 14px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 8px;
  transition: all 0.2s;
  margin-bottom: 20px;
}

.back-home:hover {
  background: #f5f7fa;
  color: #FF7D00;
}

/* 用户信息卡片 */
.profile-card {
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  margin-bottom: 24px;
}

.profile-header {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.avatar-section {
  flex-shrink: 0;
}

.avatar-section :deep(.el-avatar) {
  border: 4px solid #fff;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.info-section {
  flex: 1;
}

.nickname {
  font-size: 28px;
  font-weight: 700;
  color: #333;
  margin: 0 0 4px;
}

.username {
  font-size: 14px;
  color: #999;
  margin: 0 0 12px;
}

.bio {
  font-size: 15px;
  color: #666;
  margin: 0 0 12px;
  line-height: 1.6;
}

.create-time {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #999;
  margin: 0;
}

.action-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.edit-btn {
  background: linear-gradient(135deg, #FF7D00 0%, #FF9500 100%);
  border: none;
  color: #fff;
}

.follow-btn {
  background: linear-gradient(135deg, #FF7D00 0%, #FF9500 100%);
  border: none;
  color: #fff;
}

.logout-btn {
  background: #fff;
  border: 1px solid #ff4d4f;
  color: #ff4d4f;
}

.logout-btn:hover {
  background: #fff1f0;
  border-color: #ff4d4f;
}

/* 统计数据 */
.stats-section {
  display: flex;
  gap: 48px;
  padding-top: 24px;
  margin-top: 24px;
  border-top: 1px solid #f0f0f0;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #333;
}

.stat-label {
  font-size: 13px;
  color: #999;
  margin-top: 4px;
}

/* 笔记列表 */
.notes-section {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.section-header :deep(.el-radio-button__inner) {
  padding: 8px 16px;
}

.section-header :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: linear-gradient(135deg, #FF7D00 0%, #FF9500 100%);
  border-color: #FF7D00;
  box-shadow: none;
}

.notes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.note-card-animate {
  animation: fadeInUp 0.5s ease forwards;
  opacity: 0;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.load-more {
  text-align: center;
  padding: 30px 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .profile-header {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }
  
  .action-section {
    flex-direction: row;
    width: 100%;
    justify-content: center;
  }
  
  .stats-section {
    justify-content: center;
  }
  
  .notes-grid {
    grid-template-columns: 1fr;
  }
}
</style>
