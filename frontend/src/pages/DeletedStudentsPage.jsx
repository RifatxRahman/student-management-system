import { useState } from 'react'
import { Link } from 'react-router'
import { api } from '../api/client.js'
import { ConfirmModal } from '../components/extra.jsx'
import { Button, Card, EmptyState, LoadState, PageHeader, Table, Td } from '../components/ui.jsx'
import { useQuery } from '../hooks/useQuery.js'
import { genderLabel } from '../utils/format.js'

export default function DeletedStudentsPage() {
  const { data, error, loading, reload } = useQuery('/api/students/deleted')
  const [target, setTarget] = useState(null)

  async function restore() {
    await api(`/api/students/${target.id}/restore`, { method: 'POST' })
    setTarget(null)
    reload()
  }

  return (
    <>
      <div className="mb-3">
        <Link to="/students" className="text-sm text-gray-500 hover:text-gray-700">
          ← সব শিক্ষার্থী
        </Link>
      </div>

      <PageHeader title="মোছা শিক্ষার্থী" subtitle="এখান থেকে আবার তালিকায় ফিরিয়ে আনা যায়" />

      <Card>
        <LoadState loading={loading} error={error}>
          {data?.length === 0 ? (
            <EmptyState>কোনো মোছা শিক্ষার্থী নেই।</EmptyState>
          ) : (
            <Table headers={['আইডি', 'নাম', 'লিঙ্গ', 'কাজ']}>
              {data?.map((s) => (
                <tr key={s.id}>
                  <Td className="font-mono text-xs">{s.studentCode}</Td>
                  <Td className="font-medium text-gray-900">{s.fullName}</Td>
                  <Td>{genderLabel(s.gender)}</Td>
                  <Td>
                    <Button variant="secondary" size="sm" onClick={() => setTarget(s)}>
                      ফিরিয়ে আনুন
                    </Button>
                  </Td>
                </tr>
              ))}
            </Table>
          )}
        </LoadState>
      </Card>

      {target && (
        <ConfirmModal
          title="শিক্ষার্থী ফিরিয়ে আনুন"
          message={`${target.fullName} (${target.studentCode}) কে আবার শিক্ষার্থীর তালিকায় ফিরিয়ে আনবেন?`}
          confirmLabel="ফিরিয়ে আনুন"
          onConfirm={restore}
          onClose={() => setTarget(null)}
        />
      )}
    </>
  )
}