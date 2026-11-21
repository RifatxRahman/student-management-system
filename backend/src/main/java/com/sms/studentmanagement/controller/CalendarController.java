package com.sms.studentmanagement.controller;

import com.sms.studentmanagement.dto.AcademicEventDtos.CalendarMonthResponse;
import com.sms.studentmanagement.service.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    @GetMapping("/month")
    public CalendarMonthResponse getMonth(
            @RequestParam int year,
            @RequestParam int month) {
        return calendarService.getMonthView(year, month);
    }
}
