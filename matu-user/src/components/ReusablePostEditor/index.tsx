import axios from 'axios'
import { ArrowLeftOutlined, DisconnectOutlined, LinkOutlined, SaveOutlined, SendOutlined, TeamOutlined, UserAddOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Col, Input, InputNumber, Row, Select, Skeleton, Space, Tag, message } from 'antd'
import MDEditor, { commands, type ICommand } from '@uiw/react-md-editor'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { createCheckRecord, getCheckRecordDetail, updateCheckRecord } from '../../api/check'
import { createQuestion, getQuestionDetail, listQaCategories, updateQuestion } from '../../api/qa'
import { createPost, getPostDetail, listCategories, listTags, updatePost } from '../../api/post'
import { uploadImage } from '../../api/file'
import { createGroupConversation, createSingleConversation, sendTextMessage } from '../../api/message'
import { createOrGetCollabDocument, getConversationCollabDocument } from '../../api/collab'
import { markdownImageComponents } from '../../utils/markdownImageComponents'
import { extractFirstImageUrl, toPlainSummary } from '../../utils/markdownContent'
import { buildCollabInviteMessage } from '../../utils/collabInviteMessage'
import { applyCollabOperation, buildCollabTextOperations, CollabEditWebSocket, createCollabClientId } from '../../utils/collabWebSocket'
import { useAppSelector } from '../../store/hooks'
import type { PostCategoryVO, PostTagVO } from '../../api/type/postTypings'
import type {
  CollabJoinedPayload,
  CollabOnlineUserVO,
  CollabOperationAcceptedPayload,
  CollabOperationBroadcastPayload,
  CollabTextOperation,
} from '../../api/type/collabTypings'
import '../../pages/Article/ArticleEditorPage/page.scss'

const DRAFT_STATUS = 0
const PUBLISHED_STATUS = 1
const MARKDOWN_CONTENT_TYPE = 1
const CHECK_DRAFT_STATUS = 2
const CHECK_PUBLISHED_STATUS = 1

const getAxiosErrorMessage = (error: unknown, fallback: string) => {
  if (!axios.isAxiosError(error)) {
    return fallback
  }

  const responseData = error.response?.data

  if (typeof responseData === 'string' && responseData) {
    return responseData
  }

  if (responseData && typeof responseData === 'object') {
    const nestedData = responseData.data

    if (typeof nestedData === 'string' && nestedData) {
      return nestedData
    }

    return responseData.message || responseData.msg || responseData.error || fallback
  }

  return fallback
}

const buildSummary = (content: string) => toPlainSummary(content).slice(0, 120)

const EDITOR_IMAGE_UPLOAD_OWNER_TYPE = 1
const EDITOR_IMAGE_UPLOAD_IS_PUBLIC = 1

const getUploadedFileUrl = (payload: { url?: string; fileUrl?: string; fullUrl?: string; downloadUrl?: string }) => {
  return payload.fileUrl || payload.url || payload.fullUrl || payload.downloadUrl || ''
}

const toMarkdownImageAlt = (fileName: string) => {
  const nameWithoutExt = fileName.replace(/\.[^.]+$/, '')
  const normalizedName = nameWithoutExt.replace(/[[\]\r\n]/g, ' ').replace(/\s+/g, ' ').trim()

  return normalizedName || 'image'
}

const buildMarkdownImage = (file: File, imageUrl: string) => `![${toMarkdownImageAlt(file.name)}](${imageUrl})`

const pickImageFile = () => new Promise<File | null>((resolve) => {
  const input = document.createElement('input')
  let settled = false

  const cleanup = () => {
    window.removeEventListener('focus', handleWindowFocus)
    input.remove()
  }

  const finish = (file: File | null) => {
    if (settled) {
      return
    }

    settled = true
    cleanup()
    resolve(file)
  }

  function handleWindowFocus() {
    window.setTimeout(() => {
      if (!input.files?.length) {
        finish(null)
      }
    }, 300)
  }

  input.type = 'file'
  input.accept = 'image/*'
  input.style.display = 'none'
  input.addEventListener('change', () => finish(input.files?.[0] || null), { once: true })
  window.addEventListener('focus', handleWindowFocus, { once: true })
  document.body.appendChild(input)
  input.click()
})

