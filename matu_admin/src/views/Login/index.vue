<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import { loginApi } from '@/api'
import './index.scss'

const router = useRouter()
const loading = ref(false)
const rememberMe = ref(true)

const loginForm = reactive({
  account: '',
  password: '',
})

onMounted(() => {
  const savedAccount = localStorage.getItem('admin_account')

  if (savedAccount) {
    loginForm.account = savedAccount
  }
})

const handleForgotPassword = () => {
  ElMessage.info('请联系系统管理员重置密码')
}

const handleLogin = async () => {
  if (!loginForm.account || !loginForm.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }

  loading.value = true

  try {
    const res = await loginApi({
      account: loginForm.account,
      password: loginForm.password,
    })

    const authInfo = res.data

    if (!authInfo.roles?.includes('ADMIN')) {
      localStorage.removeItem('admin_token')
      localStorage.removeItem('admin_authorization')
      localStorage.removeItem('admin_user_info')
      localStorage.removeItem('admin_logged_in')
      ElMessage.error('当前账号不是管理员账户，无法登录后台')
      return
    }

    localStorage.setItem('admin_token', authInfo.tokenValue)
    localStorage.setItem('admin_authorization', authInfo.authorization)
    localStorage.setItem('admin_logged_in', 'true')
    localStorage.setItem(
      'admin_user_info',
      JSON.stringify({
        userId: authInfo.userId,
        username: authInfo.username,
        nickname: authInfo.nickname,
        avatar: authInfo.avatar,
        phone: authInfo.phone,
        email: authInfo.email,
        roles: authInfo.roles,
        tokenName: authInfo.tokenName,
        tokenTimeout: authInfo.tokenTimeout,
        lastLoginTime: authInfo.lastLoginTime,
      }),
    )

    if (rememberMe.value) {
      localStorage.setItem('admin_account', loginForm.account)
    } else {
      localStorage.removeItem('admin_account')
    }

    ElMessage.success('登录成功，欢迎进入管理后台')
    void router.push('/admin/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <section class="login-page__left">
      <div class="login-brand">码途后台</div>

      <div class="login-panel">
        <div class="login-panel__header">
          <h1>管理员登录</h1>
          <p>请输入账号信息后进入后台管理系统</p>
        </div>

        <el-form label-position="top" class="login-form" @submit.prevent="handleLogin">
          <div class="login-form__divider">
            <span>后台账户登录</span>
          </div>

          <el-form-item>
            <el-input
              v-model="loginForm.account"
              placeholder="请输入管理员账号或邮箱"
              size="large"
            />
          </el-form-item>

          <el-form-item>
            <el-input
              v-model="loginForm.password"
              type="password"
              show-password
              placeholder="请输入登录密码"
              size="large"
            />
          </el-form-item>

          <div class="login-form__options">
            <el-checkbox v-model="rememberMe">记住我</el-checkbox>
            <button type="button" class="login-form__link" @click="handleForgotPassword">忘记密码？</button>
          </div>

          <el-button
            type="primary"
            size="large"
            native-type="submit"
            class="login-button"
            :loading="loading"
          >
            登录并进入后台
          </el-button>
        </el-form>

        <div class="login-panel__tips">
          <span>支持用户名或邮箱登录</span>
          <span>仅 ADMIN 角色可进入后台</span>
        </div>
      </div>

      <div class="login-footer">
        <span>隐私政策</span>
        <span>Copyright 2026</span>
      </div>
    </section>

    <section class="login-page__right">
      <div class="visual-panel">
        <div class="visual-panel__content">
          <h2>计算机交流平台管理后台</h2>
          <p>
            统一管理主页动态、学习打卡、问答内容、教程专区、刷题系统、面试题库、私信与 AI
            智能问答，以及支付、搜索、系统配置等新服务。
          </p>
          <ul class="visual-panel__features">
            <li>内容审核与运营配置</li>
            <li>课程、题库与面试题统一维护</li>
            <li>支付、搜索与系统日志的新服务管理</li>
          </ul>
          <div class="visual-panel__dots">
            <span class="is-active"></span>
            <span></span>
            <span></span>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
