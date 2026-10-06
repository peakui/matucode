<script setup lang="ts">
import type { FormInstance, FormRules, TagProps } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SectionCard from '@/components/SectionCard/index.vue'
import ImageUploadField from '@/components/ImageUploadField/index.vue'
import {
  createInterviewCategoryApi,
  createInterviewQuestionApi,
  deleteInterviewCategoryApi,
  getInterviewCategoryListApi,
  getInterviewCategoryPageApi,
  getInterviewCompanyListApi,
  getInterviewQuestionDetailApi,
  getInterviewQuestionListApi,
  updateInterviewCategoryApi,
  updateInterviewQuestionApi,
} from '@/api'
import type {
  CreateInterviewCategoryRequest,
  CreateInterviewQuestionRequest,
  InterviewCategoryVO,
  InterviewCompanyVO,
  InterviewQuestionVO,
} from '@/api/types'
import './index.scss'

const loading = ref(false)
const submitLoading = ref(false)
const detailLoading = ref(false)
const formVisible = ref(false)
const detailVisible = ref(false)
const categoryVisible = ref(false)
const isEditMode = ref(false)
const isEditCategoryMode = ref(false)
const total = ref(0)
const categoryTotal = ref(0)
const categoryOptions = ref<InterviewCategoryVO[]>([])
const companyOptions = ref<InterviewCompanyVO[]>([])
const tableData = ref<InterviewQuestionVO[]>([])
const categoryTableData = ref<InterviewCategoryVO[]>([])
const currentQuestion = ref<InterviewQuestionVO | null>(null)
const currentCategory = ref<InterviewCategoryVO | null>(null)
const questionFormRef = ref<FormInstance>()
type QuestionForm = CreateInterviewQuestionRequest & { frequency?: number }

const filterForm = reactive({ keyword: '', categoryId: undefined as string | undefined, companyId: undefined as string | undefined, difficulty: undefined as number | undefined, isLocked: undefined as number | undefined, pageNum: 1, pageSize: 10 })
const categoryFilterForm = reactive({ parentId: undefined as string | undefined, status: undefined as number | undefined, keyword: '', pageNum: 1, pageSize: 5 })
const questionForm = reactive<QuestionForm>({ questionNo: '', title: '', content: '', answer: '', categoryId: undefined, companyId: undefined, positionTags: [], difficulty: 1, frequency: 0, isLocked: 0, unlockDays: 0, status: 1 })
const categoryForm = reactive<CreateInterviewCategoryRequest>({ parentId: undefined, categoryName: '', categoryDesc: '', iconUrl: '', sortOrder: 0, status: 1 })
const questionRules: FormRules<QuestionForm> = {
  questionNo: [{ required: true, message: '请输入题目编号', trigger: 'blur' }],
  title: [{ required: true, message: '请输入题目标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入题目内容', trigger: 'blur' }],
  difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }],
}
const difficultyOptions = [{ label: '简单', value: 1 }, { label: '中等', value: 2 }, { label: '困难', value: 3 }]
const lockOptions = [{ label: '未锁定', value: 0 }, { label: '已锁定', value: 1 }]
const statusOptions = [{ label: '启用', value: 1 }, { label: '停用', value: 0 }]
const categoryStatusOptions = statusOptions

const formatDateTime = (value?: string) => (!value ? '-' : value.replace('T', ' '))
const getDifficultyText = (difficulty: number) => ({ 1: '简单', 2: '中等', 3: '困难' })[difficulty] || '未知'
const getDifficultyType = (difficulty: number): TagProps['type'] => (({ 1: 'success', 2: 'warning', 3: 'danger' })[difficulty] as TagProps['type']) || 'info'
const getLockText = (isLocked: number) => (isLocked ? '已锁定' : '未锁定')
const getLockType = (isLocked: number): TagProps['type'] => (isLocked ? 'danger' : 'success')
const getStatusText = (status?: number) => (status === 0 ? '停用' : '启用')
const getStatusType = (status?: number): TagProps['type'] => (status === 0 ? 'info' : 'success')
const getCategoryStatusText = getStatusText
const getCategoryStatusType = getStatusType
const getCategoryName = (categoryId?: string, categoryName?: string) => categoryName || categoryOptions.value.find((item) => item.id === categoryId)?.categoryName || '-'
const getCompanyName = (companyId?: string, companyName?: string) => companyName || companyOptions.value.find((item) => item.id === companyId)?.companyName || '-'

