import { useState } from 'react'
import { Badge, Alert, Card, EmptyState, LoadState, PageHeader, SelectField, Table, Td } from '../components/ui.jsx'
import { useQuery } from '../hooks/useQuery.js'
import { formatMonth, todayISO } from '../utils/format.js'

const MONTH_NAMES = [
  'জানুয়ারি',
  'ফেব্রুয়ারি',
  'মার্চ',
  'এপ্রিল',
  'মে',
  'জুন',
  'জুলাই',
  'আগস্ট',
  'সেপ্টেম্বর',
  'অক্টোবর',
  'নভেম্বর',
  'ডিসেম্বর',
]

function rateTone(rate) {
  if (rate === null || rate === undefined) return 'gray'
  if (rate >= 90) return 'green'
  if (rate >= 75) return 'amber'
  return 'red'
}

function Summary({ label, value }) {
  return (
    <Card className="p-4">
      <p className="text-xs text-gray-500">{label}</p>
      <p className="mt-1 text-xl font-semibold text-gray-900">{value}</p>
    </Card>
  )
}

export default function AttendanceReportPage() {
  const [thisYear] = useState(() => Number(todayISO().slice(0, 4)))
  const [year, setYear] = useState(thisYear)
  const [month, setMonth] = useState(() => Number(todayISO().slice(5, 7)))
  const [sectionId, setSectionId] = useState('')

  const yearMonth = `${year}-${String(month).padStart(2, '0')}`

  // মাসের মাঝামাঝি (১৫ তারিখ) যে সেকশনগুলো চালু ছিল সেগুলো
  const sections = useQuery(`/api/attendance/my-sections?date=${yearMonth}-15`)
  const options = sections.data ?? []
  const effectiveSectionId = sectionId || (options.length === 1 ? String(options[0].sectionId) : '')

  const report = useQuery(
    effectiveSectionId ? `/api/attendance/reports/monthly?sectionId=${effectiveSectionId}&month=${yearMonth}` : null,
  )

  const sectionOptions = [
    { value: '', label: 'বাছুন' },
    ...options.map((s) => ({ value: String(s.sectionId), label: `${s.className} / ${s.sectionName}` })),
  ]
  const monthOptions = MONTH_NAMES.map((name, index) => ({ value: String(index + 1), label: name }))
  const yearOptions = [thisYear - 2, thisYear - 1, thisYear].map((y) => ({ value: String(y), label: String(y) }))

  return (
    <>
      <PageHeader title="মাসিক হাজিরা রিপোর্ট" subtitle="সেকশন ও মাস বেছে প্রতিজনের উপস্থিতির হার দেখুন" />

      <Card className="mb-4 p-4">
        <div className="grid gap-3 sm:grid-cols-3">
          <SelectField
            label="ক্লাস / সেকশন"
            options={sectionOptions}
            value={effectiveSectionId}
            onChange={(event) => setSectionId(event.target.value)}
          />
          <SelectField
            label="মাস"
            options={monthOptions}
            value={String(month)}
            onChange={(event) => setMonth(Number(event.target.value))}
          />
          <SelectField
            label="বছর"
            options={yearOptions}
            value={String(year)}
            onChange={(event) => setYear(Number(event.target.value))}
          />
        </div>
      </Card>

      <LoadState loading={sections.loading} error={sections.error}>
        {options.length === 0 && (
          <Card>
            <EmptyState>
              এই মাসে আপনার জন্য কোনো সেকশন নেই। শিক্ষক হলে Admin-কে বলুন আপনাকে কোনো সেকশনে বরাদ্দ দিতে।
            </EmptyState>
          </Card>
        )}
        {options.length > 0 && !effectiveSectionId && (
          <Card>
            <EmptyState>রিপোর্ট দেখতে ওপর থেকে একটা সেকশন বাছুন।</EmptyState>
          </Card>
        )}
      </LoadState>

      {effectiveSectionId && (
        <LoadState loading={report.loading} error={report.error}>
          {report.data && (
            <div className="space-y-4">
              <div>
                <h2 className="text-lg font-semibold text-gray-900">
                  {report.data.className} / {report.data.sectionName} · {formatMonth(report.data.month)}
                </h2>
              </div>

              <div className="grid gap-3 sm:grid-cols-3">
                <Summary label="আজ পর্যন্ত কর্মদিবস" value={report.data.workingDays} />
                <Summary label="যত দিন হাজিরা নেওয়া হয়েছে" value={report.data.markedDays} />
                <Summary label="শিক্ষার্থী" value={report.data.students.length} />
              </div>

              {report.data.markedDays < report.data.workingDays && (
                <Alert type="info">
                  {report.data.workingDays} কর্মদিবসের মধ্যে {report.data.markedDays} দিনের হাজিরা নেওয়া হয়েছে। যেসব
                  দিনের হাজিরা নেওয়া হয়নি, সেগুলো শতকরা হারে ধরা হয়নি।
                </Alert>
              )}

              <Card>
                {report.data.students.length === 0 ? (
                  <EmptyState>এই মাসে এই সেকশনে কোনো শিক্ষার্থী ছিল না।</EmptyState>
                ) : (
                  <Table
                    headers={['রোল', 'নাম', 'আইডি', 'উপস্থিত', 'দেরি', 'অনুপস্থিত', 'ছুটি', 'উপস্থিতির হার']}
                  >
                    {report.data.students.map((s) => (
                      <tr key={s.enrollmentId}>
                        <Td>{s.rollNumber}</Td>
                        <Td className="font-medium text-gray-900">{s.studentName}</Td>
                        <Td className="font-mono text-xs">{s.studentCode}</Td>
                        <Td>{s.present}</Td>
                        <Td>{s.late}</Td>
                        <Td>{s.absent}</Td>
                        <Td>{s.leave}</Td>
                        <Td>
                          <Badge tone={rateTone(s.percentage)}>
                            {s.percentage === null ? '—' : `${Number(s.percentage).toFixed(2)}%`}
                          </Badge>
                        </Td>
                      </tr>
                    ))}
                  </Table>
                )}
              </Card>

              <p className="text-xs text-gray-500">
                হার = (উপস্থিত + দেরি) ÷ (উপস্থিত + দেরি + অনুপস্থিত) × ১০০। "ছুটি" হিসাবের বাইরে। ৯০%-এর ওপরে সবুজ,
                ৭৫%-এর ওপরে হলুদ, তার নিচে লাল।
              </p>
            </div>
          )}
        </LoadState>
      )}
    </>
  )
}