import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import ClassSectionFields from '../components/ClassSectionFields.jsx'
import StudentBulkImportModal from '../components/StudentBulkImportModal.jsx'
import { Pagination } from '../components/extra.jsx'
import { Badge, Button, Card, EmptyState, LoadState, PageHeader, SelectField, Table, TextField, Td } from '../components/ui.jsx'
import { useDebounce } from '../hooks/useDebounce.js'
import { useQuery } from '../hooks/useQuery.js'
import { genderLabel, STUDENT_STATUS_OPTIONS, studentStatusInfo } from '../utils/format.js'

const STATUS_FILTER = [{ value: '', label: 'সব' }, ...STUDENT_STATUS_OPTIONS]
const EMPTY_FILTERS = { q: '', status: '', classId: '', sectionId: '' }

export default function StudentsPage() {
  const navigate = useNavigate()
  const [filters, setFilters] = useState(EMPTY_FILTERS)
  const [page, setPage] = useState(0)
  const [showImportModal, setShowImportModal] = useState(false)
  const [exporting, setExporting] = useState(false)
  const q = useDebounce(filters.q.trim())

  const sessions = useQuery('/api/sessions')
  const activeSession = (sessions.data ?? []).find((s) => s.active)

  // ছাঁকনি বদলালে সবসময় প্রথম পাতায় ফিরি
  function update(patch) {
    setFilters((current) => ({ ...current, ...patch }))
    setPage(0)
  }

  const params = new URLSearchParams({ page: String(page), size: '20' })
  if (q) params.set('q', q)
  if (filters.status) params.set('status', filters.status)
  if (filters.classId) params.set('classId', filters.classId)
  if (filters.sectionId) params.set('sectionId', filters.sectionId)

  const { data, error, loading, refetch } = useQuery(`/api/students?${params}`)

  async function handleExport() {
    if (!filters.classId) {
      alert('এক্সেল ফাইল এক্সপোর্ট করতে অনুগ্রহ করে নিচের ফিল্টার থেকে একটি ক্লাস নির্বাচন করুন।')
      return
    }
    if (!activeSession) {
      alert('কোনো সক্রিয় শিক্ষাবর্ষ/সেশন পাওয়া যায়নি।')
      return
    }

    try {
      setExporting(true)
      const exportParams = new URLSearchParams({
        sessionId: String(activeSession.id),
        classId: filters.classId,
      })
      if (filters.sectionId) {
        exportParams.set('sectionId', filters.sectionId)
      }

      const token = window.__accessToken || localStorage.getItem('token')
      const res = await fetch(`/api/students/export?${exportParams}`, {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      })
      if (!res.ok) throw new Error('এক্সেল ফাইল তৈরিতে ব্যর্থ হয়েছে')

      const blob = await res.blob()
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `students_class_${filters.classId}.xlsx`
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

  return (
    <>
      <PageHeader
        title="শিক্ষার্থী"
        subtitle="নাম বা আইডি দিয়ে খুঁজুন, ক্লাস ও অবস্থা দিয়ে ছেঁকে নিন"
        action={
          <div className="flex flex-wrap gap-2">
            <Button
              variant="secondary"
              onClick={handleExport}
              disabled={exporting}
              title="নির্বাচিত ক্লাসের শিক্ষার্থীদের এক্সেল ফাইলে সেভ করুন"
            >
              {exporting ? 'প্রস্তুত হচ্ছে...' : '📊 এক্সেল এক্সপোর্ট'}
            </Button>
            <Button
              variant="secondary"
              onClick={() => setShowImportModal(true)}
              title="এক্সেল ফাইল আপলোড করে একসঙ্গে অনেক শিক্ষার্থী ভর্তি করুন"
            >
              📥 বাল্ক ইমপোর্ট
            </Button>
            <Button variant="secondary" onClick={() => navigate('/students/deleted')}>
              মোছা শিক্ষার্থী
            </Button>
            <Button onClick={() => navigate('/students/new')}>নতুন শিক্ষার্থী</Button>
          </div>
        }
      />

      <Card className="mb-4 p-4">
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <TextField
            label="খুঁজুন"
            hint="নাম বা আইডি"
            value={filters.q}
            onChange={(event) => update({ q: event.target.value })}
          />
          <SelectField
            label="অবস্থা"
            options={STATUS_FILTER}
            value={filters.status}
            onChange={(event) => update({ status: event.target.value })}
          />
          <ClassSectionFields
            allowAll
            classId={filters.classId}
            sectionId={filters.sectionId}
            onChange={(patch) => update(patch)}
          />
        </div>
      </Card>

      <Card>
        <LoadState loading={loading} error={error}>
          {data?.items.length === 0 ? (
            <EmptyState>কোনো শিক্ষার্থী পাওয়া যায়নি।</EmptyState>
          ) : (
            <>
              <Table headers={['আইডি', 'নাম', 'লিঙ্গ', 'ক্লাস / সেকশন', 'রোল', 'অবস্থা']}>
                {data?.items.map((s) => {
                  const info = studentStatusInfo(s.status)
                  return (
                    <tr key={s.id}>
                      <Td className="font-mono text-xs">{s.studentCode}</Td>
                      <Td className="font-medium text-gray-900">
                        <Link to={`/students/${s.id}`} className="hover:text-indigo-700">
                          {s.fullName}
                        </Link>
                      </Td>
                      <Td>{genderLabel(s.gender)}</Td>
                      <Td>{s.className ? `${s.className} / ${s.sectionName}` : '—'}</Td>
                      <Td>{s.rollNumber ?? '—'}</Td>
                      <Td>
                        <Badge tone={info.tone}>{info.label}</Badge>
                      </Td>
                    </tr>
                  )
                })}
              </Table>
              {data && (
                <Pagination
                  page={data.page}
                  totalPages={data.totalPages}
                  totalElements={data.totalElements}
                  onChange={setPage}
                />
              )}
            </>
          )}
        </LoadState>
      </Card>

      {showImportModal && (
        <StudentBulkImportModal
          onClose={() => setShowImportModal(false)}
          onSuccess={() => {
            if (refetch) refetch()
          }}
        />
      )}
    </>
  )
}