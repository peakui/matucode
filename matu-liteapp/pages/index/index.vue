<template>
  <view class="page">
    <view class="page-head row between"
      ><view
        ><view class="eyebrow">MATU COMMUNITY</view><view class="title">每一步，都算数。</view
        ><view class="subtitle">分享所学，与同行者一起成长</view></view
      ><view class="brand-mark">途</view></view
    >
    <view class="tabs"
      ><view
        v-for="t in tabs"
        :key="t.key"
        class="tab"
        :class="{ active: active === t.key }"
        @tap="switchFeed(t.key)"
        >{{ t.label }}</view
      ></view
    >
    <template v-if="active !== 'check'"
      ><view class="search"
        ><view class="search-icon" /><input
          v-model="filters[active].keyword"
          placeholder="搜索你感兴趣的内容"
          confirm-type="search"
          @confirm="refresh"
        /><text class="link" @tap="refresh">搜索</text></view
      ><scroll-view class="chips" scroll-x :show-scrollbar="false"
        ><text class="chip" :class="{ active: !filters[active].categoryId }" @tap="chooseCategory('')"
          >全部</text
        ><text
          v-for="c in categories[active]"
          :key="c.id"
          class="chip"
          :class="{ active: filters[active].categoryId === c.id }"
          @tap="chooseCategory(c.id)"
          >{{ c.categoryName }}</text
        ></scroll-view
      ><view v-if="categoryErrors[active]" class="error-inline" @tap="loadCategories(active)"
        >分类加载失败，点此重试</view
      ></template
    >
    <view v-else class="row between check-filter"
      ><view class="muted">记录日常积累，也发现别人的坚持</view
      ><picker mode="date" fields="month" @change="chooseMonth"
        ><text class="link">{{ filters.check.monthText || '按月查看' }}</text></picker
      ><text v-if="filters.check.monthText" class="muted" @tap="clearMonth">清除</text></view
    >
    <FeedCard v-for="item in current.items" :key="idOf(item)" :item="item" :type="active" />
    <StateView
      :loading="current.loading"
      :error="current.error"
      :count="current.items.length"
      :done="current.done"
      @retry="load(current.page === 0)"
    />
  </view>
</template>
<script setup>
import { computed, nextTick, reactive, ref } from 'vue'
import { onLoad, onPullDownRefresh, onReachBottom, onPageScroll } from '@dcloudio/uni-app'
import { api } from '../../api/index.js'
import { createPager, loadPage } from '../../utils/pager.js'
import { idOf } from '../../utils/format.js'
import FeedCard from '../../components/FeedCard.vue'
import StateView from '../../components/StateView.vue'
const tabs = [
  { key: 'article', label: '文章' },
  { key: 'check', label: '打卡' },
  { key: 'qa', label: '问答' },
]
const active = ref('article')
const states = { article: createPager(), check: createPager(), qa: createPager() }
const filters = reactive({
  article: { keyword: '', categoryId: '' },
  qa: { keyword: '', categoryId: '' },
  check: { monthText: '' },
})
const categories = reactive({ article: [], qa: [] })
const categoryErrors = reactive({ article: false, qa: false })
const positions = { article: 0, check: 0, qa: 0 }
const current = computed(() => states[active.value])
async function load(reset = false) {
  const type = active.value
  const f = { ...filters[type] }
  delete f.monthText
  await loadPage(states[type], (pageNum) => api.feed(type, { ...f, pageNum, pageSize: 10 }), reset)
}
async function refresh() {
  await load(true)
  uni.stopPullDownRefresh()
}
async function loadCategories(type) {
  if (type === 'check') return
  try {
    categories[type] = (await api.categories(type)) || []
    categoryErrors[type] = false
  } catch {
    categoryErrors[type] = true
  }
}
async function switchFeed(type) {
  if (type === active.value) return
  active.value = type
  await nextTick()
  uni.pageScrollTo({ scrollTop: positions[type], duration: 0 })
  if (!states[type].loaded) load()
  if (!categories[type]?.length) loadCategories(type)
}
function chooseCategory(id) {
  filters[active.value].categoryId = id
  refresh()
}
function chooseMonth(e) {
  const [year, month] = e.detail.value.split('-')
  Object.assign(filters.check, { year, month, monthText: e.detail.value })
  refresh()
}
function clearMonth() {
  filters.check = { monthText: '' }
  refresh()
}
onLoad(() => {
  load()
  loadCategories('article')
})
onPageScroll((e) => {
  positions[active.value] = e.scrollTop
})
onPullDownRefresh(refresh)
onReachBottom(() => load())
</script>
<style scoped>
.brand-mark {
  width: 86rpx;
  height: 86rpx;
  background: #eaf0ff;
  color: #2563eb;
  border-radius: 26rpx;
  text-align: center;
  line-height: 86rpx;
  font-size: 40rpx;
  font-weight: 700;
  transform: rotate(-7deg);
}
.check-filter {
  margin-bottom: 28rpx;
  flex-wrap: wrap;
}
</style>
