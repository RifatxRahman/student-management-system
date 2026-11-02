package com.sms.studentmanagement.dto;

import com.sms.studentmanagement.entity.ExamStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public final class MarkDtos {

    private MarkDtos() {
    }

    /** "কোন সেকশনের কোন বিষয়ে মার্কস দেব" তালিকার এক সারি */
    public record MarkTarget(
            Long sectionId, String sectionName, String className,
            Long subjectId, String subjectName, String subjectCode,
            BigDecimal fullMarks, BigDecimal passMarks, boolean optional) {
    }

    /** entered = false মানে এখনো কিছু দেওয়া হয়নি। absent = true হলে marks থাকে না। */
    public record MarkRow(
            Long enrollmentId, Long studentId, String studentCode, String studentName, int rollNumber,
            BigDecimal marks, boolean absent, boolean entered) {
    }

    /** requiresReason = true মানে ফল প্রকাশিত, বদলাতে কারণ লাগবে। */
    public record MarkSheet(
            Long examId, String examName, ExamStatus examStatus,
            Long sectionId, String sectionName, String className,
            Long subjectId, String subjectName, BigDecimal fullMarks, BigDecimal passMarks, boolean optional,
            boolean editable, String lockReason, boolean requiresReason, List<MarkRow> rows) {
    }

    public record MarkEntryRequest(
            @NotNull Long enrollmentId,
            @DecimalMin("0.00") @Digits(integer = 4, fraction = 2) BigDecimal marks,
            boolean absent) {
    }

    public record SaveMarksRequest(
            @NotNull Long examId,
            @NotNull Long sectionId,
            @NotNull Long subjectId,
            @NotEmpty @Size(max = 300) List<@Valid MarkEntryRequest> entries,
            @Size(max = 500) String reason) {
    }

    public record SaveMarksResult(int created, int changed, int unchanged) {
    }
}