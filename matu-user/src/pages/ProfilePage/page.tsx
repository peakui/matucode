import {
  Avatar,
  Button,
  Card,
  Col,
  Empty,
  Modal,
  Row,
  Segmented,
  Space,
  Spin,
  Tabs,
  Tag,
  message,
} from 'antd'
import { DeleteOutlined, EditOutlined, HeartOutlined, MailOutlined, MessageOutlined, PlusOutlined, UserDeleteOutlined } from '@ant-design/icons'
import type { ReactNode } from 'react'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { FeedCard } from '../../components/FeedCard/index.tsx'
import {
  followUser,
  getCurrentUserProfile,
  getUserFollowStatus,
  getUserProfile,
  listMyCertifications,
  listMyFollowers,
  listMyFollowing,
  listUserFollowers,
  listUserFollowing,
  unfollowUser,
} from '../../api/authProfile'
import { listActiveDevelopers } from '../../api/activeDevelopers'
import type { ActiveDeveloperVO } from '../../api/activeDevelopers'
import { listCheckRecords, listLikedCheckRecords, unlikeCheckRecord } from '../../api/check'
import { createSingleConversation } from '../../api/message'
import { deletePost, listCollectedPosts, listDrafts, listLikedPosts, listPosts, uncollectPost, unlikePost } from '../../api/post'
import { listLikedQuestions, listMyQuestions, listQuestions, unvoteQuestion } from '../../api/qa'
import type { CheckRecordListItemVO } from '../../api/type/checkTypings'
import type { AuthProfileVO, CertificationRecordVO, UserFollowVO } from '../../api/type/loginTypings'
import type { PostListItemVO } from '../../api/type/postTypings'
import type { QaQuestionListItemVO } from '../../api/type/qaTypings'
import { getAvatarWithFallback, resolveAvatarUrl } from '../../utils/avatar'
import { isVipMember } from '../../utils/permissions'
import './page.scss'

type StoredUser = {
  userId?: string | number
  username?: string
  nickname?: string
  email?: string
  phone?: string
  roles?: string[]
  schoolVerified?: number
  companyVerified?: number
  titleVerified?: number
  technicalStack?: string[] | string
  avatarUrl?: string
  authorAvatar?: string
  userAvatar?: string
  userAvatarUrl?: string
  headImgUrl?: string
  signature?: string
  title?: string
  companyName?: string
  schoolName?: string
  major?: string
  grade?: string
  workYears?: number
  gender?: number
  birthday?: string
  status?: number
  lastLoginTime?: string
  lastLoginIp?: string
  createdAt?: string
  updatedAt?: string
  isVip?: number
  vipLevel?: number
  vipExpiredAt?: string | null
}

type ProfileViewUser = {
  userId?: string | number
  username: string
  nickname?: string
  email?: string
  phone?: string
  roles?: string[]
  schoolVerified?: number
  companyVerified?: number
  titleVerified?: number
  technicalStack?: string[] | string
  avatarUrl?: string
  authorAvatar?: string
  userAvatar?: string
  userAvatarUrl?: string
  headImgUrl?: string
  signature?: string
  title?: string
  companyName?: string
  schoolName?: string
  major?: string
  grade?: string
  workYears?: number
  gender?: number
  birthday?: string
  status?: number
  lastLoginTime?: string
  lastLoginIp?: string
  createdAt?: string
  updatedAt?: string
  isVip?: number
  vipLevel?: number
  vipExpiredAt?: string | null
}

const getStoredUser = (): StoredUser | null => {
  const rawUser = localStorage.getItem('codehub-user')
  if (!rawUser) return null
  try {
    return JSON.parse(rawUser) as StoredUser
  } catch {
    return null
  }
}

const syncStoredUser = (profile: AuthProfileVO) => {
  const rawStoredUser = localStorage.getItem('codehub-user')
  let currentUser: Record<string, unknown> = {}

  if (rawStoredUser) {
    try {
      currentUser = JSON.parse(rawStoredUser) as Record<string, unknown>
    } catch {
      currentUser = {}
    }
  }

  const avatarUrl = resolveAvatarUrl(profile)

  const nextUser = {
    ...currentUser,
    userId: profile.userId ?? currentUser.userId,
    username: profile.username ?? currentUser.username,
    nickname: profile.nickname ?? currentUser.nickname,
    email: profile.email ?? currentUser.email,
    phone: profile.phone ?? currentUser.phone,
    roles: profile.roles?.length ? profile.roles : currentUser.roles,
    schoolVerified: profile.schoolVerified ?? currentUser.schoolVerified,
    companyVerified: profile.companyVerified ?? currentUser.companyVerified,
    titleVerified: profile.titleVerified ?? currentUser.titleVerified,
    technicalStack: profile.technicalStack ?? currentUser.technicalStack,
    avatarUrl: avatarUrl || currentUser.avatarUrl,
    signature: profile.signature ?? currentUser.signature,
    title: profile.title ?? currentUser.title,
    companyName: profile.companyName ?? currentUser.companyName,
    schoolName: profile.schoolName ?? currentUser.schoolName,
    major: profile.major ?? currentUser.major,
    grade: profile.grade ?? currentUser.grade,
    workYears: profile.workYears ?? currentUser.workYears,
    gender: profile.gender ?? currentUser.gender,
    birthday: profile.birthday ?? currentUser.birthday,
    status: profile.status ?? currentUser.status,
    lastLoginTime: profile.lastLoginTime ?? currentUser.lastLoginTime,
    lastLoginIp: profile.lastLoginIp ?? currentUser.lastLoginIp,
    createdAt: profile.createdAt ?? currentUser.createdAt,
    updatedAt: profile.updatedAt ?? currentUser.updatedAt,
  }

  localStorage.setItem('codehub-user', JSON.stringify(nextUser))

  if (profile.userId != null) {
    localStorage.setItem('codehub-user-id', String(profile.userId))
  }
}

const toNumericUserId = (value?: string | number) => {
  if (typeof value === 'number') {
    return Number.isFinite(value) ? value : undefined
  }
  if (typeof value === 'string' && value.trim() !== '') {
    const parsed = Number(value)
    return Number.isFinite(parsed) ? parsed : undefined
  }
  return undefined
}

