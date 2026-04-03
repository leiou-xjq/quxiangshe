<template>
  <div class="note-card" @click="goToDetail">
    <!-- 用户信息 -->
    <div class="note-header">
      <div class="user-info">
        <el-avatar :size="40" :src="note.avatarUrl" @click.stop="goToUser">
          {{ note.nickname?.charAt(0) || note.username?.charAt(0) || 'U' }}
        </el-avatar>
        <div class="user-meta">
          <span class="username" @click.stop="goToUser">{{ note.nickname || note.username }}</span>
          <span class="time">{{ formatTime(note.createTime) }}</span>
        </div>
      </div>
      <el-tag v-if="note.category" size="small" type="info">{{ note.category }}</el-tag>
    </div>

    <!-- 内容 -->
    <div class="note-content">
      <h3 class="note-title">{{ note.title }}</h3>
      <p class="note-text" v-html="highlightContent"></p>
    </div>

    <!-- 图片 -->
    <div class="note-images" v-if="note.images?.length">
      <el-image
        v-for="(img, idx) in note.images.slice(0, 4)"
        :key="idx"
        :src="img"
        :preview-src-list="note.images"
        :initial-index="idx"
        fit="cover"
        class="note-image"
        :class="{ 'single': note.images.length === 1, 'multiple': note.images.length > 1 }"
      />
    </div>

    <!-- 底部操作 -->
    <div class="note-footer">
      <div class="note-stats">
        <span class="stat-item">
          <el-icon><View /></el-icon>
          {{ note.viewCount || 0 }}
        </span>
        <span class="stat-item">
          <el-icon><ChatLineRound /></el-icon>
          {{ note.commentCount || 0 }}
        </span>
        <span class="stat-item">
          <el-icon><Star /></el-icon>
          {{ note.collectCount || 0 }}
        </span>
      </div>
      
      <div class="note-actions" @click.stop>
        <el-button
          :type="note.isLiked ? 'primary' : 'default'"
          :icon="note.isLiked ? StarFilled : Star"
          circle
          size="small"
          @click="handleLike"
        />
        <el-button
          :type="note.isCollected ? 'warning' : 'default'"
          :icon="note.isCollected ? StarFilled : Star"
          circle
          size="small"
          @click="handleCollect"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useNoteStore } from '@/store/note'
import { ElMessage } from 'element-plus'
import { View, ChatLineRound, Star, StarFilled, Pointer } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const props = defineProps({
  note: {
    type: Object,
    required: true
  },
  keyword: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['like', 'collect'])

const router = useRouter()
const noteStore = useNoteStore()
const userStore = useUserStore()

// 高亮内容
const highlightContent = computed(() => {
  if (!props.keyword) return props.note.content?.slice(0, 150) + '...'
  const regex = new RegExp(`(${props.keyword})`, 'gi')
  return (props.note.content || '').slice(0, 150).replace(regex, '<em style="color: #FF7D00; font-weight: bold;">$1</em>') + '...'
})

// 格式化时间
const formatTime = (time) => {
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

// 跳转详情
const goToDetail = () => {
  router.push(`/note/${props.note.noteId}`)
}

// 跳转用户主页
const goToUser = () => {
  router.push(`/user/${props.note.userId}`)
}

// 点赞
const handleLike = async () => {
  if (!userStore.isLoggedIn()) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  emit('like', props.note.noteId)
}

// 收藏
const handleCollect = async () => {
  if (!userStore.isLoggedIn()) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  emit('collect', props.note.noteId)
}
</script>

<style scoped>
.note-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.note-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.note-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.note-header .user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-meta {
  display: flex;
  flex-direction: column;
}

.username {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.username:hover {
  color: #FF7D00;
}

.time {
  font-size: 12px;
  color: #999;
}

.note-content {
  margin-bottom: 12px;
}

.note-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px 0;
  line-height: 1.4;
}

.note-text {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.note-text :deep(em) {
  color: #409EFF;
  font-weight: bold;
  background-color: rgba(64, 158, 255, 0.1);
  padding: 0 2px;
  border-radius: 2px;
}

.note-images {
  display: grid;
  gap: 8px;
  margin-bottom: 12px;
  border-radius: 8px;
  overflow: hidden;
}

.note-images .single {
  width: 100%;
  max-height: 300px;
}

.note-images .multiple {
  width: 100%;
  height: 180px;
}

.note-images .note-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.note-images[style*="grid-template-columns: 1fr 1fr"] {
  grid-template-columns: 1fr 1fr;
}

.note-images[style*="grid-template-columns: 1fr 1fr 1fr"] {
  grid-template-columns: 1fr 1fr 1fr;
}

.note-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f5f7fa;
}

.note-stats {
  display: flex;
  gap: 16px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #999;
}

.note-actions {
  display: flex;
  gap: 8px;
}

.note-actions .el-button {
  border: none;
  background: #f5f7fa;
}

.note-actions .el-button:hover {
  background: #ecf5ff;
  color: #409EFF;
}
</style>
