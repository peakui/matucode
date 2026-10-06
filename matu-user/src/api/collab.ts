import type { AxiosResponse } from 'axios'
import { request } from './request'
import type {
  CollabApiResponse,
  CollabDocumentRequest,
  CollabDocumentVO,
  CollabOperationVO,
  CollabRoomStateVO,
} from './type/collabTypings'

async function createOrGetCollabDocument(conversationId: string | number, data: CollabDocumentRequest): Promise<CollabDocumentVO> {
  const response: AxiosResponse<CollabApiResponse<CollabDocumentVO>> = await request.post(
    `/messages/collab/conversations/${String(conversationId)}/document`,
    data,
  )
  return response.data.data
}

async function getConversationCollabDocument(conversationId: string | number): Promise<CollabDocumentVO> {
  const response: AxiosResponse<CollabApiResponse<CollabDocumentVO>> = await request.get(
    `/messages/collab/conversations/${String(conversationId)}/document`,
  )
  return response.data.data
}

async function getCollabDocument(documentId: string | number): Promise<CollabDocumentVO> {
  const response: AxiosResponse<CollabApiResponse<CollabDocumentVO>> = await request.get(`/messages/collab/documents/${String(documentId)}`)
  return response.data.data
}

async function listCollabOperations(
  documentId: string | number,
  params: { afterRevision?: number; limit?: number } = {},
): Promise<CollabOperationVO[]> {
  const response: AxiosResponse<CollabApiResponse<CollabOperationVO[] | null>> = await request.get(
    `/messages/collab/documents/${String(documentId)}/operations`,
    { params },
  )
  return response.data.data || []
}

async function getCollabRoomState(documentId: string | number): Promise<CollabRoomStateVO> {
  const response: AxiosResponse<CollabApiResponse<CollabRoomStateVO>> = await request.get(
    `/messages/collab/documents/${String(documentId)}/room-state`,
  )
  return response.data.data
}

export {
  createOrGetCollabDocument,
  getCollabDocument,
  getCollabRoomState,
  getConversationCollabDocument,
  listCollabOperations,
}