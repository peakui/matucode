import {
  ArrowLeftOutlined,
  CalendarOutlined,
  ClockCircleOutlined,
  CommentOutlined,
  EyeOutlined,
  LikeFilled,
  LikeOutlined,
  PushpinOutlined,
  ShareAltOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { Alert, Avatar, Button, Card, Col, Divider, Empty, Input, Row, Skeleton, Space, Tag, message } from 'antd'
import axios from 'axios'
import MDEditor from '@uiw/react-md-editor'
import { useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { createCheckComment, getCheckRecordDetail, likeCheckComment, likeCheckRecord, listCheckComments, shareCheckRecord, unlikeCheckComment, unlikeCheckRecord } from '../../../api/check.ts'
import type { CheckCommentVO, CheckRecordVO } from '../../../api/type/checkTypings.ts'
import { ShareLink } from '../../../components/ShareLink'
import { useAppSelector } from '../../../store/hooks'
import { getAvatarWithFallback } from '../../../utils/avatar'
import { markdownImageComponents } from '../../../utils/markdownImageComponents'
import { isVipAuthor } from '../../../utils/permissions'
import './page.scss'

const formatTime = (value?: string) => {
  if (!value) return '暂未记录'
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

const getAvatar = (_name?: string, source?: {
  avatarUrl?: string
  userAvatar?: string
  userAvatarUrl?: string
  headImgUrl?: string
}) => getAvatarWithFallback(source)

const getAuthorName = (record?: CheckRecordVO | null) => {
  return record?.nickname?.trim() || record?.username?.trim() || '匿名用户'
}

const patchCommentLike = (list: CheckCommentVO[], commentId: string | number, liked: boolean): CheckCommentVO[] =>
  list.map((item) => {
    if (String(item.id) === String(commentId)) {
      const base = item.likeCount ?? 0
      return { ...item, liked, likeCount: Math.max(base + (liked ? 1 : -1), 0) }
    }
    if (item.children?.length) {
      return { ...item, children: patchCommentLike(item.children, commentId, liked) }
    }
    return item
  })

const isVerified = (value?: number | boolean | string | null) => value === true || value === 1 || value === '1' || value === 'true' || value === 'approved' || value === '已通过'

const getIdentityBadges = (source?: {
  schoolName?: string
  authorSchoolName?: string
  commentAuthorSchoolName?: string
  userSchoolName?: string
  creatorSchoolName?: string
  schoolVerified?: number | boolean | string | null
  creatorSchoolVerified?: number | boolean | string | null
  companyName?: string
  authorCompanyName?: string
  commentAuthorCompanyName?: string
  userCompanyName?: string
  creatorCompanyName?: string
  companyVerified?: number | boolean | string | null
  creatorCompanyVerified?: number | boolean | string | null
  authorTitle?: string
  commentAuthorTitle?: string
  userTitle?: string
  jobTitle?: string
  creatorTitle?: string
  titleVerified?: number | boolean | string | null
  creatorTitleVerified?: number | boolean | string | null
  authorIsVip?: number | boolean | string | null
}) => {
  const companyName = source?.authorCompanyName?.trim() || source?.commentAuthorCompanyName?.trim() || source?.userCompanyName?.trim() || source?.creatorCompanyName?.trim() || source?.companyName?.trim()
  const title = source?.authorTitle?.trim() || source?.commentAuthorTitle?.trim() || source?.userTitle?.trim() || source?.jobTitle?.trim() || source?.creatorTitle?.trim()
  const schoolName = source?.authorSchoolName?.trim() || source?.commentAuthorSchoolName?.trim() || source?.userSchoolName?.trim() || source?.creatorSchoolName?.trim() || source?.schoolName?.trim()
  const companyVerified = Boolean(companyName) || isVerified(source?.companyVerified) || isVerified(source?.creatorCompanyVerified)
  const titleVerified = Boolean(title) || isVerified(source?.titleVerified) || isVerified(source?.creatorTitleVerified)
  const schoolVerified = Boolean(schoolName) || isVerified(source?.schoolVerified) || isVerified(source?.creatorSchoolVerified)

  return [
    ...(isVipAuthor(source) ? [{ key: 'vip', label: 'VIP', verified: true, color: 'gold' }] : []),
    { key: 'company', label: companyName || (companyVerified ? '企业认证' : ''), verified: companyVerified, color: 'blue' },
    { key: 'school', label: schoolName || (schoolVerified ? '学校认证' : ''), verified: schoolVerified, color: 'green' },
    { key: 'title', label: title || (titleVerified ? '身份认证' : ''), verified: titleVerified, color: 'purple' },
  ].filter((item) => item.verified && item.label)
}

const getProfilePath = (userId?: string | number) => {
  return userId != null ? `/profile/${encodeURIComponent(String(userId))}` : '/profile'
}

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

export function CheckDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const isLoggedIn = useAppSelector((state) => state.auth.isLoggedIn)
  const [record, setRecord] = useState<CheckRecordVO | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [commentContent, setCommentContent] = useState('')
  const [comments, setComments] = useState<CheckCommentVO[]>([])
  const [commentsLoading, setCommentsLoading] = useState(false)
  const [commentSubmitting, setCommentSubmitting] = useState(false)
  const [replyTarget, setReplyTarget] = useState<{ commentId: string | number; toUserId?: string | number; nickname: string } | null>(null)
  const [replyContent, setReplyContent] = useState('')
  const [replySubmitting, setReplySubmitting] = useState(false)
  const [likeSubmitting, setLikeSubmitting] = useState(false)
  const [commentLikeSubmitting, setCommentLikeSubmitting] = useState<Record<string, boolean>>({})
  const [shareOpen, setShareOpen] = useState(false)
  const commentSectionRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    const rawCheckId = id?.trim()

    if (!rawCheckId) {
      setRecord(null)
      setError('打卡不存在或参数无效')
      setLoading(false)
      return
    }

    let cancelled = false

    const fetchRecord = async () => {
      try {
        setLoading(true)
        setCommentsLoading(true)
        setError('')
        const [recordData, commentData] = await Promise.all([
          getCheckRecordDetail(rawCheckId),
          listCheckComments(rawCheckId),
        ])
        if (!cancelled) {
          setRecord(recordData)
          setComments(commentData || [])
        }
      } catch (fetchError) {
        console.error('get check detail error:', fetchError)
        if (!cancelled) {
          setRecord(null)
          setComments([])
          setError(getAxiosErrorMessage(fetchError, '打卡详情加载失败，请稍后重试'))
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
          setCommentsLoading(false)
        }
      }
    }

    void fetchRecord()

    return () => {
      cancelled = true
    }
  }, [id])

  const handleScrollToComments = () => {
    commentSectionRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  const handleToggleLike = async () => {
    const rawCheckId = id?.trim()
    if (!isLoggedIn) {
      message.warning('请先登录后再点赞')
      navigate('/auth')
      return
    }
    if (!rawCheckId || !record) {
      message.warning('打卡参数无效')
      return
    }

    try {
      setLikeSubmitting(true)

      if (record.liked) {
        await unlikeCheckRecord(rawCheckId)
        setRecord({
          ...record,
          liked: false,
          likeCount: Math.max((record.likeCount ?? 1) - 1, 0),
        })
        message.success('已取消点赞')
        return
      }

      await likeCheckRecord(rawCheckId)
      setRecord({
        ...record,
        liked: true,
        likeCount: (record.likeCount ?? 0) + 1,
      })
      message.success('点赞成功')
    } catch (likeError) {
      console.error('toggle check like error:', likeError)
      message.error('点赞操作失败，请稍后重试')
    } finally {
      setLikeSubmitting(false)
    }
  }

  const handleToggleCommentLike = async (comment: CheckCommentVO) => {
    const rawCheckId = id?.trim()
    if (!isLoggedIn) {
      message.warning('请先登录后再点赞')
      navigate('/auth')
      return
    }
    if (!rawCheckId || comment.id == null) return
    const key = String(comment.id)
    const liked = !!comment.liked
    try {
      setCommentLikeSubmitting((prev) => ({ ...prev, [key]: true }))
      if (liked) {
        await unlikeCheckComment(rawCheckId, comment.id)
      } else {
        await likeCheckComment(rawCheckId, comment.id)
      }
      setComments((prev) => patchCommentLike(prev, comment.id as string | number, !liked))
    } catch (likeError) {
      console.error('toggle check comment like error:', likeError)
      message.error('操作失败，请稍后重试')
    } finally {
      setCommentLikeSubmitting((prev) => {
        const next = { ...prev }
        delete next[key]
        return next
      })
    }
  }

  const handleShared = () => {
    const rawCheckId = id?.trim()
    if (!rawCheckId) return
    void shareCheckRecord(rawCheckId)
      .then(() => setRecord((prev) => (prev ? { ...prev, shareCount: (prev.shareCount ?? 0) + 1 } : prev)))
      .catch((shareError) => {
        console.error('share check error:', shareError)
      })
  }

  const handleRefreshComments = async () => {
    const rawCheckId = id?.trim()
    if (!rawCheckId) {
      return
    }

    try {
      setCommentsLoading(true)
      const commentData = await listCheckComments(rawCheckId)
      setComments(commentData || [])
    } catch (refreshError) {
      console.error('refresh check comments error:', refreshError)
      message.error('评论加载失败，请稍后重试')
    } finally {
      setCommentsLoading(false)
    }
  }

  const handleSubmitComment = async () => {
    const rawCheckId = id?.trim()
    const content = commentContent.trim()

    if (!isLoggedIn) {
      message.warning('请先登录后再评论')
      navigate('/auth')
      return
    }

    if (!rawCheckId) {
      message.error('打卡信息无效，无法评论')
      return
    }

    if (!content) {
      message.warning('请输入评论内容')
      return
    }

    try {
      setCommentSubmitting(true)
      await createCheckComment(rawCheckId, { content })
      setCommentContent('')
      message.success('评论发布成功')
      await handleRefreshComments()
      setRecord((current) => (current ? {
        ...current,
        commentCount: (current.commentCount || 0) + 1,
      } : current))
    } catch (submitError) {
      console.error('submit check comment error:', submitError)
      message.error('评论发布失败，请稍后重试')
    } finally {
      setCommentSubmitting(false)
    }
  }

  const handleSubmitReply = async () => {
    const rawCheckId = id?.trim()
    const content = replyContent.trim()
    if (!isLoggedIn) {
      message.warning('请先登录后再回复')
      navigate('/auth')
      return
    }
    if (!rawCheckId) {
      message.error('打卡信息无效，无法回复')
      return
    }
    if (!replyTarget) return
    if (!content) {
      message.warning('请输入回复内容')
      return
    }

    try {
      setReplySubmitting(true)
      await createCheckComment(rawCheckId, { content, parentId: replyTarget.commentId, replyToUserId: replyTarget.toUserId })
      setReplyContent('')
      setReplyTarget(null)
      message.success('回复发布成功')
      await handleRefreshComments()
      setRecord((current) => (current ? { ...current, commentCount: (current.commentCount || 0) + 1 } : current))
    } catch (replyError) {
      console.error('submit check reply error:', replyError)
      message.error('回复发布失败，请稍后重试')
    } finally {
      setReplySubmitting(false)
    }
  }

  if (loading) return <Card className="content-card" variant="borderless"><Skeleton active avatar paragraph={{ rows: 10 }} /></Card>
  if (error) return <Card className="content-card" variant="borderless"><Alert type="error" showIcon message={error} /></Card>
  if (!record) return <Card className="content-card" variant="borderless"><Empty description="打卡不存在" /></Card>

  const commentNameMap: Record<string, string> = {}
  const collectCommentNames = (list: CheckCommentVO[]) => {
    list.forEach((comment) => {
      if (comment.userId != null) commentNameMap[String(comment.userId)] = comment.nickname || comment.username || '匿名用户'
      if (comment.children?.length) collectCommentNames(comment.children)
    })
  }
  collectCommentNames(comments)

  const renderCommentItem = (comment: CheckCommentVO) => {
    const isReplying = replyTarget != null && String(replyTarget.commentId) === String(comment.id)
    const replyToName = comment.parentId != null && comment.replyToUserId != null ? commentNameMap[String(comment.replyToUserId)] : undefined
    return (
      <div key={comment.id} className="article-comment-thread">
        <div className="article-comment-item">
          <Avatar size={42} src={getAvatar(comment.username, { avatarUrl: comment.userAvatar, userAvatar: comment.userAvatar })} className="article-comment-avatar-link" onClick={() => navigate(getProfilePath(comment.userId))} />
          <div className="article-comment-item__body article-comment-item__body--plain">
            <div className="article-comment-item__top">
              <span className="article-comment-item__name">{comment.nickname || comment.username || '匿名用户'}</span>
              {getIdentityBadges(comment).map((badge) => <Tag key={badge.key} color={badge.color} className="article-author-badge">{badge.label}</Tag>)}
            </div>
            <div className="article-comment-item__content">
              {replyToName ? <span className="article-comment-item__reply-to" onClick={() => navigate(getProfilePath(comment.replyToUserId))}>回复 @{replyToName}：</span> : null}
              {comment.content || '暂无评论内容'}
            </div>
            <div className="article-comment-item__footer">
              <span className="article-comment-item__time">{formatTime(comment.createdAt)}</span>
              <Button
                type="text"
                size="small"
                className={`article-comment-like${comment.liked ? ' is-active' : ''}`}
                loading={!!commentLikeSubmitting[String(comment.id)]}
                icon={comment.liked ? <LikeFilled /> : <LikeOutlined />}
                onClick={() => void handleToggleCommentLike(comment)}
              >
                {comment.likeCount ?? 0}
              </Button>
              <Button
                type="text"
                size="small"
                onClick={() => {
                  setReplyTarget({ commentId: comment.id ?? '', toUserId: comment.userId, nickname: comment.nickname || comment.username || '匿名用户' })
                  setReplyContent('')
                }}
              >
                回复
              </Button>
            </div>
            {isReplying ? (
              <div className="article-comment-reply-editor">
                <Input.TextArea
                  value={replyContent}
                  onChange={(event) => setReplyContent(event.target.value)}
                  placeholder={`回复 @${replyTarget?.nickname || ''}`}
                  autoSize={{ minRows: 2, maxRows: 4 }}
                  maxLength={500}
                />
                <div className="article-comment-item__footer article-comment-reply-editor__actions">
                  <Button size="small" onClick={() => { setReplyTarget(null); setReplyContent('') }}>取消</Button>
                  <Button size="small" type="primary" loading={replySubmitting} onClick={() => void handleSubmitReply()}>回复</Button>
                </div>
              </div>
            ) : null}
          </div>
        </div>
        {comment.children?.length ? (
          <div className="article-comment-replies">{comment.children.map(renderCommentItem)}</div>
        ) : null}
      </div>
    )
  }

  const title = record.title || '今日打卡'
  const authorName = getAuthorName(record)
  const authorBadges = getIdentityBadges(record)
  const content = record.content || '暂无打卡内容'
  const summary = record.summary || '作者还没有填写打卡摘要。'
  const authorDescription = record.signature?.trim() || '这个人很低调，还没有填写个性签名。'

  return (
    <div className="article-detail-shell check-detail-page">
      <div className="article-floating-rail">
        <div
          className={`article-floating-action ${record.liked ? 'is-active' : ''}`}
          onClick={() => {
            if (!likeSubmitting) {
              void handleToggleLike()
            }
          }}
          role="button"
          tabIndex={0}
        >
          {record.liked ? <LikeFilled /> : <LikeOutlined />}
          <strong>点赞</strong>
          <span>{record.likeCount ?? 0}</span>
        </div>
        <div className="article-floating-action" onClick={handleScrollToComments} role="button" tabIndex={0}>
          <CommentOutlined />
          <strong>评论</strong>
          <span>{comments.length}</span>
        </div>
        <div
          className="article-floating-action"
          onClick={() => setShareOpen(true)}
          role="button"
          tabIndex={0}
        >
          <ShareAltOutlined />
          <strong>分享</strong>
          <span>{record.shareCount ?? 0}</span>
        </div>
      </div>

      <Row gutter={[24, 24]} className="article-page article-page--detail">
        <Col xs={24} xxl={18}>
          <Card className="content-card detail-article-card article-main-card" variant="borderless">
            <div className="article-back-row">
              <Button icon={<ArrowLeftOutlined />} className="article-back-btn" onClick={() => navigate(-1)}>
                返回列表
              </Button>
            </div>
            <div className="article-head-block">
              <Space wrap>
                <Tag color="blue" variant="filled">每日打卡</Tag>
                <Tag color={record.status === 1 ? 'green' : 'gold'} variant="filled">{record.status === 1 ? '已发布' : '草稿'}</Tag>
                {record.isTop ? <Tag color="red" variant="filled">置顶</Tag> : null}
                {record.isFeatured ? <Tag color="purple" variant="filled">推荐</Tag> : null}
                {record.location ? <Tag>{record.location}</Tag> : null}
                {record.learnHours !== undefined ? <Tag color="gold">{record.learnHours} 小时</Tag> : null}
              </Space>
              <h1 className="detail-page__title article-page__title">{title}</h1>
              <div className="article-meta-row article-meta-row--rich">
                <Space size={16} wrap>
                  <span><UserOutlined /> {authorName}{authorBadges.map((badge) => <Tag key={badge.key} color={badge.color} className="article-author-badge">{badge.label}</Tag>)}</span>
                  <span><CalendarOutlined /> {formatTime(record.checkTime || record.createdAt)}</span>
                  <span><EyeOutlined /> {record.viewCount ?? 0} 浏览</span>
                </Space>
              </div>
              <div className="article-summary-box">{summary}</div>
            </div>
            <Divider />
            <div className="article-content-block article-content-block--markdown" data-color-mode="light">
              <MDEditor.Markdown source={content} className="article-preview-markdown" components={markdownImageComponents} />
            </div>
            <Divider />
            <div className="article-comment-section" ref={commentSectionRef}>
              <div className="article-comment-section__summary">
                <div className="article-comment-section__count">{comments.length}个评论</div>
              </div>
              <div className="article-comment-editor">
                <Avatar size={48} src={getAvatar(authorName, record)} />
                <div className="article-comment-editor__main">
                  <Input.TextArea value={commentContent} onChange={(event) => setCommentContent(event.target.value)} placeholder="快来和大家讨论吧～" rows={5} maxLength={500} showCount />
                  <div className="article-comment-section__actions article-comment-section__actions--editor">
                    <Button className="article-send-btn" type="primary" loading={commentSubmitting} onClick={() => void handleSubmitComment()}>发布</Button>
                  </div>
                </div>
              </div>
              <Divider />
              <div className="article-comment-section__header"><div className="article-side-title">全部评论</div><Button size="small" onClick={() => void handleRefreshComments()} loading={commentsLoading}>刷新评论</Button></div>{comments.length ? <div className="article-comment-list">{comments.map(renderCommentItem)}</div> : <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无评论，快来抢沙发" />}
            </div>
          </Card>
        </Col>
        <Col xs={24} xxl={6} className="article-side-col">
          <Space direction="vertical" size={16} className="full-width">
            <Card className="content-card article-side-card article-author-card" variant="borderless" onClick={() => navigate(getProfilePath(record.userId))}>
              <div className="article-side-title">打卡作者</div>
              <Space align="start" size={14}>
                <Avatar size={56} src={getAvatar(authorName, record)} />
                <div>
                  <div className="active-user__name">{authorName}{authorBadges.map((badge) => <Tag key={badge.key} color={badge.color} className="article-author-badge">{badge.label}</Tag>)}</div>
                  <div className="article-author-desc article-author-desc--clamp">{authorDescription}</div>
                </div>
              </Space>
            </Card>
            <Card className="content-card article-side-card" variant="borderless">
              <div className="article-side-title">打卡信息</div>
              <ul className="bullet-list compact-list article-toc-list">
                <li><ClockCircleOutlined /> 学习时长：{record.learnHours ?? 0} 小时</li>
                <li><CommentOutlined /> 评论数：{comments.length}</li>
                <li><EyeOutlined /> 浏览数：{record.viewCount ?? 0}</li>
                <li><PushpinOutlined /> 打卡日期：{record.checkDate || '未设置'}</li>
              </ul>
            </Card>
          </Space>
        </Col>
      </Row>
      <ShareLink
        open={shareOpen}
        url={window.location.href}
        title={title}
        count={record.shareCount ?? 0}
        onClose={() => setShareOpen(false)}
        onShared={handleShared}
      />
    </div>
  )
}
