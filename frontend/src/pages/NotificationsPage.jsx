import { useState } from 'react'
import { Link } from 'react-router'
import { api } from '../api/client.js'
import { Pagination } from '../components/extra.jsx'
import { Alert, Badge, Button, Card, EmptyState, LoadState, PageHeader, SelectField, Table, Td } from '../components/ui.jsx'
import { useQuery } from '../hooks/useQuery.js'
import { formatDate } from '../utils/format.js'

const STATUS_FILTER = [
  { value: '', label: 'সব' },
  { value: 'PENDING', label: 'অপেক্ষমাণ' },
  { value: 'SENT', label: 'পাঠানো হয়েছে' },
  { value: 'FAILED', label: 'ব্যর্থ' },
]

const STATUS_INFO = {
  PENDING: { label: 'অপেক্ষমাণ', tone: 'amber' },
  SENT: { label: 'পাঠানো হয়েছে', tone: 'green' },
  FAILED: { label: 'ব্যর্থ', tone: 'red' },
}

export default function NotificationsPage() {
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)
  const [busy, setBusy] = useState(false)
  const [notice, setNotice] = useState(null) // { type, text }

  const params = new URLSearchParams({ page: String(page), size: '20' })
  if (status) params.set('status', status)
  const { data, error, loading, reload } = useQuery(`/api/admin/notifications?${params}`)

  function changeStatus(value) {
    setStatus(value)
    setPage(0)
  }

  async function dispatch() {
    setBusy(true)
    setNotice(null)
    try {
      const result = await api('/api/admin/notifications/dispatch', { method: 'POST' })
      setNotice({
        type: result.failed > 0 ? 'error' : 'success',
        text: `পাঠানো ${result.sent}, ব্যর্থ ${result.failed}। ${result.note}`,
      })
      reload()
    } catch (err) {
      setNotice({ type: 'error', text: err.message })
    } finally {
      setBusy(false)
    }
  }

  async function retry(notification) {
    setNotice(null)
    try {
      await api(`/api/admin/notifications/${notification.id}/retry`, { method: 'POST' })
      reload()
    } catch (err) {
      setNotice({ type: 'error', text: err.message })
    }
  }

  return (
    <>
      <PageHeader
        title="অভিভাবকের বার্তা"
        subtitle="অনুপস্থিতির বার্তা এখানে জমা হয়"
        action={
          <Button onClick={dispatch} disabled={busy}>
            {busy ? 'অপেক্ষা করুন...' : 'এখন পাঠাও'}
          </Button>
        }
      />

      <div className="mb-4 space-y-3">
        <Alert type="info">
          আসল SMS Gateway এখনো যুক্ত হয়নি। এখন "পাঠানো হয়েছে" মানে ডেমোতে ধরা হয়েছে, অভিভাবকের ফোনে কিছু যায়নি।
        </Alert>
        {notice && <Alert type={notice.type}>{notice.text}</Alert>}
      </div>

      <div className="mb-4 max-w-xs">
        <SelectField
          label="অবস্থা"
          options={STATUS_FILTER}
          value={status}
          onChange={(event) => changeStatus(event.target.value)}
        />
      </div>

      <Card>
        <LoadState loading={loading} error={error}>
          {data?.items.length === 0 ? (
            <EmptyState>কোনো বার্তা নেই।</EmptyState>
          ) : (
            <>
              <Table headers={['তারিখ', 'শিক্ষার্থী', 'অভিভাবক', 'বার্তা', 'অবস্থা', 'চেষ্টা', 'কাজ']}>
                {data?.items.map((n) => {
                  const info = STATUS_INFO[n.status] ?? { label: n.status, tone: 'gray' }
                  return (
                    <tr key={n.id}>
                      <Td className="whitespace-nowrap">{formatDate(n.eventDate)}</Td>
                      <Td>
                        <Link to={`/students/${n.studentId}`} className="font-medium text-gray-900 hover:text-indigo-700">
                          {n.studentName}
                        </Link>
                        <p className="font-mono text-xs text-gray-500">{n.studentCode}</p>
                      </Td>
                      <Td>
                        {n.guardianName}
                        <p className="font-mono text-xs text-gray-500">{n.phone}</p>
                      </Td>
                      <Td className="max-w-xs text-xs">{n.message}</Td>
                      <Td>
                        <Badge tone={info.tone}>{info.label}</Badge>
                        {n.status === 'FAILED' && n.lastError && (
                          <p className="mt-1 max-w-[12rem] text-xs text-red-600">{n.lastError}</p>
                        )}
                      </Td>
                      <Td>{n.attempts}</Td>
                      <Td>
                        {n.status === 'FAILED' && (
                          <Button variant="secondary" size="sm" onClick={() => retry(n)}>
                            আবার চেষ্টা
                          </Button>
                        )}
                      </Td>
                    </tr>
                  )
                })}
              </Table>
              {data && (
                <Pagination
                  page={data.page}
                  totalPages={data.totalPages}
                  totalElements={data.totalElements}
                  unit="টি"
                  onChange={setPage}
                />
              )}
            </>
          )}
        </LoadState>
      </Card>
    </>
  )
}