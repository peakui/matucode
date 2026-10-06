import { session, signedIn } from './session.js'
const key = () => 'matu-question-bookmarks-' + String(session.user?.userId || '')
export function bookmarks() {
  return signedIn() ? uni.getStorageSync(key()) || [] : []
}
export function isBookmarked(id) {
  return bookmarks().some((q) => String(q.id) === String(id))
}
export function toggleBookmark(question) {
  if (!signedIn()) return false
  const list = bookmarks()
  const exists = list.some((q) => String(q.id) === String(question.id))
  const next = exists
    ? list.filter((q) => String(q.id) !== String(question.id))
    : [
        {
          id: String(question.id),
          title: question.title,
          categoryName: question.categoryName,
          difficulty: question.difficulty,
        },
        ...list,
      ]
  uni.setStorageSync(key(), next)
  return !exists
}
