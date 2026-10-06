export interface OperationLogVO {
  id: string
  userId?: string
  username?: string
  module?: string
  action?: string
  targetType?: string
  targetId?: string
  detail?: string
  ipAddress?: string
  userAgent?: string
  result?: number
  errorMsg?: string
  createdAt?: string
}

export interface OperationLogListParams {
  userId?: string
  username?: string
  module?: string
  action?: string
  targetType?: string
  targetId?: string
  result?: number
  startTime?: string
  endTime?: string
  pageNum?: number
  pageSize?: number
}
