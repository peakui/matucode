<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  completeCourseVideoUpload,
  getFileChunkStatusApi,
  initCourseVideoUpload,
  uploadFileChunkApi,
} from '@/api'
import ImageUploadField from '@/components/ImageUploadField/index.vue'
import type { CourseVideoVO, InitCourseVideoUploadRequest } from '@/api/types'
import './index.scss'

const props = withDefaults(
  defineProps<{
    chapterId?: string | number
    modelValue?: CourseVideoVO | null
  }>(),
  {
    modelValue: null,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: CourseVideoVO | null]
}>()

const uploading = ref(false)
const progress = ref(0)
const selectedFile = ref<File | null>(null)
const currentFileName = ref('')
const videoForm = ref({
  videoTitle: '',
  videoDesc: '',
  coverUrl: '',
  duration: 0,
  resolution: '1080p',
  sortOrder: 0,
  isFreePreview: 0,
})

watch(
  () => props.modelValue,
  (value) => {
    if (!value) return
    videoForm.value = {
      videoTitle: value.videoTitle || '',
      videoDesc: value.videoDesc || '',
      coverUrl: value.coverUrl || '',
      duration: value.duration || 0,
      resolution: value.resolution || '1080p',
      sortOrder: value.sortOrder || 0,
      isFreePreview: value.isFreePreview || 0,
    }
  },
  { immediate: true },
)

const canUpload = computed(() => Boolean(props.chapterId))

const sliceFile = (file: File, chunkSize: number) => {
  const chunks: Blob[] = []
  for (let offset = 0; offset < file.size; offset += chunkSize) {
    chunks.push(file.slice(offset, Math.min(offset + chunkSize, file.size)))
  }
  return chunks
}

const handleSelectFile = (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  selectedFile.value = file
  currentFileName.value = file.name
  progress.value = 0
  input.value = ''
  ElMessage.success('视频已选择，点击保存并上传后提交')
}

const handleSave = async () => {
  const file = selectedFile.value
  if (!props.chapterId) return
  if (!file) {
    ElMessage.warning('请先选择本地视频')
    return
  }

  uploading.value = true
  progress.value = 0

  try {
    const chunkSize = 5 * 1024 * 1024
    const chunkList = sliceFile(file, chunkSize)
    const chunkCount = chunkList.length
    const initPayload: InitCourseVideoUploadRequest = {
      chapterId: props.chapterId,
      originalName: file.name,
      fileType: file.type || 'video',
      fileSize: file.size,
      fileMd5: undefined,
      chunkSize,
      chunkCount,
    }

    const uploadInit = await initCourseVideoUpload(initPayload)
    const fileId = uploadInit.fileId || 0

    if (uploadInit.uploadedChunks?.length) {
      const uploadedChunkSet = new Set(uploadInit.uploadedChunks)
      for (let index = 0; index < chunkList.length; index += 1) {
        if (uploadedChunkSet.has(index + 1)) {
          progress.value = Math.round(((index + 1) / chunkCount) * 100)
        }
      }
    }

    for (let index = 0; index < chunkList.length; index += 1) {
      if (uploadInit.uploadedChunks?.includes(index + 1)) continue
      const chunk = chunkList[index]
      if (!chunk) continue
      await uploadFileChunkApi({
        fileId,
        chunkNo: index + 1,
        file: chunk,
      })
      progress.value = Math.round(((index + 1) / chunkCount) * 100)
    }

    await getFileChunkStatusApi(fileId)

    const completed = await completeCourseVideoUpload({
      chapterId: props.chapterId,
      fileId,
      videoTitle: videoForm.value.videoTitle || file.name,
      videoDesc: videoForm.value.videoDesc,
      coverUrl: videoForm.value.coverUrl,
      resolution: videoForm.value.resolution,
      sortOrder: videoForm.value.sortOrder,
      isFreePreview: videoForm.value.isFreePreview,
    })

    emit('update:modelValue', completed)
    ElMessage.success('视频上传成功')
  } finally {
    uploading.value = false
  }
}

</script>

<template>
  <div class="chunk-video-upload-field">
    <el-form :model="videoForm" label-position="top" class="chunk-video-upload-field__form">
      <el-form-item label="视频标题">
        <el-input v-model="videoForm.videoTitle" placeholder="视频标题" size="large" />
      </el-form-item>
      <el-form-item label="视频描述">
        <el-input v-model="videoForm.videoDesc" type="textarea" :rows="3" placeholder="视频描述" />
      </el-form-item>
      <el-form-item label="分辨率">
        <el-input v-model="videoForm.resolution" placeholder="如 1080p" size="large" />
      </el-form-item>
      <div class="chunk-video-upload-field__grid">
        <el-form-item label="排序值">
          <el-input-number v-model="videoForm.sortOrder" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="是否试看">
          <el-select v-model="videoForm.isFreePreview" placeholder="是否试看">
            <el-option :value="1" label="是" />
            <el-option :value="0" label="否" />
          </el-select>
        </el-form-item>
      </div>

      <div class="chunk-video-upload-field__media-row">
        <el-form-item label="视频封面" class="chunk-video-upload-field__media-item">
          <ImageUploadField v-model="videoForm.coverUrl" :disabled="uploading" plus-only />
        </el-form-item>

        <el-form-item label="选择视频" class="chunk-video-upload-field__media-item">
          <div class="chunk-video-upload-field__picker">
            <div class="chunk-video-upload-field__picker-meta">
              <strong>{{ currentFileName || '未选择视频文件' }}</strong>
              <span>{{ progress }}%</span>
            </div>
            <p>{{ selectedFile ? '已选择视频，点击底部保存并上传后提交。' : '仅支持本地视频文件，选择后点击保存并上传。' }}</p>
            <label
              class="chunk-video-upload-field__choose"
              :class="{ 'is-disabled': !canUpload || uploading }"
            >
              <input type="file" accept="video/*" :disabled="uploading || !canUpload" @change="handleSelectFile" />
              <span>选择本地视频</span>
            </label>
          </div>
        </el-form-item>
      </div>
    </el-form>


    <div class="chunk-video-upload-field__footer">
      <p v-if="!canUpload" class="chunk-video-upload-field__hint">请先选择章节后再上传视频。</p>
      <el-button type="primary" :disabled="!canUpload" :loading="uploading" @click="handleSave">保存并上传</el-button>
    </div>
  </div>
</template>
