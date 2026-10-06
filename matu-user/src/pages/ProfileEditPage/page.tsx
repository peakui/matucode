import { ArrowLeftOutlined, CameraOutlined, FileImageOutlined, SafetyCertificateOutlined, SaveOutlined } from '@ant-design/icons'
import { Alert, Avatar, Button, Card, Col, DatePicker, Drawer, Form, Image, Input, Radio, Row, Select, Space, Spin, Table, Tag, Tooltip, message } from 'antd'
import dayjs from 'dayjs'
import type { ColumnsType } from 'antd/es/table'
import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getCurrentUserProfile, listMyCertifications, submitCertification, updateCurrentUserAvatar, updateCurrentUserProfile } from '../../api/authProfile'
import { uploadFile } from '../../api/file'
import type { AuthProfileVO, CertificationRecordVO, SubmitCertificationRequest, UpdateAuthProfileRequest } from '../../api/type/loginTypings'
import { DEFAULT_AVATAR_URL, resolveAvatarUrl } from '../../utils/avatar'
import './page.scss'

type ProfileFormValues = {
  nickname?: string
  phone?: string
  gender?: number
  birthday?: dayjs.Dayjs
  signature?: string
  schoolName?: string
  companyName?: string
  major?: string
  grade?: string
  workYears?: number
  technicalStackText?: string
  blogUrl?: string
  githubUrl?: string
  wechatUrl?: string
}

type CertificationActionType = 1 | 2 | 3

type CertificationFormValues = {
  certType: 1 | 2 | 3
  certName: string
  certProof?: string
}


const certificationTypeOptions: { label: string; value: 1 | 2 | 3 }[] = [
  { label: '学校认证', value: 1 },
  { label: '企业认证', value: 2 },
  { label: '老师认证', value: 3 },
]

const certificationStatusColorMap: Record<number, string> = {
  0: 'processing',
  1: 'success',
  2: 'error',
}

const certificationStatusCardClassMap: Record<number, string> = {
  0: 'is-pending',
  1: 'is-approved',
  2: 'is-rejected',
}

const normalizeTechnicalStack = (value?: string[] | string) => {
  if (Array.isArray(value)) {
    return value.join('、')
  }
  return value || ''
}

