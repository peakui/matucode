const COLLAB_INVITE_MESSAGE_TYPE = 'CODEHUB_COLLAB_INVITE'

type CollabInviteMessagePayload = {
  type: typeof COLLAB_INVITE_MESSAGE_TYPE
  title: string
  url: string
  conversationId?: string | number
  documentId?: string | number
}

const buildCollabInviteMessage = (payload: Omit<CollabInviteMessagePayload, 'type'>) => JSON.stringify({
  type: COLLAB_INVITE_MESSAGE_TYPE,
  ...payload,
})

const parseCollabInviteMessage = (content?: string | null): CollabInviteMessagePayload | null => {
  if (!content) {
    return null
  }

  try {
    const parsed = JSON.parse(content) as Partial<CollabInviteMessagePayload>
    if (parsed.type !== COLLAB_INVITE_MESSAGE_TYPE || !parsed.url) {
      return null
    }

    return {
      type: COLLAB_INVITE_MESSAGE_TYPE,
      title: parsed.title?.trim() || '协同编辑邀请',
      url: parsed.url,
      conversationId: parsed.conversationId,
      documentId: parsed.documentId,
    }
  } catch {
    return null
  }
}

const getCollabInviteNavigateTarget = (url: string) => {
  try {
    const parsedUrl = new URL(url, window.location.origin)
    if (parsedUrl.origin === window.location.origin) {
      return `${parsedUrl.pathname}${parsedUrl.search}${parsedUrl.hash}`
    }
    return parsedUrl.toString()
  } catch {
    return url
  }
}

const getCollabInvitePreviewText = (content?: string | null) => {
  const invite = parseCollabInviteMessage(content)
  return invite ? `协同编辑邀请：${invite.title}` : ''
}

export {
  buildCollabInviteMessage,
  getCollabInviteNavigateTarget,
  getCollabInvitePreviewText,
  parseCollabInviteMessage,
}
export type { CollabInviteMessagePayload }