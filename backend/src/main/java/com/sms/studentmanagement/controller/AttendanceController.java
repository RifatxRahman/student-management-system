package com.sms.studentmanagement.controller;

import com.sms.studentmanagement.dto.AttendanceDtos.DailySummary;
import com.sms.studentmanagement.dto.AttendanceDtos.LowAttendanceReport;
import com.sms.studentmanagement.dto.AttendanceDtos.MarkRequest;
import com.sms.studentmanagement.dto.AttendanceDtos.MarkResult;
import com.sms.studentmanagement.dto.AttendanceDtos.MonthlyReport;
import com.sms.studentmanagement.dto.AttendanceDtos.SectionOption;
import com.sms.studentmanagement.dto.AttendanceDtos.Sheet;
import com.sms.studentmanagement.dto.AttendanceDtos.StudentHistory;
import com.sms.studentmanagement.security.Actor;
import com.sms.studentmanagement.service.AttendanceReportService;
import com.sms.studentmanagement.service.AttendanceService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Admin ও Teacher দুজনেই ঢুকতে পারেন। কে কোন সেকশন পাবেন, সেটা Service ঠিক করে। */
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService service;
    private final AttendanceReportService reportService;

    @GetMapping("/my-sections")
    public List<SectionOption> mySections(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.mySections(Actor.from(authentication), date);
    }

    @GetMapping("/sheet")
    public Sheet sheet(
            Authentication authentication,
            @RequestParam Long sectionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.sheet(Actor.from(authentication), sectionId, date);
    }

    @PostMapping("/mark")
    public MarkResult mark(Authentication authentication, @Valid @RequestBody MarkRequest request) {
        return service.mark(Actor.from(authentication), request);
    }

    @PostMapping("/finalize")
    public ResponseEntity<Void> finalizeAttendance(
            Authentication authentication,
            @RequestParam Long sectionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        service.finalizeAttendance(Actor.from(authentication), sectionId, date);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/reports/monthly")
    public MonthlyReport monthly(
            Authentication authentication, @RequestParam Long sectionId, @RequestParam String month) {
        return reportService.monthly(Actor.from(authentication), sectionId, month);
    }

    @GetMapping("/reports/daily")
    public DailySummary daily(
            Authentication authentication,
            @RequestParam Long sectionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return reportService.dailySummary(Actor.from(authentication), sectionId, date);
    }

    @GetMapping("/reports/student")
    public StudentHistory studentHistory(
            Authentication authentication,
            @RequestParam Long enrollmentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.studentHistory(Actor.from(authentication), enrollmentId, from, to);
    }

    @GetMapping("/reports/low-attendance")
    public LowAttendanceReport lowAttendance(
            Authentication authentication,
            @RequestParam Long sectionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "75") int threshold) {
        return reportService.lowAttendance(
                Actor.from(authentication), sectionId, from, to, threshold);
    }
}