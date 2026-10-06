export interface QaQuestionListParams {
  categoryId?: string
  status?: number
  keyword?: string
  pageNum?: number
  pageSize?: number
  sortBy?: string
}

export interface QaQuestionListItemVO {
  id: string
  userId: string
  categoryId?: string
  title: string
  content?: string
  bountyPoints?: number
  viewCount: number
  answerCount: number
  followCount: number
  status: number
  bestAnswerId?: string | null
  createdAt?: string
  updatedAt?: string
  solvedAt?: string | null
}

export interface QaAnswerVO {
  id: string
  questionId: string
  userId: string
  content: string
  likeCount: number
  dislikeCount: number
  isAccepted: number
  acceptedAt?: string | null
  status: number
  createdAt?: string
  updatedAt?: string
}

export interface QaCommentVO {
  id: string
  targetType: number
  targetId: string
  userId: string
  parentId?: string | null
  content: string
  likeCount: number
  status: number
  createdAt?: string
  children?: QaCommentVO[]
}

export interface QaCategoryVO {
  id: string
  parentId?: string | null
  categoryName: string
  categoryDesc?: string
  iconUrl?: string
  sortOrder?: number
  questionCount?: number
  status: number
  createdAt?: string
  children?: QaCategoryVO[]
}

export interface QaWordCloudVO {
  id: string
  questionId: string
  wordData?: Array<{ word: string; count: number }>
  imageUrl?: string
  generatedAt?: string
  status?: number
}

export interface QaQuestionDetailVO extends QaQuestionListItemVO {
  category?: QaCategoryVO
  answers?: QaAnswerVO[]
  comments?: QaCommentVO[]
}

export interface CreateQuestionRequest {
  categoryId?: string
  title: string
  content: string
  bountyPoints?: number
}

export interface UpdateQuestionRequest {
  categoryId?: string
  title?: string
  content?: string
  bountyPoints?: number
  status?: number
}

export interface CreateAnswerRequest {
  content: string
}

export interface UpdateAnswerRequest {
  content?: string
  status?: number
}

export interface CreateQaCommentRequest {
  targetType: number | undefined
  targetId: string | undefined
  parentId?: string | null
  content: string
}
