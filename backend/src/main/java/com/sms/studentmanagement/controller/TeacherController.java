package com.sms.studentmanagement.controller;

import com.sms.studentmanagement.dto.PeopleDtos.AssignmentRequest;
import com.sms.studentmanagement.dto.PeopleDtos.AssignmentResponse;
import com.sms.studentmanagement.dto.PeopleDtos.TeacherRequest;
import com.sms.studentmanagement.dto.PeopleDtos.TeacherResponse;
import com.sms.studentmanagement.service.TeacherService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService service;

    // ---------- শিক্ষক নিজের জন্য (লগইন করা যে কেউ, নিজের তথ্য শুধু) ----------
    @GetMapping("/me")
    public TeacherResponse me(@AuthenticationPrincipal Jwt jwt) {
        return service.me(Long.valueOf(jwt.getSubject()));
    }

    @GetMapping("/me/assignments")
    public List<AssignmentResponse> myAssignments(
            @AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) Long sessionId) {
        return service.myAssignments(Long.valueOf(jwt.getSubject()), sessionId);
    }

    // ---------- Admin-এর জন্য ----------
    @GetMapping
    public List<TeacherResponse> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeacherResponse create(@Valid @RequestBody TeacherRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public TeacherResponse update(@PathVariable Long id, @Valid @RequestBody TeacherRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/activate")
    public TeacherResponse activate(@PathVariable Long id) {
        return service.setActive(id, true);
    }

    @PostMapping("/{id}/deactivate")
    public TeacherResponse deactivate(@PathVariable Long id) {
        return service.setActive(id, false);
    }

    @GetMapping("/{id}/assignments")
    public List<AssignmentResponse> assignments(
            @PathVariable Long id, @RequestParam(required = false) Long sessionId) {
        return service.assignments(id, sessionId);
    }

    @PostMapping("/{id}/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    public AssignmentResponse addAssignment(
            @PathVariable Long id, @Valid @RequestBody AssignmentRequest request) {
        return service.addAssignment(id, request);
    }

    @DeleteMapping("/assignments/{assignmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeAssignment(@PathVariable Long assignmentId) {
        service.removeAssignment(assignmentId);
    }
}