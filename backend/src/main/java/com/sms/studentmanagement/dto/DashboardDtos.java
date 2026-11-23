package com.sms.studentmanagement.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record StatCards(
            long totalStudents,
            long totalTeachers,
            long totalClasses,
            String currentSessionName,
            BigDecimal todayAttendancePercent,
            long todayPresent,
            long todayAbsent,
            boolean isHolidayToday) {
    }

    public record AttendanceTrendItem(
            LocalDate date,
            String dayName,
            long present,
            long absent,
            BigDecimal percentage) {
    }

    public record RecentActivity(
            Long id,
            String action,
            String entityType,
            String details,
            String performedBy,
            Instant createdAt) {
    }

    public record UpcomingHolidayItem(
            Long id,
            LocalDate holidayDate,
            String name) {
    }

    public record DashboardSummaryResponse(
            StatCards stats,
            List<AttendanceTrendItem> attendanceTrend,
            List<RecentActivity> recentActivities,
            List<UpcomingHolidayItem> upcomingHolidays) {
    }
}