const editorImageUploadCommand: ICommand = {
  ...commands.image,
  buttonProps: {
    ...(commands.image.buttonProps || {}),
    'aria-label': '上传图片',
    title: '上传图片',
  },
  execute: (_state, api) => {
    void (async () => {
      const file = await pickImageFile()

      if (!file) {
        return
      }

      if (!file.type.startsWith('image/')) {
        message.warning('请选择图片文件')
        return
      }

      const uploadMessageKey = 'editor-image-upload'
      message.open({ key: uploadMessageKey, type: 'loading', content: '图片上传中...', duration: 0 })

      try {
        const uploadedFile = await uploadImage(file, {
          ownerType: EDITOR_IMAGE_UPLOAD_OWNER_TYPE,
          isPublic: EDITOR_IMAGE_UPLOAD_IS_PUBLIC,
          onProgress: (percent, status) => {
            message.open({ key: uploadMessageKey, type: 'loading', content: `${status} ${percent}%`, duration: 0 })
          },
        })
        const imageUrl = getUploadedFileUrl(uploadedFile)

        if (!imageUrl) {
          throw new Error('上传成功，但未返回可用的图片地址')
        }

        api.replaceSelection(buildMarkdownImage(file, imageUrl))
        message.destroy(uploadMessageKey)
        message.success('图片上传成功')
      } catch (error) {
        console.error('upload editor image error:', error)
        message.destroy(uploadMessageKey)
        message.error(getAxiosErrorMessage(error, '图片上传失败，请稍后重试'))
      }
    })()
  },
}

const EDITOR_COMMANDS = commands.getCommands().map((command) => (
  command.keyCommand === 'image' ? editorImageUploadCommand : command
))

const normalizeInviteUserIds = (userIds: string[], currentUserId?: string | number) => {
  const currentUserKey = currentUserId == null ? '' : String(currentUserId)
  return Array.from(new Set(userIds.map((userId) => userId.trim()).filter(Boolean)))
    .filter((userId) => userId !== currentUserKey)
}

const replaceUrlSearchParam = (searchParams: URLSearchParams, key: string, value: string) => {
  const nextParams = new URLSearchParams(searchParams)
  nextParams.set(key, value)
  return nextParams
}

const buildCollabTitle = (mode: 'article' | 'checkin' | 'question', title: string) => {
  const typeLabel = mode === 'article' ? '文章' : mode === 'checkin' ? '打卡' : '问答'
  return `${title.trim() || `未命名${typeLabel}`} 协作编辑`
}

type ReusablePostEditorProps = {
  mode: 'article' | 'checkin' | 'question'
  initialTitle: string
  initialMarkdown: string
  titlePlaceholder: string
  publishSuccessText: string
  updateSuccessText: string
  publishErrorText: string
  updateErrorText: string
  saveDraftText?: string
  updateDraftText?: string
}

