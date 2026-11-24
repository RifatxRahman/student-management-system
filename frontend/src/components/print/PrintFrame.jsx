import '@fontsource/noto-sans-bengali/400.css'
import '@fontsource/noto-sans-bengali/600.css'
import '@fontsource/noto-sans-bengali/700.css'
import { Link } from 'react-router'

const FONT = "'Noto Sans Bengali', 'Nirmala UI', sans-serif"

// orientation: 'portrait' (মার্কশিট) অথবা 'landscape' (রেজাল্ট শিট)
export default function PrintFrame({ title, hint, orientation = 'portrait', backTo, children }) {
  return (
    <div className="min-h-screen bg-[var(--canvas)] py-6 print:bg-white print:py-0" style={{ fontFamily: FONT }}>
      <style>{`@page { size: A4 ${orientation}; margin: 10mm; }`}</style>

      <div className="mx-auto mb-4 flex max-w-[297mm] flex-wrap items-center justify-between gap-3 px-4 print:hidden">
        <div>
          <h1 className="text-lg font-semibold text-[var(--ink-primary)]">{title}</h1>
          {hint && <p className="text-sm text-[var(--ink-secondary)]">{hint}</p>}
        </div>
        <div className="flex gap-2">
          {backTo && (
            <Link
              to={backTo}
              className="rounded-[10px] bg-[var(--surface-subtle)] px-4 py-2 text-sm font-medium text-[var(--ink-primary)] border border-[var(--border-hairline)] hover:bg-[var(--surface-hover)] transition min-h-[40px]"
            >
              ← ফিরে যান
            </Link>
          )}
          <button
            type="button"
            onClick={() => window.print()}
            className="rounded-[10px] bg-[#E8A317] px-4 py-2 text-sm font-semibold text-[#12231D] hover:bg-[#D49312] transition shadow-sm min-h-[40px]"
          >
            প্রিন্ট / PDF করুন
          </button>
        </div>
      </div>

      {children}
    </div>
  )
}