import type {
  CollabCursorPayload,
  CollabEditPayload,
  CollabTextOperation,
  CollabTypingPayload,
  CollabWsInboundType,
  CollabWsMessage,
  CollabWsOutboundType,
} from '../api/type/collabTypings'
import { buildMessagesWsUrl } from './messagesWebSocketUrl'

type CollabWsHandler<TPayload = unknown> = (message: CollabWsMessage<TPayload>) => void

const buildRequestId = (prefix: string) => `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2)}`

const getCollabWsUrl = () => buildMessagesWsUrl()

const clampPosition = (position: number, content: string) => Math.max(0, Math.min(position, content.length))

const clampLength = (position: number, length: number, content: string) => Math.max(0, Math.min(length, content.length - position))

const applyCollabOperation = (content: string, operation?: CollabTextOperation) => {
  if (!operation) {
    return content
  }

  const position = clampPosition(operation.position, content)

  if (operation.op === 'insert') {
    return `${content.slice(0, position)}${operation.text || ''}${content.slice(position)}`
  }

  const length = clampLength(position, operation.length || 0, content)
  return `${content.slice(0, position)}${content.slice(position + length)}`
}

const buildCollabTextOperations = (previousContent: string, nextContent: string): CollabTextOperation[] => {
  if (previousContent === nextContent) {
    return []
  }

  let prefixLength = 0
  const shortestLength = Math.min(previousContent.length, nextContent.length)

  while (prefixLength < shortestLength && previousContent[prefixLength] === nextContent[prefixLength]) {
    prefixLength += 1
  }

  let previousSuffixIndex = previousContent.length - 1
  let nextSuffixIndex = nextContent.length - 1

  while (
    previousSuffixIndex >= prefixLength
    && nextSuffixIndex >= prefixLength
    && previousContent[previousSuffixIndex] === nextContent[nextSuffixIndex]
  ) {
    previousSuffixIndex -= 1
    nextSuffixIndex -= 1
  }

  const deletedLength = previousSuffixIndex - prefixLength + 1
  const insertedText = nextContent.slice(prefixLength, nextSuffixIndex + 1)
  const operations: CollabTextOperation[] = []

  if (deletedLength > 0) {
    operations.push({ op: 'delete', position: prefixLength, length: deletedLength })
  }

  if (insertedText) {
    operations.push({ op: 'insert', position: prefixLength, text: insertedText })
  }

  return operations
}

const createCollabClientId = () => {
  const storedClientId = sessionStorage.getItem('codehub-collab-client-id')
  if (storedClientId) {
    return storedClientId
  }

  const clientId = `web-${crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random().toString(16).slice(2)}`}`
  sessionStorage.setItem('codehub-collab-client-id', clientId)
  return clientId
}

class CollabEditWebSocket {
  private conversationId: string | number
  private documentId: string | number
  private clientId: string
  private ws: WebSocket | null = null
  private heartbeatTimer: number | null = null
  private handlers: Partial<Record<CollabWsOutboundType, Set<CollabWsHandler>>> = {}

  constructor(conversationId: string | number, documentId: string | number, clientId: string) {
    this.conversationId = conversationId
    this.documentId = documentId
    this.clientId = clientId
  }

  connect() {
    this.disconnect(false)
    const ws = new WebSocket(getCollabWsUrl())
    this.ws = ws

    ws.onopen = () => {
      this.startHeartbeat()
      this.join()
    }

    ws.onmessage = (event) => {
      if (event.data === 'pong') {
        return
      }

      try {
        const parsed = JSON.parse(event.data) as CollabWsMessage | { data?: CollabWsMessage }
        const message = 'type' in parsed ? parsed : parsed.data
        if (!message?.type) {
          return
        }
        this.emit(message.type as CollabWsOutboundType, message)
      } catch (error) {
        console.error('parse collab websocket message error:', error)
      }
    }

    ws.onerror = () => {
      ws.close()
    }

    ws.onclose = () => {
      this.stopHeartbeat()
      this.ws = null
    }
  }

  disconnect(sendLeave = true) {
    this.stopHeartbeat()

    if (sendLeave && this.ws?.readyState === WebSocket.OPEN) {
      this.leave()
    }

    this.ws?.close()
    this.ws = null
  }

  on<TPayload = unknown>(type: CollabWsOutboundType, handler: CollabWsHandler<TPayload>) {
    if (!this.handlers[type]) {
      this.handlers[type] = new Set()
    }

    this.handlers[type]?.add(handler as CollabWsHandler)

    return () => {
      this.handlers[type]?.delete(handler as CollabWsHandler)
    }
  }

  send(type: CollabWsInboundType, payload: unknown = {}, requestId = buildRequestId(type.toLowerCase())) {
    if (this.ws?.readyState !== WebSocket.OPEN) {
      return false
    }

    this.ws.send(JSON.stringify({
      type,
      requestId,
      conversationId: this.conversationId,
      documentId: this.documentId,
      clientId: this.clientId,
      payload,
    }))

    return true
  }

  join() {
    return this.send('COLLAB_JOIN', {}, buildRequestId('collab-join'))
  }

  edit(payload: CollabEditPayload) {
    return this.send('COLLAB_EDIT', payload, buildRequestId('collab-edit'))
  }

  cursor(payload: CollabCursorPayload) {
    return this.send('COLLAB_CURSOR', payload, buildRequestId('collab-cursor'))
  }

  typing(payload: CollabTypingPayload) {
    return this.send('COLLAB_TYPING', payload, buildRequestId('collab-typing'))
  }

  leave() {
    return this.send('COLLAB_LEAVE', {}, buildRequestId('collab-leave'))
  }

  private emit(type: CollabWsOutboundType, message: CollabWsMessage) {
    this.handlers[type]?.forEach((handler) => handler(message))
  }

  private startHeartbeat() {
    this.stopHeartbeat()
    this.heartbeatTimer = window.setInterval(() => {
      if (this.ws?.readyState === WebSocket.OPEN) {
        this.ws.send('ping')
      }
    }, 25000)
  }

  private stopHeartbeat() {
    if (this.heartbeatTimer != null) {
      window.clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }
}

export { applyCollabOperation, buildCollabTextOperations, CollabEditWebSocket, createCollabClientId }