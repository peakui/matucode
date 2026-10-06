<script setup lang="ts">
import type { TagProps, CheckboxValueType } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import SectionCard from '@/components/SectionCard/index.vue'
import DetailMetricsGrid from '@/components/DetailMetricsGrid/index.vue'
import ImageUploadField from '@/components/ImageUploadField/index.vue'
import {
  approveClassMemberApi,
  associateClassProblemsApi,
  createAssignmentApi,
  createClassApi,
  createClassProblemApi,
  createDiscussionApi,
  createOjProblemApi,
  createOjTestCaseApi,
  getAssignmentDetailApi,
  getAssignmentListApi,
  getAssignmentRankingApi,
  getAssignmentSubmissionListApi,
  getClassDetailApi,
  getClassListApi,
  getClassMemberListApi,
  getClassProblemDetailApi,
  getClassProblemListApi,
  getDiscussionListApi,
  getOjProblemDetailApi,
  getOjProblemListApi,
  getOjSubmissionDetailApi,
  getOjSubmissionListApi,
  getOjTestCaseListApi,
  joinClassApi,
  removeClassMemberApi,
  removeClassProblemApi,
  updateAssignmentApi,
  updateClassApi,
  updateClassProblemApi,
  updateOjProblemApi,
} from '@/api'
import type {
  AssignmentDetailVO,
  AssignmentRankingVO,
  ClassAssignmentVO,
  ClassDiscussionVO,
  ClassMemberVO,
  ClassSubmissionVO,
  ClassVO,
  CreateOjProblemRequest,
  CreateOjTestCaseRequest,
  OjProblemVO,
  OjSubmissionVO,
} from '@/api/types'
import './index.scss'

type ClassResourceType = 1 | 2
type ProblemFormScope = 'global' | 'resource'

const activeTab = ref('problems')
const loading = ref(false)
const submitLoading = ref(false)
const detailLoading = ref(false)
const submissionLoading = ref(false)
const isEditMode = ref(false)
const problemFormScope = ref<ProblemFormScope>('global')
const problemFormResourceState = ref<ClassResourceState | null>(null)
const total = ref(0)
const submissionTotal = ref(0)
const problemList = ref<OjProblemVO[]>([])
const submissionList = ref<OjSubmissionVO[]>([])
const currentProblem = ref<OjProblemVO | null>(null)
const currentSubmission = ref<OjSubmissionVO | null>(null)
const assignmentContext = ref<{ classId: string; assignment: ClassAssignmentVO } | null>(null)
const assignmentDetailTab = ref('submissions')
const assignmentDetailLoading = ref(false)
const assignmentRankingLoading = ref(false)
const assignmentSubmissionsLoading = ref(false)
const assignmentDetail = ref<AssignmentDetailVO | null>(null)
const assignmentRanking = ref<AssignmentRankingVO | null>(null)
const assignmentRankingNotice = ref('')
const assignmentSubmissions = ref<ClassSubmissionVO[]>([])
const assignmentSubmissionsTotal = ref(0)
const assignmentSubmissionFilter = reactive({
  status: undefined as number | undefined,
  userId: undefined as string | undefined,
  pageNum: 1,
  pageSize: 10,
})
const filterForm = reactive({
  keyword: '',
  difficulty: undefined as number | undefined,
  status: undefined as number | undefined,
  pageNum: 1,
  pageSize: 10,
})
const submissionFilter = reactive({
  problemId: undefined as string | undefined,
  userId: undefined as string | undefined,
  status: undefined as number | undefined,
  pageNum: 1,
  pageSize: 10,
})
const problemForm = reactive<CreateOjProblemRequest>({
  problemNo: '',
  title: '',
  description: '',
  inputFormat: '',
  outputFormat: '',
  sampleInput: '',
  sampleOutput: '',
  hint: '',
  difficulty: 1,
  categoryId: undefined,
  timeLimit: 1000,
  memoryLimit: 256,
  status: 1,
})
const testCaseList = ref<CreateOjTestCaseRequest[]>([
  {
    caseNo: 1,
    input: '',
    expectedOutput: '',
    isSample: 1,
    scoreWeight: 1,
    isHidden: 0,
  },
])

const createClassResourceState = (type: ClassResourceType) =>
  reactive({
    type,
    loading: false,
    submitLoading: false,
    detailLoading: false,
    joinLoading: false,
    memberLoading: false,
    assignmentLoading: false,
    discussionLoading: false,
    problemLoading: false,
    list: [] as ClassVO[],
    total: 0,
    members: [] as ClassMemberVO[],
    assignments: [] as ClassAssignmentVO[],
    problemPool: [] as OjProblemVO[],
    problemPoolLoading: false,
    assignmentProblemKeyword: '',
    discussions: [] as ClassDiscussionVO[],
    scopedProblems: [] as OjProblemVO[],
    scopedProblemTotal: 0,
    current: null as ClassVO | null,
    joinVisible: false,
    assignmentVisible: false,
    isEditMode: false,
    assignmentEditId: '',
    activeDetailTab: 'members',
    filter: {
      keyword: '',
      status: undefined as number | undefined,
      pageNum: 1,
      pageSize: 10,
    },
    scopedProblemFilter: {
      keyword: '',
      difficulty: undefined as number | undefined,
      status: undefined as number | undefined,
      pageNum: 1,
      pageSize: 10,
    },
    associateVisible: false,
    associateLoading: false,
    associateSubmitLoading: false,
    candidateProblems: [] as OjProblemVO[],
    candidateProblemTotal: 0,
    selectedProblemIds: [] as string[],
    candidateProblemFilter: {
      keyword: '',
      difficulty: undefined as number | undefined,
      status: undefined as number | undefined,
      pageNum: 1,
      pageSize: 10,
    },
    form: {
      name: '',
      description: '',
      coverImage: '',
      joinMode: 1,
      inviteCode: '',
      status: 1,
      startTime: '' as string,
      endTime: '' as string,
    },
    joinForm: {
      inviteCode: '',
      message: '',
    },
    assignmentForm: {
      title: '',
      description: '',
      type: 1,
      problemIds: [] as string[],
      startTime: '',
      deadline: '',
      maxAttempts: 1,
      isPublicRank: 1,
      status: 1,
    },
    discussionForm: {
      assignmentId: '',
      title: '',
      content: '',
      isAnonymous: 0,
    },
  })

type ClassResourceState = ReturnType<typeof createClassResourceState>

const classState = createClassResourceState(1)
const contestState = createClassResourceState(2)

const difficultyOptions = [
  { label: '全部难度', value: undefined },
  { label: '简单', value: 1 },
  { label: '中等', value: 2 },
  { label: '困难', value: 3 },
]
const problemStatusOptions = [
  { label: '全部状态', value: undefined },
  { label: '隐藏', value: 0 },
  { label: '发布', value: 1 },
  { label: '下架', value: 2 },
]
const editProblemStatusOptions = [
  { label: '隐藏', value: 0 },
  { label: '发布', value: 1 },
  { label: '下架', value: 2 },
]
const classStatusOptions = [
  { label: '全部状态', value: undefined },
  { label: '禁用', value: 0 },
  { label: '正常', value: 1 },
]
const editClassStatusOptions = [
  { label: '禁用', value: 0 },
  { label: '正常', value: 1 },
]
const joinModeOptions = [
  { label: '公开加入', value: 1 },
  { label: '审核加入', value: 2 },
  { label: '邀请码加入', value: 3 },
]
const assignmentTypeOptions = [
  { label: '练习', value: 1 },
  { label: '考试', value: 2 },
  { label: '竞赛', value: 3 },
]
const resourceTabs = [
  { name: 'classes', label: '班级管理', state: classState },
  { name: 'contests', label: '竞赛管理', state: contestState },
]

const headerActionText = computed(() => {
  if (activeTab.value === 'problems') return '新建 OJ 题目'
  if (activeTab.value === 'classes') return '新建班级'
  if (activeTab.value === 'contests') return '新建竞赛'
  return ''
})

const formatDateTime = (value?: string) => (!value ? '-' : value.replace('T', ' '))
const getDifficultyText = (difficulty: number) =>
  ({ 1: '简单', 2: '中等', 3: '困难' })[difficulty] || '未知'
const getDifficultyType = (difficulty: number): TagProps['type'] =>
  (({ 1: 'success', 2: 'warning', 3: 'danger' })[difficulty] as TagProps['type']) || 'info'
const getProblemStatusText = (status: number) =>
  ({ 0: '隐藏', 1: '发布', 2: '下架' })[status] || '未知'
const getProblemStatusType = (status: number): TagProps['type'] =>
  (({ 0: 'info', 1: 'success', 2: 'warning' })[status] as TagProps['type']) || 'info'
const getSubmissionStatusText = (status: number) =>
  ({ 0: '待判题', 1: 'AC', 2: 'WA', 3: 'TLE', 4: 'MLE', 5: 'RE', 6: 'CE', 7: 'SE' })[status] ||
  '未知'
const getSubmissionStatusType = (status: number): TagProps['type'] =>
  (({
    0: 'info',
    1: 'success',
    2: 'warning',
    3: 'danger',
    4: 'danger',
    5: 'danger',
    6: 'danger',
    7: 'danger',
  })[status] as TagProps['type']) || 'info'
const getClassStatusText = (status: number) => ({ 0: '禁用', 1: '正常' })[status] || '未知'
const getClassStatusType = (status: number): TagProps['type'] =>
  (({ 0: 'info', 1: 'success' })[status] as TagProps['type']) || 'info'
const getClassTypeText = (type?: number) => ({ 1: '普通班级', 2: '竞赛' })[type || 0] || '未知'
const getJoinedText = (joined?: boolean) => {
  if (joined === undefined) return '-'
  return joined ? '是' : '否'
}
const getJoinModeText = (joinMode: number) =>
  ({ 1: '公开加入', 2: '审核加入', 3: '邀请码加入' })[joinMode] || '未知'
const getMemberRoleText = (role: number) => ({ 1: '教师', 2: '助教', 3: '学生' })[role] || '未知'
const getMemberStatusText = (status: number) =>
  ({ 0: '待审核', 1: '已通过', 2: '已拒绝' })[status] || '未知'
const getMemberStatusType = (status: number): TagProps['type'] =>
  (({ 0: 'warning', 1: 'success', 2: 'danger' })[status] as TagProps['type']) || 'info'
const getAssignmentTypeText = (type: number) =>
  ({ 1: '练习', 2: '考试', 3: '竞赛' })[type] || '未知'
const getClassCreatorName = (row: ClassVO) =>
  row.creatorName || row.creatorUsername || row.nickname || row.username || row.creatorId || '-'
