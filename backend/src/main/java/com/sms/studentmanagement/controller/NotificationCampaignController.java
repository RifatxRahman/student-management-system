package com.sms.studentmanagement.controller;

import com.sms.studentmanagement.dto.CampaignDtos.CampaignResponse;
import com.sms.studentmanagement.dto.CampaignDtos.NotificationTemplateRequest;
import com.sms.studentmanagement.dto.CampaignDtos.NotificationTemplateResponse;
import com.sms.studentmanagement.dto.CampaignDtos.PreviewPlaceholderRequest;
import com.sms.studentmanagement.dto.CampaignDtos.PreviewPlaceholderResponse;
import com.sms.studentmanagement.dto.CampaignDtos.SendCampaignRequest;
import com.sms.studentmanagement.entity.NotificationType;
import com.sms.studentmanagement.service.NotificationCampaignService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NotificationCampaignController {

    private final NotificationCampaignService campaignService;

    // ==================== Templates ====================

    @GetMapping("/notification-templates")
    public List<NotificationTemplateResponse> listTemplates(
            @RequestParam(required = false) NotificationType category,
            @RequestParam(required = false, defaultValue = "false") Boolean activeOnly) {
        return campaignService.listTemplates(category, activeOnly);
    }

    @GetMapping("/notification-templates/{id}")
    public NotificationTemplateResponse getTemplate(@PathVariable Long id) {
        return campaignService.getTemplate(id);
    }

    @PostMapping("/notification-templates")
    public ResponseEntity<NotificationTemplateResponse> createTemplate(
            @Valid @RequestBody NotificationTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(campaignService.createTemplate(request));
    }

    @PutMapping("/notification-templates/{id}")
    public NotificationTemplateResponse updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody NotificationTemplateRequest request) {
        return campaignService.updateTemplate(id, request);
    }

    @DeleteMapping("/notification-templates/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        campaignService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Preview ====================

    @PostMapping("/notification-campaigns/preview")
    public PreviewPlaceholderResponse previewPlaceholder(
            @Valid @RequestBody PreviewPlaceholderRequest request) {
        return campaignService.previewPlaceholder(request);
    }

    // ==================== Campaigns ====================

    @GetMapping("/notification-campaigns")
    public List<CampaignResponse> listCampaigns() {
        return campaignService.listCampaigns();
    }

    @GetMapping("/notification-campaigns/{id}")
    public CampaignResponse getCampaign(@PathVariable Long id) {
        return campaignService.getCampaign(id);
    }

    @PostMapping("/notification-campaigns")
    public ResponseEntity<CampaignResponse> sendCampaign(
            @Valid @RequestBody SendCampaignRequest request,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "ADMIN";
        return ResponseEntity.status(HttpStatus.CREATED).body(campaignService.sendCampaign(request, username));
    }
}
