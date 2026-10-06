<template>
  <view class="page detail-page"
    ><StateView v-if="loading || error" :loading="loading" :error="error" @retry="load" /><template
      v-else-if="detail"
      ><view class="badge">{{ labels[type] }}</view
      ><view class="body-title">{{ detail.title || detail.recordTitle || '学习记录' }}</view
      ><view class="row author"
        ><UserAvatar :src="avatarOf(detail)" /><view class="grow"
          ><view>{{ nameOf(detail) }}</view
          ><view v-if="identityBadges(detail).length" class="identity"
            ><text v-for="b in identityBadges(detail)" :key="b.key" class="badge" :class="b.color">{{ b.label }}</text></view
          ><view class="muted">{{ dateOf(detail.createdAt || detail.checkDate) }}</view></view
        ></view
      ><view v-if="type === 'check' && detail.learnHours" class="check-hours"
        >本次学习 <text>{{ detail.learnHours }}</text> 小时</view
      ><ContentBody :content="detail.content || detail.summary || ''" /><view
        v-if="detail.imageUrls?.length"
        class="images"
        ><image
          v-for="url in detail.imageUrls"
          :key="url"
          :src="assetUrl(url)"
          mode="widthFix"
          @tap="preview(url)" /></view
      ><view class="section-title"
        >{{ type === 'qa' ? '回答' : '评论' }} <text class="muted">{{ discussions.total }}</text></view
      ><view v-for="item in discussions.items" :key="item.id" class="card discussion"
        ><view class="row"
          ><UserAvatar :src="avatarOf(item)" /><view class="grow"
            ><view class="name">{{ nameOf(item) }}</view
            ><view v-if="identityBadges(item).length" class="identity"
              ><text v-for="b in identityBadges(item)" :key="b.key" class="badge" :class="b.color">{{ b.label }}</text></view
            ><view class="muted">{{ dateOf(item.createdAt) }}</view></view
          ><text v-if="item.isAccepted === 1" class="badge green">已采纳</text></view
        ><view class="discussion-body"><ContentBody :content="item.content || ''" /></view></view
      ><StateView
        :loading="discussions.loading"
        :error="discussions.error"
        :count="discussions.items.length"
        :done="discussions.done"
        empty-title="还没有讨论"
        empty-text="读完之后，也可以留下你的想法"
        @retry="loadDiscussions(true)"
      /><template v-if="type !== 'qa'">
        <textarea
          v-model="comment"
          class="comment-input"
          placeholder="写下你的想法…"
          maxlength="2000"
          :auto-height="true"
        /><button
          class="secondary"
          :loading="sending"
          :disabled="sending || !comment.trim()"
          @tap="sendComment"
        >
          发布评论
        </button></template
      ><view class="bottom-actions"
        ><button v-if="type !== 'qa'" class="outline" :disabled="acting" @tap="interact('like', 'liked')">
          {{ detail.liked ? '已点赞' : '点赞' }} · {{ detail.likeCount || 0 }}</button
        ><button
          v-if="type === 'article'"
          class="outline"
          :disabled="acting"
          @tap="interact('collect', 'collected')"
        >
          {{ detail.collected ? '已收藏' : '收藏文章' }}</button
        ><button v-if="type === 'qa'" class="outline" :disabled="acting" @tap="follow">
          {{ detail.followed ? '已关注问题' : '关注这个问题' }}</button
        ><button class="outline" open-type="share">分享</button></view
      ></template
    ></view
  >
</template>
<script setup>
import { ref } from 'vue'
import { onLoad, onReachBottom, onShow, onShareAppMessage } from '@dcloudio/uni-app'
import { api } from '../../api/index.js'
import { request } from '../../api/request.js'
import { createPager, loadPage } from '../../utils/pager.js'
import { requireLogin, session } from '../../utils/session.js'
import { nameOf, avatarOf, dateOf, assetUrl, toast, identityBadges } from '../../utils/format.js'
import ContentBody from '../../components/ContentBody.vue'
import UserAvatar from '../../components/UserAvatar.vue'
import StateView from '../../components/StateView.vue'
const type = ref('article'),
  id = ref(''),
  detail = ref(null),
  loading = ref(true),
  error = ref(''),
  acting = ref(false),
  sending = ref(false),
  comment = ref(''),
  discussions = createPager()
