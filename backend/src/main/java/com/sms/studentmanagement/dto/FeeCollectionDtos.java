package com.sms.studentmanagement.dto;

import com.sms.studentmanagement.entity.FeePayment;
import com.sms.studentmanagement.entity.Invoice;
import com.sms.studentmanagement.entity.InvoiceItem;
import com.sms.studentmanagement.entity.InvoiceStatus;
import com.sms.studentmanagement.entity.PaymentMethod;
import com.sms.studentmanagement.entity.StudentWaiver;
import com.sms.studentmanagement.entity.WaiverType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class FeeCollectionDtos {

    private FeeCollectionDtos() {}

    // ==================== Student Waiver DTOs ====================

    public record StudentWaiverRequest(
            @NotNull(message = "শিক্ষার্থী নির্বাচন আবশ্যক") Long studentId,
            @NotNull(message = "সেশন নির্বাচন আবশ্যক") Long sessionId,
            @NotNull(message = "ফি খাত নির্বাচন আবশ্যক") Long feeHeadId,
            @NotNull(message = "ছাড়ের ধরন নির্বাচন আবশ্যক") WaiverType discountType,
            @NotNull(message = "ছাড়ের মান আবশ্যক")
            @DecimalMin(value = "0.00", message = "ছাড় ঋণাত্মক হতে পারে না")
            BigDecimal discountValue,
            String reason,
            Boolean active
    ) {}

    public record StudentWaiverResponse(
            Long id,
            Long studentId,
            String studentCode,
            String studentName,
            Long sessionId,
            String sessionName,
            Long feeHeadId,
            String feeHeadName,
            WaiverType discountType,
            String discountTypeBangla,
            BigDecimal discountValue,
            String reason,
            boolean active
    ) {
        public static StudentWaiverResponse from(StudentWaiver w) {
            return new StudentWaiverResponse(
                    w.getId(),
                    w.getStudent().getId(),
                    w.getStudent().getStudentCode(),
                    w.getStudent().getFullName(),
                    w.getSession().getId(),
                    w.getSession().getName(),
                    w.getFeeHead().getId(),
                    w.getFeeHead().getName(),
                    w.getDiscountType(),
                    w.getDiscountType().getBanglaLabel(),
                    w.getDiscountValue(),
                    w.getReason(),
                    w.isActive()
            );
        }
    }

    // ==================== Bulk & Custom Invoice DTOs ====================

    public record BulkInvoiceRequest(
            @NotNull(message = "সেশন নির্বাচন আবশ্যক") Long sessionId,
            @NotNull(message = "শ্রেণি নির্বাচন আবশ্যক") Long classId,
            Long sectionId,
            @NotBlank(message = "বিলিং মাস আবশ্যক (উদাঃ 2026-11)") String billingMonth,
            @NotBlank(message = "ইনভয়েসের শিরোনাম আবশ্যক") String title,
            @NotNull(message = "ইস্যু তারিখ আবশ্যক") LocalDate issueDate,
            @NotNull(message = "পরিশোধের শেষ তারিখ আবশ্যক") LocalDate dueDate,
            List<Long> feeHeadIds
    ) {}

    public record BulkInvoiceResponse(
            int totalEligibleStudents,
            int generatedInvoicesCount,
            int skippedCount,
            BigDecimal totalBilledAmount
    ) {}

    public record CustomInvoiceItemRequest(
            @NotNull(message = "ফি খাত আবশ্যক") Long feeHeadId,
            @NotNull(message = "টাকার পরিমাণ আবশ্যক")
            @DecimalMin(value = "0.01", message = "পরিমাণ ০ হতে পারে না")
            BigDecimal amount,
            BigDecimal waiverAmount
    ) {}

    public record CustomInvoiceRequest(
            @NotNull(message = "শিক্ষার্থী নির্বাচন আবশ্যক") Long studentId,
            @NotNull(message = "সেশন নির্বাচন আবশ্যক") Long sessionId,
            @NotBlank(message = "ইনভয়েসের শিরোনাম আবশ্যক") String title,
            @NotBlank(message = "বিলিং মাস আবশ্যক") String billingMonth,
            @NotNull(message = "ইস্যু তারিখ আবশ্যক") LocalDate issueDate,
            @NotNull(message = "পরিশোধের শেষ তারিখ আবশ্যক") LocalDate dueDate,
            @NotNull(message = "কমপক্ষে একটি ফি আইটেম আবশ্যক") List<CustomInvoiceItemRequest> items,
            String notes
    ) {}

    // ==================== Payment DTOs ====================

    public record PaymentCollectionRequest(
            @NotNull(message = "ইনভয়েস আইডি আবশ্যক") Long invoiceId,
            @NotNull(message = "পরিশোধিত টাকার পরিমাণ আবশ্যক")
            @DecimalMin(value = "0.01", message = "টাকার পরিমাণ ০ এর বেশি হতে হবে")
            BigDecimal amount,
            @NotNull(message = "পেমেন্ট মাধ্যম আবশ্যক") PaymentMethod paymentMethod,
            String transactionId,
            String notes
    ) {}

    public record PaymentReceiptResponse(
            Long paymentId,
            String receiptNumber,
            Instant paymentDate,
            BigDecimal amount,
            PaymentMethod paymentMethod,
            String paymentMethodBangla,
            String transactionId,
            String collectedBy,
            String notes,
            Long invoiceId,
            String invoiceNumber,
            String invoiceTitle,
            String billingMonth,
            Long studentId,
            String studentCode,
            String studentName,
            String className,
            String sectionName,
            BigDecimal invoiceNetAmount,
            BigDecimal invoicePaidAmountTotal,
            BigDecimal remainingDueAmount,
            InvoiceStatus invoiceStatus,
            String invoiceStatusBangla
    ) {
        public static PaymentReceiptResponse from(FeePayment p) {
            Invoice inv = p.getInvoice();
            return new PaymentReceiptResponse(
                    p.getId(),
                    p.getReceiptNumber(),
                    p.getPaymentDate(),
                    p.getAmount(),
                    p.getPaymentMethod(),
                    p.getPaymentMethod().getBanglaLabel(),
                    p.getTransactionId(),
                    p.getCollectedBy(),
                    p.getNotes(),
                    inv.getId(),
                    inv.getInvoiceNumber(),
                    inv.getTitle(),
                    inv.getBillingMonth(),
                    inv.getStudent() != null ? inv.getStudent().getId() : null,
                    inv.getStudent() != null ? inv.getStudent().getStudentCode() : null,
                    inv.getStudent() != null ? inv.getStudent().getFullName() : null,
                    inv.getSchoolClass() != null ? inv.getSchoolClass().getName() : null,
                    inv.getSection() != null ? inv.getSection().getName() : null,
                    inv.getNetAmount(),
                    inv.getPaidAmount(),
                    inv.getDueAmount(),
                    inv.getStatus(),
                    inv.getStatus().getBanglaLabel()
            );
        }
    }

    // ==================== Invoice Detail Responses ====================

    public record InvoiceItemResponse(
            Long id,
            Long feeHeadId,
            String feeHeadName,
            BigDecimal amount,
            BigDecimal waiverAmount,
            BigDecimal netAmount
    ) {
        public static InvoiceItemResponse from(InvoiceItem item) {
            return new InvoiceItemResponse(
                    item.getId(),
                    item.getFeeHead().getId(),
                    item.getFeeHeadName(),
                    item.getAmount(),
                    item.getWaiverAmount(),
                    item.getNetAmount()
            );
        }
    }

    public record InvoiceResponse(
            Long id,
            String invoiceNumber,
            Long studentId,
            String studentCode,
            String studentName,
            Long sessionId,
            String sessionName,
            Long classId,
            String className,
            Long sectionId,
            String sectionName,
            String title,
            String billingMonth,
            LocalDate issueDate,
            LocalDate dueDate,
            BigDecimal subtotalAmount,
            BigDecimal waiverAmount,
            BigDecimal fineAmount,
            BigDecimal netAmount,
            BigDecimal paidAmount,
            BigDecimal dueAmount,
            InvoiceStatus status,
            String statusBangla,
            String notes,
            List<InvoiceItemResponse> items,
            List<PaymentReceiptResponse> payments
    ) {
        public static InvoiceResponse from(Invoice inv) {
            List<InvoiceItemResponse> itemDtos = new ArrayList<>();
            if (inv.getItems() != null) {
                for (int i = 0; i < inv.getItems().size(); i++) {
                    itemDtos.add(InvoiceItemResponse.from(inv.getItems().get(i)));
                }
            }

            List<PaymentReceiptResponse> paymentDtos = new ArrayList<>();
            if (inv.getPayments() != null) {
                for (int i = 0; i < inv.getPayments().size(); i++) {
                    paymentDtos.add(PaymentReceiptResponse.from(inv.getPayments().get(i)));
                }
            }

            return new InvoiceResponse(
                    inv.getId(),
                    inv.getInvoiceNumber(),
                    inv.getStudent() != null ? inv.getStudent().getId() : null,
                    inv.getStudent() != null ? inv.getStudent().getStudentCode() : null,
                    inv.getStudent() != null ? inv.getStudent().getFullName() : null,
                    inv.getSession() != null ? inv.getSession().getId() : null,
                    inv.getSession() != null ? inv.getSession().getName() : null,
                    inv.getSchoolClass() != null ? inv.getSchoolClass().getId() : null,
                    inv.getSchoolClass() != null ? inv.getSchoolClass().getName() : null,
                    inv.getSection() != null ? inv.getSection().getId() : null,
                    inv.getSection() != null ? inv.getSection().getName() : null,
                    inv.getTitle(),
                    inv.getBillingMonth(),
                    inv.getIssueDate(),
                    inv.getDueDate(),
                    inv.getSubtotalAmount(),
                    inv.getWaiverAmount(),
                    inv.getFineAmount(),
                    inv.getNetAmount(),
                    inv.getPaidAmount(),
                    inv.getDueAmount(),
                    inv.getStatus(),
                    inv.getStatus().getBanglaLabel(),
                    inv.getNotes(),
                    itemDtos,
                    paymentDtos
            );
        }
    }

    public record ApplyFineRequest(
            @NotNull(message = "ফাইনের পরিমাণ আবশ্যক")
            @DecimalMin(value = "0.00", message = "ফাইন ঋণাত্মক হতে পারে না")
            BigDecimal fineAmount,
            String reason
    ) {}
}
