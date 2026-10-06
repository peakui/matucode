<script setup lang="ts">
import type { FormInstance, FormRules, TagProps } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SectionCard from '@/components/SectionCard/index.vue'
import DetailMetricsGrid from '@/components/DetailMetricsGrid/index.vue'
import {
  createAdminSysConfigApi,
  deleteAdminSysConfigApi,
  getAdminSysConfigDetailApi,
  getAdminSysConfigListApi,
  updateAdminSysConfigApi,
} from '@/api'
import type { CreateSysConfigRequest, SysConfigVO } from '@/api/types'
import './index.scss'

const loading = ref(false)
const detailLoading = ref(false)
const submitLoading = ref(false)
const detailVisible = ref(false)
const formVisible = ref(false)
const isEditMode = ref(false)
const total = ref(0)
const tableData = ref<SysConfigVO[]>([])
const currentConfig = ref<SysConfigVO | null>(null)
const formRef = ref<FormInstance>()

const filterForm = reactive({
  keyword: '',
  groupName: '',
  isPublic: undefined as number | undefined,
  pageNum: 1,
  pageSize: 10,
})

const configForm = reactive<CreateSysConfigRequest>({
  configKey: '',
  configValue: '',
  description: '',
  groupName: '',
  isPublic: 0,
})

const publicOptions: Array<{ label: string; value: number; tagType: TagProps['type'] }> = [
  { label: '私有', value: 0, tagType: 'info' },
  { label: '公开', value: 1, tagType: 'success' },
]

const configFormRules: FormRules<CreateSysConfigRequest> = {
  configKey: [{ required: true, message: '请输入配置键', trigger: 'blur' }],
}

const formatDateTime = (value?: string) => (!value ? '-' : value.replace('T', ' '))

const getPublicMeta = (isPublic?: number) =>
  publicOptions.find((item) => item.value === isPublic) ?? publicOptions[0]!

