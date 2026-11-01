import { Route, Routes } from 'react-router'
import ProtectedRoute from './auth/ProtectedRoute.jsx'
import Layout from './components/Layout.jsx'
import ChangePasswordPage from './pages/ChangePasswordPage.jsx'
import ClassDetailPage from './pages/ClassDetailPage.jsx'
import ClassesPage from './pages/ClassesPage.jsx'
import DashboardPage from './pages/DashboardPage.jsx'
import DeletedStudentsPage from './pages/DeletedStudentsPage.jsx'
import GuardiansPage from './pages/GuardiansPage.jsx'
import LoginPage from './pages/LoginPage.jsx'
import NotFoundPage from './pages/NotFoundPage.jsx'
import SessionsPage from './pages/SessionsPage.jsx'
import StudentDetailPage from './pages/StudentDetailPage.jsx'
import StudentNewPage from './pages/StudentNewPage.jsx'
import StudentsPage from './pages/StudentsPage.jsx'
import SubjectsPage from './pages/SubjectsPage.jsx'
import TeacherDetailPage from './pages/TeacherDetailPage.jsx'
import TeachersPage from './pages/TeachersPage.jsx'
import UsersPage from './pages/UsersPage.jsx'

function AdminOnly({ children }) {
  return <ProtectedRoute roles={['ADMIN']}>{children}</ProtectedRoute>
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />

      <Route
        path="/change-password"
        element={
          <ProtectedRoute>
            <ChangePasswordPage />
          </ProtectedRoute>
        }
      />

      <Route
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route index element={<DashboardPage />} />
        <Route path="sessions" element={<SessionsPage />} />
        <Route path="classes" element={<ClassesPage />} />
        <Route path="classes/:id" element={<ClassDetailPage />} />
        <Route path="subjects" element={<SubjectsPage />} />

        <Route
          path="students"
          element={
            <AdminOnly>
              <StudentsPage />
            </AdminOnly>
          }
        />
        <Route
          path="students/new"
          element={
            <AdminOnly>
              <StudentNewPage />
            </AdminOnly>
          }
        />
        <Route
          path="students/deleted"
          element={
            <AdminOnly>
              <DeletedStudentsPage />
            </AdminOnly>
          }
        />
        <Route
          path="students/:id"
          element={
            <AdminOnly>
              <StudentDetailPage />
            </AdminOnly>
          }
        />
        <Route
          path="guardians"
          element={
            <AdminOnly>
              <GuardiansPage />
            </AdminOnly>
          }
        />
        <Route
          path="teachers"
          element={
            <AdminOnly>
              <TeachersPage />
            </AdminOnly>
          }
        />
        <Route
          path="teachers/:id"
          element={
            <AdminOnly>
              <TeacherDetailPage />
            </AdminOnly>
          }
        />
        <Route
          path="users"
          element={
            <AdminOnly>
              <UsersPage />
            </AdminOnly>
          }
        />
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}