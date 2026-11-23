package com.sms.studentmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sms.studentmanagement.dto.AcademicEventDtos.CalendarMonthResponse;
import com.sms.studentmanagement.entity.AcademicEvent;
import com.sms.studentmanagement.entity.AcademicEventType;
import com.sms.studentmanagement.entity.AcademicSession;
import com.sms.studentmanagement.entity.Holiday;
import com.sms.studentmanagement.repository.AcademicEventRepository;
import com.sms.studentmanagement.repository.HolidayRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CalendarServiceTest {

    @Mock
    HolidayRepository holidayRepository;

    @Mock
    AcademicEventRepository eventRepository;

    CalendarService calendarService;

    @BeforeEach
    void setUp() {
        calendarService = new CalendarService(holidayRepository, eventRepository);
    }

    @Test
    void getMonthView_combinesHolidaysAndEvents() {
        int year = 2026;
        int month = 2; // February 2026 (28 days)

        Holiday holiday = new Holiday();
        holiday.setHolidayDate(LocalDate.of(2026, 2, 21));
        holiday.setName("Shaheed Day");
        List<Holiday> holidays = new ArrayList<>();
        holidays.add(holiday);
        when(holidayRepository.findByHolidayDateBetweenOrderByHolidayDate(any(), any())).thenReturn(holidays);

        AcademicSession session = new AcademicSession();
        session.setId(1L);
        session.setName("2026");

        AcademicEvent event = new AcademicEvent();
        event.setId(1L);
        event.setSession(session);
        event.setTitle("Sports Week");
        event.setEventType(AcademicEventType.SPORTS);
        event.setStartDate(LocalDate.of(2026, 2, 10));
        event.setEndDate(LocalDate.of(2026, 2, 12));
        List<AcademicEvent> events = new ArrayList<>();
        events.add(event);
        when(eventRepository.findInRangeWithSession(any(), any())).thenReturn(events);

        CalendarMonthResponse response = calendarService.getMonthView(year, month);

        assertNotNull(response);
        assertEquals(28, response.days().size());
        assertEquals(1, response.totalEvents());
        assertTrue(response.totalHolidays() > 0);
    }
}
