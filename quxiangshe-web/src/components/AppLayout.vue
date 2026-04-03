<template>
  <div class="app-layout">
    <!-- 顶部导航 -->
    <header class="app-header">
      <div class="header-container">
        <div class="header-left">
          <router-link to="/" class="logo">
            <el-icon :size="24" color="#FF7D00"><Promotion /></el-icon>
            <span class="logo-text">趣享社</span>
          </router-link>
          
          <div class="nav-menu">
            <div 
              class="nav-item" 
              :class="{ active: $route.path === '/' }"
              @click="$router.push('/')"
            >
              <el-icon><HomeFilled /></el-icon>
              <span>首页</span>
            </div>
            <div 
              class="nav-item"
              @click="goToSearch"
            >
              <el-icon><Search /></el-icon>
              <span>搜索</span>
            </div>
          </div>
        </div>
        
        <div class="header-right">
          <el-button type="primary" @click="handlePublish" class="publish-btn">
            <el-icon><EditPen /></el-icon>
            <span>发布笔记</span>
          </el-button>
          
          <!-- 已登录用户头像 -->
          <el-dropdown @command="handleUserCommand" v-if="userStore.isLoggedIn()">
            <div class="user-info">
              <el-avatar :size="32" :src="userStore.avatarUrl">
                {{ userStore.nickname?.charAt(0) || userStore.username?.charAt(0) || 'U' }}
              </el-avatar>
              <el-icon class="arrow-down"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>
                  我的主页
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          
          <!-- 未登录 -->
          <el-button v-else @click="$router.push('/login')" class="login-btn">
            登录
          </el-button>
        </div>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="app-main">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<script setup>
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Promotion, HomeFilled, Search, EditPen, User, SwitchButton, ArrowDown } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const goToSearch = () => {
  router.push('/search')
}

const handlePublish = () => {
  if (!userStore.isLoggedIn()) {
    ElMessageBox.confirm('登录后可以发布笔记', '提示', {
      confirmButtonText: '去登录',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      router.push('/login')
    })
    return
  }
  router.push('/publish')
}

const handleUserCommand = (command) => {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      userStore.logout()
      router.push('/login')
    })
  }
}
</script>

<style scoped>
.app-layout {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.app-header {
  position: sticky;
  top: 0;
  z-index: 1000;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.header-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 32px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  cursor: pointer;
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, #FF7D00 0%, #FF9500 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.nav-menu {
  display: flex;
  gap: 8px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 8px;
  cursor: pointer;
  color: #666;
  font-size: 14px;
  transition: all 0.2s;
}

.nav-item:hover {
  background: #f5f7fa;
  color: #FF7D00;
}

.nav-item.active {
  color: #FF7D00;
  background: rgba(255, 125, 0, 0.1);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.publish-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
  background: linear-gradient(135deg, #FF7D00 0%, #FF9500 100%);
  border: none;
}

.publish-btn:hover {
  box-shadow: 0 4px 12px rgba(255, 125, 0, 0.3);
}

.login-btn {
  background: linear-gradient(135deg, #FF7D00 0%, #FF9500 100%);
  border: none;
  color: #fff;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  padding: 4px 8px 4px 4px;
  border-radius: 24px;
  transition: background-color 0.3s;
}

.user-info:hover {
  background-color: #f5f7fa;
}

.arrow-down {
  color: #999;
  font-size: 12px;
}

.app-main {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
