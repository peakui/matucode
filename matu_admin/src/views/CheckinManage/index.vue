<script setup lang="ts">
import type { TagProps } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SectionCard from '@/components/SectionCard/index.vue'
import DetailMetricsGrid from '@/components/DetailMetricsGrid/index.vue'
import CheckinEditorForm from '@/components/CheckinEditorForm/index.vue'
import {
  createCheckRecordApi,
  deleteCheckRecordApi,
  getCheckRecordDetailApi,
  getCheckRecordListApi,
  getCheckStatisticsApi,
  updateCheckRecordApi,
} from '@/api'
import type {
  CheckRecordListItemVO,
  CheckRecordVO,
  CheckStatisticsVO,
  CreateCheckRecordRequest,
} from '@/api/types'
import './index.scss'

const loading = ref(false)
const detailLoading = ref(false)
const recordSubmitLoading = ref(false)
const checkinFormRef = ref<InstanceType<typeof CheckinEditorForm> | null>(null)
const detailVisible = ref(false)
const recordVisible = ref(false)
const isEditMode = ref(false)
const total = ref(0)
const tableData = ref<CheckRecordListItemVO[]>([])
const currentRecord = ref<CheckRecordVO | null>(null)
const statistics = ref<CheckStatisticsVO | null>(null)
const filterForm = reactive({
  userId: undefined as string | undefined,
  status: undefined as number | undefined,
  year: undefined as number | undefined,
  month: undefined as number | undefined,
  pageNum: 1,
  pageSize: 10,
})
const recordForm = reactive<CreateCheckRecordRequest>({
  title: '',
  summary: '',
  content: '',
  imageUrls: [],
  learnHours: '0.00',
  mood: undefined,
  location: '',
  checkDate: '',
  status: 1,
})
const statusOptions = [
  { label: '全部状态', value: undefined },
  { label: '已发布', value: 1 },
  { label: '草稿', value: 2 },
  { label: '已删除', value: 3 },
  { label: '仅自己可见', value: 4 },
]
const formatDateTime = (value?: string) => (!value ? '-' : value.replace('T', ' '))
const getStatusText = (status: number) =>
  ({ 1: '已发布', 2: '草稿', 3: '已删除', 4: '仅自己可见' })[status] || '未知'
const getStatusType = (status: number): TagProps['type'] =>
  (({ 1: 'success', 2: 'warning', 3: 'danger', 4: 'info' })[status] as TagProps['type']) || 'info'
