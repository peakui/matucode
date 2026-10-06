import {
  ArrowLeftOutlined,
  CheckOutlined,
  CloseOutlined,
  DeleteOutlined,
  EditOutlined,
  EyeOutlined,
  LinkOutlined,
  PlusOutlined,
  ReloadOutlined,
  TeamOutlined,
  TrophyOutlined,
  UserSwitchOutlined,
} from '@ant-design/icons'
import { Alert, Button, Card, Empty, Form, Input, InputNumber, Modal, Select, Space, Spin, Table, Tabs, Tag, message } from 'antd'
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table'
import axios from 'axios'
import { useCallback, useEffect, useMemo, useState } from 'react'
import type { Key } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  approveOjClassMember,
  associateOjClassProblems,
  createOjClassProblem,
  createOjProblemTestCase,
  getOjClassProblemDetail,
  listOjClassMembers,
  listOjClassProblems,
  listOjClasses,
  listOjProblemTestCases,
  listOjProblems,
  rejectOjClassMember,
  removeOjClassProblem,
  updateOjClassProblem,
} from '../../../api/oj'
import type {
  ClassMemberVO,
  ClassVO,
  CreateOjProblemRequest,
  CreateOjTestCaseRequest,
  ListOjClassProblemsParams,
  OjPageResponse,
  OjProblemVO,
  OjTestCaseVO,
} from '../../../api/type/ojTypings'
import { useAppSelector } from '../../../store/hooks'
import { canPublishCourse } from '../../../utils/permissions'
import './page.scss'

const DEFAULT_PAGE_SIZE = 10
const DEFAULT_PROBLEM_VALUES = {
  difficulty: 1,
  timeLimit: 1000,
  memoryLimit: 256,
  status: 1,
}

type ResourceTypeFilter = 'all' | '1' | '2'
type ProblemModalMode = 'create' | 'edit'
type TestCaseFormItem = CreateOjTestCaseRequest & { key?: string }
type ProblemFormValues = CreateOjProblemRequest & { testCases?: TestCaseFormItem[] }

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

const getDifficultyInfo = (difficulty?: number) => {
  if (difficulty === 1) return { label: '简单', color: 'green' }
  if (difficulty === 2) return { label: '中等', color: 'gold' }
  if (difficulty === 3) return { label: '困难', color: 'red' }
  return { label: '未设置', color: 'default' }
}

const getStatusInfo = (status?: number) => {
  if (status === 0) return { label: '隐藏', color: 'default' }
  if (status === 2) return { label: '下架', color: 'red' }
  return { label: '发布', color: 'green' }
}

const getMemberStatusInfo = (status?: number) => {
  if (status === 0) return { label: '待审核', color: 'gold' }
  if (status === 1) return { label: '已通过', color: 'green' }
  if (status === 2) return { label: '已拒绝', color: 'red' }
  if (status === 3) return { label: '已移出', color: 'default' }
  return { label: '未知', color: 'default' }
}

const getMemberDisplayName = (member: ClassMemberVO) => (
  member.nickname?.trim()
  || member.userName?.trim()
  || member.username?.trim()
  || `用户 ${String(member.userId ?? '-')}`
)

const getMemberApplyMessage = (member: ClassMemberVO) => (
  member.applyMessage?.trim()
  || member.message?.trim()
  || '暂无申请说明'
)

const getMemberReviewStatus = (member: ClassMemberVO, reviewedStatus?: number) => reviewedStatus ?? member.joinStatus ?? member.status

const getMemberApplyTime = (member: ClassMemberVO) => member.joinedAt || member.createdAt

const getResourceInfo = (resource?: ClassVO) => {
  if (resource?.type === 2) return { label: '竞赛', color: 'red', icon: <TrophyOutlined /> }
  return { label: '普通班级', color: 'blue', icon: <TeamOutlined /> }
}