const fetchConfigList = async () => {
  loading.value = true

  try {
    const res = await getAdminSysConfigListApi({
      keyword: filterForm.keyword || undefined,
      groupName: filterForm.groupName || undefined,
      isPublic: filterForm.isPublic,
      pageNum: filterForm.pageNum,
      pageSize: filterForm.pageSize,
    })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const fetchConfigDetail = async (configKey: string) => {
  detailLoading.value = true

  try {
    const res = await getAdminSysConfigDetailApi(configKey)
    currentConfig.value = res.data
    return res.data
  } finally {
    detailLoading.value = false
  }
}

const resetConfigForm = () => {
  configForm.configKey = ''
  configForm.configValue = ''
  configForm.description = ''
  configForm.groupName = ''
  configForm.isPublic = 0
  formRef.value?.clearValidate()
}

const handleSearch = () => {
  filterForm.pageNum = 1
  void fetchConfigList()
}

const handleReset = () => {
  filterForm.keyword = ''
  filterForm.groupName = ''
  filterForm.isPublic = undefined
  filterForm.pageNum = 1
  void fetchConfigList()
}

const handleCreate = () => {
  isEditMode.value = false
  currentConfig.value = null
  resetConfigForm()
  formVisible.value = true
}

const handleEdit = async (row: SysConfigVO) => {
  isEditMode.value = true
  formVisible.value = true
  resetConfigForm()
  const detail = await fetchConfigDetail(row.configKey)
  configForm.configKey = detail.configKey
  configForm.configValue = detail.configValue || ''
  configForm.description = detail.description || ''
  configForm.groupName = detail.groupName || ''
  configForm.isPublic = detail.isPublic ?? 0
}

const handleView = async (row: SysConfigVO) => {
  detailVisible.value = true
  await fetchConfigDetail(row.configKey)
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  submitLoading.value = true

  try {
    if (isEditMode.value && currentConfig.value) {
      await updateAdminSysConfigApi(currentConfig.value.configKey, {
        configValue: configForm.configValue,
        description: configForm.description,
        groupName: configForm.groupName,
        isPublic: configForm.isPublic,
      })
      ElMessage.success('配置更新成功')
    } else {
      await createAdminSysConfigApi({ ...configForm })
      ElMessage.success('配置创建成功')
    }

    formVisible.value = false
    resetConfigForm()
    void fetchConfigList()
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = async (row: SysConfigVO) => {
  await ElMessageBox.confirm(`确认删除配置项《${row.configKey}》吗？`, '删除配置', { type: 'warning' })
  await deleteAdminSysConfigApi(row.configKey)
  ElMessage.success('删除成功')
  void fetchConfigList()
}

const handlePageChange = (page: number) => {
  filterForm.pageNum = page
  void fetchConfigList()
}

const handleSizeChange = (size: number) => {
  filterForm.pageSize = size
  filterForm.pageNum = 1
  void fetchConfigList()
}

onMounted(async () => {
  await fetchConfigList()
})
</script>

<template>
  <div class="sys-config-manage-page">
    <SectionCard class="sys-config-manage-page__filter">
      <div class="filter-action-row">
        <el-button type="primary" @click="handleCreate">新建配置</el-button>
      </div>
      <div class="filter-toolbar">
        <el-input v-model="filterForm.keyword" placeholder="请输入配置键、值或说明关键词" clearable size="large" />
        <el-button type="primary" size="large" @click="handleSearch">搜索</el-button>
        <el-button size="large" @click="handleReset">重置</el-button>
      </div>
      <div class="filter-grid">
        <el-input v-model="filterForm.groupName" placeholder="配置分组" clearable size="large" />
        <el-select v-model="filterForm.isPublic" placeholder="公开状态" clearable size="large">
          <el-option v-for="item in publicOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </div>
    </SectionCard>

    <SectionCard class="sys-config-manage-page__table">
      <el-table :data="tableData" v-loading="loading" width="100%">
        <el-table-column prop="configKey" label="配置键" min-width="200" show-overflow-tooltip />
        <el-table-column prop="configValue" label="配置值" min-width="220" show-overflow-tooltip />
        <el-table-column prop="groupName" label="分组" width="140" />
        <el-table-column label="公开状态" width="110">
          <template #default="scope">
            <el-tag :type="getPublicMeta(scope.row.isPublic).tagType" effect="light">
              {{ getPublicMeta(scope.row.isPublic).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="200" show-overflow-tooltip />
        <el-table-column label="更新时间" min-width="170">
          <template #default="scope">{{ formatDateTime(scope.row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button type="primary" link @click="handleView(scope.row)">查看</el-button>
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

    <el-dialog v-model="formVisible" :title="isEditMode ? '编辑配置' : '新建配置'" width="680px">
      <el-form ref="formRef" :model="configForm" :rules="configFormRules" label-position="top" class="sys-config-form">
        <el-form-item label="配置键" prop="configKey">
          <el-input
            v-model="configForm.configKey"
            placeholder="例如 site.name"
            size="large"
            maxlength="128"
            :disabled="isEditMode"
          />
        </el-form-item>
        <el-form-item label="配置值" prop="configValue">
          <el-input v-model="configForm.configValue" type="textarea" :rows="4" placeholder="请输入配置值" />
        </el-form-item>
        <div class="sys-config-form__grid">
          <el-form-item label="配置分组" prop="groupName">
            <el-input v-model="configForm.groupName" placeholder="例如 base / feature" size="large" maxlength="64" />
          </el-form-item>
          <el-form-item label="公开状态" prop="isPublic">
            <el-select v-model="configForm.isPublic" placeholder="请选择公开状态" size="large">
              <el-option v-for="item in publicOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="配置说明" prop="description">
          <el-input v-model="configForm.description" placeholder="请输入配置说明" size="large" maxlength="255" />
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

    <el-dialog v-model="detailVisible" title="配置详情" width="680px">
      <div v-loading="detailLoading" class="sys-config-detail">
        <template v-if="currentConfig">
          <DetailMetricsGrid
            :items="[
              { label: '配置键', value: currentConfig.configKey },
              { label: '分组', value: currentConfig.groupName || '-' },
              { label: '公开状态', value: getPublicMeta(currentConfig.isPublic).label },
              { label: '更新时间', value: formatDateTime(currentConfig.updatedAt) },
            ]"
          />
          <div class="sys-config-detail__item">
            <span>配置值</span>
            <p>{{ currentConfig.configValue || '-' }}</p>
          </div>
          <div class="sys-config-detail__item">
            <span>说明</span>
            <p>{{ currentConfig.description || '-' }}</p>
          </div>
        </template>
      </div>
    </el-dialog>
  </div>
</template>
