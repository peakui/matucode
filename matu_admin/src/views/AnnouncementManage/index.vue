<script setup lang="ts">
import type { FormInstance, FormRules, TagProps } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SectionCard from '@/components/SectionCard/index.vue'
import DetailMetricsGrid from '@/components/DetailMetricsGrid/index.vue'
import {
  createAdminAnnouncementApi,
  deleteAdminAnnouncementApi,
  getAdminAnnouncementDetailApi,
  getAdminAnnouncementListApi,
  offlineAdminAnnouncementApi,
  publishAdminAnnouncementApi,
  updateAdminAnnouncementApi,
} from '@/api'
import type {
  AnnouncementStatus,
  AnnouncementType,
  AnnouncementVO,
  CreateAnnouncementRequest,
} from '@/api/types'
import './index.scss'

const loading = ref(false)
const detailLoading = ref(false)
const submitLoading = ref(false)
const detailVisible = ref(false)
const formVisible = ref(false)
const isEditMode = ref(false)
const total = ref(0)
const tableData = ref<AnnouncementVO[]>([])
const currentAnnouncement = ref<AnnouncementVO | null>(null)
const formRef = ref<FormInstance>()

const filterForm = reactive({
  keyword: '',
  type: undefined as AnnouncementType | undefined,
  status: undefined as AnnouncementStatus | undefined,
  pageNum: 1,
  pageSize: 10,
})

const announcementForm = reactive<CreateAnnouncementRequest>({
  title: '',
  content: '',
  type: 0,
  priority: 0,
  isPinned: 0,
  publishTime: undefined,
  expireTime: undefined,
  status: 1,
})

const announcementTypeOptions: Array<{ label: string; value: AnnouncementType; tagType: TagProps['type'] }> = [
  { label: '普通通知', value: 0, tagType: 'info' },
  { label: '重要公告', value: 1, tagType: 'warning' },
  { label: '维护通知', value: 2, tagType: 'danger' },
  { label: '活动推广', value: 3, tagType: 'success' },
]

const statusOptions: Array<{ label: string; value: AnnouncementStatus; tagType: TagProps['type'] }> = [
  { label: '草稿', value: 0, tagType: 'info' },
  { label: '已发布', value: 1, tagType: 'success' },
  { label: '已下架', value: 2, tagType: 'warning' },
]

const validateExpireTime = (_rule: unknown, value: string | undefined, callback: (error?: Error) => void) => {
  if (announcementForm.publishTime && value && new Date(value).getTime() <= new Date(announcementForm.publishTime).getTime()) {
    callback(new Error('过期时间需晚于发布时间'))
    return
  }

  callback()
}

const announcementFormRules: FormRules<CreateAnnouncementRequest> = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }],
  type: [{ required: true, message: '请选择公告类型', trigger: 'change' }],
  expireTime: [{ validator: validateExpireTime, trigger: 'change' }],
}

const formatDateTime = (value?: string | null) => (!value ? '-' : value.replace('T', ' '))

const getTypeMeta = (type: AnnouncementType): { label: string; value: AnnouncementType; tagType: TagProps['type'] } =>
  announcementTypeOptions.find((item) => item.value === type) ?? announcementTypeOptions[0]!

const getStatusMeta = (status: AnnouncementStatus): { label: string; value: AnnouncementStatus; tagType: TagProps['type'] } =>
  statusOptions.find((item) => item.value === status) ?? statusOptions[0]!

