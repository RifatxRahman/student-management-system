import { useState } from 'react'
import { api } from '../api/client.js'
import { useQuery } from '../hooks/useQuery.js'
import { EXAM_TYPE_OPTIONS } from '../utils/format.js'
import EntityForm from './EntityForm.jsx'
import { FormActions } from './extra.jsx'
import { Alert, Modal, SelectField, TextField } from './ui.jsx'

// ---------- নতুন পরীক্ষা ----------
export function ExamCreateModal({ onDone, onClose }) {
  const sessions = useQuery('/api/sessions')
  const classes = useQuery('/api/classes')
  const [values, setValues] = useState({ sessionId: '', classId: '', name: '', type: 'MIDTERM', copySubjects: true })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const sessionList = sessions.data ?? []
  const activeSession = sessionList.find((s) => s.active)
  // কিছু না বাছলে সক্রিয় সেশনটাই ধরা হয়
  const sessionId = values.sessionId || (activeSession ? String(activeSession.id) : '')

  const sessionOptions = [
    { value: '', label: 'বাছুন' },
    ...sessionList.map((s) => ({ value: String(s.id), label: s.active ? `${s.name} (সক্রিয়)` : s.name })),
  ]
  const classOptions = [
    { value: '', label: 'বাছুন' },
    ...(classes.data ?? []).filter((c) => c.active).map((c) => ({ value: String(c.id), label: c.name })),
  ]

  function change(name) {
    return (event) => setValues((current) => ({ ...current, [name]: event.target.value }))
  }

  async function submit(event) {
    event.preventDefault()
    setBusy(true)
    setError('')
    try {
      const detail = await api('/api/exams', {
        method: 'POST',
        body: {
          sessionId: Number(sessionId),
          classId: Number(values.classId),
          name: values.name.trim(),
          type: values.type,
          copySubjects: values.copySubjects,
        },
      })
      onDone(detail)
    } catch (err) {
      setError(err.message)
      setBusy(false)
    }
  }

  return (
    <Modal title="নতুন পরীক্ষা" onClose={onClose}>
      <form onSubmit={submit} className="space-y-4">
        {error && <Alert>{error}</Alert>}
        <SelectField label="সেশন" options={sessionOptions} value={sessionId} onChange={change('sessionId')} required />
        <SelectField label="ক্লাস" options={classOptions} value={values.classId} onChange={change('classId')} required />
        <TextField
          label="পরীক্ষার নাম"
          hint="যেমন: অর্ধবার্ষিক পরীক্ষা"
          maxLength={100}
          value={values.name}
          onChange={change('name')}
          required
        />
        <SelectField label="ধরন" options={EXAM_TYPE_OPTIONS} value={values.type} onChange={change('type')} />
        <label className="flex items-start gap-2 text-sm text-gray-700">
          <input
            type="checkbox"
            className="mt-1"
            checked={values.copySubjects}
            onChange={(event) => setValues((current) => ({ ...current, copySubjects: event.target.checked }))}
          />
          <span>
            ক্লাসের সব বিষয় যোগ করুন (প্রতিটা ফুল ১০০, পাস ৩৩)
            <span className="block text-xs text-gray-500">পরে প্রতিটা বিষয়ের মার্কস আলাদাভাবে বদলানো যায়।</span>
          </span>
        </label>
        <FormActions busy={busy} submitLabel="পরীক্ষা তৈরি করুন" onCancel={onClose} />
      </form>
    </Modal>
  )
}

// ---------- নাম ও ধরন বদল ----------
const EDIT_FIELDS = [
  { name: 'name', label: 'পরীক্ষার নাম', required: true },
  { name: 'type', label: 'ধরন', required: true, options: EXAM_TYPE_OPTIONS },
]

export function ExamEditModal({ exam, onDone, onClose }) {
  async function save(values) {
    await api(`/api/exams/${exam.id}`, { method: 'PUT', body: { name: values.name.trim(), type: values.type } })
    onDone()
  }

  return (
    <Modal title="পরীক্ষা সম্পাদনা" onClose={onClose}>
      <EntityForm
        fields={EDIT_FIELDS}
        initial={{ name: exam.name, type: exam.type }}
        submitLabel="সংরক্ষণ করুন"
        onSubmit={save}
        onCancel={onClose}
      />
    </Modal>
  )
}

