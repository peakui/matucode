export function resumeTime(saved, duration) {
  const time = Math.max(0, Number(saved?.watchedDuration) || 0)
  return duration > 0 && time >= duration - 2 ? 0 : time
}
export function progressSnapshot(videoId, currentTime, duration) {
  if (
    !videoId ||
    !Number.isFinite(currentTime) ||
    !Number.isFinite(duration) ||
    duration <= 0 ||
    currentTime <= 0
  )
    return null
  return {
    videoId: String(videoId),
    watchedDuration: Math.floor(Math.min(currentTime, duration)),
    progressPercent: Math.min(100, Math.floor((currentTime / duration) * 100)),
  }
}
