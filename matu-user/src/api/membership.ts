import axios from 'axios'
import { request } from './request'
import type { ApiResponse } from './type/postTypings'

export type MembershipProductCode = 'VIP_MONTH' | 'VIP_QUARTER' | 'VIP_YEAR'
export type PaymentOrderStatus = 0 | 1 | 2 | 3 | 4

export interface MembershipOrder {
  orderNo: string
  productName: string
  amount: number | string
  status: PaymentOrderStatus
  payDeadline: string
  createdAt: string
}

export interface AlipayQrPayment {
  orderNo: string
  transactionNo: string
  amount: number | string
  qrCode: string
}

export interface VipStatus {
  userId: number | string
  valid: boolean
  isVip: number
  vipLevel: number
  vipExpiredAt: string | null
  vipDaysRemaining: number
}

// The shared Axios instance does not reject business errors in HTTP 200 responses.
const unwrap = <T>(response: ApiResponse<T>): T => {
  if (response.code !== 0 || response.success === false || response.data == null) {
    throw new Error(response.message || response.msg || '请求失败，请稍后重试')
  }
  return response.data
}

export async function getMyVipStatus(signal?: AbortSignal): Promise<VipStatus> {
  const response = await request.get<ApiResponse<VipStatus>>('/auth/me/vip', { signal })
  return unwrap(response.data)
}

export async function createMembershipOrder(productCode: MembershipProductCode, signal?: AbortSignal): Promise<MembershipOrder> {
  const response = await request.post<ApiResponse<MembershipOrder>>('/pay/orders', { productCode }, { signal })
  const order = unwrap(response.data)
  if (!order.orderNo) throw new Error('订单响应缺少订单号，请先确认是否已创建订单')
  return order
}

export async function getMembershipOrder(orderNo: string, signal?: AbortSignal): Promise<MembershipOrder> {
  const response = await request.get<ApiResponse<MembershipOrder>>(`/pay/orders/${encodeURIComponent(orderNo)}`, { signal })
  const order = unwrap(response.data)
  if (order.orderNo !== orderNo || ![0, 1, 2, 3, 4].includes(order.status)) {
    throw new Error('订单响应异常，请稍后重试查询')
  }
  return order
}

export async function createAlipayQrPayment(orderNo: string, signal?: AbortSignal): Promise<AlipayQrPayment> {
  const response = await request.post<ApiResponse<AlipayQrPayment>>('/pay/alipay/qr', { orderNo }, { signal })
  const payment = unwrap(response.data)
  if (payment.orderNo !== orderNo || !payment.qrCode?.trim()) {
    throw new Error('支付二维码生成失败，请重试当前订单')
  }
  return payment
}

export function getMembershipError(error: unknown): string {
  if (axios.isAxiosError(error)) {
    if (error.response?.status === 401) return '登录已失效，请重新登录后重试'
    return error.response?.data?.message || error.response?.data?.msg || '网络请求失败，请稍后重试'
  }
  return error instanceof Error ? error.message : '请求失败，请稍后重试'
}
