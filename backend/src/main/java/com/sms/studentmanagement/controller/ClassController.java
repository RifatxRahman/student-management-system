package com.sms.studentmanagement.controller;

import com.sms.studentmanagement.dto.AcademicDtos.AssignSubjectRequest;
import com.sms.studentmanagement.dto.AcademicDtos.ClassRequest;
import com.sms.studentmanagement.dto.AcademicDtos.ClassResponse;
import com.sms.studentmanagement.dto.AcademicDtos.SectionRequest;
import com.sms.studentmanagement.dto.AcademicDtos.SectionResponse;
import com.sms.studentmanagement.dto.AcademicDtos.SubjectResponse;
import com.sms.studentmanagement.service.ClassService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class ClassController {

    private final ClassService service;

    @GetMapping
    public List<ClassResponse> list() {
        return service.listClasses();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClassResponse create(@Valid @RequestBody ClassRequest request) {
        return service.createClass(request.name(), request.displayOrder());
    }

    @PutMapping("/{id}")
    public ClassResponse update(@PathVariable Long id, @Valid @RequestBody ClassRequest request) {
        return service.updateClass(id, request.name(), request.displayOrder());
    }

    @PostMapping("/{id}/activate")
    public ClassResponse activate(@PathVariable Long id) {
        return service.setClassActive(id, true);
    }

    @PostMapping("/{id}/deactivate")
    public ClassResponse deactivate(@PathVariable Long id) {
        return service.setClassActive(id, false);
    }

    // ---------- Section ----------
    @GetMapping("/{id}/sections")
    public List<SectionResponse> sections(@PathVariable Long id) {
        return service.listSections(id);
    }

    @PostMapping("/{id}/sections")
    @ResponseStatus(HttpStatus.CREATED)
    public SectionResponse createSection(
            @PathVariable Long id, @Valid @RequestBody SectionRequest request) {
        return service.createSection(id, request.name());
    }

    // ---------- Class-Subject ----------
    @GetMapping("/{id}/subjects")
    public List<SubjectResponse> subjects(@PathVariable Long id) {
        return service.listClassSubjects(id);
    }

    @PostMapping("/{id}/subjects")
    @ResponseStatus(HttpStatus.CREATED)
    public SubjectResponse assignSubject(
            @PathVariable Long id, @Valid @RequestBody AssignSubjectRequest request) {
        return service.assignSubject(id, request.subjectId());
    }

    @DeleteMapping("/{id}/subjects/{subjectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeSubject(@PathVariable Long id, @PathVariable Long subjectId) {
        service.removeSubject(id, subjectId);
    }
}