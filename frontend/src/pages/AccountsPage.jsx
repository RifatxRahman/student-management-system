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
  { value: 'BANK', label: 'ব্যাংক চেক / ডিপোজিট (Bank)' },
  { value: 'BKASH', label: 'বিকাশ (bKash)' },
  { value: 'NAGAD', label: 'নগদ (Nagad)' },
  { value: 'ROCKET', label: 'রকেট (Rocket)' },
  { value: 'CARD', label: 'কার্ড (Card)' },
]

// ----------------- Modal: Expense Category -----------------
function ExpenseCategoryModal({ initial, onDone, onClose }) {
  const [values, setValues] = useState({
    name: initial?.name || '',
    code: initial?.code || '',
    description: initial?.description || '',
    active: initial?.active ?? true,
  })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(e) {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      const body = {
        name: values.name,
        code: values.code,
        description: values.description || null,
        active: values.active,
      }
      if (initial?.id) {
        await api(`/api/expense-categories/${initial.id}`, { method: 'PUT', body })
      } else {
        await api('/api/expense-categories', { method: 'POST', body })
      }
      onDone()
    } catch (err) {
      setError(err.message)
      setBusy(false)
    }
  }

  return (
    <Modal title={initial ? 'ব্যয়ের খাত সম্পাদনা' : 'নতুন ব্যয়ের খাত'} onClose={onClose}>
      <form onSubmit={submit} className="space-y-4">
        {error && <Alert>{error}</Alert>}
        <TextField
          label="খাতের নাম (যেমন: অফিস স্টেশনারি, ইউটিলিটি বিল)"
          value={values.name}
          onChange={(e) => setValues({ ...values, name: e.target.value })}
          required
        />
        <TextField
          label="ইউনিক কোড (যেমন: STATIONERY, UTILITY, SALARY)"
          value={values.code}
          onChange={(e) => setValues({ ...values, code: e.target.value.toUpperCase() })}
          required
        />
        <TextField
          label="বিবরণ (ঐচ্ছিক)"
          value={values.description}
          onChange={(e) => setValues({ ...values, description: e.target.value })}
        />
        <div className="flex items-center gap-2 pt-1">
          <input
            id="catActive"
            type="checkbox"
            checked={values.active}
            onChange={(e) => setValues({ ...values, active: e.target.checked })}
            className="h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-600"
          />
          <label htmlFor="catActive" className="text-sm font-medium text-gray-700">
            এই ব্যয়ের খাত সক্রিয় রাখুন
          </label>
        </div>
        <FormActions busy={busy} onCancel={onClose} submitLabel={initial ? 'সংরক্ষণ করুন' : 'তৈরি করুন'} />
      </form>
    </Modal>
  )
}

