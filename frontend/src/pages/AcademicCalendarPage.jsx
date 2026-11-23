import { useState } from 'react'
import { api } from '../api/client.js'
import { useAuth } from '../auth/useAuth.js'
import { FormActions } from '../components/extra.jsx'
import { Alert, Badge, Button, Card, LoadState, Modal, PageHeader, SelectField, TextField } from '../components/ui.jsx'
import { useQuery } from '../hooks/useQuery.js'
import { formatDate } from '../utils/format.js'

const EVENT_TYPES = [
  { value: 'EXAM', label: 'পরীক্ষা (Exam)', color: 'bg-rose-100 text-rose-800 border-rose-200' },
  { value: 'SPORTS', label: 'খেলাধুলা (Sports)', color: 'bg-emerald-100 text-emerald-800 border-emerald-200' },
  { value: 'CULTURAL', label: 'সাংস্কৃতিক অনুষ্ঠান (Cultural)', color: 'bg-indigo-100 text-indigo-800 border-indigo-200' },
  { value: 'MEETING', label: 'সমাবেশ / মিটিং (Meeting)', color: 'bg-amber-100 text-amber-800 border-amber-200' },
  { value: 'VACATION', label: 'অবকাশ / ছুটি (Vacation)', color: 'bg-purple-100 text-purple-800 border-purple-200' },
  { value: 'OBSERVANCE', label: 'দিবস উদযাপন (Observance)', color: 'bg-sky-100 text-sky-800 border-sky-200' },
  { value: 'OTHER', label: 'অন্যান্য (Other)', color: 'bg-gray-100 text-gray-800 border-gray-200' },
]

const AUDIENCE_OPTIONS = [
  { value: 'ALL', label: 'সবার জন্য (সকল শিক্ষার্থী, শিক্ষক ও অভিভাবক)' },
  { value: 'STUDENTS', label: 'শুধু শিক্ষার্থীদের জন্য' },
  { value: 'TEACHERS', label: 'শুধু শিক্ষকদের জন্য' },
  { value: 'PARENTS', label: 'শুধু অভিভাবকদের জন্য' },
]

const WEEKDAY_NAMES = ['শনিবার', 'রবিবার', 'সোমবার', 'মঙ্গলবার', 'বুধবার', 'বৃহস্পতিবার', 'শুক্রবার']

function EventModal({ initial, onDone, onClose }) {
  const [values, setValues] = useState({
    title: initial?.title || '',
    eventType: initial?.eventType || 'EXAM',
    startDate: initial?.startDate || '',
    endDate: initial?.endDate || '',
    isHoliday: initial?.holiday || false,
    description: initial?.description || '',
    targetAudience: initial?.targetAudience || 'ALL',
  })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(e) {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      const body = {
        title: values.title,
        eventType: values.eventType,
        startDate: values.startDate,
        endDate: values.endDate,
        isHoliday: values.isHoliday,
        description: values.description || null,
        targetAudience: values.targetAudience,
      }
      if (initial?.id) {
        await api(`/api/academic-events/${initial.id}`, { method: 'PUT', body })
      } else {
        await api('/api/academic-events', { method: 'POST', body })
      }
      onDone()
    } catch (err) {
      setError(err.message)
      setBusy(false)
    }
  }

  return (
    <Modal title={initial ? 'একাডেমিক ইভেন্ট সম্পাদনা' : 'নতুন একাডেমিক ইভেন্ট'} onClose={onClose}>
      <form onSubmit={submit} className="space-y-4">
        {error && <Alert>{error}</Alert>}
        <TextField
          label="ইভেন্টের শিরোনাম"
          value={values.title}
          placeholder="যেমন: ১ম সাময়িক পরীক্ষা বা বার্ষিক ক্রীড়া প্রতিযোগিতা"
          required
          onChange={(e) => setValues((c) => ({ ...c, title: e.target.value }))}
        />
        <SelectField
          label="ইভেন্টের ধরন"
          options={EVENT_TYPES.map((t) => ({ value: t.value, label: t.label }))}
          value={values.eventType}
          required
          onChange={(e) => setValues((c) => ({ ...c, eventType: e.target.value }))}
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
        <div className="flex items-center gap-2 pt-1">
          <input
            id="isHoliday"
            type="checkbox"
            checked={values.isHoliday}
            onChange={(e) => setValues((c) => ({ ...c, isHoliday: e.target.checked }))}
            className="h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
          />
          <label htmlFor="isHoliday" className="text-sm font-medium text-gray-700">
            এই দিনটিতে স্কুল ছুটি থাকবে (হাজিরা বন্ধ থাকবে)
          </label>
        </div>
        <SelectField
          label="লক্ষ্য শ্রোতা (Audience)"
          options={AUDIENCE_OPTIONS}
          value={values.targetAudience}
          required
          onChange={(e) => setValues((c) => ({ ...c, targetAudience: e.target.value }))}
        />
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">বিস্তারিত বিবরণ (ঐচ্ছিক)</label>
          <textarea
            rows={3}
            value={values.description}
            onChange={(e) => setValues((c) => ({ ...c, description: e.target.value }))}
            className="w-full rounded-md border border-gray-300 p-2 text-sm shadow-sm focus:border-indigo-500 focus:outline-none"
            placeholder="ইভেন্টের বিস্তারিত তথ্য, সময়সূচি বা নির্দেশনা..."
          />
        </div>
        <FormActions busy={busy} submitLabel="সংরক্ষণ করুন" onCancel={onClose} />
      </form>
    </Modal>
  )
}

