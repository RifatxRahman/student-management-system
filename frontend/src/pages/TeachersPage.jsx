import { useState } from 'react'
import { useNavigate } from 'react-router'
import { api } from '../api/client.js'
import EntityForm from '../components/EntityForm.jsx'
import { Alert, Badge, Button, Card, EmptyState, LoadState, Modal, PageHeader, Table, Td } from '../components/ui.jsx'
import { useQuery } from '../hooks/useQuery.js'

export default function TeachersPage() {
  const navigate = useNavigate()
  const teachers = useQuery('/api/teachers')
  const users = useQuery('/api/admin/users')
  const [modal, setModal] = useState(null) // { teacher? }
  const [actionError, setActionError] = useState('')

  async function run(action) {
    setActionError('')
    try {
      await action()
      teachers.reload()
    } catch (err) {
      setActionError(err.message)
    }
  }

  // যে সক্রিয় শিক্ষক-ইউজার এখনো কারো সাথে যুক্ত নয় (সম্পাদনার সময় নিজের ইউজারটাও থাকে)
  function buildFields(current) {
    const linked = new Set((teachers.data ?? []).map((t) => t.userId).filter(Boolean))
    const free = (users.data ?? []).filter(
      (u) => u.role === 'TEACHER' && u.status === 'ACTIVE' && (!linked.has(u.id) || u.id === current?.userId),
    )
    return [
      { name: 'fullName', label: 'পুরো নাম', required: true },
      { name: 'phone', label: 'ফোন নম্বর (ঐচ্ছিক)', hint: 'যেমন: 01712345678' },
      { name: 'designation', label: 'পদবি (ঐচ্ছিক)', hint: 'যেমন: সহকারী শিক্ষক' },
      {
        name: 'userId',
        label: 'লগইন ইউজার (ঐচ্ছিক)',
        options: [
          { value: '', label: 'লগইন যুক্ত নেই' },
          ...free.map((u) => ({ value: String(u.id), label: `${u.fullName} (${u.username})` })),
        ],
      },
    ]
  }

  async function save(values) {
    const body = {
      fullName: values.fullName,
      phone: values.phone || null,
      designation: values.designation || null,
      userId: values.userId ? Number(values.userId) : null,
    }
    if (modal.teacher) {
      await api(`/api/teachers/${modal.teacher.id}`, { method: 'PUT', body })
    } else {
      await api('/api/teachers', { method: 'POST', body })
    }
    setModal(null)
    teachers.reload()
  }

  return (
    <>
      <PageHeader
        title="শিক্ষক"
        subtitle="হাজিরা নিতে শিক্ষকের একটা লগইন ইউজার যুক্ত থাকতে হবে"
        action={<Button onClick={() => setModal({})}>নতুন শিক্ষক</Button>}
      />

      {actionError && (
        <div className="mb-4">
          <Alert>{actionError}</Alert>
        </div>
      )}

      <Card>
        <LoadState loading={teachers.loading} error={teachers.error}>
          {teachers.data?.length === 0 ? (
            <EmptyState>এখনো কোনো শিক্ষক নেই।</EmptyState>
          ) : (
            <Table headers={['নাম', 'ফোন', 'পদবি', 'লগইন', 'অবস্থা', 'কাজ']}>
              {teachers.data?.map((t) => (
                <tr key={t.id}>
                  <Td className="font-medium text-gray-900">{t.fullName}</Td>
                  <Td className="font-mono">{t.phone ?? '—'}</Td>
                  <Td>{t.designation ?? '—'}</Td>
                  <Td className="font-mono">{t.username ?? '—'}</Td>
                  <Td>
                    <Badge tone={t.active ? 'green' : 'gray'}>{t.active ? 'সক্রিয়' : 'নিষ্ক্রিয়'}</Badge>
                  </Td>
                  <Td>
                    <div className="flex flex-wrap gap-2">
                      <Button size="sm" onClick={() => navigate(`/teachers/${t.id}`)}>
                        বরাদ্দ
                      </Button>
                      <Button variant="secondary" size="sm" onClick={() => setModal({ teacher: t })}>
                        সম্পাদনা
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() =>
                          run(() => api(`/api/teachers/${t.id}/${t.active ? 'deactivate' : 'activate'}`, { method: 'POST' }))
                        }
                      >
                        {t.active ? 'নিষ্ক্রিয় করুন' : 'সক্রিয় করুন'}
                      </Button>
                    </div>
                  </Td>
                </tr>
              ))}
            </Table>
          )}
        </LoadState>
      </Card>

      {modal && (
        <Modal title={modal.teacher ? 'শিক্ষক সম্পাদনা' : 'নতুন শিক্ষক'} onClose={() => setModal(null)}>
          <EntityForm
            fields={buildFields(modal.teacher)}
            initial={modal.teacher ? { ...modal.teacher, userId: modal.teacher.userId ? String(modal.teacher.userId) : '' } : undefined}
            submitLabel="সংরক্ষণ করুন"
            onSubmit={save}
            onCancel={() => setModal(null)}
          />
        </Modal>
      )}
    </>
  )
}