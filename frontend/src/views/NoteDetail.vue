<template>
  <div class="note-detail-page" v-loading="loading">
    <!-- 笔记主体 -->
    <div class="note-card">
      <!-- 作者信息 -->
      <div class="note-author">
        <img :src="note.avatar || '/avatar/default.png'" class="author-avatar" />
        <div class="author-info">
          <div class="author-name">{{ note.nickname || '未知用户' }}</div>
          <div class="author-meta">
            {{ formatTime(note.createdAt) }}
          </div>
        </div>
        <!-- 关注按钮/删除按钮 -->
        <div class="author-actions">
          <template v-if="userStore.isLoggedIn && note.userId && note.userId === userStore.userInfo?.id">
            <el-button 
              type="danger" 
              size="small"
              @click="handleDelete"
            >
              删除
            </el-button>
          </template>
          <template v-else-if="userStore.isLoggedIn && note.userId && note.userId !== userStore.userInfo?.id">
            <el-button 
              :type="isFollowing ? 'default' : 'primary'" 
              size="small"
              @click="toggleFollow"
            >
              {{ isFollowing ? '已关注' : '关注' }}
            </el-button>
            <el-button 
              type="danger" 
              size="small"
              @click="showReportDialog"
            >
              举报
            </el-button>
          </template>
        </div>
      </div>
      
      <!-- 标题 -->
      <h1 class="note-title">{{ note.title }}</h1>
      
      <!-- 内容 -->
      <div class="note-content">{{ note.content }}</div>
      
      <!-- 标签 -->
      <div class="note-tags" v-if="note.tags && note.tags.length">
        <span v-for="tag in note.tags" :key="tag" class="tag-item">#{{ tag }}</span>
      </div>
      
      <!-- 图片 -->
      <div class="note-images" v-if="note.images && note.images.length">
        <img 
          v-for="(img, index) in note.images" 
          :key="index" 
          :src="img" 
          class="note-image"
          @click="previewImage(index)"
        />
      </div>
      
      <!-- 视频 -->
      <div class="note-video" v-if="note.video">
        <video 
          :src="note.video" 
          controls
          preload="metadata"
          playsinline
          webkit-playsinline
        ></video>
      </div>
      
      <!-- 位置 -->
      <div class="note-location" v-if="note.location">
        <el-icon><Location /></el-icon>
        {{ note.location }}
      </div>
      
      <!-- 互动按钮 -->
      <div class="note-actions">
        <div class="action-item like-btn" :class="{ active: note.liked, 'animating': animatingLike }" @click="handleLike">
          <svg class="action-icon" viewBox="0 0 24 24">
            <path v-if="note.liked" fill="currentColor" d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
            <path v-else fill="none" stroke="currentColor" stroke-width="2" d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
          </svg>
          <span>{{ note.likeCount || 0 }}</span>
        </div>
        <div class="action-item favorite-btn" :class="{ active: note.favorited, 'animating': animatingFavorite }" @click="handleFavorite">
          <svg class="action-icon" viewBox="0 0 24 24">
            <path v-if="note.favorited" fill="currentColor" d="M17 3H7c-1.1 0-2 .9-2 2v16l7-3 7 3V5c0-1.1-.9-2-2-2z"/>
            <path v-else fill="none" stroke="currentColor" stroke-width="2" d="M17 3H7c-1.1 0-2 .9-2 2v16l7-3 7 3V5c0-1.1-.9-2-2-2z"/>
          </svg>
          <span>{{ note.favoriteCount || 0 }}</span>
        </div>
        <div class="action-item" @click="handleShare">
          <el-icon><Share /></el-icon>
          <span>分享</span>
        </div>
      </div>
    </div>
    
    <!-- 评论区域 -->
    <div class="comment-section">
      <div class="comment-header">
        <h3>评论 <span class="comment-count">({{ note.commentCount || 0 }})</span></h3>
      </div>
      
      <!-- 发表评论 -->
      <div class="comment-input" v-if="userStore.isLoggedIn">
        <img :src="userStore.userInfo?.avatar || '/avatar/default.png'" class="comment-avatar" />
        <div class="comment-input-box">
          <el-input 
            v-model="commentContent" 
            type="textarea" 
            placeholder="说点什么..." 
            :rows="2"
            maxlength="500"
          />
          <el-button type="primary" size="small" :loading="commentLoading" @click="submitComment">
            发表评论
          </el-button>
        </div>
      </div>
      <div class="comment-login-tip" v-else>
        <router-link to="/login">登录</router-link> 后参与评论
      </div>
      
      <!-- 评论列表 -->
      <div class="comment-list">
        <div v-if="rootCommentsData.length === 0" class="comment-empty">
          暂无评论，快来抢沙发吧
        </div>
        
        <!-- 根评论 + 子评论 -->
        <div v-for="root in rootCommentsData" :key="root.id" class="comment-group">
          <!-- 根评论：顶格无缩进 -->
          <div class="comment-item">
            <img :src="root.avatar || '/avatar/default.png'" class="comment-avatar" @click="goToUser(root.userId)" />
            <div class="comment-content-wrap">
              <div class="comment-nickname" @click="goToUser(root.userId)">{{ root.nickname }}</div>
              <div class="comment-text">{{ root.content }}</div>
              <div class="comment-meta">
                <span class="comment-time">{{ formatTime(root.createdAt) }}</span>
                <span class="comment-like" :class="{ 'liked': root.liked }" @click="handleLikeComment(root)">
                  <el-icon><Star v-if="!root.liked" /><StarFilled v-else /></el-icon>
                  {{ root.likeCount || 0 }}
                </span>
                <span class="comment-reply" @click="handleReply(root)">回复</span>
                <span 
                  class="comment-delete" 
                  v-if="userStore.isLoggedIn && root.userId === userStore.userInfo?.id"
                  @click="handleDeleteComment(root)"
                >
                  删除
                </span>
                <span 
                  class="comment-expand" 
                  v-if="root.hasMoreChildren || root.children.length > 0"
                  @click="toggleChildren(root.id)"
                >
                  {{ root.expanded ? '收起' : '展开回复' }}
                </span>
              </div>
            </div>
          </div>
          
          <!-- 扁平化显示所有子评论：根评论下面的所有后代评论统一缩进 -->
          <template v-if="root.expanded && root.children && root.children.length">
            <template v-for="item in getAllDescendants(root.children)">
              <div class="comment-item comment-reply-item" style="margin-left:20px;">
                <img :src="item.avatar || '/avatar/default.png'" class="comment-avatar" @click="goToUser(item.userId)" />
                <div class="comment-content-wrap">
                  <div class="comment-nickname" @click="goToUser(item.userId)">
                    {{ item.nickname }}
                    <span class="reply-to" v-if="item.replyToNickname">回复 @{{ item.replyToNickname }}</span>
                  </div>
                  <div class="comment-text">{{ item.content }}</div>
                  <div class="comment-meta">
                    <span class="comment-time">{{ formatTime(item.createdAt) }}</span>
                    <span class="comment-like" :class="{ 'liked': item.liked }" @click="handleLikeComment(item)">
                      <el-icon><Star v-if="!item.liked" /><StarFilled v-else /></el-icon>
                      {{ item.likeCount || 0 }}
                    </span>
                    <span class="comment-reply" @click="handleReply(item)">回复</span>
                    <span 
                      class="comment-delete" 
                      v-if="userStore.isLoggedIn && item.userId === userStore.userInfo?.id"
                      @click="handleDeleteComment(item)"
                    >删除</span>
                  </div>
                </div>
              </div>
            </template>
          </template>
        </div>
        
        <!-- 加载更多根评论 -->
        <div v-if="rootHasMore" class="load-more-roots" @click="loadMoreRootComments">
          <span v-if="rootLoading">加载中...</span>
          <span v-else>展开更多评论</span>
        </div>
      </div>
    </div>
    
    <!-- 返回按钮 -->
    <div class="back-btn" @click="goBack">
      <el-icon><ArrowLeft /></el-icon>
    </div>
    
    <!-- 举报弹窗 -->
    <el-dialog v-model="reportDialogVisible" title="举报" width="400px">
      <el-form label-width="80px">
        <el-form-item label="举报原因">
          <el-radio-group v-model="reportReason">
            <el-radio :value="1">垃圾广告</el-radio>
            <el-radio :value="2">涉黄</el-radio>
            <el-radio :value="3">抄袭</el-radio>
            <el-radio :value="4">其他</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="详细说明">
          <el-input
            v-model="reportDescription"
            type="textarea"
            :rows="3"
            placeholder="请详细描述举报原因..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleReport">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Location, Star, StarFilled, Share, ArrowLeft } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getFollowStatus, followUser, unfollowUser } from '@/api/follow'
