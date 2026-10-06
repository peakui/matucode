// Match JSON strings before numbers, so numbers inside quoted content are never changed.
export function parseSafeJson(raw) {
  if (typeof raw !== 'string') return raw
  const safe = raw.replace(/"(?:[^"\\]|\\.)*"|-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?/g, (token) => {
    if (/^-?\d{16,}$/.test(token) && !Number.isSafeInteger(Number(token))) return `"${token}"`
    return token
  })
  return JSON.parse(safe)
}
export function unwrapResponse(status, raw) {
  let body
  try {
    body = parseSafeJson(raw)
  } catch {
    const error = new Error('服务器响应异常，请稍后重试')
    error.status = status
    throw error
  }
  const code = Number(body?.code)
  const error = new Error(body?.message || body?.msg || `请求失败（${status}）`)
  error.status = status
  error.code = code
  if (status < 200 || status >= 300 || !(body?.success === true || [0, 200, 20000].includes(code)))
    throw error
  return body.data
}
