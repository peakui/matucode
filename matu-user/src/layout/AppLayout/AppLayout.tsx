import './AppLayout.scss'

import { SearchOutlined,
  BellOutlined,
  BookOutlined,
  CrownOutlined,
  CodeOutlined,
  CommentOutlined,
  DeleteOutlined,
  EditOutlined,
  FireOutlined,
  FormOutlined,
  HomeOutlined,
  LogoutOutlined,
  MessageOutlined,
  PlayCircleOutlined,
  PlusOutlined,
  RobotOutlined,
  UserOutlined,
  CopyOutlined,
  HistoryOutlined,
  ReloadOutlined,
  StopOutlined,
  ArrowUpOutlined,
} from '@ant-design/icons'
import ProLayout, { PageContainer } from '@ant-design/pro-layout'
import MDEditor from '@uiw/react-md-editor'
import { Avatar, Badge, Button, Dropdown, Empty, Input, Modal, Popover, Space, Spin, Tooltip, notification } from 'antd'
import type { MenuProps } from 'antd'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { getCurrentUserProfile } from '../../api/authProfile'
import { deleteAiConversation, listAiConversations, listAiMessages, renameAiConversation, streamAiChat } from '../../api/ai'
import type { AiConversationVO } from '../../api/type/aiTypings'
import { getNotificationUnreadCount, listConversations } from '../../api/message'
import { logout as logoutRequest } from '../../api/login'
import type { ConversationVO, MessageReadEventData, MessageVO, PrivateMessageWsEvent } from '../../api/type/messageTypings'
import { useAppDispatch, useAppSelector } from '../../store/hooks'
import { logout, syncAuthState } from '../../store/modules/authSlice'
import { getAvatarWithFallback } from '../../utils/avatar'
import { markdownImageComponents } from '../../utils/markdownImageComponents'
import { buildSearchPath, SEARCH_KEYWORD_LIMIT } from '../../utils/search'
import { getCollabInviteNavigateTarget, parseCollabInviteMessage } from '../../utils/collabInviteMessage'
import { buildMessagesWsUrl, getStoredMessageToken } from '../../utils/messagesWebSocketUrl'

const menuData = [
  { path: '/home', name: '首页', icon: <HomeOutlined /> },
  { path: '/check-in', name: '打卡', icon: <CodeOutlined /> },
  { path: '/qa', name: '问答', icon: <CommentOutlined /> },
  { path: '/tutorials', name: '教程', icon: <PlayCircleOutlined /> },
  { path: '/practice', name: '刷题', icon: <FireOutlined /> },
  { path: '/interviews', name: '面试题', icon: <BookOutlined /> },
  { path: '/ai', name: 'AI 助手', icon: <RobotOutlined /> },
  { path: '/membership', name: 'VIP', icon: <CrownOutlined /> },
]

type ChatMessage = {
  id: string
  role: 'assistant' | 'user'
  content: string
  time: string
}

type ChatSession = {
  id: string
  title: string
  updatedAt: string
  updatedAtValue: number
  messages: ChatMessage[]
  // False only for sessions restored from the server whose messages have not been
  // fetched yet; a fresh draft session has nothing to load.
  historyLoaded?: boolean
}

const formatMessageTime = (timestamp: number) => new Date(timestamp).toLocaleTimeString('zh-CN', {
  hour: '2-digit',
  minute: '2-digit',
})

const createChatSession = (): ChatSession => {
  const now = Date.now()
  return {
    id: globalThis.crypto?.randomUUID?.() || `session-${now}-${Math.random().toString(36).slice(2)}`,
    title: '新会话',
    updatedAt: formatMessageTime(now),
    updatedAtValue: now,
    messages: [],
    historyLoaded: true,
  }
}

const toHistorySession = (record: AiConversationVO): ChatSession => {
  const parsed = record.updatedAt ? Date.parse(record.updatedAt) : NaN
  const updatedAtValue = Number.isNaN(parsed) ? Date.now() : parsed
  return {
    id: record.conversationId,
    title: record.title?.trim() || '历史会话',
    updatedAt: formatMessageTime(updatedAtValue),
    updatedAtValue,
    messages: [],
    historyLoaded: false,
  }
}

const formatHistoryMessageTime = (iso?: string) => {
  const parsed = iso ? Date.parse(iso) : NaN
  return Number.isNaN(parsed) ? '' : formatMessageTime(parsed)
}

