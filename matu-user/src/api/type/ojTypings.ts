import type { ApiResponse, PageResponse } from './postTypings'

export type OjClassType = 1 | 2
export type OjJoinMode = 1 | 2 | 3
export type OjJudgeStatus = 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7

export interface ListOjClassesParams {
  keyword?: string
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface JoinClassRequest {
  inviteCode?: string
  message?: string
}

export interface ListOjProblemsParams {
  keyword?: string
  difficulty?: number
  status?: number
  pageNum?: number
  pageSize?: number
}

export type ListOjClassProblemsParams = ListOjProblemsParams

export interface CreateOjProblemRequest {
  problemNo: string
  title: string
  description: string
  inputFormat?: string
  outputFormat?: string
  sampleInput?: string
  sampleOutput?: string
  hint?: string
  difficulty: number
  categoryId?: string | number | null
  timeLimit?: number
  memoryLimit?: number
  status?: number
}

export type UpdateOjProblemRequest = Partial<CreateOjProblemRequest>

export interface AssociateOjProblemsRequest {
  problemIds: Array<string | number>
}

export interface CreateOjTestCaseRequest {
  caseNo: number
  input: string
  expectedOutput: string
  isSample?: number
  scoreWeight?: number
  isHidden?: number
}

export interface SubmitOjCodeRequest {
  problemId: string | number
  language: string
  code: string
  assignmentId?: string | number
}

export interface ListOjSubmissionsParams {
  problemId?: string | number
  userId?: string | number
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface ClassVO {
  id?: string | number
  name?: string
  type?: OjClassType
  description?: string
  creatorId?: string | number
  coverImage?: string
  joinMode?: OjJoinMode
  inviteCode?: string
  status?: number
  joined?: boolean
  memberCount?: number
  startTime?: string
  endTime?: string
  createdAt?: string
}

export interface OjClassRankingProblem {
  problemId?: string | number
  problemNo?: string
  title?: string
  index?: number
}

export interface OjClassRankingCell {
  problemId?: string | number
  solved?: boolean
  wrongAttempts?: number
  acMinutes?: number | null
  acAt?: string | null
}

export interface OjClassRankingRow {
  rank?: number
  userId?: string | number
  userName?: string
  avatarUrl?: string | null
  solvedCount?: number
  penaltyMinutes?: number
  lastAcAt?: string | null
  cells?: OjClassRankingCell[]
}

export interface OjClassRankingVO {
  classId?: string | number
  name?: string
  startTime?: string | null
  endTime?: string | null
  memberCount?: number
  problems?: OjClassRankingProblem[]
  rows?: OjClassRankingRow[]
}

export interface ClassMemberVO {
  id?: string | number
  classId?: string | number
  userId?: string | number
  userName?: string
  username?: string
  nickname?: string
  userAvatar?: string
  avatarUrl?: string
  role?: number
  roleType?: number
  joinStatus?: number
  status?: number
  applyMessage?: string
  message?: string
  rejectReason?: string
  auditRemark?: string
  joinedAt?: string
  createdAt?: string
  updatedAt?: string
}

export interface ReviewOjClassMemberRequest {
  remark?: string
  reason?: string
}

export interface ClassAssignmentVO {
  id?: string | number
  classId?: string | number
  title?: string
  description?: string
  type?: number
  problemIds?: Array<string | number>
  startTime?: string
  deadline?: string
  maxAttempts?: number
  isPublicRank?: number
  status?: number
  createdAt?: string
}

export interface AssignmentProblemVO {
  problemId?: string | number
  problemNo?: string | null
  title?: string | null
  difficulty?: number | null
  solved?: boolean
  attemptsUsed?: number
  latestSubmissionId?: string | number | null
}

export interface AssignmentDetailVO {
  id?: string | number
  classId?: string | number
  title?: string
  description?: string
  type?: number
  startTime?: string
  deadline?: string
  maxAttempts?: number
  isPublicRank?: number
  status?: number
  submittable?: boolean
  problems?: AssignmentProblemVO[]
}

export interface AssignmentRankingRow {
  rank?: number
  userId?: string | number
  nickname?: string | null
  avatarUrl?: string | null
  solvedCount?: number
  score?: number
  lastSubmitAt?: string | null
}

export interface AssignmentRankingVO {
  assignmentId?: string | number
  totalProblems?: number
  isPublicRank?: number
  rows?: AssignmentRankingRow[]
}

export interface ClassSubmissionVO {
  id?: string | number
  assignmentId?: string | number
  userId?: string | number
  nickname?: string | null
  problemId?: string | number
  problemTitle?: string | null
  submissionId?: string | number | null
  status?: number
  score?: number
  submittedAt?: string | null
}

export interface ListAssignmentSubmissionsParams {
  userId?: string | number
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface ClassDiscussionVO {
  id?: string | number
  classId?: string | number
  assignmentId?: string | number
  title?: string
  content?: string
  isAnonymous?: number
  userId?: string | number
  userName?: string
  createdAt?: string
}

export interface OjProblemVO {
  id?: string | number
  problemNo?: string
  title?: string
  description?: string
  inputFormat?: string
  outputFormat?: string
  sampleInput?: string
  sampleOutput?: string
  hint?: string
  difficulty?: number
  categoryId?: string | number | null
  timeLimit?: number
  memoryLimit?: number
  submitCount?: number
  acceptCount?: number
  acceptRate?: number
  status?: number
  contentMd?: string | null
  contentHtml?: string | null
  starterCodeJson?: string | null
  solutionJson?: string | null
  tags?: string[] | string
  createdAt?: string
  updatedAt?: string
}

export interface OjSubmissionVO {
  id?: string | number
  problemId?: string | number
  problemNo?: string
  problemTitle?: string
  userId?: string | number
  userName?: string
  username?: string
  nickname?: string
  language?: string
  code?: string
  status?: OjJudgeStatus
  executionTime?: number
  memoryUsed?: number
  errorMessage?: string
  createdAt?: string
}

export interface OjSolvedCountStatisticsVO {
  userId?: string | number
  solvedProblemCount?: number
}

export interface OjSubmissionDetailVO {
  id?: string | number
  submissionId?: string | number
  caseNo?: number
  input?: string
  expectedOutput?: string
  actualOutput?: string
  status?: OjJudgeStatus
  executionTime?: number
  memoryUsed?: number
  errorMessage?: string
  createdAt?: string
}

export interface OjTestCaseVO {
  id?: string | number
  problemId?: string | number
  caseNo?: number
  input?: string
  expectedOutput?: string
  isSample?: number
  scoreWeight?: number
  isHidden?: number
  createdAt?: string
}

export type OjPageResponse<T> = PageResponse<T>
export type OjApiResponse<T> = ApiResponse<T>
