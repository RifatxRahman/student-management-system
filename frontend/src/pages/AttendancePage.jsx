import { useState } from 'react'
import { api } from '../api/client.js'
import { Alert, Badge, Button, Card, EmptyState, LoadState, PageHeader, SelectField, TextField } from '../components/ui.jsx'
import { useQuery } from '../hooks/useQuery.js'
import { ATTENDANCE_STATUSES, todayISO } from '../utils/format.js'

const MIN_REASON = 5

// বাছা বোতামের রং (Tailwind-এর ক্লাস পুরোটা লেখা থাকতে হয়, তাই আলাদা করে লেখা)
const ACTIVE_STYLES = {
  PRESENT: 'bg-[#168754] text-white font-semibold shadow-xs',
  ABSENT: 'bg-[#C8372D] text-white font-semibold shadow-xs',
  LATE: 'bg-[#B36B18] text-white font-semibold shadow-xs',
  LEAVE: 'bg-[#0E3B2E] dark:bg-[#1D4B3C] text-white font-semibold shadow-xs border border-emerald-400/30',
}
const IDLE_STYLE =
  'bg-[var(--surface-subtle)] text-[var(--ink-secondary)] border border-[var(--border-hairline)] hover:bg-[var(--surface-hover)] hover:text-[var(--ink-primary)] font-semibold'

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

  async function finalize() {
    if (!window.confirm('আপনি কি নিশ্চিত যে এই দিনের হাজিরা ফাইনাল ও লক করতে চান? একবার লক করলে সাধারণ শিক্ষকরা আর পরিবর্তন করতে পারবেন না।')) {
      return
    }
    setBusy(true)
    setError('')
    try {
      await api(`/api/attendance/finalize?sectionId=${sheet.sectionId}&date=${sheet.date}`, {
        method: 'POST',
      })
      onSaved({ finalized: true })
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
            <div className="flex flex-wrap items-center justify-between gap-3 border-b border-[var(--border-hairline)] px-4 py-3">
              <div className="flex flex-wrap gap-2">
                <Badge tone="green">উপস্থিত {counts.PRESENT}</Badge>
                <Badge tone="red">অনুপস্থিত {counts.ABSENT}</Badge>
                <Badge tone="amber">দেরি {counts.LATE}</Badge>
                <Badge tone="brand">ছুটি {counts.LEAVE}</Badge>
                {sheet.finalized && <Badge tone="red">লক করা (Finalized)</Badge>}
                {unmarked > 0 && <Badge>বাকি {unmarked}</Badge>}
              </div>
              {!locked && unmarked > 0 && (
                <Button variant="secondary" size="sm" onClick={markRestPresent}>
                  বাকি সবাইকে উপস্থিত করুন
                </Button>
              )}
            </div>

            <ul className="divide-y divide-[var(--border-hairline)]">
              {rows.map((row) => {
                const current = draft[row.enrollmentId]
                return (
                  <li key={row.enrollmentId} className="space-y-2 px-4 py-3">
                    <div className="flex flex-wrap items-center justify-between gap-3">
                      <div className="flex items-center gap-3">
                        <span className="flex h-8 w-8 items-center justify-center rounded-full bg-[var(--surface-subtle)] border border-[var(--border-hairline)] text-sm font-bold text-[var(--ink-primary)] font-['Anek_Bangla',sans-serif] tabular-nums">
                          {row.rollNumber}
                        </span>
                        <div>
                          <p className="text-sm font-semibold text-[var(--ink-primary)]">{row.studentName}</p>
                          <p className="font-mono text-xs text-[var(--ink-secondary)]">{row.studentCode}</p>
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
                        className="block w-full min-h-[38px] rounded-[10px] border border-[var(--border-hairline)] px-3 py-1.5 text-sm bg-[var(--surface-subtle)] text-[var(--ink-primary)] placeholder-[var(--ink-tertiary)] outline-none focus:border-[#E8A317] focus:ring-2 focus:ring-[#E8A317]/25 disabled:opacity-50"
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
              {unmarked > 0 && <p className="text-xs text-[var(--ink-secondary)] font-medium">সবার হাজিরা বাছা বাকি: {unmarked} জন।</p>}
              {counts.ABSENT > 0 && (
                <p className="text-xs text-[var(--ink-secondary)]">
                  অনুপস্থিত শিক্ষার্থীর প্রধান অভিভাবকের জন্য একটা বার্তা জমা হবে (Admin পরে পাঠাবেন)।
                </p>
              )}
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  {sheet.alreadyMarked && !sheet.finalized && (
                    <Button variant="danger" size="sm" disabled={busy} onClick={finalize}>
                      ফাইনাল ও লক করুন
                    </Button>
                  )}
                </div>
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
    if (result.finalized) {
      setMessage('হাজিরা ফাইনাল ও লক করা হয়েছে।')
    } else {
      setMessage(`সংরক্ষণ হয়েছে: নতুন ${result.created}, সংশোধন ${result.changed}, অপরিবর্তিত ${result.unchanged}`)
    }
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
            <div className="mt-2.5">
              <button
                type="button"
                onClick={() => changeDate(today)}
                className="inline-flex items-center gap-1.5 text-xs font-semibold text-[#E8A317] dark:text-[#F0B232] hover:underline focus-ring-apple rounded px-1 py-0.5"
              >
                <span aria-hidden="true">📅</span> আজকের তারিখ নির্বাচন করুন
              </button>
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