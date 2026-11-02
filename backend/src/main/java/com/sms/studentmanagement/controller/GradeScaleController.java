package com.sms.studentmanagement.controller;

import com.sms.studentmanagement.dto.GradeScaleDtos.GradeBandResponse;
import com.sms.studentmanagement.dto.GradeScaleDtos.GradeScaleRequest;
import com.sms.studentmanagement.service.GradeScaleService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/grade-scale")
@RequiredArgsConstructor
public class GradeScaleController {

    private final GradeScaleService service;

    // লগইন করা যে কেউ দেখতে পারে (ব্যক্তিগত তথ্য নয়)
    @GetMapping
    public List<GradeBandResponse> get() {
        return service.get();
    }

    // শুধু Admin (SecurityConfig-এর "বাকি সব" নিয়মে)
    @PutMapping
    public List<GradeBandResponse> replace(@Valid @RequestBody GradeScaleRequest request) {
        return service.replace(request);
    }
}