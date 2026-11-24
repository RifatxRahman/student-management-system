import { useEffect, useId } from 'react'

export function Spinner({ size = 'md' }) {
  const sizes = { sm: 'h-4 w-4', md: 'h-5 w-5', lg: 'h-8 w-8' }
  return (
    <div
      role="status"
      aria-label="লোড হচ্ছে"
      className={`animate-spin rounded-full border-2 border-[var(--border-hairline)] border-t-[#E8A317] ${sizes[size]}`}
    />
  )
}

export function FullPageSpinner() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-[var(--canvas)]">
      <Spinner size="lg" />
    </div>
  )
}

const BUTTON_VARIANTS = {
  primary:
    'bg-[#E8A317] text-[#12231D] hover:bg-[#D49210] active:bg-[#BF800B] font-semibold shadow-sm focus-visible:ring-2 focus-visible:ring-[#12231D] focus-visible:ring-offset-2',
  secondary:
    'bg-white text-[var(--ink-primary)] border border-[var(--border-hairline)] hover:bg-[var(--surface-hover)] active:bg-[var(--surface-subtle)] font-medium shadow-sm focus-visible:ring-2 focus-visible:ring-[#12231D] focus-visible:ring-offset-2',
  danger:
    'bg-[#C8372D] text-white hover:bg-[#B32D24] active:bg-[#9B241C] font-semibold shadow-sm focus-visible:ring-2 focus-visible:ring-[#C8372D] focus-visible:ring-offset-2',
  ghost:
    'text-[var(--ink-secondary)] hover:bg-[var(--surface-hover)] hover:text-[var(--ink-primary)] font-medium focus-visible:ring-2 focus-visible:ring-[#12231D] focus-visible:ring-offset-2',
}

const BUTTON_SIZES = {
  md: 'min-h-[44px] min-w-[44px] px-4 py-2.5 text-sm',
  sm: 'min-h-[36px] min-w-[36px] px-3 py-1.5 text-xs',
}

export function Button({ variant = 'primary', size = 'md', className = '', type = 'button', ...props }) {
  return (
    <button
      type={type}
      className={`inline-flex items-center justify-center rounded-[10px] transition-colors duration-150 outline-none disabled:cursor-not-allowed disabled:opacity-45 ${BUTTON_VARIANTS[variant]} ${BUTTON_SIZES[size]} ${className}`}
      {...props}
    />
  )
}

export function Card({ children, className = '' }) {
  return (
    <div
      className={`rounded-[16px] border border-[var(--border-hairline)] bg-[var(--surface)] shadow-[0_2px_8px_rgba(18,35,29,0.04)] ${className}`}
    >
      {children}
    </div>
  )
}

export function PageHeader({ title, subtitle, action }) {
  return (
    <div className="mb-6 flex flex-wrap items-start justify-between gap-3">
      <div className="signature-margin">
        <h1 className="text-2xl font-bold text-[var(--ink-primary)] tracking-normal">{title}</h1>
        {subtitle && <p className="mt-1 text-sm text-[var(--ink-secondary)] font-normal">{subtitle}</p>}
      </div>
      {action}
    </div>
  )
}

export function Alert({ type = 'error', children }) {
  const styles = {
    error: 'bg-[#FCECEB] text-[#C8372D] border border-[#F6C2BE]',
    success: 'bg-[#EAF5EE] text-[#168754] border border-[#BDE3CC]',
    info: 'bg-[#FDF4E7] text-[#B36B18] border border-[#F8E1B9]',
  }

  const icons = {
    error: (
      <svg className="h-5 w-5 shrink-0 text-[#C8372D]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
      </svg>
    ),
    success: (
      <svg className="h-5 w-5 shrink-0 text-[#168754]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
    ),
    info: (
      <svg className="h-5 w-5 shrink-0 text-[#B36B18]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
    ),
  }

  return (
    <div
      role={type === 'error' ? 'alert' : 'status'}
      className={`rounded-[10px] px-4 py-3 text-sm flex items-start gap-3 ${styles[type]}`}
    >
      {icons[type]}
      <div className="flex-1 leading-relaxed">{children}</div>
    </div>
  )
}

