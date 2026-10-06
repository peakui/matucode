import { BookOutlined, ClockCircleOutlined, PlayCircleOutlined, PlusOutlined, ReloadOutlined, SearchOutlined, SettingOutlined, StarOutlined, UserSwitchOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Col, Empty, Input, Row, Select, Space, Spin, Tag } from 'antd'
import axios from 'axios'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { listCourses } from '../../api/course'
import type { CourseListItemVO, CourseListParams } from '../../api/type/courseTypings'
import { useAppSelector } from '../../store/hooks'
import { buildSearchPath, SEARCH_KEYWORD_LIMIT } from '../../utils/search'
import { canPublishCourse } from '../../utils/permissions'
import './page.scss'

const PAGE_SIZE = 12

const levelOptions = [
  { label: '全部难度', value: 0 },
  { label: '入门', value: 1 },
  { label: '基础', value: 2 },
  { label: '进阶', value: 3 },
  { label: '高级', value: 4 },
]

const sortOptions: { label: string; value: NonNullable<CourseListParams['sortBy']> }[] = [
  { label: '最新发布', value: 'latest' },
  { label: '最受欢迎', value: 'popular' },
  { label: '评分最高', value: 'rating' },
]

const formatDuration = (totalDuration?: number) => {
  if (!totalDuration || totalDuration <= 0) return '时长待完善'
  const hours = Math.floor(totalDuration / 3600)
  const minutes = Math.floor((totalDuration % 3600) / 60)
  const seconds = totalDuration % 60
  if (hours > 0) return `${hours} 小时 ${minutes} 分钟 ${seconds} 秒`
  if (minutes > 0) return `${minutes} 分钟 ${seconds} 秒`
  return `${seconds} 秒`
}

const getCourseTag = (course: CourseListItemVO) => {
  if (course.isFree === 1) return '免费'
  if (course.price && Number(course.price) > 0) return `¥${course.price}`
  return '课程'
}

const getLevelText = (level?: number) => {
  if (level === 1) return '入门'
  if (level === 2) return '基础'
  if (level === 3) return '进阶'
  if (level === 4) return '高级'
  return '未分级'
}

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

