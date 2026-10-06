import { createRouter, createWebHistory } from 'vue-router'
import { getCurrentUserApi } from '@/api/auth'
import { clearAdminAuth } from '@/utils/auth'

const isAdminLoggedIn = () => {
  const isLoggedIn = localStorage.getItem('admin_logged_in') === 'true'
  const adminUserInfo = localStorage.getItem('admin_user_info')

  if (!isLoggedIn || !adminUserInfo || !localStorage.getItem('admin_token')) {
    return false
  }

  try {
    const userInfo = JSON.parse(adminUserInfo) as { roles?: string[] }
    return Array.isArray(userInfo.roles) && userInfo.roles.includes('ADMIN')
  } catch {
    return false
  }
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/login',
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/Login/index.vue'),
      meta: {
        title: '管理员登录',
      },
    },
    {
      path: '/admin',
      component: () => import('@/views/Layout/index.vue'),
      redirect: '/admin/dashboard',
      meta: {
        requiresAuth: true,
        title: '交流平台管理后台',
      },
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/views/Dashboard/index.vue'),
          meta: {
            title: '数据总览',
            description: '查看平台整体运营数据、业务模块概览与实时提醒。',
            requiresAuth: true,
          },
        },
        {
          path: 'home',
          name: 'home-manage',
          component: () => import('@/views/HomeManage/index.vue'),
          meta: {
            title: '主页动态管理',
            description: '管理首页交流内容、评论互动、话题标签与推荐流。',
            requiresAuth: true,
          },
        },
        {
          path: 'checkin',
          name: 'checkin-manage',
          component: () => import('@/views/CheckinManage/index.vue'),
          meta: {
            title: '学习打卡管理',
            description: '维护打卡规则、成长奖励、连续打卡统计与运营配置。',
            requiresAuth: true,
          },
        },
        {
          path: 'qa',
          name: 'qa-manage',
          component: () => import('@/views/QAManage/index.vue'),
          meta: {
            title: '问答社区管理',
            description: '管理问答内容、评论总结词云图与优质回答推荐。',
            requiresAuth: true,
          },
        },
        {
          path: 'announcement',
          name: 'announcement-manage',
          component: () => import('@/views/AnnouncementManage/index.vue'),
          meta: {
            title: '公告管理',
            description: '管理平台公告、维护通知、活动推广及发布状态。',
            requiresAuth: true,
          },
        },
        {
          path: 'feedback',
          name: 'feedback-manage',
          component: () => import('@/views/FeedbackManage/index.vue'),
          meta: {
            title: '反馈管理',
            description: '查看用户反馈并进行分配、优先级调整与回复处理。',
            requiresAuth: true,
          },
        },
        {
          path: 'course',
          name: 'course-manage',
          component: () => import('@/views/CourseManage/index.vue'),
          meta: {
            title: '课程管理',
            description: '管理课程、章节、视频与文章内容，并联通课程服务。',
            requiresAuth: true,
          },
          children: [
            {
              path: 'edit',
              name: 'course-create',
              component: () => import('@/views/CourseManage/Edit/index.vue'),
              meta: {
                title: '新建课程',
                description: '编辑课程基础信息、封面和定价。',
                requiresAuth: true,
              },
            },
            {
              path: 'edit/:courseId',
              name: 'course-edit',
              component: () => import('@/views/CourseManage/Edit/index.vue'),
              meta: {
                title: '编辑课程',
                description: '编辑课程基础信息、封面和定价。',
                requiresAuth: true,
              },
            },
            {
              path: ':courseId/chapters',
              name: 'course-chapters',
              component: () => import('@/views/CourseManage/Chapters/index.vue'),
              meta: {
                title: '章节内容管理',
                description: '按章节统一维护视频教程和文字教程。',
                requiresAuth: true,
              },
            },
            {
              path: ':courseId/videos',
              redirect: (to) => `/admin/course/${String(to.params.courseId)}/chapters`,
            },
          ],
        },
        {
          path: 'practice',
          name: 'practice-manage',
          component: () => import('@/views/PracticeManage/index.vue'),
          meta: {
            title: '刷题系统管理',
            description: '管理 OJ 题库、SQL 闯关、提交记录与排行。',
            requiresAuth: true,
          },
        },
        {
          path: 'interview',
          name: 'interview-manage',
          component: () => import('@/views/InterviewManage/index.vue'),
          meta: {
            title: '面试题库管理',
            description: '维护面试题内容、答案解锁规则和成长权益。',
            requiresAuth: true,
          },
        },
        {
          path: 'message',
          name: 'message-manage',
          component: () => import('@/views/MessageManage/index.vue'),
          meta: {
            title: '私信中心管理',
            description: '处理私信会话、举报消息和站内沟通安全策略。',
            requiresAuth: true,
          },
        },
        {
          path: 'ai',
          name: 'ai-manage',
          component: () => import('@/views/AiManage/index.vue'),
          meta: {
            title: 'AI 智能问答管理',
            description: '配置模型能力、查看调用监控和内容策略。',
            requiresAuth: true,
          },
        },
        {
          path: 'pay',
          name: 'pay-manage',
          component: () => import('@/views/PayManage/index.vue'),
          meta: {
            title: '支付管理',
            description: '查看支付订单、流水与退款记录，并对已支付流水发起退款。',
            requiresAuth: true,
          },
        },
        {
          path: 'search',
          name: 'search-manage',
          component: () => import('@/views/SearchManage/index.vue'),
          meta: {
            title: '搜索管理',
            description: '查看搜索引擎索引状态并触发全量重建。',
            requiresAuth: true,
          },
        },
        {
          path: 'file',
          name: 'file-manage',
          component: () => import('@/views/FileManage/index.vue'),
          meta: {
            title: '文件管理',
            description: '管理平台上传文件，支持检索、下载与删除。',
            requiresAuth: true,
          },
        },
        {
          path: 'auth',
          name: 'auth-manage',
          component: () => import('@/views/AuthManage/index.vue'),
          meta: {
            title: '认证体系管理',
            description: '维护学校、企业、老师认证及身份展示规则。',
            requiresAuth: true,
          },
        },
        {
          path: 'sys-config',
          name: 'sys-config-manage',
          component: () => import('@/views/SysConfigManage/index.vue'),
          meta: {
            title: '系统配置',
            description: '维护平台运行参数，区分公开与私有配置项。',
            requiresAuth: true,
          },
        },
        {
          path: 'metric',
          name: 'platform-metric-manage',
          component: () => import('@/views/PlatformMetricManage/index.vue'),
          meta: {
            title: '平台指标',
            description: '维护平台运营统计数据，支持按指标名与日期区间检索。',
            requiresAuth: true,
          },
        },
        {
          path: 'op-log',
          name: 'operation-log-manage',
          component: () => import('@/views/OperationLogManage/index.vue'),
          meta: {
            title: '操作日志',
            description: '查看后台与系统操作记录，支持按操作人、模块与时间检索。',
            requiresAuth: true,
          },
        },
      ],
    },
  ],
})

router.beforeEach(async (to, _from, next) => {
  let hasAdminAuth = isAdminLoggedIn()
  if (hasAdminAuth && (to.meta.requiresAuth || to.path === '/login')) {
    try {
      const { data } = await getCurrentUserApi()
      hasAdminAuth = Boolean(data.roles?.includes('ADMIN'))
      if (hasAdminAuth) localStorage.setItem('admin_user_info', JSON.stringify(data))
      else clearAdminAuth()
    } catch {
      // Preserve the session on transient outages, but do not enter protected pages.
      next(false)
      return
    }
  }

  if (to.meta.requiresAuth && !hasAdminAuth) {
    clearAdminAuth()
    next('/login')
    return
  }

  if (to.path === '/login' && hasAdminAuth) {
    next('/admin/dashboard')
    return
  }

  if (typeof to.meta.title === 'string') {
    document.title = to.meta.title
  }

  next()
})

export default router
