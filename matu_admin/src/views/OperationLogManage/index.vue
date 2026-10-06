<script setup lang="ts">
import type { TagProps } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import SectionCard from '@/components/SectionCard/index.vue'
import DetailMetricsGrid from '@/components/DetailMetricsGrid/index.vue'
import { getAdminOperationLogDetailApi, getAdminOperationLogListApi } from '@/api'
import type { OperationLogVO } from '@/api/types'
import './index.scss'

const loading = ref(false)
const detailLoading = ref(false)
const detailVisible = ref(false)
const total = ref(0)
const tableData = ref<OperationLogVO[]>([])
const currentLog = ref<OperationLogVO | null>(null)

const filterForm = reactive({
  username: '',
  module: '',
  action: '',
  result: undefined as number | undefined,
  timeRange: [] as string[],
  pageNum: 1,
  pageSize: 10,
})

const resultOptions: Array<{ label: string; value: number; tagType: TagProps['type'] }> = [
  { label: '成功', value: 1, tagType: 'success' },
  { label: '失败', value: 0, tagType: 'danger' },
]

const formatDateTime = (value?: string) => (!value ? '-' : value.replace('T', ' '))

const getResultMeta = (result?: number) =>
  resultOptions.find((item) => item.value === result) ?? { label: '未知', value: -1, tagType: 'info' as TagProps['type'] }

const fetchLogList = async () => {
  loading.value = true

  try {
    const [startTime, endTime] = filterForm.timeRange || []
    const res = await getAdminOperationLogListApi({
      username: filterForm.username || undefined,
      module: filterForm.module || undefined,
      action: filterForm.action || undefined,
      result: filterForm.result,
      startTime: startTime || undefined,
      endTime: endTime || undefined,
      pageNum: filterForm.pageNum,
      pageSize: filterForm.pageSize,
    })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  filterForm.pageNum = 1
  void fetchLogList()
}

const handleReset = () => {
  filterForm.username = ''
  filterForm.module = ''
  filterForm.action = ''
  filterForm.result = undefined
  filterForm.timeRange = []
  filterForm.pageNum = 1
  void fetchLogList()
}

const handleView = async (row: OperationLogVO) => {
  detailVisible.value = true
  detailLoading.value = true

  try {
    const res = await getAdminOperationLogDetailApi(row.id)
    currentLog.value = res.data
  } finally {
    detailLoading.value = false
  }
}

const handlePageChange = (page: number) => {
  filterForm.pageNum = page
  void fetchLogList()
}

const handleSizeChange = (size: number) => {
  filterForm.pageSize = size
  filterForm.pageNum = 1
  void fetchLogList()
}

onMounted(async () => {
  await fetchLogList()
})
</script>

<template>
  <div class="operation-log-manage-page">
    <SectionCard class="operation-log-manage-page__filter">
      <div class="filter-toolbar">
        <el-input v-model="filterForm.username" placeholder="操作人用户名" clearable size="large" />
        <el-button type="primary" size="large" @click="handleSearch">搜索</el-button>
        <el-button size="large" @click="handleReset">重置</el-button>
      </div>
      <div class="filter-grid">
        <el-input v-model="filterForm.module" placeholder="模块" clearable size="large" />
        <el-input v-model="filterForm.action" placeholder="操作动作" clearable size="large" />
        <el-select v-model="filterForm.result" placeholder="操作结果" clearable size="large">
          <el-option v-for="item in resultOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-date-picker
          v-model="filterForm.timeRange"
          type="datetimerange"
          value-format="YYYY-MM-DDTHH:mm:ss"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          size="large"
        />
      </div>
    </SectionCard>

    <SectionCard class="operation-log-manage-page__table">
      <el-table :data="tableData" v-loading="loading" width="100%">
        <el-table-column prop="username" label="操作人" min-width="140" show-overflow-tooltip />
        <el-table-column prop="module" label="模块" width="140" show-overflow-tooltip />
        <el-table-column prop="action" label="操作" width="160" show-overflow-tooltip />
        <el-table-column prop="targetType" label="目标类型" width="130" />
        <el-table-column prop="targetId" label="目标 ID" width="180" show-overflow-tooltip />
        <el-table-column label="结果" width="100">
          <template #default="scope">
            <el-tag :type="getResultMeta(scope.row.result).tagType" effect="light">
              {{ getResultMeta(scope.row.result).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ipAddress" label="IP" width="150" />
        <el-table-column label="时间" min-width="170">
          <template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="scope">
            <el-button type="primary" link @click="handleView(scope.row)">详情</el-button>
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

    <el-dialog v-model="detailVisible" title="日志详情" width="720px">
      <div v-loading="detailLoading" class="operation-log-detail">
        <template v-if="currentLog">
          <DetailMetricsGrid
            :items="[
              { label: '操作人', value: currentLog.username || '-' },
              { label: '用户 ID', value: currentLog.userId || '-' },
              { label: '模块', value: currentLog.module || '-' },
              { label: '操作', value: currentLog.action || '-' },
            ]"
          />
          <DetailMetricsGrid
            :items="[
              { label: '目标类型', value: currentLog.targetType || '-' },
              { label: '目标 ID', value: currentLog.targetId || '-' },
              { label: '结果', value: getResultMeta(currentLog.result).label },
              { label: '时间', value: formatDateTime(currentLog.createdAt) },
            ]"
          />
          <DetailMetricsGrid
            :items="[
              { label: 'IP 地址', value: currentLog.ipAddress || '-' },
              { label: 'User-Agent', value: currentLog.userAgent || '-' },
            ]"
          />
          <div class="operation-log-detail__item">
            <span>详情</span>
            <p>{{ currentLog.detail || '-' }}</p>
          </div>
          <div v-if="currentLog.errorMsg" class="operation-log-detail__item">
            <span>错误信息</span>
            <p class="is-error">{{ currentLog.errorMsg }}</p>
          </div>
        </template>
      </div>
    </el-dialog>
  </div>
</template>
