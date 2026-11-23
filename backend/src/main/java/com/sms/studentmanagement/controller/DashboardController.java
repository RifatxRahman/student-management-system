package com.sms.studentmanagement.controller;

import com.sms.studentmanagement.dto.DashboardDtos.DashboardSummaryResponse;
import com.sms.studentmanagement.security.Actor;
import com.sms.studentmanagement.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService service;

    @GetMapping("/summary")
    public DashboardSummaryResponse summary(Authentication authentication) {
        return service.getSummary(Actor.from(authentication));
    }
}
