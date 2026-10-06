import { ArrowLeftOutlined, CheckCircleOutlined, TrophyOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Empty, Space, Spin, Table, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getOjAssignmentDetail, getOjAssignmentRanking } from '../../../api/oj'
import type { AssignmentDetailVO, AssignmentRankingRow } from '../../../api/type/ojTypings'
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

const statusTextMap: Record<number, string> = {
  0: '未开始',
  1: '进行中',
  2: '已结束',
  3: '已批改',
}

export function AssignmentDetailPage() {
  const { id, assignmentId } = useParams()
  const navigate = useNavigate()
  const classId = id?.trim()
  const aid = assignmentId?.trim()
  const [detail, setDetail] = useState<AssignmentDetailVO>()
  const [ranking, setRanking] = useState<AssignmentRankingRow[]>([])
  const [rankingNotice, setRankingNotice] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!classId || !aid || !/^[1-9]\d*$/.test(classId) || !/^[1-9]\d*$/.test(aid)) {
      setLoading(false)
      return
    }

    const loadData = async () => {
      try {
        setLoading(true)
        const detailData = await getOjAssignmentDetail(classId, aid)
        setDetail(detailData)

        try {
          const rankingData = await getOjAssignmentRanking(classId, aid)
          setRanking(rankingData.rows || [])
          setRankingNotice('')
        } catch (error) {
          console.error('load assignment ranking error:', error)
          setRanking([])
          setRankingNotice(detailData?.isPublicRank === 1 ? '排行榜暂时无法加载' : '该作业排行榜未公开')
        }
      } catch (error) {
        console.error('load assignment detail error:', error)
      } finally {
        setLoading(false)
      }
    }

    void loadData()
  }, [classId, aid])

  if (loading) {
    return <Card className="content-card" variant="borderless"><div className="check-in-loading"><Spin /></div></Card>
  }

  if (!detail?.id) {
    return <Card className="content-card" variant="borderless"><Empty description="未找到对应作业" /></Card>
  }

  const problems = detail.problems || []
  const solvedCount = problems.filter((item) => item.solved).length

  const rankingColumns: ColumnsType<AssignmentRankingRow> = [
    {
      title: '名次',
      dataIndex: 'rank',
      width: 80,
      render: (rank: number) => (rank <= 3 ? <Tag color="gold" variant="filled">{rank}</Tag> : rank),
    },
    { title: '昵称', dataIndex: 'nickname', render: (value?: string | null, row?: AssignmentRankingRow) => value || `用户 ${row?.userId ?? ''}` },
    { title: '通过题数', dataIndex: 'solvedCount' },
    { title: '得分', dataIndex: 'score' },
    {
      title: '最近通过',
      dataIndex: 'lastSubmitAt',
      render: (value?: string | null) => (value ? value.replace('T', ' ') : '-'),
    },
  ]

  return (
    <Space direction="vertical" size={18} className="full-width assignment-detail-page">
      <Card className="content-card" variant="borderless">
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate(`/classes/${String(classId)}`)}>
          返回班级
        </Button>
        <div className="practice-list-hero">
          <div>
            <div className="channel-hero__label">班级作业</div>
            <h2>{detail.title || '未命名作业'}</h2>
            <p>{detail.description || '暂无作业描述'}</p>
            <Space wrap>
              <Tag color={detail.status === 1 ? 'green' : 'default'} variant="filled">
                {detail.status != null ? statusTextMap[detail.status] || '未知' : '未知'}
              </Tag>
              <Tag>题量 {problems.length}</Tag>
              <Tag color="blue" variant="filled">已通过 {solvedCount}</Tag>
              {detail.maxAttempts && detail.maxAttempts > 0
                ? <Tag>每题限 {detail.maxAttempts} 次</Tag>
                : <Tag>不限次数</Tag>}
              <Tag>{detail.isPublicRank === 1 ? '榜单公开' : '榜单不公开'}</Tag>
              {detail.startTime ? <Tag>开始 {detail.startTime.replace('T', ' ')}</Tag> : null}
              {detail.deadline ? <Tag color="orange">截止 {detail.deadline.replace('T', ' ')}</Tag> : null}
            </Space>
          </div>
          <div className="practice-list-badge">
            <TrophyOutlined /> 已通过 {solvedCount} / {problems.length}
          </div>
        </div>
        {detail.submittable === false ? (
          <Alert
            style={{ marginTop: 16 }}
            type="warning"
            showIcon
            message="当前作业不在可提交时间窗内，暂不能提交代码"
          />
        ) : null}
      </Card>

      <Card className="content-card" variant="borderless">
        <h3 className="assignment-section-title">作业题目</h3>
        <Space direction="vertical" size={12} className="full-width">
          {problems.map((problem, index) => {
            const attemptsLeft = detail.maxAttempts && detail.maxAttempts > 0
              ? Math.max(detail.maxAttempts - (problem.attemptsUsed ?? 0), 0)
              : null
            return (
              <div key={String(problem.problemId)} className="assignment-problem-row">
                <div className="assignment-problem-row__content">
                  <div className="assignment-problem-row__title-wrap">
                    <span className="assignment-problem-row__index">#{index + 1}</span>
                    <h4>{problem.title || '未命名题目'}</h4>
                    {problem.solved ? <Tag icon={<CheckCircleOutlined />} color="green" variant="filled">已通过</Tag> : null}
                  </div>
                  <Space wrap size={6}>
                    {problem.difficulty ? (
                      <Tag color={difficultyColorMap[problem.difficulty] || 'default'} variant="filled">
                        {difficultyTextMap[problem.difficulty] || '未知'}
                      </Tag>
                    ) : null}
                    <Tag>已提交 {problem.attemptsUsed ?? 0} 次</Tag>
                    {attemptsLeft !== null ? <Tag color={attemptsLeft > 0 ? 'blue' : 'red'}>剩余 {attemptsLeft} 次</Tag> : null}
                  </Space>
                </div>
                <Button
                  type={problem.solved ? 'default' : 'primary'}
                  disabled={detail.submittable === false || attemptsLeft === 0}
                  onClick={() => navigate(`/problem/${String(problem.problemId)}?assignmentId=${String(aid)}&classId=${String(classId)}`)}
                >
                  {problem.solved ? '再次提交' : '开始做题'}
                </Button>
              </div>
            )
          })}
          {!problems.length ? <Empty description="本次作业暂未配置题目" /> : null}
        </Space>
      </Card>

      <Card className="content-card" variant="borderless">
        <h3 className="assignment-section-title"><TrophyOutlined /> 排行榜</h3>
        {rankingNotice ? (
          <Alert type="info" showIcon message={rankingNotice} />
        ) : (
          <Table
            rowKey={(row) => String(row.userId)}
            size="middle"
            columns={rankingColumns}
            dataSource={ranking}
            pagination={false}
            locale={{ emptyText: <Empty description="暂无提交记录" /> }}
          />
        )}
      </Card>
    </Space>
  )
}
