<template>
  <div class="note-card">
    <!-- 头部：头像 + 昵称 + 时间 + 审核状态 -->
    <div class="card-header" @click.stop="goToUser">
      <img
        :src="note.avatar || '/avatar/default.png'"
        class="author-avatar"
        loading="lazy"
        @error="handleAvatarError"
      />
      <div class="author-info">
        <div class="author-name">
          {{ note.nickname || '未知用户' }}
          <!-- 审核状态标签 -->
          <el-tag 
            v-if="note.status === 0" 
            type="warning" 
            size="small" 
            class="status-tag"
          >
            审核中
          </el-tag>
          <el-tag 
            v-else-if="note.status === 2" 
            type="danger" 
            size="small" 
            class="status-tag"
          >
            审核未通过
          </el-tag>
        </div>
        <div class="author-time">{{ formatTime(note.createdAt) }}</div>
      </div>
    </div>

    <!-- 标题 -->
    <div class="card-title" @click.stop="goToNote">
      {{ note.title }}
    </div>

    <!-- 内容摘要 -->
    <div class="card-content" @click.stop="goToNote" v-if="note.content">
      {{ contentPreview }}
    </div>

    <!-- 图片宫格 -->
    <div class="card-images" v-if="note.images && note.images.length" @click.stop="goToNote">
      <div :class="['image-grid', `images-${imageCount}`]">
        <div 
          v-for="(img, index) in displayImages" 
          :key="index"
          class="image-item"
          :class="{ 'image-loaded': loadedImages.has(img) }"
        >
          <img 
            :src="img" 
            class="note-image"
            loading="lazy"
            @load="handleImageLoad(img)"
            @error="handleImageError"
          />
          <div class="image-placeholder" v-if="!loadedImages.has(img)"></div>
        </div>
      </div>
    </div>

    <!-- 视频 -->
    <div class="card-video" v-if="note.video" @click.stop="goToNote">
      <video 
        :src="note.video" 
        class="note-video"
        :poster="note.videoCover"
        preload="metadata"
        playsinline
        webkit-playsinline
        x5-playsinline
      ></video>
      <div class="video-play-icon">
        <svg viewBox="0 0 24 24">
          <path fill="currentColor" d="M8 5v14l11-7z"/>
        </svg>
      </div>
    </div>

    <!-- 底部互动栏 -->
    <div class="card-actions">
      <div
        class="action-btn like-btn"
        :class="{ active: note.liked, 'animating': animatingLike }"
        @click.stop="handleLike"
      >
        <svg class="action-icon" viewBox="0 0 24 24">
          <path v-if="note.liked" fill="currentColor" d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
          <path v-else fill="none" stroke="currentColor" stroke-width="2" d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
        </svg>
        <span class="action-count">{{ formatCount(note.likeCount) }}</span>
      </div>

      <div class="action-btn" @click.stop="goToNote">
        <svg class="action-icon" viewBox="0 0 24 24">
          <path fill="none" stroke="currentColor" stroke-width="2" d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
        </svg>
        <span class="action-count">{{ formatCount(note.commentCount) }}</span>
      </div>

      <div
        class="action-btn favorite-btn"
        :class="{ active: note.favorited, 'animating': animatingFavorite }"
        @click.stop="handleFavorite"
      >
        <svg class="action-icon" viewBox="0 0 24 24">
          <path v-if="note.favorited" fill="currentColor" d="M17 3H7c-1.1 0-2 .9-2 2v16l7-3 7 3V5c0-1.1-.9-2-2-2z"/>
          <path v-else fill="none" stroke="currentColor" stroke-width="2" d="M17 3H7c-1.1 0-2 .9-2 2v16l7-3 7 3V5c0-1.1-.9-2-2-2z"/>
        </svg>
        <span class="action-count">{{ formatCount(note.favoriteCount) }}</span>
      </div>

      <div class="action-btn" @click.stop="handleShare">
        <svg class="action-icon" viewBox="0 0 24 24">
          <path fill="none" stroke="currentColor" stroke-width="2" d="M18 8c1.1 0 2 .9 2 2s-.9 2-2 2-2-.9-2-2 .9-2 2-2zm0-4c1.1 0 2 .9 2 2s-.9 2-2 2-2-.9-2-2 .9-2 2-2zm-6 4c1.1 0 2 .9 2 2s-.9 2-2 2-2-.9-2-2 .9-2 2-2zm0-4c1.1 0 2 .9 2 2s-.9 2-2 2-2-.9-2-2 .9-2 2-2zm-6 4c1.1 0 2 .9 2 2s-.9 2-2 2-2-.9-2-2 .9-2 2-2zm0-4c1.1 0 2 .9 2 2s-.9 2-2 2-2-.9-2-2 .9-2 2-2z"/>
        </svg>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { likeNote, unlikeNote, favoriteNote, unfavoriteNote } from '@/api/note'

