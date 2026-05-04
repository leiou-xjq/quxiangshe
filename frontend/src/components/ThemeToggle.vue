<template>
  <div class="theme-toggle" @click="toggleTheme" :title="isDark ? '切换到亮色模式' : '切换到深色模式'">
    <div class="toggle-track">
      <div class="toggle-thumb" :class="{ dark: isDark }">
        <svg v-if="!isDark" class="icon sun" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="5"/>
          <line x1="12" y1="1" x2="12" y2="3"/>
          <line x1="12" y1="21" x2="12" y2="23"/>
          <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/>
          <line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/>
          <line x1="1" y1="12" x2="3" y2="12"/>
          <line x1="21" y1="12" x2="23" y2="12"/>
          <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/>
          <line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
        </svg>
        <svg v-else class="icon moon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
        </svg>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const isDark = ref(false)

const toggleTheme = () => {
  isDark.value = !isDark.value
  if (isDark.value) {
    document.documentElement.classList.add('dark')
  } else {
    document.documentElement.classList.remove('dark')
  }
  localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
}

onMounted(() => {
  const savedTheme = localStorage.getItem('theme')
  if (savedTheme === 'dark') {
    isDark.value = true
    document.documentElement.classList.add('dark')
  } else if (!savedTheme) {
    // 默认跟随系统
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
    if (prefersDark) {
      isDark.value = true
      document.documentElement.classList.add('dark')
    }
  }
})
</script>

<style scoped>
.theme-toggle {
  cursor: pointer;
  display: inline-flex;
}

.toggle-track {
  width: 52px;
  height: 28px;
  background: var(--color-bg-tertiary);
  border-radius: var(--radius-full);
  padding: 3px;
  transition: background var(--transition-normal);
}

.toggle-track:hover {
  background: var(--color-border);
}

.toggle-thumb {
  width: 22px;
  height: 22px;
  background: var(--color-primary);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform var(--transition-normal), background var(--transition-normal);
  box-shadow: var(--shadow-sm);
}

.toggle-thumb.dark {
  transform: translateX(24px);
  background: #1A1A1A;
}

.icon {
  width: 14px;
  height: 14px;
  color: #FFFFFF;
}

.sun {
  animation: spin-sun 0.3s ease;
}

.moon {
  animation: spin-moon 0.3s ease;
}

@keyframes spin-sun {
  from { transform: rotate(-90deg); opacity: 0; }
  to { transform: rotate(0deg); opacity: 1; }
}

@keyframes spin-moon {
  from { transform: rotate(90deg); opacity: 0; }
  to { transform: rotate(0deg); opacity: 1; }
}
</style>