import { NavLink } from 'react-router'
import { useAuth } from '../auth/useAuth.js'
import { roleLabel } from '../utils/format.js'
import { Badge, Button } from './ui.jsx'

const LINKS = [
  { to: '/', label: 'ড্যাশবোর্ড', end: true },
  { to: '/sessions', label: 'শিক্ষাবর্ষ (সেশন)' },
  { to: '/classes', label: 'ক্লাস ও সেকশন' },
  { to: '/subjects', label: 'বিষয়' },
  { to: '/users', label: 'ইউজার', roles: ['ADMIN'] },
]

function linkClass({ isActive }) {
  return `block rounded-lg px-3 py-2 text-sm font-medium transition ${
    isActive ? 'bg-indigo-50 text-indigo-700' : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
  }`
}

export default function Sidebar({ open, onClose }) {
  const { user, logout } = useAuth()
  const items = LINKS.filter((link) => !link.roles || link.roles.includes(user.role))

  return (
    <>
      {open && <div className="fixed inset-0 z-30 bg-black/30 lg:hidden" onClick={onClose} />}

      <aside
        className={`fixed inset-y-0 left-0 z-40 flex w-64 flex-col border-r border-gray-200 bg-white transition-transform lg:translate-x-0 ${
          open ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <div className="px-6 py-6">
          <p className="text-lg font-semibold text-gray-900">Student Management</p>
          <p className="text-xs text-gray-500">স্কুল পরিচালনা ব্যবস্থা</p>
        </div>

        <nav className="flex-1 space-y-1 px-3">
          {items.map((item) => (
            <NavLink key={item.to} to={item.to} end={item.end} onClick={onClose} className={linkClass}>
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="space-y-3 border-t border-gray-200 p-4">
          <div>
            <p className="truncate text-sm font-medium text-gray-900">{user.fullName}</p>
            <div className="mt-1 flex items-center gap-2">
              <Badge tone="indigo">{roleLabel(user.role)}</Badge>
              <span className="truncate text-xs text-gray-500">{user.username}</span>
            </div>
          </div>
          <NavLink to="/change-password" onClick={onClose} className={linkClass}>
            পাসওয়ার্ড বদলান
          </NavLink>
          <Button variant="secondary" size="sm" className="w-full" onClick={logout}>
            লগআউট
          </Button>
        </div>
      </aside>
    </>
  )
}