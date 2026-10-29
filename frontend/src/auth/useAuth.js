import { useContext } from 'react'
import { AuthContext } from './AuthContext.js'

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth শুধু AuthProvider-এর ভেতরে ব্যবহার করা যায়')
  }
  return context
}