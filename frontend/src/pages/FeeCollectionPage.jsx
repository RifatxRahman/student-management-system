import { useState } from 'react'
import { api } from '../api/client.js'
import { useAuth } from '../auth/useAuth.js'
import { FormActions } from '../components/extra.jsx'
import {
  Alert,
  Badge,
  Button,
  Card,
  EmptyState,
  LoadState,
  Modal,
  PageHeader,
  SelectField,
  Table,
  Td,
  TextField,
} from '../components/ui.jsx'
import { useQuery } from '../hooks/useQuery.js'
import { plain } from '../utils/format.js'

const PAYMENT_METHODS = [
  { value: 'CASH', label: 'নগদ (Cash)' },
  { value: 'BKASH', label: 'বিকাশ (bKash)' },
  { value: 'NAGAD', label: 'নগদ (Nagad)' },
  { value: 'ROCKET', label: 'রকেট (Rocket)' },
  { value: 'BANK', label: 'ব্যাংক জমা (Bank Deposit)' },
  { value: 'CARD', label: 'কার্ড (Card)' },
]

function getStatusBadge(status) {
  switch (status) {
    case 'PAID':
      return <Badge tone="green">পরিশোধিত</Badge>
    case 'PARTIAL':
      return <Badge tone="amber">আংশিক প্রদেয়</Badge>
    case 'UNPAID':
      return <Badge tone="red">বকেয়া</Badge>
    case 'CANCELLED':
      return <Badge tone="gray">বাতিল</Badge>
    default:
      return <Badge tone="gray">{status}</Badge>
  }
}

