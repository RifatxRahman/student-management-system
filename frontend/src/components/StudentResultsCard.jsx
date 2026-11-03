import { Link } from 'react-router'
import { useQuery } from '../hooks/useQuery.js'
import { examTypeLabel } from '../utils/format.js'
import { Badge, Card, EmptyState, LoadState, Table, Td } from './ui.jsx'

export default function StudentResultsCard({ studentId }) {
  const { data, error, loading } = useQuery(`/api/students/${studentId}/results`)

  return (
    <section>
      <h2 className="mb-3 text-lg font-semibold text-gray-900">পরীক্ষার ফল</h2>
      <Card>
        <LoadState loading={loading} error={error}>
          {data?.length === 0 ? (
            <EmptyState>এখনো কোনো পরীক্ষার ফল প্রকাশ হয়নি।</EmptyState>
          ) : (
            <Table headers={['পরীক্ষা', 'সেশন', 'ক্লাস', 'GPA', 'গ্রেড', 'ফল', 'অবস্থান', '']}>
              {data?.map((r) => (
                <tr key={r.resultId}>
                  <Td className="font-medium text-gray-900">
                    <Link to={`/exams/${r.examId}`} className="hover:text-indigo-700">
                      {r.examName}
                    </Link>
                    <p className="text-xs font-normal text-gray-500">{examTypeLabel(r.examType)}</p>
                  </Td>
                  <Td>{r.sessionName}</Td>
                  <Td>
                    {r.className} / {r.sectionName}
                  </Td>
                  <Td>{Number(r.gpa).toFixed(2)}</Td>
                  <Td>{r.grade}</Td>
                  <Td>
                    <Badge tone={r.passed ? 'green' : 'red'}>{r.passed ? 'পাস' : 'ফেল'}</Badge>
                  </Td>
                  <Td className="text-xs">
                    {r.passed ? `শ্রেণিতে ${r.positionInClass}, শাখায় ${r.positionInSection}` : '—'}
                  </Td>
                  <Td>
                    <Link
                      to={`/exams/${r.examId}/marksheets?sectionId=${r.sectionId}&enrollmentId=${r.enrollmentId}`}
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
    </section>
  )
}