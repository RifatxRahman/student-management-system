import { useParams, useSearchParams } from 'react-router'
import Marksheet from '../components/print/Marksheet.jsx'
import PrintFrame from '../components/print/PrintFrame.jsx'
import { Card, EmptyState, LoadState } from '../components/ui.jsx'
import { useQuery } from '../hooks/useQuery.js'

// ঠিকানা: /exams/{পরীক্ষার নম্বর}/marksheets?sectionId=..&enrollmentId=..
//   sectionId ও enrollmentId দুটোই দিলে শুধু একজনের, শুধু sectionId দিলে পুরো সেকশনের (প্রতিজন আলাদা পাতায়)
//   Admin sectionId ছাড়াও দিতে পারেন (পুরো ক্লাস), শিক্ষককে sectionId দিতেই হবে
export default function MarksheetPage() {
  const { examId } = useParams()
  const [params] = useSearchParams()
  const sectionId = params.get('sectionId')
  const enrollmentId = params.get('enrollmentId')

  const exam = useQuery(`/api/exams/${examId}`)
  const results = useQuery(`/api/exams/${examId}/results${sectionId ? `?sectionId=${sectionId}` : ''}`)
  const institution = useQuery('/api/institution')
  const gradeScale = useQuery('/api/grade-scale')

  const loading = exam.loading || results.loading || institution.loading || gradeScale.loading
  const error = exam.error || results.error || institution.error || gradeScale.error

  const all = results.data ?? []
  const rows = (enrollmentId ? all.filter((r) => String(r.enrollmentId) === enrollmentId) : all)
    .slice()
    .sort((a, b) => a.sectionName.localeCompare(b.sectionName, 'bn') || a.rollNumber - b.rollNumber)

  return (
    <PrintFrame
      title="মার্কশিট"
      hint={rows.length > 0 ? `${rows.length}টা মার্কশিট। প্রতিজন আলাদা পাতায় ছাপা হবে।` : undefined}
    >
      <div className="mx-auto max-w-[210mm] px-4 print:max-w-none print:px-0">
        <LoadState loading={loading} error={error}>
          {exam.data && institution.data && gradeScale.data && results.data && (
            <>
              {rows.length === 0 && (
                <Card>
                  <EmptyState>কোনো ফল পাওয়া যায়নি।</EmptyState>
                </Card>
              )}
              {rows.map((row) => (
                <Marksheet
                  key={row.enrollmentId}
                  institution={institution.data}
                  exam={exam.data.exam}
                  row={row}
                  gradeScale={gradeScale.data}
                />
              ))}
            </>
          )}
        </LoadState>
      </div>
    </PrintFrame>
  )
}