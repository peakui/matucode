<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { getDashboardOverviewApi } from '@/api'
import type { DashboardOverviewVO } from '@/api/types'
import './index.scss'

const loading = ref(false)
const overview = ref<DashboardOverviewVO | null>(null)

const emptyOverview: DashboardOverviewVO = {
  metrics: {
    articleCount: 0,
    checkinCount: 0,
    qaQuestionCount: 0,
    qaResolvedCount: 0,
    qaResolveRate: 0,
    interviewQuestionCount: 0,
    certificationCount: 0,
  },
  checkinRanking: [],
  courseStats: {
    courseCount: 0,
    publishedCourseCount: 0,
    studentCount: 0,
    videoCount: 0,
    articleCount: 0,
    certificateCount: 0,
  },
  ojStats: {
    problemCount: 0,
    submissionCount: 0,
    acceptedSubmissionCount: 0,
    passRate: 0,
    participantCount: 0,
  },
  interviewStats: {
    questionCount: 0,
    categoryCount: 0,
    companyCount: 0,
    lockedQuestionCount: 0,
  },
  certificationRankings: [],
}

const dashboardData = computed(() => overview.value || emptyOverview)
const metrics = computed(() => dashboardData.value.metrics)
const courseStats = computed(() => dashboardData.value.courseStats)
const ojStats = computed(() => dashboardData.value.ojStats)
const interviewStats = computed(() => dashboardData.value.interviewStats)
const certificationRankings = computed(() => dashboardData.value.certificationRankings)
const checkinRanking = computed(() => dashboardData.value.checkinRanking)

const summaryCards = computed(() => [
  { label: '文章数量', value: formatNumber(metrics.value.articleCount), helper: '平台文章总数' },
  { label: '打卡数量', value: formatNumber(metrics.value.checkinCount), helper: '累计学习打卡' },
  { label: '问答数量', value: formatNumber(metrics.value.qaQuestionCount), helper: '社区问题总数' },
  { label: '问答解决率', value: formatPercent(metrics.value.qaResolveRate), helper: `${formatNumber(metrics.value.qaResolvedCount)} 个已解决` },
])

const fetchOverview = async () => {
  loading.value = true
  try {
    const res = await getDashboardOverviewApi()
    overview.value = res.data
  } finally {
    loading.value = false
  }
}

function formatNumber(value?: number | string) {
  const numericValue = Number(value || 0)
  return numericValue.toLocaleString('zh-CN')
}

function formatPercent(value?: number) {
  const numericValue = Number(value || 0)
  return `${numericValue.toFixed(1)}%`
}

function formatRankName(item: { nickname?: string; username?: string; userId: string }) {
  return item.nickname || item.username || item.userId
}

onMounted(fetchOverview)
</script>

