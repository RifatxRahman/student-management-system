import { useState } from 'react'
import { Alert, Button, Modal } from './ui.jsx'

// আগের / পরের পাতা। totalPages <= 1 হলে শুধু মোট সংখ্যা দেখায়।
export function Pagination({ page, totalPages, totalElements, unit = 'জন', onChange }) {
  return (
    <div className="flex items-center justify-between border-t border-gray-100 px-4 py-3 text-sm text-gray-600">
      <span>
        মোট {totalElements} {unit}
      </span>
      {totalPages > 1 && (
        <div className="flex items-center gap-2">
          <Button variant="secondary" size="sm" disabled={page === 0} onClick={() => onChange(page - 1)}>
            আগের
          </Button>
          <span>
            {page + 1} / {totalPages}
          </span>
          <Button variant="secondary" size="sm" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>
            পরের
          </Button>
        </div>
      )}
    </div>
  )
}

// "আপনি কি নিশ্চিত?" জানালা। onConfirm সফল হলে বন্ধ করা মা-পাতার কাজ।
export function ConfirmModal({ title, message, confirmLabel = 'নিশ্চিত', danger = false, onConfirm, onClose }) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  async function confirm() {
    setBusy(true)
    setError('')
    try {
      await onConfirm()
    } catch (err) {
      setError(err.message)
      setBusy(false)
    }
  }

  return (
    <Modal title={title} onClose={onClose}>
      <div className="space-y-4">
        <p className="text-sm text-gray-600">{message}</p>
        {error && <Alert>{error}</Alert>}
        <div className="flex justify-end gap-2">
          <Button variant="secondary" onClick={onClose} disabled={busy}>
            বাতিল
          </Button>
          <Button variant={danger ? 'danger' : 'primary'} onClick={confirm} disabled={busy}>
            {busy ? 'অপেক্ষা করুন...' : confirmLabel}
          </Button>
        </div>
      </div>
    </Modal>
  )
}

// "নাম ........ মান" এক সারি
export function InfoRow({ label, children }) {
  return (
    <div className="flex justify-between gap-4 py-2 text-sm">
      <dt className="text-gray-500">{label}</dt>
      <dd className="text-right font-medium text-gray-900">{children}</dd>
    </div>
  )
}

// হাতে-বানানো ফর্মের নিচের "বাতিল" ও "জমা" বোতাম
export function FormActions({ busy, submitLabel, onCancel }) {
  return (
    <div className="flex justify-end gap-2 pt-2">
      <Button variant="secondary" onClick={onCancel} disabled={busy}>
        বাতিল
      </Button>
      <Button type="submit" disabled={busy}>
        {busy ? 'অপেক্ষা করুন...' : submitLabel}
      </Button>
    </div>
  )
}