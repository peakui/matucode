import { ArrowLeftOutlined, FireOutlined } from '@ant-design/icons'
import { Button, Card, Empty, Space, Spin, Tabs, Tag } from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getOjClassDetail, listOjAssignments, listOjClassProblems } from '../../../api/oj'
import type { ClassAssignmentVO, ClassVO, OjProblemVO } from '../../../api/type/ojTypings'
import { ClassRankingPanel } from '../../../components/ClassRankingPanel/index'
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

export function ClassDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [group, setGroup] = useState<ClassVO>()
  const [assignments, setAssignments] = useState<ClassAssignmentVO[]>([])
  const [problems, setProblems] = useState<OjProblemVO[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    // Class ids are snowflake values above Number.MAX_SAFE_INTEGER; Number(id)
    // would round to a neighbouring id and every request below would fail.
    const classId = id?.trim()
    if (!classId || !/^[1-9]\d*$/.test(classId)) {
      setLoading(false)
      return
    }

    const loadData = async () => {
      try {
        setLoading(true)
        const [detail, assignmentList, problemPage] = await Promise.all([
          getOjClassDetail(classId),
          listOjAssignments(classId),
          listOjClassProblems(classId, { status: 1, pageNum: 1, pageSize: 100 }),
        ])

        setGroup(detail)
        setAssignments(assignmentList || [])
        setProblems(problemPage.records || [])
      } catch (error) {
        console.error('load class detail error:', error)
      } finally {
        setLoading(false)
      }
    }

    void loadData()
  }, [id])

  if (loading) {
    return <Card className="content-card" variant="borderless"><div className="check-in-loading"><Spin /></div></Card>
  }

  if (!group?.id) {
    return <Card className="content-card" variant="borderless"><Empty description="未找到对应班级" /></Card>
  }

  return (
    <Space direction="vertical" size={18} className="full-width class-detail-page">
      <Card className="content-card" variant="borderless">
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/classes')}>
          返回班级列表
        </Button>
        <div className="practice-list-hero">
          <div>
            <div className="channel-hero__label">班级题单</div>
            <h2>{group.name || '未命名班级'}</h2>
            <p>{group.description || '暂无班级描述'}</p>
            <Space wrap>
              <Tag color="blue" variant="filled">普通班级</Tag>
              <Tag>成员 {group.memberCount ?? 0}</Tag>
              <Tag>作业 {assignments.length}</Tag>
            </Space>
          </div>
          <div className="practice-list-badge">
            <FireOutlined /> 当前开放 {problems.length} 道练习题
          </div>
        </div>
      </Card>

      <Card className="content-card" variant="borderless">
        <h3 className="class-section-title">作业列表</h3>
        {assignments.length ? (
          <div className="class-assignment-list">
            {assignments.map((assignment) => (
              <div key={assignment.id} className="class-assignment-row">
                <div className="class-assignment-row__content">
                  <div className="class-assignment-row__title">
                    {assignment.title || '未命名作业'}
                  </div>
                  <Space wrap size={6}>
                    <Tag color={assignment.status === 1 ? 'green' : 'default'} variant="filled">
                      {assignment.status === 1 ? '进行中' : assignment.status === 0 ? '未开始' : '已结束'}
                    </Tag>
                    <Tag>题量 {assignment.problemIds?.length ?? 0}</Tag>
                    {assignment.maxAttempts && assignment.maxAttempts > 0
                      ? <Tag>每题限 {assignment.maxAttempts} 次</Tag>
                      : <Tag>不限次数</Tag>}
                    <Tag>{assignment.isPublicRank === 1 ? '榜单公开' : '榜单不公开'}</Tag>
                    {assignment.deadline ? <Tag color="orange">截止 {assignment.deadline.replace('T', ' ')}</Tag> : null}
                  </Space>
                </div>
                <Button
                  type="primary"
                  onClick={() => navigate(`/classes/${String(id)}/assignments/${String(assignment.id)}`)}
                >
                  查看作业
                </Button>
              </div>
            ))}
          </div>
        ) : (
          <Empty description="当前班级暂无作业" />
        )}
      </Card>

      <Card className="content-card class-tabs-card" variant="borderless">
        <Tabs
          defaultActiveKey="problems"
          items={[
            {
              key: 'problems',
              label: `题目信息 (${problems.length})`,
              children: (
                <div className="class-problem-list">
                  {problems.map((problem, index) => (
                    <Card key={problem.id} className="content-card practice-list-card" variant="borderless">
                      <div className="practice-list-card__row">
                        <div className="practice-list-card__content">
                          <div className="practice-list-card__title-wrap">
                            <span className="practice-list-card__index">#{index + 1}</span>
                            <h3>{problem.title || '未命名题目'}</h3>
                          </div>
                          <Space wrap>
                            <Tag color="blue" variant="filled">OJ 题目</Tag>
                            {problem.difficulty ? <Tag color={difficultyColorMap[problem.difficulty] || 'default'} variant="filled">{difficultyTextMap[problem.difficulty] || '未知'}</Tag> : null}
                          </Space>
                          <p className="practice-list-card__desc">{problem.description || group.description || '暂无题目描述'}</p>
                        </div>
                        <Button type="primary" className="practice-list-card__action" onClick={() => navigate(`/problem/${problem.id}`)}>
                          开始做题
                        </Button>
                      </div>
                    </Card>
                  ))}
                  {!problems.length ? <Empty description="当前班级暂无可做题目" /> : null}
                </div>
              ),
            },
            {
              key: 'ranking',
              label: '排名',
              children: <ClassRankingPanel classId={id} />,
            },
          ]}
        />
      </Card>
    </Space>
  )
}
