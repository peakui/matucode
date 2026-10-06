import { request } from '@/utils/request'
import type {
  AdminOrderListParams,
  AdminOrderVO,
  AdminRefundListParams,
  AdminRefundRequest,
  AdminRefundVO,
  AdminTransactionListParams,
  AdminTransactionVO,
  PageResponse,
  RefundVO,
} from '@/api/types'

export const getAdminOrderListApi = (params: AdminOrderListParams) => {
  return request<PageResponse<AdminOrderVO>>({
    url: '/pay/admin/orders',
    method: 'get',
    params,
  })
}

export const getAdminOrderDetailApi = (orderNo: string) => {
  return request<AdminOrderVO>({
    url: `/pay/admin/orders/${encodeURIComponent(orderNo)}`,
    method: 'get',
  })
}

export const getAdminTransactionListApi = (params: AdminTransactionListParams) => {
  return request<PageResponse<AdminTransactionVO>>({
    url: '/pay/admin/transactions',
    method: 'get',
    params,
  })
}

export const getAdminRefundListApi = (params: AdminRefundListParams) => {
  return request<PageResponse<AdminRefundVO>>({
    url: '/pay/admin/refunds',
    method: 'get',
    params,
  })
}

export const createAdminRefundApi = (data: AdminRefundRequest) => {
  return request<RefundVO>({
    url: '/pay/admin/refunds',
    method: 'post',
    data,
  })
}
