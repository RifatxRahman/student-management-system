import { useState } from 'react'
import { Link } from 'react-router'
import { api } from '../api/client.js'
import { useAuth } from '../auth/useAuth.js'
import EntityForm from '../components/EntityForm.jsx'
import { Alert, Badge, Button, Card, EmptyState, LoadState, Modal, PageHeader, Table, Td } from '../components/ui.jsx'
import { useFetch } from '../hooks/useFetch.js'

const FIELDS = [
  { name: 'name', label: 'ক্লাসের নাম', hint: 'যেমন: Class 6', required: true },
  { name: 'displayOrder', label: 'ক্রম', type: 'number', hint: 'ছোট সংখ্যা আগে দেখাবে', defaultValue: 0, required: true },
]

export default function ClassesPage() {
  const { user } = useAuth()
  const isAdmin = user.role === 'ADMIN'
  const { data, error, loading, reload } = useFetch('/api/classes')
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
    const body = { name: values.name, displayOrder: Number(values.displayOrder) }
    const schoolClass = modal.schoolClass
    if (schoolClass) {
      await api(`/api/classes/${schoolClass.id}`, { method: 'PUT', body })
    } else {
      await api('/api/classes', { method: 'POST', body })
    }
    setModal(null)
    reload()
  }

  return (
    <>
      <PageHeader
        title="ক্লাস ও সেকশন"
        subtitle="ক্লাসের নামে ক্লিক করে সেকশন ও বিষয় দেখুন"
        action={isAdmin && <Button onClick={() => setModal({})}>নতুন ক্লাস</Button>}
      />

      {actionError && (
        <div className="mb-4">
          <Alert>{actionError}</Alert>
        </div>
      )}

      <Card>
        <LoadState loading={loading} error={error}>
          {data?.length === 0 ? (
            <EmptyState>এখনো কোনো ক্লাস নেই।</EmptyState>
          ) : (
            <Table headers={['ক্রম', 'নাম', 'অবস্থা', isAdmin ? 'কাজ' : '']}>
              {data?.map((schoolClass) => (
                <tr key={schoolClass.id}>
                  <Td>{schoolClass.displayOrder}</Td>
                  <Td>
                    <Link
                      to={`/classes/${schoolClass.id}`}
                      className="font-medium text-indigo-600 hover:text-indigo-700"
                    >
                      {schoolClass.name}
                    </Link>
                  </Td>
                  <Td>
                    <Badge tone={schoolClass.active ? 'green' : 'gray'}>
                      {schoolClass.active ? 'সক্রিয়' : 'নিষ্ক্রিয়'}
                    </Badge>
                  </Td>
                  <Td>
                    {isAdmin && (
                      <div className="flex flex-wrap gap-2">
                        <Button variant="secondary" size="sm" onClick={() => setModal({ schoolClass })}>
                          সম্পাদনা
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() =>
                            run(() =>
                              api(`/api/classes/${schoolClass.id}/${schoolClass.active ? 'deactivate' : 'activate'}`, {
                                method: 'POST',
                              }),
                            )
                          }
                        >
                          {schoolClass.active ? 'নিষ্ক্রিয় করুন' : 'সক্রিয় করুন'}
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
        <Modal title={modal.schoolClass ? 'ক্লাস সম্পাদনা' : 'নতুন ক্লাস'} onClose={() => setModal(null)}>
          <EntityForm
            fields={FIELDS}
            initial={modal.schoolClass}
            submitLabel="সংরক্ষণ করুন"
            onSubmit={save}
            onCancel={() => setModal(null)}
          />
        </Modal>
      )}
    </>
  )
}