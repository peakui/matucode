<template>
  <view class="page"
    ><view class="page-head"
      ><view class="title">{{ title }}</view
      ><view class="subtitle">{{
        kind === 'bookmarks' ? '仅保存在当前设备，按账号分别管理' : '你的每一份积累，都值得回顾'
      }}</view></view
    ><template v-if="authorized"
      ><template v-if="kind === 'bookmarks'"
        ><view v-for="q in saved" :key="q.id" class="card" @tap="openQuestion(q)"
          ><view class="list-title">{{ q.title }}</view
          ><view class="row between bookmark-meta"
            ><text class="badge gray">{{ q.categoryName || '面试题' }}</text
            ><text class="link" @tap.stop="remove(q)">移除收藏</text></view
          ></view
        ><StateView
          :count="saved.length"
          :done="true"
          empty-title="还没有收藏的题目"
          empty-text="在面试题详情里，收藏值得复习的知识点" /></template
      ><template v-else
        ><FeedCard
          v-for="item in pager.items"
          :key="idOf(item)"
          :item="item"
          :type="kind === 'checks' ? 'check' : 'qa'" /><StateView
          :loading="pager.loading"
          :error="pager.error"
          :count="pager.items.length"
          :done="pager.done"
          @retry="load(true)" /></template></template
    ><button v-else class="primary" @tap="login">登录后查看</button></view
  >
</template>
<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShow, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import { api } from '../../api/index.js'
import { createPager, loadPage } from '../../utils/pager.js'
import { session, signedIn, requireLogin } from '../../utils/session.js'
import { bookmarks, toggleBookmark } from '../../utils/collections.js'
import { idOf } from '../../utils/format.js'
import FeedCard from '../../components/FeedCard.vue'
import StateView from '../../components/StateView.vue'
const kind = ref('checks'),
  saved = ref([]),
  authorized = ref(false),
  pager = createPager()
let revision = -1
const title = computed(
  () => ({ checks: '我的打卡', questions: '我的问答', bookmarks: '面试题收藏' })[kind.value]
)
function load(reset = false) {
  if (!signedIn()) return
  if (kind.value === 'bookmarks') {
    saved.value = bookmarks()
    return
  }
  return loadPage(
    pager,
    (pageNum) =>
      kind.value === 'checks'
        ? api.myChecks({ userId: session.user.userId, pageNum, pageSize: 10 })
        : api.myQuestions({ pageNum, pageSize: 10 }),
    reset
  )
}
function login() {
  requireLogin()
}
function remove(q) {
  toggleBookmark(q)
  saved.value = bookmarks()
}
function openQuestion(q) {
  uni.setStorageSync('matu-interview-context', {
    ids: saved.value.map((q) => String(q.id)),
    page: 1,
    done: true,
    local: true,
  })
  uni.navigateTo({ url: '/pages/interview/detail?id=' + encodeURIComponent(q.id) })
}
onLoad((q) => {
  kind.value = ['bookmarks', 'checks', 'questions'].includes(q.kind) ? q.kind : 'checks'
  uni.setNavigationBarTitle({ title: title.value })
})
onShow(() => {
  authorized.value = signedIn()
  if (!authorized.value) {
    pager.generation++
    pager.items = []
    saved.value = []
    login()
    return
  }
  const reset = revision !== session.revision
  revision = session.revision
  if (reset || !pager.loaded || kind.value === 'bookmarks') load(true)
})
onPullDownRefresh(async () => {
  await load(true)
  uni.stopPullDownRefresh()
})
onReachBottom(() => load())
</script>
<style scoped>
.bookmark-meta {
  margin-top: 22rpx;
  font-size: 24rpx;
}
</style>
