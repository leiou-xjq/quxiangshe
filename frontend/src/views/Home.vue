<template>
  <div class="home-page">
    <!-- 信息流 -->
    <div class="feed-section">
      <div class="feed-header">
        <h3 class="feed-title">发现精彩</h3>
        <div class="feed-tabs">
          <span 
            class="feed-tab" 
            :class="{ active: activeTab === 'discover' }"
            @click="switchTab('discover')"
          >发现精彩</span>
          <span 
            class="feed-tab" 
            :class="{ active: activeTab === 'follow' }"
            @click="switchTab('follow')"
          >
            关注
            <span v-if="activeTab !== 'follow' && hasFollowUpdate" class="red-dot"></span>
          </span>
          <span 
            class="feed-tab" 
            :class="{ active: activeTab === 'popular' }"
            @click="switchTab('popular')"
          >热门</span>
        </div>
      </div>
      
      <!-- 加载中 - 骨架屏 -->
      <div v-if="loading && feedList.length === 0" class="skeleton-wrap">
        <SkeletonCard v-for="i in 5" :key="i" :image-count="i % 3 + 1" />
      </div>
      
      <!-- 空状态 -->
      <div v-else-if="!loading && feedList.length === 0" class="empty-wrap">
        <p v-if="activeTab === 'follow' && !hasFollowing">{{ emptyTip }}</p>
        <p v-else-if="activeTab === 'follow'">{{ emptyTip }}</p>
        <p v-else>还没有任何笔记，快来 <router-link to="/publish">发布第一篇</router-link> 吧！</p>
      </div>
      
      <!-- 笔记列表 - 使用 NoteCard 组件 -->
      <div v-else class="feed-list">
        <NoteCard 
          v-for="item in feedList" 
          :key="item.id"
          :note="item"
        />
      </div>
      
      <!-- 加载更多 -->
      <div class="load-more" v-if="hasMore && feedList.length > 0">
        <el-button :loading="loading" @click="loadMore" class="load-more-btn">
          {{ loading ? '加载中...' : '加载更多' }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { getFeed, getDiscoverFeed, getPopularFeed, getFollowHasUpdate, clearFollowUpdate } from '@/api/note'
import { getFollowingCount } from '@/api/follow'
import NoteCard from '@/components/NoteCard.vue'
import SkeletonCard from '@/components/SkeletonCard.vue'

const userStore = useUserStore()

const feedList = ref([])
const loading = ref(false)
const cursor = ref('')
const hasMore = ref(true)
const activeTab = ref('discover')
const hasFollowing = ref(false)
const hasFollowUpdate = ref(false)
let checkUpdateTimer = null

// 空状态文案
const emptyTip = computed(() => {
  if (!hasFollowing.value) {
    return '还没有关注任何人，快去发现感兴趣的用户吧！'
  }
  return '关注的人还没有发布笔记'
})

// 切换Tab
function switchTab(tab) {
  if (activeTab.value === tab) return
  activeTab.value = tab
  feedList.value = []
  cursor.value = ''
  hasMore.value = true
  
  // 如果切换到关注tab，检查是否有关注，并清除红点
  if (tab === 'follow') {
    checkFollowingStatus()
    clearFollowUpdateMark()
  }
  
  loadNotes()
}

// 清除关注Tab红点标记
async function clearFollowUpdateMark() {
  if (!userStore.userInfo) return
  try {
    await clearFollowUpdate()
    hasFollowUpdate.value = false
  } catch (e) {
    // 忽略错误
  }
}

// 检查关注Tab是否有更新
async function checkFollowUpdate() {
  if (!userStore.userInfo) return
  try {
    const res = await getFollowHasUpdate()
    hasFollowUpdate.value = res.data?.hasUpdate || false
  } catch (e) {
    hasFollowUpdate.value = false
  }
}

// 检查关注状态
async function checkFollowingStatus() {
  if (!userStore.userInfo) return
  try {
    const res = await getFollowingCount(userStore.userInfo.id)
    hasFollowing.value = (res.data || 0) > 0
  } catch (e) {
    hasFollowing.value = false
  }
}

// 获取当前API
function getCurrentApi() {
  switch (activeTab.value) {
    case 'follow':
      return getFeed
    case 'popular':
      return getPopularFeed
    case 'discover':
    default:
      return getDiscoverFeed
  }
}

// 加载笔记列表
async function loadNotes() {
  if (loading.value) return
  loading.value = true
  
  try {
    const api = getCurrentApi()
    const res = await api(cursor.value, 20)
    const newNotes = res.data?.data || []
    
    if (cursor.value === '') {
      feedList.value = newNotes
    } else {
      feedList.value.push(...newNotes)
    }
    
    hasMore.value = res.data?.hasMore || false
    cursor.value = res.data?.nextCursor || ''
  } catch (e) {
    // 忽略错误
  } finally {
    loading.value = false
  }
}

// 加载更多
function loadMore() {
  loadNotes()
}

onMounted(() => {
  loadNotes()
  // 检查关注Tab是否有更新
  checkFollowUpdate()
  
  // 每30秒检查一次红点（不在关注Tab时）
  checkUpdateTimer = setInterval(() => {
    if (activeTab.value !== 'follow' && userStore.userInfo) {
      checkFollowUpdate()
    }
  }, 30000)
})

onUnmounted(() => {
  if (checkUpdateTimer) {
    clearInterval(checkUpdateTimer)
  }
})
</script>

<style scoped>
@import '@/styles/home.css';

/* 加载状态 */
.loading-wrap {
  text-align: center;
  padding: 60px 0;
  color: var(--text-tertiary);
}

.loading-wrap .loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--color-gray-200);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 12px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 空状态 */
.empty-wrap {
  text-align: center;
  padding: 60px 0;
  background: var(--color-white);
  border-radius: var(--radius-lg);
  color: var(--text-tertiary);
}

.empty-wrap a {
  color: var(--color-primary);
}
</style>
