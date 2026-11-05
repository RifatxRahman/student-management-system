import { useState } from 'react'
import { Link } from 'react-router'
import { api } from '../api/client.js'
import EntityForm from '../components/EntityForm.jsx'
import { ConfirmModal } from '../components/extra.jsx'
import { Badge, Button, Card, EmptyState, LoadState, Modal, PageHeader, Table, Td } from '../components/ui.jsx'
import { useQuery } from '../hooks/useQuery.js'
import { shortTime, timeInputValue } from '../utils/format.js'

const ACTIVE_OPTIONS = [
  { value: 'true', label: 'সক্রিয়' },
  { value: 'false', label: 'নিষ্ক্রিয়' },
]

const ROOM_FIELDS = [
  { name: 'name', label: 'রুমের নাম', hint: 'যেমন: ১০১', required: true },
  { name: 'capacity', label: 'ধারণক্ষমতা (ঐচ্ছিক)', type: 'number' },
]
const ROOM_EDIT_FIELDS = [...ROOM_FIELDS, { name: 'active', label: 'অবস্থা', required: true, options: ACTIVE_OPTIONS }]

const SLOT_FIELDS = [
  { name: 'name', label: 'পিরিয়ডের নাম', hint: 'যেমন: ১ম পিরিয়ড', required: true },
  { name: 'startTime', label: 'শুরুর সময়', type: 'time', required: true },
  { name: 'endTime', label: 'শেষের সময়', type: 'time', required: true },
]
const SLOT_EDIT_FIELDS = [...SLOT_FIELDS, { name: 'active', label: 'অবস্থা', required: true, options: ACTIVE_OPTIONS }]

