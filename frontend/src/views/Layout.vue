<template>
  <div class="layout">
    <!-- PC端侧边栏 -->
    <div class="sidebar pc-only">
      <div class="logo">
          <span class="logo-icon">🏠</span>
          <span class="logo-text">理享</span>
        </div>
      <el-menu
        :default-active="route.path"
        router
        class="menu"
      >
        <el-menu-item index="/">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </el-menu-item>
        <el-menu-item index="/profile">
          <el-icon><User /></el-icon>
          <span>个人中心</span>
        </el-menu-item>
      </el-menu>
    </div>
    
    <div class="main">
      <div class="header">
        <div class="header-left">
          <div class="search-box">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索笔记/用户..."
              class="search-input"
              @keyup.enter="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </div>
        </div>
        
        <div class="header-right">
          <ThemeToggle />
          
          <el-button type="primary" class="publish-btn" @click="goToPublish">
            <el-icon><EditPen /></el-icon>
            <span>发布笔记</span>
          </el-button>
          
          <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="notification-badge">
            <el-button circle @click="goToNotifications">
              <el-icon><Bell /></el-icon>
            </el-button>
          </el-badge>
          
          <el-badge :value="messageUnreadCount" :hidden="messageUnreadCount === 0" class="notification-badge">
            <el-button circle @click="goToMessages">
              <el-icon><Message /></el-icon>
            </el-button>
          </el-badge>
          
          <div class="user-info">
            <el-dropdown @command="handleCommand">
              <div class="user-dropdown">
                <el-avatar :src="userStore.userInfo?.avatar || '/avatar/default.png'" />
                <span class="username">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
                <el-icon><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </div>
      
      <div class="content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
      
      <!-- 移动端底部Tab -->
      <MobileTab />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { HomeFilled, Bell, Plus, User, Search, EditPen, ArrowDown, Message } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getUnreadCount } from '@/api/notification'
import { getUnreadCount as getMessageUnreadCount } from '@/api/message'
import MobileTab from '@/components/MobileTab.vue'
import ThemeToggle from '@/components/ThemeToggle.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const searchKeyword = ref('')
const unreadCount = ref(0)
const messageUnreadCount = ref(0)

function handleCommand(command) {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  } else if (command === 'profile') {
    router.push('/profile')
  }
}

function goToPublish() {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  router.push('/publish')
}

function handleSearch() {
  if (!searchKeyword.value.trim()) {
    return
  }
  router.push({ path: '/search', query: { keyword: searchKeyword.value } })
}

async function fetchUnreadCount() {
  try {
    const res = await getUnreadCount()
    unreadCount.value = res.data || 0
  } catch (e) {
    console.error('获取未读数失败', e)
  }
}

async function fetchMessageUnreadCount() {
  try {
    const res = await getMessageUnreadCount()
    messageUnreadCount.value = res.data || 0
  } catch (e) {
    console.error('获取私信未读数失败', e)
  }
}

function goToNotifications() {
  router.push('/notifications')
}

function goToMessages() {
  router.push('/messages')
}

onMounted(() => {
  if (userStore.isLoggedIn && !userStore.userInfo) {
    userStore.fetchUserInfo()
  }
  fetchUnreadCount()
  fetchMessageUnreadCount()
})
</script>

<style scoped>
@import '@/styles/layout.css';

.publish-btn {
  margin-right: 16px;
  display: flex;
  align-items: center;
  gap: 6px;
}
</style>
