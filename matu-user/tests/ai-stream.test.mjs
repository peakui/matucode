import assert from 'node:assert/strict'
import { after, afterEach, before, beforeEach, test } from 'node:test'
import { createServer } from 'vite'

let server
let streamAiChat
const originalFetch = globalThis.fetch
const originalStorage = Object.getOwnPropertyDescriptor(globalThis, 'localStorage')
let storage
const request = { conversationId: 'test-conversation', message: '你好' }
const encoder = new TextEncoder()

before(async () => {
  server = await createServer({
    configFile: false,
    server: { middlewareMode: true, watch: null, ws: false },
    optimizeDeps: { noDiscovery: true, include: [] },
    appType: 'custom',
  })
  ;({ streamAiChat } = await server.ssrLoadModule('/src/api/ai.ts'))
})
beforeEach(() => {
  storage = new Map([['codehub-token', 'test-token']])
  Object.defineProperty(globalThis, 'localStorage', { configurable: true, value: { getItem: key => storage.get(key) ?? null } })
})
afterEach(() => { globalThis.fetch = originalFetch })
after(async () => {
  await server?.close()
  if (originalStorage) Object.defineProperty(globalThis, 'localStorage', originalStorage)
  else delete globalThis.localStorage
})

function sseResponse(text, bytewise = false) {
  const bytes = encoder.encode(text)
  return new Response(new ReadableStream({
    start(controller) {
      if (bytewise) for (const byte of bytes) controller.enqueue(Uint8Array.of(byte))
      else controller.enqueue(bytes)
      controller.close()
    },
  }), { headers: { 'Content-Type': 'text/event-stream;charset=UTF-8' } })
}

function collect() {
  const tokens = []
  return { tokens, handlers: { onToken: token => tokens.push(token) } }
}

test('POST 携带登录凭证、会话与 AbortSignal，正确解析逐字节 UTF-8 和 CRLF', async () => {
  const controller = new AbortController()
  let captured
  globalThis.fetch = async (url, options) => {
    captured = { url, options }
    return sseResponse(': heartbeat\r\n\r\nevent: token\r\ndata: 你好\r\n\r\nevent: done\r\ndata: [DONE]\r\n\r\n', true)
  }
  const { tokens, handlers } = collect()
  await streamAiChat(request, handlers, controller.signal)
  assert.match(captured.url, /\/ai\/chat\/stream$/)
  assert.equal(captured.options.method, 'POST')
  assert.equal(captured.options.headers.Authorization, 'Bearer test-token')
  assert.equal(captured.options.signal, controller.signal)
  assert.deepEqual(JSON.parse(captured.options.body), { ...request, stream: true })
  assert.deepEqual(tokens, ['你好'])
})

test('保留纯空格、换行及多行 data', async () => {
  globalThis.fetch = async () => sseResponse('event: token\ndata:  \n\nevent: token\ndata: \ndata: \n\nevent: token\ndata: 第一行\ndata: 第二行\n\ndata: [DONE]\n\n')
  const { tokens, handlers } = collect()
  await streamAiChat(request, handlers)
  assert.deepEqual(tokens, [' ', '\n', '第一行\n第二行'])
})

test('优先使用完整 Authorization，完成标记后的内容不再输出', async () => {
  storage.set('codehub-authorization', 'Bearer preferred-token')
  globalThis.fetch = async (_url, options) => {
    assert.equal(options.headers.Authorization, 'Bearer preferred-token')
    return sseResponse('data: 正文\n\ndata: [DONE]\n\ndata: 不应出现\n\n')
  }
  const { tokens, handlers } = collect()
  await streamAiChat(request, handlers)
  assert.deepEqual(tokens, ['正文'])
})

test('未登录时拒绝发送网络请求', async () => {
  storage.clear()
  globalThis.fetch = async () => assert.fail('不应发送请求')
  await assert.rejects(streamAiChat(request, collect().handlers), /请先登录/)
})

test('展示 HTTP 401 登录失效提示', async () => {
  globalThis.fetch = async () => new Response('', { status: 401 })
  await assert.rejects(streamAiChat(request, collect().handlers), /登录已失效/)
})

test('HTTP 200 的 JSON 业务错误也必须报错', async () => {
  globalThis.fetch = async () => Response.json({ code: 503, message: 'AI 模型服务未配置' })
  await assert.rejects(streamAiChat(request, collect().handlers), /AI 模型服务未配置/)
})

test('网关返回 HTML 时不向用户展示 HTML 内容', async () => {
  globalThis.fetch = async () => new Response('<html>private upstream details</html>', { status: 502 })
  await assert.rejects(streamAiChat(request, collect().handlers), error => {
    assert.match(error.message, /HTTP 502/)
    assert.doesNotMatch(error.message, /html|private/)
    return true
  })
})

test('SSE error 保留此前文字并将错误传给界面', async () => {
  globalThis.fetch = async () => sseResponse('event: token\ndata: 部分回答\n\nevent: error\ndata: 模型额度不足\n\n')
  const { tokens, handlers } = collect()
  await assert.rejects(streamAiChat(request, handlers), /模型额度不足/)
  assert.deepEqual(tokens, ['部分回答'])
})

test('缺少结束标记的断流不能作为成功回答', async () => {
  globalThis.fetch = async () => sseResponse('data: 部分回答\n\n')
  await assert.rejects(streamAiChat(request, collect().handlers), /连接意外中断/)
})

test('仅收到结束标记时报告模型未返回文字', async () => {
  globalThis.fetch = async () => sseResponse('event: done\ndata: [DONE]\n\n')
  await assert.rejects(streamAiChat(request, collect().handlers), /模型未返回文字/)
})

test('停止生成传播 AbortError 并释放 reader 锁', async () => {
  const controller = new AbortController()
  let body
  globalThis.fetch = async (_url, options) => {
    body = new ReadableStream({
      start(streamController) {
        streamController.enqueue(encoder.encode('data: 已生成\n\n'))
        options.signal.addEventListener('abort', () => streamController.error(new DOMException('已取消', 'AbortError')), { once: true })
      },
    })
    return new Response(body, { headers: { 'Content-Type': 'text/event-stream' } })
  }
  await assert.rejects(streamAiChat(request, { onToken: () => controller.abort() }, controller.signal), { name: 'AbortError' })
  assert.equal(body.locked, false)
})