export default function AcademicCalendarPage() {
  const { user } = useAuth()
  const isAdmin = user?.role === 'ADMIN'

  const today = new Date()
  const [year, setYear] = useState(() => today.getFullYear())
  const [month, setMonth] = useState(() => today.getMonth() + 1)
  const [modal, setModal] = useState(null) // { type: 'add' } | { type: 'edit', event } | { type: 'view', event }

  const calendar = useQuery(`/api/calendar/month?year=${year}&month=${month}`)

  function prevMonth() {
    if (month === 1) {
      setMonth(12)
      setYear((y) => y - 1)
    } else {
      setMonth((m) => m - 1)
    }
  }

  function nextMonth() {
    if (month === 12) {
      setMonth(1)
      setYear((y) => y + 1)
    } else {
      setMonth((m) => m + 1)
    }
  }

  function currentMonth() {
    setYear(today.getFullYear())
    setMonth(today.getMonth() + 1)
  }

  function reload() {
    setModal(null)
    calendar.reload()
  }

  async function deleteEvent(id) {
    if (!window.confirm('এই একাডেমিক ইভেন্টটি মুছে ফেলতে চান?')) return
    await api(`/api/academic-events/${id}`, { method: 'DELETE' })
    reload()
  }

  const days = calendar.data?.days || []

  // ক্যালেন্ডারের প্রথম দিনের বার অনুযায়ী শুরুতে ফাঁকা সেল বসানো (শনিবার = 0, রবিবার = 1 ... শুক্রবার = 6)
  const firstDay = days[0]
  let leadingBlanks = 0
  if (firstDay) {
    const map = { SATURDAY: 0, SUNDAY: 1, MONDAY: 2, TUESDAY: 3, WEDNESDAY: 4, THURSDAY: 5, FRIDAY: 6 }
    leadingBlanks = map[firstDay.dayOfWeek] ?? 0
  }

  const todayStr = today.toISOString().slice(0, 10)

  return (
    <>
      <PageHeader
        title="একাডেমিক ক্যালেন্ডার ও বার্ষিক সূচি"
        subtitle="পরীক্ষা, বার্ষিক অনুষ্ঠান, সভা ও ছুটির সামগ্রিক ক্যালেন্ডার"
        action={
          <div className="flex items-center gap-2">
            <Button variant="secondary" size="sm" onClick={prevMonth}>
              ← আগের মাস
            </Button>
            <Button variant="secondary" size="sm" onClick={currentMonth}>
              চলতি মাস
            </Button>
            <Button variant="secondary" size="sm" onClick={nextMonth}>
              পরের মাস →
            </Button>
            {isAdmin && (
              <Button size="sm" onClick={() => setModal({ type: 'add' })}>
                + ইভেন্ট যোগ করুন
              </Button>
            )}
          </div>
        }
      />

      {/* মাস ও সারসংক্ষেপ হেডার */}
      <div className="mb-4 flex flex-wrap items-center justify-between gap-4 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
        <div>
          <h2 className="text-xl font-bold text-slate-800">
            {calendar.data?.monthName} {year}
          </h2>
          <p className="text-xs text-slate-500">বাংলা শিক্ষাবর্ষ অনুযায়ী ক্যালেন্ডার সূচি</p>
        </div>
        <div className="flex gap-4">
          <div className="rounded-lg bg-indigo-50 px-3 py-1.5 text-center">
            <p className="text-xs text-indigo-600 font-medium">মোট ইভেন্ট</p>
            <p className="text-lg font-bold text-indigo-900">{calendar.data?.totalEvents ?? 0}</p>
          </div>
          <div className="rounded-lg bg-rose-50 px-3 py-1.5 text-center">
            <p className="text-xs text-rose-600 font-medium">ছুটির দিন</p>
            <p className="text-lg font-bold text-rose-900">{calendar.data?.totalHolidays ?? 0}</p>
          </div>
        </div>
      </div>

      {/* ক্যালেন্ডার গ্রিড */}
      <Card className="overflow-hidden p-0">
        <LoadState loading={calendar.loading} error={calendar.error}>
          <div className="grid grid-cols-7 border-b border-gray-200 bg-slate-50 text-center font-semibold text-xs text-gray-700">
            {WEEKDAY_NAMES.map((name, idx) => (
              <div key={name} className={`py-2.5 ${idx === 6 ? 'text-rose-600 bg-rose-50/50' : ''}`}>
                {name}
              </div>
            ))}
          </div>

          <div className="grid grid-cols-7 auto-rows-fr divide-x divide-y divide-gray-100 bg-white">
            {/* শুরু ফাঁকা সেল */}
            {Array.from({ length: leadingBlanks }).map((_, i) => (
              <div key={`blank-${i}`} className="min-h-[110px] bg-slate-50/40 p-2" />
            ))}

            {/* দিনের সেলসমূহ */}
            {days.map((day) => {
              const isToday = day.date === todayStr
              const isFriday = day.dayOfWeek === 'FRIDAY'

              return (
                <div
                  key={day.date}
                  className={`min-h-[110px] p-2 transition flex flex-col justify-between ${
                    isFriday ? 'bg-rose-50/30' : ''
                  } ${isToday ? 'ring-2 ring-indigo-500 ring-inset bg-indigo-50/20' : ''}`}
                >
                  <div className="flex items-center justify-between">
                    <span
                      className={`inline-flex h-6 w-6 items-center justify-center rounded-full text-xs font-bold ${
                        isToday
                          ? 'bg-indigo-600 text-white'
                          : isFriday
                          ? 'text-rose-600'
                          : 'text-gray-700'
                      }`}
                    >
                      {new Date(day.date).getDate()}
                    </span>
                    {day.holidayName && (
                      <span className="truncate text-[10px] font-semibold text-rose-600" title={day.holidayName}>
                        {day.holidayName}
                      </span>
                    )}
                  </div>

                  <div className="mt-1 space-y-1 flex-1">
                    {day.events?.map((e) => {
                      const typeObj = EVENT_TYPES.find((t) => t.value === e.eventType)
                      const colorClass = typeObj ? typeObj.color : 'bg-gray-100 text-gray-800 border-gray-200'

                      return (
                        <div
                          key={e.id}
                          onClick={() => setModal({ type: 'view', event: e })}
                          className={`cursor-pointer truncate rounded border px-1.5 py-0.5 text-[11px] font-medium transition hover:opacity-80 shadow-xs ${colorClass}`}
                          title={`${e.title} (${e.eventType})`}
                        >
                          {e.title}
                        </div>
                      )
                    })}
                  </div>
                </div>
              )
            })}
          </div>
        </LoadState>
      </Card>

      {/* মোডালসমূহ */}
      {modal?.type === 'add' && <EventModal onDone={reload} onClose={() => setModal(null)} />}
      {modal?.type === 'edit' && (
        <EventModal initial={modal.event} onDone={reload} onClose={() => setModal(null)} />
      )}
      {modal?.type === 'view' && (
        <Modal title={modal.event.title} onClose={() => setModal(null)}>
          <div className="space-y-3">
            <div className="flex items-center gap-2">
              <Badge tone="blue">{modal.event.eventType}</Badge>
              {modal.event.holiday && <Badge tone="red">স্কুল বন্ধ</Badge>}
              <Badge tone="gray">শ্রোতা: {modal.event.targetAudience}</Badge>
            </div>
            <p className="text-sm text-gray-600">
              <strong>তারিখ:</strong> {formatDate(modal.event.startDate)} হতে {formatDate(modal.event.endDate)}
            </p>
            {modal.event.description && (
              <div className="rounded-lg bg-gray-50 p-3 text-sm text-gray-700">
                {modal.event.description}
              </div>
            )}
            <div className="flex justify-end gap-2 pt-2">
              {isAdmin && (
                <>
                  <Button variant="danger" size="sm" onClick={() => deleteEvent(modal.event.id)}>
                    মুছে ফেলুন
                  </Button>
                  <Button variant="secondary" size="sm" onClick={() => setModal({ type: 'edit', event: modal.event })}>
                    সম্পাদনা
                  </Button>
                </>
              )}
              <Button size="sm" onClick={() => setModal(null)}>
                বন্ধ করুন
              </Button>
            </div>
          </div>
        </Modal>
      )}
    </>
  )
}
