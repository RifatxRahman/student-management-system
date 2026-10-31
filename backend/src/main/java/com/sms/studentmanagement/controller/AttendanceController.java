package com.sms.studentmanagement.controller;

import com.sms.studentmanagement.dto.AttendanceDtos.MarkRequest;
import com.sms.studentmanagement.dto.AttendanceDtos.MarkResult;
import com.sms.studentmanagement.dto.AttendanceDtos.MonthlyReport;
import com.sms.studentmanagement.dto.AttendanceDtos.SectionOption;
import com.sms.studentmanagement.dto.AttendanceDtos.Sheet;
import com.sms.studentmanagement.security.Actor;
import com.sms.studentmanagement.service.AttendanceReportService;
import com.sms.studentmanagement.service.AttendanceService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

    @GetMapping("/reports/monthly")
    public MonthlyReport monthly(
            Authentication authentication, @RequestParam Long sectionId, @RequestParam String month) {
        return reportService.monthly(Actor.from(authentication), sectionId, month);
    }
}