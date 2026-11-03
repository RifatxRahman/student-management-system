package com.sms.studentmanagement.dto;

import com.sms.studentmanagement.entity.Room;
import com.sms.studentmanagement.entity.TimeSlot;
import com.sms.studentmanagement.entity.TimetableEntry;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.time.LocalTime;

public final class RoutineDtos {

    private RoutineDtos() {
    }

    // ---------- রুম ----------
    /** active না দিলে নতুন রুম সক্রিয় ধরা হয়। */
    public record RoomRequest(
            @NotBlank @Size(max = 50) String name, @Min(1) @Max(10000) Integer capacity, Boolean active) {
    }

    public record RoomResponse(Long id, String name, Integer capacity, boolean active) {

        public static RoomResponse from(Room r) {
            return new RoomResponse(r.getId(), r.getName(), r.getCapacity(), r.isActive());
        }
    }

    // ---------- সময়ের স্লট (পিরিয়ড) ----------
    public record TimeSlotRequest(
            @NotBlank @Size(max = 50) String name,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime,
            Boolean active) {
    }

    public record TimeSlotResponse(Long id, String name, LocalTime startTime, LocalTime endTime, boolean active) {

        public static TimeSlotResponse from(TimeSlot s) {
            return new TimeSlotResponse(s.getId(), s.getName(), s.getStartTime(), s.getEndTime(), s.isActive());
        }
    }

    // ---------- রুটিন ----------
    public record TimetableRequest(
            @NotNull Long sessionId,
            @NotNull Long sectionId,
            @NotNull DayOfWeek dayOfWeek,
            @NotNull Long timeSlotId,
            @NotNull Long subjectId,
            @NotNull Long teacherId,
            Long roomId) {
    }

    /** বার ও পিরিয়ড বদলানো যায় না (বদলাতে মুছে নতুন বসান)। শুধু বিষয়, শিক্ষক ও রুম। */
    public record TimetableUpdateRequest(@NotNull Long subjectId, @NotNull Long teacherId, Long roomId) {
    }

    public record TimetableResponse(
            Long id, Long sessionId, Long sectionId, String sectionName, String className,
            DayOfWeek dayOfWeek, Long timeSlotId, String timeSlotName, LocalTime startTime, LocalTime endTime,
            Long subjectId, String subjectName, String subjectCode,
            Long teacherId, String teacherName, Long roomId, String roomName) {

        public static TimetableResponse from(TimetableEntry t) {
            return new TimetableResponse(
                    t.getId(), t.getSession().getId(), t.getSection().getId(), t.getSection().getName(),
                    t.getSection().getSchoolClass().getName(), t.getDayOfWeek(),
                    t.getTimeSlot().getId(), t.getTimeSlot().getName(), t.getTimeSlot().getStartTime(),
                    t.getTimeSlot().getEndTime(), t.getSubject().getId(), t.getSubject().getName(),
                    t.getSubject().getCode(), t.getTeacher().getId(), t.getTeacher().getFullName(),
                    t.getRoom() == null ? null : t.getRoom().getId(),
                    t.getRoom() == null ? null : t.getRoom().getName());
        }
    }
}