const formatHistoryTimestamp = (iso?: string) => {
  const parsed = iso ? Date.parse(iso) : NaN
  if (Number.isNaN(parsed)) return ''
  return new Date(parsed).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

declare global {
  interface Window {
    __codehubMessageWs?: WebSocket
    __codehubMessageWsUrl?: string
  }
}

const PRIVATE_MESSAGE_WS_EVENT = 'codehub-private-message-ws'

const dispatchPrivateMessageWsEvent = (event: PrivateMessageWsEvent) => {
  window.dispatchEvent(new CustomEvent<PrivateMessageWsEvent>(PRIVATE_MESSAGE_WS_EVENT, { detail: event }))
}

export function AppLayout() {
  const navigate = useNavigate()
  const location = useLocation()
  const dispatch = useAppDispatch()
  const theme = useAppSelector((state) => state.theme.mode)
  const isLoggedIn = useAppSelector((state) => state.auth.isLoggedIn)
  const userInfo = useAppSelector((state) => state.auth.userInfo)
  const displayName = userInfo?.nickname?.trim() || userInfo?.nikename?.trim() || userInfo?.username?.trim() || '已登录用户'
  const isChatOpen = location.pathname === '/ai'
  const [isAiSending, setIsAiSending] = useState(false)
  const [inputValue, setInputValue] = useState('')
  const [headerSearchValue, setHeaderSearchValue] = useState('')
  const [sessions, setSessions] = useState<ChatSession[]>(() => [createChatSession()])
  const [activeSessionId, setActiveSessionId] = useState(() => sessions[0].id)
  const [historyOpen, setHistoryOpen] = useState(false)
  const [historyItems, setHistoryItems] = useState<AiConversationVO[]>([])
  const [historyKeyword, setHistoryKeyword] = useState('')
  const [historyLoading, setHistoryLoading] = useState(false)
  const [historyOperatingId, setHistoryOperatingId] = useState<string | null>(null)
  const [renameTarget, setRenameTarget] = useState<AiConversationVO | null>(null)
  const [renameValue, setRenameValue] = useState('')
  const [privateMessageUnreadCount, setPrivateMessageUnreadCount] = useState(0)
  const [notificationUnreadCount, setNotificationUnreadCount] = useState(0)
  const messagesContainerRef = useRef<HTMLDivElement | null>(null)
  const messageWsRef = useRef<WebSocket | null>(null)
  const aiAbortControllerRef = useRef<AbortController | null>(null)
  const messageWsReconnectTimerRef = useRef<number | null>(null)
  const privateMessageUnreadMapRef = useRef<Record<string, number>>({})
  const sessionsRef = useRef(sessions)
  const aiHistoryRequestedRef = useRef<Set<string>>(new Set())

  useEffect(() => {
    sessionsRef.current = sessions
  }, [sessions])

  const openCollabInviteTarget = (url: string) => {
    const target = getCollabInviteNavigateTarget(url)
    if (/^https?:\/\//i.test(target)) {
      window.location.href = target
      return
    }
    navigate(target)
  }

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme)
    document.body.setAttribute('data-theme', theme)
  }, [theme])

  useEffect(() => {
    const handleStorageChange = () => {
      dispatch(syncAuthState())
    }

    window.addEventListener('storage', handleStorageChange)
    dispatch(syncAuthState())

    return () => window.removeEventListener('storage', handleStorageChange)
  }, [dispatch])

  useEffect(() => {
    if (!isLoggedIn) {
      return
    }

    const syncCurrentProfile = async () => {
      try {
        const rawStoredUser = localStorage.getItem('codehub-user')
        let currentUser: Record<string, unknown> = {}

        if (rawStoredUser) {
          try {
            currentUser = JSON.parse(rawStoredUser) as Record<string, unknown>
          } catch {
            currentUser = {}
          }
        }

        const profile = await getCurrentUserProfile()
        localStorage.setItem(
          'codehub-user',
          JSON.stringify({
            userId: profile.userId ?? currentUser.userId,
            username: profile.username ?? currentUser.username,
            nickname: profile.nickname ?? currentUser.nickname,
            email: profile.email ?? currentUser.email,
            roles: profile.roles?.length ? profile.roles : currentUser.roles,
            avatarUrl: profile.avatarUrl || currentUser.avatarUrl || getAvatarWithFallback(profile),
            title: profile.title ?? currentUser.title,
            titleVerified: profile.titleVerified ?? currentUser.titleVerified,
            schoolVerified: profile.schoolVerified ?? currentUser.schoolVerified,
            companyVerified: profile.companyVerified ?? currentUser.companyVerified,
          }),
        )
        dispatch(syncAuthState())
      } catch (error) {
        console.error('sync current profile in layout error:', error)
      }
    }

    void syncCurrentProfile()
  }, [dispatch, isLoggedIn])

  useEffect(() => {
    // 换号/退出时取消输出并清空内存会话，避免展示上一位用户的信息。
    const timer = window.setTimeout(() => {
      aiAbortControllerRef.current?.abort()
      aiHistoryRequestedRef.current = new Set()
      const draft = createChatSession()
      setInputValue('')

      if (!isLoggedIn) {
        setSessions([draft])
        setActiveSessionId(draft.id)
        return
      }

      // Restore the durable history kept in MySQL alongside a fresh draft, so a
      // refresh no longer loses past conversations.
      void (async () => {
        try {
          const page = await listAiConversations({ pageNum: 1, pageSize: 50 })
          setSessions([draft, ...page.records.map(toHistorySession)])
        } catch (error) {
          console.error('load ai conversations error:', error)
          setSessions([draft])
        } finally {
          setActiveSessionId(draft.id)
        }
      })()
    }, 0)
    return () => {
      window.clearTimeout(timer)
      aiAbortControllerRef.current?.abort()
    }
  }, [isLoggedIn, userInfo?.userId])

  useEffect(() => {
    if (!isLoggedIn) {
      privateMessageUnreadMapRef.current = {}
      const resetTimer = window.setTimeout(() => setPrivateMessageUnreadCount(0), 0)
      return () => window.clearTimeout(resetTimer)
    }

    const syncPrivateMessageUnreadCount = async () => {
      try {
        const data = await listConversations({ pageNum: 1, pageSize: 50 })
        privateMessageUnreadMapRef.current = data.records.reduce<Record<string, number>>((result, conversation) => {
          if (conversation.id != null) {
            result[String(conversation.id)] = conversation.unreadCount || 0
          }
          return result
        }, {})
        setPrivateMessageUnreadCount(Object.values(privateMessageUnreadMapRef.current).reduce((sum, count) => sum + count, 0))
      } catch (error) {
        console.error('sync private message unread count error:', error)
      }
    }

    void syncPrivateMessageUnreadCount()

    const handlePrivateMessageWsEvent = (event: Event) => {
      const detail = (event as CustomEvent<PrivateMessageWsEvent>).detail
      if (detail.type === 'MESSAGE_NEW') {
        const conversationId = detail.conversationId != null ? String(detail.conversationId) : ''
        const messageData = detail.data && typeof detail.data === 'object' ? detail.data as MessageVO : undefined
        const collabInvite = parseCollabInviteMessage(messageData?.content)
        if (conversationId && String(detail.senderId) !== String(userInfo?.userId)) {
          privateMessageUnreadMapRef.current = {
            ...privateMessageUnreadMapRef.current,
            [conversationId]: (privateMessageUnreadMapRef.current[conversationId] || 0) + 1,
          }
          setPrivateMessageUnreadCount(Object.values(privateMessageUnreadMapRef.current).reduce((sum, count) => sum + count, 0))

          if (collabInvite) {
            const notificationKey = `collab-invite-${conversationId}-${messageData?.id || Date.now()}`
            const openInviteAndCloseNotification = () => {
              notification.destroy(notificationKey)
              openCollabInviteTarget(collabInvite.url)
            }

            notification.open({
              key: notificationKey,
              placement: 'topRight',
              duration: 5,
              message: '协同编辑邀请',
              description: collabInvite.title,
              btn: (
                <Button
                  type="primary"
                  size="small"
                  onClick={(event) => {
                    event.stopPropagation()
                    openInviteAndCloseNotification()
                  }}
                >
                  进入协同编辑
                </Button>
              ),
              onClick: openInviteAndCloseNotification,
            })
          }
        }
        return
      }

      if (detail.type === 'MESSAGE_READ') {
        const readData = detail.data as MessageReadEventData | null | undefined
        const conversationId = detail.conversationId != null ? String(detail.conversationId) : ''
        if (conversationId && String(readData?.userId) === String(userInfo?.userId)) {
          privateMessageUnreadMapRef.current = { ...privateMessageUnreadMapRef.current, [conversationId]: 0 }
          setPrivateMessageUnreadCount(Object.values(privateMessageUnreadMapRef.current).reduce((sum, count) => sum + count, 0))
        }
        return
      }

      if (detail.type === 'CONVERSATION_UPDATE') {
        const conversationData = detail.data as ConversationVO | null | undefined
        if (conversationData?.id != null && conversationData.unreadCount != null) {
          privateMessageUnreadMapRef.current = {
            ...privateMessageUnreadMapRef.current,
            [String(conversationData.id)]: conversationData.unreadCount,
          }
          setPrivateMessageUnreadCount(Object.values(privateMessageUnreadMapRef.current).reduce((sum, count) => sum + count, 0))
        }
      }
    }

    window.addEventListener(PRIVATE_MESSAGE_WS_EVENT, handlePrivateMessageWsEvent)

    return () => {
      window.removeEventListener(PRIVATE_MESSAGE_WS_EVENT, handlePrivateMessageWsEvent)
    }
  }, [isLoggedIn, userInfo?.userId])

  useEffect(() => {
    if (!isLoggedIn) {
      const resetTimer = window.setTimeout(() => setNotificationUnreadCount(0), 0)
      return () => window.clearTimeout(resetTimer)
    }

    const syncNotificationUnreadCount = async () => {
      try {
        const count = await getNotificationUnreadCount()
        setNotificationUnreadCount(count)
      } catch (error) {
        console.error('sync notification unread count error:', error)
      }
    }

    void syncNotificationUnreadCount()

    const handleNotificationRead = () => {
      void syncNotificationUnreadCount()
    }

    const handleCommentReply = (event: Event) => {
      const detail = (event as CustomEvent<PrivateMessageWsEvent>).detail
      if (detail.type === 'COMMENT_REPLY') {
        setNotificationUnreadCount((current) => current + 1)
      }
    }

    window.addEventListener('codehub-notification-read', handleNotificationRead)
    window.addEventListener(PRIVATE_MESSAGE_WS_EVENT, handleCommentReply)

    return () => {
      window.removeEventListener('codehub-notification-read', handleNotificationRead)
      window.removeEventListener(PRIVATE_MESSAGE_WS_EVENT, handleCommentReply)
    }
  }, [isLoggedIn])

  useEffect(() => {
    const token = isLoggedIn ? getStoredMessageToken() : ''

    const clearReconnectTimer = () => {
      if (messageWsReconnectTimerRef.current != null) {
        window.clearTimeout(messageWsReconnectTimerRef.current)
        messageWsReconnectTimerRef.current = null
      }
    }

    const closeMessageWs = () => {
      clearReconnectTimer()
      messageWsRef.current?.close()
      messageWsRef.current = null
    }

    if (!token) {
      closeMessageWs()
      return
    }

    let stopped = false

    const connectMessageWs = () => {
      clearReconnectTimer()
      const wsUrl = buildMessagesWsUrl(token)
      console.info('private message ws connecting', wsUrl)
      const ws = new WebSocket(wsUrl)
      messageWsRef.current = ws
      window.__codehubMessageWs = ws
      window.__codehubMessageWsUrl = wsUrl

      ws.onopen = () => {
        console.info('service-message websocket connected', {
          url: wsUrl,
          readyState: ws.readyState,
          userId: userInfo?.userId,
        })
        window.dispatchEvent(new CustomEvent('codehub-private-message-ws-status', { detail: { connected: true, url: wsUrl } }))
      }

      ws.onmessage = (event) => {
        console.info('service-message websocket raw', event.data)
        if (event.data === 'pong') return
        try {
          const parsedEvent = JSON.parse(event.data) as PrivateMessageWsEvent | { data?: PrivateMessageWsEvent }
          const messageEvent = 'type' in parsedEvent ? parsedEvent : parsedEvent.data
          if (!messageEvent?.type) {
            console.warn('private message ws unknown payload', parsedEvent)
            return
          }
          console.info('private message ws event', messageEvent)
          dispatchPrivateMessageWsEvent(messageEvent)
        } catch (error) {
          console.error('parse private message ws event error:', error)
        }
      }

      ws.onerror = (event) => {
        console.error('service-message websocket error', event)
        ws.close()
      }

      ws.onclose = (event) => {
        console.warn('service-message websocket closed', event.code, event.reason)
        window.dispatchEvent(new CustomEvent('codehub-private-message-ws-status', { detail: { connected: false, code: event.code, reason: event.reason } }))
        if (!stopped && event.code !== 1002) {
          messageWsReconnectTimerRef.current = window.setTimeout(connectMessageWs, 3000)
        }
      }
    }

    connectMessageWs()

    return () => {
      stopped = true
      closeMessageWs()
      window.__codehubMessageWs = undefined
      window.__codehubMessageWsUrl = undefined
    }
  }, [isLoggedIn, userInfo?.userId])

  const activeSession = useMemo(
    () => sessions.find((session) => session.id === activeSessionId) ?? sessions[0],
    [activeSessionId, sessions],
  )

  const messages = useMemo(() => activeSession?.messages ?? [], [activeSession])

  useEffect(() => {
    if (!isChatOpen) {
      return
    }

    const container = messagesContainerRef.current
    if (!container) {
      return
    }

    requestAnimationFrame(() => {
      container.scrollTo({ top: container.scrollHeight, behavior: 'smooth' })
    })
  }, [isChatOpen, messages])

  useEffect(() => {
    if (!activeSessionId) {
      return
    }
    const session = sessionsRef.current.find((item) => item.id === activeSessionId)
    // Drafts and already-fetched sessions carry historyLoaded:true; only restore
    // the messages of a server session the first time it is opened.
    if (!session || session.historyLoaded !== false || aiHistoryRequestedRef.current.has(activeSessionId)) {
      return
    }
    aiHistoryRequestedRef.current.add(activeSessionId)

    void (async () => {
      try {
        const page = await listAiMessages(activeSessionId, { pageNum: 1, pageSize: 200 })
        const restored: ChatMessage[] = page.records.map((record) => ({
          id: `history-${record.id}`,
          role: record.role === 'user' ? 'user' : 'assistant',
          content: record.content,
          time: formatHistoryMessageTime(record.createdAt),
        }))
        setSessions((prev) => prev.map((item) => (item.id === activeSessionId
          ? { ...item, messages: restored, historyLoaded: true }
          : item)))
      } catch (error) {
        console.error('load ai messages error:', error)
        // Allow a retry on the next selection rather than leaving the session blank.
        aiHistoryRequestedRef.current.delete(activeSessionId)
      }
    })()
  }, [activeSessionId])

  const loadHistory = useCallback(async (keyword: string) => {
    try {
      setHistoryLoading(true)
      const page = await listAiConversations({ pageNum: 1, pageSize: 50, keyword: keyword.trim() || undefined })
      setHistoryItems(page.records)
    } catch (error) {
      console.error('load ai conversations error:', error)
      notification.error({ message: '会话记录加载失败', description: error instanceof Error ? error.message : '请稍后重试' })
    } finally {
      setHistoryLoading(false)
    }
  }, [])

  const openHistorySession = (record: AiConversationVO) => {
    setSessions((prev) => (prev.some((item) => item.id === record.conversationId)
      ? prev
      : [toHistorySession(record), ...prev]))
    setActiveSessionId(record.conversationId)
    setHistoryOpen(false)
  }

  const handleRenameConversation = (record: AiConversationVO) => {
    setRenameTarget(record)
    setRenameValue(record.title?.trim() || '历史会话')
  }

  const submitRename = async () => {
    const target = renameTarget
    const title = renameValue.trim()
    if (!target || !title) {
      return
    }
    try {
      setHistoryOperatingId(target.conversationId)
      await renameAiConversation(target.conversationId, title)
      setHistoryItems((prev) => prev.map((item) => (item.conversationId === target.conversationId ? { ...item, title } : item)))
      setSessions((prev) => prev.map((item) => (item.id === target.conversationId ? { ...item, title } : item)))
      setRenameTarget(null)
      notification.success({ message: '已重命名', duration: 2 })
    } catch (error) {
      notification.error({ message: '重命名失败', description: error instanceof Error ? error.message : '请稍后重试' })
    } finally {
      setHistoryOperatingId(null)
    }
  }

  const handleDeleteConversation = (record: AiConversationVO) => {
    Modal.confirm({
      title: '确认删除这个会话？',
      content: '删除后该会话及其全部消息将从服务器移除，且不可恢复。',
      okText: '删除',
      okButtonProps: { danger: true },
      cancelText: '取消',
      onOk: async () => {
        try {
          setHistoryOperatingId(record.conversationId)
          await deleteAiConversation(record.conversationId)
          aiHistoryRequestedRef.current.delete(record.conversationId)
          setHistoryItems((prev) => prev.filter((item) => item.conversationId !== record.conversationId))
          if (activeSessionId === record.conversationId) {
            const draft = createChatSession()
            setSessions((prev) => [draft, ...prev.filter((item) => item.id !== record.conversationId)])
            setActiveSessionId(draft.id)
            setInputValue('')
          } else {
            setSessions((prev) => {
              const next = prev.filter((item) => item.id !== record.conversationId)
              return next.length ? next : [createChatSession()]
            })
          }
          notification.success({ message: '已删除会话', duration: 2 })
        } catch (error) {
          notification.error({ message: '删除失败', description: error instanceof Error ? error.message : '请稍后重试' })
        } finally {
          setHistoryOperatingId(null)
        }
      },
    })
  }

  const historyPanel = (
    <div className="ai-history-panel">
      <Input.Search
        className="ai-history-panel__search"
        placeholder="搜索会话"
        allowClear
        value={historyKeyword}
        onChange={(event) => setHistoryKeyword(event.target.value)}
        onSearch={(value) => { void loadHistory(value) }}
      />
      <div className="ai-history-panel__list">
        {historyLoading ? (
          <div className="ai-history-panel__state"><Spin size="small" /></div>
        ) : historyItems.length ? (
          historyItems.map((item) => (
            <div
              key={item.conversationId}
              className={`ai-history-panel__item${item.conversationId === activeSessionId ? ' is-active' : ''}`}
            >
              <button
                type="button"
                className="ai-history-panel__item-main"
                onClick={() => openHistorySession(item)}
              >
                <span className="ai-history-panel__item-title">{item.title?.trim() || '历史会话'}</span>
                <span className="ai-history-panel__item-time">{formatHistoryTimestamp(item.updatedAt)}</span>
              </button>
              <span className="ai-history-panel__item-actions">
                <Tooltip title="重命名">
                  <Button
                    type="text"
                    size="small"
                    icon={<EditOutlined />}
                    aria-label="重命名会话"
                    disabled={historyOperatingId === item.conversationId}
                    onClick={() => handleRenameConversation(item)}
                  />
                </Tooltip>
                <Tooltip title="删除">
                  <Button
                    type="text"
                    size="small"
                    danger
                    icon={<DeleteOutlined />}
                    aria-label="删除会话"
                    disabled={historyOperatingId === item.conversationId}
                    onClick={() => handleDeleteConversation(item)}
                  />
                </Tooltip>
              </span>
            </div>
          ))
        ) : (
          <div className="ai-history-panel__state">
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description={historyKeyword.trim() ? '未找到匹配的会话' : '暂无历史会话'}
            />
          </div>
        )}
      </div>
    </div>
  )

  const handleLogout = () => {
    aiAbortControllerRef.current?.abort()
    // Invalidate the server-side session before dropping local state; a failed
    // call must not block the user from signing out.
    void logoutRequest().catch(() => undefined).finally(() => {
      dispatch(logout())
      window.dispatchEvent(new Event('storage'))
      navigate('/auth')
    })
  }

  const handleCreateSession = () => {
    const newSession = createChatSession()

    setSessions((prev) => [newSession, ...prev])
    setActiveSessionId(newSession.id)
    setInputValue('')
  }

  const handleSend = async (value?: string) => {
    const content = (value ?? inputValue).trim()
    if (!content || aiAbortControllerRef.current) {
      return
    }

    if (!isLoggedIn) {
      notification.warning({ message: '请先登录', description: '登录后才能使用 AI 学习助手。' })
      navigate('/auth')
      return
    }

    const now = Date.now()
    const userMessage: ChatMessage = {
      id: `user-${now}`,
      role: 'user',
      time: formatMessageTime(now),
      content,
    }
    const assistantMessageId = `assistant-${now + 1}`
    const assistantMessage: ChatMessage = {
      id: assistantMessageId,
      role: 'assistant',
      time: formatMessageTime(now + 1),
      content: '',
    }
    const sessionId = activeSessionId
    const controller = new AbortController()

    aiAbortControllerRef.current = controller
    setIsAiSending(true)
    setInputValue('')
    setSessions((prev) => prev.map((session) => {
      if (session.id !== sessionId) {
        return session
      }

      const nextTitle = session.messages.length <= 1 ? content.slice(0, 12) || session.title : session.title
      return {
        ...session,
        title: nextTitle,
        updatedAt: formatMessageTime(now),
        updatedAtValue: now,
        messages: [...session.messages, userMessage, assistantMessage],
      }
    }))

    const appendAssistantContent = (chunk: string) => {
      setSessions((prev) => prev.map((session) => {
        if (session.id !== sessionId) {
          return session
        }

        return {
          ...session,
          updatedAt: formatMessageTime(Date.now()),
          updatedAtValue: Date.now(),
          messages: session.messages.map((message) => message.id === assistantMessageId
            ? { ...message, content: message.content + chunk }
            : message),
        }
      }))
    }

    try {
      await streamAiChat(
        {
          conversationId: sessionId,
          message: content,
          stream: true,
          scene: 'learning-assistant',
        },
        {
          onToken: appendAssistantContent,
        },
        controller.signal,
      )

    } catch (error) {
      if ((error as Error)?.name === 'AbortError') {
        appendAssistantContent('\n\n已取消本次回答。')
      } else {
        const message = error instanceof Error ? error.message : 'AI 服务暂时不可用'
        appendAssistantContent(`\n\n请求失败：${message}`)
        notification.error({ message: 'AI 请求失败', description: message })
      }
    } finally {
      if (aiAbortControllerRef.current === controller) {
        aiAbortControllerRef.current = null
      }
      setIsAiSending(false)
    }
  }

  const profileMenuItems = useMemo<MenuProps['items']>(
    () => [
      {
        key: 'profile',
        icon: <UserOutlined />,
        label: '个人中心',
      },
      {
        key: 'messages',
        icon: <MessageOutlined />,
        label: (
          <span className="profile-menu-message-label">
            我的私信
            {privateMessageUnreadCount > 0 ? <span className="profile-menu-message-dot" /> : null}
          </span>
        ),
      },
      {
        key: 'comment-reply',
        icon: <BellOutlined />,
        label: (
          <span className="profile-menu-message-label">
            评论回复
            {notificationUnreadCount > 0 ? <span className="profile-menu-message-dot" /> : null}
          </span>
        ),
      },
      {
        type: 'divider',
      },
      {
        key: 'logout',
        icon: <LogoutOutlined />,
        label: '退出登录',
      },
    ],
    [notificationUnreadCount, privateMessageUnreadCount],
  )

  const publishMenuItems = useMemo<MenuProps['items']>(
    () => [
      {
        key: 'article',
        icon: <EditOutlined />,
        label: '文章',
      },
      {
        key: 'check-in',
        icon: <FormOutlined />,
        label: '打卡',
      },
      {
        key: 'question',
        icon: <CommentOutlined />,
        label: '提问',
      },
    ],
    [],
  )

  const handleProfileMenuClick: MenuProps['onClick'] = ({ key }) => {
    if (key === 'profile') {
      navigate('/profile')
      return
    }

    if (key === 'messages') {
      navigate('/messages')
      return
    }

    if (key === 'comment-reply') {
      navigate('/messages?section=notify')
      return
    }

    if (key === 'logout') {
      handleLogout()
    }
  }

  const handlePublishMenuClick: MenuProps['onClick'] = ({ key }) => {
    if (key === 'article') {
      navigate('/article/editor')
      return
    }

    if (key === 'check-in') {
      navigate('/check-in/editor')
      return
    }

    if (key === 'question') {
      navigate('/question/editor')
    }
  }

  const handleCopyMessage = async (content: string) => {
    try {
      await navigator.clipboard.writeText(content)
      notification.success({ message: '已复制回答', duration: 2 })
    } catch {
      notification.error({ message: '复制失败，请选择回答文字手动复制' })
    }
  }

  const isFullscreenEditor = window.location.pathname === '/article/editor'
    || window.location.pathname.startsWith('/article/editor/')
    || window.location.pathname === '/check-in/editor'
    || window.location.pathname.startsWith('/check-in/editor/')
    || window.location.pathname === '/question/editor'
    || window.location.pathname.startsWith('/question/editor/')

  const isWideContentPage = isFullscreenEditor || window.location.pathname.startsWith('/problem/')

  return (
    <div className="app-shell theme-transition">
      <ProLayout
        className={isFullscreenEditor ? 'app-layout app-layout--fullscreen-editor' : 'app-layout'}
        title="码途"
        logo={<img src="https://static-mp-2bb3fcb5-3d3f-4ca6-928b-db304b30cdfb.next.bspapp.com/logo.png" alt="logo" />}
        layout="top"
        fixedHeader
        breakpoint="lg"
        location={{ pathname: location.pathname }}
        route={{ routes: menuData }}
        menuItemRender={(item, dom) => <Link to={item.path ?? '/home'}>{dom}</Link>}
        token={{
          header: {
            colorBgHeader: 'var(--bg-secondary)',
            colorHeaderTitle: 'var(--text-primary)',
            colorTextMenu: 'var(--text-secondary)',
            colorTextMenuSelected: 'var(--primary)',
            colorTextMenuActive: 'var(--primary)',
            colorTextRightActionsItem: 'var(--text-secondary)',
            heightLayoutHeader: 68,
          },
        }}
        actionsRender={() => {
          const actions = [
            <Input
              key="search"
              allowClear
              prefix={<SearchOutlined />}
              placeholder="搜索技术、题目、教程"
              className="header-search"
              maxLength={SEARCH_KEYWORD_LIMIT}
              value={headerSearchValue}
              onChange={(event) => setHeaderSearchValue(event.target.value)}
              onPressEnter={() => {
                const normalized = headerSearchValue.trim()
                if (normalized) navigate(buildSearchPath(normalized))
              }}
            />,
            // <div key="theme" className="theme-icon-group">
            //   <Tooltip title="亮色模式">
            //     <Button
            //       type={theme === 'light' ? 'primary' : 'default'}
            //       shape="circle"
            //       className="theme-icon-button"
            //       icon={<BulbOutlined />}
            //       onClick={() => dispatch(setTheme('light'))}
            //     />
            //   </Tooltip>
            //   <Tooltip title="暗色模式">
            //     <Button
            //       type={theme === 'dark' ? 'primary' : 'default'}
            //       shape="circle"
            //       className="theme-icon-button"
            //       icon={<MoonOutlined />}
            //       onClick={() => dispatch(setTheme('dark'))}
            //     />
            //   </Tooltip>
            // </div>,
          ]

          if (!isLoggedIn) {
            actions.push(
              <Button key="login" type="primary" className="header-login-btn" onClick={() => navigate('/auth')}>
                登录
              </Button>,
            )
          } else {
            actions.push(
              <Dropdown
                key="publish"
                menu={{ items: publishMenuItems, onClick: handlePublishMenuClick }}
                trigger={['hover']}
                classNames={{ root: 'header-publish-dropdown' }}
              >
                <Button type="primary" className="header-publish-btn" icon={<PlusOutlined />}>
                  发布
                </Button>
              </Dropdown>,
            )

            actions.push(
              <Dropdown
                key="profile"
                menu={{ items: profileMenuItems, onClick: handleProfileMenuClick }}
                trigger={['click']}
              >
                <Space size={8} className="header-user-entry">
                  <Badge count={privateMessageUnreadCount} overflowCount={99} size="small">
                    <Avatar
                      src={getAvatarWithFallback({ avatarUrl: userInfo?.avatarUrl })}
                      size="small"
                    />
                  </Badge>
                  <div className="header-user-meta">
                    <div className="header-user-name">{displayName}</div>
                  </div>
                </Space>
              </Dropdown>,
            )
          }

          return actions
        }}
        headerTitleRender={(logo, title) => (
          <Space size={10} align="center">
            {logo}
            <span className="brand-title">{title}</span>
          </Space>
        )}
        avatarProps={false}
        onMenuHeaderClick={() => navigate('/home')}
      >
        <PageContainer ghost header={{ title: null, breadcrumb: undefined }}>
          <div className={isWideContentPage ? 'content-wrapper content-wrapper--fullscreen' : 'content-wrapper'}>
            {isChatOpen ? (
              <section className="ai-chat" aria-labelledby="ai-chat-title">
                <header className="ai-chat__header">
                  <div className="ai-chat__brand" id="ai-chat-title"><RobotOutlined /> 码途 AI <span>学习助手</span></div>
                  <Space size={8}>
                    <Popover
                      trigger="click"
                      placement="bottomRight"
                      open={historyOpen}
                      onOpenChange={(open) => {
                        setHistoryOpen(open)
                        if (open) {
                          void loadHistory(historyKeyword)
                        }
                      }}
                      content={historyPanel}
                      styles={{ container: { padding: 0 } }}
                    >
                      <Button type="text" icon={<HistoryOutlined />} aria-label="历史会话记录" title="历史会话保存在服务器，可跨设备查看、重命名或删除">会话记录</Button>
                    </Popover>
                    <Button type="text" icon={<PlusOutlined />} onClick={handleCreateSession}>新对话</Button>
                  </Space>
                </header>

                <div className="ai-chat__panel">
                  <div className="ai-chat__messages" ref={messagesContainerRef} role="log" aria-label="AI 对话消息" aria-busy={isAiSending}>
                    <div className="ai-chat__conversation">
                      {messages.length === 0 ? (
                        <div className="ai-chat__welcome">
                          <RobotOutlined />
                          <h1>你好，有什么可以帮你？</h1>
                          <p>一起拆解难题，让每一步学习更清晰。</p>
                          <div className="ai-chat__suggestions">
                            {['帮我制定一份学习计划', '讲解一道算法题的解题思路', '帮我准备前端面试'].map((prompt) => (
                              <button type="button" key={prompt} onClick={() => setInputValue(prompt)}>{prompt}</button>
                            ))}
                          </div>
                        </div>
                      ) : null}
                      {messages.map((message, index) => (
                        <div key={message.id} className={`chat-message chat-message--${message.role}`}>
                          <div className="chat-message__bubble">
                            <div className="chat-message__content">
                              {message.role === 'assistant' && message.content ? (
                                <MDEditor.Markdown
                                  source={message.content}
                                  className="chat-message__markdown"
                                  components={markdownImageComponents}
                                />
                              ) : (
                                message.content || (message.role === 'assistant' && isAiSending ? '正在思考…' : '')
                              )}
                            </div>
                            {message.role === 'assistant' && message.content && !isAiSending ? (
                              <div className="chat-message__actions">
                                <Tooltip title="复制回答"><Button type="text" size="small" icon={<CopyOutlined />} aria-label="复制回答" onClick={() => { void handleCopyMessage(message.content) }} /></Tooltip>
                                {messages[index - 1]?.role === 'user' ? (
                                  <Tooltip title="再问一次"><Button type="text" size="small" icon={<ReloadOutlined />} aria-label="再问一次" onClick={() => { void handleSend(messages[index - 1].content) }} /></Tooltip>
                                ) : null}
                              </div>
                            ) : null}
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>

                  <div className="ai-chat__input-area">
                    <div className="ai-chat__composer">
                      <Input.TextArea
                        value={inputValue}
                        onChange={(event) => setInputValue(event.target.value)}
                        placeholder="向码途 AI 提问"
                        aria-label="向码途 AI 提问"
                        autoSize={{ minRows: 2, maxRows: 6 }}
                        maxLength={12000}
                        onPressEnter={(event) => {
                          if (!event.shiftKey && !event.nativeEvent.isComposing) {
                            event.preventDefault()
                            void handleSend()
                          }
                        }}
                      />
                      <div className="ai-chat__composer-footer">
                        <div className="ai-chat__composer-tools">
                          <Tooltip title="新对话"><Button type="text" shape="circle" icon={<PlusOutlined />} aria-label="新对话" onClick={handleCreateSession} /></Tooltip>
                          <span><RobotOutlined /> 学习助手</span>
                        </div>
                        <div className="ai-chat__send-tools">
                          <span className="ai-chat__keyboard-hint">Enter 发送 · Shift + Enter 换行</span>
                          {isAiSending ? (
                            <Button shape="circle" icon={<StopOutlined />} aria-label="停止生成" title="停止生成" onClick={() => aiAbortControllerRef.current?.abort()} />
                          ) : (
                            <Button type="primary" shape="circle" icon={<ArrowUpOutlined />} aria-label="发送消息" title="发送消息" disabled={!inputValue.trim()} onClick={() => { void handleSend() }} />
                          )}
                        </div>
                      </div>
                    </div>
                    <div className="ai-chat__disclaimer">内容由 AI 生成，可能不准确，请注意核实</div>
                  </div>
                </div>
              </section>
            ) : <Outlet />}
          </div>
        </PageContainer>
      </ProLayout>
      {!isFullscreenEditor ? (
        <Link to="/feedback" className="floating-feedback" aria-label="意见反馈">
          <FormOutlined />
          <span>反馈</span>
        </Link>
      ) : null}
      <Modal
        title="重命名会话"
        open={renameTarget !== null}
        okText="保存"
        cancelText="取消"
        confirmLoading={historyOperatingId !== null && historyOperatingId === renameTarget?.conversationId}
        onOk={() => { void submitRename() }}
        onCancel={() => setRenameTarget(null)}
        destroyOnHidden
      >
        <Input
          value={renameValue}
          onChange={(event) => setRenameValue(event.target.value)}
          maxLength={100}
          showCount
          placeholder="请输入会话名称"
          aria-label="会话名称"
          onPressEnter={() => { void submitRename() }}
        />
      </Modal>
    </div>
  )
}
