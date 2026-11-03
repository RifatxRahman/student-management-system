package com.sms.studentmanagement.controller;

import com.sms.studentmanagement.config.InstitutionProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/institution")
@RequiredArgsConstructor
public class InstitutionController {

    private final InstitutionProperties properties;

    public record InstitutionResponse(String name, String address, String phone) {
    }

    // লগইন করা যে কেউ দেখতে পারে (প্রতিষ্ঠানের নাম গোপন কিছু নয়)
    @GetMapping
    public InstitutionResponse get() {
        return new InstitutionResponse(properties.getName(), properties.getAddress(), properties.getPhone());
    }
}