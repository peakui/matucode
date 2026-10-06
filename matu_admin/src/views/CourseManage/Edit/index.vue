<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import SectionCard from '@/components/SectionCard/index.vue'
import ImageUploadField from '@/components/ImageUploadField/index.vue'
import { createCourse, getCourseDetail, updateCourse } from '@/api'
import type { CourseDetailVO, CreateCourseRequest } from '@/api/types'
import './index.scss'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const isEdit = computed(() => Boolean(route.params.courseId))
const courseId = computed(() => String(route.params.courseId || ''))

const courseForm = reactive<CreateCourseRequest>({
  categoryId: undefined,
  title: '',
  subtitle: '',
  description: '',
  coverUrl: '',
  price: 0,
  originalPrice: 0,
  level: 1,
  language: 'zh-CN',
  isFree: 0,
})

const detail = ref<CourseDetailVO | null>(null)

const resetForm = () => {
  courseForm.categoryId = undefined
  courseForm.title = ''
  courseForm.subtitle = ''
  courseForm.description = ''
  courseForm.coverUrl = ''
  courseForm.price = 0
  courseForm.originalPrice = 0
  courseForm.level = 1
  courseForm.language = 'zh-CN'
  courseForm.isFree = 0
}

const loadDetail = async () => {
  if (!courseId.value) return
  loading.value = true
  try {
    const res = await getCourseDetail(courseId.value)
    detail.value = res
    courseForm.categoryId = res.categoryId
    courseForm.title = res.title || ''
    courseForm.subtitle = res.subtitle || ''
    courseForm.description = res.description || ''
    courseForm.coverUrl = res.coverUrl || ''
    courseForm.price = res.price || 0
    courseForm.originalPrice = res.originalPrice || 0
    courseForm.level = res.level || 1
    courseForm.language = res.language || 'zh-CN'
    courseForm.isFree = res.isFree || 0
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    if (courseId.value) {
      await updateCourse(courseId.value, { ...courseForm })
      ElMessage.success('课程已更新')
    } else {
      const res = await createCourse({ ...courseForm })
      detail.value = res
      ElMessage.success('课程已创建')
      await router.replace(`/admin/course/edit/${String(res.id || '')}`)
    }
  } finally {
    submitting.value = false
  }
}

const handleBack = () => {
  void router.push('/admin/course')
}

onMounted(async () => {
  if (isEdit.value) {
    await loadDetail()
  } else {
    resetForm()
  }
})
</script>

<template>
  <div class="course-workspace-page">
    <div class="filter-action-row">
      <el-button @click="handleBack">返回列表</el-button>
    </div>

    <SectionCard title="课程基础信息" description="封面图使用本地图片上传。">
      <el-form :model="courseForm" label-position="top" class="course-workspace-form" v-loading="loading">
        <div class="course-workspace-form__grid">
          <el-form-item label="课程标题">
            <el-input v-model="courseForm.title" placeholder="课程标题" size="large" />
          </el-form-item>
          <el-form-item label="课程副标题">
            <el-input v-model="courseForm.subtitle" placeholder="课程副标题" size="large" />
          </el-form-item>
          <el-form-item label="分类 ID">
            <el-input v-model="courseForm.categoryId" placeholder="分类 ID" size="large" />
          </el-form-item>
          <el-form-item label="课程语言">
            <el-input v-model="courseForm.language" placeholder="如 zh-CN" size="large" />
          </el-form-item>
          <el-form-item label="难度等级">
            <el-select v-model="courseForm.level" placeholder="选择难度" size="large">
              <el-option :value="1" label="入门" />
              <el-option :value="2" label="进阶" />
              <el-option :value="3" label="高级" />
            </el-select>
          </el-form-item>
          <el-form-item label="是否免费">
            <el-select v-model="courseForm.isFree" placeholder="是否免费" size="large">
              <el-option :value="1" label="免费" />
              <el-option :value="0" label="收费" />
            </el-select>
          </el-form-item>
          <el-form-item label="课程价格">
            <el-input-number v-model="courseForm.price" :min="0" controls-position="right" />
          </el-form-item>
          <el-form-item label="原价">
            <el-input-number v-model="courseForm.originalPrice" :min="0" controls-position="right" />
          </el-form-item>
        </div>

        <el-form-item label="课程封面">
          <ImageUploadField v-model="courseForm.coverUrl" plus-only />
        </el-form-item>

        <el-form-item label="课程描述">
          <el-input v-model="courseForm.description" type="textarea" :rows="6" placeholder="请输入课程描述" />
        </el-form-item>

        <div class="course-workspace-form__footer">
          <el-button @click="handleBack">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">保存课程</el-button>
        </div>
      </el-form>
    </SectionCard>

    <SectionCard v-if="detail" title="课程结构概览" description="用于快速查看章节、视频与文章结构。">
      <div class="course-workspace-summary">
        <div>
          <span>章节数</span>
          <strong>{{ detail.chapters?.length || 0 }}</strong>
        </div>
        <div>
          <span>视频数</span>
          <strong>{{ detail.chapters?.reduce((sum, chapter) => sum + (chapter.videoCount || chapter.videos?.length || 0), 0) || 0 }}</strong>
        </div>
        <div>
          <span>文章数</span>
          <strong>{{ detail.articles?.length || 0 }}</strong>
        </div>
      </div>
    </SectionCard>
  </div>
</template>
