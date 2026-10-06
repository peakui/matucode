<script setup lang="ts">
import type { FormInstance, FormRules, TagProps } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SectionCard from '@/components/SectionCard/index.vue'
import {
  createAdminRefundApi,
  getAdminOrderListApi,
  getAdminRefundListApi,
  getAdminTransactionListApi,
} from '@/api'
import type { AdminOrderVO, AdminRefundVO, AdminTransactionVO } from '@/api/types'
import './index.scss'

const activeTab = ref('orders')

const orderLoading = ref(false)
const orderTotal = ref(0)
const orderData = ref<AdminOrderVO[]>([])

const transactionLoading = ref(false)
const transactionTotal = ref(0)
const transactionData = ref<AdminTransactionVO[]>([])

const refundLoading = ref(false)
const refundTotal = ref(0)
const refundData = ref<AdminRefundVO[]>([])

const refundDialogVisible = ref(false)
const refundSubmitting = ref(false)
const refundFormRef = ref<FormInstance>()

const orderFilter = reactive({
  orderNo: '',
  userId: '',
  status: undefined as number | undefined,
  productName: '',
  timeRange: [] as string[],
  pageNum: 1,
  pageSize: 10,
})

const transactionFilter = reactive({
  transactionNo: '',
  orderNo: '',
  channel: '',
  status: undefined as number | undefined,
  timeRange: [] as string[],
  pageNum: 1,
  pageSize: 10,
})

const refundFilter = reactive({
  refundNo: '',
  transactionNo: '',
  orderNo: '',
  status: undefined as number | undefined,
  timeRange: [] as string[],
  pageNum: 1,
  pageSize: 10,
})

const refundForm = reactive({
  transactionNo: '',
  refundAmount: undefined as number | undefined,
  reason: '',
})

const refundFormRules: FormRules = {
  transactionNo: [{ required: true, message: '请输入支付流水号', trigger: 'blur' }],
  refundAmount: [{ required: true, message: '请输入退款金额', trigger: 'blur' }],
  reason: [{ required: true, message: '请输入退款原因', trigger: 'blur' }],
}

const orderStatusOptions: Array<{ label: string; value: number; tagType: TagProps['type'] }> = [
  { label: '待支付', value: 0, tagType: 'info' },
  { label: '已支付', value: 1, tagType: 'success' },
  { label: '已关闭', value: 2, tagType: 'info' },
  { label: '已退款', value: 3, tagType: 'warning' },
  { label: '部分退款', value: 4, tagType: 'warning' },
]

const transactionStatusOptions: Array<{ label: string; value: number; tagType: TagProps['type'] }> = [
  { label: '待支付', value: 0, tagType: 'info' },
  { label: '成功', value: 1, tagType: 'success' },
  { label: '失败', value: 2, tagType: 'danger' },
  { label: '已关闭', value: 3, tagType: 'info' },
  { label: '处理中', value: 4, tagType: 'warning' },
]

const refundStatusOptions: Array<{ label: string; value: number; tagType: TagProps['type'] }> = [
  { label: '处理中', value: 0, tagType: 'warning' },
  { label: '成功', value: 1, tagType: 'success' },
  { label: '失败', value: 2, tagType: 'danger' },
]

const formatDateTime = (value?: string | null) => (!value ? '-' : value.replace('T', ' '))
const formatAmount = (value?: number | null) => (value === null || value === undefined ? '-' : `¥${Number(value).toFixed(2)}`)

const getStatusMeta = (
  options: Array<{ label: string; value: number; tagType: TagProps['type'] }>,
  status: number,
) => options.find((item) => item.value === status) ?? { label: `状态 ${status}`, value: status, tagType: 'info' as const }

const fetchOrderList = async () => {
  orderLoading.value = true

  try {
    const res = await getAdminOrderListApi({
      orderNo: orderFilter.orderNo || undefined,
      userId: orderFilter.userId || undefined,
      status: orderFilter.status,
      productName: orderFilter.productName || undefined,
      startTime: orderFilter.timeRange?.[0],
      endTime: orderFilter.timeRange?.[1],
      pageNum: orderFilter.pageNum,
      pageSize: orderFilter.pageSize,
    })
    orderData.value = res.data.records || []
    orderTotal.value = res.data.total || 0
  } finally {
    orderLoading.value = false
  }
}

