export const clearAdminAuth = () => {
  for (const key of ['admin_token', 'admin_authorization', 'admin_user_info', 'admin_logged_in']) {
    localStorage.removeItem(key)
  }
}

export const expireAdminSession = () => {
  clearAdminAuth()
  if (window.location.pathname !== '/login') window.location.replace('/login')
}
