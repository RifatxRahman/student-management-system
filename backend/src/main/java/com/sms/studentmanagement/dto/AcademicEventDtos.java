package com.sms.studentmanagement.dto;

import com.sms.studentmanagement.entity.AcademicEvent;
import com.sms.studentmanagement.entity.AcademicEventType;
import com.sms.studentmanagement.entity.EventAudience;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public final class AcademicEventDtos {

    private AcademicEventDtos() {
    }

    public record AcademicEventRequest(
            Long sessionId,
            @NotBlank @Size(max = 150) String title,
            @NotNull AcademicEventType eventType,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            Boolean isHoliday,
            @Size(max = 500) String description,
            EventAudience targetAudience) {
    }

    public record AcademicEventResponse(
            Long id,
            Long sessionId,
            String sessionName,
            String title,
            AcademicEventType eventType,
            LocalDate startDate,
            LocalDate endDate,
            boolean holiday,
            String description,
            EventAudience targetAudience) {

        public static AcademicEventResponse from(AcademicEvent e) {
            return new AcademicEventResponse(
                    e.getId(),
                    e.getSession().getId(),
                    e.getSession().getName(),
                    e.getTitle(),
                    e.getEventType(),
                    e.getStartDate(),
                    e.getEndDate(),
                    e.isHoliday(),
                    e.getDescription(),
                    e.getTargetAudience());
        }
    }

    public record CalendarDayDetail(
            LocalDate date,
            DayOfWeek dayOfWeek,
            boolean weeklyOff,
            boolean holiday,
            String holidayName,
            List<AcademicEventResponse> events) {
    }

    public record CalendarMonthResponse(
            int year,
            int month,
            String monthName,
            List<CalendarDayDetail> days,
            int totalEvents,
            int totalHolidays) {
    }
}
