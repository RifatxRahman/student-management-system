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

const FREQUENCY_OPTIONS = [
  { value: 'MONTHLY', label: 'মাসিক (প্রতি মাসে নিয়মিত প্রদেয়)', tone: 'brand' },
  { value: 'TERMLY', label: 'টার্ম / সাময়িক (পরীক্ষা বা অর্ধবার্ষিকী)', tone: 'amber' },
  { value: 'YEARLY', label: 'বার্ষিক (সেশনের শুরুতে একবার)', tone: 'green' },
  { value: 'ONE_TIME', label: 'এককালীন (ভর্তির সময়)', tone: 'gray' },
]

function getFrequencyBadge(freq) {
  const item = FREQUENCY_OPTIONS.find((f) => f.value === freq)
  if (!item) return <Badge tone="gray">{freq}</Badge>
  return <Badge tone={item.tone}>{item.label.split(' ')[0]}</Badge>
}

// ----------------- Modal: Fee Head -----------------
function FeeHeadModal({ initial, onDone, onClose }) {
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
        await api(`/api/fee-heads/${initial.id}`, { method: 'PUT', body })
      } else {
        await api('/api/fee-heads', { method: 'POST', body })
      }
      onDone()
    } catch (err) {
      setError(err.message)
      setBusy(false)
    }
  }

  return (
    <Modal title={initial ? 'ফি খাত সম্পাদনা' : 'নতুন ফি খাত তৈরি'} onClose={onClose}>
      <form onSubmit={submit} className="space-y-4">
        {error && <Alert>{error}</Alert>}
        <TextField
          label="ফি খাতের নাম (যেমন: টিউশন ফি, বিজ্ঞান ল্যাব ফি)"
          value={values.name}
          onChange={(e) => setValues({ ...values, name: e.target.value })}
          required
        />
        <TextField
          label="ইউনিক কোড (যেমন: TUITION, LAB, ADM)"
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
            id="headActive"
            type="checkbox"
            checked={values.active}
            onChange={(e) => setValues({ ...values, active: e.target.checked })}
            className="h-4 w-4 rounded border-gray-300 text-[#0E3B2E] focus:ring-[#E8A317]"
          />
          <label htmlFor="headActive" className="text-sm font-medium text-gray-700">
            এই ফি খাত সক্রিয় রাখুন
          </label>
        </div>
        <FormActions busy={busy} onCancel={onClose} submitLabel={initial ? 'সংরক্ষণ করুন' : 'তৈরি করুন'} />
      </form>
    </Modal>
  )
}

