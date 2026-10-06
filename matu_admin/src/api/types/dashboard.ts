export interface DashboardMetricVO {
  articleCount: number
  checkinCount: number
  qaQuestionCount: number
  qaResolvedCount: number
  qaResolveRate: number
  interviewQuestionCount: number
  certificationCount: number
}

export interface DashboardCheckinRankItemVO {
  userId: string
  username?: string
  nickname?: string
  avatar?: string
  checkinCount: number
  continuousDays?: number
  totalLearnHours?: number | string
}

export interface DashboardCourseStatsVO {
  courseCount: number
  publishedCourseCount: number
  studentCount: number
  videoCount: number
  articleCount: number
  certificateCount: number
}

export interface DashboardOjStatsVO {
  problemCount: number
  submissionCount: number
  acceptedSubmissionCount: number
  passRate: number
  participantCount: number
}

export interface DashboardInterviewStatsVO {
  questionCount: number
  categoryCount: number
  companyCount: number
  lockedQuestionCount: number
}

export interface DashboardCertificationRankItemVO {
  name: string
  type: 'school' | 'company'
  count: number
}

export interface DashboardOverviewVO {
  metrics: DashboardMetricVO
  checkinRanking: DashboardCheckinRankItemVO[]
  courseStats: DashboardCourseStatsVO
  ojStats: DashboardOjStatsVO
  interviewStats: DashboardInterviewStatsVO
  certificationRankings: DashboardCertificationRankItemVO[]
  updatedAt?: string
}