<template>
  <div class="dashboard-page" v-loading="loading">
    <section class="stats-grid">
      <el-card v-for="item in summaryCards" :key="item.label" shadow="never" class="stats-card">
        <p class="stats-card__label">{{ item.label }}</p>
        <div class="stats-card__value-row">
          <span class="stats-card__value">{{ item.value }}</span>
        </div>
        <p class="stats-card__helper">{{ item.helper }}</p>
      </el-card>
    </section>

    <section class="overview-grid">
      <el-card shadow="never" class="overview-card overview-card--wide">
        <template #header>
          <div class="overview-card__header">
            <div>
              <h3>打卡排名</h3>
              <p>按打卡数量统计的活跃用户</p>
            </div>
          </div>
        </template>

        <el-empty v-if="!checkinRanking.length" description="暂无打卡排名数据" />
        <div v-else class="rank-list">
          <div v-for="(item, index) in checkinRanking" :key="item.userId" class="rank-row">
            <span class="rank-row__index">{{ index + 1 }}</span>
            <div class="rank-row__main">
              <strong>{{ formatRankName(item) }}</strong>
              <p>连续 {{ item.continuousDays || 0 }} 天 · 学习 {{ item.totalLearnHours || 0 }} 小时</p>
            </div>
            <span class="rank-row__value">{{ formatNumber(item.checkinCount) }} 次</span>
          </div>
        </div>
      </el-card>

      <el-card shadow="never" class="overview-card">
        <template #header>
          <div class="overview-card__header">
            <div>
              <h3>课程信息</h3>
              <p>课程、内容与学习规模</p>
            </div>
          </div>
        </template>
        <div class="metric-list">
          <div><span>课程总数</span><strong>{{ formatNumber(courseStats.courseCount) }}</strong></div>
          <div><span>已发布课程</span><strong>{{ formatNumber(courseStats.publishedCourseCount) }}</strong></div>
          <div><span>学习人数</span><strong>{{ formatNumber(courseStats.studentCount) }}</strong></div>
          <div><span>视频数量</span><strong>{{ formatNumber(courseStats.videoCount) }}</strong></div>
          <div><span>课程文章</span><strong>{{ formatNumber(courseStats.articleCount) }}</strong></div>
          <div><span>证书数量</span><strong>{{ formatNumber(courseStats.certificateCount) }}</strong></div>
        </div>
      </el-card>

      <el-card shadow="never" class="overview-card">
        <template #header>
          <div class="overview-card__header">
            <div>
              <h3>OJ 提交信息</h3>
              <p>题目、提交与通过率</p>
            </div>
          </div>
        </template>
        <div class="oj-rate">
          <el-progress type="dashboard" :percentage="Number(ojStats.passRate || 0)" :width="128" />
          <span>总体通过率</span>
        </div>
        <div class="metric-list metric-list--compact">
          <div><span>题目数量</span><strong>{{ formatNumber(ojStats.problemCount) }}</strong></div>
          <div><span>提交次数</span><strong>{{ formatNumber(ojStats.submissionCount) }}</strong></div>
          <div><span>通过提交</span><strong>{{ formatNumber(ojStats.acceptedSubmissionCount) }}</strong></div>
          <div><span>参与人数</span><strong>{{ formatNumber(ojStats.participantCount) }}</strong></div>
        </div>
      </el-card>

      <el-card shadow="never" class="overview-card">
        <template #header>
          <div class="overview-card__header">
            <div>
              <h3>面试题数量信息</h3>
              <p>题库规模与企业覆盖</p>
            </div>
          </div>
        </template>
        <div class="metric-list">
          <div><span>面试题总数</span><strong>{{ formatNumber(interviewStats.questionCount || metrics.interviewQuestionCount) }}</strong></div>
          <div><span>题目分类</span><strong>{{ formatNumber(interviewStats.categoryCount) }}</strong></div>
          <div><span>企业数量</span><strong>{{ formatNumber(interviewStats.companyCount) }}</strong></div>
          <div><span>锁定题目</span><strong>{{ formatNumber(interviewStats.lockedQuestionCount) }}</strong></div>
        </div>
      </el-card>

      <el-card shadow="never" class="overview-card">
        <template #header>
          <div class="overview-card__header">
            <div>
              <h3>认证信息</h3>
              <p>学校 / 企业认证 Top 榜</p>
            </div>
            <el-tag type="primary" effect="plain">{{ formatNumber(metrics.certificationCount) }} 条认证</el-tag>
          </div>
        </template>
        <el-empty v-if="!certificationRankings.length" description="暂无认证统计数据" />
        <div v-else class="cert-list">
          <div v-for="item in certificationRankings" :key="`${item.type}-${item.name}`" class="cert-row">
            <div>
              <strong>{{ item.name }}</strong>
              <p>{{ item.type === 'school' ? '学校认证' : '企业认证' }}</p>
            </div>
            <span>{{ formatNumber(item.count) }}</span>
          </div>
        </div>
      </el-card>
    </section>
  </div>
</template>
