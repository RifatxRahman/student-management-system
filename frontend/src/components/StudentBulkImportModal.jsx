import { useState } from 'react'
import { Alert, Badge, Button, Modal, SelectField, Spinner, Table, Td } from './ui.jsx'
import { useQuery } from '../hooks/useQuery.js'

export default function StudentBulkImportModal({ onClose, onSuccess }) {
  const sessions = useQuery('/api/sessions')
  const classes = useQuery('/api/classes')
  
  const sessionList = sessions.data ?? []
  const activeSession = sessionList.find((s) => s.active)

  const [sessionId, setSessionId] = useState('')
  const [classId, setClassId] = useState('')
  const [defaultSectionId, setDefaultSectionId] = useState('')
  const [file, setFile] = useState(null)
  const [loading, setLoading] = useState(false)
  const [downloadingTemplate, setDownloadingTemplate] = useState(false)
  const [result, setResult] = useState(null)
  const [generalError, setGeneralError] = useState('')

  const sections = useQuery(classId ? `/api/classes/${classId}/sections` : null)
  const sectionList = (sections.data ?? []).filter((s) => s.active)

  const effectiveSessionId = sessionId || (activeSession ? String(activeSession.id) : '')

  const sessionOptions = [
    { value: '', label: activeSession ? `সক্রিয় সেশন (${activeSession.name})` : 'সেশন বাছুন' },
    ...sessionList.map((s) => ({ value: String(s.id), label: s.name })),
  ]

  const classOptions = [
    { value: '', label: 'শ্রেণি নির্বাচন করুন' },
    ...(classes.data ?? []).filter((c) => c.active).map((c) => ({ value: String(c.id), label: c.name })),
  ]

  const sectionOptions = [
    { value: '', label: 'ডিফল্ট শাখা (ঐচ্ছিক)' },
    ...sectionList.map((s) => ({ value: String(s.id), label: s.name })),
  ]

  // ১. এক্সেল টেমপ্লেট ডাউনলোড
  async function handleDownloadTemplate() {
    try {
      setDownloadingTemplate(true)
      const token = window.__accessToken || localStorage.getItem('token')
      const res = await fetch('/api/students/import-template', {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      })
      if (!res.ok) throw new Error('টেমপ্লেট ডাউনলোড করতে ব্যর্থ হয়েছে')
      
      const blob = await res.blob()
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = 'student_admission_template.xlsx'
      document.body.appendChild(a)
      a.click()
      a.remove()
      window.URL.revokeObjectURL(url)
    } catch (err) {
      setGeneralError(err.message || 'টেমপ্লেট ডাউনলোডে সমস্যা হয়েছে')
    } finally {
      setDownloadingTemplate(false)
    }
  }

  // ২. বাল্ক ইমপোর্ট সাবমিট
  async function handleSubmit(e) {
    e.preventDefault()
    if (!effectiveSessionId) {
      setGeneralError('অনুগ্রহ করে একটি শিক্ষাবর্ষ/সেশন নির্বাচন করুন')
      return
    }
    if (!classId) {
      setGeneralError('অনুগ্রহ করে একটি শ্রেণি নির্বাচন করুন')
      return
    }
    if (!file) {
      setGeneralError('অনুগ্রহ করে পূরণকৃত এক্সেল (.xlsx) ফাইলটি নির্বাচন করুন')
      return
    }

    setGeneralError('')
    setLoading(true)
    setResult(null)

    try {
      const formData = new FormData()
      formData.append('file', file)
      formData.append('sessionId', effectiveSessionId)
      formData.append('classId', classId)
      if (defaultSectionId) {
        formData.append('defaultSectionId', defaultSectionId)
      }

      const token = window.__accessToken || localStorage.getItem('token')
      const res = await fetch('/api/students/bulk-import', {
        method: 'POST',
        headers: token ? { Authorization: `Bearer ${token}` } : {},
        body: formData,
      })

      const data = await res.json()
      if (!res.ok) {
        throw new Error(data.message || 'এক্সেল ইমপোর্ট ব্যর্থ হয়েছে')
      }

      setResult(data)
      if (data.successCount > 0 && onSuccess) {
        onSuccess()
      }
    } catch (err) {
      setGeneralError(err.message || 'সার্ভারে ফাইল প্রসেস করতে ব্যর্থ হয়েছে')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Modal title="এক্সেল শিট থেকে শিক্ষার্থী বাল্ক ভর্তি" onClose={onClose}>
      <div className="max-h-[85vh] overflow-y-auto p-1">
        {/* ধাপ ১: টেমপ্লেট নির্দেশনা ও ডাউনলোড */}
        <div className="mb-4 rounded-[14px] bg-[var(--surface-subtle)] p-3.5 border border-[var(--border-hairline)]">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div>
              <h4 className="text-sm font-semibold text-[var(--ink-primary)]">ধাপ ১: এক্সেল টেমপ্লেট ডাউনলোড করুন</h4>
              <p className="mt-0.5 text-xs text-[var(--ink-secondary)]">
                নির্দিষ্ট কলাম ফরম্যাটে শিক্ষার্থীদের তথ্য পূরণ করে আপলোড করুন।
              </p>
            </div>
            <Button
              variant="secondary"
              size="sm"
              onClick={handleDownloadTemplate}
              disabled={downloadingTemplate}
            >
              {downloadingTemplate ? <Spinner size="sm" /> : '📥 টেমপ্লেট (.xlsx) ডাউনলোড'}
            </Button>
          </div>
        </div>

        {generalError && (
          <div className="mb-4">
            <Alert type="error">{generalError}</Alert>
          </div>
        )}

        {/* ধাপ ২: ফাইল আপলোড ফর্ম */}
        {!result && (
          <form onSubmit={handleSubmit} className="space-y-4">
            <h4 className="text-sm font-semibold text-[var(--ink-primary)]">ধাপ ২: শ্রেণি নির্বাচন ও ফাইল আপলোড</h4>
            
            <div className="grid gap-3 sm:grid-cols-2">
              <SelectField
                label="শিক্ষাবর্ষ / সেশন *"
                options={sessionOptions}
                value={sessionId}
                onChange={(e) => setSessionId(e.target.value)}
                required
              />
              <SelectField
                label="শ্রেণি *"
                options={classOptions}
                value={classId}
                onChange={(e) => {
                  setClassId(e.target.value)
                  setDefaultSectionId('')
                }}
                required
              />
            </div>

            <SelectField
              label="ডিফল্ট শাখা (সেকশন) — এক্সেলে শাখা উল্লেখ না থাকলে এটি প্রযোজ্য হবে"
              options={sectionOptions}
              value={defaultSectionId}
              disabled={!classId}
              onChange={(e) => setDefaultSectionId(e.target.value)}
            />

            <div>
              <label className="block text-sm font-semibold text-[var(--ink-primary)] mb-1">
                এক্সেল ফাইল নির্বাচন করুন (.xlsx বা .xls) *
              </label>
              <input
                type="file"
                accept=".xlsx, .xls"
                onChange={(e) => setFile(e.target.files[0] || null)}
                className="mt-1 block w-full rounded-[10px] border border-[var(--border-hairline)] bg-[var(--surface-subtle)] px-3 py-2 text-sm text-[var(--ink-secondary)] file:mr-3 file:rounded-md file:border-0 file:bg-[#E8A317]/20 file:px-3 file:py-1 file:text-xs file:font-semibold file:text-[#E8A317] dark:file:text-[#F0B232] hover:file:bg-[#E8A317]/30"
                required
              />
            </div>

            <div className="mt-6 flex justify-end gap-2 border-t border-[var(--border-hairline)] pt-4">
              <Button variant="secondary" onClick={onClose} disabled={loading}>
                বাতিল
              </Button>
              <Button type="submit" disabled={loading || !file}>
                {loading ? (
                  <span className="flex items-center gap-2">
                    <Spinner size="sm" /> প্রসেস হচ্ছে...
                  </span>
                ) : (
                  '🚀 শিক্ষার্থী ডাটা ইমপোর্ট করুন'
                )}
              </Button>
            </div>
          </form>
        )}

        {/* ধাপ ৩: ফলাফল ও ভ্যালিডেশন রিপোর্ট */}
        {result && (
          <div className="space-y-4">
            <Alert type={result.failureCount === 0 ? 'success' : result.successCount > 0 ? 'info' : 'error'}>
              {result.message}
            </Alert>

            <div className="grid grid-cols-3 gap-2 text-center">
              <div className="rounded-lg bg-[var(--surface-subtle)] p-3 border border-[var(--border-hairline)]">
                <span className="text-xs text-[var(--ink-secondary)]">মোট রো</span>
                <p className="text-lg font-bold text-[var(--ink-primary)] font-['Anek_Bangla',sans-serif] tabular-nums">{result.totalRows}</p>
              </div>
              <div className="rounded-lg bg-[var(--semantic-success-bg)] p-3 border border-[var(--semantic-success)]/30">
                <span className="text-xs text-[var(--semantic-success)]">সফল ভর্তি</span>
                <p className="text-lg font-bold text-[var(--semantic-success)] font-['Anek_Bangla',sans-serif] tabular-nums">{result.successCount}</p>
              </div>
              <div className="rounded-lg bg-[var(--semantic-danger-bg)] p-3 border border-[var(--semantic-danger)]/30">
                <span className="text-xs text-[var(--semantic-danger)]">ব্যর্থ রো</span>
                <p className="text-lg font-bold text-[var(--semantic-danger)] font-['Anek_Bangla',sans-serif] tabular-nums">{result.failureCount}</p>
              </div>
            </div>

            {/* কোনো এরর থাকলে তালিকা */}
            {result.errors?.length > 0 && (
              <div>
                <h5 className="mb-2 text-xs font-semibold text-[var(--semantic-danger)]">
                  ⚠️ যে যে রো-তে সমস্যা পাওয়া গেছে ({result.errors.length} টি):
                </h5>
                <div className="max-h-48 overflow-y-auto rounded-lg border border-[var(--semantic-danger)]/30 bg-[var(--semantic-danger-bg)] p-2">
                  <Table headers={['রো', 'শিক্ষার্থীর নাম', 'ত্রুটির কারণ']}>
                    {result.errors.map((err, idx) => (
                      <tr key={idx} className="text-xs">
                        <Td className="font-mono font-bold text-[var(--semantic-danger)]">{err.rowNumber}</Td>
                        <Td className="font-medium text-[var(--ink-primary)]">{err.studentName}</Td>
                        <Td className="text-[var(--semantic-danger)]">{err.reason}</Td>
                      </tr>
                    ))}
                  </Table>
                </div>
              </div>
            )}

            {/* সফলভাবে যুক্ত শিক্ষার্থীদের প্রিভিউ */}
            {result.importedStudents?.length > 0 && (
              <div>
                <h5 className="mb-2 text-xs font-semibold text-[var(--semantic-success)]">
                  ✔ সফলভাবে ভর্তি হওয়া শিক্ষার্থী তালিকা ({result.importedStudents.length} জন):
                </h5>
                <div className="max-h-48 overflow-y-auto rounded-lg border border-[var(--border-hairline)] p-2">
                  <Table headers={['আইডি', 'নাম', 'শাখা', 'বরাদ্দকৃত রোল']}>
                    {result.importedStudents.slice(0, 50).map((st, idx) => (
                      <tr key={idx} className="text-xs">
                        <Td className="font-mono text-[var(--ink-secondary)] font-medium">{st.studentCode}</Td>
                        <Td className="font-medium text-[var(--ink-primary)]">{st.fullName}</Td>
                        <Td>{st.sectionName}</Td>
                        <Td className="font-bold text-[var(--ink-primary)]">{st.rollNumber}</Td>
                      </tr>
                    ))}
                  </Table>
                  {result.importedStudents.length > 50 && (
                    <p className="p-2 text-center text-xs text-[var(--ink-secondary)]">
                      ... আরও {result.importedStudents.length - 50} জন শিক্ষার্থী সফলভাবে যুক্ত হয়েছে।
                    </p>
                  )}
                </div>
              </div>
            )}

            <div className="mt-4 flex justify-end gap-2 border-t border-[var(--border-hairline)] pt-3">
              <Button
                variant="secondary"
                onClick={() => {
                  setResult(null)
                  setFile(null)
                }}
              >
                আরেকটি ফাইল দিন
              </Button>
              <Button onClick={onClose}>সম্পন্ন ও বন্ধ করুন</Button>
            </div>
          </div>
        )}
      </div>
    </Modal>
  )
}
