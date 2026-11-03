package com.sms.studentmanagement.controller;

import com.sms.studentmanagement.dto.RoutineDtos.TimetableRequest;
import com.sms.studentmanagement.dto.RoutineDtos.TimetableResponse;
import com.sms.studentmanagement.dto.RoutineDtos.TimetableUpdateRequest;
import com.sms.studentmanagement.security.Actor;
import com.sms.studentmanagement.service.TimetableService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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
@RequestMapping("/api/timetable")
@RequiredArgsConstructor
public class TimetableController {

    private final TimetableService service;

    // দেখা: লগইন করা যে কেউ। sessionId না দিলে সক্রিয় সেশন।
    @GetMapping("/sections/{sectionId}")
    public List<TimetableResponse> section(
            @PathVariable Long sectionId, @RequestParam(required = false) Long sessionId) {
        return service.sectionRoutine(sectionId, sessionId);
    }

    @GetMapping("/teachers/{teacherId}")
    public List<TimetableResponse> teacher(
            @PathVariable Long teacherId, @RequestParam(required = false) Long sessionId) {
        return service.teacherRoutine(teacherId, sessionId);
    }

    @GetMapping("/me")
    public List<TimetableResponse> me(Authentication authentication, @RequestParam(required = false) Long sessionId) {
        return service.myRoutine(Actor.from(authentication), sessionId);
    }

    // বদলানো: শুধু Admin
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TimetableResponse create(@Valid @RequestBody TimetableRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public TimetableResponse update(@PathVariable Long id, @Valid @RequestBody TimetableUpdateRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}