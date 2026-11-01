import { GENDER_OPTIONS } from './format.js'

// EntityForm-এর ঘরের তালিকা (নতুন ভর্তি ও সম্পাদনা দুই জায়গায় একই)
export const STUDENT_FIELDS = [
  { name: 'fullName', label: 'পুরো নাম', required: true },
  { name: 'dateOfBirth', label: 'জন্মতারিখ', type: 'date', required: true },
  { name: 'gender', label: 'লিঙ্গ', required: true, options: GENDER_OPTIONS },
  { name: 'phone', label: 'ফোন নম্বর (ঐচ্ছিক)', hint: 'যেমন: 01712345678' },
  { name: 'address', label: 'ঠিকানা (ঐচ্ছিক)' },
]