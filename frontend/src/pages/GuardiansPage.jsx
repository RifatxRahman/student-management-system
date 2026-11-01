import { useState } from 'react'
import { Link } from 'react-router'
import GuardianFormModal from '../components/GuardianFormModal.jsx'
import { Button, Card, EmptyState, LoadState, Modal, PageHeader, Table, TextField, Td } from '../components/ui.jsx'
import { useDebounce } from '../hooks/useDebounce.js'
import { useQuery } from '../hooks/useQuery.js'
import { relationLabel } from '../utils/format.js'

function ChildrenModal({ guardian, onClose }) {
  const { data, error, loading } = useQuery(`/api/guardians/${guardian.id}`)

  return (
    <Modal title={`${guardian.fullName}-এর সন্তান`} onClose={onClose}>
      <LoadState loading={loading} error={error}>
        {data?.children.length === 0 ? (
          <p className="text-sm text-gray-500">এখনো কোনো সন্তান যুক্ত নেই।</p>
        ) : (
          <ul className="divide-y divide-gray-100 rounded-lg border border-gray-200">
            {data?.children.map((child) => (
              <li key={child.studentId} className="flex items-center justify-between gap-3 px-3 py-2">
                <div>
                  <Link to={`/students/${child.studentId}`} className="text-sm font-medium text-gray-900 hover:text-indigo-700">
                    {child.fullName}
                  </Link>
                  <p className="text-xs text-gray-500">
                    {child.studentCode} · {relationLabel(child.relation)}
                    {child.primary ? ' · প্রধান অভিভাবক' : ''}
                  </p>
                </div>
              </li>
            ))}
          </ul>
        )}
      </LoadState>
      <div className="mt-4 flex justify-end">
        <Button variant="secondary" onClick={onClose}>
          বন্ধ করুন
        </Button>
      </div>
    </Modal>
  )
}

export default function GuardiansPage() {
  const [term, setTerm] = useState('')
  const q = useDebounce(term.trim())
  const { data, error, loading, reload } = useQuery(
    q.length >= 2 ? `/api/guardians?q=${encodeURIComponent(q)}` : null,
  )
  const [modal, setModal] = useState(null) // { type: 'form', guardian? } অথবা { type: 'children', guardian }

  function saved(guardian) {
    const wasEdit = Boolean(modal?.guardian)
    setModal(null)
    if (wasEdit) {
      reload()
    } else {
      // নতুন অভিভাবক সাথে সাথে তালিকায় দেখাতে তার ফোন দিয়ে খুঁজি
      setTerm(guardian.phone)
    }
  }

  return (
    <>
      <PageHeader
        title="অভিভাবক"
        subtitle="নাম বা ফোন নম্বর লিখে খুঁজুন"
        action={<Button onClick={() => setModal({ type: 'form' })}>নতুন অভিভাবক</Button>}
      />

      <div className="mb-4 max-w-md">
        <TextField
          label="খুঁজুন"
          hint="কমপক্ষে ২ অক্ষর লিখুন"
          value={term}
          onChange={(event) => setTerm(event.target.value)}
        />
      </div>

      <Card>
        {q.length < 2 ? (
          <EmptyState>খুঁজতে নাম বা ফোন নম্বর লিখুন।</EmptyState>
        ) : (
          <LoadState loading={loading} error={error}>
            {data?.length === 0 ? (
              <EmptyState>কাউকে পাওয়া যায়নি।</EmptyState>
            ) : (
              <Table headers={['নাম', 'ফোন', 'বিকল্প ফোন', 'ইমেইল', 'কাজ']}>
                {data?.map((g) => (
                  <tr key={g.id}>
                    <Td className="font-medium text-gray-900">{g.fullName}</Td>
                    <Td className="font-mono">{g.phone}</Td>
                    <Td className="font-mono">{g.altPhone ?? '—'}</Td>
                    <Td>{g.email ?? '—'}</Td>
                    <Td>
                      <div className="flex flex-wrap gap-2">
                        <Button variant="secondary" size="sm" onClick={() => setModal({ type: 'children', guardian: g })}>
                          সন্তান
                        </Button>
                        <Button variant="secondary" size="sm" onClick={() => setModal({ type: 'form', guardian: g })}>
                          সম্পাদনা
                        </Button>
                      </div>
                    </Td>
                  </tr>
                ))}
              </Table>
            )}
          </LoadState>
        )}
      </Card>

      {modal?.type === 'form' && (
        <GuardianFormModal guardian={modal.guardian} onSaved={saved} onClose={() => setModal(null)} />
      )}
      {modal?.type === 'children' && <ChildrenModal guardian={modal.guardian} onClose={() => setModal(null)} />}
    </>
  )
}