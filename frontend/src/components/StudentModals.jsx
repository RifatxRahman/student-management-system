import { useState } from 'react'
import { api } from '../api/client.js'
import { useQuery } from '../hooks/useQuery.js'
import { RELATION_OPTIONS, STUDENT_STATUS_OPTIONS } from '../utils/format.js'
import ClassSectionFields from './ClassSectionFields.jsx'
import EntityForm from './EntityForm.jsx'
import { FormActions } from './extra.jsx'
import GuardianFormModal from './GuardianFormModal.jsx'
import GuardianPicker from './GuardianPicker.jsx'
import { Alert, Button, Modal, SelectField, TextField } from './ui.jsx'

// ---------- অবস্থা বদল ----------
const STATUS_FIELDS = [{ name: 'status', label: 'নতুন অবস্থা', required: true, options: STUDENT_STATUS_OPTIONS }]

export function StatusModal({ student, onDone, onClose }) {
  async function save(values) {
    await api(`/api/students/${student.id}/status`, { method: 'POST', body: { status: values.status } })
    onDone()
  }

  return (
    <Modal title="অবস্থা বদলান" onClose={onClose}>
      <EntityForm
        fields={STATUS_FIELDS}
        initial={{ status: student.status }}
        submitLabel="সংরক্ষণ করুন"
        onSubmit={save}
        onCancel={onClose}
      />
    </Modal>
  )
}

// ---------- ছাড়পত্র ----------
const WITHDRAW_FIELDS = [
  { name: 'endDate', label: 'ছাড়পত্রের তারিখ (ঐচ্ছিক)', type: 'date', hint: 'খালি রাখলে আজকের তারিখ' },
]

export function WithdrawModal({ enrollment, onDone, onClose }) {
  async function save(values) {
    await api(`/api/enrollments/${enrollment.id}/withdraw`, {
      method: 'POST',
      body: { endDate: values.endDate || null },
    })
    onDone()
  }

  return (
    <Modal title="ছাড়পত্র দিন" onClose={onClose}>
      <div className="space-y-4">
        <Alert type="info">
          {enrollment.sessionName} সেশনের {enrollment.className} / {enrollment.sectionName} ভর্তিটা বন্ধ হবে।
          শিক্ষার্থীর তথ্য ও আগের হাজিরা থেকে যাবে।
        </Alert>
        <EntityForm
          fields={WITHDRAW_FIELDS}
          submitLabel="ছাড়পত্র দিন"
          onSubmit={save}
          onCancel={onClose}
        />
      </div>
    </Modal>
  )
}

// ---------- নতুন ভর্তি ----------
export function EnrollModal({ studentId, onDone, onClose }) {
  const sessions = useQuery('/api/sessions')
  const [values, setValues] = useState({ sessionId: '', classId: '', sectionId: '', rollNumber: '', startDate: '' })
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  const sessionList = sessions.data ?? []
  const activeSession = sessionList.find((s) => s.active)
  // কিছু না বাছলে সক্রিয় সেশনটাই ধরা হয়
  const sessionId = values.sessionId || (activeSession ? String(activeSession.id) : '')
  const sessionOptions = [
    { value: '', label: 'বাছুন' },
    ...sessionList.map((s) => ({ value: String(s.id), label: s.active ? `${s.name} (সক্রিয়)` : s.name })),
  ]

  function change(name) {
    return (event) => setValues((current) => ({ ...current, [name]: event.target.value }))
  }

  async function submit(event) {
    event.preventDefault()
    setError(null)
    setBusy(true)
    try {
      await api(`/api/students/${studentId}/enrollments`, {
        method: 'POST',
        body: {
          sessionId: Number(sessionId),
          classId: Number(values.classId),
          sectionId: Number(values.sectionId),
          rollNumber: values.rollNumber ? Number(values.rollNumber) : null,
          startDate: values.startDate || null,
        },
      })
      onDone()
    } catch (err) {
      setError(err)
      setBusy(false)
    }
  }

  return (
    <Modal title="নতুন ভর্তি" onClose={onClose}>
      <form onSubmit={submit} className="space-y-4">
        {error && <Alert>{error.message}</Alert>}
        <SelectField label="সেশন" options={sessionOptions} value={sessionId} onChange={change('sessionId')} required />
        <ClassSectionFields
          classId={values.classId}
          sectionId={values.sectionId}
          required
          onChange={(patch) => setValues((current) => ({ ...current, ...patch }))}
        />
        <TextField
          label="রোল নম্বর (ঐচ্ছিক)"
          type="number"
          min="1"
          hint="খালি রাখলে সিস্টেম নিজে পরের রোল দেবে"
          value={values.rollNumber}
          onChange={change('rollNumber')}
        />
        <TextField
          label="ভর্তির তারিখ (ঐচ্ছিক)"
          type="date"
          hint="খালি রাখলে আজকের তারিখ"
          value={values.startDate}
          onChange={change('startDate')}
        />
        <FormActions busy={busy} submitLabel="ভর্তি করুন" onCancel={onClose} />
      </form>
    </Modal>
  )
}

