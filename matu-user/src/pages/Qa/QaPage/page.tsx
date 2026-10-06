import { Button, Card, Col, Empty, Row, Select, Space, Spin } from 'antd'
import { useEffect, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { FeedCard } from '../../../components/FeedCard/index.tsx'
import type { PostListItemVO } from '../../../api/type/postTypings.ts'
import { useAppDispatch, useAppSelector } from '../../../store/hooks'
import { fetchQaCategories, fetchQaQuestions, setQaCategoryId, setQaSortBy } from '../../../store/modules/qaListSlice'
import './page.scss'

const mapQuestionToFeed = (item: {
  id?: string | number
  userId?: string | number
  username?: string
  nickname?: string
  avatarUrl?: string
  userAvatar?: string
  userAvatarUrl?: string
  headImgUrl?: string
  schoolName?: string
  authorSchoolName?: string
  userSchoolName?: string
  schoolVerified?: number | boolean
  companyName?: string
  authorCompanyName?: string
  userCompanyName?: string
  companyVerified?: number | boolean
  titleVerified?: number | boolean
  title?: string
  authorTitle?: string
  authorIsVip?: number | boolean
  userTitle?: string
  jobTitle?: string
  questionTitle?: string
  content?: string
  categoryId?: string | number
  categoryName?: string
  viewCount?: number
  answerCount?: number
  followCount?: number
  status?: number
  createdAt?: string
}): PostListItemVO => ({
  id: item.id,
  userId: item.userId,
  username: item.username,
  nickname: item.nickname,
  authorName: item.nickname || item.username || '匿名用户',
  avatarUrl: item.avatarUrl || item.userAvatar,
  authorAvatar: item.userAvatar || item.avatarUrl,
  userAvatar: item.userAvatar,
  userAvatarUrl: item.userAvatarUrl,
  headImgUrl: item.headImgUrl,
  schoolName: item.authorSchoolName || item.schoolName || item.userSchoolName,
  authorSchoolName: item.authorSchoolName || item.schoolName || item.userSchoolName,
  schoolVerified: item.schoolVerified,
  companyName: item.authorCompanyName || item.companyName || item.userCompanyName,
  authorCompanyName: item.authorCompanyName || item.companyName || item.userCompanyName,
  companyVerified: item.companyVerified,
  authorTitle: item.authorTitle || item.userTitle || item.jobTitle,
  titleVerified: item.titleVerified,
  authorIsVip: item.authorIsVip,
  title: item.questionTitle || item.title || '未命名问题',
  summary: item.content?.trim() || `${item.answerCount ?? 0} 个回答 · ${item.followCount ?? 0} 人关注 · ${item.viewCount ?? 0} 次浏览`,
  categoryId: item.categoryId,
  categoryName: item.categoryName || '问答',
  viewCount: item.viewCount,
  commentCount: item.answerCount,
  collectCount: item.followCount,
  shareCount: 0,
  status: item.status,
  createdAt: item.createdAt,
  tags: [item.status === 1 ? '已解决' : item.status === 2 ? '已关闭' : '待解决'],
})

export function QaPage() {
  const navigate = useNavigate()
  const dispatch = useAppDispatch()
  const { questions, categories, categoryId, sortBy, loading } = useAppSelector((state) => state.qaList)
  const isLoggedIn = useAppSelector((state) => state.auth.isLoggedIn)

  useEffect(() => {
    void dispatch(fetchQaCategories())
  }, [dispatch])

  useEffect(() => {
    void dispatch(fetchQaQuestions())
  }, [dispatch, categoryId, sortBy])

  const feedPosts = useMemo(() => questions.map(mapQuestionToFeed), [questions])

  return (
    <Row gutter={[24, 24]} align="top" className="qa-page">
      <Col xs={24} lg={18}>
        <div className="hero-banner qa-hero card-surface">
          <div>
            <div className="hero-banner__eyebrow">提出一个好问题，往往比答案更重要</div>
            <h1 className="hero-banner__title">
              把你的报错、思路和尝试过程写清楚，社区里的开发者会更快理解问题，也更容易给出高质量回答。
            </h1>
            <Space wrap>
              <Button
                type="primary"
                size="large"
                className="qa-hero__action-btn"
                onClick={() => {
                  if (!isLoggedIn) {
                    navigate('/auth')
                    return
                  }
                  navigate('/question/editor')
                }}
              >
                立即提问
              </Button>
              <Button size="large" className="soft-button" onClick={() => navigate('/tutorials')}>
                先看教程
              </Button>
            </Space>
          </div>
          <div className="hero-banner__glow" />
        </div>

        <Card className="content-card qa-filter-card" variant="borderless">
          <Space wrap>
            <Select
              allowClear
              value={categoryId}
              placeholder="全部分类"
              className="qa-filter-select"
              onChange={(value) => dispatch(setQaCategoryId(value))}
              options={categories.map((item) => ({
                label: item.categoryName || '未命名分类',
                value: item.id,
              })).filter((item) => item.value)}
            />
            <Select
              value={sortBy}
              className="qa-filter-select"
              onChange={(value) => dispatch(setQaSortBy(value))}
              options={[
                { label: '最新发布', value: 'latest' },
                { label: '最多浏览', value: 'views' },
                { label: '最多回答', value: 'answers' },
                { label: '最高悬赏', value: 'bounty' },
              ]}
            />
          </Space>
        </Card>

        <Space direction="vertical" size={20} className="full-width top-gap">
          {loading ? (
            <Card className="content-card" variant="borderless">
              <div className="qa-loading"><Spin /></div>
            </Card>
          ) : feedPosts.length ? (
            feedPosts.map((post) => (
              <FeedCard
                key={`qa-${String(post.id ?? post.title ?? 'unknown')}`}
                post={post}
                detailPath={post.id != null ? `/qa/question/${String(post.id).trim()}` : undefined}
                disableLink={post.id == null || String(post.id).trim() === ''}
              />
            ))
          ) : (
            <Card className="content-card" variant="borderless">
              <Empty description="暂无问答内容" />
            </Card>
          )}
        </Space>
      </Col>

      <Col xs={24} lg={6} className="qa-page__side">
        <Card className="content-card" variant="borderless" title="提问建议">
          <ul className="bullet-list">
            <li>描述清楚你的技术栈和运行环境。</li>
            <li>贴出核心代码片段，不要只发截图。</li>
            <li>说明你已经排查过哪些方向。</li>
            <li>将问题拆小，能提升获得回答的概率。</li>
          </ul>
        </Card>
      </Col>
    </Row>
  )
}
