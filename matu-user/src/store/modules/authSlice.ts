import { createSlice, type PayloadAction } from '@reduxjs/toolkit'

type StoredUser = {
  userId?: number
  username?: string
  nickname?: string
  nikename?: string
  email?: string
  roles?: string[]
  avatarUrl?: string
  title?: string
  titleVerified?: number
  schoolVerified?: number
  companyVerified?: number
  isVip?: number
  vipLevel?: number
  vipExpiredAt?: string | null
  vipDaysRemaining?: number
}

type AuthState = {
  isLoggedIn: boolean
  userInfo: StoredUser | null
}

const getStoredUser = (): StoredUser | null => {
  if (typeof window === 'undefined') {
    return null
  }

  const rawUser = localStorage.getItem('codehub-user')

  if (!rawUser) {
    return null
  }

  try {
    return JSON.parse(rawUser) as StoredUser
  } catch {
    return null
  }
}

const initialState: AuthState = {
  isLoggedIn: typeof window !== 'undefined' ? Boolean(localStorage.getItem('codehub-token')) : false,
  userInfo: getStoredUser(),
}

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    syncAuthState(state) {
      if (typeof window === 'undefined') {
        state.isLoggedIn = false
        state.userInfo = null
        return
      }

      state.isLoggedIn = Boolean(localStorage.getItem('codehub-token'))
      state.userInfo = getStoredUser()
    },
    setAuthState(state, action: PayloadAction<AuthState>) {
      state.isLoggedIn = action.payload.isLoggedIn
      state.userInfo = action.payload.userInfo
    },
    logout(state) {
      if (typeof window !== 'undefined') {
        localStorage.removeItem('codehub-token')
        localStorage.removeItem('token')
        localStorage.removeItem('codehub-token-name')
        localStorage.removeItem('codehub-authorization')
        localStorage.removeItem('codehub-user')
      }

      state.isLoggedIn = false
      state.userInfo = null
    },
  },
})

export const { logout, setAuthState, syncAuthState } = authSlice.actions
export type { StoredUser }
export default authSlice.reducer
