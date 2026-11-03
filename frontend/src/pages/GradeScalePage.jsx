import { useState } from 'react'
import { useAuth } from '../auth/useAuth.js'
import GradeScaleEditModal from '../components/GradeScaleEditModal.jsx'
import { Alert, Button, Card, EmptyState, LoadState, PageHeader, Table, Td } from '../components/ui.jsx'
import { useQuery } from '../hooks/useQuery.js'
import { plain } from '../utils/format.js'

export default function GradeScalePage() {
  const { user } = useAuth()
  const isAdmin = user.role === 'ADMIN'
  const { data, error, loading, reload } = useQuery('/api/grade-scale')
  const [editing, setEditing] = useState(false)

  function done() {
    setEditing(false)
    reload()
  }

  return (
    <>
      <PageHeader
        title="গ্রেড স্কেল"
        subtitle="নম্বর (শতকরা) থেকে গ্রেড ও গ্রেড পয়েন্টের নিয়ম"
        action={
          isAdmin && (
            <Button onClick={() => setEditing(true)} disabled={!data}>
              বদলান
            </Button>
          )
        }
      />

      <div className="mb-4">
        <Alert type="info">
          পাস-ফেল ঠিক হয় প্রতিটা পরীক্ষার বিষয়ের <strong>পাস মার্কস</strong> দিয়ে (সাধারণত ৩৩)। ফেল করা বিষয়ের গ্রেড
          সবসময় সবচেয়ে নিচেরটা (F) ও পয়েন্ট ০.০০। এই স্কেল শুধু পাস করা নম্বরের গ্রেড ঠিক করে।
        </Alert>
      </div>

      <Card>
        <LoadState loading={loading} error={error}>
          {data?.length === 0 ? (
            <EmptyState>কোনো গ্রেড নেই।</EmptyState>
          ) : (
            <Table headers={['গ্রেড', 'গ্রেড পয়েন্ট', 'নম্বর (শতকরা)']}>
              {data?.map((band) => (
                <tr key={band.grade}>
                  <Td className="font-semibold text-gray-900">{band.grade}</Td>
                  <Td>{Number(band.gradePoint).toFixed(2)}</Td>
                  <Td>
                    {plain(band.fromPercent)}% থেকে{' '}
                    {band.toInclusive ? `${plain(band.toPercent)}% পর্যন্ত` : `${plain(band.toPercent)}%-এর আগে পর্যন্ত`}
                  </Td>
                </tr>
              ))}
            </Table>
          )}
        </LoadState>
      </Card>

      {editing && data && <GradeScaleEditModal bands={data} onDone={done} onClose={() => setEditing(false)} />}
    </>
  )
}