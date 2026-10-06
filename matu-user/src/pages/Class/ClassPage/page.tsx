import { ArrowLeftOutlined, SettingOutlined, TeamOutlined, UserSwitchOutlined } from '@ant-design/icons'
import { Button, Card, Col, Empty, Input, Modal, Row, Space, Spin, Tag, message } from 'antd'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { joinOjClass, listOjClasses } from '../../../api/oj'
import type { ClassVO } from '../../../api/type/ojTypings'
import { useAppSelector } from '../../../store/hooks'
import { canPublishCourse } from '../../../utils/permissions'
import './page.scss'

const normalizeSearchText = (value?: string | number) => String(value ?? '').trim().toLowerCase()

const matchGroupSearch = (group: ClassVO, keyword: string) => {
  const normalizedKeyword = normalizeSearchText(keyword)
  if (!normalizedKeyword) {
    return true
  }

  return [group.id, group.name].some((value) => normalizeSearchText(value).includes(normalizedKeyword))
}

export function ClassPage() {
  const navigate = useNavigate()
  const userInfo = useAppSelector((state) => state.auth.userInfo)
  const canManageClass = canPublishCourse(userInfo)
  const [groups, setGroups] = useState<ClassVO[]>([])
  const [loading, setLoading] = useState(true)
  const [joiningId, setJoiningId] = useState<string | undefined>(undefined)
  const [joinModalOpen, setJoinModalOpen] = useState(false)
  const [pendingJoinClass, setPendingJoinClass] = useState<ClassVO | null>(null)
  const [joinReason, setJoinReason] = useState('')
  const [keyword, setKeyword] = useState('')
  const [pendingJoinIds, setPendingJoinIds] = useState<Set<string>>(() => new Set())

  useEffect(() => {
    const loadData = async () => {
      try {
        setLoading(true)
        const data = await listOjClasses({ status: 1, pageNum: 1, pageSize: 50 })
        setGroups((data.records || []).filter((item) => item.type === 1))
      } catch (error) {
        console.error('load classes error:', error)
        setGroups([])
      } finally {
        setLoading(false)
      }
    }

    void loadData()
  }, [])

  const joinedCount = useMemo(() => groups.filter((item) => item.joined).length, [groups])
  const filteredGroups = useMemo(() => groups.filter((group) => matchGroupSearch(group, keyword)), [groups, keyword])

  const handleOpenJoinModal = (group: ClassVO) => {
    setPendingJoinClass(group)
    setJoinReason('')
    setJoinModalOpen(true)
  }

  const handleCloseJoinModal = () => {
    if (joiningId) {
      return
    }
    setJoinModalOpen(false)
    setPendingJoinClass(null)
    setJoinReason('')
  }

  const handleJoin = async () => {
    const groupId = pendingJoinClass?.id
    if (groupId == null) {
      return
    }

    const normalizedGroupId = String(groupId).trim()
    if (!normalizedGroupId) {
      message.warning('班级参数无效')
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
      setPendingJoinClass(null)
      setJoinReason('')
    } catch (error) {
      console.error('join class error:', error)
      message.error('申请加入班级失败，请稍后重试')
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
          <div className="channel-hero__label">班级刷题模块</div>
          <h2>加入适合自己的班级，和同伴一起完成专题训练</h2>
          <p>班级会围绕算法、SQL 和编程语言基础组织题单，适合长期刷题和分阶段提升。</p>
        </div>
        <div className="class-hero__side">
          <div className="class-hero__badge">
            <TeamOutlined /> 已加入 {joinedCount} / {groups.length} 个班级
          </div>
          {canManageClass ? (
            <Button type="primary" icon={<SettingOutlined />} onClick={() => navigate('/classes/manage')}>
              班级管理
            </Button>
          ) : (
            <Button icon={<UserSwitchOutlined />} onClick={() => navigate('/profile/edit')}>
              申请成为老师
            </Button>
          )}
        </div>
      </div>

      <Card className="content-card class-search-card" variant="borderless">
        <Input.Search
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          allowClear
          enterButton="搜索"
          placeholder="输入班级 ID 或班级名称搜索"
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
              <Col xs={24} lg={8} key={groupId || group.name || 'group'}>
                <Card className="class-card" variant="borderless">
                  <Space direction="vertical" size={14} className="full-width">
                    <Tag color="blue" variant="filled">普通班级</Tag>
                    <h3>{group.name || '未命名班级'}</h3>
                    <p>{group.description || '暂无班级描述'}</p>
                    <div className="class-card__meta">成员 {group.memberCount ?? 0}</div>
                    <Space>
                      {!group.joined ? (
                        isPendingJoin ? (
                          <Button disabled>
                            审核中
                          </Button>
                        ) : (
                          <Button type="primary" loading={joiningId === groupId} onClick={() => handleOpenJoinModal(group)}>
                            加入班级
                          </Button>
                        )
                      ) : (
                        <Button type="primary" onClick={() => navigate(`/classes/${group.id}`)}>
                          进入班级
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
                <Empty description={keyword.trim() ? '未搜索到匹配的班级' : '暂无班级内容'} />
              </Card>
            </Col>
          )}
        </Row>
      )}

      <Modal
        title="申请加入班级"
        open={joinModalOpen}
        onOk={() => void handleJoin()}
        onCancel={handleCloseJoinModal}
        confirmLoading={Boolean(joiningId)}
        okText="发送申请"
        cancelText="取消"
        destroyOnHidden
      >
        <Space direction="vertical" size={12} className="full-width">
          <div>班级名称：{pendingJoinClass?.name || '未命名班级'}</div>
          <Input.TextArea
            value={joinReason}
            onChange={(event) => setJoinReason(event.target.value)}
            placeholder="请输入申请加入班级的理由，例如你的学习目标或基础情况"
            rows={5}
            maxLength={200}
          />
        </Space>
      </Modal>
    </Space>
  )
}
