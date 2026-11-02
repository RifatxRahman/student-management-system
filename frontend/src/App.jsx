import { Route, Routes } from 'react-router'
import ProtectedRoute from './auth/ProtectedRoute.jsx'
import Layout from './components/Layout.jsx'
import AttendancePage from './pages/AttendancePage.jsx'
import AttendanceReportPage from './pages/AttendanceReportPage.jsx'
import AuditLogsPage from './pages/AuditLogsPage.jsx'
import ChangePasswordPage from './pages/ChangePasswordPage.jsx'
import ClassDetailPage from './pages/ClassDetailPage.jsx'
import ClassesPage from './pages/ClassesPage.jsx'
import DashboardPage from './pages/DashboardPage.jsx'
import DeletedStudentsPage from './pages/DeletedStudentsPage.jsx'
import GuardiansPage from './pages/GuardiansPage.jsx'
import HolidaysPage from './pages/HolidaysPage.jsx'
import LoginPage from './pages/LoginPage.jsx'
import MarksheetPrototypePage from './pages/MarksheetPrototypePage.jsx'
import NotFoundPage from './pages/NotFoundPage.jsx'
import NotificationsPage from './pages/NotificationsPage.jsx'
import SessionsPage from './pages/SessionsPage.jsx'
import StudentDetailPage from './pages/StudentDetailPage.jsx'
import StudentNewPage from './pages/StudentNewPage.jsx'
import StudentsPage from './pages/StudentsPage.jsx'
import SubjectsPage from './pages/SubjectsPage.jsx'
import TeacherDetailPage from './pages/TeacherDetailPage.jsx'
import TeachersPage from './pages/TeachersPage.jsx'
import UsersPage from './pages/UsersPage.jsx'

const ADMIN = ['ADMIN']
const ATTENDANCE_ROLES = ['ADMIN', 'TEACHER']

function Only({ roles, children }) {
  return <ProtectedRoute roles={roles}>{children}</ProtectedRoute>
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
        <Route path="holidays" element={<HolidaysPage />} />

        <Route
          path="attendance"
          element={
            <Only roles={ATTENDANCE_ROLES}>
              <AttendancePage />
            </Only>
          }
        />
        <Route
          path="attendance/report"
          element={
            <Only roles={ATTENDANCE_ROLES}>
              <AttendanceReportPage />
            </Only>
          }
        />

        <Route
          path="students"
          element={
            <Only roles={ADMIN}>
              <StudentsPage />
            </Only>
          }
        />
        <Route
          path="students/new"
          element={
            <Only roles={ADMIN}>
              <StudentNewPage />
            </Only>
          }
        />
        <Route
          path="students/deleted"
          element={
            <Only roles={ADMIN}>
              <DeletedStudentsPage />
            </Only>
          }
        />
        <Route
          path="students/:id"
          element={
            <Only roles={ADMIN}>
              <StudentDetailPage />
            </Only>
          }
        />
        <Route
          path="guardians"
          element={
            <Only roles={ADMIN}>
              <GuardiansPage />
            </Only>
          }
        />
        <Route
          path="teachers"
          element={
            <Only roles={ADMIN}>
              <TeachersPage />
            </Only>
          }
        />
        <Route
          path="teachers/:id"
          element={
            <Only roles={ADMIN}>
              <TeacherDetailPage />
            </Only>
          }
        />
        <Route
          path="notifications"
          element={
            <Only roles={ADMIN}>
              <NotificationsPage />
            </Only>
          }
        />
        <Route
          path="audit-logs"
          element={
            <Only roles={ADMIN}>
              <AuditLogsPage />
            </Only>
          }
        />
        <Route
          path="users"
          element={
            <Only roles={ADMIN}>
              <UsersPage />
            </Only>
          }
        />
      </Route>

      <Route
        path="/prototype/marksheet"
        element={
          <ProtectedRoute roles={['ADMIN']}>
            <MarksheetPrototypePage />
          </ProtectedRoute>
        }
      />

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}