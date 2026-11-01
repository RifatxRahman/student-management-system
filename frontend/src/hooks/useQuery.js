import { useCallback, useEffect, useState } from 'react'
import { api } from '../api/client.js'

// path = null হলে কিছু আনে না (loading = false, data = null)
export function useQuery(path) {
  const [result, setResult] = useState({ path: null, data: null, error: null })
  const [tick, setTick] = useState(0)

  useEffect(() => {
    if (!path) return undefined
    let cancelled = false
    api(path)
      .then((data) => {
        if (!cancelled) setResult({ path, data, error: null })
      })
      .catch((error) => {
        if (!cancelled) setResult({ path, data: null, error })
      })
    return () => {
      cancelled = true
    }
  }, [path, tick])

  const reload = useCallback(() => setTick((t) => t + 1), [])

  // result অন্য ঠিকানার হলে সেটা দেখাই না
  const current = result.path === path
  return {
    data: current ? result.data : null,
    error: current ? result.error : null,
    loading: Boolean(path) && !current,
    reload,
  }
}