// ----------------- Modal: Expense Voucher -----------------
function ExpenseVoucherModal({ initial, categories, onDone, onClose }) {
  const todayStr = new Date().toISOString().split('T')[0]
  const [values, setValues] = useState({
    categoryId: initial?.categoryId ? String(initial.categoryId) : categories[0]?.id ? String(categories[0].id) : '',
    title: initial?.title || '',
    expenseDate: initial?.expenseDate || todayStr,
    amount: initial?.amount != null ? String(initial.amount) : '',
    paymentMethod: initial?.paymentMethod || 'CASH',
    payeeName: initial?.payeeName || '',
    referenceNo: initial?.referenceNo || '',
    notes: initial?.notes || '',
  })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(e) {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      const body = {
        categoryId: Number(values.categoryId),
        title: values.title,
        expenseDate: values.expenseDate,
        amount: Number(values.amount),
        paymentMethod: values.paymentMethod,
        payeeName: values.payeeName,
        referenceNo: values.referenceNo || null,
        notes: values.notes || null,
      }
      if (initial?.id) {
        await api(`/api/expenses/${initial.id}`, { method: 'PUT', body })
      } else {
        await api('/api/expenses', { method: 'POST', body })
      }
      onDone()
    } catch (err) {
      setError(err.message)
      setBusy(false)
    }
  }

  return (
    <Modal title={initial ? 'খরচ ভাউচার সম্পাদনা' : 'নতুন খরচ ভাউচার এন্ট্রি'} onClose={onClose}>
      <form onSubmit={submit} className="space-y-4">
        {error && <Alert>{error}</Alert>}
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <SelectField
            label="ব্যয়ের খাত (Expense Category)"
            value={values.categoryId}
            onChange={(e) => setValues({ ...values, categoryId: e.target.value })}
            required
          >
            {categories.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </SelectField>

          <TextField
            label="খরচের তারিখ"
            type="date"
            value={values.expenseDate}
            onChange={(e) => setValues({ ...values, expenseDate: e.target.value })}
            required
          />
        </div>

        <TextField
          label="ব্যয়ের বিবরণ / শিরোনাম"
          placeholder="যেমন: অফিস খাতা ও কলম ক্রয়, ডেসকো বিদ্যুৎ বিল"
          value={values.title}
          onChange={(e) => setValues({ ...values, title: e.target.value })}
          required
        />

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <TextField
            label="টাকার পরিমাণ (৳)"
            type="number"
            step="0.01"
            min="1"
            value={values.amount}
            onChange={(e) => setValues({ ...values, amount: e.target.value })}
            required
          />

          <SelectField
            label="পরিশোধের মাধ্যম"
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
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <TextField
            label="প্রাপকের নাম (কাকে দেওয়া হয়েছে)"
            placeholder="ব্যক্তি, ভেন্ডর বা প্রতিষ্ঠানের নাম"
            value={values.payeeName}
            onChange={(e) => setValues({ ...values, payeeName: e.target.value })}
            required
          />

          <TextField
            label="চালান / ক্যাশমেমো / বিল নং (ঐচ্ছিক)"
            placeholder="বিল বা ইনভয়েস নম্বর"
            value={values.referenceNo}
            onChange={(e) => setValues({ ...values, referenceNo: e.target.value })}
          />
        </div>

        <TextField
          label="অতিরিক্ত মন্তব্য (ঐচ্ছিক)"
          value={values.notes}
          onChange={(e) => setValues({ ...values, notes: e.target.value })}
        />

        <FormActions busy={busy} onCancel={onClose} submitLabel={initial ? 'সংরক্ষণ করুন' : 'ভাউচার তৈরি করুন'} />
      </form>
    </Modal>
  )
}

