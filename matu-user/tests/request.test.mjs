import assert from 'node:assert/strict'
import { after, before, beforeEach, test } from 'node:test'
import { createServer } from 'vite'

let server, request
const originalStorage = Object.getOwnPropertyDescriptor(globalThis, 'localStorage')
let storage
before(async () => {
  server = await createServer({ configFile: false, server: { middlewareMode: true, watch: null, ws: false }, optimizeDeps: { noDiscovery: true, include: [] }, appType: 'custom' })
  ;({ request } = await server.ssrLoadModule('/src/api/request.ts'))
})
beforeEach(() => {
  storage = new Map([['codehub-token', 'expired-test-token']])
  Object.defineProperty(globalThis, 'localStorage', { configurable: true, value: { getItem: (key) => storage.get(key) ?? null } })
})
after(async () => {
  await server?.close()
  if (originalStorage) Object.defineProperty(globalThis, 'localStorage', originalStorage)
  else delete globalThis.localStorage
})
const adapter = (data, inspect = () => {}) => async (config) => {
  inspect(config)
  return { data: JSON.stringify(data), status: 200, statusText: 'OK', headers: {}, config }
}

test('登录请求不携带过期凭据，业务请求携带登录凭据', async () => {
  await request.post('/auth/login', {}, { adapter: adapter({ code: 0, data: {} }, (config) => assert.equal(config.headers.Authorization, undefined)) })
  await request.get('/courses', { adapter: adapter({ code: 0, data: {} }, (config) => assert.equal(config.headers.Authorization, 'Bearer expired-test-token')) })
})
test('HTTP 200 业务失败必须拒绝，并保留错误响应供页面展示', async () => {
  await assert.rejects(request.post('/courses/1/certificate', {}, { adapter: adapter({ code: 403, message: '请先完成课程', data: null }) }), (error) => {
    assert.equal(error.message, '请先完成课程')
    assert.equal(error.response.data.code, 403)
    return true
  })
})
test('成功响应保持 Axios 协议及大整数 ID 精度', async () => {
  const response = await request.get('/feedbacks/1', { adapter: async (config) => ({
    data: '{"code":0,"data":{"id":2106721929913106434,"content":"已处理"}}',
    status: 200, statusText: 'OK', headers: {}, config,
  }) })
  assert.equal(response.data.data.id, '2106721929913106434')
  assert.equal(response.data.data.content, '已处理')
})
test('HTTP 200 中的登录过期业务响应也必须拒绝', async () => {
  await assert.rejects(request.get('/auth/me', { adapter: adapter({ code: 401, message: '登录已失效', data: null }) }), /登录已失效/)
})
