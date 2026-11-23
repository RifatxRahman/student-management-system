import { useState } from 'react'
import { api } from '../api/client.js'
import TeacherPhoto from '../components/TeacherPhoto.jsx'
import { Alert, Badge, Button, Card, EmptyState, LoadState, PageHeader, Table, Td } from '../components/ui.jsx'
import { useQuery } from '../hooks/useQuery.js'

const STATUSES = [
  { value: 'PRESENT', label: 'উপস্থিত', color: 'bg-emerald-100 text-emerald-800 border-emerald-300' },
  { value: 'LATE', label: 'বিলম্বে', color: 'bg-amber-100 text-amber-800 border-amber-300' },
  { value: 'ABSENT', label: 'অনুপস্থিত', color: 'bg-rose-100 text-rose-800 border-rose-300' },
  { value: 'LEAVE', label: 'ছুটি', color: 'bg-sky-100 text-sky-800 border-sky-300' },
  { value: 'HALF_DAY', label: 'অর্ধ-দিবস', color: 'bg-purple-100 text-purple-800 border-purple-300' },
]

export default function TeacherAttendancePage() {
  const [date, setDate] = useState(() => new Date().toISOString().slice(0, 10))
  const sheet = useQuery(`/api/teacher-attendance/sheet?date=${date}`)
  const [rowPatches, setRowPatches] = useState({ date: '', byId: {} })
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  const patches = rowPatches.date === date ? rowPatches.byId : {}
  const rows = (sheet.data?.rows ?? []).map((r) => ({
    teacherId: r.teacherId,
    employeeId: r.employeeId,
    teacherName: r.teacherName,
    designation: r.designation,
    status: r.status || (r.hasApprovedLeave ? 'LEAVE' : 'PRESENT'),
    inTime: r.inTime || '',
    outTime: r.outTime || '',
    remark: r.remark || '',
    hasApprovedLeave: r.hasApprovedLeave,
    leaveType: r.leaveType,
    ...patches[r.teacherId],
  }))

  function updateRow(teacherId, patch) {
    setRowPatches((prev) => {
      const byId = prev.date === date ? prev.byId : {}
      return {
        date,
        byId: {
          ...byId,
          [teacherId]: { ...byId[teacherId], ...patch },
        },
      }
    })
  }

  function markAllPresent() {
    setRowPatches((prev) => {
      const byId = prev.date === date ? { ...prev.byId } : {}
      for (const r of rows) {
        if (!r.hasApprovedLeave) byId[r.teacherId] = { ...byId[r.teacherId], status: 'PRESENT' }
      }
      return { date, byId }
    })
  }

  async function save() {
    setSaving(true)
    setMessage('')
    setError('')
    try {
      const body = {
        date,
        entries: rows.map((r) => ({
          teacherId: r.teacherId,
          status: r.status,
          inTime: r.inTime ? r.inTime : null,
          outTime: r.outTime ? r.outTime : null,
          remark: r.remark || null,
        })),
      }
      await api('/api/teacher-attendance/sheet', { method: 'POST', body })
      setMessage('শিক্ষক হাজিরা সফলভাবে সংরক্ষিত হয়েছে।')
      setRowPatches({ date, byId: {} })
      sheet.reload()
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  const counts = rows.reduce(
    (acc, r) => {
      if (r.status) acc[r.status] = (acc[r.status] || 0) + 1
      return acc
    },
    { PRESENT: 0, LATE: 0, ABSENT: 0, LEAVE: 0, HALF_DAY: 0 }
  )

  return (
    <>
      <PageHeader
        title="শিক্ষক ও স্টাফ হাজিরা"
        subtitle="দৈনিক শিক্ষক উপস্থিতি, বিলম্ব ও ছুটির খতিয়ান"
        action={
          <div className="flex items-center gap-3">
            <input
              type="date"
              value={date}
              onChange={(e) => setDate(e.target.value)}
              className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm font-medium shadow-sm focus:border-indigo-500 focus:outline-none"
            />
            <Button variant="secondary" size="sm" onClick={markAllPresent}>
              সবাইকে উপস্থিত দাগান
            </Button>
            <Button size="sm" disabled={saving || rows.length === 0} onClick={save}>
              {saving ? 'সংরক্ষণ হচ্ছে...' : 'হাজিরা সংরক্ষণ করুন'}
            </Button>
          </div>
        }
      />

      {message && (
        <div className="mb-4">
          <Alert tone="green">{message}</Alert>
        </div>
      )}
      {error && (
        <div className="mb-4">
          <Alert>{error}</Alert>
        </div>
      )}

      {/* সারসংক্ষেপ কার্ড */}
      <div className="mb-6 grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-6">
        <div className="rounded-xl border border-slate-200 bg-white p-3 text-center shadow-sm">
          <p className="text-xs text-slate-500 font-medium">মোট শিক্ষক</p>
          <p className="text-xl font-bold text-slate-800">{rows.length}</p>
        </div>
        <div className="rounded-xl border border-emerald-200 bg-emerald-50/50 p-3 text-center shadow-sm">
          <p className="text-xs text-emerald-700 font-medium">উপস্থিত</p>
          <p className="text-xl font-bold text-emerald-800">{counts.PRESENT}</p>
        </div>
        <div className="rounded-xl border border-amber-200 bg-amber-50/50 p-3 text-center shadow-sm">
          <p className="text-xs text-amber-700 font-medium">বিলম্বে</p>
          <p className="text-xl font-bold text-amber-800">{counts.LATE}</p>
        </div>
        <div className="rounded-xl border border-rose-200 bg-rose-50/50 p-3 text-center shadow-sm">
          <p className="text-xs text-rose-700 font-medium">অনুপস্থিত</p>
          <p className="text-xl font-bold text-rose-800">{counts.ABSENT}</p>
        </div>
        <div className="rounded-xl border border-sky-200 bg-sky-50/50 p-3 text-center shadow-sm">
          <p className="text-xs text-sky-700 font-medium">ছুটিতে</p>
          <p className="text-xl font-bold text-sky-800">{counts.LEAVE}</p>
        </div>
        <div className="rounded-xl border border-purple-200 bg-purple-50/50 p-3 text-center shadow-sm">
          <p className="text-xs text-purple-700 font-medium">অর্ধ-দিবস</p>
          <p className="text-xl font-bold text-purple-800">{counts.HALF_DAY}</p>
        </div>
      </div>

      <Card>
        <LoadState loading={sheet.loading} error={sheet.error}>
          {rows.length === 0 ? (
            <EmptyState>কোনো শিক্ষক তালিকা পাওয়া যায়নি।</EmptyState>
          ) : (
            <Table headers={['শিক্ষক ও আইডি', 'পদবি', 'উপস্থিতি স্ট্যাটাস', 'প্রবেশ / প্রস্থান', 'মন্তব্য']}>
              {rows.map((r) => (
                <tr key={r.teacherId} className={r.hasApprovedLeave ? 'bg-sky-50/30' : ''}>
                  <Td>
                    <div className="flex items-center gap-3">
                      <TeacherPhoto teacherId={r.teacherId} name={r.teacherName} className="h-9 w-9 text-xs" />
                      <div>
                        <p className="font-medium text-gray-900">{r.teacherName}</p>
                        <p className="font-mono text-xs text-indigo-600">{r.employeeId || 'আইডি নেই'}</p>
                      </div>
                    </div>
                  </Td>
                  <Td className="text-sm text-gray-600">{r.designation || '—'}</Td>
                  <Td>
                    <div className="flex flex-wrap items-center gap-1.5">
                      {STATUSES.map((s) => {
                        const active = r.status === s.value
                        return (
                          <button
                            key={s.value}
                            type="button"
                            onClick={() => updateRow(r.teacherId, { status: s.value })}
                            className={`rounded-md px-2.5 py-1 text-xs font-semibold border transition ${
                              active
                                ? `${s.color} shadow-sm ring-1 ring-black/10`
                                : 'border-gray-200 bg-white text-gray-600 hover:bg-gray-50'
                            }`}
                          >
                            {s.label}
                          </button>
                        )
                      })}
                      {r.hasApprovedLeave && (
                        <span className="ml-1">
                          <Badge tone="blue">অনুমোদিত ছুটি ({r.leaveType})</Badge>
                        </span>
                      )}
                    </div>
                  </Td>
                  <Td>
                    <div className="flex items-center gap-1.5">
                      <input
                        type="time"
                        value={r.inTime}
                        onChange={(e) => updateRow(r.teacherId, { inTime: e.target.value })}
                        className="w-24 rounded border border-gray-300 px-1.5 py-1 text-xs focus:border-indigo-500 focus:outline-none"
                      />
                      <span className="text-gray-400">-</span>
                      <input
                        type="time"
                        value={r.outTime}
                        onChange={(e) => updateRow(r.teacherId, { outTime: e.target.value })}
                        className="w-24 rounded border border-gray-300 px-1.5 py-1 text-xs focus:border-indigo-500 focus:outline-none"
                      />
                    </div>
                  </Td>
                  <Td>
                    <input
                      type="text"
                      placeholder="মন্তব্য..."
                      value={r.remark}
                      onChange={(e) => updateRow(r.teacherId, { remark: e.target.value })}
                      className="w-full rounded border border-gray-300 px-2 py-1 text-xs focus:border-indigo-500 focus:outline-none"
                    />
                  </Td>
                </tr>
              ))}
            </Table>
          )}
        </LoadState>
      </Card>
    </>
  )
}