// ----------------- Main Page Component -----------------
export default function AccountsPage() {
  const { user } = useAuth()
  const isAdmin = user.role === 'ADMIN'

  const [activeTab, setActiveTab] = useState('summary') // 'summary' | 'expenses' | 'categories'

  // Date state for Accounts Summary
  const today = new Date()
  const firstDayOfMonth = new Date(today.getFullYear(), today.getMonth(), 1).toISOString().split('T')[0]
  const todayStr = today.toISOString().split('T')[0]

  const [startDate, setStartDate] = useState(firstDayOfMonth)
  const [endDate, setEndDate] = useState(todayStr)

  // Master Categories
  const { data: categories, loading: cLoading, reload: reloadCategories } = useQuery('/api/expense-categories')

  // Accounts Summary Query
  const summaryUrl = `/api/accounts/summary?startDate=${startDate}&endDate=${endDate}`
  const { data: summary, loading: sLoading, error: sError, reload: reloadSummary } = useQuery(summaryUrl)

  // Expense List Filter State
  const [filterCatId, setFilterCatId] = useState('')
  const [filterStartDate, setFilterStartDate] = useState('')
  const [filterEndDate, setFilterEndDate] = useState('')

  const expenseUrl = `/api/expenses?${filterCatId ? `categoryId=${filterCatId}&` : ''}${
    filterStartDate ? `startDate=${filterStartDate}&` : ''
  }${filterEndDate ? `endDate=${filterEndDate}&` : ''}`

  const { data: expenses, loading: eLoading, error: eError, reload: reloadExpenses } = useQuery(expenseUrl)

  // Modals state
  const [catModal, setCatModal] = useState({ open: false, item: null })
  const [voucherModal, setVoucherModal] = useState({ open: false, item: null })
  const [exporting, setExporting] = useState(false)

  async function handleExportExpenses() {
    try {
      setExporting(true)
      const params = new URLSearchParams()
      if (filterCatId) params.set('categoryId', filterCatId)
      if (filterStartDate) params.set('startDate', filterStartDate)
      if (filterEndDate) params.set('endDate', filterEndDate)

      const token = window.__accessToken || localStorage.getItem('token')
      const res = await fetch(`/api/expenses/export?${params.toString()}`, {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      })
      if (!res.ok) throw new Error('খরচের এক্সেল ফাইল তৈরিতে ব্যর্থ হয়েছে')
      const blob = await res.blob()
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `expenses_report_${new Date().toISOString().slice(0, 10)}.xlsx`
      document.body.appendChild(a)
      a.click()
      a.remove()
      window.URL.revokeObjectURL(url)
    } catch (err) {
      alert(err.message || 'এক্সপোর্ট করতে সমস্যা হয়েছে')
    } finally {
      setExporting(false)
    }
  }

  async function deleteExpense(id) {
    if (!window.confirm('আপনি কি এই খরচ ভাউচারটি মুছে ফেলতে চান?')) return
    try {
      await api(`/api/expenses/${id}`, { method: 'DELETE' })
      reloadExpenses()
      reloadSummary()
    } catch (err) {
      alert(err.message)
    }
  }

  async function deleteCategory(id) {
    if (!window.confirm('আপনি কি এই ব্যয়ের খাতটি মুছে ফেলতে চান?')) return
    try {
      await api(`/api/expense-categories/${id}`, { method: 'DELETE' })
      reloadCategories()
    } catch (err) {
      alert(err.message)
    }
  }

  function setPresetRange(type) {
    const now = new Date()
    if (type === 'THIS_MONTH') {
      setStartDate(new Date(now.getFullYear(), now.getMonth(), 1).toISOString().split('T')[0])
      setEndDate(now.toISOString().split('T')[0])
    } else if (type === 'LAST_MONTH') {
      const first = new Date(now.getFullYear(), now.getMonth() - 1, 1).toISOString().split('T')[0]
      const last = new Date(now.getFullYear(), now.getMonth(), 0).toISOString().split('T')[0]
      setStartDate(first)
      setEndDate(last)
    } else if (type === 'THIS_YEAR') {
      setStartDate(`${now.getFullYear()}-01-01`)
      setEndDate(now.toISOString().split('T')[0])
    }
  }

  return (
    <>
      <PageHeader
        title="ব্যয় ব্যবস্থাপনা ও হিসাব বিবরণী"
        subtitle="দৈনিক পরিচালন খরচ ভাউচার, ক্যাশ/ব্যাংক ট্র্যাকিং, লাভ-ক্ষতি ও বকেয়া হিসাব"
        action={
          isAdmin && (
            <div className="flex gap-2">
              <Button variant="secondary" onClick={() => setCatModal({ open: true, item: null })}>
                + নতুন ব্যয়ের খাত
              </Button>
              <Button onClick={() => setVoucherModal({ open: true, item: null })}>
                + খরচ ভাউচার
              </Button>
            </div>
          )
        }
      />

      {/* Tabs */}
      <div className="mb-6 flex border-b border-[var(--border-hairline)]">
        <button
          type="button"
          onClick={() => setActiveTab('summary')}
          className={`border-b-2 px-5 py-2.5 text-sm font-semibold transition ${
            activeTab === 'summary'
              ? 'border-[#0E3B2E] text-[#0E3B2E]'
              : 'border-transparent text-[var(--ink-secondary)] hover:border-gray-300 hover:text-[var(--ink-primary)]'
          }`}
        >
          আর্থিক হিসাব ও লাভ-ক্ষতি
        </button>
        <button
          type="button"
          onClick={() => setActiveTab('expenses')}
          className={`border-b-2 px-5 py-2.5 text-sm font-semibold transition ${
            activeTab === 'expenses'
              ? 'border-[#0E3B2E] text-[#0E3B2E]'
              : 'border-transparent text-[var(--ink-secondary)] hover:border-gray-300 hover:text-[var(--ink-primary)]'
          }`}
        >
          খরচ ভাউচারসমূহ ({expenses?.length || 0})
        </button>
        <button
          type="button"
          onClick={() => setActiveTab('categories')}
          className={`border-b-2 px-5 py-2.5 text-sm font-semibold transition ${
            activeTab === 'categories'
              ? 'border-[#0E3B2E] text-[#0E3B2E]'
              : 'border-transparent text-[var(--ink-secondary)] hover:border-gray-300 hover:text-[var(--ink-primary)]'
          }`}
        >
          ব্যয়ের খাতসমূহ ({categories?.length || 0})
        </button>
      </div>

      {/* TAB 1: Accounts Summary & Profit / Loss */}
      {activeTab === 'summary' && (
        <div className="space-y-6">
          {/* Date Range Selector Bar */}
          <Card className="p-4 bg-gray-50/50">
            <div className="flex flex-wrap items-center justify-between gap-4">
              <div className="flex flex-wrap items-center gap-3">
                <span className="text-xs font-semibold uppercase tracking-wider text-gray-600">সময়সীমা:</span>
                <input
                  type="date"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                  className="rounded-lg border border-gray-300 bg-white px-3 py-1.5 text-sm"
                />
                <span className="text-gray-500 text-sm">হতে</span>
                <input
                  type="date"
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                  className="rounded-lg border border-gray-300 bg-white px-3 py-1.5 text-sm"
                />
              </div>

              <div className="flex flex-wrap gap-2">
                <Button variant="secondary" size="sm" onClick={() => setPresetRange('THIS_MONTH')}>
                  চলতি মাস
                </Button>
                <Button variant="secondary" size="sm" onClick={() => setPresetRange('LAST_MONTH')}>
                  গত মাস
                </Button>
                <Button variant="secondary" size="sm" onClick={() => setPresetRange('THIS_YEAR')}>
                  চলতি বছর
                </Button>
                <Button size="sm" onClick={reloadSummary}>
                  আপডেট দেখুন
                </Button>
              </div>
            </div>
          </Card>

          <LoadState loading={sLoading} error={sError}>
            {summary && (
              <>
                {/* 4 Financial KPI Cards */}
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
                  <div className="rounded-xl border border-emerald-200 bg-emerald-50/50 p-5 shadow-sm">
                    <p className="text-xs font-semibold uppercase tracking-wider text-emerald-800">মোট আয় / ফি সংগ্রহ</p>
                    <p className="mt-2 text-3xl font-extrabold text-emerald-950">৳ {plain(summary.totalIncome)}</p>
                    <p className="mt-1 text-xs text-emerald-700">{summary.totalIncomeTransactions} টি ট্রানজ্যাকশন</p>
                  </div>

                  <div className="rounded-xl border border-rose-200 bg-rose-50/50 p-5 shadow-sm">
                    <p className="text-xs font-semibold uppercase tracking-wider text-rose-800">মোট পরিচালন ব্যয়</p>
                    <p className="mt-2 text-3xl font-extrabold text-rose-950">৳ {plain(summary.totalExpense)}</p>
                    <p className="mt-1 text-xs text-rose-700">{summary.totalExpenseVouchers} টি খরচ ভাউচার</p>
                  </div>

                  <div
                    className={`rounded-xl border p-5 shadow-sm ${
                      summary.isSurplus
                        ? 'border-indigo-200 bg-indigo-50/50'
                        : 'border-amber-200 bg-amber-50/50'
                    }`}
                  >
                    <p
                      className={`text-xs font-semibold uppercase tracking-wider ${
                        summary.isSurplus ? 'text-indigo-800' : 'text-amber-800'
                      }`}
                    >
                      {summary.isSurplus ? 'নিট লাভ / উদ্বৃত্ত (Surplus)' : 'নিট ঘাটতি / লোকসান (Deficit)'}
                    </p>
                    <p
                      className={`mt-2 text-3xl font-extrabold ${
                        summary.isSurplus ? 'text-indigo-950' : 'text-amber-950'
                      }`}
                    >
                      ৳ {plain(summary.netSurplus)}
                    </p>
                    <p
                      className={`mt-1 text-xs ${
                        summary.isSurplus ? 'text-indigo-700' : 'text-amber-700'
                      }`}
                    >
                      {summary.isSurplus ? 'আয় ও ব্যয়ের উদ্বৃত্ত তহবিল' : 'আয়ের চেয়ে অতিরিক্ত ব্যয়'}
                    </p>
                  </div>

                  <div className="rounded-xl border border-amber-200 bg-amber-50/40 p-5 shadow-sm">
                    <p className="text-xs font-semibold uppercase tracking-wider text-amber-800">মোট শিক্ষার্থী বকেয়া</p>
                    <p className="mt-2 text-3xl font-extrabold text-amber-950">৳ {plain(summary.totalOutstandingDues)}</p>
                    <p className="mt-1 text-xs text-amber-700">সকল শ্রেণির অনাদায়ী ফি</p>
                  </div>
                </div>

                {/* 2 Breakdown Panels */}
                <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
                  {/* Expense Breakdown */}
                  <Card className="p-5">
                    <h3 className="text-base font-semibold text-gray-900 mb-4">ব্যয়ের খাতভিত্তিক বণ্টন</h3>
                    {summary.expenseBreakdown?.length === 0 ? (
                      <EmptyState>নির্বাচিত সময়ে কোনো খরচ রেকর্ড করা হয়নি।</EmptyState>
                    ) : (
                      <div className="space-y-4">
                        {summary.expenseBreakdown?.map((eb) => (
                          <div key={eb.categoryName} className="space-y-1">
                            <div className="flex justify-between text-xs font-medium">
                              <span className="text-gray-700">{eb.categoryName}</span>
                              <span className="text-gray-900 font-bold">
                                ৳ {plain(eb.amount)} ({plain(eb.percentage)}%)
                              </span>
                            </div>
                            <div className="h-2 w-full rounded-full bg-gray-100 overflow-hidden">
                              <div
                                className="h-full bg-rose-500 rounded-full"
                                style={{ width: `${Math.min(100, Number(eb.percentage))}%` }}
                              />
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </Card>

                  {/* Income by Method Breakdown */}
                  <Card className="p-5">
                    <h3 className="text-base font-semibold text-gray-900 mb-4">আয়ের মাধ্যমভিত্তিক বণ্টন</h3>
                    {summary.incomeBreakdown?.length === 0 ? (
                      <EmptyState>নির্বাচিত সময়ে কোনো ফি আদায় রেকর্ড করা হয়নি।</EmptyState>
                    ) : (
                      <div className="space-y-4">
                        {summary.incomeBreakdown?.map((ib) => (
                          <div key={ib.method} className="space-y-1">
                            <div className="flex justify-between text-xs font-medium">
                              <span className="text-gray-700">{ib.methodBangla}</span>
                              <span className="text-gray-900 font-bold">
                                ৳ {plain(ib.amount)} ({plain(ib.percentage)}%)
                              </span>
                            </div>
                            <div className="h-2 w-full rounded-full bg-gray-100 overflow-hidden">
                              <div
                                className="h-full bg-emerald-500 rounded-full"
                                style={{ width: `${Math.min(100, Number(ib.percentage))}%` }}
                              />
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </Card>
                </div>
              </>
            )}
          </LoadState>
        </div>
      )}

      {/* TAB 2: Expense Vouchers List */}
      {activeTab === 'expenses' && (
        <div className="space-y-6">
          {/* Filters */}
          <Card className="p-4 bg-gray-50/50">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4 items-end">
              <div>
                <label className="block text-xs font-semibold text-gray-600 mb-1">ব্যয়ের খাত</label>
                <select
                  value={filterCatId}
                  onChange={(e) => setFilterCatId(e.target.value)}
                  className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none"
                >
                  <option value="">সকল খাত</option>
                  {categories?.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-600 mb-1">শুরুর তারিখ</label>
                <input
                  type="date"
                  value={filterStartDate}
                  onChange={(e) => setFilterStartDate(e.target.value)}
                  className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-600 mb-1">শেষ তারিখ</label>
                <input
                  type="date"
                  value={filterEndDate}
                  onChange={(e) => setFilterEndDate(e.target.value)}
                  className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none"
                />
              </div>

              <div className="flex gap-2">
                <Button
                  variant="secondary"
                  className="flex-1"
                  onClick={() => {
                    setFilterCatId('')
                    setFilterStartDate('')
                    setFilterEndDate('')
                    reloadExpenses()
                  }}
                >
                  রিসেট
                </Button>
                <Button
                  variant="secondary"
                  className="flex-1 border-emerald-600 text-emerald-700 hover:bg-emerald-50"
                  onClick={handleExportExpenses}
                  disabled={exporting}
                >
                  {exporting ? 'তৈরি...' : '📥 এক্সেল'}
                </Button>
              </div>
            </div>
          </Card>

          {/* Expenses Table */}
          <Card>
            <LoadState loading={eLoading} error={eError}>
              {expenses?.length === 0 ? (
                <EmptyState>কোনো খরচ ভাউচার পাওয়া যায়নি।</EmptyState>
              ) : (
                <Table headers={['ভাউচার নং', 'তারিখ', 'ব্যয়ের খাত', 'বিবরণ ও প্রাপক', 'টাকার পরিমাণ', 'মাধ্যম', 'অ্যাকশন']}>
                  {expenses?.map((exp) => (
                    <tr key={exp.id}>
                      <Td className="font-mono text-xs font-semibold text-indigo-700">{exp.voucherNumber}</Td>
                      <Td className="text-gray-600 text-xs">{exp.expenseDate}</Td>
                      <Td className="font-medium text-gray-900">{exp.categoryName}</Td>
                      <Td>
                        <p className="font-medium text-gray-900">{exp.title}</p>
                        <p className="text-xs text-gray-500">
                          প্রাপক: {exp.payeeName} {exp.referenceNo ? `| মেমো: ${exp.referenceNo}` : ''}
                        </p>
                      </Td>
                      <Td className="font-bold text-rose-700 text-base">৳ {plain(exp.amount)}</Td>
                      <Td>
                        <Badge tone="gray">{exp.paymentMethodBangla}</Badge>
                      </Td>
                      <Td>
                        {isAdmin && (
                          <div className="flex gap-2">
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => setVoucherModal({ open: true, item: exp })}
                            >
                              বদলান
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              className="text-red-600 hover:text-red-700"
                              onClick={() => deleteExpense(exp.id)}
                            >
                              মুছুন
                            </Button>
                          </div>
                        )}
                      </Td>
                    </tr>
                  ))}
                </Table>
              )}
            </LoadState>
          </Card>
        </div>
      )}

      {/* TAB 3: Expense Categories Management */}
      {activeTab === 'categories' && (
        <Card>
          <LoadState loading={cLoading}>
            {categories?.length === 0 ? (
              <EmptyState>কোনো ব্যয়ের খাত তৈরি করা হয়নি।</EmptyState>
            ) : (
              <Table headers={['খাতের নাম', 'কোড', 'বিবরণ', 'অবস্থা', 'অ্যাকশন']}>
                {categories?.map((c) => (
                  <tr key={c.id}>
                    <Td className="font-semibold text-gray-900">{c.name}</Td>
                    <Td className="font-mono text-xs font-semibold text-indigo-600">{c.code}</Td>
                    <Td className="text-gray-600">{c.description || '—'}</Td>
                    <Td>
                      <Badge tone={c.active ? 'green' : 'gray'}>
                        {c.active ? 'সক্রিয়' : 'নিষ্ক্রিয়'}
                      </Badge>
                    </Td>
                    <Td>
                      {isAdmin && (
                        <div className="flex gap-2">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => setCatModal({ open: true, item: c })}
                          >
                            সম্পাদনা
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            className="text-red-600 hover:text-red-700"
                            onClick={() => deleteCategory(c.id)}
                          >
                            মুছুন
                          </Button>
                        </div>
                      )}
                    </Td>
                  </tr>
                ))}
              </Table>
            )}
          </LoadState>
        </Card>
      )}

      {/* Expense Category Modal */}
      {catModal.open && (
        <ExpenseCategoryModal
          initial={catModal.item}
          onDone={() => {
            setCatModal({ open: false, item: null })
            reloadCategories()
          }}
          onClose={() => setCatModal({ open: false, item: null })}
        />
      )}

      {/* Expense Voucher Modal */}
      {voucherModal.open && categories && (
        <ExpenseVoucherModal
          initial={voucherModal.item}
          categories={categories}
          onDone={() => {
            setVoucherModal({ open: false, item: null })
            reloadExpenses()
            reloadSummary()
          }}
          onClose={() => setVoucherModal({ open: false, item: null })}
        />
      )}
    </>
  )
}
