import { useState } from 'react'
import { Alert, Button, SelectField, TextField } from './ui.jsx'

export default function EntityForm({ fields, initial, submitLabel, onSubmit, onCancel }) {
  const [values, setValues] = useState(() =>
    Object.fromEntries(fields.map((f) => [f.name, initial?.[f.name] ?? f.defaultValue ?? ''])),
  )
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  const fieldErrors = error?.fieldErrors ?? {}
  const hasFieldErrors = Object.keys(fieldErrors).length > 0

  function handleChange(name) {
    return (event) => setValues((current) => ({ ...current, [name]: event.target.value }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await onSubmit(values)
    } catch (err) {
      setError(err)
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {error && <Alert>{hasFieldErrors ? 'ঘরগুলো আবার দেখুন' : error.message}</Alert>}

      {fields.map((field) =>
        field.options ? (
          <SelectField
            key={field.name}
            label={field.label}
            options={field.options}
            value={values[field.name]}
            onChange={handleChange(field.name)}
            error={fieldErrors[field.name]}
            required={field.required}
          />
        ) : (
          <TextField
            key={field.name}
            label={field.label}
            type={field.type ?? 'text'}
            hint={field.hint}
            value={values[field.name]}
            onChange={handleChange(field.name)}
            error={fieldErrors[field.name]}
            required={field.required}
          />
        ),
      )}

      <div className="flex justify-end gap-2 pt-2">
        <Button variant="secondary" onClick={onCancel}>
          বাতিল
        </Button>
        <Button type="submit" disabled={submitting}>
          {submitting ? 'অপেক্ষা করুন...' : submitLabel}
        </Button>
      </div>
    </form>
  )
}