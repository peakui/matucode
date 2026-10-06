<template>
  <view class="course-page"
    ><StateView v-if="loading || error" :loading="loading" :error="error" @retry="load" /><template
      v-else-if="course"
      ><view class="player-area"
        ><video
          v-if="playUrl"
          :key="videoId"
          id="lesson-player"
          class="player"
          :src="playUrl"
          :initial-time="initialTime"
          :autoplay="false"
          :show-mute-btn="true"
          :show-center-play-btn="true"
          @timeupdate="timeUpdate"
          @loadedmetadata="metadata"
          @pause="saveProgress"
          @ended="ended"
          @error="playError"
        /><view v-else class="player-placeholder"
          ><text class="player-symbol">▷</text
          ><text>{{ playLoading ? '正在获取播放信息' : playMessage || '选择下方课时，开始学习' }}</text
          ><button v-if="selected" class="player-retry" :disabled="playLoading" @tap="selectVideo(selected)">
            重新获取播放信息</button
          ><button v-if="!loggedIn && selected" class="player-retry" @tap="login">登录码途</button></view
        ></view
      ><view class="page course-content"
        ><view class="row"
          ><text class="badge">{{ course.isFree === 1 ? '免费课程' : '权限课程' }}</text
          ><text class="muted">{{ course.videoCount || 0 }} 节视频</text></view
        ><view class="body-title">{{ course.title }}</view
        ><view class="subtitle">{{
          selected?.videoTitle || course.subtitle || '选择一节课，开始新的学习'
        }}</view
        ><view v-if="playMessage && playUrl" class="error-inline">{{ playMessage }}</view
        ><view class="progress-note" :class="{ warning: progressError }">{{
          progressError || (loggedIn ? '学习进度将在播放过程中自动保存' : '登录后可保存学习进度')
        }}</view
        ><view class="tabs"
          ><view class="tab" :class="{ active: tab === 'catalog' }" @tap="tab = 'catalog'">课程目录</view
          ><view class="tab" :class="{ active: tab === 'intro' }" @tap="tab = 'intro'">课程简介</view
          ><view class="tab" :class="{ active: tab === 'notes' }" @tap="tab = 'notes'">学习笔记</view></view
        ><template v-if="tab === 'catalog'"
          ><view v-for="(chapter, index) in course.chapters" :key="chapter.id" class="card chapter"
            ><view class="chapter-title"
              >{{ String(index + 1).padStart(2, '0') }} / {{ chapter.chapterTitle }}</view
            ><view
              v-for="(video, vi) in chapter.videos"
              :key="video.id"
              class="lesson row"
              :class="{ active: String(video.id) === videoId }"
              @tap="selectVideo(video)"
              ><text class="lesson-number">{{ String(vi + 1).padStart(2, '0') }}</text
              ><view class="grow"
                ><view>{{ video.videoTitle }}</view
                ><view class="muted"
                  >{{ minutes(video.duration)
                  }}<text v-if="video.isFreePreview === 1 || chapter.isFreePreview === 1">
                    · 可试看</text
                  ></view
                ></view
              ><text>{{ String(video.id) === videoId ? '播放中' : '▷' }}</text></view
            ><view v-if="!chapter.videos?.length" class="muted">本章节暂无视频</view></view
          ><view v-if="!videos.length" class="footnote">课程暂未提供视频课时</view></template
        ><view v-else-if="tab === 'intro'" class="card"
          ><ContentBody :content="course.description || '暂无课程简介'" /></view
        ><view v-else class="card notes-panel"
          ><template v-if="!loggedIn"
            ><view class="muted">登录后可以查看你在各设备记录的课程笔记。</view
            ><button class="primary small notes-login" @tap="login">登录码途</button></template
          ><template v-else
            ><view class="notes-hint muted">{{
              selected ? '当前课时 · ' + selected.videoTitle : '全部课时笔记'
            }}</view
            ><view v-if="notePager.loading && !notePager.items.length" class="footnote">正在读取学习笔记…</view
            ><view v-else-if="notePager.error" class="error-inline">{{ notePager.error }}</view
            ><template v-else-if="notePager.items.length"
              ><view v-for="note in notePager.items" :key="note.id" class="note-item"
                ><view class="row between note-head"
                  ><text class="muted">{{ dateOf(note.createdAt) }}</text
                  ><text v-if="note.videoId" class="badge gray">课时</text></view
                ><text class="note-content">{{ note.content }}</text></view
              ><button
                v-if="!notePager.done"
                class="secondary small notes-more"
                :disabled="notePager.loading"
                @tap="loadNotes()"
                >{{ notePager.loading ? '加载中…' : '加载更多' }}</button
              ><view v-else class="footnote">已经看到全部笔记了</view></template
            ><view v-else class="notes-empty"
              ><view class="empty-icon">⌁</view
              ><view class="muted">还没有学习笔记，在电脑端记录后会同步到这里</view></view
            ></template
          ></view></view></template
  ></view>
