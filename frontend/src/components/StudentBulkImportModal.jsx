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
        <div className="mb-4 rounded-lg bg-indigo-50/70 p-3.5 ring-1 ring-indigo-100">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div>
              <h4 className="text-sm font-semibold text-indigo-950">ধাপ ১: এক্সেল টেমপ্লেট ডাউনলোড করুন</h4>
              <p className="mt-0.5 text-xs text-indigo-700">
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
            <h4 className="text-sm font-semibold text-gray-900">ধাপ ২: শ্রেণি নির্বাচন ও ফাইল আপলোড</h4>
            
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
              <label className="block text-sm font-medium text-gray-700">
                এক্সেল ফাইল নির্বাচন করুন (.xlsx বা .xls) *
              </label>
              <input
                type="file"
                accept=".xlsx, .xls"
                onChange={(e) => setFile(e.target.files[0] || null)}
                className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm text-gray-600 file:mr-3 file:rounded-md file:border-0 file:bg-indigo-50 file:px-3 file:py-1 file:text-xs file:font-semibold file:text-indigo-700 hover:file:bg-indigo-100"
                required
              />
            </div>

            <div className="mt-6 flex justify-end gap-2 border-t pt-4">
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
              <div className="rounded-lg bg-gray-50 p-3 ring-1 ring-gray-200">
                <span className="text-xs text-gray-500">মোট রো</span>
                <p className="text-lg font-bold text-gray-800">{result.totalRows}</p>
              </div>
              <div className="rounded-lg bg-green-50 p-3 ring-1 ring-green-200">
                <span className="text-xs text-green-700">সফল ভর্তি</span>
                <p className="text-lg font-bold text-green-700">{result.successCount}</p>
              </div>
              <div className="rounded-lg bg-red-50 p-3 ring-1 ring-red-200">
                <span className="text-xs text-red-700">ব্যর্থ রো</span>
                <p className="text-lg font-bold text-red-700">{result.failureCount}</p>
              </div>
            </div>

            {/* কোনো এরর থাকলে তালিকা */}
            {result.errors?.length > 0 && (
              <div>
                <h5 className="mb-2 text-xs font-semibold text-red-700">
                  ⚠️ যে যে রো-তে সমস্যা পাওয়া গেছে ({result.errors.length} টি):
                </h5>
                <div className="max-h-48 overflow-y-auto rounded-lg border border-red-200 bg-red-50/40 p-2">
                  <Table headers={['রো', 'শিক্ষার্থীর নাম', 'ত্রুটির কারণ']}>
                    {result.errors.map((err, idx) => (
                      <tr key={idx} className="text-xs">
                        <Td className="font-mono font-bold text-red-600">{err.rowNumber}</Td>
                        <Td className="font-medium text-gray-900">{err.studentName}</Td>
                        <Td className="text-red-700">{err.reason}</Td>
                      </tr>
                    ))}
                  </Table>
                </div>
              </div>
            )}

            {/* সফলভাবে যুক্ত শিক্ষার্থীদের প্রিভিউ */}
            {result.importedStudents?.length > 0 && (
              <div>
                <h5 className="mb-2 text-xs font-semibold text-green-700">
                  ✔ সফলভাবে ভর্তি হওয়া শিক্ষার্থী তালিকা ({result.importedStudents.length} জন):
                </h5>
                <div className="max-h-48 overflow-y-auto rounded-lg border border-gray-200 p-2">
                  <Table headers={['আইডি', 'নাম', 'শাখা', 'বরাদ্দকৃত রোল']}>
                    {result.importedStudents.slice(0, 50).map((st, idx) => (
                      <tr key={idx} className="text-xs">
                        <Td className="font-mono text-indigo-700">{st.studentCode}</Td>
                        <Td className="font-medium text-gray-900">{st.fullName}</Td>
                        <Td>{st.sectionName}</Td>
                        <Td className="font-bold text-gray-900">{st.rollNumber}</Td>
                      </tr>
                    ))}
                  </Table>
                  {result.importedStudents.length > 50 && (
                    <p className="p-2 text-center text-xs text-gray-500">
                      ... আরও {result.importedStudents.length - 50} জন শিক্ষার্থী সফলভাবে যুক্ত হয়েছে।
                    </p>
                  )}
                </div>
              </div>
            )}

            <div className="mt-4 flex justify-end gap-2 border-t pt-3">
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
