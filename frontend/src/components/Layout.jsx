import { useState } from 'react'
import { Outlet } from 'react-router'
import Sidebar from './Sidebar.jsx'

export default function Layout() {
  const [open, setOpen] = useState(false)

  return (
    <div className="min-h-screen bg-[var(--canvas)] text-[var(--ink-primary)]">
      <Sidebar open={open} onClose={() => setOpen(false)} />

      <div className="lg:pl-[264px]">
        <header className="sticky top-0 z-20 flex items-center gap-3 border-b border-[var(--border-hairline)] bg-[var(--surface)] px-4 py-3 lg:hidden">
          <button
            type="button"
            aria-label="মেনু খুলুন"
            className="rounded-[10px] px-2.5 py-1 text-xl text-[var(--ink-primary)] hover:bg-[var(--surface-hover)] focus-ring-apple"
            onClick={() => setOpen(true)}
          >
            ☰
          </button>
          <span className="font-bold text-[var(--ink-primary)] font-['Anek_Bangla',sans-serif]">Student Management</span>
        </header>

        <main className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}