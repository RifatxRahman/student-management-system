package com.sms.studentmanagement.controller;

import com.sms.studentmanagement.dto.PeopleDtos.EnrollmentResponse;
import com.sms.studentmanagement.dto.PeopleDtos.TransferRequest;
import com.sms.studentmanagement.dto.PeopleDtos.WithdrawRequest;
import com.sms.studentmanagement.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService service;

    @PostMapping("/{id}/transfer")
    public EnrollmentResponse transfer(@PathVariable Long id, @Valid @RequestBody TransferRequest request) {
        return service.transfer(id, request.classId(), request.sectionId(), request.rollNumber(), request.effectiveDate());
    }

    @PostMapping("/{id}/withdraw")
    public EnrollmentResponse withdraw(@PathVariable Long id, @RequestBody(required = false) WithdrawRequest request) {
        return service.withdraw(id, request == null ? null : request.endDate());
    }
}