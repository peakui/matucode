import type { AxiosResponse } from 'axios'
import { request } from './request'
import type {
  ApiResponse,
  CollectPostParams,
  CommentVO,
  CreateCommentRequest,
  CreatePostRequest,
  CreateReportRequest,
  ListDraftsParams,
  ListPostsParams,
  ListTagsParams,
  PageResponse,
  PostCategoryVO,
  PostDetailVO,
  PostListItemVO,
  PostTagVO,
  UpdatePostRequest,
} from './type/postTypings.ts'

const isSuccessResponse = (response: ApiResponse<unknown>) => (
  response.code === 0 || response.code === 200 || response.code === 20000 || response.success === true
)

const getErrorMessage = (response: ApiResponse<unknown>, fallback: string) => {
  return response.message || response.msg || fallback
}

async function createPost(data: CreatePostRequest): Promise<PostDetailVO> {
  const response: AxiosResponse<ApiResponse<PostDetailVO>> = await request.post('/posts', data)
  return response.data.data
}

async function updatePost(postId: string | number, data: UpdatePostRequest): Promise<PostDetailVO> {
  const response: AxiosResponse<ApiResponse<PostDetailVO>> = await request.put(`/posts/${String(postId)}`, data)
  return response.data.data
}

async function deletePost(postId: string | number): Promise<void> {
  await request.delete(`/posts/${String(postId)}`)
}

async function getPostDetail(postId: string | number): Promise<PostDetailVO> {
  const response: AxiosResponse<ApiResponse<PostDetailVO>> = await request.get(`/posts/${String(postId)}`)
  return response.data.data
}

async function listPosts(params: ListPostsParams = {}): Promise<PageResponse<PostListItemVO>> {
  const response: AxiosResponse<ApiResponse<PageResponse<PostListItemVO> | null>> = await request.get('/posts', { params })

  if (!isSuccessResponse(response.data)) {
    throw new Error(getErrorMessage(response.data, '动态流加载失败，请稍后重试'))
  }

  return response.data.data || {
    pageNum: Number(params.pageNum || 1),
    pageSize: Number(params.pageSize || 10),
    total: 0,
    totalPages: 0,
    records: [],
  }
}

async function likePost(postId: string | number): Promise<void> {
  await request.post(`/posts/${String(postId)}/like`)
}

async function unlikePost(postId: string | number): Promise<void> {
  await request.delete(`/posts/${String(postId)}/like`)
}

async function collectPost(postId: string | number, params: CollectPostParams = {}): Promise<void> {
  await request.post(`/posts/${String(postId)}/collect`, null, { params })
}

async function uncollectPost(postId: string | number): Promise<void> {
  await request.delete(`/posts/${String(postId)}/collect`)
}

async function sharePost(postId: string | number): Promise<void> {
  await request.post(`/posts/${String(postId)}/share`)
}

async function createComment(postId: string | number, data: CreateCommentRequest): Promise<CommentVO> {
  const response: AxiosResponse<ApiResponse<CommentVO>> = await request.post(`/posts/${String(postId)}/comments`, data)
  return response.data.data
}

async function listComments(postId: string | number): Promise<CommentVO[]> {
  const response: AxiosResponse<ApiResponse<CommentVO[]>> = await request.get(`/posts/${String(postId)}/comments`)
  return response.data.data
}

async function deleteComment(postId: string | number, commentId: string | number): Promise<void> {
  await request.delete(`/posts/${String(postId)}/comments/${String(commentId)}`)
}

async function likeComment(postId: string | number, commentId: string | number): Promise<void> {
  await request.post(`/posts/${String(postId)}/comments/${String(commentId)}/like`)
}

async function unlikeComment(postId: string | number, commentId: string | number): Promise<void> {
  await request.delete(`/posts/${String(postId)}/comments/${String(commentId)}/like`)
}

async function reportPost(postId: string | number, data: CreateReportRequest): Promise<void> {
  await request.post(`/posts/${String(postId)}/reports`, data)
}

async function listCategories(): Promise<PostCategoryVO[]> {
  const response: AxiosResponse<ApiResponse<PostCategoryVO[] | null>> = await request.get('/posts/categories')

  if (!isSuccessResponse(response.data)) {
    throw new Error(getErrorMessage(response.data, '文章分类加载失败，请稍后重试'))
  }

  return response.data.data || []
}

async function listDrafts(params: ListDraftsParams = {}): Promise<PageResponse<PostListItemVO>> {
  const response: AxiosResponse<ApiResponse<PageResponse<PostListItemVO> | null>> = await request.get('/posts/drafts', { params })
  return response.data.data || {
    pageNum: Number(params.pageNum || 1),
    pageSize: Number(params.pageSize || 10),
    total: 0,
    totalPages: 0,
    records: [],
  }
}

async function listCollectedPosts(params: ListDraftsParams = {}): Promise<PageResponse<PostListItemVO>> {
  const response: AxiosResponse<ApiResponse<PageResponse<PostListItemVO> | null>> = await request.get('/posts/collected', { params })
  return response.data.data || {
    pageNum: Number(params.pageNum || 1),
    pageSize: Number(params.pageSize || 10),
    total: 0,
    totalPages: 0,
    records: [],
  }
}

async function listLikedPosts(params: ListDraftsParams = {}): Promise<PageResponse<PostListItemVO>> {
  const response: AxiosResponse<ApiResponse<PageResponse<PostListItemVO> | null>> = await request.get('/posts/liked', { params })
  return response.data.data || {
    pageNum: Number(params.pageNum || 1),
    pageSize: Number(params.pageSize || 10),
    total: 0,
    totalPages: 0,
    records: [],
  }
}

async function listTags(params: ListTagsParams = {}): Promise<PostTagVO[]> {
  const response: AxiosResponse<ApiResponse<PostTagVO[] | null>> = await request.get('/posts/tags', { params })
  return response.data.data || []
}

export {
  collectPost,
  createComment,
  createPost,
  deleteComment,
  deletePost,
  getPostDetail,
  likeComment,
  likePost,
  listCategories,
  listCollectedPosts,
  listComments,
  listDrafts,
  listLikedPosts,
  listPosts,
  listTags,
  reportPost,
  sharePost,
  uncollectPost,
  unlikeComment,
  unlikePost,
  updatePost,
}
