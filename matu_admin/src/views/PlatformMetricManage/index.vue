<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SectionCard from '@/components/SectionCard/index.vue'
import {
  createAdminPlatformMetricApi,
  deleteAdminPlatformMetricApi,
  getAdminPlatformMetricLatestApi,
  getAdminPlatformMetricListApi,
  updateAdminPlatformMetricApi,
} from '@/api'
import type { CreatePlatformMetricRequest, PlatformMetricVO } from '@/api/types'
import './index.scss'

const loading = ref(false)
const latestLoading = ref(false)
const submitLoading = ref(false)
const formVisible = ref(false)
const isEditMode = ref(false)
const total = ref(0)
const tableData = ref<PlatformMetricVO[]>([])
const latestMetrics = ref<PlatformMetricVO[]>([])
const currentMetric = ref<PlatformMetricVO | null>(null)
const formRef = ref<FormInstance>()

const filterForm = reactive({
  metricKey: '',
  dateRange: [] as string[],
  pageNum: 1,
  pageSize: 10,
})

const metricForm = reactive<CreatePlatformMetricRequest>({
  metricDate: '',
  metricKey: '',
  metricValue: 0,
  extra: '',
})

const metricFormRules: FormRules<CreatePlatformMetricRequest> = {
  metricDate: [{ required: true, message: '请选择统计日期', trigger: 'change' }],
  metricKey: [{ required: true, message: '请输入指标名', trigger: 'blur' }],
  metricValue: [{ required: true, message: '请输入指标值', trigger: 'blur' }],
}

const formatDateTime = (value?: string) => (!value ? '-' : value.replace('T', ' '))

const fetchMetricList = async () => {
  loading.value = true

  try {
    const [startDate, endDate] = filterForm.dateRange || []
    const res = await getAdminPlatformMetricListApi({
      metricKey: filterForm.metricKey || undefined,
      startDate: startDate || undefined,
      endDate: endDate || undefined,
      pageNum: filterForm.pageNum,
      pageSize: filterForm.pageSize,
    })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const fetchLatestMetrics = async () => {
  latestLoading.value = true

  try {
    const res = await getAdminPlatformMetricLatestApi()
    latestMetrics.value = res.data || []
  } catch {
    latestMetrics.value = []
  } finally {
    latestLoading.value = false
  }
}

const resetMetricForm = () => {
  metricForm.metricDate = ''
  metricForm.metricKey = ''
  metricForm.metricValue = 0
  metricForm.extra = ''
  formRef.value?.clearValidate()
}

const handleSearch = () => {
  filterForm.pageNum = 1
  void fetchMetricList()
}

const handleReset = () => {
  filterForm.metricKey = ''
  filterForm.dateRange = []
  filterForm.pageNum = 1
  void fetchMetricList()
}

const handleCreate = () => {
  isEditMode.value = false
  currentMetric.value = null
  resetMetricForm()
  formVisible.value = true
}

const handleEdit = (row: PlatformMetricVO) => {
  isEditMode.value = true
  currentMetric.value = row
  resetMetricForm()
  metricForm.metricDate = row.metricDate
  metricForm.metricKey = row.metricKey
  metricForm.metricValue = row.metricValue
  metricForm.extra = row.extra || ''
  formVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  submitLoading.value = true

  try {
    if (isEditMode.value && currentMetric.value) {
      await updateAdminPlatformMetricApi(currentMetric.value.id, {
        metricValue: metricForm.metricValue,
        extra: metricForm.extra,
      })
      ElMessage.success('指标更新成功')
    } else {
      await createAdminPlatformMetricApi({ ...metricForm })
      ElMessage.success('指标创建成功')
    }

    formVisible.value = false
    resetMetricForm()
    await Promise.all([fetchMetricList(), fetchLatestMetrics()])
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = async (row: PlatformMetricVO) => {
  await ElMessageBox.confirm(
    `确认删除指标《${row.metricKey}》（${row.metricDate}）吗？`,
    '删除指标',
    { type: 'warning' },
  )
  await deleteAdminPlatformMetricApi(row.id)
  ElMessage.success('删除成功')
  await Promise.all([fetchMetricList(), fetchLatestMetrics()])
}

const handlePageChange = (page: number) => {
  filterForm.pageNum = page
  void fetchMetricList()
}

const handleSizeChange = (size: number) => {
  filterForm.pageSize = size
  filterForm.pageNum = 1
  void fetchMetricList()
}

onMounted(async () => {
  await Promise.all([fetchMetricList(), fetchLatestMetrics()])
})
</script>

<template>
  <div class="platform-metric-manage-page">
    <SectionCard title="最新指标" description="各指标最近一次的记录值。">
      <div v-loading="latestLoading" class="metric-latest">
        <div v-for="item in latestMetrics" :key="`${item.metricKey}-${item.id}`" class="metric-latest__card">
          <span>{{ item.metricKey }}</span>
          <strong>{{ item.metricValue }}</strong>
          <em>{{ item.metricDate }}</em>
        </div>
        <p v-if="!latestMetrics.length" class="metric-latest__empty">暂无指标数据</p>
      </div>
    </SectionCard>

    <SectionCard class="platform-metric-manage-page__filter">
      <div class="filter-action-row">
        <el-button type="primary" @click="handleCreate">新建指标</el-button>
      </div>
      <div class="filter-toolbar">
        <el-input v-model="filterForm.metricKey" placeholder="请输入指标名" clearable size="large" />
        <el-button type="primary" size="large" @click="handleSearch">搜索</el-button>
        <el-button size="large" @click="handleReset">重置</el-button>
      </div>
      <div class="filter-grid">
        <el-date-picker
          v-model="filterForm.dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          size="large"
        />
      </div>
    </SectionCard>

    <SectionCard class="platform-metric-manage-page__table">
      <el-table :data="tableData" v-loading="loading" width="100%">
        <el-table-column prop="metricKey" label="指标名" min-width="200" show-overflow-tooltip />
        <el-table-column prop="metricDate" label="统计日期" width="140" />
        <el-table-column prop="metricValue" label="指标值" width="140" />
        <el-table-column prop="extra" label="附加信息" min-width="200" show-overflow-tooltip />
        <el-table-column label="创建时间" min-width="170">
          <template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button type="success" link @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(scope.row)">删除</el-button>
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

    <el-dialog v-model="formVisible" :title="isEditMode ? '编辑指标' : '新建指标'" width="640px">
      <el-form ref="formRef" :model="metricForm" :rules="metricFormRules" label-position="top" class="metric-form">
        <div class="metric-form__grid">
          <el-form-item label="统计日期" prop="metricDate">
            <el-date-picker
              v-model="metricForm.metricDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="请选择统计日期"
              size="large"
              :disabled="isEditMode"
            />
          </el-form-item>
          <el-form-item label="指标名" prop="metricKey">
            <el-input
              v-model="metricForm.metricKey"
              placeholder="例如 daily_active_users"
              size="large"
              maxlength="64"
              :disabled="isEditMode"
            />
          </el-form-item>
        </div>
        <el-form-item label="指标值" prop="metricValue">
          <el-input-number v-model="metricForm.metricValue" :min="0" controls-position="right" size="large" />
        </el-form-item>
        <el-form-item label="附加信息" prop="extra">
          <el-input v-model="metricForm.extra" type="textarea" :rows="3" placeholder="可选的附加信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="formVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
            {{ isEditMode ? '保存修改' : '确认创建' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
