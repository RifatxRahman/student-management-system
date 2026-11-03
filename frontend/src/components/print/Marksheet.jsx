import { bnNumber, examTypeLabel, formatInstantDate, toBanglaDigits } from '../../utils/format.js'

// row = GET /api/exams/{id}/results-এর একটা সারি, exam = পরীক্ষার তথ্য (ExamDetail.exam)
export default function Marksheet({ institution, exam, row, gradeScale }) {
  return (
    <article className="mx-auto mb-6 w-[190mm] max-w-full bg-white p-8 text-gray-900 shadow print:mb-0 print:w-full print:break-after-page print:p-0 print:shadow-none print:last:break-after-auto">
      <header className="border-b-2 border-gray-800 pb-3 text-center">
        <h2 className="text-2xl font-bold">{institution.name}</h2>
        {institution.address && <p className="mt-1 text-sm">{institution.address}</p>}
        {institution.phone && <p className="text-sm">ফোন: {institution.phone}</p>}
        <p className="mt-3 text-lg font-semibold">একাডেমিক ট্রান্সক্রিপ্ট (মার্কশিট)</p>
        <p className="text-sm">
          {exam.name} ({examTypeLabel(exam.type)}) · শিক্ষাবর্ষ {toBanglaDigits(exam.sessionName)}
        </p>
      </header>

      <section className="mt-4 grid grid-cols-2 gap-x-8 gap-y-1 text-sm">
        <p>
          শিক্ষার্থীর নাম: <span className="font-semibold">{row.studentName}</span>
        </p>
        <p>
          আইডি: <span className="font-semibold">{toBanglaDigits(row.studentCode)}</span>
        </p>
        <p>
          শ্রেণি: <span className="font-semibold">{exam.className}</span> · শাখা:{' '}
          <span className="font-semibold">{row.sectionName}</span>
        </p>
        <p>
          রোল: <span className="font-semibold">{bnNumber(row.rollNumber)}</span>
        </p>
      </section>

      <table className="mt-4 w-full border-collapse text-sm">
        <thead>
          <tr className="bg-gray-100">
            {['বিষয়', 'পূর্ণমান', 'প্রাপ্ত নম্বর', 'গ্রেড', 'গ্রেড পয়েন্ট'].map((heading) => (
              <th key={heading} className="border border-gray-500 px-3 py-1.5 text-left font-semibold">
                {heading}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {row.subjects.map((s) => (
            <tr key={s.examSubjectId}>
              <td className="border border-gray-500 px-3 py-1.5">
                {s.subjectName}
                {s.optional ? ' (ঐচ্ছিক)' : ''}
              </td>
              <td className="border border-gray-500 px-3 py-1.5">{bnNumber(s.fullMarks)}</td>
              <td className="border border-gray-500 px-3 py-1.5">{s.absent ? 'অনুপস্থিত' : bnNumber(s.marks)}</td>
              <td className="border border-gray-500 px-3 py-1.5">{s.grade}</td>
              <td className="border border-gray-500 px-3 py-1.5">{bnNumber(s.gradePoint, 2, 2)}</td>
            </tr>
          ))}
          <tr className="bg-gray-50 font-semibold">
            <td className="border border-gray-500 px-3 py-1.5">সর্বমোট</td>
            <td className="border border-gray-500 px-3 py-1.5">{bnNumber(row.totalFullMarks)}</td>
            <td className="border border-gray-500 px-3 py-1.5">{bnNumber(row.totalMarks)}</td>
            <td className="border border-gray-500 px-3 py-1.5" colSpan={2}>
              গড় নম্বর: {bnNumber(row.averageMarks)}
            </td>
          </tr>
        </tbody>
      </table>

      <section className="mt-4 grid grid-cols-4 gap-3 text-center text-sm">
        <div className="rounded border border-gray-500 p-2">
          <p className="text-xs text-gray-600">জিপিএ</p>
          <p className="text-lg font-bold">{bnNumber(row.gpa, 2, 2)}</p>
        </div>
        <div className="rounded border border-gray-500 p-2">
          <p className="text-xs text-gray-600">গ্রেড</p>
          <p className="text-lg font-bold">{row.grade}</p>
        </div>
        <div className="rounded border border-gray-500 p-2">
          <p className="text-xs text-gray-600">ফলাফল</p>
          <p className="text-lg font-bold">{row.passed ? 'উত্তীর্ণ' : 'অনুত্তীর্ণ'}</p>
        </div>
        <div className="rounded border border-gray-500 p-2">
          <p className="text-xs text-gray-600">মেধাক্রম (শ্রেণি / শাখা)</p>
          <p className="text-lg font-bold">
            {row.passed ? `${bnNumber(row.positionInClass)} / ${bnNumber(row.positionInSection)}` : '—'}
          </p>
        </div>
      </section>

      {gradeScale.length > 0 && (
        <section className="mt-4">
          <p className="mb-1 text-xs font-semibold text-gray-700">গ্রেড স্কেল</p>
          <table className="w-full border-collapse text-xs">
            <thead>
              <tr className="bg-gray-100">
                <th className="border border-gray-400 px-2 py-1 text-left">নম্বর (শতকরা)</th>
                {gradeScale.map((band) => (
                  <th key={band.grade} className="border border-gray-400 px-2 py-1 font-medium">
                    {bnNumber(band.fromPercent)}–
                    {band.toInclusive ? bnNumber(band.toPercent) : bnNumber(Number(band.toPercent) - 0.01)}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              <tr>
                <td className="border border-gray-400 px-2 py-1">গ্রেড</td>
                {gradeScale.map((band) => (
                  <td key={band.grade} className="border border-gray-400 px-2 py-1 text-center">
                    {band.grade}
                  </td>
                ))}
              </tr>
              <tr>
                <td className="border border-gray-400 px-2 py-1">গ্রেড পয়েন্ট</td>
                {gradeScale.map((band) => (
                  <td key={band.grade} className="border border-gray-400 px-2 py-1 text-center">
                    {bnNumber(band.gradePoint, 2, 2)}
                  </td>
                ))}
              </tr>
            </tbody>
          </table>
        </section>
      )}

      <footer className="mt-12 grid grid-cols-3 gap-6 text-center text-xs">
        <div className="border-t border-gray-700 pt-2">শ্রেণি শিক্ষকের স্বাক্ষর</div>
        <div className="border-t border-gray-700 pt-2">পরীক্ষা নিয়ন্ত্রকের স্বাক্ষর</div>
        <div className="border-t border-gray-700 pt-2">অধ্যক্ষের স্বাক্ষর</div>
      </footer>

      <p className="mt-4 text-right text-[10px] text-gray-500">ফল প্রকাশের তারিখ: {formatInstantDate(exam.publishedAt)}</p>
    </article>
  )
}