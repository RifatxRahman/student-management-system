package com.sms.studentmanagement.dto;

import com.sms.studentmanagement.entity.LeaveApplication;
import com.sms.studentmanagement.entity.LeaveStatus;
import com.sms.studentmanagement.entity.LeaveType;
import com.sms.studentmanagement.entity.TeacherAttendanceStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class TeacherAttendanceDtos {

    private TeacherAttendanceDtos() {
    }

    public record TeacherAttendanceEntry(
            @NotNull Long teacherId,
            @NotNull TeacherAttendanceStatus status,
            LocalTime inTime,
            LocalTime outTime,
            @Size(max = 255) String remark) {
    }

    public record TeacherAttendanceSheetRequest(
            @NotNull LocalDate date,
            @NotEmpty List<TeacherAttendanceEntry> entries) {
    }

    public record TeacherAttendanceRow(
            Long teacherId,
            String employeeId,
            String teacherName,
            String designation,
            TeacherAttendanceStatus status,
            LocalTime inTime,
            LocalTime outTime,
            String remark,
            boolean hasApprovedLeave,
            LeaveType leaveType) {
    }

    public record TeacherAttendanceSheetResponse(
            LocalDate date,
            List<TeacherAttendanceRow> rows,
            int totalTeachers,
            int presentCount,
            int lateCount,
            int absentCount,
            int leaveCount,
            int halfDayCount) {
    }

    public record TeacherMonthlySummary(
            Long teacherId,
            String employeeId,
            String teacherName,
            int year,
            int month,
            int totalDays,
            int workingDays,
            int presentDays,
            int lateDays,
            int absentDays,
            int leaveDays,
            int halfDayDays,
            BigDecimal attendancePercentage) {
    }

    public record LeaveApplicationRequest(
            Long teacherId,
            @NotNull LeaveType leaveType,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            @DecimalMin("0.5") BigDecimal totalDays,
            @NotBlank @Size(max = 500) String reason) {
    }

    public record LeaveApplicationResponse(
            Long id,
            Long teacherId,
            String employeeId,
            String teacherName,
            String designation,
            LeaveType leaveType,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal totalDays,
            String reason,
            LeaveStatus status,
            Instant appliedAt,
            Long actionBy,
            String actionByName,
            Instant actionAt,
            String actionReason) {

        public static LeaveApplicationResponse from(LeaveApplication l, String actionByName) {
            return new LeaveApplicationResponse(
                    l.getId(),
                    l.getTeacher().getId(),
                    l.getTeacher().getEmployeeId(),
                    l.getTeacher().getFullName(),
                    l.getTeacher().getDesignation(),
                    l.getLeaveType(),
                    l.getStartDate(),
                    l.getEndDate(),
                    l.getTotalDays(),
                    l.getReason(),
                    l.getStatus(),
                    l.getAppliedAt(),
                    l.getActionBy(),
                    actionByName,
                    l.getActionAt(),
                    l.getActionReason());
        }
    }

    public record LeaveActionRequest(
            @Size(max = 255) String reason) {
    }
}