import { 
  getNoteDetail, 
  deleteNote,
  likeNote, 
  unlikeNote, 
  favoriteNote, 
  unfavoriteNote,
  getCommentList,
  addComment,
  deleteComment,
  likeComment,
  unlikeComment
} from '@/api/note'
import { submitReport, TargetType, ReportReason } from '@/api/report'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const likedComments = ref(new Set())

function processComments(list) {
  if (!list || !list.length) return
  for (const c of list) {
    c.id = c.commentId
    c.liked = likedComments.value.has(c.commentId)
    // 初始化分页
    c.childrenPage = 1
    c.hasMoreChildren = false
    if (c.children && c.children.length) {
      // 扁平化：所有子评论孙评论等都统一 depth=1
      c.children.forEach(cc => {
        cc.id = cc.commentId
        cc.liked = likedComments.value.has(cc.commentId)
      })
    }
  }
}

function flattenAllChildren(list, page = 1, perPage = 20) {
  if (!list || !list.length || !list[0].children) return []
  
  const allChildren = list[0].children || []
  const end = page * perPage
  return allChildren.slice(0, end)
}

// 递归获取所有后代评论
function getAllDescendants(list) {
  const result = []
  if (!list) return result
  for (const item of list) {
    result.push(item)
    if (item.children && item.children.length) {
      result.push(...getAllDescendants(item.children))
    }
  }
  return result
}

