<script setup>
import { ref, computed, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { sendCode, emailLogin, wechatLogin } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)
const smsLoading = ref(false)
const smsCountdown = ref(0)

const loginMode = ref('password')
const form = ref({
  loginType: 'username',
  loginValue: '',
  password: '',
  email: '',
  code: ''
})

const placeholderText = computed(() => {
  const map = {
    username: '请输入用户名',
    phone: '请输入手机号',
    email: '请输入邮箱'
  }
  return map[form.value.loginType]
})

const validatePassword = (rule, value, callback) => {
  const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d@$!%*?&]{6,20}$/
  if (!value) {
    callback(new Error('请输入密码'))
  } else if (!passwordRegex.test(value)) {
    callback(new Error('密码必须包含大小写字母和数字'))
  } else {
    callback()
  }
}

const rules = computed(() => {
  const baseRules = {
    loginValue: [
      { required: true, message: '请输入登录账号', trigger: 'blur' }
    ],
    password: [
      { required: true, validator: validatePassword, trigger: 'blur' }
    ]
  }
  
  const phoneRules = {
    email: [
      { required: true, message: '请输入邮箱', trigger: 'blur' },
      { pattern: /^[a-zA-Z0-9_+&*-]+(?:\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,7}$/, message: '邮箱格式不正确', trigger: 'blur' }
    ],
    code: [
      { required: true, message: '请输入验证码', trigger: 'blur' },
      { pattern: /^\d{6}$/, message: '验证码必须是6位数字', trigger: 'blur' }
    ]
  }
  
  return loginMode.value === 'sms' ? phoneRules : baseRules
})

