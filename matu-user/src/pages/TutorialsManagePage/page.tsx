import { ArrowLeftOutlined, EditOutlined, EyeOutlined, FileTextOutlined, PlusOutlined, ReloadOutlined, UserSwitchOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Empty, Space, Spin, Table, Tag, message } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import axios from 'axios'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { listCourses, offlineCourse, publishCourse } from '../../api/course'
import type { CourseListItemVO } from '../../api/type/courseTypings'
import { useAppSelector } from '../../store/hooks'
import { canPublishCourse } from '../../utils/permissions'
import './page.scss'

const PAGE_SIZE = 50

const getAxiosErrorMessage = (error: unknown, fallback: string) => {
  if (!axios.isAxiosError(error)) {
    return error instanceof Error && error.message ? error.message : fallback
  }

  const responseData = error.response?.data
  if (typeof responseData === 'string' && responseData) return responseData
  if (responseData && typeof responseData === 'object') {
    const data = responseData as { message?: string; msg?: string; error?: string }
    return data.message || data.msg || data.error || fallback
  }

  return error.message || fallback
}

const getLevelText = (level?: number) => {
  if (level === 1) return '入门'
  if (level === 2) return '基础'
  if (level === 3) return '进阶'
  if (level === 4) return '高级'
  return '未分级'
}

const getStatusInfo = (course: CourseListItemVO) => {
  if (course.status === 0) return { label: '草稿', color: 'gold' }
  if (course.status === 1) return { label: '已发布', color: 'green' }
  if (course.status === 2) return { label: '下架/私密', color: 'red' }
  if (course.status === 3) return { label: '已删除', color: 'default' }
  if (course.publishedAt) return { label: '已发布', color: 'green' }

  return { label: '草稿', color: 'gold' }
}

const formatPrice = (course: CourseListItemVO) => {
  if (course.isFree === 1) return '免费'
  if (course.price != null && Number(course.price) > 0) return `¥${course.price}`
  return '未设置'
}

