package com.sms.studentmanagement.controller;

import com.sms.studentmanagement.dto.AcademicDtos.SessionRequest;
import com.sms.studentmanagement.dto.AcademicDtos.SessionResponse;
import com.sms.studentmanagement.service.AcademicSessionService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final AcademicSessionService service;

    @GetMapping
    public List<SessionResponse> list() {
        return service.list();
    }

    @GetMapping("/active")
    public SessionResponse active() {
        return service.getActive();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse create(@Valid @RequestBody SessionRequest request) {
        return service.create(request.name(), request.startDate(), request.endDate());
    }

    @PutMapping("/{id}")
    public SessionResponse update(@PathVariable Long id, @Valid @RequestBody SessionRequest request) {
        return service.update(id, request.name(), request.startDate(), request.endDate());
    }

    @PostMapping("/{id}/activate")
    public SessionResponse activate(@PathVariable Long id) {
        return service.activate(id);
    }

    @PostMapping("/{id}/deactivate")
    public SessionResponse deactivate(@PathVariable Long id) {
        return service.deactivate(id);
    }
}