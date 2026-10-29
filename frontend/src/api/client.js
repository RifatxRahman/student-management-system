let accessToken = null
let unauthorizedHandler = () => {}
let refreshPromise = null

export class ApiError extends Error {
  constructor(status, message, fieldErrors) {
    super(message)
    this.status = status
    this.fieldErrors = fieldErrors || {}
  }
}

export function setAccessToken(token) {
  accessToken = token
}

export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = handler
}

async function parseError(res) {
  let body = null
  try {
    body = await res.json()
  } catch {
    // উত্তরে কোনো লেখা নেই
  }
  return new ApiError(
    res.status,
    body?.message || 'কিছু একটা সমস্যা হয়েছে, আবার চেষ্টা করুন',
    body?.fieldErrors,
  )
}

async function doRefresh() {
  const res = await fetch('/api/auth/refresh', {
    method: 'POST',
    credentials: 'same-origin',
  })
  if (!res.ok) {
    throw await parseError(res)
  }
  const data = await res.json()
  accessToken = data.accessToken
  return data
}

// একসাথে দুবার ডাকা হলেও আসল অনুরোধ যায় একটাই
// (নইলে Backend-এর "পুরনো চাবি আবার এসেছে" সুরক্ষা ভুল করে লগআউট করে দিত)
export function refreshSession() {
  if (!refreshPromise) {
    refreshPromise = doRefresh().finally(() => {
      refreshPromise = null
    })
  }
  return refreshPromise
}

export async function api(path, { method = 'GET', body, auth = true, retry = true } = {}) {
  const headers = {}
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  if (auth && accessToken) {
    headers.Authorization = `Bearer ${accessToken}`
  }

  const res = await fetch(path, {
    method,
    headers,
    credentials: 'same-origin',
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })

  // টোকেনের মেয়াদ শেষ হলে একবার নতুন টোকেন এনে আবার চেষ্টা
  if (res.status === 401 && auth && retry && !path.startsWith('/api/auth/')) {
    try {
      await refreshSession()
    } catch {
      accessToken = null
      unauthorizedHandler()
      throw new ApiError(401, 'সেশন শেষ হয়েছে, আবার লগইন করুন')
    }
    return api(path, { method, body, auth, retry: false })
  }

  if (!res.ok) {
    throw await parseError(res)
  }
  if (res.status === 204) {
    return null
  }
  return res.json()
}