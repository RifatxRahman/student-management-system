package com.sms.studentmanagement.controller;

import com.sms.studentmanagement.dto.TeacherAttendanceDtos.LeaveActionRequest;
import com.sms.studentmanagement.dto.TeacherAttendanceDtos.LeaveApplicationRequest;
import com.sms.studentmanagement.dto.TeacherAttendanceDtos.LeaveApplicationResponse;
import com.sms.studentmanagement.entity.LeaveStatus;
import com.sms.studentmanagement.service.LeaveApplicationService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class LeaveApplicationController {

    private final LeaveApplicationService leaveService;

    @GetMapping
    public List<LeaveApplicationResponse> listAll(@RequestParam(required = false) LeaveStatus status) {
        return leaveService.listAll(status);
    }

    @GetMapping("/me")
    public List<LeaveApplicationResponse> listMyLeaves(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        return leaveService.listMyLeaves(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveApplicationResponse applyAsAdmin(
            @Valid @RequestBody LeaveApplicationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        return leaveService.apply(request, userId, true);
    }

    @PostMapping("/me")
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveApplicationResponse applyAsTeacher(
            @Valid @RequestBody LeaveApplicationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        return leaveService.apply(request, userId, false);
    }

    @PostMapping("/{id}/approve")
    public LeaveApplicationResponse approve(
            @PathVariable Long id,
            @RequestBody(required = false) LeaveActionRequest action,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        String note = action != null ? action.reason() : null;
        return leaveService.approve(id, userId, note);
    }

    @PostMapping("/{id}/reject")
    public LeaveApplicationResponse reject(
            @PathVariable Long id,
            @Valid @RequestBody LeaveActionRequest action,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        return leaveService.reject(id, userId, action.reason());
    }

    @PostMapping("/{id}/cancel")
    public LeaveApplicationResponse cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        boolean isAdmin = "ADMIN".equalsIgnoreCase(jwt.getClaimAsString("role"));
        return leaveService.cancel(id, userId, isAdmin);
    }
}
