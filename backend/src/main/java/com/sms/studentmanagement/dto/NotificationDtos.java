package com.sms.studentmanagement.dto;

import com.sms.studentmanagement.entity.NotificationLog;
import com.sms.studentmanagement.entity.NotificationStatus;
import com.sms.studentmanagement.entity.NotificationType;
import java.time.Instant;
import java.time.LocalDate;

public final class NotificationDtos {

    private NotificationDtos() {
    }

    public record NotificationResponse(
            Long id, Long studentId, String studentCode, String studentName,
            Long guardianId, String guardianName, String phone, String message,
            NotificationType type, LocalDate eventDate, NotificationStatus status,
            int attempts, String lastError, Instant sentAt) {

        public static NotificationResponse from(NotificationLog n) {
            return new NotificationResponse(
                    n.getId(), n.getStudent().getId(), n.getStudent().getStudentCode(),
                    n.getStudent().getFullName(), n.getGuardian().getId(), n.getGuardian().getFullName(),
                    n.getPhone(), n.getMessage(), n.getNotificationType(), n.getEventDate(),
                    n.getStatus(), n.getAttempts(), n.getLastError(), n.getSentAt());
        }
    }

    /** demoMode=true মানে আসল SMS যায়নি। */
    public record DispatchResult(int sent, int failed, boolean demoMode, String note) {
    }
}