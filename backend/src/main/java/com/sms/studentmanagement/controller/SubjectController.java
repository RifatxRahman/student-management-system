package com.sms.studentmanagement.controller;

import com.sms.studentmanagement.dto.AcademicDtos.SubjectRequest;
import com.sms.studentmanagement.dto.AcademicDtos.SubjectResponse;
import com.sms.studentmanagement.service.SubjectService;
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
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService service;

    @GetMapping
    public List<SubjectResponse> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubjectResponse create(@Valid @RequestBody SubjectRequest request) {
        return service.create(request.name(), request.code());
    }

    @PutMapping("/{id}")
    public SubjectResponse update(@PathVariable Long id, @Valid @RequestBody SubjectRequest request) {
        return service.update(id, request.name(), request.code());
    }

    @PostMapping("/{id}/activate")
    public SubjectResponse activate(@PathVariable Long id) {
        return service.setActive(id, true);
    }

    @PostMapping("/{id}/deactivate")
    public SubjectResponse deactivate(@PathVariable Long id) {
        return service.setActive(id, false);
    }
}