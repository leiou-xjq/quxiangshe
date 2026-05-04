<template>
  <div class="edit-profile-page">
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
      <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
    </div>
    
    <div class="form-card">
      <el-form ref="formRef" :model="form" label-position="top">
        <!-- 头像 -->
        <div class="avatar-section">
          <el-avatar 
            :size="80" 
            :src="form.avatar || '/avatar/default.png'"
            @click="triggerAvatarUpload"
            class="avatar-clickable"
          />
          <input 
            ref="avatarInput"
            type="file" 
            accept="image/*" 
            style="display: none"
            @change="handleAvatarChange"
          />
          <el-button size="small" round @click="triggerAvatarUpload" :loading="uploadingAvatar">
            更换头像
          </el-button>
        </div>
        
        <el-form-item label="用户名">
          <el-input v-model="form.username" disabled />
        </el-form-item>
        
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="请输入昵称" maxlength="50" />
        </el-form-item>
        
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio :value="0">保密</el-radio>
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="生日">
          <el-date-picker 
            v-model="form.birthday" 
            type="date" 
            placeholder="选择日期"
            style="width: 100%"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        
        <el-form-item label="个人简介">
          <el-input 
            v-model="form.bio" 
            type="textarea" 
            :rows="4" 
            placeholder="介绍一下自己吧~"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, HomeFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { updateUser } from '@/api/auth'
import { uploadImage } from '@/api/note'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const saving = ref(false)
const uploadingAvatar = ref(false)
const avatarInput = ref(null)

const form = ref({
  username: '',
  nickname: '',
  phone: '',
  email: '',
  avatar: '',
  gender: 0,
  birthday: '',
  bio: ''
})

function goBack() {
  router.back()
}

function goHome() {
  router.push('/')
}

function triggerAvatarUpload() {
  avatarInput.value?.click()
}

async function handleAvatarChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过5MB')
    return
  }
  
  uploadingAvatar.value = true
  try {
    const res = await uploadImage(file)
    form.value.avatar = res.data
  } catch (e) {
    ElMessage.error('头像上传失败')
  } finally {
    uploadingAvatar.value = false
    e.target.value = ''
  }
}

async function handleSave() {
  saving.value = true
  try {
    await updateUser(form.value)
    ElMessage.success('保存成功')
    await userStore.fetchUserInfo()
    router.back()
  } catch (error) {
    console.error('保存失败:', error)
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  if (userStore.userInfo) {
    form.value = {
      username: userStore.userInfo.username || '',
      nickname: userStore.userInfo.nickname || '',
      phone: userStore.userInfo.phone || '',
      email: userStore.userInfo.email || '',
      avatar: userStore.userInfo.avatar || '/avatar/default.png',
      gender: userStore.userInfo.gender || 0,
      birthday: userStore.userInfo.birthday || '',
      bio: userStore.userInfo.bio || ''
    }
  }
})
</script>

<style scoped>
.edit-profile-page {
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

.form-card {
  background: var(--color-white);
  border-radius: var(--radius-md);
  padding: 20px;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.avatar-clickable {
  cursor: pointer;
  transition: opacity var(--transition-fast);
}

.avatar-clickable:hover {
  opacity: 0.8;
}

.form-card :deep(.el-form-item) {
  margin-bottom: 20px;
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

.form-card :deep(.el-textarea__inner) {
  background: var(--bg-input);
  border: none;
  border-radius: var(--radius-sm);
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