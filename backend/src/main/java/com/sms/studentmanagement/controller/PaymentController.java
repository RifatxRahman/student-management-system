package com.sms.studentmanagement.controller;

import com.sms.studentmanagement.dto.PaymentDtos.CreatePaymentIntentRequest;
import com.sms.studentmanagement.dto.PaymentDtos.ExecutePaymentRequest;
import com.sms.studentmanagement.dto.PaymentDtos.PaymentIntentResponse;
import com.sms.studentmanagement.dto.PaymentDtos.PaymentReceiptSummary;
import com.sms.studentmanagement.dto.PaymentDtos.PaymentTransactionDetail;
import com.sms.studentmanagement.service.OnlinePaymentGatewayService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final OnlinePaymentGatewayService paymentGatewayService;

    @PostMapping("/create-intent")
    public ResponseEntity<PaymentIntentResponse> createPaymentIntent(
            @Valid @RequestBody CreatePaymentIntentRequest request,
            Principal principal) {
        String username = principal != null ? principal.getName() : "ANONYMOUS";
        PaymentIntentResponse response = paymentGatewayService.createPaymentIntent(request, username);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/execute")
    public ResponseEntity<PaymentReceiptSummary> executePayment(
            @Valid @RequestBody ExecutePaymentRequest request,
            Principal principal) {
        String username = principal != null ? principal.getName() : "ANONYMOUS";
        PaymentReceiptSummary response = paymentGatewayService.executePayment(request, username);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/query/{trxId}")
    public ResponseEntity<PaymentTransactionDetail> queryPayment(@PathVariable String trxId) {
        PaymentTransactionDetail detail = paymentGatewayService.queryPayment(trxId);
        return ResponseEntity.ok(detail);
    }

    @GetMapping("/invoice/{invoiceId}")
    public ResponseEntity<List<PaymentTransactionDetail>> getInvoiceTransactions(@PathVariable Long invoiceId) {
        List<PaymentTransactionDetail> list = paymentGatewayService.getInvoiceTransactions(invoiceId);
        return ResponseEntity.ok(list);
    }
}