const fetchTransactionList = async () => {
  transactionLoading.value = true

  try {
    const res = await getAdminTransactionListApi({
      transactionNo: transactionFilter.transactionNo || undefined,
      orderNo: transactionFilter.orderNo || undefined,
      channel: transactionFilter.channel || undefined,
      status: transactionFilter.status,
      startTime: transactionFilter.timeRange?.[0],
      endTime: transactionFilter.timeRange?.[1],
      pageNum: transactionFilter.pageNum,
      pageSize: transactionFilter.pageSize,
    })
    transactionData.value = res.data.records || []
    transactionTotal.value = res.data.total || 0
  } finally {
    transactionLoading.value = false
  }
}

const fetchRefundList = async () => {
  refundLoading.value = true

  try {
    const res = await getAdminRefundListApi({
      refundNo: refundFilter.refundNo || undefined,
      transactionNo: refundFilter.transactionNo || undefined,
      orderNo: refundFilter.orderNo || undefined,
      status: refundFilter.status,
      startTime: refundFilter.timeRange?.[0],
      endTime: refundFilter.timeRange?.[1],
      pageNum: refundFilter.pageNum,
      pageSize: refundFilter.pageSize,
    })
    refundData.value = res.data.records || []
    refundTotal.value = res.data.total || 0
  } finally {
    refundLoading.value = false
  }
}

const handleOrderSearch = () => {
  orderFilter.pageNum = 1
  void fetchOrderList()
}

const handleOrderReset = () => {
  orderFilter.orderNo = ''
  orderFilter.userId = ''
  orderFilter.status = undefined
  orderFilter.productName = ''
  orderFilter.timeRange = []
  orderFilter.pageNum = 1
  void fetchOrderList()
}

const handleTransactionSearch = () => {
  transactionFilter.pageNum = 1
  void fetchTransactionList()
}

const handleTransactionReset = () => {
  transactionFilter.transactionNo = ''
  transactionFilter.orderNo = ''
  transactionFilter.channel = ''
  transactionFilter.status = undefined
  transactionFilter.timeRange = []
  transactionFilter.pageNum = 1
  void fetchTransactionList()
}

const handleRefundSearch = () => {
  refundFilter.pageNum = 1
  void fetchRefundList()
}

const handleRefundReset = () => {
  refundFilter.refundNo = ''
  refundFilter.transactionNo = ''
  refundFilter.orderNo = ''
  refundFilter.status = undefined
  refundFilter.timeRange = []
  refundFilter.pageNum = 1
  void fetchRefundList()
}

const handleOrderPageChange = (page: number) => {
  orderFilter.pageNum = page
  void fetchOrderList()
}

const handleOrderSizeChange = (size: number) => {
  orderFilter.pageSize = size
  orderFilter.pageNum = 1
  void fetchOrderList()
}

const handleTransactionPageChange = (page: number) => {
  transactionFilter.pageNum = page
  void fetchTransactionList()
}

const handleTransactionSizeChange = (size: number) => {
  transactionFilter.pageSize = size
  transactionFilter.pageNum = 1
  void fetchTransactionList()
}

const handleRefundPageChange = (page: number) => {
  refundFilter.pageNum = page
  void fetchRefundList()
}

const handleRefundSizeChange = (size: number) => {
  refundFilter.pageSize = size
  refundFilter.pageNum = 1
  void fetchRefundList()
}

const openRefundDialog = (transactionNo = '') => {
  refundForm.transactionNo = transactionNo
  refundForm.refundAmount = undefined
  refundForm.reason = ''
  refundFormRef.value?.clearValidate()
  refundDialogVisible.value = true
}

const handleSubmitRefund = async () => {
  await refundFormRef.value?.validate()

  await ElMessageBox.confirm(
    `确认为流水 ${refundForm.transactionNo} 发起退款 ¥${Number(refundForm.refundAmount).toFixed(2)} 吗？该操作将调用支付宝真实退款，不可撤销。`,
    '发起退款',
    { type: 'warning', confirmButtonText: '确认退款', confirmButtonClass: 'el-button--danger' },
  )

  refundSubmitting.value = true

  try {
    await createAdminRefundApi({
      transactionNo: refundForm.transactionNo,
      refundAmount: Number(refundForm.refundAmount),
      reason: refundForm.reason,
    })
    ElMessage.success('退款已提交')
    refundDialogVisible.value = false
    activeTab.value = 'refunds'
    refundFilter.pageNum = 1
    void fetchRefundList()
  } finally {
    refundSubmitting.value = false
  }
}

