import type { AxiosResponse } from 'axios'
import { request } from './request'
import type {
  ApiResponse,
  AuthProfileVO,
  CertificationRecordVO,
  FollowStatusVO,
  PageResponse,
  SubmitCertificationRequest,
  UpdateAvatarRequest,
  UpdateAuthProfileRequest,
  UserFollowVO,
} from './type/loginTypings.ts'

async function getCurrentUserProfile(): Promise<AuthProfileVO> {
  const response: AxiosResponse<ApiResponse<AuthProfileVO>> = await request.get('/auth/me')
  return response.data.data
}

const isNumericIdentifier = (value: string | number) => /^\d+$/.test(String(value).trim())

async function getUserProfile(userId: string | number): Promise<AuthProfileVO> {
  const normalizedUserId = String(userId).trim()
  const endpoint = isNumericIdentifier(normalizedUserId)
    ? `/auth/users/${encodeURIComponent(normalizedUserId)}`
    : `/auth/users/by-username/${encodeURIComponent(normalizedUserId)}`
  const response: AxiosResponse<ApiResponse<AuthProfileVO>> = await request.get(endpoint)
  return response.data.data
}

async function updateCurrentUserProfile(data: UpdateAuthProfileRequest): Promise<AuthProfileVO> {
  const response: AxiosResponse<ApiResponse<AuthProfileVO>> = await request.put('/auth/me', data)
  return response.data.data
}

async function updateCurrentUserAvatar(data: UpdateAvatarRequest): Promise<AuthProfileVO> {
  const response: AxiosResponse<ApiResponse<AuthProfileVO>> = await request.put('/auth/me/avatar', data)
  return response.data.data
}

async function submitCertification(data: SubmitCertificationRequest): Promise<CertificationRecordVO> {
  const response: AxiosResponse<ApiResponse<CertificationRecordVO>> = await request.post('/auth/certifications', data)
  return response.data.data
}

type FollowListParams = { pageNum?: number; pageSize?: number }
type UserFollowPageResponse = PageResponse<UserFollowVO>
type CertificationListResponse = PageResponse<CertificationRecordVO>

const getFallbackFollowPage = (params: FollowListParams = {}): UserFollowPageResponse => ({
  pageNum: Number(params.pageNum || 1),
  pageSize: Number(params.pageSize || 10),
  total: 0,
  totalPages: 0,
  records: [],
})

const getFallbackCertificationPage = (params: FollowListParams = {}): CertificationListResponse => ({
  pageNum: Number(params.pageNum || 1),
  pageSize: Number(params.pageSize || 10),
  total: 0,
  totalPages: 0,
  records: [],
})

async function followUser(userId: string | number): Promise<void> {
  await request.post(`/auth/users/${encodeURIComponent(String(userId))}/follow`)
}

async function unfollowUser(userId: string | number): Promise<void> {
  await request.delete(`/auth/users/${encodeURIComponent(String(userId))}/follow`)
}

async function listMyFollowing(params: FollowListParams = {}): Promise<UserFollowPageResponse> {
  const response: AxiosResponse<ApiResponse<UserFollowPageResponse | null>> = await request.get('/auth/me/following', { params })
  return response.data.data || getFallbackFollowPage(params)
}

async function listMyFollowers(params: FollowListParams = {}): Promise<UserFollowPageResponse> {
  const response: AxiosResponse<ApiResponse<UserFollowPageResponse | null>> = await request.get('/auth/me/followers', { params })
  return response.data.data || getFallbackFollowPage(params)
}

async function listUserFollowing(userId: string | number, params: FollowListParams = {}): Promise<UserFollowPageResponse> {
  const response: AxiosResponse<ApiResponse<UserFollowPageResponse | null>> = await request.get(`/auth/users/${encodeURIComponent(String(userId))}/following`, { params })
  return response.data.data || getFallbackFollowPage(params)
}

async function listUserFollowers(userId: string | number, params: FollowListParams = {}): Promise<UserFollowPageResponse> {
  const response: AxiosResponse<ApiResponse<UserFollowPageResponse | null>> = await request.get(`/auth/users/${encodeURIComponent(String(userId))}/followers`, { params })
  return response.data.data || getFallbackFollowPage(params)
}

async function getUserFollowStatus(userId: string | number): Promise<FollowStatusVO> {
  const response: AxiosResponse<ApiResponse<FollowStatusVO | null>> = await request.get(`/auth/users/${encodeURIComponent(String(userId))}/follow-status`)
  return response.data.data || { isFollowing: false }
}

async function listMyCertifications(params: FollowListParams = {}): Promise<CertificationListResponse> {
  const response: AxiosResponse<ApiResponse<CertificationListResponse | null>> = await request.get('/auth/me/certifications', { params })
  return response.data.data || getFallbackCertificationPage(params)
}

export {
  followUser,
  getCurrentUserProfile,
  getUserFollowStatus,
  getUserProfile,
  listMyCertifications,
  listMyFollowers,
  listMyFollowing,
  listUserFollowers,
  listUserFollowing,
  submitCertification,
  unfollowUser,
  updateCurrentUserAvatar,
  updateCurrentUserProfile,
}
