import type { AxiosResponse } from 'axios'
import { request } from './request'
import type { ApiResponse, AuthTokenVO, EmailCodeRequest, LoginRequest, RegisterRequest } from './type/loginTypings.ts'

const isSuccessResponse = (response: ApiResponse<unknown>) => (
  response.code === 0 || response.code === 200 || response.code === 20000 || response.success === true
)

async function login(data: LoginRequest): Promise<AuthTokenVO> {
  const response: AxiosResponse<ApiResponse<AuthTokenVO>> = await request.post('/auth/login', data)
  return response.data.data
}

async function sendRegisterEmailCode(data: EmailCodeRequest): Promise<void> {
  const payload = { email: data.email.trim() }
  const response: AxiosResponse<ApiResponse<unknown>> = await request.post('/auth/register/email-code', payload)

  if (!isSuccessResponse(response.data)) {
    throw new Error(response.data.message || response.data.msg || '验证码发送失败')
  }
}

async function register(data: RegisterRequest): Promise<AuthTokenVO> {
  const response: AxiosResponse<ApiResponse<AuthTokenVO>> = await request.post('/auth/register', data)
  return response.data.data
}

async function logout(): Promise<void> {
  await request.post('/auth/logout')
}

export { login, sendRegisterEmailCode, register, logout }
