import { useState } from 'react'
import { Pagination } from '../components/extra.jsx'
import { Card, EmptyState, LoadState, PageHeader, SelectField, Table, TextField, Td } from '../components/ui.jsx'
import { useQuery } from '../hooks/useQuery.js'
import { actionLabel, ENTITY_TYPE_OPTIONS } from '../utils/audit.js'
import { formatDateTime } from '../utils/format.js'

export default function AuditLogsPage() {
  const [filters, setFilters] = useState({ entityType: '', entityId: '' })
  const [page, setPage] = useState(0)

  function update(patch) {
    setFilters((current) => ({ ...current, ...patch }))
    setPage(0)
  }

  const params = new URLSearchParams({ page: String(page), size: '50' })
  if (filters.entityType) params.set('entityType', filters.entityType)
  if (filters.entityId) params.set('entityId', filters.entityId)
  const { data, error, loading } = useQuery(`/api/admin/audit-logs?${params}`)

  return (
    <>
      <PageHeader title="অডিট লগ" subtitle="কে, কখন, কী করেছে, সবচেয়ে নতুন আগে" />

      <Card className="mb-4 p-4">
        <div className="grid gap-3 sm:grid-cols-2">
          <SelectField
            label="বিষয়ের ধরন"
            options={ENTITY_TYPE_OPTIONS}
            value={filters.entityType}
            onChange={(event) => update({ entityType: event.target.value })}
          />
          <TextField
            label="বিষয়ের নম্বর (ঐচ্ছিক)"
            type="number"
            min="1"
            hint="যেমন: শিক্ষার্থীর নম্বর। ওপরে ধরন বেছে নিলে ভালো কাজ করে"
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
              <Table headers={['সময়', 'কে', 'কাজ', 'বিষয়', 'বিবরণ']}>
                {data?.items.map((log) => (
                  <tr key={log.id}>
                    <Td className="whitespace-nowrap text-xs">{formatDateTime(log.createdAt)}</Td>
                    <Td className="font-mono text-xs">{log.actorUsername ?? '—'}</Td>
                    <Td>
                      <p className="font-medium text-gray-900">{actionLabel(log.action)}</p>
                      <p className="font-mono text-xs text-gray-400">{log.action}</p>
                    </Td>
                    <Td className="whitespace-nowrap text-xs">
                      {log.entityType}
                      {log.entityId ? ` #${log.entityId}` : ''}
                    </Td>
                    <Td className="max-w-md text-xs">{log.details ?? '—'}</Td>
                  </tr>
                ))}
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