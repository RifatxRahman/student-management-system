import { useState } from 'react'
import { Navigate, useLocation } from 'react-router'
import { useAuth } from '../auth/useAuth.js'
import { Alert, Button, Card, TextField } from '../components/ui.jsx'

export default function LoginPage() {
  const { user, login, notice, clearNotice } = useAuth()
  const location = useLocation()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  // লগইন হয়ে গেলে নিজে থেকে ঠিক জায়গায় পাঠিয়ে দেয়
  if (user) {
    const fromPath = location.state?.from?.pathname
    const target = fromPath && fromPath !== '/change-password' ? fromPath : '/'
    return <Navigate to={user.mustChangePassword ? '/change-password' : target} replace />
  }

  async function handleSubmit(event) {
    event.preventDefault()
    clearNotice()
    setError('')
    setSubmitting(true)
    try {
      await login(username.trim(), password)
    } catch (err) {
      setError(err.message)
      setSubmitting(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-50 px-4">
      <div className="w-full max-w-sm">
        <div className="mb-6 text-center">
          <h1 className="text-2xl font-semibold text-gray-900">Student Management</h1>
          <p className="mt-1 text-sm text-gray-500">আপনার অ্যাকাউন্টে লগইন করুন</p>
        </div>

        <Card className="p-6">
          <form onSubmit={handleSubmit} className="space-y-4">
            {notice && <Alert type="success">{notice}</Alert>}
            {error && <Alert>{error}</Alert>}

            <TextField
              label="ইউজারনেম"
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              autoComplete="username"
              autoFocus
              required
            />
            <TextField
              label="পাসওয়ার্ড"
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              autoComplete="current-password"
              required
            />

            <Button type="submit" className="w-full" disabled={submitting}>
              {submitting ? 'অপেক্ষা করুন...' : 'লগইন'}
            </Button>
          </form>
        </Card>
      </div>
    </div>
  )
}