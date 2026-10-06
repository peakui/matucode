<template>
  <view class="page"
    ><view class="page-head"
      ><view class="eyebrow">MY LEARNING SPACE</view><view class="title">成长，始于积累。</view></view
    ><view class="card profile-card" @tap="open('/pages/profile/index')"
      ><view class="row"
        ><UserAvatar :src="assetUrl(session.user?.avatarUrl)" /><view class="grow"
          ><view class="profile-name">{{
            session.user?.nickname || session.user?.username || '欢迎来到码途'
          }}</view
          ><view class="muted">{{ loggedIn ? '今天也向前一步' : '登录后，开启你的学习旅程' }}</view></view
        ><text class="chevron">›</text></view
      ><view v-if="!loggedIn" class="login-cta">登录 / 关联已有账号 <text>→</text></view
      ><view v-else class="row profile-bottom"
        ><text class="badge">{{ session.user.vip ? 'VIP 学习者' : '码途学习者' }}</text
        ><text class="muted">{{ session.user.wechatBound ? '已关联微信' : '码途账号' }}</text></view
      ></view
    ><view v-if="stats" class="card stats"
      ><view v-if="stats.totalDays != null"
        ><text class="stat-number">{{ stats.totalDays }}</text
        ><text class="muted">累计打卡</text></view
      ><view v-if="stats.continuousDays != null"
        ><text class="stat-number">{{ stats.continuousDays }}</text
        ><text class="muted">连续天数</text></view
      ><view v-if="stats.totalLearnHours != null"
        ><text class="stat-number">{{ stats.totalLearnHours }}</text
        ><text class="muted">学习小时</text></view
      ></view
    ><view v-if="error" class="error-inline" @tap="sync">{{ error }} · 点此重试</view
    ><view class="section-title">我的学习</view
    ><view class="card menu"
      ><view v-for="m in menus" :key="m.kind" class="menu-row" @tap="open('/pages/mine/index?kind=' + m.kind)"
        ><view class="menu-symbol">{{ m.symbol }}</view
        ><view class="grow"
          ><view>{{ m.title }}</view
          ><view class="muted">{{ m.desc }}</view></view
        ><text class="chevron">›</text></view
      ></view
    ><view class="card menu"
      ><view class="menu-row" @tap="open('/pages/profile/index')"
        ><view class="menu-symbol">人</view><view class="grow">个人资料</view
        ><text class="chevron">›</text></view
      ><view class="menu-row" @tap="about"
        ><view class="menu-symbol">i</view><view class="grow">设置与关于</view
        ><text class="chevron">›</text></view
      ></view
    ><button v-if="loggedIn" class="outline" :disabled="loggingOut" @tap="logout">退出登录</button
    ><view class="footnote">码途 MATU · 与每一份热爱同行</view></view
  >
</template>
<script setup>
import { computed, ref } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import { api } from '../../api/index.js'
import { session, signedIn, requireLogin, updateUser, clearSession } from '../../utils/session.js'
import { assetUrl } from '../../utils/format.js'
import UserAvatar from '../../components/UserAvatar.vue'
const loggedIn = computed(() => !!session.authorization),
  stats = ref(null),
  error = ref(''),
  loggingOut = ref(false)
const menus = [
  { kind: 'bookmarks', title: '面试题收藏', desc: '本机保存，随时复习', symbol: '☆' },
  { kind: 'checks', title: '我的打卡', desc: '记录走过的每一步', symbol: '✓' },
  { kind: 'questions', title: '我的问答', desc: '在提问中发现新知', symbol: '?' },
]
async function sync() {
  stats.value = null
  error.value = ''
  if (!signedIn()) return
  const revision = session.revision
  const results = await Promise.allSettled([api.me(), api.statistics(session.user.userId)])
  if (revision !== session.revision) return
  if (results[0].status === 'fulfilled') updateUser(results[0].value)
  else error.value = results[0].reason.message
  if (results[1].status === 'fulfilled') stats.value = results[1].value
}
function open(url) {
  if (requireLogin(url)) uni.navigateTo({ url })
}
function about() {
  uni.navigateTo({ url: '/pages/about/index' })
}
function logout() {
  uni.showModal({
    title: '退出登录',
    content: '确认退出当前码途账号？',
    success: async (res) => {
      if (!res.confirm) return
      loggingOut.value = true
      try {
        await api.logout()
      } catch {
      } finally {
        clearSession()
        stats.value = null
        error.value = ''
        loggingOut.value = false
      }
    },
  })
}
onShow(sync)
onPullDownRefresh(async () => {
  await sync()
  uni.stopPullDownRefresh()
})
</script>
<style scoped>
.profile-card {
  padding: 34rpx;
}
.profile-card .avatar {
  width: 104rpx;
  height: 104rpx;
}
.profile-name {
  font-size: 35rpx;
  font-weight: 700;
  margin-bottom: 8rpx;
}
.profile-bottom {
  margin-top: 28rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid #f0f3f7;
  justify-content: space-between;
}
.login-cta {
  color: #2563eb;
  font-weight: 600;
  padding-top: 30rpx;
  display: flex;
  justify-content: space-between;
}
.stats {
  display: flex;
  justify-content: space-around;
  text-align: center;
}
.stats > view {
  display: flex;
  flex-direction: column;
}
.stat-number {
  font-size: 42rpx;
  font-weight: 700;
}
.stats .muted {
  font-size: 23rpx;
}
.menu {
  padding: 4rpx 28rpx;
}
</style>
