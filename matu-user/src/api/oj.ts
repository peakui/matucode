import type { AxiosResponse } from 'axios'
import { request } from './request'
import type {
  AssignmentDetailVO,
  AssignmentRankingVO,
  AssociateOjProblemsRequest,
  ClassAssignmentVO,
  ClassDiscussionVO,
  ClassMemberVO,
  ClassSubmissionVO,
  ClassVO,
  CreateOjProblemRequest,
  CreateOjTestCaseRequest,
  JoinClassRequest,
  ListAssignmentSubmissionsParams,
  ListOjClassProblemsParams,
  ListOjClassesParams,
  ListOjProblemsParams,
  ListOjSubmissionsParams,
  OjApiResponse,
  OjClassRankingVO,
  OjPageResponse,
  OjProblemVO,
  OjSolvedCountStatisticsVO,
  OjSubmissionDetailVO,
  OjSubmissionVO,
  OjTestCaseVO,
  ReviewOjClassMemberRequest,
  SubmitOjCodeRequest,
  UpdateOjProblemRequest,
} from './type/ojTypings'

const OJ_CLASS_PREFIX = '/oj/classes'

async function listOjClasses(params: ListOjClassesParams = {}): Promise<OjPageResponse<ClassVO>> {
  const response: AxiosResponse<OjApiResponse<OjPageResponse<ClassVO> | null>> = await request.get(OJ_CLASS_PREFIX, { params })
  return response.data.data || {
    pageNum: Number(params.pageNum || 1),
    pageSize: Number(params.pageSize || 10),
    total: 0,
    totalPages: 0,
    records: [],
  }
}

async function getOjClassDetail(classId: string | number): Promise<ClassVO> {
  const response: AxiosResponse<OjApiResponse<ClassVO>> = await request.get(`${OJ_CLASS_PREFIX}/${String(classId)}`)
  return response.data.data
}

async function joinOjClass(classId: string | number, data: JoinClassRequest = {}): Promise<void> {
  await request.post(`${OJ_CLASS_PREFIX}/${String(classId)}/join`, data)
}

async function listOjClassMembers(classId: string | number): Promise<ClassMemberVO[]> {
  const response: AxiosResponse<OjApiResponse<ClassMemberVO[] | null>> = await request.get(`${OJ_CLASS_PREFIX}/${String(classId)}/members`)
  return response.data.data || []
}

async function approveOjClassMember(classId: string | number, memberId: string | number, data: ReviewOjClassMemberRequest = {}): Promise<void> {
  await request.post(`${OJ_CLASS_PREFIX}/${String(classId)}/members/${String(memberId)}/approve`, data)
}

async function rejectOjClassMember(classId: string | number, memberId: string | number, data: ReviewOjClassMemberRequest = {}): Promise<void> {
  await request.post(`${OJ_CLASS_PREFIX}/${String(classId)}/members/${String(memberId)}/reject`, data)
}

async function listOjAssignments(classId: string | number): Promise<ClassAssignmentVO[]> {
  const response: AxiosResponse<OjApiResponse<ClassAssignmentVO[] | null>> = await request.get(`${OJ_CLASS_PREFIX}/${String(classId)}/assignments`)
  return response.data.data || []
}

async function getOjAssignmentDetail(classId: string | number, assignmentId: string | number): Promise<AssignmentDetailVO> {
  const response: AxiosResponse<OjApiResponse<AssignmentDetailVO>> = await request.get(
    `${OJ_CLASS_PREFIX}/${String(classId)}/assignments/${String(assignmentId)}`,
  )
  return response.data.data
}

async function getOjAssignmentRanking(classId: string | number, assignmentId: string | number): Promise<AssignmentRankingVO> {
  const response: AxiosResponse<OjApiResponse<AssignmentRankingVO>> = await request.get(
    `${OJ_CLASS_PREFIX}/${String(classId)}/assignments/${String(assignmentId)}/ranking`,
  )
  return response.data.data
}

async function listOjAssignmentSubmissions(
  classId: string | number,
  assignmentId: string | number,
  params: ListAssignmentSubmissionsParams = {},
): Promise<OjPageResponse<ClassSubmissionVO>> {
  const response: AxiosResponse<OjApiResponse<OjPageResponse<ClassSubmissionVO> | null>> = await request.get(
    `${OJ_CLASS_PREFIX}/${String(classId)}/assignments/${String(assignmentId)}/submissions`,
    { params },
  )
  return response.data.data || {
    pageNum: Number(params.pageNum || 1),
    pageSize: Number(params.pageSize || 20),
    total: 0,
    totalPages: 0,
    records: [],
  }
}

async function listOjDiscussions(classId: string | number, assignmentId?: string | number): Promise<ClassDiscussionVO[]> {
  const response: AxiosResponse<OjApiResponse<ClassDiscussionVO[] | null>> = await request.get(`${OJ_CLASS_PREFIX}/${String(classId)}/discussions`, {
    params: { assignmentId },
  })
  return response.data.data || []
}

async function listOjProblems(params: ListOjProblemsParams = {}): Promise<OjPageResponse<OjProblemVO>> {
  const response: AxiosResponse<OjApiResponse<OjPageResponse<OjProblemVO> | null>> = await request.get(`${OJ_CLASS_PREFIX}/problems`, { params })
  return response.data.data || {
    pageNum: Number(params.pageNum || 1),
    pageSize: Number(params.pageSize || 10),
    total: 0,
    totalPages: 0,
    records: [],
  }
}

async function getOjProblemDetail(problemId: string | number): Promise<OjProblemVO> {
  const response: AxiosResponse<OjApiResponse<OjProblemVO>> = await request.get(`${OJ_CLASS_PREFIX}/problems/${String(problemId)}`)
  return response.data.data
}

