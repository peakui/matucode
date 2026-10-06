<script setup lang="ts">
import { ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import ImageUploadField from '@/components/ImageUploadField/index.vue'
import type { CreateCheckRecordRequest } from '@/api/types'
import './index.scss'

const { form } = defineProps<{
  form: CreateCheckRecordRequest
}>()

const formRef = ref<FormInstance>()

const rules: FormRules<CreateCheckRecordRequest> = {
  title: [{ required: true, message: '请输入打卡标题', trigger: 'blur' }],
  checkDate: [{ required: true, message: '请选择打卡日期', trigger: 'change' }],
  learnHours: [{ required: true, message: '请输入学习时长', trigger: 'blur' }],
  content: [{ required: true, message: '请输入打卡正文内容', trigger: 'blur' }],
}

const getImageUrls = () => {
  if (!form.imageUrls) form.imageUrls = []
  return form.imageUrls
}

const handleImageChange = (index: number, value: string) => {
  const imageUrls = getImageUrls()
  imageUrls[index] = value
  form.imageUrls = imageUrls.filter(Boolean).slice(0, 9)
}

const handleRemoveImage = (index: number) => {
  getImageUrls().splice(index, 1)
}

const statusOptions = [
  { label: '已发布', value: 1 },
  { label: '草稿', value: 2 },
  { label: '仅自己可见', value: 4 },
]

const moodOptions = [1, 2, 3, 4, 5]

const validate = () => formRef.value?.validate()

defineExpose({ validate })
</script>

<template>
  <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="checkin-editor-form">
    <div class="checkin-editor-form__grid">
      <el-form-item label="打卡标题" prop="title">
        <el-input
          v-model="form.title"
          placeholder="请输入打卡标题"
          size="large"
          maxlength="100"
          show-word-limit
        />
      </el-form-item>
      <el-form-item label="打卡日期" prop="checkDate">
        <el-date-picker
          v-model="form.checkDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择打卡日期"
          size="large"
        />
      </el-form-item>
    </div>
    <div class="checkin-editor-form__grid checkin-editor-form__grid--triple">
      <el-form-item label="学习时长(小时)" prop="learnHours">
        <el-input v-model="form.learnHours" placeholder="例如 2.50" size="large" />
      </el-form-item>
      <el-form-item label="心情值">
        <el-select v-model="form.mood" placeholder="请选择心情" clearable size="large">
          <el-option
            v-for="item in moodOptions"
            :key="item"
            :label="`心情 ${item}`"
            :value="item"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="form.status" placeholder="请选择状态" size="large">
          <el-option
            v-for="item in statusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
    </div>
    <el-form-item label="摘要">
      <el-input
        v-model="form.summary"
        type="textarea"
        :rows="3"
        placeholder="请输入摘要"
        maxlength="255"
        show-word-limit
      />
    </el-form-item>
    <el-form-item label="正文内容" prop="content">
      <el-input v-model="form.content" type="textarea" :rows="8" placeholder="请输入打卡正文内容" />
    </el-form-item>
    <el-form-item label="打卡地点">
      <el-input
        v-model="form.location"
        placeholder="请输入打卡地点"
        size="large"
        maxlength="100"
        show-word-limit
      />
    </el-form-item>
    <el-form-item label="打卡图片">
      <div class="checkin-editor-form__images">
        <div
          v-for="(image, index) in form.imageUrls"
          :key="`${image || 'image'}-${index}`"
          class="checkin-editor-form__image-item"
        >
          <ImageUploadField :model-value="image" plus-only @update:model-value="handleImageChange(index, $event)" />
          <el-button type="danger" link @click="handleRemoveImage(index)">删除</el-button>
        </div>
        <ImageUploadField
          v-if="(form.imageUrls?.length || 0) < 9"
          model-value=""
          plus-only
          @update:model-value="handleImageChange(form.imageUrls?.length || 0, $event)"
        />
      </div>
    </el-form-item>
  </el-form>
</template>
