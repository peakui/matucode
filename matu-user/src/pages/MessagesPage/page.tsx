import {
  ArrowLeftOutlined,
  BellFilled,
  BellOutlined,
  DeleteOutlined,
  MessageOutlined,
  MoreOutlined,
  PushpinFilled,
  PushpinOutlined,
  SendOutlined,
  UndoOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { Avatar, Badge, Button, Card, Dropdown, Empty, Input, Modal, Space, Spin, Tag, message } from 'antd'
import type { MenuProps } from 'antd'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { getUserProfile } from '../../api/authProfile'
import {
  deleteMessage,
  getConversationDetail,
  getNotificationUnreadCount,
  listConversations,
  listMessages,
  listNotifications,
  markConversationRead,
  markNotificationsRead,
  muteConversation,
  recallMessage,
  sendTextMessage,
  topConversation,
  unmuteConversation,
  untopConversation,
} from '../../api/message'
import type { AuthProfileVO } from '../../api/type/loginTypings'
import type { CommentReplyNotificationData, ConversationMemberVO, ConversationVO, MessageReadEventData, MessageVO, NotificationVO, PrivateMessageWsEvent } from '../../api/type/messageTypings'
import { getAvatarWithFallback } from '../../utils/avatar'
import { getCollabInviteNavigateTarget, getCollabInvitePreviewText, parseCollabInviteMessage } from '../../utils/collabInviteMessage'
import './page.scss'

const formatTime = (value?: string | null) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

const getStoredCurrentUserId = () => {
  const directUserId = localStorage.getItem('codehub-user-id')?.trim() || localStorage.getItem('userId')?.trim()
  if (directUserId) return directUserId
  const rawUser = localStorage.getItem('codehub-user')
  if (!rawUser) return ''
  try {
    const parsed = JSON.parse(rawUser) as { userId?: string | number }
    return parsed.userId != null ? String(parsed.userId) : ''
  } catch {
    return ''
  }
}

const getOtherMember = (members?: ConversationMemberVO[] | null, currentUserId?: string) => {
  if (!members?.length) return null
  return members.find((member) => member.userId != null && String(member.userId) !== currentUserId) || members[0]
}

const getUserDisplayName = (profile?: AuthProfileVO | ConversationMemberVO) => {
  return profile?.nickname?.trim() || profile?.username?.trim() || (profile?.userId != null ? `用户 ${profile.userId}` : '')
}

const getConversationProfile = (conversation?: ConversationVO | null, currentUserId?: string, profileMap: Record<string, AuthProfileVO> = {}) => {
  const otherMember = getOtherMember(conversation?.members, currentUserId)
  if (otherMember?.userId == null) return undefined
  return profileMap[String(otherMember.userId)] || otherMember
}

const getConversationTitle = (conversation?: ConversationVO | null, currentUserId?: string, profileMap: Record<string, AuthProfileVO> = {}) => {
  if (!conversation) return '请选择会话'
  if (conversation.conversationType === 2) return conversation.conversationName?.trim() || '群聊'
  const otherMember = getOtherMember(conversation.members, currentUserId)
  if (otherMember?.userId != null) {
    const userId = String(otherMember.userId)
    return getUserDisplayName(profileMap[userId] || otherMember) || `用户 ${userId}`
  }
  return conversation.conversationName?.trim() || '用户信息加载中'
}

const getMessageContent = (item?: MessageVO | null) => {
  if (!item) return '暂无消息'
  if (item.isRecall === 1) return '消息已撤回'
  if (item.isDeleted === 1) return '消息已删除'
  if (item.messageType === 1) return getCollabInvitePreviewText(item.content) || item.content || '文本消息'
  return item.fileName || '暂不支持的消息类型'
}

const PRIVATE_MESSAGE_WS_EVENT = 'codehub-private-message-ws'

const getNotificationSourceLabel = (sourceType?: string) => {
  if (sourceType === 'check') return '打卡'
  if (sourceType === 'question') return '问题'
  if (sourceType === 'answer') return '回答'
  return '文章'
}

const getNotificationPath = (notification: NotificationVO) => {
  if (notification.sourceId == null) return undefined
  const encoded = encodeURIComponent(String(notification.sourceId))
  if (notification.sourceType === 'post') return `/article/${encoded}`
  if (notification.sourceType === 'check') return `/check-in/${encoded}`
  if (notification.sourceType === 'question' || notification.sourceType === 'answer') return `/qa/question/${encoded}`
  return undefined
}

const upsertConversation = (items: ConversationVO[], conversation: ConversationVO) => {
  const id = String(conversation.id)
  const exists = items.some((item) => String(item.id) === id)
  const next = exists ? items.map((item) => (String(item.id) === id ? { ...item, ...conversation, members: conversation.members || item.members } : item)) : [conversation, ...items]
  return next.sort((a, b) => {
    if ((a.isTop || 0) !== (b.isTop || 0)) return (b.isTop || 0) - (a.isTop || 0)
    return new Date(b.lastMessageTime || b.updatedAt || b.createdAt || 0).getTime() - new Date(a.lastMessageTime || a.updatedAt || a.createdAt || 0).getTime()
  })
}

const isSameMessage = (source: MessageVO, target: MessageVO) => {
  if (source.id != null && target.id != null && String(source.id) === String(target.id)) return true
  if (String(source.conversationId) !== String(target.conversationId)) return false
  if (String(source.senderId) !== String(target.senderId)) return false
  if ((source.content || '') !== (target.content || '')) return false

  const sourceTime = source.createdAt ? new Date(source.createdAt).getTime() : 0
  const targetTime = target.createdAt ? new Date(target.createdAt).getTime() : 0
  return Boolean(sourceTime && targetTime && Math.abs(sourceTime - targetTime) < 3000)
}

const mergeMessage = (items: MessageVO[], messageItem: MessageVO) => {
  const index = items.findIndex((item) => isSameMessage(item, messageItem))
  return index >= 0 ? items.map((item, itemIndex) => (itemIndex === index ? { ...item, ...messageItem } : item)) : [...items, messageItem]
}

export function MessagesPage() {
  const navigate = useNavigate()
  const { conversationId } = useParams<{ conversationId?: string }>()
  const [searchParams] = useSearchParams()
  const currentUserId = useMemo(() => getStoredCurrentUserId(), [])
  const [conversations, setConversations] = useState<ConversationVO[]>([])
  const [activeConversation, setActiveConversation] = useState<ConversationVO | null>(null)
  const [messages, setMessages] = useState<MessageVO[]>([])
  const [conversationLoading, setConversationLoading] = useState(false)
  const [messageLoading, setMessageLoading] = useState(false)
  const [memberProfiles, setMemberProfiles] = useState<Record<string, AuthProfileVO>>({})
  const memberProfilesRef = useRef<Record<string, AuthProfileVO>>({})
  const [sending, setSending] = useState(false)
  const [inputValue, setInputValue] = useState('')
  const messageListRef = useRef<HTMLDivElement | null>(null)
  const messagesEndRef = useRef<HTMLDivElement | null>(null)
  const [activeSection, setActiveSection] = useState<'chat' | 'notify'>(() => (searchParams.get('section') === 'notify' ? 'notify' : 'chat'))
  const [notifications, setNotifications] = useState<NotificationVO[]>([])
  const [notificationLoading, setNotificationLoading] = useState(false)
  const [notificationUnread, setNotificationUnread] = useState(0)

  const activeConversationId = conversationId?.trim() || ''
  const activeTitle = getConversationTitle(activeConversation, currentUserId, memberProfiles)
  const activeOtherMember = getOtherMember(activeConversation?.members, currentUserId)
  const activeProfile = getConversationProfile(activeConversation, currentUserId, memberProfiles)

  const loadMemberProfiles = useCallback(async (items: ConversationVO[]) => {
    const userIds = Array.from(new Set(items
      .flatMap((item) => item.members || [])
      .map((member) => member.userId)
      .filter((userId): userId is string | number => userId != null && String(userId) !== currentUserId)
      .map(String)))

    const missingUserIds = userIds.filter((userId) => !memberProfilesRef.current[userId])
    if (!missingUserIds.length) return

    const profiles = await Promise.allSettled(missingUserIds.map((userId) => getUserProfile(userId)))
    setMemberProfiles((current) => {
      const next = { ...current }
      profiles.forEach((result, index) => {
        if (result.status === 'fulfilled') {
          next[missingUserIds[index]] = result.value
        }
      })
      memberProfilesRef.current = next
      return next
    })
  }, [currentUserId])

  const loadConversations = useCallback(async () => {
    try {
      setConversationLoading(true)
      const data = await listConversations({ pageNum: 1, pageSize: 50 })
      const records = data.records || []
      setConversations(records)
      const detailResults = await Promise.allSettled(records
        .filter((item) => item.id != null && !item.members?.length)
        .map((item) => getConversationDetail(item.id!)))
      const detailMap = new Map<string, ConversationVO>()
      detailResults.forEach((result) => {
        if (result.status === 'fulfilled' && result.value.id != null) {
          detailMap.set(String(result.value.id), result.value)
        }
      })
      const mergedRecords = records.map((item) => item.id != null && detailMap.has(String(item.id)) ? { ...item, members: detailMap.get(String(item.id))?.members || item.members } : item)
      setConversations(mergedRecords)
      void loadMemberProfiles(mergedRecords)
    } catch (error) {
      console.error('load conversations error:', error)
      message.error('私信会话加载失败，请稍后重试')
      setConversations([])
    } finally {
      setConversationLoading(false)
    }
  }, [loadMemberProfiles])

  const loadActiveConversation = useCallback(async () => {
    if (!activeConversationId) {
      setActiveConversation(null)
      setMessages([])
      return
    }

    try {
      setMessageLoading(true)
      const [detail, messagePage] = await Promise.all([
        getConversationDetail(activeConversationId),
        listMessages(activeConversationId, { pageNum: 1, pageSize: 50 }),
      ])
      const orderedMessages = [...(messagePage.records || [])].reverse()
      setActiveConversation(detail)
      setMessages(orderedMessages)
      void loadMemberProfiles([detail])
      const lastMessage = orderedMessages[orderedMessages.length - 1]
      await markConversationRead(activeConversationId, lastMessage?.id)
      window.dispatchEvent(new Event('codehub-private-message-read'))
      setConversations((current) => current.map((item) => (String(item.id) === String(activeConversationId) ? { ...item, unreadCount: 0 } : item)))
    } catch (error) {
      console.error('load active conversation error:', error)
      message.error('消息加载失败，请稍后重试')
      setActiveConversation(null)
      setMessages([])
    } finally {
      setMessageLoading(false)
    }
  }, [activeConversationId, loadMemberProfiles])

  useEffect(() => {
    void loadConversations()
  }, [loadConversations])

  useEffect(() => {
    void loadActiveConversation()
  }, [loadActiveConversation])

  const loadNotificationUnread = useCallback(async () => {
    try {
      const count = await getNotificationUnreadCount()
      setNotificationUnread(count)
    } catch (error) {
      console.error('load notification unread error:', error)
    }
  }, [])

  const loadNotifications = useCallback(async () => {
    try {
      setNotificationLoading(true)
      const data = await listNotifications({ pageNum: 1, pageSize: 30 })
      setNotifications(data.records || [])
    } catch (error) {
      console.error('load notifications error:', error)
      setNotifications([])
    } finally {
      setNotificationLoading(false)
    }
  }, [])

  useEffect(() => {
    void loadNotificationUnread()
  }, [loadNotificationUnread])

  useEffect(() => {
    if (searchParams.get('section') === 'notify') {
      setActiveSection('notify')
    }
  }, [searchParams])

  useEffect(() => {
    if (activeSection === 'notify') {
      void loadNotifications()
    }
  }, [activeSection, loadNotifications])

  const scrollMessagesToBottom = useCallback((behavior: ScrollBehavior = 'auto') => {
    const scrollToEnd = (scrollBehavior: ScrollBehavior) => {
      messagesEndRef.current?.scrollIntoView({ block: 'end', behavior: scrollBehavior })
      const container = messageListRef.current
      if (container) container.scrollTop = container.scrollHeight
    }

    requestAnimationFrame(() => {
      scrollToEnd(behavior)
      window.setTimeout(() => scrollToEnd('auto'), 50)
    })
  }, [])

  useEffect(() => {
    if (messageLoading) return
    scrollMessagesToBottom('auto')
  }, [activeConversationId, messageLoading, messages.length, scrollMessagesToBottom])

  useEffect(() => {
    const handlePrivateMessageWsEvent = (event: Event) => {
      const detail = (event as CustomEvent<PrivateMessageWsEvent>).detail
      const messageDataFromEvent = detail.data && typeof detail.data === 'object' ? detail.data as MessageVO : undefined
      const eventConversationId = detail.conversationId != null
        ? String(detail.conversationId)
        : messageDataFromEvent?.conversationId != null
          ? String(messageDataFromEvent.conversationId)
          : ''

      if (detail.type === 'MESSAGE_NEW') {
        const messageData = messageDataFromEvent
        if (!messageData || !eventConversationId) {
          console.warn('private message ws MESSAGE_NEW missing data', detail)
          return
        }

        if (eventConversationId === activeConversationId) {
          setMessages((current) => mergeMessage(current, messageData))
          void markConversationRead(activeConversationId, messageData.id)
          window.dispatchEvent(new Event('codehub-private-message-read'))
          scrollMessagesToBottom(String(messageData.senderId) === currentUserId ? 'smooth' : 'auto')
        }

        setConversations((current) => {
          const exists = current.some((item) => String(item.id) === eventConversationId)
          if (!exists) {
            return upsertConversation(current, {
              id: eventConversationId,
              lastMessage: messageData,
              lastMessageTime: messageData.createdAt || detail.timestamp || null,
              unreadCount: eventConversationId === activeConversationId || String(messageData.senderId) === currentUserId ? 0 : 1,
              conversationType: 1,
              members: null,
            })
          }
          return current.map((item) => (
            String(item.id) === eventConversationId
              ? {
                ...item,
                lastMessage: messageData,
                lastMessageTime: messageData.createdAt || detail.timestamp || item.lastMessageTime,
                unreadCount: eventConversationId === activeConversationId || String(messageData.senderId) === currentUserId ? 0 : (item.unreadCount || 0) + 1,
              }
              : item
          ))
        })
        return
      }

      if (detail.type === 'CONVERSATION_UPDATE') {
        const conversationData = detail.data as ConversationVO | null | undefined
        if (!conversationData?.id) return
        const conversationId = String(conversationData.id)
        setConversations((current) => upsertConversation(current, conversationData))
        if (conversationId === activeConversationId) {
          setActiveConversation((current) => current ? { ...current, ...conversationData, members: conversationData.members || current.members } : conversationData)
          if (conversationData.lastMessage) {
            setMessages((current) => mergeMessage(current, conversationData.lastMessage!))
            scrollMessagesToBottom(String(conversationData.lastMessage.senderId) === currentUserId ? 'smooth' : 'auto')
          }
        }
        void loadMemberProfiles([conversationData])
        return
      }

      if (detail.type === 'MESSAGE_READ') {
        const readData = detail.data as MessageReadEventData | null | undefined
        if (String(readData?.userId) === currentUserId && eventConversationId) {
          setConversations((current) => current.map((item) => (String(item.id) === eventConversationId ? { ...item, unreadCount: 0 } : item)))
        }
        return
      }

      if (detail.type === 'MESSAGE_RECALL' || detail.type === 'MESSAGE_DELETE') {
        const messageData = detail.data as MessageVO | null | undefined
        if (!messageData) return
        setMessages((current) => current.map((item) => (String(item.id) === String(messageData.id) ? { ...item, ...messageData } : item)))
        setConversations((current) => current.map((item) => (String(item.id) === eventConversationId && String(item.lastMessage?.id) === String(messageData.id) ? { ...item, lastMessage: messageData } : item)))
        return
      }

      if (detail.type === 'COMMENT_REPLY') {
        const notificationData = detail.data as CommentReplyNotificationData | null | undefined
        if (!notificationData?.notificationId) return
        const notification: NotificationVO = {
          id: notificationData.notificationId,
          type: 'comment_reply',
          sourceType: notificationData.sourceType,
          sourceId: notificationData.sourceId,
          sourceTitle: notificationData.sourceTitle,
          commentId: notificationData.commentId,
          fromUserId: notificationData.fromUserId,
          fromNickname: notificationData.fromNickname,
          fromAvatar: notificationData.fromAvatar,
          contentPreview: notificationData.contentPreview,
          isRead: 0,
          createdAt: notificationData.createdAt || detail.timestamp,
        }
        setNotifications((current) => (
          current.some((item) => String(item.id) === String(notification.id)) ? current : [notification, ...current]
        ))
        setNotificationUnread((current) => current + 1)
        message.info('收到一条新的评论回复')
      }
    }

    window.addEventListener(PRIVATE_MESSAGE_WS_EVENT, handlePrivateMessageWsEvent)
    return () => window.removeEventListener(PRIVATE_MESSAGE_WS_EVENT, handlePrivateMessageWsEvent)
  }, [activeConversationId, currentUserId, loadMemberProfiles, scrollMessagesToBottom])

  const handleSendMessage = async () => {
    const content = inputValue.trim()
    if (!activeConversationId) {
      message.warning('请先选择一个私信会话')
      return
    }
    if (!content) {
      message.warning('请输入消息内容')
      return
    }

    try {
      setSending(true)
      const sentMessage = await sendTextMessage(activeConversationId, content)
      setInputValue('')
      setMessages((current) => mergeMessage(current, sentMessage))
      setConversations((current) => current.map((item) => (
        String(item.id) === activeConversationId
          ? { ...item, lastMessage: sentMessage, lastMessageTime: sentMessage.createdAt || new Date().toISOString() }
          : item
      )))
      scrollMessagesToBottom('smooth')
    } catch (error) {
      console.error('send message error:', error)
      message.error('消息发送失败，请稍后重试')
    } finally {
      setSending(false)
    }
  }

  const handleToggleTop = async (conversation: ConversationVO) => {
    if (conversation.id == null) return
    try {
      if (conversation.isTop === 1) {
        await untopConversation(conversation.id)
        message.success('已取消置顶')
      } else {
        await topConversation(conversation.id)
        message.success('已置顶')
      }
      await loadConversations()
      if (String(conversation.id) === activeConversationId) await loadActiveConversation()
    } catch (error) {
      console.error('toggle top conversation error:', error)
      message.error('会话置顶设置失败，请稍后重试')
    }
  }

  const handleToggleMute = async (conversation: ConversationVO) => {
    if (conversation.id == null) return
    try {
      if (conversation.isMuted === 1) {
        await unmuteConversation(conversation.id)
        message.success('已取消免打扰')
      } else {
        await muteConversation(conversation.id)
        message.success('已开启免打扰')
      }
      await loadConversations()
      if (String(conversation.id) === activeConversationId) await loadActiveConversation()
    } catch (error) {
      console.error('toggle mute conversation error:', error)
      message.error('会话免打扰设置失败，请稍后重试')
    }
  }

  const handleRecallMessage = (item: MessageVO) => {
    const messageId = item.id
    if (messageId == null) return
    Modal.confirm({
      title: '确认撤回这条消息？',
      content: '撤回后对方将看到“消息已撤回”。',
      okText: '撤回',
      cancelText: '取消',
      onOk: async () => {
        try {
          await recallMessage(messageId)
          setMessages((current) => current.map((row) => (
            String(row.id) === String(messageId) ? { ...row, isRecall: 1, recallTime: new Date().toISOString() } : row
          )))
          message.success('已撤回')
          if (activeConversationId) await loadActiveConversation()
        } catch (error) {
          console.error('recall message error:', error)
          message.error('撤回失败，请稍后重试')
        }
      },
    })
  }

  const handleDeleteMessage = (item: MessageVO) => {
    const messageId = item.id
    if (messageId == null) return
    Modal.confirm({
      title: '确认删除这条消息？',
      content: '删除后该消息将从当前会话中移除。',
      okText: '删除',
      okButtonProps: { danger: true },
      cancelText: '取消',
      onOk: async () => {
        try {
          await deleteMessage(messageId)
          setMessages((current) => current.filter((row) => String(row.id) !== String(messageId)))
          message.success('已删除')
          if (activeConversationId) await loadActiveConversation()
        } catch (error) {
          console.error('delete message error:', error)
          message.error('删除失败，请稍后重试')
        }
      },
    })
  }

  const getMessageMenuItems = (item: MessageVO): MenuProps['items'] => [
    {
      key: 'recall',
      icon: <UndoOutlined />,
      label: '撤回',
      disabled: item.isDeleted === 1 || item.isRecall === 1,
      onClick: () => handleRecallMessage(item),
    },
    {
      key: 'delete',
      icon: <DeleteOutlined />,
      label: '删除',
      danger: true,
      disabled: item.isDeleted === 1,
      onClick: () => handleDeleteMessage(item),
    },
  ]

  const getConversationMenuItems = (conversation: ConversationVO): MenuProps['items'] => [
    {
      key: 'top',
      icon: conversation.isTop === 1 ? <PushpinFilled /> : <PushpinOutlined />,
      label: conversation.isTop === 1 ? '取消置顶' : '置顶会话',
      onClick: () => void handleToggleTop(conversation),
    },
    {
      key: 'mute',
      icon: conversation.isMuted === 1 ? <BellFilled /> : <BellOutlined />,
      label: conversation.isMuted === 1 ? '取消免打扰' : '免打扰',
      onClick: () => void handleToggleMute(conversation),
    },
  ]

  const openCollabInvite = (url: string) => {
    const target = getCollabInviteNavigateTarget(url)
    if (/^https?:\/\//i.test(target)) {
      window.location.href = target
      return
    }
    navigate(target)
  }

  const renderMessageContent = (item: MessageVO) => {
    const invite = parseCollabInviteMessage(item.content)
    if (!invite) {
      return getMessageContent(item)
    }

    return (
      <div className="message-collab-invite">
        <div className="message-collab-invite__title">协同编辑邀请</div>
        <div className="message-collab-invite__desc">{invite.title}</div>
        <Button type="primary" size="small" onClick={() => openCollabInvite(invite.url)}>
          进入协同编辑
        </Button>
      </div>
    )
  }

  const renderConversationItem = (conversation: ConversationVO) => {
    const id = conversation.id != null ? String(conversation.id) : ''
    const active = id && id === activeConversationId
    const title = getConversationTitle(conversation, currentUserId, memberProfiles)
    const profile = getConversationProfile(conversation, currentUserId, memberProfiles)
    return (
      <div key={id || title} className={`messages-conversation-item ${active ? 'is-active' : ''}`} onClick={() => id && navigate(`/messages/${encodeURIComponent(id)}`)}>
        <Badge count={conversation.unreadCount || 0} size="small">
          <Avatar size={46} icon={<UserOutlined />} src={getAvatarWithFallback(profile || {})} />
        </Badge>
        <div className="messages-conversation-item__main">
          <div className="messages-conversation-item__top">
            <strong>{title}</strong>
            <span>{formatTime(conversation.lastMessageTime || conversation.updatedAt)}</span>
          </div>
          <div className="messages-conversation-item__preview">{getMessageContent(conversation.lastMessage)}</div>
          <Space size={6} className="messages-conversation-item__tags">
            {conversation.isTop === 1 ? <Tag color="blue">置顶</Tag> : null}
            {conversation.isMuted === 1 ? <Tag>免打扰</Tag> : null}
          </Space>
        </div>
        <Dropdown menu={{ items: getConversationMenuItems(conversation) }} trigger={['click']}>
          <Button type="text" icon={<MoreOutlined />} onClick={(event) => event.stopPropagation()} />
        </Dropdown>
      </div>
    )
  }

  const renderNotifyEntry = () => (
    <div
      role="button"
      tabIndex={0}
      className="messages-conversation-item messages-conversation-item--notify"
      onClick={() => setActiveSection('notify')}
      onKeyDown={(event) => {
        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault()
          setActiveSection('notify')
        }
      }}
    >
      <Badge count={notificationUnread} size="small">
        <Avatar size={46} icon={<BellOutlined />} />
      </Badge>
      <div className="messages-conversation-item__main">
        <div className="messages-conversation-item__top">
          <strong>评论回复</strong>
          {notificationUnread > 0 ? <span>未读</span> : null}
        </div>
        <div className="messages-conversation-item__preview">别人回复了你的评论时会出现在这里</div>
      </div>
    </div>
  )

  const handleNotificationClick = async (notification: NotificationVO) => {
    if (!notification.isRead && notification.id != null) {
      try {
        await markNotificationsRead({ ids: [notification.id] })
        setNotifications((current) => current.map((item) => (
          String(item.id) === String(notification.id) ? { ...item, isRead: 1 } : item
        )))
        setNotificationUnread((current) => Math.max(current - 1, 0))
        window.dispatchEvent(new Event('codehub-notification-read'))
      } catch (error) {
        console.error('mark notification read error:', error)
      }
    }
    const path = getNotificationPath(notification)
    if (path) navigate(path)
  }

  const renderNotificationItem = (notification: NotificationVO) => {
    const unread = !notification.isRead
    return (
      <div
        key={String(notification.id ?? notification.createdAt)}
        className={`messages-notification-item ${unread ? 'is-unread' : ''}`}
        onClick={() => void handleNotificationClick(notification)}
      >
        <Badge dot={unread} offset={[-2, 38]}>
          <Avatar size={46} icon={<UserOutlined />} src={getAvatarWithFallback({ avatarUrl: notification.fromAvatar ?? undefined })} />
        </Badge>
        <div className="messages-notification-item__main">
          <div className="messages-notification-item__top">
            <strong>{notification.fromNickname || '有人'}</strong>
            <span>{formatTime(notification.createdAt)}</span>
          </div>
          <div className="messages-notification-item__desc">
            回复了你的{getNotificationSourceLabel(notification.sourceType)}
            {notification.sourceTitle ? `「${notification.sourceTitle}」` : ''}
          </div>
          <div className="messages-notification-item__preview">{notification.contentPreview || '查看详情'}</div>
        </div>
      </div>
    )
  }

  return (
    <div className="messages-page">
      <Card className="content-card messages-card" variant="borderless">
        <div className="messages-section-nav">
          <button
            type="button"
            className={`messages-section-nav__item ${activeSection === 'chat' ? 'is-active' : ''}`}
            onClick={() => setActiveSection('chat')}
          >
            <MessageOutlined /> 私信
          </button>
          <button
            type="button"
            className={`messages-section-nav__item ${activeSection === 'notify' ? 'is-active' : ''}`}
            onClick={() => setActiveSection('notify')}
          >
            <BellOutlined /> 评论回复
            {notificationUnread > 0 ? <Badge count={notificationUnread} size="small" /> : null}
          </button>
        </div>
        <div className="messages-shell">
          {activeSection === 'chat' ? (
            <>
          <aside className={`messages-sidebar ${activeConversationId ? 'has-active' : ''}`}>
            <div className="messages-sidebar__header">
              <div>
                <div className="channel-hero__label">私信</div>
                <h2>我的私信</h2>
              </div>
            </div>
            <Input.Search placeholder="搜索会话" allowClear className="messages-search" />
            <div className="messages-conversation-list">
              {renderNotifyEntry()}
              {conversationLoading ? <div className="messages-loading"><Spin /></div> : conversations.length ? conversations.map(renderConversationItem) : <Empty description="暂无私信会话" />}
            </div>
          </aside>

          <section className={`messages-panel ${activeConversationId ? 'is-active' : ''}`}>
            {!activeConversationId ? (
              <div className="messages-empty-panel">
                <Avatar size={64} icon={<MessageOutlined />} />
                <h3>选择一个会话开始聊天</h3>
                <p>从左侧会话列表进入私信，或在用户主页点击“私信”创建单聊。</p>
              </div>
            ) : (
              <>
                <div className="messages-panel__header">
                  <Space>
                    <Button className="messages-panel__back" icon={<ArrowLeftOutlined />} onClick={() => navigate('/messages')}>返回</Button>
                    <Avatar size={44} icon={<UserOutlined />} src={getAvatarWithFallback(activeProfile || {})} />
                    <div>
                      <div className="messages-panel__title">{activeTitle}</div>
                      <div className="messages-panel__subtitle">文本私信</div>
                    </div>
                  </Space>
                  {activeOtherMember?.userId != null ? <Button onClick={() => navigate(`/profile/${encodeURIComponent(String(activeOtherMember.userId))}`)}>查看主页</Button> : null}
                </div>

                <div className="messages-list" ref={messageListRef}>
                  {messageLoading ? (
                    <div className="messages-loading"><Spin /></div>
                  ) : messages.length ? (
                    <>
                      {messages.map((item) => {
                        const isMine = currentUserId && String(item.senderId) === currentUserId
                        return (
                          <div key={String(item.id ?? item.createdAt)} className={`message-row ${isMine ? 'is-mine' : ''}`}>
                            <div className="message-bubble-wrap">
                              {isMine && item.id != null ? (
                                <Dropdown menu={{ items: getMessageMenuItems(item) }} trigger={['click']} placement="bottomLeft">
                                  <Button type="text" size="small" className="message-bubble-more" icon={<MoreOutlined />} />
                                </Dropdown>
                              ) : null}
                              <div className="message-bubble">{renderMessageContent(item)}</div>
                              <div className="message-time">{formatTime(item.createdAt)}</div>
                            </div>
                          </div>
                        )
                      })}
                      <div ref={messagesEndRef} />
                    </>
                  ) : <Empty description="暂无消息，发一条问候吧" />}
                </div>

                <div className="messages-composer">
                  <Input.TextArea
                    value={inputValue}
                    onChange={(event) => setInputValue(event.target.value)}
                    placeholder="输入私信内容，Enter 发送，Shift + Enter 换行"
                    autoSize={{ minRows: 3, maxRows: 6 }}
                    maxLength={1000}
                    onPressEnter={(event) => {
                      if (!event.shiftKey) {
                        event.preventDefault()
                        void handleSendMessage()
                      }
                    }}
                  />
                  <div className="messages-composer__footer">
                    <span>私聊</span>
                    <Button type="primary" icon={<SendOutlined />} loading={sending} onClick={() => void handleSendMessage()}>发送</Button>
                  </div>
                </div>
              </>
            )}
          </section>
            </>
          ) : (
            <>
              <aside className="messages-sidebar">
                <div className="messages-sidebar__header">
                  <div>
                    <div className="channel-hero__label">通知</div>
                    <h2>评论回复</h2>
                  </div>
                </div>
                <div className="messages-conversation-list">
                  {notificationLoading ? <div className="messages-loading"><Spin /></div> : notifications.length ? notifications.map(renderNotificationItem) : <Empty description="暂无评论回复" />}
                </div>
              </aside>
              <section className="messages-panel">
                <div className="messages-empty-panel">
                  <Avatar size={64} icon={<BellOutlined />} />
                  <h3>评论回复通知</h3>
                  <p>别人评论你的内容或回复你的评论时，会在这里展示。点击通知可前往对应内容。</p>
                </div>
              </section>
            </>
          )}
        </div>
      </Card>
    </div>
  )
}
