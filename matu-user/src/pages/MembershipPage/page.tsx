import { CrownOutlined, SafetyCertificateOutlined, ThunderboltOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Col, Modal, QRCode, Row, Space, Spin, Tag, message } from 'antd'
import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  createAlipayQrPayment,
  createMembershipOrder,
  getMembershipError,
  getMembershipOrder,
  getMyVipStatus,
  type AlipayQrPayment,
  type MembershipOrder,
  type MembershipProductCode,
  type VipStatus,
} from '../../api/membership'
import { useAppSelector } from '../../store/hooks'
import './page.scss'

type MembershipPlan = {
  id: MembershipProductCode
  name: string
  price: number
  durationDays: number
  badge: string
  description: string
  benefits: string[]
}

type PaymentState = 'checking' | 'pending' | 'paid' | 'closed' | 'refunded' | 'partial-refunded' | 'expired' | 'timeout' | 'error'

const plans: MembershipPlan[] = [
  {
    id: 'VIP_MONTH', name: '月度会员', price: 19.90, durationDays: 31, badge: '灵活体验',
    description: '适合短期冲刺学习和面试准备。',
    benefits: ['解锁会员专享面试题答案', '成长报告与学习日历', '活跃榜单会员标识'],
  },
  {
    id: 'VIP_QUARTER', name: '季度会员', price: 49.90, durationDays: 93, badge: '推荐',
    description: '适合系统训练和阶段性学习。',
    benefits: ['包含月度全部权益', '更长周期成长记录', '优先体验后续会员能力'],
  },
  {
    id: 'VIP_YEAR', name: '年度会员', price: 159.90, durationDays: 366, badge: '更划算',
    description: '适合长期学习、刷题和内容沉淀。',
    benefits: ['包含季度全部权益', '全年会员身份标识', '长期成长数据沉淀'],
  },
]

const formatDate = (value?: string | null) => {
  if (!value) return '暂无'
  const date = new Date(value.replace(' ', 'T'))
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN', { hour12: false })
}

const paymentLabels: Record<PaymentState, string> = {
  checking: '正在查询订单并准备支付二维码…',
  pending: '请使用支付宝扫码支付，系统每 3 秒自动查询支付结果。',
  paid: '支付成功，已重新查询服务端会员状态。',
  closed: '订单已关闭，请重新选择套餐下单。',
  refunded: '订单已退款，会员权益以服务端当前状态为准。',
  'partial-refunded': '订单已部分退款，会员权益以服务端当前状态为准。',
  expired: '已到支付截止时间，请勿继续扫码。可重新查询最终订单状态。',
  timeout: '自动查询已超时并停止。如已付款，请查询当前订单，避免重复付款。',
  error: '支付查询已暂停，请重试当前订单，避免重复下单。',
}

export function MembershipPage() {
  const { isLoggedIn, userInfo } = useAppSelector((state) => state.auth)
  // Account changes remount the payment session, cancelling requests and clearing old account data.
  return <MembershipContent key={`${isLoggedIn}:${userInfo?.userId ?? userInfo?.username ?? 'guest'}`} loggedIn={isLoggedIn} />
}

