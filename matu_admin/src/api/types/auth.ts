export interface LoginParams {
  account: string
  password: string
}

export interface AuthTokenVO {
  userId: string
  username: string
  nickname?: string
  avatar?: string
  avatarUrl?: string
  headImg?: string
  phone?: string
  email: string
  roles: string[]
  tokenName: string
  tokenValue: string
  authorization: string
  tokenTimeout: number
  lastLoginTime: string
}

export interface CertificationVO {
  id: string | number
  userId: string | number
  certType: number
  certTypeName?: string
  certName: string
  certProof?: string
  certStatus: number
  certStatusName?: string
  auditRemark?: string
  auditorId?: string | number
  auditTime?: string
  createdAt?: string
}

export interface CreateCertificationRequest {
  certType: number
  certName: string
  certProof: string
}

export interface MyCertificationListParams {
  pageNum?: number
  pageSize?: number
}

export interface InternalCertificationListParams {
  certType?: number
  certStatus?: number
  pageNum?: number
  pageSize?: number
}

export interface ReviewCertificationRequest {
  certStatus: 1 | 2
  auditRemark: string
}
