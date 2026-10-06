<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SectionCard from '@/components/SectionCard/index.vue'
import DetailMetricsGrid from '@/components/DetailMetricsGrid/index.vue'
import { getAdminSearchStatusApi, reindexAdminSearchApi } from '@/api'
import type { IndexStatusVO, SyncResultVO } from '@/api/types'
import './index.scss'

const loading = ref(false)
const reindexLoading = ref(false)
const status = ref<IndexStatusVO | null>(null)
const lastResult = ref<SyncResultVO | null>(null)
const errorMessage = ref('')

const fetchStatus = async () => {
  loading.value = true
  errorMessage.value = ''

  try {
    const res = await getAdminSearchStatusApi()
    status.value = res.data
  } catch {
    status.value = null
    errorMessage.value = '搜索引擎不可用，请确认 Elasticsearch 已启动。'
  } finally {
    loading.value = false
  }
}

const handleReindex = async () => {
  await ElMessageBox.confirm(
    '确认触发全量重建索引吗？该操作会从各业务服务拉取全部公开文档并整体切换索引，过程可能持续数十秒。',
    '重建索引',
    { type: 'warning' },
  )

  reindexLoading.value = true
  lastResult.value = null

  try {
    const res = await reindexAdminSearchApi()
    lastResult.value = res.data

    if (res.data.success) {
      ElMessage.success(`重建完成，共写入 ${res.data.documentCount} 条文档`)
    } else {
      ElMessage.error(res.data.message || '重建失败')
    }

    await fetchStatus()
  } finally {
    reindexLoading.value = false
  }
}

onMounted(() => {
  void fetchStatus()
})
</script>

<template>
  <div class="search-manage-page">
    <div class="filter-action-row">
      <el-button type="primary" @click="handleReindex">重建索引</el-button>
    </div>

    <SectionCard v-loading="loading || reindexLoading" class="search-manage-page__status">
      <el-alert
        v-if="errorMessage"
        type="error"
        :closable="false"
        show-icon
        :title="errorMessage"
        class="search-status__alert"
      />
      <template v-else-if="status">
        <div class="search-status__badges">
          <el-tag :type="status.indexExists ? 'success' : 'info'" effect="light">
            索引：{{ status.indexExists ? '已创建' : '尚未创建' }}
          </el-tag>
          <el-tag type="primary" effect="light">别名：{{ status.alias }}</el-tag>
        </div>
        <DetailMetricsGrid
          :items="[
            { label: '文档数量', value: status.documentCount },
            { label: '文档上限', value: status.maxDocuments },
          ]"
        />
      </template>
    </SectionCard>

    <SectionCard v-if="lastResult" class="search-manage-page__result">
      <h3 class="section-subtitle">最近一次重建结果</h3>
      <el-alert
        :type="lastResult.success ? 'success' : 'error'"
        :closable="false"
        show-icon
        :title="lastResult.success ? `重建成功，写入 ${lastResult.documentCount} 条文档` : `重建失败：${lastResult.message}`"
      />
    </SectionCard>
  </div>
</template>
