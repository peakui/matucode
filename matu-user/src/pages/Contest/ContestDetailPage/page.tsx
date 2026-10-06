import { ArrowLeftOutlined, FireOutlined, TrophyOutlined } from '@ant-design/icons'
import { Button, Card, Empty, Space, Spin, Table, Tabs, Tag } from 'antd'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getOjClassDetail, listOjAssignments, listOjClassProblems, listOjSubmissions } from '../../../api/oj'
import type { ClassAssignmentVO, ClassVO, OjProblemVO, OjSubmissionVO } from '../../../api/type/ojTypings'
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

const getContestStatusText = (status?: number) => {
  if (status === 0) return '未开始'
  if (status === 2) return '已结束'
  return '进行中'
}

const judgeStatusMap: Record<number, string> = {
  0: '等待判题',
  1: '答案正确',
  2: '答案错误',
  3: '运行错误',
  4: '超出时间限制',
  5: '超出内存限制',
  6: '编译错误',
  7: '系统错误',
}

const normalizeId = (value?: string | number) => String(value ?? '').trim()
const getJudgeStatusColor = (status?: number) => (status === 1 ? 'green' : status === 0 ? 'blue' : 'red')
const getJudgeStatusText = (status?: number) => judgeStatusMap[status ?? -1] || '未知结果'

const formatContestDateTime = (value?: string) => {
  if (!value) return '暂无'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

const getSubmissionUserName = (submission: OjSubmissionVO) => (
  submission.nickname?.trim()
    || submission.userName?.trim()
    || submission.username?.trim()
    || (submission.userId != null ? `用户 ${String(submission.userId)}` : '未知用户')
)

type ContestSubmissionRecord = {
  key: string
  problemLabel: string
  userName: string
  status?: number
  createdAt?: string
}

export function ContestDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [group, setGroup] = useState<ClassVO>()
  const [assignments, setAssignments] = useState<ClassAssignmentVO[]>([])
  const [problems, setProblems] = useState<OjProblemVO[]>([])
  const [submissions, setSubmissions] = useState<OjSubmissionVO[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const classId = id?.trim()
    if (!classId) {
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

        const contestProblems = problemPage.records || []
        const submissionPages = await Promise.all(contestProblems.map((problem) => (
          listOjSubmissions({ problemId: normalizeId(problem.id), pageNum: 1, pageSize: 200 })
        )))

        setProblems(contestProblems)
        setSubmissions(submissionPages.flatMap((page) => page.records || []))
      } catch (error) {
        console.error('load contest detail error:', error)
      } finally {
        setLoading(false)
      }
    }

    void loadData()
  }, [id])

  const problemLabelMap = useMemo(() => {
    const map = new Map<string, string>()
    problems.forEach((problem, index) => {
      const problemId = normalizeId(problem.id)
      if (!problemId) {
        return
      }

      map.set(problemId, problem.problemNo || problem.title || `第 ${index + 1} 题`)
    })
    return map
  }, [problems])

  const submissionRecords = useMemo<ContestSubmissionRecord[]>(() => submissions
    .map((submission) => {
      const problemId = normalizeId(submission.problemId)
      return {
        key: normalizeId(submission.id) || `${problemId}-${normalizeId(submission.userId)}-${submission.createdAt || ''}`,
        problemLabel: problemLabelMap.get(problemId) || submission.problemNo || submission.problemTitle || (problemId ? `题目 ${problemId}` : '未知题目'),
        userName: getSubmissionUserName(submission),
        status: submission.status,
        createdAt: submission.createdAt,
      }
    })
    .sort((a, b) => new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime()), [problemLabelMap, submissions])

  if (loading) {
    return <Card className="content-card" variant="borderless"><div className="check-in-loading"><Spin /></div></Card>
  }

  if (!group?.id) {
    return <Card className="content-card" variant="borderless"><Empty description="未找到对应竞赛" /></Card>
  }

  return (
    <Space direction="vertical" size={18} className="full-width class-detail-page">
      <Card className="content-card" variant="borderless">
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/contests')}>
          返回竞赛列表
        </Button>
        <div className="practice-list-hero">
          <div>
            <div className="channel-hero__label">竞赛题单</div>
            <h2>{group.name || '未命名竞赛'}</h2>
            <p>{group.description || '暂无竞赛描述'}</p>
            <Space wrap>
              <Tag color="red" variant="filled">竞赛</Tag>
              <Tag color={(group.status ?? 1) === 1 ? 'green' : (group.status ?? 1) === 0 ? 'blue' : 'default'}>{getContestStatusText(group.status)}</Tag>
              <Tag>参与 {group.memberCount ?? 0}</Tag>
              <Tag>作业 {assignments.length}</Tag>
            </Space>
          </div>
          <div className="practice-list-badge">
            <TrophyOutlined /> <FireOutlined /> 本场开放 {problems.length} 道竞赛题
          </div>
        </div>
      </Card>

      <Card className="content-card contest-tabs-card" variant="borderless">
        <Tabs
          defaultActiveKey="problems"
          items={[
            {
              key: 'problems',
              label: `题目信息 (${problems.length})`,
              children: (
                <div className="contest-problem-list">
                  {problems.map((problem, index) => (
                    <Card key={problem.id} className="practice-list-card" variant="borderless">
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
                  {!problems.length ? <Empty description="当前竞赛暂无可做题目" /> : null}
                </div>
              ),
            },
            {
              key: 'ranking',
              label: '排名',
              children: <ClassRankingPanel classId={id} />,
            },
            {
              key: 'submissions',
              label: `状态 (${submissionRecords.length})`,
              children: (
                <div className="contest-tab-panel">
                  <div className="contest-section-header">
                    <div>
                      <div className="channel-hero__label">提交状态</div>
                      <h3>本场竞赛提交记录</h3>
                    </div>
                  </div>
                  <Table<ContestSubmissionRecord>
                    rowKey="key"
                    dataSource={submissionRecords}
                    pagination={{ pageSize: 10 }}
                    locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无提交记录" /> }}
                    columns={[
                      { title: '题号', dataIndex: 'problemLabel' },
                      { title: '用户名称', dataIndex: 'userName' },
                      {
                        title: '结果是否正确',
                        dataIndex: 'status',
                        width: 140,
                        render: (value?: number) => <Tag color={getJudgeStatusColor(value)}>{value === 1 ? '正确' : getJudgeStatusText(value)}</Tag>,
                      },
                      { title: '提交时间', dataIndex: 'createdAt', width: 180, render: (value?: string) => formatContestDateTime(value) },
                    ]}
                  />
                </div>
              ),
            },
          ]}
        />
      </Card>
    </Space>
  )
}
