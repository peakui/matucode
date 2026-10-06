import { reactive } from 'vue'
import { idOf } from './format.js'
export function createPager() {
  return reactive({
    items: [],
    page: 0,
    total: 0,
    done: false,
    loading: false,
    error: '',
    loaded: false,
    generation: 0,
  })
}
export async function loadPage(state, fetcher, reset = false) {
  if (!reset && (state.loading || state.done)) return
  const generation = reset ? ++state.generation : state.generation
  const page = reset ? 1 : state.page + 1
  if (reset) {
    state.items = []
    state.page = 0
    state.done = false
  }
  state.loading = true
  state.error = ''
  try {
    const data = await fetcher(page)
    if (state.generation !== generation) return
    const records = data?.records || data?.list || data?.rows || data?.items || []
    if (!Array.isArray(records)) throw new Error('列表数据格式异常')
    const seen = new Set(state.items.map(idOf))
    state.items.push(
      ...records.filter((item) => {
        const id = idOf(item)
        if (seen.has(id)) return false
        seen.add(id)
        return true
      })
    )
    state.page = page
    state.total = Number(data?.total || 0)
    state.done =
      records.length === 0 ||
      (data?.total != null && state.items.length >= Number(data.total)) ||
      (data?.totalPages != null && page >= Number(data.totalPages))
    state.loaded = true
  } catch (error) {
    if (state.generation === generation) state.error = error.message
  } finally {
    if (state.generation === generation) state.loading = false
  }
}
