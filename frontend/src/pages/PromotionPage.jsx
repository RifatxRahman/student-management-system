import { useState } from 'react'
import { api } from '../api/client.js'
import { Alert, Badge, Button, Card, EmptyState, PageHeader, SelectField, Spinner, Table, Td } from '../components/ui.jsx'
import { useQuery } from '../hooks/useQuery.js'

const ACTION_OPTIONS = [
  { value: 'PROMOTE', label: 'উত্তীর্ণ (Promote)' },
  { value: 'RETAIN', label: 'একই শ্রেণিতে বহাল (Retain)' },
  { value: 'GRADUATE', label: 'গ্র্যাজুয়েট (Graduate)' },
  { value: 'SKIP', label: 'বাদ রাখুন (Skip)' },
]

export default function PromotionPage() {
  const sessions = useQuery('/api/sessions')
  const classes = useQuery('/api/classes')

  const sessionList = sessions.data ?? []
  const classList = (classes.data ?? []).filter((c) => c.active)

  // ফিল্টার স্টেট
  const [sourceSessionId, setSourceSessionId] = useState('')
  const [sourceClassId, setSourceClassId] = useState('')
  const [sourceSectionId, setSourceSectionId] = useState('')
  const [finalExamId, setFinalExamId] = useState('')
  const [targetSessionId, setTargetSessionId] = useState('')
  const [targetClassId, setTargetClassId] = useState('')
  const [meritRoll, setMeritRoll] = useState(true)

  // প্রিভিউ ও এডিট স্টেট
  const [previewData, setPreviewData] = useState(null)
  const [candidateRows, setCandidateRows] = useState([])
  const [loadingPreview, setLoadingPreview] = useState(false)
  const [executing, setExecuting] = useState(false)
  const [errorMsg, setErrorMsg] = useState('')
  const [successMsg, setSuccessMsg] = useState('')

  // সোর্স ও টার্গেট সেকশন
  const sourceSections = useQuery(sourceClassId ? `/api/classes/${sourceClassId}/sections` : null)
  const effectiveTargetClassId = targetClassId || previewData?.targetClassId
  const targetSections = useQuery(effectiveTargetClassId ? `/api/classes/${effectiveTargetClassId}/sections` : null)
  
  // সোর্স ক্লাসের পরীক্ষাসমূহ
  const examsQuery = sourceSessionId && sourceClassId
    ? `/api/exams?sessionId=${sourceSessionId}&classId=${sourceClassId}`
    : null
  const exams = useQuery(examsQuery)

  const sessionOptions = [
    { value: '', label: 'সেশন নির্বাচন করুন' },
    ...sessionList.map((s) => ({ value: String(s.id), label: `${s.name} ${s.active ? '(সক্রিয়)' : ''}` })),
  ]

  const classOptions = [
    { value: '', label: 'শ্রেণি নির্বাচন করুন' },
    ...classList.map((c) => ({ value: String(c.id), label: c.name })),
  ]

  const sourceSectionOptions = [
    { value: '', label: 'সব শাখা (সকল সেকশন)' },
    ...(sourceSections.data ?? []).filter((s) => s.active).map((s) => ({ value: String(s.id), label: s.name })),
  ]

  const targetSectionList = (targetSections.data ?? []).filter((s) => s.active)

  const examOptions = [
    { value: '', label: 'পরীক্ষার রেজাল্ট ছাড়াই সরাসরি প্রমোশন' },
    ...(exams.data ?? []).map((e) => ({ value: String(e.id), label: `${e.name} (${e.status === 'PUBLISHED' ? 'ফল প্রকাশিত' : 'খসড়া'})` })),
  ]

  // ১. প্রিভিউ লোড
  async function handleGeneratePreview(e) {
    e.preventDefault()
    if (!sourceSessionId || !sourceClassId || !targetSessionId) {
      setErrorMsg('অনুগ্রহ করে পূর্ববর্তী সেশন, শ্রেণি এবং পরবর্তী সেশন নির্বাচন করুন।')
      return
    }

    setErrorMsg('')
    setSuccessMsg('')
    setLoadingPreview(true)
    setPreviewData(null)

    try {
      const params = new URLSearchParams({
        sourceSessionId,
        sourceClassId,
        targetSessionId,
        meritRoll: String(meritRoll),
      })
      if (sourceSectionId) params.set('sourceSectionId', sourceSectionId)
      if (targetClassId) params.set('targetClassId', targetClassId)
      if (finalExamId) params.set('finalExamId', finalExamId)

      const token = window.__accessToken || localStorage.getItem('token')
      const res = await fetch(`/api/promotion/preview?${params}`, {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      })
      const data = await res.json()
      if (!res.ok) throw new Error(data.message || 'প্রিভিউ তৈরিতে সমস্যা হয়েছে')

      setPreviewData(data)
      // লোকাল এডিটেবল ক্যান্ডিডেট রো তৈরি
      setCandidateRows(data.items.map((itm) => ({
        ...itm,
        action: itm.suggestedAction,
        targetClassId: itm.suggestedTargetClassId || (targetClassId ? Number(targetClassId) : null),
        targetSectionId: itm.suggestedTargetSectionId || (targetSectionList[0]?.id ?? null),
        newRollNumber: itm.suggestedRollNumber,
      })))
    } catch (err) {
      setErrorMsg(err.message || 'প্রমোশন প্রিভিউ তৈরি করতে ব্যর্থ হয়েছে')
    } finally {
      setLoadingPreview(false)
    }
  }

  // রো এডিট আপডেট
  function updateRow(studentId, patch) {
    setCandidateRows((rows) =>
      rows.map((r) => (r.studentId === studentId ? { ...r, ...patch } : r))
    )
  }

  // বাল্ক অ্যাকশন পরিবর্তন (যেমন: সবাইকে প্রমোট করুন)
  function applyBulkAction(action) {
    setCandidateRows((rows) => rows.map((r) => ({ ...r, action })))
  }

  // ২. প্রমোশন চূড়ান্ত কার্যকরকরণ
  async function handleExecutePromotion() {
    if (!candidateRows.length) return
    const activeCount = candidateRows.filter((r) => r.action !== 'SKIP').length
    if (activeCount === 0) {
      alert('কমপক্ষে একজন শিক্ষার্থীর জন্য অ্যাকশন (উত্তীর্ণ বা রিটেইন) সিলেক্ট করুন।')
      return
    }

    if (!confirm(`আপনি কি নিশ্চিত যে ${activeCount} জন শিক্ষার্থীর জন্য নতুন সেশনে প্রমোশন ও রোল বণ্টন কার্যকর করতে চান?`)) {
      return
    }

    setExecuting(true)
    setErrorMsg('')
    setSuccessMsg('')

    try {
      const payload = {
        sourceSessionId: Number(sourceSessionId),
        targetSessionId: Number(targetSessionId),
        students: candidateRows.map((r) => ({
          studentId: r.studentId,
          action: r.action,
          targetClassId: r.targetClassId,
          targetSectionId: r.targetSectionId,
          newRollNumber: Number(r.newRollNumber) || null,
        })),
      }

      const res = await api('/api/promotion/execute', {
        method: 'POST',
        body: payload,
      })

      setSuccessMsg(res.message || 'প্রমোশন সফলভাবে সম্পন্ন হয়েছে!')
      // সফল হলে প্রিভিউ রিফ্রেশ
      setPreviewData(null)
      setCandidateRows([])
    } catch (err) {
      setErrorMsg(err.message || 'প্রমোশন কার্যকর করতে ব্যর্থ হয়েছে')
    } finally {
      setExecuting(false)
    }
  }

  return (
    <>
      <PageHeader
        title="সেশন প্রমোশন ও রোল-ওভার"
        subtitle="শিক্ষাবর্ষ শেষে পরীক্ষার ফলাফল অনুযায়ী পরবর্তী ক্লাসে উত্তীর্ণ ও মেধা অনুযায়ী নতুন রোল বণ্টন"
      />

      {errorMsg && (
        <div className="mb-4">
          <Alert type="error">{errorMsg}</Alert>
        </div>
      )}

      {successMsg && (
        <div className="mb-4">
          <Alert type="success">{successMsg}</Alert>
        </div>
      )}

      {/* কনফিগারেশন ও ফিল্টার ফর্ম */}
      <Card className="mb-6 p-5">
        <form onSubmit={handleGeneratePreview} className="space-y-4">
          <h3 className="text-base font-semibold text-gray-900 border-b pb-2">
            ১. সেশন ও শ্রেণি নির্বাচন
          </h3>

          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <SelectField
              label="পূর্ববর্তী শিক্ষাবর্ষ (সোর্স সেশন) *"
              options={sessionOptions}
              value={sourceSessionId}
              onChange={(e) => setSourceSessionId(e.target.value)}
              required
            />
            <SelectField
              label="পূর্ববর্তী শ্রেণি (সোর্স ক্লাস) *"
              options={classOptions}
              value={sourceClassId}
              onChange={(e) => {
                setSourceClassId(e.target.value)
                setSourceSectionId('')
                setFinalExamId('')
              }}
              required
            />
            <SelectField
              label="শাখা (সেকশন) — ঐচ্ছিক"
              options={sourceSectionOptions}
              value={sourceSectionId}
              disabled={!sourceClassId}
              onChange={(e) => setSourceSectionId(e.target.value)}
            />
          </div>

          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <SelectField
              label="পরবর্তী শিক্ষাবর্ষ (টার্গেট সেশন) *"
              options={sessionOptions}
              value={targetSessionId}
              onChange={(e) => setTargetSessionId(e.target.value)}
              required
            />
            <SelectField
              label="পরবর্তী শ্রেণি (টার্গেট ক্লাস) — স্বয়ংক্রিয়"
              options={[
                { value: '', label: 'স্বয়ংক্রিয় পরবর্তী শ্রেণি (Auto Next Class)' },
                ...classList.map((c) => ({ value: String(c.id), label: c.name })),
              ]}
              value={targetClassId}
              onChange={(e) => setTargetClassId(e.target.value)}
            />
            <SelectField
              label="বার্ষিক / চূড়ান্ত পরীক্ষা (ফলাফলের ভিত্তিতে প্রমোশন)"
              options={examOptions}
              value={finalExamId}
              disabled={!sourceClassId}
              onChange={(e) => setFinalExamId(e.target.value)}
            />
          </div>

          <div className="flex flex-wrap items-center justify-between gap-4 border-t pt-3">
            <label className="flex items-center gap-2 text-sm text-gray-700 cursor-pointer">
              <input
                type="checkbox"
                checked={meritRoll}
                onChange={(e) => setMeritRoll(e.target.checked)}
                className="h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
              />
              <span>ফাইনাল পরীক্ষার মেধা অবস্থান (GPA / প্রাপ্ত নম্বর) অনুযায়ী নতুন রোল বরাদ্দ করুন</span>
            </label>

            <Button type="submit" disabled={loadingPreview}>
              {loadingPreview ? (
                <span className="flex items-center gap-2">
                  <Spinner size="sm" /> প্রসেস হচ্ছে...
                </span>
              ) : (
                '🔍 প্রমোশন খসড়া তৈরি করুন'
              )}
            </Button>
          </div>
        </form>
      </Card>

      {/* প্রিভিউ ও ক্যান্ডিডেট তালিকা */}
      {previewData && (
        <Card className="p-5">
          <div className="mb-4 flex flex-wrap items-center justify-between gap-3 border-b pb-3">
            <div>
              <h3 className="text-lg font-bold text-gray-900">
                ২. প্রমোশন খসড়া ও শিক্ষার্থী তালিকা
              </h3>
              <p className="text-xs text-gray-500">
                {previewData.sourceSessionName} ({previewData.sourceClassName}) ➔ {previewData.targetSessionName} ({previewData.targetClassName})
              </p>
            </div>

            <div className="flex flex-wrap items-center gap-2">
              <span className="text-xs font-semibold text-gray-500">বাল্ক অ্যাকশন:</span>
              <Button size="sm" variant="secondary" onClick={() => applyBulkAction('PROMOTE')}>
                সবাইকে উত্তীর্ণ (Promote)
              </Button>
              <Button size="sm" variant="secondary" onClick={() => applyBulkAction('RETAIN')}>
                সবাইকে রিটেইন (Retain)
              </Button>
              <Button size="sm" variant="secondary" onClick={() => applyBulkAction('SKIP')}>
                সবাইকে বাদ (Skip)
              </Button>
            </div>
          </div>

          {/* পরিসংখ্যান সামারি */}
          <div className="mb-4 grid grid-cols-2 gap-3 sm:grid-cols-4 text-center">
            <div className="rounded-lg bg-gray-50 p-2.5 ring-1 ring-gray-200">
              <span className="text-xs text-gray-500">মোট শিক্ষার্থী</span>
              <p className="text-base font-bold text-gray-900">{previewData.totalStudents}</p>
            </div>
            <div className="rounded-lg bg-emerald-50 p-2.5 ring-1 ring-emerald-200">
              <span className="text-xs text-emerald-700">উত্তীর্ণ (পাস)</span>
              <p className="text-base font-bold text-emerald-700">{previewData.passedStudents}</p>
            </div>
            <div className="rounded-lg bg-rose-50 p-2.5 ring-1 ring-rose-200">
              <span className="text-xs text-rose-700">অকৃতকার্য (ফেল)</span>
              <p className="text-base font-bold text-rose-700">{previewData.failedStudents}</p>
            </div>
            <div className="rounded-lg bg-indigo-50 p-2.5 ring-1 ring-indigo-200">
              <span className="text-xs text-indigo-700">নির্বাচিত প্রমোশন</span>
              <p className="text-base font-bold text-indigo-700">
                {candidateRows.filter((r) => r.action === 'PROMOTE').length}
              </p>
            </div>
          </div>

          {candidateRows.length === 0 ? (
            <EmptyState>কোনো শিক্ষার্থী পাওয়া যায়নি।</EmptyState>
          ) : (
            <>
              <div className="overflow-x-auto">
                <Table headers={['আইডি', 'শিক্ষার্থীর নাম', 'বর্তমান রোল', 'ফলাফল ও জিপিএ', 'মেধা', 'প্রমোশন সিদ্ধান্ত', 'টার্গেট শাখা', 'নতুন রোল']}>
                  {candidateRows.map((row) => (
                    <tr key={row.studentId} className={row.action === 'SKIP' ? 'opacity-50' : ''}>
                      <Td className="font-mono text-xs">{row.studentCode}</Td>
                      <Td className="font-medium text-gray-900">{row.fullName}</Td>
                      <Td className="font-mono">{row.currentRollNumber} ({row.currentSectionName})</Td>
                      <Td>
                        {row.gpa != null ? (
                          <span className={`inline-flex items-center gap-1 font-bold ${row.passed ? 'text-emerald-700' : 'text-rose-700'}`}>
                            {row.gpa} ({row.grade})
                            <Badge tone={row.passed ? 'green' : 'red'}>{row.passed ? 'পাস' : 'ফেল'}</Badge>
                          </span>
                        ) : (
                          <span className="text-gray-400 text-xs">মূল্যায়ন ছাড়া</span>
                        )}
                      </Td>
                      <Td>
                        {row.meritPosition != null ? (
                          <span className="rounded-full bg-amber-100 px-2 py-0.5 text-xs font-bold text-amber-800">
                            #{row.meritPosition}
                          </span>
                        ) : (
                          '—'
                        )}
                      </Td>
                      <Td>
                        <select
                          value={row.action}
                          onChange={(e) => updateRow(row.studentId, { action: e.target.value })}
                          className="rounded-lg border border-gray-300 bg-white px-2.5 py-1 text-xs font-semibold shadow-sm focus:border-indigo-500 focus:outline-none"
                        >
                          {ACTION_OPTIONS.map((opt) => (
                            <option key={opt.value} value={opt.value}>
                              {opt.label}
                            </option>
                          ))}
                        </select>
                      </Td>
                      <Td>
                        {row.action === 'PROMOTE' || row.action === 'RETAIN' ? (
                          <select
                            value={row.targetSectionId || ''}
                            onChange={(e) => updateRow(row.studentId, { targetSectionId: Number(e.target.value) || null })}
                            className="rounded-lg border border-gray-300 bg-white px-2 py-1 text-xs shadow-sm focus:border-indigo-500 focus:outline-none"
                          >
                            {targetSectionList.map((sec) => (
                              <option key={sec.id} value={sec.id}>
                                {sec.name}
                              </option>
                            ))}
                          </select>
                        ) : (
                          '—'
                        )}
                      </Td>
                      <Td>
                        {row.action === 'PROMOTE' || row.action === 'RETAIN' ? (
                          <input
                            type="number"
                            min="1"
                            value={row.newRollNumber || ''}
                            onChange={(e) => updateRow(row.studentId, { newRollNumber: e.target.value ? Number(e.target.value) : '' })}
                            className="w-16 rounded-lg border border-gray-300 px-2 py-1 text-center font-bold text-indigo-700 shadow-sm focus:border-indigo-500 focus:outline-none"
                          />
                        ) : (
                          '—'
                        )}
                      </Td>
                    </tr>
                  ))}
                </Table>
              </div>

              {/* প্রমোশন কার্যকর করার বাটন */}
              <div className="mt-6 flex flex-wrap items-center justify-between gap-3 border-t pt-4">
                <span className="text-xs text-gray-500">
                  সতর্কতা: প্রমোশন কার্যকর করলে পূর্ববর্তী সেশনের স্থিতি 'সমাপ্ত' হবে এবং নতুন সেশনে ভর্তি সম্পন্ন হবে।
                </span>

                <Button
                  onClick={handleExecutePromotion}
                  disabled={executing || candidateRows.filter((r) => r.action !== 'SKIP').length === 0}
                  className="bg-emerald-600 hover:bg-emerald-700 text-white"
                >
                  {executing ? (
                    <span className="flex items-center gap-2">
                      <Spinner size="sm" /> প্রমোশন কার্যকর হচ্ছে...
                    </span>
                  ) : (
                    '🚀 প্রমোশন চূড়ান্ত কার্যকর করুন'
                  )}
                </Button>
              </div>
            </>
          )}
        </Card>
      )}
    </>
  )
}
