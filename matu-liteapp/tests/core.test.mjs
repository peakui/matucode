import test from 'node:test'
import assert from 'node:assert/strict'
import { parseSafeJson, unwrapResponse } from '../utils/json.js'
import { createPager, loadPage } from '../utils/pager.js'
const storage = new Map()
const notifications = []
let respond
const calls = []
globalThis.uni = {
  getStorageSync: (key) => storage.get(key),
  setStorageSync: (key, value) => storage.set(key, value),
  removeStorageSync: (key) => storage.delete(key),
  showToast: (value) => notifications.push(value),
  request: (options) => {
    calls.push(options)
    respond(options)
  },
}
const { session, saveSession, clearSession, signedIn } = await import('../utils/session.js')
const { request } = await import('../api/request.js')
const { toggleBookmark, bookmarks } = await import('../utils/collections.js')
const login = (id) => saveSession({ user: { userId: id }, authorization: 'Bearer ' + id, expiresIn: 3600 })

test('长整数 ID 保持精度，普通数值与正文不改变', () => {
  const data = parseSafeJson(
    '{"id":9223372036854775807,"negative":-9223372036854775807,"list":[9007199254740993],"text":"9223372036854775807","escaped":"a\\\" 9223372036854775807","price":18.5,"n":42,"big":1e20}'
  )
  assert.equal(data.id, '9223372036854775807')
  assert.equal(data.negative, '-9223372036854775807')
  assert.equal(data.list[0], '9007199254740993')
  assert.equal(data.price, 18.5)
  assert.equal(data.n, 42)
  assert.equal(data.big, 1e20)
})
test('成功、业务失败、HTTP 错误、HTML 网关错误分别处理', () => {
  assert.deepEqual(unwrapResponse(200, '{"code":0,"data":[1]}'), [1])
  assert.throws(() => unwrapResponse(200, '{"code":403,"message":"权限不足"}'), /权限不足/)
  assert.throws(() => unwrapResponse(401, '{"code":401,"message":"登录已失效"}'), /登录已失效/)
  assert.throws(() => unwrapResponse(502, '<html>bad gateway</html>'), /服务器响应异常/)
})
test('快速重复刷新时只保留最新结果', async () => {
  const state = createPager()
  let release
  const old = loadPage(
    state,
    () =>
      new Promise((resolve) => {
        release = resolve
      }),
    true
  )
  await loadPage(state, async () => ({ records: [{ id: 'new' }], total: 1 }), true)
  release({ records: [{ id: 'old' }], total: 1 })
  await old
  assert.deepEqual(
    state.items.map((x) => x.id),
    ['new']
  )
  assert.equal(state.done, true)
  assert.equal(state.loading, false)
})
test('分页失败不前移页码，重试不重复追加已有项目', async () => {
  const state = createPager()
  await loadPage(state, async () => ({ records: [{ id: 'a' }], total: 3 }), true)
  await loadPage(state, async () => {
    throw new Error('断网')
  })
  assert.equal(state.page, 1)
  assert.equal(state.error, '断网')
  await loadPage(state, async (page) => {
    assert.equal(page, 2)
    return { records: [{ id: 'a' }, { id: 'b' }], total: 3 }
  })
  assert.deepEqual(
    state.items.map((x) => x.id),
    ['a', 'b']
  )
  assert.equal(state.error, '')
})
test('请求读取原始文本并使用完整授权，登录入口不带旧凭证', async () => {
  login('one')
  respond = (o) => o.success({ statusCode: 200, data: '{"code":0,"data":{"id":9223372036854775807}}' })
  const result = await request('/posts')
  assert.equal(result.id, '9223372036854775807')
  assert.equal(calls.at(-1).dataType, 'text')
  assert.equal(calls.at(-1).header.Authorization, 'Bearer one')
  await request('/auth/mini/login', {}, 'POST', { publicAuth: true })
  assert.equal(calls.at(-1).header.Authorization, undefined)
})
test('过期会话只重试公开 GET，不重复写入操作', async () => {
  login('expired')
  const before = calls.length
  respond = (o) =>
    o.header.Authorization
      ? o.success({ statusCode: 401, data: '{"code":401,"message":"登录过期"}' })
      : o.success({ statusCode: 200, data: '{"code":0,"data":[]} ' })
  await request('/posts', {}, 'GET', { guest: true })
  assert.equal(calls.length - before, 2)
  assert.equal(session.authorization, '')
  login('expired')
  const writeStart = calls.length
  await assert.rejects(request('/posts/1/like', {}, 'POST'), /登录过期/)
  assert.equal(calls.length - writeStart, 1)
})
test('本机收藏按账号隔离，不保存受限答案', () => {
  login('alice')
  toggleBookmark({ id: '1', title: 'A', answer: 'private' })
  assert.equal(bookmarks().length, 1)
  assert.equal(bookmarks()[0].answer, undefined)
  login('bob')
  assert.equal(bookmarks().length, 0)
  clearSession()
  assert.equal(bookmarks().length, 0)
  login('alice')
  assert.equal(bookmarks()[0].title, 'A')
  toggleBookmark({ id: '1' })
  assert.equal(bookmarks().length, 0)
})
test('本地会话超过有效期立即清空', () => {
  login('expired')
  session.expiresAt = Date.now() - 1
  assert.equal(signedIn(), false)
  assert.equal(session.user, null)
})

