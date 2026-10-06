<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import SectionCard from '@/components/SectionCard/index.vue'
import ChunkVideoUploadField from '@/components/ChunkVideoUploadField/index.vue'
import ImageUploadField from '@/components/ImageUploadField/index.vue'
import {
  createArticle,
  createChapter,
  createVideo,
  deleteArticle,
  deleteChapter,
  deleteVideo,
  getCourseDetail,
  updateArticle,
  updateChapter,
  updateVideo,
} from '@/api'
import type {
  CourseArticleVO,
  CourseChapterVO,
  CourseDetailVO,
  CourseVideoVO,
  CreateArticleRequest,
  CreateChapterRequest,
  CreateVideoRequest,
} from '@/api/types'
import './index.scss'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const course = ref<CourseDetailVO | null>(null)
const visible = ref(false)
const videoUploadVisible = ref(false)
const videoUrlVisible = ref(false)
const articleVisible = ref(false)
const isEdit = ref(false)
const isVideoEdit = ref(false)
const isArticleEdit = ref(false)
const currentChapterId = ref<string | number | null>(null)
const currentVideoId = ref<string | number | null>(null)
const currentArticleId = ref<string | number | null>(null)
const selectedVideo = ref<CourseVideoVO | null>(null)

const courseId = computed(() => String(route.params.courseId || ''))
const chapterList = computed(() => course.value?.chapters || [])
const unboundArticles = computed(() => (course.value?.articles || []).filter((article) => !article.chapterId))

const chapterForm = ref<CreateChapterRequest>({
  courseId: '',
  chapterTitle: '',
  chapterDesc: '',
  sortOrder: 0,
  isFreePreview: 0,
})

const videoForm = ref<CreateVideoRequest>({
  chapterId: '',
  videoTitle: '',
  videoDesc: '',
  videoUrl: '',
  coverUrl: '',
  duration: 0,
  resolution: '1080p',
  sortOrder: 0,
  isFreePreview: 0,
  fileSize: 0,
})

const articleForm = ref<CreateArticleRequest>({
  courseId: '',
  chapterId: '',
  title: '',
  content: '',
  sortOrder: 0,
  status: 1,
})

const resetForm = () => {
  chapterForm.value = {
    courseId: courseId.value,
    chapterTitle: '',
    chapterDesc: '',
    sortOrder: 0,
    isFreePreview: 0,
  }
}

const resetVideoForm = (chapterId: string | number) => {
  videoForm.value = {
    chapterId,
    videoTitle: '',
    videoDesc: '',
    videoUrl: '',
    coverUrl: '',
    duration: 0,
    resolution: '1080p',
    sortOrder: 0,
    isFreePreview: 0,
    fileSize: 0,
  }
}

const resetArticleForm = (chapterId: string | number) => {
  articleForm.value = {
    courseId: courseId.value,
    chapterId,
    title: '',
    content: '',
    sortOrder: 0,
    status: 1,
  }
}

const loadCourse = async () => {
  if (!courseId.value) return
  loading.value = true
  try {
    course.value = await getCourseDetail(courseId.value)
  } finally {
    loading.value = false
  }
}

const articlesForChapter = (chapter: CourseChapterVO) => {
  const articleMap = new Map<string, CourseArticleVO>()
  ;(chapter.articles || []).forEach((article) => {
    if (article.id) articleMap.set(String(article.id), article)
  })
  ;(course.value?.articles || [])
    .filter((article) => String(article.chapterId || '') === String(chapter.id || ''))
    .forEach((article) => {
      if (article.id) articleMap.set(String(article.id), article)
    })
  return Array.from(articleMap.values()).sort((a, b) => Number(a.sortOrder || 0) - Number(b.sortOrder || 0))
}

const openCreate = () => {
  isEdit.value = false
  currentChapterId.value = null
  resetForm()
  visible.value = true
}

const openEdit = (chapter: CourseChapterVO) => {
  isEdit.value = true
  currentChapterId.value = chapter.id || null
  chapterForm.value = {
    courseId: chapter.courseId || courseId.value,
    chapterTitle: chapter.chapterTitle || '',
    chapterDesc: chapter.chapterDesc || '',
    sortOrder: chapter.sortOrder || 0,
    isFreePreview: chapter.isFreePreview || 0,
  }
  visible.value = true
}

