import { request } from '@/utils/request'
import type {
  AssignmentDetailVO,
  AssignmentRankingVO,
  AssignmentSubmissionListParams,
  ClassAssignmentVO,
  ClassDiscussionVO,
  ClassListParams,
  ClassMemberVO,
  ClassSubmissionVO,
  ClassVO,
  CreateAssignmentRequest,
  CreateClassRequest,
  CreateDiscussionRequest,
  AssociateOjProblemsRequest,
  CreateOjProblemRequest,
  CreateOjTestCaseRequest,
  JoinClassRequest,
  OjProblemListParams,
  OjProblemVO,
  OjSubmissionDetailVO,
  OjSubmissionListParams,
  OjSubmissionVO,
  OjTestCaseVO,
  PageResponse,
  SubmitOjCodeRequest,
  UpdateAssignmentRequest,
  UpdateClassRequest,
  UpdateOjProblemRequest,
} from '@/api/types'

export const getClassListApi = (params: ClassListParams) =>
  request<PageResponse<ClassVO>>({ url: '/oj/classes', method: 'get', params })
export const getClassDetailApi = (classId: string) =>
  request<ClassVO>({ url: `/oj/classes/${classId}`, method: 'get' })
export const createClassApi = (data: CreateClassRequest) =>
  request<ClassVO>({ url: '/oj/classes', method: 'post', data })
export const updateClassApi = (classId: string, data: UpdateClassRequest) =>
  request<ClassVO>({ url: `/oj/classes/${classId}`, method: 'put', data })
export const joinClassApi = (classId: string, data: JoinClassRequest) =>
  request<null>({ url: `/oj/classes/${classId}/join`, method: 'post', data })
export const approveClassMemberApi = (classId: string, memberId: string) =>
  request<null>({ url: `/oj/classes/${classId}/members/${memberId}/approve`, method: 'post' })
export const removeClassMemberApi = (classId: string, memberId: string) =>
  request<null>({ url: `/oj/classes/${classId}/members/${memberId}`, method: 'delete' })
export const getClassMemberListApi = (classId: string) =>
  request<ClassMemberVO[]>({ url: `/oj/classes/${classId}/members`, method: 'get' })
export const createAssignmentApi = (classId: string, data: CreateAssignmentRequest) =>
  request<ClassAssignmentVO>({ url: `/oj/classes/${classId}/assignments`, method: 'post', data })
export const updateAssignmentApi = (
  classId: string,
  assignmentId: string,
  data: UpdateAssignmentRequest,
) =>
  request<ClassAssignmentVO>({
    url: `/oj/classes/${classId}/assignments/${assignmentId}`,
    method: 'put',
    data,
  })
export const getAssignmentListApi = (classId: string) =>
  request<ClassAssignmentVO[]>({ url: `/oj/classes/${classId}/assignments`, method: 'get' })
export const getAssignmentDetailApi = (classId: string, assignmentId: string) =>
  request<AssignmentDetailVO>({
    url: `/oj/classes/${classId}/assignments/${assignmentId}`,
    method: 'get',
  })
export const getAssignmentRankingApi = (classId: string, assignmentId: string) =>
  request<AssignmentRankingVO>({
    url: `/oj/classes/${classId}/assignments/${assignmentId}/ranking`,
    method: 'get',
  })
export const getAssignmentSubmissionListApi = (
  classId: string,
  assignmentId: string,
  params: AssignmentSubmissionListParams,
) =>
  request<PageResponse<ClassSubmissionVO>>({
    url: `/oj/classes/${classId}/assignments/${assignmentId}/submissions`,
    method: 'get',
    params,
  })
export const createDiscussionApi = (classId: string, data: CreateDiscussionRequest) =>
  request<ClassDiscussionVO>({ url: `/oj/classes/${classId}/discussions`, method: 'post', data })
export const getDiscussionListApi = (classId: string, assignmentId?: string) =>
  request<ClassDiscussionVO[]>({
    url: `/oj/classes/${classId}/discussions`,
    method: 'get',
    params: { assignmentId },
  })
export const submitOjCodeApi = (data: SubmitOjCodeRequest) =>
  request<OjSubmissionVO>({ url: '/oj/classes/submissions', method: 'post', data })
export const createOjProblemApi = (data: CreateOjProblemRequest) =>
  request<OjProblemVO>({ url: '/oj/classes/problems', method: 'post', data })
export const updateOjProblemApi = (problemId: string, data: UpdateOjProblemRequest) =>
  request<OjProblemVO>({ url: `/oj/classes/problems/${problemId}`, method: 'put', data })
export const getOjProblemDetailApi = (problemId: string) =>
  request<OjProblemVO>({ url: `/oj/classes/problems/${problemId}`, method: 'get' })
export const getOjProblemListApi = (params: OjProblemListParams) =>
  request<PageResponse<OjProblemVO>>({ url: '/oj/classes/problems', method: 'get', params })
export const getClassProblemListApi = (classId: string, params: OjProblemListParams) =>
  request<PageResponse<OjProblemVO>>({
    url: `/oj/classes/${classId}/problems`,
    method: 'get',
    params,
  })
export const getClassProblemDetailApi = (classId: string, problemId: string) =>
  request<OjProblemVO>({ url: `/oj/classes/${classId}/problems/${problemId}`, method: 'get' })
export const createClassProblemApi = (classId: string, data: CreateOjProblemRequest) =>
  request<OjProblemVO>({ url: `/oj/classes/${classId}/problems`, method: 'post', data })
export const updateClassProblemApi = (
  classId: string,
  problemId: string,
  data: UpdateOjProblemRequest,
) => request<OjProblemVO>({ url: `/oj/classes/${classId}/problems/${problemId}`, method: 'put', data })
export const associateClassProblemsApi = (classId: string, data: AssociateOjProblemsRequest) =>
  request<null>({ url: `/oj/classes/${classId}/problems/associate`, method: 'post', data })
export const removeClassProblemApi = (classId: string, problemId: string) =>
  request<null>({ url: `/oj/classes/${classId}/problems/${problemId}`, method: 'delete' })
export const createOjTestCaseApi = (problemId: string, data: CreateOjTestCaseRequest) =>
  request<OjTestCaseVO>({
    url: `/oj/classes/problems/${problemId}/test-cases`,
    method: 'post',
    data,
  })
export const getOjTestCaseListApi = (problemId: string) =>
  request<OjTestCaseVO[]>({ url: `/oj/classes/problems/${problemId}/test-cases`, method: 'get' })
export const getOjSubmissionListApi = (params: OjSubmissionListParams) =>
  request<PageResponse<OjSubmissionVO>>({ url: '/oj/classes/submissions', method: 'get', params })
export const getOjSubmissionDetailApi = (submissionId: string) =>
  request<OjSubmissionVO>({ url: `/oj/classes/submissions/${submissionId}`, method: 'get' })
export const getOjSubmissionCaseDetailsApi = (submissionId: string) =>
  request<OjSubmissionDetailVO[]>({
    url: `/oj/classes/submissions/${submissionId}/details`,
    method: 'get',
  })
