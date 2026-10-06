import { request } from '@/utils/request'
import type {
  AuthTokenVO,
  CertificationVO,
  CreateCertificationRequest,
  InternalCertificationListParams,
  LoginParams,
  MyCertificationListParams,
  PageResponse,
  ReviewCertificationRequest,
} from '@/api/types'

export const loginApi = (data: LoginParams) => {
  return request<AuthTokenVO>({
    url: '/auth/login',
    method: 'post',
    data,
  })
}

export const getCurrentUserApi = () => {
  return request<AuthTokenVO>({
    url: '/auth/me',
    method: 'get',
  })
}

export const logoutApi = () => {
  return request<null>({
    url: '/auth/logout',
    method: 'post',
  })
}

export const createCertificationApi = (data: CreateCertificationRequest) => {
  return request<CertificationVO>({
    url: '/auth/certifications',
    method: 'post',
    data,
  })
}

export const getMyCertificationListApi = (params: MyCertificationListParams = {}) => {
  return request<PageResponse<CertificationVO>>({
    url: '/auth/me/certifications',
    method: 'get',
    params,
  })
}

export const getInternalCertificationListApi = (
  params: InternalCertificationListParams = {},
) => {
  return request<PageResponse<CertificationVO>>({
    url: '/auth/internal/certifications',
    method: 'get',
    params,
  })
}

export const reviewCertificationApi = (
  certificationId: string | number,
  data: ReviewCertificationRequest,
) => {
  return request<CertificationVO>({
    url: `/auth/internal/certifications/${String(certificationId)}/review`,
    method: 'post',
    data,
  })
}
