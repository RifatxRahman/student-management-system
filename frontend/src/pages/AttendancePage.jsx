import { useState } from 'react'
import { api } from '../api/client.js'
import { Alert, Badge, Button, Card, EmptyState, LoadState, PageHeader, SelectField, TextField } from '../components/ui.jsx'
import { useQuery } from '../hooks/useQuery.js'
import { ATTENDANCE_STATUSES, todayISO } from '../utils/format.js'

const MIN_REASON = 5

// বাছা বোতামের রং (Tailwind-এর ক্লাস পুরোটা লেখা থাকতে হয়, তাই আলাদা করে লেখা)
const ACTIVE_STYLES = {
  PRESENT: 'bg-green-600 text-white',
  ABSENT: 'bg-red-600 text-white',
  LATE: 'bg-amber-500 text-white',
  LEAVE: 'bg-indigo-600 text-white',
}
const IDLE_STYLE = 'bg-white text-gray-600 ring-1 ring-gray-300 hover:bg-gray-50'

function clean(text) {
  return (text ?? '').trim()
}

function StatusButtons({ value, disabled, onChange }) {
  return (
    <div className="flex flex-wrap gap-1.5" role="group" aria-label="হাজিরার অবস্থা">
      {ATTENDANCE_STATUSES.map((status) => (
        <button
          key={status.value}
          type="button"
          disabled={disabled}
          aria-pressed={value === status.value}
          onClick={() => onChange(status.value)}
          className={`rounded-lg px-3 py-1.5 text-xs font-medium transition disabled:cursor-not-allowed disabled:opacity-60 ${
            value === status.value ? ACTIVE_STYLES[status.value] : IDLE_STYLE
          }`}
        >
          {status.label}
        </button>
      ))}
    </div>
  )
}

