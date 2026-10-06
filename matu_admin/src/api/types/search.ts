export interface IndexStatusVO {
  alias: string
  indexExists: boolean
  documentCount: number
  maxDocuments: number
}

export interface SyncResultVO {
  success: boolean
  documentCount: number
  message: string
}
