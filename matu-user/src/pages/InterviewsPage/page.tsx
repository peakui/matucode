import { Button, Card, Col, Empty, Input, Row, Space } from 'antd'
import { SearchOutlined } from '@ant-design/icons'
import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { listInterviewCategories, listInterviewQuestions } from '../../api/interview'
import type { InterviewCategoryVO, InterviewQuestionVO } from '../../api/type/interviewTypings'
import { buildSearchPath, SEARCH_KEYWORD_LIMIT } from '../../utils/search'
import './page.scss'

const flattenCategories = (items: InterviewCategoryVO[]): InterviewCategoryVO[] => {
  const result: InterviewCategoryVO[] = []

  const walk = (nodes: InterviewCategoryVO[]) => {
    nodes.forEach((node) => {
      result.push(node)
      if (node.children?.length) {
        walk(node.children)
      }
    })
  }

  walk(items)
  return result.filter((item) => item.id)
}

export function InterviewsPage() {
  const navigate = useNavigate()
  const [keyword, setKeyword] = useState('')
  const [questions, setQuestions] = useState<InterviewQuestionVO[]>([])
  const [questionTotal, setQuestionTotal] = useState(0)
  const [categories, setCategories] = useState<InterviewCategoryVO[]>([])
  const initializedRef = useRef(false)

  useEffect(() => {
    if (initializedRef.current) {
      return
    }

    initializedRef.current = true

    const initializePage = async () => {
      // Kept independent on purpose: the question list is quota-gated (pageSize is
      // capped at 10/20/100 by role) and legitimately fails on its own. If that
      // failure blanked the whole page, the category grid would vanish with it.
      const [categoryResult, questionResult] = await Promise.allSettled([
        listInterviewCategories(),
        listInterviewQuestions({ pageNum: 1, pageSize: 10 }),
      ])

      if (categoryResult.status === 'fulfilled') {
        setCategories(categoryResult.value)
      } else {
        console.error('load interview categories error:', categoryResult.reason)
        setCategories([])
      }

      if (questionResult.status === 'fulfilled') {
        setQuestions(questionResult.value.records || [])
        setQuestionTotal(questionResult.value.total || 0)
      } else {
        console.error('load interview questions error:', questionResult.reason)
        setQuestions([])
      }
    }

    void initializePage()
  }, [])

  const flatCategories = useMemo(() => flattenCategories(categories), [categories])

  const interviewStats = useMemo(() => {
    const totalQuestions = questionTotal
    const totalCategories = flatCategories.length
    const unlockedQuestions = questions.filter((item) => item.isLocked !== 1).length
    const coverageRate = totalQuestions ? `${Math.round((unlockedQuestions / totalQuestions) * 100)}%` : '0%'

    return [
      { value: String(totalQuestions), label: '精选面试题' },
      { value: String(totalCategories), label: '专题合集' },
      { value: coverageRate, label: '高频覆盖率' },
    ]
  }, [flatCategories.length, questionTotal, questions])

  const categoryCards = useMemo(() => {
    return flatCategories.map((category) => {
      const firstInCategory = questions.find((question) => {
        return question.categoryId === category.id || question.categoryName === category.categoryName
      })

      return {
        category,
        // 真实题目数来自后端聚合，不能用已加载的 10 条列表去数，否则每张卡片最多显示 10。
        questionCount: category.questionCount ?? 0,
        previewText: category.categoryDesc || firstInCategory?.title || '点击进入查看该分类下的高频面试题',
      }
    })
  }, [flatCategories, questions])

  return (
    <Space orientation="vertical" size={16} className="full-width interviews-page">
      <div className="interview-hero-v2 card-surface">
        <div className="interview-hero-v2__main">
          <div className="channel-hero__label">面试题库</div>
          <h2>搜题、刷题、按专题系统复习</h2>
          <p>聚合前端、后端、数据库、系统设计等方向的高频题目，帮助你像使用专业面试平台一样高效准备。</p>
          <div className="interview-search-wrap">
            <Input
              prefix={<SearchOutlined />}
              size="large"
              placeholder="搜索面试题，例如：React diff / MySQL 索引 / Redis 持久化"
              value={keyword}
              maxLength={SEARCH_KEYWORD_LIMIT}
              onPressEnter={() => {
                const normalized = keyword.trim()
                if (normalized) navigate(buildSearchPath(normalized, 'interview'))
              }}
              onChange={(event) => setKeyword(event.target.value)}
            />
            <Button type="primary" size="large" onClick={() => {
              const normalized = keyword.trim()
              if (normalized) navigate(buildSearchPath(normalized, 'interview'))
            }}>
              搜索题目
            </Button>
          </div>
        </div>
        <div className="interview-stats-panel">
          {interviewStats.map((item) => (
            <div key={item.label} className="interview-stat-item">
              <strong>{item.value}</strong>
              <span>{item.label}</span>
            </div>
          ))}
        </div>
      </div>

      <Row gutter={[16, 16]} className="interview-category-grid">
        {categoryCards.length ? (
          categoryCards.map(({ category, questionCount, previewText }) => (
            <Col xs={24} sm={12} xl={6} key={category.id}>
              <Card className="interview-category-card" variant="borderless" hoverable onClick={() => navigate(`/interviews/${category.id}`)}>
                <div className="interview-category-card__content">
                  <div className="interview-category-card__icon">
                    {category.iconUrl ? <img src={category.iconUrl} alt={category.categoryName} /> : <span>{category.categoryName?.slice(0, 1) || '题'}</span>}
                  </div>
                  <div className="interview-category-card__body">
                    <Link to={`/interviews/${category.id}`} className="interview-category-card__title" onClick={(event) => event.stopPropagation()}>
                      {category.categoryName}
                    </Link>
                    <div className="interview-category-card__meta">{questionCount} 道题</div>
                    <p className="interview-category-card__desc">{previewText}</p>
                  </div>
                </div>
              </Card>
            </Col>
          ))
        ) : (
          <Col span={24}>
            <Card className="content-card" variant="borderless">
              <Empty description="暂无可用分类数据" />
            </Card>
          </Col>
        )}
      </Row>
    </Space>
  )
}
