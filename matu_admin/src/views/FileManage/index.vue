<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SectionCard from '@/components/SectionCard/index.vue'
import { deleteFileApi, getFileDownloadUrlApi, listFileBucketsApi, listFilesApi } from '@/api'
import type { FileBucketVO, FileInfoVO } from '@/api/types'
import './index.scss'

const activeTab = ref('files')

const loading = ref(false)
const total = ref(0)
const fileData = ref<FileInfoVO[]>([])

const bucketLoading = ref(false)
const bucketData = ref<FileBucketVO[]>([])

const filterForm = reactive({
  ownerId: '',
  bucketName: undefined as string | undefined,
  fileType: '',
  status: undefined as number | undefined,
  pageNum: 1,
  pageSize: 10,
})

const statusOptions = [
  { label: '上传中', value: 0, tagType: 'warning' as const },
  { label: '正常', value: 1, tagType: 'success' as const },
  { label: '已删除', value: 3, tagType: 'danger' as const },
]

const formatDateTime = (value?: string | null) => (!value ? '-' : value.replace('T', ' '))

const formatFileSize = (bytes?: number | null) => {
  if (bytes === null || bytes === undefined) {
    return '-'
  }

  if (bytes < 1024) {
    return `${bytes} B`
  }

  const units = ['KB', 'MB', 'GB', 'TB']
  let value = bytes / 1024
  let unitIndex = 0

  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024
    unitIndex += 1
  }

  return `${value.toFixed(2)} ${units[unitIndex]}`
}

const getStatusMeta = (status?: number) =>
  statusOptions.find((item) => item.value === status) ?? { label: `状态 ${status}`, value: status, tagType: 'info' as const }

const fetchFileList = async () => {
  loading.value = true

  try {
    const res = await listFilesApi({
      ownerId: filterForm.ownerId || undefined,
      bucketName: filterForm.bucketName,
      fileType: filterForm.fileType || undefined,
      status: filterForm.status,
      pageNum: filterForm.pageNum,
      pageSize: filterForm.pageSize,
    })
    fileData.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

const fetchBuckets = async () => {
  bucketLoading.value = true

  try {
    bucketData.value = await listFileBucketsApi()
  } finally {
    bucketLoading.value = false
  }
}

const handleSearch = () => {
  filterForm.pageNum = 1
  void fetchFileList()
}

const handleReset = () => {
  filterForm.ownerId = ''
  filterForm.bucketName = undefined
  filterForm.fileType = ''
  filterForm.status = undefined
  filterForm.pageNum = 1
  void fetchFileList()
}

const handlePageChange = (page: number) => {
  filterForm.pageNum = page
  void fetchFileList()
}

const handleSizeChange = (size: number) => {
  filterForm.pageSize = size
  filterForm.pageNum = 1
  void fetchFileList()
}

const handleDownload = async (row: FileInfoVO) => {
  const fileId = row.id ?? row.fileId

  if (fileId === undefined || fileId === null) {
    ElMessage.warning('文件 ID 缺失，无法获取下载链接')
    return
  }

  const res = await getFileDownloadUrlApi(fileId)

  if (!res.signedUrl) {
    ElMessage.error('获取下载链接失败')
    return
  }

  window.open(res.signedUrl, '_blank', 'noopener')
}

const handleDelete = async (row: FileInfoVO) => {
  const fileId = row.id ?? row.fileId

  if (fileId === undefined || fileId === null) {
    ElMessage.warning('文件 ID 缺失，无法删除')
    return
  }

  await ElMessageBox.confirm(
    `确认删除文件《${row.originalName || row.fileName || fileId}》吗？该操作会同时删除对象存储中的文件，不可撤销。`,
    '删除文件',
    { type: 'warning' },
  )

  await deleteFileApi(fileId)
  ElMessage.success('删除成功')
  void fetchFileList()
}

onMounted(() => {
  void fetchFileList()
  void fetchBuckets()
})
</script>

<template>
  <div class="file-manage-page">
    <SectionCard class="file-manage-page__content">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="文件列表" name="files">
          <div class="filter-grid">
            <el-input v-model="filterForm.ownerId" placeholder="所有者 ID（留空查看全部）" clearable size="large" />
            <el-select v-model="filterForm.bucketName" placeholder="存储桶" clearable size="large">
              <el-option
                v-for="bucket in bucketData"
                :key="bucket.bucketName"
                :label="bucket.bucketName"
                :value="bucket.bucketName"
              />
            </el-select>
            <el-input v-model="filterForm.fileType" placeholder="文件类型（如 image/png）" clearable size="large" />
            <el-select v-model="filterForm.status" placeholder="文件状态" clearable size="large">
              <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </div>
          <div class="filter-actions">
            <el-button type="primary" size="large" @click="handleSearch">搜索</el-button>
            <el-button size="large" @click="handleReset">重置</el-button>
          </div>
          <el-table :data="fileData" v-loading="loading" width="100%">
            <el-table-column prop="id" label="文件 ID" width="200" show-overflow-tooltip />
            <el-table-column label="文件名" min-width="200" show-overflow-tooltip>
              <template #default="scope">{{ scope.row.originalName || scope.row.fileName || '-' }}</template>
            </el-table-column>
            <el-table-column label="类型" width="140" show-overflow-tooltip>
              <template #default="scope">{{ scope.row.fileType || '-' }}</template>
            </el-table-column>
            <el-table-column label="大小" width="110">
              <template #default="scope">{{ formatFileSize(scope.row.fileSize) }}</template>
            </el-table-column>
            <el-table-column label="存储桶" width="140" show-overflow-tooltip>
              <template #default="scope">{{ scope.row.bucketName || '-' }}</template>
            </el-table-column>
            <el-table-column prop="ownerId" label="所有者 ID" width="190" show-overflow-tooltip />
            <el-table-column label="公开" width="90">
              <template #default="scope">
                <el-tag :type="scope.row.isPublic === 1 ? 'success' : 'info'" effect="light">
                  {{ scope.row.isPublic === 1 ? '公开' : '私有' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="scope">
                <el-tag :type="getStatusMeta(scope.row.status).tagType" effect="light">
                  {{ getStatusMeta(scope.row.status).label }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="downloadCount" label="下载数" width="90" />
            <el-table-column label="创建时间" min-width="170">
              <template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="scope">
                <el-button type="primary" link @click="handleDownload(scope.row)">下载</el-button>
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
        </el-tab-pane>

        <el-tab-pane label="存储桶" name="buckets">
          <el-table :data="bucketData" v-loading="bucketLoading" width="100%">
            <el-table-column prop="bucketName" label="存储桶" min-width="180" />
            <el-table-column label="存储供应商" width="150">
              <template #default="scope">{{ scope.row.storageProvider || '-' }}</template>
            </el-table-column>
            <el-table-column label="区域" width="120">
              <template #default="scope">{{ scope.row.region || '-' }}</template>
            </el-table-column>
            <el-table-column label="文件数" width="100">
              <template #default="scope">{{ scope.row.fileCount ?? '-' }}</template>
            </el-table-column>
            <el-table-column label="已用容量" width="120">
              <template #default="scope">{{ formatFileSize(scope.row.usedSize) }}</template>
            </el-table-column>
            <el-table-column label="容量上限" width="120">
              <template #default="scope">{{ formatFileSize(scope.row.maxSize) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="scope">
                <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" effect="light">
                  {{ scope.row.status === 1 ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </SectionCard>
  </div>
</template>
