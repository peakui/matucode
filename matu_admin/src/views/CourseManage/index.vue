<script setup lang="ts">
import type { TagProps } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import SectionCard from '@/components/SectionCard/index.vue'
import DetailMetricsGrid from '@/components/DetailMetricsGrid/index.vue'
import {
  createChapter,
  getCourseDetail,
  listCourses,
  offlineCourse,
  publishCourse,
} from '@/api'
import type {
  CourseArticleVO,
  CourseChapterVO,
  CourseDetailVO,
  CourseListItemVO,
  CreateChapterRequest,
} from '@/api/types'
import './index.scss'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const submitLoading = ref(false)
const detailLoading = ref(false)
const detailVisible = ref(false)
const chapterVisible = ref(false)
const total = ref(0)
const tableData = ref<CourseListItemVO[]>([])
const currentCourse = ref<CourseDetailVO | null>(null)
const activeTab = ref<'chapters'>('chapters')

const filterForm = reactive({
  keyword: '',
  categoryId: undefined as string | number | undefined,
  level: undefined as number | undefined,
  freeOnly: undefined as boolean | undefined,
  sortBy: 'latest' as 'latest' | 'popular' | 'rating',
  pageNum: 1,
  pageSize: 10,
})

const chapterForm = reactive<CreateChapterRequest>({
  courseId: '',
  chapterTitle: '',
  chapterDesc: '',
  sortOrder: 0,
  isFreePreview: 0,
})

const levelOptions = [
  { label: '全部等级', value: undefined },
  { label: '入门', value: 1 },
  { label: '进阶', value: 2 },
  { label: '高级', value: 3 },
]

const sortOptions = [
  { label: '最新', value: 'latest' },
  { label: '热门', value: 'popular' },
  { label: '评分', value: 'rating' },
]

const freeOptions = [
  { label: '全部', value: undefined },
  { label: '免费课程', value: true },
  { label: '收费课程', value: false },
]

const formatDateTime = (value?: string) => (!value ? '-' : value.replace('T', ' '))
const formatDuration = (seconds?: number) => {
  if (!seconds && seconds !== 0) return '-'
  const totalSeconds = Number(seconds)
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const remain = totalSeconds % 60
  if (hours > 0) return `${hours}h ${minutes}m ${remain}s`
  if (minutes > 0) return `${minutes}m ${remain}s`
  return `${remain}s`
}
const formatPrice = (value?: number | string) => {
  if (value === undefined || value === null || value === '') return '¥0'
  const num = Number(value)
  return Number.isNaN(num) ? String(value) : `¥${num.toFixed(2)}`
}
const getLevelText = (level?: number) => ({ 1: '入门', 2: '进阶', 3: '高级' })[level || 0] || '未知'
const getLevelType = (level?: number): TagProps['type'] =>
  (({ 1: 'success', 2: 'warning', 3: 'danger' })[level || 0] as TagProps['type']) || 'info'
const getFreeText = (isFree?: number) => (isFree === 1 ? '免费' : '收费')
const getFreeType = (isFree?: number): TagProps['type'] => (isFree === 1 ? 'success' : 'warning')
const getOwnerName = (course: CourseListItemVO) =>
  course.instructorName || course.userName || course.username || course.nickname || '-'
const getOwnerId = (course: CourseListItemVO) => course.instructorId ?? course.userId ?? '-'
const getPublishStatusText = (course: CourseListItemVO) => {
  if (typeof course.status === 'number') {
    return ({ 0: '草稿', 1: '已发布', 2: '已下架' })[course.status] || '未知'
  }
  return course.publishedAt ? '已发布' : '草稿'
}
const getPublishStatusType = (course: CourseListItemVO): TagProps['type'] => {
  if (typeof course.status === 'number') {
    return (({ 0: 'info', 1: 'success', 2: 'warning' })[course.status] as TagProps['type']) || 'info'
  }
  return course.publishedAt ? 'success' : 'info'
}
const isPublished = (course: CourseListItemVO) =>
  typeof course.status === 'number' ? course.status === 1 : Boolean(course.publishedAt)

const resetChapterForm = () => {
  chapterForm.courseId = ''
  chapterForm.chapterTitle = ''
  chapterForm.chapterDesc = ''
  chapterForm.sortOrder = 0
  chapterForm.isFreePreview = 0
}

