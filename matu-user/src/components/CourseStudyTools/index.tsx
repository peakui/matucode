import { Alert, Button, Card, Empty, Input, Pagination, message } from 'antd'
import { useEffect, useState } from 'react'
import { createNote, listNotes } from '../../api/course'
import type { CourseNoteVO } from '../../api/type/courseTypings'
import { useAppSelector } from '../../store/hooks'
import './index.scss'

export function CourseStudyTools({ courseId, videoId }: { courseId: string; videoId?: string }) {
  const loggedIn = useAppSelector((state) => state.auth.isLoggedIn)
  const [notes, setNotes] = useState<CourseNoteVO[]>([])
  const [text, setText] = useState('')
  const [page, setPage] = useState(1)
  const [total, setTotal] = useState(0)
  const [revision, setRevision] = useState(0)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (!loggedIn) return
    let active = true
    void listNotes({ courseId, videoId, onlyPublic: false, pageNum: page, pageSize: 5 })
      .then((result) => { if (active) { setNotes(result.records); setTotal(result.total); setError('') } })
      .catch(() => { if (active) setError('笔记加载失败，请确认课程学习权限后重试。') })
    return () => { active = false }
  }, [courseId, videoId, loggedIn, page, revision])

  const saveNote = async () => {
    if (!text.trim()) return
    setSaving(true)
    try {
      await createNote({ courseId, videoId, content: text.trim(), isPublic: 0 })
      setText(''); setPage(1); setRevision((value) => value + 1)
      message.success('私有笔记已保存')
    } catch { message.error('笔记保存失败，请重试') }
    finally { setSaving(false) }
  }

  if (!loggedIn) return null
  return <Card title="我的学习记录" className="top-gap study-tools">
    <div className="study-tools__body">
      <Input.TextArea
        className="study-tools__input"
        rows={5}
        maxLength={20000}
        value={text}
        onChange={(event) => setText(event.target.value)}
        placeholder="记录本节收获，笔记仅自己可见"
      />
      <div className="study-tools__actions">
        <Button type="primary" loading={saving} disabled={!text.trim()} onClick={() => void saveNote()}>保存笔记</Button>
      </div>
      {error ? (
        <Alert type="warning" title={error} action={<Button onClick={() => setRevision((value) => value + 1)}>重试</Button>} />
      ) : notes.length ? (
        notes.map((note) => (
          <div key={String(note.id)}>
            <small>{note.createdAt}</small>
            <p style={{ whiteSpace: 'pre-wrap' }}>{note.content}</p>
          </div>
        ))
      ) : (
        <Empty description="暂无学习笔记" image={Empty.PRESENTED_IMAGE_SIMPLE} />
      )}
      {total > 5 ? <Pagination current={page} pageSize={5} total={total} onChange={setPage} /> : null}
    </div>
  </Card>
}
