<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { ref } from 'vue'
import type { CreatePostRequest, PostCategoryVO } from '@/api/types'
import './index.scss'

const props = defineProps<{
  form: CreatePostRequest
  rules: FormRules<CreatePostRequest>
  categories: PostCategoryVO[]
  statusOptions: Array<{ label: string; value: number }>
}>()

const formRef = ref<FormInstance>()
defineExpose({ formRef })
</script>

<template>
  <el-form
    ref="formRef"
    :model="props.form"
    :rules="props.rules"
    label-position="top"
    class="post-editor-form"
  >
    <div class="post-editor-form__grid">
      <el-form-item label="帖子分类" prop="categoryId">
        <el-select v-model="props.form.categoryId" placeholder="请选择分类" size="large">
          <el-option
            v-for="item in props.categories"
            :key="item.id"
            :label="item.categoryName"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="发布状态" prop="status">
        <el-select v-model="props.form.status" placeholder="请选择状态" size="large">
          <el-option
            v-for="item in props.statusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
    </div>
    <el-form-item label="帖子标题" prop="title">
      <el-input
        v-model="props.form.title"
        placeholder="请输入帖子标题"
        size="large"
        maxlength="100"
        show-word-limit
      />
    </el-form-item>
    <el-form-item label="帖子摘要" prop="summary">
      <el-input
        v-model="props.form.summary"
        type="textarea"
        :rows="3"
        placeholder="请输入帖子摘要"
        maxlength="200"
        show-word-limit
      />
    </el-form-item>
    <el-form-item label="帖子内容" prop="content">
      <el-input
        v-model="props.form.content"
        type="textarea"
        :rows="8"
        placeholder="请输入帖子内容"
        maxlength="5000"
        show-word-limit
      />
    </el-form-item>
    <div class="post-editor-form__switches">
      <el-checkbox v-model="props.form.isTop" :true-value="1" :false-value="0">置顶</el-checkbox>
      <el-checkbox v-model="props.form.isEssence" :true-value="1" :false-value="0"
        >精华</el-checkbox
      >
      <el-checkbox v-model="props.form.isLock" :true-value="1" :false-value="0">锁帖</el-checkbox>
    </div>
    <el-form-item v-if="props.form.isLock" label="锁帖原因" prop="lockReason">
      <el-input v-model="props.form.lockReason" placeholder="请输入锁帖原因" size="large" />
    </el-form-item>
  </el-form>
</template>
