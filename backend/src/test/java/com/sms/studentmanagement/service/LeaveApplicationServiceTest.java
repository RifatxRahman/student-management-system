package com.sms.studentmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sms.studentmanagement.dto.TeacherAttendanceDtos.LeaveApplicationRequest;
import com.sms.studentmanagement.dto.TeacherAttendanceDtos.LeaveApplicationResponse;
import com.sms.studentmanagement.entity.LeaveApplication;
import com.sms.studentmanagement.entity.LeaveStatus;
import com.sms.studentmanagement.entity.LeaveType;
import com.sms.studentmanagement.entity.Teacher;
import com.sms.studentmanagement.entity.TeacherAttendance;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.repository.HolidayRepository;
import com.sms.studentmanagement.repository.LeaveApplicationRepository;
import com.sms.studentmanagement.repository.TeacherAttendanceRepository;
import com.sms.studentmanagement.repository.TeacherRepository;
import com.sms.studentmanagement.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class LeaveApplicationServiceTest {

    @Mock
    LeaveApplicationRepository leaveRepository;

    @Mock
    TeacherRepository teacherRepository;

    @Mock
    TeacherAttendanceRepository attendanceRepository;

    @Mock
    HolidayRepository holidayRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    AuditService auditService;

    LeaveApplicationService service;

    @BeforeEach
    void setUp() {
        service = new LeaveApplicationService(
                leaveRepository,
                teacherRepository,
                attendanceRepository,
                holidayRepository,
                userRepository,
                auditService);
    }

    @Test
    void apply_success() {
        Teacher teacher = new Teacher();
        teacher.setId(1L);
        teacher.setFullName("Sir Rahim");

        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(leaveRepository.hasOverlappingLeave(1L, LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 12))).thenReturn(false);

        LeaveApplicationRequest req = new LeaveApplicationRequest(
                1L,
                LeaveType.CASUAL,
                LocalDate.of(2026, 5, 10),
                LocalDate.of(2026, 5, 12),
                BigDecimal.valueOf(3),
                "Family emergency");

        LeaveApplicationResponse res = service.apply(req, 1L, true);

        assertNotNull(res);
        assertEquals(LeaveStatus.PENDING, res.status());
        assertEquals("Sir Rahim", res.teacherName());
        verify(leaveRepository).save(any(LeaveApplication.class));
        verify(auditService).log(any(), any(), any(), any());
    }

    @Test
    void apply_overlappingDates_throwsConflict() {
        Teacher teacher = new Teacher();
        teacher.setId(1L);

        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(leaveRepository.hasOverlappingLeave(1L, LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 12))).thenReturn(true);

        LeaveApplicationRequest req = new LeaveApplicationRequest(
                1L,
                LeaveType.CASUAL,
                LocalDate.of(2026, 5, 10),
                LocalDate.of(2026, 5, 12),
                BigDecimal.valueOf(3),
                "Medical");

        ApiException ex = assertThrows(ApiException.class, () -> service.apply(req, 1L, true));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void approve_syncsAttendanceToLeaveStatus() {
        Teacher teacher = new Teacher();
        teacher.setId(1L);
        teacher.setFullName("Sir Karim");

        LeaveApplication leave = new LeaveApplication();
        leave.setId(100L);
        leave.setTeacher(teacher);
        leave.setStatus(LeaveStatus.PENDING);
        leave.setLeaveType(LeaveType.SICK);
        leave.setStartDate(LocalDate.of(2026, 5, 11)); // Monday
        leave.setEndDate(LocalDate.of(2026, 5, 11));
        leave.setTotalDays(BigDecimal.ONE);

        when(leaveRepository.findById(100L)).thenReturn(Optional.of(leave));
        when(holidayRepository.findByHolidayDateBetweenOrderByHolidayDate(any(), any())).thenReturn(Collections.emptyList());
        when(attendanceRepository.findByTeacherIdAndAttendanceDate(1L, LocalDate.of(2026, 5, 11))).thenReturn(Optional.empty());

        LeaveApplicationResponse res = service.approve(100L, 2L, "Approved by Principal");

        assertNotNull(res);
        assertEquals(LeaveStatus.APPROVED, res.status());
        verify(attendanceRepository).save(any(TeacherAttendance.class));
        verify(auditService).log(any(), any(), any(), any());
    }

    @Test
    void reject_updatesStatusToRejected() {
        Teacher teacher = new Teacher();
        teacher.setId(1L);
        teacher.setFullName("Sir Karim");

        LeaveApplication leave = new LeaveApplication();
        leave.setId(100L);
        leave.setTeacher(teacher);
        leave.setStatus(LeaveStatus.PENDING);

        when(leaveRepository.findById(100L)).thenReturn(Optional.of(leave));

        LeaveApplicationResponse res = service.reject(100L, 2L, "Not enough staff on that day");

        assertEquals(LeaveStatus.REJECTED, res.status());
        assertEquals("Not enough staff on that day", res.actionReason());
        verify(auditService).log(any(), any(), any(), any());
    }
}