test('旧账号的迟到响应不会覆盖新账号数据', async () => {
  login('old')
  let release
  respond = (options) => {
    release = options.success
  }
  const pending = request('/auth/mini/me')
  login('new')
  release({ statusCode: 200, data: '{"code":0,"data":{"userId":"old"}}' })
  await assert.rejects(pending, /账号状态已变化/)
  assert.equal(session.user.userId, 'new')
})

test('空的 401 响应仍然使旧会话失效', async () => {
  login('blank')
  respond = (o) => o.success({ statusCode: 401, data: '' })
  await assert.rejects(request('/auth/mini/me'))
  assert.equal(session.authorization, '')
})
const { resumeTime, progressSnapshot } = await import('../utils/video.js')
test('视频进度接近结尾不提前标为完成，时长异常不发送', () => {
  assert.equal(progressSnapshot('1', 99.9, 100).progressPercent, 99)
  assert.equal(progressSnapshot('1', 100, 100).progressPercent, 100)
  assert.equal(progressSnapshot('1', 0, 100), null)
  assert.equal(progressSnapshot('1', 10, 0), null)
})
test('视频恢复到观看位置，已看完的课时从头开始', () => {
  assert.equal(resumeTime({ watchedDuration: 45 }, 100), 45)
  assert.equal(resumeTime({ watchedDuration: 100 }, 100), 0)
  assert.equal(resumeTime({ watchedDuration: -2 }, 100), 0)
})

const { uploadImage } = await import('../api/request.js')
test('头像上传保持授权、解析响应，并拒绝迟到的旧账号上传结果', async () => {
  login('avatar-user')
  let release
  uni.uploadFile = (options) => {
    assert.equal(options.name, 'file')
    assert.equal(options.header.Authorization, 'Bearer avatar-user')
    release = options.success
  }
  const pending = uploadImage('/tmp/avatar.jpg')
  release({ statusCode: 200, data: '{"code":0,"data":{"fileUrl":"https://example.test/avatar.png"}}' })
  assert.equal((await pending).fileUrl, 'https://example.test/avatar.png')
  const old = uploadImage('/tmp/avatar.jpg')
  login('another')
  release({ statusCode: 200, data: '{"code":0,"data":{"fileUrl":"https://example.test/old.png"}}' })
  await assert.rejects(old, /账号状态已变化/)
})
const { finishLogin } = await import('../utils/session.js')
test('从我的发起登录后继续打开目标页面', () => {
  globalThis.getCurrentPages = () => [{ route: 'pages/me/index' }, { route: 'pages/login/index' }]
  let redirected
  uni.redirectTo = (options) => {
    redirected = options.url
  }
  finishLogin(encodeURIComponent('/pages/profile/index'))
  assert.equal(redirected, '/pages/profile/index')
})
test('从详情登录后返回已有页面，保留原页面状态', () => {
  globalThis.getCurrentPages = () => [{ route: 'pages/content/detail' }, { route: 'pages/login/index' }]
  let returned = false
  uni.navigateBack = () => {
    returned = true
  }
  finishLogin('/pages/content/detail?type=article&id=1')
  assert.equal(returned, true)
})
