import { SearchOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Empty, Input, Pagination, Select, Space, Spin, Tag } from 'antd'
import axios from 'axios'
import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { searchContent } from '../../api/search'
import type { SearchPageResponse, SearchType } from '../../api/type/searchTypings'
import { toPlainSummary } from '../../utils/markdownContent'
import { buildSearchPath, getSearchTargetPath, readSearchParams, SEARCH_KEYWORD_LIMIT, SEARCH_WINDOW } from '../../utils/search'
import './page.scss'

const typeOptions = [
  { label: '全部内容', value: 'all' },
  { label: '文章', value: 'article' },
  { label: '课程', value: 'course' },
  { label: '面试题', value: 'interview' },
  { label: 'OJ 题目', value: 'oj' },
]
const typeLabel: Record<string, string> = { article: '文章', course: '课程', interview: '面试题', oj: 'OJ 题目' }
const difficultyLabel: Record<number, string> = { 1: '简单', 2: '中等', 3: '困难' }

function getErrorMessage(error: unknown) {
  if (axios.isAxiosError(error)) {
    if (error.response?.status === 503) return '搜索服务暂时不可用，请稍后重试。'
    if (error.response?.status === 400) return '搜索参数无效，请检查关键词、类型和分页后重新搜索。'
    return '搜索请求失败，请检查网络后重试。'
  }
  return error instanceof Error ? error.message : '搜索失败，请稍后重试。'
}

export function SearchPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const location = useLocation()
  const { keyword, type, pageNum, pageSize, error: parameterError } = readSearchParams(searchParams)
  const [retry, setRetry] = useState(0)
  const [result, setResult] = useState<{ key: string; page?: SearchPageResponse; error?: string }>()
  // A navigation (including resubmitting the same URL) or retry owns its own response.
  const requestKey = `${location.key}:${location.search}:${retry}`
  const canSearch = Boolean(keyword) && !parameterError
  const loading = canSearch && result?.key !== requestKey
  const currentResult = result?.key === requestKey ? result : undefined
  const error = parameterError || currentResult?.error
  const page = currentResult?.page
  const safePageSize = Number.isInteger(pageSize) && pageSize >= 1 && pageSize <= 50 ? pageSize : 10

  useEffect(() => {
    if (!canSearch) return
    const controller = new AbortController()
    const load = async () => {
      try {
        const data = await searchContent({ keyword, type, pageNum, pageSize }, controller.signal)
        if (!controller.signal.aborted) setResult({ key: requestKey, page: data })
      } catch (fetchError) {
        if (!controller.signal.aborted && !axios.isCancel(fetchError)) {
          setResult({ key: requestKey, error: getErrorMessage(fetchError) })
        }
      }
    }
    void load()
    return () => controller.abort()
  }, [canSearch, keyword, type, pageNum, pageSize, requestKey])

  const changeSearch = (value: string, nextType = type, nextPage = 1, nextSize = safePageSize) => {
    const nextPath = buildSearchPath(value, nextType, nextPage, nextSize)
    const nextSearch = nextPath.slice('/search'.length)
    if (location.search === nextSearch) {
      setRetry((current) => current + 1)
      return
    }
    navigate(nextPath)
  }
  const accessibleTotal = Math.min(page?.total || 0, Math.floor(SEARCH_WINDOW / safePageSize) * safePageSize)

  return (
    <Space orientation="vertical" size={18} className="full-width search-page">
      <Card className="content-card search-page__hero" variant="borderless">
        <div className="channel-hero__label">全站搜索</div>
        <h1>搜索文章、课程和题目</h1>
        <p>只展示已公开发布的内容摘要，进入详情页后查看完整内容。</p>
        <div className="search-page__toolbar">
          <Input.Search
            key={location.key}
            defaultValue={keyword}
            maxLength={SEARCH_KEYWORD_LIMIT}
            prefix={<SearchOutlined />}
            placeholder="输入关键词，最多 100 个字符"
            aria-label="搜索关键词"
            allowClear
            enterButton="搜索"
            onSearch={(value) => changeSearch(value)}
          />
          <Select aria-label="搜索内容类型" value={type} options={typeOptions} onChange={(value: SearchType) => changeSearch(keyword, value)} />
        </div>
      </Card>

      <Card className="content-card" variant="borderless" aria-busy={loading}>
        <div aria-live="polite">
          {loading ? <div className="search-page__loading"><Spin size="large" /><span>正在搜索…</span></div> : null}
          {!loading && error ? (
            <Alert type="error" showIcon title="搜索未完成" description={error} action={
              parameterError
                ? <Button onClick={() => changeSearch(keyword, type, 1, 10)}>重置分页</Button>
                : <Button onClick={() => setRetry((value) => value + 1)}>重试</Button>
            } />
          ) : null}
          {!loading && !error && !keyword ? <Empty description="请输入关键词开始搜索" /> : null}
          {!loading && !error && keyword && page && !page.records.length ? <Empty description={pageNum > 1 ? '本页没有结果，请返回第一页或调整搜索条件' : '没有找到匹配内容，请尝试其他关键词或类型'} /> : null}
          {!loading && !error && page ? (
            <div className="search-page__results">
              <div className="search-page__summary">找到 {page.total} 条结果{page.total > accessibleTotal ? '，最多可查看前 10000 条，请缩小搜索范围。' : ''}</div>
              {page.records.map((item) => {
                const targetPath = getSearchTargetPath(item)
                const content = (
                  <div className="search-result-card">
                    <div className="search-result-card__meta">
                      <Tag color="blue">{typeLabel[item.type] || '内容'}</Tag>
                      {item.categoryName ? <span>{item.categoryName}</span> : null}
                      {(item.type === 'oj' || item.type === 'interview') && item.difficulty ? <Tag>{difficultyLabel[item.difficulty] || `难度 ${item.difficulty}`}</Tag> : null}
                    </div>
                    <h2>{item.title || '未命名内容'}</h2>
                    <p>{toPlainSummary(item.summary) || '暂无摘要'}</p>
                    <Space wrap>{(item.tags || []).map((tag, index) => <Tag key={`${tag}-${index}`}>{tag}</Tag>)}</Space>
                  </div>
                )
                return targetPath ? <Link to={targetPath} key={`${item.type}-${item.id}`} className="search-result-link">{content}</Link> : <div key={`${item.type}-${item.id}`}>{content}</div>
              })}
              {pageNum > 1 && !page.records.length ? <Button onClick={() => changeSearch(keyword)}>返回第一页</Button> : null}
              {accessibleTotal > 0 ? <Pagination
                className="search-page__pagination"
                current={pageNum}
                pageSize={safePageSize}
                total={accessibleTotal}
                showSizeChanger
                pageSizeOptions={[10, 20, 50]}
                responsive
                onChange={(nextPage, nextSize) => changeSearch(keyword, type, nextSize === safePageSize ? nextPage : 1, nextSize)}
              /> : null}
            </div>
          ) : null}
        </div>
      </Card>
    </Space>
  )
}
