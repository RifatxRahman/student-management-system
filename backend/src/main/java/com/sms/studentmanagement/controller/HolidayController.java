package com.sms.studentmanagement.controller;

import com.sms.studentmanagement.dto.AttendanceDtos.HolidayRequest;
import com.sms.studentmanagement.dto.AttendanceDtos.HolidayResponse;
import com.sms.studentmanagement.service.HolidayService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/holidays")
@RequiredArgsConstructor
public class HolidayController {

    private final HolidayService service;

    // লগইন করা যে কেউ দেখতে পারে (ছুটির তালিকা ব্যক্তিগত তথ্য নয়)
    @GetMapping
    public List<HolidayResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.list(from, to);
    }

    // নিচের দুটো শুধু Admin (SecurityConfig-এর "বাকি সব" নিয়মে)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<HolidayResponse> create(@Valid @RequestBody HolidayRequest request) {
        return service.create(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}