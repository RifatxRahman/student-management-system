import { useState } from 'react'
import { api } from '../api/client.js'
import { useAuth } from '../auth/useAuth.js'
import EntityForm from '../components/EntityForm.jsx'
import { Alert, Badge, Button, Card, EmptyState, LoadState, Modal, PageHeader, Table, Td } from '../components/ui.jsx'
import { useFetch } from '../hooks/useFetch.js'
import { roleLabel } from '../utils/format.js'

const FIELDS = [
  {
    name: 'username',
    label: 'ইউজারনেম',
    hint: 'ইংরেজি অক্ষর, সংখ্যা, ডট, আন্ডারস্কোর বা হাইফেন (কমপক্ষে ৩ অক্ষর)',
    required: true,
  },
  { name: 'fullName', label: 'পুরো নাম', required: true },
  {
    name: 'role',
    label: 'ভূমিকা',
    defaultValue: 'TEACHER',
    required: true,
    options: [
      { value: 'TEACHER', label: 'শিক্ষক' },
      { value: 'ADMIN', label: 'অ্যাডমিন' },
    ],
  },
]

function PasswordDialog({ title, username, password, onClose }) {
  const [copied, setCopied] = useState(false)

  async function copy() {
    try {
      await navigator.clipboard.writeText(password)
      setCopied(true)
    } catch {
      setCopied(false)
    }
  }

  return (
    <Modal title={title} onClose={onClose} dismissible={false}>
      <div className="space-y-4">
        <Alert type="info">
          এই পাসওয়ার্ড শুধু এখন একবারই দেখা যাবে। ব্যবহারকারীকে নিরাপদে জানান। সে প্রথম লগইনেই এটা বদলে নেবে।
        </Alert>
        <div>
          <p className="text-sm text-gray-500">
            ইউজারনেম: <span className="font-medium text-gray-900">{username}</span>
          </p>
          <p className="mt-2 select-all rounded-lg bg-gray-100 px-4 py-3 text-center font-mono text-lg tracking-wider">
            {password}
          </p>
        </div>
        <div className="flex justify-end gap-2">
          <Button variant="secondary" onClick={copy}>
            {copied ? 'কপি হয়েছে ✓' : 'কপি করুন'}
          </Button>
          <Button onClick={onClose}>বন্ধ করুন</Button>
        </div>
      </div>
    </Modal>
  )
}

export default function UsersPage() {
  const { user: me } = useAuth()
  const { data, error, loading, reload } = useFetch('/api/admin/users')
  const [showCreate, setShowCreate] = useState(false)
  const [passwordInfo, setPasswordInfo] = useState(null)
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

  async function createUser(values) {
    const result = await api('/api/admin/users', { method: 'POST', body: values })
    setShowCreate(false)
    setPasswordInfo({
      title: 'অ্যাকাউন্ট তৈরি হয়েছে',
      username: result.user.username,
      password: result.temporaryPassword,
    })
    reload()
  }

  function resetPassword(target) {
    if (!window.confirm(`${target.username}-এর পাসওয়ার্ড রিসেট করবেন? তার সব লগইন বন্ধ হয়ে যাবে।`)) return
    run(async () => {
      const result = await api(`/api/admin/users/${target.id}/reset-password`, { method: 'POST' })
      setPasswordInfo({
        title: 'নতুন অস্থায়ী পাসওয়ার্ড',
        username: target.username,
        password: result.temporaryPassword,
      })
    })
  }

  function deactivate(target) {
    if (!window.confirm(`${target.username}-কে নিষ্ক্রিয় করবেন? সে সাথে সাথে বাইরে চলে যাবে।`)) return
    run(() => api(`/api/admin/users/${target.id}/deactivate`, { method: 'POST' }))
  }

  return (
    <>
      <PageHeader
        title="ইউজার"
        subtitle="শিক্ষক ও অ্যাডমিনদের অ্যাকাউন্ট"
        action={<Button onClick={() => setShowCreate(true)}>নতুন ইউজার</Button>}
      />

      {actionError && (
        <div className="mb-4">
          <Alert>{actionError}</Alert>
        </div>
      )}

      <Card>
        <LoadState loading={loading} error={error}>
          {data?.length === 0 ? (
            <EmptyState>কোনো ইউজার নেই।</EmptyState>
          ) : (
            <Table headers={['নাম', 'ইউজারনেম', 'ভূমিকা', 'অবস্থা', 'কাজ']}>
              {data?.map((u) => (
                <tr key={u.id}>
                  <Td className="font-medium text-gray-900">{u.fullName}</Td>
                  <Td className="font-mono">{u.username}</Td>
                  <Td>
                    <Badge tone="indigo">{roleLabel(u.role)}</Badge>
                  </Td>
                  <Td>
                    <div className="flex flex-wrap gap-1">
                      <Badge tone={u.status === 'ACTIVE' ? 'green' : 'gray'}>
                        {u.status === 'ACTIVE' ? 'সক্রিয়' : 'নিষ্ক্রিয়'}
                      </Badge>
                      {u.mustChangePassword && <Badge tone="amber">প্রথম লগইন বাকি</Badge>}
                    </div>
                  </Td>
                  <Td>
                    <div className="flex flex-wrap gap-2">
                      <Button variant="secondary" size="sm" onClick={() => resetPassword(u)}>
                        পাসওয়ার্ড রিসেট
                      </Button>
                      {u.status === 'ACTIVE' ? (
                        <Button variant="ghost" size="sm" disabled={u.id === me.id} onClick={() => deactivate(u)}>
                          নিষ্ক্রিয় করুন
                        </Button>
                      ) : (
                        <Button
                          size="sm"
                          onClick={() => run(() => api(`/api/admin/users/${u.id}/activate`, { method: 'POST' }))}
                        >
                          সক্রিয় করুন
                        </Button>
                      )}
                    </div>
                  </Td>
                </tr>
              ))}
            </Table>
          )}
        </LoadState>
      </Card>

      {showCreate && (
        <Modal title="নতুন ইউজার" onClose={() => setShowCreate(false)}>
          <EntityForm
            fields={FIELDS}
            submitLabel="তৈরি করুন"
            onSubmit={createUser}
            onCancel={() => setShowCreate(false)}
          />
        </Modal>
      )}

      {passwordInfo && (
        <PasswordDialog
          title={passwordInfo.title}
          username={passwordInfo.username}
          password={passwordInfo.password}
          onClose={() => setPasswordInfo(null)}
        />
      )}
    </>
  )
}