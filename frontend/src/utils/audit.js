const ACTION_LABELS = {
  STUDENT_CREATED: 'শিক্ষার্থী তৈরি',
  STUDENT_UPDATED: 'শিক্ষার্থীর তথ্য সম্পাদনা',
  STUDENT_STATUS_CHANGED: 'শিক্ষার্থীর অবস্থা বদল',
  STUDENT_DELETED: 'শিক্ষার্থী মোছা',
  STUDENT_RESTORED: 'শিক্ষার্থী ফিরিয়ে আনা',
  STUDENT_PHOTO_CHANGED: 'শিক্ষার্থীর ছবি বদল',
  STUDENT_PHOTO_REMOVED: 'শিক্ষার্থীর ছবি সরানো',
  ENROLLED: 'ভর্তি',
  TRANSFERRED: 'বদলি',
  WITHDRAWN: 'ছাড়পত্র',
  GUARDIAN_CREATED: 'অভিভাবক তৈরি',
  GUARDIAN_UPDATED: 'অভিভাবকের তথ্য সম্পাদনা',
  GUARDIAN_LINKED: 'অভিভাবক যুক্ত',
  GUARDIAN_UNLINKED: 'অভিভাবক সরানো',
  PRIMARY_GUARDIAN_CHANGED: 'প্রধান অভিভাবক বদল',
  TEACHER_CREATED: 'শিক্ষক তৈরি',
  TEACHER_UPDATED: 'শিক্ষকের তথ্য সম্পাদনা',
  TEACHER_ACTIVATED: 'শিক্ষক সক্রিয়',
  TEACHER_DEACTIVATED: 'শিক্ষক নিষ্ক্রিয়',
  ASSIGNMENT_ADDED: 'বিষয় বরাদ্দ',
  ASSIGNMENT_REMOVED: 'বরাদ্দ সরানো',
  ATTENDANCE_MARKED: 'হাজিরা নেওয়া',
  ATTENDANCE_CORRECTED: 'হাজিরা সংশোধন',
  HOLIDAY_ADDED: 'ছুটি যোগ',
  HOLIDAY_REMOVED: 'ছুটি মোছা',
  NOTIFICATION_DISPATCHED: 'বার্তা পাঠানো',
  NOTIFICATION_RETRY: 'বার্তা আবার চেষ্টা',
  ADMIN_USER_ACTION: 'ইউজার-সংক্রান্ত কাজ',
}

export function actionLabel(action) {
  return ACTION_LABELS[action] ?? action
}

// অডিট লগ ছাঁকার জন্য বিষয়ের ধরন
export const ENTITY_TYPE_OPTIONS = [
  { value: '', label: 'সব' },
  { value: 'STUDENT', label: 'শিক্ষার্থী' },
  { value: 'GUARDIAN', label: 'অভিভাবক' },
  { value: 'TEACHER', label: 'শিক্ষক' },
  { value: 'ATTENDANCE', label: 'হাজিরা (সংশোধন)' },
  { value: 'SECTION', label: 'সেকশন (হাজিরা নেওয়া)' },
  { value: 'HOLIDAY', label: 'ছুটি' },
  { value: 'NOTIFICATION', label: 'বার্তা' },
  { value: 'USER', label: 'ইউজার' },
]