const getMoodText = (mood?: number) => (!mood ? '-' : `心情 ${mood}`)
const resetRecordForm = () => {
  recordForm.title = ''
  recordForm.summary = ''
  recordForm.content = ''
  recordForm.imageUrls = []
  recordForm.learnHours = '0.00'
  recordForm.mood = undefined
  recordForm.location = ''
  recordForm.checkDate = ''
  recordForm.status = 1
}
const fetchRecordList = async () => {
  loading.value = true
  try {
    const res = await getCheckRecordListApi({ ...filterForm })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}
const fetchStatistics = async () => {
  const res = await getCheckStatisticsApi({
    userId: filterForm.userId,
    year: filterForm.year,
    month: filterForm.month,
  })
  statistics.value = res.data
}
const fetchRecordDetail = async (checkId: string) => {
  detailLoading.value = true
  try {
    const res = await getCheckRecordDetailApi(checkId)
    currentRecord.value = res.data
    return res.data
  } finally {
    detailLoading.value = false
  }
}
const handleSearch = () => {
  filterForm.pageNum = 1
  void Promise.all([fetchRecordList(), fetchStatistics()])
}
const handleReset = () => {
  filterForm.userId = undefined
  filterForm.status = undefined
  filterForm.year = undefined
  filterForm.month = undefined
  filterForm.pageNum = 1
  void Promise.all([fetchRecordList(), fetchStatistics()])
}
const handleCreate = () => {
  isEditMode.value = false
  resetRecordForm()
  recordVisible.value = true
}
const handleEdit = async (row: CheckRecordListItemVO) => {
  isEditMode.value = true
  recordVisible.value = true
  const detail = await fetchRecordDetail(row.id)
  if (!detail) return
  recordForm.title = detail.title
  recordForm.summary = detail.summary || ''
  recordForm.content = detail.content || ''
  recordForm.imageUrls = detail.imageUrls || []
  recordForm.learnHours = detail.learnHours || '0.00'
  recordForm.mood = detail.mood
  recordForm.location = detail.location || ''
  recordForm.checkDate = detail.checkDate
  recordForm.status = detail.status
}
const handleSubmitRecord = async () => {
  await checkinFormRef.value?.validate()
  recordSubmitLoading.value = true
  try {
    if (isEditMode.value && currentRecord.value) {
      await updateCheckRecordApi(currentRecord.value.id, { ...recordForm })
      ElMessage.success('打卡更新成功')
    } else {
      await createCheckRecordApi({ ...recordForm })
      ElMessage.success('打卡创建成功')
    }
    recordVisible.value = false
    resetRecordForm()
    await Promise.all([fetchRecordList(), fetchStatistics()])
  } finally {
    recordSubmitLoading.value = false
  }
}
const handleView = async (row: CheckRecordListItemVO) => {
  detailVisible.value = true
  await fetchRecordDetail(row.id)
}
const handleDelete = async (row: CheckRecordListItemVO) => {
  await ElMessageBox.confirm(`确认删除打卡《${row.title}》吗？`, '删除提示', { type: 'warning' })
  await deleteCheckRecordApi(row.id)
  ElMessage.success('删除成功')
  void Promise.all([fetchRecordList(), fetchStatistics()])
}
const handlePageChange = (page: number) => {
  filterForm.pageNum = page
  void fetchRecordList()
}
const handleSizeChange = (size: number) => {
  filterForm.pageSize = size
  filterForm.pageNum = 1
  void fetchRecordList()
}
onMounted(async () => {
  await Promise.all([fetchRecordList(), fetchStatistics()])
})
</script>

<template>
  <div class="checkin-manage-page">
    <SectionCard class="checkin-manage-page__filter">
      <div class="filter-action-row">
        <el-button type="primary" @click="handleCreate">新建打卡</el-button>
      </div>
      <div class="filter-grid">
        <el-input v-model="filterForm.userId" placeholder="按用户ID筛选" clearable size="large" />
        <el-select v-model="filterForm.status" placeholder="选择状态" clearable size="large">
          <el-option
            v-for="item in statusOptions"
            :key="item.label"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-input
          v-model.number="filterForm.year"
          placeholder="年份，如 2026"
          clearable
          size="large"
        />
        <el-input
          v-model.number="filterForm.month"
          placeholder="月份，如 4"
          clearable
          size="large"
        />
        <el-button type="primary" size="large" @click="handleSearch">搜索</el-button>
        <el-button size="large" @click="handleReset">重置</el-button>
      </div>
    </SectionCard>

    <SectionCard>
      <template v-if="statistics">
        <DetailMetricsGrid
          :items="[
            { label: '累计打卡', value: statistics.totalDays },
            { label: '当前连签', value: statistics.continuousDays },
            { label: '历史最长', value: statistics.maxContinuousDays },
            { label: '总学习时长', value: statistics.totalLearnHours },
          ]"
        />
      </template>
    </SectionCard>

    <SectionCard class="checkin-manage-page__table">
      <el-table :data="tableData" v-loading="loading" width="100%">
        <el-table-column prop="title" label="打卡标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="summary" label="摘要" min-width="220" show-overflow-tooltip />
        <el-table-column prop="learnHours" label="学习时长" width="110" />
        <el-table-column label="心情" width="100"
          ><template #default="scope">{{ getMoodText(scope.row.mood) }}</template></el-table-column
        >
        <el-table-column prop="location" label="地点" width="120" show-overflow-tooltip />
        <el-table-column prop="commentCount" label="评论" width="90" />
        <el-table-column label="状态" width="110"
          ><template #default="scope"
            ><el-tag :type="getStatusType(scope.row.status)" effect="light">{{
              getStatusText(scope.row.status)
            }}</el-tag></template
          ></el-table-column
        >
        <el-table-column label="打卡日期" min-width="140"
          ><template #default="scope">{{ scope.row.checkDate || '-' }}</template></el-table-column
        >
        <el-table-column label="操作" width="220" fixed="right"
          ><template #default="scope"
            ><el-button type="primary" link @click="handleView(scope.row)">查看</el-button
            ><el-button type="success" link @click="handleEdit(scope.row)">编辑</el-button
            ><el-button type="danger" link @click="handleDelete(scope.row)"
              >删除</el-button
            ></template
          ></el-table-column
        >
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

    <el-dialog v-model="recordVisible" :title="isEditMode ? '编辑打卡' : '新建打卡'" width="760px">
      <CheckinEditorForm ref="checkinFormRef" :form="recordForm" />
      <template #footer
        ><div class="dialog-footer">
          <el-button @click="recordVisible = false">取消</el-button
          ><el-button type="primary" :loading="recordSubmitLoading" @click="handleSubmitRecord">{{
            isEditMode ? '保存修改' : '确认创建'
          }}</el-button>
        </div></template
      >
    </el-dialog>

    <el-dialog v-model="detailVisible" title="打卡详情" width="760px">
      <div v-loading="detailLoading" class="check-detail">
        <template v-if="currentRecord">
          <div class="check-detail__item">
            <span>标题</span><strong>{{ currentRecord.title }}</strong>
          </div>
          <div class="check-detail__item">
            <span>摘要</span>
            <p>{{ currentRecord.summary || '-' }}</p>
          </div>
          <div class="check-detail__item">
            <span>正文内容</span>
            <p>{{ currentRecord.content || '-' }}</p>
          </div>
          <DetailMetricsGrid
            :items="[
              { label: '学习时长', value: currentRecord.learnHours },
              { label: '心情', value: getMoodText(currentRecord.mood) },
              { label: '评论数', value: currentRecord.commentCount },
              { label: '状态', value: getStatusText(currentRecord.status) },
            ]"
          />
        </template>
      </div>
    </el-dialog>
  </div>
</template>