const fetchCourseList = async () => {
  loading.value = true
  try {
    const res = await listCourses({
      keyword: filterForm.keyword || undefined,
      categoryId: filterForm.categoryId,
      level: filterForm.level,
      freeOnly: filterForm.freeOnly,
      sortBy: filterForm.sortBy,
      pageNum: filterForm.pageNum,
      pageSize: filterForm.pageSize,
    })
    tableData.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

const fetchCourseDetail = async (courseId: string | number) => {
  detailLoading.value = true
  try {
    const res = await getCourseDetail(courseId)
    currentCourse.value = res
    return res
  } finally {
    detailLoading.value = false
  }
}

const handleSearch = () => {
  filterForm.pageNum = 1
  void fetchCourseList()
}

const handleReset = () => {
  filterForm.keyword = ''
  filterForm.categoryId = undefined
  filterForm.level = undefined
  filterForm.freeOnly = undefined
  filterForm.sortBy = 'latest'
  filterForm.pageNum = 1
  void fetchCourseList()
}

const handleViewCourse = async (row: CourseListItemVO) => {
  detailVisible.value = true
  activeTab.value = 'chapters'
  await fetchCourseDetail(row.id || '')
}

const handlePublish = async (row: CourseListItemVO) => {
  await ElMessageBox.confirm(`确认发布课程《${row.title}》吗？`, '发布课程', { type: 'warning' })
  await publishCourse(row.id || '')
  ElMessage.success('发布成功')
  await fetchCourseList()
}

const handleOffline = async (row: CourseListItemVO) => {
  await ElMessageBox.confirm(`确认下架课程《${row.title}》吗？`, '下架课程', { type: 'warning' })
  await offlineCourse(row.id || '')
  ElMessage.success('下架成功')
  await fetchCourseList()
}

const handleOpenChapter = (course: CourseDetailVO) => {
  currentCourse.value = course
  resetChapterForm()
  chapterForm.courseId = course.id || ''
  chapterVisible.value = true
}

const handleSubmitChapter = async () => {
  if (!currentCourse.value?.id) return
  submitLoading.value = true
  try {
    await createChapter({
      courseId: chapterForm.courseId || currentCourse.value.id,
      chapterTitle: chapterForm.chapterTitle,
      chapterDesc: chapterForm.chapterDesc,
      sortOrder: chapterForm.sortOrder,
      isFreePreview: chapterForm.isFreePreview,
    })
    ElMessage.success('章节创建成功')
    chapterVisible.value = false
    await fetchCourseDetail(currentCourse.value.id)
    await fetchCourseList()
  } finally {
    submitLoading.value = false
  }
}

const handlePageChange = (page: number) => {
  filterForm.pageNum = page
  void fetchCourseList()
}

const handleSizeChange = (size: number) => {
  filterForm.pageSize = size
  filterForm.pageNum = 1
  void fetchCourseList()
}

const chapterCount = computed(() => currentCourse.value?.chapters?.length || 0)
const articleCount = computed(() => currentCourse.value?.articles?.length || 0)
const videoCount = computed(() =>
  currentCourse.value?.chapters?.reduce(
    (sum, chapter) => sum + (chapter.videoCount ?? chapter.videos?.length ?? 0),
    0,
  ) || 0,
)
const getArticlesForChapter = (chapter: CourseChapterVO) => {
  const articleMap = new Map<string, CourseArticleVO>()
  ;(chapter.articles || []).forEach((article) => {
    if (article.id) articleMap.set(String(article.id), article)
  })
  ;(currentCourse.value?.articles || [])
    .filter((article) => String(article.chapterId || '') === String(chapter.id || ''))
    .forEach((article) => {
      if (article.id) articleMap.set(String(article.id), article)
    })
  return Array.from(articleMap.values()).sort((a, b) => Number(a.sortOrder || 0) - Number(b.sortOrder || 0))
}
const formatFileSize = (size?: number) => {
  if (!size && size !== 0) return '-'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  if (size < 1024 * 1024 * 1024) return `${(size / (1024 * 1024)).toFixed(1)} MB`
  return `${(size / (1024 * 1024 * 1024)).toFixed(1)} GB`
}

const goToEdit = (courseId: string | number) => {
  void router.push(`/admin/course/edit/${String(courseId)}`)
}

const goToChapters = (courseId: string | number) => {
  void router.push(`/admin/course/${String(courseId)}/chapters`)
}

const goToCreate = () => {
  void router.push('/admin/course/edit')
}

onMounted(async () => {
  await fetchCourseList()
})
</script>

<template>
  <router-view v-if="route.path !== '/admin/course'" />
  <div v-else class="course-manage-page">
    <SectionCard class="course-manage-page__filter">
      <div class="filter-action-row">
        <el-button type="primary" @click="goToCreate">新建课程</el-button>
      </div>
      <div class="filter-grid">
        <el-input
          v-model="filterForm.keyword"
          placeholder="输入课程标题或副标题"
          clearable
          size="large"
        />
        <el-input v-model="filterForm.categoryId" placeholder="分类ID" clearable size="large" />
        <el-select v-model="filterForm.level" placeholder="选择难度" clearable size="large">
          <el-option
            v-for="item in levelOptions"
            :key="String(item.label)"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-select v-model="filterForm.freeOnly" placeholder="是否免费" clearable size="large">
          <el-option
            v-for="item in freeOptions"
            :key="String(item.label)"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-select v-model="filterForm.sortBy" placeholder="排序方式" size="large">
          <el-option
            v-for="item in sortOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-button type="primary" size="large" @click="handleSearch">搜索课程</el-button>
        <el-button size="large" @click="handleReset">重置</el-button>
      </div>
    </SectionCard>

    <SectionCard title="课程列表" description="点击编辑后可进入课程基础编辑，点击内容管理可统一维护章节、视频和文字教程。">
      <el-table :data="tableData" v-loading="loading" width="100%">
        <el-table-column prop="title" label="课程标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="subtitle" label="副标题" min-width="220" show-overflow-tooltip />
        <el-table-column label="所属用户" min-width="160" show-overflow-tooltip>
          <template #default="scope">
            <div class="course-owner-cell">
              <strong>{{ getOwnerName(scope.row) }}</strong>
              <span>ID：{{ getOwnerId(scope.row) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="等级" width="100">
          <template #default="scope">
            <el-tag :type="getLevelType(scope.row.level)" effect="light">{{ getLevelText(scope.row.level) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="价格" width="110">
          <template #default="scope">{{ formatPrice(scope.row.price) }}</template>
        </el-table-column>
        <el-table-column label="免费" width="100">
          <template #default="scope">
            <el-tag :type="getFreeType(scope.row.isFree)" effect="light">{{ getFreeText(scope.row.isFree) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="studentCount" label="学员" width="90" />
        <el-table-column prop="chapterCount" label="章节" width="90" />
        <el-table-column prop="videoCount" label="视频" width="90" />
        <el-table-column label="评分" width="90">
          <template #default="scope">{{ scope.row.rating ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="scope">
            <el-tag :type="getPublishStatusType(scope.row)" effect="light">{{ getPublishStatusText(scope.row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" min-width="180">
          <template #default="scope">{{ formatDateTime(scope.row.publishedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="360" fixed="right">
          <template #default="scope">
            <el-button type="primary" link @click="handleViewCourse(scope.row)">查看</el-button>
            <el-button type="success" link @click="goToEdit(scope.row.id || '')">编辑</el-button>
            <el-button type="primary" link @click="goToChapters(scope.row.id || '')">内容管理</el-button>
            <el-button
              v-if="!isPublished(scope.row)"
              type="success"
              link
              @click="handlePublish(scope.row)"
            >发布</el-button>
            <el-button v-else type="warning" link @click="handleOffline(scope.row)">下架</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-pagination">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :current-page="filterForm.pageNum"
          :page-size="filterForm.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </SectionCard>

    <el-dialog v-model="detailVisible" title="课程详情" width="1000px">
      <div v-loading="detailLoading" class="course-detail">
        <template v-if="currentCourse">
          <DetailMetricsGrid
            :items="[
              { label: '课程标题', value: currentCourse.title || '-' },
              { label: '副标题', value: currentCourse.subtitle || '-' },
              { label: '所属用户', value: getOwnerName(currentCourse) },
              { label: '用户 ID', value: getOwnerId(currentCourse) },
            ]"
          />
          <DetailMetricsGrid
            :items="[
              { label: '等级', value: getLevelText(currentCourse.level) },
              { label: '价格', value: formatPrice(currentCourse.price) },
              { label: '评分人数', value: currentCourse.ratingCount ?? '-' },
              { label: '总时长', value: formatDuration(currentCourse.totalDuration) },
            ]"
          />
          <DetailMetricsGrid
            :items="[
              { label: '章节数', value: chapterCount },
              { label: '视频数', value: videoCount },
              { label: '文章数', value: articleCount },
              { label: '试看章节', value: currentCourse.chapters?.filter((item) => item.isFreePreview === 1).length || 0 },
            ]"
          />
          <div class="course-detail__item">
            <span>课程描述</span>
            <p>{{ currentCourse.description || '-' }}</p>
          </div>
          <div class="course-detail__toolbar">
            <el-button type="primary" @click="handleOpenChapter(currentCourse)">新增章节</el-button>
            <el-button type="warning" @click="goToChapters(currentCourse.id || '')">章节内容管理</el-button>
          </div>

          <el-tabs v-model="activeTab">
            <el-tab-pane label="章节内容" name="chapters">
              <div v-if="currentCourse.chapters?.length" class="course-detail__chapter-list">
                <div
                  v-for="(chapter, chapterIndex) in currentCourse.chapters"
                  :key="String(chapter.id)"
                  class="course-detail__chapter-card"
                >
                  <div class="course-detail__chapter-head">
                    <div class="course-detail__chapter-title">
                      <div class="course-detail__chapter-badge">章节 {{ chapter.sortOrder ?? chapterIndex + 1 }}</div>
                      <strong>{{ chapter.chapterTitle }}</strong>
                      <p>{{ chapter.chapterDesc || '-' }}</p>
                    </div>
                    <div class="course-detail__chapter-actions">
                      <el-tag effect="light">{{ chapter.videoCount ?? chapter.videos?.length ?? 0 }} 个视频</el-tag>
                      <el-tag :type="chapter.isFreePreview === 1 ? 'success' : 'info'" effect="light">
                        {{ chapter.isFreePreview === 1 ? '试看章节' : '普通章节' }}
                      </el-tag>
                      <el-button type="primary" link @click="goToChapters(currentCourse.id || '')">编辑内容</el-button>
                    </div>
                  </div>
                  <div v-if="chapter.videos?.length" class="course-detail__video-list">
                    <div
                      v-for="(video, videoIndex) in chapter.videos"
                      :key="String(video.id)"
                      class="course-detail__video-item"
                    >
                      <div class="course-detail__video-head">
                        <div class="course-detail__video-title">
                          <span class="course-detail__video-badge">视频 {{ video.sortOrder ?? videoIndex + 1 }}</span>
                          <strong>{{ video.videoTitle }}</strong>
                        </div>
                        <div class="course-detail__video-tags">
                          <el-tag effect="light">视频</el-tag>
                          <el-tag :type="video.isFreePreview === 1 ? 'success' : 'info'" effect="light">
                            {{ video.isFreePreview === 1 ? '试看' : '非试看' }}
                          </el-tag>
                        </div>
                      </div>
                      <p>{{ video.videoDesc || '-' }}</p>
                      <div class="course-detail__video-meta">
                        <span>时长：{{ formatDuration(video.duration) }}</span>
                        <span>分辨率：{{ video.resolution || '-' }}</span>
                        <span>播放：{{ video.playCount ?? 0 }}</span>
                        <span>大小：{{ formatFileSize(video.fileSize) }}</span>
                      </div>
                    </div>
                  </div>
                  <p v-else class="course-detail__empty">暂无视频</p>

                  <div v-if="getArticlesForChapter(chapter).length" class="course-detail__article-list">
                    <div
                      v-for="article in getArticlesForChapter(chapter)"
                      :key="String(article.id)"
                      class="course-detail__article-item"
                    >
                      <div class="course-detail__article-head">
                        <div>
                          <span class="course-detail__article-badge">文字教程 {{ article.sortOrder ?? 0 }}</span>
                          <strong>{{ article.title }}</strong>
                        </div>
                        <el-tag effect="light">{{ article.wordCount ?? 0 }} 字</el-tag>
                      </div>
                      <p>{{ article.content || '-' }}</p>
                      <div class="course-detail__article-meta">
                        <span>{{ article.readTime ?? 0 }} 分钟</span>
                        <span>{{ article.viewCount ?? 0 }} 次阅读</span>
                      </div>
                    </div>
                  </div>
                  <p v-else class="course-detail__empty">暂无文字教程</p>
                </div>
              </div>
              <p v-else class="course-detail__empty">暂无章节</p>
            </el-tab-pane>
          </el-tabs>
        </template>
      </div>
    </el-dialog>

    <el-dialog v-model="chapterVisible" title="新增章节" width="640px">
      <div class="course-form__grid course-form__grid--single">
        <el-input v-model="chapterForm.chapterTitle" placeholder="章节标题" />
        <el-input v-model="chapterForm.chapterDesc" placeholder="章节描述" />
        <el-input-number v-model="chapterForm.sortOrder" :min="0" controls-position="right" />
        <el-select v-model="chapterForm.isFreePreview" placeholder="是否试看">
          <el-option :value="1" label="是" />
          <el-option :value="0" label="否" />
        </el-select>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="chapterVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmitChapter">确认创建</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
