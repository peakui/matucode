<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SectionCard from '@/components/SectionCard/index.vue'
import DetailMetricsGrid from '@/components/DetailMetricsGrid/index.vue'
import {
  deleteAdminMessageApi,
  getAdminConversationDetailApi,
  getAdminConversationListApi,
  getAdminMessageListApi,
  recallAdminMessageApi,
} from '@/api'
import type { AdminConversationVO, AdminMessageVO } from '@/api/types'
import './index.scss'

const loading = ref(false)
const detailLoading = ref(false)
const messageLoading = ref(false)
const total = ref(0)
const tableData = ref<AdminConversationVO[]>([])

const detailVisible = ref(false)
const currentConversation = ref<AdminConversationVO | null>(null)

const messageVisible = ref(false)
const messageTotal = ref(0)
const messageData = ref<AdminMessageVO[]>([])
const activeConversation = ref<AdminConversationVO | null>(null)

const conversationTypeOptions = [
  { label: '单聊', value: 1, tagType: 'success' as const },
  { label: '群聊', value: 2, tagType: 'warning' as const },
]

const messageTypeOptions = [{ label: '文本', value: 1 }]

const filterForm = reactive({
  keyword: '',
  conversationType: undefined as number | undefined,
  pageNum: 1,
  pageSize: 10,
})

const messageFilter = reactive({
  senderId: '',
  messageType: undefined as number | undefined,
  isRecall: undefined as number | undefined,
  isDeleted: undefined as number | undefined,
  keyword: '',
  timeRange: [] as string[],
  pageNum: 1,
  pageSize: 10,
})

const formatDateTime = (value?: string | null) => (!value ? '-' : value.replace('T', ' '))

const getConversationTypeMeta = (type: number) =>
  conversationTypeOptions.find((item) => item.value === type) ?? { label: `类型 ${type}`, value: type, tagType: 'info' as const }

const getMessageTypeLabel = (type: number) =>
  messageTypeOptions.find((item) => item.value === type)?.label ?? '文件/媒体'

