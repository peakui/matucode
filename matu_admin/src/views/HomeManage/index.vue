<script setup lang="ts">
import type { FormRules, TagProps } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SectionCard from '@/components/SectionCard/index.vue'
import DetailMetricsGrid from '@/components/DetailMetricsGrid/index.vue'
import PostEditorForm from '@/components/PostEditorForm/index.vue'
import {
  createPostApi,
  deletePostApi,
  deletePostCommentApi,
  getPostCategoriesApi,
  getPostCommentsApi,
  getPostDetailApi,
  getPostListApi,
  updatePostApi,
} from '@/api'
import type {
  CommentVO,
  CreatePostRequest,
  PostCategoryVO,
  PostDetailVO,
  PostListItemVO,
} from '@/api/types'
import './index.scss'

const loading = ref(false)
const detailLoading = ref(false)
const commentLoading = ref(false)
const submitLoading = ref(false)
const detailVisible = ref(false)
const commentVisible = ref(false)
const formVisible = ref(false)
const isEditMode = ref(false)
const total = ref(0)
const categoryOptions = ref<PostCategoryVO[]>([])
const tableData = ref<PostListItemVO[]>([])
const currentPost = ref<PostDetailVO | null>(null)
const currentComments = ref<CommentVO[]>([])
const flattenedComments = ref<Array<CommentVO & { level: number }>>([])
const postEditorFormRef = ref<InstanceType<typeof PostEditorForm>>()

const filterForm = reactive({
  keyword: '',
  categoryId: undefined as string | undefined,
  status: -1 as number,
  sortBy: 'latest',
  pageNum: 1,
  pageSize: 10,
})

const postForm = reactive<CreatePostRequest>({
  categoryId: undefined,
  title: '',
  summary: '',
  content: '',
  contentType: 1,
  status: 1,
  isTop: 0,
  isEssence: 0,
  isLock: 0,
  lockReason: '',
})

const postFormRules: FormRules<CreatePostRequest> = {
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  title: [{ required: true, message: '请输入帖子标题', trigger: 'blur' }],
  summary: [{ required: true, message: '请输入帖子摘要', trigger: 'blur' }],
  content: [{ required: true, message: '请输入帖子内容', trigger: 'blur' }],
}

const statusOptions: Array<{ label: string; value: number }> = [
  { label: '全部', value: -1 },
  { label: '草稿', value: 0 },
  { label: '已发布', value: 1 },
  { label: '已下架', value: 2 },
]

const editStatusOptions = [
  { label: '草稿', value: 0 },
  { label: '已发布', value: 1 },
  { label: '已下架', value: 2 },
]

const sortOptions = [
  { label: '最新发布', value: 'latest' },
  { label: '最多浏览', value: 'most_viewed' },
  { label: '最多点赞', value: 'most_liked' },
  { label: '最多评论', value: 'most_commented' },
]

const formatDateTime = (value?: string) => (!value ? '-' : value.replace('T', ' '))

const getStatusText = (status: number) =>
  status === 0 ? '草稿' : status === 1 ? '已发布' : status === 2 ? '已下架' : '未知'

const getStatusType = (status: number): TagProps['type'] =>
  status === 1 ? 'success' : status === 2 ? 'warning' : 'info'

const getCategoryName = (categoryId: string) =>
  categoryOptions.value.find((item) => item.id === categoryId)?.categoryName || '-'

const getCommentStatusText = (status: number) =>
  status === 0 ? '待审核' : status === 1 ? '正常' : status === 2 ? '已屏蔽' : '未知'

const getCommentStatusType = (status: number): TagProps['type'] =>
  status === 1 ? 'success' : status === 2 ? 'danger' : 'warning'

const getCommentLevelText = (comment: CommentVO) => (comment.parentId ? '回复评论' : '主评论')

const buildCommentRows = (
  comments: CommentVO[],
  level = 0,
): Array<CommentVO & { level: number }> => {
  return comments.flatMap((comment) => [
    { ...comment, level },
    ...buildCommentRows(comment.children || [], level + 1),
  ])
}

const fetchCategories = async () => {
  const res = await getPostCategoriesApi()
  categoryOptions.value = res.data
}

