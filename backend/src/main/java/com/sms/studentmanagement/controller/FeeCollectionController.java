package com.sms.studentmanagement.controller;

import com.sms.studentmanagement.dto.FeeCollectionDtos.ApplyFineRequest;
import com.sms.studentmanagement.dto.FeeCollectionDtos.BulkInvoiceRequest;
import com.sms.studentmanagement.dto.FeeCollectionDtos.BulkInvoiceResponse;
import com.sms.studentmanagement.dto.FeeCollectionDtos.CustomInvoiceRequest;
import com.sms.studentmanagement.dto.FeeCollectionDtos.InvoiceResponse;
import com.sms.studentmanagement.dto.FeeCollectionDtos.PaymentCollectionRequest;
import com.sms.studentmanagement.dto.FeeCollectionDtos.PaymentReceiptResponse;
import com.sms.studentmanagement.dto.FeeCollectionDtos.StudentWaiverRequest;
import com.sms.studentmanagement.dto.FeeCollectionDtos.StudentWaiverResponse;
import com.sms.studentmanagement.entity.InvoiceStatus;
import com.sms.studentmanagement.service.FeeCollectionService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FeeCollectionController {

    private final FeeCollectionService service;

    // ==================== 1. Student Waivers ====================

    @GetMapping("/fee-waivers")
    public List<StudentWaiverResponse> listWaivers(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long sessionId) {
        return service.listWaivers(studentId, sessionId);
    }

    @PostMapping("/fee-waivers")
    public ResponseEntity<StudentWaiverResponse> createOrUpdateWaiver(
            @Valid @RequestBody StudentWaiverRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createOrUpdateWaiver(request));
    }

    @DeleteMapping("/fee-waivers/{id}")
    public ResponseEntity<Void> deleteWaiver(@PathVariable Long id) {
        service.deleteWaiver(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== 2. Invoices ====================

    @GetMapping("/invoices")
    public List<InvoiceResponse> searchInvoices(
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) String billingMonth) {
        return service.searchInvoices(sessionId, classId, sectionId, studentId, status, billingMonth);
    }

    @GetMapping("/invoices/export")
    public ResponseEntity<byte[]> exportInvoices(
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) String billingMonth) {
        byte[] excelBytes = service.exportInvoicesToExcel(sessionId, classId, sectionId, studentId, status, billingMonth);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoices_report.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    @GetMapping("/invoices/{id}")
    public InvoiceResponse getInvoice(@PathVariable Long id) {
        return service.getInvoice(id);
    }

    @GetMapping("/invoices/student/{studentId}")
    public List<InvoiceResponse> getStudentInvoices(@PathVariable Long studentId) {
        return service.getStudentInvoices(studentId);
    }

    @PostMapping("/invoices/bulk")
    public ResponseEntity<BulkInvoiceResponse> generateBulkInvoices(
            @Valid @RequestBody BulkInvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.generateBulkInvoices(request));
    }

    @PostMapping("/invoices/custom")
    public ResponseEntity<InvoiceResponse> createCustomInvoice(
            @Valid @RequestBody CustomInvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createCustomInvoice(request));
    }

    @PostMapping("/invoices/{id}/fine")
    public InvoiceResponse applyLateFine(
            @PathVariable Long id,
            @Valid @RequestBody ApplyFineRequest request) {
        return service.applyLateFine(id, request);
    }

    // ==================== 3. Payments & Receipts ====================

    @PostMapping("/fee-payments")
    public ResponseEntity<PaymentReceiptResponse> collectPayment(
            @Valid @RequestBody PaymentCollectionRequest request,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "ADMIN";
        return ResponseEntity.status(HttpStatus.CREATED).body(service.collectPayment(request, username));
    }

    @GetMapping("/fee-payments/{id}/receipt")
    public PaymentReceiptResponse getPaymentReceipt(@PathVariable Long id) {
        return service.getPaymentReceipt(id);
    }
}