// ----------------- Modal: Fee Structure -----------------
function FeeStructureModal({ initial, sessions, classes, feeHeads, defaultSessionId, defaultClassId, onDone, onClose }) {
  const [values, setValues] = useState({
    sessionId: initial?.sessionId || defaultSessionId || (sessions[0]?.id ? String(sessions[0].id) : ''),
    classId: initial?.classId || defaultClassId || (classes[0]?.id ? String(classes[0].id) : ''),
    feeHeadId: initial?.feeHeadId ? String(initial.feeHeadId) : feeHeads[0]?.id ? String(feeHeads[0].id) : '',
    amount: initial?.amount != null ? String(initial.amount) : '',
    frequency: initial?.frequency || 'MONTHLY',
    dueDay: initial?.dueDay != null ? String(initial.dueDay) : '10',
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
        sessionId: Number(values.sessionId),
        classId: Number(values.classId),
        feeHeadId: Number(values.feeHeadId),
        amount: Number(values.amount),
        frequency: values.frequency,
        dueDay: Number(values.dueDay),
        description: values.description || null,
        active: values.active,
      }
      if (initial?.id) {
        await api(`/api/fee-structures/${initial.id}`, { method: 'PUT', body })
      } else {
        await api('/api/fee-structures', { method: 'POST', body })
      }
      onDone()
    } catch (err) {
      setError(err.message)
      setBusy(false)
    }
  }

  return (
    <Modal title={initial ? 'শ্রেণি ফি নিয়ম সম্পাদনা' : 'শ্রেণিতে নতুন ফি নির্ধারণ'} onClose={onClose}>
      <form onSubmit={submit} className="space-y-4">
        {error && <Alert>{error}</Alert>}
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <SelectField
            label="শিক্ষাবর্ষ (সেশন)"
            value={values.sessionId}
            onChange={(e) => setValues({ ...values, sessionId: e.target.value })}
            required
            disabled={!!initial}
          >
            {sessions.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name} {s.active ? '(সক্রিয়)' : ''}
              </option>
            ))}
          </SelectField>

          <SelectField
            label="শ্রেণি (ক্লাস)"
            value={values.classId}
            onChange={(e) => setValues({ ...values, classId: e.target.value })}
            required
            disabled={!!initial}
          >
            {classes.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </SelectField>
        </div>

        <SelectField
          label="ফি খাত (Fee Head)"
          value={values.feeHeadId}
          onChange={(e) => setValues({ ...values, feeHeadId: e.target.value })}
          required
          disabled={!!initial}
        >
          {feeHeads.map((h) => (
            <option key={h.id} value={h.id}>
              {h.name} ({h.code})
            </option>
          ))}
        </SelectField>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <TextField
            label="টাকার পরিমাণ (৳)"
            type="number"
            step="0.01"
            min="0"
            value={values.amount}
            onChange={(e) => setValues({ ...values, amount: e.target.value })}
            required
          />

          <SelectField
            label="ফ্রিকোয়েন্সি"
            value={values.frequency}
            onChange={(e) => setValues({ ...values, frequency: e.target.value })}
            required
          >
            {FREQUENCY_OPTIONS.map((f) => (
              <option key={f.value} value={f.value}>
                {f.label}
              </option>
            ))}
          </SelectField>

          <TextField
            label="পরিশোধের শেষ দিন (১-৩১)"
            type="number"
            min="1"
            max="31"
            value={values.dueDay}
            onChange={(e) => setValues({ ...values, dueDay: e.target.value })}
            required
          />
        </div>

        <TextField
          label="বিশেষ নির্দেশনা বা বিবরণ (ঐচ্ছিক)"
          value={values.description}
          onChange={(e) => setValues({ ...values, description: e.target.value })}
        />

        <div className="flex items-center gap-2 pt-1">
          <input
            id="structureActive"
            type="checkbox"
            checked={values.active}
            onChange={(e) => setValues({ ...values, active: e.target.checked })}
            className="h-4 w-4 rounded border-gray-300 text-[#0E3B2E] focus:ring-[#E8A317]"
          />
          <label htmlFor="structureActive" className="text-sm font-medium text-gray-700">
            এই ফি নিয়ম সক্রিয় রাখুন
          </label>
        </div>

        <FormActions busy={busy} onCancel={onClose} submitLabel={initial ? 'সংরক্ষণ করুন' : 'নির্ধারণ করুন'} />
      </form>
    </Modal>
  )
}

