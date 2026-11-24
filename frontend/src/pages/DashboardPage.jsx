import { Link } from 'react-router'
import { useAuth } from '../auth/useAuth.js'
import { Badge, Card, LoadState, PageHeader } from '../components/ui.jsx'
import { useQuery } from '../hooks/useQuery.js'
import { formatDate } from '../utils/format.js'

function StatCard({ title, value, subtitle, icon, to }) {
  // If value is non-numeric status string (e.g. 'নেওয়া হয়নি' or 'আজ ছুটি'), size down gracefully
  const isStatusText = typeof value === 'string' && (value.includes('নেওয়া হয়নি') || value.includes('আজ ছুটি'))

  const content = (
    <Card className="relative overflow-hidden p-5 transition duration-150 hover:shadow-md focus-ring-apple">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0 flex-1">
          <p className="text-[13px] font-medium text-[var(--ink-secondary)]">{title}</p>
          <div className="mt-2">
            {isStatusText ? (
              <div className="flex items-center gap-2">
                <span className="h-2 w-2 rounded-full bg-[#B36B18]" aria-hidden="true" />
                <span className="text-base font-semibold text-[var(--ink-secondary)] font-['Anek_Bangla',sans-serif]">
                  {value}
                </span>
              </div>
            ) : (
              <p className="text-3xl font-bold tracking-tight text-[var(--ink-primary)] font-['Anek_Bangla',sans-serif] tabular-nums">
                {value}
              </p>
            )}
          </div>
          {subtitle && (
            <p className="mt-1.5 text-xs text-[var(--ink-tertiary)] font-normal truncate">{subtitle}</p>
          )}
        </div>
        <div className="text-[var(--ink-tertiary)] shrink-0 pt-0.5" aria-hidden="true">
          {icon}
        </div>
      </div>
    </Card>
  )

  return to ? (
    <Link to={to} className="block rounded-[16px] focus-ring-apple">
      {content}
    </Link>
  ) : (
    content
  )
}

function AttendanceChart({ data }) {
  if (!data || data.length === 0) {
    return <p className="py-8 text-center text-sm text-[var(--ink-secondary)]">হাজিরার তথ্য পাওয়া যায়নি।</p>
  }

  return (
    <div className="space-y-4">
      {/* Chart Canvas with Hairline Baseline */}
      <div className="flex items-end justify-between gap-2 pt-6 pb-2 border-b border-[var(--border-hairline)] min-h-[160px]">
        {data.map((item, idx) => {
          const pct = item.percentage != null ? Number(item.percentage) : 0
          const hasData = item.percentage != null && pct > 0
          const heightPercent = hasData ? Math.max(pct, 6) : 0
          const isGood = pct >= 80

          return (
            <div key={idx} className="group relative flex flex-1 flex-col items-center gap-2">
              {/* Tooltip on hover */}
              <div className="absolute -top-10 hidden rounded-[6px] bg-[#12231D] px-2.5 py-1 text-xs font-mono text-white group-hover:block z-20 whitespace-nowrap shadow-lg">
                {item.date}: {pct.toFixed(1)}% (উপস্থিত: {item.present}, অনুপস্থিত: {item.absent})
              </div>

              {/* Bar Container */}
              <div className="flex h-36 w-full max-w-[2.25rem] items-end rounded-t bg-[#0E3B2E]/5">
                {hasData ? (
                  <div
                    style={{ height: `${heightPercent}%` }}
                    className={`w-full rounded-t transition-all duration-300 ${
                      isGood ? 'bg-[#0E3B2E] group-hover:bg-[#164E3D]' : 'bg-[#E8A317] group-hover:bg-[#D49210]'
                    }`}
                  />
                ) : (
                  /* Designed Quiet Baseline for 0 or empty values */
                  <div className="w-full h-[2px] bg-[var(--border-hairline)]" />
                )}
              </div>

              {/* Day Label & Value */}
              <span className="text-[13px] font-medium text-[var(--ink-secondary)]">{item.dayName}</span>
              <span className="text-[11px] text-[var(--ink-tertiary)] font-mono tabular-nums">
                {item.percentage != null ? `${Math.round(pct)}%` : '—'}
              </span>
            </div>
          )
        })}
      </div>

      {/* Legend with plain hairline */}
      <div className="flex items-center justify-between text-xs text-[var(--ink-secondary)] pt-1">
        <span className="flex items-center gap-2 font-medium">
          <span className="h-2.5 w-2.5 rounded-full bg-[#0E3B2E]" aria-hidden="true" /> ৮০%+ উপস্থিতি
        </span>
        <span className="flex items-center gap-2 font-medium">
          <span className="h-2.5 w-2.5 rounded-full bg-[#E8A317]" aria-hidden="true" /> ৮০% এর কম
        </span>
      </div>
    </div>
  )
}

