import { API_BASE_URL } from '../config/index.js'
export const nameOf = (item) =>
  item.nickname || item.authorName || item.userName || item.username || '码途用户'
export const idOf = (item) => String(item.id || item.checkId || item.recordId || '')
export function assetUrl(value) {
  if (!value) return ''
  const url = String(value).trim()
  if (/^https?:\/\//i.test(url)) return url
  if (url.startsWith('//')) return 'https:' + url
  if (url.startsWith('/') && !url.startsWith('//')) return API_BASE_URL.replace(/\/$/, '') + url
  return ''
}
export const avatarOf = (item) =>
  assetUrl(item.avatarUrl || item.authorAvatar || item.userAvatar || item.userAvatarUrl)
export const dateOf = (value) => (value ? String(value).replace('T', ' ').slice(0, 16) : '')
export const shortDate = (value) => (value ? String(value).slice(0, 10) : '')
export const plain = (value) =>
  String(value || '')
    .replace(/<[^>]*>/g, '')
    .replace(/!\[[^\]]*\]\([^)]*\)/g, '')
    .replace(/[#*`>]/g, '')
    .replace(/\s+/g, ' ')
    .trim()
export const difficulty = (value) => ({ 1: '基础', 2: '进阶', 3: '挑战' })[value] || '未分级'
export const toast = (error) => uni.showToast({ title: error?.message || String(error), icon: 'none' })

const pickText = (...values) => {
  for (const value of values) {
    if (typeof value === 'string' && value.trim()) return value.trim()
  }
  return ''
}
const isFlagged = (value) => value === 1 || value === '1' || value === true || value === 'true'

// 作者/评论者身份徽章：兼容各服务返回的不同字段名（文章用 authorXxx，打卡/问答用 xxx）。
export const identityBadges = (source) => {
  if (!source) return []
  const company = pickText(source.authorCompanyName, source.companyName, source.userCompanyName, source.commentAuthorCompanyName)
  const school = pickText(source.authorSchoolName, source.schoolName, source.userSchoolName, source.commentAuthorSchoolName)
  const title = pickText(source.authorTitle, source.userTitle, source.jobTitle, source.commentAuthorTitle)
  const vip = isFlagged(source.authorIsVip) || isFlagged(source.isVip)
  return [
    vip ? { key: 'vip', label: 'VIP', color: 'gold' } : null,
    company ? { key: 'company', label: company, color: 'blue' } : null,
    school ? { key: 'school', label: school, color: 'green' } : null,
    title ? { key: 'title', label: title, color: 'purple' } : null,
  ].filter(Boolean)
}
