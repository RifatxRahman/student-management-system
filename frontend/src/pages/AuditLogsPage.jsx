import { useState } from 'react'
import { Pagination } from '../components/extra.jsx'
import { Badge, Card, EmptyState, LoadState, PageHeader, SelectField, Table, TextField, Td } from '../components/ui.jsx'
import { useQuery } from '../hooks/useQuery.js'
import { actionLabel, ENTITY_TYPE_OPTIONS, STATUS_OPTIONS } from '../utils/audit.js'
import { formatDateTime } from '../utils/format.js'

export default function AuditLogsPage() {
  const [filters, setFilters] = useState({ entityType: '', entityId: '', status: '', action: '' })
  const [page, setPage] = useState(0)

  function update(patch) {
    setFilters((current) => ({ ...current, ...patch }))
    setPage(0)
  }

  const params = new URLSearchParams({ page: String(page), size: '50' })
  if (filters.entityType) params.set('entityType', filters.entityType)
  if (filters.entityId) params.set('entityId', filters.entityId)
  if (filters.status) params.set('status', filters.status)
  if (filters.action) params.set('action', filters.action)

  const { data, error, loading } = useQuery(`/api/admin/audit-logs?${params}`)

  return (
    <>
      <PageHeader
        title="সিস্টেম অডিট ও নিরাপত্তা লগ (Audit Trail)"
        subtitle="কে, কখন, কোথা থেকে (IP) এবং কী কাজ করেছে তার অপরিবর্তনীয় প্রমাণ"
      />

      <Card className="mb-4 p-4">
        <div className="grid gap-3 sm:grid-cols-3">
          <SelectField
            label="বিষয়ের ধরন"
            options={ENTITY_TYPE_OPTIONS}
            value={filters.entityType}
            onChange={(event) => update({ entityType: event.target.value })}
          />
          <SelectField
            label="কাজের অবস্থা"
            options={STATUS_OPTIONS}
            value={filters.status}
            onChange={(event) => update({ status: event.target.value })}
          />
          <TextField
            label="বিষয়ের নম্বর (ঐচ্ছিক)"
            type="number"
            min="1"
            placeholder="যেমন: ID 12"
            value={filters.entityId}
            onChange={(event) => update({ entityId: event.target.value })}
          />
        </div>
      </Card>

      <Card>
        <LoadState loading={loading} error={error}>
          {data?.items.length === 0 ? (
            <EmptyState>কোনো লগ পাওয়া যায়নি।</EmptyState>
          ) : (
            <>
              <Table headers={['সময়', 'কে', 'কাজ ও মডিউল', 'বিষয়', 'আইপি ও ক্লায়েন্ট', 'অবস্থা', 'বিবরণ']}>
                {data?.items.map((log) => (
                  <tr key={log.id}>
                    <Td className="whitespace-nowrap text-xs text-gray-500 font-mono">
                      {formatDateTime(log.createdAt)}
                    </Td>
                    <Td className="font-mono text-xs font-semibold text-gray-800">
                      {log.actorUsername ? log.actorUsername : <span className="text-gray-400">সিস্টেম</span>}
                    </Td>
                    <Td>
                      <p className="font-medium text-gray-900 text-xs">{actionLabel(log.action)}</p>
                      <p className="font-mono text-[11px] text-gray-400">{log.action}</p>
                    </Td>
                    <Td className="whitespace-nowrap text-xs">
                      <span className="font-medium text-gray-700">{log.entityType}</span>
                      {log.entityId ? <span className="font-mono text-gray-400"> #{log.entityId}</span> : ''}
                    </Td>
                    <Td className="text-xs">
                      <div className="font-mono text-indigo-600 font-medium">
                        {log.ipAddress || '127.0.0.1'}
                      </div>
                      <div className="text-[11px] text-gray-400 truncate max-w-[140px]" title={log.userAgent}>
                        {log.userAgent || 'Unknown'}
                      </div>
                    </Td>
                    <Td className="whitespace-nowrap">
                      <div className="flex flex-col gap-0.5">
                        <Badge tone={log.status === 'SUCCESS' ? 'green' : 'red'}>
                          {log.status === 'SUCCESS' ? 'সফল' : 'ব্যর্থ'}
                        </Badge>
                        {log.executionTimeMs > 0 && (
                          <span className="text-[10px] text-gray-400 font-mono">
                            {log.executionTimeMs} ms
                          </span>
                        )}
                      </div>
                    </Td>
                    <Td className="max-w-xs text-xs text-gray-700">
                      <span className="break-words font-sans">{log.details ?? '—'}</span>
                    </Td>
                  </tr>
                ))}
              </Table>
              {data && (
                <Pagination
                  page={data.page}
                  totalPages={data.totalPages}
                  totalElements={data.totalElements}
                  unit="টি লগ"
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
