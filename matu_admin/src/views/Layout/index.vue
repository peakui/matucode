<script setup lang="ts">
import {
  Bell,
  Calendar,
  ChatDotSquare,
  Coin,
  DataBoard,
  EditPen,
  Folder,
  House,
  MagicStick,
  Medal,
  Memo,
  Message,
  Search,
  Setting,
  SetUp,
  Tickets,
  TrendCharts,
} from '@element-plus/icons-vue'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import { getCurrentUserApi, logoutApi } from '@/api'
import './index.scss'

const route = useRoute()
const router = useRouter()

const menuItems = [
  { index: '/admin/dashboard', label: '数据总览', icon: DataBoard },
  { index: '/admin/home', label: '主页动态', icon: House },
  { index: '/admin/checkin', label: '学习打卡', icon: Calendar },
  { index: '/admin/qa', label: '问答社区', icon: ChatDotSquare },
  { index: '/admin/announcement', label: '公告管理', icon: Bell },
  { index: '/admin/feedback', label: '反馈管理', icon: Message },
  { index: '/admin/course', label: '课程管理', icon: Memo },
  { index: '/admin/practice', label: '刷题系统', icon: EditPen },
  { index: '/admin/interview', label: '面试题库', icon: Memo },
  { index: '/admin/message', label: '私信中心', icon: Message },
  { index: '/admin/ai', label: 'AI 智能问答', icon: MagicStick },
  { index: '/admin/pay', label: '支付管理', icon: Coin },
  { index: '/admin/search', label: '搜索管理', icon: Search },
  { index: '/admin/file', label: '文件管理', icon: Folder },
  { index: '/admin/auth', label: '认证体系', icon: Medal },
  { index: '/admin/sys-config', label: '系统配置', icon: SetUp },
  { index: '/admin/metric', label: '平台指标', icon: TrendCharts },
  { index: '/admin/op-log', label: '操作日志', icon: Tickets },
]

interface AdminUserInfo {
  userId?: string | number
  username?: string
  nickname?: string
  avatar?: string
  avatarUrl?: string
  headImg?: string
  phone?: string
  email?: string
  roles?: string[]
  tokenName?: string
  tokenTimeout?: number
  lastLoginTime?: string
}

const readUserInfo = (): AdminUserInfo => {
  const raw = localStorage.getItem('admin_user_info')
  if (!raw) return {}

  try {
    return JSON.parse(raw) as AdminUserInfo
  } catch {
    return {}
  }
}

const adminUserInfo = ref<AdminUserInfo>(readUserInfo())

const syncCurrentUser = async () => {
  try {
    const res = await getCurrentUserApi()
    adminUserInfo.value = {
      userId: res.data.userId,
      username: res.data.username,
      nickname: res.data.nickname,
      avatar: res.data.avatar,
      avatarUrl: res.data.avatarUrl,
      headImg: res.data.headImg,
      phone: res.data.phone,
      email: res.data.email,
      roles: res.data.roles,
      tokenName: res.data.tokenName,
      tokenTimeout: res.data.tokenTimeout,
      lastLoginTime: res.data.lastLoginTime,
    }
    localStorage.setItem('admin_user_info', JSON.stringify(adminUserInfo.value))
  } catch {
    adminUserInfo.value = readUserInfo()
  }
}

const currentMenuItem = computed(
  () => menuItems.find((item) => route.path === item.index || route.path.startsWith(`${item.index}/`)),
)

const currentPanelTitle = computed(() => {
  return currentMenuItem.value?.label ?? '交流平台管理后台'
})

const currentPanelDesc = computed(() => {
  return (route.meta.description as string) ?? '围绕平台内容、成长体系与智能服务进行统一管理'
})

const displayName = computed(() => adminUserInfo.value.nickname || adminUserInfo.value.username || '管理员')
const avatarUrl = computed(() => adminUserInfo.value.avatar || adminUserInfo.value.avatarUrl || adminUserInfo.value.headImg || '')
const avatarText = computed(() => (displayName.value?.trim()?.charAt(0) || 'A').toUpperCase())

const handleSelect = (index: string) => {
  void router.push(index)
}

const activeMenuIndex = computed(() => currentMenuItem.value?.index ?? route.path)

const handleOpenSettings = () => {
  void router.push('/admin/sys-config')
}

const handleLogout = async () => {
  try {
    await logoutApi()
  } catch {
    // Ignore logout API errors — clear local session regardless.
  }
  localStorage.removeItem('admin_token')
  localStorage.removeItem('admin_authorization')
  localStorage.removeItem('admin_user_info')
  localStorage.removeItem('admin_logged_in')
  ElMessage.success('已退出登录')
  void router.push('/login')
}

onMounted(async () => {
  await syncCurrentUser()
})
</script>

<template>
  <el-container class="admin-layout">
    <el-aside width="260px" class="admin-sidebar">
      <div class="brand-panel">
        <div class="brand-panel__logo">M</div>
        <div>
          <h2>码途管理台</h2>
          <p>计算机交流平台 Admin</p>
        </div>
      </div>

      <el-menu
        :default-active="activeMenuIndex"
        class="sidebar-menu"
        background-color="transparent"
        text-color="#667085"
        active-text-color="#1d4ed8"
        @select="handleSelect"
      >
        <el-menu-item
          :class="{ 'menu-is-active': activeMenuIndex === item.index }"
          v-for="item in menuItems"
          :key="item.index"
          :index="item.index"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-footer">
        <button type="button" class="sidebar-footer__item" @click="handleOpenSettings">
          <el-icon><Setting /></el-icon>
          <span>系统设置</span>
        </button>
      </div>
    </el-aside>

    <el-container class="admin-content-shell">
      <el-header class="admin-header">
        <div class="admin-header__title">
          <h1>{{ currentPanelTitle }}</h1>
          <p>{{ currentPanelDesc }}</p>
        </div>

        <div class="admin-header__toolbar">
          <button class="toolbar-icon" type="button" aria-label="通知">
            <el-icon><Bell /></el-icon>
          </button>

          <div class="toolbar-user">
            <el-avatar :src="avatarUrl" size="default">{{ avatarText }}</el-avatar>
            <strong>{{ displayName }}</strong>
          </div>

          <el-button type="danger" plain @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>

      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>