function MembershipContent({ loggedIn }: { loggedIn: boolean }) {
  const navigate = useNavigate()
  const [selectedPlanId, setSelectedPlanId] = useState<MembershipProductCode>('VIP_QUARTER')
  const [vip, setVip] = useState<VipStatus | null>(null)
  const [vipLoading, setVipLoading] = useState(loggedIn)
  const [vipError, setVipError] = useState('')
  const [vipRevision, setVipRevision] = useState(0)
  const [paying, setPaying] = useState(false)
  const [order, setOrder] = useState<MembershipOrder | null>(null)
  const [qr, setQr] = useState<AlipayQrPayment | null>(null)
  const [modalOpen, setModalOpen] = useState(false)
  const [paymentState, setPaymentState] = useState<PaymentState>('checking')
  const [paymentError, setPaymentError] = useState('')
  const [pollRevision, setPollRevision] = useState(0)
  const [createError, setCreateError] = useState('')
  const creatingRef = useRef(false)
  const createControllerRef = useRef<AbortController | null>(null)
  const pollControllerRef = useRef<AbortController | null>(null)
  const selectedPlan = plans.find((plan) => plan.id === selectedPlanId) || plans[0]
  const isVip = vip?.valid === true && vip.isVip === 1

  useEffect(() => {
    if (!loggedIn) return
    const controller = new AbortController()
    const load = async () => {
      setVipLoading(true)
      setVipError('')
      // Never keep displaying a cached valid membership if the refresh fails.
      setVip(null)
      try {
        const result = await getMyVipStatus(controller.signal)
        if (!controller.signal.aborted) setVip(result)
      } catch (error) {
        if (!controller.signal.aborted) setVipError(getMembershipError(error))
      } finally {
        if (!controller.signal.aborted) setVipLoading(false)
      }
    }
    void load()
    return () => controller.abort()
  }, [loggedIn, vipRevision])

  useEffect(() => () => {
    createControllerRef.current?.abort()
    pollControllerRef.current?.abort()
  }, [])

  useEffect(() => {
    if (!modalOpen || !order || order.status !== 0) return
    const controller = new AbortController()
    pollControllerRef.current = controller
    let timer: ReturnType<typeof setTimeout> | undefined
    let consecutiveErrors = 0
    const deadline = Date.parse(order.payDeadline?.replace(' ', 'T'))
    // Bound the polling session even if the backend omits a usable deadline.
    const watchdog = setTimeout(() => {
      controller.abort()
      clearTimeout(timer)
      setPaymentState('timeout')
    }, 15 * 60 * 1000)

    const poll = async () => {
      if (controller.signal.aborted) return
      try {
        const latest = await getMembershipOrder(order.orderNo, controller.signal)
        if (controller.signal.aborted) return
        setPaymentError('')
        if (latest.status !== 0) {
          setOrder(latest)
          setPaymentState(({ 1: 'paid', 2: 'closed', 3: 'refunded', 4: 'partial-refunded' } as const)[latest.status])
          setQr(null)
          setVipRevision((value) => value + 1)
          clearTimeout(watchdog)
          return
        }
        const currentDeadline = Date.parse(latest.payDeadline?.replace(' ', 'T'))
        const expiresAt = Number.isFinite(currentDeadline) ? currentDeadline : deadline
        if (Number.isFinite(expiresAt) && Date.now() >= expiresAt) {
          setPaymentState('expired')
          clearTimeout(watchdog)
          return
        }
        if (!qr) {
          const payment = await createAlipayQrPayment(order.orderNo, controller.signal)
          if (controller.signal.aborted) return
          setQr(payment)
        }
        consecutiveErrors = 0
        setPaymentState('pending')
        timer = setTimeout(() => void poll(), 3000)
      } catch (error) {
        if (controller.signal.aborted) return
        consecutiveErrors += 1
        setPaymentError(getMembershipError(error))
        if (consecutiveErrors >= 3) {
          setPaymentState('error')
          clearTimeout(watchdog)
        } else {
          timer = setTimeout(() => void poll(), 3000 * consecutiveErrors)
        }
      }
    }
    void poll()
    return () => {
      controller.abort()
      clearTimeout(timer)
      clearTimeout(watchdog)
    }
  }, [modalOpen, order, qr, pollRevision])

  const handlePay = async () => {
    if (!loggedIn) {
      message.warning('请先登录后再开通会员')
      navigate('/auth')
      return
    }
    // Synchronous guard protects both purchase buttons before React renders loading state.
    if (creatingRef.current) return
    if (order?.status === 0) {
      setPaymentState('checking')
      setPaymentError('')
      setPollRevision((value) => value + 1)
      setModalOpen(true)
      return
    }
    creatingRef.current = true
    const controller = new AbortController()
    createControllerRef.current = controller
    setPaying(true)
    setCreateError('')
    try {
      const created = await createMembershipOrder(selectedPlan.id, controller.signal)
      if (controller.signal.aborted) return
      setOrder(created)
      setQr(null)
      setPaymentError('')
      setPaymentState(created.status === 0 ? 'checking' : ({ 1: 'paid', 2: 'closed', 3: 'refunded', 4: 'partial-refunded' } as const)[created.status])
      if (created.status !== 0) setVipRevision((value) => value + 1)
      setModalOpen(true)
    } catch (error) {
      if (!controller.signal.aborted) {
        setCreateError(`${getMembershipError(error)}。若请求已提交但未收到响应，请先确认订单或付款情况，勿连续重复下单。`)
      }
    } finally {
      creatingRef.current = false
      if (!controller.signal.aborted) setPaying(false)
    }
  }

  const closePayment = () => {
    pollControllerRef.current?.abort()
    setModalOpen(false)
  }
  const retryPayment = () => {
    setPaymentState('checking')
    setPaymentError('')
    setPollRevision((value) => value + 1)
  }
  const purchaseText = order?.status === 0 ? '继续查询 / 支付已有订单' : `立即支付 ¥${selectedPlan.price.toFixed(2)}`
  const stopped = ['error', 'timeout', 'expired'].includes(paymentState)

  return (
    <div className="membership-page">
      <section className="membership-hero">
        <div>
          <div className="membership-hero__eyebrow"><CrownOutlined /> 码途会员中心</div>
          <h1>解锁更完整的学习能力</h1>
          <p>会员专享面试题答案、学习日历、成长报告和专属身份标识，适合系统刷题和长期成长。</p>
          <Space wrap>
            <Button type="primary" size="large" loading={paying} onClick={() => void handlePay()}>{purchaseText}</Button>
            <Button size="large" onClick={() => navigate('/profile')}>返回个人中心</Button>
          </Space>
        </div>
        <Card className="membership-status-card" variant="borderless">
          <div className="membership-status-card__label">当前状态（服务端实时查询）</div>
          <div className="membership-status-card__value">{!loggedIn ? '请先登录' : vipLoading ? '查询中…' : vipError ? '状态查询失败' : isVip ? '会员有效' : '当前无有效会员'}</div>
          <div className="membership-status-card__meta">到期时间：{formatDate(vip?.vipExpiredAt)}</div>
          {isVip && <div className="membership-status-card__meta">剩余 {vip?.vipDaysRemaining ?? 0} 天</div>}
          {vipError && <Alert type="warning" title={vipError} showIcon />}
          {loggedIn && <Button type="link" loading={vipLoading} onClick={() => setVipRevision((value) => value + 1)}>刷新会员状态</Button>}
        </Card>
      </section>
      {createError && <Alert type="error" title={createError} showIcon className="membership-error" />}
      <Row gutter={[20, 20]}>
        {plans.map((plan) => (
          <Col xs={24} md={8} key={plan.id}>
            <Card className={`membership-plan-card${selectedPlanId === plan.id ? ' membership-plan-card--active' : ''}`} variant="borderless" onClick={() => { if (!paying && order?.status !== 0) setSelectedPlanId(plan.id) }}>
              <div className="membership-plan-card__header">
                <div><h3>{plan.name}</h3><p>{plan.description}</p></div>
                <Tag color={plan.id === 'VIP_QUARTER' ? 'blue' : 'gold'}>{plan.badge}</Tag>
              </div>
              <div className="membership-plan-card__price"><span>¥</span>{plan.price.toFixed(2)}</div>
              <div className="membership-plan-card__duration">有效期 {plan.durationDays} 天</div>
              <ul>{plan.benefits.map((benefit) => <li key={benefit}><SafetyCertificateOutlined /> {benefit}</li>)}</ul>
            </Card>
          </Col>
        ))}
      </Row>
      <Card className="membership-action-card" variant="borderless">
        <div>
          <div className="membership-action-card__title"><ThunderboltOutlined /> 已选择：{selectedPlan.name}</div>
          <p>使用支付宝扫码支付，实际金额以服务端订单为准。支付成功后由服务端开通会员。</p>
        </div>
        <Button type="primary" size="large" loading={paying} onClick={() => void handlePay()}>{purchaseText}</Button>
      </Card>
      <Modal title="支付宝扫码支付" open={modalOpen} onCancel={closePayment} footer={<Space wrap>{stopped && <Button type="primary" onClick={retryPayment}>重新查询当前订单</Button>}<Button onClick={closePayment}>{order?.status === 0 ? '关闭并暂停查询' : '完成'}</Button></Space>}>
        {order && <Space orientation="vertical" size={16} className="full-width membership-payment">
          <div><strong>{order.productName}</strong> · ¥{Number(order.amount).toFixed(2)}</div>
          <div>订单号：{order.orderNo}</div>
          <div>支付截止：{formatDate(order.payDeadline)}</div>
          <Alert type={paymentState === 'paid' ? 'success' : stopped ? 'warning' : 'info'} title={paymentLabels[paymentState]} showIcon />
          {paymentError && <Alert type="warning" title={`${paymentError}${stopped ? '' : '，正在自动重试…'}`} showIcon />}
          {paymentState === 'checking' && <Spin />}
          {qr && paymentState === 'pending' && <div className="membership-payment__qr"><QRCode value={qr.qrCode} size={224} /></div>}
          {paymentState === 'paid' && <>
            <Alert type={isVip ? 'success' : 'info'} title={vipLoading ? '正在同步会员权益…' : vipError ? '会员状态查询失败，请点击刷新重试。' : isVip ? `会员有效，到期时间：${formatDate(vip?.vipExpiredAt)}` : '支付已确认，会员权益可能仍在同步，请稍后刷新。'} showIcon />
            <Button loading={vipLoading} onClick={() => setVipRevision((value) => value + 1)}>刷新会员权益</Button>
          </>}
          <div>关闭弹窗仅停止查询，不会取消服务端订单。请勿重复付款；如已支付，以服务端查询结果为准。</div>
        </Space>}
      </Modal>
    </div>
  )
}