// খাতার ভেতরের অংশ। খাতা (তথ্য) বদলালে মা-পাতা key বদলে একে নতুন করে বানায়।
function SheetEditor({ sheet, onSaved }) {
  const rows = sheet.rows
  const [draft, setDraft] = useState(() =>
    Object.fromEntries(rows.map((row) => [row.enrollmentId, { status: row.status ?? '', remark: row.remark ?? '' }])),
  )
  const [reason, setReason] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  const locked = !sheet.editable

  // গোনা: কে কোন অবস্থায়, কী বদলেছে
  const counts = { PRESENT: 0, ABSENT: 0, LATE: 0, LEAVE: 0 }
  const changedRows = []
  let unmarked = 0
  let corrections = 0
  for (const row of rows) {
    const current = draft[row.enrollmentId]
    if (!current.status) {
      unmarked += 1
      continue
    }
    counts[current.status] += 1
    if (!row.status) {
      changedRows.push(row) // নতুন হাজিরা
    } else if (current.status !== row.status || clean(current.remark) !== clean(row.remark)) {
      changedRows.push(row) // আগের হাজিরা বদলাচ্ছে (সংশোধন)
      corrections += 1
    }
  }

  const needsReason = corrections > 0
  const reasonOk = clean(reason).length >= MIN_REASON
  const canSave = !locked && changedRows.length > 0 && unmarked === 0 && (!needsReason || reasonOk)

  function setStatus(enrollmentId, status) {
    setDraft((current) => ({
      ...current,
      [enrollmentId]: { status, remark: status === 'PRESENT' ? '' : current[enrollmentId].remark },
    }))
  }

  function setRemark(enrollmentId, remark) {
    setDraft((current) => ({ ...current, [enrollmentId]: { ...current[enrollmentId], remark } }))
  }

  function markRestPresent() {
    setDraft((current) =>
      Object.fromEntries(
        Object.entries(current).map(([id, value]) => [id, value.status ? value : { status: 'PRESENT', remark: '' }]),
      ),
    )
  }

  async function save() {
    setBusy(true)
    setError('')
    try {
      const result = await api('/api/attendance/mark', {
        method: 'POST',
        body: {
          sectionId: sheet.sectionId,
          date: sheet.date,
          entries: changedRows.map((row) => ({
            enrollmentId: row.enrollmentId,
            status: draft[row.enrollmentId].status,
            remark: clean(draft[row.enrollmentId].remark) || null,
          })),
          reason: needsReason ? clean(reason) : null,
        },
      })
      setReason('')
      onSaved(result)
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="space-y-4">
      {locked && <Alert type="info">{sheet.lockReason}</Alert>}

      {rows.length === 0 ? (
        <Card>
          <EmptyState>এই তারিখে এই সেকশনে কোনো শিক্ষার্থী নেই।</EmptyState>
        </Card>
      ) : (
        <>
          <Card>
            <div className="flex flex-wrap items-center justify-between gap-3 border-b border-gray-100 px-4 py-3">
              <div className="flex flex-wrap gap-2">
                <Badge tone="green">উপস্থিত {counts.PRESENT}</Badge>
                <Badge tone="red">অনুপস্থিত {counts.ABSENT}</Badge>
                <Badge tone="amber">দেরি {counts.LATE}</Badge>
                <Badge tone="indigo">ছুটি {counts.LEAVE}</Badge>
                {unmarked > 0 && <Badge>বাকি {unmarked}</Badge>}
              </div>
              {!locked && unmarked > 0 && (
                <Button variant="secondary" size="sm" onClick={markRestPresent}>
                  বাকি সবাইকে উপস্থিত করুন
                </Button>
              )}
            </div>

            <ul className="divide-y divide-gray-100">
              {rows.map((row) => {
                const current = draft[row.enrollmentId]
                return (
                  <li key={row.enrollmentId} className="space-y-2 px-4 py-3">
                    <div className="flex flex-wrap items-center justify-between gap-3">
                      <div className="flex items-center gap-3">
                        <span className="flex h-8 w-8 items-center justify-center rounded-full bg-gray-100 text-sm font-medium text-gray-700">
                          {row.rollNumber}
                        </span>
                        <div>
                          <p className="text-sm font-medium text-gray-900">{row.studentName}</p>
                          <p className="font-mono text-xs text-gray-500">{row.studentCode}</p>
                        </div>
                      </div>
                      <StatusButtons
                        value={current.status}
                        disabled={locked}
                        onChange={(status) => setStatus(row.enrollmentId, status)}
                      />
                    </div>
                    {current.status && current.status !== 'PRESENT' && (
                      <input
                        type="text"
                        maxLength={255}
                        disabled={locked}
                        value={current.remark}
                        onChange={(event) => setRemark(row.enrollmentId, event.target.value)}
                        placeholder="মন্তব্য (ঐচ্ছিক)"
                        aria-label={`${row.studentName}-এর মন্তব্য`}
                        className="block w-full rounded-lg border border-gray-300 px-3 py-1.5 text-sm outline-none focus:ring-2 focus:ring-indigo-500 disabled:bg-gray-50"
                      />
                    )}
                  </li>
                )
              })}
            </ul>
          </Card>

          {!locked && (
            <Card className="space-y-3 p-4">
              {needsReason && (
                <>
                  <Alert type="info">
                    আগে নেওয়া {corrections} জনের হাজিরা বদলাচ্ছে। কারণ লেখা বাধ্যতামূলক। কে, কী থেকে কী ও কেন, সব
                    অডিট লগে লেখা থাকবে।
                  </Alert>
                  <TextField
                    label="সংশোধনের কারণ"
                    hint={`কমপক্ষে ${MIN_REASON} অক্ষর`}
                    maxLength={500}
                    value={reason}
                    onChange={(event) => setReason(event.target.value)}
                  />
                </>
              )}
              {error && <Alert>{error}</Alert>}
              {unmarked > 0 && <p className="text-xs text-gray-500">সবার হাজিরা বাছা বাকি: {unmarked} জন।</p>}
              {counts.ABSENT > 0 && (
                <p className="text-xs text-gray-500">
                  অনুপস্থিত শিক্ষার্থীর প্রধান অভিভাবকের জন্য একটা বার্তা জমা হবে (Admin পরে পাঠাবেন)।
                </p>
              )}
              <div className="flex justify-end">
                <Button disabled={!canSave || busy} onClick={save}>
                  {busy ? 'অপেক্ষা করুন...' : sheet.alreadyMarked ? 'সংশোধন সংরক্ষণ করুন' : 'হাজিরা সংরক্ষণ করুন'}
                </Button>
              </div>
            </Card>
          )}
        </>
      )}
    </div>
  )
}

// খাতার তথ্য বদলালে (সংরক্ষণের পর নতুন তথ্য এলে) এই চাবি বদলায়, ফলে SheetEditor নতুন করে শুরু হয়
function sheetKey(sheet) {
  const signature = sheet.rows.map((r) => `${r.enrollmentId}:${r.status ?? ''}:${r.remark ?? ''}`).join('|')
  return `${sheet.sectionId}-${sheet.date}-${signature}`
}

export default function AttendancePage() {
  const [today] = useState(todayISO)
  const [date, setDate] = useState(todayISO)
  const [sectionId, setSectionId] = useState('')
  const [message, setMessage] = useState('')

  const sections = useQuery(`/api/attendance/my-sections?date=${date}`)
  const options = sections.data ?? []
  // একটাই সেকশন থাকলে নিজে থেকেই সেটা বাছা হয়
  const effectiveSectionId = sectionId || (options.length === 1 ? String(options[0].sectionId) : '')

  const sheet = useQuery(
    effectiveSectionId && date ? `/api/attendance/sheet?sectionId=${effectiveSectionId}&date=${date}` : null,
  )

  const sectionOptions = [
    { value: '', label: 'বাছুন' },
    ...options.map((s) => ({ value: String(s.sectionId), label: `${s.className} / ${s.sectionName}` })),
  ]

  function changeDate(value) {
    setDate(value)
    setMessage('')
  }

  function changeSection(value) {
    setSectionId(value)
    setMessage('')
  }

  function saved(result) {
    setMessage(`সংরক্ষণ হয়েছে: নতুন ${result.created}, সংশোধন ${result.changed}, অপরিবর্তিত ${result.unchanged}`)
    sheet.reload()
  }

  return (
    <>
      <PageHeader title="হাজিরা" subtitle="তারিখ ও সেকশন বেছে প্রতিজনের হাজিরা দিন" />

      <Card className="mb-4 p-4">
        <div className="grid gap-3 sm:grid-cols-2">
          <div>
            <TextField
              label="তারিখ"
              type="date"
              max={today}
              value={date}
              onChange={(event) => changeDate(event.target.value)}
            />
            <div className="mt-2">
              <Button variant="ghost" size="sm" onClick={() => changeDate(today)}>
                আজকের তারিখ
              </Button>
            </div>
          </div>
          <SelectField
            label="ক্লাস / সেকশন"
            options={sectionOptions}
            value={effectiveSectionId}
            onChange={(event) => changeSection(event.target.value)}
          />
        </div>
      </Card>

      {message && (
        <div className="mb-4">
          <Alert type="success">{message}</Alert>
        </div>
      )}

      <LoadState loading={sections.loading} error={sections.error}>
        {options.length === 0 ? (
          <Card>
            <EmptyState>
              এই তারিখে আপনার জন্য কোনো সেকশন নেই। শিক্ষক হলে Admin-কে বলুন আপনাকে কোনো সেকশনে বরাদ্দ দিতে।
            </EmptyState>
          </Card>
        ) : (
          !effectiveSectionId && (
            <Card>
              <EmptyState>হাজিরা নিতে ওপর থেকে একটা সেকশন বাছুন।</EmptyState>
            </Card>
          )
        )}
      </LoadState>

      {effectiveSectionId && (
        <LoadState loading={sheet.loading} error={sheet.error}>
          {sheet.data && <SheetEditor key={sheetKey(sheet.data)} sheet={sheet.data} onSaved={saved} />}
        </LoadState>
      )}
    </>
  )
}