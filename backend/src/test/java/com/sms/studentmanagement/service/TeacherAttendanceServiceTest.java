package com.sms.studentmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sms.studentmanagement.dto.TeacherAttendanceDtos.TeacherAttendanceEntry;
import com.sms.studentmanagement.dto.TeacherAttendanceDtos.TeacherAttendanceSheetRequest;
import com.sms.studentmanagement.dto.TeacherAttendanceDtos.TeacherAttendanceSheetResponse;
import com.sms.studentmanagement.dto.TeacherAttendanceDtos.TeacherMonthlySummary;
import com.sms.studentmanagement.entity.Holiday;
import com.sms.studentmanagement.entity.LeaveApplication;
import com.sms.studentmanagement.entity.LeaveType;
import com.sms.studentmanagement.entity.Teacher;
import com.sms.studentmanagement.entity.TeacherAttendance;
import com.sms.studentmanagement.entity.TeacherAttendanceStatus;
import com.sms.studentmanagement.repository.HolidayRepository;
import com.sms.studentmanagement.repository.LeaveApplicationRepository;
import com.sms.studentmanagement.repository.TeacherAttendanceRepository;
import com.sms.studentmanagement.repository.TeacherRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeacherAttendanceServiceTest {

    @Mock
    TeacherRepository teacherRepository;

    @Mock
    TeacherAttendanceRepository attendanceRepository;

    @Mock
    LeaveApplicationRepository leaveRepository;

    @Mock
    HolidayRepository holidayRepository;

    @Mock
    AuditService auditService;

    TeacherAttendanceService service;

    @BeforeEach
    void setUp() {
        service = new TeacherAttendanceService(
                teacherRepository,
                attendanceRepository,
                leaveRepository,
                holidayRepository,
                auditService);
    }

    @Test
    void getSheet_combinesAttendanceAndApprovedLeaves() {
        LocalDate date = LocalDate.of(2026, 3, 10);

        Teacher t1 = new Teacher();
        t1.setId(1L);
        t1.setFullName("Sir Rahim");
        t1.setActive(true);

        Teacher t2 = new Teacher();
        t2.setId(2L);
        t2.setFullName("Sir Karim");
        t2.setActive(true);

        List<Teacher> teachers = new ArrayList<>();
        teachers.add(t1);
        teachers.add(t2);

        when(teacherRepository.findAllWithUser()).thenReturn(teachers);

        TeacherAttendance att1 = new TeacherAttendance();
        att1.setTeacher(t1);
        att1.setStatus(TeacherAttendanceStatus.PRESENT);
        att1.setAttendanceDate(date);

        List<TeacherAttendance> existing = new ArrayList<>();
        existing.add(att1);
        when(attendanceRepository.findByAttendanceDateWithTeacher(date)).thenReturn(existing);

        LeaveApplication leave = new LeaveApplication();
        leave.setTeacher(t2);
        leave.setLeaveType(LeaveType.CASUAL);
        List<LeaveApplication> approvedLeaves = new ArrayList<>();
        approvedLeaves.add(leave);
        when(leaveRepository.findAllApprovedLeavesOnDate(date)).thenReturn(approvedLeaves);

        TeacherAttendanceSheetResponse sheet = service.getSheet(date);

        assertNotNull(sheet);
        assertEquals(2, sheet.totalTeachers());
        assertEquals(1, sheet.presentCount());
        assertEquals(1, sheet.leaveCount());
    }

    @Test
    void saveSheet_savesAllEntriesUsingIndexLoop() {
        LocalDate date = LocalDate.of(2026, 3, 10);

        Teacher t1 = new Teacher();
        t1.setId(10L);
        t1.setFullName("Teacher 10");
        t1.setActive(true);

        when(teacherRepository.findById(10L)).thenReturn(Optional.of(t1));
        when(attendanceRepository.findByTeacherIdAndAttendanceDate(10L, date)).thenReturn(Optional.empty());

        List<Teacher> all = new ArrayList<>();
        all.add(t1);
        when(teacherRepository.findAllWithUser()).thenReturn(all);
        when(attendanceRepository.findByAttendanceDateWithTeacher(date)).thenReturn(Collections.emptyList());
        when(leaveRepository.findAllApprovedLeavesOnDate(date)).thenReturn(Collections.emptyList());

        List<TeacherAttendanceEntry> entries = new ArrayList<>();
        entries.add(new TeacherAttendanceEntry(10L, TeacherAttendanceStatus.PRESENT, LocalTime.of(8, 45), LocalTime.of(16, 0), "Regular"));

        TeacherAttendanceSheetRequest request = new TeacherAttendanceSheetRequest(date, entries);

        TeacherAttendanceSheetResponse res = service.saveSheet(request, 1L);

        assertNotNull(res);
        verify(attendanceRepository).save(any(TeacherAttendance.class));
        verify(auditService).log(any(), any(), any(), any());
    }

    @Test
    void getMonthlySummary_calculatesPercentageUsingBigDecimal() {
        Long teacherId = 5L;
        int year = 2026;
        int month = 4; // April (30 days)

        Teacher t = new Teacher();
        t.setId(teacherId);
        t.setFullName("Test Teacher");
        when(teacherRepository.findById(teacherId)).thenReturn(Optional.of(t));

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);

        Holiday holiday = new Holiday();
        holiday.setHolidayDate(LocalDate.of(2026, 4, 14)); // Bengali New Year
        List<Holiday> holidays = new ArrayList<>();
        holidays.add(holiday);
        when(holidayRepository.findByHolidayDateBetweenOrderByHolidayDate(start, end)).thenReturn(holidays);

        List<TeacherAttendance> records = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            TeacherAttendance att = new TeacherAttendance();
            att.setStatus(TeacherAttendanceStatus.PRESENT);
            records.add(att);
        }
        for (int i = 0; i < 2; i++) {
            TeacherAttendance att = new TeacherAttendance();
            att.setStatus(TeacherAttendanceStatus.HALF_DAY);
            records.add(att);
        }
        when(attendanceRepository.findByTeacherIdAndAttendanceDateBetween(teacherId, start, end)).thenReturn(records);

        TeacherMonthlySummary summary = service.getMonthlySummary(teacherId, year, month);

        assertNotNull(summary);
        assertEquals(20, summary.presentDays());
        assertEquals(2, summary.halfDayDays());
        assertNotNull(summary.attendancePercentage());
        assertEquals(BigDecimal.class, summary.attendancePercentage().getClass());
    }
}
