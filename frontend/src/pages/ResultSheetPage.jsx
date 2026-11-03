import { useParams, useSearchParams } from 'react-router'
import PrintFrame from '../components/print/PrintFrame.jsx'
import { Card, EmptyState, LoadState } from '../components/ui.jsx'
import { useQuery } from '../hooks/useQuery.js'
import { bnNumber, examTypeLabel, toBanglaDigits } from '../utils/format.js'

// ঠিকানা: /exams/{পরীক্ষার নম্বর}/result-sheet?sectionId=..   (Admin sectionId ছাড়াও দিতে পারেন, শিক্ষককে দিতেই হবে)
export default function ResultSheetPage() {
  const { examId } = useParams()
  const [params] = useSearchParams()
  const sectionId = params.get('sectionId')

  const exam = useQuery(`/api/exams/${examId}`)
  const results = useQuery(`/api/exams/${examId}/results${sectionId ? `?sectionId=${sectionId}` : ''}`)
  const institution = useQuery('/api/institution')

  const loading = exam.loading || results.loading || institution.loading
  const error = exam.error || results.error || institution.error

  return (
    <PrintFrame title="রেজাল্ট শিট" orientation="landscape" hint="প্রিন্টে পাতা আড়াআড়ি (Landscape) হবে।">
      <div className="mx-auto max-w-[297mm] px-4 print:max-w-none print:px-0">
        <LoadState loading={loading} error={error}>
          {exam.data && institution.data && results.data && (
            <div className="bg-white p-6 text-gray-900 shadow print:p-0 print:shadow-none">
              <header className="border-b-2 border-gray-800 pb-2 text-center">
                <h2 className="text-xl font-bold">{institution.data.name}</h2>
                <p className="mt-1 text-sm font-semibold">
                  রেজাল্ট শিট · {exam.data.exam.name} ({examTypeLabel(exam.data.exam.type)})
                </p>
                <p className="text-xs">
                  শ্রেণি: {exam.data.exam.className} · শিক্ষাবর্ষ: {toBanglaDigits(exam.data.exam.sessionName)} · শাখা:{' '}
                  {sectionId && results.data.length > 0 ? results.data[0].sectionName : 'সব'}
                </p>
              </header>

              {results.data.length === 0 ? (
                <Card className="mt-4">
                  <EmptyState>কোনো ফল পাওয়া যায়নি।</EmptyState>
                </Card>
              ) : (
                <table className="mt-3 w-full border-collapse text-[11px]">
                  <thead>
                    <tr className="bg-gray-100">
                      <th className="border border-gray-500 px-1.5 py-1">শাখা</th>
                      <th className="border border-gray-500 px-1.5 py-1">রোল</th>
                      <th className="border border-gray-500 px-1.5 py-1 text-left">নাম</th>
                      <th className="border border-gray-500 px-1.5 py-1">আইডি</th>
                      {exam.data.subjects.map((es) => (
                        <th key={es.id} className="border border-gray-500 px-1.5 py-1">
                          {es.subjectName}
                          <br />
                          <span className="font-normal">({bnNumber(es.fullMarks)})</span>
                        </th>
                      ))}
                      <th className="border border-gray-500 px-1.5 py-1">মোট</th>
                      <th className="border border-gray-500 px-1.5 py-1">জিপিএ</th>
                      <th className="border border-gray-500 px-1.5 py-1">গ্রেড</th>
                      <th className="border border-gray-500 px-1.5 py-1">ফল</th>
                      <th className="border border-gray-500 px-1.5 py-1">শ্রেণিতে</th>
                      <th className="border border-gray-500 px-1.5 py-1">শাখায়</th>
                    </tr>
                  </thead>
                  <tbody>
                    {results.data.map((row) => (
                      <tr key={row.enrollmentId}>
                        <td className="border border-gray-500 px-1.5 py-1 text-center">{row.sectionName}</td>
                        <td className="border border-gray-500 px-1.5 py-1 text-center">{bnNumber(row.rollNumber)}</td>
                        <td className="border border-gray-500 px-1.5 py-1">{row.studentName}</td>
                        <td className="border border-gray-500 px-1.5 py-1 font-mono">{toBanglaDigits(row.studentCode)}</td>
                        {exam.data.subjects.map((es) => {
                          const s = row.subjects.find((x) => x.examSubjectId === es.id)
                          let text = '—'
                          if (s) {
                            text = s.absent ? 'অনু.' : `${bnNumber(s.marks)} (${s.grade})${s.passed ? '' : '*'}`
                          }
                          return (
                            <td key={es.id} className="border border-gray-500 px-1.5 py-1 text-center">
                              {text}
                            </td>
                          )
                        })}
                        <td className="border border-gray-500 px-1.5 py-1 text-center">{bnNumber(row.totalMarks)}</td>
                        <td className="border border-gray-500 px-1.5 py-1 text-center">{bnNumber(row.gpa, 2, 2)}</td>
                        <td className="border border-gray-500 px-1.5 py-1 text-center">{row.grade}</td>
                        <td className="border border-gray-500 px-1.5 py-1 text-center">{row.passed ? 'উত্তীর্ণ' : 'অনুত্তীর্ণ'}</td>
                        <td className="border border-gray-500 px-1.5 py-1 text-center">
                          {row.passed ? bnNumber(row.positionInClass) : '—'}
                        </td>
                        <td className="border border-gray-500 px-1.5 py-1 text-center">
                          {row.passed ? bnNumber(row.positionInSection) : '—'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}

              <p className="mt-2 text-[10px] text-gray-600">
                * ফেল করা বিষয়। অনু. = অনুপস্থিত। (—) = বিষয়টা নেওয়া হয়নি (ঐচ্ছিক বিষয়)।
              </p>

              <footer className="mt-10 grid grid-cols-3 gap-6 text-center text-xs">
                <div className="border-t border-gray-700 pt-2">শ্রেণি শিক্ষকের স্বাক্ষর</div>
                <div className="border-t border-gray-700 pt-2">পরীক্ষা নিয়ন্ত্রকের স্বাক্ষর</div>
                <div className="border-t border-gray-700 pt-2">অধ্যক্ষের স্বাক্ষর</div>
              </footer>
            </div>
          )}
        </LoadState>
      </div>
    </PrintFrame>
  )
}