async function listOjClassProblems(classId: string | number, params: ListOjClassProblemsParams = {}): Promise<OjPageResponse<OjProblemVO>> {
  const response: AxiosResponse<OjApiResponse<OjPageResponse<OjProblemVO> | null>> = await request.get(`${OJ_CLASS_PREFIX}/${String(classId)}/problems`, { params })
  return response.data.data || {
    pageNum: Number(params.pageNum || 1),
    pageSize: Number(params.pageSize || 10),
    total: 0,
    totalPages: 0,
    records: [],
  }
}

async function getOjClassProblemDetail(classId: string | number, problemId: string | number): Promise<OjProblemVO> {
  const response: AxiosResponse<OjApiResponse<OjProblemVO>> = await request.get(`${OJ_CLASS_PREFIX}/${String(classId)}/problems/${String(problemId)}`)
  return response.data.data
}

async function getOjClassRanking(classId: string | number): Promise<OjClassRankingVO> {
  const response: AxiosResponse<OjApiResponse<OjClassRankingVO>> = await request.get(`${OJ_CLASS_PREFIX}/${String(classId)}/ranking`)
  return response.data.data
}

async function createOjClassProblem(classId: string | number, data: CreateOjProblemRequest): Promise<OjProblemVO> {
  const response: AxiosResponse<OjApiResponse<OjProblemVO>> = await request.post(`${OJ_CLASS_PREFIX}/${String(classId)}/problems`, data)
  return response.data.data
}

async function updateOjClassProblem(classId: string | number, problemId: string | number, data: UpdateOjProblemRequest): Promise<OjProblemVO> {
  const response: AxiosResponse<OjApiResponse<OjProblemVO>> = await request.put(`${OJ_CLASS_PREFIX}/${String(classId)}/problems/${String(problemId)}`, data)
  return response.data.data
}

async function associateOjClassProblems(classId: string | number, data: AssociateOjProblemsRequest): Promise<void> {
  await request.post(`${OJ_CLASS_PREFIX}/${String(classId)}/problems/associate`, data)
}

async function removeOjClassProblem(classId: string | number, problemId: string | number): Promise<void> {
  await request.delete(`${OJ_CLASS_PREFIX}/${String(classId)}/problems/${String(problemId)}`)
}

async function listOjProblemTestCases(problemId: string | number): Promise<OjTestCaseVO[]> {
  const response: AxiosResponse<OjApiResponse<OjTestCaseVO[] | null>> = await request.get(`${OJ_CLASS_PREFIX}/problems/${String(problemId)}/test-cases`)
  return response.data.data || []
}

async function createOjProblemTestCase(problemId: string | number, data: CreateOjTestCaseRequest): Promise<OjTestCaseVO> {
  const response: AxiosResponse<OjApiResponse<OjTestCaseVO>> = await request.post(`${OJ_CLASS_PREFIX}/problems/${String(problemId)}/test-cases`, data)
  return response.data.data
}

async function getOjSolvedCountStatistics(userId?: string | number): Promise<OjSolvedCountStatisticsVO> {
  const response: AxiosResponse<OjApiResponse<OjSolvedCountStatisticsVO | null>> = await request.get(`${OJ_CLASS_PREFIX}/statistics/solved-count`, {
    params: userId ? { userId } : undefined,
  })
  return response.data.data || {}
}

async function submitOjCode(data: SubmitOjCodeRequest): Promise<OjSubmissionVO> {
  const response: AxiosResponse<OjApiResponse<OjSubmissionVO>> = await request.post(`${OJ_CLASS_PREFIX}/submissions`, data)
  return response.data.data
}

async function listOjSubmissions(params: ListOjSubmissionsParams = {}): Promise<OjPageResponse<OjSubmissionVO>> {
  const response: AxiosResponse<OjApiResponse<OjPageResponse<OjSubmissionVO> | null>> = await request.get(`${OJ_CLASS_PREFIX}/submissions`, { params })
  return response.data.data || {
    pageNum: Number(params.pageNum || 1),
    pageSize: Number(params.pageSize || 20),
    total: 0,
    totalPages: 0,
    records: [],
  }
}

async function getOjSubmissionDetail(submissionId: string | number): Promise<OjSubmissionVO> {
  const response: AxiosResponse<OjApiResponse<OjSubmissionVO>> = await request.get(`${OJ_CLASS_PREFIX}/submissions/${String(submissionId)}`)
  return response.data.data
}

async function listOjSubmissionDetails(submissionId: string | number): Promise<OjSubmissionDetailVO[]> {
  const response: AxiosResponse<OjApiResponse<OjSubmissionDetailVO[] | null>> = await request.get(`${OJ_CLASS_PREFIX}/submissions/${String(submissionId)}/details`)
  return response.data.data || []
}

export {
  approveOjClassMember,
  associateOjClassProblems,
  createOjClassProblem,
  createOjProblemTestCase,
  getOjAssignmentDetail,
  getOjAssignmentRanking,
  getOjClassDetail,
  getOjClassProblemDetail,
  getOjClassRanking,
  getOjProblemDetail,
  getOjSolvedCountStatistics,
  getOjSubmissionDetail,
  joinOjClass,
  listOjAssignmentSubmissions,
  listOjAssignments,
  listOjClassMembers,
  listOjClassProblems,
  listOjClasses,
  listOjDiscussions,
  listOjProblemTestCases,
  listOjProblems,
  listOjSubmissionDetails,
  listOjSubmissions,
  rejectOjClassMember,
  removeOjClassProblem,
  submitOjCode,
  updateOjClassProblem,
}
