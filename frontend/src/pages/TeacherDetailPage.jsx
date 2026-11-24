import { useRef, useState } from 'react'
import { Link, useParams } from 'react-router'
import { api, apiUpload } from '../api/client.js'
import ClassSectionFields from '../components/ClassSectionFields.jsx'
import EntityForm from '../components/EntityForm.jsx'
import { ConfirmModal, FormActions, InfoRow } from '../components/extra.jsx'
import TeacherPhoto from '../components/TeacherPhoto.jsx'
import { Alert, Badge, Button, Card, EmptyState, LoadState, Modal, PageHeader, SelectField, Table, Td } from '../components/ui.jsx'
import { useQuery } from '../hooks/useQuery.js'
import { formatDateOrDash } from '../utils/format.js'
import { shrinkImage } from '../utils/image.js'

function PhotoCard({ teacher, onPhotoChange }) {
  const inputRef = useRef(null)
  const [version, setVersion] = useState(0)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  async function onPick(event) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    setBusy(true)
    setError('')
    try {
      const blob = await shrinkImage(file)
      const form = new FormData()
      form.append('file', blob, 'teacher.jpg')
      await apiUpload(`/api/teachers/${teacher.id}/photo`, form)
      setVersion((v) => v + 1)
      if (onPhotoChange) onPhotoChange()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  async function remove() {
    setBusy(true)
    setError('')
    try {
      await api(`/api/teachers/${teacher.id}/photo`, { method: 'DELETE' })
      setVersion((v) => v + 1)
      if (onPhotoChange) onPhotoChange()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="flex flex-col items-center gap-3">
      <TeacherPhoto key={version} teacherId={teacher.id} name={teacher.fullName} />
      <input
        ref={inputRef}
        type="file"
        accept="image/jpeg,image/png"
        className="hidden"
        onChange={onPick}
      />
      <div className="flex gap-2">
        <Button variant="secondary" size="sm" disabled={busy} onClick={() => inputRef.current.click()}>
          {busy ? 'অপেক্ষা করুন...' : 'ছবি দিন / বদলান'}
        </Button>
        <Button variant="ghost" size="sm" disabled={busy} onClick={remove}>
          সরান
        </Button>
      </div>
      {error && <Alert>{error}</Alert>}
    </div>
  )
}

function AssignmentModal({ teacherId, sessions, onDone, onClose }) {
  const activeSession = sessions.find((s) => s.active)
  const [values, setValues] = useState({ sessionId: '', classId: '', sectionId: '', subjectId: '' })
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

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
  const users = useQuery('/api/admin/users')
  const sessions = useQuery('/api/sessions')
  const [sessionFilter, setSessionFilter] = useState('')
  const assignments = useQuery(`/api/teachers/${id}/assignments${sessionFilter ? `?sessionId=${sessionFilter}` : ''}`)
  const [modal, setModal] = useState(null) // { type: 'add' } অথবা { type: 'remove', assignment } অথবা { type: 'edit' }
  const [actionError, setActionError] = useState('')

  const teacher = teachers.data?.find((t) => String(t.id) === id)
  const sessionOptions = [
    { value: '', label: 'সব সেশন' },
    ...(sessions.data ?? []).map((s) => ({ value: String(s.id), label: s.active ? `${s.name} (সক্রিয়)` : s.name })),
  ]

  function done() {
    setModal(null)
    assignments.reload()
    teachers.reload()
  }

  async function removeAssignment() {
    await api(`/api/teachers/assignments/${modal.assignment.id}`, { method: 'DELETE' })
    done()
  }

  async function toggleActive() {
    setActionError('')
    try {
      await api(`/api/teachers/${id}/${teacher.active ? 'deactivate' : 'activate'}`, { method: 'POST' })
      teachers.reload()
    } catch (err) {
      setActionError(err.message)
    }
  }

  function buildEditFields(current) {
    const linked = new Set((teachers.data ?? []).map((t) => t.userId).filter(Boolean))
    const free = (users.data ?? []).filter(
      (u) => u.role === 'TEACHER' && u.status === 'ACTIVE' && (!linked.has(u.id) || u.id === current?.userId),
    )
    return [
      { name: 'employeeId', label: 'কর্মচারী আইডি (ঐচ্ছিক)', hint: 'যেমন: T-2026-001' },
      { name: 'fullName', label: 'পুরো নাম', required: true },
      { name: 'phone', label: 'ফোন নম্বর (ঐচ্ছিক)', hint: 'যেমন: 01712345678' },
      { name: 'email', label: 'ইমেইল (ঐচ্ছিক)', type: 'email' },
      { name: 'designation', label: 'পদবি (ঐচ্ছিক)', hint: 'যেমন: সহকারী শিক্ষক' },
      { name: 'qualification', label: 'শিক্ষাগত যোগ্যতা (ঐচ্ছিক)', hint: 'যেমন: M.Sc in Mathematics' },
      { name: 'joiningDate', label: 'যোগদানের তারিখ (ঐচ্ছিক)', type: 'date' },
      { name: 'address', label: 'ঠিকানা (ঐচ্ছিক)' },
      {
        name: 'userId',
        label: 'লগইন ইউজার (ঐচ্ছিক)',
        options: [
          { value: '', label: 'লগইন যুক্ত নেই' },
          ...free.map((u) => ({ value: String(u.id), label: `${u.fullName} (${u.username})` })),
        ],
      },
    ]
  }

  async function saveTeacher(values) {
    const body = {
      employeeId: values.employeeId || null,
      fullName: values.fullName,
      phone: values.phone || null,
      email: values.email || null,
      designation: values.designation || null,
      qualification: values.qualification || null,
      joiningDate: values.joiningDate || null,
      address: values.address || null,
      userId: values.userId ? Number(values.userId) : null,
    }
    await api(`/api/teachers/${id}`, { method: 'PUT', body })
    setModal(null)
    teachers.reload()
  }

  return (
    <>
      <div className="mb-3">
        <Link to="/teachers" className="text-sm text-[var(--ink-secondary)] hover:text-[var(--ink-primary)] font-medium transition-colors">
          ← সব শিক্ষক
        </Link>
      </div>

      {actionError && (
        <div className="mb-4">
          <Alert>{actionError}</Alert>
        </div>
      )}

      {teachers.data && !teacher ? (
        <Alert>এই শিক্ষককে পাওয়া যায়নি।</Alert>
      ) : (
        <PageHeader
          title={teacher ? teacher.fullName : '...'}
          subtitle={teacher ? `${teacher.designation ?? 'পদবি নেই'}${teacher.active ? '' : ' · নিষ্ক্রিয়'}` : undefined}
          action={
            teacher && (
              <div className="flex gap-2">
                <Button variant="secondary" onClick={() => setModal({ type: 'edit' })}>
                  প্রোফাইল সম্পাদনা
                </Button>
                <Button variant="ghost" onClick={toggleActive}>
                  {teacher.active ? 'নিষ্ক্রিয় করুন' : 'সক্রিয় করুন'}
                </Button>
                <Button onClick={() => setModal({ type: 'add' })} disabled={!sessions.data}>
                  নতুন বরাদ্দ
                </Button>
              </div>
            )
          }
        />
      )}

      {teacher && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
          <Card className="p-6 flex flex-col items-center">
            <PhotoCard teacher={teacher} onPhotoChange={() => teachers.reload()} />
            <div className="mt-4 text-center">
              <h3 className="font-semibold text-[var(--ink-primary)] text-lg">{teacher.fullName}</h3>
              <p className="text-sm text-[var(--ink-secondary)] font-semibold font-mono">{teacher.employeeId ?? 'আইডি দেওয়া নেই'}</p>
              <div className="mt-2">
                <Badge tone={teacher.active ? 'green' : 'gray'}>{teacher.active ? 'সক্রিয়' : 'নিষ্ক্রিয়'}</Badge>
              </div>
            </div>
          </Card>

          <Card className="p-6 md:col-span-2">
            <h3 className="font-semibold text-[var(--ink-primary)] border-b border-[var(--border-hairline)] pb-2 mb-3">ব্যক্তিগত ও প্রাতিষ্ঠানিক তথ্য</h3>
            <dl className="divide-y divide-[var(--border-hairline)]">
              <InfoRow label="কর্মচারী আইডি">{teacher.employeeId ?? '—'}</InfoRow>
              <InfoRow label="পদবি">{teacher.designation ?? '—'}</InfoRow>
              <InfoRow label="শিক্ষাগত যোগ্যতা">{teacher.qualification ?? '—'}</InfoRow>
              <InfoRow label="যোগদানের তারিখ">{formatDateOrDash(teacher.joiningDate)}</InfoRow>
              <InfoRow label="মোবাইল ফোন">{teacher.phone ?? '—'}</InfoRow>
              <InfoRow label="ইমেইল">{teacher.email ?? '—'}</InfoRow>
              <InfoRow label="বর্তমান ঠিকানা">{teacher.address ?? '—'}</InfoRow>
              <InfoRow label="লগইন ইউজারনেম">{teacher.username ? `${teacher.username} (TEACHER)` : 'কোনো অ্যাকাউন্ট যুক্ত নেই'}</InfoRow>
            </dl>
          </Card>
        </div>
      )}

      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-semibold text-[var(--ink-primary)]">শিক্ষক ক্লাস ও বিষয় বরাদ্দ</h3>
        <div className="w-56">
          <SelectField
            options={sessionOptions}
            value={sessionFilter}
            onChange={(event) => setSessionFilter(event.target.value)}
          />
        </div>
      </div>

      <Card>
        <LoadState loading={assignments.loading} error={assignments.error}>
          {assignments.data?.length === 0 ? (
            <EmptyState>এই সেশনে এখনো কোনো বরাদ্দ নেই।</EmptyState>
          ) : (
            <Table headers={['সেশন', 'ক্লাস / সেকশন', 'বিষয়', 'কাজ']}>
              {assignments.data?.map((a) => (
                <tr key={a.id}>
                  <Td className="font-medium text-[var(--ink-primary)]">{a.sessionName}</Td>
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
      {modal?.type === 'edit' && (
        <Modal title="শিক্ষক প্রোফাইল সম্পাদনা" onClose={() => setModal(null)}>
          <EntityForm
            fields={buildEditFields(teacher)}
            initial={{
              ...teacher,
              userId: teacher.userId ? String(teacher.userId) : '',
            }}
            submitLabel="সংরক্ষণ করুন"
            onSubmit={saveTeacher}
            onCancel={() => setModal(null)}
          />
        </Modal>
      )}
      {modal?.type === 'remove' && (
        <ConfirmModal
          title="বরাদ্দ সরান"
          message={`${modal.assignment.className} / ${modal.assignment.sectionName}-এর ${modal.assignment.subjectName} বরাদ্দটা সরাবেন? এই সেকশনে আর কোনো বরাদ্দ না থাকলে শিক্ষক ওই সেকশনের হাজিরা নিতে পারবেন না।`}
          confirmLabel="সরান"
          danger
          onConfirm={removeAssignment}
          onClose={() => setModal(null)}
        />
      )}
    </>
  )
}
