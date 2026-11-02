package com.sms.studentmanagement.controller;

import com.sms.studentmanagement.dto.ResultDtos.PublishResult;
import com.sms.studentmanagement.dto.ResultDtos.ResultPreview;
import com.sms.studentmanagement.dto.ResultDtos.ResultRow;
import com.sms.studentmanagement.security.Actor;
import com.sms.studentmanagement.service.ResultService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ResultController {

    private final ResultService service;

    // preview ও publish শুধু Admin (SecurityConfig-এ তালা)
    @GetMapping("/{id}/preview")
    public ResultPreview preview(@PathVariable Long id) {
        return service.preview(id);
    }

    @PostMapping("/{id}/publish")
    public PublishResult publish(@PathVariable Long id) {
        return service.publish(id);
    }

    // প্রকাশিত ফল: Admin সব সেকশন, শিক্ষক শুধু নিজের সেকশন (Service যাচাই করে)
    @GetMapping("/{id}/results")
    public List<ResultRow> results(
            Authentication authentication, @PathVariable Long id, @RequestParam(required = false) Long sectionId) {
        return service.results(Actor.from(authentication), id, sectionId);
    }
}