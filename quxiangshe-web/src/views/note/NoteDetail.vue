<template>
  <div class="note-detail-page">
    <div class="back-section">
      <el-button class="back-btn" @click="goBack" text>
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
    </div>

    <div class="note-detail" v-loading="loading" element-loading-text="加载中...">
      <template v-if="note">
        <!-- 用户信息 -->
        <div class="note-author">
          <el-avatar 
            :size="48" 
            :src="note.avatarUrl"
            class="author-avatar"
            @click="goToUserProfile"
          >
            {{ note.nickname?.charAt(0) || note.username?.charAt(0) || 'U' }}
          </el-avatar>
          <div class="author-info">
            <div class="author-name" @click="goToUserProfile">
              {{ note.nickname || note.username }}
            </div>
            <span class="author-time">{{ formatTime(note.createTime) }}</span>
          </div>
        </div>

        <!-- 标题 -->
        <h1 class="note-title">{{ note.title }}</h1>

        <!-- 内容 -->
        <div class="note-content">{{ note.content }}</div>

        <!-- 封面图 -->
        <div class="note-cover" v-if="note.coverImage">
          <el-image
            :src="note.coverImage"
            :preview-src-list="[note.coverImage]"
            fit="cover"
            class="cover-image"
          />
        </div>

        <!-- 图片集 -->
        <div class="note-images" v-if="note.images?.length">
          <el-image
            v-for="(img, idx) in note.images"
            :key="idx"
            :src="img"
            :preview-src-list="note.images"
            :initial-index="idx"
            fit="cover"
            class="note-image"
          />
        </div>

        <!-- 标签 -->
        <div class="note-tags" v-if="note.tags?.length">
          <el-tag v-for="tag in note.tags" :key="tag" size="small" type="info">
            {{ tag }}
          </el-tag>
        </div>

        <!-- 分类 -->
        <div class="note-category" v-if="note.category">
          <el-tag size="small" type="success">{{ note.category }}</el-tag>
        </div>

        <!-- 统计与操作 -->
        <div class="note-interaction">
          <div class="note-stats">
            <span class="stat-item">
              <el-icon><View /></el-icon>
              {{ note.viewCount || 0 }}
            </span>
            <span class="stat-item">
              <el-icon><ChatLineRound /></el-icon>
              {{ note.commentCount || 0 }}
            </span>
          </div>

          <div class="note-actions">
            <el-button
              :type="note.isLiked ? 'primary' : 'default'"
              :icon="note.isLiked ? StarFilled : Star"
              circle
              @click="handleLike"
              :disabled="!userStore.isLoggedIn()"
              :title="!userStore.isLoggedIn() ? '登录后即可点赞' : ''"
            />
            <span class="action-count">{{ note.likeCount || 0 }}</span>
            <el-button
              :type="note.isCollected ? 'warning' : 'default'"
              :icon="note.isCollected ? StarFilled : Star"
              circle
              @click="handleCollect"
              :disabled="!userStore.isLoggedIn()"
              :title="!userStore.isLoggedIn() ? '登录后即可收藏' : ''"
            />
            <span class="action-count">{{ note.collectCount || 0 }}</span>
          </div>
        </div>

        <!-- 评论区域 -->
        <div class="comment-section">
          <h3 class="section-title">
            <el-icon><ChatLineRound /></el-icon>
            评论 ({{ comments.length || note.commentCount || 0 }})
          </h3>

          <!-- 评论输入框 -->
          <div class="comment-input-wrapper" v-if="userStore.isLoggedIn()">
            <el-avatar :size="36" :src="userStore.avatarUrl">
              {{ userStore.nickname?.charAt(0) || userStore.username?.charAt(0) }}
            </el-avatar>
            <div class="comment-input-inner">
              <el-input
                v-model="commentContent"
                type="textarea"
                :rows="3"
                placeholder="写下你的评论..."
                maxlength="500"
                show-word-limit
                resize="none"
              />
              <el-button 
                type="primary" 
                @click="submitComment" 
                :loading="commentLoading"
                :disabled="!commentContent.trim()"
              >
                发表评论
              </el-button>
            </div>
          </div>
          <div class="comment-login-tip" v-else>
            <el-button type="primary" @click="$router.push('/login')">登录后发表评论</el-button>
          </div>

          <!-- 评论列表 -->
          <div class="comments-list" v-loading="commentsLoading">
            <template v-if="comments.length > 0">
              <div 
                v-for="comment in comments" 
                :key="comment.commentId" 
                class="comment-item"
              >
                <el-avatar :size="36" :src="comment.avatarUrl" class="comment-avatar">
                  {{ comment.nickname?.charAt(0) || comment.username?.charAt(0) || 'U' }}
                </el-avatar>
                <div class="comment-content">
                  <div class="comment-header">
                    <span class="comment-author">{{ comment.nickname || comment.username }}</span>
                    <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
                  </div>
                  <div class="comment-text">{{ comment.content }}</div>
                  <div class="comment-actions">
                    <span 
                      class="action-btn" 
                      :class="{ active: comment.isLiked }"
                      @click="handleCommentLike(comment)"
                    >
                      <el-icon><Star /></el-icon>
                      {{ comment.likeCount || 0 }}
                    </span>
                  </div>
                </div>
              </div>

              <div class="load-more" v-if="commentsHasMore">
                <el-button 
                  :loading="commentsLoading" 
                  @click="loadMoreComments" 
                  link
                >
                  加载更多
                </el-button>
              </div>
            </template>
            <el-empty v-else-if="!commentsLoading" description="暂无评论，快来抢沙发吧~" :image-size="80" />
          </div>
        </div>
      </template>

      <el-empty v-else-if="!loading" description="笔记不存在或已被删除">
        <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useNoteStore } from '@/store/note'
