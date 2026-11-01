import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { api } from '../api/client.js'
import EntityForm from '../components/EntityForm.jsx'
import GuardianFormModal from '../components/GuardianFormModal.jsx'
import GuardianPicker from '../components/GuardianPicker.jsx'
import { Button, Card, PageHeader } from '../components/ui.jsx'
import { blankToNull, RELATION_OPTIONS } from '../utils/format.js'
import { STUDENT_FIELDS } from '../utils/studentFields.js'

const NEW_FIELDS = [
  ...STUDENT_FIELDS,
  { name: 'admissionDate', label: 'ভর্তির তারিখ (ঐচ্ছিক)', type: 'date', hint: 'খালি রাখলে আজকের তারিখ' },
  {
    name: 'guardianRelation',
    label: 'এই অভিভাবকের সম্পর্ক',
    required: true,
    defaultValue: 'FATHER',
    options: RELATION_OPTIONS,
  },
]

export default function StudentNewPage() {
  const navigate = useNavigate()
  const [guardian, setGuardian] = useState(null)
  const [showNewGuardian, setShowNewGuardian] = useState(false)

  async function create(values) {
    const { guardianRelation, ...rest } = values
    const detail = await api('/api/students', {
      method: 'POST',
      body: { ...blankToNull(rest), guardianId: guardian.id, guardianRelation },
    })
    navigate(`/students/${detail.student.id}`)
  }

  return (
    <>
      <div className="mb-3">
        <Link to="/students" className="text-sm text-gray-500 hover:text-gray-700">
          ← সব শিক্ষার্থী
        </Link>
      </div>

      <PageHeader title="নতুন শিক্ষার্থী" subtitle="আগে অভিভাবক বাছুন, তারপর শিক্ষার্থীর তথ্য দিন" />

      <div className="max-w-xl space-y-6">
        <Card className="p-5">
          <h2 className="mb-3 text-sm font-semibold text-gray-900">১. অভিভাবক</h2>
          {guardian ? (
            <div className="flex items-center justify-between gap-3 rounded-lg bg-gray-50 px-4 py-3">
              <div>
                <p className="text-sm font-medium text-gray-900">{guardian.fullName}</p>
                <p className="text-xs text-gray-500">{guardian.phone}</p>
              </div>
              <Button variant="secondary" size="sm" onClick={() => setGuardian(null)}>
                বদলান
              </Button>
            </div>
          ) : (
            <div className="space-y-4">
              <GuardianPicker onSelect={setGuardian} />
              <div className="border-t border-gray-100 pt-3">
                <Button variant="ghost" size="sm" onClick={() => setShowNewGuardian(true)}>
                  খুঁজে না পেলে নতুন অভিভাবক বানান
                </Button>
              </div>
            </div>
          )}
        </Card>

        {guardian && (
          <Card className="p-5">
            <h2 className="mb-3 text-sm font-semibold text-gray-900">২. শিক্ষার্থীর তথ্য</h2>
            <EntityForm
              fields={NEW_FIELDS}
              submitLabel="শিক্ষার্থী তৈরি করুন"
              onSubmit={create}
              onCancel={() => navigate('/students')}
            />
          </Card>
        )}
      </div>

      {showNewGuardian && (
        <GuardianFormModal
          onSaved={(saved) => {
            setGuardian(saved)
            setShowNewGuardian(false)
          }}
          onClose={() => setShowNewGuardian(false)}
        />
      )}
    </>
  )
}