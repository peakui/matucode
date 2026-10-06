import { createAsyncThunk, createSlice, type PayloadAction } from '@reduxjs/toolkit'
import { listQaCategories, listQuestions } from '../../api/qa'
import type { QaCategoryVO, QaQuestionListItemVO } from '../../api/type/qaTypings'

const PAGE_SIZE = 10

type QaListState = {
  questions: QaQuestionListItemVO[]
  categories: QaCategoryVO[]
  categoryId?: string | number
  sortBy: string
  loading: boolean
}

const initialState: QaListState = {
  questions: [],
  categories: [],
  categoryId: undefined,
  sortBy: 'latest',
  loading: true,
}

export const fetchQaCategories = createAsyncThunk('qaList/fetchCategories', async () => {
  return await listQaCategories()
})

export const fetchQaQuestions = createAsyncThunk(
  'qaList/fetchQuestions',
  async (_, { getState }) => {
    const state = getState() as { qaList: QaListState }
    const { categoryId, sortBy } = state.qaList
    const data = await listQuestions({ categoryId, pageNum: 1, pageSize: PAGE_SIZE, sortBy })
    return data.records ?? []
  },
)

const qaListSlice = createSlice({
  name: 'qaList',
  initialState,
  reducers: {
    setQaCategoryId(state, action: PayloadAction<string | number | undefined>) {
      state.categoryId = action.payload
    },
    setQaSortBy(state, action: PayloadAction<string>) {
      state.sortBy = action.payload
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchQaCategories.fulfilled, (state, action) => {
        state.categories = action.payload || []
      })
      .addCase(fetchQaCategories.rejected, (state) => {
        state.categories = []
      })
      .addCase(fetchQaQuestions.pending, (state) => {
        state.loading = true
      })
      .addCase(fetchQaQuestions.fulfilled, (state, action) => {
        state.questions = action.payload
        state.loading = false
      })
      .addCase(fetchQaQuestions.rejected, (state) => {
        state.questions = []
        state.loading = false
      })
  },
})

export const { setQaCategoryId, setQaSortBy } = qaListSlice.actions
export default qaListSlice.reducer
