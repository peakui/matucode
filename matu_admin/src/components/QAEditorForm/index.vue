<script setup lang="ts">
import { ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { QaCategoryVO } from '@/api/types'
import './index.scss'

interface QAFormModel {
  categoryId?: string | number
  title?: string
  content?: string
  bountyPoints?: number
  status?: number
}

defineProps<{
  form: QAFormModel
  categories: QaCategoryVO[]
  statusOptions?: Array<{ label: string; value: number }>
  isEdit?: boolean
}>()

const formRef = ref<FormInstance>()
const rules: FormRules<QAFormModel> = {
  title: [{ required: true, message: '请输入问题标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入详细问题内容', trigger: 'blur' }],
}

const validate = () => formRef.value?.validate()

defineExpose({ validate })
</script>

<template>
  <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="qa-editor-form">
    <div class="qa-editor-form__grid">
      <el-form-item label="问题标题" prop="title">
        <el-input
          v-model="form.title"
          placeholder="请输入问题标题"
          size="large"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>
      <el-form-item label="问题分类">
        <el-select v-model="form.categoryId" placeholder="请选择分类" clearable size="large">
          <el-option
            v-for="item in categories"
            :key="item.id"
            :label="item.categoryName"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
    </div>

    <div class="qa-editor-form__grid" :class="{ 'qa-editor-form__grid--triple': isEdit }">
      <el-form-item label="悬赏积分">
        <el-input-number
          v-model="form.bountyPoints"
          :min="0"
          :step="10"
          controls-position="right"
        />
      </el-form-item>
      <el-form-item v-if="isEdit && statusOptions?.length" label="问题状态">
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

    <el-form-item label="问题内容" prop="content">
      <el-input
        v-model="form.content"
        type="textarea"
        :rows="10"
        placeholder="请输入详细问题内容"
      />
    </el-form-item>
  </el-form>
</template>
