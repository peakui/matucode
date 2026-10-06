import {
  ArrowLeftOutlined,
  CalendarOutlined,
  CommentOutlined,
  DeleteOutlined,
  EditOutlined,
  EyeOutlined,
  HeartFilled,
  HeartOutlined,
  LikeFilled,
  LikeOutlined,
  ShareAltOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { Alert, Avatar, Button, Card, Col, Divider, Empty, Input, Modal, Row, Skeleton, Space, Tag, message } from 'antd'
import axios from 'axios'
import MDEditor from '@uiw/react-md-editor'
import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { collectPost, createComment, deleteComment, deletePost, getPostDetail, likeComment, likePost, listComments, sharePost, uncollectPost, unlikeComment, unlikePost } from '../../../api/post.ts'
import type { CommentVO, PostDetailVO } from '../../../api/type/postTypings.ts'
import { ShareLink } from '../../../components/ShareLink'
import { useAppSelector } from '../../../store/hooks'
import { getAvatarWithFallback } from '../../../utils/avatar'
import { markdownImageComponents } from '../../../utils/markdownImageComponents'
import { isVipAuthor } from '../../../utils/permissions'
import './page.scss'

const formatPublishTime = (value?: string) => {
  if (!value) return '暂未发布'
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
  authorAvatar?: string
  userAvatar?: string
  userAvatarUrl?: string
  headImgUrl?: string
}) => getAvatarWithFallback(source)

const getAuthorName = (article?: PostDetailVO | null) => {
  return article?.nickname?.trim() || article?.authorName?.trim() || article?.username?.trim() || '匿名作者'
}

const isVerified = (value?: number | boolean | string | null) => value === true || value === 1 || value === '1' || value === 'true' || value === 'approved' || value === '已通过'

