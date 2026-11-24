import { useEffect, useState } from 'react'
import { apiBlob } from '../api/client.js'

export default function TeacherPhoto({ teacherId, name, className = 'h-28 w-28' }) {
  const [photo, setPhoto] = useState({ status: 'loading', url: null })

  useEffect(() => {
    let cancelled = false
    let objectUrl = null
    apiBlob(`/api/teachers/${teacherId}/photo`)
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
  }, [teacherId])

  if (photo.status === 'ready') {
    return (
      <img
        src={photo.url}
        alt={`${name}-এর ছবি`}
        loading="lazy"
        className={`${className} rounded-full object-cover ring-1 ring-[var(--border-hairline)] shadow-sm`}
      />
    )
  }

  const initial = name?.trim()?.[0] ?? '?'
  return (
    <div
      aria-label={photo.status === 'loading' ? 'ছবি লোড হচ্ছে' : 'ছবি নেই'}
      className={`${className} flex items-center justify-center rounded-full bg-[#0E3B2E]/10 text-3xl font-semibold text-[#0E3B2E] ring-1 ring-[#0E3B2E]/20 shadow-sm`}
    >
      {initial}
    </div>
  )
}
