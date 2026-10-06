import { Button, Card, Col, Row, Space, Tag } from 'antd'
import { TeamOutlined, TrophyOutlined } from '@ant-design/icons'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { listOjProblems } from '../../../api/oj'
import type { OjProblemVO } from '../../../api/type/ojTypings'
import './page.scss'

type DynamicPracticeSection = {
  id: string
  title: string
  description: string
  items: OjProblemVO[]
}

const isSqlProblem = (problem: OjProblemVO) => {
  const text = [problem.title, problem.problemNo, problem.description, ...(problem.tags || [])]
    .filter(Boolean)
    .join(' ')
    .toLowerCase()

  return text.includes('sql') || text.includes('mysql') || text.includes('数据库')
}

const buildPracticeSections = (problems: OjProblemVO[]): DynamicPracticeSection[] => {
  const sqlProblems = problems.filter(isSqlProblem)
  const nonSqlProblems = problems.filter((item) => !isSqlProblem(item))
  const basicProblems = nonSqlProblems.filter((item) => item.difficulty === 1 || (item.difficulty !== 2 && item.difficulty !== 3))
  const algorithmProblems = nonSqlProblems.filter((item) => item.difficulty === 2 || item.difficulty === 3)

  return [
    {
      id: 'basic',
      title: '编程语言基础专区',
      description: '消除新手畏难情绪，强调代码习惯与正反馈，避免过早引入竞技压力。',
      items: basicProblems,
    },
    {
      id: 'algorithm',
      title: '算法刷题专区',
      description: '弱化“刷题量”焦虑，强化“思维训练”价值，引导用户关注解题思路而非单纯AC。',
      items: algorithmProblems,
    },
    {
      id: 'sql',
      title: 'SQL 刷题专区',
      description: '将枯燥的SQL语法转化为“解决业务问题”的闯关体验，强调实用性与即时反馈。',
      items: sqlProblems,
    },
  ]
}

export function PracticePage() {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(true)
  const [problems, setProblems] = useState<OjProblemVO[]>([])

  useEffect(() => {
    const loadProblems = async () => {
      try {
        setLoading(true)
        const data = await listOjProblems({ pageNum: 1, pageSize: 100 })
        setProblems(data.records || [])
      } catch (error) {
        console.error('load practice homepage problems error:', error)
        setProblems([])
      } finally {
        setLoading(false)
      }
    }

    void loadProblems()
  }, [])

  const practiceSections = useMemo(() => buildPracticeSections(problems), [problems])

  return (
    <Space orientation="vertical" size={20} className="full-width practice-page">
      <div className="channel-hero card-surface practice-hero">
        <div>
          <div className="channel-hero__label">OJ 刷题中心</div>
          <h2>分区刷题，按专题突破</h2>
          <p>零基础友好！从Hello World到第一道A+B，平滑曲线带你无痛入门算法世界。</p>
        </div>
      </div>

      <Row gutter={[20, 20]}>
        <Col xs={24} lg={12}>
          <Card className="practice-card" variant="borderless">
            <div className="practice-card__content">
              <Tag color="red" variant="filled">
                <TrophyOutlined /> 竞赛模块
              </Tag>
              <h3>竞赛挑战</h3>
              <p>新手友好型月赛来袭！题目梯度平缓，首杀奖励翻倍，你的第一场正式竞赛从这里开始。</p>
              <ul className="bullet-list">
                <li>查看进行中 / 未开始 / 已结束竞赛</li>
                <li>加入竞赛后进入专属题单</li>
                <li>在竞赛页面中完成 OJ 提交</li>
              </ul>
            </div>
            <Button type="primary" block className="practice-card__action" onClick={() => navigate('/contests')}>
              进入竞赛页面
            </Button>
          </Card>
        </Col>

        <Col xs={24} lg={12}>
          <Card className="practice-card" variant="borderless">
            <div className="practice-card__content">
              <Tag color="blue" variant="filled">
                <TeamOutlined /> 班级模块
              </Tag>
              <h3>班级训练</h3>
              <p>从“催促”到“引导”、从“评判”到“看见”、从“孤立”到“联结”，我们的成长从这里开始。</p>
              <ul className="bullet-list">
                <li>选择适合自己的训练班级</li>
                <li>加入班级后查看作业题单</li>
                <li>进入题目页完成代码提交</li>
              </ul>
            </div>
            <Button type="primary" block className="practice-card__action" onClick={() => navigate('/classes')}>
              进入班级页面
            </Button>
          </Card>
        </Col>
      </Row>

      <Row gutter={[20, 20]}>
        {practiceSections.map((section, index) => (
          <Col xs={24} lg={8} key={section.id}>
            <Card className="practice-card" variant="borderless">
              <div className="practice-card__content">
                <Tag color={index === 0 ? 'purple' : index === 1 ? 'blue' : 'cyan'} variant="filled">
                  { index === 0 ? '基础编程' : index === 1 ? '算法' : 'SQL'  }
                </Tag>
                <h3>{section.title}</h3>
                <p>{section.description}</p>
                <ul className="bullet-list">
                  {loading ? (
                    <li>正在加载题目...</li>
                  ) : section.items.length ? (
                    section.items.slice(0, 3).map((item) => (
                      <li key={item.id}>{item.title || `题目 #${item.id ?? '-'}`}</li>
                    ))
                  ) : (
                    <li>当前暂无题目，点击查看详情后可查看空状态页</li>
                  )}
                </ul>
              </div>
              <Button type="primary" block className="practice-card__action" onClick={() => navigate(`/practice/${section.id}`)}>
                查看题目列表
              </Button>
            </Card>
          </Col>
        ))}
      </Row>
    </Space>
  )
}
