export interface PlatformMetricVO {
  id: string
  metricDate: string
  metricKey: string
  metricValue: number
  extra?: string
  createdAt?: string
}

export interface PlatformMetricListParams {
  metricKey?: string
  startDate?: string
  endDate?: string
  pageNum?: number
  pageSize?: number
}

export interface CreatePlatformMetricRequest {
  metricDate: string
  metricKey: string
  metricValue: number
  extra?: string
}

export interface UpdatePlatformMetricRequest {
  metricValue: number
  extra?: string
}