const resetQuestionForm = () => {
  questionForm.questionNo = ''
  questionForm.title = ''
  questionForm.content = ''
  questionForm.answer = ''
  questionForm.categoryId = undefined
  questionForm.companyId = undefined
  questionForm.positionTags = []
  questionForm.difficulty = 1
  questionForm.frequency = 0
  questionForm.isLocked = 0
  questionForm.unlockDays = 0
  questionForm.status = 1
}
const resetCategoryForm = () => {
  categoryForm.parentId = undefined
  categoryForm.categoryName = ''
  categoryForm.categoryDesc = ''
  categoryForm.iconUrl = ''
  categoryForm.sortOrder = 0
  categoryForm.status = 1
}

const fetchBaseOptions = async () => {
  const [categoryRes, companyRes] = await Promise.all([getInterviewCategoryListApi(), getInterviewCompanyListApi()])
  categoryOptions.value = categoryRes.data || []
  companyOptions.value = companyRes.data || []
}

const fetchCategoryPage = async () => {
  const res = await getInterviewCategoryPageApi({
    parentId: categoryFilterForm.parentId,
    status: categoryFilterForm.status,
    keyword: categoryFilterForm.keyword || undefined,
    pageNum: categoryFilterForm.pageNum,
    pageSize: categoryFilterForm.pageSize,
  })
  categoryTableData.value = res.data.records || []
  categoryTotal.value = res.data.total || 0
}

