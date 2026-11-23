package com.sms.studentmanagement.dto;

import com.sms.studentmanagement.entity.NotificationAudience;
import com.sms.studentmanagement.entity.NotificationChannel;
import com.sms.studentmanagement.entity.NotificationCampaign;
import com.sms.studentmanagement.entity.NotificationTemplate;
import com.sms.studentmanagement.entity.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public final class CampaignDtos {

    private CampaignDtos() {}

    // ==================== Template DTOs ====================

    public record NotificationTemplateRequest(
            @NotBlank(message = "টেমপ্লেটের নাম আবশ্যক") String name,
            @NotBlank(message = "টেমপ্লেটের ইউনিক কোড আবশ্যক") String code,
            @NotNull(message = "মাধ্যম (Channel) আবশ্যক") NotificationChannel channel,
            @NotNull(message = "ক্যাটাগরি আবশ্যক") NotificationType category,
            String subject,
            @NotBlank(message = "টেমপ্লেটের বার্তা বা মেসেজ আবশ্যক") String templateBody,
            String variables,
            Boolean active
    ) {}

    public record NotificationTemplateResponse(
            Long id,
            String name,
            String code,
            NotificationChannel channel,
            String channelBangla,
            NotificationType category,
            String categoryBangla,
            String subject,
            String templateBody,
            String variables,
            boolean active
    ) {
        public static NotificationTemplateResponse from(NotificationTemplate t) {
            return new NotificationTemplateResponse(
                    t.getId(),
                    t.getName(),
                    t.getCode(),
                    t.getChannel(),
                    t.getChannel().getBanglaLabel(),
                    t.getCategory(),
                    t.getCategory().getBanglaLabel(),
                    t.getSubject(),
                    t.getTemplateBody(),
                    t.getVariables(),
                    t.isActive()
            );
        }
    }

    // ==================== Campaign DTOs ====================

    public record SendCampaignRequest(
            @NotBlank(message = "ক্যাম্পেইনের শিরোনাম আবশ্যক") String title,
            @NotNull(message = "মাধ্যম (Channel) আবশ্যক") NotificationChannel channel,
            @NotNull(message = "টার্গেট প্রাপক গ্রুপ আবশ্যক") NotificationAudience targetAudience,
            Long targetClassId,
            Long targetSectionId,
            String templateCode,
            @NotBlank(message = "মেসেজের বিষয়বস্তু আবশ্যক") String message
    ) {}

    public record CampaignResponse(
            Long id,
            String title,
            NotificationChannel channel,
            String channelBangla,
            NotificationAudience targetAudience,
            String targetAudienceBangla,
            Long targetClassId,
            String targetClassName,
            Long targetSectionId,
            String targetSectionName,
            String message,
            int totalRecipients,
            int sentCount,
            int failedCount,
            String createdBy,
            Instant createdAt
    ) {
        public static CampaignResponse from(NotificationCampaign c) {
            return new CampaignResponse(
                    c.getId(),
                    c.getTitle(),
                    c.getChannel(),
                    c.getChannel().getBanglaLabel(),
                    c.getTargetAudience(),
                    c.getTargetAudience().getBanglaLabel(),
                    c.getTargetClass() != null ? c.getTargetClass().getId() : null,
                    c.getTargetClass() != null ? c.getTargetClass().getName() : null,
                    c.getTargetSection() != null ? c.getTargetSection().getId() : null,
                    c.getTargetSection() != null ? c.getTargetSection().getName() : null,
                    c.getMessage(),
                    c.getTotalRecipients(),
                    c.getSentCount(),
                    c.getFailedCount(),
                    c.getCreatedBy(),
                    c.getCreatedAt()
            );
        }
    }

    public record PreviewPlaceholderRequest(
            @NotBlank String templateBody,
            Long studentId
    ) {}

    public record PreviewPlaceholderResponse(
            String originalTemplate,
            String renderedMessage,
            int characterCount,
            int smsParts,
            String language
    ) {}
}
