import { Suspense, lazy } from 'react'
import { Route, Routes } from 'react-router'
import ProtectedRoute from './auth/ProtectedRoute.jsx'
import Layout from './components/Layout.jsx'
import { FullPageSpinner } from './components/ui.jsx'

import DashboardPage from './pages/DashboardPage.jsx'
import LoginPage from './pages/LoginPage.jsx'

const AttendancePage = lazy(() => import('./pages/AttendancePage.jsx'))
const AttendanceReportPage = lazy(() => import('./pages/AttendanceReportPage.jsx'))
const AuditLogsPage = lazy(() => import('./pages/AuditLogsPage.jsx'))
const ChangePasswordPage = lazy(() => import('./pages/ChangePasswordPage.jsx'))
const ClassDetailPage = lazy(() => import('./pages/ClassDetailPage.jsx'))
const ClassesPage = lazy(() => import('./pages/ClassesPage.jsx'))
const DeletedStudentsPage = lazy(() => import('./pages/DeletedStudentsPage.jsx'))
const ExamDetailPage = lazy(() => import('./pages/ExamDetailPage.jsx'))
const ExamsPage = lazy(() => import('./pages/ExamsPage.jsx'))
const GradeScalePage = lazy(() => import('./pages/GradeScalePage.jsx'))
const GuardiansPage = lazy(() => import('./pages/GuardiansPage.jsx'))
const HolidaysPage = lazy(() => import('./pages/HolidaysPage.jsx'))
const MarksEntryPage = lazy(() => import('./pages/MarksEntryPage.jsx'))
const MarksheetPage = lazy(() => import('./pages/MarksheetPage.jsx'))
const NotFoundPage = lazy(() => import('./pages/NotFoundPage.jsx'))
const NotificationsPage = lazy(() => import('./pages/NotificationsPage.jsx'))
const PublishPage = lazy(() => import('./pages/PublishPage.jsx'))
const PromotionPage = lazy(() => import('./pages/PromotionPage.jsx'))
const ResultSheetPage = lazy(() => import('./pages/ResultSheetPage.jsx'))
const ResultsPage = lazy(() => import('./pages/ResultsPage.jsx'))
const RoutinePage = lazy(() => import('./pages/RoutinePage.jsx'))
const RoutineSetupPage = lazy(() => import('./pages/RoutineSetupPage.jsx'))
const AcademicCalendarPage = lazy(() => import('./pages/AcademicCalendarPage.jsx'))
const AdmitCardPage = lazy(() => import('./pages/AdmitCardPage.jsx'))
const SessionsPage = lazy(() => import('./pages/SessionsPage.jsx'))
const StudentDetailPage = lazy(() => import('./pages/StudentDetailPage.jsx'))
const StudentNewPage = lazy(() => import('./pages/StudentNewPage.jsx'))
const StudentsPage = lazy(() => import('./pages/StudentsPage.jsx'))
const SubjectsPage = lazy(() => import('./pages/SubjectsPage.jsx'))
const TeacherAttendancePage = lazy(() => import('./pages/TeacherAttendancePage.jsx'))
const TeacherDetailPage = lazy(() => import('./pages/TeacherDetailPage.jsx'))
const TeachersPage = lazy(() => import('./pages/TeachersPage.jsx'))
const LeaveApplicationsPage = lazy(() => import('./pages/LeaveApplicationsPage.jsx'))
const FeeStructurePage = lazy(() => import('./pages/FeeStructurePage.jsx'))
const FeeCollectionPage = lazy(() => import('./pages/FeeCollectionPage.jsx'))
const AccountsPage = lazy(() => import('./pages/AccountsPage.jsx'))
const UsersPage = lazy(() => import('./pages/UsersPage.jsx'))

const ADMIN = ['ADMIN']
const ATTENDANCE_ROLES = ['ADMIN', 'TEACHER']

function Only({ roles, children }) {
  return <ProtectedRoute roles={roles}>{children}</ProtectedRoute>
}

export default function App() {
  return (
    <Suspense fallback={<FullPageSpinner />}>
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

      {/* মার্কশিট ও রেজাল্ট শিট: Sidebar ছাড়া পাতা, যাতে প্রিন্টে শুধু মার্কশিট ছাপে */}
      <Route
        path="/exams/:examId/marksheets"
        element={
          <Only roles={ATTENDANCE_ROLES}>
            <MarksheetPage />
          </Only>
        }
      />
      <Route
        path="/exams/:examId/result-sheet"
        element={
          <Only roles={ATTENDANCE_ROLES}>
            <ResultSheetPage />
          </Only>
        }
      />
      <Route
        path="/exams/:examId/admit-cards"
        element={
          <Only roles={ATTENDANCE_ROLES}>
            <AdmitCardPage />
          </Only>
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

        <Route path="grade-scale" element={<GradeScalePage />} />
        <Route path="routine" element={<RoutinePage />} />
        <Route path="calendar" element={<AcademicCalendarPage />} />
        <Route
          path="routine/setup"
          element={
            <Only roles={ADMIN}>
              <RoutineSetupPage />
            </Only>
          }
        />
        <Route path="exams" element={<ExamsPage />} />
        <Route path="exams/:id" element={<ExamDetailPage />} />
        <Route
          path="exams/:examId/marks"
          element={
            <Only roles={ATTENDANCE_ROLES}>
              <MarksEntryPage />
            </Only>
          }
        />
        <Route
          path="exams/:examId/results"
          element={
            <Only roles={ATTENDANCE_ROLES}>
              <ResultsPage />
            </Only>
          }
        />
        <Route
          path="exams/:examId/publish"
          element={
            <Only roles={ADMIN}>
              <PublishPage />
            </Only>
          }
        />

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
          path="promotion"
          element={
            <Only roles={ADMIN}>
              <PromotionPage />
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
          path="teacher-attendance"
          element={
            <Only roles={ATTENDANCE_ROLES}>
              <TeacherAttendancePage />
            </Only>
          }
        />
        <Route
          path="leaves"
          element={
            <Only roles={ATTENDANCE_ROLES}>
              <LeaveApplicationsPage />
            </Only>
          }
        />
        <Route
          path="fee-structure"
          element={
            <Only roles={ADMIN}>
              <FeeStructurePage />
            </Only>
          }
        />
        <Route
          path="fee-collection"
          element={
            <Only roles={ADMIN}>
              <FeeCollectionPage />
            </Only>
          }
        />
        <Route
          path="accounts"
          element={
            <Only roles={ADMIN}>
              <AccountsPage />
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

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  </Suspense>
  )
}