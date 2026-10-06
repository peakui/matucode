export interface AdminOrderVO {
  id: string
  orderNo: string
  userId: string
  productName: string
  amount: number
  status: number
  payDeadline?: string
  createdAt?: string
  updatedAt?: string
}

export interface AdminTransactionVO {
  id: string
  transactionNo: string
  orderNo: string
  channel: string
  channelTradeNo?: string | null
  amount: number
  status: number
  payTime?: string | null
  createdAt?: string
}

export interface AdminRefundVO {
  id: string
  refundNo: string
  transactionNo: string
  orderNo: string
  refundAmount: number
  reason?: string | null
  status: number
  channelRefundNo?: string | null
  refundTime?: string | null
  createdAt?: string
}

export interface AdminOrderListParams {
  orderNo?: string
  userId?: string
  status?: number
  productName?: string
  startTime?: string
  endTime?: string
  pageNum?: number
  pageSize?: number
}

export interface AdminTransactionListParams {
  transactionNo?: string
  orderNo?: string
  channel?: string
  status?: number
  startTime?: string
  endTime?: string
  pageNum?: number
  pageSize?: number
}

export interface AdminRefundListParams {
  refundNo?: string
  transactionNo?: string
  orderNo?: string
  status?: number
  startTime?: string
  endTime?: string
  pageNum?: number
  pageSize?: number
}

export interface AdminRefundRequest {
  transactionNo: string
  refundAmount: number
  reason: string
}

export interface RefundVO {
  refundNo: string
  transactionNo: string
  orderNo: string
  refundAmount: number
  reason?: string | null
  status: number
  channelRefundNo?: string | null
  refundTime?: string | null
  createdAt?: string
}
