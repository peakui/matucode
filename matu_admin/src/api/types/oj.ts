export interface ClassListParams {
  keyword?: string
  type?: number
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface OjProblemListParams {
  keyword?: string
  difficulty?: number
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface OjSubmissionListParams {
  problemId?: string
  userId?: string
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface ClassVO {
  id: string
  name: string
  description?: string
  type?: number
  creatorId: string
  creatorName?: string
  creatorUsername?: string
  username?: string
  nickname?: string
  coverImage?: string
  joinMode: number
  inviteCode?: string
  memberCount?: number
  joined?: boolean
  status: number
  startTime?: string
  endTime?: string
  createdAt?: string
  updatedAt?: string
}

export interface ClassMemberVO {
  id: string
  classId: string
  userId: string
  username?: string
  nickname?: string
  avatar?: string
  avatarUrl?: string
  role: number
  joinStatus: number
  message?: string
  joinedAt?: string
}

export interface ClassAssignmentVO {
  id: string
  classId: string
  title: string
  description?: string
  type: number
  problemIds?: string[]
  startTime?: string
  deadline?: string
  maxAttempts?: number
  isPublicRank: number
  status: number
  createdBy: string
  createdAt?: string
  updatedAt?: string
}

export interface AssignmentProblemVO {
  problemId: string
  problemNo?: string | null
  title?: string | null
  difficulty?: number | null
  solved: boolean
  attemptsUsed: number
  latestSubmissionId?: string | null
}

export interface AssignmentDetailVO {
  id: string
  classId: string
  title: string
  description?: string
  type: number
  startTime?: string
  deadline?: string
  maxAttempts?: number
  isPublicRank: number
  status: number
  submittable: boolean
  problems: AssignmentProblemVO[]
}

export interface AssignmentRankingRow {
  rank: number
  userId: string
  nickname?: string | null
  avatarUrl?: string | null
  solvedCount: number
  score: number
  lastSubmitAt?: string | null
}

export interface AssignmentRankingVO {
  assignmentId: string
  totalProblems: number
  isPublicRank: number
  rows: AssignmentRankingRow[]
}

export interface ClassSubmissionVO {
  id: string
  assignmentId: string
  userId: string
  nickname?: string | null
  problemId: string
  problemTitle?: string | null
  submissionId?: string | null
  status: number
  score?: number
  submittedAt?: string | null
}

export interface AssignmentSubmissionListParams {
  userId?: string
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface ClassDiscussionVO {
  id: string
  classId: string
  assignmentId?: string | null
  userId: string
  username?: string
  nickname?: string
  title: string
  content: string
  isAnonymous: number
  createdAt?: string
}

export interface OjProblemVO {
  id: string
  problemNo: string
  title: string
  description: string
  contentMd?: string
  contentHtml?: string
  inputFormat?: string
  outputFormat?: string
  sampleInput?: string
  sampleOutput?: string
  hint?: string
  difficulty: number
  categoryId?: string | null
  tags?: string[] | string
  starterCodeJson?: string
  solutionJson?: string
  timeLimit: number
  memoryLimit: number
  submitCount: number
  acceptCount: number
  acceptRate?: number
  status: number
  createdAt?: string
  updatedAt?: string
}

export interface OjTestCaseVO {
  id: string
  problemId: string
  caseNo: number
  input: string
  expectedOutput: string
  isSample: number
  scoreWeight?: number
  isHidden: number
  createdAt?: string
}

export interface OjSubmissionVO {
  id: string
  problemId: string
  userId: string
  language: string
  code: string
  codeLength: number
  status: number
  executionTime: number
  memoryUsed: number
  passRate?: number
  passedCases: number
  totalCases: number
  errorMessage?: string
  judgeTime?: string
  createdAt?: string
}

export interface OjSubmissionDetailVO {
  id: string
  submissionId: string
  caseNo: number
  status: number
  executionTime: number
  memoryUsed: number
  output?: string
  expectedOutput?: string
  createdAt?: string
}

export interface CreateClassRequest {
  name: string
  description?: string
  type?: number
  coverImage?: string
  joinMode?: number
  inviteCode?: string
  startTime?: string
  endTime?: string
}

export interface UpdateClassRequest extends Partial<CreateClassRequest> {
  status?: number
}

export interface JoinClassRequest {
  inviteCode?: string
  message?: string
}

export interface CreateAssignmentRequest {
  title: string
  description?: string
  type: number
  problemIds?: string[]
  startTime?: string
  deadline?: string
  maxAttempts?: number
  isPublicRank?: number
}

export interface UpdateAssignmentRequest extends Partial<CreateAssignmentRequest> {
  status?: number
}

export interface CreateDiscussionRequest {
  assignmentId?: string | null
  title: string
  content: string
  isAnonymous?: number
}

export interface SubmitOjCodeRequest {
  problemId: string | undefined
  language: string
  code: string
}

export interface AssociateOjProblemsRequest {
  problemIds: string[]
}

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
  categoryId?: string | null
  timeLimit?: number
  memoryLimit?: number
  status?: number
}

export interface UpdateOjProblemRequest extends Partial<CreateOjProblemRequest> {}

export interface CreateOjTestCaseRequest {
  caseNo: number
  input: string
  expectedOutput: string
  isSample?: number
  scoreWeight?: number
  isHidden?: number
}