const loading = ref(false)
const commentLoading = ref(false)
const note = ref({})
const animatingLike = ref(false)
const animatingFavorite = ref(false)
const rootComments = ref([])
const rootCommentsData = ref([])
const childrenMap = ref(new Map())
const commentContent = ref('')
const replyingTo = ref(null)
const isFollowing = ref(false)
const reportDialogVisible = ref(false)
const reportReason = ref(1)
const reportDescription = ref('')
const nextCursor = ref(null)
const rootHasMore = ref(false)
const rootLoading = ref(false)

const noteId = route.params.id

// 加载关注状态
async function loadFollowStatus() {
  if (!userStore.isLoggedIn) return
  if (!note.value.userId) return
  if (note.value.userId === userStore.userInfo?.id) return
  
  try {
    const res = await getFollowStatus(note.value.userId)
    isFollowing.value = res.data
  } catch (e) {
    // 忽略
  }
}

// 切换关注状态
async function toggleFollow() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  if (!note.value.userId) return
  
  try {
    if (isFollowing.value) {
      await unfollowUser(note.value.userId)
      isFollowing.value = false
      ElMessage.success('已取消关注')
    } else {
      await followUser(note.value.userId)
      isFollowing.value = true
      ElMessage.success('关注成功')
    }
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

// 删除笔记
async function handleDelete() {
  try {
    await ElMessageBox.confirm('确定要删除这条笔记吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await deleteNote(noteId)
    ElMessage.success('删除成功')
    router.push('/')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '删除失败')
    }
  }
}

// 加载笔记详情
async function loadNoteDetail() {
  loading.value = true
  try {
    const res = await getNoteDetail(noteId)
    note.value = res.data
  } catch (e) {
    ElMessage.error('加载失败')
    router.push('/')
  } finally {
    loading.value = false
  }
}

// 加载评论列表
async function loadComments(cursor = null, append = false) {
  // 从localStorage获取之前展开的评论ID
  const storedExpandedIds = getExpandedCommentIds()
  
  const expandedIds = new Set()
  if (!append) {
    // 合并之前展开的状态
    storedExpandedIds.forEach(id => expandedIds.add(id))
    rootCommentsData.value.forEach(r => {
      if (r.expanded) {
        expandedIds.add(r.id)
      }
    })
  }
  
  rootLoading.value = true
  
  try {
    // 每次加载10条，按热度排序
    const res = await getCommentList(noteId, 10, cursor)
    const data = res.data || { roots: [], totalRoots: 0, cursor: null }
    const roots = data.roots || []
    
likedComments.value = getLikedComments()
    
    // 处理评论数据，为子评论添加id字段
    processComments(roots)
    
    if (append) {
      const newRootData = roots.map(r => ({
        ...r,
        id: r.commentId,
        liked: likedComments.value.has(r.commentId),
        expanded: false,
        childrenLoading: false
      }))
      rootCommentsData.value.push(...newRootData)
    } else {
      rootCommentsData.value = roots.map(r => ({
        ...r,
        id: r.commentId,
        liked: likedComments.value.has(r.commentId),
        expanded: expandedIds.has(r.commentId),
        childrenLoading: false
      }))
    }
    
    rootHasMore.value = rootCommentsData.value.length < data.totalRoots
    nextCursor.value = data.cursor
  } catch (e) {
    console.error('加载评论失败:', e)
  } finally {
    rootLoading.value = false
  }
}

