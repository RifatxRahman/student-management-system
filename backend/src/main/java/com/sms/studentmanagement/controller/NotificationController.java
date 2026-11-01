package com.sms.studentmanagement.controller;

import com.sms.studentmanagement.dto.NotificationDtos.DispatchResult;
import com.sms.studentmanagement.dto.NotificationDtos.NotificationResponse;
import com.sms.studentmanagement.dto.PageResponse;
import com.sms.studentmanagement.entity.NotificationStatus;
import com.sms.studentmanagement.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** শুধু Admin (/api/admin/** নিয়মে)। */
@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @GetMapping
    public PageResponse<NotificationResponse> list(
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(status, page, size);
    }

    @PostMapping("/dispatch")
    public DispatchResult dispatch() {
        return service.dispatchPending();
    }

    @PostMapping("/{id}/retry")
    public NotificationResponse retry(@PathVariable Long id) {
        return service.retry(id);
    }
}