import type { AxiosResponse } from 'axios'
import { request } from './request'
import type {
  ApiResponse,
  CheckCommentVO,
  CheckDaysStatisticsVO,
  CheckGroupVO,
  CheckRecordListItemVO,
  CheckRecordVO,
  CheckStatisticsVO,
  CreateCheckCommentRequest,
  CreateCheckGroupRequest,
  CreateCheckRecordRequest,
  ListCheckRecordsParams,
  PageResponse,
  UpdateCheckRecordRequest,
  UserAchievementVO,
} from './type/checkTypings'

async function createCheckRecord(data: CreateCheckRecordRequest): Promise<CheckRecordVO> {
  const response: AxiosResponse<ApiResponse<CheckRecordVO>> = await request.post('/checks', data)
  return response.data.data
}

async function updateCheckRecord(checkId: string | number, data: UpdateCheckRecordRequest): Promise<CheckRecordVO> {
  const response: AxiosResponse<ApiResponse<CheckRecordVO>> = await request.put(`/checks/${String(checkId)}`, data)
  return response.data.data
}

async function deleteCheckRecord(checkId: string | number): Promise<void> {
  await request.delete(`/checks/${String(checkId)}`)
}

async function getCheckRecordDetail(checkId: string | number): Promise<CheckRecordVO> {
  const response: AxiosResponse<ApiResponse<CheckRecordVO>> = await request.get(`/checks/${String(checkId)}`)
  return response.data.data
}

async function listCheckRecords(params: ListCheckRecordsParams = {}): Promise<PageResponse<CheckRecordListItemVO>> {
  const response: AxiosResponse<ApiResponse<PageResponse<CheckRecordListItemVO> | null>> = await request.get('/checks', { params })
  return response.data.data || {
    pageNum: Number(params.pageNum || 1),
    pageSize: Number(params.pageSize || 10),
    total: 0,
    totalPages: 0,
    records: [],
  }
}

async function listLikedCheckRecords(params: ListCheckRecordsParams = {}): Promise<PageResponse<CheckRecordListItemVO>> {
  const response: AxiosResponse<ApiResponse<PageResponse<CheckRecordListItemVO> | null>> = await request.get('/checks/liked', { params })
  return response.data.data || {
    pageNum: Number(params.pageNum || 1),
    pageSize: Number(params.pageSize || 10),
    total: 0,
    totalPages: 0,
    records: [],
  }
}

async function getCheckStatistics(params: Pick<ListCheckRecordsParams, 'userId' | 'year' | 'month'> = {}): Promise<CheckStatisticsVO> {
  const response: AxiosResponse<ApiResponse<CheckStatisticsVO | null>> = await request.get('/checks/statistics', { params })
  return response.data.data || {}
}

async function getCheckDaysStatistics(userId?: string | number): Promise<CheckDaysStatisticsVO> {
  const response: AxiosResponse<ApiResponse<CheckDaysStatisticsVO | null>> = await request.get('/checks/statistics/days', {
    params: userId ? { userId } : undefined,
  })
  return response.data.data || {}
}

async function listCheckAchievements(userId?: string | number): Promise<UserAchievementVO[]> {
  const response: AxiosResponse<ApiResponse<UserAchievementVO[] | null>> = await request.get('/checks/achievements', {
    params: userId ? { userId } : undefined,
  })
  return response.data.data || []
}

async function likeCheckRecord(checkId: string | number): Promise<void> {
  await request.post(`/checks/${String(checkId)}/like`)
}

async function unlikeCheckRecord(checkId: string | number): Promise<void> {
  await request.delete(`/checks/${String(checkId)}/like`)
}

async function shareCheckRecord(checkId: string | number): Promise<void> {
  await request.post(`/checks/${String(checkId)}/share`)
}

async function createCheckComment(checkId: string | number, data: CreateCheckCommentRequest): Promise<CheckCommentVO> {
  const response: AxiosResponse<ApiResponse<CheckCommentVO>> = await request.post(`/checks/${String(checkId)}/comments`, data)
  return response.data.data
}

async function listCheckComments(checkId: string | number): Promise<CheckCommentVO[]> {
  const response: AxiosResponse<ApiResponse<CheckCommentVO[]>> = await request.get(`/checks/${String(checkId)}/comments`)
  return response.data.data
}

async function deleteCheckComment(checkId: string | number, commentId: string | number): Promise<void> {
  await request.delete(`/checks/${String(checkId)}/comments/${String(commentId)}`)
}

async function likeCheckComment(checkId: string | number, commentId: string | number): Promise<void> {
  await request.post(`/checks/${String(checkId)}/comments/${String(commentId)}/like`)
}

async function unlikeCheckComment(checkId: string | number, commentId: string | number): Promise<void> {
  await request.delete(`/checks/${String(checkId)}/comments/${String(commentId)}/like`)
}

async function createCheckGroup(data: CreateCheckGroupRequest): Promise<CheckGroupVO> {
  const response: AxiosResponse<ApiResponse<CheckGroupVO>> = await request.post('/checks/groups', data)
  return response.data.data
}

async function listPublicCheckGroups(): Promise<CheckGroupVO[]> {
  const response: AxiosResponse<ApiResponse<CheckGroupVO[] | null>> = await request.get('/checks/groups')
  return response.data.data || []
}

async function joinCheckGroup(groupId: string | number): Promise<void> {
  await request.post(`/checks/groups/${String(groupId)}/join`)
}

export {
  createCheckComment,
  createCheckGroup,
  createCheckRecord,
  deleteCheckComment,
  deleteCheckRecord,
  getCheckDaysStatistics,
  getCheckRecordDetail,
  getCheckStatistics,
  joinCheckGroup,
  likeCheckComment,
  likeCheckRecord,
  listCheckAchievements,
  listCheckComments,
  listCheckRecords,
  listLikedCheckRecords,
  listPublicCheckGroups,
  shareCheckRecord,
  unlikeCheckComment,
  unlikeCheckRecord,
  updateCheckRecord,
}
