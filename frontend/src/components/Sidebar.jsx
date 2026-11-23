import { NavLink } from 'react-router'
import { useAuth } from '../auth/useAuth.js'
import { roleLabel } from '../utils/format.js'
import { Badge, Button } from './ui.jsx'

const LINKS = [
  { to: '/', label: 'ড্যাশবোর্ড', end: true },
  { to: '/attendance', label: 'হাজিরা', end: true, roles: ['ADMIN', 'TEACHER'] },
  { to: '/attendance/report', label: 'মাসিক রিপোর্ট', roles: ['ADMIN', 'TEACHER'] },
  { to: '/routine', label: 'ক্লাস রুটিন' },
  { to: '/calendar', label: 'একাডেমিক ক্যালেন্ডার' },
  { to: '/holidays', label: 'ছুটির দিন' },
  { to: '/exams', label: 'পরীক্ষা ও ফল' },
  { to: '/grade-scale', label: 'গ্রেড স্কেল' },
  { to: '/sessions', label: 'শিক্ষাবর্ষ (সেশন)' },
  { to: '/classes', label: 'ক্লাস ও সেকশন' },
  { to: '/subjects', label: 'বিষয়' },
  { to: '/students', label: 'শিক্ষার্থী', roles: ['ADMIN'] },
  { to: '/promotion', label: 'সেশন প্রমোশন', roles: ['ADMIN'] },
  { to: '/guardians', label: 'অভিভাবক', roles: ['ADMIN'] },
  { to: '/teachers', label: 'শিক্ষক', roles: ['ADMIN'] },
  { to: '/teacher-attendance', label: 'শিক্ষক হাজিরা', roles: ['ADMIN', 'TEACHER'] },
  { to: '/leaves', label: 'ছুটির আবেদন', roles: ['ADMIN', 'TEACHER'] },
  { to: '/fee-structure', label: 'ফি সেটআপ ও খাত', roles: ['ADMIN'] },
  { to: '/fee-collection', label: 'ফি আদায় ও ইনভয়েস', roles: ['ADMIN'] },
  { to: '/accounts', label: 'ব্যয় ও হিসাব বিবরণী', roles: ['ADMIN'] },
  { to: '/notifications', label: 'মেসেজ ও নোটিফিকেশন', roles: ['ADMIN'] },
  { to: '/audit-logs', label: 'অডিট লগ', roles: ['ADMIN'] },
  { to: '/users', label: 'ইউজার', roles: ['ADMIN'] },
]

function linkClass({ isActive }) {
  return `flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm font-medium transition ${
    isActive
      ? 'bg-indigo-600 text-white shadow-sm'
      : 'text-slate-300 hover:bg-slate-800 hover:text-white'
  }`
}

export default function Sidebar({ open, onClose }) {
  const { user, logout } = useAuth()
  const items = LINKS.filter((link) => !link.roles || link.roles.includes(user.role))

  return (
    <>
      {open && <div className="fixed inset-0 z-30 bg-black/50 backdrop-blur-sm lg:hidden" onClick={onClose} />}

      <aside
        className={`fixed inset-y-0 left-0 z-40 flex w-64 flex-col border-r border-slate-800 bg-slate-900 text-slate-200 transition-transform lg:translate-x-0 ${
          open ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <div className="flex items-center gap-3 border-b border-slate-800 px-6 py-5">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-tr from-indigo-600 to-violet-500 text-white shadow-md shadow-indigo-500/20">
            <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 14l9-5-9-5-9 5 9 5z" />
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 14l6.16-3.422a12.083 12.083 0 01.665 6.479A11.952 11.952 0 0012 20.055a11.952 11.952 0 00-6.824-2.998 12.078 12.078 0 01.665-6.479L12 14z" />
            </svg>
          </div>
          <div>
            <p className="text-base font-bold tracking-tight text-white">School Management</p>
            <p className="text-xs text-slate-400">স্কুল পরিচালনা ব্যবস্থা</p>
          </div>
        </div>

        <nav className="flex-1 space-y-1 overflow-y-auto px-3 py-4">
          {items.map((item) => (
            <NavLink key={item.to} to={item.to} end={item.end} onClick={onClose} className={linkClass}>
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="space-y-3 border-t border-slate-800 bg-slate-950/60 p-4">
          <div>
            <p className="truncate text-sm font-medium text-white">{user.fullName}</p>
            <div className="mt-1 flex items-center gap-2">
              <Badge tone="indigo">{roleLabel(user.role)}</Badge>
              <span className="truncate text-xs text-slate-400 font-mono">{user.username}</span>
            </div>
          </div>
          <NavLink
            to="/change-password"
            onClick={onClose}
            className="block text-xs text-slate-400 hover:text-white transition"
          >
            পাসওয়ার্ড বদলান
          </NavLink>
          <Button variant="secondary" size="sm" className="w-full bg-slate-800 hover:bg-slate-700 text-slate-200 border-slate-700" onClick={logout}>
            লগআউট
          </Button>
        </div>
      </aside>
    </>
  )
}