import { useEffect, useState } from 'react'

function App() {
  const [status, setStatus] = useState('লোড হচ্ছে...')

  useEffect(() => {
    fetch('/api/ping')
      .then((res) => res.json())
      .then((data) => setStatus(data.status))
      .catch(() => setStatus('সংযোগ ব্যর্থ'))
  }, [])

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="bg-white rounded-xl shadow p-8 text-center">
        <h1 className="text-2xl font-semibold text-gray-800">
          Student Management System
        </h1>
        <p className="mt-3 text-gray-600">
          Backend অবস্থা: <span className="font-medium">{status}</span>
        </p>
      </div>
    </div>
  )
}

export default App