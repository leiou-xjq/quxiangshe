<template>
  <div class="mobile-tab-bar">
    <div class="tab-bar-inner">
      <router-link 
        v-for="tab in tabs" 
        :key="tab.path"
        :to="tab.path"
        class="tab-item"
        :class="{ active: isActive(tab.path) }"
      >
        <el-icon :size="22">
          <component :is="tab.icon" />
        </el-icon>
        <span class="tab-label">{{ tab.label }}</span>
      </router-link>
    </div>
    <!-- 安全区域 -->
    <div class="safe-area-bottom"></div>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
import { HomeFilled, Search, Plus, Bell, User } from '@element-plus/icons-vue'

const route = useRoute()

const tabs = [
  { path: '/', label: '首页', icon: HomeFilled },
  { path: '/search', label: '搜索', icon: Search },
  { path: '/publish', label: '发布', icon: Plus },
  { path: '/notifications', label: '消息', icon: Bell },
  { path: '/profile', label: '我的', icon: User }
]

function isActive(path) {
  if (path === '/') {
    return route.path === '/'
  }
  return route.path.startsWith(path)
}
</script>

<style scoped>
.mobile-tab-bar {
  display: none;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: var(--color-white);
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.08);
  z-index: var(--z-fixed);
}

.tab-bar-inner {
  display: flex;
  justify-content: space-around;
  align-items: center;
  height: 50px;
  padding: 0 8px;
}

.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary);
  text-decoration: none;
  font-size: 10px;
  gap: 2px;
  transition: color var(--transition-fast);
  -webkit-tap-highlight-color: transparent;
}

.tab-item:active {
  background: transparent;
}

.tab-item.active {
  color: var(--color-primary);
}

.tab-label {
  font-size: 10px;
  margin-top: 2px;
}

/* 安全区域 - iPhone X系列 */
.safe-area-bottom {
  display: none;
  height: env(safe-area-inset-bottom, 0px);
  background: var(--color-white);
}

/* 移动端显示 */
@media (max-width: 768px) {
  .mobile-tab-bar {
    display: block;
  }
  
  .safe-area-bottom {
    display: block;
  }
  
  /* PC端隐藏 */
  .pc-only { display: none !important; }
}
</style>