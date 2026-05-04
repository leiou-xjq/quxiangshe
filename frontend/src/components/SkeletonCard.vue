<template>
  <div class="skeleton-card">
    <div class="skeleton-header">
      <el-skeleton animated :loading="true" :rows="0">
        <template #template>
          <div class="skeleton-user">
            <el-skeleton-item variant="circle" style="width: 40px; height: 40px;" />
            <div class="skeleton-user-info">
              <el-skeleton-item variant="text" style="width: 80px; height: 16px;" />
              <el-skeleton-item variant="text" style="width: 50px; height: 12px; margin-top: 4px;" />
            </div>
          </div>
        </template>
      </el-skeleton>
    </div>

    <div class="skeleton-content">
      <el-skeleton animated :loading="true" :rows="2">
        <template #template>
          <el-skeleton-item variant="text" style="width: 100%; height: 14px; margin-bottom: 8px;" />
          <el-skeleton-item variant="text" style="width: 70%; height: 14px;" />
        </template>
      </el-skeleton>
    </div>

    <div class="skeleton-media" v-if="imageCount > 0">
      <el-skeleton animated :loading="true" :rows="1">
        <template #template>
          <div class="skeleton-image" :style="{ aspectRatio: imageAspect }"></div>
        </template>
      </el-skeleton>
    </div>

    <div class="skeleton-actions">
      <el-skeleton animated :loading="true" :rows="0">
        <template #template>
          <div class="skeleton-btns">
            <div class="skeleton-btn">
              <el-skeleton-item variant="circle" style="width: 18px; height: 18px;" />
              <el-skeleton-item variant="text" style="width: 30px; height: 14px; margin-left: 6px;" />
            </div>
            <div class="skeleton-btn">
              <el-skeleton-item variant="circle" style="width: 18px; height: 18px;" />
              <el-skeleton-item variant="text" style="width: 30px; height: 14px; margin-left: 6px;" />
            </div>
            <div class="skeleton-btn">
              <el-skeleton-item variant="circle" style="width: 18px; height: 18px;" />
              <el-skeleton-item variant="text" style="width: 30px; height: 14px; margin-left: 6px;" />
            </div>
          </div>
        </template>
      </el-skeleton>
    </div>
  </div>
</template>

<script setup>
defineProps({
  imageCount: {
    type: Number,
    default: 1
  },
  imageAspect: {
    type: String,
    default: '16/9'
  }
})
</script>

<style scoped>
.skeleton-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.skeleton-header {
  margin-bottom: 12px;
}

.skeleton-user {
  display: flex;
  align-items: center;
  gap: 10px;
}

.skeleton-user-info {
  flex: 1;
}

.skeleton-content {
  margin-bottom: 12px;
}

.skeleton-image {
  width: 100%;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: skeleton-loading 1.5s ease-in-out infinite;
  border-radius: 4px;
}

.skeleton-actions {
  margin-top: 12px;
}

.skeleton-btns {
  display: flex;
  gap: 24px;
}

.skeleton-btn {
  display: flex;
  align-items: center;
}

@keyframes skeleton-loading {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>