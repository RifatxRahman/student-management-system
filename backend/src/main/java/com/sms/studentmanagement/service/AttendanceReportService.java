package com.sms.studentmanagement.service;

import com.sms.studentmanagement.dto.AttendanceCountRow;
import com.sms.studentmanagement.dto.AttendanceDtos.DailySummary;
import com.sms.studentmanagement.dto.AttendanceDtos.DayEntry;
import com.sms.studentmanagement.dto.AttendanceDtos.LowAttendanceReport;
import com.sms.studentmanagement.dto.AttendanceDtos.LowAttendanceRow;
import com.sms.studentmanagement.dto.AttendanceDtos.MonthlyReport;
import com.sms.studentmanagement.dto.AttendanceDtos.StudentHistory;
import com.sms.studentmanagement.dto.AttendanceDtos.StudentSummary;
import com.sms.studentmanagement.entity.Attendance;
import com.sms.studentmanagement.entity.AttendanceStatus;
import com.sms.studentmanagement.entity.Enrollment;
import com.sms.studentmanagement.entity.Section;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.exception.NotFoundException;
import com.sms.studentmanagement.repository.AttendanceRepository;
import com.sms.studentmanagement.repository.EnrollmentRepository;
import com.sms.studentmanagement.repository.SectionRepository;
import com.sms.studentmanagement.security.Actor;
import com.sms.studentmanagement.util.AttendanceMath;
import com.sms.studentmanagement.util.DhakaTime;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AttendanceReportService {

    private final SectionRepository sectionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceAccess access;
    private final AttendanceCalendar calendar;

    @Transactional(readOnly = true)
    public MonthlyReport monthly(Actor actor, Long sectionId, String monthText) {
        YearMonth month = parseMonth(monthText);
        LocalDate first = month.atDay(1);
        LocalDate last = month.atEndOfMonth();

        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new NotFoundException("সেকশন পাওয়া যায়নি"));
        access.requireSection(actor, section.getId(), first, last);

        Map<Long, Map<AttendanceStatus, Long>> counts = new HashMap<>();
        List<AttendanceCountRow> countRows = attendanceRepository.countByEnrollmentAndStatus(sectionId, first, last);
        for (int i = 0; i < countRows.size(); i++) {
            AttendanceCountRow row = countRows.get(i);
            counts.computeIfAbsent(row.enrollmentId(), k -> new EnumMap<>(AttendanceStatus.class))
                    .put(row.status(), row.count());
        }

        List<Enrollment> enrolledList = enrollmentRepository.findInSection(sectionId, first, last);
        List<StudentSummary> students = new ArrayList<>();
        for (int i = 0; i < enrolledList.size(); i++) {
            Enrollment e = enrolledList.get(i);
            students.add(summary(e, counts.getOrDefault(e.getId(), Map.of())));
        }

        // আজকের পরের দিনগুলো কর্মদিবসে গোনা হয় না
        LocalDate today = DhakaTime.today();
        LocalDate countUntil = last.isAfter(today) ? today : last;
        long workingDays = countUntil.isBefore(first) ? 0 : calendar.workingDays(first, countUntil);
        long markedDays = attendanceRepository.countMarkedDays(sectionId, first, last);

        return new MonthlyReport(
                section.getId(), section.getName(), section.getSchoolClass().getName(),
                month.toString(), workingDays, markedDays, students);
    }

    private StudentSummary summary(Enrollment e, Map<AttendanceStatus, Long> c) {
        long present = c.getOrDefault(AttendanceStatus.PRESENT, 0L);
        long late = c.getOrDefault(AttendanceStatus.LATE, 0L);
        long absent = c.getOrDefault(AttendanceStatus.ABSENT, 0L);
        long leave = c.getOrDefault(AttendanceStatus.LEAVE, 0L);
        return new StudentSummary(
                e.getId(), e.getStudent().getId(), e.getStudent().getStudentCode(),
                e.getStudent().getFullName(), e.getRollNumber(),
                present, late, absent, leave, AttendanceMath.percentage(present, late, absent));
    }

    // ==================== দৈনিক সারসংক্ষেপ ====================
    @Transactional(readOnly = true)
    public DailySummary dailySummary(Actor actor, Long sectionId, LocalDate date) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new NotFoundException("সেকশন পাওয়া যায়নি"));
        access.requireSection(actor, section.getId(), date, date);

        // ওই সেকশনে ওই দিন কতজন ছাত্র enrolled ছিল
        List<Enrollment> roster = enrollmentRepository.findInSection(sectionId, date, date);
        long total = roster.size();

        // হাজিরা থেকে গণনা
        List<AttendanceCountRow> counts = attendanceRepository
                .countByEnrollmentAndStatus(sectionId, date, date);

        long present = 0;
        long late = 0;
        long absent = 0;
        long leave = 0;
        for (int i = 0; i < counts.size(); i++) {
            AttendanceCountRow row = counts.get(i);
            switch (row.status()) {
                case PRESENT -> present += row.count();
                case LATE    -> late += row.count();
                case ABSENT  -> absent += row.count();
                case LEAVE   -> leave += row.count();
            }
        }

        BigDecimal percent = AttendanceMath.percentage(present, late, absent);

        return new DailySummary(
                section.getId(), section.getName(), section.getSchoolClass().getName(),
                date, total, present, late, absent, leave, percent);
    }

    // ==================== ছাত্রের দিনভিত্তিক ইতিহাস ====================
    @Transactional(readOnly = true)
    public StudentHistory studentHistory(Actor actor, Long enrollmentId,
                                         LocalDate from, LocalDate to) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new NotFoundException("এনরোলমেন্ট পাওয়া যায়নি"));

        access.requireSection(actor, enrollment.getSection().getId(), from, to);

        List<Attendance> records = attendanceRepository
                .findByEnrollmentAndRange(enrollmentId, from, to);

        List<DayEntry> days = new ArrayList<>();
        long present = 0;
        long late = 0;
        long absent = 0;
        long leave = 0;
        for (int i = 0; i < records.size(); i++) {
            Attendance a = records.get(i);
            days.add(new DayEntry(a.getAttendanceDate(), a.getStatus(), a.getRemark()));
            switch (a.getStatus()) {
                case PRESENT -> present++;
                case LATE    -> late++;
                case ABSENT  -> absent++;
                case LEAVE   -> leave++;
            }
        }

        BigDecimal percentage = AttendanceMath.percentage(present, late, absent);

        return new StudentHistory(
                enrollment.getStudent().getId(),
                enrollment.getStudent().getStudentCode(),
                enrollment.getStudent().getFullName(),
                enrollment.getSchoolClass().getName(),
                enrollment.getSection().getName(),
                enrollment.getRollNumber(),
                from, to,
                present, late, absent, leave, percentage, days);
    }

    // ==================== কম হাজিরার তালিকা ====================
    @Transactional(readOnly = true)
    public LowAttendanceReport lowAttendance(Actor actor, Long sectionId,
                                              LocalDate from, LocalDate to,
                                              int threshold) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new NotFoundException("সেকশন পাওয়া যায়নি"));
        access.requireSection(actor, section.getId(), from, to);

        // সব ছাত্রের status গণনা নিয়ে আসা (মাসিক রিপোর্টে যেমন করা হয়)
        Map<Long, Map<AttendanceStatus, Long>> counts = new HashMap<>();
        List<AttendanceCountRow> countRows = attendanceRepository
                .countByEnrollmentAndStatus(sectionId, from, to);
        for (int i = 0; i < countRows.size(); i++) {
            AttendanceCountRow row = countRows.get(i);
            counts.computeIfAbsent(row.enrollmentId(),
                            k -> new EnumMap<>(AttendanceStatus.class))
                    .put(row.status(), row.count());
        }

        List<Enrollment> roster = enrollmentRepository.findInSection(sectionId, from, to);
        List<LowAttendanceRow> lowStudents = new ArrayList<>();
        for (int i = 0; i < roster.size(); i++) {
            Enrollment e = roster.get(i);
            Map<AttendanceStatus, Long> c = counts.getOrDefault(e.getId(), Map.of());
            long p = c.getOrDefault(AttendanceStatus.PRESENT, 0L);
            long lt = c.getOrDefault(AttendanceStatus.LATE, 0L);
            long ab = c.getOrDefault(AttendanceStatus.ABSENT, 0L);
            long lv = c.getOrDefault(AttendanceStatus.LEAVE, 0L);
            BigDecimal pct = AttendanceMath.percentage(p, lt, ab);

            // threshold-এর নিচে হলে তালিকায় যোগ, null মানে কোনো হাজিরা নেই — সেটাও যোগ
            if (pct == null || pct.intValue() < threshold) {
                lowStudents.add(new LowAttendanceRow(
                        e.getId(), e.getStudent().getId(),
                        e.getStudent().getStudentCode(), e.getStudent().getFullName(),
                        e.getRollNumber(), p, lt, ab, lv, pct));
            }
        }

        return new LowAttendanceReport(
                section.getId(), section.getName(), section.getSchoolClass().getName(),
                from, to, threshold, lowStudents);
    }

    private static YearMonth parseMonth(String text) {
        try {
            return YearMonth.parse(text == null ? "" : text.trim());
        } catch (DateTimeParseException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "মাস লিখতে হবে এই ধরনে: 2026-10");
        }
    }
}