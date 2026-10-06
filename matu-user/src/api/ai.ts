import type { AxiosResponse } from 'axios'
import { getAuthorizationHeader, request as http } from './request'
import type {
  AiConversationQuery,
  AiConversationVO,
  AiMessageVO,
  ApiResponse,
  PageParams,
  PageResponse,
} from './type/aiTypings'

export type AiStreamRequest = {
  conversationId: string
  message: string
  scene?: string
  stream?: boolean
}

const emptyPage = <T>(params: PageParams = {}): PageResponse<T> => ({
  pageNum: Number(params.pageNum || 1),
  pageSize: Number(params.pageSize || 20),
  total: 0,
  totalPages: 0,
  records: [],
})

// The AI service returns HTTP 200 with a non-zero code for business errors
// (AiExceptionHandler), so the shared axios interceptor never rejects.
const assertSuccess = (body: ApiResponse<unknown>) => {
  if (body.code !== 0) {
    throw new Error(body.message || 'AI 服务暂时不可用')
  }
}

export async function listAiConversations(params: AiConversationQuery = {}): Promise<PageResponse<AiConversationVO>> {
  const response: AxiosResponse<ApiResponse<PageResponse<AiConversationVO> | null>> = await http.get('/ai/conversations', { params })
  return response.data.data || emptyPage<AiConversationVO>(params)
}

export async function deleteAiConversation(conversationId: string): Promise<void> {
  const response: AxiosResponse<ApiResponse<unknown>> = await http.delete(
    `/ai/conversations/${encodeURIComponent(conversationId)}`,
  )
  assertSuccess(response.data)
}

export async function renameAiConversation(conversationId: string, title: string): Promise<void> {
  const response: AxiosResponse<ApiResponse<unknown>> = await http.put(
    `/ai/conversations/${encodeURIComponent(conversationId)}`,
    { title },
  )
  assertSuccess(response.data)
}

export async function listAiMessages(conversationId: string, params: PageParams = {}): Promise<PageResponse<AiMessageVO>> {
  const response: AxiosResponse<ApiResponse<PageResponse<AiMessageVO> | null>> = await http.get(
    `/ai/conversations/${encodeURIComponent(conversationId)}/messages`,
    { params },
  )
  return response.data.data || emptyPage<AiMessageVO>(params)
}

// POST + Authorization 不能使用原生 EventSource。
export async function streamAiChat(
  request: AiStreamRequest,
  handlers: { onToken: (token: string) => void },
  signal?: AbortSignal,
) {
  const authorization = getAuthorizationHeader()
  if (!authorization) throw new Error('请先登录后再使用 AI 助手')
  const response = await fetch(`${String(http.defaults.baseURL || '/api').replace(/\/$/, '')}/ai/chat/stream`, {
    method: 'POST',
    headers: {
      Accept: 'text/event-stream',
      'Content-Type': 'application/json',
      Authorization: authorization,
      'X-Requested-With': 'XMLHttpRequest',
    },
    body: JSON.stringify({ ...request, stream: true }),
    signal,
  })
  if (!response.ok || !response.headers.get('content-type')?.includes('text/event-stream')) {
    let message = response.status === 401 ? '登录已失效，请重新登录' : `AI 接口异常（HTTP ${response.status}），请检查网关路由和服务状态`
    try {
      const body = await response.json() as { message?: string }
      if (body.message) message = body.message
    } catch { /* 不向用户显示代理返回的 HTML */ }
    throw new Error(message)
  }
  if (!response.body) throw new Error('AI 服务没有返回流式响应')
  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let finished = false
  let receivedToken = false
  const emit = (event: string) => {
    const lines = event.split(/\r?\n/)
    const name = lines.find(line => line.startsWith('event:'))?.slice(6).trim() || 'message'
    const data = lines.filter(line => line.startsWith('data:')).map(line => line.slice(5).replace(/^ /, '')).join('\n')
    if (name === 'error') throw new Error(data || 'AI 服务返回错误')
    if (name === 'done' || data === '[DONE]') { finished = true; return }
    if ((name === 'token' || name === 'message') && data) {
      receivedToken = true
      handlers.onToken(data)
    }
  }
  try {
    while (!finished) {
      const { done, value } = await reader.read()
      buffer += decoder.decode(value, { stream: !done })
      const events = buffer.split(/\r?\n\r?\n/)
      buffer = events.pop() || ''
      for (const event of events) {
        emit(event)
        if (finished) break
      }
      if (done) break
    }
    if (!finished) throw new Error('回答连接意外中断，请重新发送')
    if (!receivedToken) throw new Error('模型未返回文字，请检查模型配置和上游响应')
  } finally {
    await reader.cancel().catch(() => undefined)
    reader.releaseLock()
  }
}
