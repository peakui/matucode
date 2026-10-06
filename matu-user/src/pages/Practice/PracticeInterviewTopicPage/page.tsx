import { BookOutlined, FireOutlined, ShareAltOutlined, StarOutlined } from '@ant-design/icons'
import { Button, Card, Input, Select, Space, Table, Tag } from 'antd'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { listInterviewCategories, listInterviewQuestions } from '../../../api/interview'
import type { InterviewCategoryVO, InterviewQuestionVO } from '../../../api/type/interviewTypings'
import { buildSearchPath, SEARCH_KEYWORD_LIMIT } from '../../../utils/search'
import './page.scss'

type InterviewQuestionItem = InterviewQuestionVO & { key: number }

const difficultyColorMap: Record<number, string> = {
  1: 'green',
  2: 'gold',
  3: 'red',
}

const difficultyTextMap: Record<number, string> = {
  1: '简单',
  2: '中等',
  3: '困难',
}

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

export function PracticeInterviewTopicPage() {
  const { sectionId } = useParams()
  const navigate = useNavigate()
  const [questions, setQuestions] = useState<InterviewQuestionVO[]>([])
  const [categories, setCategories] = useState<InterviewCategoryVO[]>([])
  const [keyword, setKeyword] = useState('')
  const [difficultyFilter, setDifficultyFilter] = useState('all')

  const categoryId = useMemo(() => {
    const parsed = Number(sectionId)
    return Number.isInteger(parsed) && parsed > 0 ? parsed : undefined
  }, [sectionId])

  useEffect(() => {
    const loadCategories = async () => {
      try {
        const categoryList = await listInterviewCategories()
        setCategories(categoryList)
      } catch (error) {
        console.error('load interview topic categories error:', error)
        setCategories([])
      }
    }

    void loadCategories()
  }, [])

  useEffect(() => {
    const loadData = async () => {
      if (!categoryId) {
        setQuestions([])
        return
      }

      try {
        const questionPage = await listInterviewQuestions({ categoryId, pageNum: 1, pageSize: 10 })
        setQuestions(questionPage.records || [])
      } catch (error) {
        console.error('load interview topic page data error:', error)
        setQuestions([])
      }
    }

    void loadData()
  }, [categoryId])

  const flatCategories = useMemo(() => flattenCategories(categories), [categories])
  const currentCategory = useMemo(() => flatCategories.find((item) => item.id === categoryId), [categoryId, flatCategories])

  const filteredQuestions = useMemo(() => questions.filter((question) => {
    return difficultyFilter === 'all' || String(question.difficulty || '') === difficultyFilter
  }), [difficultyFilter, questions])

  const topicHints = useMemo(() => flatCategories.slice(0, 5), [flatCategories])

  const tableData = useMemo<InterviewQuestionItem[]>(() => filteredQuestions.map((item, index) => ({ ...item, key: item.id ?? index })), [filteredQuestions])

  const columns = [
    {
      title: '题目',
      dataIndex: 'title',
      key: 'title',
      render: (_: string, record: InterviewQuestionItem) => (
        <button type="button" className="practice-interview-topic__question-btn" onClick={() => navigate(`/detail/interview-question/${record.id}`)}>
          {record.title}
        </button>
      ),
    },
    {
      title: '难度',
      dataIndex: 'difficulty',
      key: 'difficulty',
      width: 120,
      render: (value: number) => <Tag color={difficultyColorMap[value] || 'default'}>{difficultyTextMap[value] || '未知'}</Tag>,
    },
    {
      title: '标签',
      dataIndex: 'positionTags',
      key: 'positionTags',
      width: 220,
      render: (tags: string[] | undefined) => (
        <Space wrap>
          {(tags || []).length ? (tags || []).map((tag) => <Tag key={tag}>{tag}</Tag>) : <Tag>{'通用'}</Tag>}
        </Space>
      ),
    },
  ]

  if (!categoryId || !currentCategory) {
    return <Card className="content-card" variant="borderless">未找到对应面试题专题。</Card>
  }

  return (
    <Space orientation="vertical" size={18} className="full-width practice-interview-topic-page">
      <Card className="content-card practice-interview-topic__hero" variant="borderless">
        <div className="practice-interview-topic__hero-main">
          <div className="practice-interview-topic__logo">{currentCategory.iconUrl ? <img src={currentCategory.iconUrl} alt={currentCategory.categoryName} /> : '📘'}</div>
          <div className="practice-interview-topic__hero-copy">
            <div className="channel-hero__label">面试题专题</div>
            <h1>{currentCategory.categoryName}</h1>
            <p>{currentCategory.categoryDesc || '围绕该分类下的高频面试题进行系统练习与复习。'}</p>
            <Space wrap>
              <Button onClick={() => navigate('/interviews')}>返回面试题首页</Button>
              <Button type="primary" onClick={() => navigate(`/detail/interview-question/${filteredQuestions[0]?.id ?? 1}`)}>继续练习</Button>
              <Button icon={<BookOutlined />}>在线题解</Button>
              <Button icon={<FireOutlined />}>专项练习</Button>
              <Button icon={<ShareAltOutlined />}>分享</Button>
            </Space>
          </div>
        </div>
      </Card>

      <Card className="content-card" variant="borderless">
        <div className="practice-interview-topic__toolbar">
          <div className="practice-interview-topic__toolbar-title">题目列表</div>
          <Space wrap>
            <Input.Search
              placeholder="搜索题目"
              style={{ width: 220 }}
              value={keyword}
              maxLength={SEARCH_KEYWORD_LIMIT}
              onChange={(event) => setKeyword(event.target.value)}
              onSearch={(value) => {
                if (value.trim()) navigate(buildSearchPath(value, 'interview'))
              }}
            />
            <Select defaultValue="all" value={difficultyFilter} onChange={setDifficultyFilter} options={[{ label: '全部', value: 'all' }, { label: '简单', value: '1' }, { label: '中等', value: '2' }, { label: '困难', value: '3' }]} style={{ width: 120 }} />
            <Select defaultValue="newest" options={[{ label: '最新', value: 'newest' }, { label: '最热', value: 'hot' }]} style={{ width: 120 }} />
          </Space>
        </div>
        <Table columns={columns} dataSource={tableData} pagination={false} />
      </Card>

      <Card className="content-card" variant="borderless">
        <div className="practice-interview-topic__toolbar-title">推荐专题</div>
        <Space wrap>
          {topicHints.map((topic) => (
            <Tag key={topic.id} color="blue" variant="filled" icon={<StarOutlined />}>
              {topic.categoryName}
            </Tag>
          ))}
        </Space>
      </Card>
    </Space>
  )
}
