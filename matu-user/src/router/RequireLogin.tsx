import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAppSelector } from '../store/hooks'

export function RequireLogin({ children }: { children: ReactNode }) {
  const isLoggedIn = useAppSelector((state) => state.auth.isLoggedIn)
  const location = useLocation()

  if (!isLoggedIn) {
    return <Navigate replace to="/auth" state={{ from: location.pathname }} />
  }

  return <>{children}</>
}
