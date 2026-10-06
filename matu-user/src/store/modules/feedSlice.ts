import { createAsyncThunk, createSlice, type PayloadAction } from '@reduxjs/toolkit'
import { listCategories, listPosts } from '../../api/post'
import type { PostCategoryVO, PostListItemVO } from '../../api/type/postTypings'

const PAGE_SIZE = 10

type FeedState = {
  posts: PostListItemVO[]
  categories: PostCategoryVO[]
  loading: boolean
  loadingMore: boolean
  error: string
  pageNum: number
  hasMore: boolean
  keywordInput: string
  keyword: string
  categoryId?: number
}

const initialState: FeedState = {
  posts: [],
  categories: [],
  loading: true,
  loadingMore: false,
  error: '',
  pageNum: 1,
  hasMore: true,
  keywordInput: '',
  keyword: '',
  categoryId: undefined,
}

export const fetchFeedCategories = createAsyncThunk('feed/fetchCategories', async () => {
  return await listCategories()
})

export const fetchFeedPosts = createAsyncThunk(
  'feed/fetchPosts',
  async ({ pageNum, append }: { pageNum: number, append: boolean }, { getState }) => {
    const state = getState() as { feed: FeedState }
    const { keyword, categoryId } = state.feed
    const data = await listPosts({
      status: 1,
      pageNum,
      pageSize: PAGE_SIZE,
      sortBy: 'latest',
      keyword: keyword || undefined,
      categoryId,
    })

    return {
      append,
      pageNum,
      records: data.records ?? [],
      totalPages: data.totalPages ?? 0,
    }
  },
)

const feedSlice = createSlice({
  name: 'feed',
  initialState,
  reducers: {
    setKeywordInput(state, action: PayloadAction<string>) {
      state.keywordInput = action.payload
    },
    applyKeyword(state) {
      state.keyword = state.keywordInput.trim()
    },
    setCategoryId(state, action: PayloadAction<number | undefined>) {
      state.categoryId = action.payload
    },
    resetFilters(state) {
      state.keywordInput = ''
      state.keyword = ''
      state.categoryId = undefined
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchFeedCategories.fulfilled, (state, action) => {
        state.categories = action.payload || []
      })
      .addCase(fetchFeedCategories.rejected, (state) => {
        state.categories = []
      })
      .addCase(fetchFeedPosts.pending, (state, action) => {
        const append = action.meta.arg.append
        if (append) {
          state.loadingMore = true
        } else {
          state.loading = true
          state.error = ''
        }
      })
      .addCase(fetchFeedPosts.fulfilled, (state, action) => {
        const { append, pageNum, records, totalPages } = action.payload
        state.posts = append ? [...state.posts, ...records] : records
        state.pageNum = pageNum
        state.hasMore = pageNum < totalPages
        state.loading = false
        state.loadingMore = false
      })
      .addCase(fetchFeedPosts.rejected, (state, action) => {
        const append = action.meta.arg.append
        if (!append) {
          state.posts = []
          state.error = '动态流加载失败，请稍后重试'
          state.loading = false
        }
        state.loadingMore = false
      })
  },
})

export const { applyKeyword, resetFilters, setCategoryId, setKeywordInput } = feedSlice.actions
export default feedSlice.reducer
