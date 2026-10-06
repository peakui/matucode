import type { AxiosResponse } from 'axios'
import { request } from './request'
import type { ApiResponse } from './type/loginTypings.ts'

export interface ActiveDeveloperVO {
  userId?: string | number
  username?: string
  nickname?: string
  avatarUrl?: string
  signature?: string
  schoolName?: string
  authorSchoolName?: string
  userSchoolName?: string
  schoolVerified?: number | boolean
  companyName?: string
  authorCompanyName?: string
  userCompanyName?: string
  companyVerified?: number | boolean
  authorTitle?: string
  userTitle?: string
  jobTitle?: string
  titleVerified?: number | boolean
  activityLevel?: number
}

async function listActiveDevelopers(): Promise<ActiveDeveloperVO[]> {
  const response: AxiosResponse<ApiResponse<ActiveDeveloperVO[]>> = await request.get('/auth/users/active-developers')
  return response.data.data || []
}

export { listActiveDevelopers }
