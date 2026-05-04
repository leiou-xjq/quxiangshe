<template>
  <div class="forgot-password-page">
    <div class="forgot-card">
      <div class="forgot-header">
        <h1>找回密码</h1>
        <p>通过手机验证码重置密码</p>
      </div>
      
      <el-form ref="formRef" :model="form" :rules="rules" class="forgot-form">
        <el-form-item prop="phone" class="form-item">
          <el-input 
            v-model="form.phone" 
            placeholder="请输入手机号"
            size="large"
            prefix-icon="Phone"
          />
        </el-form-item>
        
        <el-form-item prop="code" class="form-item">
          <el-input 
            v-model="form.code" 
            placeholder="请输入验证码"
            size="large"
            prefix-icon="Message"
            maxlength="6"
          >
            <template #append>
              <el-button 
                @click="handleSendCode" 
                :disabled="smsCountdown > 0 || smsLoading"
                style="min-width: 80px;"
              >
                {{ smsCountdown > 0 ? `${smsCountdown}s` : '获取验证码' }}
              </el-button>
            </template>
          </el-input>
        </el-form-item>
        
        <el-form-item prop="password" class="form-item">
          <el-input 
            v-model="form.password" 
            type="password" 
            placeholder="请输入新密码（6-20位，包含大小写字母和数字）"
            size="large"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        
        <el-form-item class="form-item btn-item">
          <el-button 
            type="primary" 
            size="large" 
            class="submit-btn"
            :loading="loading"
            @click="handleSubmit"
          >
            重置密码
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="forgot-footer">
        <span>想起密码了？</span>
        <router-link to="/login">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { sendResetCode, resetPassword } from '@/api/notification'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const smsLoading = ref(false)
const smsCountdown = ref(0)

const form = ref({
  phone: '',
  code: '',
  password: ''
})

const validatePassword = (rule, value, callback) => {
  const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d@$!%*?&]{6,20}$/
  if (!value) {
    callback(new Error('请输入新密码'))
  } else if (!passwordRegex.test(value)) {
    callback(new Error('密码必须包含大小写字母和数字'))
  } else {
    callback()
  }
}

const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '验证码必须是6位数字', trigger: 'blur' }
  ],
  password: [
    { required: true, validator: validatePassword, trigger: 'blur' }
  ]
}

async function handleSendCode() {
  if (smsCountdown.value > 0) return
  
  if (!form.value.phone) {
    ElMessage.warning('请先输入手机号')
    return
  }
  
  if (!/^1[3-9]\d{9}$/.test(form.value.phone)) {
    ElMessage.warning('手机号格式不正确')
    return
  }
  
  smsLoading.value = true
  try {
    await sendResetCode(form.value.phone)
    ElMessage.success('验证码已发送')
    smsCountdown.value = 60
    const timer = setInterval(() => {
      smsCountdown.value--
      if (smsCountdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (error) {
    console.error('发送验证码失败:', error)
  } finally {
    smsLoading.value = false
  }
}

async function handleSubmit() {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  
  loading.value = true
  
  try {
    await resetPassword({
      phone: form.value.phone,
      code: form.value.code,
      password: form.value.password
    })
    ElMessage.success('密码重置成功，请登录')
    router.push('/login')
  } catch (error) {
    ElMessage.error(error.message || '重置失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.forgot-password-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.forgot-card {
  width: 400px;
  padding: 40px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.forgot-header {
  text-align: center;
  margin-bottom: 30px;
}

.forgot-header h1 {
  margin: 0 0 10px;
  font-size: 28px;
  color: #333;
}

.forgot-header p {
  margin: 0;
  color: #999;
}

.forgot-form .form-item {
  margin-bottom: 20px;
}

.btn-item {
  margin-top: 30px;
}

.submit-btn {
  width: 100%;
}

.forgot-footer {
  text-align: center;
  margin-top: 20px;
  color: #999;
}

.forgot-footer a {
  color: #409eff;
  text-decoration: none;
  margin-left: 5px;
}
</style>
