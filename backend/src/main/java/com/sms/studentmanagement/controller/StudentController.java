package com.sms.studentmanagement.controller;

import com.sms.studentmanagement.dto.PageResponse;
import com.sms.studentmanagement.dto.PeopleDtos.EnrollRequest;
import com.sms.studentmanagement.dto.PeopleDtos.EnrollmentResponse;
import com.sms.studentmanagement.dto.PeopleDtos.LinkGuardianRequest;
import com.sms.studentmanagement.dto.PeopleDtos.StatusRequest;
import com.sms.studentmanagement.dto.PeopleDtos.StudentCreateRequest;
import com.sms.studentmanagement.dto.PeopleDtos.StudentDetail;
import com.sms.studentmanagement.dto.PeopleDtos.StudentSummary;
import com.sms.studentmanagement.dto.PeopleDtos.StudentUpdateRequest;
import com.sms.studentmanagement.entity.StudentStatus;
import com.sms.studentmanagement.service.EnrollmentService;
import com.sms.studentmanagement.service.StudentService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final EnrollmentService enrollmentService;

    @GetMapping
    public PageResponse<StudentSummary> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) StudentStatus status,
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return studentService.search(q, status, sessionId, classId, sectionId, page, size);
    }

    @GetMapping("/deleted")
    public List<StudentSummary> deleted() {
        return studentService.listDeleted();
    }

    @GetMapping("/{id}")
    public StudentDetail get(@PathVariable Long id) {
        return studentService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentDetail create(@Valid @RequestBody StudentCreateRequest request) {
        return studentService.create(request);
    }

    @PutMapping("/{id}")
    public StudentDetail update(@PathVariable Long id, @Valid @RequestBody StudentUpdateRequest request) {
        return studentService.update(id, request);
    }

    @PostMapping("/{id}/status")
    public StudentDetail changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        return studentService.changeStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        studentService.softDelete(id);
    }

    @PostMapping("/{id}/restore")
    public StudentDetail restore(@PathVariable Long id) {
        return studentService.restore(id);
    }

    // ---------- অভিভাবক ----------
    @PostMapping("/{id}/guardians")
    @ResponseStatus(HttpStatus.CREATED)
    public StudentDetail linkGuardian(@PathVariable Long id, @Valid @RequestBody LinkGuardianRequest request) {
        return studentService.linkGuardian(id, request.guardianId(), request.relation(), request.primary());
    }

    @DeleteMapping("/{id}/guardians/{guardianId}")
    public StudentDetail unlinkGuardian(@PathVariable Long id, @PathVariable Long guardianId) {
        return studentService.unlinkGuardian(id, guardianId);
    }

    @PostMapping("/{id}/guardians/{guardianId}/primary")
    public StudentDetail setPrimary(@PathVariable Long id, @PathVariable Long guardianId) {
        return studentService.setPrimaryGuardian(id, guardianId);
    }

    // ---------- ভর্তি ----------
    @PostMapping("/{id}/enrollments")
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentResponse enroll(@PathVariable Long id, @Valid @RequestBody EnrollRequest request) {
        return enrollmentService.enroll(
                id, request.sessionId(), request.classId(), request.sectionId(),
                request.rollNumber(), request.startDate());
    }
}