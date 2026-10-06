import { createSlice, type PayloadAction } from '@reduxjs/toolkit'

type ThemeMode = 'light' | 'dark'

const getInitialTheme = (): ThemeMode => {
  if (typeof window === 'undefined') {
    return 'light'
  }

  return localStorage.getItem('codehub-theme') === 'dark' ? 'dark' : 'light'
}

type ThemeState = {
  mode: ThemeMode
}

const initialState: ThemeState = {
  mode: getInitialTheme(),
}

const themeSlice = createSlice({
  name: 'theme',
  initialState,
  reducers: {
    setTheme(state, action: PayloadAction<ThemeMode>) {
      state.mode = action.payload

      if (typeof window !== 'undefined') {
        localStorage.setItem('codehub-theme', action.payload)
      }
    },
  },
})

export const { setTheme } = themeSlice.actions
export type { ThemeMode }
export default themeSlice.reducer
