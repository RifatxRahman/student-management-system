import { Link } from 'react-router'
import { useAuth } from '../auth/useAuth.js'
import { Card, PageHeader } from '../components/ui.jsx'
import { useFetch } from '../hooks/useFetch.js'
import { useQuery } from '../hooks/useQuery.js'
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
  const isAdmin = user.role === 'ADMIN'
  const session = useFetch('/api/sessions/active')
  const classes = useFetch('/api/classes')
  const subjects = useFetch('/api/subjects')
  // শিক্ষার্থী ও শিক্ষকের সংখ্যা শুধু Admin দেখেন (ব্যক্তিগত তথ্যের এলাকা)
  const students = useQuery(isAdmin ? '/api/students?status=ACTIVE&size=1' : null)
  const teachers = useQuery(isAdmin ? '/api/teachers' : null)

  const noActiveSession = session.error?.status === 404

  let sessionValue = '—'
  if (session.loading) sessionValue = '...'
  else if (noActiveSession) sessionValue = 'নেই'
  else if (session.data) sessionValue = session.data.name

  const sessionHint = session.data
    ? `${formatDate(session.data.startDate)} থেকে ${formatDate(session.data.endDate)}`
    : noActiveSession && isAdmin
      ? 'সেশন পাতা থেকে একটা সেশন সক্রিয় করুন'
      : undefined

  const activeClasses = classes.data ? classes.data.filter((c) => c.active).length : '...'
  const activeSubjects = subjects.data ? subjects.data.filter((s) => s.active).length : '...'

  let studentCount = '...'
  if (students.error) studentCount = '—'
  else if (students.data) studentCount = students.data.totalElements

  let teacherCount = '...'
  if (teachers.error) teacherCount = '—'
  else if (teachers.data) teacherCount = teachers.data.filter((t) => t.active).length

  return (
    <>
      <PageHeader title={`স্বাগতম, ${user.fullName}`} subtitle="এক নজরে আপনার প্রতিষ্ঠান" />

      <div className="grid gap-4 sm:grid-cols-3">
        <Stat label="সক্রিয় সেশন" value={sessionValue} hint={sessionHint} to="/sessions" />
        <Stat label="সক্রিয় ক্লাস" value={activeClasses} to="/classes" />
        <Stat label="সক্রিয় বিষয়" value={activeSubjects} to="/subjects" />
      </div>

      <div className="mt-4 grid gap-4 sm:grid-cols-3">
        {user.role !== 'ADMIN' && user.role !== 'TEACHER' ? null : (
          <>
            <Stat label="হাজিরা" value="নিন / দেখুন" to="/attendance" />
            <Stat label="মাসিক রিপোর্ট" value="দেখুন" to="/attendance/report" />
          </>
        )}
        <Stat label="ছুটির দিন" value="দেখুন" to="/holidays" />
      </div>

      {isAdmin && (
        <div className="mt-4 grid gap-4 sm:grid-cols-3">
          <Stat label="সক্রিয় শিক্ষার্থী" value={studentCount} to="/students" />
          <Stat label="সক্রিয় শিক্ষক" value={teacherCount} to="/teachers" />
        </div>
      )}
    </>
  )
}