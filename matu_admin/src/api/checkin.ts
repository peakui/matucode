import { request } from '@/utils/request'
import type {
  CheckCommentVO,
  CheckGroupVO,
  CheckRecordListItemVO,
  CheckRecordListParams,
  CheckRecordVO,
  CheckStatisticsVO,
  ClaimAchievementRewardRequest,
  CreateCheckCommentRequest,
  CreateCheckGroupRequest,
  CreateCheckRecordRequest,
  OperateGroupMemberRequest,
  PageResponse,
  UpdateCheckRecordRequest,
  UserAchievementVO,
} from '@/api/types'

export const getCheckRecordListApi = (params: CheckRecordListParams) => {
  return request<PageResponse<CheckRecordListItemVO>>({
    url: '/checks',
    method: 'get',
    params,
  })
}

export const getCheckRecordDetailApi = (checkId: string) => {
  return request<CheckRecordVO>({
    url: `/checks/${checkId}`,
    method: 'get',
  })
}

export const createCheckRecordApi = (data: CreateCheckRecordRequest) => {
  return request<CheckRecordVO>({
    url: '/checks',
    method: 'post',
    data,
  })
}

export const updateCheckRecordApi = (checkId: string, data: UpdateCheckRecordRequest) => {
  return request<CheckRecordVO>({
    url: `/checks/${checkId}`,
    method: 'put',
    data,
  })
}

export const deleteCheckRecordApi = (checkId: string) => {
  return request<null>({
    url: `/checks/${checkId}`,
    method: 'delete',
  })
}

export const getCheckStatisticsApi = (params: {
  userId?: string
  year?: number
  month?: number
}) => {
  return request<CheckStatisticsVO>({
    url: '/checks/statistics',
    method: 'get',
    params,
  })
}

export const getCheckAchievementsApi = (params: { userId?: string }) => {
  return request<UserAchievementVO[]>({
    url: '/checks/achievements',
    method: 'get',
    params,
  })
}

export const claimCheckAchievementRewardApi = (data: ClaimAchievementRewardRequest) => {
  return request<null>({
    url: '/checks/achievements/claim',
    method: 'post',
    data,
  })
}

export const getCheckCommentsApi = (checkId: string) => {
  return request<CheckCommentVO[]>({
    url: `/checks/${checkId}/comments`,
    method: 'get',
  })
}

export const createCheckCommentApi = (checkId: string, data: CreateCheckCommentRequest) => {
  return request<CheckCommentVO>({
    url: `/checks/${checkId}/comments`,
    method: 'post',
    data,
  })
}

export const deleteCheckCommentApi = (checkId: string, commentId: string) => {
  return request<null>({
    url: `/checks/${checkId}/comments/${commentId}`,
    method: 'delete',
  })
}

export const getCheckGroupListApi = () => {
  return request<CheckGroupVO[]>({
    url: '/checks/groups',
    method: 'get',
  })
}

export const createCheckGroupApi = (data: CreateCheckGroupRequest) => {
  return request<CheckGroupVO>({
    url: '/checks/groups',
    method: 'post',
    data,
  })
}

export const joinCheckGroupApi = (groupId: string) => {
  return request<null>({
    url: `/checks/groups/${groupId}/join`,
    method: 'post',
  })
}

export const quitCheckGroupApi = (groupId: string) => {
  return request<null>({
    url: `/checks/groups/${groupId}/quit`,
    method: 'post',
  })
}

export const removeCheckGroupMemberApi = (groupId: string, data: OperateGroupMemberRequest) => {
  return request<null>({
    url: `/checks/groups/${groupId}/remove-member`,
    method: 'post',
    data,
  })
}
