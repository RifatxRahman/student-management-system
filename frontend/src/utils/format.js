const ROLE_LABELS = {
  ADMIN: 'অ্যাডমিন',
  TEACHER: 'শিক্ষক',
}

export function roleLabel(role) {
  return ROLE_LABELS[role] ?? role
}

// "2026-01-15" -> বাংলায় তারিখ (সময়-অঞ্চলের গোলমাল এড়াতে হাতে ভাঙা হয়েছে)
export function formatDate(iso) {
  const [year, month, day] = iso.split('-').map(Number)
  return new Date(year, month - 1, day).toLocaleDateString('bn-BD', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}

// তারিখ না থাকলে "—"
export function formatDateOrDash(iso) {
  return iso ? formatDate(iso) : '—'
}

// ফর্মের ফাঁকা লেখা ('') কে null বানায় (Backend-এ ঐচ্ছিক ঘর null হিসেবেই ভালো)
export function blankToNull(values) {
  return Object.fromEntries(
    Object.entries(values).map(([key, value]) => [
      key,
      typeof value === 'string' && value.trim() === '' ? null : value,
    ]),
  )
}

// ---------- লিঙ্গ ----------
export const GENDER_OPTIONS = [
  { value: '', label: 'বাছুন' },
  { value: 'MALE', label: 'ছেলে' },
  { value: 'FEMALE', label: 'মেয়ে' },
  { value: 'OTHER', label: 'অন্য' },
]

export function genderLabel(gender) {
  return GENDER_OPTIONS.find((option) => option.value === gender)?.label ?? '—'
}

// ---------- শিক্ষার্থীর অবস্থা ----------
export const STUDENT_STATUS_OPTIONS = [
  { value: 'ACTIVE', label: 'সক্রিয়' },
  { value: 'INACTIVE', label: 'নিষ্ক্রিয়' },
  { value: 'GRADUATED', label: 'গ্র্যাজুয়েট' },
]

const STUDENT_STATUS_TONES = { ACTIVE: 'green', INACTIVE: 'gray', GRADUATED: 'indigo' }

export function studentStatusInfo(status) {
  const option = STUDENT_STATUS_OPTIONS.find((o) => o.value === status)
  return { label: option?.label ?? status, tone: STUDENT_STATUS_TONES[status] ?? 'gray' }
}

// ---------- অভিভাবকের সম্পর্ক ----------
export const RELATION_OPTIONS = [
  { value: 'FATHER', label: 'বাবা' },
  { value: 'MOTHER', label: 'মা' },
  { value: 'GUARDIAN', label: 'অভিভাবক' },
  { value: 'OTHER', label: 'অন্য' },
]

export function relationLabel(relation) {
  return RELATION_OPTIONS.find((option) => option.value === relation)?.label ?? relation
}

// ---------- ভর্তির অবস্থা ----------
const ENROLLMENT_STATUS = {
  ACTIVE: { label: 'চলমান', tone: 'green' },
  TRANSFERRED: { label: 'বদলি হয়েছে', tone: 'amber' },
  WITHDRAWN: { label: 'ছাড়পত্র দেওয়া হয়েছে', tone: 'gray' },
  COMPLETED: { label: 'সম্পন্ন', tone: 'indigo' },
}

export function enrollmentStatusInfo(status) {
  return ENROLLMENT_STATUS[status] ?? { label: status, tone: 'gray' }
}