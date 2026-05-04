<template>
  <div class="profile-page">
    <!-- 顶部导航栏 -->
    <div class="profile-nav">
      <el-button text @click="goHome">
        <el-icon><HomeFilled /></el-icon>
      </el-button>
      <span class="nav-title">个人主页</span>
      <div style="width: 40px;"></div>
    </div>
    
    <!-- 顶部背景 -->
    <div class="profile-header"></div>
    
    <!-- 用户信息卡片 -->
    <div class="profile-card">
      <div class="profile-user">
        <div class="profile-avatar-wrap">
          <el-avatar 
            :size="80" 
            :src="userStore.userInfo?.avatar || '/avatar/default.png'" 
          />
        </div>
        <div class="profile-info">
          <div class="profile-name">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</div>
          <div class="profile-username">@{{ userStore.userInfo?.username }}</div>
          <div class="profile-bio" v-if="userStore.userInfo?.bio">
            {{ userStore.userInfo.bio }}
          </div>
        </div>
        <div class="profile-actions" v-if="isCurrentUser">
          <el-button @click="goToEdit">编辑资料</el-button>
          <el-button @click="goToPassword">修改密码</el-button>
        </div>
        <div class="profile-actions" v-else>
          <el-button :type="isFollowing ? 'default' : 'primary'" @click="handleFollow">
            {{ isFollowing ? '已关注' : '关注' }}
          </el-button>
          <el-button :type="isBlocked ? 'default' : 'danger'" @click="handleBlock">
            {{ isBlocked ? '已拉黑' : '拉黑' }}
          </el-button>
        </div>
      </div>
      
      <div class="profile-stats">
        <div class="profile-stat-item" @click="goToFollowing">
          <div class="profile-stat-value">{{ followingCount }}</div>
          <div class="profile-stat-label">关注</div>
        </div>
        <div class="profile-stat-item">
          <div class="profile-stat-value">{{ fansCount }}</div>
          <div class="profile-stat-label">粉丝</div>
        </div>
        <div class="profile-stat-item">
          <div class="profile-stat-value">{{ likesCount }}</div>
          <div class="profile-stat-label">获赞</div>
        </div>
      </div>
    </div>
    
    <!-- Tab列表 -->
    <div class="profile-tabs-card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="笔记" name="notes">
          <div class="content-list" v-if="myNotes.length > 0">
            <div 
              v-for="note in myNotes" 
              :key="note.id" 
              class="content-item"
            >
              <div class="item-content" @click="goToNote(note.id)">
                <div class="item-title">{{ note.title }}</div>
                <div class="item-meta">
                  <span>{{ note.likeCount || 0 }} 赞</span>
                  <span>{{ note.commentCount || 0 }} 评论</span>
                  <span>{{ formatTime(note.createdAt) }}</span>
                </div>
              </div>
              <el-button 
                type="danger" 
                link 
                size="small"
                @click.stop="handleDeleteNote(note.id)"
              >
                删除
              </el-button>
            </div>
            <div class="load-more" v-if="hasMoreNotes" @click="loadMyNotes">
              <el-button link>加载更多</el-button>
            </div>
          </div>
          <div class="empty-tip" v-else>还没有发布过笔记</div>
        </el-tab-pane>
        
        <el-tab-pane label="收藏" name="favorites">
          <div class="content-list" v-if="myFavorites.length > 0">
            <div 
              v-for="note in myFavorites" 
              :key="note.id" 
              class="content-item"
              @click="goToNote(note.id)"
            >
              <div class="item-title">{{ note.title }}</div>
              <div class="item-meta">
                <span>{{ note.likeCount || 0 }} 赞</span>
                <span>{{ note.commentCount || 0 }} 评论</span>
                <span>{{ formatTime(note.createdAt) }}</span>
              </div>
            </div>
            <div class="load-more" v-if="hasMoreFavorites" @click="loadMyFavorites">
              <el-button link>加载更多</el-button>
            </div>
          </div>
          <div class="empty-tip" v-else>还没有收藏过笔记</div>
        </el-tab-pane>
        
        <el-tab-pane label="粉丝" name="fans">
          <div class="content-list" v-if="myFans.length > 0">
            <div 
              v-for="user in myFans" 
              :key="user.id" 
              class="user-item"
            >
              <el-avatar :size="50" :src="user.avatar || '/avatar/default.png'" />
              <div class="user-info">
                <div class="user-name">{{ user.nickname || user.username }}</div>
                <div class="user-bio" v-if="user.bio">{{ user.bio }}</div>
              </div>
              <el-button 
                v-if="user.id !== userStore.userInfo?.id"
                :type="user.isFollowing ? 'default' : 'primary'" 
                size="small"
                @click="toggleFollow(user)"
              >
                {{ user.isFollowing ? '已关注' : '关注' }}
              </el-button>
            </div>
            <div class="load-more" v-if="hasMoreFans" @click="loadMyFans">
              <el-button link>加载更多</el-button>
            </div>
          </div>
          <div class="empty-tip" v-else>还没有粉丝</div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { HomeFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getFollowingCount, getFollowersCount, getFollowersList, followUser, unfollowUser, getFollowStatus } from '@/api/follow'
