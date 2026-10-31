package com.sms.studentmanagement.service;

import com.sms.studentmanagement.dto.AttendanceDtos.HolidayRequest;
import com.sms.studentmanagement.dto.AttendanceDtos.HolidayResponse;
import com.sms.studentmanagement.entity.Holiday;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.exception.NotFoundException;
import com.sms.studentmanagement.repository.AttendanceRepository;
import com.sms.studentmanagement.repository.HolidayRepository;
import com.sms.studentmanagement.util.DhakaTime;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HolidayService {

    private static final int MAX_DAYS = 60;

    private final HolidayRepository holidayRepository;
    private final AttendanceRepository attendanceRepository;
    private final AuditService auditService;

    /** তারিখ না দিলে চলতি বছরের পুরো তালিকা। */
    @Transactional(readOnly = true)
    public List<HolidayResponse> list(LocalDate from, LocalDate to) {
        LocalDate start = from != null ? from : LocalDate.of(DhakaTime.today().getYear(), 1, 1);
        LocalDate end = to != null ? to : LocalDate.of(start.getYear(), 12, 31);
        if (end.isBefore(start)) {
            throw bad("শেষ তারিখ শুরুর আগে হতে পারে না");
        }
        return holidayRepository.findByHolidayDateBetweenOrderByHolidayDate(start, end).stream()
                .map(HolidayResponse::from)
                .toList();
    }

    /** toDate না দিলে শুধু একটা দিন। toDate দিলে মাঝের প্রতিটি দিন আলাদা সারি হয়। */
    @Transactional
    public List<HolidayResponse> create(HolidayRequest request) {
        LocalDate from = request.fromDate();
        LocalDate to = request.toDate() != null ? request.toDate() : from;
        if (to.isBefore(from)) {
            throw bad("শেষ তারিখ শুরুর আগে হতে পারে না");
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_DAYS) {
            throw bad("একবারে সর্বোচ্চ " + MAX_DAYS + " দিনের ছুটি দেওয়া যায়");
        }
        if (attendanceRepository.existsByAttendanceDateBetween(from, to)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "এই সময়ের কোনো দিনের হাজিরা নেওয়া হয়ে গেছে, তাই ছুটি দেওয়া যাচ্ছে না");
        }
        List<Holiday> existing = holidayRepository.findByHolidayDateBetweenOrderByHolidayDate(from, to);
        if (!existing.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    existing.get(0).getHolidayDate() + " তারিখে আগে থেকেই ছুটি আছে");
        }

        String name = request.name().trim();
        List<Holiday> created = new ArrayList<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            Holiday holiday = new Holiday();
            holiday.setHolidayDate(day);
            holiday.setName(name);
            created.add(holiday);
        }
        holidayRepository.saveAll(created);
        holidayRepository.flush();

        String period = to.equals(from) ? from.toString() : from + " থেকে " + to;
        auditService.log("HOLIDAY_ADDED", "HOLIDAY", created.get(0).getId(), period + " | " + name);
        return created.stream().map(HolidayResponse::from).toList();
    }

    @Transactional
    public void delete(Long id) {
        Holiday holiday = holidayRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ছুটি পাওয়া যায়নি"));
        holidayRepository.delete(holiday);
        auditService.log("HOLIDAY_REMOVED", "HOLIDAY", id, holiday.getHolidayDate() + " | " + holiday.getName());
    }

    private static ApiException bad(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}