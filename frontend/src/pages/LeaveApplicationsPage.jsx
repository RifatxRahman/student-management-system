import { useState } from 'react'
import { api } from '../api/client.js'
import { useAuth } from '../auth/useAuth.js'
import { FormActions } from '../components/extra.jsx'
import { Alert, Badge, Button, Card, EmptyState, LoadState, Modal, PageHeader, SelectField, Table, Td, TextField } from '../components/ui.jsx'
import { useQuery } from '../hooks/useQuery.js'
import { formatDate } from '../utils/format.js'

const LEAVE_TYPES = [
  { value: 'CASUAL', label: 'নৈমিত্তিক ছুটি (Casual)' },
  { value: 'SICK', label: 'চিকিৎসাজনিত ছুটি (Sick)' },
  { value: 'EARNED', label: 'অর্জিত ছুটি (Earned)' },
  { value: 'MATERNITY', label: 'মাতৃত্বকালীন ছুটি (Maternity)' },
  { value: 'UNPAID', label: 'বিনা বেতনে ছুটি (Unpaid)' },
]

const STATUS_FILTERS = [
  { value: '', label: 'সব আবেদন' },
  { value: 'PENDING', label: 'অপেক্ষমান' },
  { value: 'APPROVED', label: 'অনুমোদিত' },
  { value: 'REJECTED', label: 'প্রত্যাখ্যাত' },
  { value: 'CANCELLED', label: 'বাতিল' },
]

function ApplyLeaveModal({ teachers, isAdmin, onDone, onClose }) {
  const [values, setValues] = useState({
    teacherId: '',
    leaveType: 'CASUAL',
    startDate: '',
    endDate: '',
    totalDays: '',
    reason: '',
  })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const teacherOptions = [
    { value: '', label: 'শিক্ষক বাছাই করুন' },
    ...(teachers ?? []).map((t) => ({ value: String(t.id), label: `${t.fullName} (${t.employeeId || 'আইডি নেই'})` })),
  ]

  async function submit(e) {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      const endpoint = isAdmin ? '/api/leaves' : '/api/leaves/me'
      const body = {
        teacherId: isAdmin && values.teacherId ? Number(values.teacherId) : null,
        leaveType: values.leaveType,
        startDate: values.startDate,
        endDate: values.endDate,
        totalDays: values.totalDays ? Number(values.totalDays) : null,
        reason: values.reason,
      }
      await api(endpoint, { method: 'POST', body })
      onDone()
    } catch (err) {
      setError(err.message)
      setBusy(false)
    }
  }

  return (
    <Modal title="নতুন ছুটির আবেদন" onClose={onClose}>
      <form onSubmit={submit} className="space-y-4">
        {error && <Alert>{error}</Alert>}
        {isAdmin && (
          <SelectField
            label="শিক্ষক"
            options={teacherOptions}
            value={values.teacherId}
            required
            onChange={(e) => setValues((c) => ({ ...c, teacherId: e.target.value }))}
          />
        )}
        <SelectField
          label="ছুটির ধরন"
          options={LEAVE_TYPES}
          value={values.leaveType}
          required
          onChange={(e) => setValues((c) => ({ ...c, leaveType: e.target.value }))}
        />
        <div className="grid grid-cols-2 gap-4">
          <TextField
            label="শুরুর তারিখ"
            type="date"
            value={values.startDate}
            required
            onChange={(e) => setValues((c) => ({ ...c, startDate: e.target.value }))}
          />
          <TextField
            label="শেষের তারিখ"
            type="date"
            value={values.endDate}
            required
            onChange={(e) => setValues((c) => ({ ...c, endDate: e.target.value }))}
          />
        </div>
        <TextField
          label="মোট দিন (ঐচ্ছিক - খালি রাখলে স্বয়ংক্রিয় গণনা হবে)"
          type="number"
          step="0.5"
          value={values.totalDays}
          placeholder="যেমন: ৩ বা ০.৫"
          onChange={(e) => setValues((c) => ({ ...c, totalDays: e.target.value }))}
        />
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">ছুটির কারণ</label>
          <textarea
            required
            rows={3}
            value={values.reason}
            onChange={(e) => setValues((c) => ({ ...c, reason: e.target.value }))}
            className="w-full rounded-md border border-gray-300 p-2 text-sm shadow-sm focus:border-indigo-500 focus:outline-none"
            placeholder="ছুটির সুনির্দিষ্ট কারণ লিখুন..."
          />
        </div>
        <FormActions busy={busy} submitLabel="আবেদন জমা দিন" onCancel={onClose} />
      </form>
    </Modal>
  )
}

