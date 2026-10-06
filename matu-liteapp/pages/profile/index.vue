<template>
  <view class="page"
    ><StateView v-if="loading || error" :loading="loading" :error="error" @retry="load" /><template
      v-else-if="profile"
      ><view class="card"
        ><view class="row between"
          ><view class="label">头像</view>
          <!-- #ifdef MP-WEIXIN -->
          <button
            class="avatar-button"
            open-type="chooseAvatar"
            @chooseavatar="chooseAvatar"
            :disabled="uploading"
          >
            <UserAvatar :src="assetUrl(profile.avatarUrl)" />
          </button>
          <!-- #endif -->
          <!-- #ifndef MP-WEIXIN -->
          <button class="avatar-button" @tap="chooseAlbum" :disabled="uploading">
            <UserAvatar :src="assetUrl(profile.avatarUrl)" />
          </button>
          <!-- #endif --> </view
        ><view class="muted avatar-hint">{{ uploading ? '正在上传…' : '点击头像更换' }}</view
        ><view class="divider" /><view class="label">昵称</view
        ><input
          v-model="form.nickname"
          type="nickname"
          class="input"
          placeholder="你的名字"
          maxlength="30"
        /><view class="label">个人签名</view
        ><textarea
          v-model="form.signature"
          class="input signature"
          placeholder="介绍一下自己吧"
          maxlength="255"
        /><view class="muted">账号：{{ profile.username || '—' }}</view></view
      ><view v-if="identityBadges(profile).length" class="card identity-card"
        ><view class="label">身份信息</view>
        <view class="identity"
          ><text v-for="b in identityBadges(profile)" :key="b.key" class="badge" :class="b.color">{{ b.label }}</text></view
        ></view
      ><button class="primary" :loading="saving" :disabled="saving || uploading" @tap="save">
        保存资料
      </button></template
    ><button v-else class="primary" @tap="login">登录后编辑</button></view
  >
</template>
<script setup>
import { reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { api } from '../../api/index.js'
import { uploadImage } from '../../api/request.js'
import { signedIn, requireLogin, session, updateUser } from '../../utils/session.js'
import { assetUrl, toast, identityBadges } from '../../utils/format.js'
import UserAvatar from '../../components/UserAvatar.vue'
import StateView from '../../components/StateView.vue'
const profile = ref(null),
  loading = ref(false),
  error = ref(''),
  saving = ref(false),
  uploading = ref(false),
  form = reactive({ nickname: '', signature: '' })
let revision = -1
function login() {
  requireLogin()
}
async function load() {
  if (!signedIn()) {
    profile.value = null
    login()
    return
  }
  loading.value = true
  error.value = ''
  const current = session.revision
  try {
    const data = await api.profile()
    if (current !== session.revision) return
    profile.value = data
    for (const key of Object.keys(form)) form[key] = data[key] || ''
    revision = current
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
async function save() {
  if (!requireLogin() || saving.value) return
  if (!form.nickname.trim()) {
    toast('请输入昵称')
    return
  }
  saving.value = true
  try {
    profile.value = await api.saveProfile({ ...form, nickname: form.nickname.trim() })
    updateUser({ nickname: profile.value.nickname })
    uni.showToast({ title: '资料已保存', icon: 'success' })
  } catch (e) {
    toast(e)
  } finally {
    saving.value = false
  }
}
function chooseAlbum() {
  uni.chooseImage({
    count: 1,
    sourceType: ['album'],
    success: (r) => chooseAvatar({ detail: { avatarUrl: r.tempFilePaths[0] } }),
  })
}
async function chooseAvatar(e) {
  if (!requireLogin() || uploading.value) return
  uploading.value = true
  try {
    const file = await uploadImage(e.detail.avatarUrl)
    const url = file.fileUrl || file.url || file.fullUrl
    if (!assetUrl(url)) throw new Error('上传响应未返回有效图片地址')
    profile.value = await api.avatar(url)
    updateUser({ avatarUrl: profile.value.avatarUrl })
    uni.showToast({ title: '头像已更新', icon: 'success' })
  } catch (e) {
    toast(e)
  } finally {
    uploading.value = false
  }
}
onShow(() => {
  if (!signedIn() || !profile.value || revision !== session.revision) load()
})
</script>
<style scoped>
.avatar-button {
  padding: 0;
  background: transparent;
  border-radius: 50%;
}
.avatar-button .avatar {
  width: 100rpx;
  height: 100rpx;
}
.avatar-hint {
  text-align: right;
  margin-top: 14rpx;
  font-size: 22rpx;
}
.signature {
  height: 160rpx;
}
.input {
  background: #f9fafc;
}
</style>
