<script setup lang="ts">
import type { TagProps } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SectionCard from '@/components/SectionCard/index.vue'
import DetailMetricsGrid from '@/components/DetailMetricsGrid/index.vue'
import { getInternalCertificationListApi, reviewCertificationApi } from '@/api'
import type { CertificationVO, InternalCertificationListParams, ReviewCertificationRequest } from '@/api/types'
import './index.scss'

const loading = ref(false)
const submitLoading = ref(false)
const detailVisible = ref(false)
const total = ref(0)
const tableData = ref<CertificationVO[]>([])
const currentCertification = ref<CertificationVO | null>(null)

const filterForm = reactive<InternalCertificationListParams>({
  certType: undefined,
  certStatus: 0,
  pageNum: 1,
  pageSize: 10,
})

const reviewForm = reactive<ReviewCertificationRequest>({
  certStatus: 1,
  auditRemark: '',
})

const certTypeOptions = [
  { label: '全部类型', value: undefined },
  { label: '学校认证', value: 1 },
  { label: '企业认证', value: 2 },
  { label: '老师认证', value: 3 },
]

const certStatusOptions = [
  { label: '全部状态', value: undefined },
  { label: '待审核', value: 0 },
  { label: '已通过', value: 1 },
  { label: '已拒绝', value: 2 },
]

const formatDateTime = (value?: string) => (!value ? '-' : value.replace('T', ' '))
const getCertTypeText = (type?: number) => ({ 1: '学校认证', 2: '企业认证', 3: '老师认证' })[type || 0] || '未知'
const getCertTypeType = (type?: number): TagProps['type'] =>
  (({ 1: 'success', 2: 'primary', 3: 'warning' })[type || 0] as TagProps['type']) || 'info'
const getCertStatusText = (status?: number) => ({ 0: '待审核', 1: '已通过', 2: '已拒绝' })[status ?? -1] || '未知'
const getCertStatusType = (status?: number): TagProps['type'] =>
  (({ 0: 'warning', 1: 'success', 2: 'danger' })[status ?? -1] as TagProps['type']) || 'info'
const isPending = (row?: CertificationVO | null) => row?.certStatus === 0
const isImageProof = (url?: string) => Boolean(url && /\.(png|jpe?g|gif|webp|bmp|svg)(\?.*)?$/i.test(url))

