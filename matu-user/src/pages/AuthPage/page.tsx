import { CodeOutlined } from '@ant-design/icons'
import { Button, Checkbox, Form, Input, Typography, message } from 'antd'
import axios from 'axios'
import { useNavigate } from 'react-router-dom'
import { useState } from 'react'
import { getCurrentUserProfile } from '../../api/authProfile'
import { login, register, sendRegisterEmailCode } from '../../api/login'
import './page.scss'

const { Title, Text } = Typography

type AuthMode = 'login' | 'register'
type AuthFormValues = {
  account?: string
  username?: string
  email?: string
  emailCode?: string
  password: string
  confirmPassword?: string
}

const getAxiosErrorMessage = (error: unknown, fallback: string) => {
  if (!axios.isAxiosError(error)) {
    return fallback
  }

  const responseData = error.response?.data

  if (typeof responseData === 'string' && responseData) {
    return responseData
  }

  if (responseData && typeof responseData === 'object') {
    return responseData.message || responseData.msg || responseData.error || responseData.data || fallback
  }

  return fallback
}

const authFeatureCards = [
  { title: '技术文章', desc: '优质原创技术分享', icon: '▤' },
  { title: '问答交流', desc: '解决问题 共同进步', icon: '?' },
  { title: '学习资源', desc: '精选资源 提升技能', icon: '▦' },
  { title: 'OJ 刷题', desc: '记录训练 提升能力', icon: '✓' },
]

