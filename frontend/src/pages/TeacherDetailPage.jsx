import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { api } from '../api/client.js'
import ClassSectionFields from '../components/ClassSectionFields.jsx'
import { ConfirmModal, FormActions } from '../components/extra.jsx'
import { Alert, Badge, Button, Card, EmptyState, LoadState, Modal, PageHeader, SelectField, Table, Td } from '../components/ui.jsx'
import { useQuery } from '../hooks/useQuery.js'

function AssignmentModal({ teacherId, sessions, onDone, onClose }) {
  const activeSession = sessions.find((s) => s.active)
  const [values, setValues] = useState({ sessionId: '', classId: '', sectionId: '', subjectId: '' })
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  // কিছু না বাছলে সক্রিয় সেশনটাই ধরা হয়
  const sessionId = values.sessionId || (activeSession ? String(activeSession.id) : '')
  const subjects = useQuery(values.classId ? `/api/classes/${values.classId}/subjects` : null)

  const sessionOptions = [
    { value: '', label: 'বাছুন' },
    ...sessions.map((s) => ({ value: String(s.id), label: s.active ? `${s.name} (সক্রিয়)` : s.name })),
  ]
  const subjectOptions = [
    { value: '', label: values.classId ? 'বাছুন' : 'আগে ক্লাস বাছুন' },
    ...(subjects.data ?? [])
      .filter((s) => s.active !== false)
      .map((s) => ({ value: String(s.id), label: s.code ? `${s.name} (${s.code})` : s.name })),
  ]

  async function submit(event) {
    event.preventDefault()
    setError(null)
    setBusy(true)
    try {
      await api(`/api/teachers/${teacherId}/assignments`, {
        method: 'POST',
        body: {
          sessionId: Number(sessionId),
          sectionId: Number(values.sectionId),
          subjectId: Number(values.subjectId),
        },
      })
      onDone()
    } catch (err) {
      setError(err)
      setBusy(false)
    }
  }

  return (
    <Modal title="নতুন বরাদ্দ" onClose={onClose}>
      <form onSubmit={submit} className="space-y-4">
        {error && <Alert>{error.message}</Alert>}
        <SelectField
          label="সেশন"
          options={sessionOptions}
          value={sessionId}
          required
          onChange={(event) => setValues((c) => ({ ...c, sessionId: event.target.value }))}
        />
        <ClassSectionFields
          classId={values.classId}
          sectionId={values.sectionId}
          required
          onChange={(patch) =>
            setValues((c) => ({ ...c, ...patch, subjectId: patch.classId === c.classId ? c.subjectId : '' }))
          }
        />
        <SelectField
          label="বিষয়"
          options={subjectOptions}
          value={values.subjectId}
          required
          disabled={!values.classId}
          onChange={(event) => setValues((c) => ({ ...c, subjectId: event.target.value }))}
        />
        <FormActions busy={busy} submitLabel="বরাদ্দ করুন" onCancel={onClose} />
      </form>
    </Modal>
  )
}

export default function TeacherDetailPage() {
  const { id } = useParams()
  const teachers = useQuery('/api/teachers')
  const sessions = useQuery('/api/sessions')
  const [sessionFilter, setSessionFilter] = useState('')
  const assignments = useQuery(`/api/teachers/${id}/assignments${sessionFilter ? `?sessionId=${sessionFilter}` : ''}`)
  const [modal, setModal] = useState(null) // { type: 'add' } অথবা { type: 'remove', assignment }

  const teacher = teachers.data?.find((t) => String(t.id) === id)
  const sessionOptions = [
    { value: '', label: 'সব সেশন' },
    ...(sessions.data ?? []).map((s) => ({ value: String(s.id), label: s.active ? `${s.name} (সক্রিয়)` : s.name })),
  ]

  function done() {
    setModal(null)
    assignments.reload()
  }

  async function remove() {
    await api(`/api/teachers/assignments/${modal.assignment.id}`, { method: 'DELETE' })
    done()
  }

  return (
    <>
      <div className="mb-3">
        <Link to="/teachers" className="text-sm text-gray-500 hover:text-gray-700">
          ← সব শিক্ষক
        </Link>
      </div>

      {teachers.data && !teacher ? (
        <Alert>এই শিক্ষককে পাওয়া যায়নি।</Alert>
      ) : (
        <PageHeader
          title={teacher ? teacher.fullName : '...'}
          subtitle={teacher ? `${teacher.designation ?? 'পদবি নেই'}${teacher.active ? '' : ' · নিষ্ক্রিয়'}` : undefined}
          action={
            teacher && (
              <Button onClick={() => setModal({ type: 'add' })} disabled={!sessions.data}>
                নতুন বরাদ্দ
              </Button>
            )
          }
        />
      )}

      <div className="mb-4 max-w-xs">
        <SelectField
          label="সেশন অনুযায়ী দেখুন"
          options={sessionOptions}
          value={sessionFilter}
          onChange={(event) => setSessionFilter(event.target.value)}
        />
      </div>

      <Card>
        <LoadState loading={assignments.loading} error={assignments.error}>
          {assignments.data?.length === 0 ? (
            <EmptyState>এখনো কোনো বরাদ্দ নেই।</EmptyState>
          ) : (
            <Table headers={['সেশন', 'ক্লাস / সেকশন', 'বিষয়', 'কাজ']}>
              {assignments.data?.map((a) => (
                <tr key={a.id}>
                  <Td className="font-medium text-gray-900">{a.sessionName}</Td>
                  <Td>
                    {a.className} / {a.sectionName}
                  </Td>
                  <Td>
                    {a.subjectName}
                    {a.subjectCode && (
                      <span className="ml-2">
                        <Badge>{a.subjectCode}</Badge>
                      </span>
                    )}
                  </Td>
                  <Td>
                    <Button variant="ghost" size="sm" onClick={() => setModal({ type: 'remove', assignment: a })}>
                      সরান
                    </Button>
                  </Td>
                </tr>
              ))}
            </Table>
          )}
        </LoadState>
      </Card>

      {modal?.type === 'add' && (
        <AssignmentModal teacherId={id} sessions={sessions.data ?? []} onDone={done} onClose={() => setModal(null)} />
      )}
      {modal?.type === 'remove' && (
        <ConfirmModal
          title="বরাদ্দ সরান"
          message={`${modal.assignment.className} / ${modal.assignment.sectionName}-এর ${modal.assignment.subjectName} বরাদ্দটা সরাবেন? এই সেকশনে আর কোনো বরাদ্দ না থাকলে শিক্ষক ওই সেকশনের হাজিরা নিতে পারবেন না।`}
          confirmLabel="সরান"
          danger
          onConfirm={remove}
          onClose={() => setModal(null)}
        />
      )}
    </>
  )
}