// 加载更多根评论
function loadMoreRootComments() {
  if (!rootHasMore.value || rootLoading.value) return
  loadComments(nextCursor.value, true)
}

// 获取localStorage中保存的展开状态
function getExpandedCommentIds() {
  const storageKey = `expanded_comments_${noteId}`
  const stored = localStorage.getItem(storageKey)
  if (stored) {
    try {
      return new Set(JSON.parse(stored))
    } catch (e) {
      return new Set()
    }
  }
  return new Set()
}

// 保存展开状态到localStorage
function saveExpandedCommentIds(ids) {
  const storageKey = `expanded_comments_${noteId}`
  localStorage.setItem(storageKey, JSON.stringify([...ids]))
}

// 展开/收起子评论
function toggleChildren(rootId) {
  const rootData = rootCommentsData.value.find(r => r.id === rootId)
  if (!rootData) return
  
  rootData.expanded = !rootData.expanded
  
  // 保存到localStorage
  const expandedIds = getExpandedCommentIds()
  if (rootData.expanded) {
    expandedIds.add(rootId)
  } else {
    expandedIds.delete(rootId)
  }
  saveExpandedCommentIds(expandedIds)
  
  if (rootData.expanded && rootData.children) {
    // 首次展开，保存原始children数组用于分页
    rootData.originalChildren = [...rootData.children]
    rootData.allChildrenCount = rootData.children.length
    rootData.childrenPage = 1
    const initialChildren = rootData.children.slice(0, 20)
    rootData.children = initialChildren
    rootData.hasMoreChildren = rootData.children.length < rootData.allChildrenCount
  }
}

// 加载更多子评论
function loadMoreChildren(rootId) {
  const rootData = rootCommentsData.value.find(r => r.id === rootId)
  if (!rootData || !rootData.expanded || !rootData.allChildrenCount) return
  
  const originalChildren = rootData.originalChildren
  if (!originalChildren) return
  
  rootData.childrenPage = (rootData.childrenPage || 1) + 1
  const pageSize = 20
  const end = rootData.childrenPage * pageSize
  rootData.children = originalChildren.slice(0, end)
  rootData.hasMoreChildren = end < rootData.allChildrenCount
}

// 获取本地存储的用户点赞评论ID集合
function getLikedComments() {
  const storageKey = `liked_comments_${userStore.userInfo?.id}_${noteId}`
  const saved = localStorage.getItem(storageKey)
  return saved ? new Set(JSON.parse(saved)) : new Set()
}

// 保存点赞状态到本地
function saveLikedComments(likedSet) {
  const storageKey = `liked_comments_${userStore.userInfo?.id}_${noteId}`
  localStorage.setItem(storageKey, JSON.stringify([...likedSet]))
}

// 删除评论 - 只能删除自己的评论，或笔记发布者删除任意评论
async function handleDeleteComment(comment) {
  // 检查是否登录
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  
  const currentUserId = userStore.userInfo?.id
  const noteUserId = note.value.userId
  
  console.log('Delete check:', { commentUserId: comment.userId, currentUserId, noteUserId, isLoggedIn: userStore.isLoggedIn })
  
  // 判断权限：是自己发布的评论，或者是笔记发布者
  const canDelete = (comment.userId === currentUserId) || (noteUserId === currentUserId)
  
  if (!canDelete) {
    console.log('Cannot delete: comment.userId=%s, currentUserId=%s, noteUserId=%s', comment.userId, currentUserId, noteUserId)
    ElMessage.warning('只能删除自己的评论')
    return
  }
  
  try {
    const tips = comment.children && comment.children.length 
      ? '删除评论后，其下的所有回复也会一并删除，确定删除吗？' 
      : '确定删除这条评论吗？'
    await ElMessageBox.confirm(tips, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await deleteComment(comment.commentId, noteId)
    ElMessage.success('删除成功')
    
    // 重新加载评论和笔记详情
    loadComments()
    loadNoteDetail()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '删除失败')
    }
  }
}

// 格式化时间
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

