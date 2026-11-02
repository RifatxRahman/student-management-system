package com.sms.studentmanagement.controller;

import com.sms.studentmanagement.dto.ExamDtos.ExamDetail;
import com.sms.studentmanagement.dto.ExamDtos.ExamRequest;
import com.sms.studentmanagement.dto.ExamDtos.ExamResponse;
import com.sms.studentmanagement.dto.ExamDtos.ExamSubjectRequest;
import com.sms.studentmanagement.dto.ExamDtos.ExamSubjectUpdateRequest;
import com.sms.studentmanagement.dto.ExamDtos.ExamUpdateRequest;
import com.sms.studentmanagement.service.ExamService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService service;

    // দেখা: লগইন করা যে কেউ (শিক্ষককেও মার্কস দিতে পরীক্ষার তালিকা দেখতে হবে)
    @GetMapping
    public List<ExamResponse> list(
            @RequestParam(required = false) Long sessionId, @RequestParam(required = false) Long classId) {
        return service.list(sessionId, classId);
    }

    @GetMapping("/{id}")
    public ExamDetail get(@PathVariable Long id) {
        return service.get(id);
    }

    // নিচের সবগুলো শুধু Admin (SecurityConfig-এর "বাকি সব" নিয়মে)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExamDetail create(@Valid @RequestBody ExamRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public ExamDetail update(@PathVariable Long id, @Valid @RequestBody ExamUpdateRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PostMapping("/{id}/subjects")
    @ResponseStatus(HttpStatus.CREATED)
    public ExamDetail addSubject(@PathVariable Long id, @Valid @RequestBody ExamSubjectRequest request) {
        return service.addSubject(id, request);
    }

    @PutMapping("/subjects/{examSubjectId}")
    public ExamDetail updateSubject(
            @PathVariable Long examSubjectId, @Valid @RequestBody ExamSubjectUpdateRequest request) {
        return service.updateSubject(examSubjectId, request);
    }

    @DeleteMapping("/subjects/{examSubjectId}")
    public ExamDetail removeSubject(@PathVariable Long examSubjectId) {
        return service.removeSubject(examSubjectId);
    }
}