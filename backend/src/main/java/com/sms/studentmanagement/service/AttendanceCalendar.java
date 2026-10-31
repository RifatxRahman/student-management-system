package com.sms.studentmanagement.service;

import com.sms.studentmanagement.config.AttendanceProperties;
import com.sms.studentmanagement.entity.Holiday;
import com.sms.studentmanagement.repository.HolidayRepository;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AttendanceCalendar {

    private final HolidayRepository holidayRepository;
    private final AttendanceProperties properties;

    /** দিনটা বন্ধ হলে কারণ ফেরায়, খোলা (কর্মদিবস) হলে null। */
    public String closedReason(LocalDate date) {
        if (properties.getWeeklyOffDays().contains(date.getDayOfWeek())) {
            return "সাপ্তাহিক ছুটির দিন";
        }
        return holidayRepository.findByHolidayDate(date)
                .map(h -> "ছুটি: " + h.getName())
                .orElse(null);
    }

    /** [from, to] সময়ে সাপ্তাহিক ছুটি ও Admin-এর দেওয়া ছুটি বাদ দিয়ে কত দিন কর্মদিবস। */
    public long workingDays(LocalDate from, LocalDate to) {
        Set<LocalDate> holidays = holidayRepository
                .findByHolidayDateBetweenOrderByHolidayDate(from, to).stream()
                .map(Holiday::getHolidayDate)
                .collect(Collectors.toSet());
        long count = 0;
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            boolean weeklyOff = properties.getWeeklyOffDays().contains(day.getDayOfWeek());
            if (!weeklyOff && !holidays.contains(day)) {
                count++;
            }
        }
        return count;
    }
}