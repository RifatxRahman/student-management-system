import { Link } from 'react-router'
import { useAuth } from '../auth/useAuth.js'
import { Badge, Card, LoadState, PageHeader } from '../components/ui.jsx'
import { useQuery } from '../hooks/useQuery.js'
import { formatDate } from '../utils/format.js'

function StatCard({ title, value, subtitle, icon, tone = 'indigo', to }) {
  const tones = {
    indigo: 'bg-indigo-50 text-indigo-600 border-indigo-100',
    green: 'bg-emerald-50 text-emerald-600 border-emerald-100',
    amber: 'bg-amber-50 text-amber-600 border-amber-100',
    purple: 'bg-purple-50 text-purple-600 border-purple-100',
  }

  const content = (
    <Card className="relative overflow-hidden p-5 transition hover:shadow-md">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm font-medium text-gray-500">{title}</p>
          <p className="mt-1 text-2xl font-bold tracking-tight text-gray-900">{value}</p>
          {subtitle && <p className="mt-1 text-xs text-gray-500">{subtitle}</p>}
        </div>
        <div className={`flex h-12 w-12 items-center justify-center rounded-xl border ${tones[tone]}`}>
          {icon}
        </div>
      </div>
    </Card>
  )

  return to ? <Link to={to}>{content}</Link> : content
}

