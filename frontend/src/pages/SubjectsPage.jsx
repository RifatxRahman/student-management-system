import { useState } from 'react'
import { api } from '../api/client.js'
import { useAuth } from '../auth/useAuth.js'
import EntityForm from '../components/EntityForm.jsx'
import { Alert, Badge, Button, Card, EmptyState, LoadState, Modal, PageHeader, Table, Td } from '../components/ui.jsx'
import { useFetch } from '../hooks/useFetch.js'

const FIELDS = [
  { name: 'name', label: 'বিষয়ের নাম', hint: 'যেমন: Mathematics', required: true },
  { name: 'code', label: 'কোড', hint: 'ইংরেজিতে, যেমন: MATH (নিজে বড় হাতের হয়ে যাবে)', required: true },
]

export default function SubjectsPage() {
  const { user } = useAuth()
  const isAdmin = user.role === 'ADMIN'
  const { data, error, loading, reload } = useFetch('/api/subjects')
  const [modal, setModal] = useState(null)
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
    const subject = modal.subject
    if (subject) {
      await api(`/api/subjects/${subject.id}`, { method: 'PUT', body: values })
    } else {
      await api('/api/subjects', { method: 'POST', body: values })
    }
    setModal(null)
    reload()
  }

  return (
    <>
      <PageHeader
        title="বিষয়"
        subtitle="প্রতিষ্ঠানের সব বিষয়ের তালিকা"
        action={isAdmin && <Button onClick={() => setModal({})}>নতুন বিষয়</Button>}
      />

      {actionError && (
        <div className="mb-4">
          <Alert>{actionError}</Alert>
        </div>
      )}

      <Card>
        <LoadState loading={loading} error={error}>
          {data?.length === 0 ? (
            <EmptyState>এখনো কোনো বিষয় নেই।</EmptyState>
          ) : (
            <Table headers={['নাম', 'কোড', 'অবস্থা', isAdmin ? 'কাজ' : '']}>
              {data?.map((subject) => (
                <tr key={subject.id}>
                  <Td className="font-medium text-gray-900">{subject.name}</Td>
                  <Td className="font-mono">{subject.code}</Td>
                  <Td>
                    <Badge tone={subject.active ? 'green' : 'gray'}>
                      {subject.active ? 'সক্রিয়' : 'নিষ্ক্রিয়'}
                    </Badge>
                  </Td>
                  <Td>
                    {isAdmin && (
                      <div className="flex flex-wrap gap-2">
                        <Button variant="secondary" size="sm" onClick={() => setModal({ subject })}>
                          সম্পাদনা
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() =>
                            run(() =>
                              api(`/api/subjects/${subject.id}/${subject.active ? 'deactivate' : 'activate'}`, {
                                method: 'POST',
                              }),
                            )
                          }
                        >
                          {subject.active ? 'নিষ্ক্রিয় করুন' : 'সক্রিয় করুন'}
                        </Button>
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
        <Modal title={modal.subject ? 'বিষয় সম্পাদনা' : 'নতুন বিষয়'} onClose={() => setModal(null)}>
          <EntityForm
            fields={FIELDS}
            initial={modal.subject}
            submitLabel="সংরক্ষণ করুন"
            onSubmit={save}
            onCancel={() => setModal(null)}
          />
        </Modal>
      )}
    </>
  )
}