package com.sms.studentmanagement.controller;

import com.sms.studentmanagement.dto.StudentResultDtos.StudentResultSummary;
import com.sms.studentmanagement.service.StudentResultService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** শুধু Admin (/api/students/** নিয়মে)। শিক্ষার্থী ও অভিভাবকের নিজের দেখা Phase 4-এ। */
@RestController
@RequestMapping("/api/students/{id}/results")
@RequiredArgsConstructor
public class StudentResultController {

    private final StudentResultService service;

    @GetMapping
    public List<StudentResultSummary> history(@PathVariable Long id) {
        return service.history(id);
    }
}