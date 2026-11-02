import '@fontsource/noto-sans-bengali/400.css'
import '@fontsource/noto-sans-bengali/600.css'
import '@fontsource/noto-sans-bengali/700.css'

const FONT = "'Noto Sans Bengali', 'Nirmala UI', sans-serif"

const SUBJECTS = [
  { name: 'বাংলা', full: 100, got: 82, grade: 'A+', point: 5 },
  { name: 'ইংরেজি', full: 100, got: 74, grade: 'A', point: 4 },
  { name: 'গণিত', full: 100, got: 91, grade: 'A+', point: 5 },
  { name: 'বিজ্ঞান', full: 100, got: 68, grade: 'A-', point: 3.5 },
  { name: 'বাংলাদেশ ও বিশ্বপরিচয়', full: 100, got: 77, grade: 'A', point: 4 },
  { name: 'ধর্ম ও নৈতিক শিক্ষা', full: 50, got: 44, grade: 'A+', point: 5 },
]

// যুক্তাক্ষরের কঠিন নমুনা। র + ZWJ + ্য = র‍্য (র‍্যাঙ্ক-এর জন্য)
const CONJUNCTS = [
  'ক্ষ', 'জ্ঞ', 'ঙ্গ', 'ঙ্ক', 'ন্দ', 'ম্ব', 'ত্র', 'শ্র', 'ক্ক', 'ন্ত', 'ষ্ঠ', 'স্ফ', 'হ্ম', 'দ্ধ', 'ঞ্জ', 'গ্ধ',
  'ব্যাকরণ', 'বিদ্যালয়', 'সন্ধ্যা', 'উজ্জ্বল', 'মৃত্যু', 'ধর্ম', 'কর্ম', 'র্ত্য', 'ক্রিকেট', 'স্কুল', 'ঋণ',
  'অধ্যক্ষ', 'শ্রেণি', 'ষষ্ঠ', 'বিশ্বপরিচয়', 'ইংরেজি', 'ফলাফল', 'প্রকাশিত',
  'র\u200D্যাঙ্ক', 'র\u200D্যাব',
]

function bn(value, digits = 0) {
  return Number(value).toLocaleString('bn-BD', { minimumFractionDigits: digits, maximumFractionDigits: digits })
}

