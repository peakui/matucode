import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'
import { getCheckStatistics, listCheckRecords, listPublicCheckGroups } from '../../api/check'
import type { CheckGroupVO, CheckRecordListItemVO, CheckStatisticsVO } from '../../api/type/checkTypings'

const PAGE_SIZE = 10

type CheckListState = {
  records: CheckRecordListItemVO[]
  statistics?: CheckStatisticsVO
  groups: CheckGroupVO[]
  loading: boolean
  error: string
}

const initialState: CheckListState = {
  records: [],
  statistics: undefined,
  groups: [],
  loading: true,
  error: '',
}

export const fetchCheckListData = createAsyncThunk('checkList/fetchData', async () => {
  const now = new Date()
  const [recordData, statisticsResult, groupResult] = await Promise.allSettled([
    listCheckRecords({ status: 1, pageNum: 1, pageSize: PAGE_SIZE }),
    getCheckStatistics({ year: now.getFullYear(), month: now.getMonth() + 1 }),
    listPublicCheckGroups(),
  ])

  return {
    records: recordData.status === 'fulfilled' ? (recordData.value.records ?? []) : [],
    recordError: recordData.status === 'rejected',
    statistics: statisticsResult.status === 'fulfilled' ? statisticsResult.value : {},
    groups: groupResult.status === 'fulfilled' ? (groupResult.value || []) : [],
  }
})

const checkListSlice = createSlice({
  name: 'checkList',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchCheckListData.pending, (state) => {
        state.loading = true
        state.error = ''
      })
      .addCase(fetchCheckListData.fulfilled, (state, action) => {
        state.records = action.payload.records
        state.statistics = action.payload.statistics
        state.groups = action.payload.groups
        state.error = action.payload.recordError ? '打卡列表加载失败，请稍后重试' : ''
        state.loading = false
      })
      .addCase(fetchCheckListData.rejected, (state) => {
        state.records = []
        state.statistics = {}
        state.groups = []
        state.error = '打卡列表加载失败，请稍后重试'
        state.loading = false
      })
  },
})

export default checkListSlice.reducer