// 点赞
async function handleLike() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  try {
    if (note.value.liked) {
      await unlikeNote(noteId)
      note.value.liked = false
      note.value.likeCount = Math.max(0, (note.value.likeCount || 1) - 1)
    } else {
      await likeNote(noteId)
      note.value.liked = true
      note.value.likeCount = (note.value.likeCount || 0) + 1
      animatingLike.value = true
      setTimeout(() => { animatingLike.value = false }, 400)
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

// 收藏
async function handleFavorite() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  try {
    if (note.value.favorited) {
      await unfavoriteNote(noteId)
      note.value.favorited = false
      note.value.favoriteCount = Math.max(0, (note.value.favoriteCount || 1) - 1)
    } else {
      await favoriteNote(noteId)
      note.value.favorited = true
      note.value.favoriteCount = (note.value.favoriteCount || 0) + 1
      animatingFavorite.value = true
      setTimeout(() => { animatingFavorite.value = false }, 500)
      ElMessage.success('收藏成功')
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

// 分享
function handleShare() {
  ElMessageBox.confirm('复制链接分享给好友', '分享', {
    confirmButtonText: '复制链接',
    cancelButtonText: '取消',
  }).then(() => {
    const url = window.location.origin + `/note/${noteId}`
    navigator.clipboard.writeText(url)
    ElMessage.success('链接已复制')
  }).catch(() => {})
}

// 返回
function goBack() {
  router.back()
}

// 跳转用户主页
function goToUser(userId) {
  if (userId) {
    router.push(`/user/${userId}`)
  }
}

// 预览图片
function previewImage(index) {
  // 简单处理，实际可使用图片预览组件
  ElMessage.info(`图片 ${index + 1}`)
}

// 回复评论（根评论或子评论）
function handleReply(comment) {
  console.log('handleReply called, comment:', comment)
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  // 直接使用commentId，不是id
  replyingTo.value = {
    parentId: comment.commentId,
    toNickname: comment.nickname
  }
  console.log('replyingTo set to:', replyingTo.value)
  // 清空评论框内容（让用户自己输入）
  commentContent.value = ''
  // 滚动到评论框
  nextTick(() => {
    const commentInput = document.querySelector('.comment-input')
    if (commentInput) {
      commentInput.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
    // 让输入框获得焦点
    const textarea = document.querySelector('.comment-input-box textarea')
    if (textarea) {
      textarea.focus()
    }
  })
}

// 点赞评论
async function handleLikeComment(comment) {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  
  const commentId = comment.commentId || comment.id
  const storageKey = `liked_comments_${userStore.userInfo?.id}_${noteId}`
  const likedSet = getLikedComments()
  
  try {
    if (likedSet.has(commentId)) {
      // 取消点赞
      await unlikeComment(commentId)
      likedSet.delete(commentId)
      comment.liked = false
      comment.likeCount = Math.max(0, (comment.likeCount || 1) - 1)
      ElMessage.success('取消点赞')
    } else {
      // 添加点赞
      await likeComment(commentId)
      likedSet.add(commentId)
      comment.liked = true
      comment.likeCount = (comment.likeCount || 0) + 1
      ElMessage.success('点赞成功')
    }
    // 保存到本地存储
    saveLikedComments(likedSet)
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

// 发表评论/回复
async function submitComment() {
  if (!commentContent.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  
  commentLoading.value = true
  try {
    let content = commentContent.value.trim()
    
    const data = {
      noteId: parseInt(noteId),
      content: content
    }
    
    if (replyingTo.value) {
      data.parentId = replyingTo.value.parentId
      console.log('Submitting comment with parentId:', data.parentId, 'toNickname:', replyingTo.value.toNickname)
      data.content = `回复 @${replyingTo.value.toNickname}：${content}`
    } else {
      console.log('Submitting root comment, no parentId')
    }
    
    await addComment(data)
    ElMessage.success('评论成功')
    commentContent.value = ''
    replyingTo.value = null
    
    loadComments()
    loadNoteDetail()
  } catch (e) {
    if (e.message?.includes('违规')) {
      ElMessage.error('评论内容涉嫌违规')
    }
  } finally {
    commentLoading.value = false
  }
}

// 显示举报弹窗
function showReportDialog() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  reportDialogVisible.value = true
}

// 提交举报
async function handleReport() {
  try {
    await submitReport(TargetType.NOTE, noteId, reportReason.value, reportDescription.value)
    ElMessage.success('举报成功')
    reportDialogVisible.value = false
    reportReason.value = 1
    reportDescription.value = ''
  } catch (e) {
    ElMessage.error(e.message || '举报失败')
  }
}

onMounted(async () => {
  if (userStore.isLoggedIn && !userStore.userInfo) {
    await userStore.fetchUserInfo()
  }
  await Promise.all([
    loadNoteDetail(),
    loadComments(),
    loadFollowStatus()
  ])
})
</script>

<style scoped>
@import '@/styles/note-detail.css';
</style>
