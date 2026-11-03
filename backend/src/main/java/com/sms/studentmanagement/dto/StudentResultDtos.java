package com.sms.studentmanagement.dto;

import com.sms.studentmanagement.entity.ExamType;
import java.math.BigDecimal;
import java.time.Instant;

public final class StudentResultDtos {

    private StudentResultDtos() {
    }

    /** examId, sectionId, enrollmentId দিয়ে মার্কশিটের ঠিকানা বানানো যায়। ফেল হলে অবস্থান null। */
    public record StudentResultSummary(
            Long resultId, Long examId, String examName, ExamType examType,
            Long sessionId, String sessionName, String className,
            Long sectionId, String sectionName, Long enrollmentId, int rollNumber,
            BigDecimal totalMarks, BigDecimal totalFullMarks, BigDecimal averageMarks,
            BigDecimal gpa, String grade, boolean passed,
            Integer positionInClass, Integer positionInSection, Instant publishedAt) {
    }
}