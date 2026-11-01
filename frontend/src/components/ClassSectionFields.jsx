import { useQuery } from '../hooks/useQuery.js'
import { SelectField } from './ui.jsx'

// allowAll = true হলে "সব" বিকল্প থাকে (ছাঁকার জন্য)। classId ও sectionId লেখা (string)।
export default function ClassSectionFields({ classId, sectionId, onChange, allowAll = false, required = false }) {
  const classes = useQuery('/api/classes')
  const sections = useQuery(classId ? `/api/classes/${classId}/sections` : null)
  const emptyLabel = allowAll ? 'সব' : 'বাছুন'

  const classOptions = [
    { value: '', label: emptyLabel },
    ...(classes.data ?? []).filter((c) => c.active).map((c) => ({ value: String(c.id), label: c.name })),
  ]
  const sectionOptions = [
    { value: '', label: classId ? emptyLabel : 'আগে ক্লাস বাছুন' },
    ...(sections.data ?? []).filter((s) => s.active).map((s) => ({ value: String(s.id), label: s.name })),
  ]

  return (
    <>
      <SelectField
        label="ক্লাস"
        options={classOptions}
        value={classId}
        required={required}
        onChange={(event) => onChange({ classId: event.target.value, sectionId: '' })}
      />
      <SelectField
        label="সেকশন"
        options={sectionOptions}
        value={sectionId}
        required={required}
        disabled={!classId}
        onChange={(event) => onChange({ classId, sectionId: event.target.value })}
      />
    </>
  )
}