import { Button, Card, Col, Row, Space, Spin, Tag, Alert, Empty, Input, Select } from 'antd'
import axios from 'axios'
import {
  FireOutlined,
  NotificationOutlined,
  ReloadOutlined,
  SearchOutlined,
} from '@ant-design/icons'
import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { FeedCard } from '../../components/FeedCard/index.tsx'
import { listCategories, listPosts } from '../../api/post'
import { listActiveDevelopers } from '../../api/activeDevelopers'
import { useAppSelector } from '../../store/hooks'
import { listAnnouncements } from '../../api/announcement'
import { getCheckDaysStatistics } from '../../api/check'
import { getOjSolvedCountStatistics } from '../../api/oj'
import type { PostCategoryVO, PostListItemVO } from '../../api/type/postTypings'
import type { AnnouncementListItemVO } from '../../api/type/announcementTypings'
import type { ActiveDeveloperVO } from '../../api/activeDevelopers'
import { buildSearchPath, SEARCH_KEYWORD_LIMIT } from '../../utils/search'
import { getAvatarWithFallback } from '../../utils/avatar'
import './page.scss'

const PAGE_SIZE = 10

const getAxiosErrorMessage = (error: unknown, fallback: string) => {
  if (!axios.isAxiosError(error)) {
    if (error instanceof Error && error.message) {
      return error.message
    }

    return fallback
  }

  const responseData = error.response?.data

  if (typeof responseData === 'string' && responseData) {
    return responseData
  }

  if (responseData && typeof responseData === 'object') {
    const errorData = responseData as { message?: string; msg?: string; error?: string }
    return errorData.message || errorData.msg || errorData.error || fallback
  }

  return error.message || fallback
}

const isVerified = (value?: number | boolean | string | null) => value === true || value === 1 || value === '1' || value === 'true' || value === 'approved' || value === '已通过'

const getAnnouncementTypeInfo = (type?: number) => {
  if (type === 1) return { label: '重要公告', color: 'red' }
  if (type === 2) return { label: '维护通知', color: 'orange' }
  if (type === 3) return { label: '活动推广', color: 'cyan' }
  return { label: '普通通知', color: 'blue' }
}

const formatAnnouncementDate = (value?: string) => {
  if (!value) return '暂无时间'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(date)
}

const getIdentityBadges = (source?: {
  schoolName?: string
  authorSchoolName?: string
  userSchoolName?: string
  schoolVerified?: number | boolean | string | null
  companyName?: string
  authorCompanyName?: string
  userCompanyName?: string
  companyVerified?: number | boolean | string | null
  authorTitle?: string
  userTitle?: string
  jobTitle?: string
  titleVerified?: number | boolean | string | null
}) => {
  const companyName = source?.authorCompanyName?.trim() || source?.userCompanyName?.trim() || source?.companyName?.trim()
  const title = source?.authorTitle?.trim() || source?.userTitle?.trim() || source?.jobTitle?.trim()
  const schoolName = source?.authorSchoolName?.trim() || source?.userSchoolName?.trim() || source?.schoolName?.trim()
  const companyVerified = Boolean(companyName) || isVerified(source?.companyVerified)
  const titleVerified = Boolean(title) || isVerified(source?.titleVerified)
  const schoolVerified = Boolean(schoolName) || isVerified(source?.schoolVerified)

  return [
    { key: 'company', label: companyName || (companyVerified ? '企业认证' : ''), verified: companyVerified, color: 'blue' },
    { key: 'school', label: schoolName || (schoolVerified ? '学校认证' : ''), verified: schoolVerified, color: 'green' },
    { key: 'title', label: title || (titleVerified ? '身份认证' : ''), verified: titleVerified, color: 'purple' },
  ].filter((item) => item.verified && item.label)
}