// ----------------- Page Component -----------------
export default function FeeStructurePage() {
  const { user } = useAuth()
  const isAdmin = user.role === 'ADMIN'

  const [activeTab, setActiveTab] = useState('structure') // 'structure' | 'heads' | 'clone'

  // Master data
  const { data: sessions, loading: sLoading } = useQuery('/api/sessions')
  const { data: classes, loading: cLoading } = useQuery('/api/classes')
  const { data: feeHeads, loading: hLoading, reload: reloadHeads } = useQuery('/api/fee-heads')

  // Selected filters for Fee Structure tab
  const [selectedSessionId, setSelectedSessionId] = useState('')
  const [selectedClassId, setSelectedClassId] = useState('')

  // Resolve defaults once sessions/classes load
  const activeSession = sessions?.find((s) => s.active) || sessions?.[0]
  const currentSessionId = selectedSessionId || (activeSession?.id ? String(activeSession.id) : '')
  const firstClass = classes?.[0]
  const currentClassId = selectedClassId || (firstClass?.id ? String(firstClass.id) : '')

  // Query fee structures for selected class & session
  const feeStructureUrl =
    currentSessionId && currentClassId
      ? `/api/fee-structures?sessionId=${currentSessionId}&classId=${currentClassId}`
      : null
  const { data: feeStructures, loading: fsLoading, error: fsError, reload: reloadStructures } = useQuery(feeStructureUrl)

  // Query class fee summary
  const summaryUrl =
    currentSessionId && currentClassId
      ? `/api/fee-structures/summary?sessionId=${currentSessionId}&classId=${currentClassId}`
      : null
  const { data: classSummary, reload: reloadSummary } = useQuery(summaryUrl)

  // State for modals
  const [headModal, setHeadModal] = useState({ open: false, item: null })
  const [structureModal, setStructureModal] = useState({ open: false, item: null })

  // State for Clone Session
  const [cloneSourceId, setCloneSourceId] = useState('')
  const [cloneTargetId, setCloneTargetId] = useState('')
  const [cloneOverwrite, setCloneOverwrite] = useState(false)
  const [cloneBusy, setCloneBusy] = useState(false)
  const [cloneMsg, setCloneMsg] = useState(null)

  async function handleClone(e) {
    e.preventDefault()
    if (!cloneSourceId || !cloneTargetId) return
    setCloneBusy(true)
    setCloneMsg(null)
    try {
      const res = await api('/api/fee-structures/copy', {
        method: 'POST',
        body: {
          sourceSessionId: Number(cloneSourceId),
          targetSessionId: Number(cloneTargetId),
          overwriteExisting: cloneOverwrite,
        },
      })
      setCloneMsg({ type: 'success', text: res.message || 'অনুলিপি সফলভাবে সম্পন্ন হয়েছে!' })
      reloadStructures()
      reloadSummary()
    } catch (err) {
      setCloneMsg({ type: 'error', text: err.message })
    } finally {
      setCloneBusy(false)
    }
  }

  async function deleteStructure(id) {
    if (!window.confirm('আপনি কি নিশ্চিত যে এই ফি নিয়মটি মুছে ফেলতে চান?')) return
    try {
      await api(`/api/fee-structures/${id}`, { method: 'DELETE' })
      reloadStructures()
      reloadSummary()
    } catch (err) {
      alert(err.message)
    }
  }

  async function deleteHead(id) {
    if (!window.confirm('আপনি কি নিশ্চিত যে এই ফি খাতটি মুছে ফেলতে চান?')) return
    try {
      await api(`/api/fee-heads/${id}`, { method: 'DELETE' })
      reloadHeads()
    } catch (err) {
      alert(err.message)
    }
  }

  return (
    <>
      <PageHeader
        title="ফি কাঠামো ও খাত ব্যবস্থাপনা"
        subtitle="শ্রেণিভিত্তিক বেতন ও অন্যান্য ফি নির্ধারণ, ফ্রিকোয়েন্সি এবং সেশন কপি পলিসি"
        action={
          isAdmin && (
            <div className="flex gap-2">
              <Button variant="secondary" onClick={() => setHeadModal({ open: true, item: null })}>
                + নতুন ফি খাত
              </Button>
              <Button onClick={() => setStructureModal({ open: true, item: null })}>
                + শ্রেণি ফি নির্ধারণ
              </Button>
            </div>
          )
        }
      />

      {/* Navigation Tabs */}
      <div className="mb-6 flex border-b border-[var(--border-hairline)]">
        <button
          type="button"
          onClick={() => setActiveTab('structure')}
          className={`border-b-2 px-5 py-2.5 text-sm font-semibold transition ${
            activeTab === 'structure'
              ? 'border-[#0E3B2E] text-[#0E3B2E]'
              : 'border-transparent text-[var(--ink-secondary)] hover:border-gray-300 hover:text-[var(--ink-primary)]'
          }`}
        >
          শ্রেণিভিত্তিক ফি কাঠামো
        </button>
        <button
          type="button"
          onClick={() => setActiveTab('heads')}
          className={`border-b-2 px-5 py-2.5 text-sm font-semibold transition ${
            activeTab === 'heads'
              ? 'border-[#0E3B2E] text-[#0E3B2E]'
              : 'border-transparent text-[var(--ink-secondary)] hover:border-gray-300 hover:text-[var(--ink-primary)]'
          }`}
        >
          ফি খাতসমূহ ({feeHeads?.length || 0})
        </button>
        <button
          type="button"
          onClick={() => setActiveTab('clone')}
          className={`border-b-2 px-5 py-2.5 text-sm font-semibold transition ${
            activeTab === 'clone'
              ? 'border-[#0E3B2E] text-[#0E3B2E]'
              : 'border-transparent text-[var(--ink-secondary)] hover:border-gray-300 hover:text-[var(--ink-primary)]'
          }`}
        >
          সেশন অনুলিপি / কপি
        </button>
      </div>

      {/* TAB 1: Fee Structure Setup */}
      {activeTab === 'structure' && (
        <div className="space-y-6">
          {/* Filter Bar */}
          <Card className="p-4 bg-gray-50/50">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 items-end">
              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-gray-600 mb-1">
                  শিক্ষাবর্ষ (সেশন)
                </label>
                <select
                  value={currentSessionId}
                  onChange={(e) => setSelectedSessionId(e.target.value)}
                  className="w-full rounded-[10px] border border-[var(--border-hairline)] bg-white px-3 py-2 text-sm text-[var(--ink-primary)] focus:border-[#E8A317] focus:ring-2 focus:ring-[#E8A317]/25 focus:outline-none"
                >
                  {sessions?.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.name} {s.active ? '(চলতি সক্রিয়)' : ''}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-gray-600 mb-1">
                  শ্রেণি (ক্লাস)
                </label>
                <select
                  value={currentClassId}
                  onChange={(e) => setSelectedClassId(e.target.value)}
                  className="w-full rounded-[10px] border border-[var(--border-hairline)] bg-white px-3 py-2 text-sm text-[var(--ink-primary)] focus:border-[#E8A317] focus:ring-2 focus:ring-[#E8A317]/25 focus:outline-none"
                >
                  {classes?.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
                </select>
              </div>

              <div className="flex gap-2">
                <Button
                  variant="secondary"
                  className="w-full"
                  onClick={() => {
                    reloadStructures()
                    reloadSummary()
                  }}
                >
                  রিফ্রেশ করুন
                </Button>
              </div>
            </div>
          </Card>

          {/* Live Summary Metrics */}
          {classSummary && (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <div className="rounded-[16px] border border-[var(--border-hairline)] bg-[var(--surface-subtle)] p-4">
                <p className="text-[11px] font-semibold uppercase tracking-wider text-[var(--ink-secondary)]">মোট মাসিক টিউশন</p>
                <p className="mt-1 text-2xl font-bold text-[var(--ink-primary)] font-['Anek_Bangla',sans-serif] tabular-nums">৳ {plain(classSummary.totalMonthlyAmount)}</p>
                <p className="mt-1 text-xs text-[var(--ink-tertiary)]">প্রতি মাসে শিক্ষার্থীর প্রদেয়</p>
              </div>

              <div className="rounded-[16px] border border-[#F6DFC0] bg-[#FDF4E7]/60 p-4">
                <p className="text-[11px] font-semibold uppercase tracking-wider text-[#B36B18]">টার্ম / পরীক্ষা ফি</p>
                <p className="mt-1 text-2xl font-bold text-[#B36B18] font-['Anek_Bangla',sans-serif] tabular-nums">৳ {plain(classSummary.totalTermlyAmount)}</p>
                <p className="mt-1 text-xs text-[#B36B18]">প্রতি টার্মে প্রদেয়</p>
              </div>

              <div className="rounded-[16px] border border-[#BDE3CC] bg-[#EAF5EE]/60 p-4">
                <p className="text-[11px] font-semibold uppercase tracking-wider text-[#168754]">বাৎসরিক সেশন চার্জ</p>
                <p className="mt-1 text-2xl font-bold text-[#168754] font-['Anek_Bangla',sans-serif] tabular-nums">৳ {plain(classSummary.totalYearlyAmount)}</p>
                <p className="mt-1 text-xs text-[#168754]">বছরের শুরুতে একবার</p>
              </div>

              <div className="rounded-[16px] border border-[var(--border-hairline)] bg-[var(--surface-subtle)] p-4">
                <p className="text-[11px] font-semibold uppercase tracking-wider text-[var(--ink-secondary)]">এককালীন / ভর্তি ফি</p>
                <p className="mt-1 text-2xl font-bold text-[var(--ink-primary)] font-['Anek_Bangla',sans-serif] tabular-nums">৳ {plain(classSummary.totalOneTimeAmount)}</p>
                <p className="mt-1 text-xs text-[var(--ink-tertiary)]">ভর্তির সময় প্রযোজ্য</p>
              </div>
            </div>
          )}

          {/* Fee Structure Table */}
          <Card>
            <LoadState loading={fsLoading || sLoading || cLoading} error={fsError}>
              {feeStructures?.length === 0 ? (
                <EmptyState>
                  এই শ্রেণির জন্য এখনো কোনো ফি কাঠামো নির্ধারণ করা হয়নি। উপরের &quot;+ শ্রেণি ফি নির্ধারণ&quot; বাটনে ক্লিক করে
                  ফি যোগ করুন।
                </EmptyState>
              ) : (
                <Table headers={['ফি খাত', 'কোড', 'টাকার পরিমাণ', 'ফ্রিকোয়েন্সি', 'পরিশোধের শেষ তারিখ', 'স্ট্যাটাস', 'অ্যাকশন']}>
                  {feeStructures?.map((fs) => (
                    <tr key={fs.id} className={!fs.active ? 'opacity-60 bg-gray-50' : ''}>
                      <Td className="font-semibold text-gray-900">
                        {fs.feeHeadName}
                        {fs.description && <p className="text-xs text-gray-500 font-normal">{fs.description}</p>}
                      </Td>
                      <Td className="font-mono text-xs">{fs.feeHeadCode}</Td>
                      <Td className="font-bold text-[var(--ink-primary)] text-base">৳ {plain(fs.amount)}</Td>
                      <Td>{getFrequencyBadge(fs.frequency)}</Td>
                      <Td>প্রতি মাসের {fs.dueDay} তারিখ</Td>
                      <Td>
                        <Badge tone={fs.active ? 'green' : 'gray'}>
                          {fs.active ? 'সক্রিয়' : 'নিষ্ক্রিয়'}
                        </Badge>
                      </Td>
                      <Td>
                        {isAdmin && (
                          <div className="flex gap-2">
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => setStructureModal({ open: true, item: fs })}
                            >
                              বদলান
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              className="text-red-600 hover:text-red-700"
                              onClick={() => deleteStructure(fs.id)}
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

      {/* TAB 2: Fee Heads Management */}
      {activeTab === 'heads' && (
        <Card>
          <LoadState loading={hLoading}>
            {feeHeads?.length === 0 ? (
              <EmptyState>কোনো ফি খাত তৈরি করা হয়নি।</EmptyState>
            ) : (
              <Table headers={['খাতের নাম', 'ইউনিক কোড', 'বিবরণ', 'অবস্থা', 'অ্যাকশন']}>
                {feeHeads?.map((h) => (
                  <tr key={h.id}>
                    <Td className="font-semibold text-gray-900">{h.name}</Td>
                    <Td className="font-mono text-xs font-semibold text-[var(--ink-secondary)]">{h.code}</Td>
                    <Td className="text-gray-600">{h.description || '—'}</Td>
                    <Td>
                      <Badge tone={h.active ? 'green' : 'gray'}>
                        {h.active ? 'সক্রিয়' : 'নিষ্ক্রিয়'}
                      </Badge>
                    </Td>
                    <Td>
                      {isAdmin && (
                        <div className="flex gap-2">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => setHeadModal({ open: true, item: h })}
                          >
                            সম্পাদনা
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            className="text-red-600 hover:text-red-700"
                            onClick={() => deleteHead(h.id)}
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

      {/* TAB 3: Clone / Copy Session Fee Matrix */}
      {activeTab === 'clone' && (
        <Card className="max-w-2xl p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-2">সেশন ফি অনুলিপি (Clone Fee Matrix)</h3>
          <p className="text-sm text-gray-600 mb-6">
            নতুন শিক্ষাবর্ষ শুরুর সময় পূর্ববর্তী শিক্ষাবর্ষের সকল শ্রেণির ফি নিয়মাবলি এক ক্লিকে নতুন শিক্ষাবর্ষে অনুলিপি
            করুন। এতে প্রতি ক্লাসের জন্য আলাদা আলাদা ফি পুনরায় সেট করার পরিশ্রম বাঁচে।
          </p>

          {cloneMsg && (
            <div className="mb-4">
              <Alert type={cloneMsg.type}>{cloneMsg.text}</Alert>
            </div>
          )}

          <form onSubmit={handleClone} className="space-y-4">
            <SelectField
              label="উৎস শিক্ষাবর্ষ (যেখান থেকে কপি করবেন)"
              value={cloneSourceId}
              onChange={(e) => setCloneSourceId(e.target.value)}
              required
            >
              <option value="">সেশন নির্বাচন করুন</option>
              {sessions?.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name}
                </option>
              ))}
            </SelectField>

            <SelectField
              label="টার্গেট শিক্ষাবর্ষ (যে সেশনে কপি করা হবে)"
              value={cloneTargetId}
              onChange={(e) => setCloneTargetId(e.target.value)}
              required
            >
              <option value="">সেশন নির্বাচন করুন</option>
              {sessions?.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name}
                </option>
              ))}
            </SelectField>

            <div className="flex items-center gap-2 pt-2">
              <input
                id="overwrite"
                type="checkbox"
                checked={cloneOverwrite}
                onChange={(e) => setCloneOverwrite(e.target.checked)}
                className="h-4 w-4 rounded border-gray-300 text-[#0E3B2E] focus:ring-[#E8A317]"
              />
              <label htmlFor="overwrite" className="text-sm text-gray-700">
                টার্গেট সেশনে ইতিমধ্যে বিদ্যমান ফি কাঠামো থাকলে তা প্রতিস্থাপন (Overwrite) করুন
              </label>
            </div>

            <div className="pt-4">
              <Button type="submit" disabled={cloneBusy || !cloneSourceId || !cloneTargetId}>
                {cloneBusy ? 'অনুলিপি হচ্ছে...' : 'ফি অনুলিপি করুন'}
              </Button>
            </div>
          </form>
        </Card>
      )}

      {/* Fee Head Modal */}
      {headModal.open && (
        <FeeHeadModal
          initial={headModal.item}
          onDone={() => {
            setHeadModal({ open: false, item: null })
            reloadHeads()
          }}
          onClose={() => setHeadModal({ open: false, item: null })}
        />
      )}

      {/* Fee Structure Modal */}
      {structureModal.open && sessions && classes && feeHeads && (
        <FeeStructureModal
          initial={structureModal.item}
          sessions={sessions}
          classes={classes}
          feeHeads={feeHeads}
          defaultSessionId={currentSessionId}
          defaultClassId={currentClassId}
          onDone={() => {
            setStructureModal({ open: false, item: null })
            reloadStructures()
            reloadSummary()
          }}
          onClose={() => setStructureModal({ open: false, item: null })}
        />
      )}
    </>
  )
}
