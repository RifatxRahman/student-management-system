package com.sms.studentmanagement.controller;

import com.sms.studentmanagement.dto.PromotionDtos.PromotionExecuteRequest;
import com.sms.studentmanagement.dto.PromotionDtos.PromotionExecuteResponse;
import com.sms.studentmanagement.dto.PromotionDtos.PromotionPreviewResponse;
import com.sms.studentmanagement.service.StudentPromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/promotion")
@RequiredArgsConstructor
public class StudentPromotionController {

    private final StudentPromotionService promotionService;

    @GetMapping("/preview")
    @PreAuthorize("hasAuthority('PERM_STUDENT_MANAGE') or hasRole('ADMIN')")
    public ResponseEntity<PromotionPreviewResponse> getPreview(
            @RequestParam Long sourceSessionId,
            @RequestParam Long sourceClassId,
            @RequestParam(required = false) Long sourceSectionId,
            @RequestParam Long targetSessionId,
            @RequestParam(required = false) Long targetClassId,
            @RequestParam(required = false) Long finalExamId,
            @RequestParam(defaultValue = "true") boolean meritRoll) {

        PromotionPreviewResponse response = promotionService.preview(
                sourceSessionId, sourceClassId, sourceSectionId,
                targetSessionId, targetClassId, finalExamId, meritRoll);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/execute")
    @PreAuthorize("hasAuthority('PERM_STUDENT_MANAGE') or hasRole('ADMIN')")
    public ResponseEntity<PromotionExecuteResponse> executePromotion(
            @RequestBody PromotionExecuteRequest request) {

        PromotionExecuteResponse response = promotionService.execute(request);
        return ResponseEntity.ok(response);
    }
}