// ---------- বদলি (ক্লাস/সেকশন বদল) ----------
export function TransferModal({ enrollment, onDone, onClose }) {
  const [values, setValues] = useState({ classId: '', sectionId: '', rollNumber: '', effectiveDate: '' })
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  function change(name) {
    return (event) => setValues((current) => ({ ...current, [name]: event.target.value }))
  }

  async function submit(event) {
    event.preventDefault()
    setError(null)
    setBusy(true)
    try {
      await api(`/api/enrollments/${enrollment.id}/transfer`, {
        method: 'POST',
        body: {
          classId: Number(values.classId),
          sectionId: Number(values.sectionId),
          rollNumber: values.rollNumber ? Number(values.rollNumber) : null,
          effectiveDate: values.effectiveDate || null,
        },
      })
      onDone()
    } catch (err) {
      setError(err)
      setBusy(false)
    }
  }

  return (
    <Modal title="বদলি" onClose={onClose}>
      <form onSubmit={submit} className="space-y-4">
        <Alert type="info">
          এখন: {enrollment.className} / {enrollment.sectionName} (রোল {enrollment.rollNumber})। পুরনো ভর্তি বন্ধ
          হয়ে নতুন ভর্তি তৈরি হবে।
        </Alert>
        {error && <Alert>{error.message}</Alert>}
        <ClassSectionFields
          classId={values.classId}
          sectionId={values.sectionId}
          required
          onChange={(patch) => setValues((current) => ({ ...current, ...patch }))}
        />
        <TextField
          label="নতুন রোল নম্বর (ঐচ্ছিক)"
          type="number"
          min="1"
          hint="খালি রাখলে সিস্টেম নিজে পরের রোল দেবে"
          value={values.rollNumber}
          onChange={change('rollNumber')}
        />
        <TextField
          label="বদলির তারিখ (ঐচ্ছিক)"
          type="date"
          hint="খালি রাখলে আজকের তারিখ"
          value={values.effectiveDate}
          onChange={change('effectiveDate')}
        />
        <FormActions busy={busy} submitLabel="বদলি করুন" onCancel={onClose} />
      </form>
    </Modal>
  )
}

// ---------- আরেকজন অভিভাবক যোগ ----------
export function LinkGuardianModal({ studentId, onDone, onClose }) {
  const [guardian, setGuardian] = useState(null)
  const [showNew, setShowNew] = useState(false)
  const [relation, setRelation] = useState('MOTHER')
  const [primary, setPrimary] = useState(false)
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  if (showNew) {
    return (
      <GuardianFormModal
        onSaved={(saved) => {
          setGuardian(saved)
          setShowNew(false)
        }}
        onClose={() => setShowNew(false)}
      />
    )
  }

  async function submit(event) {
    event.preventDefault()
    setError(null)
    setBusy(true)
    try {
      await api(`/api/students/${studentId}/guardians`, {
        method: 'POST',
        body: { guardianId: guardian.id, relation, primary },
      })
      onDone()
    } catch (err) {
      setError(err)
      setBusy(false)
    }
  }

  return (
    <Modal title="অভিভাবক যোগ করুন" onClose={onClose}>
      {!guardian ? (
        <div className="space-y-4">
          <GuardianPicker onSelect={setGuardian} />
          <div className="flex items-center justify-between gap-2 border-t border-gray-100 pt-3">
            <Button variant="ghost" size="sm" onClick={() => setShowNew(true)}>
              নতুন অভিভাবক বানান
            </Button>
            <Button variant="secondary" onClick={onClose}>
              বাতিল
            </Button>
          </div>
        </div>
      ) : (
        <form onSubmit={submit} className="space-y-4">
          {error && <Alert>{error.message}</Alert>}
          <div className="rounded-lg bg-gray-50 px-4 py-3 text-sm">
            <p className="font-medium text-gray-900">{guardian.fullName}</p>
            <p className="text-gray-500">{guardian.phone}</p>
          </div>
          <SelectField
            label="সম্পর্ক"
            options={RELATION_OPTIONS}
            value={relation}
            onChange={(event) => setRelation(event.target.value)}
          />
          <label className="flex items-center gap-2 text-sm text-gray-700">
            <input type="checkbox" checked={primary} onChange={(event) => setPrimary(event.target.checked)} />
            প্রধান অভিভাবক করুন (অনুপস্থিতির বার্তা এই নম্বরে যাবে)
          </label>
          <FormActions busy={busy} submitLabel="যোগ করুন" onCancel={onClose} />
        </form>
      )}
    </Modal>
  )
}