import { useUserStore } from '@/store/user'
import { noteApi } from '@/api/note'
import { ElMessage } from 'element-plus'
import { ArrowLeft, View, ChatLineRound, Star, StarFilled } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const noteStore = useNoteStore()
const userStore = useUserStore()

const note = ref(null)
const loading = ref(false)
const comments = ref([])
const commentsLoading = ref(false)
const lastCommentId = ref(null)
const commentsHasMore = ref(false)
const commentContent = ref('')
const commentLoading = ref(false)

onMounted(async () => {
  await loadNoteDetail()
  if (note.value) {
    await loadComments()
  }
})

const loadNoteDetail = async () => {
  loading.value = true
  try {
    note.value = await noteStore.fetchNoteDetail(route.params.noteId)
  } finally {
    loading.value = false
  }
}

const loadComments = async (loadMore = false) => {
  if (!loadMore) {
    commentsLoading.value = true
  }
  try {
    const res = await noteApi.getComments(route.params.noteId, lastCommentId.value, 20)
    const newComments = res.data?.items || []
    
    if (loadMore) {
      comments.value = [...comments.value, ...newComments]
    } else {
      comments.value = newComments
    }
    
    lastCommentId.value = res.data?.lastCommentId
    commentsHasMore.value = res.data?.hasMore
  } catch (e) {
  } finally {
    commentsLoading.value = false
  }
}

const loadMoreComments = () => {
  loadComments(true)
}

