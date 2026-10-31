package com.sms.studentmanagement.service;

import com.sms.studentmanagement.dto.AttendanceCountRow;
import com.sms.studentmanagement.dto.AttendanceDtos.MonthlyReport;
import com.sms.studentmanagement.dto.AttendanceDtos.StudentSummary;
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
        for (AttendanceCountRow row : attendanceRepository.countByEnrollmentAndStatus(sectionId, first, last)) {
            counts.computeIfAbsent(row.enrollmentId(), k -> new EnumMap<>(AttendanceStatus.class))
                    .put(row.status(), row.count());
        }

        List<StudentSummary> students = enrollmentRepository.findInSection(sectionId, first, last).stream()
                .map(e -> summary(e, counts.getOrDefault(e.getId(), Map.of())))
                .toList();

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

    private static YearMonth parseMonth(String text) {
        try {
            return YearMonth.parse(text == null ? "" : text.trim());
        } catch (DateTimeParseException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "মাস লিখতে হবে এই ধরনে: 2026-10");
        }
    }
}