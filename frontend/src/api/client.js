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

// সব অনুরোধ এই এক জায়গা দিয়ে যায়
//   form = true  : body হলো FormData (ছবি আপলোড)
//   as = 'blob'  : উত্তর ফাইল হিসেবে আনা (ছবি দেখা)
async function request(path, { method = 'GET', body, form = false, as = 'json', auth = true, retry = true } = {}) {
  const headers = {}
  let payload
  if (body !== undefined) {
    if (form) {
      payload = body // FormData: ব্রাউজার নিজে Content-Type (boundary-সহ) বসায়
    } else {
      headers['Content-Type'] = 'application/json'
      payload = JSON.stringify(body)
    }
  }
  if (auth && accessToken) {
    headers.Authorization = `Bearer ${accessToken}`
  }

  const res = await fetch(path, {
    method,
    headers,
    credentials: 'same-origin',
    body: payload,
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
    return request(path, { method, body, form, as, auth, retry: false })
  }

  if (!res.ok) {
    throw await parseError(res)
  }
  if (res.status === 204) {
    return null
  }
  return as === 'blob' ? res.blob() : res.json()
}

export function api(path, options) {
  return request(path, options)
}

export function apiUpload(path, formData) {
  return request(path, { method: 'POST', body: formData, form: true })
}

export function apiBlob(path) {
  return request(path, { as: 'blob' })
}