const fetchConversationList = async () => {
  loading.value = true

  try {
    const res = await getAdminConversationListApi({
      keyword: filterForm.keyword || undefined,
      conversationType: filterForm.conversationType,
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
  void fetchConversationList()
}

const handleReset = () => {
  filterForm.keyword = ''
  filterForm.conversationType = undefined
  filterForm.pageNum = 1
  void fetchConversationList()
}

const handleViewDetail = async (row: AdminConversationVO) => {
  detailVisible.value = true
  detailLoading.value = true
  currentConversation.value = null

  try {
    const res = await getAdminConversationDetailApi(row.id)
    currentConversation.value = res.data
  } finally {
    detailLoading.value = false
  }
}

const fetchMessageList = async () => {
  if (!activeConversation.value) {
    return
  }

  messageLoading.value = true

  try {
    const res = await getAdminMessageListApi({
      conversationId: activeConversation.value.id,
      senderId: messageFilter.senderId || undefined,
      messageType: messageFilter.messageType,
      isRecall: messageFilter.isRecall,
      isDeleted: messageFilter.isDeleted,
      keyword: messageFilter.keyword || undefined,
      startTime: messageFilter.timeRange?.[0],
      endTime: messageFilter.timeRange?.[1],
      pageNum: messageFilter.pageNum,
      pageSize: messageFilter.pageSize,
    })
    messageData.value = res.data.records || []
    messageTotal.value = res.data.total || 0
  } finally {
    messageLoading.value = false
  }
}

const handleOpenMessages = (row: AdminConversationVO) => {
  activeConversation.value = row
  messageFilter.senderId = ''
  messageFilter.messageType = undefined
  messageFilter.isRecall = undefined
  messageFilter.isDeleted = undefined
  messageFilter.keyword = ''
  messageFilter.timeRange = []
  messageFilter.pageNum = 1
  messageVisible.value = true
  void fetchMessageList()
}

const handleMessageSearch = () => {
  messageFilter.pageNum = 1
  void fetchMessageList()
}

const handleMessageReset = () => {
  messageFilter.senderId = ''
  messageFilter.messageType = undefined
  messageFilter.isRecall = undefined
  messageFilter.isDeleted = undefined
  messageFilter.keyword = ''
  messageFilter.timeRange = []
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

const handleRecallMessage = async (row: AdminMessageVO) => {
  await ElMessageBox.confirm('确认撤回该消息吗？撤回后会话成员将收到撤回通知。', '撤回消息', { type: 'warning' })
  await recallAdminMessageApi(row.id)
  ElMessage.success('撤回成功')
  void fetchMessageList()
}

const handleDeleteMessage = async (row: AdminMessageVO) => {
  await ElMessageBox.confirm('确认删除该消息吗？该操作不可撤销。', '删除消息', { type: 'warning' })
  await deleteAdminMessageApi(row.id)
  ElMessage.success('删除成功')
  void fetchMessageList()
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

onMounted(() => {
  void fetchConversationList()
})
</script>

<template>
  <div class="message-manage-page">
    <SectionCard class="message-manage-page__filter">
      <div class="filter-toolbar">
        <el-input v-model="filterForm.keyword" placeholder="请输入群聊名称关键词" clearable size="large" />
        <el-button type="primary" size="large" @click="handleSearch">搜索</el-button>
        <el-button size="large" @click="handleReset">重置</el-button>
      </div>
      <div class="filter-grid">
        <el-select v-model="filterForm.conversationType" placeholder="会话类型" clearable size="large">
          <el-option v-for="item in conversationTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </div>
    </SectionCard>

    <SectionCard class="message-manage-page__table">
      <el-table :data="tableData" v-loading="loading" width="100%">
        <el-table-column prop="id" label="会话 ID" width="200" show-overflow-tooltip />
        <el-table-column label="类型" width="90">
          <template #default="scope">
            <el-tag :type="getConversationTypeMeta(scope.row.conversationType).tagType" effect="light">
              {{ getConversationTypeMeta(scope.row.conversationType).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="会话名称" min-width="180" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.conversationName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="creatorId" label="创建者 ID" width="200" show-overflow-tooltip />
        <el-table-column prop="memberCount" label="成员数" width="90" />
        <el-table-column label="最新消息" min-width="220" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.lastMessagePreview || '-' }}</template>
        </el-table-column>
        <el-table-column label="最新消息时间" min-width="170">
          <template #default="scope">{{ formatDateTime(scope.row.lastMessageTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="scope">
            <el-tag :type="scope.row.isDeleted === 1 ? 'danger' : 'success'" effect="light">
              {{ scope.row.isDeleted === 1 ? '已删除' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button type="primary" link @click="handleViewDetail(scope.row)">会话详情</el-button>
            <el-button type="success" link @click="handleOpenMessages(scope.row)">消息记录</el-button>
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

    <el-dialog v-model="detailVisible" title="会话详情" width="820px">
      <div v-loading="detailLoading" class="conversation-detail">
        <template v-if="currentConversation">
          <DetailMetricsGrid
            :items="[
              { label: '会话 ID', value: currentConversation.id },
              { label: '会话类型', value: getConversationTypeMeta(currentConversation.conversationType).label },
              { label: '会话名称', value: currentConversation.conversationName || '-' },
              { label: '创建者 ID', value: currentConversation.creatorId || '-' },
            ]"
          />
          <DetailMetricsGrid
            :items="[
              { label: '成员数', value: currentConversation.memberCount },
              { label: '状态', value: currentConversation.isDeleted === 1 ? '已删除' : '正常' },
              { label: '创建时间', value: formatDateTime(currentConversation.createdAt) },
              { label: '更新时间', value: formatDateTime(currentConversation.updatedAt) },
            ]"
          />
          <div class="conversation-detail__section-title">群成员</div>
          <el-table :data="currentConversation.members || []" size="small" width="100%">
            <el-table-column prop="userId" label="用户 ID" min-width="200" show-overflow-tooltip />
            <el-table-column prop="role" label="角色" width="90" />
            <el-table-column prop="unreadCount" label="未读数" width="90" />
            <el-table-column label="免打扰" width="90">
              <template #default="scope">{{ scope.row.isMuted === 1 ? '是' : '否' }}</template>
            </el-table-column>
            <el-table-column label="置顶" width="80">
              <template #default="scope">{{ scope.row.isTop === 1 ? '是' : '否' }}</template>
            </el-table-column>
            <el-table-column label="加入时间" min-width="170">
              <template #default="scope">{{ formatDateTime(scope.row.joinedAt) }}</template>
            </el-table-column>
          </el-table>
        </template>
      </div>
    </el-dialog>

    <el-dialog v-model="messageVisible" title="消息记录" width="960px">
      <div class="message-filter">
        <el-input v-model="messageFilter.keyword" placeholder="消息内容关键词" clearable size="large" @keyup.enter="handleMessageSearch" />
        <el-input v-model="messageFilter.senderId" placeholder="发送者 ID" clearable size="large" />
        <el-select v-model="messageFilter.messageType" placeholder="消息类型" clearable size="large">
          <el-option v-for="item in messageTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="messageFilter.isRecall" placeholder="撤回状态" clearable size="large">
          <el-option label="未撤回" :value="0" />
          <el-option label="已撤回" :value="1" />
        </el-select>
        <el-select v-model="messageFilter.isDeleted" placeholder="删除状态" clearable size="large">
          <el-option label="未删除" :value="0" />
          <el-option label="已删除" :value="1" />
        </el-select>
        <el-date-picker
          v-model="messageFilter.timeRange"
          type="datetimerange"
          value-format="YYYY-MM-DDTHH:mm:ss"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          size="large"
        />
        <div class="message-filter__actions">
          <el-button type="primary" size="large" @click="handleMessageSearch">搜索</el-button>
          <el-button size="large" @click="handleMessageReset">重置</el-button>
        </div>
      </div>

      <el-table :data="messageData" v-loading="messageLoading" width="100%">
        <el-table-column prop="id" label="消息 ID" width="190" show-overflow-tooltip />
        <el-table-column prop="senderId" label="发送者 ID" width="190" show-overflow-tooltip />
        <el-table-column label="类型" width="90">
          <template #default="scope">{{ getMessageTypeLabel(scope.row.messageType) }}</template>
        </el-table-column>
        <el-table-column label="内容" min-width="220" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.content || scope.row.fileName || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="150">
          <template #default="scope">
            <el-tag v-if="scope.row.isRecall === 1" type="warning" effect="light">已撤回</el-tag>
            <el-tag v-else-if="scope.row.isDeleted === 1" type="danger" effect="light">已删除</el-tag>
            <el-tag v-else type="success" effect="light">正常</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发送时间" min-width="170">
          <template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="scope">
            <el-button
              type="warning"
              link
              :disabled="scope.row.isRecall === 1"
              @click="handleRecallMessage(scope.row)"
            >撤回</el-button>
            <el-button
              type="danger"
              link
              :disabled="scope.row.isDeleted === 1"
              @click="handleDeleteMessage(scope.row)"
            >删除</el-button>
          </template>
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
