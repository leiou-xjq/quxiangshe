<template>
  <div class="search-page">
    <!-- 搜索栏 -->
    <div class="search-header">
      <el-button :icon="ArrowLeft" circle @click="goToHome" class="back-btn" />
      <el-input
        v-model="keyword"
        placeholder="搜索笔记..."
        class="search-input"
        size="large"
        @keyup.enter="handleSearch"
        @focus="showHistory = true"
      >
        <template #append>
          <el-button :icon="Search" @click="handleSearch" />
        </template>
      </el-input>
    </div>
    
    <!-- 历史搜索记录 -->
    <div class="history-section" v-if="showHistory && searchHistory.length > 0">
      <div class="history-header">
        <span class="history-title">历史搜索</span>
        <span class="history-clear" @click="clearHistory">清除</span>
      </div>
      <div class="history-list">
        <span 
          v-for="(item, index) in searchHistory" 
          :key="index"
          class="history-item"
          @click="useHistory(item)"
        >{{ item }}</span>
      </div>
    </div>
    
    <!-- Tab切换 -->
    <div class="search-tabs">
      <span 
        class="tab-item" 
        :class="{ active: activeTab === 'notes' }"
        @click="switchTab('notes')"
      >笔记</span>
      <span 
        class="tab-item" 
        :class="{ active: activeTab === 'users' }"
        @click="switchTab('users')"
      >用户</span>
    </div>
    
    <!-- 标签筛选栏 - 仅笔记Tab显示 -->
    <div class="tag-filter" v-if="activeTab === 'notes'">
      <span 
        class="tag-item" 
        :class="{ active: selectedTags.length === 0 }"
        @click="selectTag(null)"
      >全部</span>
      <span 
        v-for="tag in tagList" 
        :key="tag"
        class="tag-item"
        :class="{ active: selectedTags.includes(tag) }"
        @click="toggleTag(tag)"
      >{{ tag }}</span>
    </div>
    
    <!-- 加载中 -->
    <div v-if="loading" class="loading-wrap">
      <div class="loading-spinner"></div>
      <p>搜索中...</p>
    </div>
    
    <!-- 空状态 -->
    <div v-else-if="!loading && resultList.length === 0 && hasSearched" class="empty-wrap">
      <p>未找到相关{{ activeTab === 'notes' ? '笔记' : '用户' }}</p>
    </div>
    
    <!-- 搜索结果 - 笔记 -->
    <div v-else-if="activeTab === 'notes'" class="result-list">
      <div 
        class="note-card" 
        v-for="item in resultList" 
        :key="item.id"
        @click="handleNoteClick(item)"
      >
        <div class="note-title">{{ item.title }}</div>
        <div class="note-tags" v-if="item.tags">
          <span 
            v-for="tag in parseTags(item.tags)" 
            :key="tag" 
            class="note-tag"
          >{{ tag }}</span>
        </div>
        <div class="note-stats">
          <span class="stat-item">
            <el-icon><Star /></el-icon>
            {{ item.likeCount || 0 }}
          </span>
          <span class="stat-item">
            <el-icon><ChatDotRound /></el-icon>
            {{ item.commentCount || 0 }}
          </span>
        </div>
      </div>
    </div>
    
    <!-- 搜索结果 - 用户 -->
    <div v-else-if="activeTab === 'users'" class="result-list">
      <div 
        class="user-card" 
        v-for="item in resultList" 
        :key="item.id"
        @click="handleUserClick(item)"
      >
        <el-avatar :src="item.avatar || '/avatar/default.png'" :size="48" />
        <div class="user-info">
          <div class="user-nickname">{{ item.nickname || item.username }}</div>
          <div class="user-bio" v-if="item.bio">{{ item.bio }}</div>
        </div>
      </div>
    </div>
    
    <!-- 加载更多（与首页一致） -->
    <div class="load-more" v-if="hasMore && resultList.length > 0">
      <el-button :loading="loading" @click="loadMore" class="load-more-btn">
        {{ loading ? '加载中...' : '加载更多' }}
      </el-button>
    </div>
    
    <!-- 无更多数据 -->
    <div v-else-if="hasSearched && resultList.length > 0" class="no-more-tip">
      没有更多了
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Search, Star, ChatDotRound, ArrowLeft } from '@element-plus/icons-vue'
import { searchNotes, searchUsers } from '@/api/search'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()

