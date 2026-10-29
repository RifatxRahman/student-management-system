package com.sms.studentmanagement.controller;

import com.sms.studentmanagement.dto.AcademicDtos.SectionRequest;
import com.sms.studentmanagement.dto.AcademicDtos.SectionResponse;
import com.sms.studentmanagement.service.ClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sections")
@RequiredArgsConstructor
public class SectionController {

    private final ClassService service;

    @PutMapping("/{id}")
    public SectionResponse update(@PathVariable Long id, @Valid @RequestBody SectionRequest request) {
        return service.updateSection(id, request.name());
    }

    @PostMapping("/{id}/activate")
    public SectionResponse activate(@PathVariable Long id) {
        return service.setSectionActive(id, true);
    }

    @PostMapping("/{id}/deactivate")
    public SectionResponse deactivate(@PathVariable Long id) {
        return service.setSectionActive(id, false);
    }
}