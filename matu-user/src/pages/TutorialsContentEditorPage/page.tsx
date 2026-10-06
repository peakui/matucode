import { ArrowLeftOutlined, PlusOutlined, FileTextOutlined, VideoCameraOutlined, EditOutlined, UploadOutlined, DeleteOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Col, Empty, Form, Input, InputNumber, Modal, Progress, Row, Select, Space, Spin, Switch, Tabs, Tag, Upload, message } from 'antd'
import MDEditor from '@uiw/react-md-editor'
import SparkMD5 from 'spark-md5'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { createArticle, createChapter, createVideo, deleteArticle, deleteVideo, getCourseDetail, initCourseVideoUpload, completeCourseVideoUpload, updateArticle, updateVideo } from '../../api/course'
import { uploadChunk, uploadFile } from '../../api/file'
import type { CourseArticleVO, CourseVideoVO, CreateArticleRequest, CreateChapterRequest, CreateVideoRequest, CourseDetailVO, UpdateArticleRequest, UpdateVideoRequest } from '../../api/type/courseTypings'
import { useAppSelector } from '../../store/hooks'
import { canPublishCourse } from '../../utils/permissions'
import './page.scss'

type FileLike = File & { uid?: string }

type ChapterFormValues = {
  chapterTitle: string
  chapterDesc?: string
  sortOrder?: number
  isFreePreview?: boolean
}

type ArticleFormValues = {
  chapterId?: string | number
  title: string
  content: string
  sortOrder?: number
  status?: number
}

type VideoFormValues = {
  chapterId?: string | number
  videoTitle: string
  videoDesc?: string
  coverUrl?: string
  resolution?: string
  sortOrder?: number
  isFreePreview?: boolean
  videoUrl?: string
}

type ContentModalMode = 'chapter' | 'article' | 'video'
type VideoInputMode = 'url' | 'upload'
type ContentEditMode = 'create' | 'edit'

const CHUNK_SIZE = 1 * 1024 * 1024
const CHUNK_RETRY_LIMIT = 3

const getAxiosErrorMessage = (error: unknown, fallback: string) => {
  if (error instanceof Error && error.message) return error.message
  return fallback
}

const formatDuration = (totalDuration?: number) => {
  if (!totalDuration || totalDuration <= 0) return '时长待完善'
  const hours = Math.floor(totalDuration / 3600)
  const minutes = Math.floor((totalDuration % 3600) / 60)
  const seconds = totalDuration % 60
  if (hours > 0) return `${hours} 小时 ${minutes} 分钟 ${seconds} 秒`
  if (minutes > 0) return `${minutes} 分钟 ${seconds} 秒`
  return `${seconds} 秒`
}

const normalizeFileUrl = (value?: string) => value?.trim() || ''

const resolveFileUrl = (payload: { url?: string; fileUrl?: string; fullUrl?: string; downloadUrl?: string }) => {
  return payload.url || payload.fileUrl || payload.fullUrl || payload.downloadUrl || ''
}

const getChapterLessons = (course?: CourseDetailVO, chapterId?: string | number) => {
  const videos = (course?.chapters || []).find((chapter) => String(chapter.id) === String(chapterId))?.videos || []
  const articles = (course?.articles || []).filter((article) => String(article.chapterId || '') === String(chapterId || ''))
  return {
    videos: [...videos].sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0)),
    articles: [...articles].sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0)),
  }
}

const getSortedChapters = (course?: CourseDetailVO) => {
  return [...(course?.chapters || [])].sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
}

const getNextSortOrder = (items: Array<{ sortOrder?: number }>) => {
  const maxSort = items.reduce((max, item) => Math.max(max, item.sortOrder || 0), 0)
  return maxSort > 0 ? maxSort + 1 : items.length + 1
}

