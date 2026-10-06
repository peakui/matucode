import { Button, Card, Col, Empty, Input, Modal, Row, Space, Spin, Tag, message } from 'antd'
import { ArrowLeftOutlined, TrophyOutlined } from '@ant-design/icons'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { joinOjClass, listOjClasses } from '../../../api/oj'
import type { ClassVO } from '../../../api/type/ojTypings'
import './page.scss'

const getContestStatusText = (status?: number) => {
  if (status === 0) return '未开始'
  if (status === 2) return '已结束'
  return '进行中'
}

const normalizeSearchText = (value?: string | number) => String(value ?? '').trim().toLowerCase()

const matchGroupSearch = (group: ClassVO, keyword: string) => {
  const normalizedKeyword = normalizeSearchText(keyword)
  if (!normalizedKeyword) {
    return true
  }

  return [group.id, group.name].some((value) => normalizeSearchText(value).includes(normalizedKeyword))
}

export function ContestPage() {
  const navigate = useNavigate()
  const [groups, setGroups] = useState<ClassVO[]>([])
  const [loading, setLoading] = useState(true)
  const [joiningId, setJoiningId] = useState<string | undefined>(undefined)
  const [joinModalOpen, setJoinModalOpen] = useState(false)
  const [pendingJoinContest, setPendingJoinContest] = useState<ClassVO | null>(null)
  const [joinReason, setJoinReason] = useState('')
  const [keyword, setKeyword] = useState('')
  const [pendingJoinIds, setPendingJoinIds] = useState<Set<string>>(() => new Set())

  useEffect(() => {
    const loadData = async () => {
      try {
        setLoading(true)
        const data = await listOjClasses({ status: 1, pageNum: 1, pageSize: 50 })
        setGroups((data.records || []).filter((item) => item.type === 2))
      } catch (error) {
        console.error('load contests error:', error)
        setGroups([])
      } finally {
        setLoading(false)
      }
    }

    void loadData()
  }, [])

  const activeCount = useMemo(() => groups.filter((item) => (item.status ?? 1) === 1).length, [groups])
  const joinedCount = useMemo(() => groups.filter((item) => item.joined).length, [groups])
  const filteredGroups = useMemo(() => groups.filter((group) => matchGroupSearch(group, keyword)), [groups, keyword])

  const handleOpenJoinModal = (group: ClassVO) => {
    setPendingJoinContest(group)
    setJoinReason('')
    setJoinModalOpen(true)
  }

  const handleCloseJoinModal = () => {
    if (joiningId) {
      return
    }

    setJoinModalOpen(false)
    setPendingJoinContest(null)
    setJoinReason('')
  }

  const handleJoin = async () => {
    const groupId = pendingJoinContest?.id

    if (groupId == null) {
      return
    }

    const normalizedGroupId = String(groupId).trim()
    if (!normalizedGroupId) {
      message.warning('竞赛参数无效')
      return
    }

    const reason = joinReason.trim()
    if (!reason) {
      message.warning('请输入申请理由')
      return
    }

    try {
      setJoiningId(normalizedGroupId)
      await joinOjClass(normalizedGroupId, { message: reason })
      setPendingJoinIds((prev) => {
        const next = new Set(prev)
        next.add(normalizedGroupId)
        return next
      })
      message.success('申请已发送，等待管理员审核')
      setJoinModalOpen(false)
      setPendingJoinContest(null)
      setJoinReason('')
    } catch (error) {
      console.error('join contest error:', error)
      message.error('申请加入竞赛失败，请稍后重试')
    } finally {
      setJoiningId(undefined)
    }
  }

  return (
    <Space direction="vertical" size={20} className="full-width class-page">
      <div className="channel-hero card-surface class-hero">
        <div>
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/practice')} className="class-hero__back-btn">
            返回刷题页
          </Button>
          <div className="channel-hero__label">竞赛模块</div>
          <h2>通过竞赛模式限时刷题，在实战节奏里检验掌握程度</h2>
          <p>有不同难度的竞赛供大家参加，实时关注。</p>
        </div>
        <div className="class-hero__side">
          <div className="class-hero__badge">
            <TrophyOutlined /> 已加入 {joinedCount} / {groups.length} 个竞赛
          </div>
          <div className="class-hero__badge">
            <TrophyOutlined /> 进行中竞赛 {activeCount} 场
          </div>
        </div>
      </div>

      <Card className="content-card class-search-card" variant="borderless">
        <Input.Search
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          allowClear
          enterButton="搜索"
          placeholder="输入竞赛 ID 或竞赛名称搜索"
        />
      </Card>

      {loading ? (
        <Card className="content-card" variant="borderless">
          <div className="check-in-loading"><Spin /></div>
        </Card>
      ) : (
        <Row gutter={[20, 20]}>
          {filteredGroups.length ? filteredGroups.map((group) => {
            const groupId = String(group.id ?? '').trim()
            const isPendingJoin = pendingJoinIds.has(groupId)

            return (
              <Col xs={24} lg={8} key={groupId || group.name || 'contest'}>
                <Card className="class-card" variant="borderless">
                  <Space direction="vertical" size={14} className="full-width">
                    <Space wrap>
                      <Tag color="red" variant="filled">竞赛</Tag>
                      <Tag color={(group.status ?? 1) === 1 ? 'green' : (group.status ?? 1) === 0 ? 'blue' : 'default'}>
                        {getContestStatusText(group.status)}
                      </Tag>
                    </Space>
                    <h3>{group.name || '未命名竞赛'}</h3>
                    <p>{group.description || '暂无竞赛描述'}</p>
                    <div className="class-card__meta">参与 {group.memberCount ?? 0}</div>
                    <Space>
                      {!group.joined ? (
                        isPendingJoin ? (
                          <Button disabled>
                            审核中
                          </Button>
                        ) : (
                          <Button type="primary" loading={joiningId === groupId} onClick={() => handleOpenJoinModal(group)}>
                            加入竞赛
                          </Button>
                        )
                      ) : (
                        <Button type="primary" onClick={() => navigate(`/contests/${groupId || group.id}`)}>
                          进入竞赛
                        </Button>
                      )}
                    </Space>
                  </Space>
                </Card>
              </Col>
            )
          }) : (
            <Col span={24}>
              <Card className="content-card" variant="borderless">
                <Empty description={keyword.trim() ? '未搜索到匹配的竞赛' : '暂无竞赛内容'} />
              </Card>
            </Col>
          )}
        </Row>
      )}

      <Modal
        title="申请加入竞赛"
        open={joinModalOpen}
        okText="发送申请"
        cancelText="取消"
        confirmLoading={Boolean(joiningId)}
        onOk={() => void handleJoin()}
        onCancel={handleCloseJoinModal}
        destroyOnHidden
      >
        <Space direction="vertical" size={12} className="full-width">
          <div>竞赛名称：{pendingJoinContest?.name || '未命名竞赛'}</div>
          <Input.TextArea
            value={joinReason}
            onChange={(event) => setJoinReason(event.target.value)}
            placeholder="请输入申请加入竞赛的理由，例如你的参赛目标或刷题基础"
            autoSize={{ minRows: 4, maxRows: 6 }}
            maxLength={200}
          />
        </Space>
      </Modal>
    </Space>
  )
}