const handleSubmit = async () => {
  if (!courseId.value) return
  submitting.value = true
  try {
    if (isEdit.value && currentChapterId.value) {
      await updateChapter(currentChapterId.value, { ...chapterForm.value })
      ElMessage.success('章节已更新')
    } else {
      await createChapter({ ...chapterForm.value, courseId: courseId.value })
      ElMessage.success('章节已创建')
    }
    visible.value = false
    await loadCourse()
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (chapter: CourseChapterVO) => {
  await ElMessageBox.confirm(`确认删除章节《${chapter.chapterTitle || '未命名章节'}》吗？`, '删除章节', {
    type: 'warning',
  })
  await deleteChapter(chapter.id || '')
  ElMessage.success('章节已删除')
  await loadCourse()
}

const openVideoUpload = (chapter: CourseChapterVO) => {
  currentChapterId.value = chapter.id || null
  selectedVideo.value = null
  videoUploadVisible.value = true
}

const handleLocalVideoUploaded = async (video: CourseVideoVO | null) => {
  selectedVideo.value = video
  if (!video) return
  videoUploadVisible.value = false
  await loadCourse()
}

const openVideoUrlCreate = (chapter: CourseChapterVO) => {
  isVideoEdit.value = false
  currentVideoId.value = null
  currentChapterId.value = chapter.id || null
  resetVideoForm(chapter.id || '')
  videoUrlVisible.value = true
}

const openVideoUrlEdit = (chapter: CourseChapterVO, video: CourseVideoVO) => {
  isVideoEdit.value = true
  currentVideoId.value = video.id || null
  currentChapterId.value = chapter.id || null
  videoForm.value = {
    chapterId: video.chapterId || chapter.id || '',
    videoTitle: video.videoTitle || '',
    videoDesc: video.videoDesc || '',
    videoUrl: video.videoUrl || '',
    coverUrl: video.coverUrl || '',
    duration: video.duration || 0,
    resolution: video.resolution || '1080p',
    sortOrder: video.sortOrder || 0,
    isFreePreview: video.isFreePreview || 0,
    fileSize: video.fileSize || 0,
  }
  videoUrlVisible.value = true
}

const handleSubmitVideoUrl = async () => {
  if (!currentChapterId.value) return
  if (!videoForm.value.videoUrl) {
    ElMessage.warning('请输入视频 URL')
    return
  }
  submitting.value = true
  try {
    if (isVideoEdit.value && currentVideoId.value) {
      await updateVideo(currentVideoId.value, { ...videoForm.value })
      ElMessage.success('视频已更新')
    } else {
      await createVideo({ ...videoForm.value, chapterId: currentChapterId.value })
      ElMessage.success('视频已创建')
    }
    videoUrlVisible.value = false
    await loadCourse()
  } finally {
    submitting.value = false
  }
}

const handleDeleteVideo = async (video: CourseVideoVO) => {
  await ElMessageBox.confirm(`确认删除视频《${video.videoTitle || '未命名视频'}》吗？`, '删除视频', {
    type: 'warning',
  })
  await deleteVideo(video.id || '')
  ElMessage.success('视频已删除')
  await loadCourse()
}

const openArticleCreate = (chapter: CourseChapterVO) => {
  isArticleEdit.value = false
  currentArticleId.value = null
  currentChapterId.value = chapter.id || null
  resetArticleForm(chapter.id || '')
  articleVisible.value = true
}

const openArticleEdit = (chapter: CourseChapterVO, article: CourseArticleVO) => {
  isArticleEdit.value = true
  currentArticleId.value = article.id || null
  currentChapterId.value = chapter.id || null
  articleForm.value = {
    courseId: article.courseId || courseId.value,
    chapterId: article.chapterId || chapter.id || '',
    title: article.title || '',
    content: article.content || '',
    sortOrder: article.sortOrder || 0,
    status: 1,
  }
  articleVisible.value = true
}

const handleSubmitArticle = async () => {
  if (!currentChapterId.value) return
  submitting.value = true
  try {
    if (isArticleEdit.value && currentArticleId.value) {
      await updateArticle(currentArticleId.value, { ...articleForm.value, chapterId: currentChapterId.value })
      ElMessage.success('文字教程已更新')
    } else {
      await createArticle({ ...articleForm.value, courseId: courseId.value, chapterId: currentChapterId.value })
      ElMessage.success('文字教程已创建')
    }
    articleVisible.value = false
    await loadCourse()
  } finally {
    submitting.value = false
  }
}

const handleDeleteArticle = async (article: CourseArticleVO) => {
  await ElMessageBox.confirm(`确认删除文字教程《${article.title || '未命名教程'}》吗？`, '删除文字教程', {
    type: 'warning',
  })
  await deleteArticle(article.id || '')
  ElMessage.success('文字教程已删除')
  await loadCourse()
}

const handleBack = () => {
  void router.push(`/admin/course/edit/${courseId.value}`)
}

onMounted(async () => {
  await loadCourse()
})
</script>

<template>
  <div class="course-workspace-page">
    <div class="filter-action-row">
      <el-button type="primary" @click="openCreate">新增章节</el-button>
    </div>

    <SectionCard :title="course?.title || '课程章节'" description="每个章节下可上传本地视频、提交视频 URL，并编辑文字教程。">
      <div v-loading="loading" class="chapter-list">
        <div v-for="(chapter, index) in chapterList" :key="String(chapter.id)" class="chapter-card">
          <div class="chapter-card__head">
            <div>
              <div class="chapter-card__badge">章节 {{ chapter.sortOrder ?? index + 1 }}</div>
              <strong>{{ chapter.chapterTitle }}</strong>
              <p>{{ chapter.chapterDesc || '-' }}</p>
            </div>
            <div class="chapter-card__actions">
              <el-tag effect="light">{{ chapter.videoCount ?? chapter.videos?.length ?? 0 }} 个视频</el-tag>
              <el-tag effect="light">{{ articlesForChapter(chapter).length }} 篇文字教程</el-tag>
              <el-tag :type="chapter.isFreePreview === 1 ? 'success' : 'info'" effect="light">
                {{ chapter.isFreePreview === 1 ? '试看' : '普通' }}
              </el-tag>
              <el-button type="primary" link @click="openEdit(chapter)">编辑章节</el-button>
              <el-button type="danger" link @click="handleDelete(chapter)">删除章节</el-button>
            </div>
          </div>

          <div class="chapter-card__content">
            <div class="chapter-card__content-section">
              <div class="chapter-card__section-head">
                <div>
                  <span>视频教程</span>
                  <p>支持本地视频断点续传到 OSS，也支持提交外部 URL。</p>
                </div>
                <div>
                  <el-button type="primary" plain @click="openVideoUpload(chapter)">上传本地视频</el-button>
                  <el-button type="primary" @click="openVideoUrlCreate(chapter)">提交 URL</el-button>
                </div>
              </div>

              <div v-if="chapter.videos?.length" class="chapter-card__videos">
                <div v-for="(video, videoIndex) in chapter.videos" :key="String(video.id)" class="video-card">
                  <div class="video-card__head">
                    <div>
                      <span>视频 {{ video.sortOrder ?? videoIndex + 1 }}</span>
                      <strong>{{ video.videoTitle }}</strong>
                    </div>
                    <div class="video-card__actions">
                      <el-tag :type="video.isFreePreview === 1 ? 'success' : 'info'" effect="light">
                        {{ video.isFreePreview === 1 ? '试看' : '非试看' }}
                      </el-tag>
                      <el-button type="primary" link @click="openVideoUrlEdit(chapter, video)">编辑</el-button>
                      <el-button type="danger" link @click="handleDeleteVideo(video)">删除</el-button>
                    </div>
                  </div>
                  <p>{{ video.videoDesc || '-' }}</p>
                  <div class="video-card__meta">
                    <span>时长：{{ video.duration ?? 0 }} 秒</span>
                    <span>分辨率：{{ video.resolution || '-' }}</span>
                    <span>播放：{{ video.playCount ?? 0 }}</span>
                    <span>资源：{{ video.videoUrl || '本地上传' }}</span>
                  </div>
                </div>
              </div>
              <p v-else class="chapter-list__empty">本章节暂无视频教程</p>
            </div>

            <div class="chapter-card__content-section">
              <div class="chapter-card__section-head">
                <div>
                  <span>文字教程</span>
                  <p>文字教程绑定当前章节，与视频教程一起管理。</p>
                </div>
                <el-button type="success" @click="openArticleCreate(chapter)">新增文字教程</el-button>
              </div>

              <div v-if="articlesForChapter(chapter).length" class="chapter-card__articles">
                <div v-for="article in articlesForChapter(chapter)" :key="String(article.id)" class="article-card">
                  <div class="article-card__head">
                    <div>
                      <span>文字教程 {{ article.sortOrder ?? 0 }}</span>
                      <strong>{{ article.title }}</strong>
                    </div>
                    <div class="article-card__actions">
                      <el-tag effect="light">{{ article.wordCount ?? 0 }} 字</el-tag>
                      <el-button type="primary" link @click="openArticleEdit(chapter, article)">编辑</el-button>
                      <el-button type="danger" link @click="handleDeleteArticle(article)">删除</el-button>
                    </div>
                  </div>
                  <p>{{ article.content || '-' }}</p>
                  <div class="article-card__meta">
                    <span>阅读：{{ article.readTime ?? 0 }} 分钟</span>
                    <span>浏览：{{ article.viewCount ?? 0 }}</span>
                  </div>
                </div>
              </div>
              <p v-else class="chapter-list__empty">本章节暂无文字教程</p>
            </div>
          </div>
        </div>

        <p v-if="!chapterList.length" class="chapter-list__empty">暂无章节</p>
      </div>

      <div v-if="unboundArticles.length" class="unbound-articles">
        <strong>未绑定章节的文字教程</strong>
        <p>这些历史文章没有 chapterId，建议编辑后绑定到具体章节。</p>
        <div class="chapter-card__articles">
          <div v-for="article in unboundArticles" :key="String(article.id)" class="article-card">
            <div class="article-card__head">
              <div>
                <span>未绑定</span>
                <strong>{{ article.title }}</strong>
              </div>
              <el-tag effect="light">{{ article.wordCount ?? 0 }} 字</el-tag>
            </div>
            <p>{{ article.content || '-' }}</p>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="chapter-list__footer">
          <el-button @click="handleBack">返回课程编辑</el-button>
          <el-button type="primary" @click="openCreate">新增章节</el-button>
        </div>
      </template>
    </SectionCard>

    <el-dialog v-model="visible" :title="isEdit ? '编辑章节' : '新增章节'" width="640px">
      <el-form :model="chapterForm" label-position="top">
        <el-form-item label="章节标题">
          <el-input v-model="chapterForm.chapterTitle" placeholder="章节标题" size="large" />
        </el-form-item>
        <el-form-item label="章节描述">
          <el-input v-model="chapterForm.chapterDesc" type="textarea" :rows="4" placeholder="章节描述" />
        </el-form-item>
        <div class="chapter-form__grid">
          <el-form-item label="排序">
            <el-input-number v-model="chapterForm.sortOrder" :min="0" controls-position="right" />
          </el-form-item>
          <el-form-item label="是否试看">
            <el-select v-model="chapterForm.isFreePreview" placeholder="是否试看">
              <el-option :value="1" label="是" />
              <el-option :value="0" label="否" />
            </el-select>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="visible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            {{ isEdit ? '保存修改' : '确认创建' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="videoUploadVisible" title="上传本地视频" width="860px">
      <ChunkVideoUploadField
        v-model="selectedVideo"
        :chapter-id="currentChapterId || undefined"
        @update:model-value="handleLocalVideoUploaded"
      />
    </el-dialog>

    <el-dialog v-model="videoUrlVisible" :title="isVideoEdit ? '编辑视频教程' : '提交视频 URL'" width="760px">
      <el-form :model="videoForm" label-position="top" class="content-editor">
        <el-form-item label="视频标题">
          <el-input v-model="videoForm.videoTitle" placeholder="视频标题" size="large" />
        </el-form-item>
        <el-form-item label="视频 URL">
          <el-input v-model="videoForm.videoUrl" placeholder="https://..." size="large" />
        </el-form-item>
        <el-form-item label="视频描述">
          <el-input v-model="videoForm.videoDesc" type="textarea" :rows="3" placeholder="视频描述" />
        </el-form-item>
        <el-form-item label="视频封面">
          <ImageUploadField v-model="videoForm.coverUrl" plus-only />
        </el-form-item>
        <div class="chapter-form__grid">
          <el-form-item label="时长（秒）">
            <el-input-number v-model="videoForm.duration" :min="0" controls-position="right" />
          </el-form-item>
          <el-form-item label="文件大小（字节）">
            <el-input-number v-model="videoForm.fileSize" :min="0" controls-position="right" />
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="videoForm.sortOrder" :min="0" controls-position="right" />
          </el-form-item>
          <el-form-item label="分辨率">
            <el-input v-model="videoForm.resolution" placeholder="如 1080p" />
          </el-form-item>
          <el-form-item label="是否试看">
            <el-select v-model="videoForm.isFreePreview" placeholder="是否试看">
              <el-option :value="1" label="是" />
              <el-option :value="0" label="否" />
            </el-select>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="videoUrlVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmitVideoUrl">
            {{ isVideoEdit ? '保存修改' : '确认提交' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="articleVisible" :title="isArticleEdit ? '编辑文字教程' : '新增文字教程'" width="800px">
      <el-form :model="articleForm" label-position="top" class="content-editor">
        <el-form-item label="教程标题">
          <el-input v-model="articleForm.title" placeholder="文字教程标题" size="large" />
        </el-form-item>
        <div class="chapter-form__grid">
          <el-form-item label="排序">
            <el-input-number v-model="articleForm.sortOrder" :min="0" controls-position="right" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="articleForm.status" placeholder="状态">
              <el-option :value="1" label="发布" />
              <el-option :value="0" label="草稿" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="教程正文">
          <el-input v-model="articleForm.content" type="textarea" :rows="10" placeholder="文字教程正文" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="articleVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmitArticle">
            {{ isArticleEdit ? '保存修改' : '确认创建' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
