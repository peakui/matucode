const DEFAULT_MESSAGE_WS_URL = 'ws://localhost:9008/messages/ws'

const getStoredMessageToken = () => (
  localStorage.getItem('token')
  || localStorage.getItem('codehub-token')
  || ''
).trim().replace(/^Bearer\s+/i, '')

// Resolve the private-message WebSocket endpoint. Prefer an explicit override,
// then derive it from the API base, then from the current origin (so a deployed
// build goes through the gateway instead of a baked-in localhost address).
export const resolveMessagesWsBaseUrl = () => {
  const explicitWsUrl = import.meta.env.VITE_MESSAGE_WS_URL?.trim() || import.meta.env.VITE_MESSAGES_WS_URL?.trim()
  if (explicitWsUrl) {
    return explicitWsUrl
  }

  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim()
  if (apiBaseUrl?.startsWith('http://') || apiBaseUrl?.startsWith('https://')) {
    const url = new URL(apiBaseUrl)
    url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
    url.pathname = '/messages/ws'
    url.search = ''
    return url.toString()
  }

  if (typeof window !== 'undefined' && window.location?.host) {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    return `${protocol}//${window.location.host}/api/messages/ws`
  }

  return DEFAULT_MESSAGE_WS_URL
}

export const buildMessagesWsUrl = (token: string = getStoredMessageToken()) => {
  const baseUrl = resolveMessagesWsBaseUrl()
  const separator = baseUrl.includes('?') ? '&' : '?'
  return `${baseUrl}${separator}token=${encodeURIComponent(token || '')}`
}

export { getStoredMessageToken }
