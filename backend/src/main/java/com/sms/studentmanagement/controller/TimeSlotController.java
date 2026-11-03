package com.sms.studentmanagement.controller;

import com.sms.studentmanagement.dto.RoutineDtos.TimeSlotRequest;
import com.sms.studentmanagement.dto.RoutineDtos.TimeSlotResponse;
import com.sms.studentmanagement.service.TimeSlotService;
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
@RequestMapping("/api/time-slots")
@RequiredArgsConstructor
public class TimeSlotController {

    private final TimeSlotService service;

    @GetMapping
    public List<TimeSlotResponse> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TimeSlotResponse create(@Valid @RequestBody TimeSlotRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public TimeSlotResponse update(@PathVariable Long id, @Valid @RequestBody TimeSlotRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}