const getMemberName = (row: ClassMemberVO) => row.nickname || row.username || row.userId || '-'
const getResourceName = (state: ClassResourceState) => (state.type === 1 ? '班级' : '竞赛')
const getProblemTitle = (row: OjProblemVO) => row.title || row.problemNo || row.id

const assignmentProblemPool = (state: ClassResourceState) => {
  const keyword = state.assignmentProblemKeyword.trim().toLowerCase()
  if (!keyword) return state.problemPool
  return state.problemPool.filter((item) => {
    const title = (item.title || '').toLowerCase()
    const problemNo = (item.problemNo || '').toLowerCase()
    return title.includes(keyword) || problemNo.includes(keyword)
  })
}
const isAssignmentProblemSelected = (state: ClassResourceState, id: string) =>
  state.assignmentForm.problemIds.some((item) => String(item) === String(id))
const toggleAssignmentProblem = (state: ClassResourceState, id: string, checked: CheckboxValueType) => {
  const target = String(id)
  if (checked) {
    if (!isAssignmentProblemSelected(state, target)) {
      state.assignmentForm.problemIds.push(target)
    }
  } else {
    state.assignmentForm.problemIds = state.assignmentForm.problemIds.filter((item) => String(item) !== target)
  }
}
const getAssignmentProblems = (state: ClassResourceState, row: ClassAssignmentVO) => {
  const ids = (row.problemIds || []).map(String)
  return state.problemPool.filter((item) => ids.includes(String(item.id)))
}

type PracticeOverlay = 'problem-detail' | 'problem-form' | 'class-detail' | 'class-form' | 'submission-detail' | 'assignment-detail'
const overlayStack = ref<PracticeOverlay[]>([])
const currentOverlay = computed<PracticeOverlay | null>(
  () => overlayStack.value[overlayStack.value.length - 1] ?? null,
)
const activeClassState = ref<ClassResourceState>(classState)
const openOverlay = (view: PracticeOverlay) => {
  overlayStack.value.push(view)
}
const closeOverlay = () => {
  overlayStack.value.pop()
}
const overlayTitle = computed(() => {
  const view = currentOverlay.value
  if (view === 'problem-detail') return '题目详情'
  if (view === 'problem-form') {
    const scopeName =
      problemFormScope.value === 'resource' && problemFormResourceState.value
        ? getResourceName(problemFormResourceState.value)
        : 'OJ'
    return `${isEditMode.value ? '编辑' : '新建'}${scopeName}题目`
  }
  if (view === 'class-detail') return `${getResourceName(activeClassState.value)}详情`
  if (view === 'class-form') {
    return `${activeClassState.value.isEditMode ? '编辑' : '新建'}${getResourceName(activeClassState.value)}`
  }
  if (view === 'submission-detail') return '提交详情'
  if (view === 'assignment-detail') {
    return assignmentContext.value ? `作业详情 · ${assignmentContext.value.assignment.title}` : '作业详情'
  }
  return ''
})