function AttendanceChart({ data }) {
  if (!data || data.length === 0) {
    return <p className="text-sm text-gray-500">হাজিরার তথ্য পাওয়া যায়নি।</p>
  }

  return (
    <div className="space-y-4">
      <div className="flex items-end justify-between gap-2 pt-6">
        {data.map((item, idx) => {
          const pct = item.percentage != null ? Number(item.percentage) : 0
          const heightPercent = Math.max(pct, 5) // minimum bar visibility
          const isGood = pct >= 80

          return (
            <div key={idx} className="group relative flex flex-1 flex-col items-center gap-2">
              {/* Tooltip on hover */}
              <div className="absolute -top-10 hidden rounded bg-gray-900 px-2 py-1 text-xs font-mono text-white group-hover:block z-10 whitespace-nowrap shadow">
                {item.date}: {pct.toFixed(1)}% (উপস্থিত: {item.present}, অনুপস্থিত: {item.absent})
              </div>

              {/* Bar */}
              <div className="flex h-36 w-full max-w-[2.5rem] items-end rounded-t bg-gray-100">
                <div
                  style={{ height: `${heightPercent}%` }}
                  className={`w-full rounded-t transition-all duration-500 ${
                    isGood ? 'bg-indigo-600 group-hover:bg-indigo-700' : 'bg-amber-500 group-hover:bg-amber-600'
                  }`}
                />
              </div>

              {/* Day Label */}
              <span className="text-xs font-medium text-gray-600">{item.dayName}</span>
              <span className="text-[10px] text-gray-400 font-mono">{item.percentage != null ? `${Math.round(pct)}%` : '—'}</span>
            </div>
          )
        })}
      </div>
      <div className="flex items-center justify-between text-xs text-gray-500 border-t border-gray-100 pt-3">
        <span className="flex items-center gap-1.5">
          <span className="h-2.5 w-2.5 rounded-full bg-indigo-600" /> ৮০%+ উপস্থিতি
        </span>
        <span className="flex items-center gap-1.5">
          <span className="h-2.5 w-2.5 rounded-full bg-amber-500" /> ৮০% এর কম
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
            {/* ১. স্ট্যাটাস কার্ডস */}
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <StatCard
                title="মোট শিক্ষার্থী"
                value={stats.totalStudents}
                subtitle="সকল শ্রেণিতে নিবন্ধিত"
                tone="indigo"
                to="/students"
                icon={
                  <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
                  </svg>
                }
              />

              <StatCard
                title="মোট শিক্ষক"
                value={stats.totalTeachers}
                subtitle="সক্রিয় শিক্ষক ও কর্মী"
                tone="purple"
                to="/teachers"
                icon={
                  <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 13.255A23.931 23.931 0 0112 15c-3.183 0-6.22-.62-9-1.745M16 6V4a2 2 0 00-2-2h-4a2 2 0 00-2 2v2m4 6h.01M5 20h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
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
                tone={stats.isHolidayToday ? 'amber' : 'green'}
                to="/attendance"
                icon={
                  <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                }
              />

              <StatCard
                title="মোট শ্রেণি ও সেকশন"
                value={stats.totalClasses}
                subtitle="চলমান অ্যাকাডেমিক ক্লাস"
                tone="amber"
                to="/classes"
                icon={
                  <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                  </svg>
                }
              />
            </div>

            {/* ২. মাঝের সেকশন: চার্ট ও কুইক অ্যাকশন */}
            <div className="grid gap-6 lg:grid-cols-3">
              {/* ৭ দিনের উপস্থিতি ট্রেন্ড চার্ট */}
              <Card className="p-6 lg:col-span-2">
                <div className="flex items-center justify-between border-b border-gray-100 pb-4">
                  <div>
                    <h3 className="text-base font-semibold text-gray-900">গত ৭ দিনের উপস্থিতির ট্রেন্ড</h3>
                    <p className="text-xs text-gray-500">কর্মদিবসগুলোর গড় উপস্থিতির শতকরা হার</p>
                  </div>
                  <Link to="/attendance/report" className="text-xs font-medium text-indigo-600 hover:text-indigo-800">
                    মাসিক রিপোর্ট →
                  </Link>
                </div>
                <AttendanceChart data={trend} />
              </Card>

              {/* কুইক অ্যাকশন বোতামসমূহ */}
              <Card className="flex flex-col justify-between p-6">
                <div>
                  <h3 className="text-base font-semibold text-gray-900 border-b border-gray-100 pb-4">
                    কুইক অ্যাকশন
                  </h3>
                  <div className="mt-4 space-y-2.5">
                    <Link
                      to="/attendance"
                      className="flex items-center justify-between rounded-lg border border-gray-200 px-3.5 py-2.5 text-sm font-medium text-gray-700 transition hover:border-indigo-500 hover:bg-indigo-50/50 hover:text-indigo-700"
                    >
                      <span className="flex items-center gap-2.5">
                        <span className="flex h-7 w-7 items-center justify-center rounded-md bg-indigo-100 text-indigo-600">✓</span>
                        আজকের হাজিরা নিন
                      </span>
                      <span className="text-gray-400">→</span>
                    </Link>

                    <Link
                      to="/students/new"
                      className="flex items-center justify-between rounded-lg border border-gray-200 px-3.5 py-2.5 text-sm font-medium text-gray-700 transition hover:border-indigo-500 hover:bg-indigo-50/50 hover:text-indigo-700"
                    >
                      <span className="flex items-center gap-2.5">
                        <span className="flex h-7 w-7 items-center justify-center rounded-md bg-emerald-100 text-emerald-600">+</span>
                        নতুন শিক্ষার্থী ভর্তি
                      </span>
                      <span className="text-gray-400">→</span>
                    </Link>

                    <Link
                      to="/notifications"
                      className="flex items-center justify-between rounded-lg border border-gray-200 px-3.5 py-2.5 text-sm font-medium text-gray-700 transition hover:border-indigo-500 hover:bg-indigo-50/50 hover:text-indigo-700"
                    >
                      <span className="flex items-center gap-2.5">
                        <span className="flex h-7 w-7 items-center justify-center rounded-md bg-amber-100 text-amber-600">✉</span>
                        অভিভাবকের বার্তা পাঠান
                      </span>
                      <span className="text-gray-400">→</span>
                    </Link>

                    <Link
                      to="/routine"
                      className="flex items-center justify-between rounded-lg border border-gray-200 px-3.5 py-2.5 text-sm font-medium text-gray-700 transition hover:border-indigo-500 hover:bg-indigo-50/50 hover:text-indigo-700"
                    >
                      <span className="flex items-center gap-2.5">
                        <span className="flex h-7 w-7 items-center justify-center rounded-md bg-purple-100 text-purple-600">📅</span>
                        ক্লাস রুটিন দেখুন
                      </span>
                      <span className="text-gray-400">→</span>
                    </Link>
                  </div>
                </div>

                <div className="mt-6 rounded-lg bg-slate-50 p-3 text-xs text-gray-500">
                  <p className="font-medium text-gray-700">সহায়িকা</p>
                  <p className="mt-0.5">হাজিরা ফাইনাল করার পর পরিবর্তনের জন্য Admin-এর অনুমোদনের প্রয়োজন হবে।</p>
                </div>
              </Card>
            </div>

            {/* ৩. নিচের সেকশন: সাম্প্রতিক কার্যক্রম ও আসছে ছুটি */}
            <div className="grid gap-6 lg:grid-cols-2">
              {/* সাম্প্রতিক অডিট কার্যক্রম */}
              <Card className="p-6">
                <div className="flex items-center justify-between border-b border-gray-100 pb-3">
                  <h3 className="text-base font-semibold text-gray-900">সাম্প্রতিক কার্যক্রম</h3>
                  <Link to="/audit-logs" className="text-xs font-medium text-indigo-600 hover:text-indigo-800">
                    সব অডিট লগ →
                  </Link>
                </div>

                {activities.length === 0 ? (
                  <p className="py-6 text-center text-sm text-gray-500">কোনো সাম্প্রতিক কার্যক্রম পাওয়া যায়নি।</p>
                ) : (
                  <ul className="mt-3 divide-y divide-gray-100 text-sm">
                    {activities.map((act) => (
                      <li key={act.id} className="py-3">
                        <div className="flex items-center justify-between">
                          <Badge tone="indigo">{act.action}</Badge>
                          <span className="text-xs text-gray-400">{formatDate(act.createdAt)}</span>
                        </div>
                        <p className="mt-1 text-xs text-gray-700 line-clamp-2">{act.details}</p>
                      </li>
                    ))}
                  </ul>
                )}
              </Card>

              {/* আসছে ছুটির তালিকা */}
              <Card className="p-6">
                <div className="flex items-center justify-between border-b border-gray-100 pb-3">
                  <h3 className="text-base font-semibold text-gray-900">আসন্ন ছুটির দিন</h3>
                  <Link to="/holidays" className="text-xs font-medium text-indigo-600 hover:text-indigo-800">
                    ছুটির ক্যালেন্ডার →
                  </Link>
                </div>

                {holidays.length === 0 ? (
                  <p className="py-6 text-center text-sm text-gray-500">সামনে কোনো নির্ধারিত ছুটি নেই।</p>
                ) : (
                  <ul className="mt-3 divide-y divide-gray-100 text-sm">
                    {holidays.map((h) => (
                      <li key={h.id} className="flex items-center justify-between py-3">
                        <div>
                          <p className="font-medium text-gray-900">{h.name}</p>
                          <p className="text-xs text-gray-500">{formatDate(h.holidayDate)}</p>
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