const props = defineProps({
  note: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['like', 'favorite', 'update'])

const router = useRouter()
const userStore = useUserStore()
const loadedImages = ref(new Set())
const animatingLike = ref(false)
const animatingFavorite = ref(false)

function handleImageLoad(imgUrl) {
  loadedImages.value.add(imgUrl)
}

const imageCount = computed(() => {
  const len = props.note.images?.length || 0
  return len > 9 ? 9 : len
})

const displayImages = computed(() => {
  return (props.note.images || []).slice(0, 9)
})

const contentPreview = computed(() => {
  const content = props.note.content || ''
  return content.length > 100 ? content.substring(0, 100) + '...' : content
})

function formatTime(time) {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString()
}

function formatCount(count) {
  if (!count || count <= 0) return ''
  if (count >= 10000) {
    return (count / 10000).toFixed(1).replace(/\.0$/, '') + 'w'
  }
  if (count >= 1000) {
    return (count / 1000).toFixed(1).replace(/\.0$/, '') + 'k'
  }
  return count
}

function goToUser() {
  if (props.note.userId) {
    router.push(`/user/${props.note.userId}`)
  }
}

function goToNote() {
  if (props.note.id) {
    router.push(`/note/${props.note.id}`)
  }
}

async function handleLike() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  animatingLike.value = true
  setTimeout(() => { animatingLike.value = false }, 300)

  try {
    if (props.note.liked) {
      await unlikeNote(props.note.id)
      props.note.liked = false
      props.note.likeCount = Math.max(0, (props.note.likeCount || 1) - 1)
    } else {
      await likeNote(props.note.id)
      props.note.liked = true
      props.note.likeCount = (props.note.likeCount || 0) + 1
    }
    emit('like', props.note)
    emit('update')
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

async function handleFavorite() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  animatingFavorite.value = true
  setTimeout(() => { animatingFavorite.value = false }, 300)

  try {
    if (props.note.favorited) {
      await unfavoriteNote(props.note.id)
      props.note.favorited = false
      props.note.favoriteCount = Math.max(0, (props.note.favoriteCount || 1) - 1)
      ElMessage.success('已取消收藏')
    } else {
      await favoriteNote(props.note.id)
      props.note.favorited = true
      props.note.favoriteCount = (props.note.favoriteCount || 0) + 1
      ElMessage.success('收藏成功')
    }
    emit('favorite', props.note)
    emit('update')
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

function handleShare() {
  const url = `${window.location.origin}/note/${props.note.id}`
  if (navigator.clipboard) {
    navigator.clipboard.writeText(url).then(() => {
      ElMessage.success('链接已复制')
    }).catch(() => {
      ElMessage.error('复制失败')
    })
  } else {
    const textarea = document.createElement('textarea')
    textarea.value = url
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    ElMessage.success('链接已复制')
  }
}

function handleAvatarError(e) {
  e.target.src = '/avatar/default.png'
}

function handleImageError(e) {
  e.target.style.display = 'none'
  const img = e.target
  const parent = img.parentElement
  if (parent) {
    parent.classList.add('image-error')
  }
}
</script>

<style scoped>
@import '@/styles/design-tokens.css';

.note-card {
  background: var(--color-bg);
  border-radius: var(--radius-md);
  padding: var(--spacing-lg);
  margin-bottom: var(--spacing-md);
  transition: all var(--transition-normal);
  cursor: pointer;
}

.note-card:hover {
  box-shadow: var(--shadow-md);
}

.card-header {
  display: flex;
  align-items: center;
  margin-bottom: var(--spacing-md);
}

.author-avatar {
  width: var(--avatar-md);
  height: var(--avatar-md);
  border-radius: 50%;
  object-fit: cover;
  margin-right: var(--spacing-md);
  transition: transform var(--transition-fast);
}

.card-header:hover .author-avatar {
  transform: scale(1.05);
}

.author-info {
  flex: 1;
}

.author-name {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  color: var(--color-text);
}

.card-header:hover .author-name {
  color: var(--color-primary);
}

.author-time {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
  margin-top: 2px;
}

.card-title {
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text);
  line-height: var(--line-height-normal);
  margin-bottom: var(--spacing-sm);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-title:hover {
  color: var(--color-primary);
}

.card-content {
  font-size: var(--font-size-base);
  color: var(--color-text-secondary);
  line-height: var(--line-height-relaxed);
  margin-bottom: var(--spacing-md);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-images {
  margin-bottom: var(--spacing-md);
}

.image-grid {
  display: grid;
  gap: var(--spacing-xs);
}

.images-1 .image-item {
  grid-column: span 1;
}

.images-1 .note-image {
  width: 100%;
  max-height: 300px;
  object-fit: cover;
  border-radius: var(--radius-sm);
}

.images-2 .image-item,
.images-3 .image-item {
  grid-template-columns: 1fr 1fr;
}

.images-2 .note-image,
.images-3 .note-image {
  width: 100%;
  height: 150px;
  object-fit: cover;
  border-radius: var(--radius-sm);
}

.images-4 .image-item {
  grid-template-columns: 1fr 1fr;
}

.images-4 .note-image,
.images-5 .note-image,
.images-6 .note-image {
  width: 100%;
  height: 120px;
  object-fit: cover;
  border-radius: var(--radius-sm);
}

.images-5 .image-grid,
.images-6 .image-grid {
  grid-template-columns: repeat(3, 1fr);
}

.images-7 .image-grid,
.images-8 .image-grid,
.images-9 .image-grid {
  grid-template-columns: repeat(3, 1fr);
}

.images-7 .note-image,
.images-8 .note-image,
.images-9 .note-image {
  width: 100%;
  height: 100px;
  object-fit: cover;
  border-radius: var(--radius-sm);
}

.image-item {
  position: relative;
  overflow: hidden;
  background: var(--color-bg-secondary);
}

.image-item.image-loaded .image-placeholder {
  opacity: 0;
}

.image-placeholder {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(90deg, 
    var(--color-bg-secondary) 0%, 
    var(--color-bg-tertiary) 50%, 
    var(--color-bg-secondary) 100%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  transition: opacity 0.3s ease;
}

@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

.image-item.image-error .image-placeholder {
  display: none;
}

.note-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform var(--transition-slow), opacity 0.3s ease;
  opacity: 0;
  position: relative;
  z-index: 1;
}

.image-loaded .note-image {
  opacity: 1;
}

.note-card:hover .note-image {
  transform: scale(1.02);
}

/* 视频样式 */
.card-video {
  position: relative;
  margin-bottom: var(--spacing-md);
  border-radius: var(--radius-sm);
  overflow: hidden;
  background: var(--color-bg-secondary);
}

.note-video {
  width: 100%;
  display: block;
  max-height: 400px;
  object-fit: contain;
  background: #000;
}

.video-play-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 60px;
  height: 60px;
  background: var(--color-bg-overlay);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-reverse);
  opacity: 0.9;
  transition: all var(--transition-fast);
}

.video-play-icon svg {
  width: 30px;
  height: 30px;
  margin-left: 4px;
}

.card-video:hover .video-play-icon {
  opacity: 1;
  transform: translate(-50%, -50%) scale(1.1);
}

.card-actions {
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding-top: var(--spacing-md);
  border-top: 1px solid var(--color-border);
}

.action-btn {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  color: var(--color-text-tertiary);
  font-size: var(--font-size-sm);
  transition: all var(--transition-fast);
  cursor: pointer;
  padding: var(--spacing-xs) var(--spacing-sm);
  border-radius: var(--radius-full);
}

.action-btn:hover {
  background: var(--color-bg-secondary);
  color: var(--color-primary);
}

.action-btn.active {
  color: var(--color-primary);
}

.action-btn.active .action-icon,
.action-btn.animating .action-icon {
  animation: heartBeat 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

/* 点赞按钮动画增强 */
.action-btn.like-btn.animating .action-icon {
  filter: drop-shadow(0 0 4px rgba(255, 107, 107, 0.6));
  animation: likeBounce 0.6s ease;
}

/* 收藏按钮动画增强 */
.action-btn.favorite-btn.animating .action-icon {
  filter: drop-shadow(0 0 6px rgba(255, 215, 0, 0.8));
  animation: favoriteShine 0.5s ease;
}

.action-btn.animating {
  transform: scale(1.15);
  transition: transform 0.15s ease;
}

@keyframes heartBeat {
  0% { transform: scale(1); }
  25% { transform: scale(1.3); }
  50% { transform: scale(0.9); }
  75% { transform: scale(1.15); }
  100% { transform: scale(1); }
}

@keyframes likeBounce {
  0% { transform: scale(1); }
  20% { transform: scale(1.4); }
  40% { transform: scale(0.8); }
  60% { transform: scale(1.2); }
  80% { transform: scale(0.95); }
  100% { transform: scale(1); }
}

@keyframes favoriteShine {
  0% { transform: scale(1) rotate(0deg); }
  25% { transform: scale(1.3) rotate(-10deg); }
  50% { transform: scale(0.9) rotate(5deg); }
  75% { transform: scale(1.1) rotate(-5deg); }
  100% { transform: scale(1) rotate(0deg); }
}

.action-icon {
  width: 18px;
  height: 18px;
}

.action-count {
  min-width: 20px;
}

@media (max-width: 600px) {
  .note-card {
    padding: var(--spacing-md);
    margin-bottom: var(--spacing-sm);
  }

  .author-avatar {
    width: var(--avatar-sm);
    height: var(--avatar-sm);
  }

  .card-title {
    font-size: var(--font-size-base);
  }

  .card-content {
    font-size: var(--font-size-sm);
  }

  .action-icon {
    width: 16px;
    height: 16px;
  }
}
</style>