const fetchQuestionList = async () => {
  loading.value = true
  try {
    const res = await getInterviewQuestionListApi({ keyword: filterForm.keyword || undefined, categoryId: filterForm.categoryId, companyId: filterForm.companyId, difficulty: filterForm.difficulty, isLocked: filterForm.isLocked, pageNum: filterForm.pageNum, pageSize: filterForm.pageSize })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const fetchQuestionDetail = async (questionId: string) => {
  detailLoading.value = true
  try {
    const res = await getInterviewQuestionDetailApi(questionId)
    currentQuestion.value = res.data
    return res.data
  } finally {
    detailLoading.value = false
  }
}

const handleSearch = () => {
  filterForm.pageNum = 1
  void fetchQuestionList()
}
const handleReset = () => {
  filterForm.keyword = ''
  filterForm.categoryId = undefined
  filterForm.companyId = undefined
  filterForm.difficulty = undefined
  filterForm.isLocked = undefined
  filterForm.pageNum = 1
  void fetchQuestionList()
}
const handleCreate = () => {
  isEditMode.value = false
  currentQuestion.value = null
  resetQuestionForm()
  formVisible.value = true
}
const handleCreateCategory = () => {
  isEditCategoryMode.value = false
  currentCategory.value = null
  resetCategoryForm()
  categoryVisible.value = true
}
const handleEditCategory = (row: InterviewCategoryVO) => {
  isEditCategoryMode.value = true
  currentCategory.value = row
  categoryForm.parentId = row.parentId
  categoryForm.categoryName = row.categoryName
  categoryForm.categoryDesc = row.categoryDesc || ''
  categoryForm.iconUrl = row.iconUrl || ''
  categoryForm.sortOrder = row.sortOrder || 0
  categoryForm.status = row.status ?? 1
  categoryVisible.value = true
}
const handleDeleteCategory = async (row: InterviewCategoryVO) => {
  await ElMessageBox.confirm(`确认删除分类《${row.categoryName}》吗？`, '删除提示', { type: 'warning' })
  await deleteInterviewCategoryApi(row.id)
  ElMessage.success('分类删除成功')
  await Promise.all([fetchBaseOptions(), fetchCategoryPage()])
}
const handleSubmitCategory = async () => {
  submitLoading.value = true
  try {
    if (!categoryForm.categoryName.trim()) {
      ElMessage.warning('请填写分类名称')
      return
    }
    if (isEditCategoryMode.value && currentCategory.value) {
      await updateInterviewCategoryApi(currentCategory.value.id, { ...categoryForm })
      ElMessage.success('分类更新成功')
    } else {
      await createInterviewCategoryApi({ ...categoryForm })
      ElMessage.success('分类创建成功')
    }
    categoryVisible.value = false
    resetCategoryForm()
    await Promise.all([fetchBaseOptions(), fetchCategoryPage()])
  } finally {
    submitLoading.value = false
  }
}
const handleEdit = async (row: InterviewQuestionVO) => {
  isEditMode.value = true
  const detail = await fetchQuestionDetail(row.id)
  if (!detail) return
  currentQuestion.value = detail
  questionForm.questionNo = detail.questionNo || ''
  questionForm.title = detail.title
  questionForm.content = detail.content || ''
  questionForm.answer = detail.answer || ''
  questionForm.categoryId = detail.categoryId
  questionForm.companyId = detail.companyId
  questionForm.positionTags = detail.positionTags || []
  questionForm.difficulty = detail.difficulty
  questionForm.frequency = detail.frequency || 0
  questionForm.isLocked = detail.isLocked
  questionForm.unlockDays = detail.unlockDays || 0
  questionForm.status = detail.status ?? 1
  formVisible.value = true
}
const handleView = async (row: InterviewQuestionVO) => {
  detailVisible.value = true
  await fetchQuestionDetail(row.id)
}
const handleSubmit = async () => {
  await questionFormRef.value?.validate()
  submitLoading.value = true
  try {
    if (isEditMode.value && currentQuestion.value) {
      const { questionNo, ...updatePayload } = questionForm
      await updateInterviewQuestionApi(currentQuestion.value.id, { ...updatePayload })
      ElMessage.success('题目更新成功')
    } else {
      const { frequency, ...createPayload } = questionForm
      await createInterviewQuestionApi({ ...createPayload })
      ElMessage.success('题目创建成功')
    }
    formVisible.value = false
    resetQuestionForm()
    await fetchQuestionList()
  } finally {
    submitLoading.value = false
  }
}
const handlePageChange = (page: number) => { filterForm.pageNum = page; void fetchQuestionList() }
const handleSizeChange = (size: number) => { filterForm.pageSize = size; filterForm.pageNum = 1; void fetchQuestionList() }
const handleCategoryPageChange = (page: number) => { categoryFilterForm.pageNum = page; void fetchCategoryPage() }
const handleCategorySizeChange = (size: number) => { categoryFilterForm.pageSize = size; categoryFilterForm.pageNum = 1; void fetchCategoryPage() }

onMounted(async () => { await Promise.all([fetchBaseOptions(), fetchCategoryPage(), fetchQuestionList()]) })
</script>

<template>
  <div class="interview-manage-page">
    <SectionCard title="分类管理" description="维护面试题分类信息。">
      <template #header-extra>
        <el-button type="primary" @click="handleCreateCategory">新建分类</el-button>
      </template>
      <el-table :data="categoryTableData" width="100%">
        <el-table-column prop="categoryName" label="分类名称" min-width="180" />
        <el-table-column prop="categoryDesc" label="分类描述" min-width="220" show-overflow-tooltip />
        <el-table-column prop="sortOrder" label="排序" width="90" />
        <el-table-column label="状态" width="100"><template #default="scope"><el-tag :type="getCategoryStatusType(scope.row.status)" effect="light">{{ getCategoryStatusText(scope.row.status) }}</el-tag></template></el-table-column>
        <el-table-column label="创建时间" min-width="180"><template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template></el-table-column>
        <el-table-column label="操作" width="160" fixed="right"><template #default="scope"><el-button type="success" link @click="handleEditCategory(scope.row)">编辑</el-button><el-button type="danger" link @click="handleDeleteCategory(scope.row)">删除</el-button></template></el-table-column>
      </el-table>
      <div class="table-pagination">
        <el-pagination background layout="total, sizes, prev, pager, next, jumper" :current-page="categoryFilterForm.pageNum" :page-size="categoryFilterForm.pageSize" :page-sizes="[5, 10, 20]" :total="categoryTotal" @current-change="handleCategoryPageChange" @size-change="handleCategorySizeChange" />
      </div>
    </SectionCard>

    <SectionCard>
      <div class="filter-action-row">
        <el-button type="primary" @click="handleCreate">新建面试题</el-button>
      </div>
      <div class="filter-grid">
        <el-input v-model="filterForm.keyword" placeholder="输入题目关键词" clearable size="large" />
        <el-select v-model="filterForm.categoryId" placeholder="选择分类" clearable size="large"><el-option v-for="item in categoryOptions" :key="item.id" :label="item.categoryName" :value="item.id" /></el-select>
        <el-select v-model="filterForm.companyId" placeholder="选择公司" clearable size="large"><el-option v-for="item in companyOptions" :key="item.id" :label="item.companyName" :value="item.id" /></el-select>
        <el-select v-model="filterForm.difficulty" placeholder="选择难度" clearable size="large"><el-option v-for="item in difficultyOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select>
        <el-select v-model="filterForm.isLocked" placeholder="选择锁定状态" clearable size="large"><el-option v-for="item in lockOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select>
        <el-button type="primary" size="large" @click="handleSearch">搜索</el-button>
        <el-button size="large" @click="handleReset">重置</el-button>
      </div>
    </SectionCard>

    <SectionCard>
      <el-table :data="tableData" v-loading="loading" width="100%">
        <el-table-column prop="title" label="题目标题" min-width="260" show-overflow-tooltip />
        <el-table-column label="分类" width="140"><template #default="scope">{{ getCategoryName(scope.row.categoryId, scope.row.categoryName) }}</template></el-table-column>
        <el-table-column label="公司" width="140"><template #default="scope">{{ getCompanyName(scope.row.companyId, scope.row.companyName) }}</template></el-table-column>
        <el-table-column label="难度" width="110"><template #default="scope"><el-tag :type="getDifficultyType(scope.row.difficulty)" effect="light">{{ getDifficultyText(scope.row.difficulty) }}</el-tag></template></el-table-column>
        <el-table-column label="锁定状态" width="120"><template #default="scope"><el-tag :type="getLockType(scope.row.isLocked)" effect="light">{{ getLockText(scope.row.isLocked) }}</el-tag></template></el-table-column>
        <el-table-column prop="frequency" label="频次" width="90" />
        <el-table-column prop="viewCount" label="浏览数" width="90" />
        <el-table-column prop="collectCount" label="收藏数" width="90" />
        <el-table-column label="状态" width="100"><template #default="scope"><el-tag :type="getStatusType(scope.row.status)" effect="light">{{ getStatusText(scope.row.status) }}</el-tag></template></el-table-column>
        <el-table-column label="创建时间" min-width="180"><template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template></el-table-column>
        <el-table-column label="操作" width="160" fixed="right"><template #default="scope"><el-button type="primary" link @click="handleView(scope.row)">查看</el-button><el-button type="success" link @click="handleEdit(scope.row)">编辑</el-button></template></el-table-column>
      </el-table>
      <div class="table-pagination">
        <el-pagination background layout="total, sizes, prev, pager, next, jumper" :current-page="filterForm.pageNum" :page-size="filterForm.pageSize" :page-sizes="[10, 20, 50]" :total="total" @current-change="handlePageChange" @size-change="handleSizeChange" />
      </div>
    </SectionCard>

    <el-dialog v-model="categoryVisible" :title="isEditCategoryMode ? '编辑分类' : '新建分类'" width="760px">
      <div class="form-grid">
        <el-select v-model="categoryForm.parentId" placeholder="选择父分类" clearable><el-option v-for="item in categoryOptions" :key="item.id" :label="item.categoryName" :value="item.id" /></el-select>
        <el-input-number v-model="categoryForm.sortOrder" :min="0" controls-position="right" />
        <el-select v-model="categoryForm.status" placeholder="选择状态"><el-option v-for="item in categoryStatusOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select>
      </div>
      <div class="form-block"><span>分类封面</span><ImageUploadField v-model="categoryForm.iconUrl" plus-only /></div>
      <div class="form-block"><span>分类名称</span><el-input v-model="categoryForm.categoryName" placeholder="请输入分类名称" /></div>
      <div class="form-block"><span>分类描述</span><el-input v-model="categoryForm.categoryDesc" type="textarea" :rows="4" placeholder="请输入分类描述" /></div>
      <template #footer><div class="dialog-footer"><el-button @click="categoryVisible = false">取消</el-button><el-button type="primary" :loading="submitLoading" @click="handleSubmitCategory">{{ isEditCategoryMode ? '保存修改' : '确认创建' }}</el-button></div></template>
    </el-dialog>

    <el-dialog v-model="formVisible" :title="isEditMode ? '编辑面试题' : '新建面试题'" width="820px">
      <el-form ref="questionFormRef" :model="questionForm" :rules="questionRules" label-position="top">
        <div class="form-grid">
          <el-form-item label="题目编号" prop="questionNo">
            <el-input v-model="questionForm.questionNo" placeholder="题目编号，如 A1001" />
          </el-form-item>
          <el-form-item label="分类" prop="categoryId">
            <el-select v-model="questionForm.categoryId" placeholder="选择分类" clearable><el-option v-for="item in categoryOptions" :key="item.id" :label="item.categoryName" :value="item.id" /></el-select>
          </el-form-item>
          <el-form-item label="公司" prop="companyId">
            <el-select v-model="questionForm.companyId" placeholder="选择公司" clearable><el-option v-for="item in companyOptions" :key="item.id" :label="item.companyName" :value="item.id" /></el-select>
          </el-form-item>
          <el-form-item label="难度" prop="difficulty">
            <el-select v-model="questionForm.difficulty" placeholder="选择难度"><el-option v-for="item in difficultyOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select>
          </el-form-item>
          <el-form-item label="锁定状态" prop="isLocked">
            <el-select v-model="questionForm.isLocked" placeholder="选择锁定状态"><el-option v-for="item in lockOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select>
          </el-form-item>
          <el-form-item label="解锁天数" prop="unlockDays">
            <el-input-number v-model="questionForm.unlockDays" :min="0" controls-position="right" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="questionForm.status" placeholder="选择状态"><el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select>
          </el-form-item>
          <el-form-item v-if="isEditMode" label="出现频次" prop="frequency">
            <el-input-number v-model="questionForm.frequency" :min="0" controls-position="right" />
          </el-form-item>
        </div>
        <el-form-item label="岗位标签" prop="positionTags" class="form-block">
          <el-select v-model="questionForm.positionTags" multiple filterable allow-create default-first-option placeholder="输入后回车添加岗位标签"><el-option v-for="tag in questionForm.positionTags" :key="tag" :label="tag" :value="tag" /></el-select>
        </el-form-item>
        <el-form-item label="题目标题" prop="title" class="form-block"><el-input v-model="questionForm.title" placeholder="请输入题目标题" /></el-form-item>
        <el-form-item label="题目内容" prop="content" class="form-block"><el-input v-model="questionForm.content" type="textarea" :rows="8" placeholder="请输入题目内容" /></el-form-item>
        <el-form-item label="参考答案" prop="answer" class="form-block"><el-input v-model="questionForm.answer" type="textarea" :rows="6" placeholder="请输入参考答案" /></el-form-item>
      </el-form>
      <template #footer><div class="dialog-footer"><el-button @click="formVisible = false">取消</el-button><el-button type="primary" :loading="submitLoading" @click="handleSubmit">{{ isEditMode ? '保存修改' : '确认创建' }}</el-button></div></template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="题目详情" width="820px">
      <div v-loading="detailLoading" class="detail-panel">
        <template v-if="currentQuestion">
          <div class="detail-panel__item"><span>题目标题</span><strong>{{ currentQuestion.title }}</strong></div>
          <div class="detail-panel__item"><span>题目内容</span><p>{{ currentQuestion.content || '-' }}</p></div>
          <div class="detail-panel__item"><span>参考答案</span><p>{{ currentQuestion.answer || '-' }}</p></div>
          <div class="detail-grid">
            <div class="detail-panel__item"><span>题目编号</span><strong>{{ currentQuestion.questionNo || '-' }}</strong></div>
            <div class="detail-panel__item"><span>发布者 ID</span><strong>{{ currentQuestion.publisherId || '-' }}</strong></div>
            <div class="detail-panel__item"><span>分类</span><strong>{{ getCategoryName(currentQuestion.categoryId, currentQuestion.categoryName) }}</strong></div>
            <div class="detail-panel__item"><span>公司</span><strong>{{ getCompanyName(currentQuestion.companyId, currentQuestion.companyName) }}</strong></div>
            <div class="detail-panel__item"><span>岗位标签</span><strong>{{ currentQuestion.positionTags?.join('、') || '-' }}</strong></div>
            <div class="detail-panel__item"><span>难度</span><strong>{{ getDifficultyText(currentQuestion.difficulty) }}</strong></div>
            <div class="detail-panel__item"><span>出现频次</span><strong>{{ currentQuestion.frequency ?? 0 }}</strong></div>
            <div class="detail-panel__item"><span>浏览数</span><strong>{{ currentQuestion.viewCount ?? 0 }}</strong></div>
            <div class="detail-panel__item"><span>收藏数</span><strong>{{ currentQuestion.collectCount ?? 0 }}</strong></div>
            <div class="detail-panel__item"><span>锁定状态</span><strong>{{ getLockText(currentQuestion.isLocked) }}</strong></div>
            <div class="detail-panel__item"><span>解锁天数</span><strong>{{ currentQuestion.unlockDays ?? 0 }}</strong></div>
            <div class="detail-panel__item"><span>状态</span><strong>{{ getStatusText(currentQuestion.status) }}</strong></div>
            <div class="detail-panel__item"><span>创建时间</span><strong>{{ formatDateTime(currentQuestion.createdAt) }}</strong></div>
            <div class="detail-panel__item"><span>更新时间</span><strong>{{ formatDateTime(currentQuestion.updatedAt) }}</strong></div>
          </div>
        </template>
      </div>
    </el-dialog>
  </div>
</template>