</template>
<script setup>
import { computed, ref, watch } from 'vue'
import { onLoad, onShow, onHide, onUnload, onShareAppMessage } from '@dcloudio/uni-app'
import { api } from '../../api/index.js'
import { session, signedIn, requireLogin } from '../../utils/session.js'
import { assetUrl, dateOf } from '../../utils/format.js'
import { resumeTime, progressSnapshot } from '../../utils/video.js'
import { createPager, loadPage } from '../../utils/pager.js'
import ContentBody from '../../components/ContentBody.vue'
import StateView from '../../components/StateView.vue'
const id = ref(''),
  course = ref(null),
  loading = ref(true),
  error = ref(''),
  tab = ref('catalog'),
  selected = ref(null),
  videoId = ref(''),
  playUrl = ref(''),
  playMessage = ref(''),
  playLoading = ref(false),
  initialTime = ref(0),
  progressError = ref(''),
  notePager = createPager()
const loggedIn = computed(() => !!session.authorization),
  videos = computed(() => course.value?.chapters?.flatMap((ch) => ch.videos || []) || [])
let progressMap = {},
  currentTime = 0,
  duration = 0,
  lastSavedAt = 0,
  selectVersion = 0,
  revision = -1,
  saveQueue = Promise.resolve()
const minutes = (value) => (value ? `${Math.ceil(Number(value) / 60)} 分钟` : '视频课时')
async function load() {
  loading.value = true
  error.value = ''
  revision = session.revision
  try {
    course.value = await api.course(id.value)
    await loadProgress()
    if (selected.value) await selectVideo(selected.value)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
async function loadProgress() {
  progressMap = {}
  progressError.value = ''
  if (!signedIn()) return
  try {
    const data = await api.progress(id.value)
    for (const item of data || []) progressMap[String(item.videoId)] = item
  } catch {
    progressError.value = '历史进度暂未读取成功，本次可以继续学习'
  }
}
async function selectVideo(video) {
  saveProgress()
  const version = ++selectVersion
  selected.value = video
  videoId.value = String(video.id)
  playUrl.value = ''
  playMessage.value = ''
  playLoading.value = true
  currentTime = 0
  duration = Number(video.duration) || 0
  initialTime.value = 0
  try {
    const data = await api.play(video.id)
    if (version !== selectVersion) return
    if (data.playable !== true || !assetUrl(data.videoUrl)) {
      playMessage.value = data.message || '该课时暂不可播放'
      return
    }
    const saved = progressMap[String(video.id)]
    initialTime.value = resumeTime(saved, duration)
    currentTime = initialTime.value
    playUrl.value = assetUrl(data.videoUrl)
    lastSavedAt = Date.now()
  } catch (e) {
    if (version === selectVersion) playMessage.value = e.message
  } finally {
    if (version === selectVersion) playLoading.value = false
  }
}
function timeUpdate(e) {
  currentTime = Number(e.detail.currentTime) || 0
  duration = Number(e.detail.duration) || duration
  if (Date.now() - lastSavedAt >= 15000) saveProgress()
}
function metadata(e) {
  duration = Number(e.detail.duration) || duration
}
function saveProgress() {
  if (!signedIn() || !videoId.value || !playUrl.value || !duration || !currentTime) return
  lastSavedAt = Date.now()
  const auth = session.authorization,
    vid = videoId.value
  const data = progressSnapshot(vid, currentTime, duration)
  if (!data) return
  progressMap[vid] = data
  saveQueue = saveQueue
    .catch(() => {})
    .then(async () => {
      if (session.authorization !== auth) return
      try {
        await api.saveProgress(id.value, data)
        if (session.authorization === auth) progressError.value = ''
      } catch {
        if (session.authorization === auth) progressError.value = '进度暂未同步，联网后播放时会重试'
      }
    })
}
function ended() {
  currentTime = duration
  saveProgress()
}
function playError() {
  playMessage.value = '视频加载失败，请检查网络或重新选择课时'
  playUrl.value = ''
}
function pause() {
  uni.createVideoContext('lesson-player').pause()
  saveProgress()
}
function login() {
  pause()
  requireLogin()
}
function loadNotes(reset = false) {
  if (!signedIn() || !id.value) return
  const videoId = selected.value?.id
  return loadPage(
    notePager,
    (pageNum) =>
      api.notes({ courseId: id.value, videoId, onlyPublic: false, pageNum, pageSize: 10 }),
    reset
  )
}
watch(tab, (value) => {
  if (value === 'notes') loadNotes(true)
})
watch(
  () => selected.value?.id,
  () => {
    if (tab.value === 'notes') loadNotes(true)
  }
)
onLoad((q) => {
  id.value = q.id || ''
  load()
})
onShow(async () => {
  if (id.value && revision !== session.revision) {
    pause()
    playUrl.value = ''
    await load()
    if (tab.value === 'notes') loadNotes(true)
  }
})
onHide(pause)
onUnload(() => {
  pause()
  selectVersion++
})
onShareAppMessage(() => ({
  title: course.value?.title || '码途视频教程',
  path: '/pages/course/detail?id=' + encodeURIComponent(id.value),
}))
</script>
<style scoped>
.player-area {
  background: #172338;
}
.player {
  width: 100%;
  height: 422rpx;
}
.player-placeholder {
  min-height: 422rpx;
  padding: 42rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #c6d3e8;
  text-align: center;
  gap: 20rpx;
  font-size: 27rpx;
}
.player-symbol {
  font-size: 80rpx;
  color: #849bc1;
}
.player-retry {
  font-size: 24rpx;
  padding: 12rpx 24rpx;
  background: #2c4160;
  color: #e8effa;
}
.course-content .body-title {
  margin: 20rpx 0 14rpx;
  font-size: 36rpx;
}
.progress-note {
  font-size: 23rpx;
  color: #8895aa;
  padding: 24rpx 0;
}
.warning {
  color: #ba7b39;
}
.chapter-title {
  font-size: 28rpx;
  font-weight: 650;
  margin-bottom: 10rpx;
}
.lesson {
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f0f2f5;
  font-size: 27rpx;
}
.lesson:last-child {
  border: 0;
}
.lesson.active {
  color: #2563eb;
}
.lesson .muted {
  font-size: 22rpx;
  margin-top: 6rpx;
}
.lesson-number {
  font-size: 23rpx;
  color: #a3aebf;
  margin-right: 8rpx;
}
.notes-panel {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}
.notes-hint {
  font-size: 24rpx;
}
.note-item {
  padding-bottom: 20rpx;
  border-bottom: 1rpx solid #f0f2f5;
}
.note-item:last-child {
  border: 0;
}
.note-head {
  margin-bottom: 8rpx;
}
.note-content {
  display: block;
  font-size: 27rpx;
  color: #333;
  white-space: pre-wrap;
  word-break: break-word;
}
.notes-more {
  align-self: center;
}
.notes-login {
  align-self: flex-start;
}
.notes-empty {
  padding: 60rpx 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16rpx;
  text-align: center;
}
.empty-icon {
  font-size: 64rpx;
  color: #c3cbd8;
}
</style>
