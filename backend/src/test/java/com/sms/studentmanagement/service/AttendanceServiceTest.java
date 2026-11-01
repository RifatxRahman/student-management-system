package com.sms.studentmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sms.studentmanagement.config.AttendanceProperties;
import com.sms.studentmanagement.dto.AttendanceDtos.MarkEntry;
import com.sms.studentmanagement.dto.AttendanceDtos.MarkRequest;
import com.sms.studentmanagement.dto.AttendanceDtos.MarkResult;
import com.sms.studentmanagement.entity.Attendance;
import com.sms.studentmanagement.entity.AttendanceStatus;
import com.sms.studentmanagement.entity.Enrollment;
import com.sms.studentmanagement.entity.SchoolClass;
import com.sms.studentmanagement.entity.Section;
import com.sms.studentmanagement.entity.Student;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.repository.AttendanceRepository;
import com.sms.studentmanagement.repository.EnrollmentRepository;
import com.sms.studentmanagement.repository.SectionRepository;
import com.sms.studentmanagement.repository.TeacherAssignmentRepository;
import com.sms.studentmanagement.security.Actor;
import com.sms.studentmanagement.util.DhakaTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AttendanceServiceTest {

    @Mock
    SectionRepository sectionRepository;

    @Mock
    TeacherAssignmentRepository assignmentRepository;

    @Mock
    EnrollmentRepository enrollmentRepository;

    @Mock
    AttendanceRepository attendanceRepository;

    @Mock
    AttendanceAccess access;

    @Mock
    AttendanceCalendar calendar;

    @Mock
    AuditService auditService;

    @Mock
    NotificationService notificationService;

    AttendanceService service;

    final Actor admin = new Actor(1L, true);
    final Actor teacher = new Actor(10L, false);
    final LocalDate today = DhakaTime.today();

    @BeforeEach
    void setUp() {
        service = new AttendanceService(
                sectionRepository, assignmentRepository, enrollmentRepository, attendanceRepository,
                access, calendar, new AttendanceProperties(), auditService, notificationService);

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(1L);
        schoolClass.setName("Class 1");
        Section section = new Section();
        section.setId(10L);
        section.setName("A");
        section.setSchoolClass(schoolClass);
        when(sectionRepository.findById(10L)).thenReturn(Optional.of(section));
    }

    private Enrollment enrollment(long id, int roll) {
        Student student = new Student();
        student.setId(id);
        student.setStudentCode("2026-0000" + id);
        student.setFullName("Student " + id);
        Enrollment enrollment = new Enrollment();
        enrollment.setId(id);
        enrollment.setStudent(student);
        enrollment.setRollNumber(roll);
        return enrollment;
    }

    private void rosterOf(Enrollment... enrollments) {
        when(enrollmentRepository.findInSection(eq(10L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(enrollments));
    }

    private Attendance saved(long id, Enrollment enrollment, LocalDate date, AttendanceStatus status) {
        Attendance attendance = new Attendance();
        attendance.setId(id);
        attendance.setEnrollment(enrollment);
        attendance.setAttendanceDate(date);
        attendance.setStatus(status);
        return attendance;
    }

    private MarkRequest request(LocalDate date, String reason, MarkEntry... entries) {
        return new MarkRequest(10L, date, List.of(entries), reason);
    }

    @Test
    void rejectsFutureDate() {
        LocalDate tomorrow = today.plusDays(1);

        assertThrows(ApiException.class, () -> service.mark(admin,
                request(tomorrow, null, new MarkEntry(100L, AttendanceStatus.PRESENT, null))));
        verify(attendanceRepository, never()).saveAll(any());
    }

    @Test
    void rejectsClosedDay() {
        when(calendar.closedReason(today)).thenReturn("ছুটি: টেস্ট");

        assertThrows(ApiException.class, () -> service.mark(admin,
                request(today, null, new MarkEntry(100L, AttendanceStatus.PRESENT, null))));
    }

    @Test
    void teacherCannotMarkOlderThanWindow() {
        LocalDate old = today.minusDays(10);

        assertThrows(ApiException.class, () -> service.mark(teacher,
                request(old, null, new MarkEntry(100L, AttendanceStatus.PRESENT, null))));
    }

    @Test
    void adminCanMarkOlderThanWindow() {
        LocalDate old = today.minusDays(10);
        rosterOf(enrollment(100L, 1));

        MarkResult result = service.mark(admin,
                request(old, null, new MarkEntry(100L, AttendanceStatus.PRESENT, null)));

        assertEquals(1, result.created());
    }

    @Test
    void checksSectionAccess() {
        rosterOf(enrollment(100L, 1));

        service.mark(teacher, request(today, null, new MarkEntry(100L, AttendanceStatus.PRESENT, null)));

        verify(access).requireSection(teacher, 10L, today, today);
    }

    @Test
    void rejectsStudentOutsideRoster() {
        rosterOf(enrollment(100L, 1));

        assertThrows(ApiException.class, () -> service.mark(admin,
                request(today, null, new MarkEntry(999L, AttendanceStatus.PRESENT, null))));
    }

    @Test
    void rejectsSameStudentTwice() {
        rosterOf(enrollment(100L, 1));

        assertThrows(ApiException.class, () -> service.mark(admin, request(today, null,
                new MarkEntry(100L, AttendanceStatus.PRESENT, null),
                new MarkEntry(100L, AttendanceStatus.ABSENT, null))));
    }

    @Test
    void correctionWithoutReasonIsRejected() {
        Enrollment e = enrollment(100L, 1);
        rosterOf(e);
        when(attendanceRepository.findForSectionOnDate(10L, today))
                .thenReturn(List.of(saved(55L, e, today, AttendanceStatus.PRESENT)));

        assertThrows(ApiException.class, () -> service.mark(admin,
                request(today, null, new MarkEntry(100L, AttendanceStatus.ABSENT, null))));
        verify(auditService, never()).log(anyString(), anyString(), anyLong(), anyString());
    }

    @Test
    void correctionWithReasonIsSavedAndLogged() {
        Enrollment e = enrollment(100L, 1);
        Attendance existing = saved(55L, e, today, AttendanceStatus.PRESENT);
        rosterOf(e);
        when(attendanceRepository.findForSectionOnDate(10L, today)).thenReturn(List.of(existing));

        MarkResult result = service.mark(admin, request(today, "ভুল করে উপস্থিত দিয়েছিলাম",
                new MarkEntry(100L, AttendanceStatus.ABSENT, null)));

        assertEquals(1, result.changed());
        assertEquals(AttendanceStatus.ABSENT, existing.getStatus());
        assertEquals(1L, existing.getMarkedBy());
        verify(auditService).log(eq("ATTENDANCE_CORRECTED"), eq("ATTENDANCE"), eq(55L), contains("কারণ"));
    }

    @Test
    void sameDataNeedsNoReasonAndNoCorrectionLog() {
        Enrollment e = enrollment(100L, 1);
        rosterOf(e);
        when(attendanceRepository.findForSectionOnDate(10L, today))
                .thenReturn(List.of(saved(55L, e, today, AttendanceStatus.PRESENT)));

        MarkResult result = service.mark(admin,
                request(today, null, new MarkEntry(100L, AttendanceStatus.PRESENT, null)));

        assertEquals(1, result.unchanged());
        verify(auditService, never()).log(eq("ATTENDANCE_CORRECTED"), anyString(), any(), anyString());
    }
    @Test
    void absentQueuesNotification() {
        Enrollment e = enrollment(100L, 1);
        rosterOf(e);

        service.mark(admin, request(today, null, new MarkEntry(100L, AttendanceStatus.ABSENT, null)));

        verify(notificationService).queueAbsence(e.getStudent(), today);
    }

    @Test
    void presentDoesNotQueueNotification() {
        rosterOf(enrollment(100L, 1));

        service.mark(admin, request(today, null, new MarkEntry(100L, AttendanceStatus.PRESENT, null)));

        verify(notificationService, never()).queueAbsence(any(), any());
    }

    @Test
    void correctingAbsentToPresentCancelsUnsentNotification() {
        Enrollment e = enrollment(100L, 1);
        rosterOf(e);
        when(attendanceRepository.findForSectionOnDate(10L, today))
                .thenReturn(List.of(saved(55L, e, today, AttendanceStatus.ABSENT)));

        service.mark(admin, request(today, "ভুল করে অনুপস্থিত দিয়েছিলাম",
                new MarkEntry(100L, AttendanceStatus.PRESENT, null)));

        verify(notificationService).cancelUnsentAbsence(100L, today);
    }
}