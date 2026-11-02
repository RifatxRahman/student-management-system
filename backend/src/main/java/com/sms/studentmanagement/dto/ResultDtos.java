package com.sms.studentmanagement.dto;

import com.sms.studentmanagement.dto.ExamDtos.ExamResponse;
import java.math.BigDecimal;
import java.util.List;

public final class ResultDtos {

    private ResultDtos() {
    }

    public record ResultSubjectRow(
            Long examSubjectId, String subjectName, String subjectCode, boolean optional,
            BigDecimal fullMarks, BigDecimal passMarks, BigDecimal marks, boolean absent,
            String grade, BigDecimal gradePoint, boolean passed) {
    }

    /** resultId = null হলে এটা প্রিভিউ (এখনো সেভ হয়নি)। ফেল হলে অবস্থান null। */
    public record ResultRow(
            Long resultId, Long enrollmentId, Long studentId, String studentCode, String studentName,
            Long sectionId, String sectionName, int rollNumber,
            BigDecimal totalMarks, BigDecimal totalFullMarks, BigDecimal averageMarks,
            BigDecimal gpa, String grade, boolean passed,
            Integer positionInClass, Integer positionInSection, List<ResultSubjectRow> subjects) {
    }

    public record MissingMark(Long enrollmentId, String studentName, int rollNumber, String sectionName, String subjectName) {
    }

    /** missing = কার কোন বিষয়ের মার্কস বাকি (প্রথম ২০টা), missingTotal = মোট কয়টা বাকি */
    public record ResultPreview(
            ExamResponse exam, int studentCount, int completeCount,
            List<MissingMark> missing, int missingTotal, List<ResultRow> rows) {
    }

    public record PublishResult(int students, int passed, int failed) {
    }
}