const fetchPostList = async () => {
  loading.value = true

  try {
    const res = await getPostListApi({
      keyword: filterForm.keyword || undefined,
      categoryId: filterForm.categoryId,
      status: filterForm.status,
      pageNum: filterForm.pageNum,
      pageSize: filterForm.pageSize,
      sortBy: filterForm.sortBy,
    })

    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const fetchPostDetail = async (postId: string) => {
  detailLoading.value = true

  try {
    const res = await getPostDetailApi(postId)
    currentPost.value = res.data
    return res.data
  } finally {
    detailLoading.value = false
  }
}

const fetchPostComments = async (postId: string) => {
  commentLoading.value = true

  try {
    const res = await getPostCommentsApi(postId)
    currentComments.value = res.data || []
    flattenedComments.value = buildCommentRows(currentComments.value)
  } finally {
    commentLoading.value = false
  }
}

const resetPostForm = () => {
  postForm.categoryId = undefined
  postForm.title = ''
  postForm.summary = ''
  postForm.content = ''
  postForm.contentType = 1
  postForm.status = 1
  postForm.isTop = 0
  postForm.isEssence = 0
  postForm.isLock = 0
  postForm.lockReason = ''
}

const handleSearch = () => {
  filterForm.pageNum = 1
  void fetchPostList()
}

const handleReset = () => {
  filterForm.keyword = ''
  filterForm.categoryId = undefined
  filterForm.status = -1
  filterForm.sortBy = 'latest'
  filterForm.pageNum = 1
  void fetchPostList()
}

const handleCreate = () => {
  isEditMode.value = false
  currentPost.value = null
  resetPostForm()
  formVisible.value = true
}

const handleEdit = async (row: PostListItemVO) => {
  isEditMode.value = true
  formVisible.value = true

  const detail = await fetchPostDetail(row.id)
  if (!detail) return

  currentPost.value = detail
  postForm.categoryId = detail.categoryId
  postForm.title = detail.title
  postForm.summary = detail.summary
  postForm.content = detail.content || ''
  postForm.contentType = detail.contentType ?? 1
  postForm.status = detail.status
  postForm.isTop = detail.isTop
  postForm.isEssence = detail.isEssence
  postForm.isLock = detail.isLock
  postForm.lockReason = detail.lockReason || ''
}

const handleView = async (row: PostListItemVO) => {
  detailVisible.value = true
  await fetchPostDetail(row.id)
}

const handleCommentManage = async (row: PostListItemVO) => {
  currentPost.value = { ...row } as PostDetailVO
  commentVisible.value = true
  await fetchPostComments(row.id)
}

const handleSubmit = async () => {
  await postEditorFormRef.value?.formRef?.validate()
  submitLoading.value = true

  try {
    const payload: CreatePostRequest = {
      ...postForm,
      lockReason: postForm.isLock ? postForm.lockReason : '',
    }

    if (isEditMode.value && currentPost.value) {
      await updatePostApi(currentPost.value.id, payload)
      ElMessage.success('帖子更新成功')
    } else {
      await createPostApi(payload)
      ElMessage.success('帖子创建成功')
    }

    formVisible.value = false
    resetPostForm()
    void fetchPostList()
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = async (row: PostListItemVO) => {
  await ElMessageBox.confirm(`确认删除帖子《${row.title}》吗？`, '删除提示', {
    type: 'warning',
  })
  await deletePostApi(row.id)
  ElMessage.success('删除成功')
  void fetchPostList()
}

const handleDeleteComment = async (comment: CommentVO) => {
  if (!currentPost.value) return

  await ElMessageBox.confirm('确认删除该评论吗？', '删除评论', {
    type: 'warning',
  })
  await deletePostCommentApi(currentPost.value.id, comment.id)
  ElMessage.success('评论删除成功')
  await fetchPostComments(currentPost.value.id)
}

const handlePageChange = (page: number) => {
  filterForm.pageNum = page
  void fetchPostList()
}

const handleSizeChange = (size: number) => {
  filterForm.pageSize = size
  filterForm.pageNum = 1
  void fetchPostList()
}

onMounted(async () => {
  await Promise.all([fetchCategories(), fetchPostList()])
})
</script>

<template>
  <div class="post-manage-page">
    <SectionCard class="post-manage-page__filter">
      <div class="filter-action-row">
        <el-button type="primary" @click="handleCreate">新建帖子</el-button>
      </div>
      <div class="filter-toolbar">
        <el-input
          v-model="filterForm.keyword"
          placeholder="请输入帖子标题或摘要关键词"
          clearable
          size="large"
        />
        <el-button type="primary" size="large" @click="handleSearch">搜索</el-button>
        <el-button size="large" @click="handleReset">重置</el-button>
      </div>
      <div class="filter-grid">
        <el-select v-model="filterForm.categoryId" placeholder="选择分类" clearable size="large">
          <el-option
            v-for="item in categoryOptions"
            :key="item.id"
            :label="item.categoryName"
            :value="item.id"
          />
        </el-select>
        <el-select v-model="filterForm.status" placeholder="选择状态" size="large">
          <el-option
            v-for="item in statusOptions"
            :key="item.label"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-select v-model="filterForm.sortBy" placeholder="排序方式" size="large">
          <el-option
            v-for="item in sortOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </div>
    </SectionCard>

    <SectionCard class="post-manage-page__table">
      <el-table :data="tableData" v-loading="loading" width="100%">
        <el-table-column prop="title" label="帖子标题" min-width="220" show-overflow-tooltip />
        <el-table-column label="分类" width="120">
          <template #default="scope">{{ getCategoryName(scope.row.categoryId) }}</template>
        </el-table-column>
        <el-table-column prop="userId" label="用户ID" width="110" />
        <el-table-column prop="summary" label="帖子摘要" min-width="260" show-overflow-tooltip />
        <el-table-column prop="viewCount" label="浏览" width="90" />
        <el-table-column prop="likeCount" label="点赞" width="90" />
        <el-table-column prop="commentCount" label="评论" width="90" />
        <el-table-column prop="collectCount" label="收藏" width="90" />
        <el-table-column label="标记" min-width="160">
          <template #default="scope">
            <div class="post-flags">
              <el-tag v-if="scope.row.isTop" type="danger" effect="light">置顶</el-tag>
              <el-tag v-if="scope.row.isEssence" type="success" effect="light">精华</el-tag>
              <el-tag v-if="scope.row.isLock" type="warning" effect="light">锁帖</el-tag>
              <span v-if="!scope.row.isTop && !scope.row.isEssence && !scope.row.isLock">-</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)" effect="light">{{
              getStatusText(scope.row.status)
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" min-width="180">
          <template #default="scope">{{ formatDateTime(scope.row.publishedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="scope">
            <el-button type="primary" link @click="handleView(scope.row)">查看</el-button>
            <el-button type="success" link @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="warning" link @click="handleCommentManage(scope.row)"
              >评论管理</el-button
            >
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

    <el-dialog v-model="formVisible" :title="isEditMode ? '编辑帖子' : '新建帖子'" width="780px">
      <PostEditorForm
        ref="postEditorFormRef"
        :form="postForm"
        :rules="postFormRules"
        :categories="categoryOptions"
        :status-options="editStatusOptions"
      />
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="formVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">{{
            isEditMode ? '保存修改' : '确认发布'
          }}</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="帖子详情" width="760px">
      <div v-loading="detailLoading" class="post-detail">
        <template v-if="currentPost">
          <div class="post-detail__item">
            <span>标题</span><strong>{{ currentPost.title }}</strong>
          </div>
          <div class="post-detail__item">
            <span>摘要</span>
            <p>{{ currentPost.summary || '-' }}</p>
          </div>
          <div class="post-detail__item">
            <span>帖子内容</span>
            <p>{{ currentPost.content || '-' }}</p>
          </div>
          <DetailMetricsGrid
            :items="[
              { label: '浏览', value: currentPost.viewCount },
              { label: '点赞', value: currentPost.likeCount },
              { label: '评论', value: currentPost.commentCount },
              { label: '收藏', value: currentPost.collectCount },
            ]"
          />
          <DetailMetricsGrid
            :items="[
              { label: '状态', value: getStatusText(currentPost.status) },
              { label: '发布时间', value: formatDateTime(currentPost.publishedAt) },
            ]"
          />
          <DetailMetricsGrid
            :items="[
              { label: '置顶', value: currentPost.isTop ? '是' : '否' },
              { label: '精华', value: currentPost.isEssence ? '是' : '否' },
            ]"
          />
          <div class="post-detail__item">
            <span>锁帖原因</span>
            <p>{{ currentPost.lockReason || '-' }}</p>
          </div>
        </template>
      </div>
    </el-dialog>

    <el-dialog v-model="commentVisible" title="评论管理" width="860px">
      <div v-loading="commentLoading">
        <div v-if="currentPost" class="comment-header">
          <strong>{{ currentPost.title }}</strong
          ><span>共 {{ flattenedComments.length }} 条评论</span>
        </div>
        <el-table :data="flattenedComments" width="100%">
          <el-table-column label="评论层级" width="120">
            <template #default="scope">
              <span :style="{ paddingLeft: `${scope.row.level * 16}px` }">{{
                getCommentLevelText(scope.row)
              }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="scope">
              <el-tag :type="getCommentStatusType(scope.row.status)" effect="light">{{
                getCommentStatusText(scope.row.status)
              }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="parentId" label="父评论ID" width="120">
            <template #default="scope">{{ scope.row.parentId || '-' }}</template>
          </el-table-column>
          <el-table-column prop="content" label="评论内容" min-width="260" show-overflow-tooltip />
          <el-table-column prop="userId" label="用户ID" width="100" />
          <el-table-column prop="ipAddress" label="IP" width="140" show-overflow-tooltip />
          <el-table-column prop="likeCount" label="点赞" width="90" />
          <el-table-column prop="replyCount" label="回复" width="90" />
          <el-table-column label="创建时间" min-width="170">
            <template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="更新时间" min-width="170">
            <template #default="scope">{{ formatDateTime(scope.row.updatedAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="scope"
              ><el-button type="danger" link @click="handleDeleteComment(scope.row)"
                >删除评论</el-button
              ></template
            >
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>
