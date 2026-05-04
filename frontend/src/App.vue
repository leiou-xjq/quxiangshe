<template>
  <router-view v-slot="{ Component }">
    <transition name="page-fade" mode="out-in">
      <component :is="Component" />
    </transition>
  </router-view>
</template>

<script setup>
// App根组件
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  height: 100%;
  width: 100%;
}

#app {
  min-height: 100%;
  width: 100%;
}

/* 响应式根容器 - 保证移动端全屏 */
.app-container {
  min-height: 100vh;
  width: 100%;
  overflow-x: hidden;
}

/* 移动端安全区域适配 */
@supports (padding-bottom: env(safe-area-inset-bottom)) {
  .mobile-safe-area {
    padding-bottom: env(safe-area-inset-bottom);
  }
}

/* 页面切换动画 - 小红书风格 */
.page-fade-enter-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.page-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-5px);
}

/* 横向滑动页面切换 */
.page-slide-enter-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.page-slide-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.page-slide-enter-from {
  opacity: 0;
  transform: translateX(30px);
}

.page-slide-leave-to {
  opacity: 0;
  transform: translateX(-30px);
}
</style>