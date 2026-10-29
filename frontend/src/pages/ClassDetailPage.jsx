import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { api } from '../api/client.js'
import { useAuth } from '../auth/useAuth.js'
import EntityForm from '../components/EntityForm.jsx'
import { Alert, Badge, Button, Card, EmptyState, LoadState, Modal, PageHeader, Table, Td } from '../components/ui.jsx'
import { useFetch } from '../hooks/useFetch.js'

const SECTION_FIELDS = [{ name: 'name', label: 'সেকশনের নাম', hint: 'যেমন: A', required: true }]

export default function ClassDetailPage() {
  const { id } = useParams()
  const { user } = useAuth()
  const isAdmin = user.role === 'ADMIN'

  const classes = useFetch('/api/classes')
  const sections = useFetch(`/api/classes/${id}/sections`)
  const assigned = useFetch(`/api/classes/${id}/subjects`)
  const allSubjects = useFetch('/api/subjects')

  const [modal, setModal] = useState(null) // { type: 'section', section? } অথবা { type: 'subject' }
  const [actionError, setActionError] = useState('')

  const schoolClass = classes.data?.find((c) => String(c.id) === id)

  async function run(action, ...reloaders) {
    setActionError('')
    try {
      await action()
      reloaders.forEach((reload) => reload())
    } catch (err) {
      setActionError(err.message)
    }
  }

  async function saveSection(values) {
    const section = modal.section
    if (section) {
      await api(`/api/sections/${section.id}`, { method: 'PUT', body: { name: values.name } })
    } else {
      await api(`/api/classes/${id}/sections`, { method: 'POST', body: { name: values.name } })
    }
    setModal(null)
    sections.reload()
  }

  async function assignSubject(values) {
    await api(`/api/classes/${id}/subjects`, { method: 'POST', body: { subjectId: Number(values.subjectId) } })
    setModal(null)
    assigned.reload()
  }

  const availableSubjects =
    allSubjects.data?.filter((s) => s.active && !assigned.data?.some((a) => a.id === s.id)) ?? []

  return (
    <>
      <div className="mb-3">
        <Link to="/classes" className="text-sm text-gray-500 hover:text-gray-700">
          ← সব ক্লাস
        </Link>
      </div>

      {classes.data && !schoolClass ? (
        <Alert>এই ক্লাসটি পাওয়া যায়নি।</Alert>
      ) : (
        <PageHeader
          title={schoolClass ? schoolClass.name : '...'}
          subtitle={schoolClass && !schoolClass.active ? 'এই ক্লাসটি এখন নিষ্ক্রিয়' : undefined}
        />
      )}

      {actionError && (
        <div className="mb-4">
          <Alert>{actionError}</Alert>
        </div>
      )}

      <div className="space-y-8">
        {/* ---------- সেকশন ---------- */}
        <section>
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-lg font-semibold text-gray-900">সেকশন</h2>
            {isAdmin && schoolClass?.active && (
              <Button size="sm" onClick={() => setModal({ type: 'section' })}>
                নতুন সেকশন
              </Button>
            )}
          </div>
          <Card>
            <LoadState loading={sections.loading} error={sections.error}>
              {sections.data?.length === 0 ? (
                <EmptyState>এই ক্লাসে এখনো কোনো সেকশন নেই।</EmptyState>
              ) : (
                <Table headers={['নাম', 'অবস্থা', isAdmin ? 'কাজ' : '']}>
                  {sections.data?.map((section) => (
                    <tr key={section.id}>
                      <Td className="font-medium text-gray-900">{section.name}</Td>
                      <Td>
                        <Badge tone={section.active ? 'green' : 'gray'}>
                          {section.active ? 'সক্রিয়' : 'নিষ্ক্রিয়'}
                        </Badge>
                      </Td>
                      <Td>
                        {isAdmin && (
                          <div className="flex flex-wrap gap-2">
                            <Button
                              variant="secondary"
                              size="sm"
                              onClick={() => setModal({ type: 'section', section })}
                            >
                              সম্পাদনা
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() =>
                                run(
                                  () =>
                                    api(`/api/sections/${section.id}/${section.active ? 'deactivate' : 'activate'}`, {
                                      method: 'POST',
                                    }),
                                  sections.reload,
                                )
                              }
                            >
                              {section.active ? 'নিষ্ক্রিয় করুন' : 'সক্রিয় করুন'}
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
        </section>

        {/* ---------- বিষয় ---------- */}
        <section>
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-lg font-semibold text-gray-900">এই ক্লাসের বিষয়</h2>
            {isAdmin && schoolClass?.active && (
              <Button size="sm" onClick={() => setModal({ type: 'subject' })}>
                বিষয় যোগ করুন
              </Button>
            )}
          </div>
          <Card>
            <LoadState loading={assigned.loading} error={assigned.error}>
              {assigned.data?.length === 0 ? (
                <EmptyState>এই ক্লাসে এখনো কোনো বিষয় যোগ করা হয়নি।</EmptyState>
              ) : (
                <Table headers={['বিষয়', 'কোড', isAdmin ? 'কাজ' : '']}>
                  {assigned.data?.map((subject) => (
                    <tr key={subject.id}>
                      <Td className="font-medium text-gray-900">{subject.name}</Td>
                      <Td className="font-mono">{subject.code}</Td>
                      <Td>
                        {isAdmin && (
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => {
                              if (window.confirm(`"${subject.name}" এই ক্লাস থেকে সরাবেন?`)) {
                                run(
                                  () => api(`/api/classes/${id}/subjects/${subject.id}`, { method: 'DELETE' }),
                                  assigned.reload,
                                )
                              }
                            }}
                          >
                            সরান
                          </Button>
                        )}
                      </Td>
                    </tr>
                  ))}
                </Table>
              )}
            </LoadState>
          </Card>
        </section>
      </div>

      {modal?.type === 'section' && (
        <Modal title={modal.section ? 'সেকশন সম্পাদনা' : 'নতুন সেকশন'} onClose={() => setModal(null)}>
          <EntityForm
            fields={SECTION_FIELDS}
            initial={modal.section}
            submitLabel="সংরক্ষণ করুন"
            onSubmit={saveSection}
            onCancel={() => setModal(null)}
          />
        </Modal>
      )}

      {modal?.type === 'subject' && (
        <Modal title="বিষয় যোগ করুন" onClose={() => setModal(null)}>
          {availableSubjects.length === 0 ? (
            <div className="space-y-4">
              <Alert type="info">যোগ করার মতো কোনো সক্রিয় বিষয় নেই। আগে "বিষয়" পাতায় নতুন বিষয় বানান।</Alert>
              <div className="flex justify-end">
                <Button variant="secondary" onClick={() => setModal(null)}>
                  বন্ধ করুন
                </Button>
              </div>
            </div>
          ) : (
            <EntityForm
              fields={[
                {
                  name: 'subjectId',
                  label: 'বিষয়',
                  required: true,
                  options: [
                    { value: '', label: 'বেছে নিন' },
                    ...availableSubjects.map((s) => ({ value: String(s.id), label: `${s.name} (${s.code})` })),
                  ],
                },
              ]}
              submitLabel="যোগ করুন"
              onSubmit={assignSubject}
              onCancel={() => setModal(null)}
            />
          )}
        </Modal>
      )}
    </>
  )
}