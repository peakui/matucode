<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import SectionCard from '@/components/SectionCard/index.vue'
import DetailMetricsGrid from '@/components/DetailMetricsGrid/index.vue'
import MarkdownContent from '@/components/MarkdownContent/index.vue'
import {
  getAdminAiConversationListApi,
  getAdminAiMessageListApi,
  getAdminAiStatusApi,
  getAdminAiTaskApi,
  getAdminMcpServerListApi,
  getAdminMcpToolListApi,
} from '@/api'
import type { AdminAiConversationVO, AdminAiMessageVO, AiAdminStatusVO, McpServerVO, McpToolVO } from '@/api/types'
import './index.scss'

const activeTab = ref('conversations')

const statusLoading = ref(false)
const status = ref<AiAdminStatusVO | null>(null)

const loading = ref(false)
const total = ref(0)
const conversationData = ref<AdminAiConversationVO[]>([])

const messageLoading = ref(false)
const messageTotal = ref(0)
const messageData = ref<AdminAiMessageVO[]>([])
const messageVisible = ref(false)
const activeConversation = ref<AdminAiConversationVO | null>(null)

const mcpServerLoading = ref(false)
const mcpToolLoading = ref(false)
const mcpServers = ref<McpServerVO[]>([])
const mcpTools = ref<McpToolVO[]>([])

const taskLoading = ref(false)
const taskQueryId = ref('')
const taskResult = ref<Record<string, unknown> | null>(null)

const filterForm = reactive({
  keyword: '',
  pageNum: 1,
  pageSize: 10,
})

const messageFilter = reactive({
  userId: '',
  pageNum: 1,
  pageSize: 20,
})

const formatDateTime = (value?: string | null) => (!value ? '-' : value.replace('T', ' '))

const fetchStatus = async () => {
  statusLoading.value = true

  try {
    const res = await getAdminAiStatusApi()
    status.value = res.data
  } finally {
    statusLoading.value = false
  }
}

