import { useState } from 'react'
import { Link } from 'react-router'
import { useAuth } from '../auth/useAuth.js'
import { Alert, Button, Card, TextField } from '../components/ui.jsx'

export default function ChangePasswordPage() {
  const { user, changePassword, logout } = useAuth()
  const [form, setForm] = useState({ current: '', next: '', confirm: '' })
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  function handleChange(field) {
    return (event) => setForm((current) => ({ ...current, [field]: event.target.value }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')

    if (form.next !== form.confirm) {
      setError('নতুন পাসওয়ার্ড দুই ঘরে মেলেনি')
      return
    }
    if (form.next.length < 8 || !/\p{L}/u.test(form.next) || !/\p{Nd}/u.test(form.next)) {
      setError('পাসওয়ার্ড কমপক্ষে ৮ অক্ষরের হতে হবে, আর অক্ষর ও সংখ্যা দুটোই থাকতে হবে')
      return
    }

    setSubmitting(true)
    try {
      await changePassword(form.current, form.next)
    } catch (err) {
      setError(err.message)
      setSubmitting(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-[var(--canvas)] px-4">
      <div className="w-full max-w-sm">
        <div className="mb-6 text-center">
          <h1 className="text-2xl font-bold text-[var(--ink-primary)] font-['Anek_Bangla',sans-serif]">পাসওয়ার্ড বদলান</h1>
          <p className="mt-1 text-sm text-[var(--ink-secondary)]">{user.fullName}</p>
        </div>

        <Card className="p-6">
          <form onSubmit={handleSubmit} className="space-y-4">
            {user.mustChangePassword && (
              <Alert type="info">
                আপনার অ্যাকাউন্টে অস্থায়ী পাসওয়ার্ড আছে। চালিয়ে যেতে নিজের একটা নতুন পাসওয়ার্ড দিন।
              </Alert>
            )}
            {error && <Alert>{error}</Alert>}

            <TextField
              label="বর্তমান পাসওয়ার্ড"
              type="password"
              value={form.current}
              onChange={handleChange('current')}
              autoComplete="current-password"
              required
            />
            <TextField
              label="নতুন পাসওয়ার্ড"
              type="password"
              hint="কমপক্ষে ৮ অক্ষর, অক্ষর ও সংখ্যা দুটোই থাকতে হবে"
              value={form.next}
              onChange={handleChange('next')}
              autoComplete="new-password"
              required
            />
            <TextField
              label="নতুন পাসওয়ার্ড আবার"
              type="password"
              value={form.confirm}
              onChange={handleChange('confirm')}
              autoComplete="new-password"
              required
            />

            <Button type="submit" className="w-full" disabled={submitting}>
              {submitting ? 'অপেক্ষা করুন...' : 'পাসওয়ার্ড বদলান'}
            </Button>
          </form>
        </Card>

        <div className="mt-4 text-center text-sm">
          {user.mustChangePassword ? (
            <button type="button" className="text-[var(--ink-secondary)] hover:text-[var(--ink-primary)]" onClick={logout}>
              লগআউট
            </button>
          ) : (
            <Link to="/" className="text-[var(--ink-secondary)] hover:text-[var(--ink-primary)]">
              ← ফিরে যান
            </Link>
          )}
        </div>
      </div>
    </div>
  )
}