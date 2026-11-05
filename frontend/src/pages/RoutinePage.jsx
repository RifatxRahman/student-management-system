import { useState } from 'react'
import { Link } from 'react-router'
import { useAuth } from '../auth/useAuth.js'
import ClassSectionFields from '../components/ClassSectionFields.jsx'
import RoutineGrid from '../components/RoutineGrid.jsx'
import RoutineCellModal from '../components/RoutineModals.jsx'
import { Alert, Button, Card, EmptyState, LoadState, PageHeader, SelectField } from '../components/ui.jsx'
import { useQuery } from '../hooks/useQuery.js'

export default function RoutinePage() {
  const { user } = useAuth()
  const isAdmin = user.role === 'ADMIN'
  const [mode, setMode] = useState(isAdmin ? 'section' : 'me') // 'section' | 'teacher' | 'me'
  const [place, setPlace] = useState({ classId: '', sectionId: '' })
  const [teacherId, setTeacherId] = useState('')
  const [cell, setCell] = useState(null) // { day, slot, entry? }

  const session = useQuery('/api/sessions/active')
  const slots = useQuery('/api/time-slots')
  const teachers = useQuery(isAdmin && mode === 'teacher' ? '/api/teachers' : null)

  let path = null
  if (mode === 'me') {
    path = '/api/timetable/me'
  } else if (mode === 'section' && place.sectionId) {
    path = `/api/timetable/sections/${place.sectionId}`
  } else if (mode === 'teacher' && teacherId) {
    path = `/api/timetable/teachers/${teacherId}`
  }
  // সক্রিয় সেশন না থাকলে রুটিনও নেই
  const routine = useQuery(session.data ? path : null)

  const tabs = isAdmin
    ? [
        { key: 'section', label: 'সেকশনের রুটিন' },
        { key: 'teacher', label: 'শিক্ষকের রুটিন' },
      ]
    : [
        { key: 'me', label: 'আমার রুটিন' },
        { key: 'section', label: 'সেকশনের রুটিন' },
      ]

  const canEdit = isAdmin && mode === 'section' && Boolean(session.data) && Boolean(place.sectionId)
  const noSession = session.error?.status === 404

  const teacherOptions = [
    { value: '', label: 'বাছুন' },
    ...(teachers.data ?? []).filter((t) => t.active).map((t) => ({ value: String(t.id), label: t.fullName })),
  ]

  function openCell(day, slot) {
    setCell({ day, slot })
  }

  function openEntry(entry) {
    const slot = (slots.data ?? []).find((s) => s.id === entry.timeSlotId)
    if (slot) setCell({ day: entry.dayOfWeek, slot, entry })
  }

  function done() {
    setCell(null)
    routine.reload()
  }

  return (
    <>
      <PageHeader
        title="ক্লাস রুটিন"
        subtitle={session.data ? `সক্রিয় সেশন: ${session.data.name}` : 'সপ্তাহের ক্লাসের ছক'}
        action={
          isAdmin && (
            <Link
              to="/routine/setup"
              className="rounded-lg bg-white px-4 py-2 text-sm font-medium text-gray-700 ring-1 ring-gray-300 hover:bg-gray-50"
            >
              রুম ও পিরিয়ড
            </Link>
          )
        }
      />

      {noSession && (
        <div className="mb-4">
          <Alert>কোনো সেশন সক্রিয় নেই। সেশন পাতা থেকে একটা সেশন সক্রিয় করুন।</Alert>
        </div>
      )}

      <div className="mb-4 flex flex-wrap gap-2">
        {tabs.map((tab) => (
          <Button key={tab.key} variant={mode === tab.key ? 'primary' : 'secondary'} size="sm" onClick={() => setMode(tab.key)}>
            {tab.label}
          </Button>
        ))}
      </div>

      {mode === 'section' && (
        <Card className="mb-4 p-4">
          <div className="grid gap-3 sm:grid-cols-2">
            <ClassSectionFields
              classId={place.classId}
              sectionId={place.sectionId}
              required
              onChange={(next) => setPlace(next)}
            />
          </div>
        </Card>
      )}

      {mode === 'teacher' && (
        <Card className="mb-4 max-w-sm p-4">
          <SelectField
            label="শিক্ষক"
            options={teacherOptions}
            value={teacherId}
            onChange={(event) => setTeacherId(event.target.value)}
          />
        </Card>
      )}

      {canEdit && (
        <div className="mb-4">
          <Alert type="info">
            ফাঁকা ঘরে <strong>+</strong> চেপে ক্লাস বসান, বসানো ঘরে চেপে বদলান বা সরান। শুধু ওই সেকশনে যে শিক্ষকের বিষয়-বরাদ্দ
            আছে তাঁকেই বসানো যায়, আর একই শিক্ষক বা রুম একই সময়ে দুজায়গায় বসানো যায় না।
          </Alert>
        </div>
      )}

      {path === null && !noSession && (
        <Card>
          <EmptyState>
            {mode === 'section' ? 'রুটিন দেখতে ক্লাস ও সেকশন বাছুন।' : 'রুটিন দেখতে একজন শিক্ষক বাছুন।'}
          </EmptyState>
        </Card>
      )}

      {path !== null && session.data && (
        <Card>
          <LoadState loading={routine.loading || slots.loading} error={routine.error || slots.error}>
            {routine.data && slots.data && (
              <>
                {slots.data.length === 0 ? (
                  <EmptyState>
                    এখনো কোনো পিরিয়ড (সময়ের স্লট) বানানো হয়নি।
                    {isAdmin && (
                      <>
                        {' '}
                        <Link to="/routine/setup" className="font-medium text-indigo-700 hover:underline">
                          রুম ও পিরিয়ড পাতা থেকে বানান
                        </Link>
                      </>
                    )}
                  </EmptyState>
                ) : (
                  <RoutineGrid
                    entries={routine.data}
                    slots={slots.data}
                    mode={mode === 'section' ? 'section' : 'teacher'}
                    canEdit={canEdit}
                    onCellClick={openCell}
                    onEntryClick={openEntry}
                  />
                )}
              </>
            )}
          </LoadState>
        </Card>
      )}

      {cell && session.data && (
        <RoutineCellModal
          sessionId={session.data.id}
          sectionId={place.sectionId}
          day={cell.day}
          slot={cell.slot}
          entry={cell.entry}
          onDone={done}
          onClose={() => setCell(null)}
        />
      )}
    </>
  )
}