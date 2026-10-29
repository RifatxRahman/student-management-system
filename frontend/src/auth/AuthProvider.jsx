import { useCallback, useEffect, useMemo, useState } from 'react'
import { api, refreshSession, setAccessToken, setUnauthorizedHandler } from '../api/client.js'
import { AuthContext } from './AuthContext.js'

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)
  const [notice, setNotice] = useState('')

  // অ্যাপ খোলার সময়: কুকি দিয়ে আগের লগইন ফিরিয়ে আনার চেষ্টা
  useEffect(() => {
    setUnauthorizedHandler(() => setUser(null))
    refreshSession()
      .then((data) => setUser(data.user))
      .catch(() => setUser(null))
      .finally(() => setLoading(false))
  }, [])

  const login = useCallback(async (username, password) => {
    const data = await api('/api/auth/login', {
      method: 'POST',
      body: { username, password },
      auth: false,
    })
    setAccessToken(data.accessToken)
    setUser(data.user)
    return data.user
  }, [])

  const logout = useCallback(async () => {
    try {
      await api('/api/auth/logout', { method: 'POST', auth: false })
    } finally {
      setAccessToken(null)
      setUser(null)
    }
  }, [])

  const changePassword = useCallback(async (currentPassword, newPassword) => {
    await api('/api/auth/change-password', {
      method: 'POST',
      body: { currentPassword, newPassword },
    })
    // পাসওয়ার্ড বদলালে Backend সব পুরনো লগইন বাতিল করে, তাই নতুন করে ঢুকতে হবে
    setAccessToken(null)
    setNotice('পাসওয়ার্ড বদলেছে। নতুন পাসওয়ার্ড দিয়ে আবার লগইন করুন।')
    setUser(null)
  }, [])

  const clearNotice = useCallback(() => setNotice(''), [])

  const value = useMemo(
    () => ({ user, loading, notice, login, logout, changePassword, clearNotice }),
    [user, loading, notice, login, logout, changePassword, clearNotice],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}