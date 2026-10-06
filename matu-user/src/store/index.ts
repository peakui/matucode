import { configureStore } from '@reduxjs/toolkit'
import authReducer from './modules/authSlice'
import checkListReducer from './modules/checkListSlice'
import feedReducer from './modules/feedSlice'
import qaListReducer from './modules/qaListSlice'
import themeReducer from './modules/themeSlice'

export const store = configureStore({
  reducer: {
    auth: authReducer,
    checkList: checkListReducer,
    feed: feedReducer,
    qaList: qaListReducer,
    theme: themeReducer,
  },
})

export type RootState = ReturnType<typeof store.getState>
export type AppDispatch = typeof store.dispatch
