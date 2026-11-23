package com.sms.studentmanagement.dto;

import java.math.BigDecimal;
import java.util.List;

public final class PromotionDtos {

    private PromotionDtos() {
    }

    public enum PromotionAction {
        PROMOTE("উত্তীর্ণ / প্রমোট"),
        RETAIN("একই শ্রেণিতে বহাল / রিটেইন"),
        GRADUATE("গ্র্যাজুয়েট / সমাপ্ত"),
        SKIP("বাদ রাখুন / অপরিবর্তিত");

        private final String banglaLabel;

        PromotionAction(String banglaLabel) {
            this.banglaLabel = banglaLabel;
        }

        public String getBanglaLabel() {
            return banglaLabel;
        }
    }

    public record PromotionPreviewItem(
            Long studentId,
            String studentCode,
            String fullName,
            Long currentEnrollmentId,
            Long currentSectionId,
            String currentSectionName,
            int currentRollNumber,
            BigDecimal gpa,
            String grade,
            Boolean passed,
            Integer meritPosition,
            boolean alreadyEnrolledInTarget,
            PromotionAction suggestedAction,
            Long suggestedTargetClassId,
            String suggestedTargetClassName,
            Long suggestedTargetSectionId,
            String suggestedTargetSectionName,
            int suggestedRollNumber
    ) {}

    public record PromotionPreviewResponse(
            Long sourceSessionId,
            String sourceSessionName,
            Long sourceClassId,
            String sourceClassName,
            Long targetSessionId,
            String targetSessionName,
            Long targetClassId,
            String targetClassName,
            int totalStudents,
            int passedStudents,
            int failedStudents,
            List<PromotionPreviewItem> items
    ) {}

    public record StudentPromotionItem(
            Long studentId,
            PromotionAction action,
            Long targetClassId,
            Long targetSectionId,
            Integer newRollNumber
    ) {}

    public record PromotionExecuteRequest(
            Long sourceSessionId,
            Long targetSessionId,
            List<StudentPromotionItem> students
    ) {}

    public record PromotionExecuteResponse(
            int totalProcessed,
            int promotedCount,
            int retainedCount,
            int graduatedCount,
            int skippedCount,
            String message
    ) {}
}
