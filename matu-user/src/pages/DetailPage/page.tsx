import {
  ArrowLeftOutlined,
  BookOutlined,
  EditOutlined,
  ClockCircleOutlined,
  FileTextOutlined,
  FireOutlined,
  PlayCircleOutlined,
  StarOutlined,
  VideoCameraOutlined,
} from '@ant-design/icons'
import { Alert, Avatar, Button, Card, Col, Divider, Empty, Input, Modal, Rate, Row, Skeleton, Space, Tag, message } from 'antd'
import MDEditor from '@uiw/react-md-editor'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getUserProfile } from '../../api/authProfile'
import { createReview, getCourseDetail, getVideoPlayInfo, listReviews } from '../../api/course'
import { collectInterviewQuestion, getInterviewQuestionDetail, getInterviewQuestionProgress } from '../../api/interview'
import type { CourseArticleVO, CourseChapterVO, CourseDetailVO, CourseReviewVO, CourseVideoVO, VideoPlayVO } from '../../api/type/courseTypings'
import type { InterviewQuestionVO, UserQuestionProgressVO } from '../../api/type/interviewTypings'
import type { AuthProfileVO } from '../../api/type/loginTypings'
import { DEFAULT_AVATAR_URL, getAvatarWithFallback } from '../../utils/avatar'
import { canPublishCourse, canViewInterviewAnswer } from '../../utils/permissions'
import { markdownImageComponents } from '../../utils/markdownImageComponents'
import { useAppSelector } from '../../store/hooks'
import { CourseVideoPlayer } from '../../components/CourseVideoPlayer'
import { CourseStudyTools } from '../../components/CourseStudyTools'
import './page.scss'

type CourseLessonItem = {
  id?: string | number
  chapterId?: string | number
  chapterTitle?: string
  type: 'video' | 'article'
  title: string
  desc?: string
  duration?: number
  readTime?: number
  sortOrder?: number
  source: CourseVideoVO | CourseArticleVO
}

type CourseLessonGroup = {
  key: string
  title: string
  desc?: string
  lessons: CourseLessonItem[]
  chapter?: CourseChapterVO
}

const getLevelText = (level?: number) => {
  if (level === 1) return '入门'
  if (level === 2) return '基础'
  if (level === 3) return '进阶'
  if (level === 4) return '高级'
  return '未分级'
}

const formatCourseDuration = (totalDuration?: number) => {
  if (!totalDuration || totalDuration <= 0) return '时长待完善'
  const hours = Math.floor(totalDuration / 3600)
  const minutes = Math.floor((totalDuration % 3600) / 60)
  const seconds = totalDuration % 60
  if (hours > 0) return `${hours} 小时 ${minutes} 分钟 ${seconds} 秒`
  if (minutes > 0) return `${minutes} 分钟 ${seconds} 秒`
  return `${seconds} 秒`
}

const normalizeRouteId = (value?: string) => value?.trim() || ''

const getCourseArticlesByChapter = (course?: CourseDetailVO, chapterId?: string | number) => {
  return (course?.articles || []).filter((article) => String(article.chapterId || '') === String(chapterId || ''))
}

const getLessonSortValue = (lesson: CourseLessonItem) => lesson.sortOrder ?? Number.MAX_SAFE_INTEGER

const getChapterLessons = (course: CourseDetailVO, chapter: CourseChapterVO): CourseLessonItem[] => {
  const videos = (chapter.videos || []).map<CourseLessonItem>((video) => ({
    id: video.id,
    chapterId: chapter.id,
    chapterTitle: chapter.chapterTitle,
    type: 'video',
    title: video.videoTitle || '未命名视频',
    desc: video.videoDesc,
    duration: video.duration,
    sortOrder: video.sortOrder,
    source: video,
  }))

  const articles = getCourseArticlesByChapter(course, chapter.id).map<CourseLessonItem>((article) => ({
    id: article.id,
    chapterId: chapter.id,
    chapterTitle: chapter.chapterTitle,
    type: 'article',
    title: article.title || '未命名图文教程',
    desc: article.content ? `${article.content.replace(/[-#>*_`]/g, '').slice(0, 72)}...` : undefined,
    readTime: article.readTime,
    sortOrder: article.sortOrder,
    source: article,
  }))

  return [...videos, ...articles].sort((a, b) => getLessonSortValue(a) - getLessonSortValue(b))
}

const getStandaloneLessonItems = (course?: CourseDetailVO) => {
  return (course?.articles || [])
    .filter((article) => !article.chapterId)
    .map<CourseLessonItem>((article) => ({
      id: article.id,
      chapterId: article.chapterId,
      type: 'article',
      title: article.title || '未命名图文教程',
      desc: article.content ? `${article.content.replace(/[-#>*_`]/g, '').slice(0, 72)}...` : undefined,
      readTime: article.readTime,
      sortOrder: article.sortOrder,
      source: article,
    }))
    .sort((a, b) => getLessonSortValue(a) - getLessonSortValue(b))
}

