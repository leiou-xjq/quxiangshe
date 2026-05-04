<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-left">
        <div class="brand-info">
          <div class="brand-logo">
            <svg viewBox="0 0 48 48" width="48" height="48">
              <circle cx="24" cy="24" r="24" fill="#FF7D00"/>
              <text x="24" y="32" text-anchor="middle" fill="white" font-size="20" font-weight="bold">享</text>
            </svg>
          </div>
          <h1 class="brand-name">趣享社</h1>
          <p class="brand-slogan">分享精彩，发现美好</p>
        </div>
        
        <div class="qr-code-area">
          <div class="qr-code">
            <div class="qr-placeholder">
              <svg viewBox="0 0 120 120" width="120" height="120">
                <rect width="120" height="120" fill="white" rx="8"/>
                <rect x="15" y="15" width="35" height="35" fill="#FF7D00" rx="4"/>
                <rect x="70" y="15" width="35" height="35" fill="#FF7D00" rx="4"/>
                <rect x="15" y="70" width="35" height="35" fill="#FF7D00" rx="4"/>
                <rect x="25" y="25" width="15" height="15" fill="white"/>
                <rect x="80" y="25" width="15" height="15" fill="white"/>
                <rect x="25" y="80" width="15" height="15" fill="white"/>
              </svg>
            </div>
          </div>
          <p class="qr-tip">打开趣享社APP扫码登录</p>
        </div>
      </div>

      <div class="login-right">
        <div class="login-tabs">
          <span 
            :class="{ active: loginType === 'code' }" 
            @click="loginType = 'code'"
          >
            验证码登录
          </span>
          <span 
            :class="{ active: loginType === 'password' }" 
            @click="loginType = 'password'"
          >
            密码登录
          </span>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          class="login-form"
        >
          <template v-if="loginType === 'code'">
            <el-form-item prop="phone">
              <el-input
                v-model="form.phone"
                placeholder="请输入手机号"
                size="large"
                prefix-icon="Iphone"
                maxlength="11"
              />
            </el-form-item>
            
            <el-form-item prop="code">
              <el-input
                v-model="form.code"
                placeholder="请输入验证码"
                size="large"
                maxlength="6"
              >
                <template #suffix>
                  <span 
                    class="code-btn" 
                    :class="{ disabled: counting }"
                    @click="handleSendCode"
                  >
                    {{ counting ? countdown + 's' : '获取验证码' }}
                  </span>
                </template>
              </el-input>
            </el-form-item>
          </template>

          <template v-if="loginType === 'password'">
            <el-form-item prop="username">
              <el-input
                v-model="form.username"
                placeholder="请输入用户名/手机号/邮箱"
                size="large"
                prefix-icon="User"
              />
            </el-form-item>
            
            <el-form-item prop="password">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="请输入密码"
                size="large"
                prefix-icon="Lock"
                show-password
                @keyup.enter="handleSubmit"
              />
            </el-form-item>
            
            <div class="form-options">
              <el-checkbox v-model="rememberMe">记住我</el-checkbox>
              <a href="#" class="forgot-link">忘记密码?</a>
            </div>
          </template>
          
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="submit-btn"
            @click="handleSubmit"
          >
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form>

        <div class="login-footer">
          <p class="agreement-text">
            登录即表示同意 
            <a href="#">《用户协议》</a> 
            和 
            <a href="#">《隐私政策》</a>
          </p>
          <p class="register-link">
            还没有账号?
            <router-link to="/register">立即注册</router-link>
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, phoneLogin, sendVerifyCode } from '@/api/auth'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const loginType = ref('code')
const rememberMe = ref(false)

const form = reactive({
  username: '',
  password: '',
  phone: '',
  code: ''
})

const counting = ref(false)
const countdown = ref(60)

const phoneRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '请输入6位验证码', trigger: 'blur' }
  ]
}

