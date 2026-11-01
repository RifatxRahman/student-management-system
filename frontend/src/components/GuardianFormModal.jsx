import { api } from '../api/client.js'
import { blankToNull } from '../utils/format.js'
import EntityForm from './EntityForm.jsx'
import { Modal } from './ui.jsx'

const FIELDS = [
  { name: 'fullName', label: 'পুরো নাম', required: true },
  { name: 'phone', label: 'ফোন নম্বর', hint: 'যেমন: 01712345678', required: true },
  { name: 'altPhone', label: 'বিকল্প ফোন (ঐচ্ছিক)' },
  { name: 'email', label: 'ইমেইল (ঐচ্ছিক)', type: 'email' },
  { name: 'address', label: 'ঠিকানা (ঐচ্ছিক)' },
]

// guardian থাকলে সম্পাদনা, না থাকলে নতুন। সংরক্ষণ হলে onSaved(অভিভাবক) ডাকে।
export default function GuardianFormModal({ guardian, onSaved, onClose }) {
  async function save(values) {
    const body = blankToNull(values)
    const saved = guardian
      ? await api(`/api/guardians/${guardian.id}`, { method: 'PUT', body })
      : await api('/api/guardians', { method: 'POST', body })
    onSaved(saved)
  }

  return (
    <Modal title={guardian ? 'অভিভাবকের তথ্য সম্পাদনা' : 'নতুন অভিভাবক'} onClose={onClose}>
      <EntityForm
        fields={FIELDS}
        initial={guardian}
        submitLabel="সংরক্ষণ করুন"
        onSubmit={save}
        onCancel={onClose}
      />
    </Modal>
  )
}