import { getMyNotes, getMyFavorites, getMyLikesCount, getUserLikesCount, deleteNote } from '@/api/note'
import { checkBlocked, blockUser, unblockUser } from '@/api/blacklist'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const userId = ref(route.query.id || userStore.userInfo?.id)
const isCurrentUser = computed(() => userId.value === userStore.userInfo?.id)

const followingCount = ref(0)
const fansCount = ref(0)
const likesCount = ref(0)
const isFollowing = ref(false)
const isBlocked = ref(false)

const activeTab = ref('notes')
const myNotes = ref([])
const myFavorites = ref([])
const myFans = ref([])
const notesPage = ref(1)
const favoritesPage = ref(1)
const fansCursor = ref(null)
const hasMoreNotes = ref(true)
const hasMoreFavorites = ref(true)
const hasMoreFans = ref(true)
const loadingNotes = ref(false)
const loadingFavorites = ref(false)
const loadingFans = ref(false)

// 跳转
function goHome() {
  router.push('/')
}

function goToEdit() {
  router.push('/profile/edit')
}

function goToPassword() {
  router.push('/profile/password')
}

function goToNote(noteId) {
  router.push(`/note/${noteId}`)
}

function goToFollowing() {
  router.push(`/follow-list/following?userId=${userId.value}`)
}

function goToFans() {
  router.push(`/follow-list/fans?userId=${userId.value}`)
}

// 格式化时间
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

// 加载统计数据
async function loadStats() {
  try {
    // 确保有 userId
    if (!userId.value) {
      if (userStore.userInfo) {
        userId.value = userStore.userInfo.id
      }
    }
    if (!userId.value) return
    
    const isCurrentUser = userId.value === userStore.userInfo?.id
    
    const [followingRes, fansRes, likesRes] = await Promise.all([
      getFollowingCount(userId.value),
      getFollowersCount(userId.value),
      isCurrentUser ? getMyLikesCount() : getUserLikesCount(userId.value)
    ])
    console.log('loadStats结果:', followingRes.data, fansRes.data)
    followingCount.value = followingRes.data || 0
    fansCount.value = fansRes.data || 0
    likesCount.value = likesRes.data || 0
  } catch (e) {
    console.error('加载统计数据失败:', e)
  }
}

// 加载我的笔记
async function loadMyNotes() {
  if (loadingNotes.value) return
  loadingNotes.value = true
  try {
    const res = await getMyNotes(notesPage.value, 10)
    const newNotes = res.data || []
    if (notesPage.value === 1) {
      myNotes.value = newNotes
    } else {
      myNotes.value.push(...newNotes)
    }
    hasMoreNotes.value = newNotes.length === 10
    if (hasMoreNotes.value) {
      notesPage.value++
    }
  } catch (e) {
    console.error('加载笔记失败:', e)
  } finally {
    loadingNotes.value = false
  }
}

// 删除笔记
async function handleDeleteNote(noteId) {
  try {
    await ElMessageBox.confirm('确定要删除这条笔记吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await deleteNote(noteId)
    ElMessage.success('删除成功')
    myNotes.value = myNotes.value.filter(n => n.id !== noteId)
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '删除失败')
    }
  }
}

// 加载我的收藏
async function loadMyFavorites() {
  if (loadingFavorites.value) return
  loadingFavorites.value = true
  try {
    const res = await getMyFavorites(favoritesPage.value, 10)
    const newFavorites = res.data || []
    if (favoritesPage.value === 1) {
      myFavorites.value = newFavorites
    } else {
      myFavorites.value.push(...newFavorites)
    }
    hasMoreFavorites.value = newFavorites.length === 10
    if (hasMoreFavorites.value) {
      favoritesPage.value++
    }
  } catch (e) {
    console.error('加载收藏失败:', e)
  } finally {
    loadingFavorites.value = false
  }
}

