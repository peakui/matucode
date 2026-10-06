import type { StoredUser } from '../store/modules/authSlice'

const ADMIN_ROLE_KEYWORDS = ['admin', 'administrator', 'role_admin', 'role_administrator', 'sys_admin', 'super_admin', '管理员']
const VIP_ROLE_KEYWORDS = ['vip', 'role_vip', 'premium', '会员', '付费会员']
// 与后端 CourseServiceImpl.requireUploader 的 role("teacher","instructor","verified","certified") 对齐
const COURSE_PUBLISHER_ROLE_KEYWORDS = ['teacher', 'instructor', 'verified', 'certified', '讲师']

const normalizeRole = (role: string) => role.trim().toLowerCase()

const hasMatchingRole = (roles: string[] | undefined, keywords: string[]) => {
  if (!roles?.length) {
    return false
  }

  const normalizedRoles = roles.map(normalizeRole)
  return normalizedRoles.some((role) => keywords.some((keyword) => role.includes(normalizeRole(keyword))))
}

const isVerified = (value?: number) => value === 1

type VipLike = { isVip?: number | boolean | string | null; vipExpiredAt?: string | null }

const isAdminUser = (user?: Pick<StoredUser, 'roles'> | null) => hasMatchingRole(user?.roles, ADMIN_ROLE_KEYWORDS)

const isVipUser = (user?: Pick<StoredUser, 'roles'> | null) => hasMatchingRole(user?.roles, VIP_ROLE_KEYWORDS)

// 展示用：VIP 状态来自 UserProfile（isVip/vipExpiredAt），非角色。后端已把过期置为 0，这里再兜底校验一次。
const isVipMember = (user?: VipLike | null) => {
  if (!user) {
    return false
  }

  const flag = user.isVip
  const active = flag === 1 || flag === true || flag === '1' || flag === 'true'
  if (!active) {
    return false
  }

  if (!user.vipExpiredAt) {
    return true
  }

  const expiry = new Date(user.vipExpiredAt).getTime()
  return Number.isNaN(expiry) || expiry > Date.now()
}

// 内容作者 VIP：后端已按过期时间归零，这里只做真值判断。
const isVipAuthor = (source?: { authorIsVip?: number | boolean | string | null } | null) => {
  const flag = source?.authorIsVip
  return flag === 1 || flag === true || flag === '1' || flag === 'true'
}

const canViewInterviewAnswer = (user?: Pick<StoredUser, 'roles'> | null) => isAdminUser(user) || isVipUser(user)

const canPublishCourse = (user?: Pick<StoredUser, 'roles' | 'titleVerified'> | null) => {
  if (!user) {
    return false
  }

  return isAdminUser(user) || hasMatchingRole(user.roles, COURSE_PUBLISHER_ROLE_KEYWORDS) || isVerified(user.titleVerified)
}

export { canPublishCourse, canViewInterviewAnswer, isAdminUser, isVerified, isVipAuthor, isVipMember, isVipUser }
