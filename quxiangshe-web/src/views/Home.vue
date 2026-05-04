<template>
  <div class="home-page">
    <!-- Hero区域 -->
    <div class="hero-section">
      <h1 class="hero-title">发现美好，分享生活</h1>
      <p class="hero-subtitle">记录你的精彩瞬间，分享给更多人</p>
    </div>

    <!-- 搜索栏 -->
    <div class="search-section">
      <div class="search-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索笔记、用户..."
          prefix-icon="Search"
          clearable
          @keyup.enter="handleSearch"
          @clear="clearSearch"
          class="search-input"
        >
          <template #append>
            <el-button :icon="Search" @click="handleSearch" class="search-btn" />
          </template>
        </el-input>
      </div>
    </div>

    <!-- 分类标签 -->
    <div class="category-section">
      <div class="category-tabs">
        <span 
          v-for="cat in categories" 
          :key="cat"
          class="category-tag"
          :class="{ active: activeCategory === cat }"
          @click="handleCategoryChange(cat)"
        >
          {{ cat }}
        </span>
      </div>
    </div>

    <!-- 笔记列表 -->
    <div class="notes-container" v-loading="noteStore.loading" element-loading-text="加载中...">
      <template v-if="noteStore.homeNotes.length > 0">
        <div class="notes-grid">
          <NoteCard
            v-for="(note, index) in noteStore.homeNotes"
            :key="note.noteId"
            :note="note"
            :keyword="searchKeyword"
            :style="{ animationDelay: `${index * 0.05}s` }"
            class="note-card-animate"
            @like="handleLike"
            @collect="handleCollect"
          />
        </div>
        
        <!-- 加载更多 -->
        <div class="load-more" v-if="noteStore.hasMore">
          <el-button 
            :loading="noteStore.loading" 
            @click="loadMore"
            type="primary" 
            link
          >
            加载更多
          </el-button>
        </div>
        
        <el-empty 
          v-if="!noteStore.hasMore && noteStore.homeNotes.length > 0" 
          description="没有更多了"
        />
      </template>
      
      <!-- 空状态 -->
      <el-empty 
        v-else-if="!noteStore.loading" 
        description="还没有笔记，快去发布第一条吧！"
      >
        <el-button type="primary" @click="$router.push('/publish')">
          发布笔记
        </el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useNoteStore } from '@/store/note'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import NoteCard from '@/components/NoteCard.vue'

const router = useRouter()
const noteStore = useNoteStore()

const searchKeyword = ref('')
const activeCategory = ref('全部')
const categories = ['全部', '旅行', '美食', '科技', '生活', '运动', '娱乐']

// 初始化加载
onMounted(() => {
  if (noteStore.homeNotes.length === 0) {
    noteStore.fetchHomeNotes()
  }
})

// 搜索
const handleSearch = () => {
  if (searchKeyword.value.trim()) {
    router.push({
      path: '/search',
      query: { keyword: searchKeyword.value, type: 'all' }
    })
  }
}

// 清空搜索
const clearSearch = () => {
  noteStore.clearNotes()
  noteStore.fetchHomeNotes()
}

// 分类切换
const handleCategoryChange = (category) => {
  activeCategory.value = category
  noteStore.clearNotes()
  noteStore.fetchHomeNotes()
}

// 加载更多
const loadMore = () => {
  noteStore.fetchHomeNotes(noteStore.lastNoteId)
}

// 点赞
const handleLike = async (noteId) => {
  try {
    await noteStore.likeNote(noteId)
    ElMessage.success('点赞成功')
  } catch (e) {
    // 错误已在拦截器处理
  }
}

// 收藏
const handleCollect = async (noteId) => {
  try {
    await noteStore.collectNote(noteId)
    ElMessage.success('收藏成功')
  } catch (e) {
    // 错误已在拦截器处理
  }
}
</script>

<style scoped>
.home-page {
  min-height: calc(100vh - 108px);
}

/* Hero区域 */
.hero-section {
  background: linear-gradient(135deg, #FF7D00 0%, #FF9500 100%);
  padding: 48px 20px;
  text-align: center;
  border-radius: 0 0 24px 24px;
  margin-bottom: 24px;
}

.hero-title {
  font-size: 36px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 12px;
  letter-spacing: 2px;
}

.hero-subtitle {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.85);
  margin: 0;
}

/* 搜索区域 */
.search-section {
  max-width: 720px;
  margin: -30px auto 24px;
  padding: 0 20px;
  position: relative;
  z-index: 10;
}

.search-bar {
  background: #fff;
  padding: 8px;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
}

.search-input {
  width: 100%;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 12px;
  box-shadow: none;
  border: 1px solid #e0e0e0;
}

.search-input :deep(.el-input__wrapper):hover {
  border-color: #FF7D00;
}

.search-input :deep(.el-input__wrapper.is-focus) {
  border-color: #FF7D00;
  box-shadow: 0 0 0 3px rgba(255, 125, 0, 0.1);
}

.search-input :deep(.el-input-group__append) {
  background: transparent;
  border: none;
  padding: 0 12px;
}

.search-btn {
  background: linear-gradient(135deg, #FF7D00 0%, #FF9500 100%);
  border: none;
  color: #fff;
  border-radius: 10px;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 分类区域 */
.category-section {
  max-width: 800px;
  margin: 0 auto 24px;
  padding: 0 20px;
}

.category-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: center;
}

.category-tag {
  padding: 8px 20px;
  border-radius: 20px;
  background: #fff;
  color: #666;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid #e0e0e0;
}

.category-tag:hover {
  border-color: #FF7D00;
  color: #FF7D00;
}

.category-tag.active {
  background: linear-gradient(135deg, #FF7D00 0%, #FF9500 100%);
  color: #fff;
  border-color: transparent;
}

/* 笔记列表 */
.notes-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  min-height: 400px;
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
  .hero-section {
    padding: 32px 20px;
  }
  
  .hero-title {
    font-size: 24px;
  }
  
  .hero-subtitle {
    font-size: 14px;
  }
  
  .search-section {
    margin-top: -20px;
  }
  
  .notes-grid {
    grid-template-columns: 1fr;
  }
}
</style>
