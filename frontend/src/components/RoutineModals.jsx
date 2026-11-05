import { useState } from 'react'
import { api } from '../api/client.js'
import { useQuery } from '../hooks/useQuery.js'
import { dayLabel, shortTime } from '../utils/format.js'
import { ConfirmModal, FormActions } from './extra.jsx'
import { Alert, Button, Modal, SelectField } from './ui.jsx'

// entry থাকলে বদল (ও মোছা), না থাকলে নতুন বসানো। সফল হলে onDone() ডাকে।
export default function RoutineCellModal({ sessionId, sectionId, day, slot, entry, onDone, onClose }) {
  const options = useQuery(`/api/timetable/options?sectionId=${sectionId}&sessionId=${sessionId}`)
  const rooms = useQuery('/api/rooms')
  const [pair, setPair] = useState(entry ? `${entry.subjectId}:${entry.teacherId}` : '')
  const [roomId, setRoomId] = useState(entry?.roomId ? String(entry.roomId) : '')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [confirming, setConfirming] = useState(false)

  const pairOptions = (options.data ?? []).map((o) => ({
    value: `${o.subjectId}:${o.teacherId}`,
    label: `${o.subjectName} — ${o.teacherName}`,
  }))
  // বদলের সময় এখনকার জোড়াটা (বরাদ্দ সরে গেলেও) তালিকায় থাকে, যাতে ভুলে বদলে না যায়
  const currentValue = entry ? `${entry.subjectId}:${entry.teacherId}` : ''
  if (entry && !pairOptions.some((o) => o.value === currentValue)) {
    pairOptions.unshift({ value: currentValue, label: `${entry.subjectName} — ${entry.teacherName} (বরাদ্দ নেই)` })
  }
  const selectOptions = [{ value: '', label: 'বাছুন' }, ...pairOptions]

  const roomOptions = [
    { value: '', label: 'রুম ছাড়া' },
    ...(rooms.data ?? [])
      .filter((r) => r.active || String(r.id) === roomId)
      .map((r) => ({ value: String(r.id), label: r.capacity ? `${r.name} (ধারণক্ষমতা ${r.capacity})` : r.name })),
  ]

  async function submit(event) {
    event.preventDefault()
    const [subjectId, teacherId] = pair.split(':').map(Number)
    const body = { subjectId, teacherId, roomId: roomId ? Number(roomId) : null }
    setBusy(true)
    setError('')
    try {
      if (entry) {
        await api(`/api/timetable/${entry.id}`, { method: 'PUT', body })
      } else {
        await api('/api/timetable', {
          method: 'POST',
          body: {
            ...body,
            sessionId: Number(sessionId),
            sectionId: Number(sectionId),
            dayOfWeek: day,
            timeSlotId: slot.id,
          },
        })
      }
      onDone()
    } catch (err) {
      setError(err.message)
      setBusy(false)
    }
  }

  async function remove() {
    await api(`/api/timetable/${entry.id}`, { method: 'DELETE' })
    onDone()
  }

  const title = `${dayLabel(day)} · ${slot.name} (${shortTime(slot.startTime)}–${shortTime(slot.endTime)})`

  if (confirming) {
    return (
      <ConfirmModal
        title="রুটিন থেকে সরান"
        message={`${entry.subjectName} (${entry.teacherName}) ক্লাসটা ${title} ঘর থেকে সরাবেন? ঘরটা আবার ফাঁকা হয়ে যাবে।`}
        confirmLabel="সরান"
        danger
        onConfirm={remove}
        onClose={() => setConfirming(false)}
      />
    )
  }

  return (
    <Modal title={title} onClose={onClose}>
      <form onSubmit={submit} className="space-y-4">
        {error && <Alert>{error}</Alert>}
        {options.data?.length === 0 && !entry && (
          <Alert type="info">
            এই সেকশনে এখনো কোনো শিক্ষকের বিষয়-বরাদ্দ নেই। আগে শিক্ষক পাতা থেকে শিক্ষককে এই সেকশনের বিষয়ে বরাদ্দ দিন।
          </Alert>
        )}
        <SelectField
          label="বিষয় ও শিক্ষক"
          options={selectOptions}
          value={pair}
          onChange={(event) => setPair(event.target.value)}
          required
        />
        <SelectField
          label="রুম (ঐচ্ছিক)"
          options={roomOptions}
          value={roomId}
          onChange={(event) => setRoomId(event.target.value)}
        />
        <FormActions busy={busy} submitLabel={entry ? 'সংরক্ষণ করুন' : 'বসান'} onCancel={onClose} />
        {entry && (
          <div className="border-t border-gray-100 pt-3">
            <Button variant="danger" size="sm" onClick={() => setConfirming(true)}>
              এই ঘর থেকে সরান
            </Button>
          </div>
        )}
      </form>
    </Modal>
  )
}