package com.sms.studentmanagement.controller;

import com.sms.studentmanagement.dto.TeacherAttendanceDtos.TeacherAttendanceSheetRequest;
import com.sms.studentmanagement.dto.TeacherAttendanceDtos.TeacherAttendanceSheetResponse;
import com.sms.studentmanagement.dto.TeacherAttendanceDtos.TeacherMonthlySummary;
import com.sms.studentmanagement.entity.Teacher;
import com.sms.studentmanagement.exception.NotFoundException;
import com.sms.studentmanagement.repository.TeacherRepository;
import com.sms.studentmanagement.service.TeacherAttendanceService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teacher-attendance")
@RequiredArgsConstructor
public class TeacherAttendanceController {

    private final TeacherAttendanceService attendanceService;
    private final TeacherRepository teacherRepository;

    @GetMapping("/sheet")
    public TeacherAttendanceSheetResponse getSheet(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return attendanceService.getSheet(targetDate);
    }

    @PostMapping("/sheet")
    public TeacherAttendanceSheetResponse saveSheet(
            @Valid @RequestBody TeacherAttendanceSheetRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        return attendanceService.saveSheet(request, userId);
    }

    @GetMapping("/monthly")
    public TeacherMonthlySummary getMonthlySummary(
            @RequestParam Long teacherId,
            @RequestParam int year,
            @RequestParam int month) {
        return attendanceService.getMonthlySummary(teacherId, year, month);
    }

    @GetMapping("/me")
    public TeacherMonthlySummary getMyMonthlySummary(
            @RequestParam int year,
            @RequestParam int month,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        Teacher teacher = teacherRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("আপনার শিক্ষক প্রোফাইল পাওয়া যায়নি"));
        return attendanceService.getMonthlySummary(teacher.getId(), year, month);
    }
}
