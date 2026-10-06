import { BugOutlined, CheckCircleOutlined, FileTextOutlined, ReloadOutlined, SendOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Empty, Form, Input, Modal, Pagination, Select, Space, Spin, Tag, message } from 'antd'
import axios from 'axios'
import { useCallback, useEffect, useState } from 'react'
import { createFeedback, getFeedbackDetail, listMyFeedbacks } from '../../api/feedback'
import type { CreateFeedbackRequest, FeedbackDetailVO, FeedbackListItemVO } from '../../api/type/feedbackTypings'
import { useAppSelector } from '../../store/hooks'
import './page.scss'

const DEFAULT_PAGE_SIZE = 5

type FeedbackFormValues = {
  contactEmail?: string
  type?: number
  title: string
  content: string
}

const feedbackTypeOptions = [
  { label: 'Bug 报告', value: 0 },
  { label: '功能建议', value: 1 },
  { label: '内容举报', value: 2 },
  { label: '账号问题', value: 3 },
  { label: '其他', value: 4 },
]

const getAxiosErrorMessage = (error: unknown, fallback: string) => {
  if (!axios.isAxiosError(error)) {
    return error instanceof Error && error.message ? error.message : fallback
  }

  const responseData = error.response?.data
  if (typeof responseData === 'string' && responseData) return responseData
  if (responseData && typeof responseData === 'object') {
    const data = responseData as { message?: string; msg?: string; error?: string }
    return data.message || data.msg || data.error || fallback
  }

  return error.message || fallback
}

const getFeedbackTypeInfo = (type?: number) => {
  if (type === 1) return { label: '功能建议', color: 'blue' }
  if (type === 2) return { label: '内容举报', color: 'red' }
  if (type === 3) return { label: '账号问题', color: 'purple' }
  if (type === 4) return { label: '其他', color: 'default' }
  return { label: 'Bug 报告', color: 'orange' }
}

const getFeedbackStatusInfo = (status?: number) => {
  if (status === 1) return { label: '处理中', color: 'blue' }
  if (status === 2) return { label: '已解决', color: 'green' }
  if (status === 3) return { label: '已拒绝', color: 'red' }
  if (status === 4) return { label: '已关闭', color: 'default' }
  return { label: '待处理', color: 'gold' }
}

