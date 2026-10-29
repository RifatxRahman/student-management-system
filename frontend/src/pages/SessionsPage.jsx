import { useState } from 'react'
import { api } from '../api/client.js'
import { useAuth } from '../auth/useAuth.js'
import EntityForm from '../components/EntityForm.jsx'
import { Alert, Badge, Button, Card, EmptyState, LoadState, Modal, PageHeader, Table, Td } from '../components/ui.jsx'
import { useFetch } from '../hooks/useFetch.js'
import { formatDate } from '../utils/format.js'

const FIELDS = [
  { name: 'name', label: 'সেশনের নাম', hint: 'যেমন: 2026', required: true },
  { name: 'startDate', label: 'শুরুর তারিখ', type: 'date', required: true },
  { name: 'endDate', label: 'শেষ তারিখ', type: 'date', required: true },
]

export default function SessionsPage() {
  const { user } = useAuth()
  const isAdmin = user.role === 'ADMIN'
  const { data, error, loading, reload } = useFetch('/api/sessions')
  const [modal, setModal] = useState(null) // null অথবা { session? }
  const [actionError, setActionError] = useState('')

  async function run(action) {
    setActionError('')
    try {
      await action()
      reload()
    } catch (err) {
      setActionError(err.message)
    }
  }

  async function save(values) {
    const session = modal.session
    if (session) {
      await api(`/api/sessions/${session.id}`, { method: 'PUT', body: values })
    } else {
      await api('/api/sessions', { method: 'POST', body: values })
    }
    setModal(null)
    reload()
  }

  return (
    <>
      <PageHeader
        title="শিক্ষাবর্ষ (সেশন)"
        subtitle="একসাথে শুধু একটা সেশন সক্রিয় থাকে"
        action={isAdmin && <Button onClick={() => setModal({})}>নতুন সেশন</Button>}
      />

      {actionError && (
        <div className="mb-4">
          <Alert>{actionError}</Alert>
        </div>
      )}

      <Card>
        <LoadState loading={loading} error={error}>
          {data?.length === 0 ? (
            <EmptyState>এখনো কোনো সেশন নেই।</EmptyState>
          ) : (
            <Table headers={['নাম', 'শুরু', 'শেষ', 'অবস্থা', isAdmin ? 'কাজ' : '']}>
              {data?.map((session) => (
                <tr key={session.id}>
                  <Td className="font-medium text-gray-900">{session.name}</Td>
                  <Td>{formatDate(session.startDate)}</Td>
                  <Td>{formatDate(session.endDate)}</Td>
                  <Td>
                    <Badge tone={session.active ? 'green' : 'gray'}>
                      {session.active ? 'সক্রিয়' : 'নিষ্ক্রিয়'}
                    </Badge>
                  </Td>
                  <Td>
                    {isAdmin && (
                      <div className="flex flex-wrap gap-2">
                        <Button variant="secondary" size="sm" onClick={() => setModal({ session })}>
                          সম্পাদনা
                        </Button>
                        {session.active ? (
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => run(() => api(`/api/sessions/${session.id}/deactivate`, { method: 'POST' }))}
                          >
                            নিষ্ক্রিয় করুন
                          </Button>
                        ) : (
                          <Button
                            size="sm"
                            onClick={() => run(() => api(`/api/sessions/${session.id}/activate`, { method: 'POST' }))}
                          >
                            সক্রিয় করুন
                          </Button>
                        )}
                      </div>
                    )}
                  </Td>
                </tr>
              ))}
            </Table>
          )}
        </LoadState>
      </Card>

      {modal && (
        <Modal title={modal.session ? 'সেশন সম্পাদনা' : 'নতুন সেশন'} onClose={() => setModal(null)}>
          <EntityForm
            fields={FIELDS}
            initial={modal.session}
            submitLabel="সংরক্ষণ করুন"
            onSubmit={save}
            onCancel={() => setModal(null)}
          />
        </Modal>
      )}
    </>
  )
}