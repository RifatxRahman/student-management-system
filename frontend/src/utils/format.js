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