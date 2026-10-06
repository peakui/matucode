import { Card, Empty, Space, Spin, Table, Tag, Tooltip } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useEffect, useState } from 'react'
import { getOjClassRanking } from '../../api/oj'
import type { OjClassRankingCell, OjClassRankingRow, OjClassRankingVO } from '../../api/type/ojTypings'
import './index.scss'

interface ClassRankingPanelProps {
  classId?: string
}

const formatDateTime = (value?: string | null) => {
  if (!value) return '未设置'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

const formatPenalty = (minutes?: number) => {
  const value = minutes ?? 0
  const hours = Math.floor(value / 60)
  const rest = value % 60
  return hours > 0 ? `${hours}h${String(rest).padStart(2, '0')}m` : `${rest}m`
}

const problemLabel = (index?: number) => {
  if (!index || index < 1) return '?'
  if (index <= 26) return String.fromCharCode(64 + index)
  return `P${index}`
}

const renderCell = (cell?: OjClassRankingCell) => {
  if (!cell || (!cell.solved && !cell.wrongAttempts)) {
    return <span className="class-ranking-cell class-ranking-cell--empty">—</span>
  }
  if (cell.solved) {
    return (
      <span className="class-ranking-cell class-ranking-cell--solved">
        +{cell.wrongAttempts ?? 0}
        <small>{cell.acMinutes ?? 0}′</small>
      </span>
    )
  }
  return <span className="class-ranking-cell class-ranking-cell--failed">-{cell.wrongAttempts}</span>
}

export function ClassRankingPanel({ classId }: ClassRankingPanelProps) {
  const [ranking, setRanking] = useState<OjClassRankingVO>()
  const [loading, setLoading] = useState(false)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    const id = classId?.trim()
    if (!id) {
      setRanking(undefined)
      setFailed(false)
      return
    }

    let cancelled = false
    const load = async () => {
      try {
        setLoading(true)
        setFailed(false)
        const data = await getOjClassRanking(id)
        if (!cancelled) {
          setRanking(data)
        }
      } catch (error) {
        console.error('load class ranking error:', error)
        if (!cancelled) {
          setRanking(undefined)
          setFailed(true)
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    void load()
    return () => {
      cancelled = true
    }
  }, [classId])

  if (loading) {
    return <div className="class-ranking-loading"><Spin /></div>
  }

  if (failed) {
    return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="排行榜加载失败，请稍后重试" />
  }

  const problems = ranking?.problems ?? []
  const rows = ranking?.rows ?? []

  const columns: ColumnsType<OjClassRankingRow> = [
    { title: '排名', dataIndex: 'rank', width: 72, render: (value?: number) => value ?? '-' },
    {
      title: '选手',
      dataIndex: 'userName',
      render: (value: string | undefined, row) => (
        <div className="class-ranking-user">
          <span className="class-ranking-user__name">{value || `用户 ${String(row.userId ?? '')}`}</span>
        </div>
      ),
    },
    {
      title: '通过',
      dataIndex: 'solvedCount',
      width: 88,
      render: (value?: number) => <Tag color="green">{value ?? 0}</Tag>,
    },
    {
      title: '罚时',
      dataIndex: 'penaltyMinutes',
      width: 100,
      render: (value?: number) => formatPenalty(value),
    },
    ...problems.map((problem) => ({
      title: (
        <Tooltip title={`${problem.problemNo ? `${problem.problemNo} ` : ''}${problem.title || ''}`}>
          <span>{problemLabel(problem.index)}</span>
        </Tooltip>
      ),
      key: `problem-${String(problem.problemId)}`,
      width: 104,
      align: 'center' as const,
      render: (_: unknown, row: OjClassRankingRow) => {
        const cell = row.cells?.find((item) => String(item.problemId) === String(problem.problemId))
        return renderCell(cell)
      },
    })),
  ]

  return (
    <div className="class-ranking-panel">
      <div className="class-ranking-panel__header">
        <div>
          <div className="channel-hero__label">ACM 罚时榜</div>
          <h3>按通过题数与罚时排名</h3>
        </div>
        <Space wrap size={6}>
          <Tag>成员 {ranking?.memberCount ?? 0}</Tag>
          <Tag color="blue">开始 {formatDateTime(ranking?.startTime)}</Tag>
          <Tag color="orange">结束 {formatDateTime(ranking?.endTime)}</Tag>
        </Space>
      </div>
      {rows.length ? (
        <Table<OjClassRankingRow>
          rowKey={(row) => String(row.userId ?? row.rank)}
          dataSource={rows}
          columns={columns}
          pagination={false}
          scroll={{ x: 'max-content' }}
        />
      ) : (
        <Card variant="borderless" className="content-card">
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无排名数据" />
        </Card>
      )}
      <div className="class-ranking-panel__tip">
        罚时 = 首次通过用时（分钟）+ 20 × 通过前的错误提交次数；用时以竞赛开始时间为基准。
      </div>
    </div>
  )
}

export default ClassRankingPanel