const labels = { article: '文章', check: '学习打卡', qa: '问答' }
let revision = -1
async function load() {
  loading.value = true
  error.value = ''
  revision = session.revision
  try {
    detail.value = await api.detail(type.value, id.value)
    await loadDiscussions(true)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
function loadDiscussions(reset = false) {
  return loadPage(
    discussions,
    async (pageNum) => {
      const data = await api.discussions(type.value, id.value, { pageNum, pageSize: 10 })
      return Array.isArray(data) ? { records: data, total: data.length } : data
    },
    reset
  )
}
async function interact(action, field) {
  if (!requireLogin() || acting.value) return
  acting.value = true
  try {
    await api.interact(type.value, id.value, action, !!detail.value[field])
    detail.value[field] = !detail.value[field]
    if (action === 'like')
      detail.value.likeCount = Math.max(
        0,
        Number(detail.value.likeCount || 0) + (detail.value[field] ? 1 : -1)
      )
  } catch (e) {
    toast(e)
  } finally {
    acting.value = false
  }
}
async function follow() {
  if (!requireLogin() || acting.value) return
  acting.value = true
  try {
    await request(
      `/qa/questions/${encodeURIComponent(id.value)}/${detail.value.followed ? 'unfollow' : 'follow'}`,
      {},
      'POST'
    )
    detail.value.followed = !detail.value.followed
  } catch (e) {
    toast(e)
  } finally {
    acting.value = false
  }
}
async function sendComment() {
  if (!requireLogin() || sending.value || !comment.value.trim()) return
  sending.value = true
  try {
    await api.comment(type.value, id.value, comment.value.trim())
    comment.value = ''
    await loadDiscussions(true)
    uni.showToast({ title: '评论已发布', icon: 'success' })
  } catch (e) {
    toast(e)
  } finally {
    sending.value = false
  }
}
function preview(url) {
  uni.previewImage({ current: assetUrl(url), urls: detail.value.imageUrls.map(assetUrl).filter(Boolean) })
}
onLoad((q) => {
  type.value = labels[q.type] ? q.type : 'article'
  id.value = q.id || ''
  load()
})
onShow(() => {
  if (id.value && revision !== session.revision) load()
})
onReachBottom(() => {
  if (type.value === 'qa') loadDiscussions()
})
onShareAppMessage(() => ({
  title: detail.value?.title || '码途 · 一起学习',
  path: `/pages/content/detail?type=${type.value}&id=${encodeURIComponent(id.value)}`,
}))
</script>
<style scoped>
.detail-page {
  background: #fff;
  min-height: 100vh;
}
.body-title {
  margin-top: 20rpx;
}
.author {
  margin-bottom: 36rpx;
}
.check-hours {
  background: #eff8f2;
  color: #4c8961;
  padding: 20rpx 24rpx;
  border-radius: 16rpx;
  margin-bottom: 28rpx;
}
.check-hours text {
  font-size: 36rpx;
  font-weight: 700;
  margin: 0 10rpx;
}
.images image {
  width: 100%;
  border-radius: 18rpx;
  margin: 20rpx 0;
}
.discussion .avatar {
  width: 50rpx;
  height: 50rpx;
}
.discussion .muted {
  font-size: 21rpx;
}
.discussion-body {
  margin-top: 20rpx;
}
.name {
  font-size: 25rpx;
}
.comment-input {
  width: 100%;
  min-height: 130rpx;
  background: #f7f8fa;
  padding: 22rpx;
  border-radius: 16rpx;
  margin: 20rpx 0;
  font-size: 27rpx;
}
.bottom-actions {
  margin-top: 24rpx;
}
</style>
