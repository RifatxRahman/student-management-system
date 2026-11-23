import { useState, useEffect } from 'react'
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
      { value: 'ADMIN', label: 'অ্যাডমিন' },
      { value: 'TEACHER', label: 'শিক্ষক' },
      { value: 'ACCOUNTANT', label: 'হিসাবরক্ষক' },
      { value: 'STAFF', label: 'অফিস সহকারী' },
    ],
  },
]

const MODULE_LABELS = {
  STUDENTS: 'শিক্ষার্থী ও অভিভাবক',
  ATTENDANCE: 'হাজিরা ও উপস্থিতি',
  MARKS: 'পরীক্ষা ও ফলাফল',
  FEES: 'ফি ও ইনভয়েস কালেকশন',
  EXPENSES: 'ব্যয় ও হিসাবরক্ষণ',
  NOTIFICATIONS: 'এসএমএস ও নোটিফিকেশন',
  ADMIN: 'ইউজার ও সিস্টেম প্রশাসন',
}

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

function UserPermissionsModal({ user, onClose, onUpdated }) {
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [data, setData] = useState(null)
  const [saving, setSaving] = useState(false)
  // overrides state: { [permissionId]: 'DEFAULT' | 'GRANT' | 'REVOKE' }
  const [overrides, setOverrides] = useState({})

  useEffect(() => {
    async function load() {
      try {
        setLoading(true)
        const res = await api(`/api/admin/rbac/users/${user.id}`)
        setData(res)
        const initial = {}
        for (let i = 0; i < res.permissions.length; i++) {
          const p = res.permissions[i]
          if (p.customOverride === true) {
            initial[p.permissionId] = 'GRANT'
          } else if (p.customOverride === false) {
            initial[p.permissionId] = 'REVOKE'
          } else {
            initial[p.permissionId] = 'DEFAULT'
          }
        }
        setOverrides(initial)
      } catch (err) {
        setError(err.message)
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [user.id])

  async function handleSave() {
    try {
      setSaving(true)
      setError('')
      const overrideItems = []
      const keys = Object.keys(overrides)
      for (let i = 0; i < keys.length; i++) {
        const permId = Number(keys[i])
        const state = overrides[permId]
        if (state === 'GRANT') {
          overrideItems.push({ permissionId: permId, granted: true })
        } else if (state === 'REVOKE') {
          overrideItems.push({ permissionId: permId, granted: false })
        }
      }

      await api(`/api/admin/rbac/users/${user.id}`, {
        method: 'PUT',
        body: { overrides: overrideItems },
      })
      onUpdated?.()
      onClose()
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  // গ্রুপ অনুযায়ী বিন্যাস
  const grouped = {}
  if (data?.permissions) {
    for (let i = 0; i < data.permissions.length; i++) {
      const p = data.permissions[i]
      if (!grouped[p.module]) {
        grouped[p.module] = []
      }
      grouped[p.module].push(p)
    }
  }

  return (
    <Modal title={`${user.fullName} (${user.username}) - পারমিশন নিয়ন্ত্রণ`} onClose={onClose}>
      <div className="space-y-4 max-h-[75vh] overflow-y-auto pr-1">
        {error && <Alert>{error}</Alert>}
        <div className="bg-indigo-50 border border-indigo-100 rounded-lg p-3 text-xs text-indigo-900 flex justify-between items-center">
          <div>
            মূল ভূমিকা: <span className="font-semibold">{roleLabel(user.role)}</span>
          </div>
          <div className="text-gray-500">
            রোল ডিফল্ট পারমিশনের বাইরে নির্দিষ্ট ইউজারের জন্য বিশেষ অনুমতি দিতে বা বাতিল করতে পারবেন।
          </div>
        </div>

        {loading ? (
          <div className="py-8 text-center text-gray-500 text-sm">অনুমতি ডেটা লোড হচ্ছে...</div>
        ) : (
          <div className="space-y-6">
            {Object.entries(grouped).map(([moduleName, items]) => (
              <div key={moduleName} className="border border-gray-200 rounded-lg p-3">
                <h4 className="font-semibold text-sm text-gray-800 border-b pb-2 mb-3">
                  {MODULE_LABELS[moduleName] || moduleName}
                </h4>
                <div className="space-y-3">
                  {items.map((item) => {
                    const currentVal = overrides[item.permissionId] || 'DEFAULT'
                    return (
                      <div
                        key={item.permissionId}
                        className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 p-2 rounded hover:bg-gray-50 border-b border-gray-100 last:border-0"
                      >
                        <div className="text-sm">
                          <div className="font-medium text-gray-900">{item.name}</div>
                          <div className="text-xs text-gray-500 font-mono">
                            {item.code} • রোলের ডিফল্ট:{' '}
                            <span className={item.roleDefault ? 'text-green-600 font-semibold' : 'text-gray-500'}>
                              {item.roleDefault ? 'অনুমোদিত' : 'অননুমোদিত'}
                            </span>
                          </div>
                        </div>
                        <div className="flex items-center gap-1.5 shrink-0 text-xs">
                          <button
                            type="button"
                            onClick={() =>
                              setOverrides((prev) => ({ ...prev, [item.permissionId]: 'DEFAULT' }))
                            }
                            className={`px-2.5 py-1 rounded transition ${
                              currentVal === 'DEFAULT'
                                ? 'bg-gray-700 text-white font-medium'
                                : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                            }`}
                          >
                            রোলের মতো
                          </button>
                          <button
                            type="button"
                            onClick={() =>
                              setOverrides((prev) => ({ ...prev, [item.permissionId]: 'GRANT' }))
                            }
                            className={`px-2.5 py-1 rounded transition ${
                              currentVal === 'GRANT'
                                ? 'bg-green-600 text-white font-medium'
                                : 'bg-gray-100 text-gray-600 hover:bg-green-50 hover:text-green-700'
                            }`}
                          >
                            অনুমতি দিন ✓
                          </button>
                          <button
                            type="button"
                            onClick={() =>
                              setOverrides((prev) => ({ ...prev, [item.permissionId]: 'REVOKE' }))
                            }
                            className={`px-2.5 py-1 rounded transition ${
                              currentVal === 'REVOKE'
                                ? 'bg-red-600 text-white font-medium'
                                : 'bg-gray-100 text-gray-600 hover:bg-red-50 hover:text-red-700'
                            }`}
                          >
                            বাতিল করুন ✕
                          </button>
                        </div>
                      </div>
                    )
                  })}
                </div>
              </div>
            ))}
          </div>
        )}

        <div className="flex justify-end gap-2 pt-2 border-t border-gray-100">
          <Button variant="secondary" onClick={onClose} disabled={saving}>
            বাতিল
          </Button>
          <Button onClick={handleSave} disabled={saving || loading}>
            {saving ? 'সংরক্ষণ হচ্ছে...' : 'পারমিশন আপডেট করুন'}
          </Button>
        </div>
      </div>
    </Modal>
  )
}

function RoleMatrixTab() {
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [matrix, setMatrix] = useState([])
  const [permissions, setPermissions] = useState([])
  const [selectedRole, setSelectedRole] = useState('TEACHER')
  const [saving, setSaving] = useState(false)
  const [rolePermMap, setRolePermMap] = useState({}) // { [role]: Set of permIds }

  useEffect(() => {
    async function loadData() {
      try {
        setLoading(true)
        setError('')
        const [permsRes, rolesRes] = await Promise.all([
          api('/api/admin/rbac/permissions'),
          api('/api/admin/rbac/roles'),
        ])
        setPermissions(permsRes)
        setMatrix(rolesRes)

        const map = {}
        for (let i = 0; i < rolesRes.length; i++) {
          const r = rolesRes[i]
          map[r.role] = new Set(r.assignedPermissionIds)
        }
        setRolePermMap(map)
      } catch (err) {
        setError(err.message)
      } finally {
        setLoading(false)
      }
    }
    loadData()
  }, [])

  function togglePerm(role, permId) {
    if (role === 'ADMIN') return // ADMIN has all by default
    setRolePermMap((prev) => {
      const currentSet = new Set(prev[role] || [])
      if (currentSet.has(permId)) {
        currentSet.delete(permId)
      } else {
        currentSet.add(permId)
      }
      return { ...prev, [role]: currentSet }
    })
  }

  async function handleSaveRolePermissions(role) {
    try {
      setSaving(true)
      setError('')
      setSuccess('')
      const ids = Array.from(rolePermMap[role] || [])
      const updatedMatrix = await api('/api/admin/rbac/roles', {
        method: 'PUT',
        body: { role, permissionIds: ids },
      })
      setMatrix(updatedMatrix)
      setSuccess(`${roleLabel(role)} রোলের পারমিশন সফলভাবে সংরক্ষিত হয়েছে!`)
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  // গ্রুপ অনুযায়ী পারমিশন
  const grouped = {}
  for (let i = 0; i < permissions.length; i++) {
    const p = permissions[i]
    if (!grouped[p.module]) grouped[p.module] = []
    grouped[p.module].push(p)
  }

  const roleList = [
    { code: 'TEACHER', label: 'শিক্ষক' },
    { code: 'ACCOUNTANT', label: 'হিসাবরক্ষক' },
    { code: 'STAFF', label: 'অফিস সহকারী' },
    { code: 'ADMIN', label: 'অ্যাডমিন (সর্বোচ্চ ক্ষমতা)' },
  ]

  return (
    <div className="space-y-4">
      {error && <Alert>{error}</Alert>}
      {success && <Alert type="success">{success}</Alert>}

      {/* ভূমিকা বাছার ট্যাব */}
      <div className="flex flex-wrap gap-2 border-b pb-3">
        {roleList.map((r) => (
          <button
            key={r.code}
            type="button"
            onClick={() => {
              setSelectedRole(r.code)
              setSuccess('')
            }}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition ${
              selectedRole === r.code
                ? 'bg-indigo-600 text-white shadow-sm'
                : 'bg-white text-gray-700 border border-gray-200 hover:bg-gray-50'
            }`}
          >
            {r.label}
          </button>
        ))}
      </div>

      <LoadState loading={loading} error={null}>
        <div className="space-y-6">
          <div className="flex items-center justify-between bg-gray-50 p-3 rounded-lg border border-gray-200">
            <div>
              <span className="font-semibold text-gray-900">{roleLabel(selectedRole)}</span> রোলের অনুমোদিত ক্ষমতা
              {selectedRole === 'ADMIN' && (
                <span className="text-xs text-amber-700 ml-2 font-medium">(অ্যাডমিনের সব পারমিশন অপরিবর্তনীয়ভাবে চালু থাকে)</span>
              )}
            </div>
            {selectedRole !== 'ADMIN' && (
              <Button onClick={() => handleSaveRolePermissions(selectedRole)} disabled={saving}>
                {saving ? 'সংরক্ষণ হচ্ছে...' : `${roleLabel(selectedRole)} পারমিশন সংরক্ষণ`}
              </Button>
            )}
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {Object.entries(grouped).map(([mod, perms]) => (
              <Card key={mod} className="p-4">
                <h4 className="font-semibold text-sm text-gray-900 border-b pb-2 mb-3 flex items-center justify-between">
                  <span>{MODULE_LABELS[mod] || mod}</span>
                  <span className="text-xs font-normal text-gray-500">{perms.length} টি ক্ষমতা</span>
                </h4>
                <div className="space-y-2">
                  {perms.map((p) => {
                    const isChecked = selectedRole === 'ADMIN' || (rolePermMap[selectedRole]?.has(p.id) ?? false)
                    return (
                      <label
                        key={p.id}
                        className={`flex items-start gap-2.5 p-2 rounded cursor-pointer transition ${
                          isChecked ? 'bg-indigo-50/50' : 'hover:bg-gray-50'
                        } ${selectedRole === 'ADMIN' ? 'cursor-not-allowed opacity-80' : ''}`}
                      >
                        <input
                          type="checkbox"
                          checked={isChecked}
                          disabled={selectedRole === 'ADMIN'}
                          onChange={() => togglePerm(selectedRole, p.id)}
                          className="mt-0.5 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                        />
                        <div className="text-xs">
                          <div className="font-medium text-gray-900">{p.name}</div>
                          <div className="text-gray-500 font-mono">{p.code}</div>
                        </div>
                      </label>
                    )
                  })}
                </div>
              </Card>
            ))}
          </div>
        </div>
      </LoadState>
    </div>
  )
}

export default function UsersPage() {
  const { user: me } = useAuth()
  const { data, error, loading, reload } = useFetch('/api/admin/users')
  const [activeTab, setActiveTab] = useState('users') // 'users' | 'matrix'
  const [showCreate, setShowCreate] = useState(false)
  const [passwordInfo, setPasswordInfo] = useState(null)
  const [actionError, setActionError] = useState('')
  const [selectedUserForPerms, setSelectedUserForPerms] = useState(null)

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
        title="ইউজার ও ভূমিকা নিয়ন্ত্রণ (RBAC)"
        subtitle="সিস্টেম ইউজার, রোল এবং সূক্ষ্ম পারমিশন (Fine-Grained Permissions) ব্যবস্থাপনা"
        action={
          activeTab === 'users' && (
            <Button onClick={() => setShowCreate(true)}>নতুন ইউজার</Button>
          )
        }
      />

      {actionError && (
        <div className="mb-4">
          <Alert>{actionError}</Alert>
        </div>
      )}

      {/* ট্যাব নেভিগেশন */}
      <div className="flex border-b border-gray-200 mb-6 gap-6">
        <button
          type="button"
          onClick={() => setActiveTab('users')}
          className={`pb-3 text-sm font-semibold transition border-b-2 -mb-px ${
            activeTab === 'users'
              ? 'border-indigo-600 text-indigo-600'
              : 'border-transparent text-gray-500 hover:text-gray-700'
          }`}
        >
          ইউজার তালিকা ও অ্যাকাউন্ট
        </button>
        <button
          type="button"
          onClick={() => setActiveTab('matrix')}
          className={`pb-3 text-sm font-semibold transition border-b-2 -mb-px ${
            activeTab === 'matrix'
              ? 'border-indigo-600 text-indigo-600'
              : 'border-transparent text-gray-500 hover:text-gray-700'
          }`}
        >
          রোল ও পারমিশন ম্যাট্রিক্স
        </button>
      </div>

      {activeTab === 'users' ? (
        <Card>
          <LoadState loading={loading} error={error}>
            {data?.length === 0 ? (
              <EmptyState>কোনো ইউজার নেই।</EmptyState>
            ) : (
              <Table headers={['নাম', 'ইউজারনেম', 'ভূমিকা', 'অবস্থা', 'পারমিশন', 'কাজ']}>
                {data?.map((u) => (
                  <tr key={u.id}>
                    <Td className="font-medium text-gray-900">{u.fullName}</Td>
                    <Td className="font-mono">{u.username}</Td>
                    <Td>
                      <Badge tone={u.role === 'ADMIN' ? 'red' : u.role === 'TEACHER' ? 'indigo' : 'amber'}>
                        {roleLabel(u.role)}
                      </Badge>
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
                      <Button
                        variant="secondary"
                        size="sm"
                        onClick={() => setSelectedUserForPerms(u)}
                      >
                        অনুমতি কাস্টমাইজ
                      </Button>
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
      ) : (
        <RoleMatrixTab />
      )}

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

      {selectedUserForPerms && (
        <UserPermissionsModal
          user={selectedUserForPerms}
          onClose={() => setSelectedUserForPerms(null)}
          onUpdated={reload}
        />
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
