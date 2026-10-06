import type { ApiResponse } from './messageTypings'

export type CollabOperationType = 'insert' | 'delete'

export interface CollabTextOperation {
  op: CollabOperationType
  position: number
  text?: string
  length?: number
}

export interface CollabDocumentVO {
  id: string | number
  conversationId: string | number
  title?: string
  content?: string
  revision: number
  createdBy?: string | number
  createdAt?: string
  updatedAt?: string
}

export interface CollabDocumentRequest {
  title: string
  content: string
}

export interface CollabOnlineUserVO {
  userId?: string | number
  clientId?: string
  cursor?: number
  selectionStart?: number
  selectionEnd?: number
}

export interface CollabOperationVO {
  id?: string | number
  documentId: string | number
  conversationId: string | number
  revision: number
  baseRevision?: number
  userId?: string | number
  clientId?: string
  requestId?: string
  operation?: CollabTextOperation
  transformedOperation?: CollabTextOperation
  createdAt?: string
}

export interface CollabRoomStateVO {
  document: CollabDocumentVO
  onlineUsers: CollabOnlineUserVO[]
  recentOperations: CollabOperationVO[]
}

export type CollabWsInboundType = 'COLLAB_JOIN' | 'COLLAB_EDIT' | 'COLLAB_CURSOR' | 'COLLAB_TYPING' | 'COLLAB_LEAVE'

export type CollabWsOutboundType =
  | 'COLLAB_JOINED'
  | 'COLLAB_USER_JOINED'
  | 'COLLAB_OPERATION_ACCEPTED'
  | 'COLLAB_OPERATION_BROADCAST'
  | 'COLLAB_CURSOR_BROADCAST'
  | 'COLLAB_TYPING_BROADCAST'
  | 'COLLAB_LEFT'
  | 'COLLAB_USER_LEFT'
  | 'COLLAB_ERROR'

export interface CollabWsMessage<TPayload = unknown> {
  type: CollabWsInboundType | CollabWsOutboundType
  requestId?: string
  conversationId?: string | number
  documentId?: string | number
  clientId?: string
  senderId?: string | number
  revision?: number
  payload?: TPayload
  timestamp?: string
}

export interface CollabJoinedPayload {
  document?: CollabDocumentVO
  onlineUsers?: CollabOnlineUserVO[]
  recentOperations?: CollabOperationVO[]
}

export interface CollabEditPayload {
  baseRevision: number
  operation: CollabTextOperation
}

export interface CollabOperationAcceptedPayload {
  revision: number
  baseRevision?: number
  operation?: CollabTextOperation
  transformedOperation?: CollabTextOperation
}

export interface CollabOperationBroadcastPayload {
  revision: number
  transformedOperation?: CollabTextOperation
}

export interface CollabCursorPayload {
  cursor: number
  selectionStart: number
  selectionEnd: number
}

export interface CollabTypingPayload {
  cursor: number
}

export type CollabApiResponse<T> = ApiResponse<T>