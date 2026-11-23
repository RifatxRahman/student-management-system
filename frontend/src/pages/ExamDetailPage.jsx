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
  const progress = useQuery(`/api/marks/progress?examId=${id}`)
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

            {/* ---------- মার্কস এন্ট্রি ও অগ্রগতি ---------- */}
            <section>
              <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
                <h2 className="text-lg font-semibold text-gray-900">মার্কস এন্ট্রি ও অগ্রগতি</h2>
                {progress.data && (
                  <div className="flex items-center gap-3 text-xs">
                    <span className="text-gray-600">
                      অগ্রগতি: <strong className="text-indigo-600 font-bold">{progress.data.overallCompletionPercentage}%</strong> ({progress.data.completedSheets}/{progress.data.totalSheets} বিষয় সম্পন্ন)
                    </span>
                    <div className="h-2 w-32 overflow-hidden rounded-full bg-gray-200">
                      <div
                        className="h-full bg-indigo-600 transition-all"
                        style={{ width: `${progress.data.overallCompletionPercentage}%` }}
                      />
                    </div>
                  </div>
                )}
              </div>

              <Card className="p-4">
                <LoadState loading={targets.loading} error={targets.error}>
                  {groups.length === 0 ? (
                    <EmptyState>
                      {isAdmin
                        ? 'এই ক্লাসে কোনো সক্রিয় সেকশন বা পরীক্ষায় কোনো বিষয় নেই।'
                        : 'এই পরীক্ষায় মার্কস দেওয়ার মতো কোনো সেকশন-বিষয় আপনার বরাদ্দে নেই। Admin-কে বলুন।'}
                    </EmptyState>
                  ) : (
                    <div className="space-y-6">
                      {groups.map((group) => (
                        <div key={group.key} className="border-b border-gray-100 pb-4 last:border-b-0 last:pb-0">
                          <p className="mb-3 text-sm font-bold text-gray-800">{group.name}</p>
                          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                            {group.items.map((target) => {
                              const prog = progress.data?.progressList?.find(
                                (p) => p.sectionId === target.sectionId && p.subjectId === target.subjectId
                              )
                              const isCompleted = prog?.status === 'COMPLETED'
                              const isPartial = prog?.status === 'PARTIAL'

                              return (
                                <Link
                                  key={`${target.sectionId}-${target.subjectId}`}
                                  to={`/exams/${id}/marks?sectionId=${target.sectionId}&subjectId=${target.subjectId}`}
                                  className="flex flex-col justify-between rounded-xl border border-gray-200 bg-white p-3 hover:border-indigo-300 hover:shadow-sm transition"
                                >
                                  <div className="flex items-start justify-between gap-2">
                                    <div>
                                      <p className="font-semibold text-sm text-gray-900">{target.subjectName}</p>
                                      <p className="text-xs text-gray-500 font-mono">
                                        ফুল: {plain(target.fullMarks)} | পাস: {plain(target.passMarks)}
                                        {target.optional ? ' (ঐচ্ছিক)' : ''}
                                      </p>
                                    </div>
                                    <Badge tone={isCompleted ? 'green' : isPartial ? 'yellow' : 'gray'}>
                                      {isCompleted ? '১০০% সম্পন্ন' : isPartial ? `${prog?.completionPercentage}%` : 'বাকি'}
                                    </Badge>
                                  </div>
                                  {prog?.assignedTeacherName && (
                                    <div className="mt-2 pt-2 border-t border-gray-50 flex items-center justify-between text-xs text-gray-500">
                                      <span>শিক্ষক: <strong className="text-gray-700">{prog.assignedTeacherName}</strong></span>
                                      <span className="text-indigo-600 font-medium">নম্বর দিন →</span>
                                    </div>
                                  )}
                                </Link>
                              )
                            })}
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