const formatTime = (value?: string | null) => {
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

const getClientExtraInfo = () => ({
  browser: navigator.userAgent,
  os: navigator.platform,
  language: navigator.language,
  resolution: `${window.screen.width}x${window.screen.height}`,
  url: window.location.href,
})

export function FeedbackPage() {
  const isLoggedIn = useAppSelector((state) => state.auth.isLoggedIn)
  const userInfo = useAppSelector((state) => state.auth.userInfo)
  const [form] = Form.useForm<FeedbackFormValues>()
  const [submitting, setSubmitting] = useState(false)
  const [feedbacks, setFeedbacks] = useState<FeedbackListItemVO[]>([])
  const [feedbackLoading, setFeedbackLoading] = useState(false)
  const [feedbackError, setFeedbackError] = useState('')
  const [pageNum, setPageNum] = useState(1)
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE)
  const [total, setTotal] = useState(0)
  const [detailOpen, setDetailOpen] = useState(false)
  const [detailLoading, setDetailLoading] = useState(false)
  const [detailError, setDetailError] = useState('')
  const [detail, setDetail] = useState<FeedbackDetailVO | null>(null)

  const loadMyFeedbacks = useCallback(async (nextPage = pageNum, nextPageSize = pageSize) => {
    if (!isLoggedIn) {
      setFeedbacks([])
      setTotal(0)
      return
    }

    try {
      setFeedbackLoading(true)
      setFeedbackError('')
      const data = await listMyFeedbacks({ pageNum: nextPage, pageSize: nextPageSize })
      setFeedbacks(data.records || [])
      setPageNum(data.pageNum || nextPage)
      setPageSize(data.pageSize || nextPageSize)
      setTotal(data.total || 0)
    } catch (fetchError) {
      console.error('load my feedbacks error:', fetchError)
      setFeedbacks([])
      setFeedbackError(getAxiosErrorMessage(fetchError, '我的反馈加载失败'))
    } finally {
      setFeedbackLoading(false)
    }
  }, [isLoggedIn, pageNum, pageSize])

  useEffect(() => {
    if (userInfo?.email) {
      form.setFieldValue('contactEmail', userInfo.email)
    }
  }, [form, userInfo?.email])

  useEffect(() => {
    void loadMyFeedbacks(1, DEFAULT_PAGE_SIZE)
  }, [loadMyFeedbacks])

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      const payload: CreateFeedbackRequest = {
        contactEmail: values.contactEmail?.trim() || undefined,
        type: values.type ?? 0,
        title: values.title.trim(),
        content: values.content.trim(),
        attachments: [],
        extraInfo: getClientExtraInfo(),
      }

      setSubmitting(true)
      await createFeedback(payload)
      message.success('反馈已提交，感谢你的建议')
      form.resetFields()
      if (userInfo?.email) {
        form.setFieldValue('contactEmail', userInfo.email)
      }
      void loadMyFeedbacks(1, pageSize)
    } catch (submitError) {
      if (submitError && typeof submitError === 'object' && 'errorFields' in submitError) {
        return
      }
      console.error('create feedback error:', submitError)
      message.error(getAxiosErrorMessage(submitError, '反馈提交失败，请稍后重试'))
    } finally {
      setSubmitting(false)
    }
  }

  const handleOpenDetail = async (feedback: FeedbackListItemVO) => {
    if (feedback.id == null) return
    try {
      setDetailOpen(true)
      setDetailLoading(true)
      setDetailError('')
      setDetail(null)
      const data = await getFeedbackDetail(feedback.id)
      setDetail(data)
    } catch (fetchError) {
      console.error('load feedback detail error:', fetchError)
      setDetailError(getAxiosErrorMessage(fetchError, '反馈详情加载失败'))
    } finally {
      setDetailLoading(false)
    }
  }

  return (
    <Space direction="vertical" size={20} className="full-width feedback-page">
      <Card className="content-card feedback-hero" variant="borderless">
        <div className="channel-hero__label">用户反馈</div>
        <h2>把问题和建议告诉我们</h2>
        <p>支持匿名反馈。已登录用户提交后可以在本页查看自己的反馈处理状态。</p>
      </Card>

      <div className="feedback-layout">
        <Card className="content-card feedback-form-card" variant="borderless">
          <div className="feedback-section-title">
            <BugOutlined /> 提交反馈
          </div>
          <Form<FeedbackFormValues>
            form={form}
            layout="vertical"
            initialValues={{ type: 0 }}
          >
            <Form.Item name="type" label="反馈类型">
              <Select options={feedbackTypeOptions} />
            </Form.Item>
            <Form.Item
              name="title"
              label="反馈标题"
              rules={[{ required: true, message: '请输入反馈标题' }, { max: 255, message: '标题最多 255 字' }]}
            >
              <Input placeholder="例如：页面按钮无法点击" />
            </Form.Item>
            <Form.Item
              name="content"
              label="反馈内容"
              rules={[{ required: true, message: '请输入反馈内容' }]}
            >
              <Input.TextArea rows={7} placeholder="请描述问题出现的位置、操作步骤、期望结果或建议内容" />
            </Form.Item>
            <Form.Item
              name="contactEmail"
              label="联系邮箱"
              rules={[{ type: 'email', message: '请输入正确的邮箱格式' }, { max: 255, message: '邮箱最多 255 字' }]}
              extra="选填。匿名反馈也可以留下邮箱，方便后续联系。"
            >
              <Input placeholder="user@example.com" />
            </Form.Item>
            <Alert
              type="info"
              showIcon
              description="提交时会自动附带浏览器、系统、分辨率和当前页面地址，帮助定位问题。"
              className="feedback-form-tip"
            />
            <Button type="primary" size="large" icon={<SendOutlined />} loading={submitting} onClick={() => void handleSubmit()}>
              提交反馈
            </Button>
          </Form>
        </Card>

        <Card className="content-card feedback-list-card" variant="borderless">
          <div className="feedback-section-title feedback-section-title--between">
            <span><FileTextOutlined /> 我的反馈</span>
            {isLoggedIn ? (
              <Button size="small" icon={<ReloadOutlined />} loading={feedbackLoading} onClick={() => void loadMyFeedbacks(1, pageSize)}>
                刷新
              </Button>
            ) : null}
          </div>

          {!isLoggedIn ? (
            <Alert type="info" showIcon description="登录后可以查看自己提交过的反馈和处理状态。匿名反馈提交后不会出现在这里。" />
          ) : feedbackLoading ? (
            <div className="feedback-loading"><Spin size="large" /></div>
          ) : feedbackError ? (
            <Alert type="error" showIcon description={feedbackError} />
          ) : feedbacks.length ? (
            <Space direction="vertical" size={12} className="full-width">
              {feedbacks.map((feedback) => {
                const typeInfo = getFeedbackTypeInfo(feedback.type)
                const statusInfo = getFeedbackStatusInfo(feedback.status)
                return (
                  <button key={String(feedback.id ?? feedback.title)} type="button" className="feedback-item" onClick={() => void handleOpenDetail(feedback)}>
                    <div className="feedback-item__top">
                      <Space wrap size={6}>
                        <Tag color={typeInfo.color}>{typeInfo.label}</Tag>
                        <Tag color={statusInfo.color}>{statusInfo.label}</Tag>
                      </Space>
                      <span>{formatTime(feedback.createdAt)}</span>
                    </div>
                    <strong>{feedback.title || '未命名反馈'}</strong>
                    {feedback.repliedAt ? <div className="feedback-item__reply"><CheckCircleOutlined /> 已回复</div> : null}
                  </button>
                )
              })}
              <div className="feedback-pagination">
                <Pagination
                  current={pageNum}
                  pageSize={pageSize}
                  total={total}
                  showSizeChanger
                  showTotal={(count) => `共 ${count} 条反馈`}
                  onChange={(nextPage, nextPageSize) => void loadMyFeedbacks(nextPage, nextPageSize)}
                />
              </div>
            </Space>
          ) : (
            <Empty description="暂无反馈记录" />
          )}
        </Card>
      </div>

      <Modal
        title="反馈详情"
        open={detailOpen}
        onCancel={() => setDetailOpen(false)}
        footer={null}
        width={720}
        destroyOnHidden
      >
        {detailLoading ? (
          <div className="feedback-loading"><Spin size="large" /></div>
        ) : detailError ? (
          <Alert type="error" showIcon description={detailError} />
        ) : detail ? (
          <div className="feedback-detail">
            <Space wrap size={8} className="feedback-detail__meta">
              <Tag color={getFeedbackTypeInfo(detail.type).color}>{getFeedbackTypeInfo(detail.type).label}</Tag>
              <Tag color={getFeedbackStatusInfo(detail.status).color}>{getFeedbackStatusInfo(detail.status).label}</Tag>
              <span>{formatTime(detail.createdAt)}</span>
            </Space>
            <h3>{detail.title || '未命名反馈'}</h3>
            <div className="feedback-detail__block">
              <strong>反馈内容</strong>
              <p>{detail.content || '暂无内容'}</p>
            </div>
            <div className="feedback-detail__block">
              <strong>官方回复</strong>
              <p>{detail.replyContent || '暂未回复'}</p>
            </div>
            <div className="feedback-detail__meta-grid">
              {detail.history?.map((item) => <div key={String(item.id)}>
                <span>{formatTime(item.createdAt)} · {getFeedbackStatusInfo(item.fromStatus).label} → {getFeedbackStatusInfo(item.toStatus).label}</span>
                <p>{item.replyContent || '处理状态已更新'}</p>
              </div>)}
              <div><span>联系邮箱</span><strong>{detail.contactEmail || '暂无'}</strong></div>
              <div><span>回复时间</span><strong>{formatTime(detail.repliedAt)}</strong></div>
              <div><span>解决时间</span><strong>{formatTime(detail.resolvedAt)}</strong></div>
              <div><span>更新时间</span><strong>{formatTime(detail.updatedAt)}</strong></div>
            </div>
          </div>
        ) : (
          <Empty description="暂无反馈详情" />
        )}
      </Modal>
    </Space>
  )
}
