import { ArrowLeftOutlined, BookOutlined, CodeOutlined, HistoryOutlined, PlayCircleOutlined, ReloadOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Col, Drawer, Row, Select, Space, Spin, Tag, message } from 'antd'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { getOjAssignmentDetail, getOjProblemDetail, getOjSubmissionDetail, listOjSubmissionDetails, listOjSubmissions, submitOjCode } from '../../api/oj'
import type { AssignmentDetailVO, OjProblemVO, OjSubmissionVO } from '../../api/type/ojTypings'
import { CodeEditor } from '../../components/CodeEditor'
import './page.scss'

const difficultyTextMap: Record<number, string> = { 1: '简单', 2: '中等', 3: '困难' }
const judgeStatusMap: Record<number, string> = { 0: '等待判题', 1: '答案正确', 2: '答案错误', 3: '运行错误', 4: '超出时间限制', 5: '超出内存限制', 6: '编译错误', 7: '系统错误' }
const defaultTemplateMap: Record<string, string> = {
  java: 'public class Main {\n  public static void main(String[] args) {\n    \n  }\n}',
  c: '#include <stdio.h>\n\nint main() {\n    \n    return 0;\n}',
  cpp: '#include <bits/stdc++.h>\nusing namespace std;\n\nint main() {\n    \n    return 0;\n}',
  python: 'def main():\n    pass\n\n\nif __name__ == "__main__":\n    main()\n',
}
const pendingStatuses = new Set([0])
const POLL_INTERVAL = 2000
const MAX_POLL_COUNT = 15
const getStatusColor = (s?: number) => (s === 1 ? 'green' : s === 0 ? 'blue' : 'red')
const normalizeRouteId = (value?: string) => value?.trim() || ''

