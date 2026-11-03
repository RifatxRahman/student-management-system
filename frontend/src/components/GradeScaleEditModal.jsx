import { useState } from 'react'
import { api } from '../api/client.js'
import { FormActions } from './extra.jsx'
import { Alert, Button, Modal } from './ui.jsx'

const INPUT =
  'block w-full rounded-lg border border-gray-300 px-2.5 py-1.5 text-sm outline-none focus:ring-2 focus:ring-indigo-500'

// ভুল থাকলে বাংলা বার্তা, ঠিক থাকলে ফাঁকা লেখা (Backend-এর নিয়মের হুবহু প্রতিফলন)
function validate(rows) {
  if (rows.length < 2) return 'কমপক্ষে ২টা গ্রেড লাগবে'

  const mins = new Set()
  const names = new Set()
  const parsed = []
  for (const row of rows) {
    const min = Number(row.minPercent)
    const point = Number(row.gradePoint)
    const grade = row.grade.trim()
    if (row.minPercent.trim() === '' || Number.isNaN(min) || min < 0 || min > 100) {
      return 'শুরুর শতাংশ ০ থেকে ১০০-র মধ্যে হতে হবে'
    }
    if (!grade) return 'প্রতিটা গ্রেডের নাম লিখুন'
    if (row.gradePoint.trim() === '' || Number.isNaN(point) || point < 0 || point > 10) {
      return 'গ্রেড পয়েন্ট ০ থেকে ১০-এর মধ্যে হতে হবে'
    }
    if (mins.has(min)) return `দুটো গ্রেডের শুরু একই: ${min}%`
    if (names.has(grade.toLowerCase())) return `একই গ্রেডের নাম দুবার আছে: ${grade}`
    mins.add(min)
    names.add(grade.toLowerCase())
    parsed.push({ min, point, grade })
  }

  parsed.sort((a, b) => a.min - b.min)
  if (parsed[0].min !== 0) return 'সবচেয়ে নিচের গ্রেড ০% থেকে শুরু হতে হবে, নইলে কিছু নম্বরের গ্রেড থাকবে না'
  for (let i = 1; i < parsed.length; i += 1) {
    if (parsed[i].point < parsed[i - 1].point) {
      return `উঁচু নম্বরের গ্রেড পয়েন্ট নিচেরটার চেয়ে কম হতে পারে না: ${parsed[i].grade}`
    }
  }
  return ''
}

// bands = GET /api/grade-scale-এর উত্তর। সেভ হলে onDone() ডাকে।
export default function GradeScaleEditModal({ bands, onDone, onClose }) {
  const [rows, setRows] = useState(() =>
    bands.map((b) => ({
      minPercent: String(Number(b.fromPercent)),
      grade: b.grade,
      gradePoint: String(Number(b.gradePoint)),
    })),
  )
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  function update(index, field, value) {
    setRows((current) => current.map((row, i) => (i === index ? { ...row, [field]: value } : row)))
  }

  function addRow() {
    setRows((current) => [...current, { minPercent: '', grade: '', gradePoint: '' }])
  }

  function removeRow(index) {
    setRows((current) => current.filter((_, i) => i !== index))
  }

  async function submit(event) {
    event.preventDefault()
    const problem = validate(rows)
    if (problem) {
      setError(problem)
      return
    }
    setBusy(true)
    setError('')
    try {
      await api('/api/grade-scale', {
        method: 'PUT',
        body: {
          bands: rows.map((row) => ({
            minPercent: Number(row.minPercent),
            grade: row.grade.trim(),
            gradePoint: Number(row.gradePoint),
          })),
        },
      })
      onDone()
    } catch (err) {
      setError(err.message)
      setBusy(false)
    }
  }

  return (
    <Modal title="গ্রেড স্কেল বদলান" onClose={onClose}>
      <form onSubmit={submit} className="space-y-4">
        <Alert type="info">
          প্রতিটা গ্রেড "কত শতাংশ থেকে" শুরু। যেমন A+ ৮০ থেকে, A ৭০ থেকে, তাহলে ৭৯.৯৯% পড়বে A-তে। আগে প্রকাশ হওয়া
          ফল এতে বদলাবে না, শুধু পরের পরীক্ষাগুলোতে খাটবে।
        </Alert>
        {error && <Alert>{error}</Alert>}

        <div className="grid grid-cols-[1fr_1fr_1fr_auto] gap-2 text-xs font-medium text-gray-500">
          <span>শুরু (%)</span>
          <span>গ্রেড</span>
          <span>গ্রেড পয়েন্ট</span>
          <span />
        </div>
        {rows.map((row, index) => (
          // সারির ক্রম বদলায় না, তাই index দিয়ে key দেওয়া নিরাপদ
          // // eslint-disable-next-line react/no-array-index-key
          <div key={index} className="grid grid-cols-[1fr_1fr_1fr_auto] items-center gap-2">
            <input
              className={INPUT}
              type="number"
              min="0"
              max="100"
              step="0.01"
              value={row.minPercent}
              onChange={(event) => update(index, 'minPercent', event.target.value)}
              aria-label="শুরুর শতাংশ"
            />
            <input
              className={INPUT}
              type="text"
              maxLength={10}
              value={row.grade}
              onChange={(event) => update(index, 'grade', event.target.value)}
              aria-label="গ্রেড"
            />
            <input
              className={INPUT}
              type="number"
              min="0"
              max="10"
              step="0.01"
              value={row.gradePoint}
              onChange={(event) => update(index, 'gradePoint', event.target.value)}
              aria-label="গ্রেড পয়েন্ট"
            />
            <Button variant="ghost" size="sm" onClick={() => removeRow(index)}>
              ✕
            </Button>
          </div>
        ))}

        <Button variant="secondary" size="sm" onClick={addRow}>
          + গ্রেড যোগ করুন
        </Button>

        <FormActions busy={busy} submitLabel="সংরক্ষণ করুন" onCancel={onClose} />
      </form>
    </Modal>
  )
}