export function HomePage() {
  const navigate = useNavigate()
  const isLoggedIn = useAppSelector((state) => state.auth.isLoggedIn)
  const [posts, setPosts] = useState<PostListItemVO[]>([])
  const [categories, setCategories] = useState<PostCategoryVO[]>([])
  const [activeDevelopers, setActiveDevelopers] = useState<ActiveDeveloperVO[]>([])
  const [announcements, setAnnouncements] = useState<AnnouncementListItemVO[]>([])
  const [announcementLoading, setAnnouncementLoading] = useState(false)
  const [announcementError, setAnnouncementError] = useState('')
  const [stats, setStats] = useState([
    { value: 0, label: '累计打卡天数' },
    { value: 0, label: '刷题题目(道)' },
  ])
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [error, setError] = useState('')
  const [pageNum, setPageNum] = useState(1)
  const [hasMore, setHasMore] = useState(true)
  const [keywordInput, setKeywordInput] = useState('')
  const [keyword, setKeyword] = useState('')
  const [categoryId, setCategoryId] = useState<number | undefined>()

  const fetchPosts = useCallback(async (nextPage: number, append: boolean, nextKeyword = keyword, nextCategoryId = categoryId) => {
    const setLoadingState = append ? setLoadingMore : setLoading

    try {
      setLoadingState(true)
      if (!append) {
        setError('')
      }

      const data = await listPosts({
        status: 1,
        pageNum: nextPage,
        pageSize: PAGE_SIZE,
        sortBy: 'latest',
        keyword: nextKeyword || undefined,
        categoryId: nextCategoryId,
      })
      const nextRecords = data.records ?? []

      setPosts((prev) => (append ? [...prev, ...nextRecords] : nextRecords))
      setPageNum(nextPage)
      setHasMore(nextPage < (data.totalPages ?? 0))
    } catch (fetchError) {
      console.error('list posts error:', fetchError)

      if (!append) {
        setPosts([])
        setError(getAxiosErrorMessage(fetchError, '动态流加载失败，请稍后重试'))
      }
    } finally {
      setLoadingState(false)
    }
  }, [categoryId, keyword])

  const fetchAnnouncements = useCallback(async () => {
    try {
      setAnnouncementLoading(true)
      setAnnouncementError('')
      const data = await listAnnouncements({ pageNum: 1, pageSize: 3 })
      setAnnouncements(data.records || [])
    } catch (fetchError) {
      console.error('list announcements error:', fetchError)
      setAnnouncements([])
      setAnnouncementError(getAxiosErrorMessage(fetchError, '公告加载失败'))
    } finally {
      setAnnouncementLoading(false)
    }
  }, [])

  useEffect(() => {
    let cancelled = false

    const fetchBaseData = async () => {
      try {
        const [categoryData, activeDeveloperData] = await Promise.all([
          listCategories(),
          listActiveDevelopers(),
        ])
        if (!cancelled) {
          setCategories(categoryData)
          setActiveDevelopers((activeDeveloperData || []).slice(0, 3))
        }
      } catch (fetchError) {
        console.error('list base data error:', fetchError)
        if (!cancelled) {
          setActiveDevelopers([])
        }
      }

      try {
        const [checkDaysData, solvedCountData] = await Promise.all([
          getCheckDaysStatistics(),
          getOjSolvedCountStatistics(),
        ])
        if (!cancelled) {
          setStats([
            { value: checkDaysData.totalDays ?? 0, label: '累计打卡天数' },
            { value: solvedCountData.solvedProblemCount ?? 0, label: '刷题题目(道)' },
          ])
        }
      } catch (statsError) {
        console.error('list statistics error:', statsError)
      }
    }

    void fetchBaseData()
    void fetchAnnouncements()
    void fetchPosts(1, false)

    return () => {
      cancelled = true
    }
  }, [fetchAnnouncements, fetchPosts])

  const handleLoadMore = async () => {
    if (!hasMore || loadingMore) {
      return
    }

    await fetchPosts(pageNum + 1, true)
  }

  const handleSearch = () => {
    const normalizedKeyword = keywordInput.trim()
    if (!normalizedKeyword) return
    navigate(buildSearchPath(normalizedKeyword, 'article'))
  }

  const handleCategoryChange = async (value?: number) => {
    setCategoryId(value)
    await fetchPosts(1, false, keyword, value)
  }

  const handleResetFilters = async () => {
    setKeywordInput('')
    setKeyword('')
    setCategoryId(undefined)
    await fetchPosts(1, false, '', undefined)
  }

  return (
      <Row gutter={[24, 24]} align="top" className="home-page">
      <Col xs={24} lg={18}>
        <div className="hero-banner card-surface">
          <div>
            <div className="hero-banner__eyebrow">欢迎回来, 开发者!</div>
            <h1 className="hero-banner__title">“Keep coding, keep going. ” —— 持续编码，步履不停。</h1>
            <Space wrap>
              <Button
                type="primary"
                size="large"
                className="hero-banner__write-btn"
                onClick={() => {
                  if (!isLoggedIn) {
                    navigate('/auth')
                    return
                  }
                  navigate('/article/editor')
                }}
              >
                写文章
              </Button>
            </Space>
          </div>
          <div className="hero-banner__glow" />
        </div>

        <Card className="content-card home-page__filter-card" variant="borderless">
          <div className="home-page__filter-bar">
            <Input
              value={keywordInput}
              onChange={(event) => setKeywordInput(event.target.value)}
              placeholder="搜索文章标题、摘要或关键字"
              prefix={<SearchOutlined />}
              className="home-page__search-input"
              maxLength={SEARCH_KEYWORD_LIMIT}
              onPressEnter={() => handleSearch()}
            />
            <Select
              value={categoryId}
              onChange={(value) => void handleCategoryChange(value)}
              allowClear
              placeholder="全部分类"
              className="home-page__category-select"
              options={categories.map((item) => ({
                label: item.categoryName || '未命名分类',
                value: item.id,
              }))}
            />
            <Button type="primary" onClick={() => void handleSearch()}>
              搜索
            </Button>
            <Button icon={<ReloadOutlined />} onClick={() => void handleResetFilters()}>
              重置
            </Button>
          </div>
        </Card>

        <Space orientation="vertical" size={20} className="full-width">
          {loading ? (
            <Card className="content-card" variant="borderless">
              <div className="home-page__loading">
                <Spin size="large" />
              </div>
            </Card>
          ) : null}

          {!loading && error ? (
            <Card className="content-card" variant="borderless">
              <Alert type="error" showIcon description={error} />
            </Card>
          ) : null}

          {!loading && !error && !posts.length ? (
            <Card className="content-card" variant="borderless">
              <Empty description="没有符合当前筛选条件的文章" />
            </Card>
          ) : null}

          {!loading && !error ? posts.map((post) => (
            <FeedCard
              key={String(post.id ?? post.title ?? Math.random())}
              post={post}
              detailPath={post.id != null ? `/article/${String(post.id).trim()}` : undefined}
              disableLink={post.id == null || String(post.id).trim() === ''}
            />
          )) : null}

          {!loading && !error && posts.length ? (
            <Card className="content-card home-page__load-more-card" variant="borderless">
              <div className="home-page__load-more">
                {hasMore ? (
                  <Button type="primary" loading={loadingMore} onClick={() => void handleLoadMore()}>
                    {loadingMore ? '加载中...' : '加载更多'}
                  </Button>
                ) : (
                  <div className="home-page__load-more-text">已经到底啦，没有更多文章了</div>
                )}
              </div>
            </Card>
          ) : null}
        </Space>
      </Col>

      <Col xs={24} lg={6} className="home-page__side">
        <Space orientation="vertical" size={20} className="full-width">
          <Card className="side-card" variant="borderless">
            <div className="side-card__title">
              <FireOutlined /> 学习统计
            </div>
            <div className="stats-grid">
              {stats.map((item) => (
                <div key={item.label} className="stats-grid__item">
                  <div className="stats-grid__value">{item.value}</div>
                  <div className="stats-grid__label">{item.label}</div>
                </div>
              ))}
            </div>
            <Button block size="large" className="dark-button" onClick={() => navigate('/practice')}>
              去刷题闯关
            </Button>
          </Card>

          <Card className="side-card" variant="borderless">
            <div className="side-card__title">活跃开发者</div>
            <Space orientation="vertical" size={18} className="full-width">
              {activeDevelopers.length ? activeDevelopers.map((user) => {
                const displayName = user.nickname?.trim() || user.username?.trim() || '匿名用户'
                const profilePath = user.userId != null ? `/profile/${encodeURIComponent(String(user.userId))}` : '/profile'
                return (
                  <div key={String(user.userId ?? displayName)} className="active-user">
                    <Space>
                      <button type="button" className="active-user__avatar-button" onClick={() => navigate(profilePath)} aria-label={`查看${displayName}的个人信息`}>
                        <img
                          className="active-user__avatar"
                          src={getAvatarWithFallback(user)}
                          alt={displayName}
                        />
                      </button>
                      <div>
                        <div className="active-user__name">{displayName}{getIdentityBadges(user).map((badge) => <Tag key={badge.key} color={badge.color} className="active-user__badge">{badge.label}</Tag>)}</div>
                        <div className="active-user__role">活跃度 {user.activityLevel ?? 0}</div>
                      </div>
                    </Space>
                  </div>
                )
              }) : <Empty description="暂无活跃开发者" />}
            </Space>
          </Card>

          <div className="notice-card">
            <div className="side-card__title notice-card__title">
              <span><NotificationOutlined /> 平台公告</span>
              <Button size="small" type="text" className="notice-card__refresh" loading={announcementLoading} onClick={() => void fetchAnnouncements()}>
                刷新
              </Button>
            </div>
            {announcementLoading ? (
              <div className="notice-card__loading">
                <Spin size="small" />
              </div>
            ) : announcementError ? (
              <div className="notice-card__empty">{announcementError}</div>
            ) : announcements.length ? announcements.map((announcement) => {
              const typeInfo = getAnnouncementTypeInfo(announcement.type)
              return (
                <div key={String(announcement.id ?? announcement.title)} className="notice-item">
                  <div className="notice-item__meta">
                    <strong>{formatAnnouncementDate(announcement.publishTime || announcement.createdAt)}</strong>
                    {announcement.isPinned === 1 ? <Tag color="red" className="notice-item__tag">置顶</Tag> : null}
                  </div>
                  <div className="notice-item__title">
                    {announcement.title || '未命名公告'}
                  </div>
                  <div className="notice-item__footer">
                    <Tag color={typeInfo.color} className="notice-item__tag">{typeInfo.label}</Tag>
                    <span>阅读 {announcement.clickCount ?? 0}</span>
                  </div>
                </div>
              )
            }) : (
              <div className="notice-card__empty">暂无平台公告</div>
            )}
          </div>
        </Space>
      </Col>
      </Row>
  )
}
