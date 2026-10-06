import { request } from '@/utils/request'
import type {
  CommentVO,
  CreatePostRequest,
  PageResponse,
  PostCategoryVO,
  PostDetailVO,
  PostListItemVO,
  PostListParams,
  UpdatePostRequest,
} from '@/api/types'

export const getPostListApi = (params: PostListParams) => {
  return request<PageResponse<PostListItemVO>>({
    url: '/posts',
    method: 'get',
    params,
  })
}

export const createPostApi = (data: CreatePostRequest) => {
  return request<PostDetailVO>({
    url: '/posts',
    method: 'post',
    data,
  })
}

export const updatePostApi = (postId: string, data: UpdatePostRequest) => {
  return request<PostDetailVO>({
    url: `/posts/${postId}`,
    method: 'put',
    data,
  })
}

export const getPostDetailApi = (postId: string) => {
  return request<PostDetailVO>({
    url: `/posts/${postId}`,
    method: 'get',
  })
}

export const getPostCategoriesApi = () => {
  return request<PostCategoryVO[]>({
    url: '/posts/categories',
    method: 'get',
  })
}

export const getPostCommentsApi = (postId: string) => {
  return request<CommentVO[]>({
    url: `/posts/${postId}/comments`,
    method: 'get',
  })
}

export const deletePostCommentApi = (postId: string, commentId: string) => {
  return request<null>({
    url: `/posts/${postId}/comments/${commentId}`,
    method: 'delete',
  })
}

export const deletePostApi = (postId: string) => {
  return request<null>({
    url: `/posts/${postId}`,
    method: 'delete',
  })
}
