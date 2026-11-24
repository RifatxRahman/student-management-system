import { useState } from 'react'
import { api } from '../api/client.js'
import { useAuth } from '../auth/useAuth.js'
import { FormActions, Pagination } from '../components/extra.jsx'
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
import { formatDate } from '../utils/format.js'

const AUDIENCE_OPTIONS = [
  { value: 'ALL', label: 'সকল শিক্ষার্থী ও অভিভাবক (School Wide)' },
  { value: 'CLASS', label: 'নির্দিষ্ট শ্রেণি (Class Wise)' },
  { value: 'SECTION', label: 'নির্দিষ্ট সেকশন (Section Wise)' },
  { value: 'FEE_DEFAULTERS', label: 'বকেয়া ফি প্রাপক শিক্ষার্থী (Fee Defaulters)' },
]

const TEMPLATE_CATEGORIES = [
  { value: 'FEE_DUE', label: 'ফি বকেয়া নোটিশ', tone: 'amber' },
  { value: 'ABSENCE', label: 'অনুপস্থিতি অ্যালার্ট', tone: 'rose' },
  { value: 'EXAM_NOTICE', label: 'পরীক্ষার নোটিশ', tone: 'brand' },
  { value: 'RESULT_PUBLISH', label: 'ফলাফল প্রকাশ', tone: 'green' },
  { value: 'ANNOUNCEMENT', label: 'সাধারণ ঘোষণা', tone: 'gray' },
]

function getCategoryBadge(cat) {
  const found = TEMPLATE_CATEGORIES.find((c) => c.value === cat)
  if (!found) return <Badge tone="gray">{cat}</Badge>
  return <Badge tone={found.tone}>{found.label}</Badge>
}

