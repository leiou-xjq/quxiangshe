<template>
  <div class="search-page">
    <!-- 搜索头部 -->
    <div class="search-header">
      <div class="search-box">
        <el-input
          v-model="keyword"
          placeholder="搜索笔记、博主..."
          prefix-icon="Search"
          clearable
          size="large"
          @keyup.enter="handleSearch"
          @clear="handleClear"
          class="search-input"
        >
          <template #append>
            <el-button :icon="Search" @click="handleSearch" class="search-btn" />
          </template>
        </el-input>
      </div>
    </div>

    <!-- 搜索类型切换 -->
    <div class="search-type-tabs">
      <el-radio-group v-model="searchType" @change="handleTypeChange">
        <el-radio-button label="note">搜索笔记</el-radio-button>
        <el-radio-button label="user">搜索博主</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 热门笔记（无搜索时） -->
    <div class="hot-notes-section" v-if="!hasSearched">
      <h3 class="section-title">
        <el-icon><TrendCharts /></el-icon>
        热门笔记
      </h3>
      <div class="notes-grid" v-loading="hotLoading">
        <NoteCard
          v-for="(note, index) in hotNotes"
          :key="note.noteId"
          :note="note"
          :style="{ animationDelay: `${index * 0.05}s` }"
          class="note-card-animate"
        />
      </div>
      <el-empty v-if="!hotLoading && hotNotes.length === 0" description="暂无热门笔记" />
    </div>

    <!-- 搜索结果 -->
    <div class="search-results" v-else>
      <div class="results-header">
        <span class="results-count" v-if="searchType === 'note'">
          找到 {{ (noteResults.notes || []).length }} 条笔记
        </span>
        <span class="results-count" v-else>
          找到 {{ (userResults.users || []).length }} 位博主
        </span>
      </div>

      <!-- 笔记搜索结果 -->
      <template v-if="searchType === 'note'">
        <div class="notes-grid" v-loading="searchLoading">
          <NoteCard
            v-for="(note, index) in noteResults.notes"
            :key="note.noteId"
            :note="note"
            :keyword="keyword"
            :style="{ animationDelay: `${index * 0.05}s` }"
            class="note-card-animate"
          />
        </div>
        <el-empty v-if="!searchLoading && (noteResults.notes || []).length === 0" :description="`未找到与「${keyword}」相关的笔记`" />
        
        <div class="load-more" v-if="noteResults.hasMore">
          <el-button :loading="searchLoading" @click="loadMoreNotes" link>加载更多</el-button>
        </div>
      </template>

      <!-- 用户搜索结果 -->
      <template v-else>
        <div class="users-list" v-loading="searchLoading">
          <div 
            v-for="(user, index) in userResults.users" 
            :key="user.userId"
            class="user-card"
            @click="goToUserProfile(user.userId)"
            :style="{ animationDelay: `${index * 0.05}s` }"
          >
            <el-avatar :size="64" :src="user.avatarUrl" class="user-avatar">
              {{ user.nickname?.charAt(0) || user.username?.charAt(0) || 'U' }}
            </el-avatar>
            <div class="user-info">
              <div class="user-name" v-html="user.highlightNickname || user.nickname || user.username"></div>
              <div class="user-bio" v-if="user.bio">{{ user.bio }}</div>
              <div class="user-meta">
                <span>@{{ user.username }}</span>
                <span>·</span>
                <span>加入于 {{ formatDate(user.createTime) }}</span>
              </div>
            </div>
            <el-button type="primary" size="small" class="follow-btn">关注</el-button>
          </div>
        </div>
        <el-empty v-if="!searchLoading && (userResults.users || []).length === 0" :description="`未找到与「${keyword}」相关的博主`" />

        <div class="load-more" v-if="userResults.hasMore">
          <el-button :loading="searchLoading" @click="loadMoreUsers" link>加载更多</el-button>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { noteApi } from '@/api/note'
import { ElMessage } from 'element-plus'
import { Search, TrendCharts } from '@element-plus/icons-vue'
import NoteCard from '@/components/NoteCard.vue'

const router = useRouter()
const route = useRoute()

const keyword = ref('')
const searchType = ref('note')
const hasSearched = ref(false)
const hotLoading = ref(false)
const searchLoading = ref(false)
const hotNotes = ref([])

const noteResults = reactive({
  notes: [],
  totalCount: 0,
  hasMore: false
})

const userResults = reactive({
  users: [],
  totalCount: 0,
  hasMore: false
})

const notePage = ref(1)
const userPage = ref(1)

onMounted(() => {
  loadHotNotes()
  
  if (route.query.keyword) {
    keyword.value = route.query.keyword
    handleSearch()
  }
})

watch(() => route.query.keyword, (newKeyword) => {
  if (newKeyword) {
    keyword.value = newKeyword
    handleSearch()
  }
})

