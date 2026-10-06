<script setup lang="ts">
import type { TagProps } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SectionCard from '@/components/SectionCard/index.vue'
import DetailMetricsGrid from '@/components/DetailMetricsGrid/index.vue'
import QAEditorForm from '@/components/QAEditorForm/index.vue'
import {
  acceptQaAnswerApi,
  createQaQuestionApi,
  deleteQaAnswerApi,
  deleteQaQuestionApi,
  getQaAnswerListApi,
  getQaCategoryListApi,
  getQaQuestionDetailApi,
  getQaQuestionListApi,
  getQaWordCloudApi,
  updateQaQuestionApi,
} from '@/api'
import type {
  CreateQuestionRequest,
  QaAnswerVO,
  QaCategoryVO,
  QaQuestionDetailVO,
  QaQuestionListItemVO,
  QaWordCloudVO,
  UpdateQuestionRequest,
} from '@/api/types'
import './index.scss'

const loading = ref(false)
const detailLoading = ref(false)
const answerLoading = ref(false)
const submitLoading = ref(false)
const qaFormRef = ref<InstanceType<typeof QAEditorForm> | null>(null)
const detailVisible = ref(false)
const formVisible = ref(false)
const answerVisible = ref(false)
const isEditMode = ref(false)
const total = ref(0)
const answerTotal = ref(0)
const tableData = ref<QaQuestionListItemVO[]>([])
const currentAnswers = ref<QaAnswerVO[]>([])
const categoryOptions = ref<QaCategoryVO[]>([])
const currentQuestion = ref<QaQuestionDetailVO | null>(null)
const currentWordCloud = ref<QaWordCloudVO | null>(null)
const filterForm = reactive({
  categoryId: undefined as string | undefined,
  status: undefined as number | undefined,
  keyword: '',
  sortBy: 'latest',
  pageNum: 1,
  pageSize: 10,
})
const answerQuery = reactive({ pageNum: 1, pageSize: 10 })
const questionForm = reactive<CreateQuestionRequest & UpdateQuestionRequest>({
  categoryId: undefined,
  title: '',
  content: '',
  bountyPoints: 0,
  status: 0,
})
const statusOptions = [
  { label: '全部状态', value: undefined },
  { label: '待解决', value: 0 },
  { label: '已解决', value: 1 },
  { label: '已关闭', value: 2 },
  { label: '已删除', value: 3 },
]
const editStatusOptions = [
  { label: '待解决', value: 0 },
  { label: '已解决', value: 1 },
  { label: '已关闭', value: 2 },
  { label: '已删除', value: 3 },
]
const sortOptions = [
  { label: '最新发布', value: 'latest' },
  { label: '悬赏优先', value: 'bounty' },
  { label: '最多回答', value: 'most_answered' },
  { label: '最多浏览', value: 'most_viewed' },
]
const formatDateTime = (value?: string) => (!value ? '-' : value.replace('T', ' '))
const getStatusText = (status: number) =>
  ({ 0: '待解决', 1: '已解决', 2: '已关闭', 3: '已删除' })[status] || '未知'
const getStatusType = (status: number): TagProps['type'] =>
  (({ 0: 'warning', 1: 'success', 2: 'info', 3: 'danger' })[status] as TagProps['type']) || 'info'
const getAnswerStatusText = (status: number) =>
  ({ 0: '待审核', 1: '显示', 2: '隐藏', 3: '删除' })[status] || '未知'
const getAnswerStatusType = (status: number): TagProps['type'] =>
  (({ 0: 'warning', 1: 'success', 2: 'info', 3: 'danger' })[status] as TagProps['type']) || 'info'
const getCategoryName = (categoryId?: string) =>
  categoryOptions.value.find((item) => item.id === categoryId)?.categoryName || '-'
