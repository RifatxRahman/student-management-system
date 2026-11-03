import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { api } from '../api/client.js'
import { ConfirmModal } from '../components/extra.jsx'
import { Alert, Badge, Button, Card, EmptyState, LoadState, PageHeader, Table, Td } from '../components/ui.jsx'
import { useQuery } from '../hooks/useQuery.js'
import { plain } from '../utils/format.js'

function Summary({ label, value, tone }) {
  return (
    <Card className="p-4">
      <p className="text-xs text-gray-500">{label}</p>
      <p className={`mt-1 text-xl font-semibold ${tone === 'bad' ? 'text-red-600' : 'text-gray-900'}`}>{value}</p>
    </Card>
  )
}

export default function PublishPage() {
  const { examId } = useParams()
  const navigate = useNavigate()
  const preview = useQuery(`/api/exams/${examId}/preview`)
  const [confirming, setConfirming] = useState(false)

  async function publish() {
    await api(`/api/exams/${examId}/publish`, { method: 'POST' })
    navigate(`/exams/${examId}/results`)
  }

  const data = preview.data
  const canPublish = Boolean(data) && data.missingTotal === 0 && data.studentCount > 0

  return (
    <>
      <div className="mb-3">
        <Link to={`/exams/${examId}`} className="text-sm text-gray-500 hover:text-gray-700">
          ← পরীক্ষায় ফিরে যান
        </Link>
      </div>

      <PageHeader
        title="প্রিভিউ ও প্রকাশ"
        subtitle={data ? `${data.exam.name} · ${data.exam.className}` : 'ফল প্রকাশের আগে যাচাই'}
        action={
          <Button disabled={!canPublish} onClick={() => setConfirming(true)}>
            ফল প্রকাশ করুন
          </Button>
        }
      />

      <LoadState loading={preview.loading} error={preview.error}>
        {data && (
          <div className="space-y-4">
            <div className="grid gap-3 sm:grid-cols-3">
              <Summary label="চলমান শিক্ষার্থী" value={data.studentCount} />
              <Summary label="মার্কস সম্পূর্ণ" value={data.completeCount} />
              <Summary
                label="মার্কস বাকি (বিষয় ধরে)"
                value={data.missingTotal}
                tone={data.missingTotal > 0 ? 'bad' : 'good'}
              />
            </div>

            {data.studentCount === 0 && (
              <Alert>এই ক্লাসে এই সেশনে কোনো চলমান শিক্ষার্থী নেই, তাই প্রকাশ করার মতো কিছু নেই।</Alert>
            )}

            {data.missingTotal > 0 && (
              <Card className="p-4">
                <p className="mb-2 text-sm font-semibold text-red-700">
                  এই মার্কসগুলো বাকি ({data.missingTotal}টা
                  {data.missing.length < data.missingTotal ? `, প্রথম ${data.missing.length}টা দেখানো হলো` : ''})
                </p>
                <ul className="divide-y divide-gray-100 text-sm">
                  {data.missing.map((m) => (
                    <li key={`${m.enrollmentId}-${m.subjectName}`} className="flex flex-wrap items-center justify-between gap-2 py-1.5">
                      <span>
                        {m.sectionName} · রোল {m.rollNumber} · {m.studentName}
                      </span>
                      <Badge tone="red">{m.subjectName}</Badge>
                    </li>
                  ))}
                </ul>
                <p className="mt-2 text-xs text-gray-500">
                  মার্কস এন্ট্রি পাতা থেকে এগুলো দিয়ে এসে আবার এই পাতা খুলুন (কিছু শিক্ষকের বাকি থাকলে তাঁকে জানান)।
                </p>
              </Card>
            )}

            <Card>
              {data.rows.length === 0 ? (
                <EmptyState>যাদের সব আবশ্যিক বিষয়ের মার্কস সম্পূর্ণ, তাদের ফল এখানে দেখাবে।</EmptyState>
              ) : (
                <Table headers={['শাখা', 'রোল', 'নাম', 'মোট', 'GPA', 'গ্রেড', 'ফল', 'শ্রেণিতে', 'শাখায়']}>
                  {data.rows.map((row) => (
                    <tr key={row.enrollmentId}>
                      <Td>{row.sectionName}</Td>
                      <Td>{row.rollNumber}</Td>
                      <Td className="font-medium text-gray-900">{row.studentName}</Td>
                      <Td>
                        {plain(row.totalMarks)} / {plain(row.totalFullMarks)}
                      </Td>
                      <Td>{Number(row.gpa).toFixed(2)}</Td>
                      <Td>{row.grade}</Td>
                      <Td>
                        <Badge tone={row.passed ? 'green' : 'red'}>{row.passed ? 'পাস' : 'ফেল'}</Badge>
                      </Td>
                      <Td>{row.passed ? row.positionInClass : '—'}</Td>
                      <Td>{row.passed ? row.positionInSection : '—'}</Td>
                    </tr>
                  ))}
                </Table>
              )}
            </Card>
          </div>
        )}
      </LoadState>

      {confirming && data && (
        <ConfirmModal
          title="ফল প্রকাশ করবেন?"
          message={`${data.studentCount} জনের ফল প্রকাশ হবে আর পরীক্ষা Lock হয়ে যাবে। এরপর শিক্ষক আর মার্কস বদলাতে পারবেন না, শুধু Admin কারণ লিখে সংশোধন করতে পারবেন। প্রকাশ আর ফেরানো যায় না।`}
          confirmLabel="প্রকাশ করুন"
          onConfirm={publish}
          onClose={() => setConfirming(false)}
        />
      )}
    </>
  )
}