// ----------------- Modal: Collect Payment -----------------
function PaymentModal({ invoice, onDone, onClose }) {
  const [values, setValues] = useState({
    amount: invoice?.dueAmount != null ? String(invoice.dueAmount) : '',
    paymentMethod: 'CASH',
    transactionId: '',
    notes: '',
  })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [receipt, setReceipt] = useState(null)

  async function submit(e) {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      const body = {
        invoiceId: invoice.id,
        amount: Number(values.amount),
        paymentMethod: values.paymentMethod,
        transactionId: values.transactionId || null,
        notes: values.notes || null,
      }
      const res = await api('/api/fee-payments', { method: 'POST', body })
      setReceipt(res)
    } catch (err) {
      setError(err.message)
      setBusy(false)
    }
  }

  if (receipt) {
    return (
      <Modal title="টাকা জমার রসিদ (Money Receipt)" onClose={onDone}>
        <div className="space-y-4 print:p-0">
          <div className="rounded-xl border border-gray-200 bg-gray-50/70 p-5 text-center">
            <h2 className="text-xl font-bold text-gray-900">School Management System</h2>
            <p className="text-xs text-gray-500">অফিসিয়াল ফি আদায় রসিদ</p>
            <div className="mt-3 inline-block rounded-full bg-indigo-50 px-3 py-1 text-xs font-semibold text-indigo-700">
              রসিদ নং: {receipt.receiptNumber}
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3 text-sm">
            <div>
              <p className="text-xs text-gray-500">শিক্ষার্থী</p>
              <p className="font-semibold text-gray-900">{receipt.studentName}</p>
              <p className="text-xs text-gray-600 font-mono">{receipt.studentCode}</p>
            </div>
            <div className="text-right">
              <p className="text-xs text-gray-500">শ্রেণি ও সেকশন</p>
              <p className="font-semibold text-gray-900">{receipt.className} - {receipt.sectionName}</p>
              <p className="text-xs text-gray-600">বিলিং মাস: {receipt.billingMonth}</p>
            </div>
          </div>

          <div className="rounded-lg border border-gray-200 p-4 space-y-2 text-sm bg-white">
            <div className="flex justify-between border-b pb-2">
              <span className="text-gray-600">ইনভয়েস শিরোনাম:</span>
              <span className="font-medium text-gray-900">{receipt.invoiceTitle}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">পেমেন্ট মাধ্যম:</span>
              <span className="font-medium text-gray-900">{receipt.paymentMethodBangla}</span>
            </div>
            {receipt.transactionId && (
              <div className="flex justify-between">
                <span className="text-gray-600">ট্রানজ্যাকশন আইডি / স্লিপ:</span>
                <span className="font-mono text-gray-900">{receipt.transactionId}</span>
              </div>
            )}
            <div className="flex justify-between pt-2 border-t">
              <span className="font-semibold text-gray-900">এইমাত্র পরিশোধিত টাকা:</span>
              <span className="text-lg font-bold text-emerald-600">৳ {plain(receipt.amount)}</span>
            </div>
            <div className="flex justify-between text-xs text-gray-500">
              <span>অবশিষ্ট বকেয়া:</span>
              <span className="font-semibold text-rose-600">৳ {plain(receipt.remainingDueAmount)}</span>
            </div>
          </div>

          <div className="flex justify-between text-xs text-gray-400">
            <span>আদায়কারী: {receipt.collectedBy}</span>
            <span>তারিখ: {new Date(receipt.paymentDate).toLocaleString('bn-BD')}</span>
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <Button variant="secondary" onClick={() => window.print()}>
              রসিদ প্রিন্ট করুন
            </Button>
            <Button onClick={onDone}>
              সম্পন্ন
            </Button>
          </div>
        </div>
      </Modal>
    )
  }

  return (
    <Modal title="ফি পেমেন্ট গ্রহণ" onClose={onClose}>
      <form onSubmit={submit} className="space-y-4">
        {error && <Alert>{error}</Alert>}

        <div className="rounded-lg bg-gray-50 p-3 text-sm">
          <p className="font-medium text-gray-900">{invoice.studentName} ({invoice.studentCode})</p>
          <p className="text-xs text-gray-500">{invoice.className} - {invoice.sectionName} | {invoice.title}</p>
          <div className="mt-2 flex justify-between text-xs">
            <span>মোট নিট ফি: ৳ {plain(invoice.netAmount)}</span>
            <span className="font-bold text-rose-600">বর্তমান বকেয়া: ৳ {plain(invoice.dueAmount)}</span>
          </div>
        </div>

        <TextField
          label="পরিশোধিত টাকার পরিমাণ (৳)"
          type="number"
          step="0.01"
          min="1"
          max={invoice.dueAmount}
          value={values.amount}
          onChange={(e) => setValues({ ...values, amount: e.target.value })}
          required
        />

        <SelectField
          label="পেমেন্ট মাধ্যম"
          value={values.paymentMethod}
          onChange={(e) => setValues({ ...values, paymentMethod: e.target.value })}
          required
        >
          {PAYMENT_METHODS.map((m) => (
            <option key={m.value} value={m.value}>
              {m.label}
            </option>
          ))}
        </SelectField>

        <TextField
          label="ট্রানজ্যাকশন আইডি / ব্যাংক ডিপোজিট স্লিপ নং (ঐচ্ছিক)"
          value={values.transactionId}
          onChange={(e) => setValues({ ...values, transactionId: e.target.value })}
          placeholder="যেমন: bKash TrxID বা Bank Slip #"
        />

        <TextField
          label="মন্তব্য (ঐচ্ছিক)"
          value={values.notes}
          onChange={(e) => setValues({ ...values, notes: e.target.value })}
        />

        <FormActions busy={busy} onCancel={onClose} submitLabel="পেমেন্ট নিশ্চিত করুন" />
      </form>
    </Modal>
  )
}

// ----------------- Modal: Apply Late Fine -----------------
function LateFineModal({ invoice, onDone, onClose }) {
  const [fineAmount, setFineAmount] = useState('50.00')
  const [reason, setReason] = useState('বিলম্ব ফি / Late Payment Fine')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(e) {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      await api(`/api/invoices/${invoice.id}/fine`, {
        method: 'POST',
        body: { fineAmount: Number(fineAmount), reason },
      })
      onDone()
    } catch (err) {
      setError(err.message)
      setBusy(false)
    }
  }

  return (
    <Modal title="বিলম্ব ফি (Late Fine) আরোপ" onClose={onClose}>
      <form onSubmit={submit} className="space-y-4">
        {error && <Alert>{error}</Alert>}
        <p className="text-sm text-gray-600">
          ইনভয়েস: <strong>{invoice.invoiceNumber}</strong> ({invoice.studentName})
        </p>
        <TextField
          label="জরিমানা / বিলম্ব ফি এর পরিমাণ (৳)"
          type="number"
          step="0.01"
          min="0"
          value={fineAmount}
          onChange={(e) => setFineAmount(e.target.value)}
          required
        />
        <TextField
          label="কারণ"
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          required
        />
        <FormActions busy={busy} onCancel={onClose} submitLabel="ফাইন যোগ করুন" />
      </form>
    </Modal>
  )
}