async function handleSendCode() {
  if (smsCountdown.value > 0) return

  if (!form.value.email) {
    ElMessage.warning('请先输入邮箱')
    return
  }

  if (!/^[a-zA-Z0-9_+&*-]+(?:\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,7}$/.test(form.value.email)) {
    ElMessage.warning('邮箱格式不正确')
    return
  }

  smsLoading.value = true
  try {
    await sendCode(form.value.email)
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

async function handleLogin() {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }

  loading.value = true

  try {
    if (loginMode.value === 'sms') {
      const res = await emailLogin({
        email: form.value.email,
        code: form.value.code
      })
      userStore.setTokens(res.data)
      ElMessage.success('登录成功')
    } else {
      await userStore.login(form.value)
      ElMessage.success('登录成功')
    }

    const redirect = route.query.redirect || '/'
    await nextTick()
    router.push(redirect)
  } catch (error) {
    ElMessage.error(error.message || '登录失败')
  } finally {
    loading.value = false
  }
}

async function handleWechatLogin() {
  ElMessage.info('微信登录功能开发中，请使用其他方式登录')
}
</script>

<template>
  <div class="login-page">
    <div class="login-decoration">
      <div class="decoration-circle circle-1"></div>
      <div class="decoration-circle circle-2"></div>
      <div class="decoration-circle circle-3"></div>
      <div class="decoration-circle circle-4"></div>
      <div class="decoration-circle circle-5"></div>
      <div class="floating-emoji emoji-1">🌸</div>
      <div class="floating-emoji emoji-2">✨</div>
      <div class="floating-emoji emoji-3">🌈</div>
      <div class="floating-emoji emoji-4">💫</div>
    </div>

    <div class="login-card">
      <div class="login-header">
        <h1 class="logo">
          <span class="logo-icon">🏠</span>
          <span class="logo-text">理享</span>
        </h1>
        <p class="subtitle">🌟 欢迎来到理享，发现美好，分享生活 🌟</p>
      </div>
      
      <el-form ref="formRef" :model="form" :rules="rules" class="login-form">
        <el-form-item prop="loginMode">
          <el-radio-group v-model="loginMode" class="login-mode-group">
            <el-radio-button value="password">密码登录</el-radio-button>
            <el-radio-button value="sms">验证码登录</el-radio-button>
          </el-radio-group>
        </el-form-item>
        
        <template v-if="loginMode === 'password'">
          <el-form-item prop="loginType">
            <el-radio-group v-model="form.loginType" class="login-type-group">
              <el-radio-button value="username">用户名</el-radio-button>
            </el-radio-group>
          </el-form-item>
          
          <el-form-item prop="loginValue" class="form-item">
            <el-input 
              v-model="form.loginValue" 
              :placeholder="placeholderText"
              size="large"
              prefix-icon="User"
            />
          </el-form-item>
          
          <el-form-item prop="password" class="form-item">
            <el-input 
              v-model="form.password" 
              type="password" 
              placeholder="请输入密码"
              size="large"
              prefix-icon="Lock"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>
        </template>
        
        <template v-else>
          <el-form-item prop="email" class="form-item">
            <el-input
              v-model="form.email"
              placeholder="请输入邮箱"
              size="large"
              prefix-icon="Message"
            />
          </el-form-item>
          
          <el-form-item prop="code" class="form-item">
            <el-input 
              v-model="form.code" 
              placeholder="请输入验证码"
              size="large"
              prefix-icon="Message"
              maxlength="6"
              @keyup.enter="handleLogin"
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
        </template>
        
        <el-form-item class="form-item btn-item">
          <el-button 
            type="primary" 
            size="large" 
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="login-footer">
        <span>还没有账号？</span>
        <router-link to="/register">立即注册</router-link>
      </div>
      
      <div class="login-extra">
        <router-link to="/forgot-password">忘记密码？</router-link>
      </div>
      
      <div class="third-party-login">
        <div class="divider">
          <span>其他登录方式</span>
        </div>
        <div class="third-party-icons">
          <el-button circle class="wechat-btn" @click="handleWechatLogin">
            <svg viewBox="0 0 24 24" width="20" height="20">
              <path fill="#07C160" d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 0 1 .213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.29.295a.326.326 0 0 0 .167-.054l1.903-1.114a.864.864 0 0 1 .717-.098 10.16 10.16 0 0 0 2.837.403c.276 0 .543-.027.811-.05-.857-2.578.157-4.972 1.932-6.446 1.703-1.415 3.882-1.98 5.853-1.838-.576-3.583-4.196-6.348-8.596-6.348zM5.785 5.991c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178A1.17 1.17 0 0 1 4.623 7.17c0-.651.52-1.18 1.162-1.18zm5.813 0c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178 1.17 1.17 0 0 1-1.162-1.178c0-.651.52-1.18 1.162-1.18zm5.34 2.867c-1.797-.052-3.746.512-5.28 1.786-1.72 1.428-2.687 3.72-1.78 6.22.942 2.453 3.666 4.229 6.884 4.229.826 0 1.622-.12 2.361-.336a.722.722 0 0 1 .598.082l1.584.926a.272.272 0 0 0 .14.047c.134 0 .24-.111.24-.248 0-.06-.024-.12-.04-.177l-.327-1.233a.582.582 0 0 1-.023-.156.49.49 0 0 1 .201-.398C23.024 18.48 24 16.82 24 14.98c0-3.21-2.931-5.837-6.656-6.088V8.89c-.135-.008-.266-.03-.403-.03zm-2.053 7.432c-.476 0-.867-.436-.867-.972s.391-.972.867-.972c.477 0 .868.436.868.972s-.391.972-.868.972zm4.836 0c-.476 0-.867-.436-.867-.972s.391-.972.867-.972c.477 0 .868.436.868.972s-.391.972-.868.972z"/>
            </svg>
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import '@/styles/login.css';

.login-mode-group {
  width: 100%;
  display: flex;
}

.login-mode-group :deep(.el-radio-button) {
  flex: 1;
}

.login-mode-group :deep(.el-radio-button__inner) {
  width: 100%;
}

.login-extra {
  text-align: center;
  margin-top: 15px;
}

.login-extra a {
  color: #999;
  text-decoration: none;
  font-size: 14px;
}

.login-extra a:hover {
  color: #409eff;
}

.third-party-login {
  margin-top: 20px;
}

.divider {
  display: flex;
  align-items: center;
  margin: 20px 0;
}

.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #eee;
}

.divider span {
  padding: 0 15px;
  color: #999;
  font-size: 12px;
}

.third-party-icons {
  display: flex;
  justify-content: center;
  gap: 15px;
}

.third-party-icons .el-button {
  width: 44px;
  height: 44px;
  padding: 0;
}
</style>
