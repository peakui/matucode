export type SearchType = 'all' | 'article' | 'course' | 'interview' | 'oj'

export interface SearchResultItem {
  id: string
  type: Exclude<SearchType, 'all'>
  title?: string
  summary?: string
  tags?: string[]
  categoryName?: string
  difficulty?: number
  coverUrl?: string
  targetPath?: string
  publishedAt?: string
  createdAt?: string
}

export interface SearchPageResponse {
  pageNum: number
  pageSize: number
  total: number
  totalPages: number
  records: SearchResultItem[]
}