const toTechnicalStackPayload = (value?: string) => {
  return (value || '')
    .split(/[、,，\n]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

const resolveProfileAvatar = (profile?: AuthProfileVO | null) => {
  return profile?.avatarUrl || profile?.userAvatar || profile?.userAvatarUrl || profile?.headImgUrl || ''
}

const getAvatarUrl = (profile?: AuthProfileVO | null) => {
  return resolveAvatarUrl(profile) || DEFAULT_AVATAR_URL
}

const syncStoredUser = (profile: AuthProfileVO) => {
  const rawStoredUser = localStorage.getItem('codehub-user')
  let currentUser: Record<string, unknown> = {}

  if (rawStoredUser) {
    try {
      currentUser = JSON.parse(rawStoredUser) as Record<string, unknown>
    } catch {
      currentUser = {}
    }
  }

  const resolvedAvatar = resolveProfileAvatar(profile)

  const nextUser = {
    ...currentUser,
    userId: profile.userId ?? currentUser.userId,
    username: profile.username ?? currentUser.username,
    email: profile.email ?? currentUser.email,
    roles: profile.roles?.length ? profile.roles : currentUser.roles,
    nickname: profile.nickname ?? currentUser.nickname,
    phone: profile.phone ?? currentUser.phone,
    avatarUrl: resolvedAvatar || currentUser.avatarUrl,
    schoolName: profile.schoolName ?? currentUser.schoolName,
    schoolVerified: profile.schoolVerified ?? currentUser.schoolVerified,
    companyName: profile.companyName ?? currentUser.companyName,
    companyVerified: profile.companyVerified ?? currentUser.companyVerified,
    title: profile.title ?? currentUser.title,
    titleVerified: profile.titleVerified ?? currentUser.titleVerified,
  }

  localStorage.setItem('codehub-user', JSON.stringify(nextUser))

  if (profile.userId != null) {
    localStorage.setItem('codehub-user-id', String(profile.userId))
  }

  if (profile.authorization) {
    localStorage.setItem('codehub-authorization', profile.authorization)
  }

  if (profile.tokenValue) {
    localStorage.setItem('codehub-token', profile.tokenValue)
  }
}

const getUploadedFileUrl = (payload: { url?: string; fileUrl?: string; fullUrl?: string; downloadUrl?: string }) => {
  return payload.url || payload.fileUrl || payload.fullUrl || payload.downloadUrl || ''
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

const getCertificationTypeName = (value?: number) => {
  return certificationTypeOptions.find((item) => item.value === value)?.label || '未知认证'
}

const isApprovedCertification = (record: CertificationRecordVO) => (
  record.certStatus === 1 || /通过|approved/i.test(record.certStatusName || '')
)

const getApprovedCertificationName = (records: CertificationRecordVO[], certType: 1 | 2 | 3) => (
  records.find((record) => record.certType === certType && isApprovedCertification(record))?.certName?.trim() || ''
)

const applyApprovedCertificationsToProfile = (profile: AuthProfileVO | null, records: CertificationRecordVO[] = []) => {
  if (!profile) return null

  const approvedSchoolName = getApprovedCertificationName(records, 1)
  const approvedCompanyName = getApprovedCertificationName(records, 2)
  const approvedTitle = getApprovedCertificationName(records, 3)

  return {
    ...profile,
    schoolName: approvedSchoolName || profile.schoolName,
    schoolVerified: approvedSchoolName ? 1 : profile.schoolVerified,
    companyName: approvedCompanyName || profile.companyName,
    companyVerified: approvedCompanyName ? 1 : profile.companyVerified,
    title: approvedTitle || profile.title,
    titleVerified: approvedTitle ? 1 : profile.titleVerified,
  }
}

const getCertificationNameFromProfile = (certType: 1 | 2 | 3, profileValues: ProfileFormValues, currentProfile?: AuthProfileVO | null) => {
  if (certType === 1) {
    return (currentProfile?.schoolVerified ? currentProfile.schoolName : profileValues.schoolName)?.trim() || ''
  }

  if (certType === 2) {
    return (currentProfile?.companyVerified ? currentProfile.companyName : profileValues.companyName)?.trim() || ''
  }

  return currentProfile?.title?.trim() || ''
}

export function ProfileEditPage() {
  const navigate = useNavigate()
  const [form] = Form.useForm<ProfileFormValues>()
  const [certificationForm] = Form.useForm<CertificationFormValues>()
  const fileInputRef = useRef<HTMLInputElement | null>(null)
  const certProofInputRef = useRef<HTMLInputElement | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [avatarSaving, setAvatarSaving] = useState(false)
  const [profile, setProfile] = useState<AuthProfileVO | null>(null)
  const [certificationDrawerOpen, setCertificationDrawerOpen] = useState(false)
  const [certificationSubmitting, setCertificationSubmitting] = useState(false)
  const [certificationUploading, setCertificationUploading] = useState(false)
  const [certificationListLoading, setCertificationListLoading] = useState(false)
  const [certificationRecords, setCertificationRecords] = useState<CertificationRecordVO[]>([])
  const [uploadedCertProofUrl, setUploadedCertProofUrl] = useState('')

  const loadCertificationRecords = async () => {
    try {
      setCertificationListLoading(true)
      const data = await listMyCertifications({ pageNum: 1, pageSize: 20 })
      const records = data.records || []
      setCertificationRecords(records)
      return records
    } catch (error) {
      console.error('load my certifications error:', error)
      setCertificationRecords([])
      message.error('认证申请列表加载失败，请稍后重试')
      return []
    } finally {
      setCertificationListLoading(false)
    }
  }

  useEffect(() => {
    const loadProfile = async () => {
      try {
        setLoading(true)
        const [profileData, certifications] = await Promise.all([
          getCurrentUserProfile(),
          loadCertificationRecords(),
        ])
        const certifiedProfileData = applyApprovedCertificationsToProfile(profileData, certifications) || profileData
        setProfile(certifiedProfileData)
        form.setFieldsValue({
          nickname: certifiedProfileData.nickname || certifiedProfileData.username || '',
          phone: certifiedProfileData.phone || '',
          gender: certifiedProfileData.gender,
          birthday: certifiedProfileData.birthday ? dayjs(certifiedProfileData.birthday) : undefined,
          signature: certifiedProfileData.signature || '',
          schoolName: certifiedProfileData.schoolName || '',
          companyName: certifiedProfileData.companyName || '',
          major: certifiedProfileData.major || '',
          grade: certifiedProfileData.grade || '',
          workYears: certifiedProfileData.workYears,
          technicalStackText: normalizeTechnicalStack(certifiedProfileData.technicalStack),
          blogUrl: certifiedProfileData.blogUrl || '',
          githubUrl: certifiedProfileData.githubUrl || '',
          wechatUrl: certifiedProfileData.wechatUrl || '',
        })
        syncStoredUser(certifiedProfileData)
      } catch (error) {
        console.error('load current user profile error:', error)
        message.error('用户资料加载失败，请稍后重试')
      } finally {
        setLoading(false)
      }
    }

    void loadProfile()
  }, [form])

  const certifiedProfile = useMemo(() => applyApprovedCertificationsToProfile(profile, certificationRecords), [certificationRecords, profile])
  const avatarSrc = useMemo(() => getAvatarUrl(certifiedProfile), [certifiedProfile])
  const certifiedSchoolName = certifiedProfile?.schoolVerified ? certifiedProfile.schoolName?.trim() || '' : ''
  const certifiedCompanyName = certifiedProfile?.companyVerified ? certifiedProfile.companyName?.trim() || '' : ''
  const certificationSummary = useMemo(() => ({
    total: certificationRecords.length,
    pending: certificationRecords.filter((item) => item.certStatus === 0).length,
    approved: certificationRecords.filter((item) => item.certStatus === 1).length,
    rejected: certificationRecords.filter((item) => item.certStatus === 2).length,
  }), [certificationRecords])

  const certificationColumns = useMemo<ColumnsType<CertificationRecordVO>>(() => [
    {
      title: '认证类型',
      dataIndex: 'certTypeName',
      key: 'certTypeName',
      width: 120,
      render: (_value, record) => record.certTypeName || getCertificationTypeName(record.certType),
    },
    {
      title: '认证名称',
      dataIndex: 'certName',
      key: 'certName',
      render: (value: string | undefined) => value || '未填写',
    },
    {
      title: '证明材料',
      dataIndex: 'certProof',
      key: 'certProof',
      width: 140,
      render: (value: string | undefined) => value ? <Image width={56} height={56} src={value} alt="证明材料" className="profile-cert-proof-thumb" /> : '暂无',
    },
    {
      title: '状态',
      dataIndex: 'certStatusName',
      key: 'certStatusName',
      width: 120,
      render: (_value, record) => (
        <span className={`profile-cert-status-badge ${certificationStatusCardClassMap[record.certStatus ?? 0] || ''}`}>
          <Tag color={certificationStatusColorMap[record.certStatus ?? 0] || 'default'}>{record.certStatusName || '待审核'}</Tag>
        </span>
      ),
    },
    {
      title: '审核备注',
      dataIndex: 'auditRemark',
      key: 'auditRemark',
      render: (value: string | null | undefined) => value || '暂无',
    },
    {
      title: '提交时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (value: string | undefined) => formatTime(value),
    },
  ], [])

  const handleSave = async (values: ProfileFormValues) => {
    const payload: UpdateAuthProfileRequest = {
      nickname: values.nickname?.trim() || undefined,
      phone: values.phone?.trim() || undefined,
      gender: values.gender,
      birthday: values.birthday ? values.birthday.format('YYYY-MM-DD') : undefined,
      signature: values.signature?.trim() || undefined,
      schoolName: (certifiedProfile?.schoolVerified ? certifiedProfile.schoolName : values.schoolName)?.trim() || undefined,
      companyName: (certifiedProfile?.companyVerified ? certifiedProfile.companyName : values.companyName)?.trim() || undefined,
      major: values.major?.trim() || undefined,
      grade: values.grade?.trim() || undefined,
      workYears: values.workYears,
      technicalStack: toTechnicalStackPayload(values.technicalStackText),
      blogUrl: values.blogUrl?.trim() || undefined,
      githubUrl: values.githubUrl?.trim() || undefined,
      wechatUrl: values.wechatUrl?.trim() || undefined,
    }

    try {
      setSaving(true)
      const data = await updateCurrentUserProfile(payload)
      const certifiedData = applyApprovedCertificationsToProfile(data, certificationRecords) || data
      setProfile((prev) => ({ ...prev, ...certifiedData }))
      syncStoredUser(certifiedData)
      message.success('用户信息更新成功')
    } catch (error) {
      console.error('update current user profile error:', error)
      message.error('用户信息更新失败，请稍后重试')
    } finally {
      setSaving(false)
    }
  }

  const handleClickAvatarUpload = () => {
    fileInputRef.current?.click()
  }

  const handleAvatarFileChange = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    event.target.value = ''

    if (!file) {
      return
    }

    try {
      setAvatarSaving(true)
      const uploadedFile = await uploadFile(file, { bizType: 'avatar', folder: 'avatar' })
      const avatarUrl = getUploadedFileUrl(uploadedFile)

      if (!avatarUrl) {
        throw new Error('上传成功，但未返回可用的头像地址')
      }

      const data = await updateCurrentUserAvatar({ avatarUrl })
      setProfile((prev) => ({ ...prev, ...data, avatarUrl }))
      syncStoredUser({ ...data, avatarUrl })
      message.success('头像更新成功')
    } catch (error) {
      console.error('update current user avatar error:', error)
      message.error('头像更新失败，请稍后重试')
    } finally {
      setAvatarSaving(false)
    }
  }

  const handleOpenCertificationDrawer = (certType: CertificationActionType = 1) => {
    const profileValues = form.getFieldsValue()
    setCertificationDrawerOpen(true)
    setUploadedCertProofUrl('')
    certificationForm.setFieldsValue({
      certType,
      certName: getCertificationNameFromProfile(certType, profileValues, certifiedProfile),
      certProof: '',
    })
  }

  const handleCertificationTypeChange = (certType: 1 | 2 | 3) => {
    const profileValues = form.getFieldsValue()
    const currentCertName = certificationForm.getFieldValue('certName')?.trim() || ''
    const nextCertName = getCertificationNameFromProfile(certType, profileValues, certifiedProfile)
    certificationForm.setFieldsValue({
      certType,
      certName: nextCertName || currentCertName,
    })
  }

  const handleClickCertProofUpload = () => {
    certProofInputRef.current?.click()
  }

  const handleCertificationProofFileChange = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    event.target.value = ''

    if (!file) {
      return
    }

    try {
      setCertificationUploading(true)
      const uploadedFile = await uploadFile(file, { bizType: 'certification', folder: 'certification' })
      const certProof = getUploadedFileUrl(uploadedFile)

      if (!certProof) {
        throw new Error('上传成功，但未返回可用的证明材料地址')
      }

      setUploadedCertProofUrl(certProof)
      certificationForm.setFieldsValue({ certProof })
      message.success('证明材料上传成功')
    } catch (error) {
      console.error('upload certification proof error:', error)
      message.error('证明材料上传失败，请稍后重试')
    } finally {
      setCertificationUploading(false)
    }
  }

  const handleSubmitCertification = async (values: CertificationFormValues) => {
    const payload: SubmitCertificationRequest = {
      certType: values.certType,
      certName: values.certName.trim(),
      certProof: values.certProof?.trim() || uploadedCertProofUrl,
    }

    if (!payload.certProof) {
      message.warning('请先上传证明材料')
      return
    }

    try {
      setCertificationSubmitting(true)
      await submitCertification(payload)
      message.success('认证申请提交成功，等待审核')
      setCertificationDrawerOpen(false)
      certificationForm.resetFields()
      setUploadedCertProofUrl('')
      const records = await loadCertificationRecords()
      setProfile((prev) => applyApprovedCertificationsToProfile(prev, records))
    } catch (error) {
      console.error('submit certification error:', error)
      message.error('认证申请提交失败，请稍后重试')
    } finally {
      setCertificationSubmitting(false)
    }
  }

  if (loading) {
    return <Card className="content-card" variant="borderless"><div className="profile-edit-loading"><Spin size="large" /></div></Card>
  }

  return (
    <>
      <Space direction="vertical" size={20} className="full-width profile-edit-page">
        <Card className="content-card profile-edit-header" variant="borderless">
          <Space direction="vertical" size={16} className="full-width">
            <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/profile')} className="profile-edit-back-btn">
              返回个人主页
            </Button>
            <div className="profile-edit-hero">
              <div>
                <div className="channel-hero__label">个人资料编辑</div>
                <h2>{certifiedProfile?.nickname || certifiedProfile?.username || '用户资料'}</h2>
                <p>在这里维护你的基础资料、学习身份和技术链接信息，也可以发起学校 / 企业 / 老师认证申请。</p>
              </div>
              <div className="profile-edit-avatar-block">
                <Avatar size={96} src={avatarSrc} />
                <input ref={fileInputRef} type="file" accept="image/*" className="profile-edit-file-input" onChange={(event) => void handleAvatarFileChange(event)} />
                <Button icon={<CameraOutlined />} loading={avatarSaving} onClick={handleClickAvatarUpload}>
                  修改头像
                </Button>
              </div>
            </div>
          </Space>
        </Card>

        <Card className="content-card profile-edit-card" variant="borderless">
          <div className="profile-cert-toolbar">
            <div>
              <div className="profile-cert-toolbar__title">认证申请</div>
              <div className="profile-cert-toolbar__desc">支持学校认证、企业认证和老师认证，提交后会进入审核流程。</div>
            </div>
            <Tooltip title="学校/企业/老师认证通过后，资料页会自动使用认证名称。">
              <Button type="primary" icon={<SafetyCertificateOutlined />} onClick={() => handleOpenCertificationDrawer(1)}>
                发起认证申请
              </Button>
            </Tooltip>
          </div>

          <Alert
            type="info"
            showIcon
            className="profile-cert-alert"
            message="上传证明材料时请点击上传图片。"
          />

          <div className="profile-cert-status-grid">
            <div className="profile-cert-status-card is-total">
              <div className="profile-cert-status-card__label">全部申请</div>
              <div className="profile-cert-status-card__value">{certificationSummary.total}</div>
            </div>
            <div className="profile-cert-status-card is-pending">
              <div className="profile-cert-status-card__label">待审核</div>
              <div className="profile-cert-status-card__value">{certificationSummary.pending}</div>
            </div>
            <div className="profile-cert-status-card is-approved">
              <div className="profile-cert-status-card__label">已通过</div>
              <div className="profile-cert-status-card__value">{certificationSummary.approved}</div>
            </div>
            <div className="profile-cert-status-card is-rejected">
              <div className="profile-cert-status-card__label">已驳回</div>
              <div className="profile-cert-status-card__value">{certificationSummary.rejected}</div>
            </div>
          </div>

          <Table
            rowKey={(record) => String(record.id ?? `${record.certType}-${record.createdAt}`)}
            columns={certificationColumns}
            dataSource={certificationRecords}
            loading={certificationListLoading}
            pagination={false}
            locale={{ emptyText: '暂未提交认证申请' }}
            className="profile-cert-table"
          />
        </Card>

        <Card className="content-card profile-edit-card" variant="borderless">
          <Form form={form} layout="vertical" onFinish={(values) => void handleSave(values)}>
            <Row gutter={[20, 0]}>
              <Col xs={24} md={12}>
                <Form.Item label="用户名">
                  <Input value={certifiedProfile?.username || ''} disabled />
                </Form.Item>
              </Col>
              <Col xs={24} md={12}>
                <Form.Item name="nickname" label="昵称" rules={[{ max: 50, message: '昵称最多 50 个字符' }]}>
                  <Input placeholder="请输入昵称" maxLength={50} />
                </Form.Item>
              </Col>
              <Col xs={24} md={12}>
                <Form.Item label="邮箱">
                  <Input value={certifiedProfile?.email || ''} disabled />
                </Form.Item>
              </Col>
              <Col xs={24} md={12}>
                <Form.Item name="phone" label="手机号" rules={[{ max: 20, message: '手机号最多 20 个字符' }]}>
                  <Input placeholder="请输入手机号" maxLength={20} />
                </Form.Item>
              </Col>
              <Col xs={24} md={12}>
                <Form.Item name="gender" label="性别">
                  <Radio.Group>
                    <Radio value={0}>女</Radio>
                    <Radio value={1}>男</Radio>
                    <Radio value={2}>保密</Radio>
                  </Radio.Group>
                </Form.Item>
              </Col>
              <Col xs={24} md={12}>
                <Form.Item name="birthday" label="生日">
                  <DatePicker className="full-width" format="YYYY-MM-DD" placeholder="请选择日期" />
                </Form.Item>
              </Col>
              <Col span={24}>
                <Form.Item name="signature" label="个性签名" rules={[{ max: 255, message: '个性签名最多 255 个字符' }]}>
                  <Input.TextArea rows={4} placeholder="请输入个性签名" maxLength={255} showCount />
                </Form.Item>
              </Col>
              <Col xs={24} md={12}>
                <Form.Item label="学校名称" extra="学校信息通过认证申请维护，认证通过后自动使用。">
                  <Input value={certifiedSchoolName || '认证通过后自动使用'} disabled placeholder="认证通过后自动使用" />
                </Form.Item>
              </Col>
              <Col xs={24} md={12}>
                <Form.Item label="学校认证状态">
                  <div className="profile-cert-inline-field">
                    <Input value={certifiedProfile?.schoolVerified ? '已认证' : '未认证'} disabled />
                    <Button onClick={() => handleOpenCertificationDrawer(1)}>{certifiedProfile?.schoolVerified ? '重新申请' : '去认证'}</Button>
                  </div>
                </Form.Item>
              </Col>
              <Col xs={24} md={12}>
                <Form.Item label="公司名称" extra="公司信息通过认证申请维护，认证通过后自动使用。">
                  <Input value={certifiedCompanyName || '认证通过后自动使用'} disabled placeholder="认证通过后自动使用" />
                </Form.Item>
              </Col>
              <Col xs={24} md={12}>
                <Form.Item label="企业认证状态">
                  <div className="profile-cert-inline-field">
                    <Input value={certifiedProfile?.companyVerified ? '已认证' : '未认证'} disabled />
                    <Button onClick={() => handleOpenCertificationDrawer(2)}>{certifiedProfile?.companyVerified ? '重新申请' : '去认证'}</Button>
                  </div>
                </Form.Item>
              </Col>
              <Col xs={24} md={12}>
                <Form.Item label="头衔" extra="头衔由系统直接发放，不需要手动认证。">
                  <Input value={certifiedProfile?.title || '系统发放'} disabled placeholder="系统发放" />
                </Form.Item>
              </Col>
              <Col xs={24} md={12}>
                <Form.Item name="major" label="专业">
                  <Input placeholder="请输入专业" />
                </Form.Item>
              </Col>
              <Col xs={24} md={12}>
                <Form.Item name="grade" label="年级 / 届别">
                  <Input placeholder="请输入年级或届别" />
                </Form.Item>
              </Col>
              <Col xs={24} md={12}>
                <Form.Item name="workYears" label="工作年限">
                  <Input type="number" min={0} placeholder="请输入工作年限" />
                </Form.Item>
              </Col>
              <Col xs={24} md={12}>
                <Form.Item name="technicalStackText" label="技术栈">
                  <Input placeholder="多个技术栈请用逗号分隔，例如 Java, Spring Boot, MySQL" />
                </Form.Item>
              </Col>
              <Col xs={24} md={12}>
                <Form.Item name="blogUrl" label="个人博客">
                  <Input placeholder="请输入博客地址" />
                </Form.Item>
              </Col>
              <Col xs={24} md={12}>
                <Form.Item name="githubUrl" label="GitHub">
                  <Input placeholder="请输入 GitHub 地址" />
                </Form.Item>
              </Col>
              <Col span={24}>
                <Form.Item name="wechatUrl" label="微信二维码地址">
                  <Input placeholder="请输入微信二维码图片地址" />
                </Form.Item>
              </Col>
            </Row>

            <div className="profile-edit-actions">
              <Button onClick={() => navigate('/profile')}>取消</Button>
              <Button type="primary" icon={<SaveOutlined />} htmlType="submit" loading={saving}>
                提交保存
              </Button>
            </div>
          </Form>
        </Card>
      </Space>

      <Drawer
        title="提交认证申请"
        open={certificationDrawerOpen}
        onClose={() => setCertificationDrawerOpen(false)}
        size={520}
        destroyOnHidden
      >
        <Form form={certificationForm} layout="vertical" initialValues={{ certType: 1 }} onFinish={(values) => void handleSubmitCertification(values)}>
          <Form.Item name="certType" label="认证类型" rules={[{ required: true, message: '请选择认证类型' }]}>
            <Select options={certificationTypeOptions} onChange={handleCertificationTypeChange} />
          </Form.Item>

          <Form.Item name="certName" label="认证名称" rules={[{ required: true, message: '请输入认证名称' }, { max: 100, message: '认证名称最多 100 个字符' }]}>
            <Input placeholder="例如：清华大学 / 腾讯 / 高校老师" maxLength={100} />
          </Form.Item>

          <Form.Item name="certProof" label="证明材料地址" hidden>
            <Input />
          </Form.Item>

          <Form.Item label="证明材料" required>
            <div className="profile-cert-upload-box">
              <input ref={certProofInputRef} type="file" accept="image/*" className="profile-edit-file-input" onChange={(event) => void handleCertificationProofFileChange(event)} />
              {uploadedCertProofUrl ? (
                <div className="profile-cert-upload-preview">
                  <Image src={uploadedCertProofUrl} alt="证明材料" className="profile-cert-upload-preview__image" />
                  <Button onClick={handleClickCertProofUpload} loading={certificationUploading}>重新上传</Button>
                </div>
              ) : (
                <Button icon={<FileImageOutlined />} loading={certificationUploading} onClick={handleClickCertProofUpload}>
                  点击上传证明材料图片
                </Button>
              )}
            </div>
          </Form.Item>

          <Alert
            type="warning"
            showIcon
            className="profile-cert-submit-tip"
            message="请上传清晰的证明材料图片。"
          />

          <div className="profile-cert-drawer-actions">
            <Button onClick={() => setCertificationDrawerOpen(false)}>取消</Button>
            <Button type="primary" htmlType="submit" loading={certificationSubmitting}>
              提交申请
            </Button>
          </div>
        </Form>
      </Drawer>
    </>
  )
}
