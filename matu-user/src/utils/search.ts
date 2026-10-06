import type { SearchResultItem, SearchType } from '../api/type/searchTypings'

export const SEARCH_WINDOW = 10000
export const SEARCH_KEYWORD_LIMIT = 100
export const searchTypes: SearchType[] = ['all', 'article', 'course', 'interview', 'oj']

export function readSearchParams(params: URLSearchParams) {
  const keyword = (params.get('keyword') || '').trim()
  const rawType = params.get('type') || 'all'
  const type = searchTypes.includes(rawType as SearchType) ? rawType as SearchType : 'all'
  const rawPage = params.get('pageNum') ?? '1'
  const rawSize = params.get('pageSize') ?? '10'
  const pageNum = Number(rawPage)
  const pageSize = Number(rawSize)
  let error = ''
  if (keyword.length > SEARCH_KEYWORD_LIMIT) error = '关键词最多为 100 个字符，请缩短后重新搜索。'
  else if (!searchTypes.includes(rawType as SearchType)) error = '不支持的搜索类型，请重新选择内容类型。'
  else if (!/^\d+$/.test(rawPage) || !Number.isSafeInteger(pageNum) || pageNum < 1
    || !/^\d+$/.test(rawSize) || !Number.isSafeInteger(pageSize) || pageSize < 1 || pageSize > 50) {
    error = '分页参数无效：页码须为正整数，每页最多 50 条。'
  } else if (pageNum * pageSize > SEARCH_WINDOW) error = '只能查看前 10000 条结果，请缩小搜索范围或返回第一页。'
  return { keyword, type, pageNum, pageSize, error }
}

export function buildSearchPath(keyword: string, type: SearchType = 'all', pageNum = 1, pageSize = 10) {
  return `/search?${new URLSearchParams({ keyword: keyword.trim(), type, pageNum: String(pageNum), pageSize: String(pageSize) })}`
}

// Only known content routes are allowed; never trust a server-provided URL as a navigation target.
export function getSearchTargetPath(item: SearchResultItem): string | undefined {
  const prefixes: Record<Exclude<SearchType, 'all'>, string> = {
    article: '/article/', course: '/detail/course/', interview: '/detail/interview-question/', oj: '/problem/',
  }
  const prefix = prefixes[item.type]
  if (!prefix) return undefined
  const validId = (id: string) => /^[a-zA-Z0-9_-]+$/.test(id) && id !== 'editor'
  if (item.targetPath?.startsWith(prefix) && validId(item.targetPath.slice(prefix.length))) return item.targetPath
  const id = String(item.id ?? '').trim()
  if (validId(id)) return `${prefix}${id}`
  return item.type === 'interview' ? '/interviews' : undefined
}
