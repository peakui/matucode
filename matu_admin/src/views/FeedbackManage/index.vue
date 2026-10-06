<script setup lang="ts">
import type { FormInstance, FormRules, TagProps } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import SectionCard from '@/components/SectionCard/index.vue'
import DetailMetricsGrid from '@/components/DetailMetricsGrid/index.vue'
import {
  getAdminFeedbackDetailApi,
  getAdminFeedbackListApi,
  updateAdminFeedbackApi,
} from '@/api'
import type {
  FeedbackPriority,
  FeedbackStatus,
  FeedbackType,
  FeedbackVO,
  UpdateFeedbackRequest,
} from '@/api/types'
import './index.scss'

const loading = ref(false)
const detailLoading = ref(false)
const submitLoading = ref(false)
const detailVisible = ref(false)
const processVisible = ref(false)
const total = ref(0)
const tableData = ref<FeedbackVO[]>([])
const currentFeedback = ref<FeedbackVO | null>(null)
const processFormRef = ref<FormInstance>()

const filterForm = reactive({
  keyword: '',
  type: undefined as FeedbackType | undefined,
  status: undefined as FeedbackStatus | undefined,
  priority: undefined as FeedbackPriority | undefined,
  assigneeId: '',
  userId: '',
  pageNum: 1,
  pageSize: 10,
})

const processForm = reactive<UpdateFeedbackRequest>({
  status: 1,
  priority: 1,
  assigneeId: '',
  replyContent: '',
})

const feedbackTypeOptions: Array<{ label: string; value: FeedbackType; tagType: TagProps['type'] }> = [
  { label: 'Bug 报告', value: 0, tagType: 'danger' },
  { label: '功能建议', value: 1, tagType: 'success' },
  { label: '内容举报', value: 2, tagType: 'warning' },
  { label: '账号问题', value: 3, tagType: 'primary' },
  { label: '其他', value: 4, tagType: 'info' },
]

const feedbackStatusOptions: Array<{ label: string; value: FeedbackStatus; tagType: TagProps['type'] }> = [
  { label: '待处理', value: 0, tagType: 'warning' },
  { label: '处理中', value: 1, tagType: 'primary' },
  { label: '已解决', value: 2, tagType: 'success' },
  { label: '已拒绝', value: 3, tagType: 'danger' },
  { label: '已关闭', value: 4, tagType: 'info' },
]

const priorityOptions: Array<{ label: string; value: FeedbackPriority; tagType: TagProps['type'] }> = [
  { label: '低', value: 0, tagType: 'info' },
  { label: '中', value: 1, tagType: 'primary' },
  { label: '高', value: 2, tagType: 'warning' },
  { label: '紧急', value: 3, tagType: 'danger' },
]

const processFormRules: FormRules<UpdateFeedbackRequest> = {
  status: [{ required: true, message: '请选择处理状态', trigger: 'change' }],
  priority: [{ required: true, message: '请选择优先级', trigger: 'change' }],
}

const formatDateTime = (value?: string | null) => (!value ? '-' : value.replace('T', ' '))

const getTypeMeta = (type: FeedbackType): { label: string; value: FeedbackType; tagType: TagProps['type'] } =>
  feedbackTypeOptions.find((item) => item.value === type) ?? feedbackTypeOptions[4]!

const getStatusMeta = (status: FeedbackStatus): { label: string; value: FeedbackStatus; tagType: TagProps['type'] } =>
  feedbackStatusOptions.find((item) => item.value === status) ?? feedbackStatusOptions[0]!

const getPriorityMeta = (priority: FeedbackPriority): { label: string; value: FeedbackPriority; tagType: TagProps['type'] } =>
  priorityOptions.find((item) => item.value === priority) ?? priorityOptions[1]!

const stringifyExtraInfo = (extraInfo?: Record<string, unknown>) => {
  if (!extraInfo || Object.keys(extraInfo).length === 0) return '-'
  return JSON.stringify(extraInfo, null, 2)
}

