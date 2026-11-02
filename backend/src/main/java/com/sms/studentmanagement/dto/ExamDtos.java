package com.sms.studentmanagement.dto;

import com.sms.studentmanagement.entity.Exam;
import com.sms.studentmanagement.entity.ExamStatus;
import com.sms.studentmanagement.entity.ExamSubject;
import com.sms.studentmanagement.entity.ExamType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class ExamDtos {

    private ExamDtos() {
    }

    /** copySubjects = true হলে ক্লাসের সব সক্রিয় বিষয় ফুল ১০০, পাস ৩৩ হিসেবে যোগ হয়। */
    public record ExamRequest(
            @NotNull Long sessionId,
            @NotNull Long classId,
            @NotBlank @Size(max = 100) String name,
            @NotNull ExamType type,
            boolean copySubjects) {
    }

    public record ExamUpdateRequest(@NotBlank @Size(max = 100) String name, @NotNull ExamType type) {
    }

    public record ExamSubjectRequest(
            @NotNull Long subjectId,
            @NotNull @DecimalMin("0.01") @Digits(integer = 4, fraction = 2) BigDecimal fullMarks,
            @NotNull @DecimalMin("0.00") @Digits(integer = 4, fraction = 2) BigDecimal passMarks,
            boolean optional) {
    }

    public record ExamSubjectUpdateRequest(
            @NotNull @DecimalMin("0.01") @Digits(integer = 4, fraction = 2) BigDecimal fullMarks,
            @NotNull @DecimalMin("0.00") @Digits(integer = 4, fraction = 2) BigDecimal passMarks,
            boolean optional) {
    }

    public record ExamResponse(
            Long id, Long sessionId, String sessionName, Long classId, String className,
            String name, ExamType type, ExamStatus status, Instant publishedAt) {

        public static ExamResponse from(Exam e) {
            return new ExamResponse(
                    e.getId(), e.getSession().getId(), e.getSession().getName(),
                    e.getSchoolClass().getId(), e.getSchoolClass().getName(),
                    e.getName(), e.getExamType(), e.getStatus(), e.getPublishedAt());
        }
    }

    public record ExamSubjectResponse(
            Long id, Long subjectId, String subjectName, String subjectCode,
            BigDecimal fullMarks, BigDecimal passMarks, boolean optional) {

        public static ExamSubjectResponse from(ExamSubject es) {
            return new ExamSubjectResponse(
                    es.getId(), es.getSubject().getId(), es.getSubject().getName(), es.getSubject().getCode(),
                    es.getFullMarks(), es.getPassMarks(), es.isOptional());
        }
    }

    /** totalFullMarks = ঐচ্ছিক বাদে সব বিষয়ের ফুল মার্কসের যোগ। */
    public record ExamDetail(ExamResponse exam, List<ExamSubjectResponse> subjects, BigDecimal totalFullMarks) {
    }
}