const fileToMd5 = async (file: File) => {
  const spark = new SparkMD5.ArrayBuffer()
  const chunkSize = 2 * 1024 * 1024
  let offset = 0

  while (offset < file.size) {
    const chunk = file.slice(offset, offset + chunkSize)
    const buffer = await chunk.arrayBuffer()
    spark.append(buffer)
    offset += chunkSize
  }

  return spark.end()
}

export function TutorialsContentEditorPage() {
  const navigate = useNavigate()
  const { id } = useParams()
  const userInfo = useAppSelector((state) => state.auth.userInfo)
  const canManage = canPublishCourse(userInfo)
  const [course, setCourse] = useState<CourseDetailVO | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [modalOpen, setModalOpen] = useState(false)
  const [modalMode, setModalMode] = useState<ContentModalMode>('chapter')
  const [contentEditMode, setContentEditMode] = useState<ContentEditMode>('create')
  const [editingVideo, setEditingVideo] = useState<CourseVideoVO | null>(null)
  const [editingArticle, setEditingArticle] = useState<CourseArticleVO | null>(null)
  const [videoInputMode, setVideoInputMode] = useState<VideoInputMode>('url')
  const [chapterForm] = Form.useForm<ChapterFormValues>()
  const [articleForm] = Form.useForm<ArticleFormValues>()
  const [videoForm] = Form.useForm<VideoFormValues>()
  const [articleContent, setArticleContent] = useState('')
  const [uploadingVideo, setUploadingVideo] = useState(false)
  const [videoProgress, setVideoProgress] = useState('')
  const [videoProgressPercent, setVideoProgressPercent] = useState(0)
  const [videoFileUploading, setVideoFileUploading] = useState(false)
  const [videoFileName, setVideoFileName] = useState('')
  const [videoCoverUploading, setVideoCoverUploading] = useState(false)
  const [videoCoverPreview, setVideoCoverPreview] = useState('')
  const [selectedVideoFile, setSelectedVideoFile] = useState<FileLike | null>(null)
  const [selectedVideoFileMd5, setSelectedVideoFileMd5] = useState('')

  const courseId = id?.trim() || ''
  const sortedChapters = useMemo(() => getSortedChapters(course || undefined), [course])

  const loadCourse = useCallback(async () => {
    if (!courseId) return
    try {
      setLoading(true)
      const detail = await getCourseDetail(courseId)
      setCourse(detail)
    } catch (error) {
      console.error('load course content error:', error)
      message.error('课程内容加载失败，请稍后重试')
      navigate('/tutorials/manage', { replace: true })
    } finally {
      setLoading(false)
    }
  }, [courseId, navigate])

  const restorePageScroll = useCallback(() => {
    document.body.style.overflow = ''
    document.body.style.width = ''
    document.documentElement.style.overflow = ''
  }, [])

  const handleCloseModal = useCallback(() => {
    setModalOpen(false)
    window.setTimeout(restorePageScroll, 0)
  }, [restorePageScroll])

  useEffect(() => {
    if (!canManage) {
      message.warning('你没有课程管理权限，请先完成讲师认证或使用管理员账号')
      navigate('/tutorials', { replace: true })
      return
    }

    void loadCourse()
  }, [canManage, loadCourse, navigate])

  useEffect(() => restorePageScroll, [restorePageScroll])

  const openChapterModal = () => {
    const nextSortOrder = getNextSortOrder(sortedChapters)
    chapterForm.resetFields()
    chapterForm.setFieldsValue({ sortOrder: nextSortOrder, isFreePreview: false })
    setModalMode('chapter')
    setModalOpen(true)
  }

  const openArticleModal = (chapterId?: string | number) => {
    const lessons = getChapterLessons(course || undefined, chapterId)
    articleForm.resetFields()
    articleForm.setFieldsValue({ chapterId, sortOrder: getNextSortOrder([...lessons.videos, ...lessons.articles]), status: 1 })
    setArticleContent('')
    setEditingArticle(null)
    setContentEditMode('create')
    setModalMode('article')
    setModalOpen(true)
  }

  const openEditArticleModal = (article: CourseArticleVO) => {
    articleForm.resetFields()
    articleForm.setFieldsValue({ chapterId: article.chapterId, title: article.title || '', sortOrder: article.sortOrder, status: 1 })
    setArticleContent(article.content || '')
    setEditingArticle(article)
    setContentEditMode('edit')
    setModalMode('article')
    setModalOpen(true)
  }

  const openVideoModal = (chapterId?: string | number) => {
    const lessons = getChapterLessons(course || undefined, chapterId)
    videoForm.resetFields()
    videoForm.setFieldsValue({ chapterId, sortOrder: getNextSortOrder([...lessons.videos, ...lessons.articles]), isFreePreview: false })
    setSelectedVideoFile(null)
    setSelectedVideoFileMd5('')
    setVideoProgress('')
    setVideoProgressPercent(0)
    setVideoFileName('')
    setVideoCoverPreview('')
    setEditingVideo(null)
    setContentEditMode('create')
    setVideoInputMode('url')
    setModalMode('video')
    setModalOpen(true)
  }

  const openEditVideoModal = (video: CourseVideoVO) => {
    videoForm.resetFields()
    videoForm.setFieldsValue({
      chapterId: video.chapterId,
      videoTitle: video.videoTitle || '',
      videoDesc: video.videoDesc || '',
      coverUrl: video.coverUrl || '',
      resolution: video.resolution || '',
      sortOrder: video.sortOrder,
      isFreePreview: video.isFreePreview === 1,
      videoUrl: video.videoUrl || '',
    })
    setSelectedVideoFile(null)
    setSelectedVideoFileMd5('')
    setVideoProgress('')
    setVideoProgressPercent(0)
    setVideoFileName(video.videoUrl ? '已上传视频文件' : '')
    setVideoCoverPreview(video.coverUrl || '')
    setEditingVideo(video)
    setContentEditMode('edit')
    setVideoInputMode('url')
    setModalMode('video')
    setModalOpen(true)
  }

  const handleVideoFileUpload = (file: File) => {
    void (async () => {
      try {
        setVideoFileUploading(true)
        const uploadedFile = await uploadFile(file, { bizType: 'course-video', folder: 'course-video' })
        const videoUrl = resolveFileUrl(uploadedFile)

        if (!videoUrl) {
          throw new Error('上传成功，但未返回可用的视频地址')
        }

        setVideoFileName(file.name)
        videoForm.setFieldValue('videoUrl', videoUrl)
        message.success('视频上传成功')
      } catch (error) {
        console.error('upload video file error:', error)
        message.error('视频上传失败，请稍后重试')
      } finally {
        setVideoFileUploading(false)
      }
    })()

    return false
  }

  const handleVideoCoverUpload = (file: File) => {
    void (async () => {
      try {
        setVideoCoverUploading(true)
        const uploadedFile = await uploadFile(file, { bizType: 'course-video-cover', folder: 'course-video-cover' })
        const coverUrl = resolveFileUrl(uploadedFile)

        if (!coverUrl) {
          throw new Error('上传成功，但未返回可用的视频封面地址')
        }

        setVideoCoverPreview(coverUrl)
        videoForm.setFieldValue('coverUrl', coverUrl)
        message.success('视频封面上传成功')
      } catch (error) {
        console.error('upload video cover error:', error)
        message.error('视频封面上传失败，请稍后重试')
      } finally {
        setVideoCoverUploading(false)
      }
    })()

    return false
  }

  const handleCreateChapter = async (values: ChapterFormValues) => {
    if (!courseId) return
    try {
      setSaving(true)
      const payload: CreateChapterRequest = {
        courseId,
        chapterTitle: values.chapterTitle.trim(),
        chapterDesc: values.chapterDesc?.trim() || undefined,
        sortOrder: values.sortOrder,
        isFreePreview: values.isFreePreview ? 1 : 0,
      }
      await createChapter(payload)
      message.success('章节创建成功')
      handleCloseModal()
      await loadCourse()
    } catch (error) {
      console.error('create chapter error:', error)
      message.error(getAxiosErrorMessage(error, '章节创建失败，请稍后重试'))
    } finally {
      setSaving(false)
    }
  }

  const handleCreateArticle = async () => {
    if (!courseId) return
    try {
      const values = await articleForm.validateFields()
      const articleTitle = values.title?.trim()
      const nextArticleContent = articleContent.trim()
      if (!articleTitle) {
        message.warning('请输入图文标题')
        return
      }
      if (!nextArticleContent) {
        message.warning('请输入正文')
        return
      }
      setSaving(true)
      if (contentEditMode === 'edit' && editingArticle?.id != null) {
        const payload: UpdateArticleRequest = {
          chapterId: values.chapterId,
          title: articleTitle,
          content: nextArticleContent,
          sortOrder: values.sortOrder,
          status: values.status,
        }
        await updateArticle(editingArticle.id, payload)
        message.success('图文教程更新成功')
      } else {
        const payload: CreateArticleRequest = {
          courseId,
          chapterId: values.chapterId,
          title: articleTitle,
          content: nextArticleContent,
          sortOrder: values.sortOrder,
          status: values.status,
        }
        await createArticle(payload)
        message.success('图文教程创建成功')
      }
      handleCloseModal()
      await loadCourse()
    } catch (error) {
      if (error instanceof Error && error.message.includes('validation')) return
      console.error('create article error:', error)
      message.error(getAxiosErrorMessage(error, '图文教程创建失败，请稍后重试'))
    } finally {
      setSaving(false)
    }
  }

  const uploadVideoChunks = async (file: FileLike, uploadedChunks: number[] = [], fileId?: string | number) => {
    const totalChunks = Math.ceil(file.size / CHUNK_SIZE)
    const uploadedChunkSet = new Set(uploadedChunks)
    setVideoProgressPercent(Math.floor((uploadedChunkSet.size / totalChunks) * 90))

    for (let chunkNo = 1; chunkNo <= totalChunks; chunkNo += 1) {
      if (uploadedChunkSet.has(chunkNo)) continue
      const start = (chunkNo - 1) * CHUNK_SIZE
      const end = Math.min(file.size, start + CHUNK_SIZE)
      const chunkFile = file.slice(start, end)
      let failure: unknown
      for (let attempt = 0; attempt < CHUNK_RETRY_LIMIT; attempt += 1) {
        try {
          await uploadChunk({ fileId: fileId || '', chunkNo, file: chunkFile })
          failure = undefined
          break
        } catch (error) {
          failure = error
          if (attempt < CHUNK_RETRY_LIMIT - 1) await new Promise((resolve) => window.setTimeout(resolve, 500 * (attempt + 1)))
        }
      }
      if (failure) throw failure
      uploadedChunkSet.add(chunkNo)
      setVideoProgress(`已上传 ${uploadedChunkSet.size}/${totalChunks} 分片`)
      setVideoProgressPercent(Math.floor((uploadedChunkSet.size / totalChunks) * 90))
    }
    setVideoProgress('分片上传完成')
    setVideoProgressPercent(90)
  }

  const handleUploadVideo = async (values: VideoFormValues) => {
    if (!courseId || !values.chapterId || !selectedVideoFile) return
    try {
      setSaving(true)
      setUploadingVideo(true)
      setVideoProgressPercent(0)
      setVideoProgress('正在计算文件 MD5...')
      const fileMd5 = selectedVideoFileMd5 || await fileToMd5(selectedVideoFile)
      setSelectedVideoFileMd5(fileMd5)
      setVideoProgressPercent(5)

      setVideoProgress('正在初始化上传...')
      const initResult = await getCourseDetail(courseId)
      const chapter = initResult.chapters?.find((item) => String(item.id) === String(values.chapterId))
      if (!chapter) throw new Error('未找到对应章节')

      const initData = await initCourseVideoUpload({
        chapterId: values.chapterId,
        originalName: selectedVideoFile.name,
        fileType: selectedVideoFile.type,
        fileSize: selectedVideoFile.size,
        fileMd5,
        chunkSize: CHUNK_SIZE,
        chunkCount: Math.ceil(selectedVideoFile.size / CHUNK_SIZE),
      })

      await uploadVideoChunks(selectedVideoFile, initData.uploadedChunks || [], initData.fileId)

      setVideoProgress('正在完成上传...')
      setVideoProgressPercent(95)
      await completeCourseVideoUpload({
        chapterId: values.chapterId,
        fileId: initData.fileId || '',
        videoTitle: values.videoTitle.trim(),
        videoDesc: values.videoDesc?.trim() || undefined,
        coverUrl: normalizeFileUrl(values.coverUrl) || undefined,
        resolution: values.resolution?.trim() || undefined,
        sortOrder: values.sortOrder,
        isFreePreview: values.isFreePreview ? 1 : 0,
      })

      setVideoProgressPercent(100)
      message.success('视频创建成功')
      handleCloseModal()
      await loadCourse()
    } catch (error) {
      console.error('upload video error:', error)
      message.error(getAxiosErrorMessage(error, '视频上传失败，请稍后重试'))
    } finally {
      setSaving(false)
      setUploadingVideo(false)
      setVideoProgress('')
      setVideoProgressPercent(0)
    }
  }

  const handleCreateVideo = async () => {
    if (!courseId) return
    const values = await videoForm.validateFields()
    if (videoInputMode === 'upload') {
      await handleUploadVideo(values)
      return
    }

    try {
      setSaving(true)
      if (contentEditMode === 'edit' && editingVideo?.id != null) {
        const payload: UpdateVideoRequest = {
          chapterId: values.chapterId,
          videoTitle: values.videoTitle.trim(),
          videoDesc: values.videoDesc?.trim() || undefined,
          videoUrl: normalizeFileUrl(values.videoUrl) || undefined,
          coverUrl: normalizeFileUrl(values.coverUrl) || undefined,
          resolution: values.resolution?.trim() || undefined,
          sortOrder: values.sortOrder,
          isFreePreview: values.isFreePreview ? 1 : 0,
          status: 1,
        }
        await updateVideo(editingVideo.id, payload)
        message.success('视频更新成功')
      } else {
        const payload: CreateVideoRequest = {
          chapterId: values.chapterId!,
          videoTitle: values.videoTitle.trim(),
          videoDesc: values.videoDesc?.trim() || undefined,
          videoUrl: normalizeFileUrl(values.videoUrl),
          coverUrl: normalizeFileUrl(values.coverUrl) || undefined,
          resolution: values.resolution?.trim() || undefined,
          sortOrder: values.sortOrder,
          isFreePreview: values.isFreePreview ? 1 : 0,
        }
        await createVideo(payload)
        message.success('视频创建成功')
      }
      handleCloseModal()
      await loadCourse()
    } catch (error) {
      console.error('create video error:', error)
      message.error(getAxiosErrorMessage(error, '视频创建失败，请稍后重试'))
    } finally {
      setSaving(false)
    }
  }

  const handleDeleteVideo = (video: CourseVideoVO) => {
    if (video.id == null) return
    Modal.confirm({
      title: '确认删除视频？',
      content: video.videoTitle || '该视频会被软删除，删除后列表将刷新。',
      okText: '删除',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: async () => {
        await deleteVideo(video.id!)
        message.success('视频删除成功')
        await loadCourse()
      },
    })
  }

  const handleDeleteArticle = (article: CourseArticleVO) => {
    if (article.id == null) return
    Modal.confirm({
      title: '确认删除图文教程？',
      content: article.title || '该图文教程会被软删除，删除后列表将刷新。',
      okText: '删除',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: async () => {
        await deleteArticle(article.id!)
        message.success('图文教程删除成功')
        await loadCourse()
      },
    })
  }

  const chapterOptions = sortedChapters.map((chapter) => ({ label: chapter.chapterTitle || '未命名章节', value: chapter.id }))

  if (!canManage) {
    return null
  }

  return (
    <Space direction="vertical" size={20} className="full-width tutorials-content-page">
      <Card className="content-card tutorials-content-hero" variant="borderless">
        <Space direction="vertical" size={16} className="full-width">
          <Space wrap>
            <Button icon={<ArrowLeftOutlined />} onClick={() => navigate(`/detail/course/${courseId}`)}>
              返回课程详情
            </Button>
            <Button icon={<EditOutlined />} onClick={() => navigate(`/tutorials/editor/${courseId}`)}>
              编辑基础信息
            </Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={openChapterModal}>
              新增章节
            </Button>
          </Space>
          <div>
            <div className="channel-hero__label">课程章节管理</div>
            <h2>{course?.title || '未命名课程'}</h2>
            <p>可以在这里维护课程章节下的视频和图文内容，支持新增、编辑和删除。</p>
          </div>
        </Space>
      </Card>

      <Alert type="info" showIcon message="课程内容维护" description="章节、视频、图文均可在这里修改。" />

      <Card className="content-card tutorials-content-card" variant="borderless">
        {loading ? (
          <div className="tutorials-content-loading"><Spin size="large" /></div>
        ) : course ? (
          <Space direction="vertical" size={16} className="full-width">
            {sortedChapters.length ? sortedChapters.map((chapter, chapterIndex) => {
              const lessons = getChapterLessons(course, chapter.id)
              return (
                <Card key={String(chapter.id ?? chapterIndex)} className="tutorials-content-chapter" variant="outlined">
                  <div className="tutorials-content-chapter__head">
                    <div className="tutorials-content-chapter__title">
                      <div className="channel-hero__label">第 {chapterIndex + 1} 章</div>
                      <h3>{chapter.chapterTitle || '未命名章节'}</h3>
                      <p>{chapter.chapterDesc || '暂无章节描述'}</p>
                    </div>
                    <Space wrap className="tutorials-content-chapter__actions">
                      <Tag color="blue">{lessons.videos.length + lessons.articles.length} 个内容</Tag>
                      {chapter.isFreePreview === 1 ? <Tag color="green">可试看</Tag> : null}
                      <Button htmlType="button" icon={<PlusOutlined />} onClick={() => openVideoModal(chapter.id)}>添加视频</Button>
                      <Button htmlType="button" icon={<FileTextOutlined />} onClick={() => openArticleModal(chapter.id)}>添加图文</Button>
                    </Space>
                  </div>

                  {lessons.videos.length || lessons.articles.length ? (
                    <div className="tutorials-content-list">
                      {lessons.videos.map((video) => (
                        <div key={String(video.id)} className="tutorials-content-item">
                          <div className="tutorials-content-item__main">
                            <strong><VideoCameraOutlined /> {video.videoTitle || '未命名视频'}</strong>
                            <p>{video.videoDesc || '暂无视频描述'}</p>
                          </div>
                          <Space wrap className="tutorials-content-item__meta">
                            <Tag color="purple">视频</Tag>
                            <Tag>{formatDuration(video.duration)}</Tag>
                            <Button size="small" icon={<EditOutlined />} onClick={() => openEditVideoModal(video)}>编辑</Button>
                            <Button size="small" danger icon={<DeleteOutlined />} onClick={() => handleDeleteVideo(video)}>删除</Button>
                          </Space>
                        </div>
                      ))}
                      {lessons.articles.map((article) => (
                        <div key={String(article.id)} className="tutorials-content-item">
                          <div className="tutorials-content-item__main">
                            <strong><FileTextOutlined /> {article.title || '未命名图文教程'}</strong>
                            <p>{article.content ? article.content.slice(0, 120) : '暂无正文内容'}</p>
                          </div>
                          <Space wrap className="tutorials-content-item__meta">
                            <Tag color="gold">图文</Tag>
                            <Tag>{article.readTime || 0} 分钟阅读</Tag>
                            <Button size="small" icon={<EditOutlined />} onClick={() => openEditArticleModal(article)}>编辑</Button>
                            <Button size="small" danger icon={<DeleteOutlined />} onClick={() => handleDeleteArticle(article)}>删除</Button>
                          </Space>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <Empty description="该章节暂未添加视频或图文内容" />
                  )}
                </Card>
              )
            }) : (
              <Empty description="暂无章节，先新增一个章节吧" />
            )}
          </Space>
        ) : null}
      </Card>

      <Modal
        title={modalMode === 'chapter' ? '新增章节' : modalMode === 'article' ? `${contentEditMode === 'edit' ? '编辑' : '新增'}图文教程` : `${contentEditMode === 'edit' ? '编辑' : '新增'}视频`}
        open={modalOpen}
        onCancel={handleCloseModal}
        afterOpenChange={(open) => {
          if (!open) restorePageScroll()
        }}
        destroyOnHidden
        onOk={modalMode === 'chapter' ? () => void chapterForm.submit() : modalMode === 'article' ? () => void handleCreateArticle() : () => void handleCreateVideo()}
        okText={contentEditMode === 'edit' ? '保存' : '新增'}
        cancelText="取消"
        confirmLoading={saving || uploadingVideo}
        width={modalMode === 'video' ? 880 : 640}
      >
        {modalMode === 'chapter' ? (
          <Form form={chapterForm} layout="vertical" onFinish={(values) => void handleCreateChapter(values)} initialValues={{ isFreePreview: false }}>
            <Form.Item name="chapterTitle" label="章节标题" rules={[{ required: true, message: '请输入章节标题' }]}>
              <Input placeholder="请输入章节标题" />
            </Form.Item>
            <Form.Item name="chapterDesc" label="章节描述">
              <Input.TextArea rows={4} placeholder="请输入章节描述" />
            </Form.Item>
            <Row gutter={16}>
              <Col span={12}>
                <Form.Item name="sortOrder" label="排序值">
                  <InputNumber className="full-width" min={1} />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item name="isFreePreview" label="是否试看" valuePropName="checked">
                  <Switch checkedChildren="是" unCheckedChildren="否" />
                </Form.Item>
              </Col>
            </Row>
          </Form>
        ) : modalMode === 'article' ? (
          <Form form={articleForm} layout="vertical" initialValues={{ status: 1 }}>
            <Form.Item name="chapterId" label="所属章节" rules={[{ required: true, message: '请选择所属章节' }]}>
              <Select options={chapterOptions} placeholder="请选择章节" allowClear />
            </Form.Item>
            <Form.Item name="title" label="图文标题" rules={[{ required: true, message: '请输入图文标题' }]}>
              <Input placeholder="请输入图文标题" />
            </Form.Item>
            <Form.Item label="正文" required>
              <div className="tutorials-content-md-editor" data-color-mode="light">
                <MDEditor value={articleContent} onChange={(value) => setArticleContent(value || '')} preview="edit" height={420} />
              </div>
            </Form.Item>
            <Row gutter={16}>
              <Col span={12}>
                <Form.Item name="sortOrder" label="排序值">
                  <InputNumber className="full-width" min={1} />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item name="status" label="状态">
                  <InputNumber className="full-width" min={0} max={1} />
                </Form.Item>
              </Col>
            </Row>
          </Form>
        ) : (
          <Form form={videoForm} layout="vertical" initialValues={{ isFreePreview: false }}>
            <Form.Item name="chapterId" label="所属章节" rules={[{ required: true, message: '请选择所属章节' }]}>
              <Select options={chapterOptions} placeholder="请选择章节" allowClear />
            </Form.Item>
            {contentEditMode === 'create' ? (
              <Tabs
                activeKey={videoInputMode}
                onChange={(key) => setVideoInputMode(key as VideoInputMode)}
                items={[
                  { key: 'url', label: '已有视频地址' },
                  { key: 'upload', label: '上传本地视频' },
                ]}
              />
            ) : null}
            {videoInputMode === 'url' ? (
              contentEditMode === 'edit' ? (
                <Form.Item name="videoUrl" label="视频文件">
                  <div className="tutorials-content-upload-panel">
                    <Upload accept="video/*" showUploadList={false} beforeUpload={handleVideoFileUpload} maxCount={1}>
                      <Button htmlType="button" icon={<UploadOutlined />} loading={videoFileUploading}>
                        {videoFileName ? '重新上传视频' : '上传视频'}
                      </Button>
                    </Upload>
                    {videoFileName ? <Tag color="blue">{videoFileName}</Tag> : null}
                  </div>
                </Form.Item>
              ) : (
                <Form.Item name="videoUrl" label="视频地址" rules={[{ required: true, message: '请输入视频地址' }]}>
                  <Input placeholder="请输入视频地址" />
                </Form.Item>
              )
            ) : (
              <div className="tutorials-content-upload-panel">
                <Upload
                  accept="video/*"
                  beforeUpload={(file) => {
                    setSelectedVideoFile(file as FileLike)
                    void fileToMd5(file).then((md5) => setSelectedVideoFileMd5(md5))
                    return false
                  }}
                  maxCount={1}
                >
                  <Button icon={<UploadOutlined />}>{selectedVideoFile ? '重新选择视频' : '选择视频文件'}</Button>
                </Upload>
                {selectedVideoFile ? <Tag color="blue">{selectedVideoFile.name}</Tag> : null}
                {uploadingVideo || videoProgress ? (
                  <div className="tutorials-content-upload-progress">
                    <Progress percent={videoProgressPercent} status={videoProgressPercent >= 100 ? 'success' : 'active'} />
                    {videoProgress ? <div className="tutorials-content-upload-status">{videoProgress}</div> : null}
                  </div>
                ) : null}
              </div>
            )}
            <Form.Item name="videoTitle" label="视频标题" rules={[{ required: true, message: '请输入视频标题' }]}>
              <Input placeholder="请输入视频标题" />
            </Form.Item>
            <Form.Item name="videoDesc" label="视频描述">
              <Input.TextArea rows={4} placeholder="请输入视频描述" />
            </Form.Item>
            <Form.Item label="视频封面">
              <div className="tutorials-video-cover-upload">
                {videoCoverPreview ? <img src={videoCoverPreview} alt="视频封面预览" /> : null}
                <Upload accept="image/*" showUploadList={false} beforeUpload={handleVideoCoverUpload} maxCount={1}>
                  <Button htmlType="button" icon={<UploadOutlined />} loading={videoCoverUploading}>
                    {videoCoverPreview ? '重新上传封面' : '上传视频封面'}
                  </Button>
                </Upload>
              </div>
            </Form.Item>
            <Form.Item name="resolution" label="分辨率">
              <Input placeholder="例如 1080p" />
            </Form.Item>
            <Row gutter={16}>
              <Col span={12}>
                <Form.Item name="sortOrder" label="排序值">
                  <InputNumber className="full-width" min={1} />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item name="isFreePreview" label="是否试看" valuePropName="checked">
                  <Switch checkedChildren="是" unCheckedChildren="否" />
                </Form.Item>
              </Col>
            </Row>
          </Form>
        )}
      </Modal>
    </Space>
  )
}
