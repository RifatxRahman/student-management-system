import { useState } from 'react'
import { useDebounce } from '../hooks/useDebounce.js'
import { useQuery } from '../hooks/useQuery.js'
import { Button, LoadState, TextField } from './ui.jsx'

export default function GuardianPicker({ onSelect }) {
  const [term, setTerm] = useState('')
  const debounced = useDebounce(term.trim())
  const search = useQuery(debounced.length >= 2 ? `/api/guardians?q=${encodeURIComponent(debounced)}` : null)

  return (
    <div className="space-y-3">
      <TextField
        label="অভিভাবক খুঁজুন"
        hint="নাম বা ফোন নম্বরের কমপক্ষে ২ অক্ষর লিখুন"
        value={term}
        onChange={(event) => setTerm(event.target.value)}
      />
      {debounced.length >= 2 && (
        <LoadState loading={search.loading} error={search.error}>
          {search.data?.length === 0 ? (
            <p className="text-sm text-gray-500">কাউকে পাওয়া যায়নি।</p>
          ) : (
            <ul className="divide-y divide-gray-100 rounded-lg border border-gray-200">
              {search.data?.map((g) => (
                <li key={g.id} className="flex items-center justify-between gap-3 px-3 py-2">
                  <div>
                    <p className="text-sm font-medium text-gray-900">{g.fullName}</p>
                    <p className="text-xs text-gray-500">{g.phone}</p>
                  </div>
                  <Button size="sm" variant="secondary" onClick={() => onSelect(g)}>
                    বাছুন
                  </Button>
                </li>
              ))}
            </ul>
          )}
        </LoadState>
      )}
    </div>
  )
}