const fetchCertificationList = async () => {
  loading.value = true
  try {
    const res = await getInternalCertificationListApi({
      certType: filterForm.certType,
      certStatus: filterForm.certStatus,
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
  void fetchCertificationList()
}

const handleReset = () => {
  filterForm.certType = undefined
  filterForm.certStatus = 0
  filterForm.pageNum = 1
  void fetchCertificationList()
}

const openProof = (url?: string) => {
  if (!url) return
  window.open(url, '_blank', 'noopener,noreferrer')
}

const handleView = (row: CertificationVO) => {
  currentCertification.value = row
  reviewForm.certStatus = 1
  reviewForm.auditRemark = row.auditRemark || ''
  detailVisible.value = true
}

const handleReview = (row: CertificationVO, status?: 1 | 2) => {
  currentCertification.value = row
  reviewForm.certStatus = status || 1
  reviewForm.auditRemark = row.auditRemark || ''
  detailVisible.value = true
}

const handleSubmitReview = async () => {
  if (!currentCertification.value) return
  const auditRemark = reviewForm.auditRemark?.trim() || ''
  if (!auditRemark) {
    ElMessage.warning('请填写审核备注')
    return
  }

  await ElMessageBox.confirm(
    reviewForm.certStatus === 1 ? '确认通过该认证申请吗？' : '确认拒绝该认证申请吗？',
    '审核确认',
    { type: 'warning' },
  )

  submitLoading.value = true
  try {
    await reviewCertificationApi(currentCertification.value.id, {
      certStatus: reviewForm.certStatus,
      auditRemark,
    })
    ElMessage.success('审核提交成功')
    detailVisible.value = false
    await fetchCertificationList()
  } finally {
    submitLoading.value = false
  }
}

const handlePageChange = (page: number) => {
  filterForm.pageNum = page
  void fetchCertificationList()
}

const handleSizeChange = (size: number) => {
  filterForm.pageSize = size
  filterForm.pageNum = 1
  void fetchCertificationList()
}

onMounted(async () => {
  await fetchCertificationList()
})
</script>

<template>
  <div class="auth-manage-page">
    <SectionCard class="auth-manage-page__filter">
      <div class="filter-grid">
        <el-select v-model="filterForm.certType" placeholder="认证类型" clearable size="large">
          <el-option
            v-for="item in certTypeOptions"
            :key="item.label"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-select v-model="filterForm.certStatus" placeholder="审核状态" clearable size="large">
          <el-option
            v-for="item in certStatusOptions"
            :key="item.label"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-button type="primary" size="large" @click="handleSearch">搜索</el-button>
        <el-button size="large" @click="handleReset">重置</el-button>
      </div>
    </SectionCard>

    <SectionCard title="认证申请列表" description="默认展示待审核申请，可按认证类型和审核状态筛选。">
      <el-table :data="tableData" v-loading="loading" width="100%">
        <el-table-column prop="id" label="申请ID" width="120" show-overflow-tooltip />
        <el-table-column prop="userId" label="用户ID" width="120" show-overflow-tooltip />
        <el-table-column label="认证类型" width="130">
          <template #default="scope">
            <el-tag :type="getCertTypeType(scope.row.certType)" effect="light">
              {{ scope.row.certTypeName || getCertTypeText(scope.row.certType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="certName" label="认证名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="证明材料" width="120">
          <template #default="scope">
            <el-button v-if="scope.row.certProof" type="primary" link @click="openProof(scope.row.certProof)">
              查看凭证
            </el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="scope">
            <el-tag :type="getCertStatusType(scope.row.certStatus)" effect="light">
              {{ scope.row.certStatusName || getCertStatusText(scope.row.certStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="auditRemark" label="审核备注" min-width="180" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.auditRemark || '-' }}</template>
        </el-table-column>
        <el-table-column label="审核人" width="120" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.auditorId || '-' }}</template>
        </el-table-column>
        <el-table-column label="审核时间" min-width="170">
          <template #default="scope">{{ formatDateTime(scope.row.auditTime) }}</template>
        </el-table-column>
        <el-table-column label="提交时间" min-width="170">
          <template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="scope">
            <el-button type="primary" link @click="handleView(scope.row)">查看</el-button>
            <template v-if="isPending(scope.row)">
              <el-button type="success" link @click="handleReview(scope.row, 1)">通过</el-button>
              <el-button type="danger" link @click="handleReview(scope.row, 2)">拒绝</el-button>
            </template>
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

    <el-dialog v-model="detailVisible" title="认证申请详情" width="860px">
      <div v-if="currentCertification" class="certification-detail">
        <DetailMetricsGrid
          :items="[
            { label: '申请ID', value: currentCertification.id },
            { label: '用户ID', value: currentCertification.userId },
            { label: '认证类型', value: currentCertification.certTypeName || getCertTypeText(currentCertification.certType) },
            { label: '认证名称', value: currentCertification.certName || '-' },
          ]"
        />
        <DetailMetricsGrid
          :items="[
            { label: '审核状态', value: currentCertification.certStatusName || getCertStatusText(currentCertification.certStatus) },
            { label: '审核人', value: currentCertification.auditorId || '-' },
            { label: '审核时间', value: formatDateTime(currentCertification.auditTime) },
            { label: '提交时间', value: formatDateTime(currentCertification.createdAt) },
          ]"
        />

        <div class="certification-detail__block">
          <div class="certification-detail__block-head">
            <strong>证明材料</strong>
            <el-button v-if="currentCertification.certProof" type="primary" link @click="openProof(currentCertification.certProof)">
              新窗口打开
            </el-button>
          </div>
          <el-image
            v-if="isImageProof(currentCertification.certProof)"
            class="certification-detail__proof"
            :src="currentCertification.certProof"
            :preview-src-list="[currentCertification.certProof]"
            fit="cover"
          />
          <p v-else class="certification-detail__proof-link">
            {{ currentCertification.certProof || '暂无证明材料' }}
          </p>
        </div>

        <div class="certification-detail__block">
          <strong>审核备注</strong>
          <p>{{ currentCertification.auditRemark || '-' }}</p>
        </div>

        <el-form v-if="isPending(currentCertification)" :model="reviewForm" label-position="top" class="review-form">
          <el-form-item label="审核结果">
            <el-radio-group v-model="reviewForm.certStatus">
              <el-radio-button :value="1">通过</el-radio-button>
              <el-radio-button :value="2">拒绝</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="审核备注">
            <el-input
              v-model="reviewForm.auditRemark"
              type="textarea"
              :rows="4"
              placeholder="请填写审核备注"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
          <el-button
            v-if="isPending(currentCertification)"
            type="primary"
            :loading="submitLoading"
            @click="handleSubmitReview"
          >
            确认审核
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