const formatTime = (value?: string) => {
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

const normalizeClassId = (value?: string | number) => String(value ?? '').trim()

const normalizeProblemPayload = (values: ProblemFormValues): CreateOjProblemRequest => ({
  problemNo: values.problemNo?.trim(),
  title: values.title?.trim(),
  description: values.description?.trim(),
  inputFormat: values.inputFormat?.trim(),
  outputFormat: values.outputFormat?.trim(),
  sampleInput: values.sampleInput,
  sampleOutput: values.sampleOutput,
  hint: values.hint?.trim(),
  difficulty: Number(values.difficulty || 1),
  categoryId: values.categoryId ? String(values.categoryId).trim() : undefined,
  timeLimit: Number(values.timeLimit || 1000),
  memoryLimit: Number(values.memoryLimit || 256),
  status: Number(values.status ?? 1),
})

const normalizeTestCasePayloads = (testCases?: TestCaseFormItem[]): CreateOjTestCaseRequest[] => (
  testCases || []
).map((item, index) => ({
  caseNo: Number(item.caseNo || index + 1),
  input: item.input ?? '',
  expectedOutput: item.expectedOutput ?? '',
  isSample: Number(item.isSample ?? 0),
  scoreWeight: Number(item.scoreWeight ?? 1),
  isHidden: Number(item.isHidden ?? 0),
})).filter((item) => item.input.trim() || item.expectedOutput.trim())

const buildProblemFormValues = (problem: OjProblemVO, testCases: OjTestCaseVO[] = []): ProblemFormValues => ({
  problemNo: problem.problemNo || '',
  title: problem.title || '',
  description: problem.description || '',
  inputFormat: problem.inputFormat || '',
  outputFormat: problem.outputFormat || '',
  sampleInput: problem.sampleInput || '',
  sampleOutput: problem.sampleOutput || '',
  hint: problem.hint || '',
  difficulty: problem.difficulty || 1,
  categoryId: problem.categoryId ?? undefined,
  timeLimit: problem.timeLimit || 1000,
  memoryLimit: problem.memoryLimit || 256,
  status: problem.status ?? 1,
  testCases: testCases.length ? testCases.map((item, index) => ({
    key: String(item.id ?? index),
    caseNo: item.caseNo || index + 1,
    input: item.input || '',
    expectedOutput: item.expectedOutput || '',
    isSample: item.isSample ?? 0,
    scoreWeight: item.scoreWeight ?? 1,
    isHidden: item.isHidden ?? 0,
  })) : [{ caseNo: 1, input: '', expectedOutput: '', isSample: 1, scoreWeight: 1, isHidden: 0 }],
})

export function ClassManagePage() {
  const navigate = useNavigate()
  const userInfo = useAppSelector((state) => state.auth.userInfo)
  const canManage = canPublishCourse(userInfo)
  const [problemForm] = Form.useForm<ProblemFormValues>()
  const [resources, setResources] = useState<ClassVO[]>([])
  const [resourceLoading, setResourceLoading] = useState(true)
  const [resourceError, setResourceError] = useState('')
  const [resourceKeyword, setResourceKeyword] = useState('')
  const [resourceType, setResourceType] = useState<ResourceTypeFilter>('1')
  const [selectedResource, setSelectedResource] = useState<ClassVO | null>(null)
  const [detailOpen, setDetailOpen] = useState(false)
  const [problemPage, setProblemPage] = useState<OjPageResponse<OjProblemVO>>({ records: [], total: 0, pageNum: 1, pageSize: DEFAULT_PAGE_SIZE, totalPages: 0 })
  const [problemQuery, setProblemQuery] = useState<ListOjClassProblemsParams>({ pageNum: 1, pageSize: DEFAULT_PAGE_SIZE })
  const [problemLoading, setProblemLoading] = useState(false)
  const [problemModalOpen, setProblemModalOpen] = useState(false)
  const [problemModalMode, setProblemModalMode] = useState<ProblemModalMode>('create')
  const [editingProblem, setEditingProblem] = useState<OjProblemVO | null>(null)
  const [problemSaving, setProblemSaving] = useState(false)
  const [associationOpen, setAssociationOpen] = useState(false)
  const [globalProblemPage, setGlobalProblemPage] = useState<OjPageResponse<OjProblemVO>>({ records: [], total: 0, pageNum: 1, pageSize: DEFAULT_PAGE_SIZE, totalPages: 0 })
  const [globalProblemQuery, setGlobalProblemQuery] = useState<ListOjClassProblemsParams>({ pageNum: 1, pageSize: DEFAULT_PAGE_SIZE, status: 1 })
  const [globalProblemLoading, setGlobalProblemLoading] = useState(false)
  const [selectedProblemIds, setSelectedProblemIds] = useState<Key[]>([])
  const [associating, setAssociating] = useState(false)
  const [members, setMembers] = useState<ClassMemberVO[]>([])
  const [memberLoading, setMemberLoading] = useState(false)
  const [reviewingMemberId, setReviewingMemberId] = useState<string>()
  const [reviewedMemberStatusMap, setReviewedMemberStatusMap] = useState<Partial<Record<string, 1 | 2>>>({})

  const loadResources = useCallback(async () => {
    try {
      setResourceLoading(true)
      setResourceError('')
      const data = await listOjClasses({ pageNum: 1, pageSize: 100 })
      setResources(data.records || [])
    } catch (error) {
      console.error('load class manage resources error:', error)
      setResourceError(getAxiosErrorMessage(error, '班级管理列表加载失败，请稍后重试'))
      setResources([])
    } finally {
      setResourceLoading(false)
    }
  }, [])

  const loadResourceProblems = useCallback(async (resourceId: string | number, nextQuery?: ListOjClassProblemsParams) => {
    const query = { ...problemQuery, ...nextQuery }
    try {
      setProblemLoading(true)
      const data = await listOjClassProblems(resourceId, query)
      setProblemPage(data)
      setProblemQuery(query)
    } catch (error) {
      console.error('load class problems error:', error)
      message.error(getAxiosErrorMessage(error, '关联题目加载失败'))
      setProblemPage({ records: [], total: 0, pageNum: query.pageNum || 1, pageSize: query.pageSize || DEFAULT_PAGE_SIZE, totalPages: 0 })
    } finally {
      setProblemLoading(false)
    }
  }, [problemQuery])

  const loadResourceMembers = useCallback(async (resourceId: string | number) => {
    try {
      setMemberLoading(true)
      const data = await listOjClassMembers(resourceId)
      setMembers(data || [])
    } catch (error) {
      console.error('load class members error:', error)
      message.error(getAxiosErrorMessage(error, '成员申请加载失败'))
      setMembers([])
    } finally {
      setMemberLoading(false)
    }
  }, [])

  const loadGlobalProblems = useCallback(async (nextQuery?: ListOjClassProblemsParams) => {
    const query = { ...globalProblemQuery, ...nextQuery }
    try {
      setGlobalProblemLoading(true)
      const data = await listOjProblems(query)
      setGlobalProblemPage(data)
      setGlobalProblemQuery(query)
    } catch (error) {
      console.error('load global problems error:', error)
      message.error(getAxiosErrorMessage(error, '全局题库加载失败'))
      setGlobalProblemPage({ records: [], total: 0, pageNum: query.pageNum || 1, pageSize: query.pageSize || DEFAULT_PAGE_SIZE, totalPages: 0 })
    } finally {
      setGlobalProblemLoading(false)
    }
  }, [globalProblemQuery])

  useEffect(() => {
    if (!canManage) {
      setResourceLoading(false)
      return
    }

    void loadResources()
  }, [canManage, loadResources])

  const filteredResources = useMemo(() => {
    const keyword = resourceKeyword.trim().toLowerCase()
    return resources.filter((item) => {
      const matchType = resourceType === 'all' || String(item.type ?? 1) === resourceType
      const matchKeyword = !keyword || [item.id, item.name, item.description].some((value) => String(value ?? '').toLowerCase().includes(keyword))
      return matchType && matchKeyword
    })
  }, [resourceKeyword, resourceType, resources])

  const summary = useMemo(() => ({
    total: resources.length,
    classes: resources.filter((item) => item.type === 1 || item.type == null).length,
    contests: resources.filter((item) => item.type === 2).length,
    active: resources.filter((item) => (item.status ?? 1) === 1).length,
  }), [resources])

  const openDetail = (resource: ClassVO) => {
    const resourceId = normalizeClassId(resource.id)
    if (!resourceId) {
      message.warning('资源 ID 无效')
      return
    }

    setSelectedResource(resource)
    setDetailOpen(true)
    setProblemQuery({ pageNum: 1, pageSize: DEFAULT_PAGE_SIZE })
    void loadResourceProblems(resourceId, { pageNum: 1, pageSize: DEFAULT_PAGE_SIZE })
    void loadResourceMembers(resourceId)
  }

  const openCreateProblem = () => {
    setProblemModalMode('create')
    setEditingProblem(null)
    problemForm.setFieldsValue({
      ...DEFAULT_PROBLEM_VALUES,
      problemNo: '',
      title: '',
      description: '',
      inputFormat: '',
      outputFormat: '',
      sampleInput: '',
      sampleOutput: '',
      hint: '',
      testCases: [{ caseNo: 1, input: '', expectedOutput: '', isSample: 1, scoreWeight: 1, isHidden: 0 }],
    })
    setProblemModalOpen(true)
  }

  const openEditProblem = async (problem: OjProblemVO) => {
    const resourceId = normalizeClassId(selectedResource?.id)
    const problemId = normalizeClassId(problem.id)
    if (!resourceId || !problemId) return

    try {
      setProblemModalMode('edit')
      setEditingProblem(problem)
      setProblemModalOpen(true)
      setProblemSaving(true)
      const [detail, testCases] = await Promise.all([
        getOjClassProblemDetail(resourceId, problemId),
        listOjProblemTestCases(problemId),
      ])
      problemForm.setFieldsValue(buildProblemFormValues(detail, testCases))
      setEditingProblem(detail)
    } catch (error) {
      console.error('load problem edit detail error:', error)
      message.error(getAxiosErrorMessage(error, '题目详情加载失败'))
      setProblemModalOpen(false)
    } finally {
      setProblemSaving(false)
    }
  }

  const handleSaveProblem = async () => {
    const resourceId = normalizeClassId(selectedResource?.id)
    if (!resourceId) return

    try {
      const values = await problemForm.validateFields()
      const payload = normalizeProblemPayload(values)
      const testCases = normalizeTestCasePayloads(values.testCases)
      setProblemSaving(true)

      if (problemModalMode === 'create') {
        const created = await createOjClassProblem(resourceId, payload)
        const createdProblemId = normalizeClassId(created.id)
        if (createdProblemId) {
          await Promise.all(testCases.map((item) => createOjProblemTestCase(createdProblemId, item)))
        }
        message.success('题目新增成功')
      } else {
        const problemId = normalizeClassId(editingProblem?.id)
        if (!problemId) return
        await updateOjClassProblem(resourceId, problemId, payload)
        message.success('题目更新成功')
      }

      setProblemModalOpen(false)
      await loadResourceProblems(resourceId, { ...problemQuery })
    } catch (error) {
      if (error && typeof error === 'object' && 'errorFields' in error) {
        return
      }
      console.error('save class problem error:', error)
      message.error(getAxiosErrorMessage(error, '题目保存失败'))
    } finally {
      setProblemSaving(false)
    }
  }

  const handleRemoveProblem = async (problem: OjProblemVO) => {
    const resourceId = normalizeClassId(selectedResource?.id)
    const problemId = normalizeClassId(problem.id)
    if (!resourceId || !problemId) return

    Modal.confirm({
      title: '确认移除这道题目？',
      content: `将从当前${selectedResource?.type === 2 ? '竞赛' : '班级'}中解除关联：${problem.title || problem.problemNo || problemId}`,
      okText: '确认移除',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await removeOjClassProblem(resourceId, problemId)
          message.success('题目已移除')
          await loadResourceProblems(resourceId, { ...problemQuery })
        } catch (error) {
          console.error('remove class problem error:', error)
          message.error(getAxiosErrorMessage(error, '题目移除失败'))
        }
      },
    })
  }

  const openAssociation = () => {
    setSelectedProblemIds([])
    setAssociationOpen(true)
    void loadGlobalProblems({ pageNum: 1, pageSize: DEFAULT_PAGE_SIZE, status: 1 })
  }

  const handleAssociateProblems = async () => {
    const resourceId = normalizeClassId(selectedResource?.id)
    if (!resourceId) return
    if (!selectedProblemIds.length) {
      message.warning('请选择要关联的题目')
      return
    }

    try {
      setAssociating(true)
      await associateOjClassProblems(resourceId, { problemIds: selectedProblemIds.map(String) })
      message.success('题目关联成功')
      setAssociationOpen(false)
      await loadResourceProblems(resourceId, { pageNum: 1, pageSize: problemQuery.pageSize || DEFAULT_PAGE_SIZE })
    } catch (error) {
      console.error('associate class problems error:', error)
      message.error(getAxiosErrorMessage(error, '题目关联失败'))
    } finally {
      setAssociating(false)
    }
  }

  const handleReviewMember = async (member: ClassMemberVO, action: 'approve' | 'reject') => {
    const resourceId = normalizeClassId(selectedResource?.id)
    const memberId = normalizeClassId(member.id)
    if (!resourceId || !memberId) {
      message.warning('申请记录参数无效')
      return
    }

    const isApprove = action === 'approve'
    Modal.confirm({
      title: isApprove ? '确认通过该申请？' : '确认拒绝该申请？',
      content: `${getMemberDisplayName(member)} · ${getMemberApplyMessage(member)}`,
      okText: isApprove ? '通过申请' : '拒绝申请',
      cancelText: '取消',
      okButtonProps: { danger: !isApprove },
      onOk: async () => {
        try {
          setReviewingMemberId(memberId)
          if (isApprove) {
            await approveOjClassMember(resourceId, memberId, { remark: '同意加入' })
            setReviewedMemberStatusMap((prev) => ({ ...prev, [memberId]: 1 }))
            setMembers((prev) => prev.map((item) => normalizeClassId(item.id) === memberId ? { ...item, joinStatus: 1, status: 1 } : item))
            message.success('已通过申请')
          } else {
            await rejectOjClassMember(resourceId, memberId, { reason: '管理员拒绝加入' })
            setReviewedMemberStatusMap((prev) => ({ ...prev, [memberId]: 2 }))
            setMembers((prev) => prev.map((item) => normalizeClassId(item.id) === memberId ? { ...item, joinStatus: 2, status: 2 } : item))
            message.success('已拒绝申请')
          }
          await loadResourceMembers(resourceId)
          await loadResources()
        } catch (error) {
          console.error('review class member error:', error)
          message.error(getAxiosErrorMessage(error, isApprove ? '通过申请失败' : '拒绝申请失败'))
        } finally {
          setReviewingMemberId(undefined)
        }
      },
    })
  }

  const resourceColumns = useMemo<ColumnsType<ClassVO>>(() => [
    {
      title: '资源',
      key: 'resource',
      render: (_value, resource) => {
        const info = getResourceInfo(resource)
        return (
          <div className="class-manage-resource-cell">
            <strong>{resource.name || '未命名资源'}</strong>
            <span>{resource.description || '暂无描述'}</span>
            <Space wrap size={6}>
              <Tag color={info.color} icon={info.icon}>{info.label}</Tag>
              <Tag>ID {String(resource.id ?? '-')}</Tag>
            </Space>
          </div>
        )
      },
    },
    {
      title: '成员',
      dataIndex: 'memberCount',
      width: 100,
      render: (value: number | undefined) => value ?? 0,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 110,
      render: (value: number | undefined) => {
        const status = getStatusInfo(value)
        return <Tag color={status.color}>{status.label}</Tag>
      },
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      width: 180,
      render: (value: string | undefined) => formatTime(value),
    },
    {
      title: '操作',
      key: 'action',
      width: 170,
      render: (_value, resource) => (
        <Space>
          <Button type="primary" icon={<EyeOutlined />} onClick={() => openDetail(resource)}>
            管理详情
          </Button>
        </Space>
      ),
    },
  ], [])

  const problemColumns = useMemo<ColumnsType<OjProblemVO>>(() => [
    {
      title: '题目',
      key: 'problem',
      render: (_value, problem) => (
        <div className="class-manage-problem-cell">
          <strong>{problem.problemNo || '-'} · {problem.title || '未命名题目'}</strong>
          <span>{problem.description || '暂无描述'}</span>
        </div>
      ),
    },
    {
      title: '难度',
      dataIndex: 'difficulty',
      width: 100,
      render: (value: number | undefined) => {
        const difficulty = getDifficultyInfo(value)
        return <Tag color={difficulty.color}>{difficulty.label}</Tag>
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (value: number | undefined) => {
        const status = getStatusInfo(value)
        return <Tag color={status.color}>{status.label}</Tag>
      },
    },
    {
      title: '通过率',
      key: 'acceptRate',
      width: 120,
      render: (_value, problem) => `${problem.acceptRate ?? 0}%`,
    },
    {
      title: '限制',
      key: 'limit',
      width: 160,
      render: (_value, problem) => `${problem.timeLimit ?? 1000}ms / ${problem.memoryLimit ?? 256}MB`,
    },
    {
      title: '操作',
      key: 'action',
      width: 190,
      render: (_value, problem) => (
        <Space>
          <Button icon={<EditOutlined />} onClick={() => void openEditProblem(problem)}>编辑</Button>
          <Button danger icon={<DeleteOutlined />} onClick={() => void handleRemoveProblem(problem)}>移除</Button>
        </Space>
      ),
    },
  ], [selectedResource, problemQuery, loadResourceProblems])

  const memberColumns = useMemo<ColumnsType<ClassMemberVO>>(() => [
    {
      title: '申请用户',
      key: 'user',
      render: (_value, member) => (
        <div className="class-manage-member-cell">
          <strong>{getMemberDisplayName(member)}</strong>
          <span>用户ID {String(member.userId ?? '-')}</span>
        </div>
      ),
    },
    {
      title: '申请说明',
      key: 'message',
      render: (_value, member) => getMemberApplyMessage(member),
    },
    {
      title: '状态',
      dataIndex: 'joinStatus',
      width: 110,
      render: (_value: number | undefined, member) => {
        const memberId = normalizeClassId(member.id)
        const status = getMemberStatusInfo(getMemberReviewStatus(member, reviewedMemberStatusMap[memberId]))
        return <Tag color={status.color}>{status.label}</Tag>
      },
    },
    {
      title: '申请时间',
      dataIndex: 'joinedAt',
      width: 180,
      render: (_value: string | undefined, member) => formatTime(getMemberApplyTime(member)),
    },
    {
      title: '操作',
      key: 'action',
      width: 180,
      render: (_value, member) => {
        const memberId = normalizeClassId(member.id)
        const effectiveStatus = getMemberReviewStatus(member, reviewedMemberStatusMap[memberId])
        const isPending = effectiveStatus === 0 || effectiveStatus == null
        const loading = reviewingMemberId === memberId
        return isPending ? (
          <Space>
            <Button type="primary" icon={<CheckOutlined />} loading={loading} onClick={() => void handleReviewMember(member, 'approve')}>通过</Button>
            <Button danger icon={<CloseOutlined />} loading={loading} onClick={() => void handleReviewMember(member, 'reject')}>拒绝</Button>
          </Space>
        ) : <Tag color="green">已审批</Tag>
      },
    },
  ], [reviewingMemberId, reviewedMemberStatusMap, selectedResource, loadResourceMembers, loadResources])

  const pendingMemberReviewCount = useMemo(() => members.filter((member) => {
    const memberId = normalizeClassId(member.id)
    const effectiveStatus = getMemberReviewStatus(member, reviewedMemberStatusMap[memberId])
    return effectiveStatus === 0 || effectiveStatus == null
  }).length, [members, reviewedMemberStatusMap])

  const globalProblemColumns = useMemo<ColumnsType<OjProblemVO>>(() => problemColumns.filter((column) => column.key !== 'action'), [problemColumns])

  if (!canManage) {
    return (
      <Space direction="vertical" size={20} className="full-width class-manage-page">
        <Card className="content-card class-manage-denied" variant="borderless">
          <div className="class-manage-denied__icon"><UserSwitchOutlined /></div>
          <h2>暂时没有班级管理权限</h2>
          <p>需要教师、助教或管理员权限后，才能管理班级、竞赛和关联题目。</p>
          <Space wrap className="class-manage-denied__actions">
            <Button type="primary" icon={<UserSwitchOutlined />} onClick={() => navigate('/profile/edit')}>申请权限</Button>
            <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/classes')}>返回班级列表</Button>
          </Space>
        </Card>
      </Space>
    )
  }

  return (
    <Space direction="vertical" size={20} className="full-width class-manage-page">
      <Card className="content-card class-manage-hero" variant="borderless">
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/classes')} className="class-manage-back-btn">
          返回班级列表
        </Button>
        <div className="channel-hero__label">班级管理</div>
        <h2>管理班级、竞赛和关联题目</h2>
        <p>进入资源详情后，可以查看当前题目、新增专属题目、编辑题目基础信息、关联全局题库题目或移除关联。</p>
      </Card>

      <Card className="content-card class-manage-card" variant="borderless">
        <div className="class-manage-toolbar">
          <div className="class-manage-summary-grid">
            <div className="summary-tile"><strong>{summary.total}</strong><span>全部资源</span></div>
            <div className="summary-tile"><strong>{summary.classes}</strong><span>普通班级</span></div>
            <div className="summary-tile"><strong>{summary.contests}</strong><span>竞赛</span></div>
            <div className="summary-tile"><strong>{summary.active}</strong><span>发布中</span></div>
          </div>
          <Space wrap>
            <Input.Search
              allowClear
              placeholder="搜索资源 ID、名称或描述"
              value={resourceKeyword}
              onChange={(event) => setResourceKeyword(event.target.value)}
              className="class-manage-search"
            />
            <Select<ResourceTypeFilter>
              value={resourceType}
              onChange={setResourceType}
              options={[
                { label: '普通班级', value: '1' },
                { label: '竞赛', value: '2' },
                { label: '全部资源', value: 'all' },
              ]}
              className="class-manage-type-select"
            />
            <Button icon={<ReloadOutlined />} loading={resourceLoading} onClick={() => void loadResources()}>刷新</Button>
          </Space>
        </div>

        {resourceLoading ? (
          <div className="class-manage-loading"><Spin size="large" /></div>
        ) : resourceError ? (
          <Alert type="error" showIcon message={resourceError} />
        ) : filteredResources.length ? (
          <Table<ClassVO>
            rowKey={(resource) => normalizeClassId(resource.id) || resource.name || 'resource'}
            columns={resourceColumns}
            dataSource={filteredResources}
            pagination={{ pageSize: DEFAULT_PAGE_SIZE }}
            className="class-manage-table"
          />
        ) : (
          <Empty description="暂无可管理资源" />
        )}
      </Card>

      <Modal
        title={selectedResource?.type === 2 ? '竞赛详情管理' : '班级详情管理'}
        open={detailOpen}
        onCancel={() => setDetailOpen(false)}
        footer={null}
        width={1080}
        destroyOnHidden
      >
        <Space direction="vertical" size={16} className="full-width class-manage-detail">
          <div className="class-manage-detail__header">
            <div>
              <h3>{selectedResource?.name || '未命名资源'}</h3>
              <p>{selectedResource?.description || '暂无描述'}</p>
            </div>
            <Space wrap>
              <Tag color={getResourceInfo(selectedResource || undefined).color} icon={getResourceInfo(selectedResource || undefined).icon}>
                {getResourceInfo(selectedResource || undefined).label}
              </Tag>
              <Tag>成员 {selectedResource?.memberCount ?? 0}</Tag>
              <Tag>ID {String(selectedResource?.id ?? '-')}</Tag>
            </Space>
          </div>

          <Tabs
            defaultActiveKey="problems"
            items={[
              {
                key: 'basic',
                label: '基础信息',
                children: (
                  <div className="class-manage-basic-grid">
                    <div><span>资源名称</span><strong>{selectedResource?.name || '暂无'}</strong></div>
                    <div><span>资源类型</span><strong>{getResourceInfo(selectedResource || undefined).label}</strong></div>
                    <div><span>成员数量</span><strong>{selectedResource?.memberCount ?? 0}</strong></div>
                    <div><span>加入方式</span><strong>{selectedResource?.joinMode ?? '暂无'}</strong></div>
                    <div><span>邀请码</span><strong>{selectedResource?.inviteCode || '暂无'}</strong></div>
                    <div><span>创建时间</span><strong>{formatTime(selectedResource?.createdAt)}</strong></div>
                  </div>
                ),
              },
              {
                key: 'approvals',
                label: `审核审批${pendingMemberReviewCount ? ` (${pendingMemberReviewCount})` : ''}`,
                children: (
                  <Space direction="vertical" size={14} className="full-width">
                    <div className="class-manage-problem-toolbar">
                      <div>
                        <strong>加入申请</strong>
                        <p className="class-manage-toolbar-desc">处理用户加入当前{selectedResource?.type === 2 ? '竞赛' : '班级'}的申请。</p>
                      </div>
                      <Button icon={<ReloadOutlined />} loading={memberLoading} onClick={() => selectedResource?.id != null && void loadResourceMembers(selectedResource.id)}>刷新</Button>
                    </div>
                    <Table<ClassMemberVO>
                      rowKey={(member) => normalizeClassId(member.id) || `${String(member.userId ?? '')}-${String(member.createdAt ?? '')}`}
                      columns={memberColumns}
                      dataSource={members}
                      loading={memberLoading}
                      pagination={{ pageSize: DEFAULT_PAGE_SIZE }}
                      locale={{ emptyText: <Empty description="暂无待处理申请" /> }}
                    />
                  </Space>
                ),
              },
              {
                key: 'problems',
                label: '关联题目',
                children: (
                  <Space direction="vertical" size={14} className="full-width">
                    <div className="class-manage-problem-toolbar">
                      <Space wrap>
                        <Input.Search
                          allowClear
                          placeholder="搜索题号或题目标题"
                          onSearch={(value) => selectedResource?.id != null && void loadResourceProblems(selectedResource.id, { keyword: value.trim(), pageNum: 1 })}
                          className="class-manage-search"
                        />
                        <Select
                          allowClear
                          placeholder="难度"
                          className="class-manage-filter-select"
                          onChange={(value?: number) => selectedResource?.id != null && void loadResourceProblems(selectedResource.id, { difficulty: value, pageNum: 1 })}
                          options={[{ label: '简单', value: 1 }, { label: '中等', value: 2 }, { label: '困难', value: 3 }]}
                        />
                        <Select
                          allowClear
                          placeholder="状态"
                          className="class-manage-filter-select"
                          onChange={(value?: number) => selectedResource?.id != null && void loadResourceProblems(selectedResource.id, { status: value, pageNum: 1 })}
                          options={[{ label: '隐藏', value: 0 }, { label: '发布', value: 1 }, { label: '下架', value: 2 }]}
                        />
                      </Space>
                      <Space wrap>
                        <Button icon={<ReloadOutlined />} loading={problemLoading} onClick={() => selectedResource?.id != null && void loadResourceProblems(selectedResource.id, { ...problemQuery })}>刷新</Button>
                        <Button icon={<LinkOutlined />} onClick={openAssociation}>关联已有题目</Button>
                        <Button type="primary" icon={<PlusOutlined />} onClick={openCreateProblem}>新增题目</Button>
                      </Space>
                    </div>
                    <Table<OjProblemVO>
                      rowKey={(problem) => normalizeClassId(problem.id) || problem.problemNo || 'problem'}
                      columns={problemColumns}
                      dataSource={problemPage.records || []}
                      loading={problemLoading}
                      pagination={{
                        current: problemPage.pageNum || 1,
                        pageSize: problemPage.pageSize || DEFAULT_PAGE_SIZE,
                        total: problemPage.total || 0,
                        showSizeChanger: true,
                      }}
                      onChange={(pagination: TablePaginationConfig) => selectedResource?.id != null && void loadResourceProblems(selectedResource.id, {
                        pageNum: pagination.current || 1,
                        pageSize: pagination.pageSize || DEFAULT_PAGE_SIZE,
                      })}
                    />
                  </Space>
                ),
              },
            ]}
          />
        </Space>
      </Modal>

      <Modal
        title={problemModalMode === 'create' ? '新增题目' : '编辑题目'}
        open={problemModalOpen}
        onOk={() => void handleSaveProblem()}
        onCancel={() => setProblemModalOpen(false)}
        confirmLoading={problemSaving}
        okText="保存"
        cancelText="取消"
        width={900}
        destroyOnHidden
      >
        <Spin spinning={problemSaving && problemModalMode === 'edit'}>
          <Form<ProblemFormValues> form={problemForm} layout="vertical" className="class-manage-problem-form">
            <div className="class-manage-form-grid">
              <Form.Item name="problemNo" label="题目编号" rules={[{ required: true, message: '请输入题目编号' }]}><Input placeholder="例如 A1001" /></Form.Item>
              <Form.Item name="title" label="题目标题" rules={[{ required: true, message: '请输入题目标题' }]}><Input placeholder="请输入题目标题" /></Form.Item>
              <Form.Item name="difficulty" label="难度" rules={[{ required: true, message: '请选择难度' }]}><Select options={[{ label: '简单', value: 1 }, { label: '中等', value: 2 }, { label: '困难', value: 3 }]} /></Form.Item>
              <Form.Item name="status" label="状态"><Select options={[{ label: '隐藏', value: 0 }, { label: '发布', value: 1 }, { label: '下架', value: 2 }]} /></Form.Item>
              <Form.Item name="timeLimit" label="时间限制 ms"><InputNumber min={1} className="full-width" /></Form.Item>
              <Form.Item name="memoryLimit" label="内存限制 MB"><InputNumber min={1} className="full-width" /></Form.Item>
              <Form.Item name="categoryId" label="分类 ID"><Input placeholder="可选" /></Form.Item>
            </div>
            <Form.Item name="description" label="题目描述" rules={[{ required: true, message: '请输入题目描述' }]}><Input.TextArea rows={4} /></Form.Item>
            <div className="class-manage-form-grid class-manage-form-grid--two">
              <Form.Item name="inputFormat" label="输入格式"><Input.TextArea rows={3} /></Form.Item>
              <Form.Item name="outputFormat" label="输出格式"><Input.TextArea rows={3} /></Form.Item>
              <Form.Item name="sampleInput" label="样例输入"><Input.TextArea rows={3} /></Form.Item>
              <Form.Item name="sampleOutput" label="样例输出"><Input.TextArea rows={3} /></Form.Item>
            </div>
            <Form.Item name="hint" label="提示"><Input.TextArea rows={2} /></Form.Item>

            {problemModalMode === 'create' ? (
              <Form.List name="testCases">
                {(fields, { add, remove }) => (
                  <div className="class-manage-testcase-list">
                    <div className="class-manage-testcase-list__header">
                      <strong>测试用例</strong>
                      <Button icon={<PlusOutlined />} onClick={() => add({ caseNo: fields.length + 1, input: '', expectedOutput: '', isSample: 0, scoreWeight: 1, isHidden: 0 })}>添加用例</Button>
                    </div>
                    {fields.map((field, index) => (
                      <Card key={field.key} size="small" className="class-manage-testcase-card">
                        <div className="class-manage-testcase-card__header">
                          <strong>用例 {index + 1}</strong>
                          {fields.length > 1 ? <Button danger type="text" icon={<DeleteOutlined />} onClick={() => remove(field.name)}>删除</Button> : null}
                        </div>
                        <div className="class-manage-form-grid class-manage-form-grid--two">
                          <Form.Item name={[field.name, 'caseNo']} label="序号"><InputNumber min={1} className="full-width" /></Form.Item>
                          <Form.Item name={[field.name, 'scoreWeight']} label="权重"><InputNumber min={0} className="full-width" /></Form.Item>
                          <Form.Item name={[field.name, 'isSample']} label="是否样例"><Select options={[{ label: '否', value: 0 }, { label: '是', value: 1 }]} /></Form.Item>
                          <Form.Item name={[field.name, 'isHidden']} label="是否隐藏"><Select options={[{ label: '否', value: 0 }, { label: '是', value: 1 }]} /></Form.Item>
                          <Form.Item name={[field.name, 'input']} label="输入"><Input.TextArea rows={3} /></Form.Item>
                          <Form.Item name={[field.name, 'expectedOutput']} label="期望输出"><Input.TextArea rows={3} /></Form.Item>
                        </div>
                      </Card>
                    ))}
                  </div>
                )}
              </Form.List>
            ) : (
              <Alert showIcon type="info" message="当前编辑只更新题目基础信息，测试用例编辑需等待后端补充更新/删除接口。" />
            )}
          </Form>
        </Spin>
      </Modal>

      <Modal
        title="关联已有 OJ 题目"
        open={associationOpen}
        onOk={() => void handleAssociateProblems()}
        onCancel={() => setAssociationOpen(false)}
        confirmLoading={associating}
        okText="确认关联"
        cancelText="取消"
        width={920}
        destroyOnHidden
      >
        <Space direction="vertical" size={14} className="full-width">
          <div className="class-manage-problem-toolbar">
            <Space wrap>
              <Input.Search
                allowClear
                placeholder="搜索全局题号或题目标题"
                onSearch={(value) => void loadGlobalProblems({ keyword: value.trim(), pageNum: 1 })}
                className="class-manage-search"
              />
              <Select
                allowClear
                placeholder="难度"
                className="class-manage-filter-select"
                onChange={(value?: number) => void loadGlobalProblems({ difficulty: value, pageNum: 1 })}
                options={[{ label: '简单', value: 1 }, { label: '中等', value: 2 }, { label: '困难', value: 3 }]}
              />
            </Space>
            <Button icon={<ReloadOutlined />} loading={globalProblemLoading} onClick={() => void loadGlobalProblems({ ...globalProblemQuery })}>刷新</Button>
          </div>
          <Table<OjProblemVO>
            rowKey={(problem) => normalizeClassId(problem.id) || problem.problemNo || 'global-problem'}
            columns={globalProblemColumns}
            dataSource={globalProblemPage.records || []}
            loading={globalProblemLoading}
            rowSelection={{ selectedRowKeys: selectedProblemIds, onChange: setSelectedProblemIds }}
            pagination={{
              current: globalProblemPage.pageNum || 1,
              pageSize: globalProblemPage.pageSize || DEFAULT_PAGE_SIZE,
              total: globalProblemPage.total || 0,
              showSizeChanger: true,
            }}
            onChange={(pagination: TablePaginationConfig) => void loadGlobalProblems({
              pageNum: pagination.current || 1,
              pageSize: pagination.pageSize || DEFAULT_PAGE_SIZE,
            })}
          />
        </Space>
      </Modal>
    </Space>
  )
}