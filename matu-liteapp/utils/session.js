import { reactive } from 'vue'
const KEY = 'matu-mini-session'
const stored = uni.getStorageSync(KEY)
export const session = reactive({
  authorization: stored?.authorization || '',
  user: stored?.user || null,
  expiresAt: stored?.expiresAt || 0,
  revision: 0,
})
export function clearSession() {
  session.authorization = ''
  session.user = null
  session.expiresAt = 0
  session.revision++
  uni.removeStorageSync(KEY)
  uni.removeStorageSync('matu-interview-context')
}
export function signedIn() {
  if (session.expiresAt && session.expiresAt <= Date.now()) clearSession()
  return !!session.authorization
}
export function saveSession(data) {
  if (!data?.authorization || !data?.user?.userId) throw new Error('登录响应缺少会话信息')
  session.revision++
  session.authorization = data.authorization
  session.user = data.user
  session.expiresAt = data.expiresIn > 0 ? Date.now() + data.expiresIn * 1000 : 0
  persist()
}
export function updateUser(data) {
  session.user = { ...session.user, ...data }
  persist()
}
function persist() {
  uni.setStorageSync(KEY, {
    authorization: session.authorization,
    user: session.user,
    expiresAt: session.expiresAt,
  })
}
export function currentUrl() {
  const pages = getCurrentPages()
  const page = pages[pages.length - 1]
  if (!page) return '/pages/me/index'
  const query = Object.entries(page.options || {})
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
    .join('&')
  return `/${page.route}${query ? '?' + query : ''}`
}
let opening = false
export function requireLogin(returnTo = currentUrl()) {
  if (signedIn()) return true
  if (!opening) {
    opening = true
    uni.navigateTo({
      url: '/pages/login/index?returnTo=' + encodeURIComponent(returnTo),
      complete: () => {
        setTimeout(() => {
          opening = false
        }, 400)
      },
    })
  }
  return false
}
export function finishLogin(returnTo) {
  for (let i = 0; i < 2 && /^%2f/i.test(returnTo || ''); i++) {
    try {
      returnTo = decodeURIComponent(returnTo)
    } catch {
      break
    }
  }
  const pages = getCurrentPages()
  const target = /^\/pages\/[\w/-]+(?:\?.*)?$/.test(returnTo || '') ? returnTo : '/pages/me/index'
  const targetPath = target.split('?')[0]
  if (pages.length > 1 && '/' + pages[pages.length - 2].route === targetPath) {
    uni.navigateBack()
    return
  }
  if (
    ['/pages/index/index', '/pages/tutorials/index', '/pages/interviews/index', '/pages/me/index'].includes(
      targetPath
    )
  )
    uni.switchTab({ url: targetPath })
  else uni.redirectTo({ url: target })
}
