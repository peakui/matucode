import { API_BASE_URL, REQUEST_TIMEOUT } from '../config/index.js'
import { unwrapResponse } from '../utils/json.js'
import { session, signedIn, clearSession } from '../utils/session.js'
const clean = (data) =>
  Object.fromEntries(Object.entries(data || {}).filter(([, v]) => v !== undefined && v !== null && v !== ''))
export function request(path, data = {}, method = 'GET', options = {}) {
  signedIn()
  const authorization = options.publicAuth ? '' : session.authorization
  return new Promise((resolve, reject) => {
    uni.request({
      url: API_BASE_URL.replace(/\/$/, '') + path,
      data: clean(data),
      method,
      timeout: REQUEST_TIMEOUT,
      dataType: 'text',
      responseType: 'text',
      header: {
        'Content-Type': 'application/json',
        ...(authorization ? { Authorization: authorization } : {}),
      },
      success: async (res) => {
        try {
          const data = unwrapResponse(res.statusCode, res.data)
          if (authorization && session.authorization !== authorization)
            throw new Error('账号状态已变化，请重新加载')
          resolve(data)
        } catch (error) {
          const expired =
            error.status === 401 ||
            error.code === 401 ||
            (error.status === 400 && /未登录|登录已失效|请重新登录|登录凭证无效/.test(error.message))
          if (expired && authorization) {
            if (session.authorization === authorization) {
              clearSession()
              uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
            }
            // Only replay public reads; never replay a write or private request.
            if (options.guest && method === 'GET' && !options.retried) {
              try {
                resolve(await request(path, data, method, { ...options, publicAuth: true, retried: true }))
              } catch (e) {
                reject(e)
              }
              return
            }
          }
          reject(error)
        }
      },
      fail: () => reject(new Error('网络连接失败，请检查网络后重试')),
    })
  })
}
export function uploadImage(filePath) {
  signedIn()
  const authorization = session.authorization
  if (!authorization) return Promise.reject(new Error('请先登录后上传头像'))
  return new Promise((resolve, reject) =>
    uni.uploadFile({
      url: API_BASE_URL + '/files/upload',
      filePath,
      name: 'file',
      header: { Authorization: authorization },
      formData: { bizType: 'avatar', isPublic: '1' },
      success: (res) => {
        try {
          const data = unwrapResponse(res.statusCode, res.data)
          if (authorization && session.authorization !== authorization)
            throw new Error('账号状态已变化，请重新加载')
          resolve(data)
        } catch (e) {
          if ((e.status === 401 || e.code === 401) && session.authorization === authorization) clearSession()
          reject(e)
        }
      },
      fail: () => reject(new Error('图片上传失败，请重试')),
    })
  )
}
