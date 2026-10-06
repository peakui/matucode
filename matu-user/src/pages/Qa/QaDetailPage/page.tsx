import {
  ArrowLeftOutlined,
  CalendarOutlined,
  CheckCircleOutlined,
  CommentOutlined,
  EyeOutlined,
  LikeFilled,
  LikeOutlined,
  MessageOutlined,
  PushpinOutlined,
  ShareAltOutlined,
  StarOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { Alert, Avatar, Button, Card, Col, Divider, Empty, Input, Row, Skeleton, Space, Tag, message } from 'antd'
import MDEditor from '@uiw/react-md-editor'
import { useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { createAnswer, createQaComment, getQuestionDetail, likeQaComment, listQaComments, listQuestionAnswers, acceptAnswer, shareQuestion, unlikeQaComment } from '../../../api/qa.ts'
import type { QaAnswerVO, QaCommentVO, QaQuestionDetailVO } from '../../../api/type/qaTypings.ts'
import { ShareLink } from '../../../components/ShareLink'
import { useAppSelector } from '../../../store/hooks'
import { getAvatarWithFallback } from '../../../utils/avatar'
import { isAdminUser, isVipAuthor } from '../../../utils/permissions'
import { markdownImageComponents } from '../../../utils/markdownImageComponents'
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

const getAvatar = (_name?: string, avatarUrl?: string, userAvatar?: string, userAvatarUrl?: string, headImgUrl?: string) => getAvatarWithFallback({ avatarUrl, userAvatar, userAvatarUrl, headImgUrl })

const getAuthorName = (question?: QaQuestionDetailVO | null) => {
  return question?.nickname?.trim() || question?.username?.trim() || '匿名用户'
}

const isVerified = (value?: number | boolean | string | null) => value === true || value === 1 || value === '1' || value === 'true' || value === 'approved' || value === '已通过'

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
  authorIsVip?: number | boolean | string | null
}) => {
  const companyName = source?.authorCompanyName?.trim() || source?.userCompanyName?.trim() || source?.companyName?.trim()
  const title = source?.authorTitle?.trim() || source?.userTitle?.trim() || source?.jobTitle?.trim()
  const schoolName = source?.authorSchoolName?.trim() || source?.userSchoolName?.trim() || source?.schoolName?.trim()
  const companyVerified = Boolean(companyName) || isVerified(source?.companyVerified)
  const titleVerified = Boolean(title) || isVerified(source?.titleVerified)
  const schoolVerified = Boolean(schoolName) || isVerified(source?.schoolVerified)

  return [
    ...(isVipAuthor(source) ? [{ key: 'vip', label: 'VIP', verified: true, color: 'gold' }] : []),
    { key: 'company', label: companyName || (companyVerified ? '企业认证' : ''), verified: companyVerified, color: 'blue' },
    { key: 'school', label: schoolName || (schoolVerified ? '学校认证' : ''), verified: schoolVerified, color: 'green' },
    { key: 'title', label: title || (titleVerified ? '身份认证' : ''), verified: titleVerified, color: 'purple' },
  ].filter((item) => item.verified && item.label)
}

const normalizeRouteId = (value?: string) => value?.trim() || ''

const patchCommentLike = (list: QaCommentVO[], commentId: string | number, liked: boolean): QaCommentVO[] =>
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

const getProfilePath = (userId?: string | number) => {
  return userId != null ? `/profile/${encodeURIComponent(String(userId))}` : '/profile'
}

const isSameUser = (left?: string | number, right?: string | number) => {
  if (left == null || right == null) {
    return false
  }

  return String(left) === String(right)
}

const getRequestErrorMessage = (error: unknown, fallback: string) => {
  if (error && typeof error === 'object' && 'response' in error) {
    const responseData = (error as { response?: { data?: { message?: string; msg?: string; error?: string } | string } }).response?.data
    if (typeof responseData === 'string' && responseData) return responseData
    if (responseData && typeof responseData === 'object') return responseData.message || responseData.msg || responseData.error || fallback
  }
  if (error instanceof Error && error.message) return error.message
  return fallback
}

