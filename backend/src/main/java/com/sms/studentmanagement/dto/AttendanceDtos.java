package com.sms.studentmanagement.dto;

import com.sms.studentmanagement.entity.AttendanceStatus;
import com.sms.studentmanagement.entity.Enrollment;
import com.sms.studentmanagement.entity.Holiday;
import com.sms.studentmanagement.entity.Section;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class AttendanceDtos {

    private AttendanceDtos() {
    }

    // ---------- ছুটি ----------
    public record HolidayRequest(
            @NotNull LocalDate fromDate,
            LocalDate toDate,
            @NotBlank @Size(max = 100) String name) {
    }

    public record HolidayResponse(Long id, LocalDate date, String name) {

        public static HolidayResponse from(Holiday h) {
            return new HolidayResponse(h.getId(), h.getHolidayDate(), h.getName());
        }
    }

    // ---------- হাজিরা ----------
    public record SectionOption(Long sectionId, String sectionName, Long classId, String className) {

        public static SectionOption from(Section s) {
            return new SectionOption(
                    s.getId(), s.getName(), s.getSchoolClass().getId(), s.getSchoolClass().getName());
        }
    }

    public record SheetRow(
            Long enrollmentId, Long studentId, String studentCode, String studentName,
            int rollNumber, AttendanceStatus status, String remark) {

        public static SheetRow from(Enrollment e, AttendanceStatus status, String remark) {
            return new SheetRow(
                    e.getId(), e.getStudent().getId(), e.getStudent().getStudentCode(),
                    e.getStudent().getFullName(), e.getRollNumber(), status, remark);
        }
    }

    /** editable=false হলে lockReason-এ কারণ থাকে। alreadyMarked=true মানে এই দিনের হাজিরা আগে নেওয়া হয়েছে। */
    public record Sheet(
            Long sectionId, String sectionName, String className, LocalDate date,
            boolean editable, String lockReason, boolean alreadyMarked, List<SheetRow> rows) {
    }

    public record MarkEntry(
            @NotNull Long enrollmentId,
            @NotNull AttendanceStatus status,
            @Size(max = 255) String remark) {
    }

    public record MarkRequest(
            @NotNull Long sectionId,
            @NotNull LocalDate date,
            @NotEmpty @Size(max = 200) List<@Valid MarkEntry> entries,
            @Size(max = 500) String reason) {
    }

    public record MarkResult(int created, int changed, int unchanged) {
    }

    // ---------- রিপোর্ট ----------
    public record StudentSummary(
            Long enrollmentId, Long studentId, String studentCode, String studentName, int rollNumber,
            long present, long late, long absent, long leave, BigDecimal percentage) {
    }

    /** workingDays = মাসের কর্মদিবস (আজ পর্যন্ত)। markedDays = যত দিন এই সেকশনে হাজিরা নেওয়া হয়েছে। */
    public record MonthlyReport(
            Long sectionId, String sectionName, String className, String month,
            long workingDays, long markedDays, List<StudentSummary> students) {
    }
}