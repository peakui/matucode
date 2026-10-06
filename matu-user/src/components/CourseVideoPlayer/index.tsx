import { Alert, Button, Progress, Space, Spin } from 'antd'
import { useEffect, useRef, useState } from 'react'
import { getMyProgress, updateProgress } from '../../api/course'
import { useAppSelector } from '../../store/hooks'

export function CourseVideoPlayer({ courseId, videoId, src, poster }: {
  courseId: string; videoId: string; src: string; poster?: string
}) {
  const loggedIn = useAppSelector((state) => state.auth.isLoggedIn)
  const videoRef = useRef<HTMLVideoElement>(null)
  const lastSaved = useRef(0)
  const [loading, setLoading] = useState(loggedIn)
  const [resumeAt, setResumeAt] = useState(0)
  const [percent, setPercent] = useState(0)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    if (!loggedIn) { setLoading(false); return }
    void getMyProgress(courseId).then((items) => {
      const progress = items.find((item) => String(item.videoId) === videoId)
      if (active) {
        setPercent(Number(progress?.progressPercent || 0))
        setResumeAt(progress?.isCompleted === 1 ? 0 : progress?.watchedDuration || 0)
      }
    }).catch(() => { if (active) setError('学习进度读取失败，本次播放仍可继续保存进度。') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [courseId, videoId, loggedIn])

  const save = async (video: HTMLVideoElement, force = false) => {
    if (!loggedIn || !Number.isFinite(video.duration) || video.duration <= 0) return
    if (!force && Date.now() - lastSaved.current < 15000) return
    lastSaved.current = Date.now()
    try {
      const progress = await updateProgress(courseId, {
        videoId,
        watchedDuration: Math.floor(video.currentTime),
        progressPercent: video.ended ? 100 : Math.min(99.99, Math.round(video.currentTime / video.duration * 10000) / 100),
      })
      setPercent((previous) => Math.max(previous, Number(progress.progressPercent || 0)))
      setError('')
    } catch {
      setError('学习进度保存失败，请重试。')
    }
  }

  return <Space orientation="vertical" className="full-width">
    {loading ? <Spin /> : <video ref={videoRef} controls src={src} poster={poster} className="course-video-player"
      onLoadedMetadata={(event) => { event.currentTarget.currentTime = Math.min(resumeAt, Math.max(0, event.currentTarget.duration - 1)) }}
      onTimeUpdate={(event) => { void save(event.currentTarget) }}
      onPause={(event) => { void save(event.currentTarget, true) }}
      onEnded={(event) => { void save(event.currentTarget, true) }} />}
    {loggedIn ? <Progress percent={Math.round(percent)} /> : <Alert type="info" title="登录后自动保存学习进度" />}
    {error ? <Alert type="warning" title={error} action={<Button size="small" onClick={() => { if (videoRef.current) void save(videoRef.current, true) }}>重试保存</Button>} /> : null}
  </Space>
}
