package com.sms.studentmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sms.studentmanagement.config.AttendanceProperties;
import com.sms.studentmanagement.entity.Holiday;
import com.sms.studentmanagement.repository.HolidayRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AttendanceCalendarTest {

    @Mock
    HolidayRepository holidayRepository;

    AttendanceCalendar calendar;

    @BeforeEach
    void setUp() {
        calendar = new AttendanceCalendar(holidayRepository, new AttendanceProperties());
    }

    private Holiday holiday(LocalDate date, String name) {
        Holiday holiday = new Holiday();
        holiday.setHolidayDate(date);
        holiday.setName(name);
        return holiday;
    }

    @Test
    void fridayIsClosed() {
        // ২০২৬-১১-০৬ শুক্রবার
        when(holidayRepository.findByHolidayDate(any())).thenReturn(Optional.empty());
        assertNotNull(calendar.closedReason(LocalDate.of(2026, 11, 6)));
    }

    @Test
    void normalDayIsOpen() {
        // ২০২৬-১১-০৮ রবিবার
        when(holidayRepository.findByHolidayDate(any())).thenReturn(Optional.empty());
        assertNull(calendar.closedReason(LocalDate.of(2026, 11, 8)));
    }

    @Test
    void adminHolidayIsClosed() {
        LocalDate day = LocalDate.of(2026, 11, 10);
        when(holidayRepository.findByHolidayDate(day)).thenReturn(Optional.of(holiday(day, "টেস্ট ছুটি")));
        assertNotNull(calendar.closedReason(day));
    }

    @Test
    void workingDaysSkipFridaysOnly() {
        when(holidayRepository.findByHolidayDateBetweenOrderByHolidayDate(any(), any())).thenReturn(List.of());
        assertEquals(26, calendar.workingDays(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)));
    }

    @Test
    void holidayOnFridayIsNotCountedTwice() {
        // ১০ নভেম্বর (মঙ্গলবার) ও ২৭ নভেম্বর (শুক্রবার) ছুটি → ২৬ − ১ = ২৫
        when(holidayRepository.findByHolidayDateBetweenOrderByHolidayDate(any(), any())).thenReturn(List.of(
                holiday(LocalDate.of(2026, 11, 10), "ক"),
                holiday(LocalDate.of(2026, 11, 27), "খ")));
        assertEquals(25, calendar.workingDays(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)));
    }
}