const getCourseLessonGroups = (course: CourseDetailVO): CourseLessonGroup[] => {
  const chapterGroups = (course.chapters || []).map<CourseLessonGroup>((chapter, chapterIndex) => ({
    key: String(chapter.id ?? chapter.chapterTitle ?? chapterIndex),
    title: chapter.chapterTitle || '未命名章节',
    desc: chapter.chapterDesc || '本章节简介正在完善中。',
    lessons: getChapterLessons(course, chapter),
    chapter,
  }))
  const standaloneLessons = getStandaloneLessonItems(course)

  if (!standaloneLessons.length) return chapterGroups

  return [
    ...chapterGroups,
    {
      key: 'standalone-articles',
      title: '独立图文教程',
      desc: '未归属章节的图文内容。',
      lessons: standaloneLessons,
    },
  ]
}

function CourseDetailView({ course, courseId }: { course: CourseDetailVO; courseId: string }) {
  const navigate = useNavigate()
  const userInfo = useAppSelector((state) => state.auth.userInfo)
  const canEditCourse = canPublishCourse(userInfo) && (userInfo?.roles?.includes('ADMIN') || String(userInfo?.userId) === String(course.instructorId))
  const firstLesson = getCourseLessonGroups(course).flatMap((group) => group.lessons)[0]
  const [instructor, setInstructor] = useState<AuthProfileVO | null>(null)
  const [instructorLoading, setInstructorLoading] = useState(false)
  const [reviewOpen, setReviewOpen] = useState(false)
  const [reviewSubmitting, setReviewSubmitting] = useState(false)
  const [reviewLoading, setReviewLoading] = useState(false)
  const [reviewRating, setReviewRating] = useState(5)
  const [reviewContent, setReviewContent] = useState('')
  const [reviews, setReviews] = useState<CourseReviewVO[]>([])
  const chapters = course.chapters || []
  const standaloneArticles = getStandaloneLessonItems(course)
  const instructorId = course.instructorId
  const instructorName = instructor?.nickname?.trim() || instructor?.username?.trim() || (instructorId ? `用户 ${instructorId}` : 'CodeHub 官方教程')
  const instructorDesc = instructor?.signature?.trim() || instructor?.title?.trim() || '持续更新课程内容'
  const instructorAvatar = getAvatarWithFallback(instructor || undefined)

  const loadReviews = async () => {
    try {
      setReviewLoading(true)
      const data = await listReviews(courseId, { pageNum: 1, pageSize: 10 })
      setReviews(data.records || [])
    } catch (error) {
      console.error('load course reviews error:', error)
      message.error('课程评价加载失败，请稍后重试')
    } finally {
      setReviewLoading(false)
    }
  }

  const openReviewModal = () => {
    setReviewRating(5)
    setReviewContent('')
    setReviewOpen(true)
    void loadReviews()
  }

  const handleSubmitReview = async () => {
    try {
      setReviewSubmitting(true)
      await createReview({
        courseId,
        rating: reviewRating,
        content: reviewContent.trim() || undefined,
      })
      message.success('评分成功')
      setReviewContent('')
      await loadReviews()
    } catch (error) {
      console.error('submit course review error:', error)
      message.error('评分失败，请稍后重试')
    } finally {
      setReviewSubmitting(false)
    }
  }

  useEffect(() => {
    if (!instructorId) {
      setInstructor(null)
      return
    }

    let cancelled = false
    const loadInstructor = async () => {
      try {
        setInstructorLoading(true)
        const data = await getUserProfile(instructorId)
        if (!cancelled) {
          setInstructor(data)
        }
      } catch (error) {
        console.error('load course instructor error:', error)
        if (!cancelled) {
          setInstructor(null)
        }
      } finally {
        if (!cancelled) {
          setInstructorLoading(false)
        }
      }
    }

    void loadInstructor()
    return () => {
      cancelled = true
    }
  }, [instructorId])

  return (
    <div className="course-detail-shell">
      <div className="course-floating-rail">
        <button type="button" className="course-floating-action" onClick={openReviewModal}>
          <StarOutlined />
          <strong>评分</strong>
          <span>{course.rating || '暂无'}</span>
        </button>
      </div>
      <Row gutter={[24, 24]} className="detail-page course-detail-page">
        <Col xs={24} lg={18}>
        <Card className="content-card detail-article-card course-hero-card" variant="borderless">
          <div className="course-detail-topbar">
            <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/tutorials')}>
              返回教程列表
            </Button>
            {canEditCourse ? (
              <Space className="course-detail-actions">
                <Button icon={<FileTextOutlined />} onClick={() => navigate(`/tutorials/editor/${courseId}/content`)}>
                  管理章节内容
                </Button>
                <Button type="primary" icon={<EditOutlined />} onClick={() => navigate(`/tutorials/editor/${courseId}`)}>
                  编辑课程
                </Button>
              </Space>
            ) : null}
          </div>
          <div className="course-detail-hero">
            <div className="course-detail-hero__main">
              <div className="course-detail-tags">
                <Tag color={course.isFree === 1 ? 'green' : 'blue'} variant="filled">{course.isFree === 1 ? '免费课程' : '付费课程'}</Tag>
                <Tag color="purple" variant="filled">{getLevelText(course.level)}</Tag>
                {course.rating ? <Tag color="gold" icon={<StarOutlined />}>{course.rating} 分</Tag> : null}
              </div>
              <h1 className="detail-page__title">{course.title || '未命名课程'}</h1>
              <div className="detail-page__meta">{course.subtitle || '系统化教程，按章节循序渐进学习。'}</div>
              <p className="detail-page__content">{course.description || '课程介绍正在完善中，下面可以查看课程章节、视频课和图文教程。'}</p>
              <Space wrap size={16} className="course-basic-meta">
                <span><BookOutlined /> {chapters.length || course.chapterCount || 0} 个章节</span>
                <span><PlayCircleOutlined /> {course.videoCount || 0} 个视频</span>
                <span><ClockCircleOutlined /> {formatCourseDuration(course.totalDuration)}</span>
              </Space>
            </div>
            <div className="course-detail-side">
              {course.coverUrl ? <img src={course.coverUrl} alt={course.title || '课程封面'} className="course-detail-cover" /> : <div className="course-detail-cover course-detail-cover--placeholder"><PlayCircleOutlined /></div>}
            </div>
          </div>
        </Card>

        <Card className="content-card course-chapter-card" variant="borderless">
          <div className="course-section-heading">
            <div>
              <div className="article-side-title">课程章节</div>
              <div className="course-section-subtitle">每个章节下包含对应的视频课或文字教程，点击即可开始学习。</div>
            </div>
            <Tag color="blue">{chapters.length} 章</Tag>
          </div>

          {chapters.length ? (
            <div className="course-chapter-list">
              {chapters.map((chapter, chapterIndex) => {
                const lessons = getChapterLessons(course, chapter)
                return (
                  <div className="course-chapter-item" key={String(chapter.id ?? chapter.chapterTitle ?? chapterIndex)}>
                    <div className="course-chapter-head">
                      <div>
                        <div className="course-chapter-kicker">第 {chapterIndex + 1} 章</div>
                        <h3>{chapter.chapterTitle || '未命名章节'}</h3>
                        <p>{chapter.chapterDesc || '本章节简介正在完善中。'}</p>
                      </div>
                      <Space className="course-chapter-tags" wrap>
                        <Tag>{lessons.length} 个课时</Tag>
                        {chapter.isFreePreview === 1 ? <Tag color="green">可试看</Tag> : null}
                      </Space>
                    </div>

                    {lessons.length ? (
                      <div className="course-lesson-list">
                        {lessons.map((lesson, lessonIndex) => (
                          <button
                            type="button"
                            className="course-lesson-row"
                            key={`${lesson.type}-${String(lesson.id ?? lessonIndex)}`}
                            onClick={() => navigate(`/detail/course/${courseId}/${lesson.type}/${String(lesson.id)}`)}
                          >
                            <span className={`course-lesson-icon course-lesson-icon--${lesson.type}`}>
                              {lesson.type === 'video' ? <VideoCameraOutlined /> : <FileTextOutlined />}
                            </span>
                            <span className="course-lesson-main">
                              <strong>{lesson.title}</strong>
                              <small>{lesson.desc || (lesson.type === 'video' ? '点击进入视频教程页面' : '点击进入文字教程页面')}</small>
                            </span>
                            <span className="course-lesson-meta">
                              {lesson.type === 'video' ? formatCourseDuration(lesson.duration) : `${lesson.readTime || 0} 分钟阅读`}
                            </span>
                          </button>
                        ))}
                      </div>
                    ) : (
                      <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="该章节暂未添加视频或文字教程" />
                    )}
                  </div>
                )
              })}
            </div>
          ) : standaloneArticles.length ? (
            <div className="course-lesson-list">
              {standaloneArticles.map((article: CourseLessonItem) => (
                <button type="button" className="course-lesson-row" key={String(article.id)} onClick={() => navigate(`/detail/course/${courseId}/article/${String(article.id)}`)}>
                  <span className="course-lesson-icon course-lesson-icon--article"><FileTextOutlined /></span>
                  <span className="course-lesson-main"><strong>{article.title || '未命名图文教程'}</strong><small>点击进入文字教程页面</small></span>
                  <span className="course-lesson-meta">{article.readTime || 0} 分钟阅读</span>
                </button>
              ))}
            </div>
          ) : (
            <Empty description="暂无章节内容" />
          )}
        </Card>
      </Col>

      <Col xs={24} lg={6}>
        <Space orientation="vertical" size={16} className="full-width">
          <Card className="content-card" variant="borderless" title="课程基本信息">
            <Space orientation="vertical" size={14} className="full-width">
              <div className="detail-stat-item"><span><BookOutlined /></span><span>难度：{getLevelText(course.level)}</span></div>
              <div className="detail-stat-item"><span><ClockCircleOutlined /></span><span>总时长：{formatCourseDuration(course.totalDuration)}</span></div>
              <div className="detail-stat-item"><span><StarOutlined /></span><span>评分：{course.rating || '暂无评分'}</span></div>
            </Space>
          </Card>
          <Card className="content-card" variant="borderless" title="讲师 / 来源">
            <div className="course-instructor-row" role={instructor?.userId != null ? 'button' : undefined} onClick={() => instructor?.userId != null ? navigate(`/profile/${encodeURIComponent(String(instructor.userId))}`) : undefined}>
              <Avatar size={52} src={instructorAvatar} />
              <div className="course-instructor-info">
                <div className="active-user__name">{instructorLoading ? '讲师信息加载中...' : instructorName}</div>
                <div className="active-user__role">{instructorDesc}</div>
              </div>
            </div>
            <Button type="primary" block className="top-gap" disabled={!firstLesson} onClick={() => firstLesson && navigate(`/detail/course/${courseId}/${firstLesson.type}/${String(firstLesson.id)}`)}>
              开始学习
            </Button>
          </Card>
        </Space>
        </Col>
      </Row>
      <Modal
        title="课程评分"
        open={reviewOpen}
        onCancel={() => setReviewOpen(false)}
        onOk={() => void handleSubmitReview()}
        okText="提交评分"
        cancelText="取消"
        confirmLoading={reviewSubmitting}
        width={680}
      >
        <Space orientation="vertical" size={18} className="full-width">
          <div className="course-review-form">
            <Rate value={reviewRating} onChange={setReviewRating} />
            <Input.TextArea rows={4} value={reviewContent} onChange={(event) => setReviewContent(event.target.value)} placeholder="可以补充你的课程评价" />
          </div>
          <Divider />
          <div className="course-review-list">
            <div className="course-review-list__title">课程评价</div>
            {reviewLoading ? <Skeleton active paragraph={{ rows: 3 }} /> : reviews.length ? reviews.map((review) => (
              <div key={String(review.id ?? `${review.userId}-${review.createdAt}`)} className="course-review-item">
                <div className="course-review-item__head">
                  <Rate disabled value={review.rating || 0} className="course-review-rate" />
                  <span>{review.createdAt || '刚刚'}</span>
                </div>
                <p>{review.content || '暂无评价内容'}</p>
              </div>
            )) : <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无课程评价" />}
          </div>
        </Space>
      </Modal>
    </div>
  )
}

function CourseLessonView({ course, courseId, lessonType, lessonId }: { course: CourseDetailVO; courseId: string; lessonType: string; lessonId: string }) {
  const navigate = useNavigate()
  const [videoInfo, setVideoInfo] = useState<VideoPlayVO | null>(null)
  const [videoLoading, setVideoLoading] = useState(false)
  const lessonGroups = getCourseLessonGroups(course)
  const lessons = lessonGroups.flatMap((group) => group.lessons)
  const currentLesson = lessons.find((lesson) => lesson.type === lessonType && String(lesson.id) === lessonId)
  const currentIndex = lessons.findIndex((lesson) => lesson.type === lessonType && String(lesson.id) === lessonId)
  const previousLesson = currentIndex > 0 ? lessons[currentIndex - 1] : undefined
  const nextLesson = currentIndex >= 0 ? lessons[currentIndex + 1] : undefined

  useEffect(() => {
    if (lessonType !== 'video' || !lessonId) {
      setVideoInfo(null)
      return
    }

    let active = true
    setVideoInfo(null)
    const loadVideo = async () => {
      try {
        setVideoLoading(true)
        const info = await getVideoPlayInfo(lessonId)
        if (active) setVideoInfo(info)
      } catch (error) {
        console.error('load video play info error:', error)
        if (active) setVideoInfo(null)
      } finally {
        if (active) setVideoLoading(false)
      }
    }

    void loadVideo()
    return () => { active = false }
  }, [lessonId, lessonType])

  const goLesson = (lesson?: CourseLessonItem) => {
    if (!lesson?.id) return
    navigate(`/detail/course/${courseId}/${lesson.type}/${String(lesson.id)}`)
  }

  if (!currentLesson) {
    return <Card className="content-card" variant="borderless"><Empty description="课时不存在" /></Card>
  }

  const article = currentLesson.type === 'article' ? currentLesson.source as CourseArticleVO : undefined
  const video = currentLesson.type === 'video' ? currentLesson.source as CourseVideoVO : undefined
  const playableUrl = videoInfo?.playable ? videoInfo.videoUrl : undefined

  return (
    <Row gutter={[24, 24]} className="detail-page course-lesson-page">
      <Col xs={24} lg={18}>
        <Card className="content-card detail-article-card" variant="borderless">
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate(`/detail/course/${courseId}`)} className="detail-back-btn">
            返回课程详情
          </Button>
          <Space wrap>
            <Tag color={currentLesson.type === 'video' ? 'blue' : 'green'} variant="filled">{currentLesson.type === 'video' ? '视频教程' : '文字教程'}</Tag>
            <Tag>{course.title || '课程'}</Tag>
          </Space>
          <h1 className="detail-page__title">{currentLesson.title}</h1>
          <div className="detail-page__meta">{currentLesson.type === 'video' ? formatCourseDuration(currentLesson.duration) : `${article?.readTime || 0} 分钟阅读 · ${article?.wordCount || 0} 字`}</div>

          <Divider />

          {currentLesson.type === 'video' ? (
            <div className="course-video-block">
              {videoLoading ? <Skeleton active paragraph={{ rows: 6 }} /> : playableUrl ? <CourseVideoPlayer key={`${courseId}-${lessonId}`} courseId={courseId} videoId={lessonId} src={playableUrl} poster={video?.coverUrl} /> : <Alert type="warning" showIcon message={videoInfo?.message || '暂无可播放的视频地址'} />}
              <p className="detail-page__content">{video?.videoDesc || '本节视频介绍正在完善中。'}</p>
            </div>
          ) : (
            <div className="article-content-block article-content-block--markdown" data-color-mode="light">
              <MDEditor.Markdown source={article?.content || '暂无文字教程内容'} className="article-preview-markdown" />
            </div>
          )}

          <Divider />
          <div className="course-lesson-nav">
            <Button disabled={!previousLesson} onClick={() => goLesson(previousLesson)}>上一节</Button>
            <Button type="primary" disabled={!nextLesson} onClick={() => goLesson(nextLesson)}>下一节</Button>
          </div>
          {course.canWatch ? <CourseStudyTools key={`${courseId}-${lessonType}-${lessonId}`} courseId={courseId} videoId={lessonType === 'video' ? lessonId : undefined} /> : null}
        </Card>
      </Col>

      <Col xs={24} lg={6}>
        <Card className="content-card" variant="borderless" title="课程目录">
          <Space orientation="vertical" size={12} className="full-width course-mini-catalog">
            {lessonGroups.map((group, groupIndex) => (
              <div key={group.key} className="course-mini-group">
                <div className="course-mini-group__head">
                  <div className="course-mini-group__title">第 {groupIndex + 1} 章</div>
                </div>
                {group.lessons.length ? (
                  <Space orientation="vertical" size={8} className="full-width">
                    {group.lessons.map((lesson) => (
                      <button type="button" key={`${group.key}-${lesson.type}-${String(lesson.id)}`} className={`course-mini-lesson ${lesson.type === lessonType && String(lesson.id) === lessonId ? 'is-active' : ''}`} onClick={() => goLesson(lesson)}>
                        <span className="course-mini-lesson__icon">
                          {lesson.type === 'video' ? <VideoCameraOutlined /> : <FileTextOutlined />}
                        </span>
                        <span className="course-mini-lesson__content">
                          <strong>{lesson.title}</strong>
                          <small>{lesson.type === 'video' ? formatCourseDuration(lesson.duration) : `${lesson.readTime || 0} 分钟阅读`}</small>
                        </span>
                      </button>
                    ))}
                  </Space>
                ) : (
                  <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="该章节暂未添加内容" />
                )}
              </div>
            ))}
          </Space>
        </Card>
      </Col>
    </Row>
  )
}

