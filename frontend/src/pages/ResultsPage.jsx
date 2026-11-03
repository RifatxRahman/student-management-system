import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { useAuth } from '../auth/useAuth.js'
import { Alert, Badge, Card, EmptyState, LoadState, PageHeader, SelectField, Table, Td } from '../components/ui.jsx'
import { useQuery } from '../hooks/useQuery.js'
import { plain } from '../utils/format.js'

const LINK_BUTTON =
  'rounded-lg bg-white px-4 py-2 text-sm font-medium text-gray-700 ring-1 ring-gray-300 hover:bg-gray-50'

export default function ResultsPage() {
  const { examId } = useParams()
  const { user } = useAuth()
  const isAdmin = user.role === 'ADMIN'
  const exam = useQuery(`/api/exams/${examId}`)
  const [sectionChoice, setSectionChoice] = useState('')

  // Admin: ক্লাসের সব সেকশন। শিক্ষক: মার্কস এন্ট্রির তালিকা থেকে তাঁর সেকশনগুলো (একই সেকশন একবার)
  const adminSections = useQuery(isAdmin && exam.data ? `/api/classes/${exam.data.exam.classId}/sections` : null)
  const teacherTargets = useQuery(isAdmin ? null : `/api/marks/targets?examId=${examId}`)

  // eslint-disable-next-line no-useless-assignment
  let sectionOptions = []
  if (isAdmin) {
    sectionOptions = [
      { value: '', label: 'সব সেকশন (পুরো ক্লাস)' },
      ...(adminSections.data ?? []).filter((s) => s.active).map((s) => ({ value: String(s.id), label: s.name })),
    ]
  } else {
    const seen = new Set()
    sectionOptions = [{ value: '', label: 'বাছুন' }]
    for (const target of teacherTargets.data ?? []) {
      if (!seen.has(target.sectionId)) {
        seen.add(target.sectionId)
        sectionOptions.push({ value: String(target.sectionId), label: `${target.className} / ${target.sectionName}` })
      }
    }
  }

  // শিক্ষকের একটাই সেকশন থাকলে নিজে থেকে সেটাই বাছা হয়
  const onlyOne = !isAdmin && sectionOptions.length === 2 ? sectionOptions[1].value : ''
  const sectionId = sectionChoice || onlyOne
  const canLoad = isAdmin || Boolean(sectionId)

  const results = useQuery(canLoad ? `/api/exams/${examId}/results${sectionId ? `?sectionId=${sectionId}` : ''}` : null)

  const sectionQuery = sectionId ? `?sectionId=${sectionId}` : ''

  return (
    <>
      <div className="mb-3">
        <Link to={`/exams/${examId}`} className="text-sm text-gray-500 hover:text-gray-700">
          ← পরীক্ষায় ফিরে যান
        </Link>
      </div>

      <PageHeader
        title="ফলাফল"
        subtitle={exam.data ? `${exam.data.exam.name} · ${exam.data.exam.className} · ${exam.data.exam.sessionName}` : undefined}
        action={
          canLoad && (
            <div className="flex flex-wrap gap-2">
              <Link to={`/exams/${examId}/marksheets${sectionQuery}`} className={LINK_BUTTON}>
                মার্কশিট প্রিন্ট
              </Link>
              <Link to={`/exams/${examId}/result-sheet${sectionQuery}`} className={LINK_BUTTON}>
                রেজাল্ট শিট প্রিন্ট
              </Link>
            </div>
          )
        }
      />

      <Card className="mb-4 p-4">
        <div className="max-w-sm">
          <SelectField
            label="সেকশন"
            options={sectionOptions}
            value={sectionId}
            onChange={(event) => setSectionChoice(event.target.value)}
          />
        </div>
      </Card>

      {!canLoad && (
        <Card>
          <EmptyState>
            {teacherTargets.loading
              ? 'লোড হচ্ছে...'
              : sectionOptions.length <= 1
                ? 'এই পরীক্ষায় আপনার জন্য কোনো সেকশন নেই। Admin-কে বলুন আপনাকে বরাদ্দ দিতে।'
                : 'ফল দেখতে ওপর থেকে একটা সেকশন বাছুন।'}
          </EmptyState>
        </Card>
      )}

      {canLoad && (
        <Card>
          <LoadState loading={results.loading} error={results.error}>
            {results.data?.length === 0 ? (
              <EmptyState>কোনো ফল পাওয়া যায়নি।</EmptyState>
            ) : (
              <Table
                headers={['শাখা', 'রোল', 'নাম', 'আইডি', 'মোট', 'গড়', 'GPA', 'গ্রেড', 'ফল', 'শ্রেণিতে', 'শাখায়', '']}
              >
                {results.data?.map((row) => (
                  <tr key={row.enrollmentId}>
                    <Td>{row.sectionName}</Td>
                    <Td>{row.rollNumber}</Td>
                    <Td className="font-medium text-gray-900">{row.studentName}</Td>
                    <Td className="font-mono text-xs">{row.studentCode}</Td>
                    <Td>
                      {plain(row.totalMarks)} / {plain(row.totalFullMarks)}
                    </Td>
                    <Td>{plain(row.averageMarks)}</Td>
                    <Td>{Number(row.gpa).toFixed(2)}</Td>
                    <Td>{row.grade}</Td>
                    <Td>
                      <Badge tone={row.passed ? 'green' : 'red'}>{row.passed ? 'পাস' : 'ফেল'}</Badge>
                    </Td>
                    <Td>{row.passed ? row.positionInClass : '—'}</Td>
                    <Td>{row.passed ? row.positionInSection : '—'}</Td>
                    <Td>
                      <Link
                        to={`/exams/${examId}/marksheets?sectionId=${row.sectionId}&enrollmentId=${row.enrollmentId}`}
                        className="text-xs font-medium text-indigo-700 hover:underline"
                      >
                        মার্কশিট
                      </Link>
                    </Td>
                  </tr>
                ))}
              </Table>
            )}
          </LoadState>
        </Card>
      )}

      {canLoad && results.data && (
        <div className="mt-4">
          <Alert type="info">
            মেধাক্রম শুধু পাস করাদের মধ্যে, আগে GPA আর সমান হলে মোট নম্বর দিয়ে। দুজনের দুটোই সমান হলে একই অবস্থান (১, ২, ২,
            ৪...)। ফেল করাদের অবস্থান নেই।
          </Alert>
        </div>
      )}
    </>
  )
}