function RejectModal({ leave, onDone, onClose }) {
  const [reason, setReason] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(e) {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      await api(`/api/leaves/${leave.id}/reject`, {
        method: 'POST',
        body: { reason },
      })
      onDone()
    } catch (err) {
      setError(err.message)
      setBusy(false)
    }
  }

  return (
    <Modal title="ছুটির আবেদন প্রত্যাখ্যান" onClose={onClose}>
      <form onSubmit={submit} className="space-y-4">
        {error && <Alert>{error}</Alert>}
        <p className="text-sm text-gray-600">
          <strong>{leave.teacherName}</strong>-এর ({leave.startDate} হতে {leave.endDate}) ছুটির আবেদন প্রত্যাখ্যানের কারণ উল্লেখ করুন:
        </p>
        <textarea
          required
          rows={3}
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          className="w-full rounded-md border border-gray-300 p-2 text-sm shadow-sm focus:border-rose-500 focus:outline-none"
          placeholder="প্রত্যাখ্যানের সুস্পষ্ট কারণ লিখুন..."
        />
        <div className="flex justify-end gap-2">
          <Button variant="secondary" onClick={onClose} disabled={busy}>
            বাতিল
          </Button>
          <Button variant="danger" type="submit" disabled={busy}>
            {busy ? 'অপেক্ষা করুন...' : 'প্রত্যাখ্যান নিশ্চিত করুন'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}

export default function LeaveApplicationsPage() {
  const { user } = useAuth()
  const isAdmin = user?.role === 'ADMIN'
  const [statusFilter, setStatusFilter] = useState('')
  const endpoint = isAdmin
    ? `/api/leaves${statusFilter ? `?status=${statusFilter}` : ''}`
    : '/api/leaves/me'
  const leaves = useQuery(endpoint)
  const teachers = useQuery(isAdmin ? '/api/teachers' : null)

  const [modal, setModal] = useState(null) // { type: 'apply' } | { type: 'reject', leave } | { type: 'approve', leave } | { type: 'cancel', leave }
  const [actionError, setActionError] = useState('')

  function reload() {
    setModal(null)
    leaves.reload()
  }

  async function approveLeave(leave) {
    setActionError('')
    try {
      await api(`/api/leaves/${leave.id}/approve`, { method: 'POST', body: {} })
      reload()
    } catch (err) {
      setActionError(err.message)
    }
  }

  async function cancelLeave(leave) {
    setActionError('')
    try {
      await api(`/api/leaves/${leave.id}/cancel`, { method: 'POST' })
      reload()
    } catch (err) {
      setActionError(err.message)
    }
  }

  function statusTone(status) {
    switch (status) {
      case 'APPROVED':
        return 'green'
      case 'PENDING':
        return 'yellow'
      case 'REJECTED':
        return 'red'
      default:
        return 'gray'
    }
  }

  return (
    <>
      <PageHeader
        title="শিক্ষক ছুটির আবেদন ও অনুমোদন"
        subtitle="অনুমোদিত ছুটি স্বয়ংক্রিয়ভাবে শিক্ষক উপস্থিতির খাতায় সমন্বয় হয়ে যায়"
        action={
          <Button onClick={() => setModal({ type: 'apply' })}>
            নতুন ছুটির আবেদন
          </Button>
        }
      />

      {actionError && (
        <div className="mb-4">
          <Alert>{actionError}</Alert>
        </div>
      )}

      {isAdmin && (
        <div className="mb-4 flex gap-2">
          {STATUS_FILTERS.map((f) => (
            <button
              key={f.value}
              onClick={() => setStatusFilter(f.value)}
              className={`rounded-lg px-3 py-1.5 text-xs font-medium transition ${
                statusFilter === f.value
                  ? 'bg-indigo-600 text-white shadow-sm'
                  : 'bg-white text-gray-600 border border-gray-200 hover:bg-gray-50'
              }`}
            >
              {f.label}
            </button>
          ))}
        </div>
      )}

      <Card>
        <LoadState loading={leaves.loading} error={leaves.error}>
          {leaves.data?.length === 0 ? (
            <EmptyState>কোনো ছুটির আবেদন পাওয়া যায়নি।</EmptyState>
          ) : (
            <Table headers={['শিক্ষক', 'ছুটির ধরন', 'সময়কাল', 'মোট দিন', 'কারণ', 'অবস্থা', 'অ্যাকশন তথ্য', 'কাজ']}>
              {leaves.data?.map((l) => (
                <tr key={l.id}>
                  <Td>
                    <p className="font-medium text-gray-900">{l.teacherName}</p>
                    <p className="font-mono text-xs text-indigo-600">{l.employeeId || '—'}</p>
                  </Td>
                  <Td>
                    <Badge tone={l.leaveType === 'UNPAID' ? 'red' : 'blue'}>
                      {l.leaveType}
                    </Badge>
                  </Td>
                  <Td className="text-xs">
                    <p className="font-medium text-gray-900">{formatDate(l.startDate)}</p>
                    <p className="text-gray-500">হতে {formatDate(l.endDate)}</p>
                  </Td>
                  <Td className="font-semibold text-gray-800">{l.totalDays} দিন</Td>
                  <Td className="max-w-xs truncate text-xs text-gray-700" title={l.reason}>
                    {l.reason}
                  </Td>
                  <Td>
                    <Badge tone={statusTone(l.status)}>{l.status}</Badge>
                  </Td>
                  <Td className="text-xs">
                    {l.actionByName && (
                      <p className="text-gray-700">দ্বারা: <span className="font-medium">{l.actionByName}</span></p>
                    )}
                    {l.actionReason && (
                      <p className="italic text-gray-500">"{l.actionReason}"</p>
                    )}
                    {!l.actionByName && !l.actionReason && <span className="text-gray-400">—</span>}
                  </Td>
                  <Td>
                    <div className="flex flex-wrap gap-1.5">
                      {isAdmin && l.status === 'PENDING' && (
                        <>
                          <Button size="sm" onClick={() => approveLeave(l)}>
                            অনুমোদন
                          </Button>
                          <Button variant="danger" size="sm" onClick={() => setModal({ type: 'reject', leave: l })}>
                            বাতিল
                          </Button>
                        </>
                      )}
                      {l.status === 'PENDING' && (!isAdmin || l.teacherId) && (
                        <Button variant="ghost" size="sm" onClick={() => cancelLeave(l)}>
                          বাতিল করুন
                        </Button>
                      )}
                    </div>
                  </Td>
                </tr>
              ))}
            </Table>
          )}
        </LoadState>
      </Card>

      {modal?.type === 'apply' && (
        <ApplyLeaveModal
          teachers={teachers.data}
          isAdmin={isAdmin}
          onDone={reload}
          onClose={() => setModal(null)}
        />
      )}
      {modal?.type === 'reject' && (
        <RejectModal
          leave={modal.leave}
          onDone={reload}
          onClose={() => setModal(null)}
        />
      )}
    </>
  )
}
