import { Link } from 'react-router'

export default function NotFoundPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-[var(--canvas)] px-4 text-center">
      <p className="text-5xl font-bold text-[var(--ink-tertiary)] font-['Anek_Bangla',sans-serif]">404</p>
      <h1 className="mt-4 text-xl font-bold text-[var(--ink-primary)] font-['Anek_Bangla',sans-serif]">পাতাটি পাওয়া যায়নি</h1>
      <Link to="/" className="mt-4 text-sm font-semibold text-[#0E3B2E] dark:text-[#E8A317] hover:underline">
        ← ড্যাশবোর্ডে ফিরুন
      </Link>
    </div>
  )
}