// ----------------- Modal: Notification Template -----------------
function TemplateModal({ initial, onDone, onClose }) {
  const [values, setValues] = useState({
    name: initial?.name || '',
    code: initial?.code || '',
    channel: initial?.channel || 'SMS',
    category: initial?.category || 'ANNOUNCEMENT',
    subject: initial?.subject || '',
    templateBody: initial?.templateBody || '',
    active: initial?.active ?? true,
  })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  function insertVar(v) {
    setValues((prev) => ({ ...prev, templateBody: prev.templateBody + v }))
  }

  async function submit(e) {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      const body = {
        name: values.name,
        code: values.code,
        channel: values.channel,
        category: values.category,
        subject: values.subject || null,
        templateBody: values.templateBody,
        variables: 'student_name,class_name,roll_no,due_amount,school_name,date',
        active: values.active,
      }
      if (initial?.id) {
        await api(`/api/notification-templates/${initial.id}`, { method: 'PUT', body })
      } else {
        await api('/api/notification-templates', { method: 'POST', body })
      }
      onDone()
    } catch (err) {
      setError(err.message)
      setBusy(false)
    }
  }

  return (
    <Modal title={initial ? 'টেমপ্লেট সম্পাদনা' : 'নতুন মেসেজ টেমপ্লেট'} onClose={onClose}>
      <form onSubmit={submit} className="space-y-4">
        {error && <Alert>{error}</Alert>}
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <TextField
            label="টেমপ্লেটের নাম"
            value={values.name}
            onChange={(e) => setValues({ ...values, name: e.target.value })}
            placeholder="যেমন: বকেয়া ফি তাগাদা"
            required
          />

          <TextField
            label="ইউনিক কোড"
            value={values.code}
            onChange={(e) => setValues({ ...values, code: e.target.value.toUpperCase() })}
            placeholder="যেমন: FEE_REMINDER_BN"
            required
          />
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <SelectField
            label="ক্যাটাগরি"
            value={values.category}
            onChange={(e) => setValues({ ...values, category: e.target.value })}
            required
          >
            {TEMPLATE_CATEGORIES.map((c) => (
              <option key={c.value} value={c.value}>
                {c.label}
              </option>
            ))}
          </SelectField>

          <SelectField
            label="মাধ্যম (Channel)"
            value={values.channel}
            onChange={(e) => setValues({ ...values, channel: e.target.value })}
            required
          >
            <option value="SMS">এসএমএস (SMS)</option>
            <option value="EMAIL">ইমেইল (Email)</option>
            <option value="BOTH">উভয় (SMS & Email)</option>
          </SelectField>
        </div>

        <div>
          <label className="block text-xs font-semibold text-[var(--ink-secondary)] mb-1">
            বার্তা বা মেসেজের বডি (ডাইনামিক ভ্যারিয়েবল সহ)
          </label>
          <div className="mb-2 flex flex-wrap gap-1.5 text-xs">
            <span className="text-[var(--ink-tertiary)] py-1">ভ্যারিয়েবল যোগ করুন:</span>
            <button type="button" onClick={() => insertVar('{student_name}')} className="rounded-[6px] bg-[var(--surface-subtle)] border border-[var(--border-hairline)] text-[var(--ink-primary)] px-2 py-0.5 hover:bg-[var(--surface-hover)] transition">
              + শিক্ষার্থী নাম
            </button>
            <button type="button" onClick={() => insertVar('{class_name}')} className="rounded-[6px] bg-[var(--surface-subtle)] border border-[var(--border-hairline)] text-[var(--ink-primary)] px-2 py-0.5 hover:bg-[var(--surface-hover)] transition">
              + শ্রেণি
            </button>
            <button type="button" onClick={() => insertVar('{roll_no}')} className="rounded-[6px] bg-[var(--surface-subtle)] border border-[var(--border-hairline)] text-[var(--ink-primary)] px-2 py-0.5 hover:bg-[var(--surface-hover)] transition">
              + রোল
            </button>
            <button type="button" onClick={() => insertVar('{due_amount}')} className="rounded-[6px] bg-[var(--surface-subtle)] border border-[var(--border-hairline)] text-[var(--ink-primary)] px-2 py-0.5 hover:bg-[var(--surface-hover)] transition">
              + বকেয়া
            </button>
            <button type="button" onClick={() => insertVar('{school_name}')} className="rounded-[6px] bg-[var(--surface-subtle)] border border-[var(--border-hairline)] text-[var(--ink-primary)] px-2 py-0.5 hover:bg-[var(--surface-hover)] transition">
              + বিদ্যালয়
            </button>
            <button type="button" onClick={() => insertVar('{date}')} className="rounded-[6px] bg-[var(--surface-subtle)] border border-[var(--border-hairline)] text-[var(--ink-primary)] px-2 py-0.5 hover:bg-[var(--surface-hover)] transition">
              + তারিখ
            </button>
          </div>
          <textarea
            rows={4}
            value={values.templateBody}
            onChange={(e) => setValues({ ...values, templateBody: e.target.value })}
            className="w-full rounded-[10px] border border-[var(--border-hairline)] bg-[var(--surface-subtle)] p-3 text-sm text-[var(--ink-primary)] focus:border-[#E8A317] focus:ring-2 focus:ring-[#E8A317]/25 focus:outline-none"
            required
          />
        </div>

        <div className="flex items-center gap-2 pt-1">
          <input
            id="templateActive"
            type="checkbox"
            checked={values.active}
            onChange={(e) => setValues({ ...values, active: e.target.checked })}
            className="h-4 w-4 rounded border-gray-300 text-[#0E3B2E] focus:ring-[#E8A317]"
          />
          <label htmlFor="templateActive" className="text-sm font-medium text-[var(--ink-secondary)]">
            এই টেমপ্লেট সক্রিয় রাখুন
          </label>
        </div>

        <FormActions busy={busy} onCancel={onClose} submitLabel={initial ? 'সংরক্ষণ করুন' : 'টেমপ্লেট তৈরি করুন'} />
      </form>
    </Modal>
  )
}

