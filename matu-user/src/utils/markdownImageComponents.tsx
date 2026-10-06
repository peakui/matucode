import { Image } from 'antd'
import type { Components } from 'react-markdown'

const toImageSource = (value?: string | Blob) => {
  return typeof value === 'string' ? value : ''
}

const toThumbnailSource = (source: string) => {
  try {
    const url = new URL(source)
    if (!url.hostname.includes('aliyuncs.com')) {
      return source
    }
    const process = 'image/resize,m_lfit,w_1600/quality,q_85'
    url.searchParams.set('x-oss-process', process)
    return url.toString()
  } catch {
    return source
  }
}

export const markdownImageComponents: Components = {
  img: ({ src, alt }) => {
    const imageSrc = toImageSource(src)

    if (!imageSrc) {
      return null
    }

    return (
      <Image
        src={toThumbnailSource(imageSrc)}
        alt={alt || '正文图片'}
        className="markdown-content-image"
        loading="lazy"
        decoding="async"
        preview={{ src: imageSrc, mask: '查看大图' }}
      />
    )
  },
}