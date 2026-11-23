package com.sms.studentmanagement.controller;

import com.sms.studentmanagement.dto.FeeDtos.ClassFeeSummaryResponse;
import com.sms.studentmanagement.dto.FeeDtos.CopyFeeStructureRequest;
import com.sms.studentmanagement.dto.FeeDtos.FeeHeadRequest;
import com.sms.studentmanagement.dto.FeeDtos.FeeHeadResponse;
import com.sms.studentmanagement.dto.FeeDtos.FeeStructureRequest;
import com.sms.studentmanagement.dto.FeeDtos.FeeStructureResponse;
import com.sms.studentmanagement.service.FeeStructureService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
public class FeeStructureController {

    private final FeeStructureService feeStructureService;

    // ==================== Fee Heads ====================

    @GetMapping("/fee-heads")
    public List<FeeHeadResponse> listFeeHeads(
            @RequestParam(required = false, defaultValue = "false") Boolean activeOnly) {
        return feeStructureService.listFeeHeads(activeOnly);
    }

    @GetMapping("/fee-heads/{id}")
    public FeeHeadResponse getFeeHead(@PathVariable Long id) {
        return feeStructureService.getFeeHead(id);
    }

    @PostMapping("/fee-heads")
    public ResponseEntity<FeeHeadResponse> createFeeHead(@Valid @RequestBody FeeHeadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(feeStructureService.createFeeHead(request));
    }

    @PutMapping("/fee-heads/{id}")
    public FeeHeadResponse updateFeeHead(@PathVariable Long id, @Valid @RequestBody FeeHeadRequest request) {
        return feeStructureService.updateFeeHead(id, request);
    }

    @DeleteMapping("/fee-heads/{id}")
    public ResponseEntity<Void> deleteFeeHead(@PathVariable Long id) {
        feeStructureService.deleteFeeHead(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Fee Structures ====================

    @GetMapping("/fee-structures")
    public List<FeeStructureResponse> listFeeStructures(
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) Long classId) {
        return feeStructureService.listFeeStructures(sessionId, classId);
    }

    @GetMapping("/fee-structures/{id}")
    public FeeStructureResponse getFeeStructure(@PathVariable Long id) {
        return feeStructureService.getFeeStructure(id);
    }

    @PostMapping("/fee-structures")
    public ResponseEntity<FeeStructureResponse> createFeeStructure(
            @Valid @RequestBody FeeStructureRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(feeStructureService.createFeeStructure(request));
    }

    @PutMapping("/fee-structures/{id}")
    public FeeStructureResponse updateFeeStructure(
            @PathVariable Long id,
            @Valid @RequestBody FeeStructureRequest request) {
        return feeStructureService.updateFeeStructure(id, request);
    }

    @DeleteMapping("/fee-structures/{id}")
    public ResponseEntity<Void> deleteFeeStructure(@PathVariable Long id) {
        feeStructureService.deleteFeeStructure(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/fee-structures/summary")
    public ClassFeeSummaryResponse getClassFeeSummary(
            @RequestParam(required = false) Long sessionId,
            @RequestParam Long classId) {
        return feeStructureService.getClassFeeSummary(sessionId, classId);
    }

    @PostMapping("/fee-structures/copy")
    public ResponseEntity<Map<String, Object>> copySessionFees(
            @Valid @RequestBody CopyFeeStructureRequest request) {
        int count = feeStructureService.copySessionFees(request);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "সফলভাবে " + count + " টি ফি কাঠামো অনুলিপি করা হয়েছে।",
                "copiedCount", count
        ));
    }
}