export function Badge({ tone = 'gray', children }) {
  const tones = {
    gray: 'bg-[#EEF2F0] text-[#5F736A] border border-[#DCE4E0]',
    green: 'bg-[#EAF5EE] text-[#168754] border border-[#BDE3CC]',
    brand: 'bg-[#EAF2EF] text-[#0E3B2E] border border-[#C7DDD4]',
    indigo: 'bg-[#EAF2EF] text-[#0E3B2E] border border-[#C7DDD4]',
    amber: 'bg-[#FDF4E7] text-[#B36B18] border border-[#F8E1B9]',
    red: 'bg-[#FCECEB] text-[#C8372D] border border-[#F6C2BE]',
  }

  const dotColors = {
    gray: 'bg-[#5F736A]',
    green: 'bg-[#168754]',
    brand: 'bg-[#0E3B2E]',
    indigo: 'bg-[#0E3B2E]',
    amber: 'bg-[#B36B18]',
    red: 'bg-[#C8372D]',
  }

  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-semibold ${tones[tone]}`}
    >
      <span className={`h-1.5 w-1.5 rounded-full ${dotColors[tone]}`} aria-hidden="true" />
      {children}
    </span>
  )
}

export function TextField({ label, error, hint, className = '', ...props }) {
  const id = useId()
  const hintId = `${id}-hint`
  const errorId = `${id}-error`
  const describedBy = error ? errorId : hint ? hintId : undefined

  return (
    <div>
      {label && (
        <label htmlFor={id} className="block text-[13px] font-medium text-[var(--ink-secondary)] mb-1.5">
          {label}
        </label>
      )}
      <input
        id={id}
        aria-describedby={describedBy}
        aria-invalid={Boolean(error)}
        className={`block w-full min-h-[44px] rounded-[10px] border px-3.5 py-2.5 text-sm bg-white text-[var(--ink-primary)] shadow-sm outline-none transition duration-150 focus:border-[#E8A317] focus:ring-2 focus:ring-[#E8A317]/25 ${
          error ? 'border-[#C8372D] focus:border-[#C8372D] focus:ring-[#C8372D]/20' : 'border-[var(--border-hairline)]'
        } ${className}`}
        {...props}
      />
      {hint && !error && (
        <p id={hintId} className="mt-1 text-xs text-[var(--ink-tertiary)]">
          {hint}
        </p>
      )}
      {error && (
        <p id={errorId} role="alert" className="mt-1.5 flex items-center gap-1 text-xs text-[#C8372D] font-medium">
          <span aria-hidden="true">⚠</span> {error}
        </p>
      )}
    </div>
  )
}

export function SelectField({ label, error, options, children, className = '', ...props }) {
  const id = useId()
  const errorId = `${id}-error`

  return (
    <div>
      {label && (
        <label htmlFor={id} className="block text-[13px] font-medium text-[var(--ink-secondary)] mb-1.5">
          {label}
        </label>
      )}
      <select
        id={id}
        aria-describedby={error ? errorId : undefined}
        aria-invalid={Boolean(error)}
        className={`block w-full min-h-[44px] rounded-[10px] border bg-white px-3.5 py-2.5 text-sm text-[var(--ink-primary)] shadow-sm outline-none transition duration-150 focus:border-[#E8A317] focus:ring-2 focus:ring-[#E8A317]/25 ${
          error ? 'border-[#C8372D] focus:border-[#C8372D] focus:ring-[#C8372D]/20' : 'border-[var(--border-hairline)]'
        } ${className}`}
        {...props}
      >
        {options
          ? options.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))
          : children}
      </select>
      {error && (
        <p id={errorId} role="alert" className="mt-1.5 flex items-center gap-1 text-xs text-[#C8372D] font-medium">
          <span aria-hidden="true">⚠</span> {error}
        </p>
      )}
    </div>
  )
}

export function Modal({ title, onClose, dismissible = true, children }) {
  useEffect(() => {
    if (!dismissible) return undefined
    const onKey = (event) => {
      if (event.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [dismissible, onClose])

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/45 backdrop-blur-[2px] p-4 transition-opacity duration-200"
      onMouseDown={dismissible ? onClose : undefined}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-label={title}
        className="w-full max-w-md rounded-[16px] border border-[var(--border-hairline)] bg-[var(--surface)] p-6 shadow-2xl transition-all"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="signature-margin">
          <h2 className="text-lg font-bold text-[var(--ink-primary)] tracking-normal">{title}</h2>
        </div>
        <div className="mt-4">{children}</div>
      </div>
    </div>
  )
}

export function Table({ headers, children }) {
  return (
    <div className="overflow-x-auto rounded-[12px] border border-[var(--border-hairline)] bg-[var(--surface)]">
      <table className="min-w-full divide-y divide-[var(--border-hairline)] text-sm">
        <thead className="bg-[var(--surface-subtle)]">
          <tr>
            {headers.map((header, index) => (
              <th
                key={`${header}-${index}`}
                scope="col"
                className="px-4 py-3 text-left text-[13px] font-semibold text-[var(--ink-secondary)]"
              >
                {header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-[var(--border-hairline)] bg-white">{children}</tbody>
      </table>
    </div>
  )
}

export function Td({ children, className = '' }) {
  return <td className={`px-4 py-3.5 text-[var(--ink-primary)] ${className}`}>{children}</td>
}

export function EmptyState({ children }) {
  return <p className="px-4 py-12 text-center text-sm text-[var(--ink-secondary)]">{children}</p>
}

export function LoadState({ loading, error, children }) {
  if (loading) {
    return (
      <div className="flex justify-center py-12">
        <Spinner />
      </div>
    )
  }
  if (error) {
    return <Alert>{error.message}</Alert>
  }
  return children
}
