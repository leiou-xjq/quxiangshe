<template>
  <div class="publish-page">
    <div class="publish-card">
      <div class="publish-header">
        <h2>发布笔记</h2>
        <p class="publish-tip">分享你的精彩生活</p>
      </div>
      
      <el-form :model="form" :rules="rules" ref="formRef" label-position="top" class="publish-form">
        <!-- 标题 -->
        <el-form-item label="标题" prop="title">
          <el-input 
            v-model="form.title" 
            placeholder="请输入标题" 
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        
        <!-- 内容 -->
        <el-form-item label="内容" prop="content">
          <el-input 
            v-model="form.content" 
            type="textarea" 
            placeholder="请输入正文内容" 
            :rows="6"
            maxlength="5000"
            show-word-limit
            @blur="checkContentSensitive"
          />
        </el-form-item>
        
        <!-- 标签 -->
        <el-form-item label="标签">
          <div class="tags-section">
            <el-checkbox-group v-model="form.tags">
              <el-checkbox label="生活" value="生活" />
              <el-checkbox label="美食" value="美食" />
              <el-checkbox label="旅行" value="旅行" />
              <el-checkbox label="摄影" value="摄影" />
              <el-checkbox label="健身" value="健身" />
              <el-checkbox label="读书" value="读书" />
              <el-checkbox label="音乐" value="音乐" />
              <el-checkbox label="游戏" value="游戏" />
            </el-checkbox-group>
          </div>
        </el-form-item>
        
        <!-- 视频上传 -->
        <el-form-item label="视频">
          <div class="video-section">
            <div v-if="form.video" class="video-preview">
              <video :src="form.video" controls></video>
              <div class="video-delete" @click="removeVideo">
                <el-icon><Close /></el-icon>
              </div>
            </div>
            <div v-else class="video-upload-btn" @click="triggerVideoUpload">
              <el-icon :size="32"><VideoCamera /></el-icon>
              <span>添加视频</span>
              <span class="video-tip">支持 mp4, mov, webm，不超过100MB</span>
            </div>
            <input 
              ref="videoInput" 
              type="file" 
              accept="video/mp4,video/quicktime,video/webm" 
              style="display: none"
              @change="handleVideoChange"
            />
          </div>
        </el-form-item>
        
        <!-- 图片上传 -->
        <el-form-item label="图片">
          <div class="upload-section">
            <div class="image-list">
              <div 
                v-for="(img, index) in form.images" 
                :key="index" 
                class="image-item"
              >
                <img :src="img" />
                <div class="image-delete" @click="removeImage(index)">
                  <el-icon><Close /></el-icon>
                </div>
              </div>
              <div 
                v-if="form.images.length < 9" 
                class="upload-btn"
                @click="triggerUpload"
              >
                <el-icon :size="32"><Plus /></el-icon>
                <span>添加图片</span>
              </div>
            </div>
            <input 
              ref="fileInput" 
              type="file" 
              accept="image/*" 
              multiple 
              style="display: none"
              @change="handleFileChange"
            />
          </div>
        </el-form-item>
        
        <!-- 地理位置 -->
        <el-form-item label="位置">
          <el-input 
            v-model="form.location" 
            placeholder="添加位置信息（可选）"
          >
            <template #prefix>
              <el-icon><Location /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        
        <!-- 提交按钮 -->
        <div class="publish-actions">
          <div class="draft-info">
            <el-button text type="info" size="small" @click="handleClearDraft">
              清除草稿
            </el-button>
          </div>
          <div class="action-btns">
            <el-button @click="handleCancel">取消</el-button>
            <el-button
              type="primary"
              :loading="submitting"
              @click="handleSubmit"
            >
              发布
            </el-button>
          </div>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Close, Plus, Location, VideoCamera } from '@element-plus/icons-vue'
import { createNote, uploadImage, uploadVideo } from '@/api/note'
import { saveDraft, loadDraft, clearDraft, debounceSave, getDraftInfo } from '@/utils/draft'

const router = useRouter()
const formRef = ref(null)
const fileInput = ref(null)
const videoInput = ref(null)
const submitting = ref(false)
const videoUploading = ref(false)
const draftLoaded = ref(false)

