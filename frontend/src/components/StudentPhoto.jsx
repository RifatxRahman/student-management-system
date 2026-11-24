import { useEffect, useState } from 'react'
import { apiBlob } from '../api/client.js'

// ছবি বদলের পর নতুন ছবি আনতে মা-পাতা এই কম্পোনেন্টে নতুন key দেয় (আবার শুরু থেকে লোড হয়)
export default function StudentPhoto({ studentId, name, className = 'h-28 w-28' }) {
  const [photo, setPhoto] = useState({ status: 'loading', url: null })

  useEffect(() => {
    let cancelled = false
    let objectUrl = null
    apiBlob(`/api/students/${studentId}/photo`)
      .then((blob) => {
        if (cancelled) return
        objectUrl = URL.createObjectURL(blob)
        setPhoto({ status: 'ready', url: objectUrl })
      })
      .catch(() => {
        if (!cancelled) setPhoto({ status: 'none', url: null })
      })
    return () => {
      cancelled = true
      if (objectUrl) URL.revokeObjectURL(objectUrl)
    }
  }, [studentId])

  if (photo.status === 'ready') {
    return (
      <img
        src={photo.url}
        alt={`${name}-এর ছবি`}
        loading="lazy"
        className={`${className} rounded-full object-cover ring-1 ring-[var(--border-hairline)]`}
      />
    )
  }

  const initial = name?.trim()?.[0] ?? '?'
  return (
    <div
      aria-label={photo.status === 'loading' ? 'ছবি লোড হচ্ছে' : 'ছবি নেই'}
      className={`${className} flex items-center justify-center rounded-full bg-[#0E3B2E]/10 text-3xl font-semibold text-[#0E3B2E] border border-[#0E3B2E]/15`}
    >
      {initial}
    </div>
  )
}