const formatTime = (value?: string) => {
  if (!value) return '暂无'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

export function TutorialsManagePage() {
  const navigate = useNavigate()
  const userInfo = useAppSelector((state) => state.auth.userInfo)
  const canManage = canPublishCourse(userInfo)
  const [courses, setCourses] = useState<CourseListItemVO[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [operatingCourseId, setOperatingCourseId] = useState<string | number>()

  const loadCourses = useCallback(async () => {
    try {
      setLoading(true)
      setError('')
      const data = await listCourses({ pageNum: 1, pageSize: PAGE_SIZE, sortBy: 'latest', mine: true })
      setCourses(data.records || [])
    } catch (loadError) {
      console.error('load managed courses error:', loadError)
      setCourses([])
      setError(getAxiosErrorMessage(loadError, '课程管理列表加载失败，请稍后重试'))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    if (!canManage) {
      setLoading(false)
      return
    }

    void loadCourses()
  }, [canManage, loadCourses])

  const handlePublish = useCallback(async (courseId?: string | number) => {
    if (courseId == null) return

    try {
      setOperatingCourseId(courseId)
      await publishCourse(courseId)
      message.success('课程发布成功')
      await loadCourses()
    } catch (publishError) {
      console.error('publish course error:', publishError)
      message.error(getAxiosErrorMessage(publishError, '课程发布失败，请稍后重试'))
    } finally {
      setOperatingCourseId(undefined)
    }
  }, [loadCourses])

  const handleOffline = useCallback(async (courseId?: string | number) => {
    if (courseId == null) return

    try {
      setOperatingCourseId(courseId)
      await offlineCourse(courseId)
      message.success('课程已下线')
      await loadCourses()
    } catch (offlineError) {
      console.error('offline course error:', offlineError)
      message.error(getAxiosErrorMessage(offlineError, '课程下线失败，请稍后重试'))
    } finally {
      setOperatingCourseId(undefined)
    }
  }, [loadCourses])

  const summary = useMemo(() => ({
    total: courses.length,
    draft: courses.filter((course) => getStatusInfo(course).label === '草稿').length,
    published: courses.filter((course) => getStatusInfo(course).label === '已发布').length,
    offline: courses.filter((course) => getStatusInfo(course).label === '下架/私密').length,
  }), [courses])

  const columns = useMemo<ColumnsType<CourseListItemVO>>(() => [
    {
      title: '课程',
      dataIndex: 'title',
      key: 'title',
      render: (_value, course) => (
        <div className="course-manage-title-cell">
          <strong>{course.title || '未命名课程'}</strong>
          <span>{course.subtitle || '暂无副标题'}</span>
        </div>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 110,
      render: (_value, course) => {
        const status = getStatusInfo(course)
        return <Tag color={status.color} variant="filled">{status.label}</Tag>
      },
    },
    {
      title: '难度',
      dataIndex: 'level',
      key: 'level',
      width: 100,
      render: (value: number | undefined) => <Tag color="purple">{getLevelText(value)}</Tag>,
    },
    {
      title: '价格',
      key: 'price',
      width: 110,
      render: (_value, course) => formatPrice(course),
    },
    {
      title: '内容',
      key: 'content',
      width: 160,
      render: (_value, course) => `${course.chapterCount ?? 0} 章 / ${course.videoCount ?? 0} 视频`,
    },
    {
      title: '发布时间',
      dataIndex: 'publishedAt',
      key: 'publishedAt',
      width: 180,
      render: (value: string | undefined) => formatTime(value),
    },
    {
      title: '操作',
      key: 'actions',
      width: 260,
      render: (_value, course) => {
        const status = getStatusInfo(course)
        const loadingAction = operatingCourseId === course.id
        return (
          <Space wrap>
            <Button icon={<EyeOutlined />} onClick={() => navigate(course.id != null ? `/detail/course/${String(course.id)}` : '/tutorials')}>
              查看详情
            </Button>
            <Button icon={<EditOutlined />} disabled={course.id == null} onClick={() => course.id != null && navigate(`/tutorials/editor/${String(course.id)}`)}>
              编辑
            </Button>
            <Button icon={<FileTextOutlined />} disabled={course.id == null} onClick={() => course.id != null && navigate(`/tutorials/editor/${String(course.id)}/content`)}>
              章节内容
            </Button>
            {status.label === '已发布' ? (
              <Button danger loading={loadingAction} onClick={() => void handleOffline(course.id)}>
                下线
              </Button>
            ) : (
              <Button type="primary" loading={loadingAction} onClick={() => void handlePublish(course.id)}>
                发布
              </Button>
            )}
          </Space>
        )
      },
    },
  ], [handleOffline, handlePublish, navigate, operatingCourseId])

  if (!canManage) {
    return (
      <Space direction="vertical" size={20} className="full-width course-manage-page">
        <Card className="content-card course-manage-denied" variant="borderless">
          <div className="course-manage-denied__icon">
            <UserSwitchOutlined />
          </div>
          <h2>暂时没有课程管理权限</h2>
          <p>成为讲师后可以进入课程管理页面，发布课程、维护章节内容和管理课程状态。</p>
          <Space wrap className="course-manage-denied__actions">
            <Button type="primary" icon={<UserSwitchOutlined />} onClick={() => navigate('/profile/edit')}>
              申请成为讲师
            </Button>
            <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/tutorials')}>
              返回课程列表
            </Button>
          </Space>
        </Card>
      </Space>
    )
  }

  return (
    <Space direction="vertical" size={20} className="full-width course-manage-page">
      <Card className="content-card course-manage-hero" variant="borderless">
        <Space direction="vertical" size={16} className="full-width">
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/tutorials')} className="course-manage-back-btn">
            返回课程列表
          </Button>
          <div>
            <div className="channel-hero__label">课程管理</div>
            <h2>管理已发布课程和草稿</h2>
            <p>在这里查看课程状态，发布草稿课程，或将已发布课程下线。</p>
          </div>
        </Space>
      </Card>

      <Card className="content-card course-manage-card" variant="borderless">
        <div className="course-manage-toolbar">
          <div className="course-manage-summary-grid">
            <div className="summary-tile"><strong>{summary.total}</strong><span>全部课程</span></div>
            <div className="summary-tile"><strong>{summary.draft}</strong><span>草稿</span></div>
            <div className="summary-tile"><strong>{summary.published}</strong><span>已发布</span></div>
            <div className="summary-tile"><strong>{summary.offline}</strong><span>下架/私密</span></div>
          </div>
          <Space wrap>
            <Button icon={<ReloadOutlined />} onClick={() => void loadCourses()} loading={loading}>刷新</Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/tutorials/editor')}>发布课程</Button>
          </Space>
        </div>

        {loading ? (
          <div className="course-manage-loading"><Spin size="large" /></div>
        ) : error ? (
          <Alert type="error" showIcon message={error} />
        ) : courses.length ? (
          <Table<CourseListItemVO>
            rowKey={(course) => String(course.id ?? course.title)}
            columns={columns}
            dataSource={courses}
            pagination={false}
            className="course-manage-table"
          />
        ) : (
          <Empty description="暂无可管理课程" />
        )}
      </Card>
    </Space>
  )
}
