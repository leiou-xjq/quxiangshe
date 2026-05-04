<template>
  <div class="publish-page">
    <div class="publish-card">
      <h2 class="page-title">发布笔记</h2>
      
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleSubmit"
      >
        <!-- 标题 -->
        <el-form-item label="标题" prop="title">
          <el-input
            v-model="form.title"
            placeholder="请输入笔记标题"
            maxlength="200"
            show-word-limit
            size="large"
          />
        </el-form-item>

        <!-- 内容 -->
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="8"
            placeholder="请输入笔记内容"
            maxlength="10000"
            show-word-limit
          />
        </el-form-item>

        <!-- 分类 -->
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="选择分类" clearable>
            <el-option label="旅行" value="旅行" />
            <el-option label="美食" value="美食" />
            <el-option label="科技" value="科技" />
            <el-option label="数码" value="数码" />
            <el-option label="生活" value="生活" />
            <el-option label="情感" value="情感" />
            <el-option label="默认" value="默认" />
          </el-select>
        </el-form-item>

        <!-- 标签 -->
        <el-form-item label="标签">
          <div class="tags-input">
            <el-tag
              v-for="tag in form.tags"
              :key="tag"
              closable
              @close="removeTag(tag)"
              class="tag-item"
            >
              {{ tag }}
            </el-tag>
            <el-input
              v-if="tagInputVisible"
              ref="tagInputRef"
              v-model="tagInputValue"
              class="tag-input"
              size="small"
              @keyup.enter="addTag"
              @blur="addTag"
            />
            <el-button v-else size="small" @click="showTagInput">
              + 添加标签
            </el-button>
          </div>
        </el-form-item>

        <!-- 封面图 -->
        <el-form-item label="封面图">
          <div class="cover-upload">
            <el-input
              v-model="form.coverImage"
              placeholder="请输入封面图URL"
              clearable
            >
              <template #prepend>URL</template>
            </el-input>
            <el-image
              v-if="form.coverImage"
              :src="form.coverImage"
              fit="cover"
              class="cover-preview"
            />
          </div>
        </el-form-item>

        <!-- 图片 -->
        <el-form-item label="图片">
          <div class="images-input">
            <div v-for="(url, idx) in form.images" :key="idx" class="image-item">
              <el-image :src="url" fit="cover" />
              <el-icon class="delete-icon" @click="removeImage(idx)"><Close /></el-icon>
            </div>
            <el-button class="add-image-btn" @click="addImage">
              <el-icon><Plus /></el-icon>
              添加图片
            </el-button>
          </div>
        </el-form-item>

        <!-- 提交 -->
        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="submitting" size="large">
            {{ submitting ? '发布中...' : '发布笔记' }}
          </el-button>
          <el-button size="large" @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useNoteStore } from '@/store/note'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import { Close, Plus } from '@element-plus/icons-vue'

const router = useRouter()
const noteStore = useNoteStore()
const userStore = useUserStore()

const formRef = ref(null)
const submitting = ref(false)

const form = ref({
  title: '',
  content: '',
  category: '',
  tags: [],
  coverImage: '',
  images: []
})

const rules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { min: 1, max: 200, message: '标题长度1-200字符', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入内容', trigger: 'blur' },
    { min: 1, max: 10000, message: '内容长度1-10000字符', trigger: 'blur' }
  ]
}

// 标签处理
const tagInputVisible = ref(false)
const tagInputValue = ref('')
const tagInputRef = ref(null)

const showTagInput = async () => {
  tagInputVisible.value = true
  await nextTick()
  tagInputRef.value?.focus()
}

const addTag = () => {
  if (tagInputValue.value.trim() && form.value.tags.length < 5) {
    if (!form.value.tags.includes(tagInputValue.value.trim())) {
      form.value.tags.push(tagInputValue.value.trim())
    }
    tagInputValue.value = ''
  }
  tagInputVisible.value = false
}

const removeTag = (tag) => {
  form.value.tags = form.value.tags.filter(t => t !== tag)
}

// 图片处理
const addImage = () => {
  const url = prompt('请输入图片URL:')
  if (url && url.startsWith('http')) {
    if (form.value.images.length < 9) {
      form.value.images.push(url)
    } else {
      ElMessage.warning('最多9张图片')
    }
  } else if (url) {
    ElMessage.warning('请输入有效的图片URL')
  }
}

const removeImage = (idx) => {
  form.value.images.splice(idx, 1)
}

// 提交
const handleSubmit = async () => {
  if (!userStore.isLoggedIn()) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await noteStore.createNote(form.value)
    ElMessage.success('发布成功')
    router.push('/profile')
  } catch (e) {
    ElMessage.error(e.message || '发布失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.publish-page {
  max-width: 800px;
  margin: 0 auto;
}

.publish-card {
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 24px 0;
  color: #333;
}

.tags-input {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.tag-item {
  margin: 0;
}

.tag-input {
  width: 100px;
}

.cover-upload {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.cover-preview {
  width: 200px;
  height: 120px;
  border-radius: 8px;
  overflow: hidden;
}

.images-input {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.image-item {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: 8px;
  overflow: hidden;
}

.image-item .el-image {
  width: 100%;
  height: 100%;
}

.delete-icon {
  position: absolute;
  top: 4px;
  right: 4px;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  border-radius: 50%;
  padding: 4px;
  cursor: pointer;
}

.add-image-btn {
  width: 100px;
  height: 100px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.add-image-btn:hover {
  border-color: #409EFF;
  color: #409EFF;
}
</style>
