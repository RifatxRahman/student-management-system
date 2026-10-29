import { Navigate, useLocation } from 'react-router'
import { FullPageSpinner } from '../components/ui.jsx'
import { useAuth } from './useAuth.js'

export default function ProtectedRoute({ roles, children }) {
  const { user, loading } = useAuth()
  const location = useLocation()

  if (loading) {
    return <FullPageSpinner />
  }
  if (!user) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }
  if (user.mustChangePassword && location.pathname !== '/change-password') {
    return <Navigate to="/change-password" replace />
  }
  if (roles && !roles.includes(user.role)) {
    return <Navigate to="/" replace />
  }
  return children
}