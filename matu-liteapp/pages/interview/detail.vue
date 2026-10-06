<template>
  <view class="page"
    ><StateView v-if="loading || error" :loading="loading" :error="error" @retry="load" /><template
      v-else-if="question"
      ><view class="row meta"
        ><text class="badge">{{ question.categoryName || '面试题' }}</text
        ><text class="muted">{{ difficulty(question.difficulty) }}</text
        ><text v-if="question.questionNo" class="muted">#{{ question.questionNo }}</text></view
      ><view class="body-title">{{ question.title }}</view
      ><view class="card"><ContentBody :content="question.content || question.title || ''" /></view
      ><view class="card answer-card"
        ><view class="row between"
          ><view class="section-title">参考答案</view><text class="muted">先思考，再查看</text></view
        ><view v-if="!answerAllowed" class="paragraph">{{
          question.isLocked === 1 ? '此答案需要相应访问权限，登录后可重新检查。' : '当前答案暂不可查看。'
        }}</view
        ><view v-else-if="!question.answer" class="paragraph">这道题暂未提供参考答案。</view
        ><template v-else
          ><button v-if="!expanded" class="secondary" @tap="expanded = true">查看参考答案</button
          ><template v-else
            ><ContentBody :content="question.answer" /><text class="link collapse" @tap="expanded = false"
              >收起答案</text
            ></template
          ></template
        ><button v-if="!loggedIn && !answerAllowed" class="secondary" @tap="login">
          登录并查看权限
        </button></view
      ><view class="section-title">这道题，掌握得怎么样？</view
      ><view class="row progress-options"
        ><button
          v-for="p in statuses"
          :key="p.value"
          :class="progress === p.value ? 'secondary' : 'outline'"
          :disabled="saving"
          @tap="mark(p.value)"
        >
          {{ p.label }}
        </button></view
      ><view v-if="progressError" class="error-inline" @tap="loadProgress"
        >{{ progressError }} · 点此重试</view
      ><view class="footnote">掌握状态登录后同步到码途账号</view
      ><view class="bottom-actions"
        ><button class="outline" :disabled="moving || position <= 0" @tap="move(-1)">上一题</button
        ><button class="secondary" @tap="bookmark">{{ collected ? '已收藏' : '本机收藏' }}</button
        ><button class="outline" :disabled="moving || !hasNext" @tap="move(1)">下一题</button></view
      ></template
    ></view
  >
</template>
<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShow, onShareAppMessage } from '@dcloudio/uni-app'
import { api } from '../../api/index.js'
import { requireLogin, session, signedIn } from '../../utils/session.js'
import { isBookmarked, toggleBookmark } from '../../utils/collections.js'
import { difficulty, toast } from '../../utils/format.js'
import ContentBody from '../../components/ContentBody.vue'
import StateView from '../../components/StateView.vue'
const id = ref(''),
  question = ref(null),
  loading = ref(true),
  error = ref(''),
  expanded = ref(false),
  progress = ref(0),
  progressError = ref(''),
  saving = ref(false),
  collected = ref(false),
  moving = ref(false),
  context = ref(null)
let revision = -1,
  sequence = 0
const statuses = [
  { value: 1, label: '已练习' },
  { value: 2, label: '已掌握' },
  { value: 3, label: '需复习' },
]
const loggedIn = computed(() => !!session.authorization)
const answerAllowed = computed(
  () =>
    question.value?.answerVisible !== false &&
    (question.value?.isLocked !== 1 || question.value?.answerVisible === true)
)
const position = computed(() => context.value?.ids?.indexOf(id.value) ?? -1)
const hasNext = computed(
  () => position.value >= 0 && (position.value < context.value.ids.length - 1 || !context.value.done)
)
async function load() {
  const seq = ++sequence
  loading.value = true
  error.value = ''
  question.value = null
  expanded.value = false
  progress.value = 0
  progressError.value = ''
  revision = session.revision
  try {
    const data = await api.question(id.value)
    if (seq !== sequence) return
    question.value = data
    collected.value = isBookmarked(id.value)
    await loadProgress()
  } catch (e) {
    if (seq === sequence) error.value = e.message
  } finally {
    if (seq === sequence) loading.value = false
  }
}
async function loadProgress() {
  if (!signedIn()) return
  const current = id.value
  const rev = session.revision
  try {
    const data = await api.questionProgress(current)
    if (current === id.value && rev === session.revision) {
      progress.value = data?.status || 0
      progressError.value = ''
    }
  } catch (e) {
    if (current === id.value && rev === session.revision && !/学习进度不存在/.test(e.message))
      progressError.value = e.message
  }
}
function login() {
  requireLogin()
}
async function mark(value) {
  if (!requireLogin() || saving.value) return
  saving.value = true
  try {
    await api.saveQuestionProgress(id.value, value)
    progress.value = value
    progressError.value = ''
    uni.showToast({ title: '学习状态已保存', icon: 'success' })
  } catch (e) {
    toast(e)
  } finally {
    saving.value = false
  }
}
function bookmark() {
  if (!requireLogin()) return
  collected.value = toggleBookmark(question.value)
  uni.showToast({ title: collected.value ? '已收藏到本机' : '已取消收藏', icon: 'none' })
}
async function move(direction) {
  if (moving.value || position.value < 0) return
  moving.value = true
  try {
    const next = position.value + direction
    if (next >= context.value.ids.length && !context.value.done) {
      const data = await api.questions({
        ...context.value.params,
        pageNum: context.value.page + 1,
        pageSize: 10,
      })
      const records = data?.records || []
      const known = new Set(context.value.ids)
      context.value.ids.push(...records.map((q) => String(q.id)).filter((q) => !known.has(q)))
      context.value.page++
      context.value.done = !records.length || context.value.ids.length >= Number(data.total)
      uni.setStorageSync('matu-interview-context', context.value)
    }
    if (context.value.ids[next]) {
      id.value = context.value.ids[next]
      await load()
      uni.pageScrollTo({ scrollTop: 0, duration: 0 })
    } else uni.showToast({ title: '已经是最后一题', icon: 'none' })
  } catch (e) {
    toast(e)
  } finally {
    moving.value = false
  }
}
onLoad((q) => {
  id.value = String(q.id || '')
  const c = uni.getStorageSync('matu-interview-context')
  if (c?.ids?.includes(id.value)) context.value = c
  load()
})
onShow(() => {
  if (id.value && revision !== session.revision) load()
  collected.value = isBookmarked(id.value)
})
onShareAppMessage(() => ({
  title: question.value?.title || '码途面试题',
  path: '/pages/interview/detail?id=' + encodeURIComponent(id.value),
}))
</script>
<style scoped>
.meta {
  flex-wrap: wrap;
  margin: 12rpx 0 24rpx;
}
.answer-card .section-title {
  margin: 0 0 24rpx;
}
.answer-card .muted {
  font-size: 22rpx;
}
.progress-options {
  gap: 14rpx;
}
.progress-options button {
  flex: 1;
  padding: 20rpx 8rpx;
  font-size: 26rpx;
}
.collapse {
  display: block;
  margin-top: 28rpx;
}
.bottom-actions {
  background: #f7f8fa;
}
.bottom-actions button {
  padding: 20rpx 10rpx;
  font-size: 26rpx;
}
</style>
