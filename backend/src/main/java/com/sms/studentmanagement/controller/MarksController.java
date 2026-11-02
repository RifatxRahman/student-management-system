package com.sms.studentmanagement.controller;

import com.sms.studentmanagement.dto.MarkDtos.MarkSheet;
import com.sms.studentmanagement.dto.MarkDtos.MarkTarget;
import com.sms.studentmanagement.dto.MarkDtos.SaveMarksRequest;
import com.sms.studentmanagement.dto.MarkDtos.SaveMarksResult;
import com.sms.studentmanagement.security.Actor;
import com.sms.studentmanagement.service.MarksService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Admin ও Teacher ঢুকতে পারেন। কোন সেকশন-বিষয়ে পারবেন, সেটা Service যাচাই করে। */
@RestController
@RequestMapping("/api/marks")
@RequiredArgsConstructor
public class MarksController {

    private final MarksService service;

    @GetMapping("/targets")
    public List<MarkTarget> targets(Authentication authentication, @RequestParam Long examId) {
        return service.targets(Actor.from(authentication), examId);
    }

    @GetMapping("/sheet")
    public MarkSheet sheet(
            Authentication authentication,
            @RequestParam Long examId,
            @RequestParam Long sectionId,
            @RequestParam Long subjectId) {
        return service.sheet(Actor.from(authentication), examId, sectionId, subjectId);
    }

    @PostMapping("/save")
    public SaveMarksResult save(Authentication authentication, @Valid @RequestBody SaveMarksRequest request) {
        return service.save(Actor.from(authentication), request);
    }
}