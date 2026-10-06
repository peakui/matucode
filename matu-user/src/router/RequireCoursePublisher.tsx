import type { ReactNode } from 'react'
import { Button, Result } from 'antd'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useAppSelector } from '../store/hooks'
import { canPublishCourse } from '../utils/permissions'

// Guards the course/class authoring routes so a logged-in user without publishing
// rights never reaches the editors. The backend still enforces this server-side;
// the guard only keeps the UI honest.
export function RequireCoursePublisher({ children }: { children: ReactNode }) {
  const isLoggedIn = useAppSelector((state) => state.auth.isLoggedIn)
  const userInfo = useAppSelector((state) => state.auth.userInfo)
  const location = useLocation()
  const navigate = useNavigate()

  if (!isLoggedIn) {
    return <Navigate replace to="/auth" state={{ from: location.pathname }} />
  }

  if (!canPublishCourse(userInfo)) {
    return (
      <Result
        status="403"
        title="无权限访问"
        subTitle="当前账号没有教程/班级的编辑与管理权限。"
        extra={<Button type="primary" onClick={() => navigate('/tutorials')}>返回教程</Button>}
      />
    )
  }

  return <>{children}</>
}