const submitComment = async () => {
  if (!commentContent.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  
  commentLoading.value = true
  try {
    await noteApi.createComment(route.params.noteId, {
      content: commentContent.value.trim()
    })
    ElMessage.success('评论成功')
    commentContent.value = ''
    lastCommentId.value = null
    await loadComments()
    note.value.commentCount = (note.value.commentCount || 0) + 1
  } catch (e) {
  } finally {
    commentLoading.value = false
  }
}

const handleLike = async () => {
  if (!userStore.isLoggedIn()) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  try {
    if (note.value.isLiked) {
      await noteStore.unlikeNote(note.value.noteId)
      note.value.isLiked = false
      note.value.likeCount = Math.max(0, (note.value.likeCount || 0) - 1)
      ElMessage.success('取消点赞')
    } else {
      await noteStore.likeNote(note.value.noteId)
      note.value.isLiked = true
      note.value.likeCount = (note.value.likeCount || 0) + 1
      ElMessage.success('点赞成功')
    }
  } catch (e) {
  }
}

const handleCollect = async () => {
  if (!userStore.isLoggedIn()) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  try {
    if (note.value.isCollected) {
      await noteStore.uncollectNote(note.value.noteId)
      note.value.isCollected = false
      note.value.collectCount = Math.max(0, (note.value.collectCount || 0) - 1)
      ElMessage.success('取消收藏')
    } else {
      await noteStore.collectNote(note.value.noteId)
      note.value.isCollected = true
      note.value.collectCount = (note.value.collectCount || 0) + 1
      ElMessage.success('收藏成功')
    }
  } catch (e) {
  }
}

const handleCommentLike = async (comment) => {
  if (!userStore.isLoggedIn()) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  try {
    if (comment.isLiked) {
      await noteApi.unlikeComment(comment.commentId)
      comment.isLiked = false
      comment.likeCount = Math.max(0, (comment.likeCount || 0) - 1)
    } else {
      await noteApi.likeComment(comment.commentId)
      comment.isLiked = true
      comment.likeCount = (comment.likeCount || 0) + 1
    }
  } catch (e) {
  }
}

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}

const goToUserProfile = () => {
  if (note.value?.userId) {
    router.push(`/user/${note.value.userId}`)
  }
}

const formatTime = (time) => {
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
  
  return time.replace('T', ' ').substring(0, 19)
}
</script>

<style scoped>
.note-detail-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 20px 40px;
}

.back-section {
  padding: 16px 0;
}

.back-btn {
  color: #666;
}

.back-btn:hover {
  color: #FF7D00;
}

.note-detail {
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.note-author {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.author-avatar {
  cursor: pointer;
  transition: transform 0.2s;
}

.author-avatar:hover {
  transform: scale(1.05);
}

.author-info {
  flex: 1;
}

.author-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  cursor: pointer;
  transition: color 0.2s;
}

.author-name:hover {
  color: #FF7D00;
}

.author-time {
  font-size: 13px;
  color: #999;
}

.note-title {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 24px;
  line-height: 1.4;
}

.note-content {
  font-size: 16px;
  color: #333;
  line-height: 1.8;
  white-space: pre-wrap;
  margin-bottom: 24px;
}

.note-cover {
  margin-bottom: 20px;
  border-radius: 12px;
  overflow: hidden;
}

.cover-image {
  width: 100%;
  max-height: 400px;
}

.note-images {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
  margin-bottom: 20px;
}

.note-image {
  width: 100%;
  height: 200px;
  border-radius: 8px;
  cursor: pointer;
}

.note-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.note-category {
  margin-bottom: 20px;
}

.note-interaction {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 0;
  border-top: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 32px;
}

.note-stats {
  display: flex;
  gap: 20px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #666;
}

.note-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-count {
  font-size: 14px;
  color: #666;
  min-width: 24px;
}

.comment-section {
  margin-top: 32px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0 0 20px;
}

.comment-input-wrapper {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.comment-input-inner {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: flex-end;
}

.comment-login-tip {
  padding: 20px;
  text-align: center;
  background: #f9f9f9;
  border-radius: 12px;
  margin-bottom: 24px;
}

.comments-list {
  min-height: 200px;
}

.comment-item {
  display: flex;
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px solid #f5f5f5;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-avatar {
  flex-shrink: 0;
}

.comment-content {
  flex: 1;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.comment-author {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.comment-time {
  font-size: 12px;
  color: #999;
}

.comment-text {
  font-size: 14px;
  color: #333;
  line-height: 1.6;
  margin-bottom: 8px;
}

.comment-actions {
  display: flex;
  gap: 16px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #999;
  cursor: pointer;
  transition: color 0.2s;
}

.action-btn:hover {
  color: #FF7D00;
}

.action-btn.active {
  color: #FF7D00;
}

.load-more {
  text-align: center;
  padding: 20px 0;
}

@media (max-width: 768px) {
  .note-detail {
    padding: 20px;
  }
  
  .note-title {
    font-size: 22px;
  }
  
  .note-interaction {
    flex-direction: column;
    gap: 16px;
  }
  
  .note-actions {
    width: 100%;
    justify-content: center;
  }
}
</style>