export function TutorialsPage() {
  const navigate = useNavigate()
  const userInfo = useAppSelector((state) => state.auth.userInfo)
  const publishEnabled = canPublishCourse(userInfo)
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [courses, setCourses] = useState<CourseListItemVO[]>([])
  const [pageNum, setPageNum] = useState(1)
  const [hasMore, setHasMore] = useState(false)
  const [total, setTotal] = useState(0)
  const [keywordInput, setKeywordInput] = useState('')
  const [keyword, setKeyword] = useState('')
  const [level, setLevel] = useState<number | undefined>()
  const [freeOnly, setFreeOnly] = useState<boolean | undefined>()
  const [sortBy, setSortBy] = useState<NonNullable<CourseListParams['sortBy']>>('latest')
  const [error, setError] = useState('')

  const fetchCourses = useCallback(async (
    nextPage: number,
    append: boolean,
    nextOptions: Partial<Pick<CourseListParams, 'keyword' | 'level' | 'freeOnly' | 'sortBy'>> = {},
  ) => {
    const setLoadingState = append ? setLoadingMore : setLoading
    const nextKeyword = nextOptions.keyword ?? keyword
    const nextLevel = nextOptions.level ?? level
    const nextFreeOnly = nextOptions.freeOnly ?? freeOnly
    const nextSortBy = nextOptions.sortBy ?? sortBy

    try {
      setLoadingState(true)
      if (!append) setError('')

      const data = await listCourses({
        status: 1,
        pageNum: nextPage,
        pageSize: PAGE_SIZE,
        sortBy: nextSortBy,
        keyword: nextKeyword || undefined,
        level: nextLevel,
        freeOnly: nextFreeOnly,
      })
      const records = data.records || []
      setCourses((prev) => (append ? [...prev, ...records] : records))
      setPageNum(nextPage)
      setTotal(data.total || 0)
      setHasMore(nextPage * PAGE_SIZE < (data.total || 0))
    } catch (fetchError) {
      console.error('load courses error:', fetchError)
      if (!append) {
        setCourses([])
        setTotal(0)
        setHasMore(false)
        setError(getAxiosErrorMessage(fetchError, '课程列表加载失败，请检查 service-course 接口'))
      }
    } finally {
      setLoadingState(false)
    }
  }, [freeOnly, keyword, level, sortBy])

  useEffect(() => {
    void fetchCourses(1, false)
  }, [fetchCourses])

  const totalVideos = useMemo(() => courses.reduce((sum, course) => sum + (course.videoCount || 0), 0), [courses])
  const totalChapters = useMemo(() => courses.reduce((sum, course) => sum + (course.chapterCount || 0), 0), [courses])

  const handleSearch = () => {
    const normalizedKeyword = keywordInput.trim()
    if (!normalizedKeyword) return
    navigate(buildSearchPath(normalizedKeyword, 'course'))
  }

  const handleLevelChange = async (value: number) => {
    const nextLevel = value || undefined
    setLevel(nextLevel)
    await fetchCourses(1, false, { level: nextLevel })
  }

  const handleFreeOnlyChange = async (value?: boolean) => {
    setFreeOnly(value)
    await fetchCourses(1, false, { freeOnly: value })
  }

  const handleSortChange = async (value: NonNullable<CourseListParams['sortBy']>) => {
    setSortBy(value)
    await fetchCourses(1, false, { sortBy: value })
  }

  const handleReset = async () => {
    setKeywordInput('')
    setKeyword('')
    setLevel(undefined)
    setFreeOnly(undefined)
    setSortBy('latest')
    await fetchCourses(1, false, { keyword: '', level: undefined, freeOnly: undefined, sortBy: 'latest' })
  }

  return (
    <Space orientation="vertical" size={20} className="full-width tutorials-page">
      <div className="channel-hero card-surface tutorials-hero">
        <div>
          <div className="channel-hero__label">课程教程中心</div>
          <h2>用系统课程和实战视频，补齐你的知识图谱</h2>
          <p>不只是教语法，更是教思维。[语言/框架] 进阶之路，帮你构建完整的计算机知识体系。</p>
        </div>
      </div>

      <Card className="content-card tutorials-content-card" variant="borderless">
        <div className="tutorials-section-toolbar">
          <div className="tutorials-section-title">课程列表</div>
          {publishEnabled ? (
            <Space wrap>
              <Button icon={<SettingOutlined />} onClick={() => navigate('/tutorials/manage')}>
                课程管理
              </Button>
              <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/tutorials/editor')}>
                发布课程
              </Button>
            </Space>
          ) : (
            <Button icon={<UserSwitchOutlined />} onClick={() => navigate('/profile/edit')}>
              申请成为老师
            </Button>
          )}
        </div>
            <div className="tutorial-filters">
              <Input
                value={keywordInput}
                onChange={(event) => setKeywordInput(event.target.value)}
                maxLength={SEARCH_KEYWORD_LIMIT}
                onPressEnter={() => handleSearch()}
                placeholder="搜索课程标题、简介或关键词"
                prefix={<SearchOutlined />}
                className="tutorial-search"
              />
              <Space wrap>
                <Select value={level || 0} options={levelOptions} onChange={(value) => void handleLevelChange(value)} className="tutorial-filter-select" />
                <Select
                  allowClear
                  value={freeOnly}
                  placeholder="全部课程"
                  options={[{ label: '只看免费', value: true }, { label: '付费课程', value: false }]}
                  onChange={(value) => void handleFreeOnlyChange(value)}
                  className="tutorial-filter-select"
                />
                <Select value={sortBy} options={sortOptions} onChange={(value) => void handleSortChange(value)} className="tutorial-filter-select" />
                <Button type="primary" onClick={() => void handleSearch()}>搜索</Button>
                <Button icon={<ReloadOutlined />} onClick={() => void handleReset()}>重置</Button>
              </Space>
            </div>

            <div className="tutorials-summary-grid">
              <div className="summary-tile"><strong>{total}</strong><span>匹配课程</span></div>
              <div className="summary-tile"><strong>{totalChapters}</strong><span>当前页章节</span></div>
              <div className="summary-tile"><strong>{totalVideos}</strong><span>当前页视频</span></div>
              <div className="summary-tile"><strong>{courses.filter((course) => course.isFree === 1).length}</strong><span>当前页免费课</span></div>
            </div>

            {loading ? (
              <div className="profile-tab-loading"><Spin size="large" /></div>
            ) : error ? (
              <Alert type="error" showIcon message={error} />
            ) : courses.length ? (
              <>
                <Row gutter={[16, 16]} className="tutorials-course-list">
                  {courses.map((course) => (
                    <Col xs={24} sm={12} lg={8} key={String(course.id ?? course.title)}>
                      <Card hoverable className="course-card" onClick={() => navigate(course.id != null ? `/detail/course/${String(course.id)}` : '/tutorials')}>
                        <div className="course-card__media">
                          {course.coverUrl ? <img alt={course.title || '课程封面'} src={course.coverUrl} className="course-card__cover" /> : <div className="course-card__placeholder"><PlayCircleOutlined /></div>}
                        </div>
                        <div className="course-card__content">
                          <Space wrap>
                            <Tag color={course.isFree === 1 ? 'green' : 'blue'} variant="filled">{getCourseTag(course)}</Tag>
                            <Tag color="purple" variant="filled">{getLevelText(course.level)}</Tag>
                            {course.rating ? <Tag color="gold" icon={<StarOutlined />}>{course.rating}</Tag> : null}
                          </Space>
                          <h3>{course.title || '未命名课程'}</h3>
                          <p>{course.subtitle || '课程简介正在完善中。'}</p>
                          <Space wrap size={16} className="course-card__meta-line">
                            <span><BookOutlined /> 章节 {course.chapterCount ?? 0}</span>
                            <span><PlayCircleOutlined /> 视频 {course.videoCount ?? 0}</span>
                            <span><ClockCircleOutlined /> {formatDuration(course.totalDuration)}</span>
                          </Space>
                          <div className="course-card__footer">
                            <Button type="primary" onClick={(event) => { event.stopPropagation(); navigate(course.id != null ? `/detail/course/${String(course.id)}` : '/tutorials') }}>查看详情</Button>
                          </div>
                        </div>
                      </Card>
                    </Col>
                  ))}
                </Row>
                {hasMore ? (
                  <div className="tutorials-load-more">
                    <Button type="primary" loading={loadingMore} onClick={() => void fetchCourses(pageNum + 1, true)}>加载更多</Button>
                  </div>
                ) : null}
              </>
            ) : (
              <Empty description="暂无课程数据" />
            )}
      </Card>
    </Space>
  )
}
