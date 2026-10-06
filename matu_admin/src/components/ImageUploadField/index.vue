<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadFileApi } from '@/api'
import './index.scss'

const props = withDefaults(
  defineProps<{
    modelValue?: string
    disabled?: boolean
    plusOnly?: boolean
  }>(),
  {
    modelValue: '',
    disabled: false,
    plusOnly: false,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const fileInput = ref<HTMLInputElement | null>(null)
const uploading = ref(false)
const previewUrl = ref(props.modelValue)

watch(
  () => props.modelValue,
  (value) => {
    previewUrl.value = value || ''
  },
)

const triggerPick = () => {
  if (props.disabled || uploading.value) return
  fileInput.value?.click()
}

const handleFileChange = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  uploading.value = true
  try {
    const res = await uploadFileApi({
      file,
      isPublic: 1,
    })
    const url = res.fileUrl || ''
    previewUrl.value = url
    emit('update:modelValue', url)
    ElMessage.success('封面上传成功')
  } finally {
    uploading.value = false
    input.value = ''
  }
}
</script>

<template>
  <div class="image-upload-field" :class="{ 'image-upload-field--plus-only': plusOnly }">
    <button
      class="image-upload-field__preview"
      type="button"
      :disabled="disabled || uploading"
      @click="triggerPick"
    >
      <img v-if="previewUrl" :src="previewUrl" alt="cover" />
      <div v-else class="image-upload-field__empty">
        <span>+</span>
      </div>
    </button>
    <input
      ref="fileInput"
      type="file"
      accept="image/*"
      class="image-upload-field__input"
      :disabled="disabled || uploading"
      @change="handleFileChange"
    />
  </div>
</template>
