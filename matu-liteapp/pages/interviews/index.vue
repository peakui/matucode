<template>
  <view class="page"
    ><view class="page-head"
      ><view class="eyebrow">PREPARE WITH CONFIDENCE</view><view class="title">把每道题，弄明白。</view
      ><view class="subtitle">查漏补缺，为下一次面试做好准备</view></view
    ><view class="search"
      ><view class="search-icon" /><input
        v-model="keyword"
        placeholder="搜索知识点或面试题"
        confirm-type="search"
        @confirm="refresh"
      /><text class="link" @tap="refresh">搜索</text></view
    ><scroll-view scroll-x class="chips" :show-scrollbar="false"
      ><text class="chip" :class="{ active: !categoryId }" @tap="choose('')">全部方向</text
      ><text
        v-for="c in categories"
        :key="c.id"
        class="chip"
        :class="{ active: categoryId === c.id }"
        @tap="choose(c.id)"
        >{{ c.categoryName }}</text
      ></scroll-view
    ><view v-if="categoryError" class="error-inline" @tap="loadCategories">分类加载失败，点此重试</view
    ><view class="row between filter"
      ><text class="muted">循序练习，稳步掌握</text
      ><picker :range="difficulties" @change="setDifficulty"
        ><text class="link">{{ difficulties[level] }}⌄</text></picker
      ></view
    ><view v-for="(q, index) in pager.items" :key="q.id" class="card question-card" @tap="open(q, index)"
      ><view class="row"
        ><view class="number">{{ String(index + 1).padStart(2, '0') }}</view
        ><view class="grow"
          ><view class="list-title">{{ q.title }}</view
          ><view class="row tags"
            ><text class="badge gray">{{ q.categoryName || '面试题' }}</text
            ><text class="muted">{{ difficulty(q.difficulty) }}</text
            ><text v-if="q.isLocked === 1" class="badge">受限内容</text></view
          ></view
        ><text class="chevron">›</text></view
      ></view
    ><StateView
      :loading="pager.loading"
      :error="pager.error"
      :count="pager.items.length"
      :done="pager.done"
      @retry="load(pager.page === 0)"
  /></view>
</template>
<script setup>
import { ref } from 'vue'
import { onLoad, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import { api } from '../../api/index.js'
import { createPager, loadPage } from '../../utils/pager.js'
import { difficulty } from '../../utils/format.js'
import StateView from '../../components/StateView.vue'
const pager = createPager(),
  keyword = ref(''),
  categoryId = ref(''),
  level = ref(0),
  categories = ref([]),
  categoryError = ref(false)
const difficulties = ['全部难度', '基础', '进阶', '挑战']
const params = () => ({
  keyword: keyword.value,
  categoryId: categoryId.value,
  difficulty: level.value || undefined,
})
function load(reset = false) {
  const data = params()
  return loadPage(pager, (pageNum) => api.questions({ ...data, pageNum, pageSize: 10 }), reset)
}
async function refresh() {
  await load(true)
  uni.stopPullDownRefresh()
}
function choose(id) {
  categoryId.value = id
  refresh()
}
function setDifficulty(e) {
  level.value = Number(e.detail.value)
  refresh()
}
async function loadCategories() {
  try {
    categories.value = (await api.interviewCategories()) || []
    categoryError.value = false
  } catch {
    categoryError.value = true
  }
}
function open(q, index) {
  uni.setStorageSync('matu-interview-context', {
    ids: pager.items.map((q) => String(q.id)),
    page: pager.page,
    done: pager.done,
    params: params(),
  })
  uni.navigateTo({ url: '/pages/interview/detail?id=' + encodeURIComponent(q.id) + '&index=' + index })
}
onLoad(() => {
  load()
  loadCategories()
})
onPullDownRefresh(refresh)
onReachBottom(() => load())
</script>
<style scoped>
.filter {
  margin-bottom: 24rpx;
}
.question-card {
  padding: 28rpx 22rpx;
}
.number {
  align-self: flex-start;
  background: #f0f4fc;
  color: #6d89b5;
  border-radius: 12rpx;
  font-size: 24rpx;
  padding: 8rpx 12rpx;
  margin-right: 6rpx;
  font-weight: 700;
}
.tags {
  margin-top: 16rpx;
  flex-wrap: wrap;
  gap: 12rpx;
}
</style>
