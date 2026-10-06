import { ArrowLeftOutlined, FireOutlined } from '@ant-design/icons'
import { Button, Card, Empty, Input, Space, Spin, Tag } from 'antd'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { listOjProblems } from '../../../api/oj'
import type { OjProblemVO } from '../../../api/type/ojTypings'
import { buildSearchPath, SEARCH_KEYWORD_LIMIT } from '../../../utils/search'
import './page.scss'

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

const isSqlProblem = (problem: OjProblemVO) => {
  const text = [problem.title, problem.problemNo, problem.description, ...(problem.tags || [])]
    .filter(Boolean)
    .join(' ')
    .toLowerCase()

  return text.includes('sql') || text.includes('mysql') || text.includes('数据库')
}

const sectionConfig: Record<string, { title: string, description: string }> = {
  basic: {
    title: '编程语言基础专区',
    description: '消除新手畏难情绪，强调代码习惯与正反馈，避免过早引入竞技压力。',
  },
  algorithm: {
    title: '算法刷题专区',
    description: '弱化“刷题量”焦虑，强化“思维训练”价值，引导用户关注解题思路而非单纯AC。',
  },
  sql: {
    title: 'SQL 刷题专区',
    description: '将枯燥的SQL语法转化为“解决业务问题”的闯关体验，强调实用性与即时反馈。',
  },
}

export function PracticeListPage() {
  const { sectionId } = useParams()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(true)
  const [sectionProblems, setSectionProblems] = useState<OjProblemVO[]>([])
  const [keyword, setKeyword] = useState('')

  const section = useMemo(() => {
    if (!sectionId) {
      return undefined
    }

    return sectionConfig[sectionId]
  }, [sectionId])

  const filteredProblems = useMemo(() => sectionProblems, [sectionProblems])

  useEffect(() => {
    if (!sectionId || !section) {
      setLoading(false)
      setSectionProblems([])
      return
    }

    const loadProblems = async () => {
      try {
        setLoading(true)
        const data = await listOjProblems({ pageNum: 1, pageSize: 100 })
        const records = data.records || []

        const filtered = sectionId === 'basic'
          ? records.filter((problem) => !isSqlProblem(problem) && (problem.difficulty === 1 || (problem.difficulty !== 2 && problem.difficulty !== 3)))
          : sectionId === 'algorithm'
            ? records.filter((problem) => !isSqlProblem(problem) && (problem.difficulty === 2 || problem.difficulty === 3))
            : records.filter(isSqlProblem)

        setSectionProblems(filtered)
      } catch (error) {
        console.error('load practice problems error:', error)
        setSectionProblems([])
      } finally {
        setLoading(false)
      }
    }

    void loadProblems()
  }, [section, sectionId])

  if (!section) {
    return <Card className="content-card" variant="borderless">未找到对应题库。</Card>
  }

  return (
    <Space orientation="vertical" size={18} className="full-width practice-list-page">
      <Card className="content-card" variant="borderless">
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/practice')}>
          返回题库首页
        </Button>
        <div className="practice-list-hero">
          <div>
            <div className="channel-hero__label">题目列表</div>
            <h2>{section.title}</h2>
            <p>{section.description}</p>
          </div>
          <div className="practice-list-badge">
            <FireOutlined /> 共 {filteredProblems.length} 道可练习题目
          </div>
        </div>
      </Card>

      <Card className="content-card practice-list-search-card" variant="borderless">
        <Input.Search
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          maxLength={SEARCH_KEYWORD_LIMIT}
          allowClear
          enterButton="搜索"
          onSearch={(value) => {
            if (value.trim()) navigate(buildSearchPath(value, 'oj'))
          }}
          placeholder="输入题目 ID、题号、标题或标签搜索"
        />
      </Card>

      {loading ? (
        <Card className="content-card" variant="borderless">
          <div className="check-in-loading"><Spin /></div>
        </Card>
      ) : (
        <Space orientation="vertical" size={16} className="full-width">
          {filteredProblems.map((problem, index) => (
            <Card key={problem.id} className="content-card practice-list-card" variant="borderless">
              <div className="practice-list-card__row">
                <div className="practice-list-card__content">
                  <div className="practice-list-card__title-wrap">
                    <span className="practice-list-card__index">#{index + 1}</span>
                    <h3>{problem.title || `题目 #${problem.id ?? '-'}`}</h3>
                  </div>
                  <Space wrap>
                    <Tag color="blue" variant="filled">OJ 题目</Tag>
                    <Tag color={difficultyColorMap[problem.difficulty || 2] || 'default'} variant="filled">
                      {difficultyTextMap[problem.difficulty || 2] || '未知'}
                    </Tag>
                  </Space>
                  <p className="practice-list-card__desc">{problem.description || section.description}</p>
                </div>
                <Button type="primary" className="practice-list-card__action" onClick={() => navigate(`/problem/${problem.id}`)}>
                  开始练习
                </Button>
              </div>
            </Card>
          ))}

          {!filteredProblems.length ? (
            <Card className="content-card" variant="borderless">
              <Empty description={keyword.trim() ? '未搜索到匹配的题目' : '当前专区暂无可练习题目'} />
            </Card>
          ) : null}
        </Space>
      )}
    </Space>
  )
}
