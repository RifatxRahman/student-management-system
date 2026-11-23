package com.sms.studentmanagement.controller;

import com.sms.studentmanagement.dto.AcademicEventDtos.AcademicEventRequest;
import com.sms.studentmanagement.dto.AcademicEventDtos.AcademicEventResponse;
import com.sms.studentmanagement.service.AcademicEventService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/academic-events")
@RequiredArgsConstructor
public class AcademicEventController {

    private final AcademicEventService eventService;

    @GetMapping
    public List<AcademicEventResponse> list(
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return eventService.list(sessionId, from, to);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AcademicEventResponse create(@Valid @RequestBody AcademicEventRequest request) {
        return eventService.create(request);
    }

    @PutMapping("/{id}")
    public AcademicEventResponse update(
            @PathVariable Long id,
            @Valid @RequestBody AcademicEventRequest request) {
        return eventService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        eventService.delete(id);
    }
}