// ----------------- Modal: Invoice Detail -----------------
function InvoiceDetailModal({ invoiceId, onClose }) {
  const { data: inv, loading, error } = useQuery(`/api/invoices/${invoiceId}`)

  return (
    <Modal title="ইনভয়েস ও রসিদ বিবরণী" onClose={onClose}>
      <LoadState loading={loading} error={error}>
        {inv && (
          <div className="space-y-4 text-sm">
            <div className="rounded-lg bg-gray-50 p-4 border border-gray-200">
              <div className="flex justify-between items-start">
                <div>
                  <h4 className="font-bold text-gray-900 text-base">{inv.title}</h4>
                  <p className="text-xs text-gray-500 font-mono">ইনভয়েস নং: {inv.invoiceNumber}</p>
                  <p className="mt-1 text-sm text-indigo-700 font-medium">
                    {inv.studentName} ({inv.studentCode}) — {inv.className} ({inv.sectionName})
                  </p>
                </div>
                <div>{getStatusBadge(inv.status)}</div>
              </div>
              <div className="mt-3 grid grid-cols-2 gap-2 text-xs text-gray-600">
                <p>ইস্যু তারিখ: {inv.issueDate}</p>
                <p>পরিশোধের শেষ তারিখ: {inv.dueDate}</p>
              </div>
            </div>

            {/* Item Breakdown */}
            <div>
              <h5 className="font-semibold text-gray-900 mb-2">খাতভিত্তিক ফি বিভাজন</h5>
              <div className="rounded-lg border border-gray-200 overflow-hidden">
                <table className="w-full text-left text-xs">
                  <thead className="bg-gray-100 text-gray-700">
                    <tr>
                      <th className="p-2.5">ফি খাত</th>
                      <th className="p-2.5 text-right">মূল ফি</th>
                      <th className="p-2.5 text-right">ছাড়/ওয়েভার</th>
                      <th className="p-2.5 text-right">নিট ফি</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-200">
                    {inv.items?.map((it) => (
                      <tr key={it.id}>
                        <td className="p-2.5 font-medium">{it.feeHeadName}</td>
                        <td className="p-2.5 text-right">৳ {plain(it.amount)}</td>
                        <td className="p-2.5 text-right text-emerald-600">
                          {Number(it.waiverAmount) > 0 ? `-৳ ${plain(it.waiverAmount)}` : '—'}
                        </td>
                        <td className="p-2.5 text-right font-semibold">৳ {plain(it.netAmount)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Financial Summary */}
            <div className="rounded-lg bg-slate-50 p-3 space-y-1.5 text-xs text-slate-700">
              <div className="flex justify-between">
                <span>সাবটোটাল:</span>
                <span>৳ {plain(inv.subtotalAmount)}</span>
              </div>
              {Number(inv.waiverAmount) > 0 && (
                <div className="flex justify-between text-emerald-600">
                  <span>মোট ছাড় / ওয়েভার:</span>
                  <span>- ৳ {plain(inv.waiverAmount)}</span>
                </div>
              )}
              {Number(inv.fineAmount) > 0 && (
                <div className="flex justify-between text-rose-600">
                  <span>বিলম্ব জরিমানা:</span>
                  <span>+ ৳ {plain(inv.fineAmount)}</span>
                </div>
              )}
              <div className="flex justify-between font-bold text-sm text-slate-900 pt-1 border-t">
                <span>মোট নিট প্রদেয়:</span>
                <span>৳ {plain(inv.netAmount)}</span>
              </div>
              <div className="flex justify-between text-emerald-700">
                <span>মোট পরিশোধিত:</span>
                <span>৳ {plain(inv.paidAmount)}</span>
              </div>
              <div className="flex justify-between font-bold text-rose-700">
                <span>অবশিষ্ট বকেয়া:</span>
                <span>৳ {plain(inv.dueAmount)}</span>
              </div>
            </div>

            {/* Payment Receipts History */}
            {inv.payments && inv.payments.length > 0 && (
              <div>
                <h5 className="font-semibold text-gray-900 mb-2">পরিশোধের ইতিহাস ({inv.payments.length})</h5>
                <div className="space-y-2">
                  {inv.payments.map((p) => (
                    <div key={p.paymentId} className="flex justify-between items-center rounded-lg border p-2.5 text-xs">
                      <div>
                        <p className="font-semibold text-gray-900">
                          ৳ {plain(p.amount)} ({p.paymentMethodBangla})
                        </p>
                        <p className="text-gray-500 font-mono">রসিদ: {p.receiptNumber}</p>
                      </div>
                      <div className="text-right text-gray-500">
                        <p>{new Date(p.paymentDate).toLocaleDateString('bn-BD')}</p>
                        <p>আদায়কারী: {p.collectedBy}</p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}
      </LoadState>
    </Modal>
  )
}

// ----------------- Modal: Student Waiver -----------------
function StudentWaiverModal({ sessions, students, feeHeads, onDone, onClose }) {
  const [values, setValues] = useState({
    studentId: students[0]?.id ? String(students[0].id) : '',
    sessionId: sessions.find((s) => s.active)?.id ? String(sessions.find((s) => s.active).id) : '',
    feeHeadId: feeHeads[0]?.id ? String(feeHeads[0].id) : '',
    discountType: 'PERCENTAGE',
    discountValue: '50.00',
    reason: 'দরিদ্র ও মেধাবী শিক্ষার্থী বৃত্তি',
  })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(e) {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      const body = {
        studentId: Number(values.studentId),
        sessionId: Number(values.sessionId),
        feeHeadId: Number(values.feeHeadId),
        discountType: values.discountType,
        discountValue: Number(values.discountValue),
        reason: values.reason,
        active: true,
      }
      await api('/api/fee-waivers', { method: 'POST', body })
      onDone()
    } catch (err) {
      setError(err.message)
      setBusy(false)
    }
  }

  return (
    <Modal title="শিক্ষার্থী ওয়েভার / বিশেষ ছাড় অনুমোদন" onClose={onClose}>
      <form onSubmit={submit} className="space-y-4">
        {error && <Alert>{error}</Alert>}
        <SelectField
          label="শিক্ষার্থী নির্বাচন"
          value={values.studentId}
          onChange={(e) => setValues({ ...values, studentId: e.target.value })}
          required
        >
          {students.map((s) => (
            <option key={s.id} value={s.id}>
              {s.fullName} ({s.studentCode})
            </option>
          ))}
        </SelectField>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <SelectField
            label="শিক্ষাবর্ষ (সেশন)"
            value={values.sessionId}
            onChange={(e) => setValues({ ...values, sessionId: e.target.value })}
            required
          >
            {sessions.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name}
              </option>
            ))}
          </SelectField>

          <SelectField
            label="ফি খাত"
            value={values.feeHeadId}
            onChange={(e) => setValues({ ...values, feeHeadId: e.target.value })}
            required
          >
            {feeHeads.map((h) => (
              <option key={h.id} value={h.id}>
                {h.name}
              </option>
            ))}
          </SelectField>
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <SelectField
            label="ছাড়ের ধরন"
            value={values.discountType}
            onChange={(e) => setValues({ ...values, discountType: e.target.value })}
            required
          >
            <option value="PERCENTAGE">শতকরা হার (%)</option>
            <option value="FIXED_AMOUNT">নির্দিষ্ট টাকা (৳)</option>
          </SelectField>

          <TextField
            label={values.discountType === 'PERCENTAGE' ? 'শতকরা মান (%)' : 'টাকার পরিমাণ (৳)'}
            type="number"
            step="0.01"
            min="0"
            max={values.discountType === 'PERCENTAGE' ? '100' : undefined}
            value={values.discountValue}
            onChange={(e) => setValues({ ...values, discountValue: e.target.value })}
            required
          />
        </div>

        <TextField
          label="ছাড়ের কারণ বা অনুমোদনের রেফারেন্স"
          value={values.reason}
          onChange={(e) => setValues({ ...values, reason: e.target.value })}
          required
        />

        <FormActions busy={busy} onCancel={onClose} submitLabel="ওয়েভার যুক্ত করুন" />
      </form>
    </Modal>
  )
}

// ----------------- Main Page Component -----------------
export default function FeeCollectionPage() {
  const { user } = useAuth()
  const isAdmin = user.role === 'ADMIN'

  const [activeTab, setActiveTab] = useState('invoices') // 'invoices' | 'bulk' | 'waivers'

  // Master Data
  const { data: sessions } = useQuery('/api/sessions')
  const { data: classes } = useQuery('/api/classes')
  const { data: feeHeads } = useQuery('/api/fee-heads')
  const { data: students } = useQuery('/api/students?size=200')

  const activeSession = sessions?.find((s) => s.active) || sessions?.[0]

  // Filters for Invoices Tab
  const [filterSessionId, setFilterSessionId] = useState('')
  const [filterClassId, setFilterClassId] = useState('')
  const [filterStatus, setFilterStatus] = useState('')
  const [filterMonth, setFilterMonth] = useState('')

  const effectiveSessionId = filterSessionId || (activeSession?.id ? String(activeSession.id) : '')

  const invoiceQueryUrl = effectiveSessionId
    ? `/api/invoices?sessionId=${effectiveSessionId}${filterClassId ? `&classId=${filterClassId}` : ''}${
        filterStatus ? `&status=${filterStatus}` : ''
      }${filterMonth ? `&billingMonth=${filterMonth}` : ''}`
    : null

  const { data: invoices, loading: invLoading, error: invError, reload: reloadInvoices } = useQuery(invoiceQueryUrl)
  const { data: waivers, loading: wLoading, reload: reloadWaivers } = useQuery('/api/fee-waivers')

  // Modals state
  const [payModal, setPayModal] = useState({ open: false, invoice: null })
  const [fineModal, setFineModal] = useState({ open: false, invoice: null })
  const [detailModal, setDetailModal] = useState({ open: false, invoiceId: null })
  const [waiverModal, setWaiverModal] = useState(false)

  // Bulk Invoice Form State
  const todayStr = new Date().toISOString().split('T')[0]
  const currentMonthStr = todayStr.substring(0, 7) // '2026-11'
  const [bulkSessionId, setBulkSessionId] = useState('')
  const [bulkClassId, setBulkClassId] = useState('')
  const [bulkMonth, setBulkMonth] = useState(currentMonthStr)
  const [bulkTitle, setBulkTitle] = useState('মাসিক টিউশন ফি')
  const [bulkDueDate, setBulkDueDate] = useState('')
  const [bulkBusy, setBulkBusy] = useState(false)
  const [bulkResult, setBulkResult] = useState(null)
  const [bulkError, setBulkError] = useState('')

  async function handleBulkGenerate(e) {
    e.preventDefault()
    setBulkBusy(true)
    setBulkError('')
    setBulkResult(null)
    try {
      const body = {
        sessionId: Number(bulkSessionId || effectiveSessionId),
        classId: Number(bulkClassId || classes?.[0]?.id),
        billingMonth: bulkMonth,
        title: bulkTitle,
        issueDate: todayStr,
        dueDate: bulkDueDate || todayStr,
      }
      const res = await api('/api/invoices/bulk', { method: 'POST', body })
      setBulkResult(res)
      reloadInvoices()
    } catch (err) {
      setBulkError(err.message)
    } finally {
      setBulkBusy(false)
    }
  }

  async function deleteWaiver(id) {
    if (!window.confirm('আপনি কি এই ওয়েভারটি বাতিল করতে চান?')) return
    try {
      await api(`/api/fee-waivers/${id}`, { method: 'DELETE' })
      reloadWaivers()
    } catch (err) {
      alert(err.message)
    }
  }

  // Financial KPI totals
  let totalBilled = 0
  let totalCollected = 0
  let totalDue = 0
  if (invoices && invoices.length > 0) {
    for (let i = 0; i < invoices.length; i++) {
      totalBilled += Number(invoices[i].netAmount || 0)
      totalCollected += Number(invoices[i].paidAmount || 0)
      totalDue += Number(invoices[i].dueAmount || 0)
    }
  }

  return (
    <>
      <PageHeader
        title="ফি আদায় ও শিক্ষার্থী ইনভয়েস"
        subtitle="মাসিক ও সাময়িক ইনভয়েস তৈরি, ফি আদায়, রশিদ প্রিন্ট এবং ওয়েভার পলিসি"
        action={
          isAdmin && (
            <div className="flex gap-2">
              <Button variant="secondary" onClick={() => setWaiverModal(true)}>
                + বিশেষ ওয়েভার
              </Button>
              <Button onClick={() => setActiveTab('bulk')}>
                + বাল্ক ইনভয়েস তৈরি
              </Button>
            </div>
          )
        }
      />

      {/* Tabs */}
      <div className="mb-6 flex border-b border-gray-200">
        <button
          type="button"
          onClick={() => setActiveTab('invoices')}
          className={`border-b-2 px-5 py-2.5 text-sm font-medium transition ${
            activeTab === 'invoices'
              ? 'border-indigo-600 text-indigo-600'
              : 'border-transparent text-gray-500 hover:border-gray-300 hover:text-gray-700'
          }`}
        >
          ইনভয়েস ও ফি আদায় ({invoices?.length || 0})
        </button>
        <button
          type="button"
          onClick={() => setActiveTab('bulk')}
          className={`border-b-2 px-5 py-2.5 text-sm font-medium transition ${
            activeTab === 'bulk'
              ? 'border-indigo-600 text-indigo-600'
              : 'border-transparent text-gray-500 hover:border-gray-300 hover:text-gray-700'
          }`}
        >
          বাল্ক ইনভয়েস জেনারেটর
        </button>
        <button
          type="button"
          onClick={() => setActiveTab('waivers')}
          className={`border-b-2 px-5 py-2.5 text-sm font-medium transition ${
            activeTab === 'waivers'
              ? 'border-indigo-600 text-indigo-600'
              : 'border-transparent text-gray-500 hover:border-gray-300 hover:text-gray-700'
          }`}
        >
          শিক্ষার্থী ওয়েভার / বৃত্তি ({waivers?.length || 0})
        </button>
      </div>

      {/* TAB 1: Invoices & Payment Collection */}
      {activeTab === 'invoices' && (
        <div className="space-y-6">
          {/* Filter Bar */}
          <Card className="p-4 bg-gray-50/50">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-5 items-end">
              <div>
                <label className="block text-xs font-semibold text-gray-600 mb-1">শিক্ষাবর্ষ</label>
                <select
                  value={effectiveSessionId}
                  onChange={(e) => setFilterSessionId(e.target.value)}
                  className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none"
                >
                  {sessions?.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.name}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-600 mb-1">শ্রেণি (ক্লাস)</label>
                <select
                  value={filterClassId}
                  onChange={(e) => setFilterClassId(e.target.value)}
                  className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none"
                >
                  <option value="">সব শ্রেণি</option>
                  {classes?.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-600 mb-1">পরিশোধের অবস্থা</label>
                <select
                  value={filterStatus}
                  onChange={(e) => setFilterStatus(e.target.value)}
                  className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none"
                >
                  <option value="">সকল অবস্থা</option>
                  <option value="UNPAID">বকেয়া (Unpaid)</option>
                  <option value="PARTIAL">আংশিক প্রদেয় (Partial)</option>
                  <option value="PAID">পরিশোধিত (Paid)</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-600 mb-1">বিলিং মাস</label>
                <input
                  type="month"
                  value={filterMonth}
                  onChange={(e) => setFilterMonth(e.target.value)}
                  className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none"
                />
              </div>

              <div>
                <Button
                  variant="secondary"
                  className="w-full"
                  onClick={() => {
                    setFilterClassId('')
                    setFilterStatus('')
                    setFilterMonth('')
                    reloadInvoices()
                  }}
                >
                  রিসেট ফিল্টার
                </Button>
              </div>
            </div>
          </Card>

          {/* Financial KPIs */}
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <div className="rounded-xl border border-indigo-100 bg-indigo-50/50 p-4">
              <p className="text-xs font-semibold uppercase tracking-wider text-indigo-700">মোট দাবি / বিলিং</p>
              <p className="mt-1 text-2xl font-bold text-indigo-900">৳ {plain(totalBilled)}</p>
              <p className="mt-1 text-xs text-indigo-600">{invoices?.length || 0} টি ইনভয়েস</p>
            </div>

            <div className="rounded-xl border border-emerald-100 bg-emerald-50/50 p-4">
              <p className="text-xs font-semibold uppercase tracking-wider text-emerald-700">মোট সংগৃহীত ফি</p>
              <p className="mt-1 text-2xl font-bold text-emerald-900">৳ {plain(totalCollected)}</p>
              <p className="mt-1 text-xs text-emerald-600">ক্যাশ ও ডিজিটাল পেমেন্ট</p>
            </div>

            <div className="rounded-xl border border-rose-100 bg-rose-50/50 p-4">
              <p className="text-xs font-semibold uppercase tracking-wider text-rose-700">মোট বকেয়া ফি</p>
              <p className="mt-1 text-2xl font-bold text-rose-900">৳ {plain(totalDue)}</p>
              <p className="mt-1 text-xs text-rose-600">আদায়যোগ্য অবশিষ্ট</p>
            </div>
          </div>

          {/* Invoices Table */}
          <Card>
            <LoadState loading={invLoading} error={invError}>
              {invoices?.length === 0 ? (
                <EmptyState>কোনো ইনভয়েস পাওয়া যায়নি। &quot;+ বাল্ক ইনভয়েস তৈরি&quot; ট্যাবে গিয়ে ইনভয়েস জেনারেট করুন।</EmptyState>
              ) : (
                <Table headers={['ইনভয়েস নং', 'শিক্ষার্থী ও শ্রেণি', 'শিরোনাম ও মাস', 'নিট ফি', 'পরিশোধিত', 'বকেয়া', 'স্ট্যাটাস', 'অ্যাকশন']}>
                  {invoices?.map((inv) => (
                    <tr key={inv.id}>
                      <Td className="font-mono text-xs font-semibold text-gray-900">
                        <button
                          type="button"
                          onClick={() => setDetailModal({ open: true, invoiceId: inv.id })}
                          className="hover:text-indigo-600 underline"
                        >
                          {inv.invoiceNumber}
                        </button>
                      </Td>
                      <Td>
                        <p className="font-semibold text-gray-900">{inv.studentName}</p>
                        <p className="text-xs text-gray-500 font-mono">
                          {inv.studentCode} | {inv.className} ({inv.sectionName})
                        </p>
                      </Td>
                      <Td>
                        <p className="text-gray-900">{inv.title}</p>
                        <p className="text-xs text-gray-500">মাস: {inv.billingMonth}</p>
                      </Td>
                      <Td className="font-semibold">৳ {plain(inv.netAmount)}</Td>
                      <Td className="text-emerald-700 font-medium">৳ {plain(inv.paidAmount)}</Td>
                      <Td className="text-rose-700 font-bold">৳ {plain(inv.dueAmount)}</Td>
                      <Td>{getStatusBadge(inv.status)}</Td>
                      <Td>
                        <div className="flex gap-1.5">
                          {inv.status !== 'PAID' && isAdmin && (
                            <>
                              <Button
                                size="sm"
                                onClick={() => setPayModal({ open: true, invoice: inv })}
                              >
                                পেমেন্ট
                              </Button>
                              <Button
                                variant="secondary"
                                size="sm"
                                onClick={() => setFineModal({ open: true, invoice: inv })}
                              >
                                ফাইন
                              </Button>
                            </>
                          )}
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => setDetailModal({ open: true, invoiceId: inv.id })}
                          >
                            রসিদ
                          </Button>
                        </div>
                      </Td>
                    </tr>
                  ))}
                </Table>
              )}
            </LoadState>
          </Card>
        </div>
      )}

      {/* TAB 2: Bulk Invoice Generator */}
      {activeTab === 'bulk' && (
        <Card className="max-w-2xl p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-2">বাল্ক ইনভয়েস জেনারেটর (Bulk Fee Invoicing)</h3>
          <p className="text-sm text-gray-600 mb-6">
            একটি ক্লাসের সকল সক্রিয় শিক্ষার্থীর জন্য এক ক্লিকে মাসিক টিউশন ফি ও অন্যান্য নিয়মিত ফি ইনভয়েস তৈরি করুন।
            সিস্টেম স্বয়ংক্রিয়ভাবে শিক্ষার্থীর জন্য নির্ধারিত বৃত্তি ও ওয়েভার হিসাব করে নিট বিল প্রস্তুত করবে।
          </p>

          {bulkError && (
            <div className="mb-4">
              <Alert>{bulkError}</Alert>
            </div>
          )}

          {bulkResult && (
            <div className="mb-4">
              <Alert type="success">
                সফলভাবে সম্পন্ন হয়েছে! মোট শিক্ষার্থী: {bulkResult.totalEligibleStudents} জন, নতুন ইনভয়েস তৈরি: {bulkResult.generatedInvoicesCount} টি,
                আগে তৈরি থাকায় স্কিপ: {bulkResult.skippedCount} টি, মোট বিলিং: ৳ {plain(bulkResult.totalBilledAmount)}।
              </Alert>
            </div>
          )}

          <form onSubmit={handleBulkGenerate} className="space-y-4">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <SelectField
                label="শিক্ষাবর্ষ (সেশন)"
                value={bulkSessionId || effectiveSessionId}
                onChange={(e) => setBulkSessionId(e.target.value)}
                required
              >
                {sessions?.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name}
                  </option>
                ))}
              </SelectField>

              <SelectField
                label="শ্রেণি (ক্লাস)"
                value={bulkClassId || classes?.[0]?.id}
                onChange={(e) => setBulkClassId(e.target.value)}
                required
              >
                {classes?.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </SelectField>
            </div>

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <TextField
                label="বিলিং মাস (YYYY-MM)"
                type="month"
                value={bulkMonth}
                onChange={(e) => setBulkMonth(e.target.value)}
                required
              />

              <TextField
                label="ইনভয়েস শিরোনাম"
                value={bulkTitle}
                onChange={(e) => setBulkTitle(e.target.value)}
                required
              />
            </div>

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <TextField
                label="পরিশোধের শেষ তারিখ (Due Date)"
                type="date"
                value={bulkDueDate}
                onChange={(e) => setBulkDueDate(e.target.value)}
                required
              />
            </div>

            <div className="pt-4">
              <Button type="submit" disabled={bulkBusy}>
                {bulkBusy ? 'ইনভয়েস তৈরি হচ্ছে...' : 'ইনভয়েস জেনারেট করুন'}
              </Button>
            </div>
          </form>
        </Card>
      )}

      {/* TAB 3: Student Waivers */}
      {activeTab === 'waivers' && (
        <Card>
          <LoadState loading={wLoading}>
            {waivers?.length === 0 ? (
              <EmptyState>কোনো শিক্ষার্থীর জন্য বিশেষ ছাড় বা বৃত্তি যুক্ত করা হয়নি।</EmptyState>
            ) : (
              <Table headers={['শিক্ষার্থী', 'শিক্ষাবর্ষ', 'ফি খাত', 'ছাড়ের ধরন ও মান', 'অনুমোদনের কারণ', 'স্ট্যাটাস', 'অ্যাকশন']}>
                {waivers?.map((w) => (
                  <tr key={w.id}>
                    <Td>
                      <p className="font-semibold text-gray-900">{w.studentName}</p>
                      <p className="text-xs text-gray-500 font-mono">{w.studentCode}</p>
                    </Td>
                    <Td>{w.sessionName}</Td>
                    <Td className="font-medium text-gray-900">{w.feeHeadName}</Td>
                    <Td className="font-bold text-emerald-700">
                      {w.discountType === 'PERCENTAGE' ? `${plain(w.discountValue)}%` : `৳ ${plain(w.discountValue)}`}
                    </Td>
                    <Td className="text-gray-600">{w.reason || '—'}</Td>
                    <Td>
                      <Badge tone={w.active ? 'green' : 'gray'}>
                        {w.active ? 'সক্রিয়' : 'নিষ্ক্রিয়'}
                      </Badge>
                    </Td>
                    <Td>
                      {isAdmin && (
                        <Button
                          variant="ghost"
                          size="sm"
                          className="text-red-600 hover:text-red-700"
                          onClick={() => deleteWaiver(w.id)}
                        >
                          মুছুন
                        </Button>
                      )}
                    </Td>
                  </tr>
                ))}
              </Table>
            )}
          </LoadState>
        </Card>
      )}

      {/* Modals */}
      {payModal.open && (
        <PaymentModal
          invoice={payModal.invoice}
          onDone={() => {
            setPayModal({ open: false, invoice: null })
            reloadInvoices()
          }}
          onClose={() => setPayModal({ open: false, invoice: null })}
        />
      )}

      {fineModal.open && (
        <LateFineModal
          invoice={fineModal.invoice}
          onDone={() => {
            setFineModal({ open: false, invoice: null })
            reloadInvoices()
          }}
          onClose={() => setFineModal({ open: false, invoice: null })}
        />
      )}

      {detailModal.open && (
        <InvoiceDetailModal
          invoiceId={detailModal.invoiceId}
          onClose={() => setDetailModal({ open: false, invoiceId: null })}
        />
      )}

      {waiverModal && sessions && students && feeHeads && (
        <StudentWaiverModal
          sessions={sessions}
          students={students}
          feeHeads={feeHeads}
          onDone={() => {
            setWaiverModal(false)
            reloadWaivers()
          }}
          onClose={() => setWaiverModal(false)}
        />
      )}
    </>
  )
}