export default function DashboardPage() {
  const { user } = useAuth()
  const { data, loading, error } = useQuery('/api/dashboard/summary')

  const stats = data?.stats
  const trend = data?.attendanceTrend ?? []
  const activities = data?.recentActivities ?? []
  const holidays = data?.upcomingHolidays ?? []

  return (
    <div className="space-y-6">
      <PageHeader
        title={`স্বাগতম, ${user.fullName}`}
        subtitle={`সেশন: ${stats?.currentSessionName ?? '...'} · আজকের সম্পূর্ণ চিত্র`}
      />

      <LoadState loading={loading} error={error}>
        {data && (
          <>
            {/* ১. স্ট্যাটাস কার্ডস (৪টি সুষম কার্ড, নম্বর হিরো, ক্লিন লাইন আইকন) */}
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <StatCard
                title="মোট শিক্ষার্থী"
                value={stats.totalStudents}
                subtitle="সকল শ্রেণিতে নিবন্ধিত"
                to="/students"
                icon={
                  <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.75} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
                  </svg>
                }
              />

              <StatCard
                title="মোট শিক্ষক"
                value={stats.totalTeachers}
                subtitle="সক্রিয় শিক্ষক ও কর্মী"
                to="/teachers"
                icon={
                  <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.75} d="M21 13.255A23.931 23.931 0 0112 15c-3.183 0-6.22-.62-9-1.745M16 6V4a2 2 0 00-2-2h-4a2 2 0 00-2 2v2m4 6h.01M5 20h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                  </svg>
                }
              />

              <StatCard
                title="আজকের উপস্থিতি"
                value={
                  stats.isHolidayToday
                    ? 'আজ ছুটি'
                    : stats.todayAttendancePercent != null
                      ? `${Number(stats.todayAttendancePercent).toFixed(1)}%`
                      : 'নেওয়া হয়নি'
                }
                subtitle={
                  stats.isHolidayToday
                    ? 'সাপ্তাহিক বা সরকারি ছুটি'
                    : `উপস্থিত: ${stats.todayPresent} · অনুপস্থিত: ${stats.todayAbsent}`
                }
                to="/attendance"
                icon={
                  <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.75} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                }
              />

              <StatCard
                title="মোট শ্রেণি ও সেকশন"
                value={stats.totalClasses}
                subtitle="চলমান অ্যাকাডেমিক ক্লাস"
                to="/classes"
                icon={
                  <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.75} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                  </svg>
                }
              />
            </div>

            {/* ২. মাঝের সেকশন: চার্ট ও কুইক অ্যাকশন */}
            <div className="grid gap-6 lg:grid-cols-3">
              {/* ৭ দিনের উপস্থিতি ট্রেন্ড চার্ট */}
              <Card className="p-6 lg:col-span-2">
                <div className="flex items-center justify-between border-b border-[var(--border-hairline)] pb-4">
                  <div>
                    <h3 className="text-base font-bold text-[var(--ink-primary)] font-['Anek_Bangla',sans-serif]">
                      গত ৭ দিনের উপস্থিতির ট্রেন্ড
                    </h3>
                    <p className="text-xs text-[var(--ink-secondary)] mt-0.5">কর্মদিবসগুলোর গড় উপস্থিতির শতকরা হার</p>
                  </div>
                  <Link
                    to="/attendance/report"
                    className="text-xs font-semibold text-[var(--ink-secondary)] hover:text-[#0E3B2E] transition-colors"
                  >
                    মাসিক রিপোর্ট →
                  </Link>
                </div>
                <AttendanceChart data={trend} />
              </Card>

              {/* কুইক অ্যাকশন গ্রুপড লিস্ট */}
              <Card className="flex flex-col justify-between p-6">
                <div>
                  <h3 className="text-base font-bold text-[var(--ink-primary)] font-['Anek_Bangla',sans-serif] border-b border-[var(--border-hairline)] pb-4">
                    কুইক অ্যাকশন
                  </h3>

                  <div className="mt-4 divide-y divide-[var(--border-hairline)] rounded-[12px] border border-[var(--border-hairline)] overflow-hidden">
                    <Link
                      to="/attendance"
                      className="flex items-center justify-between px-3.5 py-3 text-sm font-medium text-[var(--ink-primary)] hover:bg-[var(--surface-hover)] transition-colors"
                    >
                      <span className="flex items-center gap-3">
                        <svg className="h-[18px] w-[18px] text-[var(--ink-secondary)]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.75} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                        আজকের হাজিরা নিন
                      </span>
                      <span className="text-[var(--ink-tertiary)] text-xs font-bold">→</span>
                    </Link>

                    <Link
                      to="/students/new"
                      className="flex items-center justify-between px-3.5 py-3 text-sm font-medium text-[var(--ink-primary)] hover:bg-[var(--surface-hover)] transition-colors"
                    >
                      <span className="flex items-center gap-3">
                        <svg className="h-[18px] w-[18px] text-[var(--ink-secondary)]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.75} d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
                        </svg>
                        নতুন শিক্ষার্থী ভর্তি
                      </span>
                      <span className="text-[var(--ink-tertiary)] text-xs font-bold">→</span>
                    </Link>

                    <Link
                      to="/notifications"
                      className="flex items-center justify-between px-3.5 py-3 text-sm font-medium text-[var(--ink-primary)] hover:bg-[var(--surface-hover)] transition-colors"
                    >
                      <span className="flex items-center gap-3">
                        <svg className="h-[18px] w-[18px] text-[var(--ink-secondary)]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.75} d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" />
                        </svg>
                        অভিভাবকের বার্তা পাঠান
                      </span>
                      <span className="text-[var(--ink-tertiary)] text-xs font-bold">→</span>
                    </Link>

                    <Link
                      to="/routine"
                      className="flex items-center justify-between px-3.5 py-3 text-sm font-medium text-[var(--ink-primary)] hover:bg-[var(--surface-hover)] transition-colors"
                    >
                      <span className="flex items-center gap-3">
                        <svg className="h-[18px] w-[18px] text-[var(--ink-secondary)]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.75} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                        </svg>
                        ক্লাস রুটিন দেখুন
                      </span>
                      <span className="text-[var(--ink-tertiary)] text-xs font-bold">→</span>
                    </Link>
                  </div>
                </div>

                {/* শান্ত ইনসেট সহায়িকা ব্লক */}
                <div className="mt-5 rounded-[10px] border border-[var(--border-hairline)] bg-[var(--surface-subtle)] p-3.5 text-xs text-[var(--ink-secondary)]">
                  <p className="font-semibold text-[var(--ink-primary)]">সহায়িকা</p>
                  <p className="mt-1 leading-relaxed">হাজিরা ফাইনাল করার পর পরিবর্তনের জন্য Admin-এর অনুমোদনের প্রয়োজন হবে।</p>
                </div>
              </Card>
            </div>

            {/* ৩. নিচের সেকশন: সাম্প্রতিক কার্যক্রম ও আসছে ছুটি */}
            <div className="grid gap-6 lg:grid-cols-2">
              {/* সাম্প্রতিক অডিট কার্যক্রম */}
              <Card className="p-6">
                <div className="flex items-center justify-between border-b border-[var(--border-hairline)] pb-3.5">
                  <h3 className="text-base font-bold text-[var(--ink-primary)] font-['Anek_Bangla',sans-serif]">
                    সাম্প্রতিক কার্যক্রম
                  </h3>
                  <Link
                    to="/audit-logs"
                    className="text-xs font-semibold text-[var(--ink-secondary)] hover:text-[#0E3B2E] transition-colors"
                  >
                    সব অডিট লগ →
                  </Link>
                </div>

                {activities.length === 0 ? (
                  <p className="py-8 text-center text-sm text-[var(--ink-secondary)]">কোনো সাম্প্রতিক কার্যক্রম পাওয়া যায়নি।</p>
                ) : (
                  <ul className="mt-3 divide-y divide-[var(--border-hairline)] text-sm">
                    {activities.map((act) => (
                      <li key={act.id} className="py-3">
                        <div className="flex items-center justify-between">
                          <Badge tone="indigo">{act.action}</Badge>
                          <span className="text-xs text-[var(--ink-tertiary)] font-mono">{formatDate(act.createdAt)}</span>
                        </div>
                        <p className="mt-1.5 text-xs text-[var(--ink-secondary)] line-clamp-2 leading-relaxed">
                          {act.details}
                        </p>
                      </li>
                    ))}
                  </ul>
                )}
              </Card>

              {/* আসছে ছুটির তালিকা */}
              <Card className="p-6">
                <div className="flex items-center justify-between border-b border-[var(--border-hairline)] pb-3.5">
                  <h3 className="text-base font-bold text-[var(--ink-primary)] font-['Anek_Bangla',sans-serif]">
                    আসন্ন ছুটির দিন
                  </h3>
                  <Link
                    to="/holidays"
                    className="text-xs font-semibold text-[var(--ink-secondary)] hover:text-[#0E3B2E] transition-colors"
                  >
                    ছুটির ক্যালেন্ডার →
                  </Link>
                </div>

                {holidays.length === 0 ? (
                  <p className="py-8 text-center text-sm text-[var(--ink-secondary)]">সামনে কোনো নির্ধারিত ছুটি নেই।</p>
                ) : (
                  <ul className="mt-3 divide-y divide-[var(--border-hairline)] text-sm">
                    {holidays.map((h) => (
                      <li key={h.id} className="flex items-center justify-between py-3">
                        <div>
                          <p className="font-semibold text-[var(--ink-primary)] text-sm">{h.name}</p>
                          <p className="text-xs text-[var(--ink-tertiary)] mt-0.5 font-mono">{formatDate(h.holidayDate)}</p>
                        </div>
                        <Badge tone="amber">ছুটি</Badge>
                      </li>
                    ))}
                  </ul>
                )}
              </Card>
            </div>
          </>
        )}
      </LoadState>
    </div>
  )
}
