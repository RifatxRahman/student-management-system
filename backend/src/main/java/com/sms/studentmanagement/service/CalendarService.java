package com.sms.studentmanagement.service;

import com.sms.studentmanagement.dto.AcademicEventDtos.AcademicEventResponse;
import com.sms.studentmanagement.dto.AcademicEventDtos.CalendarDayDetail;
import com.sms.studentmanagement.dto.AcademicEventDtos.CalendarMonthResponse;
import com.sms.studentmanagement.entity.AcademicEvent;
import com.sms.studentmanagement.entity.Holiday;
import com.sms.studentmanagement.repository.AcademicEventRepository;
import com.sms.studentmanagement.repository.HolidayRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final HolidayRepository holidayRepository;
    private final AcademicEventRepository eventRepository;

    @Transactional(readOnly = true)
    public CalendarMonthResponse getMonthView(int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);

        List<Holiday> holidays = holidayRepository.findByHolidayDateBetweenOrderByHolidayDate(start, end);
        List<AcademicEvent> events = eventRepository.findInRangeWithSession(start, end);

        Map<LocalDate, String> holidayMap = new HashMap<>();
        for (int i = 0; i < holidays.size(); i++) {
            Holiday h = holidays.get(i);
            holidayMap.put(h.getHolidayDate(), h.getName());
        }

        Map<LocalDate, List<AcademicEventResponse>> eventMap = new HashMap<>();
        for (int i = 0; i < events.size(); i++) {
            AcademicEvent e = events.get(i);
            AcademicEventResponse dto = AcademicEventResponse.from(e);
            LocalDate dStart = e.getStartDate().isBefore(start) ? start : e.getStartDate();
            LocalDate dEnd = e.getEndDate().isAfter(end) ? end : e.getEndDate();

            for (LocalDate d = dStart; !d.isAfter(dEnd); d = d.plusDays(1)) {
                List<AcademicEventResponse> list = eventMap.computeIfAbsent(d, k -> new ArrayList<>());
                list.add(dto);
            }
        }

        List<CalendarDayDetail> days = new ArrayList<>();
        int totalHolidays = 0;

        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            boolean isFriday = d.getDayOfWeek() == DayOfWeek.FRIDAY;
            String holidayName = holidayMap.get(d);
            boolean isHoliday = isFriday || holidayName != null;
            if (isHoliday) {
                totalHolidays++;
            }

            List<AcademicEventResponse> dayEvents = eventMap.getOrDefault(d, List.of());

            days.add(new CalendarDayDetail(
                    d,
                    d.getDayOfWeek(),
                    isFriday,
                    holidayName != null,
                    holidayName,
                    dayEvents));
        }

        String monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        return new CalendarMonthResponse(
                year,
                month,
                monthName,
                days,
                events.size(),
                totalHolidays);
    }
}
