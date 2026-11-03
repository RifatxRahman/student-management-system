import { useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router'
import { api } from '../api/client.js'
import { Alert, Badge, Button, Card, EmptyState, LoadState, PageHeader, SelectField, TextField } from '../components/ui.jsx'
import { useQuery } from '../hooks/useQuery.js'
import { examStatusInfo, plain } from '../utils/format.js'

const MIN_REASON = 5
const NUMBER_FORMAT = /^\d+(\.\d{1,2})?$/

function clean(text) {
  return (text ?? '').trim()
}

// খাতার ভেতরের অংশ। খাতার তথ্য বদলালে মা-পাতা key বদলে একে নতুন করে বানায়।
function MarksEditor({ sheet, onSaved }) {
  const rows = sheet.rows
  const full = Number(sheet.fullMarks)
  const pass = Number(sheet.passMarks)

  const [draft, setDraft] = useState(() =>
    Object.fromEntries(
      rows.map((row) => [
        row.enrollmentId,
        { marks: row.marks === null || row.marks === undefined ? '' : String(Number(row.marks)), absent: row.absent },
      ]),
    ),
  )
  const [reason, setReason] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  const locked = !sheet.editable

  // প্রতিটা সারির অবস্থা: কী বদলেছে, কোনো সমস্যা আছে কি না
  const problems = {}
  const changedRows = []
  let entered = 0
  for (const row of rows) {
    const current = draft[row.enrollmentId]
    const text = clean(current.marks)

    if (current.absent) {
      entered += 1
      if (!(row.entered && row.absent)) changedRows.push(row)
      continue
    }
    if (text === '') {
      if (row.entered) problems[row.enrollmentId] = 'আগে দেওয়া নম্বর মুছে ফেলা যায় না। ঠিক নম্বর লিখুন বা অনুপস্থিত দিন'
      continue
    }
    const value = Number(text)
    if (!NUMBER_FORMAT.test(text)) {
      problems[row.enrollmentId] = 'নম্বর সংখ্যা হতে হবে (দশমিকের পরে সর্বোচ্চ ২ ঘর)'
    } else if (value > full) {
      problems[row.enrollmentId] = `ফুল মার্কস (${plain(full)})-এর বেশি হতে পারে না`
    } else {
      entered += 1
      if (!(row.entered && !row.absent && Number(row.marks) === value)) changedRows.push(row)
    }
  }

  const problemCount = Object.keys(problems).length
  const needsReason = sheet.requiresReason && changedRows.length > 0
  const reasonOk = clean(reason).length >= MIN_REASON
  const canSave = !locked && changedRows.length > 0 && problemCount === 0 && (!needsReason || reasonOk)

  function setMarks(enrollmentId, marks) {
    setDraft((current) => ({ ...current, [enrollmentId]: { ...current[enrollmentId], marks } }))
  }

  function setAbsent(enrollmentId, absent) {
    setDraft((current) => ({
      ...current,
      [enrollmentId]: { marks: absent ? '' : current[enrollmentId].marks, absent },
    }))
  }

  async function save() {
    setBusy(true)
    setError('')
    try {
      const result = await api('/api/marks/save', {
        method: 'POST',
        body: {
          examId: sheet.examId,
          sectionId: sheet.sectionId,
          subjectId: sheet.subjectId,
          entries: changedRows.map((row) => ({
            enrollmentId: row.enrollmentId,
            marks: draft[row.enrollmentId].absent ? null : Number(clean(draft[row.enrollmentId].marks)),
            absent: draft[row.enrollmentId].absent,
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
      {sheet.requiresReason && !locked && (
        <Alert type="info">
          ফল প্রকাশিত হয়ে গেছে। এখন যেকোনো বদলে কারণ লিখতে হবে। প্রতিটা বদল অডিট লগে থাকবে আর ফল ও মেধাক্রম নিজে নতুন করে
          হিসাব হবে।
        </Alert>
      )}

      {rows.length === 0 ? (
        <Card>
          <EmptyState>এই সেকশনে কোনো চলমান শিক্ষার্থী নেই।</EmptyState>
        </Card>
      ) : (
        <>
          <Card>
            <div className="flex flex-wrap items-center justify-between gap-2 border-b border-gray-100 px-4 py-3 text-sm text-gray-700">
              <span>
                {sheet.subjectName} · ফুল মার্কস <strong>{plain(sheet.fullMarks)}</strong> · পাস মার্কস{' '}
                <strong>{plain(sheet.passMarks)}</strong>
                {sheet.optional ? ' · ঐচ্ছিক বিষয়' : ''}
              </span>
              <Badge tone={entered === rows.length ? 'green' : 'amber'}>
                {entered}/{rows.length} জনের এন্ট্রি হয়েছে
              </Badge>
            </div>

            <ul className="divide-y divide-gray-100">
              {rows.map((row) => {
                const current = draft[row.enrollmentId]
                const text = clean(current.marks)
                const problem = problems[row.enrollmentId]
                const willFail = !problem && (current.absent || (text !== '' && Number(text) < pass))
                return (
                  <li key={row.enrollmentId} className="px-4 py-3">
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
                      <div className="flex items-center gap-3">
                        <input
                          type="text"
                          inputMode="decimal"
                          disabled={locked || current.absent}
                          value={current.marks}
                          onChange={(event) => setMarks(row.enrollmentId, event.target.value)}
                          placeholder="নম্বর"
                          aria-label={`${row.studentName}-এর নম্বর`}
                          className={`w-24 rounded-lg border px-3 py-1.5 text-sm outline-none focus:ring-2 focus:ring-indigo-500 disabled:bg-gray-50 ${
                            problem ? 'border-red-400' : 'border-gray-300'
                          }`}
                        />
                        <label className="flex items-center gap-1.5 text-sm text-gray-700">
                          <input
                            type="checkbox"
                            disabled={locked}
                            checked={current.absent}
                            onChange={(event) => setAbsent(row.enrollmentId, event.target.checked)}
                          />
                          অনুপস্থিত
                        </label>
                      </div>
                    </div>
                    {problem && <p className="mt-1.5 text-xs text-red-600">{problem}</p>}
                    {willFail && (
                      <p className="mt-1.5 text-xs text-amber-700">
                        {current.absent ? 'অনুপস্থিত মানে এই বিষয়ে ফেল' : `পাস মার্কসের (${plain(sheet.passMarks)}) নিচে, ফেল ধরা হবে`}
                      </p>
                    )}
                  </li>
                )
              })}
            </ul>
          </Card>

          {!locked && (
            <Card className="space-y-3 p-4">
              {needsReason && (
                <TextField
                  label="বদলের কারণ"
                  hint={`কমপক্ষে ${MIN_REASON} অক্ষর`}
                  maxLength={500}
                  value={reason}
                  onChange={(event) => setReason(event.target.value)}
                />
              )}
              {error && <Alert>{error}</Alert>}
              {problemCount > 0 && (
                <p className="text-xs text-red-600">{problemCount}টা সারিতে সমস্যা আছে, ঠিক না করলে সেভ হবে না।</p>
              )}
              <div className="flex items-center justify-between gap-3">
                <p className="text-xs text-gray-500">
                  {changedRows.length > 0 ? `${changedRows.length} জনের নতুন বা বদলানো নম্বর সেভ হবে।` : 'সেভ করার মতো কোনো বদল নেই।'}
                </p>
                <Button disabled={!canSave || busy} onClick={save}>
                  {busy ? 'অপেক্ষা করুন...' : 'মার্কস সংরক্ষণ করুন'}
                </Button>
              </div>
            </Card>
          )}
        </>
      )}
    </div>
  )
}

// খাতার তথ্য বদলালে (সেভের পর নতুন তথ্য এলে) এই চাবি বদলায়, ফলে MarksEditor নতুন করে শুরু হয়
function sheetKey(sheet) {
  const signature = sheet.rows
    .map((r) => `${r.enrollmentId}:${r.entered ? 1 : 0}:${r.absent ? 1 : 0}:${r.marks ?? ''}`)
    .join('|')
  return `${sheet.examId}-${sheet.sectionId}-${sheet.subjectId}-${signature}`
}

export default function MarksEntryPage() {
  const { examId } = useParams()
  const [params, setParams] = useSearchParams()
  const sectionId = params.get('sectionId') ?? ''
  const subjectId = params.get('subjectId') ?? ''
  const [message, setMessage] = useState('')

  const exam = useQuery(`/api/exams/${examId}`)
  const targets = useQuery(`/api/marks/targets?examId=${examId}`)
  const sheet = useQuery(
    sectionId && subjectId ? `/api/marks/sheet?examId=${examId}&sectionId=${sectionId}&subjectId=${subjectId}` : null,
  )

  const options = [
    { value: '', label: 'বাছুন' },
    ...(targets.data ?? []).map((t) => ({
      value: `${t.sectionId}:${t.subjectId}`,
      label: `${t.className} / ${t.sectionName} · ${t.subjectName}`,
    })),
  ]

  function select(value) {
    setMessage('')
    if (!value) {
      setParams({})
      return
    }
    const [section, subject] = value.split(':')
    setParams({ sectionId: section, subjectId: subject })
  }

  function saved(result) {
    setMessage(`সংরক্ষণ হয়েছে: নতুন ${result.created}, বদল ${result.changed}, অপরিবর্তিত ${result.unchanged}`)
    sheet.reload()
  }

  return (
    <>
      <div className="mb-3">
        <Link to={`/exams/${examId}`} className="text-sm text-gray-500 hover:text-gray-700">
          ← পরীক্ষায় ফিরে যান
        </Link>
      </div>

      <PageHeader
        title="মার্কস এন্ট্রি"
        subtitle={
          exam.data
            ? `${exam.data.exam.name} · ${exam.data.exam.className} · ${examStatusInfo(exam.data.exam.status).label}`
            : 'পরীক্ষার মার্কস দিন'
        }
      />

      <Card className="mb-4 p-4">
        <SelectField
          label="সেকশন ও বিষয়"
          options={options}
          value={sectionId && subjectId ? `${sectionId}:${subjectId}` : ''}
          onChange={(event) => select(event.target.value)}
        />
      </Card>

      {message && (
        <div className="mb-4">
          <Alert type="success">{message}</Alert>
        </div>
      )}

      <LoadState loading={targets.loading} error={targets.error}>
        {targets.data?.length === 0 && (
          <Card>
            <EmptyState>মার্কস দেওয়ার মতো কোনো সেকশন-বিষয় আপনার জন্য নেই। শিক্ষক হলে Admin-কে বলুন বরাদ্দ দিতে।</EmptyState>
          </Card>
        )}
        {targets.data?.length > 0 && !(sectionId && subjectId) && (
          <Card>
            <EmptyState>মার্কস দিতে ওপর থেকে একটা সেকশন ও বিষয় বাছুন।</EmptyState>
          </Card>
        )}
      </LoadState>

      {sectionId && subjectId && (
        <LoadState loading={sheet.loading} error={sheet.error}>
          {sheet.data && <MarksEditor key={sheetKey(sheet.data)} sheet={sheet.data} onSaved={saved} />}
        </LoadState>
      )}
    </>
  )
}