const form = reactive({
  title: '',
  content: '',
  tags: [],
  images: [],
  video: '',
  videoCover: '',
  location: ''
})

const rules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { min: 1, max: 200, message: '标题长度1-200字', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入内容', trigger: 'blur' }
  ]
}

onMounted(async () => {
  const draft = loadDraft()
  if (draft) {
    let message = '检测到上次未发布的草稿，是否恢复？'
    if (draft.imagesExpired) {
      message += '\n\n注意：图片链接可能已失效，请重新上传。'
    }
    try {
      await ElMessageBox.confirm(
        message,
        '恢复草稿',
        {
          confirmButtonText: '恢复',
          cancelButtonText: '不恢复',
          type: 'warning'
        }
      )
      if (draft.imagesExpired) {
        form.images = []
        ElMessage.warning('图片已清空，请重新上传')
      } else {
        Object.assign(form, draft)
      }
      draftLoaded.value = true
      ElMessage.success('草稿已恢复')
    } catch {
      clearDraft()
    }
  }
})

watch(
  () => form,
  () => {
    if (form.title || form.content || form.images.length || form.video) {
      debounceSave({ ...form })
    }
  },
  { deep: true }
)

function clearDraftAndReset() {
  clearDraft()
  form.title = ''
  form.content = ''
  form.tags = []
  form.images = []
  form.video = ''
  form.videoCover = ''
  form.location = ''
  draftLoaded.value = false
}

async function handleClearDraft() {
  try {
    await ElMessageBox.confirm('确定要清除草稿吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    clearDraftAndReset()
    ElMessage.success('草稿已清除')
  } catch {}
}

// 触发文件上传
function triggerUpload() {
  fileInput.value.click()
}

// 处理文件选择
async function handleFileChange(e) {
  const files = Array.from(e.target.files)
  if (files.length + form.images.length > 9) {
    ElMessage.warning('最多只能上传9张图片')
    return
  }
  
  for (const file of files) {
    if (file.size > 5 * 1024 * 1024) {
      ElMessage.warning('单张图片大小不能超过5MB')
      continue
    }
    
    try {
      const res = await uploadImage(file)
      form.images.push(res.data)
    } catch (e) {
      ElMessage.error('图片上传失败')
    }
  }
  
  e.target.value = ''
}

// 删除图片
function removeImage(index) {
  form.images.splice(index, 1)
}

// 触发视频上传
function triggerVideoUpload() {
  videoInput.value.click()
}

// 处理视频选择
async function handleVideoChange(e) {
  const file = e.target.files[0]
  if (!file) return
  
  if (file.size > 100 * 1024 * 1024) {
    ElMessage.warning('视频大小不能超过100MB')
    return
  }
  
  const allowedTypes = ['video/mp4', 'video/quicktime', 'video/webm']
  if (!allowedTypes.includes(file.type)) {
    ElMessage.warning('仅支持 mp4, mov, webm 格式')
    return
  }
  
  videoUploading.value = true
  try {
    const res = await uploadVideo(file)
    form.video = res.data
    form.videoCover = '' // 暂时不处理封面
    ElMessage.success('视频上传成功')
  } catch (e) {
    ElMessage.error('视频上传失败')
  } finally {
    videoUploading.value = false
    e.target.value = ''
  }
}

// 删除视频
function removeVideo() {
  form.video = ''
  form.videoCover = ''
}

// 取消
function handleCancel() {
  router.push('/')
}

// 提交
async function handleSubmit() {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitting.value = true
    
    try {
      const data = {
        title: form.title,
        content: form.content,
        tags: form.tags,
        images: form.images,
        video: form.video,
        videoCover: form.videoCover,
        location: form.location
      }
      
      await createNote(data)
      ElMessage.success('发布成功')
      clearDraft()
      router.push('/')
    } catch (e) {
      if (e.message?.includes('违规')) {
        ElMessage.error('标题或内容涉嫌违规')
      }
    } finally {
      submitting.value = false
    }
  })
}
</script>

<style scoped>
@import '@/styles/publish.css';
</style>
