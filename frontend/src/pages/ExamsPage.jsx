import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { useAuth } from '../auth/useAuth.js'
import { ExamCreateModal } from '../components/ExamModals.jsx'
import { Badge, Button, Card, EmptyState, LoadState, PageHeader, SelectField, Table, Td } from '../components/ui.jsx'
import { useQuery } from '../hooks/useQuery.js'
import { examStatusInfo, examTypeLabel } from '../utils/format.js'

export default function ExamsPage() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const isAdmin = user.role === 'ADMIN'
  const sessions = useQuery('/api/sessions')
  const classes = useQuery('/api/classes')
  const [sessionFilter, setSessionFilter] = useState('') // '' = সক্রিয় সেশন, 'all' = সব সেশন
  const [classFilter, setClassFilter] = useState('')
  const [creating, setCreating] = useState(false)

  const sessionList = sessions.data ?? []
  const activeSession = sessionList.find((s) => s.active)
  let sessionId = ''
  if (sessionFilter && sessionFilter !== 'all') {
    sessionId = sessionFilter
  } else if (!sessionFilter && activeSession) {
    sessionId = String(activeSession.id)
  }

  const params = new URLSearchParams()
  if (sessionId) params.set('sessionId', sessionId)
  if (classFilter) params.set('classId', classFilter)
  const query = params.toString()
  // সেশনের তালিকা আসার আগে আনি না (নইলে প্রথমে সব, পরে ছাঁকা, এমন ঝিলিক দেখা যায়)
  const exams = useQuery(sessions.loading ? null : `/api/exams${query ? `?${query}` : ''}`)

  const sessionOptions = [
    { value: '', label: activeSession ? `সক্রিয় সেশন (${activeSession.name})` : 'সব সেশন' },
    ...sessionList.map((s) => ({ value: String(s.id), label: s.name })),
    { value: 'all', label: 'সব সেশন' },
  ]
  const classOptions = [
    { value: '', label: 'সব ক্লাস' },
    ...(classes.data ?? []).map((c) => ({ value: String(c.id), label: c.name })),
  ]

  return (
    <>
      <PageHeader
        title="পরীক্ষা"
        subtitle="পরীক্ষার তালিকা, বিষয়, মার্কস এন্ট্রি ও ফল"
        action={isAdmin && <Button onClick={() => setCreating(true)}>নতুন পরীক্ষা</Button>}
      />

      <Card className="mb-4 p-4">
        <div className="grid gap-3 sm:grid-cols-2">
          <SelectField
            label="সেশন"
            options={sessionOptions}
            value={sessionFilter}
            onChange={(event) => setSessionFilter(event.target.value)}
          />
          <SelectField
            label="ক্লাস"
            options={classOptions}
            value={classFilter}
            onChange={(event) => setClassFilter(event.target.value)}
          />
        </div>
      </Card>

      <Card>
        <LoadState loading={sessions.loading || exams.loading} error={exams.error}>
          {exams.data?.length === 0 ? (
            <EmptyState>কোনো পরীক্ষা পাওয়া যায়নি।</EmptyState>
          ) : (
            <Table headers={['পরীক্ষা', 'ধরন', 'ক্লাস', 'সেশন', 'অবস্থা']}>
              {exams.data?.map((exam) => {
                const info = examStatusInfo(exam.status)
                return (
                  <tr key={exam.id}>
                    <Td className="font-medium text-gray-900">
                      <Link to={`/exams/${exam.id}`} className="hover:text-indigo-700">
                        {exam.name}
                      </Link>
                    </Td>
                    <Td>{examTypeLabel(exam.type)}</Td>
                    <Td>{exam.className}</Td>
                    <Td>{exam.sessionName}</Td>
                    <Td>
                      <Badge tone={info.tone}>{info.label}</Badge>
                    </Td>
                  </tr>
                )
              })}
            </Table>
          )}
        </LoadState>
      </Card>

      {creating && (
        <ExamCreateModal
          onDone={(detail) => navigate(`/exams/${detail.exam.id}`)}
          onClose={() => setCreating(false)}
        />
      )}
    </>
  )
}