package com.sms.studentmanagement.controller;

import com.sms.studentmanagement.dto.PageResponse;
import com.sms.studentmanagement.dto.PeopleDtos.AuditLogResponse;
import com.sms.studentmanagement.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditService auditService;

    @GetMapping
    public PageResponse<AuditLogResponse> list(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return auditService.search(entityType, entityId, page, size);
    }
}