export function DetailPage() {
  const navigate = useNavigate()
  const { type, id, lessonType, lessonId } = useParams()
  const [interviewQuestion, setInterviewQuestion] = useState<InterviewQuestionVO>()
  const [interviewProgress, setInterviewProgress] = useState<UserQuestionProgressVO>()
  const [interviewPublisher, setInterviewPublisher] = useState<AuthProfileVO | null>(null)
  const [interviewPublisherLoading, setInterviewPublisherLoading] = useState(false)
  const [interviewLoading, setInterviewLoading] = useState(false)
  const [courseDetail, setCourseDetail] = useState<CourseDetailVO>()
  const [courseLoading, setCourseLoading] = useState(false)
  const [interviewAnswerExpanded, setInterviewAnswerExpanded] = useState(false)
  const [interviewLoadedFor, setInterviewLoadedFor] = useState('')
  const { isLoggedIn, userInfo } = useAppSelector((state) => state.auth)
  const interviewRequestKey = `${type}:${id}:${isLoggedIn}:${userInfo?.userId ?? userInfo?.username ?? 'guest'}`

  useEffect(() => {
    if (type !== 'interview-question' || !id) return

    // Question ids are snowflake values above Number.MAX_SAFE_INTEGER; Number(id)
    // would round to a neighbouring id and the detail request would 404, so the raw
    // string has to be kept.
    const questionId = id.trim()
    if (!/^[1-9]\d*$/.test(questionId)) return
    let cancelled = false

    const loadInterviewData = async () => {
      try {
        setInterviewLoading(true)
        setInterviewAnswerExpanded(false)
        setInterviewLoadedFor('')
        setInterviewQuestion(undefined)
        const detail = await getInterviewQuestionDetail(questionId)
        if (cancelled) return
        setInterviewLoadedFor(interviewRequestKey)
        setInterviewQuestion(detail)

        try {
          const progress = await getInterviewQuestionProgress(questionId)
          if (!cancelled) setInterviewProgress(progress)
        } catch (error) {
          if (cancelled) return
          console.warn('load interview progress error:', error)
          setInterviewProgress(undefined)
        }
      } catch (error) {
        if (cancelled) return
        console.error('load interview question detail error:', error)
        setInterviewQuestion(undefined)
        setInterviewProgress(undefined)
        setInterviewPublisher(null)
      } finally {
        if (!cancelled) setInterviewLoading(false)
      }
    }

    void loadInterviewData()
    return () => { cancelled = true }
  }, [id, type, interviewRequestKey])

  useEffect(() => {
    if (type !== 'interview-question') return

    const publisherId = interviewQuestion?.publisherId
    if (!publisherId) {
      setInterviewPublisher(null)
      return
    }

    let cancelled = false
    const loadPublisher = async () => {
      try {
        setInterviewPublisherLoading(true)
        const profile = await getUserProfile(publisherId)
        if (!cancelled) {
          setInterviewPublisher(profile)
        }
      } catch (error) {
        console.error('load interview publisher error:', error)
        if (!cancelled) {
          setInterviewPublisher(null)
        }
      } finally {
        if (!cancelled) {
          setInterviewPublisherLoading(false)
        }
      }
    }

    void loadPublisher()
    return () => {
      cancelled = true
    }
  }, [interviewQuestion?.publisherId, type])

  useEffect(() => {
    if (type !== 'course' || !id) return

    const courseId = normalizeRouteId(id)
    if (!courseId) return

    const loadCourseData = async () => {
      try {
        setCourseLoading(true)
        const detail = await getCourseDetail(courseId)
        setCourseDetail(detail)
      } catch (error) {
        console.error('load course detail error:', error)
        setCourseDetail(undefined)
      } finally {
        setCourseLoading(false)
      }
    }

    void loadCourseData()
  }, [id, type])

  const detail = useMemo(() => {
    if (type !== 'interview-question') {
      return null
    }

    return interviewQuestion
      ? {
          title: interviewQuestion.title || '未命名面试题',
          subtitle: `${interviewQuestion.categoryName || '未分类'} · ${interviewQuestion.companyName || '通用题库'}`,
          tag: '推荐答案',
          cover: '',
          content: interviewQuestion.content || '暂无题目内容',
          answer: interviewQuestion.answer || '暂无参考答案',
          bullets: [
            `题目编号：${interviewQuestion.questionNo || '-'}`,
            `难度：${interviewQuestion.difficulty || '-'} / 3`,
            `岗位标签：${(interviewQuestion.positionTags || []).join(' / ') || '通用'}`,
          ],
          stats: [
            { icon: <BookOutlined />, value: interviewQuestion.categoryName || '未分类' },
            { icon: <FireOutlined />, value: `收藏 ${interviewQuestion.collectCount || 0}` },
            { icon: <ClockCircleOutlined />, value: `练习 ${interviewProgress?.practiceCount || 0} 次` },
          ],
        }
      : null
  }, [interviewProgress?.practiceCount, interviewQuestion, type])

  const interviewPublisherName = interviewPublisher?.nickname?.trim() || interviewPublisher?.username?.trim() || (interviewQuestion?.publisherId ? `用户 ${interviewQuestion.publisherId}` : 'CodeHub 官方整理')
  const interviewPublisherDesc = interviewPublisher?.signature?.trim() || interviewPublisher?.title?.trim() || (interviewQuestion?.publisherId ? '面试题发布者' : '围绕当前主题持续更新内容')
  const interviewPublisherAvatar = interviewQuestion?.publisherId ? getAvatarWithFallback(interviewPublisher || undefined) : DEFAULT_AVATAR_URL
  const interviewAnswerVisible = interviewQuestion?.answerVisible === true && interviewLoadedFor === interviewRequestKey
  const canClickInterviewAnswer = interviewAnswerVisible
  const canViewInterviewAnswerByRole = canViewInterviewAnswer(userInfo)
  const interviewAnswerGateText = interviewAnswerVisible
    ? '已解锁，可查看完整答案'
    : !isLoggedIn
      ? '登录后即可查看完整参考答案'
      : !canViewInterviewAnswerByRole
        ? '开通会员即可解锁完整参考答案'
        : '当前账号暂不可查看此题答案'

  if (type === 'course') {
    if (courseLoading) return <Card className="content-card" variant="borderless"><Skeleton active avatar paragraph={{ rows: 10 }} /></Card>
    if (!courseDetail || !id) return <Card className="content-card" variant="borderless"><Empty description="未找到课程详情" /></Card>
    if (lessonType && lessonId) return <CourseLessonView course={courseDetail} courseId={id} lessonType={lessonType} lessonId={lessonId} />
    return <CourseDetailView course={courseDetail} courseId={id} />
  }

  if (type === 'interview-question' && interviewLoading) {
    return <Card className="content-card" variant="borderless"><div className="check-in-loading"><Skeleton active /></div></Card>
  }

  if (!detail) {
    return <Card className="content-card" variant="borderless">未找到对应详情内容。</Card>
  }

  return (
    <Row gutter={[24, 24]} className="detail-page">
      <Col xs={24} lg={18}>
        <Card className="content-card detail-article-card" variant="borderless">
          {type === 'interview-question' ? (
            <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/interviews')} className="detail-back-btn">
              返回面试刷题
            </Button>
          ) : null}
          {/* <Tag color="blue" variant="filled">
            {detail.tag}
          </Tag> */}
          <h1 className="detail-page__title">{detail.title}</h1>
          <div className="detail-page__meta">{detail.subtitle}</div>
          {detail.cover ? <img src={detail.cover} alt={detail.title} className="detail-page__cover" /> : null}
          <h3>{type === 'interview-question' ? '题目内容' : '正文内容'}</h3>
          <div className="article-content-block article-content-block--markdown" data-color-mode="light">
            <MDEditor.Markdown source={detail.content} className="article-preview-markdown" components={markdownImageComponents} />
          </div>
          {type === 'interview-question' ? (
            <>
              <Divider />
              <div className="interview-answer-tabs">
                <button type="button" className="interview-answer-tabs__item is-active">推荐答案</button>
              </div>
              <section className={`interview-answer-panel ${interviewAnswerExpanded ? 'is-visible' : 'is-locked'}`}>
                <div className="interview-answer-panel__head">
                  <h3>回答重点</h3>
                  {!interviewAnswerExpanded ? <span>隐藏答案</span> : null}
                </div>
                <div className="article-content-block article-content-block--markdown interview-answer-content" data-color-mode="light">
                  <MDEditor.Markdown source={detail.answer} className="article-preview-markdown" components={markdownImageComponents} />
                </div>
                {!interviewAnswerExpanded ? (
                  <div className="interview-answer-panel__mask">
                    <Button
                      type="primary"
                      size="large"
                      shape="round"
                      onClick={() => {
                        if (canClickInterviewAnswer) {
                          setInterviewAnswerExpanded(true)
                          return
                        }
                        if (!isLoggedIn) {
                          navigate('/auth')
                          return
                        }
                        if (!canViewInterviewAnswerByRole) {
                          navigate('/membership')
                          return
                        }
                        message.warning('当前账号暂不可查看此题答案')
                      }}
                    >
                      {canClickInterviewAnswer
                        ? '点击查看完整答案'
                        : !isLoggedIn
                          ? '登录后查看答案'
                          : !canViewInterviewAnswerByRole
                            ? '开通会员查看答案'
                            : '当前不可查看答案'}
                    </Button>
                    <span>{interviewAnswerGateText}</span>
                  </div>
                ) : null}
              </section>
            </>
          ) : null}
          <Divider />
          <h3>内容摘要</h3>
          <ul className="bullet-list">
            {detail.bullets.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </Card>
      </Col>

      <Col xs={24} lg={6}>
        <Space orientation="vertical" size={16} className="full-width">
          <Card className="content-card detail-info-card" variant="borderless" title="信息概览">
            <Space orientation="vertical" size={14} className="full-width interview-detail-info-card detail-info-list">
              {detail.stats.map((stat) => (
                <div key={stat.value} className="detail-stat-item">
                  <span>{stat.icon}</span>
                  <span>{stat.value}</span>
                </div>
              ))}
            </Space>
          </Card>
          <Card className="content-card" variant="borderless" title="作者 / 讲师">
            <Space className={interviewQuestion?.publisherId ? 'course-instructor-row' : undefined} onClick={() => interviewQuestion?.publisherId ? navigate(`/profile/${encodeURIComponent(String(interviewQuestion.publisherId))}`) : undefined}>
              <Avatar size={52} src={interviewPublisherAvatar} />
              <div>
                <div className="active-user__name">{interviewPublisherLoading ? '发布者加载中...' : interviewPublisherName}</div>
                <div className="active-user__role">{interviewPublisherDesc}</div>
              </div>
            </Space>
            {type === 'interview-question' && interviewQuestion?.id ? (
              <Button type="primary" block className="top-gap" onClick={() => void collectInterviewQuestion(interviewQuestion.id!)}>
                立即收藏
              </Button>
            ) : (
              <Button type="primary" block className="top-gap">
                立即收藏
              </Button>
            )}
          </Card>
        </Space>
      </Col>
    </Row>
  )
}
