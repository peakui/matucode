export interface ApiResponse<T> {
  code: number
  message: string
  msg?: string
  success?: boolean
  data: T
}

export interface LoginRequest {
  account: string
  password: string
}

export interface EmailCodeRequest {
  email: string
}

export interface RegisterRequest {
  username: string
  email: string
  emailCode: string
  phone?: string
  password: string
  confirmPassword: string
}

export interface AuthTokenVO {
  userId: number
  username: string
  email: string
  roles: string[]
  tokenName: string
  tokenValue: string
  authorization: string
  tokenTimeout: number
  lastLoginTime: string
}

export interface AuthProfileVO {
  userId?: string | number
  username?: string
  nickname?: string
  email?: string
  phone?: string
  avatarUrl?: string
  userAvatar?: string
  userAvatarUrl?: string
  headImgUrl?: string
  followingCount?: number
  followerCount?: number
  gender?: number
  birthday?: string
  signature?: string
  status?: number
  lastLoginIp?: string
  createdAt?: string
  updatedAt?: string
  deletedAt?: string | null
  schoolName?: string
  schoolVerified?: number
  schoolVerifyTime?: string
  companyName?: string
  companyVerified?: number
  companyVerifyTime?: string
  isVip?: number
  vipLevel?: number
  vipExpiredAt?: string | null
  vipDaysRemaining?: number
  title?: string
  titleVerified?: number
  major?: string
  grade?: string
  workYears?: number
  technicalStack?: string[] | string
  blogUrl?: string
  githubUrl?: string
  wechatUrl?: string
  roles?: string[]
  tokenName?: string
  tokenValue?: string
  authorization?: string
  tokenTimeout?: number
  lastLoginTime?: string
}

export interface UpdateAuthProfileRequest {
  nickname?: string
  phone?: string
  gender?: number
  birthday?: string
  signature?: string
  schoolName?: string
  companyName?: string
  title?: string
  major?: string
  grade?: string
  workYears?: number
  technicalStack?: string[] | string
  blogUrl?: string
  githubUrl?: string
  wechatUrl?: string
}

export interface UpdateAvatarRequest {
  avatarUrl: string
}

export interface SubmitCertificationRequest {
  certType: 1 | 2 | 3
  certName: string
  certProof: string
}

export interface CertificationRecordVO {
  id?: string | number
  userId?: string | number
  certType?: 1 | 2 | 3
  certTypeName?: string
  certName?: string
  certProof?: string
  certStatus?: number
  certStatusName?: string
  auditRemark?: string | null
  auditorId?: string | number | null
  auditTime?: string | null
  createdAt?: string
}

export interface PageResponse<T> {
  pageNum?: number
  pageSize?: number
  total?: number
  totalPages?: number
  records?: T[]
}

export interface UserFollowVO {
  userId?: string | number
  username?: string
  nickname?: string
  avatarUrl?: string
  signature?: string
  isFollowed?: number
  followerCount?: number
  followingCount?: number
}

export interface FollowStatusVO {
  isFollowing?: boolean
}
