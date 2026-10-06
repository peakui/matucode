import type { AxiosResponse } from 'axios'
import { request } from './request'
import type { ApiResponse } from './type/postTypings'
import type { SearchPageResponse, SearchType } from './type/searchTypings'

export async function searchContent(
  params: { keyword: string; type?: SearchType; pageNum?: number; pageSize?: number },
  signal?: AbortSignal,
): Promise<SearchPageResponse> {
  const response: AxiosResponse<ApiResponse<SearchPageResponse | null>> = await request.get('/search', { params, signal })
  const payload = response.data
  if (payload.code !== 0) throw new Error(payload.message || '搜索失败，请稍后重试。')
  if (!payload.data || !Array.isArray(payload.data.records)) throw new Error('搜索服务返回了无效数据，请重试。')
  return payload.data
}