// ----------------- Modal: Send Campaign SMS -----------------
function SendCampaignModal({ classes, templates, onDone, onClose }) {
  const [title, setTitle] = useState('')
  const [audience, setAudience] = useState('ALL')
  const [classId, setClassId] = useState('')
  const [sectionId, setSectionId] = useState('')
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  const { data: sections } = useQuery(
    audience === 'SECTION' && classId ? `/api/classes/${classId}/sections` : null
  )

  const isUnicode = /[\u0980-\u09FF]/.test(message)
  const charCount = message.length
  let smsParts = 0
  if (charCount > 0) {
    if (isUnicode) {
      smsParts = charCount <= 70 ? 1 : Math.ceil(charCount / 67)
    } else {
      smsParts = charCount <= 160 ? 1 : Math.ceil(charCount / 153)
    }
  }

  function handleSelectTemplate(code) {
    if (!code) return
    const t = templates?.find((x) => x.code === code)
    if (t) {
      setMessage(t.templateBody)
      if (!title) setTitle(t.name)
    }
  }

  function insertVar(v) {
    setMessage((prev) => prev + v)
  }

  async function submit(e) {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      const body = {
        title,
        channel: 'SMS',
        targetAudience: audience,
        targetClassId: classId ? Number(classId) : null,
        targetSectionId: sectionId ? Number(sectionId) : null,
        message,
      }
      const res = await api('/api/notification-campaigns', { method: 'POST', body })
      onDone(res)
    } catch (err) {
      setError(err.message)
      setBusy(false)
    }
  }

  return (
    <Modal title="নতুন এসএমএস প্রেরণ ও ব্রডকাস্ট" onClose={onClose}>
      <form onSubmit={submit} className="space-y-4">
        {error && <Alert>{error}</Alert>}
        <TextField
          label="ক্যাম্পেইন বা বার্তার শিরোনাম"
          placeholder="যেমন: নভেম্বর মাসের বকেয়া ফি তাগাদা"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          required
        />

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <SelectField
            label="টার্গেট প্রাপক গ্রুপ"
            value={audience}
            onChange={(e) => {
              setAudience(e.target.value)
              if (e.target.value !== 'SECTION') setSectionId('')
              if (e.target.value === 'ALL' || e.target.value === 'FEE_DEFAULTERS') setClassId('')
            }}
            required
          >
            {AUDIENCE_OPTIONS.map((a) => (
              <option key={a.value} value={a.value}>
                {a.label}
              </option>
            ))}
          </SelectField>

          <SelectField
            label="টেমপ্লেট থেকে লোড করুন (ঐচ্ছিক)"
            onChange={(e) => handleSelectTemplate(e.target.value)}
          >
            <option value="">কাস্টম মেসেজ লিখুন</option>
            {templates?.map((t) => (
              <option key={t.code} value={t.code}>
                {t.name}
              </option>
            ))}
          </SelectField>
        </div>

        {audience === 'CLASS' && (
          <SelectField
            label="শ্রেণি নির্বাচন"
            value={classId}
            onChange={(e) => setClassId(e.target.value)}
            required
          >
            <option value="">শ্রেণি পছন্দ করুন</option>
            {classes?.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </SelectField>
        )}

        {audience === 'SECTION' && (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <SelectField
              label="শ্রেণি নির্বাচন"
              value={classId}
              onChange={(e) => {
                setClassId(e.target.value)
                setSectionId('')
              }}
              required
            >
              <option value="">শ্রেণি পছন্দ করুন</option>
              {classes?.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </SelectField>

            <SelectField
              label="সেকশন নির্বাচন"
              value={sectionId}
              onChange={(e) => setSectionId(e.target.value)}
              required
              disabled={!classId}
            >
              <option value="">{classId ? 'সেকশন পছন্দ করুন' : 'আগে শ্রেণি পছন্দ করুন'}</option>
              {sections?.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name}
                </option>
              ))}
            </SelectField>
          </div>
        )}

        <div>
          <div className="flex justify-between items-center mb-1">
            <label className="block text-xs font-semibold text-[var(--ink-secondary)]">এসএমএস মেসেজ বডি</label>
            <span className="text-xs font-mono text-[var(--ink-tertiary)]">
              {charCount} অক্ষর | {smsParts} টি SMS ({isUnicode ? 'বাংলা' : 'ইংরেজি'})
            </span>
          </div>

          <div className="mb-2 flex flex-wrap gap-1.5 text-xs">
            <button type="button" onClick={() => insertVar('{student_name}')} className="rounded-[6px] bg-[var(--surface-subtle)] text-[var(--ink-primary)] border border-[var(--border-hairline)] px-2 py-0.5 hover:bg-[var(--surface-hover)] font-medium transition">
              + নাম
            </button>
            <button type="button" onClick={() => insertVar('{class_name}')} className="rounded-[6px] bg-[var(--surface-subtle)] text-[var(--ink-primary)] border border-[var(--border-hairline)] px-2 py-0.5 hover:bg-[var(--surface-hover)] font-medium transition">
              + শ্রেণি
            </button>
            <button type="button" onClick={() => insertVar('{due_amount}')} className="rounded-[6px] bg-[var(--surface-subtle)] text-[var(--ink-primary)] border border-[var(--border-hairline)] px-2 py-0.5 hover:bg-[var(--surface-hover)] font-medium transition">
              + বকেয়া
            </button>
            <button type="button" onClick={() => insertVar('{school_name}')} className="rounded-[6px] bg-[var(--surface-subtle)] text-[var(--ink-primary)] border border-[var(--border-hairline)] px-2 py-0.5 hover:bg-[var(--surface-hover)] font-medium transition">
              + প্রতিষ্ঠান
            </button>
          </div>

          <textarea
            rows={4}
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            placeholder="এসএমএস টেক্সট লিখুন..."
            className="w-full rounded-[10px] border border-[var(--border-hairline)] bg-[var(--surface-subtle)] p-3 text-sm text-[var(--ink-primary)] focus:border-[#E8A317] focus:ring-2 focus:ring-[#E8A317]/25 focus:outline-none"
            required
          />
        </div>

        <div className="rounded-[10px] bg-[var(--surface-subtle)] border border-[var(--border-hairline)] p-2.5 text-xs flex justify-between items-center text-[var(--ink-secondary)]">
          <div>
            <span className="font-semibold text-[var(--ink-primary)]">কাউন্টার: </span>
            {charCount} ক্যারেক্টার | {smsParts} পার্ট
          </div>
          <Badge tone={isUnicode ? 'brand' : 'green'}>
            {isUnicode ? 'বাংলা ইউনিকোড (৭০ অক্ষর/পার্ট)' : 'ইংরেজি GSM (১৬০ অক্ষর/পার্ট)'}
          </Badge>
        </div>

        <FormActions busy={busy} onCancel={onClose} submitLabel="এসএমএস প্রেরণ করুন" />
      </form>
    </Modal>
  )
}

