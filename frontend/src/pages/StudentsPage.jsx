import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import ClassSectionFields from '../components/ClassSectionFields.jsx'
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
  const q = useDebounce(filters.q.trim())

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

  const { data, error, loading } = useQuery(`/api/students?${params}`)

  return (
    <>
      <PageHeader
        title="শিক্ষার্থী"
        subtitle="নাম বা আইডি দিয়ে খুঁজুন, ক্লাস ও অবস্থা দিয়ে ছেঁকে নিন"
        action={
          <div className="flex gap-2">
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
    </>
  )
}