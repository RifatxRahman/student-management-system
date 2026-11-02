import { useState } from 'react'
import { api } from '../api/client.js'
import { useAuth } from '../auth/useAuth.js'
import EntityForm from '../components/EntityForm.jsx'
import { ConfirmModal } from '../components/extra.jsx'
import { Alert, Button, Card, EmptyState, LoadState, Modal, PageHeader, SelectField, Table, Td } from '../components/ui.jsx'
import { useQuery } from '../hooks/useQuery.js'
import { blankToNull, formatDate, formatWeekday, todayISO } from '../utils/format.js'

const FIELDS = [
  { name: 'fromDate', label: 'তারিখ (শুরু)', type: 'date', required: true },
  {
    name: 'toDate',
    label: 'শেষ তারিখ (ঐচ্ছিক)',
    type: 'date',
    hint: 'একদিনের ছুটি হলে খালি রাখুন। একাধিক দিন হলে শেষ দিন দিন (একবারে সর্বোচ্চ ৬০ দিন)',
  },
  { name: 'name', label: 'ছুটির নাম', hint: 'যেমন: বিজয় দিবস', required: true },
]

export default function HolidaysPage() {
  const { user } = useAuth()
  const isAdmin = user.role === 'ADMIN'
  const [thisYear] = useState(() => Number(todayISO().slice(0, 4)))
  const [year, setYear] = useState(thisYear)
  const { data, error, loading, reload } = useQuery(`/api/holidays?from=${year}-01-01&to=${year}-12-31`)
  const [modal, setModal] = useState(null) // { type: 'add' } অথবা { type: 'remove', holiday }

  const yearOptions = [thisYear - 1, thisYear, thisYear + 1].map((y) => ({ value: String(y), label: String(y) }))

  async function add(values) {
    await api('/api/holidays', { method: 'POST', body: blankToNull(values) })
    setModal(null)
    reload()
  }

  async function remove() {
    await api(`/api/holidays/${modal.holiday.id}`, { method: 'DELETE' })
    setModal(null)
    reload()
  }

  return (
    <>
      <PageHeader
        title="ছুটির দিন"
        subtitle="এই দিনগুলোতে হাজিরা নেওয়া হয় না"
        action={isAdmin && <Button onClick={() => setModal({ type: 'add' })}>ছুটি যোগ করুন</Button>}
      />

      <div className="mb-4 max-w-xs">
        <SelectField
          label="বছর"
          options={yearOptions}
          value={String(year)}
          onChange={(event) => setYear(Number(event.target.value))}
        />
      </div>

      <div className="mb-4">
        <Alert type="info">
          সাপ্তাহিক ছুটি (যেমন শুক্রবার) এখানে দেখানো হয় না, সেগুলো আগে থেকেই হাজিরার বাইরে। আগে হাজিরা নেওয়া হয়ে গেছে
          এমন দিনে ছুটি দেওয়া যায় না।
        </Alert>
      </div>

      <Card>
        <LoadState loading={loading} error={error}>
          {data?.length === 0 ? (
            <EmptyState>{year} সালে এখনো কোনো ছুটি যোগ করা হয়নি।</EmptyState>
          ) : (
            <Table headers={['তারিখ', 'বার', 'নাম', isAdmin ? 'কাজ' : '']}>
              {data?.map((holiday) => (
                <tr key={holiday.id}>
                  <Td className="font-medium text-gray-900">{formatDate(holiday.date)}</Td>
                  <Td>{formatWeekday(holiday.date)}</Td>
                  <Td>{holiday.name}</Td>
                  <Td>
                    {isAdmin && (
                      <Button variant="ghost" size="sm" onClick={() => setModal({ type: 'remove', holiday })}>
                        মুছুন
                      </Button>
                    )}
                  </Td>
                </tr>
              ))}
            </Table>
          )}
        </LoadState>
      </Card>

      {modal?.type === 'add' && (
        <Modal title="ছুটি যোগ করুন" onClose={() => setModal(null)}>
          <EntityForm fields={FIELDS} submitLabel="যোগ করুন" onSubmit={add} onCancel={() => setModal(null)} />
        </Modal>
      )}
      {modal?.type === 'remove' && (
        <ConfirmModal
          title="ছুটি মুছুন"
          message={`${formatDate(modal.holiday.date)} (${modal.holiday.name}) ছুটিটা মুছবেন? ওই দিন থেকে আবার হাজিরা নেওয়া যাবে।`}
          confirmLabel="মুছুন"
          danger
          onConfirm={remove}
          onClose={() => setModal(null)}
        />
      )}
    </>
  )
}