export default function RoutineSetupPage() {
  const rooms = useQuery('/api/rooms')
  const slots = useQuery('/api/time-slots')
  // modal: { kind: 'room' | 'slot' | 'deleteRoom' | 'deleteSlot', item? }
  const [modal, setModal] = useState(null)

  function close() {
    setModal(null)
  }

  function done() {
    setModal(null)
    rooms.reload()
    slots.reload()
  }

  async function saveRoom(values) {
    const body = { name: values.name.trim(), capacity: values.capacity ? Number(values.capacity) : null }
    if (modal.item) {
      await api(`/api/rooms/${modal.item.id}`, { method: 'PUT', body: { ...body, active: values.active === 'true' } })
    } else {
      await api('/api/rooms', { method: 'POST', body })
    }
    done()
  }

  async function saveSlot(values) {
    const body = { name: values.name.trim(), startTime: values.startTime, endTime: values.endTime }
    if (modal.item) {
      await api(`/api/time-slots/${modal.item.id}`, { method: 'PUT', body: { ...body, active: values.active === 'true' } })
    } else {
      await api('/api/time-slots', { method: 'POST', body })
    }
    done()
  }

  async function deleteRoom() {
    await api(`/api/rooms/${modal.item.id}`, { method: 'DELETE' })
    done()
  }

  async function deleteSlot() {
    await api(`/api/time-slots/${modal.item.id}`, { method: 'DELETE' })
    done()
  }

  return (
    <>
      <div className="mb-3">
        <Link to="/routine" className="text-sm text-gray-500 hover:text-gray-700">
          ← রুটিনে ফিরে যান
        </Link>
      </div>

      <PageHeader title="রুম ও পিরিয়ড" subtitle="রুটিনের ছক বানানোর আগে এগুলো ঠিক করে নিন" />

      <div className="space-y-8">
        {/* ---------- পিরিয়ড ---------- */}
        <section>
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-lg font-semibold text-gray-900">পিরিয়ড (সময়ের স্লট)</h2>
            <Button size="sm" onClick={() => setModal({ kind: 'slot' })}>
              নতুন পিরিয়ড
            </Button>
          </div>
          <Card>
            <LoadState loading={slots.loading} error={slots.error}>
              {slots.data?.length === 0 ? (
                <EmptyState>এখনো কোনো পিরিয়ড নেই। প্রথমে একটা বানান (যেমন ১ম পিরিয়ড, ০৯:০০–০৯:৪৫)।</EmptyState>
              ) : (
                <Table headers={['পিরিয়ড', 'সময়', 'অবস্থা', 'কাজ']}>
                  {slots.data?.map((slot) => (
                    <tr key={slot.id}>
                      <Td className="font-medium text-gray-900">{slot.name}</Td>
                      <Td>
                        {shortTime(slot.startTime)}–{shortTime(slot.endTime)}
                      </Td>
                      <Td>
                        <Badge tone={slot.active ? 'green' : 'gray'}>{slot.active ? 'সক্রিয়' : 'নিষ্ক্রিয়'}</Badge>
                      </Td>
                      <Td>
                        <div className="flex flex-wrap gap-2">
                          <Button variant="secondary" size="sm" onClick={() => setModal({ kind: 'slot', item: slot })}>
                            সম্পাদনা
                          </Button>
                          <Button variant="ghost" size="sm" onClick={() => setModal({ kind: 'deleteSlot', item: slot })}>
                            মুছুন
                          </Button>
                        </div>
                      </Td>
                    </tr>
                  ))}
                </Table>
              )}
            </LoadState>
          </Card>
        </section>

        {/* ---------- রুম ---------- */}
        <section>
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-lg font-semibold text-gray-900">রুম</h2>
            <Button size="sm" onClick={() => setModal({ kind: 'room' })}>
              নতুন রুম
            </Button>
          </div>
          <Card>
            <LoadState loading={rooms.loading} error={rooms.error}>
              {rooms.data?.length === 0 ? (
                <EmptyState>এখনো কোনো রুম নেই। রুম ছাড়াও রুটিন বসানো যায়, তবে রুম থাকলে রুমের সংঘাতও ধরা পড়ে।</EmptyState>
              ) : (
                <Table headers={['রুম', 'ধারণক্ষমতা', 'অবস্থা', 'কাজ']}>
                  {rooms.data?.map((room) => (
                    <tr key={room.id}>
                      <Td className="font-medium text-gray-900">{room.name}</Td>
                      <Td>{room.capacity ?? '—'}</Td>
                      <Td>
                        <Badge tone={room.active ? 'green' : 'gray'}>{room.active ? 'সক্রিয়' : 'নিষ্ক্রিয়'}</Badge>
                      </Td>
                      <Td>
                        <div className="flex flex-wrap gap-2">
                          <Button variant="secondary" size="sm" onClick={() => setModal({ kind: 'room', item: room })}>
                            সম্পাদনা
                          </Button>
                          <Button variant="ghost" size="sm" onClick={() => setModal({ kind: 'deleteRoom', item: room })}>
                            মুছুন
                          </Button>
                        </div>
                      </Td>
                    </tr>
                  ))}
                </Table>
              )}
            </LoadState>
          </Card>
        </section>
      </div>

      {/* ---------- জানালাগুলো ---------- */}
      {modal?.kind === 'slot' && (
        <Modal title={modal.item ? 'পিরিয়ড সম্পাদনা' : 'নতুন পিরিয়ড'} onClose={close}>
          <EntityForm
            fields={modal.item ? SLOT_EDIT_FIELDS : SLOT_FIELDS}
            initial={
              modal.item
                ? {
                    name: modal.item.name,
                    startTime: timeInputValue(modal.item.startTime),
                    endTime: timeInputValue(modal.item.endTime),
                    active: String(modal.item.active),
                  }
                : undefined
            }
            submitLabel="সংরক্ষণ করুন"
            onSubmit={saveSlot}
            onCancel={close}
          />
        </Modal>
      )}
      {modal?.kind === 'room' && (
        <Modal title={modal.item ? 'রুম সম্পাদনা' : 'নতুন রুম'} onClose={close}>
          <EntityForm
            fields={modal.item ? ROOM_EDIT_FIELDS : ROOM_FIELDS}
            initial={
              modal.item
                ? {
                    name: modal.item.name,
                    capacity: modal.item.capacity === null ? '' : String(modal.item.capacity),
                    active: String(modal.item.active),
                  }
                : undefined
            }
            submitLabel="সংরক্ষণ করুন"
            onSubmit={saveRoom}
            onCancel={close}
          />
        </Modal>
      )}
      {modal?.kind === 'deleteSlot' && (
        <ConfirmModal
          title="পিরিয়ড মুছুন"
          message={`"${modal.item.name}" পিরিয়ডটা মুছবেন? রুটিনে ব্যবহার হচ্ছে এমন পিরিয়ড মোছা যায় না, সেটা নিষ্ক্রিয় করুন।`}
          confirmLabel="মুছুন"
          danger
          onConfirm={deleteSlot}
          onClose={close}
        />
      )}
      {modal?.kind === 'deleteRoom' && (
        <ConfirmModal
          title="রুম মুছুন"
          message={`"${modal.item.name}" রুমটা মুছবেন? রুটিনে ব্যবহার হচ্ছে এমন রুম মোছা যায় না, সেটা নিষ্ক্রিয় করুন।`}
          confirmLabel="মুছুন"
          danger
          onConfirm={deleteRoom}
          onClose={close}
        />
      )}
    </>
  )
}