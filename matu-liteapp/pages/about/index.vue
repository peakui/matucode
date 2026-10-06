<template>
  <view class="page"
    ><view class="page-head"
      ><view class="eyebrow">MATU</view><view class="title">为热爱学习的你。</view
      ><view class="subtitle">码途 · 微信小程序 1.0.0</view></view
    ><view class="card"
      ><view class="section-title">随时随地，继续成长</view
      ><view class="paragraph"
        >阅读文章、发现学习记录、观看视频课程、复习面试知识。用碎片时间，把知识一点点积累起来。</view
      ><view class="divider" /><view>关于学习数据</view
      ><view class="paragraph"
        >内容、个人资料和已登录的学习进度来自码途服务。面试题收藏仅保存在当前设备，按账号隔离，暂不与 PC
        同步。</view
      ><view>本机数据</view
      ><view class="paragraph"
        >清除本机收藏不会删除账号或服务器上的学习记录。重新安装小程序或清理微信缓存可能导致本机收藏丢失。</view
      ><button class="outline" @tap="clearBookmarks">清空当前账号的本机收藏</button
      ><view class="divider" /><view>登录与个人信息</view
      ><view class="paragraph"
        >登录凭证仅用于访问你的码途账号；头像和资料仅在你主动保存时提交。微信登录未启用时可使用已有账号密码登录。</view
      ></view
    ></view
  >
</template>
<script setup>
import { requireLogin, session } from '../../utils/session.js'
function clearBookmarks() {
  if (!requireLogin()) return
  uni.showModal({
    title: '清空本机收藏',
    content: '将移除当前账号在此设备上保存的面试题收藏，无法恢复。',
    success: (r) => {
      if (r.confirm) {
        uni.removeStorageSync('matu-question-bookmarks-' + session.user.userId)
        uni.showToast({ title: '已清空', icon: 'success' })
      }
    },
  })
}
</script>
<style scoped>
.section-title {
  margin-top: 0;
}
</style>
