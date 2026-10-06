import { getAvatarWithFallback } from '../../utils/avatar'
import { isVipAuthor } from '../../utils/permissions'
import { extractFirstImageUrl, toPlainSummary } from '../../utils/markdownContent'
import './FeedCard.scss'

import {
  ClockCircleOutlined,
  CommentOutlined,
  EllipsisOutlined,
  LikeOutlined,
  ShareAltOutlined,
} from '@ant-design/icons'
import { Avatar, Button, Card, Image, Space, Tag } from 'antd'
import type { ReactNode } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import type { PostListItemVO } from '../../api/type/postTypings'

interface FeedCardProps {
  post: PostListItemVO
  extraActions?: ReactNode
  disableLink?: boolean
  detailPath?: string
}

const formatTime = (value?: string) => {
  if (!value) {
    return '刚刚发布'
  }

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return value
  }

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

const getAuthorName = (post: PostListItemVO) => {
  return post.nickname?.trim() || post.authorName?.trim() || post.username?.trim() || '匿名作者'
}

const isVerified = (value?: number | boolean | string | null) => value === true || value === 1 || value === '1' || value === 'true' || value === 'approved' || value === '已通过'

const getAuthorBadges = (post: PostListItemVO) => {
  const schoolName = post.authorSchoolName?.trim() || post.schoolName?.trim()
  const companyName = post.authorCompanyName?.trim() || post.companyName?.trim()
  const title = post.authorTitle?.trim() || post.userTitle?.trim() || post.jobTitle?.trim()
  const companyVerified = Boolean(companyName) || isVerified(post.companyVerified)
  const titleVerified = Boolean(title) || isVerified(post.titleVerified)
  const schoolVerified = Boolean(schoolName) || isVerified(post.schoolVerified)

  return [
    ...(isVipAuthor(post) ? [{ key: 'vip', label: 'VIP', verified: true, color: 'gold' }] : []),
    { key: 'company', label: companyName || (companyVerified ? '企业认证' : ''), verified: companyVerified, color: 'blue' },
    { key: 'school', label: schoolName || (schoolVerified ? '学校认证' : ''), verified: schoolVerified, color: 'green' },
    { key: 'title', label: title || (titleVerified ? '身份认证' : ''), verified: titleVerified, color: 'purple' },
  ].filter((item) => item.verified && item.label)
}

export function FeedCard({ post, extraActions, disableLink = false, detailPath }: FeedCardProps) {
  const navigate = useNavigate()
  const authorName = getAuthorName(post)
  const authorBadges = getAuthorBadges(post)
  const title = post.title || '未命名文章'
  const summaryText = toPlainSummary(post.summary)
  const coverImage = post.coverImage || extractFirstImageUrl(post.summary)
  const coverImages = coverImage ? [coverImage] : []
  const displayTags = post.tags?.slice(0, 3) ?? []
  const normalizedPostId = typeof post.id === 'number' ? post.id : Number(post.id)
  const hasValidDefaultPostId = Number.isFinite(normalizedPostId) && normalizedPostId > 0
  const hasCustomDetailPath = Boolean(detailPath)
  const targetPath = detailPath || (hasValidDefaultPostId ? `/article/${normalizedPostId}` : '')

  const cardContent = (
    <Card className="feed-card" variant="borderless">
      <div className="feed-card__header">
        <Space align="start" size={14} className="feed-card__author-block">
          <button
            type="button"
            className="feed-card__avatar-link"
            onClick={(event) => {
              event.stopPropagation()
              if (post.userId != null && String(post.userId).trim() !== '') {
                navigate(`/profile/${encodeURIComponent(String(post.userId))}`)
                return
              }
              navigate(`/profile/${encodeURIComponent(authorName)}`)
            }}
          >
            <Avatar size={52} src={getAvatarWithFallback(post)} />
          </button>
          <div className="feed-card__author-info">
            <div className="feed-card__author-row">
              <span className="feed-card__author-name">{authorName}</span>
              {authorBadges.map((badge) => (
                <Tag key={badge.key} color={badge.color} className="feed-card__author-badge">
                  {badge.label}
                </Tag>
              ))}
            </div>
            <div className="feed-card__meta">
              <ClockCircleOutlined /> {formatTime(post.publishedAt || post.createdAt)}
            </div>
          </div>
        </Space>
        {extraActions || <Button type="text" icon={<EllipsisOutlined />} />}
      </div>

      <div className="feed-card__body">
        <div className="detail-link-title feed-card__title">{title}</div>
        {summaryText ? <div className="feed-card__content">{summaryText}</div> : null}
        {displayTags.length || post.categoryName ? (
          <div className="feed-card__tags">
            {post.categoryName ? (
              <Tag color="blue" variant="filled">
                {post.categoryName}
              </Tag>
            ) : null}
            {displayTags.map((tag) => (
              <Tag key={tag} color="gold">
                {tag}
              </Tag>
            ))}
          </div>
        ) : null}
      </div>

      {coverImages.length > 0 ? (
        <div className={`feed-card__gallery images-${coverImages.length}`}>
          {coverImages.map((image, index) => (
            <Image
              key={`${image}-${index}`}
              src={image}
              alt={authorName}
              preview={false}
              className="feed-card__image"
            />
          ))}
        </div>
      ) : null}

      <div className="feed-card__footer">
        <Space size={24}>
          <span>
            <LikeOutlined /> {post.likeCount ?? 0}
          </span>
          <span>
            <CommentOutlined /> {post.commentCount ?? 0}
          </span>
          <span>
            <ShareAltOutlined /> 分享 {post.shareCount ?? 0}
          </span>
        </Space>
      </div>
    </Card>
  )

  if (disableLink || (!hasCustomDetailPath && !hasValidDefaultPostId)) {
    return <div className="feed-card-link">{cardContent}</div>
  }

  return (
    <Link to={targetPath} className="feed-card-link">
      {cardContent}
    </Link>
  )
}