const keyword = ref('')
const activeTab = ref('notes')
const resultList = ref([])
const loading = ref(false)
const hasMore = ref(false)
const searchAfter = ref(null)
const hasSearched = ref(false)

// 标签相关
const tagList = ['生活', '美食', '旅行', '摄影', '健身', '读书', '音乐', '游戏']
const selectedTags = ref([])

// 历史搜索记录
const showHistory = ref(false)
const searchHistory = ref([])

// 加载历史记录
function loadHistory() {
  try {
    const history = localStorage.getItem('searchHistory')
    searchHistory.value = history ? JSON.parse(history) : []
  } catch (e) {
    searchHistory.value = []
  }
}

// 保存历史记录
function saveHistory(keyword) {
  if (!keyword.trim()) return
  let history = searchHistory.value.filter(h => h !== keyword)
  history.unshift(keyword)
  searchHistory.value = history.slice(0, 10)
  localStorage.setItem('searchHistory', JSON.stringify(searchHistory.value))
}

// 使用历史记录
function useHistory(keyword) {
  keyword.value = keyword
  showHistory.value = false
  handleSearch()
}

// 清除历史记录
function clearHistory() {
  searchHistory.value = []
  localStorage.removeItem('searchHistory')
}

onMounted(() => {
  loadHistory()
  if (route.query.keyword) {
    keyword.value = route.query.keyword
    handleSearch()
  }
  
  // 点击其他地方隐藏历史记录
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

function handleClickOutside(e) {
  if (!e.target.closest('.search-header') && !e.target.closest('.history-section')) {
    showHistory.value = false
  }
}

function switchTab(tab) {
  if (activeTab.value === tab) return
  activeTab.value = tab
  resultList.value = []
  searchAfter.value = null
  hasMore.value = false
  hasSearched.value = false
  if (keyword.value) {
    handleSearch()
  }
}

// 选择标签（单选）
function selectTag(tag) {
  selectedTags.value = tag ? [tag] : []
  handleSearch()
}

// 切换标签（多选）
function toggleTag(tag) {
  if (selectedTags.value.includes(tag)) {
    selectedTags.value = selectedTags.value.filter(t => t !== tag)
  } else {
    selectedTags.value.push(tag)
  }
  handleSearch()
}

// 解析标签
function parseTags(tagsStr) {
  if (!tagsStr) return []
  try {
    return JSON.parse(tagsStr)
  } catch (e) {
    return []
  }
}

// 搜索（与首页一致：首次搜索替换列表，分页追加）
async function handleSearch() {
  // 笔记Tab：需要有关键词或标签（否则提示）；用户Tab：可以直接搜索
  if (activeTab.value === 'notes' && !keyword.value.trim() && selectedTags.value.length === 0) {
    ElMessage.warning('请输入搜索关键词或选择标签')
    return
  }
  
  // 保存历史记录
  if (keyword.value.trim()) {
    saveHistory(keyword.value)
  }
  
  // 隐藏历史记录
  showHistory.value = false
  
  loading.value = true
  searchAfter.value = null
  hasMore.value = false
  hasSearched.value = true
  
  try {
    if (activeTab.value === 'notes') {
      const res = await searchNotes(keyword.value, 20, null, selectedTags.value.length > 0 ? selectedTags.value : null)
      resultList.value = res.data?.data || []
      hasMore.value = res.data?.hasMore || false
      searchAfter.value = res.data?.nextSearchAfter || null
    } else {
      const res = await searchUsers(keyword.value, 20)
      resultList.value = res.data?.data || []
      hasMore.value = res.data?.hasMore || false
      searchAfter.value = res.data?.nextSearchAfter || null
    }
  } catch (e) {
    ElMessage.error('搜索失败')
  } finally {
    loading.value = false
  }
}

// 加载更多（与首页一致）
function loadMore() {
  if (loading.value || !hasMore.value) return
  doLoadMore()
}

// 加载更多核心逻辑
async function doLoadMore() {
  if (!searchAfter.value) {
    console.log('searchAfter为空，无法加载更多')
    return
  }
  
  console.log('加载更多: searchAfter=', searchAfter.value)
  
  loading.value = true
  try {
    if (activeTab.value === 'notes') {
      const res = await searchNotes(keyword.value, 20, searchAfter.value, selectedTags.value.length > 0 ? selectedTags.value : null)
      const newList = res.data?.data || []
      resultList.value.push(...newList)
      hasMore.value = res.data?.hasMore || false
      searchAfter.value = res.data?.nextSearchAfter || null
    } else {
      const res = await searchUsers(keyword.value, 20, searchAfter.value)
      const newList = res.data?.data || []
      resultList.value.push(...newList)
      hasMore.value = res.data?.hasMore || false
      searchAfter.value = res.data?.nextSearchAfter || null
    }
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function handleNoteClick(item) {
  router.push(`/note/${item.id}`)
}

function handleUserClick(item) {
  router.push(`/user/${item.id}`)
}

function goToHome() {
  router.push('/')
}
</script>

<style scoped>
@import '@/styles/design-tokens.css';

.search-page {
  max-width: 800px;
  margin: 0 auto;
}

.search-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.back-btn {
  flex-shrink: 0;
}

.search-input {
  flex: 1;
}

/* 历史搜索记录 */
.history-section {
  background: var(--color-bg);
  border-radius: var(--radius-md);
  padding: var(--spacing-md);
  margin-bottom: var(--spacing-md);
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-sm);
}

.history-title {
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);
}

.history-clear {
  font-size: var(--font-size-sm);
  color: var(--color-primary);
  cursor: pointer;
}

.history-clear:hover {
  text-decoration: underline;
}

.history-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-sm);
}

