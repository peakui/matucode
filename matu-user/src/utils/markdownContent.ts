const MARKDOWN_IMAGE_PATTERN = /!\[[^\]]*\]\(\s*([^)\s]+?)(?:\s+["'][^"']*["'])?\s*\)/
const MARKDOWN_IMAGE_PATTERN_GLOBAL = /!\[[^\]]*\]\(\s*[^)]*?\)/g
const MARKDOWN_LINK_PATTERN_GLOBAL = /\[([^\]]*)\]\(\s*[^)]*?\)/g
const BARE_IMAGE_URL_PATTERN = /https?:\/\/[^\s)]+\.(?:png|jpe?g|gif|webp|bmp|svg)(?:\?[^\s)]*)?/i

// Summaries generated before this helper existed kept the image alt text and
// raw URL (only the brackets were stripped), so bare URLs must be dropped too.
const normalizeSummary = (value: string) =>
  value
    .replace(/```[\s\S]*?```/g, ' ')
    .replace(MARKDOWN_IMAGE_PATTERN_GLOBAL, ' ')
    .replace(MARKDOWN_LINK_PATTERN_GLOBAL, '$1')
    .replace(/https?:\/\/\S+/gi, ' ')
    .replace(/[#>*_`~!\-[\]()]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()

export const extractFirstImageUrl = (value?: string) => {
  if (!value) {
    return undefined
  }

  return MARKDOWN_IMAGE_PATTERN.exec(value)?.[1] || BARE_IMAGE_URL_PATTERN.exec(value)?.[0]
}

export const toPlainSummary = (value?: string) => (value ? normalizeSummary(value) : '')