const passwordRules = {
  username: [{ required: true, message: '请输入用户名/手机号/邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const rules = computed(() => {
  return loginType.value === 'code' ? phoneRules : passwordRules
})

watch(loginType, () => {
  formRef.value?.resetFields()
  form.code = ''
})

const handleSendCode = async () => {
  if (counting.value) return
  
  if (!form.phone || !/^1[3-9]\d{9}$/.test(form.phone)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  
  try {
    await sendVerifyCode(form.phone)
    ElMessage.success('验证码已发送')
    counting.value = true
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
        counting.value = false
        countdown.value = 60
      }
    }, 1000)
  } catch (error) {
    ElMessage.error(error.message || '发送失败')
  }
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  loading.value = true
  
  try {
    let result
    if (loginType.value === 'code') {
      result = await phoneLogin({ phone: form.phone, code: form.code })
    } else {
      result = await login({ username: form.username, password: form.password })
    }
    
    ElMessage.success('登录成功')
    userStore.setUser(result.userId, result.username, result.accessToken, result.nickname, result.avatarUrl, result.refreshToken)
    router.push('/')
  } catch (error) {
    ElMessage.error(error.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
  padding: 20px;
}

.login-container {
  display: flex;
  width: 900px;
  height: 520px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.login-left {
  width: 360px;
  background: linear-gradient(160deg, #FF7D00 0%, #FF9500 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #fff;
}

.brand-info {
  text-align: center;
  margin-bottom: 40px;
}

.brand-logo {
  margin-bottom: 16px;
}

.brand-name {
  font-size: 32px;
  font-weight: 700;
  margin: 0 0 8px;
}

.brand-slogan {
  font-size: 14px;
  opacity: 0.9;
  margin: 0;
}

.qr-code-area {
  text-align: center;
}

.qr-code {
  width: 140px;
  height: 140px;
  background: #fff;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.qr-placeholder {
  padding: 10px;
}

.qr-tip {
  font-size: 13px;
  opacity: 0.85;
  margin: 0;
}

.login-right {
  flex: 1;
  padding: 48px 48px 32px;
  display: flex;
  flex-direction: column;
}

.login-tabs {
  display: flex;
  margin-bottom: 32px;
  border-bottom: 1px solid #eee;
}

.login-tabs span {
  flex: 1;
  text-align: center;
  padding: 12px 0;
  cursor: pointer;
  color: #666;
  font-size: 16px;
  font-weight: 500;
  border-bottom: 2px solid transparent;
  transition: all 0.2s ease;
}

.login-tabs span:hover {
  color: #FF7D00;
}

.login-tabs span.active {
  color: #FF7D00;
  border-bottom-color: #FF7D00;
}

.login-form {
  flex: 1;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.form-options :deep(.el-checkbox__label) {
  color: #999;
  font-size: 13px;
}

.forgot-link {
  color: #FF7D00;
  font-size: 13px;
  text-decoration: none;
}

.forgot-link:hover {
  text-decoration: underline;
}

.submit-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #FF7D00 0%, #FF9500 100%);
  border: none;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.submit-btn:hover {
  box-shadow: 0 8px 20px rgba(255, 125, 0, 0.3);
  transform: translateY(-1px);
}

.login-footer {
  margin-top: auto;
  text-align: center;
}

.agreement-text {
  font-size: 12px;
  color: #999;
  margin: 0 0 12px;
}

.agreement-text a {
  color: #FF7D00;
  text-decoration: none;
}

.agreement-text a:hover {
  text-decoration: underline;
}

.register-link {
  font-size: 13px;
  color: #666;
  margin: 0;
}

.register-link a {
  color: #FF7D00;
  text-decoration: none;
  font-weight: 500;
}

.register-link a:hover {
  text-decoration: underline;
}

.code-btn {
  cursor: pointer;
  color: #FF7D00;
  font-size: 13px;
  padding: 0 8px;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.code-btn:hover:not(.disabled) {
  color: #FF9500;
}

.code-btn.disabled {
  color: #bbb;
  cursor: not-allowed;
}

:deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: none !important;
  border: 1px solid #e0e0e0;
  padding: 4px 12px;
}

:deep(.el-input__wrapper):hover {
  border-color: #FF7D00;
}

:deep(.el-input__wrapper.is-focus) {
  border-color: #FF7D00;
  box-shadow: 0 0 0 2px rgba(255, 125, 0, 0.1) !important;
}

:deep(.el-input__inner) {
  height: 40px;
}

:deep(.el-form-item) {
  margin-bottom: 20px;
}

:deep(.el-form-item__label) {
  color: #666;
  font-size: 14px;
  padding-bottom: 8px;
}
</style>
