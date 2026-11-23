package com.sms.studentmanagement.dto;

import com.sms.studentmanagement.entity.PaymentGatewayProvider;
import com.sms.studentmanagement.entity.PaymentTransactionStatus;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public final class PaymentDtos {

    private PaymentDtos() {
    }

    public record CreatePaymentIntentRequest(
            @NotNull Long invoiceId,
            PaymentGatewayProvider gatewayProvider,
            String customerPhone,
            String idempotencyKey
    ) {}

    public record PaymentIntentResponse(
            String trxId,
            Long invoiceId,
            String invoiceNumber,
            String studentName,
            BigDecimal amount,
            PaymentGatewayProvider gatewayProvider,
            PaymentTransactionStatus status,
            String gatewayPaymentId,
            String paymentUrl,
            String message
    ) {}

    public record ExecutePaymentRequest(
            @NotNull String trxId,
            String gatewayPaymentId,
            String otp,
            String pin
    ) {}

    public record PaymentReceiptSummary(
            String trxId,
            String receiptNumber,
            Long invoiceId,
            BigDecimal amountPaid,
            BigDecimal remainingDue,
            String gatewayTrxId,
            Instant paidAt,
            String message
    ) {}

    public record PaymentTransactionDetail(
            Long id,
            String trxId,
            Long invoiceId,
            String invoiceNumber,
            String studentName,
            BigDecimal amount,
            PaymentGatewayProvider gatewayProvider,
            PaymentTransactionStatus status,
            String gatewayPaymentId,
            String gatewayTrxId,
            String customerPhone,
            Instant createdAt,
            Instant verifiedAt
    ) {}
}