const resetQuestionForm = () => {
  questionForm.categoryId = undefined
  questionForm.title = ''
  questionForm.content = ''
  questionForm.bountyPoints = 0
  questionForm.status = 0
}
const fetchCategoryList = async () => {
  const res = await getQaCategoryListApi()
  categoryOptions.value = res.data || []
}
const fetchQuestionList = async () => {
  loading.value = true
  try {
    const res = await getQaQuestionListApi({
      ...filterForm,
      keyword: filterForm.keyword || undefined,
    })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}
const fetchQuestionDetail = async (questionId: string) => {
  detailLoading.value = true
  try {
    const [detailRes, wordCloudRes] = await Promise.all([
      getQaQuestionDetailApi(questionId),
      getQaWordCloudApi(questionId).catch(() => ({ data: null })),
    ])
    currentQuestion.value = detailRes.data
    currentWordCloud.value = wordCloudRes.data
  } finally {
    detailLoading.value = false
  }
}
const fetchAnswerList = async (questionId: string) => {
  answerLoading.value = true
  try {
    const res = await getQaAnswerListApi(questionId, answerQuery)
    currentAnswers.value = res.data.records || []
    answerTotal.value = res.data.total || 0
  } finally {
    answerLoading.value = false
  }
}
const handleSearch = () => {
  filterForm.pageNum = 1
  void fetchQuestionList()
}
const handleReset = () => {
  filterForm.categoryId = undefined
  filterForm.status = undefined
  filterForm.keyword = ''
  filterForm.sortBy = 'latest'
  filterForm.pageNum = 1
  void fetchQuestionList()
}
const handleCreate = () => {
  isEditMode.value = false
  resetQuestionForm()
  formVisible.value = true
}
const handleEdit = async (row: QaQuestionListItemVO) => {
  isEditMode.value = true
  formVisible.value = true
  const res = await getQaQuestionDetailApi(row.id)
  currentQuestion.value = res.data
  questionForm.categoryId = res.data.categoryId
  questionForm.title = res.data.title
  questionForm.content = res.data.content || ''
  questionForm.bountyPoints = res.data.bountyPoints || 0
  questionForm.status = res.data.status
}
const handleSubmit = async () => {
  await qaFormRef.value?.validate()
  submitLoading.value = true
  try {
    if (isEditMode.value && currentQuestion.value) {
      await updateQaQuestionApi(currentQuestion.value.id, { ...questionForm })
      ElMessage.success('问题更新成功')
    } else {
      await createQaQuestionApi({
        categoryId: questionForm.categoryId,
        title: questionForm.title || '',
        content: questionForm.content || '',
        bountyPoints: questionForm.bountyPoints || 0,
      })
      ElMessage.success('问题创建成功')
    }
    formVisible.value = false
    resetQuestionForm()
    await fetchQuestionList()
  } finally {
    submitLoading.value = false
  }
}
const handleView = async (row: QaQuestionListItemVO) => {
  detailVisible.value = true
  await fetchQuestionDetail(row.id)
}
const handleAnswerManage = async (row: QaQuestionListItemVO) => {
  currentQuestion.value = { ...row } as QaQuestionDetailVO
  answerVisible.value = true
  answerQuery.pageNum = 1
  await fetchAnswerList(row.id)
}
const handleAcceptAnswer = async (answer: QaAnswerVO) => {
  if (!currentQuestion.value) return
  await acceptQaAnswerApi(currentQuestion.value.id, answer.id)
  ElMessage.success('采纳成功')
  await fetchAnswerList(currentQuestion.value.id)
}
const handleDeleteAnswer = async (answer: QaAnswerVO) => {
  await ElMessageBox.confirm('确认删除该回答吗？', '删除回答', { type: 'warning' })
  await deleteQaAnswerApi(answer.id)
  ElMessage.success('删除成功')
  if (currentQuestion.value) await fetchAnswerList(currentQuestion.value.id)
}
const handleDelete = async (row: QaQuestionListItemVO) => {
  await ElMessageBox.confirm(`确认删除问题《${row.title}》吗？`, '删除提示', { type: 'warning' })
  await deleteQaQuestionApi(row.id)
  ElMessage.success('删除成功')
  void fetchQuestionList()
}
const handlePageChange = (page: number) => {
  filterForm.pageNum = page
  void fetchQuestionList()
}
const handleSizeChange = (size: number) => {
  filterForm.pageSize = size
  filterForm.pageNum = 1
  void fetchQuestionList()
}
const handleAnswerPageChange = (page: number) => {
  answerQuery.pageNum = page
  if (currentQuestion.value) void fetchAnswerList(currentQuestion.value.id)
}
onMounted(async () => {
  await Promise.all([fetchCategoryList(), fetchQuestionList()])
})
</script>

<template>
  <div class="qa-manage-page">
    <SectionCard class="qa-manage-page__filter">
      <div class="filter-action-row">
        <el-button type="primary" @click="handleCreate">新建问题</el-button>
      </div>
      <div class="filter-grid">
        <el-input
          v-model="filterForm.keyword"
          placeholder="输入标题或内容关键词"
          clearable
          size="large"
        /><el-select v-model="filterForm.categoryId" placeholder="选择分类" clearable size="large"
          ><el-option
            v-for="item in categoryOptions"
            :key="item.id"
            :label="item.categoryName"
            :value="item.id" /></el-select
        ><el-select v-model="filterForm.status" placeholder="选择状态" clearable size="large"
          ><el-option
            v-for="item in statusOptions"
            :key="item.label"
            :label="item.label"
            :value="item.value" /></el-select
        ><el-select v-model="filterForm.sortBy" placeholder="排序方式" size="large"
          ><el-option
            v-for="item in sortOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value" /></el-select
        ><el-button type="primary" size="large" @click="handleSearch">搜索</el-button
        ><el-button size="large" @click="handleReset">重置</el-button>
      </div></SectionCard
    >
    <SectionCard class="qa-manage-page__table"
      ><el-table :data="tableData" v-loading="loading" width="100%"
        ><el-table-column
          prop="title"
          label="问题标题"
          min-width="240"
          show-overflow-tooltip
        /><el-table-column label="分类" width="120"
          ><template #default="scope">{{
            getCategoryName(scope.row.categoryId)
          }}</template></el-table-column
        ><el-table-column prop="userId" label="用户ID" width="100" /><el-table-column
          prop="bountyPoints"
          label="悬赏"
          width="90"
        /><el-table-column prop="viewCount" label="浏览" width="90" /><el-table-column
          prop="answerCount"
          label="回答"
          width="90"
        /><el-table-column prop="followCount" label="关注" width="90" /><el-table-column
          label="状态"
          width="110"
          ><template #default="scope"
            ><el-tag :type="getStatusType(scope.row.status)" effect="light">{{
              getStatusText(scope.row.status)
            }}</el-tag></template
          ></el-table-column
        ><el-table-column label="发布时间" min-width="180"
          ><template #default="scope">{{
            formatDateTime(scope.row.createdAt)
          }}</template></el-table-column
        ><el-table-column label="操作" width="280" fixed="right"
          ><template #default="scope"
            ><el-button type="primary" link @click="handleView(scope.row)">查看</el-button
            ><el-button type="success" link @click="handleEdit(scope.row)">编辑</el-button
            ><el-button type="warning" link @click="handleAnswerManage(scope.row)"
              >回答管理</el-button
            ><el-button type="danger" link @click="handleDelete(scope.row)"
              >删除</el-button
            ></template
          ></el-table-column
        ></el-table
      >
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
        /></div
    ></SectionCard>
    <el-dialog v-model="formVisible" :title="isEditMode ? '编辑问题' : '新建问题'" width="760px"
      ><QAEditorForm
        ref="qaFormRef"
        :form="questionForm"
        :categories="categoryOptions"
        :status-options="editStatusOptions"
        :is-edit="isEditMode"
      /><template #footer
        ><div class="dialog-footer">
          <el-button @click="formVisible = false">取消</el-button
          ><el-button type="primary" :loading="submitLoading" @click="handleSubmit">{{
            isEditMode ? '保存修改' : '确认创建'
          }}</el-button>
        </div></template
      ></el-dialog
    >
    <el-dialog v-model="detailVisible" title="问题详情" width="860px"
      ><div v-loading="detailLoading" class="qa-detail">
        <template v-if="currentQuestion"
          ><div class="qa-detail__item">
            <span>标题</span><strong>{{ currentQuestion.title }}</strong>
          </div>
          <div class="qa-detail__item">
            <span>内容</span>
            <p>{{ currentQuestion.content || '-' }}</p>
          </div>
          <DetailMetricsGrid
            :items="[
              { label: '分类', value: getCategoryName(currentQuestion.categoryId) },
              { label: '悬赏', value: currentQuestion.bountyPoints || 0 },
              { label: '浏览', value: currentQuestion.viewCount },
              { label: '回答', value: currentQuestion.answerCount },
            ]"
          /><DetailMetricsGrid
            :items="[
              { label: '关注', value: currentQuestion.followCount },
              { label: '状态', value: getStatusText(currentQuestion.status) },
              { label: '最佳回答ID', value: currentQuestion.bestAnswerId || '-' },
              { label: '发布时间', value: formatDateTime(currentQuestion.createdAt) },
            ]"
          />
          <div class="qa-detail__item">
            <span>词云图</span>
            <p>{{ currentWordCloud?.imageUrl || '暂无词云图' }}</p>
          </div></template
        >
      </div></el-dialog
    >
    <el-dialog v-model="answerVisible" title="回答管理" width="860px"
      ><div v-loading="answerLoading">
        <el-table :data="currentAnswers" width="100%"
          ><el-table-column
            prop="content"
            label="回答内容"
            min-width="260"
            show-overflow-tooltip
          /><el-table-column prop="userId" label="用户ID" width="100" /><el-table-column
            prop="likeCount"
            label="赞"
            width="80"
          /><el-table-column prop="dislikeCount" label="踩" width="80" /><el-table-column
            label="采纳"
            width="90"
            ><template #default="scope">{{
              scope.row.isAccepted ? '已采纳' : '未采纳'
            }}</template></el-table-column
          ><el-table-column label="状态" width="100"
            ><template #default="scope"
              ><el-tag :type="getAnswerStatusType(scope.row.status)" effect="light">{{
                getAnswerStatusText(scope.row.status)
              }}</el-tag></template
            ></el-table-column
          ><el-table-column label="创建时间" min-width="160"
            ><template #default="scope">{{
              formatDateTime(scope.row.createdAt)
            }}</template></el-table-column
          ><el-table-column label="操作" width="160" fixed="right"
            ><template #default="scope"
              ><el-button
                type="primary"
                link
                :disabled="!!scope.row.isAccepted"
                @click="handleAcceptAnswer(scope.row)"
                >采纳</el-button
              ><el-button type="danger" link @click="handleDeleteAnswer(scope.row)"
                >删除</el-button
              ></template
            ></el-table-column
          ></el-table
        >
        <div class="table-pagination">
          <el-pagination
            background
            layout="total, prev, pager, next"
            :current-page="answerQuery.pageNum"
            :page-size="answerQuery.pageSize"
            :total="answerTotal"
            @current-change="handleAnswerPageChange"
          />
        </div></div
    ></el-dialog>
  </div>
</template>
