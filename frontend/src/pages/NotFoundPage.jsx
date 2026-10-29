import { Link } from 'react-router'

export default function NotFoundPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-gray-50 px-4 text-center">
      <p className="text-5xl font-semibold text-gray-300">404</p>
      <h1 className="mt-4 text-xl font-semibold text-gray-900">পাতাটি পাওয়া যায়নি</h1>
      <Link to="/" className="mt-4 text-sm font-medium text-indigo-600 hover:text-indigo-700">
        ← ড্যাশবোর্ডে ফিরুন
      </Link>
    </div>
  )
}