import { useEffect, useState } from 'react'
import { api } from '../api/client.js'
import TeacherPhoto from '../components/TeacherPhoto.jsx'
import { Alert, Badge, Button, Card, EmptyState, LoadState, PageHeader, Table, Td } from '../components/ui.jsx'
import { useQuery } from '../hooks/useQuery.js'

const STATUSES = [
  { value: 'PRESENT', label: 'উপস্থিত', color: 'bg-emerald-100 text-emerald-800 border-emerald-300' },
  { value: 'LATE', label: 'বিলম্বে', color: 'bg-amber-100 text-amber-800 border-amber-300' },
  { value: 'ABSENT', label: 'অনুপস্থিত', color: 'bg-rose-100 text-rose-800 border-rose-300' },
  { value: 'LEAVE', label: 'ছুটি', color: 'bg-[#EAF2EF] text-[#0E3B2E] border-[#C7DDD4]' },
  { value: 'HALF_DAY', label: 'অর্ধ-দিবস', color: 'bg-[#FDF4E7] text-[#B36B18] border-[#F6DFC0]' },
]

export default function TeacherAttendancePage() {
  const [date, setDate] = useState(() => new Date().toISOString().slice(0, 10))
  const sheet = useQuery(`/api/teacher-attendance/sheet?date=${date}`)
  const [rows, setRows] = useState([])
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    if (sheet.data?.rows) {
      setRows(
        sheet.data.rows.map((r) => ({
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
        }))
      )
    }
  }, [sheet.data])

  function updateRow(teacherId, patch) {
    setRows((prev) =>
      prev.map((r) => (r.teacherId === teacherId ? { ...r, ...patch } : r))
    )
  }

  function markAllPresent() {
    setRows((prev) =>
      prev.map((r) =>
        r.hasApprovedLeave ? r : { ...r, status: 'PRESENT' }
      )
    )
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
              className="rounded-[10px] border border-[var(--border-hairline)] bg-[var(--surface-subtle)] px-3 py-1.5 text-sm font-medium text-[var(--ink-primary)] shadow-sm focus:border-[#E8A317] focus:ring-1 focus:ring-[#E8A317] focus:outline-none"
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
        <div className="rounded-[16px] border border-[var(--border-hairline)] bg-[var(--surface-subtle)] p-3 text-center shadow-xs">
          <p className="text-xs text-[var(--ink-secondary)] font-medium">মোট শিক্ষক</p>
          <p className="text-xl font-bold text-[var(--ink-primary)] font-['Anek_Bangla',sans-serif] tabular-nums">{rows.length}</p>
        </div>
        <div className="rounded-[16px] border border-[var(--semantic-success)]/30 bg-[var(--semantic-success-bg)] p-3 text-center shadow-xs">
          <p className="text-xs text-[var(--semantic-success)] font-medium">উপস্থিত</p>
          <p className="text-xl font-bold text-[var(--semantic-success)] font-['Anek_Bangla',sans-serif] tabular-nums">{counts.PRESENT}</p>
        </div>
        <div className="rounded-[16px] border border-[var(--semantic-warning)]/30 bg-[var(--semantic-warning-bg)] p-3 text-center shadow-xs">
          <p className="text-xs text-[var(--semantic-warning)] font-medium">বিলম্বে</p>
          <p className="text-xl font-bold text-[var(--semantic-warning)] font-['Anek_Bangla',sans-serif] tabular-nums">{counts.LATE}</p>
        </div>
        <div className="rounded-[16px] border border-[var(--semantic-danger)]/30 bg-[var(--semantic-danger-bg)] p-3 text-center shadow-xs">
          <p className="text-xs text-[var(--semantic-danger)] font-medium">অনুপস্থিত</p>
          <p className="text-xl font-bold text-[var(--semantic-danger)] font-['Anek_Bangla',sans-serif] tabular-nums">{counts.ABSENT}</p>
        </div>
        <div className="rounded-[16px] border border-[var(--border-hairline)] bg-[var(--surface-subtle)] p-3 text-center shadow-xs">
          <p className="text-xs text-[var(--ink-secondary)] font-medium">ছুটিতে</p>
          <p className="text-xl font-bold text-[var(--ink-primary)] font-['Anek_Bangla',sans-serif] tabular-nums">{counts.LEAVE}</p>
        </div>
        <div className="rounded-[16px] border border-[var(--border-hairline)] bg-[var(--surface-subtle)] p-3 text-center shadow-xs">
          <p className="text-xs text-[var(--ink-secondary)] font-medium">অর্ধ-দিবস</p>
          <p className="text-xl font-bold text-[var(--ink-primary)] font-['Anek_Bangla',sans-serif] tabular-nums">{counts.HALF_DAY}</p>
        </div>
      </div>

      <Card>
        <LoadState loading={sheet.loading} error={sheet.error}>
          {rows.length === 0 ? (
            <EmptyState>কোনো শিক্ষক তালিকা পাওয়া যায়নি।</EmptyState>
          ) : (
            <Table headers={['শিক্ষক ও আইডি', 'পদবি', 'উপস্থিতি স্ট্যাটাস', 'প্রবেশ / প্রস্থান', 'মন্তব্য']}>
              {rows.map((r) => (
                <tr key={r.teacherId} className={r.hasApprovedLeave ? 'bg-[#0E3B2E]/5' : ''}>
                  <Td>
                    <div className="flex items-center gap-3">
                      <TeacherPhoto teacherId={r.teacherId} name={r.teacherName} className="h-9 w-9 text-xs" />
                      <div>
                        <p className="font-semibold text-[var(--ink-primary)]">{r.teacherName}</p>
                        <p className="font-mono text-xs text-[var(--ink-secondary)]">{r.employeeId || 'আইডি নেই'}</p>
                      </div>
                    </div>
                  </Td>
                  <Td className="text-sm text-[var(--ink-secondary)]">{r.designation || '—'}</Td>
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
                                : 'border-[var(--border-hairline)] bg-[var(--surface-subtle)] text-[var(--ink-secondary)] hover:bg-[var(--surface-hover)] hover:text-[var(--ink-primary)]'
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
                        className="w-24 rounded-[8px] border border-[var(--border-hairline)] bg-[var(--surface-subtle)] text-[var(--ink-primary)] px-1.5 py-1 text-xs focus:border-[#E8A317] focus:ring-1 focus:ring-[#E8A317] focus:outline-none"
                      />
                      <span className="text-[var(--ink-secondary)]">-</span>
                      <input
                        type="time"
                        value={r.outTime}
                        onChange={(e) => updateRow(r.teacherId, { outTime: e.target.value })}
                        className="w-24 rounded-[8px] border border-[var(--border-hairline)] bg-[var(--surface-subtle)] text-[var(--ink-primary)] px-1.5 py-1 text-xs focus:border-[#E8A317] focus:ring-1 focus:ring-[#E8A317] focus:outline-none"
                      />
                    </div>
                  </Td>
                  <Td>
                    <input
                      type="text"
                      placeholder="মন্তব্য..."
                      value={r.remark}
                      onChange={(e) => updateRow(r.teacherId, { remark: e.target.value })}
                      className="w-full rounded-[8px] border border-[var(--border-hairline)] bg-[var(--surface-subtle)] text-[var(--ink-primary)] placeholder-[var(--ink-tertiary)] px-2 py-1 text-xs focus:border-[#E8A317] focus:ring-1 focus:ring-[#E8A317] focus:outline-none"
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