export default function MarksheetPrototypePage() {
  const total = SUBJECTS.reduce((sum, s) => sum + s.got, 0)
  const full = SUBJECTS.reduce((sum, s) => sum + s.full, 0)
  const gpa = SUBJECTS.reduce((sum, s) => sum + s.point, 0) / SUBJECTS.length

  return (
    <div className="min-h-screen bg-gray-100 py-8 print:bg-white print:py-0" style={{ fontFamily: FONT }}>
      <style>{'@page { size: A4; margin: 12mm; }'}</style>

      <div className="mx-auto mb-4 flex max-w-[210mm] items-center justify-between gap-4 px-4 print:hidden">
        <div>
          <h1 className="text-lg font-semibold text-gray-900">বাংলা PDF পরীক্ষা (প্রোটোটাইপ)</h1>
          <p className="text-sm text-gray-600">
            নিচের বোতাম চেপে "Save as PDF" বেছে PDF বানান, তারপর PDF খুলে যুক্তাক্ষর ঠিক আছে কি না দেখুন।
          </p>
        </div>
        <button
          type="button"
          onClick={() => window.print()}
          className="shrink-0 rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700"
        >
          প্রিন্ট / PDF করুন
        </button>
      </div>

      <div className="mx-auto max-w-[210mm] bg-white p-8 shadow print:max-w-none print:p-0 print:shadow-none">
        <header className="border-b-2 border-gray-800 pb-4 text-center">
          <h2 className="text-2xl font-bold text-gray-900">আদর্শ উচ্চ বিদ্যালয়</h2>
          <p className="mt-1 text-sm text-gray-700">ঢাকা, বাংলাদেশ</p>
          <p className="mt-3 text-lg font-semibold text-gray-900">একাডেমিক ট্রান্সক্রিপ্ট (নমুনা)</p>
          <p className="text-sm text-gray-700">বার্ষিক পরীক্ষা {bn(2026)}</p>
        </header>

        <section className="mt-5 grid grid-cols-2 gap-x-8 gap-y-1 text-sm text-gray-900">
          <p>শিক্ষার্থীর নাম: <span className="font-semibold">মোঃ আব্দুল্লাহ আল মামুন</span></p>
          <p>আইডি: <span className="font-semibold">{bn(2026)}-{bn(5, 0).padStart(5, '০')}</span></p>
          <p>শ্রেণি: <span className="font-semibold">ষষ্ঠ</span> · শাখা: <span className="font-semibold">ক</span></p>
          <p>রোল: <span className="font-semibold">{bn(5)}</span></p>
        </section>

        <table className="mt-5 w-full border-collapse text-sm">
          <thead>
            <tr className="bg-gray-100">
              {['বিষয়', 'পূর্ণমান', 'প্রাপ্ত নম্বর', 'গ্রেড', 'গ্রেড পয়েন্ট'].map((h) => (
                <th key={h} className="border border-gray-500 px-3 py-2 text-left font-semibold">
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {SUBJECTS.map((s) => (
              <tr key={s.name}>
                <td className="border border-gray-500 px-3 py-1.5">{s.name}</td>
                <td className="border border-gray-500 px-3 py-1.5">{bn(s.full)}</td>
                <td className="border border-gray-500 px-3 py-1.5">{bn(s.got)}</td>
                <td className="border border-gray-500 px-3 py-1.5">{s.grade}</td>
                <td className="border border-gray-500 px-3 py-1.5">{bn(s.point, 2)}</td>
              </tr>
            ))}
            <tr className="bg-gray-50 font-semibold">
              <td className="border border-gray-500 px-3 py-1.5">সর্বমোট</td>
              <td className="border border-gray-500 px-3 py-1.5">{bn(full)}</td>
              <td className="border border-gray-500 px-3 py-1.5">{bn(total)}</td>
              <td className="border border-gray-500 px-3 py-1.5" colSpan={2}>
                জিপিএ: {bn(gpa, 2)}
              </td>
            </tr>
          </tbody>
        </table>

        <p className="mt-4 text-sm text-gray-900">
          ফলাফল: <span className="font-semibold">উত্তীর্ণ</span> · মেধাক্রম ({'র\u200D্যাঙ্ক'}):{' '}
          <span className="font-semibold">{bn(3)}</span>
        </p>

        <section className="mt-6 rounded border border-dashed border-gray-400 p-3">
          <p className="mb-2 text-xs font-semibold text-gray-700">যুক্তাক্ষর পরীক্ষা (সবগুলো ঠিকভাবে জোড়া লাগা চাই)</p>
          <p className="text-lg leading-relaxed text-gray-900">{CONJUNCTS.join('  ')}</p>
          <p className="mt-2 text-sm text-gray-700">
            বাংলা বাক্য: "শিক্ষার্থীর ফলাফল প্রকাশিত হলো। অধ্যক্ষের স্বাক্ষর ও বিদ্যালয়ের সিলমোহর প্রয়োজন।"
          </p>
          <p className="mt-1 text-sm font-semibold text-gray-900">বোল্ড লেখা: ক্ষ জ্ঞ ঙ্গ ন্দ ষ্ঠ র‍্যাঙ্ক ধন্যবাদ</p>
        </section>

        <footer className="mt-12 grid grid-cols-3 gap-6 text-center text-xs text-gray-800">
          <div className="border-t border-gray-700 pt-2">শ্রেণি শিক্ষকের স্বাক্ষর</div>
          <div className="border-t border-gray-700 pt-2">পরীক্ষা নিয়ন্ত্রকের স্বাক্ষর</div>
          <div className="border-t border-gray-700 pt-2">অধ্যক্ষের স্বাক্ষর</div>
        </footer>
      </div>
    </div>
  )
}