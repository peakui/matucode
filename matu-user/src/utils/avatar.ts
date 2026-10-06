export const DEFAULT_AVATAR_URL = 'https://your-bucket.oss-cn-hangzhou.aliyuncs.com/defaulthead.png'

type AvatarSource = {
  avatarUrl?: string
  authorAvatar?: string
  userAvatar?: string
  userAvatarUrl?: string
  headImgUrl?: string
  avatar?: string
}

const normalizeAvatarUrl = (url?: string) => {
  const value = url?.trim()

  if (!value) {
    return ''
  }

  if (/^(https?:)?\/\//i.test(value) || value.startsWith('data:') || value.startsWith('blob:')) {
    return value
  }

  return value.startsWith('/') ? value : `/${value}`
}

export const resolveAvatarUrl = (source?: AvatarSource | null) => {
  return normalizeAvatarUrl(
    source?.avatarUrl
    || source?.authorAvatar
    || source?.userAvatar
    || source?.userAvatarUrl
    || source?.headImgUrl
    || source?.avatar,
  )
}

export const getAvatarWithFallback = (source: AvatarSource | null | undefined) => {
  return resolveAvatarUrl(source) || DEFAULT_AVATAR_URL
}
