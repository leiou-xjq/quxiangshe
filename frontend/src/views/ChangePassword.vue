<template>
  <div class="change-password-page">
    <div class="page-header">
      <div class="header-left">
        <el-button text @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <el-button text @click="goHome">
          <el-icon><HomeFilled /></el-icon>
        </el-button>
      </div>
      <span class="page-title">修改密码</span>
      <div style="width: 60px;"></div>
    </div>
    
    <div class="form-card">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input 
            v-model="form.oldPassword" 
            type="password" 
            show-password 
            placeholder="请输入原密码" 
          />
        </el-form-item>
        
        <el-form-item label="新密码" prop="newPassword">
          <el-input 
            v-model="form.newPassword" 
            type="password" 
            show-password 
            placeholder="请输入新密码" 
          />
        </el-form-item>
        
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input 
            v-model="form.confirmPassword" 
            type="password" 
            show-password 
            placeholder="请再次输入新密码" 
          />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="loading" style="width: 100%">
            修改密码
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, HomeFilled } from '@element-plus/icons-vue'
import { changePassword as changePasswordApi } from '@/api/auth'

const router = useRouter()

const formRef = ref(null)
const loading = ref(false)

const form = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const rules = {
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.value.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

function goBack() {
  router.back()
}

function goHome() {
  router.push('/')
}

async function handleSubmit() {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    loading.value = true
    try {
      await changePasswordApi(form.value)
      ElMessage.success('密码修改成功')
      router.back()
    } catch (e) {
      console.error('密码修改失败:', e)
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.change-password-page {
  min-height: 100vh;
  background: var(--bg-page);
  padding: 16px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header .el-button {
  display: flex;
  align-items: center;
  gap: 4px;
}

.page-title {
  font-size: var(--font-size-md);
  font-weight: 600;
  color: var(--text-primary);
}

.form-card {
  background: var(--color-white);
  border-radius: var(--radius-md);
  padding: 20px;
}

.form-card :deep(.el-form-item) {
  margin-bottom: 24px;
}

.form-card :deep(.el-form-item__label) {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  padding-bottom: 8px;
}

.form-card :deep(.el-input__wrapper) {
  background: var(--bg-input);
  border: none;
  border-radius: var(--radius-sm);
  box-shadow: none;
  padding: 10px 14px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 4px;
}

.header-left .el-button {
  display: flex;
  align-items: center;
  font-size: 18px;
}
</style>