onMounted(() => {
  void fetchOrderList()
  void fetchTransactionList()
  void fetchRefundList()
})
</script>

<template>
  <div class="pay-manage-page">
    <SectionCard class="pay-manage-page__content">
      <div class="filter-action-row">
        <el-button type="primary" @click="openRefundDialog()">发起退款</el-button>
      </div>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="订单" name="orders">
          <div class="filter-grid">
            <el-input v-model="orderFilter.orderNo" placeholder="订单号" clearable size="large" />
            <el-input v-model="orderFilter.userId" placeholder="用户 ID" clearable size="large" />
            <el-input v-model="orderFilter.productName" placeholder="商品名称" clearable size="large" />
            <el-select v-model="orderFilter.status" placeholder="订单状态" clearable size="large">
              <el-option v-for="item in orderStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-date-picker
              v-model="orderFilter.timeRange"
              type="datetimerange"
              value-format="YYYY-MM-DDTHH:mm:ss"
              range-separator="至"
              start-placeholder="创建开始时间"
              end-placeholder="创建结束时间"
              size="large"
            />
          </div>
          <div class="filter-actions">
            <el-button type="primary" size="large" @click="handleOrderSearch">搜索</el-button>
            <el-button size="large" @click="handleOrderReset">重置</el-button>
          </div>
          <el-table :data="orderData" v-loading="orderLoading" width="100%">
            <el-table-column prop="orderNo" label="订单号" min-width="200" show-overflow-tooltip />
            <el-table-column prop="userId" label="用户 ID" min-width="180" show-overflow-tooltip />
            <el-table-column prop="productName" label="商品名称" min-width="160" show-overflow-tooltip />
            <el-table-column label="金额" width="120">
              <template #default="scope">{{ formatAmount(scope.row.amount) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="110">
              <template #default="scope">
                <el-tag :type="getStatusMeta(orderStatusOptions, scope.row.status).tagType" effect="light">
                  {{ getStatusMeta(orderStatusOptions, scope.row.status).label }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="支付截止" min-width="170">
              <template #default="scope">{{ formatDateTime(scope.row.payDeadline) }}</template>
            </el-table-column>
            <el-table-column label="创建时间" min-width="170">
              <template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template>
            </el-table-column>
          </el-table>
          <div class="table-pagination">
            <el-pagination
              background
              layout="total, sizes, prev, pager, next, jumper"
              :current-page="orderFilter.pageNum"
              :page-size="orderFilter.pageSize"
              :page-sizes="[10, 20, 50]"
              :total="orderTotal"
              @current-change="handleOrderPageChange"
              @size-change="handleOrderSizeChange"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="支付流水" name="transactions">
          <div class="filter-grid">
            <el-input v-model="transactionFilter.transactionNo" placeholder="流水号" clearable size="large" />
            <el-input v-model="transactionFilter.orderNo" placeholder="订单号" clearable size="large" />
            <el-input v-model="transactionFilter.channel" placeholder="支付渠道" clearable size="large" />
            <el-select v-model="transactionFilter.status" placeholder="流水状态" clearable size="large">
              <el-option v-for="item in transactionStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-date-picker
              v-model="transactionFilter.timeRange"
              type="datetimerange"
              value-format="YYYY-MM-DDTHH:mm:ss"
              range-separator="至"
              start-placeholder="创建开始时间"
              end-placeholder="创建结束时间"
              size="large"
            />
          </div>
          <div class="filter-actions">
            <el-button type="primary" size="large" @click="handleTransactionSearch">搜索</el-button>
            <el-button size="large" @click="handleTransactionReset">重置</el-button>
          </div>
          <el-table :data="transactionData" v-loading="transactionLoading" width="100%">
            <el-table-column prop="transactionNo" label="流水号" min-width="200" show-overflow-tooltip />
            <el-table-column prop="orderNo" label="订单号" min-width="190" show-overflow-tooltip />
            <el-table-column prop="channel" label="渠道" width="110" />
            <el-table-column label="金额" width="120">
              <template #default="scope">{{ formatAmount(scope.row.amount) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="110">
              <template #default="scope">
                <el-tag :type="getStatusMeta(transactionStatusOptions, scope.row.status).tagType" effect="light">
                  {{ getStatusMeta(transactionStatusOptions, scope.row.status).label }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="支付时间" min-width="170">
              <template #default="scope">{{ formatDateTime(scope.row.payTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="scope">
                <el-button
                  v-if="scope.row.status === 1"
                  type="danger"
                  link
                  @click="openRefundDialog(scope.row.transactionNo)"
                >发起退款</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="table-pagination">
            <el-pagination
              background
              layout="total, sizes, prev, pager, next, jumper"
              :current-page="transactionFilter.pageNum"
              :page-size="transactionFilter.pageSize"
              :page-sizes="[10, 20, 50]"
              :total="transactionTotal"
              @current-change="handleTransactionPageChange"
              @size-change="handleTransactionSizeChange"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="退款记录" name="refunds">
          <div class="filter-grid">
            <el-input v-model="refundFilter.refundNo" placeholder="退款单号" clearable size="large" />
            <el-input v-model="refundFilter.transactionNo" placeholder="流水号" clearable size="large" />
            <el-input v-model="refundFilter.orderNo" placeholder="订单号" clearable size="large" />
            <el-select v-model="refundFilter.status" placeholder="退款状态" clearable size="large">
              <el-option v-for="item in refundStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-date-picker
              v-model="refundFilter.timeRange"
              type="datetimerange"
              value-format="YYYY-MM-DDTHH:mm:ss"
              range-separator="至"
              start-placeholder="创建开始时间"
              end-placeholder="创建结束时间"
              size="large"
            />
          </div>
          <div class="filter-actions">
            <el-button type="primary" size="large" @click="handleRefundSearch">搜索</el-button>
            <el-button size="large" @click="handleRefundReset">重置</el-button>
          </div>
          <el-table :data="refundData" v-loading="refundLoading" width="100%">
            <el-table-column prop="refundNo" label="退款单号" min-width="200" show-overflow-tooltip />
            <el-table-column prop="transactionNo" label="流水号" min-width="190" show-overflow-tooltip />
            <el-table-column prop="orderNo" label="订单号" min-width="190" show-overflow-tooltip />
            <el-table-column label="退款金额" width="120">
              <template #default="scope">{{ formatAmount(scope.row.refundAmount) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="110">
              <template #default="scope">
                <el-tag :type="getStatusMeta(refundStatusOptions, scope.row.status).tagType" effect="light">
                  {{ getStatusMeta(refundStatusOptions, scope.row.status).label }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="退款原因" min-width="180" show-overflow-tooltip>
              <template #default="scope">{{ scope.row.reason || '-' }}</template>
            </el-table-column>
            <el-table-column label="退款时间" min-width="170">
              <template #default="scope">{{ formatDateTime(scope.row.refundTime) }}</template>
            </el-table-column>
          </el-table>
          <div class="table-pagination">
            <el-pagination
              background
              layout="total, sizes, prev, pager, next, jumper"
              :current-page="refundFilter.pageNum"
              :page-size="refundFilter.pageSize"
              :page-sizes="[10, 20, 50]"
              :total="refundTotal"
              @current-change="handleRefundPageChange"
              @size-change="handleRefundSizeChange"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </SectionCard>

    <el-dialog v-model="refundDialogVisible" title="发起退款" width="560px">
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="该操作会调用支付宝真实退款且不可撤销，请确认流水号与金额无误。"
        class="refund-dialog__alert"
      />
      <el-form ref="refundFormRef" :model="refundForm" :rules="refundFormRules" label-position="top">
        <el-form-item label="支付流水号" prop="transactionNo">
          <el-input v-model="refundForm.transactionNo" placeholder="请输入支付流水号" size="large" />
        </el-form-item>
        <el-form-item label="退款金额（元）" prop="refundAmount">
          <el-input-number v-model="refundForm.refundAmount" :min="0.01" :precision="2" :step="1" controls-position="right" size="large" />
        </el-form-item>
        <el-form-item label="退款原因" prop="reason">
          <el-input v-model="refundForm.reason" type="textarea" :rows="3" maxlength="200" show-word-limit placeholder="请输入退款原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="refundDialogVisible = false">取消</el-button>
          <el-button type="danger" :loading="refundSubmitting" @click="handleSubmitRefund">确认退款</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