export function AuthPage() {
  const [mode, setMode] = useState<AuthMode>('login')
  const [submitting, setSubmitting] = useState(false)
  const [sendingCode, setSendingCode] = useState(false)
  const [countdown, setCountdown] = useState(0)
  const [form] = Form.useForm<AuthFormValues>()
  const navigate = useNavigate()

  const handleSendCode = async () => {
    try {
      const email = (form.getFieldValue('email') || '').trim()

      if (!email) {
        message.warning('请先输入邮箱地址')
        return
      }

      form.setFieldValue('email', email)
      await form.validateFields(['email'])
      setSendingCode(true)
      await sendRegisterEmailCode({ email })
      message.success('验证码已发送，请注意查收')

      setCountdown(60)
      const timer = window.setInterval(() => {
        setCountdown((prev) => {
          if (prev <= 1) {
            window.clearInterval(timer)
            return 0
          }

          return prev - 1
        })
      }, 1000)
    } catch (error) {
      console.error('send email code error:', error)
      message.error(getAxiosErrorMessage(error, '验证码发送失败，请稍后重试'))
    } finally {
      setSendingCode(false)
    }
  }

  const handleSubmit = async (values: AuthFormValues) => {
    try {
      setSubmitting(true)

      const response =
        mode === 'login'
          ? await login({
              account: values.account ?? '',
              password: values.password,
            })
          : await register({
              username: values.username ?? '',
              email: values.email ?? '',
              emailCode: values.emailCode ?? '',
              password: values.password,
              confirmPassword: values.confirmPassword ?? '',
            })

      const authorizationToken = response.authorization?.trim()
      const tokenValue = response.tokenValue?.trim()

      localStorage.setItem('codehub-token', tokenValue || '')
      localStorage.setItem('token', tokenValue || '')
      localStorage.setItem('codehub-token-name', 'Authorization')
      if (authorizationToken) {
        localStorage.setItem('codehub-authorization', authorizationToken)
      } else if (tokenValue) {
        localStorage.setItem('codehub-authorization', `Bearer ${tokenValue}`)
      } else {
        localStorage.removeItem('codehub-authorization')
      }
      localStorage.setItem(
        'codehub-user',
        JSON.stringify({
          userId: response.userId,
          username: response.username,
          nickname: (response as typeof response & { nickname?: string; nikename?: string }).nickname,
          nikename: (response as typeof response & { nickname?: string; nikename?: string }).nikename,
          email: response.email,
          roles: response.roles,
        }),
      )

      try {
        const rawStoredUser = localStorage.getItem('codehub-user')
        let currentUser: Record<string, unknown> = {}

        if (rawStoredUser) {
          try {
            currentUser = JSON.parse(rawStoredUser) as Record<string, unknown>
          } catch {
            currentUser = {}
          }
        }

        const profile = await getCurrentUserProfile()
        localStorage.setItem(
          'codehub-user',
          JSON.stringify({
            userId: profile.userId ?? currentUser.userId,
            username: profile.username ?? currentUser.username,
            nickname: profile.nickname ?? currentUser.nickname,
            email: profile.email ?? currentUser.email,
            roles: profile.roles?.length ? profile.roles : currentUser.roles,
            avatarUrl: profile.avatarUrl ?? currentUser.avatarUrl,
            title: profile.title ?? currentUser.title,
            titleVerified: profile.titleVerified ?? currentUser.titleVerified,
            schoolVerified: profile.schoolVerified ?? currentUser.schoolVerified,
            companyVerified: profile.companyVerified ?? currentUser.companyVerified,
          }),
        )
      } catch (profileError) {
        console.error('sync profile after auth error:', profileError)
      }
      window.dispatchEvent(new Event('storage'))
      message.success(mode === 'login' ? '登录成功' : '注册成功')
      navigate('/home')
    } catch (error) {
      console.error(error)
      message.error(
        getAxiosErrorMessage(
          error,
          mode === 'login' ? '登录失败，请检查账号密码或稍后重试' : '注册失败，请稍后重试',
        ),
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="auth-page auth-reference-page">
      <div className="auth-reference-shell">
        <section className="auth-reference-visual">
          <div className="auth-reference-orbit auth-reference-orbit--one" />
          <div className="auth-reference-orbit auth-reference-orbit--two" />
          <div className="auth-reference-grid-glow" />
          <div className="auth-reference-floating-code auth-reference-floating-code--one">
            <CodeOutlined />
          </div>
          <div className="auth-reference-floating-code auth-reference-floating-code--two">function</div>
          <div className="auth-reference-floating-code auth-reference-floating-code--three">const</div>
          <div className="auth-reference-floating-code auth-reference-floating-code--four">return</div>

          <div className="auth-reference-brand">
            <span className="auth-reference-brand__logo">◔</span>
            <span>Matu User</span>
          </div>

          <div className="auth-reference-hero">
            <div className="auth-reference-copy">
              <Title level={1}>
                连接每一个
                <span>热爱技术的人</span>
              </Title>
              <Text>分享技术经验 · 探索优质资源 · 参与项目协作</Text>
            </div>

            <div className="auth-reference-showcase">
              <div className="auth-reference-feature-grid">
                {authFeatureCards.map((item) => (
                  <div className="auth-reference-feature-card" key={item.title}>
                    <span className="auth-reference-feature-card__icon">{item.icon}</span>
                    <div>
                      <strong>{item.title}</strong>
                      <small>{item.desc}</small>
                    </div>
                  </div>
                ))}
              </div>

              <div className="auth-reference-device">
                <div className="auth-reference-device__screen">
                  <span className="auth-reference-device__dot" />
                  <span className="auth-reference-device__dot" />
                  <span className="auth-reference-device__dot" />
                  <div className="auth-reference-device__line auth-reference-device__line--short" />
                  <div className="auth-reference-device__line" />
                  <div className="auth-reference-device__line auth-reference-device__line--blue" />
                  <div className="auth-reference-device__line" />
                  <div className="auth-reference-device__line auth-reference-device__line--cyan" />
                  <div className="auth-reference-device__line auth-reference-device__line--short" />
                </div>
              </div>

              <div className="auth-reference-cup">
                <CodeOutlined />
              </div>
              <div className="auth-reference-book auth-reference-book--one" />
              <div className="auth-reference-book auth-reference-book--two" />
            </div>
          </div>

          <div className="auth-reference-bottom-copy">
            <Title level={2}>高效、轻松、快速</Title>
            <Text>
              Matu 帮你把学习记录、题库进度与问答沉淀收束到一个地方，让每天的成长都有迹可循。
            </Text>
          </div>

        </section>

        <section className="auth-reference-form-panel">
          <div className="auth-reference-admin-brand">
            <div className="auth-reference-admin-brand__mark" />
            <div className="auth-reference-admin-brand__content">
              <div className="auth-reference-admin-brand__title-row">
                <Title level={3}>Matu User</Title>
              </div>
              <Text>计算机交流平台登录页面</Text>
            </div>
          </div>

          <div className="auth-reference-form-wrap auth-reference-form-wrap--admin">
            <div className="auth-reference-heading auth-reference-heading--admin">
              <span className="auth-reference-heading__eyebrow">用户入口</span>
              <Title level={1}>{mode === 'login' ? '登录到主页' : '注册账号'}</Title>
            </div>

            <Form<AuthFormValues>
              form={form}
              layout="vertical"
              className="auth-reference-form auth-reference-form--admin"
              onFinish={handleSubmit}
            >
              <Form.Item
                label="账号"
                name={mode === 'login' ? 'account' : 'username'}
                rules={[{ required: true, message: '请输入账号' }]}
              >
                <Input size="large" placeholder={mode === 'login' ? '请输入账号' : '请输入你要注册的账号'} />
              </Form.Item>

              {mode === 'register' ? (
                <>
                  <Form.Item
                    label="邮箱"
                    name="email"
                    rules={[
                      { required: true, message: '请输入邮箱地址' },
                      { type: 'email', message: '请输入正确的邮箱地址' },
                    ]}
                  >
                    <Input size="large" placeholder="请输入邮箱地址" />
                  </Form.Item>

                  <Form.Item
                    label="邮箱验证码"
                    name="emailCode"
                    rules={[
                      { required: true, message: '请输入邮箱验证码' },
                      { len: 6, message: '邮箱验证码必须为 6 位' },
                      { pattern: /^\d{6}$/, message: '邮箱验证码只能为 6 位数字' },
                    ]}
                  >
                    <Input
                      size="large"
                      placeholder="请输入邮箱验证码"
                      inputMode="numeric"
                      maxLength={6}
                      onChange={(event) => {
                        const nextValue = event.target.value.replace(/\D/g, '').slice(0, 6)
                        form.setFieldValue('emailCode', nextValue)
                      }}
                      suffix={(
                        <Button
                          type="link"
                          className="auth-reference-code-button"
                          onClick={handleSendCode}
                          disabled={countdown > 0 || sendingCode}
                          loading={sendingCode}
                        >
                          {countdown > 0 ? `${countdown}s 后重试` : '获取验证码'}
                        </Button>
                      )}
                    />
                  </Form.Item>
                </>
              ) : null}

              <Form.Item
                label="密码"
                name="password"
                rules={[
                  { required: true, message: '请输入密码' },
                  { min: 6, max: 20, message: '密码长度需在 6-20 位之间' },
                ]}
              >
                <Input size="large" type="password" placeholder="请输入密码" />
              </Form.Item>

              {mode === 'register' ? (
                <Form.Item
                  label="确认密码"
                  name="confirmPassword"
                  dependencies={['password']}
                  rules={[
                    { required: true, message: '请再次输入密码' },
                    ({ getFieldValue }) => ({
                      validator(_, value) {
                        if (!value || getFieldValue('password') === value) {
                          return Promise.resolve()
                        }

                        return Promise.reject(new Error('两次输入的密码不一致'))
                      },
                    }),
                  ]}
                >
                  <Input size="large" type="password" placeholder="请再次输入密码" />
                </Form.Item>
              ) : null}

              {mode === 'login' ? (
                <div className="auth-reference-row auth-reference-row--admin">
                  <Checkbox defaultChecked>记住本次登录</Checkbox>
                </div>
              ) : (
                <Text className="auth-reference-register-tip">注册后即可登录。</Text>
              )}

              <Button
                type="primary"
                htmlType="submit"
                block
                loading={submitting}
                className="auth-reference-submit auth-reference-submit--admin"
              >
                {mode === 'login' ? '登录' : '注册'}
              </Button>
            </Form>

            <div className="auth-reference-switch auth-reference-switch--admin">
              {mode === 'login' ? '还没有账号？' : '已有账号？'}
              <button type="button" onClick={() => setMode(mode === 'login' ? 'register' : 'login')}>
                {mode === 'login' ? '去注册' : '去登录'}
              </button>
            </div>
          </div>

          <div className="auth-reference-footer auth-reference-footer--admin">
            Peak
          </div>
        </section>
      </div>
    </div>
  )
}
