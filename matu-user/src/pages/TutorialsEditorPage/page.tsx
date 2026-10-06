import { ArrowLeftOutlined, CloudUploadOutlined, PlusOutlined, SaveOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Col, Form, Input, InputNumber, Row, Select, Space, Spin, Switch, message } from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { createCourse, getCourseDetail, publishCourse, updateCourse } from '../../api/course'
import { uploadFile } from '../../api/file'
import type { CreateCourseRequest } from '../../api/type/courseTypings'
import { useAppSelector } from '../../store/hooks'
import { canPublishCourse } from '../../utils/permissions'
import './page.scss'

type CourseFormValues = {
  title: string
  subtitle?: string
  description?: string
  coverUrl?: string
  price?: number
  originalPrice?: number
  level?: number
  language?: string
  isFree?: boolean
}

const levelOptions = [
  { label: '入门', value: 1 },
  { label: '基础', value: 2 },
  { label: '进阶', value: 3 },
  { label: '高级', value: 4 },
]

export function TutorialsEditorPage() {
  const navigate = useNavigate()
  const { id } = useParams()
  const userInfo = useAppSelector((state) => state.auth.userInfo)
  const canPublish = canPublishCourse(userInfo)
  const [form] = Form.useForm<CourseFormValues>()
  const [saving, setSaving] = useState(false)
  const [loadingCourse, setLoadingCourse] = useState(false)
  const [coverUploading, setCoverUploading] = useState(false)
  const [coverPreview, setCoverPreview] = useState('')
  const isEditMode = Boolean(id)

  useEffect(() => {
    if (!canPublish) {
      message.warning('你没有课程发布权限，请先完成讲师认证或使用管理员账号')
      navigate('/tutorials', { replace: true })
    }
  }, [canPublish, navigate])

  useEffect(() => {
    if (!canPublish || !id) return

    let cancelled = false
    const loadCourse = async () => {
      try {
        setLoadingCourse(true)
        const course = await getCourseDetail(id)
        if (cancelled) return

        const nextCoverUrl = course.coverUrl || ''
        setCoverPreview(nextCoverUrl)
        form.setFieldsValue({
          title: course.title || '',
          subtitle: course.subtitle || '',
          description: course.description || '',
          coverUrl: nextCoverUrl,
          price: course.price == null ? undefined : Number(course.price),
          originalPrice: course.originalPrice == null ? undefined : Number(course.originalPrice),
          level: course.level || 1,
          language: course.language || '',
          isFree: course.isFree === 1,
        })
      } catch (error) {
        console.error('load course for edit error:', error)
        message.error('课程信息加载失败，请稍后重试')
        navigate('/tutorials/manage', { replace: true })
      } finally {
        if (!cancelled) {
          setLoadingCourse(false)
        }
      }
    }

    void loadCourse()
    return () => {
      cancelled = true
    }
  }, [canPublish, form, id, navigate])

  const resolveFileUrl = (payload: { url?: string; fileUrl?: string; fullUrl?: string; downloadUrl?: string }) => {
    return payload.url || payload.fileUrl || payload.fullUrl || payload.downloadUrl || ''
  }

  const handleCoverButtonClick = () => {
    const fileInput = document.createElement('input')
    fileInput.type = 'file'
    fileInput.accept = 'image/*'
    fileInput.onchange = (event) => {
      void (async () => {
        const input = event.target as HTMLInputElement | null
        const file = input?.files?.[0]
        if (input) {
          input.value = ''
        }

        if (!file) {
          return
        }

        try {
          setCoverUploading(true)
          const uploadedFile = await uploadFile(file, { bizType: 'course-cover', folder: 'course-cover' })
          const coverUrl = resolveFileUrl(uploadedFile)

          if (!coverUrl) {
            throw new Error('上传成功，但未返回可用的封面地址')
          }

          setCoverPreview(coverUrl)
          form.setFieldValue('coverUrl', coverUrl)
          message.success('封面上传成功')
        } catch (error) {
          console.error('upload course cover error:', error)
          message.error('封面上传失败，请稍后重试')
        } finally {
          setCoverUploading(false)
        }
      })()
    }
    fileInput.click()
  }

  const handleSubmit = async (values: CourseFormValues, publish = false) => {
    const resolvedCoverUrl = values.coverUrl?.trim() || coverPreview || ''

    if (!resolvedCoverUrl) {
      message.warning('请先上传课程封面')
      return
    }

    const payload: CreateCourseRequest = {
      title: values.title.trim(),
      subtitle: values.subtitle?.trim() || undefined,
      description: values.description?.trim() || undefined,
      coverUrl: resolvedCoverUrl,
      price: values.isFree ? 0 : values.price,
      originalPrice: values.isFree ? undefined : values.originalPrice,
      level: values.level,
      language: values.language?.trim() || undefined,
      isFree: values.isFree ? 1 : 0,
    }

    try {
      setSaving(true)
      const course = isEditMode && id ? await updateCourse(id, payload) : await createCourse(payload)
      const courseId = course?.id ?? id
      if (publish && courseId != null) {
        await publishCourse(courseId)
      }
      message.success(publish ? '课程已创建并发布' : isEditMode ? '课程修改成功' : '课程创建成功')
      if (courseId != null) {
        navigate(`/detail/course/${String(courseId)}`)
      } else if (id) {
        navigate(`/detail/course/${id}`)
      } else {
        navigate('/tutorials')
      }
    } catch (error) {
      console.error(isEditMode ? 'update course error:' : 'create course error:', error)
      message.error(publish ? '发布失败，请稍后重试' : isEditMode ? '课程修改失败，请稍后重试' : '课程创建失败，请稍后重试')
    } finally {
      setSaving(false)
    }
  }

  const handleSaveAndPublish = async () => {
    let values: CourseFormValues
    try {
      values = await form.validateFields()
    } catch {
      return
    }
    await handleSubmit(values, true)
  }

  if (!canPublish) {
    return (
      <Card className="content-card tutorials-editor-denied" variant="borderless">
        <Alert type="warning" showIcon message="你没有课程发布权限" description="需要管理员账号，或通过头衔认证的讲师账号才能发布课程。" />
        <Button type="primary" onClick={() => navigate('/tutorials')} className="tutorials-editor-denied__back">
          返回课程列表
        </Button>
      </Card>
    )
  }

  return (
    <Space direction="vertical" size={20} className="full-width tutorials-editor-page">
      <Card className="content-card tutorials-editor-hero" variant="borderless">
        <Space direction="vertical" size={16} className="full-width">
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate(isEditMode && id ? `/detail/course/${id}` : '/tutorials')} className="tutorials-editor-back-btn">
            {isEditMode ? '返回课程详情' : '返回课程列表'}
          </Button>
          <div>
            <div className="channel-hero__label">{isEditMode ? '课程编辑' : '课程发布'}</div>
            <h2>{isEditMode ? '编辑课程基础信息' : '创建一门新的课程'}</h2>
            <p>{isEditMode ? '修改课程标题、封面、价格和简介等基础信息。' : '先完成基础信息创建，保存后可继续补章节、视频和图文内容。'}</p>
          </div>
        </Space>
      </Card>

      <Card className="content-card tutorials-editor-card" variant="borderless">
        {loadingCourse ? (
          <div className="tutorials-editor-loading"><Spin size="large" /></div>
        ) : (
        <Form form={form} layout="vertical" onFinish={(values) => void handleSubmit(values)} initialValues={{ level: 1, isFree: true }}>
          <Row gutter={[20, 0]}>
            <Col xs={24} md={12}>
              <Form.Item name="title" label="课程标题" rules={[{ required: true, message: '请输入课程标题' }, { max: 100, message: '课程标题最多 100 个字符' }]}>
                <Input placeholder="请输入课程标题" maxLength={100} />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item name="subtitle" label="副标题" rules={[{ max: 120, message: '副标题最多 120 个字符' }]}>
                <Input placeholder="请输入课程副标题" maxLength={120} />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item name="level" label="难度">
                <Select options={levelOptions} />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item name="language" label="授课语言" rules={[{ max: 20, message: '授课语言最多 20 个字符' }]}>
                <Input placeholder="例如：中文 / English" maxLength={20} />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="description" label="课程简介" rules={[{ max: 1000, message: '课程简介最多 1000 个字符' }]}>
                <Input.TextArea rows={5} placeholder="请输入课程简介" maxLength={1000} showCount />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item name="coverUrl" label="课程封面" rules={[{ required: true, message: '请先上传课程封面' }]}>
                <div className="tutorials-cover-upload">
                  {coverPreview ? (
                    <button type="button" className="tutorials-cover-upload__preview" onClick={handleCoverButtonClick} aria-label="重新上传课程封面">
                      <img src={coverPreview} alt="课程封面预览" />
                    </button>
                  ) : (
                    <button
                      type="button"
                      className="tutorials-cover-upload__trigger"
                      onClick={handleCoverButtonClick}
                      disabled={coverUploading}
                      aria-label={coverUploading ? '封面上传中' : '上传课程封面'}
                    >
                      <PlusOutlined />
                    </button>
                  )}
                </div>
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item name="isFree" label="是否免费" valuePropName="checked">
                <Switch checkedChildren="免费" unCheckedChildren="付费" />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item shouldUpdate noStyle>
                {({ getFieldValue }) => (getFieldValue('isFree') ? null : (
                  <Form.Item name="price" label="课程价格" rules={[{ required: true, message: '请输入课程价格' }]}>
                    <InputNumber min={0} precision={2} className="full-width" placeholder="请输入课程价格" />
                  </Form.Item>
                ))}
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item shouldUpdate noStyle>
                {({ getFieldValue }) => (getFieldValue('isFree') ? null : (
                  <Form.Item name="originalPrice" label="划线价">
                    <InputNumber min={0} precision={2} className="full-width" placeholder="请输入原价" />
                  </Form.Item>
                ))}
              </Form.Item>
            </Col>
          </Row>

          <div className="tutorials-editor-actions">
            <Button onClick={() => navigate(isEditMode && id ? `/detail/course/${id}` : '/tutorials')}>取消</Button>
            <Button icon={<SaveOutlined />} htmlType="submit" loading={saving || coverUploading}>
              {isEditMode ? '保存修改' : '创建课程'}
            </Button>
            <Button type="primary" icon={<CloudUploadOutlined />} onClick={() => void handleSaveAndPublish()} loading={saving || coverUploading}>
              保存并发布
            </Button>
          </div>
        </Form>
        )}
      </Card>
    </Space>
  )
}