const getQuestionIdCandidates = (value?: string) => {
  const normalized = normalizeRouteId(value)
  if (!normalized) {
    return []
  }

  const candidates = [normalized]
  const numericValue = Number(normalized)

  if (Number.isFinite(numericValue)) {
    candidates.push(String(numericValue))
  }

  return Array.from(new Set(candidates))
}

export function QaDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [question, setQuestion] = useState<QaQuestionDetailVO | null>(null)
  const [answers, setAnswers] = useState<QaAnswerVO[]>([])
  const [comments, setComments] = useState<QaCommentVO[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [commentContent, setCommentContent] = useState('')
  const [answerContent, setAnswerContent] = useState('')
  const [commentSubmitting, setCommentSubmitting] = useState(false)
  const [replyTarget, setReplyTarget] = useState<{ commentId: string | number; toUserId?: string | number; nickname: string } | null>(null)
  const [replyContent, setReplyContent] = useState('')
  const [replySubmitting, setReplySubmitting] = useState(false)
  const [answerSubmitting, setAnswerSubmitting] = useState(false)
  const [acceptingAnswerId, setAcceptingAnswerId] = useState<string | number | null>(null)
  const [commentLikeSubmitting, setCommentLikeSubmitting] = useState<Record<string, boolean>>({})
  const [shareOpen, setShareOpen] = useState(false)
  const answerSectionRef = useRef<HTMLDivElement | null>(null)
  const commentSectionRef = useRef<HTMLDivElement | null>(null)
  const currentUser = useAppSelector((state) => state.auth.userInfo)
  const isLoggedIn = useAppSelector((state) => state.auth.isLoggedIn)

  const loadData = async (questionId: string) => {
    let lastError: unknown = null

    for (const candidateId of getQuestionIdCandidates(questionId)) {
      try {
        const [detail, answerPage, commentList] = await Promise.all([
          getQuestionDetail(candidateId),
          listQuestionAnswers(candidateId, { pageNum: 1, pageSize: 20 }),
          listQaComments(1, candidateId),
        ])

        setQuestion(detail)
        setAnswers(answerPage.records ?? detail.answers ?? [])
        setComments(commentList)
        return
      } catch (error) {
        lastError = error
      }
    }

    throw lastError instanceof Error ? lastError : new Error('问题详情加载失败')
  }

  useEffect(() => {
    const questionId = normalizeRouteId(id)
    if (!questionId) {
      setQuestion(null)
      setAnswers([])
      setComments([])
      setError('问题不存在或参数无效')
      setLoading(false)
      return
    }

    let cancelled = false

    const fetchQuestion = async () => {
      try {
        setLoading(true)
        setError('')
        await loadData(questionId)
      } catch (fetchError) {
        console.error('get qa detail error:', fetchError)
        if (!cancelled) {
          setQuestion(null)
          setAnswers([])
          setComments([])
          setError('问题详情加载失败，请稍后重试')
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    void fetchQuestion()

    return () => {
      cancelled = true
    }
  }, [id])

  const handleSubmitAnswer = async () => {
    const questionId = normalizeRouteId(id)
    const content = answerContent.trim()
    if (!isLoggedIn) {
      message.warning('请先登录后再回答')
      navigate('/auth')
      return
    }
    if (!questionId) return message.warning('问题参数无效')
    if (!content) return message.warning('请输入回答内容')

    try {
      setAnswerSubmitting(true)
      await createAnswer(questionId, { content })
      setAnswerContent('')
      await loadData(questionId)
      message.success('回答成功')
    } catch (submitError) {
      console.error('create answer error:', submitError)
      message.error('回答失败，请稍后重试')
    } finally {
      setAnswerSubmitting(false)
    }
  }

  const handleAcceptAnswer = async (answer: QaAnswerVO) => {
    const questionId = question?.id ?? normalizeRouteId(id)
    if (questionId == null || String(questionId).trim() === '') return message.warning('问题参数无效')
    if (answer.id == null || String(answer.id).trim() === '') return message.warning('回答参数无效')

    try {
      setAcceptingAnswerId(answer.id)
      await acceptAnswer(questionId, answer.id)
      setQuestion((prev) => prev ? { ...prev, status: 1, bestAnswerId: answer.id } : prev)
      setAnswers((prev) => prev.map((item) => ({
        ...item,
        isAccepted: isSameUser(item.id, answer.id) ? 1 : item.isAccepted,
      })))
      await loadData(String(questionId))
      message.success('已采纳该回答，问题已标记为已解决')
    } catch (acceptError) {
      console.error('accept qa answer error:', acceptError)
      message.error(getRequestErrorMessage(acceptError, '采纳失败，请稍后重试'))
    } finally {
      setAcceptingAnswerId(null)
    }
  }

  const handleSubmitComment = async () => {
    const questionId = normalizeRouteId(id)
    const content = commentContent.trim()
    if (!isLoggedIn) {
      message.warning('请先登录后再评论')
      navigate('/auth')
      return
    }
    if (!questionId) return message.warning('问题参数无效')
    if (!content) return message.warning('请输入评论内容')

    try {
      setCommentSubmitting(true)
      await createQaComment({ targetType: 1, targetId: questionId, content })
      setCommentContent('')
      await loadData(questionId)
      message.success('评论成功')
    } catch (submitError) {
      console.error('create qa comment error:', submitError)
      message.error('评论失败，请稍后重试')
    } finally {
      setCommentSubmitting(false)
    }
  }

  const handleScrollToAnswers = () => {
    answerSectionRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  const handleScrollToComments = () => {
    commentSectionRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  const handleToggleCommentLike = async (comment: QaCommentVO) => {
    if (!isLoggedIn) {
      message.warning('请先登录后再点赞')
      navigate('/auth')
      return
    }
    if (comment.id == null) return
    const key = String(comment.id)
    const liked = !!comment.liked
    try {
      setCommentLikeSubmitting((prev) => ({ ...prev, [key]: true }))
      if (liked) {
        await unlikeQaComment(comment.id)
      } else {
        await likeQaComment(comment.id)
      }
      setComments((prev) => patchCommentLike(prev, comment.id as string | number, !liked))
    } catch (likeError) {
      console.error('toggle qa comment like error:', likeError)
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
    const questionId = question?.id ?? normalizeRouteId(id)
    if (questionId == null || String(questionId).trim() === '') return
    void shareQuestion(questionId)
      .then(() => setQuestion((prev) => (prev ? { ...prev, shareCount: (prev.shareCount ?? 0) + 1 } : prev)))
      .catch((shareError) => {
        console.error('share qa error:', shareError)
      })
  }

  const handleSubmitReply = async () => {
    const questionId = question?.id ?? normalizeRouteId(id)
    const content = replyContent.trim()
    if (!isLoggedIn) {
      message.warning('请先登录后再回复')
      navigate('/auth')
      return
    }
    if (!questionId) return message.warning('问题参数无效')
    if (!replyTarget) return
    if (!content) return message.warning('请输入回复内容')

    try {
      setReplySubmitting(true)
      await createQaComment({ targetType: 1, targetId: questionId, parentId: replyTarget.commentId, replyToUserId: replyTarget.toUserId, content })
      setReplyContent('')
      setReplyTarget(null)
      await loadData(String(questionId))
      message.success('回复成功')
    } catch (replyError) {
      console.error('create qa reply error:', replyError)
      message.error('回复失败，请稍后重试')
    } finally {
      setReplySubmitting(false)
    }
  }

  if (loading) {
    return (
      <Card className="content-card" variant="borderless">
        <Skeleton active avatar paragraph={{ rows: 10 }} />
      </Card>
    )
  }

  if (error) {
    return (
      <Card className="content-card" variant="borderless">
        <Alert type="error" showIcon message={error} />
      </Card>
    )
  }

  if (!question) {
    return (
      <Card className="content-card" variant="borderless">
        <Empty description="问题不存在" />
      </Card>
    )
  }

  const authorName = getAuthorName(question)
  const authorBadges = getIdentityBadges(question)
  const authorDescription = question.signature?.trim() || '这个人很低调，还没有填写个性签名。'
  const currentUserName = currentUser?.nickname?.trim() || currentUser?.username?.trim() || '当前用户'
  const currentUserAvatar = getAvatar(currentUserName, currentUser?.avatarUrl)
  const canAcceptAnswer = question.status !== 1 && (isSameUser(currentUser?.userId, question.userId) || isAdminUser(currentUser))

  const commentNameMap: Record<string, string> = {}
  const collectCommentNames = (list: QaCommentVO[]) => {
    list.forEach((comment) => {
      if (comment.userId != null) commentNameMap[String(comment.userId)] = comment.nickname || comment.username || '匿名用户'
      if (comment.children?.length) collectCommentNames(comment.children)
    })
  }
  collectCommentNames(comments)

  const renderCommentItem = (comment: QaCommentVO) => {
    const isReplying = replyTarget != null && String(replyTarget.commentId) === String(comment.id)
    const replyToName = comment.parentId != null && comment.replyToUserId != null ? commentNameMap[String(comment.replyToUserId)] : undefined
    return (
      <div key={comment.id} className="article-comment-thread">
        <div className="article-comment-item">
          <Avatar size={42} src={getAvatar(comment.nickname || comment.username, comment.avatarUrl, comment.userAvatar)} className="article-comment-avatar-link" onClick={() => navigate(getProfilePath(comment.userId))} />
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

  return (
    <div className="article-detail-shell qa-detail-shell">
      <div className="article-floating-rail">
        <div className="article-floating-action" onClick={handleScrollToAnswers} role="button" tabIndex={0}>
          <MessageOutlined />
          <strong>回答</strong>
          <span>{question.answerCount ?? answers.length}</span>
        </div>
        <div className="article-floating-action" onClick={handleScrollToComments} role="button" tabIndex={0}>
          <CommentOutlined />
          <strong>评论</strong>
          <span>{comments.length}</span>
        </div>
        <div className="article-floating-action" onClick={() => setShareOpen(true)} role="button" tabIndex={0}>
          <ShareAltOutlined />
          <strong>分享</strong>
          <span>{question.shareCount ?? 0}</span>
        </div>
      </div>

      <Row gutter={[24, 24]} className="article-page article-page--detail qa-detail-page">
        <Col xs={24} xxl={18}>
          <Card className="content-card detail-article-card article-main-card" variant="borderless">
            <div className="article-back-row">
              <Button icon={<ArrowLeftOutlined />} className="article-back-btn" onClick={() => navigate(-1)}>
                返回列表
              </Button>
            </div>

            <div className="article-head-block">
            <Space wrap>
              <Tag color="blue" variant="filled">{question.categoryName || '问答'}</Tag>
              <Tag color={question.status === 1 ? 'green' : question.status === 2 ? 'red' : 'gold'} variant="filled">
                {question.status === 1 ? '已解决' : question.status === 2 ? '已关闭' : '待解决'}
              </Tag>
              {question.bountyPoints ? <Tag color="gold">悬赏 {question.bountyPoints} 积分</Tag> : null}
            </Space>

            <h1 className="detail-page__title article-page__title">{question.title || '未命名问题'}</h1>

            <div className="article-meta-row article-meta-row--rich">
              <Space size={16} wrap>
                <span><UserOutlined /> {authorName}{authorBadges.map((badge) => <Tag key={badge.key} color={badge.color} className="article-author-badge">{badge.label}</Tag>)}</span>
                <span><CalendarOutlined /> {formatTime(question.createdAt)}</span>
                <span><EyeOutlined /> {question.viewCount ?? 0} 浏览</span>
              </Space>
            </div>
          </div>

          <Divider />

          <div className="article-content-block article-content-block--markdown" data-color-mode="light">
            <MDEditor.Markdown source={question.content || '暂无问题描述'} className="article-preview-markdown" components={markdownImageComponents} />
          </div>

          <Divider />

          <div ref={answerSectionRef} className="article-comment-editor qa-answer-editor">
            <Avatar size={48} src={currentUserAvatar} />
            <div className="article-comment-editor__main">
              <div className="article-editor-label">发布回答</div>
              <Input.TextArea
                value={answerContent}
                onChange={(event) => setAnswerContent(event.target.value)}
                placeholder="写下你的回答，尽量说明思路、原因和解决方案～"
                rows={6}
                maxLength={5000}
                showCount
              />
              <div className="article-comment-section__actions article-comment-section__actions--editor">
                <Button className="article-send-btn" type="primary" loading={answerSubmitting} onClick={() => void handleSubmitAnswer()}>
                  发布回答
                </Button>
              </div>
            </div>
          </div>

          <Divider />
          <div className="article-side-title">全部回答</div>
          {answers.length ? (
            <div className="article-comment-list">
              {answers.map((answer) => (
                <div key={answer.id} className="article-comment-item">
                  <Avatar size={42} src={getAvatar(answer.nickname || answer.username, answer.avatarUrl, answer.userAvatar)} className="article-comment-avatar-link" onClick={() => navigate(getProfilePath(answer.userId))} />
                  <div className="article-comment-item__body article-comment-item__body--plain">
                    <div className="article-comment-item__top qa-answer-item__top">
                      <div className="qa-answer-item__author">
                        <span className="article-comment-item__name">{answer.nickname || answer.username || '匿名用户'}</span>
                        {getIdentityBadges(answer).map((badge) => <Tag key={badge.key} color={badge.color} className="article-author-badge">{badge.label}</Tag>)}
                        {answer.isAccepted ? <Tag color="green" icon={<CheckCircleOutlined />}>已采纳</Tag> : null}
                      </div>
                      {canAcceptAnswer && !answer.isAccepted ? (
                        <Button
                          size="small"
                          type="primary"
                          icon={<CheckCircleOutlined />}
                          loading={isSameUser(acceptingAnswerId ?? undefined, answer.id)}
                          onClick={() => void handleAcceptAnswer(answer)}
                        >
                          采纳
                        </Button>
                      ) : null}
                    </div>
                    <div className="article-comment-item__content">{answer.content || '暂无回答内容'}</div>
                    <div className="article-comment-item__footer">
                      <span className="article-comment-item__time">{formatTime(answer.createdAt)}</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无回答" />
          )}

          <Divider />

          <div className="article-comment-section" ref={commentSectionRef}>
            <div className="article-comment-editor">
              <Avatar size={48} src={currentUserAvatar} />
              <div className="article-comment-editor__main">
                <div className="article-editor-label">发表评论</div>
                <Input.TextArea
                  value={commentContent}
                  onChange={(event) => setCommentContent(event.target.value)}
                  placeholder="快来和大家讨论吧～"
                  rows={5}
                  maxLength={500}
                  showCount
                />
                <div className="article-comment-section__actions article-comment-section__actions--editor">
                  <Button className="article-send-btn" type="primary" loading={commentSubmitting} onClick={() => void handleSubmitComment()}>
                    发布
                  </Button>
                </div>
              </div>
            </div>

            <Divider />

            <div className="article-comment-section__summary">
              <div className="article-comment-section__count">{comments.length}个评论</div>
            </div>
            {comments.length ? (
              <div className="article-comment-list">
                {comments.map(renderCommentItem)}
              </div>
            ) : (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无评论" />
            )}
          </div>
        </Card>
        </Col>

        <Col xs={24} xxl={6} className="article-side-col">
        <Space direction="vertical" size={16} className="full-width">
          <Card className="content-card article-side-card article-author-card" variant="borderless" onClick={() => navigate(getProfilePath(question.userId))}>
            <div className="article-side-title">提问者</div>
            <Space align="start" size={14}>
              <Avatar size={56} src={getAvatar(authorName, question.avatarUrl, question.userAvatar, question.userAvatarUrl, question.headImgUrl)} />
              <div>
                <div className="active-user__name">{authorName}{authorBadges.map((badge) => <Tag key={badge.key} color={badge.color} className="article-author-badge">{badge.label}</Tag>)}</div>
                <div className="article-author-desc article-author-desc--clamp">{authorDescription}</div>
              </div>
            </Space>
          </Card>

          <Card className="content-card article-side-card" variant="borderless">
            <div className="article-side-title">问题信息</div>
            <ul className="bullet-list compact-list article-toc-list">
              <li><MessageOutlined /> 回答数：{question.answerCount ?? answers.length}</li>
              <li><CommentOutlined /> 评论数：{comments.length}</li>
              <li><StarOutlined /> 关注数：{question.followCount ?? 0}</li>
              <li><PushpinOutlined /> 最佳答案：{question.bestAnswerId ? `#${question.bestAnswerId}` : '暂无'}</li>
            </ul>
          </Card>
        </Space>
      </Col>
    </Row>
    <ShareLink
      open={shareOpen}
      url={window.location.href}
      title={question.title}
      count={question.shareCount ?? 0}
      onClose={() => setShareOpen(false)}
      onShared={handleShared}
    />
    </div>
  )
}
