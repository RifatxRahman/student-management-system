package com.sms.studentmanagement.dto;

import com.sms.studentmanagement.entity.AcademicSession;
import com.sms.studentmanagement.entity.SchoolClass;
import com.sms.studentmanagement.entity.Section;
import com.sms.studentmanagement.entity.Subject;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public final class AcademicDtos {

    private AcademicDtos() {
    }

    // ---------- Academic Session ----------
    public record SessionRequest(
            @NotBlank @Size(max = 50) String name,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate) {
    }

    public record SessionResponse(
            Long id, String name, LocalDate startDate, LocalDate endDate, boolean active) {

        public static SessionResponse from(AcademicSession s) {
            return new SessionResponse(
                    s.getId(), s.getName(), s.getStartDate(), s.getEndDate(), s.isActive());
        }
    }

    // ---------- Class ----------
    public record ClassRequest(
            @NotBlank @Size(max = 50) String name,
            @Min(0) @Max(1000) int displayOrder) {
    }

    public record ClassResponse(Long id, String name, int displayOrder, boolean active) {

        public static ClassResponse from(SchoolClass c) {
            return new ClassResponse(c.getId(), c.getName(), c.getDisplayOrder(), c.isActive());
        }
    }

    // ---------- Section ----------
    public record SectionRequest(@NotBlank @Size(max = 20) String name) {
    }

    public record SectionResponse(Long id, Long classId, String name, boolean active) {

        public static SectionResponse from(Section s) {
            return new SectionResponse(
                    s.getId(), s.getSchoolClass().getId(), s.getName(), s.isActive());
        }
    }

    // ---------- Subject ----------
    public record SubjectRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank
            @Size(max = 20)
            @Pattern(regexp = "^[A-Za-z0-9_-]+$",
                    message = "কোডে শুধু ইংরেজি অক্ষর, সংখ্যা, আন্ডারস্কোর ও হাইফেন চলবে")
            String code) {
    }

    public record SubjectResponse(Long id, String name, String code, boolean active) {

        public static SubjectResponse from(Subject s) {
            return new SubjectResponse(s.getId(), s.getName(), s.getCode(), s.isActive());
        }
    }

    // ---------- Class-Subject ----------
    public record AssignSubjectRequest(@NotNull Long subjectId) {
    }
}