<template>
  <view class="page login-page"
    ><view class="login-logo">途</view><view class="title">欢迎来到码途</view
    ><view class="subtitle">让每一次学习，都有迹可循</view
    ><view v-if="ticket" class="card bind-card"
      ><view class="section-title">关联你的码途账号</view
      ><view class="paragraph">已有 PC 账号？绑定后可使用同一个账号学习。</view
      ><view class="tabs"
        ><view class="tab" :class="{ active: action === 'BIND' }" @tap="action = 'BIND'">绑定已有账号</view
        ><view class="tab" :class="{ active: action === 'CREATE' }" @tap="action = 'CREATE'"
          >创建新账号</view
        ></view
      ><view v-if="action === 'CREATE'" class="paragraph"
        >将创建一个新的码途账号，与已有账号的学习记录分开。</view
      ></view
    ><view v-if="!ticket || action === 'BIND'" class="form"
      ><view class="label">账号</view
      ><input
        v-model="account"
        class="input"
        :placeholder="accountFocused ? '' : '用户名或邮箱'"
        maxlength="100"
        @focus="accountFocused = true"
        @blur="accountFocused = false" /><view class="label"
        >密码</view
      ><input
        v-model="password"
        class="input"
        :placeholder="passwordFocused ? '' : '请输入密码'"
        :password="true"
        maxlength="128"
        @focus="passwordFocused = true"
        @blur="passwordFocused = false"
        @confirm="submit" /></view
    ><view v-if="error" class="error-inline">{{ error }}</view
    ><button class="primary" :loading="busy" :disabled="busy" @tap="submit">
      {{ ticket ? (action === 'BIND' ? '绑定并登录' : '创建新账号并登录') : '登录' }}</button
    ><template v-if="!ticket">
      <!-- #ifdef MP-WEIXIN -->
      <button v-if="wechatEnabled" class="wechat-button" :loading="busy" :disabled="busy" @tap="wechatLogin">
        微信快捷登录</button
      ><view v-else class="footnote">{{
        capabilityError ? '微信登录状态获取失败，可使用账号登录' : '可使用已有的码途账号登录'
      }}</view
      ><text v-if="capabilityError" class="link" @tap="checkCapabilities">重新检查微信登录</text>
      <!-- #endif --> </template
    ><button v-else class="outline reset" :disabled="busy" @tap="resetWechat">返回其他登录方式</button
    ><view class="footnote">账号与码途网站通用 · 公开内容可免登录浏览</view
    ><text class="link" @tap="back">先逛一逛</text></view
  >
</template>
<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { api } from '../../api/index.js'
import { saveSession, finishLogin } from '../../utils/session.js'
const account = ref(''),
  password = ref(''),
  accountFocused = ref(false),
  passwordFocused = ref(false),
  ticket = ref(''),
  action = ref('BIND'),
  busy = ref(false),
  error = ref(''),
  wechatEnabled = ref(false),
  capabilityError = ref(false)
let returnTo = ''
async function checkCapabilities() {
  try {
    const c = await api.capabilities()
    wechatEnabled.value = !!c?.wechatLogin
    capabilityError.value = false
  } catch {
    capabilityError.value = true
  }
}
function done(data) {
  saveSession(data)
  password.value = ''
  ticket.value = ''
  finishLogin(returnTo)
}
async function submit() {
  if (busy.value) return
  if ((!ticket.value || action.value === 'BIND') && (!account.value.trim() || !password.value)) {
    error.value = '请输入账号和密码'
    return
  }
  busy.value = true
  error.value = ''
  try {
    done(
      ticket.value
        ? await api.complete({
            ticket: ticket.value,
            action: action.value,
            ...(action.value === 'BIND' ? { account: account.value.trim(), password: password.value } : {}),
          })
        : await api.login({ account: account.value.trim(), password: password.value })
    )
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}
async function wechatLogin() {
  if (busy.value) return
  busy.value = true
  error.value = ''
  try {
    const code = await new Promise((resolve, reject) =>
      uni.login({
        provider: 'weixin',
        success: (r) => (r.code ? resolve(r.code) : reject(new Error('未取得微信登录凭证'))),
        fail: () => reject(new Error('微信登录未完成，请重试')),
      })
    )
    const data = await api.wechat(code)
    if (data.needsBinding) {
      ticket.value = data.ticket
      action.value = 'BIND'
    } else done(data.session)
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}
function resetWechat() {
  ticket.value = ''
  password.value = ''
  error.value = ''
}
function back() {
  if (getCurrentPages().length > 1) uni.navigateBack()
  else uni.switchTab({ url: '/pages/index/index' })
}
onLoad((q) => {
  returnTo = q.returnTo || ''
  checkCapabilities()
})
</script>
<style scoped>
.login-page {
  padding: 80rpx 48rpx;
  text-align: center;
}
.login-logo {
  width: 106rpx;
  height: 106rpx;
  line-height: 106rpx;
  background: #2563eb;
  border-radius: 30rpx;
  color: #fff;
  font-size: 54rpx;
  font-weight: 700;
  margin: 0 auto 36rpx;
}
.form {
  text-align: left;
  margin: 60rpx 0 28rpx;
}
.wechat-button {
  padding: 22rpx 28rpx;
  margin-top: 24rpx;
  background: #e8f6ee;
  color: #218952;
  font-weight: 600;
}
.bind-card {
  text-align: left;
  margin-top: 42rpx;
}
.bind-card .section-title {
  margin-top: 0;
}
.reset {
  margin-top: 20rpx;
}
</style>
