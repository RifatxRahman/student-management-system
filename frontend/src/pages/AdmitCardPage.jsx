import { useParams, useSearchParams } from 'react-router'
import AdmitCard from '../components/print/AdmitCard.jsx'
import PrintFrame from '../components/print/PrintFrame.jsx'
import { Card, EmptyState, LoadState } from '../components/ui.jsx'
import { useQuery } from '../hooks/useQuery.js'

export default function AdmitCardPage() {
  const { examId } = useParams()
  const [params] = useSearchParams()
  const sectionId = params.get('sectionId')
  const studentId = params.get('studentId')

  const exam = useQuery(`/api/exams/${examId}`)
  const institution = useQuery('/api/institution')

  const examData = exam.data?.exam
  const subjects = exam.data?.subjects ?? []

  // সেশন ও ক্লাস আইডি পাওয়ার পর শিক্ষার্থীদের তালিকা আনা
  const studentQuery = examData
    ? `/api/students?sessionId=${examData.sessionId}&classId=${examData.classId}&size=300${
        sectionId ? `&sectionId=${sectionId}` : ''
      }`
    : null

  const students = useQuery(studentQuery)

  const loading = exam.loading || institution.loading || (studentQuery ? students.loading : true)
  const error = exam.error || institution.error || students.error

  const allStudents = students.data?.items ?? []
  const filteredStudents = (
    studentId ? allStudents.filter((s) => String(s.id) === studentId) : allStudents
  )
    .slice()
    .sort((a, b) => (a.sectionName || '').localeCompare(b.sectionName || '', 'bn') || (a.rollNumber ?? 0) - (b.rollNumber ?? 0))

  return (
    <PrintFrame
      title="পরীক্ষার প্রবেশপত্র (Admit Card)"
      hint={
        filteredStudents.length > 0
          ? `মোট ${filteredStudents.length} জন শিক্ষার্থীর প্রবেশপত্র তৈরি হয়েছে। প্রতি পাতায় ২টি করে প্রবেশপত্র ছাপা হবে।`
          : undefined
      }
      backTo={`/exams/${examId}`}
    >
      <div className="mx-auto max-w-[210mm] px-4 print:max-w-none print:px-0">
        <LoadState loading={loading} error={error}>
          {examData && institution.data && (
            <>
              {filteredStudents.length === 0 ? (
                <Card>
                  <EmptyState>কোনো শিক্ষার্থী পাওয়া যায়নি।</EmptyState>
                </Card>
              ) : (
                <div className="space-y-4 print:space-y-0">
                  {filteredStudents.map((st) => (
                    <AdmitCard
                      key={st.id}
                      institution={institution.data}
                      exam={examData}
                      student={st}
                      subjects={subjects}
                    />
                  ))}
                </div>
              )}
            </>
          )}
        </LoadState>
      </div>
    </PrintFrame>
  )
}