// ---------- বিষয় যোগ ----------
export function SubjectAddModal({ exam, existingSubjectIds, onDone, onClose }) {
  const classSubjects = useQuery(`/api/classes/${exam.classId}/subjects`)
  const [values, setValues] = useState({ subjectId: '', fullMarks: '100', passMarks: '33', optional: false })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const subjectOptions = [
    { value: '', label: 'বাছুন' },
    ...(classSubjects.data ?? [])
      .filter((s) => s.active !== false && !existingSubjectIds.includes(s.id))
      .map((s) => ({ value: String(s.id), label: s.code ? `${s.name} (${s.code})` : s.name })),
  ]

  function change(name) {
    return (event) => setValues((current) => ({ ...current, [name]: event.target.value }))
  }

  async function submit(event) {
    event.preventDefault()
    const full = Number(values.fullMarks)
    const pass = Number(values.passMarks)
    if (!(full > 0)) {
      setError('ফুল মার্কস ০-র বেশি হতে হবে')
      return
    }
    if (Number.isNaN(pass) || pass < 0 || pass > full) {
      setError('পাস মার্কস ০ থেকে ফুল মার্কসের মধ্যে হতে হবে')
      return
    }
    setBusy(true)
    setError('')
    try {
      await api(`/api/exams/${exam.id}/subjects`, {
        method: 'POST',
        body: { subjectId: Number(values.subjectId), fullMarks: full, passMarks: pass, optional: values.optional },
      })
      onDone()
    } catch (err) {
      setError(err.message)
      setBusy(false)
    }
  }

  return (
    <Modal title="বিষয় যোগ করুন" onClose={onClose}>
      <form onSubmit={submit} className="space-y-4">
        {error && <Alert>{error}</Alert>}
        <SelectField
          label="বিষয়"
          options={subjectOptions}
          value={values.subjectId}
          onChange={change('subjectId')}
          required
        />
        <p className="-mt-2 text-xs text-gray-500">শুধু এই ক্লাসের বিষয় আসে। না পেলে আগে ক্লাস পাতা থেকে বিষয় যোগ করুন।</p>
        <TextField label="ফুল মার্কস" type="number" min="0.01" step="0.01" value={values.fullMarks} onChange={change('fullMarks')} required />
        <TextField label="পাস মার্কস" type="number" min="0" step="0.01" value={values.passMarks} onChange={change('passMarks')} required />
        <label className="flex items-center gap-2 text-sm text-gray-700">
          <input
            type="checkbox"
            checked={values.optional}
            onChange={(event) => setValues((current) => ({ ...current, optional: event.target.checked }))}
          />
          ঐচ্ছিক বিষয় (পাস না করলে ফেল ধরা হয় না, ভালো করলে GPA-তে বোনাস)
        </label>
        <FormActions busy={busy} submitLabel="যোগ করুন" onCancel={onClose} />
      </form>
    </Modal>
  )
}

// ---------- বিষয়ের মার্কস বদল ----------
export function SubjectEditModal({ subject, onDone, onClose }) {
  const [values, setValues] = useState({
    fullMarks: String(Number(subject.fullMarks)),
    passMarks: String(Number(subject.passMarks)),
    optional: subject.optional,
  })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  function change(name) {
    return (event) => setValues((current) => ({ ...current, [name]: event.target.value }))
  }

  async function submit(event) {
    event.preventDefault()
    const full = Number(values.fullMarks)
    const pass = Number(values.passMarks)
    if (!(full > 0)) {
      setError('ফুল মার্কস ০-র বেশি হতে হবে')
      return
    }
    if (Number.isNaN(pass) || pass < 0 || pass > full) {
      setError('পাস মার্কস ০ থেকে ফুল মার্কসের মধ্যে হতে হবে')
      return
    }
    setBusy(true)
    setError('')
    try {
      await api(`/api/exams/subjects/${subject.id}`, {
        method: 'PUT',
        body: { fullMarks: full, passMarks: pass, optional: values.optional },
      })
      onDone()
    } catch (err) {
      setError(err.message)
      setBusy(false)
    }
  }

  return (
    <Modal title={`${subject.subjectName}: মার্কস বদল`} onClose={onClose}>
      <form onSubmit={submit} className="space-y-4">
        {error && <Alert>{error}</Alert>}
        <TextField label="ফুল মার্কস" type="number" min="0.01" step="0.01" value={values.fullMarks} onChange={change('fullMarks')} required />
        <TextField label="পাস মার্কস" type="number" min="0" step="0.01" value={values.passMarks} onChange={change('passMarks')} required />
        <label className="flex items-center gap-2 text-sm text-gray-700">
          <input
            type="checkbox"
            checked={values.optional}
            onChange={(event) => setValues((current) => ({ ...current, optional: event.target.checked }))}
          />
          ঐচ্ছিক বিষয়
        </label>
        <p className="text-xs text-gray-500">
          এই বিষয়ে ইতিমধ্যে মার্কস এন্ট্রি হয়ে থাকলে ফুল মার্কস তার সর্বোচ্চ নম্বরের কম করা যায় না।
        </p>
        <FormActions busy={busy} submitLabel="সংরক্ষণ করুন" onCancel={onClose} />
      </form>
    </Modal>
  )
}