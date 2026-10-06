import axios, { AxiosError } from 'axios'

const apiBaseURL = import.meta.env.VITE_API_BASE_URL?.trim() || '/api'
const apiTimeout = Number(import.meta.env.VITE_API_TIMEOUT || 10000)
const bigIntFieldPattern = /"((?:[^"\\]|\\.)*)":\s*(-?\d{16,})/g

// The gateway rejects any request carrying an expired token with 401, and
// downstream services report a missing identity as HTTP 400 with these texts.
// Both mean the stored session can no longer be used.
const SESSION_INVALID_HINTS = ['未登录', '登录已失效', '请重新登录', '登录凭证无效']
let sessionInvalidHandled = false

const isSessionInvalid = (error: AxiosError) => {
  const status = error.response?.status
  if (status === 401) {
    return true
  }
  if (status !== 400) {
    return false
  }
  const data = error.response?.data
  if (!data || typeof data !== 'object') {
    return false
  }
  const body = data as { code?: number; message?: string; msg?: string }
  if (body.code === 401) {
    return true
  }
  const message = String(body.message ?? body.msg ?? '')
  return SESSION_INVALID_HINTS.some((hint) => message.includes(hint))
}

// An expired token breaks even public content requests, so drop the session and
// reload once as a guest: the header flips back to "登录" and content loads again.
const handleSessionInvalid = () => {
  if (typeof window === 'undefined' || sessionInvalidHandled) {
    return
  }
  const hasToken = Boolean(localStorage.getItem('codehub-token') || localStorage.getItem('token'))
  if (!hasToken) {
    return
  }
  sessionInvalidHandled = true
  void import('../store').then(({ store }) =>
    import('../store/modules/authSlice').then(({ logout }) => {
      store.dispatch(logout())
      window.location.reload()
    }),
  )
}

const normalizeLargeIntegerFields = (value: string) => {
  return value.replace(bigIntFieldPattern, '"$1":"$2"')
}

const parseJsonSafely = (payload: string) => {
  try {
    return JSON.parse(payload)
  } catch {
    return payload
  }
}

const transformBigIntJson = (payload: unknown) => {
  if (typeof payload !== 'string') {
    return payload
  }

  const trimmedPayload = payload.trim()
  if (!trimmedPayload) {
    return payload
  }

  const normalizedPayload = normalizeLargeIntegerFields(trimmedPayload)
  return parseJsonSafely(normalizedPayload)
}

export const request = axios.create({
  baseURL: apiBaseURL,
  timeout: apiTimeout,
  transformResponse: [transformBigIntJson],
})

export const getAuthorizationHeader = () => {
  const storedAuthorization = localStorage.getItem('codehub-authorization')?.trim()
  const rawToken = localStorage.getItem('codehub-token')?.trim().replace(/^Bearer\s+/i, '')

  if (storedAuthorization) {
    return storedAuthorization
  }

  if (!rawToken) {
    return ''
  }

  return `Bearer ${rawToken}`
}

// The gateway rejects an expired token before the request reaches the auth
// service, so a stale token would block logging in again. Never send it here.
const AUTH_ENTRY_PATHS = ['/auth/login', '/auth/register', '/auth/mini/login']

request.interceptors.request.use(
  (config) => {
    const authorization = getAuthorizationHeader()
    const isAuthEntry = AUTH_ENTRY_PATHS.some((path) => (config.url ?? '').startsWith(path))

    if (authorization && !isAuthEntry) {
      config.headers.Authorization = authorization
    }

    config.headers['X-Requested-With'] = 'XMLHttpRequest'

    if (import.meta.env.DEV) {
      config.headers['X-Client-Env'] = 'development'
    }

    return config
  },
  (error) => Promise.reject(error),
)

request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body === 'object' && typeof body.code === 'number'
      && ![0, 200, 20000].includes(body.code)) {
      if (body.code === 401) handleSessionInvalid()
      return Promise.reject(new AxiosError(body.message || body.msg || '请求失败', 'ERR_BAD_RESPONSE', response.config, response.request, response))
    }
    return response
  },
  (error) => {
    if (axios.isCancel(error) || error.code === 'ERR_CANCELED') {
      console.warn('[api][canceled]')
      return Promise.reject(error)
    }

    if (import.meta.env.DEV) {
      console.error('[api][error]', error.response?.status)
    }

    if (isSessionInvalid(error)) {
      console.warn('登录状态已失效，请重新登录。')
      handleSessionInvalid()
    }

    return Promise.reject(error)
  },
)