const getGenderLabel = (value?: number) => {
  if (value === 1) return '男'
  if (value === 2) return '女'
  if (value === 0) return '保密'
  return ''
}

const getUserStatusLabel = (value?: number) => {
  if (value === 1) return '正常'
  if (value === 0) return '禁用'
  if (value === 2) return '冻结'
  return ''
}

const formatProfileDateTime = (value?: string) => {
  if (!value) return ''
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

const getRequestErrorMessage = (error: unknown, fallback: string) => {
  if (error && typeof error === 'object' && 'response' in error) {
    const responseData = (error as { response?: { data?: { message?: string; msg?: string; error?: string } | string } }).response?.data
    if (typeof responseData === 'string' && responseData) return responseData
    if (responseData && typeof responseData === 'object') return responseData.message || responseData.msg || responseData.error || fallback
  }
  if (error instanceof Error && error.message) return error.message
  return fallback
}

const isSelfFollowMessage = (value: string) => /自己|本人|self/i.test(value)

const mapQuestionToFeed = (item: QaQuestionListItemVO, fallbackUser?: Pick<ProfileViewUser, 'schoolName' | 'schoolVerified' | 'companyName' | 'companyVerified' | 'title' | 'titleVerified'>): PostListItemVO => ({
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
  schoolName: item.authorSchoolName || item.schoolName || item.userSchoolName || fallbackUser?.schoolName,
  authorSchoolName: item.authorSchoolName || item.schoolName || item.userSchoolName || fallbackUser?.schoolName,
  schoolVerified: item.schoolVerified ?? fallbackUser?.schoolVerified,
  companyName: item.authorCompanyName || item.companyName || item.userCompanyName || fallbackUser?.companyName,
  authorCompanyName: item.authorCompanyName || item.companyName || item.userCompanyName || fallbackUser?.companyName,
  companyVerified: item.companyVerified ?? fallbackUser?.companyVerified,
  authorTitle: item.authorTitle || item.userTitle || item.jobTitle || fallbackUser?.title,
  titleVerified: item.titleVerified ?? fallbackUser?.titleVerified,
  authorIsVip: item.authorIsVip,
  categoryName: item.categoryName || '问答',
  title: item.title || '未命名问题',
  summary: item.content?.trim() || `${item.answerCount ?? 0} 个回答 · ${item.followCount ?? 0} 人关注 · ${item.viewCount ?? 0} 次浏览`,
  viewCount: item.viewCount,
  commentCount: item.answerCount,
  collectCount: item.followCount,
  status: item.status,
  createdAt: item.createdAt,
  tags: [item.status === 1 ? '已解决' : item.status === 2 ? '已关闭' : '待解决'],
})

const mapCheckToFeed = (item: CheckRecordListItemVO, fallbackUser?: Pick<ProfileViewUser, 'schoolName' | 'schoolVerified' | 'companyName' | 'companyVerified' | 'title' | 'titleVerified'>): PostListItemVO => ({
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
  schoolName: item.authorSchoolName || item.schoolName || item.userSchoolName || fallbackUser?.schoolName,
  authorSchoolName: item.authorSchoolName || item.schoolName || item.userSchoolName || fallbackUser?.schoolName,
  schoolVerified: item.schoolVerified ?? fallbackUser?.schoolVerified,
  companyName: item.authorCompanyName || item.companyName || item.userCompanyName || fallbackUser?.companyName,
  authorCompanyName: item.authorCompanyName || item.companyName || item.userCompanyName || fallbackUser?.companyName,
  companyVerified: item.companyVerified ?? fallbackUser?.companyVerified,
  authorTitle: item.authorTitle || item.userTitle || item.jobTitle || fallbackUser?.title,
  titleVerified: item.titleVerified ?? fallbackUser?.titleVerified,
  authorIsVip: item.authorIsVip,
  categoryName: '打卡',
  title: item.recordTitle || item.title || '未命名打卡',
  summary: item.summary?.trim() || item.content?.trim() || '暂无打卡内容',
  viewCount: item.viewCount,
  likeCount: item.likeCount,
  commentCount: item.commentCount,
  createdAt: item.createdAt || item.checkTime || item.checkDate,
  tags: item.checkDate ? [item.checkDate] : ['学习打卡'],
})

const mapPostToFeed = (item: PostListItemVO, fallbackUser?: Pick<ProfileViewUser, 'nickname' | 'username' | 'avatarUrl' | 'authorAvatar' | 'userAvatar' | 'userAvatarUrl' | 'headImgUrl' | 'schoolName' | 'schoolVerified' | 'companyName' | 'companyVerified' | 'title' | 'titleVerified'>): PostListItemVO => ({
  ...item,
  username: item.username || fallbackUser?.username,
  nickname: item.nickname || fallbackUser?.nickname,
  authorName: item.nickname || item.authorName || fallbackUser?.nickname || fallbackUser?.username || '匿名用户',
  avatarUrl: item.avatarUrl || item.authorAvatar || item.userAvatar || fallbackUser?.avatarUrl || fallbackUser?.userAvatar || fallbackUser?.userAvatarUrl || fallbackUser?.headImgUrl,
  authorAvatar: item.authorAvatar || item.avatarUrl || item.userAvatar || fallbackUser?.authorAvatar || fallbackUser?.userAvatar || fallbackUser?.avatarUrl || fallbackUser?.userAvatarUrl || fallbackUser?.headImgUrl,
  schoolName: item.authorSchoolName || item.schoolName || fallbackUser?.schoolName,
  authorSchoolName: item.authorSchoolName || item.schoolName || fallbackUser?.schoolName,
  schoolVerified: item.schoolVerified ?? fallbackUser?.schoolVerified,
  companyName: item.authorCompanyName || item.companyName || fallbackUser?.companyName,
  authorCompanyName: item.authorCompanyName || item.companyName || fallbackUser?.companyName,
  companyVerified: item.companyVerified ?? fallbackUser?.companyVerified,
  authorTitle: item.authorTitle || item.userTitle || item.jobTitle || fallbackUser?.title,
  titleVerified: item.titleVerified ?? fallbackUser?.titleVerified,
})

const isVerified = (value?: number | boolean | string | null) => value === true || value === 1 || value === '1' || value === 'true' || value === 'approved' || value === '已通过'

const isApprovedCertification = (record: CertificationRecordVO) => (
  isVerified(record.certStatus) || /通过|approved/i.test(record.certStatusName || '')
)

const getApprovedCertificationName = (records: CertificationRecordVO[], certType: 1 | 2 | 3) => (
  records.find((record) => record.certType === certType && isApprovedCertification(record))?.certName?.trim() || ''
)

const getCertifiedProfile = <T extends Pick<ProfileViewUser, 'schoolName' | 'companyName' | 'title' | 'schoolVerified' | 'companyVerified' | 'titleVerified'>>(user: T, certifications: CertificationRecordVO[] = []) => {
  const approvedSchoolName = getApprovedCertificationName(certifications, 1)
  const approvedCompanyName = getApprovedCertificationName(certifications, 2)
  const approvedTitle = getApprovedCertificationName(certifications, 3)
  const certifiedSchoolName = approvedSchoolName || user.schoolName?.trim()
  const certifiedCompanyName = approvedCompanyName || user.companyName?.trim()
  const certifiedTitle = approvedTitle || user.title?.trim()

  return {
    ...user,
    schoolName: certifiedSchoolName || user.schoolName,
    companyName: certifiedCompanyName || user.companyName,
    title: certifiedTitle || user.title,
    schoolVerified: isVerified(user.schoolVerified) || approvedSchoolName ? 1 : user.schoolVerified,
    companyVerified: isVerified(user.companyVerified) || approvedCompanyName ? 1 : user.companyVerified,
    titleVerified: isVerified(user.titleVerified) || approvedTitle ? 1 : user.titleVerified,
  }
}

const getCertifiedProfileTags = (user: ProfileViewUser) => [
  ...(user.companyName?.trim() || isVerified(user.companyVerified) ? [{ key: 'cert-company', label: user.companyName?.trim() || '企业认证', color: 'blue' }] : []),
  ...(user.schoolName?.trim() || isVerified(user.schoolVerified) ? [{ key: 'cert-school', label: user.schoolName?.trim() || '学校认证', color: 'green' }] : []),
  ...(user.title?.trim() || isVerified(user.titleVerified) ? [{ key: 'cert-title', label: user.title?.trim() || '身份认证', color: 'purple' }] : []),
]

const normalizeTechnicalStack = (value?: string[] | string) => {
  if (Array.isArray(value)) return value
  return (value || '').split(/[、,，\n]/).map((item) => item.trim()).filter(Boolean)
}

const getProfileTags = (user: ProfileViewUser) => [
  ...(isVipMember(user) ? [{ key: 'vip', label: 'VIP', color: 'gold' }] : []),
  ...normalizeTechnicalStack(user.technicalStack).slice(0, 5).map((stack) => ({ key: `stack-${stack}`, label: stack, color: 'geekblue' })),
]

const getActiveDeveloperBadges = (source: ActiveDeveloperVO) => {
  const companyName = source.authorCompanyName?.trim() || source.userCompanyName?.trim() || source.companyName?.trim()
  const schoolName = source.authorSchoolName?.trim() || source.userSchoolName?.trim() || source.schoolName?.trim()
  const title = source.authorTitle?.trim() || source.userTitle?.trim() || source.jobTitle?.trim()
  const companyVerified = Boolean(companyName) || isVerified(source.companyVerified)
  const schoolVerified = Boolean(schoolName) || isVerified(source.schoolVerified)
  const titleVerified = Boolean(title) || isVerified(source.titleVerified)

  return [
    { key: 'company', label: companyName || (companyVerified ? '企业认证' : ''), verified: companyVerified, color: 'blue' },
    { key: 'school', label: schoolName || (schoolVerified ? '学校认证' : ''), verified: schoolVerified, color: 'green' },
    { key: 'title', label: title || (titleVerified ? '身份认证' : ''), verified: titleVerified, color: 'purple' },
  ].filter((item) => item.verified && item.label)
}

type LikedFeedItem = PostListItemVO & { likedContentType: 'article' | 'check' | 'question' }

const getFeedDetailPath = (post: PostListItemVO, contentType: 'article' | 'question' | 'check') => {
  if (post.id == null || String(post.id).trim() === '') {
    return undefined
  }

  const id = encodeURIComponent(String(post.id).trim())

  if (contentType === 'question') {
    return `/qa/question/${id}`
  }

  if (contentType === 'check') {
    return `/check-in/${id}`
  }

  return `/article/${id}`
}

export function ProfilePage() {
  const navigate = useNavigate()
  const { userId } = useParams<{ userId?: string }>()
  const [storedUser, setStoredUser] = useState<StoredUser | null>(() => getStoredUser())
  const [viewedUserProfile, setViewedUserProfile] = useState<AuthProfileVO | null>(null)
  const [publishedPosts, setPublishedPosts] = useState<PostListItemVO[]>([])
  const [draftPosts, setDraftPosts] = useState<PostListItemVO[]>([])
  const [collectedPosts, setCollectedPosts] = useState<PostListItemVO[]>([])
  const [checkPosts, setCheckPosts] = useState<PostListItemVO[]>([])
  const [questionPosts, setQuestionPosts] = useState<PostListItemVO[]>([])
  const [likedPosts, setLikedPosts] = useState<PostListItemVO[]>([])
  const [likedChecks, setLikedChecks] = useState<PostListItemVO[]>([])
  const [likedQuestions, setLikedQuestions] = useState<PostListItemVO[]>([])
  const [likedFilter, setLikedFilter] = useState<'all' | 'post' | 'check' | 'question'>('all')
  const [loadingPosts, setLoadingPosts] = useState(true)
  const [followingUsers, setFollowingUsers] = useState<UserFollowVO[]>([])
  const [followerUsers, setFollowerUsers] = useState<UserFollowVO[]>([])
  const [followSubmittingUserId, setFollowSubmittingUserId] = useState<string | number | null>(null)
  const [profileFollowed, setProfileFollowed] = useState(false)
  const [profileFollowLoading, setProfileFollowLoading] = useState(false)
  const [messageCreating, setMessageCreating] = useState(false)
  const [profileLoading, setProfileLoading] = useState(false)
  const [certificationRecords, setCertificationRecords] = useState<CertificationRecordVO[]>([])
  const [profileNotFound, setProfileNotFound] = useState(false)
  const [deletingPostId, setDeletingPostId] = useState<string | number | null>(null)
  const [activeDevelopers, setActiveDevelopers] = useState<ActiveDeveloperVO[]>([])
  const [loadingActiveDevelopers, setLoadingActiveDevelopers] = useState(false)

  const viewedUser = useMemo<ProfileViewUser>(() => {
    const profileSource = userId ? viewedUserProfile : (viewedUserProfile ?? storedUser)

    if (userId) {
      return {
        userId: profileSource?.userId,
        username: profileSource?.username || userId,
        nickname: profileSource?.nickname,
        email: profileSource?.email,
        phone: profileSource?.phone,
        roles: profileSource?.roles?.length ? profileSource.roles : ['开发者'],
        schoolVerified: profileSource?.schoolVerified,
        companyVerified: profileSource?.companyVerified,
        titleVerified: profileSource?.titleVerified,
        technicalStack: profileSource?.technicalStack,
        avatarUrl: profileSource?.avatarUrl,
        authorAvatar: profileSource?.userAvatar || profileSource?.avatarUrl,
        userAvatar: profileSource?.userAvatar,
        userAvatarUrl: profileSource?.userAvatarUrl,
        headImgUrl: profileSource?.headImgUrl,
        signature: profileSource?.signature,
        title: profileSource?.title,
        companyName: profileSource?.companyName,
        schoolName: profileSource?.schoolName,
        major: profileSource?.major,
        grade: profileSource?.grade,
        workYears: profileSource?.workYears,
        gender: profileSource?.gender,
        birthday: profileSource?.birthday,
        status: profileSource?.status,
        lastLoginTime: profileSource?.lastLoginTime,
        lastLoginIp: profileSource?.lastLoginIp,
        createdAt: profileSource?.createdAt,
        updatedAt: profileSource?.updatedAt,
        isVip: profileSource?.isVip,
        vipLevel: profileSource?.vipLevel,
        vipExpiredAt: profileSource?.vipExpiredAt,
      }
    }

    return {
      userId: profileSource?.userId,
      username: profileSource?.username ?? '未登录用户',
      nickname: profileSource?.nickname,
      email: profileSource?.email,
      phone: profileSource?.phone,
      roles: profileSource?.roles?.length ? profileSource.roles : ['开发者'],
      schoolVerified: profileSource?.schoolVerified,
      companyVerified: profileSource?.companyVerified,
      titleVerified: profileSource?.titleVerified,
      technicalStack: profileSource?.technicalStack,
      avatarUrl: profileSource?.avatarUrl,
      authorAvatar: profileSource?.userAvatar || profileSource?.avatarUrl,
      userAvatar: profileSource?.userAvatar,
      userAvatarUrl: profileSource?.userAvatarUrl,
      headImgUrl: profileSource?.headImgUrl,
      signature: profileSource?.signature,
      title: profileSource?.title,
      companyName: profileSource?.companyName,
      schoolName: profileSource?.schoolName,
      major: profileSource?.major,
      grade: profileSource?.grade,
      workYears: profileSource?.workYears,
      gender: profileSource?.gender,
      birthday: profileSource?.birthday,
      status: profileSource?.status,
      lastLoginTime: profileSource?.lastLoginTime,
      lastLoginIp: profileSource?.lastLoginIp,
      createdAt: profileSource?.createdAt,
      updatedAt: profileSource?.updatedAt,
      isVip: profileSource?.isVip,
      vipLevel: profileSource?.vipLevel,
      vipExpiredAt: profileSource?.vipExpiredAt,
    }
  }, [storedUser, userId, viewedUserProfile])

  const isSelfProfile = !userId
  const certifiedViewedUser = useMemo(
    () => getCertifiedProfile(viewedUser, isSelfProfile ? certificationRecords : []),
    [certificationRecords, isSelfProfile, viewedUser],
  )

  useEffect(() => {
    const loadCurrentProfile = async () => {
      try {
        setProfileLoading(true)
        setProfileNotFound(false)

        if (!userId) {
          const [profile, certifications] = await Promise.all([
            getCurrentUserProfile(),
            listMyCertifications({ pageNum: 1, pageSize: 20 }),
          ])
          const detailProfile = profile.userId != null ? await getUserProfile(profile.userId) : profile
          syncStoredUser(detailProfile)
          setViewedUserProfile(detailProfile)
          setStoredUser(getStoredUser())
          setCertificationRecords(certifications.records ?? [])
          return
        }

        setCertificationRecords([])
        setViewedUserProfile(null)
        const profile = await getUserProfile(userId)
        setViewedUserProfile(profile)
      } catch (error) {
        console.error('load profile page user error:', error)
        setCertificationRecords([])
        if (userId) {
          setViewedUserProfile(null)
          setProfileNotFound(true)
        }
      } finally {
        setProfileLoading(false)
      }
    }

    void loadCurrentProfile()
  }, [userId])

  const fetchProfileContent = useCallback(async () => {
    const isSelf = !userId

    if (!isSelf && certifiedViewedUser.userId == null) {
      setPublishedPosts([])
      setDraftPosts([])
      setCollectedPosts([])
      setCheckPosts([])
      setQuestionPosts([])
      setLikedPosts([])
      setLikedChecks([])
      setLikedQuestions([])
      setFollowingUsers([])
      setFollowerUsers([])
      setLoadingPosts(true)
      return
    }

    try {
      setLoadingPosts(true)
      const followingRequest = isSelf && certifiedViewedUser.userId != null
        ? listMyFollowing({ pageNum: 1, pageSize: 20 })
        : certifiedViewedUser.userId != null
          ? listUserFollowing(certifiedViewedUser.userId, { pageNum: 1, pageSize: 20 })
          : Promise.resolve({ records: [] as UserFollowVO[] })

      const followerRequest = isSelf && certifiedViewedUser.userId != null
        ? listMyFollowers({ pageNum: 1, pageSize: 20 })
        : certifiedViewedUser.userId != null
          ? listUserFollowers(certifiedViewedUser.userId, { pageNum: 1, pageSize: 20 })
          : Promise.resolve({ records: [] as UserFollowVO[] })

      const questionRequest = isSelf
        ? listMyQuestions({ pageNum: 1, pageSize: 20 })
        : certifiedViewedUser.userId != null
          ? listQuestions({ userId: certifiedViewedUser.userId, pageNum: 1, pageSize: 20, sortBy: 'latest' })
          : Promise.resolve({ records: [] as QaQuestionListItemVO[] })

      const likedPostsRequest = isSelf
        ? listLikedPosts({ pageNum: 1, pageSize: 20 })
        : Promise.resolve({ records: [] as PostListItemVO[] })
      const likedChecksRequest = isSelf
        ? listLikedCheckRecords({ pageNum: 1, pageSize: 20 })
        : Promise.resolve({ records: [] as CheckRecordListItemVO[] })
      const likedQuestionsRequest = isSelf
        ? listLikedQuestions({ pageNum: 1, pageSize: 20 })
        : Promise.resolve({ records: [] as QaQuestionListItemVO[] })

      const [postData, draftData, collectedData, checkData, myQuestions, followingData, followerData, likedPostData, likedCheckData, likedQuestionData] = await Promise.all([
        listPosts({ userId: certifiedViewedUser.userId, pageNum: 1, pageSize: 20, sortBy: 'latest' }),
        isSelf ? listDrafts({ pageNum: 1, pageSize: 20 }) : Promise.resolve({ records: [] as PostListItemVO[] }),
        isSelf ? listCollectedPosts({ pageNum: 1, pageSize: 20 }) : Promise.resolve({ records: [] as PostListItemVO[] }),
        listCheckRecords({ userId: toNumericUserId(certifiedViewedUser.userId), pageNum: 1, pageSize: 20 }),
        questionRequest,
        followingRequest,
        followerRequest,
        likedPostsRequest,
        likedChecksRequest,
        likedQuestionsRequest,
      ])
      setPublishedPosts((postData.records ?? []).map((item) => mapPostToFeed(item, certifiedViewedUser)))
      setDraftPosts((draftData.records ?? []).map((item) => mapPostToFeed(item, certifiedViewedUser)))
      setCollectedPosts((collectedData.records ?? []).map((item) => mapPostToFeed(item, certifiedViewedUser)))
      setCheckPosts((checkData.records ?? []).map((item) => mapCheckToFeed(item, certifiedViewedUser)))
      setQuestionPosts((myQuestions.records ?? []).map((item) => mapQuestionToFeed(item, certifiedViewedUser)))
      setLikedPosts((likedPostData.records ?? []).map((item) => mapPostToFeed(item, certifiedViewedUser)))
      setLikedChecks((likedCheckData.records ?? []).map((item) => mapCheckToFeed(item, certifiedViewedUser)))
      setLikedQuestions((likedQuestionData.records ?? []).map((item) => mapQuestionToFeed(item, certifiedViewedUser)))
      setFollowingUsers(followingData.records ?? [])
      setFollowerUsers(followerData.records ?? [])
    } catch (error) {
      console.error('fetch profile content error:', error)
      setPublishedPosts([])
      setDraftPosts([])
      setCollectedPosts([])
      setCheckPosts([])
      setQuestionPosts([])
      setLikedPosts([])
      setLikedChecks([])
      setLikedQuestions([])
      setFollowingUsers([])
      setFollowerUsers([])
    } finally {
      setLoadingPosts(false)
    }
  }, [certifiedViewedUser, userId])

  useEffect(() => {
    void fetchProfileContent()
  }, [fetchProfileContent])

  useEffect(() => {
    let cancelled = false

    const fetchActiveDevelopers = async () => {
      try {
        setLoadingActiveDevelopers(true)
        const data = await listActiveDevelopers()
        if (!cancelled) {
          setActiveDevelopers((data || []).slice(0, 3))
        }
      } catch (error) {
        console.error('list active developers error:', error)
        if (!cancelled) {
          setActiveDevelopers([])
        }
      } finally {
        if (!cancelled) {
          setLoadingActiveDevelopers(false)
        }
      }
    }

    void fetchActiveDevelopers()

    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    const loadFollowStatus = async () => {
      if (!userId || viewedUser.userId == null) {
        setProfileFollowed(false)
        return
      }

      try {
        const status = await getUserFollowStatus(viewedUser.userId)
        setProfileFollowed(Boolean(status.isFollowing))
      } catch (error) {
        console.error('load user follow status error:', error)
        setProfileFollowed(false)
      }
    }

    void loadFollowStatus()
  }, [userId, viewedUser.userId])

  const currentUserId = storedUser?.userId != null ? String(storedUser.userId) : ''
  const displayName = certifiedViewedUser.nickname?.trim() || certifiedViewedUser.username
  const displayEmail = certifiedViewedUser.email ?? '暂无邮箱信息'
  const displayUserId = certifiedViewedUser.userId != null ? `用户ID ${String(certifiedViewedUser.userId)}` : ''
  const displayDescription = certifiedViewedUser.signature || '这个人很低调，还没有填写个人简介。'
  const displayMetaItems = [
    displayEmail,
    displayUserId,
  ].filter(Boolean)
  const profileTags = getProfileTags(certifiedViewedUser)
  const profileCertificationTags = getCertifiedProfileTags(certifiedViewedUser)

  const profileStats = [
    { title: '帖子', value: publishedPosts.length },
    { title: '打卡', value: checkPosts.length },
    { title: '问答', value: questionPosts.length },
    { title: '草稿', value: draftPosts.length },
    { title: '关注', value: followingUsers.length },
    { title: '粉丝', value: followerUsers.length },
  ]

  const handleStartPrivateMessage = async () => {
    if (!currentUserId) {
      message.warning('请先登录后再发送私信')
      navigate('/auth')
      return
    }

    if (viewedUser.userId == null) {
      message.warning('用户信息加载中，请稍后再试')
      return
    }

    if (String(viewedUser.userId) === currentUserId) {
      message.warning('不能给自己发送私信')
      return
    }

    try {
      setMessageCreating(true)
      const conversation = await createSingleConversation(viewedUser.userId)
      if (conversation.id == null) {
        throw new Error('会话创建成功，但未返回会话 ID')
      }
      navigate(`/messages/${encodeURIComponent(String(conversation.id))}`)
    } catch (error) {
      console.error('create private message conversation error:', error)
      const errorMessage = getRequestErrorMessage(error, '私信会话创建失败，请稍后重试')
      message.error(errorMessage.includes('自己') ? '不能给自己发送私信' : errorMessage)
    } finally {
      setMessageCreating(false)
    }
  }

  const handleToggleProfileFollow = async () => {
    if (viewedUser.userId == null) {
      message.warning('用户信息加载中，请稍后再试')
      return
    }

    if (currentUserId && String(viewedUser.userId) === currentUserId) {
      message.warning('不能关注自己')
      return
    }

    try {
      setProfileFollowLoading(true)
      if (profileFollowed) {
        await unfollowUser(viewedUser.userId)
        setProfileFollowed(false)
        message.success('已取消关注')
      } else {
        await followUser(viewedUser.userId)
        setProfileFollowed(true)
        message.success('关注成功')
      }
      await fetchProfileContent()
    } catch (error) {
      console.error('toggle profile follow error:', error)
      const errorMessage = getRequestErrorMessage(error, '操作失败，请稍后重试')
      message.error(isSelfFollowMessage(errorMessage) ? '不能关注自己' : errorMessage)
    } finally {
      setProfileFollowLoading(false)
    }
  }

  const handleDeletePost = async (post: PostListItemVO) => {
    if (post.id == null || String(post.id).trim() === '') return

    const confirmed = await new Promise<boolean>((resolve) => {
      Modal.confirm({
        title: '确认删除这篇文章吗？',
        content: `删除后将无法恢复：${post.title || '未命名文章'}`,
        okText: '确认删除',
        cancelText: '取消',
        okButtonProps: { danger: true },
        onOk: () => resolve(true),
        onCancel: () => resolve(false),
      })
    })

    if (!confirmed) return

    try {
      setDeletingPostId(post.id)
      await deletePost(post.id)
      message.success('文章删除成功')
      await fetchProfileContent()
    } catch (error) {
      console.error('delete post error:', error)
      message.error('文章删除失败，请稍后重试')
    } finally {
      setDeletingPostId(null)
    }
  }

  const renderFeedList = (
    posts: PostListItemVO[],
    emptyText: string,
    extraActions?: (post: PostListItemVO) => ReactNode,
    contentType: 'article' | 'question' | 'check' = 'article',
    resolveContentType?: (post: PostListItemVO) => 'article' | 'question' | 'check',
  ) => {
    if (loadingPosts) {
      return <div className="profile-tab-loading"><Spin size="large" /></div>
    }

    if (!posts.length) {
      return <Empty description={emptyText} />
    }

    return (
      <Space orientation="vertical" size={20} className="full-width profile-feed-list">
        {posts.map((post) => (
          <FeedCard
            key={String(post.id ?? post.title ?? 'post')}
            post={post}
            disableLink={post.id == null || String(post.id).trim() === ''}
            detailPath={getFeedDetailPath(post, resolveContentType ? resolveContentType(post) : contentType)}
            extraActions={extraActions ? extraActions(post) : undefined}
          />
        ))}
      </Space>
    )
  }

  const renderPostActions = (post: PostListItemVO) => (
    <div className="feed-card__actions" onClick={(event) => event.stopPropagation()}>
      <Button icon={<EditOutlined />} onClick={() => navigate(post.id != null && String(post.id).trim() !== '' ? `/article/editor/${String(post.id).trim()}` : '/article/editor')}>
        编辑
      </Button>
      <Button danger icon={<DeleteOutlined />} loading={deletingPostId === post.id} onClick={() => void handleDeletePost(post)}>
        删除
      </Button>
    </div>
  )

  const handleUncollectPost = async (post: PostListItemVO) => {
    if (post.id == null || String(post.id).trim() === '') return
    try {
      await uncollectPost(post.id)
      setCollectedPosts((prev) => prev.filter((item) => item.id !== post.id))
      message.success('已取消收藏')
    } catch (error) {
      console.error('uncollect post error:', error)
      message.error('取消收藏失败，请稍后重试')
    }
  }

  const renderUncollectAction = (post: PostListItemVO) => (
    <div className="feed-card__actions" onClick={(event) => event.stopPropagation()}>
      <Button icon={<HeartOutlined />} onClick={() => void handleUncollectPost(post)}>
        取消收藏
      </Button>
    </div>
  )

  const handleUnlike = async (post: PostListItemVO) => {
    if (post.id == null || String(post.id).trim() === '') return
    const contentType = (post as LikedFeedItem).likedContentType ?? 'article'
    try {
      if (contentType === 'check') {
        await unlikeCheckRecord(post.id)
        setLikedChecks((prev) => prev.filter((item) => item.id !== post.id))
      } else if (contentType === 'question') {
        await unvoteQuestion(post.id)
        setLikedQuestions((prev) => prev.filter((item) => item.id !== post.id))
      } else {
        await unlikePost(post.id)
        setLikedPosts((prev) => prev.filter((item) => item.id !== post.id))
      }
      message.success('已取消点赞')
    } catch (error) {
      console.error('unlike content error:', error)
      message.error('取消点赞失败，请稍后重试')
    }
  }

  const renderUnlikeAction = (post: PostListItemVO) => (
    <div className="feed-card__actions" onClick={(event) => event.stopPropagation()}>
      <Button icon={<HeartOutlined />} onClick={() => void handleUnlike(post)}>
        取消点赞
      </Button>
    </div>
  )

  const likedItems = useMemo<LikedFeedItem[]>(() => {
    const items: LikedFeedItem[] = []
    if (likedFilter === 'all' || likedFilter === 'post') {
      items.push(...likedPosts.map((item) => ({ ...item, likedContentType: 'article' as const })))
    }
    if (likedFilter === 'all' || likedFilter === 'check') {
      items.push(...likedChecks.map((item) => ({ ...item, likedContentType: 'check' as const })))
    }
    if (likedFilter === 'all' || likedFilter === 'question') {
      items.push(...likedQuestions.map((item) => ({ ...item, likedContentType: 'question' as const })))
    }
    if (likedFilter === 'all') {
      items.sort((a, b) => new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime())
    }
    return items
  }, [likedFilter, likedPosts, likedChecks, likedQuestions])

  const renderLikedTab = () => (
    <div className="full-width">
      <Segmented
        className="profile-liked-filter"
        value={likedFilter}
        onChange={(value) => setLikedFilter(value as typeof likedFilter)}
        options={[
          { label: '全部', value: 'all' },
          { label: '文章', value: 'post' },
          { label: '打卡', value: 'check' },
          { label: '问答', value: 'question' },
        ]}
      />
      {renderFeedList(
        likedItems,
        '你还没有点赞任何内容',
        renderUnlikeAction,
        'article',
        (post) => (post as LikedFeedItem).likedContentType ?? 'article',
      )}
    </div>
  )

  const renderProfileInfo = () => {
    if (profileLoading) {
      return <div className="profile-tab-loading"><Spin size="large" /></div>
    }

    const emptyText = '暂无'
    const profileSections = [
      {
        title: '基本信息',
        items: [
          { label: '昵称', value: certifiedViewedUser.nickname || certifiedViewedUser.username || emptyText },
          { label: '账号', value: certifiedViewedUser.username || emptyText },
          { label: '简介', value: certifiedViewedUser.signature || emptyText },
          { label: '性别', value: getGenderLabel(certifiedViewedUser.gender) || emptyText },
          { label: '生日', value: certifiedViewedUser.birthday || emptyText },
          { label: '账号状态', value: getUserStatusLabel(certifiedViewedUser.status) || emptyText },
          { label: '手机', value: certifiedViewedUser.phone || emptyText },
          { label: '邮箱', value: certifiedViewedUser.email || emptyText },
          { label: '编号', value: certifiedViewedUser.userId != null ? String(certifiedViewedUser.userId) : emptyText },
          { label: '最近登录', value: formatProfileDateTime(certifiedViewedUser.lastLoginTime) || emptyText },
          { label: '注册时间', value: formatProfileDateTime(certifiedViewedUser.createdAt) || emptyText },
        ],
      },
      {
        title: '学习信息',
        items: [
          { label: '兴趣', value: certifiedViewedUser.roles?.join('、') || emptyText },
          { label: '主攻方向', value: certifiedViewedUser.title || certifiedViewedUser.major || emptyText },
          { label: '目标', value: certifiedViewedUser.signature || emptyText },
        ],
      },
      {
        title: '教育信息',
        items: [
          { label: '学校', value: certifiedViewedUser.schoolName || emptyText },
          { label: '专业', value: certifiedViewedUser.major || emptyText },
          { label: '学历', value: certifiedViewedUser.grade || emptyText },
          { label: '毕业年份', value: emptyText },
        ],
      },
      {
        title: '职业信息',
        items: [
          { label: '工作状态', value: certifiedViewedUser.workYears != null ? `${certifiedViewedUser.workYears} 年经验` : emptyText },
          { label: '公司', value: certifiedViewedUser.companyName || emptyText },
          { label: '岗位', value: certifiedViewedUser.title || emptyText },
        ],
      },
    ]

    return (
      <div className="profile-info-panel">
        {profileSections.map((section) => (
          <section className="profile-info-section" key={section.title}>
            <div className="profile-info-section__header">
              <h3>{section.title}</h3>
            </div>
            <div className="profile-info-list">
              {section.items.map((item) => (
                <div className="profile-info-row" key={`${section.title}-${item.label}`}>
                  <span className="profile-info-row__label">{item.label}</span>
                  <span className="profile-info-row__value">{item.value}</span>
                </div>
              ))}
            </div>
          </section>
        ))}
      </div>
    )
  }

  const handleToggleFollowUser = async (user: UserFollowVO) => {
    if (user.userId == null) {
      return
    }

    try {
      setFollowSubmittingUserId(user.userId)
      if (user.isFollowed === 1) {
        await unfollowUser(user.userId)
        if (viewedUser.userId != null && String(user.userId) === String(viewedUser.userId)) {
          setProfileFollowed(false)
        }
        message.success('已取消关注')
      } else {
        await followUser(user.userId)
        if (viewedUser.userId != null && String(user.userId) === String(viewedUser.userId)) {
          setProfileFollowed(true)
        }
        message.success('关注成功')
      }
      await fetchProfileContent()
    } catch (error) {
      console.error('toggle follow user error:', error)
      const errorMessage = getRequestErrorMessage(error, '操作失败，请稍后重试')
      message.error(isSelfFollowMessage(errorMessage) ? '不能关注自己' : errorMessage)
    } finally {
      setFollowSubmittingUserId(null)
    }
  }

  const renderFollowUserList = (users: UserFollowVO[], emptyText: string) => {
    if (loadingPosts) {
      return <div className="profile-tab-loading"><Spin size="large" /></div>
    }

    if (!users.length) {
      return <Empty description={emptyText} />
    }

    return (
      <Card className="content-card" variant="borderless">
        <Space orientation="vertical" size={18} className="full-width profile-follow-user-list">
          {users.map((user) => {
            const displayFollowName = user.nickname || user.username || '匿名用户'
            return (
              <div key={String(user.userId ?? displayFollowName)} className="profile-user-item">
                <Space onClick={() => navigate(`/profile/${encodeURIComponent(String(user.userId ?? (user.username || displayFollowName)))}`)}>
                  <Avatar size={48} src={getAvatarWithFallback({ avatarUrl: user.avatarUrl })} />
                  <div>
                    <div className="profile-user-item__name">{displayFollowName}</div>
                    <div className="profile-user-item__role">{user.signature || '这个人很低调，还没有填写简介。'}</div>
                    <div className="profile-user-item__role">粉丝 {user.followerCount ?? 0} · 关注 {user.followingCount ?? 0}</div>
                  </div>
                </Space>
                <Button
                  shape="round"
                  size="small"
                  type={user.isFollowed === 1 ? 'default' : 'primary'}
                  loading={followSubmittingUserId === user.userId}
                  onClick={() => void handleToggleFollowUser(user)}
                >
                  {user.isFollowed === 1 ? '已关注' : '关注'}
                </Button>
              </div>
            )
          })}
        </Space>
      </Card>
    )
  }

  const shouldShowPostActions = !userId

  if (profileNotFound) {
    return (
      <Card className="content-card" variant="borderless">
        <Empty description="未找到该用户资料" />
      </Card>
    )
  }

  return (
    <div className="profile-page">
      <Row gutter={[24, 24]} align="top">
        <Col xs={24} lg={18}>
          <Card className="profile-hero-card" variant="borderless">
            <div className="profile-hero">
              <div className="profile-hero__main">
                <Avatar size={92} className="profile-hero__avatar" src={getAvatarWithFallback(certifiedViewedUser)} />
                <div className="profile-hero__info">
                  <div className="profile-hero__name-row">
                    <h1>{displayName}</h1>
                    <div className="profile-hero__tag-list">
                      {profileLoading ? <Tag color="processing">同步资料中</Tag> : null}
                      {profileTags.map((tag) => <Tag color={tag.color} key={tag.key}>{tag.label}</Tag>)}
                      {profileCertificationTags.map((tag) => <Tag color={tag.color} key={tag.key} className="profile-hero__cert-tag">{tag.label}</Tag>)}
                    </div>
                  </div>
                  <div className="profile-hero__desc">{displayDescription}</div>
                  <Space wrap size={[16, 10]} className="profile-hero__meta">
                    {displayMetaItems.map((item) => <span key={item}>{item}</span>)}
                  </Space>
                </div>
              </div>

              <div className="profile-hero__actions">
                {isSelfProfile ? (
                  <Button icon={<EditOutlined />} onClick={() => navigate('/profile/edit')}>修改用户信息</Button>
                ) : (
                  <>
                    <Button
                      type={profileFollowed ? 'default' : 'primary'}
                      icon={profileFollowed ? <UserDeleteOutlined /> : <PlusOutlined />}
                      loading={profileFollowLoading}
                      onClick={() => void handleToggleProfileFollow()}
                    >
                      {profileFollowed ? '取消关注' : '关注'}
                    </Button>
                    <Button icon={<MailOutlined />} loading={messageCreating} onClick={() => void handleStartPrivateMessage()}>私信</Button>
                  </>
                )}
              </div>
            </div>
            <div className="profile-stats-grid">
              {profileStats.map((item) => (
                <div key={item.title} className="profile-stats-grid__item">
                  <div className="profile-stats-grid__value">{item.value}</div>
                  <div className="profile-stats-grid__label">{item.title}</div>
                </div>
              ))}
            </div>
          </Card>

          <Card className="profile-content-card" variant="borderless">
            <Tabs
              defaultActiveKey="posts"
              items={[
                { key: 'posts', label: '帖子', children: renderFeedList(publishedPosts, '该用户还没有发布帖子', shouldShowPostActions ? renderPostActions : undefined) },
                { key: 'checks', label: '打卡', children: renderFeedList(checkPosts, '还没有打卡记录', undefined, 'check') },
                { key: 'questions', label: '问答', children: renderFeedList(questionPosts, '还没有提问记录', undefined, 'question') },
                { key: 'drafts', label: '草稿', children: userId ? <div className="profile-tab-placeholder">仅本人可查看草稿。</div> : renderFeedList(draftPosts, '你还没有保存草稿', renderPostActions) },
                { key: 'collected', label: '收藏', children: userId ? <div className="profile-tab-placeholder">仅本人可查看收藏。</div> : renderFeedList(collectedPosts, '你还没有收藏任何帖子', renderUncollectAction) },
                { key: 'liked', label: '我点赞的', children: userId ? <div className="profile-tab-placeholder">仅本人可查看点赞。</div> : renderLikedTab() },
                { key: 'profile', label: '资料', children: renderProfileInfo() },
                { key: 'follow', label: '关注', children: renderFollowUserList(followingUsers, '暂时还没有关注任何用户') },
                { key: 'fans', label: '粉丝', children: renderFollowUserList(followerUsers, '暂时还没有粉丝') },
              ]}
            />
          </Card>
        </Col>

        <Col xs={24} lg={6}>
          <Space orientation="vertical" size={20} className="full-width">
            <Card className="profile-side-card profile-side-card--promo" variant="borderless">
              <div className="profile-side-card__title">专属能力会员</div>
              <p>会员专享学习日历、活跃榜单和成长报告，帮助你更系统地记录成长。</p>
              <Button type="primary" block onClick={() => navigate('/membership')}>立即查看</Button>
            </Card>

            <Card className="profile-side-card" variant="borderless">
              <div className="profile-side-card__title"><MessageOutlined /> 活跃开发者</div>
              {loadingActiveDevelopers ? (
                <div className="profile-side-card__loading"><Spin /></div>
              ) : (
                <Space orientation="vertical" size={18} className="full-width">
                  {activeDevelopers.length ? activeDevelopers.map((user) => {
                    const displayActiveName = user.nickname?.trim() || user.username?.trim() || '匿名用户'
                    const activeProfilePath = user.userId != null ? `/profile/${encodeURIComponent(String(user.userId))}` : '/profile'
                    return (
                      <div key={String(user.userId ?? displayActiveName)} className="active-user">
                        <Space>
                          <button type="button" className="active-user__avatar-button" onClick={() => navigate(activeProfilePath)} aria-label={`查看${displayActiveName}的个人信息`}>
                            <img className="active-user__avatar" src={getAvatarWithFallback(user)} alt={displayActiveName} />
                          </button>
                          <div>
                            <div className="active-user__name">
                              {displayActiveName}
                              {getActiveDeveloperBadges(user).map((badge) => <Tag key={badge.key} color={badge.color} className="active-user__badge">{badge.label}</Tag>)}
                            </div>
                            <div className="active-user__role">活跃度 {user.activityLevel ?? 0}</div>
                          </div>
                        </Space>
                      </div>
                    )
                  }) : <Empty description="暂无活跃开发者" />}
                </Space>
              )}
            </Card>
          </Space>
        </Col>
      </Row>
    </div>
  )
}