// ----------------- Main Page Component -----------------
export default function NotificationsPage() {
  const { user } = useAuth()
  const isAdmin = user.role === 'ADMIN'

  const [activeTab, setActiveTab] = useState('campaign') // 'campaign' | 'templates' | 'logs'

  // Campaign Form State
  const [campTitle, setCampTitle] = useState('')
  const [campAudience, setCampAudience] = useState('ALL')
  const [campClassId, setCampClassId] = useState('')
  const [campSectionId, setCampSectionId] = useState('')
  const [campMessage, setCampMessage] = useState('')
  const [campBusy, setCampBusy] = useState(false)
  const [campNotice, setCampNotice] = useState(null)

  // Log View State
  const [logStatus, setLogStatus] = useState('')
  const [logPage, setLogPage] = useState(0)
  const [logBusy, setLogBusy] = useState(false)
  const [logNotice, setLogNotice] = useState(null)

  // Template Modal State
  const [tempModal, setTempModal] = useState({ open: false, item: null })
  const [sendSmsModal, setSendSmsModal] = useState(false)

  // Master Data
  const { data: classes } = useQuery('/api/classes')
  const { data: templates, reload: reloadTemplates } = useQuery('/api/notification-templates')
  const { data: campaigns, reload: reloadCampaigns } = useQuery('/api/notification-campaigns')
  const { data: campSections } = useQuery(
    campAudience === 'SECTION' && campClassId ? `/api/classes/${campClassId}/sections` : null
  )

  const logParams = new URLSearchParams({ page: String(logPage), size: '20' })
  if (logStatus) logParams.set('status', logStatus)
  const { data: logData, error: logError, loading: logLoading, reload: reloadLogs } = useQuery(
    `/api/admin/notifications?${logParams}`
  )

  // SMS parts calculation helper
  const isUnicode = /[\u0980-\u09FF]/.test(campMessage)
  const charCount = campMessage.length
  let smsParts = 0
  if (charCount > 0) {
    if (isUnicode) {
      smsParts = charCount <= 70 ? 1 : Math.ceil(charCount / 67)
    } else {
      smsParts = charCount <= 160 ? 1 : Math.ceil(charCount / 153)
    }
  }

  function handleSelectTemplate(code) {
    if (!code) return
    const t = templates?.find((x) => x.code === code)
    if (t) {
      setCampMessage(t.templateBody)
      if (!campTitle) setCampTitle(t.name)
    }
  }

  function insertVarToCampaign(v) {
    setCampMessage((prev) => prev + v)
  }

  async function handleSendCampaign(e) {
    e.preventDefault()
    setCampBusy(true)
    setCampNotice(null)
    try {
      const body = {
        title: campTitle,
        channel: 'SMS',
        targetAudience: campAudience,
        targetClassId: campClassId ? Number(campClassId) : null,
        targetSectionId: campSectionId ? Number(campSectionId) : null,
        message: campMessage,
      }
      const res = await api('/api/notification-campaigns', { method: 'POST', body })
      setCampNotice({
        type: res.failedCount > 0 ? 'error' : 'success',
        text: `ক্যাম্পেইন সম্পন্ন! মোট প্রাপক: ${res.totalRecipients} জন, সফল: ${res.sentCount} টি, ব্যর্থ: ${res.failedCount} টি।`,
      })
      reloadCampaigns()
      reloadLogs()
      setCampTitle('')
      setCampMessage('')
    } catch (err) {
      setCampNotice({ type: 'error', text: err.message })
    } finally {
      setCampBusy(false)
    }
  }

  async function dispatchQueue() {
    setLogBusy(true)
    setLogNotice(null)
    try {
      const result = await api('/api/admin/notifications/dispatch', { method: 'POST' })
      setLogNotice({
        type: result.failed > 0 ? 'error' : 'success',
        text: `পাঠানো ${result.sent}, ব্যর্থ ${result.failed}। ${result.note}`,
      })
      reloadLogs()
    } catch (err) {
      setLogNotice({ type: 'error', text: err.message })
    } finally {
      setLogBusy(false)
    }
  }

  async function retryLog(id) {
    setLogNotice(null)
    try {
      await api(`/api/admin/notifications/${id}/retry`, { method: 'POST' })
      reloadLogs()
    } catch (err) {
      setLogNotice({ type: 'error', text: err.message })
    }
  }

  async function deleteTemplate(id) {
    if (!window.confirm('আপনি কি নিশ্চিত যে এই টেমপ্লেটটি মুছে ফেলতে চান?')) return
    try {
      await api(`/api/notification-templates/${id}`, { method: 'DELETE' })
      reloadTemplates()
    } catch (err) {
      alert(err.message)
    }
  }

  return (
    <>
      <PageHeader
        title="মেসেজ ও নোটিফিকেশন সেন্টার"
        subtitle="অভিভাবক অ্যালার্ট টেমপ্লেট, বাল্ক এসএমএস সম্প্রচার, বকেয়া তাগাদা ও ডেলিভারি হিস্ট্রি"
        action={
          isAdmin && (
            <div className="flex gap-2">
              <Button variant="secondary" onClick={() => setTempModal({ open: true, item: null })}>
                + নতুন টেমপ্লেট
              </Button>
              <Button onClick={() => setSendSmsModal(true)}>
                + এসএমএস প্রেরণ
              </Button>
            </div>
          )
        }
      />

      {/* Tabs */}
      <div className="mb-6 flex border-b border-[var(--border-hairline)]">
        <button
          type="button"
          onClick={() => setActiveTab('campaign')}
          className={`border-b-2 px-5 py-2.5 text-sm font-semibold transition ${
            activeTab === 'campaign'
              ? 'border-[#0E3B2E] dark:border-[#E8A317] text-[#0E3B2E] dark:text-[#E8A317]'
              : 'border-transparent text-[var(--ink-secondary)] hover:border-[var(--border-hairline)] hover:text-[var(--ink-primary)]'
          }`}
        >
          বাল্ক এসএমএস প্রেরণ ও ক্যাম্পেইন
        </button>
        <button
          type="button"
          onClick={() => setActiveTab('templates')}
          className={`border-b-2 px-5 py-2.5 text-sm font-semibold transition ${
            activeTab === 'templates'
              ? 'border-[#0E3B2E] dark:border-[#E8A317] text-[#0E3B2E] dark:text-[#E8A317]'
              : 'border-transparent text-[var(--ink-secondary)] hover:border-[var(--border-hairline)] hover:text-[var(--ink-primary)]'
          }`}
        >
          মেসেজ টেমপ্লেটসমূহ ({templates?.length || 0})
        </button>
        <button
          type="button"
          onClick={() => setActiveTab('logs')}
          className={`border-b-2 px-5 py-2.5 text-sm font-semibold transition ${
            activeTab === 'logs'
              ? 'border-[#0E3B2E] dark:border-[#E8A317] text-[#0E3B2E] dark:text-[#E8A317]'
              : 'border-transparent text-[var(--ink-secondary)] hover:border-[var(--border-hairline)] hover:text-[var(--ink-primary)]'
          }`}
        >
          ডেলিভারি লগ ও কিউ
        </button>
      </div>

      {/* TAB 1: Bulk Campaign & Send SMS */}
      {activeTab === 'campaign' && (
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
          {/* Send SMS Form */}
          <div className="lg:col-span-2">
            <Card className="p-6">
              <h3 className="text-base font-semibold text-[var(--ink-primary)] mb-2">নোটিফিকেশন ও এসএমএস ব্রডকাস্ট</h3>
              <p className="text-xs text-[var(--ink-secondary)] mb-6">
                নির্দিষ্ট গ্রুপ বা সম্পূর্ণ স্কুলের অভিভাবকদের জন্য স্বয়ংক্রিয় ভ্যারিয়েবল প্রতিস্থাপন সহ এসএমএস প্রেরণ করুন।
              </p>

              {campNotice && (
                <div className="mb-4">
                  <Alert type={campNotice.type}>{campNotice.text}</Alert>
                </div>
              )}

              <form onSubmit={handleSendCampaign} className="space-y-4">
                <TextField
                  label="ক্যাম্পেইন বা বার্তার শিরোনাম"
                  placeholder="যেমন: নভেম্বর মাসের বকেয়া ফি তাগাদা"
                  value={campTitle}
                  onChange={(e) => setCampTitle(e.target.value)}
                  required
                />

                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                  <SelectField
                    label="টার্গেট প্রাপক গ্রুপ"
                    value={campAudience}
                    onChange={(e) => {
                      setCampAudience(e.target.value)
                      if (e.target.value !== 'SECTION') setCampSectionId('')
                      if (e.target.value === 'ALL' || e.target.value === 'FEE_DEFAULTERS') setCampClassId('')
                    }}
                    required
                  >
                    {AUDIENCE_OPTIONS.map((a) => (
                      <option key={a.value} value={a.value}>
                        {a.label}
                      </option>
                    ))}
                  </SelectField>

                  <SelectField
                    label="টেমপ্লেট থেকে লোড করুন (ঐচ্ছিক)"
                    onChange={(e) => handleSelectTemplate(e.target.value)}
                  >
                    <option value="">কাস্টম মেসেজ লিখুন</option>
                    {templates?.map((t) => (
                      <option key={t.code} value={t.code}>
                        {t.name}
                      </option>
                    ))}
                  </SelectField>
                </div>

                {campAudience === 'CLASS' && (
                  <SelectField
                    label="শ্রেণি নির্বাচন"
                    value={campClassId}
                    onChange={(e) => setCampClassId(e.target.value)}
                    required
                  >
                    <option value="">শ্রেণি পছন্দ করুন</option>
                    {classes?.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name}
                      </option>
                    ))}
                  </SelectField>
                )}

                {campAudience === 'SECTION' && (
                  <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                    <SelectField
                      label="শ্রেণি নির্বাচন"
                      value={campClassId}
                      onChange={(e) => {
                        setCampClassId(e.target.value)
                        setCampSectionId('')
                      }}
                      required
                    >
                      <option value="">শ্রেণি পছন্দ করুন</option>
                      {classes?.map((c) => (
                        <option key={c.id} value={c.id}>
                          {c.name}
                        </option>
                      ))}
                    </SelectField>

                    <SelectField
                      label="সেকশন নির্বাচন"
                      value={campSectionId}
                      onChange={(e) => setCampSectionId(e.target.value)}
                      required
                      disabled={!campClassId}
                    >
                      <option value="">{campClassId ? 'সেকশন পছন্দ করুন' : 'আগে শ্রেণি পছন্দ করুন'}</option>
                      {campSections?.map((s) => (
                        <option key={s.id} value={s.id}>
                          {s.name}
                        </option>
                      ))}
                    </SelectField>
                  </div>
                )}

                <div>
                  <div className="flex justify-between items-center mb-1">
                    <label className="block text-xs font-semibold text-gray-700">এসএমএস মেসেজ বডি</label>
                    <span className="text-xs font-mono text-gray-500">
                      {charCount} অক্ষর | {smsParts} টি SMS ({isUnicode ? 'বাংলা' : 'ইংরেজি'})
                    </span>
                  </div>

                  <div className="mb-2 flex flex-wrap gap-1.5 text-xs">
                    <button type="button" onClick={() => insertVarToCampaign('{student_name}')} className="rounded-[6px] bg-[var(--surface-subtle)] text-[var(--ink-primary)] border border-[var(--border-hairline)] px-2 py-0.5 hover:bg-[var(--surface-hover)] font-medium transition">
                      + নাম
                    </button>
                    <button type="button" onClick={() => insertVarToCampaign('{class_name}')} className="rounded-[6px] bg-[var(--surface-subtle)] text-[var(--ink-primary)] border border-[var(--border-hairline)] px-2 py-0.5 hover:bg-[var(--surface-hover)] font-medium transition">
                      + শ্রেণি
                    </button>
                    <button type="button" onClick={() => insertVarToCampaign('{due_amount}')} className="rounded-[6px] bg-[var(--surface-subtle)] text-[var(--ink-primary)] border border-[var(--border-hairline)] px-2 py-0.5 hover:bg-[var(--surface-hover)] font-medium transition">
                      + বকেয়া
                    </button>
                    <button type="button" onClick={() => insertVarToCampaign('{school_name}')} className="rounded-[6px] bg-[var(--surface-subtle)] text-[var(--ink-primary)] border border-[var(--border-hairline)] px-2 py-0.5 hover:bg-[var(--surface-hover)] font-medium transition">
                      + প্রতিষ্ঠান
                    </button>
                  </div>

                  <textarea
                    rows={5}
                    value={campMessage}
                    onChange={(e) => setCampMessage(e.target.value)}
                    placeholder="এসএমএস টেক্সট লিখুন..."
                    className="w-full rounded-[10px] border border-[var(--border-hairline)] bg-[var(--surface-subtle)] p-3 text-sm text-[var(--ink-primary)] focus:border-[#E8A317] focus:ring-2 focus:ring-[#E8A317]/25 focus:outline-none"
                    required
                  />
                </div>

                {/* SMS Parts & Cost Live Card */}
                <div className="rounded-[10px] bg-[var(--surface-subtle)] border border-[var(--border-hairline)] p-3 text-xs flex justify-between items-center text-[var(--ink-secondary)]">
                  <div>
                    <span className="font-semibold text-[var(--ink-primary)]">কাউন্টার: </span>
                    {charCount} ক্যারেক্টার | প্রতিটি এসএমএস: {smsParts} পার্ট
                  </div>
                  <Badge tone={isUnicode ? 'brand' : 'green'}>
                    {isUnicode ? 'বাংলা ইউনিকোড (৭০ অক্ষর/পার্ট)' : 'ইংরেজি GSM (১৬০ অক্ষর/পার্ট)'}
                  </Badge>
                </div>

                <div className="pt-2">
                  <Button type="submit" disabled={campBusy || !campMessage || !campTitle}>
                    {campBusy ? 'মেসেজ প্রেরণ হচ্ছে...' : 'এসএমএস প্রেরণ করুন'}
                  </Button>
                </div>
              </form>
            </Card>
          </div>

          {/* Recent Campaigns Sidebar */}
          <div>
            <Card className="p-5">
              <h4 className="text-sm font-semibold text-[var(--ink-primary)] mb-3">সাম্প্রতিক ক্যাম্পেইন হিস্ট্রি</h4>
              {campaigns?.length === 0 ? (
                <EmptyState>কোনো ক্যাম্পেইন তৈরি করা হয়নি।</EmptyState>
              ) : (
                <div className="space-y-3">
                  {campaigns?.slice(0, 6).map((c) => (
                    <div key={c.id} className="rounded-[10px] border border-[var(--border-hairline)] p-3 text-xs space-y-1 bg-[var(--surface-subtle)]">
                      <p className="font-semibold text-[var(--ink-primary)]">{c.title}</p>
                      <p className="text-[var(--ink-secondary)] font-mono line-clamp-2">{c.message}</p>
                      <div className="flex justify-between items-center pt-1 text-[var(--ink-tertiary)]">
                        <span>প্রাপক: {c.totalRecipients} জন</span>
                        <span className="text-[#168754] dark:text-emerald-400 font-medium">সফল: {c.sentCount}</span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </Card>
          </div>
        </div>
      )}

      {/* TAB 2: Notification Templates */}
      {activeTab === 'templates' && (
        <Card>
          {templates?.length === 0 ? (
            <EmptyState>কোনো নোটিফিকেশন টেমপ্লেট নেই। উপরের &quot;+ নতুন টেমপ্লেট&quot; বাটনে ক্লিক করে তৈরি করুন।</EmptyState>
          ) : (
            <Table headers={['টেমপ্লেট নাম', 'ইউনিক কোড', 'ক্যাটাগরি', 'বার্তা প্রিভিউ', 'অবস্থা', 'অ্যাকশন']}>
              {templates?.map((t) => (
                <tr key={t.id}>
                  <Td className="font-semibold text-[var(--ink-primary)]">{t.name}</Td>
                  <Td className="font-mono text-xs text-[#0E3B2E] dark:text-[#E8A317] font-semibold">{t.code}</Td>
                  <Td>{getCategoryBadge(t.category)}</Td>
                  <Td className="text-xs text-[var(--ink-secondary)] max-w-xs truncate">{t.templateBody}</Td>
                  <Td>
                    <Badge tone={t.active ? 'green' : 'gray'}>
                      {t.active ? 'সক্রিয়' : 'নিষ্ক্রিয়'}
                    </Badge>
                  </Td>
                  <Td>
                    {isAdmin && (
                      <div className="flex gap-2">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => setTempModal({ open: true, item: t })}
                        >
                          বদলান
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          className="text-[var(--semantic-danger)] hover:underline"
                          onClick={() => deleteTemplate(t.id)}
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
        </Card>
      )}

      {/* TAB 3: Delivery Logs */}
      {activeTab === 'logs' && (
        <div className="space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="w-48">
              <SelectField label="অবস্থা" value={logStatus} onChange={(e) => setLogStatus(e.target.value)}>
                <option value="">সব</option>
                <option value="PENDING">অপেক্ষমাণ (Pending)</option>
                <option value="SENT">পাঠানো হয়েছে (Sent)</option>
                <option value="FAILED">ব্যর্থ (Failed)</option>
              </SelectField>
            </div>
            {isAdmin && (
              <Button onClick={dispatchQueue} disabled={logBusy}>
                {logBusy ? 'পাঠানো হচ্ছে…' : 'অপেক্ষমাণ বার্তা পাঠান'}
              </Button>
            )}
          </div>

          {logNotice && <Alert type={logNotice.type}>{logNotice.text}</Alert>}

          <Card>
            <LoadState loading={logLoading} error={logError}>
              {logData?.items?.length === 0 ? (
                <EmptyState>কোনো বার্তা নেই।</EmptyState>
              ) : (
                <Table headers={['শিক্ষার্থী', 'অভিভাবক', 'ফোন', 'ধরন', 'তারিখ', 'বার্তা', 'অবস্থা', 'অ্যাকশন']}>
                  {logData?.items?.map((n) => (
                    <tr key={n.id}>
                      <Td className="font-medium text-[var(--ink-primary)]">{n.studentName}</Td>
                      <Td className="text-[var(--ink-secondary)]">{n.guardianName || '—'}</Td>
                      <Td className="font-mono text-xs text-[var(--ink-primary)]">{n.phone}</Td>
                      <Td>{getCategoryBadge(n.type || n.notificationType)}</Td>
                      <Td className="text-xs text-[var(--ink-secondary)]">{formatDate(n.eventDate)}</Td>
                      <Td className="text-xs text-[var(--ink-secondary)] max-w-xs truncate" title={n.message}>
                        {n.message}
                      </Td>
                      <Td>
                        <Badge tone={n.status === 'SENT' ? 'green' : n.status === 'FAILED' ? 'red' : 'amber'}>
                          {n.status === 'SENT' ? 'পাঠানো হয়েছে' : n.status === 'FAILED' ? 'ব্যর্থ' : 'অপেক্ষমাণ'}
                        </Badge>
                      </Td>
                      <Td>
                        {n.status === 'FAILED' && isAdmin && (
                          <Button size="sm" variant="secondary" onClick={() => retryLog(n.id)}>
                            পুনরায়
                          </Button>
                        )}
                      </Td>
                    </tr>
                  ))}
                </Table>
              )}
            </LoadState>
            {logData && <Pagination page={logPage} totalPages={logData.totalPages} onPageChange={setLogPage} />}
          </Card>
        </div>
      )}

      {/* Template Modal */}
      {tempModal.open && (
        <TemplateModal
          initial={tempModal.item}
          onDone={() => {
            setTempModal({ open: false, item: null })
            reloadTemplates()
          }}
          onClose={() => setTempModal({ open: false, item: null })}
        />
      )}

      {/* Send Campaign SMS Modal */}
      {sendSmsModal && (
        <SendCampaignModal
          classes={classes}
          templates={templates}
          onDone={(res) => {
            setSendSmsModal(false)
            setCampNotice({
              type: res.failedCount > 0 ? 'error' : 'success',
              text: `ক্যাম্পেইন সম্পন্ন! মোট প্রাপক: ${res.totalRecipients} জন, সফল: ${res.sentCount} টি, ব্যর্থ: ${res.failedCount} টি।`,
            })
            reloadCampaigns()
            reloadLogs()
          }}
          onClose={() => setSendSmsModal(false)}
        />
      )}
    </>
  )
}