// 加载我的粉丝
async function loadMyFans() {
  if (loadingFans.value) return
  loadingFans.value = true
  try {
    console.log('loadMyFans - userId:', userId.value, 'cursor:', fansCursor.value)
    const res = await getFollowersList(fansCursor.value, 20, userId.value)
    console.log('loadMyFans - response:', res)
    const newFans = res.data?.records || []
    console.log('loadMyFans - newFans:', newFans)
    if (!fansCursor.value) {
      myFans.value = newFans
    } else {
      myFans.value.push(...newFans)
    }
    hasMoreFans.value = res.data?.hasMore || false
    fansCursor.value = res.data?.nextCursor || null
    console.log('loadMyFans - hasMore:', hasMoreFans.value, 'nextCursor:', fansCursor.value)
  } catch (e) {
    console.error('加载粉丝失败:', e)
  } finally {
    loadingFans.value = false
  }
}

// 切换关注
async function toggleFollow(user) {
  console.log('toggleFollow - userId:', userId.value, 'target user:', user.id)
  try {
    if (user.isFollowing) {
      await unfollowUser(user.id)
      user.isFollowing = false
      ElMessage.success('已取消关注')
    } else {
      await followUser(user.id)
      user.isFollowing = true
      ElMessage.success('关注成功')
    }
    await loadStats()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

// 关注/取消关注当前页面用户
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
    await loadStats()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

// 拉黑/取消拉黑
async function handleBlock() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  try {
    if (isBlocked.value) {
      await unblockUser(userId.value)
      isBlocked.value = false
      ElMessage.success('已取消拉黑')
    } else {
      await blockUser(userId.value)
      isBlocked.value = true
      ElMessage.success('拉黑成功')
    }
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

// Tab切换
function handleTabChange(tabName) {
  if (tabName === 'notes' && myNotes.value.length === 0) {
    loadMyNotes()
  } else if (tabName === 'favorites' && myFavorites.value.length === 0) {
    loadMyFavorites()
  } else if (tabName === 'fans') {
    // 任何用户的粉丝Tab都需要加载（自己的或其他用户的）
    fansCursor.value = null
    myFans.value = []
    hasMoreFans.value = true
    loadMyFans()
    // 只有查看其他用户的粉丝Tab才需要刷新关注状态
    if (!isCurrentUser.value) {
      loadFollowStatus()
    }
  }
}

// 加载关注状态
async function loadFollowStatus() {
  try {
    console.log('loadFollowStatus - userId:', userId.value)
    const res = await getFollowStatus(userId.value)
    console.log('loadFollowStatus - response:', res)
    isFollowing.value = res.data || false
    console.log('loadFollowStatus - isFollowing:', isFollowing.value)
  } catch (e) {
    console.error('加载关注状态失败:', e)
  }
}

onMounted(async () => {
  userId.value = route.query.id || userStore.userInfo?.id
  await loadData()
})

async function loadData() {
  console.log('loadData - 开始, userId:', userId.value, 'userInfo:', !!userStore.userInfo)
  if (!userStore.userInfo) {
    await userStore.fetchUserInfo()
  }
  if (!userId.value) {
    userId.value = userStore.userInfo?.id
  }
  console.log('loadData - 之后, userId:', userId.value)
  if (!userId.value) return
  await loadStats()
  await loadMyNotes()
  
  // 如果不是当前用户，加载关注状态和拉黑状态
  if (!isCurrentUser.value) {
    await loadFollowStatus()
    await loadBlockStatus()
  }
}

// 加载拉黑状态
async function loadBlockStatus() {
  try {
    const res = await checkBlocked(userId.value)
    isBlocked.value = res.data || false
  } catch (e) {
    console.error('加载拉黑状态失败:', e)
  }
}

watch(() => route.query.id, async () => {
  userId.value = route.query.id || userStore.userInfo?.id
  if (userId.value) {
    await loadData()
  }
})

watch(() => userStore.userInfo, async () => {
  if (userStore.userInfo && !userId.value) {
    userId.value = userStore.userInfo.id
    await loadData()
  }
})
</script>

<style scoped>
@import '@/styles/profile.css';
</style>