const fetchAnnouncementList = async () => {
  loading.value = true

  try {
    const res = await getAdminAnnouncementListApi({
      keyword: filterForm.keyword || undefined,
      type: filterForm.type,
      status: filterForm.status,
      pageNum: filterForm.pageNum,
      pageSize: filterForm.pageSize,
    })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const fetchAnnouncementDetail = async (id: string) => {
  detailLoading.value = true

  try {
    const res = await getAdminAnnouncementDetailApi(id)
    currentAnnouncement.value = res.data
    return res.data
  } finally {
    detailLoading.value = false
  }
}

const resetAnnouncementForm = () => {
  announcementForm.title = ''
  announcementForm.content = ''
  announcementForm.type = 0
  announcementForm.priority = 0
  announcementForm.isPinned = 0
  announcementForm.publishTime = undefined
  announcementForm.expireTime = undefined
  announcementForm.status = 1
  formRef.value?.clearValidate()
}

const handleSearch = () => {
  filterForm.pageNum = 1
  void fetchAnnouncementList()
}

const handleReset = () => {
  filterForm.keyword = ''
  filterForm.type = undefined
  filterForm.status = undefined
  filterForm.pageNum = 1
  void fetchAnnouncementList()
}

const handleCreate = () => {
  isEditMode.value = false
  currentAnnouncement.value = null
  resetAnnouncementForm()
  formVisible.value = true
}

const fillAnnouncementForm = (detail: AnnouncementVO) => {
  announcementForm.title = detail.title
  announcementForm.content = detail.content || ''
  announcementForm.type = detail.type
  announcementForm.priority = detail.priority
  announcementForm.isPinned = detail.isPinned
  announcementForm.publishTime = detail.publishTime
  announcementForm.expireTime = detail.expireTime
  announcementForm.status = detail.status
}

const handleEdit = async (row: AnnouncementVO) => {
  isEditMode.value = true
  formVisible.value = true
  resetAnnouncementForm()
  const detail = await fetchAnnouncementDetail(row.id)
  fillAnnouncementForm(detail)
}

const handleView = async (row: AnnouncementVO) => {
  detailVisible.value = true
  await fetchAnnouncementDetail(row.id)
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  submitLoading.value = true

  try {
    const payload: CreateAnnouncementRequest = { ...announcementForm }

    if (isEditMode.value && currentAnnouncement.value) {
      await updateAdminAnnouncementApi(currentAnnouncement.value.id, payload)
      ElMessage.success('公告更新成功')
    } else {
      await createAdminAnnouncementApi(payload)
      ElMessage.success('公告创建成功')
    }

    formVisible.value = false
    resetAnnouncementForm()
    void fetchAnnouncementList()
  } finally {
    submitLoading.value = false
  }
}

const handlePublish = async (row: AnnouncementVO) => {
  await ElMessageBox.confirm(`确认发布公告《${row.title}》吗？`, '发布公告', { type: 'warning' })
  await publishAdminAnnouncementApi(row.id)
  ElMessage.success('发布成功')
  void fetchAnnouncementList()
}

const handleOffline = async (row: AnnouncementVO) => {
  await ElMessageBox.confirm(`确认下架公告《${row.title}》吗？`, '下架公告', { type: 'warning' })
  await offlineAdminAnnouncementApi(row.id)
  ElMessage.success('下架成功')
  void fetchAnnouncementList()
}

const handleDelete = async (row: AnnouncementVO) => {
  await ElMessageBox.confirm(`确认删除或下架公告《${row.title}》吗？`, '删除公告', { type: 'warning' })
  await deleteAdminAnnouncementApi(row.id)
  ElMessage.success('操作成功')
  void fetchAnnouncementList()
}

const handlePageChange = (page: number) => {
  filterForm.pageNum = page
  void fetchAnnouncementList()
}

const handleSizeChange = (size: number) => {
  filterForm.pageSize = size
  filterForm.pageNum = 1
  void fetchAnnouncementList()
}

onMounted(async () => {
  await fetchAnnouncementList()
})
</script>

<template>
  <div class="announcement-manage-page">
    <SectionCard class="announcement-manage-page__filter">
      <div class="filter-action-row">
        <el-button type="primary" @click="handleCreate">新建公告</el-button>
      </div>
      <div class="filter-toolbar">
        <el-input v-model="filterForm.keyword" placeholder="请输入公告标题或内容关键词" clearable size="large" />
        <el-button type="primary" size="large" @click="handleSearch">搜索</el-button>
        <el-button size="large" @click="handleReset">重置</el-button>
      </div>
      <div class="filter-grid">
        <el-select v-model="filterForm.type" placeholder="公告类型" clearable size="large">
          <el-option v-for="item in announcementTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="filterForm.status" placeholder="公告状态" clearable size="large">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </div>
    </SectionCard>

    <SectionCard class="announcement-manage-page__table">
      <el-table :data="tableData" v-loading="loading" width="100%">
        <el-table-column prop="title" label="公告标题" min-width="220" show-overflow-tooltip />
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
        <el-table-column prop="priority" label="优先级" width="100" />
        <el-table-column label="置顶" width="90">
          <template #default="scope">{{ scope.row.isPinned ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column prop="clickCount" label="点击数" width="100" />
        <el-table-column label="发布时间" min-width="170">
          <template #default="scope">{{ formatDateTime(scope.row.publishTime) }}</template>
        </el-table-column>
        <el-table-column label="过期时间" min-width="170">
          <template #default="scope">{{ formatDateTime(scope.row.expireTime) }}</template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="170">
          <template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="scope">
            <el-button type="primary" link @click="handleView(scope.row)">查看</el-button>
            <el-button type="success" link @click="handleEdit(scope.row)">编辑</el-button>
            <el-button v-if="scope.row.status !== 1" type="warning" link @click="handlePublish(scope.row)">发布</el-button>
            <el-button v-if="scope.row.status === 1" type="warning" link @click="handleOffline(scope.row)">下架</el-button>
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

    <el-dialog v-model="formVisible" :title="isEditMode ? '编辑公告' : '新建公告'" width="780px">
      <el-form ref="formRef" :model="announcementForm" :rules="announcementFormRules" label-position="top" class="announcement-form">
        <div class="announcement-form__grid">
          <el-form-item label="公告类型" prop="type">
            <el-select v-model="announcementForm.type" placeholder="请选择公告类型" size="large">
              <el-option v-for="item in announcementTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="发布状态" prop="status">
            <el-select v-model="announcementForm.status" placeholder="请选择发布状态" size="large">
              <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="公告标题" prop="title">
          <el-input v-model="announcementForm.title" placeholder="请输入公告标题" size="large" maxlength="255" show-word-limit />
        </el-form-item>
        <el-form-item label="公告内容" prop="content">
          <el-input v-model="announcementForm.content" type="textarea" :rows="8" placeholder="请输入公告内容" maxlength="5000" show-word-limit />
        </el-form-item>
        <div class="announcement-form__grid">
          <el-form-item label="排序权重" prop="priority">
            <el-input-number v-model="announcementForm.priority" :min="0" :max="9999" controls-position="right" size="large" />
          </el-form-item>
          <el-form-item label="是否置顶" prop="isPinned">
            <el-switch v-model="announcementForm.isPinned" :active-value="1" :inactive-value="0" active-text="置顶" inactive-text="不置顶" />
          </el-form-item>
        </div>
        <div class="announcement-form__grid">
          <el-form-item label="发布时间" prop="publishTime">
            <el-date-picker v-model="announcementForm.publishTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="请选择发布时间" size="large" />
          </el-form-item>
          <el-form-item label="过期时间" prop="expireTime">
            <el-date-picker v-model="announcementForm.expireTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="请选择过期时间" size="large" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="formVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">{{ isEditMode ? '保存修改' : '确认创建' }}</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="公告详情" width="760px">
      <div v-loading="detailLoading" class="announcement-detail">
        <template v-if="currentAnnouncement">
          <div class="announcement-detail__item">
            <span>标题</span><strong>{{ currentAnnouncement.title }}</strong>
          </div>
          <div class="announcement-detail__item">
            <span>内容</span>
            <p>{{ currentAnnouncement.content || '-' }}</p>
          </div>
          <DetailMetricsGrid
            :items="[
              { label: '类型', value: getTypeMeta(currentAnnouncement.type).label },
              { label: '状态', value: getStatusMeta(currentAnnouncement.status).label },
              { label: '优先级', value: currentAnnouncement.priority },
              { label: '置顶', value: currentAnnouncement.isPinned ? '是' : '否' },
            ]"
          />
          <DetailMetricsGrid
            :items="[
              { label: '点击数', value: currentAnnouncement.clickCount },
              { label: '作者 ID', value: currentAnnouncement.authorId || '-' },
              { label: '发布时间', value: formatDateTime(currentAnnouncement.publishTime) },
              { label: '过期时间', value: formatDateTime(currentAnnouncement.expireTime) },
            ]"
          />
          <DetailMetricsGrid
            :items="[
              { label: '创建时间', value: formatDateTime(currentAnnouncement.createdAt) },
              { label: '更新时间', value: formatDateTime(currentAnnouncement.updatedAt) },
            ]"
          />
        </template>
      </div>
    </el-dialog>
  </div>
</template>
