<template>
  <view class="page"
    ><view class="page-head"
      ><view class="eyebrow">LEARN BY WATCHING</view><view class="title">好课程，慢慢学。</view
      ><view class="subtitle">从一节课开始，把知识变成自己的能力</view></view
    ><view class="search"
      ><view class="search-icon" /><input
        v-model="keyword"
        placeholder="搜索视频课程"
        confirm-type="search"
        @confirm="refresh"
      /><text class="link" @tap="refresh">搜索</text></view
    ><scroll-view scroll-x class="chips" :show-scrollbar="false"
      ><text
        v-for="l in levels"
        :key="l.value"
        class="chip"
        :class="{ active: level === l.value }"
        @tap="setLevel(l.value)"
        >{{ l.label }}</text
      ></scroll-view
    ><view class="row between filter-row"
      ><text class="muted">系统学习 · 持续进阶</text
      ><picker :range="sorts" range-key="label" @change="setSort"
        ><text class="link">{{ sorts[sortIndex].label }}⌄</text></picker
      ></view
    ><view v-for="course in pager.items" :key="course.id" class="card course-card" @tap="open(course.id)"
      ><view class="course-cover"
        ><image v-if="assetUrl(course.coverUrl)" :src="assetUrl(course.coverUrl)" mode="aspectFill" /><view
          v-else
          class="cover-placeholder"
          ><text class="code-mark">{{ codeMark }}</text><text>码途 · 视频课堂</text></view
        ><text class="play-icon">▶</text
        ><text class="cover-tag">{{ course.isFree === 1 ? '免费学习' : '权限课程' }}</text></view
      ><view class="course-info"
        ><view class="row"
          ><text class="badge">{{ difficulty(course.level) }}</text
          ><text class="muted">{{ course.videoCount || 0 }} 节视频</text></view
        ><view class="list-title course-title">{{ course.title }}</view
        ><view class="summary">{{ course.subtitle || '循序渐进，构建完整的知识体系' }}</view
        ><view class="row teacher"
          ><UserAvatar :src="assetUrl(course.instructorAvatarUrl)" /><text>{{
            course.instructorNickname || course.instructorName || '码途讲师'
          }}</text></view
        ></view
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
// Keep angle brackets in runtime text; static entities are decoded into invalid WXML by the compiler.
const codeMark = '< / >'
import { ref } from 'vue'
import { onLoad, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import { api } from '../../api/index.js'
import { createPager, loadPage } from '../../utils/pager.js'
import { assetUrl, difficulty } from '../../utils/format.js'
import StateView from '../../components/StateView.vue'
import UserAvatar from '../../components/UserAvatar.vue'
const pager = createPager(),
  keyword = ref(''),
  level = ref(0),
  sortIndex = ref(0)
const levels = [
  { value: 0, label: '全部课程' },
  { value: 1, label: '入门基础' },
  { value: 2, label: '进阶提升' },
  { value: 3, label: '高级实战' },
]
const sorts = [
  { label: '最新发布', value: 'latest' },
  { label: '最多学习', value: 'popular' },
  { label: '评分优先', value: 'rating' },
]
function load(reset = false) {
  return loadPage(
    pager,
    (pageNum) =>
      api.courses({
        pageNum,
        pageSize: 10,
        keyword: keyword.value,
        level: level.value || undefined,
        sortBy: sorts[sortIndex.value].value,
      }),
    reset
  )
}
async function refresh() {
  await load(true)
  uni.stopPullDownRefresh()
}
function setLevel(value) {
  level.value = value
  refresh()
}
function setSort(e) {
  sortIndex.value = Number(e.detail.value)
  refresh()
}
function open(id) {
  uni.navigateTo({ url: '/pages/course/detail?id=' + encodeURIComponent(id) })
}
onLoad(() => load())
onPullDownRefresh(refresh)
onReachBottom(() => load())
</script>
<style scoped>
.filter-row {
  margin-bottom: 24rpx;
}
.course-card {
  padding: 0;
  overflow: hidden;
}
.course-cover {
  position: relative;
  height: 330rpx;
  background: #e8efff;
}
.course-cover image {
  width: 100%;
  height: 100%;
}
.cover-placeholder {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #e9effa;
  color: #6d87b1;
  font-size: 24rpx;
}
.code-mark {
  font-size: 76rpx;
  font-weight: 700;
  color: #305bbb;
}
.play-icon {
  position: absolute;
  right: 28rpx;
  bottom: 28rpx;
  border-radius: 50%;
  width: 60rpx;
  height: 60rpx;
  line-height: 60rpx;
  text-align: center;
  background: #ffffffdc;
  color: #2563eb;
  font-size: 24rpx;
}
.cover-tag {
  position: absolute;
  top: 20rpx;
  left: 20rpx;
  padding: 5rpx 14rpx;
  border-radius: 8rpx;
  background: #ffffffec;
  color: #4b6694;
  font-size: 22rpx;
}
.course-info {
  padding: 28rpx;
}
.course-title {
  margin-top: 16rpx;
}
.summary {
  font-size: 25rpx;
  color: #8993a3;
  margin-top: 10rpx;
}
.teacher {
  margin-top: 26rpx;
  color: #6b778b;
  font-size: 24rpx;
}
.teacher .avatar {
  width: 42rpx;
  height: 42rpx;
}
</style>
