import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { api } from '../api/client.js'
import { useAuth } from '../auth/useAuth.js'
import { ExamEditModal, SubjectAddModal, SubjectEditModal } from '../components/ExamModals.jsx'
import { ConfirmModal } from '../components/extra.jsx'
import { Alert, Badge, Button, Card, EmptyState, LoadState, PageHeader, Table, Td } from '../components/ui.jsx'
import { useQuery } from '../hooks/useQuery.js'
import { examStatusInfo, examTypeLabel, plain } from '../utils/format.js'

export default function ExamDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()
  const isAdmin = user.role === 'ADMIN'
  const { data, error, loading, reload } = useQuery(`/api/exams/${id}`)
  const targets = useQuery(`/api/marks/targets?examId=${id}`)
  const [modal, setModal] = useState(null) // { type, ...তথ্য }

  const exam = data?.exam
  const isDraft = exam?.status === 'DRAFT'
  const canEdit = isAdmin && isDraft

  // মার্কস এন্ট্রির তালিকা সেকশন ধরে ধরে সাজানো
  const groups = []
  const byKey = new Map()
  for (const target of targets.data ?? []) {
    if (!byKey.has(target.sectionId)) {
      const group = { key: target.sectionId, name: `${target.className} / ${target.sectionName}`, items: [] }
      byKey.set(target.sectionId, group)
      groups.push(group)
    }
    byKey.get(target.sectionId).items.push(target)
  }

  function done() {
    setModal(null)
    reload()
  }

  async function removeExam() {
    await api(`/api/exams/${id}`, { method: 'DELETE' })
    navigate('/exams')
  }

  async function removeSubject() {
    await api(`/api/exams/subjects/${modal.subject.id}`, { method: 'DELETE' })
    done()
  }

  return (
    <>
      <div className="mb-3">
        <Link to="/exams" className="text-sm text-gray-500 hover:text-gray-700">
          ← সব পরীক্ষা
        </Link>
      </div>

      <LoadState loading={loading} error={error}>
        {data && (
          <>
            <PageHeader
              title={exam.name}
              subtitle={`${exam.className} · ${exam.sessionName} · ${examTypeLabel(exam.type)}`}
              action={
                <div className="flex flex-wrap items-center gap-2">
                  <Badge tone={examStatusInfo(exam.status).tone}>{examStatusInfo(exam.status).label}</Badge>
                  {canEdit && (
                    <Button variant="secondary" onClick={() => setModal({ type: 'edit' })}>
                      সম্পাদনা
                    </Button>
                  )}
                  {canEdit && (
                    <Button variant="danger" onClick={() => setModal({ type: 'deleteExam' })}>
                      মুছুন
                    </Button>
                  )}
                  {canEdit && (
                    <Button onClick={() => navigate(`/exams/${id}/publish`)}>প্রিভিউ ও প্রকাশ</Button>
                  )}
                  {!isDraft && <Button onClick={() => navigate(`/exams/${id}/results`)}>ফলাফল দেখুন</Button>}
                </div>
              }
            />

            {!isDraft && (
              <div className="mb-4">
                <Alert type="info">
                  ফল প্রকাশিত হয়ে গেছে, তাই পরীক্ষা ও বিষয় আর বদলানো যায় না। মার্কসে ভুল থাকলে Admin কারণ লিখে সংশোধন
                  করতে পারেন (মার্কস এন্ট্রির খাতা থেকে)।
                </Alert>
              </div>
            )}

            {/* ---------- বিষয় ---------- */}
            <section className="mb-6">
              <div className="mb-3 flex items-center justify-between">
                <h2 className="text-lg font-semibold text-gray-900">
                  বিষয় <span className="text-sm font-normal text-gray-500">(আবশ্যিক বিষয়ের মোট ফুল মার্কস: {plain(data.totalFullMarks)})</span>
                </h2>
                {canEdit && (
                  <Button size="sm" onClick={() => setModal({ type: 'addSubject' })}>
                    বিষয় যোগ করুন
                  </Button>
                )}
              </div>
              <Card>
                {data.subjects.length === 0 ? (
                  <EmptyState>এখনো কোনো বিষয় যোগ হয়নি।</EmptyState>
                ) : (
                  <Table headers={['বিষয়', 'কোড', 'ফুল মার্কস', 'পাস মার্কস', 'ধরন', canEdit ? 'কাজ' : '']}>
                    {data.subjects.map((subject) => (
                      <tr key={subject.id}>
                        <Td className="font-medium text-gray-900">{subject.subjectName}</Td>
                        <Td className="font-mono text-xs">{subject.subjectCode}</Td>
                        <Td>{plain(subject.fullMarks)}</Td>
                        <Td>{plain(subject.passMarks)}</Td>
                        <Td>
                          <Badge tone={subject.optional ? 'indigo' : 'gray'}>{subject.optional ? 'ঐচ্ছিক' : 'আবশ্যিক'}</Badge>
                        </Td>
                        <Td>
                          {canEdit && (
                            <div className="flex flex-wrap gap-2">
                              <Button variant="secondary" size="sm" onClick={() => setModal({ type: 'editSubject', subject })}>
                                মার্কস বদল
                              </Button>
                              <Button variant="ghost" size="sm" onClick={() => setModal({ type: 'removeSubject', subject })}>
                                সরান
                              </Button>
                            </div>
                          )}
                        </Td>
                      </tr>
                    ))}
                  </Table>
                )}
              </Card>
            </section>

            {/* ---------- মার্কস এন্ট্রি ---------- */}
            <section>
              <h2 className="mb-3 text-lg font-semibold text-gray-900">মার্কস এন্ট্রি</h2>
              <Card className="p-4">
                <LoadState loading={targets.loading} error={targets.error}>
                  {groups.length === 0 ? (
                    <EmptyState>
                      {isAdmin
                        ? 'এই ক্লাসে কোনো সক্রিয় সেকশন বা পরীক্ষায় কোনো বিষয় নেই।'
                        : 'এই পরীক্ষায় মার্কস দেওয়ার মতো কোনো সেকশন-বিষয় আপনার বরাদ্দে নেই। Admin-কে বলুন।'}
                    </EmptyState>
                  ) : (
                    <div className="space-y-4">
                      {groups.map((group) => (
                        <div key={group.key}>
                          <p className="mb-2 text-sm font-semibold text-gray-700">{group.name}</p>
                          <div className="flex flex-wrap gap-2">
                            {group.items.map((target) => (
                              <Link
                                key={`${target.sectionId}-${target.subjectId}`}
                                to={`/exams/${id}/marks?sectionId=${target.sectionId}&subjectId=${target.subjectId}`}
                                className="rounded-lg bg-white px-3 py-1.5 text-sm font-medium text-indigo-700 ring-1 ring-indigo-200 hover:bg-indigo-50"
                              >
                                {target.subjectName}
                                {target.optional ? ' (ঐচ্ছিক)' : ''}
                              </Link>
                            ))}
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </LoadState>
              </Card>
            </section>

            {/* ---------- জানালাগুলো ---------- */}
            {modal?.type === 'edit' && <ExamEditModal exam={exam} onDone={done} onClose={() => setModal(null)} />}
            {modal?.type === 'deleteExam' && (
              <ConfirmModal
                title="পরীক্ষা মুছুন"
                message={`"${exam.name}" পরীক্ষাটা মুছবেন? মার্কস এন্ট্রি হয়ে গেলে মোছা যায় না।`}
                confirmLabel="মুছুন"
                danger
                onConfirm={removeExam}
                onClose={() => setModal(null)}
              />
            )}
            {modal?.type === 'addSubject' && (
              <SubjectAddModal
                exam={exam}
                existingSubjectIds={data.subjects.map((s) => s.subjectId)}
                onDone={done}
                onClose={() => setModal(null)}
              />
            )}
            {modal?.type === 'editSubject' && (
              <SubjectEditModal subject={modal.subject} onDone={done} onClose={() => setModal(null)} />
            )}
            {modal?.type === 'removeSubject' && (
              <ConfirmModal
                title="বিষয় সরান"
                message={`${modal.subject.subjectName} বিষয়টা এই পরীক্ষা থেকে সরাবেন? মার্কস এন্ট্রি হয়ে গেলে সরানো যায় না।`}
                confirmLabel="সরান"
                danger
                onConfirm={removeSubject}
                onClose={() => setModal(null)}
              />
            )}
          </>
        )}
      </LoadState>
    </>
  )
}