const ACTION_LABELS = {
  // Authentication & Security
  AUTH_LOGIN_SUCCESS: 'লগইন সফল',
  AUTH_LOGIN_FAILED: 'লগইন ব্যর্থ',
  AUTH_ACCOUNT_LOCKED: 'অ্যাকাউন্ট সাময়িক লক',
  AUTH_LOGOUT: 'লগআউট',
  AUTH_PASSWORD_CHANGED: 'পাসওয়ার্ড বদল',
  AUTH_PASSWORD_CHANGE_FAILED: 'পাসওয়ার্ড বদল ব্যর্থ',
  SECURITY_TOKEN_THEFT_DETECTED: 'টোকেন চুরির সন্দেহ শনাক্ত',

  // RBAC & Users
  ADMIN_USER_ACTION: 'ইউজার প্রশাসন',
  RBAC_USER_OVERRIDE: 'ইউজার পারমিশন ওভাররাইড',
  RBAC_ROLE_MATRIX_UPDATE: 'রোল পারমিশন ম্যাট্রিক্স আপডেট',
  ROLE_PERMISSIONS_UPDATED: 'রোল পারমিশন বদল',
  USER_PERMISSIONS_OVERRIDDEN: 'কাস্টম পারমিশন ওভাররাইড',

  // Students & Guardians
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

  // Teachers & Attendance
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

  // Finance & Accounts
  FEE_HEAD_CREATED: 'ফি খাত তৈরি',
  FEE_STRUCTURE_CREATED: 'ফি স্ট্রাকচার তৈরি',
  INVOICE_GENERATED: 'ইনভয়েস তৈরি',
  FEE_PAYMENT_COLLECTED: 'ফি আদায়',
  EXPENSE_RECORDED: 'ব্যয় এন্ট্রি',

  // Notifications
  NOTIFICATION_DISPATCHED: 'বার্তা পাঠানো',
  NOTIFICATION_RETRY: 'বার্তা আবার চেষ্টা',
  NOTIFICATION_CAMPAIGN_DISPATCHED: 'ক্যাম্পেইন প্রেরণ',
}

export function actionLabel(action) {
  return ACTION_LABELS[action] ?? action
}

// অডিট লগ ছাঁকার জন্য বিষয়ের ধরন
export const ENTITY_TYPE_OPTIONS = [
  { value: '', label: 'সব বিষয়' },
  { value: 'USER', label: 'ব্যবহারকারী / নিরাপত্তা' },
  { value: 'ROLE', label: 'রোল ও পারমিশন' },
  { value: 'STUDENT', label: 'শিক্ষার্থী' },
  { value: 'GUARDIAN', label: 'অভিভাবক' },
  { value: 'TEACHER', label: 'শিক্ষক' },
  { value: 'ATTENDANCE', label: 'হাজিরা' },
  { value: 'SECTION', label: 'সেকশন' },
  { value: 'HOLIDAY', label: 'ছুটি' },
  { value: 'INVOICE', label: 'ফি ইনভয়েস' },
  { value: 'EXPENSE', label: 'ব্যয়' },
  { value: 'CAMPAIGN', label: 'বার্তা ক্যাম্পেইন' },
]

export const STATUS_OPTIONS = [
  { value: '', label: 'সব অবস্থা' },
  { value: 'SUCCESS', label: 'সফল (SUCCESS)' },
  { value: 'FAILED', label: 'ব্যর্থ / সতর্কবার্তা (FAILED)' },
]
