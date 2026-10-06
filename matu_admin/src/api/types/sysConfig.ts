export interface SysConfigVO {
  configKey: string
  configValue?: string
  description?: string
  groupName?: string
  isPublic?: number
  updatedAt?: string
}

export interface SysConfigListParams {
  groupName?: string
  isPublic?: number
  keyword?: string
  pageNum?: number
  pageSize?: number
}

export interface CreateSysConfigRequest {
  configKey: string
  configValue?: string
  description?: string
  groupName?: string
  isPublic?: number
}

export interface UpdateSysConfigRequest {
  configValue?: string
  description?: string
  groupName?: string
  isPublic?: number
}
