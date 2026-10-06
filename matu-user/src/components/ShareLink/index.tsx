import { CopyOutlined, LinkOutlined, ShareAltOutlined } from '@ant-design/icons'
import { Button, Divider, Input, Modal, QRCode, Space, message } from 'antd'
import { useEffect, useRef } from 'react'
import './ShareLink.scss'

interface ShareLinkProps {
  open: boolean
  url: string
  title?: string
  count?: number
  onClose: () => void
  onShared: () => void
}

export function ShareLink({ open, url, title, count, onClose, onShared }: ShareLinkProps) {
  const countedRef = useRef(false)
  const canNativeShare = typeof navigator !== 'undefined' && typeof navigator.share === 'function'

  useEffect(() => {
    if (!open) {
      countedRef.current = false
      return
    }
    if (countedRef.current) {
      return
    }
    countedRef.current = true
    onShared()
  }, [open, onShared])

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(url)
      message.success('链接已复制，快去分享给好友吧')
    } catch {
      message.info(url)
    }
  }

  const handleNativeShare = async () => {
    if (!canNativeShare) {
      return
    }
    try {
      await navigator.share({ title: title || document.title, url })
    } catch {
      // 用户取消了系统分享面板，不额外处理
    }
  }

  return (
    <Modal open={open} onCancel={onClose} footer={null} title="分享" centered width={420}>
      <div className="share-link">
        <div className="share-link__qr">
          <QRCode value={url} size={196} />
          <div className="share-link__qr-tip">扫码即可打开</div>
        </div>
        <Divider plain>或复制下方链接</Divider>
        <Input
          readOnly
          value={url}
          prefix={<LinkOutlined />}
          suffix={
            <Button type="text" size="small" icon={<CopyOutlined />} onClick={() => void handleCopy()}>
              复制
            </Button>
          }
        />
        <Space className="share-link__actions" size={12} style={{ width: '100%' }}>
          <Button type="primary" icon={<CopyOutlined />} onClick={() => void handleCopy()}>
            复制链接
          </Button>
          {canNativeShare ? (
            <Button icon={<ShareAltOutlined />} onClick={() => void handleNativeShare()}>
              系统分享
            </Button>
          ) : null}
        </Space>
        {count != null ? <div className="share-link__count">已被分享 {count} 次</div> : null}
      </div>
    </Modal>
  )
}

export default ShareLink
