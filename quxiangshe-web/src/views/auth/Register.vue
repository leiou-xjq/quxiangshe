<template>
  <div class="register-page">
    <div class="register-container">
      <div class="register-left">
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

      <div class="register-right">
        <div class="register-header">
          <h2 class="register-title">注册账号</h2>
          <p class="register-subtitle">加入趣享社，开始分享你的精彩生活</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          class="register-form"
        >
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="用户名（4-20个字符）"
              size="large"
              prefix-icon="User"
            />
          </el-form-item>

          <el-form-item prop="phone">
            <el-input
              v-model="form.phone"
              placeholder="手机号"
              size="large"
              prefix-icon="Iphone"
              maxlength="11"
            />
          </el-form-item>

          <el-form-item prop="code">
            <el-input
              v-model="form.code"
              placeholder="验证码"
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
          
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="设置密码（6-20个字符）"
              size="large"
              prefix-icon="Lock"
              show-password
            />
          </el-form-item>

          <el-form-item prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="确认密码"
              size="large"
              prefix-icon="Lock"
              show-password
            />
          </el-form-item>
          
          <div class="agreement-wrap">
            <el-checkbox v-model="agreeTerms">
              <span class="agreement-text">
                我已阅读并同意 
                <a href="#">《用户协议》</a> 
                和 
                <a href="#">《隐私政策》</a>
              </span>
            </el-checkbox>
          </div>
          
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            :disabled="!agreeTerms"
            class="submit-btn"
            @click="handleSubmit"
          >
            {{ loading ? '注册中...' : '注 册' }}
          </el-button>
        </el-form>

        <div class="register-footer">
          <p class="login-link">
            已有账号?
            <router-link to="/login">立即登录</router-link>
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register, sendVerifyCode, checkUsernameExists, checkPhoneExists } from '@/api/auth'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const agreeTerms = ref(false)
const counting = ref(false)
const countdown = ref(60)

const form = reactive({
  username: '',
  phone: '',
  code: '',
  password: '',
  confirmPassword: ''
})

const validatePass2 = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.password) {
    callback(new Error('两次密码输入不一致'))
  } else {
    callback()
  }
}

const validateUsername = async (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入用户名'))
  } else if (value.length < 4 || value.length > 20) {
    callback(new Error('用户名4-20个字符'))
  } else {
    try {
      const exists = await checkUsernameExists(value)
      if (exists) {
        callback(new Error('用户名已被注册'))
      } else {
        callback()
      }
    } catch (e) {
      callback()
    }
  }
}

const validatePhone = async (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入手机号'))
  } else if (!/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('手机号格式不正确'))
  } else {
    try {
      const exists = await checkPhoneExists(value)
      if (exists) {
        callback(new Error('手机号已被注册'))
      } else {
        callback()
      }
    } catch (e) {
      callback()
    }
  }
}

const rules = {
  username: [
    { required: true, validator: validateUsername, trigger: 'blur' }
  ],
  phone: [
    { required: true, validator: validatePhone, trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '请输入6位验证码', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请设置密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码6-20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validatePass2, trigger: 'blur' }
  ]
}

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
    await register({
      username: form.username,
      phone: form.phone,
      password: form.password
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (error) {
    ElMessage.error(error.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
  padding: 20px;
}

.register-container {
  display: flex;
  width: 900px;
  height: 620px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.register-left {
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

.register-right {
  flex: 1;
  padding: 40px 48px 32px;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.register-header {
  margin-bottom: 28px;
}

.register-title {
  font-size: 28px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px;
}

.register-subtitle {
  font-size: 14px;
  color: #999;
  margin: 0;
}

.register-form {
  flex: 1;
}

.agreement-wrap {
  margin-bottom: 20px;
}

.agreement-text {
  color: #999;
  font-size: 12px;
}

.agreement-text a {
  color: #FF7D00;
  text-decoration: none;
}

.agreement-text a:hover {
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

.submit-btn:hover:not(:disabled) {
  box-shadow: 0 8px 20px rgba(255, 125, 0, 0.3);
  transform: translateY(-1px);
}

.submit-btn:disabled {
  background: #ccc;
}

.register-footer {
  text-align: center;
  margin-top: auto;
  padding-top: 16px;
}

.login-link {
  font-size: 13px;
  color: #666;
  margin: 0;
}

.login-link a {
  color: #FF7D00;
  text-decoration: none;
  font-weight: 500;
}

.login-link a:hover {
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
  margin-bottom: 18px;
}

:deep(.el-form-item__label) {
  color: #666;
  font-size: 14px;
  padding-bottom: 8px;
}

:deep(.el-checkbox__label) {
  color: #999;
  font-size: 12px;
}
</style>
