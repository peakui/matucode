import './StandaloneLayout.scss'

import { Button } from 'antd'
import { BulbOutlined, MoonOutlined } from '@ant-design/icons'
import { Outlet, useLocation } from 'react-router-dom'
import { useEffect, useState } from 'react'

export type ThemeMode = 'light' | 'dark'

export function StandaloneLayout() {
  const location = useLocation()
  const [theme, setTheme] = useState<ThemeMode>(() => {
    const savedTheme = localStorage.getItem('codehub-theme')
    return savedTheme === 'dark' ? 'dark' : 'light'
  })

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme)
    document.body.setAttribute('data-theme', theme)
    localStorage.setItem('codehub-theme', theme)
  }, [theme])

  return (
    <div className="standalone-shell">
      <div className="standalone-theme-switch">
        <Button
          type="default"
          icon={theme === 'light' ? <MoonOutlined /> : <BulbOutlined />}
          onClick={() => setTheme(theme === 'light' ? 'dark' : 'light')}
        >
          {theme === 'light' ? '切换暗色' : '切换亮色'}
        </Button>
      </div>
      <div key={location.pathname}>
        <Outlet />
      </div>
    </div>
  )
}
