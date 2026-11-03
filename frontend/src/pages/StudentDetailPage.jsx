import { useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { api, apiUpload } from '../api/client.js'
import EntityForm from '../components/EntityForm.jsx'
import { ConfirmModal, InfoRow } from '../components/extra.jsx'
import {
  EnrollModal,
  LinkGuardianModal,
  StatusModal,
  TransferModal,
  WithdrawModal,
} from '../components/StudentModals.jsx'
import StudentPhoto from '../components/StudentPhoto.jsx'
import StudentResultsCard from '../components/StudentResultsCard.jsx'
import { Alert, Badge, Button, Card, EmptyState, LoadState, Modal, PageHeader, Table, Td } from '../components/ui.jsx'
import { useQuery } from '../hooks/useQuery.js'
import {
  blankToNull,
  enrollmentStatusInfo,
  formatDate,
  formatDateOrDash,
  genderLabel,
  relationLabel,
  studentStatusInfo,
} from '../utils/format.js'
import { shrinkImage } from '../utils/image.js'
import { STUDENT_FIELDS } from '../utils/studentFields.js'

// ---------- ছবির অংশ ----------
function PhotoCard({ student }) {
  const inputRef = useRef(null)
  const [version, setVersion] = useState(0)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  async function onPick(event) {
    const file = event.target.files?.[0]
    event.target.value = '' // একই ছবি আবার বাছতে চাইলে যেন কাজ করে
    if (!file) return
    setBusy(true)
    setError('')
    try {
      const blob = await shrinkImage(file)
      const form = new FormData()
      form.append('file', blob, 'photo.jpg')
      await apiUpload(`/api/students/${student.id}/photo`, form)
      setVersion((v) => v + 1)
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  async function remove() {
    setBusy(true)
    setError('')
    try {
      await api(`/api/students/${student.id}/photo`, { method: 'DELETE' })
      setVersion((v) => v + 1)
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="flex flex-col items-center gap-3">
      <StudentPhoto key={version} studentId={student.id} name={student.fullName} />
      <input
        ref={inputRef}
        type="file"
        accept="image/jpeg,image/png"
        className="hidden"
        onChange={onPick}
      />
      <div className="flex gap-2">
        <Button variant="secondary" size="sm" disabled={busy} onClick={() => inputRef.current.click()}>
          {busy ? 'অপেক্ষা করুন...' : 'ছবি দিন / বদলান'}
        </Button>
        <Button variant="ghost" size="sm" disabled={busy} onClick={remove}>
          সরান
        </Button>
      </div>
      {error && <Alert>{error}</Alert>}
    </div>
  )
}

export default function StudentDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { data, error, loading, reload } = useQuery(`/api/students/${id}`)
  // modal: null অথবা { type, ...তথ্য }
  const [modal, setModal] = useState(null)
  const [actionError, setActionError] = useState('')

  function close() {
    setModal(null)
  }

  function done() {
    setModal(null)
    reload()
  }

  async function run(action) {
    setActionError('')
    try {
      await action()
      reload()
    } catch (err) {
      setActionError(err.message)
    }
  }

  async function saveEdit(values) {
    await api(`/api/students/${id}`, { method: 'PUT', body: blankToNull(values) })
    done()
  }

  async function deleteStudent() {
    await api(`/api/students/${id}`, { method: 'DELETE' })
    navigate('/students')
  }

  async function unlink() {
    await api(`/api/students/${id}/guardians/${modal.link.guardianId}`, { method: 'DELETE' })
    done()
  }

  const student = data?.student
  const status = student ? studentStatusInfo(student.status) : null

  return (
    <>
      <div className="mb-3">
        <Link to="/students" className="text-sm text-gray-500 hover:text-gray-700">
          ← সব শিক্ষার্থী
        </Link>
      </div>

      <LoadState loading={loading} error={error}>
        {data && (
          <>
            <PageHeader
              title={student.fullName}
              subtitle={`আইডি: ${student.studentCode}`}
              action={
                <div className="flex flex-wrap gap-2">
                  <Button variant="secondary" onClick={() => setModal({ type: 'edit' })}>
                    সম্পাদনা
                  </Button>
                  <Button variant="secondary" onClick={() => setModal({ type: 'status' })}>
                    অবস্থা বদলান
                  </Button>
                  <Button variant="danger" onClick={() => setModal({ type: 'delete' })}>
                    মুছুন
                  </Button>
                </div>
              }
            />

            {actionError && (
              <div className="mb-4">
                <Alert>{actionError}</Alert>
              </div>
            )}

            <div className="space-y-6">
              {/* ---------- মূল তথ্য ---------- */}
              <Card className="p-5">
                <div className="flex flex-col gap-6 sm:flex-row">
                  <PhotoCard student={student} />
                  <dl className="flex-1 divide-y divide-gray-100">
                    <InfoRow label="অবস্থা">
                      <Badge tone={status.tone}>{status.label}</Badge>
                    </InfoRow>
                    <InfoRow label="জন্মতারিখ">{formatDate(student.dateOfBirth)}</InfoRow>
                    <InfoRow label="লিঙ্গ">{genderLabel(student.gender)}</InfoRow>
                    <InfoRow label="ফোন">{student.phone ?? '—'}</InfoRow>
                    <InfoRow label="ঠিকানা">{student.address ?? '—'}</InfoRow>
                    <InfoRow label="ভর্তির তারিখ">{formatDateOrDash(student.admissionDate)}</InfoRow>
                  </dl>
                </div>
              </Card>

              {/* ---------- অভিভাবক ---------- */}
              <section>
                <div className="mb-3 flex items-center justify-between">
                  <h2 className="text-lg font-semibold text-gray-900">অভিভাবক</h2>
                  <Button size="sm" onClick={() => setModal({ type: 'link' })}>
                    অভিভাবক যোগ করুন
                  </Button>
                </div>
                <Card>
                  {data.guardians.length === 0 ? (
                    <EmptyState>কোনো অভিভাবক যুক্ত নেই।</EmptyState>
                  ) : (
                    <Table headers={['নাম', 'ফোন', 'সম্পর্ক', '', 'কাজ']}>
                      {data.guardians.map((link) => (
                        <tr key={link.guardianId}>
                          <Td className="font-medium text-gray-900">{link.fullName}</Td>
                          <Td className="font-mono">{link.phone}</Td>
                          <Td>{relationLabel(link.relation)}</Td>
                          <Td>{link.primary && <Badge tone="green">প্রধান</Badge>}</Td>
                          <Td>
                            <div className="flex flex-wrap gap-2">
                              {!link.primary && (
                                <Button
                                  variant="secondary"
                                  size="sm"
                                  onClick={() =>
                                    run(() =>
                                      api(`/api/students/${id}/guardians/${link.guardianId}/primary`, {
                                        method: 'POST',
                                      }),
                                    )
                                  }
                                >
                                  প্রধান করুন
                                </Button>
                              )}
                              <Button variant="ghost" size="sm" onClick={() => setModal({ type: 'unlink', link })}>
                                সরান
                              </Button>
                            </div>
                          </Td>
                        </tr>
                      ))}
                    </Table>
                  )}
                </Card>
              </section>

              {/* ---------- ভর্তি ---------- */}
              <section>
                <div className="mb-3 flex items-center justify-between">
                  <h2 className="text-lg font-semibold text-gray-900">ভর্তির ইতিহাস</h2>
                  <Button size="sm" onClick={() => setModal({ type: 'enroll' })}>
                    নতুন ভর্তি
                  </Button>
                </div>
                <Card>
                  {data.enrollments.length === 0 ? (
                    <EmptyState>এখনো কোনো ক্লাসে ভর্তি করানো হয়নি।</EmptyState>
                  ) : (
                    <Table headers={['সেশন', 'ক্লাস / সেকশন', 'রোল', 'অবস্থা', 'সময়কাল', 'কাজ']}>
                      {data.enrollments.map((e) => {
                        const info = enrollmentStatusInfo(e.status)
                        return (
                          <tr key={e.id}>
                            <Td className="font-medium text-gray-900">{e.sessionName}</Td>
                            <Td>
                              {e.className} / {e.sectionName}
                            </Td>
                            <Td>{e.rollNumber}</Td>
                            <Td>
                              <Badge tone={info.tone}>{info.label}</Badge>
                            </Td>
                            <Td className="text-xs">
                              {formatDate(e.startDate)}
                              {e.endDate ? ` থেকে ${formatDate(e.endDate)}` : ' থেকে চলছে'}
                            </Td>
                            <Td>
                              {e.status === 'ACTIVE' && (
                                <div className="flex flex-wrap gap-2">
                                  <Button
                                    variant="secondary"
                                    size="sm"
                                    onClick={() => setModal({ type: 'transfer', enrollment: e })}
                                  >
                                    বদলি
                                  </Button>
                                  <Button
                                    variant="ghost"
                                    size="sm"
                                    onClick={() => setModal({ type: 'withdraw', enrollment: e })}
                                  >
                                    ছাড়পত্র
                                  </Button>
                                </div>
                              )}
                            </Td>
                          </tr>
                        )
                      })}
                    </Table>
                  )}
                </Card>
              </section>
            </div>


            <div className="mt-6">
              <StudentResultsCard studentId={student.id} />
            </div>

            {/* ---------- জানালাগুলো ---------- */}
            {modal?.type === 'edit' && (
              <Modal title="শিক্ষার্থীর তথ্য সম্পাদনা" onClose={close}>
                <EntityForm
                  fields={STUDENT_FIELDS}
                  initial={student}
                  submitLabel="সংরক্ষণ করুন"
                  onSubmit={saveEdit}
                  onCancel={close}
                />
              </Modal>
            )}
            {modal?.type === 'status' && <StatusModal student={student} onDone={done} onClose={close} />}
            {modal?.type === 'delete' && (
              <ConfirmModal
                title="শিক্ষার্থী মুছুন"
                message={`${student.fullName} (${student.studentCode}) কে মুছবেন? পরে "মোছা শিক্ষার্থী" পাতা থেকে ফিরিয়ে আনা যাবে। চলমান ভর্তি থাকলে আগে ছাড়পত্র দিতে হবে।`}
                confirmLabel="মুছুন"
                danger
                onConfirm={deleteStudent}
                onClose={close}
              />
            )}
            {modal?.type === 'link' && <LinkGuardianModal studentId={student.id} onDone={done} onClose={close} />}
            {modal?.type === 'unlink' && (
              <ConfirmModal
                title="অভিভাবক সরান"
                message={`${modal.link.fullName} কে এই শিক্ষার্থীর অভিভাবকের তালিকা থেকে সরাবেন? (শেষ অভিভাবক বা প্রধান অভিভাবককে সরানো যায় না)`}
                confirmLabel="সরান"
                danger
                onConfirm={unlink}
                onClose={close}
              />
            )}
            {modal?.type === 'enroll' && <EnrollModal studentId={student.id} onDone={done} onClose={close} />}
            {modal?.type === 'transfer' && (
              <TransferModal enrollment={modal.enrollment} onDone={done} onClose={close} />
            )}
            {modal?.type === 'withdraw' && (
              <WithdrawModal enrollment={modal.enrollment} onDone={done} onClose={close} />
            )}
          </>
        )}
      </LoadState>
    </>
  )
}