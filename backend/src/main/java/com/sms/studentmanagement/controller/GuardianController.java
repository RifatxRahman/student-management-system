package com.sms.studentmanagement.controller;

import com.sms.studentmanagement.dto.PeopleDtos.GuardianDetail;
import com.sms.studentmanagement.dto.PeopleDtos.GuardianRequest;
import com.sms.studentmanagement.dto.PeopleDtos.GuardianResponse;
import com.sms.studentmanagement.service.GuardianService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guardians")
@RequiredArgsConstructor
public class GuardianController {

    private final GuardianService service;

    @GetMapping
    public List<GuardianResponse> search(@RequestParam String q) {
        return service.search(q);
    }

    @GetMapping("/{id}")
    public GuardianDetail get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GuardianResponse create(@Valid @RequestBody GuardianRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public GuardianResponse update(@PathVariable Long id, @Valid @RequestBody GuardianRequest request) {
        return service.update(id, request);
    }
}