.history-item {
  padding: 6px 12px;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  background: var(--color-bg-secondary);
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.history-item:hover {
  background: var(--color-bg-tertiary);
}

.search-tabs {
  display: flex;
  gap: 20px;
  margin-bottom: 16px;
  border-bottom: 1px solid var(--color-border);
}

.tab-item {
  padding: 10px 20px;
  cursor: pointer;
  color: var(--color-text-secondary);
  border-bottom: 2px solid transparent;
  transition: all var(--transition-fast);
}

.tab-item:hover {
  color: var(--color-primary);
}

.tab-item.active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
  font-weight: 600;
}

/* 标签筛选栏 */
.tag-filter {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
  padding: 12px;
  background: var(--color-bg);
  border-radius: var(--radius-md);
}

.tag-item {
  padding: 6px 14px;
  font-size: 13px;
  color: var(--color-text-secondary);
  background: var(--color-bg-secondary);
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.tag-item:hover {
  background: var(--color-bg-tertiary);
}

.tag-item.active {
  color: var(--color-text-reverse);
  background: var(--color-primary);
}

.loading-wrap {
  text-align: center;
  padding: 60px 0;
  color: var(--color-text-tertiary);
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 12px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-wrap {
  text-align: center;
  padding: 60px 0;
  color: var(--color-text-tertiary);
}

.result-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.note-card {
  background: var(--color-bg);
  border-radius: var(--radius-md);
  padding: 16px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.note-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.note-title {
  font-size: 16px;
  font-weight: 500;
  color: var(--color-text);
  margin-bottom: 8px;
}

.note-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}

.note-tag {
  padding: 2px 8px;
  font-size: 12px;
  color: var(--color-primary);
  background: var(--color-bg-secondary);
  border-radius: var(--radius-full);
}

.note-stats {
  display: flex;
  gap: 16px;
  color: var(--text-tertiary);
  font-size: 14px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--color-bg);
  border-radius: var(--radius-md);
  padding: 16px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.user-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.user-info {
  flex: 1;
}

.user-nickname {
  font-size: 16px;
  font-weight: 500;
  color: var(--color-text);
}

.user-bio {
  font-size: 14px;
  color: var(--color-text-tertiary);
  margin-top: 4px;
}

.load-more {
  text-align: center;
  margin-top: 20px;
}

.load-more-btn {
  width: 200px;
}

.no-more-tip {
  text-align: center;
  padding: 20px 0;
  color: var(--color-text-tertiary);
  font-size: 14px;
}
</style>