export function ReusablePostEditor({
  mode,
  initialTitle,
  initialMarkdown,
  titlePlaceholder,
  publishSuccessText,
  updateSuccessText,
  publishErrorText,
  updateErrorText,
  saveDraftText = '保存草稿',
  updateDraftText = '更新草稿',
}: ReusablePostEditorProps) {
  const navigate = useNavigate()
  const { id } = useParams<{ id?: string }>()
  const [searchParams, setSearchParams] = useSearchParams()
  const currentUser = useAppSelector((state) => state.auth.userInfo)
  const editPostId = id?.trim()
  const isEditMode = Boolean(editPostId)
  const [title, setTitle] = useState(initialTitle)
  const [markdown, setMarkdown] = useState(initialMarkdown)
  const [categoryId, setCategoryId] = useState<string | number | undefined>()
  const [learnHours, setLearnHours] = useState<number | null>(1)
  const [tagNames, setTagNames] = useState<string[]>([])
  const [categories, setCategories] = useState<PostCategoryVO[]>([])
  const [tagOptions, setTagOptions] = useState<PostTagVO[]>([])
  const [metaError, setMetaError] = useState('')
  const [publishing, setPublishing] = useState(false)
  const [savingDraft, setSavingDraft] = useState(false)
  const [loading, setLoading] = useState(true)
  const [collabInviteUserIds, setCollabInviteUserIds] = useState<string[]>([])
  const [collabStarting, setCollabStarting] = useState(false)
  const [collabActive, setCollabActive] = useState(false)
  const [collabConnected, setCollabConnected] = useState(false)
  const [collabConversationId, setCollabConversationId] = useState<string | number>()
  const [collabDocumentId, setCollabDocumentId] = useState<string | number>()
  const [collabRevision, setCollabRevision] = useState(0)
  const [collabOnlineUsers, setCollabOnlineUsers] = useState<CollabOnlineUserVO[]>([])
  const [collabTypingUserIds, setCollabTypingUserIds] = useState<Array<string | number>>([])
  const collabClientIdRef = useRef(createCollabClientId())
  const collabWsRef = useRef<CollabEditWebSocket | null>(null)
  const collabActiveRef = useRef(false)
  const markdownRef = useRef(initialMarkdown)
  const collabRevisionRef = useRef(0)
  const pendingOperationsRef = useRef<CollabTextOperation[]>([])
  const sendingOperationRef = useRef(false)
  const applyingRemoteOperationRef = useRef(false)
  const typingClearTimerRef = useRef<number | null>(null)
  const collabConversationIdFromUrl = searchParams.get('collabConversationId')?.trim() || ''

  const isArticleMode = mode === 'article'
  const isCheckinMode = mode === 'checkin'
  const isQuestionMode = mode === 'question'
  const requiresCategory = isArticleMode || isQuestionMode
  const normalizedTitle = title.trim()
  const normalizedContent = markdown.trim()
  const hasLearnHours = typeof learnHours === 'number' && Number.isFinite(learnHours) && learnHours > 0
  const hasTags = tagNames.length > 0
  const canSubmit = Boolean(
    normalizedTitle
      && normalizedContent
      && (!requiresCategory || categoryId !== undefined)
      && (!isArticleMode || hasTags)
      && (!isCheckinMode || hasLearnHours),
  )
  const collabShareLink = useMemo(() => {
    if (!collabConversationId || typeof window === 'undefined') {
      return ''
    }

    const url = new URL(window.location.href)
    url.searchParams.set('collabConversationId', String(collabConversationId))
    return url.toString()
  }, [collabConversationId])

  useEffect(() => {
    collabActiveRef.current = collabActive
  }, [collabActive])

  useEffect(() => {
    markdownRef.current = markdown
  }, [markdown])

  useEffect(() => {
    collabRevisionRef.current = collabRevision
  }, [collabRevision])

  const clearTypingUsersLater = useCallback(() => {
    if (typingClearTimerRef.current != null) {
      window.clearTimeout(typingClearTimerRef.current)
    }

    typingClearTimerRef.current = window.setTimeout(() => {
      setCollabTypingUserIds([])
      typingClearTimerRef.current = null
    }, 1600)
  }, [])

  const sendNextCollabOperation = useCallback(() => {
    const ws = collabWsRef.current
    const operation = pendingOperationsRef.current[0]

    if (!collabActiveRef.current || !ws || !operation || sendingOperationRef.current) {
      return
    }

    const sent = ws.edit({
      baseRevision: collabRevisionRef.current,
      operation,
    })

    if (sent) {
      sendingOperationRef.current = true
    }
  }, [])

  const applyRemoteMarkdownOperation = useCallback((operation: CollabTextOperation | undefined, revision?: number) => {
    if (!operation) {
      return
    }

    const nextMarkdown = applyCollabOperation(markdownRef.current, operation)
    applyingRemoteOperationRef.current = true
    markdownRef.current = nextMarkdown
    setMarkdown(nextMarkdown)

    window.queueMicrotask(() => {
      applyingRemoteOperationRef.current = false
    })

    if (revision != null) {
      collabRevisionRef.current = revision
      setCollabRevision(revision)
    }
  }, [])

  const disconnectCollab = useCallback((showMessage = false) => {
    collabWsRef.current?.disconnect()
    collabWsRef.current = null
    collabActiveRef.current = false
    pendingOperationsRef.current = []
    sendingOperationRef.current = false
    setCollabActive(false)
    setCollabConnected(false)
    setCollabConversationId(undefined)
    setCollabDocumentId(undefined)
    setCollabOnlineUsers([])
    setCollabTypingUserIds([])

    if (typingClearTimerRef.current != null) {
      window.clearTimeout(typingClearTimerRef.current)
      typingClearTimerRef.current = null
    }

    if (showMessage) {
      message.info('已退出协同编辑')
    }
  }, [])

  const connectCollabRoom = useCallback((conversationId: string | number, documentId: string | number, revision: number, resetDocumentContent = false) => {
    const initialMarkdown = markdownRef.current
    collabWsRef.current?.disconnect()
    pendingOperationsRef.current = []
    sendingOperationRef.current = false
    collabRevisionRef.current = revision
    setCollabRevision(revision)
    setCollabConversationId(conversationId)
    setCollabDocumentId(documentId)
    setCollabActive(true)
    setCollabConnected(false)

    const ws = new CollabEditWebSocket(conversationId, documentId, collabClientIdRef.current)
    collabWsRef.current = ws

    ws.on<CollabJoinedPayload>('COLLAB_JOINED', (msg) => {
      const payload = msg.payload
      const joinedRevision = payload?.document?.revision ?? msg.revision ?? revision
      collabRevisionRef.current = joinedRevision
      setCollabRevision(joinedRevision)
      setCollabOnlineUsers(payload?.onlineUsers || [])
      setCollabConnected(true)

      if (typeof payload?.document?.content === 'string' && payload.document.content !== markdownRef.current) {
        if (resetDocumentContent) {
          markdownRef.current = initialMarkdown
          setMarkdown(initialMarkdown)
          pendingOperationsRef.current.push(...buildCollabTextOperations(payload.document.content, initialMarkdown))
          sendNextCollabOperation()
        } else {
          applyingRemoteOperationRef.current = true
          markdownRef.current = payload.document.content
          setMarkdown(payload.document.content)
          window.queueMicrotask(() => {
            applyingRemoteOperationRef.current = false
          })
        }
      }

      message.success('已加入协同编辑')
    })

    ws.on<CollabOnlineUserVO[]>('COLLAB_USER_JOINED', (msg) => {
      setCollabOnlineUsers(msg.payload || [])
      message.info('有协作者加入编辑')
    })

    ws.on<CollabOnlineUserVO[]>('COLLAB_USER_LEFT', (msg) => {
      setCollabOnlineUsers(msg.payload || [])
    })

    ws.on<CollabOperationAcceptedPayload>('COLLAB_OPERATION_ACCEPTED', (msg) => {
      const nextRevision = msg.payload?.revision ?? msg.revision
      if (nextRevision != null) {
        collabRevisionRef.current = nextRevision
        setCollabRevision(nextRevision)
      }

      pendingOperationsRef.current.shift()
      sendingOperationRef.current = false
      sendNextCollabOperation()
    })

    ws.on<CollabOperationBroadcastPayload>('COLLAB_OPERATION_BROADCAST', (msg) => {
      const nextRevision = msg.payload?.revision ?? msg.revision
      applyRemoteMarkdownOperation(msg.payload?.transformedOperation, nextRevision)
    })

    ws.on('COLLAB_TYPING_BROADCAST', (msg) => {
      if (msg.senderId != null && String(msg.senderId) !== String(currentUser?.userId)) {
        setCollabTypingUserIds((prev) => Array.from(new Set([...prev, msg.senderId as string | number])))
        clearTypingUsersLater()
      }
    })

    ws.on<string>('COLLAB_ERROR', (msg) => {
      const errorMessage = typeof msg.payload === 'string' ? msg.payload : '协同编辑同步失败'
      message.error(errorMessage)
      pendingOperationsRef.current = []
      sendingOperationRef.current = false
    })

    ws.connect()
  }, [applyRemoteMarkdownOperation, clearTypingUsersLater, currentUser?.userId, sendNextCollabOperation])

  const joinCollabByConversationId = useCallback(async (conversationId: string | number) => {
    try {
      setCollabStarting(true)
      const document = await getConversationCollabDocument(conversationId)
      connectCollabRoom(conversationId, document.id, document.revision || 0)
    } catch (error) {
      console.error('join collab room error:', error)
      message.error(getAxiosErrorMessage(error, '加入协同编辑失败，请确认你已在协作会话成员中'))
    } finally {
      setCollabStarting(false)
    }
  }, [connectCollabRoom])

  const handleStartCollab = useCallback(async () => {
    const inviteUserIds = normalizeInviteUserIds(collabInviteUserIds, currentUser?.userId)

    if (!inviteUserIds.length) {
      message.warning('请输入至少一个要邀请的用户 ID')
      return
    }

    try {
      setCollabStarting(true)
      const collabTitle = buildCollabTitle(mode, title)
      const isSingleInvite = inviteUserIds.length === 1
      const conversation = isSingleInvite
        ? await createSingleConversation(inviteUserIds[0])
        : await createGroupConversation({
            conversationName: `${collabTitle} ${new Date().toLocaleString('zh-CN')}`,
            memberIds: inviteUserIds,
          })

      if (conversation.id == null) {
        throw new Error('协作会话创建成功，但未返回会话 ID')
      }

      const document = await createOrGetCollabDocument(conversation.id, {
        title: collabTitle,
        content: markdownRef.current,
      })
      const nextParams = replaceUrlSearchParam(searchParams, 'collabConversationId', String(conversation.id))
      setSearchParams(nextParams, { replace: true })
      connectCollabRoom(conversation.id, document.id, document.revision || 0, isSingleInvite)

      const shareUrl = new URL(window.location.href)
      shareUrl.searchParams.set('collabConversationId', String(conversation.id))
      await sendTextMessage(conversation.id, buildCollabInviteMessage({
        title: collabTitle,
        url: shareUrl.toString(),
        conversationId: conversation.id,
        documentId: document.id,
      }))
      message.success('协同编辑已开启，邀请消息已发送')
    } catch (error) {
      console.error('start collab edit error:', error)
      message.error(getAxiosErrorMessage(error, '协同编辑开启失败，请稍后重试'))
    } finally {
      setCollabStarting(false)
    }
  }, [collabInviteUserIds, connectCollabRoom, currentUser?.userId, mode, searchParams, setSearchParams, title])

  const handleCopyCollabLink = useCallback(async () => {
    if (!collabShareLink) {
      return
    }

    try {
      await navigator.clipboard.writeText(collabShareLink)
      message.success('协作链接已复制')
    } catch {
      message.info(collabShareLink)
    }
  }, [collabShareLink])

  const handleMarkdownChange = useCallback((value?: string) => {
    const nextMarkdown = value ?? ''
    const previousMarkdown = markdownRef.current
    markdownRef.current = nextMarkdown
    setMarkdown(nextMarkdown)

    if (!collabActiveRef.current || applyingRemoteOperationRef.current) {
      return
    }

    const operations = buildCollabTextOperations(previousMarkdown, nextMarkdown)
    if (!operations.length) {
      return
    }

    pendingOperationsRef.current.push(...operations)
    sendNextCollabOperation()

    const cursor = nextMarkdown.length
    collabWsRef.current?.typing({ cursor })
    collabWsRef.current?.cursor({ cursor, selectionStart: cursor, selectionEnd: cursor })
  }, [sendNextCollabOperation])

  useEffect(() => {
    if (loading || !collabConversationIdFromUrl || collabActiveRef.current) {
      return
    }

    void joinCollabByConversationId(collabConversationIdFromUrl)
  }, [collabConversationIdFromUrl, joinCollabByConversationId, loading])

  useEffect(() => () => {
    disconnectCollab()
  }, [disconnectCollab])

  useEffect(() => {
    let cancelled = false

    const fetchBaseOptions = async () => {
      if (!isArticleMode && !isQuestionMode) {
        return
      }

      try {
        setMetaError('')

        if (isArticleMode) {
          const [categoryData, tagData] = await Promise.all([listCategories(), listTags()])

          if (!cancelled) {
            setCategories(categoryData || [])
            setTagOptions(tagData || [])
          }

          return
        }

        const categoryData = await listQaCategories()

        if (!cancelled) {
          setCategories((categoryData || []).map((item) => ({
            id: item.id,
            categoryName: item.categoryName,
          })))
          setTagOptions([])
        }
      } catch (error) {
        console.error('load post meta options error:', error)

        if (!cancelled) {
          setCategories([])
          setTagOptions([])
          setMetaError(isQuestionMode ? '问题分类加载失败，请确认接口可用且当前登录状态有效。' : '文章分类和标签加载失败，请确认接口可用且当前登录状态有效。')
        }
      }
    }

    const fetchPost = async () => {
      if (!isEditMode || !editPostId) {
        setLoading(false)
        return
      }

      try {
        if (isCheckinMode) {
          const check = await getCheckRecordDetail(editPostId)

          if (!cancelled) {
            setTitle(check.title || initialTitle)
            setMarkdown(check.content || initialMarkdown)
            setLearnHours(check.learnHours ?? 1)
          }

          return
        }

        if (isQuestionMode) {
          const question = await getQuestionDetail(editPostId)

          if (!cancelled) {
            setTitle(question.title || initialTitle)
            setMarkdown(question.content || initialMarkdown)
            setCategoryId(question.categoryId)
          }

          return
        }

        const post = await getPostDetail(editPostId)

        if (!cancelled) {
          setTitle(post.title || initialTitle)
          setMarkdown(post.content || initialMarkdown)

          if (isArticleMode) {
            setCategoryId(post.categoryId)
            setTagNames(post.tags || [])
          }
        }
      } catch (error) {
        console.error('load editor post error:', error)

        if (!cancelled) {
          message.error(getAxiosErrorMessage(error, '内容加载失败，请稍后重试'))
          navigate('/home')
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    void fetchBaseOptions()
    void fetchPost()

    if (!isEditMode) {
      setLoading(false)
    }

    return () => {
      cancelled = true
    }
  }, [editPostId, initialMarkdown, initialTitle, isArticleMode, isCheckinMode, isEditMode, isQuestionMode, navigate])

  const submitPost = async (status: number) => {
    const normalizedTitle = title.trim()
    const normalizedContent = markdown.trim()

    if (!normalizedTitle) {
      message.warning('请输入标题')
      return
    }

    if (!normalizedContent) {
      message.warning('请输入内容')
      return
    }

    if ((isArticleMode || isQuestionMode) && categoryId === undefined) {
      message.warning(isQuestionMode ? '请选择问题分类' : '请选择文章分类')
      return
    }

    if (isArticleMode && !hasTags) {
      message.warning('请选择至少一个文章标签')
      return
    }

    if (isCheckinMode && !hasLearnHours) {
      message.warning('请输入学习时长')
      return
    }

    const setLoadingState = status === PUBLISHED_STATUS ? setPublishing : setSavingDraft

    try {
      setLoadingState(true)

      if (isCheckinMode) {
        const payload = {
          title: normalizedTitle,
          summary: buildSummary(normalizedContent),
          content: normalizedContent,
          learnHours: learnHours ?? 0,
          status: status === PUBLISHED_STATUS ? CHECK_PUBLISHED_STATUS : CHECK_DRAFT_STATUS,
          checkDate: new Date().toISOString().slice(0, 10),
        }

        const check = isEditMode && editPostId
          ? await updateCheckRecord(editPostId, payload)
          : await createCheckRecord(payload)

        message.success(status === PUBLISHED_STATUS ? (isEditMode ? updateSuccessText : publishSuccessText) : (isEditMode ? updateDraftText : saveDraftText))
        navigate(check.id ? `/check-in/${check.id}` : '/check-in')
        return
      }

      if (isQuestionMode) {
        const payload = {
          title: normalizedTitle,
          content: normalizedContent,
          categoryId,
          bountyPoints: 0,
        }

        const question = isEditMode && editPostId
          ? await updateQuestion(editPostId, payload)
          : await createQuestion(payload)

        message.success(status === PUBLISHED_STATUS ? (isEditMode ? updateSuccessText : publishSuccessText) : (isEditMode ? updateDraftText : saveDraftText))
        navigate(question.id ? '/qa' : '/qa')
        return
      }

      const coverImageUrl = extractFirstImageUrl(normalizedContent)
      const payload = {
        title: normalizedTitle,
        contentType: MARKDOWN_CONTENT_TYPE,
        content: normalizedContent,
        status,
        ...(isArticleMode
          ? {
              summary: buildSummary(normalizedContent),
              categoryId,
              tagNames,
              ...(coverImageUrl
                ? { images: [{ imageUrl: coverImageUrl, imageType: '封面', sortOrder: 0 }] }
                : {}),
            }
          : {}),
      }

      const post = isEditMode && editPostId
        ? await updatePost(editPostId, {
            ...payload,
            changeDesc: status === PUBLISHED_STATUS
              ? '更新文章'
              : '更新草稿',
          })
        : await createPost(payload)

      message.success(status === PUBLISHED_STATUS ? (isEditMode ? updateSuccessText : publishSuccessText) : (isEditMode ? updateDraftText : saveDraftText))

      if (post.id) {
        navigate(`/article/${post.id}`)
        return
      }

      navigate('/home')
    } catch (error) {
      console.error('submit post error:', error)
      message.error(getAxiosErrorMessage(error, status === PUBLISHED_STATUS ? (isEditMode ? updateErrorText : publishErrorText) : '草稿保存失败，请稍后重试'))
    } finally {
      setLoadingState(false)
    }
  }

  if (loading) {
    return (
      <Card className="content-card" variant="borderless">
        <Skeleton active paragraph={{ rows: 12 }} />
      </Card>
    )
  }

  return (
    <div className="article-editor-page" data-color-mode="light">
      <div className="article-editor-page__inner">
        <div className="article-editor-topbar">
          <Space wrap>
            <Button icon={<ArrowLeftOutlined />} onClick={() => navigate(-1)}>
              返回
            </Button>
          </Space>
          <Space wrap>
            <Button icon={<SaveOutlined />} loading={savingDraft} disabled={publishing || !canSubmit} onClick={() => void submitPost(DRAFT_STATUS)}>
              {isEditMode ? updateDraftText : saveDraftText}
            </Button>
            <Button type="primary" icon={<SendOutlined />} loading={publishing} disabled={savingDraft || !canSubmit} onClick={() => void submitPost(PUBLISHED_STATUS)}>
              {isEditMode
                ? isCheckinMode
                  ? '更新打卡'
                  : mode === 'question'
                    ? '更新问题'
                    : '更新文章'
                : isCheckinMode
                  ? '发布打卡'
                  : mode === 'question'
                    ? '发布提问'
                    : '发布文章'}
            </Button>
          </Space>
        </div>

        <div className="article-editor-titlebar">
          <Input value={title} onChange={(event) => setTitle(event.target.value)} placeholder={`* ${titlePlaceholder}`} className="article-editor-title-input" size="large" variant="borderless" />
        </div>

        {isCheckinMode ? (
          <Card className="content-card article-editor-meta-card" variant="borderless">
            <Row gutter={[16, 16]}>
              <Col xs={24} md={8}>
                <div className="article-editor-field-label"><span className="article-editor-field-label__required">*</span>学习时长</div>
                <InputNumber
                  value={learnHours}
                  onChange={(value) => setLearnHours(value)}
                  placeholder="请输入学习时长"
                  min={0}
                  max={24}
                  step={0.5}
                  addonAfter="h"
                  className="article-editor-learn-hours"
                />
              </Col>
            </Row>
          </Card>
        ) : null}

        {(isArticleMode || isQuestionMode) ? (
          <Card className="content-card article-editor-meta-card" variant="borderless">
            <Row gutter={[16, 16]}>
              {metaError ? (
                <Col span={24}>
                  <Alert type="warning" showIcon message={metaError} />
                </Col>
              ) : null}
              <Col xs={24} md={12} lg={8}>
                <div className="article-editor-field-label"><span className="article-editor-field-label__required">*</span>{isQuestionMode ? '问题分类' : '文章分类'}</div>
                <Select
                  value={categoryId}
                  onChange={(value) => setCategoryId(value)}
                  placeholder={isQuestionMode ? '请选择问题分类' : '请选择文章分类'}
                  className="article-editor-select"
                  options={(categories || []).map((item) => ({
                    label: item.categoryName || '未命名分类',
                    value: item.id,
                  })).filter((item) => item.value !== undefined)}
                />
              </Col>
              {isArticleMode ? (
                <Col xs={24} md={12} lg={16}>
                  <div className="article-editor-field-label"><span className="article-editor-field-label__required">*</span>文章标签</div>
                  <Select
                    mode="tags"
                    value={tagNames}
                    onChange={(values) => setTagNames(values.slice(0, 8))}
                    placeholder="输入或选择标签，最多 8 个"
                    className="article-editor-select"
                    options={(tagOptions || []).map((item) => ({
                      label: item.tagName || '未命名标签',
                      value: item.tagName || '',
                    })).filter((item) => item.value)}
                  />
                </Col>
              ) : null}
              {isArticleMode && tagNames.length ? (
                <Col span={24}>
                  <div className="article-editor-tag-preview">
                    {tagNames.map((tag) => (
                      <Tag key={tag} color="blue">
                        {tag}
                      </Tag>
                    ))}
                  </div>
                </Col>
              ) : null}
            </Row>
          </Card>
        ) : null}

        <Card className="content-card article-editor-collab-card" variant="borderless">
          <div className="article-editor-collab-card__header">
            <div>
              <div className="article-editor-collab-card__title"><TeamOutlined /> 协同编辑</div>
              <div className="article-editor-collab-card__desc">邀请一个或多个用户共同编辑正文 Markdown，标题、分类和标签仍由当前发布页维护。</div>
            </div>
            <Tag color={collabConnected ? 'green' : collabActive ? 'gold' : 'default'}>
              {collabConnected ? '已连接' : collabActive ? '连接中' : '未开启'}
            </Tag>
          </div>
          <Row gutter={[12, 12]} align="middle">
            <Col xs={24} lg={12}>
              <Select
                mode="tags"
                value={collabInviteUserIds}
                onChange={setCollabInviteUserIds}
                placeholder="输入用户 ID 后回车，支持邀请 1 个或多个用户"
                tokenSeparators={[',', '，', ' ']}
                className="article-editor-collab-card__invite"
                open={false}
              />
            </Col>
            <Col xs={24} lg={12}>
              <Space wrap>
                <Button type="primary" icon={<UserAddOutlined />} loading={collabStarting} onClick={() => void handleStartCollab()}>
                  {collabActive ? '重新邀请' : '开启协同'}
                </Button>
                <Button icon={<LinkOutlined />} disabled={!collabShareLink} onClick={() => void handleCopyCollabLink()}>
                  复制协作链接
                </Button>
                <Button icon={<DisconnectOutlined />} disabled={!collabActive} onClick={() => disconnectCollab(true)}>
                  退出协同
                </Button>
              </Space>
            </Col>
          </Row>
          {collabActive ? (
            <div className="article-editor-collab-card__status">
              <Space wrap size={[8, 8]}>
                {collabConversationId != null ? <Tag>会话 {String(collabConversationId)}</Tag> : null}
                {collabDocumentId != null ? <Tag>文档 {String(collabDocumentId)}</Tag> : null}
                <Tag>版本 {collabRevision}</Tag>
                <Tag color="blue">在线 {collabOnlineUsers.length}</Tag>
                {collabTypingUserIds.length ? <Tag color="purple">用户 {collabTypingUserIds.map(String).join('、')} 正在编辑</Tag> : null}
              </Space>
            </div>
          ) : null}
        </Card>

        <Row gutter={[20, 20]} className="article-editor-grid">
          <Col xs={24} className="article-editor-grid__col">
            <Card className="content-card article-editor-panel article-editor-panel--form" variant="borderless">
              <div className="article-editor-markdown article-editor-markdown--full">
                <MDEditor
                  value={markdown}
                  onChange={handleMarkdownChange}
                  preview="live"
                  height={640}
                  commands={EDITOR_COMMANDS}
                  previewOptions={{ components: markdownImageComponents }}
                />
              </div>
            </Card>
          </Col>
        </Row>
      </div>
    </div>
  )
}
