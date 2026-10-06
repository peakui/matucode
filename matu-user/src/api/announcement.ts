import type { AxiosResponse } from 'axios'
import { request } from './request'
import type { ApiResponse } from './type/postTypings'
import type { AnnouncementDetailVO, AnnouncementPageResponse, ListAnnouncementsParams } from './type/announcementTypings'

const getFallbackAnnouncementPage = (params: ListAnnouncementsParams): AnnouncementPageResponse => ({
  pageNum: Number(params.pageNum || 1),
  pageSize: Number(params.pageSize || 10),
  total: 0,
  totalPages: 0,
  records: [],
})

async function listAnnouncements(params: ListAnnouncementsParams = {}): Promise<AnnouncementPageResponse> {
  const response: AxiosResponse<ApiResponse<AnnouncementPageResponse | null>> = await request.get('/announcements', { params })
  return response.data.data || getFallbackAnnouncementPage(params)
}

async function getAnnouncementDetail(id: string | number): Promise<AnnouncementDetailVO> {
  const response: AxiosResponse<ApiResponse<AnnouncementDetailVO>> = await request.get(`/announcements/${String(id)}`)
  return response.data.data
}

export { listAnnouncements, getAnnouncementDetail }