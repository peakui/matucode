import { request } from '@/utils/request'
import type {
  AnnouncementListParams,
  AnnouncementVO,
  CreateAnnouncementRequest,
  PageResponse,
  UpdateAnnouncementRequest,
} from '@/api/types'

export const getAdminAnnouncementListApi = (params: AnnouncementListParams) => {
  return request<PageResponse<AnnouncementVO>>({
    url: '/admin/info/announcements',
    method: 'get',
    params,
  })
}

export const getAdminAnnouncementDetailApi = (id: string) => {
  return request<AnnouncementVO>({
    url: `/admin/info/announcements/${id}`,
    method: 'get',
  })
}

export const createAdminAnnouncementApi = (data: CreateAnnouncementRequest) => {
  return request<AnnouncementVO>({
    url: '/admin/info/announcements',
    method: 'post',
    data,
  })
}

export const updateAdminAnnouncementApi = (id: string, data: UpdateAnnouncementRequest) => {
  return request<AnnouncementVO>({
    url: `/admin/info/announcements/${id}`,
    method: 'put',
    data,
  })
}

export const deleteAdminAnnouncementApi = (id: string) => {
  return request<null>({
    url: `/admin/info/announcements/${id}`,
    method: 'delete',
  })
}

export const publishAdminAnnouncementApi = (id: string) => {
  return request<null>({
    url: `/admin/info/announcements/${id}/publish`,
    method: 'post',
  })
}

export const offlineAdminAnnouncementApi = (id: string) => {
  return request<null>({
    url: `/admin/info/announcements/${id}/offline`,
    method: 'post',
  })
}