const fetchConversationList = async () => {
  loading.value = true

  try {
    const res = await getAdminAiConversationListApi({
      keyword: filterForm.keyword || undefined,
      pageNum: filterForm.pageNum,
      pageSize: filterForm.pageSize,
    })
    conversationData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  filterForm.pageNum = 1
  void fetchConversationList()
}

const handleReset = () => {
  filterForm.keyword = ''
  filterForm.pageNum = 1
  void fetchConversationList()
}

const handlePageChange = (page: number) => {
  filterForm.pageNum = page
  void fetchConversationList()
}

const handleSizeChange = (size: number) => {
  filterForm.pageSize = size
  filterForm.pageNum = 1
  void fetchConversationList()
}

const fetchMessageList = async () => {
  if (!activeConversation.value) {
    return
  }

  messageLoading.value = true

  try {
    const res = await getAdminAiMessageListApi(activeConversation.value.conversationId, {
      userId: messageFilter.userId || undefined,
      pageNum: messageFilter.pageNum,
      pageSize: messageFilter.pageSize,
    })
    messageData.value = res.data.records || []
    messageTotal.value = res.data.total || 0
  } finally {
    messageLoading.value = false
  }
}

const handleOpenMessages = (row: AdminAiConversationVO) => {
  activeConversation.value = row
  messageFilter.userId = row.userId
  messageFilter.pageNum = 1
  messageVisible.value = true
  void fetchMessageList()
}

const handleMessageSearch = () => {
  messageFilter.pageNum = 1
  void fetchMessageList()
}

const handleMessagePageChange = (page: number) => {
  messageFilter.pageNum = page
  void fetchMessageList()
}

const handleMessageSizeChange = (size: number) => {
  messageFilter.pageSize = size
  messageFilter.pageNum = 1
  void fetchMessageList()
}

const fetchMcpServers = async () => {
  mcpServerLoading.value = true

  try {
    const res = await getAdminMcpServerListApi()
    mcpServers.value = res.data || []
  } finally {
    mcpServerLoading.value = false
  }
}

const fetchMcpTools = async () => {
  mcpToolLoading.value = true

  try {
    const res = await getAdminMcpToolListApi()
    mcpTools.value = res.data || []
  } finally {
    mcpToolLoading.value = false
  }
}

const handleQueryTask = async () => {
  if (!taskQueryId.value.trim()) {
    ElMessage.warning('请输入任务 ID')
    return
  }

  taskLoading.value = true
  taskResult.value = null

  try {
    const res = await getAdminAiTaskApi(taskQueryId.value.trim())
    taskResult.value = res.data
  } finally {
    taskLoading.value = false
  }
}

onMounted(() => {
  void fetchStatus()
  void fetchConversationList()
  void fetchMcpServers()
  void fetchMcpTools()
})
</script>

<template>
  <div class="ai-manage-page">
    <SectionCard v-loading="statusLoading" class="ai-manage-page__status">
      <div class="ai-status">
        <div class="ai-status__badges">
          <el-tag :type="status?.recordStoreAvailable ? 'success' : 'danger'" effect="light">
            会话记录存储：{{ status?.recordStoreAvailable ? '可用' : '不可用' }}
          </el-tag>
          <el-tag :type="status?.mcpEnabled ? 'success' : 'info'" effect="light">
            MCP：{{ status?.mcpEnabled ? `已启用（${status?.mcpServerCount} 个服务）` : '未启用' }}
          </el-tag>
          <el-tag :type="status?.ragEnabled ? 'success' : 'info'" effect="light">
            知识库检索：{{ status?.ragEnabled ? '已启用' : '未启用' }}
          </el-tag>
        </div>
        <div v-if="status && !status.recordStoreAvailable" class="ai-status__warning">
          会话记录数据库未配置或不可达，会话与消息列表将返回空数据。
        </div>
        <DetailMetricsGrid
          :items="[
            { label: '对话模型', value: status?.chatModel || '-' },
            { label: '向量模型', value: status?.embeddingModel || '-' },
          ]"
        />
      </div>
    </SectionCard>

    <SectionCard class="ai-manage-page__content">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="会话记录" name="conversations">
          <div class="filter-toolbar">
            <el-input
              v-model="filterForm.keyword"
              placeholder="按标题 / 会话 ID / 用户 ID 搜索"
              clearable
              size="large"
              @keyup.enter="handleSearch"
            />
            <el-button type="primary" size="large" @click="handleSearch">搜索</el-button>
            <el-button size="large" @click="handleReset">重置</el-button>
          </div>
          <el-table :data="conversationData" v-loading="loading" width="100%">
            <el-table-column prop="conversationId" label="会话 ID" min-width="200" show-overflow-tooltip />
            <el-table-column prop="userId" label="用户 ID" min-width="180" show-overflow-tooltip />
            <el-table-column label="标题" min-width="200" show-overflow-tooltip>
              <template #default="scope">{{ scope.row.title || '-' }}</template>
            </el-table-column>
            <el-table-column label="场景" width="120">
              <template #default="scope">{{ scope.row.scene || '-' }}</template>
            </el-table-column>
            <el-table-column prop="messageCount" label="消息数" width="90" />
            <el-table-column label="创建时间" min-width="170">
              <template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="更新时间" min-width="170">
              <template #default="scope">{{ formatDateTime(scope.row.updatedAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="scope">
                <el-button type="primary" link @click="handleOpenMessages(scope.row)">查看消息</el-button>
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

        <el-tab-pane label="MCP 服务" name="mcp">
          <h3 class="section-subtitle">服务列表</h3>
          <el-table :data="mcpServers" v-loading="mcpServerLoading" width="100%">
            <el-table-column prop="name" label="服务名称" min-width="150" />
            <el-table-column prop="url" label="服务地址" min-width="260" show-overflow-tooltip />
            <el-table-column label="状态" width="100">
              <template #default="scope">
                <el-tag :type="scope.row.enabled ? 'success' : 'info'" effect="light">
                  {{ scope.row.enabled ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="allowToolCount" label="允许工具数" width="120" />
            <el-table-column label="允许工具" min-width="240" show-overflow-tooltip>
              <template #default="scope">
                {{ scope.row.allowTools?.length ? scope.row.allowTools.join('、') : '全部' }}
              </template>
            </el-table-column>
          </el-table>

          <h3 class="section-subtitle">可用工具</h3>
          <el-table :data="mcpTools" v-loading="mcpToolLoading" width="100%">
            <el-table-column prop="server" label="所属服务" width="150" />
            <el-table-column prop="name" label="工具名称" min-width="180" />
            <el-table-column label="描述" min-width="300" show-overflow-tooltip>
              <template #default="scope">{{ scope.row.description || '-' }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!mcpToolLoading && !mcpTools.length" description="暂无可用工具（MCP 未启用或服务不可达）" />
        </el-tab-pane>

        <el-tab-pane label="异步任务" name="task">
          <div class="filter-toolbar">
            <el-input v-model="taskQueryId" placeholder="请输入任务 ID" clearable size="large" @keyup.enter="handleQueryTask" />
            <el-button type="primary" size="large" :loading="taskLoading" @click="handleQueryTask">查询</el-button>
          </div>
          <el-descriptions v-if="taskResult" :column="2" border>
            <el-descriptions-item v-for="(value, key) in taskResult" :key="String(key)" :label="String(key)">
              {{ value }}
            </el-descriptions-item>
          </el-descriptions>
          <el-empty v-else description="输入任务 ID 查询异步任务状态（任务数据仅保留 1 小时）" />
        </el-tab-pane>
      </el-tabs>
    </SectionCard>

    <el-dialog v-model="messageVisible" title="会话消息" width="880px">
      <div class="message-filter">
        <el-input v-model="messageFilter.userId" placeholder="用户 ID" clearable size="large" @keyup.enter="handleMessageSearch" />
        <el-button type="primary" size="large" @click="handleMessageSearch">搜索</el-button>
      </div>
      <el-table :data="messageData" v-loading="messageLoading" width="100%">
        <el-table-column prop="role" label="角色" width="100" />
        <el-table-column label="内容" min-width="480">
          <template #default="scope">
            <MarkdownContent :content="scope.row.content" />
          </template>
        </el-table-column>
        <el-table-column label="时间" min-width="170">
          <template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template>
        </el-table-column>
      </el-table>
      <div class="table-pagination">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :current-page="messageFilter.pageNum"
          :page-size="messageFilter.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="messageTotal"
          @current-change="handleMessagePageChange"
          @size-change="handleMessageSizeChange"
        />
      </div>
    </el-dialog>
  </div>
</template>
