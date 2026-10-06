import { Alert, Button, Card, Col, Empty, Row, Space, Spin, Tag } from 'antd'
import { FireOutlined, TeamOutlined } from '@ant-design/icons'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { joinCheckGroup } from '../../../api/check.ts'
import { FeedCard } from '../../../components/FeedCard/index.tsx'
import type { CheckRecordListItemVO } from '../../../api/type/checkTypings.ts'
import { useAppDispatch, useAppSelector } from '../../../store/hooks'
import { fetchCheckListData } from '../../../store/modules/checkListSlice'
import './page.scss'

const getCheckSummary = (item: {
  summary?: string
  content?: string
}) => {
  if (item.summary?.trim()) {
    return item.summary.trim()
  }

  return item.content?.replace(/[#>*_`~\-[\]()]/g, ' ').replace(/\s+/g, ' ').trim().slice(0, 120) || '今天还没有填写打卡摘要。'
}

const mapCheckPostToFeed = (item: CheckRecordListItemVO) => ({
  id: item.id || item.recordId || item.checkId,
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
  owner: item.owner,
  title: item.recordTitle || item.title || '今日打卡',
  summary: getCheckSummary(item),
  categoryName: '每日打卡',
  likeCount: item.likeCount,
  commentCount: item.commentCount,
  shareCount: 0,
  createdAt: item.checkTime || item.createdAt,
  tags: [item.location, item.learnHours ? `${item.learnHours}h` : undefined].filter(Boolean) as string[],
})

const isVerified = (value?: number | boolean | string | null) => value === true || value === 1 || value === '1' || value === 'true' || value === 'approved' || value === '已通过'

const getGroupCreatorBadges = (group: {
  creatorSchoolName?: string
  creatorSchoolVerified?: number | boolean | string | null
  creatorCompanyName?: string
  creatorCompanyVerified?: number | boolean | string | null
  creatorTitle?: string
  creatorTitleVerified?: number | boolean | string | null
}) => {
  const companyName = group.creatorCompanyName?.trim()
  const title = group.creatorTitle?.trim()
  const schoolName = group.creatorSchoolName?.trim()
  const companyVerified = Boolean(companyName) || isVerified(group.creatorCompanyVerified)
  const titleVerified = Boolean(title) || isVerified(group.creatorTitleVerified)
  const schoolVerified = Boolean(schoolName) || isVerified(group.creatorSchoolVerified)

  return [
    { key: 'company', label: companyName || (companyVerified ? '企业认证' : ''), verified: companyVerified, color: 'blue' },
    { key: 'school', label: schoolName || (schoolVerified ? '学校认证' : ''), verified: schoolVerified, color: 'green' },
    { key: 'title', label: title || (titleVerified ? '身份认证' : ''), verified: titleVerified, color: 'purple' },
  ].filter((item) => item.verified && item.label)
}

export function CheckInPage() {
  const navigate = useNavigate()
  const dispatch = useAppDispatch()
  const { records, statistics, groups, loading, error } = useAppSelector((state) => state.checkList)
  const isLoggedIn = useAppSelector((state) => state.auth.isLoggedIn)
  const [joiningGroupId, setJoiningGroupId] = useState<string | number>()

  useEffect(() => {
    void dispatch(fetchCheckListData())
  }, [dispatch])

  const feedPosts = useMemo(() => records.map(mapCheckPostToFeed), [records])
  const checkFeedItems = useMemo(() => records.map((record, index) => {
    const rawCheckId = record.id == null ? '' : String(record.id).trim()

    return {
      key: rawCheckId || index,
      post: feedPosts[index],
      detailPath: rawCheckId ? `/check-in/${rawCheckId}` : undefined,
    }
  }), [feedPosts, records])

  const checkInStats = useMemo(
    () => [
      { value: statistics?.totalDays ?? 0, label: '总共打卡天数' },
      { value: statistics?.continuousDays ?? 0, label: '当前连续打卡天数' },
      { value: statistics?.totalLikesReceived ?? 0, label: '累计获得点赞数' },
    ],
    [statistics],
  )

  const handleJoinGroup = async (groupId?: string | number) => {
    if (!groupId) {
      return
    }

    if (!isLoggedIn) {
      navigate('/auth')
      return
    }

    try {
      setJoiningGroupId(groupId)
      await joinCheckGroup(Number(groupId))
      await dispatch(fetchCheckListData())
    } catch (joinError) {
      console.error('join check group error:', joinError)
    } finally {
      setJoiningGroupId(undefined)
    }
  }

  return (
    <Row gutter={[24, 24]} align="top" className="check-in-page">
      <Col xs={24} lg={18}>
        <div className="hero-banner check-in-hero card-surface">
          <div>
            <div className="hero-banner__eyebrow">看看大家今天都完成了什么打卡</div>
            <h1 className="hero-banner__title">
              在这里记录学习进度，也可以浏览其他人的每日复盘，从别人的打卡文章里获得方法、节奏和灵感。
            </h1>
            <Space wrap>
              <Button
                type="primary"
                size="large"
                className="check-in-hero__action-btn"
                onClick={() => {
                  if (!isLoggedIn) {
                    navigate('/auth')
                    return
                  }
                  navigate('/check-in/editor')
                }}
              >
                写今日打卡
              </Button>
              <Button size="large" className="soft-button" onClick={() => navigate('/practice')}>
                去刷题
              </Button>
            </Space>
          </div>
          <div className="hero-banner__glow" />
        </div>

        {error ? <Alert type="error" showIcon message={error} className="check-in-alert" /> : null}

        <Space orientation="vertical" size={20} className="full-width check-in-feed-wrap">
          <div className="side-card__title check-in-feed-title">最新打卡动态</div>
          {loading ? (
            <Card className="content-card" variant="borderless">
              <div className="check-in-loading"><Spin /></div>
            </Card>
          ) : feedPosts.length ? (
            <Space orientation="vertical" size={20} className="full-width">
              {checkFeedItems.map((item) => (
                <div
                  key={item.key}
                  className={item.detailPath ? 'check-in-feed-card is-clickable' : 'check-in-feed-card'}
                  onClick={() => {
                    if (item.detailPath) {
                      navigate(item.detailPath)
                    }
                  }}
                  onKeyDown={(event) => {
                    if (item.detailPath && (event.key === 'Enter' || event.key === ' ')) {
                      event.preventDefault()
                      navigate(item.detailPath)
                    }
                  }}
                  role={item.detailPath ? 'button' : undefined}
                  tabIndex={item.detailPath ? 0 : -1}
                >
                  <FeedCard
                    post={item.post}
                    disableLink
                  />
                </div>
              ))}
            </Space>
          ) : (
            <Card className="content-card" variant="borderless">
              <Empty description="暂时还没有打卡内容" />
            </Card>
          )}
        </Space>
      </Col>

      <Col xs={24} lg={6} className="check-in-page__side">
        <Space orientation="vertical" size={20} className="full-width">
          <Card className="side-card" variant="borderless">
            <div className="side-card__title">
              <FireOutlined /> 打卡概览
            </div>
            <div className="stats-grid">
              {checkInStats.map((item) => (
                <div key={item.label} className="stats-grid__item">
                  <div className="stats-grid__value">{item.value}</div>
                  <div className="stats-grid__label">{item.label}</div>
                </div>
              ))}
            </div>
          </Card>

          <Card className="side-card" variant="borderless">
            <div className="side-card__title">
              <TeamOutlined /> 打卡小组
            </div>
            <Space direction="vertical" size={12} className="full-width">
              {groups.length ? groups.slice(0, 4).map((group) => (
                <div key={group.id} className="check-in-group-item">
                  <div>
                    <div className="check-in-group-item__title">{group.groupName || '未命名小组'}</div>
                    {group.creatorName ? (
                      <div className="check-in-group-item__creator">
                        <span>创建者 {group.creatorName}</span>
                        {getGroupCreatorBadges(group).map((badge) => (
                          <Tag key={badge.key} color={badge.color} className="check-in-group-item__badge">
                            {badge.label}
                          </Tag>
                        ))}
                      </div>
                    ) : null}
                    <div className="check-in-group-item__meta">
                      <Tag color="blue">成员 {group.memberCount ?? 0}</Tag>
                      <Tag color="purple">打卡 {group.articleCount ?? 0}</Tag>
                    </div>
                  </div>
                  <Button size="small" loading={joiningGroupId === group.id} onClick={() => void handleJoinGroup(group.id)}>
                    加入
                  </Button>
                </div>
              )) : <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无公开小组" />}
            </Space>
          </Card>
        </Space>
      </Col>
    </Row>
  )
}