const resetProblemForm = () => {
  problemForm.problemNo = ''
  problemForm.title = ''
  problemForm.description = ''
  problemForm.inputFormat = ''
  problemForm.outputFormat = ''
  problemForm.sampleInput = ''
  problemForm.sampleOutput = ''
  problemForm.hint = ''
  problemForm.difficulty = 1
  problemForm.categoryId = undefined
  problemForm.timeLimit = 1000
  problemForm.memoryLimit = 256
  problemForm.status = 1
  testCaseList.value = [
    {
      caseNo: 1,
      input: '',
      expectedOutput: '',
      isSample: 1,
      scoreWeight: 1,
      isHidden: 0,
    },
  ]
}
const fetchProblemList = async () => {
  loading.value = true
  try {
    const res = await getOjProblemListApi({
      keyword: filterForm.keyword || undefined,
      difficulty: filterForm.difficulty,
      status: filterForm.status,
      pageNum: filterForm.pageNum,
      pageSize: filterForm.pageSize,
    })
    problemList.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}
const fetchSubmissionList = async () => {
  submissionLoading.value = true
  try {
    const res = await getOjSubmissionListApi({
      problemId: submissionFilter.problemId || undefined,
      userId: submissionFilter.userId || undefined,
      status: submissionFilter.status,
      pageNum: submissionFilter.pageNum,
      pageSize: submissionFilter.pageSize,
    })

    submissionList.value = res.data.records || []
    submissionTotal.value = res.data.total || 0
  } finally {
    submissionLoading.value = false
  }
}
const fetchClassResourceList = async (state: ClassResourceState) => {
  state.loading = true
  try {
    const res = await getClassListApi({
      keyword: state.filter.keyword || undefined,
      type: state.type,
      status: state.filter.status,
      pageNum: state.filter.pageNum,
      pageSize: state.filter.pageSize,
    })
    const records = res.data.records || []
    const filteredRecords = records.filter((item) => item.type === undefined || item.type === state.type)
    state.list = filteredRecords
    state.total = filteredRecords.length === records.length ? res.data.total || 0 : filteredRecords.length
  } finally {
    state.loading = false
  }
}
const fetchClassMembers = async (state: ClassResourceState) => {
  if (!state.current) return
  state.memberLoading = true
  try {
    const res = await getClassMemberListApi(state.current.id)
    state.members = res.data || []
  } finally {
    state.memberLoading = false
  }
}
const fetchAssignments = async (state: ClassResourceState) => {
  if (!state.current) return
  state.assignmentLoading = true
  try {
    const res = await getAssignmentListApi(state.current.id)
    state.assignments = res.data || []
  } finally {
    state.assignmentLoading = false
  }
}
const fetchProblemPool = async (state: ClassResourceState) => {
  if (!state.current) return
  state.problemPoolLoading = true
  try {
    const res = await getClassProblemListApi(state.current.id, { pageNum: 1, pageSize: 200 })
    state.problemPool = res.data.records || []
  } finally {
    state.problemPoolLoading = false
  }
}
const fetchDiscussions = async (state: ClassResourceState) => {
  if (!state.current) return
  state.discussionLoading = true
  try {
    const res = await getDiscussionListApi(state.current.id)
    state.discussions = res.data || []
  } finally {
    state.discussionLoading = false
  }
}
const fetchScopedProblems = async (state: ClassResourceState) => {
  if (!state.current) return
  state.problemLoading = true
  try {
    const res = await getClassProblemListApi(state.current.id, {
      keyword: state.scopedProblemFilter.keyword || undefined,
      difficulty: state.scopedProblemFilter.difficulty,
      status: state.scopedProblemFilter.status,
      pageNum: state.scopedProblemFilter.pageNum,
      pageSize: state.scopedProblemFilter.pageSize,
    })
    state.scopedProblems = res.data.records || []
    state.scopedProblemTotal = res.data.total || 0
  } finally {
    state.problemLoading = false
  }
}
const fetchCandidateProblems = async (state: ClassResourceState) => {
  state.associateLoading = true
  try {
    const res = await getOjProblemListApi({
      keyword: state.candidateProblemFilter.keyword || undefined,
      difficulty: state.candidateProblemFilter.difficulty,
      status: state.candidateProblemFilter.status,
      pageNum: state.candidateProblemFilter.pageNum,
      pageSize: state.candidateProblemFilter.pageSize,
    })
    state.candidateProblems = res.data.records || []
    state.candidateProblemTotal = res.data.total || 0
  } finally {
    state.associateLoading = false
  }
}
const fetchClassDetailResources = async (state: ClassResourceState) => {
  await Promise.all([
    fetchClassMembers(state),
    fetchAssignments(state),
    fetchDiscussions(state),
    fetchScopedProblems(state),
    fetchProblemPool(state),
  ])
}
const handleHeaderAction = () => {
  if (activeTab.value === 'problems') {
    handleCreateProblem()
    return
  }

  if (activeTab.value === 'classes') {
    handleCreateClassResource(classState)
    return
  }

  if (activeTab.value === 'contests') {
    handleCreateClassResource(contestState)
  }
}
const handleSearch = () => {
  filterForm.pageNum = 1
  void fetchProblemList()
}
const handleReset = () => {
  filterForm.keyword = ''
  filterForm.difficulty = undefined
  filterForm.status = undefined
  filterForm.pageNum = 1
  void fetchProblemList()
}
const handleSubmissionSearch = () => {
  submissionFilter.pageNum = 1
  void fetchSubmissionList()
}
const handleSubmissionReset = () => {
  submissionFilter.problemId = undefined
  submissionFilter.userId = undefined
  submissionFilter.status = undefined
  submissionFilter.pageNum = 1
  submissionFilter.pageSize = 10
  void fetchSubmissionList()
}
const handleClassResourceSearch = (state: ClassResourceState) => {
  state.filter.pageNum = 1
  void fetchClassResourceList(state)
}
const handleClassResourceReset = (state: ClassResourceState) => {
  state.filter.keyword = ''
  state.filter.status = undefined
  state.filter.pageNum = 1
  state.filter.pageSize = 10
  void fetchClassResourceList(state)
}
const resetClassForm = (state: ClassResourceState) => {
  state.form.name = ''
  state.form.description = ''
  state.form.coverImage = ''
  state.form.joinMode = 1
  state.form.inviteCode = ''
  state.form.status = 1
  state.form.startTime = ''
  state.form.endTime = ''
}
const handleCreateClassResource = (state: ClassResourceState) => {
  state.isEditMode = false
  state.current = null
  resetClassForm(state)
  activeClassState.value = state
  openOverlay('class-form')
}
const fillClassForm = (state: ClassResourceState, row: ClassVO) => {
  state.current = row
  state.form.name = row.name || ''
  state.form.description = row.description || ''
  state.form.coverImage = row.coverImage || ''
  state.form.joinMode = row.joinMode ?? 1
  state.form.inviteCode = row.inviteCode || ''
  state.form.status = row.status ?? 1
  state.form.startTime = row.startTime || ''
  state.form.endTime = row.endTime || ''
}
const handleEditClassResource = async (state: ClassResourceState, row: ClassVO) => {
  state.isEditMode = true
  fillClassForm(state, row)
  activeClassState.value = state
  openOverlay('class-form')

  try {
    const res = await getClassDetailApi(row.id)
    fillClassForm(state, res.data)
  } catch {
    ElMessage.warning(`未获取到${getResourceName(state)}详情，已使用列表数据回填`)
  }
}
const handleSubmitClassResource = async (state: ClassResourceState) => {
  if (!state.form.name.trim()) {
    ElMessage.warning(`请填写${getResourceName(state)}名称`)
    return
  }

  state.submitLoading = true
  try {
    const payload = {
      name: state.form.name,
      description: state.form.description || undefined,
      type: state.type,
      coverImage: state.form.coverImage || undefined,
      joinMode: state.form.joinMode,
      inviteCode: state.form.inviteCode || undefined,
      startTime: state.type === 2 ? (state.form.startTime || undefined) : undefined,
      endTime: state.type === 2 ? (state.form.endTime || undefined) : undefined,
    }

    if (state.isEditMode && state.current) {
      await updateClassApi(state.current.id, { ...payload, status: state.form.status })
      ElMessage.success(`${getResourceName(state)}更新成功`)
    } else {
      await createClassApi(payload)
      ElMessage.success(`${getResourceName(state)}创建成功`)
    }

    closeOverlay()
    if (currentOverlay.value === 'class-detail' && state.current) {
      await loadClassDetail(state)
    } else {
      await fetchClassResourceList(state)
    }
  } finally {
    state.submitLoading = false
  }
}
const handleOpenJoinDialog = (state: ClassResourceState, row: ClassVO) => {
  state.current = row
  state.joinForm.inviteCode = ''
  state.joinForm.message = ''
  state.joinVisible = true
}
const handleJoinClassResource = async (state: ClassResourceState) => {
  if (!state.current) return
  state.joinLoading = true
  try {
    await joinClassApi(state.current.id, {
      inviteCode: state.joinForm.inviteCode || undefined,
      message: state.joinForm.message || undefined,
    })
    ElMessage.success(`${getResourceName(state)}加入申请已提交`)
    state.joinVisible = false
    await fetchClassResourceList(state)
  } finally {
    state.joinLoading = false
  }
}
const loadClassDetail = async (state: ClassResourceState) => {
  if (!state.current) return
  state.detailLoading = true
  try {
    const res = await getClassDetailApi(state.current.id)
    state.current = res.data
    await fetchClassDetailResources(state)
  } finally {
    state.detailLoading = false
  }
}
const handleOpenClassDetail = async (state: ClassResourceState, row: ClassVO) => {
  state.current = row
  state.activeDetailTab = 'members'
  activeClassState.value = state
  openOverlay('class-detail')
  await loadClassDetail(state)
}
const handleApproveMember = async (state: ClassResourceState, row: ClassMemberVO) => {
  if (!state.current) return
  await approveClassMemberApi(state.current.id, row.id)
  ElMessage.success('成员已通过审核')
  await fetchClassMembers(state)
}
const handleRemoveMember = async (state: ClassResourceState, row: ClassMemberVO) => {
  if (!state.current) return
  await ElMessageBox.confirm(`确定移除成员 ${getMemberName(row)} 吗？`, '移除成员', { type: 'warning' })
  await removeClassMemberApi(state.current.id, row.id)
  ElMessage.success('成员已移除')
  await fetchClassMembers(state)
}
const resetAssignmentForm = (state: ClassResourceState) => {
  state.assignmentEditId = ''
  state.assignmentForm.title = ''
  state.assignmentForm.description = ''
  state.assignmentForm.type = state.type === 2 ? 3 : 1
  state.assignmentForm.problemIds = []
  state.assignmentForm.startTime = ''
  state.assignmentForm.deadline = ''
  state.assignmentForm.maxAttempts = 1
  state.assignmentForm.isPublicRank = 1
  state.assignmentForm.status = 1
}
const handleCreateAssignment = async (state: ClassResourceState) => {
  resetAssignmentForm(state)
  state.assignmentProblemKeyword = ''
  await fetchProblemPool(state)
  state.assignmentVisible = true
}
const handleEditAssignment = async (state: ClassResourceState, row: ClassAssignmentVO) => {
  state.assignmentEditId = row.id
  state.assignmentForm.title = row.title || ''
  state.assignmentForm.description = row.description || ''
  state.assignmentForm.type = row.type || (state.type === 2 ? 3 : 1)
  state.assignmentForm.problemIds = (row.problemIds || []).map(String)
  state.assignmentForm.startTime = row.startTime || ''
  state.assignmentForm.deadline = row.deadline || ''
  state.assignmentForm.maxAttempts = row.maxAttempts || 1
  state.assignmentForm.isPublicRank = row.isPublicRank ?? 1
  state.assignmentForm.status = row.status ?? 1
  state.assignmentProblemKeyword = ''
  await fetchProblemPool(state)
  state.assignmentVisible = true
}
const handleSubmitAssignment = async (state: ClassResourceState) => {
  if (!state.current) return
  if (!state.assignmentForm.title.trim()) {
    ElMessage.warning('请填写作业标题')
    return
  }

  state.submitLoading = true
  try {
    const payload = {
      title: state.assignmentForm.title,
      description: state.assignmentForm.description || undefined,
      type: state.assignmentForm.type,
      problemIds: state.assignmentForm.problemIds,
      startTime: state.assignmentForm.startTime || undefined,
      deadline: state.assignmentForm.deadline || undefined,
      maxAttempts: state.assignmentForm.maxAttempts,
      isPublicRank: state.assignmentForm.isPublicRank,
    }

    if (state.assignmentEditId) {
      await updateAssignmentApi(state.current.id, state.assignmentEditId, {
        ...payload,
        status: state.assignmentForm.status,
      })
      ElMessage.success('作业更新成功')
    } else {
      await createAssignmentApi(state.current.id, payload)
      ElMessage.success('作业创建成功')
    }

    state.assignmentVisible = false
    await fetchAssignments(state)
  } finally {
    state.submitLoading = false
  }
}
const handleCreateDiscussion = async (state: ClassResourceState) => {
  if (!state.current) return
  if (!state.discussionForm.title.trim() || !state.discussionForm.content.trim()) {
    ElMessage.warning('请填写讨论标题和内容')
    return
  }

  state.discussionLoading = true
  try {
    await createDiscussionApi(state.current.id, {
      assignmentId: state.discussionForm.assignmentId || undefined,
      title: state.discussionForm.title,
      content: state.discussionForm.content,
      isAnonymous: state.discussionForm.isAnonymous,
    })
    state.discussionForm.assignmentId = ''
    state.discussionForm.title = ''
    state.discussionForm.content = ''
    state.discussionForm.isAnonymous = 0
    ElMessage.success('讨论创建成功')
    await fetchDiscussions(state)
  } finally {
    state.discussionLoading = false
  }
}
const handleScopedProblemSearch = (state: ClassResourceState) => {
  state.scopedProblemFilter.pageNum = 1
  void fetchScopedProblems(state)
}
const handleScopedProblemReset = (state: ClassResourceState) => {
  state.scopedProblemFilter.keyword = ''
  state.scopedProblemFilter.difficulty = undefined
  state.scopedProblemFilter.status = undefined
  state.scopedProblemFilter.pageNum = 1
  state.scopedProblemFilter.pageSize = 10
  void fetchScopedProblems(state)
}
const handleOpenAssociateDialog = async (state: ClassResourceState) => {
  if (!state.current) return
  state.selectedProblemIds = []
  state.candidateProblemFilter.keyword = ''
  state.candidateProblemFilter.difficulty = undefined
  state.candidateProblemFilter.status = undefined
  state.candidateProblemFilter.pageNum = 1
  state.candidateProblemFilter.pageSize = 10
  state.associateVisible = true
  await fetchCandidateProblems(state)
}
const handleCandidateProblemSearch = (state: ClassResourceState) => {
  state.candidateProblemFilter.pageNum = 1
  void fetchCandidateProblems(state)
}
const handleCandidateProblemReset = (state: ClassResourceState) => {
  state.candidateProblemFilter.keyword = ''
  state.candidateProblemFilter.difficulty = undefined
  state.candidateProblemFilter.status = undefined
  state.candidateProblemFilter.pageNum = 1
  state.candidateProblemFilter.pageSize = 10
  state.selectedProblemIds = []
  void fetchCandidateProblems(state)
}
const handleCandidateSelectionChange = (state: ClassResourceState, rows: OjProblemVO[]) => {
  state.selectedProblemIds = rows.map((item) => item.id)
}
const handleCandidateProblemPageChange = (state: ClassResourceState, page: number) => {
  state.candidateProblemFilter.pageNum = page
  void fetchCandidateProblems(state)
}
const handleCandidateProblemSizeChange = (state: ClassResourceState, size: number) => {
  state.candidateProblemFilter.pageSize = size
  state.candidateProblemFilter.pageNum = 1
  void fetchCandidateProblems(state)
}
const handleAssociateProblems = async (state: ClassResourceState) => {
  if (!state.current) return
  if (!state.selectedProblemIds.length) {
    ElMessage.warning('请选择要关联的题目')
    return
  }

  state.associateSubmitLoading = true
  try {
    await associateClassProblemsApi(state.current.id, { problemIds: state.selectedProblemIds })
    ElMessage.success('题目关联成功')
    state.associateVisible = false
    await fetchScopedProblems(state)
  } finally {
    state.associateSubmitLoading = false
  }
}
const handleRemoveScopedProblem = async (state: ClassResourceState, row: OjProblemVO) => {
  if (!state.current) return
  await ElMessageBox.confirm(`确定从${getResourceName(state)}中移除题目《${getProblemTitle(row)}》吗？`, '移除题目', { type: 'warning' })
  await removeClassProblemApi(state.current.id, row.id)
  ElMessage.success('题目已移除')
  await fetchScopedProblems(state)
}
const handleViewScopedProblem = async (state: ClassResourceState, row: OjProblemVO) => {
  if (!state.current) return
  problemFormScope.value = 'resource'
  problemFormResourceState.value = state
  activeClassState.value = state
  openOverlay('problem-detail')
  detailLoading.value = true
  try {
    const res = await getClassProblemDetailApi(state.current.id, row.id)
    currentProblem.value = res.data
  } finally {
    detailLoading.value = false
  }
}
const handleCreateProblem = () => {
  isEditMode.value = false
  problemFormScope.value = 'global'
  problemFormResourceState.value = null
  currentProblem.value = null
  resetProblemForm()
  openOverlay('problem-form')
}
const handleCreateScopedProblem = (state: ClassResourceState) => {
  if (!state.current) return
  isEditMode.value = false
  problemFormScope.value = 'resource'
  problemFormResourceState.value = state
  activeClassState.value = state
  currentProblem.value = null
  resetProblemForm()
  openOverlay('problem-form')
}
const refreshCurrentProblemDetail = async () => {
  if (!currentProblem.value) return
  const problemId = currentProblem.value.id
  const resourceState = problemFormScope.value === 'resource' ? problemFormResourceState.value : null
  const resourceId = resourceState?.current?.id

  detailLoading.value = true
  try {
    const res = resourceId
      ? await getClassProblemDetailApi(resourceId, problemId)
      : await getOjProblemDetailApi(problemId)
    currentProblem.value = res.data
  } finally {
    detailLoading.value = false
  }
}
const handleSubmitProblem = async () => {
  submitLoading.value = true
  try {
    const resourceState = problemFormScope.value === 'resource' ? problemFormResourceState.value : null
    const resourceId = resourceState?.current?.id

    if (isEditMode.value && currentProblem.value) {
      if (resourceState && resourceId) {
        await updateClassProblemApi(resourceId, currentProblem.value.id, { ...problemForm })
      } else {
        await updateOjProblemApi(currentProblem.value.id, { ...problemForm })
      }
      ElMessage.success('题目更新成功')
    } else {
      const res = resourceState && resourceId
        ? await createClassProblemApi(resourceId, { ...problemForm })
        : await createOjProblemApi({ ...problemForm })

      const validTestCases = testCaseList.value.filter(
        (item) => item.input.trim() && item.expectedOutput.trim(),
      )

      if (validTestCases.length > 0) {
        await Promise.all(validTestCases.map((item) => createOjTestCaseApi(res.data.id, item)))
      }

      ElMessage.success('题目创建成功')
    }

    closeOverlay()
    if (currentOverlay.value === 'problem-detail') {
      await refreshCurrentProblemDetail()
    } else if (resourceState) {
      await fetchScopedProblems(resourceState)
    } else {
      await fetchProblemList()
    }
  } finally {
    submitLoading.value = false
  }
}
const handleAddTestCase = () => {
  testCaseList.value.push({
    caseNo: testCaseList.value.length + 1,
    input: '',
    expectedOutput: '',
    isSample: 0,
    scoreWeight: 1,
    isHidden: 0,
  })
}

const handleRemoveTestCase = (index: number) => {
  testCaseList.value.splice(index, 1)
  testCaseList.value = testCaseList.value.map((item, itemIndex) => ({
    ...item,
    caseNo: itemIndex + 1,
  }))
}

const handleViewProblem = async (row: OjProblemVO) => {
  problemFormScope.value = 'global'
  problemFormResourceState.value = null
  openOverlay('problem-detail')
  detailLoading.value = true
  try {
    const res = await getOjProblemDetailApi(row.id)
    currentProblem.value = res.data
  } finally {
    detailLoading.value = false
  }
}

const handleEditProblemByRow = async (row: OjProblemVO) => {
  problemFormScope.value = 'global'
  problemFormResourceState.value = null
  detailLoading.value = true
  try {
    const [detailRes, testCaseRes] = await Promise.all([
      getOjProblemDetailApi(row.id),
      getOjTestCaseListApi(row.id),
    ])
    currentProblem.value = detailRes.data
    testCaseList.value =
      testCaseRes.data?.length > 0
        ? testCaseRes.data.map((item) => ({
            caseNo: item.caseNo,
            input: item.input || '',
            expectedOutput: item.expectedOutput || '',
            isSample: item.isSample ?? 0,
            scoreWeight: item.scoreWeight ?? 1,
            isHidden: item.isHidden ?? 0,
          }))
        : [
            {
              caseNo: 1,
              input: detailRes.data.sampleInput || '',
              expectedOutput: detailRes.data.sampleOutput || '',
              isSample: 1,
              scoreWeight: 1,
              isHidden: 0,
            },
          ]
    handleEditProblem()
  } finally {
    detailLoading.value = false
  }
}

const handleEditScopedProblem = async (state: ClassResourceState, row: OjProblemVO) => {
  if (!state.current) return
  problemFormScope.value = 'resource'
  problemFormResourceState.value = state
  detailLoading.value = true
  try {
    const [detailRes, testCaseRes] = await Promise.all([
      getClassProblemDetailApi(state.current.id, row.id),
      getOjTestCaseListApi(row.id),
    ])
    currentProblem.value = detailRes.data
    testCaseList.value =
      testCaseRes.data?.length > 0
        ? testCaseRes.data.map((item) => ({
            caseNo: item.caseNo,
            input: item.input || '',
            expectedOutput: item.expectedOutput || '',
            isSample: item.isSample ?? 0,
            scoreWeight: item.scoreWeight ?? 1,
            isHidden: item.isHidden ?? 0,
          }))
        : [
            {
              caseNo: 1,
              input: detailRes.data.sampleInput || '',
              expectedOutput: detailRes.data.sampleOutput || '',
              isSample: 1,
              scoreWeight: 1,
              isHidden: 0,
            },
          ]
    handleEditProblem()
  } finally {
    detailLoading.value = false
  }
}

const fillProblemForm = (problem: OjProblemVO) => {
  problemForm.problemNo = problem.problemNo
  problemForm.title = problem.title
  problemForm.description = problem.description || ''
  problemForm.inputFormat = problem.inputFormat || ''
  problemForm.outputFormat = problem.outputFormat || ''
  problemForm.sampleInput = problem.sampleInput || ''
  problemForm.sampleOutput = problem.sampleOutput || ''
  problemForm.hint = problem.hint || ''
  problemForm.difficulty = problem.difficulty
  problemForm.categoryId = problem.categoryId || undefined
  problemForm.timeLimit = problem.timeLimit
  problemForm.memoryLimit = problem.memoryLimit
  problemForm.status = problem.status
}

const handleEditProblem = () => {
  if (!currentProblem.value) return

  isEditMode.value = true
  fillProblemForm(currentProblem.value)

  if (!testCaseList.value.length) {
    testCaseList.value = [
      {
        caseNo: 1,
        input: currentProblem.value.sampleInput || '',
        expectedOutput: currentProblem.value.sampleOutput || '',
        isSample: 1,
        scoreWeight: 1,
        isHidden: 0,
      },
    ]
  }

  openOverlay('problem-form')
}

const handleViewSubmission = async (row: { id: string }) => {
  openOverlay('submission-detail')
  submissionLoading.value = true
  try {
    const res = await getOjSubmissionDetailApi(row.id)
    currentSubmission.value = res.data
  } finally {
    submissionLoading.value = false
  }
}

const fetchAssignmentDetail = async () => {
  if (!assignmentContext.value) return
  assignmentDetailLoading.value = true
  try {
    const res = await getAssignmentDetailApi(
      assignmentContext.value.classId,
      assignmentContext.value.assignment.id,
    )
    assignmentDetail.value = res.data
  } finally {
    assignmentDetailLoading.value = false
  }
}

const fetchAssignmentRanking = async () => {
  if (!assignmentContext.value) return
  assignmentRankingLoading.value = true
  assignmentRankingNotice.value = ''
  try {
    const res = await getAssignmentRankingApi(
      assignmentContext.value.classId,
      assignmentContext.value.assignment.id,
    )
    assignmentRanking.value = res.data
  } catch {
    assignmentRanking.value = null
    assignmentRankingNotice.value =
      assignmentContext.value.assignment.isPublicRank === 1
        ? '排行榜暂时无法加载'
        : '该作业排行榜未公开，学生不可见（教师与管理员始终可见）'
  } finally {
    assignmentRankingLoading.value = false
  }
}

const fetchAssignmentSubmissions = async () => {
  if (!assignmentContext.value) return
  assignmentSubmissionsLoading.value = true
  try {
    const res = await getAssignmentSubmissionListApi(
      assignmentContext.value.classId,
      assignmentContext.value.assignment.id,
      {
        userId: assignmentSubmissionFilter.userId || undefined,
        status: assignmentSubmissionFilter.status,
        pageNum: assignmentSubmissionFilter.pageNum,
        pageSize: assignmentSubmissionFilter.pageSize,
      },
    )
    assignmentSubmissions.value = res.data.records || []
    assignmentSubmissionsTotal.value = res.data.total || 0
  } finally {
    assignmentSubmissionsLoading.value = false
  }
}

const handleOpenAssignmentDetail = async (state: ClassResourceState, row: ClassAssignmentVO) => {
  if (!state.current) return
  assignmentContext.value = { classId: state.current.id, assignment: row }
  assignmentDetail.value = null
  assignmentRanking.value = null
  assignmentRankingNotice.value = ''
  assignmentSubmissions.value = []
  assignmentSubmissionsTotal.value = 0
  assignmentSubmissionFilter.userId = undefined
  assignmentSubmissionFilter.status = undefined
  assignmentSubmissionFilter.pageNum = 1
  assignmentDetailTab.value = 'submissions'
  openOverlay('assignment-detail')
  await Promise.all([fetchAssignmentDetail(), fetchAssignmentRanking(), fetchAssignmentSubmissions()])
}

const handleAssignmentSubmissionSearch = () => {
  assignmentSubmissionFilter.pageNum = 1
  void fetchAssignmentSubmissions()
}

const handleAssignmentSubmissionReset = () => {
  assignmentSubmissionFilter.userId = undefined
  assignmentSubmissionFilter.status = undefined
  assignmentSubmissionFilter.pageNum = 1
  void fetchAssignmentSubmissions()
}

const handleAssignmentSubmissionPageChange = (page: number) => {
  assignmentSubmissionFilter.pageNum = page
  void fetchAssignmentSubmissions()
}
const handlePageChange = (page: number) => {
  filterForm.pageNum = page
  void fetchProblemList()
}
const handleSizeChange = (size: number) => {
  filterForm.pageSize = size
  filterForm.pageNum = 1
  void fetchProblemList()
}
const handleSubmissionPageChange = (page: number) => {
  submissionFilter.pageNum = page
  void fetchSubmissionList()
}
const handleSubmissionSizeChange = (size: number) => {
  submissionFilter.pageSize = size
  submissionFilter.pageNum = 1
  void fetchSubmissionList()
}
const handleClassPageChange = (state: ClassResourceState, page: number) => {
  state.filter.pageNum = page
  void fetchClassResourceList(state)
}
const handleClassSizeChange = (state: ClassResourceState, size: number) => {
  state.filter.pageSize = size
  state.filter.pageNum = 1
  void fetchClassResourceList(state)
}
const handleScopedProblemPageChange = (state: ClassResourceState, page: number) => {
  state.scopedProblemFilter.pageNum = page
  void fetchScopedProblems(state)
}
const handleScopedProblemSizeChange = (state: ClassResourceState, size: number) => {
  state.scopedProblemFilter.pageSize = size
  state.scopedProblemFilter.pageNum = 1
  void fetchScopedProblems(state)
}

onMounted(async () => {
  await Promise.all([
    fetchProblemList(),
    fetchSubmissionList(),
    fetchClassResourceList(classState),
    fetchClassResourceList(contestState),
  ])
})
</script>

<template>
  <div class="practice-manage-page">
    <template v-if="currentOverlay">
      <div class="practice-detail-view">
        <div class="practice-detail-view__header">
          <el-button :icon="ArrowLeft" @click="closeOverlay">返回</el-button>
          <h2 class="practice-detail-view__title">{{ overlayTitle }}</h2>
        </div>

        <SectionCard v-if="currentOverlay === 'problem-detail'">
          <div v-loading="detailLoading" class="practice-detail">
            <template v-if="currentProblem">
              <div class="problem-form__section-header">
                <strong>题目基础信息</strong>
                <el-button type="primary" @click="handleEditProblem">编辑题目</el-button>
              </div>
              <div class="practice-detail__item">
                <span>题号</span><strong>{{ currentProblem.problemNo }}</strong>
              </div>
              <div class="practice-detail__item">
                <span>标题</span><strong>{{ currentProblem.title }}</strong>
              </div>
              <div class="practice-detail__item">
                <span>题目描述</span>
                <p>{{ currentProblem.description || currentProblem.contentMd || '-' }}</p>
              </div>
              <div class="problem-form__grid">
                <div class="practice-detail__item">
                  <span>输入格式</span>
                  <p>{{ currentProblem.inputFormat || '-' }}</p>
                </div>
                <div class="practice-detail__item">
                  <span>输出格式</span>
                  <p>{{ currentProblem.outputFormat || '-' }}</p>
                </div>
              </div>
              <div class="problem-form__grid">
                <div class="practice-detail__item">
                  <span>样例输入</span>
                  <pre>{{ currentProblem.sampleInput || '-' }}</pre>
                </div>
                <div class="practice-detail__item">
                  <span>样例输出</span>
                  <pre>{{ currentProblem.sampleOutput || '-' }}</pre>
                </div>
              </div>
              <div class="practice-detail__item">
                <span>提示</span>
                <p>{{ currentProblem.hint || '-' }}</p>
              </div>
              <DetailMetricsGrid
                :items="[
                  { label: '难度', value: getDifficultyText(currentProblem.difficulty) },
                  { label: '状态', value: getProblemStatusText(currentProblem.status) },
                  { label: '分类ID', value: currentProblem.categoryId || '-' },
                  { label: '时间限制', value: `${currentProblem.timeLimit} ms` },
                ]"
              />
              <DetailMetricsGrid
                :items="[
                  { label: '内存限制', value: `${currentProblem.memoryLimit} MB` },
                  { label: '提交数', value: currentProblem.submitCount },
                  { label: '通过数', value: currentProblem.acceptCount },
                  { label: '通过率', value: `${currentProblem.acceptRate ?? 0}%` },
                ]"
              />
              <DetailMetricsGrid
                :items="[
                  { label: '创建时间', value: formatDateTime(currentProblem.createdAt) },
                  { label: '更新时间', value: formatDateTime(currentProblem.updatedAt) },
                ]"
              />
            </template>
          </div>
        </SectionCard>

        <SectionCard v-else-if="currentOverlay === 'problem-form'">
          <div class="problem-form__grid">
            <el-input v-model="problemForm.problemNo" placeholder="题目编号，如 A1001" />
            <el-input v-model="problemForm.title" placeholder="题目名称" />
            <el-select v-model="problemForm.difficulty" placeholder="选择难度">
              <el-option
                v-for="item in difficultyOptions.filter((item) => item.value !== undefined)"
                :key="item.label"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
            <el-select v-model="problemForm.status" placeholder="选择状态">
              <el-option
                v-for="item in editProblemStatusOptions"
                :key="item.label"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </div>

          <div class="problem-form__grid problem-form__grid--narrow">
            <el-input v-model="problemForm.categoryId" placeholder="分类ID，1是普通oj，2是班级/竞赛的题目" />
            <el-input-number v-model="problemForm.timeLimit" :min="1" controls-position="right" />
            <el-input-number
              v-model="problemForm.memoryLimit"
              :min="1"
              controls-position="right"
            />
          </div>

          <div class="problem-form__block">
            <span class="problem-form__label">题目描述</span>
            <el-input
              v-model="problemForm.description"
              type="textarea"
              :rows="5"
              placeholder="对应 oj_problems.description"
            />
          </div>

          <div class="problem-form__grid">
            <div class="problem-form__block">
              <span class="problem-form__label">输入格式</span>
              <el-input
                v-model="problemForm.inputFormat"
                type="textarea"
                :rows="4"
                placeholder="对应 oj_problems.input_format"
              />
            </div>
            <div class="problem-form__block">
              <span class="problem-form__label">输出格式</span>
              <el-input
                v-model="problemForm.outputFormat"
                type="textarea"
                :rows="4"
                placeholder="对应 oj_problems.output_format"
              />
            </div>
          </div>

          <div class="problem-form__grid">
            <div class="problem-form__block">
              <span class="problem-form__label">样例输入</span>
              <el-input
                v-model="problemForm.sampleInput"
                type="textarea"
                :rows="4"
                placeholder="对应 oj_problems.sample_input"
              />
            </div>
            <div class="problem-form__block">
              <span class="problem-form__label">样例输出</span>
              <el-input
                v-model="problemForm.sampleOutput"
                type="textarea"
                :rows="4"
                placeholder="对应 oj_problems.sample_output"
              />
            </div>
          </div>

          <div class="problem-form__block">
            <span class="problem-form__label">提示</span>
            <el-input
              v-model="problemForm.hint"
              type="textarea"
              :rows="3"
              placeholder="对应 oj_problems.hint"
            />
          </div>

          <div class="problem-form__section-header">
            <strong>测试用例配置</strong>
            <el-button type="primary" plain @click="handleAddTestCase">新增用例</el-button>
          </div>

          <div
            v-for="(item, index) in testCaseList"
            :key="`test-case-${index}`"
            class="problem-form__testcase"
          >
            <div class="problem-form__section-header">
              <span>测试用例 #{{ index + 1 }}</span>
              <el-button type="danger" link :disabled="testCaseList.length === 1" @click="handleRemoveTestCase(index)">
                删除
              </el-button>
            </div>
            <div class="problem-form__grid">
              <el-input-number v-model="item.caseNo" :min="1" controls-position="right" />
              <el-input-number
                v-model="item.scoreWeight"
                :min="0"
                :step="0.5"
                controls-position="right"
              />
              <el-select v-model="item.isSample" placeholder="是否样例">
                <el-option :value="0" label="否" />
                <el-option :value="1" label="是" />
              </el-select>
              <el-select v-model="item.isHidden" placeholder="是否隐藏">
                <el-option :value="0" label="否" />
                <el-option :value="1" label="是" />
              </el-select>
            </div>
            <div class="problem-form__grid">
              <el-input
                v-model="item.input"
                type="textarea"
                :rows="4"
                placeholder="对应 oj_test_cases.input"
              />
              <el-input
                v-model="item.expectedOutput"
                type="textarea"
                :rows="4"
                placeholder="对应 oj_test_cases.expected_output"
              />
            </div>
          </div>

          <div class="dialog-footer">
            <el-button @click="closeOverlay">取消</el-button>
            <el-button type="primary" :loading="submitLoading" @click="handleSubmitProblem">
              {{ isEditMode ? '保存修改' : '确认创建' }}
            </el-button>
          </div>
        </SectionCard>

        <SectionCard v-else-if="currentOverlay === 'class-detail'">
          <div v-loading="activeClassState.detailLoading" class="class-detail">
            <template v-if="activeClassState.current">
              <div class="class-detail__summary">
                <div class="class-detail__summary-main">
                  <span>{{ getResourceName(activeClassState) }}名称</span>
                  <strong>{{ activeClassState.current.name }}</strong>
                  <p>{{ activeClassState.current.description || '暂无说明' }}</p>
                </div>
                <DetailMetricsGrid
                  :items="[
                    { label: 'ID', value: activeClassState.current.id },
                    { label: '类型', value: getClassTypeText(activeClassState.current.type ?? activeClassState.type) },
                    { label: '创建者', value: getClassCreatorName(activeClassState.current) },
                    { label: '加入方式', value: getJoinModeText(activeClassState.current.joinMode) },
                    { label: '成员数', value: activeClassState.current.memberCount ?? activeClassState.members.length },
                    { label: '是否已加入', value: getJoinedText(activeClassState.current.joined) },
                    { label: '状态', value: getClassStatusText(activeClassState.current.status) },
                    { label: '创建时间', value: formatDateTime(activeClassState.current.createdAt) },
                    { label: '更新时间', value: formatDateTime(activeClassState.current.updatedAt) },
                  ]"
                />
                <div class="class-detail__summary-extra">
                  <div>
                    <span>封面图片</span>
                    <template v-if="activeClassState.current.coverImage">
                      <el-image :src="activeClassState.current.coverImage" fit="cover" class="class-detail__cover" />
                      <el-link :href="activeClassState.current.coverImage" target="_blank" type="primary" :underline="false">
                        {{ activeClassState.current.coverImage }}
                      </el-link>
                    </template>
                    <p v-else>暂无封面</p>
                  </div>
                  <div>
                    <span>邀请码</span>
                    <strong>{{ activeClassState.current.inviteCode || '暂无邀请码' }}</strong>
                  </div>
                  <div class="class-detail__summary-actions">
                    <el-button type="primary" @click="handleEditClassResource(activeClassState, activeClassState.current)">编辑信息</el-button>
                  </div>
                </div>
              </div>

              <el-tabs v-model="activeClassState.activeDetailTab" class="class-detail__tabs">
                <el-tab-pane label="成员管理" name="members">
                  <el-table :data="activeClassState.members" v-loading="activeClassState.memberLoading" width="100%">
                    <el-table-column label="成员" min-width="180" show-overflow-tooltip>
                      <template #default="scope">{{ getMemberName(scope.row) }}</template>
                    </el-table-column>
                    <el-table-column prop="userId" label="用户ID" min-width="120" show-overflow-tooltip />
                    <el-table-column label="角色" width="100">
                      <template #default="scope">{{ getMemberRoleText(scope.row.role) }}</template>
                    </el-table-column>
                    <el-table-column label="状态" width="110">
                      <template #default="scope">
                        <el-tag :type="getMemberStatusType(scope.row.joinStatus)" effect="light">
                          {{ getMemberStatusText(scope.row.joinStatus) }}
                        </el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column label="加入时间" min-width="170">
                      <template #default="scope">{{ formatDateTime(scope.row.joinedAt) }}</template>
                    </el-table-column>
                    <el-table-column label="操作" width="160" fixed="right">
                      <template #default="scope">
                        <el-button
                          type="success"
                          link
                          :disabled="scope.row.joinStatus === 1"
                          @click="handleApproveMember(activeClassState, scope.row)"
                        >
                          通过
                        </el-button>
                        <el-button type="danger" link @click="handleRemoveMember(activeClassState, scope.row)">移除</el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                </el-tab-pane>

                <el-tab-pane v-if="activeClassState.type === 1" label="作业管理" name="assignments">
                  <div class="class-detail__toolbar">
                    <el-button type="primary" @click="handleCreateAssignment(activeClassState)">新增作业</el-button>
                  </div>
                  <el-table :data="activeClassState.assignments" v-loading="activeClassState.assignmentLoading" width="100%">
                    <el-table-column type="expand">
                      <template #default="scope">
                        <div class="assignment-problems">
                          <el-table
                            v-if="getAssignmentProblems(activeClassState, scope.row).length"
                            :data="getAssignmentProblems(activeClassState, scope.row)"
                            size="small"
                            width="100%"
                          >
                            <el-table-column prop="problemNo" label="题号" width="140" />
                            <el-table-column label="题目" min-width="220" show-overflow-tooltip>
                              <template #default="row">{{ getProblemTitle(row.row) }}</template>
                            </el-table-column>
                            <el-table-column label="难度" width="110">
                              <template #default="row">
                                <el-tag :type="getDifficultyType(row.row.difficulty)" effect="light">
                                  {{ getDifficultyText(row.row.difficulty) }}
                                </el-tag>
                              </template>
                            </el-table-column>
                          </el-table>
                          <el-empty v-else description="该作业暂无题目" :image-size="60" />
                        </div>
                      </template>
                    </el-table-column>
                    <el-table-column prop="title" label="作业标题" min-width="180" show-overflow-tooltip />
                    <el-table-column label="类型" width="100">
                      <template #default="scope">{{ getAssignmentTypeText(scope.row.type) }}</template>
                    </el-table-column>
                    <el-table-column label="题目数" width="90">
                      <template #default="scope">{{ scope.row.problemIds?.length || 0 }}</template>
                    </el-table-column>
                    <el-table-column label="开始时间" min-width="170">
                      <template #default="scope">{{ formatDateTime(scope.row.startTime) }}</template>
                    </el-table-column>
                    <el-table-column label="截止时间" min-width="170">
                      <template #default="scope">{{ formatDateTime(scope.row.deadline) }}</template>
                    </el-table-column>
                    <el-table-column label="状态" width="100">
                      <template #default="scope">
                        <el-tag :type="getClassStatusType(scope.row.status)" effect="light">
                          {{ getClassStatusText(scope.row.status) }}
                        </el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column label="操作" width="170" fixed="right">
                      <template #default="scope">
                        <el-button type="primary" link @click="handleOpenAssignmentDetail(activeClassState, scope.row)">提交/排行</el-button>
                        <el-button type="success" link @click="handleEditAssignment(activeClassState, scope.row)">编辑</el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                </el-tab-pane>

                <el-tab-pane label="讨论区" name="discussions">
                  <el-form :model="activeClassState.discussionForm" label-position="top" class="discussion-form">
                    <div class="problem-form__grid">
                      <el-form-item label="关联任务">
                        <el-select v-model="activeClassState.discussionForm.assignmentId" placeholder="可不关联任务" clearable>
                          <el-option
                            v-for="item in activeClassState.assignments"
                            :key="item.id"
                            :label="item.title"
                            :value="item.id"
                          />
                        </el-select>
                      </el-form-item>
                      <el-form-item label="是否匿名">
                        <el-select v-model="activeClassState.discussionForm.isAnonymous" placeholder="是否匿名">
                          <el-option :value="0" label="否" />
                          <el-option :value="1" label="是" />
                        </el-select>
                      </el-form-item>
                    </div>
                    <el-form-item label="讨论标题">
                      <el-input v-model="activeClassState.discussionForm.title" placeholder="请输入讨论标题" />
                    </el-form-item>
                    <el-form-item label="讨论内容">
                      <el-input
                        v-model="activeClassState.discussionForm.content"
                        type="textarea"
                        :rows="4"
                        placeholder="请输入讨论内容"
                      />
                    </el-form-item>
                    <div class="class-detail__toolbar">
                      <el-button type="primary" :loading="activeClassState.discussionLoading" @click="handleCreateDiscussion(activeClassState)">
                        发布讨论
                      </el-button>
                    </div>
                  </el-form>

                  <div class="discussion-list" v-loading="activeClassState.discussionLoading">
                    <div v-for="item in activeClassState.discussions" :key="item.id" class="discussion-list__item">
                      <div>
                        <strong>{{ item.title }}</strong>
                        <span>{{ item.isAnonymous ? '匿名用户' : item.nickname || item.username || item.userId }}</span>
                      </div>
                      <p>{{ item.content }}</p>
                      <small>{{ formatDateTime(item.createdAt) }}</small>
                    </div>
                    <el-empty v-if="!activeClassState.discussions.length" description="暂无讨论" />
                  </div>
                </el-tab-pane>

                <el-tab-pane :label="activeClassState.type === 1 ? '题目池' : '竞赛题目'" name="problems">
                  <div class="problem-form__section-header">
                    <strong>{{ getResourceName(activeClassState) }}题目</strong>
                    <div class="class-detail__toolbar class-detail__toolbar--inline">
                      <el-button type="success" @click="handleCreateScopedProblem(activeClassState)">新增题目</el-button>
                      <el-button type="primary" plain @click="handleOpenAssociateDialog(activeClassState)">关联已有题目</el-button>
                    </div>
                  </div>
                  <div class="filter-grid class-detail__filter">
                    <el-input
                      v-model="activeClassState.scopedProblemFilter.keyword"
                      placeholder="输入题目关键词"
                      clearable
                      size="large"
                    />
                    <el-select v-model="activeClassState.scopedProblemFilter.difficulty" placeholder="选择难度" clearable size="large">
                      <el-option
                        v-for="item in difficultyOptions"
                        :key="item.label"
                        :label="item.label"
                        :value="item.value"
                      />
                    </el-select>
                    <el-select v-model="activeClassState.scopedProblemFilter.status" placeholder="选择状态" clearable size="large">
                      <el-option
                        v-for="item in problemStatusOptions"
                        :key="item.label"
                        :label="item.label"
                        :value="item.value"
                      />
                    </el-select>
                    <el-button type="primary" size="large" @click="handleScopedProblemSearch(activeClassState)">搜索题目</el-button>
                    <el-button size="large" @click="handleScopedProblemReset(activeClassState)">重置</el-button>
                  </div>
                  <el-table :data="activeClassState.scopedProblems" v-loading="activeClassState.problemLoading" width="100%">
                    <el-table-column prop="problemNo" label="题号" width="120" />
                    <el-table-column label="题目" min-width="220" show-overflow-tooltip>
                      <template #default="scope">{{ getProblemTitle(scope.row) }}</template>
                    </el-table-column>
                    <el-table-column label="难度" width="110">
                      <template #default="scope">
                        <el-tag :type="getDifficultyType(scope.row.difficulty)" effect="light">
                          {{ getDifficultyText(scope.row.difficulty) }}
                        </el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column label="状态" width="100">
                      <template #default="scope">
                        <el-tag :type="getProblemStatusType(scope.row.status)" effect="light">
                          {{ getProblemStatusText(scope.row.status) }}
                        </el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column label="操作" width="180" fixed="right">
                      <template #default="scope">
                        <el-button type="primary" link @click="handleViewScopedProblem(activeClassState, scope.row)">详情</el-button>
                        <el-button type="success" link @click="handleEditScopedProblem(activeClassState, scope.row)">编辑</el-button>
                        <el-button type="danger" link @click="handleRemoveScopedProblem(activeClassState, scope.row)">移除</el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                  <div class="table-pagination">
                    <el-pagination
                      background
                      layout="total, sizes, prev, pager, next, jumper"
                      :current-page="activeClassState.scopedProblemFilter.pageNum"
                      :page-size="activeClassState.scopedProblemFilter.pageSize"
                      :page-sizes="[10, 20, 50]"
                      :total="activeClassState.scopedProblemTotal"
                      @current-change="(page: number) => handleScopedProblemPageChange(activeClassState, page)"
                      @size-change="(size: number) => handleScopedProblemSizeChange(activeClassState, size)"
                    />
                  </div>
                </el-tab-pane>
              </el-tabs>
            </template>
          </div>
        </SectionCard>

        <SectionCard v-else-if="currentOverlay === 'class-form'">
          <el-form :model="activeClassState.form" label-position="top" class="class-resource-form">
            <div class="problem-form__grid">
              <el-form-item :label="`${getResourceName(activeClassState)}名称`">
                <el-input v-model="activeClassState.form.name" :placeholder="`请输入${getResourceName(activeClassState)}名称`" />
              </el-form-item>
              <el-form-item label="加入方式">
                <el-select v-model="activeClassState.form.joinMode" placeholder="选择加入方式">
                  <el-option
                    v-for="item in joinModeOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </div>
            <el-form-item :label="`${getResourceName(activeClassState)}封面`">
              <ImageUploadField v-model="activeClassState.form.coverImage" plus-only />
            </el-form-item>
            <div class="problem-form__grid">
              <el-form-item label="邀请码">
                <el-input v-model="activeClassState.form.inviteCode" placeholder="邀请码加入时填写" />
              </el-form-item>
              <el-form-item v-if="activeClassState.isEditMode" label="状态">
                <el-select v-model="activeClassState.form.status" placeholder="选择状态">
                  <el-option
                    v-for="item in editClassStatusOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </div>
            <div v-if="activeClassState.type === 2" class="problem-form__grid">
              <el-form-item label="开始时间">
                <el-date-picker
                  v-model="activeClassState.form.startTime"
                  type="datetime"
                  value-format="YYYY-MM-DDTHH:mm:ss"
                  placeholder="竞赛开始时间（罚时基准）"
                />
              </el-form-item>
              <el-form-item label="结束时间">
                <el-date-picker
                  v-model="activeClassState.form.endTime"
                  type="datetime"
                  value-format="YYYY-MM-DDTHH:mm:ss"
                  placeholder="竞赛结束时间（可留空）"
                />
              </el-form-item>
            </div>
            <el-form-item label="说明">
              <el-input
                v-model="activeClassState.form.description"
                type="textarea"
                :rows="4"
                :placeholder="`请输入${getResourceName(activeClassState)}说明`"
              />
            </el-form-item>
          </el-form>
          <div class="dialog-footer">
            <el-button @click="closeOverlay">取消</el-button>
            <el-button type="primary" :loading="activeClassState.submitLoading" @click="handleSubmitClassResource(activeClassState)">
              保存
            </el-button>
          </div>
        </SectionCard>

        <SectionCard v-else-if="currentOverlay === 'submission-detail'">
          <div v-loading="submissionLoading" class="practice-detail">
            <template v-if="currentSubmission">
              <DetailMetricsGrid
                :items="[
                  { label: '提交ID', value: currentSubmission.id },
                  { label: '题目ID', value: currentSubmission.problemId },
                  { label: '用户ID', value: currentSubmission.userId },
                  { label: '语言', value: currentSubmission.language },
                ]"
              />
              <DetailMetricsGrid
                :items="[
                  { label: '判题状态', value: getSubmissionStatusText(currentSubmission.status) },
                  { label: '执行耗时', value: `${currentSubmission.executionTime} ms` },
                  { label: '内存使用', value: `${currentSubmission.memoryUsed} KB` },
                  { label: '通过率', value: `${currentSubmission.passRate ?? 0}%` },
                ]"
              />
              <div class="practice-detail__item">
                <span>代码</span>
                <pre>{{ currentSubmission.code }}</pre>
              </div>
            </template>
          </div>
        </SectionCard>

        <SectionCard v-else-if="currentOverlay === 'assignment-detail'">
          <div class="practice-detail">
            <DetailMetricsGrid
              v-if="assignmentContext"
              :items="[
                { label: '作业标题', value: assignmentContext.assignment.title },
                { label: '题目总数', value: assignmentDetail?.problems?.length ?? assignmentContext.assignment.problemIds?.length ?? 0 },
                { label: '每题限次', value: assignmentContext.assignment.maxAttempts && assignmentContext.assignment.maxAttempts > 0 ? assignmentContext.assignment.maxAttempts : '不限' },
                { label: '榜单', value: assignmentContext.assignment.isPublicRank === 1 ? '公开' : '不公开' },
              ]"
            />

            <el-tabs v-model="assignmentDetailTab" class="class-detail__tabs">
              <el-tab-pane label="提交记录" name="submissions">
                <div class="class-detail__toolbar">
                  <el-input
                    v-model="assignmentSubmissionFilter.userId"
                    placeholder="按用户ID筛选"
                    clearable
                    style="width: 200px"
                    @keyup.enter="handleAssignmentSubmissionSearch"
                  />
                  <el-select
                    v-model="assignmentSubmissionFilter.status"
                    placeholder="全部状态"
                    clearable
                    style="width: 160px"
                  >
                    <el-option :value="1" label="已完成" />
                    <el-option :value="0" label="未完成" />
                    <el-option :value="2" label="超时未交" />
                  </el-select>
                  <el-button type="primary" @click="handleAssignmentSubmissionSearch">查询</el-button>
                  <el-button @click="handleAssignmentSubmissionReset">重置</el-button>
                </div>
                <el-table :data="assignmentSubmissions" v-loading="assignmentSubmissionsLoading" width="100%">
                  <el-table-column label="学生" min-width="160" show-overflow-tooltip>
                    <template #default="scope">{{ scope.row.nickname || scope.row.userId }}</template>
                  </el-table-column>
                  <el-table-column label="题目" min-width="200" show-overflow-tooltip>
                    <template #default="scope">{{ scope.row.problemTitle || scope.row.problemId }}</template>
                  </el-table-column>
                  <el-table-column label="状态" width="110">
                    <template #default="scope">
                      <el-tag :type="scope.row.status === 1 ? 'success' : scope.row.status === 2 ? 'danger' : 'info'" effect="light">
                        {{ scope.row.status === 1 ? '已完成' : scope.row.status === 2 ? '超时未交' : '未完成' }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="score" label="得分" width="100" />
                  <el-table-column label="提交时间" min-width="170">
                    <template #default="scope">{{ formatDateTime(scope.row.submittedAt) }}</template>
                  </el-table-column>
                  <el-table-column label="操作" width="110" fixed="right">
                    <template #default="scope">
                      <el-button
                        v-if="scope.row.submissionId"
                        type="primary"
                        link
                        @click="handleViewSubmission({ id: String(scope.row.submissionId) })"
                      >
                        查看提交
                      </el-button>
                      <span v-else>-</span>
                    </template>
                  </el-table-column>
                </el-table>
                <el-pagination
                  v-if="assignmentSubmissionsTotal > 0"
                  :current-page="assignmentSubmissionFilter.pageNum"
                  :page-size="assignmentSubmissionFilter.pageSize"
                  :total="assignmentSubmissionsTotal"
                  layout="total, prev, pager, next"
                  @current-change="handleAssignmentSubmissionPageChange"
                />
              </el-tab-pane>

              <el-tab-pane label="排行榜" name="ranking">
                <div v-loading="assignmentRankingLoading">
                  <el-alert
                    v-if="assignmentRankingNotice"
                    :title="assignmentRankingNotice"
                    type="info"
                    :closable="false"
                    show-icon
                  />
                  <el-table v-else :data="assignmentRanking?.rows || []" width="100%">
                    <el-table-column prop="rank" label="名次" width="80">
                      <template #default="scope">
                        <el-tag v-if="scope.row.rank <= 3" type="warning" effect="light">{{ scope.row.rank }}</el-tag>
                        <span v-else>{{ scope.row.rank }}</span>
                      </template>
                    </el-table-column>
                    <el-table-column label="学生" min-width="180" show-overflow-tooltip>
                      <template #default="scope">{{ scope.row.nickname || scope.row.userId }}</template>
                    </el-table-column>
                    <el-table-column prop="solvedCount" label="通过题数" width="110" />
                    <el-table-column prop="score" label="得分" width="110" />
                    <el-table-column label="最近通过" min-width="170">
                      <template #default="scope">{{ scope.row.lastSubmitAt ? formatDateTime(scope.row.lastSubmitAt) : '-' }}</template>
                    </el-table-column>
                  </el-table>
                </div>
              </el-tab-pane>
            </el-tabs>
          </div>
        </SectionCard>
      </div>
    </template>

    <template v-else>
      <div class="filter-action-row">
        <el-button type="primary" @click="handleHeaderAction">{{ headerActionText }}</el-button>
      </div>

      <el-tabs v-model="activeTab" class="practice-tabs">
      <el-tab-pane label="OJ 题库" name="problems">
        <SectionCard class="practice-manage-page__filter">
          <div class="filter-grid">
            <el-input
              v-model="filterForm.keyword"
              placeholder="输入题号或题目标题关键词"
              clearable
              size="large"
            />
            <el-select v-model="filterForm.difficulty" placeholder="选择难度" clearable size="large">
              <el-option
                v-for="item in difficultyOptions"
                :key="item.label"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
            <el-select v-model="filterForm.status" placeholder="选择状态" clearable size="large">
              <el-option
                v-for="item in problemStatusOptions"
                :key="item.label"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
            <el-button type="primary" size="large" @click="handleSearch">搜索题目</el-button>
            <el-button size="large" @click="handleReset">重置</el-button>
          </div>
        </SectionCard>

        <SectionCard
          class="practice-manage-page__table"
          title="OJ 题库管理"
          description="查看题目基础信息、难度和状态。"
        >
          <el-table :data="problemList" v-loading="loading" width="100%">
            <el-table-column prop="problemNo" label="题号" width="120" />
            <el-table-column prop="title" label="题目标题" min-width="220" show-overflow-tooltip />
            <el-table-column label="难度" width="110">
              <template #default="scope">
                <el-tag :type="getDifficultyType(scope.row.difficulty)" effect="light">
                  {{ getDifficultyText(scope.row.difficulty) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="submitCount" label="提交数" width="90" />
            <el-table-column prop="acceptCount" label="通过数" width="90" />
            <el-table-column label="状态" width="100">
              <template #default="scope">
                <el-tag :type="getProblemStatusType(scope.row.status)" effect="light">
                  {{ getProblemStatusText(scope.row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" min-width="180">
              <template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="scope">
                <el-button type="primary" link @click="handleViewProblem(scope.row)">查看详情</el-button>
                <el-button type="success" link @click="handleEditProblemByRow(scope.row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="table-pagination">
            <el-pagination
              background
              layout="total, sizes, prev, pager, next, jumper"
              :current-page="filterForm.pageNum"
              :page-size="filterForm.pageSize"
              :page-sizes="[10, 20, 50]"
              :total="total"
              @current-change="handlePageChange"
              @size-change="handleSizeChange"
            />
          </div>
        </SectionCard>
      </el-tab-pane>

      <el-tab-pane label="提交记录" name="submissions">
        <SectionCard
          class="practice-manage-page__table"
          title="提交记录"
          description="查看用户提交和判题结果。"
        >
          <div class="filter-grid">
            <el-input
              v-model="submissionFilter.problemId"
              placeholder="按题目ID筛选"
              clearable
              size="large"
            />
            <el-input
              v-model="submissionFilter.userId"
              placeholder="按用户ID筛选"
              clearable
              size="large"
            />
            <el-select v-model="submissionFilter.status" placeholder="选择判题状态" clearable size="large">
              <el-option :value="0" label="待判题" />
              <el-option :value="1" label="AC" />
              <el-option :value="2" label="WA" />
              <el-option :value="3" label="TLE" />
              <el-option :value="4" label="MLE" />
              <el-option :value="5" label="RE" />
              <el-option :value="6" label="CE" />
              <el-option :value="7" label="SE" />
            </el-select>
            <el-button type="primary" size="large" @click="handleSubmissionSearch">搜索记录</el-button>
            <el-button size="large" @click="handleSubmissionReset">重置</el-button>
          </div>
          <el-table :data="submissionList" v-loading="submissionLoading" width="100%">
            <el-table-column prop="id" label="提交ID" width="100" />
            <el-table-column prop="problemId" label="题目ID" width="100" />
            <el-table-column prop="userId" label="用户ID" width="100" />
            <el-table-column prop="language" label="语言" width="110" />
            <el-table-column label="状态" width="100">
              <template #default="scope">
                <el-tag :type="getSubmissionStatusType(scope.row.status)" effect="light">
                  {{ getSubmissionStatusText(scope.row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="提交时间" min-width="180">
              <template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="scope">
                <el-button type="primary" link @click="handleViewSubmission(scope.row)">查看详情</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="table-pagination">
            <el-pagination
              background
              layout="total, sizes, prev, pager, next, jumper"
              :current-page="submissionFilter.pageNum"
              :page-size="submissionFilter.pageSize"
              :page-sizes="[10, 20, 50]"
              :total="submissionTotal"
              @current-change="handleSubmissionPageChange"
              @size-change="handleSubmissionSizeChange"
            />
          </div>
        </SectionCard>
      </el-tab-pane>

      <el-tab-pane v-for="tab in resourceTabs" :key="tab.name" :label="tab.label" :name="tab.name">
        <SectionCard
          class="practice-manage-page__table"
          :title="tab.label"
          :description="`管理${getResourceName(tab.state)}基础信息、成员、${tab.state.type === 1 ? '作业、' : ''}题目与讨论。`"
        >
          <div class="filter-grid">
            <el-input
              v-model="tab.state.filter.keyword"
              :placeholder="`输入${getResourceName(tab.state)}名称关键词`"
              clearable
              size="large"
            />
            <el-select v-model="tab.state.filter.status" placeholder="选择状态" clearable size="large">
              <el-option
                v-for="item in classStatusOptions"
                :key="item.label"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
            <el-button type="primary" size="large" @click="handleClassResourceSearch(tab.state)">搜索</el-button>
            <el-button size="large" @click="handleClassResourceReset(tab.state)">重置</el-button>
            <el-button type="success" size="large" @click="handleCreateClassResource(tab.state)">
              新建{{ getResourceName(tab.state) }}
            </el-button>
          </div>

          <el-table :data="tab.state.list" v-loading="tab.state.loading" width="100%">
            <el-table-column prop="name" :label="`${getResourceName(tab.state)}名称`" min-width="220" show-overflow-tooltip />
            <el-table-column label="加入方式" width="120">
              <template #default="scope">{{ getJoinModeText(scope.row.joinMode) }}</template>
            </el-table-column>
            <el-table-column label="成员数" width="100">
              <template #default="scope">{{ scope.row.memberCount ?? 0 }}</template>
            </el-table-column>
            <el-table-column label="创建者" min-width="140" show-overflow-tooltip>
              <template #default="scope">{{ getClassCreatorName(scope.row) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="scope">
                <el-tag :type="getClassStatusType(scope.row.status)" effect="light">
                  {{ getClassStatusText(scope.row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" min-width="180">
              <template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="260" fixed="right">
              <template #default="scope">
                <el-button type="primary" link @click="handleOpenClassDetail(tab.state, scope.row)">详情</el-button>
                <el-button type="success" link @click="handleEditClassResource(tab.state, scope.row)">编辑</el-button>
                <el-button type="warning" link @click="handleOpenJoinDialog(tab.state, scope.row)">加入</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="table-pagination">
            <el-pagination
              background
              layout="total, sizes, prev, pager, next, jumper"
              :current-page="tab.state.filter.pageNum"
              :page-size="tab.state.filter.pageSize"
              :page-sizes="[10, 20, 50]"
              :total="tab.state.total"
              @current-change="(page: number) => handleClassPageChange(tab.state, page)"
              @size-change="(size: number) => handleClassSizeChange(tab.state, size)"
            />
          </div>
        </SectionCard>
      </el-tab-pane>
    </el-tabs>
    </template>

    <template v-for="tab in resourceTabs" :key="`${tab.name}-dialogs`">
      <el-dialog v-model="tab.state.joinVisible" :title="`加入${getResourceName(tab.state)}`" width="560px">
        <el-form :model="tab.state.joinForm" label-position="top">
          <el-form-item label="邀请码">
            <el-input v-model="tab.state.joinForm.inviteCode" placeholder="公开加入可不填" />
          </el-form-item>
          <el-form-item label="申请说明">
            <el-input
              v-model="tab.state.joinForm.message"
              type="textarea"
              :rows="4"
              placeholder="需要审核时可填写申请说明"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <div class="dialog-footer">
            <el-button @click="tab.state.joinVisible = false">取消</el-button>
            <el-button type="primary" :loading="tab.state.joinLoading" @click="handleJoinClassResource(tab.state)">
              提交
            </el-button>
          </div>
        </template>
      </el-dialog>

      <el-dialog v-model="tab.state.associateVisible" :title="`关联已有题目到${getResourceName(tab.state)}`" width="960px">
        <div class="filter-grid class-detail__filter">
          <el-input
            v-model="tab.state.candidateProblemFilter.keyword"
            placeholder="输入题号或题目标题关键词"
            clearable
            size="large"
          />
          <el-select v-model="tab.state.candidateProblemFilter.difficulty" placeholder="选择难度" clearable size="large">
            <el-option
              v-for="item in difficultyOptions"
              :key="item.label"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <el-select v-model="tab.state.candidateProblemFilter.status" placeholder="选择状态" clearable size="large">
            <el-option
              v-for="item in problemStatusOptions"
              :key="item.label"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <el-button type="primary" size="large" @click="handleCandidateProblemSearch(tab.state)">搜索题目</el-button>
          <el-button size="large" @click="handleCandidateProblemReset(tab.state)">重置</el-button>
        </div>
        <el-table
          :data="tab.state.candidateProblems"
          v-loading="tab.state.associateLoading"
          width="100%"
          @selection-change="(rows: OjProblemVO[]) => handleCandidateSelectionChange(tab.state, rows)"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column prop="problemNo" label="题号" width="120" />
          <el-table-column label="题目" min-width="220" show-overflow-tooltip>
            <template #default="scope">{{ getProblemTitle(scope.row) }}</template>
          </el-table-column>
          <el-table-column label="难度" width="110">
            <template #default="scope">
              <el-tag :type="getDifficultyType(scope.row.difficulty)" effect="light">
                {{ getDifficultyText(scope.row.difficulty) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="scope">
              <el-tag :type="getProblemStatusType(scope.row.status)" effect="light">
                {{ getProblemStatusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="提交数" width="90">
            <template #default="scope">{{ scope.row.submitCount ?? 0 }}</template>
          </el-table-column>
        </el-table>
        <div class="table-pagination">
          <el-pagination
            background
            layout="total, sizes, prev, pager, next, jumper"
            :current-page="tab.state.candidateProblemFilter.pageNum"
            :page-size="tab.state.candidateProblemFilter.pageSize"
            :page-sizes="[10, 20, 50]"
            :total="tab.state.candidateProblemTotal"
            @current-change="(page: number) => handleCandidateProblemPageChange(tab.state, page)"
            @size-change="(size: number) => handleCandidateProblemSizeChange(tab.state, size)"
          />
        </div>
        <template #footer>
          <div class="dialog-footer">
            <el-button @click="tab.state.associateVisible = false">取消</el-button>
            <el-button type="primary" :loading="tab.state.associateSubmitLoading" @click="handleAssociateProblems(tab.state)">
              确认关联
            </el-button>
          </div>
        </template>
      </el-dialog>

      <el-dialog
        v-model="tab.state.assignmentVisible"
        :title="tab.state.assignmentEditId ? '编辑作业' : '新增作业'"
        width="760px"
      >
        <el-form :model="tab.state.assignmentForm" label-position="top" class="class-resource-form">
          <div class="problem-form__grid">
            <el-form-item label="作业标题">
              <el-input v-model="tab.state.assignmentForm.title" placeholder="请输入作业标题" />
            </el-form-item>
            <el-form-item label="作业类型">
              <el-select v-model="tab.state.assignmentForm.type" placeholder="选择作业类型">
                <el-option
                  v-for="item in assignmentTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </div>
          <el-form-item label="作业题目（来自班级题目池）">
            <div class="assignment-problem-picker">
              <el-input
                v-model="tab.state.assignmentProblemKeyword"
                placeholder="输入题号或题目标题筛选"
                clearable
              />
              <el-table
                :data="assignmentProblemPool(tab.state)"
                v-loading="tab.state.problemPoolLoading"
                size="small"
                max-height="260"
                width="100%"
              >
                <el-table-column label="选择" width="70">
                  <template #default="scope">
                    <el-checkbox
                      :model-value="isAssignmentProblemSelected(tab.state, scope.row.id)"
                      @change="(checked: CheckboxValueType) => toggleAssignmentProblem(tab.state, scope.row.id, checked)"
                    />
                  </template>
                </el-table-column>
                <el-table-column prop="problemNo" label="题号" width="120" />
                <el-table-column label="题目" min-width="200" show-overflow-tooltip>
                  <template #default="scope">{{ getProblemTitle(scope.row) }}</template>
                </el-table-column>
                <el-table-column label="难度" width="100">
                  <template #default="scope">
                    <el-tag :type="getDifficultyType(scope.row.difficulty)" effect="light">
                      {{ getDifficultyText(scope.row.difficulty) }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
              <el-empty
                v-if="!tab.state.problemPoolLoading && !tab.state.problemPool.length"
                description="该班级题目池为空，请先在“题目池”中关联题目"
                :image-size="60"
              />
              <div class="assignment-problem-picker__selected">
                已选 {{ tab.state.assignmentForm.problemIds.length }} 题
              </div>
            </div>
          </el-form-item>
          <div class="problem-form__grid">
            <el-form-item label="开始时间">
              <el-date-picker
                v-model="tab.state.assignmentForm.startTime"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ss"
                placeholder="选择开始时间"
              />
            </el-form-item>
            <el-form-item label="截止时间">
              <el-date-picker
                v-model="tab.state.assignmentForm.deadline"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ss"
                placeholder="选择截止时间"
              />
            </el-form-item>
          </div>
          <div class="problem-form__grid">
            <el-form-item label="最大提交次数">
              <el-input-number v-model="tab.state.assignmentForm.maxAttempts" :min="1" controls-position="right" />
            </el-form-item>
            <el-form-item label="公开排行">
              <el-select v-model="tab.state.assignmentForm.isPublicRank" placeholder="是否公开排行">
                <el-option :value="0" label="否" />
                <el-option :value="1" label="是" />
              </el-select>
            </el-form-item>
          </div>
          <el-form-item v-if="tab.state.assignmentEditId" label="状态">
            <el-select v-model="tab.state.assignmentForm.status" placeholder="选择状态">
              <el-option
                v-for="item in editClassStatusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="作业说明">
            <el-input
              v-model="tab.state.assignmentForm.description"
              type="textarea"
              :rows="4"
              placeholder="请输入作业说明"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <div class="dialog-footer">
            <el-button @click="tab.state.assignmentVisible = false">取消</el-button>
            <el-button type="primary" :loading="tab.state.submitLoading" @click="handleSubmitAssignment(tab.state)">
              保存
            </el-button>
          </div>
        </template>
      </el-dialog>
    </template>
  </div>
</template>