const fetchFeedbackList = async () => {
  loading.value = true

  try {
    const res = await getAdminFeedbackListApi({
      keyword: filterForm.keyword || undefined,
      type: filterForm.type,
      status: filterForm.status,
      priority: filterForm.priority,
      assigneeId: filterForm.assigneeId || undefined,
      userId: filterForm.userId || undefined,
      pageNum: filterForm.pageNum,
      pageSize: filterForm.pageSize,
    })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const fetchFeedbackDetail = async (id: string) => {
  detailLoading.value = true

  try {
    const res = await getAdminFeedbackDetailApi(id)
    currentFeedback.value = res.data
    return res.data
  } finally {
    detailLoading.value = false
  }
}

const resetProcessForm = () => {
  processForm.status = 1
  processForm.priority = 1
  processForm.assigneeId = ''
  processForm.replyContent = ''
  processFormRef.value?.clearValidate()
}

const fillProcessForm = (detail: FeedbackVO) => {
  processForm.status = detail.status
  processForm.priority = detail.priority
  processForm.assigneeId = detail.assigneeId || ''
  processForm.replyContent = detail.replyContent || ''
}

const handleSearch = () => {
  filterForm.pageNum = 1
  void fetchFeedbackList()
}

const handleReset = () => {
  filterForm.keyword = ''
  filterForm.type = undefined
  filterForm.status = undefined
  filterForm.priority = undefined
  filterForm.assigneeId = ''
  filterForm.userId = ''
  filterForm.pageNum = 1
  void fetchFeedbackList()
}

const handleView = async (row: FeedbackVO) => {
  detailVisible.value = true
  await fetchFeedbackDetail(row.id)
}

const handleProcess = async (row: FeedbackVO) => {
  processVisible.value = true
  resetProcessForm()
  const detail = await fetchFeedbackDetail(row.id)
  fillProcessForm(detail)
}

const handleSubmitProcess = async () => {
  if (!currentFeedback.value) return

  await processFormRef.value?.validate()
  submitLoading.value = true

  try {
    await updateAdminFeedbackApi(currentFeedback.value.id, {
      status: processForm.status,
      priority: processForm.priority,
      assigneeId: processForm.assigneeId || undefined,
      replyContent: processForm.replyContent || undefined,
    })
    ElMessage.success('反馈处理成功')
    processVisible.value = false
    resetProcessForm()
    void fetchFeedbackList()
  } finally {
    submitLoading.value = false
  }
}

const handlePageChange = (page: number) => {
  filterForm.pageNum = page
  void fetchFeedbackList()
}

const handleSizeChange = (size: number) => {
  filterForm.pageSize = size
  filterForm.pageNum = 1
  void fetchFeedbackList()
}

onMounted(async () => {
  await fetchFeedbackList()
})
</script>

<template>
  <div class="feedback-manage-page">
    <SectionCard class="feedback-manage-page__filter">
      <div class="filter-toolbar">
        <el-input v-model="filterForm.keyword" placeholder="请输入反馈标题或内容关键词" clearable size="large" />
        <el-button type="primary" size="large" @click="handleSearch">搜索</el-button>
        <el-button size="large" @click="handleReset">重置</el-button>
      </div>
      <div class="feedback-filter-grid">
        <el-select v-model="filterForm.type" placeholder="反馈类型" clearable size="large">
          <el-option v-for="item in feedbackTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="filterForm.status" placeholder="处理状态" clearable size="large">
          <el-option v-for="item in feedbackStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="filterForm.priority" placeholder="优先级" clearable size="large">
          <el-option v-for="item in priorityOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-input v-model="filterForm.assigneeId" placeholder="处理人 ID" clearable size="large" />
        <el-input v-model="filterForm.userId" placeholder="提交用户 ID" clearable size="large" />
      </div>
    </SectionCard>

    <SectionCard class="feedback-manage-page__table">
      <el-table :data="tableData" v-loading="loading" width="100%">
        <el-table-column prop="title" label="反馈标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="username" label="用户" width="130">
          <template #default="scope">{{ scope.row.username || scope.row.userId || '匿名用户' }}</template>
        </el-table-column>
        <el-table-column prop="contactEmail" label="联系邮箱" min-width="180" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.contactEmail || '-' }}</template>
        </el-table-column>
        <el-table-column label="类型" width="120">
          <template #default="scope">
            <el-tag :type="getTypeMeta(scope.row.type).tagType" effect="light">{{ getTypeMeta(scope.row.type).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="scope">
            <el-tag :type="getStatusMeta(scope.row.status).tagType" effect="light">{{ getStatusMeta(scope.row.status).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="优先级" width="100">
          <template #default="scope">
            <el-tag :type="getPriorityMeta(scope.row.priority).tagType" effect="light">{{ getPriorityMeta(scope.row.priority).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assigneeId" label="处理人" width="110">
          <template #default="scope">{{ scope.row.assigneeId || '-' }}</template>
        </el-table-column>
        <el-table-column label="回复时间" min-width="170">
          <template #default="scope">{{ formatDateTime(scope.row.repliedAt) }}</template>
        </el-table-column>
        <el-table-column label="解决时间" min-width="170">
          <template #default="scope">{{ formatDateTime(scope.row.resolvedAt) }}</template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="170">
          <template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="scope">
            <el-button type="primary" link @click="handleView(scope.row)">查看</el-button>
            <el-button type="success" link @click="handleProcess(scope.row)">处理</el-button>
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

    <el-dialog v-model="detailVisible" title="反馈详情" width="780px">
      <div v-loading="detailLoading" class="feedback-detail">
        <template v-if="currentFeedback">
          <div class="feedback-detail__item">
            <span>反馈标题</span><strong>{{ currentFeedback.title }}</strong>
          </div>
          <div class="feedback-detail__item">
            <span>反馈内容</span>
            <p>{{ currentFeedback.content || '-' }}</p>
          </div>
          <DetailMetricsGrid
            :items="[
              { label: '反馈类型', value: getTypeMeta(currentFeedback.type).label },
              { label: '处理状态', value: getStatusMeta(currentFeedback.status).label },
              { label: '优先级', value: getPriorityMeta(currentFeedback.priority).label },
              { label: '处理人 ID', value: currentFeedback.assigneeId || '-' },
            ]"
          />
          <DetailMetricsGrid
            :items="[
              { label: '用户', value: currentFeedback.username || currentFeedback.userId || '匿名用户' },
              { label: '联系邮箱', value: currentFeedback.contactEmail || '-' },
              { label: 'IP 地址', value: currentFeedback.ipAddress || '-' },
            ]"
          />
          <div class="feedback-detail__item">
            <span>官方回复</span>
            <p>{{ currentFeedback.replyContent || '-' }}</p>
          </div>
          <div class="feedback-detail__item">
            <span>处理历史</span>
            <el-timeline v-if="currentFeedback.history?.length">
              <el-timeline-item v-for="item in currentFeedback.history" :key="item.id" :timestamp="formatDateTime(item.createdAt)">
                {{ getStatusMeta(item.fromStatus).label }} → {{ getStatusMeta(item.toStatus).label }} · 操作人 {{ item.operatorId }}
                <p v-if="item.replyContent">{{ item.replyContent }}</p>
              </el-timeline-item>
            </el-timeline>
            <p v-else>暂无处理记录</p>
          </div>
          <div class="feedback-detail__item">
            <span>附件</span>
            <div v-if="currentFeedback.attachments?.length" class="attachment-list">
              <a v-for="item in currentFeedback.attachments" :key="item.url" :href="item.url" target="_blank" rel="noreferrer">
                {{ item.name }}（{{ item.size }} bytes）
              </a>
            </div>
            <p v-else>-</p>
          </div>
          <div class="feedback-detail__item">
            <span>环境信息</span>
            <pre>{{ stringifyExtraInfo(currentFeedback.extraInfo) }}</pre>
          </div>
          <DetailMetricsGrid
            :items="[
              { label: '回复时间', value: formatDateTime(currentFeedback.repliedAt) },
              { label: '解决时间', value: formatDateTime(currentFeedback.resolvedAt) },
              { label: '创建时间', value: formatDateTime(currentFeedback.createdAt) },
              { label: '更新时间', value: formatDateTime(currentFeedback.updatedAt) },
            ]"
          />
        </template>
      </div>
    </el-dialog>

    <el-dialog v-model="processVisible" title="处理反馈" width="680px">
      <el-form ref="processFormRef" :model="processForm" :rules="processFormRules" label-position="top" class="feedback-process-form">
        <div v-if="currentFeedback" class="process-summary">
          <strong>{{ currentFeedback.title }}</strong>
          <p>{{ currentFeedback.content || '-' }}</p>
        </div>
        <div class="feedback-process-form__grid">
          <el-form-item label="处理状态" prop="status">
            <el-select v-model="processForm.status" placeholder="请选择处理状态" size="large">
              <el-option v-for="item in feedbackStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="优先级" prop="priority">
            <el-select v-model="processForm.priority" placeholder="请选择优先级" size="large">
              <el-option v-for="item in priorityOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="处理人 ID" prop="assigneeId">
          <el-input v-model="processForm.assigneeId" placeholder="请输入处理人 ID" size="large" clearable />
        </el-form-item>
        <el-form-item label="官方回复" prop="replyContent">
          <el-input v-model="processForm.replyContent" type="textarea" :rows="5" placeholder="请输入官方回复" maxlength="1000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="processVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmitProcess">保存处理结果</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
