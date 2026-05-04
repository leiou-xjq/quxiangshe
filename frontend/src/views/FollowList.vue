<template>
  <div class="follow-list-page">
    <div class="follow-list-header">
      <el-button circle @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
      </el-button>
      <h2>{{ type === 'following' ? '关注' : '粉丝' }}</h2>
    </div>
    
    <div class="follow-list" v-loading="loading">
      <div v-if="list.length === 0 && !loading" class="empty">
        <el-empty :description="type === 'following' ? '还没有关注任何人' : '还没有粉丝'" />
      </div>
      
      <div 
        v-for="user in list" 
        :key="user.id" 
        class="follow-item"
        @click="goToProfile(user.id)"
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
          @click.stop="toggleFollow(user)"
        >
          {{ user.isFollowing ? '已关注' : '关注' }}
        </el-button>
      </div>
      
      <div class="load-more" v-if="hasMore" @click="loadMore">
        <el-button link>加载更多</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getFollowingList, getFollowersList, followUser, unfollowUser } from '@/api/follow'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const type = computed(() => route.params.type || 'following')
const userId = computed(() => route.query.userId || userStore.userInfo?.id)

const loading = ref(false)
const list = ref([])
const cursor = ref(null)
const hasMore = ref(true)
const size = 20

function goBack() {
  router.back()
}

function goToProfile(id) {
  router.push(`/user/${id}`)
}

async function fetchList() {
  loading.value = true
  try {
    let res
    if (type.value === 'following') {
      res = await getFollowingList(userId.value, cursor.value, size)
    } else {
      res = await getFollowersList(cursor.value, size, userId.value)
    }
    const newList = res.data?.records || []
    if (!cursor.value) {
      list.value = newList
    } else {
      list.value.push(...newList)
    }
    hasMore.value = res.data?.hasMore || false
    cursor.value = res.data?.nextCursor || null
  } catch (error) {
    console.error('加载列表失败:', error)
  } finally {
    loading.value = false
  }
}

async function toggleFollow(user) {
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
  } catch (error) {
    console.error('操作失败:', error)
  }
}

function loadMore() {
  fetchList()
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.follow-list-page {
  padding: 20px;
  max-width: 600px;
  margin: 0 auto;
}

.follow-list-header {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 20px;
}

.follow-list-header h2 {
  margin: 0;
}

.follow-list {
  min-height: 400px;
}

.follow-item {
  display: flex;
  align-items: center;
  padding: 15px;
  border-bottom: 1px solid #eee;
  cursor: pointer;
}

.follow-item:hover {
  background: #f5f5f5;
}

.user-info {
  flex: 1;
  margin-left: 15px;
}

.user-name {
  font-weight: 500;
  margin-bottom: 5px;
}

.user-bio {
  font-size: 12px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 200px;
}

.empty {
  padding: 50px 0;
}

.load-more {
  text-align: center;
  padding: 20px;
}
</style>
