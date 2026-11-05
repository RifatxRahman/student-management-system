import { dayLabel, shortTime, WEEK_DAYS } from '../utils/format.js'

// mode: 'section' (কার্ডে শিক্ষকের নাম) অথবা 'teacher' (কার্ডে সেকশনের নাম)
// canEdit = true হলে ফাঁকা ঘরে + আর বসানো ঘর চাপা যায়
export default function RoutineGrid({ entries, slots, mode, canEdit, onCellClick, onEntryClick }) {
  const usedDays = new Set(entries.map((e) => e.dayOfWeek))
  // শুক্রবার (সাপ্তাহিক ছুটি) শুধু তখনই দেখাই যখন সেদিন কিছু বসানো আছে
  const days = WEEK_DAYS.filter((day) => day !== 'FRIDAY' || usedDays.has(day))

  const usedSlots = new Set(entries.map((e) => e.timeSlotId))
  const rows = slots.filter((slot) => slot.active || usedSlots.has(slot.id))
  const byCell = new Map(entries.map((e) => [`${e.dayOfWeek}:${e.timeSlotId}`, e]))

  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[640px] border-collapse text-sm">
        <thead>
          <tr className="bg-gray-50">
            <th className="border border-gray-200 px-3 py-2 text-left font-semibold text-gray-700">পিরিয়ড</th>
            {days.map((day) => (
              <th key={day} className="border border-gray-200 px-3 py-2 text-left font-semibold text-gray-700">
                {dayLabel(day)}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((slot) => (
            <tr key={slot.id}>
              <td className="border border-gray-200 px-3 py-2 align-top">
                <p className="font-medium text-gray-900">{slot.name}</p>
                <p className="text-xs text-gray-500">
                  {shortTime(slot.startTime)}–{shortTime(slot.endTime)}
                </p>
              </td>
              {days.map((day) => {
                const entry = byCell.get(`${day}:${slot.id}`)
                return (
                  <td key={day} className="h-16 border border-gray-200 p-1 align-top">
                    {entry ? (
                      <button
                        type="button"
                        disabled={!canEdit}
                        onClick={() => onEntryClick(entry)}
                        className={`block w-full rounded-lg bg-indigo-50 px-2 py-1.5 text-left ${
                          canEdit ? 'hover:bg-indigo-100' : 'cursor-default'
                        }`}
                      >
                        <span className="block text-sm font-medium text-indigo-900">{entry.subjectName}</span>
                        <span className="block text-xs text-gray-600">
                          {mode === 'section' ? entry.teacherName : `${entry.className} / ${entry.sectionName}`}
                        </span>
                        {entry.roomName && <span className="block text-xs text-gray-500">রুম {entry.roomName}</span>}
                      </button>
                    ) : (
                      canEdit && (
                        <button
                          type="button"
                          onClick={() => onCellClick(day, slot)}
                          aria-label={`${dayLabel(day)} ${slot.name} ঘরে ক্লাস বসান`}
                          className="flex h-full min-h-12 w-full items-center justify-center rounded-lg text-lg text-gray-300 hover:bg-gray-50 hover:text-indigo-600"
                        >
                          +
                        </button>
                      )
                    )}
                  </td>
                )
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}