const getIdentityBadges = (source?: {
  schoolName?: string
  authorSchoolName?: string
  commentAuthorSchoolName?: string
  userSchoolName?: string
  schoolVerified?: number | boolean | string | null
  companyName?: string
  authorCompanyName?: string
  commentAuthorCompanyName?: string
  userCompanyName?: string
  companyVerified?: number | boolean | string | null
  authorTitle?: string
  commentAuthorTitle?: string
  userTitle?: string
  jobTitle?: string
  titleVerified?: number | boolean | string | null
  authorIsVip?: number | boolean | string | null
} | null) => {
  const companyName = source?.authorCompanyName?.trim() || source?.commentAuthorCompanyName?.trim() || source?.userCompanyName?.trim() || source?.companyName?.trim()
  const title = source?.authorTitle?.trim() || source?.commentAuthorTitle?.trim() || source?.userTitle?.trim() || source?.jobTitle?.trim()
  const schoolName = source?.authorSchoolName?.trim() || source?.commentAuthorSchoolName?.trim() || source?.userSchoolName?.trim() || source?.schoolName?.trim()
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

const patchCommentLike = (list: CommentVO[], commentId: string | number, liked: boolean): CommentVO[] =>
  list.map((item) => {
    if (String(item.id) === String(commentId)) {
      const base = item.likeCount ?? 0
      return { ...item, liked, likeCount: Math.max(base + (liked ? 1 : -1), 0) }
    }
    if (item.replies?.length) {
      return { ...item, replies: patchCommentLike(item.replies, commentId, liked) }
    }
    return item
  })

const getProfilePath = (userId?: string | number, username?: string, fallbackName?: string) => {
  const profileKey = userId != null ? String(userId) : username?.trim() || fallbackName?.trim() || ''
  return profileKey ? `/profile/${encodeURIComponent(profileKey)}` : '/profile'
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

export function ArticlePage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const currentUser = useAppSelector((state) => state.auth.userInfo)
  const isLoggedIn = useAppSelector((state) => state.auth.isLoggedIn)
  const [article, setArticle] = useState<PostDetailVO | null>(null)
  const [comments, setComments] = useState<CommentVO[]>([])
  const [loading, setLoading] = useState(true)
  const [commentsLoading, setCommentsLoading] = useState(false)
  const [error, setError] = useState('')
  const [commentContent, setCommentContent] = useState('')
  const [commentSubmitting, setCommentSubmitting] = useState(false)
  const [replyTarget, setReplyTarget] = useState<{ commentId: string | number; toUserId?: string | number; nickname: string } | null>(null)
  const [replyContent, setReplyContent] = useState('')
  const [replySubmitting, setReplySubmitting] = useState(false)
  const [likeSubmitting, setLikeSubmitting] = useState(false)
  const [commentLikeSubmitting, setCommentLikeSubmitting] = useState<Record<string, boolean>>({})
  const [commentDeleting, setCommentDeleting] = useState<Record<string, boolean>>({})
  const [collectSubmitting, setCollectSubmitting] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [shareOpen, setShareOpen] = useState(false)
  const commentSectionRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    const postId = normalizeRouteId(id)
    if (!postId) {
      setArticle(null)
      setComments([])
      setError('文章不存在或参数无效')
      setLoading(false)
      return
    }

    let cancelled = false
    const fetchArticle = async () => {
      try {
        setLoading(true)
        setError('')
        const [articleData, commentsData] = await Promise.all([getPostDetail(postId), listComments(postId)])
        if (!cancelled) {
          setArticle(articleData)
          setComments(commentsData)
        }
      } catch (fetchError) {
        console.error('get article detail error:', fetchError)
        if (!cancelled) {
          setArticle(null)
          setComments([])
          setError(getAxiosErrorMessage(fetchError, '文章加载失败，请稍后重试'))
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    void fetchArticle()
    return () => {
      cancelled = true
    }
  }, [id])

  const tags = useMemo(() => article?.tags?.filter(Boolean) ?? [], [article?.tags])
  const displayTitle = article?.title || '未命名文章'
  const publishTime = formatPublishTime(article?.publishedAt || article?.createdAt)
  const authorName = getAuthorName(article)
  const authorBadges = getIdentityBadges(article)
  const categoryName = article?.categoryName || '未分类'
  const authorDescription = article?.signature?.trim() || '这个人很低调，还没有填写个性签名。'
  const content = article?.content || '暂无正文内容'
  const displayedComments = comments.length ? comments : article?.recentComments || []
  const currentUserName = currentUser?.nickname?.trim() || currentUser?.username?.trim() || '当前用户'
  const currentUserAvatar = getAvatar(currentUserName, currentUser || undefined)
  const commentNameMap = useMemo(() => {
    const map: Record<string, string> = {}
    const walk = (list: CommentVO[]) => {
      list.forEach((comment) => {
        if (comment.userId != null) {
          map[String(comment.userId)] = comment.userName || '匿名用户'
        }
        if (comment.replies?.length) walk(comment.replies)
      })
    }
    walk(displayedComments)
    return map
  }, [displayedComments])
  const totalCommentCount = useMemo(() => {
    const count = (list: CommentVO[]): number =>
      list.reduce((sum, comment) => sum + 1 + (comment.replies?.length ? count(comment.replies) : 0), 0)
    return count(displayedComments)
  }, [displayedComments])

  const handleRefreshComments = async () => {
    const postId = normalizeRouteId(id)
    if (!postId) return
    try {
      setCommentsLoading(true)
      setComments(await listComments(postId))
    } catch (refreshError) {
      console.error('refresh comments error:', refreshError)
      message.error('评论加载失败，请稍后重试')
    } finally {
      setCommentsLoading(false)
    }
  }

  const handleSubmitComment = async () => {
    const postId = normalizeRouteId(id)
    const contentValue = commentContent.trim()
    if (!isLoggedIn) {
      message.warning('请先登录后再评论')
      navigate('/auth')
      return
    }
    if (!postId) return message.warning('文章参数无效')
    if (!contentValue) return message.warning('请输入评论内容')

    try {
      setCommentSubmitting(true)
      await createComment(postId, { content: contentValue })
      setCommentContent('')
      message.success('评论成功')
      await handleRefreshComments()
      setArticle((prev) => (prev ? { ...prev, commentCount: (prev.commentCount ?? 0) + 1 } : prev))
    } catch (submitError) {
      console.error('create comment error:', submitError)
      message.error('评论失败，请稍后重试')
    } finally {
      setCommentSubmitting(false)
    }
  }

  const handleSubmitReply = async () => {
    const postId = normalizeRouteId(id)
    const contentValue = replyContent.trim()
    if (!isLoggedIn) {
      message.warning('请先登录后再回复')
      navigate('/auth')
      return
    }
    if (!postId) return message.warning('文章参数无效')
    if (!replyTarget) return
    if (!contentValue) return message.warning('请输入回复内容')

    try {
      setReplySubmitting(true)
      await createComment(postId, { content: contentValue, parentId: replyTarget.commentId, replyToUserId: replyTarget.toUserId })
      setReplyContent('')
      setReplyTarget(null)
      message.success('回复成功')
      await handleRefreshComments()
      setArticle((prev) => (prev ? { ...prev, commentCount: (prev.commentCount ?? 0) + 1 } : prev))
    } catch (replyError) {
      console.error('create reply error:', replyError)
      message.error('回复失败，请稍后重试')
    } finally {
      setReplySubmitting(false)
    }
  }

  const handleToggleLike = async () => {
    const postId = normalizeRouteId(id)
    if (!isLoggedIn) {
      message.warning('请先登录后再点赞')
      navigate('/auth')
      return
    }
    if (!postId || !article) return message.warning('文章参数无效')
    try {
      setLikeSubmitting(true)
      if (article.liked) {
        await unlikePost(postId)
        setArticle({ ...article, liked: false, likeCount: Math.max((article.likeCount ?? 1) - 1, 0) })
        return message.success('已取消点赞')
      }
      await likePost(postId)
      setArticle({ ...article, liked: true, likeCount: (article.likeCount ?? 0) + 1 })
      message.success('点赞成功')
    } catch (likeError) {
      console.error('toggle like error:', likeError)
      message.error('操作失败，请稍后重试')
    } finally {
      setLikeSubmitting(false)
    }
  }

  const handleToggleCommentLike = async (comment: CommentVO) => {
    const postId = normalizeRouteId(id)
    if (!isLoggedIn) {
      message.warning('请先登录后再点赞')
      navigate('/auth')
      return
    }
    if (!postId || comment.id == null) return
    const key = String(comment.id)
    const liked = !!comment.liked
    try {
      setCommentLikeSubmitting((prev) => ({ ...prev, [key]: true }))
      if (liked) {
        await unlikeComment(postId, comment.id)
      } else {
        await likeComment(postId, comment.id)
      }
      setComments((prev) => patchCommentLike(prev, comment.id as string | number, !liked))
    } catch (likeError) {
      console.error('toggle comment like error:', likeError)
      message.error('操作失败，请稍后重试')
    } finally {
      setCommentLikeSubmitting((prev) => {
        const next = { ...prev }
        delete next[key]
        return next
      })
    }
  }

  const handleDeleteComment = async (comment: CommentVO) => {
    const postId = normalizeRouteId(id)
    if (!postId || comment.id == null) return
    if (!isLoggedIn) {
      message.warning('请先登录后再删除评论')
      navigate('/auth')
      return
    }
    const confirmed = await new Promise<boolean>((resolve) => {
      Modal.confirm({
        title: '确认删除这条评论？',
        content: comment.replies?.length ? '删除后该评论及其回复将不可恢复。' : '删除后该评论将不可恢复。',
        okText: '确认删除',
        cancelText: '取消',
        okButtonProps: { danger: true },
        onOk: () => resolve(true),
        onCancel: () => resolve(false),
      })
    })
    if (!confirmed) return
    const key = String(comment.id)
    try {
      setCommentDeleting((prev) => ({ ...prev, [key]: true }))
      await deleteComment(postId, comment.id)
      if (replyTarget && String(replyTarget.commentId) === key) {
        setReplyTarget(null)
        setReplyContent('')
      }
      message.success('评论已删除')
      const [commentsData, articleData] = await Promise.all([listComments(postId), getPostDetail(postId)])
      setComments(commentsData)
      setArticle(articleData)
    } catch (deleteError) {
      console.error('delete comment error:', deleteError)
      message.error('删除失败，请稍后重试')
    } finally {
      setCommentDeleting((prev) => {
        const next = { ...prev }
        delete next[key]
        return next
      })
    }
  }

  const handleToggleCollect = async () => {
    const postId = normalizeRouteId(id)
    if (!isLoggedIn) {
      message.warning('请先登录后再收藏')
      navigate('/auth')
      return
    }
    if (!postId || !article) return message.warning('文章参数无效')
    try {
      setCollectSubmitting(true)
      if (article.collected) {
        await uncollectPost(postId)
        setArticle({ ...article, collected: false, collectCount: Math.max((article.collectCount ?? 1) - 1, 0) })
        return message.success('已取消收藏')
      }
      await collectPost(postId)
      setArticle({ ...article, collected: true, collectCount: (article.collectCount ?? 0) + 1 })
      message.success('收藏成功')
    } catch (collectError) {
      console.error('toggle collect error:', collectError)
      message.error('收藏操作失败，请稍后重试')
    } finally {
      setCollectSubmitting(false)
    }
  }

  const handleScrollToComments = () => {
    commentSectionRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  const handleDeleteArticle = async () => {
    const postId = normalizeRouteId(id)
    if (!postId || !article?.owner) return
    const confirmed = await new Promise<boolean>((resolve) => {
      Modal.confirm({
        title: '确认删除这篇文章？',
        content: `删除后无法恢复：${article.title || '未命名文章'}`,
        okText: '确认删除',
        cancelText: '取消',
        okButtonProps: { danger: true },
        onOk: () => resolve(true),
        onCancel: () => resolve(false),
      })
    })
    if (!confirmed) return
    try {
      setDeleting(true)
      await deletePost(postId)
      message.success('文章删除成功')
      navigate('/profile')
    } catch (deleteError) {
      console.error('delete article error:', deleteError)
      message.error('文章删除失败，请稍后重试')
    } finally {
      setDeleting(false)
    }
  }

  const handleShared = () => {
    const postId = normalizeRouteId(id)
    if (!postId) return
    void sharePost(postId)
      .then(() => setArticle((prev) => (prev ? { ...prev, shareCount: (prev.shareCount ?? 0) + 1 } : prev)))
      .catch((shareError) => {
        console.error('share article error:', shareError)
      })
  }

  if (loading) return <Card className="content-card" variant="borderless"><Skeleton active avatar paragraph={{ rows: 10 }} /></Card>
  if (error) return <Card className="content-card" variant="borderless"><Space direction="vertical" size={16} className="full-width"><Alert type="error" message={error} showIcon /><Space><Button icon={<ArrowLeftOutlined />} onClick={() => navigate(-1)}>返回</Button><Button type="primary" onClick={() => window.location.reload()}>重新加载</Button></Space></Space></Card>
  if (!article) return <Card className="content-card" variant="borderless"><Empty description="文章不存在" /></Card>

  const renderCommentItem = (comment: CommentVO) => {
    const isReplying = replyTarget != null && String(replyTarget.commentId) === String(comment.id)
    const replyToName = comment.parentId != null && comment.replyToUserId != null ? commentNameMap[String(comment.replyToUserId)] : undefined
    return (
      <div key={comment.id} className="article-comment-thread">
        <div className="article-comment-item">
          <Avatar size={42} src={getAvatar(comment.userName, { avatarUrl: comment.avatarUrl, userAvatar: comment.userAvatar })} className="article-comment-avatar-link" onClick={() => navigate(getProfilePath(comment.userId, comment.userName, comment.userName))} />
          <div className="article-comment-item__body article-comment-item__body--plain">
            <div className="article-comment-item__top">
              <span className="article-comment-item__name">{comment.userName || '匿名用户'}</span>
              {getIdentityBadges(comment).map((badge) => <Tag key={badge.key} color={badge.color} className="article-author-badge">{badge.label}</Tag>)}
            </div>
            <div className="article-comment-item__content">
              {replyToName ? <span className="article-comment-item__reply-to" onClick={() => navigate(getProfilePath(comment.replyToUserId, replyToName, replyToName))}>回复 @{replyToName}：</span> : null}
              {comment.content || '暂无评论内容'}
            </div>
            <div className="article-comment-item__footer">
              <span className="article-comment-item__time">{formatPublishTime(comment.createdAt)}</span>
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
                  setReplyTarget({ commentId: comment.id ?? '', toUserId: comment.userId, nickname: comment.userName || '匿名用户' })
                  setReplyContent('')
                }}
              >
                回复
              </Button>
              {comment.userId === article?.userId || comment.owner ? <Button type="text" danger size="small" icon={<DeleteOutlined />} loading={!!commentDeleting[String(comment.id)]} onClick={() => void handleDeleteComment(comment)} /> : null}
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
        {comment.replies?.length ? (
          <div className="article-comment-replies">{comment.replies.map(renderCommentItem)}</div>
        ) : null}
      </div>
    )
  }

  return (
    <div className="article-detail-shell">
      <div className="article-floating-rail">
        <div className={`article-floating-action ${article.liked ? 'is-active' : ''}`} onClick={() => void handleToggleLike()} role="button" tabIndex={0}>
          {article.liked ? <LikeFilled /> : <LikeOutlined />}
          <strong>点赞</strong>
          <span>{article.likeCount ?? 0}</span>
        </div>
        <div className={`article-floating-action ${article.collected ? 'is-active' : ''}`} onClick={() => void handleToggleCollect()} role="button" tabIndex={0}>
          {article.collected ? <HeartFilled /> : <HeartOutlined />}
          <strong>收藏</strong>
          <span>{article.collectCount ?? 0}</span>
        </div>
        <div className="article-floating-action" onClick={handleScrollToComments} role="button" tabIndex={0}>
          <CommentOutlined />
          <strong>评论</strong>
          <span>{article.commentCount ?? 0}</span>
        </div>
        <div className="article-floating-action" onClick={() => setShareOpen(true)} role="button" tabIndex={0}>
          <ShareAltOutlined />
          <strong>分享</strong>
          <span>{article.shareCount ?? 0}</span>
        </div>
      </div>

      <Row gutter={[24, 24]} className="article-page article-page--detail">
        <Col xs={24} xxl={18}>
          <Card className="content-card detail-article-card article-main-card" variant="borderless">
            <div className="article-back-row"><Button icon={<ArrowLeftOutlined />} className="article-back-btn" onClick={() => navigate(-1)}>返回列表</Button>{article.owner ? <div className="article-owner-actions"><Button icon={<EditOutlined />} onClick={() => navigate(`/article/editor/${article.id}`)}>编辑文章</Button><Button danger icon={<DeleteOutlined />} loading={deleting} onClick={() => void handleDeleteArticle()}>删除文章</Button></div> : null}</div>
          <div className="article-head-block"><Space wrap><Tag color="blue" variant="filled">{categoryName}</Tag><Tag color={article.status === 0 ? 'gold' : 'green'} variant="filled">{article.status === 0 ? '草稿' : '已发布'}</Tag>{article.isTop ? <Tag color="red" variant="filled">置顶</Tag> : null}{article.isEssence ? <Tag color="purple" variant="filled">精选</Tag> : null}{tags.map((tag) => <Tag key={tag}>{tag}</Tag>)}</Space><h1 className="detail-page__title article-page__title">{displayTitle}</h1><div className="article-meta-row article-meta-row--rich"><Space size={16} wrap><span><UserOutlined /> {authorName}{authorBadges.map((badge) => <Tag key={badge.key} color={badge.color} className="article-author-badge">{badge.label}</Tag>)}</span><span><CalendarOutlined /> {publishTime}</span><span><EyeOutlined /> {article.viewCount ?? 0} 阅读</span></Space></div></div>
          <Divider />
          <div className="article-content-block article-content-block--markdown" data-color-mode="light"><MDEditor.Markdown source={content} className="article-preview-markdown" components={markdownImageComponents} /></div>
          <Divider />
          <div className="article-action-row"><Button type={article.liked ? 'primary' : 'default'} icon={article.liked ? <LikeFilled /> : <LikeOutlined />} loading={likeSubmitting} onClick={() => void handleToggleLike()}>点赞 {article.likeCount ?? 0}</Button><Button type={article.collected ? 'primary' : 'default'} icon={article.collected ? <HeartFilled /> : <HeartOutlined />} loading={collectSubmitting} onClick={() => void handleToggleCollect()}>收藏 {article.collectCount ?? 0}</Button><Button icon={<CommentOutlined />}>评论 {article.commentCount ?? 0}</Button><Button icon={<ShareAltOutlined />} onClick={() => setShareOpen(true)}>分享 {article.shareCount ?? 0}</Button></div>
          <Divider />
          <div className="article-comment-section" ref={commentSectionRef}>
            <div className="article-comment-section__summary">
              <div className="article-comment-section__count">{totalCommentCount}个评论</div>
            </div>
            <div className="article-comment-editor">
              <Avatar size={48} src={currentUserAvatar} />
              <div className="article-comment-editor__main">
                <div className="article-editor-label">发表评论</div>
                <Input.TextArea value={commentContent} onChange={(event) => setCommentContent(event.target.value)} placeholder="快来和大家讨论吧～" rows={5} maxLength={500} showCount />
                <div className="article-comment-section__actions article-comment-section__actions--editor">
                  <Button className='article-send-btn' type="primary" loading={commentSubmitting} onClick={() => void handleSubmitComment()}>发布</Button>
                </div>
              </div>
            </div>
            <Divider />
            <div className="article-comment-section__header"><div><div className="article-side-title">全部评论</div><div className="article-comment-list-tip">下面是评论内容</div></div><Button size="small" onClick={() => void handleRefreshComments()} loading={commentsLoading}>刷新评论</Button></div>{displayedComments.length ? <div className="article-comment-list">{displayedComments.map(renderCommentItem)}</div> : <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无评论，快来抢沙发" />}</div>
        </Card>
        </Col>
        <Col xs={24} xxl={6} className="article-side-col">
          <Space direction="vertical" size={16} className="full-width">
            <Card className="content-card article-side-card article-author-card" variant="borderless" onClick={() => article.userId != null && navigate(`/profile/${encodeURIComponent(String(article.userId))}`)}>
              <div className="article-side-title">作者信息</div>
              <Space align="start" size={14}>
                <Avatar size={56} src={getAvatar(authorName, article)} />
                <div>
                  <div className="active-user__name">{authorName}{authorBadges.map((badge) => <Tag key={badge.key} color={badge.color} className="article-author-badge">{badge.label}</Tag>)}</div>
                  <div className="article-author-desc article-author-desc--clamp">{authorDescription}</div>
                </div>
              </Space>
            </Card>
            <Card className="content-card article-side-card" variant="borderless">
              <div className="article-side-title">文章信息</div>
              <ul className="bullet-list compact-list article-toc-list">
                <li>字数：{article.wordCount ?? 0}</li>
                <li>预计阅读：{article.readTime ?? 0} 分钟</li>
                <li>版本号：{article.version ?? 1}</li>
                <li>最后更新：{formatPublishTime(article.updatedAt)}</li>
              </ul>
            </Card>
          </Space>
        </Col>
      </Row>
      <ShareLink
        open={shareOpen}
        url={window.location.href}
        title={article.title}
        count={article.shareCount ?? 0}
        onClose={() => setShareOpen(false)}
        onShared={handleShared}
      />
    </div>
  )
}
