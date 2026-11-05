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



// ---------- তারিখ ও সময় ----------
function pad(number) {
  return String(number).padStart(2, '0')
}

// ব্রাউজারের স্থানীয় তারিখ -> "2026-11-02"
export function toISODate(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

export function todayISO() {
  return toISODate(new Date())
}

// "2026-11-02" -> "সোমবার"
export function formatWeekday(iso) {
  const [year, month, day] = iso.split('-').map(Number)
  return new Date(year, month - 1, day).toLocaleDateString('bn-BD', { weekday: 'long' })
}

// "2026-10" -> "অক্টোবর ২০২৬"
export function formatMonth(yearMonth) {
  const [year, month] = yearMonth.split('-').map(Number)
  return new Date(year, month - 1, 1).toLocaleDateString('bn-BD', { year: 'numeric', month: 'long' })
}

// সার্ভারের সময় (ISO) -> ঢাকার সময়ে তারিখ ও সময়
export function formatDateTime(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('bn-BD', {
    timeZone: 'Asia/Dhaka',
    dateStyle: 'medium',
    timeStyle: 'short',
  })
}

// ---------- হাজিরার অবস্থা ----------
export const ATTENDANCE_STATUSES = [
  { value: 'PRESENT', label: 'উপস্থিত' },
  { value: 'ABSENT', label: 'অনুপস্থিত' },
  { value: 'LATE', label: 'দেরি' },
  { value: 'LEAVE', label: 'ছুটি' },
]


// ---------- মার্কশিটের জন্য ----------
const BANGLA_DIGITS = ['০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯']

// "2026-00001" -> "২০২৬-০০০০১"
export function toBanglaDigits(text) {
  return String(text ?? '').replace(/[0-9]/g, (digit) => BANGLA_DIGITS[Number(digit)])
}

// 82 -> "৮২", 82.5 -> "৮২.৫", (4.5, 2, 2) -> "৪.৫০"
export function bnNumber(value, minimumFractionDigits = 0, maximumFractionDigits = 2) {
  if (value === null || value === undefined) return '—'
  return Number(value).toLocaleString('bn-BD', {
    minimumFractionDigits,
    maximumFractionDigits: Math.max(minimumFractionDigits, maximumFractionDigits),
  })
}

const EXAM_TYPE_LABELS = { MIDTERM: 'মধ্যবর্তী পরীক্ষা', FINAL: 'চূড়ান্ত পরীক্ষা', TEST: 'ক্লাস টেস্ট' }

export function examTypeLabel(type) {
  return EXAM_TYPE_LABELS[type] ?? type
}

// সার্ভারের সময় (ISO) -> ঢাকার তারিখ ("২ নভেম্বর ২০২৬")
export function formatInstantDate(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString('bn-BD', {
    timeZone: 'Asia/Dhaka',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}


// ---------- পরীক্ষা (অ্যাডমিন স্ক্রিনের জন্য) ----------
export const EXAM_TYPE_OPTIONS = [
  { value: 'MIDTERM', label: 'মধ্যবর্তী পরীক্ষা' },
  { value: 'FINAL', label: 'চূড়ান্ত পরীক্ষা' },
  { value: 'TEST', label: 'ক্লাস টেস্ট' },
]

export function examStatusInfo(status) {
  return status === 'PUBLISHED' ? { label: 'প্রকাশিত', tone: 'green' } : { label: 'খসড়া', tone: 'amber' }
}

// 100.00 -> "100", 82.5 -> "82.5", null -> "—"
export function plain(value) {
  return value === null || value === undefined ? '—' : String(Number(value))
}


// ---------- রুটিন ----------
// বাংলাদেশের সপ্তাহের ক্রম: শনিবার থেকে শুক্রবার
export const WEEK_DAYS = ['SATURDAY', 'SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY']

const DAY_LABELS = {
  SATURDAY: 'শনিবার',
  SUNDAY: 'রবিবার',
  MONDAY: 'সোমবার',
  TUESDAY: 'মঙ্গলবার',
  WEDNESDAY: 'বুধবার',
  THURSDAY: 'বৃহস্পতিবার',
  FRIDAY: 'শুক্রবার',
}

export function dayLabel(day) {
  return DAY_LABELS[day] ?? day
}

// "09:00:00" -> "০৯:০০"
export function shortTime(time) {
  return time ? toBanglaDigits(time.slice(0, 5)) : ''
}

// "09:00:00" -> "09:00" (সময়ের ঘরে বসানোর জন্য)
export function timeInputValue(time) {
  return time ? time.slice(0, 5) : ''
}