export function ProblemPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const assignmentId = searchParams.get('assignmentId')?.trim() || undefined
  const assignmentClassId = searchParams.get('classId')?.trim() || undefined
  const problemId = normalizeRouteId(id)
  const isValidProblemId = Boolean(problemId)
  const pollTimerRef = useRef<number | null>(null)
  const pollCountRef = useRef(0)
  const [language, setLanguage] = useState('java')
  const [code, setCode] = useState(defaultTemplateMap.java)
  const [problem, setProblem] = useState<OjProblemVO>()
  const [latestSubmission, setLatestSubmission] = useState<OjSubmissionVO>()
  const [submissionHistory, setSubmissionHistory] = useState<OjSubmissionVO[]>([])
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [polling, setPolling] = useState(false)
  const [historyOpen, setHistoryOpen] = useState(false)
  const [assignmentContext, setAssignmentContext] = useState<AssignmentDetailVO>()

  const clearPollTimer = () => {
    if (pollTimerRef.current !== null) {
      window.clearTimeout(pollTimerRef.current)
      pollTimerRef.current = null
    }
  }

  const loadSubmissionHistory = async (targetProblemId: string) => {
    const submissions = await listOjSubmissions({ problemId: targetProblemId, pageNum: 1, pageSize: 10 })
    const history = submissions.records || []
    setSubmissionHistory(history)
    return history
  }

  const loadSubmissionDetails = async (submissionId?: string | number) => {
    if (submissionId == null || String(submissionId).trim() === '') {
      return
    }

    try {
      await listOjSubmissionDetails(String(submissionId).trim())
    } catch (error) {
      console.warn('load submission details error:', error)
    }
  }

  const refreshLatestSubmission = async (targetProblemId: string) => {
    const history = await loadSubmissionHistory(targetProblemId)
    const last = history[0]
    setLatestSubmission(last)
    await loadSubmissionDetails(last?.id)
    return last
  }

  const startPollingSubmission = useCallback((submissionId: string | number) => {
    const normalizedSubmissionId = String(submissionId).trim()
    if (!normalizedSubmissionId) {
      return
    }

    clearPollTimer()
    pollCountRef.current = 0
    setPolling(true)
    const poll = async () => {
      try {
        const latest = await getOjSubmissionDetail(normalizedSubmissionId)
        setLatestSubmission(latest)
        if (latest.id != null) await loadSubmissionDetails(latest.id)
        if (!pendingStatuses.has(latest.status ?? 0)) {
          setPolling(false)
          await loadSubmissionHistory(problemId)
          return
        }
        if (pollCountRef.current >= MAX_POLL_COUNT) {
          setPolling(false)
          message.info('判题仍在进行中，已停止自动轮询，可手动刷新提交记录。')
          await loadSubmissionHistory(problemId)
          return
        }
        pollCountRef.current += 1
        pollTimerRef.current = window.setTimeout(() => void poll(), POLL_INTERVAL)
      } catch (error) {
        console.error('poll submission error:', error)
        setPolling(false)
      }
    }
    void poll()
  }, [problemId])

  useEffect(() => {
    if (!isValidProblemId) {
      setLoading(false)
      return
    }
    const loadData = async () => {
      try {
        setLoading(true)
        const detail = await getOjProblemDetail(problemId)
        setProblem(detail)
        const history = await loadSubmissionHistory(problemId)
        const last = history[0]
        setLatestSubmission(last)
        await loadSubmissionDetails(last?.id)
        if (last?.id != null && String(last.id).trim() !== '' && pendingStatuses.has(last.status ?? 0)) startPollingSubmission(last.id)
      } catch (error) {
        console.error('load oj problem error:', error)
      } finally {
        setLoading(false)
      }
    }
    void loadData()
    return () => {
      clearPollTimer()
      setPolling(false)
    }
  }, [isValidProblemId, problemId, startPollingSubmission])

  useEffect(() => {
    if (!assignmentId || !assignmentClassId) {
      setAssignmentContext(undefined)
      return
    }
    let active = true
    void (async () => {
      try {
        const detail = await getOjAssignmentDetail(assignmentClassId, assignmentId)
        if (active) setAssignmentContext(detail)
      } catch (error) {
        console.error('load assignment context error:', error)
      }
    })()
    return () => {
      active = false
    }
  }, [assignmentId, assignmentClassId])

  const parsedStarterCode = useMemo(() => {
    if (!problem?.starterCodeJson) return undefined
    try {
      return JSON.parse(problem.starterCodeJson) as Record<string, string>
    } catch {
      return undefined
    }
  }, [problem?.starterCodeJson])

  useEffect(() => {
    const starter = parsedStarterCode?.[language] || defaultTemplateMap[language]
    if (starter) setCode(starter)
  }, [language, parsedStarterCode])

  const handleRefreshSubmissions = async () => {
    if (!isValidProblemId) return
    try {
      const last = await refreshLatestSubmission(problemId)
      if (last?.id != null && String(last.id).trim() !== '' && pendingStatuses.has(last.status ?? 0)) startPollingSubmission(last.id)
    } catch (error) {
      console.error('refresh submissions error:', error)
      message.error('刷新提交记录失败，请稍后重试')
    }
  }

  const handleOpenHistory = async () => {
    setHistoryOpen(true)
    await handleRefreshSubmissions()
  }

  const handleSelectSubmission = async (submission: OjSubmissionVO) => {
    setLatestSubmission(submission)
    await loadSubmissionDetails(submission.id)
    clearPollTimer()
    if (submission.id != null && String(submission.id).trim() !== '' && pendingStatuses.has(submission.status ?? 0)) startPollingSubmission(submission.id)
    else setPolling(false)
  }

  const handleSubmit = async () => {
    if (!isValidProblemId || !code.trim()) return message.warning('请先输入代码后再提交')
    try {
      setSubmitting(true)
      const submission = await submitOjCode({
        problemId,
        language,
        code,
        ...(assignmentId ? { assignmentId } : {}),
      })
      setLatestSubmission(submission)
      await loadSubmissionHistory(problemId)
      setHistoryOpen(true)
      if (submission.id != null && String(submission.id).trim() !== '') startPollingSubmission(submission.id)
      message.success('提交成功，正在轮询判题结果')
    } catch (error) {
      console.error('submit oj code error:', error)
      const responseData = (error as { response?: { data?: { message?: string; msg?: string } } })?.response?.data
      message.error(responseData?.message || responseData?.msg || '提交失败，请稍后重试')
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) return <Card className="content-card" variant="borderless"><div className="check-in-loading"><Spin /></div></Card>
  if (!isValidProblemId) return <Card className="content-card" variant="borderless">题目 ID 无效。</Card>
  if (problem?.id == null || String(problem.id).trim() === '') return <Card className="content-card" variant="borderless">题目不存在。</Card>

  const currentProblem = problem

  const assignmentProblem = assignmentContext?.problems?.find((item) => String(item.problemId) === problemId)
  const attemptsLeft = assignmentContext?.maxAttempts && assignmentContext.maxAttempts > 0
    ? Math.max(assignmentContext.maxAttempts - (assignmentProblem?.attemptsUsed ?? 0), 0)
    : null
  const assignmentBlocked = assignmentContext != null && (assignmentContext.submittable === false || attemptsLeft === 0)

  return (
    <div className="problem-workspace">
      {assignmentContext ? (
        <Alert
          type={assignmentBlocked ? 'warning' : 'info'}
          showIcon
          style={{ marginBottom: 16 }}
          message={`本次提交计入班级作业《${assignmentContext.title || '未命名作业'}》`}
          description={
            <Space wrap size={6}>
              <Tag color={assignmentContext.status === 1 ? 'green' : 'default'} variant="filled">
                {assignmentContext.status === 1 ? '进行中' : assignmentContext.status === 0 ? '未开始' : '已结束'}
              </Tag>
              {assignmentContext.deadline ? <Tag color="orange">截止 {assignmentContext.deadline.replace('T', ' ')}</Tag> : null}
              <Tag>已提交 {assignmentProblem?.attemptsUsed ?? 0} 次</Tag>
              {attemptsLeft !== null ? <Tag color={attemptsLeft > 0 ? 'blue' : 'red'}>剩余 {attemptsLeft} 次</Tag> : null}
              {assignmentProblem?.solved ? <Tag color="green" variant="filled">已通过</Tag> : null}
              {assignmentBlocked ? <Tag color="red" variant="filled">当前不可提交</Tag> : null}
            </Space>
          }
        />
      ) : null}
      <Row gutter={[24, 24]} justify="center" className="problem-page">
        <Col xs={24} xxl={6} xl={7}>
          <Card className="content-card problem-content-card" variant="borderless">
            <div className="problem-back-row">
              <Button icon={<ArrowLeftOutlined />} className="problem-back-btn" onClick={() => navigate(-1)}>
                返回
              </Button>
            </div>
            <Space wrap><Tag color="blue" variant="filled">OJ 题目</Tag><Tag color="gold" variant="filled">{difficultyTextMap[currentProblem.difficulty || 2] || '未知'}</Tag></Space>
            <h1 className="detail-page__title">{currentProblem.title}</h1>
            <p className="detail-page__content">{currentProblem.description}</p>
            {currentProblem.contentHtml ? <div className="detail-page__content" dangerouslySetInnerHTML={{ __html: currentProblem.contentHtml }} /> : null}
            <div className="problem-example-block">
              <h3><BookOutlined /> 输入格式</h3><pre>{currentProblem.inputFormat || '暂无输入格式说明'}</pre>
              <h3><PlayCircleOutlined /> 输出格式</h3><pre>{currentProblem.outputFormat || '暂无输出格式说明'}</pre>
              <h3><BookOutlined /> 示例输入</h3><pre>{currentProblem.sampleInput || '暂无示例输入'}</pre>
              <h3><PlayCircleOutlined /> 示例输出</h3><pre>{currentProblem.sampleOutput || '暂无示例输出'}</pre>
            </div>
            {currentProblem.hint ? <Alert type="info" showIcon message={currentProblem.hint} /> : null}
          </Card>
        </Col>
        <Col xs={24} xxl={18} xl={17}>
          <Card className="content-card problem-editor-card oj-editor-layout" variant="borderless">
            <div className="oj-editor-layout__toolbar problem-editor-head">
              <div>
                <div className="oj-editor-layout__title"><CodeOutlined /> 代码编辑器</div>
                <div className="problem-editor-subtitle">提交结果和历史记录已收纳到右侧抽屉中查看。</div>
              </div>
              <Space wrap>
                <Select value={language} onChange={setLanguage} options={[{ label: 'Java', value: 'java' }, { label: 'C', value: 'c' }, { label: 'C++', value: 'cpp' }, { label: 'Python', value: 'python' }]} style={{ width: 120 }} />
                <Button icon={<HistoryOutlined />} onClick={() => void handleOpenHistory()}>查看记录</Button>
                <Button type="primary" loading={submitting} onClick={() => void handleSubmit()}>提交</Button>
              </Space>
            </div>
            <div className="oj-editor-layout__main"><CodeEditor language={language} value={code} onChange={setCode} height="860px" /></div>
            <div className="problem-status-bar problem-status-bar--below-editor">
              <div className="problem-status-pill"><span>最新状态</span><Tag color={getStatusColor(latestSubmission?.status)}>{latestSubmission ? judgeStatusMap[latestSubmission.status || 0] || '未知状态' : '暂无提交'}</Tag></div>
              <div className="problem-status-pill"><span>最近提交</span><strong>{latestSubmission?.createdAt || '-'}</strong></div>
              <div className="problem-status-pill"><span>历史数量</span><strong>{submissionHistory.length} 条</strong></div>
              {polling ? <Tag color="processing">判题轮询中</Tag> : null}
            </div>
          </Card>
        </Col>
      </Row>
      <Drawer title="提交记录" placement="right" size={460} onClose={() => setHistoryOpen(false)} open={historyOpen} className="problem-history-drawer">
        <div className="problem-history-panel">
          <div className="oj-result-panel">
            <div className="oj-result-panel__header"><h3>最新提交结果</h3><Space wrap>{polling ? <Tag color="processing">判题轮询中</Tag> : null}<Button size="small" icon={<ReloadOutlined />} onClick={() => void handleRefreshSubmissions()}>刷新</Button></Space></div>
            {latestSubmission ? <div className="oj-result-grid"><div className="oj-result-grid__item"><span>状态</span><Tag color={getStatusColor(latestSubmission.status)}>{judgeStatusMap[latestSubmission.status || 0] || '未知状态'}</Tag></div><div className="oj-result-grid__item"><span>语言</span><strong>{latestSubmission.language || language}</strong></div><div className="oj-result-grid__item"><span>耗时</span><strong>{latestSubmission.executionTime ?? '-'} ms</strong></div><div className="oj-result-grid__item"><span>内存</span><strong>{latestSubmission.memoryUsed ?? '-'} KB</strong></div><div className="oj-result-grid__item oj-result-grid__item--full"><span>提交时间</span><strong>{latestSubmission.createdAt || '-'}</strong></div>{latestSubmission.errorMessage ? <div className="oj-result-grid__item oj-result-grid__item--full"><Alert type="error" showIcon message={latestSubmission.errorMessage} /></div> : null}</div> : <p className="oj-empty-text">暂无提交记录</p>}
          </div>
          <div className="problem-analysis problem-analysis--compact">
            <div className="problem-analysis__header"><h3>提交历史</h3><span>最近 {submissionHistory.length} 条</span></div>
            <Space direction="vertical" size={10} className="full-width">
              {submissionHistory.length ? submissionHistory.map((item) => <button key={String(item.id)} type="button" className={`submission-history-item${item.id === latestSubmission?.id ? ' submission-history-item--active' : ''}`} onClick={() => void handleSelectSubmission(item)}><div className="submission-history-item__top"><span>#{item.id}</span><Tag color={getStatusColor(item.status)}>{judgeStatusMap[item.status || 0] || '未知状态'}</Tag></div><div className="submission-history-item__meta"><span>{item.language || '-'}</span><span>{item.executionTime ?? '-'} ms</span><span>{item.memoryUsed ?? '-'} KB</span></div></button>) : <p className="oj-empty-text">暂无历史提交</p>}
            </Space>
          </div>
        </div>
      </Drawer>
    </div>
  )
}
