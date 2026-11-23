package com.sms.studentmanagement.service;

import com.sms.studentmanagement.dto.TeacherAttendanceDtos.TeacherAttendanceEntry;
import com.sms.studentmanagement.dto.TeacherAttendanceDtos.TeacherAttendanceRow;
import com.sms.studentmanagement.dto.TeacherAttendanceDtos.TeacherAttendanceSheetRequest;
import com.sms.studentmanagement.dto.TeacherAttendanceDtos.TeacherAttendanceSheetResponse;
import com.sms.studentmanagement.dto.TeacherAttendanceDtos.TeacherMonthlySummary;
import com.sms.studentmanagement.entity.Holiday;
import com.sms.studentmanagement.entity.LeaveApplication;
import com.sms.studentmanagement.entity.Teacher;
import com.sms.studentmanagement.entity.TeacherAttendance;
import com.sms.studentmanagement.entity.TeacherAttendanceStatus;
import com.sms.studentmanagement.exception.NotFoundException;
import com.sms.studentmanagement.repository.HolidayRepository;
import com.sms.studentmanagement.repository.LeaveApplicationRepository;
import com.sms.studentmanagement.repository.TeacherAttendanceRepository;
import com.sms.studentmanagement.repository.TeacherRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeacherAttendanceService {

    private final TeacherRepository teacherRepository;
    private final TeacherAttendanceRepository attendanceRepository;
    private final LeaveApplicationRepository leaveRepository;
    private final HolidayRepository holidayRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public TeacherAttendanceSheetResponse getSheet(LocalDate date) {
        List<Teacher> allTeachers = teacherRepository.findAllWithUser();
        List<TeacherAttendance> existingList = attendanceRepository.findByAttendanceDateWithTeacher(date);
        List<LeaveApplication> approvedLeaves = leaveRepository.findAllApprovedLeavesOnDate(date);

        Map<Long, TeacherAttendance> attendanceMap = new HashMap<>();
        for (int i = 0; i < existingList.size(); i++) {
            TeacherAttendance att = existingList.get(i);
            attendanceMap.put(att.getTeacher().getId(), att);
        }

        Map<Long, LeaveApplication> leaveMap = new HashMap<>();
        for (int i = 0; i < approvedLeaves.size(); i++) {
            LeaveApplication leave = approvedLeaves.get(i);
            leaveMap.put(leave.getTeacher().getId(), leave);
        }

        List<TeacherAttendanceRow> rows = new ArrayList<>();
        int presentCount = 0;
        int lateCount = 0;
        int absentCount = 0;
        int leaveCount = 0;
        int halfDayCount = 0;

        for (int i = 0; i < allTeachers.size(); i++) {
            Teacher teacher = allTeachers.get(i);
            if (!teacher.isActive()) {
                continue;
            }

            TeacherAttendance att = attendanceMap.get(teacher.getId());
            LeaveApplication leave = leaveMap.get(teacher.getId());

            TeacherAttendanceStatus status = null;
            java.time.LocalTime inTime = null;
            java.time.LocalTime outTime = null;
            String remark = null;

            if (att != null) {
                status = att.getStatus();
                inTime = att.getInTime();
                outTime = att.getOutTime();
                remark = att.getRemark();
            } else if (leave != null) {
                status = TeacherAttendanceStatus.LEAVE;
                remark = "ছুটি: " + leave.getLeaveType().name();
            }

            if (status != null) {
                switch (status) {
                    case PRESENT -> presentCount++;
                    case LATE -> lateCount++;
                    case ABSENT -> absentCount++;
                    case LEAVE -> leaveCount++;
                    case HALF_DAY -> halfDayCount++;
                }
            }

            rows.add(new TeacherAttendanceRow(
                    teacher.getId(),
                    teacher.getEmployeeId(),
                    teacher.getFullName(),
                    teacher.getDesignation(),
                    status,
                    inTime,
                    outTime,
                    remark,
                    leave != null,
                    leave != null ? leave.getLeaveType() : null));
        }

        return new TeacherAttendanceSheetResponse(
                date,
                rows,
                rows.size(),
                presentCount,
                lateCount,
                absentCount,
                leaveCount,
                halfDayCount);
    }

    @Transactional
    public TeacherAttendanceSheetResponse saveSheet(TeacherAttendanceSheetRequest request, Long currentUserId) {
        LocalDate date = request.date();
        List<TeacherAttendanceEntry> entries = request.entries();

        for (int i = 0; i < entries.size(); i++) {
            TeacherAttendanceEntry entry = entries.get(i);
            Teacher teacher = teacherRepository.findById(entry.teacherId())
                    .orElseThrow(() -> new NotFoundException("শিক্ষক পাওয়া যায়নি (ID: " + entry.teacherId() + ")"));

            Optional<TeacherAttendance> existingOpt = attendanceRepository.findByTeacherIdAndAttendanceDate(teacher.getId(), date);
            TeacherAttendance attendance;
            if (existingOpt.isPresent()) {
                attendance = existingOpt.get();
            } else {
                attendance = new TeacherAttendance();
                attendance.setTeacher(teacher);
                attendance.setAttendanceDate(date);
            }

            attendance.setStatus(entry.status());
            attendance.setInTime(entry.inTime());
            attendance.setOutTime(entry.outTime());
            attendance.setRemark(entry.remark() != null ? entry.remark().trim() : null);
            attendance.setMarkedBy(currentUserId);
            attendanceRepository.save(attendance);
        }

        auditService.log("TEACHER_ATTENDANCE_SAVED", "TEACHER_ATTENDANCE", null,
                "তারিখ: " + date + ", মোট এন্ট্রি: " + entries.size());

        return getSheet(date);
    }

    @Transactional(readOnly = true)
    public TeacherMonthlySummary getMonthlySummary(Long teacherId, int year, int month) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new NotFoundException("শিক্ষক পাওয়া যায়নি"));

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);

        List<TeacherAttendance> records = attendanceRepository.findByTeacherIdAndAttendanceDateBetween(teacherId, start, end);
        List<Holiday> holidays = holidayRepository.findByHolidayDateBetweenOrderByHolidayDate(start, end);

        Set<LocalDate> holidayDates = new HashSet<>();
        for (int i = 0; i < holidays.size(); i++) {
            holidayDates.add(holidays.get(i).getHolidayDate());
        }

        int totalDays = end.getDayOfMonth();
        int workingDays = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            // শুক্রবার ও স্কুল ছুটির দিন কাজের দিন নয়
            if (d.getDayOfWeek() != DayOfWeek.FRIDAY && !holidayDates.contains(d)) {
                workingDays++;
            }
        }

        int presentDays = 0;
        int lateDays = 0;
        int absentDays = 0;
        int leaveDays = 0;
        int halfDayDays = 0;

        for (int i = 0; i < records.size(); i++) {
            TeacherAttendance att = records.get(i);
            switch (att.getStatus()) {
                case PRESENT -> presentDays++;
                case LATE -> lateDays++;
                case ABSENT -> absentDays++;
                case LEAVE -> leaveDays++;
                case HALF_DAY -> halfDayDays++;
            }
        }

        BigDecimal attendancePercentage;
        if (workingDays == 0) {
            attendancePercentage = BigDecimal.valueOf(100.00).setScale(2, RoundingMode.HALF_UP);
        } else {
            BigDecimal totalWorking = BigDecimal.valueOf(workingDays);
            BigDecimal attended = BigDecimal.valueOf(presentDays + lateDays)
                    .add(BigDecimal.valueOf(halfDayDays).multiply(BigDecimal.valueOf(0.5)));
            attendancePercentage = attended.multiply(BigDecimal.valueOf(100))
                    .divide(totalWorking, 2, RoundingMode.HALF_UP);
        }

        return new TeacherMonthlySummary(
                teacher.getId(),
                teacher.getEmployeeId(),
                teacher.getFullName(),
                year,
                month,
                totalDays,
                workingDays,
                presentDays,
                lateDays,
                absentDays,
                leaveDays,
                halfDayDays,
                attendancePercentage);
    }
}
