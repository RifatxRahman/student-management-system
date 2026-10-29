import { Link } from 'react-router'
import { useAuth } from '../auth/useAuth.js'
import { Card, PageHeader } from '../components/ui.jsx'
import { useFetch } from '../hooks/useFetch.js'
import { formatDate } from '../utils/format.js'

function Stat({ label, value, hint, to }) {
  const content = (
    <Card className="p-5 transition hover:shadow-md">
      <p className="text-sm text-gray-500">{label}</p>
      <p className="mt-2 text-2xl font-semibold text-gray-900">{value}</p>
      {hint && <p className="mt-1 text-xs text-gray-500">{hint}</p>}
    </Card>
  )
  return to ? <Link to={to}>{content}</Link> : content
}

export default function DashboardPage() {
  const { user } = useAuth()
  const session = useFetch('/api/sessions/active')
  const classes = useFetch('/api/classes')
  const subjects = useFetch('/api/subjects')

  const noActiveSession = session.error?.status === 404

  let sessionValue = '—'
  if (session.loading) sessionValue = '...'
  else if (noActiveSession) sessionValue = 'নেই'
  else if (session.data) sessionValue = session.data.name

  const sessionHint = session.data
    ? `${formatDate(session.data.startDate)} থেকে ${formatDate(session.data.endDate)}`
    : noActiveSession && user.role === 'ADMIN'
      ? 'সেশন পাতা থেকে একটা সেশন সক্রিয় করুন'
      : undefined

  const activeClasses = classes.data ? classes.data.filter((c) => c.active).length : '...'
  const activeSubjects = subjects.data ? subjects.data.filter((s) => s.active).length : '...'

  return (
    <>
      <PageHeader title={`স্বাগতম, ${user.fullName}`} subtitle="এক নজরে আপনার প্রতিষ্ঠান" />

      <div className="grid gap-4 sm:grid-cols-3">
        <Stat label="সক্রিয় সেশন" value={sessionValue} hint={sessionHint} to="/sessions" />
        <Stat label="সক্রিয় ক্লাস" value={activeClasses} to="/classes" />
        <Stat label="সক্রিয় বিষয়" value={activeSubjects} to="/subjects" />
      </div>
    </>
  )
}