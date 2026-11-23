import { plain } from '../../utils/format.js'

export default function AdmitCard({ institution, exam, student, subjects }) {
  const schoolName = institution?.name || 'আদর্শ উচ্চ বিদ্যালয়'
  const schoolAddress = institution?.address || 'ঢাকা, বাংলাদেশ'
  const schoolPhone = institution?.phone || ''

  return (
    <div className="mb-6 rounded-lg border-2 border-indigo-900 bg-white p-5 print:mb-4 print:rounded-none print:border-2 print:border-black print:p-4 print:break-inside-avoid">
      {/* ১. প্রাতিষ্ঠানিক হেডার */}
      <div className="border-b-2 border-indigo-900 pb-3 text-center print:border-black">
        <h2 className="text-xl font-bold tracking-tight text-indigo-950 print:text-black">
          {schoolName}
        </h2>
        <p className="text-xs text-gray-600 print:text-black">{schoolAddress} {schoolPhone && `· ফোন: ${schoolPhone}`}</p>
        
        <div className="mt-2 flex items-center justify-center">
          <span className="rounded-md bg-indigo-900 px-4 py-1 text-xs font-bold uppercase tracking-wider text-white print:bg-black print:text-white">
            প্রবেশপত্র / ADMIT CARD
          </span>
        </div>
        <p className="mt-1 text-sm font-semibold text-gray-800 print:text-black">
          {exam.name} ({exam.sessionName || 'শিক্ষাবর্ষ'})
        </p>
      </div>

      {/* ২. শিক্ষার্থী ও প্রবেশপত্র বিবরণী */}
      <div className="my-3 flex items-start justify-between gap-4">
        <div className="grid flex-1 grid-cols-2 gap-x-4 gap-y-1.5 text-xs text-gray-900">
          <div>
            <span className="text-gray-500">শিক্ষার্থীর নাম: </span>
            <strong className="text-sm font-bold text-gray-950">{student.fullName}</strong>
          </div>
          <div>
            <span className="text-gray-500">শিক্ষার্থী আইডি (Code): </span>
            <strong className="font-mono text-sm font-bold text-indigo-900 print:text-black">{student.studentCode}</strong>
          </div>
          <div>
            <span className="text-gray-500">শ্রেণি: </span>
            <strong className="font-semibold">{student.className || exam.className}</strong>
          </div>
          <div>
            <span className="text-gray-500">শাখা / সেকশন: </span>
            <strong className="font-semibold">{student.sectionName || '—'}</strong>
          </div>
          <div>
            <span className="text-gray-500">রোল নম্বর: </span>
            <strong className="text-sm font-bold text-indigo-900 print:text-black">{student.rollNumber ?? '—'}</strong>
          </div>
          <div>
            <span className="text-gray-500">লিঙ্গ: </span>
            <span>{student.gender === 'MALE' ? 'ছাত্র' : student.gender === 'FEMALE' ? 'ছাত্রী' : 'অন্যান্য'}</span>
          </div>
        </div>

        {/* ছবি ফ্রেম */}
        <div className="flex h-24 w-20 flex-shrink-0 flex-col items-center justify-center rounded border border-dashed border-gray-400 bg-gray-50 text-[10px] text-gray-400 print:border-black print:text-black">
          <span>পাসপোর্ট</span>
          <span>সাইজ ছবি</span>
        </div>
      </div>

      {/* ৩. পরীক্ষার বিষয়সূচি তালিকা */}
      <div className="my-2 overflow-hidden rounded border border-gray-300 print:border-black">
        <table className="w-full text-left text-xs">
          <thead className="bg-gray-100 font-semibold text-gray-700 print:bg-gray-200 print:text-black">
            <tr className="border-b border-gray-300 print:border-black">
              <th className="py-1 px-2 text-center w-8">ক্রমিক</th>
              <th className="py-1 px-2">বিষয়</th>
              <th className="py-1 px-2 text-center">কোড</th>
              <th className="py-1 px-2 text-center">পূর্ণমান</th>
              <th className="py-1 px-2 text-center">তারিখ ও সময়</th>
              <th className="py-1 px-2 text-center">কক্ষ নং</th>
              <th className="py-1 px-2 text-center">পরিদর্শকের স্বাক্ষর</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200 text-gray-800 print:divide-black print:text-black">
            {subjects && subjects.length > 0 ? (
              subjects.map((sub, idx) => (
                <tr key={sub.id || idx}>
                  <td className="py-1 px-2 text-center text-gray-500">{idx + 1}</td>
                  <td className="py-1 px-2 font-medium">{sub.subjectName}</td>
                  <td className="py-1 px-2 text-center font-mono">{sub.subjectCode}</td>
                  <td className="py-1 px-2 text-center">{plain(sub.fullMarks)}</td>
                  <td className="py-1 px-2 text-center text-gray-500">সময়সূচি অনুযায়ী</td>
                  <td className="py-1 px-2 text-center text-gray-500"></td>
                  <td className="py-1 px-2 text-center"></td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="7" className="py-2 text-center text-gray-400">পরীক্ষার বিষয় তালিকা প্রস্তুত করা হচ্ছে</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* ৪. পরীক্ষার্থীদের বিশেষ নির্দেশাবলী */}
      <div className="my-2 rounded bg-gray-50 p-2 text-[10px] leading-tight text-gray-600 print:bg-white print:text-black">
        <p className="font-bold text-gray-700 print:text-black mb-0.5">পরীক্ষার্থীদের জন্য বিশেষ নির্দেশাবলী:</p>
        <p>১. পরীক্ষা শুরুর অন্তত ২০ মিনিট পূর্বে নিজ নিজ আসনে প্রবেশ করতে হবে। প্রবেশপত্র প্রদর্শন ব্যতিরেকে কোনো পরীক্ষার্থীকে পরীক্ষা কক্ষে প্রবেশ করতে দেওয়া হবে না।</p>
        <p>২. পরীক্ষা কক্ষে মোবাইল ফোন, ক্যালকুলেটর (অননুমোদিত) বা কোনো প্রকার ইলেকট্রনিক ডিভাইস বহন সম্পূর্ণ নিষিদ্ধ ও বহিষ্কারযোগ্য অপরাধ।</p>
      </div>

      {/* ৫. স্বাক্ষর ও সিল */}
      <div className="mt-6 flex items-end justify-between px-4 pt-4 text-xs font-semibold text-gray-800 print:text-black">
        <div className="text-center">
          <div className="w-32 border-t border-gray-400 print:border-black pt-1">
            শ্রেণি শিক্ষকের স্বাক্ষর
          </div>
        </div>
        <div className="rounded-full border border-dashed border-gray-300 px-3 py-1 text-[10px] text-gray-400 print:border-black print:text-black">
          বিদ্যালয়ের সিলমোহর
        </div>
        <div className="text-center">
          <div className="w-36 border-t border-gray-400 print:border-black pt-1">
            অধ্যক্ষ / প্রধান শিক্ষকের স্বাক্ষর
          </div>
        </div>
      </div>
    </div>
  )
}