const loadHotNotes = async () => {
  hotLoading.value = true
  try {
    const res = await noteApi.searchNotes('', '', 1, 20)
    const notes = res.data?.notes || []
    hotNotes.value = notes.sort((a, b) => {
      const scoreA = (a.likeCount || 0) + (a.collectCount || 0)
      const scoreB = (b.likeCount || 0) + (b.collectCount || 0)
      return scoreB - scoreA
    })
  } catch (e) {
    hotNotes.value = []
  } finally {
    hotLoading.value = false
  }
}

const handleSearch = () => {
  if (!keyword.value.trim()) {
    hasSearched.value = false
    return
  }
  
  hasSearched.value = true
  notePage.value = 1
  userPage.value = 1
  noteResults.notes = []
  userResults.users = []
  
  if (searchType.value === 'note') {
    searchNotes()
  } else {
    searchUsers()
  }
}

const handleClear = () => {
  hasSearched.value = false
  keyword.value = ''
}

const handleTypeChange = () => {
  if (hasSearched.value && keyword.value.trim()) {
    notePage.value = 1
    userPage.value = 1
    noteResults.notes = []
    userResults.users = []
    
    if (searchType.value === 'note') {
      searchNotes()
    } else {
      searchUsers()
    }
  }
}

const searchNotes = async (loadMore = false) => {
  if (!loadMore) {
    searchLoading.value = true
  }
  
  try {
    const res = await noteApi.searchNotes(keyword.value, '', notePage.value, 20)
    const newNotes = res.data?.notes || []
    
    if (loadMore) {
      noteResults.notes = [...noteResults.notes, ...newNotes]
    } else {
      noteResults.notes = newNotes
    }
    
    noteResults.totalCount = res.data?.totalCount || 0
    noteResults.hasMore = res.data?.hasMore || false
  } catch (e) {
  } finally {
    searchLoading.value = false
  }
}

const searchUsers = async (loadMore = false) => {
  if (!loadMore) {
    searchLoading.value = true
  }
  
  try {
    const res = await noteApi.searchUsers(keyword.value, userPage.value, 20)
    const newUsers = res.data?.users || []
    
    if (loadMore) {
      userResults.users = [...userResults.users, ...newUsers]
    } else {
      userResults.users = newUsers
    }
    
    userResults.totalCount = res.data?.totalCount || 0
    userResults.hasMore = res.data?.hasMore || false
  } catch (e) {
  } finally {
    searchLoading.value = false
  }
}

const loadMoreNotes = () => {
  notePage.value++
  searchNotes(true)
}

const loadMoreUsers = () => {
  userPage.value++
  searchUsers(true)
}

const goToUserProfile = (userId) => {
  router.push(`/user/${userId}`)
}

const formatDate = (date) => {
  if (!date) return ''
  return date.split('T')[0]
}
</script>

<style scoped>
.search-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 0 20px 40px;
}

.search-header {
  background: linear-gradient(135deg, #FF7D00 0%, #FF9500 100%);
  padding: 40px 20px;
  margin: -24px -20px 24px;
  border-radius: 0 0 24px 24px;
}

.search-box {
  max-width: 600px;
  margin: 0 auto;
}

.search-input {
  width: 100%;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 12px;
  box-shadow: none;
  border: none;
  padding: 4px 8px;
}

.search-input :deep(.el-input-group__append) {
  background: #fff;
  border: none;
  padding: 0;
}

.search-btn {
  background: linear-gradient(135deg, #FF7D00 0%, #FF9500 100%);
  border: none;
  color: #fff;
  border-radius: 0 12px 12px 0;
  width: 50px;
  height: 40px;
}

.search-type-tabs {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
}

.search-type-tabs :deep(.el-radio-button__inner) {
  padding: 10px 24px;
}

.search-type-tabs :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: linear-gradient(135deg, #FF7D00 0%, #FF9500 100%);
  border-color: #FF7D00;
  box-shadow: none;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin: 0 0 20px;
}

.notes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
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

.results-header {
  margin-bottom: 20px;
}

.results-count {
  font-size: 14px;
  color: #999;
}

.users-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.user-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.user-avatar {
  flex-shrink: 0;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}

.user-name :deep(em) {
  color: #FF7D00;
  font-style: normal;
  font-weight: bold;
}

.user-bio {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-meta {
  display: flex;
  gap: 8px;
  font-size: 12px;
  color: #999;
}

.follow-btn {
  background: linear-gradient(135deg, #FF7D00 0%, #FF9500 100%);
  border: none;
}

.load-more {
  text-align: center;
  padding: 30px 0;
}

@media (max-width: 768px) {
  .search-header {
    padding: 30px 16px;
    margin: -24px -16px 24px;
  }
  
  .notes-grid {
    grid-template-columns: 1fr;
  }
  
  .user-card {
    flex-direction: column;
    text-align: center;
  }
  
  .user-meta {
    justify-content: center;
  }
  
  .follow-btn {
    width: 100%;
  }
}
</style>
